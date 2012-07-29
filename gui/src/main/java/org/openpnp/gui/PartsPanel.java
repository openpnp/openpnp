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

package org.openpnp.gui;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.regex.PatternSyntaxException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableRowSorter;

import org.openpnp.gui.components.AutoSelectTextTable;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.tablemodel.PartsTableModel;
import org.openpnp.model.Configuration;
import org.openpnp.model.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class PartsPanel extends JPanel {
	private final static Logger logger = LoggerFactory.getLogger(PartsPanel.class);
	
	final private Configuration configuration;
	final private Frame frame;
	
	private PartsTableModel partsTableModel;
	private TableRowSorter<PartsTableModel> partsTableSorter;
	private JTextField searchTextField;
	private JTable partsTable;

	public PartsPanel(Configuration configuration, Frame frame) {
		this.configuration = configuration;
		this.frame = frame;
		
		setLayout(new BorderLayout(0, 0));
		partsTableModel = new PartsTableModel(configuration);
		partsTableSorter = new TableRowSorter<PartsTableModel>(partsTableModel);

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
		searchTextField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent arg0) {
				search();
			}
		});
		panel_1.add(searchTextField);
		searchTextField.setColumns(15);

		partsTable = new AutoSelectTextTable(partsTableModel);
		partsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		add(new JScrollPane(partsTable), BorderLayout.CENTER);

		partsTable.setRowSorter(partsTableSorter);
		
		partsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) {
					return;
				}
				Part part = getSelectedPart();
				
				deletePartAction.setEnabled(part != null);
			}
		});
		
		
		deletePartAction.setEnabled(false);
		
		JButton btnNewPart = toolBar.add(newPartAction);
		btnNewPart.setToolTipText("");
		JButton btnDeletePart = toolBar.add(deletePartAction);
		btnDeletePart.setToolTipText("");
	}
	
	private Part getSelectedPart() {
		int index = partsTable.getSelectedRow();
		if (index == -1) {
			return null;
		}
		index = partsTable.convertRowIndexToModel(index);
		return partsTableModel.getPart(index);
	}
	
	private void search() {
		RowFilter<PartsTableModel, Object> rf = null;
		// If current expression doesn't parse, don't update.
		try {
			rf = RowFilter.regexFilter("(?i)"
					+ searchTextField.getText().trim());
		}
		catch (PatternSyntaxException e) {
			logger.warn("Search failed", e);
			return;
		}
		partsTableSorter.setRowFilter(rf);
	}

	public Action newPartAction = new AbstractAction() {
		{
			putValue(SMALL_ICON, new ImageIcon(getClass().getResource("/icons/new.png")));
			putValue(NAME, "New Part...");
			putValue(SHORT_DESCRIPTION, "Create a new part, specifying it's ID.");
		}
		@Override
		public void actionPerformed(ActionEvent arg0) {
			String id = JOptionPane.showInputDialog(frame, "Please enter an ID for the new part.");
			if (id == null) {
				return;
			}
			if (configuration.getPart(id) != null) {
				MessageBoxes.errorBox(frame, "Error", "Part ID " + id + " already exists.");
				return;
			}
			Part part = new Part();
			part.setId(id);
			configuration.addPart(part);
			partsTableModel.fireTableDataChanged();
		}
	};
	
	public Action deletePartAction = new AbstractAction() {
		{
			putValue(SMALL_ICON, new ImageIcon(getClass().getResource("/icons/delete.png")));
			putValue(NAME, "Delete Part");
			putValue(SHORT_DESCRIPTION, "Delete the currently selected part.");
		}
		@Override
		public void actionPerformed(ActionEvent arg0) {
		}
	};
}
