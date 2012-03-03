package org.openpnp.spi;

import java.awt.image.BufferedImage;


public interface VisionProvider {
	public BufferedImage process(BufferedImage image);
}
