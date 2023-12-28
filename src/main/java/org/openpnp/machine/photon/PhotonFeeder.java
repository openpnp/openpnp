package org.openpnp.machine.photon;

import org.apache.commons.io.IOUtils;
import org.openpnp.ConfigurationListener;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.photon.exceptions.FeedFailureException;
import org.openpnp.machine.photon.exceptions.FeederHasNoLocationOffsetException;
import org.openpnp.machine.photon.exceptions.NoSlotAddressException;
import org.openpnp.machine.photon.exceptions.UnconfiguredSlotException;
import org.openpnp.machine.photon.protocol.ErrorTypes;
import org.openpnp.machine.photon.protocol.PhotonBus;
import org.openpnp.machine.photon.protocol.PhotonBusInterface;
import org.openpnp.machine.photon.protocol.commands.*;
import org.openpnp.machine.photon.sheets.FeederPropertySheet;
import org.openpnp.machine.photon.sheets.GlobalConfigPropertySheet;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.ReferenceFeeder;
import org.openpnp.machine.reference.driver.GcodeDriver;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Solutions;
import org.openpnp.spi.*;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.util.Utils2D;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhotonFeeder extends ReferenceFeeder {
    public static final String ACTUATOR_DATA_NAME = "PhotonFeederData";
    PhotonProperties photonProperties;

    @Attribute(required = false)
    protected String hardwareId;

    protected Integer slotAddress = null;

    @Attribute(required = false)
    protected int partPitch = 4;

    @Element(required = false)
    private Length tapeWidth = new Length(8, LengthUnit.Millimeters);

    protected boolean initialized = false;

    @Attribute(required = false)
    private boolean visionEnabled = false;

    @Element(required = false)
    private CvPipeline pipeline = createDefaultPipeline();

    private Location visionOffset = new Location(LengthUnit.Millimeters);

    private Length holeDiameter = new Length(1.5, LengthUnit.Millimeters);

    private Length holePitch = new Length(4, LengthUnit.Millimeters);

    private Length referenceHoleToPartLinear = new Length(2, LengthUnit.Millimeters);

    @Element(required = false)
    private Location offset;

    private static PhotonBusInterface photonBus;

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

    public Length getHoleDiameter() {
        return holeDiameter;
    }

    public void setHoleDiameter(Length holeDiameter) {
        this.holeDiameter = holeDiameter;
    }

    public Length getHolePitch() {
        return holePitch;
    }

    public void setHolePitch(Length holePitch) {
        this.holePitch = holePitch;
    }

    public Length getTapeWidth() {
        return tapeWidth;
    }

    public void setTapeWidth(Length tapeWidth) {
        this.tapeWidth = tapeWidth;
    }

    public Length getHoleDiameterMin() {
        return getHoleDiameter().multiply(0.85);
    }

    public Length getHoleDiameterMax() {
        return getHoleDiameter().multiply(1.15);
    }

    public Length getHolePitchMin() {
        return getHolePitch().multiply(0.9);
    }

    public Length getHoleDistanceMin() {
        return getTapeWidth().multiply(0.25);
    }

    private Length getHoleToPartLateral() {
        Length tapeWidth = this.tapeWidth.convertToUnits(LengthUnit.Millimeters);
        return new Length(tapeWidth.getValue() / 2 - 0.5, LengthUnit.Millimeters);
    }

    public Length getHoleDistanceMax() {
        // 1.75mm = 1.5mm holes are 1mm from the edge of the tape (as per EIA-481)
        Length tapeEdgeToFeedHoleCenter = new Length(1.75, LengthUnit.Millimeters);
        // The distance from the centre of the component to the edge of the tape. Gives a bit of leeway for not
        // clicking exactly in the centre of the component, but is close enough to eliminate most false-positives.
        return tapeEdgeToFeedHoleCenter.add(getHoleToPartLateral());
    }

    public Length getHoleLineDistanceMax() {
        return new Length(0.5, LengthUnit.Millimeters);
    }

    @Override
    public Location getPickLocation() throws Exception {
        verifyFeederLocationIsFullyConfigured();
        Location pickLocation = offset.offsetWithRotationFrom(getSlot().getLocation());
        if (visionEnabled) {
            pickLocation = pickLocation.add(visionOffset);
        }
        return pickLocation;
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
        firePropertyChange("offsets", oldValue, offsets);
    }

    public Location getOffset() {
        return offset;
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

    @Override
    public void feed(Nozzle nozzle) throws Exception {
        int max_retry = photonProperties.getFeederCommunicationMaxRetry();
        int attempts = 0;

        // Send MoveForwardCommand using RS485.
        int timeToWaitMillis = 0;
        while (true) {
            if (attempts++ > max_retry) {
                throw new FeedFailureException("Failed to feed for an unknown reason. Is the feeder inserted?");
            }

            findSlotAddressIfNeeded();
            initializeIfNeeded();

            if (!initialized) {
                continue;
            }

            verifyFeederLocationIsFullyConfigured();

            MoveFeedForward moveFeedForward = new MoveFeedForward(slotAddress, partPitch * 10);
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

            timeToWaitMillis = moveFeedForwardResponse.expectedTimeToFeed;
            break;
        }

        // Wait for feedback from the feeder that the position has been reached.
        attempts = 0;
        while (true) {
            if (attempts++ >= 3) {
                throw new FeedFailureException("Feeder timed out when we requested a feed status update.");
            }

            //noinspection BusyWait
            Thread.sleep(timeToWaitMillis);

            MoveFeedStatus moveFeedStatus = new MoveFeedStatus(slotAddress);
            MoveFeedStatus.Response moveFeedStatusResponse = moveFeedStatus.send(photonBus);

            if (moveFeedStatusResponse == null) {
                continue; // Timeout. retry after delay.
            }

            if (moveFeedStatusResponse.error == ErrorTypes.NONE) {
                break;
            } else if (moveFeedStatusResponse.error == ErrorTypes.COULD_NOT_REACH) {
                throw new FeedFailureException("Feeder could not reach its destination.");
            }
        }

        // If Vision is disabled, then rely on the registered pick location, otherwise use vision to
        // detect the tape hole location offset and use that as a mean to compensate the variance in
        // positionning.
        updateVisionOffsets(nozzle);
    }

    private void updateVisionOffsets(Nozzle nozzle) throws Exception {
        if (!visionEnabled) {
            return;
        }
        if (partPitch < 4) {
            // Not handled yet.
            return;
        }

        // Use our last pick location as a best guess.
        Location pickLocation = getPickLocation();

        // go to where we expect to find the next reference hole
        Camera camera = nozzle.getHead().getDefaultCamera();
        //ensureFeederZ(camera);

        // Compute the orientation of the tape. This suppose that the slot-location is in front of
        // the picking location.
        Location tapeVector = Location.origin.subtract(offset);

        // Discard any miss alignment, and consider that the feeders are perfectly aligned as a
        // small miss-alignment would not cause much problems.
        if (tapeVector.getLengthX().getValue() > tapeVector.getLengthY().getValue()) {
            tapeVector = tapeVector.multiply(1, 0, 0, 1);
        } else {
            tapeVector = tapeVector.multiply(0, 1, 0, 1);
        }

        // Normalize the tapeVector.
        tapeVector = Location.origin.unitVectorTo(tapeVector);

        // Compute the hole location based on the tapeVector and pick location.
        Location lateralVector = tapeVector.rotateXy(90);
        Location expectedLocation =
            pickLocation.add(lateralVector.multiply(getHoleToPartLateral().getValue()));
        if (partPitch < 4) {
            throw new Exception("Part pitch smaller than 4 is not yet handled.");
        }
        // For tapes with a part pitch >= 4 there is always a reference
        // hole 2mm from a part so we just multiply by the part pitch
        // skipping over holes that are not reference holes.
        expectedLocation = expectedLocation.add(tapeVector.multiply(-2));

        // Move the camera above the expected location for the hole.
        MovableUtils.moveToLocationAtSafeZ(camera, expectedLocation);

        // and look for the hole
        Location actualLocation = findClosestHole(camera);
        if (actualLocation == null) {
            throw new Exception("Unable to locate reference hole. End of strip? Too close to the feeder window?");
        }

        // make sure it's not too far away. The feeder should only move by increments of 4
        // millimeters, and the camera is not supposed to scan beyond.
        Length distance = actualLocation.getLinearLengthTo(expectedLocation)
                .convertToUnits(LengthUnit.Millimeters);
        if (distance.getValue() > 2) {
            throw new Exception("Located hole is too far.");
        }

        // Record the position difference between the expected hole location and the actual hole
        // location. Any deviations would be used when locating the next hole, or when locating the
        // next part. The difference is added to the vision offset which was used previously to
        // compute the pick location.
        visionOffset = visionOffset.add(actualLocation.subtract(expectedLocation));
    }

    private Location findClosestHole(Camera camera) throws Exception {
        Integer pxMaxDistance = (int) VisionUtils.toPixels(getHolePitch(), camera);
        Integer pxMinDiameter = (int) VisionUtils.toPixels(getHoleDiameterMin(), camera);
        Integer pxMaxDiameter = (int) VisionUtils.toPixels(getHoleDiameterMax(), camera);

        try (CvPipeline pipeline = getPipeline()) {
            // Process the pipeline to clean up the image and detect the tape holes
            pipeline.setProperty("camera", camera);
            pipeline.setProperty("feeder", this);
            pipeline.setProperty("DetectCircularSymmetry.maxDistance", pxMaxDistance / 2);
            pipeline.setProperty("DetectCircularSymmetry.minDiameter", pxMinDiameter);
            pipeline.setProperty("DetectCircularSymmetry.maxDiameter", pxMaxDiameter);
            // Limit the search to a to the area where the new hole might have moved into, and limit
            // it to the distance between 2 holes, in order to avoid more than one hole in frame.
            pipeline.setProperty("DetectCircularSymmetry.searchHeight", pxMaxDistance);
            pipeline.setProperty("DetectCircularSymmetry.searchWidth", pxMinDiameter / 2);
            pipeline.process();

            if (MainFrame.get() != null) {
                try {
                    MainFrame.get().getCameraViews().getCameraView(camera)
                            .showFilteredImage(OpenCvUtils.toBufferedImage(pipeline.getWorkingImage()), 250);
                }
                catch (Exception e) {
                    // if we aren't running in the UI this will fail, and that's okay
                }
            }

            // Grab the results
            List<CvStage.Result.Circle> results = pipeline.getExpectedResult(VisionUtils.PIPELINE_RESULTS_NAME)
                    .getExpectedListModel(CvStage.Result.Circle.class,
                            new Exception("Feeder " + getName() + ": No tape holes found."));

            // Return the only hole in the search window.
            CvStage.Result.Circle closestResult = results.get(0);
            Location holeLocation = VisionUtils.getPixelLocation(camera, closestResult.x, closestResult.y);
            return holeLocation;
        }
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
    }

    public int getPartPitch() {
        return partPitch;
    }

    public void setVisionEnabled(boolean enable) {
        visionEnabled = enable;
        visionOffset = new Location(LengthUnit.Millimeters);
    }

    public boolean getVisionEnabled() {
        return visionEnabled;
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

    public CvPipeline getPipeline() {
        return pipeline;
    }

    public void resetPipeline() {
        pipeline = createDefaultPipeline();
    }

    private static CvPipeline createDefaultPipeline() {
        try {
            String xml = IOUtils.toString(PhotonFeeder.class
                    .getResource("PhotonFeeder-DefaultPipeline.xml"));
            return new CvPipeline(xml);
        }
        catch (Exception e) {
            throw new Error(e);
        }
    }
}
