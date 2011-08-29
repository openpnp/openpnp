package org.openpnp.spi;

import java.awt.image.BufferedImage;

import org.openpnp.CameraListener;

/**
 * Represents a Camera attached to the system and allows a caller to retrieve images from it.
 * This is a simple abstract that is expected to grow as the system's image processing becomes
 * more full featured. A caller can call capture() one time to get the dimensions and details
 * about the returned image. The Camera is expected to return all future images using
 * the same dimensions and type. 
 */
public interface Camera {
	public String getName();
	/**
	 * Immediately captures an image from the camera and returns it in it's native format.
	 * @return
	 */
	public BufferedImage capture();
	
	/**
	 * Registers a listener to receive continuous images from the camera at a rate less than
	 * or equal to maximumFps images per second.
	 * @param listener
	 * @param maximumFps
	 */
	public void startContinuousCapture(CameraListener listener, int maximumFps);
	
	/**
	 * Requests that the continuous capture be stopped for the previously registered listener. If the
	 * Camera has other listeners they should still receive updates.
	 * @param listener
	 */
	public void stopContinuousCapture(CameraListener listener);
}
