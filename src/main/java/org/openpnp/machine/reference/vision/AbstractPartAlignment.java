package org.openpnp.machine.reference.vision;

import javax.swing.Action;
import javax.swing.Icon;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.vision.wizards.BottomVisionSettingsConfigurationWizard;
import org.openpnp.model.BottomVisionSettings;
import org.openpnp.model.Configuration;
import org.openpnp.model.Part;
import org.openpnp.model.PartSettingsHolder;
import org.openpnp.model.PartSettingsRoot;
import org.openpnp.spi.PartAlignment;
import org.openpnp.util.VisionUtils;

public abstract class AbstractPartAlignment extends AbstractPartSettingsHolder implements PartSettingsRoot, PartAlignment {

    @Override 
    public PartSettingsHolder getParentHolder(PartSettingsHolder partSettingsHolder) {
        if (partSettingsHolder instanceof Part) {
            return ((Part) partSettingsHolder).getPackage();
        }
        else if (partSettingsHolder instanceof org.openpnp.model.Package) {
            return this;
        }
        else {
            return null;
        }
    }

    @Override 
    public BottomVisionSettings getInheritedVisionSettings(PartSettingsHolder partSettingsHolder) {
        while (partSettingsHolder != null) {
            BottomVisionSettings visionSettings = partSettingsHolder.getBottomVisionSettings();
            if (visionSettings != null) {
                return visionSettings;
            }
            partSettingsHolder = getParentHolder(partSettingsHolder);
        }
        return null;
    }

    @Override
    public BottomVisionSettings getVisionSettings(PartSettingsHolder partSettingsHolder) {
        return partSettingsHolder.getBottomVisionSettings();
    }

    public static BottomVisionSettings getInheritedVisionSettings(PartSettingsHolder partSettingsHolder, boolean allowDisabled) {
        AbstractPartAlignment partAlignment = getPartAlignment(partSettingsHolder, allowDisabled);
        if (partAlignment != null) {
            BottomVisionSettings visionSettings = partAlignment.getInheritedVisionSettings(partSettingsHolder);
            if (partAlignment.canHandle(partSettingsHolder, allowDisabled)) {
                return visionSettings;
            }
        }
        return null;
    }

    public static AbstractPartAlignment getPartAlignment(PartSettingsHolder partSettingsHolder, boolean allowDisabled) {
        // Search for enabled first, then fall back to disabled, if allowed.
        for (boolean allowDisabledPass : (allowDisabled ? new boolean [] { false, true } : new boolean [] { false })) {
            for (PartAlignment partAlignment : Configuration.get().getMachine().getPartAlignments()) {
                if (partAlignment.isEnabled() || allowDisabledPass) {
                    // TODO: if there are ever multiple Alignment<->VisionSettings classes, they would have to be matched up here. 
                    if (partAlignment instanceof AbstractPartAlignment) {
                        return (AbstractPartAlignment) partAlignment;
                    }
                }
            }
        }
        return null;
    }

    public static AbstractPartAlignment getPartAlignment(PartSettingsHolder partSettingsHolder) {
        return getPartAlignment(partSettingsHolder, false);
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return null;
    }

    @Override
    public Icon getPropertySheetHolderIcon() {
        return null;
    }

    @Override
    public Wizard getPartConfigurationWizard(PartSettingsHolder partSettingsHolder) {
        BottomVisionSettings visionSettings = getInheritedVisionSettings(partSettingsHolder);
        try {
            visionSettings.getCvPipeline().setProperty("camera", VisionUtils.getBottomVisionCamera());
        }
        catch (Exception e) {
        }
        return new BottomVisionSettingsConfigurationWizard(visionSettings, partSettingsHolder);
    }
}
