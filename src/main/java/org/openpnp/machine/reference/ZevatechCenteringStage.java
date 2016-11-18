package org.openpnp.machine.reference;

import org.apache.commons.io.IOUtils;
import org.opencv.core.RotatedRect;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.vision.wizards.ReferenceBottomVisionConfigurationWizard;
import org.openpnp.machine.reference.vision.wizards.ReferenceBottomVisionPartConfigurationWizard;
import org.openpnp.machine.reference.wizards.ZevatechCenteringStageConfigurationWizard;
import org.openpnp.machine.reference.wizards.ZevatechCenteringStagePartConfigurationWizard;
import org.openpnp.model.*;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PartAlignment;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.util.MovableUtils;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.Root;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public class ZevatechCenteringStage implements PartAlignment {
    @Attribute(required = false)
    protected boolean enabled = false;

    @ElementMap(required = false)
    protected Map<String, PartSettings> partSettingsByPartId = new HashMap<>();

    @Attribute(required = false)
    protected Location centeringStageLocation = new Location(LengthUnit.Millimeters, 0, 0, 0, 0);

    @Override
    public PartAlignmentOffset findOffsets(Part part, BoardLocation boardLocation, Location placementLocation, Nozzle nozzle) throws Exception {

        return new PartAlignmentOffset(null,false);
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
