package org.openpnp.machine.reference;

import org.apache.commons.io.IOUtils;
import org.opencv.core.RotatedRect;
import org.openpnp.ConfigurationListener;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.vision.wizards.ReferenceBottomVisionConfigurationWizard;
import org.openpnp.machine.reference.vision.wizards.ReferenceBottomVisionPartConfigurationWizard;
import org.openpnp.machine.reference.wizards.ZevatechCenteringStageConfigurationWizard;
import org.openpnp.machine.reference.wizards.ZevatechCenteringStagePartConfigurationWizard;
import org.openpnp.model.*;
import org.openpnp.model.Package;
import org.openpnp.spi.*;
import org.openpnp.util.MovableUtils;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.*;

import javax.swing.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ZevatechCenteringStage implements PartAlignment {
    @Attribute(required = false)
    protected boolean enabled = false;

    @ElementMap(required = false)
    protected Map<String, PartSettings> partSettingsByPartId = new HashMap<>();

    @Element(required = false)
    private Location centeringStageLocation = new Location(LengthUnit.Millimeters);

    @Attribute(required = false)
    private boolean allowIncompatibleParts;

    @Override
    public String getId() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getName() {
        return "CenteringStage";
    }

    @Override
    public void setName(String name) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean canHandle(Part part) {

        boolean result = enabled &
                (allowIncompatibleParts || getPartSettings(part).isEnabled());
        Logger.debug("{}.canHandle({}) => {}", part.getId(), result);
        return result;
    }

    @Override
    public PartAlignmentOffset findOffsets(Part part, BoardLocation boardLocation, Location placementLocation, Nozzle nozzle) throws Exception {

        // put the part on the centering stage
        nozzle.moveTo(centeringStageLocation);
        nozzle.place();

        Actuator actuator = nozzle.getHead().getActuatorByName("CenteringStage");
        if (actuator == null) {
            actuator = Configuration.get().getMachine().getActuatorByName("CenteringStage");
        }
        if (actuator == null) {
            throw new Exception("Post pick failed. Unable to find an actuator named " + "CenteringSTage");
        }

        // Final part rotation is a combination of alignment rotation, placement rotation and board rotation. If your board is always at zero rotation (fixed in a jig, perhaps) then you don't have to worry about that one, but you still have placement rotation to deal with.
        double rotation = 0;

        if(boardLocation != null && placementLocation != null)
        {
            rotation = placementLocation.getRotation() + boardLocation.getLocation().getRotation();
        }
        // actuate the stage centering jaw
        actuator.actuate(true);

        // rotate the stage to our desired angle
        actuator.actuate(rotation);

        // unactuate the stage centering jaw
        actuator.actuate(false);

        // pick the (hopefully now centered) part back up
        nozzle.pick(part);

        // set the stage back to zero
        actuator.actuate((double) 0);

        Location alignmentLocation = new Location(LengthUnit.Millimeters);
        if(placementLocation != null)
            alignmentLocation.derive(null,null,null,(double) 0);

        return new PartAlignmentOffset(null,true);
    }


    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return "ZevatechCenteringStage";
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {
                new PropertySheetWizardAdapter(new ZevatechCenteringStageConfigurationWizard(this))};
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

    public Location getLocation() {
        return centeringStageLocation;
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
     /*   try {
            partSettings.getPipeline().setCamera(VisionUtils.getBottomVisionCamera());
        }
        catch (Exception e) {
            e.printStackTrace();
        } */
        return new ZevatechCenteringStagePartConfigurationWizard(this, part);
    }

    @Root
    public static class PartSettings {
        @Attribute
        protected boolean enabled;

        public PartSettings() {

        }

        public PartSettings(ZevatechCenteringStage centeringStage) {
            setEnabled(centeringStage.isEnabled());
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

    }
}
