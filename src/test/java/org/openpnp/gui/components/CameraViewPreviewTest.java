package org.openpnp.gui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openpnp.CameraPreviewListener.Preview;
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
            Preview preview = view.previewBuffer(1920, 1080);
            assertNotNull(preview);
            assertEquals(0, preview.x, 1e-9);
            assertEquals(1920, preview.width, 1e-9);
            BufferedImage buffer = preview.image;
            assertNotSame(shown, buffer);
            assertEquals(800, buffer.getWidth());
            assertEquals(450, buffer.getHeight());
            assertEquals(BufferedImage.TYPE_INT_RGB, buffer.getType());
            view.previewReceived(preview, 1920, 1080);
            shown = buffer;
            seen.add(buffer);
        }
        assertEquals(2, seen.size());
    }

    @Test
    public void skipsPendingBuffer() {
        CameraView view = view();
        BufferedImage first = view.previewBuffer(1920, 1080).image;
        BufferedImage second = view.previewBuffer(1920, 1080).image;
        assertNotSame(first, second);
    }

    @Test
    public void zoomedViewsGetTheVisibleRegionAtViewSize() {
        CameraView view = view();
        for (MouseWheelListener listener : view.getMouseWheelListeners()) {
            listener.mouseWheelMoved(new MouseWheelEvent(view, MouseEvent.MOUSE_WHEEL, 0, 0, 400, 300, 0, false,
                    MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, -10));
        }
        Preview preview = view.previewBuffer(1920, 1080);
        assertEquals(800, preview.image.getWidth());
        assertEquals(600, preview.image.getHeight());
        assertEquals(1920 / 2.0, preview.x + preview.width / 2, 1.0);
        assertEquals(1080 / 2.0, preview.y + preview.height / 2, 1.0);
        assertTrue(preview.width < 1920 / 1.5);
        assertEquals(preview.width / 800, preview.height / 600, 1e-3);
    }

    @Test
    public void filteredViewsWantFullFrames() {
        CameraView view = view();
        view.setCameraViewFilter((camera, image) -> image);
        assertNull(view.previewBuffer(1920, 1080));
    }
}
