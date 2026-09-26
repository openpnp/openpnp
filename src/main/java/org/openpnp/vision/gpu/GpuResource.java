package org.openpnp.vision.gpu;

/**
 * Something that holds GPU objects and can be kept in a GpuCache.
 */
public interface GpuResource extends AutoCloseable {
    boolean isClosed();

    @Override
    void close();
}
