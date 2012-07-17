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
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;
import org.openpnp.gui.MachineControlsPanel;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.JBindings;
import org.openpnp.gui.support.JBindings.WrappedBinding;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.PartConverter;
import org.openpnp.gui.support.SaveResetBindingListener;
import org.openpnp.gui.support.Wizard;
import org.openpnp.gui.support.WizardContainer;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Feeder;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import org.openpnp.gui.components.LocationButtonsPanel;

public class FeederConfigurationWizard extends JPanel implements Wizard {
	private final Feeder feeder;
	private final Configuration configuration;
	private final MachineControlsPanel machineControlsPanel;

	private WizardContainer wizardContainer;
	private JButton btnSave;
	private JButton btnCancel;

	private JPanel panelLocation;
	private JLabel lblX_1;
	private JLabel lblY_1;
	private JLabel lblZ;
	private JLabel lblRotation;
	private JTextField textFieldLocationX;
	private JTextField textFieldLocationY;
	private JTextField textFieldLocationZ;
	private JTextField textFieldLocationC;
	private JPanel panelPart;

	private List<WrappedBinding> wrappedBindings = new ArrayList<WrappedBinding>();

	public FeederConfigurationWizard(Feeder feeder,
			Configuration configuration,
			MachineControlsPanel machineControlsPanel) {
		this.feeder = feeder;
		this.configuration = configuration;
		this.machineControlsPanel = machineControlsPanel;

		setLayout(new BorderLayout());

		JPanel panelFields = new JPanel();
		panelFields.setLayout(new BoxLayout(panelFields, BoxLayout.Y_AXIS));

		JScrollPane scrollPane = new JScrollPane(panelFields);

		panelPart = new JPanel();
		panelPart.setBorder(new TitledBorder(null, "Part",
				TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panelFields.add(panelPart);
		panelPart
				.setLayout(new FormLayout(new ColumnSpec[] {
						FormFactory.RELATED_GAP_COLSPEC,
						ColumnSpec.decode("default:grow"),
						FormFactory.RELATED_GAP_COLSPEC,
						ColumnSpec.decode("default:grow"), }, new RowSpec[] {
						FormFactory.RELATED_GAP_ROWSPEC,
						FormFactory.DEFAULT_ROWSPEC, }));

		Vector<String> partIds = new Vector<String>();
		for (Part part : configuration.getParts()) {
			partIds.add(part.getId());
		}
		comboBoxPart = new JComboBox(partIds);
		AutoCompleteDecorator.decorate(comboBoxPart);
		panelPart.add(comboBoxPart, "2, 2, left, default");

		panelLocation = new JPanel();
		panelLocation.setBorder(new TitledBorder(new EtchedBorder(
				EtchedBorder.LOWERED, null, null), "Pickup Location",
				TitledBorder.LEADING, TitledBorder.TOP, null,
				new Color(0, 0, 0)));
		panelFields.add(panelLocation);
		panelLocation.setLayout(new FormLayout(new ColumnSpec[] {
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("left:default:grow"),},
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
		textFieldLocationX.setColumns(8);

		textFieldLocationY = new JTextField();
		panelLocation.add(textFieldLocationY, "4, 4");
		textFieldLocationY.setColumns(8);

		textFieldLocationZ = new JTextField();
		panelLocation.add(textFieldLocationZ, "6, 4");
		textFieldLocationZ.setColumns(8);

		textFieldLocationC = new JTextField();
		panelLocation.add(textFieldLocationC, "8, 4");
		textFieldLocationC.setColumns(8);
		
		locationButtonsPanel = new LocationButtonsPanel(textFieldLocationX, textFieldLocationY, textFieldLocationZ, textFieldLocationC);
		panelLocation.add(locationButtonsPanel, "10, 4");
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
		LengthConverter lengthConverter = new LengthConverter(configuration);
		DoubleConverter doubleConverter = new DoubleConverter(
				configuration.getLengthDisplayFormat());
		PartConverter partConverter = new PartConverter(configuration);
		SaveResetBindingListener listener = new SaveResetBindingListener(
				saveAction, cancelAction);

		wrappedBindings.add(JBindings.bind(feeder, "location.lengthX",
				textFieldLocationX, "text", lengthConverter, listener));
		wrappedBindings.add(JBindings.bind(feeder, "location.lengthY",
				textFieldLocationY, "text", lengthConverter, listener));
		wrappedBindings.add(JBindings.bind(feeder, "location.lengthZ",
				textFieldLocationZ, "text", lengthConverter, listener));
		wrappedBindings.add(JBindings.bind(feeder, "location.rotation",
				textFieldLocationC, "text", doubleConverter, listener));
		wrappedBindings.add(JBindings.bind(feeder, "part", comboBoxPart,
				"selectedItem", partConverter, listener));

		ComponentDecorators
				.decorateWithAutoSelectAndLengthConversion(textFieldLocationX);
		ComponentDecorators
				.decorateWithAutoSelectAndLengthConversion(textFieldLocationY);
		ComponentDecorators
				.decorateWithAutoSelectAndLengthConversion(textFieldLocationZ);
		ComponentDecorators.decorateWithAutoSelect(textFieldLocationC);
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
			wizardContainer.wizardCompleted(FeederConfigurationWizard.this);
		}
	};

	private Action cancelAction = new AbstractAction("Reset") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			loadFromModel();
		}
	};

	private JComboBox comboBoxPart;
	private LocationButtonsPanel locationButtonsPanel;
}