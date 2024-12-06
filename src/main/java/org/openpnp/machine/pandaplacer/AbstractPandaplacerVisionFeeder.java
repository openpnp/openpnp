/*
 * Copyright (C) 2024 <pandaplacer.ca@gmail.com>
 * based on the ReferencePushPullFeeder
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

package org.openpnp.machine.pandaplacer;

import org.openpnp.ConfigurationListener;
import org.openpnp.machine.reference.FeederWithOptions;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MachineListener;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.FeederVisionHelper;
import org.openpnp.util.FeederVisionHelper.FeederVisionHelperParams;
import org.openpnp.util.FeederVisionHelper.FindFeaturesMode;
import org.openpnp.util.FeederVisionHelper.PipelineType;
import org.openpnp.vision.pipeline.CvPipeline;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

public abstract class AbstractPandaplacerVisionFeeder extends FeederWithOptions {

    // Rotation of the part within the feeder (i.e. within the tape)
    // This is compatible with tonyluken's PR #943 or a similar solution.
    // Conversely, feeder.location.rotation contains the orientation of the feeder itself
    // and it defines the local feeder coordinate system. The rotationInFeeder here can be removed
    // once it is inherited.
    @Attribute(required=false)
    protected Double rotationInFeeder = Double.valueOf(0.0);

    @Attribute(required = false)
    protected boolean normalizePickLocation = true;
    @Attribute(required = false)
    protected boolean snapToAxis = false;

    @Element(required = false)
    protected Location hole1Location = new Location(LengthUnit.Millimeters);
    @Element(required = false)
    protected Location hole2Location = new Location(LengthUnit.Millimeters);

    @Element(required = false)
    private Length partPitch = new Length(4, LengthUnit.Millimeters);
    @Element(required = false)
    private Length feedPitch = new Length(4, LengthUnit.Millimeters);

    @Attribute(required = false)
    private long feedCount = 0;

    @Element(required = false)
    private CvPipeline pipeline = FeederVisionHelper.createDefaultPipeline(PipelineType.CircularSymmetry);
    @Attribute(required = false)
    protected PipelineType pipelineType = PipelineType.CircularSymmetry;

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
    protected double calibrationToleranceMm = 1.95;
    // vision and comparison sprocket hole tolerance (in size, position)
    @Attribute(required = false)
    protected double sprocketHoleToleranceMm = 0.6;

    @Attribute(required = false)
    protected int calibrateMaxPasses = 3;
    // how close the camera has to be to prevent one more pass
    @Attribute(required = false)
    protected double calibrateToleranceMm = 0.3;
    @Attribute(required = false)
    protected int calibrateMinStatistic = 2;

    // Some EIA 481 standard constants.
    static final double sprocketHoleDiameterMm = 1.5;
    static final double sprocketHolePitchMm = 4;

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


    public void checkHomedState(Machine machine) {
        if (!machine.isHomed()) {
            this.resetCalibration();
        }
    }

    public AbstractPandaplacerVisionFeeder() {
        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            @Override
            public void configurationComplete(Configuration configuration) throws Exception {
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

    private FeederVisionHelperParams getVisionHelperParams(Camera camera, CvPipeline pipeline) {
        return new FeederVisionHelperParams(camera, this.pipelineType, pipeline, 2000
            , normalizePickLocation, snapToAxis
            , partPitch, feedPitch, 1 //multiplier is fixed to 1, not used in Pandaplacer feeders
            , location, hole1Location, hole2Location
            , calibrationToleranceMm, sprocketHoleToleranceMm);
    }

    public Camera getCamera() throws Exception {
        return  Configuration.get()
                .getMachine()
                .getDefaultHead()
                .getDefaultCamera();
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

//    // Inherited from ReferenceFeeder. Actual feeders will implement this
//    @Override
//    public void feed(Nozzle nozzle) throws Exception {
//    }

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

    protected void obtainCalibratedVisionOffset() throws Exception {
        Camera camera = getCamera();
        try (CvPipeline pipeline = getCvPipeline(camera, true, false)) {
            performVisionOperations(camera, pipeline, false, false, true);
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

    public long getFeedCount() {
        return feedCount;
    }

    public void setFeedCount(long feedCount) {
        long oldValue = this.feedCount;
        this.feedCount = feedCount;
        firePropertyChange("feedCount", oldValue, feedCount);
    }

    public CalibrationTrigger getCalibrationTrigger() {
        return calibrationTrigger;
    }

    public void setCalibrationTrigger(CalibrationTrigger calibrationTrigger) {
        Object oldValue = this.calibrationTrigger;
        this.calibrationTrigger = calibrationTrigger;
        firePropertyChange("calibrationTrigger", oldValue, calibrationTrigger);
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

    public CvPipeline getCvPipeline(Camera camera, boolean clone, boolean autoSetup) {
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
                // Auto-Setup: search range is set to be full camera resolution (bigger dimension is used)
                // to be able to detect sprocket holes at the edge of the image. Search range defines circle's
                // radius with origin in center. Full resolution is used as radius to cover image corners.
                Location upp = camera.getUnitsPerPixelAtZ();
                range = camera.getWidth() > camera.getHeight() ?
                        upp.getLengthX().multiply(camera.getHeight())
                        : upp.getLengthY().multiply(camera.getWidth());
            }
            else {
                // Normal mode: search range is half the distance between the holes plus one pitch.
                range = getHole1Location().getLinearLengthTo(getHole2Location())
                        .multiply(0.5)
                        .add(new Length(sprocketHolePitchMm, LengthUnit.Millimeters));
            }
            pipeline.setProperty("sprocketHole.maxDistance", range);

            return pipeline;
        }
        catch (CloneNotSupportedException e) {
            throw new Error(e);
        }
    }

    public void showFeatures() throws Exception {
        Camera camera = getCamera();
        ensureCameraZ(camera, true);
        camera.moveTo(this.getLocation()); //make sure the camera is pointing to the currently selected pick location
        try (CvPipeline pipeline = getCvPipeline(camera, true, true)) {

            // Process vision and show feature without applying anything
            FeederVisionHelper tape = new FeederVisionHelper(getVisionHelperParams(camera, pipeline));
            tape.findFeatures(null);
        }
    }

    public void autoSetup() throws Exception {
        Camera camera = getCamera();
        if (calibrationTrigger == CalibrationTrigger.None) {
            // Just assume the user wants it now
            setCalibrationTrigger(CalibrationTrigger.UntilConfident);
        }

        ensureCameraZ(camera, true);
        // Try with the current pipeline.
        Exception e = autoSetupPipeline(camera, this.pipelineType);
        if (e != null) {
            // No luck, throw.
            Logger.debug(e, "Auto-Setup: final exception");
            throw e;
        }
    }

    protected Exception autoSetupPipeline(Camera camera, PipelineType type) {
        try (CvPipeline pipeline = getCvPipeline(camera, true, true)) {
            // Process vision and get some features
            FeederVisionHelper feature = new FeederVisionHelper(getVisionHelperParams(camera, pipeline))
                    .findFeatures(FindFeaturesMode.FromPickLocationGetHoles);
            // Store the initial vision based results
            setLocation(feature.getCalibratedPickLocation());
            setHole1Location(feature.getCalibratedHole1Location());
            setHole2Location(feature.getCalibratedHole2Location());
            // As we've changed all this -> reset any stats
            resetCalibrationStatistics();
            try {
                // Now run a sprocket hole calibration
                performVisionOperations(camera, pipeline, true, true, false);
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

    @Override
    public Location getJobPreparationLocation() {
        if (visionOffset == null
            && (calibrationTrigger != CalibrationTrigger.None)) {
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
            if (calibrationTrigger != CalibrationTrigger.None) {
                // Calibrate the feeder.
              performSprocketCalibration();
            }
            else {
                assertCalibrated(false);
            }
        }
    }


    public void performSprocketCalibration() throws Exception {
        Camera camera = getCamera();
        try (CvPipeline pipeline = getCvPipeline(camera, true, false)) {
            // run a sprocket hole calibration
            performVisionOperations(camera, pipeline, false, false, true);
        }
    }


    protected void performVisionOperations(Camera camera, CvPipeline pipeline,
            boolean storeHoles, boolean storePickLocation, boolean storeVisionOffset) throws Exception {
        Location runningHole1Location = getHole1Location();
        Location runningHole2Location = getHole2Location();
        Location runningPickLocation = getLocation();
        Location runningVisionOffset = getVisionOffset();
        ensureCameraZ(camera, true);

        FeederVisionHelper feature = null;

        if (storeHoles || storePickLocation || storeVisionOffset) {
            // Calibrate the exact hole locations by obtaining a mid-point lock on them,
            // assuming that any camera lens and Z parallax distortion is symmetric.
            for (int i = 0; i < calibrateMaxPasses; i++) {
                // move the camera to the mid-point
                Location midPoint = runningHole1Location.add(runningHole2Location).multiply(0.5, 0.5, 0, 0)
                        .derive(camera.getLocation(), false, false, true, false)
                        .derive(null, null, null, runningPickLocation.getRotation()+getRotationInFeeder());
                Logger.debug("calibrating sprocket holes pass "+ i+ " midPoint is "+midPoint);
                MovableUtils.moveToLocationAtSafeZ(camera, midPoint);
                feature = new FeederVisionHelper(getVisionHelperParams(camera, pipeline))
                        .findFeatures(FindFeaturesMode.CalibrateHoles);
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
    }

}
