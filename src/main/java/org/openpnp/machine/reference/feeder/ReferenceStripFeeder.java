/*
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.machine.reference.feeder;



import java.util.List;

import javax.swing.Action;

import org.apache.commons.io.IOUtils;
import org.openpnp.ConfigurationListener;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.FeederWithOptions;
import org.openpnp.machine.reference.feeder.wizards.ReferenceStripFeederConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Point;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MachineListener;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.util.Utils2D;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persist;


/**
 * Implementation of Feeder that indexes through a strip of cut tape. This is a specialization of
 * the tray feeder that knows specifics about tape so that vision capabilities can be added.
 */

/**
 * SMD tape standard info from http://www.liteplacer.com/setup-tape-positions-2/
 * 
 * holes 1.5mm
 * 
 * hole pitch 4mm
 * 
 * first part center to reference hole linear is 2mm
 * 
 * tape width is multiple of 4mm
 * 
 * part pitch is multiple of 4mm except for 0402 and smaller, where it is 2mm
 * 
 * hole to part lateral is tape width / 2 - 0.5mm
 */
public class ReferenceStripFeeder extends FeederWithOptions {
    public enum TapeType {
        WhitePaper("White Paper"),
        BlackPlastic("Black Plastic"),
        ClearPlastic("Clear Plastic");

        private String name;

        TapeType(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    @Element(required = false)
    private Location referenceHoleLocation = new Location(LengthUnit.Millimeters);

    @Element(required = false)
    private Location lastHoleLocation = new Location(LengthUnit.Millimeters);

    @Element(required = false)
    private Length partPitch = new Length(4, LengthUnit.Millimeters);

    @Element(required = false)
    private Length tapeWidth = new Length(8, LengthUnit.Millimeters);

    @Attribute(required = false)
    private TapeType tapeType = TapeType.WhitePaper;

    @Attribute(required = false)
    private Boolean standardEia481 = null;

    @Attribute(required = false)
    private boolean visionEnabled = true;

    @Element(required = false)
    private CvPipeline pipeline = createDefaultPipeline();

    @Attribute
    private int feedCount = 0;

	@Attribute(required = false)
	private int maxFeedCount = 0;

    private Length holeDiameter = new Length(1.5, LengthUnit.Millimeters);

    private Length holePitch = new Length(4, LengthUnit.Millimeters);

    private Length referenceHoleToPartLinear = new Length(2, LengthUnit.Millimeters);

    private Location visionLocation;
    private Location visionLocationReference;

    public Length getHoleDiameterMin() {
        return getHoleDiameter().multiply(0.9);
    }

    public Length getHoleDiameterMax() {
        return getHoleDiameter().multiply(1.1);
    }

    public Length getHolePitchMin() {
        return getHolePitch().multiply(0.9);
    }

    public Length getHoleDistanceMin() {
        return getTapeWidth().multiply(0.25);
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

    public ReferenceStripFeeder() {
        // Listen to the machine become unhomed to invalidate feeder calibration.
        // Note that home()  first switches the machine isHomed() state off, then on again,
        // so we also catch re-homing.
        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            @Override
            public void configurationComplete(Configuration configuration) throws Exception {
                Configuration.get().getMachine().addListener(new MachineListener.Adapter() {

                    @Override
                    public void machineHeadActivity(Machine machine, Head head) {
                        checkHomedState(machine);
                    }

                    @Override
                    public void machineEnabled(Machine machine) {
                        checkHomedState(machine);
                    }
                });
            }
        });
    }

    private void checkHomedState(Machine machine) {
        if (!machine.isHomed()) {
            resetVision();
        }
    }

    @Override
    public Location getPickLocation() throws Exception {
        int feedCount = this.feedCount;

        /*
         * As a special case, before the feeder has been fed we return the pick location
         * as if the feeder had been fed. This keeps us from returning a pick location
         * that is off the edge of the strip.
         */
        if (feedCount == 0) {
            feedCount = 1;
        }
        // Find the location of the part linearly along the tape
        Location[] lineLocations = getIdealLineLocations();
        // 20160608 - ldpgh/lutz_dd
        // partPichtAdjusted:double ... match prev. partPitch.getValue()
        // partPitchAdjusted is the euclidian distance of ReferenceHole and NextHole and divided by
        // the amount of part locations in between. This Part count is derived from the distance
        // and the given partPitch in GUI and afterwards rounded to the next integer value.
        // partPitch/partPitchAdjusted limitation
        // It's the P1 value according to EIA-481-C, October 2003, pg. 9, 11, 13
        // Accuracy variations as specified in the document are not taken into account!
        double partPitchAdjusted = lineLocations[0].getLinearDistanceTo(lineLocations[1]);
        double holeCount = (Math.round(partPitchAdjusted / partPitch.getValue()));

        // if the two points are at least 1 hole apart, we can compute the adjusted pitch. 
        // otherwise use the un-adjusted holePitch (and avoid a divide by zero).  The second 
        // case means the feeder is set up incorrectly but has been tested to behave in a 
        // reasonable way, even if the two points are coincident
        if (holeCount > 0) {
        	partPitchAdjusted = partPitchAdjusted / (Math.round(partPitchAdjusted / partPitch.getValue()));
        } else {
        	partPitchAdjusted = holePitch.getValue();
        }
        	
        Location l = Utils2D.getPointAlongLine(lineLocations[0], lineLocations[1],
                new Length((feedCount - 1) * partPitchAdjusted, partPitch.getUnits()));
        // Create the offsets that are required to go from a reference hole
        // to the part in the tape
        Length x = getHoleToPartLateral().convertToUnits(l.getUnits());
        Length y = referenceHoleToPartLinear.convertToUnits(l.getUnits());
        Point p = new Point(x.getValue(), y.getValue());

        // Determine the angle that the tape is at. For historical reasons, our local coordinates are
        // rotated -90° (vertical tape with sprocket holes left). 
        double angle = Utils2D.getAngleFromPoint(lineLocations[1], lineLocations[0]) - 90;
        // Rotate the part offsets by the angle to move it into the right
        // coordinate space
        p = Utils2D.rotatePoint(p, angle);
        // And add the offset to the location we calculated previously
        l = l.add(new Location(l.getUnits(), p.x, p.y, 0, 0));
        // Add in the angle of the tape plus the angle of the part in the tape
        // so that the part is picked at the right angle
        if (isStandardEia481()) {
            // Angle with the sprocket holes on top, as per the EIA-481-C industry standard.  
            angle += 90;
        }
        l = l.derive(null, null, null, angle + getLocation().getRotation());

        return l;
    }

    public void ensureFeederZ(Camera camera) throws Exception {
        if (camera.isUnitsPerPixelAtZCalibrated()
                && !getReferenceHoleLocation().getLengthZ().isInitialized()) {
            throw new Exception("Feeder "+getName()+": please set the Reference Hole Location Z coordinate first, "
                    + "it is required to determine the true scale of the camera view for accurate computer vision.");
        }
    }

    public Location[] getIdealLineLocations() {
        if (visionLocation == null) {
            return new Location[] {referenceHoleLocation, lastHoleLocation};
        }

        Location effectiveReferenceHoleLocation;
        Location effectiveLastHoleLocation;
        if (visionLocationReference == null) {
            effectiveReferenceHoleLocation = referenceHoleLocation;
            effectiveLastHoleLocation = lastHoleLocation;
        }
        else {
            effectiveReferenceHoleLocation = visionLocationReference;
            // We have a vision-adjusted reference.
            // Apply the same offset to the configured last hole, so that the relative position (ie tape angle) is unchanged
            effectiveLastHoleLocation = lastHoleLocation.add(visionLocationReference).subtract(referenceHoleLocation);
        }

        double d1 = effectiveReferenceHoleLocation.getLinearLengthTo(effectiveLastHoleLocation)
                .convertToUnits(LengthUnit.Millimeters).getValue();
        double d2 = effectiveReferenceHoleLocation.getLinearLengthTo(visionLocation)
                .convertToUnits(LengthUnit.Millimeters).getValue();
        if (d2 > d1 * 0.9) { // factor to bias towards using vision location if distances are approximately equal
            return new Location[] {effectiveReferenceHoleLocation, visionLocation};
        }
        else {
            return new Location[] {effectiveReferenceHoleLocation, effectiveLastHoleLocation};
        }
    }

    public void feed(Nozzle nozzle) throws Exception {
        if (getFeedOptions() == FeedOptions.Normal) {
            setFeedCount(getFeedCount() + 1);
        }

        // Throw an exception when the feeder runs out of parts
        if ((maxFeedCount > 0) && (feedCount > maxFeedCount)) {
			throw new Exception("Tried to feed part: " + part.getId() + "  Feeder " + name + " empty.");
		}

        if (feedCount!=1 && visionLocationReference == null) {
            // We are performing the first pick in the middle of the strip.
            // Determine the exact location of the reference hole too.
            updateVisionOffsets(nozzle,1);
        }

        if (feedOptions == FeedOptions.Normal) {
            updateVisionOffsets(nozzle,feedCount);
        }
        if (getFeedOptions() == FeedOptions.SkipNext) {
            setFeedOptions(FeedOptions.Normal);
        }
    }

    private void updateVisionOffsets(Nozzle nozzle,Integer visionFeedCount) throws Exception {
        if (!visionEnabled) {
            return;
        }
        // go to where we expect to find the next reference hole
        Camera camera = nozzle.getHead().getDefaultCamera();
        ensureFeederZ(camera);
        Location expectedLocation = null;
        Location[] lineLocations = getIdealLineLocations();

        if (partPitch.convertToUnits(LengthUnit.Millimeters).getValue() < 4) {
            // For tapes with a part pitch < 4 we need to check each hole
            // twice since there are two parts per reference hole.
            // Note the use of holePitch here and partPitch in the
            // alternate case below.
            expectedLocation = Utils2D.getPointAlongLine(lineLocations[0], lineLocations[1],
                    holePitch.multiply((visionFeedCount - 1) / 2));
        }
        else {
            // For tapes with a part pitch >= 4 there is always a reference
            // hole 2mm from a part so we just multiply by the part pitch
            // skipping over holes that are not reference holes.
            expectedLocation = Utils2D.getPointAlongLine(lineLocations[0], lineLocations[1],
                    partPitch.multiply(visionFeedCount - 1));
        }
        MovableUtils.moveToLocationAtSafeZ(camera, expectedLocation);
        // and look for the hole
        Location actualLocation = findClosestHole(camera);
        if (actualLocation == null) {
            throw new Exception("Unable to locate reference hole. End of strip?");
        }
        // make sure it's not too far away
        Length distance = actualLocation.getLinearLengthTo(expectedLocation)
                .convertToUnits(LengthUnit.Millimeters);
        if (distance.getValue() > 2) {
            throw new Exception("Unable to locate reference hole. End of strip?");
        }

        if (visionFeedCount==1) {
            visionLocationReference = actualLocation;
        }

        visionLocation = actualLocation;
    }

    private Location findClosestHole(Camera camera) throws Exception {
        try (CvPipeline pipeline = getPipeline()) {
            Integer pxMinDistance = (int) VisionUtils.toPixels(getHolePitchMin(), camera);
            Integer pxMinDiameter = (int) VisionUtils.toPixels(getHoleDiameterMin(), camera);
            Integer pxMaxDiameter = (int) VisionUtils.toPixels(getHoleDiameterMax(), camera);
    
            // Process the pipeline to clean up the image and detect the tape holes
            pipeline.setProperty("camera", camera);
            pipeline.setProperty("feeder", this);
            pipeline.setProperty("DetectFixedCirclesHough.minDistance", pxMinDistance);
            pipeline.setProperty("DetectFixedCirclesHough.minDiameter", pxMinDiameter);
            pipeline.setProperty("DetectFixedCirclesHough.maxDiameter", pxMaxDiameter);
            pipeline.setProperty("sprocketHole.diameter", getHoleDiameter());
            // Search range is half-way to the next hole. 
            pipeline.setProperty("sprocketHole.maxDistance", getHolePitch().multiply(0.5));
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

            // Find the closest result
            results.sort((a, b) -> {
                Double da = VisionUtils.getPixelLocation(camera, a.x, a.y)
                        .getLinearDistanceTo(camera.getLocation());
                Double db = VisionUtils.getPixelLocation(camera, b.x, b.y)
                        .getLinearDistanceTo(camera.getLocation());
                return da.compareTo(db);
            });
    
            CvStage.Result.Circle closestResult = results.get(0);
            Location holeLocation = VisionUtils.getPixelLocation(camera, closestResult.x, closestResult.y);
            return holeLocation;
        }
    }
   
    /**
     * Returns if the feeder can take back a part.
     * Makes the assumption, that after each feed a pick followed,
     * so the pockets are now empty.
     */
    @Override
    public boolean canTakeBackPart() {
        if (feedCount > 0 ) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void takeBackPart(Nozzle nozzle) throws Exception {
        // first check if we can and want to take back this part (should be always be checked before calling, but to be sure)
        if (nozzle.getPart() == null) {
            throw new UnsupportedOperationException("No part loaded that could be taken back.");
        }
        if (!nozzle.getPart().equals(getPart())) {
            throw new UnsupportedOperationException("Feeder: " + getName() + " - Can not take back " + nozzle.getPart().getName() + " this feeder only supports " + getPart().getName());
        }
        if (!canTakeBackPart()) {
            throw new UnsupportedOperationException("Feeder: " + getName() + " - Currently no free slot. Can not take back the part.");
        }
        
        // ok, now put the part back on the location of the last pick
        nozzle.moveToPickLocation(this);
        // put the part back
        nozzle.place();
        nozzle.moveToSafeZ();
        if (nozzle.isPartOffEnabled(Nozzle.PartOffStep.AfterPlace) && !nozzle.isPartOff()) {
            throw new Exception("Feeder: " + getName() + " - Putting part back failed, check nozzle tip");
        }
        // change FeedCount
        setFeedCount(getFeedCount() - 1);
    }
        
    
    public CvPipeline getPipeline() {
        return pipeline;
    }

    public void resetPipeline() {
        pipeline = createDefaultPipeline();
    }

    private Length getHoleToPartLateral() {
        Length tapeWidth = this.tapeWidth.convertToUnits(LengthUnit.Millimeters);
        return new Length(tapeWidth.getValue() / 2 - 0.5, LengthUnit.Millimeters);
    }

    public TapeType getTapeType() {
        return tapeType;
    }

    public void setTapeType(TapeType tapeType) {
        this.tapeType = tapeType;
    }

    public Location getReferenceHoleLocation() {
        return referenceHoleLocation;
    }

    public void setReferenceHoleLocation(Location referenceHoleLocation) {
        Object oldValue = this.referenceHoleLocation;
        this.referenceHoleLocation = referenceHoleLocation;
        resetVision();
        firePropertyChange("referenceHoleLocation", oldValue, referenceHoleLocation);
    }

    public void resetVision() {
        visionLocation = null;
        visionLocationReference = null;
    }

    public Location getLastHoleLocation() {
        return lastHoleLocation;
    }

    public void setLastHoleLocation(Location lastHoleLocation) {
        Object oldValue = this.lastHoleLocation;
        this.lastHoleLocation = lastHoleLocation;
        resetVision();
        firePropertyChange("lastHoleLocation", oldValue, lastHoleLocation);
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

    public Length getPartPitch() {
        return partPitch;
    }

    public void setPartPitch(Length partPitch) {
        this.partPitch = partPitch;
    }

    public Length getTapeWidth() {
        return tapeWidth;
    }

    public void setTapeWidth(Length tapeWidth) {
        this.tapeWidth = tapeWidth;
    }

    public int getFeedCount() {
        return feedCount;
    }

    public void setFeedCount(int feedCount) {
        int oldValue = this.feedCount;
        this.feedCount = feedCount;
        firePropertyChange("feedCount", oldValue, feedCount);

        if (feedCount==0) {
            // Feeder has been reset, so restart vision from the configured starting points
            resetVision();
        }
    }

	public int getMaxFeedCount() {
		return maxFeedCount;
	}

	public void setMaxFeedCount(int count) {
		maxFeedCount = count;
	}

    public Length getReferenceHoleToPartLinear() {
        return referenceHoleToPartLinear;
    }

    public void setReferenceHoleToPartLinear(Length referenceHoleToPartLinear) {
        this.referenceHoleToPartLinear = referenceHoleToPartLinear;
    }

    public boolean isVisionEnabled() {
        return visionEnabled;
    }

    public void setVisionEnabled(boolean visionEnabled) {
        this.visionEnabled = visionEnabled;
    }

    @Commit
    void commit() {
        if (standardEia481 == null) {
            // If loaded from old configuration, we must migrate it to the EIA-481-C industry 
            // standard rotation in tape, where tape 0° is with the sprocket holes on top.
            setLocation(getLocation().derive(null, null, null, 
                    Utils2D.angleNorm(getLocation().getRotation() - 90, 180)));
            standardEia481 = true;
        }
    }

    @Persist
    private void persist() {
        // Make sure the EIA-481 flag is initialized before persisting.
        isStandardEia481(); // using side effect
    }

    public boolean isStandardEia481() {
        if (standardEia481 == null) {
            // This is a new feeder.
            standardEia481 = true;
        }
        return standardEia481;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceStripFeederConfigurationWizard(this);
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

    private static CvPipeline createDefaultPipeline() {
        try {
            String xml = IOUtils.toString(ReferenceStripFeeder.class
                    .getResource("ReferenceStripFeeder-DefaultPipeline.xml"));
            return new CvPipeline(xml);
        }
        catch (Exception e) {
            throw new Error(e);
        }
    }
}

// this code left here in case we want to use it in the future. it is for
// calculating how many parts there are based on the first and last reference hole.
//// figure out how many parts there should be by taking the delta
//// between the two holes and dividing it by part pitch
// double holeToHoleDistance = lastHoleLocation.getLinearDistanceTo(referenceHoleLocation);
//// take the ceil of the distance to account for any minor offset from
//// center of the hole
// holeToHoleDistance = Math.ceil(holeToHoleDistance);
// double partPitch = this.partPitch.convertToUnits(lastHoleLocation.getUnits()).getValue();
//// And floor the part count because you can't have a partial part.
// double partCount = Math.floor(holeToHoleDistance / partPitch);
//
//// if (feedCount > partCount) {
//// throw new Exception(String.format("No more parts available in feeder %s", getName()));
//// }
//
