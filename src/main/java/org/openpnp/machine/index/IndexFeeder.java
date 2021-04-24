package org.openpnp.machine.index;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.index.protocol.ErrorTypes;
import org.openpnp.machine.index.protocol.IndexCommands;
import org.openpnp.machine.index.protocol.PacketResponse;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.ReferenceFeeder;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.spi.*;
import org.simpleframework.xml.Attribute;

import javax.swing.*;

import static org.openpnp.machine.index.protocol.IndexResponses.*;

public class IndexFeeder extends ReferenceFeeder {
    public static final String ACTUATOR_NAME = "INDEX_ACTUATOR";
    IndexProperties indexProperties;

    @Attribute(required = false)
    protected String hardwareId;

    protected Integer slotAddress = null;

    @Attribute(required = false)
    protected int partPitch = 4;

    protected boolean initialized = false;

    public IndexFeeder() {
        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            @Override
            public void configurationLoaded(Configuration configuration) {
                indexProperties = new IndexProperties(configuration.getMachine());
            }
        });
    }

    @Override
    public Location getPickLocation() throws Exception {
        return null;
    }

    @Override
    public void prepareForJob(boolean visit) throws Exception {
        for (int i = 0; i <= indexProperties.getFeederCommunicationMaxRetry(); i++) {
            findSlotAddressIfNeeded();

            initializeIfNeeded();

            if(initialized) {
                super.prepareForJob(visit);
                return;
            }
        }

        throw new Exception("Failed to find and initialize the feeder");
    }

    private void findSlotAddressIfNeeded() throws Exception {
        if (slotAddress != null) {
            return;
        }

        Actuator actuator = getActuator();
        String feederAddressResponseString = actuator.read(IndexCommands.getFeederAddress(hardwareId));

        PacketResponse response = GetFeederAddress.decode(feederAddressResponseString);
        if(! response.isOk()) {
            ErrorTypes error = response.getError();

            if(error == ErrorTypes.TIMEOUT) {
                return;
            }
        }
        setSlotAddress(response.getFeederAddress());
    }

    private void initializeIfNeeded() throws Exception {
        if(initialized || slotAddress == null) {
            return;
        }

        Actuator actuator = getActuator();
        String responseString = actuator.read(IndexCommands.initializeFeeder(slotAddress, hardwareId));
        PacketResponse response = InitializeFeeder.decode(responseString);

        if(!response.isOk()) {
            if(response.getError() == ErrorTypes.TIMEOUT) {
                slotAddress = null;
                return;
            } else if(response.getError() == ErrorTypes.WRONG_FEEDER_UUID) {
                IndexFeeder otherFeeder = findByHardwareId(response.getUuid());
                if(otherFeeder == null) {
                    otherFeeder = new IndexFeeder();
                    otherFeeder.setHardwareId(response.getUuid());
                    Configuration.get().getMachine().addFeeder(otherFeeder);
                }

                // This other feeder is in the slot we thought we were
                otherFeeder.setSlotAddress(response.getFeederAddress());

                return;
            }
        }

        initialized = true;
    }

    static Actuator getActuator() {
        Machine machine = Configuration.get().getMachine();
        Actuator actuator = machine.getActuatorByName(ACTUATOR_NAME);

        if(actuator == null) {
            actuator = new ReferenceActuator();
            actuator.setName(ACTUATOR_NAME);
            try {
                machine.addActuator(actuator);
            } catch (Exception exception) {
                exception.printStackTrace(); // TODO Probably need to log this, figure out why it can happen first
            }
        }

        return actuator;
    }

    @Override
    public void feed(Nozzle nozzle) throws Exception {
        for (int i = 0; i <= indexProperties.getFeederCommunicationMaxRetry(); i++) {
            findSlotAddressIfNeeded();
            initializeIfNeeded();

            Actuator actuator = getActuator();
            String ackResponseString = actuator.read(IndexCommands.moveFeedForward(slotAddress, partPitch * 10));

            PacketResponse ackResponse = MoveFeedForward.decode(ackResponseString);
            if (!ackResponse.isOk()) {
                ErrorTypes error = ackResponse.getError();
                if (error == ErrorTypes.UNINITIALIZED_FEEDER ||
                        error == ErrorTypes.TIMEOUT) {
                    slotAddress = null;
                    initialized = false;
                    continue;
                }
            }

            return;
        }

        throw new Exception("Failed to feed");
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return "Index feeder sheet holder title";
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {
                new PropertySheet() {
                    @Override
                    public String getPropertySheetTitle() {
                        return "Search Property Sheet";
                    }

                    @Override
                    public JPanel getPropertySheetPanel() {
                        JPanel panel = new JPanel();
                        panel.add(new JButton("Search or something IDK"));
                        return panel;
                    }
                }
        };
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        return new PropertySheetHolder[0];
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return new Action[0];
    }

    @Override
    public Wizard getConfigurationWizard() {
        return null;
    }

    @Override
    public String getName() {
        return "Something!";
    }

    /**
     * The IndexFeeder assumes you have a physical slot that is numbered 1 - 254. That
     * value is also used in the protocol as the address of the feeder once the feeder
     * is initialized.
     *
     * @return The slot address of this feeder or null if it doesn't have one.
     */
    public Integer getSlotAddress() {
        return slotAddress;
    }

    public void setSlotAddress(Integer slotAddress) {
        // Find any other index feeders and if they have this slot address, set their address to null
        IndexFeeder otherFeeder = findBySlotAddress(slotAddress);
        if(otherFeeder != null) {
            otherFeeder.slotAddress = null;
            otherFeeder.initialized = false;
        }

        this.slotAddress = slotAddress;
    }

    public String getHardwareId() {
        return hardwareId;
    }

    public void setHardwareId(String hardwareId) {
        this.hardwareId = hardwareId;
    }

    @Override
    public boolean isEnabled() {
        return super.isEnabled() &&
                getHardwareId() != null &&
                getPart() !=  null;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setPartPitch(int partPitch) {
        this.partPitch = partPitch;
    }

    public static IndexFeeder findByHardwareId(String hardwareId) {
        for (Feeder feeder : Configuration.get().getMachine().getFeeders()) {
            if(! (feeder instanceof IndexFeeder)) {
                continue;
            }

            IndexFeeder indexFeeder = (IndexFeeder) feeder;

            // Are we explicitly asking for feeders with null hardware Id?
            if(indexFeeder.hardwareId == null && hardwareId == null) {
                return indexFeeder;
            }

            if(indexFeeder.hardwareId != null && indexFeeder.hardwareId.equals(hardwareId)) {
                return indexFeeder;
            }
        }

        return null;
    }

    public static IndexFeeder findBySlotAddress(int slotAddress) {
        for (Feeder feeder : Configuration.get().getMachine().getFeeders()) {
            if(! (feeder instanceof IndexFeeder)) {
                continue;
            }

            IndexFeeder indexFeeder = (IndexFeeder) feeder;

            if(indexFeeder.slotAddress != null && indexFeeder.slotAddress.equals(slotAddress)) {
                return indexFeeder;
            }
        }

        return null;
    }

    public static void findAllFeeders() throws Exception {
        Machine machine = Configuration.get().getMachine();
        IndexProperties indexProperties = new IndexProperties(machine);
        Actuator actuator = getActuator();
        int maxFeederAddress = indexProperties.getMaxFeederAddress();

        for (int address = 1; address <= maxFeederAddress; address++) {
            String command = IndexCommands.getFeederId(address);
            String response = actuator.read(command);
            PacketResponse packetResponse = GetFeederId.decode(response);

            if(packetResponse.isOk()) {
                IndexFeeder otherFeeder = findByHardwareId(packetResponse.getUuid());
                if(otherFeeder == null) {
                    // Try to find an existing feeder without a hardware id before making a new one
                    otherFeeder = findByHardwareId(null);
                    if(otherFeeder == null) {
                        otherFeeder = new IndexFeeder();
                        Configuration.get().getMachine().addFeeder(otherFeeder);
                    }
                }
                otherFeeder.setHardwareId(packetResponse.getUuid());
                otherFeeder.setSlotAddress(address);
            } else if(packetResponse.getError() == ErrorTypes.TIMEOUT) {
                IndexFeeder otherFeeder = findBySlotAddress(address);
                if(otherFeeder != null) {
                    otherFeeder.slotAddress = null;
                    otherFeeder.initialized = false;
                }
            }
        }
    }
}
