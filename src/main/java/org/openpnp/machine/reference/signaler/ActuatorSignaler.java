package org.openpnp.machine.reference.signaler;

import org.openpnp.ConfigurationListener;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Machine;
import org.openpnp.spi.base.AbstractJobProcessor;
import org.openpnp.spi.base.AbstractMachine;
import org.openpnp.spi.base.AbstractSignaler;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persist;

/**
 * An ActuatorSignaler can signal certain device or job states by using a machine actuator e.g. signaling lights
 */
public class ActuatorSignaler extends AbstractSignaler {

    protected Machine machine;
    protected Actuator actuator;

    @Attribute(required = false)
    protected String actuatorId;

    @Attribute(required = false)
    protected AbstractJobProcessor.State jobState;

    @Attribute(required = false)
    protected AbstractMachine.State machineState;

    public ActuatorSignaler() {
        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            @Override
            public void configurationLoaded(Configuration configuration) throws Exception {
                machine = configuration.getMachine();
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
    
    public Actuator getActuator() {
        return actuator;
    }

    public void setActuator(Actuator actuator) {
        this.actuator = actuator;
    }

    public AbstractJobProcessor.State getJobState() {
        return jobState;
    }

    public void setJobState(AbstractJobProcessor.State jobState) {
        this.jobState = jobState;
    }

    public AbstractMachine.State getMachineState() {
        return machineState;
    }

    public void setMachineState(AbstractMachine.State machineState) {
        this.machineState = machineState;
    }
}
