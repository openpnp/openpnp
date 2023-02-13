/*
 * Copyright (C) 2022 Jason von Nieda <jason@vonnieda.org>, Tony Luken <tonyluken62+openpnp@gmail.com>
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

package org.openpnp.events;

import org.openpnp.spi.Definable;

public class DefinitionStructureChangedEvent {
    final public Definable<?> definition;
    final public String changedName;
    final public Object source;
    
    public DefinitionStructureChangedEvent(Definable<?> definition, String changedName, Object source) {
        this.definition = definition;
        this.changedName = changedName;
        this.source = source;
    }
    
    public String toString() {
        return "definition: " + definition + " changedItem: " + changedName + " source: "+ source;
    }
}
