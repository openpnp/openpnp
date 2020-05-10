package org.openpnp.machine.reference.axis;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.axis.wizards.ReferenceCamClockwiseAxisConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.spi.base.AbstractMachine;
import org.openpnp.spi.base.AbstractSingleTransformedAxis;

/**
 * A TransformedAxis for heads with dual linear Z axes powered by one motor. The two Z axes are
 * defined as normal and negated. Normal gets the raw coordinate value and negated gets the same
 * value negated. So, as normal moves up, negated moves down.
 */
public class ReferenceCamClockwiseAxis extends AbstractSingleTransformedAxis {

    public ReferenceCamClockwiseAxis() {
        super();
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceCamClockwiseAxisConfigurationWizard((AbstractMachine)Configuration.get().getMachine(), this);
    }

    public ReferenceCamCounterClockwiseAxis getMasterAxis() {
        if (inputAxis != null) {
            return (ReferenceCamCounterClockwiseAxis)inputAxis;
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
