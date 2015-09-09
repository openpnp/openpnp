/*
 	Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 	
 	This file is package of OpenPnP.
 	
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

package org.openpnp.gui;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.regex.PatternSyntaxException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableRowSorter;

import org.openpnp.gui.components.AutoSelectTextTable;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.components.reticle.PackageReticle;
import org.openpnp.gui.components.reticle.Reticle;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.IdentifiableListCellRenderer;
import org.openpnp.gui.support.IdentifiableTableCellRenderer;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.PackagesComboBoxModel;
import org.openpnp.gui.tablemodel.PackagesTableModel;
import org.openpnp.model.Configuration;
import org.openpnp.model.Package;
import org.openpnp.model.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class PackagesPanel extends JPanel {
	private final static Logger logger = LoggerFactory.getLogger(PackagesPanel.class);
	
	final private Configuration configuration;
	final private Frame frame;
	
	private PackagesTableModel packagesTableModel;
	private TableRowSorter<PackagesTableModel> packagesTableSorter;
	private JTextField searchTextField;
	private JTable packagesTable;

	public PackagesPanel(Configuration configuration, Frame frame) {
		this.configuration = configuration;
		this.frame = frame;
		
		setLayout(new BorderLayout(0, 0));
		packagesTableModel = new PackagesTableModel(configuration);
		packagesTableSorter = new TableRowSorter<PackagesTableModel>(packagesTableModel);

		JPanel panel_5 = new JPanel();
		add(panel_5, BorderLayout.NORTH);
		panel_5.setLayout(new BorderLayout(0, 0));

		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		panel_5.add(toolBar);

		JPanel panel_1 = new JPanel();
		panel_5.add(panel_1, BorderLayout.EAST);

		JLabel lblSearch = new JLabel("Search");
		panel_1.add(lblSearch);

		searchTextField = new JTextField();
		searchTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent e) {
                search();
            }
            
            @Override
            public void insertUpdate(DocumentEvent e) {
                search();
            }
            
            @Override
            public void changedUpdate(DocumentEvent e) {
                search();
            }
        });
		panel_1.add(searchTextField);
		searchTextField.setColumns(15);
		
		JComboBox packagesCombo = new JComboBox(new PackagesComboBoxModel());
		packagesCombo.setRenderer(new IdentifiableListCellRenderer<org.openpnp.model.Package>());

		packagesTable = new AutoSelectTextTable(packagesTableModel);
		packagesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		packagesTable.setDefaultEditor(org.openpnp.model.Package.class, new DefaultCellEditor(packagesCombo));
		packagesTable.setDefaultRenderer(org.openpnp.model.Package.class, new IdentifiableTableCellRenderer<org.openpnp.model.Package>());
		
		add(new JScrollPane(packagesTable), BorderLayout.CENTER);

		packagesTable.setRowSorter(packagesTableSorter);
		
		packagesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) {
					return;
				}
				Package this_package = getSelectedPackage();
				
				deletePackageAction.setEnabled(this_package != null);
				
                CameraView cameraView = MainFrame.cameraPanel.getSelectedCameraView();
                if (cameraView != null) {
                    if (this_package != null) {
                        Reticle reticle = new PackageReticle(this_package);
                        cameraView.setReticle(PackagesPanel.this.getClass().getName(), reticle);
                    }
                    else {
                        MainFrame.cameraPanel.getSelectedCameraView().removeReticle(PackagesPanel.this.getClass().getName());
                    }                                       
                }
			}
		});


        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentHidden(ComponentEvent e) {
                CameraView cameraView = MainFrame.cameraPanel.getSelectedCameraView();
                if (cameraView != null) {
                    cameraView.removeReticle(PackagesPanel.this.getClass().getName());
                }
            }
        });        
		
		deletePackageAction.setEnabled(false);
		
		JButton btnNewPackage = toolBar.add(newPackageAction);
		btnNewPackage.setToolTipText("");
		JButton btnDeletePackage = toolBar.add(deletePackageAction);
		btnDeletePackage.setToolTipText("");
	}
	
	private Package getSelectedPackage() {
		int index = packagesTable.getSelectedRow();
		if (index == -1) {
			return null;
		}
		index = packagesTable.convertRowIndexToModel(index);
		return packagesTableModel.getPackage(index);
	}
	
	private void search() {
		RowFilter<PackagesTableModel, Object> rf = null;
		// If current expression doesn't parse, don't update.
		try {
			rf = RowFilter.regexFilter("(?i)"
					+ searchTextField.getText().trim());
		}
		catch (PatternSyntaxException e) {
			logger.warn("Search failed", e);
			return;
		}
		packagesTableSorter.setRowFilter(rf);
	}

	public final Action newPackageAction = new AbstractAction() {
		{
			putValue(SMALL_ICON, Icons.add);
			putValue(NAME, "New Package...");
			putValue(SHORT_DESCRIPTION, "Create a new package, specifying it's ID.");
		}
		@Override
		public void actionPerformed(ActionEvent arg0) {
			String id;
			while ((id = JOptionPane.showInputDialog(frame, "Please enter an ID for the new package.")) != null) {
				if (configuration.getPackage(id) != null) {
					MessageBoxes.errorBox(frame, "Error", "Package ID " + id + " already exists.");
					continue;
				}
				Package this_package = new Package(id);
				
				configuration.addPackage(this_package);
				packagesTableModel.fireTableDataChanged();
				Helpers.selectLastTableRow(packagesTable);
				break;
			}
		}
	};
	
	public final Action deletePackageAction = new AbstractAction() {
		{
			putValue(SMALL_ICON, Icons.delete);
			putValue(NAME, "Delete Package");
			putValue(SHORT_DESCRIPTION, "Delete the currently selected package.");
		}
		@Override
		public void actionPerformed(ActionEvent arg0) {
            // Check to make sure there are no parts using this package.
            for (Part part : Configuration.get().getParts()) {
                if (part.getPackage() == getSelectedPackage()) {
                    MessageBoxes.errorBox(getTopLevelAncestor(), "Error", getSelectedPackage().getId() + " cannot be deleted. It is used by " + part.getId());
                    return;
                }
            }
            int ret = JOptionPane.showConfirmDialog(
                    getTopLevelAncestor(), 
                    "Are you sure you want to delete " + getSelectedPackage().getId(),
                    "Delete " + getSelectedPackage().getId() + "?",
                    JOptionPane.YES_NO_OPTION);
            if (ret == JOptionPane.YES_OPTION) {
                Configuration.get().removePackage(getSelectedPackage());
            }
		}
	};
}
