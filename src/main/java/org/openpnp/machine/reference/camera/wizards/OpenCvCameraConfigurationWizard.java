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

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.machine.reference.camera.OpenCvCamera;
import org.openpnp.machine.reference.wizards.ReferenceCameraConfigurationWizard;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class OpenCvCameraConfigurationWizard extends ReferenceCameraConfigurationWizard {
	private final OpenCvCamera camera;

	private JPanel panelGeneral;

	public OpenCvCameraConfigurationWizard(
			OpenCvCamera camera) {
	    super(camera);
	    
		this.camera = camera;

		panelGeneral = new JPanel();
		contentPanel.add(panelGeneral);
		panelGeneral.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "General", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		panelGeneral.setLayout(new FormLayout(new ColumnSpec[] {
		        FormSpecs.RELATED_GAP_COLSPEC,
		        FormSpecs.DEFAULT_COLSPEC,
		        FormSpecs.RELATED_GAP_COLSPEC,
		        FormSpecs.DEFAULT_COLSPEC,
		        FormSpecs.RELATED_GAP_COLSPEC,
		        FormSpecs.DEFAULT_COLSPEC,},
		    new RowSpec[] {
		        FormSpecs.RELATED_GAP_ROWSPEC,
		        FormSpecs.DEFAULT_ROWSPEC,
		        FormSpecs.RELATED_GAP_ROWSPEC,
		        FormSpecs.DEFAULT_ROWSPEC,
		        FormSpecs.RELATED_GAP_ROWSPEC,
		        FormSpecs.DEFAULT_ROWSPEC,
		        FormSpecs.RELATED_GAP_ROWSPEC,
		        FormSpecs.DEFAULT_ROWSPEC,}));
		
		JLabel lblDeviceId = new JLabel("Device Index");
		panelGeneral.add(lblDeviceId, "2, 2, right, default");
		
		comboBoxDeviceIndex = new JComboBox();
		for (int i = 0; i < 10; i++) {
			comboBoxDeviceIndex.addItem(new Integer(i));
		}
		panelGeneral.add(comboBoxDeviceIndex, "4, 2, left, default");
		
		lblPreferredWidth = new JLabel("Preferred Width");
		panelGeneral.add(lblPreferredWidth, "2, 6, right, default");
		
		textFieldPreferredWidth = new JTextField();
		panelGeneral.add(textFieldPreferredWidth, "4, 6, fill, default");
		textFieldPreferredWidth.setColumns(10);
		
		lbluseFor = new JLabel("(Use 0 for native resolution)");
		panelGeneral.add(lbluseFor, "6, 6");
		
		lblPreferredHeight = new JLabel("Preferred Height");
		panelGeneral.add(lblPreferredHeight, "2, 8, right, default");
		
		textFieldPreferredHeight = new JTextField();
		panelGeneral.add(textFieldPreferredHeight, "4, 8, fill, default");
		textFieldPreferredHeight.setColumns(10);
		
		lbluseFor_1 = new JLabel("(Use 0 for native resolution)");
		panelGeneral.add(lbluseFor_1, "6, 8");
	}

	@Override
	public void createBindings() {
	    IntegerConverter intConverter = new IntegerConverter();
	    super.createBindings();
        addWrappedBinding(camera, "preferredWidth", textFieldPreferredWidth, "text", intConverter);
        addWrappedBinding(camera, "preferredHeight", textFieldPreferredHeight, "text", intConverter);
		// Should always be last so that it doesn't trigger multiple camera reloads.
        addWrappedBinding(camera, "deviceIndex", comboBoxDeviceIndex, "selectedItem");
		
        ComponentDecorators.decorateWithAutoSelect(textFieldPreferredWidth);
        ComponentDecorators.decorateWithAutoSelect(textFieldPreferredHeight);
	}
	
	@Override
    protected void saveToModel() {
        super.saveToModel();
        if (camera.isDirty()) {
            camera.setDeviceIndex(camera.getDeviceIndex());
        }
    }

    private JComboBox comboBoxDeviceIndex;
	private JLabel lblPreferredWidth;
	private JLabel lblPreferredHeight;
	private JTextField textFieldPreferredWidth;
	private JTextField textFieldPreferredHeight;
	private JLabel lbluseFor;
	private JLabel lbluseFor_1;
}