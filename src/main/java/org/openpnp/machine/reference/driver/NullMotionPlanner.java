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

import org.openpnp.machine.reference.ReferenceDriver;
import org.openpnp.machine.reference.ReferenceHeadMountable;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.axis.ReferenceControllerAxis;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Configuration;
import org.openpnp.spi.ControllerAxis;
import org.openpnp.spi.Driver;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Axis.Type;
import org.openpnp.spi.Movable.MoveToOption;
import org.openpnp.util.Utils2D;

/**
 * Simplest possible implementation of the motion planner. Just proxies unmodified moveTo()s
 * to the drivers directly. 
 *
 */
public class NullMotionPlanner extends AbstractSimpleMotionPlanner {

    @Override
    public void moveToPlanning(HeadMountable hm, AxesLocation axesLocation, double speed, MoveToOption... options) throws Exception {
        super.moveToPlanning(hm, axesLocation, speed, options);

        // The null motion planner is a pure proxy, so talk to the driver(s) immediately.
        ReferenceMachine machine = (ReferenceMachine) Configuration.get().getMachine();
        boolean moved = false;
        for (Driver driver : axesLocation.getAxesDrivers(machine)) {
            ((ReferenceDriver) driver).moveTo((ReferenceHeadMountable) hm, axesLocation, speed, options);
            moved = true;
        }
        if (moved) {
            machine.fireMachineHeadActivity(hm.getHead());
        }
    }

    @Override
    public void waitForCompletion(HeadMountable hm, CompletionType completionType) throws Exception {
        // The null motion planner is a pure proxy, so there is nothing left to do except wait for the driver(s).
        ReferenceMachine machine = (ReferenceMachine) Configuration.get().getMachine();
        AxesLocation mappedAxes = hm.getMappedAxes(machine);
        if (!mappedAxes.isEmpty()) {
            for (Driver driver : mappedAxes.getAxesDrivers(machine)) {
                ((ReferenceDriver) driver).waitForCompletion((ReferenceHeadMountable) hm, completionType);
            }
            machine.fireMachineHeadActivity(hm.getHead());
        }
        // Now the physical completion is done, go do the abstract stuff.
        super.waitForCompletion(hm, completionType);
    }
}
