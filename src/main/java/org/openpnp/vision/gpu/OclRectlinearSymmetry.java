package org.openpnp.vision.gpu;

/**
 * Computes the rotated cross-sections of one DetectRectlinearSymmetry pass for all angles on the GPU.
 */
public class OclRectlinearSymmetry {
    private static native void crossSections(byte[] pixels, float[] gammaLut, float[] sines,
            float[] cosines, int[] params, float[] floatParams, float[] sums, float[] weights,
            float[] maskedWeights);

    /**
     * Outputs are indexed by angle, then bin (the wCross x-section bins followed by the hCross
     * y-section bins), then channel for the sums.
     */
    public static void crossSections(byte[] pixels, float[] gammaLut, float[] sines, float[] cosines,
            int width, int channels, int x0, int sub, int cols, int rows, int cxPixels, int cyPixels,
            int wCross, int hCross, float cxCross, float cyCross, float thresholdLuminance,
            float[] sums, float[] weights, float[] maskedWeights) {
        if (!OclSupport.isAvailable()) {
            throw new IllegalStateException("OpenCL is not available");
        }
        crossSections(pixels, gammaLut, sines, cosines,
                new int[] { width, channels, x0, sub, cols, rows, cxPixels, cyPixels, wCross, hCross,
                        sines.length },
                new float[] { cxCross, cyCross, thresholdLuminance }, sums, weights, maskedWeights);
    }
}
