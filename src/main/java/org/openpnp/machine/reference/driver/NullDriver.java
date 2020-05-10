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

package org.openpnp.machine.reference.driver;

import java.io.IOException;
import java.util.HashMap;

import javax.swing.Action;
import javax.swing.Icon;

import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.ReferenceDriver;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.machine.reference.ReferenceHeadMountable;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.MappedAxes;
import org.openpnp.spi.Head;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.base.AbstractDriver;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;

/**
 * An example of the simplest possible driver that can support multiple heads. This driver maintains
 * a set of coordinates for each Head that it is asked to handle and simply logs all commands sent
 * to it.
 */
public class NullDriver extends AbstractDriver implements ReferenceDriver {

    @Attribute(required = false)
    private double feedRateMmPerMinute = 5000;

    private boolean enabled;

    @Override
    public void home(ReferenceHead head, MappedAxes mappedAxes, Location location) throws Exception {
        Logger.debug("home()");
        checkEnabled();
        mappedAxes.setLocation(location, this);
    }


    @Override
    public void resetLocation(ReferenceHead head, MappedAxes mappedAxes, Location location)
            throws Exception {
        mappedAxes.setLocation(location, this);
    }

    /**
     * Commands the driver to move the given ReferenceHeadMountable to the specified Location at the
     * given speed. Please see the comments for this method in the code for some important
     * considerations when writing your own driver.
     */
    @Override
    public void moveTo(ReferenceHeadMountable hm, MappedAxes mappedAxes, Location location, double speed)
            throws Exception {
        Logger.debug("moveTo({}, {}, {})", hm, location, speed);
        checkEnabled();

        // Convert the Location to millimeters, since that's the unit that
        // this driver works in natively.
        location = location.convertToUnits(LengthUnit.Millimeters);

        // Get the current location of the Head that we'll move
        Location hl = mappedAxes.getLocation(this);

        if (feedRateMmPerMinute > 0) {
            simulateMovement(hm, mappedAxes, location, hl, speed);
        }

        // Now that movement is complete, update the stored Location to the new
        // Location.
        mappedAxes.setLocation(location, this);
    }

    /**
     * Simulates true machine movement, which takes time, by tracing the required movement lines
     * over a period of time based on the input speed.
     * 
     * @param hm
     * @param location
     * @param hl
     * @param speed
     * @throws Exception
     */
    protected void simulateMovement(ReferenceHeadMountable hm, MappedAxes mappedAxes, Location location, Location hl,
            double speed) throws Exception {
        double x = hl.getX();
        double y = hl.getY();
        double z = hl.getZ();
        double c = hl.getRotation();

        double x1 = x;
        double y1 = y;
        double z1 = z;
        double c1 = c;
        double x2 = Double.isNaN(location.getX()) ? x : location.getX();
        double y2 = Double.isNaN(location.getY()) ? y : location.getY();
        double z2 = Double.isNaN(location.getZ()) ? z : location.getZ();
        double c2 = Double.isNaN(location.getRotation()) ? c : location.getRotation();

        c2 = c2 % 360.0;

        // Calculate the linear distance to travel in each axis.
        double vx = x2 - x1;
        double vy = y2 - y1;
        double vz = z2 - z1;
        double vc = c2 - c1;

        // Calculate the linear distance to travel in each plane XY, Z and C.
        double pxy = Math.sqrt(vx * vx + vy * vy);
        double pz = Math.abs(vz);
        double pc = Math.abs(vc);

        // Distance moved in each plane so far.
        double dxy = 0, dz = 0, dc = 0;

        // The distance that we'll move each loop.
        double distancePerTick = (feedRateMmPerMinute * speed) / 60.0 / 10.0;
        double distancePerTickC = distancePerTick * 10;

        while (dxy < pxy || dz < pz || dc < pc) {
            if (dxy < pxy) {
                x = x1 + (vx / pxy * dxy);
                y = y1 + (vy / pxy * dxy);
            }
            else {
                x = x2;
                y = y2;
            }
            if (dz < pz) {
                z = z1 + dz * (vz < 0 ? -1 : 1);
            }
            else {
                z = z2;
            }
            if (dc < pc) {
                c = c1 + dc * (vc < 0 ? -1 : 1);
            }
            else {
                c = c2;
            }

            hl = hl.derive(x, y, z, c);
            mappedAxes.setLocation(hl, this);

            // Provide live updates to the Machine as the move progresses.
            ((ReferenceMachine) Configuration.get().getMachine())
                    .fireMachineHeadActivity(hm.getHead());

            try {
                Thread.sleep(100);
            }
            catch (Exception e) {

            }

            dxy = Math.min(pxy, dxy + distancePerTick);
            dz = Math.min(pz, dz + distancePerTick);
            dc = Math.min(pc, dc + distancePerTickC);
        }
    }

    @Override
    public void actuate(ReferenceActuator actuator, double value) throws Exception {
        Logger.debug("actuate({}, {})", actuator, value);
        checkEnabled();
        if (feedRateMmPerMinute > 0) {
            Thread.sleep(500);
        }
    }

    @Override
    public void actuate(ReferenceActuator actuator, boolean on) throws Exception {
        Logger.debug("actuate({}, {})", actuator, on);
        checkEnabled();
        if (feedRateMmPerMinute > 0) {
            Thread.sleep(500);
        }
    }
    
    @Override
    public String actuatorRead(ReferenceActuator actuator) throws Exception {
        return Math.random() + "";
    }

    @Override
    public void setEnabled(boolean enabled) throws Exception {
        Logger.debug("setEnabled({})", enabled);
        this.enabled = enabled;
    }

    @Override
    public Wizard getConfigurationWizard() {
        return null;
    }
    private void checkEnabled() throws Exception {
        if (!enabled) {
            throw new Exception("Driver is not yet enabled!");
        }
    }

    public double getFeedRateMmPerMinute() {
        return feedRateMmPerMinute;
    }

    public void setFeedRateMmPerMinute(double feedRateMmPerMinute) {
        this.feedRateMmPerMinute = feedRateMmPerMinute;
    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public LengthUnit getUnits() {
        return LengthUnit.Millimeters;
    }

    @Deprecated
    @Override
    public void migrateDriver(ReferenceMachine machine) throws Exception {
        machine.addDriver(this);
        createAxisMappingDefaults(machine); 
    }
}
