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

/**
 * An interface used to indicate a one-way relationship of one object (the definition) to one or
 * more other objects (the defined objects). Changes to the definition object flow to the defined
 * objects but changes made locally to any of the defined objects do not affect the definition.
 * 
 * <p>
 * Property setters are used to update the defined objects when the definition is changed, i.e., for
 * a property defined as {@code MyObjectType myObject;}, the class should provide a setter method
 * with a prototype of {@code void setMyObject(MyObjectType newValue)}. This setter should set the
 * property to the new value and then call {@code firePropertyChange("myObject", oldvalue, 
 * newValue);} to notify the property change listeners of the change.
 * 
 * <p>
 * For some reason, Java Beans fails to recognize indexed property setters if they have the same
 * name as the property and will entirely bypass the setter and just modify the element of the List
 * or array directly. To work around this "feature", the index property setters must be named
 * differently than the property. It is suggested that the names of indexed property setters use the
 * singular version of the property name. For instance, suppose the property is defined as
 * {@code List<MyObjectType> myObjects;}. In that case, the class should provide an indexed setter
 * method with a prototype of {@code void setMyObject(int index, MyObjectType newValue)} (Important:
 * note the use of the singular setMyObject rather than the plural setMyObjects). This setter should
 * set the item at the specified index to the new value and then call
 * {@code fireIndexedPropertyChange("myObject", index, oldvalue, newValue);} (again note the use of
 * the singular "myObject" rather than the plural "myObjects") to notify the property change
 * listeners of the change.
 * 
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
    
    /**
     * Checks to see if this object has been modified
     * @return true if the object has been modified
     */
    public boolean isDirty();
    
    /**
     * Sets the state of the flag that indicates this object has been modified
     * @param dirty - the state to set the flag, true meaning the object has been modified
     */
    public void setDirty(boolean dirty);
    
    /**
     * This method is called when bound properties are changed. It updates the properties of
     * all objects that are defined by this object. It also marks this object as being modified
     * whenever one or more of its bound properties change value. 
     */
    @Override
    public default void propertyChange(PropertyChangeEvent evt) {
        T definition = getDefinition();
        if (evt.getSource() != this || !evt.getPropertyName().equals("dirty")) {
            //Set the flag indicating this object has been modified
            setDirty(true);
            
            //Call the setters on the defined objects only when it is the defining object that
            //has changed
            if (evt.getSource() == definition && this != definition) {
                try {
                    if (evt instanceof IndexedPropertyChangeEvent) {
                        //Call the setPropertyName(index, newValue) method on the defined object.
                        PropertyUtils.setIndexedProperty(this, evt.getPropertyName(), 
                            ((IndexedPropertyChangeEvent) evt).getIndex(), evt.getNewValue());
                    }
                    else {
                        //Call the setPropertyName(newValue) method on the defined object.
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
