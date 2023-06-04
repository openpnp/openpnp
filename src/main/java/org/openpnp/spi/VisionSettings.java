package org.openpnp.spi;

import org.openpnp.model.Identifiable;
import org.openpnp.model.Named;

public interface VisionSettings extends WizardConfigurable, Identifiable, Named {

    /**
     * Reset the VisionSettings to their stock default.
     */
    void resetToDefault();
}
