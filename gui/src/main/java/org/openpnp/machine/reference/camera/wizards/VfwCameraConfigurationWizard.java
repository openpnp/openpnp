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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.openpnp.gui.support.JBindings;
import org.openpnp.gui.support.JBindings.WrappedBinding;
import org.openpnp.gui.support.SaveResetBindingListener;
import org.openpnp.gui.support.Wizard;
import org.openpnp.gui.support.WizardContainer;
import org.openpnp.machine.reference.camera.VfwCamera;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

public class VfwCameraConfigurationWizard extends JPanel implements Wizard {
	private final VfwCamera camera;

	private WizardContainer wizardContainer;
	private JButton btnSave;
	private JButton btnCancel;
	private JPanel panelGeneral;

	private List<WrappedBinding> wrappedBindings = new ArrayList<WrappedBinding>();

	public VfwCameraConfigurationWizard(
			VfwCamera camera) {
		this.camera = camera;

		setLayout(new BorderLayout());

		JPanel panelFields = new JPanel();
		panelFields.setLayout(new BoxLayout(panelFields, BoxLayout.Y_AXIS));

		JScrollPane scrollPane = new JScrollPane(panelFields);

		panelGeneral = new JPanel();
		panelFields.add(panelGeneral);
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
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,}));
		
		JLabel lblDeviceId = new JLabel("Driver");
		panelGeneral.add(lblDeviceId, "2, 2, right, default");
		
		Object[] deviceIds = null;
		try {
			deviceIds = camera.getDrivers().toArray(new String[] {});
		}
		catch (Exception e) {
			// TODO:
		}
		comboBoxDriver = new JComboBox(deviceIds);
		panelGeneral.add(comboBoxDriver, "4, 2, left, default");
		
		chckbxShowVideoSource = new JCheckBox("Show Video Source Dialog?");
		panelGeneral.add(chckbxShowVideoSource, "2, 4, 3, 1");
		
		chckbxShowVideoFormat = new JCheckBox("Show Video Format Dialog?");
		panelGeneral.add(chckbxShowVideoFormat, "2, 6, 3, 1");
		
		chckbxShowVideoDisplay = new JCheckBox("Show Video Display Dialog?");
		panelGeneral.add(chckbxShowVideoDisplay, "2, 8, 3, 1");
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
		SaveResetBindingListener listener = new SaveResetBindingListener(saveAction, cancelAction);
		
		// The order of the properties is important. We want all the booleans
		// to be set before we set the driver because setting the driver
		// applies all the settings.
		wrappedBindings.add(JBindings.bind(camera, "showVideoSourceDialog",
				chckbxShowVideoSource, "selected", listener));
		wrappedBindings.add(JBindings.bind(camera, "showVideoFormatDialog",
				chckbxShowVideoFormat, "selected", listener));
		wrappedBindings.add(JBindings.bind(camera, "showVideoDisplayDialog",
				chckbxShowVideoDisplay, "selected", listener));
		wrappedBindings.add(JBindings.bind(camera, "driver",
				comboBoxDriver, "selectedItem", listener));
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
					.wizardCompleted(VfwCameraConfigurationWizard.this);
		}
	};

	private Action cancelAction = new AbstractAction("Reset") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			loadFromModel();
		}
	};
	private JComboBox comboBoxDriver;
	private JCheckBox chckbxShowVideoSource;
	private JCheckBox chckbxShowVideoFormat;
	private JCheckBox chckbxShowVideoDisplay;
}