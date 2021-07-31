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
import org.openpnp.spi.Camera;
import org.openpnp.spi.CoordinateAxis;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.base.AbstractAxis;
import org.openpnp.spi.base.AbstractControllerAxis;
import org.openpnp.spi.base.AbstractHeadMountable;
import org.openpnp.spi.base.AbstractNozzle;

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
            if (isDefaultHead && solutions.getTargetMilestone() == Milestone.Welcome) { 
                solutions.add(new Solutions.Issue(
                        head, 
                        "Create nozzles for this head.", 
                        "Choose the number and type of your nozzles.", 
                        Solutions.Severity.Fundamental,
                        "https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Nozzle-Setup") {

                    {
                        setChoice(head.getNozzleSolution());
                        multiplier = head.getNozzleSolutionsMultiplier();
                    }
                    @Override
                    public void setState(Solutions.State state) throws Exception {
                        if (state == State.Solved) {
                            if (solutions.confirm("<html>"
                                    + "<p>Accepting this solution may change your machine configuration fundamentally.<br/>"
                                    + "The following will happen:<p/>"
                                    + "<ol>"
                                    + "<li>The current machine configuration is saved (same as File/Save Configuration).</li>"
                                    + "<li>The new solution overwrites your existing nozzle and axis configuration.</li>"
                                    + "<li>As far as nozzles and axes remain the same type and count, their detail configuration is preserved.</li>"
                                    + "<li>A nozzle solution can be applied multiple times, you can revisit and expand it.</li>"
                                    + "<li><span color=\"red\">Caution:</span> Reopen will not restore the previous configuration, only enable a fresh choice.<br/>"
                                    + "If you want to restore previous configuration you must restore the saved configuration manually.</li>"
                                    + "</ol>"
                                    + "<br/>"
                                    +"Are you sure?</html>", true)) {
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
                                        "Number of Nozzle Units",
                                        "The Number of Nozzles or Pairs of Nozzles",
                                        1, 8) {
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
                        return new Solutions.Issue.Choice[] {
                                new Solutions.Issue.Choice(NozzleSolution.Standalone, 
                                        "<html><h3>Standalone Nozzle</h3>"
                                                + "<p>A nozzle has its own dedicated Z axis motor</p>"
                                                + "</html>",
                                                Icons.nozzleSingle),
                                new Solutions.Issue.Choice(NozzleSolution.DualNegated, 
                                        "<html><h3>Nozzle Pair, Shared Z Axis, Negated</h3>"
                                                + "<p>A nozzle pair shares a Z axis motor. "
                                                + "When the first nozzle moves up, then second one moves down equally. "
                                                + "The nozzles are negatively coupled by rack and pinion or belt.</p>"
                                                + "</html>",
                                                Icons.nozzleDualNeg),
                                new Solutions.Issue.Choice(NozzleSolution.DualCam, 
                                        "<html><h3>Nozzle Pair, Shared Z Axis, Cam</h3>"
                                                + "<p>A nozzle pair shares a Z axis motor. "
                                                + "The two nozzles are pushed down by a rotational cam, pulled up with a spring.</p>"
                                                + "</html>",
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
                if (camera.getAxisX() != null && camera.getAxisY() != null) {
                    for (HeadMountable hm : head.getHeadMountables()) {
                        addInconsistentAxisIssue(solutions, camera, hm, Axis.Type.X);
                        addInconsistentAxisIssue(solutions, camera, hm, Axis.Type.Y);
                        if (hm instanceof Nozzle) {
                            if (hm.getAxisZ() == null) {
                                solutions.add(new Solutions.PlainIssue(
                                        hm, 
                                        "Nozzle "+hm.getName()+" does not have a Z axis assigned.", 
                                        "Please assign a proper Z axis. You might need to create one first.", 
                                        Severity.Error,
                                        "https://github.com/openpnp/openpnp/wiki/Mapping-Axes"));
                            }
                            if (hm.getAxisRotation() == null) {
                                solutions.add(new Solutions.PlainIssue(
                                        hm, 
                                        "Nozzle "+hm.getName()+" does not have a Rotation axis assigned.", 
                                        "Please assign a proper Rotation axis. You might need to create one first.", 
                                        Severity.Error,
                                        "https://github.com/openpnp/openpnp/wiki/Mapping-Axes"));
                            }
                            if (hm.getAxisZ() != null && hm.getAxisRotation() != null) {
                                for (Nozzle nozzle2 : head.getNozzles()) {
                                    if (nozzle2 == hm) {
                                        break;
                                    }
                                    if (nozzle2.getAxisZ() == hm.getAxisZ()) {
                                        solutions.add(new Solutions.PlainIssue(
                                                head, 
                                                "Nozzles "+nozzle2.getName()+" and "+hm.getName()+" have the same Z axis assigned.", 
                                                "Please assign a different Z axis.", 
                                                Severity.Error,
                                                "https://github.com/openpnp/openpnp/wiki/Mapping-Axes"));
                                    }
                                    if (nozzle2.getAxisRotation() == hm.getAxisRotation()) {
                                        solutions.add(new Solutions.PlainIssue(
                                                head, 
                                                "Nozzles "+nozzle2.getName()+" and "+hm.getName()+" have the same Rotation axis assigned.", 
                                                "Please assign a different Rotation axis.", 
                                                Severity.Error,
                                                "https://github.com/openpnp/openpnp/wiki/Mapping-Axes"));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    protected void addMissingAxisIssue(Solutions solutions, final Camera camera, Axis.Type type) {
        // Find a default axis.
        final AbstractAxis axis = head.getMachine().getDefaultAxis(type);
        solutions.add(new Solutions.Issue(
                camera, 
                "Missing "+type.name()+" axis assignment. Assign one to continue.", 
                (axis == null ? 
                        "Create and assign a "+type.name()+" axis manually."  
                        : "Assign "+axis.getName()+" as "+type.name()+"."), 
                Severity.Fundamental,
                "https://github.com/openpnp/openpnp/wiki/Mapping-Axes") {

            @Override
            public void setState(Solutions.State state) throws Exception {
                ((AbstractHeadMountable) camera).setAxis(
                        ((AbstractAxis)(state == State.Solved ? axis : null)),
                        type);
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
