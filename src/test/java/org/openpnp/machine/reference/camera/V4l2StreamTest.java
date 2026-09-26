package org.openpnp.machine.reference.camera;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.openpnp.util.OpenCvUtils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.openpnp.vision.gpu.GpuCameraTransform;
import org.openpnp.vision.gpu.GpuImage;
import org.openpnp.vision.gpu.GpuRecording;
import org.openpnp.vision.gpu.GpuRuntime;

public class V4l2StreamTest {
    private static final String DEVICE = System.getenv("OPENPNP_V4L2_TEST_DEVICE");

    @Test
    public void capturesDistinctFrames() throws Exception {
        Assumptions.assumeTrue(DEVICE != null, "set OPENPNP_V4L2_TEST_DEVICE to a V4L2 unique ID");
        Assumptions.assumeTrue(GpuRuntime.isAvailable(), "no Vulkan GPU");
        nu.pattern.OpenCV.loadLocally();
        int width = Integer.getInteger("width", 640);
        int height = Integer.getInteger("height", 480);
        try (V4l2Stream stream = new V4l2Stream(DEVICE, width, height, 30)) {
            BufferedImage first = stream.captureBgr(2000);
            assertNotNull(first);
            assertEquals(width, first.getWidth());
            long t0 = System.nanoTime();
            int frames = 30;
            BufferedImage last = null;
            for (int i = 0; i < frames; i++) {
                last = stream.captureBgr(2000);
                assertNotNull(last);
            }
            double fps = frames / ((System.nanoTime() - t0) / 1e9);
            System.out.println("V4L2 " + DEVICE + ": zero copy " + stream.isZeroCopy() + ", " + fps + " fps");
            Mat a = OpenCvUtils.toMat(first);
            assertTrue(Core.mean(a).val[0] > 0 || Core.mean(a).val[1] > 0, "frame is black");
            assertTrue(fps > 20, "fps " + fps);
            String save = System.getenv("OPENPNP_V4L2_SAVE_FRAME");
            if (save != null) {
                javax.imageio.ImageIO.write(last, "png", new java.io.File(save));
            }
        }
    }

    @Test
    public void heldFramesSettleAndRecordWithoutRunningOutOfBuffers() throws Exception {
        Assumptions.assumeTrue(DEVICE != null, "set OPENPNP_V4L2_TEST_DEVICE to a V4L2 unique ID");
        Assumptions.assumeTrue(GpuRuntime.hasArrayIndexing(), "no Vulkan GPU");
        nu.pattern.OpenCV.loadLocally();
        try (V4l2Stream stream = new V4l2Stream(DEVICE, 640, 480, 30);
                GpuCameraTransform transform = new GpuCameraTransform()) {
            for (int i = 0; i < 20; i++) {
                V4l2Stream.Frame frame = stream.acquireFrame(2000);
                assertNotNull(frame, "frame " + i);
                Mat settle = frame.settleImage(transform, 0, 0, 640, 480, 5);
                assertEquals(128, settle.cols());
                assertEquals(96, settle.rows());
                GpuImage image;
                try (GpuRecording recording = GpuRecording.open()) {
                    image = frame.record(transform);
                }
                frame.close();
                Mat full = image.download();
                image.release();
                Mat gray = new Mat();
                Imgproc.cvtColor(full, gray, Imgproc.COLOR_BGR2GRAY);
                Mat small = new Mat();
                Imgproc.resize(gray, small, new Size(128, 96), 0.2, 0.2);
                Mat diff = new Mat();
                Core.absdiff(small, settle, diff);
                assertEquals(0, Core.countNonZero(diff), "frame " + i);
            }
            V4l2Stream.Frame dropped = stream.acquireFrame(2000);
            dropped.close();
            assertNotNull(stream.captureBgr(2000));
        }
    }
}
