package org.openpnp;

import java.awt.image.BufferedImage;

public interface CameraListener {
	public void frameReceived(BufferedImage img);
}
