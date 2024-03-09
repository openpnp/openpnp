package org.openpnp.spi;

import java.util.Set;

import org.openpnp.model.Length;
import org.openpnp.model.Location;
import org.openpnp.model.Part;

/**
 * A Nozzle is a tool capable of picking up parts and releasing them. It is attached to a Head and
 * may move entirely with the head or partially independent of it. A Nozzle has a current NozzleTip
 * that defines what types of Packages it can handle and it may have the capability of changing it's
 * NozzleTip.
 */
public interface Nozzle
        extends HeadMountable, WizardConfigurable, PropertySheetHolder {
    /**
     * Get the NozzleTip currently attached to the Nozzle.
     * 
     * @return
     */
    NozzleTip getNozzleTip();

    /**
     * Nozzle pick-to-place rotation mode. 
     *
     */
    public enum RotationMode {
        /**
         * The part is picked so it is at its absolute angle on the nozzle (as drawn in the E-CAD). This is the default.
         */
        AbsolutePartAngle,
        /**
         * The part is picked so it is rotated for placement, when the nozzle is at 0°.
         */
        PlacementAngle,
        /**
         * Rotation is minimized by just picking at whatever angle the nozzle happens to be.
         */
        MinimalRotation,
        
        /**
         * Mode needed when the nozzle has limited rotation articulation. 
         */
        LimitedArticulation
    }

    /**
     * @return the RotationMode of the Nozzle. See {@link Nozzle.RotationMode}.
     */
    public RotationMode getRotationMode();

    /**
     * @return the rotation mode offset currently set.
     */
    public Double getRotationModeOffset();

    /**
     * Set the rotation mode offset to be applied.
     * 
     * @param rotationModeOffset
     */
    void setRotationModeOffset(Double rotationModeOffset);

    /**
     * @return Whether the bottom vision aligment of parts adjust the Rotation Mode of the nozzle to include the 
     * alignment rotation offset.
     */
    public boolean isAligningRotationMode();

    /**
     * Prepare the Nozzle for the next placement rotation. This will apply a rotation offset to the Nozzle 
     * that implements the {@link RotationMode} and is subject to articulation limits, if present.
     * 
     * @param pickLocation
     * @param placementLocation
     * @throws Exception
     */
    void prepareForPickAndPlaceArticulation(Location pickLocation, Location placementLocation) throws Exception;

    /**
     * Move the Nozzle to the given feeder pick location. This will move at safe Z and position the Nozzle
     * so it is ready for {@link #pick(Part)}. This might or might not involve offsets and actions for 
     * contact-probing e.g. to determine the feeder's calibrated Z. 
     * 
     * @param feeder 
     * 
     * @throws Exception
     */
    public void moveToPickLocation(Feeder feeder) throws Exception;

    /**
     * Commands the Nozzle to perform it's pick operation. Generally this just consists of turning
     * on the vacuum. When this is called during job processing the processor will have already
     * positioned the nozzle over the part to be picked and lowered it to the correct height. Some
     * implementations may choose to do further work in pick(), such as firing air cylinders,
     * monitoring pressure sensors, probing for contact etc.
     * 
     * @throws Exception
     */
    public void pick(Part part) throws Exception;

    /**
     * Move the Nozzle to the given placementLocation. This will move at safe Z and position the Nozzle
     * so it is ready for {@link #place()}. This might or might not involve offsets and actions for 
     * contact-probing. 
     * 
     * @param placementLocation
     * @param part Part to be placed, null on discard. 
     * @throws Exception
     */
    void moveToPlacementLocation(Location placementLocation, Part part) throws Exception;

    /**
     * Commands the Nozzle to perform it's place operation. Generally this just consists of
     * releasing vacuum and may include a puff of air to set the Part. When this is called during
     * job processing the processor will have already positioned the nozzle over the part to be
     * placed and lowered it to the correct height.
     * 
     * @throws Exception
     */
    public void place() throws Exception;

    /**
     * Changer interface:
     * 
     * Command the Nozzle to load the given NozzleTip as it's current NozzleTip. If this returns
     * without Exception then subsequently calling getNozzleTip() should return the same NozzleTip
     * as was passed to this call.
     * 
     * If the specified NozzleTip is already loaded this method should do nothing.
     * 
     * @param nozzleTip
     * @throws Exception
     */
    public void loadNozzleTip(NozzleTip nozzleTip) throws Exception;

    /**
     * Changer interface:
     * 
     * Unload the current NozzleTip from the Nozzle, leaving it empty.
     * 
     * After this call getNozzleTip() should return null.
     * 
     * @throws Exception
     */
    public void unloadNozzleTip() throws Exception;
    
    /**
     * Changer interface:
     * 
     * Unload the current NozzleTip from the Nozzle, leaving it empty.
     * 
     * After this call getNozzleTip() should return null.
     * 
     * This is a special version of the unloadNozzleTip() method that does not interrupt on manual nozzle tip
     * changes if a loadNozzleTip() is known to follow.
     * 
     * @param loadNozzleTipFollows If true, a loadNozzleTip() call will follow and no interrutp for unload will be generated.
     * @throws Exception
     */
    public void unloadNozzleTip(boolean loadNozzleTipFollows) throws Exception;

    /**
     * Get the part that is currently picked on the Nozzle, or null if none is picked.
     * Should typically be non-null after a pick operation and before a place operation and null
     * after a pick operation. Of note, it should be non-null after a failed pick operation
     * so that the system can determine which part it may need to discard. It may also be null
     * if a user initiated, manual, pick is performed with no Part to reference. 
     */
    public Part getPart();

    public enum PartOnStep {
        AfterPick,
        Align,
        BeforePlace
    }

    /**
     * Returns true if the isPartOn() method is available. Some machines do not have
     * vacuum sensors or other part detection sensors, so this feature is optional.
     * 
     * @param step determines which JobProcessor Step wants to perform the check 
     * @return
     */
    public boolean isPartOnEnabled(PartOnStep step);
    

    public enum PartOffStep {
        AfterPlace,
        BeforePick
    }

    /**
     * Returns true if the isPartOff() method is available. Some machines do not have
     * vacuum sensors or other part detection sensors, so this feature is optional.
     * 
     * @param step determines which JobProcessor Step wants to perform the check 
     * @return
     */
    public boolean isPartOffEnabled(PartOffStep step);

    /**
     * Returns true if a part appears to be on the nozzle. This is typically implemented by
     * checking a vacuum level range, but other methods such as laser or vision detection
     * are possible.
     * @return
     */
    public boolean isPartOn() throws Exception;
    
    /**
     * Returns true if a part appears to be off the nozzle. This is typically implemented by
     * checking a vacuum level range, but other methods such as laser or vision detection
     * are possible.
     * @return
     */
    public boolean isPartOff() throws Exception;
    
    /**
     * @return A set of nozzle tips compatible with this nozzle. 
     */
    public Set<NozzleTip> getCompatibleNozzleTips();

    /**
     * @param part
     * @return A set of nozzle tips compatible with this nozzle and the given part.
     */
    public Set<NozzleTip> getCompatibleNozzleTips(Part part);

    public void addCompatibleNozzleTip(NozzleTip nt);
    
    public void removeCompatibleNozzleTip(NozzleTip nt);
    
    boolean isNozzleTipChangedOnManualFeed();

    public void calibrate() throws Exception;
    public boolean isCalibrated();

    /**
     * @return The height of the part currently on the nozzle. If the part height is not yet 
     * known, the maximum part height configured on the nozzle tip is returned.
     * If no part is on the nozzle, a zero Length is returned.  
     */
    default Length getSafePartHeight(){
        return getSafePartHeight(getPart());
    }

    /**
     * @return The height of the given part. If the part height is not yet 
     * known (to be probed), the maximum part height configured on the nozzle tip is returned.
     * If part is null, a zero Length is returned.  
     */
    Length getSafePartHeight(Part part);

}
