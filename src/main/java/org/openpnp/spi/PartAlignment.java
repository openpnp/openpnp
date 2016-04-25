package org.openpnp.spi;

import org.openpnp.gui.support.Wizard;
import org.openpnp.model.Location;
import org.openpnp.model.Part;

/**
 * A method to allow after-pick, pre-place alignment of parts on the nozzle. Bottom vision
 * is an implementation of this interface, but other implementations could include laser
 * alignment or pit alignment.  
 */
public interface PartAlignment extends PropertySheetHolder {
    /**
     * Perform the part alignment operation. The method must return a Location containing
     * the offsets on the nozzle of the aligned part and these offsets will be applied
     * by the JobProcessor. The offsets returned may be zero if the alignment process
     * results in physical alignment of the part as in the case of pit based alignment. The
     * Z portion of the Location is ignored.
     * @param part
     * @param nozzle
     * @return
     * @throws Exception if the alignment fails for any reason. The caller may retry.
     */
    Location findOffsets(Part part, Nozzle nozzle) throws Exception;
    
    /**
     * Get a Wizard for configuring the PartAlignment instance properties for a specific
     * Part.
     * @param part
     * @return
     */
    Wizard getPartConfigurationWizard(Part part);
}
