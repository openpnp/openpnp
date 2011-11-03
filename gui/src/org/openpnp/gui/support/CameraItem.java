package org.openpnp.gui.support;

import org.openpnp.spi.Camera;

public class CameraItem {
	private Camera camera;
	
	public CameraItem(Camera camera) {
		this.camera = camera;
	}
	
	public Camera getCamera() {
		return camera;
	}
	
	@Override
	public String toString() {
		return camera.getName();
	}
}