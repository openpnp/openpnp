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

import javax.swing.Action;

import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.psh.ActuatorsPropertySheetHolder;
import org.openpnp.machine.reference.psh.CamerasPropertySheetHolder;
import org.openpnp.machine.reference.psh.NozzlesPropertySheetHolder;
import org.openpnp.machine.reference.solutions.HeadSolutions;
import org.openpnp.machine.reference.wizards.ReferenceHeadConfigurationWizard;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.model.Motion.MotionOption;
import org.openpnp.model.Part;
import org.openpnp.model.Solutions;
import org.openpnp.spi.Axis;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Locatable.LocationOption;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.base.AbstractHead;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;

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
                    axesHomingLocation = hm.toRaw(hm.toHeadLocation(getHomingFiducialLocation()));
                }
                else {
                    // Use bare X, Y homing coordinates (legacy mode).
                    axesHomingLocation =  new AxesLocation(machine, 
                            (axis) -> (axis.getHomeCoordinate())); 
                    // For best legacy support, we suppress the camera calibration head offsets, but we do not
                    // suppress the head offsets in general. Whether it was a bug or a feature to not account for the 
                    // head offsets, can be left open. 
                    Location cameraHomingLocation = hm.toHeadMountableLocation(hm.toTransformed(axesHomingLocation), 
                            LocationOption.SuppressCameraCalibration);
                    // Having the legacy camera location, we can derive the axesHomingLocation, this time accounting 
                    // for the camera calibration head offsets.
                    axesHomingLocation = hm.toRaw(hm.toHeadLocation(cameraHomingLocation));
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

    public enum NozzleSolution {
        Standalone,
        DualNegated,
        DualCam
    }

    @Attribute(required=false) 
    private NozzleSolution nozzleSolution;
    @Attribute(required=false) 
    int nozzleSolutionsMultiplier = 1;

    public NozzleSolution getNozzleSolution() {
        return nozzleSolution;
    }

    public void setNozzleSolution(NozzleSolution nozzleSolution) {
        this.nozzleSolution = nozzleSolution;
    }

    public int getNozzleSolutionsMultiplier() {
        return nozzleSolutionsMultiplier;
    }

    public void setNozzleSolutionsMultiplier(int nozzleSolutionsMultiplier) {
        this.nozzleSolutionsMultiplier = nozzleSolutionsMultiplier;
    }

    @Override
    public void findIssues(Solutions solutions) {
        new HeadSolutions(this).findIssues(solutions);
        super.findIssues(solutions);
    }
}