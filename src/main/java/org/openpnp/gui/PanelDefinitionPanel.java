/*
 * Copyright (C) 2022 Jason von Nieda <jason@vonnieda.org>, Tony Luken <tonyluken62+openpnp@gmail.com>
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
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;

import org.openpnp.Translations;
import org.openpnp.events.DefinitionStructureChangedEvent;
import org.openpnp.events.PlacementsHolderLocationSelectedEvent;
import org.openpnp.events.PlacementSelectedEvent;
import org.openpnp.gui.components.AutoSelectTextTable;
import org.openpnp.gui.components.ExistingBoardOrPanelDialog;
import org.openpnp.gui.panelization.ChildFiducialSelectorDialog;
import org.openpnp.gui.panelization.PanelArrayBuilderDialog;
import org.openpnp.gui.support.ActionGroup;
import org.openpnp.gui.support.CustomBooleanRenderer;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.IdentifiableListCellRenderer;
import org.openpnp.gui.support.IdentifiableTableCellRenderer;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.PartsComboBoxModel;
import org.openpnp.gui.tablemodel.PlacementsHolderLocationsTableModel;
import org.openpnp.gui.tablemodel.PanelFiducialsTableModel;
import org.openpnp.model.AbstractLocatable;
import org.openpnp.model.Board;
import org.openpnp.model.Board.Side;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Configuration.TablesLinked;
import org.openpnp.model.PlacementsHolderLocation;
import org.openpnp.model.Location;
import org.openpnp.model.Panel;
import org.openpnp.model.PanelLocation;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.pmw.tinylog.Logger;

import com.google.common.eventbus.Subscribe;

import javax.swing.JSplitPane;
import javax.swing.border.EtchedBorder;
import java.awt.FileDialog;

@SuppressWarnings("serial")
public class PanelDefinitionPanel extends JPanel implements PropertyChangeListener {
    private static final String PREF_DIVIDER_POSITION = "PanelDefinitionPanel.dividerPosition"; //$NON-NLS-1$
    private static final int PREF_DIVIDER_POSITION_DEF = -1;
    
    private Preferences prefs = Preferences.userNodeForPackage(PanelDefinitionPanel.class);
    
    private AutoSelectTextTable fiducialTable;
    private PanelFiducialsTableModel fiducialTableModel;
    private TableRowSorter<PanelFiducialsTableModel> fiducialTableSorter;
    
    private ActionGroup fiducialSingleSelectionActionGroup;
    private ActionGroup fiducialMultiSelectionActionGroup;
    
    private AutoSelectTextTable childrenTable;
    private PlacementsHolderLocationsTableModel childrenTableModel;
    private TableRowSorter<PlacementsHolderLocationsTableModel> childrenTableSorter;
    
    private ActionGroup childrenSingleSelectionActionGroup;
    private ActionGroup childrenMultiSelectionActionGroup;
    
    private PanelLocation rootPanelLocation = new PanelLocation();
    private Panel panel;
    private PanelsPanel panelsPanel;
    
    private JSplitPane splitPane;
    private MainFrame frame;
    private Configuration configuration;
    private boolean dirty;

    public PanelDefinitionPanel(PanelsPanel panelsPanel) {
    	this.panelsPanel = panelsPanel;
    	frame = MainFrame.get();
    	configuration = Configuration.get();
        createUi();
    }
    
    private void createUi() {
        setBorder(new TitledBorder(null, "Panel Definition", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        
        Configuration configuration = Configuration.get();
        
        fiducialSingleSelectionActionGroup = new ActionGroup(removeFiducialAction, setSideAction, 
                setEnabledAction);
        fiducialSingleSelectionActionGroup.setEnabled(false);

        fiducialMultiSelectionActionGroup = new ActionGroup(removeFiducialAction, setSideAction,
                setEnabledAction);
        fiducialMultiSelectionActionGroup.setEnabled(false);

        childrenSingleSelectionActionGroup = new ActionGroup(removeChildAction, setSideAction,  
                setEnabledAction, setCheckFidsAction, createArrayAction);
        childrenSingleSelectionActionGroup.setEnabled(false);

        childrenMultiSelectionActionGroup = new ActionGroup(removeChildAction, setSideAction,
                setEnabledAction, setCheckFidsAction);
        childrenMultiSelectionActionGroup.setEnabled(false);

        @SuppressWarnings({"unchecked", "rawtypes"})
        JComboBox<PartsComboBoxModel> partsComboBox = new JComboBox(new PartsComboBoxModel());
        partsComboBox.setMaximumRowCount(20);
        partsComboBox.setRenderer(new IdentifiableListCellRenderer<Part>());
        @SuppressWarnings({"unchecked", "rawtypes"})
        JComboBox<Side> sidesComboBox = new JComboBox(Side.values());
        
        setLayout(new BorderLayout(0, 0));
        
        splitPane = new JSplitPane();
        splitPane.setResizeWeight(0.5);
        splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        splitPane.setBorder(null);
        splitPane.setContinuousLayout(true);
        add(splitPane, BorderLayout.CENTER);
        
        JPanel pnlChildren = new JPanel();
        pnlChildren.setBorder(new TitledBorder(null, "Panel Children", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        splitPane.setLeftComponent(pnlChildren);
        pnlChildren.setLayout(new BorderLayout(0, 0));
        
        JPanel pnlChildrenToolbar = new JPanel();
        pnlChildren.add(pnlChildrenToolbar, BorderLayout.NORTH);
        pnlChildrenToolbar.setLayout(new BorderLayout(0, 0));
        
        JToolBar toolBarChildren = new JToolBar();
        toolBarChildren.setFloatable(false);
        pnlChildrenToolbar.add(toolBarChildren);
        
        JButton btnAddChild = new JButton(addChildAction);
        btnAddChild.setToolTipText("Add a new or existing panel or board to this panel.");
        btnAddChild.setText("Add Panel or Board");
        btnAddChild.setHideActionText(true);
        btnAddChild.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                JPopupMenu menu = new JPopupMenu();
                menu.add(new JMenuItem(addNewBoardAction));
                menu.add(new JMenuItem(addExistingBoardAction));
                menu.addSeparator();
                menu.add(new JMenuItem(addNewPanelAction));
                menu.add(new JMenuItem(addExistingPanelAction));
                menu.show(btnAddChild, (int) btnAddChild.getWidth(), (int) btnAddChild.getHeight());
            }
        });
        toolBarChildren.add(btnAddChild);
        
        JButton btnRemoveChild = new JButton(removeChildAction);
        btnRemoveChild.setToolTipText("Remove the currently selected panel(s) and/or board(s) from this panel.");
        btnRemoveChild.setText("Remove Panel(s) and/or Board(s)");
        btnRemoveChild.setHideActionText(true);
        toolBarChildren.add(btnRemoveChild);
        toolBarChildren.addSeparator();
        
        JButton btnCreateArray = new JButton(createArrayAction);
        btnCreateArray.setToolTipText("Create an array of children from the selected child.");
        btnCreateArray.setText("Create Array");
        btnCreateArray.setHideActionText(true);
        toolBarChildren.add(btnCreateArray);
        
        childrenTableModel = new PlacementsHolderLocationsTableModel(configuration) {
            
            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return super.isCellEditable(rowIndex, columnIndex) && (columnIndex > 3);
            }
        };
        childrenTableModel.setRootPanelLocation(rootPanelLocation);
        childrenTableSorter = new TableRowSorter<>(childrenTableModel);
        
        childrenTable = new AutoSelectTextTable(childrenTableModel);
        TableColumnModel tcm = childrenTable.getColumnModel();
//        tcm.removeColumn(tcm.getColumn(10)); //remove Check Fids column
//        tcm.removeColumn(tcm.getColumn(9)); //remove Enabled column
        tcm.removeColumn(tcm.getColumn(7)); //remove Z column

        childrenTable.setRowSorter(childrenTableSorter);
        childrenTable.getTableHeader().setDefaultRenderer(new MultisortTableHeaderCellRenderer());
        childrenTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        childrenTable.setDefaultEditor(Side.class, new DefaultCellEditor(sidesComboBox));
        childrenTable.setDefaultRenderer(Boolean.class, new CustomBooleanRenderer());

        
        childrenTable.getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                Logger.trace("TableModelEvent = " +
                    "col:" + e.getColumn() +
                    " firstRow:" + e.getFirstRow() +
                    " lastRow:" + e.getLastRow() +
                    " source:" + e.getSource() +
                    " type:" + e.getType() );
                SwingUtilities.invokeLater(() -> {
                    fiducialTableModel.fireTableDataChanged();
                });
            }
        });

        childrenTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }
                
                boolean updateLinkedTables = MainFrame.get().getTabs().getSelectedComponent() == MainFrame.get().getPanelsTab() 
                        && Configuration.get().getTablesLinked() == TablesLinked.Linked;

                List<PlacementsHolderLocation<?>> selections = getChildrenSelections();
                if (selections.size() > 1) {
                    // multi select
                    childrenSingleSelectionActionGroup.setEnabled(false);
                    childrenMultiSelectionActionGroup.setEnabled(true);
                    if (updateLinkedTables) {
                        Configuration.get().getBus()
                            .post(new PlacementsHolderLocationSelectedEvent(null, PanelDefinitionPanel.this));
                        Configuration.get().getBus()
                            .post(new PlacementSelectedEvent(null, null, PanelDefinitionPanel.this));
                    }
                }
                else if (selections.size() == 1) {
                    // single select
                    childrenMultiSelectionActionGroup.setEnabled(false);
                    childrenSingleSelectionActionGroup.setEnabled(selections != null);
                    if (updateLinkedTables) {
                        Configuration.get().getBus()
                            .post(new PlacementsHolderLocationSelectedEvent(selections.get(0), PanelDefinitionPanel.this));
                        Configuration.get().getBus()
                            .post(new PlacementSelectedEvent(null, selections.get(0), PanelDefinitionPanel.this));
                    }
                }
                else {
                    // no select
                    childrenSingleSelectionActionGroup.setEnabled(false);
                    childrenMultiSelectionActionGroup.setEnabled(false);
                    if (updateLinkedTables) {
                        Configuration.get().getBus()
                            .post(new PlacementsHolderLocationSelectedEvent(null, PanelDefinitionPanel.this));
                        Configuration.get().getBus()
                            .post(new PlacementSelectedEvent(null, null, PanelDefinitionPanel.this));
                    }
                }
            }
        });
        childrenTable.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == ' ') {
                    PlacementsHolderLocation<?> child = getChildrenSelection();
                    child.setLocallyEnabled(!child.isLocallyEnabled());
                    refreshSelectedRow();
                }
                else {
                    super.keyTyped(e);
                }
            }
        });

        JPopupMenu childrenPopupMenu = new JPopupMenu();

        JMenu setChildrenSideMenu = new JMenu(setSideAction);
        for (Board.Side side : Board.Side.values()) {
            setChildrenSideMenu.add(new SetChildrenSideAction(side));
        }
        childrenPopupMenu.add(setChildrenSideMenu);

        JMenu setChildrenEnabledMenu = new JMenu(setEnabledAction);
        setChildrenEnabledMenu.add(new SetChildrenEnabledAction(true));
        setChildrenEnabledMenu.add(new SetChildrenEnabledAction(false));
        childrenPopupMenu.add(setChildrenEnabledMenu);
        
        JMenu setChildrenCheckFidsMenu = new JMenu(setCheckFidsAction);
        setChildrenCheckFidsMenu.add(new SetCheckFidsAction(true));
        setChildrenCheckFidsMenu.add(new SetCheckFidsAction(false));
        childrenPopupMenu.add(setChildrenCheckFidsMenu);
        
        childrenTable.setComponentPopupMenu(childrenPopupMenu);

        JScrollPane scrollPaneChildren = new JScrollPane(childrenTable);
        pnlChildren.add(scrollPaneChildren);

        
        
        JPanel pnlFiducials = new JPanel();
        pnlFiducials.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, new Color(255, 255, 255), new Color(160, 160, 160)), "Panel Fiducials", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        splitPane.setRightComponent(pnlFiducials);
        pnlFiducials.setLayout(new BorderLayout(0, 0));
        
        JPanel pnlFiducialsToolbar = new JPanel();
        pnlFiducials.add(pnlFiducialsToolbar, BorderLayout.NORTH);
        pnlFiducialsToolbar.setLayout(new BorderLayout(0, 0));
        
        JToolBar toolBarFiducials = new JToolBar();
        toolBarFiducials.setFloatable(false);
        pnlFiducialsToolbar.add(toolBarFiducials, BorderLayout.CENTER);
        
        JButton btnAddFiducial = new JButton(addFiducialAction);
        btnAddFiducial.setToolTipText("Add a fiducial to this panel.");
        btnAddFiducial.setHideActionText(true);
        toolBarFiducials.add(btnAddFiducial);
        
        JButton btnRemoveFiducial = new JButton(removeFiducialAction);
        btnRemoveFiducial.setToolTipText("Remove the selected fiducial(s) from this panel.");
        btnRemoveFiducial.setHideActionText(true);
        toolBarFiducials.add(btnRemoveFiducial);
        
        JButton btnUseChildFiducial = new JButton(useChildFiducialAction);
        btnUseChildFiducial.setToolTipText("Use children's fiducials/placements for this panel's alignment.");
        btnUseChildFiducial.setHideActionText(true);
        toolBarFiducials.add(btnUseChildFiducial);
        
        fiducialTableModel = new PanelFiducialsTableModel(rootPanelLocation.getPanel());
        fiducialTableSorter = new TableRowSorter<>(fiducialTableModel);
        
        fiducialTable = new AutoSelectTextTable(fiducialTableModel);
        
        fiducialTable.setRowSorter(fiducialTableSorter);
        fiducialTable.getTableHeader().setDefaultRenderer(new MultisortTableHeaderCellRenderer());
        fiducialTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        fiducialTable.setDefaultEditor(Side.class, new DefaultCellEditor(sidesComboBox));
        fiducialTable.setDefaultEditor(Part.class, new DefaultCellEditor(partsComboBox));
        fiducialTable.setDefaultRenderer(Part.class, new IdentifiableTableCellRenderer<Part>());
        fiducialTable.setDefaultRenderer(Boolean.class, new CustomBooleanRenderer());
        fiducialTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }
                
                boolean updateLinkedTables = MainFrame.get().getTabs().getSelectedComponent() == MainFrame.get().getPanelsTab() 
                        && Configuration.get().getTablesLinked() == TablesLinked.Linked;
                
                if (getFiducialSelections().size() > 1) {
                    // multi select
                    fiducialSingleSelectionActionGroup.setEnabled(false);
                    fiducialMultiSelectionActionGroup.setEnabled(true);
                    if (updateLinkedTables) {
                        Configuration.get().getBus().post(new PlacementSelectedEvent(null,
                                rootPanelLocation, PanelDefinitionPanel.this));
                    }
                }
                else {
                    // single select, or no select
                    fiducialMultiSelectionActionGroup.setEnabled(false);
                    fiducialSingleSelectionActionGroup.setEnabled(getFiducialSelection() != null);
                    if (updateLinkedTables) {
                        Configuration.get().getBus().post(new PlacementSelectedEvent(getFiducialSelection(),
                                rootPanelLocation, PanelDefinitionPanel.this));
                    }
                }
            }
        });

        JPopupMenu fiducialPopupMenu = new JPopupMenu();

        JMenu setFiducialSideMenu = new JMenu(setSideAction);
        for (Board.Side side : Board.Side.values()) {
            setFiducialSideMenu.add(new SetFiducialSideAction(side));
        }
        fiducialPopupMenu.add(setFiducialSideMenu);

        JMenu setFiducialEnabledMenu = new JMenu(setEnabledAction);
        setFiducialEnabledMenu.add(new SetFiducialEnabledAction(true));
        setFiducialEnabledMenu.add(new SetFiducialEnabledAction(false));
        fiducialPopupMenu.add(setFiducialEnabledMenu);
        
        fiducialTable.setComponentPopupMenu(fiducialPopupMenu);


        JScrollPane scrollPaneFiducials = new JScrollPane(fiducialTable);
        pnlFiducials.add(scrollPaneFiducials, BorderLayout.CENTER);
        
        
        splitPane.setDividerLocation(prefs.getInt(PREF_DIVIDER_POSITION, PREF_DIVIDER_POSITION_DEF));
        splitPane.addPropertyChangeListener("dividerLocation", new PropertyChangeListener() { //$NON-NLS-1$
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                prefs.putInt(PREF_DIVIDER_POSITION, splitPane.getDividerLocation());
            }
        });

        Configuration.get().getBus().register(this);
    }
    
    @Subscribe
    public void panelDefinitionStructureChanged(DefinitionStructureChangedEvent event) {
        Logger.trace("panelDefinitionStructureChanged DefinitionStructureChangedEvent = " + event);
        if (rootPanelLocation != null && 
                event.definition == rootPanelLocation.getPanel()) {
            SwingUtilities.invokeLater(() -> {
                refresh();
            });
        }
    }

    public void setPanel(Panel panel) throws IOException {
        this.panel = panel;
        rootPanelLocation.setGlobalSide(Side.Top);
//        if (rootPanelLocation.getPanel() != null) {
//            rootPanelLocation.getPanel().dispose();
//        }
        rootPanelLocation.setPanel(/*new Panel*/(panel));
//        PanelLocation.setParentsOfAllDescendants(rootPanelLocation);
        rootPanelLocation.dump("");
        childrenTableModel.setPlacementsHolderLocations(rootPanelLocation.getChildren());
        fiducialTableModel.setPanel(rootPanelLocation.getPanel());
    }
    
    public void refresh() {
        childrenTableModel.fireTableDataChanged();
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        boolean oldValue = this.dirty;
        this.dirty = dirty;
        firePropertyChange("dirty", oldValue, dirty);
    }

    public void refreshSelectedRow() {
        int index = childrenTable.convertRowIndexToModel(childrenTable.getSelectedRow());
        childrenTableModel.fireTableRowsUpdated(index, index);
    }

    public Placement getFiducialSelection() {
        List<Placement> selectedFiducials = getFiducialSelections();
        if (selectedFiducials.isEmpty()) {
            return null;
        }
        return selectedFiducials.get(0);
    }

    public List<Placement> getFiducialSelections() {
        List<Placement> fiducials = new ArrayList<>();
        int[] selectedRows = fiducialTable.getSelectedRows();
        for (int selectedRow : selectedRows) {
            selectedRow = fiducialTable.convertRowIndexToModel(selectedRow);
            fiducials.add(panel.getPlacements().get(selectedRow));
        }
        return fiducials;
    }

    public PlacementsHolderLocation<?> getChildrenSelection() {
        List<PlacementsHolderLocation<?>> selectedChildren = getChildrenSelections();
        if (selectedChildren.isEmpty()) {
            return null;
        }
        return selectedChildren.get(0);
    }

    public List<PlacementsHolderLocation<?>> getChildrenSelections() {
        List<PlacementsHolderLocation<?>> selectedChildren = new ArrayList<>();
        int[] selectedRows = childrenTable.getSelectedRows();
        for (int selectedRow : selectedRows) {
            selectedRow = childrenTable.convertRowIndexToModel(selectedRow);
            selectedChildren.add(panel.getChildren().get(selectedRow));
        }
        return selectedChildren;
    }

    public final Action addFiducialAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.add);
            putValue(NAME, "Add Fiducial");
            putValue(SHORT_DESCRIPTION, "Add a fiducial part to this panel.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (Configuration.get().getParts().size() == 0) {
                MessageBoxes.errorBox(getTopLevelAncestor(), "Error",
                        "There are currently no parts defined in the system. Please create at least one part before creating a placement.");
                return;
            }

            String id = JOptionPane.showInputDialog(getTopLevelAncestor(),
                    "Please enter an ID for the new fiducial.");
            if (id == null) {
                return;
            }
            
            // Check if the new placement ID is unique
            for(Placement comparePlacement : rootPanelLocation.getPanel().getPlacements()) {
                if (comparePlacement.getId().equals(id)) {
                    MessageBoxes.errorBox(getTopLevelAncestor(), "Error",
                            "The ID for the new fiducial already exists");
                    return;
                }
            }
            
            Placement placement = new Placement(id);

            placement.setPart(Configuration.get().getParts().get(0));
            placement.setLocation(new Location(Configuration.get().getSystemUnits()));
            placement.setSide(rootPanelLocation.getGlobalSide());
            placement.setType(Placement.Type.Fiducial);

//            ((Panel) rootPanelLocation.getPanel().getDefinedBy()).addPlacement(placement);
            panel.addPlacement(placement);
            fiducialTableModel.fireTableDataChanged();
            Helpers.selectLastTableRow(fiducialTable);
            
            Configuration.get().getBus()
                .post(new DefinitionStructureChangedEvent(panel, "placements", PanelDefinitionPanel.this));
        }
    };

    public final Action removeFiducialAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.delete);
            putValue(NAME, "Remove Fiducial(s)");
            putValue(SHORT_DESCRIPTION, "Remove the currently selected fiducial(s).");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            for (Placement placement : getFiducialSelections()) {
//                ((Panel) rootPanelLocation.getPanel().getDefinedBy()).removePlacement(placement);
                panel.removePlacement(placement);
                placement.dispose();
            }
            fiducialTableModel.fireTableDataChanged();
            
            Configuration.get().getBus()
                .post(new DefinitionStructureChangedEvent(panel, "placements", PanelDefinitionPanel.this));
        }
    };

    public final Action useChildFiducialAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.autoPanelizeFidCheck);
            putValue(NAME, "Use Child Fiducial");
            putValue(SHORT_DESCRIPTION, "Copy the selected child's fiducial(s) to use as this panel's fiducial(s).");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            ChildFiducialSelectorDialog dialog = new ChildFiducialSelectorDialog(rootPanelLocation);
            dialog.setVisible(true);
            fiducialTableModel.fireTableDataChanged();
            
            Configuration.get().getBus()
            .post(new DefinitionStructureChangedEvent(panel, "placements", PanelDefinitionPanel.this));
        }
    };

    public final Action addChildAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.add);
            putValue(NAME, "New Child");
            putValue(SHORT_DESCRIPTION, "Add a new or existing panel or board to this panel.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
        }
    };

    public final Action addNewBoardAction = new AbstractAction() {
        {
            putValue(NAME, Translations.getString("JobPanel.Action.Job.AddBoard.NewBoard")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("JobPanel.Action.Job.AddBoard.NewBoard.Description")); //$NON-NLS-1$
            putValue(MNEMONIC_KEY, KeyEvent.VK_N);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            FileDialog fileDialog = new FileDialog(frame, "Save New Board As...", FileDialog.SAVE); //$NON-NLS-1$
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

                Helpers.selectLastTableRow(childrenTable);
            }
            catch (Exception e) {
                e.printStackTrace();
                MessageBoxes.errorBox(frame, "Unable to create new board", e.getMessage()); //$NON-NLS-1$
            }
        }
    };

    public final Action addExistingBoardAction = new AbstractAction() {
        {
            putValue(NAME, Translations.getString("JobPanel.Action.Job.AddBoard.ExistingBoard")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("JobPanel.Action.Job.AddBoard.ExistingBoard.Description")); //$NON-NLS-1$
            putValue(MNEMONIC_KEY, KeyEvent.VK_E);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            ExistingBoardOrPanelDialog existingBoardDialog = new ExistingBoardOrPanelDialog(
                    Configuration.get(), Board.class, "Add existing board to panel");
            existingBoardDialog.setVisible(true);
            File file = existingBoardDialog.getFile();
            if (file == null) {
                return;
            }
            try {
                addBoard(file);
                Helpers.selectLastTableRow(childrenTable);
            }
            catch (Exception e) {
                e.printStackTrace();
                MessageBoxes.errorBox(frame, "Board load failed", e.getMessage()); //$NON-NLS-1$
            }
        }
    };

    protected void addBoard(File file) throws Exception {
        //Make a deep copy of the board's definition to add to the panel
        Board board = new Board(configuration.getBoard(file));
        
        BoardLocation boardLocation = new BoardLocation(board);
//        boardLocation.addPropertyChangeListener(this);
//        ((Panel) rootPanelLocation.getPanel().getDefinedBy()).addChild(boardLocation);
        panel.addChild(boardLocation);
//        PanelLocation.setParentsOfAllDescendants(rootPanelLocation);
        childrenTableModel.fireTableDataChanged();
        
        Configuration.get().getBus()
        .post(new DefinitionStructureChangedEvent(rootPanelLocation.getPanel(), "children", PanelDefinitionPanel.this));
    }
    
    public final Action addNewPanelAction = new AbstractAction() {
        {
            putValue(NAME, Translations.getString("JobPanel.Action.Job.AddBoard.NewPanel")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("JobPanel.Action.Job.AddBoard.NewPanel.Description")); //$NON-NLS-1$
//            putValue(MNEMONIC_KEY, KeyEvent.VK_N);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            FileDialog fileDialog = new FileDialog(frame, "Save New Panel As...", FileDialog.SAVE); //$NON-NLS-1$
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

                addPanel(file);
//                Panel panel = configuration.getPanel(file);
//                PanelLocation panelLocation = new PanelLocation(panel);
//                verifyNoCircularReferences(rootPanelLocation, panelLocation);
//                
////                panelLocation.addPropertyChangeListener(PanelDefinitionPanel.this);
//                ((Panel) rootPanelLocation.getPanel().getDefinedBy()).addChild(panelLocation);
//                PanelLocation.setParentsOfAllDescendants(rootPanelLocation);
//                childrenTableModel.fireTableDataChanged();

                Helpers.selectLastTableRow(childrenTable);
                
//                Configuration.get().getBus()
//                .post(new DefinitionStructureChangedEvent(rootPanelLocation.getPanel(), "children", PanelDefinitionPanel.this));
            }
            catch (Exception e) {
                e.printStackTrace();
                MessageBoxes.errorBox(frame, "Unable to create new panel", e.getMessage()); //$NON-NLS-1$
            }
        }
    };

    public final Action addExistingPanelAction = new AbstractAction() {
        {
            putValue(NAME, Translations.getString("JobPanel.Action.Job.AddBoard.ExistingPanel")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("JobPanel.Action.Job.AddBoard.ExistingPanel.Description")); //$NON-NLS-1$
//            putValue(MNEMONIC_KEY, KeyEvent.VK_E);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            ExistingBoardOrPanelDialog existingPanelDialog = new ExistingBoardOrPanelDialog(
                    Configuration.get(), Panel.class, "Add existing panel to panel");
            existingPanelDialog.setVisible(true);
            File file = existingPanelDialog.getFile();
            if (file == null) {
                return;
            }
            try {
                addPanel(file);
//                PanelLocation panelLocation = new PanelLocation();
//                panelLocation.setFileName(file.getAbsolutePath());
//                configuration.resolvePanel(null, panelLocation);
//                verifyNoCircularReferences(rootPanelLocation, panelLocation);
//                
////                panelLocation.addPropertyChangeListener(PanelDefinitionPanel.this);
//                ((Panel) rootPanelLocation.getPanel().getDefinedBy()).addChild(panelLocation);
//                PanelLocation.setParentsOfAllDescendants(rootPanelLocation);
//                childrenTableModel.fireTableDataChanged();

                Helpers.selectLastTableRow(childrenTable);
                
//                Configuration.get().getBus()
//                    .post(new DefinitionStructureChangedEvent(rootPanelLocation.getPanel(), "children", PanelDefinitionPanel.this));
            }
            catch (Exception e) {
                e.printStackTrace();
                MessageBoxes.errorBox(frame, "Panel load failed", e.getMessage()); //$NON-NLS-1$
            }
        }
    };

    protected void addPanel(File file) throws Exception {
        //Make a deep copy of the panel's definition to add to the panel
        Panel newPanel = new Panel(configuration.getPanel(file));
        
        PanelLocation panelLocation = new PanelLocation(newPanel);
        verifyNoCircularReferences(rootPanelLocation, panelLocation);
        panel.addChild(panelLocation);
        PanelLocation.setParentsOfAllDescendants(rootPanelLocation);
        childrenTableModel.fireTableDataChanged();
        
        Configuration.get().getBus()
            .post(new DefinitionStructureChangedEvent(panel, "children", PanelDefinitionPanel.this));
    }
    
    private void verifyNoCircularReferences(PanelLocation root, PanelLocation decendant) throws Exception {
        if (decendant.getPanel().getFile().equals(root.getPanel().getFile())) {
            throw new Exception("A panel can't be made a decendant of itself.");
        }
        for (PlacementsHolderLocation<?> child : decendant.getChildren()) {
            if (child instanceof PanelLocation) {
                verifyNoCircularReferences(root, (PanelLocation) child);
            }
        }
    }
    
    public final Action removeChildAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.delete);
            putValue(NAME, "Remove Child(ren)");
            putValue(SHORT_DESCRIPTION, "Remove the currently selected panel(s) and/or board(s) from this panel.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            for (PlacementsHolderLocation<?> child : getChildrenSelections()) {
                rootPanelLocation.getPanel().getDefinedBy().removeChild(child);
                child.dispose();
            }
            childrenTableModel.fireTableDataChanged();
            
            Configuration.get().getBus()
                .post(new DefinitionStructureChangedEvent(rootPanelLocation.getPanel(), "children", PanelDefinitionPanel.this));
        }
    };

    public final Action createArrayAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.autoPanelize);
            putValue(NAME, "Create array of children");
            putValue(SHORT_DESCRIPTION, "Created an array of children from the selected child.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            PlacementsHolderLocation<?> child = getChildrenSelection();
            PanelArrayBuilderDialog dlg = new PanelArrayBuilderDialog(rootPanelLocation, child);
            dlg.setVisible(true);
            
            Configuration.get().getBus()
            .post(new DefinitionStructureChangedEvent(rootPanelLocation.getPanel(), "children", PanelDefinitionPanel.this));

        }
    };

    public final Action setSideAction = new AbstractAction() {
        {
            putValue(NAME, "Set Side");
            putValue(SHORT_DESCRIPTION, "Set side(s) to...");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {}
    };

    class SetFiducialSideAction extends AbstractAction {
        final Board.Side side;

        public SetFiducialSideAction(Board.Side side) {
            this.side = side;
            putValue(NAME, side.toString());
            putValue(SHORT_DESCRIPTION, "Set fiducial side(s) to " + side.toString());
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            for (Placement fiducial : getFiducialSelections()) {
                fiducial.getDefinedBy().setSide(side);
                fiducialTableModel.fireTableDataChanged();
            }
        }
    };
    
    class SetChildrenSideAction extends AbstractAction {
        final Board.Side side;

        public SetChildrenSideAction(Board.Side side) {
            this.side = side;
            putValue(NAME, side.toString());
            putValue(SHORT_DESCRIPTION, "Set children side(s) to " + side.toString());
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            for (PlacementsHolderLocation<?> child : getChildrenSelections()) {
                child.getDefinedBy().setGlobalSide(side);
                childrenTableModel.fireTableDataChanged();
            }
        }
    };
    
    public final Action setEnabledAction = new AbstractAction() {
        {
            putValue(NAME, "Set Enabled");
            putValue(SHORT_DESCRIPTION, "Set enabled to...");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {}
    };

    class SetFiducialEnabledAction extends AbstractAction {
        final Boolean enabled;

        public SetFiducialEnabledAction(Boolean enabled) {
            this.enabled = enabled;
            String name = enabled ? "Enabled" : "Disabled";
            putValue(NAME, name);
            putValue(SHORT_DESCRIPTION, "Set enabled to " + name);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            for (Placement fiducial : getFiducialSelections()) {
                ((Placement) fiducial.getDefinedBy()).setEnabled(enabled);
            }
            fiducialTableModel.fireTableDataChanged();   
        }
    };

    class SetChildrenEnabledAction extends AbstractAction {
        final Boolean enabled;

        public SetChildrenEnabledAction(Boolean enabled) {
            this.enabled = enabled;
            String name = enabled ? "Enabled" : "Disabled";
            putValue(NAME, name);
            putValue(SHORT_DESCRIPTION, "Set enabled to " + name);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            for (PlacementsHolderLocation<?> child : getChildrenSelections()) {
                child.getDefinedBy().setLocallyEnabled(enabled);
            }
            childrenTableModel.fireTableDataChanged();   
        }
    };

    public final Action setCheckFidsAction = new AbstractAction() {
        {
            putValue(NAME, "Set Check Fids");
            putValue(SHORT_DESCRIPTION, "Set check fids to...");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {}
    };

    class SetCheckFidsAction extends AbstractAction {
        final Boolean value;

        public SetCheckFidsAction(Boolean value) {
            this.value = value;
            String name = value ? "Check" : "Don't Check";
            putValue(NAME, name);
            putValue(SHORT_DESCRIPTION, "Set check fids to " + value);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            for (PlacementsHolderLocation<?> child : getChildrenSelections()) {
                child.getDefinedBy().setCheckFiducials(value);
            }
            childrenTableModel.fireTableDataChanged();   
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        Logger.trace("PropertyChangeEvent = " + evt);
        if (evt.getPropertyName() == "children") {
            childrenTableModel.setPlacementsHolderLocations(rootPanelLocation.getChildren());
            childrenTableModel.fireTableDataChanged();   
        }
        if (evt.getSource() != this && evt.getPropertyName() != "dirty") {
            setDirty(true);
        }
    }

    public void selectFiducial(Placement placement) {
        if (placement == null) {
            fiducialTable.getSelectionModel().clearSelection();
            return;
        }
        Logger.trace(String.format("Attempting to select Placement @%08x defined by @%08x", placement.hashCode(), placement.getDefinedBy().hashCode()));
        for (int i = 0; i < fiducialTableModel.getRowCount(); i++) {
            Logger.trace(String.format("...found Placement @%08x defined by @%08x", fiducialTableModel.getRowObjectAt(i).hashCode(), fiducialTableModel.getRowObjectAt(i).getDefinedBy().hashCode()));
            if (fiducialTableModel.getRowObjectAt(i) == placement.getDefinedBy()) {
                int index = fiducialTable.convertRowIndexToView(i);
                fiducialTable.getSelectionModel().setSelectionInterval(index, index);
                fiducialTable.scrollRectToVisible(new Rectangle(fiducialTable.getCellRect(index, 0, true)));
                break;
            }
        }
    }
    
    public void selectChild(PlacementsHolderLocation<?> child) {
        if (child == null) {
            childrenTable.getSelectionModel().clearSelection();
            return;
        }
        Logger.trace(String.format("Attempting to select %s @%08x defined by @%08x", child.getClass().getSimpleName(), child.hashCode(), child.getDefinedBy().hashCode()));
        for (int i = 0; i < childrenTableModel.getRowCount(); i++) {
            Logger.trace(String.format("...found %s @%08x defined by @%08x", childrenTableModel.getRowObjectAt(i).getClass().getSimpleName(), childrenTableModel.getRowObjectAt(i).hashCode(), ((AbstractLocatable<?>) childrenTableModel.getRowObjectAt(i)).getDefinedBy().hashCode()));
            if (childrenTableModel.getRowObjectAt(i) == child.getDefinedBy()) {
                int index = childrenTable.convertRowIndexToView(i);
                childrenTable.getSelectionModel().setSelectionInterval(index, index);
                childrenTable.scrollRectToVisible(new Rectangle(childrenTable.getCellRect(index, 0, true)));
                break;
            }
        }
    }
}
