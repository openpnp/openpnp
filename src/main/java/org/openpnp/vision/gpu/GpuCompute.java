package org.openpnp.vision.gpu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openpnp.vision.gpu.GpuPipeline.Binding;

/**
 * Shared caches for compute kernels: compiled pipelines, constant inputs keyed by their content,
 * and recorded single-dispatch programs.
 */
public class GpuCompute {
    public static final long TIMEOUT_NS = TimeUnit.SECONDS.toNanos(5);

    private static final GpuCache<List<Object>, GpuPipeline> pipelines = new GpuCache<>(10, TimeUnit.MINUTES);
    private static final GpuCache<ArrayKey, GpuBuffer> constants = new GpuCache<>(2, TimeUnit.MINUTES);
    private static final GpuCache<List<Object>, GpuProgram> programs = new GpuCache<>(2, TimeUnit.MINUTES);

    private static final class ArrayKey {
        final Object array;
        final int hash;

        ArrayKey(Object array) {
            this.array = array;
            this.hash = array instanceof int[] ? Arrays.hashCode((int[]) array) : Arrays.hashCode((float[]) array);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ArrayKey)) {
                return false;
            }
            Object other = ((ArrayKey) o).array;
            if (array instanceof int[]) {
                return other instanceof int[] && Arrays.equals((int[]) array, (int[]) other);
            }
            return other instanceof float[] && Arrays.equals((float[]) array, (float[]) other);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    public static GpuPipeline pipeline(String shader, int[] specialization, Binding... bindings) {
        List<Object> key = new ArrayList<>();
        key.add(shader);
        key.add(Arrays.toString(specialization));
        key.addAll(Arrays.asList(bindings));
        return pipelines.get(key, k -> new GpuPipeline(shader, specialization, bindings));
    }

    /**
     * A GPU copy of an array that is never written again. Equal arrays share one buffer.
     */
    public static GpuBuffer constant(int[] values) {
        return constants.get(new ArrayKey(values.clone()), k -> {
            GpuBuffer buffer = new GpuBuffer(Math.max(16, values.length * 4L), true);
            buffer.map().asIntBuffer().put(values);
            return buffer;
        });
    }

    public static GpuBuffer constant(float[] values) {
        return constants.get(new ArrayKey(values.clone()), k -> {
            GpuBuffer buffer = new GpuBuffer(Math.max(16, values.length * 4L), true);
            buffer.map().asFloatBuffer().put(values);
            return buffer;
        });
    }

    /**
     * A program running one dispatch, recorded once per pipeline, group counts and buffers.
     */
    public static GpuProgram program(GpuPipeline pipeline, int groupsX, int groupsY, int groupsZ,
            GpuBuffer... bindings) {
        List<Object> key = new ArrayList<>();
        key.add(pipeline);
        key.add(groupsX);
        key.add(groupsY);
        key.add(groupsZ);
        key.addAll(Arrays.asList(bindings));
        return programs.get(key, k -> new GpuProgram.Builder()
                .dispatch(pipeline, groupsX, groupsY, groupsZ, bindings).build());
    }

    /**
     * A reusable host-visible buffer of at least size bytes, replaced when too small.
     */
    public static GpuBuffer scratch(GpuBuffer current, long size) {
        size = Math.max(16, (size + 15) & ~15L);
        if (current != null && !current.isClosed() && current.getSize() >= size) {
            return current;
        }
        if (current != null) {
            current.close();
        }
        return new GpuBuffer(size, true);
    }
}
