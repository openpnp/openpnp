package org.openpnp.vision.gpu;

/**
 * A compiled compute shader. Specialization constants are fixed at compile time, so the driver can
 * fold them; use them only for values that change the shape of the code.
 */
public class GpuPipeline extends GpuObject {
    public enum Binding {
        Uniform,
        Storage
    }

    private final int bindingCount;

    public GpuPipeline(String shader, int[] specialization, Binding... bindings) {
        super(GpuNative.createPipeline(shader, specialization, uniformFlags(bindings)));
        this.bindingCount = bindings.length;
    }

    public int getBindingCount() {
        return bindingCount;
    }

    private static int[] uniformFlags(Binding[] bindings) {
        int[] flags = new int[bindings.length];
        for (int i = 0; i < bindings.length; i++) {
            flags[i] = bindings[i] == Binding.Uniform ? 1 : 0;
        }
        return flags;
    }
}
