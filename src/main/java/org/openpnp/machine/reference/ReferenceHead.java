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
import java.util.List;

import javax.swing.Action;

import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.psh.ActuatorsPropertySheetHolder;
import org.openpnp.machine.reference.psh.CamerasPropertySheetHolder;
import org.openpnp.machine.reference.psh.NozzlesPropertySheetHolder;
import org.openpnp.machine.reference.wizards.ReferenceHeadConfigurationWizard;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.model.Motion.MotionOption;
import org.openpnp.model.Part;
import org.openpnp.model.Solutions;
import org.openpnp.model.Solutions.Issue;
import org.openpnp.model.Solutions.Severity;
import org.openpnp.model.Solutions.State;
import org.openpnp.spi.Axis;
import org.openpnp.spi.Camera;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.base.AbstractAxis;
import org.openpnp.spi.base.AbstractHead;
import org.openpnp.spi.base.AbstractHeadMountable;
import org.pmw.tinylog.Logger;

public class ReferenceHead extends AbstractHead {

    @Override
    public void home() throws Exception {
        // Note, don't call super.home() yet, need to do visual homing first.
        Logger.debug("{}.home()", getName());
        ReferenceMachine machine = getMachine();

        visualHome(machine, true);

        super.home();
        // Let everybody know.
        getMachine().fireMachineHeadActivity(this);
    }

    public void visualHome(ReferenceMachine machine, boolean apply) throws Exception {
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

            if (apply) {
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
                // Reset to the axes homing location as the new Working Coordinate System.
                machine.getMotionPlanner().setGlobalOffsets(axesHomingLocation);
            }
        }
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

    @Override
    public void findIssues(List<Issue> issues) {
        super.findIssues(issues);
        Camera camera = null;
        try {
            camera = getDefaultCamera();
        }
        catch (Exception e) {
        }
        if (camera != null) {
            if (camera.getAxisX() == null) {
                addMissingAxisIssue(issues, camera, Axis.Type.X);
            }
            if (camera.getAxisY() == null) {
                addMissingAxisIssue(issues, camera, Axis.Type.Y);
            }
            if (camera.getAxisX() != null && camera.getAxisY() != null) {
                for (HeadMountable hm : getHeadMountables()) {
                    addInconsistentAxisIssue(issues, camera, hm, Axis.Type.X);
                    addInconsistentAxisIssue(issues, camera, hm, Axis.Type.Y);
                    if (hm instanceof Nozzle) {
                        if (hm.getAxisZ() == null) {
                            issues.add(new Solutions.PlainIssue(
                                    this, 
                                    "Nozzle "+hm.getName()+" does not have a Z axis assigned.", 
                                    "Please assign a proper Z axis. You might need to create one first.", 
                                    Severity.Error,
                                    "https://github.com/openpnp/openpnp/wiki/Mapping-Axes"));
                        }
                        if (hm.getAxisRotation() == null) {
                            issues.add(new Solutions.PlainIssue(
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
                                    issues.add(new Solutions.PlainIssue(
                                            this, 
                                            "Nozzles "+nozzle2.getName()+" and "+hm.getName()+" have the same Z axis assigned.", 
                                            "Please assign a different Z axis.", 
                                            Severity.Error,
                                            "https://github.com/openpnp/openpnp/wiki/Mapping-Axes"));
                                }
                                if (nozzle2.getAxisRotation() == hm.getAxisRotation()) {
                                    issues.add(new Solutions.PlainIssue(
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

    protected void addMissingAxisIssue(List<Issue> issues, final Camera camera, Axis.Type type) {
        // Find a default axis.
        final AbstractAxis axis = getMachine().getDefaultAxis(type);
        issues.add(new Solutions.Issue(
                camera, 
                "Missing "+type.name()+" axis assignment. Assign one to continue.", 
                (axis == null ? 
                        "Create and assign "+type.name()+" axis."  
                        : "Assign "+axis.getName()+" as "+type.name()+"."), 
                Severity.Fundamental,
                "https://github.com/openpnp/openpnp/wiki/Mapping-Axes") {

            @Override
            public void setState(Solutions.State state) throws Exception {
                if (confirmStateChange(state)) {
                    ((AbstractHeadMountable) camera).setAxis(
                            ((AbstractAxis)(state == State.Solved ? axis : null)),
                            type);
                    super.setState(state);
                }
            }

            @Override
            public boolean canBeAutoSolved() {
                return axis != null;
            }
        });
    }

    protected void addInconsistentAxisIssue(List<Issue> issues, final Camera camera,
            HeadMountable hm, Axis.Type type) {
        final Axis oldAxis = hm.getAxis(type);
        if ((hm instanceof Nozzle || oldAxis != null) 
                && oldAxis != camera.getAxis(type)) {
            issues.add(new Solutions.Issue(
                    hm, 
                    "Inconsistent "+type.name()+" axis assignment "
                            +(oldAxis != null ? oldAxis.getName() : "null")
                            +" (not the same as default camera "+camera.getName()+").", 
                            "Assign "+camera.getAxisX().getName()+" as "+type.name()+".", 
                            (hm instanceof Nozzle) ? Severity.Error : Severity.Warning,
                    "https://github.com/openpnp/openpnp/wiki/Mapping-Axes") {

                @Override
                public void setState(Solutions.State state) throws Exception {
                    if (confirmStateChange(state)) {
                        ((AbstractHeadMountable) hm).setAxis(
                                ((AbstractAxis)(state == State.Solved ? camera.getAxis(type) : oldAxis)),
                                type);
                        super.setState(state);
                    }
                }
            });
        }
    }
}
