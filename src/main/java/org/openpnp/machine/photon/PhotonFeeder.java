package org.openpnp.machine.photon;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Action;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.photon.exceptions.FeedFailureException;
import org.openpnp.machine.photon.exceptions.FeederHasNoLocationOffsetException;
import org.openpnp.machine.photon.exceptions.NoSlotAddressException;
import org.openpnp.machine.photon.exceptions.UnconfiguredSlotException;
import org.openpnp.machine.photon.protocol.ErrorTypes;
import org.openpnp.machine.photon.protocol.PhotonBus;
import org.openpnp.machine.photon.protocol.PhotonBusInterface;
import org.openpnp.machine.photon.protocol.commands.GetFeederAddress;
import org.openpnp.machine.photon.protocol.commands.GetFeederId;
import org.openpnp.machine.photon.protocol.commands.InitializeFeeder;
import org.openpnp.machine.photon.protocol.commands.MoveFeedForward;
import org.openpnp.machine.photon.protocol.commands.MoveFeedStatus;
import org.openpnp.machine.photon.sheets.FeederPropertySheet;
import org.openpnp.machine.photon.sheets.GlobalConfigPropertySheet;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.ReferenceFeeder;
import org.openpnp.machine.reference.driver.GcodeDriver;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Solutions;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Driver;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.util.MovableUtils;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

public class PhotonFeeder extends ReferenceFeeder {
    public static final String ACTUATOR_DATA_NAME = "PhotonFeederData";
    PhotonProperties photonProperties;

    @Attribute(required = false)
    protected String hardwareId;

    protected Integer slotAddress = null;

    @Attribute(required = false)
    protected int partPitch = 4;

    protected boolean initialized = false;

    @Element(required = false)
    private Location offset;

    final private double correctionLimit = 5.0; // millimeters
    private Location pickCorrectionOffset = new Location(LengthUnit.Millimeters);
    private int visionsSinceLastFeed = 0;

    private static PhotonBusInterface photonBus;

    @Attribute(required = false)
    protected boolean moveWhileFeeding = true;

    public PhotonFeeder() {
        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            @Override
            public void configurationLoaded(Configuration configuration) {
                photonProperties = new PhotonProperties(configuration.getMachine());

                // Ensure actuators are added to the machine when it has PhotonFeeders
                getDataActuator();
                populatePhotonBus();
            }
        });
    }

    public static void setBus(PhotonBusInterface bus) {
        photonBus = bus;
    }

    public static PhotonBusInterface getBus() {
        populatePhotonBus();
        return photonBus;
    }

    private static void populatePhotonBus() {
        if (photonBus != null) {
            return;
        }

        photonBus = new PhotonBus(0, getDataActuator());
    }

    private Location getPickCorrectionOffset() throws Exception {
        return pickCorrectionOffset;
    }

    private Location getPickLocationWithoutError() throws Exception {
        verifyFeederLocationIsFullyConfigured();

        return offset.offsetWithRotationFrom(getSlot().getLocation());
    }

    @Override
    public Location getPickLocation() throws Exception {
        return getPickLocationWithoutError().add(getPickCorrectionOffset());
    }

    private void verifyFeederLocationIsFullyConfigured() throws NoSlotAddressException,
            UnconfiguredSlotException, FeederHasNoLocationOffsetException {
        if (slotAddress == null) {
            throw new NoSlotAddressException(
                    String.format("Photon Feeder with address %s has no address. Is it inserted?", hardwareId)
            );
        }

        if (getSlot().getLocation() == null) {
            throw new UnconfiguredSlotException(
                    String.format("The slot at address %s has no location configured.", slotAddress)
            );
        }

        if (offset == null) {
            throw new FeederHasNoLocationOffsetException(
                    String.format("Photon Feeder with address %s has no location offset.", hardwareId)
            );
        }
    }

    public void setOffset(Location offsets) {
        Object oldValue = this.offset;
        this.offset = offsets;
        pickCorrectionOffset = new Location(LengthUnit.Millimeters);
        firePropertyChange("offsets", oldValue, offsets);
    }

    public Location getOffset() {
        return offset;
    }

    public boolean getMoveWhileFeeding() {
        return moveWhileFeeding;
    }

    public void setMoveWhileFeeding(boolean moveWhileFeeding) {
        this.moveWhileFeeding = moveWhileFeeding;
    }

    @Override
    public void findIssues(Solutions solutions) {
        super.findIssues(solutions);

        if (hardwareId == null) {
            return;
        }

        if (slotAddress != null && getSlot().getLocation() == null) {
            solutions.add(new Solutions.PlainIssue(
                    this,
                    "Feeder slot has no configured location",
                    "Select the feeder in the Feeders tab and make sure the slot has a set location",
                    Solutions.Severity.Error,
                    "https://github.com/openpnp/openpnp/wiki/Photon-Feeder#slots-and-feeder-locations"
            ));
        }

        if (offset == null) {
            solutions.add(new Solutions.PlainIssue(
                    this,
                    "Feeder has no configured offset",
                    "Select the feeder in the Feeders tab and make sure the feeder has an offset location from the slot",
                    Solutions.Severity.Error,
                    "https://github.com/openpnp/openpnp/wiki/Photon-Feeder#slots-and-feeder-locations"
            ));
        }
    }

    @Override
    public void prepareForJob(boolean visit) throws Exception {
        for (int i = 0; i <= photonProperties.getFeederCommunicationMaxRetry(); i++) {
            findSlotAddressIfNeeded();

            initializeIfNeeded();

            if (initialized) {
                verifyFeederLocationIsFullyConfigured();

                super.prepareForJob(visit);
                return;
            }
        }

        throw new Exception("Failed to find and initialize the feeder");
    }

    public void findSlotAddress() throws Exception {
        findSlotAddress(true);
    }

    private void findSlotAddress(boolean force) throws Exception {
        if (slotAddress != null && !force) {
            return;
        }

        GetFeederAddress getFeederAddress = new GetFeederAddress(hardwareId);
        GetFeederAddress.Response response = getFeederAddress.send(photonBus);

        if (response == null) {
            setSlotAddress(null);
            return;
        }
        setSlotAddress(response.fromAddress);
    }

    private void findSlotAddressIfNeeded() throws Exception {
        findSlotAddress(false);
    }

    public void initializeIfNeeded() throws Exception {
        if (initialized || slotAddress == null) {
            return;
        }

        InitializeFeeder initializeFeeder = new InitializeFeeder(slotAddress, hardwareId);
        InitializeFeeder.Response response = initializeFeeder.send(photonBus);

        if (response == null) {
            slotAddress = null;
        } else if (response.error == ErrorTypes.WRONG_FEEDER_UUID) {
            PhotonFeeder otherFeeder = findByHardwareId(response.uuid);

            // If we don't know about that feeder, let's go ahead and create it
            if (otherFeeder == null) {
                otherFeeder = new PhotonFeeder();
                otherFeeder.setHardwareId(response.uuid);
                Configuration.get().getMachine().addFeeder(otherFeeder);
            }

            // This other feeder is in the slot we thought we were
            otherFeeder.setSlotAddress(response.fromAddress);
        } else {
            initialized = true;
        }
    }

    static Actuator getDataActuator() {
        Machine machine = Configuration.get().getMachine();

        Actuator actuator = machine.getActuatorByName(ACTUATOR_DATA_NAME);

        if (actuator == null) {
            actuator = createDefaultActuator(machine);
        }

        return actuator;
    }

    private static Actuator createDefaultActuator(Machine machine) {
        Actuator actuator;
        actuator = new ReferenceActuator();
        actuator.setName(ACTUATOR_DATA_NAME);

        for (Driver driver : machine.getDrivers()) {
            if(! (driver instanceof GcodeDriver)) {
                continue;
            }
            GcodeDriver gcodeDriver = (GcodeDriver) driver;
            gcodeDriver.setCommand(actuator, GcodeDriver.CommandType.ACTUATOR_READ_COMMAND, "M485 {Value}");
            gcodeDriver.setCommand(actuator, GcodeDriver.CommandType.ACTUATOR_READ_REGEX, "rs485-reply: (?<Value>.*)");
            break;  // Only set this on 1 GCodeDriver
        }

        try {
            machine.addActuator(actuator);
        } catch (Exception exception) {
            exception.printStackTrace(); // TODO Probably need to log this, figure out why it can happen first
        }
        return actuator;
    }

    private void feed(Nozzle nozzle, int distance_HundredMicrons) throws Exception {
        for (int i = 0; i <= photonProperties.getFeederCommunicationMaxRetry(); i++) {
            findSlotAddressIfNeeded();
            initializeIfNeeded();

            if (!initialized) {
                continue;
            }

            verifyFeederLocationIsFullyConfigured();

            MoveFeedForward moveFeedForward = new MoveFeedForward(slotAddress, distance_HundredMicrons);
            MoveFeedForward.Response moveFeedForwardResponse = moveFeedForward.send(photonBus);

            if (moveFeedForwardResponse == null) {
                slotAddress = null;
                initialized = false;
                throw new FeedFailureException("Feed command timed out");
            } else if (moveFeedForwardResponse.error == ErrorTypes.UNINITIALIZED_FEEDER) {
                slotAddress = null;
                initialized = false;
                continue;  // We'll initialize it on a retry
            }

            // The feeder gives us expectedTimeToFeed, but it is way too conservative.
            // Use expectedTimeToFeed to bound how long we will wait,
            // but use polling to check the status of the feed.
            Duration expectedFeedDuration = Duration.ofMillis(moveFeedForwardResponse.expectedTimeToFeed);
            long endTimeNanos = System.nanoTime() + expectedFeedDuration.toNanos() * 3;
            for (int j = 0; j <= photonProperties.getFeederCommunicationMaxRetry() || System.nanoTime() <= endTimeNanos; j++) {
                Thread.sleep(50); // MAGIC: this feels like a good number, there is no particular reason it is this way.

                if (j == 0 && nozzle != null && Configuration.get().getMachine().isHomed() && getMoveWhileFeeding()) {
                    MovableUtils.moveToLocationAtSafeZ(nozzle, getPickLocation().derive(null, null, Double.NaN, null));
                }

                MoveFeedStatus moveFeedStatus = new MoveFeedStatus(slotAddress);
                MoveFeedStatus.Response moveFeedStatusResponse = moveFeedStatus.send(photonBus);

                if (moveFeedStatusResponse == null) {
                    continue; // Timeout. retry after delay.
                }

                if (moveFeedStatusResponse.error == ErrorTypes.NONE) {
                    return;
                } else if (moveFeedStatusResponse.error == ErrorTypes.COULD_NOT_REACH) {
                    throw new FeedFailureException("Feeder could not reach its destination.");
                }
            }

            throw new FeedFailureException("Feeder timed out when we requested a feed status update.");
        }

        throw new FeedFailureException("Failed to feed for an unknown reason. Is the feeder inserted?");
    }

    @Override
    public void feed(Nozzle nozzle) throws Exception {
        switch (getFeedOptions()) {
        case Normal:
            break;
        case SkipNext:
            setFeedOptions(FeedOptions.Normal);
            return;
        case Disable:
            return;
        }

        visionsSinceLastFeed = 0;

        // To solve long term drift. Nudge the part pitch sent to the feeder if the correction offset is big enough.
        Location nudgeOffset = new Location(LengthUnit.Millimeters);
        int partPitchNudgeTicks = 0;
        final double feedTickMm = 0.1;

        double yPlaneErrorMm = pickCorrectionOffset.getLengthY().convertToUnits(LengthUnit.Millimeters).getValue();

        if (yPlaneErrorMm <= -feedTickMm || feedTickMm <= yPlaneErrorMm) {
            // far enough to nudge the part pitch
            partPitchNudgeTicks = -(int)(yPlaneErrorMm / feedTickMm);
            nudgeOffset = new Location(LengthUnit.Millimeters, 0, (double)partPitchNudgeTicks * feedTickMm, 0, 0);
            if (getSlotAddress() > 25) {
                // back row of feeders; tape flows oposite direction as our commands so invert the nudge
                partPitchNudgeTicks = -partPitchNudgeTicks;
            }
            Logger.debug("{}: Nudging tape by {} ticks", getSlotAddress(), partPitchNudgeTicks);
        }

        pickCorrectionOffset = pickCorrectionOffset.add(nudgeOffset);

        try {
            feed(nozzle, getPartPitch() * 10 + partPitchNudgeTicks);
        } catch (Exception e) {
            // Didn't feed, revert correction offset.
            pickCorrectionOffset = pickCorrectionOffset.subtract(nudgeOffset);
            throw e;
        }
    }

    public void feedOneMm() throws Exception {
        feed(null, 10);
        pickCorrectionOffset = new Location(LengthUnit.Millimeters);
    }

    @Override
    public String getPropertySheetHolderTitle() {
        String classSimpleName = getClass().getSimpleName();
        if (hardwareId == null) {
            return String.format("Unconfigured %s", classSimpleName);
        } else {
            return String.format("%s %s", classSimpleName, getName());
        }
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        List<PropertySheet> sheets = new ArrayList<>();

        if (hardwareId != null) {
            sheets.add(new FeederPropertySheet(this));
        }

        sheets.add(new GlobalConfigPropertySheet());

        return sheets.toArray(new PropertySheet[0]);
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
        if (hardwareId == null) {
            return String.format("Unconfigured %s", getClass().getSimpleName());
        }

        StringBuilder result = new StringBuilder();
        result.append(name);
        result.append(" (Slot: ");

        if (slotAddress == null) {
            result.append("None");
        } else {
            result.append(slotAddress);
        }

        result.append(")");

        return result.toString();
    }

    @Override
    public void setName(String name) {
        Matcher matcher = Pattern.compile("(\\(Slot: [\\w+]+\\))").matcher(name);
        while (matcher.find()) {
            name = name.replace(matcher.group(), "");
        }

        name = name.trim();

        super.setName(name);
    }

    /**
     * The PhotonFeeder assumes you have a physical slot that is numbered 1 - 254. That
     * value is also used in the protocol as the address of the feeder once the feeder
     * is initialized.
     *
     * @return The slot address of this feeder or null if it doesn't have one.
     */
    public Integer getSlotAddress() {
        return slotAddress;
    }

    public PhotonFeederSlots.Slot getSlot() {
        if (slotAddress == null) {
            return null;
        }

        return photonProperties.getFeederSlots().getSlot(slotAddress);
    }

    public void setSlotAddress(Integer slotAddress) {
        PhotonFeederSlots.Slot oldSlot = getSlot();
        Integer oldValue = this.slotAddress;
        String oldName = this.getName();

        if (slotAddress != null) {
            // Find any other photon feeders and if they have this slot address, set their address to null
            PhotonFeeder otherFeeder = findBySlotAddress(slotAddress);
            if (otherFeeder != null) {
                otherFeeder.slotAddress = null;
                otherFeeder.initialized = false;
            }
        }

        this.slotAddress = slotAddress;

        pickCorrectionOffset = new Location(LengthUnit.Millimeters);

        firePropertyChange("slotAddress", oldValue, slotAddress);
        firePropertyChange("slot", oldSlot, getSlot());
        firePropertyChange("name", oldName, getName());
    }

    public String getHardwareId() {
        return hardwareId;
    }

    public void setHardwareId(String hardwareId) {
        String oldValue = this.hardwareId;
        this.hardwareId = hardwareId;

        if (getClass().getSimpleName().equals(name)) {
            name = hardwareId;
        }

        pickCorrectionOffset = new Location(LengthUnit.Millimeters);

        firePropertyChange("hardwareId", oldValue, hardwareId);
    }

    @Override
    public boolean isEnabled() {
        return super.isEnabled() &&
                hardwareId != null &&
                partId != null &&
                slotAddress != null &&
                offset != null &&
                getSlot().getLocation() != null;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setPartPitch(int partPitch) {
        this.partPitch = partPitch;
        pickCorrectionOffset = new Location(LengthUnit.Millimeters);
    }

    public int getPartPitch() {
        return partPitch;
    }

    public static PhotonFeeder findByHardwareId(String hardwareId) {
        for (Feeder feeder : Configuration.get().getMachine().getFeeders()) {
            if (!(feeder instanceof PhotonFeeder)) {
                continue;
            }

            PhotonFeeder photonFeeder = (PhotonFeeder) feeder;

            // Are we explicitly asking for feeders with null hardware ID?
            if (photonFeeder.hardwareId == null && hardwareId == null) {
                return photonFeeder;
            }

            if (photonFeeder.hardwareId != null && photonFeeder.hardwareId.equals(hardwareId)) {
                return photonFeeder;
            }
        }

        return null;
    }

    public static PhotonFeeder findBySlotAddress(int slotAddress) {
        for (Feeder feeder : Configuration.get().getMachine().getFeeders()) {
            if (!(feeder instanceof PhotonFeeder)) {
                continue;
            }

            PhotonFeeder photonFeeder = (PhotonFeeder) feeder;

            if (photonFeeder.slotAddress != null && photonFeeder.slotAddress.equals(slotAddress)) {
                return photonFeeder;
            }
        }

        return null;
    }

    public enum FeederSearchState {
        UNKNOWN,
        SEARCHING,
        FOUND,
        MISSING
    }

    public interface FeederSearchProgressConsumer {
        void accept(int feederAddress, FeederSearchState feederSearchState);
    }

    public static void findAllFeeders(FeederSearchProgressConsumer progressUpdate) throws Exception {
        Logger.info("Searching for Photon Feeders");
        Machine machine = Configuration.get().getMachine();
        PhotonProperties photonProperties = new PhotonProperties(machine);
        int maxFeederAddress = photonProperties.getMaxFeederAddress();
        Logger.debug("Max Photon feeder address: " + maxFeederAddress);

        List<PhotonFeeder> feedersToAdd = new ArrayList<>();

        for (int address = 1; address <= maxFeederAddress; address++) {
            Logger.debug("Querying Photon feeder address: " + address);

            if (progressUpdate != null) {
                progressUpdate.accept(address, FeederSearchState.SEARCHING);
            }

            GetFeederId getFeederId = new GetFeederId(address);
            GetFeederId.Response response = getFeederId.send(photonBus);

            if (progressUpdate != null) {
                progressUpdate.accept(address, response == null ? FeederSearchState.MISSING : FeederSearchState.FOUND);
            }

            if (response == null) {
                PhotonFeeder otherFeeder = findBySlotAddress(address);
                if (otherFeeder != null) {
                    otherFeeder.slotAddress = null;
                    otherFeeder.initialized = false;
                }
            } else {
                PhotonFeeder otherFeeder = findByHardwareId(response.uuid);
                if (otherFeeder == null) {
                    // Try to find an existing feeder without a hardware id before making a new one
                    otherFeeder = findByHardwareId(null);
                    if (otherFeeder == null) {
                        otherFeeder = new PhotonFeeder();
                        feedersToAdd.add(otherFeeder);
                    }
                }

                otherFeeder.setHardwareId(response.uuid);
                otherFeeder.setSlotAddress(address);

                Logger.trace("Found feeder with hardware uuid " + otherFeeder.getHardwareId() + " at address " + otherFeeder.getSlotAddress());
            }
        }

        for (PhotonFeeder feeder : feedersToAdd) {
            Configuration.get().getMachine().addFeeder(feeder);
        }
    }

    @Override
    public boolean canTakeBackPart() {
        return getFeedOptions() == FeedOptions.Normal;
    }

    @Override
    public void takeBackPart(Nozzle nozzle) throws Exception {
        super.takeBackPart(nozzle);
        putPartBack(nozzle);
        setFeedOptions(FeedOptions.SkipNext);
    }

    @Override
    public boolean supportsFeedOptions() {
        return true;
    }

    @Override
    public void bottomVisionResultCallback(Location offset) {
        Location pickLocation;
        try {
            pickLocation = getPickLocationWithoutError();
        } catch (Exception e) {
            Logger.error("{}: Could not get pick location without error for bottom vision callback: {}", getSlotAddress(), e);
            return;
        }

        // Add half of the error to the running total to avoid over-correction
        Location before = pickCorrectionOffset;

        double factor = 1 / (double)(2 << visionsSinceLastFeed); // Start at 0.5 then have each new vision half.
        visionsSinceLastFeed++;

        pickCorrectionOffset = pickCorrectionOffset.add(offset.rotateXy(pickLocation.getRotation()).multiply(factor));
        Logger.debug("{}: bottom vision reports pick error of: {}; old pick correction: {} new {}", getSlotAddress(), offset, before, pickCorrectionOffset);

        double distance = pickCorrectionOffset.convertToUnits(LengthUnit.Millimeters).getLinearDistanceTo(0, 0);
        if (distance > correctionLimit) {
            // Avoid physical damage, saturate it back to limit.
            pickCorrectionOffset = pickCorrectionOffset.multiply(correctionLimit / distance);
        }
    }
}
