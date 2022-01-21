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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
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
import org.openpnp.gui.support.WizardUtils;
import org.openpnp.gui.wizards.CameraConfigurationWizard;
import org.openpnp.gui.wizards.CameraVisionConfigurationWizard;
import org.openpnp.machine.reference.camera.AutoFocusProvider;
import org.openpnp.machine.reference.camera.OpenPnpCaptureCamera;
import org.openpnp.machine.reference.camera.calibration.AdvancedCalibration;
import org.openpnp.machine.reference.camera.calibration.LensCalibrationParams;
import org.openpnp.machine.reference.camera.wizards.ReferenceCameraWhiteBalanceConfigurationWizard;
import org.openpnp.machine.reference.solutions.ActuatorSolutions;
import org.openpnp.machine.reference.wizards.ReferenceCameraCalibrationConfigurationWizard;
import org.openpnp.machine.reference.wizards.ReferenceCameraCalibrationWizard;
import org.openpnp.machine.reference.wizards.ReferenceCameraPositionConfigurationWizard;
import org.openpnp.machine.reference.wizards.ReferenceCameraTransformsConfigurationWizard;
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
import org.openpnp.util.SimpleGraph;
import org.openpnp.util.UiUtils;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.LensCalibration;
import org.openpnp.vision.LensCalibration.LensModel;
import org.openpnp.vision.LensCalibration.Pattern;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

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
    protected double redBalance = 1.0; 

    @Attribute(required = false)
    protected double greenBalance = 1.0; 

    @Attribute(required = false)
    protected double blueBalance = 1.0; 

    @Attribute(required = false)
    protected double redGamma = 1.0; 

    @Attribute(required = false)
    protected double greenGamma = 1.0; 

    @Attribute(required = false)
    protected double blueGamma = 1.0; 

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

    @Attribute(required = false)
    private double whiteBalanceLeadFractile = 0.8;

    @Attribute(required = false)
    private double whiteBalanceClipFractile = 0.99;

    @Attribute(required = false)
    private double whiteBalanceGammaFractile = 0.5;

    @Element(required = false)
    private double[] redColorMap;
    @Element(required = false)
    private double[] greenColorMap;
    @Element(required = false)
    private double[] blueColorMap;


    private boolean calibrating;
    private CalibrationCallback calibrationCallback;
    private int calibrationCountGoal = 25;

    private Mat undistortionMap1;
    private Mat undistortionMap2;
    private Mat lut;

    private LensCalibration lensCalibration;

    private Actuator lightActuator;

    private int undistortionMappingSemaphore = 0; //-1:writing, 0:free, >0:reading

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
    public Location getCalibratedHeadOffsets(LocationOption... options) {
        if (! Arrays.asList(options).contains(LocationOption.SuppressCameraCalibration)) {
            return getHeadOffsets().add(advancedCalibration.getCalibratedOffsets());
        }
        else {
            return getHeadOffsets();
        }
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

    public double getRedBalance() {
        return redBalance;
    }

    public void setRedBalance(double redBalance) {
        Object oldValue = this.redBalance;
        this.redBalance = redBalance;
        resetColorMaps();
        firePropertyChange("redBalance", oldValue, redBalance);
        firePropertyChange("colorBalanceGraph", null, getColorBalanceGraph());
        cameraViewHasChanged(null);
    }

    public double getGreenBalance() {
        return greenBalance;
    }

    public void setGreenBalance(double greenBalance) {
        Object oldValue = this.greenBalance;
        this.greenBalance = greenBalance;
        resetColorMaps();
        firePropertyChange("greenBalance", oldValue, greenBalance);
        firePropertyChange("colorBalanceGraph", null, getColorBalanceGraph());
        cameraViewHasChanged(null);
    }

    public double getBlueBalance() {
        return blueBalance;
    }

    public void setBlueBalance(double blueBalance) {
        Object oldValue = this.blueBalance;
        this.blueBalance = blueBalance;
        resetColorMaps();
        firePropertyChange("blueBalance", oldValue, blueBalance);
        firePropertyChange("colorBalanceGraph", null, getColorBalanceGraph());
        cameraViewHasChanged(null);
    }


    public double getRedGamma() {
        return redGamma;
    }

    public void setRedGamma(double redGamma) {
        Object oldValue = this.redGamma;
        this.redGamma = redGamma;
        resetColorMaps();
        firePropertyChange("redGamma", oldValue, redGamma);
        firePropertyChange("colorBalanceGraph", null, getColorBalanceGraph());
        cameraViewHasChanged(null);
    }

    public double getGreenGamma() {
        return greenGamma;
    }

    public void setGreenGamma(double greenGamma) {
        Object oldValue = this.greenGamma;
        this.greenGamma = greenGamma;
        resetColorMaps();
        firePropertyChange("greenGamma", oldValue, greenGamma);
        firePropertyChange("colorBalanceGraph", null, getColorBalanceGraph());
        cameraViewHasChanged(null);
    }

    public double getBlueGamma() {
        return blueGamma;
    }

    public void setBlueGamma(double blueGamma) {
        Object oldValue = this.blueGamma;
        this.blueGamma = blueGamma;
        resetColorMaps();
        firePropertyChange("blueGamma", oldValue, blueGamma);
        firePropertyChange("colorBalanceGraph", null, getColorBalanceGraph());
        cameraViewHasChanged(null);
    }

    public boolean isDeinterlace() {
        return isDeinterlaced();
    }

    public void setDeinterlace(boolean deinterlace) {
        this.deinterlace = deinterlace;
    }

    public boolean isWhiteBalanced() {
        return redBalance != 1.0 || greenBalance != 1.0 || blueBalance != 1.0
                || redGamma != 1.0 || greenGamma != 1.0 || blueGamma != 1.0
                || (redColorMap != null & blueColorMap != null & greenColorMap != null); 
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

            if (advancedCalibration.isOverridingOldTransformsAndDistortionCorrectionSettings()) {
                //Skip all the old style image transforms and distortion corrections except for 
                //deinterlacing, cropping, and white balancing
                if (isDeinterlaced() || isCropped() || isWhiteBalanced() || advancedCalibration.isEnabled()) {
                    Mat mat = OpenCvUtils.toMat(image);
                    mat = deinterlace(mat);
                    mat = crop(mat);
                    mat = whiteBalance(mat);
                    mat = advancedUndistort(mat);
                    image = OpenCvUtils.toBufferedImage(mat);
                    mat.release();
                }
            }
            // Old style of image transforms and distortion correction
            // We do skip the convert to and from Mat if no transforms are needed.
            // But we must enter while performing original calibration.
            else if (isDeinterlaced()
                || isCropped() 
                || isCalibrating()
                || isUndistorted()
                || isScaled()
                || isRotated()
                || isOffset()
                || isFlipped()
                || isWhiteBalanced()) {

                Mat mat = OpenCvUtils.toMat(image);

                mat = deinterlace(mat);

                mat = crop(mat);

                mat = whiteBalance(mat);

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

    @Override
    public Location getUnitsPerPixel(Length viewingPlaneZ) {
        if (advancedCalibration.isOverridingOldTransformsAndDistortionCorrectionSettings() && 
                advancedCalibration.isValid()) {
            double upp = 0;
            if (takeAdvancedCalibrationSemaphore()) {
                upp = advancedCalibration.getDistanceToCameraAtZ(viewingPlaneZ).
                            convertToUnits(LengthUnit.Millimeters).getValue() / 
                            advancedCalibration.getVirtualCameraMatrix().get(0, 0)[0];
                releaseAdvancedCalibrationSemaphore();
            }
            upp = Double.isFinite(upp) ? upp : 0;
            return new Location(LengthUnit.Millimeters, upp, upp, 0, 0);
        }
        return super.getUnitsPerPixel(viewingPlaneZ);
    }

    /**
     * Attempts to take the semaphore for reading the undistortion mapping. If it is successfully 
     * taken, true is returned, otherwise false is returned. If the semaphore is taken, the caller 
     * must call releaseAdvancedCalibrationSemaphore() to release the semaphore.
     * @return true if the semaphore was successfully taken
     */
    private synchronized boolean takeAdvancedCalibrationSemaphore() {
        return takeAdvancedCalibrationSemaphore(true);
    }
    
    /**
     * Attempts to take the semaphore protecting the undistortion mapping and virtual camera matrix.
     * If it is successfully taken, true is returned, otherwise false is returned. If the semaphore
     * is taken, the caller must call releaseAdvancedCalibrationSemaphore() to release the 
     * semaphore.
     * @param forReading - flag to indicate whether the semaphore is being taken for reading (true)
     * or for writing (false)
     * @return true if the semaphore was successfully taken
     */
    private synchronized boolean takeAdvancedCalibrationSemaphore(boolean forReading) {
        if (forReading && undistortionMappingSemaphore >= 0) {
            undistortionMappingSemaphore += 1; //any number of threads can take it for reading
            return true;
        }
        else if (!forReading && undistortionMappingSemaphore == 0) {
            undistortionMappingSemaphore = -1; //only one thread can write at a time
            return true;
        }
        return false;
    }

    /**
     * Returns the semaphore that was taken by takeUndistortionMappingSemaphore()
     */
    private synchronized void releaseAdvancedCalibrationSemaphore() {
        if (undistortionMappingSemaphore > 0) {
            undistortionMappingSemaphore -= 1;
        }
        else if (undistortionMappingSemaphore < 0) {
            undistortionMappingSemaphore = 0;
        }
    }
   
    private Mat advancedUndistort(Mat mat) {
        if (!advancedCalibration.isEnabled()) {
            return mat;
        }
        Mat dst = mat.clone();
        if (takeAdvancedCalibrationSemaphore()) {
            if (undistortionMap1 == null || undistortionMap2 == null) {
                releaseAdvancedCalibrationSemaphore();
                while (takeAdvancedCalibrationSemaphore(false)) {
                    //spin until we take the semaphore for writing
                }
                if (undistortionMap1 == null) {
                    undistortionMap1 = new Mat();
                }
                if (undistortionMap2 == null) {
                    undistortionMap2 = new Mat();
                }
                advancedCalibration.initUndistortRectifyMap(mat.size(), 
                        undistortionMap1, undistortionMap2);
            }
            Imgproc.remap(mat, dst, undistortionMap1, undistortionMap2, Imgproc.INTER_LINEAR);
            releaseAdvancedCalibrationSemaphore();
        }
        mat.release();

        return dst;
    }

    private Mat whiteBalance(Mat mat) {
        if (isWhiteBalanced() && mat.channels() == 3) {
            initWhiteBalanceLut();
            Mat whiteBalanced = new Mat();
            Core.LUT(mat, lut, whiteBalanced);
            mat.release();
            mat = whiteBalanced;
        }
        return mat;
    }

    protected void initWhiteBalanceLut() {
        if (lut == null) {
            byte[] data = new byte[3];
            lut = new Mat(256, 1, CvType.CV_8UC3);
            if (redColorMap != null & blueColorMap != null & greenColorMap != null) {
                final int levels = redColorMap.length;
                final int range = 256/levels;
                final int halfRange = range/2;
                for (int i = 0; i < 256; i++) {
                    // Indexed BGR.
                    int level1 = (i+halfRange)/range;
                    int level0 = level1 - 1;
                    double weight1 = ((i + halfRange) % range)/(double)range; 
                    double weight0 = 1.0 - weight1;
                    if (level0 < 0) {
                        level0 = 0;
                        weight0 = -weight0;
                    }
                    if (level1 >= levels) {
                        level1 = levels-2;
                        weight0 += 2*weight1; 
                        weight1 = -weight1;
                    }
                    data[2] = (byte)Math.max(0, Math.min(255, (redColorMap[level0]*weight0 + redColorMap[level1]*weight1)*255.0));
                    data[1] = (byte)Math.max(0, Math.min(255, (greenColorMap[level0]*weight0 + greenColorMap[level1]*weight1)*255.0));
                    data[0] = (byte)Math.max(0, Math.min(255, (blueColorMap[level0]*weight0 + blueColorMap[level1]*weight1)*255.0));
                    lut.put(i, 0, data);
                }
            }
            else {
                for (int i = 0; i < 256; i++) {
                    // Indexed BGR.
                    data[2] = (byte)Math.min(255, Math.pow(i/255.0*redBalance, 1/redGamma)*255.0);
                    data[1] = (byte)Math.min(255, Math.pow(i/255.0*greenBalance, 1/greenGamma)*255.0);
                    data[0] = (byte)Math.min(255, Math.pow(i/255.0*blueBalance, 1/blueGamma)*255.0);
                    lut.put(i, 0, data);
                }
            }
        }
    }

    public void autoAdjustWhiteBalance(boolean averaged) throws Exception {
        // Switch it off to get a neutral image.
        resetWhiteBalance();
        // Capture.
        BufferedImage image = lightSettleAndCapture();
        // Calculate the histogram.
        long[][] histogram = VisionUtils.computeImageHistogram(image);
        // Analyze the percentiles.
        long pixels = image.getHeight()*image.getWidth();
        double percentileGamma[] = new double[3];
        double percentileLead[] = new double[3];
        double percentileClip[] = new double[3];
        double sum[] = new double[3];
        double n[] = new double[3];
        for (int ch = 0; ch < 3; ch++) {
            long accumulated = 0;
            for (int bin = 0; bin < 256; bin++) {
                long value = histogram[ch][bin];
                accumulated += value;
                if (accumulated < pixels*whiteBalanceGammaFractile) {
                    percentileGamma[ch] = bin;
                }
                if (accumulated < pixels*whiteBalanceLeadFractile) {
                    percentileLead[ch] = bin;
                }
                else {
                    sum[ch] += bin*value;
                    n[ch] += value;
                }
                if (accumulated < pixels*whiteBalanceClipFractile) {
                    percentileClip[ch] = bin;
                }
            }
            sum[ch] /= n[ch];
        }
        // Adapt the other channels to the one with the highest signal in the result.
        double resultLead[] = averaged ? sum : percentileLead;
        double lead = Math.max(Math.max(resultLead[0], resultLead[1]), resultLead[2]);
        if (lead < 32) {
            throw new Exception("The camera "+getName()+" exposure is too low!");
        }

        double r = lead/resultLead[0];
        double g = lead/resultLead[1];
        double b = lead/resultLead[2];

        // We do not: Norm to the maximum clip percentile, but never amplify.
        double clip = 1.0; //Math.max(1.0, Math.max(Math.max(r*percentileClip[0], g*percentileClip[1]), b*percentileClip[2])/255.0);

        // Set the new balance.
        setRedBalance(r/clip);
        setGreenBalance(g/clip);
        setBlueBalance(b/clip);
        
        // Scale the gamma percentile.
        percentileGamma[0] *= r/clip/255; 
        percentileGamma[1] *= g/clip/255; 
        percentileGamma[2] *= b/clip/255;
        
        // Make the gamma match by percentile.
        double midGamma = (percentileGamma[0] + percentileGamma[1] + percentileGamma[2])/3;
        setRedGamma(Math.log(percentileGamma[0])/Math.log(midGamma));
        setGreenGamma(Math.log(percentileGamma[1])/Math.log(midGamma));
        setBlueGamma(Math.log(percentileGamma[2])/Math.log(midGamma));
    }

    public void autoAdjustWhiteBalanceMapped(int levels) throws Exception {
        // Switch it off to get a neutral image.
        resetWhiteBalance();
        // Capture.
        BufferedImage image = lightSettleAndCapture();
        // Map the balance on various levels.
        final int range = 256/levels;
        final int halfRange = range/2;
        final int clip = 254; 
        final int balanceLimit = 4;
        long[][] histogram = VisionUtils.computeImageHistogram(image);
        double lead[] = new double[3];
        long pixels = image.getHeight()*image.getWidth();
        int fractileLead = 0;
        for (int ch = 0; ch < 3; ch++) {
            long accumulated = 0;
            for (int bin = 0; bin < 256; bin++) {
                long value = histogram[ch][bin];
                accumulated += value;
                if (accumulated > pixels*whiteBalanceLeadFractile) {
                    lead[ch] = bin;
                    if (bin > fractileLead) {
                        fractileLead = bin;
                    }
                    break;
                }
            }
        }
        double amplify = fractileLead*3.0/(lead[0] + lead[1] + lead[2]);
        long[][][] histogramAtLevel = new long[3][levels][255+range];
        long[][] counts = new long[3][levels];
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = (rgb >> 0) & 0xff;
                int lum = ((r >= clip || g >= clip || b >= clip) ? 255 // clip all when one is clipped 
                        : (int)(amplify*(r+g+b)/3));
                if (lum >= 1) {
                    if (r >= 1 && lum/r < balanceLimit && r/lum < balanceLimit) {
                        int level = r/range;
                        int midLevel = level*range+halfRange;
                        histogramAtLevel[0][level][Math.min(255, midLevel*lum/r)]++;
                        counts[0][level]++;
                    }
                    if (g >= 1 && lum/g < balanceLimit && g/lum < balanceLimit) {
                        int level = g/range;
                        int midLevel = level*range+halfRange;
                        histogramAtLevel[1][level][Math.min(255, midLevel*lum/g)]++;
                        counts[1][level]++;
                    }
                    if (b >= 1 && lum/b < balanceLimit && b/lum < balanceLimit) {
                        int level = b/range;
                        int midLevel = level*range+halfRange;
                        histogramAtLevel[2][level][Math.min(255, midLevel*lum/b)]++;
                        counts[2][level]++;
                    }
                }
            }
        }
        // Map the colors.
        double [][] colorMap = new double[3][levels];
        for (int ch = 0; ch < 3; ch++) {
            for (int level = 0; level < levels; level++) {
                long median = counts[ch][level]/2;
                long accumulated = 0;
                for (int bin = 0; bin < 256; bin++) {
                    long value = histogramAtLevel[ch][level][bin];
                    accumulated += value;
                    if (accumulated > median) {
                        colorMap[ch][level] = bin/255.0;
                        break;
                    }
                }
            }
            // Sanity check
            for (int level = 1; level < levels; level++) {
                if (level == levels) {
                    if (colorMap[ch][level] > colorMap[ch][level+1]) {
                        // extrapolate
                        colorMap[ch][level] = (colorMap[ch][level+1]*3 - colorMap[ch][level+2])/2;
                    }
                }
                else if (colorMap[ch][level-1] > colorMap[ch][level]) {
                    if (level+1 >= levels) {
                        // extrapolate
                        colorMap[ch][level] = (colorMap[ch][level-1]*3 - colorMap[ch][level-2])/2;
                    }
                    else {
                        if (colorMap[ch][level-1] > colorMap[ch][level+1]) {
                            throw new Exception("Image does not contain grayscales at level "+100*level/levels+" ... "+100*(level+1)/levels+"%.");
//                            colorMap[ch][level] = colorMap[ch][level-1];
                        }
                        else {
                        // Simply interpolate.
                        colorMap[ch][level] = (colorMap[ch][level-1] + colorMap[ch][level+1])/2;
                        }
                    }
                }
            }
        }

        // As an approximation, compute the parametric white balance (as sort of feedback 
        // for the user, and starting point for manual override).
        autoAdjustWhiteBalance(false);
        // But immediately reset the maps and LUT.
        resetColorMaps();
        // And assign our new color maps.
        redColorMap = colorMap[0];
        greenColorMap = colorMap[1];
        blueColorMap = colorMap[2];
        firePropertyChange("colorBalanceGraph", null, getColorBalanceGraph());
        cameraViewHasChanged(null);
    }

    public SimpleGraph getColorBalanceGraph() {
        final String COLOR_GRAPH = "C"; 
        SimpleGraph colorGraph = new SimpleGraph();
        colorGraph.setRelativePaddingLeft(0.1);
        colorGraph.setRelativePaddingRight(0.05);
        SimpleGraph.DataScale scale =  colorGraph.getScale(COLOR_GRAPH);
        scale.setRelativePaddingTop(0.05);
        scale.setRelativePaddingBottom(0.1);
        scale.setColor(SimpleGraph.getDefaultGridColor());
        scale.setSquareAspectRatio(true);
        if (!isWhiteBalanced()) {
            // Show identity.
            SimpleGraph.DataRow lum = colorGraph.getRow(COLOR_GRAPH, "lum");
            lum.setColor(SimpleGraph.getDefaultGridColor());
            lum.recordDataPoint(0, 0);
            lum.recordDataPoint(255, 255);
            return colorGraph;
        }
        else {
            // LUT graph.
            SimpleGraph.DataRow red = colorGraph.getRow(COLOR_GRAPH, "red");
            red.setColor(new Color(255, 0, 0));
            SimpleGraph.DataRow green = colorGraph.getRow(COLOR_GRAPH, "green");
            green.setColor(new Color(0, 255, 0));
            SimpleGraph.DataRow blue = colorGraph.getRow(COLOR_GRAPH, "blue");
            blue.setColor(new Color(0, 0, 255));
            // Make sure the LUT is initialized.
            initWhiteBalanceLut();
            Mat lut = this.lut;
            byte[] data = new byte[3];
            for (int i = 0; i < 256; i++) {
                lut.get(i, 0, data);
                // BGR indexed.
                red.recordDataPoint(i, Byte.toUnsignedInt(data[2]));
                green.recordDataPoint(i, Byte.toUnsignedInt(data[1]));
                blue.recordDataPoint(i, Byte.toUnsignedInt(data[0]));
            }
            return colorGraph;
        }
    }

    public void resetWhiteBalance() throws Exception {
        setRedBalance(1.0);
        setGreenBalance(1.0);
        setBlueBalance(1.0);
        setRedGamma(1.0);
        setGreenGamma(1.0);
        setBlueGamma(1.0);
        resetColorMaps();
    }

    protected void resetColorMaps() {
        redColorMap = null;
        greenColorMap = null;
        blueColorMap = null;
        if (lut != null) {
            lut.release();
            lut = null;
        }
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

    /**
     * Flips the image. NOTE!!!!! - flipX means to flip about the x-axis which is a vertical flip
     * and flipY means to flip about the y-axis which is a horizontal flip
     * @param mat
     * @return
     */
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
        while (!takeAdvancedCalibrationSemaphore(false)) {
            //spin until it is available
        }
        // Clear the calibration cache
        if (undistortionMap1 != null) {
            undistortionMap1.release();
            undistortionMap1 = null;
        }
        if (undistortionMap2 != null) {
            undistortionMap2.release();
            undistortionMap2 = null;
        }
        releaseAdvancedCalibrationSemaphore();
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
                new PropertySheetWizardAdapter(WizardUtils.configurationWizardFactory(CameraConfigurationWizard.class, this.getId(), this), "General Configuration"),
                new PropertySheetWizardAdapter(WizardUtils.configurationWizardFactory(CameraVisionConfigurationWizard.class, this.getId(), this), "Camera Settling"),
                new PropertySheetWizardAdapter(getConfigurationWizard(), "Device Settings"),
                new PropertySheetWizardAdapter(WizardUtils.configurationWizardFactory(ReferenceCameraWhiteBalanceConfigurationWizard.class, this.getId(), this), "White Balance"),
                new PropertySheetWizardAdapter(WizardUtils.configurationWizardFactory(ReferenceCameraPositionConfigurationWizard.class, this.getId(), getMachine(), this), "Position"),
                new PropertySheetWizardAdapter(WizardUtils.configurationWizardFactory(ReferenceCameraCalibrationConfigurationWizard.class, this.getId(), this), "Lens Calibration"),
                new PropertySheetWizardAdapter(WizardUtils.configurationWizardFactory(ReferenceCameraTransformsConfigurationWizard.class, this.getId(), this), "Image Transforms"),
                new PropertySheetWizardAdapter(WizardUtils.configurationWizardFactory(ReferenceCameraCalibrationWizard.class, this.getId(), this), "Advanced Calibration")
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

    public interface CalibrationCallback {
        public void callback(int progressCurrent, int progressMax, boolean complete);
    }

    @Override
    public void findIssues(Solutions solutions) {
        super.findIssues(solutions);
        if (solutions.isTargeting(Milestone.Vision)) {
            /* replaced by more advanced solutions
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
            if (getLightActuator() != null) {
                ActuatorSolutions.findActuateIssues(solutions, this, this.getLightActuator(), "camera light",
                    "https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Camera-Lighting");
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
        Head cameraHead = camera.getHead();
        List<Camera> list = (cameraHead == null ? machine.getCameras() : cameraHead.getCameras());
        Camera replaced = null;
        int index;
        for (index = 0; index < list.size(); index++) {
            if (list.get(index).getId().equals(camera.getId())) {
                replaced = list.get(index);
                if (cameraHead == null) {
                    machine.removeCamera(replaced);
                }
                else {
                    cameraHead.removeCamera(replaced);
                }
                MainFrame.get().getCameraViews().removeCamera(replaced);
                if (replaced instanceof AutoCloseable) {
                    try {
                        ((AutoCloseable) replaced).close();
                    }
                    catch (Exception e) {
                        Logger.warn(e);
                    }
                }
                break;
            }
        }
        final int formerIndex = index;

        UiUtils.messageBoxOnExceptionLater(() -> {
            // Add the new one.
            if (cameraHead == null) {
                machine.addCamera(camera);
            }
            else {
                cameraHead.addCamera(camera);
            }
            // Permutate it back to the old list place (cumbersome but works).
            for (int p = list.size() - formerIndex; p > 1; p--) {
                if (cameraHead == null) {
                    machine.permutateCamera(camera, -1);
                }
                else {
                    cameraHead.permutateCamera(camera, -1);
                }
            }
            if (camera instanceof AbstractBroadcastingCamera) {
                ((AbstractBroadcastingCamera) camera).reinitialize();
            }
            MainFrame.get().getCameraViews().addCamera(camera);
        });
    }
}
