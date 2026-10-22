package org.openpnp.vision.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.openpnp.vision.gpu.GpuImageOps.ColorConversion;

public class GpuImageOpsTest {
    @BeforeAll
    public static void setup() {
        nu.pattern.OpenCV.loadLocally();
        Assumptions.assumeTrue(GpuRuntime.hasByteStorage(), "no Vulkan GPU with 8-bit storage");
    }

    private static Mat colorImage() {
        Mat mat = new Mat(241, 323, CvType.CV_8UC3);
        Core.randu(mat, 0, 256);
        Imgproc.GaussianBlur(mat, mat, new Size(7, 7), 0);
        Imgproc.circle(mat, new Point(160, 120), 50, new Scalar(250, 20, 40), -1);
        Imgproc.rectangle(mat, new Point(10, 10), new Point(60, 80), new Scalar(128, 128, 128), -1);
        Imgproc.rectangle(mat, new Point(250, 150), new Point(300, 200), new Scalar(0, 0, 0), -1);
        return mat;
    }

    private static Mat grayImage() {
        Mat gray = new Mat();
        Imgproc.cvtColor(colorImage(), gray, Imgproc.COLOR_BGR2GRAY);
        return gray;
    }

    private static double maxDifference(Mat expected, GpuImage actual) {
        Mat result = actual.download();
        actual.release();
        assertEquals(expected.type(), result.type());
        assertEquals(expected.size(), result.size());
        Mat diff = new Mat();
        Core.absdiff(expected, result, diff);
        return Core.minMaxLoc(diff.reshape(1)).maxVal;
    }

    private static Mat cpuMaskCircle(Mat mat, int diameter) {
        Mat mask = Mat.zeros(mat.size(), mat.type());
        Mat masked = Mat.zeros(mat.size(), mat.type());
        if (diameter != 0) {
            Imgproc.circle(mask, new Point(mat.cols() * 0.5, mat.rows() * 0.5), Math.abs(diameter) / 2,
                    new Scalar(255, 255, 255), -1);
        }
        if (diameter < 0) {
            Core.bitwise_not(mask, mask);
        }
        mat.copyTo(masked, mask);
        return masked;
    }

    @Test
    public void maskCircleMatchesOpenCv() {
        for (Mat mat : new Mat[] { colorImage(), grayImage() }) {
            for (int diameter : new int[] { 0, 1, 2, 3, 57, 100, 200, -150, 100000 }) {
                GpuImage src = GpuImage.upload(mat);
                int radius = diameter == 0 ? -1 : Math.abs(diameter) / 2;
                GpuImage dst = GpuImageOps.maskCircle(src, (int) (mat.cols() * 0.5), (int) (mat.rows() * 0.5),
                        radius, diameter < 0);
                src.release();
                assertEquals(0, maxDifference(cpuMaskCircle(mat, diameter), dst), "diameter " + diameter);
            }
        }
    }

    @Test
    public void thresholdMatchesOpenCv() {
        for (Mat mat : new Mat[] { colorImage(), grayImage() }) {
            for (boolean invert : new boolean[] { false, true }) {
                Mat expected = new Mat();
                Imgproc.threshold(mat, expected, 100.7, 255, invert ? Imgproc.THRESH_BINARY_INV : Imgproc.THRESH_BINARY);
                GpuImage src = GpuImage.upload(mat);
                GpuImage dst = GpuImageOps.threshold(src, 100.7, invert);
                src.release();
                assertEquals(0, maxDifference(expected, dst));
            }
        }
    }

    @Test
    public void gaussianBlurWithinOneLevel() {
        for (Mat mat : new Mat[] { colorImage(), grayImage() }) {
            for (int kernelSize : new int[] { 3, 7, 9 }) {
                Mat expected = new Mat();
                Imgproc.GaussianBlur(mat, expected, new Size(kernelSize, kernelSize), 0);
                GpuImage src = GpuImage.upload(mat);
                GpuImage dst = GpuImageOps.gaussianBlur(src, kernelSize);
                src.release();
                assertTrue(maxDifference(expected, dst) <= 1, "kernel " + kernelSize);
            }
        }
    }

    @Test
    public void colorConversionsMatchOpenCv() {
        Mat color = colorImage();
        Mat hsvFull = new Mat();
        Imgproc.cvtColor(color, hsvFull, Imgproc.COLOR_BGR2HSV_FULL);
        Mat hsv = new Mat();
        Imgproc.cvtColor(color, hsv, Imgproc.COLOR_BGR2HSV);
        Object[][] cases = {
                { color, ColorConversion.Bgr2Gray, 0 },
                { color, ColorConversion.Rgb2Gray, 0 },
                { grayImage(), ColorConversion.Gray2Bgr, 0 },
                { color, ColorConversion.Bgr2Hsv, 0 },
                { color, ColorConversion.Bgr2HsvFull, 0 },
                { hsv, ColorConversion.Hsv2Bgr, 1 },
                { hsvFull, ColorConversion.Hsv2BgrFull, 1 },
        };
        for (Object[] c : cases) {
            Mat src = (Mat) c[0];
            ColorConversion conversion = (ColorConversion) c[1];
            Mat expected = new Mat();
            Imgproc.cvtColor(src, expected, conversion.openCvCode);
            GpuImage gpu = GpuImage.upload(src);
            GpuImage dst = GpuImageOps.convertColor(gpu, conversion);
            gpu.release();
            double diff = maxDifference(expected, dst);
            assertTrue(diff <= (int) c[2], conversion + " differs by " + diff);
        }
    }

    private static Mat cpuMaskHsv(Mat hsv, int[] r, boolean invert, boolean binary) {
        Mat mask = new Mat();
        if (r[0] <= r[1]) {
            Core.inRange(hsv, new Scalar(r[0], r[2], r[4]), new Scalar(r[1], r[3], r[5]), mask);
        }
        else {
            Mat mask2 = new Mat();
            Core.inRange(hsv, new Scalar(r[0], r[2], r[4]), new Scalar(255, r[3], r[5]), mask);
            Core.inRange(hsv, new Scalar(0, r[2], r[4]), new Scalar(r[1], r[3], r[5]), mask2);
            Core.bitwise_or(mask, mask2, mask);
        }
        if (!invert) {
            Core.bitwise_not(mask, mask);
        }
        if (binary) {
            return mask;
        }
        Mat masked = Mat.zeros(hsv.size(), hsv.type());
        hsv.copyTo(masked, mask);
        return masked;
    }

    @Test
    public void maskHsvMatchesOpenCv() {
        Mat hsv = new Mat();
        Imgproc.cvtColor(colorImage(), hsv, Imgproc.COLOR_BGR2HSV_FULL);
        int[][] ranges = { { 20, 200, 30, 255, 40, 255 }, { 230, 30, 0, 255, 0, 255 }, { 0, 255, 0, 40, 0, 255 } };
        for (int[] r : ranges) {
            for (boolean invert : new boolean[] { false, true }) {
                for (boolean binary : new boolean[] { false, true }) {
                    GpuImage src = GpuImage.upload(hsv);
                    GpuImage dst = GpuImageOps.maskHsv(src, r[0], r[1], r[2], r[3], r[4], r[5], invert, binary);
                    src.release();
                    assertEquals(0, maxDifference(cpuMaskHsv(hsv, r, invert, binary), dst));
                }
            }
        }
    }

    @Test
    public void chainedOpsNeedNoWaits() {
        Mat color = colorImage();
        Mat expected = new Mat();
        Imgproc.cvtColor(cpuMaskCircle(color, 200), expected, Imgproc.COLOR_BGR2GRAY);
        Imgproc.threshold(expected, expected, 90, 255, Imgproc.THRESH_BINARY);
        GpuImage src = GpuImage.upload(color);
        GpuImage masked = GpuImageOps.maskCircle(src, color.cols() / 2, color.rows() / 2, 100, false);
        GpuImage gray = GpuImageOps.convertColor(masked, ColorConversion.Bgr2Gray);
        GpuImage binary = GpuImageOps.threshold(gray, 90, false);
        src.release();
        masked.release();
        gray.release();
        assertEquals(0, maxDifference(expected, binary));
    }
}
