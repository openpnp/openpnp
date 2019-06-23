/*
 * Copyright (C) 2019 <mark@makr.zone>
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

import java.util.HashMap;
import java.util.Map;

import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.machine.reference.wizards.ContactProbeNozzleWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Feeder;
import org.openpnp.util.Collect;
import org.openpnp.util.MovableUtils;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Element;

public class ContactProbeNozzle extends ReferenceNozzle {
    
    public ContactProbeNozzle() {
       super();
    }

    public ContactProbeNozzle(String id) {
        super(id);
    }

    @Element(required = false)
    private String contactProbeActuatorName = "";

    @Element(required = false)
    private Length probeOffset = new Length(1, LengthUnit.Millimeters);

    @Override
    public PropertySheet[] getPropertySheets() {
        return Collect.concat(super.getPropertySheets(), 
                new PropertySheet[] { new PropertySheetWizardAdapter(new ContactProbeNozzleWizard(this), "Contact Probe") });
    }

    @Override
    public void moveToPickLocation(Feeder feeder) throws Exception {
        Location location = feeder.getPickLocation();
        try {
            Map<String, Object> globals = new HashMap<>();
            globals.put("nozzle", this);
            globals.put("feeder", feeder);
            globals.put("location", location);
            Configuration.get().getScripting().on("Nozzle.MoveToPickLocation", globals);
        }
        catch (Exception e) {
            Logger.warn(e);
        }

        // Move nozzle to Z offset above the target location.
        location = location.add(new Location(this.probeOffset.getUnits(), 0, 0, probeOffset.getValue(), 0));
        MovableUtils.moveToLocationAtSafeZ(this, location);
        // Probe down from there.
        actuateContactProbe(true);
    }

    @Override
    public void pick(Part part) throws Exception {
        super.pick(part);
        // Probe retract.
        actuateContactProbe(false);
    }

    @Override
    public void moveToPlacementLocation(Location location) throws Exception {
        try {
            Map<String, Object> globals = new HashMap<>();
            globals.put("nozzle", this);
            globals.put("location", location);
            Configuration.get().getScripting().on("Nozzle.MoveToPlacementLocation", globals);
        }
        catch (Exception e) {
            Logger.warn(e);
        }
        
        // Move nozzle to Z offset above the target location.
        location = location.add(new Location(this.probeOffset.getUnits(), 0, 0, probeOffset.getValue(), 0));
        MovableUtils.moveToLocationAtSafeZ(this, location);
        // Probe down from there.
        actuateContactProbe(true);
    }

    @Override
    public void place() throws Exception {
        super.place();
        // Probe retract.
        actuateContactProbe(false);
    }

    protected Actuator getContactProbeActuator() throws Exception {
        Actuator actuator = getHead().getActuatorByName(contactProbeActuatorName);
        if (actuator == null) {
            throw new Exception(String.format("Can't find contact probe actuator %s", contactProbeActuatorName));
        }
        return actuator;
    }
    
    protected void actuateContactProbe(boolean on) throws Exception {
        getContactProbeActuator().actuate(on);
    }

    public String getContactProbeActuatorName() {
        return contactProbeActuatorName;
    }

    public void setContactProbeActuatorName(String contactProbeActuatorName) {
        String oldValue = this.contactProbeActuatorName;
        this.contactProbeActuatorName = contactProbeActuatorName;
        if (oldValue != contactProbeActuatorName) {
            firePropertyChange("contactProbeActuatorName", oldValue, contactProbeActuatorName);
        }
    }

    public Length getProbeOffset() {
        return probeOffset;
    }

    public void setProbeOffset(Length probeOffset) {
        Length oldValue = this.probeOffset;
        this.probeOffset = probeOffset;
        if (oldValue != probeOffset) {
            firePropertyChange("probeOffset", oldValue, probeOffset);
        }
    }
}
