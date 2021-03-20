/*
 * Copyright (C) 2020 <mark@makr.zone>
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

package org.openpnp.machine.reference;

import java.util.LinkedHashMap;
import java.util.Map;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.wizards.ReferenceActuatorMapperConfigurationWizard;
import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.simpleframework.xml.ElementMap;

public class ReferenceActuatorMapper extends ReferenceActuator {

    @ElementMap(entry = "map", key = "key", value = "value", inline = true, required = false)
    protected Map<Object, Mapping> map = new LinkedHashMap<Object, Mapping>();

    public ReferenceActuatorMapper() {
    }

    public Map<Object, Mapping> getMap() {
        return map;
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceActuatorMapperConfigurationWizard(getMachine(), this);
    }

    static public class Mapping extends AbstractModelObject {

        @ElementMap(entry = "actuator", key = "name", value = "value", attribute = true, inline = true)
        protected Map<String, Object> actuators = new LinkedHashMap<String, Object>();

        private ReferenceActuatorMapper container;

        public ReferenceActuatorMapper getContainer() {
            return container;
        }
        public void setContainer(ReferenceActuatorMapper container) {
            this.container = container;
        }

        public static Actuator getActuatorByName(String name) {
            Machine machine = Configuration.get().getMachine();
            Actuator actuator = machine.getActuatorByName(name);
            if (actuator != null) {
                return actuator;
            }
            for (Head head : machine.getHeads()) {
                actuator = head.getActuatorByName(name);
                if(actuator != null) {
                    return actuator;
                }
            }
            throw new IllegalArgumentException("Invalid actuator: " + name);
        }

        public Map<String, Object> getActuators() {
            return actuators;
        }

        public void actuate() throws Exception {
            for (Map.Entry<String, Object> entry : actuators.entrySet()) {
                Actuator actuator = getActuatorByName(entry.getKey());
                actuator.actuate(entry.getValue());
            }
        }
    }

    @Override
    protected void driveActuation(Object value) throws Exception {
        Mapping mapping = map.get(value);
        if (mapping == null) {
            throw new IllegalArgumentException("Invalid value: " + value);
        }
        mapping.actuate();
    }
}
