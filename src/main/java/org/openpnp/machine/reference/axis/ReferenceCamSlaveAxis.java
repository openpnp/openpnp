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

    public ReferenceCamMasterAxis getMasterAxis() {
        if (inputAxis != null) {
            return (ReferenceCamMasterAxis)inputAxis;
        }
        return null;
    }

    @Override
    public double toRaw(Location location, double [][] invertedAffineTransform) {
        if (getMasterAxis() != null) {
            return getMasterAxis().toRaw(location, true);
        }
        return 0.0;
    }

    @Override
    public double toTransformed(Location location) {
        if (getMasterAxis() != null) {
            return getMasterAxis().toTransformed(location, true);
        }
        return 0.0;
    }
}
