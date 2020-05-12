package org.openpnp.machine.reference.axis;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.axis.wizards.ReferenceCamClockwiseAxisConfigurationWizard;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Configuration;
import org.openpnp.spi.base.AbstractMachine;
import org.openpnp.spi.base.AbstractSingleTransformedAxis;

/**
 * A TransformedAxis for heads with dual rocker or seesaw driven Z axes powered by one motor. 
 * The two Z axes are defined as counter-clockwise and clockwise according how the rocker rotates. 
 */
public class ReferenceCamClockwiseAxis extends AbstractSingleTransformedAxis {

    public ReferenceCamClockwiseAxis() {
        super();
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceCamClockwiseAxisConfigurationWizard((AbstractMachine)Configuration.get().getMachine(), this);
    }

    public ReferenceCamCounterClockwiseAxis getCounterClockwiseAxis() {
        if (inputAxis != null) {
            return (ReferenceCamCounterClockwiseAxis)inputAxis;
        }
        return null;
    }

    @Override
    public AxesLocation toRaw(AxesLocation location) throws Exception {
        if (getCounterClockwiseAxis() == null) {
            throw new Exception(getName()+" has no counter-clock input axis set");
        }
        double transformedCoordinate = location.getCoordinate(this);
        double rawCoordinate = getCounterClockwiseAxis().toRawCoordinate(transformedCoordinate, true);
        // store the transformed input axis (we're skipping the counter-clock axis)
        location = location.put(new AxesLocation(getCounterClockwiseAxis().getInputAxis(), rawCoordinate));
        // recurse
        return getCounterClockwiseAxis().getInputAxis().toRaw(location);
    }

    @Override
    public AxesLocation toTransformed(AxesLocation location) {
        if (getCounterClockwiseAxis() == null) {
            return location.put(new AxesLocation(this, 0.0));
        }
        // recurse
        location = getCounterClockwiseAxis().toTransformed(location);
        // get the input of the input (we're skipping the counter-clock axis)
        double rawCoordinate = location.getCoordinate(getCounterClockwiseAxis().getInputAxis());
        double transformedCoordinate  = getCounterClockwiseAxis().toTransformedCoordinate(rawCoordinate, true);
        return location.put(new AxesLocation(this, transformedCoordinate));
    }
}
