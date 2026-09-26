package org.openpnp.vision.gpu;

import java.nio.ByteBuffer;

import org.openpnp.vision.gpu.GpuPipeline.Binding;

/**
 * Computes the rotated cross-sections of one DetectRectlinearSymmetry pass for all angles on the GPU.
 */
public class GpuRectlinearSymmetry {
    private static GpuBuffer params;
    private static GpuBuffer pixelBuffer;
    private static GpuBuffer sumBuffer;
    private static GpuBuffer weightBuffer;
    private static GpuBuffer maskedBuffer;

    public static boolean isAvailable() {
        return GpuRuntime.isAvailable();
    }

    /**
     * Outputs are indexed by angle, then bin (the wCross x-section bins followed by the hCross
     * y-section bins), then channel for the sums.
     */
    public static synchronized void crossSections(byte[] pixels, float[] gammaLut, float[] sines, float[] cosines,
            int width, int channels, int x0, int sub, int cols, int rows, int cxPixels, int cyPixels,
            int wCross, int hCross, float cxCross, float cyCross, float thresholdLuminance,
            float[] sums, float[] weights, float[] maskedWeights) {
        if (!isAvailable()) {
            throw new IllegalStateException("GPU is not available");
        }
        int angles = sines.length;
        int bins = wCross + hCross;
        params = GpuCompute.scratch(params, 64);
        pixelBuffer = GpuCompute.scratch(pixelBuffer, pixels.length);
        sumBuffer = GpuCompute.scratch(sumBuffer, (long) angles * bins * channels * 4);
        weightBuffer = GpuCompute.scratch(weightBuffer, (long) angles * bins * 4);
        maskedBuffer = GpuCompute.scratch(maskedBuffer, (long) angles * bins * 4);
        GpuPipeline pipeline = GpuCompute.pipeline("rectlinear_symmetry", new int[] { channels }, Binding.Uniform,
                Binding.Storage, Binding.Storage, Binding.Storage, Binding.Storage, Binding.Storage,
                Binding.Storage, Binding.Storage);
        GpuProgram program = GpuCompute.program(pipeline, (bins + 63) / 64, angles, 1, params, pixelBuffer,
                GpuCompute.constant(gammaLut), GpuCompute.constant(sines), GpuCompute.constant(cosines),
                sumBuffer, weightBuffer, maskedBuffer);
        ByteBuffer p = params.map();
        int[] values = { width, x0, sub, cols, rows, cxPixels, cyPixels, wCross, hCross, angles };
        for (int i = 0; i < values.length; i++) {
            p.putInt(i * 4, values[i]);
        }
        p.putFloat(40, cxCross).putFloat(44, cyCross).putFloat(48, thresholdLuminance);
        pixelBuffer.map().put(pixels);
        GpuRuntime.await(program.submit(), GpuCompute.TIMEOUT_NS);
        sumBuffer.map().asFloatBuffer().get(sums, 0, angles * bins * channels);
        weightBuffer.map().asFloatBuffer().get(weights, 0, angles * bins);
        maskedBuffer.map().asFloatBuffer().get(maskedWeights, 0, angles * bins);
    }
}
