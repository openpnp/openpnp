package org.openpnp.vision.gpu;

import java.util.ArrayList;
import java.util.List;

/**
 * A prerecorded command buffer. Parameters live in buffers the shaders read, so a program is
 * recorded once and resubmitted with new values written into those buffers.
 */
public class GpuProgram extends GpuObject {
    private static final long TIMEOUT_NS = 5_000_000_000L;

    private GpuProgram(long handle) {
        super(handle);
    }

    /**
     * Queues the program and returns the timeline value to pass to GpuRuntime.await(). A program
     * that is still running is waited for first.
     */
    public long submit() {
        return GpuNative.submit(handle());
    }

    public void run() {
        GpuRuntime.await(submit(), TIMEOUT_NS);
    }

    public static class Builder {
        private final List<Long> pipelines = new ArrayList<>();
        private final List<Long> buffers = new ArrayList<>();
        private final List<Integer> bufferCounts = new ArrayList<>();
        private final List<Integer> groups = new ArrayList<>();
        private final List<Long> indirect = new ArrayList<>();
        private final List<Long> offsets = new ArrayList<>();

        public Builder dispatch(GpuPipeline pipeline, int groupsX, int groupsY, int groupsZ, GpuBuffer... bindings) {
            add(pipeline, bindings, null, 0);
            groups.set(groups.size() - 3, groupsX);
            groups.set(groups.size() - 2, groupsY);
            groups.set(groups.size() - 1, groupsZ);
            return this;
        }

        /**
         * The group counts are three uints at offset in groupCounts, written by the CPU or an
         * earlier dispatch.
         */
        public Builder dispatchIndirect(GpuPipeline pipeline, GpuBuffer groupCounts, long offset,
                GpuBuffer... bindings) {
            add(pipeline, bindings, groupCounts, offset);
            return this;
        }

        public Builder copy(GpuBuffer src, long srcOffset, GpuBuffer dst, long dstOffset, long size) {
            pipelines.add(0L);
            buffers.add(src.handle());
            buffers.add(dst.handle());
            bufferCounts.add(2);
            addGroups();
            indirect.add(0L);
            offsets.add(srcOffset);
            offsets.add(dstOffset);
            offsets.add(size);
            return this;
        }

        private void add(GpuPipeline pipeline, GpuBuffer[] bindings, GpuBuffer groupCounts, long offset) {
            if (bindings.length != pipeline.getBindingCount()) {
                throw new IllegalArgumentException("pipeline has " + pipeline.getBindingCount()
                        + " bindings, got " + bindings.length + " buffers");
            }
            pipelines.add(pipeline.handle());
            for (GpuBuffer buffer : bindings) {
                buffers.add(buffer.handle());
            }
            bufferCounts.add(bindings.length);
            addGroups();
            indirect.add(groupCounts == null ? 0L : groupCounts.handle());
            offsets.add(offset);
            offsets.add(0L);
            offsets.add(0L);
        }

        private void addGroups() {
            groups.add(1);
            groups.add(1);
            groups.add(1);
        }

        public GpuProgram build() {
            return new GpuProgram(GpuNative.createProgram(
                    pipelines.stream().mapToLong(Long::longValue).toArray(),
                    buffers.stream().mapToLong(Long::longValue).toArray(),
                    bufferCounts.stream().mapToInt(Integer::intValue).toArray(),
                    groups.stream().mapToInt(Integer::intValue).toArray(),
                    indirect.stream().mapToLong(Long::longValue).toArray(),
                    offsets.stream().mapToLong(Long::longValue).toArray()));
        }
    }
}
