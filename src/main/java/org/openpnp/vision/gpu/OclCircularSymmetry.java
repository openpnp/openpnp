package org.openpnp.vision.gpu;

/**
 * Scores all candidate centers of one DetectCircularSymmetry search pass on the GPU.
 */
public class OclCircularSymmetry {
    private static native void score(byte[] pixels, int[] binStart, int[] offsets, int[] params,
            float[] scores, int[] radii);

    /**
     * Pixel offsets must be grouped by (ring, channel, angle) bin, with binStart holding each bin's
     * first index and a final end index. Writes NaN scores for candidates outside the search radius.
     */
    public static void score(byte[] pixels, int[] binStart, int[] offsets, int width, int channels,
            int x0, int sub, int cols, int rows, int xSearch, int ySearch, int rSearchSq, int rDim,
            int angleDim, int mode, int r0, int minDiameter, float[] scores, int[] radii) {
        if (!OclSupport.isAvailable()) {
            throw new IllegalStateException("OpenCL is not available");
        }
        score(pixels, binStart, offsets, new int[] { width, channels, x0, sub, cols, rows, xSearch,
                ySearch, rSearchSq, rDim, angleDim, mode, r0, minDiameter }, scores, radii);
    }
}
