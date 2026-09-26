package org.openpnp.gui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openpnp.machine.reference.camera.ImageCamera;
import org.openpnp.model.Configuration;

public class CameraViewPreviewTest {
    @BeforeAll
    public static void setup() throws Exception {
        System.setProperty("java.awt.headless", "true");
        File dir = Files.createTempDirectory("camera-view-test").toFile();
        Configuration.initialize(dir);
    }

    private static CameraView view() {
        CameraView view = new CameraView();
        view.setCamera(new ImageCamera());
        view.setSize(800, 600);
        return view;
    }

    @Test
    public void handsOutViewSizedBuffersNeverShownOrPending() {
        CameraView view = view();
        Set<BufferedImage> seen = new HashSet<>();
        BufferedImage shown = null;
        for (int i = 0; i < 10; i++) {
            BufferedImage buffer = view.previewBuffer(1920, 1080);
            assertNotNull(buffer);
            assertNotSame(shown, buffer);
            assertEquals(800, buffer.getWidth());
            assertEquals(450, buffer.getHeight());
            assertEquals(BufferedImage.TYPE_INT_RGB, buffer.getType());
            view.previewReceived(buffer, 1920, 1080);
            shown = buffer;
            seen.add(buffer);
        }
        assertEquals(2, seen.size());
    }

    @Test
    public void skipsPendingBuffer() {
        CameraView view = view();
        BufferedImage first = view.previewBuffer(1920, 1080);
        BufferedImage second = view.previewBuffer(1920, 1080);
        assertNotSame(first, second);
    }

    @Test
    public void filteredViewsWantFullFrames() {
        CameraView view = view();
        view.setCameraViewFilter((camera, image) -> image);
        assertNull(view.previewBuffer(1920, 1080));
    }
}
