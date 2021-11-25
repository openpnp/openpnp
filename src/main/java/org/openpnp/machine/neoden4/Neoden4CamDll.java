package org.openpnp.machine.neoden4;

import com.sun.jna.Library;
import com.sun.jna.Native;

public interface Neoden4CamDll extends Library {
	
	public static Neoden4CamDll INSTANCE = 
			(Neoden4CamDll) Native.synchronizedLibrary(Native.load("NeodenCamera.dll", Neoden4CamDll.class));

	public boolean img_capture(int which_camera);

	public int img_init();

	public boolean img_led(int camera, short mode);

	public int img_read(int which_camera,  byte[] pFrameBuffer, int BytesToRead, int timeoutMs);

	public int img_readAsy(int which_camera, byte[] pFrameBuffer, int BytesToRead, int timeoutMs);

	public int img_reset(int which_camera);

	public boolean img_set_exp(int which_camera, short exposure);

	public boolean img_set_gain(int which_camera, short gain);

	public boolean img_set_lt(int which_camera, short a2, short a3);

	public boolean img_set_wh(int which_camera, short w, short h);
}
