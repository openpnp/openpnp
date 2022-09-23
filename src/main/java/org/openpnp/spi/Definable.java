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

package org.openpnp.spi;

/**
 * An interface used to indicate a one-way relationship of one object (the definition) to one or 
 * more other objects (the defined objects).  Changes to the definition object should flow to the 
 * defined objects but changes made locally to any of the defined objects should not affect the 
 * definition. Typically, property change listeners are used to update the defined objects when the
 * definition is changed.
 * @param <T> the type of the Definable
 */
public interface Definable<T> {
    /**
     * 
     * @return the defining object
     */
    public T getDefinition();
    
    /**
     * Sets the defining object
     * @param definable - the definition
     */
    public void setDefinition(T definable);
    
    /**
     * Tests to see if this object is defined by another
     * @param potentialDefinition - the other object 
     * @return
     */
    public boolean isDefinedBy(Object potentialDefinition);
}
