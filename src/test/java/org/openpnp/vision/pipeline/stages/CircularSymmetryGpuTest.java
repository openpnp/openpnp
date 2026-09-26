package org.openpnp.vision.pipeline.stages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.openpnp.vision.gpu.GpuCircularSymmetry;
import org.openpnp.vision.gpu.GpuImage;
import org.openpnp.vision.gpu.GpuRecording;
import org.openpnp.vision.gpu.GpuRuntime;
import org.openpnp.vision.pipeline.CvStage.Result.Circle;
import org.openpnp.vision.pipeline.stages.DetectCircularSymmetry.ScoreRange;
import org.openpnp.vision.pipeline.stages.DetectCircularSymmetry.SymmetryCircle;
import org.openpnp.vision.pipeline.stages.DetectCircularSymmetry.SymmetryScore;

public class CircularSymmetryGpuTest {
    @BeforeAll
    public static void setup() {
        nu.pattern.OpenCV.loadLocally();
    }

    @AfterEach
    public void restore() {
        DetectCircularSymmetry.gpuEnabled = true;
        DetectCircularSymmetry.gpuMinWork = 1_000_000;
    }

    private static Mat testImage(int channels) {
        Mat mat = new Mat(480, 640, CvType.CV_8UC3, new Scalar(40, 60, 50));
        Imgproc.circle(mat, new Point(250.3, 210.7), 60, new Scalar(200, 180, 190), 12);
        Imgproc.circle(mat, new Point(470, 330), 45, new Scalar(160, 200, 90), -1);
        Imgproc.circle(mat, new Point(470, 330), 20, new Scalar(20, 30, 40), -1);
        Imgproc.rectangle(mat, new Rect(20, 360, 140, 100), new Scalar(255, 255, 255), -1);
        Mat noise = new Mat(mat.size(), mat.type());
        Core.randn(noise, 0, 6);
        Core.add(mat, noise, mat);
        Imgproc.GaussianBlur(mat, mat, new Size(5, 5), 0);
        if (channels == 1) {
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);
        }
        return mat;
    }

    private static List<Circle> find(Mat image, boolean gpu, SymmetryScore mode, int maxTargets,
            int superSampling) throws Exception {
        DetectCircularSymmetry.gpuEnabled = gpu;
        DetectCircularSymmetry.gpuMinWork = 0;
        return DetectCircularSymmetry.findCircularSymmetry(image, 320, 240, 90, 150, 500, 600, 440,
                maxTargets, 1.2, 0.0, 8, superSampling, mode, false, false, new ScoreRange());
    }

    @Test
    public void gpuMatchesCpu() throws Exception {
        Assumptions.assumeTrue(GpuCircularSymmetry.isAvailable());
        for (int channels : new int[] { 1, 3 }) {
            Mat image = testImage(channels);
            for (SymmetryScore mode : SymmetryScore.values()) {
                for (int maxTargets : new int[] { 1, 3 }) {
                    for (int superSampling : new int[] { 1, 4 }) {
                        List<Circle> cpu = find(image, false, mode, maxTargets, superSampling);
                        List<Circle> gpu = find(image, true, mode, maxTargets, superSampling);
                        assertTrue(DetectCircularSymmetry.gpuEnabled, "GPU path failed");
                        assertFalse(cpu.isEmpty());
                        assertSame(cpu, gpu, channels + " channels, " + mode + ", " + maxTargets
                                + " targets, superSampling " + superSampling);
                    }
                }
            }
        }
    }

    @Test
    public void flatSaturatedAreaHasNoSymmetry() throws Exception {
        Assumptions.assumeTrue(GpuCircularSymmetry.isAvailable());
        Mat image = new Mat(300, 300, CvType.CV_8UC1, new Scalar(255));
        for (SymmetryScore mode : SymmetryScore.values()) {
            List<Circle> gpu = DetectCircularSymmetry.findCircularSymmetry(image, 150, 150, 40, 60, 100,
                    100, 100, 1, 1.2, 0.0, 8, 1, mode, false, false, new ScoreRange());
            DetectCircularSymmetry.gpuEnabled = false;
            List<Circle> cpu = DetectCircularSymmetry.findCircularSymmetry(image, 150, 150, 40, 60, 100,
                    100, 100, 1, 1.2, 0.0, 8, 1, mode, false, false, new ScoreRange());
            DetectCircularSymmetry.gpuEnabled = true;
            assertEquals(cpu.size(), gpu.size(), mode + ": cpu " + cpu + " gpu " + gpu);
        }
    }

    @Test
    public void randomImagesMatch() throws Exception {
        Assumptions.assumeTrue(GpuCircularSymmetry.isAvailable());
        Random random = new Random(3);
        for (int i = 0; i < 6; i++) {
            Mat image = new Mat(200, 260, CvType.CV_8UC3);
            Core.randu(image, 0, 256);
            Imgproc.GaussianBlur(image, image, new Size(9, 9), 0);
            SymmetryScore mode = SymmetryScore.values()[i % 3];
            int x = 100 + random.nextInt(60);
            int y = 80 + random.nextInt(40);
            DetectCircularSymmetry.gpuMinWork = 0;
            DetectCircularSymmetry.gpuEnabled = false;
            List<Circle> cpu = DetectCircularSymmetry.findCircularSymmetry(image, x, y, 20, 40, 80, 80,
                    80, 2, 0.5, 0.0, 4, 2, mode, false, false, new ScoreRange());
            DetectCircularSymmetry.gpuEnabled = true;
            List<Circle> gpu = DetectCircularSymmetry.findCircularSymmetry(image, x, y, 20, 40, 80, 80,
                    80, 2, 0.5, 0.0, 4, 2, mode, false, false, new ScoreRange());
            assertSame(cpu, gpu, mode + " at " + x + "," + y);
        }
    }

    private static List<Circle> search(Mat image, int x, int y, int minDiameter, int maxDiameter, int searchDiameter,
            int searchWidth, int searchHeight, int subSampling, int superSampling, SymmetryScore mode,
            boolean diagnostics, Mat painted) throws Exception {
        GpuImage src = GpuImage.upload(image);
        GpuCircularSymmetry.Search search;
        try (GpuRecording recording = GpuRecording.open()) {
            search = GpuCircularSymmetry.search(src, x, y, minDiameter, maxDiameter, searchDiameter, searchWidth,
                    searchHeight, 1.2, subSampling, superSampling, mode.ordinal(), mode.getAngularBins(),
                    diagnostics, diagnostics);
        }
        List<Circle> circles = new ArrayList<>();
        double[] found = search.result();
        if (found != null) {
            circles.add(new SymmetryCircle(found[0], found[1], found[2], found[3]));
        }
        if (search.getDiagnosticImage() != null) {
            search.getDiagnosticImage().download().copyTo(painted);
            search.getDiagnosticImage().release();
        }
        src.release();
        return circles;
    }

    @Test
    public void recordedSearchMatchesCpuDrivenGpuSearch() throws Exception {
        Assumptions.assumeTrue(GpuCircularSymmetry.isAvailable() && GpuRuntime.hasByteStorage());
        for (int channels : new int[] { 1, 3 }) {
            Mat image = testImage(channels);
            for (SymmetryScore mode : SymmetryScore.values()) {
                for (int superSampling : new int[] { 1, 3, 8 }) {
                    for (int[] geometry : new int[][] { { 320, 240, 90, 150, 500, 600, 440, 8 },
                            { 250, 212, 100, 150, 30, 30, 30, 8 }, { 470, 330, 30, 50, 60, 60, 60, 4 },
                            { 40, 30, 20, 40, 200, 200, 200, 8 } }) {
                        DetectCircularSymmetry.gpuMinWork = 0;
                        List<Circle> expected = DetectCircularSymmetry.findCircularSymmetry(image, geometry[0],
                                geometry[1], geometry[2], geometry[3], geometry[4], geometry[5], geometry[6], 1, 1.2,
                                0.0, geometry[7], superSampling, mode, false, false, new ScoreRange());
                        List<Circle> recorded = search(image, geometry[0], geometry[1], geometry[2], geometry[3],
                                geometry[4], geometry[5], geometry[6], geometry[7], superSampling, mode, false, null);
                        assertSame(expected, recorded, channels + " channels, " + mode + ", superSampling "
                                + superSampling + " at " + geometry[0] + "," + geometry[1]);
                    }
                }
            }
        }
    }

    @Test
    public void recordedSearchPaintsDiagnosticsLikeCpu() throws Exception {
        Assumptions.assumeTrue(GpuCircularSymmetry.isAvailable() && GpuRuntime.hasByteStorage());
        for (int channels : new int[] { 1, 3 }) {
            for (int[] geometry : new int[][] { { 320, 240, 90, 150 }, { 320, 240, 280, 300 } }) {
                Mat image = testImage(channels);
                Mat cpu = image.clone();
                DetectCircularSymmetry.gpuMinWork = 0;
                List<Circle> expected = DetectCircularSymmetry.findCircularSymmetry(cpu, geometry[0], geometry[1],
                        geometry[2], geometry[3], 500, 600, 440, 1, 1.2, 0.0, 8, 1, SymmetryScore.OverallVarianceVsRingVarianceSum,
                        true, true, new ScoreRange());
                Mat painted = new Mat();
                List<Circle> recorded = search(image, geometry[0], geometry[1], geometry[2], geometry[3], 500, 600,
                        440, 8, 1, SymmetryScore.OverallVarianceVsRingVarianceSum, true, painted);
                assertSame(expected, recorded, channels + " channels");
                Mat diff = new Mat();
                Core.absdiff(cpu, painted, diff);
                assertTrue(Core.minMaxLoc(diff.reshape(1)).maxVal <= 2, channels + " channels");
                assertTrue(Core.mean(diff.reshape(1)).val[0] < 0.01, channels + " channels");
                assertTrue(Core.countNonZero(changed(image, cpu)) > 1000);
            }
        }
    }

    private static Mat changed(Mat a, Mat b) {
        Mat diff = new Mat();
        Core.absdiff(a, b, diff);
        return diff.reshape(1);
    }

    @Test
    public void recordedSearchReportsCroppedRange() throws Exception {
        Assumptions.assumeTrue(GpuCircularSymmetry.isAvailable() && GpuRuntime.hasByteStorage());
        Mat image = testImage(1);
        assertThrows(Exception.class, () -> search(image, 320, 240, 400, 500, 50, 50, 50, 8, 1,
                SymmetryScore.OverallVarianceVsRingVarianceSum, false, null));
    }

    private static void assertSame(List<Circle> cpu, List<Circle> gpu, String what) {
        assertTrue(DetectCircularSymmetry.gpuEnabled, "GPU path failed");
        what += ": cpu " + cpu + " gpu " + gpu;
        assertEquals(cpu.size(), gpu.size(), what);
        for (int i = 0; i < cpu.size(); i++) {
            SymmetryCircle c = (SymmetryCircle) cpu.get(i);
            SymmetryCircle g = (SymmetryCircle) gpu.get(i);
            assertEquals(c.x, g.x, 1e-9, what);
            assertEquals(c.y, g.y, 1e-9, what);
            assertEquals(c.diameter, g.diameter, 1e-9, what);
            assertEquals(c.score, g.score, Math.abs(c.score) * 1e-4, what);
        }
    }
}
