package org.openpnp.machine.reference.vision;

import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Action;
import javax.swing.Icon;

import org.apache.commons.io.IOUtils;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Size;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceNozzleTip;
import org.openpnp.machine.reference.vision.wizards.ReferenceBottomVisionConfigurationWizard;
import org.openpnp.machine.reference.vision.wizards.ReferenceBottomVisionPartConfigurationWizard;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Footprint;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
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

public class ReferenceBottomVision implements PartAlignment {


    @Element(required = false)
    protected CvPipeline pipeline = createDefaultPipeline();

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

    @ElementMap(required = false)
    protected Map<String, PartSettings> partSettingsByPartId = new HashMap<>();

    @Override
    public PartAlignmentOffset findOffsets(Part part, BoardLocation boardLocation,
            Location placementLocation, Nozzle nozzle) throws Exception {
        PartSettings partSettings = getPartSettings(part);

        if (!isEnabled() || !partSettings.isEnabled()) {
            return new PartAlignmentOffset(new Location(LengthUnit.Millimeters), false);
        }

        if (part == null || nozzle.getPart() == null) {
            throw new Exception("No part on nozzle.");
        }
        if (part != nozzle.getPart()) {
            throw new Exception("Part mismatch with part on nozzle.");
        }

        Camera camera = VisionUtils.getBottomVisionCamera();

        if ((partSettings.getPreRotateUsage() == PreRotateUsage.Default && preRotate)
                || (partSettings.getPreRotateUsage() == PreRotateUsage.AlwaysOn)) {
            return findOffsetsPreRotate(part, boardLocation, placementLocation, nozzle, camera,
                    partSettings);
        }
        else {
            return findOffsetsPostRotate(part, boardLocation, placementLocation, nozzle, camera,
                    partSettings);
        }
    }

    public Location getCameraLocationAtPartHeight(Part part, Camera camera, Nozzle nozzle, double angle) throws Exception {
        if (part.isPartHeightUnknown()) {
            if (camera.getFocusProvider() != null
                    && nozzle.getNozzleTip() instanceof ReferenceNozzleTip) {
                ReferenceNozzleTip nt = (ReferenceNozzleTip) nozzle.getNozzleTip(); 
                Location location1 = camera.getLocation(nozzle)
                        .derive(null, null, null, angle);
                Location location0 = location1.add(new Location(nt.getMaxPartHeight().getUnits(), 
                        0, 0, nt.getMaxPartHeight().getValue(), 0));
                Location focus = camera.getFocusProvider().autoFocus(camera, nozzle, nt.getMaxPartDiameter(), location0, location1);
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
            Location placementLocation, Nozzle nozzle, Camera camera, PartSettings partSettings)
            throws Exception {
        double wantedAngle = placementLocation.getRotation();
        if (boardLocation != null) {
            wantedAngle = Utils2D.calculateBoardPlacementLocation(boardLocation, placementLocation)
                           .getRotation();
        }
        wantedAngle = angleNorm(wantedAngle, 180.);
        // Wanted location.
        Location wantedLocation = getCameraLocationAtPartHeight(part, camera, nozzle, wantedAngle);
                
        Location nozzleLocation = wantedLocation;
        MovableUtils.moveToLocationAtSafeZ(nozzle, nozzleLocation);
        final Location center = new Location(maxLinearOffset.getUnits());
        
        try (CvPipeline pipeline = partSettings.getPipeline()) {

            // The running, iterative offset.
            Location offsets = new Location(nozzleLocation.getUnits());
            // Try getting a good fix on the part in multiple passes.
            for(int pass = 0;;) {
                RotatedRect rect = processPipelineAndGetResult(pipeline, camera, part, nozzle);
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
                if (partSettings.getMaxRotation() == MaxRotation.Adjust ) {
                    angleOffset = angleNorm(angleOffset);
                } else {
                    // turning more than 180° in one direction makes no sense
                    angleOffset = angleNorm(angleOffset, 180);
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
                if (!partSizeCheck(part, partSettings, rect, camera) ) {
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
            Logger.debug("Final offsets {}", offsets);
            displayResult(pipeline, part, offsets, camera);
            return new PartAlignment.PartAlignmentOffset(offsets, true);
        }
    }

    private PartAlignmentOffset findOffsetsPostRotate(Part part, BoardLocation boardLocation,
            Location placementLocation, Nozzle nozzle, Camera camera, PartSettings partSettings)
                    throws Exception {
        // Create a location that is the Camera's X, Y, it's Z + part height
        // and a rotation of 0, unless preRotate is enabled
        Location wantedLocation = getCameraLocationAtPartHeight(part, camera, nozzle, 0.);
        
        MovableUtils.moveToLocationAtSafeZ(nozzle, wantedLocation);

        try (CvPipeline pipeline = partSettings.getPipeline()) {
            RotatedRect rect = processPipelineAndGetResult(pipeline, camera, part, nozzle);
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
            if (partSettings.getMaxRotation() == MaxRotation.Adjust ) {
                angleOffset = angleNorm(angleOffset);
            } else {
                // turning more than 180° in one direction makes no sense
                angleOffset = angleNorm(angleOffset, 180);
            }
            
            if (!partSizeCheck(part, partSettings, rect, camera) ) {
                throw new Exception(String.format(
                        "ReferenceBottomVision (%s): Incorrect part size.",
                        part.getId() 
                        ));          	
            }

            // Set the angle on the offsets.
            offsets = offsets.derive(null, null, null, angleOffset);
            Logger.debug("Final offsets {}", offsets);

            displayResult(pipeline, part, offsets, camera);

            return new PartAlignmentOffset(offsets, false);
        }
    }
    
    
    private boolean partSizeCheck(Part part, PartSettings partSettings, RotatedRect partRect, Camera camera) {
        // Check if this test needs to be done
        PartSettings.PartSizeCheckMethod partSizeCheckMethod = partSettings.getCheckPartSizeMethod();

        double checkWidth = 0.0;
        double checkHeight = 0.0;

        Footprint footprint = part.getPackage().getFootprint();
        LengthUnit footprintLengthUnit = footprint.getUnits();

        // Get the part footprint body dimensions to compare to
        switch (partSizeCheckMethod) {
        case Disabled:
            return true;
        case BodySize:
            checkWidth = footprint.getBodyWidth();
            checkHeight = footprint.getBodyHeight();
            break;
        case PadExtents:
            Rectangle bounds = footprint.getPadsShape().getBounds();
            checkWidth = bounds.getWidth();
            checkHeight = bounds.getHeight();
            break;
        }

        // Make sure width is the longest dimension
        if (checkHeight > checkWidth) {
            double height = checkHeight;
            double width = checkWidth;
            checkWidth = height;
            checkHeight = width;
        }

        Length width = new Length(checkWidth, footprintLengthUnit);
        Length height = new Length(checkHeight, footprintLengthUnit);
        double pxWidth = VisionUtils.toPixels(width, camera);
        double pxHeight = VisionUtils.toPixels(height, camera);

        // Make sure width is the longest dimension
        Size measuredSize = partRect.size;
        if (measuredSize.height > measuredSize.width) {
            double mHeight = measuredSize.height;
            double mWidth = measuredSize.width;
            measuredSize.height = mWidth;
            measuredSize.width = mHeight;
        }

        double widthTolerance = pxWidth * 0.01 * (double) partSettings.getCheckSizeTolerancePercent();
        double heightTolerance = pxHeight * 0.01 * (double) partSettings.getCheckSizeTolerancePercent();
        double pxMaxWidth = pxWidth + widthTolerance;
        double pxMinWidth = pxWidth - widthTolerance;
        double pxMaxHeight = pxHeight + heightTolerance;
        double pxMinHeight = pxHeight - heightTolerance;

        if (measuredSize.width > pxMaxWidth) {
            Logger.debug("Package pixel width {} : limit {} : measured {}", pxWidth, pxMaxWidth, measuredSize.width);
            return false;
        } else if (measuredSize.width < pxMinWidth) {
            Logger.debug("Package pixel width {} : limit {} : measured {}", pxWidth, pxMinWidth, measuredSize.width);
            return false;
        } else if (measuredSize.height > pxMaxHeight) {
            Logger.debug("Package pixel height {} : limit {} : measured {}", pxHeight, pxMaxHeight,
                    measuredSize.height);
            return false;
        } else if (measuredSize.height < pxMinHeight) {
            Logger.debug("Package pixel height {} : limit {} : measured {}", pxHeight, pxMinHeight,
                    measuredSize.height);
            return false;
        }

        Logger.debug("Package {} pixel size ok. Width {}, Height {}", part.getId(), measuredSize.width, measuredSize.height);
        return true;
    }

    private static void displayResult(CvPipeline pipeline, Part part, Location offsets, Camera camera) {
        MainFrame mainFrame = MainFrame.get();
        if (mainFrame != null) {
            try {
                String s = String.format("%s : %s", part.getId(), offsets.toString());
                mainFrame
                .getCameraViews()
                .getCameraView(camera)
                .showFilteredImage(OpenCvUtils.toBufferedImage(pipeline.getWorkingImage()), s,
                        1500);
            }
            catch (Exception e) {
                // Throw away, just means we're running outside of the UI.
            }
        }
    }

    private static RotatedRect processPipelineAndGetResult(CvPipeline pipeline, Camera camera, Part part,
            Nozzle nozzle) throws Exception {
        pipeline.setProperty("camera", camera);
        pipeline.setProperty("part", part);
        pipeline.setProperty("nozzle", nozzle);
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
    public boolean canHandle(Part part) {
        PartSettings partSettings = getPartSettings(part);
        boolean result = (enabled && partSettings.isEnabled());
        Logger.debug("{}.canHandle({}) => {}", part.getId(), result);
        return result;
    }

    public static CvPipeline createDefaultPipeline() {
        try {
            String xml = IOUtils.toString(ReferenceBottomVision.class.getResource(
                    "ReferenceBottomVision-DefaultPipeline.xml"));
            return new CvPipeline(xml);
        }
        catch (Exception e) {
            throw new Error(e);
        }
    }

    private static double angleNorm(double val, double lim) {
        double clip = lim * 2;
        while (Math.abs(val) > lim) {
            val += (val < 0.) ? clip : -clip;
        }
        return val;
    }

    private static double angleNorm(double val) {
        return angleNorm(val, 45.);
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

    public CvPipeline getPipeline() {
        return pipeline;
    }

    public void setPipeline(CvPipeline pipeline) {
        this.pipeline = pipeline;
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
    
    @Override
    public String getPropertySheetHolderTitle() {
        return "Bottom Vision";
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        return null;
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {
                new PropertySheetWizardAdapter(new ReferenceBottomVisionConfigurationWizard(this))};
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return null;
    }

    @Override
    public Icon getPropertySheetHolderIcon() {
        return null;
    }

    public PartSettings getPartSettings(Part part) {
        PartSettings partSettings = this.partSettingsByPartId.get(part.getId());
        if (partSettings == null) {
            partSettings = new PartSettings(this);
            this.partSettingsByPartId.put(part.getId(), partSettings);
        }
        return partSettings;
    }

    public Map<String, PartSettings> getPartSettingsByPartId() {
        return partSettingsByPartId;
    }

    @Override
    public Wizard getPartConfigurationWizard(Part part) {
        PartSettings partSettings = getPartSettings(part);
        try {
            partSettings.getPipeline()
                        .setProperty("camera", VisionUtils.getBottomVisionCamera());
        }
        catch (Exception e) {
        }
        return new ReferenceBottomVisionPartConfigurationWizard(this, part);
    }
    
    public enum PreRotateUsage {
        Default, AlwaysOn, AlwaysOff
    }
    
    public enum MaxRotation {
        Adjust, Full
    }
    
    @Root
    public static class PartSettings {

        public enum PartSizeCheckMethod {
            Disabled, BodySize, PadExtents
        }

        @Attribute
        protected boolean enabled;
        @Attribute(required = false)
        protected PreRotateUsage preRotateUsage = PreRotateUsage.Default;
        
        @Attribute(required = false)
        protected PartSizeCheckMethod checkPartSizeMethod = PartSizeCheckMethod.Disabled;

        @Attribute(required = false)
        protected int checkSizeTolerancePercent = 20;

        @Attribute(required = false)
        protected MaxRotation maxRotation = MaxRotation.Adjust;
        
        @Element
        protected CvPipeline pipeline;

        public PartSettings() {

        }

        public PartSettings(ReferenceBottomVision bottomVision) {
            setEnabled(bottomVision.isEnabled());
            try {
                setPipeline(bottomVision.getPipeline()
                                        .clone());
            }
            catch (Exception e) {
                throw new Error(e);
            }
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public PreRotateUsage getPreRotateUsage() {
            return preRotateUsage;
        }

        public void setPreRotateUsage(PreRotateUsage preRotateUsage) {
            this.preRotateUsage = preRotateUsage;
        }

        public CvPipeline getPipeline() {
            return pipeline;
        }

        public void setPipeline(CvPipeline pipeline) {
            this.pipeline = pipeline;
        }
        
        public MaxRotation getMaxRotation() {
            return maxRotation;
        }

        public void setMaxRotation(MaxRotation maxRotation) {
            this.maxRotation = maxRotation;
        }

        public PartSizeCheckMethod getCheckPartSizeMethod() {
        	return checkPartSizeMethod;
        }
        
        public void setCheckPartSizeMethod(PartSizeCheckMethod checkPartSizeMethod) {
            this.checkPartSizeMethod = checkPartSizeMethod;
        }

        public int getCheckSizeTolerancePercent() {
        	return checkSizeTolerancePercent;
        }

        public void setCheckSizeTolerancePercent(int checkSizeTolerancePercent) {
            this.checkSizeTolerancePercent = checkSizeTolerancePercent;
        }

    }
}
