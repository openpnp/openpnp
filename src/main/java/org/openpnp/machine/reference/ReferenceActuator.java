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

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.wizards.ReferenceActuatorConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MachineListener;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.base.AbstractActuator;
import org.openpnp.util.UiUtils;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;

public class ReferenceActuator extends AbstractActuator implements ReferenceHeadMountable {

    @Element
    private Location headOffsets = new Location(LengthUnit.Millimeters);

    public enum MachineStateActuation {
        LeaveAsIs,
        AssumeUnknown,
        AssumeActuatedOff,
        AssumeActuatedOn,
        ActuateOff,
        ActuateOn;

        public static MachineStateActuation[] booleanValues() {
            return values();
        }
        public static MachineStateActuation[] otherValues() {
            return new MachineStateActuation[] {
                    LeaveAsIs,
                    AssumeUnknown,
            };
        }
    };

    @Attribute(required = false)
    protected MachineStateActuation enabledActuation = MachineStateActuation.AssumeUnknown;
    @Attribute(required = false)
    protected MachineStateActuation homedActuation = MachineStateActuation.LeaveAsIs;
    @Attribute(required = false)
    protected MachineStateActuation disabledActuation = MachineStateActuation.LeaveAsIs;

    @Attribute
    private int index;

    @ElementList(entry = "value", inline = true, required = false)
    protected List<Object> values;

    @Deprecated
    @Element(required = false)
    protected Length safeZ = null;

    protected Object lastActuationValue;

    public ReferenceActuator() {
        Configuration.get().addListener(new ConfigurationListener.Adapter() {

            @Override
            public void configurationLoaded(Configuration configuration) throws Exception {
                Configuration.get().getMachine().addListener(new MachineListener.Adapter() {
                    @Override
                    public void machineEnabled(Machine machine) {
                        actuateMachineState(machine, getEnabledActuation(), true);
                    }
                    @Override
                    public void machineHomed(Machine machine, boolean isHomed) {
                        if (isHomed) {
                            actuateMachineState(machine, getHomedActuation(), true);
                        }
                    }
                    @Override
                    public void machineAboutToBeDisabled(Machine machine, String reason) {
                        actuateMachineState(machine, getDisabledActuation(), false);
                    }
                });
            }
        });
    }

    @Override
    public void setHeadOffsets(Location headOffsets) {
        this.headOffsets = headOffsets;
    }

    @Override
    public Location getHeadOffsets() {
        return headOffsets;
    }

    public MachineStateActuation getEnabledActuation() {
        return enabledActuation;
    }

    public void setEnabledActuation(MachineStateActuation enabledActuation) {
        this.enabledActuation = enabledActuation;
    }

    public MachineStateActuation getHomedActuation() {
        return homedActuation;
    }

    public void setHomedActuation(MachineStateActuation homedActuation) {
        this.homedActuation = homedActuation;
    }

    public MachineStateActuation getDisabledActuation() {
        return disabledActuation;
    }

    public void setDisabledActuation(MachineStateActuation disabledActuation) {
        this.disabledActuation = disabledActuation;
    }

    public int getIndex() {
        return index;
    }
    
    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public Object getLastActuationValue() {
        return lastActuationValue;
    }

    protected void setLastActuationValue(Object lastActuationValue) {
        Object oldValue = this.lastActuationValue;
        this.lastActuationValue = lastActuationValue;
        firePropertyChange("lastActuationValue", oldValue, lastActuationValue);
        if (oldValue == null || !oldValue.equals(lastActuationValue)) {
            getMachine().fireMachineActuatorActivity(this);
        }
    }

    @Override
    public Location getCameraToolCalibratedOffset(Camera camera) {
        return new Location(camera.getUnitsPerPixel().getUnits());
    }

    @Override
    public void setValueClass(Class<?> valueClass) {
        super.setValueClass(valueClass);
    }

    public void actuateMachineState(Machine machine, MachineStateActuation machineStateActuation, boolean deferred) {
        // Need to execute this now, before the machine is being disabled.
        switch (machineStateActuation) {
            case LeaveAsIs:
                break;
            case AssumeUnknown:
                setLastActuationValue(null);
                break;
            case AssumeActuatedOff:
                setLastActuationValue(false);
                break;
            case AssumeActuatedOn:
                setLastActuationValue(true);
                break;
            case ActuateOff:
                tryActuate(machine, false, deferred);
                break;
            case ActuateOn:
                tryActuate(machine, true, deferred);
                break;
        }
    }

    protected void tryActuate(Machine machine, Object value, boolean deferred) {
        try {
            if (deferred) {
                UiUtils.submitUiMachineTask(() -> {
                    actuate(value);
                });
            }
            else {
                machine.execute(() -> {
                    actuate(value);
                    return true;
                }, true, 0);
            }
        }
        catch (Exception e) {
            MessageBoxes.errorBox(MainFrame.get(), "Error actuating "+getName(), e);
        }
    }

    @Override
    public Object[] getValues() {
        if (Boolean.class.isAssignableFrom(valueClass)) {
            return new Object[] {Boolean.FALSE, Boolean.TRUE};
        }
        if (values == null) {
           return null;
        }
        return values.toArray();
    }

    @Override
    public void actuate(Object value) throws Exception {
        if (isCoordinatedBeforeActuate()) {
            coordinateWithMachine(false);
        }
        Logger.debug("{}.actuate({})", getName(), value);
        driveActuation(value);
        setLastActuationValue(value);
        if (isCoordinatedAfterActuate()) {
            coordinateWithMachine(true);
        }
        getMachine().fireMachineHeadActivity(head);
    }

    protected void driveActuation(Object value) throws Exception {
        getDriver().actuate(this, value);
    }

    @Override
    public Object read(Object value) throws Exception {
        if (isCoordinatedBeforeRead()) {
            coordinateWithMachine(false);
        }
        value = getDriver().actuatorRead(this, value);
        Logger.debug("{}.read(): {}", getName(), value);
        if (isCoordinatedAfterActuate()) {
            coordinateWithMachine(true);
        }
        getMachine().fireMachineHeadActivity(head);
        return value;
    }

    @Override
    public void home() throws Exception {}

    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceActuatorConfigurationWizard(getMachine(), this);
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName() + " " + getName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        return null;
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        ArrayList<PropertySheet> propertySheets = new ArrayList<>();
        propertySheets.add(new PropertySheetWizardAdapter(getConfigurationWizard()));
        if (getInterlockMonitor() != null) {
            propertySheets.add(new PropertySheetWizardAdapter(getInterlockMonitor().getConfigurationWizard(this), "Axis Interlock"));
        }
        return propertySheets.toArray(new PropertySheet[propertySheets.size()]);
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return new Action[] { deleteAction };
    }
    
    public Action deleteAction = new AbstractAction("Delete Actuator") {
        {
            putValue(SMALL_ICON, Icons.delete);
            putValue(NAME, "Delete Actuator");
            putValue(SHORT_DESCRIPTION, "Delete the currently selected actuator.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            int ret = JOptionPane.showConfirmDialog(MainFrame.get(),
                    "Are you sure you want to delete " + getName() + "?",
                    "Delete " + getName() + "?", JOptionPane.YES_NO_OPTION);
            if (ret == JOptionPane.YES_OPTION) {
                if (getHead() != null) {
                    getHead().removeActuator(ReferenceActuator.this);
                }
                else {
                    Configuration.get().getMachine().removeActuator(ReferenceActuator.this);
                }
            }
        }
    };

    @Override
    public String toString() {
        return getName();
    }

    ReferenceMachine getMachine() {
        return (ReferenceMachine) Configuration.get().getMachine();
    }
}
