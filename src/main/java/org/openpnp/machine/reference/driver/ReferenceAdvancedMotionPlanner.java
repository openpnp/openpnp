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
import org.openpnp.machine.reference.wizards.ReferenceAdvancedMotionPlannerConfigurationWizard;
import org.openpnp.model.AbstractMotionPath;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Motion;
import org.openpnp.model.MotionProfile;
import org.openpnp.model.Motion.MotionOption;
import org.openpnp.spi.HeadMountable;
import org.simpleframework.xml.Attribute;

/**
 * Advanced Motion Planner. TODO: 
 *
 */
public class ReferenceAdvancedMotionPlanner extends AbstractMotionPlanner {

    @Attribute(required = false)
    private boolean allowContinuousMotion = false;

    public boolean isAllowContinuousMotion() {
        return allowContinuousMotion;
    }

    public void setAllowContinuousMotion(boolean allowContinuousMotion) {
        this.allowContinuousMotion = allowContinuousMotion;
    }

    protected class PlannerPath extends AbstractMotionPath {
        private final List<Motion> executionPlan;

        public PlannerPath(List<Motion> executionPlan) {
            super();
            this.executionPlan = executionPlan;
        }

        @Override
        public int size() {
            return executionPlan.size();
        }

        @Override
        public MotionProfile[] get(int i) {
            return executionPlan.get(i).getAxesProfiles();
        }
    }

    @Override
    protected void optimizeExecutionPlan(List<Motion> executionPlan,
            CompletionType completionType) throws Exception {
        PlannerPath path = new PlannerPath(executionPlan);
        path.solve();
    }

    @Override
    public void moveTo(HeadMountable hm, AxesLocation axesLocation, double speed,
            MotionOption... options) throws Exception {
        super.moveTo(hm, axesLocation, speed, options);

        if (!allowContinuousMotion) {
            getMachine().getMotionPlanner().waitForCompletion(hm, 
                    Arrays.asList(options).contains(MotionOption.JogMotion) ? 
                            CompletionType.CommandJog 
                            : CompletionType.WaitForStillstand);
        }
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceAdvancedMotionPlannerConfigurationWizard(this);
    }
}
