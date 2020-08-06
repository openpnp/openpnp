/* Copyright (C) 2020 <mark@makr.zone>
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

 package org.openpnp.spi.base;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JOptionPane;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Axis;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Movable.LocationOption;
import org.openpnp.spi.PropertySheetHolder;
import org.simpleframework.xml.Attribute;

/**
 * The Abstract Axis implements the common denominator of all the sub-classes. 
 *
 */
public abstract class AbstractAxis extends AbstractModelObject implements Axis {

    @Attribute
    protected String id;

    @Attribute(required = false)
    protected String name;

    @Attribute(required = false)
    protected Axis.Type type;

    public AbstractAxis() {
        this.id = Configuration.createId("AXS");
        this.name = getClass().getSimpleName();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        Object oldValue = this.name;
        this.name = name;
        firePropertyChange("name", oldValue, name);
    }

    @Override
    public Axis.Type getType() {
        return type;
    }

    @Override
    public void setType(Axis.Type type) {
        this.type = type;
    }

    /**
     * @param machine The Machine with the axes to be considered.
     * @return The set of ControllerAxes that are the ultimate input axes of the axis stack. 
     */
    public abstract AxesLocation getCoordinateAxes(Machine machine);
    public static AxesLocation getCoordinateAxes(AbstractAxis axis, Machine machine) {
        if (axis != null) {
            return axis.getCoordinateAxes(machine);
        }
        return AxesLocation.zero;
    }

    /**
     * Transform the raw axis coordinate(s) taken from the specified location into the 
     * transformed coordinate embedded into the returned location. 
     * The transformed location is what the user sees, while the raw location is what the
     * motion controller sees.
     * Some transformations handle multiple axes, therefore the full location is passed through.
     * A ControllerAxis will just return the unchanged coordinate. 
     * 
     * @param location
     * @return the transformed axis coordinate.  
     * @throws Exception 
     */
    public abstract AxesLocation toTransformed(AxesLocation location, LocationOption... options); 

    /**
     * Transform the specified transformed coordinate taken from the specified location into the 
     * raw coordinate embedded into the returned location. 
     * The transformed location is what the user sees, while the raw location is what the
     * motion controller sees.
     * Some transformations handle multiple axes, therefore the full Location is passed through.
     * 
     * A ControllerAxis will just return the unchanged coordinate. 
     *        
     * @param location
     * @return the raw axis coordinate in the LengthUnit of the given Location.  
     */
    public abstract AxesLocation toRaw(AxesLocation location, LocationOption... options) throws Exception; 

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {
                new PropertySheetWizardAdapter(getConfigurationWizard()),
        };
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return new Action[] { deleteAction, permutateUpAction, permutateDownAction };
    }

    @SuppressWarnings("serial")
    public Action deleteAction = new AbstractAction("Delete Axis") {
        {
            putValue(SMALL_ICON, Icons.delete);
            putValue(NAME, "Delete Axis");
            putValue(SHORT_DESCRIPTION, "Delete the currently selected axis.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            int ret = JOptionPane.showConfirmDialog(MainFrame.get(),
                    "Are you sure you want to delete " + getName() + "?",
                    "Delete " + getName() + "?", JOptionPane.YES_NO_OPTION);
            if (ret == JOptionPane.YES_OPTION) {
                Configuration.get().getMachine().removeAxis(AbstractAxis.this);
            }
        }
    };

    @SuppressWarnings("serial")
    public Action permutateUpAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.arrowUp);
            putValue(NAME, "Permutate Up");
            putValue(SHORT_DESCRIPTION, "Move the currently selected axis one position up.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            Configuration.get().getMachine().permutateAxis(AbstractAxis.this, -1);
        }
    };

    @SuppressWarnings("serial")
    public Action permutateDownAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.arrowDown);
            putValue(NAME, "Permutate Down");
            putValue(SHORT_DESCRIPTION, "Move the currently selected axis one position down.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            Configuration.get().getMachine().permutateAxis(AbstractAxis.this, +1);
        }
    };

    @Override
    public Icon getPropertySheetHolderIcon() {
        if (type == Axis.Type.Rotation) {
            return Icons.axisRotation;
        }
        else {
            return Icons.axisCartesian;
        }
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        return null;
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName() + " " + getName();
    }

}
