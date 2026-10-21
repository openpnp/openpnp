/*
 * Copyright (C) 2022 <mark@makr.zone>
 * inspired and based on work
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

package org.openpnp.machine.reference.solutions;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import javax.swing.SwingUtilities;

import org.openpnp.Translations;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraPanel;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.components.CameraView.RenderingQuality;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.camera.AbstractBroadcastingCamera;
import org.openpnp.machine.reference.camera.AbstractSettlingCamera.SettleMethod;
import org.openpnp.machine.reference.camera.OpenPnpCaptureCamera;
import org.openpnp.machine.reference.camera.OpenPnpCaptureCamera.CapturePropertyHolder;
import org.openpnp.machine.reference.camera.ReferenceCamera;
import org.openpnp.machine.reference.camera.SwitcherCamera;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Solutions;
import org.openpnp.model.Solutions.Issue;
import org.openpnp.model.Solutions.Milestone;
import org.openpnp.model.Solutions.Severity;
import org.openpnp.model.Solutions.State;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MotionPlanner.CompletionType;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.UiUtils;
import org.openpnp.util.VisionUtils;
import org.pmw.tinylog.Logger;

public class CameraSolutions implements Solutions.Subject  {
    ReferenceCamera camera;

    final static long EXPOSURE_ADAPT_MILLISECONDS = 700;
    final static int EXPOSURE_ADJUST_MAX_STEPS = 64;

    public CameraSolutions(ReferenceCamera camera) {
        this.camera = camera;
    }

    @Override
    public void findIssues(Solutions solutions) {
        if (solutions.isTargeting(Milestone.Connect)) {
            if (camera instanceof OpenPnpCaptureCamera
                && ((OpenPnpCaptureCamera) camera).getDevice() == null) {
                solutions.add(new Solutions.PlainIssue(
                        camera, 
                        Translations.getString("CameraSolutions.Connect.Issue"),  //$NON-NLS-1$
                        Translations.getString("CameraSolutions.Connect.Solution"),  //$NON-NLS-1$
                        Severity.Fundamental,
                        "https://github.com/openpnp/openpnp/wiki/OpenPnpCaptureCamera")); //$NON-NLS-1$
            }
        }
        if (solutions.isTargeting(Milestone.Basics)) {
            ActuatorSolutions.findActuateIssues(solutions, camera, camera.getLightActuator(), Translations.getString("CameraSolutions.Qualifier.CameraLight"), //$NON-NLS-1$
                "https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Camera-Lighting");
            if (camera instanceof SwitcherCamera) {
                ActuatorSolutions.findActuateIssues(solutions, camera, ((SwitcherCamera)camera).getActuator(), Translations.getString("CameraSolutions.Qualifier.CameraSwitcher"), //$NON-NLS-1$
                    "https://github.com/openpnp/openpnp/wiki/SwitcherCamera#configuration");
            }
        }
        if (solutions.isTargeting(Milestone.Vision)) {
            final double previewFps = camera.getPreviewFps();
            if (previewFps > 15) {
                solutions.add(new Solutions.Issue(
                        camera, 
                        Translations.getString("CameraSolutions.Issue.HighPreviewFps"), //$NON-NLS-1$
                        Translations.getString("CameraSolutions.Solution.HighPreviewFps"), //$NON-NLS-1$
                        Severity.Suggestion,
                        "https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration_General-Camera-Setup#general-configuration") {


                    @Override
                    public void setState(Solutions.State state) throws Exception {
                        camera.setPreviewFps((state == Solutions.State.Solved) ? 5.0 : previewFps);
                        super.setState(state);
                    }
                });
            }
            if (! camera.isSuspendPreviewInTasks()) {
                solutions.add(new Solutions.Issue(
                        camera, 
                        Translations.getString((camera instanceof SwitcherCamera) ? "CameraSolutions.Issue.SuspendPreview.Switcher" : "CameraSolutions.Issue.SuspendPreview.Recommended"), //$NON-NLS-1$ //$NON-NLS-2$
                        Translations.getString("CameraSolutions.Solution.SuspendPreview"), //$NON-NLS-1$
                        ((camera instanceof SwitcherCamera) ? Severity.Error : Severity.Suggestion),
                        "https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration_General-Camera-Setup#general-configuration") {

                    @Override
                    public void setState(Solutions.State state) throws Exception {
                        camera.setSuspendPreviewInTasks((state == Solutions.State.Solved));
                        super.setState(state);
                    }
                });
            }
            if (! camera.isAutoVisible()) {
                solutions.add(new Solutions.Issue(
                        camera, 
                        Translations.getString("CameraSolutions.Issue.AutoCameraView"), //$NON-NLS-1$
                        Translations.getString("CameraSolutions.Solution.AutoCameraView"), //$NON-NLS-1$
                        Severity.Suggestion,
                        "https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration_General-Camera-Setup#general-configuration") {

                    @Override
                    public void setState(Solutions.State state) throws Exception {
                        camera.setAutoVisible((state == Solutions.State.Solved));
                        super.setState(state);
                    }
                });
            }
            CameraPanel cameraPanel = MainFrame.get().getCameraViews();
            CameraView view = cameraPanel.getCameraView(camera);
            if (view != null) {
                final RenderingQuality renderingQuality = view.getRenderingQuality();
                if (renderingQuality.ordinal() < RenderingQuality.High.ordinal()) {
                    solutions.add(new Solutions.Issue(
                            camera, 
                            Translations.getString("CameraSolutions.Issue.RenderingQuality"), //$NON-NLS-1$
                            Translations.getString("CameraSolutions.Solution.RenderingQuality"), //$NON-NLS-1$
                            Severity.Suggestion,
                            "https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration_General-Camera-Setup#camera-view-configuration") {

                        @Override
                        public void setState(Solutions.State state) throws Exception {
                            view.setRenderingQuality((state == Solutions.State.Solved) ? RenderingQuality.High : renderingQuality);
                            camera.cameraViewHasChanged(null);
                            super.setState(state);
                        }
                    });
                }
            }
            Machine machine = Configuration.get().getMachine();
            if (machine instanceof ReferenceMachine 
                    && (camera.getHead() == null || camera.getHead() instanceof ReferenceHead)) {
                VisionSolutions visionSolutions = ((ReferenceMachine)machine).getVisionSolutions();
                ReferenceHead head = (ReferenceHead) camera.getHead();
                if ((head == null || visionSolutions.isSolvedPrimaryXY(head)) 
                        && camera.getUnitsPerPixelPrimary().isInitialized()) {
                    HeadMountable movable0;
                    Location location0;
                    if (camera.getHead() == null) {
                        try {
                            movable0 = machine.getDefaultHead().getDefaultNozzle();
                            location0 = camera.getLocation(movable0);
                        }
                        catch (Exception e) {
                            movable0 = null;
                            location0 = null;
                        }
                    }
                    else {
                        movable0 = camera;
                        location0 = head.getCalibrationPrimaryFiducialLocation();
                    }
                    final HeadMountable movable = movable0;
                    final Location location = location0;
                    final SettleMethod oldSettleMethod = camera.getSettleMethod();
                    if (oldSettleMethod == SettleMethod.FixedTime) {
                        solutions.add(new Solutions.Issue(
                                camera, 
                                Translations.getString("CameraSolutions.Issue.AdaptiveSettling"), //$NON-NLS-1$
                                Translations.getString("CameraSolutions.Solution.AdaptiveSettling"), //$NON-NLS-1$
                                Severity.Fundamental,
                                "https://github.com/openpnp/openpnp/wiki/Camera-Settling") {

                            @Override 
                            public void activate() throws Exception {
                                MainFrame.get().getMachineControls().setSelectedTool(camera);
                                MovableUtils.fireTargetedUserAction(camera);
                            }

                            @Override 
                            public String getExtendedDescription() {
                                String caution = (movable == camera ? 
                                        Translations.format("CameraSolutions.ExtendedDescription.AdaptiveSettling.CautionCamera", camera.getName()) //$NON-NLS-1$
                                        : (movable != null ? 
                                                Translations.format("CameraSolutions.ExtendedDescription.AdaptiveSettling.CautionMovable", movable.getClass().getSimpleName(), movable.getName(), camera.getName()) //$NON-NLS-1$
                                                : ""));
                                String results = (getState() == State.Solved  ? 
                                        Translations.format("CameraSolutions.ExtendedDescription.AdaptiveSettling.Results", //$NON-NLS-1$
                                                camera.getSettleMethod(),
                                                camera.getRecordedComputeMilliseconds(),
                                                camera.getRecordedSettleMilliseconds(),
                                                (camera.getRecordedSettleMilliseconds() > camera.getSettleTimeoutMs()/2 ?
                                                        Translations.getString("CameraSolutions.ExtendedDescription.AdaptiveSettling.LargeSettleTime") : ""), //$NON-NLS-1$
                                                camera.getName())
                                        : "");
                                return Translations.format("CameraSolutions.ExtendedDescription.AdaptiveSettling", caution, results); //$NON-NLS-1$
                            }
                            @Override
                            public void setState(Solutions.State state) throws Exception {
                                if (state == State.Solved) {
                                    final State oldState = getState();
                                    UiUtils.submitUiMachineTask(
                                            () -> {
                                                calibrateCameraSettling(this, (ReferenceMachine) machine, movable, location);
                                                return true;
                                            },
                                            (result) -> {
                                                UiUtils.messageBoxOnException(() -> super.setState(state));
                                            },
                                            (t) -> {
                                                UiUtils.showError(t);
                                                // restore old state
                                                UiUtils.messageBoxOnException(() -> setState(oldState));
                                            });
                                }
                                else {
                                    camera.setSettleMethod(oldSettleMethod);
                                    super.setState(state);
                                }
                            }
                        });
                    }
                }
            }
            boolean propertiesOK = true;
            if (camera instanceof OpenPnpCaptureCamera) {
                OpenPnpCaptureCamera pnpCamera = (OpenPnpCaptureCamera) camera;
                // Some properties just need to be set to best values.
                propertiesOK = addCapturePropertyValueSolution(solutions, 
                        "brightness", pnpCamera.getBrightness(), 
                        Translations.getString("CameraSolutions.Solution.CapturePropertyValue.Revert"), null, 
                        "https://github.com/openpnp/openpnp/wiki/OpenPnpCaptureCamera#camera-properties")
                        && propertiesOK;
                propertiesOK = addCapturePropertyValueSolution(solutions, 
                        "contrast", pnpCamera.getContrast(), 
                        Translations.getString("CameraSolutions.Solution.CapturePropertyValue.Revert"), null, 
                        "https://github.com/openpnp/openpnp/wiki/OpenPnpCaptureCamera#camera-properties")
                        && propertiesOK;
                propertiesOK = addCapturePropertyValueSolution(solutions, 
                        "gamma", pnpCamera.getGamma(), 
                        Translations.getString("CameraSolutions.Solution.CapturePropertyValue.Revert"), null, 
                        "https://github.com/openpnp/openpnp/wiki/OpenPnpCaptureCamera#camera-properties")
                        && propertiesOK;
                propertiesOK = addCapturePropertyValueSolution(solutions, 
                        "gain", pnpCamera.getGain(), 
                        Translations.getString("CameraSolutions.Solution.CapturePropertyValue.Revert"), null, 
                        "https://github.com/openpnp/openpnp/wiki/OpenPnpCaptureCamera#camera-properties")
                        && propertiesOK;

                // Exposure needs special logic.
                final CapturePropertyHolder exposureProperty = pnpCamera.getExposure();
                propertiesOK = addCapturePropertyAutoSolution(solutions, 
                        "exposure", exposureProperty, 
                        Translations.getString("CameraSolutions.Solution.CapturePropertyAuto.Exposure"),
                        (Solutions.Issue issue) -> {
                            final State oldState = issue.getState();
                            UiUtils.submitUiMachineTask(() -> {
                                setStaticExposure(pnpCamera, exposureProperty, 
                                        EXPOSURE_ADAPT_MILLISECONDS, EXPOSURE_ADJUST_MAX_STEPS);
                                return true;
                            },
                                    (result) -> {
                                    },
                                    (t) -> {
                                        UiUtils.showError(t);
                                        // restore old state
                                        UiUtils.messageBoxOnException(() -> issue.setState(oldState));
                                    });
                            return true;
                        }, 
                        "https://github.com/openpnp/openpnp/wiki/OpenPnpCaptureCamera#camera-properties")
                        && propertiesOK;
                // More regular props.
                addCapturePropertyValueSolution(solutions, 
                        "sharpness", pnpCamera.getSharpness(), 
                        Translations.getString("CameraSolutions.Solution.CapturePropertyValue.Min"), pnpCamera.getSharpness().getMin(), 
                        "https://github.com/openpnp/openpnp/wiki/OpenPnpCaptureCamera#camera-properties");
                propertiesOK = addCapturePropertyValueSolution(solutions, 
                        "hue", pnpCamera.getHue(), 
                        Translations.getString("CameraSolutions.Solution.CapturePropertyValue.Revert"), null, 
                        "https://github.com/openpnp/openpnp/wiki/OpenPnpCaptureCamera#camera-properties")
                        && propertiesOK;
                propertiesOK = addCapturePropertyValueSolution(solutions, 
                        "saturation", pnpCamera.getSaturation(), 
                        Translations.getString("CameraSolutions.Solution.CapturePropertyValue.Revert"), null, 
                        "https://github.com/openpnp/openpnp/wiki/OpenPnpCaptureCamera#camera-properties")
                        && propertiesOK;
                propertiesOK = addCapturePropertyValueSolution(solutions, 
                        "white balance", pnpCamera.getWhiteBalance(), 
                        Translations.getString("CameraSolutions.Solution.CapturePropertyValue.WhiteBalance"), null, 
                        "https://github.com/openpnp/openpnp/wiki/Camera-White-Balance#problems-with-device-white-balance")
                        && propertiesOK;
            }

            if (!camera.isWhiteBalanced()) {
                BufferedImage image = camera.captureRaw();
                long [][] histogram = VisionUtils.computeImageHistogram(image);
                if (!(Arrays.equals(histogram[0], histogram[1]) || Arrays.equals(histogram[0], histogram[2]))) {
                    // Image is not monochrome.
                    final boolean _propertiesOK = propertiesOK;
                    solutions.add(new Solutions.Issue(
                            camera, 
                            Translations.format("CameraSolutions.Issue.WhiteBalance", camera.getName()), //$NON-NLS-1$
                            Translations.getString("CameraSolutions.Solution.WhiteBalance"), //$NON-NLS-1$
                            Severity.Suggestion,
                            "https://github.com/openpnp/openpnp/wiki/Camera-White-Balance") {

                        @Override 
                        public void activate() throws Exception {
                            MainFrame.get().getMachineControls().setSelectedTool(camera);
                            camera.ensureCameraVisible();
                        }

                        @Override 
                        public String getExtendedDescription() {
                            return Translations.format("CameraSolutions.ExtendedDescription.WhiteBalance", //$NON-NLS-1$
                                    (_propertiesOK ? "" : Translations.getString("CameraSolutions.ExtendedDescription.WhiteBalance.Warning")), //$NON-NLS-1$
                                    camera.getName());
                        }

                        @Override
                        public void setState(Solutions.State state) throws Exception {
                            if (state == Solutions.State.Solved) {
                                camera.autoAdjustWhiteBalanceMapped(8);
                            }
                            else {
                                camera.resetWhiteBalance();
                            }
                            super.setState(state);
                        }
                    });
                }
            }
        }
    }

    protected void setStaticExposure(OpenPnpCaptureCamera camera,
            final CapturePropertyHolder exposureProperty, final long adaptMilliseconds, int maxSteps)
            throws Exception, InterruptedException {
        CameraView cameraView = MainFrame.get().getCameraViews().ensureCameraVisible(camera); 
        // Capture a reference image.
        if (!exposureProperty.isAuto()) {
            exposureProperty.setAuto(true);
            Thread.sleep(adaptMilliseconds);
        }
        BufferedImage frame0 = camera.lightSettleAndCapture();
        SwingUtilities.invokeLater(() -> cameraView.showFilteredImage(frame0, "Auto Exposure", adaptMilliseconds*3));
        long [][] autoHistogram = VisionUtils.computeImageHistogramHsv(frame0);
        // Switch off Auto, and increase the manual value, until it best matches the Auto exposed.
        exposureProperty.setAuto(false);
        Thread.sleep(adaptMilliseconds); // sleep more for initial auto exposure
        int bestValue = exposureProperty.getDefault();
        long bestDiff = Long.MAX_VALUE;
        int expMin = exposureProperty.getMin();
        int expMax = exposureProperty.getMax();
        int steps = Math.min(maxSteps - 1, expMax - expMin);
        for (int step = 0;
                step <= steps;
                step++) {
            int value = expMin + step*(expMax - expMin)/steps;
            exposureProperty.setValue(value);
            Thread.sleep(adaptMilliseconds);
            BufferedImage frame = camera.lightSettleAndCapture();
            final String msg = "Exposure "+value+" (probe "+(step+1)+"/"+(steps+1)+")";
            SwingUtilities.invokeLater(() -> cameraView.showFilteredImage(frame, msg, adaptMilliseconds*2));
            long [][] histogram = VisionUtils.computeImageHistogramHsv(frame);
            long diff = 0;
            for (int v = 0; v <= 255; v++) {
                diff += Math.pow(histogram[2][v] - autoHistogram[2][v], 2);
            }
            if (bestDiff > diff) {
                bestDiff = diff;
                bestValue = value;
            }
        }
        exposureProperty.setValue(bestValue);
        Thread.sleep(adaptMilliseconds);
        BufferedImage frame1 = camera.lightSettleAndCapture();
        final String msg = "Exposure "+bestValue+" (set)";
        SwingUtilities.invokeLater(() -> cameraView.showFilteredImage(frame1, msg, 4000));
    }

    protected boolean addCapturePropertyAutoSolution(Solutions solutions, 
            String propertyName, CapturePropertyHolder property, 
            String valueName, Function<Solutions.Issue, Boolean> valueSetter, 
            String uri) {
        if (property.isAuto()) {
            final int oldValue = property.getValue();
            solutions.add(new Solutions.Issue(
                    camera, 
                    Translations.format("CameraSolutions.Issue.CapturePropertyAuto", propertyName, camera.getName()), //$NON-NLS-1$
                    Translations.format("CameraSolutions.Solution.CapturePropertyAuto", propertyName, valueName),
                            Severity.Suggestion,
                            uri) {

                @Override 
                public void activate() throws Exception {
                    MainFrame.get().getMachineControls().setSelectedTool(camera);
                    camera.ensureCameraVisible();
                }

                @Override
                public void setState(Solutions.State state) throws Exception {
                    Function<Issue, Boolean> valueSetterToApply = null;
                    if (state == Solutions.State.Solved) {
                        if (valueSetter == null) {
                            property.setAuto(false);
                            property.setValue(property.getDefault());
                        }
                        else {
                            valueSetterToApply = valueSetter;
                        }
                    }
                    else {
                        property.setAuto(true);
                        property.setValue(oldValue);
                    }
                    camera.lightSettleAndCapture();
                    camera.ensureCameraVisible();
                    if (valueSetterToApply != null) {
                        // Call the value setter as the last thing, as it might spawn a machine task 
                        // and we need to avoid threading conflicts on camera light or switcher actuators.
                        valueSetterToApply.apply(this);
                    }
                    super.setState(state);
                }
            });
            return false;
        }
        return true;
    }

    protected boolean addCapturePropertyValueSolution(Solutions solutions, 
            String propertyName, CapturePropertyHolder property, 
            String valueName, Integer suggestedValue, 
            String uri) {
        final int wantedValue = (suggestedValue != null ? suggestedValue : property.getDefault());
        if (property.getValue() != wantedValue || property.isAuto()) {
            final boolean oldAuto = property.isAuto();
            final int oldValue = property.getValue();
            solutions.add(new Solutions.Issue(
                    camera, 
                    Translations.format("CameraSolutions.Issue.CapturePropertyValue", propertyName, camera.getName(), wantedValue), //$NON-NLS-1$
                    Translations.format("CameraSolutions.Solution.CapturePropertyValue", //$NON-NLS-1$
                            (oldAuto ? Translations.format("CameraSolutions.Solution.CapturePropertyValue.SwitchOffAuto", propertyName) : Translations.getString("CameraSolutions.Solution.CapturePropertyValue.Therefore")), //$NON-NLS-1$ //$NON-NLS-2$
                            valueName),
                            Severity.Suggestion,
                            uri) {

                @Override 
                public void activate() throws Exception {
                    MainFrame.get().getMachineControls().setSelectedTool(camera);
                    camera.ensureCameraVisible();
                }

                @Override
                public void setState(Solutions.State state) throws Exception {
                    if (state == Solutions.State.Solved) {
                        property.setAuto(false);
                        property.setValue(wantedValue);
                    }
                    else {
                        property.setAuto(oldAuto);
                        property.setValue(oldValue);
                    }
                    camera.lightSettleAndCapture();
                    camera.ensureCameraVisible();
                    super.setState(state);
                }
            });
            return false;
        }
        return true;
    }

    public void calibrateCameraSettling(Solutions.Issue issue, ReferenceMachine machine, HeadMountable movable, Location location) throws Exception {
        VisionSolutions visionSolutions = machine.getVisionSolutions();
        camera.setSettleMethod(SettleMethod.Motion);
        camera.setSettleMaskCircle(0);
        camera.setSettleDebounce(2);
        camera.setSettleFullColor(false);
        camera.setSettleGradients(false);
        camera.setSettleContrastEnhance(0);
        camera.setSettleThreshold(1);
        double wantedPixelRes = visionSolutions.getSettleWantedResolutionMm()
                /camera.getUnitsPerPixel().convertToUnits(LengthUnit.Millimeters).getX();
        camera.setSettleGaussianBlur(((int)(wantedPixelRes*5))|1);
        camera.setSettleTimeoutMs(visionSolutions.getZeroKnowledgeSettleTimeMs());
        camera.setSettleDiagnostics(true);
        // Try this.
        performSettleTest(machine, movable, location, visionSolutions.getSettleTestMoveMm());
        if (camera.getRecordedComputeMilliseconds() > visionSolutions.getSettleAcceptableComputeTime()) {
            camera.setSettleMethod(SettleMethod.Maximum);
            camera.setSettleMaskCircle(Math.sqrt(2));
            camera.setSettleDebounce(3);
            camera.setSettleThreshold(visionSolutions.getSettleMaximumPixelDiff());
            do {
                // Try this.
                performSettleTest(machine, movable, location, visionSolutions.getSettleTestMoveMm());
                if (camera.getRecordedSettleMilliseconds() < camera.getSettleTimeoutMs()/2) {
                    // OK, not timed out
                    break;
                }
                // Timed out, increase the pixel difference
                camera.setSettleThreshold(Math.ceil(camera.getSettleThreshold()*1.4));
            }
            while (camera.getSettleThreshold() < 50);
        }
    }

    private void performSettleTest(Machine machine, HeadMountable movable, Location location, double settleMoveMm)
            throws Exception, InterruptedException {
        if (movable != null) {
            Location location0 = location.add(new Location(LengthUnit.Millimeters, settleMoveMm, settleMoveMm, 0, 0)); 
            MovableUtils.moveToLocationAtSafeZ(movable, location);
            machine.getMotionPlanner().waitForCompletion(movable, CompletionType.WaitForStillstand);
            Thread.sleep(1000);
            movable.moveTo(location0);
            movable.moveTo(location);
        }
        camera.lightSettleAndCapture();
        Logger.debug("Settle test with method "+camera.getSettleMethod()+" has "
        + "compute time "+camera.getRecordedComputeMilliseconds()+"ms, "
        + "settle time "+camera.getRecordedSettleMilliseconds()+"ms.");
    }

    /**
     * Create a replacement OpenPnpCaptureCamera for this camera with some of the
     * generic settings transferred.  
     * 
     * @return
     */
    public static OpenPnpCaptureCamera createReplacementCamera(ReferenceCamera oldCamera) {
        OpenPnpCaptureCamera camera = new OpenPnpCaptureCamera();
        camera.setHead(oldCamera.getHead());
        camera.setId(oldCamera.getId());
        camera.setLooking(oldCamera.getLooking());
        camera.setName(oldCamera.getName());
        camera.setHeadOffsets(oldCamera.getHeadOffsets());
        camera.setAxisX(oldCamera.getAxisX());
        camera.setAxisY(oldCamera.getAxisY());
        camera.setAxisZ(oldCamera.getAxisZ());
        camera.setAxisRotation(oldCamera.getAxisRotation());
        camera.setPreviewFps(oldCamera.getPreviewFps());
        camera.setSuspendPreviewInTasks(oldCamera.isSuspendPreviewInTasks());
        camera.setAutoVisible(oldCamera.isAutoVisible());
        camera.setLightActuator(oldCamera.getLightActuator());
        camera.setAllowMachineActuators(oldCamera.isAllowMachineActuators());
        camera.setBeforeCaptureLightOn(oldCamera.isBeforeCaptureLightOn());
        camera.setAfterCaptureLightOff(oldCamera.isAfterCaptureLightOff());
        camera.setUserActionLightOn(oldCamera.isUserActionLightOn());
        camera.setAntiGlareLightOff(oldCamera.isAntiGlareLightOff());
        camera.setVisionProvider(oldCamera.getVisionProvider());
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
