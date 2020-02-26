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
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
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
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceFeeder;
import org.openpnp.machine.reference.feeder.wizards.ReferenceGestureFeederConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.util.HslColor;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.util.Utils2D;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.FluentCv;
import org.openpnp.vision.Ransac;
import org.openpnp.vision.Ransac.Line;
import org.openpnp.vision.SimpleHistogram;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.CvStage.Result;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Commit;

public class ReferenceGestureFeeder extends ReferenceFeeder {


    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    @Element(required = false)
    protected Location hole1Location = new Location(LengthUnit.Millimeters);
    @Element(required = false)
    protected Location hole2Location = new Location(LengthUnit.Millimeters);

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
    protected double feedSpeedForward1 = 1.0; 
    @Attribute(required = false)
    protected double feedSpeedForward2 = 1.0; 
    @Attribute(required = false)
    protected double feedSpeedForward3 = 1.0; 
    @Attribute(required = false)
    protected double feedSpeedForwardEnd = 1.0;
    @Attribute(required = false)
    protected double feedSpeedBackward3 = 1.0;
    @Attribute(required = false)
    protected double feedSpeedBackward2 = 1.0;
    @Attribute(required = false)
    protected double feedSpeedBackward1 = 1.0;
    @Attribute(required = false)
    protected double feedSpeedBackward0 = 1.0;

    @Attribute(required = false)
    protected boolean includedForward1 = false; 
    @Attribute(required = false)
    protected boolean includedForward2 = false; 
    @Attribute(required = false)
    protected boolean includedForward3 = false; 
    @Attribute(required = false)
    protected boolean includedForwardEnd = true; 

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
    protected boolean includedBackward0 = true; 
    @Attribute(required = false)
    protected boolean includedBackward1 = false; 
    @Attribute(required = false)
    protected boolean includedBackward2 = false; 
    @Attribute(required = false)
    protected boolean includedBackward3 = false; 

    @Attribute(required = false)
    protected String actuatorName;
    @Attribute(required = false)
    protected String peelOffActuatorName;

    @Attribute(required = false)
    private long feedCount = 0;

    @Element(required = false)
    private CvPipeline pipeline = createDefaultPipeline();

    @Attribute(required = false)
    private boolean visionEnabled = true;

    // These are not on the GUI but can be tweaked in the machine.xml
    @Attribute(required = false)
    private double calibrationToleranceMm = 4;
    @Attribute(required = false)
    private double sprocketHoleToleranceMm = 0.3;

    /*
     * visionOffset contains the difference between where the part was expected to be and where it
     * is. Subtracting these offsets from the pickLocation produces the correct pick location.
     */
    protected Location visionOffset;

    static final Location nullLocation = new Location(LengthUnit.Millimeters);

    @Commit
    public void commit() {
    }

    public Camera getCamera() throws Exception {
        return  Configuration.get()
                .getMachine()
                .getDefaultHead()
                .getDefaultCamera();
    }

    public void assertCalibrated() throws Exception {
        if (visionOffset == null || visionOffset.equals(nullLocation)) {
            // not yet calibrated
            if (isVisionEnabled()) {
                MovableUtils.moveToLocationAtSafeZ(getCamera(), getUncalibratedPickLocation(1-getFeedCount()));
                visionOffset = getCalibratedVisionOffset();
                if (visionOffset == null) {
                    // no lock obtained
                    throw new Exception(String.format("Vision failed on feeder %s.", getName()));
                }
            }
            else{
                visionOffset = null;
            }
        }
    }

    @Override
    public Location getPickLocation() throws Exception {
        assertCalibrated();
        return getUncalibratedPickLocation(0)
                .subtractWithRotation(getVisionOffset());
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

        if (getFeedCount() % getPartsPerFeedOperation() == 0) {
            // Modulo of feed count is zero - no more parts there to pick, must feed 

            // Make sure we're calibrated
            assertCalibrated();

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
                if (includedForward1 && (isFirst || includedMulti1)) {
                    actuator.moveTo(feedMid1Location, feedSpeedForward1*baseSpeed);
                }
                if (includedForward2 && (isFirst || includedMulti2)) {
                    actuator.moveTo(feedMid2Location, feedSpeedForward2*baseSpeed);
                }
                if (includedForward3 && (isFirst || includedMulti3)) {
                    actuator.moveTo(feedMid3Location, feedSpeedForward3*baseSpeed);
                }
                if (includedForwardEnd && (isFirst || includedMultiEnd)) {
                    actuator.moveTo(feedEndLocation, feedSpeedForwardEnd*baseSpeed);
                }

                // Start the take up actuator
                if (peelOffActuator != null) {
                    peelOffActuator.actuate(true);
                }

                // Now move back to the start location to move the tape.
                if (includedBackward3 && (isLast || includedMulti3)) {
                    actuator.moveTo(feedMid3Location, feedSpeedBackward3 * baseSpeed);
                }
                if (includedBackward2 && (isLast || includedMulti2)) {
                    actuator.moveTo(feedMid2Location, feedSpeedBackward2 * baseSpeed);
                }
                if (includedBackward1 && (isLast || includedMulti1)) {
                    actuator.moveTo(feedMid1Location, feedSpeedBackward1*baseSpeed);
                }
                if (includedBackward0 && (isLast || includedMulti0)) {
                    actuator.moveTo(feedStartLocation, feedSpeedBackward0*baseSpeed);
                }

                // Stop the take up actuator
                if (peelOffActuator != null) {
                    peelOffActuator.actuate(false);
                }

                // disable actuator
                actuator.actuate(false);
            }

            head.moveToSafeZ();
        } 
        else {
            Logger.debug("Multi parts feed: skipping feed " + feedCount);
        }

        // increment feed count 
        setFeedCount(getFeedCount()+1);
    }

    private Location getCalibratedVisionOffset() throws Exception {
        Camera camera = Configuration.get()
                .getMachine()
                .getDefaultHead()
                .getDefaultCamera();
        try (CvPipeline pipeline = getCvPipeline(camera, true)) {

            // Process vision and show feature without applying anything
            pipeline.process();
            FindFeatures features = new FindFeatures(camera, pipeline, 2000).invoke();
            return features.getCalibratedVisionOffset();
        }
    }

    @Override
    public String toString() {
        return String.format("ReferenceGestureFeeder id %s", id);
    }

    public Location getHole1Location() {
        return hole1Location;
    }

    public void setHole1Location(Location hole1Location) {
        this.hole1Location = hole1Location;
    }

    public Location getHole2Location() {
        return hole2Location;
    }

    public void setHole2Location(Location hole2Location) {
        this.hole2Location = hole2Location;
    }

    public Location getFeedStartLocation() {
        return feedStartLocation;
    }

    public void setFeedStartLocation(Location feedStartLocation) {
        this.feedStartLocation = feedStartLocation;
    }

    public Location getFeedMid1Location() {
        return feedMid1Location;
    }

    public void setFeedMid1Location(Location feedMid1Location) {
        this.feedMid1Location = feedMid1Location;
    }

    public Location getFeedMid2Location() {
        return feedMid2Location;
    }

    public void setFeedMid2Location(Location feedMid2Location) {
        this.feedMid2Location = feedMid2Location;
    }

    public Location getFeedMid3Location() {
        return feedMid3Location;
    }

    public void setFeedMid3Location(Location feedMid3Location) {
        this.feedMid3Location = feedMid3Location;
    }

    public Location getFeedEndLocation() {
        return feedEndLocation;
    }

    public void setFeedEndLocation(Location feedEndLocation) {
        this.feedEndLocation = feedEndLocation;
    }

    public Length getPartPitch() {
        return partPitch;
    }

    public void setPartPitch(Length partPitch) {
        this.partPitch = partPitch;
    }

    public Length getFeedPitch() {
        return feedPitch;
    }


    public void setFeedPitch(Length feedPitch) {
        this.feedPitch = feedPitch;
    }

    public double getFeedSpeedForward1() {
        return feedSpeedForward1;
    }

    public void setFeedSpeedForward1(double feedSpeedForward1) {
        this.feedSpeedForward1 = feedSpeedForward1;
    }

    public double getFeedSpeedForward2() {
        return feedSpeedForward2;
    }


    public void setFeedSpeedForward2(double feedSpeedForward2) {
        this.feedSpeedForward2 = feedSpeedForward2;
    }


    public double getFeedSpeedForward3() {
        return feedSpeedForward3;
    }


    public void setFeedSpeedForward3(double feedSpeedForward3) {
        this.feedSpeedForward3 = feedSpeedForward3;
    }


    public double getFeedSpeedForwardEnd() {
        return feedSpeedForwardEnd;
    }


    public void setFeedSpeedForwardEnd(double feedSpeedForwardEnd) {
        this.feedSpeedForwardEnd = feedSpeedForwardEnd;
    }


    public double getFeedSpeedBackward3() {
        return feedSpeedBackward3;
    }


    public void setFeedSpeedBackward3(double feedSpeedBackward3) {
        this.feedSpeedBackward3 = feedSpeedBackward3;
    }


    public double getFeedSpeedBackward2() {
        return feedSpeedBackward2;
    }


    public void setFeedSpeedBackward2(double feedSpeedBackward2) {
        this.feedSpeedBackward2 = feedSpeedBackward2;
    }


    public double getFeedSpeedBackward1() {
        return feedSpeedBackward1;
    }


    public void setFeedSpeedBackward1(double feedSpeedBackward1) {
        this.feedSpeedBackward1 = feedSpeedBackward1;
    }


    public double getFeedSpeedBackward0() {
        return feedSpeedBackward0;
    }


    public void setFeedSpeedBackward0(double feedSpeedBackward0) {
        this.feedSpeedBackward0 = feedSpeedBackward0;
    }


    public boolean isIncludedForward1() {
        return includedForward1;
    }

    public void setIncludedForward1(boolean includedForward1) {
        this.includedForward1 = includedForward1;
    }

    public boolean isIncludedForward2() {
        return includedForward2;
    }

    public void setIncludedForward2(boolean includedForward2) {
        this.includedForward2 = includedForward2;
    }

    public boolean isIncludedForward3() {
        return includedForward3;
    }

    public void setIncludedForward3(boolean includedForward3) {
        this.includedForward3 = includedForward3;
    }

    public boolean isIncludedForwardEnd() {
        return includedForwardEnd;
    }

    public void setIncludedForwardEnd(boolean includedForwardEnd) {
        this.includedForwardEnd = includedForwardEnd;
    }

    public boolean isIncludedMulti0() {
        return includedMulti0;
    }

    public void setIncludedMulti0(boolean includedMulti0) {
        this.includedMulti0 = includedMulti0;
    }

    public boolean isIncludedMulti1() {
        return includedMulti1;
    }

    public void setIncludedMulti1(boolean includedMulti1) {
        this.includedMulti1 = includedMulti1;
    }

    public boolean isIncludedMulti2() {
        return includedMulti2;
    }

    public void setIncludedMulti2(boolean includedMulti2) {
        this.includedMulti2 = includedMulti2;
    }

    public boolean isIncludedMulti3() {
        return includedMulti3;
    }

    public void setIncludedMulti3(boolean includedMulti3) {
        this.includedMulti3 = includedMulti3;
    }

    public boolean isIncludedMultiEnd() {
        return includedMultiEnd;
    }

    public void setIncludedMultiEnd(boolean includedMultiEnd) {
        this.includedMultiEnd = includedMultiEnd;
    }

    public boolean isIncludedBackward0() {
        return includedBackward0;
    }

    public void setIncludedBackward0(boolean includedBackward0) {
        this.includedBackward0 = includedBackward0;
    }

    public boolean isIncludedBackward1() {
        return includedBackward1;
    }

    public void setIncludedBackward1(boolean includedBackward1) {
        this.includedBackward1 = includedBackward1;
    }

    public boolean isIncludedBackward2() {
        return includedBackward2;
    }

    public void setIncludedBackward2(boolean includedBackward2) {
        this.includedBackward2 = includedBackward2;
    }

    public boolean isIncludedBackward3() {
        return includedBackward3;
    }

    public void setIncludedBackward3(boolean includedBackward3) {
        this.includedBackward3 = includedBackward3;
    }

    public String getActuatorName() {
        return actuatorName;
    }

    public void setActuatorName(String actuatorName) {
        String oldValue = this.actuatorName;
        this.actuatorName = actuatorName;
        propertyChangeSupport.firePropertyChange("actuatorName", oldValue, actuatorName);
    }

    public String getPeelOffActuatorName() {
        return peelOffActuatorName;
    }

    public void setPeelOffActuatorName(String actuatorName) {
        String oldValue = this.peelOffActuatorName;
        this.peelOffActuatorName = actuatorName;
        propertyChangeSupport.firePropertyChange("actuatorName", oldValue, actuatorName);
    }

    public long getFeedCount() {
        return feedCount;
    }

    public void setFeedCount(long feedCount) {
        long oldValue = this.feedCount;
        this.feedCount = feedCount;
        propertyChangeSupport.firePropertyChange("feedCount", oldValue, feedCount);
    }

    public long getFeedMultiplier() {
        return feedMultiplier;
    }

    public void setFeedMultiplier(long feedMultiplier) {
        this.feedMultiplier = feedMultiplier;
    }

    public Length getPartsToSprocketHoleDistance() {
        return new Length(getLocation().getLinearDistanceToLineSegment(getHole1Location(), getHole2Location()), 
                getLocation().getUnits());
    }

    public long getPartsPerFeedOperation() {
        long feedsPerPart = (long)Math.ceil(getPartPitch().divide(getFeedPitch()));
        return Math.round(getFeedMultiplier()*Math.ceil(feedsPerPart*getFeedPitch().divide(getPartPitch())));
    }

    Location transformMachineToFeederLocation(Location location) {
        Location feedUnit = getHole1Location().unitVectorTo(getHole2Location());
        double angle = Math.atan2(feedUnit.getY(), feedUnit.getX());
        // translate back 
        location = location.subtractWithRotation(getLocation());
        // rotate back
        return new Location(location.getUnits(), 
                location.getX()*feedUnit.getX() + location.getY()*feedUnit.getY(),
                location.getX()*feedUnit.getY() - location.getY()*feedUnit.getX(),
                location.getZ(), location.getRotation()-angle);
    }

    Location transformFeederToMachineLocation(Location location) {
        Location feedUnit = getHole1Location().unitVectorTo(getHole2Location());
        double angle = Math.atan2(feedUnit.getY(), feedUnit.getX());
        // rotate
        location = new Location(location.getUnits(), 
                location.getX()*feedUnit.getX() - location.getY()*feedUnit.getY(),
                location.getX()*feedUnit.getY() + location.getY()*feedUnit.getX(),
                location.getZ(), location.getRotation()+angle);
        // translate
        return location
                .addWithRotation(getLocation());
    }

    public Location getUncalibratedPickLocation(long partNumber)  {
        // Calculate the pick location in local feeder coordinates.
        long partInFeed = (getFeedCount()-1+partNumber) % getPartsPerFeedOperation();
        Location feederLocation = new Location(partPitch.getUnits(), partPitch.multiply((double)partInFeed).getValue(), 0, 0, 0);
        Location machineLocation = transformFeederToMachineLocation(feederLocation);
        return machineLocation;
    } 


    public CvPipeline getPipeline() {
        return pipeline;
    }

    public void setPipeline(CvPipeline pipeline) {
        this.pipeline = pipeline;
    }

    public boolean isVisionEnabled() {
        return visionEnabled;
    }

    public void setVisionEnabled(boolean visionEnabled) {
        this.visionEnabled = visionEnabled;
    }

    public Location getVisionOffset() {
        if (isVisionEnabled() && visionOffset != null) {
            return visionOffset;
        }
        else {
            return nullLocation;
        }
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
                    .getResource("ReferenceGestureFeeder-DefaultPipeline.xml"));
            return new CvPipeline(xml);
        }
        catch (Exception e) {
            throw new Error(e);
        }
    }

    public class FindFeatures {
        private Camera camera;
        private CvPipeline pipeline;
        private long showResultMilliseconds;
        private Location calibratedVisionOffset;
        private Location calibratedHole1Location;
        private Location calibratedHole2Location;

        public Location getCalibratedVisionOffset() {
            return calibratedVisionOffset;
        }
        // recognized stuff
        private List<Result.Circle> holes;
        private List<Line> lines;

        public FindFeatures(Camera camera, CvPipeline pipeline, final long showResultMilliseconds) {
            this.camera = camera;
            this.pipeline = pipeline;
            this.showResultMilliseconds = showResultMilliseconds;
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
            Color centerColor = new HslColor(color).getComplementary();
            for (Result.Circle circle : features) {
                org.opencv.core.Point c =  new org.opencv.core.Point(circle.x, circle.y);
                Imgproc.circle(mat, c, (int) (circle.diameter+0.5)/2, FluentCv.colorToScalar(color), 2);
                Imgproc.circle(mat, c, 2, FluentCv.colorToScalar(centerColor), 3);
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
            Size size = Imgproc.getTextSize(String.valueOf(getPartsPerFeedOperation()), 
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
            // go through all the pockets, step-wise 
            for (int i = step; i <= getPartsPerFeedOperation(); i += step) {
                String text = String.valueOf(i);
                Size textSize = Imgproc.getTextSize(text, Core.FONT_HERSHEY_PLAIN, fontScale, 2, baseLine);

                Location partLocation = getUncalibratedPickLocation(i - getFeedCount())
                        .convertToUnits(LengthUnit.Millimeters);
                // go below the pocket
                Location textLocation = transformMachineToFeederLocation(partLocation);
                textLocation = textLocation.add(new Location(LengthUnit.Millimeters, 0., -textSizeMm.getY()*0.25, 0., 0.));
                textLocation = transformFeederToMachineLocation(textLocation).convertToUnits(LengthUnit.Millimeters);
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
                // in accordance with EIA-481 etc. we use millimeters.
                Location mmScale = camera.getUnitsPerPixel().convertToUnits(LengthUnit.Millimeters);
                final double sprocketHoleDiameterMm = 1.5;
                final double sprocketHolePitchMm = 4;
                final double sprocketHoleDiameterPx = sprocketHoleDiameterMm/mmScale.getX();
                final double sprocketHolePitchPx = sprocketHolePitchMm/mmScale.getX();
                final double sprocketHoleTolerancePx = sprocketHoleToleranceMm/mmScale.getX(); 
                // Grab the results
                resultsList = (List) pipeline.getResult(VisionUtils.PIPELINE_RESULTS_NAME).model;

                // Convert into circles
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
                for (Ransac.Line line : ransacLines) {
                    Point a = line.a;
                    Point b = line.b;

                    Location aLocation = VisionUtils.getPixelLocation(camera, a.x, a.y);
                    Location bLocation = VisionUtils.getPixelLocation(camera, b.x, b.y);

                    // Checks the distance to the line.
                    double distanceMm = camera.getLocation().convertToUnits(LengthUnit.Millimeters).getLinearDistanceToLineSegment(aLocation, bLocation);
                    if (distanceMm < calibrationToleranceMm) {
                        // Take the first line that is close enough, as the lines are ordered by length (descending).
                        bestLine = line;
                        bestOrigin = aLocation.add(bLocation).multiply(0.5, 0.5, 0, 0);
                        bestUnitVector = aLocation.unitVectorTo(bLocation);
                        lines.add(bestLine);
                        break;
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

                    // Try to determine the tape position along the best line.
                    SimpleHistogram histogramDistance = new SimpleHistogram(0.05);
                    for (Result.Circle circle : holes) {
                        Location location = VisionUtils.getPixelLocation(camera, circle.x, circle.y)
                                .convertToUnits(LengthUnit.Millimeters)
                                .subtract(bestOrigin);
                        // the distance along the best line is the dot product (perpendicular projection)
                        double distanceMm = location.dotProduct(bestUnitVector).getValue();
                        // calculate the nearest positive modulo distance
                        double position = distanceMm % sprocketHolePitchMm;
                        if (position < 0.) {
                            position += sprocketHolePitchMm;
                        }
                        // Simply record the position in three pitch-modulo bins to support wrap-around with kernel.  
                        histogramDistance.add(position-sprocketHolePitchMm, 1.0);
                        histogramDistance.add(position, 1.0);
                        histogramDistance.add(position+sprocketHolePitchMm, 1.0);
                    }
                    // Deepest notch
                    double holeOriginDistanceMm = histogramDistance.getMaximumKey();
                    // Move the calibrated origin to the notch
                    Location calibratedOrigin = bestOrigin
                            .add(bestUnitVector.multiply(holeOriginDistanceMm, holeOriginDistanceMm, 0, 0));
                    // Calculate the distance to the hole 1, 2 location using the dot product (perpendicular projection)
                    // Round to the nearest sprocket pitch multiple.
                    double calibratedHole1DistanceMm = Math.round(getHole1Location().convertToUnits(LengthUnit.Millimeters)
                            .subtract(calibratedOrigin)
                            .dotProduct(bestUnitVector).getValue()/sprocketHolePitchMm)*sprocketHolePitchMm;
                    double calibratedHole2DistanceMm = Math.round(getHole2Location().convertToUnits(LengthUnit.Millimeters)
                            .subtract(calibratedOrigin)
                            .dotProduct(bestUnitVector).getValue()/sprocketHolePitchMm)*sprocketHolePitchMm;
                    // Using the distance, create new calibrated hole 1, 2 locations. 
                    calibratedHole1Location = calibratedOrigin
                            .add(bestUnitVector.multiply(calibratedHole1DistanceMm, calibratedHole1DistanceMm, 0, 0));
                    calibratedHole2Location = calibratedOrigin
                            .add(bestUnitVector.multiply(calibratedHole2DistanceMm, calibratedHole2DistanceMm, 0, 0));
                    // Calculate the angle offset too.
                    // Note, as Ransac may deliver the line either way, we norm this to +/-90°  
                    Location uncalibratedUnitVector = getHole1Location().unitVectorTo(getHole2Location());
                    double angleOffset = angleNorm(Math.atan2(uncalibratedUnitVector.getY(), uncalibratedUnitVector.getX())
                            - Math.atan2(bestUnitVector.getY(), bestUnitVector.getX()));
                    // Fuse the two to get the calibrated vision offset
                    calibratedVisionOffset = getHole1Location()
                            .subtract(calibratedHole1Location)
                            .derive(null, null, null, angleOffset);

                    // Add tick marks for show
                    Location tick = new Location(LengthUnit.Millimeters, -bestUnitVector.getY(), bestUnitVector.getX(), 0, 0);
                    org.openpnp.model.Point hole1A = VisionUtils.getLocationPixels(camera, calibratedHole1Location.subtract(tick));
                    org.openpnp.model.Point hole1B = VisionUtils.getLocationPixels(camera, calibratedHole1Location.add(tick));
                    lines.add(new Ransac.Line(new Point(hole1A.x, hole1A.y), new Point(hole1B.x, hole1B.y)));
                    org.openpnp.model.Point hole2A = VisionUtils.getLocationPixels(camera, calibratedHole2Location.subtract(tick));
                    org.openpnp.model.Point hole2B = VisionUtils.getLocationPixels(camera, calibratedHole2Location.add(tick));
                    lines.add(new Ransac.Line(new Point(hole2A.x, hole2A.y), new Point(hole2B.x, hole2B.y)));
                }

                if (showResultMilliseconds > 0) {
                    // Draw the result onto the pipeline image.
                    Mat resultMat = pipeline.getWorkingImage().clone();
                    drawHoles(resultMat, getHoles(), Color.green);
                    drawLines(resultMat, getLines(), new Color(0, 0, 128));
                    drawPartNumbers(resultMat, Color.orange);
                    File file = Configuration.get().createResourceFile(getClass(), "reference-gesture-feeder", ".png");
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
    }

    public void showFeatures() throws Exception {
        Camera camera = Configuration.get()
                .getMachine()
                .getDefaultHead()
                .getDefaultCamera();
        try (CvPipeline pipeline = getCvPipeline(camera, true)) {

            // Process vision and show feature without applying anything
            pipeline.process();
            new FindFeatures(camera, pipeline, 2000).invoke();
        }
    }


    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceGestureFeederConfigurationWizard(this);
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
