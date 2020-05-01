package org.openpnp.machine.marek;

import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.machine.reference.ReferenceNozzleTip;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Movable.MoveToOption;
import org.pmw.tinylog.Logger;

public class MarekNozzle extends ReferenceNozzle {
    public MarekNozzle() {
        super();
    }
    
    public MarekNozzle(String id) {
        super(id);
    }
    
    @Override
    public void moveTo(Location location, double speed, MoveToOption... options) throws Exception {
        // Shortcut Double.NaN. Sending Double.NaN in a Location is an old API that should no
        // longer be used. It will be removed eventually:
        // https://github.com/openpnp/openpnp/issues/255
        // In the mean time, since Double.NaN would cause a problem for calibration, we shortcut
        // it here by replacing any NaN values with the current value from the driver.
        Location loc=location.derive(null,null,null,null);               // change
        location = location.convertToUnits(LengthUnit.Millimeters);      // change
        Location currentLocation = getLocation().convertToUnits(location.getUnits());
        
        if (Double.isNaN(location.getX())) {
            location = location.derive(currentLocation.getX(), null, null, null);
        }
        if (Double.isNaN(location.getY())) {
            location = location.derive(null, currentLocation.getY(), null, null);
        }
        if (Double.isNaN(location.getZ())) {
            location = location.derive(null, null, currentLocation.getZ(), null);
        }
        if (Double.isNaN(location.getRotation())) {
            location = location.derive(null, null, null, currentLocation.getRotation());
        }

        if (isLimitRotation() && !Double.isNaN(location.getRotation())
                && Math.abs(location.getRotation()) > 180) {
            if (location.getRotation() < 0) {
                location = location.derive(null, null, null, location.getRotation() + 360);
            }
            else {
                location = location.derive(null, null, null, location.getRotation() - 360);
            }
        }

        ReferenceNozzleTip calibrationNozzleTip = getCalibrationNozzleTip();
        if (calibrationNozzleTip != null && calibrationNozzleTip.getCalibration().isCalibrated(this)) {
            Location correctionOffset = calibrationNozzleTip.getCalibration().getCalibratedOffset(this, location.getRotation());
            location = location.subtract(correctionOffset);
            Logger.debug("{}.moveTo({}, {}) (runout compensation: {})", getName(), location, speed, correctionOffset);
        } else {
            Logger.debug("{}.moveTo({}, {})", getName(), location, speed);
        }

    // Cris's change: inhibit motion if no change, to inhibit unnecessary move repetition
        int n=0;
        if (location.getX()==currentLocation.getX()) { n++;
            location = location.derive(Double.NaN, null, null, null);
        }
        if (location.getY()==currentLocation.getY()) { n++;
            location = location.derive(null,Double.NaN, null, null);
        }
        if (location.getZ()==currentLocation.getZ()) { n++;
            location = location.derive(null,null,Double.NaN, null);
        }
        if (location.getRotation()==currentLocation.getRotation()) { n++;
            location = location.derive(null,null,null,Double.NaN);
        }
        if(n==4) { return; }

    //Marek change: section below is to fire actuator for pneumatic head control in relation to Z location (change at -2)
        if(getDownActuator()!=null) {
            if(location.getZ()<-2 && currentLocation.getZ() >=-2) {
                Logger.debug("{}.moveTo(Nozzle Down)", getId());
                getDownActuator().actuate(true);
            }
            if(location.getZ()>=-2 && currentLocation.getZ() <-2) {
                Logger.debug("{}.moveTo(Nozzle Up)", getId());
                getDownActuator().actuate(false);
            }
        }

    // Cri's change: avoid bug inside Gcode driver - it seems be higher code duplication, isn't it?
        if (Double.isNaN(location.getX())) {
            location = location.derive(currentLocation.getX(), null, null, null);
        }
        if (Double.isNaN(location.getY())) {
            location = location.derive(null, currentLocation.getY(), null, null);
        }
        if (Double.isNaN(location.getZ())) {
            location = location.derive(null, null, currentLocation.getZ(), null);
        }
        if (Double.isNaN(location.getRotation())) {
            location = location.derive(null, null, null, currentLocation.getRotation());
        }

        location = location.convertToUnits(loc.getUnits()); // convert units back; // Cri's change

        ((ReferenceHead) getHead()).moveTo(this, location, getHead().getMaxPartSpeed() * speed, options);
        getMachine().fireMachineHeadActivity(head);
    }

    @Override
    public void moveToSafeZ(double speed) throws Exception {
        Logger.debug("{}.moveToSafeZ({})", getName(), speed);
        Length safeZ = this.safeZ.convertToUnits(getLocation().getUnits());
        Location l = new Location(getLocation().getUnits(), Double.NaN, Double.NaN,
                safeZ.getValue(), Double.NaN);
        //getDriver().moveTo(this, l, getHead().getMaxPartSpeed() * speed); // change
        //getMachine().fireMachineHeadActivity(head);                       // change
        moveTo(l);                                                          // change
    }
    
    Actuator getDownActuator() {
        return getHead().getMachine().getActuator(getId());
    }
    
    ReferenceMachine getMachine() {
        return (ReferenceMachine) Configuration.get().getMachine();
    }
}
