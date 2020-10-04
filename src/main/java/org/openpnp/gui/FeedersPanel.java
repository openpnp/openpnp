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
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
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

import org.openpnp.events.BoardLocationSelectedEvent;
import org.openpnp.events.FeederSelectedEvent;
import org.openpnp.gui.JobPanel.SetCheckFidsAction;
import org.openpnp.gui.JobPanel.SetEnabledAction;
import org.openpnp.gui.JobPanel.SetSideAction;
import org.openpnp.gui.components.AutoSelectTextTable;
import org.openpnp.gui.components.ClassSelectionDialog;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.ActionGroup;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.Wizard;
import org.openpnp.gui.support.WizardContainer;
import org.openpnp.gui.tablemodel.FeedersTableModel;
import org.openpnp.model.Board;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.openpnp.spi.JobProcessor.JobProcessorException;
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
                            AbstractConfigurationWizard wizard = (AbstractConfigurationWizard) ps.getPropertySheetPanel();
                            wizard.setWizardContainer(FeedersPanel.this);
                            configurationPanel.addTab(ps.getPropertySheetTitle(), wizard);
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
        boolean feederConfigurationIsDirty = false;
        int i = 0;
        while (!feederConfigurationIsDirty && (i<configurationPanel.getComponentCount())) {
            feederConfigurationIsDirty = ((AbstractConfigurationWizard) configurationPanel.getComponent(i)).isDirty();
            i++;
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
				    int j = 0;
				    while ((j<configurationPanel.getComponentCount())) {
				    	AbstractConfigurationWizard wizard = ((AbstractConfigurationWizard) configurationPanel.getComponent(j));
				        if (wizard.isDirty()) {
							wizard.apply();
				        }
				        j++;
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
                if (tableModel.getFeeder(i) == event.feeder) {
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
            table.getSelectionModel().clearSelection();
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                if (tableModel.getFeeder(i) == feeder) {
                    int index = table.convertRowIndexToView(i);
                    table.getSelectionModel().setSelectionInterval(index, index);
                    table.scrollRectToVisible(new Rectangle(table.getCellRect(index, 0, true)));
                    break;
                }
            }
        }
    }

    private Feeder findFeeder(Part part, boolean enabled) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Feeder feeder = tableModel.getFeeder(i); 
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
            Logger.warn("Search failed", e);
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
            title = "Select Feeder...";
        }
        else {
            title = "Select Feeder for " + part.getId() + "...";
        }
        ClassSelectionDialog<Feeder> dialog =
                new ClassSelectionDialog<>(JOptionPane.getFrameForComponent(FeedersPanel.this),
                        title, "Please select a Feeder implemention from the list below.",
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

    public Action newFeederAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.add);
            putValue(NAME, "New Feeder...");
            putValue(SHORT_DESCRIPTION, "Create a new feeder.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            newFeeder(null);
        }
    };

    public Action deleteFeederAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.delete);
            putValue(NAME, "Delete Feeder");
            putValue(SHORT_DESCRIPTION, "Delete the selected feeder.");
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
                    "Are you sure you want to delete " + formattedIds + "?",
                    "Delete " + selections.size() + " feeders?", JOptionPane.YES_NO_OPTION);
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
            putValue(NAME, "Feed");
            putValue(SHORT_DESCRIPTION, "Command the selected feeder to perform a feed operation.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                Feeder feeder = getSelection();
                Nozzle nozzle = MainFrame.get().getMachineControls().getSelectedNozzle();

                nozzle.moveToSafeZ();
                feeder.feed(nozzle);
                Location pickLocation = feeder.getPickLocation();
                MovableUtils.moveToLocationAtSafeZ(nozzle, pickLocation);
            });
        }
    };

    public Action pickFeederAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.pick);
            putValue(NAME, "Pick");
            putValue(SHORT_DESCRIPTION, "Perform a feed and pick on the selected feeder.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                Feeder feeder = getSelection();

                pickFeeder(feeder);
            });
        }
    };

    public static void pickFeeder(Feeder feeder) throws Exception, JobProcessorException {
        // Simulate a "one feeder" job, prepare the feeder.
        if (feeder.getJobPreparationLocation() != null) {
            feeder.prepareForJob(true);
        }
        feeder.prepareForJob(false);

        // Check the nozzle tip package compatibility.
        Nozzle nozzle = MainFrame.get().getMachineControls().getSelectedNozzle();
        org.openpnp.model.Package packag = feeder.getPart().getPackage();
        if (nozzle.getNozzleTip() == null || 
                !packag.getCompatibleNozzleTips().contains(nozzle.getNozzleTip())) {
            // Wrong nozzle tip, try find one that works.
            boolean resolved = false;
            if (nozzle.isNozzleTipChangedOnManualFeed()) {
                for (NozzleTip nozzleTip : packag.getCompatibleNozzleTips()) {
                    if (nozzle.getCompatibleNozzleTips().contains(nozzleTip)) {
                        // Found a compatible one. Unload and load like the JobProcessor.
                        nozzle.unloadNozzleTip();
                        nozzle.loadNozzleTip(nozzleTip);
                        resolved = true;
                        break;
                    }
                }
            }
            if (nozzle.getNozzleTip() == null) {
                throw new Exception("Can't pick, no nozzle tip loaded on nozzle "+nozzle.getName()+". "
                        +"You may want to enable automatic nozzle tip change on manual pick on the Nozzle / Tool Changer.");
            }
            else if (! resolved) {
                throw new Exception("Can't pick, loaded nozzle tip "+
                        nozzle.getNozzleTip().getName()+" is not compatible with package "+packag.getId()+". "
                        +"You may want to enable automatic nozzle tip change on manual pick on the Nozzle / Tool Changer.");
            }
        }

        // Like in the JobProcessor, make sure it is calibrated.
        if (!nozzle.isCalibrated()) {
            nozzle.calibrate();
        }

        // Perform the feed.
        nozzle.moveToSafeZ();
        feeder.feed(nozzle);

        // Go to the pick location and pick.
        Location pickLocation = feeder.getPickLocation();
        MovableUtils.moveToLocationAtSafeZ(nozzle, pickLocation);
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
    }

    public Action moveCameraToPickLocation = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.centerCameraOnFeeder);
            putValue(NAME, "Move Camera");
            putValue(SHORT_DESCRIPTION,
                    "Move the camera to the selected feeder's current pick location.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                Feeder feeder = getSelection();
                Camera camera = MainFrame.get().getMachineControls().getSelectedTool().getHead()
                        .getDefaultCamera();
                Location pickLocation = feeder.getPickLocation();
                MovableUtils.moveToLocationAtSafeZ(camera, pickLocation);
            });
        }
    };

    public Action moveToolToPickLocation = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.centerNozzleOnFeeder);
            putValue(NAME, "Move Tool");
            putValue(SHORT_DESCRIPTION,
                    "Move the tool to the selected feeder's current pick location.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                Feeder feeder = getSelection();
                Nozzle nozzle = MainFrame.get().getMachineControls().getSelectedNozzle();

                Location pickLocation = feeder.getPickLocation();
                MovableUtils.moveToLocationAtSafeZ(nozzle, pickLocation);
            });
        }
    };
    
    public final Action setEnabledAction = new AbstractAction() {
        {
            putValue(NAME, "Set Enabled");
            putValue(SHORT_DESCRIPTION, "Set board(s) enabled to...");
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
}
