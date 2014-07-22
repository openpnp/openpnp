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

import javax.swing.Action;

import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.gui.wizards.CameraConfigurationWizard;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.machine.reference.camera.wizards.OpenCvCameraConfigurationWizard;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.PropertySheetHolder.PropertySheet;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.core.Commit;

import com.googlecode.javacv.FrameGrabber;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

/**
 * A Camera implementation based on the OpenCV FrameGrabbers.
 */
public class OpenCvCamera extends ReferenceCamera implements Runnable {
	@Attribute(required=true)
	private int deviceIndex = 0;
	
	private FrameGrabber fg;
	private Thread thread;
	
	public OpenCvCamera() {
	}
	
	@Commit
	private void commit() {
		setDeviceIndex(deviceIndex);
	}
	
	@Override
	public synchronized BufferedImage capture() {
		try {
			IplImage image = fg.grab();
			return image.getBufferedImage();
		}
		catch (Exception e) {
			return null;
		}
	}
	
	public void run() {
		while (!Thread.interrupted()) {
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
			catch (InterruptedException e) {
				break;
			}
		}
	}
	
	public int getDeviceIndex() {
		return deviceIndex;
	}

	public synchronized void setDeviceIndex(int deviceIndex) {
		this.deviceIndex = deviceIndex;
		if (thread != null) {
			thread.interrupt();
			try {
				thread.join();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (fg != null) {
			try {
				fg.stop();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			try {
				fg.release();
			}
			catch (Exception e) {
				e.printStackTrace();
				return;
			}
		}
		try {
			fg = FrameGrabber.createDefault(deviceIndex);
			fg.start();
		}
		catch (Exception e) {
			e.printStackTrace();
			return;
		}
		thread = new Thread(this);
		thread.start();
	}

	@Override
	public Wizard getConfigurationWizard() {
		return new OpenCvCameraConfigurationWizard(this);
	}
	
    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName() + " " + getId();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {
                new PropertySheetWizardAdapter(new CameraConfigurationWizard(this)),
                new PropertySheetWizardAdapter(getConfigurationWizard())
        };
    }    
}
