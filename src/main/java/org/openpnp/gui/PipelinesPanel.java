package org.openpnp.gui;

import org.openpnp.gui.components.AutoSelectTextTable;
import org.openpnp.gui.support.*;
import org.openpnp.gui.tablemodel.PipelinesTableModel;
import org.openpnp.model.Configuration;
import org.openpnp.model.Pipeline;
import org.openpnp.spi.PartAlignment;

import javax.swing.*;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

public class PipelinesPanel extends JPanel implements WizardContainer {

    private static final String PREF_DIVIDER_POSITION = "PackagesPanel.dividerPosition";
    private static final int PREF_DIVIDER_POSITION_DEF = -1;
    private Preferences prefs = Preferences.userNodeForPackage(PipelinesPanel.class);

    private final Frame frame;

    private PipelinesTableModel tableModel;
    private TableRowSorter<PipelinesTableModel> tableSorter;
    private JTable table;
    private ActionGroup singleSelectionActionGroup;
    private ActionGroup multiSelectionActionGroup;

    public PipelinesPanel(Configuration configuration, Frame frame) {
        this.frame = frame;

        singleSelectionActionGroup = new ActionGroup(deletePipelineAction, pastePipelineFromClipboardAction, copyPipelineToClipboardAction);
        singleSelectionActionGroup.setEnabled(false);
        multiSelectionActionGroup = new ActionGroup(deletePipelineAction);
        multiSelectionActionGroup.setEnabled(false);

        setLayout(new BorderLayout(0, 0));

        createAndAddToolbar();

        tableModel = new PipelinesTableModel();
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

        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }

            List<Pipeline> selections = getSelections();

            if (selections.size() > 1) {
                singleSelectionActionGroup.setEnabled(false);
                multiSelectionActionGroup.setEnabled(true);
            } else {
                multiSelectionActionGroup.setEnabled(false);
                singleSelectionActionGroup.setEnabled(!selections.isEmpty());
            }

            Pipeline pipeline = getSelection();

            int selectedTab = tabbedPane.getSelectedIndex();
            tabbedPane.removeAll();

            if (pipeline != null) {
                PartAlignment vision = Configuration.get().getMachine().getPartAlignments().get(0);
                Wizard wizard = vision.getPipelineConfigurationWizard(pipeline);
                if (wizard != null) {
                    JPanel panel = new JPanel();
                    panel.setLayout(new BorderLayout());
                    panel.add(wizard.getWizardPanel());
                    tabbedPane.add(wizard.getWizardName(), new JScrollPane(panel));
                    wizard.setWizardContainer(PipelinesPanel.this);
                }
            }

            if (selectedTab >= 0 && selectedTab < tabbedPane.getTabCount()) {
                tabbedPane.setSelectedIndex(selectedTab);
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

        toolBar.add(newPipelineAction);
        toolBar.add(deletePipelineAction);

        toolBar.addSeparator();

        JButton copyToClipboardButton = new JButton(copyPipelineToClipboardAction);
        copyToClipboardButton.setHideActionText(true);
        toolBar.add(copyToClipboardButton);

        JButton pasteFromClipboardButton = new JButton(pastePipelineFromClipboardAction);
        pasteFromClipboardButton.setHideActionText(true);
        toolBar.add(pasteFromClipboardButton);
    }

    private Pipeline getSelection() {
        List<Pipeline> selections = getSelections();
        if (selections.size() != 1) {
            return null;
        }
        return selections.get(0);
    }

    private List<Pipeline> getSelections() {
        List<Pipeline> selections = new ArrayList<>();
        for (int selectedRow : table.getSelectedRows()) {
            selectedRow = table.convertRowIndexToModel(selectedRow);
            selections.add(tableModel.getPipeline(selectedRow));
        }
        return selections;
    }

    public final Action newPipelineAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.add);
            putValue(NAME, "New Pipeline...");
            putValue(SHORT_DESCRIPTION, "Create a new pipeline, specifying it's ID.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String id;
            while ((id = JOptionPane.showInputDialog(frame,
                    "Please enter an ID for the new pipeline.")) != null) {
                if (Configuration.get().getPipeline(id) == null) {
                    Pipeline pipeline = new Pipeline(id);

                    Configuration.get().addPipeline(pipeline);
                    tableModel.fireTableDataChanged();
                    Helpers.selectLastTableRow(table);
                    break;
                }

                MessageBoxes.errorBox(frame, "Error", "Pipeline ID " + id + " already exists.");
            }
        }
    };

    public final Action deletePipelineAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.delete);
            putValue(NAME, "Delete Pipeline");
            putValue(SHORT_DESCRIPTION, "Delete the currently selected pipeline.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            List<Pipeline> selections = getSelections();
            List<String> ids = selections.stream().map(Pipeline::getId).collect(Collectors.toList());

            String formattedIds;
            if (ids.size() <= 3) {
                formattedIds = String.join(", ", ids);
            } else {
                formattedIds = String.join(", ", ids.subList(0, 3)) + ", and " + (ids.size() - 3) + " others";
            }

            int ret = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                    "Are you sure you want to delete " + formattedIds + "?",
                    "Delete " + selections.size() + " pipelines?", JOptionPane.YES_NO_OPTION);
            if (ret == JOptionPane.YES_OPTION) {
                for (Pipeline pipeline : selections) {
                    Configuration.get().removePipeline(pipeline);
                }
            }
        }
    };

    public final Action copyPipelineToClipboardAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.copy);
            putValue(NAME, "Copy Pipeline to Clipboard");
            putValue(SHORT_DESCRIPTION,
                    "Copy the currently selected pipeline to the clipboard in text format.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {

        }
    };

    public final Action pastePipelineFromClipboardAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.paste);
            putValue(NAME, "Create Pipeline from Clipboard");
            putValue(SHORT_DESCRIPTION, "Create a new pipeline from a definition on the clipboard.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {

        }
    };

    @Override
    public void wizardCompleted(Wizard wizard) {

    }

    @Override
    public void wizardCancelled(Wizard wizard) {

    }
}
