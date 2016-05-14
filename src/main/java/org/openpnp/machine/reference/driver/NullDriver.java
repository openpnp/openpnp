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
import org.openpnp.machine.reference.ReferencePasteDispenser;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Head;
import org.openpnp.spi.PropertySheetHolder;
import org.simpleframework.xml.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An example of the simplest possible driver that can support multiple heads. This driver maintains
 * a set of coordinates for each Head that it is asked to handle and simply logs all commands sent
 * to it.
 */
public class NullDriver implements ReferenceDriver {
    private final static Logger logger = LoggerFactory.getLogger(NullDriver.class);

    @Attribute(required = false)
    private double feedRateMmPerMinute = 5000;

    private HashMap<Head, Location> headLocations = new HashMap<>();

    private boolean enabled;

    /**
     * Gets the Location object being tracked for a specific Head. This is the absolute coordinates
     * of a virtual Head on the machine.
     * 
     * @param head
     * @return
     */
    protected Location getHeadLocation(Head head) {
        Location l = headLocations.get(head);
        if (l == null) {
            l = new Location(LengthUnit.Millimeters, 0, 0, 0, 0);
            setHeadLocation(head, l);
        }
        return l;
    }

    protected void setHeadLocation(Head head, Location l) {
        headLocations.put(head, l);
    }

    @Override
    public void home(ReferenceHead head) throws Exception {
        logger.debug("home()");
        checkEnabled();
        setHeadLocation(head, getHeadLocation(head).derive(0.0, 0.0, 0.0, 0.0));
    }

    /**
     * Return the Location of a specific ReferenceHeadMountable on the machine. We get the
     * coordinates for the Head the object is attached to, and then we add the offsets assigned to
     * the object to make the coordinates correct for that object.
     */
    @Override
    public Location getLocation(ReferenceHeadMountable hm) {
        return getHeadLocation(hm.getHead()).add(hm.getHeadOffsets());
    }

    /**
     * Commands the driver to move the given ReferenceHeadMountable to the specified Location at the
     * given speed. Please see the comments for this method in the code for some important
     * considerations when writing your own driver.
     */
    @Override
    public void moveTo(ReferenceHeadMountable hm, Location location, double speed)
            throws Exception {
        logger.debug("moveTo({}, {}, {})", new Object[] {hm, location, speed});
        checkEnabled();

        // Subtract the offsets from the incoming Location. This converts the
        // offset coordinates to driver / absolute coordinates.
        location = location.subtract(hm.getHeadOffsets());

        // Convert the Location to millimeters, since that's the unit that
        // this driver works in natively.
        location = location.convertToUnits(LengthUnit.Millimeters);

        // Get the current location of the Head that we'll move
        Location hl = getHeadLocation(hm.getHead());

        if (feedRateMmPerMinute > 0) {
            simulateMovement(hm, location, hl, speed);
        }

        // Now that movement is complete, update the stored Location to the new
        // Location, unless the incoming Location specified an axis with a value
        // of NaN. NaN is interpreted to mean "Don't move this axis" so we don't
        // update the value, either.

        hl = hl.derive(Double.isNaN(location.getX()) ? null : location.getX(),
                Double.isNaN(location.getY()) ? null : location.getY(),
                Double.isNaN(location.getZ()) ? null : location.getZ(),
                Double.isNaN(location.getRotation()) ? null : location.getRotation());

        setHeadLocation(hm.getHead(), hl);
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
    protected void simulateMovement(ReferenceHeadMountable hm, Location location, Location hl,
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
            setHeadLocation(hm.getHead(), hl);

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
    public void pick(ReferenceNozzle nozzle) throws Exception {
        logger.debug("pick({})", nozzle);
        checkEnabled();
        if (feedRateMmPerMinute > 0) {
            Thread.sleep(500);
        }
    }

    @Override
    public void place(ReferenceNozzle nozzle) throws Exception {
        logger.debug("place({})", nozzle);
        checkEnabled();
        if (feedRateMmPerMinute > 0) {
            Thread.sleep(500);
        }
    }

    @Override
    public void actuate(ReferenceActuator actuator, double value) throws Exception {
        logger.debug("actuate({}, {})", actuator, value);
        checkEnabled();
        if (feedRateMmPerMinute > 0) {
            Thread.sleep(500);
        }
    }

    @Override
    public void actuate(ReferenceActuator actuator, boolean on) throws Exception {
        logger.debug("actuate({}, {})", actuator, on);
        checkEnabled();
        if (feedRateMmPerMinute > 0) {
            Thread.sleep(500);
        }
    }

    @Override
    public void dispense(ReferencePasteDispenser dispenser, Location startLocation,
            Location endLocation, long dispenseTimeMilliseconds) throws Exception {
        logger.debug("dispense({}, {}, {}, {})",
                new Object[] {dispenser, startLocation, endLocation, dispenseTimeMilliseconds});
        checkEnabled();
        Thread.sleep(dispenseTimeMilliseconds);
    }

    @Override
    public void setEnabled(boolean enabled) throws Exception {
        logger.debug("setEnabled({})", enabled);
        this.enabled = enabled;
    }

    @Override
    public Wizard getConfigurationWizard() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {new PropertySheetWizardAdapter(getConfigurationWizard())};
    }

    @Override
    public Icon getPropertySheetHolderIcon() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        // TODO Auto-generated method stub
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
        // TODO Auto-generated method stub

    }
}
