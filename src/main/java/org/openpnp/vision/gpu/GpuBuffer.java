package org.openpnp.vision.gpu;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class GpuBuffer extends GpuObject {
    private final long size;
    private ByteBuffer mapped;

    /**
     * Host-visible buffers can be read and written through map(); others live only on the GPU.
     */
    public GpuBuffer(long size, boolean hostVisible) {
        super(GpuNative.createBuffer(size, hostVisible));
        this.size = size;
        if (hostVisible) {
            mapped = GpuNative.map(handle()).order(ByteOrder.nativeOrder());
        }
    }

    public long getSize() {
        return size;
    }

    /**
     * The buffer's memory. Don't touch it while a submitted program still uses the buffer, and
     * never after close().
     */
    public synchronized ByteBuffer map() {
        if (mapped == null) {
            throw new IllegalStateException(isClosed() ? "GpuBuffer is closed" : "GpuBuffer is not host visible");
        }
        return mapped.duplicate().order(ByteOrder.nativeOrder());
    }

    @Override
    public synchronized void close() {
        mapped = null;
        super.close();
    }
}
