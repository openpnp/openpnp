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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AbstractBindingListener;
import org.jdesktop.beansbinding.Binding;
import org.jdesktop.beansbinding.BindingListener;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.JBindings;
import org.openpnp.gui.support.JBindings.WrappedBinding;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.Wizard;
import org.openpnp.gui.support.WizardContainer;
import org.openpnp.model.LengthUnit;
import org.openpnp.spi.Camera;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import javax.swing.JComboBox;

public class CameraConfigurationWizard extends JPanel implements Wizard {
	private final Camera camera;

	private WizardContainer wizardContainer;

	private JTextField uppX;
	private JTextField uppY;
	private JButton btnSave;
	private JButton btnCancel;
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
	
	private List<WrappedBinding> wrappedBindings = new ArrayList<WrappedBinding>();
	
	public CameraConfigurationWizard(Camera camera) {
		this.camera = camera;

		setLayout(new BorderLayout());

		JPanel panelFields = new JPanel();
		panelFields.setLayout(new BoxLayout(panelFields, BoxLayout.Y_AXIS));

		JScrollPane scrollPane = new JScrollPane(panelFields);
		
		panelLocation = new JPanel();
		panelLocation.setBorder(new TitledBorder(null, "Location / Offsets", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panelFields.add(panelLocation);
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
		panelFields.add(panelUpp);
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
				FormFactory.DEFAULT_ROWSPEC,}));

		JLabel lblX = new JLabel("X");
		panelUpp.add(lblX, "2, 2");

		JLabel lblY = new JLabel("Y");
		panelUpp.add(lblY, "4, 2");

		uppX = new JTextField();
		panelUpp.add(uppX, "2, 4");
		uppX.setColumns(6);

		uppY = new JTextField();
		panelUpp.add(uppY, "4, 4");
		uppY.setColumns(6);
		
		btnMeasure = new JButton("Measure");
		btnMeasure.setAction(measureAction);
		panelUpp.add(btnMeasure, "6, 4");
		
		btnCancelMeasure = new JButton("Cancel");
		btnCancelMeasure.setAction(cancelMeasureAction);
		btnCancelMeasure.setVisible(false);
		panelUpp.add(btnCancelMeasure, "8, 4");
		scrollPane.setBorder(null);
		add(scrollPane, BorderLayout.CENTER);

		JPanel panelActions = new JPanel();
		panelActions.setLayout(new FlowLayout(FlowLayout.RIGHT));
		add(panelActions, BorderLayout.SOUTH);

		btnCancel = new JButton(cancelAction);
		panelActions.add(btnCancel);

		btnSave = new JButton(saveAction);
		panelActions.add(btnSave);

		createBindings();
		loadFromModel();
	}

	private void createBindings() {
		LengthConverter lengthConverter = new LengthConverter("%2.3f%s");
		DoubleConverter doubleConverter = new DoubleConverter("%2.3f");
		IntegerConverter intConverter = new IntegerConverter("%d");
		BindingListener listener = new AbstractBindingListener() {
			@Override
			public void synced(Binding binding) {
				saveAction.setEnabled(true);
				cancelAction.setEnabled(true);
			}
		};

		wrappedBindings.add(JBindings.bind(camera, "unitsPerPixel.lengthX",
				uppX, "text", lengthConverter, listener));
		wrappedBindings.add(JBindings.bind(camera, "unitsPerPixel.lengthY",
				uppY, "text", lengthConverter, listener));
		
		wrappedBindings.add(JBindings.bind(camera, "location.lengthX",
				textFieldLocationX, "text", lengthConverter, listener));
		wrappedBindings.add(JBindings.bind(camera, "location.lengthY",
				textFieldLocationY, "text", lengthConverter, listener));
		wrappedBindings.add(JBindings.bind(camera, "location.lengthZ",
				textFieldLocationZ, "text", lengthConverter, listener));
		wrappedBindings.add(JBindings.bind(camera, "location.rotation",
				textFieldLocationC, "text", doubleConverter, listener));
	}

	private void loadFromModel() {
		for (WrappedBinding wrappedBinding : wrappedBindings) {
			wrappedBinding.reset();
		}
		saveAction.setEnabled(false);
		cancelAction.setEnabled(false);
	}

	private void saveToModel() {
		for (WrappedBinding wrappedBinding : wrappedBindings) {
			wrappedBinding.save();
		}
		saveAction.setEnabled(false);
		cancelAction.setEnabled(false);
	}

	@Override
	public void setWizardContainer(WizardContainer wizardContainer) {
		this.wizardContainer = wizardContainer;
	}

	@Override
	public JPanel getWizardPanel() {
		return this;
	}

	@Override
	public String getWizardName() {
		// TODO Auto-generated method stub
		return null;
	}

	private Action saveAction = new AbstractAction("Apply") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			saveToModel();
			wizardContainer
					.wizardCompleted(CameraConfigurationWizard.this);
		}
	};

	private Action cancelAction = new AbstractAction("Reset") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			loadFromModel();
		}
	};
	
	private Action measureAction = new AbstractAction("Measure") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			btnMeasure.setAction(confirmAction);
			btnCancelMeasure.setVisible(true);
			CameraView cameraView = MainFrame.cameraPanel.setSelectedCamera(camera);
			if (cameraView == null) {
				MessageBoxes.errorBox(CameraConfigurationWizard.this, "Error", "Unable to locate Camera.");
			}
			cameraView.setSelectionEnabled(true);
			cameraView.setSelection(0, 0, 100, 100);
		}
	};
	
	private Action confirmAction = new AbstractAction("Confirm") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			btnMeasure.setAction(measureAction);
			btnCancelMeasure.setVisible(false);
			CameraView cameraView = MainFrame.cameraPanel.getSelectedCameraView();
			if (cameraView == null) {
				MessageBoxes.errorBox(CameraConfigurationWizard.this, "Error", "Unable to locate Camera.");
			}
			cameraView.setSelectionEnabled(false);
			Rectangle selection = cameraView.getSelection();
		}
	};
	
	private Action cancelMeasureAction = new AbstractAction("Cancel") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			btnMeasure.setAction(measureAction);
			btnCancelMeasure.setVisible(false);
			CameraView cameraView = MainFrame.cameraPanel.getSelectedCameraView();
			if (cameraView == null) {
				MessageBoxes.errorBox(CameraConfigurationWizard.this, "Error", "Unable to locate Camera.");
			}
			cameraView.setSelectionEnabled(false);
		}
	};
}