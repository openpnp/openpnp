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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point3;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;
import org.openpnp.CameraListener;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.gui.wizards.CameraConfigurationWizard;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.machine.reference.camera.wizards.OpenCvCameraConfigurationWizard;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.util.OpenCvUtils;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persist;

/**
 * A Camera implementation based on the OpenCV FrameGrabbers.
 */
public class OpenCvCamera extends ReferenceCamera implements Runnable {
    static {
        nu.pattern.OpenCV.loadShared();
        System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
    }    
    
	@Attribute(name="deviceIndex", required=true)
	private int deviceIndex = 0;
	
	@Element(required=false)
	private Calibration calibration = new Calibration();
	
	@Attribute(required=false)
	private int preferredWidth;
	@Attribute(required=false)
	private int preferredHeight;
	
	private VideoCapture fg = new VideoCapture();
	private Thread thread;
	private boolean dirty = false;
	
	public OpenCvCamera() {
	}
	
	@Override
	public synchronized BufferedImage capture() {
	    if (thread == null) {
	        setDeviceIndex(deviceIndex);
	    }
		try {
		    Mat mat = new Mat();
		    if (!fg.read(mat)) {
		        return null;
		    }
            if (calibration.isEnabled()) {
                mat = undistort(mat);
            }
//		    if (calibration.isEnabled()) {
//		        mat = estimatePose(mat);
//		    }
		    BufferedImage img = OpenCvUtils.toBufferedImage(mat);
		    mat.release();
		    return transformImage(img);
		}
		catch (Exception e) {
			return null;
		}
	}
	
	private Mat undistort(Mat mat) {
        Mat dst = new Mat();
        Imgproc.undistort(
                mat, 
                dst, 
                calibration.getCameraMatrix(), 
                calibration.getDistortionCoefficients());
        return dst;
	}

	/**
	 * This is just an experiment related to bed leveling. Doesn't belong
	 * here and will go away eventually.
	 * @param mat
	 * @return
	 */
	private Mat estimatePose(Mat mat) {
        Size patternSize = new Size(5, 6);
        MatOfPoint2f corners = new MatOfPoint2f();

        boolean found = Calib3d.findCirclesGridDefault(
                mat, 
                patternSize, 
                corners,
                Calib3d.CALIB_CB_SYMMETRIC_GRID);
	    if (found) {
	        Calib3d.drawChessboardCorners(
	                mat, 
	                patternSize, 
	                corners, 
	                found);
	        Mat rvec = new Mat();
	        Mat tvec = new Mat();
	        List<Point3> objectPointsList = new ArrayList<>();
	        for (int y = 0; y < patternSize.height; y++) {
	            for (int x = 0; x < patternSize.width; x++) {
	                objectPointsList.add(new Point3(y, x, 0));
	            }
	        }
	        MatOfPoint3f objectPoints = new MatOfPoint3f();
	        objectPoints.fromList(objectPointsList);
            
	        MatOfDouble distortionCoefficients = new MatOfDouble(calibration.getDistortionCoefficients());
	        boolean solved = Calib3d.solvePnP(
	                objectPoints, 
	                corners, 
	                calibration.getCameraMatrix(), 
	                distortionCoefficients, 
	                rvec, 
	                tvec);
	        if (solved) {
	            Mat rmat = new Mat();
                Calib3d.Rodrigues(rvec, rmat);
                double RAD_2_DEG = 180 / Math.PI;
                double x = tvec.get(0, 0)[0];
                double y = tvec.get(1, 0)[0];
                double z = tvec.get(2, 0)[0];
                double a = rmat.get(0, 0)[0] * RAD_2_DEG;
                double b = rmat.get(1, 0)[0] * RAD_2_DEG;
                double c = rmat.get(2, 0)[0] * RAD_2_DEG;
                System.out.println(String.format("x %2.2f, y %2.2f, z %2.2f, a %2.2f, b %2.2f, c %2.2f", 
                        x, 
                        y, 
                        z, 
                        a, 
                        b, 
                        c));
	        }
	    }
	    return mat;
	}
	
	@Override
    public synchronized void startContinuousCapture(CameraListener listener, int maximumFps) {
	    if (thread == null) {
	        setDeviceIndex(deviceIndex);
	    }
        super.startContinuousCapture(listener, maximumFps);
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
			thread = null;
		}
		try {
		    setDirty(false);
		    width = null;
		    height = null;
		    fg.open(deviceIndex);
            if (preferredWidth != 0) {
                fg.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, preferredWidth);
            }
            if (preferredHeight != 0) {
                fg.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, preferredHeight);
            }
		}
		catch (Exception e) {
			e.printStackTrace();
			return;
		}
		thread = new Thread(this);
		thread.start();
	}
	
	public Calibration getCalibration() {
	    return calibration;
	}
	
    public int getPreferredWidth() {
        return preferredWidth;
    }

    public void setPreferredWidth(int preferredWidth) {
        this.preferredWidth = preferredWidth;
        setDirty(true);
    }

    public int getPreferredHeight() {
        return preferredHeight;
    }

    public void setPreferredHeight(int preferredHeight) {
        this.preferredHeight = preferredHeight;
        setDirty(true);
    }
    
    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    @Override
	public Wizard getConfigurationWizard() {
		return new OpenCvCameraConfigurationWizard(this);
	}
    
    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName() + " " + getName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
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
    
    @Override
    public Action[] getPropertySheetHolderActions() {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public void close() throws IOException {
        super.close();
        if (thread != null) {
            thread.interrupt();
            try {
                thread.join();
            }
            catch (Exception e) {
                
            }
        }
        if (fg.isOpened()) {
            fg.release();
        }
    }

    public static class Calibration {
        @Attribute(required=false)
        private boolean enabled = false;
        
        @Element(name="cameraMatrix", required=false)
        private double[] cameraMatrixArr = new double[9];
        
        @Element(name="distortionCoefficients", required=false)
        private double[] distortionCoefficientsArr = new double[5];
        
        private Mat cameraMatrix = new Mat(3, 3, CvType.CV_64FC1);
        private Mat distortionCoefficients = new Mat(5, 1, CvType.CV_64FC1);
        
        @Commit
        private void commit() {
            cameraMatrix.put(0, 0, cameraMatrixArr);
            distortionCoefficients.put(0, 0, distortionCoefficientsArr);
        }
        
        @Persist
        private void persist() {
            cameraMatrix.get(0, 0, cameraMatrixArr);
            distortionCoefficients.get(0, 0, distortionCoefficientsArr);
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Mat getCameraMatrix() {
            return cameraMatrix;
        }

        public void setCameraMatrix(Mat cameraMatrix) {
            this.cameraMatrix = cameraMatrix;
        }

        public Mat getDistortionCoefficients() {
            return distortionCoefficients;
        }

        public void setDistortionCoefficients(Mat distortionCoefficients) {
            this.distortionCoefficients = distortionCoefficients;
        }
    }
}
