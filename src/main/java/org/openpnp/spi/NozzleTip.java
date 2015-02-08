package org.openpnp.spi;

import org.openpnp.model.Identifiable;
import org.openpnp.model.Named;
import org.openpnp.model.Part;

/**
 * A NozzleTip is the physical interface between a Nozzle and a Part. 
 */
public interface NozzleTip extends Identifiable, Named, WizardConfigurable, PropertySheetHolder {
    public boolean canHandle(Part part);
}
