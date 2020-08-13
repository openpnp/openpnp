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

package org.openpnp.machine.rapidplacer;

import javax.swing.Action;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceFeeder;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

public class RapidFeeder extends ReferenceFeeder {
    static String actuatorName = "RAPIDFEEDER";
    
    @Element(required = false)
    protected Location scanStartLocation = new Location(LengthUnit.Millimeters);

    @Element(required = false)
    protected Location scanEndLocation = new Location(LengthUnit.Millimeters);

    @Element(required = false)
    protected Length scanIncrement = new Length(4, LengthUnit.Millimeters);
    
    @Attribute(required = false)
    protected String address;
    
    @Attribute(required = false)
    protected int pitch = 4;

    @Override
    public Location getPickLocation() throws Exception {
        return location;
    }

    @Override
    public void feed(Nozzle nozzle) throws Exception {
        Actuator actuator = nozzle.getHead().getActuatorByName(actuatorName);
        if (actuator == null) {
            actuator = Configuration.get().getMachine().getActuatorByName(actuatorName);
        }
        if (actuator == null) {
            throw new Exception("Feed failed. Unable to find an actuator named " + actuatorName);
        }
        actuator.actuate(String.format("%s %d", address, pitch));
    }
    
	@Override
    public Wizard getConfigurationWizard() {
        return new RapidFeederConfigurationWizard(this);
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName() + " " + getName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        return null;
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return null;
    }

    public Location getScanStartLocation() {
        return scanStartLocation;
    }

    public void setScanStartLocation(Location scanStartLocation) {
        this.scanStartLocation = scanStartLocation;
        firePropertyChange("scanStartLocation", null, scanStartLocation);
    }

    public Location getScanEndLocation() {
        return scanEndLocation;
    }

    public void setScanEndLocation(Location scanEndLocation) {
        this.scanEndLocation = scanEndLocation;
        firePropertyChange("scanEndLocation", null, scanEndLocation);
    }

    public Length getScanIncrement() {
        return scanIncrement;
    }

    public void setScanIncrement(Length scanIncrement) {
        this.scanIncrement = scanIncrement;
        firePropertyChange("scanIncrements", null, scanIncrement);
    }
    
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
        firePropertyChange("address", null, address);
    }

    public int getPitch() {
        return pitch;
    }

    public void setPitch(int pitch) {
        this.pitch = pitch;
        firePropertyChange("pitch", null, pitch);
    }
}
