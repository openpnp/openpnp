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
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Size;
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
import org.openpnp.machine.reference.wizards.ReferenceCameraPositionConfigurationWizard;
import org.openpnp.machine.reference.wizards.ReferenceCameraTransformsConfigurationWizard;
import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Solutions;
import org.openpnp.model.Solutions.Milestone;
import org.openpnp.model.Solutions.Severity;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Camera;
import org.openpnp.spi.FocusProvider;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.util.Collect;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.vision.LensCalibration;
import org.openpnp.vision.LensCalibration.LensModel;
import org.openpnp.vision.LensCalibration.Pattern;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
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
     */
    @Override
    public BufferedImage capture() {
        try {
            Map<String, Object> globals = new HashMap<>();
            globals.put("camera", this);
            Configuration.get().getScripting().on("Camera.BeforeCapture", globals);
        }
        catch (Exception e) {
            Logger.warn(e);
        }
        BufferedImage image = captureTransformed();
        try {
            Map<String, Object> globals = new HashMap<>();
            globals.put("camera", this);
            Configuration.get().getScripting().on("Camera.AfterCapture", globals);
        }
        catch (Exception e) {
            Logger.warn(e);
        }
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

            // We do skip the convert to and from Mat if no transforms are needed.
            // But we must enter while calibrating. 
            if (isDeinterlaced()
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

    protected void clearCalibrationCache() {
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

    @Override
    public PropertySheet[] getPropertySheets() {
        PropertySheet[] sheets = new PropertySheet[] {
                new PropertySheetWizardAdapter(new CameraConfigurationWizard(this), "General Configuration"),
                new PropertySheetWizardAdapter(new CameraVisionConfigurationWizard(this), "Vision"),
                new PropertySheetWizardAdapter(getConfigurationWizard(), "Device Settings"),
                new PropertySheetWizardAdapter(new ReferenceCameraPositionConfigurationWizard(getMachine(), this), "Position"),
                new PropertySheetWizardAdapter(new ReferenceCameraCalibrationConfigurationWizard(this), "Lens Calibration"),
                new PropertySheetWizardAdapter(new ReferenceCameraTransformsConfigurationWizard(this), "Image Transforms")
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

    public static class LensCalibrationParams extends AbstractModelObject {
        @Attribute(required = false)
        private boolean enabled = false;

        @Element(name = "cameraMatrix", required = false)
        private double[] cameraMatrixArr = new double[9];

        @Element(name = "distortionCoefficients", required = false)
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
