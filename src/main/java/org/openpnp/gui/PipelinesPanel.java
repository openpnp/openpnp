package org.openpnp.gui;

import org.openpnp.gui.components.AutoSelectTextTable;
import org.openpnp.gui.support.*;
import org.openpnp.gui.tablemodel.PipelinesTableModel;
import org.openpnp.model.Configuration;
import org.openpnp.model.Package;
import org.openpnp.model.Pipeline;
import org.openpnp.spi.PartAlignment;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

public class PipelinesPanel extends JPanel implements WizardContainer {

    private static final String PREF_DIVIDER_POSITION = "PackagesPanel.dividerPosition";
    private static final int PREF_DIVIDER_POSITION_DEF = -1;
    private Preferences prefs = Preferences.userNodeForPackage(PipelinesPanel.class);

    private PipelinesTableModel tableModel;
    private TableRowSorter<PipelinesTableModel> tableSorter;
    private JTable table;
    private ActionGroup singleSelectionActionGroup;
    private ActionGroup multiSelectionActionGroup;

    public PipelinesPanel(Configuration configuration, Frame frame) {

        singleSelectionActionGroup = new ActionGroup(deletePipelineAction, pickPipelineAction, copyPipelineToClipboardAction);
        singleSelectionActionGroup.setEnabled(false);
        multiSelectionActionGroup = new ActionGroup(deletePipelineAction);
        multiSelectionActionGroup.setEnabled(false);

        setLayout(new BorderLayout(0, 0));
        tableModel = new PipelinesTableModel();
        tableSorter = new TableRowSorter<>(tableModel);

//        JComboBox pipelinesCombo = new JComboBox(new PackagesComboBoxModel<Pipeline>());
//        pipelinesCombo.setRenderer(new IdentifiableListCellRenderer<Pipeline>());

        JSplitPane splitPane = new JSplitPane();
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

        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);

        table = new AutoSelectTextTable(tableModel);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
//        table.setDefaultEditor(org.openpnp.model.Package.class,
//                new DefaultCellEditor(pipelinesCombo));
//        table.setDefaultRenderer(org.openpnp.model.Package.class,
//                new IdentifiableTableCellRenderer<Package>());

        table.setRowSorter(tableSorter);
        table.getTableHeader().setDefaultRenderer(new MultisortTableHeaderCellRenderer());
        splitPane.setLeftComponent(new JScrollPane(table));
        splitPane.setRightComponent(tabbedPane);

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
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
            }
        });
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

    public final Action deletePipelineAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {

        }
    };

    public final Action pickPipelineAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {

        }
    };

    public final Action copyPipelineToClipboardAction = new AbstractAction() {
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
