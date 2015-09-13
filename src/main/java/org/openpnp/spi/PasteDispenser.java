package org.openpnp.spi;

import org.openpnp.model.Identifiable;
import org.openpnp.model.Location;
import org.openpnp.model.Named;

public interface PasteDispenser extends Identifiable, Named, HeadMountable, WizardConfigurable, PropertySheetHolder {
    /**
     * Command the dispenser to dispense from the startLocation to the
     * endLocation taking dispenseTimeMilliseconds milliseconds.
     * 
     * If the endLocation is null the dispenser should dispense at the
     * startLocation. If both locations are null the dispenser will dispense
     * at the current location.
     *  
     * @param startLocation
     * @param endLocation
     * @param dispenseTimeMilliseconds
     * @throws Exception
     */
    public void dispense(Location startLocation, Location endLocation, long dispenseTimeMilliseconds) throws Exception;
}
