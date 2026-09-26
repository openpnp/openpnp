package org.openpnp.vision.gpu;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.openpnp.vision.gpu.GpuPipeline.Binding;

/**
 * Vision pipeline operations on GpuImages, matching their OpenCV counterparts. Each returns a new
 * image right after submitting; nothing waits until someone downloads a result.
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

    private static final GpuCache<List<Object>, Op> ops = new GpuCache<>(2, TimeUnit.MINUTES);
    private static int[] hsvTables;

    // A recorded dispatch with its own parameters, which may only be rewritten once the GPU ran it.
    private static final class Op implements GpuResource {
        final GpuProgram program;
        final GpuBuffer params;
        long lastSubmit;

        Op(GpuProgram program, GpuBuffer params) {
            this.program = program;
            this.params = params;
        }

        @Override
        public boolean isClosed() {
            return program.isClosed();
        }

        @Override
        public void close() {
            program.close();
            params.close();
        }
    }

    public static GpuImage maskCircle(GpuImage src, int cx, int cy, int radius, boolean invert) {
        int[] halfWidth = radius >= 0 ? circleHalfWidths(radius, Math.max(cy, src.rows() - cy)) : new int[1];
        GpuImage dst = GpuImage.allocate(src.rows(), src.cols(), src.type());
        run("mask_circle", new int[] { src.channels() }, groups2d(src), src, dst, GpuCompute.constant(halfWidth),
                src.cols(), src.rows(), cx, cy, radius, invert ? 1 : 0);
        return dst;
    }

    public static GpuImage threshold(GpuImage src, double threshold, boolean invert) {
        int count = src.rows() * src.cols() * src.channels();
        GpuImage dst = GpuImage.allocate(src.rows(), src.cols(), src.type());
        run("threshold", new int[0], new int[] { (count + 255) / 256, 1, 1 }, src, dst, null, count,
                (int) Math.floor(threshold), invert ? 1 : 0);
        return dst;
    }

    public static GpuImage gaussianBlur(GpuImage src, int kernelSize) {
        Mat kernel = Imgproc.getGaussianKernel(kernelSize, 0, CvType.CV_32F);
        float[] weights = new float[kernelSize];
        kernel.get(0, 0, weights);
        kernel.release();
        GpuImage dst = GpuImage.allocate(src.rows(), src.cols(), src.type());
        run("gaussian_blur", new int[] { src.channels(), kernelSize }, groups2d(src), src, dst,
                GpuCompute.constant(weights), src.cols(), src.rows());
        return dst;
    }

    /**
     * Null when the conversion doesn't fit the image's channel count.
     */
    public static GpuImage convertColor(GpuImage src, ColorConversion conversion) {
        if (src.channels() != conversion.inChannels) {
            return null;
        }
        int pixels = src.rows() * src.cols();
        GpuImage dst = GpuImage.allocate(src.rows(), src.cols(), CvType.CV_8UC(conversion.outChannels));
        run("convert_color", new int[] { conversion.shaderCode }, new int[] { (pixels + 255) / 256, 1, 1 }, src, dst,
                GpuCompute.constant(hsvTables()), pixels);
        return dst;
    }

    /**
     * Blackens pixels inside the HSV ranges (hue may wrap past 255), or all others when inverted;
     * with binaryMask returns the 0/255 mask of kept pixels instead.
     */
    public static GpuImage maskHsv(GpuImage src, int hueMin, int hueMax, int saturationMin, int saturationMax,
            int valueMin, int valueMax, boolean invert, boolean binaryMask) {
        int pixels = src.rows() * src.cols();
        GpuImage dst = GpuImage.allocate(src.rows(), src.cols(), binaryMask ? CvType.CV_8UC1 : CvType.CV_8UC3);
        run("mask_hsv", new int[] { binaryMask ? 1 : 0 }, new int[] { (pixels + 255) / 256, 1, 1 }, src, dst, null,
                pixels, hueMin, hueMax, saturationMin, saturationMax, valueMin, valueMax, invert ? 1 : 0);
        return dst;
    }

    private static int[] groups2d(GpuImage image) {
        return new int[] { (image.cols() + 15) / 16, (image.rows() + 15) / 16, 1 };
    }

    private static void run(String shader, int[] spec, int[] groups, GpuImage src, GpuImage dst, GpuBuffer extra,
            int... params) {
        Binding[] bindings = extra == null
                ? new Binding[] { Binding.Uniform, Binding.Storage, Binding.Storage }
                : new Binding[] { Binding.Uniform, Binding.Storage, Binding.Storage, Binding.Storage };
        GpuPipeline pipeline = GpuCompute.pipeline(shader, spec, bindings);
        GpuBuffer in = src.buffer();
        GpuBuffer out = dst.buffer();
        Op op = ops.get(Arrays.asList(pipeline, groups[0], groups[1], groups[2], in, out, extra), k -> {
            GpuBuffer paramBuffer = new GpuBuffer(64, true);
            GpuBuffer[] buffers = extra == null ? new GpuBuffer[] { paramBuffer, in, out }
                    : new GpuBuffer[] { paramBuffer, in, out, extra };
            return new Op(new GpuProgram.Builder().dispatch(pipeline, groups[0], groups[1], groups[2], buffers)
                    .build(), paramBuffer);
        });
        synchronized (op) {
            if (op.lastSubmit > 0) {
                GpuRuntime.await(op.lastSubmit, GpuCompute.TIMEOUT_NS);
            }
            ByteBuffer p = op.params.map();
            for (int i = 0; i < params.length; i++) {
                p.putInt(i * 4, params[i]);
            }
            op.lastSubmit = op.program.submit();
        }
        src.readBy(op.lastSubmit);
        dst.writtenBy(op.lastSubmit);
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
