package org.openpnp.machine.reference.axis;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.axis.wizards.ReferenceCamSlaveAxisConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.spi.base.AbstractMachine;
import org.openpnp.spi.base.AbstractSingleTransformedAxis;

/**
 * A TransformedAxis for heads with dual linear Z axes powered by one motor. The two Z axes are
 * defined as normal and negated. Normal gets the raw coordinate value and negated gets the same
 * value negated. So, as normal moves up, negated moves down.
 */
public class ReferenceCamSlaveAxis extends AbstractSingleTransformedAxis {

    public ReferenceCamSlaveAxis() {
        super();
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceCamSlaveAxisConfigurationWizard((AbstractMachine)Configuration.get().getMachine(), this);
    }

    @Override
    public Location transformToRaw(Location location) {
        return location;
    }

    @Override
    public Location transformFromRaw(Location location) {
        // it's reversible
        return transformToRaw(location);
    }
}
