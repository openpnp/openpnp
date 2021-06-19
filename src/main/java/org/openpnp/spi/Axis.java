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

package org.openpnp.spi;

import org.openpnp.model.Identifiable;
import org.openpnp.model.Named;
import org.openpnp.model.Solutions;

/**
 * An Axis can be any coordinate dimension, either a raw machine controller axis or 
 * a Cartesian linear coordinate dimension or a rotary coordinate dimension to be transformed 
 * into a raw axis. Furthermore there are virtual axes to store coordinate states. 
 *  
 */
public interface Axis extends Identifiable, Named, WizardConfigurable, PropertySheetHolder, Solutions.Subject {
    public enum Type {
        X,
        Y,
        Z,
        Rotation;
        
        public String getDefaultLetter() {
            if (this == Rotation) {
                return "C";
            }
            return this.toString();
        }
    }

    public Type getType();

    public void setType(Type type);
}
