package org.openpnp.machine.reference.signaler;

import javax.swing.Action;
import javax.swing.Icon;

import org.openpnp.ConfigurationListener;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.base.AbstractJobProcessor;
import org.openpnp.spi.base.AbstractMachine;
import org.openpnp.spi.base.AbstractSignaler;
import org.simpleframework.xml.Attribute;

/**
 * An ActuatorSignaler can signal certain device or job states by using a machine actuator e.g. signaling lights
 */
public class ActuatorSignaler extends AbstractSignaler {

    protected ReferenceMachine machine;
    protected Actuator actuator;

    @Attribute(required = true)
    protected String actuatorId;

    @Attribute(required = false)
    protected AbstractJobProcessor.State jobState;

    @Attribute(required = false)
    protected AbstractMachine.State machineState;

    public ActuatorSignaler() {
        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            @Override
            public void configurationLoaded(Configuration configuration) throws Exception {
                machine = (ReferenceMachine) configuration.getMachine();
                actuator = machine.getActuator(actuatorId);
            }
        });
    }

    @Override
    public void signalMachineState(AbstractMachine.State state) {
        if(actuator != null && machineState != null) {
            try {
                if(state == machineState) {
                    this.actuator.actuate(true);
                } else {
                    this.actuator.actuate(false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void signalJobProcessorState(AbstractJobProcessor.State state) {
        if(actuator != null && jobState != null) {
            try {
                if(state == jobState) {
                    this.actuator.actuate(true);
                } else {
                    this.actuator.actuate(false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName() + " " + getName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        return new PropertySheetHolder[0];
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[0];
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return new Action[0];
    }

    @Override
    public Icon getPropertySheetHolderIcon() {
        return null;
    }
}
