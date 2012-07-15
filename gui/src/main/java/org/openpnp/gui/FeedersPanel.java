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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.prefs.Preferences;
import java.util.regex.PatternSyntaxException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableRowSorter;

import org.openpnp.gui.components.ClassSelectionDialog;
import org.openpnp.gui.components.SelectAllTable;
import org.openpnp.gui.support.ActionGroup;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.Wizard;
import org.openpnp.gui.support.WizardContainer;
import org.openpnp.gui.tablemodel.FeedersTableModel;
import org.openpnp.gui.wizards.FeederConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeedersPanel extends JPanel implements WizardContainer {
	private final static Logger logger = LoggerFactory.getLogger(FeedersPanel.class);
	
	private final Configuration configuration;
	private final MachineControlsPanel machineControlsPanel;
	
	private static final String PREF_DIVIDER_POSITION = "FeedersPanel.dividerPosition";
	private static final int PREF_DIVIDER_POSITION_DEF = -1;
	
	private JTable table;

	private FeedersTableModel tableModel;
	private TableRowSorter<FeedersTableModel> tableSorter;
	private JTextField searchTextField;
	private JPanel generalConfigPanel;
	private JPanel feederSpecificConfigPanel;
	
	private ActionGroup feederSelectedActionGroup; 

	private Preferences prefs = Preferences.userNodeForPackage(FeedersPanel.class);
	
	public FeedersPanel(Configuration configuration, MachineControlsPanel machineControlsPanel) {
		this.configuration = configuration;
		this.machineControlsPanel = machineControlsPanel;
		
		setLayout(new BorderLayout(0, 0));
		tableModel = new FeedersTableModel(configuration);

		JPanel panel = new JPanel();
		add(panel, BorderLayout.NORTH);
		panel.setLayout(new BorderLayout(0, 0));

		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		panel.add(toolBar, BorderLayout.CENTER);
		
		JButton btnNewFeeder = new JButton(newFeederAction);
		toolBar.add(btnNewFeeder);
		
		JButton btnDeleteFeeder = new JButton(deleteFeederAction);
		toolBar.add(btnDeleteFeeder);
		
		toolBar.addSeparator();
		toolBar.add(feedFeederAction);
		
		JPanel panel_1 = new JPanel();
		panel.add(panel_1, BorderLayout.EAST);

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
		table = new SelectAllTable(tableModel);
		tableSorter = new TableRowSorter<FeedersTableModel>(tableModel);
		
		final JSplitPane splitPane = new JSplitPane();
		splitPane.setContinuousLayout(true);
		splitPane.setDividerLocation(prefs.getInt(PREF_DIVIDER_POSITION, PREF_DIVIDER_POSITION_DEF));
		splitPane.addPropertyChangeListener("dividerLocation",
				new PropertyChangeListener() {
					@Override
					public void propertyChange(PropertyChangeEvent evt) {
						prefs.putInt(PREF_DIVIDER_POSITION,
								splitPane.getDividerLocation());
					}
				});
		add(splitPane, BorderLayout.CENTER);
		
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		
		generalConfigPanel = new JPanel();
		tabbedPane.addTab("General Configuration", null, generalConfigPanel, null);
		generalConfigPanel.setLayout(new BorderLayout(0, 0));
		
		feederSpecificConfigPanel = new JPanel();
		tabbedPane.addTab("Feeder Specific", null, feederSpecificConfigPanel, null);
		feederSpecificConfigPanel.setLayout(new BorderLayout(0, 0));
		
		
		splitPane.setLeftComponent(new JScrollPane(table));
		splitPane.setRightComponent(tabbedPane);
		table.setRowSorter(tableSorter);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		feederSelectedActionGroup = new ActionGroup(deleteFeederAction, feedFeederAction);
		
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) {
					return;
				}
				
				Feeder feeder = getSelectedFeeder();
				
				feederSelectedActionGroup.setEnabled(feeder != null);
				
				generalConfigPanel.removeAll();
				feederSpecificConfigPanel.removeAll();
				if (feeder != null) {
					Wizard generalConfigWizard = new FeederConfigurationWizard(feeder, 
							FeedersPanel.this.configuration, 
							FeedersPanel.this.machineControlsPanel);
					if (generalConfigWizard != null) {
						generalConfigWizard.setWizardContainer(FeedersPanel.this);
						JPanel panel = generalConfigWizard.getWizardPanel();
						generalConfigPanel.add(panel);
					}
					Wizard wizard = feeder.getConfigurationWizard();
					if (wizard != null) {
						wizard.setWizardContainer(FeedersPanel.this);
						JPanel panel = wizard.getWizardPanel();
						feederSpecificConfigPanel.add(panel);
					}
				}
				revalidate();
				repaint();
			}
		});
		
		feederSelectedActionGroup.setEnabled(false);
	}
	
	private Feeder getSelectedFeeder() {
		int index = table.getSelectedRow();
		
		if (index == -1) {
			return null;
		}
		
		index = table.convertRowIndexToModel(index);
		return tableModel.getFeeder(index);
	}

	private void search() {
		RowFilter<FeedersTableModel, Object> rf = null;
		// If current expression doesn't parse, don't update.
		try {
			rf = RowFilter.regexFilter("(?i)" + searchTextField.getText().trim());
		}
		catch (PatternSyntaxException e) {
			logger.warn("Search failed", e);
			return;
		}
		tableSorter.setRowFilter(rf);
	}

	@Override
	public void wizardCompleted(Wizard wizard) {
		configuration.setDirty(true);
	}

	@Override
	public void wizardCancelled(Wizard wizard) {
	}
	
	@Override
	public Configuration getConfiguration() {
		return configuration;
	}

	@Override
	public MachineControlsPanel getMachineControlsPanel() {
		return machineControlsPanel;
	}
	

	public Action newFeederAction = new AbstractAction("New Feeder...") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			ClassSelectionDialog<Feeder> dialog = new ClassSelectionDialog<Feeder>(
					JOptionPane.getFrameForComponent(FeedersPanel.this), 
					"Select Feeder...", 
					"Please select a Feeder implemention from the list below.", 
					configuration.getMachine().getCompatibleFeederClasses());
			dialog.setVisible(true);
			Class<? extends Feeder> feederClass = dialog.getSelectedClass();
			if (feederClass == null) {
				return;
			}
			try {
				Feeder feeder = feederClass.newInstance();
				configuration.resolve(feeder);
				configuration.getMachine().addFeeder(feeder);
				tableModel.refresh();
				configuration.setDirty(true);
			}
			catch (Exception e) {
				MessageBoxes.errorBox(
						JOptionPane.getFrameForComponent(FeedersPanel.this), 
						"Feeder Error", 
						e);
			}
		}
	};

	public Action deleteFeederAction = new AbstractAction("Delete Feeder") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			configuration.getMachine().removeFeeder(getSelectedFeeder());
		}
	};
	

	public Action feedFeederAction = new AbstractAction("Feed") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			new Thread() {
				public void run() {
					Feeder feeder = getSelectedFeeder();
					Head head = configuration.getMachine().getHeads().get(0);
					try {
						feeder.feed(head, feeder.getLocation());
					}
					catch (Exception e) {
						MessageBoxes.errorBox(FeedersPanel.this, "Feed Error", e);
					}
				}
			}.start();
		}
	};
}