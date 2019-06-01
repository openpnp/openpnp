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

import javax.swing.JCheckBox;

import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.machine.reference.signaler.SoundSignaler;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class SoundSignalerConfigurationWizard extends AbstractConfigurationWizard {
    private final SoundSignaler signaler;
    private JCheckBox chckbxError;
    private JCheckBox chckbxSuccess;

    public SoundSignalerConfigurationWizard(SoundSignaler actuator) {
        this.signaler = actuator;
        createUi();
    }
    private void createUi() {
        contentPanel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        chckbxError = new JCheckBox("Play sound on error?");
        contentPanel.add(chckbxError, "2, 2");
        
        chckbxSuccess = new JCheckBox("Play sound on completion?");
        contentPanel.add(chckbxSuccess, "2, 4");
    }

    @Override
    public void createBindings() {
        addWrappedBinding(signaler, "enableErrorSound", chckbxError, "selected");
        addWrappedBinding(signaler, "enableFinishedSound", chckbxSuccess, "selected");
    }
}
