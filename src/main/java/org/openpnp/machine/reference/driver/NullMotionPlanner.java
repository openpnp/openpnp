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

package org.openpnp.machine.reference.driver;

import org.openpnp.model.AxesLocation;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Movable.MoveToOption;

/**
 * Simplest possible implementation of the motion planner. Just sends unmodified moveTo()s
 * to the drivers. 
 *
 */
public class NullMotionPlanner extends AbstractMotionPlanner {

    @Override
    public void waitForCompletion(HeadMountable hm, CompletionType completionType) throws Exception {
        // The motion plan is in no way refined in the NullMotionPlanner. Execute the moves 1:1. 
        executeMotionPlan(completionType);
        // Wait for drivers.
        waitForDriverCompletion(hm, completionType);
        // Now the physical completion is done, do the abstract stuff.
        super.waitForCompletion(hm, completionType);
    }
}