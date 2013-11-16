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

package org.openpnp.gui.wizards;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Camera;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

public class CameraConfigurationWizard extends AbstractConfigurationWizard {
	private final Camera camera;

	private JTextField textFieldUppX;
	private JTextField textFieldUppY;
	private JTextField textFieldOffX;
	private JTextField textFieldOffY;
	private JTextField textFieldOffZ;
	private JPanel panelUpp, panelOff;
	private JButton btnMeasure;
	private JButton btnCancelMeasure;
	
	public CameraConfigurationWizard(Camera camera) {
		this.camera = camera;

		panelUpp = new JPanel();
		contentPanel.add(panelUpp);
		panelUpp.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "Units Per Pixel", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		panelUpp.setLayout(new FormLayout(new ColumnSpec[] {
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,},
			new RowSpec[] {
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,}));

		JLabel lblX = new JLabel("X");
		panelUpp.add(lblX, "2, 2");

		JLabel lblY = new JLabel("Y");
		panelUpp.add(lblY, "4, 2");

		textFieldUppX = new JTextField();
		panelUpp.add(textFieldUppX, "2, 4");
		textFieldUppX.setColumns(8);

		textFieldUppY = new JTextField();
		panelUpp.add(textFieldUppY, "4, 4");
		textFieldUppY.setColumns(8);
		
		btnMeasure = new JButton("Measure");
		btnMeasure.setAction(measureAction);
		panelUpp.add(btnMeasure, "6, 4");

		btnCancelMeasure = new JButton("Cancel");
		btnCancelMeasure.setAction(cancelMeasureAction);
		panelUpp.add(btnCancelMeasure, "8, 4");
		
		lblNewLabel = new JLabel("<html>\n<ol>\n<li>Place an object that is 1 unit by 1 unit square onto the table. Graphing paper is a good, easy choice for this.\n<li>Jog the camera to where it is centered over the object and in focus.\n<li>Use the camera selection rectangle to measure the object and press the Confirm button.\n</ol>\n</html>");
		panelUpp.add(lblNewLabel, "2, 6, 6, 1, default, fill");

		panelOff = new JPanel();
		contentPanel.add(panelOff);
		panelOff.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "Head Offset", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		panelOff.setLayout(new FormLayout(new ColumnSpec[] {
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,},
			new RowSpec[] {
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,}));

		JLabel olblX = new JLabel("X");
		panelOff.add(olblX, "2, 2");

		JLabel olblY = new JLabel("Y");
		panelOff.add(olblY, "4, 2");

		JLabel olblZ = new JLabel("Z");
		panelOff.add(olblZ, "6, 2");
		
		
		textFieldOffX = new JTextField();
		panelOff.add(textFieldOffX, "2, 4");
		textFieldOffX.setColumns(8);

		textFieldOffY = new JTextField();
		panelOff.add(textFieldOffY, "4, 4");
		textFieldOffY.setColumns(8);

		textFieldOffZ = new JTextField();
		panelOff.add(textFieldOffZ, "6, 4");
		textFieldOffZ.setColumns(8);
		
		
	}

	@Override
	public void createBindings() {
		LengthConverter lengthConverter = new LengthConverter();
		
		MutableLocationProxy unitsPerPixel = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, camera, "unitsPerPixel", unitsPerPixel, "location");
        addWrappedBinding(unitsPerPixel, "lengthX", textFieldUppX, "text", lengthConverter);
        addWrappedBinding(unitsPerPixel, "lengthY", textFieldUppY, "text", lengthConverter);
		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldUppX);
		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldUppY);
		

		MutableLocationProxy headOffsets = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, camera, "headOffsets", headOffsets, "location");
        addWrappedBinding(headOffsets, "lengthX", textFieldOffX, "text", lengthConverter);
        addWrappedBinding(headOffsets, "lengthY", textFieldOffY, "text", lengthConverter);
        addWrappedBinding(headOffsets, "lengthZ", textFieldOffZ, "text", lengthConverter);
		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldUppX);
		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldUppY);
		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldUppY);
		
	}
	
	private Action measureAction = new AbstractAction("Measure") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			btnMeasure.setAction(confirmMeasureAction);
			cancelMeasureAction.setEnabled(true);
			CameraView cameraView = MainFrame.cameraPanel.setSelectedCamera(camera);
			if (cameraView == null) {
				MessageBoxes.errorBox(CameraConfigurationWizard.this, "Error", "Unable to locate Camera.");
			}
			cameraView.setSelectionEnabled(true);
			cameraView.setSelection(0, 0, 100, 100);
		}
	};
	
	private Action confirmMeasureAction = new AbstractAction("Confirm") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			btnMeasure.setAction(measureAction);
			cancelMeasureAction.setEnabled(false);
			CameraView cameraView = MainFrame.cameraPanel.getSelectedCameraView();
			if (cameraView == null) {
				MessageBoxes.errorBox(CameraConfigurationWizard.this, "Error", "Unable to locate Camera.");
			}
			cameraView.setSelectionEnabled(false);
			Rectangle selection = cameraView.getSelection();
			textFieldUppX.setText(String.format(Configuration.get().getLengthDisplayFormat(), (1.0 / selection.width)));
			textFieldUppY.setText(String.format(Configuration.get().getLengthDisplayFormat(), (1.0 / selection.height)));
		}
	};
	
	private Action cancelMeasureAction = new AbstractAction("Cancel") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			btnMeasure.setAction(measureAction);
			cancelMeasureAction.setEnabled(false);
			CameraView cameraView = MainFrame.cameraPanel.getSelectedCameraView();
			if (cameraView == null) {
				MessageBoxes.errorBox(CameraConfigurationWizard.this, "Error", "Unable to locate Camera.");
			}
			cameraView.setSelectionEnabled(false);
		}
	};
	private JLabel lblNewLabel;
}