package org.openpnp.machine.reference.driver.test;

import java.io.IOException;

import javax.swing.Action;
import javax.swing.Icon;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.ReferenceHeadMountable;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Motion.MoveToCommand;
import org.openpnp.spi.Driver;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MotionPlanner.CompletionType;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.base.AbstractDriver;
import org.simpleframework.xml.Attribute;

public class TestDriver extends AbstractDriver implements Driver {
    @Attribute(required = false)
    private String dummy;

    private Driver delegate = new TestDriverDelegate();

    public void setDelegate(Driver delegate) {
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
    public AxesLocation getMomentaryLocation(long timeout) throws Exception {
        return delegate.getMomentaryLocation(-1);
    }

    @Override
    public void moveTo(ReferenceHeadMountable hm, MoveToCommand move)
            throws Exception {
        
        // Take only this driver's axes.
        AxesLocation newDriverLocation = move.getLocation();
        // Take the current driver location of the given axes.
        AxesLocation oldDriverLocation = new AxesLocation(newDriverLocation.getAxes(this), 
                (axis) -> (axis.getDriverLengthCoordinate()));
        if (!oldDriverLocation.matches(newDriverLocation)) {
            delegate.moveTo(hm, move);
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

    public static class TestDriverDelegate implements Driver {
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
        public AxesLocation getMomentaryLocation(long timeout) throws Exception {
            return null;
        }

        @Override
        public void moveTo(ReferenceHeadMountable hm, MoveToCommand move)
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
        public MotionControlType getMotionControlType() {
            return MotionControlType.Full3rdOrderControl;
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

        @Override
        public boolean isUsingLetterVariables() {
            return false;
        }

        @Override
        public Length getFeedRatePerSecond() {
            return null;
        }

        @Override
        public boolean isMotionPending() {
            return false;
        }

        @Override
        public double getMinimumVelocity() {
            return 0;
        }
   }

    @Override
    public MotionControlType getMotionControlType() {
        return MotionControlType.Full3rdOrderControl;
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
    public void migrateDriver(Machine machine) throws Exception {
        machine.addDriver(this);
        createAxisMappingDefaults((ReferenceMachine) machine);
    }

    @Override
    public boolean isMotionPending() {
        return false;
    }
}
