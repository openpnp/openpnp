/*
 	Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 	
 	This file is part of OpenPnP.
 	
	OpenPnP is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenPnP is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenPnP.  If not, see <http://www.gnu.org/licenses/>.
 	
 	For more information about OpenPnP visit http://openpnp.org
*/

package org.openpnp.machine.reference.camera.wizards;

import java.awt.Color;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.openpnp.gui.support.AbstractWizard;
import org.openpnp.machine.reference.camera.OpenCvCamera;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class OpenCvCameraConfigurationWizard extends AbstractWizard {
	private final OpenCvCamera camera;

	private JPanel panelGeneral;

	public OpenCvCameraConfigurationWizard(
			OpenCvCamera camera) {
		this.camera = camera;

		panelGeneral = new JPanel();
		contentPanel.add(panelGeneral);
		panelGeneral.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "General", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		panelGeneral.setLayout(new FormLayout(new ColumnSpec[] {
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),},
			new RowSpec[] {
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,}));
		
		JLabel lblDeviceId = new JLabel("Device Index");
		panelGeneral.add(lblDeviceId, "2, 2, right, default");
		
		comboBoxDeviceIndex = new JComboBox();
		for (int i = 0; i < 10; i++) {
			comboBoxDeviceIndex.addItem(new Integer(i));
		}
		panelGeneral.add(comboBoxDeviceIndex, "4, 2, left, default");
	}

	@Override
	public void createBindings() {
		addWrappedBinding(camera, "deviceIndex", comboBoxDeviceIndex, "selectedItem");
	}

	private JComboBox comboBoxDeviceIndex;
}