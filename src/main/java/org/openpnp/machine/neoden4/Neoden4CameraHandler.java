package org.openpnp.machine.neoden4;

import org.pmw.tinylog.Logger;

import com.jgoodies.common.base.SystemUtils;
import com.sun.jna.Native;

public final class Neoden4CameraHandler implements Neoden4CameraDriver {

	private static Neoden4CameraDriver driver;

	public Neoden4CameraHandler() {
		String sharedLib = "neodencam"; // translates to libneodencam.so
		if (SystemUtils.IS_OS_WINDOWS) {
			sharedLib = "NeodenCamera.dll";
		}

		driver = (Neoden4CameraDriver) Native
				.synchronizedLibrary(Native.load(sharedLib, Neoden4CameraDriver.class));
	}

	private static Neoden4CameraHandler instance;

	public static synchronized Neoden4CameraHandler getInstance() {
		if (instance == null) {
			instance = new Neoden4CameraHandler();
			instance.initializeCameras();
		}
		return instance;
	}

	private void initializeCameras() {
		int cameras = Neoden4CameraHandler.getInstance().img_init();

		if (cameras < 2) {
			Logger.error(String.format("Bummer, detected %d cameras...", cameras));
		} else {
			Logger.info(String.format("Detected %d neoden cameras...", cameras));
		}

		try {
			Thread.sleep(100);
			instance.img_reset(1);
			Thread.sleep(10);
			instance.img_set_wh(1, (short) 1024, (short) 1024);
			Thread.sleep(10);
			instance.img_set_lt(1, (short) 0, (short) 0);
			Thread.sleep(10);
			instance.img_reset(5);
			Thread.sleep(10);
			instance.img_set_wh(5, (short) 1024, (short) 1024);
			Thread.sleep(10);
			instance.img_set_lt(5, (short) 0, (short) 0);
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean img_capture(int which_camera) {
		return driver.img_capture(which_camera);
	}

	@Override
	public int img_init() {
		return driver.img_init();
	}

	@Override
	public boolean img_led(int camera, short mode) {
		return driver.img_led(camera, mode);
	}

	@Override
	public int img_read(int which_camera, byte[] pFrameBuffer, int BytesToRead, int timeoutMs) {
		return driver.img_read(which_camera, pFrameBuffer, BytesToRead, timeoutMs);
	}

	@Override
	public int img_readAsy(int which_camera, byte[] pFrameBuffer, int BytesToRead, int timeoutMs) {
		return driver.img_readAsy(which_camera, pFrameBuffer, BytesToRead, timeoutMs);
	}

	@Override
	public int img_reset(int which_camera) {
		return driver.img_reset(which_camera);
	}

	@Override
	public boolean img_set_exp(int which_camera, short exposure) {
		return driver.img_set_exp(which_camera, exposure);
	}

	@Override
	public boolean img_set_gain(int which_camera, short gain) {
		return driver.img_set_gain(which_camera, gain);
	}

	@Override
	public boolean img_set_lt(int which_camera, short a2, short a3) {
		return driver.img_set_lt(which_camera, a2, a3);
	}

	@Override
	public boolean img_set_wh(int which_camera, short w, short h) {
		return driver.img_set_wh(which_camera, w, h);
	}

}
