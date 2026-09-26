package org.openpnp.vision.gpu;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.LongConsumer;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.openpnp.vision.gpu.GpuPipeline.Binding;

/**
 * Converts camera frames on the GPU and applies a camera's white balance LUT and composite
 * transform remap, either at full resolution or downscaled for a preview.
 */
public class GpuCameraTransform implements AutoCloseable {
    public enum Input {
        Yuyv,
        Bgr,
        Gray
    }

    private static final int OUTPUT_PACKED = 0;
    private static final int OUTPUT_XRGB = 1;
    private static final int OUTPUT_SETTLE = 2;
    private static final int PARAMS_SIZE = 64;
    private static final int MAX_BOX = 4;
    private static final long TIMEOUT_NS = TimeUnit.SECONDS.toNanos(5);

    private static final GpuCache<List<Integer>, GpuPipeline> pipelines = new GpuCache<>(10, TimeUnit.MINUTES);

    private final GpuCache<TargetKey, Target> targets = new GpuCache<>(1, TimeUnit.MINUTES);
    private GpuBuffer map;
    private GpuBuffer lut;
    private final GpuBuffer unused;
    private GpuBuffer upload;
    private int mapWidth;
    private int mapHeight;
    private boolean closed;

    private static final class TargetKey {
        final List<GpuBuffer> source;
        final Input input;
        final int output;
        final int dstWidth;
        final int dstHeight;
        final int box;

        TargetKey(List<GpuBuffer> source, Input input, int output, int dstWidth, int dstHeight, int box) {
            this.source = source;
            this.input = input;
            this.output = output;
            this.dstWidth = dstWidth;
            this.dstHeight = dstHeight;
            this.box = box;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof TargetKey)) {
                return false;
            }
            TargetKey k = (TargetKey) o;
            return source.equals(k.source) && input == k.input && output == k.output && dstWidth == k.dstWidth
                    && dstHeight == k.dstHeight && box == k.box;
        }

        @Override
        public int hashCode() {
            return Objects.hash(source, input, output, dstWidth, dstHeight, box);
        }
    }

    // A recorded program with its own parameters and output, used by one render at a time.
    private static class Target implements GpuResource {
        final GpuProgram program;
        final GpuBuffer params;
        final GpuBuffer output;

        Target(GpuProgram program, GpuBuffer params, GpuBuffer output) {
            this.program = program;
            this.params = params;
            this.output = output;
        }

        @Override
        public boolean isClosed() {
            return program.isClosed();
        }

        @Override
        public void close() {
            program.close();
            params.close();
            output.close();
        }
    }

    public GpuCameraTransform() {
        if (!GpuRuntime.hasArrayIndexing()) {
            throw new IllegalStateException("GPU is not available");
        }
        unused = new GpuBuffer(16, false);
    }

    /**
     * The buffers a camera captures into, padded to the slots the shader binds. Frames are
     * rendered from one of them by index, so one recorded program serves every frame.
     */
    public synchronized GpuBuffer[] slots(GpuBuffer... buffers) {
        if (buffers.length > GpuPipeline.SLOTS) {
            throw new IllegalArgumentException("at most " + GpuPipeline.SLOTS + " frame buffers");
        }
        GpuBuffer[] slots = Arrays.copyOf(buffers, GpuPipeline.SLOTS);
        Arrays.fill(slots, buffers.length, slots.length, unused);
        return slots;
    }

    /**
     * mapX and mapY are CV_32FC1 source coordinates for every output pixel, or null to keep the
     * input geometry. lut is a 256x1 CV_8UC3 table applied before the remap, or null.
     */
    public synchronized void setTransform(Mat mapX, Mat mapY, Mat lut) {
        checkOpen();
        targets.clear();
        if (map != null) {
            map.close();
            map = null;
        }
        if (this.lut != null) {
            this.lut.close();
            this.lut = null;
        }
        if (mapX != null) {
            mapWidth = mapX.cols();
            mapHeight = mapX.rows();
            float[] xs = new float[mapWidth * mapHeight];
            float[] ys = new float[xs.length];
            mapX.get(0, 0, xs);
            mapY.get(0, 0, ys);
            map = new GpuBuffer(xs.length * 8L, true);
            ByteBuffer mapped = map.map();
            for (int i = 0; i < xs.length; i++) {
                mapped.putFloat(i * 8, xs[i]);
                mapped.putFloat(i * 8 + 4, ys[i]);
            }
        }
        if (lut != null) {
            if (lut.type() != CvType.CV_8UC3 || lut.total() != 256) {
                throw new IllegalArgumentException("LUT must be 256 CV_8UC3 entries");
            }
            byte[] table = new byte[256 * 3];
            lut.get(0, 0, table);
            this.lut = new GpuBuffer(256 * 4, true);
            ByteBuffer mapped = this.lut.map();
            for (int i = 0; i < 256; i++) {
                mapped.putInt(i * 4, (table[i * 3] & 0xFF) | (table[i * 3 + 1] & 0xFF) << 8
                        | (table[i * 3 + 2] & 0xFF) << 16);
            }
        }
    }

    /**
     * The width and height of full resolution renders of a width x height input.
     */
    public synchronized int[] outputSize(int width, int height) {
        return map != null ? new int[] { mapWidth, mapHeight } : new int[] { width, height };
    }

    /**
     * Renders a frame from one of the slots at full resolution. submitted receives the GPU value to
     * wait for before the source may be overwritten.
     */
    public synchronized BufferedImage render(GpuBuffer[] slots, int slot, Input input, int width, int height,
            int stride, LongConsumer submitted) {
        checkOpen();
        int dstWidth = map != null ? mapWidth : width;
        int dstHeight = map != null ? mapHeight : height;
        BufferedImage image = new BufferedImage(dstWidth, dstHeight,
                input == Input.Gray ? BufferedImage.TYPE_BYTE_GRAY : BufferedImage.TYPE_3BYTE_BGR);
        byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        Target target = target(new TargetKey(Arrays.asList(slots), input, OUTPUT_PACKED, dstWidth, dstHeight, 1));
        run(target, params(slot, width, height, stride, dstWidth, dstHeight, 1, 1), submitted);
        target.output.map().get(data);
        return image;
    }

    /**
     * Records rendering a frame at full resolution into the thread's GpuRecording. The slot must
     * stay untouched until the recording was submitted and the GPU passed its value.
     */
    public synchronized GpuImage record(GpuBuffer[] slots, int slot, Input input, int width, int height,
            int stride) {
        checkOpen();
        int dstWidth = map != null ? mapWidth : width;
        int dstHeight = map != null ? mapHeight : height;
        GpuPipeline pipeline = pipeline(input, OUTPUT_PACKED, 1);
        int invocations = (dstWidth * dstHeight + 3) / 4;
        try (GpuRecording recording = GpuRecording.open()) {
            GpuImage image = recording.image(dstHeight, dstWidth, input == Input.Gray ? CvType.CV_8UC1
                    : CvType.CV_8UC3);
            recording.dispatch(pipeline, (invocations + 63) / 64, 1, 1,
                    params(slot, width, height, stride, dstWidth, dstHeight, 1, 1), slots,
                    map != null ? map : unused, lut != null ? lut : unused, image);
            return image;
        }
    }

    /**
     * Renders a frame downscaled (or upscaled) into a TYPE_INT_RGB image of any size.
     */
    public synchronized void renderPreview(GpuBuffer[] slots, int slot, Input input, int width, int height,
            int stride, BufferedImage preview, LongConsumer submitted) {
        checkOpen();
        int fullWidth = map != null ? mapWidth : width;
        int fullHeight = map != null ? mapHeight : height;
        int dstWidth = preview.getWidth();
        int dstHeight = preview.getHeight();
        float scaleX = (float) fullWidth / dstWidth;
        float scaleY = (float) fullHeight / dstHeight;
        int box = Math.max(1, Math.min(MAX_BOX, (int) Math.ceil(Math.max(scaleX, scaleY))));
        Target target = target(new TargetKey(Arrays.asList(slots), input, OUTPUT_XRGB, dstWidth, dstHeight, box));
        run(target, params(slot, width, height, stride, dstWidth, dstHeight, scaleX, scaleY), submitted);
        target.output.map().asIntBuffer().get(((DataBufferInt) preview.getRaster().getDataBuffer()).getData());
    }

    /**
     * Renders what motion settling compares: the full resolution frame converted to gray like
     * BGR2GRAY, cropped to cropWidth x cropHeight at cropX, cropY, then shrunk by an integer
     * divisor like an INTER_LINEAR resize.
     */
    public synchronized Mat renderSettle(GpuBuffer[] slots, int slot, Input input, int width, int height, int stride,
            int cropX, int cropY, int cropWidth, int cropHeight, int divisor, LongConsumer submitted) {
        checkOpen();
        int dstWidth = cropWidth / divisor;
        int dstHeight = cropHeight / divisor;
        Target target = target(new TargetKey(Arrays.asList(slots), input, OUTPUT_SETTLE, dstWidth, dstHeight, 1));
        ByteBuffer params = params(slot, width, height, stride, dstWidth, dstHeight, 1, 1);
        params.putInt(40, cropX).putInt(44, cropY).putInt(48, divisor);
        run(target, params, submitted);
        Mat mat = new Mat(dstHeight, dstWidth, CvType.CV_8UC1);
        byte[] data = new byte[dstWidth * dstHeight];
        target.output.map().get(data);
        mat.put(0, 0, data);
        return mat;
    }

    /**
     * Transforms a TYPE_3BYTE_BGR or TYPE_BYTE_GRAY image.
     */
    public synchronized BufferedImage apply(BufferedImage image) {
        checkOpen();
        Input input;
        int stride;
        if (image.getType() == BufferedImage.TYPE_3BYTE_BGR) {
            input = Input.Bgr;
            stride = image.getWidth() * 3;
        }
        else if (image.getType() == BufferedImage.TYPE_BYTE_GRAY) {
            input = Input.Gray;
            stride = image.getWidth();
        }
        else {
            throw new IllegalArgumentException("Unsupported image type " + image.getType());
        }
        byte[] src = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        long size = (src.length + 3) & ~3L;
        if (upload == null || upload.getSize() != size) {
            if (upload != null) {
                upload.close();
            }
            targets.clear();
            upload = new GpuBuffer(size, true);
        }
        upload.map().put(src);
        return render(slots(upload), 0, input, image.getWidth(), image.getHeight(), stride, value -> {
        });
    }

    private Target target(TargetKey key) {
        return targets.get(key, k -> {
            int pixels = k.dstWidth * k.dstHeight;
            long outputSize = k.output == OUTPUT_XRGB ? pixels * 4L
                    : ((pixels + 3) / 4) * (k.input == Input.Gray || k.output == OUTPUT_SETTLE ? 4L : 12L);
            int invocations = k.output == OUTPUT_XRGB ? pixels : (pixels + 3) / 4;
            GpuPipeline pipeline = pipeline(k.input, k.output, k.box);
            GpuBuffer params = new GpuBuffer(PARAMS_SIZE, true);
            GpuBuffer output = new GpuBuffer(outputSize, true);
            GpuBuffer[] buffers = new GpuBuffer[GpuPipeline.SLOTS + 4];
            buffers[0] = params;
            for (int i = 0; i < GpuPipeline.SLOTS; i++) {
                buffers[1 + i] = k.source.get(i);
            }
            buffers[GpuPipeline.SLOTS + 1] = map != null ? map : unused;
            buffers[GpuPipeline.SLOTS + 2] = lut != null ? lut : unused;
            buffers[GpuPipeline.SLOTS + 3] = output;
            GpuProgram program = new GpuProgram.Builder()
                    .dispatch(pipeline, (invocations + 63) / 64, 1, 1, buffers)
                    .build();
            return new Target(program, params, output);
        });
    }

    private GpuPipeline pipeline(Input input, int output, int box) {
        boolean hasMap = map != null;
        boolean hasLut = lut != null && input != Input.Gray;
        List<Integer> spec = Arrays.asList(input.ordinal(), output, hasMap ? 1 : 0, hasLut ? 1 : 0, box);
        return pipelines.get(spec, s -> new GpuPipeline("camera_transform", s.stream().mapToInt(Integer::intValue)
                .toArray(), Binding.Uniform, Binding.Slots, Binding.Storage, Binding.Storage, Binding.Storage));
    }

    private ByteBuffer params(int slot, int width, int height, int stride, int dstWidth, int dstHeight,
            float scaleX, float scaleY) {
        ByteBuffer params = GpuRecording.params();
        params.putInt(width).putInt(height).putInt(stride).putInt(dstWidth).putInt(dstHeight).putInt(mapWidth)
                .putInt(mapHeight).putInt(slot).putFloat(scaleX).putFloat(scaleY).putInt(0).putInt(0).putInt(1);
        return params;
    }

    private void run(Target target, ByteBuffer params, LongConsumer submitted) {
        ByteBuffer mapped = target.params.map();
        mapped.put(params.array(), 0, PARAMS_SIZE);
        long value = target.program.submit();
        submitted.accept(value);
        GpuRuntime.await(value, TIMEOUT_NS);
    }

    public synchronized boolean isClosed() {
        return closed;
    }

    private void checkOpen() {
        if (closed) {
            throw new IllegalStateException("closed");
        }
    }

    @Override
    public synchronized void close() {
        if (!closed) {
            closed = true;
            targets.clear();
            unused.close();
            for (GpuBuffer buffer : new GpuBuffer[] { map, lut, upload }) {
                if (buffer != null) {
                    buffer.close();
                }
            }
        }
    }
}
