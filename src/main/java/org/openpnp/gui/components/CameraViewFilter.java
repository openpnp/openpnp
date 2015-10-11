package org.openpnp.gui.components;

import java.awt.image.BufferedImage;

import org.openpnp.spi.Camera;

public interface CameraViewFilter {
	public BufferedImage filterCameraImage(Camera camera, BufferedImage image);
}
