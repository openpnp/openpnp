package org.openpnp.vision.gpu;

import java.nio.ByteBuffer;

final class GpuNative {
    private GpuNative() {
    }

    static native String init();

    static native boolean hasInt64();

    static native boolean hasByteStorage();

    static native boolean hasArrayIndexing();

    static native long completed();

    static native boolean await(long value, long timeoutNs);

    static native void release(long handle);

    static native long createBuffer(long size, boolean hostVisible);

    static native ByteBuffer map(long handle);

    static native long createPipeline(String shader, int[] spec, int[] uniformBindings, int[] counts);

    static native long createProgram(long[] pipelines, long[] buffers, long[] bufferRanges, int[] bufferCounts,
            int[] groups, long[] indirect, long[] offsets);

    static native long submit(long program);
}
