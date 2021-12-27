package org.openpnp.model;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.vision.ReferenceFiducialLocator.PartSettings;
import org.openpnp.machine.reference.vision.wizards.FiducialVisionSettingsConfigurationWizard;

public class FiducialVisionSettings extends AbstractVisionSettings {

    @Override
    public Wizard getConfigurationWizard() {
        return new FiducialVisionSettingsConfigurationWizard(this, null);
    }

    public Wizard getConfigurationWizard(PartSettingsHolder settingsHolder) {
        return new FiducialVisionSettingsConfigurationWizard(this, settingsHolder);
    }

    public FiducialVisionSettings() {
        super(Configuration.createId("FVS"));
    }

    public FiducialVisionSettings(String id) {
        super(id);
    }

    public FiducialVisionSettings(PartSettings partSettings) {
        this();
        this.setEnabled(partSettings.isEnabled());
        this.setCvPipeline(partSettings.getPipeline());
    }

    public void setValues(FiducialVisionSettings another) {
        setEnabled(another.isEnabled());
        try {
            setCvPipeline(another.getCvPipeline().clone());
        }
        catch (CloneNotSupportedException e) {
        }
        Configuration.get().fireVisionSettingsChanged();
    }

}
