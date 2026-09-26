package org.openpnp.vision.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.openpnp.machine.reference.camera.ImageCamera;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Camera.SettleOption;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.vision.pipeline.CvStage.Result.Circle;
import org.openpnp.vision.gpu.GpuBuffer;
import org.openpnp.vision.gpu.GpuCameraTransform;
import org.openpnp.vision.gpu.GpuCameraTransform.Input;
import org.openpnp.vision.gpu.GpuCircularSymmetry;
import org.openpnp.vision.gpu.GpuImage;
import org.openpnp.vision.gpu.GpuRuntime;

public class GpuPipelineTest {
    private static BufferedImage frame;

    static class FixedCamera extends ImageCamera {
        @Override
        public BufferedImage settleAndCapture(SettleOption settleOption) {
            return frame;
        }

        @Override
        public void actuateLightBeforeCapture(Object light) {
        }

        @Override
        public void actuateLightAfterCapture() {
        }
    }

    // Hands the frame to the pipeline the way a V4L2 camera does: as a transform recorded into
    // the pipeline's command buffer.
    static class GpuFixedCamera extends FixedCamera {
        private final GpuCameraTransform transform = new GpuCameraTransform();
        private final GpuBuffer upload;

        GpuFixedCamera() {
            byte[] data = ((DataBufferByte) frame.getRaster().getDataBuffer()).getData();
            upload = new GpuBuffer(data.length, true);
            upload.map().put(data);
        }

        @Override
        public GpuImage settleAndCaptureGpu(SettleOption settleOption) {
            return transform.record(transform.slots(upload), 0, Input.Bgr, frame.getWidth(), frame.getHeight(),
                    frame.getWidth() * 3);
        }
    }

    private static final String FIDUCIAL = "<cv-pipeline><stages>"
            + "<cv-stage class=\"org.openpnp.vision.pipeline.stages.ImageCapture\" name=\"image\" enabled=\"true\" default-light=\"true\" settle-option=\"Settle\" count=\"1\"/>"
            + "<cv-stage class=\"org.openpnp.vision.pipeline.stages.MaskCircle\" name=\"mask\" enabled=\"true\" diameter=\"900\" property-name=\"MaskCircle\"/>"
            + "<cv-stage class=\"org.openpnp.vision.pipeline.stages.DetectCircularSymmetry\" name=\"cir\" enabled=\"true\" min-diameter=\"100\" max-diameter=\"140\" max-distance=\"350\" search-width=\"0\" search-height=\"0\" max-target-count=\"1\" min-symmetry=\"1.2\" corr-symmetry=\"0.0\" outer-margin=\"0.2\" inner-margin=\"0.4\" sub-sampling=\"8\" super-sampling=\"4\" symmetry-score=\"OverallVarianceVsRingVarianceSum\" property-name=\"\" diagnostics=\"true\" heat-map=\"true\"/>"
            + "<cv-stage class=\"org.openpnp.vision.pipeline.stages.ConvertModelToKeyPoints\" name=\"results\" enabled=\"true\" model-stage-name=\"cir\"/>"
            + "</stages></cv-pipeline>";

    private static final String BOTTOM_VISION = "<cv-pipeline><stages>"
            + "<cv-stage class=\"org.openpnp.vision.pipeline.stages.ImageCapture\" name=\"CaptureImage\" enabled=\"true\" default-light=\"true\" settle-option=\"Settle\" count=\"1\"/>"
            + "<cv-stage class=\"org.openpnp.vision.pipeline.stages.MaskCircle\" name=\"MaskCircle\" enabled=\"true\" diameter=\"1000\" property-name=\"\"/>"
            + "<cv-stage class=\"org.openpnp.vision.pipeline.stages.ConvertColor\" name=\"ConvertToGray\" enabled=\"true\" conversion=\"Bgr2Gray\"/>"
            + "<cv-stage class=\"org.openpnp.vision.pipeline.stages.BlurGaussian\" name=\"InitialBlur\" enabled=\"true\" kernel-size=\"9\" property-name=\"BlurGaussian\"/>"
            + "<cv-stage class=\"org.openpnp.vision.pipeline.stages.Threshold\" name=\"Threshold\" enabled=\"true\" threshold=\"150\" auto=\"false\" invert=\"false\"/>"
            + "<cv-stage class=\"org.openpnp.vision.pipeline.stages.BlurGaussian\" name=\"SecondBlur\" enabled=\"true\" kernel-size=\"3\" property-name=\"BlurGaussian\"/>"
            + "<cv-stage class=\"org.openpnp.vision.pipeline.stages.MinAreaRect\" name=\"results\" enabled=\"true\" threshold-min=\"149\" threshold-max=\"256\" expected-angle=\"0.0\" search-angle=\"45.0\" left-edge=\"true\" right-edge=\"true\" top-edge=\"true\" bottom-edge=\"true\" diagnostics=\"false\" property-name=\"MinAreaRect\"/>"
            + "<cv-stage class=\"org.openpnp.vision.pipeline.stages.ImageRecall\" name=\"RecallOriginal\" enabled=\"true\" image-stage-name=\"CaptureImage\"/>"
            + "<cv-stage class=\"org.openpnp.vision.pipeline.stages.DrawRotatedRects\" name=\"DrawRectangle\" enabled=\"true\" rotated-rects-stage-name=\"results\" thickness=\"2\" draw-rect-center=\"false\" rect-center-radius=\"20\" show-orientation=\"false\"/>"
            + "</stages></cv-pipeline>";

    private static String hsvPipeline(int softEdge) {
        return "<cv-pipeline><stages>"
                + "<cv-stage class=\"org.openpnp.vision.pipeline.stages.ImageCapture\" name=\"CaptureImage\" enabled=\"true\" default-light=\"true\" settle-option=\"Settle\" count=\"1\"/>"
                + "<cv-stage class=\"org.openpnp.vision.pipeline.stages.BlurGaussian\" name=\"Blur\" enabled=\"true\" kernel-size=\"7\" property-name=\"BlurGaussian\"/>"
                + "<cv-stage class=\"org.openpnp.vision.pipeline.stages.MaskCircle\" name=\"MaskCircle\" enabled=\"true\" diameter=\"900\" property-name=\"MaskCircle\"/>"
                + "<cv-stage class=\"org.openpnp.vision.pipeline.stages.ConvertColor\" name=\"ToHsv\" enabled=\"true\" conversion=\"Bgr2HsvFull\"/>"
                + "<cv-stage class=\"org.openpnp.vision.pipeline.stages.MaskHsv\" name=\"MaskHsv\" enabled=\"true\" auto=\"false\" fraction-to-mask=\"0.0\" hue-min=\"80\" hue-max=\"150\" saturation-min=\"64\" saturation-max=\"255\" value-min=\"0\" value-max=\"225\" soft-edge=\"" + softEdge + "\" soft-factor=\"" + (softEdge > 0 ? "0.75" : "1.0") + "\" invert=\"false\" binary-mask=\"false\" property-name=\"MaskHsv\"/>"
                + "<cv-stage class=\"org.openpnp.vision.pipeline.stages.ConvertColor\" name=\"ToBgr\" enabled=\"true\" conversion=\"Hsv2BgrFull\"/>"
                + "<cv-stage class=\"org.openpnp.vision.pipeline.stages.ConvertColor\" name=\"ToGray\" enabled=\"true\" conversion=\"Bgr2Gray\"/>"
                + "<cv-stage class=\"org.openpnp.vision.pipeline.stages.Threshold\" name=\"Threshold\" enabled=\"true\" threshold=\"60\" auto=\"false\" invert=\"false\"/>"
                + "<cv-stage class=\"org.openpnp.vision.pipeline.stages.MinAreaRect\" name=\"results\" enabled=\"true\" threshold-min=\"100\" threshold-max=\"256\" expected-angle=\"0.0\" search-angle=\"45.0\" left-edge=\"true\" right-edge=\"true\" top-edge=\"true\" bottom-edge=\"true\" diagnostics=\"false\" property-name=\"MinAreaRect\"/>"
                + "</stages></cv-pipeline>";
    }

    @BeforeAll
    public static void setup() throws Exception {
        nu.pattern.OpenCV.loadLocally();
        Configuration.initialize(Files.createTempDirectory("gpu-pipeline-test").toFile());
        Mat mat = new Mat(1080, 1920, CvType.CV_8UC3, new Scalar(40, 35, 30));
        Imgproc.circle(mat, new Point(960, 540), 300, new Scalar(90, 60, 30), -1);
        RotatedRect part = new RotatedRect(new Point(955, 548), new Size(260, 140), 12.5);
        Point[] corners = new Point[4];
        part.points(corners);
        Imgproc.fillConvexPoly(mat, new org.opencv.core.MatOfPoint(corners), new Scalar(220, 225, 230));
        Imgproc.circle(mat, new Point(700, 400), 60, new Scalar(200, 60, 20), -1);
        Mat noise = new Mat(mat.size(), mat.type());
        Core.randn(noise, 0, 6);
        Core.add(mat, noise, mat);
        frame = OpenCvUtils.toBufferedImage(mat);
    }

    @AfterEach
    public void restore() {
        CvPipeline.gpuEnabled = true;
    }

    private static CvPipeline run(String xml, boolean gpu) throws Exception {
        return run(xml, gpu, new FixedCamera());
    }

    private static CvPipeline run(String xml, boolean gpu, FixedCamera camera) throws Exception {
        CvPipeline.gpuEnabled = gpu;
        CvPipeline pipeline = new CvPipeline(xml);
        pipeline.setProperty("camera", camera);
        pipeline.process();
        return pipeline;
    }

    private static long idleTimeline() {
        long value = GpuRuntime.completed();
        GpuRuntime.await(value, 5_000_000_000L);
        return value;
    }

    @Test
    public void capturedFrameStaysOnGpuAndRunsInOneSubmit() throws Exception {
        Assumptions.assumeTrue(CvPipeline.isGpuAvailable() && GpuRuntime.hasArrayIndexing());
        GpuFixedCamera camera = new GpuFixedCamera();
        try (CvPipeline cpu = run(BOTTOM_VISION, false)) {
            run(BOTTOM_VISION, true, camera).close();
            long before = idleTimeline();
            try (CvPipeline gpu = run(BOTTOM_VISION, true, camera)) {
                assertEquals(before + 1, idleTimeline());
                assertNotNull(gpu.getResult("CaptureImage").getGpuImage());
                assertSameRect((RotatedRect) cpu.getResult("results").model,
                        (RotatedRect) gpu.getResult("results").model);
                assertTrue(meanDifference(cpu.getWorkingImage(), gpu.getWorkingImage()) < 0.05);
                assertEquals(frame.getWidth(), gpu.getLastCapturedImage().getWidth());
            }
        }
    }

    @Test
    public void fiducialRunsInOneSubmitAndMatchesCpu() throws Exception {
        Assumptions.assumeTrue(CvPipeline.isGpuAvailable() && GpuRuntime.hasArrayIndexing()
                && GpuCircularSymmetry.isAvailable());
        GpuFixedCamera camera = new GpuFixedCamera();
        try (CvPipeline cpu = run(FIDUCIAL, false)) {
            run(FIDUCIAL, true, camera).close();
            long before = idleTimeline();
            try (CvPipeline gpu = run(FIDUCIAL, true, camera)) {
                List<?> circles = (List<?>) gpu.getResult("cir").model;
                assertEquals(before + 1, idleTimeline());
                List<?> expected = (List<?>) cpu.getResult("cir").model;
                assertEquals(1, circles.size());
                assertEquals(expected.size(), circles.size());
                Circle e = (Circle) expected.get(0);
                Circle c = (Circle) circles.get(0);
                assertEquals(e.x, c.x, 1e-9);
                assertEquals(e.y, c.y, 1e-9);
                assertEquals(e.diameter, c.diameter, 1e-9);
                assertEquals(700.5, c.x, 2.0);
                assertTrue(meanDifference(cpu.getWorkingImage(), gpu.getWorkingImage()) < 0.05);
            }
        }
    }

    private static void assertSameRect(RotatedRect expected, RotatedRect actual) {
        assertEquals(expected.center.x, actual.center.x, 1.0);
        assertEquals(expected.center.y, actual.center.y, 1.0);
        assertEquals(expected.size.width, actual.size.width, 2.0);
        assertEquals(expected.size.height, actual.size.height, 2.0);
        assertEquals(expected.angle, actual.angle, 0.5);
    }

    private static double meanDifference(Mat a, Mat b) {
        Mat diff = new Mat();
        Core.absdiff(a, b, diff);
        return Core.mean(diff.reshape(1)).val[0];
    }

    @Test
    public void bottomVisionMatchesCpu() throws Exception {
        Assumptions.assumeTrue(CvPipeline.isGpuAvailable(), "no Vulkan GPU with 8-bit storage");
        try (CvPipeline cpu = run(BOTTOM_VISION, false); CvPipeline gpu = run(BOTTOM_VISION, true)) {
            assertNull(cpu.getResult("Threshold").getGpuImage());
            assertNotNull(gpu.getResult("Threshold").getGpuImage());
            assertSameRect((RotatedRect) cpu.getResult("results").model, (RotatedRect) gpu.getResult("results").model);
            assertTrue(meanDifference(cpu.getWorkingImage(), gpu.getWorkingImage()) < 0.05);
            assertTrue(meanDifference(cpu.getResult("InitialBlur").getImage(), gpu.getResult("InitialBlur").getImage()) < 0.05);
        }
    }

    @Test
    public void hsvMaskMatchesCpu() throws Exception {
        Assumptions.assumeTrue(CvPipeline.isGpuAvailable(), "no Vulkan GPU with 8-bit storage");
        for (int softEdge : new int[] { 0, 16 }) {
            try (CvPipeline cpu = run(hsvPipeline(softEdge), false); CvPipeline gpu = run(hsvPipeline(softEdge), true)) {
                // Soft edges only exist on the CPU, which takes the image off the GPU from there on.
                assertEquals(softEdge == 0, gpu.getResult("Threshold").getGpuImage() != null);
                assertSameRect((RotatedRect) cpu.getResult("results").model, (RotatedRect) gpu.getResult("results").model);
                assertTrue(meanDifference(cpu.getResult("ToGray").getImage(), gpu.getResult("ToGray").getImage()) < 0.05,
                        "soft edge " + softEdge);
            }
        }
    }

    @Test
    public void repeatedRunsReuseGpuMemory() throws Exception {
        Assumptions.assumeTrue(CvPipeline.isGpuAvailable(), "no Vulkan GPU with 8-bit storage");
        CvPipeline pipeline = new CvPipeline(BOTTOM_VISION);
        pipeline.setProperty("camera", new FixedCamera());
        RotatedRect first = null;
        for (int i = 0; i < 20; i++) {
            pipeline.process();
            RotatedRect rect = (RotatedRect) pipeline.getResult("results").model;
            if (first == null) {
                first = rect;
            }
            assertSameRect(first, rect);
        }
        pipeline.close();
        assertTrue(GpuRuntime.isAvailable());
    }
}
