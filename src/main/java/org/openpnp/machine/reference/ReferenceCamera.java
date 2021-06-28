/*
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.machine.reference;

import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;
import org.openpnp.ConfigurationListener;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraPanel;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.components.CameraView.RenderingQuality;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.wizards.CameraConfigurationWizard;
import org.openpnp.gui.wizards.CameraVisionConfigurationWizard;
import org.openpnp.machine.reference.camera.AutoFocusProvider;
import org.openpnp.machine.reference.camera.OpenPnpCaptureCamera;
import org.openpnp.machine.reference.camera.SimulatedUpCamera;
import org.openpnp.machine.reference.wizards.ReferenceCameraCalibrationConfigurationWizard;
import org.openpnp.machine.reference.wizards.ReferenceCameraCalibrationWizard;
import org.openpnp.machine.reference.wizards.ReferenceCameraPositionConfigurationWizard;
import org.openpnp.machine.reference.wizards.ReferenceCameraTransformsConfigurationWizard;
import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.model.Solutions;
import org.openpnp.model.Solutions.Milestone;
import org.openpnp.model.Solutions.Severity;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Camera;
import org.openpnp.spi.FocusProvider;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.util.CameraCalibrationUtils;
import org.openpnp.util.Collect;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.vision.LensCalibration;
import org.openpnp.vision.LensCalibration.LensModel;
import org.openpnp.vision.LensCalibration.Pattern;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementArray;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persist;

public abstract class ReferenceCamera extends AbstractBroadcastingCamera implements ReferenceHeadMountable {
    static {
        nu.pattern.OpenCV.loadShared();
    }

    @Attribute(required = false)
    private int captureTryCount = 4;

    @Attribute(required = false)
    private int captureTryTimeoutMs = 2000;

    @Element(required = false)
    private Location headOffsets = new Location(LengthUnit.Millimeters);

    @Attribute(required = false)
    protected double rotation = 0;

    @Attribute(required = false)
    protected boolean flipX = false;

    @Attribute(required = false)
    protected boolean flipY = false;

    @Deprecated
    @Element(required = false)
    protected Length safeZ = null;

    @Attribute(required = false)
    protected int offsetX = 0;

    @Attribute(required = false)
    protected int offsetY = 0;

    @Attribute(required = false)
    protected int cropWidth = 0;

    @Attribute(required = false)
    protected int cropHeight = 0;
    
    @Attribute(required = false)
    protected int scaleWidth = 0;
    
    @Attribute(required = false)
    protected int scaleHeight = 0;
    
    @Attribute(required = false)
    protected boolean deinterlace;

    @Element(required = false)
    private LensCalibrationParams calibration = new LensCalibrationParams();

    @Element(required = false)
    private AdvancedCalibration advancedCalibration = new AdvancedCalibration();

    @Attribute(required = false)
    private String lightActuatorId; 
    @Attribute(required = false)
    private boolean allowMachineActuators = false;

    @Attribute(required = false)
    private FocusSensingMethod focusSensingMethod = FocusSensingMethod.None;

    @Element(required = false)
    protected FocusProvider focusProvider = new AutoFocusProvider();

    private boolean calibrating;
    private CalibrationCallback calibrationCallback;
    private int calibrationCountGoal = 25;

    private Mat undistortionMap1;
    private Mat undistortionMap2;

    private LensCalibration lensCalibration;

    private Actuator lightActuator;

    public enum FocusSensingMethod {
        None,
        AutoFocus
    }

    public ReferenceCamera() {
        super();
        Configuration.get().addListener(new ConfigurationListener.Adapter() {

            @Override
            public void configurationLoaded(Configuration configuration) throws Exception {
                // We don't have access to machine or head here. So we need to scan them all. 
                // I'm sure there is a better solution.
                Machine machine = configuration.getMachine();
                lightActuator = machine.getActuator(lightActuatorId);
                for (Head head : machine.getHeads()) {
                    if (lightActuator == null) {
                        lightActuator = head.getActuator(lightActuatorId);
                    }
                }
            }
        });
    }
    
    /**
     * Captures an image using captureTransformed() and performs scripting and lighting events
     * before and after the capture.
     * @throws Exception 
     */
    @Override
    public BufferedImage capture() throws Exception {
        Map<String, Object> globals = new HashMap<>();
        globals.put("camera", this);
        Configuration.get().getScripting().on("Camera.BeforeCapture", globals);

        BufferedImage image = captureTransformed();

        Configuration.get().getScripting().on("Camera.AfterCapture", globals);
        return image;
    }
    
    /**
     * Captures an image using captureRaw(), applies local transformations and returns the image.
     */
    @Override
    public BufferedImage captureTransformed() {
        return transformImage(captureRaw());
    }
    
    /**
     * Captures an image using safeInternalCapture() and returns it without any transformations
     * applied.
     */
    @Override
    public BufferedImage captureRaw() {
        return safeInternalCapture();
    }

    @Override
    public boolean hasNewFrame() {
        // Default behavior: always has frames when open.
        return isOpen();
    }

    protected abstract BufferedImage internalCapture();
    
    /**
     * Wraps internalCapture() to ensure that a null image is never returned. Attempts to
     * retry capture if the capture returns null and if no image can be captured returns a
     * default image. Several of the low level camera drivers return null when there is a
     * capture error, but these are often temporary and we would prefer not to have bad
     * images returned. The retry is intended to smooth this out.
     * @return
     */
    protected synchronized BufferedImage safeInternalCapture() {
        if (! ensureOpen()) {
            return getCaptureErrorImage();
        }
        long t1 = System.currentTimeMillis() + captureTryTimeoutMs;
        int i = 0;
        while (true) {
            BufferedImage image = internalCapture();
            i++;
            if (image != null) {
                return image;
            }
            if (i >= getCaptureTryCount()) {
                break;
            }
            if (System.currentTimeMillis() > t1) {
                // Timed out.
                break;
            }
            Logger.trace("Camera {} failed to return an image. Retrying.", this);
            Thread.yield();
        }
        Logger.warn("Camera {} failed to return an image after {} tries.", this, i);
        return getCaptureErrorImage();
    }

    protected int getCaptureTryCount() {
        return captureTryCount;
    }

    @Override
    public synchronized int getWidth() {
        if (width == null) {
            determineSize();
        }
        return width;
    }

    @Override
    public synchronized int getHeight() {
        if (height == null) {
            determineSize();
        }
        return height;
    }

    private void determineSize() {
        if (isOpen()) {
            BufferedImage image = captureTransformed();
            width = image.getWidth();
            height = image.getHeight();
        }
        else {
            width = 640;
            height = 480;
        }
    }

    @Override
    public Location getHeadOffsets() {
        return headOffsets;
    }

    @Override
    public void setHeadOffsets(Location headOffsets) {
        this.headOffsets = headOffsets;
        viewHasChanged();
    }

    @Override
    public void home() throws Exception {
    }

    protected void viewHasChanged() {
        if (this.getLooking() == Looking.Up) {
            // Changing an up-looking camera view invalidates the nozzle tip calibration.
            ReferenceNozzleTipCalibration.resetAllNozzleTips();
        }
    }

    public double getRotation() {
        return rotation;
    }

    public void setRotation(double rotation) {
        this.rotation = rotation;
        viewHasChanged();
    }

    public boolean isFlipX() {
        return flipX;
    }

    public void setFlipX(boolean flipX) {
        this.flipX = flipX;
        viewHasChanged();
    }

    public boolean isFlipY() {
        return flipY;
    }

    public void setFlipY(boolean flipY) {
        this.flipY = flipY;
        viewHasChanged();
    }

    public int getOffsetX() {
        return offsetX;
    }

    public void setOffsetX(int offsetX) {
        this.offsetX = offsetX;
        viewHasChanged();
    }

    public int getOffsetY() {
        return offsetY;
    }

    public void setOffsetY(int offsetY) {
        this.offsetY = offsetY;
        viewHasChanged();
    }

    public int getCropWidth() {
        return cropWidth;
    }

    public void setCropWidth(int cropWidth) {
        this.cropWidth = cropWidth;
        viewHasChanged();
    }

    public int getCropHeight() {
        return cropHeight;
    }

    public void setCropHeight(int cropHeight) {
        this.cropHeight = cropHeight;
        viewHasChanged();
    }

    public int getScaleWidth() {
        return scaleWidth;
    }

    public void setScaleWidth(int scaleWidth) {
        this.scaleWidth = scaleWidth;
        viewHasChanged();
    }

    public int getScaleHeight() {
        return scaleHeight;
    }

    public void setScaleHeight(int scaleHeight) {
        this.scaleHeight = scaleHeight;
        viewHasChanged();
    }
    
    public boolean isDeinterlace() {
        return isDeinterlaced();
    }

    public void setDeinterlace(boolean deinterlace) {
        this.deinterlace = deinterlace;
    }

    @Override
    public Actuator getLightActuator() {
        return lightActuator;
    }

    public void setLightActuator(Actuator lightActuator) {
        this.lightActuator = lightActuator;
        this.lightActuatorId = (lightActuator == null) ? null : lightActuator.getId();
    }

    public boolean isAllowMachineActuators() {
        return allowMachineActuators;
    }

    public void setAllowMachineActuators(boolean allowMachineActuators) {
        this.allowMachineActuators = allowMachineActuators;
    }

    public FocusSensingMethod getFocusSensingMethod() {
        return focusSensingMethod;
    }

    public void setFocusSensingMethod(FocusSensingMethod partHeightVisionMethod) {
        this.focusSensingMethod = partHeightVisionMethod;
        // if we ever expand the methods this would be the point where another method's focusProvider
        // would be instantiated.
    }

    @Override
    public FocusProvider getFocusProvider() {
        if (getFocusSensingMethod() != FocusSensingMethod.None) {
            return focusProvider;
        }
        else {
            return null;
        }
    }

    protected BufferedImage transformImage(BufferedImage image) {
        try {
            if (image == null) {
                return null;
            }

            if (advancedCalibration.isOverrideOldStyleSettings()) {
                //Skip all the old style image transforms and distortion corrections
                if (advancedCalibration.isEnabled()) {
                    //Use the new advanced image transformation and distortion correction
                    Mat mat = OpenCvUtils.toMat(image);
                    mat = advancedUndistort(mat);
                    image = OpenCvUtils.toBufferedImage(mat);
                    mat.release();
                }
            }
            // Old style of image transforms and distortion correction
            // We do skip the convert to and from Mat if no transforms are needed.
            // But we must enter while calibrating.
            else if (isDeinterlaced()
                || isCropped() 
                || isCalibrating()
                || isUndistorted()
                || isScaled()
                || isRotated()
                || isOffset()
                || isFlipped()) {

                Mat mat = OpenCvUtils.toMat(image);

                mat = deinterlace(mat);

                mat = crop(mat);

                mat = calibrate(mat);

                mat = undistort(mat);

                // apply affine transformations
                mat = scale(mat);

                mat = rotate(mat);

                mat = offset(mat);

                mat = flip(mat);

                image = OpenCvUtils.toBufferedImage(mat);
                mat.release();
            }
            if (image != null) {
                // save the new image dimensions
                width = image.getWidth();
                height = image.getHeight();
                setLastTransformedImage(image);
            }
        }
        catch (Exception e) {
            Logger.error(e);
        }
        return image;
    }

    private Mat advancedUndistort(Mat mat) {
        if (undistortionMap1 == null || undistortionMap2 == null) {
            undistortionMap1 = new Mat();
            undistortionMap2 = new Mat();
            advancedCalibration.initUndistortRectifyMap(mat.size(), 
                    ((double)advancedCalibration.alphaPercent)/100.0, 
                    undistortionMap1, undistortionMap2);
            double dz = defaultZ.convertToUnits(LengthUnit.Millimeters).getValue();
            double distanceToCamera = advancedCalibration.getDistanceToCameraAtZ(defaultZ).getValue();
            Logger.trace("distanceToCamera@defaultZ = " + distanceToCamera);
            double uppX = distanceToCamera / advancedCalibration.virtualCameraMatrix.get(0, 0)[0];
            double uppY = distanceToCamera / advancedCalibration.virtualCameraMatrix.get(1, 1)[0];
            Logger.trace("primary uppX = " + uppX);
            Logger.trace("primary uppY = " + uppY);
            setUnitsPerPixelPrimary(new Location(LengthUnit.Millimeters, uppX, uppY, dz, 0));
            if (ReferenceCamera.this.looking == Looking.Down) {
                setCameraPrimaryZ(new Length(0, LengthUnit.Millimeters));
            }
            else {
                setCameraPrimaryZ(defaultZ);
            }
            
            distanceToCamera = advancedCalibration.getDistanceToCameraAtZ(new Length(dz + 20, 
                    LengthUnit.Millimeters)).getValue();
            Logger.trace("distanceToCamera@defaultZ+20 = " + distanceToCamera);
            uppX = distanceToCamera / advancedCalibration.virtualCameraMatrix.get(0, 0)[0];
            uppY = distanceToCamera / advancedCalibration.virtualCameraMatrix.get(1, 1)[0];
            Logger.trace("secondary uppX = " + uppX);
            Logger.trace("secondary uppY = " + uppY);
            setUnitsPerPixelSecondary(new Location(LengthUnit.Millimeters, uppX, uppY, dz + 20, 0));
            if (ReferenceCamera.this.looking == Looking.Down) {
                setCameraSecondaryZ(new Length(0, LengthUnit.Millimeters));
            }
            else {
                setCameraSecondaryZ(defaultZ);
            }
            
            setEnableUnitsPerPixel3D(true);

            if (getHead() == null) {
                setHeadOffsets(new Location(LengthUnit.Millimeters, advancedCalibration.getVect_m_cHat_m().get(0, 0)[0],
                        advancedCalibration.getVect_m_cHat_m().get(1, 0)[0], dz, 0));
            }
        }
        
        Mat dst = mat.clone();
        Imgproc.remap(mat, dst, undistortionMap1, undistortionMap2, Imgproc.INTER_LINEAR);
        mat.release();

        return dst;
    }

    private Mat crop(Mat mat) {
        if (isCropped()) {
            int cw = (cropWidth != 0 && cropWidth < (int) mat.size().width) ? cropWidth : (int) mat.size().width;
            int ch = (cropHeight != 0 && cropHeight < (int) mat.size().height) ? cropHeight : (int) mat.size().height;
            Rect roi = new Rect(
                    (int) ((mat.size().width / 2) - (cw / 2)),
                    (int) ((mat.size().height / 2) - (ch / 2)),
                    cw,
                    ch);
            Mat tmp = new Mat(mat, roi);
            tmp.copyTo(mat);
            tmp.release();
        }
        return mat;
    }

    protected boolean isCropped() {
        return cropWidth != 0 || cropHeight != 0;
    }

    private Mat deinterlace(Mat mat) {
        if (!isDeinterlaced()) {
            return mat;
        }
        Mat dst = new Mat(mat.size(), mat.type());
        for (int i = 0; i < mat.rows() / 2; i++) {
            mat.row(i).copyTo(dst.row(i * 2));
            mat.row(i + mat.rows() / 2).copyTo(dst.row(i * 2 + 1));
        }
        mat.release();
        return dst;
    }

    protected boolean isDeinterlaced() {
        return deinterlace;
    }

    private Mat rotate(Mat mat) {
        if (!isRotated()) {
            return mat;
        }

        // See:
        // http://stackoverflow.com/questions/22041699/rotate-an-image-without-cropping-in-opencv-in-c
        Point center = new Point(mat.width() / 2D, mat.height() / 2D);
        Mat mapMatrix = Imgproc.getRotationMatrix2D(center, rotation, 1.0);

        // determine bounding rectangle
        Rect bbox = new RotatedRect(center, mat.size(), rotation).boundingRect();
        // adjust transformation matrix
        double[] cx = mapMatrix.get(0, 2);
        double[] cy = mapMatrix.get(1, 2);
        cx[0] += bbox.width / 2D - center.x;
        cy[0] += bbox.height / 2D - center.y;
        mapMatrix.put(0, 2, cx);
        mapMatrix.put(1, 2, cy);

        Mat dst = new Mat(bbox.width, bbox.height, mat.type());
        Imgproc.warpAffine(mat, dst, mapMatrix, bbox.size(), Imgproc.INTER_LINEAR);
        mat.release();

        mapMatrix.release();

        return dst;
    }

    protected boolean isRotated() {
        return rotation != 0D;
    }

    private Mat offset(Mat mat) {
        if (!isOffset()) {
            return mat;
        }

        Mat mapMatrix = new Mat(2, 3, CvType.CV_32F) {
            {
                put(0, 0, 1, 0, offsetX);
                put(1, 0, 0, 1, offsetY);
            }
        };

        Mat dst = mat.clone();
        Imgproc.warpAffine(mat, dst, mapMatrix, mat.size(), Imgproc.INTER_LINEAR);
        mat.release();

        mapMatrix.release();

        return dst;
    }

    protected boolean isOffset() {
        return offsetX != 0D || offsetY != 0D;
    }
    
    private Mat scale(Mat mat) {
        if (!isScaled()) {
            return mat;
        }
        Mat dst = new Mat();
        Imgproc.resize(mat, dst, new Size(scaleWidth, scaleHeight));
        mat.release();
        return dst;
    }

    protected boolean isScaled() {
        return scaleWidth != 0D || scaleHeight != 0D;
    }
    
    private Mat undistort(Mat mat) {
        if (!isUndistorted()) {
            return mat;
        }

        if (undistortionMap1 == null || undistortionMap2 == null) {
            undistortionMap1 = new Mat();
            undistortionMap2 = new Mat();
            Mat rectification = Mat.eye(3, 3, CvType.CV_32F);
            Calib3d.initUndistortRectifyMap(calibration.getCameraMatrixMat(),
                    calibration.getDistortionCoefficientsMat(), rectification,
                    calibration.getCameraMatrixMat(), mat.size(), CvType.CV_32FC1, undistortionMap1,
                    undistortionMap2);
            rectification.release();
        }

        Mat dst = mat.clone();
        Imgproc.remap(mat, dst, undistortionMap1, undistortionMap2, Imgproc.INTER_LINEAR);
        mat.release();

        return dst;
    }

    public void setUndistorted(boolean undistorted) {
        if (!undistorted) {
            clearCalibrationCache();
        }
        calibration.setEnabled(undistorted);
    }

    protected boolean isUndistorted() {
        return calibration.isEnabled();
    }

    protected Mat flip(Mat mat) {
        if (isFlipped()) {
            int flipCode;
            if (flipX && flipY) {
                flipCode = -1;
            }
            else {
                flipCode = flipX ? 0 : 1;
            }
            Core.flip(mat, mat, flipCode);
        }
        return mat;
    }

    protected boolean isFlipped() {
        return flipX || flipY;
    }

    private Mat calibrate(Mat mat) {
        if (!isCalibrating()) {
            return mat;
        }

        // Get the number of images counted so far.
        int count = lensCalibration.getPatternFoundCount();

        // Submit an image for counting. If it is good the count will increase.
        Mat appliedMat = lensCalibration.apply(mat);
        if (appliedMat == null) {
            // nothing was found in the image
            return mat;
        }

        // If the count changed then we have counted a new image, so let the caller know.
        if (count != lensCalibration.getPatternFoundCount()) {
            // If we've reached our goal, finish the process.
            if (lensCalibration.getPatternFoundCount() == calibrationCountGoal) {
                calibrationCallback.callback(lensCalibration.getPatternFoundCount(),
                        calibrationCountGoal, true);
                lensCalibration.calibrate();
                calibration.setCameraMatrixMat(lensCalibration.getCameraMatrix());
                calibration
                        .setDistortionCoefficientsMat(lensCalibration.getDistortionCoefficients());
                clearCalibrationCache();
                calibration.setEnabled(true);

                lensCalibration.close();
                lensCalibration = null;
                calibrating = false;
            }
            // Otherwise just report the addition.
            else {
                calibrationCallback.callback(lensCalibration.getPatternFoundCount(),
                        calibrationCountGoal, false);
            }
        }

        return appliedMat;
    }

    public boolean isCalibrating() {
        return calibrating;
    }

    public void clearCalibrationCache() {
        // Clear the calibration cache
        if (undistortionMap1 != null) {
            undistortionMap1.release();
            undistortionMap1 = null;
        }
        if (undistortionMap2 != null) {
            undistortionMap2.release();
            undistortionMap2 = null;
        }
    }

    public void startCalibration(CalibrationCallback callback) {
        this.calibrationCallback = callback;
        calibration.setEnabled(false);
        lensCalibration = new LensCalibration(LensModel.Pinhole, Pattern.AsymmetricCirclesGrid, 4,
                11, 15, 750);
        calibrating = true;
    }

    public void cancelCalibration() {
        if (isCalibrating()) {
            lensCalibration.close();
        }
        calibrating = false;
    }

    public LensCalibrationParams getCalibration() {
        return calibration;
    }

    public AdvancedCalibration getAdvancedCalibration() {
        return advancedCalibration;
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        PropertySheet[] sheets = new PropertySheet[] {
                new PropertySheetWizardAdapter(new CameraConfigurationWizard(this), "General Configuration"),
                new PropertySheetWizardAdapter(new CameraVisionConfigurationWizard(this), "Vision"),
                new PropertySheetWizardAdapter(getConfigurationWizard(), "Device Settings"),
                new PropertySheetWizardAdapter(new ReferenceCameraPositionConfigurationWizard(getMachine(), this), "Position"),
                new PropertySheetWizardAdapter(new ReferenceCameraCalibrationConfigurationWizard(this), "Lens Calibration"),
                new PropertySheetWizardAdapter(new ReferenceCameraTransformsConfigurationWizard(this), "Image Transforms"),
                new PropertySheetWizardAdapter(new ReferenceCameraCalibrationWizard(this), "Experimental Calibration"),
        };
        if (getFocusSensingMethod() != FocusSensingMethod.None) {
                sheets = Collect.concat(sheets, new PropertySheet[] {
                        new PropertySheetWizardAdapter(getFocusProvider().getConfigurationWizard(this), "Auto Focus"),
                });
        }
        return sheets;
    }
    
    @Override
    public Action[] getPropertySheetHolderActions() {
        return new Action[] { deleteAction };
    }
    
    public Action deleteAction = new AbstractAction("Delete Camera") {
        {
            putValue(SMALL_ICON, Icons.delete);
            putValue(NAME, "Delete Camera");
            putValue(SHORT_DESCRIPTION, "Delete the currently selected camera.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            int ret = JOptionPane.showConfirmDialog(MainFrame.get(),
                    "Are you sure you want to delete " + getName() + "?",
                    "Delete " + getName() + "?", JOptionPane.YES_NO_OPTION);
            if (ret == JOptionPane.YES_OPTION) {
                if (getHead() != null) {
                    getHead().removeCamera(ReferenceCamera.this);
                }
                else {
                    Configuration.get().getMachine().removeCamera(ReferenceCamera.this);
                }
                MainFrame.get().getCameraViews().removeCamera(ReferenceCamera.this);
                try {
                    ReferenceCamera.this.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };
    
    ReferenceMachine getMachine() {
        return (ReferenceMachine) Configuration.get().getMachine();
    }

    public interface CalibrationCallback {
        public void callback(int progressCurrent, int progressMax, boolean complete);
    }

    public static class AdvancedCalibration extends LensCalibrationParams {
        @Attribute(required = false)
        private boolean overrideOldStyleSettings = false;
           
        @Element(required = false)
        private String calibrationRigId;
        
        @Element(name = "virtualCameraMatrix", required = false)
        private double[] virtualCameraMatrix_cArr = new double[9];

        @Element(name = "rectification", required = false)
        private double[] rectification_cArr = new double[9];

        @Element(name = "rotate_m_c", required = false)
        private double[] rotate_m_cArr = new double[9];

        @Element(name = "vect_c_m_c", required = false)
        private double[] vect_c_m_cArr = new double[3];

        @Element(name = "vect_m_c_m", required = false)
        private double[] vect_m_c_mArr = new double[3];

        @Element(name = "unit_c_cz_m", required = false)
        private double[] unit_c_cz_mArr = new double[3];

        @Element(name = "vect_m_cHat_m", required = false)
        private double[] vect_m_cHat_mArr = new double[3];

        @Element(name = "vect_c_newPrincipalPoint_c", required = false)
        private double[] vect_c_newPrincipalPoint_cArr = new double[3];
        
        @ElementArray(required = false)
        private double[] calibrationPatternZ;
        
        @Attribute(required = false)
        private int alphaPercent = 50;
        
        @ElementArray(required = false)
        private double[][][] savedTestPattern3dPointsList;
        
        @ElementArray(required = false)
        private double[][][] savedTestPatternImagePointsList;
        
        @Attribute(required = false)
        private double zRotationError = 0;

        @Attribute(required = false)
        private double yRotationError = 0;

        @Attribute(required = false)
        private double xRotationError = 0;

        /**
         * @return the overrideOldStyleSettings
         */
        public boolean isOverrideOldStyleSettings() {
            return overrideOldStyleSettings;
        }

        /**
         * @param overrideOldStyleSettings the overrideOldStyleSettings to set
         */
        public void setOverrideOldStyleSettings(boolean overrideOldStyleSettings) {
            this.overrideOldStyleSettings = overrideOldStyleSettings;
        }

        /**
         * @return the savedTestPatternImagePointsList
         */
        public double[][][] getSavedTestPatternImagePointsList() {
            return savedTestPatternImagePointsList;
        }

        /**
         * @return the savedTestPattern3dPointsList
         */
        public double[][][] getSavedTestPattern3dPointsList() {
            return savedTestPattern3dPointsList;
        }

        /**
         * @param savedTestPattern3dPointsList the savedTestPattern3dPointsList to set
         */
        public void setSavedTestPattern3dPointsList(double[][][] savedTestPattern3dPointsList) {
            this.savedTestPattern3dPointsList = savedTestPattern3dPointsList;
        }

        /**
         * @param savedTestPatternImagePointsList the savedTestPatternImagePointsList to set
         */
        public void setSavedTestPatternImagePointsList(double[][][] savedTestPatternImagePointsList) {
            this.savedTestPatternImagePointsList = savedTestPatternImagePointsList;
        }

        /**
         * @return the alphaPercent
         */
        public int getAlphaPercent() {
            return alphaPercent;
        }

        /**
         * @param alphaPercent the alphaPercent to set
         */
        public void setAlphaPercent(int alphaPercent) {
            this.alphaPercent = alphaPercent;
        }

        private Part calibrationRig;
        private Mat virtualCameraMatrix = Mat.eye(3, 3, CvType.CV_64FC1);
        private Mat rectification = Mat.eye(3, 3, CvType.CV_64FC1);
        private Mat rotate_m_c = Mat.eye(3, 3, CvType.CV_64FC1);
        private Mat vect_c_m_c = Mat.zeros(3, 1, CvType.CV_64FC1);
        private Mat vect_m_c_m = Mat.zeros(3, 1, CvType.CV_64FC1);
        private Mat unit_c_cz_m = Mat.zeros(3, 1, CvType.CV_64FC1);
        private Mat vect_m_cHat_m = Mat.zeros(3, 1, CvType.CV_64FC1);
        private Mat vect_c_newPrincipalPoint_c = Mat.zeros(3, 1, CvType.CV_64FC1);


        
        @Commit
        private void commit() {
            super.commit();
            if (calibrationRigId == null) {
                calibrationRigId = Configuration.get().getParts().get(0).getId();
            }
            calibrationRig = Configuration.get().getPart(calibrationRigId);
            virtualCameraMatrix.put(0, 0, virtualCameraMatrix_cArr);
            rectification.put(0, 0, rectification_cArr);
            rotate_m_c.put(0, 0, rotate_m_cArr);
            vect_c_m_c.put(0, 0, vect_c_m_cArr);
            vect_m_c_m.put(0, 0, vect_m_c_mArr);
            unit_c_cz_m.put(0, 0, unit_c_cz_mArr);
            vect_m_cHat_m.put(0, 0, vect_m_cHat_mArr);
            vect_c_newPrincipalPoint_c.put(0, 0, vect_c_newPrincipalPoint_cArr);
            
            //For some reason, the serialization/deserialization process doesn't seem to correctly
            //handle 3D arrays.  If an n x m x p array is serialized and then deserialized, the
            //array comes back as n x m*p x 1.  Is there a better way to fix this?
            if (savedTestPattern3dPointsList != null) {
                if (savedTestPattern3dPointsList[0][0].length == 1) {
                    int numberOfPatterns = savedTestPattern3dPointsList.length;
                    double[][][] temp = new double[numberOfPatterns][][];
                    for (int i=0; i<numberOfPatterns; i++) {
                        int numberOfPoints = savedTestPattern3dPointsList[i].length / 3;
                        temp[i] = new double[numberOfPoints][3];
                        for (int j=0; j<numberOfPoints; j++) {
                            temp[i][j][0] = savedTestPattern3dPointsList[i][3*j][0];
                            temp[i][j][1] = savedTestPattern3dPointsList[i][3*j+1][0];
                            temp[i][j][2] = savedTestPattern3dPointsList[i][3*j+2][0];
                        }
                    }
                    savedTestPattern3dPointsList = temp;
                }
            }

            if (savedTestPatternImagePointsList != null) {
                if (savedTestPatternImagePointsList[0][0].length == 1) {
                    int numberOfPatterns = savedTestPatternImagePointsList.length;
                    double[][][] temp = new double[numberOfPatterns][][];
                    for (int i=0; i<numberOfPatterns; i++) {
                        int numberOfPoints = savedTestPatternImagePointsList[i].length / 2;
                        temp[i] = new double[numberOfPoints][2];
                        for (int j=0; j<numberOfPoints; j++) {
                            temp[i][j][0] = savedTestPatternImagePointsList[i][2*j][0];
                            temp[i][j][1] = savedTestPatternImagePointsList[i][2*j+1][0];
                        }
                    }
                    savedTestPatternImagePointsList = temp;
                }
            }
        }
        
        @Persist
        private void persist() {
            super.persist();
            virtualCameraMatrix.get(0, 0, virtualCameraMatrix_cArr);
            rectification.get(0, 0, rectification_cArr);
            rotate_m_c.get(0, 0, rotate_m_cArr);
            vect_c_m_c.get(0, 0, vect_c_m_cArr);
            vect_m_c_m.get(0, 0, vect_m_c_mArr);
            unit_c_cz_m.get(0, 0, unit_c_cz_mArr);
            vect_m_cHat_m.get(0, 0, vect_m_cHat_mArr);
            vect_c_newPrincipalPoint_c.get(0, 0, vect_c_newPrincipalPoint_cArr);
        }

        /**
         * @return the calibrationRig
         */
        public Part getCalibrationRig() {
            return calibrationRig;
        }

        /**
         * @param calibrationRig the calibrationRig to set
         */
        public void setCalibrationRig(Part calibrationRig) {
            this.calibrationRig = calibrationRig;
            calibrationRigId = calibrationRig.getId();
        }

        /**
         * @return the calibrationRigId
         */
        public String getCalibrationRigId() {
            return calibrationRigId;
        }

        /**
         * @param calibrationRigId the calibrationRigId to set
         */
        public void setCalibrationRigId(String calibrationRigId) {
            this.calibrationRigId = calibrationRigId;
            calibrationRig = Configuration.get().getPart(calibrationRigId);
        }

        /**
         * @return the virtual camera matrix
         */
        public Mat getVirtualCameraMatrix() {
            return virtualCameraMatrix;
        }

        /**
         * @param virtualCameraMatrix - the virtual camera matrix to set
         */
        public void setVirtualCameraMatrix(Mat virtualCameraMatrix) {
            this.virtualCameraMatrix = virtualCameraMatrix.clone();
        }

        /**
         * @return the rectification matrix
         */
        public Mat getRectification() {
            return rectification;
        }

        /**
         * @param rectification - the rectification matrix to set
         */
        public void setRectify(Mat rectification) {
            this.rectification = rectification.clone();
        }

        public Mat getRotate_m_c() {
            return rotate_m_c;
        }

        public void setRotate_m_c(Mat rotate_m_c) {
            this.rotate_m_c = rotate_m_c.clone();
        }

        public Mat getVect_c_m_c() {
            return vect_c_m_c;
        }

        public void setVect_c_m_c(Mat vect_c_m_c) {
            this.vect_c_m_c = vect_c_m_c.clone();
        }

        /**
         * @return the vect_m_c_m
         */
        public Mat getVect_m_c_m() {
            return vect_m_c_m;
        }

        /**
         * @param vect_m_c_m the vect_m_c_m to set
         */
        public void setVect_m_c_m(Mat vect_m_c_m) {
            this.vect_m_c_m = vect_m_c_m;
        }

        /**
         * @return the unit_c_cz_m
         */
        public Mat getUnit_c_cz_m() {
            return unit_c_cz_m;
        }

        /**
         * @param unit_c_cz_m the unit_c_cz_m to set
         */
        public void setUnit_c_cz_m(Mat unit_c_cz_m) {
            this.unit_c_cz_m = unit_c_cz_m;
        }

        /**
         * @return the vect_m_cHat_m
         */
        public Mat getVect_m_cHat_m() {
            return vect_m_cHat_m;
        }

        /**
         * @param vect_m_cHat_m the vect_m_cHat_m to set
         */
        public void setVect_m_cHat_m(Mat vect_m_cHat_m) {
            this.vect_m_cHat_m = vect_m_cHat_m;
        }

        /**
         * @return the vect_c_newPrincipalPoint_c
         */
        public Mat getVect_c_newPrincipalPoint_c() {
            return vect_c_newPrincipalPoint_c;
        }

        /**
         * @param vect_c_newPrincipalPoint_c the vect_c_newPrincipalPoint_c to set
         */
        public void setVect_c_newPrincipalPoint_c(Mat vect_c_newPrincipalPoint_c) {
            this.vect_c_newPrincipalPoint_c = vect_c_newPrincipalPoint_c;
        }

        /**
         * @return the calibrationPatternZ
         */
        public double[] getCalibrationPatternZ() {
            return calibrationPatternZ;
        }

        /**
         * @param calibrationPatternZ the calibrationPatternZ to set
         */
        public void setCalibrationPatternZ(double[] calibrationPatternZ) {
            this.calibrationPatternZ = calibrationPatternZ;
        }

        /**
         * @return the zRotationError
         */
        public double getzRotationError() {
            return zRotationError;
        }

        /**
         * @param zRotationError the zRotationError to set
         */
        public void setzRotationError(double zRotationError) {
            this.zRotationError = zRotationError;
        }

        /**
         * @return the yRotationError
         */
        public double getyRotationError() {
            return yRotationError;
        }

        /**
         * @param yRotationError the yRotationError to set
         */
        public void setyRotationError(double yRotationError) {
            this.yRotationError = yRotationError;
        }

        /**
         * @return the xRotationError
         */
        public double getxRotationError() {
            return xRotationError;
        }

        /**
         * @param xRotationError the xRotationError to set
         */
        public void setxRotationError(double xRotationError) {
            this.xRotationError = xRotationError;
        }

        public void processRawCalibrationData(Size size, Length defaultZ) {
            processRawCalibrationData(savedTestPattern3dPointsList, 
                    savedTestPatternImagePointsList, size, defaultZ);
        }
        
        public void processRawCalibrationData(double[][][] testPattern3dPoints, 
                double[][][] testPatternImagePoints, Size size, Length defaultZ) {
            
            savedTestPattern3dPointsList = testPattern3dPoints;
            savedTestPatternImagePointsList = testPatternImagePoints;

            int numberOfTestPatterns = Math.min(testPattern3dPoints.length, 
                    testPatternImagePoints.length);
            
            //Setting one or more bits in this bit mapped field will cause the corresponding test
            //pattern to be skipped.  This is purely for debugging purposes as normally all
            //collected test patterns should be processed.
            int testPatternsToSkip = 0; 
            
            //Count the number of test patterns to actually use
            int numberOfTestPatternsToUse = 0;
            int testBit = 1;
            for (int i=0; i<numberOfTestPatterns; i++) {
                if ((testPatternsToSkip & testBit) == 0) {
                    numberOfTestPatternsToUse++;
                }
                testBit <<= 1; //shift left
            }
            
            //Copy only those test patterns to use
            double[][][] testPattern3dPointsToUse = new double[numberOfTestPatternsToUse][][];
            double[][][] testPatternImagePointsToUse = new double[numberOfTestPatternsToUse][][];
            double[] testPatternZ = new double[numberOfTestPatternsToUse];
            testBit = 1;
            int iTP = 0;
            for (int i=0; i<numberOfTestPatterns; i++) {
                if ((testPatternsToSkip & testBit) == 0) {
                    testPattern3dPointsToUse[iTP] = new double[testPattern3dPoints[i].length][];
                    System.arraycopy((Object)testPattern3dPoints[i], 0,
                            (Object)testPattern3dPointsToUse[iTP], 0, testPattern3dPoints[i].length);
                    testPatternImagePointsToUse[iTP] = new double[testPatternImagePoints[i].length][];
                    System.arraycopy((Object)testPatternImagePoints[i], 0, 
                            (Object)testPatternImagePointsToUse[iTP], 0, 
                            testPatternImagePoints[i].length);
                    testPatternZ[iTP] = testPattern3dPoints[i][0][2];
                    iTP++;
                }
                testBit <<= 1; //shift left
            }
            testPattern3dPoints = testPattern3dPointsToUse;
            testPatternImagePoints = testPatternImagePointsToUse;
            numberOfTestPatterns = numberOfTestPatternsToUse;
            
            //Create lists of Mats to pass to Calib3d.calibrateCamera
            //Note that the Z component of all the test patterns is set to zero so the camera
            //will appear as if it is changing height rather than the pattern.  That will be
            //corrected for when the tvecs are processed later.
            List<Mat> testPattern3dPointsList = new ArrayList<>();
            List<Mat> testPatternImagePointsList = new ArrayList<>();
            for (int iTestPattern=0; iTestPattern<numberOfTestPatterns; iTestPattern++) {
                double[][] tpArray = testPattern3dPoints[iTestPattern];
                Mat tp3dMat = new Mat();
                for (int ptIdx=0; ptIdx<tpArray.length; ptIdx++) {
                    Point3 pt3 = new Point3(tpArray[ptIdx]);
                    pt3.z = 0; //set Z coordinate to zero
                    tp3dMat.push_back(new MatOfPoint3f(pt3));
                }
                testPattern3dPointsList.add(tp3dMat);
                
                tpArray = testPatternImagePoints[iTestPattern];
                Mat tp2dMat = new Mat();
                for (int ptIdx=0; ptIdx<tpArray.length; ptIdx++) {
                    tp2dMat.push_back(new MatOfPoint2f(new Point(tpArray[ptIdx])));
                }
                testPatternImagePointsList.add(tp2dMat);
            }

            List<Mat> rvecs = new ArrayList<>();
            List<Mat> tvecs = new ArrayList<>();
            
            cameraMatrix.release();
            cameraMatrix = Mat.eye(3, 3, CvType.CV_64FC1);
//            cameraMatrix.put(0, 0, 1000);
//            cameraMatrix.put(1, 1, 1000);
            cameraMatrix.put(0, 2, (size.width - 1.0)/2.0);
            cameraMatrix.put(1, 2, (size.height - 1.0)/2.0);
            distortionCoefficients.release();
            distortionCoefficients = Mat.zeros(5, 1, CvType.CV_64FC1);
            
            //OpenCV's Calib3d.calibrateCamera is used to get an initial estimate of the intrinsic 
            //and extrinsic camera parameters.  Note that the camera model used by 
            //Calib3d.calibrateCamera is quite general and has more degrees of freedom than is 
            //necessary for modeling openpnp cameras.  For instance, it assumes the camera can
            //be rotated and translated differently relative each test pattern.  But in actuality, 
            //for openpnp, the camera has a fixed rotation relative to all test patterns and the 
            //camera Z coordinate (wrt to the machine) is fixed during the collection of all test 
            //patterns.  Because it has more degrees of freedom than necessary, the RMS error of the
            //model fit may appear good but parameter estimates may in fact be significantly in
            //error.
            double rms = Calib3d.calibrateCamera(testPattern3dPointsList, 
                    testPatternImagePointsList, size,
                    cameraMatrix, distortionCoefficients, rvecs, tvecs, 
                    Calib3d.CALIB_FIX_PRINCIPAL_POINT );
            Logger.trace("Calib3d.calibrateCamera rms = " + rms);
            
            for (Mat tp : testPattern3dPointsList) {
                tp.release();
            }
            for (Mat tp : testPatternImagePointsList) {
                tp.release();
            }
            
            Logger.trace("cameraMatrix = " + cameraMatrix.dump());
            Logger.trace("distortionCoefficients = " + distortionCoefficients.dump());

            //Setup and populate an array of camera parameters with an initial guess based on the
            //solution obtained by Calib3d.calibrateCamera
            double[] cameraParams = new double[13+2*numberOfTestPatterns];
            cameraParams[0] = cameraMatrix.get(0, 0)[0];
            cameraParams[1] = cameraMatrix.get(1, 1)[0];
            cameraParams[2] = cameraMatrix.get(0, 2)[0];
            cameraParams[3] = cameraMatrix.get(1, 2)[0];
            cameraParams[4] = distortionCoefficients.get(0, 0)[0];
            cameraParams[5] = distortionCoefficients.get(1, 0)[0];
            cameraParams[6] = distortionCoefficients.get(2, 0)[0];
            cameraParams[7] = distortionCoefficients.get(3, 0)[0];
            cameraParams[8] = distortionCoefficients.get(4, 0)[0];

            //For openpnp, since the physical camera can't rotate relative to the machine axis, all
            //of the rotation vectors should be exactly the same.  But due to the extra degrees of 
            //freedom discussed above, they may not be exactly the same so we'll just average them
            //to get an estimate of the one rotation vector.
            Mat averageRvec = Mat.zeros(3, 1, CvType.CV_64FC1);
            double weight = 1.0/rvecs.size();
            for (Mat rvec : rvecs) {
                Core.addWeighted(rvec, weight, averageRvec, 1, 0, averageRvec);
                Logger.trace("rvec = " + rvec.dump());
            }
            cameraParams[9] = averageRvec.get(0, 0)[0];
            cameraParams[10] = averageRvec.get(1, 0)[0];
            cameraParams[11] = averageRvec.get(2, 0)[0];
            averageRvec.release();
            
            //tvecs contains the vector from the camera origin to the test pattern origin with 
            //components expressed in the camera reference system.  Since the Z coordinate
            //of the test patterns were all set to zero before being passed to calibrateCamera, the
            //true Z coordinate of the test patterns must be accounted for here. 
            weight = 1.0/tvecs.size();
            int iTestPattern = 0;
            for (Mat vect_c_tp_c : tvecs) {
                Logger.trace("vect_c_tp_c = " + vect_c_tp_c.dump());
                
                //Construct a vector from the machine origin to the test pattern origin with
                //component in the machine reference system
                Mat vect_m_tp_m = Mat.zeros(3, 1, CvType.CV_64FC1);
                vect_m_tp_m.put(2, 0, testPatternZ[iTestPattern]);
                Logger.trace("vect_m_tp_m = " + vect_m_tp_m.dump());
                
                //Convert the applicable rotation vector to a rotation matrix
                Calib3d.Rodrigues(rvecs.get(iTestPattern), rotate_m_c);
                rvecs.get(iTestPattern).release();
                Logger.trace("rotate_m_c = " + rotate_m_c.dump());

                //Construct a vector from the machine origin to the camera origin with components in 
                //the machine reference system: vect_m_c_m = vect_m_tp_m - rotate_m_c.t() * vect_c_tp_c
                Mat tempVect_m_c_m = Mat.zeros(3, 1, CvType.CV_64FC1);
                Core.gemm(rotate_m_c.t(), vect_c_tp_c, -1, vect_m_tp_m, 1, tempVect_m_c_m);
                vect_c_tp_c.release();
                vect_m_tp_m.release();
                Logger.trace("tempVect_m_c_m = " + tempVect_m_c_m.dump());
                
                //Ideally, all the tempVect_m_c_m vectors should have the same z component but again
                //due to the extra degrees of freedom discussed above, they may be different so 
                //average the Z component to estimate the true Z component
                cameraParams[12] += weight*tempVect_m_c_m.get(2,  0)[0];         //Z
                
                //The X and Y components can legitimately be different due to accidental horizontal
                //offsets introduced when the calibration rig was changed in height
                cameraParams[13 + 2*iTestPattern] = tempVect_m_c_m.get(0, 0)[0]; //X
                cameraParams[14 + 2*iTestPattern] = tempVect_m_c_m.get(1, 0)[0]; //Y
                tempVect_m_c_m.release();
                
                iTestPattern++;
            }
            
            //Compute a new set of camera parameters based on openpnp's more restrictive camera
            //model that avoids the excess degree of freedom problem discussed above
            cameraParams = CameraCalibrationUtils.ComputeBestCameraParameters(
                    testPattern3dPoints, testPatternImagePoints, cameraParams, 
                    CameraCalibrationUtils.FIX_PRINCIPAL_POINT);
            
            //Use the new estimates for the physical camera's matrix
            cameraMatrix.put(0, 0, cameraParams[0]);
            cameraMatrix.put(1, 1, cameraParams[1]);
            cameraMatrix.put(0, 2, cameraParams[2]);
            cameraMatrix.put(1, 2, cameraParams[3]);
            Logger.trace("cameraMatrix = " + cameraMatrix.dump());
            
            //Use the new estimates for the physical camera's lens distortion coefficients
            distortionCoefficients.put(0, 0, cameraParams[4]);
            distortionCoefficients.put(1, 0, cameraParams[5]);
            distortionCoefficients.put(2, 0, cameraParams[6]);
            distortionCoefficients.put(3, 0, cameraParams[7]);
            distortionCoefficients.put(4, 0, cameraParams[8]);
            Logger.trace("distortionCoefficients = " + distortionCoefficients.dump());
            
            //Convert the new estimate of the rotation vector to a rotation matrix that transforms
            //vectors with components represented in the machine reference frame to those 
            //represented in the camera's reference frame
            Mat rvec = Mat.zeros(3, 1, CvType.CV_64FC1);
            rvec.put(0,  0, cameraParams[9]);
            rvec.put(1,  0, cameraParams[10]);
            rvec.put(2,  0, cameraParams[11]);
            Calib3d.Rodrigues(rvec, rotate_m_c);
            rvec.release();
            Logger.trace("rotate_m_c = " + rotate_m_c.dump());
            
            //The camera x/y position is set to that determined by the first test pattern.  For top
            //cameras this isn't terribly important (as long as all top cameras are handled the
            //same) but for bottom cameras, the camera position needs to be set by the test pattern
            //that was collected at defaultZ.  Otherwise, if the nozzle's Z axis is not orthogonal
            //to the X/Y plane, a horizontal error would be introduced. 
            vect_m_c_m.release();
            vect_m_c_m = Mat.zeros(3, 1, CvType.CV_64FC1);
            vect_m_c_m.put(2, 0, cameraParams[12]); //Z
            vect_m_c_m.put(0, 0, cameraParams[13]); //X
            vect_m_c_m.put(1, 0, cameraParams[14]); //Y
            Logger.trace("vect_m_c_m = " + vect_m_c_m.dump());
            
            //Create a unit Z vector
            Mat unitZ = Mat.zeros(3, 1, CvType.CV_64FC1);
            unitZ.put(2, 0, 1);
            Logger.trace("unitZ = " + unitZ.dump());
            
            //Construct a unit vector along the camera's positive z axis with components 
            //represented in the machine reference system
            unit_c_cz_m.release();
            unit_c_cz_m = Mat.zeros(3, 1, CvType.CV_64FC1);
            Core.gemm(rotate_m_c.t(), unitZ, 1, unitZ, 0, unit_c_cz_m);
            Logger.trace("unit_c_cz_m = " + unit_c_cz_m.dump());
            
            //Construct a unit vector in the direction of the machine's positive z axis with 
            //components represented in the camera reference system
            Mat unit_c_mz_c = Mat.zeros(3, 1, CvType.CV_64FC1);
            Core.gemm(rotate_m_c, unitZ, 1, unitZ, 0, unit_c_mz_c);
            Logger.trace("unit_c_mz_c = " + unit_c_mz_c.dump());
            
            //Compute the Z offset from the camera to the default Z plane
            double cameraToZPlane = defaultZ.convertToUnits(LengthUnit.Millimeters).getValue() - 
                    vect_m_c_m.get(2, 0)[0];
            Logger.trace("cameraToZPlane = " + cameraToZPlane);
            
            //Construct a vector from the camera origin to the nearest point on the defaultZ plane
            //with components in the camera reference system
            Mat vect_c_defaultZ_c = Mat.zeros(3, 1, CvType.CV_64FC1);
            Core.multiply(unit_c_mz_c, new Scalar(cameraToZPlane/unit_c_mz_c.get(2, 0)[0]), 
                    vect_c_defaultZ_c);
            Logger.trace("vect_c_defaultZ_c = " + vect_c_defaultZ_c.dump());
            
            //Normalize the vector to find the new principal point
            Core.multiply(vect_c_defaultZ_c, new Scalar(1.0/vect_c_defaultZ_c.get(2, 0)[0]), 
                    vect_c_newPrincipalPoint_c);
            vect_c_defaultZ_c.release();
            Logger.trace("vect_c_newPrincipalPoint_c = " + vect_c_newPrincipalPoint_c.dump());

            //Construct a vector from the camera origin to the intersection of the camera 
            //Z-axis and the default Z plane with components in the machine reference system
            Mat vect_c_defaultZPrincipalPoint_m = Mat.zeros(3, 1, CvType.CV_64FC1);
            Core.multiply(unit_c_cz_m, new Scalar(cameraToZPlane/unit_c_cz_m.get(2, 0)[0]), 
                    vect_c_defaultZPrincipalPoint_m);
            Logger.trace("vect_c_defaultZPrincipalPoint_m = " + vect_c_defaultZPrincipalPoint_m.dump());

            //Construct a vector from the machine origin to the intersection of the camera 
            //Z-axis and the default Z plane with components in the machine reference system
            Mat vect_m_defaultZPrincipalPoint_m = Mat.zeros(3, 1, CvType.CV_64FC1);
            Core.add(vect_m_c_m, vect_c_defaultZPrincipalPoint_m, vect_m_defaultZPrincipalPoint_m);
            Logger.trace("vect_m_defaultZPrincipalPoint_m = " + vect_m_defaultZPrincipalPoint_m.dump());
            
            //Compute the absolute distance from the camera origin to the default Z plane principal 
            //point
            double absoluteCameraToDefaultZPrincipalPointDistance = 
                    Core.norm(vect_c_defaultZPrincipalPoint_m, Core.NORM_L2);  
            Logger.trace("absoluteCameraToDefaultZPrincipalPointDistance = " +
                    absoluteCameraToDefaultZPrincipalPointDistance);
            
            //Compute the vector from the machine origin to the virtual camera's origin with
            //components in the machine reference system.  The virtual camera is centered above and
            //is looking straight down onto the default Z principal point.  This may be intuitive 
            //for top cameras, but this is also desired for bottom cameras as this will make the 
            //image appear as if the bottom of the part was taken from above through the top of the 
            //part by an x-ray camera (which is desired).
            vect_m_cHat_m.release();
//            vect_m_cHat_m = vect_m_defaultZPrincipalPoint_m.clone();
            vect_m_cHat_m = vect_m_c_m.clone();
            vect_m_cHat_m.put(2, 0, defaultZ.convertToUnits(LengthUnit.Millimeters).getValue() + 
                    absoluteCameraToDefaultZPrincipalPointDistance);
            vect_m_defaultZPrincipalPoint_m.release();
            Logger.trace("vect_m_cHat_m = " + vect_m_cHat_m.dump());
            
            //Construct the rotation matrix that converts vectors represented in the machine reference
            //system to vectors represented in the virtual camera's reference system (X-axis aligned
            //with the machine's X-axis, Y and Z-axis in the opposite direction than the machine's 
            //Y and Z-axis).
            Mat rotate_m_cHat = Mat.zeros(3, 3, CvType.CV_64FC1);
            rotate_m_cHat.put(0,  0,  1);
            rotate_m_cHat.put(1,  1,  -1);
            rotate_m_cHat.put(2,  2, -1);
            
            //Compute the rotation matrix that converts vector components from physical camera 
            //coordinate system to the virtual camera coordinate system
            Mat rotate_cHat_c = Mat.eye(3, 3, CvType.CV_64FC1);
            //rotate_cHat_c = rotate_m_c * rotate_m_cHat.t()
            Core.gemm(rotate_m_c, rotate_m_cHat.t(), 1, rotate_m_c, 0, rotate_cHat_c);
            Logger.trace("rotate_cHat_c = " + rotate_cHat_c.dump());
            zRotationError = -Math.toDegrees(Math.atan2(rotate_cHat_c.get(0, 1)[0], 
                    rotate_cHat_c.get(0, 0)[0]));
            while (zRotationError > 45) zRotationError -= 90;
            while (zRotationError < -45) zRotationError += 90;
            yRotationError = Math.toDegrees(Math.asin(rotate_cHat_c.get(0, 2)[0]));
            while (yRotationError > 45) yRotationError -= 90;
            while (yRotationError < -45) yRotationError += 90;
            xRotationError = Math.toDegrees(Math.atan2(rotate_cHat_c.get(1, 2)[0], 
                    rotate_cHat_c.get(2, 2)[0]));
            while (xRotationError > 45) xRotationError -= 90;
            while (xRotationError < -45) xRotationError += 90;
            rotate_cHat_c.release();
            
            Logger.trace("Physical camera rotational errors from ideal (as measured by the right hand rule about each machine axis):");
            Logger.trace("Z axis rotational error = {} degrees", zRotationError);
            Logger.trace("Y axis rotational error = {} degrees", yRotationError);
            Logger.trace("X axis rotational error = {} degrees", xRotationError);
            
            //Construct the rectification matrix
            rectification = CameraCalibrationUtils.computeRectificationMatrix(rotate_m_c, 
                    vect_m_c_m, rotate_m_cHat, vect_m_cHat_m, 
                    defaultZ.convertToUnits(LengthUnit.Millimeters).getValue());
            Logger.trace("rectification = " + rectification.dump());
            
            //Cleanup
            rotate_m_cHat.release();
            unitZ.release();
             
            enabled = true;
        }
        
        protected void initUndistortRectifyMap(Size size, double alpha, Mat undistortionMap1, 
                Mat undistortionMap2) {
            virtualCameraMatrix = CameraCalibrationUtils.computeVirtualCameraMatrix(cameraMatrix, 
                    distortionCoefficients, rectification, size, alpha, vect_c_newPrincipalPoint_c);
            Logger.trace("virtualCameraMatrix = " + virtualCameraMatrix.dump());

            Calib3d.initUndistortRectifyMap(cameraMatrix,
                distortionCoefficients, rectification,
                virtualCameraMatrix, size, CvType.CV_32FC1,
                undistortionMap1, undistortionMap2);
        }
        

        public Location getOffsetAtZ(Location location) {
            return getOffsetAtZ(location.getLengthZ());
        }
        
        public Location getOffsetAtZ(Length lengthZ) {
            double z = lengthZ.convertToUnits(LengthUnit.Millimeters).getValue();
            
            double cameraToZPlane = z - vect_m_c_m.get(2, 0)[0];
            
            Mat vect_c_p_m = Mat.zeros(3, 1, CvType.CV_64FC1);
            Core.multiply(unit_c_cz_m, new Scalar(cameraToZPlane/unit_c_cz_m.get(2, 0)[0]), vect_c_p_m);
//            Logger.trace("vect_c_p_m = " + vect_c_p_m.dump());
            
            Location offset = new Location(LengthUnit.Millimeters, vect_c_p_m.get(0, 0)[0], 
                    vect_c_p_m.get(1, 0)[0], vect_c_p_m.get(2, 0)[0], 0).convertToUnits(lengthZ.getUnits());
            
            vect_c_p_m.release();
            
            return offset;
        }
        
        protected Length getDistanceToCameraAtZ(Length lengthZ) {
            Location p = getOffsetAtZ(lengthZ);
            return new Length(p.getXyzDistanceTo(new Location(LengthUnit.Millimeters, 0, 0, 0, 0)), 
                    p.getUnits());
        }
        
    }
    
    public static class LensCalibrationParams extends AbstractModelObject {
        @Attribute(required = false)
        protected boolean enabled = false;

        @Element(name = "cameraMatrix", required = false)
        private double[] cameraMatrixArr = new double[9];

        @Element(name = "distortionCoefficients", required = false)
        private double[] distortionCoefficientsArr = new double[5];

        protected Mat cameraMatrix = new Mat(3, 3, CvType.CV_64FC1);
        protected Mat distortionCoefficients = new Mat(5, 1, CvType.CV_64FC1);

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
            Object oldValue = this.isEnabled();
            this.enabled = enabled;
            firePropertyChange("enabled", oldValue, enabled);
        }

        public Mat getCameraMatrixMat() {
            return cameraMatrix;
        }

        public void setCameraMatrixMat(Mat cameraMatrix) {
            this.cameraMatrix = cameraMatrix.clone();
        }

        public Mat getDistortionCoefficientsMat() {
            return distortionCoefficients;
        }

        public void setDistortionCoefficientsMat(Mat distortionCoefficients) {
            this.distortionCoefficients = distortionCoefficients.clone();
        }
    }

    @Override
    public void findIssues(Solutions solutions) {
        super.findIssues(solutions);
        if (solutions.isTargeting(Milestone.Vision)) {
            if (getLooking() == Looking.Up
                    && isFlipX() == isFlipY()
                    && ! (this instanceof SimulatedUpCamera)) {
                solutions.add(new Solutions.PlainIssue(
                        this, 
                        "An up-looking camera should usually mirror the image.", 
                        "Enable either Flip X or Flip Y (but not both) in the camera's Image Transforms.", 
                        Severity.Warning,
                        "https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration:-General-Camera-Setup#set-rotation-and-transforms"));
            }
            /*duplicate
            if (getUnitsPerPixel().getX() == 0 && getUnitsPerPixel().getY() == 0) {
                solutions.add(new Solutions.PlainIssue(
                        this, 
                        "Units per pixel are not yet set.", 
                        "Perform the Units Per Pixel measurement in the General Configuration tab .", 
                        Severity.Error,
                        "https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration:-General-Camera-Setup#set-units-per-pixel"));
            }
            */
            final double previewFps = getPreviewFps();
            if (previewFps > 15) {
                solutions.add(new Solutions.Issue(
                        this, 
                        "A high Preview FPS value might create undue CPU load.", 
                        "Set to 5 FPS.", 
                        Severity.Suggestion,
                        "https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration:-General-Camera-Setup#general-configuration") {


                    @Override
                    public void setState(Solutions.State state) throws Exception {
                        setPreviewFps((state == Solutions.State.Solved) ? 5.0 : previewFps);
                        super.setState(state);
                    }
                });
            }
            if (! isSuspendPreviewInTasks()) {
                solutions.add(new Solutions.Issue(
                        this, 
                        "It is recommended to suspend camera preview during machine tasks / Jobs.", 
                        "Enable Suspend during tasks.", 
                        Severity.Suggestion,
                        "https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration:-General-Camera-Setup#general-configuration") {

                    @Override
                    public void setState(Solutions.State state) throws Exception {
                        setSuspendPreviewInTasks((state == Solutions.State.Solved));
                        super.setState(state);
                    }
                });
            }
            if (! isAutoVisible()) {
                solutions.add(new Solutions.Issue(
                        this, 
                        "In single camera preview OpenPnP can automatically switch the camera for you.", 
                        "Enable Auto Camera View.", 
                        Severity.Suggestion,
                        "https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration:-General-Camera-Setup#general-configuration") {

                    @Override
                    public void setState(Solutions.State state) throws Exception {
                        setAutoVisible((state == Solutions.State.Solved));
                        super.setState(state);
                    }
                });
            }
            CameraPanel cameraPanel = MainFrame.get().getCameraViews();
            CameraView view = cameraPanel.getCameraView(this);
            if (view != null) {
                final RenderingQuality renderingQuality = view.getRenderingQuality();
                if (renderingQuality.ordinal() < RenderingQuality.High.ordinal()) {
                    solutions.add(new Solutions.Issue(
                            this, 
                            "The preview rendering quality can be improved.", 
                            "Set to Rendering Quality to High (right click the Camera View to see other options).", 
                            Severity.Suggestion,
                            "https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration:-General-Camera-Setup#camera-view-configuration") {

                        @Override
                        public void setState(Solutions.State state) throws Exception {
                            view.setRenderingQuality((state == Solutions.State.Solved) ? RenderingQuality.High : renderingQuality);
                            cameraViewHasChanged(null);
                            super.setState(state);
                        }
                    });
                }
            }
        }
    }

    /**
     * Create a replacement OpenPnpCaptureCamera for this camera with some of the
     * generic settings transferred.  
     * 
     * @return
     */
    protected OpenPnpCaptureCamera createReplacementCamera() {
        OpenPnpCaptureCamera camera = new OpenPnpCaptureCamera();
        camera.setHead(getHead());
        camera.setId(getId());
        camera.setLooking(getLooking());
        camera.setName(getName());
        camera.setHeadOffsets(getHeadOffsets());
        camera.setAxisX(getAxisX());
        camera.setAxisY(getAxisY());
        camera.setAxisZ(getAxisZ());
        camera.setAxisRotation(getAxisRotation());
        camera.setPreviewFps(getPreviewFps());
        camera.setSuspendPreviewInTasks(isSuspendPreviewInTasks());
        camera.setAutoVisible(isAutoVisible());
        camera.setLightActuator(getLightActuator());
        camera.setAllowMachineActuators(isAllowMachineActuators());
        camera.setBeforeCaptureLightOn(isBeforeCaptureLightOn());
        camera.setAfterCaptureLightOff(isAfterCaptureLightOff());
        camera.setUserActionLightOn(isUserActionLightOn());
        camera.setAntiGlareLightOff(isAntiGlareLightOff());
        return camera;
    }

    /**
     * Replace a camera with the same Id at the same place in the cameras list.
     * 
     * @param camera
     * @throws Exception
     */
    public static void replaceCamera(Camera camera) throws Exception {
        // Disable the machine, so the driver isn't connected.
        Machine machine = Configuration.get().getMachine();
        // Find the old driver with the same Id.
        List<Camera> list = (camera.getHead() == null ? machine.getCameras() : camera.getHead().getCameras());
        Camera replaced = null;
        int index;
        for (index = 0; index < list.size(); index++) {
            if (list.get(index).getId().equals(camera.getId())) {
                replaced = list.get(index);
                if (camera instanceof AbstractBroadcastingCamera) {
                    ((AbstractBroadcastingCamera) replaced).stop();
                }
                if (replaced.getHead() == null) {
                    machine.removeCamera(replaced);
                }
                else {
                    replaced.getHead().removeCamera(replaced);
                }
                break;
            }
        }
        // Add the new one.
        if (replaced.getHead() == null) {
            machine.addCamera(camera);
        }
        else {
            replaced.getHead().addCamera(camera);
        }
        // Permutate it back to the old list place (cumbersome but works).
        for (int p = list.size()-index; p > 1; p--) {
            if (replaced.getHead() == null) {
                machine.permutateCamera(camera, -1);
            }
            else {
                replaced.getHead().permutateCamera(camera, -1);
            }
        }
        if (camera instanceof AbstractBroadcastingCamera) {
            ((AbstractBroadcastingCamera) camera).reinitialize();
        }
    }
}
