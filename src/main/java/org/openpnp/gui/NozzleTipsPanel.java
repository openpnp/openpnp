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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.prefs.Preferences;
import java.util.regex.PatternSyntaxException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
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

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.components.AutoSelectTextTable;
import org.openpnp.gui.support.HeadCellValue;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.Wizard;
import org.openpnp.gui.support.WizardContainer;
import org.openpnp.gui.tablemodel.NozzleTipsTableModel;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Head;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class NozzleTipsPanel extends JPanel implements WizardContainer {
	private final static Logger logger = LoggerFactory.getLogger(NozzleTipsPanel.class);

	private static final String PREF_DIVIDER_POSITION = "NozzleTipsPanel.dividerPosition";
	private static final int PREF_DIVIDER_POSITION_DEF = -1;
	
	private JTable table;

	private NozzleTipsTableModel tableModel;
	private TableRowSorter<NozzleTipsTableModel> tableSorter;
	private JTextField searchTextField;
	private JComboBox headsComboBox;

	private Preferences prefs = Preferences.userNodeForPackage(NozzleTipsPanel.class);

	public NozzleTipsPanel() {		
		setLayout(new BorderLayout(0, 0));
		tableModel = new NozzleTipsTableModel(Configuration.get());

		JPanel panel = new JPanel();
		add(panel, BorderLayout.NORTH);
		panel.setLayout(new BorderLayout(0, 0));

		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		panel.add(toolBar, BorderLayout.CENTER);
		
		JButton btnLoad = new JButton(loadAction);
		btnLoad.setHideActionText(true);
		toolBar.add(btnLoad);
		
		JButton btnUnload = new JButton(unloadAction);
		btnUnload.setHideActionText(true);
		toolBar.add(btnUnload);
		
		JPanel panel_1 = new JPanel();
		panel.add(panel_1, BorderLayout.EAST);

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

//		JComboBox lookingComboBox = new JComboBox(Looking.values());
		headsComboBox = new JComboBox();
		
		table = new AutoSelectTextTable(tableModel);
		tableSorter = new TableRowSorter<NozzleTipsTableModel>(tableModel);
//		table.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(lookingComboBox));
		table.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(headsComboBox));
		
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
		splitPane.setLeftComponent(new JScrollPane(table));
		table.setRowSorter(tableSorter);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		splitPane.setRightComponent(tabbedPane);
		
		generalConfigPanel = new JPanel();
		tabbedPane.addTab("General Configuration", null, generalConfigPanel, null);
		generalConfigPanel.setLayout(new BorderLayout(0, 0));
		
		nozzleTipSpecificConfigPanel = new JPanel();
		tabbedPane.addTab("Nozzle Tip Specific", null, nozzleTipSpecificConfigPanel, null);
		nozzleTipSpecificConfigPanel.setLayout(new BorderLayout(0, 0));
		
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) {
					return;
				}
				int index = table.getSelectedRow();
				
				generalConfigPanel.removeAll();
				nozzleTipSpecificConfigPanel.removeAll();
				if (index != -1) {
					index = table.convertRowIndexToModel(index);
					NozzleTip nozzleTip = tableModel.getNozzleTip(index);
//					Wizard generalConfigWizard = new NozzleConfigurationWizard(nozzle);
					Wizard generalConfigWizard = null;
					if (generalConfigWizard != null) {
						generalConfigWizard.setWizardContainer(NozzleTipsPanel.this);
						JPanel panel = generalConfigWizard.getWizardPanel();
						generalConfigPanel.add(panel);
					}
					Wizard nozzleTipSpecificConfigWizard = nozzleTip.getConfigurationWizard();
					if (nozzleTipSpecificConfigWizard != null) {
					    nozzleTipSpecificConfigWizard.setWizardContainer(NozzleTipsPanel.this);
						JPanel panel = nozzleTipSpecificConfigWizard.getWizardPanel();
						nozzleTipSpecificConfigPanel.add(panel);
					}
				}
				
				revalidate();
				repaint();
			}
		});
		
        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            public void configurationComplete(Configuration configuration) throws Exception {
                headsComboBox.removeAllItems();
                headsComboBox.addItem(new HeadCellValue((Head) null)); 
                for (Head head : configuration.getMachine().getHeads()) {
                    headsComboBox.addItem(new HeadCellValue(head));
                }
            }
        });
	}
	
    private NozzleTip getSelectedNozzleTip() {
        int index = table.getSelectedRow();

        if (index == -1) {
            return null;
        }

        index = table.convertRowIndexToModel(index);
        return tableModel.getNozzleTip(index);
    }
    
    private Nozzle getSelectedNozzle() {
        int index = table.getSelectedRow();

        if (index == -1) {
            return null;
        }

        index = table.convertRowIndexToModel(index);
        return tableModel.getNozzle(index);
    }
    
	private void search() {
		RowFilter<NozzleTipsTableModel, Object> rf = null;
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
		Configuration.get().setDirty(true);
	}

	@Override
	public void wizardCancelled(Wizard wizard) {
	}
	
    public Action loadAction = new AbstractAction("Load") {
        {
            putValue(SMALL_ICON,
                    new ImageIcon(getClass().getResource("/icons/load.png")));
            putValue(NAME, "Load");
            putValue(SHORT_DESCRIPTION,
                    "Load the currently selected nozzle tip.");
        }
        
        @Override
        public void actionPerformed(ActionEvent arg0) {
            MainFrame.machineControlsPanel.submitMachineTask(new Runnable() {
                public void run() {
                    try {
                        getSelectedNozzle().loadNozzleTip(getSelectedNozzleTip());
                    }
                    catch (Exception e) {
                        MessageBoxes.errorBox(getTopLevelAncestor(),
                                "Movement Error", e);
                    }
                }
            });
        }
    };
    
    public Action unloadAction = new AbstractAction("Unoad") {
        {
            putValue(SMALL_ICON,
                    new ImageIcon(getClass().getResource("/icons/unload.png")));
            putValue(NAME, "Unload");
            putValue(SHORT_DESCRIPTION,
                    "Unoad the currently loaded nozzle tip.");
        }
        
        @Override
        public void actionPerformed(ActionEvent arg0) {
            MainFrame.machineControlsPanel.submitMachineTask(new Runnable() {
                public void run() {
                    try {
                        getSelectedNozzle().unloadNozzleTip();
                    }
                    catch (Exception e) {
                        MessageBoxes.errorBox(getTopLevelAncestor(),
                                "Movement Error", e);
                    }
                }
            });
        }
    };
    
	private JPanel generalConfigPanel;
	private JPanel nozzleTipSpecificConfigPanel;
}