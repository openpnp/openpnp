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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.UIManager;

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
import org.openpnp.model.Solutions;
import org.openpnp.model.Solutions.Severity;
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
    protected double minimumSpeed = 0.05;

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

    @Attribute(required = false)
    private boolean startLocationEnabled = false;
    @Attribute(required = false)
    private boolean mid1LocationEnabled = false;
    @Attribute(required = false)
    private boolean mid2LocationEnabled = false;
    @Attribute(required = false)
    private boolean endLocationEnabled = false;

    @Attribute(required = false)
    private boolean toMid1SafeZ = true;
    @Attribute(required = false)
    private boolean toMid2SafeZ = true;
    @Attribute(required = false)
    private boolean toEndSafeZ = true;


    // Transient data
    protected SimpleGraph motionGraph = null;
    protected SimpleGraph recordingMotionGraph = null;
    private Double recordingT0;
    private Map<Driver, Double> recordingT = new HashMap<>();
    private AxesLocation recordingLocation0 = AxesLocation.zero;

    private Double moveTimePlanned;
    private Double moveTimeActual;
    private Double recordingMoveTimePlanned;
    private boolean interpolationFailed;
    private boolean recordingInterpolationFailed;
    private boolean recordingMotionLocked;

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

    @Override
    public double getMinimumSpeed() {
        return minimumSpeed;
    }

    public void setMinimumSpeed(double minimumSpeed) {
        this.minimumSpeed = minimumSpeed;
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

    public boolean isStartLocationEnabled() {
        return startLocationEnabled;
    }

    public void setStartLocationEnabled(boolean startLocationEnabled) {
        this.startLocationEnabled = startLocationEnabled;
    }

    public boolean isMid1LocationEnabled() {
        return mid1LocationEnabled;
    }

    public void setMid1LocationEnabled(boolean mid1LocationEnabled) {
        this.mid1LocationEnabled = mid1LocationEnabled;
    }

    public boolean isMid2LocationEnabled() {
        return mid2LocationEnabled;
    }

    public void setMid2LocationEnabled(boolean mid2LocationEnabled) {
        this.mid2LocationEnabled = mid2LocationEnabled;
    }

    public boolean isEndLocationEnabled() {
        return endLocationEnabled;
    }

    public void setEndLocationEnabled(boolean endLocationEnabled) {
        this.endLocationEnabled = endLocationEnabled;
    }

    public boolean isToMid1SafeZ() {
        return toMid1SafeZ;
    }

    public void setToMid1SafeZ(boolean toMid1SafeZ) {
        this.toMid1SafeZ = toMid1SafeZ;
    }

    public boolean isToMid2SafeZ() {
        return toMid2SafeZ;
    }

    public void setToMid2SafeZ(boolean toMid2SafeZ) {
        this.toMid2SafeZ = toMid2SafeZ;
    }

    public boolean isToEndSafeZ() {
        return toEndSafeZ;
    }

    public void setToEndSafeZ(boolean toEndSafeZ) {
        this.toEndSafeZ = toEndSafeZ;
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

    private void setRecordingMotionLocked(boolean recordingMotionLocked) {
        this.recordingMotionLocked = recordingMotionLocked;
        if (!recordingMotionLocked) {
            publishDiagnostics();
        }
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
                // Both locations are in the Safe Zone. Add the uncoordinated flags.
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
        Color gridColor = UIManager.getColor ( "PasswordField.capsLockIconColor" );
        if (gridColor == null) {
            gridColor = new Color(0, 0, 0, 64);
        } else {
            gridColor = new Color(gridColor.getRed(), gridColor.getGreen(), gridColor.getBlue(), 64);
        }
        // start a new graph
        SimpleGraph motionGraph = new SimpleGraph();
        motionGraph.setRelativePaddingLeft(0.05);
        for (Axis axis : getMachine().getAxes()) {
            if (axis instanceof ControllerAxis) {
                final int alphaBlend = 0x70;
                // The s Data scale is visible and is the main scale. 

                // Reverse order for better drawing order.
                SimpleGraph.DataScale jScale =  motionGraph.getScale(axis.getName()+" j");
                jScale.setSymmetricIfSigned(true);
                SimpleGraph.DataRow jRow = motionGraph.getRow(axis.getName()+" j", "j");
                jRow.setDisplayCycleMask(0x2);
                jRow.setColor(new Color(0xDD, 0xBB, 0x00)); 

                SimpleGraph.DataScale aScale =  motionGraph.getScale(axis.getName()+" a");
                aScale.setSymmetricIfSigned(true);
                SimpleGraph.DataRow aRow = motionGraph.getRow(axis.getName()+" a", "a");
                aRow.setDisplayCycleMask(0x2);
                aRow.setColor(new Color(0xFF, 0x00, 0x00)); 
                aRow = motionGraph.getRow(axis.getName()+" a", "a'");
                aRow.setDisplayCycleMask(0x1);
                aRow.setColor(new Color(0xFF, 0x00, 0x00, alphaBlend)); 

                SimpleGraph.DataScale vScale =  motionGraph.getScale(axis.getName()+" V");
                vScale.setSymmetricIfSigned(true);
                SimpleGraph.DataRow vRow = motionGraph.getRow(axis.getName()+" V", "V");
                vRow.setDisplayCycleMask(0x2);
                vRow.setColor(new Color(00, 0x5B, 0xD9)); // the OpenPNP blue
                vRow = motionGraph.getRow(axis.getName()+" V", "V'");
                vRow.setDisplayCycleMask(0x1);
                vRow.setColor(new Color(00, 0x5B, 0xD9, alphaBlend)); 

                SimpleGraph.DataScale sScale =  motionGraph.getScale(axis.getName());
                sScale.setColor(gridColor);
                sScale.setLabelShown(true);

                SimpleGraph.DataRow sRow = motionGraph.getRow(axis.getName(), "s");
                sRow.setDisplayCycleMask(0x2);
                sRow.setColor(new Color(00, 0x77, 0x00)); 
                sRow = motionGraph.getRow(axis.getName(), "s'");
                sRow.setDisplayCycleMask(0x1);
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
        double padding = 1.0/(count*10);
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
                            scale.setRelativePaddingTop(padding*2+((double)n)/count);
                            scale.setRelativePaddingBottom(padding*2+((double)count - n - 1)/count);
                        }
                        else {
                            scale.setRelativePaddingTop(padding+((double)n)/count);
                            scale.setRelativePaddingBottom(padding+((double)count - n - 1)/count);
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
    protected void recordDiagnostics(Motion plannedMotion, MoveToCommand moveToCommand, Driver driver, boolean firstAfterCoordination, boolean firstDriver) {
        super.recordDiagnostics(plannedMotion, moveToCommand, driver, firstAfterCoordination, firstDriver);
        if (diagnosticsEnabled) {
            final double tick = 1e-7;
            final double dt = Math.min(0.001, plannedMotion.getTime()/1000 + 1e-6); // 1ms or 1/1000 of whole motion 
            if (recordingMotionGraph == null) {
                startNewMotionGraph();
                recordingT0 = plannedMotion.getPlannedTime0();
                recordingInterpolationFailed = false;
                recordingMoveTimePlanned = 0.0;
            }
            Double timeStart = moveToCommand.getTimeStart();
            double planTime0 = plannedMotion.getPlannedTime0() - recordingT0;
            if (firstAfterCoordination && firstDriver && timeStart == 0) {
                for (Driver driver0 : getMachine().getDrivers()) {
                    recordingT.put(driver0, planTime0);
                }
                recordingLocation0 = plannedMotion.getLocation0();
            }
            AxesLocation segment = moveToCommand.getLocation0().motionSegmentTo(moveToCommand.getMovedAxesLocation());

            // Calculate the factor from this single lead Axis to the rate along the relevant axes 
            // (either linear or rotational axes, according to RS274NGC).  
            double distance = segment.getRS274NGCMetric(driver, 
                    (axis) -> segment.getCoordinate(axis));
            double factorRS274NGC = (distance != 0 ? 1/distance : 1);

            Double d = moveToCommand.getTimeDuration(); 
            AxesLocation recordingLocation1 = recordingLocation0.put(moveToCommand.getMovedAxesLocation());
            if (showApproximation ) {
                for (ControllerAxis axis : plannedMotion.getLocation1().getAxes(driver)) {
                    MotionProfile profile = plannedMotion.getAxesProfiles()[plannedMotion.getAxisIndex(axis)];
                    if (recordingMotionGraph.getRow(axis.getName(), "s'").size() > 0 
                            || ! profile.isEmpty()) {
                        double t = recordingT.get(driver);
                        SimpleGraph.DataRow sRow = recordingMotionGraph.getRow(axis.getName(), "s'");
                        SimpleGraph.DataRow vRow = recordingMotionGraph.getRow(axis.getName()+" V", "V'");
                        SimpleGraph.DataRow aRow = recordingMotionGraph.getRow(axis.getName()+" a", "a'");
                        if (d != null) {
                            d -= 3*tick;// subtract one tick to make it unique.
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
                                boolean done = false;
                                if (v != null && plannedMotion.getTime() == moveToCommand.getTimeDuration()) { 
                                    // Single trapezoidal move
                                    v *= factor;
                                    double t0 = (v-v0)/a;
                                    double t1 = (v-v1)/a;
                                    double tMid = d-t0-t1;
                                    if (tMid < tick) {
                                        if (t0 > t1) {
                                            t0 = Math.max(0, d-t1-tick*3);
                                        } else {
                                            t1 = Math.max(0, d-t0-tick*3);
                                        }
                                        tMid = d-t0-t1;
                                    }
                                    if ((t0 > 2*tick || t1 > 2*tick) && tMid >= 0) {
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
                                        done = true;
                                    }
                                }
                                if (! done) {
                                    if (d > 0) {
                                        // One-sided ramp.
                                        // Acceleration is deduced from v0, v1 rather than from the given acceleration.
                                        a = (v1 - v0)/d;
                                        // s Ramp
                                        for (double ts = dt; ts < d; ts += dt) {
                                            double sm = s0 + v0*ts + 1./2*a*Math.pow(ts, 2);
                                            sRow.recordDataPoint(t+ts, sm);
                                        }
                                        aRow.recordDataPoint(t, a);
                                        aRow.recordDataPoint(t+d, a);
                                    }
//                                    else {
//                                        aRow.recordDataPoint(t, 0);
//                                        aRow.recordDataPoint(t+d, 0);
//                                    }
                                }
                                vRow.recordDataPoint(t, v0);
                                vRow.recordDataPoint(t+d, v1);
                            }
                            // Nick to zero
                            aRow.recordDataPoint(t-tick, 0);
                            aRow.recordDataPoint(t+d+tick, 0);
                        }
                        else if (timeStart == 0 && driver.getMotionControlType().isUnpredictable()) {
                            // No approximation possible due to driver setting. Just connect s and show limits to illustrate.
                            d = plannedMotion.getTime()-3*tick;
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
                            // Nick to zero
                            aRow.recordDataPoint(t-tick, 0);
                            aRow.recordDataPoint(t+d+tick, 0);
                        }
                    }
                }
                // Remember were we were.
                if (moveToCommand.getTimeDuration() != null) {
                    recordingT.put(driver, recordingT.get(driver) + moveToCommand.getTimeDuration());
                }
                recordingLocation0 = recordingLocation1; 
            }

            if (timeStart == 0 && firstDriver) {
                // First interpolation command: Show the planned/non-interpolated motion.
                double tm = planTime0;
                double dm = plannedMotion.getTime() - 3*tick;
                AxesLocation segmentAll = plannedMotion.getLocation0().motionSegmentTo(plannedMotion.getLocation1());
                for (ControllerAxis axis : plannedMotion.getLocation1().getControllerAxes()) {
                    if (segmentAll.contains(axis) || recordingMotionGraph.getRow(axis.getName(), "s").size() > 0) {
                        MotionProfile profile = plannedMotion.getAxesProfiles()[plannedMotion.getAxisIndex(axis)];
                        SimpleGraph.DataRow sRow = recordingMotionGraph.getRow(axis.getName(), "s");
                        SimpleGraph.DataRow vRow = recordingMotionGraph.getRow(axis.getName()+" V", "V");
                        SimpleGraph.DataRow aRow = recordingMotionGraph.getRow(axis.getName()+" a", "a");
                        SimpleGraph.DataRow jRow = null;
                        if (!profile.isConstantAcceleration()) {
                            jRow = recordingMotionGraph.getRow(axis.getName()+" j", "j");
                        }
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
                        aRow.recordDataPoint(tm - tick, 0);
                        aRow.recordDataPoint(tm + dm + tick, 0);
                        if (jRow != null) {
                            jRow.recordDataPoint(tm - tick, 0);
                            jRow.recordDataPoint(tm + dm + tick, 0);
                        }
                    }
                }
                recordingMoveTimePlanned += plannedMotion.getTime();
                if (plannedMotion.hasOption(MotionOption.InterpolationFailed)) {
                    recordingInterpolationFailed = true;
                }
            }
        }
    }

    @Override
    protected void publishDiagnostics() {
        if (!recordingMotionLocked) {
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
    }

    public void testMotion(HeadMountable tool, boolean reverse) throws Exception {
        boolean wasDiagnosticsEnabled = isDiagnosticsEnabled(); 
        Double dt = null;
        try {
            setDiagnosticsEnabled(false);
            double speed = getMachine().getSpeed();
            Location initialLocation = getInitialLocation(reverse);
            Location l = tool.getLocation().convertToUnits(LengthUnit.Millimeters);
            if (reverse) {
                if (l.getXyzcDistanceTo(initialLocation) > 0.1) {
                    MovableUtils.moveToLocationAtSafeZ(tool, initialLocation);
                }
                tool.waitForCompletion(CompletionType.WaitForUnconditionalCoordination);
                setDiagnosticsEnabled(true);
                setRecordingMotionLocked(true); 
                double t0 = NanosecondTime.getRuntimeSeconds();
                if (mid2LocationEnabled && midLocation2 != initialLocation) {
                    if (!toEndSafeZ) {
                        tool.moveTo(midLocation2, toEndSpeed*speed);
                    }
                    else {
                        MovableUtils.moveToLocationAtSafeZ(tool, midLocation2, toEndSpeed*speed);
                    }
                }
                if (mid1LocationEnabled && midLocation1 != initialLocation) {
                    if (!toMid2SafeZ) {
                        tool.moveTo(midLocation1, toMid2Speed*speed);
                    }
                    else {
                        MovableUtils.moveToLocationAtSafeZ(tool, midLocation1, toMid2Speed*speed);
                    }
                }
                if (startLocationEnabled) {
                    if (!toMid1SafeZ) {
                        tool.moveTo(startLocation, toMid1Speed*speed);
                    }
                    else {
                        MovableUtils.moveToLocationAtSafeZ(tool, startLocation, toMid1Speed*speed);
                    }
                }
                tool.waitForCompletion(CompletionType.WaitForStillstand);
                dt = NanosecondTime.getRuntimeSeconds() - t0;
            }
            else {
                if (l.getXyzcDistanceTo(initialLocation) > 0.1) {
                    MovableUtils.moveToLocationAtSafeZ(tool, initialLocation);
                }
                tool.waitForCompletion(CompletionType.WaitForUnconditionalCoordination);
                setDiagnosticsEnabled(true);
                setRecordingMotionLocked(true); 
                double t0 = NanosecondTime.getRuntimeSeconds();
                if (mid1LocationEnabled && midLocation1 != initialLocation) {
                    if (!toMid1SafeZ) {
                        tool.moveTo(midLocation1, toMid1Speed*speed);
                    }
                    else {
                        MovableUtils.moveToLocationAtSafeZ(tool, midLocation1, toMid1Speed*speed);
                    }
                }
                if (mid2LocationEnabled && midLocation2 != initialLocation) {
                    if (!toMid2SafeZ) {
                        tool.moveTo(midLocation2, toMid2Speed*speed);
                    }
                    else {
                        MovableUtils.moveToLocationAtSafeZ(tool, midLocation2, toMid2Speed*speed);
                    }
                }
                if (endLocationEnabled) {
                    if (!toEndSafeZ) {
                        tool.moveTo(endLocation, toEndSpeed*speed);
                    }
                    else {
                        MovableUtils.moveToLocationAtSafeZ(tool, endLocation, toEndSpeed*speed);
                    }
                }
                tool.waitForCompletion(CompletionType.WaitForStillstand);
                dt = NanosecondTime.getRuntimeSeconds() - t0;
            }
        }
        finally {
            // Switch off diagnostics for move to Safe Z.
            setDiagnosticsEnabled(false);
            tool.moveToSafeZ(0.2);
            tool.waitForCompletion(CompletionType.CommandJog);
            setDiagnosticsEnabled(wasDiagnosticsEnabled);
            // Unlock and publish.
            setRecordingMotionLocked(false);
            if (dt != null) {
                setMoveTimeActual(dt);
            }
        }
    }

    public Location getInitialLocation(boolean reverse) {
        Location location = null;
        if (reverse) {
            if (endLocationEnabled) {
                location = endLocation;
            }
            else if (mid2LocationEnabled) {
                location = midLocation2;
            }
            else if (mid1LocationEnabled) {
                location = midLocation1;
            }
        }
        else {
            if (startLocationEnabled) {
                location = startLocation;
            }
            else if (mid1LocationEnabled) {
                location = midLocation1;
            }
            else if (mid2LocationEnabled) {
                location = midLocation2;
            }
        }
        return location;
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

    @Override
    public void findIssues(Solutions solutions) {
        super.findIssues(solutions);
        if (!isAllowContinuousMotion()) {
            solutions.add(new Solutions.Issue(
                    this, 
                    "Use continuous motion. OpenPnP will only wait for the machine when really needed.", 
                    "Enable Continuous Motion.", 
                    Severity.Suggestion.Suggestion,
                    "https://github.com/openpnp/openpnp/wiki/Motion-Planner#motion-planner") {

                @Override
                public void setState(Solutions.State state) throws Exception {
                    setAllowContinuousMotion((state == Solutions.State.Solved));
                    super.setState(state);
                }
            });
        }
    }
}
