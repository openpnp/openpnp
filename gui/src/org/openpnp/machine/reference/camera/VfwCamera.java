/*
 	Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 	
 	This file is part of OpenPnP.
 	
	OpenPnP is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenPnP is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenPnP.  If not, see <http://www.gnu.org/licenses/>.
 	
 	For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.machine.reference.camera;

import java.awt.image.BufferedImage;

import org.openpnp.machine.reference.ReferenceMachine;
import org.simpleframework.xml.Attribute;
import org.vonnieda.vfw.CaptureDevice;

/**
<pre>
{@code
<!--
	Specify driver="" to obtain a list of available drivers on the console. 
-->
<Configuration 
	driver="Microsoft WDM Image Capture (Win32)" 
	showVideoSourceDialog="false" 
	showVideoFormatDialog="false" 
	showVideoDisplayDialog="false">
</Configuration>
}
</pre>
 */
public class VfwCamera extends AbstractCamera implements Runnable {
	@Attribute
	private String driver;
	@Attribute
	private boolean showVideoSourceDialog;
	@Attribute
	private boolean showVideoFormatDialog;
	@Attribute
	private boolean showVideoDisplayDialog;
	
	private CaptureDevice captureDevice;
	private int width, height;
	
	private BufferedImage lastImage;
	
	private Object captureLock = new Object();
	
	public void setReferenceMachine(ReferenceMachine machine) throws Exception {
		super.setReferenceMachine(machine);
		if (driver == null || driver.trim().length() == 0) {
			System.out.println("No driver specified for VfwCamera [" + getName() + "]. Available drivers are:");
			System.out.println();
			for (String s : CaptureDevice.getCaptureDrivers()) {
				System.out.println("\"" + s + "\"");
			}
			System.out.println();
			System.out.println("Please specify one of the available drivers in the driver attribute of the Configuration for this Camera.");
			// TODO: change to throw exception so we can show in a dialog
			System.exit(1);
		}
		
		new Thread(this).start();
	}
	
	public void run() {
		try {
			captureDevice = CaptureDevice.getCaptureDevice(driver);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
			
		if (showVideoSourceDialog) {
			captureDevice.showVideoSourceDialog();
		}
		
		if (showVideoFormatDialog) {
			captureDevice.showVideoFormatDialog();
		}
		
		if (showVideoDisplayDialog) {
			captureDevice.showVideoDisplayDialog();
		}
		
		width = (int) captureDevice.getVideoDimensions().getWidth();
		height = (int) captureDevice.getVideoDimensions().getHeight();
		
		while (true) {
			int[] captureData = captureDevice.captureFrame();
			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
			image.setRGB(0, 0, width, height, captureData, 0, width);
			broadcastCapture(lastImage = image);
			synchronized (captureLock) {
				captureLock.notify();
			}
			try {
				Thread.sleep(1000 / 30);
			}
			catch (Exception e) {
			}
		}
	}
	
	@Override
	public BufferedImage capture() {
		synchronized (captureLock) {
			try {
				captureLock.wait();
				BufferedImage image = lastImage;
				return image;
			}
			catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
	}
}
