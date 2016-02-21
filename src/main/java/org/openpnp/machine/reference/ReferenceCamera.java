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
import java.util.ArrayList;
import java.util.List;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point3;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.openpnp.ConfigurationListener;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.base.AbstractCamera;
import org.openpnp.util.OpenCvUtils;
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
    private LensCalibration calibration = new LensCalibration();
    
    protected ReferenceMachine machine;
    protected ReferenceDriver driver;
    
    private boolean calibrating;
    private CalibrationCallback calibrationCallback;
    private long calibrationLastImageTime;
    private static int calibrationImagesMax = 25;
    private int calibrationImagesCaptured;
    private List<Mat> calibrationImagePoints = new ArrayList<>();
    private List<Mat> calibrationObjectPoints = new ArrayList<>();
    private int calibrationGridWidth = 4;
    private int calibrationGridHeight = 11;
    private double calibrationObjectSize = 15;
    private int calibrationDelay = 1500;
    
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
	    if (calibrating) {
	        image = processCalibrationImage(image);
	    }
	    
        if (calibration.isEnabled()) {
            Mat mat = OpenCvUtils.toMat(image);
            mat = undistort(mat);
            image = OpenCvUtils.toBufferedImage(mat);
        }
	    
        if (rotation == 0 && !flipX && !flipY && offsetX == 0 && offsetY == 0) {
            return image;
        }
        
        BufferedImage out = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        Graphics2D g2d = out.createGraphics();
        AffineTransform xform = new AffineTransform();

        xform.translate(offsetX, offsetY);
        
        if (flipY) {
            xform.scale(-1, 1); 
            xform.translate(-image.getWidth(), 0);
        }
        
        if (flipX) {
            xform.scale(1, -1); 
            xform.translate(0, -image.getHeight());
        }
        
        if (rotation != 0) {
            xform.rotate(Math.toRadians(-rotation), image.getWidth() / 2.0D, image.getHeight() / 2.0D);
        }
        
        g2d.drawImage(image, xform, null);
        g2d.dispose();
        return out;
    }
	
	private Mat undistort(Mat mat) {
        Mat dst = new Mat();
        Imgproc.undistort(
                mat, 
                dst, 
                calibration.getCameraMatrixMat(), 
                calibration.getDistortionCoefficientsMat());
        return dst;
	}
	
	private BufferedImage processCalibrationImage(BufferedImage image) {
	    Mat mat = OpenCvUtils.toMat(image);
        Size patternSize = new Size(calibrationGridWidth, calibrationGridHeight);
        MatOfPoint2f corners = new MatOfPoint2f();
        boolean found = Calib3d.findCirclesGridDefault(
                mat, 
                patternSize, 
                corners,
                Calib3d.CALIB_CB_ASYMMETRIC_GRID);
        if (!found) {
            return image;
        }
        Calib3d.drawChessboardCorners(
                mat, 
                patternSize, 
                corners, 
                found);
        
        if (System.currentTimeMillis() - calibrationLastImageTime > calibrationDelay) {
            calibrationLastImageTime = System.currentTimeMillis();
            calibrationImagesCaptured++;
            calibrationImagePoints.add(corners);
            MatOfPoint3f obj = new MatOfPoint3f();
            for( int i = 0; i < patternSize.height; i++ ) {
                for( int j = 0; j < patternSize.width; j++ ) {
                    obj.push_back(new MatOfPoint3f(new Point3((2 * j + i % 2) * calibrationObjectSize, i * calibrationObjectSize, 0)));
                }
            }
            calibrationObjectPoints.add(obj);
            if (calibrationImagesCaptured >= calibrationImagesMax) {
                calibrating = false;
                calibrationCallback.callback(calibrationImagesCaptured, calibrationImagesMax, true);
                List<Mat> rvecs = new ArrayList<>();
                List<Mat> tvecs = new ArrayList<>();
                Mat cameraMatrix = new Mat(3, 3, CvType.CV_64FC1);
                Mat distortionCoefficients = new Mat(5, 1, CvType.CV_64FC1);
                cameraMatrix.put(0, 0, 1);
                cameraMatrix.put(1, 1, 1);
                Calib3d.calibrateCamera(
                        calibrationObjectPoints, 
                        calibrationImagePoints, 
                        mat.size(), 
                        cameraMatrix, 
                        distortionCoefficients, 
                        rvecs, 
                        tvecs);
                calibration.cameraMatrix = cameraMatrix;
                calibration.distortionCoefficients = distortionCoefficients;
                calibration.enabled = true;
            }
            else {
                calibrationCallback.callback(calibrationImagesCaptured, calibrationImagesMax, false);
            }
        }
        
        return OpenCvUtils.toBufferedImage(mat);
	}
	
	public interface CalibrationCallback {
	    public void callback(int progressCurrent, int progressMax, boolean complete);
	}
	
	public void beginCalibration(CalibrationCallback callback) {
	    this.calibrationCallback = callback;
	    calibration.enabled = false;
	    calibrationLastImageTime = 0;
	    calibrationImagesCaptured = 0;
	    calibrationImagePoints = new ArrayList<>();
	    calibrating = true;
	}
	
	public void completeCalibration() {
	    calibrating = false;
	}
	
	public void cancelCalibration() {
	    calibrating = false;
	}
	
    public LensCalibration getCalibration() {
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
    

    public static class LensCalibration {
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
