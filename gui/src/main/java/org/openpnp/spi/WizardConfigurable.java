package org.openpnp.spi;

import org.openpnp.gui.support.Wizard;

public interface WizardConfigurable {
    /**
     * Gets a Wizard that can be used to configure this object.
     * @return
     */
    Wizard getConfigurationWizard();
}
