package org.openpnp.machine.reference.driver;

import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.ReferenceDriver;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.machine.reference.ReferenceHeadMountable;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.simpleframework.xml.Attribute;

public class NullDriver2 implements ReferenceDriver {
    @Attribute(required=false)
    private String dummy;
    
    @Override
    public void home(ReferenceHead head) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void moveTo(ReferenceHeadMountable hm, Location location,
            double speed) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public Location getLocation(ReferenceHeadMountable hm) {
        return new Location(LengthUnit.Millimeters);
    }

    @Override
    public void pick(ReferenceNozzle nozzle) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void place(ReferenceNozzle nozzle) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void actuate(ReferenceActuator actuator, boolean on)
            throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void actuate(ReferenceActuator actuator, double value)
            throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void setEnabled(boolean enabled) throws Exception {
        // TODO Auto-generated method stub

    }

}
