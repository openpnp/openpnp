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
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Motion;
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
    public void home(ReferenceMachine machine) throws Exception {
        delegate.home(machine);
    }

    @Override
    public void setGlobalOffsets(ReferenceMachine machine, AxesLocation location)
            throws Exception {
        delegate.setGlobalOffsets(machine, location);
    }

    @Override
    public void moveTo(ReferenceHeadMountable hm, Motion motion, MoveToOption... options)
            throws Exception {
        
        // Take only this driver's axes.
        AxesLocation newDriverLocation = motion.getLocation();
        // Take the current driver location of the given axes.
        AxesLocation oldDriverLocation = new AxesLocation(newDriverLocation.getAxes(this), 
                (axis) -> (axis.getDriverLengthCoordinate()));
        if (!oldDriverLocation.matches(newDriverLocation)) {
            delegate.moveTo(hm, motion);
            // Store to axes
            newDriverLocation.setToDriverCoordinates(this);
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
        public void home(ReferenceMachine machine) throws Exception {

        }

        @Override
        public void setGlobalOffsets(ReferenceMachine machine, AxesLocation location)
                throws Exception {
        }
 
        @Override
        public void moveTo(ReferenceHeadMountable hm, Motion motion, MoveToOption... options)
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
        public void waitForCompletion(ReferenceHeadMountable hm, CompletionType completionType) throws Exception {
        }

        @Deprecated
        @Override
        public void migrateDriver(ReferenceMachine machine) throws Exception {
        }

        @Override
        public boolean isUsingLetterVariables() {
            return false;
        }

        @Override
        public Length getFeedRatePerSecond() {
            return null;
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
    public void waitForCompletion(ReferenceHeadMountable hm, CompletionType completionType) throws Exception {
    }

    @Override
    public boolean isUsingLetterVariables() {
        return false;
    }

    @Deprecated
    @Override
    public void migrateDriver(ReferenceMachine machine) throws Exception {
        machine.addDriver(this);
        createAxisMappingDefaults(machine);
    }
}
