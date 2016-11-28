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

import javax.swing.Action;

import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.camera.ImageCamera;
import org.openpnp.machine.reference.camera.OnvifIPCamera;
import org.openpnp.machine.reference.camera.OpenCvCamera;
import org.openpnp.machine.reference.camera.SimulatedUpCamera;
import org.openpnp.machine.reference.camera.Webcams;
import org.openpnp.machine.reference.driver.NullDriver;
import org.openpnp.machine.reference.feeder.ReferenceAutoFeeder;
import org.openpnp.machine.reference.feeder.ReferenceDragFeeder;
import org.openpnp.machine.reference.feeder.ReferenceStripFeeder;
import org.openpnp.machine.reference.feeder.ReferenceTrayFeeder;
import org.openpnp.machine.reference.feeder.ReferenceTubeFeeder;
import org.openpnp.machine.reference.vision.ReferenceBottomVision;
import org.openpnp.machine.reference.vision.ReferenceFiducialLocator;
import org.openpnp.machine.reference.wizards.ReferenceMachineConfigurationWizard;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.FiducialLocator;
import org.openpnp.spi.Head;
import org.openpnp.spi.PartAlignment;
import org.openpnp.spi.PasteDispenseJobProcessor;
import org.openpnp.spi.PnpJobProcessor;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.base.AbstractMachine;
import org.openpnp.spi.base.SimplePropertySheetHolder;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Element;

public class ReferenceMachine extends AbstractMachine {


    @Element(required = false)
    private ReferenceDriver driver = new NullDriver();

    @Element(required = false)
    protected PnpJobProcessor pnpJobProcessor = new ReferencePnpJobProcessor();

    @Element(required = false)
    protected PasteDispenseJobProcessor pasteDispenseJobProcessor = new ReferencePasteDispenseJobProcessor();

    @Element(required = false)
    protected PasteDispenseJobProcessor glueDispenseJobProcessor = new ReferenceGlueDispenseJobProcessor();

    @Element(required = false)
    protected PartAlignment partAlignment = new ReferenceBottomVision();

    @Element(required = false)
    protected FiducialLocator fiducialLocator = new ReferenceFiducialLocator();

    private boolean enabled;

    private List<Class<? extends Feeder>> registeredFeederClasses = new ArrayList<>();

    public ReferenceDriver getDriver() {
        return driver;
    }

    public void setDriver(ReferenceDriver driver) throws Exception {
        if (driver != this.driver) {
            setEnabled(false);
            close();
        }
        this.driver = driver;
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
                driver.setEnabled(true);
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
                driver.setEnabled(false);
                this.enabled = false;
            }
            catch (Exception e) {
                fireMachineDisableFailed(e.getMessage());
                throw e;
            }
            fireMachineDisabled("User requested stop.");
        }
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
        children.add(new SimplePropertySheetHolder("Signalers", getSignalers()));
        children.add(new SimplePropertySheetHolder("Feeders", getFeeders()));
        children.add(new SimplePropertySheetHolder("Heads", getHeads()));
        children.add(new SimplePropertySheetHolder("Cameras", getCameras()));
        children.add(new SimplePropertySheetHolder("Actuators", getActuators()));
        children.add(
                new SimplePropertySheetHolder("Driver", Collections.singletonList(getDriver())));
        children.add(new SimplePropertySheetHolder("Job Processors",
                Arrays.asList(getPnpJobProcessor()/* , getPasteDispenseJobProcessor() */)));

        List<PropertySheetHolder> vision = new ArrayList<>();
        vision.add(getPartAlignment());
        vision.add(getFiducialLocator());
        children.add(new SimplePropertySheetHolder("Vision", vision));
        return children.toArray(new PropertySheetHolder[] {});
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        // TODO Auto-generated method stub
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
    public List<Class<? extends Feeder>> getCompatibleFeederClasses() {
        List<Class<? extends Feeder>> l = new ArrayList<>();
        l.add(ReferenceStripFeeder.class);
        l.add(ReferenceTrayFeeder.class);
        l.add(ReferenceDragFeeder.class);
        l.add(ReferenceTubeFeeder.class);
        l.add(ReferenceAutoFeeder.class);
        l.addAll(registeredFeederClasses);
        return l;
    }

    @Override
    public List<Class<? extends Camera>> getCompatibleCameraClasses() {
        List<Class<? extends Camera>> l = new ArrayList<>();
        l.add(Webcams.class);
        l.add(OpenCvCamera.class);
        l.add(OnvifIPCamera.class);
        l.add(ImageCamera.class);
        l.add(SimulatedUpCamera.class);
        return l;
    }

    @Override
    public void home() throws Exception {
        Logger.debug("home");
        super.home();
    }

    @Override
    public void close() throws IOException {
        try {
            driver.close();
        }
        catch (Exception e) {
            e.printStackTrace();
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
    public PartAlignment getPartAlignment() {
        return partAlignment;
    }

    @Override
    public FiducialLocator getFiducialLocator() {
        return fiducialLocator;
    }

    @Override
    public PnpJobProcessor getPnpJobProcessor() {
        return pnpJobProcessor;
    }

    @Override
    public PasteDispenseJobProcessor getPasteDispenseJobProcessor() {
        return pasteDispenseJobProcessor;
    }

    @Override
    public PasteDispenseJobProcessor getGlueDispenseJobProcessor() {
        return glueDispenseJobProcessor;
    }

}
