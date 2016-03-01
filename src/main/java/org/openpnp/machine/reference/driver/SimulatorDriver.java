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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Locale;

import javax.swing.Action;
import javax.swing.Icon;

import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.ReferenceDriver;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.machine.reference.ReferenceHeadMountable;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.machine.reference.ReferencePasteDispenser;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Head;
import org.openpnp.spi.PropertySheetHolder;
import org.simpleframework.xml.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimulatorDriver implements ReferenceDriver {
    private final static Logger logger = LoggerFactory.getLogger(SimulatorDriver.class);

    @Attribute(required = false)
    private double feedRateMmPerMinute;

    private HashMap<Head, Location> headLocations = new HashMap<>();

    private boolean enabled;

    private Socket socket;
    private DataInputStream in;
    private PrintStream out;

    public SimulatorDriver() throws Exception {
        connect();
    }

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
        send("h");
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

        String movable;
        if (hm.toString().equals("N1")) {
            movable = "Nozzle1";
        }
        else if (hm.toString().equals("N2")) {
            movable = "Nozzle2";
        }
        else if (hm.toString().contains("Camera")) {
            movable = "Camera";
        }
        else if (hm.toString().equals("A1")) {
            movable = "Actuator";
        }
        else {
            throw new Exception("Don't know what " + hm.toString() + " is.");
        }

        send(String.format(Locale.US, "m,%s,%f,%f,%f,%f", movable, location.getX(), location.getY(),
                location.getZ(), location.getRotation()));

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
            Location endLocation, long dispenseTimeMilliseconds) throws Exception {}

    @Override
    public void setEnabled(boolean enabled) throws Exception {
        logger.debug("setEnabled({})", enabled);
        this.enabled = enabled;
    }

    private void checkEnabled() throws Exception {
        if (!enabled) {
            throw new Exception("Driver is not yet enabled!");
        }
    }

    // TODO: This reconnect stuff totally doesn't work
    private void connect() {
        if (socket == null || !socket.isConnected()) {
            System.out.println("Connecting to simulator...");
        }
        while (socket == null || !socket.isConnected()) {
            try {
                socket = new Socket("localhost", 9037);
                in = new DataInputStream(socket.getInputStream());
                out = new PrintStream(socket.getOutputStream());
                System.out.println("Connected!");
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void send(String s) {
        try {
            connect();
            out.print(s);
            out.print("\n");
            String line = in.readLine();
            if (!line.trim().equals("ok")) {
                throw new Exception("Didn't expect: " + line);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
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
    public Action[] getPropertySheetHolderActions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Icon getPropertySheetHolderIcon() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void close() throws IOException {
        // TODO Auto-generated method stub

    }
}
