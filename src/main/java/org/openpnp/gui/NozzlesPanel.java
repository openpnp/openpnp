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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.prefs.Preferences;
import java.util.regex.PatternSyntaxException;

import javax.swing.DefaultCellEditor;
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
import org.openpnp.gui.support.Wizard;
import org.openpnp.gui.support.WizardContainer;
import org.openpnp.gui.tablemodel.NozzlesTableModel;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Head;
import org.openpnp.spi.Nozzle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class NozzlesPanel extends JPanel implements WizardContainer {
	private final static Logger logger = LoggerFactory.getLogger(NozzlesPanel.class);

	private static final String PREF_DIVIDER_POSITION = "NozzlesPanel.dividerPosition";
	private static final int PREF_DIVIDER_POSITION_DEF = -1;
	
	private final Frame frame;
	private final Configuration configuration;
	
	private JTable table;

	private NozzlesTableModel tableModel;
	private TableRowSorter<NozzlesTableModel> tableSorter;
	private JTextField searchTextField;
	private JComboBox headsComboBox;

	private Preferences prefs = Preferences.userNodeForPackage(NozzlesPanel.class);

	public NozzlesPanel(Frame frame, Configuration configuration) {
		this.frame = frame;
		this.configuration = configuration;
		
		setLayout(new BorderLayout(0, 0));
		tableModel = new NozzlesTableModel(configuration);

		JPanel panel = new JPanel();
		add(panel, BorderLayout.NORTH);
		panel.setLayout(new BorderLayout(0, 0));

		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		panel.add(toolBar, BorderLayout.CENTER);
		
//		JButton btnNewCamera = new JButton(newCameraAction);
//		btnNewCamera.setHideActionText(true);
//		toolBar.add(btnNewCamera);
		
//		JButton btnDeleteCamera = new JButton(deleteCameraAction);
//		btnDeleteCamera.setHideActionText(true);
//		toolBar.add(btnDeleteCamera);
		
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
		tableSorter = new TableRowSorter<NozzlesTableModel>(tableModel);
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
		
		nozzleSpecificConfigPanel = new JPanel();
		tabbedPane.addTab("Nozzle Specific", null, nozzleSpecificConfigPanel, null);
		nozzleSpecificConfigPanel.setLayout(new BorderLayout(0, 0));
		
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) {
					return;
				}
				int index = table.getSelectedRow();
				
				generalConfigPanel.removeAll();
				nozzleSpecificConfigPanel.removeAll();
				if (index != -1) {
					index = table.convertRowIndexToModel(index);
					Nozzle nozzle = tableModel.getNozzle(index);
//					Wizard generalConfigWizard = new NozzleConfigurationWizard(nozzle);
					Wizard generalConfigWizard = null;
					if (generalConfigWizard != null) {
						generalConfigWizard.setWizardContainer(NozzlesPanel.this);
						JPanel panel = generalConfigWizard.getWizardPanel();
						generalConfigPanel.add(panel);
					}
					Wizard nozzleSpecificConfigWizard = nozzle.getConfigurationWizard();
					if (nozzleSpecificConfigWizard != null) {
					    nozzleSpecificConfigWizard.setWizardContainer(NozzlesPanel.this);
						JPanel panel = nozzleSpecificConfigWizard.getWizardPanel();
						nozzleSpecificConfigPanel.add(panel);
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
	
	private void search() {
		RowFilter<NozzlesTableModel, Object> rf = null;
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
	
	private JPanel generalConfigPanel;
	private JPanel nozzleSpecificConfigPanel;
}