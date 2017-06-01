package org.openpnp.machine.reference.vision;

import java.util.HashMap;
import java.util.Map;

import javax.swing.Action;
import javax.swing.Icon;

import org.apache.commons.io.IOUtils;
import org.opencv.core.RotatedRect;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraView;
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

        Camera camera = VisionUtils.getBottomVisionCamera();

        // Pre-rotate to minimize runout
        double preRotateAngle = 0;
        if ( boardLocation != null && placementLocation != null) {
            preRotateAngle =
                    Utils2D.calculateBoardPlacementLocation(boardLocation, placementLocation)
                           .getRotation();
        }

        // Create a location that is the Camera's X, Y, it's Z + part height
        // and a rotation of 0, unless preRotate is enabled
        Location startLocation = camera.getLocation();
        Length partHeight = part.getHeight();
        Location partHeightLocation =
                new Location(partHeight.getUnits(), 0, 0, partHeight.getValue(), 0);
        startLocation = startLocation.add(partHeightLocation)
                                     .derive(null, null, null, preRotate?preRotateAngle:null);

        MovableUtils.moveToLocationAtSafeZ(nozzle, startLocation);

        CvPipeline pipeline = partSettings.getPipeline();

        pipeline.setCamera(camera);
        pipeline.setNozzle(nozzle);
        pipeline.setValue(preRotateAngle);     
        pipeline.process();
        boolean postRotate = Double.isNaN(pipeline.getValue());
        preRotateAngle = preRotate || postRotate ? preRotateAngle : 0.;
        
        Result result = pipeline.getResult("result");
        if (!(result.model instanceof RotatedRect)) {
            throw new Exception("Bottom vision alignment failed for part " + part.getId()
                    + " on nozzle " + nozzle.getName() + ". No result found.");
        }
        RotatedRect rect = (RotatedRect) result.model;
        Logger.debug("Result rect {}", rect);

        // Create the offsets object. This is the physical distance from
        // the center of the camera to the located part.
        Location offsets = VisionUtils.getPixelCenterOffsets(camera, rect.center.x, rect.center.y);

        // We assume that the part is never picked more than 45º rotated
        // so if OpenCV tells us it's rotated more than 45º we correct
        // it. This seems to happen quite a bit when the angle of rotation
        // is close to 0.
        while (Math.abs(preRotateAngle) > 45) {
            if (preRotateAngle < 0) {
                preRotateAngle += 90;
            }
            else {
                preRotateAngle -= 90;
            }
        }

        double angle = rect.angle + preRotateAngle;    // angle is CW, preRotateAngle is CCW
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
        if(preRotate||postRotate) {
            Location placementLocation = new Location (LengthUnit.Millimeters);
            Location location =
                    new Location(LengthUnit.Millimeters).rotateXyCenterPoint(offsets,angle);
            location = location.derive(null, null, null, nozzle.getLocation().getRotation()-angle);
            offsets = location.subtract(offsets);
        }


        Logger.debug("Final offsets {}", offsets);

        OpenCvUtils.saveDebugImage(ReferenceBottomVision.class, "findOffsets", "result",
                pipeline.getWorkingImage());

        CameraView cameraView = MainFrame.get()
                                         .getCameraViews()
                                         .getCameraView(camera);
        String s = rect.size.toString() + " " + rect.angle + "°";
        cameraView.showFilteredImage(OpenCvUtils.toBufferedImage(pipeline.getWorkingImage()), s,
                1500);


        return new PartAlignmentOffset(offsets,preRotate||postRotate);
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



    @Override
    public String getId() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setName(String name) {
        // TODO Auto-generated method stub

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
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {
                new PropertySheetWizardAdapter(new ReferenceBottomVisionConfigurationWizard(this))};
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Icon getPropertySheetHolderIcon() {
        // TODO Auto-generated method stub
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
                        .setCamera(VisionUtils.getBottomVisionCamera());
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
