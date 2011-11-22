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

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.List;

import org.openpnp.machine.reference.ReferenceMachine;

import com.lti.civil.CaptureDeviceInfo;
import com.lti.civil.CaptureException;
import com.lti.civil.CaptureObserver;
import com.lti.civil.CaptureStream;
import com.lti.civil.CaptureSystem;
import com.lti.civil.CaptureSystemFactory;
import com.lti.civil.DefaultCaptureSystemFactorySingleton;
import com.lti.civil.Image;
import com.lti.civil.VideoFormat;
import com.lti.civil.awt.AWTImageConverter;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
<pre>
{@code
<!-- 
	Specify deviceId="" to get a list of available devices on the console. 
-->
<Configuration deviceId="?" />
}
</pre>
 */
public class LtiCivilCamera extends AbstractCamera implements CaptureObserver {
	@XStreamOmitField
	private CaptureSystemFactory captureSystemFactory;
	@XStreamOmitField
	private CaptureSystem captureSystem;
	@XStreamOmitField
	private CaptureStream captureStream;
	@XStreamOmitField
	private VideoFormat videoFormat;

	@XStreamAsAttribute
	@XStreamAlias(value="device-id")
	private String deviceId;
	@XStreamAsAttribute
	@XStreamAlias(value="force-grayscale")
	private boolean forceGrayscale;
	
	@XStreamOmitField
	private int width, height;
	
	@XStreamOmitField
	private BufferedImage grayImage;
	
	@Override
	public void start(ReferenceMachine machine) throws Exception {
		super.start(machine);
		
		captureSystemFactory = DefaultCaptureSystemFactorySingleton.instance();
		captureSystem = captureSystemFactory.createCaptureSystem();
		
		if (deviceId == null || deviceId.trim().length() == 0) {
			// TODO make this a dialog
			System.out.println("No deviceId specified for LtiCivilCamera [" + getName() + "]. Available deviceIds are:");
			System.out.println();
			for (CaptureDeviceInfo captureDeviceInfo : (List<CaptureDeviceInfo>) captureSystem.getCaptureDeviceInfoList()) {
				System.out.println("\"" + captureDeviceInfo.getDeviceID() + "\"");
			}
			System.out.println();
			System.out.println("Please specify one of the available deviceIds in the deviceId attribute of the Configuration for this Camera.");
			// TODO: change to throw Exception so we can show in a dialog
			System.exit(1);
		}
		
		
		captureStream = captureSystem.openCaptureDeviceStream(deviceId);
		videoFormat = captureStream.getVideoFormat();
		width = videoFormat.getWidth();
		height = videoFormat.getHeight();
		System.out.println("Camera " + getName() + " dimensions are " + width + ", " + height);
		captureStream.setObserver(this);
		captureStream.start();
	}
	
	@Override
	public void onError(CaptureStream captureStream, CaptureException captureException) {
	}

	@Override
	// TODO should probably turn off the stream if there are no listeners, to save CPU
	public void onNewImage(CaptureStream captureStream, Image newImage) {
		if (listeners.size() > 0) {
			BufferedImage bImage = AWTImageConverter.toBufferedImage(newImage);
			if (forceGrayscale) {
				if (grayImage == null) {
					grayImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
				}
				Graphics g = grayImage.getGraphics();
				g.drawImage(bImage, 0, 0, null);  
				g.dispose();
				broadcastCapture(grayImage);
			}
			else {
				broadcastCapture(bImage);
			}
		}
	}

	@Override
	public BufferedImage capture() {
		// TODO not implemented
		throw new Error("LtiCivilCamera.capture() not yet implemented.");
	}
}
