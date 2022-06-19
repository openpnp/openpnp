package org.openpnp.model;

import java.util.List;

/**
 * Common base class of Parts, Packages, PartAlignment, to handle assigned settings' inheritance. 
 *
 */
public interface PartSettingsHolder extends Identifiable {
    public default String getShortName() {
        return getId();
    }

    public abstract BottomVisionSettings getBottomVisionSettings();
    public abstract void setBottomVisionSettings(BottomVisionSettings visionSettings);
    /**
     * @param baseHolder
     * @return the list if PartSettingsHolder that override/specialize the visions settings of this base. 
     */
    List<PartSettingsHolder> getSpecializedBottomVisionIn();
    void generalizeBottomVisionSettings();

    public abstract FiducialVisionSettings getFiducialVisionSettings();
    public abstract void setFiducialVisionSettings(FiducialVisionSettings visionSettings);
    /**
     * @param baseHolder
     * @return the list if PartSettingsHolder that override/specialize the visions settings of this base. 
     */
    List<PartSettingsHolder> getSpecializedFiducialVisionIn();
    void generalizeFiducialVisionSettings();
}
