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

import java.awt.Color;
import java.util.Arrays;
import java.util.List;

import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.driver.wizards.ReferenceAdvancedMotionPlannerConfigurationWizard;
import org.openpnp.machine.reference.driver.wizards.ReferenceAdvancedMotionPlannerDiagnosticsWizard;
import org.openpnp.model.AbstractMotionPath;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Motion;
import org.openpnp.model.Motion.MotionOption;
import org.openpnp.model.Motion.MoveToCommand;
import org.openpnp.model.MotionProfile;
import org.openpnp.spi.Axis;
import org.openpnp.spi.ControllerAxis;
import org.openpnp.spi.Driver;
import org.openpnp.spi.HeadMountable;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.NanosecondTime;
import org.openpnp.util.SimpleGraph;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

/**
 * The Advanced Motion Planner applies any optimizing to the planned path. 
 *
 */
public class ReferenceAdvancedMotionPlanner extends AbstractMotionPlanner {

    @Attribute(required = false)
    private boolean allowContinuousMotion = false;
    @Attribute(required = false)
    private boolean allowUncoordinated = false;
    @Attribute(required = false)
    private boolean diagnosticsEnabled = false;
    @Attribute(required = false)
    private boolean interpolationRetiming = true;
    
    @Attribute(required = false)
    private boolean showApproximation = true;
    
    @Element(required = false)
    Location startLocation = new Location(LengthUnit.Millimeters);
    @Element(required = false)
    Location midLocation1 = new Location(LengthUnit.Millimeters);
    @Element(required = false)
    Location midLocation2 = new Location(LengthUnit.Millimeters);
    @Element(required = false)
    Location endLocation = new Location(LengthUnit.Millimeters);

    @Attribute(required = false)
    double toMid1Speed = 1.0;
    @Attribute(required = false)
    double toMid2Speed = 1.0;
    @Attribute(required = false)
    double toEndSpeed = 1.0;

    // Transient data
    protected SimpleGraph motionGraph = null;
    protected SimpleGraph recordingMotionGraph = null;
    private double recordingT0;
    private double recordingT;
    private Double moveTimePlanned;
    private Double moveTimeActual;
    private Double recordingMoveTimePlanned;
    private boolean interpolationFailed;
    private boolean recordingInterpolationFailed;
    private AxesLocation recordingLocation0;

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

    @Override
    public boolean isInterpolationRetiming() {
        return interpolationRetiming;
    }

    public void setInterpolationRetiming(boolean interpolationRetiming) {
        this.interpolationRetiming = interpolationRetiming;
    }

    public boolean isDiagnosticsEnabled() {
        return diagnosticsEnabled;
    }

    public void setDiagnosticsEnabled(boolean diagnosticsEnabled) {
        this.diagnosticsEnabled = diagnosticsEnabled;
    }

    public SimpleGraph getMotionGraph() {
        return motionGraph;
    }

    public void setMotionGraph(SimpleGraph motionGraph) {
        Object oldValue = this.motionGraph;
        this.motionGraph = motionGraph;
        firePropertyChange("motionGraph", oldValue, motionGraph);
    }

    public Location getStartLocation() {
        return startLocation;
    }

    public void setStartLocation(Location startLocation) {
        this.startLocation = startLocation;
    }

    public Location getMidLocation1() {
        return midLocation1;
    }

    public void setMidLocation1(Location midLocation1) {
        this.midLocation1 = midLocation1;
    }

    public Location getMidLocation2() {
        return midLocation2;
    }

    public void setMidLocation2(Location midLocation2) {
        this.midLocation2 = midLocation2;
    }

    public Location getEndLocation() {
        return endLocation;
    }

    public void setEndLocation(Location endLocation) {
        this.endLocation = endLocation;
    }

    public double getToMid1Speed() {
        return toMid1Speed;
    }

    public void setToMid1Speed(double toMid1Speed) {
        this.toMid1Speed = toMid1Speed;
    }

    public double getToMid2Speed() {
        return toMid2Speed;
    }

    public void setToMid2Speed(double toMid2Speed) {
        this.toMid2Speed = toMid2Speed;
    }

    public double getToEndSpeed() {
        return toEndSpeed;
    }

    public void setToEndSpeed(double toEndSpeed) {
        this.toEndSpeed = toEndSpeed;
    }

    public Double getMoveTimePlanned() {
        return moveTimePlanned;
    }

    public void setMoveTimePlanned(Double moveTimePlanned) {
        Object oldValue = this.moveTimePlanned;
        this.moveTimePlanned = moveTimePlanned;
        firePropertyChange("moveTimePlanned", oldValue, moveTimePlanned);
    }

    public Double getMoveTimeActual() {
        return moveTimeActual;
    }

    public void setMoveTimeActual(Double moveTimeActual) {
        Object oldValue = this.moveTimeActual;
        this.moveTimeActual = moveTimeActual;
        firePropertyChange("moveTimeActual", oldValue, moveTimeActual);
    }

    public boolean isInterpolationFailed() {
        return interpolationFailed;
    }

    public void setInterpolationFailed(boolean interpolationFailed) {
        Object oldValue = this.interpolationFailed;
        this.interpolationFailed = interpolationFailed;
        firePropertyChange("interpolationFailed", oldValue, interpolationFailed);
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
                        //| MotionOption.SynchronizeEarlyBird.flag()
                        //| MotionOption.SynchronizeLastMinute.flag()
                        ;
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

    protected void startNewMotionGraph() {
        // start a new graph 
        SimpleGraph motionGraph = new SimpleGraph();
        motionGraph.setRelativePaddingLeft(0.05);
        for (Axis axis : getMachine().getAxes()) {
            if (axis instanceof ControllerAxis) {
                final int alphaBlend = 0x70;
                // The s Data scale is visible and is the main scale. 

                // Reverse order for better drawing order.
                SimpleGraph.DataScale jScale =  motionGraph.getScale(axis.getName()+" j");
                SimpleGraph.DataRow jRow = motionGraph.getRow(axis.getName()+" j", "j");
                jRow.setColor(new Color(0xDD, 0xBB, 0x00)); 

                SimpleGraph.DataScale aScale =  motionGraph.getScale(axis.getName()+" a");
                SimpleGraph.DataRow aRow = motionGraph.getRow(axis.getName()+" a", "a");
                aRow.setColor(new Color(0xFF, 0x00, 0x00)); 
                aRow = motionGraph.getRow(axis.getName()+" a", "a'");
                aRow.setColor(new Color(0xFF, 0x00, 0x00, alphaBlend)); 

                SimpleGraph.DataScale vScale =  motionGraph.getScale(axis.getName()+" V");
                SimpleGraph.DataRow vRow = motionGraph.getRow(axis.getName()+" V", "V");
                vRow.setColor(new Color(00, 0x5B, 0xD9)); // the OpenPNP blue
                vRow = motionGraph.getRow(axis.getName()+" V", "V'");
                vRow.setColor(new Color(00, 0x5B, 0xD9, alphaBlend)); 

                SimpleGraph.DataScale sScale =  motionGraph.getScale(axis.getName());
                sScale.setColor(new Color(0, 0, 0, 64));
                sScale.setLabelShown(true);

                SimpleGraph.DataRow sRow = motionGraph.getRow(axis.getName(), "s");
                sRow.setColor(new Color(00, 0x77, 0x00)); 
                sRow = motionGraph.getRow(axis.getName(), "s'");
                sRow.setColor(new Color(00, 0x77, 0x00, alphaBlend));
            }
        }
        motionGraph.getT();
        recordingMotionGraph = motionGraph;

    }

    protected boolean rearrangeGraph() {
        if (recordingMotionGraph == null) {
            return !diagnosticsEnabled;
        }
        int count = 0;
        for (Axis axis : getMachine().getAxes()) {
            if (axis instanceof ControllerAxis) {
                SimpleGraph.DataScale sScale = recordingMotionGraph.getScale(axis.getName());
                if (sScale.getMaximum() != null) {
                    count++;
                }
            }
        }
        int n = 0;
        for (Axis axis : getMachine().getAxes()) {
            if (axis instanceof ControllerAxis) {
                SimpleGraph.DataScale sScale = recordingMotionGraph.getScale(axis.getName());
                if (sScale.getMaximum() != null) {
                    SimpleGraph.DataScale [] scales =  new SimpleGraph.DataScale [] {
                            sScale,
                            recordingMotionGraph.getScale(axis.getName()+" V"),
                            recordingMotionGraph.getScale(axis.getName()+" a"),
                            recordingMotionGraph.getScale(axis.getName()+" j"),
                    };
                    int i = 0;
                    for (SimpleGraph.DataScale scale : scales) {
                        if (i == 3) {
                            // Jerk
                            scale.setRelativePaddingTop(-0.15+((double)n + 1)/count);
                            scale.setRelativePaddingBottom(0.05+((double)count - n - 1)/count);
                        }
                        else {
                            scale.setRelativePaddingTop(0.05+((double)n)/count);
                            scale.setRelativePaddingBottom(0.05+((double)count - n - 1)/count);
                        }
                        i++;
                    }
                    n++;
                }
            }
        }
        return count > 0;
    }

    @Override
    protected void recordDiagnostics(Motion plannedMotion, MoveToCommand moveToCommand, Driver driver) {
        super.recordDiagnostics(plannedMotion, moveToCommand, driver);
        if (diagnosticsEnabled) {
            final double tick = 1e-8;
            final double dt = Math.min(0.001, plannedMotion.getTime()/1000 + 1e-6); // 1ms or 1/1000 of whole motion 
            if (recordingMotionGraph == null) {
                startNewMotionGraph();
                recordingT0 = plannedMotion.getPlannedTime0();
                recordingT = 0;
                recordingInterpolationFailed = false;
                recordingLocation0 = plannedMotion.getLocation0();
            }
            AxesLocation segment = moveToCommand.getLocation0().motionSegmentTo(moveToCommand.getMovedAxesLocation());

            // Calculate the factor from this single lead Axis to the rate along the relevant axes 
            // (either linear or rotational axes, according to RS274NGC).  
            double distance = segment.getRS274NGCMetric(driver, 
                    (axis) -> segment.getCoordinate(axis));
            double factorRS274NGC = (distance != 0 ? 1/distance : 1);

            Double timeStart = moveToCommand.getTimeStart();
            Double d = moveToCommand.getTimeDuration(); 
            AxesLocation recordingLocation1 = recordingLocation0.put(moveToCommand.getMovedAxesLocation());
            for (ControllerAxis axis : plannedMotion.getLocation1().getAxes(driver)) {
                MotionProfile profile = plannedMotion.getAxesProfiles()[plannedMotion.getAxisIndex(axis)];
                if (recordingMotionGraph.getRow(axis.getName(), "s'").size() > 0 
                        || ! profile.isEmpty()) {
                    double t = recordingT;
                    if (showApproximation ) {
                        SimpleGraph.DataRow sRow = recordingMotionGraph.getRow(axis.getName(), "s'");
                        SimpleGraph.DataRow vRow = recordingMotionGraph.getRow(axis.getName()+" V", "V'");
                        SimpleGraph.DataRow aRow = recordingMotionGraph.getRow(axis.getName()+" a", "a'");
                        if (d != null) {
                            d -= tick;// subtract one tick to make it unique.
                            Double v = moveToCommand.getFeedRatePerSecond();
                            Double v0 = moveToCommand.getV0();
                            Double v1 = moveToCommand.getV1();
                            Double a = moveToCommand.getAccelerationPerSecond2();
                            if (v0 != null && v1 != null && a != null) {
                                // Approximated constant acceleration move. Reconstruct Profile.
                                double s0 = recordingLocation0.getCoordinate(axis);
                                double s1 = recordingLocation1.getCoordinate(axis);
                                double factor = (s1 - s0)*factorRS274NGC;
                                v0 *= factor;
                                v1 *= factor;
                                a *= factor;
                                if (t == 0) {
                                    sRow.recordDataPoint(t, s0);
                                }
                                sRow.recordDataPoint(t+d, s1);
                                if (v != null && plannedMotion.getTime() == moveToCommand.getTimeDuration()) { 
                                    // Single trapezoidal move
                                    v *= factor;
                                    double t0 = (v-v0)/a;
                                    double t1 = (v-v1)/a;
                                    double tMid = d-t0-t1;
                                    if (tMid < tick) {
                                        if (t0 > t1) {
                                            t0 = d-t1-tick;
                                        } else {
                                            t1 = d-t0-tick;
                                        }
                                    }
                                    if ((t0 > 2*tick || t1 > 2*tick) && tMid > -tick*2) {
                                        // Valid Trapezoidal.
                                        vRow.recordDataPoint(t+t0, v);
                                        vRow.recordDataPoint(t+d-t1, v);
                                        if (t0 > tick*2) {
                                            aRow.recordDataPoint(t, a);
                                            aRow.recordDataPoint(t+t0-tick, a);
                                        }
                                        if (tMid > tick*4) {
                                            aRow.recordDataPoint(t+t0, 0);
                                            aRow.recordDataPoint(t+d-t1, 0);
                                        }
                                        if (t1 > tick*2) {
                                            aRow.recordDataPoint(t+d-t1+tick, -a);
                                            aRow.recordDataPoint(t+d, -a);
                                        }
                                        // s Forward
                                        for (double ts = dt; ts <= t0; ts += dt) {
                                            double sm = s0 + v0*ts + 1./2*a*Math.pow(ts, 2);
                                            sRow.recordDataPoint(t+ts, sm);
                                        }
                                        // s Backward
                                        for (double ts = dt; ts <= t1; ts += dt) {
                                            double sm = s1 - v1*ts - 1./2*a*Math.pow(ts, 2);
                                            sRow.recordDataPoint(t+d-ts, sm);
                                        }
                                    }
                                }
                                else if (d > 0) {
                                    // Approximation ramp (always one-sided).
                                    // Acceleration is deduced from v0, v1 rather than from the given acceleration (which is never smaller than the minimum)
                                    a = (v1 - v0)/d;
                                    // s Ramp
                                    for (double ts = dt; ts < d; ts += dt) {
                                        double sm = s0 + v0*ts + 1./2*a*Math.pow(ts, 2);
                                        sRow.recordDataPoint(t+ts, sm);
                                    }
                                    aRow.recordDataPoint(t, a);
                                    aRow.recordDataPoint(t+d, a);
                                }
                                else {
                                    aRow.recordDataPoint(t, 0);
                                    aRow.recordDataPoint(t+d, 0);
                                }
                                vRow.recordDataPoint(t, v0);
                                vRow.recordDataPoint(t+d, v1);
                            }
                        }
                        else if (timeStart == 0 && driver.getMotionControlType().isUnpredictable()) {
                            // No approximation possible due to driver setting. Just connect s and show limits to illustrate.
                            d = plannedMotion.getTime()-tick;
                            sRow.recordDataPoint(t, profile.getMomentaryLocation(0));
                            sRow.recordDataPoint(t+d, profile.getMomentaryLocation(d));
                            if (vRow.size() == 0) {
                                vRow.recordDataPoint(t+d*0.45, profile.getVelocityMax());
                                vRow.recordDataPoint(t+d*0.55, profile.getVelocityMax());
                            }
                            if (! profile.isConstantAcceleration()) {
                                if (aRow.size() == 0) {
                                    aRow.recordDataPoint(t+d*0.2, profile.getAccelerationMax());
                                    aRow.recordDataPoint(t+d*0.3, profile.getAccelerationMax());
                                }
                            }
                        }
                    }

                    if (timeStart == 0) {
                        // First interpolation command: Show the true 3rd order control motion.
                        double tm = plannedMotion.getPlannedTime0() - recordingT0;
                        SimpleGraph.DataRow sRow = recordingMotionGraph.getRow(axis.getName(), "s");
                        SimpleGraph.DataRow vRow = recordingMotionGraph.getRow(axis.getName()+" V", "V");
                        SimpleGraph.DataRow aRow = recordingMotionGraph.getRow(axis.getName()+" a", "a");
                        SimpleGraph.DataRow jRow = null;
                        if (!profile.isConstantAcceleration()) {
                            jRow = recordingMotionGraph.getRow(axis.getName()+" j", "j");
                        }
                        double dm = plannedMotion.getTime();
                        for (double ts = 0; ts <= dm; ts += dt) {
                            double s = profile.getMomentaryLocation(ts);
                            double v = profile.getMomentaryVelocity(ts);
                            double a = profile.getMomentaryAcceleration(ts);
                            sRow.recordDataPoint(tm + ts, s);
                            vRow.recordDataPoint(tm + ts, v);
                            aRow.recordDataPoint(tm + ts, a);
                            if (jRow != null) {
                                double j = profile.getMomentaryJerk(ts);
                                jRow.recordDataPoint(tm + ts, j);
                            }
                        }
                        recordingMoveTimePlanned = tm + dm;
                        if (plannedMotion.hasOption(MotionOption.InterpolationFailed)) {
                            recordingInterpolationFailed = true;
                        }
                    }
                }
            }
            if (moveToCommand.getTimeDuration() != null) {
                recordingT += moveToCommand.getTimeDuration();
            }
            recordingLocation0 = recordingLocation1; 
        }
    }

    @Override
    protected void publishDiagnostics() {
        super.publishDiagnostics();
        if (rearrangeGraph()) {
            setMoveTimePlanned(recordingMoveTimePlanned);
            setMoveTimeActual(null);
            setInterpolationFailed(recordingInterpolationFailed);
            setMotionGraph(recordingMotionGraph);
            recordingMotionGraph = null;
            recordingMoveTimePlanned = null;
            recordingInterpolationFailed = false;
        }
    }

    public void testMotion(HeadMountable tool, boolean reverse) throws Exception {
        Location l = tool.getLocation().convertToUnits(LengthUnit.Millimeters);
        double speed = getMachine().getSpeed();
        if (reverse) {
            if (l.getLinearDistanceTo(endLocation) > 4) {
                MovableUtils.moveToLocationAtSafeZ(tool, endLocation);
            }
            tool.waitForCompletion(CompletionType.WaitForStillstand);
            double t0 = NanosecondTime.getRuntimeSeconds();
            tool.moveTo(midLocation2, toEndSpeed*speed);
            tool.moveTo(midLocation1, toMid2Speed*speed);
            tool.moveTo(startLocation, toMid1Speed*speed);
            tool.waitForCompletion(CompletionType.WaitForStillstand);
            setMoveTimeActual(NanosecondTime.getRuntimeSeconds() - t0);
        }
        else {
            if (l.getLinearDistanceTo(startLocation) > 4) {
                MovableUtils.moveToLocationAtSafeZ(tool, startLocation);
            }
            tool.waitForCompletion(CompletionType.WaitForStillstand);
            double t0 = NanosecondTime.getRuntimeSeconds();
            tool.moveTo(midLocation1, toMid1Speed*speed);
            tool.moveTo(midLocation2, toMid2Speed*speed);
            tool.moveTo(endLocation, toEndSpeed*speed);
            tool.waitForCompletion(CompletionType.WaitForStillstand);
            setMoveTimeActual(NanosecondTime.getRuntimeSeconds() - t0);
        }
        //tool.moveToSafeZ();
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceAdvancedMotionPlannerConfigurationWizard(this);
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {
                new PropertySheetWizardAdapter(getConfigurationWizard(), "Motion Planner"),
                new PropertySheetWizardAdapter(new ReferenceAdvancedMotionPlannerDiagnosticsWizard(this), "Motion Planner Diagnostics"),
                };
    }
}
