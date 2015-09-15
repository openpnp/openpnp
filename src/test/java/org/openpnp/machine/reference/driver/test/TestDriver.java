package org.openpnp.machine.reference.driver.test;

import java.io.IOException;

import javax.swing.Action;
import javax.swing.Icon;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.ReferenceDriver;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.machine.reference.ReferenceHeadMountable;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.machine.reference.ReferencePasteDispenser;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.PropertySheetHolder;
import org.simpleframework.xml.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestDriver implements ReferenceDriver {
    private final static Logger logger = LoggerFactory
            .getLogger(TestDriver.class);
    
    @Attribute(required=false)
    private String dummy;
    
    private Location location = new Location(LengthUnit.Millimeters, 0, 0, 0, 0);
    
    private ReferenceDriver delegate = new TestDriverDelegate();
    
    public void setDelegate(ReferenceDriver delegate) {
        this.delegate = delegate;
    }
    
    @Override
    public Wizard getConfigurationWizard() {
        return null;
    }

    @Override
    public void home(ReferenceHead head) throws Exception {
        logger.debug("home()");
        location = new Location(LengthUnit.Millimeters, 0, 0, 0, 0);
        delegate.home(head);
    }

    @Override
    public void moveTo(ReferenceHeadMountable hm, Location location,
            double speed) throws Exception {
        // Subtract the offsets from the incoming Location. This converts the
        // offset coordinates to driver / absolute coordinates.
        location = location.subtract(hm.getHeadOffsets());

        // Convert the Location to millimeters, since that's the unit that
        // this driver works in natively.
        location = location.convertToUnits(LengthUnit.Millimeters);

        // Get the current location of the Head that we'll move
        Location hl = this.location;
        
        hl = hl.derive(
                Double.isNaN(location.getX()) ? null : location.getX(),
                Double.isNaN(location.getY()) ? null : location.getY(),
                Double.isNaN(location.getZ()) ? null : location.getZ(),
                Double.isNaN(location.getRotation()) ? null : location.getRotation());

        if (!this.location.equals(hl)) {
            this.location = hl;
            
            logger.debug("moveTo({}, {}, {})", new Object[] { hm, this.location, speed });
            
            delegate.moveTo(hm, this.location, speed);
        }
    }

    @Override
    public Location getLocation(ReferenceHeadMountable hm) {
        return location;
    }

    @Override
    public void pick(ReferenceNozzle nozzle) throws Exception {
        logger.debug("pick({} {})", nozzle, nozzle.getNozzleTip());
        delegate.pick(nozzle);
    }

    @Override
    public void place(ReferenceNozzle nozzle) throws Exception {
        logger.debug("place({} {})", nozzle, nozzle.getNozzleTip());
        delegate.place(nozzle);
    }

    @Override
    public void actuate(ReferenceActuator actuator, boolean on)
            throws Exception {
        logger.debug("actuate({}, {})", actuator, on);
        delegate.actuate(actuator, on);
    }

    @Override
    public void actuate(ReferenceActuator actuator, double value)
            throws Exception {
        logger.debug("actuate({}, {})", actuator, value);
        delegate.actuate(actuator, value);
    }
    
    @Override
    public void dispense(ReferencePasteDispenser dispenser,
            Location startLocation, Location endLocation,
            long dispenseTimeMilliseconds) throws Exception {
    }

    @Override
    public void setEnabled(boolean enabled) throws Exception {
        logger.debug("setEnabled({})", enabled);
        delegate.setEnabled(enabled);
    }
    
    public static class TestDriverDelegate implements ReferenceDriver {

        @Override
        public Wizard getConfigurationWizard() {
            // TODO Auto-generated method stub
            return null;
        }

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
            // TODO Auto-generated method stub
            return null;
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
        public void dispense(ReferencePasteDispenser dispenser,
                Location startLocation, Location endLocation,
                long dispenseTimeMilliseconds) throws Exception {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void setEnabled(boolean enabled) throws Exception {
            // TODO Auto-generated method stub
            
        }

        @Override
        public String getPropertySheetHolderTitle() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public PropertySheetHolder[] getChildPropertySheetHolders() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public PropertySheet[] getPropertySheets() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Action[] getPropertySheetHolderActions() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Icon getPropertySheetHolderIcon() {
            // TODO Auto-generated method stub
            return null;
        }
        
        @Override
        public void close() throws IOException {
            // TODO Auto-generated method stub
            
        }
    }

    @Override
    public String getPropertySheetHolderTitle() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Icon getPropertySheetHolderIcon() {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public void close() throws IOException {
        // TODO Auto-generated method stub
        
    }
}
