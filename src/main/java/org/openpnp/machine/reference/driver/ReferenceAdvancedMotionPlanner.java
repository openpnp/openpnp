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
import org.openpnp.machine.reference.axis.ReferenceControllerAxis;
import org.openpnp.machine.reference.wizards.ReferenceAdvancedMotionPlannerConfigurationWizard;
import org.openpnp.model.AbstractMotionPath;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Motion;
import org.openpnp.model.MotionProfile;
import org.openpnp.model.Motion.MotionOption;
import org.openpnp.model.MotionProfile.ProfileOption;
import org.openpnp.spi.ControllerAxis;
import org.openpnp.spi.HeadMountable;
import org.simpleframework.xml.Attribute;

/**
 * The Advanced Motion Planner applies optimizing planning to the path. TODO: doc.
 *
 */
public class ReferenceAdvancedMotionPlanner extends AbstractMotionPlanner {

    @Attribute(required = false)
    private boolean allowContinuousMotion = false;
    @Attribute(required = false)
    private boolean allowUncoordinated;

    public boolean isAllowContinuousMotion() {
        return allowContinuousMotion;
    }

    public void setAllowContinuousMotion(boolean allowContinuousMotion) {
        this.allowContinuousMotion = allowContinuousMotion;
    }

    public boolean isAllowUncoordinated() {
        return allowUncoordinated;
    }

    public void setAllowUncoordinated(boolean allowUncoordinated) {
        this.allowUncoordinated = allowUncoordinated;
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
    protected Motion addMotion(HeadMountable hm, double speed, AxesLocation location0,
            AxesLocation location1, int options) {
        if (allowUncoordinated) {
            if (location0.isInSafeZone()
                    && location1.isInSafeZone()) {
                // Both locations are in the Save Zone. Add the uncoordinated flags.
                options |= MotionOption.UncoordinatedMotion.flag()
                        | MotionOption.LimitToSafeZone.flag()
                        | MotionOption.SynchronizeStraighten.flag()
                        | MotionOption.SynchronizeEarlyBird.flag()
                        | MotionOption.SynchronizeLastMinute.flag();
            }
        }
        return super.addMotion(hm, speed, location0, location1, options);
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
