package org.openpnp.machine.reference.signaler;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.signaler.wizards.ActuatorSignalerConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Machine;
import org.openpnp.spi.base.AbstractJobProcessor;
import org.openpnp.spi.base.AbstractMachine;
import org.openpnp.spi.base.AbstractSignaler;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.core.Persist;

/**
 * An ActuatorSignaler can signal certain device or job states by using a machine actuator e.g. signaling lights
 */
public class ActuatorSignaler extends AbstractSignaler {

    protected Actuator actuator;

    @Attribute(required = false)
    protected String actuatorId;

    @Attribute(required = false)
    protected AbstractJobProcessor.State jobState;

    @Attribute(required = false)
    @Deprecated // not used anymore, but in place because the current xml parser can not handle extra data
    protected AbstractMachine.State machineState;

    public ActuatorSignaler() {
        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            @Override
            public void configurationLoaded(Configuration configuration) throws Exception {
                Machine machine = configuration.getMachine();
                actuator = machine.getActuator(actuatorId);
            }
        });
    }
    
    @Persist
    public void persist() {
        if (actuator != null) {
            actuatorId = actuator.getId();
        }
    }

    // update given actuator to newState
    // only execute an update if the state changes
    private void updateActuatorState(boolean newState) {
        if (actuator != null                                 // make sure an actuator is defined
                && (   actuator.isActuated() == null         // some actuator don't provide isActuated()
                    || actuator.isActuated() != newState)) { // if they do, check if the new state is different
            try {
            	actuator.actuate(newState);                  // then set new state
            } catch (Exception e) {
                e.printStackTrace();
            }
    	}
    }
    
    @Override
    public void signalJobProcessorState(AbstractJobProcessor.State state) {
        if(actuator != null && jobState != null) {
            updateActuatorState(state == jobState);
        }
    }
    
    public Actuator getActuator() {
        return actuator;
    }

    public void setActuator(Actuator actuator) {
        this.actuator = actuator;
        firePropertyChange("actuator", null, actuator);
    }

    public AbstractJobProcessor.State getJobState() {
        return jobState;
    }

    public void setJobState(AbstractJobProcessor.State jobState) {
        this.jobState = jobState;
        firePropertyChange("jobState", null, jobState);
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new ActuatorSignalerConfigurationWizard(this);
    }
}
