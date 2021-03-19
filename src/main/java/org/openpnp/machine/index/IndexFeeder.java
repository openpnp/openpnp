package org.openpnp.machine.index;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.index.protocol.ErrorTypes;
import org.openpnp.machine.index.protocol.IndexCommands;
import org.openpnp.machine.index.protocol.PacketResponse;
import org.openpnp.machine.reference.ReferenceFeeder;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;
import org.simpleframework.xml.Attribute;

import javax.swing.*;

import static org.openpnp.machine.index.protocol.IndexResponses.*;

public class IndexFeeder extends ReferenceFeeder {
    public static final String ACTUATOR_NAME = "INDEX_ACTUATOR";

    @Attribute(required = false)
    protected String hardwareId;

    protected Integer slotAddress = null;

    @Attribute(required = false)
    protected int partPitch = 4;

    protected boolean initialized = false;

    @Override
    public Location getPickLocation() throws Exception {
        return null;
    }

    @Override
    public void prepareForJob(boolean visit) throws Exception {
        // TODO Better limit the number of times this can run before throwing an exception
        for (int i = 0; i < 10; i++) {
            findSlotAddressIfNeeded();

            initializeIfNeeded();

            if(initialized) {
                break;
            }
        }

        super.prepareForJob(visit);
    }

    private void findSlotAddressIfNeeded() throws Exception {
        if (slotAddress != null) {
            return;
        }

        Actuator actuator = getActuator();
        String feederAddressResponseString = actuator.read(IndexCommands.getFeederAddress(hardwareId));

        PacketResponse response = GetFeederAddress.decode(feederAddressResponseString);
        setSlotAddress(response.getFeederAddress());
    }

    private void initializeIfNeeded() throws Exception {
        if(initialized) {
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
                otherFeeder.setSlotAddress(slotAddress);

                return;
            }
        }

        initialized = true;
    }

    private Actuator getActuator() {
        return Configuration.get().getMachine().getActuatorByName(ACTUATOR_NAME);
    }

    @Override
    public void feed(Nozzle nozzle) throws Exception {
        // TODO Better limit the number of times this can run before throwing an exception
        for (int i = 0; i < 10; i++) {
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
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return null;
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
        this.slotAddress = slotAddress;

        if(slotAddress == null) {
            return;
        }

        // Find any other index feeders and if they have this slot address, set their address to null
        for (Feeder feeder : Configuration.get().getMachine().getFeeders()) {
            if(! (feeder instanceof IndexFeeder)) {
                continue;
            }

            if(feeder == this) {
                continue;
            }

            IndexFeeder indexFeeder = (IndexFeeder) feeder;

            if(indexFeeder.slotAddress == null) {
                continue;
            }

            if(this.slotAddress.equals(indexFeeder.slotAddress)) {
                indexFeeder.slotAddress = null;
                indexFeeder.initialized = false;
            }
        }
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

            if(indexFeeder.hardwareId != null && indexFeeder.hardwareId.equals(hardwareId)) {
                return indexFeeder;
            }
        }

        return null;
    }
}
