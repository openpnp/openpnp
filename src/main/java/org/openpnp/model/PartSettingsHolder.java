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
    /**
     * @return the parent PartSettingsHolder in the Part -> Package -> PartAlignment hierarchy.
     */
    public abstract PartSettingsHolder getParentHolder();
    public abstract BottomVisionSettings getVisionSettings();
    public abstract void setVisionSettings(BottomVisionSettings visionSettings);
    public default void resetVisionSettings() { 
        setVisionSettings(null); 
    }
    /**
     * @param baseHolder
     * @return the list if PartSettingsHolder that override/specialize the visions settings of this base. 
     */
    List<PartSettingsHolder> getSpecializedIn();
    void resetSpecializedVisionSettings();
}
