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

    @Element(required = false)
    protected Length safeZ = new Length(0, LengthUnit.Millimeters);

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

    @Override
    public void actuate(boolean on) throws Exception {
        Logger.debug("{}.actuate({})", getName(), on);
        getDriver().actuate(this, on);
        getMachine().fireMachineHeadActivity(head);
    }

    @Override
    public Location getLocation() {
        return getDriver().getLocation(this);
    }

    @Override
    public void actuate(double value) throws Exception {
        Logger.debug("{}.actuate({})", getName(), value);
        getDriver().actuate(this, value);
        getMachine().fireMachineHeadActivity(head);
    }
    
    @Override
    public String read() throws Exception {
        String value = getDriver().actuatorRead(this);
        Logger.debug("{}.read(): {}", getName(), value);
        getMachine().fireMachineHeadActivity(head);
        return value;
    }

    @Override
    public void moveTo(Location location, double speed) throws Exception {
        Logger.debug("{}.moveTo({}, {})", getName(), location, speed);
        getDriver().moveTo(this, location, getHead().getMaxPartSpeed() * speed);
        getMachine().fireMachineHeadActivity(head);
    }

    @Override
    public void moveToSafeZ(double speed) throws Exception {
        Logger.debug("{}.moveToSafeZ({})", getName(), speed);
        Length safeZ = this.safeZ.convertToUnits(getLocation().getUnits());
        Location l = new Location(getLocation().getUnits(), Double.NaN, Double.NaN,
                safeZ.getValue(), Double.NaN);
        getDriver().moveTo(this, l, getHead().getMaxPartSpeed() * speed);
        getMachine().fireMachineHeadActivity(head);
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceActuatorConfigurationWizard(this);
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
        return new PropertySheet[] {new PropertySheetWizardAdapter(getConfigurationWizard())};
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

    public Length getSafeZ() {
        return safeZ;
    }

    public void setSafeZ(Length safeZ) {
        this.safeZ = safeZ;
    }
    
    ReferenceDriver getDriver() {
        return getMachine().getDriver();
    }
    
    ReferenceMachine getMachine() {
        return (ReferenceMachine) Configuration.get().getMachine();
    }
}
