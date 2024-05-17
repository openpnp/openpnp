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

package org.openpnp.util;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.openpnp.gui.MainFrame;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.vision.FluentCv;
import org.openpnp.vision.Ransac;
import org.openpnp.vision.Ransac.Line;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.CvStage.Result;
import org.openpnp.vision.pipeline.stages.SimpleOcr;
import org.pmw.tinylog.Logger;

/*
 * This is a helper class for locating the tape holes and pockets.
 * It is mostly used in feeders with large pick windows exposing at least two holes.
 * Calculates the exact location of the part pocket according to the EIA 481 standard.
 */

public class FeederVisionHelper {

    public enum FindFeaturesMode {
      FromPickLocationGetHoles,
      CalibrateHoles,
      OcrOnly
    }

    // initial calibration tolerance, i.e. how much the feeder can be shifted physically
    private double calibrationToleranceMm = 1.95;
    // vision and comparison sprocket hole tolerance (in size, position)
    private double sprocketHoleToleranceMm = 0.6;

    // Some EIA 481 standard constants.
    static final double sprocketHoleDiameterMm = 1.5;
    static final double sprocketHolePitchMm = 4;
    static final double minSprocketHolesDistanceMm = 3.5;

    // tape parameters
    private boolean normalizePickLocation = true;
    private boolean snapToAxis = false;

    private Location partLocation = new Location(LengthUnit.Millimeters);
    private Location hole1Location = new Location(LengthUnit.Millimeters);
    private Location hole2Location = new Location(LengthUnit.Millimeters);

    private Length partPitch = new Length(4, LengthUnit.Millimeters);
    private Length feedPitch = new Length(4, LengthUnit.Millimeters);


    private Camera camera;
    private CvPipeline pipeline;
    private long showResultMilliseconds;
    private FindFeaturesMode autoSetupMode;

    private Location calibratedVisionOffset;
    private Location calibratedHole1Location;
    private Location calibratedHole2Location;
    private Location calibratedPickLocation;
    // OCR detection
    private SimpleOcr.OcrModel detectedOcrModel;

    // RotationInFeeder is not required by the Helper class as it is only needed to determine the final Pick Location
    // This Helper class does not provide a public method for Pick Location and only determines it internally for drawing purposes exclusively, where rotation is considered 0
    // Rotation of the part within the feeder (i.e. within the tape)
//  // private Double rotationInFeeder = Double.valueOf(0.0);

    // recognized stuff
    private List<Result.Circle> holes;
    private List<Line> lines;

    public FeederVisionHelper(Camera camera, CvPipeline pipeline, final long showResultMilliseconds, FindFeaturesMode autoSetupMode) {
        this.camera = camera;
        this.pipeline = pipeline;
        this.showResultMilliseconds = showResultMilliseconds;
        this.autoSetupMode = autoSetupMode;
    }

    private void setTapeSpecs(boolean normalizePickLocation, boolean snapToAxis, Length partPitch, Length feedPitch, Location partLocation, Location hole1Location, Location hole2Location) {
      this.normalizePickLocation = normalizePickLocation;
      this.snapToAxis = snapToAxis;
      this.partPitch = partPitch;
      this.feedPitch = feedPitch;
      this.hole1Location = hole1Location;
      this.hole2Location = hole2Location;
      this.partLocation = partLocation;
    }

    private void setCalibrationTolerance(double calibrationToleranceMm, double sprocketHoleToleranceMm) {
      this.calibrationToleranceMm = calibrationToleranceMm;
      this.sprocketHoleToleranceMm = sprocketHoleToleranceMm;
}

    public long getPartsPerFeedCycle() {
        long feedsPerPart = (long)Math.ceil(partPitch.divide(feedPitch));
        return Math.round(1*Math.ceil(feedsPerPart*feedPitch.divide(partPitch)));
    }

    public Length getTapeWidth() {
      // infer the tape width from EIA-481 where:
      // 1) tape edge to hole middle = 1.75mm
      // 2) hole middle to part middle is 3.5 / 5.5 / 7.5 / 11.5mm for tape of 8 / 12 / 16 / 24mm width
      // 3) therefore
      // 3) a) hole middle to part middle + 0.5mm = half of tape width
      // 3) b) (hole middle to part middle + 0.5mm) * 2 = tapeWidth
        Location hole1Location = transformMachineToFeederLocation(this.hole1Location, null)
                .convertToUnits(LengthUnit.Millimeters);
        final double partToSprocketHoleHalfTapeWidthDiffMm = 0.5; // deducted from EIA-481
        double tapeWidth = Math.round(hole1Location.getY()+partToSprocketHoleHalfTapeWidthDiffMm)*2;
        return new Length(tapeWidth, LengthUnit.Millimeters);
    }

    public Location getCalibratedVisionOffset() {
      return this.calibratedVisionOffset;
    }

    public Location getCalibratedHole1Location() {
      return this.calibratedHole1Location;
    }

    public Location getCalibratedHole2Location() {
      return this.calibratedHole2Location;
    }

    public Location getCalibratedPickLocation() {
      return this.calibratedPickLocation;
    }

    public SimpleOcr.OcrModel getDetectedOcrModel() {
      return this.detectedOcrModel;
    }


    public static Location forwardTransform(Location location, Location transform) {
        return location.rotateXy(transform.getRotation()).addWithRotation(transform);
    }

    public static Location backwardTransform(Location location, Location transform) {
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

        Location unitVector = hole1Location.unitVectorTo(hole2Location);
        if (!(Double.isFinite(unitVector.getX()) && Double.isFinite(unitVector.getY()))) {
            // Catch (yet) undefined hole locations.
            unitVector = new Location(hole1Location.getUnits(), 0, 1, 0, 0);
        }
        double rotationTape = Math.atan2(unitVector.getY(), unitVector.getX())*180.0/Math.PI;
        Location transform = this.partLocation.derive(null, null, null, rotationTape);
        if (Math.abs(rotationTape - this.partLocation.getRotation()) > 0.1) {
            // HACK: something is not up-to-date -> refresh
            this.partLocation = transform;
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


    private Location getPartLocation(long partInCycle, Location visionOffset)  {
        // If the feeder is advancing more than one part per feed cycle (e.g. with 2mm pitch tape or if a multiplier is
        // given), we need to cycle through multiple pick locations. partInCycle is 1-based and goes to getPartsPerFeedCycle().
        long offsetPitches = (getPartsPerFeedCycle() - partInCycle) % getPartsPerFeedCycle();
        Location feederLocation = new Location(partPitch.getUnits(), partPitch.multiply((double)offsetPitches).getValue(),
                0, 0, 0);
        Location machineLocation = transformFeederToMachineLocation(feederLocation, visionOffset);
        return machineLocation;
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
            Imgproc.circle(mat, c, (int) (circle.diameter+0.5)/2, FluentCv.colorToScalar(color), 2, Imgproc.LINE_AA);
            Imgproc.circle(mat, c, 1, FluentCv.colorToScalar(color), 3, Imgproc.LINE_AA);
        }
    }

    private void drawLines(Mat mat, List<Line> lines, Color color) {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        for (Line line : lines) {
            Imgproc.line(mat, line.a, line.b, FluentCv.colorToScalar(color), 2, Imgproc.LINE_AA);
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
        double feederPocketPitchMm =  partPitch.convertToUnits(LengthUnit.Millimeters).getValue();
        if (feederPocketPitchMm < 1.) {
            // feeder not set up yet
            return;
        }

        // calculate the diagonal text size
        double fontScale = 1.0;
        Size size = Imgproc.getTextSize(String.valueOf(getPartsPerFeedCycle()),
                Imgproc.FONT_HERSHEY_PLAIN, fontScale, 2, baseLine);
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
        // go through all the parts, step-wise
        for (int i = step; i <= getPartsPerFeedCycle(); i += step) {
            String text = String.valueOf(i);
            Size textSize = Imgproc.getTextSize(text, Imgproc.FONT_HERSHEY_PLAIN, fontScale, 2, baseLine);

            Location partLocation = getPartLocation(i, calibratedVisionOffset)
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
                        Imgproc.FONT_HERSHEY_PLAIN,
                        fontScale,
                        FluentCv.colorToScalar(color), 2, 0, false);
            }
        }
    }

    public FeederVisionHelper findFeatures(boolean normalizePickLocation, boolean snapToAxis, Length partPitch, Length feedPitch, Location partLocation, Location hole1Location, Location hole2Location)
        throws Exception {
      return findFeatures(normalizePickLocation, snapToAxis, partPitch, feedPitch, partLocation, hole1Location, hole2Location
            ,this.calibrationToleranceMm, this.sprocketHoleToleranceMm);
    }

/*
 * This method will use Computer Vision to detect and calculate EIA-481 standard tape features - holes & part pockets
 * - detects the tape holes first as they are defined in the standard, 4mm apart
 * - calculates the tape width based on the distance between the initial nozzle location and where the holes were found
 * - determines the exact location for the part pocket center point based on EIA-481 specs and the input parameter partPitch
 * Notes:
 * - normalizePickLocation: Normalize the pick location relative to the tape holes according to the EIA-481 standard
 * - snapToAxis: Snap rows of tape holes to the Axis parallel
 * - partPitch: Pitch of the parts in the tape (2mm, 4mm, 8mm, 12mm, etc.)
 * - feedPitch: How much the tape will be advanced by one actuation (usually multiples of 4mm)
 *    - if partPitch > feedPitch then multiple actuations will be executed
 * - partLocation: approximate location of the part pocket as set using jogging the head
 * - hole1Location, hole2Location: provide if known (maybe from previous calibration), critical only to the "OcrOnly" mode
 * - calibrationToleranceMm, sprocketHoleToleranceMm: calibration tolerance parameters, either used from internal constants or provided by the caller class
 */
    public FeederVisionHelper findFeatures(boolean normalizePickLocation, boolean snapToAxis, Length partPitch, Length feedPitch, Location partLocation, Location hole1Location, Location hole2Location
        ,double calibrationToleranceMm, double sprocketHoleToleranceMm)
        throws Exception {
      setTapeSpecs(normalizePickLocation, snapToAxis, partPitch, feedPitch, partLocation, hole1Location, hole2Location);
      setCalibrationTolerance(calibrationToleranceMm, sprocketHoleToleranceMm);
        List resultsList = null;

        try {
            // in accordance with EIA-481 etc. we use all millimeters.
            Location mmScale = camera.getUnitsPerPixelAtZ()
                    .convertToUnits(LengthUnit.Millimeters);
            // reset the features
            holes = new ArrayList<>();
            lines = new ArrayList<>();

            if (autoSetupMode == FindFeaturesMode.OcrOnly) {
                // No vision calibration wanted - just copy the pre-set locations
                calibratedHole1Location = hole1Location;
                calibratedHole2Location = hole2Location;
                calibratedPickLocation  = this.partLocation;
            }
            else {
                final double partPitchMinMm = 2;
                final double sprocketHoleToPartMinMm = 3.5; // sprocket hole to part @ 8mm
                final double sprocketHoleToPartGridMm = 2;  // +multiples of 2mm for wider tapes
                final double sprocketHoleDiameterPx = sprocketHoleDiameterMm/mmScale.getX();
                final double sprocketHolePitchPx = sprocketHolePitchMm/mmScale.getX();
                final double sprocketHoleTolerancePx = sprocketHoleToleranceMm/mmScale.getX();
                // Grab the results
                resultsList = pipeline.getExpectedResult(VisionUtils.PIPELINE_RESULTS_NAME)
                        .getExpectedModel(List.class);

                // Convert eligible results into circles
                List<CvStage.Result.Circle> results = new ArrayList<>();;
                for (Object result : resultsList) {
                    if ((result) instanceof Result.Circle) {
                        Result.Circle circle = ((Result.Circle) result);
                        if (Math.abs(circle.diameter*mmScale.getX() - sprocketHoleDiameterMm) < sprocketHoleToleranceMm) {
                            results.add(circle);
                        }
                        else {
                            Logger.debug("Dismissed Circle with non-compliant diameter "+(circle.diameter*mmScale.getX())+"mm, "
                                    + "allowed tolerance is ±"+sprocketHoleToleranceMm+"mm");
                        }
                    }
                    else if ((result) instanceof RotatedRect) {
                        RotatedRect rect = ((RotatedRect) result);
                        double diameter = (rect.size.width+rect.size.height)/2.0;
                        if (Math.abs(rect.size.width*mmScale.getX() - sprocketHoleDiameterMm) < sprocketHoleToleranceMm
                                && Math.abs(rect.size.height*mmScale.getX() - sprocketHoleDiameterMm) < sprocketHoleToleranceMm) {
                            results.add(new Result.Circle(rect.center.x, rect.center.y, diameter));
                        }
                        else {
                            Logger.debug("Dismissed RotatedRect with non-compliant width or height "
                                    +(rect.size.width*mmScale.getX())+"mm x "+rect.size.height*mmScale.getX()+"mm, "
                                            + "allowed tolerance is ±"+sprocketHoleToleranceMm+"mm");
                        }
                    }
                    else if ((result) instanceof KeyPoint) {
                        KeyPoint keyPoint = ((KeyPoint) result);
                        results.add(new Result.Circle(keyPoint.pt.x, keyPoint.pt.y, sprocketHoleDiameterPx));
                    }
                }

                // collect the circles into a list of points
                List<Point> points = new ArrayList<>();
                for (Result.Circle circle : results) {
                    points.add(new Point(circle.x, circle.y));
                }
                List<Ransac.Line> ransacLines = Ransac.ransac(points, 100, sprocketHoleTolerancePx,
                        sprocketHolePitchPx, sprocketHoleTolerancePx, false);
                if (ransacLines.isEmpty()) {
                    Logger.debug("Ransac algorithm has not found any lines of sprocket holes with "+sprocketHolePitchMm+"mm pitch, "
                            + "allowed pitch and line tolerance is ±"+sprocketHoleToleranceMm+"mm");
                }
                // Get the best line within the calibration tolerance
                Ransac.Line bestLine = null;
                Location bestUnitVector = null;
                double bestDistanceMm = Double.MAX_VALUE;
                for (Ransac.Line line : ransacLines) {
                    Point a = line.a;
                    Point b = line.b;

                    Location aLocation = VisionUtils.getPixelLocation(camera, a.x, a.y);
                    Location bLocation = VisionUtils.getPixelLocation(camera, b.x, b.y);

                    // Checks the distance to the line.
                    // In Auto-Setup/Preview mode we go from the pick location and there must be a minimum distance
                    // in order not to confuse pockets for sprocket holes. But then take the closest one, in order not
                    // to confuse with the neighboring tape's holes. We assume the pick location is always closer to our
                    // sprocket holes than to the neighboring tape's holes.
                    // In Calibration mode we are between the the sprocket holes, and there is no minimum distance
                    // and the line must simply be within calibration tolerance.
                    double distanceMm = camera.getLocation().convertToUnits(LengthUnit.Millimeters)
                            .getLinearDistanceToLineSegment(aLocation, bLocation);
                    double minDistanceMm = (autoSetupMode == FindFeaturesMode.CalibrateHoles ?
                            0 : minSprocketHolesDistanceMm)
                            - sprocketHoleToleranceMm;
                    double maxDistanceMm = (autoSetupMode == FindFeaturesMode.CalibrateHoles ?
                            calibrationToleranceMm : bestDistanceMm);

                    if (distanceMm >= minDistanceMm && distanceMm < maxDistanceMm) {
                        bestLine = line;
                        bestUnitVector = aLocation.unitVectorTo(bLocation);
                        bestDistanceMm = distanceMm;
                        lines.add(line);
                        if (autoSetupMode == FindFeaturesMode.CalibrateHoles) {
                            // Take the first line that is close enough, as the lines are ordered by length (descending).
                            break;
                        }
                        // Otherwise take the closest line, go on.
                    }
                    else if (autoSetupMode == null) {
                        lines.add(line);
                        Logger.debug("Dismissed line by distance, "+(distanceMm)+"mm, not within "+minDistanceMm+"mm .. "+maxDistanceMm+"mm");
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

                    if (autoSetupMode == FindFeaturesMode.FromPickLocationGetHoles
                            || (autoSetupMode == null && !(hole1Location.isInitialized() && hole2Location.isInitialized()))) {
                        // because we sorted the holes by distance, the first two are our holes 1 and 2
                        if (holes.size() < 2) {
                            throw new Exception("At least two sprocket holes need to be recognized");
                        }
                        calibratedHole1Location = VisionUtils.getPixelLocation(camera, holes.get(0).x, holes.get(0).y)
                                .convertToUnits(LengthUnit.Millimeters);
                        calibratedHole2Location = VisionUtils.getPixelLocation(camera, holes.get(1).x, holes.get(1).y)
                                .convertToUnits(LengthUnit.Millimeters);
                        Location pocketLocation = camera.getLocation().convertToUnits(LengthUnit.Millimeters);
                        double angle1 = Math.atan2(calibratedHole1Location.getY()-pocketLocation.getY(), calibratedHole1Location.getX()-pocketLocation.getX());
                        double angle2 = Math.atan2(calibratedHole2Location.getY()-pocketLocation.getY(), calibratedHole2Location.getX()-pocketLocation.getX());
                        double angleDiff = Utils2D.angleNorm(Math.toDegrees(angle2-angle1), 180);
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
                                .derive(this.partLocation, false, false, true, false) // previous Z
                                .derive(null,  null, null, angleTape); // preliminary feeeder orientation
                    }
                    else {
                        // find the two holes matching
                        for (Result.Circle hole : holes) {
                            Location l = VisionUtils.getPixelLocation(camera, hole.x, hole.y)
                                    .convertToUnits(LengthUnit.Millimeters);
                            double dist1Mm = l.getLinearDistanceTo(hole1Location);
                            double dist2Mm = l.getLinearDistanceTo(hole2Location);
                            if (dist1Mm < calibrationToleranceMm && dist1Mm < dist2Mm) {
                                calibratedHole1Location = l;
                            }
                            else if (dist2Mm < calibrationToleranceMm && dist2Mm < dist1Mm) {
                                calibratedHole2Location = l;
                            }
                        }
                        if (calibratedHole1Location == null || calibratedHole2Location == null) {
                            if (autoSetupMode  == FindFeaturesMode.CalibrateHoles) {
                                throw new Exception("The two reference sprocket holes cannot be recognized");
                            }
                        }
                        else {
                            if (calibratedHole1Location.unitVectorTo(calibratedHole2Location)
                                    .dotProduct(bestUnitVector).getValue() < 0.0) {
                                // turn the unit vector around
                                bestUnitVector = bestUnitVector.multiply(-1.0, -1.0, 0, 0);
                            }
                            if (this.snapToAxis) {
                                if (Math.abs(bestUnitVector.getX()) > Math.abs(bestUnitVector.getY())*5) {
                                    // close enough, snap to X
                                    bestUnitVector = new Location(LengthUnit.Millimeters, Math.signum(bestUnitVector.getX()), 0, 0, 0);
                                }
                                else if (Math.abs(bestUnitVector.getY()) > Math.abs(bestUnitVector.getX())*5) {
                                    // close enough, snap to Y
                                    bestUnitVector = new Location(LengthUnit.Millimeters, 0, Math.signum(bestUnitVector.getY()), 0, 0);
                                }
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
                            Logger.trace("[TapeUtils] calibrated hole locations are: " + calibratedHole1Location + ", " +calibratedHole2Location);
                            if (autoSetupMode  == FindFeaturesMode.CalibrateHoles) {
                                // get the current pick location relative to hole 1
                                Location pickLocation = this.partLocation.convertToUnits(LengthUnit.Millimeters);
                                Location relativePickLocation = pickLocation
                                        .subtract(hole1Location);
                                // rotate from old angle
                                relativePickLocation =  relativePickLocation.rotateXy(-pickLocation.getRotation())
                                        .derive(null, null, null, 0.0);
                                // normalize to a nominal local pick location according to EIA 481
                                if (this.normalizePickLocation) {
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
                        calibratedVisionOffset = this.partLocation
                                .subtractWithRotation(calibratedPickLocation)
                                .derive(null, null, 0.0, null);
                        Logger.debug("calibrated vision offset is: " + calibratedVisionOffset
                                + ", length is: "+calibratedVisionOffset.getLinearLengthTo(Location.origin));

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
                            Logger.debug("calibrated pick location is: " + calibratedPickLocation);
                        }
                    }
                }
            }

            Result ocrStageResult = pipeline.getResult("OCR");
            if (ocrStageResult != null) {
                // when the stage is not enabled, a weird situation is created since the thrown error is about "Result.Circle, RotatedRect, KeyPoint"
                if (ocrStageResult.stage.isEnabled()) {
                      detectedOcrModel = (SimpleOcr.OcrModel) ocrStageResult.model;
                }
            }

            if (showResultMilliseconds > 0) {
                // Draw the result onto the pipeline image.
                Mat resultMat = pipeline.getWorkingImage().clone();
                drawHoles(resultMat, getHoles(), Color.green);
                drawLines(resultMat, getLines(), new Color(0, 0, 255));
                drawPartNumbers(resultMat, Color.orange);
                drawOcrText(resultMat, Color.orange);
                if (getHoles().isEmpty()) {
                    Imgproc.line(resultMat, new Point(0, 0), new Point(resultMat.cols()-1, resultMat.rows()-1),
                            FluentCv.colorToScalar(Color.red), 2, Imgproc.LINE_AA);
                    Imgproc.line(resultMat, new Point(0, resultMat.rows()-1), new Point(resultMat.cols()-1, 0),
                            FluentCv.colorToScalar(Color.red), 2, Imgproc.LINE_AA);
                }

                if (Logger.getLevel() == org.pmw.tinylog.Level.DEBUG || Logger.getLevel() == org.pmw.tinylog.Level.TRACE) {
                    File file = Configuration.get().createResourceFile(getClass(), "tape-utils", ".png");
                    Imgcodecs.imwrite(file.getAbsolutePath(), resultMat);
                }
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
}
