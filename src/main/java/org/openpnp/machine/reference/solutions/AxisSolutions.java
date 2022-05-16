/*
 * Copyright (C) 2022 <mark@makr.zone>
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.openpnp.machine.reference.axis.ReferenceControllerAxis;
import org.openpnp.machine.reference.driver.GcodeDriver;
import org.openpnp.machine.reference.vision.ReferenceBottomVision;
import org.openpnp.machine.reference.vision.ReferenceBottomVision.PreRotateUsage;
import org.openpnp.model.AbstractVisionSettings;
import org.openpnp.model.BottomVisionSettings;
import org.openpnp.model.Configuration;
import org.openpnp.model.Solutions;
import org.openpnp.model.Solutions.Milestone;
import org.openpnp.model.Solutions.Severity;
import org.openpnp.model.Solutions.State;
import org.openpnp.model.Solutions.Subject;
import org.openpnp.spi.Axis;
import org.openpnp.spi.Axis.Type;
import org.openpnp.spi.Driver.MotionControlType;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.Nozzle.RotationMode;
import org.openpnp.spi.PartAlignment;
import org.openpnp.spi.base.AbstractControllerAxis;
import org.openpnp.spi.base.AbstractNozzle;

/**
 * This helper class implements the Issues & Solutions for the ReferenceHead. 
 * The idea is not to pollute the head implementation itself.
 *
 */
public class AxisSolutions implements Solutions.Subject {
    public static final String[] VALID_AXIS_LETTERS = new String[] { "X", "Y", "Z", "U", "V", "W", "A", "B", "C", "D", "E" };

    private final ReferenceControllerAxis axis;
    private Machine machine;

    public AxisSolutions(ReferenceControllerAxis axis) {
        this.axis = axis;
        this.machine = Configuration.get().getMachine();
    }

    @Override
    public void findIssues(Solutions solutions) {

        if (solutions.isTargeting(Milestone.Basics)) {
            if (axis.getDriver() == null) {
                solutions.add(new Solutions.PlainIssue(
                        axis, 
                        "Axis is not assigned to a driver.", 
                        "Assign a driver.", 
                        Severity.Fundamental,
                        "https://github.com/openpnp/openpnp/wiki/Machine-Axes#controller-settings"));

            }
            if (axis.getLetter().isEmpty()) {
                solutions.add(new AxisLetterIssue(
                        axis, 
                        "Axis letter is missing. Assign the letter to continue.", 
                        "Please assign the correct controller axis letter. Choose from the list or enter a custom letter "
                                + "(some contoller may support an extended range of letters).", 
                                Severity.Fundamental,
                        "https://github.com/openpnp/openpnp/wiki/Machine-Axes#controller-settings"));
            }
            else if (axis.getLetter().equals("E")) {
                if (axis.getDriver() != null && !axis.getDriver().isSupportingPreMove()) {
                    solutions.add(new AxisLetterIssue(
                            axis, 
                            "Avoid axis letter E, if possible. Use proper rotation axes instead.", 
                            "Check if your controller supports proper axes A B C (etc.) instead of E.", 
                            Severity.Warning,
                            "https://github.com/openpnp/openpnp/wiki/Advanced-Motion-Control#migration-from-a-previous-version"));
                }
            }
            else if (!getValidAxisLetters().contains(axis.getLetter())) {
                solutions.add(new AxisLetterIssue(
                        axis, 
                        "Axis letter "+axis.getLetter()+" is not a controller/G-code standard letter, one of "+String.join(" ", getValidAxisLetters())+".", 
                        "Please assign the correct controller axis letter (some contoller may support an extended range of letters, "
                                + "in which case you can dismiss this warning).", 
                                Severity.Warning,
                        "https://github.com/openpnp/openpnp/wiki/Machine-Axes#controller-settings"));
            }
            if (axis.getDriver() != null && !axis.getLetter().isEmpty() 
                    && !(axis.getLetter().equals("E") && axis.getDriver().isSupportingPreMove())) {
                ArrayList<String> duplicates = new ArrayList<>();
                for (Axis otherAxis : machine.getAxes()) {
                    if (otherAxis instanceof AbstractControllerAxis) {
                        AbstractControllerAxis otherControllerAxis = (AbstractControllerAxis) otherAxis;
                        if (otherControllerAxis.getDriver() == axis.getDriver()
                                && otherControllerAxis.getLetter().equals(axis.getLetter())) {
                            duplicates.add(otherControllerAxis.getName());
                        }
                    }
                }
                if (duplicates.size() > 1) {
                    solutions.add(new AxisLetterIssue(
                            axis, 
                            "Duplicate axis letter "+axis.getLetter()+" on axes "+String.join(", ", duplicates)+".", 
                            "Assign the unique axis letter where wrong, press Accept, then press Find Issues & Solutions again to clear the correct one.", 
                            Severity.Error,
                            "https://github.com/openpnp/openpnp/wiki/Machine-Axes#controller-settings"));
                }
            }
        }
        if (solutions.isTargeting(Milestone.Kinematics)) {
            if (axis.getMotionLimit(1) > 0 
                && Math.abs(axis.getMotionLimit(1)*2 - axis.getMotionLimit(2)) < 0.1) {
                // HACK: migration sets the acceleration to twice the feed-rate, that's our "signal" that the user has not yet
                // tuned them.
                solutions.add(new Solutions.PlainIssue(
                        axis, 
                        "Feed-rate, acceleration, jerk etc. can now be set individually per axis.", 
                        "Go to Machine Setup / Axes / "+axis.getClass().getSimpleName()+" "+axis.getName()+" and tune "
                                + "Feed Rate, Acceleration for best performance.", 
                        Severity.Suggestion,
                        "https://github.com/openpnp/openpnp/wiki/Machine-Axes#kinematic-settings--rate-limits"));
            }
            if (axis.getDriver() != null) {
                MotionControlType motionControlType = axis.getDriver().getMotionControlType();
                if (motionControlType.isControllingFeedRate()
                        && axis.getMotionLimit(1) <= 0) {
                    solutions.add(new Solutions.PlainIssue(
                            axis, 
                            "For motion control type "+motionControlType+" a feed-rate must be set on axis "+axis.getName()+".", 
                            "Go to Machine Setup / Axes / "+axis.getClass().getSimpleName()+" "+axis.getName()+" and set the Feed Rate.", 
                            Severity.Error,
                            "https://github.com/openpnp/openpnp/wiki/Machine-Axes#kinematic-settings--rate-limits"));
                }
                if (motionControlType.isControllingAcceleration()
                        && axis.getMotionLimit(2) <= 0) {
                    solutions.add(new Solutions.PlainIssue(
                            axis, 
                            "For motion control type "+motionControlType+" an acceleration limit must be set on axis "+axis.getName()+".", 
                            "Go to Machine Setup / Axes / "+axis.getClass().getSimpleName()+" "+axis.getName()+" and set the Acceleration.", 
                            Severity.Error,
                            "https://github.com/openpnp/openpnp/wiki/Machine-Axes#kinematic-settings--rate-limits"));
                }
                if (motionControlType.isControllingJerk()
                        && axis.getMotionLimit(3) <= 0) {
                    solutions.add(new Solutions.PlainIssue(
                            axis, 
                            "For motion control type "+motionControlType+" a jerk limit must be set on axis "+axis.getName()+".", 
                            "Go to Machine Setup / Axes / "+axis.getClass().getSimpleName()+" "+axis.getName()+" and set the Jerk.", 
                            Severity.Error,
                            "https://github.com/openpnp/openpnp/wiki/Machine-Axes#kinematic-settings--rate-limits"));
                }
            }
            if (axis.getType() == Type.Rotation) {
                boolean isUnlimitedArticulation = true;
                if (axis.getDefaultHeadMountable() instanceof AbstractNozzle) {
                    AbstractNozzle nozzle = (AbstractNozzle)axis.getDefaultHeadMountable();
                    double[] limits = new double[] {-180, 180};
                    try {
                        limits = nozzle.getRotationModeLimits();
                    }
                    catch (Exception e) {
                        // ignore this here
                    }
                    final double epsilon = 1e-5;
                    if (nozzle.getRotationMode() == RotationMode.LimitedArticulation) {
                        isUnlimitedArticulation = false;
                    }
                    else if (limits[1] - limits[0] < 360 - epsilon) {
                        isUnlimitedArticulation = false;
                        final RotationMode oldRotationMode = nozzle.getRotationMode();
                        solutions.add(new Solutions.Issue(
                                nozzle, 
                                "Rotation axis "+axis.getName()+" is limiting Nozzle "+nozzle.getName()+" to less than 360°. "
                                        + "Must use the " + RotationMode.LimitedArticulation + " rotation mode.", 
                                "Set the " + RotationMode.LimitedArticulation + " rotation mode.", 
                                Severity.Error,
                                "https://github.com/openpnp/openpnp/wiki/Nozzle-Rotation-Mode") {

                            @Override
                            public void setState(Solutions.State state) throws Exception {
                                nozzle.setRotationMode((state == Solutions.State.Solved) ?
                                        RotationMode.LimitedArticulation :
                                            oldRotationMode);
                                super.setState(state);
                            }
                        });
                    }
                    if (!isUnlimitedArticulation) {
                        // Checking that ReferenceBottomVision has pre-rotate enabled. 
                        for (PartAlignment partAlignment : machine.getPartAlignments()) {
                            if (partAlignment instanceof ReferenceBottomVision) {
                                ReferenceBottomVision referenceBottomVision = (ReferenceBottomVision) partAlignment;
                                if (!referenceBottomVision.isPreRotate()) {
                                    solutions.add(new Solutions.Issue(
                                            referenceBottomVision, 
                                            "Pre-rotate bottom vision must be enabled, because the machine has a limited articulation nozzle.", 
                                            "Enable Pre-Rotate.", 
                                            Severity.Error,
                                            "https://github.com/openpnp/openpnp/wiki/Bottom-Vision#global-configuration") {

                                        @Override
                                        public void setState(Solutions.State state) throws Exception {
                                            referenceBottomVision.setPreRotate((state == Solutions.State.Solved));
                                            super.setState(state);
                                        }
                                    });
                                }
                                // Check all parts.
                                List<BottomVisionSettings> visionSettings = new ArrayList<>();
                                List<String> items = new ArrayList<>();
                                for (AbstractVisionSettings settings : Configuration.get().getVisionSettings()) {
                                    if (settings instanceof BottomVisionSettings
                                            && ((BottomVisionSettings) settings).getPreRotateUsage() == PreRotateUsage.AlwaysOff) {
                                        items.add(settings.getName());
                                        visionSettings.add((BottomVisionSettings) settings);
                                    }
                                }
                                items.sort(null);
                                if (!visionSettings.isEmpty()) {
                                    solutions.add(new Solutions.Issue(
                                            referenceBottomVision, 
                                            "Pre-rotate bottom vision must be allowed on all vision settings, because the machine has a "
                                                    + "limited articulation nozzle.", 
                                                    "Switch from "+PreRotateUsage.AlwaysOff+" to "+PreRotateUsage.Default, 
                                                    Severity.Error,
                                            "https://github.com/openpnp/openpnp/wiki/Bottom-Vision#part-configuration") {


                                        @Override 
                                        public String getExtendedDescription() {
                                            return "<html><p>Switch vision settings pre-rotate usage from <strong>"+PreRotateUsage.AlwaysOff+"</strong> "
                                                    + "to <strong>"+PreRotateUsage.Default+"</strong> on these parts:</p>"
                                                    + "<ol><li>"
                                                    + items.stream().collect(Collectors.joining("</li><li>"))
                                                    + "</li><ol>"
                                                    + "</html>";
                                        }

                                        @Override
                                        public void setState(Solutions.State state) throws Exception {
                                            for (BottomVisionSettings setting : visionSettings) {
                                                setting.setPreRotateUsage((state == State.Solved) ?
                                                        PreRotateUsage.Default : PreRotateUsage.AlwaysOff);
                                            }
                                            super.setState(state);
                                        }
                                    });
                                }
                            }
                        }
                    }
                }
                if (isUnlimitedArticulation) {
                    if (axis.getDefaultHeadMountable() instanceof Nozzle) {
                        // Axis is used on nozzle, suggest some optimizations.
                        if (!axis.isWrapAroundRotation()) {
                            solutions.add(new Solutions.Issue(
                                    axis, 
                                    "Rotation can be optimized by wrapping-around the shorter way. Best combined with Limit ±180°.", 
                                    "Enable Wrap Around.", 
                                    Severity.Suggestion,
                                    "https://github.com/openpnp/openpnp/wiki/Machine-Axes#controller-settings-rotational-axis") {

                                @Override
                                public void setState(Solutions.State state) throws Exception {
                                    axis.setWrapAroundRotation((state == Solutions.State.Solved));
                                    super.setState(state);
                                }
                            });
                        }
                        if (!axis.isLimitRotation()) {
                            solutions.add(new Solutions.Issue(
                                    axis, 
                                    "Rotation can be optimized by limiting angles to ±180°. "
                                            + "Best combined with Wrap Around.", 
                                            "Enable Limit to Range.", 
                                            Severity.Suggestion,
                                    "https://github.com/openpnp/openpnp/wiki/Machine-Axes#controller-settings-rotational-axis") {

                                @Override
                                public void setState(Solutions.State state) throws Exception {
                                    axis.setLimitRotation((state == Solutions.State.Solved));
                                    super.setState(state);
                                }
                            });
                        }
                    }
                }
                else {
                    // Limited articulation, we need to treat things differently.
                    if (axis.isWrapAroundRotation()) {
                        solutions.add(new Solutions.Issue(
                                axis, 
                                "Rotation cannot be wrapped-around on a limited articulation axis.", 
                                "Disable Wrap Around.", 
                                Severity.Error,
                                "https://github.com/openpnp/openpnp/wiki/Nozzle-Rotation-Mode#setting-up-the-nozzle-rotation-axis") {

                            @Override
                            public void setState(Solutions.State state) throws Exception {
                                axis.setWrapAroundRotation(!(state == Solutions.State.Solved));
                                super.setState(state);
                            }
                        });
                    }
                    if (!axis.isLimitRotation()) {
                        solutions.add(new Solutions.Issue(
                                axis, 
                                "Rotation must be limited on a limited articulation axis.", 
                                "Enable Limit to Range.", 
                                Severity.Error,
                                "https://github.com/openpnp/openpnp/wiki/Nozzle-Rotation-Mode#setting-up-the-nozzle-rotation-axis") {

                            @Override
                            public void setState(Solutions.State state) throws Exception {
                                axis.setLimitRotation((state == Solutions.State.Solved));
                                super.setState(state);
                            }
                        });
                    }
                }
            }
        }
    }

    protected List<String> getValidAxisLetters() {
        if (axis.getDriver() instanceof GcodeDriver)  { 
            List<String> letters = ((GcodeDriver)axis.getDriver()).getReportedAxesLetters();
            if (letters.size() > 0) {
                return letters;
            }
        }
        return Arrays.asList(AxisSolutions.VALID_AXIS_LETTERS);
    }

    public class AxisLetterIssue extends Solutions.Issue {
        final String oldAxisLetter;
        String newAxisLetter;
        public AxisLetterIssue(Subject subject, String issue, String solution, Severity severity, String uri) {
            super(subject, issue, solution, severity, uri);
            oldAxisLetter = axis.getLetter();
            if (oldAxisLetter.isEmpty()) {
                String suggestedAxisLetter = axis.getName().toUpperCase().substring(0, 1);
                if ("XYZ".contains(suggestedAxisLetter)) {
                    newAxisLetter = suggestedAxisLetter;
                }
            }
            else {
                newAxisLetter = oldAxisLetter;
            }
        }

        @Override
        public void setState(Solutions.State state) throws Exception {
            if (state == State.Solved) {
                if (newAxisLetter.isEmpty()) {
                    throw new Exception("Axis letter must not be empty");
                }
                axis.setLetter(newAxisLetter);
            }
            else {
                axis.setLetter(oldAxisLetter);
            }
            super.setState(state);
        }

        @Override
        public Solutions.Issue.CustomProperty[] getProperties() {
            return new Solutions.Issue.CustomProperty[] {
                    new Solutions.Issue.StringProperty(
                            "Axis Letter",
                            "Axis letter as used in G-code sent to the controller.") {

                        @Override
                        public String get() {
                            return newAxisLetter;
                        }

                        @Override
                        public void set(String value) {
                            newAxisLetter = value;
                        }

                        @Override
                        public String[] getSuggestions() {
                            ArrayList<String> list = new ArrayList<>();
                            for (String axisLetter : getValidAxisLetters()) {
                                boolean taken = false;
                                for (Axis otherAxis : machine.getAxes()) {
                                    if (otherAxis != axis && otherAxis instanceof AbstractControllerAxis) {
                                        AbstractControllerAxis otherControllerAxis = (AbstractControllerAxis) otherAxis;
                                        if (otherControllerAxis.getDriver() == axis.getDriver()
                                                && otherControllerAxis.getLetter().equals(axisLetter)) {
                                            taken = true;
                                            break;
                                        }
                                    }
                                }
                                if (!taken) {
                                    list.add(axisLetter);
                                }
                            }
                            return list.toArray(new String[list.size()]);
                        }
                    }
            };
        }
    }
}
