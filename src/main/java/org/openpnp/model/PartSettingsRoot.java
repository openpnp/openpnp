package org.openpnp.model;

/**
 * Root part settings holder, i.e. the Vision configuration objects in the Machine Setup. 
 *
 */
public interface PartSettingsRoot extends PartSettingsHolder {
    public abstract PartSettingsHolder getParentHolder(PartSettingsHolder partSettingsHolder);
    public abstract AbstractVisionSettings getVisionSettings(PartSettingsHolder partSettingsHolder);
    public abstract AbstractVisionSettings getInheritedVisionSettings(PartSettingsHolder partSettingsHolder);
}
