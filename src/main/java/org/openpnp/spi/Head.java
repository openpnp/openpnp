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

    public void moveToSafeZ(double speed) throws Exception;

    public void moveToSafeZ() throws Exception;

    public List<PasteDispenser> getPasteDispensers();

    public PasteDispenser getPasteDispenser(String id);

    public Camera getDefaultCamera() throws Exception;

    public Nozzle getDefaultNozzle() throws Exception;

    public PasteDispenser getDefaultPasteDispenser() throws Exception;
    
    public void setMachine(Machine machine);
    
    public Machine getMachine();
}
