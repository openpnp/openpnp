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

package org.openpnp.machine.reference.feeder.wizards;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.JBindings;
import org.openpnp.gui.support.JBindings.WrappedBinding;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.ApplyResetBindingListener;
import org.openpnp.gui.support.Wizard;
import org.openpnp.gui.support.WizardContainer;
import org.openpnp.machine.reference.feeder.ReferenceTrayFeeder;
import org.openpnp.model.Configuration;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

public class ReferenceTrayFeederConfigurationWizard extends JPanel implements Wizard {
	private final ReferenceTrayFeeder feeder;

	private WizardContainer wizardContainer;
	
	private JTextField textFieldOffsetsX;
	private JTextField textFieldOffsetsY;
	private JTextField textFieldOffsetsZ;
	private JTextField textFielTrayCountX;
	private JTextField textFieldTrayCountY;
	
	private List<WrappedBinding> wrappedBindings = new ArrayList<WrappedBinding>();

	public ReferenceTrayFeederConfigurationWizard(ReferenceTrayFeeder referenceTrayFeeder) {
		feeder = referenceTrayFeeder;
		
		JPanel panelFields = new JPanel();
		
		panelFields.setLayout(new FormLayout(new ColumnSpec[] {
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,},
			new RowSpec[] {
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,}));

		JLabel lblX = new JLabel("X");
		panelFields.add(lblX, "4, 2");

		JLabel lblY = new JLabel("Y");
		panelFields.add(lblY, "6, 2");

		JLabel lblZ = new JLabel("Z");
		panelFields.add(lblZ, "8, 2");

		JLabel lblFeedStartLocation = new JLabel("Offsets");
		panelFields.add(lblFeedStartLocation, "2, 4, right, default");

		textFieldOffsetsX = new JTextField();
		panelFields.add(textFieldOffsetsX, "4, 4, fill, default");
		textFieldOffsetsX.setColumns(10);

		textFieldOffsetsY = new JTextField();
		panelFields.add(textFieldOffsetsY, "6, 4, fill, default");
		textFieldOffsetsY.setColumns(10);

		textFieldOffsetsZ = new JTextField();
		panelFields.add(textFieldOffsetsZ, "8, 4, fill, default");
		textFieldOffsetsZ.setColumns(10);

		JLabel lblTrayCount = new JLabel("Tray Count");
		panelFields.add(lblTrayCount, "2, 6, right, default");

		textFielTrayCountX = new JTextField();
		panelFields.add(textFielTrayCountX, "4, 6, fill, default");
		textFielTrayCountX.setColumns(10);

		textFieldTrayCountY = new JTextField();
		panelFields.add(textFieldTrayCountY, "6, 6, fill, default");
		textFieldTrayCountY.setColumns(10);

		JButton btnSave = new JButton(saveAction);
		setLayout(new BorderLayout(0, 0));
		
		JScrollPane scrollPane = new JScrollPane(panelFields);
		scrollPane.getVerticalScrollBar().setUnitIncrement(Configuration.get().getVerticalScrollUnitIncrement());
		scrollPane.setBorder(null);
		add(scrollPane, BorderLayout.CENTER);
		
		JPanel panelActions = new JPanel();
		panelActions.setLayout(new FlowLayout(FlowLayout.RIGHT));
		
		JButton btnCancel = new JButton(cancelAction);
		panelActions.add(btnCancel);
		
		panelActions.add(btnSave, "8, 26");
		
		add(panelActions, BorderLayout.SOUTH);

		createBindings();
		loadFromModel();
	}
	
	private void createBindings() {
		LengthConverter lengthConverter = new LengthConverter(Configuration.get());
		IntegerConverter integerConverter = new IntegerConverter();
		ApplyResetBindingListener listener = new ApplyResetBindingListener(saveAction, cancelAction);
		
		wrappedBindings.add(JBindings.bind(feeder, "offsets.lengthX", textFieldOffsetsX, "text", lengthConverter, listener));
		wrappedBindings.add(JBindings.bind(feeder, "offsets.lengthY", textFieldOffsetsY, "text", lengthConverter, listener));
		wrappedBindings.add(JBindings.bind(feeder, "offsets.lengthZ", textFieldOffsetsZ, "text", lengthConverter, listener));
		
		wrappedBindings.add(JBindings.bind(feeder, "trayCountX", textFielTrayCountX, "text", integerConverter, listener));
		wrappedBindings.add(JBindings.bind(feeder, "trayCountY", textFieldTrayCountY, "text", integerConverter, listener));
		
		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldOffsetsX);
		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldOffsetsY);
		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldOffsetsZ);
		
		ComponentDecorators.decorateWithAutoSelect(textFielTrayCountX);
		ComponentDecorators.decorateWithAutoSelect(textFieldTrayCountY);
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
			wizardContainer.wizardCompleted(ReferenceTrayFeederConfigurationWizard.this);
		}
	};
	
	private Action cancelAction = new AbstractAction("Reset") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			loadFromModel();
		}
	};
}