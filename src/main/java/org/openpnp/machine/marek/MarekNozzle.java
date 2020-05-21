package org.openpnp.machine.marek;

import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Movable.MoveToOption;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Element;

public class MarekNozzle extends ReferenceNozzle {
    @Element(required=false)
    Length switchZ = new Length(-2.0, LengthUnit.Millimeters);
    
    public MarekNozzle() {
        super();
    }
    
    public MarekNozzle(String id) {
        super(id);
    }
    
    @Override
    public void moveTo(Location location, double speed, MoveToOption... options) throws Exception {
        Location currentLocation = getLocation().convertToUnits(location.getUnits());
        location = toHeadLocation(location, currentLocation);

        //Marek change: section below is to fire actuator for pneumatic head control in relation to Z location 
        double switchZ = this.switchZ.convertToUnits(location.getUnits()).getValue();
        if(getDownActuator()!=null) {
            if(location.getZ() < switchZ && currentLocation.getZ() >= switchZ) {
                Logger.debug("{}.moveTo(Nozzle Down)", getId());
                getDownActuator().actuate(true);
            }
            if(location.getZ()>= switchZ && currentLocation.getZ() < switchZ) {
                Logger.debug("{}.moveTo(Nozzle Up)", getId());
                getDownActuator().actuate(false);
            }
        }

        ((ReferenceHead) getHead()).moveTo(this, location, getHead().getMaxPartSpeed() * speed, options);
        getMachine().fireMachineHeadActivity(head);
    }

    Actuator getDownActuator() {
        return getHead().getMachine().getActuator(getId());
    }

    public Length getSwitchZ() {
        return switchZ;
    }

    public void setSwitchZ(Length switchZ) {
        this.switchZ = switchZ;
    }
}
