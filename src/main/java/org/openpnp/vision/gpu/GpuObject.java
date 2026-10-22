package org.openpnp.vision.gpu;

/**
 * Owns a native GPU object. Closing it only drops Java's reference; the object is destroyed once
 * neither the GPU nor any program still uses it.
 */
public abstract class GpuObject implements GpuResource {
    private long handle;

    protected GpuObject(long handle) {
        this.handle = handle;
    }

    synchronized long handle() {
        if (handle == 0) {
            throw new IllegalStateException(getClass().getSimpleName() + " is closed");
        }
        return handle;
    }

    public synchronized boolean isClosed() {
        return handle == 0;
    }

    @Override
    public synchronized void close() {
        if (handle != 0) {
            GpuNative.release(handle);
            handle = 0;
        }
    }
}
