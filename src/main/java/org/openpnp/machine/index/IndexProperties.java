package org.openpnp.machine.index;

import org.openpnp.spi.Machine;

public class IndexProperties {
    private static final String FEEDER_COMMUNICATION_MAX_RETRY = "IndexMachines.FeederCommunicationMaxRetry";

    private final Machine machine;

    public IndexProperties(Machine machine) {
        this.machine = machine;
    }

    public int getFeederCommunicationMaxRetry() {
        Integer maxRetry = (Integer) machine.getProperty(FEEDER_COMMUNICATION_MAX_RETRY);

        if(maxRetry == null) {
            maxRetry = 3;
            setFeederCommunicationMaxRetry(maxRetry);
        }

        return maxRetry;
    }

    public void setFeederCommunicationMaxRetry(int maxRetry) {
        machine.setProperty(FEEDER_COMMUNICATION_MAX_RETRY, maxRetry);
    }
}
