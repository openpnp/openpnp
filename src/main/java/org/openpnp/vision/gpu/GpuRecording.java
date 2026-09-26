package org.openpnp.vision.gpu;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.LongConsumer;

import org.openpnp.vision.gpu.GpuProgram.Range;

/**
 * Collects GPU work into a single command buffer that is submitted once, when a result is needed
 * or the outermost open() is closed. The command buffer, its parameters and the images it writes
 * are cached by the recorded structure, so repeating the same work only rewrites parameters.
 * Recordings are per thread; open() joins the thread's current one.
 */
public final class GpuRecording implements AutoCloseable {
    public static final int PARAMS_SIZE = 256;

    private static final ThreadLocal<GpuRecording> current = new ThreadLocal<>();
    private static final GpuCache<List<Object>, Variant> variants = new GpuCache<>(2, TimeUnit.MINUTES);

    private int depth;
    private boolean failed;
    private final List<Step> steps = new ArrayList<>();
    private final List<GpuImage> outputs = new ArrayList<>();
    private final List<Scratch> scratches = new ArrayList<>();
    private final List<GpuImage> inputs = new ArrayList<>();
    private final List<LongConsumer> submitted = new ArrayList<>();

    private static final class Step {
        final GpuPipeline pipeline;
        final int[] groups;
        final byte[] params;
        final Object[] args;

        Step(GpuPipeline pipeline, int[] groups, byte[] params, Object[] args) {
            this.pipeline = pipeline;
            this.groups = groups;
            this.params = params;
            this.args = args;
        }
    }

    /**
     * A buffer only the recorded work uses, such as intermediate results; a host visible one can
     * be read back once the work was submitted.
     */
    public final class Scratch {
        private final long size;
        private final boolean hostVisible;
        private GpuBuffer buffer;
        private Instance instance;
        private long ready;

        private Scratch(long size, boolean hostVisible) {
            this.size = size;
            this.hostVisible = hostVisible;
        }

        /**
         * Submits the recording if needed, waits for the GPU and copies the buffer. A host visible
         * scratch keeps its recorded program from being reused until it was read once.
         */
        public ByteBuffer read() {
            if (!hostVisible) {
                throw new IllegalStateException("scratch buffer is not host visible");
            }
            synchronized (GpuRecording.this) {
                if (buffer == null) {
                    flush();
                }
            }
            if (buffer == null) {
                throw new IllegalStateException("GPU work failed");
            }
            try {
                GpuRuntime.await(ready, GpuCompute.TIMEOUT_NS);
                ByteBuffer mapped = buffer.map();
                mapped.limit((int) size);
                ByteBuffer copy = ByteBuffer.allocate((int) size).order(ByteOrder.nativeOrder());
                copy.put(mapped);
                copy.flip();
                return copy;
            }
            finally {
                synchronized (GpuRecording.this) {
                    if (instance != null) {
                        instance.release();
                        instance = null;
                    }
                }
            }
        }
    }

    /**
     * Joins the thread's current recording, or starts one that is submitted when closed.
     */
    public static GpuRecording open() {
        GpuRecording recording = current.get();
        if (recording == null) {
            recording = new GpuRecording();
            current.set(recording);
        }
        recording.depth++;
        return recording;
    }

    public static ByteBuffer params() {
        return ByteBuffer.allocate(PARAMS_SIZE).order(ByteOrder.nativeOrder());
    }

    public static ByteBuffer params(int... values) {
        ByteBuffer params = params();
        for (int value : values) {
            params.putInt(value);
        }
        return params;
    }

    /**
     * A new image the recorded work writes.
     */
    public synchronized GpuImage image(int rows, int cols, int type) {
        GpuImage image = new GpuImage(rows, cols, type, this);
        outputs.add(image);
        return image;
    }

    public synchronized Scratch scratch(long size, boolean hostVisible) {
        Scratch scratch = new Scratch(Math.max(16, (size + 15) & ~15L), hostVisible);
        scratches.add(scratch);
        return scratch;
    }

    /**
     * Records a dispatch. params (binding 0, filled up to its position) may be null for shaders
     * without; args bind in order and may be GpuImages, GpuBuffers, Scratches or GpuBuffer arrays
     * for Slots bindings.
     */
    public synchronized void dispatch(GpuPipeline pipeline, int groupsX, int groupsY, int groupsZ, ByteBuffer params,
            Object... args) {
        byte[] bytes = null;
        if (params != null) {
            bytes = new byte[params.position()];
            params.duplicate().flip().get(bytes);
        }
        steps.add(new Step(pipeline, new int[] { groupsX, groupsY, groupsZ }, bytes, args));
    }

    public synchronized void copy(GpuImage src, GpuImage dst) {
        steps.add(new Step(null, null, null, new Object[] { src, dst }));
    }

    /**
     * Calls back with the timeline value that runs the work recorded so far, or 0 if it never
     * runs, e.g. to hand an input buffer back once the GPU has read it.
     */
    public synchronized void onSubmit(LongConsumer callback) {
        submitted.add(callback);
    }

    /**
     * Submits everything recorded so far; later work starts a new command buffer.
     */
    public synchronized void flush() {
        if (steps.isEmpty()) {
            finish(0);
            return;
        }
        Instance instance = null;
        long value = 0;
        try {
            List<Object> key = key();
            instance = variants.get(key, k -> new Variant()).acquire(this);
            if (instance.lastSubmit > 0) {
                GpuRuntime.await(instance.lastSubmit, GpuCompute.TIMEOUT_NS);
            }
            ByteBuffer mapped = instance.params.map();
            for (int i = 0; i < steps.size(); i++) {
                if (steps.get(i).params != null) {
                    mapped.position(i * PARAMS_SIZE);
                    mapped.put(steps.get(i).params);
                }
            }
            for (int i = 0; i < outputs.size(); i++) {
                outputs.get(i).bind(instance.nodes[i], instance);
            }
            for (int i = 0; i < scratches.size(); i++) {
                Scratch scratch = scratches.get(i);
                scratch.buffer = instance.scratch[i];
                if (scratch.hostVisible) {
                    instance.hold();
                    scratch.instance = instance;
                }
            }
            value = instance.program.submit();
            instance.lastSubmit = value;
        }
        catch (RuntimeException e) {
            failed = true;
            for (GpuImage output : outputs) {
                output.fail();
            }
            throw e;
        }
        finally {
            if (instance != null) {
                instance.release();
            }
            finish(value);
        }
    }

    private void finish(long value) {
        for (GpuImage output : outputs) {
            output.writtenBy(value);
        }
        for (GpuImage input : inputs) {
            input.readBy(value);
        }
        for (Scratch scratch : scratches) {
            scratch.ready = value;
        }
        List<LongConsumer> callbacks = new ArrayList<>(submitted);
        steps.clear();
        outputs.clear();
        scratches.clear();
        inputs.clear();
        submitted.clear();
        RuntimeException failure = null;
        for (LongConsumer callback : callbacks) {
            try {
                callback.accept(value);
            }
            catch (RuntimeException e) {
                failure = e;
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    /**
     * Whether submitting any of the recorded work failed.
     */
    public synchronized boolean hasFailed() {
        return failed;
    }

    @Override
    public void close() {
        synchronized (this) {
            if (--depth > 0) {
                return;
            }
            current.remove();
        }
        flush();
    }

    // Describes the steps with the buffers they bind: images and scratches written by this
    // recording by index, anything else by identity.
    private List<Object> key() {
        inputs.clear();
        List<Object> key = new ArrayList<>();
        for (GpuImage output : outputs) {
            key.add(output.byteSize());
        }
        key.add("|");
        for (Scratch scratch : scratches) {
            key.add(scratch.hostVisible ? scratch.size : -scratch.size);
        }
        for (Step step : steps) {
            key.add("|");
            if (step.pipeline != null) {
                key.add(step.pipeline);
                key.add(step.groups[0]);
                key.add(step.groups[1]);
                key.add(step.groups[2]);
                key.add(step.params != null);
            }
            for (Object arg : step.args) {
                addArg(key, arg);
            }
        }
        return key;
    }

    private void addArg(List<Object> key, Object arg) {
        if (arg instanceof GpuImage) {
            GpuImage image = (GpuImage) arg;
            int index = indexOf(outputs, image);
            if (index >= 0) {
                key.add("i" + index);
            }
            else {
                key.add(image.buffer());
                inputs.add(image);
            }
        }
        else if (arg instanceof Scratch) {
            int index = indexOf(scratches, arg);
            key.add(index >= 0 ? "s" + index : ((Scratch) arg).buffer);
        }
        else if (arg instanceof GpuBuffer[]) {
            for (GpuBuffer buffer : (GpuBuffer[]) arg) {
                key.add(buffer);
            }
        }
        else if (arg instanceof GpuBuffer) {
            key.add(arg);
        }
        else {
            throw new IllegalArgumentException("cannot bind " + arg);
        }
    }

    private static int indexOf(List<?> list, Object item) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == item) {
                return i;
            }
        }
        return -1;
    }

    private GpuBuffer resolve(Object arg, Instance instance) {
        if (arg instanceof GpuImage) {
            int index = indexOf(outputs, arg);
            return index >= 0 ? instance.nodes[index] : ((GpuImage) arg).buffer();
        }
        if (arg instanceof Scratch) {
            int index = indexOf(scratches, arg);
            return index >= 0 ? instance.scratch[index] : ((Scratch) arg).buffer;
        }
        return (GpuBuffer) arg;
    }

    private Instance record(Variant variant) {
        Instance instance = new Instance(variant);
        try {
            instance.params = new GpuBuffer((long) Math.max(1, steps.size()) * PARAMS_SIZE, true);
            instance.nodes = new GpuBuffer[outputs.size()];
            for (int i = 0; i < outputs.size(); i++) {
                instance.nodes[i] = new GpuBuffer(Math.max(16, outputs.get(i).byteSize()), true);
            }
            instance.scratch = new GpuBuffer[scratches.size()];
            for (int i = 0; i < scratches.size(); i++) {
                instance.scratch[i] = new GpuBuffer(scratches.get(i).size, scratches.get(i).hostVisible);
            }
            GpuProgram.Builder builder = new GpuProgram.Builder();
            for (int s = 0; s < steps.size(); s++) {
                Step step = steps.get(s);
                if (step.pipeline == null) {
                    GpuBuffer src = resolve(step.args[0], instance);
                    GpuBuffer dst = resolve(step.args[1], instance);
                    builder.copy(src, 0, dst, 0, ((GpuImage) step.args[1]).byteSize());
                    continue;
                }
                List<Range> ranges = new ArrayList<>();
                if (step.params != null) {
                    ranges.add(new Range(instance.params, (long) s * PARAMS_SIZE, PARAMS_SIZE));
                }
                for (Object arg : step.args) {
                    if (arg instanceof GpuBuffer[]) {
                        for (GpuBuffer buffer : (GpuBuffer[]) arg) {
                            ranges.add(new Range(buffer, 0, 0));
                        }
                    }
                    else {
                        ranges.add(new Range(resolve(arg, instance), 0, 0));
                    }
                }
                builder.dispatch(step.pipeline, step.groups[0], step.groups[1], step.groups[2],
                        ranges.toArray(new Range[0]));
            }
            instance.program = builder.build();
            return instance;
        }
        catch (RuntimeException e) {
            instance.close();
            throw e;
        }
    }

    /**
     * One recorded copy of the work with its own buffers. It is reused once nothing holds on to
     * the images it wrote.
     */
    static final class Instance {
        private final Variant variant;
        private GpuProgram program;
        private GpuBuffer params;
        private GpuBuffer[] nodes = new GpuBuffer[0];
        private GpuBuffer[] scratch = new GpuBuffer[0];
        private long lastSubmit;
        private int busy;
        private boolean retired;

        private Instance(Variant variant) {
            this.variant = variant;
        }

        void hold() {
            synchronized (variant) {
                busy++;
            }
        }

        void release() {
            synchronized (variant) {
                if (--busy == 0 && retired) {
                    close();
                }
            }
        }

        private void close() {
            for (GpuObject object : new GpuObject[] { program, params }) {
                if (object != null) {
                    object.close();
                }
            }
            for (GpuBuffer buffer : nodes) {
                if (buffer != null) {
                    buffer.close();
                }
            }
            for (GpuBuffer buffer : scratch) {
                if (buffer != null) {
                    buffer.close();
                }
            }
        }
    }

    private static final class Variant implements GpuResource {
        private final List<Instance> instances = new ArrayList<>();
        private boolean closed;

        synchronized Instance acquire(GpuRecording recording) {
            for (Instance instance : instances) {
                if (instance.busy == 0) {
                    instance.busy = 1;
                    return instance;
                }
            }
            Instance instance = recording.record(this);
            instance.busy = 1;
            if (closed) {
                instance.retired = true;
            }
            else {
                instances.add(instance);
            }
            return instance;
        }

        @Override
        public synchronized boolean isClosed() {
            return closed;
        }

        // Instances still holding images close once those are released.
        @Override
        public synchronized void close() {
            closed = true;
            for (Instance instance : instances) {
                if (instance.busy == 0) {
                    instance.close();
                }
                else {
                    instance.retired = true;
                }
            }
            instances.clear();
        }
    }
}
