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
import org.vonnieda.vfw.CaptureDevice;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

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
	@XStreamAsAttribute
	private String driver;
	@XStreamAsAttribute
	@XStreamAlias(value="show-video-source-dialog")
	private boolean showVideoSourceDialog;
	@XStreamAsAttribute
	@XStreamAlias(value="show-video-format-dialog")
	private boolean showVideoFormatDialog;
	@XStreamAsAttribute
	@XStreamAlias(value="show-video-display-dialog")
	private boolean showVideoDisplayDialog;
	
	@XStreamOmitField
	private CaptureDevice captureDevice;
	@XStreamAsAttribute
	private int width, height;
	
	public void start(ReferenceMachine machine) throws Exception {
		super.start(machine);
		if (driver == null || driver.trim().length() == 0) {
			System.out.println("No driver specified for VfwCamera [" + getName() + "]. Available drivers are:");
			System.out.println();
			for (String s : CaptureDevice.getCaptureDrivers()) {
				System.out.println("\"" + s + "\"");
			}
			System.out.println();
			System.out.println("Please specify one of the available drivers in the driver attribute of the Configuration for this Camera.");
			// TODO: change to throw exceptino so we can show in a dialog
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
		
		System.out.println("Camera " + getName() + " dimensions are " + width + ", " + height);
		
		while (true) {
			if (listeners.size() > 0) {
				BufferedImage frame = captureFrame();
				broadcastCapture(frame);
			}
			try {
				Thread.sleep(1000 / 30);
			}
			catch (Exception e) {
			}
		}
	}
	
	private synchronized BufferedImage captureFrame() {
		int[] frame = captureDevice.captureFrame();
		BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
		bi.setRGB(0, 0, width, height, frame, 0, width);
		return bi;
	}

	@Override
	public BufferedImage capture() {
		return captureFrame();
	}
}
