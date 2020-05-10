package org.openpnp.machine.reference.driver.test;

import java.io.IOException;

import javax.swing.Action;
import javax.swing.Icon;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.ReferenceDriver;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.machine.reference.ReferenceHeadMountable;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.MappedAxes;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.base.AbstractDriver;
import org.simpleframework.xml.Attribute;

public class TestDriver extends AbstractDriver implements ReferenceDriver {
    @Attribute(required = false)
    private String dummy;

    private ReferenceDriver delegate = new TestDriverDelegate();

    public void setDelegate(ReferenceDriver delegate) {
        this.delegate = delegate;
    }

    @Override
    public void home(ReferenceHead head, MappedAxes mappedAxes, Location location) throws Exception {
        location = new Location(LengthUnit.Millimeters, 0, 0, 0, 0);
        delegate.home(head, mappedAxes, location);
    }

    @Override
    public void resetLocation(ReferenceHead head, MappedAxes mappedAxes, Location location)
            throws Exception {
        delegate.resetLocation(head, mappedAxes, location);
    }

    @Override
    public void moveTo(ReferenceHeadMountable hm, MappedAxes mappedAxes, Location location, double speed)
            throws Exception {
        // Convert the Location to millimeters, since that's the unit that
        // this driver works in natively.
        location = location.convertToUnits(LengthUnit.Millimeters);

        if (!mappedAxes.locationMatches(mappedAxes.getLocation(this), location, this)) {
            delegate.moveTo(hm, mappedAxes, location, speed);
            mappedAxes.setLocation(location, this);
        }
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
        public void home(ReferenceHead head, MappedAxes mappedAxes, Location location) throws Exception {

        }

        @Override
        public void resetLocation(ReferenceHead head, MappedAxes mappedAxes, Location location)
                throws Exception {
        }
 
        @Override
        public void moveTo(ReferenceHeadMountable hm, MappedAxes mappedAxes, Location location, double speed)
                throws Exception {

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

        @Override
        public String getName() {
            return null;
        }

        @Override
        public void setName(String name) {
        }

        @Override
        public String getId() {
            return null;
        }

        @Override
        public LengthUnit getUnits() {
            return LengthUnit.Millimeters;
        }

        @Deprecated
        @Override
        public void migrateDriver(ReferenceMachine machine) throws Exception {
        }

        @Override
        public boolean isSupportingPreMove() {
            return false;
        }
   }

    @Override
    public LengthUnit getUnits() {
        return LengthUnit.Millimeters;
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

    @Deprecated
    @Override
    public void migrateDriver(ReferenceMachine machine) throws Exception {
        machine.addDriver(this);
        createAxisMappingDefaults(machine);
    }
}
