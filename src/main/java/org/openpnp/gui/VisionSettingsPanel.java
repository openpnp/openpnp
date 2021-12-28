package org.openpnp.gui;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.table.TableRowSorter;

import org.openpnp.gui.components.AutoSelectTextTable;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.Wizard;
import org.openpnp.gui.support.WizardContainer;
import org.openpnp.gui.tablemodel.VisionSettingsTableModel;
import org.openpnp.machine.reference.vision.AbstractPartAlignment;
import org.openpnp.machine.reference.vision.ReferenceFiducialLocator;
import org.openpnp.model.AbstractVisionSettings;
import org.openpnp.model.BottomVisionSettings;
import org.openpnp.model.Configuration;
import org.openpnp.model.Configuration.VisionSettingsConfigurationHolder;
import org.openpnp.model.FiducialVisionSettings;
import org.openpnp.model.PartSettingsHolder;
import org.simpleframework.xml.Serializer;

public class VisionSettingsPanel extends JPanel implements WizardContainer {

    private static final String PREF_DIVIDER_POSITION = "VisionSettingsPanel.dividerPosition";
    private static final int PREF_DIVIDER_POSITION_DEF = -1;
    private Preferences prefs = Preferences.userNodeForPackage(VisionSettingsPanel.class);

    private final Frame frame;
    protected AbstractVisionSettings selectedVisionSettings;

    private VisionSettingsTableModel tableModel;
    private TableRowSorter<VisionSettingsTableModel> tableSorter;
    private JTable table;
    private JComboBox visionTypeFilter;

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

            AbstractVisionSettings selectedVisionSettings = getSelection();
            if (selectedVisionSettings != null) {
                this.selectedVisionSettings = selectedVisionSettings;
            }
            tabbedPane.removeAll();

            if (selectedVisionSettings != null) {
                Wizard wizard = selectedVisionSettings.getConfigurationWizard();
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
        tableModel.addTableModelListener(e -> {
            if (selectedVisionSettings != null) { 
                // Reselect previously selected settings.
                Helpers.selectObjectTableRow(table, selectedVisionSettings);
            }
        });
        filterTable();

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
        
        JPanel filterPanel = new JPanel();
        toolbarPanel.add(filterPanel, BorderLayout.EAST);
        
        JLabel lblFilterType = new JLabel("Type");
        filterPanel.add(lblFilterType);
        
        visionTypeFilter = new JComboBox(VisionTypeFilter.values());
        filterPanel.add(visionTypeFilter);
        visionTypeFilter.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent arg0) {
                filterTable();
            }
        });

        toolBar.add(newSettingsAction);
        toolBar.add(deleteSettingsAction);
        toolBar.addSeparator();
        toolBar.add(copyPackageToClipboardAction);
        toolBar.add(pastePackageToClipboardAction);

        toolBar.addSeparator();
    }

    protected enum VisionTypeFilter {
        BottomVision,
        FiducialVision
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
            selections.add(tableModel.getRowObjectAt(selectedRow));
        }
        return selections;
    }

    public final Action newSettingsAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.add);
            putValue(NAME, "New Settings");
            putValue(SHORT_DESCRIPTION, "Create a new Bottom Vision Settings.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            AbstractVisionSettings visionSettings = new BottomVisionSettings();
            visionSettings.setName(BottomVisionSettings.class.getSimpleName());
            Configuration.get().addVisionSettings(visionSettings);
            Helpers.selectObjectTableRow(table, visionSettings);
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

            List<PartSettingsHolder> usedIn = new ArrayList<>();
            for (AbstractVisionSettings settings : selections) {
                usedIn.addAll(settings.getUsedBottomVisionIn());
            }

            if (!usedIn.isEmpty()) {
                String errorNames = new AbstractVisionSettings.ListConverter(false).convertForward(usedIn);
                MessageBoxes.errorBox(getTopLevelAncestor(), "Error",
                        "The selection cannot be deleted. It is used by " + errorNames + ".");
                return;
            }

            List<String> names = selections.stream().map(AbstractVisionSettings::getName).collect(Collectors.toList());
            String formattedNames;
            if (names.size() <= 10) {
                formattedNames = String.join(", ", names);
            } else {
                formattedNames = String.join(", ", names.subList(0, 5)) + ", and " + (names.size() - 5) + " others";
            }

            int ret = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                    "Are you sure you want to delete " + formattedNames + "?",
                    "Delete " + selections.size() + " vision settings?", JOptionPane.YES_NO_OPTION);
            if (ret == JOptionPane.YES_OPTION) {
                for (AbstractVisionSettings visionSettings : selections) {
                    Configuration.get().removeVisionSettings(visionSettings);
                }
            }
        }
    };

    public final Action copyPackageToClipboardAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.copy);
            putValue(NAME, "Copy Vision Settings to Clipboard");
            putValue(SHORT_DESCRIPTION,
                    "Copy the currently selected vision settings to the clipboard in text format.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            List<AbstractVisionSettings> visionSettings = getSelections();
            if (visionSettings.isEmpty()) {
                return;
            }
            try {
                VisionSettingsConfigurationHolder holder = new VisionSettingsConfigurationHolder();
                holder.visionSettings.addAll(visionSettings);
                Serializer s = Configuration.createSerializer();
                StringWriter w = new StringWriter();
                s.write(holder, w);
                StringSelection stringSelection = new StringSelection(w.toString());
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(stringSelection, null);
            }
            catch (Exception e) {
                MessageBoxes.errorBox(getTopLevelAncestor(), "Copy Failed", e);
            }
        }
    };

    public final Action pastePackageToClipboardAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.paste);
            putValue(NAME, "Create Vision Settings from Clipboard");
            putValue(SHORT_DESCRIPTION, "Create a new vision setting from a definition on the clipboard.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            try {
                Serializer ser = Configuration.createSerializer();
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                String s = (String) clipboard.getData(DataFlavor.stringFlavor);
                StringReader r = new StringReader(s);
                VisionSettingsConfigurationHolder holder = ser.read(VisionSettingsConfigurationHolder.class, s);
                table.clearSelection();
                for (AbstractVisionSettings visionSettings : holder.visionSettings) {
                    visionSettings.setId(Configuration.createId(visionSettings.getId().substring(0, 3)));
                    for (AbstractVisionSettings visionSettings2 : Configuration.get().getVisionSettings()) {
                        if (visionSettings2.getName().equals(visionSettings.getName())) {
                            visionSettings.setName(visionSettings+" (Copy)");
                            break;
                        }
                    }
                    Configuration.get().addVisionSettings(visionSettings);
                }
                Helpers.selectObjectTableRows(table, holder.visionSettings);
            }
            catch (Exception e) {
                MessageBoxes.errorBox(getTopLevelAncestor(), "Paste Failed", e);
            }
        }
    };

    @Override
    public void wizardCompleted(Wizard wizard) {

    }

    @Override
    public void wizardCancelled(Wizard wizard) {

    }

    public void selectVisionSettingsInTable(PartSettingsHolder partSettingsHolder) {
        final VisionTypeFilter filterType = (VisionTypeFilter) visionTypeFilter.getSelectedItem();
        AbstractVisionSettings visionSettings = null;
        switch (filterType) {
            case BottomVision:
                visionSettings = AbstractPartAlignment.getInheritedVisionSettings(partSettingsHolder, true);
                break;
            case FiducialVision:
                visionSettings = ReferenceFiducialLocator.getDefault().getInheritedVisionSettings(partSettingsHolder);
                break;
        }
        selectVisionSettingsInTable(visionSettings);
    }

    public void selectVisionSettingsInTable(AbstractVisionSettings visionSettings) {
        if (getSelection() != visionSettings) {
            Helpers.selectObjectTableRow(table, visionSettings);
        }
    }

    protected void filterTable() {
        final VisionTypeFilter filterType = (VisionTypeFilter) visionTypeFilter.getSelectedItem();
        tableSorter.setRowFilter(new RowFilter<VisionSettingsTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends VisionSettingsTableModel, ? extends Integer> entry) {
                AbstractVisionSettings visionSettings = tableModel.getRowObjectAt(entry.getIdentifier());
                switch (filterType) {
                    case BottomVision:
                        return visionSettings instanceof BottomVisionSettings;
                    case FiducialVision:
                        return visionSettings instanceof FiducialVisionSettings;
                    default: 
                        return false;
                }
            }
        });
    }
}
