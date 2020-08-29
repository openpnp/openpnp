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
import java.util.List;

import javax.swing.Action;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.marek.MarekNozzle;
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
import org.openpnp.model.Configuration;
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
import org.openpnp.spi.base.AbstractMachine;
import org.openpnp.spi.base.SimplePropertySheetHolder;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Commit;

public class ReferenceMachine extends AbstractMachine {
    @Deprecated
    @Element(required = false)
    private ReferenceDriver driver = null;

    @Element(required = false)
    protected PnpJobProcessor pnpJobProcessor = new ReferencePnpJobProcessor();

    @Element(required = false)
    protected FiducialLocator fiducialLocator = new ReferenceFiducialLocator();

    @Element(required = false)
    protected MotionPlanner motionPlanner = new NullMotionPlanner();

    @Element(required = false)
    private boolean homeAfterEnabled = false;

    private boolean enabled;

    private boolean isHomed = false;

    private List<Class<? extends Axis>> registeredAxisClasses = new ArrayList<>();

    private List<Class<? extends Feeder>> registeredFeederClasses = new ArrayList<>();

    private List<Class<? extends Driver>> registeredDriverClasses = new ArrayList<>();

    @Commit
    protected void commit() {
        super.commit();
    }

    public ReferenceDriver getDefaultDriver() {
        // If this is a brand new Machine, create a NullDriver.
        if (drivers.isEmpty()) {
            drivers.add(new NullDriver());
        }
        return (ReferenceDriver) drivers.get(0);
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
                             if (driver != null) {
                                 // Note, the migrated driver will add itself to the machine driver list 
                                 // and for GcodeDrivers it will recurse into the sub-drivers.
                                 driver.migrateDriver(ReferenceMachine.this);
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
            try {
                for (Driver driver : getDrivers()) {
                    ((ReferenceDriver)driver).setEnabled(true);
                }
                this.enabled = true;
            }
            catch (Exception e) {
                fireMachineEnableFailed(e.getMessage());
                throw e;
            }
            fireMachineEnabled();
        }
        else {
            try {
                for (Driver driver : getDrivers()) {
                    ((ReferenceDriver)driver).setEnabled(false);
                }
                this.enabled = false;
            }
            catch (Exception e) {
                fireMachineDisableFailed(e.getMessage());
                throw e;
            }
            fireMachineDisabled("User requested stop.");

            // remove homed-flag if machine is disabled
            this.setHomed(false);
        }
    }

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
        return new PropertySheet[] {new PropertySheetWizardAdapter(getConfigurationWizard())};
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
        return l;
    }

    @Override
    public List<Class<? extends Nozzle>> getCompatibleNozzleClasses() {
        List<Class<? extends Nozzle>> l = new ArrayList<>();
        l.add(ReferenceNozzle.class);
        l.add(ContactProbeNozzle.class);
        l.add(MarekNozzle.class);
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

    private List<Class<? extends PartAlignment>> registeredAlignmentClasses = new ArrayList<>();

    @Override
    public void home() throws Exception {
        Logger.debug("homing machine");
        
        // if one rehomes, the isHomed flag has to be removed
        this.setHomed(false);
        
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

    @Override
    public void setHomed(boolean isHomed) {
        Logger.debug("setHomed({})", isHomed);
        this.isHomed = isHomed;
        firePropertyChange("homed", null, this.isHomed);
    }
}
