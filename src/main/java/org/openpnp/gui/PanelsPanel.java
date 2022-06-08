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
import java.awt.FileDialog;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.openpnp.Translations;
import org.openpnp.events.FiducialLocatableLocationSelectedEvent;
import org.openpnp.events.PlacementSelectedEvent;
import org.openpnp.gui.components.AutoSelectTextTable;
import org.openpnp.gui.support.ActionGroup;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.tablemodel.FiducialLocatableTableModel;
import org.openpnp.model.Panel;
import org.openpnp.model.PanelLocation;
import org.openpnp.model.Board;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.pmw.tinylog.Logger;

import com.google.common.eventbus.Subscribe;

@SuppressWarnings("serial")
public class PanelsPanel extends JPanel {
    final private Configuration configuration;
    final private MainFrame frame;

    private static final String PREF_DIVIDER_POSITION = "PanelsPanel.dividerPosition"; //$NON-NLS-1$
    private static final int PREF_DIVIDER_POSITION_DEF = -1;

    private FiducialLocatableTableModel panelsTableModel;
    private JTable panelsTable;
    private JSplitPane splitPane;

    private ActionGroup singleSelectionActionGroup;
    private ActionGroup multiSelectionActionGroup;

    private Preferences prefs = Preferences.userNodeForPackage(PanelsPanel.class);

    private final PanelDefinitionPanel panelDefinitionPanel;

    public PanelsPanel(Configuration configuration, MainFrame frame) {
        this.configuration = configuration;
        this.frame = frame;
        
        singleSelectionActionGroup = new ActionGroup(removePanelAction, copyPanelAction);
        singleSelectionActionGroup.setEnabled(false);
        
        multiSelectionActionGroup = new ActionGroup(removePanelAction);
        multiSelectionActionGroup.setEnabled(false);
        
        panelsTableModel = new FiducialLocatableTableModel(configuration, 
                () -> configuration.getPanels(), Panel.class);
        configuration.addPropertyChangeListener("panels", new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                Logger.trace("PropertyChangeEvent = " + evt);
                panelsTableModel.fireTableDataChanged();
            }});
        
        panelsTable = new AutoSelectTextTable(panelsTableModel) {
            @Override
            public String getToolTipText(MouseEvent e) {

                java.awt.Point p = e.getPoint();
                int row = rowAtPoint(p);
                int col = columnAtPoint(p);

                if (row >= 0) {
                    if (col == 0) {
                        row = panelsTable.convertRowIndexToModel(row);
                        return configuration.getPanels().get(row).getFile().toString();
                    }
                }

                return super.getToolTipText();
            }
        };

        panelsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        panelsTable.getSelectionModel()
                .addListSelectionListener(new ListSelectionListener() {
                    @Override
                    public void valueChanged(ListSelectionEvent e) {
                        if (e.getValueIsAdjusting()) {
                            return;
                        }
                        
                        List<Panel> selections = getSelections();
                        if (selections.size() == 0) {
                            singleSelectionActionGroup.setEnabled(false);
                            multiSelectionActionGroup.setEnabled(false);
                            panelDefinitionPanel.setPanel(null);
//                            Configuration.get().getBus()
//                                .post(new FiducialLocatableLocationSelectedEvent(null, BoardsPanel.this));
                        }
                        else if (selections.size() == 1) {
                            multiSelectionActionGroup.setEnabled(false);
                            singleSelectionActionGroup.setEnabled(true);
                            panelDefinitionPanel.setPanel((Panel) selections.get(0));
//                            Configuration.get().getBus()
//                                .post(new FiducialLocatableLocationSelectedEvent((BoardLocation) selections.get(0), BoardsPanel.this));
                        }
                        else {
                            singleSelectionActionGroup.setEnabled(false);
                            multiSelectionActionGroup.setEnabled(true);
                            panelDefinitionPanel.setPanel(null);
//                            Configuration.get().getBus()
//                                .post(new FiducialLocatableLocationSelectedEvent(null, BoardsPanel.this));
                        }
                    }
                });

        setLayout(new BorderLayout(0, 0));

        splitPane = new JSplitPane();
        splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        splitPane.setBorder(null);
        splitPane.setContinuousLayout(true);
        splitPane.setDividerLocation(prefs.getInt(PREF_DIVIDER_POSITION, PREF_DIVIDER_POSITION_DEF));
        splitPane.addPropertyChangeListener("dividerLocation", new PropertyChangeListener() { //$NON-NLS-1$
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                prefs.putInt(PREF_DIVIDER_POSITION, splitPane.getDividerLocation());
            }
        });

        JPanel pnlPanels = new JPanel();
        pnlPanels.setBorder(new TitledBorder(null,
                Translations.getString("PanelsPanel.Tab.Panels"),
                TitledBorder.LEADING, TitledBorder.TOP, null)); //$NON-NLS-1$
        pnlPanels.setLayout(new BorderLayout(0, 0));

        JToolBar toolBarPanels = new JToolBar();
        toolBarPanels.setFloatable(false);
        pnlPanels.add(toolBarPanels, BorderLayout.NORTH);

        toolBarPanels.addSeparator();
        JButton btnAddPanel = new JButton(addPanelAction);
        btnAddPanel.setHideActionText(true);
        btnAddPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                JPopupMenu menu = new JPopupMenu();
                menu.add(new JMenuItem(addNewPanelAction));
                menu.add(new JMenuItem(addExistingPanelAction));
                menu.show(btnAddPanel, (int) btnAddPanel.getWidth(), (int) btnAddPanel.getHeight());
            }
        });
        toolBarPanels.add(btnAddPanel);
        
        JButton btnRemovePanel = new JButton(removePanelAction);
        btnRemovePanel.setHideActionText(true);
        toolBarPanels.add(btnRemovePanel);
        
        JButton btnCopyPanel = new JButton(copyPanelAction);
        btnCopyPanel.setHideActionText(true);
        toolBarPanels.add(btnCopyPanel);

        pnlPanels.add(new JScrollPane(panelsTable));
        splitPane.setLeftComponent(pnlPanels);
        
        panelDefinitionPanel = new PanelDefinitionPanel(this);
        splitPane.setRightComponent(panelDefinitionPanel);
        
        add(splitPane);
        

        Configuration.get().getBus().register(this);
    }
    
    public JTable getFiducialLocatableLocationsTable() {
        return panelsTable;
    }

    @Subscribe
    public void fiducialLocatableLocationSelected(FiducialLocatableLocationSelectedEvent event) {
        if (event.source == this || event.fiducialLocatableLocation == null) {
            return;
        }
        if (event.fiducialLocatableLocation.getFiducialLocatable() instanceof Panel) {
            SwingUtilities.invokeLater(() -> {
                selectPanel((Panel) event.fiducialLocatableLocation.getFiducialLocatable());
            });
        }
        else if (event.fiducialLocatableLocation.getParent() instanceof PanelLocation) {
            SwingUtilities.invokeLater(() -> {
                selectPanel((Panel) event.fiducialLocatableLocation.getParent().getFiducialLocatable());
                panelDefinitionPanel.selectChild(event.fiducialLocatableLocation);
            });
        }
    }

    @Subscribe
    public void placementSelected(PlacementSelectedEvent event) {
        if (event.source == this || event.source == panelDefinitionPanel || event.fiducialLocatableLocation == null || !(event.fiducialLocatableLocation.getFiducialLocatable() instanceof Panel)) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            selectPanel((Panel) event.fiducialLocatableLocation.getFiducialLocatable());
            panelDefinitionPanel.selectFiducial(event.placement);
        });
    }

    private void selectPanel(Panel panel) {
        if (panel == null) {
            panelsTable.getSelectionModel().clearSelection();
            return;
        }
        for (int i = 0; i < panelsTableModel.getRowCount(); i++) {
            if (configuration.getPanels().get(i) == panel) {
                int index = panelsTable.convertRowIndexToView(i);
                panelsTable.getSelectionModel().setSelectionInterval(index, index);
                panelsTable.scrollRectToVisible(
                        new Rectangle(panelsTable.getCellRect(index, 0, true)));
                break;
            }
        }
    }

    public void refresh() {
        panelsTableModel.fireTableDataChanged();
    }

    public void refreshSelectedRow() {
        int index = panelsTable.convertRowIndexToModel(panelsTable.getSelectedRow());
        panelsTableModel.fireTableRowsUpdated(index, index);
    }

    public Panel getSelection() {
        List<Panel> selections = getSelections();
        if (selections.isEmpty()) {
            return null;
        }
        return selections.get(0);
    }

    public List<Panel> getSelections() {
        ArrayList<Panel> selections = new ArrayList<>();
        int[] selectedRows = panelsTable.getSelectedRows();
        for (int selectedRow : selectedRows) {
            selectedRow = panelsTable.convertRowIndexToModel(selectedRow);
            selections.add(configuration.getPanels().get(selectedRow));
        }
        return selections;
    }

    public final Action addPanelAction = new AbstractAction() {
        {
            putValue(NAME, Translations.getString("BoardPanel.Action.AddBoard")); //$NON-NLS-1$
            putValue(SMALL_ICON, Icons.add);
            putValue(SHORT_DESCRIPTION, Translations.getString("BoardPanel.Action.AddBoard.Description")); //$NON-NLS-1$
            putValue(MNEMONIC_KEY, KeyEvent.VK_A);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {}
    };

    public final Action addNewPanelAction = new AbstractAction() {
        {
            putValue(NAME, Translations.getString("BoardPanel.Action.AddBoard.NewBoard")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("BoardPanel.Action.AddBoard.NewBoard.Description")); //$NON-NLS-1$
            putValue(MNEMONIC_KEY, KeyEvent.VK_N);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            FileDialog fileDialog = new FileDialog(frame, Translations.getString("BoardPanel.Action.AddBoard.NewBoard.SaveDialog"), FileDialog.SAVE); //$NON-NLS-1$
            fileDialog.setFilenameFilter(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".board.xml"); //$NON-NLS-1$
                }
            });
            fileDialog.setFile("*.board.xml");
            fileDialog.setVisible(true);
            try {
                String filename = fileDialog.getFile();
                if (filename == null) {
                    return;
                }
                if (!filename.toLowerCase().endsWith(".board.xml")) { //$NON-NLS-1$
                    filename = filename + ".board.xml"; //$NON-NLS-1$
                }
                File file = new File(new File(fileDialog.getDirectory()), filename);

                addBoard(file);

                Helpers.selectLastTableRow(panelsTable);
            }
            catch (Exception e) {
                e.printStackTrace();
                MessageBoxes.errorBox(frame, Translations.getString("BoardPanel.Action.AddBoard.NewBoard.ErrorMessage"), e.getMessage()); //$NON-NLS-1$
            }
//            updatePanelizationIconState();
        }
    };

    public final Action addExistingPanelAction = new AbstractAction() {
        {
            putValue(NAME, Translations.getString("BoardPanel.Action.AddBoard.ExistingBoard")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("BoardPanel.Action.AddBoard.ExistingBoard.Description")); //$NON-NLS-1$
            putValue(MNEMONIC_KEY, KeyEvent.VK_E);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            FileDialog fileDialog = new FileDialog(frame);
            fileDialog.setFilenameFilter(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".board.xml"); //$NON-NLS-1$
                }
            });
            fileDialog.setFile("*.board.xml");
            fileDialog.setVisible(true);
            try {
                if (fileDialog.getFile() == null) {
                    return;
                }
                File file = new File(new File(fileDialog.getDirectory()), fileDialog.getFile());

                addBoard(file);

                Helpers.selectLastTableRow(panelsTable);
            }
            catch (Exception e) {
                e.printStackTrace();
                MessageBoxes.errorBox(frame, Translations.getString("BoardPanel.Action.AddBoard.ExistingBoard.ErrorMessage"), e.getMessage()); //$NON-NLS-1$
            }
        }
    };

    protected void addBoard(File file) throws Exception {
        Board board = configuration.getBoard(file);
        BoardLocation boardLocation = new BoardLocation(board);
        // TODO: Move to a list property listener.
        panelsTableModel.fireTableDataChanged();
    }
    
    public final Action removePanelAction = new AbstractAction("Remove Board") { //$NON-NLS-1$
        {
            putValue(SMALL_ICON, Icons.delete);
            putValue(NAME, Translations.getString("BoardPanel.Action.RemoveBoard")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("BoardPanel.Action.RemoveBoard.Description")); //$NON-NLS-1$
            putValue(MNEMONIC_KEY, KeyEvent.VK_R);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            for (Panel selection : getSelections()) {
                configuration.removePanel(selection);

            }
            panelsTableModel.fireTableDataChanged();
        }
    };
    
    public final Action copyPanelAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.copy);
            putValue(NAME, Translations.getString("PanelsPanel.Action.CopyPanel")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("PanelsPanel.Action.CopyPanel.Description")); //$NON-NLS-1$
            putValue(MNEMONIC_KEY, KeyEvent.VK_COPY);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            Panel panelToCopy = getSelection();
            FileDialog fileDialog = new FileDialog(frame, Translations.getString("PanelsPanel.Action.CopyPanel.SaveDialog"), FileDialog.SAVE); //$NON-NLS-1$
            fileDialog.setFilenameFilter(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".panel.xml"); //$NON-NLS-1$
                }
            });
            fileDialog.setFile("*.panel.xml");
            fileDialog.setVisible(true);
            try {
                String filename = fileDialog.getFile();
                if (filename == null) {
                    return;
                }
                if (!filename.toLowerCase().endsWith(".panel.xml")) { //$NON-NLS-1$
                    filename = filename + ".panel.xml"; //$NON-NLS-1$
                }
                File file = new File(new File(fileDialog.getDirectory()), filename);

                Panel newPanel = new Panel(panelToCopy);
                newPanel.setFile(file);
                newPanel.setName(file.getName());
                newPanel.setDirty(true);
                configuration.addPanel(newPanel);
                panelsTableModel.fireTableDataChanged();
                Helpers.selectLastTableRow(panelsTable);
            }
            catch (Exception e) {
                e.printStackTrace();
                MessageBoxes.errorBox(frame, Translations.getString("BoardPanel.Action.CopyBoard.ErrorMessage"), e.getMessage()); //$NON-NLS-1$
            }
        }
    };

}
