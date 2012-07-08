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

import org.openpnp.RequiresConfigurationResolution;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.model.Configuration;
import org.simpleframework.xml.Attribute;

import com.googlecode.javacv.FrameGrabber;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

/**
 * A Camera implementation based on the OpenCV FrameGrabbers. Not yet anywhere
 * near complete. 
 *
 */
public class OpenCvCamera extends ReferenceCamera implements RequiresConfigurationResolution, Runnable {
	@Attribute(required=false)
	private String str;
	
	private FrameGrabber fg;
	
	public OpenCvCamera() {
		super("OpenCvCamera");
		try {
			fg = FrameGrabber.createDefault(0);
			fg.start();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		new Thread(this).start();
	}

	@Override
	public void resolve(Configuration configuration) throws Exception {
		super.resolve(configuration);
	}
	
	@Override
	public BufferedImage capture() {
		try {
			IplImage image = fg.grab();
			return image.getBufferedImage();
		}
		catch (Exception e) {
			return null;
		}
	}
	
	public void run() {
		while (true) {
			try {
				BufferedImage image = capture();
				if (image != null) {
					broadcastCapture(image);
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			try {
				Thread.sleep(1000 / 24);
			}
			catch (Exception e) {
				
			}
		}
	}

	@Override
	public Wizard getConfigurationWizard() {
		return null;
	}
}
