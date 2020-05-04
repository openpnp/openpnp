package org.openpnp.machine.reference.feeder;

import java.util.List;

import javax.swing.Action;

import org.apache.commons.io.IOUtils;
import org.opencv.core.RotatedRect;
import org.openpnp.ConfigurationListener;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceFeeder;
import org.openpnp.machine.reference.feeder.wizards.ReferenceHeapFeederConfigurationWizard;
import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.Configuration;
import org.openpnp.model.Identifiable;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Named;
import org.openpnp.model.Part;
import org.openpnp.spi.Camera;
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

public class ReferenceHeapFeeder extends ReferenceFeeder {
    private final static Logger logger = LoggerFactory.getLogger(ReferenceHeapFeeder.class);

    // some settings that I might expose later
    final int vacuumOnStableMS = 250;
    final int vacuumOffStableMS = 250;
    final int maxThrowRetries = 12;
    
    @Attribute(required = false)
    private String dropBoxId;

    private DropBox dropBox;
    
    @Attribute(required = false)
    private double boxDepth = 10.0f;
    
    @Attribute(required = false)
    private double feedFeedDepth = 0;

    @Element(required = false)
    private Location way1 = new Location(LengthUnit.Millimeters);
    
    @Element(required = false)
    private Location way2 = new Location(LengthUnit.Millimeters);
    
    @Element(required = false)
    private Location way3 = new Location(LengthUnit.Millimeters);
    
    @Attribute(required = false)
    private int throwAwayDropBoxContentAfterFailedFeeds = 6;

    @Attribute(required = false)
    private int requiredVacuumDifference = 200;

    @Element(required = false)
    private CvPipeline feederPipeline = createFeederPipeline();

    @Element(required = false)
    private CvPipeline trainingPipeline = createTrainingPipeline();

    private Location pickLocation;
    
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
    @Persist
    public void persist() {
        dropBoxId = getDropBox().getId();
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

    @Override
    public void feed(Nozzle nozzle) throws Exception {       
        // there might be foreign parts in the dropBox, clean up first.
        if (dropBox.getLastHeap() != this) {
            dropBox.clean(nozzle);
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

    private void fetchParts(Nozzle nozzle) {
        // TODO Auto-generated method stub
        
    }
    private Location getFeederPart(Nozzle nozzle) {
        // TODO Auto-generated method stub
        return null;
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
    
    public double getBoxDepth() {
        return boxDepth;
    }
    public void setBoxDepth(double boxDepth) {
        this.boxDepth = boxDepth;
    }
    public double getFeedFeedDepth() {
        return feedFeedDepth;
    }
    public void setFeedFeedDepth(double feedFeedDepth) {
        this.feedFeedDepth = feedFeedDepth;
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
    public int getVacuumOnStableMS() {
        return vacuumOnStableMS;
    }
    public int getVacuumOffStableMS() {
        return vacuumOffStableMS;
    }

    @Root
    public static class DropBox extends AbstractModelObject implements Identifiable, Named {
        
        public CvPipeline getPartPipeline() {
            return partPipeline;
        }

        public boolean tryToFlipSomePart(Nozzle nozzle) {
            // TODO Auto-generated method stub
            return false;
        }

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

        private void removePart(Nozzle nozzle, Location partLocation) throws Exception {
            // basically two cases, back to feeder or to the trash
            if (lastHeap == null) { // unknown parts => trash
                // check nozzle tip
                if ( !dummyPartForUnknown.getPackage().getCompatibleNozzleTips().contains(nozzle)) {
                    nozzle.loadNozzleTip(dummyPartForUnknown.getPackage().getCompatibleNozzleTips().toArray(new NozzleTip[0])[0]);
                }
                pickPart(nozzle, partLocation, Configuration.get().getPart("HeapFeeder-Dummy"));
                dropPart(nozzle, Configuration.get().getMachine().getDiscardLocation());
            } else {    // known origin, not wasting parts
                if ( !lastHeap.getPart().getPackage().getCompatibleNozzleTips().contains(nozzle)) {
                    nozzle.loadNozzleTip(lastHeap.getPart().getPackage().getCompatibleNozzleTips().toArray(new NozzleTip[0])[0]);
                }
                pickPart(nozzle, partLocation, lastHeap.getPart());
                nozzle.moveToSafeZ();
                lastHeap.moveToHeap();
                dropPart(nozzle, lastHeap.getLocation());
            }
            
        }

        private void pickPart(Nozzle nozzle, Location location, Part part) throws Exception {
            // Move to pick location.
            MovableUtils.moveToLocationAtSafeZ(nozzle, location);
    
            // Pick
            nozzle.pick(part);
    
            // Retract
            nozzle.moveToSafeZ();
        }

        public void dropPart(Nozzle nozzle, Location location) throws Exception {
            // move to the  location
            MovableUtils.moveToLocationAtSafeZ(nozzle,
                    Configuration.get().getMachine().getDiscardLocation());
            // discard the part
            nozzle.place();
            nozzle.moveToSafeZ();
        }

        
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
                        double partHeight = 0d;
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

        private Location getNearestPart(CvPipeline pipeline, Camera camera, Nozzle nozzle) {
            // Process the pipeline to extract RotatedRect results
            pipeline.setProperty("camera", camera);
            pipeline.setProperty("nozzle", nozzle);
            pipeline.setProperty("feeder", this);
            pipeline.process();
            // Grab the results
            List<RotatedRect> results =
                    (List<RotatedRect>) pipeline.getResult(VisionUtils.PIPELINE_RESULTS_NAME).model;
            if (results.isEmpty()) {
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

        public ReferenceHeapFeeder getLastHeap() {
            return lastHeap;
        }

        public void setLastHeap(ReferenceHeapFeeder lastHeap) {
            this.lastHeap = lastHeap;
        }


        @Attribute(name = "id")
        final private String id;

        @Attribute
        private String name;
                        
        @Element
        private CvPipeline partPipeline = createPartPipeline();
        
        @Element
        private Location centerBottomLocation = new Location(LengthUnit.Millimeters);

        @Attribute
        private String dummyPartIdForUnknown;
        
        private Part dummyPartForUnknown;
        
        @Commit
        public void commit() {
            Configuration.get().addListener(new ConfigurationListener() {
                @Override
                public void configurationComplete(Configuration configuration) throws Exception {
                    setDummyPartForUnknown(Configuration.get().getPart(dummyPartIdForUnknown));
                }

                @Override
                public void configurationLoaded(Configuration configuration) throws Exception {
                    // do nothing
                }
            });
        }
        @Persist
        public void persist() {
            dummyPartIdForUnknown = dummyPartForUnknown.getId();
        }
        
        private ReferenceHeapFeeder lastHeap = null;
        
        public DropBox() {
            this(Configuration.createId("DropBox-"));
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

        public DropBox(@Attribute(name = "id") String id) {
            if (id == null) {
                throw new Error("Id is required.");
            }
            this.id = id;
            this.name = id;
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
        
        @Override
        public String toString() {
            return name;
        }
        
        public void resetPartPipeline() {
            partPipeline = createPartPipeline();
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

    public void moveToHeap() {
        // TODO Auto-generated method stub
        
    }
}
