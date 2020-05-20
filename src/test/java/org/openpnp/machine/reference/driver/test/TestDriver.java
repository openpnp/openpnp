package org.openpnp.machine.reference.driver.test;

import java.io.IOException;

import javax.swing.Action;
import javax.swing.Icon;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.ReferenceDriver;
import org.openpnp.machine.reference.ReferenceHeadMountable;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.MappedAxes;
import org.openpnp.spi.MotionPlanner.CompletionType;
import org.openpnp.spi.Movable.MoveToOption;
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
    public void home(ReferenceMachine machine, MappedAxes mappedAxes) throws Exception {
        delegate.home(machine, mappedAxes);
    }

    @Override
    public void resetLocation(ReferenceMachine machine, MappedAxes mappedAxes, AxesLocation location)
            throws Exception {
        delegate.resetLocation(machine, mappedAxes, location);
    }

    @Override
    public void moveTo(ReferenceHeadMountable hm, MappedAxes mappedAxes, AxesLocation location, double speed, MoveToOption... options)
            throws Exception {
        if (!mappedAxes.locationsMatch(mappedAxes.getDriverLocation(), location)) {
            delegate.moveTo(hm, mappedAxes, location, speed);
            mappedAxes.setDriverLocation(location);
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
        public void home(ReferenceMachine machine, MappedAxes mappedAxes) throws Exception {

        }

        @Override
        public void resetLocation(ReferenceMachine machine, MappedAxes mappedAxes, AxesLocation location)
                throws Exception {
        }
 
        @Override
        public void moveTo(ReferenceHeadMountable hm, MappedAxes mappedAxes, AxesLocation location, double speed, MoveToOption... options)
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

        @Override
        public boolean isSupportingPreMove() {
            return false;
        }

        @Override
        public void waitForCompletion(ReferenceHeadMountable hm, MappedAxes mappedAxes,
                CompletionType completionType) throws Exception {
        }

        @Deprecated
        @Override
        public void migrateDriver(ReferenceMachine machine) throws Exception {
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

    @Override
    public void waitForCompletion(ReferenceHeadMountable hm, MappedAxes mappedAxes,
            CompletionType completionType) throws Exception {
    }

    @Deprecated
    @Override
    public void migrateDriver(ReferenceMachine machine) throws Exception {
        machine.addDriver(this);
        createAxisMappingDefaults(machine);
    }
}
