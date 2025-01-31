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

package org.openpnp.machine.reference.signaler.wizards;

import javax.swing.JComboBox;
import javax.swing.JLabel;

import org.openpnp.Translations;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.machine.reference.signaler.ActuatorSignaler;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.base.AbstractJobProcessor;
import org.openpnp.spi.base.AbstractJobProcessor.State;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class ActuatorSignalerConfigurationWizard extends AbstractConfigurationWizard {
    private final ActuatorSignaler signaler;
    private JLabel lblNewLabel;
    private JLabel lblNewLabel_1;
    private JComboBox<Actuator> actuator;
    private JComboBox<State> jobState;

    public ActuatorSignalerConfigurationWizard(ActuatorSignaler actuator) {
        this.signaler = actuator;
        createUi();
    }
    private void createUi() {
        contentPanel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        lblNewLabel = new JLabel(Translations.getString("ActuatorSignalerConfigurationWizard.ActuatorLabel")); //$NON-NLS-1$
        contentPanel.add(lblNewLabel, "2, 2, right, default");
        
        actuator = new JComboBox<Actuator>();
        contentPanel.add(actuator, "4, 2, fill, default");
        
        lblNewLabel_1 = new JLabel(Translations.getString("ActuatorSignalerConfigurationWizard.JobStateLabel")); //$NON-NLS-1$
        contentPanel.add(lblNewLabel_1, "2, 4, right, default");
        
        jobState = new JComboBox<State>();
        contentPanel.add(jobState, "4, 4, fill, default");
        
        for (Actuator actuator : Configuration.get().getMachine().getActuators()) {
            this.actuator.addItem(actuator);
        }
        
        jobState.addItem(null);
        for (AbstractJobProcessor.State state : AbstractJobProcessor.State.values()) {
            this.jobState.addItem(state);
        }
    }

    @Override
    public void createBindings() {
        addWrappedBinding(signaler, "actuator", actuator, "selectedItem");
        addWrappedBinding(signaler, "jobState", jobState, "selectedItem");
    }
}
