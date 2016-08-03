package org.openpnp.machine.reference.vision;

import org.apache.commons.io.IOUtils;
import org.opencv.core.RotatedRect;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.vision.wizards.ReferenceBottomVisionConfigurationWizard;
import org.openpnp.machine.reference.vision.wizards.ReferenceBottomVisionPartConfigurationWizard;
import org.openpnp.model.*;
import org.openpnp.spi.Head;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PartAlignment;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.Utils2D;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.Root;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public class CenteringStage implements PartAlignment {
    private static final Logger logger = LoggerFactory.getLogger(CenteringStage.class);

    @Attribute(required = false)
    protected boolean enabled = false;

    @ElementMap(required = false)
    protected Map<String, PartSettings> partSettingsByPartId = new HashMap<>();

    @Override
    public PartAlignmentOffset findOffsets(Part part, BoardLocation boardLocation, Location placementLocation, Nozzle nozzle) throws Exception {
        PartSettings partSettings = getPartSettings(part);

        if (!isEnabled() || !partSettings.isEnabled()) {
            return new PartAlignmentOffset(new Location(LengthUnit.Millimeters),false);
        }

        // move to our centering stage *TODO configurable location*
        Location startLocation = new Location(LengthUnit.Millimeters, 0,0,0,0 );
        Length partHeight = part.getHeight();
        Location partHeightLocation =
                new Location(partHeight.getUnits(), 0, 0, partHeight.getValue(), 0);
        startLocation = startLocation.add(partHeightLocation).derive(null, null, null, 0d);

        MovableUtils.moveToLocationAtSafeZ(nozzle, startLocation);

        // place the part on our centering stage...
        nozzle.place();
        nozzle.moveToSafeZ();

        // clamp up the centering stage jaw
        Head head = nozzle.getHead();
        Actuator centeringActuator = head.getActuatorByName("centeringStage");
        centeringActuator.actuate(true);

        // calculate final rotation...
        Location finalLocation = Utils2D.calculateBoardPlacementLocation(boardLocation,placementLocation);

        // rotate the stage to final location
        // TODO....

        // unclamp the centering stage jaw
        centeringActuator.actuate(false);

        // pick the part on our centering stage...
        MovableUtils.moveToLocationAtSafeZ(nozzle, startLocation);
        nozzle.pick(part);
        nozzle.moveToSafeZ();

        // return a part alignment structure with preRotated flag set, so job processor will not do any further
        // rotations, also return the final calculated location
        return new PartAlignmentOffset(finalLocation,true);
    }


    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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
        return null;
       // return new PropertySheet[] {
              //  new PropertySheetWizardAdapter(new ReferenceBottomVisionConfigurationWizard(this))};
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
            partSettings = new PartSettings();
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
          //  partSettings.getPipeline().setCamera(VisionUtils.getBottomVisionCamera());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Root
    public static class PartSettings {
        @Attribute
        protected boolean enabled;


        public PartSettings() {

        }
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

    }
}
