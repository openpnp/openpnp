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

package org.openpnp.machine.reference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Action;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.neoden4.NeoDen4Driver;
import org.openpnp.machine.neoden4.Neoden4Camera;
import org.openpnp.machine.rapidplacer.RapidFeeder;
import org.openpnp.machine.reference.axis.ReferenceCamClockwiseAxis;
import org.openpnp.machine.reference.axis.ReferenceCamCounterClockwiseAxis;
import org.openpnp.machine.reference.axis.ReferenceControllerAxis;
import org.openpnp.machine.reference.axis.ReferenceLinearTransformAxis;
import org.openpnp.machine.reference.axis.ReferenceMappedAxis;
import org.openpnp.machine.reference.axis.ReferenceVirtualAxis;
import org.openpnp.machine.reference.camera.ImageCamera;
import org.openpnp.machine.reference.camera.MjpgCaptureCamera;
import org.openpnp.machine.reference.camera.OnvifIPCamera;
import org.openpnp.machine.reference.camera.OpenCvCamera;
import org.openpnp.machine.reference.camera.OpenPnpCaptureCamera;
import org.openpnp.machine.reference.camera.SimulatedUpCamera;
import org.openpnp.machine.reference.camera.SwitcherCamera;
import org.openpnp.machine.reference.camera.Webcams;
import org.openpnp.machine.reference.driver.GcodeAsyncDriver;
import org.openpnp.machine.reference.driver.GcodeDriver;
import org.openpnp.machine.reference.driver.NullDriver;
import org.openpnp.machine.reference.driver.NullMotionPlanner;
import org.openpnp.machine.reference.driver.ReferenceAdvancedMotionPlanner;
import org.openpnp.machine.reference.feeder.AdvancedLoosePartFeeder;
import org.openpnp.machine.reference.feeder.BlindsFeeder;
import org.openpnp.machine.reference.feeder.ReferenceAutoFeeder;
import org.openpnp.machine.reference.feeder.ReferenceDragFeeder;
import org.openpnp.machine.reference.feeder.ReferenceLeverFeeder;
import org.openpnp.machine.reference.feeder.ReferenceLoosePartFeeder;
import org.openpnp.machine.reference.feeder.ReferencePushPullFeeder;
import org.openpnp.machine.reference.feeder.ReferenceRotatedTrayFeeder;
import org.openpnp.machine.reference.feeder.ReferenceSlotAutoFeeder;
import org.openpnp.machine.reference.feeder.ReferenceStripFeeder;
import org.openpnp.machine.reference.feeder.ReferenceTrayFeeder;
import org.openpnp.machine.reference.feeder.ReferenceTubeFeeder;
import org.openpnp.machine.reference.feeder.SchultzFeeder;
import org.openpnp.machine.reference.feeder.SlotSchultzFeeder;
import org.openpnp.machine.reference.psh.ActuatorsPropertySheetHolder;
import org.openpnp.machine.reference.psh.AxesPropertySheetHolder;
import org.openpnp.machine.reference.psh.CamerasPropertySheetHolder;
import org.openpnp.machine.reference.psh.DriversPropertySheetHolder;
import org.openpnp.machine.reference.psh.NozzleTipsPropertySheetHolder;
import org.openpnp.machine.reference.psh.SignalersPropertySheetHolder;
import org.openpnp.machine.reference.signaler.ActuatorSignaler;
import org.openpnp.machine.reference.signaler.SoundSignaler;
import org.openpnp.machine.reference.vision.ReferenceBottomVision;
import org.openpnp.machine.reference.vision.ReferenceFiducialLocator;
import org.openpnp.machine.reference.wizards.ReferenceMachineConfigurationWizard;
import org.openpnp.machine.reference.wizards.ReferenceMachineSolutionsWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.Solutions;
import org.openpnp.model.Solutions.Issue;
import org.openpnp.model.Solutions.Severity;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Axis;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Driver;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.FiducialLocator;
import org.openpnp.spi.Head;
import org.openpnp.spi.MotionPlanner;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PartAlignment;
import org.openpnp.spi.PnpJobProcessor;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.Signaler;
import org.openpnp.spi.base.AbstractDriver;
import org.openpnp.spi.base.AbstractMachine;
import org.openpnp.spi.base.SimplePropertySheetHolder;
import org.openpnp.util.Collect;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.core.Commit;

public class ReferenceMachine extends AbstractMachine {
    @Deprecated
    @Element(required = false)
    private Driver driver = null;

    @Element(required = false)
    protected PnpJobProcessor pnpJobProcessor = new ReferencePnpJobProcessor();

    @Element(required = false)
    protected FiducialLocator fiducialLocator = new ReferenceFiducialLocator();

    @Element(required = false)
    protected MotionPlanner motionPlanner = new NullMotionPlanner();

    @Element(required = false)
    private boolean homeAfterEnabled = false;

    @ElementList(required = false)
    Set<String> dismissedSolutions = new HashSet<>();

    private boolean enabled;

    private boolean isHomed = false;

    private Solutions solutions = new Solutions();

    private List<Class<? extends Axis>> registeredAxisClasses = new ArrayList<>();

    private List<Class<? extends Feeder>> registeredFeederClasses = new ArrayList<>();

    private List<Class<? extends Driver>> registeredDriverClasses = new ArrayList<>();

    @Commit
    protected void commit() {
        super.commit();
    }

    public Driver getDefaultDriver() {
        // If this is a brand new Machine, create a NullDriver.
        if (drivers.isEmpty()) {
            drivers.add(new NullDriver());
        }
        return drivers.get(0);
    }

    public ReferenceMachine() {
        Configuration.get()
                     .addListener(new ConfigurationListener.Adapter() {

                         @Override
                         public void configurationLoaded(Configuration configuration)
                                 throws Exception {
                             if (partAlignments.isEmpty()) {
                                 partAlignments.add(new ReferenceBottomVision());
                             }
                             // Migrate the driver.
                             if (driver != null && driver instanceof AbstractDriver) {
                                 // Note, the migrated driver will add itself to the machine driver list 
                                 // and for GcodeDrivers it will recurse into the sub-drivers.
                                 ((AbstractDriver)driver).migrateDriver(ReferenceMachine.this);
                                 driver = null;
                             }
                         }
                     });
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) throws Exception {
        Logger.debug("setEnabled({})", enabled);
        if (enabled) {
            List<Driver> enabledDrivers = new ArrayList<>();
            try {
                for (Driver driver : getDrivers()) {
                    driver.setEnabled(true);
                    enabledDrivers.add(driver);
                }
                this.enabled = true;
            }
            catch (Exception e) {
                // In a multi-driver machine, we must make sure its all-or-nothing, 
                // like a roll-back in a database -> if one fails, disable the others again.
                // We want reverse disabling order.
                Collections.reverse(enabledDrivers);
                for (Driver driver : enabledDrivers) {
                    try {
                        driver.setEnabled(false);
                    }
                    catch (Exception e1) {
                        Logger.warn(e1);
                    }
                }
                fireMachineEnableFailed(e.getMessage());
                throw e;
            }
            fireMachineEnabled();
        }
        else {
            // remove homed-flag if machine is disabled
            this.setHomed(false);
            fireMachineAboutToBeDisabled("User requested stop.");
            // In a multi-driver machine, we must try to disable all drivers even if one throws.
            Exception e = null;
            List<Driver> enabledDrivers = new ArrayList<>();
            enabledDrivers.addAll(getDrivers());
            // We want reverse disabling order.
            Collections.reverse(enabledDrivers);
            for (Driver driver : enabledDrivers) {
                try {
                    driver.setEnabled(false);
                }
                catch (Exception e1) {
                    Logger.warn(e1);
                    e = e1;
                }
            }
            this.enabled = false;
            if (e != null) {
                fireMachineDisableFailed(e.getMessage());
                throw e;
            }
            fireMachineDisabled("User requested stop.");
        }
    }

    @Override
    public MotionPlanner getMotionPlanner() {
        return motionPlanner;
    }

    public void setMotionPlanner(MotionPlanner motionPlanner) {
        this.motionPlanner = motionPlanner;
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceMachineConfigurationWizard(this);
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        ArrayList<PropertySheetHolder> children = new ArrayList<>();
        children.add(new AxesPropertySheetHolder(this, "Axes", getAxes(), null));
        children.add(new SignalersPropertySheetHolder(this, "Signalers", getSignalers(), null));
        children.add(new SimplePropertySheetHolder("Feeders", getFeeders()));
        children.add(new SimplePropertySheetHolder("Heads", getHeads()));
        children.add(new NozzleTipsPropertySheetHolder("Nozzle Tips", getNozzleTips(), null));
        children.add(new CamerasPropertySheetHolder(null, "Cameras", getCameras(), null));
        children.add(new ActuatorsPropertySheetHolder(null, "Actuators", getActuators(), null));
        children.add(new DriversPropertySheetHolder(this, "Drivers", getDrivers(), null));
        children.add(new SimplePropertySheetHolder("Job Processors",
                Arrays.asList(getPnpJobProcessor())));

        List<PropertySheetHolder> vision = new ArrayList<>();
        for (PartAlignment alignment : getPartAlignments()) {
            vision.add(alignment);
        }
        vision.add(getFiducialLocator());
        children.add(new SimplePropertySheetHolder("Vision", vision));
        return children.toArray(new PropertySheetHolder[] {});
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return null;
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return Collect.concat(new PropertySheet[] { 
                    new PropertySheetWizardAdapter(getConfigurationWizard()),
                    new PropertySheetWizardAdapter(new ReferenceMachineSolutionsWizard(this), "Issues & Solutions")
                },
                getMotionPlanner().getPropertySheets());
    }

    public void registerFeederClass(Class<? extends Feeder> cls) {
        registeredFeederClasses.add(cls);
    }

    @Override
    public List<Class<? extends Axis>> getCompatibleAxisClasses() {
        List<Class<? extends Axis>> l = new ArrayList<>();
        l.add(ReferenceControllerAxis.class);
        l.add(ReferenceVirtualAxis.class);
        l.add(ReferenceMappedAxis.class);
        l.add(ReferenceCamCounterClockwiseAxis.class);
        l.add(ReferenceCamClockwiseAxis.class);
        l.add(ReferenceLinearTransformAxis.class);
        return l;
    }

    @Override
    public List<Class<? extends Feeder>> getCompatibleFeederClasses() {
        List<Class<? extends Feeder>> l = new ArrayList<>();
        l.add(ReferenceStripFeeder.class);
        l.add(ReferenceTrayFeeder.class);
        l.add(ReferenceRotatedTrayFeeder.class);
        l.add(ReferenceDragFeeder.class);
        l.add(ReferenceLeverFeeder.class);
        l.add(ReferencePushPullFeeder.class);
        l.add(ReferenceTubeFeeder.class);
        l.add(ReferenceAutoFeeder.class);
        l.add(ReferenceSlotAutoFeeder.class);
        l.add(ReferenceLoosePartFeeder.class);
        l.add(AdvancedLoosePartFeeder.class);
        l.add(BlindsFeeder.class);
        l.add(SchultzFeeder.class);
        l.add(SlotSchultzFeeder.class);
        l.add(RapidFeeder.class);
        l.addAll(registeredFeederClasses);
        return l;
    }

    @Override
    public List<Class<? extends Camera>> getCompatibleCameraClasses() {
        List<Class<? extends Camera>> l = new ArrayList<>();
        l.add(OpenPnpCaptureCamera.class);
        l.add(OpenCvCamera.class);
        l.add(Neoden4Camera.class);
        l.add(Webcams.class);
        l.add(OnvifIPCamera.class);
        l.add(ImageCamera.class);
        l.add(SwitcherCamera.class);
        l.add(SimulatedUpCamera.class);
        l.add(MjpgCaptureCamera.class);
        return l;
    }

    @Override
    public List<Class<? extends Nozzle>> getCompatibleNozzleClasses() {
        List<Class<? extends Nozzle>> l = new ArrayList<>();
        l.add(ReferenceNozzle.class);
        l.add(ContactProbeNozzle.class);
        return l;
    }

    @Override
    public List<Class<? extends Actuator>> getCompatibleActuatorClasses() {
        List<Class<? extends Actuator>> l = new ArrayList<>();
        l.add(ReferenceActuator.class);
        l.add(HttpActuator.class);
        l.add(ScriptActuator.class);
        return l;
    }

    @Override
    public List<Class<? extends Signaler>> getCompatibleSignalerClasses() {
        List<Class<? extends Signaler>> l = new ArrayList<>();
        l.add(SoundSignaler.class);
        l.add(ActuatorSignaler.class);
        return l;
    }

    @Override
    public List<Class<? extends Driver>> getCompatibleDriverClasses() {
        List<Class<? extends Driver>> l = new ArrayList<>();
        l.add(NullDriver.class);
        l.add(GcodeDriver.class);
        l.add(GcodeAsyncDriver.class);
        l.add(NeoDen4Driver.class);
        return l;
    }

    @Override
    public List<Class<? extends MotionPlanner>> getCompatibleMotionPlannerClasses() {
        List<Class<? extends MotionPlanner>> l = new ArrayList<>();
        l.add(NullMotionPlanner.class);
        l.add(ReferenceAdvancedMotionPlanner.class);
        return l;
    }

    private List<Class<? extends PartAlignment>> registeredAlignmentClasses = new ArrayList<>();

    protected MotionPlanner mootionPlanner;

    @Override
    public void home() throws Exception {
        Logger.debug("homing machine");

        if (isHomed()) {
            // if one rehomes, the isHomed flag has to be removed
            this.setHomed(false);
        }

        getMotionPlanner().home();
        super.home();

        try {
            Configuration.get().getScripting().on("Machine.AfterHoming", null);
        }
        catch (Exception e) {
            Logger.warn(e);
        }

        // if homing went well, set machine homed-flag true
        this.setHomed(true);     
    }

    @Override
    public void close() throws IOException {
        for (Driver driver : getDrivers()) {
            try {
                driver.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (Camera camera : getCameras()) {
            try {
                camera.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (Head head : getHeads()) {
            for (Camera camera : head.getCameras()) {
                try {
                    camera.close();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public FiducialLocator getFiducialLocator() {
        return fiducialLocator;
    }

    @Override
    public PnpJobProcessor getPnpJobProcessor() {
        return pnpJobProcessor;
    }

    public boolean getHomeAfterEnabled() {
        return homeAfterEnabled;
    }

    public void setHomeAfterEnabled(boolean newValue) {
        this.homeAfterEnabled = newValue;
    }

    @Override
    public boolean isHomed() {
        return this.isHomed;
    }

    public Solutions getSolutions() {
        return solutions;
    }

    @Override
    public void setHomed(boolean isHomed) {
        Logger.info("setHomed({})", isHomed);
        this.isHomed = isHomed;
        firePropertyChange("homed", null, this.isHomed);
        fireMachineHomed(isHomed);
    }

    @Override
    public void findIssues(List<Solutions.Issue> issues) {
        if (getMotionPlanner() instanceof NullMotionPlanner) {
            issues.add(new Solutions.Issue(
                    this, 
                    "Advanced Motion Planner not set. Accept or Dismiss to continue.", 
                    "Change to ReferenceAdvancedMotionPlanner", 
                    Solutions.Severity.Fundamental,
                    "https://github.com/openpnp/openpnp/wiki/Motion-Planner#choosing-a-motion-planner") {
                final MotionPlanner oldMotionPlanner =  ReferenceMachine.this.getMotionPlanner();

                @Override
                public void setState(Solutions.State state) throws Exception {
                    if (confirmStateChange(state)) {
                        if ((state == Solutions.State.Solved)) {
                            setMotionPlanner(new ReferenceAdvancedMotionPlanner());
                        } 
                        else {
                            setMotionPlanner(oldMotionPlanner);
                        }
                        // Reselect the tree path to reload the wizard with potentially different property sheets. 
                        MainFrame.get().getMachineSetupTab().selectCurrentTreePath();
                        super.setState(state);
                    }
                }
            });
        }
        super.findIssues(issues);
    }

    public void setSolutionsIssues(List<Issue> issues) {
        if (issues.size() == 0) {
            issues.add(new Solutions.Issue(
                    this, 
                    "No issues detected.", 
                    "", 
                    Solutions.Severity.Information,
                    null));
        }
        // Go through the issues and set initially dismissed ones.
        // Also install listeners to update the dismissedTroubleshooting.
        for (Issue issue : issues) {
            if (isSolutionsIssueDismissed(issue)) {
                issue.setInitiallyDismissed();
            }
            issue.addPropertyChangeListener("state", e -> {
                if (e.getOldValue() == Solutions.State.Dismissed) {
                    setSolutionsIssueDismissed(issue, false);
                }
                if (issue.getState() == Solutions.State.Dismissed) {
                    setSolutionsIssueDismissed(issue, true);
                }
                int row = solutions.getIssues().indexOf(issue);
                solutions.fireTableRowsUpdated(row, row);
            });
        }
        // Sort by state (initially only Open and Dismissed possible) and place Fundamentals first.
        issues.sort(new Comparator<Issue>() {
            @Override
            public int compare(Issue o1, Issue o2) {
                int d = o1.getState().ordinal() - o2.getState().ordinal();
                if (d != 0) {
                    return d;
                }
                if (o1.getSeverity() == Severity.Fundamental && o2.getSeverity() != Severity.Fundamental) {
                    return -1;
                }
                else if (o2.getSeverity() == Severity.Fundamental) {
                    return 1;
                }
                else {
                    return 0;
                }
            }
        });
        // Finally set the issues.
        getSolutions().setIssues(issues);
    }

    public boolean isSolutionsIssueDismissed(Issue issue) {
        return dismissedSolutions.contains(issue.getFingerprint());
    }
    public void setSolutionsIssueDismissed(Issue issue, boolean dismissed) {
        if (dismissed) {
            dismissedSolutions.add(issue.getFingerprint()); 
        }
        else {
            dismissedSolutions.remove(issue.getFingerprint());
        }
    }
}
