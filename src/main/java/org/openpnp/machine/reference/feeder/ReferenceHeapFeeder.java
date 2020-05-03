package org.openpnp.machine.reference.feeder;

import javax.swing.Action;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceFeeder;
import org.openpnp.machine.reference.feeder.wizards.ReferenceHeapFeederConfigurationWizard;
import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.Configuration;
import org.openpnp.model.Identifiable;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Named;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.util.IdentifiableList;
import org.openpnp.vision.pipeline.CvPipeline;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persist;

public class ReferenceHeapFeeder extends ReferenceFeeder {
    // some settings that I might expose later
    final int vacuumOnStableMS = 250;
    final int vacuumOffStableMS = 250;
    
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

    
    public ReferenceHeapFeeder() {
        super();
        retryCount = 12;
    }
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
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void feed(Nozzle nozzle) throws Exception {
        // TODO Auto-generated method stub
        
    }

    @Override
    public String getPropertySheetHolderTitle() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        // TODO Auto-generated method stub
        return null;
    }

    private CvPipeline createTrainingPipeline() {
        // TODO Auto-generated method stub
        return null;
    }
    private CvPipeline createFeederPipeline() {
        // TODO Auto-generated method stub
        return null;
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

        public void setPartPipeline(CvPipeline partPipeline) {
            this.partPipeline = partPipeline;
        }

        public Location getCenterBottomLocation() {
            return centerBottomLocation;
        }

        public void setCenterBottomLocation(Location centerBottomLocation) {
            this.centerBottomLocation = centerBottomLocation;
        }

        public NozzleTip getNozzleTipForUnknown() {
            return nozzleTipForUnknown;
        }

        public void setNozzleTipForUnknown(NozzleTip nozzleTipForUnknown) {
            this.nozzleTipForUnknown = nozzleTipForUnknown;
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
                        
        @Attribute
        private CvPipeline partPipeline = createPartPipeline();
        
        @Element
        private Location centerBottomLocation = new Location(LengthUnit.Millimeters);

        @Attribute
        private String nozzleTipIdForUnknown;
        
        private NozzleTip nozzleTipForUnknown;
        
        @Commit
        public void commit() {
            Configuration.get().addListener(new ConfigurationListener() {
                @Override
                public void configurationComplete(Configuration configuration) throws Exception {
                    setNozzleTipForUnknown(Configuration.get().getMachine().getNozzleTip(nozzleTipIdForUnknown));
                }

                @Override
                public void configurationLoaded(Configuration configuration) throws Exception {
                    // do nothing
                }
            });
        }
        @Persist
        public void persist() {
            nozzleTipIdForUnknown = nozzleTipForUnknown.getId();
        }
        
        private ReferenceHeapFeeder lastHeap = null;
        
        public DropBox() {
            this(Configuration.createId("DropBox-"));
        }

        private CvPipeline createPartPipeline() {
            // TODO Auto-generated method stub
            return null;
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
}
