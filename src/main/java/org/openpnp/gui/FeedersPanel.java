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
import java.awt.event.ActionEvent;
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
import org.openpnp.gui.components.ClassSelectionDialog;
import org.openpnp.gui.support.ActionGroup;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.Wizard;
import org.openpnp.gui.support.WizardContainer;
import org.openpnp.gui.tablemodel.FeedersTableModel;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Nozzle;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.UiUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class FeedersPanel extends JPanel implements WizardContainer {
    private final static Logger logger = LoggerFactory.getLogger(FeedersPanel.class);

    private final Configuration configuration;
    private final MainFrame mainFrame;

    private static final String PREF_DIVIDER_POSITION = "FeedersPanel.dividerPosition";
    private static final int PREF_DIVIDER_POSITION_DEF = -1;

    private JTable table;

    private FeedersTableModel tableModel;
    private TableRowSorter<FeedersTableModel> tableSorter;
    private JTextField searchTextField;
    private JPanel configurationPanel;

    private ActionGroup feederSelectedActionGroup;

    private Preferences prefs = Preferences.userNodeForPackage(FeedersPanel.class);

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
        toolBar.add(feedFeederAction);
        toolBar.add(moveCameraToPickLocation);
        toolBar.add(moveToolToPickLocation);
        toolBar.add(pickFeederAction);

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

        final JSplitPane splitPane = new JSplitPane();
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

        table.setRowSorter(tableSorter);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        configurationPanel = new JPanel();
        configurationPanel.setBorder(new TitledBorder(null, "Configuration", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));

        feederSelectedActionGroup = new ActionGroup(deleteFeederAction, feedFeederAction,
                pickFeederAction, moveCameraToPickLocation, moveToolToPickLocation);

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }

                Feeder feeder = getSelectedFeeder();

                feederSelectedActionGroup.setEnabled(feeder != null);

                configurationPanel.removeAll();
                if (feeder != null) {
                    Wizard wizard = feeder.getConfigurationWizard();
                    if (wizard != null) {
                        wizard.setWizardContainer(FeedersPanel.this);
                        JPanel panel = wizard.getWizardPanel();
                        configurationPanel.add(panel);
                    }
                }
                revalidate();
                repaint();
            }
        });

        feederSelectedActionGroup.setEnabled(false);

        splitPane.setLeftComponent(new JScrollPane(table));
        splitPane.setRightComponent(configurationPanel);
        configurationPanel.setLayout(new BorderLayout(0, 0));
    }

    /**
     * Activate the Feeders tab and show the Feeder for the specified Part. If none exists, prompt
     * the user to create a new one.
     * 
     * @param feeder
     */
    public void showFeederForPart(Part part) {
        mainFrame.showTab("Feeders");

        Feeder feeder = findFeeder(part);
        if (feeder == null) {
            newFeeder(part);
        }
        else {
            table.getSelectionModel().clearSelection();
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                if (tableModel.getFeeder(i).getPart() == part) {
                    table.getSelectionModel().setSelectionInterval(0, i);
                    break;
                }
            }
        }
    }

    private Feeder findFeeder(Part part) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (tableModel.getFeeder(i).getPart() == part) {
                return tableModel.getFeeder(i);
            }
        }
        return null;
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
        // Repaint the table so that any changed fields get updated.
        table.repaint();
    }

    @Override
    public void wizardCancelled(Wizard wizard) {}

    private void newFeeder(Part part) {
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
            Feeder feeder = feederClass.newInstance();

            feeder.setPart(part == null ? Configuration.get().getParts().get(0) : part);

            configuration.getMachine().addFeeder(feeder);
            tableModel.refresh();
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
            int ret = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                    "Are you sure you want to delete " + getSelectedFeeder().getName() + "?",
                    "Delete " + getSelectedFeeder().getName() + "?", JOptionPane.YES_NO_OPTION);
            if (ret == JOptionPane.YES_OPTION) {
                configuration.getMachine().removeFeeder(getSelectedFeeder());
                tableModel.refresh();
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
            new Thread() {
                public void run() {
                    Feeder feeder = getSelectedFeeder();
                    Nozzle nozzle = MainFrame.machineControlsPanel.getSelectedNozzle();

                    try {
                        nozzle.moveToSafeZ();
                        feeder.feed(nozzle);
                        Location pickLocation = feeder.getPickLocation();
                        MovableUtils.moveToLocationAtSafeZ(nozzle, pickLocation);
                    }
                    catch (Exception e) {
                        MessageBoxes.errorBox(FeedersPanel.this, "Feed Error", e);
                    }
                }
            }.start();
        }
    };

    public Action pickFeederAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.load);
            putValue(NAME, "Pick");
            putValue(SHORT_DESCRIPTION, "Perform a feed and pick on the selected feeder.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            new Thread() {
                public void run() {
                    Feeder feeder = getSelectedFeeder();
                    Nozzle nozzle = MainFrame.machineControlsPanel.getSelectedNozzle();

                    try {
                        nozzle.moveToSafeZ();
                        feeder.feed(nozzle);
                        Location pickLocation = feeder.getPickLocation();
                        MovableUtils.moveToLocationAtSafeZ(nozzle, pickLocation);
                        nozzle.pick(feeder.getPart());
                        nozzle.moveToSafeZ();
                    }
                    catch (Exception e) {
                        MessageBoxes.errorBox(FeedersPanel.this, "Feed Error", e);
                    }
                }
            }.start();
        }
    };

    public Action moveCameraToPickLocation = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.centerCamera);
            putValue(NAME, "Move Camera");
            putValue(SHORT_DESCRIPTION,
                    "Move the camera to the selected feeder's current pick location.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                Feeder feeder = getSelectedFeeder();
                Camera camera = MainFrame.machineControlsPanel.getSelectedTool().getHead()
                        .getDefaultCamera();
                Location pickLocation = feeder.getPickLocation();
                MovableUtils.moveToLocationAtSafeZ(camera, pickLocation);
            });
        }
    };

    public Action moveToolToPickLocation = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.centerTool);
            putValue(NAME, "Move Tool");
            putValue(SHORT_DESCRIPTION,
                    "Move the tool to the selected feeder's current pick location.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            new Thread() {
                public void run() {
                    Feeder feeder = getSelectedFeeder();
                    Nozzle nozzle = MainFrame.machineControlsPanel.getSelectedNozzle();

                    try {
                        Location pickLocation = feeder.getPickLocation();
                        MovableUtils.moveToLocationAtSafeZ(nozzle, pickLocation);
                    }
                    catch (Exception e) {
                        MessageBoxes.errorBox(FeedersPanel.this, "Movement Error", e);
                    }
                }
            }.start();
        }
    };
}
