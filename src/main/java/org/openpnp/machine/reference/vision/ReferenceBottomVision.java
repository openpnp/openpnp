package org.openpnp.machine.reference.vision;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Size;
import org.openpnp.ConfigurationListener;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.machine.reference.ReferenceNozzleTip;
import org.openpnp.machine.reference.ReferenceNozzleTipCalibration;
import org.openpnp.machine.reference.ReferenceNozzleTipCalibration.BackgroundCalibrationMethod;
import org.openpnp.machine.reference.vision.wizards.BottomVisionSettingsConfigurationWizard;
import org.openpnp.machine.reference.vision.wizards.ReferenceBottomVisionConfigurationWizard;
import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.AbstractVisionSettings;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.BottomVisionSettings;
import org.openpnp.model.Configuration;
import org.openpnp.model.Footprint;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.model.PartSettingsHolder;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PartAlignment;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.util.Utils2D;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage.Result;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.Root;

public class ReferenceBottomVision extends AbstractPartAlignment {

    @Deprecated
    @Element(required = false)
    protected CvPipeline pipeline;

    @Attribute(required = false)
    protected boolean enabled = false;

    @Attribute(required = false)
    protected boolean preRotate = false;

    @Attribute(required = false)
    protected int maxVisionPasses = 3;

    @Element(required = false)
    protected Length maxLinearOffset = new Length(1, LengthUnit.Millimeters);

    @Attribute(required = false)
    protected double maxAngularOffset = 10;

    @Attribute(required = false)
    protected double testAlignmentAngle = 0.0;

    /**
     * Edge detection pixels. These must correspond to the maximum subSampling in stages
     * and/or to the MJPEG compression block artifact size. Typically 8.
     */
    @Attribute(required = false)
    private int edgeDetectionPixels = 8;

    @Deprecated
    @ElementMap(required = false)
    protected Map<String, PartSettings> partSettingsByPartId = null;

    public ReferenceBottomVision() {
        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            @Override
            public void configurationComplete(Configuration configuration) throws Exception {
                migratePartSettings(configuration);
                if (bottomVisionSettings == null) {
                    // Recovery mode, take any setting.
                    for (AbstractVisionSettings settings : configuration.getVisionSettings()) {
                        if (settings instanceof BottomVisionSettings) {
                            bottomVisionSettings = (BottomVisionSettings) settings;
                            break;
                        }
                    }
                }
            }
        });
    }

    @Override
    public PartAlignmentOffset findOffsets(Part part, BoardLocation boardLocation,
            Location placementLocation, Nozzle nozzle) throws Exception {
        BottomVisionSettings bottomVisionSettings = getInheritedVisionSettings(part);

        if (!isEnabled() || !bottomVisionSettings.isEnabled()) {
            return new PartAlignmentOffset(new Location(LengthUnit.Millimeters), false);
        }

        if (part == null || nozzle.getPart() == null) {
            throw new Exception("No part on nozzle.");
        }
        if (part != nozzle.getPart()) {
            throw new Exception("Part mismatch with part on nozzle.");
        }

        Camera camera = VisionUtils.getBottomVisionCamera();

        if ((bottomVisionSettings.getPreRotateUsage() == PreRotateUsage.Default && preRotate)
                || (bottomVisionSettings.getPreRotateUsage() == PreRotateUsage.AlwaysOn)) {
            return findOffsetsPreRotate(part, boardLocation, placementLocation, nozzle, camera, bottomVisionSettings);
        }
        else {
            return findOffsetsPostRotate(part, boardLocation, placementLocation, nozzle, camera, bottomVisionSettings);
        }
    }

    public Location getCameraLocationAtPartHeight(Part part, Camera camera, Nozzle nozzle, double angle) throws Exception {
        if (part == null) {
            // No part height accounted for.
            return camera.getLocation(nozzle)
                    .derive(null, null, null, angle);
        }
        if (part.isPartHeightUnknown()) {
            if (camera.getFocusProvider() != null
                    && nozzle.getNozzleTip() instanceof ReferenceNozzleTip) {
                ReferenceNozzleTip nt = (ReferenceNozzleTip) nozzle.getNozzleTip(); 
                Location location1 = camera.getLocation(nozzle)
                        .derive(null, null, null, angle);
                Location location0 = location1.add(new Location(nt.getMaxPartHeight().getUnits(), 
                        0, 0, nt.getMaxPartHeight().getValue(), 0));
                Location focus = camera.getFocusProvider().autoFocus(camera, nozzle, nt.getMaxPartDiameterWithTolerance(), location0, location1);
                Length partHeight = focus.getLengthZ().subtract(location1.getLengthZ());
                if (partHeight.getValue() <= 0.001) {
                    throw new Exception("Auto focus part height determination failed. Camera seems to have focused on nozzle tip.");
                }
                Logger.info("Part "+part.getId()+" height set to "+partHeight+" by camera focus provider.");
                part.setHeight(partHeight);
            }
            if (part.isPartHeightUnknown()) {
                throw new Exception("Part height unknown and camera "+camera.getName()+" does not support part height sensing.");
            }
        }
        return camera.getLocation(nozzle)
                .add(new Location(part.getHeight()
                        .getUnits(),
                        0.0, 0.0, part.getHeight()
                        .getValue(),
                        0.0))
                .derive(null, null, null, angle);
    }

    private PartAlignmentOffset findOffsetsPreRotate(Part part, BoardLocation boardLocation,
            Location placementLocation, Nozzle nozzle, Camera camera, BottomVisionSettings bottomVisionSettings)
                    throws Exception {
        double wantedAngle = placementLocation.getRotation();
        if (boardLocation != null) {
            wantedAngle = Utils2D.calculateBoardPlacementLocation(boardLocation, placementLocation)
                    .getRotation();
        }
        wantedAngle = Utils2D.angleNorm(wantedAngle, 180.);
        // Wanted location.
        Location wantedLocation = getCameraLocationAtPartHeight(part, camera, nozzle, wantedAngle);

        Location nozzleLocation = wantedLocation;
        MovableUtils.moveToLocationAtSafeZ(nozzle, nozzleLocation);
        final Location center = new Location(maxLinearOffset.getUnits());

        try (CvPipeline pipeline = bottomVisionSettings.getPipeline()) {

            // The running, iterative offset.
            Location offsets = new Location(nozzleLocation.getUnits());
            // Try getting a good fix on the part in multiple passes.
            for(int pass = 0;;) {
                RotatedRect rect = processPipelineAndGetResult(pipeline, camera, part, nozzle,
                        wantedLocation, bottomVisionSettings);
                camera=(Camera)pipeline.getProperty("camera");

                Logger.debug("Bottom vision part {} result rect {}", part.getId(), rect);

                // Create the offsets object. This is the physical distance from
                // the center of the camera to the located part.
                offsets = VisionUtils.getPixelCenterOffsets(camera, rect.center.x, rect.center.y);

                double angleOffset = VisionUtils.getPixelAngle(camera, rect.angle) - wantedAngle;
                // Most OpenCV Pipelines can only tell us the angle of the recognized rectangle in a   
                // wrapping-around range of 0° .. 90° as it has no notion of which rectangle side 
                // is which. We can assume that the part is never picked more than +/-45º rotated.
                // So we change the range wrapping-around to -45° .. +45°. See angleNorm():
                if (bottomVisionSettings.getMaxRotation() == MaxRotation.Adjust ) {
                    angleOffset = Utils2D.angleNorm(angleOffset);
                } else {
                    // turning more than 180° in one direction makes no sense
                    angleOffset = Utils2D.angleNorm(angleOffset, 180);
                }

                // When we rotate the nozzle later to compensate for the angle offset, the X, Y offsets 
                // will change too, as the off-center part rotates around the nozzle axis.
                // So we need to compensate for that.
                offsets = offsets.rotateXy(-angleOffset)
                        .derive(null, null,	null, angleOffset);
                nozzleLocation = nozzleLocation.subtractWithRotation(offsets);

                if (++pass >= maxVisionPasses) {
                    // Maximum number of passes reached. 
                    break;
                }

                // We not only check the center offset but also the corner offset brought about by the angular offset
                // so a large part will react more sensitively to angular offsets.
                Point corners[] = new Point[4];
                rect.points(corners);
                Location corner = VisionUtils.getPixelCenterOffsets(camera, corners[0].x, corners[0].y)
                        .convertToUnits(maxLinearOffset.getUnits());
                Location cornerWithAngularOffset = corner.rotateXy(angleOffset);
                if (!partSizeCheck(part, bottomVisionSettings, rect, camera) ) {
                    throw new Exception(String.format(
                            "ReferenceBottomVision (%s): Incorrect part size.",
                            part.getId() 
                            )); 
                }
                else if (center.getLinearDistanceTo(offsets) > getMaxLinearOffset().getValue()) {
                    Logger.debug("Offsets too large {} : center offset {} > {}", 
                            offsets, center.getLinearDistanceTo(offsets), getMaxLinearOffset().getValue()); 
                } 
                else if (corner.getLinearDistanceTo(cornerWithAngularOffset) >  getMaxLinearOffset().getValue()) {
                    Logger.debug("Offsets too large {} : corner offset {} > {}", 
                            offsets, corner.getLinearDistanceTo(cornerWithAngularOffset), getMaxLinearOffset().getValue()); 
                }
                else if (Math.abs(angleOffset) > getMaxAngularOffset()) {
                    Logger.debug("Offsets too large {} : angle offset {} > {}", 
                            offsets, Math.abs(angleOffset), getMaxAngularOffset());
                }
                else {
                    // We have a good enough fix - go on with that. 
                    break;                		
                }

                // Not a good enough fix - try again with corrected position.
                nozzle.moveTo(nozzleLocation);
            }
            Logger.debug("Offsets accepted {}", offsets);
            // Calculate cumulative offsets over all the passes.  
            offsets = wantedLocation.subtractWithRotation(nozzleLocation);

            // subtract visionCenterOffset
            offsets = offsets.subtract(bottomVisionSettings.getVisionOffset().rotateXy(wantedAngle));

            displayResult(pipeline, part, offsets, camera, nozzle);
            offsetsCheck(part, nozzle, offsets);

            return new PartAlignment.PartAlignmentOffset(offsets, true);
        }
    }

    private PartAlignmentOffset findOffsetsPostRotate(Part part, BoardLocation boardLocation,
            Location placementLocation, Nozzle nozzle, Camera camera, BottomVisionSettings bottomVisionSettings)
                    throws Exception {
        // Create a location that is the Camera's X, Y, it's Z + part height
        // and a rotation of 0, unless preRotate is enabled
        Location wantedLocation = getCameraLocationAtPartHeight(part, camera, nozzle, 0.);

        MovableUtils.moveToLocationAtSafeZ(nozzle, wantedLocation);

        try (CvPipeline pipeline = bottomVisionSettings.getPipeline()) {
            RotatedRect rect = processPipelineAndGetResult(pipeline, camera, part, nozzle, wantedLocation, bottomVisionSettings);
            camera=(Camera)pipeline.getProperty("camera");

            Logger.debug("Bottom vision part {} result rect {}", part.getId(), rect);

            // Create the offsets object. This is the physical distance from
            // the center of the camera to the located part.
            Location offsets = VisionUtils.getPixelCenterOffsets(camera, rect.center.x, rect.center.y);

            double angleOffset = VisionUtils.getPixelAngle(camera, rect.angle);
            // Most OpenCV Pipelines can only tell us the angle of the recognized rectangle in a   
            // wrapping-around range of 0° .. 90° as it has no notion of which rectangle side 
            // is which. We can assume that the part is never picked more than +/-45º rotated.
            // So we change the range wrapping-around to -45° .. +45°. See angleNorm():
            if (bottomVisionSettings.getMaxRotation() == MaxRotation.Adjust ) {
                angleOffset = Utils2D.angleNorm(angleOffset);
            } else {
                // turning more than 180° in one direction makes no sense
                angleOffset = Utils2D.angleNorm(angleOffset, 180);
            }

            if (!partSizeCheck(part, bottomVisionSettings, rect, camera) ) {
                
            }

            // Set the angle on the offsets.
            offsets = offsets.derive(null, null, null, angleOffset);

            // subtract visionCenterOffset
            offsets = offsets.subtract(bottomVisionSettings.getVisionOffset().rotateXy(offsets.getRotation()));

            displayResult(pipeline, part, offsets, camera, nozzle);
            offsetsCheck(part, nozzle, offsets);

            return new PartAlignmentOffset(offsets, false);
        }
    }

    protected void offsetsCheck(Part part, Nozzle nozzle, Location offsets) throws Exception {
        if (nozzle.getNozzleTip() instanceof ReferenceNozzleTip) {
            Length offsetsLength = offsets.getLinearLengthTo(Location.origin);
            Length maxPickTolerance = ((ReferenceNozzleTip) nozzle.getNozzleTip()).getMaxPickTolerance();
            if (offsetsLength.compareTo(maxPickTolerance) > 0) {
                LengthConverter lengthConverter = new LengthConverter(); 
                throw new Exception("Part "+part.getId()+" bottom vision offsets length "+lengthConverter.convertForward(offsetsLength)
                +" larger than allowed "+lengthConverter.convertForward(maxPickTolerance));
            }
        }
    }

    private boolean partSizeCheck(Part part, BottomVisionSettings bottomVisionSettings, RotatedRect partRect, Camera camera) throws Exception {
        // Check if this test needs to be done
        Location partSize = bottomVisionSettings.getPartCheckSize(part, false);
        if (partSize == null) {
            return true;
        }

        // Make sure width is the longest dimension
        if (partSize.getY() > partSize.getX()) {
            partSize = new Location(partSize.getUnits(), partSize.getY(), partSize.getX(), 0, 0);
        }

        double pxWidth = VisionUtils.toPixels(partSize.getLengthX(), camera);
        double pxHeight = VisionUtils.toPixels(partSize.getLengthY(), camera);

        // Make sure width is the longest dimension
        Size measuredSize = partRect.size;
        if (measuredSize.height > measuredSize.width) {
            double mHeight = measuredSize.height;
            double mWidth = measuredSize.width;
            measuredSize.height = mWidth;
            measuredSize.width = mHeight;
        }

        double widthTolerance = pxWidth * 0.01 * (double) bottomVisionSettings.getCheckSizeTolerancePercent();
        double heightTolerance = pxHeight * 0.01 * (double) bottomVisionSettings.getCheckSizeTolerancePercent();
        double pxMaxWidth = pxWidth + widthTolerance;
        double pxMinWidth = pxWidth - widthTolerance;
        double pxMaxHeight = pxHeight + heightTolerance;
        double pxMinHeight = pxHeight - heightTolerance;
        boolean ret;
        Location upp = camera.getUnitsPerPixelAtZ();
        LengthConverter lengthConverter = new LengthConverter();
        String measuredWidth = lengthConverter.convertForward(upp.getLengthX().multiply(measuredSize.width));
        String measuredHeight = lengthConverter.convertForward(upp.getLengthY().multiply(measuredSize.height));
        String nominalWidth = lengthConverter.convertForward(partSize.getLengthX());
        String nominalHeight = lengthConverter.convertForward(partSize.getLengthY());
        String msg;
        if (measuredSize.width > pxMaxWidth) {
            msg = String.format("Part %s width too large: nominal %s, limit %s, measured %s", part.getId(), 
                    nominalWidth, lengthConverter.convertForward(upp.getLengthX().multiply(pxMaxWidth)), 
                    measuredWidth);
            ret = false;
        } else if (measuredSize.width < pxMinWidth) {
            msg = String.format("Part %s width too small: nominal %s, limit %s, measured %s", part.getId(), 
                    nominalWidth, lengthConverter.convertForward(upp.getLengthX().multiply(pxMinWidth)), 
                    measuredWidth);
            ret = false;
        } else if (measuredSize.height > pxMaxHeight) {
            msg = String.format("Part %s height too large: nominal %s, limit %s, measured %s", part.getId(), 
                    nominalHeight, lengthConverter.convertForward(upp.getLengthY().multiply(pxMaxHeight)),
                    measuredHeight);
            ret = false;
        } else if (measuredSize.height < pxMinHeight) {
            msg = String.format("Part %s height too small: nominal %s, limit %s, measured %s", part.getId(), 
                    nominalHeight, lengthConverter.convertForward(upp.getLengthY().multiply(pxMinHeight)),
                    measuredHeight);
            ret = false;
        }
        else {
            msg = String.format("Part %s size ok. Width %s, Height %s", part.getId(), measuredWidth, measuredHeight);
            ret = true;
        }
        Logger.debug(msg);
        if (! ret) {
            throw new Exception(msg);
        }
        return true;
    }

    private static void displayResult(CvPipeline pipeline, Part part, Location offsets, Camera camera, Nozzle nozzle) {
        Logger.debug("Final offsets {}, distance {}", offsets, offsets.getLinearDistanceTo(Location.origin));
        MainFrame mainFrame = MainFrame.get();
        if (mainFrame != null) {
            try {
                String s = String.format("%s : %s", part.getId(), offsets.toString());
                mainFrame
                .getCameraViews()
                .getCameraView(camera)
                .showFilteredImage(OpenCvUtils.toBufferedImage(pipeline.getWorkingImage()), s,
                        1500);
                // Also make sure the right nozzle is selected for correct cross-hair rotation.
                MovableUtils.fireTargetedUserAction(nozzle);
            }
            catch (Exception e) {
                // Throw away, just means we're running outside of the UI.
            }
        }
    }

    public void preparePipeline(CvPipeline pipeline, Map<String, Object> pipelineParameterAssignments, 
            Camera camera, Nozzle nozzle, Location wantedLocation, BottomVisionSettings bottomVisionSettings) {
        pipeline.setProperty("camera", camera);
        Location partSize = null;
        // Set the footprint.
        if (nozzle.getPart() != null && nozzle.getPart().getPackage() != null) {
            Footprint footprint = nozzle.getPart().getPackage().getFootprint();
            pipeline.setProperty("footprint", footprint);
            partSize = bottomVisionSettings.getPartCheckSize(nozzle.getPart(), true);
        }
        // Set alignment parameters.
        pipeline.setProperty("alignment.center", wantedLocation);
        pipeline.setProperty("alignment.expectedAngle", wantedLocation.getRotation());
        // Set the background removal properties.
        if (nozzle.getNozzleTip() instanceof ReferenceNozzleTip) { 
            ReferenceNozzleTip referenceNozzleTip = (ReferenceNozzleTip) nozzle.getNozzleTip();
            pipeline.setProperty("alignment.searchDistance", referenceNozzleTip.getMaxPickTolerance()
                    .multiply(1.2)); // Allow for some tolerance, we will check the result later.
            pipeline.setProperty("MaskCircle.diameter", referenceNozzleTip.getMaxPartDiameterWithTolerance());
            ReferenceNozzleTipCalibration calibration = referenceNozzleTip.getCalibration();
            if (calibration != null 
                    && calibration.getBackgroundCalibrationMethod() != BackgroundCalibrationMethod.None) {
                pipeline.setProperty("BlurGaussian.kernelSize", calibration.getMinimumDetailSize());
                pipeline.setProperty("MaskHsv.hueMin", 
                        Math.max(0, calibration.getBackgroundMinHue() - calibration.getBackgroundTolHue()));
                pipeline.setProperty("MaskHsv.hueMax", 
                        Math.min(255, calibration.getBackgroundMaxHue() + calibration.getBackgroundTolHue()));
                pipeline.setProperty("MaskHsv.saturationMin", 
                        Math.max(0, calibration.getBackgroundMinSaturation() - calibration.getBackgroundTolSaturation()));
                pipeline.setProperty("MaskHsv.saturationMax", 255);  
                // no need to restrict to this: Math.min(255, calibration.getBackgroundMaxSaturation() + calibration.getBackgroundTolSaturation()));
                pipeline.setProperty("MaskHsv.valueMin", 0); 
                // no need to restrict to this: Math.max(0, calibration.getBackgroundMinValue() - calibration.getBackgroundTolValue()));
                pipeline.setProperty("MaskHsv.valueMax", 
                        Math.min(255, calibration.getBackgroundMaxValue() +  calibration.getBackgroundTolValue()));
            }
        }
        if (partSize != null) {
            // Add a margin for edge detection.
            Location upp = camera.getUnitsPerPixelAtZ();
            pipeline.setProperty("alignment.maxWidth", partSize.getLengthX()
                    .add(upp.getLengthX().multiply(edgeDetectionPixels*2)));
            pipeline.setProperty("alignment.maxHeight", partSize.getLengthY()
                    .add(upp.getLengthY().multiply(edgeDetectionPixels*2)));
        }
        else if (nozzle.getNozzleTip() instanceof ReferenceNozzleTip) {
            // No part size available. Use the maximum diameter. 
            Length maxPartDiameter = ((ReferenceNozzleTip) nozzle.getNozzleTip()).getMaxPartDiameter();
            pipeline.setProperty("alignment.maxWidth", maxPartDiameter);
            pipeline.setProperty("alignment.maxHeight", maxPartDiameter);
        }

        pipeline.setProperties(pipelineParameterAssignments);
    }

    private RotatedRect processPipelineAndGetResult(CvPipeline pipeline, Camera camera, Part part,
            Nozzle nozzle, Location wantedLocation, BottomVisionSettings bottomVisionSettings) throws Exception {
        preparePipeline(pipeline, bottomVisionSettings.getPipelineParameterAssignments(), camera, nozzle, wantedLocation, bottomVisionSettings);
        pipeline.process();

        Result result = pipeline.getResult(VisionUtils.PIPELINE_RESULTS_NAME);

        // Fall back to the old name of "result" instead of "results" for backwards
        // compatibility.
        if (result == null) {
            result = pipeline.getResult("result");
        }

        if (result == null) {
            throw new Exception(String.format(
                    "ReferenceBottomVision (%s): Pipeline error. Pipeline must contain a result named '%s'.",
                    part.getId(), VisionUtils.PIPELINE_RESULTS_NAME));
        }

        if (result.model == null) {
            throw new Exception(String.format(
                    "ReferenceBottomVision (%s): No result found.",
                    part.getId()));
        }

        if (!(result.model instanceof RotatedRect)) {
            throw new Exception(String.format(
                    "ReferenceBottomVision (%s): Incorrect pipeline result type (%s). Expected RotatedRect.",
                    part.getId(), result.model.getClass().getSimpleName()));
        }

        return (RotatedRect) result.model;
    }

    @Override
    public boolean canHandle(PartSettingsHolder settingsHolder, boolean allowDisabled) {
        BottomVisionSettings visionSettings = getInheritedVisionSettings(settingsHolder);
        if (visionSettings != null) {
            boolean isEnabled = (enabled && visionSettings.isEnabled());
            if (!allowDisabled) {
                Logger.trace("{}.canHandle({}) => {}, {}", this.getClass().getSimpleName(), 
                        settingsHolder == null ? "" : settingsHolder.getId(), visionSettings, isEnabled ? "enabled" : "disabled");
            }
            return allowDisabled || isEnabled;
        }
        return false;
    }

    private BottomVisionSettings createBottomVisionSettings(String id, String name, CvPipeline pipeline) {
        BottomVisionSettings bottomVisionSettings;
        try {
            bottomVisionSettings = new BottomVisionSettings(id);
            bottomVisionSettings.setName(name);
            bottomVisionSettings.setEnabled(true);
            bottomVisionSettings.setPipeline(pipeline);
            return bottomVisionSettings;
        }
        catch (Exception e) {
            throw new Error(e);
        }
    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void setName(String name) {
    }

    @Override
    public String getShortName() {
        return getPropertySheetHolderTitle();
    }

    @Override
    public void setBottomVisionSettings(BottomVisionSettings visionSettings) {
        if (visionSettings == null) {
            return; // do not allow null
        }
        super.setBottomVisionSettings(visionSettings);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isPreRotate() {
        return preRotate;
    }

    public void setPreRotate(boolean preRotate) {
        this.preRotate = preRotate;
    }

    public int getMaxVisionPasses() {
        return maxVisionPasses;
    }

    public void setMaxVisionPasses(int maxVisionPasses) {
        this.maxVisionPasses = maxVisionPasses;
    }

    public Length getMaxLinearOffset() {
        return maxLinearOffset;
    }

    public void setMaxLinearOffset(Length maxLinearOffset) {
        this.maxLinearOffset = maxLinearOffset;
    }

    public double getMaxAngularOffset() {
        return maxAngularOffset;
    }

    public void setMaxAngularOffset(double maxAngularOffset) {
        this.maxAngularOffset = maxAngularOffset;
    }

    public double getTestAlignmentAngle() {
        return testAlignmentAngle;
    }

    public void setTestAlignmentAngle(double testAlignmentAngle) {
        Object oldValue = this.testAlignmentAngle; 
        this.testAlignmentAngle = testAlignmentAngle;
        firePropertyChange("testAlignmentAngle", oldValue, testAlignmentAngle);
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return "Bottom Vision";
    }

    public static CvPipeline createStockPipeline(String variant) {
        try {
            String xml = IOUtils.toString(ReferenceBottomVision.class
                    .getResource("ReferenceBottomVision-"+variant+"Pipeline.xml"));
            return new CvPipeline(xml);
        }
        catch (Exception e) {
            throw new Error(e);
        }
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        return null;
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {
                new PropertySheetWizardAdapter(new ReferenceBottomVisionConfigurationWizard(this)),
                new PropertySheetWizardAdapter(new BottomVisionSettingsConfigurationWizard(getBottomVisionSettings(), this))};
    }

    public enum PreRotateUsage {
        Default, AlwaysOn, AlwaysOff
    }

    public enum PartSizeCheckMethod {
        Disabled, BodySize, PadExtents
    }

    public enum MaxRotation {
        Adjust, Full
    }

    @Deprecated
    @Root
    public static class PartSettings extends AbstractModelObject {

        @Deprecated
        @Attribute
        protected boolean enabled = true;
        @Deprecated
        @Attribute(required = false)
        protected PreRotateUsage preRotateUsage = PreRotateUsage.Default;

        @Deprecated
        @Attribute(required = false)
        protected PartSizeCheckMethod checkPartSizeMethod = PartSizeCheckMethod.Disabled;

        @Deprecated
        @Attribute(required = false)
        protected int checkSizeTolerancePercent = 20;

        @Deprecated
        @Attribute(required = false)
        protected MaxRotation maxRotation = MaxRotation.Adjust;

        @Deprecated
        @Element(required = false)
        protected Location visionOffset = new Location(LengthUnit.Millimeters);

        @Deprecated
        @Element
        protected CvPipeline pipeline;

        @Deprecated
        public PartSettings() {
        }

        @Deprecated
        public boolean isEnabled() {
            return enabled;
        }

        @Deprecated
        public PreRotateUsage getPreRotateUsage() {
            return preRotateUsage;
        }

        @Deprecated
        public CvPipeline getPipeline() {
            return pipeline;
        }

        @Deprecated
        public void setPipeline(CvPipeline pipeline) {
            this.pipeline = pipeline;
        }

        @Deprecated
        public MaxRotation getMaxRotation() {
            return maxRotation;
        }

        public PartSizeCheckMethod getCheckPartSizeMethod() {
            return checkPartSizeMethod;
        }

        public int getCheckSizeTolerancePercent() {
            return checkSizeTolerancePercent;
        }

        public Location getVisionOffset() {
            return visionOffset;
        }
    }

    protected void migratePartSettings(Configuration configuration) {
        if (partSettingsByPartId == null) {
            AbstractVisionSettings stockVisionSettings = configuration.getVisionSettings(AbstractVisionSettings.STOCK_BOTTOM_ID);
            if (stockVisionSettings == null) {
                // Fresh configuration: need to migrate the stock and default settings, even if no partSettingsById are present.  
                partSettingsByPartId = new HashMap<>();
            }
            else { 
                // Reassign the stock pipeline.
                stockVisionSettings.setPipeline(createStockPipeline("Default"));
                // Add the reclinear symmetry pipeline if missing.
                AbstractVisionSettings rectlinearVisionSettings = configuration.getVisionSettings(AbstractVisionSettings.STOCK_BOTTOM_RECTLINEAR_ID);
                if (rectlinearVisionSettings == null) {
                    rectlinearVisionSettings = createRectlinearBottomVisionSettings();
                    configuration.addVisionSettings(rectlinearVisionSettings);
                }
                // Reassign the stock pipeline.
                rectlinearVisionSettings.setPipeline(createStockPipeline("Rectlinear"));
                return;
            }
        }

        HashMap<String, BottomVisionSettings> bottomVisionSettingsHashMap = new HashMap<>();
        // Create the factory stock settings.
        BottomVisionSettings stockBottomVisionSettings = createStockBottomVisionSettings();
        configuration.addVisionSettings(stockBottomVisionSettings);
        BottomVisionSettings rectlinearBottomVisionSettings = createRectlinearBottomVisionSettings();
        configuration.addVisionSettings(rectlinearBottomVisionSettings);
        PartSettings equivalentPartSettings = new PartSettings();
        equivalentPartSettings.setPipeline(stockBottomVisionSettings.getPipeline());
        bottomVisionSettingsHashMap.put(AbstractVisionSettings.createSettingsFingerprint(equivalentPartSettings), stockBottomVisionSettings);
        // Migrate the default settings.
        BottomVisionSettings defaultBottomVisionSettings = new BottomVisionSettings(AbstractVisionSettings.DEFAULT_BOTTOM_ID);
        defaultBottomVisionSettings.setName("- Default Machine Bottom Vision -");
        defaultBottomVisionSettings.setEnabled(enabled);
        configuration.addVisionSettings(defaultBottomVisionSettings);
        if(pipeline != null) {
            defaultBottomVisionSettings.setPipeline(pipeline);
            pipeline = null;
        }
        else {
            defaultBottomVisionSettings.setPipeline(stockBottomVisionSettings.getPipeline());
        }
        setBottomVisionSettings(defaultBottomVisionSettings);
        equivalentPartSettings.setPipeline(defaultBottomVisionSettings.getPipeline());
        bottomVisionSettingsHashMap.put(AbstractVisionSettings.createSettingsFingerprint(equivalentPartSettings), defaultBottomVisionSettings);
        for (Part part: configuration.getParts()) {
            part.setBottomVisionSettings(null);
        }
        for (org.openpnp.model.Package pkg : configuration.getPackages()) {
            pkg.setBottomVisionSettings(null);
        }
        partSettingsByPartId.forEach((partId, partSettings) -> {
            if (partSettings == null) {
                return;
            }

            try {
                Part part = configuration.getPart(partId);
                if (part != null) { 
                    String serializedHash = AbstractVisionSettings.createSettingsFingerprint(partSettings);
                    BottomVisionSettings bottomVisionSettings = bottomVisionSettingsHashMap.get(serializedHash);
                    if (bottomVisionSettings == null) {
                        bottomVisionSettings = new BottomVisionSettings(partSettings);
                        bottomVisionSettings.setName("");
                        bottomVisionSettingsHashMap.put(serializedHash, bottomVisionSettings);

                        configuration.addVisionSettings(bottomVisionSettings);
                    }

                    part.setBottomVisionSettings((bottomVisionSettings != defaultBottomVisionSettings) ? bottomVisionSettings : null);
                    Logger.info("Part "+partId+" BottomVisionSettings migrated.");
                } else {
                    Logger.warn("Part "+partId+" BottomVisionSettings with no part.");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        partSettingsByPartId = null;

        optimizeVisionSettings(configuration);
    }

    protected BottomVisionSettings createRectlinearBottomVisionSettings() {
        return createBottomVisionSettings(AbstractVisionSettings.STOCK_BOTTOM_RECTLINEAR_ID, 
                "- Rectlinear Symmetry Bottom Vision Settings -", createStockPipeline("Rectlinear"));
    }

    protected BottomVisionSettings createStockBottomVisionSettings() {
        return createBottomVisionSettings(AbstractVisionSettings.STOCK_BOTTOM_ID, 
                "- Stock Bottom Vision Settings -", createStockPipeline("Default"));
    }

    public void optimizeVisionSettings(Configuration configuration) {
        // Remove any duplicate settings.
        HashMap<String, AbstractVisionSettings> bottomVisionSettingsHashMap = new HashMap<>();
        BottomVisionSettings defaultVisionSettings = getBottomVisionSettings();
        // Make it dominant in case it is identical to stock.
        bottomVisionSettingsHashMap.put(AbstractVisionSettings.createSettingsFingerprint(defaultVisionSettings), defaultVisionSettings);
        for (AbstractVisionSettings visionSettings : configuration.getVisionSettings()) {
            if (visionSettings instanceof BottomVisionSettings) {
                String serializedHash = AbstractVisionSettings.createSettingsFingerprint(visionSettings);
                AbstractVisionSettings firstVisionSettings = bottomVisionSettingsHashMap.get(serializedHash);
                if (firstVisionSettings == null) {
                    bottomVisionSettingsHashMap.put(serializedHash, visionSettings);
                }
                else if (visionSettings != defaultVisionSettings
                        && !visionSettings.isStockSetting()) {
                    // Duplicate, remove any references.
                    for (PartSettingsHolder holder : visionSettings.getUsedBottomVisionIn()) {
                        holder.setBottomVisionSettings((BottomVisionSettings) firstVisionSettings);
                    }
                    if (visionSettings.getUsedBottomVisionIn().size() == 0) {
                        if (firstVisionSettings != defaultVisionSettings  
                                && !firstVisionSettings.isStockSetting()) {
                            firstVisionSettings.setName(firstVisionSettings.getName()+" + "+visionSettings.getName());
                        }
                        configuration.removeVisionSettings(visionSettings);
                    }
                }
            }
        }

        // Per package, search the most common settings on parts, and make them inherited package setting.
        for (org.openpnp.model.Package pkg : configuration.getPackages()) {
            HashMap<String, Integer> histogram = new HashMap<>();
            BottomVisionSettings mostFrequentVisionSettings = null;
            int highestFrequency = 0;
            BottomVisionSettings packageVisionSettings = AbstractPartAlignment.getInheritedVisionSettings(pkg, true);
            for (Part part: configuration.getParts()) {
                if (part.getPackage() == pkg) {
                    BottomVisionSettings visionSettings = AbstractPartAlignment.getInheritedVisionSettings(part, true);
                    String id = visionSettings != null ? visionSettings.getId() : "";
                    Integer frequency = histogram.get(id);
                    frequency = (frequency != null ? frequency + 1 : 1);
                    histogram.put(id, frequency);
                    if (highestFrequency < frequency) {
                        highestFrequency = frequency;
                        mostFrequentVisionSettings = visionSettings;
                    }
                }
            }
            if (mostFrequentVisionSettings != null) {
                if (mostFrequentVisionSettings == defaultVisionSettings) {
                    pkg.setBottomVisionSettings(null);
                }
                else {
                    pkg.setBottomVisionSettings(mostFrequentVisionSettings);
                }
                for (Part part: configuration.getParts()) {
                    if (part.getPackage() == pkg) {
                        if (part.getBottomVisionSettings() == mostFrequentVisionSettings) {
                            // Parts inherit from package now.
                            part.setBottomVisionSettings(null);
                        }
                        else if (part.getBottomVisionSettings() == null 
                                && packageVisionSettings != mostFrequentVisionSettings){
                            // Former package settings were inherited, now we must freeze them. 
                            part.setBottomVisionSettings(packageVisionSettings);
                        }
                    }
                }
                if (mostFrequentVisionSettings != defaultVisionSettings
                        && !mostFrequentVisionSettings.isStockSetting()
                        && !mostFrequentVisionSettings.getName().isEmpty() 
                        && mostFrequentVisionSettings.getUsedBottomVisionIn().size() == 1) {
                    // If these part settings are now unique to the package, name them so. 
                    mostFrequentVisionSettings.setName(pkg.getShortName());
                }
            }
        }

        // Set missing names by usage.
        AbstractVisionSettings.ListConverter listConverter = new AbstractVisionSettings.ListConverter(false);
        int various = 0;
        for (AbstractVisionSettings visionSettings : configuration.getVisionSettings()) {
            if (visionSettings instanceof BottomVisionSettings) {
                List<PartSettingsHolder> usedIn = visionSettings.getUsedBottomVisionIn();
                if (!visionSettings.isStockSetting()
                        && visionSettings != defaultVisionSettings
                        && usedIn.isEmpty()) {
                    configuration.removeVisionSettings(visionSettings);
                }
                else if (visionSettings.getName().isEmpty()) {
                    if (usedIn.size() <= 3) {
                        visionSettings.setName(listConverter.convertForward(usedIn));
                    }
                    else {
                        various++;
                        visionSettings.setName("Migrated "+various);
                    }
                }
            }
        }
    }

    public static ReferenceBottomVision getDefault() { 
        return (ReferenceBottomVision) Configuration.get().getMachine().getPartAlignments().get(0);
    }
}
