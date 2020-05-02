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

package org.openpnp.machine.reference.wizards;

import org.openpnp.gui.wizards.AbstractTransformedAxisConfigurationWizard;
import org.openpnp.machine.reference.ReferenceNegatedAxis;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Axis;
import org.openpnp.spi.base.AbstractAxis;
import org.openpnp.spi.base.AbstractMachine;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

@SuppressWarnings("serial")
public class ReferenceNegatedAxisConfigurationWizard extends AbstractTransformedAxisConfigurationWizard {
    protected final ReferenceNegatedAxis axis;

    public ReferenceNegatedAxisConfigurationWizard(ReferenceNegatedAxis axis) {
        super((AbstractMachine) Configuration.get().getMachine());
        this.axis = axis;
        
        type.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                adaptDialog();
            }
        });
    }

    private void adaptDialog() {
        Axis.Type selectedType = (Axis.Type) type.getSelectedItem();
        
        lblInputAxisX.setVisible(selectedType == Axis.Type.X);
        inputAxisX.setVisible(selectedType == Axis.Type.X);
        
        lblInputAxisY.setVisible(selectedType == Axis.Type.Y);
        inputAxisY.setVisible(selectedType == Axis.Type.Y);
        
        lblInputAxisZ.setVisible(selectedType == Axis.Type.Z);
        inputAxisZ.setVisible(selectedType == Axis.Type.Z);
        
        lblInputAxisRotation.setVisible(selectedType == Axis.Type.Rotation);
        inputAxisRotation.setVisible(selectedType == Axis.Type.Rotation);
    }

    @Override
    public void createBindings() {
        super.createBindings();
    }


    @Override
    protected AbstractAxis getAxis() {
        return axis;
    }

    @Override
    protected String getTransformationSettingsTitle() {
        return "Negated Axis";
    }
}
