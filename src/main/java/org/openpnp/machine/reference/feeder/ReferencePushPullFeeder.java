/*
 * Copyright (C) 2020 <mark@makr.zone>
 * based on the ReferenceLeverFeeder 
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

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.Action;

import org.apache.commons.io.IOUtils;
import org.openpnp.ConfigurationListener;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceFeeder;
import org.openpnp.machine.reference.feeder.wizards.ReferencePushPullFeederConfigurationWizard;
import org.openpnp.machine.reference.feeder.wizards.ReferencePushPullMotionConfigurationWizard;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.model.RegionOfInterest;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Axis;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MachineListener;
import org.openpnp.spi.MotionPlanner;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.util.FeederVisionHelper;
import org.openpnp.util.FeederVisionHelper.FeederVisionHelperParams;
import org.openpnp.util.FeederVisionHelper.FindFeaturesMode;
import org.openpnp.util.FeederVisionHelper.PipelineType;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.OcrUtils;
import org.openpnp.util.TravellingSalesman;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.stages.SimpleOcr;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Persist;

public class ReferencePushPullFeeder extends ReferenceFeeder {

    // Rotation of the part within the feeder (i.e. within the tape)
    // This is compatible with tonyluken's pending PR #943 or a similar solution.
    // Conversely, feeder.location.rotation contains the orientation of the feeder itself
    // and it defines the local feeder coordinate system. The rotationInFeeder here can be removed
    // once it is inherited. 
    @Attribute(required=false)
    protected Double rotationInFeeder = Double.valueOf(0.0);

    @Attribute(required = false)
    protected boolean normalizePickLocation = true;

    @Attribute(required = false)
    protected boolean snapToAxis = true;

    @Element(required = false)
    protected Location hole1Location = new Location(LengthUnit.Millimeters);
    @Element(required = false)
    protected Location hole2Location = new Location(LengthUnit.Millimeters);

    @Attribute(required = false)
    protected boolean usedAsTemplate = false; 

    @Attribute(required = false)
    protected boolean calibrateMotionX = true; 
    @Attribute(required = false)
    protected boolean calibrateMotionY = true; 

    @Element
    protected Location feedStartLocation = new Location(LengthUnit.Millimeters);
    @Element(required = false)
    protected Location feedMid1Location = new Location(LengthUnit.Millimeters);
    @Element(required = false)
    protected Location feedMid2Location = new Location(LengthUnit.Millimeters);
    @Element(required = false)
    protected Location feedMid3Location = new Location(LengthUnit.Millimeters);
    @Element
    protected Location feedEndLocation = new Location(LengthUnit.Millimeters);
    @Element(required = false)
    private Length partPitch = new Length(4, LengthUnit.Millimeters);
    @Element(required = false)
    private Length feedPitch = new Length(4, LengthUnit.Millimeters);
    @Attribute(required = false)
    private long feedMultiplier= 1;

    @Element(required = false)
    protected double feedSpeedPush1 = 1.0; 
    @Attribute(required = false)
    protected double feedSpeedPush2 = 1.0; 
    @Attribute(required = false)
    protected double feedSpeedPush3 = 1.0; 
    @Attribute(required = false)
    protected double feedSpeedPushEnd = 1.0;
    @Attribute(required = false)
    protected double feedSpeedPull3 = 1.0;
    @Attribute(required = false)
    protected double feedSpeedPull2 = 1.0;
    @Attribute(required = false)
    protected double feedSpeedPull1 = 1.0;
    @Attribute(required = false)
    protected double feedSpeedPull0 = 1.0;

    @Attribute(required = false)
    protected boolean includedPush1 = false; 
    @Attribute(required = false)
    protected boolean includedPush2 = false; 
    @Attribute(required = false)
    protected boolean includedPush3 = false; 
    @Attribute(required = false)
    protected boolean includedPushEnd = true; 

    @Attribute(required = false)
    protected boolean includedMulti0 = true; 
    @Attribute(required = false)
    protected boolean includedMulti1 = false; 
    @Attribute(required = false)
    protected boolean includedMulti2 = false; 
    @Attribute(required = false)
    protected boolean includedMulti3 = false; 
    @Attribute(required = false)
    protected boolean includedMultiEnd = true; 

    @Attribute(required = false)
    protected boolean includedPull0 = true; 
    @Attribute(required = false)
    protected boolean includedPull1 = false; 
    @Attribute(required = false)
    protected boolean includedPull2 = false; 
    @Attribute(required = false)
    protected boolean includedPull3 = false; 

    @Attribute(required = false)
    protected boolean additiveRotation = true;

    @Attribute(required = false)
    private String actuatorName;
    protected Actuator actuator;
    /**
     * "peelOff" is a legacy name, it is now recommended to use the rotation axis for peeling
     */
    @Attribute(required = false)
    private  String peelOffActuatorName;
    protected Actuator actuator2;

    @Attribute(required = false)
    private long feedCount = 0;

    @Element(required = false)
    private CvPipeline pipeline = FeederVisionHelper.createDefaultPipeline(PipelineType.ColorKeyed);
    @Attribute(required = false)
    protected PipelineType pipelineType = PipelineType.ColorKeyed;

    @Attribute(required = false)
    protected String ocrFontName = "Liberation Mono";
    @Attribute(required = false)
    protected double ocrFontSizePt = 7.0;
    @Element(required = false)
    protected RegionOfInterest ocrRegion = null; 
    
    public enum OcrWrongPartAction {
        None,
        SwapFeeders,
        SwapOrCreate,
        ChangePart,
        ChangePartAndClone
    }

    @Attribute(required = false)
    protected OcrWrongPartAction ocrWrongPartAction = OcrWrongPartAction.SwapOrCreate;
    @Attribute(required = false)
    protected boolean ocrDiscoverOnJobStart = true;
    @Attribute(required = false)
    protected boolean ocrStopAfterWrongPart = false;

    @Element(required = false)
    private Length precisionWanted = new Length(0.1, LengthUnit.Millimeters);

    @Attribute(required = false)
    private int calibrationCount = 0;
    @Element(required = false)
    private Length sumOfErrors = new Length(0, LengthUnit.Millimeters);
    @Element(required = false)
    private Length sumOfErrorSquares = new Length(0, LengthUnit.Millimeters);


    // These are not on the GUI but can be tweaked in the machine.xml /////////////////

    // initial calibration tolerance, i.e. how much the feeder can be shifted physically 
    @Attribute(required = false)
    private double calibrationToleranceMm = 1.95;
    // vision and comparison sprocket hole tolerance (in size, position)
    @Attribute(required = false)
    private double sprocketHoleToleranceMm = 0.6;
    // for rows of feeders, the tolerance in X, Y
    @Attribute(required = false)
    private double rowLocationToleranceMm = 4.0; 
    // for rows of feeders, the tolerance in Z
    @Attribute(required = false)
    private double rowZLocationToleranceMm = 1.0; 

    @Attribute(required = false)
    private int calibrateMaxPasses = 3; 
    // how close the camera has to be to prevent one more pass
    @Attribute(required = false)
    private double calibrateToleranceMm = 0.3; 
    @Attribute(required = false)
    private int calibrateMinStatistic = 2; 

    // Some EIA 481 standard constants.
    static final double sprocketHoleDiameterMm = 1.5;
    static final double sprocketHolePitchMm = 4;
    static final double minSprocketHolesDistanceMm = 3.5;

    /*
     * visionOffset contains the difference between where the part was expected to be and where it
     * is. Subtracting these offsets from the pickLocation produces the correct pick location.
     */
    protected Location visionOffset;

    public enum CalibrationTrigger {
        None,
        OnFirstUse,
        UntilConfident,
        OnEachTapeFeed
    }

    @Attribute(required = false)
    protected CalibrationTrigger calibrationTrigger = CalibrationTrigger.UntilConfident;

    public static final Location nullLocation = new Location(LengthUnit.Millimeters);

    private void checkHomedState(Machine machine) {
        if (!machine.isHomed()) {
            this.resetCalibration();
        }
    }

    public ReferencePushPullFeeder() {
        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            @Override
            public void configurationComplete(Configuration configuration) throws Exception {
                // Resolve the actuators by name (legacy way).
                Head head = Configuration.get().getMachine().getDefaultHead();
                try {
                    actuator = head.getActuatorByName(actuatorName);
                }
                catch (Exception e) {
                }
                try {
                    actuator2 = head.getActuatorByName(peelOffActuatorName);
                }
                catch (Exception e) {
                }
                // Listen to the machine become unhomed to invalidate feeder calibration.
                // Note that home()  first switches the machine isHomed() state off, then on again, 
                // so we also catch re-homing.
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

    @Persist
    private void persist() {
        // Make sure the newest names are persisted (legacy way).
        actuatorName = (actuator == null ? null : actuator.getName()); 
        peelOffActuatorName = (actuator2 == null ? null : actuator2.getName()); 
    }

    public Camera getCamera() throws Exception {
        return  Configuration.get()
                .getMachine()
                .getDefaultHead()
                .getDefaultCamera();
    }

    private FeederVisionHelperParams getVisionHelperParams(Camera camera, CvPipeline pipeline) {
        return new FeederVisionHelperParams(camera, pipelineType, pipeline, 2000
            , this.normalizePickLocation, this.snapToAxis
            , this.partPitch, this.feedPitch, this.feedMultiplier
            , this.location, this.hole1Location, this.hole2Location
            , this.calibrationToleranceMm, this.sprocketHoleToleranceMm);
    }

    public void assertCalibrated(boolean tapeFeed) throws Exception {
        if (getHole1Location().convertToUnits(LengthUnit.Millimeters).getLinearDistanceTo(getHole2Location()) < 3) {
            throw new Exception("Feeder "+getName()+" sprocket hole locations undefined/too close together.");
        }
        if ((visionOffset == null && calibrationTrigger != CalibrationTrigger.None)
                || (tapeFeed && calibrationTrigger == CalibrationTrigger.UntilConfident && !isPrecisionSufficient())
                || (tapeFeed && calibrationTrigger == CalibrationTrigger.OnEachTapeFeed)) {
            // not yet calibrated (enough)
            obtainCalibratedVisionOffset();
            if (visionOffset == null) {
                // no lock obtained
                throw new Exception(String.format("Vision failed on feeder %s.", getName()));
            }
        }
    }

    public boolean isPrecisionSufficient() {
        if (calibrationCount < calibrateMinStatistic) {
            return false;
        }
        else if (getPrecisionConfidenceLimit().divide(getPrecisionWanted()) > 1.0) {
            return false;
        }
        return true;
    }

    public boolean isVisionEnabled() {
        return calibrationTrigger != CalibrationTrigger.None;
    }

    @Override
    public Location getPickLocation() throws Exception {
        // Numbers are 1-based (a feed is needed before the very first part can be picked),
        // therefore the modulo calculation is a bit gnarly.
        // The 1-based approach has the benefit, that at feed count 0 (reset) the part closest to the reel 
        // is the pick location which is the last part in a multi-part feed cycle, which is the one we want for setup.  
        long partInCycle = ((getFeedCount()+getPartsPerFeedCycle()-1) % getPartsPerFeedCycle())+1;
        return getPickLocation(partInCycle, visionOffset);
    }

    @Override
    public void feed(Nozzle nozzle) throws Exception {
        Logger.debug("feed({})", nozzle);

        Head head = nozzle.getHead();
        if (actuator == null) {
            throw new Exception(String.format("No feed actuator assigned to feeder %s",
                    getName()));
        }

        if (getFeedCount() % getPartsPerFeedCycle() == 0) {
            // Modulo of feed count is zero - no more parts there to pick, must feed 

            // Make sure we're calibrated
            assertCalibrated(false);

            // Create the effective feed locations, applying the vision offset.
            Location visionOffsets = getVisionOffset()
                    .multiply(
                            (isCalibrateMotionX() ? 1 : 0),
                            (isCalibrateMotionY() ? 1 : 0), 
                            1,  // Z currently not used, but maybe later?
                            0); // Make sure there is no rotation.
            Location feedStartLocation = getFeedStartLocation()
                    .subtractWithRotation(visionOffsets);
            Location feedMid1Location = getFeedMid1Location()
                    .subtractWithRotation(visionOffsets);
            Location feedMid2Location = getFeedMid2Location()
                    .subtractWithRotation(visionOffsets);
            Location feedMid3Location = getFeedMid3Location()
                    .subtractWithRotation(visionOffsets);
            Location feedEndLocation = getFeedEndLocation()
                    .subtractWithRotation(visionOffsets);

            MotionPlanner motionPlanner = Configuration.get().getMachine().getMotionPlanner();
            if (actuator.getAxisRotation() != null && isAdditiveRotation()) {
                // Reset to the rotation axis to zero.
                AxesLocation rotation = actuator.toRaw(actuator.toHeadLocation(
                        actuator.getLocation().multiply(1, 1, 1, 0)))
                        .byType(Axis.Type.Rotation); 
                motionPlanner.setGlobalOffsets(rotation);
            }

            // Move to the Feed Start Location
            MovableUtils.moveToLocationAtSafeZ(actuator, feedStartLocation);
            double baseSpeed = actuator.getHead().getMachine().getSpeed();

            long feedsPerPart = (long)Math.ceil(getPartPitch().divide(getFeedPitch()));
            long n = getFeedMultiplier()*feedsPerPart;
            for (long i = 0; i < n; i++) {  // perform multiple feed actuations if required

                boolean isFirst = (i == 0); 
                boolean isLast = (i == n-1); 

                // enable actuator (may do nothing)
                actuator.actuate(true);

                // Push the lever by following the path of locations
                if (includedPush1 && (isFirst || includedMulti1)) {
                    actuator.moveTo(feedMid1Location, feedSpeedPush1*baseSpeed);
                }
                if (includedPush2 && (isFirst || includedMulti2)) {
                    actuator.moveTo(feedMid2Location, feedSpeedPush2*baseSpeed);
                }
                if (includedPush3 && (isFirst || includedMulti3)) {
                    actuator.moveTo(feedMid3Location, feedSpeedPush3*baseSpeed);
                }
                if (includedPushEnd && (isFirst || includedMultiEnd)) {
                    actuator.moveTo(feedEndLocation, feedSpeedPushEnd*baseSpeed);
                }

                // Start the take up actuator
                if (actuator2 != null) {
                    actuator2.actuate(true);
                }

                // Now move back to the start location to move the tape.
                if (includedPull3 && (isLast || includedMulti3)) {
                    actuator.moveTo(feedMid3Location, feedSpeedPull3 * baseSpeed);
                }
                if (includedPull2 && (isLast || includedMulti2)) {
                    actuator.moveTo(feedMid2Location, feedSpeedPull2 * baseSpeed);
                }
                if (includedPull1 && (isLast || includedMulti1)) {
                    actuator.moveTo(feedMid1Location, feedSpeedPull1*baseSpeed);
                }
                if (includedPull0 && (isLast || includedMulti0)) {
                    actuator.moveTo(feedStartLocation, feedSpeedPull0*baseSpeed);
                }

                // Stop the take up actuator
                if (actuator2 != null) {
                    actuator2.actuate(false);
                }

                // disable actuator
                actuator.actuate(false);

                if (isAdditiveRotation()) {
                    // Reset to the rotation axis to zero for the next iteration. 
                    AxesLocation rotation = actuator.toRaw(actuator.toHeadLocation(
                            actuator.getLocation().multiply(1, 1, 1, 0)))
                            .byType(Axis.Type.Rotation); 
                    motionPlanner.setGlobalOffsets(rotation);
                }

                // Note, the feedStartLocation can be thought a) to be the target of the pull or 
                // b) the starting point of the next push. So we need to look at it a second time 
                // here, AFTER having disabled the actuators, and AFTER having reset the rotation 
                // coordinate. 
                // Well, this is what you need for a multi-actuation feed in a drag pin & peeler scenario.
                if (includedMulti0 && !(isLast || includedPull0)) {
                    actuator.moveTo(feedStartLocation, feedSpeedPull0*baseSpeed);
                }

            }

            head.moveToSafeZ();

            // Make sure we're calibrated after type feed
            assertCalibrated(true);
        } 
        else {
            Logger.debug("Multi parts feed: skipping tape feed at feed count " + feedCount);
        }

        // increment feed count 
        setFeedCount(getFeedCount()+1);
    }

    public void ensureCameraZ(Camera camera, boolean setZ) throws Exception {
        if (camera.isUnitsPerPixelAtZCalibrated()
                && !getLocation().getLengthZ().isInitialized()) {
            throw new Exception("Feeder "+getName()+": Please set the Pick Location Z coordinate first, "
                    + "it is required to determine the true scale of the camera view for accurate computer vision.");
        }
        if (setZ && getLocation().getLengthZ().isInitialized()) {
            // If we already have the Feeder Z, move the camera there to get the right units per pixel.
            camera.moveTo(camera.getLocation().deriveLengths(null, null, getLocation().getLengthZ(), null));
        }
    }

    private void obtainCalibratedVisionOffset() throws Exception {
        Camera camera = getCamera();
        try (CvPipeline pipeline = getCvPipeline(camera, true, false, false)) {
            OcrWrongPartAction ocrAction = OcrWrongPartAction.None;
            boolean ocrStop = false;
            if (visionOffset == null) {
                // this is the very first calibration -> also detect OCR
                ocrAction = getOcrWrongPartAction();
                ocrStop = isOcrStopAfterWrongPart();
            }
            performVisionOperations(camera, pipeline, false, false, true, ocrAction, ocrStop, null);
        }
    }

    @Override
    public String toString() {
        return String.format("%s id %s", getClass().getName(), id);
    }

    @Override
    public void setLocation(Location location) {
        super.setLocation(location);
        resetCalibration();
    }

    public Double getRotationInFeeder() {
        if (rotationInFeeder == null) {
            rotationInFeeder = Double.valueOf(0.0);
        }
        return rotationInFeeder;
    }

    public void setRotationInFeeder(Double rotationInFeeder) {
        Object oldValue = this.rotationInFeeder;
        this.rotationInFeeder = rotationInFeeder;
        firePropertyChange("rotationInFeeder", oldValue, rotationInFeeder);
    }

    public boolean isNormalizePickLocation() {
        return normalizePickLocation;
    }

    public void setNormalizePickLocation(boolean normalizePickLocation) {
        Object oldValue = this.normalizePickLocation;
        this.normalizePickLocation = normalizePickLocation;
        firePropertyChange("normalizePickLocation", oldValue, normalizePickLocation);
    }

    public boolean isSnapToAxis() {
        return snapToAxis;
    }

    public void setSnapToAxis(boolean snapToAxis) {
        Object oldValue = this.snapToAxis;
        this.snapToAxis = snapToAxis;
        firePropertyChange("snapToAxis", oldValue, snapToAxis);
    }

    public Location getHole1Location() {
        return hole1Location;
    }

    public void setHole1Location(Location hole1Location) {
        Object oldValue = this.hole1Location;
        this.hole1Location = hole1Location;
        firePropertyChange("hole1Location", oldValue, hole1Location);
        resetCalibration();
    }

    public Location getHole2Location() {
        return hole2Location;
    }

    public void setHole2Location(Location hole2Location) {
        Object oldValue = this.hole2Location;
        this.hole2Location = hole2Location;
        firePropertyChange("hole2Location", oldValue, hole2Location);
        resetCalibration();
    }

    public boolean isUsedAsTemplate() {
        return usedAsTemplate;
    }

    public void setUsedAsTemplate(boolean usedAsTemplate) {
        Object oldValue = this.usedAsTemplate;
        this.usedAsTemplate = usedAsTemplate;
        firePropertyChange("usedAsTemplate", oldValue, usedAsTemplate);
        // this also changes the status implicitly 
        firePropertyChange("cloneTemplateStatus", "", getCloneTemplateStatus());
    }

    public boolean isCalibrateMotionX() {
        return calibrateMotionX;
    }

    public void setCalibrateMotionX(boolean calibrateMotionX) {
        Object oldValue = this.calibrateMotionX;
        this.calibrateMotionX = calibrateMotionX;
        firePropertyChange("calibrateMotionX", oldValue, calibrateMotionX);
    }

    public boolean isCalibrateMotionY() {
        return calibrateMotionY;
    }

    public void setCalibrateMotionY(boolean calibrateMotionY) {
        Object oldValue = this.calibrateMotionY;
        this.calibrateMotionY = calibrateMotionY;
        firePropertyChange("calibrateMotionY", oldValue, calibrateMotionY);
    }

    public Location getFeedStartLocation() {
        return feedStartLocation;
    }

    public void setFeedStartLocation(Location feedStartLocation) {
        Object oldValue = this.feedStartLocation;
        this.feedStartLocation = feedStartLocation;
        firePropertyChange("feedStartLocation", oldValue, feedStartLocation);
    }

    public Location getFeedMid1Location() {
        return feedMid1Location;
    }

    public void setFeedMid1Location(Location feedMid1Location) {
        Object oldValue = this.feedMid1Location;
        this.feedMid1Location = feedMid1Location;
        firePropertyChange("feedMid1Location", oldValue, feedMid1Location);
    }

    public Location getFeedMid2Location() {
        return feedMid2Location;
    }

    public void setFeedMid2Location(Location feedMid2Location) {
        Object oldValue = this.feedMid2Location;
        this.feedMid2Location = feedMid2Location;
        firePropertyChange("feedMid2Location", oldValue, feedMid2Location);
    }

    public Location getFeedMid3Location() {
        return feedMid3Location;
    }

    public void setFeedMid3Location(Location feedMid3Location) {
        Object oldValue = this.feedMid3Location;
        this.feedMid3Location = feedMid3Location;
        firePropertyChange("feedMid3Location", oldValue, feedMid3Location);
    }

    public Location getFeedEndLocation() {
        return feedEndLocation;
    }

    public void setFeedEndLocation(Location feedEndLocation) {
        Object oldValue = this.feedEndLocation;
        this.feedEndLocation = feedEndLocation;
        firePropertyChange("feedEndLocation", oldValue, feedEndLocation);
    }

    public Length getPartPitch() {
        return partPitch;
    }

    public void setPartPitch(Length partPitch) {
        Object oldValue = this.partPitch;
        this.partPitch = partPitch;
        firePropertyChange("partPitch", oldValue, partPitch);
    }

    public Length getFeedPitch() {
        return feedPitch;
    }

    public void setFeedPitch(Length feedPitch) {
        Object oldValue = this.feedPitch;
        this.feedPitch = feedPitch;
        firePropertyChange("feedPitch", oldValue, feedPitch);
    }

    public double getFeedSpeedPush1() {
        return feedSpeedPush1;
    }

    public void setFeedSpeedPush1(double feedSpeedPush1) {
        Object oldValue = this.feedSpeedPush1;
        this.feedSpeedPush1 = feedSpeedPush1;
        firePropertyChange("feedSpeedPush1", oldValue, feedSpeedPush1);
    }

    public double getFeedSpeedPush2() {
        return feedSpeedPush2;
    }

    public void setFeedSpeedPush2(double feedSpeedPush2) {
        Object oldValue = this.feedSpeedPush2;
        this.feedSpeedPush2 = feedSpeedPush2;
        firePropertyChange("feedSpeedPush2", oldValue, feedSpeedPush2);
    }

    public double getFeedSpeedPush3() {
        return feedSpeedPush3;
    }

    public void setFeedSpeedPush3(double feedSpeedPush3) {
        Object oldValue = this.feedSpeedPush3;
        this.feedSpeedPush3 = feedSpeedPush3;
        firePropertyChange("feedSpeedPush3", oldValue, feedSpeedPush3);
    }

    public double getFeedSpeedPushEnd() {
        return feedSpeedPushEnd;
    }

    public void setFeedSpeedPushEnd(double feedSpeedPushEnd) {
        Object oldValue = this.feedSpeedPushEnd;
        this.feedSpeedPushEnd = feedSpeedPushEnd;
        firePropertyChange("feedSpeedPushEnd", oldValue, feedSpeedPushEnd);
    }

    public double getFeedSpeedPull3() {
        return feedSpeedPull3;
    }

    public void setFeedSpeedPull3(double feedSpeedPull3) {
        Object oldValue = this.feedSpeedPull3;
        this.feedSpeedPull3 = feedSpeedPull3;
        firePropertyChange("feedSpeedPull3", oldValue, feedSpeedPull3);
    }

    public double getFeedSpeedPull2() {
        return feedSpeedPull2;
    }

    public void setFeedSpeedPull2(double feedSpeedPull2) {
        Object oldValue = this.feedSpeedPull2;
        this.feedSpeedPull2 = feedSpeedPull2;
        firePropertyChange("feedSpeedPull2", oldValue, feedSpeedPull2);
    }

    public double getFeedSpeedPull1() {
        return feedSpeedPull1;
    }

    public void setFeedSpeedPull1(double feedSpeedPull1) {
        Object oldValue = this.feedSpeedPull1;
        this.feedSpeedPull1 = feedSpeedPull1;
        firePropertyChange("feedSpeedPull1", oldValue, feedSpeedPull1);
    }

    public double getFeedSpeedPull0() {
        return feedSpeedPull0;
    }

    public void setFeedSpeedPull0(double feedSpeedPull0) {
        Object oldValue = this.feedSpeedPull0;
        this.feedSpeedPull0 = feedSpeedPull0;
        firePropertyChange("feedSpeedPull0", oldValue, feedSpeedPull0);
    }

    public boolean isIncludedPush1() {
        return includedPush1;
    }

    public void setIncludedPush1(boolean includedPush1) {
        Object oldValue = this.includedPush1;
        this.includedPush1 = includedPush1;
        firePropertyChange("includedPush1", oldValue, includedPush1);
    }

    public boolean isIncludedPush2() {
        return includedPush2;
    }

    public void setIncludedPush2(boolean includedPush2) {
        Object oldValue = this.includedPush2;
        this.includedPush2 = includedPush2;
        firePropertyChange("includedPush2", oldValue, includedPush2);
    }

    public boolean isIncludedPush3() {
        return includedPush3;
    }

    public void setIncludedPush3(boolean includedPush3) {
        Object oldValue = this.includedPush3;
        this.includedPush3 = includedPush3;
        firePropertyChange("includedPush3", oldValue, includedPush3);
    }

    public boolean isIncludedPushEnd() {
        return includedPushEnd;
    }

    public void setIncludedPushEnd(boolean includedPushEnd) {
        Object oldValue = this.includedPushEnd;
        this.includedPushEnd = includedPushEnd;
        firePropertyChange("includedPushEnd", oldValue, includedPushEnd);
    }

    public boolean isIncludedMulti0() {
        return includedMulti0;
    }

    public void setIncludedMulti0(boolean includedMulti0) {
        Object oldValue = this.includedMulti0;
        this.includedMulti0 = includedMulti0;
        firePropertyChange("includedMulti0", oldValue, includedMulti0);
    }

    public boolean isIncludedMulti1() {
        return includedMulti1;
    }

    public void setIncludedMulti1(boolean includedMulti1) {
        Object oldValue = this.includedMulti1;
        this.includedMulti1 = includedMulti1;
        firePropertyChange("includedMulti1", oldValue, includedMulti1);
    }

    public boolean isIncludedMulti2() {
        return includedMulti2;
    }

    public void setIncludedMulti2(boolean includedMulti2) {
        Object oldValue = this.includedMulti2;
        this.includedMulti2 = includedMulti2;
        firePropertyChange("includedMulti2", oldValue, includedMulti2);
    }

    public boolean isIncludedMulti3() {
        return includedMulti3;
    }

    public void setIncludedMulti3(boolean includedMulti3) {
        Object oldValue = this.includedMulti3;
        this.includedMulti3 = includedMulti3;
        firePropertyChange("includedMulti3", oldValue, includedMulti3);
    }

    public boolean isIncludedMultiEnd() {
        return includedMultiEnd;
    }

    public void setIncludedMultiEnd(boolean includedMultiEnd) {
        Object oldValue = this.includedMultiEnd;
        this.includedMultiEnd = includedMultiEnd;
        firePropertyChange("includedMultiEnd", oldValue, includedMultiEnd);
    }

    public boolean isIncludedPull0() {
        return includedPull0;
    }

    public void setIncludedPull0(boolean includedPull0) {
        Object oldValue = this.includedPull0;
        this.includedPull0 = includedPull0;
        firePropertyChange("includedPull0", oldValue, includedPull0);
    }

    public boolean isIncludedPull1() {
        return includedPull1;
    }

    public void setIncludedPull1(boolean includedPull1) {
        Object oldValue = this.includedPull1;
        this.includedPull1 = includedPull1;
        firePropertyChange("includedPull1", oldValue, includedPull1);
    }

    public boolean isIncludedPull2() {
        return includedPull2;
    }

    public void setIncludedPull2(boolean includedPull2) {
        Object oldValue = this.includedPull2;
        this.includedPull2 = includedPull2;
        firePropertyChange("includedPull2", oldValue, includedPull2);
    }

    public boolean isIncludedPull3() {
        return includedPull3;
    }

    public void setIncludedPull3(boolean includedPull3) {
        Object oldValue = this.includedPull3;
        this.includedPull3 = includedPull3;
        firePropertyChange("includedPull3", oldValue, includedPull3);
    }

    public boolean isAdditiveRotation() {
        return additiveRotation;
    }

    public void setAdditiveRotation(boolean additiveRotation) {
        Object oldValue = this.additiveRotation;
        this.additiveRotation = additiveRotation;
        firePropertyChange("relativeRotation", oldValue, additiveRotation);
    }

    public Actuator getActuator() {
        return actuator;
    }

    public void setActuator(Actuator actuator) {
        Object oldValue = this.actuator;
        this.actuator = actuator;
        firePropertyChange("actuator", oldValue, actuator);
    }

    public Actuator getActuator2() {
        return actuator2;
    }

    public void setActuator2(Actuator actuator2) {
        Object oldValue = this.actuator2;
        this.actuator2 = actuator2;
        firePropertyChange("actuator2", oldValue, actuator2);
    }

    public long getFeedCount() {
        return feedCount;
    }

    public void setFeedCount(long feedCount) {
        long oldValue = this.feedCount;
        this.feedCount = feedCount;
        firePropertyChange("feedCount", oldValue, feedCount);
    }

    public long getFeedMultiplier() {
        return feedMultiplier;
    }

    public void setFeedMultiplier(long feedMultiplier) {
        Object oldValue = this.feedMultiplier;
        this.feedMultiplier = feedMultiplier;
        firePropertyChange("feedMultiplier", oldValue, feedMultiplier);
    }

    public CalibrationTrigger getCalibrationTrigger() {
        return calibrationTrigger;
    }

    public void setCalibrationTrigger(CalibrationTrigger calibrationTrigger) {
        Object oldValue = this.calibrationTrigger;
        this.calibrationTrigger = calibrationTrigger;
        firePropertyChange("calibrationTrigger", oldValue, calibrationTrigger);
    }

    public String getOcrFontName() {
        return ocrFontName;
    }

    public void setOcrFontName(String ocrFontName) {
        Object oldValue = this.ocrFontName;
        this.ocrFontName = ocrFontName;
        firePropertyChange("ocrFontName", oldValue, ocrFontName);
    }

    public double getOcrFontSizePt() {
        return ocrFontSizePt;
    }

    public void setOcrFontSizePt(double ocrFontSizePt) {
        Object oldValue = this.ocrFontSizePt;
        this.ocrFontSizePt = ocrFontSizePt;
        firePropertyChange("ocrFontSizePt", oldValue, ocrFontSizePt);
    }

    public RegionOfInterest getOcrRegion() {
        return ocrRegion;
    }

    public void setOcrRegion(RegionOfInterest ocrRegion) {
        Object oldValue = this.ocrRegion;
        this.ocrRegion = ocrRegion;
        firePropertyChange("ocrRegion", oldValue, ocrRegion);
    }

    public OcrWrongPartAction getOcrWrongPartAction() {
        return ocrWrongPartAction;
    }

    public void setOcrWrongPartAction(OcrWrongPartAction ocrWrongPartAction) {
        Object oldValue = this.ocrWrongPartAction;
        this.ocrWrongPartAction = ocrWrongPartAction;
        firePropertyChange("ocrWrongPartAction", oldValue, ocrWrongPartAction);
    }

    public boolean isOcrDiscoverOnJobStart() {
        return ocrDiscoverOnJobStart;
    }

    public void setOcrDiscoverOnJobStart(boolean ocrDiscoverOnJobStart) {
        Object oldValue = this.ocrDiscoverOnJobStart;
        this.ocrDiscoverOnJobStart = ocrDiscoverOnJobStart;
        firePropertyChange("ocrDiscoverOnJobStart", oldValue, ocrDiscoverOnJobStart);
    }

    public boolean isOcrStopAfterWrongPart() {
        return ocrStopAfterWrongPart;
    }

    public void setOcrStopAfterWrongPart(boolean ocrStopAfterWrongPart) {
        Object oldValue = this.ocrStopAfterWrongPart;
        this.ocrStopAfterWrongPart = ocrStopAfterWrongPart;
        firePropertyChange("ocrStopAfterWrongPart", oldValue, ocrStopAfterWrongPart);
    }

    public Length getPrecisionWanted() {
        return precisionWanted;
    }

    public void setPrecisionWanted(Length precisionWanted) {
        Object oldValue = this.precisionWanted;
        this.precisionWanted = precisionWanted;
        firePropertyChange("precisionWanted", oldValue, precisionWanted);
    }

    public int getCalibrationCount() {
        return calibrationCount;
    }

    public void setCalibrationCount(int calibrationCount) {
        int oldValue = this.calibrationCount;
        Length oldPrecision = getPrecisionAverage();
        Length oldConfidence = getPrecisionConfidenceLimit();
        this.calibrationCount = calibrationCount;
        firePropertyChange("calibrationCount", oldValue, calibrationCount);
        if (oldValue !=  calibrationCount) {
            // this also implicitly changes the stats
            firePropertyChange("precisionAverage", oldPrecision, getPrecisionAverage());
            firePropertyChange("precisionConfidenceLimit", oldConfidence, getPrecisionConfidenceLimit());
        }
    }

    public Length getSumOfErrors() {
        return sumOfErrors;
    }

    public void addCalibrationError(Length error) {
        sumOfErrors = sumOfErrors.add(error);
        // this is a bit dodgy as the true unit is actually the length unit squared, but we'll use the square root of that later, so it will be fine.
        error = error.convertToUnits(sumOfErrorSquares.getUnits());
        sumOfErrorSquares = sumOfErrorSquares.add(error.multiply(error.getValue()));
        setCalibrationCount(getCalibrationCount()+1); // will also fire average and confidence prop change
    }

    public Length getPrecisionAverage() {
        return calibrationCount > 0 ? 
                sumOfErrors.multiply(1.0/calibrationCount) 
                : new Length(0, LengthUnit.Millimeters);
    }

    public void setPrecisionAverage(Length precisionAverage) {
        // swallow this
    }

    public Length getPrecisionConfidenceLimit() {
        if (calibrationCount >= 2) {
            // Note, we don't take the average of the error, because the error is already a distance that is distributed 
            // around the true sprocket holes center location i.e. distributed around zero i.e. zero is the mean 
            // (this is limited math knowledge speaking).   
            Length variance = sumOfErrorSquares.multiply(1.0/(calibrationCount-1));
            Length scatter = new Length(Math.sqrt(variance.getValue()/Math.sqrt(calibrationCount)), variance.getUnits()); 
            return scatter.multiply(1.64); // 95% confidence interval, normal distribution.
        }
        else {
            return new Length(0, LengthUnit.Millimeters);
        }
    }

    public void setPrecisionConfidenceLimit(Length precisionConfidenceLimit) {
        // swallow this
    }

    public Length getPartsToSprocketHoleDistance() {
        return new Length(getLocation().getLinearDistanceToLineSegment(getHole1Location(), getHole2Location()), 
                getLocation().getUnits());
    }

    public long getPartsPerFeedCycle() {
        return FeederVisionHelper.getPartsPerFeedCycle(getVisionHelperParams(null, null));
    }

    public void resetCalibrationStatistics() {
        sumOfErrors = new Length(0, LengthUnit.Millimeters);
        sumOfErrorSquares = new Length(0, LengthUnit.Millimeters);
        setCalibrationCount(0);
        resetCalibration();
    }

    public Location getPickLocation(long partInCycle, Location visionOffset)  {
        return FeederVisionHelper.getPartLocation(partInCycle, visionOffset, getVisionHelperParams(null, null), getRotationInFeeder());
    } 


    public CvPipeline getPipeline() {
        return pipeline;
    }

    public void setPipeline(CvPipeline pipeline) {
        this.pipeline = pipeline;
    }

    public PipelineType getPipelineType() {
        return pipelineType;
    }

    public void setPipelineType(PipelineType pipelineType) {
        Object oldValue = this.pipelineType;
        this.pipelineType = pipelineType;
        firePropertyChange("pipelineType", oldValue, pipelineType);
    }

    public Location getVisionOffset() {
        if (isVisionEnabled() && visionOffset != null) {
            return visionOffset;
        }
        else {
            return Location.origin;
        }
    }

    public void setVisionOffset(Location visionOffset) {
        this.visionOffset = visionOffset;
    }

    public void resetCalibration() {
        setVisionOffset(null);
    }

    public void resetPipeline(PipelineType type) {
        pipeline = FeederVisionHelper.createDefaultPipeline(type);
        setPipelineType(type);
    }

    public Location getNominalVisionLocation() throws Exception {
        if (!(hole1Location.isInitialized() && hole2Location.isInitialized())) {
            // not yet initialized, just return the current camera location
            return getCamera().getLocation();
        }
        else {
            ensureCameraZ(getCamera(), false);
            return getHole1Location().add(getHole2Location()).multiply(0.5)
                    .deriveLengths(null, null, getLocation().getLengthZ(), 
                            getLocation().getRotation()+getRotationInFeeder());
        }
    }

    public Location getOcrLocation() throws Exception {
        Location offsets = ocrRegion.getOffsets();
        if ( offsets == null || !offsets.isInitialized() ) {
            return getNominalVisionLocation();
        }

        return getNominalVisionLocation().add(offsets);
    }

    protected void setupOcr(Camera camera, CvPipeline pipeline, Location hole1, Location hole2, Location pickLocation) {
        pipeline.setProperty("regionOfInterest", getOcrRegion());
        pipeline.setProperty("SimpleOcr.fontName", getOcrFontName());
        pipeline.setProperty("SimpleOcr.fontSizePt", getOcrFontSizePt());
        pipeline.setProperty("SimpleOcr.alphabet", OcrUtils.getConsolidatedPartsAlphabet(null, "\\"));
    }

    protected void setupOcr(Camera camera, CvPipeline pipeline) {
        setupOcr(camera, pipeline, getHole1Location(), getHole2Location(), getLocation());
    }

    protected void disableOcr(Camera camera, CvPipeline pipeline) {
        pipeline.setProperty("regionOfInterest", null);
        pipeline.setProperty("SimpleOcr.fontName", null);
        pipeline.setProperty("SimpleOcr.fontSizePt", null);
        pipeline.setProperty("SimpleOcr.alphabet", ""); // empty alphabet switches OCR off
    }

    public CvPipeline getCvPipeline(Camera camera, boolean clone, boolean performOcr, boolean autoSetup) {
        try {
            CvPipeline pipeline = getPipeline();
            if (clone) {
                pipeline = pipeline.clone();
            }
            pipeline.setProperty("camera", camera);
            pipeline.setProperty("feeder", this);
            pipeline.setProperty("sprocketHole.diameter", new Length(sprocketHoleDiameterMm, LengthUnit.Millimeters));
            Length range;
            if (autoSetup) { 
                // Auto-Setup: search Range is half camera. 
                Location upp = camera.getUnitsPerPixelAtZ();
                range = camera.getWidth() > camera.getHeight() ? 
                        upp.getLengthY().multiply(camera.getHeight()/2)
                        : upp.getLengthX().multiply(camera.getWidth()/2);
            }
            else {
                // Normal mode: search range is half the distance between the holes plus one pitch. 
                range = getHole1Location().getLinearLengthTo(getHole2Location())
                        .multiply(0.5)
                        .add(new Length(sprocketHolePitchMm, LengthUnit.Millimeters));
            }
            pipeline.setProperty("sprocketHole.maxDistance", range);
            if (performOcr && getOcrRegion() != null) {
                setupOcr(camera, pipeline);
            }
            else {
                disableOcr(camera, pipeline);
            }

            return pipeline;
        }
        catch (CloneNotSupportedException e) {
            throw new Error(e);
        }
    }

    public void showFeatures() throws Exception {
        Camera camera = getCamera();
        ensureCameraZ(camera, true);
        try (CvPipeline pipeline = getCvPipeline(camera, true, true, true)) {

            // Process vision and show feature without applying anything
            FeederVisionHelper feature = new FeederVisionHelper(getVisionHelperParams(camera, pipeline));
            feature.findFeatures(null);
        }
    }

    public void autoSetup() throws Exception {
        Camera camera = getCamera();
        // First preliminary smart clone to get a pipeline from the most suitable template.
        if (!isUsedAsTemplate() && getTemplateFeeder(null) != null) {
            Logger.debug("Auto-Setup: trying with cloned pipeline, template: feeder "+getTemplateFeeder(null).getName());
            smartClone(null, true, false, false, true, true);
        }
        else {
            Logger.debug("Auto-Setup: trying with currently assigned pipeline");
        }
        if (calibrationTrigger == CalibrationTrigger.None) {
            // Just assume the user wants it now 
            setCalibrationTrigger(CalibrationTrigger.UntilConfident);
        }

        ensureCameraZ(camera, true);
        // Try with cloned pipeline.
        Exception e = autoSetupPipeline(camera, null); 
        if (e != null) {
            Logger.debug(e, "Auto-Setup: exception");
            // Failed, try with pipeline type defaults.
            for (PipelineType type : PipelineType.values()) {
                Logger.debug("Auto-Setup: trying with stock pipeline type "+type);
                e = autoSetupPipeline(camera, type);
                if (e == null) {
                    // Success.
                    Logger.debug(e, "Auto-Setup: success");
                    return;
                }
                Logger.debug(e, "Auto-Setup: exception");
            }
            // Still no luck, throw.
            Logger.debug(e, "Auto-Setup: final exception");
            throw e;
        }
    }

    protected Exception autoSetupPipeline(Camera camera, PipelineType type) {
        if (type != null) {
            resetPipeline(type);
        }
        try (CvPipeline pipeline = getCvPipeline(camera, true, true, true)) {
            // Process vision and get some features 
            FeederVisionHelper feature = new FeederVisionHelper(getVisionHelperParams(camera, pipeline));
            feature.findFeatures(FindFeaturesMode.FromPickLocationGetHoles);

            // Store the initial vision based results
            setLocation(feature.getCalibratedPickLocation());
            setHole1Location(feature.getCalibratedHole1Location());
            setHole2Location(feature.getCalibratedHole2Location());
            // Second preliminary smart clone to get pipeline, OCR region etc. from a template,
            // this time with proper transformation. This may again be overwritten if
            // OCR recognizes the proper part.
            if (!isUsedAsTemplate() && getTemplateFeeder(null) != null) {
                Logger.debug("Auto-Setup: secondary clone"+(type == null ? " including pipeline" : " excluding pipeline")
                        +", template feeder: "+getTemplateFeeder(null).getName());
                smartClone(null, true, true, true, true, type == null);
            }
            // As we've changed all this -> reset any stats
            resetCalibrationStatistics();
            try {
                // Now run a sprocket hole calibration, make sure to change the part (not swap it)
                performVisionOperations(camera, pipeline, true, true, false, OcrWrongPartAction.ChangePart, false, null);
            }
            finally {
                // Move the camera back to the pick location, including when there is an exception.
                MovableUtils.moveToLocationAtSafeZ(camera, getLocation());
                MovableUtils.fireTargetedUserAction(camera);
            }
            return null;
        }
        catch (Exception e) {
            return e;
        }
    }

    public List<ReferencePushPullFeeder> getPushPullFeedersFromPool(List<Feeder> pool) {
        // Get all the feeders with the right type.
        List<ReferencePushPullFeeder> list = new ArrayList<>();
        for (Feeder feeder : pool) {
            if (feeder instanceof ReferencePushPullFeeder) {
                ReferencePushPullFeeder pushPullFeeder = (ReferencePushPullFeeder) feeder;
                list.add(pushPullFeeder);
            }
        }
        return list;
    }

    public List<ReferencePushPullFeeder> getAllPushPullFeeders() {
        return getPushPullFeedersFromPool(Configuration.get().getMachine().getFeeders());
    }

    public Length getTapeWidth() {
        // infer the tape width 
        Location hole1Location = FeederVisionHelper.transformMachineToFeederLocation(getHole1Location(), null, this.getVisionHelperParams(null, null))
                .convertToUnits(LengthUnit.Millimeters);
        final double partToSprocketHoleHalfTapeWidthDiffMm = 0.5; // deducted from EIA-481
        double tapeWidth = Math.round(hole1Location.getY()+partToSprocketHoleHalfTapeWidthDiffMm)*2;
        return new Length(tapeWidth, LengthUnit.Millimeters);
    }

    protected org.openpnp.model.Package getPartPackage() {
        if (getPart() != null) {
            return getPart().getPackage();
        }
        return null;
    }

    protected boolean isOnSameRowLocation(ReferencePushPullFeeder feederTemplate) {
        Location delta = getLocation()
                .convertToUnits(LengthUnit.Millimeters)
                .subtract(feederTemplate.getLocation());
        if (Math.abs(delta.getZ()) < rowZLocationToleranceMm) {
            if (Math.abs(delta.getX()) < rowLocationToleranceMm) {
                return true;
            }
            if (Math.abs(delta.getY()) < rowLocationToleranceMm) {
                return true;
            }
        }
        return false;
    }

    /*    protected boolean matchesTapeSettings(ReferencePushPullFeeder other) {
        // the tape/feed geometry must match
        if (!getFeedPitch().equals(other.getFeedPitch())) {
            return false;
        }
        if (!getTapeWidth().equals(other.getTapeWidth())) {
            return false;
        }
        if (!getPartPitch().equals(other.getPartPitch())) {
            return false;
        }
        if (getFeedMultiplier() != other.getFeedMultiplier()) {
            return false;
        }
        double hole1RelativeDistanceMm = transformMachineToFeederLocation(getHole1Location(), null).convertToUnits(LengthUnit.Millimeters)
                .getLinearDistanceTo(other.transformMachineToFeederLocation(other.getHole1Location(), null));
        if (hole1RelativeDistanceMm > sprocketHoleToleranceMm) {
            return false;
        }
        return true;
    }
     */

    static boolean compatiblePartPackages(Part part1, Part part2) {
        if (part1 == null) {
            return false;
        }
        if (part2 == null) {
            return false;
        }

        org.openpnp.model.Package package1 = part1.getPackage();
        org.openpnp.model.Package package2 = part2.getPackage();
        if (package1 == package2) {
            return true;
        }
        if ((package1.getTapeSpecification() != null && !package1.getTapeSpecification().isEmpty())
                && package1.getTapeSpecification().equals(package2.getTapeSpecification())) {
            return true;
        }
        return false;
    }

    public ReferencePushPullFeeder getTemplateFeeder(Part compatiblePart) {
        List<ReferencePushPullFeeder> list = getAllPushPullFeeders();

        Part comparePart = compatiblePart == null ? getPart() : compatiblePart;
        // Rank the feeders by similarity.
        Collections.sort(list, new Comparator<ReferencePushPullFeeder>() {
            @Override
            public int compare(ReferencePushPullFeeder feeder1, ReferencePushPullFeeder feeder2)  {
                int diff; 
                // compatible parts/packages favored
                diff = (compatiblePartPackages(comparePart, feeder1.getPart())?0:1) - (compatiblePartPackages(comparePart, feeder2.getPart())?0:1);
                if (diff != 0) {
                    return diff;
                }
                // template is favored
                diff = (feeder1.isUsedAsTemplate()?0:1) - (feeder2.isUsedAsTemplate()?0:1);
                if (diff != 0) {
                    return diff;
                }
                // same feed pitch is favored
                diff = (getFeedPitch().equals(feeder1.getFeedPitch())?0:1) - (getFeedPitch().equals(feeder2.getFeedPitch())?0:1);
                if (diff != 0) {
                    return diff;
                }
                // same tape width is favored
                diff = (getTapeWidth().equals(feeder1.getTapeWidth())?0:1) - (getTapeWidth().equals(feeder2.getTapeWidth())?0:1);
                if (diff != 0) {
                    return diff;
                }
                // same part pitch is favored
                diff = (getPartPitch().equals(feeder1.getPartPitch())?0:1) - (getPartPitch().equals(feeder2.getPartPitch())?0:1);
                if (diff != 0) {
                    return diff;
                }
                // same row is favored
                diff = (isOnSameRowLocation(feeder1)?0:1) - (isOnSameRowLocation(feeder2)?0:1);
                if (diff != 0) {
                    return diff;
                }
                // enabled is favored
                diff = (feeder1.isEnabled()?0:1) - (feeder2.isEnabled()?0:1);
                if (diff != 0) {
                    return diff;
                }

                // between equally good feeders, take the closer one
                return Double.valueOf(feeder1.getLocation().convertToUnits(LengthUnit.Millimeters).getLinearDistanceTo(getLocation()))
                        .compareTo(feeder2.getLocation().convertToUnits(LengthUnit.Millimeters).getLinearDistanceTo(getLocation()));
            }
        });

        for (ReferencePushPullFeeder templateFeeder : list) {
            if (templateFeeder.getPart() != null && templateFeeder != this) { 
                if (compatiblePart == null || compatiblePartPackages(compatiblePart, templateFeeder.getPart())) {
                    // the first ranking does it  
                    return templateFeeder;
                }
            }
        }
        return null;
    }

    public List<ReferencePushPullFeeder> getCompatibleFeeders() {
        List<ReferencePushPullFeeder> list = new ArrayList<>();
        for (ReferencePushPullFeeder feeder : getAllPushPullFeeders()) {
            if (feeder.getPart() != null && feeder != this) { 
                if (ReferencePushPullFeeder.compatiblePartPackages(getPart(), feeder.getPart())) {
                    list.add(feeder);
                }
            }
        }
        return list;
    }

    public String getCloneTemplateStatus() {
        Part part = getPart();
        String status = "<span>";
        if (isUsedAsTemplate()) {
            status += "Clones to ";
            int n = 0;
            for (ReferencePushPullFeeder targetFeeder : getCompatibleFeeders()) {
                if (targetFeeder.getPart() != null) {
                    if (n++ > 0) {
                        status += ",<br/>";
                    }
                    status += targetFeeder.getName()+" "+targetFeeder.getPart().getId();
                }
            }
            status += n == 0 ? "none." : " (Count: "+n+")";
        }
        else {
            ReferencePushPullFeeder templateFeeder = getTemplateFeeder(null);
            if (templateFeeder != null) {
                status += "<strong>"+templateFeeder.getName()+"</strong>";
                if (templateFeeder.getPart() != null) {
                    status += " with part <strong>"+templateFeeder.getPart().getId()+"</strong>";
                }
                if (part != null 
                        && compatiblePartPackages(part, templateFeeder.getPart())) { 
                    org.openpnp.model.Package package1 = part.getPackage();
                    if (package1 != null) {
                        if (package1.getTapeSpecification() != null && !package1.getTapeSpecification().isEmpty()) {
                            status += " selected by common tape & reel specification <strong>"+package1.getTapeSpecification()+"</strong>";
                        }
                        else {
                            status += " selected by common package <strong>"+package1.getId()+"</strong>";
                        }
                    }
                }
                else {
                    status += " selected as partial match.<br/>"
                            + "<strong style=\"color:red\">Tape & reel specifications/packages are incompatible, settings must be reviewed.</strong>";
                }
            }
            else {
                status += "<strong>None found</strong>";
            }
        }
        status += "</span>";
        return status;
    }

    public void setCloneTemplateStatus(String cloneTemplateStatus) {
        // does nothing
        firePropertyChange("cloneTemplateStatus", cloneTemplateStatus, getCloneTemplateStatus());
    }

    public void smartClone(Part compatiblePart, 
            boolean cloneLocationSettings, boolean cloneTapeSettings, boolean clonePushPullSettings, boolean cloneVisionSettings, boolean clonePipeline) throws Exception {
        // get us the best template feeder
        ReferencePushPullFeeder templateFeeder = getTemplateFeeder(compatiblePart);
        if (templateFeeder == null) {
            if (compatiblePart == null) {
                throw new Exception("Feeder "+getName()+": No suitable template feeder found to clone."); 
            }
            else {
                throw new Exception("Feeder "+getName()+": No template feeder found to clone for part "+compatiblePart.getId()+" compatibility.");
            }
        }
        cloneFeederSettings(cloneLocationSettings, cloneTapeSettings, clonePushPullSettings, cloneVisionSettings, 
                clonePipeline, templateFeeder);
    }

    public void cloneFeederSettings(boolean cloneLocationSettings, boolean cloneTapeSettings, boolean clonePushPullSettings,
            boolean cloneVisionSettings, boolean clonePipeline, ReferencePushPullFeeder templateFeeder)
                    throws CloneNotSupportedException {
        if (cloneLocationSettings) {
            // just the Z from the location
            setLocation(getLocation().derive(templateFeeder.getLocation(), false, false, true, false));
            // options
            setNormalizePickLocation(templateFeeder.isNormalizePickLocation());
            setSnapToAxis(templateFeeder.isSnapToAxis());
        }
        if (cloneTapeSettings) {
            // Tape and feed spec
            setPartPitch(templateFeeder.getPartPitch());
            setRotationInFeeder(templateFeeder.getRotationInFeeder());
            setFeedPitch(templateFeeder.getFeedPitch());
            setFeedMultiplier(templateFeeder.getFeedMultiplier());
            // reset
            setFeedCount(0);
        }
        if (clonePushPullSettings) {
            // clone the actuators
            setActuator(templateFeeder.getActuator());
            setActuator2(templateFeeder.getActuator2());
            // clone all the speeds
            setFeedSpeedPush1(templateFeeder.getFeedSpeedPush1());
            setFeedSpeedPush2(templateFeeder.getFeedSpeedPush2());
            setFeedSpeedPush3(templateFeeder.getFeedSpeedPush3());
            setFeedSpeedPushEnd(templateFeeder.getFeedSpeedPushEnd());
            setFeedSpeedPull3(templateFeeder.getFeedSpeedPull3());
            setFeedSpeedPull2(templateFeeder.getFeedSpeedPull2());
            setFeedSpeedPull1(templateFeeder.getFeedSpeedPull1());
            setFeedSpeedPull0(templateFeeder.getFeedSpeedPull0());
            // clone the switches
            setIncludedPush1(templateFeeder.isIncludedPush1());
            setIncludedPush2(templateFeeder.isIncludedPush2());
            setIncludedPush3(templateFeeder.isIncludedPush3());
            setIncludedPushEnd(templateFeeder.isIncludedPushEnd());
            setIncludedPull3(templateFeeder.isIncludedPull3());
            setIncludedPull2(templateFeeder.isIncludedPull2());
            setIncludedPull1(templateFeeder.isIncludedPull1());
            setIncludedPull0(templateFeeder.isIncludedPull0());
            setIncludedMulti0(templateFeeder.isIncludedMulti0());
            setIncludedMulti1(templateFeeder.isIncludedMulti1());
            setIncludedMulti2(templateFeeder.isIncludedMulti2());
            setIncludedMulti3(templateFeeder.isIncludedMulti3());
            setIncludedMultiEnd(templateFeeder.isIncludedMultiEnd());
        }
        if (cloneVisionSettings) {
            // other settings
            setCalibrationTrigger(templateFeeder.getCalibrationTrigger());
            setPrecisionWanted(templateFeeder.getPrecisionWanted());
            setOcrFontName(templateFeeder.getOcrFontName());
            setOcrFontSizePt(templateFeeder.getOcrFontSizePt());
            setOcrWrongPartAction(templateFeeder.getOcrWrongPartAction());
            setOcrDiscoverOnJobStart(templateFeeder.isOcrDiscoverOnJobStart());
            setOcrStopAfterWrongPart(templateFeeder.isOcrStopAfterWrongPart());
            // reset statistics
            resetCalibration();
            resetCalibrationStatistics();
            setFeedCount(0);
        }
        if (clonePipeline) {
            // clone the pipeline
            setPipeline(templateFeeder.getPipeline().clone());
            setPipelineType(templateFeeder.getPipelineType());
        }
        // now transform over all the locations
        setFeederLocation(FeederVisionHelper.getTransform(null, this.getVisionHelperParams(null, null)), false, true, true, templateFeeder);
    }

    protected static Location relocatedLocation(Location location, Location oldTransform, Location newTransform) {
        if (!location.isInitialized()) {
            // a location with all zeroes is assumed as uninitialized 
            return location;
        }
        else {
            Location feederLocalLocation = FeederVisionHelper.backwardTransform(location, oldTransform);
            location = FeederVisionHelper.forwardTransform(feederLocalLocation, newTransform);
            return location;
        }
    }
    protected static Location relocatedXyLocation(Location location, Location oldTransform, Location newTransform) {
        // disregard Z and Rotation
        return relocatedLocation(location.multiply(1.0, 1.0, 0.0, 0.0), oldTransform, newTransform);
    }
    protected static Location relocatedXyzLocation(Location location, Location oldTransform, Location newTransform) {
        // disregard Rotation
        return relocatedLocation(location.multiply(1.0, 1.0, 1.0, 0.0), oldTransform, newTransform);
    }

    protected void setFeederLocation(Location newTransform, 
            boolean primary, boolean pushPull, boolean vision, 
            ReferencePushPullFeeder templateFeeder) {
        // Relocate the feeder to match the given new transformation.
        Location oldTransform = FeederVisionHelper.getTransform(null, templateFeeder.getVisionHelperParams(null, null));

        if (primary) {
            setHole1Location(relocatedXyLocation(templateFeeder.getHole1Location(), oldTransform, newTransform));
            setHole2Location(relocatedXyLocation(templateFeeder.getHole2Location(), oldTransform, newTransform));
        }
        if (pushPull) {
            if (Math.abs(Math.sin(Math.toRadians(oldTransform.getRotation() - newTransform.getRotation()))) 
                    > Math.sin(Math.toRadians(45))) {
                // Rotated, flip the switches.
                setCalibrateMotionX(templateFeeder.isCalibrateMotionY());
                setCalibrateMotionY(templateFeeder.isCalibrateMotionX());
            }
            else {
                setCalibrateMotionX(templateFeeder.isCalibrateMotionX());
                setCalibrateMotionY(templateFeeder.isCalibrateMotionY());
            }
            setAdditiveRotation(templateFeeder.isAdditiveRotation());
            setFeedStartLocation(relocatedLocation(templateFeeder.getFeedStartLocation(), oldTransform, newTransform));
            setFeedMid1Location(relocatedLocation(templateFeeder.getFeedMid1Location(), oldTransform, newTransform));
            setFeedMid2Location(relocatedLocation(templateFeeder.getFeedMid2Location(), oldTransform, newTransform));
            setFeedMid3Location(relocatedLocation(templateFeeder.getFeedMid3Location(), oldTransform, newTransform));
            setFeedEndLocation(relocatedLocation(templateFeeder.getFeedEndLocation(), oldTransform, newTransform));
        }
        if (vision) {
            if (templateFeeder.getOcrRegion()!= null) {
                setOcrRegion(templateFeeder.getOcrRegion()
                        .rotateXy(newTransform.getRotation()-oldTransform.getRotation()));
            }
        }
        if (primary) {
            // finally and only now set the new pick location (in case some transformations happens in the getters/setters in the future)
            setLocation(relocatedLocation(templateFeeder.getLocation(), oldTransform, newTransform));
        }
    }
    protected void relocateFeeder(Location newTransform) {
        // relocate from itself
        setFeederLocation(newTransform, true, true, true, this);
    }
    protected void swapOutFeeders(ReferencePushPullFeeder other) {
        // Swap out two feeders' locations.
        Location transform1 = FeederVisionHelper.getTransform(null, this.getVisionHelperParams(null, null));
        Location transform2 = FeederVisionHelper.getTransform(null, other.getVisionHelperParams(null, null));
        this.relocateFeeder(transform2);
        other.relocateFeeder(transform1);
    }

    protected void triggerOcrAction(SimpleOcr.OcrModel ocrModel, OcrWrongPartAction ocrAction, boolean ocrStop,
            StringBuilder report) throws Exception {
        if (ocrAction == OcrWrongPartAction.None && ! ocrStop) {
            return; 
        }

        Part ocrPart = OcrUtils.identifyDetectedPart(ocrModel, this);
        Part currentPart = getPart();
        if (currentPart == null) {
            // No part set yet 
            Logger.trace("OCR detected part in feeder "+getId()+", OCR part "+ocrPart.getId());
            setOcrDetectedPart(ocrPart, true);
        }
        else if (ocrPart != null && ocrPart != currentPart) {
            // Wrong part selected in feeder
            Logger.trace("OCR detected wrong part in slot of feeder "+getName()
            +", current part "+currentPart.getId()+" != OCR part "+ocrPart.getId());
            ReferencePushPullFeeder otherFeeder = null;
            for (ReferencePushPullFeeder feeder : getAllPushPullFeeders()) {
                if (feeder.getPart() == ocrPart) {
                    otherFeeder = feeder;
                    Logger.trace("other feeder "+feeder.getName()
                    +" has OCR detected part "+ocrPart.getId());
                    break;
                }
            }
            if (ocrAction == OcrWrongPartAction.SwapFeeders) {
                if (otherFeeder == null) {
                    throw new Exception("OCR detected part "+ocrPart.getId()+" in slot of feeder "+getName()
                    +" is not present in any other feeder. Cannot swap out feeders.");
                }
                swapOutFeeders(otherFeeder);
                otherFeeder.setEnabled(true);
            }
            if (ocrAction == OcrWrongPartAction.SwapOrCreate) {
                if (otherFeeder == null) {
                    // no other feeder has the OCR part -> create a new one
                    Location newLocation =  getLocation();
                    otherFeeder = createNewAtLocation(newLocation, ocrPart, this);
                    if (compatiblePartPackages(ocrPart, currentPart)) {
                        // compatible parts, clone settings from this one
                        otherFeeder.cloneFeederSettings(true, true, true, true, true, this);
                    }
                    else {
                        // incompatible parts, do a smart clone
                        otherFeeder.smartClone(ocrPart, true, true, true, true, true);
                    }
                    // disable this one
                    setEnabled(false);
                    // TODO: the two feeders now sit on top of each other - move this one away?
                }
                else {
                    swapOutFeeders(otherFeeder);
                }
            }
            if (ocrAction == OcrWrongPartAction.ChangePart) {
                setOcrDetectedPart(ocrPart, false);
            }
            else if (ocrAction == OcrWrongPartAction.ChangePartAndClone) {
                setOcrDetectedPart(ocrPart, true);
            }
            if (ocrStop) {
                throw new Exception("OCR detected different part in feeder "+getName()
                +", current part "+currentPart.getId()+" vs. OCR part "+ocrPart.getId()+". Action performed: "+ocrAction.toString()+". Please review.");
            }
            else if (report != null) {
                report.append("<p>Feeder "+getName()
                        +": current part "+currentPart.getId()+", OCR part "+ocrPart.getId()+". Action performed: "+ocrAction.toString()+".</p>");
            }
        }
    }

    public ReferencePushPullFeeder createNewAtLocation(Location newLocation, Part part, ReferencePushPullFeeder templateFeeder)
            throws Exception {
        ReferencePushPullFeeder feeder;
        feeder = new ReferencePushPullFeeder();
        feeder.setPart(part != null ? part : Configuration.get().getParts().get(0));
        feeder.setFeederLocation(newLocation, true, true, true, templateFeeder);
        feeder.setEnabled(isEnabled());
        // add to machine
        Configuration.get().getMachine().addFeeder(feeder);
        return feeder;
    }

    public ReferencePushPullFeeder createNewInRow()
            throws Exception {
        double bestDistanceMm = Double.MAX_VALUE;
        ReferencePushPullFeeder closestFeeder = null;
        for (ReferencePushPullFeeder feeder : getAllPushPullFeeders()) {
            if (feeder != this && feeder.getPart() != null) {
                double distanceMm = feeder.getLocation().convertToUnits(LengthUnit.Millimeters)
                        .getLinearDistanceTo(this.getLocation());
                if (closestFeeder == null 
                        || distanceMm < bestDistanceMm) {
                    bestDistanceMm = distanceMm;
                    closestFeeder = feeder;
                }
            }
        }
        // the default row unit assumption is the 3D printed feeder that was developed together with this feeder class 
        // adding +8mm to the tape width.  A clockwise around the table arrangement is assumed.
        Location rowUnit = FeederVisionHelper.transformFeederToMachineLocation(
                    new Location(LengthUnit.Millimeters,
                0,
                -getTapeWidth().convertToUnits(LengthUnit.Millimeters).getValue() - 8.0, // it is "down" in tape orientation
                        0, 0
                    )
                    , null
                    , this.getVisionHelperParams(null, null)
                )
                .subtract(getLocation());
        // but if we have another feeder, the row unit is properly calculated
        if (closestFeeder != null) {
            rowUnit = getLocation().subtract(closestFeeder.getLocation()).convertToUnits(LengthUnit.Millimeters);
            if (isSnapToAxis()) {
                if (Math.abs(rowUnit.getX()) > rowLocationToleranceMm && Math.abs(rowUnit.getY()) <= rowLocationToleranceMm) {
                    // row along X axis -> snap to it
                    rowUnit = rowUnit.multiply(1.0, 0.0, 0.0, 0.0);
                }
                else if (Math.abs(rowUnit.getY()) > rowLocationToleranceMm && Math.abs(rowUnit.getX()) <= rowLocationToleranceMm) {
                    // row along Y axis -> snap to it
                    rowUnit = rowUnit.multiply(0.0, 1.0, 0.0, 0.0);
                }
                else {
                    throw new Exception("Closest feeder "+closestFeeder.getName()+" "+closestFeeder.getPart().getId()+" does not form a row in X and Y");
                }
            }
        }
        Location newLocation = getLocation().add(rowUnit);
        ReferencePushPullFeeder newFeeder = createNewAtLocation(newLocation, null, this);
        newFeeder.cloneFeederSettings(true, true, true, true, true, this);
        return newFeeder;
    }


    protected void setOcrDetectedPart(Part ocrPart, boolean clone) throws Exception {
        if (isUsedAsTemplate()) {
            if (!compatiblePartPackages(ocrPart, getPart())) {
                throw new Exception("Feeder "+getName()+" is used as a template and can only be OCR-assigned parts with same tape specification or package.");
            }
            setPart(ocrPart);
        }
        else {
            setPart(ocrPart);
            if (clone) {
                smartClone(ocrPart, true, true, true, true, true);
            }
        }
    }

    @Override
    public Location getJobPreparationLocation() {
        if (visionOffset == null 
            && (isOcrDiscoverOnJobStart() || calibrationTrigger != CalibrationTrigger.None)) {
            return getPickLocation(0, null);
        }
        else {
            return null;
        }
    }

    @Override
    public void prepareForJob(boolean visit) throws Exception {
        super.prepareForJob(visit);
        if (visit && visionOffset == null) {
            if (isOcrDiscoverOnJobStart()) {
                // Check the part in the feeder using OCR, this also calibrates the feeder.
                // Note, we cannot change the parts at this point, it is too late in the Job Process, so we always stop.
                performOcr(OcrWrongPartAction.None, true, null);
            }
            else {
                assertCalibrated(false);
            }
        }
    }
    
    public void performOcrOnFeederList(List<ReferencePushPullFeeder> ocrFeederList, 
            OcrWrongPartAction ocrAction, boolean ocrStop, StringBuilder report) throws Exception {
        // Note, we want to be able to swap out feeders' locations while doing the OCR process, so we need 
        // to plan the machine travel by locations rather than by the feeders themselves. We will later search 
        // for the feeder by location.
        List<Location> feederLocationList = new ArrayList<>();
        for (ReferencePushPullFeeder feeder : ocrFeederList) {
            feederLocationList.add(feeder.getPickLocation(0, null).convertToUnits(LengthUnit.Millimeters)); // Btw, convert to mm
        }

        // Use a Travelling Salesman algorithm to optimize the path to all the feeders OCR regions.
        TravellingSalesman<Location> tsm = new TravellingSalesman<>(
                feederLocationList, 
                new TravellingSalesman.Locator<Location>() { 
                    @Override
                    public Location getLocation(Location locatable) {
                        return locatable;
                    }
                }, 
                // start from current location
                getCamera().getLocation(), 
                // no particular end location
                null);

        // Solve it (using the default heuristics).
        tsm.solve();

        // Finally perform the feeders OCR along the travel path.
        for (Location location : tsm.getTravel()) {
            // Search the feeder currently at this location (it might have been swapped out by the OCR)
            for (ReferencePushPullFeeder ocrFeeder : ocrFeederList) {
                if (location.getLinearDistanceTo(ocrFeeder.getPickLocation(0, null)) < calibrationToleranceMm) {
                    ocrFeeder.performOcr(
                            ocrAction != null ? ocrAction : ocrFeeder.getOcrWrongPartAction(),
                                    ocrAction != null ? ocrStop : ocrFeeder.isOcrStopAfterWrongPart(),
                                            report);
                    break;
                }
            }
        }
    }

    public void performOcrOnAllFeeders(OcrWrongPartAction ocrAction, boolean ocrStop, StringBuilder report) throws Exception { 
        // Filter the feeders needing OCR
        List<ReferencePushPullFeeder> ocrFeederList = new ArrayList<>(); 
        for (ReferencePushPullFeeder feeder : getAllPushPullFeeders()) {
            if (feeder.isEnabled() && (feeder.getOcrWrongPartAction() != OcrWrongPartAction.None || isOcrStopAfterWrongPart())) {
                ocrFeederList.add(feeder);
            }
        }
        if (ocrFeederList.size() == 0) {
            throw new Exception("No enabled feeder with OCR found.");
        }
        // Now bulk-OCR. 
        performOcrOnFeederList(ocrFeederList, ocrAction, ocrStop, report);
    }


    public void performOcr(OcrWrongPartAction ocrAction, boolean ocrStop, StringBuilder report) throws Exception {
        if (getOcrRegion() == null) {
            throw new Exception("Feeder "+getName()+" has no OCR region defined.");
        }
        Camera camera = getCamera();
        try (CvPipeline pipeline = getCvPipeline(camera, true, true, false)) {
            // run a sprocket hole calibration, including OCR
            performVisionOperations(camera, pipeline, false, false, true, ocrAction, ocrStop, report); 
        }
    }

    protected void performVisionOperations(Camera camera, CvPipeline pipeline, 
            boolean storeHoles, boolean storePickLocation, boolean storeVisionOffset, OcrWrongPartAction ocrAction, boolean ocrStop,
            StringBuilder report) throws Exception {
        Location runningHole1Location = getHole1Location();
        Location runningHole2Location = getHole2Location();
        Location runningPickLocation = getLocation();
        Location runningVisionOffset = getVisionOffset();
        ensureCameraZ(camera, true);
        
        FeederVisionHelper feature = null;

        final boolean ocrPass = ((ocrAction != OcrWrongPartAction.None || ocrStop) && getOcrRegion() != null);
        Location ocrOffsets = ocrPass ? getOcrRegion().getOffsets() : null;
        final boolean ocrZeroOffset = (ocrOffsets == null || !ocrOffsets.isInitialized());

        if ((ocrPass && ocrZeroOffset) ||  storeHoles || storePickLocation || storeVisionOffset) {
            // Calibrate the exact hole locations by obtaining a mid-point lock on them,
            // assuming that any camera lens and Z parallax distortion is symmetric.
            for (int i = 0; i < calibrateMaxPasses; i++) {
                // move the camera to the mid-point 
                Location midPoint = runningHole1Location.add(runningHole2Location).multiply(0.5, 0.5, 0, 0)
                        .derive(camera.getLocation(), false, false, true, false)
                        .derive(null, null, null, runningPickLocation.getRotation()+getRotationInFeeder());
                Logger.debug("calibrating sprocket holes pass "+ i+ " midPoint is "+midPoint);
                MovableUtils.moveToLocationAtSafeZ(camera, midPoint);
                if(ocrPass && ocrZeroOffset) {
                    setupOcr(camera, pipeline, runningHole1Location, runningHole2Location, runningPickLocation);
                } else {
                    disableOcr(camera, pipeline);
                }

                // this findFeatures should be for FindFeaturesMode.CalibrateHoles but...
                // ...if the calibrationTrigger is set to None, don't perform any calibration and use the already found location, hole1 & hole2 by doing the find with OcrOnly
                // this ^^^ preserves the existing behaviour of ReferencePushPullFeeder -> detailed discussion in PR#1623 (https://github.com/openpnp/openpnp/pull/1623)
                FindFeaturesMode findMode = (calibrationTrigger == CalibrationTrigger.None) ? FindFeaturesMode.OcrOnly : FindFeaturesMode.CalibrateHoles;
                feature = new FeederVisionHelper(getVisionHelperParams(camera, pipeline));
                feature.findFeatures(findMode);

                runningHole1Location = feature.getCalibratedHole1Location();
                runningHole2Location = feature.getCalibratedHole2Location();
                runningPickLocation = feature.getCalibratedPickLocation();

                // calculate the worst pick location delta this gives, cycle part 1 is the worst as it is farthest away
                Location uncalibratedPick1Location = getPickLocation(1, runningVisionOffset);
                Location calibratedPick1Location = getPickLocation(1, feature.getCalibratedVisionOffset());
                Length error = calibratedPick1Location.getLinearLengthTo(uncalibratedPick1Location);
                Logger.trace("new vision offset "+feature.getCalibratedVisionOffset()
                        +" vs. previous vision offset "+runningVisionOffset+" results in error "+error+" at the (farthest) pick location");
                // store data if requested
                if (storeHoles) {
                    setHole1Location(runningHole1Location);
                    setHole2Location(runningHole2Location);
                }
                if (storePickLocation) {
                    setLocation(runningPickLocation);
                }
                if (storeVisionOffset) {
                    // update the stats
                    if (visionOffset != null) {
                        // Only when a previous vision offset has been stored, should we store the error
                        // because the feeder might have been moved physically. The user's actions are 
                        // not part of the calibration error. :-)
                        addCalibrationError(error);
                    }
                    setVisionOffset(feature.getCalibratedVisionOffset());
                }
                // is it good enough? Compare with running offset.
                if (error.convertToUnits(LengthUnit.Millimeters).getValue() < calibrateToleranceMm) {
                    break;
                }
                runningVisionOffset = feature.getCalibratedVisionOffset();
            }
        }
        
        // setup OCR if wanted
        if (ocrPass) {
            //If ROI offset is not null then use the existing capture.
            if (!ocrZeroOffset) {
                Location ocrLocation = getOcrLocation();
                MovableUtils.moveToLocationAtSafeZ(camera, ocrLocation);
                setupOcr(camera, pipeline, runningHole1Location, runningHole2Location, runningPickLocation);
                feature = new FeederVisionHelper(getVisionHelperParams(camera, pipeline));
                feature.findFeatures(FindFeaturesMode.OcrOnly);
            }
            if (feature.getDetectedOcrModel() == null) {
                Logger.warn("Feeder "+getName()+" OCR operation expected, but no \"OCR\" stage result obtained from pipeline.");
            }
            else {
                Logger.trace("got OCR text "+feature.getDetectedOcrModel().getText());
                triggerOcrAction(feature.getDetectedOcrModel(), ocrAction, ocrStop, report);
            }
            disableOcr(camera, pipeline);
        }
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new ReferencePushPullFeederConfigurationWizard(this);
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName() + " " + getName();
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {
                new PropertySheetWizardAdapter(getConfigurationWizard(), "Configuration"),
                new PropertySheetWizardAdapter(new ReferencePushPullMotionConfigurationWizard(this), "Push-Pull Motion"),
        };
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        return null;
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return null;
    }
}
