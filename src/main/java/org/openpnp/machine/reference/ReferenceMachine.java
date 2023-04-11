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
import java.util.List;
import java.util.Set;

import javax.swing.Action;

import org.openpnp.ConfigurationListener;
import org.openpnp.Translations;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.neoden4.NeoDen4Driver;
import org.openpnp.machine.neoden4.NeoDen4FeederActuator;
import org.openpnp.machine.neoden4.Neoden4Camera;
import org.openpnp.machine.neoden4.Neoden4Feeder;
import org.openpnp.machine.neoden4.Neoden4Signaler;
import org.openpnp.machine.neoden4.Neoden4SwitcherCamera;
import org.openpnp.machine.photon.PhotonFeeder;
import org.openpnp.machine.rapidplacer.RapidFeeder;
import org.openpnp.machine.reference.actuator.ThermistorToLinearSensorActuator;
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
import org.openpnp.machine.reference.feeder.ReferenceHeapFeeder;
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
import org.openpnp.machine.reference.solutions.CalibrationSolutions;
import org.openpnp.machine.reference.solutions.KinematicSolutions;
import org.openpnp.machine.reference.solutions.NozzleTipSolutions;
import org.openpnp.machine.reference.solutions.ScriptingSolutions;
import org.openpnp.machine.reference.solutions.VisionSolutions;
import org.openpnp.machine.reference.vision.ReferenceBottomVision;
import org.openpnp.machine.reference.vision.ReferenceFiducialLocator;
import org.openpnp.machine.reference.wizards.ReferenceMachineConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Solutions;
import org.openpnp.model.Solutions.Milestone;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Axis;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Driver;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.FiducialLocator;
import org.openpnp.spi.Head;
import org.openpnp.spi.MotionPlanner;
import org.openpnp.spi.MotionPlanner.CompletionType;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PartAlignment;
import org.openpnp.spi.PnpJobProcessor;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.Signaler;
import org.openpnp.spi.base.AbstractDriver;
import org.openpnp.spi.base.AbstractMachine;
import org.openpnp.spi.base.SimplePropertySheetHolder;
import org.openpnp.util.Collect;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.UiUtils;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
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

    @Element(required = false)
    private boolean parkAfterHomed = false;

    @Attribute(required = false)
    private boolean autoToolSelect = true;

    @Attribute(required = false)
    private boolean safeZPark = true;

    @Element(required = false)
    private Length unsafeZRoamingDistance = new Length(10, LengthUnit.Millimeters);

    @Element(required = false)
    private boolean poolScriptingEngines = false;

    @Element(required = false)
    private Solutions solutions = new Solutions();

    @Deprecated // now in the Solutions object.
    @ElementList(required = false)
    Set<String> dismissedSolutions = null;

    private boolean enabled;

    private boolean isHomed = false;

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
                boolean syncLocation = false;
                for (Driver driver : getDrivers()) {
                    driver.setEnabled(true);
                    enabledDrivers.add(driver);
                    if (driver.isSyncInitialLocation()) {
                        syncLocation = true;
                    }
                }
                this.enabled = true;
                if (syncLocation) {
                    // We wait for still-stand, because as a side-effect, it will allow OpenPnP to sync its
                    // position to the initial reported location (see Driver.isSyncInitialLocation()).
                    getMotionPlanner().waitForCompletion(null, CompletionType.WaitForStillstand);
                }
                if (getHomeAfterEnabled() && isTask(Thread.currentThread())) {
                    UiUtils.submitUiMachineTask(() -> home());
                }
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
            getMotionPlanner().unhome();
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
        Object oldValue = this.motionPlanner;
        this.motionPlanner = motionPlanner;
        firePropertyChange("motionPlanner", oldValue, motionPlanner);
    }

    @Override
    public boolean isAutoToolSelect() {
        return autoToolSelect;
    }

    public void setAutoToolSelect(boolean autoToolSelect) {
        Object oldValue = this.autoToolSelect;
        this.autoToolSelect = autoToolSelect;
        firePropertyChange("autoToolSelect", oldValue, autoToolSelect);
    }

    @Override
    public boolean isSafeZPark() {
        return safeZPark;
    }

    public void setSafeZPark(boolean safeZPark) {
        Object oldValue = this.safeZPark;
        this.safeZPark = safeZPark;
        firePropertyChange("safeZPark", oldValue, safeZPark);
    }

    @Override
    public boolean isParkAfterHomed() {
        return parkAfterHomed;
    }

    public void setParkAfterHomed(boolean parkAfterHomed) {
        this.parkAfterHomed = parkAfterHomed;
    }

    @Override
    public Length getUnsafeZRoamingDistance() {
        return unsafeZRoamingDistance;
    }

    public void setUnsafeZRoamingDistance(Length unsafeZRoamingDistance) {
        Object oldValue = this.unsafeZRoamingDistance;
        this.unsafeZRoamingDistance = unsafeZRoamingDistance;
        firePropertyChange("safeRoamingDistance", oldValue, unsafeZRoamingDistance);
    }

    @Override
    public boolean isPoolScriptingEngines() {
        return poolScriptingEngines;
    }

    public void setPoolScriptingEngines(boolean poolScriptingEngines) {
        this.poolScriptingEngines = poolScriptingEngines;
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
        children.add(new AxesPropertySheetHolder(this, Translations.getString(
                "ReferenceMachine.PropertySheetHolder.Axes.title"), getAxes(), null)); //$NON-NLS-1$
        children.add(new SignalersPropertySheetHolder(this, Translations.getString(
                "ReferenceMachine.PropertySheetHolder.Signalers.title"), getSignalers(), null)); //$NON-NLS-1$
        children.add(new SimplePropertySheetHolder(Translations.getString(
                "ReferenceMachine.PropertySheetHolder.Feeders.title"), getFeeders())); //$NON-NLS-1$
        children.add(new SimplePropertySheetHolder(Translations.getString(
                "ReferenceMachine.PropertySheetHolder.Heads.title"), getHeads())); //$NON-NLS-1$
        children.add(new NozzleTipsPropertySheetHolder(Translations.getString(
                "ReferenceMachine.PropertySheetHolder.NozzleTips.title"), //$NON-NLS-1$
                getNozzleTips(), null));
        children.add(new CamerasPropertySheetHolder(null, Translations.getString(
                "ReferenceMachine.PropertySheetHolder.Cameras.title"), getCameras(), null)); //$NON-NLS-1$
        children.add(new ActuatorsPropertySheetHolder(null, Translations.getString(
                "ReferenceMachine.PropertySheetHolder.Actuators.title"), getActuators(), null)); //$NON-NLS-1$
        children.add(new DriversPropertySheetHolder(this, Translations.getString(
                "ReferenceMachine.PropertySheetHolder.Drivers.title"), getDrivers(), null)); //$NON-NLS-1$
        children.add(new SimplePropertySheetHolder(Translations.getString(
                "ReferenceMachine.PropertySheetHolder.JobProcessors.title"), Arrays.asList(getPnpJobProcessor()))); //$NON-NLS-1$

        List<PropertySheetHolder> vision = new ArrayList<>();
        for (PartAlignment alignment : getPartAlignments()) {
            vision.add(alignment);
        }
        vision.add(getFiducialLocator());
        children.add(new SimplePropertySheetHolder(Translations.getString(
                "ReferenceMachine.PropertySheetHolder.Vision.title"), vision)); //$NON-NLS-1$
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
        l.add(ReferenceHeapFeeder.class);
        l.add(BlindsFeeder.class);
        l.add(SchultzFeeder.class);
        l.add(SlotSchultzFeeder.class);
        l.add(RapidFeeder.class);
        l.add(Neoden4Feeder.class);
        l.add(PhotonFeeder.class);
        l.addAll(registeredFeederClasses);
        return l;
    }

    @Override
    public List<Class<? extends Camera>> getCompatibleCameraClasses() {
        List<Class<? extends Camera>> l = new ArrayList<>();
        l.add(OpenPnpCaptureCamera.class);
        l.add(OpenCvCamera.class);
        l.add(Neoden4Camera.class);
        l.add(Neoden4SwitcherCamera.class);
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
        l.add(ThermistorToLinearSensorActuator.class);
        l.add(NeoDen4FeederActuator.class);
        return l;
    }

    @Override
    public List<Class<? extends Signaler>> getCompatibleSignalerClasses() {
        List<Class<? extends Signaler>> l = new ArrayList<>();
        l.add(SoundSignaler.class);
        l.add(ActuatorSignaler.class);
        l.add(Neoden4Signaler.class);
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
            getMotionPlanner().unhome();
            this.setHomed(false);
        }

        getMotionPlanner().home();
        super.home();

        Configuration.get().getScripting().on("Machine.AfterHoming", null);

        // if homing went well, set machine homed-flag true
        this.setHomed(true);
        
        if (isParkAfterHomed()) {
            for (Head head : getHeads()) {
                MovableUtils.park(head);
            }
        }
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
    @Override
    public void setHomed(boolean isHomed) {
        Logger.info("setHomed({})", isHomed);
        this.isHomed = isHomed;
        firePropertyChange("homed", null, this.isHomed);
        fireMachineHomed(isHomed);
    }

    public Solutions getSolutions() {
        if (dismissedSolutions != null) {
            // Migrate to Solutions object.
            solutions.migrateDismissedSolutions(dismissedSolutions);
            dismissedSolutions = null;
        }
        return solutions;
    }

    //@Element(required = false)
    private KinematicSolutions kinematicSolutions = new KinematicSolutions(); 

    //@Element(required = false)
    private NozzleTipSolutions nozzleTipSolutions = new NozzleTipSolutions();

    @Element(required = false)
    private VisionSolutions visualSolutions = new VisionSolutions();

    public VisionSolutions getVisionSolutions() {
        return visualSolutions;
    }

    @Element(required = false)
    private CalibrationSolutions calibrationSolutions = new CalibrationSolutions(); 

    public CalibrationSolutions getCalibrationSolutions() {
        return calibrationSolutions;
    }

    private ScriptingSolutions scriptingSolutions = new ScriptingSolutions();

    @Override
    public void findIssues(Solutions solutions) {
        kinematicSolutions.setMachine(this).findIssues(solutions);
        nozzleTipSolutions.setMachine(this).findIssues(solutions);
        visualSolutions.setMachine(this).findIssues(solutions);
        calibrationSolutions.setMachine(this).findIssues(solutions);
        scriptingSolutions.setMachine(this).findIssues(solutions);

        if (solutions.isTargeting(Milestone.Advanced)) {
            if (getMotionPlanner() instanceof NullMotionPlanner) {
                solutions.add(new Solutions.Issue(
                        this, 
                        "Advanced Motion Planner not set. Accept or Dismiss to continue.", 
                        "Change to ReferenceAdvancedMotionPlanner", 
                        Solutions.Severity.Fundamental,
                        "https://github.com/openpnp/openpnp/wiki/Motion-Planner#choosing-a-motion-planner") {
                    final MotionPlanner oldMotionPlanner =  ReferenceMachine.this.getMotionPlanner();

                    @Override
                    public void setState(Solutions.State state) throws Exception {
                        if ((state == Solutions.State.Solved)) {
                            setMotionPlanner(new ReferenceAdvancedMotionPlanner());
                        } 
                        else {
                            setMotionPlanner(oldMotionPlanner);
                        }
                        super.setState(state);
                    }
                });
            }
        }
        else {
            // Conservative settings.
            if (!(getMotionPlanner() instanceof NullMotionPlanner)) {
                solutions.add(new Solutions.Issue(
                        this, 
                        "Advanced motion planner set. Revert to a simpler, safer planner.", 
                        "Change to NullMotionPlanner", 
                        Solutions.Severity.Information,
                        "https://github.com/openpnp/openpnp/wiki/Motion-Planner#choosing-a-motion-planner") {
                    final MotionPlanner oldMotionPlanner =  ReferenceMachine.this.getMotionPlanner();

                    @Override
                    public boolean isUnhandled( ) {
                        // Never handle a conservative solution as unhandled.
                        return false;
                    }

                    @Override 
                    public String getExtendedDescription() {
                        return "<html><span color=\"red\">CAUTION:</span> This is a troubleshooting option, offered to remove the ReferenceAdvancedMotionPlanner "
                                + "if it causes problems, or if you don't want it after all. Going back to the plain NullPlanner will lose you all the "
                                + "advanced configuration.</html>";
                    }

                    @Override
                    public void setState(Solutions.State state) throws Exception {
                        if ((state == Solutions.State.Solved)) {
                            setMotionPlanner(new NullMotionPlanner());
                        } 
                        else {
                            setMotionPlanner(oldMotionPlanner);
                        }
                        super.setState(state);
                    }
                });
            }
        }
        if (solutions.isTargeting(Milestone.Basics) && ! isAutoToolSelect()) {
            solutions.add(new Solutions.Issue(
                    this, 
                    "OpenPnP can often automatically select the right tool for you in Machine Controls.", 
                    "Enable Auto tool select.", 
                    Solutions.Severity.Suggestion,
                    "https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration:-Machine-Setup#configuration") {

                @Override
                public void setState(Solutions.State state) throws Exception {
                    setAutoToolSelect((state == Solutions.State.Solved));
                    super.setState(state);
                }
            });
        }
        super.findIssues(solutions);
    }
}
