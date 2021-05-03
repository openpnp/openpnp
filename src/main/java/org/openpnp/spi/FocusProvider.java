/*
 * Copyright (C) 2021 <mark@makr.zone>
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

import org.openpnp.gui.support.Wizard;
import org.openpnp.model.Length;
import org.openpnp.model.Location;

public interface FocusProvider {

    /**
     * Focus the camera to the subject by moving the movable (this might move the subject or the camera).
     *  
     * @param camera
     * @param movable
     * @param subjectMaxSize
     * @param location0
     * @param location1
     * @throws Exception
     */
    Location autoFocus(Camera camera, HeadMountable movable, Length subjectMaxSize, Location location0,
            Location location1) throws Exception;

    Wizard getConfigurationWizard(Camera camera);

}
