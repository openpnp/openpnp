package org.openpnp.vision.pipeline.stages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.openpnp.vision.gpu.OclSupport;
import org.openpnp.vision.pipeline.stages.DetectRectlinearSymmetry.ScoreRange;
import org.openpnp.vision.pipeline.stages.DetectRectlinearSymmetry.SymmetryFunction;

public class RectlinearSymmetryGpuTest {
    @BeforeAll
    public static void setup() {
        nu.pattern.OpenCV.loadLocally();
    }

    @AfterEach
    public void restore() {
        DetectRectlinearSymmetry.gpuEnabled = true;
    }

    private static Mat part(int channels, double angle) {
        Mat mat = new Mat(600, 800, CvType.CV_8UC3, new Scalar(25, 30, 20));
        Point[] body = new Point[4];
        new RotatedRect(new Point(410.3, 297.6), new Size(160, 100), -angle).points(body);
        Imgproc.fillConvexPoly(mat, new MatOfPoint(body), new Scalar(210, 200, 190));
        Point[] pad = new Point[4];
        new RotatedRect(new Point(410.3, 297.6), new Size(96, 110), -angle).points(pad);
        Imgproc.fillConvexPoly(mat, new MatOfPoint(pad), new Scalar(110, 130, 120));
        Mat noise = new Mat(mat.size(), mat.type());
        Core.randn(noise, 0, 5);
        Core.add(mat, noise, mat);
        Imgproc.GaussianBlur(mat, mat, new Size(3, 3), 0);
        if (channels == 1) {
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);
        }
        return mat;
    }

    private static RotatedRect find(Mat image, boolean gpu, double expectedAngle, SymmetryFunction function,
            int superSampling) throws Exception {
        DetectRectlinearSymmetry.gpuEnabled = gpu;
        return DetectRectlinearSymmetry.findReclinearSymmetry(image.clone(), 400, 300, expectedAngle, 260,
                260, 60, 30, 2, function, function, 20, 8, superSampling, 5, 2.5, 128, false, false,
                new ScoreRange());
    }

    @Test
    public void gpuMatchesCpu() throws Exception {
        Assumptions.assumeTrue(OclSupport.isAvailable());
        for (int channels : new int[] { 1, 3 }) {
            for (double angle : new double[] { 7, 90, 222 }) {
                Mat image = part(channels, angle);
                for (SymmetryFunction function : SymmetryFunction.values()) {
                    for (int superSampling : new int[] { 1, 2 }) {
                        RotatedRect cpu = find(image, false, Math.round(angle / 45) * 45, function, superSampling);
                        RotatedRect gpu = find(image, true, Math.round(angle / 45) * 45, function, superSampling);
                        assertTrue(DetectRectlinearSymmetry.gpuEnabled, "GPU path failed");
                        String what = channels + " channels, angle " + angle + ", " + function
                                + ", superSampling " + superSampling + ": cpu " + cpu + " gpu " + gpu;
                        if (cpu == null) {
                            assertEquals(null, gpu, what);
                            continue;
                        }
                        assertNotNull(gpu, what);
                        assertEquals(cpu.center.x, gpu.center.x, 0.25, what);
                        assertEquals(cpu.center.y, gpu.center.y, 0.25, what);
                        assertEquals(cpu.size.width, gpu.size.width, 2, what);
                        assertEquals(cpu.size.height, gpu.size.height, 2, what);
                        assertEquals(cpu.angle, gpu.angle, 0.2, what);
                    }
                }
            }
        }
    }
}
