package org.openpnp.machine.reference.axis;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.axis.wizards.ReferenceNegatedAxisConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.spi.Axis;
import org.openpnp.spi.base.AbstractControllerAxis;
import org.openpnp.spi.base.AbstractMachine;
import org.openpnp.spi.base.AbstractTransformedAxis;
import org.openpnp.spi.base.AbstractSingleTransformedAxis;

/**
 * A TransformedAxis for heads with dual linear Z axes powered by one motor. The two Z axes are
 * defined as normal and negated. Normal gets the raw coordinate value and negated gets the same
 * value negated. So, as normal moves up, negated moves down.
 */
public class ReferenceNegatedAxis extends AbstractSingleTransformedAxis {

    public ReferenceNegatedAxis() {
        super();
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceNegatedAxisConfigurationWizard((AbstractMachine)Configuration.get().getMachine(), this);
    }

    @Override
    public double toRaw(Location location, double [][] invertedAffineTransform) {
        if (inputAxis != null) {
            switch(type) {
            case X:
                return inputAxis.toRaw(location.derive(-location.getX(), null, null, null), invertedAffineTransform);
            case Y:
                return inputAxis.toRaw(location.derive(null, -location.getY(), null, null), invertedAffineTransform);
            case Z:
                return inputAxis.toRaw(location.derive(null, null, -location.getZ(),  null), invertedAffineTransform);
            case Rotation:
                return inputAxis.toRaw(location.derive(null, null, null, -location.getRotation()), invertedAffineTransform);
            }
        }
        return 0.0;
    }

    @Override
    public double toTransformed(Location location) {
        if (inputAxis != null) {
            return -inputAxis.toTransformed(location);
        }
        return 0.0;
    }
}
