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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.prefs.Preferences;
import java.util.regex.PatternSyntaxException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableRowSorter;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.components.AutoSelectTextTable;
import org.openpnp.gui.components.ClassSelectionDialog;
import org.openpnp.gui.support.HeadCellValue;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.Wizard;
import org.openpnp.gui.support.WizardContainer;
import org.openpnp.gui.tablemodel.CamerasTableModel;
import org.openpnp.gui.wizards.CameraConfigurationWizard;
import org.openpnp.machine.reference.vision.OpenCvVisionProvider;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Camera.Looking;
import org.openpnp.spi.Head;
import org.openpnp.spi.VisionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class CamerasPanel extends JPanel implements WizardContainer {
	private final static Logger logger = LoggerFactory.getLogger(CamerasPanel.class);

	private static final String PREF_DIVIDER_POSITION = "CamerasPanel.dividerPosition";
	private static final int PREF_DIVIDER_POSITION_DEF = -1;
	
	private final Frame frame;
	private final Configuration configuration;
	
	private JTable table;

	private CamerasTableModel tableModel;
	private TableRowSorter<CamerasTableModel> tableSorter;
	private JTextField searchTextField;
	private JComboBox headsComboBox;

	private Preferences prefs = Preferences.userNodeForPackage(CamerasPanel.class);

	public CamerasPanel(Frame frame, Configuration configuration) {
		this.frame = frame;
		this.configuration = configuration;
		
		setLayout(new BorderLayout(0, 0));
		tableModel = new CamerasTableModel(configuration);

		JPanel panel = new JPanel();
		add(panel, BorderLayout.NORTH);
		panel.setLayout(new BorderLayout(0, 0));

		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		panel.add(toolBar, BorderLayout.CENTER);
		
		JButton btnNewCamera = new JButton(newCameraAction);
		btnNewCamera.setHideActionText(true);
		toolBar.add(btnNewCamera);
		
		JButton btnDeleteCamera = new JButton(deleteCameraAction);
		btnDeleteCamera.setHideActionText(true);
		toolBar.add(btnDeleteCamera);
		
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

		JComboBox lookingComboBox = new JComboBox(Looking.values());
		headsComboBox = new JComboBox();
		
		table = new AutoSelectTextTable(tableModel);
		tableSorter = new TableRowSorter<>(tableModel);
		table.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(lookingComboBox));
		table.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(headsComboBox));
		
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
		
        cameraSpecificConfigPanel = new JPanel();
        tabbedPane.addTab("Camera Specific", null, cameraSpecificConfigPanel, null);
        cameraSpecificConfigPanel.setLayout(new BorderLayout(0, 0));
        
        visionProviderConfigPanel = new JPanel();
        tabbedPane.addTab("Vision Provider", null, visionProviderConfigPanel, null);
        visionProviderConfigPanel.setLayout(new BorderLayout(0, 0));
        
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) {
					return;
				}
				int index = table.getSelectedRow();
				
				generalConfigPanel.removeAll();
                cameraSpecificConfigPanel.removeAll();
                visionProviderConfigPanel.removeAll();
                
				if (index != -1) {
					index = table.convertRowIndexToModel(index);
					Camera camera = tableModel.getCamera(index);
					Wizard generalConfigWizard = new CameraConfigurationWizard(camera);
					if (generalConfigWizard != null) {
						generalConfigWizard.setWizardContainer(CamerasPanel.this);
						JPanel panel = generalConfigWizard.getWizardPanel();
						generalConfigPanel.add(panel);
					}
					
                    Wizard cameraSpecificConfigWizard = camera.getConfigurationWizard();
                    if (cameraSpecificConfigWizard != null) {
                        cameraSpecificConfigWizard.setWizardContainer(CamerasPanel.this);
                        JPanel panel = cameraSpecificConfigWizard.getWizardPanel();
                        cameraSpecificConfigPanel.add(panel);
                    }
                    
                    VisionProvider visionProvider = camera.getVisionProvider();
                    if (visionProvider != null) {
                        Wizard visionProviderConfigWizard = visionProvider.getConfigurationWizard();
                        if (visionProviderConfigWizard != null) {
                            visionProviderConfigWizard.setWizardContainer(CamerasPanel.this);
                            JPanel panel = visionProviderConfigWizard.getWizardPanel();
                            visionProviderConfigPanel.add(panel);
                        }
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
	
    private Camera getSelectedCamera() {
        int index = table.getSelectedRow();

        if (index == -1) {
            return null;
        }

        index = table.convertRowIndexToModel(index);
        return tableModel.getCamera(index);
    }

    private void search() {
		RowFilter<CamerasTableModel, Object> rf = null;
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
	}

	@Override
	public void wizardCancelled(Wizard wizard) {
	}
	
	public Action newCameraAction = new AbstractAction() {
		{
			putValue(SMALL_ICON, Icons.add);
			putValue(NAME, "New Camera...");
			putValue(SHORT_DESCRIPTION,
					"Create a new camera.");
		}
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			ClassSelectionDialog<Camera> dialog = new ClassSelectionDialog<>(
					JOptionPane.getFrameForComponent(CamerasPanel.this),
					"Select Camera...",
					"Please select a Camera implemention from the list below.",
					configuration.getMachine().getCompatibleCameraClasses());
			dialog.setVisible(true);
			Class<? extends Camera> cameraClass = dialog.getSelectedClass();
			if (cameraClass == null) {
				return;
			}
			try {
				Camera camera = cameraClass.newInstance();
				
				camera.setUnitsPerPixel(new Location(Configuration.get().getSystemUnits()));
				try {
					if (camera.getVisionProvider() == null) {
						camera.setVisionProvider(new OpenCvVisionProvider());
					}
				}
				catch (Exception e) {
					logger.debug("Couldn't set default vision provider. Meh.");
				}
				
				
				configuration.getMachine().addCamera(camera);
				
				MainFrame.cameraPanel.addCamera(camera);
				tableModel.refresh();
				Helpers.selectLastTableRow(table);
			}
			catch (Exception e) {
				MessageBoxes.errorBox(
						JOptionPane.getFrameForComponent(CamerasPanel.this), 
						"Camera Error", 
						e);
			}
		}
	};

	public Action deleteCameraAction = new AbstractAction("Delete Camera") {
		{
			putValue(SMALL_ICON, Icons.delete);
			putValue(NAME, "Delete Camera");
			putValue(SHORT_DESCRIPTION,
					"Delete the currently selected camera.");
		}
		@Override
		public void actionPerformed(ActionEvent arg0) {
		    Camera camera = getSelectedCamera();
            int ret = JOptionPane.showConfirmDialog(
                    getTopLevelAncestor(), 
                    "Are you sure you want to delete " + camera.getName() + "?",
                    "Delete " + camera.getName() + "?",
                    JOptionPane.YES_NO_OPTION);
            if (ret == JOptionPane.YES_OPTION) {
                if (camera.getHead() != null) {
                    camera.getHead().removeCamera(camera);
                }
                else {
                    configuration.getMachine().removeCamera(camera);
                }
                tableModel.refresh();
                MessageBoxes.errorBox(
                        getTopLevelAncestor(), 
                        "Restart Required", 
                        camera.getName() + " has been removed. Please restart OpenPnP for the changes to take effect.");
            }
		}
	};
	
	
	private JPanel generalConfigPanel;
    private JPanel cameraSpecificConfigPanel;
    private JPanel visionProviderConfigPanel;
}