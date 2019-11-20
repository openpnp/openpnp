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

    @Override
    public PropertySheet[] getPropertySheets() {
        return Collect.concat(super.getPropertySheets(), new PropertySheet[] {
                new PropertySheetWizardAdapter(new ContactProbeNozzleWizard(this), "Contact Probe") });
    }

    @Override
    public void pick(Part part) throws Exception {
        try {
            Map<String, Object> globals = new HashMap<>();
            globals.put("nozzle", this);
            globals.put("part", part);
            Configuration.get().getScripting().on("Nozzle.BeforePickProbe", globals);
        } catch (Exception e) {
            Logger.warn(e);
        }

        // First probe down from current position above the part until the probe sensor
        // is triggered.
        actuateContactProbe(true);
        // Now call the default pick() which usually just turns on the vacuum.
        super.pick(part);
        // Retract from probing i.e. until the probe sensor is released.
        actuateContactProbe(false);

        try {
            Map<String, Object> globals = new HashMap<>();
            globals.put("nozzle", this);
            globals.put("part", part);
            Configuration.get().getScripting().on("Nozzle.AfterPickProbe", globals);
        } catch (Exception e) {
            Logger.warn(e);
        }
    }

    @Override
    public void place() throws Exception {
        try {
            Map<String, Object> globals = new HashMap<>();
            globals.put("nozzle", this);
            globals.put("part", part);
            Configuration.get().getScripting().on("Nozzle.BeforePlaceProbe", globals);
        } catch (Exception e) {
            Logger.warn(e);
        }

        // First probe down from current position above the PCB until the probe sensor
        // is triggered.
        actuateContactProbe(true);
        // Now call the default place() which usually just turns off the vacuum.
        super.place();
        // Retract from probing i.e. until the probe sensor is released.
        actuateContactProbe(false);

        try {
            Map<String, Object> globals = new HashMap<>();
            globals.put("nozzle", this);
            globals.put("part", part);
            Configuration.get().getScripting().on("Nozzle.AfterPlaceProbe", globals);
        } catch (Exception e) {
            Logger.warn(e);
        }
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
}
