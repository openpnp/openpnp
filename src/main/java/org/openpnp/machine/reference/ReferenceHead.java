/*
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

package org.openpnp.machine.reference;

import java.util.ArrayList;
import java.util.LinkedHashSet;

import javax.swing.Action;

import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.axis.ReferenceCamClockwiseAxis;
import org.openpnp.machine.reference.axis.ReferenceCamCounterClockwiseAxis;
import org.openpnp.machine.reference.axis.ReferenceControllerAxis;
import org.openpnp.machine.reference.axis.ReferenceMappedAxis;
import org.openpnp.machine.reference.psh.ActuatorsPropertySheetHolder;
import org.openpnp.machine.reference.psh.CamerasPropertySheetHolder;
import org.openpnp.machine.reference.psh.NozzlesPropertySheetHolder;
import org.openpnp.machine.reference.wizards.ReferenceHeadConfigurationWizard;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.Location;
import org.openpnp.model.Motion.MotionOption;
import org.openpnp.model.Part;
import org.openpnp.model.Solutions;
import org.openpnp.model.Solutions.Severity;
import org.openpnp.model.Solutions.State;
import org.openpnp.spi.Axis;
import org.openpnp.spi.Camera;
import org.openpnp.spi.CoordinateAxis;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.base.AbstractAxis;
import org.openpnp.spi.base.AbstractHead;
import org.openpnp.spi.base.AbstractHeadMountable;
import org.openpnp.spi.base.AbstractNozzle;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;

public class ReferenceHead extends AbstractHead {

    @Override
    public void home() throws Exception {
        Logger.debug("{}.home()", getName());

        // Note, don't call super.home() yet, need to do the physical homing first.
        if (getVisualHomingMethod() != VisualHomingMethod.None) {
            /*
             * The head default camera should now be (if everything has homed correctly) directly
             * above the homing fiducial on the machine bed, use the head camera scan for this and make sure
             * this is exactly central - otherwise we move the camera until it is, and then reset all
             * the axis back to the fiducial location as this is calibrated home.
             */
            HeadMountable hm = getDefaultCamera();
            Part homePart = Configuration.get().getPart("FIDUCIAL-HOME");
            if (homePart == null) {
                throw new Exception("Visual homing is missing the FIDUCIAL-HOME part. Please create it.");
            }
            Location homingLocation = Configuration.get().getMachine().getFiducialLocator()
                    .getHomeFiducialLocation(getHomingFiducialLocation(), homePart);
            if (homingLocation == null) {
                // Homing failed
                throw new Exception("Visual homing failed");
            }

            ReferenceMachine machine = getMachine();
            AxesLocation axesHomingLocation;
            if (getVisualHomingMethod() == VisualHomingMethod.ResetToFiducialLocation) {
                // Convert fiducial location to raw coordinates
                // TODO: are you sure the toHeadLocation() is needed?
                axesHomingLocation = hm.toRaw(hm.toHeadLocation(getHomingFiducialLocation()));
            }
            else {
                // Use bare X, Y homing coordinates (legacy mode).
                axesHomingLocation =  new AxesLocation(machine, 
                        (axis) -> (axis.getHomeCoordinate())); 
            }
            // Just take the X and Y axes.
            axesHomingLocation = axesHomingLocation.byType(Axis.Type.X, Axis.Type.Y); 
            // Reset to the homing fiducial location as the new Working Coordinate System.
            machine.getMotionPlanner().setGlobalOffsets(axesHomingLocation);
        }
        // Now that the machine is physically homed, do the logical homing.
        super.home();
        // Let everybody know.
        getMachine().fireMachineHeadActivity(this);
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceHeadConfigurationWizard(this);
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName() + " " + getName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        ArrayList<PropertySheetHolder> children = new ArrayList<>();
        children.add(new NozzlesPropertySheetHolder(this, "Nozzles", getNozzles(), null));
        children.add(new CamerasPropertySheetHolder(this, "Cameras", getCameras(), null));
        children.add(new ActuatorsPropertySheetHolder(this, "Actuators", getActuators(), null));
        return children.toArray(new PropertySheetHolder[] {});
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {new PropertySheetWizardAdapter(getConfigurationWizard())};
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return null;
    }

    @Override
    public void moveToSafeZ(double speed) throws Exception {
        Logger.debug("{}.moveToSafeZ({})", getName(), speed);
        super.moveToSafeZ(speed);
    }

    @Override 
    public boolean isInsideSoftLimits(HeadMountable hm, Location location)  throws Exception {
        if (hm instanceof ReferenceHeadMountable) {
            Location headLocation = ((AbstractHeadMountable) hm).toHeadLocation(location);
            AxesLocation axesLocation = ((AbstractHeadMountable) hm).toRaw(headLocation);
            return (getMachine().getMotionPlanner().isValidLocation(axesLocation));
        }
        return true;
    }

    @Override 
    public void moveTo(HeadMountable hm, Location location, double speed, MotionOption... options) throws Exception {
        ReferenceMachine machine = getMachine();
        AxesLocation mappedAxes = hm.getMappedAxes(machine);
        if (!mappedAxes.isEmpty()) {
            AxesLocation axesLocation = hm.toRaw(location);
            machine.getMotionPlanner().moveTo(hm, axesLocation, speed, options);
        }
    }

    @Override
    public String toString() {
        return getName();
    }

    public ReferenceMachine getMachine() {
        return (ReferenceMachine) Configuration.get().getMachine();
    }

    private enum NozzleSolution {
        Standalone,
        DualNegated,
        DualCam
    }

    @Attribute(required=false) 
    private NozzleSolution nozzleSolution;
    @Attribute(required=false) 
    int numberOfNozzleSolutions = 1;
    
    public int getNumberOfNozzleSolutions() {
        return numberOfNozzleSolutions;
    }

    public void setNumberOfNozzleSolutions(int n) {
        this.numberOfNozzleSolutions = n;
    }


    @Override
    public void findIssues(Solutions solutions) {
        Camera camera = null;
        boolean isDefaultHead = false;
        try {
            camera = getDefaultCamera();
            isDefaultHead = (getMachine().getDefaultHead() == this); 
        }
        catch (Exception e) {
        }
        if (camera != null) {
            
            final Camera theCamera = camera;
            if (isDefaultHead) { 
                solutions.add(new Solutions.Issue(
                        this, 
                        "Create nozzles for this head.", 
                        "Choose the type and number of your nozzles", 
                        Solutions.Severity.Fundamental,
                        "https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Nozzle-Setup") {
                    {
                        setChoice(nozzleSolution);
                    }
                    @Override
                    public void setState(Solutions.State state) throws Exception {
                        if (state == State.Solved) {
                            if (confirm("<html>The nozzle solution can only be applied (multiple times), it cannot be undone,<br/>"
                                    + "your existing nozzle and axis configuration will be oeverwritten.<br/><br/>"
                                    +"Are you sure?</html>", true)) {
                                createNozzleSolution(theCamera, (NozzleSolution) getChoice(), getNumberOfNozzleSolutions());
                            }
                        }
                        super.setState(state);
                    }

                    @Override
                    public boolean canBeUndone() {
                        return false;
                    }

                    @Override
                    public Solutions.Choice[] getChoices() {
                        return new Solutions.Choice[] {
                                new Solutions.Choice(NozzleSolution.Standalone, 
                                        "<html><h3>Standalone Nozzle<h3>"
                                                + "<p>The nozzle(s) have their own dedicated Z axis motor</p>"
                                                + "</html>",
                                                Icons.nozzleSingle),
                                new Solutions.Choice(NozzleSolution.DualNegated, 
                                        "<html><h3>Dual Nozzle, Shared Z Axis, Negated<h3>"
                                                + "<p>Two nozzles share a Z axis motor. "
                                                + "The second nozzle moves equally up when the first one moves down. "
                                                + "The nozzles are negatively coupled by rack and pinion or belt.</p>"
                                                + "</html>",
                                                Icons.nozzleDualNeg),
                                new Solutions.Choice(NozzleSolution.DualCam, 
                                        "<html><h3>Dual Nozzle, Shared Z Axis, Cam<h3>"
                                                + "<p>Two nozzles share a Z axis motor. "
                                                + "The two nozzles are pushed down by a rotational cam, and pulled up with a spring.</p>"
                                                + "</html>",
                                                Icons.nozzleDualCam),
                        };
                    }
                });
            }
            if (nozzleSolution != null) {

                if (camera.getAxisX() == null) {
                    addMissingAxisIssue(solutions, camera, Axis.Type.X);
                }
                if (camera.getAxisY() == null) {
                    addMissingAxisIssue(solutions, camera, Axis.Type.Y);
                }
                if (camera.getAxisX() != null && camera.getAxisY() != null) {
                    for (HeadMountable hm : getHeadMountables()) {
                        addInconsistentAxisIssue(solutions, camera, hm, Axis.Type.X);
                        addInconsistentAxisIssue(solutions, camera, hm, Axis.Type.Y);
                        if (hm instanceof Nozzle) {
                            if (hm.getAxisZ() == null) {
                                solutions.add(new Solutions.PlainIssue(
                                        this, 
                                        "Nozzle "+hm.getName()+" does not have a Z axis assigned.", 
                                        "Please assign a proper Z axis. You might need to create one first.", 
                                        Severity.Error,
                                        "https://github.com/openpnp/openpnp/wiki/Mapping-Axes"));
                            }
                            if (hm.getAxisRotation() == null) {
                                solutions.add(new Solutions.PlainIssue(
                                        this, 
                                        "Nozzle "+hm.getName()+" does not have a Rotation axis assigned.", 
                                        "Please assign a proper Rotation axis. You might need to create one first.", 
                                        Severity.Error,
                                        "https://github.com/openpnp/openpnp/wiki/Mapping-Axes"));
                            }
                            if (hm.getAxisZ() != null && hm.getAxisRotation() != null) {
                                for (Nozzle nozzle2 : getNozzles()) {
                                    if (nozzle2 == hm) {
                                        break;
                                    }
                                    if (nozzle2.getAxisZ() == hm.getAxisZ()) {
                                        solutions.add(new Solutions.PlainIssue(
                                                this, 
                                                "Nozzles "+nozzle2.getName()+" and "+hm.getName()+" have the same Z axis assigned.", 
                                                "Please assign a different Z axis.", 
                                                Severity.Error,
                                                "https://github.com/openpnp/openpnp/wiki/Mapping-Axes"));
                                    }
                                    if (nozzle2.getAxisRotation() == hm.getAxisRotation()) {
                                        solutions.add(new Solutions.PlainIssue(
                                                this, 
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
        super.findIssues(solutions);
    }
    protected void addMissingAxisIssue(Solutions solutions, final Camera camera, Axis.Type type) {
        // Find a default axis.
        final AbstractAxis axis = getMachine().getDefaultAxis(type);
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

    private void createNozzleSolution(Camera camera, NozzleSolution nozzleSolution, int n) throws Exception {
        // Recycle existing nozzles and axes.
        LinkedHashSet<AbstractNozzle> nozzles = new LinkedHashSet<>();
        LinkedHashSet<AbstractAxis> axesZ = new LinkedHashSet<>();
        LinkedHashSet<AbstractAxis> axesC = new LinkedHashSet<>();
        LinkedHashSet<ReferenceMappedAxis> axesNegated = new LinkedHashSet<>();
        LinkedHashSet<ReferenceCamCounterClockwiseAxis> axesCam1 = new LinkedHashSet<>();
        LinkedHashSet<ReferenceCamClockwiseAxis> axesCam2 = new LinkedHashSet<>();
        for (Nozzle nozzle : getNozzles()) {
            if (nozzle instanceof AbstractNozzle) { 
                Axis axisZ = nozzle.getAxisZ();
                Axis axisC = nozzle.getAxisRotation();
                // Collect nozzle.
                nozzles.add((AbstractNozzle) nozzle);
                // Collect the underlying raw axes. 
                axesZ.add((AbstractAxis) getRawAxis(axisZ));
                axesC.add((AbstractAxis) getRawAxis(axisC));
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
        }
        for (int i = 0; i < n; i++) {
            String suffix = n > 1 ? String.valueOf(i+1) : "";
            String suffix1 = String.valueOf(i*2+1);
            String suffix2 = String.valueOf(i*2+2);
            switch (nozzleSolution) {
                case Standalone: {
                    AbstractNozzle n1 = reuseOrCreateNozzle(camera, nozzles, suffix);
                    n1.setAxisZ(reuseOrCreateAxis(axesZ, ReferenceControllerAxis.class, Axis.Type.Z, suffix));
                    n1.setAxisRotation(reuseOrCreateAxis(axesC, ReferenceControllerAxis.class, Axis.Type.Rotation, suffix));
                    break;
                }
                case DualNegated: { 
                    AbstractNozzle n1 = reuseOrCreateNozzle(camera, nozzles, suffix1);
                    AbstractNozzle n2 = reuseOrCreateNozzle(camera, nozzles, suffix2);
                    AbstractAxis z1 = reuseOrCreateAxis(axesZ, ReferenceControllerAxis.class, Axis.Type.Z, suffix1);
                    AbstractAxis c1 = reuseOrCreateAxis(axesC, ReferenceControllerAxis.class, Axis.Type.Rotation, suffix1);
                    ReferenceMappedAxis z2 = reuseOrCreateAxis(axesNegated, ReferenceMappedAxis.class, Axis.Type.Z, suffix2);
                    z2.setInputAxis(z1);
                    z2.setMapInput0(new Length(0, Configuration.get().getSystemUnits()));
                    z2.setMapInput1(new Length(1, Configuration.get().getSystemUnits()));
                    z2.setMapOutput0(new Length(0, Configuration.get().getSystemUnits()));
                    z2.setMapOutput1(new Length(-1, Configuration.get().getSystemUnits()));
                    AbstractAxis c2 = reuseOrCreateAxis(axesC, ReferenceControllerAxis.class, Axis.Type.Rotation, suffix2);
                    n1.setAxisZ(z1);
                    n1.setAxisRotation(c1);
                    n2.setAxisZ(z2);
                    n2.setAxisRotation(c2);
                    break;
                }
                case DualCam: {
                    AbstractNozzle n1 = reuseOrCreateNozzle(camera, nozzles, suffix1);
                    AbstractNozzle n2 = reuseOrCreateNozzle(camera, nozzles, suffix2);
                    AbstractAxis zn = reuseOrCreateAxis(axesZ, ReferenceControllerAxis.class, Axis.Type.Z, "N"+suffix);
                    AbstractAxis c1 = reuseOrCreateAxis(axesC, ReferenceControllerAxis.class, Axis.Type.Rotation, suffix1);
                    ReferenceCamCounterClockwiseAxis z1 = reuseOrCreateAxis(axesCam1, ReferenceCamCounterClockwiseAxis.class, Axis.Type.Z, suffix1);
                    z1.setInputAxis(zn);
                    ReferenceCamClockwiseAxis z2 = reuseOrCreateAxis(axesCam2, ReferenceCamClockwiseAxis.class, Axis.Type.Z, suffix2);
                    z2.setInputAxis(z1);;
                    AbstractAxis c2 = reuseOrCreateAxis(axesC, ReferenceControllerAxis.class, Axis.Type.Rotation, suffix2);
                    n1.setAxisZ(z1);
                    n1.setAxisRotation(c1);
                    n2.setAxisZ(z2);
                    n2.setAxisRotation(c2);
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
            getMachine().removeAxis(unusedAxis);
        }
        // Cleanup unused nozzles
        for (AbstractNozzle unusedNozzle : nozzles) {
            removeNozzle(unusedNozzle);
        }
        this.nozzleSolution = nozzleSolution;
    }

    private CoordinateAxis getRawAxis(Axis axis) { 
        try {
            return ((AbstractAxis) axis).getCoordinateAxes(getMachine()).getAxis(axis.getType());
        }
        catch (Exception e) {
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
            addNozzle(nozzle);
        }
        else {
            nozzles.remove(nozzle);
        }
        nozzle.setName("N"+i);
        nozzle.setAxisX((AbstractAxis) camera.getAxisX());
        nozzle.setAxisY((AbstractAxis) camera.getAxisX());
        return nozzle;
    }

    private <T extends Axis, C extends T> T reuseOrCreateAxis(LinkedHashSet<T> axes, Class<C> cls, Axis.Type type, String i)
            throws Exception {
        T axis = null;
        for (T a : axes) {
            axis = a;
            break;
        }
        if (axis == null) {
            axis = cls.newInstance();
            axis.setType(type);
            int pos = getMachine().getAxes().size();
            getMachine().addAxis(axis);
            while (pos > 0 && getMachine().getAxes().get(pos-1).getType().ordinal() > type.ordinal()) {
                getMachine().permutateAxis(axis, -1);
                pos--;
            }
        }
        else {
            axes.remove(axis);
        }
        axis.setName(type.getDefaultLetter()+i);
        return axis;
    }
}
