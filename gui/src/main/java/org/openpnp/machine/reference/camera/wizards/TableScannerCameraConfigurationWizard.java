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
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.openpnp.gui.support.JBindings;
import org.openpnp.gui.support.JBindings.WrappedBinding;
import org.openpnp.gui.support.SaveResetBindingListener;
import org.openpnp.gui.support.Wizard;
import org.openpnp.gui.support.WizardContainer;
import org.openpnp.machine.reference.camera.TableScannerCamera;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

public class TableScannerCameraConfigurationWizard extends JPanel implements Wizard {
	private final TableScannerCamera camera;

	private WizardContainer wizardContainer;
	private JButton btnSave;
	private JButton btnCancel;
	private JPanel panelGeneral;

	private List<WrappedBinding> wrappedBindings = new ArrayList<WrappedBinding>();

	public TableScannerCameraConfigurationWizard(
			TableScannerCamera camera) {
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
				ColumnSpec.decode("default:grow"),
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,},
			new RowSpec[] {
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,}));
		
		lblCacheDirectory = new JLabel("Cache Directory");
		panelGeneral.add(lblCacheDirectory, "2, 2, right, default");
		
		textFieldCacheDirectory = new JTextField();
		panelGeneral.add(textFieldCacheDirectory, "4, 2, fill, default");
		textFieldCacheDirectory.setColumns(10);
		
		btnBrowse = new JButton(browseAction);
		panelGeneral.add(btnBrowse, "6, 2");
		
		lblUrl = new JLabel("URL");
		panelGeneral.add(lblUrl, "2, 4, right, default");
		
		textFieldUrl = new JTextField();
		panelGeneral.add(textFieldUrl, "4, 4, fill, default");
		textFieldUrl.setColumns(10);
		
		btnCheck = new JButton("Check");
		panelGeneral.add(btnCheck, "6, 4");
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
		
		wrappedBindings.add(JBindings.bind(camera, "cacheDirectoryPath",
				textFieldCacheDirectory, "text", listener));
		wrappedBindings.add(JBindings.bind(camera, "sourceUri",
				textFieldUrl, "text", listener));
		
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
	
	private Action browseAction = new AbstractAction("Browse") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			JFileChooser fileDialog = new JFileChooser(textFieldCacheDirectory.getText());
			fileDialog.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			if (fileDialog.showSaveDialog(getTopLevelAncestor()) == JFileChooser.APPROVE_OPTION) {
				File file = fileDialog.getCurrentDirectory();
				if (file != null) {
					textFieldCacheDirectory.setText(file.getAbsolutePath());
				}
			}
		}
	};

	
	private Action saveAction = new AbstractAction("Apply") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			saveToModel();
			wizardContainer
					.wizardCompleted(TableScannerCameraConfigurationWizard.this);
		}
	};

	private Action cancelAction = new AbstractAction("Reset") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			loadFromModel();
		}
	};
	private JLabel lblCacheDirectory;
	private JTextField textFieldCacheDirectory;
	private JButton btnBrowse;
	private JLabel lblUrl;
	private JTextField textFieldUrl;
	private JButton btnCheck;
}