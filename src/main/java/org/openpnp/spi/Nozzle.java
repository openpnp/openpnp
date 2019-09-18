package org.openpnp.spi;

import java.util.Set;

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
     * Commands the Nozzle to perform it's pick operation. Generally this just consists of turning
     * on the vacuum. When this is called during job processing the processor will have already
     * positioned the nozzle over the part to be picked and lowered it to the correct height. Some
     * implementations may choose to do further work in pick(), such as firing air cylinders,
     * monitoring pressure sensors, etc.
     * 
     * @throws Exception
     */
    public void pick(Part part) throws Exception;

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
     * Get the part that is currently picked on the Nozzle, or null if none is picked.
     * Should typically be non-null after a pick operation and before a place operation and null
     * after a pick operation. Of note, it should be non-null after a failed pick operation
     * so that the system can determine which part it may need to discard. It may also be null
     * if a user initiated, manual, pick is performed with no Part to reference. 
     */
    public Part getPart();
    
    /**
     * Returns true if the isPartDetected() method is available. Some machines do not have
     * vacuum sensors or other part detection sensors, so this feature is optional.
     * @return
     */
    public boolean isPartDetectionEnabled();
    
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
    
    public Set<NozzleTip> getCompatibleNozzleTips();
    
    public void addCompatibleNozzleTip(NozzleTip nt);
    
    public void removeCompatibleNozzleTip(NozzleTip nt);
    
    public void calibrate() throws Exception;
    public boolean isCalibrated();
}
