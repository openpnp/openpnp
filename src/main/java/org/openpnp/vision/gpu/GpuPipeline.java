package org.openpnp.vision.gpu;

import java.util.Arrays;

/**
 * A compiled compute shader. Specialization constants are fixed at compile time, so the driver can
 * fold them; use them only for values that change the shape of the code.
 */
public class GpuPipeline extends GpuObject {
    public static final int SLOTS = 8;

    public enum Binding {
        Uniform(1),
        Storage(1),
        // An array of storage buffers the shader indexes with a parameter, such as a camera's frames.
        Slots(SLOTS);

        final int count;

        Binding(int count) {
            this.count = count;
        }
    }

    private final int bufferCount;

    public GpuPipeline(String shader, int[] specialization, Binding... bindings) {
        super(GpuNative.createPipeline(shader, specialization, uniformFlags(bindings), counts(bindings)));
        this.bufferCount = Arrays.stream(bindings).mapToInt(b -> b.count).sum();
    }

    /**
     * The number of buffers a dispatch binds, counting each array element.
     */
    public int getBufferCount() {
        return bufferCount;
    }

    private static int[] uniformFlags(Binding[] bindings) {
        return Arrays.stream(bindings).mapToInt(b -> b == Binding.Uniform ? 1 : 0).toArray();
    }

    private static int[] counts(Binding[] bindings) {
        return Arrays.stream(bindings).mapToInt(b -> b.count).toArray();
    }
}
