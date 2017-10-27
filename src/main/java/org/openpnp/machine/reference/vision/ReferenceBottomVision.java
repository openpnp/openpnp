package org.openpnp.machine.reference.vision;

import java.util.HashMap;
import java.util.Map;

import javax.swing.Action;
import javax.swing.Icon;

import org.apache.commons.io.IOUtils;
import org.opencv.core.RotatedRect;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.vision.wizards.ReferenceBottomVisionConfigurationWizard;
import org.openpnp.machine.reference.vision.wizards.ReferenceBottomVisionPartConfigurationWizard;
import org.openpnp.model.BoardLocation;
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

        if (preRotate) {
            return findOffsetsPreRotate(part, boardLocation, placementLocation, nozzle, camera,
                    partSettings);
        }
        else {
            return findOffsetsPostRotate(part, boardLocation, placementLocation, nozzle, camera,
                    partSettings);
        }
    }

    private static PartAlignmentOffset findOffsetsPreRotate(Part part, BoardLocation boardLocation,
            Location placementLocation, Nozzle nozzle, Camera camera, PartSettings partSettings)
            throws Exception {
        double angle = placementLocation.getRotation();
        if (boardLocation != null) {
            angle = Utils2D.calculateBoardPlacementLocation(boardLocation, placementLocation)
                           .getRotation();
        }
        angle = angleNorm(angle, 180.);
        double placementAngle = angle;
        Location location = camera.getLocation()
                                  .add(new Location(part.getHeight()
                                                        .getUnits(),
                                          0.0, 0.0, part.getHeight()
                                                        .getValue(),
                                          0.0))
                                  .derive(null, null, null, angle);
        MovableUtils.moveToLocationAtSafeZ(nozzle, location);

        try (CvPipeline pipeline = partSettings.getPipeline()) {

            RotatedRect rect = processPipelineAndGetResult(pipeline, camera, part, nozzle);

            angle = angleNorm(angleNorm(angle)
                    + angleNorm((rect.size.width < rect.size.height) ? 90 + rect.angle : rect.angle));
            // error is -angle
            // See https://github.com/openpnp/openpnp/pull/590 for explanations of the magic
            // values below.
            if (Math.abs(angle) > 0.0765) {
                angle += 0.0567 * Math.signum(angle);
            } // rounding

            nozzle.moveTo(new Location(LengthUnit.Millimeters, Double.NaN, Double.NaN, Double.NaN,
                    placementAngle + angle));

            rect = processPipelineAndGetResult(pipeline, camera, part, nozzle);

            Logger.debug("Result rect {}", rect);
            Location offsets = VisionUtils.getPixelCenterOffsets(camera, rect.center.x, rect.center.y)
                                          .derive(null, null, null, Double.NaN);

            displayResult(pipeline, part, offsets, camera);
            return new PartAlignment.PartAlignmentOffset(offsets, true);
        }
    }

    private static PartAlignmentOffset findOffsetsPostRotate(Part part, BoardLocation boardLocation,
            Location placementLocation, Nozzle nozzle, Camera camera, PartSettings partSettings)
            throws Exception {
        // Create a location that is the Camera's X, Y, it's Z + part height
        // and a rotation of 0, unless preRotate is enabled
        Location startLocation = camera.getLocation();
        Length partHeight = part.getHeight();
        Location partHeightLocation =
                new Location(partHeight.getUnits(), 0, 0, partHeight.getValue(), 0);
        startLocation = startLocation.add(partHeightLocation)
                                     .derive(null, null, null, 0.);

        MovableUtils.moveToLocationAtSafeZ(nozzle, startLocation);

        try (CvPipeline pipeline = partSettings.getPipeline()) {
            RotatedRect rect = processPipelineAndGetResult(pipeline, camera, part, nozzle);
    
            Logger.debug("Result rect {}", rect);
    
            // Create the offsets object. This is the physical distance from
            // the center of the camera to the located part.
            Location offsets = VisionUtils.getPixelCenterOffsets(camera, rect.center.x, rect.center.y);
    
            // We assume that the part is never picked more than 45º rotated
            // so if OpenCV tells us it's rotated more than 45º we correct
            // it. This seems to happen quite a bit when the angle of rotation
            // is close to 0.
            double angle = rect.angle;
            while (Math.abs(angle) > 45) {
                if (angle < 0) {
                    angle += 90;
                }
                else {
                    angle -= 90;
                }
            }
    
            // Set the angle on the offsets.
            offsets = offsets.derive(null, null, null, -angle);
            Logger.debug("Final offsets {}", offsets);
    
            offsets = offsets.derive(null, null, null, offsets.getRotation());
    
            displayResult(pipeline, part, offsets, camera);
    
            return new PartAlignmentOffset(offsets, false);
        }
    }

    private static void displayResult(CvPipeline pipeline, Part part, Location offsets, Camera camera) {
        try {
            String s = String.format("%s : %s", part.getId(), offsets.toString());
            MainFrame.get()
                     .getCameraViews()
                     .getCameraView(camera)
                     .showFilteredImage(OpenCvUtils.toBufferedImage(pipeline.getWorkingImage()), s,
                             1500);
        }
        catch (Exception e) {
            // Throw away, just means we're running outside of the UI.
        }
    }

    private static RotatedRect processPipelineAndGetResult(CvPipeline pipeline, Camera camera, Part part,
            Nozzle nozzle) throws Exception {
        pipeline.setProperty("camera", camera);
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

    @Root
    public static class PartSettings {
        @Attribute
        protected boolean enabled;

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

        public CvPipeline getPipeline() {
            return pipeline;
        }

        public void setPipeline(CvPipeline pipeline) {
            this.pipeline = pipeline;
        }
    }
}
