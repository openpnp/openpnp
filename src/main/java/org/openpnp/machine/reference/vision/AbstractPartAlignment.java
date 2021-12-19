package org.openpnp.machine.reference.vision;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.vision.wizards.BottomVisionSettingsConfigurationWizard;
import org.openpnp.model.BottomVisionSettings;
import org.openpnp.model.Configuration;
import org.openpnp.model.PartSettingsHolder;
import org.openpnp.spi.PartAlignment;
import org.openpnp.util.VisionUtils;

public abstract class AbstractPartAlignment extends AbstractPartSettingsHolder implements PartAlignment {

    public BottomVisionSettings getInheritedVisionSettings(PartSettingsHolder partSettingsHolder) {
        while (partSettingsHolder != null) {
            BottomVisionSettings visionSettings = partSettingsHolder.getVisionSettings();
            if (visionSettings != null) {
                return visionSettings;
            }
            partSettingsHolder = partSettingsHolder.getParentHolder();
        }
        return null;
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
    public Wizard getPartConfigurationWizard(PartSettingsHolder partSettingsHolder) {
        BottomVisionSettings visionSettings = getInheritedVisionSettings(partSettingsHolder);
        try {
            visionSettings.getCvPipeline().setProperty("camera", VisionUtils.getBottomVisionCamera());
        }
        catch (Exception e) {
        }
        return new BottomVisionSettingsConfigurationWizard(visionSettings, partSettingsHolder);
    }

    @Override
    public PartSettingsHolder getParentHolder() {
        return null;
    }

}
