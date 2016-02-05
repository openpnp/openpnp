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
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openpnp.gui.components.AutoSelectTextTable;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.tablemodel.FootprintTableModel;
import org.openpnp.model.Footprint;
import org.openpnp.model.Footprint.Pad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class FootprintPanel extends JPanel {
	private final static Logger logger = LoggerFactory.getLogger(FootprintPanel.class);
	
	private FootprintTableModel tableModel;
	private JTable table;
	
	final private Footprint footprint;

	public FootprintPanel(Footprint footprint) {
	    this.footprint = footprint;
	    
		setLayout(new BorderLayout(0, 0));
		tableModel = new FootprintTableModel(footprint);

		JPanel panel_5 = new JPanel();
		add(panel_5, BorderLayout.NORTH);
		panel_5.setLayout(new BorderLayout(0, 0));

		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		panel_5.add(toolBar);

		table = new AutoSelectTextTable(tableModel);
        table.setAutoCreateRowSorter(true);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }
                
                Pad pad = getSelectedPad();
                
                deleteAction.setEnabled(pad != null);
            }
        });
		
		
		add(new JScrollPane(table), BorderLayout.CENTER);

		deleteAction.setEnabled(false);
		
		JButton btnNew = toolBar.add(newAction);
		JButton btnDelete = toolBar.add(deleteAction);
	}
	
	private Pad getSelectedPad() {
		int index = table.getSelectedRow();
		if (index == -1) {
			return null;
		}
		index = table.convertRowIndexToModel(index);
		return tableModel.getPad(index);
	}
	
	public final Action newAction = new AbstractAction() {
		{
			putValue(SMALL_ICON, Icons.add);
			putValue(NAME, "New Part...");
			putValue(SHORT_DESCRIPTION, "Create a new part, specifying it's ID.");
		}
		@Override
		public void actionPerformed(ActionEvent arg0) {
			String name;
			while ((name = JOptionPane.showInputDialog(getTopLevelAncestor(), "Please enter a name for the new pad.")) != null) {
			    Pad pad = new Pad();
			    pad.setName(name);
				footprint.addPad(pad);
				tableModel.fireTableDataChanged();
				Helpers.selectLastTableRow(table);
				break;
			}
		}
	};
	
	public final Action deleteAction = new AbstractAction() {
		{
			putValue(SMALL_ICON, Icons.delete);
			putValue(NAME, "Delete Part");
			putValue(SHORT_DESCRIPTION, "Delete the currently selected part.");
		}
		@Override
		public void actionPerformed(ActionEvent arg0) {
		    int ret = JOptionPane.showConfirmDialog(
		            getTopLevelAncestor(), 
		            "Are you sure you want to delete " + getSelectedPad().getName(),
		            "Delete " + getSelectedPad().getName() + "?",
		            JOptionPane.YES_NO_OPTION);
		    if (ret == JOptionPane.YES_OPTION) {
	            footprint.removePad(getSelectedPad());
		    }
		}
	};
}
