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

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.Action;

import org.apache.commons.io.IOUtils;
import org.opencv.core.Core;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.openpnp.ConfigurationListener;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceFeeder;
import org.openpnp.machine.reference.feeder.wizards.ReferencePushPullFeederConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MachineListener;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.FluentCv;
import org.openpnp.vision.Ransac;
import org.openpnp.vision.Ransac.Line;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.CvStage.Result;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Commit;

public class ReferencePushPullFeeder extends ReferenceFeeder {

    // Rotation of the part within the feeder (i.e. within the tape)
    // This is compatible with tonyluken's pending PR #943 or a similar solution.
    // Conversely, feeder.location.rotation contains the orientation of the feeder itself
    // and it defines the local feeder coordinate system. The rotationInFeeder here can be removed
    // once it is inherited. 
    @Attribute(required=false)
    protected Double rotationInFeeder = new Double(0.0);

    @Attribute(required = false)
    protected boolean normalizePickLocation = true;

    @Element(required = false)
    protected Location hole1Location = new Location(LengthUnit.Millimeters);
    @Element(required = false)
    protected Location hole2Location = new Location(LengthUnit.Millimeters);

    @Attribute(required = false)
    protected boolean preferredAsTemplate = false; 

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
    protected String actuatorName;
    @Attribute(required = false)
    protected String peelOffActuatorName;

    @Attribute(required = false)
    private long feedCount = 0;

    @Element(required = false)
    private CvPipeline pipeline = createDefaultPipeline();

    @Element(required = false)
    private Length precisionWanted = new Length(0.1, LengthUnit.Millimeters);

    @Attribute(required = false)
    private int calibrationCount = 0;
    @Element(required = false)
    private Length sumOfErrors = new Length(0, LengthUnit.Millimeters);
    @Element(required = false)
    private Length sumOfErrorSquares = new Length(0, LengthUnit.Millimeters);


    // These are not on the GUI but can be tweaked in the machine.xml /////////////////

    @Attribute(required = false)
    private double calibrationToleranceMm = 1.95;
    @Attribute(required = false)
    private double sprocketHoleToleranceMm = 0.3;
    @Attribute(required = false)
    private double rowLocationToleranceMm = 4.0; 
    @Attribute(required = false)
    private double rowZLocationToleranceMm = 1.0; 

    @Attribute(required = false)
    private int calibrateMaxPasses = 3; 
    @Attribute(required = false)
    private double calibrateToleranceMm = 0.3; // how close the camera has to be to the end result
    @Attribute(required = false)
    private int calibrateMinStatistic = 2; 

    /*
     * visionOffset contains the difference between where the part was expected to be and where it
     * is. Subtracting these offsets from the pickLocation produces the correct pick location.
     */
    protected Location visionOffset;

    static final Location nullLocation = new Location(LengthUnit.Millimeters);

    private void checkHomedState(Machine machine) {
        if (!machine.isHomed()) {
            this.resetCalibration();
        }
    }

    public ReferencePushPullFeeder() {
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

    @Commit
    public void commit() {
    }

    public Camera getCamera() throws Exception {
        return  Configuration.get()
                .getMachine()
                .getDefaultHead()
                .getDefaultCamera();
    }


    public enum CalibrationTrigger {
        None,
        OnFirstUse,
        UntilConfident,
        OnEachTapeFeed
    }

    @Attribute(required = false)
    protected CalibrationTrigger calibrationTrigger = CalibrationTrigger.UntilConfident;

    public void assertCalibrated(boolean tapeFeed) throws Exception {
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
        assertCalibrated(false);
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

        if (actuatorName == null || actuatorName.isEmpty()) {
            throw new Exception(String.format("No actuator name set on feeder %s.", getName()));
        }


        Head head = nozzle.getHead();
        Actuator actuator = head.getActuatorByName(actuatorName);
        if (actuator == null) {
            throw new Exception(String.format("No Actuator found with name %s on feed Head %s",
                    actuatorName, head.getName()));
        }

        Actuator peelOffActuator = head.getActuatorByName(peelOffActuatorName);

        if (getFeedCount() % getPartsPerFeedCycle() == 0) {
            // Modulo of feed count is zero - no more parts there to pick, must feed 

            // Make sure we're calibrated
            assertCalibrated(false);

            // Create the effective feed locations, keeping the Rotation intact and applying the vision offset.
            Location feedStartLocation = getFeedStartLocation()
                    .derive(actuator.getLocation(), false, false, false, true)
                    .subtractWithRotation(getVisionOffset());
            Location feedMid1Location = getFeedMid1Location()
                    .derive(actuator.getLocation(), false, false, false, true)
                    .subtractWithRotation(getVisionOffset());
            Location feedMid2Location = getFeedMid2Location()
                    .derive(actuator.getLocation(), false, false, false, true)
                    .subtractWithRotation(getVisionOffset());
            Location feedMid3Location = getFeedMid3Location()
                    .derive(actuator.getLocation(), false, false, false, true)
                    .subtractWithRotation(getVisionOffset());
            Location feedEndLocation = getFeedEndLocation()
                    .derive(actuator.getLocation(), false, false, false, true)
                    .subtractWithRotation(getVisionOffset());

            // Move to the Feed Start Location
            MovableUtils.moveToLocationAtSafeZ(actuator, feedStartLocation);
            double baseSpeed = actuator.getHead().getMachine().getSpeed();

            long feedsPerPart = (long)Math.ceil(getPartPitch().divide(getFeedPitch()));
            long n = getFeedMultiplier()*feedsPerPart;
            for (long i = 0; i < n; i++) {  // perform multiple feeds if required

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
                if (peelOffActuator != null) {
                    peelOffActuator.actuate(true);
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
                if (peelOffActuator != null) {
                    peelOffActuator.actuate(false);
                }

                // disable actuator
                actuator.actuate(false);
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

    private void obtainCalibratedVisionOffset() throws Exception {
        Camera camera = getCamera();
        try (CvPipeline pipeline = getCvPipeline(camera, true)) {
            calibrateSprocketHoles(camera, pipeline, false, false, true);
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
            rotationInFeeder = new Double(0.0);
        }
        return rotationInFeeder;
    }

    public void setRotationInFeeder(Double rotationInFeeder) {
        this.rotationInFeeder = rotationInFeeder;
    }

    public boolean isNormalizePickLocation() {
        return normalizePickLocation;
    }

    public void setNormalizePickLocation(boolean normalizePickLocation) {
        this.normalizePickLocation = normalizePickLocation;
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

    public boolean isPreferredAsTemplate() {
        return preferredAsTemplate;
    }

    public void setPreferredAsTemplate(boolean preferredAsTemplate) {
        this.preferredAsTemplate = preferredAsTemplate;
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

    public String getActuatorName() {
        return actuatorName;
    }

    public void setActuatorName(String actuatorName) {
        String oldValue = this.actuatorName;
        this.actuatorName = actuatorName;
        firePropertyChange("actuatorName", oldValue, actuatorName);
    }

    public String getPeelOffActuatorName() {
        return peelOffActuatorName;
    }

    public void setPeelOffActuatorName(String actuatorName) {
        String oldValue = this.peelOffActuatorName;
        this.peelOffActuatorName = actuatorName;
        firePropertyChange("peelOffActuatorName", oldValue, actuatorName);
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

    public Length getPartsToSprocketHoleDistance() {
        return new Length(getLocation().getLinearDistanceToLineSegment(getHole1Location(), getHole2Location()), 
                getLocation().getUnits());
    }

    public long getPartsPerFeedCycle() {
        long feedsPerPart = (long)Math.ceil(getPartPitch().divide(getFeedPitch()));
        return Math.round(getFeedMultiplier()*Math.ceil(feedsPerPart*getFeedPitch().divide(getPartPitch())));
    }

    public void resetCalibrationStatistics() {
        sumOfErrors = new Length(0, LengthUnit.Millimeters);
        sumOfErrorSquares = new Length(0, LengthUnit.Millimeters);
        setCalibrationCount(0);
        resetCalibration();
    }

    public Location forwardTransform(Location location, Location transform) {
        return location.rotateXy(transform.getRotation()).addWithRotation(transform);
    }

    public Location backwardTransform(Location location, Location transform) {
        return location.subtractWithRotation(transform).rotateXy(-transform.getRotation());
    }

    protected Location getTransform(Location visionOffset) {
        // Our local feeder coordinate system is relative to the EIA 481 standard tape orientation
        // i.e. with the sprocket holes on top and the tape advancing to the right, which is our +X
        // The pick location is on [0, 0] local, which corresponds to feeder.location global.
        // The feeder.location.rotation contains the orientation of the tape on the machine.
        
        // to make sure we get the right rotation, we update it from the sprocket holes
        // instead of trusting the location.rotation. This might happen when the user fiddles 
        // with the locations manually.
       
        Location unitVector = getHole1Location().unitVectorTo(getHole2Location());
        double rotationTape = Math.atan2(unitVector.getY(), unitVector.getX())*180.0/Math.PI;
        Location transform = getLocation().derive(null, null, null, rotationTape);
        if (Math.abs(rotationTape - getLocation().getRotation()) > 0.1) {
            // HACK: something is not up-to-date -> refresh
            setLocation(transform);
        }
        
        if (visionOffset != null) {
            transform = transform.subtractWithRotation(visionOffset);
        }
        return transform;
    }

    protected Location transformFeederToMachineLocation(Location feederLocation, Location visionOffset) {
        return forwardTransform(feederLocation, getTransform(visionOffset));
    }

    protected Location transformMachineToFeederLocation(Location machineLocation, Location visionOffset) {
        return backwardTransform(machineLocation, getTransform(visionOffset));
    }

    public Location getPickLocation(long partInCycle, Location visionOffset)  {
        // If the feeder is advancing more than one part per feed cycle (e.g. with 2mm pitch tape or if a multiplier is
        // given), we need to cycle through multiple pick locations. partInCycle is 1-based and goes to getPartsPerFeedCycle().
        long offsetPitches = (getPartsPerFeedCycle() - partInCycle) % getPartsPerFeedCycle();
        Location feederLocation = new Location(partPitch.getUnits(), partPitch.multiply((double)offsetPitches).getValue(), 
                0, 0, getRotationInFeeder());
        Location machineLocation = transformFeederToMachineLocation(feederLocation, visionOffset);
        return machineLocation;
    } 


    public CvPipeline getPipeline() {
        return pipeline;
    }

    public void setPipeline(CvPipeline pipeline) {
        this.pipeline = pipeline;
    }

    public Location getVisionOffset() {
        if (isVisionEnabled() && visionOffset != null) {
            return visionOffset;
        }
        else {
            return nullLocation;
        }
    }

    public void setVisionOffset(Location visionOffset) {
        this.visionOffset = visionOffset;
    }


    public void resetCalibration() {
        setVisionOffset(null);
    }

    public void resetPipeline() {
        pipeline = createDefaultPipeline();
    }

    public CvPipeline getCvPipeline(Camera camera, boolean clone) {
        try {
            CvPipeline pipeline = getPipeline();
            if (clone) {
                pipeline = pipeline.clone();
            }
            pipeline.setProperty("camera", camera);
            pipeline.setProperty("feeder", this);

            return pipeline;
        }
        catch (CloneNotSupportedException e) {
            throw new Error(e);
        }
    }

    private static CvPipeline createDefaultPipeline() {
        try {
            String xml = IOUtils.toString(BlindsFeeder.class
                    .getResource("ReferencePushPullFeeder-DefaultPipeline.xml"));
            return new CvPipeline(xml);
        }
        catch (Exception e) {
            throw new Error(e);
        }
    }

    public enum AutoSetupMode {
        FromPickLocationGetHoles,
        CalibrateHoles
    }

    public class FindFeatures {
        private Camera camera;
        private CvPipeline pipeline;
        private long showResultMilliseconds;
        private AutoSetupMode autoSetupMode;
        private Location calibratedVisionOffset;
        private Location calibratedHole1Location;
        private Location calibratedHole2Location;
        private Location calibratedPickLocation;


        public Location getCalibratedVisionOffset() {
            return calibratedVisionOffset;
        }
        // recognized stuff
        private List<Result.Circle> holes;
        private List<Line> lines;

        public FindFeatures(Camera camera, CvPipeline pipeline, final long showResultMilliseconds, AutoSetupMode autoSetupMode) {
            this.camera = camera;
            this.pipeline = pipeline;
            this.showResultMilliseconds = showResultMilliseconds;
            this.autoSetupMode = autoSetupMode;
        }

        public List<Result.Circle> getHoles() {
            return holes;
        }
        public List<Line> getLines() {
            return lines;
        }

        private void drawHoles(Mat mat, List<Result.Circle> features, Color color) {
            if (features == null || features.isEmpty()) {
                return;
            }
            for (Result.Circle circle : features) {
                org.opencv.core.Point c =  new org.opencv.core.Point(circle.x, circle.y);
                Imgproc.circle(mat, c, (int) (circle.diameter+0.5)/2, FluentCv.colorToScalar(color), 2);
                Imgproc.circle(mat, c, 2, FluentCv.colorToScalar(color), 3);
            }
        }

        private void drawLines(Mat mat, List<Line> lines, Color color) {
            if (lines == null || lines.isEmpty()) {
                return;
            }
            for (Line line : lines) {
                Imgproc.line(mat, line.a, line.b, FluentCv.colorToScalar(color), 2);
            }
        }

        // number the parts in the pockets
        private void drawPartNumbers(Mat mat, Color color) {
            // make sure the numbers are not too dense
            int [] baseLine = null;
            double feederPocketPitchMm =  getPartPitch().convertToUnits(LengthUnit.Millimeters).getValue();
            if (feederPocketPitchMm < 1.) {
                // feeder not set up yet
                return;
            }

            // calculate the diagonal text size
            double fontScale = 1.0;
            Size size = Imgproc.getTextSize(String.valueOf(getPartsPerFeedCycle()), 
                    Core.FONT_HERSHEY_PLAIN, fontScale, 2, baseLine);
            Location textSizeMm = camera.getUnitsPerPixel().multiply(size.width, size.height, 0., 0.)
                    .convertToUnits(LengthUnit.Millimeters);
            if (textSizeMm.getY() < 0.0) {
                textSizeMm = textSizeMm.multiply(1.0, -1.0, 0.0, 0.0);
            }
            final double minFontSizeMm = 0.6;
            if (textSizeMm.getY() < minFontSizeMm) {
                fontScale = minFontSizeMm / textSizeMm.getY();
                textSizeMm = textSizeMm.multiply(fontScale, fontScale, 0.0, 0.0);
            }
            double textSizePitchCount = textSizeMm.getLinearDistanceTo(nullLocation)/feederPocketPitchMm;
            int step;
            if (textSizePitchCount < 0.75) {
                step = 1;
            }
            else if (textSizePitchCount < 1.5) {
                step = 2;
            }
            else if (textSizePitchCount < 4) {
                step = 5;
            }
            else {
                // something must be wrong - feeder probably not set up correctly (yet)
                return;
            }
            // go through all the parts, step-wise 
            for (int i = step; i <= getPartsPerFeedCycle(); i += step) {
                String text = String.valueOf(i);
                Size textSize = Imgproc.getTextSize(text, Core.FONT_HERSHEY_PLAIN, fontScale, 2, baseLine);

                Location partLocation = getPickLocation(i, calibratedVisionOffset)
                        .convertToUnits(LengthUnit.Millimeters);
                // TODO: go besides part
                Location textLocation = transformMachineToFeederLocation(partLocation, calibratedVisionOffset);
                textLocation = textLocation.add(new Location(LengthUnit.Millimeters, 0., -textSizeMm.getY()*0.25, 0., 0.));
                textLocation = transformFeederToMachineLocation(textLocation, calibratedVisionOffset)
                        .convertToUnits(LengthUnit.Millimeters);
                org.openpnp.model.Point p = VisionUtils.getLocationPixels(camera, textLocation);
                if (p.x > 0 && p.x < camera.getWidth() && p.y > 0 && p.y < camera.getHeight()) {
                    // roughly in the visible range - draw it
                    // determine the alignment based on where the text is located in relation to the pocket
                    double dx = textLocation.getX() - partLocation.getX();
                    double dy = textLocation.getY() - partLocation.getY();
                    // the alignment, in relation to the lower left corner of the text
                    double alignX, alignY;
                    if (Math.abs(dx) > Math.abs(dy)) {
                        // more horizontal displacement 
                        if (dx < 0) {
                            // to the left
                            alignX = -textSize.width;
                            alignY = textSize.height/2;
                        }
                        else {
                            // to the right
                            alignX = 0.;
                            alignY = textSize.height/2;
                        }
                    }
                    else {
                        // more vertical displacement
                        if (dy > 0) {
                            // above
                            alignX = -textSize.width/2;
                            alignY = 0.0;
                        }
                        else {
                            // below
                            alignX = -textSize.width/2;
                            alignY = textSize.height;
                        }
                    }
                    Imgproc.putText(mat, text, 
                            new org.opencv.core.Point(p.x + alignX, p.y + alignY), 
                            Core.FONT_HERSHEY_PLAIN, 
                            fontScale, 
                            FluentCv.colorToScalar(color), 2, 0, false);
                }
            }
        }


        public FindFeatures invoke() throws Exception {
            List resultsList = null; 
            try {
                // in accordance with EIA-481 etc. we use all millimeters.
                Location mmScale = camera.getUnitsPerPixel().convertToUnits(LengthUnit.Millimeters);
                final double sprocketHoleDiameterMm = 1.5;
                final double sprocketHolePitchMm = 4;
                final double partPitchMinMm = 2;
                final double sprocketHoleToPartMinMm = 3.5; // sprocket hole to part @ 8mm
                final double sprocketHoleToPartGridMm = 2;  // +multiples of 2mm for wider tapes 
                final double sprocketHoleDiameterPx = sprocketHoleDiameterMm/mmScale.getX();
                final double sprocketHolePitchPx = sprocketHolePitchMm/mmScale.getX();
                final double sprocketHoleTolerancePx = sprocketHoleToleranceMm/mmScale.getX(); 
                // Grab the results
                resultsList = (List) pipeline.getResult(VisionUtils.PIPELINE_RESULTS_NAME).model;

                // Convert eligible results into circles
                List<CvStage.Result.Circle> results = new ArrayList<>();;
                for (Object result : resultsList) {
                    if ((result) instanceof Result.Circle) {
                        Result.Circle circle = ((Result.Circle) result);
                        if (Math.abs(circle.diameter*mmScale.getX() - sprocketHoleDiameterMm) < sprocketHoleToleranceMm) {
                            results.add(circle);
                        }
                    }
                    else if ((result) instanceof RotatedRect) {
                        RotatedRect rect = ((RotatedRect) result);
                        double diameter = (rect.size.width+rect.size.height)/2.0;
                        if (Math.abs(rect.size.width*mmScale.getX() - sprocketHoleDiameterMm) < sprocketHoleToleranceMm
                                && Math.abs(rect.size.height*mmScale.getX() - sprocketHoleDiameterMm) < sprocketHoleToleranceMm) {
                            results.add(new Result.Circle(rect.center.x, rect.center.y, diameter));
                        }
                    }
                    else if ((result) instanceof KeyPoint) {
                        KeyPoint keyPoint = ((KeyPoint) result);
                        results.add(new Result.Circle(keyPoint.pt.x, keyPoint.pt.y, sprocketHoleDiameterPx));
                    }
                }


                // reset the features
                holes = new ArrayList<>();
                lines = new ArrayList<>();

                // collect the circles into a list of points
                List<Point> points = new ArrayList<>();
                for (Result.Circle circle : results) {
                    points.add(new Point(circle.x, circle.y));
                }
                List<Ransac.Line> ransacLines = Ransac.ransac(points, 100, sprocketHoleTolerancePx, sprocketHolePitchPx, sprocketHoleTolerancePx);
                // Get the best line within the calibration tolerance
                Ransac.Line bestLine = null;
                Location bestOrigin = null;
                Location bestUnitVector = null;
                double bestDistanceMm = Double.MAX_VALUE;
                for (Ransac.Line line : ransacLines) {
                    Point a = line.a;
                    Point b = line.b;

                    Location aLocation = VisionUtils.getPixelLocation(camera, a.x, a.y);
                    Location bLocation = VisionUtils.getPixelLocation(camera, b.x, b.y);

                    // Checks the distance to the line.
                    double distanceMm = camera.getLocation().convertToUnits(LengthUnit.Millimeters).getLinearDistanceToLineSegment(aLocation, bLocation);
                    if (distanceMm < (autoSetupMode == AutoSetupMode.CalibrateHoles ? calibrationToleranceMm : bestDistanceMm)) {
                        // Take the first line that is close enough, as the lines are ordered by length (descending).
                        // In autoSetupMode take the closest line.
                        bestLine = line;
                        bestOrigin = aLocation.add(bLocation).multiply(0.5, 0.5, 0, 0);
                        bestUnitVector = aLocation.unitVectorTo(bLocation);
                        bestDistanceMm = distanceMm;
                        lines.add(bestLine);
                        break;
                    }
                }

                if (autoSetupMode != null) {
                    if (bestLine == null) {
                        throw new Exception("No line of sprocket holes can be recognized"); 
                    }
                }
                if (bestLine != null) {
                    // Filter the circles by distance from the resulting line
                    for (Result.Circle circle : results) {
                        Point p = new Point(circle.x, circle.y);
                        if (FluentCv.pointToLineDistance(bestLine.a, bestLine.b, p) <= sprocketHoleTolerancePx) {
                            holes.add(circle);
                        }
                    }

                    // Sort holes by distance from camera center.
                    Collections.sort(holes, new Comparator<Result.Circle>() {
                        @Override
                        public int compare(Result.Circle o1, Result.Circle o2) {
                            double d1 = VisionUtils.getPixelLocation(camera, o1.x, o1.y).getLinearDistanceTo(camera.getLocation());
                            double d2 = VisionUtils.getPixelLocation(camera, o2.x, o2.y).getLinearDistanceTo(camera.getLocation());
                            return Double.compare(d1, d2);
                        }
                    });

                    if (autoSetupMode  == AutoSetupMode.FromPickLocationGetHoles) {
                        // because we sorted the holes by distance, the first two are our holes 1 and 2
                        if (holes.size() < 2) {
                            throw new Exception("At least two sprocket holes need to be recognized"); 
                        }
                        calibratedHole1Location = VisionUtils.getPixelLocation(camera, holes.get(0).x, holes.get(0).y)
                                .convertToUnits(LengthUnit.Millimeters);
                        calibratedHole2Location = VisionUtils.getPixelLocation(camera, holes.get(1).x, holes.get(1).y)
                                .convertToUnits(LengthUnit.Millimeters);
                        Location partLocation = camera.getLocation().convertToUnits(LengthUnit.Millimeters);
                        double angle1 = Math.atan2(calibratedHole1Location.getY()-partLocation.getY(), calibratedHole1Location.getX()-partLocation.getX());
                        double angle2 = Math.atan2(calibratedHole2Location.getY()-partLocation.getY(), calibratedHole2Location.getX()-partLocation.getX());
                        double angleDiff = angleNorm180((angle2-angle1)*180/Math.PI);
                        if (angleDiff > 0) {
                            // The holes 1 and 2 must appear counter-clockwise from the part location, swap them! 
                            Location swap = calibratedHole2Location;
                            calibratedHole2Location = calibratedHole1Location;
                            calibratedHole1Location = swap;
                        }
                        if (calibratedHole1Location.unitVectorTo(calibratedHole2Location)
                                .dotProduct(bestUnitVector).getValue() < 0.0) {
                            // turn the unite vector around
                            bestUnitVector = bestUnitVector.multiply(-1.0, -1.0, 0, 0);
                        }
                        // determine the correct transformation
                        double angleTape = Math.atan2(bestUnitVector.getY(), bestUnitVector.getX())*180.0/Math.PI;
                        // preliminary pick location
                        calibratedPickLocation = camera.getLocation()
                                .derive(getLocation(), false, false, true, false) // previous Z
                                .derive(null,  null, null, angleTape); // preliminary feeeder orientation
                    }
                    else {
                        // find the two holes matching 
                        for (Result.Circle hole : holes) {
                            Location l = VisionUtils.getPixelLocation(camera, hole.x, hole.y)
                                    .convertToUnits(LengthUnit.Millimeters);
                            double dist1Mm = l.getLinearDistanceTo(getHole1Location()); 
                            double dist2Mm = l.getLinearDistanceTo(getHole2Location()); 
                            if (dist1Mm < calibrationToleranceMm && dist1Mm < dist2Mm) {
                                calibratedHole1Location = l;
                            }
                            else if (dist2Mm < calibrationToleranceMm && dist2Mm < dist1Mm) {
                                calibratedHole2Location = l;
                            }
                        }
                        if (calibratedHole1Location == null || calibratedHole2Location == null) {
                            if (autoSetupMode  == AutoSetupMode.CalibrateHoles) {
                                throw new Exception("The two reference sprocket holes cannot be recognized"); 
                            }
                        }
                        else {
                            if (calibratedHole1Location.unitVectorTo(calibratedHole2Location)
                                    .dotProduct(bestUnitVector).getValue() < 0.0) {
                                // turn the unite vector around
                                bestUnitVector = bestUnitVector.multiply(-1.0, -1.0, 0, 0);
                            }
                            // determine the correct transformation
                            double angleTape = Math.atan2(bestUnitVector.getY(), bestUnitVector.getX())*180.0/Math.PI;
                            // the new calibration target is really the mid-point
                            Location midPoint = calibratedHole1Location.add(calibratedHole2Location).multiply(0.5, 0.5, 0, 0);
                            // but let's project that back to the real hole positions with nominal pitch (undistorted by the camera lens and Z parallax)
                            double distanceHolesMm = Math.round(calibratedHole1Location.getLinearDistanceTo(calibratedHole2Location)
                                    /sprocketHolePitchMm)*sprocketHolePitchMm;
                            calibratedHole1Location = midPoint.subtract(bestUnitVector.multiply(distanceHolesMm*0.5, distanceHolesMm*0.5, 0, 0));
                            calibratedHole2Location = midPoint.add(bestUnitVector.multiply(distanceHolesMm*0.5, distanceHolesMm*0.5, 0, 0));
                            Logger.trace("["+getClass().getName()+"] calibrated hole locations are: " + calibratedHole1Location + ", " +calibratedHole2Location);
                            if (autoSetupMode  == AutoSetupMode.CalibrateHoles) {
                                // get the current pick location relative to hole 1
                                Location pickLocation = getLocation().convertToUnits(LengthUnit.Millimeters);
                                Location relativePickLocation = pickLocation
                                        .subtract(getHole1Location());
                                // rotate from old angle 
                                relativePickLocation =  relativePickLocation.rotateXy(-pickLocation.getRotation())
                                        .derive(null, null, null, 0.0);
                                // normalize to a nominal local pick location according to EIA 481
                                if (normalizePickLocation) {
                                    relativePickLocation = new Location(LengthUnit.Millimeters,
                                            Math.round(relativePickLocation.getX()/partPitchMinMm)*partPitchMinMm,
                                            -sprocketHoleToPartMinMm+Math.round((relativePickLocation.getY()+sprocketHoleToPartMinMm)/sprocketHoleToPartGridMm)*sprocketHoleToPartGridMm,
                                            0, 0);
                                }
                                // calculate the new pick location with the new hole 1 location and tape angle 
                                calibratedPickLocation = calibratedHole1Location.add(relativePickLocation.rotateXy(angleTape))
                                        .derive(null, null, pickLocation.getZ(), angleTape);
                            }
                        }
                    }

                    if (calibratedHole1Location != null && calibratedPickLocation != null) {
                        // we have our calibrated locations
                        // Get the calibrated vision offset (with Z always 0)
                        calibratedVisionOffset = getLocation()
                                .subtractWithRotation(calibratedPickLocation)
                                .derive(null, null, 0.0, null);
                        Logger.debug("["+getClass().getName()+"] calibrated vision offset is: " + calibratedVisionOffset + ", length is: "+calibratedVisionOffset.getLinearLengthTo(nullLocation));

                        // Add tick marks for show
                        if (calibratedPickLocation != null) {
                            org.openpnp.model.Point a;
                            org.openpnp.model.Point b;
                            Location tick = new Location(LengthUnit.Millimeters, -bestUnitVector.getY(), bestUnitVector.getX(), 0, 0);
                            a = VisionUtils.getLocationPixels(camera, calibratedPickLocation.subtract(tick));
                            b = VisionUtils.getLocationPixels(camera, calibratedPickLocation.add(tick));
                            lines.add(new Ransac.Line(new Point(a.x, a.y), new Point(b.x, b.y)));
                            a = VisionUtils.getLocationPixels(camera, calibratedPickLocation.subtract(bestUnitVector));
                            b = VisionUtils.getLocationPixels(camera, calibratedPickLocation.add(bestUnitVector));
                            lines.add(new Ransac.Line(new Point(a.x, a.y), new Point(b.x, b.y)));
                            Logger.debug("["+getClass().getName()+"] calibrated pick location is: " + calibratedPickLocation);
                        }
                    }
                }

                if (showResultMilliseconds > 0) {
                    // Draw the result onto the pipeline image.
                    Mat resultMat = pipeline.getWorkingImage().clone();
                    drawHoles(resultMat, getHoles(), Color.green);
                    drawLines(resultMat, getLines(), new Color(0, 0, 255));
                    drawPartNumbers(resultMat, Color.orange);
                    File file = Configuration.get().createResourceFile(getClass(), "push-pull-feeder", ".png");
                    Imgcodecs.imwrite(file.getAbsolutePath(), resultMat);
                    BufferedImage showResult = OpenCvUtils.toBufferedImage(resultMat);
                    resultMat.release();
                    MainFrame.get().getCameraViews().getCameraView(camera)
                    .showFilteredImage(showResult, showResultMilliseconds);
                }
            }
            catch (ClassCastException e) {
                throw new Exception("Unrecognized result type (should be Result.Circle, RotatedRect, KeyPoint): " + resultsList);
            }
            return this;
        }

        private double angleNorm(double angle) {
            while (angle > 90) {
                angle -= 180;
            }
            while (angle < -90) {
                angle += 180;
            }
            return angle;
        }
        private double angleNorm180(double angle) {
            while (angle > 180) {
                angle -= 360;
            }
            while (angle < -180) {
                angle += 360;
            }
            return angle;
        }
    }

    public void showFeatures() throws Exception {
        Camera camera = getCamera();
        try (CvPipeline pipeline = getCvPipeline(camera, true)) {

            // Process vision and show feature without applying anything
            pipeline.process();
            new FindFeatures(camera, pipeline, 2000, null).invoke();
        }
    }

    public void autoSetup() throws Exception {
        Camera camera = getCamera();
        try (CvPipeline pipeline = getCvPipeline(camera, true)) {
            // Process vision and get some features 
            pipeline.process();
            FindFeatures feature = new FindFeatures(camera, pipeline, 2000, AutoSetupMode.FromPickLocationGetHoles)
                    .invoke();
            // Start with the camera position - but keep any Z value already set
            setLocation(feature.calibratedPickLocation);
            // as we changed this -> reset any stats
            resetCalibrationStatistics();
            // Store the initial vision based sprocket hole finds
            setHole1Location(feature.calibratedHole1Location);
            setHole2Location(feature.calibratedHole2Location);
            // now run a hole calibration
            calibrateSprocketHoles(camera, pipeline, true, true, false);
            // move the camera over the pick location
            MovableUtils.moveToLocationAtSafeZ(camera, getLocation());
        }
    }

    protected List<ReferencePushPullFeeder> getFeedersOnSameRow() {
        // Get all the feeders connected by location.
        List<ReferencePushPullFeeder> list = new ArrayList<>();
        for (Feeder feeder : Configuration.get().getMachine().getFeeders()) {
            if (feeder instanceof ReferencePushPullFeeder) {
                ReferencePushPullFeeder pushPullFeeder = (ReferencePushPullFeeder) feeder;
                list.add(pushPullFeeder);
            }
        }

        return list;
    }



    public Length getTapeWidth() {
        Location hole1Location = transformMachineToFeederLocation(getHole1Location(), null)
                .convertToUnits(LengthUnit.Millimeters);
        final double partToSprocketHoleHalfTapeWidthDiffMm = 0.5; // deducted from EIA-481
        double tapeWidth = Math.round(hole1Location.getY()+partToSprocketHoleHalfTapeWidthDiffMm)*2;
        return new Length(tapeWidth, LengthUnit.Millimeters);
    }

    protected boolean isSameTapeWidth(ReferencePushPullFeeder feederTemplate) {
        return getTapeWidth().equals(feederTemplate.getTapeWidth());
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



    protected ReferencePushPullFeeder getTemplateFeeder() {
        List<ReferencePushPullFeeder> list = getFeedersOnSameRow();

        // Rank the feeders by similarity.
        Collections.sort(list, new Comparator<ReferencePushPullFeeder>() {
            @Override
            public int compare(ReferencePushPullFeeder feeder1, ReferencePushPullFeeder feeder2)  {
                int diff; 
                // same feed pitch is favored
                diff = (getFeedPitch().equals(feeder1.getFeedPitch())?0:1) - (getFeedPitch().equals(getFeedPitch())?0:1);
                if (diff != 0) {
                    return diff;
                }
                // same tape width is favored
                diff = (getTapeWidth().equals(feeder1.getTapeWidth())?0:1) - (getTapeWidth().equals(feeder2.getTapeWidth())?0:1);
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
                // doesn't matter much but if it is available, the same part pitch is favored
                diff = (getPartPitch().equals(feeder1.getPartPitch())?0:1) - (getPartPitch().equals(getPartPitch())?0:1);
                if (diff != 0) {
                    return diff;
                }

                // between equally good feeders, take the closer one
                return new Double(feeder1.getLocation().convertToUnits(LengthUnit.Millimeters).getLinearDistanceTo(getLocation()))
                        .compareTo(feeder2.getLocation().convertToUnits(LengthUnit.Millimeters).getLinearDistanceTo(getLocation()));
            }
        });

        for (ReferencePushPullFeeder feederTemplate : list) {
            if (feederTemplate != this) { 
                // the first ranking does it  
                return feederTemplate;
            }
        }
        return null;
    }
    
    private Location cloneIfDefined(ReferencePushPullFeeder feederTemplate, Location location) {
        if (location.equals(nullLocation)) {
            return nullLocation;
        }
        else {
            Location feederLocation = feederTemplate.transformMachineToFeederLocation(location, null);
            return transformFeederToMachineLocation(feederLocation, null);
        }
    }
    
    public void smartClone() throws Exception {
        // get us a juicy template feeder
        ReferencePushPullFeeder feederTemplate = getTemplateFeeder();
        if (feederTemplate == null) {
            throw new Exception("No suitable template feeder found to clone."); 
        }
        // clone the actuators
        setActuatorName(feederTemplate.getActuatorName());
        setPeelOffActuatorName(feederTemplate.getPeelOffActuatorName());
        // translate all the locations
        setFeedStartLocation(cloneIfDefined(feederTemplate, feederTemplate.getFeedStartLocation()));
        setFeedMid1Location(cloneIfDefined(feederTemplate, feederTemplate.getFeedMid1Location())); 
        setFeedMid2Location(cloneIfDefined(feederTemplate, feederTemplate.getFeedMid2Location())); 
        setFeedMid3Location(cloneIfDefined(feederTemplate, feederTemplate.getFeedMid3Location())); 
        setFeedEndLocation(cloneIfDefined(feederTemplate, feederTemplate.getFeedEndLocation())); 
        // clone all the speeds
        setFeedSpeedPush1(feederTemplate.getFeedSpeedPush1());
        setFeedSpeedPush2(feederTemplate.getFeedSpeedPush2());
        setFeedSpeedPush3(feederTemplate.getFeedSpeedPush3());
        setFeedSpeedPushEnd(feederTemplate.getFeedSpeedPushEnd());
        setFeedSpeedPull3(feederTemplate.getFeedSpeedPull3());
        setFeedSpeedPull2(feederTemplate.getFeedSpeedPull2());
        setFeedSpeedPull1(feederTemplate.getFeedSpeedPull1());
        setFeedSpeedPull0(feederTemplate.getFeedSpeedPull0());
        // clone the switches
        setIncludedPush1(feederTemplate.isIncludedPush1());
        setIncludedPush2(feederTemplate.isIncludedPush2());
        setIncludedPush3(feederTemplate.isIncludedPush3());
        setIncludedPushEnd(feederTemplate.isIncludedPushEnd());
        setIncludedPull3(feederTemplate.isIncludedPull3());
        setIncludedPull2(feederTemplate.isIncludedPull2());
        setIncludedPull1(feederTemplate.isIncludedPull1());
        setIncludedPull0(feederTemplate.isIncludedPull0());
        setIncludedMulti0(feederTemplate.isIncludedMulti0());
        setIncludedMulti1(feederTemplate.isIncludedMulti1());
        setIncludedMulti2(feederTemplate.isIncludedMulti2());
        setIncludedMulti3(feederTemplate.isIncludedMulti3());
        setIncludedMultiEnd(feederTemplate.isIncludedMultiEnd());
        // clone the pipeline
        setPipeline(feederTemplate.getPipeline().clone());
        // other settings
        setPrecisionWanted(feederTemplate.getPrecisionWanted());
        setCalibrationTrigger(feederTemplate.getCalibrationTrigger());
        // reset
        resetCalibration();
        resetCalibrationStatistics();
        setFeedCount(0);
    }

    protected void calibrateSprocketHoles(Camera camera, CvPipeline pipeline, 
            boolean storeHoles, boolean storePickLocation, boolean storeVisionOffset) throws Exception {
        Location runningHole1Location = getHole1Location();
        Location runningHole2Location = getHole2Location();
        Location runningVisionOffset = getVisionOffset();
        // Calibrate the exact hole locations by obtaining a mid-point lock on them,
        // assuming that any camera lens and Z parallax distortion is symmetric.
        for (int i = 0; i < calibrateMaxPasses; i++) {
            // move the camera to the mid-point 
            Location midPoint = runningHole1Location.add(runningHole2Location).multiply(0.5, 0.5, 0, 0)
                    .derive(camera.getLocation(), false, false, true, false)
                    .derive(getLocation(), false, false, false, true);
            Logger.debug("["+getClass().getName()+"] calibrating sprocket holes pass "+ i+ " midPoint is "+midPoint);
            MovableUtils.moveToLocationAtSafeZ(camera, midPoint);
            // take a new shot
            pipeline.process();
            FindFeatures feature = new FindFeatures(camera, pipeline, 2000, AutoSetupMode.CalibrateHoles).invoke();
            runningHole1Location = feature.calibratedHole1Location;
            runningHole2Location = feature.calibratedHole2Location;
            // calculate the worst pick location delta this gives, cycle part 1 is the worst as it is farthest away
            Location uncalibratedPickLocation = getPickLocation(1, runningVisionOffset);
            Location calibratedPickLocation = getPickLocation(1, feature.calibratedVisionOffset);
            Length error = calibratedPickLocation.getLinearLengthTo(uncalibratedPickLocation);
            Logger.trace("["+getClass().getName()+"] new vision offset "+feature.calibratedVisionOffset
                    +" vs. previous vision offset "+runningVisionOffset+" results in error "+error+" at the pick location");
            // store data if requested
            if (storeHoles) {
                setHole1Location(runningHole1Location);
                setHole2Location(runningHole2Location);
            }
            if (storePickLocation) {
                setLocation(feature.calibratedPickLocation);
            }
            if (storeVisionOffset) {
                // update the stats
                if (visionOffset != null) {
                    // only when a previous vision offset has been stored, should we store the error
                    addCalibrationError(error);
                }
                setVisionOffset(feature.calibratedVisionOffset);
            }
            // is it good enough? Compare with running offset.
            if (error.convertToUnits(LengthUnit.Millimeters).getValue() < calibrateToleranceMm) {
                break;
            }
            runningVisionOffset = feature.calibratedVisionOffset;
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
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        return null;
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return null;
    }
}
