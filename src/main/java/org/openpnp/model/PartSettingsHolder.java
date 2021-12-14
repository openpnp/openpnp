package org.openpnp.model;

/**
 * Common base class of Parts and Packages, to handle assigned settings' inheritance. 
 *
 */
public interface PartSettingsHolder extends Identifiable {
    public abstract BottomVisionSettings getVisionSettings();
    public abstract void setVisionSettings(BottomVisionSettings visionSettings);
}
