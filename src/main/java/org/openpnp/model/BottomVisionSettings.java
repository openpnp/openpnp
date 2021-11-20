package org.openpnp.model;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.vision.wizards.VisionSettingsConfigurationWizard;

public class BottomVisionSettings extends AbstractVisionSettings {

    @Override
    public Wizard getConfigurationWizard() {
        return new VisionSettingsConfigurationWizard(this);
    }

    public BottomVisionSettings() {
    }

    public BottomVisionSettings(String id) {
        super(id);
    }
}
