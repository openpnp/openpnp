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

import java.util.Arrays;
import java.util.List;

import org.openpnp.gui.support.Wizard;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Motion;
import org.openpnp.model.Motion.MotionOption;
import org.openpnp.spi.HeadMountable;

/**
 * Simplest possible implementation of the motion planner. Just executes the unmodified motion commands 1:1. 
 *
 */
public class NullMotionPlanner extends AbstractMotionPlanner {

    @Override
    protected void optimizeExecutionPlan(List<Motion> executionPlan,
            CompletionType completionType) {
        // The NullMotionPLanner does nothing to the motion execution plan.
    }

    @Override
    public void moveTo(HeadMountable hm, AxesLocation axesLocation, double speed,
            MotionOption... options) throws Exception {
        super.moveTo(hm, axesLocation, speed, options);
        // The NullMotionPLanner executes moves immediately, one by one.
        getMachine().getMotionPlanner().waitForCompletion(hm, 
                Arrays.asList(options).contains(MotionOption.JogMotion) ? 
                        CompletionType.CommandJog 
                        : CompletionType.WaitForStillstand);
    }

    @Override
    public Wizard getConfigurationWizard() {
        return null;
    }
}