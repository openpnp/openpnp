package org.openpnp.machine.reference.driver.test;

import java.io.IOException;

import javax.swing.Action;
import javax.swing.Icon;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.ReferenceDriver;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.machine.reference.ReferenceHeadMountable;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Movable.MoveToOption;
import org.openpnp.spi.PropertySheetHolder;
import org.simpleframework.xml.Attribute;

public class TestDriver implements ReferenceDriver {
    @Attribute(required = false)
    private String dummy;

    private Location location = new Location(LengthUnit.Millimeters, 0, 0, 0, 0);

    private ReferenceDriver delegate = new TestDriverDelegate();

    public void setDelegate(ReferenceDriver delegate) {
        this.delegate = delegate;
    }

    @Override
    public void home(ReferenceHead head) throws Exception {
        location = new Location(LengthUnit.Millimeters, 0, 0, 0, 0);
        delegate.home(head);
    }

    @Override
    public void moveTo(ReferenceHeadMountable hm, Location location, double speed, MoveToOption...options)
            throws Exception {
        // Subtract the offsets from the incoming Location. This converts the
        // offset coordinates to driver / absolute coordinates.
        location = location.subtract(hm.getHeadOffsets());

        // Convert the Location to millimeters, since that's the unit that
        // this driver works in natively.
        location = location.convertToUnits(LengthUnit.Millimeters);

        // Get the current location of the Head that we'll move
        Location hl = this.location;

        hl = hl.derive(Double.isNaN(location.getX()) ? null : location.getX(),
                Double.isNaN(location.getY()) ? null : location.getY(),
                Double.isNaN(location.getZ()) ? null : location.getZ(),
                Double.isNaN(location.getRotation()) ? null : location.getRotation());

        if (!this.location.equals(hl)) {
            this.location = hl;
            delegate.moveTo(hm, this.location, speed);
        }
    }

    @Override
    public Location getLocation(ReferenceHeadMountable hm) {
        return location.add(hm.getHeadOffsets());
    }
    
    @Override
    public void actuate(ReferenceActuator actuator, boolean on) throws Exception {
        delegate.actuate(actuator, on);
    }

    @Override
    public void actuate(ReferenceActuator actuator, double value) throws Exception {
        delegate.actuate(actuator, value);
    }

    @Override
    public void setEnabled(boolean enabled) throws Exception {
        delegate.setEnabled(enabled);
    }

    public static class TestDriverDelegate implements ReferenceDriver {
        @Override
        public Wizard getConfigurationWizard() {
            return null;
        }

        @Override
        public void home(ReferenceHead head) throws Exception {

        }

        @Override
        public void moveTo(ReferenceHeadMountable hm, Location location, double speed, MoveToOption...options)
                throws Exception {

        }

        @Override
        public Location getLocation(ReferenceHeadMountable hm) {
            return null;
        }

        @Override
        public void actuate(ReferenceActuator actuator, boolean on) throws Exception {

        }

        @Override
        public void actuate(ReferenceActuator actuator, double value) throws Exception {

        }

        @Override
        public void setEnabled(boolean enabled) throws Exception {

        }

        @Override
        public String getPropertySheetHolderTitle() {
            return null;
        }

        @Override
        public PropertySheetHolder[] getChildPropertySheetHolders() {
            return null;
        }

        @Override
        public PropertySheet[] getPropertySheets() {
            return null;
        }

        @Override
        public Action[] getPropertySheetHolderActions() {
            return null;
        }

        @Override
        public Icon getPropertySheetHolderIcon() {
            return null;
        }

        @Override
        public void close() throws IOException {

        }
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return null;
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        return null;
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return null;
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return null;
    }

    @Override
    public Icon getPropertySheetHolderIcon() {
        return null;
    }

    @Override
    public void close() throws IOException {
    }
    
    @Override
    public Wizard getConfigurationWizard() {
        return null;
    }
}
