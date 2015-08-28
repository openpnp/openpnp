package org.openpnp.spi;

import org.openpnp.model.Identifiable;
import org.openpnp.model.Named;

public interface PasteDispenser extends Identifiable, Named, HeadMountable, WizardConfigurable, PropertySheetHolder {
    public void dispense() throws Exception;
}
