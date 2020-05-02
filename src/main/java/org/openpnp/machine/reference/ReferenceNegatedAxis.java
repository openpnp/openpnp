package org.openpnp.machine.reference;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.wizards.ReferenceNegatedAxisConfigurationWizard;
import org.openpnp.model.Location;
import org.openpnp.spi.base.AbstractTransformedAxis;

/**
 * A TransformedAxis for heads with dual linear Z axes powered by one motor. The two Z axes are
 * defined as normal and negated. Normal gets the raw coordinate value and negated gets the same
 * value negated. So, as normal moves up, negated moves down.
 */
public class ReferenceNegatedAxis extends AbstractTransformedAxis {

    ReferenceNegatedAxis() {
        super();
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceNegatedAxisConfigurationWizard(this);
    }

    @Override
    public Location transformToRaw(Location location) {
        switch(type) {
            case X:
                return location.derive(-location.getX(), null, null, null);
            case Y:
                return location.derive(null, -location.getY(), null, null);
            case Z:
                return location.derive(null, null, -location.getZ(),  null);
            case Rotation:
                return location.derive(null, null, null, -location.getRotation());
            default:
                return location;
        }
    }

    @Override
    public Location transformFromRaw(Location location) {
        // it's reversible
        return transformToRaw(location);
    }
}
