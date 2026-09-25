package org.openpnp.machine.reference.camera;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.openpnp.model.Configuration;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.vision.gpu.OclSupport;

public class CameraTransformGpuTest {
    static class TestCamera extends ImageCamera {
    }

    @BeforeAll
    public static void setup() throws Exception {
        nu.pattern.OpenCV.loadLocally();
        File dir = Files.createTempDirectory("camera-transform-test").toFile();
        Configuration.initialize(dir);
    }

    private static BufferedImage testImage() {
        Mat mat = new Mat(480, 640, CvType.CV_8UC3);
        byte[] data = new byte[480 * 640 * 3];
        Random random = new Random(1);
        for (int y = 0, i = 0; y < 480; y++) {
            for (int x = 0; x < 640; x++) {
                boolean square = ((x / 40) + (y / 40)) % 2 == 0;
                data[i++] = (byte) (x * 255 / 640);
                data[i++] = (byte) (y * 255 / 480);
                data[i++] = (byte) ((square ? 200 : 40) + random.nextInt(16));
            }
        }
        mat.put(0, 0, data);
        Imgproc.GaussianBlur(mat, mat, new Size(3, 3), 0);
        BufferedImage image = OpenCvUtils.toBufferedImage(mat);
        mat.release();
        return image;
    }

    private static TestCamera cpuCamera(Consumer<TestCamera> settings) {
        TestCamera camera = camera(settings);
        camera.gpuTransforms = false;
        return camera;
    }

    private static TestCamera camera(Consumer<TestCamera> settings) {
        TestCamera camera = new TestCamera();
        settings.accept(camera);
        return camera;
    }

    private static BufferedImage compositeRemap(TestCamera camera, BufferedImage image) {
        Mat[] maps = camera.buildTransformMaps(image.getWidth(), image.getHeight(), false);
        Mat fixedXY = new Mat();
        Mat fixedFraction = new Mat();
        Imgproc.convertMaps(maps[0], maps[1], fixedXY, fixedFraction, CvType.CV_16SC2, false);
        Mat src = OpenCvUtils.toMat(image);
        Mat dst = new Mat();
        Imgproc.remap(src, dst, fixedXY, fixedFraction, Imgproc.INTER_LINEAR, Core.BORDER_CONSTANT);
        return OpenCvUtils.toBufferedImage(dst);
    }

    private static void assertSimilar(BufferedImage expected, BufferedImage actual) {
        assertEquals(expected.getWidth(), actual.getWidth());
        assertEquals(expected.getHeight(), actual.getHeight());
        Mat a = OpenCvUtils.toMat(expected);
        Mat b = OpenCvUtils.toMat(actual);
        Mat diff = new Mat();
        Core.absdiff(a, b, diff);
        Mat gray = diff.reshape(1);
        double mean = Core.mean(gray).val[0];
        Mat large = new Mat();
        Imgproc.threshold(gray, large, 8, 255, Imgproc.THRESH_BINARY);
        double largeFraction = Core.countNonZero(large) / (double) gray.total();
        assertTrue(mean < 1.0, "mean difference " + mean);
        assertTrue(largeFraction < 0.01, "fraction of pixels off by more than 8: " + largeFraction);
    }

    private static Consumer<TestCamera> rotateFlip = c -> {
        c.setRotation(0.37);
        c.setFlipX(true);
        c.setFlipY(true);
    };

    private static Consumer<TestCamera> cropScaleOffset = c -> {
        c.setCropWidth(500);
        c.setCropHeight(400);
        c.setScaleWidth(800);
        c.setScaleHeight(600);
        c.setOffsetX(7);
        c.setOffsetY(-5);
    };

    private static Consumer<TestCamera> undistort = c -> {
        Mat cameraMatrix = Mat.eye(3, 3, CvType.CV_64F);
        cameraMatrix.put(0, 0, 600);
        cameraMatrix.put(1, 1, 600);
        cameraMatrix.put(0, 2, 320);
        cameraMatrix.put(1, 2, 240);
        Mat distortion = new Mat(1, 5, CvType.CV_64F);
        distortion.put(0, 0, -0.2, 0.05, 0, 0, 0);
        c.getCalibration().setCameraMatrixMat(cameraMatrix);
        c.getCalibration().setDistortionCoefficientsMat(distortion);
        c.getCalibration().setEnabled(true);
        c.setRotation(-0.11);
        c.setFlipX(true);
    };

    private static Consumer<TestCamera> deinterlaceRotate = c -> {
        c.setDeinterlace(true);
        c.setRotation(12);
    };

    @Test
    public void compositeMapMatchesCpuChain() {
        BufferedImage image = testImage();
        for (Consumer<TestCamera> settings : List.of(rotateFlip, cropScaleOffset, undistort)) {
            assertSimilar(cpuCamera(settings).transformImage(image), compositeRemap(camera(settings), image));
        }
    }

    @Test
    public void gpuMatchesCpu() {
        Assumptions.assumeTrue(OclSupport.isAvailable());
        BufferedImage image = testImage();
        Consumer<TestCamera> whiteBalance = c -> {
            c.setRedBalance(1.2);
            c.setBlueGamma(0.8);
            c.setRotation(0.37);
        };
        Consumer<TestCamera> whiteBalanceOnly = c -> c.setGreenBalance(0.9);
        for (Consumer<TestCamera> settings : List.of(rotateFlip, cropScaleOffset, undistort, deinterlaceRotate, whiteBalance, whiteBalanceOnly)) {
            BufferedImage cpu = cpuCamera(settings).transformImage(image);
            TestCamera gpu = camera(settings);
            assertSimilar(cpu, gpu.transformImage(image));
            assertSimilar(cpu, gpu.transformImage(image));
        }
    }
}
