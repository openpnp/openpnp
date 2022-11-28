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

import java.beans.IndexedPropertyChangeEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.pmw.tinylog.Logger;

/**
 * An interface used to indicate a one-way relationship of one object (the definition) to one or 
 * more other objects (the defined objects).  Changes to the definition object should flow to the 
 * defined objects but changes made locally to any of the defined objects should not affect the 
 * definition. Typically, property change listeners are used to update the defined objects when the
 * definition is changed.
 * @param <T> the type of the Definable
 */
public interface Definable<T> extends PropertyChangeListener {
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
    public boolean isDefinition(Object potentialDefinition);
    
    public boolean isDirty();
    
    public void setDirty(boolean dirty);
    
    @Override
    public default void propertyChange(PropertyChangeEvent evt) {
        T definition = getDefinition();
        Logger.trace(String.format("PropertyChangeEvent handled by %s @%08x defined by @%08x = %s", this.getClass().getSimpleName(), this.hashCode(), definition == null ? null : getDefinition().hashCode(), evt));
        if (evt.getSource() != this || !evt.getPropertyName().equals("dirty")) {
            setDirty(true);
            if (evt.getSource() == definition && this != definition) {
                Logger.trace(String.format("Attempting to set %s @%08x property %s = %s", this.getClass().getSimpleName(), this.hashCode(), evt.getPropertyName(), evt.getNewValue()));
                try {
                    if (evt instanceof IndexedPropertyChangeEvent) {
                        PropertyUtils.setIndexedProperty(this, evt.getPropertyName(), 
                            ((IndexedPropertyChangeEvent) evt).getIndex(), evt.getNewValue());
                    }
                    else {
                        BeanUtils.setProperty(this, evt.getPropertyName(), evt.getNewValue());
                    }
                }
                catch (IllegalAccessException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                catch (InvocationTargetException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                catch (NoSuchMethodException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        
    }
    
}
