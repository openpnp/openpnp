package org.openpnp.spi;

import org.openpnp.model.Identifiable;
import org.openpnp.model.Part;

/**
 * A NozzleTip is the physical interface between a Nozzle and a Part. 
 */
public interface NozzleTip extends Identifiable, WizardConfigurable, PropertySheetConfigurable {
    public boolean canHandle(Part part);
}
