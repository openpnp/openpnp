package org.openpnp.machine.index;

import org.openpnp.spi.Machine;

public class IndexProperties {
    private static final String FEEDER_COMMUNICATION_MAX_RETRY = "IndexMachines.FeederCommunicationMaxRetry";
    private static final String MAX_FEEDER_ADDRESS = "IndexMachines.MaxFeederAddress";

    private Machine machine;

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

    public int getMaxFeederAddress() {
        Integer maxFeederAddress = (Integer) machine.getProperty(MAX_FEEDER_ADDRESS);

        if(maxFeederAddress == null) {
            maxFeederAddress = 32;
            setMaxFeederAddress(maxFeederAddress);
        }

        return maxFeederAddress;
    }

    public void setMaxFeederAddress(int maxFeederAddress) {
        machine.setProperty(MAX_FEEDER_ADDRESS, maxFeederAddress);
    }
}
