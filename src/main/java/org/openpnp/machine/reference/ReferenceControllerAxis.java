package org.openpnp.machine.reference;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.wizards.ReferenceControllerAxisConfigurationWizard;
import org.openpnp.spi.base.AbstractControllerAxis;

public class ReferenceControllerAxis extends AbstractControllerAxis {

    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceControllerAxisConfigurationWizard(this);
    }

}
