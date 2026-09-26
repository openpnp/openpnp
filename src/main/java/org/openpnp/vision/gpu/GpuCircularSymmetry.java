package org.openpnp.vision.gpu;

import java.nio.ByteBuffer;

import org.openpnp.vision.gpu.GpuPipeline.Binding;

/**
 * Scores all candidate centers of one DetectCircularSymmetry search pass on the GPU.
 */
public class GpuCircularSymmetry {
    private static GpuBuffer params;
    private static GpuBuffer pixelBuffer;
    private static GpuBuffer scoreBuffer;
    private static GpuBuffer radiusBuffer;

    public static boolean isAvailable() {
        return GpuRuntime.hasInt64();
    }

    /**
     * Pixel offsets must be grouped by (ring, channel, angle) bin, with binStart holding each bin's
     * first index and a final end index. Writes NaN scores for candidates outside the search radius.
     */
    public static synchronized void score(byte[] pixels, int[] binStart, int[] offsets, int width, int channels,
            int x0, int sub, int cols, int rows, int xSearch, int ySearch, int rSearchSq, int rDim,
            int angleDim, int mode, int r0, int minDiameter, float[] scores, int[] radii) {
        if (!isAvailable()) {
            throw new IllegalStateException("GPU with 64-bit integers is not available");
        }
        params = GpuCompute.scratch(params, 64);
        pixelBuffer = GpuCompute.scratch(pixelBuffer, pixels.length);
        scoreBuffer = GpuCompute.scratch(scoreBuffer, cols * rows * 4L);
        radiusBuffer = GpuCompute.scratch(radiusBuffer, cols * rows * 4L);
        GpuPipeline pipeline = GpuCompute.pipeline("circular_symmetry", new int[] { channels, mode, angleDim },
                Binding.Uniform, Binding.Storage, Binding.Storage, Binding.Storage, Binding.Storage, Binding.Storage);
        GpuProgram program = GpuCompute.program(pipeline, (cols + 7) / 8, (rows + 7) / 8, 1, params, pixelBuffer,
                GpuCompute.constant(binStart), GpuCompute.constant(offsets), scoreBuffer, radiusBuffer);
        ByteBuffer p = params.map();
        int[] values = { width, x0, sub, cols, rows, xSearch, ySearch, rSearchSq, rDim, r0, minDiameter };
        for (int i = 0; i < values.length; i++) {
            p.putInt(i * 4, values[i]);
        }
        pixelBuffer.map().put(pixels);
        GpuRuntime.await(program.submit(), GpuCompute.TIMEOUT_NS);
        scoreBuffer.map().asFloatBuffer().get(scores, 0, cols * rows);
        radiusBuffer.map().asIntBuffer().get(radii, 0, cols * rows);
    }
}
