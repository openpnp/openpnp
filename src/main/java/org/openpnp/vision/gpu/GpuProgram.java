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

    /**
     * Part of a buffer bound to one descriptor; size 0 binds up to the end.
     */
    public static final class Range {
        final GpuBuffer buffer;
        final long offset;
        final long size;

        public Range(GpuBuffer buffer, long offset, long size) {
            this.buffer = buffer;
            this.offset = offset;
            this.size = size;
        }
    }

    public static class Builder {
        private final List<Long> pipelines = new ArrayList<>();
        private final List<Long> buffers = new ArrayList<>();
        private final List<Long> ranges = new ArrayList<>();
        private final List<Integer> bufferCounts = new ArrayList<>();
        private final List<Integer> groups = new ArrayList<>();
        private final List<Long> indirect = new ArrayList<>();
        private final List<Long> offsets = new ArrayList<>();

        public Builder dispatch(GpuPipeline pipeline, int groupsX, int groupsY, int groupsZ, GpuBuffer... bindings) {
            return dispatch(pipeline, groupsX, groupsY, groupsZ, whole(bindings));
        }

        public Builder dispatch(GpuPipeline pipeline, int groupsX, int groupsY, int groupsZ, Range... bindings) {
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
            add(pipeline, whole(bindings), groupCounts, offset);
            return this;
        }

        public Builder copy(GpuBuffer src, long srcOffset, GpuBuffer dst, long dstOffset, long size) {
            pipelines.add(0L);
            addBuffer(new Range(src, 0, 0));
            addBuffer(new Range(dst, 0, 0));
            bufferCounts.add(2);
            addGroups();
            indirect.add(0L);
            offsets.add(srcOffset);
            offsets.add(dstOffset);
            offsets.add(size);
            return this;
        }

        private static Range[] whole(GpuBuffer[] bindings) {
            Range[] whole = new Range[bindings.length];
            for (int i = 0; i < bindings.length; i++) {
                whole[i] = new Range(bindings[i], 0, 0);
            }
            return whole;
        }

        private void add(GpuPipeline pipeline, Range[] bindings, GpuBuffer groupCounts, long offset) {
            if (bindings.length != pipeline.getBufferCount()) {
                throw new IllegalArgumentException("pipeline binds " + pipeline.getBufferCount()
                        + " buffers, got " + bindings.length);
            }
            pipelines.add(pipeline.handle());
            for (Range range : bindings) {
                addBuffer(range);
            }
            bufferCounts.add(bindings.length);
            addGroups();
            indirect.add(groupCounts == null ? 0L : groupCounts.handle());
            offsets.add(offset);
            offsets.add(0L);
            offsets.add(0L);
        }

        private void addBuffer(Range range) {
            buffers.add(range.buffer.handle());
            ranges.add(range.offset);
            ranges.add(range.size);
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
                    ranges.stream().mapToLong(Long::longValue).toArray(),
                    bufferCounts.stream().mapToInt(Integer::intValue).toArray(),
                    groups.stream().mapToInt(Integer::intValue).toArray(),
                    indirect.stream().mapToLong(Long::longValue).toArray(),
                    offsets.stream().mapToLong(Long::longValue).toArray()));
        }
    }
}
