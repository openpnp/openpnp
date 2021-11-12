/*
 * Copyright (C) 2016 Jason von Nieda <jason@vonnieda.org>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.machine.neoden4;

import java.awt.image.BufferedImage;

import org.opencv.core.Mat;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.neoden4.wizards.Neoden4CameraConfigurationWizard;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.model.Configuration;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.util.OpenCvUtils;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;

/**
 * A Camera implementation for ONVIF compatible IP cameras.
 */
public class Neoden4Camera extends ReferenceCamera {
	@Attribute(required = true)
	private int cameraId = 1;

	@Attribute(required = false)
	private int width = 1024;
	@Attribute(required = false)
	private int height = 1024;
	@Attribute(required = false)
	private int timeout = 1000;

	@Attribute(required = false)
	private int shiftX = 0;
	@Attribute(required = false)
	private int shiftY = 0;

	private boolean dirty = false;
	private int lastExposure = 0;
	private int lastGain = 0;

	public Neoden4Camera() {
	}

	private BufferedImage convertToRgb(BufferedImage image) {
		Mat mat = OpenCvUtils.toMat(image);
		return OpenCvUtils.toBufferedImage(OpenCvUtils.toRGB(mat));
	}

	@Override
	public synchronized BufferedImage internalCapture() {
		Logger.debug(String.format("internalCapture() [cameraId:%d]", cameraId));

		long tStart = System.currentTimeMillis();

		if (!ensureOpen()) {
			Logger.trace(String.format("ensureOpen [cameraId:%d] failed", cameraId));
			return null;
		}

		try {
			byte[] data = new byte[width * height];

			Thread.sleep(10);
			int ret = Neoden4CameraHandler.getInstance().img_readAsy(cameraId, data, data.length, timeout);
			if (ret != 1) {
				Logger.error(String.format("img_readAsy() ret = %d, [cameraId:%d]", ret, cameraId));
				resetCamera();
				return null;
			}
			BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
			img.getRaster().setDataElements(0, 0, width, height, data);
			BufferedImage imgRGB = convertToRgb(img);

			Logger.debug(String.format("internalCapture() done in: %d", System.currentTimeMillis() - tStart));
			return imgRGB;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private void resetCamera() {
		Logger.trace(String.format("Resetting camera [cameraId:%d]", cameraId));
		try {
			Thread.sleep(100);
			cameraReset();
			setCameraExposure(lastExposure);
			setCameraGain(lastGain);
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private synchronized void cameraReset() {
		Logger.trace(String.format("imgReset() [cameraId:%d]", cameraId));
		try {
			Thread.sleep(10);
			Neoden4CameraHandler.getInstance().img_reset(cameraId);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private synchronized void setCameraExposure(int exposure) {
		Logger.trace(String.format("imgSetExposure() [cameraId:%d]", cameraId, exposure));
		try {
			Thread.sleep(10);
			Neoden4CameraHandler.getInstance().img_set_exp(cameraId, (short) exposure);
			lastExposure = exposure;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private synchronized void setCameraGain(int gain) {
		Logger.trace(String.format("imgSetGain() [cameraId:%d, gain:%d]", cameraId, gain));
		try {
			Thread.sleep(10);
			Neoden4CameraHandler.getInstance().img_set_gain(cameraId, (short) gain);
			lastGain = gain;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public synchronized void setCameraExposureAndGain(int exposure, int gain) {
		boolean reset = false;
		if (gain != lastGain) {
			lastGain = gain;
			reset = true;
		}

		if (exposure != lastExposure) {
			lastExposure = exposure;
			reset = true;
		}

		if (reset) {
			resetCamera();
		}
	}

	@Override
	public synchronized void open() throws Exception {
		stop();
		Logger.trace(String.format("open() [cameraId:%d, width: %d, height: %d]", cameraId, width, height));
		resetCamera();
		super.open();
	}

	public int getCameraId() {
		return cameraId;
	}

	public synchronized void setCameraId(int cameraId) {
		synchronized (Neoden4Camera.class) {
			this.cameraId = cameraId;
		}
	}

	public int getWidth() {
		return this.width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return this.height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getTimeout() {
		return this.timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public int getShiftX() {
		return this.shiftX;
	}

	public void setShiftX(int shiftX) {
		this.shiftX = shiftX;
	}

	public int getShiftY() {
		return this.shiftY;
	}

	public void setShiftY(int shiftY) {
		this.shiftY = shiftY;
	}

	public boolean isDirty() {
		return dirty;
	}

	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	@Override
	public Wizard getConfigurationWizard() {
		return new Neoden4CameraConfigurationWizard(this);
	}

	@Override
	public String getPropertySheetHolderTitle() {
		return getClass().getSimpleName() + " " + getName();
	}

	@Override
	public PropertySheetHolder[] getChildPropertySheetHolders() {
		return null;
	}
}
