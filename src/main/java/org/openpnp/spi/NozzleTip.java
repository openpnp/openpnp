package org.openpnp.spi;

import org.openpnp.model.Identifiable;
import org.openpnp.model.Length;
import org.openpnp.model.Named;
import org.openpnp.model.Solutions;

/**
 * A NozzleTip is the physical interface between a Nozzle and a Part.
 */
public interface NozzleTip extends Identifiable, Named, Solutions.Subject, WizardConfigurable, PropertySheetHolder {
    /**
     * @return The outside diameter of the NozzleTip at the lowest ca. 0.75mm. Most nozzle tips seem to have 
     * a cylindrical (i.e. constant diameter) point of about this height. This diameter is used to calculate 
     * pushing and dragging offsets. 
     */
    public Length getDiameterLow();
    /**
     * @return If the NozzleTip is allowed to be used for pushing and dragging. Should only be enabled for 
     * NozzleTips that are sturdy enough to take the lateral forces.  
     */
    public boolean isPushAndDragAllowed();

    /**
     * Perform any homing operation on each nozzle tip. The head and driver have already been homed
     * at this time. 
     * 
     * @throws Exception
     */
    void home() throws Exception;
}
