package org.openpnp.machine.reference.feeder;

import java.util.List;

import javax.swing.Action;

import org.apache.commons.io.IOUtils;
import org.opencv.core.RotatedRect;
import org.openpnp.ConfigurationListener;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceFeeder;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.machine.reference.ReferenceNozzleTip;
import org.openpnp.machine.reference.feeder.wizards.ReferenceHeapFeederConfigurationWizard;
import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.Configuration;
import org.openpnp.model.Identifiable;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Named;
import org.openpnp.model.Part;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Movable;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.util.IdentifiableList;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Implementation of the HeapFeeder.
 * A feeder that uses "heaps" of loose parts (one type) and vision to detect correct orientation (
 * side up/down, maybe rotation) after throwing a few parts in a "DropBox" for sorting. DropBoxes are 
 * shared between Heaps, but there can be multiple DropBoxes (for best background color, reduce travel ...)
 * 
 * Vision logic of the AdvancedLoosePartFeeder for detection of correctly laying parts.
 *
 */
public class ReferenceHeapFeeder extends ReferenceFeeder {
    private final static Logger logger = LoggerFactory.getLogger(ReferenceHeapFeeder.class);

    // some settings that I might expose later
    final static int maxThrowRetries = 12;

    /**
     * Associated DropBox.
     * Here are the parts are dropped for vision before picking them.
     * Also used to turn parts by dropping them again and again.
     */
    private DropBox dropBox;
    @Attribute(required = false)
    private String dropBoxId;   // for saving

    /**
     * depth of the DropBox. 
     * Maximum addition to the coordinates from the "top center"
     */
    @Attribute(required = false)
    private double boxDepth = -25.0f;

    /**
     * To save time, store the depth of the last pick, so we did not need to start at the top every time.
     */
    @Attribute(required = false)
    private double lastFeedDepth = -5;

    /**
     * Three locations used to escape a "maze" of heaps.
     * Mowing from a heap: safeZ, move to Location 1, Location 2, Location 3
     * Mowing to a heap the other direction.
     * Should be choosen, that a part is not moved other heps, so if a part is lost, it is not mixed with other parts.
     */
    @Element(required = false)
    private Location way1 = new Location(LengthUnit.Millimeters);

    @Element(required = false)
    private Location way2 = new Location(LengthUnit.Millimeters);

    @Element(required = false)
    private Location way3 = new Location(LengthUnit.Millimeters);

    /**
     * If so many attemps to flip a part fails, throw all remaining parts away.
     * Used to avoid endless retries if a part is not recognizable.
     */
    @Attribute(required = false)
    private int throwAwayDropBoxContentAfterFailedFeeds = 7;

    /**
     * Needed increase in the vacuum while stirring in a heap to define "part(s) on the nozzle".
     * partOn() not used, since the chance is high, that there are larger leakages.
     */
    @Attribute(required = false)
    private int requiredVacuumDifference = 300;

    /**
     * Pipeline to detect parts (correct laying).
     */
    @Element(required = false)
    private CvPipeline feederPipeline = createFeederPipeline();

    /**
     * Pipeline for getting a reference image.
     */
    @Element(required = false)
    private CvPipeline trainingPipeline = createTrainingPipeline();

    private Location pickLocation;

    /**
     * After loading the configuration, set the dropBox with the stored id.
     */
    @Commit
    public void commit() {
        Configuration.get().addListener(new ConfigurationListener() {
            @Override
            public void configurationComplete(Configuration configuration) throws Exception {
                setDropBox(getDropBoxes().get(dropBoxId));
            }

            @Override
            public void configurationLoaded(Configuration configuration) throws Exception {
                // do nothing
            }
        });
    }

    /**
     * on save set/update the DropBoxId.
     */
    @Persist
    public void persist() {
        dropBoxId = getDropBox().getId();
    }


    /**
     * Get all defined DropBoxes.
     * @return List of DropBoxes
     */
    public static synchronized IdentifiableList<DropBox> getDropBoxes() {
        DropBoxProperty dropBoxProperty = (DropBoxProperty) Configuration.get().getMachine().getProperty("ReferenceHeapFeeder.dropBoxes");
        if (dropBoxProperty == null) {
            dropBoxProperty = new DropBoxProperty();
            DropBox dropBox = new DropBox();
            dropBox.setName("Green");
            dropBoxProperty.boxes.add(dropBox);
            Configuration.get().getMachine().setProperty("ReferenceHeapFeeder.dropBoxes", dropBoxProperty);
        }
        return dropBoxProperty.boxes;
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceHeapFeederConfigurationWizard(this);
    }

    /**
     * If a pick location exist, retur that.
     * Else the center of the DropBox.
     */
    @Override
    public Location getPickLocation() throws Exception {
        if (pickLocation != null) {
            return pickLocation;
        } else if (dropBox != null) {
            return new Location(dropBox.centerBottomLocation.getUnits(), dropBox.centerBottomLocation.getX(), dropBox.centerBottomLocation.getY(),
                    dropBox.centerBottomLocation.getZ() + part.getHeight().getValue(), dropBox.centerBottomLocation.getRotation());
        }
        return null;
    }

    /**
     * The core part of the HeapFeeder logic.
     * Cleans DropBox, get parts, tries to identify good one, else try to flip a part.
     */
    @Override
    public void feed(Nozzle nozzle) throws Exception {       
        // there might be foreign parts in the dropBox, clean up first.
        if (dropBox.getLastHeap() != this) {
            dropBox.clean(nozzle);
        } 
        // now claim the dropBox
        dropBox.setLastHeap(this);

        // just to be sure it's the right nozzle for the job
        if ( !getPart().getPackage().getCompatibleNozzleTips().contains(nozzle.getNozzleTip())) {
            nozzle.loadNozzleTip(getPart().getPackage().getCompatibleNozzleTips().toArray(new NozzleTip[0])[0]);
        }

        // no part found => no pick location
        pickLocation = null;

        // now try to get a good part
        for (int attempt = 0; attempt <= maxThrowRetries; attempt++) {
            pickLocation = getFeederPart(nozzle);
            if (pickLocation != null) {
                return; // found part
            }
            // no part found, try to flip a part by throwing it in the dropBox again
            if (!dropBox.tryToFlipSomePart(nozzle)) {
                // nothing there, get new parts
                fetchParts(nozzle);
            }
            // to many failed attempts, discard parts in the dropBox (maybe damaged/wrong parts)
            if (attempt > 0 && attempt % throwAwayDropBoxContentAfterFailedFeeds == 0) {
                // deny the parts are from this heap => trash
                dropBox.setLastHeap(null);
                dropBox.clean(nozzle);
            }
        }
    }
    
    /**
     * Get a few sample parts for reference image
     * @param nozzle used nozzle
     * @throws Exception something goes wrong
     */
    public void getSamples(Nozzle nozzle) throws Exception {       
        // clenaup
        dropBox.clean(nozzle);

        // now claim the dropBox
        dropBox.setLastHeap(this);
        
        // just to be sure it's the right nozzle for the job
        if ( !getPart().getPackage().getCompatibleNozzleTips().contains(nozzle.getNozzleTip())) {
            nozzle.loadNozzleTip(getPart().getPackage().getCompatibleNozzleTips().toArray(new NozzleTip[0])[0]);
        }
        
        // get  parts
        fetchParts(nozzle);
    }


    /**
     * Moves the nozzle to this Heap.
     * Uses the three "waypoints", sets "SpeedOverPrecision".
     * @param nozzle used nozzle
     * @throws Exception    Something went wrong
     */
    public void moveToHeap(Nozzle nozzle) throws Exception {
        nozzle.moveToSafeZ();
        nozzle.moveTo(way3.derive(null, null, Double.NaN, Double.NaN), Movable.MoveToOption.SpeedOverPrecision);
        nozzle.moveTo(way2.derive(null, null, Double.NaN, Double.NaN), Movable.MoveToOption.SpeedOverPrecision);
        nozzle.moveTo(way1.derive(null, null, Double.NaN, Double.NaN), Movable.MoveToOption.SpeedOverPrecision);
        nozzle.moveTo(location);
    }

    /**
     * Moves the nozzle away from this Heap.
     * Uses the three "waypoints", sets "SpeedOverPrecision".
     * @param nozzle used nozzle
     * @throws Exception    Something went wrong
     */
    public void moveFromHeap(Nozzle nozzle) throws Exception {
        nozzle.moveToSafeZ();
        nozzle.moveTo(way1.derive(null, null, Double.NaN, Double.NaN), Movable.MoveToOption.SpeedOverPrecision);
        nozzle.moveTo(way2.derive(null, null, Double.NaN, Double.NaN), Movable.MoveToOption.SpeedOverPrecision);
        nozzle.moveTo(way3.derive(null, null, Double.NaN, Double.NaN), Movable.MoveToOption.SpeedOverPrecision);
    }

    /**
     * Just a method to read the vacuum.
     * @param nozzle a Reference nozzle
     * @return  Value read.
     * @throws Exception Something went wrong.
     */
    double readVacuum(Nozzle nozzle) throws Exception {
        Actuator vacuumActuator = nozzle.getHead().getActuatorByName(((ReferenceNozzle)nozzle).getVacuumActuatorName());
        return Double.parseDouble(vacuumActuator.read());
    }

    /**
     * Checks if there is a stable vacuum difference to a base level.
     * @param nozzle used nozzle
     * @param baseLevel the base level the vacuum is compared to
     * @param requiredVacuumDifference as the name said
     * @return true if difference is stable and larger or equal the required value.
     * @throws Exception something went wrong
     */
    private boolean stableVacuumDifferenceReached(Nozzle nozzle, double baseLevel, int requiredVacuumDifference) throws Exception {
        // check if 25ms above the required difference to avoid false positives due to noise, return on first below
        for (int i = 0; i < 5; i++) {
            if (Math.abs(readVacuum(nozzle) - baseLevel) < requiredVacuumDifference) {
                return false;
            }
            Thread.sleep(5);
        }
        return true;
    }


    /**
     * Gets parts from the Heap and drops them in the associated DropBox
     * @param nozzle    The used nozzle
     * @throws Exception something unexpected happend.
     */
    private void fetchParts(Nozzle nozzle) throws Exception {
        // just to be sure it's the right nozzle for the job
        if ( !getPart().getPackage().getCompatibleNozzleTips().contains(nozzle.getNozzleTip())) {
            nozzle.loadNozzleTip(getPart().getPackage().getCompatibleNozzleTips().toArray(new NozzleTip[0])[0]);
        }
        // prepare
        nozzle.moveToSafeZ();
        nozzle.getHead().getActuatorByName(((ReferenceNozzle)nozzle).getVacuumActuatorName()).actuate(true);
        long vacuumOn = System.currentTimeMillis();
        // move to heap
        nozzle.moveTo(location.derive(null, null, Double.NaN, null), Movable.MoveToOption.SpeedOverPrecision);
        // if that didn't take long enough, wait
        ((ReferenceNozzle)nozzle).getPlaceDwellMilliseconds();
        vacuumOn = vacuumOn - (System.currentTimeMillis() + 
                Math.round(1.3 * (((ReferenceNozzle)nozzle).getPickDwellMilliseconds() + ((ReferenceNozzleTip)nozzle.getNozzleTip()).getPickDwellMilliseconds())));  // 1.3 times the pick time, just be sure it is really stable
        if (vacuumOn > 0 ) {
            Thread.sleep(vacuumOn);
        }
        // save current value as reference value
        double vacuumLevel = readVacuum(nozzle);
        // last pick location
        nozzle.moveTo(location.add(new Location(LengthUnit.Millimeters, 0, 0, lastFeedDepth, 0)), Movable.MoveToOption.SpeedOverPrecision);
        // while vacuum difference is not reached, slowly stir in the heap
        double currentDepth = lastFeedDepth + part.getHeight().getValue() / 2; // start always a bit higher than last time, to be sure that level is empty
        for (int i = 0; ! stableVacuumDifferenceReached(nozzle, vacuumLevel, requiredVacuumDifference) && currentDepth > (boxDepth + part.getHeight().getValue()); i++) {
            switch (i % 8) {
                case 0: {
                    moveToHeapCorner(nozzle, currentDepth, 0); 
                    break;
                }
                case 1: {
                    moveToHeapCorner(nozzle, currentDepth, 1); 
                    break;
                }
                case 2: {
                    moveToHeapCorner(nozzle, currentDepth, 3); 
                    break;
                }
                case 3: {
                    moveToHeapCorner(nozzle, currentDepth, 2); 
                    break;
                }
                case 4: {
                    moveToHeapCorner(nozzle, currentDepth, 0);
                    break;
                }
                case 5: {
                    moveToHeapCorner(nozzle, currentDepth, 3);
                    break;
                }
                case 6: {
                    moveToHeapCorner(nozzle, currentDepth, 1);
                    break;
                }
                case 7: {
                    moveToHeapCorner(nozzle, currentDepth, 2); 
                    break;
                }
            }
            if (i % 4 == 3) {
                currentDepth -= Math.max(0.1, (part.getHeight().getValue() / 2.0));
            }
        }
        // if at the bottom => failed
        if (currentDepth < (boxDepth + part.getHeight().getValue())) {
            throw new Exception("HeapFeeder " + getName() + ": Can not grab parts. Heap Empty or VacuumDifference wrong.");
        }

        // we have parts
        lastFeedDepth = currentDepth;
        nozzle.pick(getPart()); // so the nozzle knows what it is carring. introduces some additional delay
        // but rewrite of pick/place without calling nozzle doesn't seem worth, slow anyway
        nozzle.moveToSafeZ();
        moveFromHeap(nozzle); // safe way away from the other heaps
        dropBox.dropInto(nozzle); // drop the parts in the dropBox
    }

    /**
     * A small helper, that calculates the coordinates of the four corners for movement (0-3) 
     * and moves the nozzle there.
     * @param nozzle nozzle to move
     * @param currentDepth current depth relativ to the top of the Heap
     * @param corner destination corner
     * @throws Exception something went wrong.
     */
    private void moveToHeapCorner(Nozzle nozzle, double currentDepth, int corner) throws Exception {
        Location destination = location.add(new Location(LengthUnit.Millimeters, 0, 0, currentDepth, 0));
        switch (corner) {
            case 0: {
                destination = destination.add(new Location(LengthUnit.Millimeters, +1.00, -1.00, 0, 0));
                break;
            }
            case 1: {
                destination = destination.add(new Location(LengthUnit.Millimeters, +1.00, +1.00, 0, 0));
                break;
            }
            case 2: {
                destination = destination.add(new Location(LengthUnit.Millimeters, -1.00, +1.00, 0, 0));
                break;
            }
            case 3: {
                destination = destination.add(new Location(LengthUnit.Millimeters, -1.00, -1.00, 0, 0));
                break;
            }
        }
        nozzle.moveTo(destination, 0.5, Movable.MoveToOption.SpeedOverPrecision);
    }

    /**
     * Gets a feed location, if a part is in the DropBox upside up.
     * So make the location more precise, three tries are done where the camera is moved over the last
     * detected location.
     * @param nozzle used nozzle
     * @return location or null
     * @throws Exception something went wrong.
     */
    private Location getFeederPart(Nozzle nozzle) throws Exception {
        Location location = dropBox.centerBottomLocation.derive(null, null, Double.NaN, 0.0);

        // if there is a part, get a precise location
        for (int i = 0; i < 3 && location != null; i++) {
            location = locateFeederPart(nozzle);
            if (location != null) {
                // adjust Z
                location = location.derive(null, null, dropBox.centerBottomLocation.convertToUnits(location.getUnits()).getZ()
                        + part.getHeight().convertToUnits(location.getUnits()).getValue(), null);
            }
        }
        return location;
    }

    /**
     * Executes the vision pipeline to locate a part.
     * @param nozzle used nozzle
     * @return location or null
     * @throws Exception something went wrong
     */
    private Location locateFeederPart(Nozzle nozzle) throws Exception {
        Camera camera = nozzle.getHead().getDefaultCamera();
        MovableUtils.moveToLocationAtSafeZ(camera, dropBox.getCenterBottomLocation().derive(null, null, Double.NaN, 0d));
        try (CvPipeline pipeline = getFeederPipeline()) {
            // Process the pipeline to extract RotatedRect results
            pipeline.setProperty("camera", camera);
            pipeline.setProperty("nozzle", nozzle);
            pipeline.setProperty("feeder", this);
            pipeline.process();
            // Grab the results
            List<RotatedRect> results = (List<RotatedRect>) pipeline.getResult(VisionUtils.PIPELINE_RESULTS_NAME).model;
            if ((results == null) || results.isEmpty()) {
                //nothing found
                return null;
            }
            // Find the closest result
            results.sort((a, b) -> {
                Double da = VisionUtils.getPixelLocation(camera, a.center.x, a.center.y)
                        .getLinearDistanceTo(camera.getLocation());
                Double db = VisionUtils.getPixelLocation(camera, b.center.x, b.center.y)
                        .getLinearDistanceTo(camera.getLocation());
                return da.compareTo(db);
            });
            RotatedRect result = results.get(0);
            Location location = VisionUtils.getPixelLocation(camera, result.center.x, result.center.y);
            // Get the result's Location
            // Update the location with the result's rotation
            location = location.derive(null, null, null, -(result.angle + getLocation().getRotation()));
            // Update the location with the correct Z, which is the configured Location's Z
            // plus the part height.
            location =
                    location.derive(null, null,
                            this.location.convertToUnits(location.getUnits()).getZ()
                            + part.getHeight().convertToUnits(location.getUnits()).getValue(),
                            null);
            MainFrame.get().getCameraViews().getCameraView(camera)
            .showFilteredImage(OpenCvUtils.toBufferedImage(pipeline.getWorkingImage()), 250);
            return location;
        }
    }

    private CvPipeline createTrainingPipeline() {
        try {
            String xml = IOUtils.toString(ReferenceHeapFeeder.class
                    .getResource("HeapFeeder-Training-DefaultPipeline.xml"));
            return new CvPipeline(xml);
        }
        catch (Exception e) {
            throw new Error(e);
        }       
    }

    private CvPipeline createFeederPipeline() {
        try {
            String xml = IOUtils.toString(ReferenceHeapFeeder.class
                    .getResource("HeapFeeder-Part-DefaultPipeline.xml"));
            return new CvPipeline(xml);
        }
        catch (Exception e) {
            throw new Error(e);
        }       
    }

    public void resetFeederPipeline() {
        feederPipeline = createFeederPipeline();
    }

    public void resetTrainingPipeline() {
        trainingPipeline = createTrainingPipeline();
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName() + " " + getName();
    }
    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        return null;
    }
    @Override
    public Action[] getPropertySheetHolderActions() {
        return null;
    }
    public double getBoxDepth() {
        return boxDepth;
    }
    public void setBoxDepth(double boxDepth) {
        this.boxDepth = boxDepth;
    }
    public double getLastFeedDepth() {
        return lastFeedDepth;
    }
    public void setLastFeedDepth(double feedFeedDepth) {
        this.lastFeedDepth = feedFeedDepth;
    }
    public Location getWay1() {
        return way1;
    }
    public void setWay1(Location way) {
        this.way1 = way;
    }
    public Location getWay2() {
        return way2;
    }
    public void setWay2(Location way) {
        this.way2 = way;
    }
    public Location getWay3() {
        return way3;
    }
    public void setWay3(Location way) {
        this.way3 = way;
    }
    public int getThrowAwayDropBoxContentAfterFailedFeeds() {
        return throwAwayDropBoxContentAfterFailedFeeds;
    }
    public void setThrowAwayDropBoxContentAfterFailedFeeds(
            int throwAwayDropBoxContentAfterFailedFeeds) {
        this.throwAwayDropBoxContentAfterFailedFeeds = throwAwayDropBoxContentAfterFailedFeeds;
    }
    public int getRequiredVacuumDifference() {
        return requiredVacuumDifference;
    }
    public void setRequiredVacuumDifference(int requiredVacuumDifference) {
        this.requiredVacuumDifference = requiredVacuumDifference;
    }
    public CvPipeline getFeederPipeline() {
        return feederPipeline;
    }
    public void setFeederPipeline(CvPipeline feederPipeline) {
        this.feederPipeline = feederPipeline;
    }
    public CvPipeline getTrainingPipeline() {
        return trainingPipeline;
    }
    public void setTrainingPipeline(CvPipeline trainingPipeline) {
        this.trainingPipeline = trainingPipeline;
    }
    public DropBox getDropBox() {
        if (dropBox == null) {
            dropBox = getDropBoxes().get(getDropBoxes().size() - 1);
        }
        return dropBox;
    }
    public void setDropBox(DropBox dropBox) throws Exception {
        if (dropBox == null) {
            throw new Exception("DropBox is required.");
        }
        this.dropBox = dropBox;
    }

    // Workaround that in the Wizzard te old feeder is cached otherwise
    public Location getDropBoxLocation() {
        return dropBox.getCenterBottomLocation();
    }
    // Workaround that in the Wizzard te old feeder is cached otherwise
    public void setDropBoxLocation(Location dropBox) throws Exception {
        if (dropBox == null) {
            throw new Exception("Location is required.");
        }
        this.dropBox.setCenterBottomLocation(dropBox);
    }
    
    public Location getDropBoxDropLocation() {
        return dropBox.getDropLocation();
    }
    // Workaround that in the Wizzard te old feeder is cached otherwise
    public void setDropBoxDropLocation(Location dropBox) throws Exception {
        if (dropBox == null) {
            throw new Exception("Location is required.");
        }
        this.dropBox.setDropLocation(dropBox);
    }

    

    @Root
    public static class DropBox extends AbstractModelObject implements Identifiable, Named {
        @Attribute(name = "id")
        final private String id;

        @Attribute
        private String name;

        /**
         * The pipeline to detect all parts in a DropBox, should detect everything, despite orientation, wrong part and so on.
         */
        @Element
        private CvPipeline partPipeline = createPartPipeline();

        /**
         * Center (xy) and bottom (z) of the DropBox
         */
        @Element
        private Location centerBottomLocation = new Location(LengthUnit.Millimeters);
        
        /**
         * Location where to drop parts
         */
        @Element(required = false)
        private Location dropLocation = new Location(LengthUnit.Millimeters);

        
        /**
         * "Fake" Part that is used to move unknown parts.
         * Used for partHeight, nozzleTip selection and so on.
         */
        private Part dummyPartForUnknown;
        @Attribute
        private String dummyPartIdForUnknown;


        private ReferenceHeapFeeder lastHeap = null;

        public DropBox() {
            this(Configuration.createId("DropBox-"));
        }

        /**
         * Creates a new DropBox.
         * @param id ID and name of the new DropBox
         */
        public DropBox(@Attribute(name = "id") String id) {
            if (id == null) {
                throw new Error("Id is required.");
            }
            this.id = id;
            this.name = id;
        }

        /**
         * Get the object reference for the dummy part after configuration load
         */
        @Commit
        public void commit() {
            Configuration.get().addListener(new ConfigurationListener() {
                @Override
                public void configurationComplete(Configuration configuration) throws Exception {
                    setDummyPartForUnknown(Configuration.get().getPart(dummyPartIdForUnknown));
                    lastHeap = null;
                }

                @Override
                public void configurationLoaded(Configuration configuration) throws Exception {
                    // do nothing
                }
            });
        }

        /**
         * sets/updates thedummy partId before configuration save
         */
        @Persist
        public void persist() {
            if (dummyPartForUnknown == null) {
                dummyPartIdForUnknown = "HeapFedder-Dummy";
            } else {
                dummyPartIdForUnknown = dummyPartForUnknown.getId();
            }
        }

        /**
         * Drops the parts on the nozzle into this DropBox.
         * @param nozzle used nozzle
         * @throws Exception something went wrong.
         */
        public void dropInto(Nozzle nozzle) throws Exception {
            dropPart(nozzle, dropLocation);
        }

        /**
         * tries to flip a part in the DropBox
         * @param nozzle used nozze
         * @return true if a part has been detected and dropped again, otherwise false
         * @throws Exception something went wrong 
         */
        public boolean tryToFlipSomePart(Nozzle nozzle) throws Exception {
            // is there a part
            Location partLocation = getPartPickLocation(nozzle);
            if (partLocation == null || lastHeap == null) {
                return false; // is empty
            } else {
                // pick part, move up, drop ist
                pickPart(nozzle, partLocation, lastHeap.getPart());
                dropInto(nozzle);
                return true;
            }
        }

        /**
         * Clean up a DropBox.
         * Moves parts to the last Heap, if that i unknown, move to the trash.
         * @param nozzle used nozzle.
         * @throws Exception something wrong.
         */
        public void clean(Nozzle nozzle) throws Exception {
            int maxAttempts = 30;

            for (int i = 0; i < maxAttempts; i++) {
                // is there a part
                Location partLocation = getPartPickLocation(nozzle);
                if (partLocation == null) {
                    lastHeap = null;
                    return; // is empty
                } else {
                    removePart(nozzle, partLocation);
                }
            }
            throw new Exception("DropBox " + getName() + ": Even after " + maxAttempts + " attempts the DropBox is not detected as empty. Check Pipeline.");
        }

        /**
         * Moves a single part, either to the heap where it belongs or the trash if unknown
         * @param nozzle used nozze
         * @param partLocation pick location
         * @throws Exception something went wrong
         */
        private void removePart(Nozzle nozzle, Location partLocation) throws Exception {
            // basically two cases, back to feeder or to the trash
            if (lastHeap == null) { // unknown parts => trash
                // check nozzle tip
                if ( !dummyPartForUnknown.getPackage().getCompatibleNozzleTips().contains(nozzle.getNozzleTip())) {
                    nozzle.loadNozzleTip(dummyPartForUnknown.getPackage().getCompatibleNozzleTips().toArray(new NozzleTip[0])[0]);
                }
                pickPart(nozzle, partLocation, dummyPartForUnknown);
                dropPart(nozzle, Configuration.get().getMachine().getDiscardLocation());
            } else {    // known origin, not wasting parts
                if ( !lastHeap.getPart().getPackage().getCompatibleNozzleTips().contains(nozzle.getNozzleTip())) {
                    nozzle.loadNozzleTip(lastHeap.getPart().getPackage().getCompatibleNozzleTips().toArray(new NozzleTip[0])[0]);
                }
                pickPart(nozzle, partLocation, lastHeap.getPart());
                nozzle.moveToSafeZ();
                lastHeap.moveToHeap(nozzle);
                dropPart(nozzle, lastHeap.getLocation());
            }

        }

        /**
         * Picks a part up
         * @param nozzle used nozzle
         * @param location pick location
         * @param part what part
         * @throws Exception something went wrong.
         */
        private void pickPart(Nozzle nozzle, Location location, Part part) throws Exception {
            // Move to pick location.
            MovableUtils.moveToLocationAtSafeZ(nozzle, location);
            // Pick
            nozzle.pick(part);
            // Retract
            nozzle.moveToSafeZ();
        }

        /**
         * Drop(s) the part(s) on the nozzle
         * @param nozzle used nozzle
         * @param location destination
         * @throws Exception something went wrong
         */
        public void dropPart(Nozzle nozzle, Location location) throws Exception {
            // move to the  location
            if (nozzle.getLocation().getLinearDistanceTo(location) > 0.0001) {
                nozzle.moveToSafeZ();
            }
            nozzle.moveTo(location);
            // discard the part
            nozzle.place();
            // blow off the part
            Actuator blowOffValve = nozzle.getHead().getActuatorByName(((ReferenceNozzle) nozzle).getBlowOffActuatorName()); 
            if (blowOffValve != null) {
                blowOffValve.actuate(true);
            }
            Thread.sleep(Math.round(1.1 * (((ReferenceNozzle)nozzle).getPlaceDwellMilliseconds() + ((ReferenceNozzleTip)nozzle.getNozzleTip()).getPlaceDwellMilliseconds())));
            // move the nozzle a bit to help parts fall down
            if (blowOffValve != null) {
                blowOffValve.actuate(false);
            }
            if (!nozzle.isPartOff()) {
                throw new Exception("DropBox " + getName() + ": Dropping part failed, check nozzle tip");
            }
        }


        /**
         * Detects the location of a part in the dropBox.
         * @param nozzle used nozzle
         * @return the location of a detected part or null if no part found
         * @throws Exception something went wrong.
         */
        private Location getPartPickLocation(Nozzle nozzle) throws Exception {
            Camera camera = nozzle.getHead()
                    .getDefaultCamera();
            // Move to the feeder pick location
            MovableUtils.moveToLocationAtSafeZ(camera, centerBottomLocation.derive(null, null, Double.NaN, 0d));
            Location partLocation;
            try (CvPipeline pipeline = getPartPipeline()) {
                partLocation = getNearestPart(pipeline, camera, nozzle);
                if (partLocation != null) {
                    camera.moveTo(partLocation.derive(null, null, null, 0.0));
                    partLocation = getNearestPart(pipeline, camera, nozzle);
                    if (partLocation != null) {
                        camera.moveTo(partLocation.derive(null, null, null, 0.0));
                        double partHeight = dummyPartForUnknown.getHeight().getValue();
                        if (lastHeap != null) {
                            partHeight = lastHeap.getPart().getHeight().getValue();
                        } 
                        partLocation = partLocation.derive(null, null, centerBottomLocation.getZ() + partHeight, null);
                    } else {
                        throw new Exception("DropBox " + getName() + ": Part is not detected again, check Pipeline");
                    }
                }
                MainFrame.get()
                .getCameraViews()
                .getCameraView(camera)
                .showFilteredImage(OpenCvUtils.toBufferedImage(pipeline.getWorkingImage()),
                        1000);
            }
            return partLocation;
        }

        /**
         * Returns the nearest part from the current center.
         * @param pipeline used pipeline
         * @param camera used camera
         * @param nozzle used nozzle
         * @return location if found, otherwise null
         */
        private Location getNearestPart(CvPipeline pipeline, Camera camera, Nozzle nozzle) {
            // Process the pipeline to extract RotatedRect results
            pipeline.setProperty("camera", camera);
            pipeline.setProperty("nozzle", nozzle);
            pipeline.setProperty("feeder", this);
            pipeline.process();
            // Grab the results
            List<RotatedRect> results =
                    (List<RotatedRect>) pipeline.getResult(VisionUtils.PIPELINE_RESULTS_NAME).model;
            if (results == null || results.isEmpty()) {
                return null;
            }
            // Find the closest result
            results.sort((a, b) -> {
                Double da = VisionUtils.getPixelLocation(camera, a.center.x, a.center.y)
                        .getLinearDistanceTo(camera.getLocation());
                Double db = VisionUtils.getPixelLocation(camera, b.center.x, b.center.y)
                        .getLinearDistanceTo(camera.getLocation());
                return da.compareTo(db);
            });
            RotatedRect result = results.get(0);
            // Get the result's Location
            Location location = VisionUtils.getPixelLocation(camera, result.center.x, result.center.y);
            // Update the location's rotation with the result's angle
            location = location.derive(null, null, null, result.angle + this.centerBottomLocation.getRotation());
            return location;
        }

        @Override
        public String toString() {
            return name;
        }

        public void resetPartPipeline() {
            partPipeline = createPartPipeline();
        }

        private CvPipeline createPartPipeline() {
            try {
                String xml = IOUtils.toString(ReferenceHeapFeeder.class
                        .getResource("HeapFeeder-DropBox-DefaultPipeline.xml"));
                return new CvPipeline(xml);
            }
            catch (Exception e) {
                throw new Error(e);
            }       
        }

        public CvPipeline getPartPipeline() {
            return partPipeline;
        }

        public void setPartPipeline(CvPipeline partPipeline) {
            this.partPipeline = partPipeline;
        }

        public Location getCenterBottomLocation() {
            return centerBottomLocation;
        }

        public void setCenterBottomLocation(Location centerBottomLocation) {
            this.centerBottomLocation = centerBottomLocation;
        }


        public Part getDummyPartForUnknown() {
            return dummyPartForUnknown;
        }

        public void setDummyPartForUnknown(Part dummyPartForUnknown) {
            this.dummyPartForUnknown = dummyPartForUnknown;
        }
        
        public Location getDropLocation() {
            return dropLocation;
        }

        public void setDropLocation(Location dropLocation) {
            this.dropLocation = dropLocation;
        }

        public ReferenceHeapFeeder getLastHeap() {
            return lastHeap;
        }

        public void setLastHeap(ReferenceHeapFeeder lastHeap) {
            this.lastHeap = lastHeap;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            Object oldValue = this.name;
            this.name = name;
            firePropertyChange("name", oldValue, name);
        }
    }

    /**
     * This class is just a delegate wrapper around a list. 
     */
    @Root
    public static class DropBoxProperty {
        @ElementList
        IdentifiableList<DropBox> boxes = new IdentifiableList<>();
    }
}
