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

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.wizards.ReferenceActuatorConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.base.AbstractActuator;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

public class ReferenceActuator extends AbstractActuator implements ReferenceHeadMountable {

    @Element
    private Location headOffsets = new Location(LengthUnit.Millimeters);

    @Attribute
    private int index;

    @Deprecated
    @Element(required = false)
    protected Length safeZ = null;

    protected Object lastActuationValue;
    
    public ReferenceActuator() {
    }

    @Override
    public void setHeadOffsets(Location headOffsets) {
        this.headOffsets = headOffsets;
    }

    @Override
    public Location getHeadOffsets() {
        return headOffsets;
    }

    public int getIndex() {
        return index;
    }
    
    public void setIndex(int index) {
        this.index = index;
    }

    @Element(required = false)
    private ReferenceActuatorProfiles actuatorProfiles;

    @Override
    public Object getLastActuationValue() {
        return lastActuationValue;
    }

    @Override
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
    public void setValueType(ActuatorValueType valueType) {
        super.setValueType(valueType);
    }

    public ReferenceActuatorProfiles getActuatorProfiles() {
        if (actuatorProfiles == null && getValueType() == ActuatorValueType.Profile) 
        { actuatorProfiles = new ReferenceActuatorProfiles();
        }
        return actuatorProfiles;
    }

    public void setActuatorProfiles(ReferenceActuatorProfiles actuatorProfiles) {
        this.actuatorProfiles = actuatorProfiles;
        firePropertyChange("actuatorProfiles", null, actuatorProfiles);
        firePropertyChange("profileValues", null, getProfileValues());
    }

    @Override
    protected String getDefaultOnProfile() {
        ReferenceActuatorProfiles.Profile profile = getActuatorProfiles().findProfile(true);
        return (profile != null ? profile.getName() : null);
    }

    @Override
    protected String getDefaultOffProfile() {
        ReferenceActuatorProfiles.Profile profile = getActuatorProfiles().findProfile(false);
        return (profile != null ? profile.getName() : null);
    }

    @Override
    public String[] getProfileValues() {
        if (getValueType() == ActuatorValueType.Profile) {
            return getActuatorProfiles().getProfileNames();
        }
        return new String[] {};
    }

    @Override
    public void actuate(boolean on) throws Exception {
        if (isCoordinatedBeforeActuate()) {
            coordinateWithMachine(false);
        }
        Logger.debug("{}.actuate({})", getName(), on);
        if (getValueType() == ActuatorValueType.Profile) {
            actuateProfile(on);
        }
        else {
            driveActuation(on);
            setLastActuationValue(on);
        }
        if (isCoordinatedAfterActuate()) {
            coordinateWithMachine(true);
        }
        getMachine().fireMachineHeadActivity(head);
    }

    protected void driveActuation(boolean on) throws Exception {
        getDriver().actuate(this, on);
    }

    @Override
    public void actuate(double value) throws Exception {
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

    protected void driveActuation(double value) throws Exception {
        getDriver().actuate(this, value);
    }

    @Override
    public void actuate(String value) throws Exception {
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

    protected void driveActuation(String value) throws Exception {
        getDriver().actuate(this, value);
    }

    @Override
    public void actuateProfile(String name) throws Exception {
        if (getActuatorProfiles() != null) {
            setLastActuationValue(getActuatorProfiles().actuate(this, name));
        }
    }

    @Override
    public void actuateProfile(boolean on) throws Exception {
        if (getActuatorProfiles() != null) {
            setLastActuationValue(getActuatorProfiles().actuate(this, on));
        }
    }

    @Override
    public String read() throws Exception {
        if (isCoordinatedBeforeRead()) {
            coordinateWithMachine(false);
        }
        String value = getDriver().actuatorRead(this);
        Logger.debug("{}.read(): {}", getName(), value);
        if (isCoordinatedAfterActuate()) {
            coordinateWithMachine(true);
        }
        getMachine().fireMachineHeadActivity(head);
        return value;
    }

    @Override
    public String read(double parameter) throws Exception {
        if (isCoordinatedBeforeRead()) {
            coordinateWithMachine(false);
        }
        String value = getDriver().actuatorRead(this, parameter);
        Logger.debug("{}.readWithDouble({}): {}", getName(), parameter, value);
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
        if (getValueType() == ActuatorValueType.Profile) {
            propertySheets.add(new PropertySheetWizardAdapter(getActuatorProfiles().getConfigurationWizard(this), "Profiles"));
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

    public void fireProfilesChanged() {
        firePropertyChange("profileValues", null, getProfileValues());
    }
}
