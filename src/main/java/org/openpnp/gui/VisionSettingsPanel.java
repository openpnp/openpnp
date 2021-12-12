package org.openpnp.gui;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableRowSorter;

import org.openpnp.gui.components.AutoSelectTextTable;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.Wizard;
import org.openpnp.gui.support.WizardContainer;
import org.openpnp.gui.tablemodel.VisionSettingsTableModel;
import org.openpnp.model.AbstractVisionSettings;
import org.openpnp.model.BottomVisionSettings;
import org.openpnp.model.Configuration;

public class VisionSettingsPanel extends JPanel implements WizardContainer {

    private static final String PREF_DIVIDER_POSITION = "VisionSettingsPanel.dividerPosition";
    private static final int PREF_DIVIDER_POSITION_DEF = -1;
    private Preferences prefs = Preferences.userNodeForPackage(VisionSettingsPanel.class);

    private final Frame frame;

    private VisionSettingsTableModel tableModel;
    private TableRowSorter<VisionSettingsTableModel> tableSorter;
    private JTable table;

    public VisionSettingsPanel(Frame frame) {
        this.frame = frame;

        setLayout(new BorderLayout(0, 0));

        createAndAddToolbar();

        tableModel = new VisionSettingsTableModel();
        tableSorter = new TableRowSorter<>(tableModel);

        JSplitPane splitPane = new JSplitPane();
        splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        splitPane.setContinuousLayout(true);
        splitPane.setDividerLocation(prefs.getInt(PREF_DIVIDER_POSITION, PREF_DIVIDER_POSITION_DEF));
        splitPane.addPropertyChangeListener("dividerLocation", evt -> prefs.putInt(PREF_DIVIDER_POSITION, splitPane.getDividerLocation()));
        add(splitPane, BorderLayout.CENTER);

        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);

        table = new AutoSelectTextTable(tableModel);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        table.setRowSorter(tableSorter);
        table.getTableHeader().setDefaultRenderer(new MultisortTableHeaderCellRenderer());

        table.getColumnModel().getColumn(0).setPreferredWidth(200);
        table.getColumnModel().getColumn(1).setPreferredWidth(600);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }

            AbstractVisionSettings visionSettings = getSelection();
            tabbedPane.removeAll();

            if (visionSettings != null) {
                Wizard wizard = visionSettings.getConfigurationWizard();
                if (wizard != null) {
                    JPanel panel = new JPanel();
                    panel.setLayout(new BorderLayout());
                    panel.add(wizard.getWizardPanel());
                    tabbedPane.add(wizard.getWizardName(), new JScrollPane(panel));
                    wizard.setWizardContainer(VisionSettingsPanel.this);
                }
            }
            revalidate();
            repaint();
        });

        splitPane.setLeftComponent(new JScrollPane(table));
        splitPane.setRightComponent(tabbedPane);
    }

    private void createAndAddToolbar() {
        JPanel toolbarPanel = new JPanel();
        add(toolbarPanel, BorderLayout.NORTH);
        toolbarPanel.setLayout(new BorderLayout(0, 0));

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolbarPanel.add(toolBar);

        JPanel upperPanel = new JPanel();
        toolbarPanel.add(upperPanel, BorderLayout.EAST);

        toolBar.add(newSettingsAction);
        toolBar.add(deleteSettingsAction);

        toolBar.addSeparator();
    }

    private AbstractVisionSettings getSelection() {
        List<AbstractVisionSettings> selections = getSelections();
        if (selections.size() != 1) {
            return null;
        }
        return selections.get(0);
    }

    private List<AbstractVisionSettings> getSelections() {
        List<AbstractVisionSettings> selections = new ArrayList<>();
        for (int selectedRow : table.getSelectedRows()) {
            selectedRow = table.convertRowIndexToModel(selectedRow);
            selections.add(tableModel.getVisionSettings(selectedRow));
        }
        return selections;
    }

    public final Action newSettingsAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.add);
            putValue(NAME, "New Settings...");
            putValue(SHORT_DESCRIPTION, "Create a new settings, specifying it's ID.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String id;
            while ((id = JOptionPane.showInputDialog(frame,
                    "Please enter an ID for the new settings.")) != null) {
                if (Configuration.get().getBottomVisionSettings(id) == null) {
                    AbstractVisionSettings visionSettings = new BottomVisionSettings(id);

                    Configuration.get().addVisionSettings(visionSettings);
                    tableModel.fireTableDataChanged();
                    Helpers.selectLastTableRow(table);
                    break;
                }

                MessageBoxes.errorBox(frame, "Error", "VisionSettings ID " + id + " already exists.");
            }
        }
    };

    public final Action deleteSettingsAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.delete);
            putValue(NAME, "Delete Settings");
            putValue(SHORT_DESCRIPTION, "Delete the currently selected settings.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            List<AbstractVisionSettings> selections = getSelections();

            List<String> usedIn = new ArrayList<>();
            for (AbstractVisionSettings settings : selections) {
                usedIn.addAll(settings.getUsedIn());
            }

            if (!usedIn.isEmpty()) {
                String errorIds = String.join(", ", usedIn);
                MessageBoxes.errorBox(getTopLevelAncestor(), "Error",
                        "The selection cannot be deleted. It is used by " + errorIds);
                return;
            }

            List<String> ids = selections.stream().map(AbstractVisionSettings::getId).collect(Collectors.toList());
            String formattedIds;
            if (ids.size() <= 3) {
                formattedIds = String.join(", ", ids);
            } else {
                formattedIds = String.join(", ", ids.subList(0, 3)) + ", and " + (ids.size() - 3) + " others";
            }

            int ret = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                    "Are you sure you want to delete " + formattedIds + "?",
                    "Delete " + selections.size() + " vision settings?", JOptionPane.YES_NO_OPTION);
            if (ret == JOptionPane.YES_OPTION) {
                for (AbstractVisionSettings visionSettings : selections) {
                    Configuration.get().removeVisionSettings(visionSettings);
                }
            }
        }
    };

    @Override
    public void wizardCompleted(Wizard wizard) {

    }

    @Override
    public void wizardCancelled(Wizard wizard) {

    }
}
