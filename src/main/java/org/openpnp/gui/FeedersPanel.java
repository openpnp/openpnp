/*
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableRowSorter;

import org.openpnp.Translations;
import org.openpnp.events.FeederSelectedEvent;
import org.openpnp.gui.components.AutoSelectTextTable;
import org.openpnp.gui.components.ClassSelectionDialog;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.ActionGroup;
import org.openpnp.gui.support.CustomBooleanRenderer;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.Wizard;
import org.openpnp.gui.support.WizardContainer;
import org.openpnp.gui.tablemodel.FeedersTableModel;
import org.openpnp.machine.reference.vision.AbstractPartAlignment;
import org.openpnp.machine.reference.vision.ReferenceBottomVision;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Configuration.TablesLinked;
import org.openpnp.model.Job;
import org.openpnp.model.Length;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.JobProcessor.JobProcessorException;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.openpnp.spi.PartAlignment;
import org.openpnp.spi.PropertySheetHolder.PropertySheet;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.UiUtils;
import org.pmw.tinylog.Logger;

import com.google.common.eventbus.Subscribe;

@SuppressWarnings("serial")
public class FeedersPanel extends JPanel implements WizardContainer {
    private final Configuration configuration;
    private final MainFrame mainFrame;

    private static final String PREF_DIVIDER_POSITION = "FeedersPanel.dividerPosition";
    private static final int PREF_DIVIDER_POSITION_DEF = -1;

    private JTable table;

    private FeedersTableModel tableModel;
    private TableRowSorter<FeedersTableModel> tableSorter;
    private JTextField searchTextField;

    private ActionGroup singleSelectActionGroup;
    private ActionGroup multiSelectActionGroup;

    private Preferences prefs = Preferences.userNodeForPackage(FeedersPanel.class);
    
    private JTabbedPane configurationPanel;
    private int priorRowIndex = -1;
    private String priorFeederId;
    private HashMap<Class, Integer> lastSelectedTabIndex = new HashMap<>();
    
    public FeedersPanel(Configuration configuration, MainFrame mainFrame) {
        this.configuration = configuration;
        this.mainFrame = mainFrame;

        setLayout(new BorderLayout(0, 0));
        tableModel = new FeedersTableModel(configuration);

        JPanel panel = new JPanel();
        add(panel, BorderLayout.NORTH);
        panel.setLayout(new BorderLayout(0, 0));

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        panel.add(toolBar, BorderLayout.CENTER);

        JButton btnNewFeeder = new JButton(newFeederAction);
        btnNewFeeder.setHideActionText(true);
        toolBar.add(btnNewFeeder);

        JButton btnDeleteFeeder = new JButton(deleteFeederAction);
        btnDeleteFeeder.setHideActionText(true);
        toolBar.add(btnDeleteFeeder);

        toolBar.addSeparator();
        toolBar.add(pickFeederAction);
        toolBar.add(feedFeederAction);
        toolBar.add(moveCameraToPickLocation);
        toolBar.add(moveToolToPickLocation);

        JPanel panel_1 = new JPanel();
        panel.add(panel_1, BorderLayout.EAST);

        JLabel lblSearch = new JLabel(Translations.getString("FeedersPanel.SearchLabel.text")); //$NON-NLS-1$
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
		table.setDefaultRenderer(Boolean.class, new CustomBooleanRenderer() {
			// cells are grayed if the feeder is not used by any enabled placement.
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column) {
				final Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
						column);
				if (!isSelected) {
					String partId = (String) tableModel.getValueAt(tableSorter.convertRowIndexToModel(row), 2);
					Job job = mainFrame.getJobTab().getJob();
					boolean bFound = false;

					for (BoardLocation boardLocation : job.getBoardLocations()) {
						// Only check enabled boards
						if (!boardLocation.isEnabled()) {
							continue;
						}

						for (Placement placement : boardLocation.getBoard().getPlacements()) {
							// Ignore placements that aren't placements
							if (placement.getType() != Placement.Type.Placement) {
								continue;
							}
							if (!placement.isEnabled()) {
								continue;
							}

							if (placement.getPart() != null && placement.getPart().getId().equals(partId)) {
								bFound = true;
								break;
							}
						}
						if (bFound) {
							break;
						}
					}
					if (!bFound) {
                        c.setEnabled(false);
                    } else {
					    c.setEnabled(true);
                    }
				}
				return c;
			}
		});
        tableSorter = new TableRowSorter<>(tableModel);
        table.getColumnModel().moveColumn(1,  2);

        final JSplitPane splitPane = new JSplitPane();
        splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        splitPane.setContinuousLayout(true);
        splitPane
                .setDividerLocation(prefs.getInt(PREF_DIVIDER_POSITION, PREF_DIVIDER_POSITION_DEF));
        splitPane.addPropertyChangeListener("dividerLocation", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                prefs.putInt(PREF_DIVIDER_POSITION, splitPane.getDividerLocation());
            }
        });
        
        add(splitPane, BorderLayout.CENTER);
        splitPane.setLeftComponent(new JScrollPane(table));
        table.setRowSorter(tableSorter);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        configurationPanel = new JTabbedPane(JTabbedPane.TOP);
        splitPane.setRightComponent(configurationPanel);

        singleSelectActionGroup = new ActionGroup(deleteFeederAction, feedFeederAction,
                pickFeederAction, moveCameraToPickLocation, moveToolToPickLocation,
                setEnabledAction);
        singleSelectActionGroup.setEnabled(false);
        
        multiSelectActionGroup = new ActionGroup(deleteFeederAction, setEnabledAction);
        multiSelectActionGroup.setEnabled(false);
        
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }
                List<Feeder> selections = getSelections();
                if (selections.size() == 0) {
                    singleSelectActionGroup.setEnabled(false);
                    multiSelectActionGroup.setEnabled(false);
                }
                else if (selections.size() == 1) {
                    multiSelectActionGroup.setEnabled(false);
                    singleSelectActionGroup.setEnabled(true);
                }
                else {
                    singleSelectActionGroup.setEnabled(false);
                    multiSelectActionGroup.setEnabled(true);
                }

                if (table.getSelectedRow() != priorRowIndex) {
                    if (keepUnAppliedFeederConfigurationChanges()) {
                        table.setRowSelectionInterval(priorRowIndex, priorRowIndex);
                        return;
                    }
                    priorRowIndex = table.getSelectedRow();

                    Feeder feeder = getSelection();

                    configurationPanel.removeAll();
                    if (feeder != null) {
                        priorFeederId = feeder.getId();
                        PropertySheet[] propertySheets = feeder.getPropertySheets();
                        for (PropertySheet ps : propertySheets) {
                            JPanel panel = ps.getPropertySheetPanel();
                            if(panel instanceof AbstractConfigurationWizard) {
                                AbstractConfigurationWizard wizard = (AbstractConfigurationWizard) panel;
                                wizard.setWizardContainer(FeedersPanel.this);
                            }
                            configurationPanel.addTab(ps.getPropertySheetTitle(), panel);
                        }
                        // Re-select the last selected tab of that feeder class. 
                        if (lastSelectedTabIndex.get(feeder.getClass()) != null) {
                            configurationPanel.setSelectedIndex(Math.max(0, Math.min(configurationPanel.getTabCount()-1, 
                                    lastSelectedTabIndex.get(feeder.getClass()))));
                        }
                        if (mainFrame.getTabs().getSelectedComponent() == mainFrame.getFeedersTab()
                              &&  Configuration.get().getTablesLinked() == TablesLinked.Linked
                              && feeder.getPart() != null) {
                            mainFrame.getPartsTab().selectPartInTableAndUpdateLinks(feeder.getPart());
                        }
                    }

                    revalidate();
                    repaint();

                    Configuration.get().getBus().post(new FeederSelectedEvent(feeder, FeedersPanel.this));
                }
            }
        });

        Configuration.get().getBus().register(this);
        
        JPopupMenu popupMenu = new JPopupMenu();

        JMenu setEnabledMenu = new JMenu(setEnabledAction);
        setEnabledMenu.add(new SetEnabledAction(true));
        setEnabledMenu.add(new SetEnabledAction(false));
        popupMenu.add(setEnabledMenu);

        table.setComponentPopupMenu(popupMenu);
    }

    private boolean keepUnAppliedFeederConfigurationChanges() {
        Feeder priorFeeder = configuration.getMachine().getFeeder(priorFeederId);
        if (priorFeeder != null) {
            // Btw., remember the tab that was selected for this feeder class.
            lastSelectedTabIndex.put(priorFeeder.getClass(), configurationPanel.getSelectedIndex());
        }
        boolean feederConfigurationIsDirty = false;
        for (Component component : configurationPanel.getComponents()) {
            if(component instanceof AbstractConfigurationWizard) {
                feederConfigurationIsDirty = ((AbstractConfigurationWizard) component).isDirty();
                if(feederConfigurationIsDirty) {
                    break;
                }
            }
        }
        if (feederConfigurationIsDirty && (priorFeeder != null)) {
            int selection = JOptionPane.showConfirmDialog(null,
                    priorFeeder.getName() + " changed.  Apply changes?",
                    "Warning!",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null
                    );
            switch (selection) {
                case JOptionPane.YES_OPTION:
                    for (Component component : configurationPanel.getComponents()) {
                        if(component instanceof AbstractConfigurationWizard) {
                            AbstractConfigurationWizard wizard = (AbstractConfigurationWizard) component;
                            if(wizard.isDirty()) {
                                wizard.apply();
                            }
                        }
                    }
                    return false;
                case JOptionPane.NO_OPTION:
                    return false;
                case JOptionPane.CANCEL_OPTION:
                default:
                    return true;
            }
        } else {
            return false;
        }
    }

    @Subscribe
    public void feederSelected(FeederSelectedEvent event) {
        if (event.source == this) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            mainFrame.showTab("Feeders");
            
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                if (tableModel.getRowObjectAt(i) == event.feeder) {
                    int index = table.convertRowIndexToView(i);
                    table.getSelectionModel().setSelectionInterval(index, index);
                    table.scrollRectToVisible(new Rectangle(table.getCellRect(index, 0, true)));
                    break;
                }
            }
        });
    }

    /**
     * Activate the Feeders tab and show the Feeder for the specified Part. If none exists, prompt
     * the user to create a new one.
     * 
     * @param part
     */
    public void showFeederForPart(Part part) {
        mainFrame.showTab("Feeders");
        searchTextField.setText("");
        search();

        Feeder feeder = findFeeder(part, true);
        // Prefer enabled feeders but fall back to disabled ones.
        if (feeder == null) {
            feeder = findFeeder(part, false);
        }
        if (feeder == null) {
            newFeeder(part);
        }
        else {
            Helpers.selectObjectTableRow(table, feeder);
        }
    }

    private Feeder findFeeder(Part part, boolean enabled) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Feeder feeder = tableModel.getRowObjectAt(i); 
            if (feeder.getPart() == part && feeder.isEnabled() == enabled) {
                return feeder;
            }
        }
        return null;
    }

    public Feeder getSelection() {
        List<Feeder> selections = getSelections();
        if (selections.size() != 1) {
            return null;
        }
        return selections.get(0);
    }

    public List<Feeder> getSelections() {
        ArrayList<Feeder> selections = new ArrayList<>();
        int[] selectedRows = table.getSelectedRows();
        for (int selectedRow : selectedRows) {
            selectedRow = table.convertRowIndexToModel(selectedRow);
            selections.add(Configuration.get().getMachine().getFeeders().get(selectedRow));
        }
        return selections;
    }

    private void search() {
        RowFilter<FeedersTableModel, Object> rf = null;
        // If current expression doesn't parse, don't update.
        try {
            rf = RowFilter.regexFilter("(?i)" + searchTextField.getText().trim());
        }
        catch (PatternSyntaxException e) {
            Logger.warn(e, "Search failed");
            return;
        }
        tableSorter.setRowFilter(rf);
    }

    @Override
    public void wizardCompleted(Wizard wizard) {
        // Repaint the table so that any changed fields get updated.
        table.repaint();
    }

    @Override
    public void wizardCancelled(Wizard wizard) {}

    private void newFeeder(Part part) {
        if (keepUnAppliedFeederConfigurationChanges()) {
            return;
        }
        
        if (Configuration.get().getParts().size() == 0) {
            MessageBoxes.errorBox(getTopLevelAncestor(), "Error",
                    "There are currently no parts defined in the system. Please create at least one part before creating a feeder.");
            return;
        }

        String title;
        if (part == null) {
            title = Translations.getString("FeedersPanel.SelectFeederImplementationDialog.Select.title"); //$NON-NLS-1$
        }
        else {
            title = Translations.getString("FeedersPanel.SelectFeederImplementationDialog.SelectFor.title" //$NON-NLS-1$
            ) + " " + part.getId() + "..."; //$NON-NLS-1$ //$NON-NLS-2$
        }
        ClassSelectionDialog<Feeder> dialog =
                new ClassSelectionDialog<>(JOptionPane.getFrameForComponent(FeedersPanel.this),
                        title, Translations.getString(
                                "FeedersPanel.SelectFeederImplementationDialog.Description"), //$NON-NLS-1$
                        configuration.getMachine().getCompatibleFeederClasses());
        dialog.setVisible(true);
        Class<? extends Feeder> feederClass = dialog.getSelectedClass();
        if (feederClass == null) {
            return;
        }
        try {
            priorFeederId = null;
            
            Feeder feeder = feederClass.newInstance();

            feeder.setPart(part == null ? Configuration.get().getParts().get(0) : part);

            configuration.getMachine().addFeeder(feeder);
            tableModel.refresh();

            searchTextField.setText("");
            search();

            Helpers.selectLastTableRow(table);
        }

        catch (Exception e) {
            MessageBoxes.errorBox(JOptionPane.getFrameForComponent(FeedersPanel.this),
                    "Feeder Error", e);
        }
    }
    
    public void updateView() {
    	tableModel.fireTableChanged(null);
    }

    protected Location preliminaryPickLocation(Feeder feeder, Nozzle nozzle) throws Exception {
        Location pickLocation = feeder.getPickLocation();
        if (feeder.isPartHeightAbovePickLocation()) {
            Length partHeight = nozzle.getSafePartHeight(feeder.getPart());
            pickLocation = pickLocation.add(new Location(partHeight.getUnits(), 0, 0, partHeight.getValue(), 0));
        }
        return pickLocation;
    }

    public Action newFeederAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.add);
            putValue(NAME, Translations.getString("FeedersPanel.Action.NewFeeder")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("FeedersPanel.Action.NewFeeder.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            newFeeder(null);
        }
    };

    public Action deleteFeederAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.delete);
            putValue(NAME, Translations.getString("FeedersPanel.Action.DeleteFeeder")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("FeedersPanel.Action.DeleteFeeder.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            List<Feeder> selections = getSelections();
            List<String> ids = selections.stream().map(Feeder::getName).collect(Collectors.toList());
            String formattedIds;
            if (ids.size() <= 3) {
                formattedIds = String.join(", ", ids);
            }
            else {
                formattedIds = String.join(", ", ids.subList(0, 3)) + ", and " + (ids.size() - 3) + " others";
            }
            
            int ret = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                    Translations.getString("DialogMessages.ConfirmDelete.text") + " " + formattedIds + "?", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    Translations.getString("DialogMessages.ConfirmDelete.title") + " " + //$NON-NLS-1$ //$NON-NLS-2$
                            selections.size() + " " + Translations.getString("CommonWords.feeders") + "?", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    JOptionPane.YES_NO_OPTION);
            if (ret == JOptionPane.YES_OPTION) {
                for (Feeder feeder : selections) {
                    configuration.getMachine().removeFeeder(feeder);
                    tableModel.refresh();
                }
            }
        }
    };

    public Action feedFeederAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.feed);
            putValue(NAME, Translations.getString("FeedersPanel.Action.FeedFeeder")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("FeedersPanel.Action.FeedFeeder.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                Feeder feeder = getSelection();
                // Do the feed and get the nozzle that would be used for the subsequent pick. 
                Nozzle nozzle = feedFeeder(feeder);
            });
        }
    };

    public Action pickFeederAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.pick);
            putValue(NAME, Translations.getString("FeedersPanel.Action.PickFeeder")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("FeedersPanel.Action.PickFeeder.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                Feeder feeder = getSelection();

                pickFeeder(feeder);
            });
        }
    };

    /**
     * Perform a job-like feed operations sequence. 
     * 
     * @param feeder
     * @return the nozzle to be used for a subsequent pick.
     * @throws Exception
     */
    public static Nozzle feedFeeder(Feeder feeder) throws Exception {
        if (feeder.getPart() == null) {
            throw new Exception("Feeder "+feeder.getName()+" has no part.");
        }
        // Simulate a "one feeder" job, prepare the feeder.
        if (feeder.getJobPreparationLocation() != null) {
            feeder.prepareForJob(true);
        }
        feeder.prepareForJob(false);

        Nozzle nozzle = getCompatibleNozzleAndTip(feeder, true);

        // Like in the JobProcessor, make sure it is calibrated.
        if (!nozzle.isCalibrated()) {
            nozzle.calibrate();
        }

        // Perform the feed.
        nozzle.moveToSafeZ();
        feeder.feed(nozzle);
        return nozzle;
    }

    /**
     * Perform a job-like feed and pick operations sequence. 
     * 
     * @param feeder
     * @throws Exception
     * @throws JobProcessorException
     */
    public static void pickFeeder(Feeder feeder) throws Exception, JobProcessorException {
        // Do the feed an get the nozzle for the pick.
        Nozzle nozzle = feedFeeder(feeder);

        // Perform the vacuum check, if enabled.
        if (nozzle.isPartOffEnabled(Nozzle.PartOffStep.BeforePick)) {
            // Part-off check can only be done at safe Z. An explicit move to safe Z is needed, because some feeder classes 
            // may move the nozzle to (near) the pick location i.e. down in Z in feed().
            nozzle.moveToSafeZ();
            if(!nozzle.isPartOff()) {
                throw new JobProcessorException(nozzle, "Part vacuum-detected on nozzle before pick.");
            }
        }

        // Make sure the nozzle can articulate from pick to placement. 
        Location placementLocation = getTestPlacementLocation(feeder.getPart());
        nozzle.prepareForPickAndPlaceArticulation(feeder.getPickLocation(), 
                placementLocation);

        // Go to the pick location and pick.
        nozzle.moveToPickLocation(feeder);
        nozzle.pick(feeder.getPart());
        nozzle.moveToSafeZ();

        // After the pick. 
        feeder.postPick(nozzle);

        // Perform the vacuum check, if enabled.
        if (nozzle.isPartOnEnabled(Nozzle.PartOnStep.AfterPick)) {
            if(!nozzle.isPartOn()) {
                throw new JobProcessorException(nozzle, "No part detected.");
            }
        }
        // The part is now on the nozzle.
        MovableUtils.fireTargetedUserAction(nozzle);
        if (MainFrame.get().getTabs().getSelectedComponent() == MainFrame.get().getFeedersTab() 
                && Configuration.get().getTablesLinked() == TablesLinked.Linked) {
            MainFrame.get().getPartsTab().selectPartInTableAndUpdateLinks(feeder.getPart());
        }
    }

    /**
     * Create a test placement location from the discard location and the test alignment angle. 
     * 
     * @param part
     * @return
     */
    public static Location getTestPlacementLocation(Part part) {
        PartAlignment aligner = AbstractPartAlignment.getPartAlignment(part);
        Location placementLocation = Configuration.get().getMachine().getDiscardLocation();
        placementLocation = new Location(placementLocation.getUnits(),
                placementLocation.getX(), 
                placementLocation.getY(), 
                placementLocation.getZ(), 
                (aligner instanceof ReferenceBottomVision ? 
                        ((ReferenceBottomVision)aligner).getTestAlignmentAngle() 
                        : 0.0));
        return placementLocation;
    }

    protected static Nozzle getCompatibleNozzleAndTip(Feeder feeder, boolean allowNozzleTipChange) throws Exception {
        if (feeder.getPart() == null) {
            throw new Exception("Feeder has not part set.");
        }
        // Check the nozzle tip package compatibility.
        Nozzle nozzle = MainFrame.get().getMachineControls().getSelectedNozzle();
        org.openpnp.model.Package packag = feeder.getPart().getPackage();
        if (nozzle.getNozzleTip() == null || 
                !packag.getCompatibleNozzleTips().contains(nozzle.getNozzleTip())) {
            // Wrong nozzle tip, try find one that works.
            Nozzle altNozzle = null;
            // Try find a good nozzle tip.
            for (NozzleTip nozzleTip : packag.getCompatibleNozzleTips()) {
                Nozzle nozzle2 = nozzleTip.getNozzleWhereLoaded();
                if (nozzle2 == null 
                        && nozzle.getCompatibleNozzleTips().contains(nozzleTip)) {
                    // Found a compatible one. 
                    if (nozzle.isNozzleTipChangedOnManualFeed() && allowNozzleTipChange) {
                        // Unload and load like the JobProcessor.
                        nozzle.unloadNozzleTip();
                        nozzle.loadNozzleTip(nozzleTip);
                        return nozzle; // Success.
                    }
                }
                if (altNozzle == null && nozzle2 != null){
                    altNozzle = nozzle2;
                }
            }
            String errMsg = "";
            if (nozzle.getNozzleTip() == null) {
                errMsg += "No nozzle tip loaded on nozzle "+nozzle.getName()+". ";
            }
            else {
                errMsg += "Nozzle "+nozzle.getName()+" loaded nozzle tip "+
                        nozzle.getNozzleTip().getName()+" is not compatible with package "+packag.getId()+". ";
                if (nozzle.getPart() != null) {
                    errMsg += "There is already a part "+nozzle.getPart().getId()+" loaded. "; 
                }
            }
            if (altNozzle != null) {
                errMsg += "Consider selecting nozzle "+altNozzle.getName()+", "
                        + "it has compatible nozzle tip "+altNozzle.getNozzleTip().getName()+" loaded. ";
            }
            else if (allowNozzleTipChange && !nozzle.isNozzleTipChangedOnManualFeed()) { 
                errMsg += "You may want to enable automatic nozzle tip change on manual pick on the "
                        + "Nozzle / Tool Changer. ";
            }
            errMsg += "The pick will always be performed with the nozzle selected in the Machine Controls. ";
            throw new Exception(errMsg);
        }
        return nozzle;
    }

    public Action moveCameraToPickLocation = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.centerCameraOnFeeder);
            putValue(NAME, Translations.getString("FeedersPanel.Action.MoveCameraToPick")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION,Translations.getString("FeedersPanel.Action.MoveCameraToPick.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                Feeder feeder = getSelection();
                Camera camera = MainFrame.get().getMachineControls().getSelectedTool().getHead()
                        .getDefaultCamera();
                Nozzle nozzle;
                try {
                    nozzle = getCompatibleNozzleAndTip(feeder, false);
                }
                catch (Exception e) {
                    nozzle = MainFrame.get().getMachineControls().getSelectedNozzle();
                }
                Location pickLocation = preliminaryPickLocation(feeder, nozzle);
                MovableUtils.moveToLocationAtSafeZ(camera, pickLocation);
                MovableUtils.fireTargetedUserAction(camera);
            });
        }
    };

    public Action moveToolToPickLocation = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.centerNozzleOnFeeder);
            putValue(NAME, Translations.getString("FeedersPanel.Action.MoveToolToPick")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION,Translations.getString("FeedersPanel.Action.MoveToolToPick.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                Feeder feeder = getSelection();
                Nozzle nozzle = getCompatibleNozzleAndTip(feeder, true);

                Location pickLocation = preliminaryPickLocation(feeder, nozzle);
                MovableUtils.moveToLocationAtSafeZ(nozzle, pickLocation);
                MovableUtils.fireTargetedUserAction(nozzle);
            });
        }
    };
    
    public final Action setEnabledAction = new AbstractAction() {
        {
            putValue(NAME, Translations.getString("FeedersPanel.Action.SetEnabled")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("FeedersPanel.Action.SetEnabled.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {}
    };

    class SetEnabledAction extends AbstractAction {
        final Boolean value;

        public SetEnabledAction(Boolean value) {
            this.value = value;
            String name = value ? "Enabled" : "Disabled";
            putValue(NAME, name);
            putValue(SHORT_DESCRIPTION, "Set board(s) enabled to " + value);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            for (Feeder f : getSelections()) {
                f.setEnabled(value);
            }
            table.repaint();
        }
    };

    public void selectFeederInTable(Feeder feeder) {
        Helpers.selectObjectTableRow(table, feeder);
    }
    public void selectFeederForPart(Part part) {
        if (getSelection() == null || getSelection().getPart() != part) {
            Feeder feeder = findFeeder(part, true);
            // Prefer enabled feeders but fall back to disabled ones.
            if (feeder == null) {
                feeder = findFeeder(part, false);
            }
            if (feeder != null) {
                Helpers.selectObjectTableRow(table, feeder);
            }
        }
    }
}
