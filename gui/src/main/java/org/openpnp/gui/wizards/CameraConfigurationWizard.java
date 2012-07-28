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

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MessageBoxes;
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
	private JPanel panelUpp;
	private JPanel panelLocation;
	private JLabel lblX_1;
	private JLabel lblY_1;
	private JLabel lblZ;
	private JLabel lblRotation;
	private JTextField textFieldLocationX;
	private JTextField textFieldLocationY;
	private JTextField textFieldLocationZ;
	private JTextField textFieldLocationC;
	private JButton btnMeasure;
	private JButton btnCancelMeasure;
	
	public CameraConfigurationWizard(Camera camera) {
		this.camera = camera;

		
		panelLocation = new JPanel();
		panelLocation.setBorder(new TitledBorder(null, "Location / Offsets", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		contentPanel.add(panelLocation);
		panelLocation.setLayout(new FormLayout(new ColumnSpec[] {
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),},
			new RowSpec[] {
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,}));
		
		lblX_1 = new JLabel("X");
		panelLocation.add(lblX_1, "2, 2");
		
		lblY_1 = new JLabel("Y");
		panelLocation.add(lblY_1, "4, 2");
		
		lblZ = new JLabel("Z");
		panelLocation.add(lblZ, "6, 2");
		
		lblRotation = new JLabel("Rotation");
		panelLocation.add(lblRotation, "8, 2");
		
		textFieldLocationX = new JTextField();
		panelLocation.add(textFieldLocationX, "2, 4");
		textFieldLocationX.setColumns(6);
		
		textFieldLocationY = new JTextField();
		panelLocation.add(textFieldLocationY, "4, 4");
		textFieldLocationY.setColumns(6);
		
		textFieldLocationZ = new JTextField();
		panelLocation.add(textFieldLocationZ, "6, 4");
		textFieldLocationZ.setColumns(6);
		
		textFieldLocationC = new JTextField();
		panelLocation.add(textFieldLocationC, "8, 4");
		textFieldLocationC.setColumns(6);

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
	}

	@Override
	public void createBindings() {
		LengthConverter lengthConverter = new LengthConverter(Configuration.get());
		DoubleConverter doubleConverter = new DoubleConverter(Configuration.get().getLengthDisplayFormat());
		
		addWrappedBinding(camera, "unitsPerPixel.lengthX", textFieldUppX, "text", lengthConverter);
		addWrappedBinding(camera, "unitsPerPixel.lengthY", textFieldUppY, "text", lengthConverter);
		
		addWrappedBinding(camera.getLocation(), "lengthX", textFieldLocationX, "text", lengthConverter);
		addWrappedBinding(camera.getLocation(), "lengthY", textFieldLocationY, "text", lengthConverter);
		addWrappedBinding(camera.getLocation(), "lengthZ", textFieldLocationZ, "text", lengthConverter);
		addWrappedBinding(camera, "location.rotation", textFieldLocationC, "text", doubleConverter);
		
		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldUppX);
		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldUppY);

		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldLocationX);
		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldLocationY);
		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldLocationZ);
		
		ComponentDecorators.decorateWithAutoSelect(textFieldLocationC);
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