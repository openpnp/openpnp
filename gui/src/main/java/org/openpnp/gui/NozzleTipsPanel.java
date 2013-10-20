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
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.regex.PatternSyntaxException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
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
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableRowSorter;

import org.openpnp.gui.components.AutoSelectTextTable;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.components.ClassSelectionDialog;
import org.openpnp.gui.components.reticle.OutlineReticle;
import org.openpnp.gui.support.ActionGroup;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.Wizard;
import org.openpnp.gui.support.WizardContainer;
import org.openpnp.gui.tablemodel.NozzleTipsTableModel;
import org.openpnp.machine.zippy.ZippyMachine;
import org.openpnp.machine.zippy.ZippyNozzle;
import org.openpnp.machine.zippy.ZippyNozzleTip;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.model.Outline;
import org.openpnp.model.Part;
import org.openpnp.spi.Head;
import org.openpnp.spi.NozzleTip;
import org.openpnp.spi.Nozzle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.border.TitledBorder;

@SuppressWarnings("serial")
public class NozzleTipsPanel extends JPanel implements WizardContainer {
	private final static Logger logger = LoggerFactory
			.getLogger(NozzleTipsPanel.class);

	private final Configuration configuration;

	private static final String PREF_DIVIDER_POSITION = "NozzleTipsPanel.dividerPosition";
	private static final int PREF_DIVIDER_POSITION_DEF = -1;

	private JTable table;

	private NozzleTipsTableModel tableModel;
	private TableRowSorter<NozzleTipsTableModel> tableSorter;
	private JTextField searchTextField;
    private JPanel configurationPanel;
    private JPanel changersetupPanel;

	private ActionGroup nozzletipSelectedActionGroup;

	private Preferences prefs = Preferences
			.userNodeForPackage(NozzleTipsPanel.class);

	public NozzleTipsPanel(Configuration configuration) {
		this.configuration = configuration;

		setLayout(new BorderLayout(0, 0));
		tableModel = new NozzleTipsTableModel(configuration);

		JPanel panel = new JPanel();
		add(panel, BorderLayout.NORTH);
		panel.setLayout(new BorderLayout(0, 0));

		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		panel.add(toolBar, BorderLayout.CENTER);

		JButton btnNewNozzleTip = new JButton(newNozzleTipAction);
		btnNewNozzleTip.setHideActionText(true);
		toolBar.add(btnNewNozzleTip);

		JButton btnDeleteNozzleTip = new JButton(deleteNozzleTipAction);
		btnDeleteNozzleTip.setHideActionText(true);
		toolBar.add(btnDeleteNozzleTip);

		toolBar.addSeparator();
		toolBar.add(loadNozzleTipAction);
		toolBar.add(unloadNozzleTipAction);
		toolBar.add(calibrateOffsetsAction);

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
		table = new AutoSelectTextTable(tableModel);
		tableSorter = new TableRowSorter<NozzleTipsTableModel>(tableModel);

		final JSplitPane splitPane = new JSplitPane();
		splitPane.setContinuousLayout(true);
		splitPane.setDividerLocation(prefs.getInt(PREF_DIVIDER_POSITION,
				PREF_DIVIDER_POSITION_DEF));
		splitPane.addPropertyChangeListener("dividerLocation",
				new PropertyChangeListener() {
					@Override
					public void propertyChange(PropertyChangeEvent evt) {
						prefs.putInt(PREF_DIVIDER_POSITION,
								splitPane.getDividerLocation());
					}
				});
		add(splitPane, BorderLayout.CENTER);

		table.setRowSorter(tableSorter);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		configurationPanel = new JPanel();
		configurationPanel.setBorder(new TitledBorder(null, "Configuration", TitledBorder.LEADING, TitledBorder.TOP, null, null));

		nozzletipSelectedActionGroup = new ActionGroup(deleteNozzleTipAction,
				loadNozzleTipAction, unloadNozzleTipAction, calibrateOffsetsAction);

		table.getSelectionModel().addListSelectionListener(
				new ListSelectionListener() {
					@Override
					public void valueChanged(ListSelectionEvent e) {
						if (e.getValueIsAdjusting()) {
							return;
						}

						NozzleTip nozzletip = getSelectedNozzleTip();

						nozzletipSelectedActionGroup.setEnabled(nozzletip != null);

						configurationPanel.removeAll();
						if (nozzletip != null) {
							Wizard wizard = nozzletip.getConfigurationWizard();
							if (wizard != null) {
								wizard.setWizardContainer(NozzleTipsPanel.this);
								JPanel panel = wizard.getWizardPanel();
								configurationPanel.add(panel);
							}
						}
						revalidate();
						repaint();
					}
				});

		nozzletipSelectedActionGroup.setEnabled(false);
		
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		splitPane.setRightComponent(tabbedPane);
		
        splitPane.setLeftComponent(new JScrollPane(table));
        splitPane.setRightComponent(configurationPanel);
        configurationPanel.setLayout(new BorderLayout(0, 0));
        
//		tabbedPane.addTab("Calibration", null, configurationPanel, null);
//		configurationPanel.setLayout(new BorderLayout(0, 0));

//		changersetupPanel = new JPanel();
//		tabbedPane.addTab("Changer Setup", null, changersetupPanel, null);
//		changersetupPanel.setLayout(new BorderLayout(0, 0));
		
	}

	private NozzleTip getSelectedNozzleTip() {
		int index = table.getSelectedRow();

		if (index == -1) {
			return null;
		}

		index = table.convertRowIndexToModel(index);
		return tableModel.getNozzleTip(index);
	}

	private void search() {
		RowFilter<NozzleTipsTableModel, Object> rf = null;
		// If current expression doesn't parse, don't update.
		try {
			rf = RowFilter.regexFilter("(?i)"
					+ searchTextField.getText().trim());
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

	public Action newNozzleTipAction = new AbstractAction() {
		{
			putValue(SMALL_ICON,
					new ImageIcon(getClass().getResource("/icons/new.png")));
			putValue(NAME, "New NozzleTip...");
			putValue(SHORT_DESCRIPTION, "Create a new nozzletip.");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
/*			if (Configuration.get().getParts().size() == 0) {
				MessageBoxes
						.errorBox(
								getTopLevelAncestor(),
								"Error",
								"There are currently no parts defined in the system. Please create at least one part before creating a nozzletip.");
				return;
			}
*/
			ClassSelectionDialog<NozzleTip> dialog = new ClassSelectionDialog<NozzleTip>(
					JOptionPane.getFrameForComponent(NozzleTipsPanel.this),
					"Select NozzleTip...",
					"Please select a NozzleTip implemention from the list below.",
					((ZippyMachine) configuration.getMachine()).getCompatibleNozzleTipClasses()); 
			dialog. setVisible(true); 
			Class<? extends NozzleTip> nozzletipClass = dialog.getSelectedClass();
			if (nozzletipClass == null) {
				return;
			}

			//add message box here to select from list of heads on the machine
			ArrayList<String> configured_heads = new ArrayList<String>(); //new empty list for heads
			for (Head head : configuration.getMachine().getHeads()) { //for each head
				configured_heads.add(head.getId()); //add to list from above
			}
		    String[] headArr = new String[configured_heads.size()];
		    headArr = configured_heads.toArray(headArr);
		    String selected_head_str;
			if(configured_heads.size()>1){
				selected_head_str = (String)JOptionPane.showInputDialog(
						JOptionPane.getFrameForComponent(NozzleTipsPanel.this), //frame
				        "Select Head...", //question
	                    "Please select an Installed Nozzle from the list below.", //dialog label
	                    JOptionPane.QUESTION_MESSAGE,
	                    null,
	                    headArr,
	                    headArr[0]);
			} else {
				selected_head_str = headArr[0];
			}

			
			//add message box here to select from list of nozzles on the machine
			ArrayList<String> configured_nozzles = new ArrayList<String>(); //new empty list for nozzles
			for (Head head : configuration.getMachine().getHeads()) { //for each head
				for (Nozzle nozzle : head.getNozzles()) { //for each nozzle
					configured_nozzles.add(nozzle.getId()); //add to list from above
				}
			}
		    String[] nozzleArr = new String[configured_nozzles.size()];
		    nozzleArr = configured_nozzles.toArray(nozzleArr);
		    String selected_nozzle_str;
		    if(configured_nozzles.size()>1){
				selected_nozzle_str = (String)JOptionPane.showInputDialog(
						JOptionPane.getFrameForComponent(NozzleTipsPanel.this), //frame
				        "Select Nozzle...", //question
	                    "Please select an Installed Nozzle from the list below.", //dialog label
	                    JOptionPane.QUESTION_MESSAGE,
	                    null,
	                    nozzleArr,
	                    nozzleArr[0]);
		    } else { 
		    	selected_nozzle_str = nozzleArr[0];
		    }
			try {
				NozzleTip nozzletip = nozzletipClass.newInstance();

				((ZippyNozzleTip) nozzletip).setId(Helpers.createUniqueName("NT", ( ((ZippyMachine) Configuration.get().getMachine()).getNozzleTips()), "id"));
				
				((ZippyNozzleTip) nozzletip).setNozzleOffsets(new Location(Configuration.get().getSystemUnits()));

				
				((ZippyNozzle) configuration.getMachine().getHead(selected_head_str).getNozzle(selected_nozzle_str)).addNozzleTip(nozzletip);
				tableModel.refresh();
				Helpers.selectLastTableRow(table);
				configuration.setDirty(true);
			}
			catch (Exception e) {
				MessageBoxes.errorBox(
						JOptionPane.getFrameForComponent(NozzleTipsPanel.this),
						"NozzleTip Error", e);
			}
		}
	};

	public Action deleteNozzleTipAction = new AbstractAction() {
		{
			putValue(SMALL_ICON,
					new ImageIcon(getClass().getResource("/icons/delete.png")));
			putValue(NAME, "Delete NozzleTip");
			putValue(SHORT_DESCRIPTION, "Delete the selected nozzletip.");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
//			configuration.getMachine().removeNozzleTip(getSelectedNozzleTip());
			MessageBoxes.notYetImplemented(getTopLevelAncestor());
		}
	};

	public Action loadNozzleTipAction = new AbstractAction() {
		{
			putValue(SMALL_ICON, new ImageIcon(getClass().getResource("/icons/load.png")));
			putValue(NAME, "Load");
			putValue(SHORT_DESCRIPTION,	"Command the selected nozzletip to perform a load operation.");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			new Thread() {
				public void run() {
					ZippyNozzleTip nozzletip = (ZippyNozzleTip) getSelectedNozzleTip();
					Nozzle nozzle = MainFrame.machineControlsPanel.getSelectedNozzle();
					try {
						nozzletip.load(nozzle);
					}
					catch (Exception e) {
						MessageBoxes.errorBox(NozzleTipsPanel.this, "Load Error",
								e);
					}
				}
			}.start();
		}
	};

	public Action unloadNozzleTipAction = new AbstractAction() {
		{
			putValue(SMALL_ICON, new ImageIcon(getClass().getResource("/icons/unload.png")));
			putValue(NAME, "Unload");
			putValue(SHORT_DESCRIPTION,	"Command the selected nozzletip to perform an unload operation.");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			new Thread() {
				public void run() {
					ZippyNozzleTip nozzletip = (ZippyNozzleTip) getSelectedNozzleTip();
					Nozzle nozzle = MainFrame.machineControlsPanel.getSelectedNozzle();
					try {
						//probably add two of these, a "load" and an "unload" to put down and pick up nozzletips
						nozzletip.unload(nozzle);
					}
					catch (Exception e) {
						MessageBoxes.errorBox(NozzleTipsPanel.this, "Unload Error",	e);
					}
				}
			}.start();
		}
	};
	
	public Action calibrateOffsetsAction = new AbstractAction() {
		{
			putValue(SMALL_ICON, new ImageIcon(getClass().getResource("/icons/calibrate-tip.png")));
			putValue(NAME, "Calibrate Tip");
			putValue(SHORT_DESCRIPTION,	"Calibrate nozzle tip offsets for the selected nozzletip.");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			ZippyNozzleTip nozzletip = (ZippyNozzleTip) getSelectedNozzleTip();
			Nozzle nozzle = MainFrame.machineControlsPanel.getSelectedNozzle();
			if (nozzletip != null) {
				Location currentOffsets = nozzletip.getNozzleOffsets();
				try {
					currentOffsets = nozzletip.calibrate(nozzle);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
/*				if (part != null) {
					org.openpnp.model.Package pkg = part.getPackage();
					if (pkg != null) {
						Outline outline = pkg.getOutline();
						CameraView cameraView = MainFrame.cameraPanel
								.getSelectedCameraView();
						if (cameraView.getReticle(this) != null) {
							cameraView.removeReticle(this);
						}
						else {
							cameraView.setReticle(this, new OutlineReticle(
									outline));
						}
					}
				}
*/			}
		}
	};
}