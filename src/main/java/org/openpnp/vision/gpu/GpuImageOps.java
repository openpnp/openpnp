package org.openpnp.vision.gpu;

import java.util.Arrays;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.openpnp.vision.gpu.GpuPipeline.Binding;

/**
 * Vision pipeline operations on GpuImages, matching their OpenCV counterparts. Each records into the
 * thread's GpuRecording and returns a new image; nothing waits until someone needs a result.
 */
public class GpuImageOps {
    public enum ColorConversion {
        Bgr2Gray(Imgproc.COLOR_BGR2GRAY, 0, 3, 1),
        Rgb2Gray(Imgproc.COLOR_RGB2GRAY, 1, 3, 1),
        Gray2Bgr(Imgproc.COLOR_GRAY2BGR, 2, 1, 3),
        Gray2Rgb(Imgproc.COLOR_GRAY2RGB, 2, 1, 3),
        Bgr2Hsv(Imgproc.COLOR_BGR2HSV, 3, 3, 3),
        Bgr2HsvFull(Imgproc.COLOR_BGR2HSV_FULL, 4, 3, 3),
        Hsv2Bgr(Imgproc.COLOR_HSV2BGR, 5, 3, 3),
        Hsv2BgrFull(Imgproc.COLOR_HSV2BGR_FULL, 6, 3, 3);

        final int openCvCode;
        final int shaderCode;
        final int inChannels;
        final int outChannels;

        ColorConversion(int openCvCode, int shaderCode, int inChannels, int outChannels) {
            this.openCvCode = openCvCode;
            this.shaderCode = shaderCode;
            this.inChannels = inChannels;
            this.outChannels = outChannels;
        }

        public static ColorConversion of(int openCvCode) {
            for (ColorConversion c : values()) {
                if (c.openCvCode == openCvCode) {
                    return c;
                }
            }
            return null;
        }
    }

    private static int[] hsvTables;

    public static GpuImage maskCircle(GpuImage src, int cx, int cy, int radius, boolean invert) {
        int[] halfWidth = radius >= 0 ? circleHalfWidths(radius, Math.max(cy, src.rows() - cy)) : new int[1];
        return run("mask_circle", new int[] { src.channels() }, groups2d(src), src, src.type(),
                GpuCompute.constant(halfWidth), src.cols(), src.rows(), cx, cy, radius, invert ? 1 : 0);
    }

    public static GpuImage threshold(GpuImage src, double threshold, boolean invert) {
        int count = src.rows() * src.cols() * src.channels();
        return run("threshold", new int[0], new int[] { (count + 255) / 256, 1, 1 }, src, src.type(), null, count,
                (int) Math.floor(threshold), invert ? 1 : 0);
    }

    public static GpuImage gaussianBlur(GpuImage src, int kernelSize) {
        Mat kernel = Imgproc.getGaussianKernel(kernelSize, 0, CvType.CV_32F);
        float[] weights = new float[kernelSize];
        kernel.get(0, 0, weights);
        kernel.release();
        return run("gaussian_blur", new int[] { src.channels(), kernelSize }, groups2d(src), src, src.type(),
                GpuCompute.constant(weights), src.cols(), src.rows());
    }

    /**
     * Null when the conversion doesn't fit the image's channel count.
     */
    public static GpuImage convertColor(GpuImage src, ColorConversion conversion) {
        if (src.channels() != conversion.inChannels) {
            return null;
        }
        int pixels = src.rows() * src.cols();
        return run("convert_color", new int[] { conversion.shaderCode }, new int[] { (pixels + 255) / 256, 1, 1 },
                src, CvType.CV_8UC(conversion.outChannels), GpuCompute.constant(hsvTables()), pixels);
    }

    /**
     * Blackens pixels inside the HSV ranges (hue may wrap past 255), or all others when inverted;
     * with binaryMask returns the 0/255 mask of kept pixels instead.
     */
    public static GpuImage maskHsv(GpuImage src, int hueMin, int hueMax, int saturationMin, int saturationMax,
            int valueMin, int valueMax, boolean invert, boolean binaryMask) {
        int pixels = src.rows() * src.cols();
        return run("mask_hsv", new int[] { binaryMask ? 1 : 0 }, new int[] { (pixels + 255) / 256, 1, 1 }, src,
                binaryMask ? CvType.CV_8UC1 : CvType.CV_8UC3, null, pixels, hueMin, hueMax, saturationMin,
                saturationMax, valueMin, valueMax, invert ? 1 : 0);
    }

    /**
     * Per row of a gray image: the first and last column with a value in [low, high], or -1, then
     * the count and column sum of those pixels.
     */
    public static int[] rowExtremes(GpuImage src, int low, int high) {
        if (src.channels() != 1) {
            throw new IllegalArgumentException("row extremes need a gray image");
        }
        GpuPipeline pipeline = GpuCompute.pipeline("row_extremes", new int[0], Binding.Uniform, Binding.Storage,
                Binding.Storage);
        GpuRecording.Scratch extremes;
        try (GpuRecording recording = GpuRecording.open()) {
            extremes = recording.scratch(src.rows() * 16L, true);
            recording.dispatch(pipeline, (src.rows() + 63) / 64, 1, 1,
                    GpuRecording.params(src.cols(), src.rows(), low, high), src, extremes);
        }
        int[] values = new int[src.rows() * 4];
        extremes.read().asIntBuffer().get(values);
        return values;
    }

    private static int[] groups2d(GpuImage image) {
        return new int[] { (image.cols() + 15) / 16, (image.rows() + 15) / 16, 1 };
    }

    private static GpuImage run(String shader, int[] spec, int[] groups, GpuImage src, int dstType, GpuBuffer extra,
            int... params) {
        Binding[] bindings = extra == null
                ? new Binding[] { Binding.Uniform, Binding.Storage, Binding.Storage }
                : new Binding[] { Binding.Uniform, Binding.Storage, Binding.Storage, Binding.Storage };
        GpuPipeline pipeline = GpuCompute.pipeline(shader, spec, bindings);
        try (GpuRecording recording = GpuRecording.open()) {
            GpuImage dst = recording.image(src.rows(), src.cols(), dstType);
            Object[] args = extra == null ? new Object[] { src, dst } : new Object[] { src, dst, extra };
            recording.dispatch(pipeline, groups[0], groups[1], groups[2], GpuRecording.params(params), args);
            return dst;
        }
    }

    /**
     * The half width of each row OpenCV's midpoint algorithm fills for a filled circle, by row
     * distance from the center, up to maxRows.
     */
    static int[] circleHalfWidths(int radius, int maxRows) {
        int[] half = new int[Math.min(radius, maxRows) + 1];
        Arrays.fill(half, -1);
        int err = 0;
        int dx = radius;
        int dy = 0;
        int plus = 1;
        int minus = (radius << 1) - 1;
        while (dx >= dy) {
            if (dy < half.length) {
                half[dy] = Math.max(half[dy], dx);
            }
            if (dx < half.length) {
                half[dx] = Math.max(half[dx], dy);
            }
            dy++;
            err += plus;
            plus += 2;
            int mask = (err <= 0 ? 1 : 0) - 1;
            err -= minus & mask;
            dx += mask;
            minus -= mask & 2;
        }
        return half;
    }

    // OpenCV's RGB to HSV division tables: sdiv, hdiv for a 180 hue range, hdiv for 256.
    private static synchronized int[] hsvTables() {
        if (hsvTables == null) {
            hsvTables = new int[768];
            for (int i = 1; i < 256; i++) {
                hsvTables[i] = (int) Math.rint((255 << 12) / (1.0 * i));
                hsvTables[256 + i] = (int) Math.rint((180 << 12) / (6.0 * i));
                hsvTables[512 + i] = (int) Math.rint((256 << 12) / (6.0 * i));
            }
        }
        return hsvTables;
    }
}
