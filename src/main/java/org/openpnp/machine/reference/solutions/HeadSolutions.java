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

import java.util.ArrayList;
import java.util.LinkedHashSet;

import org.openpnp.Translations;
import org.openpnp.gui.support.Icons;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.machine.reference.ReferenceHead.NozzleSolution;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.machine.reference.axis.ReferenceCamClockwiseAxis;
import org.openpnp.machine.reference.axis.ReferenceCamCounterClockwiseAxis;
import org.openpnp.machine.reference.axis.ReferenceControllerAxis;
import org.openpnp.machine.reference.axis.ReferenceMappedAxis;
import org.openpnp.machine.reference.axis.ReferenceVirtualAxis;
import org.openpnp.machine.reference.driver.GcodeDriver;
import org.openpnp.machine.reference.driver.GcodeDriver.CommandType;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.Solutions;
import org.openpnp.model.Solutions.Milestone;
import org.openpnp.model.Solutions.Severity;
import org.openpnp.model.Solutions.State;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Axis;
import org.openpnp.spi.Axis.Type;
import org.openpnp.spi.Camera;
import org.openpnp.spi.CoordinateAxis;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.base.AbstractAxis;
import org.openpnp.spi.base.AbstractControllerAxis;
import org.openpnp.spi.base.AbstractHeadMountable;
import org.openpnp.spi.base.AbstractNozzle;
import org.pmw.tinylog.Logger;

/**
 * This helper class implements the Issues & Solutions for the ReferenceHead. 
 * The idea is not to pollute the head implementation itself.
 *
 */
public class HeadSolutions implements Solutions.Subject {
    private final ReferenceHead head;

    public HeadSolutions(ReferenceHead head) {
        this.head = head;
    }

    @Override
    public void findIssues(Solutions solutions) {
        Camera camera = null;
        boolean isDefaultHead = false;
        try {
            camera = head.getDefaultCamera();
            isDefaultHead = (head.getMachine().getDefaultHead() == head); 
        }
        catch (Exception e) {
        }
        if (camera != null) {

            final Camera theCamera = camera;
            if (isDefaultHead && solutions.isTargeting(Milestone.Welcome)) { 
                solutions.add(new Solutions.Issue(
                        head,
                        Translations.getString("HeadSolutions.Issue.CreateNozzles"),
                        Translations.getString("HeadSolutions.Solution.CreateNozzles"),
                        Solutions.Severity.Fundamental,
                        "https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Nozzle-Setup") {

                    {
                        NozzleSolution nozzleSolution = head.getNozzleSolution();
                        if (nozzleSolution == null) {
                            // This is a first time evaluation. Determine the current machine configuration.
                            // Note, this will fail, if this is a mixed or otherwise exotic solution machine, but the user is responsible to 
                            // not accept the solution then.
                            int multiplier = 0;
                            nozzleSolution = NozzleSolution.Standalone;
                            for (Nozzle nozzle : head.getNozzles()) {
                                if (nozzle.getAxisZ() instanceof ReferenceCamCounterClockwiseAxis) {
                                    multiplier++; // counts dual cam
                                    nozzleSolution = NozzleSolution.DualCam;
                                }
                                else if (nozzle.getAxisZ() instanceof ReferenceMappedAxis) {
                                    // not counted (will be counted by its ReferenceControllerAxis counterpart)
                                    nozzleSolution = NozzleSolution.DualNegated;
                                }
                                else if (nozzle.getAxisZ() instanceof ReferenceControllerAxis) {
                                    multiplier++; // counts standalone or dual negated 
                                }
                            }
                            if (multiplier == 0) {
                                multiplier++;
                            }
                            head.setNozzleSolution(nozzleSolution);
                            head.setNozzleSolutionsMultiplier(multiplier);
                            if (! solutions.isAtMostTargeting(Milestone.Welcome)) {
                                // If this is not a fresh machine i.e. not starting with the Welcome Milestone, 
                                // remember this as already solved (it can be revisited by re-opening it).
                                solutions.setSolutionsIssueSolved(this, true);
                            }
                        }
                        setChoice(nozzleSolution);
                        multiplier = head.getNozzleSolutionsMultiplier();
                    }
                    @Override
                    public void setState(Solutions.State state) throws Exception {
                        if (state == State.Solved) {
                            if (solutions.confirm(Translations.getString(
                                    "HeadSolutions.Issue.CreateNozzles.Confirm"), true)) {
                                createNozzleSolution(theCamera, (NozzleSolution) getChoice(), multiplier);
                                // Remember this is solved (it can be revisited).
                                solutions.setSolutionsIssueSolved(this, true);
                                super.setState(state);
                            }
                        }
                        else {
                            solutions.setSolutionsIssueSolved(this, false);
                            super.setState(state);
                        }
                    }

                    private int multiplier;

                    @Override
                    public Solutions.Issue.CustomProperty[] getProperties() {
                        return new Solutions.Issue.CustomProperty[] {
                                new Solutions.Issue.IntegerProperty(
                                        Translations.getString(
                                                "HeadSolutions.Solution.CreateNozzles.NumberOfNozzlesLabel.text"),
                                        Translations.getString(
                                                "HeadSolutions.Solution.CreateNozzles.NumberOfNozzlesLabel.toolTipText"
                                        ), 1, 8) {
                                    @Override
                                    public int get() {
                                        return multiplier;
                                    }
                                    @Override
                                    public void set(int value) {
                                        multiplier = value;
                                    }
                                },
                        };
                    }
                    @Override
                    public Solutions.Issue.Choice[] getChoices() {
                        return new Solutions.Issue.Choice[]{
                                new Solutions.Issue.Choice(NozzleSolution.Standalone, Translations.getString(
                                        "HeadSolutions.Solution.CreateNozzles.Choice.0"),
                                        Icons.nozzleSingle),
                                new Solutions.Issue.Choice(NozzleSolution.DualNegated, Translations.getString(
                                        "HeadSolutions.Solution.CreateNozzles.Choice.1"),
                                        Icons.nozzleDualNeg),
                                new Solutions.Issue.Choice(NozzleSolution.DualCam, Translations.getString(
                                        "HeadSolutions.Solution.CreateNozzles.Choice.2"),
                                        Icons.nozzleDualCam),
                        };
                    }
                });
            }
            if (solutions.isTargeting(Milestone.Basics)) {
                if (camera.getAxisX() == null) {
                    addMissingAxisIssue(solutions, camera, Axis.Type.X);
                }
                if (camera.getAxisY() == null) {
                    addMissingAxisIssue(solutions, camera, Axis.Type.Y);
                }
                if (camera.getAxisZ() == null) {
                    addMissingAxisIssue(solutions, camera, Axis.Type.Z);
                }
                if (camera.getAxisRotation() == null) {
                    addMissingAxisIssue(solutions, camera, Axis.Type.Rotation);
                }
                if (camera.getAxisX() != null && camera.getAxisY() != null) {
                    for (HeadMountable hm : head.getHeadMountables()) {
                        addInconsistentAxisIssue(solutions, camera, hm, Axis.Type.X);
                        addInconsistentAxisIssue(solutions, camera, hm, Axis.Type.Y);
                        if (hm instanceof Nozzle) {
                            perNozzleSolutions(solutions, (Nozzle) hm);
                        }
                    }
                }

                if (head.getPumpActuator() != null) {
                    ActuatorSolutions.findActuateIssues(solutions, head, head.getPumpActuator(), "pump control",
                        "https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Vacuum-Setup#pump-control-setup");
                }
                if (head.getzProbeActuator() != null) {
                    ActuatorSolutions.findActuatorReadIssues(solutions, head, head.getzProbeActuator(), "Z probe",
                        "https://github.com/openpnp/openpnp/wiki/Z-Probing");
                }
            }
        }
    }

    protected void perNozzleSolutions(Solutions solutions, Nozzle nozzle) {
        if (nozzle.getAxisZ() == null) {
            solutions.add(new Solutions.PlainIssue(
                    nozzle, 
                    "Nozzle "+nozzle.getName()+" does not have a Z axis assigned.", 
                    "Please assign a proper Z axis. You might need to create one first.", 
                    Severity.Error,
                    "https://github.com/openpnp/openpnp/wiki/Mapping-Axes"));
        }
        if (nozzle.getAxisRotation() == null) {
            solutions.add(new Solutions.PlainIssue(
                    nozzle, 
                    "Nozzle "+nozzle.getName()+" does not have a Rotation axis assigned.", 
                    "Please assign a proper Rotation axis. You might need to create one first.", 
                    Severity.Error,
                    "https://github.com/openpnp/openpnp/wiki/Mapping-Axes"));
        }
        if (nozzle.getAxisZ() != null && nozzle.getAxisRotation() != null) {
            for (Camera camera2 : head.getCameras()) {
                if (camera2.getAxisZ() == nozzle.getAxisZ()) {
                    sameAxisAssignedIssue(solutions, camera2, nozzle, camera2.getAxisZ(), Axis.Type.Z);
                }
                if (camera2.getAxisRotation() == nozzle.getAxisRotation()) {
                    sameAxisAssignedIssue(solutions, camera2, nozzle, camera2.getAxisRotation(), Axis.Type.Rotation);
                }
            }
            for (Nozzle nozzle2 : head.getNozzles()) {
                if (nozzle2 == nozzle) {
                    break;
                }
                if (nozzle2.getAxisZ() == nozzle.getAxisZ()) {
                    solutions.add(new Solutions.PlainIssue(
                            nozzle, 
                            "Nozzles "+nozzle2.getName()+" and "+nozzle.getName()+" have the same Z axis assigned.", 
                            "Please assign a different Z axis.", 
                            Severity.Error,
                            "https://github.com/openpnp/openpnp/wiki/Mapping-Axes"));
                }
                if (nozzle2.getAxisRotation() == nozzle.getAxisRotation()) {
                    solutions.add(new Solutions.PlainIssue(
                            nozzle, 
                            "Nozzles "+nozzle2.getName()+" and "+nozzle.getName()+" have the same Rotation axis assigned.", 
                            "It is OK to share rotation axes. If intentional, just dismiss this issue. Otherwise assign a different Rotation axis.", 
                            Severity.Information,
                            "https://github.com/openpnp/openpnp/wiki/Mapping-Axes"));
                }
            }
        }
    }

    protected void sameAxisAssignedIssue(Solutions solutions, Camera camera, HeadMountable hm,
            Axis axis, Axis.Type type) {
        solutions.add(new Solutions.Issue(
                head, 
                "Camera "+camera.getName()+" and "+hm.getName()+" have the same "+type+" axis "+axis.getName()+" assigned.", 
                "Unassign the "+type+" axis "+axis.getName()+" from the camera "+camera.getName()+". Later you can press Find Issues & Solutions "
                        + "again to get a new Solution for a virtual axis replacement.",
                Severity.Error,
                "https://github.com/openpnp/openpnp/wiki/Machine-Axes#referencevirtualaxis") {
            @Override
            public void setState(Solutions.State state) throws Exception {
                ((AbstractHeadMountable) camera).setAxis(
                        ((AbstractAxis)(state == State.Solved ? null : axis)),
                        type);
                super.setState(state);
            }
        });
    }
    protected void addMissingAxisIssue(Solutions solutions, final Camera camera, Axis.Type type) {
        // Find a default axis.
        AbstractAxis suggestedAxis;
        boolean isNewAxis = false;
        boolean isXY = (type == Type.X || type == Type.Y);
        if (isXY) {
            suggestedAxis = head.getMachine().getDefaultAxis(type);
        }
        else {
            suggestedAxis = null;
            for (Axis axisCand : head.getMachine().getAxes()) {
                if (axisCand.getType() == type && axisCand instanceof ReferenceVirtualAxis) {
                    suggestedAxis = (AbstractAxis) axisCand;
                    break;
                }
            }
            if (suggestedAxis == null) {
                ReferenceVirtualAxis virtualAxis = new ReferenceVirtualAxis();
                isNewAxis = true;
                virtualAxis.setType(type);
                virtualAxis.setName(type+" "+camera.getName());
                suggestedAxis = virtualAxis;
            }
        }
        final AbstractAxis axis = suggestedAxis;
        final boolean isNew = isNewAxis;
        solutions.add(new Solutions.Issue(
                camera, 
                (isXY ? 
                        "Missing "+type.name()+" axis assignment. Assign one to continue.":
                            "Virtual "+type.name()+" axis recommended for camera "+camera.getName()+"."), 
                (axis == null ? 
                        "Create and assign a "+type.name()+" axis manually."  
                        : "Assign "+(isNew ? "new " : "existing ")+axis.getClass().getSimpleName()+" "+axis.getName()+" as "+type.name()+"."), 
                isXY ? Severity.Fundamental : Severity.Suggestion,
                "https://github.com/openpnp/openpnp/wiki/Mapping-Axes") {

            @Override
            public void setState(Solutions.State state) throws Exception {
                ((AbstractHeadMountable) camera).setAxis(
                        ((AbstractAxis)(state == State.Solved ? axis : null)),
                        type);
                if (isNew) {
                    try {
                        if (state == State.Solved) {
                            head.getMachine().addAxis(axis);
                        }
                        else {
                            head.getMachine().removeAxis(axis);
                        }
                    }
                    catch (Exception e) {
                        Logger.warn(e);
                    }
                }
                super.setState(state);
            }

            @Override
            public boolean canBeAccepted() {
                return axis != null;
            }
        });
    }

    protected void addInconsistentAxisIssue(Solutions solutions, final Camera camera,
            HeadMountable hm, Axis.Type type) {
        final Axis oldAxis = hm.getAxis(type);
        if ((hm instanceof Nozzle || oldAxis != null) 
                && oldAxis != camera.getAxis(type)) {
            solutions.add(new Solutions.Issue(
                    hm, 
                    "Inconsistent "+type.name()+" axis assignment "
                            +(oldAxis != null ? oldAxis.getName() : "null")
                            +" (not the same as default camera "+camera.getName()+").", 
                            "Assign "+camera.getAxis(type).getName()+" as the "+type.name()+" axis.", 
                            (hm instanceof Nozzle) ? Severity.Error : Severity.Warning,
                    "https://github.com/openpnp/openpnp/wiki/Mapping-Axes") {

                @Override
                public void setState(Solutions.State state) throws Exception {
                    ((AbstractHeadMountable) hm).setAxis(
                            ((AbstractAxis)(state == State.Solved ? camera.getAxis(type) : oldAxis)),
                            type);
                    super.setState(state);
                }
            });
        }
    }

    private synchronized void createNozzleSolution(Camera camera, 
            NozzleSolution nozzleSolution, int nozzleSolutionsMultiplier) throws Exception {
        Configuration.get().save();
        // Recycle existing nozzles and axes.
        LinkedHashSet<AbstractNozzle> nozzles = new LinkedHashSet<>();
        LinkedHashSet<AbstractAxis> axesZ = new LinkedHashSet<>();
        LinkedHashSet<AbstractAxis> axesC = new LinkedHashSet<>();
        LinkedHashSet<ReferenceMappedAxis> axesNegated = new LinkedHashSet<>();
        LinkedHashSet<ReferenceCamCounterClockwiseAxis> axesCam1 = new LinkedHashSet<>();
        LinkedHashSet<ReferenceCamClockwiseAxis> axesCam2 = new LinkedHashSet<>();
        LinkedHashSet<Actuator> valveActuators = new LinkedHashSet<>();
        LinkedHashSet<Actuator> senseActuators = new LinkedHashSet<>();
        for (Nozzle nozzle : head.getNozzles()) {
            if (nozzle instanceof AbstractNozzle) { 
                Axis axisZ = nozzle.getAxisZ();
                Axis axisC = nozzle.getAxisRotation();
                // Collect nozzle.
                nozzles.add((AbstractNozzle) nozzle);
                // Collect the underlying raw axes. 
                axesZ.add((AbstractAxis) getRawAxis(head.getMachine(), axisZ));
                axesC.add((AbstractAxis) getRawAxis(head.getMachine(), axisC));
                // Collect the transformed axes.
                if (axisZ instanceof ReferenceMappedAxis) {
                    axesNegated.add((ReferenceMappedAxis) axisZ);
                }
                if (axisZ instanceof ReferenceCamCounterClockwiseAxis) {
                    axesCam1.add((ReferenceCamCounterClockwiseAxis) axisZ);
                }
                if (axisZ instanceof ReferenceCamClockwiseAxis) {
                    axesCam2.add((ReferenceCamClockwiseAxis) axisZ);
                }
            }
            if (nozzle instanceof ReferenceNozzle) {
                ReferenceNozzle refNozzle = (ReferenceNozzle) nozzle;
                senseActuators.add(refNozzle.getVacuumSenseActuator());
                // Sensing and valve actuator were sometimes shared, but we want to regenerate these as separate Actuators,
                // so only add the valve actuator, if not shared.
                if (refNozzle.getVacuumSenseActuator() != refNozzle.getVacuumActuator()) {
                    valveActuators.add(refNozzle.getVacuumActuator());
                }
            }
        }
        for (int i = 0; i < nozzleSolutionsMultiplier; i++) {
            String suffix = nozzleSolutionsMultiplier > 1 ? String.valueOf(i+1) : "";
            String suffix1 = String.valueOf(i*2+1);
            String suffix2 = String.valueOf(i*2+2);
            switch (nozzleSolution) {
                case Standalone: {
                    AbstractNozzle n1 = reuseOrCreateNozzle(camera, nozzles, suffix);
                    n1.setAxisZ(reuseOrCreateAxis(camera, axesZ, ReferenceControllerAxis.class, Axis.Type.Z, suffix));
                    n1.setAxisRotation(reuseOrCreateAxis(camera, axesC, ReferenceControllerAxis.class, Axis.Type.Rotation, suffix));
                    assignVacuumActuators(n1, valveActuators, senseActuators, suffix);
                    break;
                }
                case DualNegated: { 
                    AbstractNozzle n1 = reuseOrCreateNozzle(camera, nozzles, suffix1);
                    AbstractNozzle n2 = reuseOrCreateNozzle(camera, nozzles, suffix2);
                    AbstractAxis z1 = reuseOrCreateAxis(camera, axesZ, ReferenceControllerAxis.class, Axis.Type.Z, suffix1);
                    AbstractAxis c1 = reuseOrCreateAxis(camera, axesC, ReferenceControllerAxis.class, Axis.Type.Rotation, suffix1);
                    ReferenceMappedAxis z2 = reuseOrCreateAxis(camera, axesNegated, ReferenceMappedAxis.class, Axis.Type.Z, suffix2);
                    z2.setInputAxis(z1);
                    z2.setMapInput0(new Length(0, Configuration.get().getSystemUnits()));
                    z2.setMapInput1(new Length(1, Configuration.get().getSystemUnits()));
                    z2.setMapOutput0(new Length(0, Configuration.get().getSystemUnits()));
                    z2.setMapOutput1(new Length(-1, Configuration.get().getSystemUnits()));
                    AbstractAxis c2 = reuseOrCreateAxis(camera, axesC, ReferenceControllerAxis.class, Axis.Type.Rotation, suffix2);
                    n1.setAxisZ(z1);
                    n1.setAxisRotation(c1);
                    n2.setAxisZ(z2);
                    n2.setAxisRotation(c2);
                    assignVacuumActuators(n1, valveActuators, senseActuators, suffix1);
                    assignVacuumActuators(n2, valveActuators, senseActuators, suffix2);
                    break;
                }
                case DualCam: {
                    AbstractNozzle n1 = reuseOrCreateNozzle(camera, nozzles, suffix1);
                    AbstractNozzle n2 = reuseOrCreateNozzle(camera, nozzles, suffix2);
                    AbstractAxis zn = reuseOrCreateAxis(camera, axesZ, ReferenceControllerAxis.class, Axis.Type.Z, "N"+suffix);
                    AbstractAxis c1 = reuseOrCreateAxis(camera, axesC, ReferenceControllerAxis.class, Axis.Type.Rotation, suffix1);
                    ReferenceCamCounterClockwiseAxis z1 = reuseOrCreateAxis(camera, axesCam1, ReferenceCamCounterClockwiseAxis.class, Axis.Type.Z, suffix1);
                    z1.setInputAxis(zn);
                    ReferenceCamClockwiseAxis z2 = reuseOrCreateAxis(camera, axesCam2, ReferenceCamClockwiseAxis.class, Axis.Type.Z, suffix2);
                    z2.setInputAxis(z1);;
                    AbstractAxis c2 = reuseOrCreateAxis(camera, axesC, ReferenceControllerAxis.class, Axis.Type.Rotation, suffix2);
                    n1.setAxisZ(z1);
                    n1.setAxisRotation(c1);
                    n2.setAxisZ(z2);
                    n2.setAxisRotation(c2);
                    assignVacuumActuators(n1, valveActuators, senseActuators, suffix1);
                    assignVacuumActuators(n2, valveActuators, senseActuators, suffix2);
                    break;
                }
            }
        }
        // Cleanup unused axes
        ArrayList<Axis> unused = new ArrayList<>();
        unused.addAll(axesNegated);
        unused.addAll(axesCam1);
        unused.addAll(axesCam2);
        unused.addAll(axesZ);
        unused.addAll(axesC);
        for (Axis unusedAxis : unused) {
            head.getMachine().removeAxis(unusedAxis);
        }
        // Cleanup unused nozzles
        for (AbstractNozzle unusedNozzle : nozzles) {
            head.removeNozzle(unusedNozzle);
        }
        // Cleanup unused Actuators
        ArrayList<Actuator> unusedActuators = new ArrayList<>();
        unusedActuators.addAll(valveActuators);
        unusedActuators.addAll(senseActuators);
        for (Actuator unusedActuator : unusedActuators) {
            head.removeActuator(unusedActuator);
        }
        // Store the solution.
        head.setNozzleSolution(nozzleSolution);
        head.setNozzleSolutionsMultiplier(nozzleSolutionsMultiplier);
    }

    public void assignVacuumActuators(AbstractNozzle n, 
            LinkedHashSet<Actuator> valveActuators, LinkedHashSet<Actuator> senseActuators,
            String suffix) throws Exception {
        if (n instanceof ReferenceNozzle) {
            ReferenceNozzle nozzle = (ReferenceNozzle) n;
            nozzle.setVacuumActuator(reuseOrCreateActuator(valveActuators, "VAC"+suffix));
            nozzle.setVacuumSenseActuator(reuseOrCreateActuator(senseActuators, "VACS"+suffix));
            Actuator vacuumSenseActuator = nozzle.getVacuumSenseActuator();
            Actuator vacuumValveActuator = nozzle.getVacuumActuator();
            if (vacuumSenseActuator.getDriver() instanceof GcodeDriver) {
                // Sensing and valve actuator were sometimes shared, so when regenerating these as separate Actuators,
                // the command would be lost. Instead, reuse the valve ACTUATE_BOOLEAN_COMMAND command. 
                GcodeDriver driver = (GcodeDriver) vacuumSenseActuator.getDriver();
                if (driver.getCommand(vacuumSenseActuator, CommandType.ACTUATE_BOOLEAN_COMMAND) != null
                        && driver.getCommand(vacuumValveActuator, CommandType.ACTUATE_BOOLEAN_COMMAND) == null) {
                    driver.setCommand(vacuumValveActuator, CommandType.ACTUATE_BOOLEAN_COMMAND, driver.getCommand(vacuumSenseActuator, CommandType.ACTUATE_BOOLEAN_COMMAND));
                }
            }
        }
    }

    public static CoordinateAxis getRawAxis(Machine machine, Axis axis) { 
        if (axis instanceof AbstractAxis) {
            try {
                return ((AbstractAxis) axis).getCoordinateAxes(machine).getAxis(axis.getType());
            }
            catch (Exception e) {
            }
        }
        return null;
    }

    private AbstractNozzle reuseOrCreateNozzle(Camera camera, LinkedHashSet<AbstractNozzle> nozzles, String i)
            throws Exception {
        AbstractNozzle nozzle = null;
        for (AbstractNozzle n : nozzles) {
            nozzle = n;
            break;
        }
        if (nozzle == null) {
            nozzle = new ReferenceNozzle();
            head.addNozzle(nozzle);
        }
        else {
            nozzles.remove(nozzle);
        }
        nozzle.setName("N"+i);
        nozzle.setAxisX((AbstractAxis) camera.getAxisX());
        nozzle.setAxisY((AbstractAxis) camera.getAxisY());
        return nozzle;
    }

    private <T extends Axis, C extends T> T reuseOrCreateAxis(Camera camera, LinkedHashSet<T> axes, Class<C> cls, Axis.Type type, String i)
            throws Exception {
        T axis = null;
        for (T a : axes) {
            axis = a;
            break;
        }
        if (axis == null) {
            axis = cls.newInstance();
            axis.setType(type);
            ReferenceMachine machine = head.getMachine();
            int pos = machine.getAxes().size();
            machine.addAxis(axis);
            while (pos > 0 && machine.getAxes().get(pos-1).getType().ordinal() > type.ordinal()) {
                head.getMachine().permutateAxis(axis, -1);
                pos--;
            }
            if (axis instanceof AbstractControllerAxis 
                    && camera.getAxisX() instanceof AbstractControllerAxis) {
                // Inherit the driver.
                ((AbstractControllerAxis) axis).setDriver(((AbstractControllerAxis) camera.getAxisX()).getDriver());
            }
        }
        else {
            axes.remove(axis);
        }
        axis.setName(type.getDefaultLetter()+i);
        return axis;
    }

    private Actuator reuseOrCreateActuator(LinkedHashSet<Actuator> actuators, String i)
            throws Exception {
        Actuator actuator = null;
        for (Actuator a : actuators) {
            actuator = a;
            break;
        }
        if (actuator == null) {
            actuator = new ReferenceActuator();
            head.addActuator(actuator);
        }
        else {
            actuators.remove(actuator);
        }
        actuator.setName("A"+i);
        return actuator;
    }
}
