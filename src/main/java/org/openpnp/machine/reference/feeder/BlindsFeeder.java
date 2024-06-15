/*
 * Copyright (C) 2019-2020 <mark@makr.zone>
 * based on the ReferenceStripFeeder 
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
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.Action;

import org.apache.commons.io.IOUtils;
import org.opencv.core.Mat;
import org.opencv.core.RotatedRect;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.openpnp.ConfigurationListener;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceFeeder;
import org.openpnp.machine.reference.feeder.wizards.BlindsFeederArrayConfigurationWizard;
import org.openpnp.machine.reference.feeder.wizards.BlindsFeederConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.model.Point;
import org.openpnp.model.RegionOfInterest;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MachineListener;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.util.HslColor;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.OcrUtils;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.util.TravellingSalesman;
import org.openpnp.util.Utils2D;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.FluentCv;
import org.openpnp.vision.Ransac.Line;
import org.openpnp.vision.SimpleHistogram;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage.Result;
import org.openpnp.vision.pipeline.stages.SimpleOcr;
import org.openpnp.vision.pipeline.stages.SimpleOcr.OcrModel;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;


/**
 * Implementation of Feeder that indexes through an array of cut tape strips held
 * by a 3D printed base. Each tape lane has a blinds style cover that can open/close by 
 * shifting half the pocket pitch. This only works for tapes where the pockets use up 
 * less than half the pocket pitch as is usually the case with punched paper carrier tapes 
 * but also for some plastic/embossed carrier tapes.  
 */
public class BlindsFeeder extends ReferenceFeeder {

    @Element(required = false)
    private Location fiducial1Location = new Location(LengthUnit.Millimeters);

    @Element(required = false)
    private Location fiducial2Location = new Location(LengthUnit.Millimeters);

    @Element(required = false)
    private Location fiducial3Location = new Location(LengthUnit.Millimeters);

    @Attribute(required = false)
    private boolean normalize = true;

    @Element(required = false)
    private Length tapeLength = new Length(0, LengthUnit.Millimeters);

    @Element(required = false)
    private Length feederExtent = new Length(0, LengthUnit.Millimeters);

    @Element(required = false)
    private Length pocketCenterline = new Length(0, LengthUnit.Millimeters);

    @Element(required = false)
    private Length pocketPitch = new Length(0, LengthUnit.Millimeters);

    @Element(required = false)
    private Length pocketSize = new Length(0, LengthUnit.Millimeters);

    @Element(required = false)
    private CvPipeline pipeline = createDefaultPipeline();

    @Attribute(required = false)
    private boolean visionEnabled = true;

    @Attribute
    private int feedCount = 0;

    @Attribute(required = false)
    private int feederNo = 0;

    @Attribute(required = false)
    private int feedersTotal = 0;

    @Attribute(required = false)
    private String feederGroupName = defaultGroupName;

    @Attribute(required = false)
    private int pocketCount = 0;

    @Attribute(required = false)
    private int firstPocket = 1;

    @Attribute(required = false)
    private int lastPocket = 0;

    @Element(required = false)
    private Length sprocketPitch = new Length(4, LengthUnit.Millimeters);

    @Element(required = false)
    private Length edgeOpenDistance = new Length(2, LengthUnit.Millimeters); 

    @Element(required = false)
    private Length edgeClosedDistance = new Length(2, LengthUnit.Millimeters); 

    // we have 1mm push edge standard and the smallest CP40 nozzle tip has 0.8mm usable tip shaft
    @Element(required = false)
    private Length pushZOffset = new Length(0.25, LengthUnit.Millimeters); 

    @Attribute(required = false)
    private double pushSpeed = 0.1;

    public enum OcrAction {
        None,
        CheckCorrect,
        ChangePart
    }
    @Attribute(required = false) 
    private OcrAction ocrAction = OcrAction.None;

    @Element(required = false)
    private Length ocrMargin = new Length(20, LengthUnit.Millimeters); 

    @Attribute(required = false) 
    private String ocrFontName = "Liberation Mono";

    @Attribute(required = false) 
    private double ocrFontSizePt = 7.0;

    public enum OcrTextOrientation {
        AwayFromTape,
        TowardsTape
    };
    @Attribute(required = false) 
    private OcrTextOrientation ocrTextOrientation = OcrTextOrientation.AwayFromTape;


    // These internal setting are not on the GUI but can be changed in the XML.
    @Attribute(required = false)
    private int fidLocMaxPasses = 3;

    @Attribute(required = false)
    private double fidLocToleranceMm = 0.5;

    @Attribute(required = false)
    private double pocketPosToleranceMm = 0.1;

    /**
     * Maximum label size (derived from the fact that even 8mm tape labels must fit).
     */
    @Attribute(required = false) 
    private double maxLabelSizeMm = 10.0;

    /**
     * Offset of the tape center from the pocket center according to EIA 481 (derived).
     */
    @Attribute(required = false) 
    private double tapeCenterOffsetMm = -1.0625; 

    // Transient state
    private Length coverPosition = new Length(Double.NaN, LengthUnit.Millimeters);
    private Length pocketDistance = new Length(Double.NaN, LengthUnit.Millimeters);
    private boolean calibrating = false;
    private boolean calibrated = false;

    public static final String defaultGroupName = "Default";
    private static final List<String> locationGroupNamesList = Arrays.asList(new String[]{defaultGroupName, defaultGroupName.toUpperCase(), "LOCATION", "NONE", ""});

    private void checkHomedState(Machine machine) {
        if (!machine.isHomed()) {
            this.setCalibrated(false);
            this.setCoverPosition(new Length(Double.NaN, LengthUnit.Millimeters));
        }
    }

    public BlindsFeeder() {
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

    private void recalculateGeometry() {
        // This geometry must match the 3D printed feeder. So this code must remain in sync with the OpenSCAD file. 
        // @see /openpnp/src/main/resources/org/openpnp/machine/reference/feeder/BlindsFeeder-Library.scad 
        // and search for "recalculateGeometry()". 

        // The fiducials give us the first and last sprocket hole positions directly (in feeder-local X-coordinates).
        // The pockets are then aligned to the sprocket holes according to the EIA 481-C-2003 standard. 

        // According to the EIA-481-C, pockets align with the mid-point between two sprocket holes, 
        // however for the 2mm pitch tapes (0402 and smaller) obviously the pockets also directly 
        // align with sprocket holes.
        // This means that for 2mm pitch parts we can accommodate one more pocket in the tape i.e. 
        // the first one aligns with the sprocket instead of half the sprocket pitch away. 

        if (pocketPitch.getValue() > 0.0) {
            boolean isSmallPitch = isSmallPitch();
            setPocketCount((int)(Math.floor(tapeLength.divide(pocketPitch)))
                    + (isSmallPitch ? 1 : 0));
            // The wanted pocket center position relative to the pocket pitch is 0.25 for blinds covers,
            // but 0.5 for all other cover types where the part can be larger than half the pitch.  
            double pitchRelativePosition = (coverType == CoverType.BlindsCover ? 0.25 : 0.5);
            // Align the pocket center to the nearest sprocketPitch. Make sure to round 0.5 downwards 
            // (hence the -0.001).
            Length pocketAlign = sprocketPitch
                    .multiply(Math.round(pocketPitch.multiply(pitchRelativePosition).divide(sprocketPitch) - 0.001)); 
            // Now shift that to a mid-point between two sprocket holes (unless it is small pitch)
            setPocketDistance(sprocketPitch.multiply(isSmallPitch ? 0.0 : 0.5)
                    .add(pocketAlign)); 
        }
    }

    public boolean isSmallPitch() {
        return Math.round(sprocketPitch.divide(pocketPitch)) == 2;
    }

    private void assertCalibration() throws Exception {
        // Make sure the feeder locations are calibrated, if vision is enabled. 
        if (isVisionEnabled()) {
            if (!isCalibrated()) {
                calibrateFeederLocations();
            }
        }
        recalculateGeometry();
    }

    /**  
     * @param pocketNumber 1-based number of the pocket 
     * @return the Location of the pocket with the given number in the tape
     */
    public Location getUncalibratedPickLocation(double pocketNumber)  {
        recalculateGeometry();
        // Calculate the pick location in local feeder coordinates. 
        Length feederX = pocketPitch.multiply(pocketNumber-1.0).convertToUnits(location.getUnits()).add(pocketDistance);
        Length feederY = pocketCenterline.convertToUnits(location.getUnits());

        Location feederLocation = new Location(location.getUnits(), feederX.getValue(), feederY.getValue(), 
                location.getZ(), getPickRotationInTape());
        Location machineLocation = transformFeederToMachineLocation(feederLocation);
        return machineLocation;
    } 

    public Location getPickLocation(double pocketNumber) throws Exception {
        return getUncalibratedPickLocation(pocketNumber);
    }

    @Override
    public Location getPickLocation() throws Exception {
        return getPickLocation(this.getFedPocketNumber());
    }

    private int getFedPocketNumber() {
        return this.getFeedCount()+this.getFirstPocket()-1;
    }

    private boolean isBlindsCoverAlignmentReversed() {
        return false;
    }

    public boolean isCoverOpenState(boolean openState) {
        if (coverType == CoverType.NoCover) {
            // with no cover it is always open
            return openState == true;
        }
        if (Double.isNaN(coverPosition.getValue())) {
            // Unknown means no
            return false;
        }
        if (coverType == CoverType.BlindsCover) {
            double positionError = Math.abs(coverPosition.subtract(pocketDistance).convertToUnits(LengthUnit.Millimeters).getValue());
            return ((positionError < pocketPosToleranceMm) ^ isBlindsCoverAlignmentReversed()) == openState;
        }
        if (coverType == CoverType.PushCover) {
            return (coverPosition.getValue() > 0.0) == openState;
        }

        return false;
    }

    public boolean isCoverOpen() {
        return isCoverOpenState(true);
    }

    public boolean isCoverClosed() {
        return isCoverOpenState(false);
    }

    public boolean isCoverOpenChecked()  throws Exception {
        assertCalibration();
        if (Double.isNaN(coverPosition.getValue())) {
            Camera camera = getCamera();
            // Look at the 2nd blind, so you have one before and after, see Findfeatures.invoke(). 
            Location cameraOpenPosition = getPickLocation(2);
            // Move the camera over the blinds to check the open position.
            MovableUtils.moveToLocationAtSafeZ(camera, cameraOpenPosition);
            findCoverPosition(camera);
            Logger.debug("[BlindsFeeder.isCoverOpenChecked] pocketPosition: {}, pocketDistance {}, error {}", 
                    coverPosition, pocketDistance, coverPosition.subtract(pocketDistance));
        }
        return isCoverOpen();
    }

    public Camera getCamera() throws Exception {
        return Configuration.get()
                .getMachine()
                .getDefaultHead()
                .getDefaultCamera();
    }

    public void feed(Nozzle nozzle) throws Exception {
        if (getFirstPocket() + getFeedCount() > getLastPocket()) {
            throw new Exception("Feeder "+getName()+" part "+getPart().getId()+" empty.");
        }

        assertCalibration();
        if (coverType == CoverType.BlindsCover) {
            if (coverActuation == CoverActuation.CheckOpen) {
                if (!isCoverOpenChecked()) {
                    // Invalidate position to measure again after user intervention.
                    coverPosition = new Length(Double.NaN, LengthUnit.Millimeters);
                    throw new Exception("Feeder "+getName()+" "+getPart().getName()+": cover is not open. Please open manually.");
                }
            }
            else if (coverActuation == CoverActuation.OpenOnFirstUse || coverActuation == CoverActuation.OpenOnJobStart) {
                if (!isCoverOpen()) {
                    // Note, with CoverActuation.OpenOnJobStart, this should only happen with a manual or exceptional feed.
                    // Also restore the previously loaded NozzleTip afterwards. 
                    actuateCover(nozzle, true, true, true);
                }
            }
        }
        else if (coverType == CoverType.PushCover) {
            actuateCover(nozzle, true, true, true);
        }
        // increase the feed count 
        setFeedCount(getFeedCount() + 1);
    }

    /**
     * Returns if the feeder can take back a part.
     * Makes the assumption, that after each feed a pick followed,
     * so the pockets are now empty.
     */
    @Override
    public boolean canTakeBackPart() {
        if (getFeedCount() > 0 ) {  
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

        // if not yet open, open it (this should rarely be necessary as we likely just picked the part from this feeder;
        // however if cover opening is needed, it will only work if the machine has multiple nozzles and one is free, 
        // as cover opening is forbidden, when a part is on he nozzle). 
        if (!isCoverOpen()) {
            // repeat last feed operation, so that the pickLocation is the last free spot 
            setFeedCount(getFeedCount() - 1); // is immediately increased during feed
            feed(nozzle);
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

    private void setupOcr(Camera camera, CvPipeline pipeline, OcrAction ocrAction) {
        if (ocrAction != OcrAction.None) {
            pipeline.setProperty("regionOfInterest", getOcrRegion(camera));
            pipeline.setProperty("SimpleOcr.fontName", getOcrFontName());
            pipeline.setProperty("SimpleOcr.fontSizePt", getOcrFontSizePt());
            pipeline.setProperty("SimpleOcr.alphabet", OcrUtils.getConsolidatedPartsAlphabet(null, "\\"));
        }
        else {
            pipeline.setProperty("regionOfInterest", null);
            pipeline.setProperty("SimpleOcr.fontName", null);
            pipeline.setProperty("SimpleOcr.fontSizePt", null);
            pipeline.setProperty("SimpleOcr.alphabet", ""); // empty alphabet switches OCR off
        }
    }

    private RegionOfInterest getOcrRegion(Camera camera) {
        // By default we take the camera Y to define the ROI.  
        Location[] cornerLocations = getOcrRegionCornerLocations(getPocketCenterline());
        // Transform all into camera offset locations.
        int i = 0;
        Location cameraLocation = camera.getLocation();
        Location [] cameraOffsets = new Location[cornerLocations.length];
        for (Location feederLocation1 : cornerLocations) {
            Location machineLocation = transformFeederToMachineLocation(feederLocation1);
            cameraOffsets[i++] = machineLocation.subtract(cameraLocation);
        }
        // Create the ROI.
        RegionOfInterest roi = new RegionOfInterest(cameraOffsets[0], cameraOffsets[1], cameraOffsets[2], true);
        return roi; 
    }

    public Location[] getOcrRegionCornerLocations(Length feederY) {
        double ocrMarginMm = Math.abs(getOcrMargin().convertToUnits(LengthUnit.Millimeters).getValue());
        double feederYMm = feederY.convertToUnits(LengthUnit.Millimeters).getValue();
        double feederY0Mm = feederYMm 
                + tapeCenterOffsetMm - maxLabelSizeMm*0.5;
        double feederY1Mm = feederYMm 
                + tapeCenterOffsetMm + maxLabelSizeMm*0.5;
        // Create the feeder relative ROI for OCR, i.e. in the X margin area of the feeder.
        double edgePositionMm;
        if (getOcrMargin().getValue() < 0) {
            // HACK: by specifying a negative margin, the label is searched on the feeder end. 
            edgePositionMm = getTapeLength().add(getEdgeClosedDistance())
                    .convertToUnits(LengthUnit.Millimeters).getValue() + ocrMarginMm;
        }
        else {
            // Otherwise take the feeder beginning edge. 
            edgePositionMm = -getEdgeOpenDistance()
                    .convertToUnits(LengthUnit.Millimeters).getValue();
        }
        Location [] feederLocations;
        if (getOcrTextOrientation() == OcrTextOrientation.AwayFromTape) {
            // Scanning upright.
            feederLocations = new Location[] {
                    new Location(LengthUnit.Millimeters, edgePositionMm, feederY0Mm, 0, 0),
                    new Location(LengthUnit.Millimeters, edgePositionMm-ocrMarginMm, feederY0Mm, 0, 0),
                    new Location(LengthUnit.Millimeters, edgePositionMm, feederY1Mm, 0, 0)
            };
        }
        else {
            // Scanning upside-down. 
            feederLocations = new Location[] {
                    new Location(LengthUnit.Millimeters, edgePositionMm-ocrMarginMm, feederY1Mm, 0, 0),
                    new Location(LengthUnit.Millimeters, edgePositionMm, feederY1Mm, 0, 0),
                    new Location(LengthUnit.Millimeters, edgePositionMm-ocrMarginMm, feederY0Mm, 0, 0)
            };
        }
        return feederLocations;
    }

    public class FindFeatures {
        private Camera camera;
        private CvPipeline pipeline;
        private long showResultMilliseconds;

        // recognized stuff
        private List<RotatedRect> blinds;
        private List<RotatedRect> fiducials;
        private List<Line> lines;
        private double pocketSizeMm;
        private double pocketPositionMm;
        private double pocketPitchMm;
        private double pocketCenterlineMm;

        private SimpleOcr.OcrModel detectedOcrModel;

        public FindFeatures(Camera camera, CvPipeline pipeline, final long showResultMilliseconds) {
            this.camera = camera;
            this.pipeline = pipeline;
            this.showResultMilliseconds = showResultMilliseconds;
        }

        public List<RotatedRect> getBlinds() {
            return blinds;
        }
        public List<RotatedRect> getFiducials() {
            return fiducials;
        }
        public List<Line> getLines() {
            return lines;
        }
        public double getPocketSizeMm() {
            return pocketSizeMm;
        }
        public double getPocketPositionMm() {
            return pocketPositionMm;
        }
        public double getPocketPitchMm() {
            return pocketPitchMm;
        }
        public double getPocketCenterlineMm() {
            return pocketCenterlineMm;
        }

        private void drawRotatedRects(Mat mat, List<RotatedRect> features, Color color) {
            if (features == null || features.isEmpty()) {
                return;
            }
            Color centerColor = new HslColor(color).getComplementary();
            for (RotatedRect rect : features) {
                double x = rect.center.x;
                double y = rect.center.y;
                FluentCv.drawRotatedRect(mat, rect, color, 3);
                Imgproc.circle(mat, new org.opencv.core.Point(x, y), 2, FluentCv.colorToScalar(centerColor), 3, Imgproc.LINE_AA);
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

        private void drawOcrText(Mat mat, Color color) {
            if (detectedOcrModel != null) {
                Imgproc.putText(mat, detectedOcrModel.getText(), 
                        new org.opencv.core.Point(20, mat.rows()-20), 
                        Imgproc.FONT_HERSHEY_PLAIN, 
                        3, 
                        FluentCv.colorToScalar(Color.black), 6, 0, false);
                Imgproc.putText(mat, detectedOcrModel.getText(), 
                        new org.opencv.core.Point(20, mat.rows()-20), 
                        Imgproc.FONT_HERSHEY_PLAIN, 
                        3, 
                        FluentCv.colorToScalar(color), 2, 0, false);
            }
        }

        // number the parts in the pockets
        private void drawPartNumbers(Mat mat, Color color) {
            // make sure the numbers are not too dense
            int [] baseLine = null;
            double feederPocketPitchMm =  getPocketPitch().convertToUnits(LengthUnit.Millimeters).getValue();
            if (feederPocketPitchMm < 1.) {
                // feeder not set up yet
                return;
            }
            double feederPocketSizeMm =  getPocketSize().convertToUnits(LengthUnit.Millimeters).getValue();

            // calculate the diagonal text size
            double fontScale = 1.0;
            Size size = Imgproc.getTextSize(String.valueOf(getPocketCount()), Imgproc.FONT_HERSHEY_PLAIN, fontScale, 2, baseLine);
            Location textSizeMm = camera.getUnitsPerPixelAtZ().multiply(size.width, size.height, 0., 0.)
                    .convertToUnits(LengthUnit.Millimeters);
            if (textSizeMm.getY() < 0.0) {
                textSizeMm = textSizeMm.multiply(1.0, -1.0, 0.0, 0.0);
            }
            final double minFontSizeMm = 0.6;
            if (textSizeMm.getY() < minFontSizeMm) {
                fontScale = minFontSizeMm / textSizeMm.getY();
                textSizeMm = textSizeMm.multiply(fontScale, fontScale, 0.0, 0.0);
            }
            double textSizePitchCount = textSizeMm.getLinearDistanceTo(Location.origin)/feederPocketPitchMm;
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
            for (int i = step; i <= getPocketCount(); i += step) {
                String text = String.valueOf(i);
                Size textSize = Imgproc.getTextSize(text, Imgproc.FONT_HERSHEY_PLAIN, fontScale, 2, baseLine);

                Location partLocation = getUncalibratedPickLocation(i).convertToUnits(LengthUnit.Millimeters);
                // go below the pocket
                Location textLocation = transformMachineToFeederLocation(partLocation);
                textLocation = textLocation.add(new Location(LengthUnit.Millimeters, 0., -feederPocketSizeMm*0.5-textSizeMm.getY()*0.25, 0., 0.));
                textLocation = transformFeederToMachineLocation(textLocation).convertToUnits(LengthUnit.Millimeters);
                Point p = VisionUtils.getLocationPixels(camera, textLocation);
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
                            Imgproc.FONT_HERSHEY_PLAIN, 
                            fontScale, 
                            FluentCv.colorToScalar(color), 2, 0, false);
                }
            }

        }

        public FindFeatures invoke() throws Exception {
            List<RotatedRect> results = null;
            try {
                // Grab the results
                results = pipeline.getExpectedResult(VisionUtils.PIPELINE_RESULTS_NAME)
                        .getExpectedListModel(RotatedRect.class, 
                                null/*???new Exception("Feeder " + getName() + ": No features found.")*/);


                Result ocrStageResult = pipeline.getResult("OCR"); 
                if (ocrStageResult != null 
                        && pipeline.getProperty("SimpleOcr.alphabet") instanceof String
                        && ! ((String) pipeline.getProperty("SimpleOcr.alphabet")).isEmpty()) {
                    detectedOcrModel = ocrStageResult.getExpectedModel(SimpleOcr.OcrModel.class);
                }

                // in accordance with EIA-481 etc. we use millimeters.
                Location mmScale = camera.getUnitsPerPixelAtZ().convertToUnits(LengthUnit.Millimeters);
                // TODO: configurable?
                final double fidMin = 1.4;
                final double fidMax = 2.3;
                final double fidAspect = 1.3; 
                // TODO: configurable?
                final double tolerance = 1.4;
                double w = getPocketPitch().convertToUnits(LengthUnit.Millimeters).getValue()*0.5;
                double h = getPocketSize().convertToUnits(LengthUnit.Millimeters).getValue();
                double blindMin = Math.min(w, h)/tolerance;
                double blindMax = Math.max(w, h)*tolerance;
                double blindAspect = tolerance*tolerance*blindMax/blindMin;
                if (blindMax == 0) {
                    blindMax = 22;
                    blindAspect = 2;
                }
                if (blindMin == 0) {
                    blindMin = 0.5;
                    blindAspect = 3;
                }
                double positionTolerance = 5;
                double angleTolerance = 20;

                double pocketSizePreset = pocketSize.convertToUnits(LengthUnit.Millimeters).getValue();
                if (pocketSizePreset > 0) {
                    positionTolerance = pocketSizePreset*0.45; 
                }
                double pocketRange = Math.max(w, 2)*3.5;
                // Convert camera center.
                Location cameraFeederLocation = transformMachineToFeederLocation(camera.getLocation())
                        .convertToUnits(LengthUnit.Millimeters);
                double cameraFeederX = cameraFeederLocation.getX();  
                double cameraFeederY = cameraFeederLocation.getY();  
                // Try to make sense of it.
                boolean angleTolerant = 
                        !(getFiducial1Location().isInitialized() && getFiducial2Location().isInitialized());
                boolean positionTolerant = angleTolerant;// || getPocketCenterline().getValue() == 0;
                blinds = new ArrayList<>();
                fiducials = new ArrayList<>();

                // Create center and corner histograms
                SimpleHistogram histogramUpper = new SimpleHistogram(0.1);
                SimpleHistogram histogramLower = new SimpleHistogram(0.1);

                for (RotatedRect result : results) {
                    org.opencv.core.Point points[] = new org.opencv.core.Point[4];
                    result.points(points);
                    boolean isAtMargin = false;
                    for (int i = 0; i < 4; i++) {
                        // Filter out rects sticking to the edges.
                        if (points[i].x <= 2 || points[i].x >= camera.getWidth() - 2
                                || points[i].y <= 2 || points[i].y >= camera.getHeight() - 2) {
                            isAtMargin = true;
                        }
                        // Next, please.
                        i++;
                    }
                    if (! isAtMargin) {
                        // Convert shape center.
                        Location center = transformMachineToFeederLocation(VisionUtils.getPixelLocation(camera, result.center.x, result.center.y))
                                .convertToUnits(LengthUnit.Millimeters);
                        double angle = transformPixelToFeederAngle(result.angle);
                        Location mmSize = mmScale.multiply(result.size.width, result.size.height, 0, 0);
                        if (positionTolerant || cameraFeederLocation.getLinearDistanceTo(center) < positionTolerance) {
                            if (angleTolerant || Math.abs(Utils2D.angleNorm(angle - 45)) < angleTolerance) {
                                if (mmSize.getX() > fidMin && mmSize.getX() < fidMax 
                                        && mmSize.getY() > fidMin && mmSize.getY() < fidMax
                                        && mmSize.getX()/mmSize.getY() < fidAspect 
                                        && mmSize.getY()/mmSize.getX() < fidAspect) {
                                    fiducials.add(result);
                                    Logger.debug("accepted fiducal candidate: result {}, mmSize {}", 
                                            result, mmSize);
                                }
                                else {
                                    Logger.debug("dismissed fiducal candidate: result {}, mmSize {}", 
                                            result, mmSize);
                                }
                            }
                            else {
                                Logger.debug("dismissed fiducal candidate: result {}, angle {}", 
                                        result, angle);
                            }
                        }
                        else {
                            Logger.debug("dismissed fiducal candidate: result {}, center {}", 
                                    result, center);
                        }
                        if (positionTolerant || Math.abs(cameraFeederY - center.getY()) < positionTolerance) {
                            if (positionTolerant || Math.abs(cameraFeederX - center.getX()) < pocketRange) {
                                    if (angleTolerant || Math.abs(Utils2D.angleNorm(angle - 0)) < angleTolerance) {
                                    if (mmSize.getX() > blindMin && mmSize.getX() < blindMax && mmSize.getY() > blindMin && mmSize.getY() < blindMax
                                            && mmSize.getX()/mmSize.getY() < blindAspect 
                                            && mmSize.getY()/mmSize.getX() < blindAspect) {
                                        // Fits the size requirements.
                                        if (!positionTolerant) {
                                            // Add corners' Y to histogram for later pocket size analysis. 
                                            for (org.opencv.core.Point point : points) {
                                                Location corner = transformMachineToFeederLocation(VisionUtils.getPixelLocation(camera, point.x, point.y))
                                                        .convertToUnits(LengthUnit.Millimeters);
                                                if (corner.getY() < cameraFeederY) {
                                                    histogramLower.add(corner.getY(), 1.);
                                                }
                                                else {
                                                    histogramUpper.add(corner.getY(), 1.);
                                                }
                                            }
                                        }
                                        blinds.add(result);
                                        Logger.debug("accepted pocket candidate: result {}, mmSize {}", 
                                                result, mmSize);
                                    }
                                    else {
                                        Logger.debug("dismissed pocket candidate: result {}, mmSize {}", 
                                                result, mmSize);
                                    }
                                }
                                else {
                                    Logger.debug("dismissed pocket candidate: result {}, angle {}", 
                                            result, angle);
                                }
                            }
                            else {
                                Logger.debug("dismissed pocket candidate: result {}, X {}", 
                                        result, center.getX());
                            }
                        }
                        else {
                            Logger.debug("dismissed pocket candidate: result {}, Y {}", 
                                    result, center.getY());
                        }
                    }
                }

                // Sort fiducials by distance from camera center.
                Collections.sort(fiducials, new Comparator<RotatedRect>() {
                    @Override
                    public int compare(RotatedRect o1, RotatedRect o2) {
                        double d1 = VisionUtils.getPixelLocation(camera, o1.center.x, o1.center.y).getLinearDistanceTo(camera.getLocation());
                        double d2 = VisionUtils.getPixelLocation(camera, o2.center.x, o2.center.y).getLinearDistanceTo(camera.getLocation());
                        return Double.compare(d1, d2);
                    }
                });

                // Sort blinds by feeder local X.
                Collections.sort(blinds, new Comparator<RotatedRect>() {
                    @Override
                    public int compare(RotatedRect o1, RotatedRect o2) {
                        double d1 = transformMachineToFeederLocation(VisionUtils.getPixelLocation(camera, o1.center.x, o1.center.y)).getX();
                        double d2 = transformMachineToFeederLocation(VisionUtils.getPixelLocation(camera, o2.center.x, o2.center.y)).getX();
                        return Double.compare(d1, d2);
                    }
                });


                // Try to determine the pocket size and center by evaluating the histogram of corner feeder local Y coordinates.
                double bestLowerY = histogramLower.getMaximumKey();
                double bestUpperY = histogramUpper.getMaximumKey();
                pocketSizeMm = bestUpperY - bestLowerY;
                pocketCenterlineMm = Math.round((bestUpperY + bestLowerY)*0.5);
                Logger.debug("histogram in Y: pocketCenterlineMm {}, pocketSizeMm {}", 
                        pocketCenterlineMm, pocketSizeMm);

                lines = new ArrayList<>();
                for (Double bestY : new double[]{bestLowerY, pocketCenterlineMm, bestUpperY}) {
                    if (!Double.isNaN(bestY)) {
                        // create a line for visual feedback
                        Location l1 = new Location(LengthUnit.Millimeters, -100., bestY, 0., 0.);
                        Location l2 = new Location(LengthUnit.Millimeters, +100., bestY, 0., 0.);
                        l1 = transformFeederToMachineLocation(l1);
                        l2 = transformFeederToMachineLocation(l2);
                        Point p1 = VisionUtils.getLocationPixels(camera, l1);
                        Point p2 = VisionUtils.getLocationPixels(camera, l2);
                        Line line = new Line(new org.opencv.core.Point(p1.x, p1.y), new org.opencv.core.Point(p2.x, p2.y));
                        lines.add(line);
                    }
                }

                // Try to determine the pocket pitch by creating a histogram of sorted blinds distances. 
                Location previous = null;
                SimpleHistogram histogramPitch = new SimpleHistogram(2);
                for (RotatedRect rect : blinds) {
                    Location location = transformMachineToFeederLocation(VisionUtils.getPixelLocation(camera, rect.center.x, rect.center.y))
                            .convertToUnits(LengthUnit.Millimeters);
                    if (previous != null) {
                        double pitch = location.getX() - previous.getX();
                        histogramPitch.add(pitch, 1.0);
                    }
                    previous = location;
                }
                pocketPitchMm = histogramPitch.getMaximumKey();
                Logger.debug("histogram in pitch: pocketPitchMm {}", 
                        pocketPitchMm);

                // Try to determine the pocket position from feeder zero X.
                double pocketPitchPosMm = (getPocketPitch().getValue() != 0. ? 
                        getPocketPitch().convertToUnits(LengthUnit.Millimeters).getValue() 
                        :   pocketPitchMm); 
                SimpleHistogram histogramDistance = new SimpleHistogram(0.05);
                for (RotatedRect rect : blinds) {
                    Location location = transformMachineToFeederLocation(VisionUtils.getPixelLocation(camera, rect.center.x, rect.center.y))
                            .convertToUnits(LengthUnit.Millimeters);
                    // restrict pockets to near camera center, to limit parallax/focal plane errors.
                    if (Math.abs(location.getX() - cameraFeederX) < (2+pocketPitchPosMm*1.5)) {
                        // calculate the positive modulo distance
                        double position = location.getX() % pocketPitchPosMm;
                        if (position < 0.) {
                            position += pocketPitchPosMm;
                        }
                        // Simply record the position in three pitch-modulo bins to support wrap-around with kernel.  
                        histogramDistance.add(position-pocketPitchPosMm, 1.0);
                        histogramDistance.add(position, 1.0);
                        histogramDistance.add(position+pocketPitchPosMm, 1.0);
                    }
                }
                pocketPositionMm = histogramDistance.getMaximumKey();
                Logger.debug("histogram position: pocketPositionMm {} using pitch {}", 
                        pocketPositionMm, pocketPitchPosMm);

                if (!Double.isNaN(pocketPositionMm)) {
                    if (pocketPositionMm < 0.0) {
                        pocketPositionMm += pocketPitchMm;
                    }
                    if (pocketPositionMm > pocketPitchMm) {
                        pocketPositionMm -= pocketPitchMm;
                    }
                    // create a line for visual feedback
                    Location l1 = new Location(LengthUnit.Millimeters, pocketPositionMm, pocketCenterlineMm-100., 0., 0.);
                    Location l2 = new Location(LengthUnit.Millimeters, pocketPositionMm, pocketCenterlineMm+100., 0., 0.);
                    l1 = transformFeederToMachineLocation(l1);
                    l2 = transformFeederToMachineLocation(l2);
                    Point p1 = VisionUtils.getLocationPixels(camera, l1);
                    Point p2 = VisionUtils.getLocationPixels(camera, l2);
                    Line line = new Line(new org.opencv.core.Point(p1.x, p1.y), new org.opencv.core.Point(p2.x, p2.y));
                    lines.add(line);
                }

                if (showResultMilliseconds > 0) {
                    // Draw the result onto the pipeline image.
                    Mat resultMat = pipeline.getWorkingImage().clone();
                    drawRotatedRects(resultMat, getBlinds(), Color.blue);
                    drawRotatedRects(resultMat, getFiducials(), Color.white);
                    drawLines(resultMat, getLines(), new Color(0, 0, 128));
                    drawPartNumbers(resultMat, Color.orange);
                    drawOcrText(resultMat, Color.orange);

                    if (Logger.getLevel() == org.pmw.tinylog.Level.DEBUG || Logger.getLevel() == org.pmw.tinylog.Level.TRACE) {
                        File file = Configuration.get().createResourceFile(getClass(), "blinds-feeder", ".png");
                        Imgcodecs.imwrite(file.getAbsolutePath(), resultMat);
                    }
                    BufferedImage showResult = OpenCvUtils.toBufferedImage(resultMat);
                    resultMat.release();
                    MainFrame.get().getCameraViews().getCameraView(camera)
                    .showFilteredImage(showResult, showResultMilliseconds);
                }
            }
            catch (ClassCastException e) {
                throw new Exception("Unrecognized result type (should be RotatedRect): " + results);
            }

            return this;
        }
    }

    public void showFeatures() throws Exception {
        Camera camera = getCamera();
        ensureCameraZ(camera);
        try (CvPipeline pipeline = getCvPipeline(camera, true, OcrAction.None)) {

            // Process vision and show feature without applying anything
            pipeline.process();
            new FindFeatures(camera, pipeline, 2000).invoke();
        }
    }

    public void findPocketsAndCenterline(Camera camera) throws Exception {
        // Try to clone some info from another feeder.
        BlindsFeeder template = updateFromConnectedFeeder(camera.getLocation(), false);
        if (template != null && !getLocation().getLengthZ().isInitialized()) {
            // Clone Z as a proposal.
            setLocation(getLocation().deriveLengths(null,  null, template.getLocation().getLengthZ(), null));
        }

        if (!(fiducial1Location.isInitialized() && fiducial2Location.isInitialized() && fiducial3Location.isInitialized())) {
            throw new Exception("Feeder " + getName() + ": Please set the fiducials first (camera center is outside any previously defined fiducial area).");
        }

        if (coverType == CoverType.BlindsCover) {
            // For a BlindsCover we can use vision to try and determine the specs.
            ensureCameraZ(camera);
            try (CvPipeline pipeline = getCvPipeline(camera, true, getOcrAction())) {

                // Reset the specs to allow FindFeatures to acquire them freely.
                setPocketCenterline(new Length(0., LengthUnit.Millimeters)); 
                setPocketPitch(new Length(0., LengthUnit.Millimeters)); 
                setPocketSize(new Length(0., LengthUnit.Millimeters)); 

                // Process vision
                pipeline.process();

                // Grab the results
                BlindsFeeder.FindFeatures findFeaturesResults = new FindFeatures(camera, pipeline, 1000).invoke();

                if (Double.isNaN(findFeaturesResults.getPocketCenterlineMm())) {
                    throw new Exception("Feeder " + getName() + ": Tape centerline not found.");
                }

                if (Double.isNaN(findFeaturesResults.getPocketPitchMm())) {
                    throw new Exception("Feeder " + getName() + ": Pocket pitch not found.");
                }
                // TODO: validate pitch against known EIA pitch values {2, 4, 8, 12, 16...}

                if (Double.isNaN(findFeaturesResults.getPocketSizeMm())) {
                    throw new Exception("Feeder " + getName() + ": Pocket size not found.");
                }

                setPocketCenterline(new Length(findFeaturesResults.getPocketCenterlineMm(), LengthUnit.Millimeters));
                setPocketPitch(new Length(findFeaturesResults.getPocketPitchMm(), LengthUnit.Millimeters));
                setPocketSize(new Length(findFeaturesResults.getPocketSizeMm(), LengthUnit.Millimeters));

                if (getOcrAction() != OcrAction.None) {
                    OcrModel detectedOcrModel = findFeaturesResults.detectedOcrModel;
                    Logger.trace("OCR text "+detectedOcrModel.getText());
                    triggerOcrAction(detectedOcrModel, getOcrAction());
                }
            }
        }
        else  {
            // For other cover types we can only capture the pocket centerline from the camera position. 

            // Transform camera location to feeder local Millimeter Y coordinate rounded to 1mm. According to EIA-481-C and the design
            // of the 3D printed feeder we can assert all coordinates are integral 1mm values.
            Location cameraLocation = camera.getLocation();
            Location feederLocation = transformMachineToFeederLocation(cameraLocation).convertToUnits(LengthUnit.Millimeters);
            setPocketCenterline(new Length(Math.round(feederLocation.getY()), LengthUnit.Millimeters));
        }
    }

    public void ensureCameraZ(Camera camera) throws Exception {
        if (camera.isUnitsPerPixelAtZCalibrated()
                && !getLocation().getLengthZ().isInitialized()) {
            throw new Exception("Feeder "+getName()+": Please set the Part Z first, it is required to determine "
                    + "the true scale of the camera view for accurate computer vision.");
        }
        if (getLocation().getLengthZ().isInitialized()) {
            // If we already have the Feeder Z, move the camera there to get the right units per pixel.
            camera.moveTo(camera.getLocation().deriveLengths(null, null, getLocation().getLengthZ(), null));
        }
    }

    public void triggerOcrAction(OcrModel detectedOcrModel, OcrAction ocrAction) throws Exception {
        Part ocrPart = OcrUtils.identifyDetectedPart(detectedOcrModel, this);
        Part currentPart = getPart();
        if (currentPart == null) {
            // No part set yet 
            Logger.trace("OCR detected part in feeder "+getId()+", OCR part "+ocrPart.getId());
            setPart(ocrPart);
        }
        else if (ocrPart != null && ocrPart != currentPart) {
            // Wrong part selected in feeder
            Logger.trace("OCR detected wrong part in slot of feeder "+getName()
            +", current part "+currentPart.getId()+" != OCR part "+ocrPart.getId());
            if (ocrAction == OcrAction.ChangePart) {
                setPart(ocrPart);
                ocrChangedPartId = currentPart.getId();
            }
            else if (ocrAction == OcrAction.CheckCorrect) {
                throw new Exception("OCR detected wrong part in slot of feeder "+getName()
                +", current part "+currentPart.getId()+" != OCR part "+ocrPart.getId());
            }
        }
        else if (ocrPart == null) {
            if (ocrAction == OcrAction.ChangePart
                    || ocrAction == OcrAction.CheckCorrect) {
                throw new Exception("OCR could not detect part in slot of feeder "+getName()
                +", current part "+currentPart.getId());
            }
        }
    }

    public void performOcr(Camera camera, OcrAction ocrAction) throws Exception {
        // TODO: try to roll this into the isCoverOpenChecked() vision, if possible 
        // (which will likely be a question of part pitch and camera view size, i.e. for small parts 
        // and wide camera views it will be possible, but not for large parts or narrow camera views).

        recalculateGeometry();

        // Calculate the nominal OCR detection location in local feeder coordinates. 
        Location[] cornerLocations = getOcrRegionCornerLocations(getPocketCenterline());
        // As the cornerLocations describe three corners of a rectangle, we can just take the mid-point
        // of the diagonal between corners 1 and 2 to get the center. 
        Location feederLocation = cornerLocations[1].add(cornerLocations[2]).multiply(0.5);
        Location machineLocation = transformFeederToMachineLocation(feederLocation);

        // Move the camera over the label.
        MovableUtils.moveToLocationAtSafeZ(camera, machineLocation);

        try (CvPipeline pipeline = getCvPipeline(camera, true, ocrAction)) {
            // Process vision
            pipeline.process();

            // Grab the results
            BlindsFeeder.FindFeatures findFeaturesResults = new FindFeatures(camera, pipeline, 1000).invoke();

            OcrModel detectedOcrModel = findFeaturesResults.detectedOcrModel;
            if (detectedOcrModel == null) {
                throw new Exception("Feeder "+getName()+" is missing an \"OCR\" stage in the pipeline.");
            }
            Logger.trace("OCR text "+detectedOcrModel.getText());
            triggerOcrAction(detectedOcrModel, ocrAction);
        }
    }

    public void findCoverPosition(Camera camera) throws Exception {
        try (CvPipeline pipeline = getCvPipeline(camera, true, OcrAction.None)) {
            // Process vision
            pipeline.process();

            // Grab the results
            BlindsFeeder.FindFeatures findFeaturesResults = new FindFeatures(camera, pipeline, 1000).invoke();

            if (Double.isNaN(findFeaturesResults.getPocketPositionMm())) {
                throw new Exception("Feeder " + getName() + ": Pocket position not found.");
            }

            setCoverPosition(new Length(findFeaturesResults.getPocketPositionMm(), LengthUnit.Millimeters));
        }
    }

    public void calibrateCoverEdges()  throws Exception {
        if (coverType != CoverType.BlindsCover) {
            throw new Exception("Feeder " + getName() + ": Only Blinds Cover can be calibrated.");
        }
        Camera camera = getCamera();

        // Calculate the wanted open/closed positions.
        assertCalibration();
        Length pitchHalf = pocketPitch.multiply(0.5).convertToUnits(LengthUnit.Millimeters);
        Length wantedOpenPosition = getPocketDistance();
        Length wantedClosedPosition = getPocketDistance().add(pitchHalf).modulo(pocketPitch);
        double cameraPocket = Math.floor((getFirstPocket()+getLastPocket())/2.0);
        Location cameraOpenPosition = getPickLocation(cameraPocket);
        Location cameraClosedPosition = getPickLocation(cameraPocket-0.5);

        double factor = isBlindsCoverAlignmentReversed() ? -1.0 : 1.0;

        // Close cover to start with a known opposite position.
        actuateCover(false);

        // Calibration passes loop. 
        for (int i = 0; i < 3; i++) {
            // Open the cover.
            actuateCover(true);
            // Move the camera over the blinds to check the open position.
            MovableUtils.moveToLocationAtSafeZ(camera, cameraOpenPosition);
            // Where is it? 
            findCoverPosition(camera);
            Length offsetOpen = getCoverPosition()
                    .subtract(wantedOpenPosition)
                    .convertToUnits(LengthUnit.Millimeters);
            if (offsetOpen.getValue() > pitchHalf.getValue()) {
                offsetOpen = offsetOpen.subtract(pocketPitch);
            }
            if (offsetOpen.getValue() < -pitchHalf.getValue()) {
                offsetOpen = offsetOpen.add(pocketPitch);
            }
            // Apply the error to the edge.
            setEdgeOpenDistance(getEdgeOpenDistance()
                    .add(offsetOpen.multiply(factor)));

            // Close the cover.
            actuateCover(false);
            // Move the camera over the blinds to check the closed position.
            MovableUtils.moveToLocationAtSafeZ(camera, cameraClosedPosition);
            // Where is it? 
            findCoverPosition(camera);
            Length offsetClosed = getCoverPosition()
                    .subtract(wantedClosedPosition)
                    .convertToUnits(LengthUnit.Millimeters);
            if (offsetClosed.getValue() > pitchHalf.getValue()) {
                offsetClosed = offsetClosed.subtract(pocketPitch);
            }
            if (offsetClosed.getValue() < -pitchHalf.getValue()) {
                offsetClosed = offsetClosed.add(pocketPitch);
            }
            // Apply the error to the edge.
            setEdgeClosedDistance(getEdgeClosedDistance()
                    .subtract(offsetClosed.multiply(factor)));

            // Test against half the tolerance used for the "check open" test.
            if (Math.abs(offsetOpen.getValue()) < pocketPosToleranceMm*0.5
                    && Math.abs(offsetClosed.getValue()) < pocketPosToleranceMm*0.5) {
                // Both good enough - no more passes needed.
                break;
            }
        }
    }

    private Location locateFiducial(Camera camera, Location location) throws Exception {
        if (!location.isInitialized()) {
            throw new Exception("Feeder " + getName() + ": Fiducial location not set.");
        }

        // Take Z off the camera.
        ensureCameraZ(camera);
        location = location.derive(camera.getLocation(), false, false, true, false);
        try (CvPipeline pipeline = getCvPipeline(camera, true, OcrAction.None)) {

            for (int i = 0; i < fidLocMaxPasses; i++) {
                // Move to the location
                MovableUtils.moveToLocationAtSafeZ(camera, location);

                //camera.settleAndCapture();
                // Process vision
                pipeline.process();

                // Interpret the results
                BlindsFeeder.FindFeatures findFeaturesResults = new FindFeatures(camera, pipeline, 250).invoke();
                List<RotatedRect> fiducials = findFeaturesResults.getFiducials();
                if (fiducials.isEmpty()) {
                    throw new Exception("Feeder " + getName() + ": Fiducial not found.");
                }
                // Convert location.
                RotatedRect bestFiducial = fiducials.get(0);
                Location bestFiducialLocation = VisionUtils.getPixelLocation(camera, bestFiducial.center.x, bestFiducial.center.y);
                double mmDistance = bestFiducialLocation.getLinearLengthTo(location).convertToUnits(LengthUnit.Millimeters).getValue();
                Logger.debug("bestFiducialLocation: {}, mmDistance {}", 
                        bestFiducialLocation, mmDistance);

                // update location
                location = bestFiducialLocation;

                if (mmDistance < fidLocToleranceMm) {
                    // Already a good enough fix - skip further passes
                    break;
                }
            }
            // Return the final result.
            return location;
        }
    }

    public void calibrateFeederLocations() throws Exception {
        if (!isCalibrating()) {

            if ( !Configuration.get().getMachine().isHomed() ) {
                throw new Exception("Feeder " + getName() + ": Machine not yet homed.");
            }

            setCalibrating(true);
            try {
                Camera camera = getCamera();
                setFiducial1Location(locateFiducial(camera, getFiducial1Location()));
                setFiducial3Location(locateFiducial(camera, getFiducial3Location()));
                if (camera.isReachable(getFiducial2Location())) {
                    setFiducial2Location(locateFiducial(camera, getFiducial2Location()));
                }
                else {
                    // Fiducial 2 not reachable by camera -> reconstruct from Fiducials 1 und 3.
                    Location yAxis = getFiducial3Location().subtract(getFiducial1Location());
                    double distance = getFiducial3Location().getLinearDistanceTo((getFiducial1Location()));
                    yAxis = yAxis.multiply(1/distance, 1/distance, 0., 0.);
                    Location xAxis = new Location(yAxis.getUnits(), yAxis.getY(), -yAxis.getX(), 0., 0.);
                    double scale = getTapeLength().convertToUnits(xAxis.getUnits()).getValue();
                    Location reconstructedFiducial2 = getFiducial1Location().add(xAxis.multiply(scale, scale, 0., 0.));
                    setFiducial2Location(reconstructedFiducial2);
                }
                setCalibrated(true);
            }
            finally {
                setCalibrating(false);
            }
        }
    }

    public enum CoverType {
        NoCover, BlindsCover, PushCover
    }
    public enum CoverActuation {
        Manual, CheckOpen, OpenOnFirstUse, OpenOnJobStart
    }

    @Attribute(required = false)
    private CoverType coverType = CoverType.BlindsCover;

    @Attribute(required = false)
    private CoverActuation coverActuation = CoverActuation.OpenOnJobStart;

    public static List<BlindsFeeder> getFeedersWithCoverToActuate(List<Feeder> feederPool, 
            CoverActuation [] coverActuations,
            boolean openState) {
        // Filter out all the BlindsFeeders  
        List<BlindsFeeder> feederList = new ArrayList<>();
        for (Feeder feeder : feederPool)  {
            if (feeder instanceof BlindsFeeder) {
                BlindsFeeder blindsFeeder = (BlindsFeeder)feeder;
                // Take only those with blinds cover.
                if (blindsFeeder.getCoverType() == CoverType.BlindsCover) {
                    // Filter by cover actuation.
                    for (CoverActuation coverActuation : coverActuations) {
                        if (blindsFeeder.getCoverActuation() == coverActuation) {
                            // Take only enabled and/or positively opened feeders to be closed now
                            if (feeder.isEnabled() || (blindsFeeder.isCoverOpenState(true) && ! openState)) {
                                if (!blindsFeeder.isCoverOpenState(openState)) {
                                    feederList.add(blindsFeeder);
                                }
                            }
                        }
                    }
                }
            }
        }
        return feederList;
    }

    public static void actuateAllFeederCovers(Nozzle preferredNozzle, boolean openState) throws Exception  {
        List<BlindsFeeder> feederList = getFeedersWithCoverToActuate(Configuration.get().getMachine().getFeeders(), 
                new CoverActuation [] { CoverActuation.Manual, CoverActuation.CheckOpen, CoverActuation.OpenOnFirstUse, CoverActuation.OpenOnJobStart }, 
                openState); 
        if (feederList.size() == 0) {
            throw new Exception("No feeders found to "+(openState ? "open." : "close."));
        }
        actuateListedFeederCovers(preferredNozzle, feederList, openState, false);
    }

    public static void actuateListedFeederCovers(Nozzle preferredNozzle, List<BlindsFeeder> feederList, boolean openState, boolean restoreNozzleTip) throws Exception  {
        if (feederList.size() == 0) {
            // nothing to do, avoid changing the nozzle tip for nothing.
            return;
        }

        // Get the push nozzle for the current position. This will also throw if none is allowed.
        NozzleAndTipForPushing nozzleAndTipForPushing = BlindsFeeder.getNozzleAndTipForPushing(preferredNozzle, true);

        // Use a Travelling Salesman algorithm to optimize the path to actuate all the feeder covers.
        TravellingSalesman<BlindsFeeder> tsm = new TravellingSalesman<>(
                feederList, 
                new TravellingSalesman.Locator<BlindsFeeder>() { 
                    @Override
                    public Location getLocation(BlindsFeeder locatable) {
                        return locatable.getUncalibratedPickLocation(openState ? 0 : locatable.getPocketCount()+1);
                    }
                }, 
                // start from current location
                nozzleAndTipForPushing.getNozzle().getLocation(), 
                // no end location
                null);

        // Solve it using the default heuristics.
        tsm.solve();

        // Finally actuate the feeders along the travel path.
        for (BlindsFeeder blindsFeeder : tsm.getTravel()) {
            blindsFeeder.actuateCover(openState);
        }

        if (restoreNozzleTip) {
            nozzleAndTipForPushing.restoreNozzleTipLoadedBefore();
        }
    }

    public double getPickRotationInTape() {
        // Get the pick rotation in relation to the tape orientation (feeder local coordinates).

        // * What is 0° for the rotation of the part is determined by how the part footprint
        //   is drawn in the ECAD. However look up "Zero Component Orientation" for the 
        //   standardized way to do this. 
        // * What is 0° for the rotation of the tape is defined in accordance to the 
        //   EIA-481-C "Quadrant designations" ("upper left" etc.).
        // * Consequently a pick rotation of 0° means that the part is oriented upwards as 
        //   drawn in the ECAD, when holding the tape horizontal with the sprocket holes 
        //   at the top. If the tape has sprocket holes on both sides, look at the round, not 
        //   the elongated holes. 
        // * Also consult "EIA-481-C" to see how parts should be oriented in the tape.

        // Our local feeder coordinate system has the positive X axis advance the pick location. 
        // Y is the direction in which the feeders are arrayed. With the feeders arranged around 
        // the machine and the first parts facing the center, this results in a counter-clockwise 
        // layout of the feeders when seen from above (like pins on an electronics part).
        // Following the pins analogy, and in accordance with the EIA-481-C "Quadrant designations", 
        // the first feeders should be in the upper left quadrant, with our local coordinate system 
        // rotated by 180°. Consequently the sprocket holes are then on top like in the EIA-481-C 
        // definition. The same meaning is adopted in the OpenPNP ReferenceStripFeeder. 
        // 
        // This means that we need to rotate by 180 degrees from our feeder local coordinate system. 
        return location.getRotation() + 180.;
    }

    public static class NozzleAndTipForPushing {
        private Nozzle nozzle;
        private NozzleTip nozzleTipLoadedBefore;
        private boolean changed;

        public NozzleAndTipForPushing(Nozzle nozzle, NozzleTip nozzleTipLoadedBefore, boolean changed) {
            super();
            this.nozzle = nozzle;
            this.nozzleTipLoadedBefore = nozzleTipLoadedBefore;
            this.changed = changed;
        }
        void restoreNozzleTipLoadedBefore() throws Exception {
            if (this.nozzle != null & this.nozzleTipLoadedBefore != null && this.changed) {
                if (this.nozzleTipLoadedBefore != this.nozzle.getNozzleTip()) {
                    this.nozzle.loadNozzleTip(this.nozzleTipLoadedBefore);
                }
            }
        }
        public Nozzle getNozzle() {
            return nozzle;
        }
        public NozzleTip getNozzleTip() {
            if (this.nozzle != null) {
                return this.nozzle.getNozzleTip();
            }
            return null;
        }
        public NozzleTip getNozzleTipLoadedBefore() {
            return nozzleTipLoadedBefore;
        }
        public boolean isChanged() {
            return changed;
        }
    }
    public static NozzleAndTipForPushing getNozzleAndTipForPushing(Nozzle preferredNozzle, boolean loadNozzleTipIfNeeded) throws Exception {
        // Search for any nozzle and nozzle tip that may be used for cover pushing. 
        // First, try the preferredNozzle.
        if (preferredNozzle != null ) {
            if (preferredNozzle.getPart() == null) { 
                // Nozzle is free
                NozzleTip nozzleTip = preferredNozzle.getNozzleTip();
                if (nozzleTip != null && nozzleTip.isPushAndDragAllowed()) {
                    // Return the nozzle and nozzle tip.
                    return new NozzleAndTipForPushing(preferredNozzle, nozzleTip, false);
                }
            }
        }

        // Second, try any free nozzle with loaded nozzle tip. 
        Machine machine = Configuration.get().getMachine();
        for (Head head : machine.getHeads()) {
            for (Nozzle nozzle :  head.getNozzles()) {
                if (nozzle.getPart() == null) { 
                    // Nozzle is free
                    NozzleTip nozzleTip = nozzle.getNozzleTip();
                    if (nozzleTip != null && nozzleTip.isPushAndDragAllowed()) {
                        // Return the nozzle and nozzle tip.
                        return new NozzleAndTipForPushing(nozzle, nozzleTip, false);
                    }
                }
            }
        }

        // Third, try load a nozzle tip for pushing.
        if (loadNozzleTipIfNeeded) {
            // We're allowed to load the nozzle tip, go find a free nozzle.
            for (Head head : machine.getHeads()) {
                for (Nozzle nozzle :  head.getNozzles()) {
                    if (nozzle.getPart() == null) { 
                        // Nozzle is free
                        for (NozzleTip pushNozzleTip : nozzle.getCompatibleNozzleTips()) {
                            if (pushNozzleTip.isPushAndDragAllowed()) {
                                boolean goodToGo = true;
                                for (Head otherHead : machine.getHeads()) {
                                    for (Nozzle otherNozzle :  otherHead.getNozzles()) {
                                        if (otherNozzle.getNozzleTip() == pushNozzleTip) {
                                            // Loaded on another nozzle, this means, it has a part on, otherwise 
                                            // it would already have been taken above.
                                            goodToGo = false;
                                            break;
                                        }
                                    }
                                }
                                if (goodToGo) {
                                    NozzleTip nozzleTip = nozzle.getNozzleTip();
                                    // Alas, found one for pushing, load it
                                    nozzle.loadNozzleTip(pushNozzleTip);
                                    // And return the nozzle and nozzle tip.
                                    return new NozzleAndTipForPushing(nozzle, nozzleTip, true);
                                }
                            }
                        }
                    }
                }
            }
            // None could be loaded.
            throw new Exception("BlindsFeeder: No Nozzle/NozzleTip found that allows pushing and has no part loaded.");
        }
        // None compatible.
        return new NozzleAndTipForPushing(null, null, false);
    }


    @Override
    public Location getJobPreparationLocation() {
        if ((isVisionEnabled() && !isCalibrated())
            || getOcrAction() != OcrAction.None
            || (getCoverActuation() == CoverActuation.OpenOnJobStart
                && ! isCoverOpen())) {
            return getUncalibratedPickLocation(0);
        }
        else {
            return null;
        }
    }

    // transient store for the job preparation nozzle tip change and OCR
    private NozzleAndTipForPushing nozzleAndTipForPushing = null;
    private String ocrChangedPartId = null;

    @Override
    public void prepareForJob(boolean visit) throws Exception {
        super.prepareForJob(visit);
        if (visit) {
            assertCalibration();
            
            ocrChangedPartId = null;
            if (getCoverActuation() == CoverActuation.OpenOnJobStart
                    && ! isCoverOpen()) {
                // Get the push nozzle for the current position. This will also throw if none is allowed.
                // Note, we save this for pass two of the feeder prep to restore.
                nozzleAndTipForPushing = BlindsFeeder.getNozzleAndTipForPushing(null, true);

                // Actuate the cover.
                actuateCover(true);

                if (!nozzleAndTipForPushing.isChanged()) {
                    // no need to remember
                    nozzleAndTipForPushing = null;
                }
            }
            if (getOcrAction() != OcrAction.None) {
                performOcr(getCamera(), getOcrAction());
            }
        }
        else {
            // in the non-visit preparation step, let's restore the nozzle tip if changed before
            if (nozzleAndTipForPushing != null) {
                nozzleAndTipForPushing.restoreNozzleTipLoadedBefore();
                nozzleAndTipForPushing = null;
            }
            if (ocrChangedPartId != null) {
                // When we changed at least one part through OCR, we must stop the job, because
                // the pre-flight check might now have become invalid.
                // Report all the changed parts.
                StringBuilder msg = new StringBuilder();
                msg.append("OCR changed parts: ");
                for (BlindsFeeder feeder : getAllBlindsFeeders()) {
                    if (feeder.ocrChangedPartId != null) {
                        msg.append(feeder.getClass().getSimpleName());
                        msg.append(" ");
                        msg.append(feeder.getName());
                        msg.append(" part ");
                        msg.append(ocrChangedPartId);
                        msg.append(" changed to ");
                        msg.append(feeder.getPart().getId());
                        msg.append(". ");
                        feeder.ocrChangedPartId = null;
                    }
                }
                msg.append("Please review.");
                throw new Exception(msg.toString());
            }
        }
    }

    public void actuateCover(Nozzle preferredNozzle, boolean openState) throws Exception {
        // If needed, load a NozzleTip that allows pushing but do not restore the previously loaded NozzleTip. 
        actuateCover(preferredNozzle, openState, true, false);
    }

    public void actuateCover(boolean openState) throws Exception {
        actuateCover(null, openState);
    }

    public void actuateCover(Nozzle preferredNozzle, boolean openState, boolean loadNozzleTipIfNeeded, boolean restoreNozzleTip) throws Exception {
        if (coverType == CoverType.NoCover) {
            throw new Exception("Feeder " + getName() + ": has no cover to actuate.");
        }
        else {
            if (location.getZ() == 0.0) {
                throw new Exception("Feeder " + getName() + " Part Z not set.");
            }

            // Get the nozzle for pushing
            NozzleAndTipForPushing nozzleAndTipForPushing = BlindsFeeder.getNozzleAndTipForPushing(preferredNozzle, loadNozzleTipIfNeeded);
            Nozzle nozzle = nozzleAndTipForPushing.getNozzle();
            NozzleTip nozzleTip = nozzleAndTipForPushing.getNozzleTip();
            if (nozzleTip == null) {
                throw new Exception("Feeder " + getName() + 
                        ": loaded nozzle tips do not allow pushing. Check the nozzle tip configuration or change the nozzle tip.");
            }
            Length nozzleTipDiameter = nozzleTip.getDiameterLow(); 
            if (nozzleTipDiameter.getValue() == 0.) {
                throw new Exception("Feeder " + getName() + ": current nozzle tip "+nozzleTip.getId()+
                        " has push diameter not set. Check the nozzle tip configuration.");
            }

            assertCalibration();

            if (coverType == CoverType.BlindsCover) {
                // Calculate the motion for the cover to be pushed in feeder local coordinates. 
                Length feederX0 = (openState ^ isBlindsCoverAlignmentReversed() ? 
                        edgeOpenDistance.multiply(-1.0)
                        .subtract(pocketPitch).multiply(0.5)
                        .subtract(sprocketPitch.multiply(0.5)) // go half sprocket too far back
                        .subtract(nozzleTipDiameter.multiply(0.5)) 
                        : 
                            edgeClosedDistance
                            .add(tapeLength) 
                            .add(pocketPitch).multiply(0.5)
                            .add(sprocketPitch.multiply(0.5)) // go half sprocket too far
                            .add(nozzleTipDiameter.multiply(0.5)))
                        .convertToUnits(location.getUnits());
                Length feederX1 = (openState  ^ isBlindsCoverAlignmentReversed() ? 
                        edgeOpenDistance.multiply(-1.0)
                        .subtract(nozzleTipDiameter.multiply(0.5))
                        : 
                            edgeClosedDistance
                            .add(tapeLength)
                            .add(nozzleTipDiameter.multiply(0.5)))
                        .convertToUnits(location.getUnits());
                Length feederY = pocketCenterline
                        .convertToUnits(location.getUnits());
                Length feederZ = location.getLengthZ().add(pushZOffset);

                Location feederLocation0 = new Location(location.getUnits(), 
                        feederX0.getValue(), 
                        feederY.getValue(), 
                        feederZ.getValue(), 
                        getPickRotationInTape());
                Location feederLocation1 = new Location(location.getUnits(), 
                        feederX1.getValue(), 
                        feederY.getValue(), 
                        feederZ.getValue(), 
                        getPickRotationInTape());

                // Convert to machine locations
                Location machineLocation0 = transformFeederToMachineLocation(feederLocation0);
                Location machineLocation1 = transformFeederToMachineLocation(feederLocation1);

                // Execute the motion
                MovableUtils.moveToLocationAtSafeZ(nozzle, machineLocation0);
                nozzle.moveTo(machineLocation1, nozzle.getHead().getMachine().getSpeed()*pushSpeed);
                nozzle.getHead().moveToSafeZ();
                // Store the new pocket position.
                setCoverPosition(openState ? pocketDistance : pocketDistance.subtract(pocketPitch.multiply(0.5)));
            }
            else if (coverType == CoverType.PushCover && openState) {
                // Calculate the motion for the cover to be pushed in feeder local coordinates.
                Location pickLocation = getPickLocation(getFedPocketNumber()+1);
                Location pickFeederLocation = transformMachineToFeederLocation(pickLocation);
                Length feederX0 = (getFeedCount() == 0 || !isCoverOpen()?
                        pocketPitch.multiply(-0.5)
                        .subtract(nozzleTipDiameter.multiply(1))
                        :
                            coverPosition
                            .subtract(pocketPitch.multiply(0.5))  
                            .subtract(nozzleTipDiameter.multiply(1)))
                        .convertToUnits(location.getUnits());
                Length feederX1 = pickFeederLocation.getLengthX()
                        .add(pocketPitch.multiply(0.5))
                        .subtract(nozzleTipDiameter.multiply(0.5))
                        .convertToUnits(location.getUnits());
                Length feederY = pickFeederLocation.getLengthY()
                        .convertToUnits(location.getUnits());
                Length feederZ = location.getLengthZ().subtract(pushZOffset);

                Location feederLocation0 = new Location(location.getUnits(), 
                        feederX0.getValue(), 
                        feederY.getValue(), 
                        feederZ.getValue(), 
                        getPickRotationInTape());
                Location feederLocation1 = new Location(location.getUnits(), 
                        feederX1.getValue(), 
                        feederY.getValue(), 
                        feederZ.getValue(), 
                        getPickRotationInTape());

                // Convert to machine locations
                Location machineLocation0 = transformFeederToMachineLocation(feederLocation0);
                Location machineLocation1 = transformFeederToMachineLocation(feederLocation1);

                // Execute the motion
                MovableUtils.moveToLocationAtSafeZ(nozzle, machineLocation0);
                nozzle.moveTo(machineLocation1, nozzle.getHead().getMachine().getSpeed()*pushSpeed);
                nozzle.moveTo(pickLocation.derive(machineLocation1,  false, false, true, false));
                // do not: nozzle.getHead().moveToSafeZ();
                // Store the newly uncovered pocket position.
                setCoverPosition(pickFeederLocation.getLengthX());
            }
            if (restoreNozzleTip) {
                nozzleAndTipForPushing.restoreNozzleTipLoadedBefore();
            }
        }
    }

    /**
     * The Machine to Feeder transformation and inverse.
     */
    private AffineTransform tx;
    private AffineTransform txInverse;
    private double txRotation;


    public void invalidateFeederTransformation() {
        tx = null;
        txInverse = null;
    }

    /**
     * Generates the transformation from feeder local coordinate system to 
     * machine coordinates as defined by the fiducials.
     * @return
     */
    private boolean updateFeederToMachineTransform() {
        // Make sure the inverse will be regenerated.
        txInverse = null;
        // Get some basics. 
        Location origin = fiducial1Location;
        double mm = new Length(1, LengthUnit.Millimeters).convertToUnits(origin.getUnits()).getValue();
        double distance = origin.getLinearDistanceTo(fiducial2Location);
        // Check sanity.
        if (! (fiducial1Location.isInitialized() 
                && fiducial2Location.isInitialized()
                && distance > 1*mm)) {
            // Some fiducials not set yet or invalid - just take the unity transform for now.  
            tx = new AffineTransform();
            // Translate for fiducial 1 (if set).
            tx.translate(origin.getX(), origin.getY());
            txRotation = 0.;
            return false;
        }
        if (!normalize) {
            // We know the fiducial distance should be a multiple of 2mm as it is aligned with the 
            // sprockets in the 3D printed model. 
            distance = Math.round(distance/2/mm)*2*mm;
        }
        // Update the tape length to rounded distance.
        setTapeLength(new Length(Math.round(distance/2/mm)*2*mm, origin.getUnits()));

        Location axisX;
        Location axisY; 
        axisX = fiducial2Location.convertToUnits(origin.getUnits()).subtract(origin).multiply(1/distance, 1/distance, 0, 0);

        // Fiducial 3 may be the one across from fiducial 1 or 2 - the nearer one is it. 
        Location ref = (fiducial3Location.convertToUnits(origin.getUnits()).getLinearDistanceTo(fiducial2Location) 
                < fiducial3Location.convertToUnits(origin.getUnits()).getLinearDistanceTo(fiducial1Location) ?
                        fiducial2Location : fiducial1Location).convertToUnits(origin.getUnits());
        distance = fiducial3Location.isInitialized() ? ref.getLinearDistanceTo(fiducial3Location) : 0;
        if (normalize || distance < 1*mm) {
            // We want to normalize or fiducial 3 is not set or has no distance. 
            // Take the cross product of the X axis to form the Y axis (i.e. the fiducial 3 is ignored for the axis).
            axisY = new Location(axisX.getUnits(), -axisX.getY(), axisX.getX(), 0, 0);
            // Fiducial 3 can still serve to get the extent of the overall feeder.
            setFeederExtent(new Length(Math.round(distance/mm)*mm, origin.getUnits()));
        }
        else {
            // We know the fiducial distance should be a multiple of 1mm as this is enforced in the 3D printed model. 
            distance = Math.round(distance/mm)*mm;
            axisY = fiducial3Location.convertToUnits(origin.getUnits()).subtract(ref).multiply(1/distance, 1/distance, 0, 0);
            // Fiducial 3 also serves to get the extent of the overall feeder.
            setFeederExtent(new Length(distance, origin.getUnits()));
        }
        // Finally create the transformation.  
        tx = new AffineTransform(
                axisX.getX(), axisX.getY(), 
                axisY.getX(), axisY.getY(), 
                origin.getX(), origin.getY());
        // Reconstruct the angle at which the feeder lays on the machine. When "normalize" is off, it is only
        // an approximation valid for lines parallel to the tapes (as is most useful). It ignores shear in the
        // transform.
        txRotation = Math.toDegrees(Math.atan2(axisX.getY(), axisX.getX()));

        // recalculate the geometry
        recalculateGeometry();
        return true;
    }

    AffineTransform getFeederToMachineTransform() {
        if (tx == null) {
            updateFeederToMachineTransform();
        }
        return tx;
    }

    double getFeederToMachineRotation() {
        if (tx == null) {
            updateFeederToMachineTransform();
        }
        return txRotation;
    }

    double getMachineToFeederRotation() {
        if (tx == null) {
            updateFeederToMachineTransform();
        }
        return -txRotation;
    }

    private AffineTransform getMachineToFeederTransform() {
        if (txInverse == null) {
            txInverse = new AffineTransform(getFeederToMachineTransform());
            try {
                txInverse.invert();
            }
            catch (NoninvertibleTransformException e) {
                // Should really never happen, as getFeederToMachineTransform() falls back to benign transforms.   
                // Just take the unity transform for now. 
                txInverse = new AffineTransform();
                // Translate from fiducial 1 (if set).
                txInverse.translate(-fiducial1Location.getX(), -fiducial1Location.getY());
            }
        }
        return txInverse;
    }

    public Location transformFeederToMachineLocation(Location feederLocation) {
        AffineTransform tx = getFeederToMachineTransform();
        double rotation = getFeederToMachineRotation();
        feederLocation = feederLocation.convertToUnits(fiducial1Location.getUnits()); 
        Point2D.Double ptDst = new Point2D.Double();
        tx.transform(new Point2D.Double(feederLocation.getX(), feederLocation.getY()), ptDst);
        return new Location(fiducial1Location.getUnits(), ptDst.getX(), ptDst.getY(), 
                feederLocation.getZ(), feederLocation.getRotation()+rotation);
    }

    public Location transformMachineToFeederLocation(Location machineLocation) {
        AffineTransform tx = getMachineToFeederTransform();
        double rotation = getMachineToFeederRotation();
        machineLocation = machineLocation.convertToUnits(fiducial1Location.getUnits()); 
        Point2D.Double ptDst = new Point2D.Double();
        tx.transform(new Point2D.Double(machineLocation.getX(), machineLocation.getY()), ptDst);
        return new Location(fiducial1Location.getUnits(), ptDst.getX(), ptDst.getY(), 
                machineLocation.getZ(), machineLocation.getRotation()+rotation);
    }

    public double transformFeederToMachineAngle(double angle) {
        return angle+getFeederToMachineRotation();
    }
    public double transformMachineToFeederAngle(double angle) {
        // We don't need th reverse transform, a simple sign reversion will do.
        return angle+getMachineToFeederRotation();
    }
    public double transformPixelToFeederAngle(double angle) {
        // Pixel angles are left-handed, coming from the OpenCV coordinate system, where 
        // Z points away from the viewer whereas in OpenPNP the opposite is true. 
        return -transformMachineToFeederAngle(angle);
    }

    public boolean isLocationInFeeder(Location location, boolean fiducial1MatchOnly) {
        // First check if it is a fiducial 1 match.  
        if (!fiducial1Location.isInitialized()) {
            // Never match a uninizialized fiducial.
            return false;
        }
        if (fiducial1Location.convertToUnits(LengthUnit.Millimeters).getLinearDistanceTo(location) < 2) {
            // Direct fiducial 1 match with tolerance.
            return true;
        }
        else if (fiducial1MatchOnly) {
            // No fiducial 1 match but one is required.
            return false;
        }
        // No match on the fiducial 1, so check whole feeder holder area.	
        Location feederLocation = transformMachineToFeederLocation(location);
        double mm = new Length(1, LengthUnit.Millimeters).convertToUnits(feederLocation.getUnits()).getValue();
        if (feederLocation.getX() >= -1*mm && feederLocation.getX() <= tapeLength.convertToUnits(feederLocation.getUnits()).getValue() + 1*mm) {
            if (feederLocation.getY() >= -1*mm && feederLocation.getY() <= feederExtent.getValue() + 1*mm) {
                return true;
            }
        }
        return false;
    }

    public static List<String> getBlindsFeederGroupNames() {
        List<String> list = new ArrayList<>();
        list.add(defaultGroupName);

        for (Feeder feeder : Configuration.get().getMachine().getFeeders()) {
            if (feeder instanceof BlindsFeeder) {
                BlindsFeeder blindsFeeder = (BlindsFeeder) feeder;
                String feederGroupName = blindsFeeder.getFeederGroupName();
                if (!list.contains(feederGroupName)) {
                    list.add(feederGroupName);
                }
            }
        }
        return list;
    }

    public static List<BlindsFeeder> getBlindsFeedersWithGroupName(String groupName) {
        List<BlindsFeeder> list = new ArrayList<>();
        for (Feeder feeder : Configuration.get().getMachine().getFeeders()) {
            if (feeder instanceof BlindsFeeder) {
                BlindsFeeder blindsFeeder = (BlindsFeeder) feeder;
                if(blindsFeeder.getFeederGroupName().equals(groupName)) {
                    list.add(blindsFeeder);
                }
            }
        }
        return list;
    }

    public static List<BlindsFeeder> getAllBlindsFeeders() {
        // Get all the BlindsFeeder instances on the machine.
        List<BlindsFeeder> list = new ArrayList<>();
        for (Feeder feeder : Configuration.get().getMachine().getFeeders()) {
            if (feeder instanceof BlindsFeeder) {
                BlindsFeeder blindsFeeder = (BlindsFeeder) feeder;
                list.add(blindsFeeder);
            }
        }
        return list;
    }

    public static List<BlindsFeeder> getConnectedFeedersByLocation(Location location, boolean fiducial1MatchOnly) {
        // Get all the feeders with connected by location.
        List<BlindsFeeder> list = new ArrayList<>();
        for (Feeder feeder : Configuration.get().getMachine().getFeeders()) {
            if (feeder instanceof BlindsFeeder) {
                BlindsFeeder blindsFeeder = (BlindsFeeder) feeder;
                if (blindsFeeder.isLocationInFeeder(location, fiducial1MatchOnly)) {
                    list.add(blindsFeeder);
                }
            }
        }
        // Sort by feeder tape centerline.
        Collections.sort(list, new Comparator<BlindsFeeder>() {
            @Override
            public int compare(BlindsFeeder feeder1, BlindsFeeder feeder2) {
                return new Double(feeder1.getPocketCenterline().getValue()).compareTo(feeder2.getPocketCenterline()
                        .convertToUnits(feeder1.getPocketCenterline().getUnits()).getValue());
            }
        });
        return list;
    }

    public static List<BlindsFeeder> filterFeedersByGroupName(List<BlindsFeeder> feeders, String groupName) {
        // Get all the feeders with connected by location.
        List<BlindsFeeder> list = new ArrayList<>();
        for (Feeder feeder : feeders) {
            if (feeder instanceof BlindsFeeder) {
                BlindsFeeder blindsFeeder = (BlindsFeeder) feeder;
                if (blindsFeeder.feederGroupName.equals(groupName)) {
                    list.add(blindsFeeder);
                }
            }
        }
        return list;
    }

    public List<BlindsFeeder> getConnectedFeeders() {
        // Get all the feeders with the same fiducial 1 location.
        return getConnectedFeeders(fiducial1Location, true);
    }

    public List<BlindsFeeder> getConnectedFeeders(Location location, boolean fiducial1MatchOnly) {
        List<BlindsFeeder> feeder_list = getConnectedFeedersByLocation(location, fiducial1MatchOnly);
        return filterFeedersByGroupName(feeder_list, this.feederGroupName);
    }

    private boolean isUpdating = false;

    public void updateFromConnectedFeeder(BlindsFeeder feeder) {
        if (this != feeder && ! isUpdating) {
            try {
                isUpdating = true; 
                setFiducial1Location(feeder.fiducial1Location);
                setFiducial2Location(feeder.fiducial2Location);
                setFiducial3Location(feeder.fiducial3Location);
                setTapeLength(feeder.tapeLength);
                setFeederExtent(feeder.feederExtent);
                setNormalize(feeder.normalize);
                if (this.pocketCenterline.equals(feeder.pocketCenterline)) {
                    // The tape is shared by two feeders -> update the pocket position (open/close state) and calibrated edges.
                    // NOTE: the user is responsible to ensure a non-overlapping first pocket/last pocket range 
                    setCoverPosition(feeder.coverPosition);
                    setEdgeOpenDistance(feeder.edgeOpenDistance);
                    setEdgeClosedDistance(feeder.edgeClosedDistance);
                }
                setCalibrated(feeder.calibrated);
                setVisionEnabled(feeder.visionEnabled);
                setFeederGroupNameFromOther(feeder.getFeederGroupName());
                setOcrAction(feeder.getOcrAction());
                setOcrMargin(feeder.getOcrMargin());
                setOcrFontName(feeder.getOcrFontName());
                setOcrFontSizePt(feeder.getOcrFontSizePt());
                setOcrTextOrientation(feeder.getOcrTextOrientation());
                setPipeline(feeder.getPipeline());
            }
            finally {
                isUpdating = false;
            }
        }
    }

    private void updateTapeNumbering()    {
        // Renumber the feeder tape lanes.
        List<BlindsFeeder> list = getConnectedFeeders(fiducial1Location, true);
        int feedersTotal = list.size();
        int feederNo = 0;
        for (BlindsFeeder feeder : list) {
            feeder.setFeedersTotal(feedersTotal);
            feeder.setFeederNo(++feederNo);
        }
    }

    public BlindsFeeder updateFromConnectedFeeder(Location location, boolean fiducial1MatchOnly) {
        // Take the last from the list.
        BlindsFeeder template = null;
        for (BlindsFeeder feeder : getConnectedFeeders(location, fiducial1MatchOnly)) {
            if (feeder != this) {
                template = feeder;
            }
        }
        if (template != null) {
            updateFromConnectedFeeder(template);
        }
        if (fiducial1Location.isInitialized()) {
            // Now that we have the (partial) coordinate system we can calculate the tape pocket centerline.
            Location feederLocation = transformMachineToFeederLocation(location);
            double mm = new Length(1, LengthUnit.Millimeters).convertToUnits(feederLocation.getUnits()).getValue();
            // Take the nearest integer Millimeter value. 
            this.setPocketCenterline(new Length(Math.round(feederLocation.getY()/mm)*mm, feederLocation.getUnits()));
        }
        updateTapeNumbering();
        return template;
    }

    public void updateConnectedFeedersFromThis(Location location, boolean fiducial1MatchOnly) {
        if (! isUpdating) {
            try {
                isUpdating = true;
                // Transform might have changed.
                updateFeederToMachineTransform();
                // Update all the feeders on the same 3D printed holder from this.
                for (BlindsFeeder feeder : getConnectedFeeders(location, fiducial1MatchOnly)) {
                    if (feeder != this) {
                        feeder.updateFromConnectedFeeder(this);
                    }
                }
            }
            finally {
                isUpdating = false;
            }
        }
    }
    public void updateConnectedFeedersFromThis() {
        updateConnectedFeedersFromThis(fiducial1Location, true);
    }

    public CvPipeline getPipeline() {
        return pipeline;
    }

    public void setPipeline(CvPipeline pipeline) {
        CvPipeline oldValue = this.pipeline;
        this.pipeline = pipeline;
        if (oldValue != pipeline) {
            firePropertyChange("pipeline", oldValue, pipeline);
            updateConnectedFeedersFromThis();
        }
    }

    public void resetPipeline() {
        setPipeline(createDefaultPipeline());
    }

    public void setOcrSettingsToAllFeeders() {
        // Update all the BlindsFeeder instances' OCR settings from this one.
        for (BlindsFeeder feeder : getAllBlindsFeeders()) {
            if (feeder != this) {
                setOcrAction(feeder.getOcrAction());
                setOcrMargin(feeder.getOcrMargin());
                setOcrFontName(feeder.getOcrFontName());
                setOcrFontSizePt(feeder.getOcrFontSizePt());
                setOcrTextOrientation(feeder.getOcrTextOrientation());
            }
        }
    }

    public void setPipelineToAllFeeders() throws CloneNotSupportedException {
        // Update all the BlindsFeeder instances' pipeline from this one.
        for (BlindsFeeder feeder : getAllBlindsFeeders()) {
            if (feeder != this) {
                feeder.pipeline = pipeline.clone();
            }
        }
    }

    public CvPipeline getCvPipeline(Camera camera, boolean clone, OcrAction ocrAction) {
        try {
            CvPipeline pipeline = getPipeline();
            if (clone) {
                pipeline = pipeline.clone();
            }
            pipeline.setProperty("camera", camera);
            pipeline.setProperty("feeder", this);
            setupOcr(camera, pipeline, ocrAction);

            /* TODO: read the override property in the FilterContours stage
             * 
             * // Provide pixel min/max area to pipeline.
            // We restrict this to 24mm tape carrier having a 20.1mm max pocket size.
            // See ANSI/EIA-48 1 -C p. 11.
            double mm = VisionUtils.toPixels(new Length(1, LengthUnit.Millimeters), camera);
            Integer minArea = (int) (0.8*mm*1*mm); // 2mm pitch punched paper carrier tape (0402). 
            Integer maxArea = (int) (21*mm*21*mm); // 24mm tape carrier.
            // feeder specific
            double nominalArea = VisionUtils.toPixels(feeder.getPocketPitch(), camera)
             *VisionUtils.toPixels(feeder.getPocketSize(), camera);
            Integer minArea = (int) (0.75*nominalArea); 
            Integer maxArea = (int) (1.25*nominalArea); 
            pipeline.setProperty("FilterContours.minArea", minArea);
            pipeline.setProperty("FilterContours.maxArea", maxArea);
             */
            return pipeline;
        }
        catch (CloneNotSupportedException e) {
            throw new Error(e);
        }
    }

    public boolean isCalibrating() {
        return calibrating;
    }

    private void setCalibrating(boolean calibrating) {
        this.calibrating = calibrating;
    }

    public boolean isCalibrated() {
        return calibrated;
    }

    private void setCalibrated(boolean calibrated) {
        boolean oldValue = this.calibrated;
        this.calibrated = calibrated;
        if (oldValue != calibrated) {
            this.updateConnectedFeedersFromThis();
            firePropertyChange("calibrated", oldValue, calibrated);
        }
    }

    public Location getFiducial1Location() {
        return fiducial1Location;
    }

    public void setFiducial1Location(Location fiducial1Location) {
        Location oldValue = this.fiducial1Location;
        this.fiducial1Location = fiducial1Location;
        if (! oldValue.equals(fiducial1Location)) {
            this.invalidateFeederTransformation();
            firePropertyChange("fiducial1Location", oldValue, fiducial1Location);
            if (! (oldValue.isInitialized() 
                    || fiducial2Location.isInitialized()
                    || fiducial3Location.isInitialized())) {
                // That's an initial fix. Try to clone from another feeder.
                updateFromConnectedFeeder(fiducial1Location, true);
            }
            else {
                this.updateConnectedFeedersFromThis(oldValue, true);
                if (oldValue.isInitialized()
                        && oldValue.convertToUnits(LengthUnit.Millimeters).getLinearDistanceTo(fiducial1Location) > 2) {
                    // Large change in fiducial 1 location - move fiducials 2 and 3 as well (i.e. move the whole feeder).
                    if (fiducial2Location.isInitialized()) {
                        // reset fiducial2Location to prevent handling as rotation
                        Location oldValue2 = fiducial2Location;
                        fiducial2Location = new Location(fiducial2Location.getUnits());
                        setFiducial2Location(oldValue2.add(fiducial1Location.subtract(oldValue)));
                    }
                    if (fiducial3Location.isInitialized()) {
                        setFiducial3Location(fiducial3Location.add(fiducial1Location.subtract(oldValue)));
                    }
                }
            }
        }
    }

    public Location getFiducial2Location() {
        return fiducial2Location;
    }

    public void setFiducial2Location(Location fiducial2Location) {
        Location oldValue = this.fiducial2Location;
        this.fiducial2Location = fiducial2Location;
        if (! oldValue.equals(fiducial2Location)) {
            this.invalidateFeederTransformation();
            this.updateConnectedFeedersFromThis();
            firePropertyChange("fiducial2Location", oldValue, fiducial2Location);
            if (oldValue.isInitialized()
                    && oldValue.convertToUnits(LengthUnit.Millimeters).getLinearDistanceTo(fiducial1Location) > 2) {
                // Large change in fiducial 2 location - rotate fiducial 3 (i.e. rotate the whole feeder).
                if (fiducial3Location.isInitialized() && fiducial1Location.isInitialized()) {
                    // new unit vectors (rotated)
                    Location unitX = fiducial1Location.unitVectorTo(fiducial2Location);
                    Location unitY = new Location(unitX.getUnits(), -unitX.getY(), unitX.getX(), 0.0, 0.0);
                    if (fiducial1Location.getLinearDistanceTo(fiducial3Location) < 
                            oldValue.getLinearDistanceTo(fiducial3Location)) {
                        // Fiducial 3 is at front
                        double feederExtent = fiducial3Location.getLinearDistanceTo(fiducial1Location);
                        setFiducial3Location(fiducial1Location.add(unitY.multiply(feederExtent, feederExtent, 0.0, 0.0)));
                    }
                    else {
                        // Fiducial 3 is at back
                        double feederExtent = fiducial3Location.convertToUnits(unitX.getUnits()).getLinearDistanceTo(oldValue);
                        setFiducial3Location(fiducial2Location.add(unitY.multiply(feederExtent, feederExtent, 0.0, 0.0)));
                    }
                }
            }
        }
    }

    public Location getFiducial3Location() {
        return fiducial3Location;
    }

    public void setFiducial3Location(Location fiducial3Location) {
        Location oldValue = this.fiducial3Location;
        this.fiducial3Location = fiducial3Location;
        if (! oldValue.equals(fiducial3Location)) {
            this.invalidateFeederTransformation();
            this.updateConnectedFeedersFromThis();
            firePropertyChange("fiducial3Location", oldValue, fiducial3Location);
        }
    }

    public boolean isNormalize() {
        return normalize;
    }

    public void setNormalize(boolean normalize) {
        boolean oldValue = this.normalize;
        this.normalize = normalize;
        if (oldValue != normalize) {
            this.invalidateFeederTransformation();
            this.updateConnectedFeedersFromThis();
            firePropertyChange("normalize", oldValue, normalize);
        }
    }

    public Length getTapeLength() {
        return tapeLength;
    }


    public void setTapeLength(Length tapeLength) {
        Length oldValue = this.tapeLength;
        this.tapeLength = tapeLength;
        if (! oldValue.equals(tapeLength)) {
            this.updateConnectedFeedersFromThis();
            firePropertyChange("tapeLength", oldValue, tapeLength);
        }
    }


    public Length getFeederExtent() {
        return feederExtent;
    }


    public void setFeederExtent(Length feederExtent) {
        Length oldValue = this.feederExtent;
        this.feederExtent = feederExtent;
        if (! oldValue.equals(feederExtent)) {
            this.updateConnectedFeedersFromThis();
            firePropertyChange("feederExtent", oldValue, feederExtent);
        }
    }


    public Length getPocketCenterline() {
        return pocketCenterline;
    }


    public void setPocketCenterline(Length pocketCenterline) {
        Length oldValue = this.pocketCenterline;
        this.pocketCenterline = pocketCenterline;
        firePropertyChange("pocketCenterline", oldValue, pocketCenterline);
        updateTapeNumbering();
    }


    public Length getPocketPitch() {
        return pocketPitch;
    }


    public void setPocketPitch(Length pocketPitch) {
        Length oldValue = this.pocketPitch;
        this.pocketPitch = pocketPitch;
        firePropertyChange("pocketPitch", oldValue, pocketPitch);
    }


    public Length getPocketSize() {
        return pocketSize;
    }


    public void setPocketSize(Length pocketSize) {
        Length oldValue = this.pocketSize;
        this.pocketSize = pocketSize;
        firePropertyChange("pocketSize", oldValue, pocketSize);
    }

    public int getPocketCount() {
        return pocketCount;
    }

    public void setPocketCount(int pocketCount) {
        int oldValue = this.pocketCount;
        this.pocketCount = pocketCount;
        firePropertyChange("pocketCount", oldValue, pocketCount);
        // adapt the first and last pocket to a new count
        this.setFirstPocket(Math.max(1, Math.min(pocketCount, this.getFirstPocket()))); 
        this.setLastPocket(Math.min(pocketCount, Math.max(this.getFirstPocket(), this.getLastPocket() - oldValue + pocketCount))); 
    }

    public int getFirstPocket() {
        return firstPocket;
    }

    public void setFirstPocket(int firstPocket) {
        int oldValue = this.firstPocket;
        this.firstPocket = firstPocket;
        firePropertyChange("firstPocket", oldValue, firstPocket);
    }

    public int getLastPocket() {
        return lastPocket;
    }

    public void setLastPocket(int lastPocket) {
        int oldValue = this.lastPocket;
        this.lastPocket = lastPocket;
        firePropertyChange("lastPocket", oldValue, lastPocket);
    }

    public Length getPocketDistance() {
        return pocketDistance;
    }

    public void setPocketDistance(Length pocketDistance) {
        Length oldValue = this.pocketDistance;
        this.pocketDistance = pocketDistance;
        firePropertyChange("pocketDistance", oldValue, pocketDistance);
    }

    public Length getCoverPosition() {
        return coverPosition;
    }

    public void setCoverPosition(Length coverPosition) {
        Length oldValue = this.coverPosition;
        this.coverPosition = coverPosition;
        if (!(oldValue.equals(coverPosition)
                || Double.isNaN(oldValue.getValue()) == Double.isNaN(coverPosition.getValue()))) {
            firePropertyChange("coverPosition", oldValue, coverPosition);
            this.updateConnectedFeedersFromThis();
        }
    }

    public String getFeederGroupName() {
        if (locationGroupNamesList.contains(feederGroupName)) {
            return defaultGroupName;
        }
        return feederGroupName;
    }

    public void setFeederGroupName(String newFeederGroupName) {
        // Filter out a no change to group name.
        if (this.feederGroupName.equals(newFeederGroupName)) {
            return;
        }

        String oldName = this.feederGroupName;
        String proposedGroupName = newFeederGroupName;

        // Check if the group name is one of the location keywords. If so reset name to
        // default location.
        if (locationGroupNamesList.contains(newFeederGroupName.toUpperCase())) {
            proposedGroupName = defaultGroupName;
        }

        List<BlindsFeeder> connected_feeders = getConnectedFeeders();
        List<BlindsFeeder> feedersWithNewGroupName = getBlindsFeedersWithGroupName(proposedGroupName);
        List<String> feederGroupNames = getBlindsFeederGroupNames();

        boolean proposedIsDefault = proposedGroupName.contentEquals(defaultGroupName);
        boolean proposedIsExistingGroup = feederGroupNames.contains(proposedGroupName);

        boolean locationIsNull = !(fiducial1Location.isInitialized() && fiducial2Location.isInitialized() && fiducial3Location.isInitialized());

        boolean canJoinNamedGroup = (locationIsNull && (feedersWithNewGroupName.size() > 0));

        boolean canRenameGroup = (!locationIsNull && !proposedIsDefault && !proposedIsExistingGroup);

        boolean singleCanJoinNamedGroup = (!locationIsNull && feederGroupNames.contains(proposedGroupName)
                && (connected_feeders.size() <= 1));

        boolean canLeaveGroupForDefault = (!locationIsNull && feederGroupNames.contains(oldName)
                && proposedIsDefault);

        if (canJoinNamedGroup) {
            BlindsFeeder copyFeeder = feedersWithNewGroupName.get(0);
            this.updateFromConnectedFeeder(copyFeeder);
        } else if (singleCanJoinNamedGroup) {
            BlindsFeeder copyFeeder = feedersWithNewGroupName.get(0);
            this.updateFromConnectedFeeder(copyFeeder);
        } else if (canRenameGroup) {
            for (BlindsFeeder feeder : connected_feeders) {
                feeder.setFeederGroupNameFromOther(proposedGroupName);
            }
        } else if (canLeaveGroupForDefault) {
            proposedGroupName = defaultGroupName;
            this.feederGroupName = proposedGroupName;
        } else {
            proposedGroupName = oldName;
        }

        // Ensure this feeder group is changed if connected feeders is empty.
        firePropertyChange("feederGroupName", oldName, proposedGroupName);
    }

    public void setFeederGroupNameFromOther(String newFeederGroupName) {
        this.feederGroupName = newFeederGroupName;
    }

    public int getFeedCount() {
        return feedCount;
    }

    public void setFeedCount(int feedCount) {
        int oldValue = this.feedCount;
        this.feedCount = feedCount;
        firePropertyChange("feedCount", oldValue, feedCount);
    }

    public boolean isVisionEnabled() {
        return visionEnabled;
    }

    public void setVisionEnabled(boolean visionEnabled) {
        boolean oldValue = this.visionEnabled;
        this.visionEnabled = visionEnabled;
        if (oldValue != visionEnabled) {
            firePropertyChange("visionEnabled", oldValue, visionEnabled);
            this.updateConnectedFeedersFromThis();
        }
    }

    public int getFeederNo() {
        return feederNo;
    }

    public void setFeederNo(int feederNo) {
        int oldValue = this.feederNo;
        this.feederNo = feederNo;
        firePropertyChange("feederNo", oldValue, feederNo);
    }


    public int getFeedersTotal() {
        return feedersTotal;
    }

    public Length getEdgeOpenDistance() {
        return edgeOpenDistance;
    }

    public void setEdgeOpenDistance(Length edgeOpenDistance) {
        Length oldValue = this.edgeOpenDistance;
        this.edgeOpenDistance = edgeOpenDistance;
        if (! oldValue.equals(edgeOpenDistance)) {
            this.updateConnectedFeedersFromThis();
            firePropertyChange("edgeOpenDistance", oldValue, edgeOpenDistance);
        }
    }

    public Length getEdgeClosedDistance() {
        return edgeClosedDistance;
    }

    public void setEdgeClosedDistance(Length edgeClosedDistance) {
        Length oldValue = this.edgeClosedDistance;
        this.edgeClosedDistance = edgeClosedDistance;
        if (! oldValue.equals(edgeClosedDistance)) {
            this.updateConnectedFeedersFromThis();
            firePropertyChange("edgeClosedDistance", oldValue, edgeClosedDistance);
        }
    }

    public double getPushSpeed() {
        return pushSpeed;
    }

    public void setPushSpeed(double pushSpeed) {
        this.pushSpeed = pushSpeed;
    }

    public Length getPushZOffset() {
        return pushZOffset;
    }

    public void setPushZOffset(Length pushZOffset) {
        Length oldValue = this.pushZOffset;
        this.pushZOffset = pushZOffset;
        firePropertyChange("pushZOffset", oldValue, pushZOffset);
    }

    public CoverType getCoverType() {
        return coverType;
    }

    public void setCoverType(CoverType coverType) {
        this.coverType = coverType;
    }

    public CoverActuation getCoverActuation() {
        return coverActuation;
    }

    public void setCoverActuation(CoverActuation coverActuation) {
        this.coverActuation = coverActuation;
    }

    public void setFeedersTotal(int feedersTotal) {
        int oldValue = this.feedersTotal;
        this.feedersTotal = feedersTotal;
        firePropertyChange("feedersTotal", oldValue, feedersTotal);
    }

    public OcrAction getOcrAction() {
        return ocrAction;
    }

    public void setOcrAction(OcrAction ocrAction) {
        OcrAction oldValue = this.ocrAction;
        this.ocrAction = ocrAction;
        if (oldValue != ocrAction) {
            firePropertyChange("ocrAction", oldValue, ocrAction);
            this.updateConnectedFeedersFromThis();
        }
    }

    public Length getOcrMargin() {
        return ocrMargin;
    }

    public void setOcrMargin(Length ocrMargin) {
        Length oldValue = this.ocrMargin;
        this.ocrMargin = ocrMargin;
        if (oldValue != ocrMargin) {
            firePropertyChange("ocrMargin", oldValue, ocrMargin);
            this.updateConnectedFeedersFromThis();
        }
    }

    public String getOcrFontName() {
        return ocrFontName;
    }

    public void setOcrFontName(String ocrFontName) {
        String oldValue = this.ocrFontName;
        this.ocrFontName = ocrFontName;
        if (oldValue != ocrFontName) {
            firePropertyChange("ocrFontName", oldValue, ocrFontName);
            this.updateConnectedFeedersFromThis();
        }
    }

    public double getOcrFontSizePt() {
        return ocrFontSizePt;
    }

    public void setOcrFontSizePt(double ocrFontSizePt) {
        double oldValue = this.ocrFontSizePt;
        this.ocrFontSizePt = ocrFontSizePt;
        if (oldValue != ocrFontSizePt) {
            firePropertyChange("ocrFontSizePt", oldValue, ocrFontSizePt);
            this.updateConnectedFeedersFromThis();
        }
    }

    public OcrTextOrientation getOcrTextOrientation() {
        return ocrTextOrientation;
    }

    public void setOcrTextOrientation(OcrTextOrientation ocrTextOrientation) {
        OcrTextOrientation oldValue = this.ocrTextOrientation;
        this.ocrTextOrientation = ocrTextOrientation;
        if (oldValue != ocrTextOrientation) {
            firePropertyChange("ocrTextOrientation", oldValue, ocrTextOrientation);
            this.updateConnectedFeedersFromThis();
        }
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new BlindsFeederConfigurationWizard(this);
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName() + " " + getName();
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {
                new PropertySheetWizardAdapter(getConfigurationWizard(), "Configuration"),
                new PropertySheetWizardAdapter(new BlindsFeederArrayConfigurationWizard(this), "Feeder Array"),
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

    private static CvPipeline createDefaultPipeline() {
        try {
            String xml = IOUtils.toString(BlindsFeeder.class
                    .getResource("BlindsFeeder-DefaultPipeline.xml"));
            return new CvPipeline(xml);
        }
        catch (Exception e) {
            throw new Error(e);
        }
    }

    @Override
    public void applyLocationOffset(Location offset) throws Exception {
        super.applyLocationOffset(offset);

        // Note: keeping 'live' code, rather than commenting out, so these usages do not rot and can be found.
        if (false) {
            // this feeder has some custom behavior, so we can't do this
            setFiducial1Location(getFiducial1Location().addWithRotation(offset));
            setFiducial2Location(getFiducial2Location().addWithRotation(offset));
            setFiducial3Location(getFiducial3Location().addWithRotation(offset));
        } else {
            // instead we have to do it like this
            Location oldFiducial1Location = this.fiducial1Location;
            fiducial1Location = fiducial1Location.addWithRotation(offset);
            firePropertyChange("fiducial1Location", oldFiducial1Location, fiducial1Location);

            Location oldFiducial2Location = this.fiducial2Location;
            fiducial2Location = fiducial2Location.addWithRotation(offset);
            firePropertyChange("fiducial2Location", oldFiducial2Location, fiducial2Location);

            Location oldFiducial3Location = this.fiducial3Location;
            fiducial3Location = fiducial3Location.addWithRotation(offset);
            firePropertyChange("fiducial3Location", oldFiducial3Location, fiducial3Location);

            this.invalidateFeederTransformation();
        }
    }
}

