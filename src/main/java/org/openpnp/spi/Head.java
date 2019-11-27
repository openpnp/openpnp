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

package org.openpnp.spi;

import java.util.List;

import org.openpnp.model.Identifiable;
import org.openpnp.model.Location;
import org.openpnp.model.Named;


/**
 * A Head is a movable group of components attached to a Machine. Components which can be attached
 * consist of Nozzles, Actuators and Cameras. A Head itself is not directly movable, but can be
 * moved by moving any one of it's components. When any attached component is moved in (at least) X
 * or Y, it is expected that all components attached to the Head also move in the same axes.
 */
public interface Head extends Identifiable, Named, WizardConfigurable, PropertySheetHolder {
    /**
     * Get a list of Nozzles that are attached to this head.
     * 
     * @return
     */
    public List<Nozzle> getNozzles();

    /**
     * Get the Nozzle attached to this Head that has the specified id.
     * 
     * @param id
     * @return
     */
    public Nozzle getNozzle(String id);

    /**
     * Get a list of Actuators that are attached to this Head.
     * 
     * @return
     */
    public List<Actuator> getActuators();

    /**
     * Get the Actuator attached to this Head that has the specified id.
     * 
     * @param id
     * @return
     */
    public Actuator getActuator(String id);

    /**
     * Get the Actuator attached to this Head that has the specified name.
     * Returns null if the name is null or empty.
     * 
     * @param id
     * @return
     */
    public Actuator getActuatorByName(String name);

    /**
     * Get a list of Cameras that are attached to this Head.
     * 
     * @return
     */
    public List<Camera> getCameras();

    /**
     * Get the Camera attached to this Head that has the specified id.
     * 
     * @param id
     * @return
     */
    public Camera getCamera(String id);

    /**
     * Directs the Head to move to it's home position and to move any attached devices to their home
     * positions.
     */
    void home() throws Exception;

    public void addCamera(Camera camera) throws Exception;

    public void removeCamera(Camera camera);

    public void addNozzle(Nozzle nozzle) throws Exception;

    public void removeNozzle(Nozzle nozzle);

    public void addActuator(Actuator actuator) throws Exception;

    public void removeActuator(Actuator actuator);

    public void moveToSafeZ(double speed) throws Exception;

    public void moveToSafeZ() throws Exception;

    public Camera getDefaultCamera() throws Exception;

    public Nozzle getDefaultNozzle() throws Exception;

    public void setMachine(Machine machine);
    
    public Machine getMachine();
    
    public Location getParkLocation();

    /**
     * Returns true if any nozzle on the Head is currently carrying a part.
     * @return
     */
    public boolean isCarryingPart(); 

    /**
     * Returns the maximum speed percentage allowed by any parts being carried
     * by the Nozzles on the Head. The slowest part will dictate the max.
     * @return
     */
	public double getMaxPartSpeed(); 
	
    public Actuator getZProbe(); 
    
    public Actuator getPump(); 

    /**
     * Returns true if the given HeadMountable can go to the specified location within soft-limits.
     * @param hm
     * @param location
     * @return
     */
    public boolean isInsideSoftLimits(HeadMountable hm, Location location) throws Exception;
}
