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

package org.openpnp.machine.reference;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.openpnp.ConfigurationListener;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.base.AbstractCamera;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.vision.LensCalibration;
import org.openpnp.vision.LensCalibration.LensModel;
import org.openpnp.vision.LensCalibration.Pattern;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ReferenceCamera extends AbstractCamera implements ReferenceHeadMountable {
    static {
        nu.pattern.OpenCV.loadShared();
        System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
    }    

    protected final static Logger logger = LoggerFactory
            .getLogger(ReferenceCamera.class);
    
    @Element(required=false)
    private Location headOffsets = new Location(LengthUnit.Millimeters);
    
    @Attribute(required=false)
    protected double rotation = 0;
    
    @Attribute(required=false)
    protected boolean flipX = false;
    
    @Attribute(required=false)
    protected boolean flipY = false;
    
    @Element(required=false)
    protected Length safeZ = new Length(0, LengthUnit.Millimeters);
    
    @Attribute(required=false)
    protected int offsetX = 0;
    
    @Attribute(required=false)
    protected int offsetY = 0;
    
    @Element(required=false)
    private LensCalibrationParams calibration = new LensCalibrationParams();
    
    private boolean calibrating;
    private CalibrationCallback calibrationCallback;
    private int calibrationCountGoal = 25;
    
    protected ReferenceMachine machine;
    protected ReferenceDriver driver;
    
    
    private LensCalibration lensCalibration;
    
    public ReferenceCamera() {
        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            @Override
            public void configurationLoaded(Configuration configuration)
                    throws Exception {
                machine = (ReferenceMachine) configuration.getMachine();
                driver = machine.getDriver();
            }
        });
    }
    
    @Override
    public Location getHeadOffsets() {
        return headOffsets;
    }
   
    @Override
    public void setHeadOffsets(Location headOffsets) {
        this.headOffsets = headOffsets;
    }
    
    @Override
    public void moveTo(Location location, double speed) throws Exception {
        logger.debug("moveTo({}, {})", new Object[] { location, speed } );
        driver.moveTo(this, location, speed);
        machine.fireMachineHeadActivity(head);
    }

    @Override
    public void moveToSafeZ(double speed) throws Exception {
        logger.debug("{}.moveToSafeZ({})", new Object[] { getName(), speed } );
        Length safeZ = this.safeZ.convertToUnits(getLocation().getUnits());
        Location l = new Location(getLocation().getUnits(), Double.NaN,
                Double.NaN, safeZ.getValue(), Double.NaN);
        driver.moveTo(this, l, speed);
        machine.fireMachineHeadActivity(head);
    }
    
    public double getRotation() {
        return rotation;
    }

    public void setRotation(double rotation) {
        this.rotation = rotation;
    }
    
    public boolean isFlipX() {
        return flipX;
    }

    public void setFlipX(boolean flipX) {
        this.flipX = flipX;
    }

    public boolean isFlipY() {
        return flipY;
    }

    public void setFlipY(boolean flipY) {
        this.flipY = flipY;
    }
    
    public int getOffsetX() {
		return offsetX;
	}

	public void setOffsetX(int offsetX) {
		this.offsetX = offsetX;
	}

	public int getOffsetY() {
		return offsetY;
	}

	public void setOffsetY(int offsetY) {
		this.offsetY = offsetY;
	}

	protected BufferedImage transformImage(BufferedImage image) {
	    Mat mat = OpenCvUtils.toMat(image);
	    
        mat = calibrate(mat);
        
        mat = undistort(mat);
        
        // apply affine transformations
        if (rotation != 0) {
            // TODO: Fix cropping of rotated image:
            // http://stackoverflow.com/questions/22041699/rotate-an-image-without-cropping-in-opencv-in-c
            Point center = new Point(mat.width() / 2D, mat.height() / 2D);
            Mat mapMatrix = Imgproc.getRotationMatrix2D(center, rotation, 1.0);
            Imgproc.warpAffine(mat, mat, mapMatrix, mat.size(), Imgproc.INTER_LINEAR);
        }
        
        if (offsetX != 0 || offsetY != 0) {
            Mat mapMatrix = new Mat(2, 3, CvType.CV_32F) {
                {
                    put(0, 0, 1, 0, offsetX);
                    put(1, 0, 0, 1, offsetY);
                }
            };
            Imgproc.warpAffine(mat, mat, mapMatrix, mat.size(), Imgproc.INTER_LINEAR);
        }

        if (flipX || flipY) {
            int flipCode;
            if (flipX && flipY) {
                flipCode = -1;
            }
            else {
                flipCode = flipX ? 0 : 1;
            }
            Mat dst = new Mat();
            Core.flip(mat, dst, flipCode);
            mat = dst;
        }
        
        image = OpenCvUtils.toBufferedImage(mat);
        
        return image;
    }
	
	private Mat undistort(Mat mat) {
	    if (!calibration.isEnabled()) {
	        return mat;
	    }
        Mat dst = new Mat();
        Imgproc.undistort(
                mat, 
                dst, 
                calibration.getCameraMatrixMat(), 
                calibration.getDistortionCoefficientsMat());
        return dst;
	}
	
	private Mat calibrate(Mat mat) {
	    if (!calibrating) {
	        return mat;
	    }
	    
        int count = lensCalibration.getPatternFoundCount(); 
	    
	    Mat appliedMat = lensCalibration.apply(mat);
	    if (appliedMat == null) {
	        // nothing was found in the image
	        return mat;
	    }
	    
	    if (count != lensCalibration.getPatternFoundCount()) {
	        // a new image was counted, so let the caller know
	        if (lensCalibration.getPatternFoundCount() == calibrationCountGoal) {
	            calibrationCallback.callback(lensCalibration.getPatternFoundCount(), calibrationCountGoal, true);
	            lensCalibration.calibrate();
                calibration.setCameraMatrixMat(lensCalibration.getCameraMatrix());
                calibration.setDistortionCoefficientsMat(lensCalibration.getDistortionCoefficients());
                calibration.setEnabled(true);
                calibrating = false;
	        }
	        else {
	            calibrationCallback.callback(lensCalibration.getPatternFoundCount(), calibrationCountGoal, false);
	        }
	    }
	    
	    return appliedMat;
	}
	
	public void startCalibration(CalibrationCallback callback) {
	    this.calibrationCallback = callback;
	    calibration.setEnabled(false);
	    lensCalibration = new LensCalibration(
	            LensModel.Pinhole, 
	            Pattern.AsymmetricCirclesGrid, 
	            4, 
	            11, 
	            15,
	            750);
	    calibrating = true;
	}
	
	public void cancelCalibration() {
	    calibrating = false;
	}
	
    public LensCalibrationParams getCalibration() {
        return calibration;
    }

    @Override
    public Location getLocation() {
        // If this is a fixed camera we just treat the head offsets as it's
        // table location.
        if (getHead() == null) {
            return getHeadOffsets();
        }
        return driver.getLocation(this);
    }

	public Length getSafeZ() {
		return safeZ;
	}

	public void setSafeZ(Length safeZ) {
		this.safeZ = safeZ;
	}

    @Override
    public void close() throws IOException {
    }
    
    public interface CalibrationCallback {
        public void callback(int progressCurrent, int progressMax, boolean complete);
    }

    public static class LensCalibrationParams {
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

        public Mat getCameraMatrixMat() {
            return cameraMatrix;
        }

        public void setCameraMatrixMat(Mat cameraMatrix) {
            this.cameraMatrix = cameraMatrix;
        }

        public Mat getDistortionCoefficientsMat() {
            return distortionCoefficients;
        }

        public void setDistortionCoefficientsMat(Mat distortionCoefficients) {
            this.distortionCoefficients = distortionCoefficients;
        }
    }
}
