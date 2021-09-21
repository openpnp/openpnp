/*
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 *
 * This file is package of OpenPnP.
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
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableRowSorter;

import org.openpnp.gui.components.AutoSelectTextTable;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.support.*;
import org.openpnp.gui.tablemodel.PackagesTableModel;
import org.openpnp.model.Configuration;
import org.openpnp.model.Package;
import org.openpnp.model.Part;
import org.openpnp.model.Pipeline;
import org.openpnp.spi.Camera;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Serializer;

import static javax.swing.SwingConstants.TOP;

@SuppressWarnings("serial")
public class PackagesPanel extends JPanel {


    private static final String PREF_DIVIDER_POSITION = "PackagesPanel.dividerPosition";
    private static final int PREF_DIVIDER_POSITION_DEF = -1;
    private Preferences prefs = Preferences.userNodeForPackage(PackagesPanel.class);

    private final Configuration configuration;
    private final Frame frame;

    private PackagesTableModel tableModel;
    private TableRowSorter<PackagesTableModel> tableSorter;
    private JTextField searchTextField;
    private JTable table;
    private ActionGroup singleSelectionActionGroup;
    private ActionGroup multiSelectionActionGroup;
    private JTabbedPane tabbedPane;

    public PackagesPanel(Configuration configuration, Frame frame) {
        this.configuration = configuration;
        this.frame = frame;

        singleSelectionActionGroup = new ActionGroup(deletePackageAction, copyPackageToClipboardAction);
        singleSelectionActionGroup.setEnabled(false);
        multiSelectionActionGroup = new ActionGroup(deletePackageAction);
        multiSelectionActionGroup.setEnabled(false);

        setLayout(new BorderLayout(0, 0));

        createAndAddToolbar();

        tableModel = new PackagesTableModel();
        tableSorter = new TableRowSorter<>(tableModel);

        JSplitPane splitPane = new JSplitPane();
        splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        splitPane.setContinuousLayout(true);
        splitPane
                .setDividerLocation(prefs.getInt(PREF_DIVIDER_POSITION, PREF_DIVIDER_POSITION_DEF));
        splitPane.addPropertyChangeListener("dividerLocation",
                evt -> prefs.putInt(PREF_DIVIDER_POSITION, splitPane.getDividerLocation()));
        add(splitPane, BorderLayout.CENTER);

        tabbedPane = new JTabbedPane(TOP);

        tableSetup();

        splitPane.setLeftComponent(new JScrollPane(table));
        splitPane.setRightComponent(tabbedPane);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentHidden(ComponentEvent e) {
                try {
                    Camera camera =
                            Configuration.get().getMachine().getDefaultHead().getDefaultCamera();
                    CameraView cameraView = MainFrame.get().getCameraViews().getCameraView(camera);
                    if (cameraView == null) {
                        return;
                    }
                    cameraView.removeReticle(PackageVisionPanel.class.getName());
                } catch (Exception e1) {
                }
            }
        });
    }

    private void createAndAddToolbar() {
        JPanel toolbarAndSearch = new JPanel();
        add(toolbarAndSearch, BorderLayout.NORTH);
        toolbarAndSearch.setLayout(new BorderLayout(0, 0));

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolbarAndSearch.add(toolBar);

        JPanel upperPanel = new JPanel();
        toolbarAndSearch.add(upperPanel, BorderLayout.EAST);

        JLabel lblSearch = new JLabel("Search");
        upperPanel.add(lblSearch);

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
        upperPanel.add(searchTextField);
        searchTextField.setColumns(15);

        toolBar.add(newPackageAction);
        toolBar.add(deletePackageAction);
        toolBar.addSeparator();
        toolBar.add(copyPackageToClipboardAction);
        toolBar.add(pastePackageFromClipboardAction);
    }

    private void tableSetup() {
        JComboBox<Pipeline> pipelinesCombo = new JComboBox<>(new PipelinesComboBoxModel());
        pipelinesCombo.setMaximumRowCount(20);
        pipelinesCombo.setRenderer(new IdentifiableListCellRenderer<>());

        table = new AutoSelectTextTable(tableModel);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        table.setDefaultEditor(Pipeline.class,
                new DefaultCellEditor(pipelinesCombo));
        table.setDefaultRenderer(Pipeline.class,
                new IdentifiableTableCellRenderer<Pipeline>());

        table.setRowSorter(tableSorter);
        table.getTableHeader().setDefaultRenderer(new MultisortTableHeaderCellRenderer());

        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }

            List<Package> selections = getSelections();

            if (selections.size() > 1) {
                singleSelectionActionGroup.setEnabled(false);
                multiSelectionActionGroup.setEnabled(true);
            } else {
                multiSelectionActionGroup.setEnabled(false);
                singleSelectionActionGroup.setEnabled(!selections.isEmpty());
            }

            Package pkg = getSelection();

            int selectedTab = tabbedPane.getSelectedIndex();
            tabbedPane.removeAll();

            if (pkg != null) {
                packageSelectionSetup(pkg, selectedTab);
            }

            revalidate();
            repaint();
        });
    }

    private void packageSelectionSetup(Package pkg, int selectedTab) {
        tabbedPane.add("Nozzle Tips", new PackageNozzleTipsPanel(pkg));
        tabbedPane.add("Vision", new JScrollPane(new PackageVisionPanel(pkg.getFootprint())));
        tabbedPane.add("Settings", new JScrollPane(new PackageSettingsPanel(pkg)));
        if (selectedTab != -1) {
            tabbedPane.setSelectedIndex(selectedTab);
        }
    }

    private Package getSelection() {
        List<Package> selections = getSelections();
        if (selections.size() != 1) {
            return null;
        }
        return selections.get(0);
    }

    private List<Package> getSelections() {
        List<Package> selections = new ArrayList<>();
        for (int selectedRow : table.getSelectedRows()) {
            selectedRow = table.convertRowIndexToModel(selectedRow);
            selections.add(tableModel.getPackage(selectedRow));
        }
        return selections;
    }

    private void search() {
        RowFilter<PackagesTableModel, Object> rf = null;
        // If current expression doesn't parse, don't update.
        try {
            rf = RowFilter.regexFilter("(?i)" + searchTextField.getText().trim());
        } catch (PatternSyntaxException e) {
            Logger.warn(e, "Search failed");
            return;
        }
        tableSorter.setRowFilter(rf);
    }

    public final Action newPackageAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.add);
            putValue(NAME, "New Package...");
            putValue(SHORT_DESCRIPTION, "Create a new package, specifying it's ID.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            String id;
            while ((id = JOptionPane.showInputDialog(frame,
                    "Please enter an ID for the new package.")) != null) {
                if (configuration.getPackage(id) == null) {
                    Package pkg = new Package(id);

                    configuration.addPackage(pkg);
                    tableModel.fireTableDataChanged();
                    Helpers.selectLastTableRow(table);
                    break;
                }

                MessageBoxes.errorBox(frame, "Error", "Package ID " + id + " already exists.");
            }
        }
    };

    public final Action deletePackageAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.delete);
            putValue(NAME, "Delete Package");
            putValue(SHORT_DESCRIPTION, "Delete the currently selected package.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            // Check to make sure there are no parts using this package.
            List<Package> selections = getSelections();
            for (Package pkg : selections) {
                for (Part part : Configuration.get().getParts()) {
                    if (part.getPackage() == pkg) {
                        MessageBoxes.errorBox(getTopLevelAncestor(), "Error",
                                pkg.getId() + " cannot be deleted. It is used by "
                                        + part.getId());
                        return;
                    }
                }
            }

            List<String> ids = selections.stream().map(Package::getId).collect(Collectors.toList());
            String formattedIds;
            if (ids.size() <= 3) {
                formattedIds = String.join(", ", ids);
            } else {
                formattedIds = String.join(", ", ids.subList(0, 3)) + ", and " + (ids.size() - 3) + " others";
            }

            int ret = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                    "Are you sure you want to delete " + formattedIds + "?",
                    "Delete " + selections.size() + " packages?", JOptionPane.YES_NO_OPTION);
            if (ret == JOptionPane.YES_OPTION) {
                for (Package pkg : selections) {
                    Configuration.get().removePackage(pkg);
                }
            }
        }
    };

    public final Action copyPackageToClipboardAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.copy);
            putValue(NAME, "Copy Package to Clipboard");
            putValue(SHORT_DESCRIPTION,
                    "Copy the currently selected package to the clipboard in text format.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            Package pkg = getSelection();
            if (pkg == null) {
                return;
            }
            try {
                Serializer s = Configuration.createSerializer();
                StringWriter w = new StringWriter();
                s.write(pkg, w);
                StringSelection stringSelection = new StringSelection(w.toString());
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(stringSelection, null);
            } catch (Exception e) {
                MessageBoxes.errorBox(getTopLevelAncestor(), "Copy Failed", e);
            }
        }
    };

    public final Action pastePackageFromClipboardAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.paste);
            putValue(NAME, "Create Package from Clipboard");
            putValue(SHORT_DESCRIPTION, "Create a new package from a definition on the clipboard.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            try {
                Serializer ser = Configuration.createSerializer();
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                String s = (String) clipboard.getData(DataFlavor.stringFlavor);
                Package pkg = ser.read(Package.class, s);
                for (int i = 0; ; i++) {
                    if (Configuration.get().getPackage(pkg.getId() + "-" + i) == null) {
                        pkg.setId(pkg.getId() + "-" + i);
                        Configuration.get().addPackage(pkg);
                        break;
                    }
                }
                tableModel.fireTableDataChanged();
                Helpers.selectLastTableRow(table);
            } catch (Exception e) {
                MessageBoxes.errorBox(getTopLevelAncestor(), "Paste Failed", e);
            }
        }
    };
}
