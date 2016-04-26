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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.prefs.Preferences;
import java.util.regex.PatternSyntaxException;

import javax.swing.AbstractAction;
import javax.swing.Action;
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

import org.openpnp.gui.components.AutoSelectTextTable;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.support.ActionGroup;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.tablemodel.PackagesTableModel;
import org.openpnp.model.Configuration;
import org.openpnp.model.Package;
import org.openpnp.model.Part;
import org.openpnp.spi.Camera;
import org.simpleframework.xml.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class PackagesPanel extends JPanel {
    private final static Logger logger = LoggerFactory.getLogger(PackagesPanel.class);

    private static final String PREF_DIVIDER_POSITION = "PackagesPanel.dividerPosition";
    private static final int PREF_DIVIDER_POSITION_DEF = -1;
    private Preferences prefs = Preferences.userNodeForPackage(PackagesPanel.class);

    final private Configuration configuration;
    final private Frame frame;

    private PackagesTableModel tableModel;
    private TableRowSorter<PackagesTableModel> tableSorter;
    private JTextField searchTextField;
    private JTable table;
    private ActionGroup rowSelectedActionGroup;

    public PackagesPanel(Configuration configuration, Frame frame) {
        this.configuration = configuration;
        this.frame = frame;

        setLayout(new BorderLayout(0, 0));
        tableModel = new PackagesTableModel(configuration);
        tableSorter = new TableRowSorter<>(tableModel);

        JPanel toolbarAndSearch = new JPanel();
        add(toolbarAndSearch, BorderLayout.NORTH);
        toolbarAndSearch.setLayout(new BorderLayout(0, 0));

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolbarAndSearch.add(toolBar);

        JPanel panel_1 = new JPanel();
        toolbarAndSearch.add(panel_1, BorderLayout.EAST);

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

        JSplitPane splitPane = new JSplitPane();
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
        JPanel footprintPanel = new JPanel();
        footprintPanel.setLayout(new BorderLayout());
        tabbedPane.add("Footprint", new JScrollPane(footprintPanel));


        table = new AutoSelectTextTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }

                Package pkg = getSelectedPackage();

                rowSelectedActionGroup.setEnabled(pkg != null);

                footprintPanel.removeAll();

                if (pkg != null) {
                    footprintPanel.add(new FootprintPanel(pkg.getFootprint()), BorderLayout.CENTER);
                }

                revalidate();
                repaint();
            }
        });

        table.setRowSorter(tableSorter);

        splitPane.setLeftComponent(new JScrollPane(table));
        splitPane.setRightComponent(tabbedPane);

        rowSelectedActionGroup = new ActionGroup(deletePackageAction, copyPackageToClipboardAction);
        rowSelectedActionGroup.setEnabled(false);

        toolBar.add(newPackageAction);
        toolBar.add(deletePackageAction);
        toolBar.addSeparator();
        toolBar.add(copyPackageToClipboardAction);
        toolBar.add(pastePackageToClipboardAction);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentHidden(ComponentEvent e) {
                try {
                    Camera camera =
                            Configuration.get().getMachine().getDefaultHead().getDefaultCamera();
                    CameraView cameraView = MainFrame.cameraPanel.getCameraView(camera);
                    if (cameraView == null) {
                        return;
                    }
                    cameraView.removeReticle(FootprintPanel.class.getName());
                }
                catch (Exception e1) {
                }
            }
        });
    }

    private Package getSelectedPackage() {
        int index = table.getSelectedRow();
        if (index == -1) {
            return null;
        }
        index = table.convertRowIndexToModel(index);
        return tableModel.getPackage(index);
    }

    private void search() {
        RowFilter<PackagesTableModel, Object> rf = null;
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
                if (configuration.getPackage(id) != null) {
                    MessageBoxes.errorBox(frame, "Error", "Package ID " + id + " already exists.");
                    continue;
                }
                Package this_package = new Package(id);

                configuration.addPackage(this_package);
                tableModel.fireTableDataChanged();
                Helpers.selectLastTableRow(table);
                break;
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
            for (Part part : Configuration.get().getParts()) {
                if (part.getPackage() == getSelectedPackage()) {
                    MessageBoxes.errorBox(getTopLevelAncestor(), "Error",
                            getSelectedPackage().getId() + " cannot be deleted. It is used by "
                                    + part.getId());
                    return;
                }
            }
            int ret = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                    "Are you sure you want to delete " + getSelectedPackage().getId() + "?",
                    "Delete " + getSelectedPackage().getId() + "?", JOptionPane.YES_NO_OPTION);
            if (ret == JOptionPane.YES_OPTION) {
                Configuration.get().removePackage(getSelectedPackage());
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
            Package pkg = getSelectedPackage();
            if (pkg == null) {
                return;
            }
            try {
                Serializer s = Configuration.get().createSerializer();
                StringWriter w = new StringWriter();
                s.write(pkg, w);
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
            putValue(NAME, "Create Package from Clipboard");
            putValue(SHORT_DESCRIPTION, "Create a new package from a definition on the clipboard.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            try {
                Serializer ser = Configuration.get().createSerializer();
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                String s = (String) clipboard.getData(DataFlavor.stringFlavor);
                StringReader r = new StringReader(s);
                Package pkg = ser.read(Package.class, s);
                for (int i = 0;; i++) {
                    if (Configuration.get().getPackage(pkg.getId() + "-" + i) == null) {
                        pkg.setId(pkg.getId() + "-" + i);
                        Configuration.get().addPackage(pkg);
                        break;
                    }
                }
                tableModel.fireTableDataChanged();
                Helpers.selectLastTableRow(table);
            }
            catch (Exception e) {
                MessageBoxes.errorBox(getTopLevelAncestor(), "Paste Failed", e);
            }
        }
    };
}
