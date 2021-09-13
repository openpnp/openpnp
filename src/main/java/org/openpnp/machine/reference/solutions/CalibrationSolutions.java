/*
 * Copyright (C) 2021 <mark@makr.zone>
 * inspired and based on work
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

package org.openpnp.machine.reference.solutions;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.machine.reference.axis.ReferenceControllerAxis;
import org.openpnp.machine.reference.axis.ReferenceControllerAxis.BacklashCompensationMethod;
import org.openpnp.machine.reference.feeder.ReferenceTubeFeeder;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Package;
import org.openpnp.model.Part;
import org.openpnp.model.Solutions;
import org.openpnp.model.Solutions.Milestone;
import org.openpnp.model.Solutions.State;
import org.openpnp.spi.Axis.Type;
import org.openpnp.spi.Camera;
import org.openpnp.spi.CoordinateAxis;
import org.openpnp.spi.Head;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.MotionPlanner.CompletionType;
import org.openpnp.spi.Nozzle;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.SimpleGraph;
import org.openpnp.util.UiUtils;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.pipeline.CvStage.Result.Circle;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

/**
 * This helper class implements the Issues & Solutions for the Calibration Milestone. 
 */
public class CalibrationSolutions implements Solutions.Subject {

    @Attribute(required = false)
    private int backlashCalibrationPasses = 4;

    @Attribute(required = false)
    private double errorDampening = 0.9;

    @Attribute(required = false)
    private double backlashTestMoveMm = 10;

    @Attribute(required = false)
    private double backlashTestMoveLargeMm = 90;

    @Attribute(required = false)
    private double stepTestMm = 1;

    @Attribute(required = false)
    private double maxSneakUpOffsetMm = 2.5;

    @Attribute(required = false)
    private double acceptableSneakUpOffsetMm = 0.8;

    @Element(required=false)
    private double[] backlashProbingSpeeds = new double [] { 0.25, 0.33, 0.5, 0.75, 1 };

    @Attribute(required = false)
    private double backlashDistanceFactor = Math.pow(2.0, 0.5); 

    @Attribute(required = false)
    private int nozzleOffsetAngles = 6;

    @Attribute(required = false)
    private long extraVacuumDwellMs = 300;


    public CalibrationSolutions setMachine(ReferenceMachine machine) {
        this.machine = machine;
        return this;
    }

    private ReferenceMachine machine;


    @Override
    public void findIssues(Solutions solutions) {
        if (solutions.isTargeting(Milestone.Calibration)) {
            for (Head h : machine.getHeads()) {
                if (h instanceof ReferenceHead) {
                    ReferenceHead head = (ReferenceHead) h;
                    ReferenceCamera defaultCamera = null;
                    try {
                        defaultCamera = (ReferenceCamera)head.getDefaultCamera();
                    }
                    catch (Exception e) {
                        // Ignore missing camera.
                    }
                    Nozzle defaultNozzle = null;
                    try {
                        defaultNozzle = head.getDefaultNozzle();
                    }
                    catch (Exception e1) {
                    }
                    if (defaultCamera != null) {
                        for (Camera camera : head.getCameras()) {
                            if (camera instanceof ReferenceCamera) {
                                perDownLookingCameraSolutions(solutions, head, defaultCamera, defaultNozzle, (ReferenceCamera) camera);
                            }
                        }
                        for (Nozzle nozzle : head.getNozzles()) {
                            if (nozzle instanceof ReferenceNozzle) {
                                perNozzleSolutions(solutions, head, defaultCamera, defaultNozzle, (ReferenceNozzle) nozzle);
                            }
                        }
                        perHeadSolutions(solutions, head, defaultCamera);
                    }
                }
            } 

            Camera defaultCamera = null;
            Nozzle defaultNozzle = null;
            try {
                defaultCamera = VisionUtils.getBottomVisionCamera();
                Head head = machine.getDefaultHead();
                defaultNozzle = head.getDefaultNozzle();
            }
            catch (Exception e1) {
            }
            if (defaultCamera != null && defaultNozzle  != null) {
                for (Camera camera : machine.getCameras()) {
                    if (camera instanceof ReferenceCamera) {
                        perUpLookingCameraSolutions(solutions, defaultCamera, defaultNozzle, (ReferenceCamera) camera);
                    }
                }
            }
        }
    }

    private void perHeadSolutions(Solutions solutions, ReferenceHead head, ReferenceCamera defaultCamera) {
    }

    private void perDownLookingCameraSolutions(Solutions solutions, ReferenceHead head,
            ReferenceCamera defaultCamera, Nozzle defaultNozzle, ReferenceCamera camera) {

        VisionSolutions visualSolutions = machine.getVisionSolutions();
        if (visualSolutions.isSolvedPrimaryXY(head)) {
            // Calibrate backlash.
            if (camera == defaultCamera) {
                CoordinateAxis rawAxisX = HeadSolutions.getRawAxis(machine, camera.getAxisX());
                CoordinateAxis rawAxisY = HeadSolutions.getRawAxis(machine, camera.getAxisY());
                for (CoordinateAxis rawAxis : new CoordinateAxis[] {rawAxisX, rawAxisY}) {
                    if (rawAxis instanceof ReferenceControllerAxis) {
                        ReferenceControllerAxis axis = (ReferenceControllerAxis)rawAxis;
                        BacklashCompensationMethod oldMethod = axis.getBacklashCompensationMethod();
                        Length oldOffset = axis.getBacklashOffset();
                        Length oldSneakUp = axis.getSneakUpOffset();
                        double oldSpeed = axis.getBacklashSpeedFactor();
                        Length oldAcceptableTolerance = axis.getAcceptableTolerance();

                        solutions.add(new Solutions.Issue(
                                camera, 
                                "Calibrate backlash compensation for axis "+axis.getName()+".", 
                                "Automatically calibrates the backlash compensation for "+axis.getName()+" using the primary calibration fiducial.", 
                                Solutions.Severity.Fundamental,
                                "https://github.com/openpnp/openpnp/wiki/Calibration-Solutions#calibrating-backlash-compensation") {

                            {
                                tolerance = oldAcceptableTolerance;
                            }
                            
                            private Length tolerance;
                            
                            @Override 
                            public void activate() throws Exception {
                                MainFrame.get().getMachineControls().setSelectedTool(camera);
                                camera.ensureCameraVisible();
                            }

                            @Override 
                            public String getExtendedDescription() {
                                return "<html>"
                                        + "<p>Backlash compensation is used to avoid the effects of any looseness or play in the mechanical "
                                        + "linkages of machine axes. More information can be found in the Wiki (press the blue Info button below).</p><br/>"
                                        + "<p>Set the acceptable <strong>Tolerance ±</strong> as high as possible to allow for a more efficient backlash "
                                        + "compensation method, avoiding extra moves and direction changes.</p><br/>"
                                        + "<p><span color=\"red\">CAUTION 1</span>: The camera "+camera.getName()+" will move over the primary fiducial "
                                        + "and then perform a calibration motion pattern, moving the axis "+axis.getName()+" over its full soft-limit range.</p><br/>"
                                        + "<p><span color=\"red\">CAUTION 2</span>: The machine will also perform a visual homing cycle, once the new backlash "
                                        + "compensation method is established. This is done to recalibrate the coordinate system that might be affected by the "
                                        + "new method.</p><br/>"
                                        + "<p>When ready, press Accept.</p>"
                                        + (getState() == State.Solved ? 
                                                "<br/><h4>Results:</h4>"
                                                + "<table>"
                                                + "<tr><td align=\"right\">Detected Backlash:</td>"
                                                + "<td>"+axis.getBacklashOffset()+"</td></tr>"
                                                + "<tr><td align=\"right\">Selected Method:</td>"
                                                + "<td>"+axis.getBacklashCompensationMethod().toString()+"</td></tr>"
                                                + "<tr><td align=\"right\">Sneak-up Distance:</td>"
                                                + "<td>"+axis.getSneakUpOffset()+"</td></tr>"
                                                + "<tr><td align=\"right\">Speed Factor:</td>"
                                                + "<td>"+axis.getBacklashSpeedFactor()+"</td></tr>"
                                                + "<tr><td align=\"right\">Applicable Resolution:</td>"
                                                + "<td>"+String.format("%.4f", getAxisCalibrationTolerance(camera, axis, true))+" mm</td></tr>"
                                                + "</table>" 
                                                : "")
                                        + "</html>";
                            }

                            @Override
                            public Solutions.Issue.CustomProperty[] getProperties() {
                                return new Solutions.Issue.CustomProperty[] {
                                        new Solutions.Issue.LengthProperty(
                                                "Tolerance ±",
                                                "Set the targe tolerance. By granting a larger tolerance, a more efficient backlash compensation method may be eligible.") {
                                            @Override
                                            public Length get() {
                                                return tolerance;
                                            }
                                            @Override
                                            public void set(Length value) {
                                                tolerance = value;
                                            }
                                        },
                                };
                            }

                            @Override
                            public void setState(Solutions.State state) throws Exception {
                                if (state == State.Solved) {
                                    if (! visualSolutions.isSolvedPrimaryXY(head)) {
                                        throw new Exception("The head "+head.getName()+" primary fiducial location X and Y must be set first.");
                                    }

                                    final State oldState = getState();
                                    UiUtils.submitUiMachineTask(
                                            () -> {
                                                axis.setAcceptableTolerance(tolerance);
                                                calibrateAxisBacklash(head, camera, camera, axis, tolerance);
                                                return true;
                                            },
                                            (result) -> {
                                                UiUtils.messageBoxOnException(() -> super.setState(state));
                                                // Persist this solved state.
                                                solutions.setSolutionsIssueSolved(this, true);
                                            },
                                            (t) -> {
                                                UiUtils.showError(t);
                                                // restore old state
                                                UiUtils.messageBoxOnException(() -> setState(oldState));
                                            });
                                }
                                else {
                                    axis.setBacklashCompensationMethod(oldMethod);
                                    axis.setBacklashOffset(oldOffset);
                                    axis.setSneakUpOffset(oldSneakUp);
                                    axis.setBacklashSpeedFactor(oldSpeed);
                                    axis.setAcceptableTolerance(oldAcceptableTolerance);
                                    // Persist this unsolved state.
                                    solutions.setSolutionsIssueSolved(this, false);
                                    super.setState(state);
                                }
                            }
                        });
                    }
                }
            }
        }
    }

    private void perNozzleSolutions(Solutions solutions, ReferenceHead head, ReferenceCamera defaultCamera,
            Nozzle defaultNozzle, ReferenceNozzle nozzle) {
        VisionSolutions visualSolutions = machine.getVisionSolutions();
        if (visualSolutions.isSolvedPrimaryXY(head) && visualSolutions.isSolvedPrimaryZ(head) 
                && (nozzle == defaultNozzle || nozzle.getHeadOffsets().isInitialized())) {
            final Location oldNozzleOffsets = nozzle.getHeadOffsets();
            final Length oldTestObjectDiameter = head.getCalibrationTestObjectDiameter(); 
            // Get the test subject diameter.
            solutions.add(visualSolutions.new VisionFeatureIssue(
                    nozzle, 
                    defaultCamera,
                    oldTestObjectDiameter,
                    "Calibrate precise camera ↔ nozzle "+nozzle.getName()+" offsets.", 
                    "Use a test object to perform the precision camera ↔ nozzle "+nozzle.getName()+" offsets calibration.", 
                    Solutions.Severity.Fundamental,
                    "https://github.com/openpnp/openpnp/wiki/Calibration-Solutions#calibrating-precision-camera-to-nozzle-offsets") {

                @Override 
                public String getExtendedDescription() {
                    return "<html>"
                            + "<p>To calibrate precision camera ↔ nozzle offsets, we let the nozzle pick, rotate and place a small "
                            + "test object and then measure the resulting offsets using the camera.</p><br/>"
                            + "<p>Instructions about suitable test objects etc. must be obtained in the OpenPnP "
                            + "Wiki. Press the blue Info button (below) to open the Wiki.</p><br/>"
                            + "<p>Place the calibration test object onto the calibration primary fiducial.</p><br/>"
                            + "<p>Jog camera " + defaultCamera.getName()
                            + " over the test object. Target it with the cross-hairs.</p><br/>"
                            + "<p>Adjust the <strong>Feature diameter</strong> up and down and see if it is detected right in the "
                            + "camera view. A green circle and cross-hairs should appear and hug the test object contour. "
                            + "Zoom the camera using the scroll-wheel.</p><br/>"
                            + "<p><strong color=\"red\">Caution:</strong> The nozzle "+nozzle.getName()+" will move to the test object "
                            + "and perform the calibration pick & place pattern. Make sure to load the right nozzle tip and "
                            + "ready the vacuum system.</p><br/>"
                            + "<p>When ready, press Accept.</p>"
                            + (getState() == State.Solved && !nozzle.getHeadOffsets().equals(oldNozzleOffsets) ? 
                                    "<br/><h4>Results:</h4>"
                                    + "<table>"
                                    + "<tr><td align=\"right\">Detected Nozzle Head Offsets:</td>"
                                    + "<td>"+nozzle.getHeadOffsets()+"</td></tr>"
                                    + "<tr><td align=\"right\">Previous Nozzle Head Offsets:</td>"
                                    + "<td>"+oldNozzleOffsets+"</td></tr>"
                                    + "<tr><td align=\"right\">Difference:</td>"
                                    + "<td>"+nozzle.getHeadOffsets().subtract(oldNozzleOffsets)+"</td></tr>"
                                    + "</table>" 
                                    : "")
                            + "</html>";
                }

                @Override
                public void setState(Solutions.State state) throws Exception {
                    if (state == State.Solved) {
                        if (! visualSolutions.isSolvedPrimaryXY(head)) {
                            throw new Exception("The head "+head.getName()+" primary fiducial location X and Y must be set first.");
                        }
                        if (! visualSolutions.isSolvedPrimaryZ(head)) {
                            throw new Exception("The head "+head.getName()+" primary fiducial location Z must be set first.");
                        }
                        if (! (nozzle == defaultNozzle || nozzle.getHeadOffsets().isInitialized())) {
                            throw new Exception("The nozzle "+nozzle.getName()+" head offsets must be roughly set first. "
                                    + "Use the \"Noozle "+nozzle.getName()+" offset for the primary fiducial\" calibration.");
                        }
                        final State oldState = getState();
                        UiUtils.submitUiMachineTask(
                                () -> {
                                    Circle testObject = visualSolutions
                                            .getSubjectPixelLocation(defaultCamera, null, new Circle(0, 0, featureDiameter), 0, null, null);
                                    head.setCalibrationTestObjectDiameter(
                                            defaultCamera.getUnitsPerPixelPrimary().getLengthX().multiply(testObject.getDiameter()));
                                    calibrateNozzleOffsets(head, defaultCamera, nozzle);
                                    return true;
                                },
                                (result) -> {
                                    UiUtils.messageBoxOnException(() -> super.setState(state));
                                    // Persist this solved state.
                                    solutions.setSolutionsIssueSolved(this, true);
                                },
                                (t) -> {
                                    UiUtils.showError(t);
                                    // restore old state
                                    UiUtils.messageBoxOnException(() -> setState(oldState));
                                });
                    }
                    else {
                        // Restore the head offset
                        nozzle.setHeadOffsets(oldNozzleOffsets);
                        head.setCalibrationTestObjectDiameter(oldTestObjectDiameter);
                        // Persist this unsolved state.
                        solutions.setSolutionsIssueSolved(this, false);
                        super.setState(state);
                    }
                }
            });
        }
    }

    private void perUpLookingCameraSolutions(Solutions solutions, Camera defaultCamera,
            Nozzle defaultNozzle, ReferenceCamera camera) {

    }

    public void calibrateAxisBacklash(ReferenceHead head, ReferenceCamera camera,
            HeadMountable movable, ReferenceControllerAxis axis, Length acceptableTolerance) throws Exception {
        // Check pre-conditions (this method can be called from outside Issues & Solutions).
        if (!(axis.isSoftLimitLowEnabled() && axis.isSoftLimitHighEnabled())) {
            throw new Exception("Axis "+axis.getName()+" must have soft limits enabled for backlash calibration.");
        }
        if (! head.getCalibrationPrimaryFiducialLocation().isInitialized()) {
            throw new Exception("Head "+head.getName()+" primary fiducial location must be set for backlash calibration.");
        }
        if (! head.getCalibrationPrimaryFiducialDiameter().isInitialized()) {
            throw new Exception("Head "+head.getName()+" primary fiducial diameter must be set for backlash calibration.");
        }
        // Make sure to disable any backlash compensation.
        axis.setBacklashCompensationMethod(BacklashCompensationMethod.None);

        // Use the primary calibration fiducial for calibration.
        Location location = head.getCalibrationPrimaryFiducialLocation();
        Length fiducialDiameter = head.getCalibrationPrimaryFiducialDiameter();
        MovableUtils.moveToLocationAtSafeZ(movable, location);
        location = machine.getVisionSolutions()
                .centerInOnSubjectLocation(camera, movable,
                        fiducialDiameter, "Backlash Calibration Start Location", false);

        // General note: We always use mm.
        // Calculate the unit vector for the axis in both logical and axis coordinates. 
        Location unit = new Location(LengthUnit.Millimeters, 
                (axis.getType() == Type.X ? 1 : 0), 
                (axis.getType() == Type.Y ? 1 : 0),
                0, 0);
        AxesLocation axesLocation0 = movable.toRaw(location);
        AxesLocation axesLocation1 = movable.toRaw(location.add(unit));
        double mmAxis = axesLocation1.getCoordinate(axis) - axesLocation0.getCoordinate(axis); 

        double stepMm = getAxisCalibrationTolerance(camera, axis, false);
        Length stepLength = new Length(stepMm, LengthUnit.Millimeters);
        double toleranceMm = getAxisCalibrationTolerance(camera, axis, false);
        if (acceptableTolerance != null) {
            toleranceMm = Math.ceil(acceptableTolerance.convertToUnits(LengthUnit.Millimeters).getValue()/toleranceMm)*toleranceMm;
        }
        double toleranceUnits = new Length(toleranceMm, LengthUnit.Millimeters).convertToUnits(axis.getUnits()).getValue();

        // Diagnostics graph.
        final String ERROR = "E";
        final String ABSOLUTE = "A";
        final String ABSOLUTE_RANDOM = "AR";
        final String RELATIVE = "R";
        final String SCALE = "S";
        final String BACKLASH = "B";
        final String OVERSHOOT = "O";
        final String LIMIT0 = "L0";
        final String LIMIT1 = "L1";

        SimpleGraph stepTestGraph = new SimpleGraph();
        stepTestGraph.setRelativePaddingLeft(0.05);
        SimpleGraph.DataScale errorScale =  stepTestGraph.getScale(ERROR);
        errorScale.setRelativePaddingBottom(0.2);
        errorScale.setSymmetricIfSigned(true);
        errorScale.setColor(SimpleGraph.getDefaultGridColor());
        stepTestGraph.getRow(ERROR, ABSOLUTE)
        .setColor(new Color(0xFF, 0, 0));
        stepTestGraph.getRow(ERROR, RELATIVE)
        .setColor(new Color(0, 0x5B, 0xD9)); // the OpenPNP blue
        stepTestGraph.getRow(ERROR, RELATIVE)
        .setMarkerShown(true);
        stepTestGraph.getRow(ERROR, RELATIVE)
        .setLineShown(false);
        stepTestGraph.getRow(ERROR, ABSOLUTE_RANDOM)
        .setColor(new Color(0xBB, 0x77, 0));
        stepTestGraph.getRow(ERROR, ABSOLUTE_RANDOM)
        .setMarkerShown(true);
        stepTestGraph.getRow(ERROR, ABSOLUTE_RANDOM)
        .setLineShown(false);
        stepTestGraph.getRow(ERROR, LIMIT0)
        .setColor(new Color(0, 0x80, 0)); 
        stepTestGraph.getRow(ERROR, LIMIT1)
        .setColor(new Color(0, 0x80, 0));

        SimpleGraph distanceGraph = new SimpleGraph();
        distanceGraph.setRelativePaddingLeft(0.05);
        SimpleGraph.DataScale stepScale =  distanceGraph.getScale(SCALE);
        stepScale.setRelativePaddingBottom(0.2);
        stepScale.setColor(SimpleGraph.getDefaultGridColor());
        distanceGraph.getRow(SCALE, BACKLASH+0)
        .setColor(new Color(00, 0x5B, 0xD9)); // the OpenPNP blue
        distanceGraph.getRow(SCALE, OVERSHOOT+0)
        .setColor(new Color(0xFF, 0, 0)); 
        distanceGraph.getRow(SCALE, BACKLASH+1)
        .setColor(new Color(00, 0x5B, 0xD9, 128)); // the OpenPNP blue
        distanceGraph.getRow(SCALE, OVERSHOOT+1)
        .setColor(new Color(0xFF, 0, 0, 128)); 

        SimpleGraph speedGraph = new SimpleGraph();
        speedGraph.setRelativePaddingLeft(0.05);
        SimpleGraph.DataScale speedScale =  speedGraph.getScale(SCALE);
        speedScale.setRelativePaddingBottom(0.2);
        speedScale.setColor(SimpleGraph.getDefaultGridColor());
        speedGraph.getRow(SCALE, BACKLASH)
        .setColor(new Color(0xFF, 0, 0));

        // Perform a step test over a small distance.
        MovableUtils.moveToLocationAtSafeZ(movable, displacedAxisLocation(movable, axis, location, -backlashTestMoveLargeMm*mmAxis, false));
        double minimumSpeed = backlashProbingSpeeds[0];
        int step = 0;
        Location referenceLocation = location;
        Location stepLocation0 = null;
        for (double stepPos = -stepTestMm/2; stepPos < stepTestMm/2; stepPos += stepMm) {
            step++;
            Location startMoveLocation = displacedAxisLocation(movable, axis, location, (stepPos - stepTestMm)*mmAxis, false);
            movable.moveTo(startMoveLocation);
            Location nominalStepLocation = displacedAxisLocation(movable, axis, location, stepPos*mmAxis, false);
            movable.moveTo(nominalStepLocation, minimumSpeed);
            Location stepLocation1 = machine.getVisionSolutions().getDetectedLocation(camera, movable, 
                    location, fiducialDiameter, "Accuracy Test Step "+step, false);
            if (stepLocation0 != null) {
                Length absoluteErr = stepLocation1.subtract(referenceLocation).dotProduct(unit);
                double absoluteErrUnits = absoluteErr.convertToUnits(axis.getUnits()).getValue();
                stepTestGraph.getRow(ERROR, ABSOLUTE)
                        .recordDataPoint(step, absoluteErrUnits);
                Length relativeErr = stepLocation1.subtract(stepLocation0).dotProduct(unit);
                double relativeErrorUnits = relativeErr.convertToUnits(axis.getUnits()).getValue();
                stepTestGraph.getRow(ERROR, RELATIVE)
                    .recordDataPoint(step, relativeErrorUnits);
                stepTestGraph.getRow(ERROR, LIMIT0)
                    .recordDataPoint(step, -toleranceUnits);
                stepTestGraph.getRow(ERROR, LIMIT1)
                    .recordDataPoint(step, toleranceUnits);
            }
            else {
                referenceLocation = location
                        .add(stepLocation1.subtract(referenceLocation));
            }
            stepLocation0 = stepLocation1;
        }

        // Perform a backlash test over distances. The distances are a geometric series.   
        MovableUtils.moveToLocationAtSafeZ(movable, location, minimumSpeed);
        axis.setBacklashCompensationMethod(BacklashCompensationMethod.None);
        ArrayList<Double> backlashProbingDistances = new ArrayList<>();
        double distance0 = Double.NEGATIVE_INFINITY;
        for (double distanceMm = stepMm*2; distanceMm <= backlashTestMoveMm; distanceMm *= backlashDistanceFactor) {
            if (distanceMm - distance0 >= stepMm) {
                backlashProbingDistances.add(distanceMm);
                distance0 = distanceMm;
            }
        }
        // Pseudo-entry for full range.
        backlashProbingDistances.add(backlashTestMoveMm*2);
        Collections.reverse(backlashProbingDistances);
        double[] backlashOffsetByDistance = new double [backlashProbingDistances.size()];
        int iDistance = 0;
        double maxBacklash = Double.NEGATIVE_INFINITY;
        double maxBacklashDistance = 0;
        boolean maxBacklashOpen = true;
        double minBacklash = Double.POSITIVE_INFINITY;
        double minBacklashDistance = 0;
        LengthConverter lengthConverter = new LengthConverter();
        for (int pass = 0; pass < 2; pass++) {
            for (double distance : backlashProbingDistances) {
                // measure the backlash offset over distance.
                for (int reverse = 0; reverse <= 1; reverse++) {
                    if (reverse == 1 && distance > backlashTestMoveMm) {
                        continue;
                    }
                    // Approach from minus.
                    if (pass == 0) {
                        // Sneak-up
                        if (reverse == 0) {
                            MovableUtils.moveToLocationAtSafeZ(movable, displacedAxisLocation(movable, axis, location, -backlashTestMoveLargeMm*mmAxis, distance > backlashTestMoveMm));
                        }
                        MovableUtils.moveToLocationAtSafeZ(movable, displacedAxisLocation(movable, axis, location, -distance*mmAxis, distance > backlashTestMoveMm));
                        movable.moveTo(location, minimumSpeed);
                    }
                    else {
                        // Overshoot
                        if (reverse == 0) {
                            MovableUtils.moveToLocationAtSafeZ(movable, displacedAxisLocation(movable, axis, location, -backlashTestMoveMm*mmAxis, distance > backlashTestMoveMm), minimumSpeed);
                        }
                        MovableUtils.moveToLocationAtSafeZ(movable, displacedAxisLocation(movable, axis, location, -distance*mmAxis, distance > backlashTestMoveMm), minimumSpeed);
                        movable.waitForCompletion(CompletionType.WaitForStillstand);
                        Thread.sleep(500);
                        movable.moveTo(location);
                    }
                    String passTitle = pass == 0 ? "Backlash at Sneak-up Distance " : "Overshoot at Distance ";
                    String distanceOutput = distance > backlashTestMoveMm ? "∞" : lengthConverter.convertForward(new Length(distance, LengthUnit.Millimeters));
                    Location effective0 = machine.getVisionSolutions().getDetectedLocation(camera, movable, 
                            location, fiducialDiameter, passTitle+distanceOutput+" ►", false);

                    // Approach from plus.
                    if (pass == 0) {
                        // Sneak-up
                        if (reverse == 0) {
                            MovableUtils.moveToLocationAtSafeZ(movable, displacedAxisLocation(movable, axis, location, backlashTestMoveLargeMm*mmAxis, distance > backlashTestMoveMm));
                        }
                        MovableUtils.moveToLocationAtSafeZ(movable, displacedAxisLocation(movable, axis, location, distance*mmAxis, distance > backlashTestMoveMm));
                        movable.moveTo(location, minimumSpeed);
                    }
                    else {
                        // Overshoot
                        if (reverse == 0) {
                            MovableUtils.moveToLocationAtSafeZ(movable, displacedAxisLocation(movable, axis, location, backlashTestMoveMm*mmAxis, distance > backlashTestMoveMm), minimumSpeed);
                        }
                        MovableUtils.moveToLocationAtSafeZ(movable, displacedAxisLocation(movable, axis, location, distance*mmAxis, distance > backlashTestMoveMm), minimumSpeed);
                        movable.waitForCompletion(CompletionType.WaitForStillstand);
                        Thread.sleep(500);
                        movable.moveTo(location);
                    }
                    Location effective1 = machine.getVisionSolutions().getDetectedLocation(camera, movable, 
                            location, fiducialDiameter, passTitle+distanceOutput+" ◄", false);

                    double mmError = effective1.subtract(effective0).dotProduct(unit).getValue();
                    if (movable == camera) {
                        // If the camera moves (and not the subject) then the subject's 
                        // displacement is negative.
                        mmError = -mmError;
                    }

                    // Record this for the distance. 
                    if (pass == 0 && reverse == 0) {
                        backlashOffsetByDistance[iDistance++] = mmError;
                    }
                    double errorUnits = new Length(mmError, LengthUnit.Millimeters).convertToUnits(axis.getUnits()).getValue();
                    if (pass == 0) {
                        distanceGraph.getRow(SCALE, BACKLASH+reverse).recordDataPoint(distance, 
                                errorUnits);
                    }
                    else {
                        distanceGraph.getRow(SCALE, OVERSHOOT+reverse).recordDataPoint(distance, 
                                (maxBacklash-errorUnits)/2);
                    }

                    if (pass == 0 && distance > maxBacklash && distance <= maxSneakUpOffsetMm) {
                        if (maxBacklashOpen && mmError >= maxBacklash - 2*toleranceMm) {
                            if (mmError > maxBacklash) {
                                maxBacklash = mmError;
                            }
                            // Take the last distance i.e the smallest (this is a descending loop) that has an 
                            // error within 2 * tolerance of the maxBacklash (we're assuming a two-sided ± tolerance).
                            maxBacklashDistance = distance;
                            // Remember the minimum acceptable backlash.
                            if (mmError < minBacklash) {
                                minBacklash = mmError;
                                minBacklashDistance = distance;
                            }
                        }
                        else {
                            // Once it dips under the maximum minus tolerance, its not eligible.
                            maxBacklashOpen = false;
                        }
                    }
                }
            }
        }
        double sneakUpOffset = Math.max(maxBacklash, maxBacklashDistance);
        double[] backlashOffsetBySpeed = new double [backlashProbingSpeeds.length];
        int iSpeed = 0;
        for (double speed : backlashProbingSpeeds) {
            // Reset the config.
            axis.setBacklashCompensationMethod(speed < 1.0 ? BacklashCompensationMethod.DirectionalSneakUp : BacklashCompensationMethod.DirectionalCompensation);
            axis.setBacklashOffset(new Length(0, LengthUnit.Millimeters));
            axis.setSneakUpOffset(new Length(speed < 1.0 ? sneakUpOffset : 0, LengthUnit.Millimeters));
            axis.setBacklashSpeedFactor(speed);
            // Find the right backlash offset.
            double offsetMm = 0;
            for (int pass = 0; pass < backlashCalibrationPasses; pass++) {
                // Approach from minus.
                MovableUtils.moveToLocationAtSafeZ(movable, displacedAxisLocation(movable, axis, location, -backlashTestMoveLargeMm*mmAxis, false));
                movable.moveTo(location);
                Location effective0 = machine.getVisionSolutions().getDetectedLocation(camera, movable, 
                        location, fiducialDiameter, "Backlash at Speed "+speed+"× ►", false);

                // Approach from plus.
                MovableUtils.moveToLocationAtSafeZ(movable, displacedAxisLocation(movable, axis, location, backlashTestMoveLargeMm*mmAxis, false));
                movable.moveTo(location);
                Location effective1 = machine.getVisionSolutions().getDetectedLocation(camera, movable, 
                        location, fiducialDiameter, "Backlash at Speed "+speed+"× ◄", false);

                double mmError = effective1.subtract(effective0).dotProduct(unit).getValue();
                if (movable == camera) {
                    // If the camera moves (and not the subject) then the subject's 
                    // displacement is negative.
                    mmError = -mmError;
                }
                offsetMm += mmError*mmAxis*errorDampening;
                if (pass == 0 && mmError <= -toleranceMm) {
                    // Overshoot - cannot compensate.
                    break;
                }
                axis.setBacklashOffset(new Length(offsetMm, LengthUnit.Millimeters)
                        .convertToUnits(axis.getDriver().getUnits()));
                if (Math.abs(mmError) < toleranceMm) {
                    break;
                }
            }
            // Record this for the speed. 
            backlashOffsetBySpeed[iSpeed++] = offsetMm;
            double offsetUnits = new Length(offsetMm, LengthUnit.Millimeters).convertToUnits(axis.getUnits()).getValue();
            speedGraph.getRow(SCALE, BACKLASH).recordDataPoint(speed, 
                    offsetUnits);
        }

        // Determine consistency over speed.
        int consistent = 0;
        double offsetMmSum = 0;
        for (double offsetMm : backlashOffsetBySpeed) {
            if (offsetMm <= -toleranceMm
                    || Math.abs(offsetMm - backlashOffsetBySpeed[0]) > toleranceMm) {
                // inconsistent
                break;
            }
            offsetMmSum += offsetMm;
            consistent++;
        }
        double offsetMmAvg = offsetMmSum/consistent;
        double offsetMmMax = 0;
        iSpeed = 0;
        for (double offsetMm : backlashOffsetBySpeed) {
            offsetMmMax = Math.max(offsetMmMax, Math.abs(offsetMm));
            Logger.debug("Axis "+axis.getName()+" backlash offsets at speed factor "+backlashProbingSpeeds[iSpeed++]+" is "+offsetMm);
        }
        Logger.debug("Axis "+axis.getName()+" backlash offsets analysis, consistent: "+consistent+", avg offset: "+offsetMmAvg+", max offset: "+offsetMmMax);
        // Set the backlash method according to consistency.
        if (sneakUpOffset > acceptableSneakUpOffsetMm) {
            // No acceptable sneak-up offset possible. Go for one-sided.
            axis.setBacklashCompensationMethod(BacklashCompensationMethod.OneSidedPositioning);
            axis.setBacklashOffset(new Length(maxBacklash, LengthUnit.Millimeters));
            axis.setSneakUpOffset(new Length(0, LengthUnit.Millimeters));
            axis.setBacklashSpeedFactor(backlashProbingSpeeds[0]);
            Logger.debug("Axis "+axis.getName()+" backlash offsets analysis, sneakUpOffset: "+sneakUpOffset+" unacceptable (> "+acceptableSneakUpOffsetMm+")");
        }
        else if (offsetMmAvg < sneakUpOffset - toleranceMm) {
            // Even at lowest speed, we got a sneak-up distance larger than the backlash.
            axis.setBacklashCompensationMethod(BacklashCompensationMethod.DirectionalSneakUp);
            axis.setBacklashOffset(new Length((minBacklash + maxBacklash)/2, LengthUnit.Millimeters));
            axis.setSneakUpOffset(new Length(sneakUpOffset, LengthUnit.Millimeters));
            axis.setBacklashSpeedFactor(backlashProbingSpeeds[0]);
        }
        else if (consistent == backlashProbingSpeeds.length) {
            // We got consistent backlash over all the speeds and distances.
            if (offsetMmAvg < toleranceMm) {
                // Smaller than resolution, no need for compensation.
                axis.setBacklashCompensationMethod(BacklashCompensationMethod.None);
            }
            else {
                // Consistent over speed, can be compensated by directional method. 
                axis.setBacklashCompensationMethod(BacklashCompensationMethod.DirectionalCompensation);
            }
            axis.setBacklashOffset(new Length(offsetMmAvg, LengthUnit.Millimeters));
            axis.setSneakUpOffset(new Length(0, LengthUnit.Millimeters));
            axis.setBacklashSpeedFactor(backlashProbingSpeeds[consistent-1]);
        }
        else if (consistent > 0) {
            // Not consistent over speed.
            axis.setBacklashCompensationMethod(BacklashCompensationMethod.DirectionalSneakUp);
            axis.setBacklashOffset(new Length(offsetMmAvg, LengthUnit.Millimeters));
            axis.setSneakUpOffset(new Length(sneakUpOffset, LengthUnit.Millimeters));
            axis.setBacklashSpeedFactor(backlashProbingSpeeds[consistent-1]);
        }
        else {
            throw new Exception("Axis "+axis.getName()+" seems to overshoot, even at the lowest speed factor. "
                    + "Make sure OpenPnP has effective acceleration/jerk control. "
                    + "Automatic compensation not possible.");
        }
        // Because this change may affect the coordinate system, perform a (visual) homing cycle.
        head.visualHome(machine, true);
        // Go back to the fiducial.
        MovableUtils.moveToLocationAtSafeZ(movable, location);
        // Test the new settings with random moves.
        referenceLocation = machine.getVisionSolutions()
                .centerInOnSubjectLocation(camera, movable,
                        fiducialDiameter, "Backlash Compensation Test Location", false);
        final int fraction = 2;
        final double minLog = Math.log(stepMm);
        final double maxLog = Math.log(backlashTestMoveLargeMm);
        final double rangeLog = maxLog - minLog; 
        step = 0;
        for (double stepPos = -stepTestMm/2; stepPos < stepTestMm/2; stepPos += stepMm*fraction) {
            step++;
            Location startMoveLocation = displacedAxisLocation(movable, axis, location, 
                    Math.signum(Math.random()-0.5)*Math.exp(Math.random()*rangeLog + minLog)*mmAxis, false);
            movable.moveTo(startMoveLocation);
            Location nominalStepLocation = displacedAxisLocation(movable, axis, location, stepPos*mmAxis, false);
            movable.moveTo(nominalStepLocation);
            Location stepLocation1 = machine.getVisionSolutions().getDetectedLocation(camera, movable, 
                    location, fiducialDiameter, "Random Move Accuracy Test Step "+step, false);
            Length absoluteErr = stepLocation1.subtract(referenceLocation).dotProduct(unit);
            double absoluteErrUnits = absoluteErr.convertToUnits(axis.getUnits()).getValue();
            stepTestGraph.getRow(ERROR, ABSOLUTE_RANDOM)
            .recordDataPoint(2+(step-1)*fraction, absoluteErrUnits);
        }
        // Publish the graphs.
        axis.setStepTestGraph(stepTestGraph);
        axis.setBacklashSpeedTestGraph(speedGraph);
        axis.setBacklashDistanceTestGraph(distanceGraph);
    }

    private double getAxisCalibrationTolerance(ReferenceCamera camera,
            ReferenceControllerAxis axis, boolean tolerance) {
        // Get the axis resolution (it might still be at the default 0.0001).
        Length resolution = new Length(axis.getResolution(), axis.getDriver().getUnits());
        // Take a multiple of the native resolution that is >= 0.01mm
        Length finestResolution = new Length(0.01, LengthUnit.Millimeters);
        resolution = resolution.multiply(Math.ceil(finestResolution.divide(resolution)));
        // Get the sub-pixel resolution of the detection capability. 
        Length subPixelUnit = (axis.getType() == Type.X  
                ? camera.getUnitsPerPixelPrimary().getLengthX() 
                        : camera.getUnitsPerPixelPrimary().getLengthY())
                .multiply(1.0/machine.getVisionSolutions().getSuperSampling());
        if (tolerance) {
            // Round up to the next full sub-pixel (Note, this also covers the case where the axis resolution is not yet set).
            resolution = subPixelUnit.multiply(Math.ceil(resolution.divide(subPixelUnit)))
                    .convertToUnits(LengthUnit.Millimeters);
            // Add 1% for safe double comparison.
            resolution = resolution.multiply(1.01);
        }
        else {
            if (resolution.compareTo(subPixelUnit) < 0) {
                // Axis resolution probably not set, or ridiculously fine. Use sub-pixels.
                resolution = subPixelUnit;
            }
        }
        return resolution.getValue();
    }

    private Location displacedAxisLocation(HeadMountable movable, ReferenceControllerAxis axis,
            Location location, double displacement, boolean fullRange) throws Exception {
        location = movable.toHeadLocation(location);
        AxesLocation axesLocation = movable.toRaw(location);
        if (fullRange) {
            axesLocation = axesLocation.put(displacement < 0 ? 
                    new AxesLocation(axis, axis.getSoftLimitLow()) : 
                        new AxesLocation(axis, axis.getSoftLimitHigh()));
        }
        else {
            axesLocation = axesLocation.add(new AxesLocation(axis, displacement));
        }
        Location newLocation = movable.toTransformed(axesLocation);
        newLocation = movable.toHeadMountableLocation(newLocation);
        return newLocation;
    }

    private void calibrateNozzleOffsets(ReferenceHead head, ReferenceCamera defaultCamera, ReferenceNozzle nozzle)
            throws Exception {
        try {
            // Create a pseudo part, package and feeder to enable pick and place.
            Part testPart = new Part("TEST-OBJECT");
            testPart.setHeight(new Length(0.01, LengthUnit.Millimeters));
            Package packag = new Package("TEST-OBJECT-PACKAGE");
            testPart.setPackage(packag);
            ReferenceTubeFeeder feeder = new ReferenceTubeFeeder();
            feeder.setPart(testPart);
            // Get the initial precise test object location. It must lay on the primary fiducial. 
            MovableUtils.moveToLocationAtSafeZ(defaultCamera, head.getCalibrationPrimaryFiducialLocation());
            Location location = machine.getVisionSolutions()
                    .centerInOnSubjectLocation(defaultCamera, defaultCamera,
                            head.getCalibrationTestObjectDiameter(), "Nozzle Offset Calibration", false);
            // We accumulate all the detected differences and only calculate the centroid in the end. 
            int accumulated = 0;
            Location offsetsDiff = new Location(LengthUnit.Millimeters);
            double da = 360.0 / nozzleOffsetAngles;
            for (double angle = -180 + da / 2; angle < 180; angle += da) {
                // Subtract from accumulation.
                offsetsDiff = offsetsDiff.subtract(location);
                // Replace Z.
                location = location.derive(head.getCalibrationPrimaryFiducialLocation(), false,
                        false, true, false);
                // Pick the test object at the location.
                feeder.setLocation(location.derive(null, null, null, angle));
                nozzle.moveToPickLocation(feeder);
                nozzle.pick(testPart);
                // Extra wait time.
                Thread.sleep(extraVacuumDwellMs);
                // Place the part 180° rotated. This way we will detect the true nozzle rotation axis, which is 
                // the true nozzle location, namely in the center of the two detected locations. Note that run-out 
                // is cancelled out too, so run-out compensation is no prerequisite. 
                Location placementLocation = location.derive(null, null, null, angle + 180.0);
                nozzle.moveToPlacementLocation(placementLocation, testPart);
                nozzle.place();
                // Extra wait time.
                Thread.sleep(extraVacuumDwellMs);
                // Look where it is now.
                MovableUtils.moveToLocationAtSafeZ(defaultCamera, location);
                Location newlocation = machine.getVisionSolutions()
                        .centerInOnSubjectLocation(defaultCamera, defaultCamera,
                                head.getCalibrationTestObjectDiameter(), "Nozzle Offset Calibration "+angle+"°", false);
                // Add to accumulation.
                offsetsDiff = offsetsDiff.add(newlocation);
                accumulated += 2;
                Logger.debug("Nozzle "+nozzle.getName()+" has placed at offsets "
                        +newlocation.subtract(location)+ " at angle "+angle);
                // Next
                location = newlocation;
            }
            // Compute the average of the accumulated offsets differences. Take only X, Y.
            offsetsDiff = offsetsDiff.multiply(1.0 / accumulated)
                    .multiply(1, 1, 0, 0);
            Location headOffsets = nozzle.getHeadOffsets()
                    .add(offsetsDiff);
            Logger.info("Set nozzle " + nozzle.getName() + " head offsets to " + headOffsets
                    + " (previously " + nozzle.getHeadOffsets() + ")");
            nozzle.setHeadOffsets(headOffsets);
        }
        finally {
            if (nozzle.getPart() != null) {
                nozzle.place();
            }
            // Move nozzle to safe Z and with zero rotation to avoid any confusion as to the calibrated offsets.
            MovableUtils.moveToLocationAtSafeZ(nozzle, nozzle.getLocation()
                    .deriveLengths(null, null, nozzle.getSafeZ(), 0.0));
        }
    }
}
