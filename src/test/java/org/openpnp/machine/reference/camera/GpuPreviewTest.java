package org.openpnp.machine.reference.camera;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openpnp.CameraPreviewListener;
import org.openpnp.model.Configuration;
import org.openpnp.vision.gpu.GpuCameraTransform;
import org.openpnp.vision.gpu.GpuRuntime;

public class GpuPreviewTest {
    private static final String DEVICE = System.getenv("OPENPNP_V4L2_TEST_DEVICE");

    static class StreamCamera extends ImageCamera {
        V4l2Stream stream;

        @Override
        protected V4l2Stream getV4l2Stream() {
            return stream;
        }
    }

    static class Preview implements CameraPreviewListener {
        final List<BufferedImage> received = new ArrayList<>();
        final List<BufferedImage> full = new ArrayList<>();
        int frameWidth;
        boolean wanted = true;
        boolean fullFrames;

        @Override
        public void frameReceived(BufferedImage img) {
            full.add(img);
        }

        @Override
        public boolean isPreviewWanted() {
            return wanted;
        }

        @Override
        public CameraPreviewListener.Preview previewBuffer(int frameWidth, int frameHeight) {
            return fullFrames ? null : new CameraPreviewListener.Preview(new BufferedImage(frameWidth / 4,
                    frameHeight / 4, BufferedImage.TYPE_INT_RGB), 0, 0, frameWidth, frameHeight);
        }

        @Override
        public void previewReceived(CameraPreviewListener.Preview preview, int frameWidth, int frameHeight) {
            received.add(preview.image);
            this.frameWidth = frameWidth;
        }
    }

    @BeforeAll
    public static void setup() throws Exception {
        Assumptions.assumeTrue(DEVICE != null, "set OPENPNP_V4L2_TEST_DEVICE to a V4L2 unique ID");
        Assumptions.assumeTrue(GpuRuntime.isAvailable(), "no Vulkan GPU");
        nu.pattern.OpenCV.loadLocally();
        File dir = Files.createTempDirectory("gpu-preview-test").toFile();
        Configuration.initialize(dir);
    }

    private static void waitForPreview(StreamCamera camera, Preview preview, int count) throws Exception {
        long deadline = System.currentTimeMillis() + 3000;
        while (preview.received.size() < count && System.currentTimeMillis() < deadline) {
            camera.broadcastPreview();
            Thread.sleep(5);
        }
    }

    @Test
    public void rendersPreviewsAtViewSize() throws Exception {
        StreamCamera camera = new StreamCamera();
        Preview preview = new Preview();
        try (V4l2Stream stream = new V4l2Stream(DEVICE, 640, 480, 30)) {
            camera.stream = stream;
            camera.startContinuousCapture(preview);
            waitForPreview(camera, preview, 3);
            assertEquals(3, preview.received.size());
            assertEquals(160, preview.received.get(0).getWidth());
            assertEquals(640, preview.frameWidth);
            assertTrue(preview.full.isEmpty());

            BufferedImage full = camera.getLastBroadcastImage();
            assertNotNull(full);
            assertEquals(640, full.getWidth());

            preview.wanted = false;
            int before = preview.received.size();
            Thread.sleep(100);
            assertTrue(camera.broadcastPreview());
            assertEquals(before, preview.received.size());

            preview.fullFrames = true;
            preview.wanted = true;
            assertTrue(!camera.broadcastPreview());
        }
    }

    @Test
    public void previewsDontConsumeVisionFrames() throws Exception {
        StreamCamera camera = new StreamCamera();
        Preview preview = new Preview();
        try (V4l2Stream stream = new V4l2Stream(DEVICE, 640, 480, 30);
                GpuCameraTransform transform = new GpuCameraTransform()) {
            camera.stream = stream;
            camera.startContinuousCapture(preview);
            BufferedImage first = stream.captureTransformed(transform, 1000);
            waitForPreview(camera, preview, 2);
            long t0 = System.nanoTime();
            BufferedImage second = stream.captureTransformed(transform, 1000);
            assertNotNull(second);
            assertNotSame(first, second);
            assertTrue(System.nanoTime() - t0 < 500_000_000L);
        }
    }
}
