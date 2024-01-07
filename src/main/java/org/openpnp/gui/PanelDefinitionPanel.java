/*
 * Copyright (C) 2023 Jason von Nieda <jason@vonnieda.org>, Tony Luken <tonyluken62+openpnp@gmail.com>
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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
import javax.swing.table.DefaultTableCellRenderer;
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
import org.openpnp.gui.support.MonospacedFontTableCellRenderer;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.IdentifiableListCellRenderer;
import org.openpnp.gui.support.IdentifiableTableCellRenderer;
import org.openpnp.gui.support.LengthCellValue;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.PartsComboBoxModel;
import org.openpnp.gui.support.RotationCellValue;
import org.openpnp.gui.support.TableUtils;
import org.openpnp.gui.tablemodel.PlacementsHolderLocationsTableModel;
import org.openpnp.gui.tablemodel.PlacementsHolderPlacementsTableModel;
import org.openpnp.gui.viewers.PlacementsHolderLocationViewerDialog;
import org.openpnp.model.Board;
import org.openpnp.model.Abstract2DLocatable.Side;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Configuration.TablesLinked;
import org.openpnp.model.PlacementsHolderLocation;
import org.openpnp.util.IdentifiableList;
import org.openpnp.model.Location;
import org.openpnp.model.Panel;
import org.openpnp.model.PanelLocation;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.openpnp.model.PlacementsHolder;

import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.border.EtchedBorder;
import java.awt.FileDialog;
import java.awt.Frame;

@SuppressWarnings("serial")
public class PanelDefinitionPanel extends JPanel implements PropertyChangeListener {
    private static final String PREF_DIVIDER_POSITION = "PanelDefinitionPanel.dividerPosition"; //$NON-NLS-1$
    private static final int PREF_DIVIDER_POSITION_DEF = -1;
    
    private Preferences prefs = Preferences.userNodeForPackage(PanelDefinitionPanel.class);
    
    private AutoSelectTextTable fiducialTable;
    private PlacementsHolderPlacementsTableModel fiducialTableModel;
    private TableRowSorter<PlacementsHolderPlacementsTableModel> fiducialTableSorter;
    
    private ActionGroup fiducialSingleSelectionActionGroup;
    private ActionGroup fiducialMultiSelectionActionGroup;
    
    private AutoSelectTextTable childrenTable;
    private PlacementsHolderLocationsTableModel childrenTableModel;
    private TableRowSorter<PlacementsHolderLocationsTableModel> childrenTableSorter;
    
    private ActionGroup childrenSingleSelectionActionGroup;
    private ActionGroup childrenMultiSelectionActionGroup;
    private ActionGroup replaceChildrenSelectionActionGroup;
    
    private PanelLocation rootPanelLocation = new PanelLocation();
    private Panel panel;
    private PanelsPanel panelsPanel;
    
    private PlacementsHolderLocationViewerDialog panelViewer;

    private JSplitPane splitPane;
    private MainFrame frame;
    private Configuration configuration;
    private boolean dirty;

    public PanelDefinitionPanel(PanelsPanel panelsPanel) {
    	this.panelsPanel = panelsPanel;
    	frame = MainFrame.get();
    	configuration = Configuration.get();
        createUi();
        addChildAction.setEnabled(false);
        viewerAction.setEnabled(false);
        addFiducialAction.setEnabled(false);
        useChildFiducialAction.setEnabled(false);
    }
    
    private void createUi() {
        setBorder(new TitledBorder(null, Translations.getString("PanelDefinition.Title"), //$NON-NLS-1$
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        
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

        replaceChildrenSelectionActionGroup = new ActionGroup(replaceChildrenAction);
        replaceChildrenSelectionActionGroup.setEnabled(false);
        
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
        pnlChildren.setBorder(new TitledBorder(null, Translations.getString("PanelDefinition.Children.Title"),  //$NON-NLS-1$
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        splitPane.setLeftComponent(pnlChildren);
        pnlChildren.setLayout(new BorderLayout(0, 0));
        
        JPanel pnlChildrenToolbar = new JPanel();
        pnlChildren.add(pnlChildrenToolbar, BorderLayout.NORTH);
        pnlChildrenToolbar.setLayout(new BorderLayout(0, 0));
        
        JToolBar toolBarChildren = new JToolBar();
        toolBarChildren.setFloatable(false);
        pnlChildrenToolbar.add(toolBarChildren);
        
        JButton btnAddChild = new JButton(addChildAction);
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
        btnRemoveChild.setHideActionText(true);
        toolBarChildren.add(btnRemoveChild);
        toolBarChildren.addSeparator();
        
        JButton btnCreateArray = new JButton(createArrayAction);
        btnCreateArray.setHideActionText(true);
        toolBarChildren.add(btnCreateArray);
        
        toolBarChildren.addSeparator();
        
        JButton btnViewer = new JButton(viewerAction);
        btnViewer.setHideActionText(true);
        toolBarChildren.add(btnViewer);
        
        childrenTableModel = new PlacementsHolderLocationsTableModel(configuration) {
            
            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return columnIndex <= 1 || columnIndex >= 4;
            }
        };
        childrenTableModel.setRootPanelLocation(rootPanelLocation);
        childrenTableSorter = new TableRowSorter<>(childrenTableModel);
        
        childrenTable = new AutoSelectTextTable(childrenTableModel);
        TableColumnModel tcm = childrenTable.getColumnModel();
        tcm.removeColumn(tcm.getColumn(7)); //remove Z column
        
        childrenTable.setRowSorter(childrenTableSorter);
        childrenTable.getTableHeader().setDefaultRenderer(new MultisortTableHeaderCellRenderer());
        childrenTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        childrenTable.setDefaultEditor(Side.class, new DefaultCellEditor(sidesComboBox));
        childrenTable.setDefaultRenderer(Boolean.class, new CustomBooleanRenderer());
        childrenTable.setDefaultRenderer(LengthCellValue.class, new MonospacedFontTableCellRenderer());
        childrenTable.setDefaultRenderer(RotationCellValue.class, new MonospacedFontTableCellRenderer());
        childrenTable.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
        
        TableUtils.setColumnAlignment(childrenTableModel, childrenTable);
        
        TableUtils.installColumnWidthSavers(childrenTable, prefs, "PanelDefinitionPanel.childrenTable.columnWidth"); //$NON-NLS-1$
        
        childrenTable.getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
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
                    boolean allSameDefinition = true;
                    boolean starting = true;
                    PlacementsHolder<?> definition = null;
                    for (PlacementsHolderLocation<?> phl : selections) {
                        if (starting) {
                            definition = phl.getPlacementsHolder().getDefinition();
                            starting = false;
                        }
                        else {
                            if (phl.getPlacementsHolder().getDefinition() != definition) {
                                allSameDefinition = false;
                                break;
                            }
                        }
                    }
                    replaceChildrenSelectionActionGroup.setEnabled(allSameDefinition);
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
                    replaceChildrenSelectionActionGroup.setEnabled(selections != null);
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
                    replaceChildrenSelectionActionGroup.setEnabled(false);
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

        JMenuItem changeChildrenMenu = new JMenuItem(replaceChildrenAction);
        childrenPopupMenu.add(changeChildrenMenu);
        
        JMenu setChildrenSideMenu = new JMenu(setSideAction);
        for (Side side : Side.values()) {
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
        pnlFiducials.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, 
                new Color(255, 255, 255), new Color(160, 160, 160)), 
                Translations.getString("PanelDefinition.PanelAlignment.Title"), //$NON-NLS-1$
                TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        splitPane.setRightComponent(pnlFiducials);
        pnlFiducials.setLayout(new BorderLayout(0, 0));
        
        JPanel pnlFiducialsToolbar = new JPanel();
        pnlFiducials.add(pnlFiducialsToolbar, BorderLayout.NORTH);
        pnlFiducialsToolbar.setLayout(new BorderLayout(0, 0));
        
        JToolBar toolBarFiducials = new JToolBar();
        toolBarFiducials.setFloatable(false);
        pnlFiducialsToolbar.add(toolBarFiducials, BorderLayout.CENTER);
        
        JButton btnAddFiducial = new JButton(addFiducialAction);
        btnAddFiducial.setHideActionText(true);
        toolBarFiducials.add(btnAddFiducial);
        
        JButton btnRemoveFiducial = new JButton(removeFiducialAction);
        btnRemoveFiducial.setHideActionText(true);
        toolBarFiducials.add(btnRemoveFiducial);
        
        JButton btnUseChildFiducial = new JButton(useChildFiducialAction);
        btnUseChildFiducial.setHideActionText(true);
        toolBarFiducials.add(btnUseChildFiducial);
        
        fiducialTableModel = new PlacementsHolderPlacementsTableModel(this) {
            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                if (!super.isCellEditable(rowIndex, columnIndex)) {
                    return false;
                }
                if (getRowObjectAt(rowIndex).getId().contains(PanelLocation.ID_DELIMITTER) && columnIndex > 0) {
                    return false;
                }
                return true;
            }
        };
        fiducialTableSorter = new TableRowSorter<>(fiducialTableModel);
        
        fiducialTable = new AutoSelectTextTable(fiducialTableModel);
        tcm = fiducialTable.getColumnModel();
        tcm.removeColumn(tcm.getColumn(11)); //remove Comments column
        tcm.removeColumn(tcm.getColumn(10)); //remove Error Handling column
        tcm.removeColumn(tcm.getColumn(9)); //remove Status column
        tcm.removeColumn(tcm.getColumn(8)); //remove Placed column
        tcm.removeColumn(tcm.getColumn(7)); //remove Type column
        
        fiducialTable.setRowSorter(fiducialTableSorter);
        fiducialTable.getTableHeader().setDefaultRenderer(new MultisortTableHeaderCellRenderer());
        fiducialTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        fiducialTable.setDefaultEditor(Side.class, new DefaultCellEditor(sidesComboBox));
        fiducialTable.setDefaultRenderer(Side.class, new DefaultTableCellRenderer());
        fiducialTable.setDefaultEditor(Part.class, new DefaultCellEditor(partsComboBox));
        fiducialTable.setDefaultRenderer(Part.class, new IdentifiableTableCellRenderer<Part>());
        fiducialTable.setDefaultRenderer(Boolean.class, new CustomBooleanRenderer());
        fiducialTable.setDefaultRenderer(LengthCellValue.class, new MonospacedFontTableCellRenderer());
        fiducialTable.setDefaultRenderer(RotationCellValue.class, new MonospacedFontTableCellRenderer());
        fiducialTable.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
        
        TableUtils.setColumnAlignment(fiducialTableModel, fiducialTable);
        
        TableUtils.installColumnWidthSavers(fiducialTable, prefs, "PanelDefinitionPanel.fiducialTable.columnWidth"); //$NON-NLS-1$
        
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
        for (Side side : Side.values()) {
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
    
    public void setPanel(Panel panel) throws IOException {
        this.panel = panel;
        rootPanelLocation.setGlobalSide(Side.Top);
        rootPanelLocation.setPanel(panel);
        childrenTableModel.setPlacementsHolderLocations(rootPanelLocation.getChildren());
        fiducialTableModel.setPlacementsHolder(rootPanelLocation.getPanel());
        addChildAction.setEnabled(panel != null);
        viewerAction.setEnabled(panel != null);
        addFiducialAction.setEnabled(panel != null);
        useChildFiducialAction.setEnabled(panel != null);
        if (panelViewer != null) {
            panelViewer.setPlacementsHolder(panel);
        }
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
        firePropertyChange("dirty", oldValue, dirty); //$NON-NLS-1$
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
        List<Placement> placements = new IdentifiableList<>(panel.getPlacements());
        placements.addAll(panel.getPseudoPlacements());
        for (int selectedRow : selectedRows) {
            selectedRow = fiducialTable.convertRowIndexToModel(selectedRow);
            fiducials.add(placements.get(selectedRow));
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
            selectedChildren.add((PlacementsHolderLocation<?>) childrenTableModel.getRowObjectAt(selectedRow));
        }
        return selectedChildren;
    }

    public final Action addFiducialAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.add);
            putValue(NAME, Translations.getString("PanelDefinition.PanelAlignment.Add")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("PanelDefinition.PanelAlignment.Add.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (Configuration.get().getParts().size() == 0) {
                MessageBoxes.errorBox(getTopLevelAncestor(), Translations.getString("General.Error"), //$NON-NLS-1$
                        Translations.getString("PanelDefinition.PanelAlignment.Add.Error.NoParts")); //$NON-NLS-1$
                return;
            }

            String id = JOptionPane.showInputDialog(getTopLevelAncestor(),
                    Translations.getString("PanelDefinition.PanelAlignment.Add.EnterIdMessage")); //$NON-NLS-1$
            if (id == null) {
                return;
            }
            
            // Check if the new placement ID is unique
            for(Placement comparePlacement : rootPanelLocation.getPanel().getPlacements()) {
                if (comparePlacement.getId().equals(id)) {
                    MessageBoxes.errorBox(getTopLevelAncestor(), Translations.getString("General.Error"), //$NON-NLS-1$
                            Translations.getString("PanelDefinition.PanelAlignment.Add.Error.IdExists")); //$NON-NLS-1$
                    return;
                }
            }
            
            Placement placement = new Placement(id);

            placement.setPart(Configuration.get().getParts().get(0));
            placement.setLocation(new Location(Configuration.get().getSystemUnits()));
            placement.setSide(rootPanelLocation.getGlobalSide());
            placement.setType(Placement.Type.Fiducial);

            panel.addPlacement(placement);

            Configuration.get().getBus()
                .post(new DefinitionStructureChangedEvent(panel, "placements", PanelDefinitionPanel.this)); //$NON-NLS-1$

            fiducialTableModel.fireTableDataChanged();
            
            Helpers.selectObjectTableRow(fiducialTable, placement);
        }
    };

    public final Action removeFiducialAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.delete);
            putValue(NAME, Translations.getString("PanelDefinition.PanelAlignment.Remove")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("PanelDefinition.PanelAlignment.Remove.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            for (Placement placement : getFiducialSelections()) {
                if (panel.getPseudoPlacements().contains(placement)) {
                    panel.removePseudoPlacement(placement);
                }
                else {
                    panel.removePlacement(placement);
                }
                placement.dispose();
            }
            fiducialTableModel.fireTableDataChanged();
            
            Configuration.get().getBus()
                .post(new DefinitionStructureChangedEvent(panel, "placements", PanelDefinitionPanel.this)); //$NON-NLS-1$
        }
    };

    public final Action useChildFiducialAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.useChildFiducial);
            putValue(NAME, Translations.getString("PanelDefinition.PanelAlignment.UseChildren")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("PanelDefinition.PanelAlignment.UseChildren.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            ChildFiducialSelectorDialog dialog = new ChildFiducialSelectorDialog(rootPanelLocation);
            dialog.setVisible(true);
            
            Configuration.get().getBus()
                .post(new DefinitionStructureChangedEvent(panel, "placements", PanelDefinitionPanel.this)); //$NON-NLS-1$

            fiducialTableModel.fireTableDataChanged();
        }
    };

    public final Action addChildAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.add);
            putValue(NAME, Translations.getString("PanelDefinition.Children.Add")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("PanelDefinition.Children.Add.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
        }
    };

    public final Action addNewBoardAction = new AbstractAction() {
        {
            putValue(NAME, Translations.getString("PanelDefinition.Children.Add.NewBoard")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("PanelDefinition.Children.Add.NewBoard.Description")); //$NON-NLS-1$
            putValue(MNEMONIC_KEY, KeyEvent.VK_N);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            FileDialog fileDialog = new FileDialog(frame, 
                    Translations.getString("PanelDefinition.Children.Add.NewBoard.DialogTitle"), //$NON-NLS-1$
                    FileDialog.SAVE);
            fileDialog.setFilenameFilter(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".board.xml"); //$NON-NLS-1$
                }
            });
            fileDialog.setFile("*.board.xml"); //$NON-NLS-1$
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

                BoardLocation newBoardLocation = addBoard(file);
                Helpers.selectObjectTableRow(childrenTable, newBoardLocation);
            }
            catch (Exception e) {
                e.printStackTrace();
                MessageBoxes.errorBox(frame, 
                        Translations.getString("PanelDefinition.Children.Add.NewBoard.SaveError"), //$NON-NLS-1$
                        e.getMessage());
            }
        }
    };

    public final Action addExistingBoardAction = new AbstractAction() {
        {
            putValue(NAME, Translations.getString("PanelDefinition.Children.Add.ExistingBoard")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("PanelDefinition.Children.Add.ExistingBoard.Description")); //$NON-NLS-1$
            putValue(MNEMONIC_KEY, KeyEvent.VK_E);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            ExistingBoardOrPanelDialog existingBoardDialog = new ExistingBoardOrPanelDialog(
                    Configuration.get(), Board.class,
                    Translations.getString("PanelDefinition.Children.Add.ExistingBoard.DialogTitle")); //$NON-NLS-1$
            existingBoardDialog.setVisible(true);
            File file = existingBoardDialog.getFile();
            existingBoardDialog.dispose();
            if (file == null) {
                return;
            }
            try {
                BoardLocation newBoardLocation = addBoard(file);
                Helpers.selectObjectTableRow(childrenTable, newBoardLocation);
            }
            catch (Exception e) {
                e.printStackTrace();
                MessageBoxes.errorBox(frame, 
                        Translations.getString("PanelDefinition.Children.Add.ExistingBoard.LoadError"), //$NON-NLS-1$
                        e.getMessage());
            }
        }
    };

    protected BoardLocation addBoard(File file) throws Exception {
        //Make a deep copy of the board's definition to add to the panel
        Board board = new Board(configuration.getBoard(file));
        
        BoardLocation boardLocation = new BoardLocation(board);
        boardLocation.setParent(rootPanelLocation);
        panel.addChild(boardLocation);
        childrenTableModel.fireTableDataChanged();
        
        Configuration.get().getBus()
            .post(new DefinitionStructureChangedEvent(rootPanelLocation.getPanel(), "children",  //$NON-NLS-1$
                    PanelDefinitionPanel.this));
        
        return boardLocation;
    }
    
    public final Action addNewPanelAction = new AbstractAction() {
        {
            putValue(NAME, Translations.getString("PanelDefinition.Children.Add.NewPanel")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("PanelDefinition.Children.Add.NewPanel.Description")); //$NON-NLS-1$
//            putValue(MNEMONIC_KEY, KeyEvent.VK_N);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            FileDialog fileDialog = new FileDialog(frame, 
                    Translations.getString("PanelDefinition.Children.Add.NewPanel.DialogTitle"), //$NON-NLS-1$
                    FileDialog.SAVE);
            fileDialog.setFilenameFilter(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".panel.xml"); //$NON-NLS-1$
                }
            });
            fileDialog.setFile("*.panel.xml"); //$NON-NLS-1$
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

                PanelLocation newPanelLocation = addPanel(file);
                Helpers.selectObjectTableRow(childrenTable, newPanelLocation);
            }
            catch (Exception e) {
                e.printStackTrace();
                MessageBoxes.errorBox(frame, 
                        Translations.getString("PanelDefinition.Children.Add.NewPanel.SaveError"), //$NON-NLS-1$
                        e.getMessage());
            }
        }
    };

    public final Action addExistingPanelAction = new AbstractAction() {
        {
            putValue(NAME, Translations.getString("PanelDefinition.Children.Add.ExistingPanel")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("PanelDefinition.Children.Add.ExistingPanel.Description")); //$NON-NLS-1$
//            putValue(MNEMONIC_KEY, KeyEvent.VK_E);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            ExistingBoardOrPanelDialog existingPanelDialog = new ExistingBoardOrPanelDialog(
                    Configuration.get(), Panel.class, 
                    Translations.getString("PanelDefinition.Children.Add.ExistingPanel.DialogTitle")); //$NON-NLS-1$
            existingPanelDialog.setVisible(true);
            File file = existingPanelDialog.getFile();
            existingPanelDialog.dispose();
            if (file == null) {
                return;
            }
            try {
                PanelLocation newPanelLocation = addPanel(file);
                Helpers.selectObjectTableRow(childrenTable, newPanelLocation);
            }
            catch (Exception e) {
                e.printStackTrace();
                MessageBoxes.errorBox(frame, 
                        Translations.getString("PanelDefinition.Children.Add.ExistingPanel.LoadError"), //$NON-NLS-1$
                        e.getMessage());
            }
        }
    };

    protected PanelLocation addPanel(File file) throws Exception {
        //Make a deep copy of the panel's definition to add to the panel
        Panel newPanel = new Panel(configuration.getPanel(file));
        
        PanelLocation panelLocation = new PanelLocation(newPanel);
        verifyNoCircularReferences(rootPanelLocation, panelLocation);
        panel.addChild(panelLocation);
        PanelLocation.setParentsOfAllDescendants(rootPanelLocation);
        childrenTableModel.fireTableDataChanged();
        
        Configuration.get().getBus()
            .post(new DefinitionStructureChangedEvent(panel, "children", PanelDefinitionPanel.this)); //$NON-NLS-1$
        
        return panelLocation;
    }
    
    private void verifyNoCircularReferences(PanelLocation root, PanelLocation decendant) throws Exception {
        if (decendant.getPanel().getFile().equals(root.getPanel().getFile())) {
            throw new Exception(Translations.getString("PanelDefinition.Children.Add.CircularReferenceError")); //$NON-NLS-1$
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
            putValue(NAME, Translations.getString("PanelDefinition.Children.Remove")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("PanelDefinition.Children.Remove.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            List<PlacementsHolderLocation<?>> selectedChildren = getChildrenSelections();
            for (PlacementsHolderLocation<?> child : selectedChildren) {
                rootPanelLocation.getPanel().getDefinition().removeChild(child);
            }
            childrenTableModel.fireTableDataChanged();
            
            Configuration.get().getBus()
                .post(new DefinitionStructureChangedEvent(rootPanelLocation.getPanel(), "children", //$NON-NLS-1$
                        PanelDefinitionPanel.this));
        }
    };

    public final Action createArrayAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.autoPanelize);
            putValue(NAME, Translations.getString("PanelDefinition.Children.CreateArray")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("PanelDefinition.Children.CreateArray.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            PlacementsHolderLocation<?> child = getChildrenSelection();
            PanelArrayBuilderDialog dlg = new PanelArrayBuilderDialog(rootPanelLocation, child, () -> refresh());
            dlg.setVisible(true);
            
            Configuration.get().getBus()
            .post(new DefinitionStructureChangedEvent(rootPanelLocation.getPanel(), "children", //$NON-NLS-1$
                    PanelDefinitionPanel.this));
            
            Helpers.selectObjectTableRow(childrenTable, child);
        }
    };
    
    public final Action viewerAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.colorTrue);
            putValue(NAME, ""); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION,
                    Translations.getString("PanelDefinition.Children.ViewPanel.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (panelViewer == null) {
                panelViewer = new PlacementsHolderLocationViewerDialog(rootPanelLocation, false);
                panelViewer.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        panelViewer = null;
                    }
                });
            }
            else {
                panelViewer.setExtendedState(Frame.NORMAL);
            }
            panelViewer.setVisible(true);
        }
    };

    public final Action replaceChildrenAction = new AbstractAction() {
        {
            putValue(NAME, Translations.getString("PanelDefinition.Children.Replace")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("PanelDefinition.Children.Replace.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            List<PlacementsHolderLocation<?>> selectedChildren = getChildrenSelections();
            ExistingBoardOrPanelDialog existingBoardOrPanelDialog;
            File file;
            if (selectedChildren.get(0) instanceof BoardLocation) {
                existingBoardOrPanelDialog = new ExistingBoardOrPanelDialog(
                        Configuration.get(), Board.class,
                        Translations.getString("PanelDefinition.Children.Replace.ExistingBoard.DialogTitle")); //$NON-NLS-1$
            }
            else {
                existingBoardOrPanelDialog = new ExistingBoardOrPanelDialog(
                        Configuration.get(), Panel.class,
                        Translations.getString("PanelDefinition.Children.Replace.ExistingPanel.DialogTitle")); //$NON-NLS-1$
            }

            existingBoardOrPanelDialog.setVisible(true);
            file = existingBoardOrPanelDialog.getFile();
            existingBoardOrPanelDialog.dispose();
            if (file == null) {
                return;
            }
                
            try {
                if (selectedChildren.get(0) instanceof BoardLocation) {
                    for (PlacementsHolderLocation<?> oldChild : selectedChildren) {
                        //Make a deep copy of the board's definition to add to the panel
                        Board board = new Board(configuration.getBoard(file));
                        
                        BoardLocation boardLocation = new BoardLocation(board);
                        boardLocation.setParent(oldChild.getParent());
                        panel.replaceChild(oldChild, boardLocation);
                    }
                }
                else {
                    for (PlacementsHolderLocation<?> oldChild : selectedChildren) {
                        //Make a deep copy of the panel's definition to add to the panel
                        Panel panelCopy = new Panel(configuration.getPanel(file));
                        
                        PanelLocation panelLocation = new PanelLocation(panelCopy);
                        panelLocation.setParent(oldChild.getParent());
                        PanelLocation.setParentsOfAllDescendants(panelLocation);
                        panel.replaceChild(oldChild, panelLocation);
                    }
                }
                
                childrenTableModel.fireTableDataChanged();
                
                Configuration.get().getBus()
                    .post(new DefinitionStructureChangedEvent(rootPanelLocation.getPanel(), "children", //$NON-NLS-1$
                            PanelDefinitionPanel.this));
            }
            catch (Exception e) {
                e.printStackTrace();
                MessageBoxes.errorBox(frame, 
                        Translations.getString("PanelDefinition.Children.Replace.LoadError"), //$NON-NLS-1$
                        e.getMessage());
            }
        }
    };

    public final Action setSideAction = new AbstractAction() {
        {
            putValue(NAME, Translations.getString("PanelDefinition.SetSide")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("PanelDefinition.SetSide.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {}
    };

    class SetFiducialSideAction extends AbstractAction {
        final Side side;

        public SetFiducialSideAction(Side side) {
            this.side = side;
            String name = side == Side.Top ?
                    Translations.getString("Placement.Side.Top") : //$NON-NLS-1$
                    Translations.getString("Placement.Side.Bottom"); //$NON-NLS-1$
            putValue(NAME, name); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("PanelDefinition.SetSide.Fiducials.Description") + //$NON-NLS-1$
                    " " + name); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            List<Placement> selections = getFiducialSelections();
            for (Placement fiducial : selections) {
                fiducial.setSide(side);
                fiducialTableModel.fireTableCellUpdated(fiducial, 
                        Translations.getString("PlacementsHolderPlacementsTableModel.ColumnName.Side")); //$NON-NLS-1$
            }
            Helpers.selectObjectTableRows(fiducialTable, selections);
        }
    };
    
    class SetChildrenSideAction extends AbstractAction {
        final Side side;

        public SetChildrenSideAction(Side side) {
            this.side = side;
            String name = side == Side.Top ?
                    Translations.getString("Placement.Side.Top") : //$NON-NLS-1$
                    Translations.getString("Placement.Side.Bottom"); //$NON-NLS-1$
            putValue(NAME, name); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("PanelDefinition.SetSide.Children.Description") +  //$NON-NLS-1$
                    " " + name); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            List<PlacementsHolderLocation<?>> selections = getChildrenSelections();
            for (PlacementsHolderLocation<?> child : selections) {
                child.getDefinition().setGlobalSide(side);
                childrenTableModel.fireTableCellDecendantsUpdated(child, 
                        Translations.getString("PlacementsHolderLocationsTableModel.ColumnName.Side")); //$NON-NLS-1$
            }
            Helpers.selectObjectTableRows(childrenTable, selections);
        }
    };
    
    public final Action setEnabledAction = new AbstractAction() {
        {
            putValue(NAME, Translations.getString("PanelDefinition.SetEnabled")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("PanelDefinition.SetEnabled.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {}
    };

    class SetFiducialEnabledAction extends AbstractAction {
        final Boolean enabled;

        public SetFiducialEnabledAction(Boolean enabled) {
            this.enabled = enabled;
            String name = enabled ? 
                    Translations.getString("General.Enabled") : //$NON-NLS-1$
                    Translations.getString("General.Disabled"); //$NON-NLS-1$
            putValue(NAME, name);
            putValue(SHORT_DESCRIPTION, Translations.getString("PanelDefinition.SetEnabled.Fiducials.Description") //$NON-NLS-1$
                    + " " + name); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            List<Placement> selections = getFiducialSelections();
            for (Placement fiducial : selections) {
                fiducial.setEnabled(enabled);
                fiducialTableModel.fireTableCellUpdated(fiducial, 
                        Translations.getString("PlacementsHolderPlacementsTableModel.ColumnName.Enabled")); //$NON-NLS-1$
            }
            Helpers.selectObjectTableRows(fiducialTable, selections);
        }
    };

    class SetChildrenEnabledAction extends AbstractAction {
        final Boolean enabled;

        public SetChildrenEnabledAction(Boolean enabled) {
            this.enabled = enabled;
            String name = enabled ?
                    Translations.getString("General.Enabled") : //$NON-NLS-1$
                    Translations.getString("General.Disabled"); //$NON-NLS-1$
            putValue(NAME, name);
            putValue(SHORT_DESCRIPTION, Translations.getString("PanelDefinition.SetEnabled.Children.Description") //$NON-NLS-1$
                    + " " + name); //$NON-NLS-2$ //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            List<PlacementsHolderLocation<?>> selections = getChildrenSelections();
            for (PlacementsHolderLocation<?> child : selections) {
                child.getDefinition().setLocallyEnabled(enabled);
                childrenTableModel.fireTableCellDecendantsUpdated(child, 
                        Translations.getString("PlacementsHolderLocationsTableModel.ColumnName.Enabled")); //$NON-NLS-1$
            }
            Helpers.selectObjectTableRows(childrenTable, selections);
        }
    };

    public final Action setCheckFidsAction = new AbstractAction() {
        {
            putValue(NAME, Translations.getString("PanelDefinition.SetCheckFids")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("PanelDefinition.SetCheckFids.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {}
    };

    class SetCheckFidsAction extends AbstractAction {
        final Boolean value;

        public SetCheckFidsAction(Boolean value) {
            this.value = value;
            String name = value ?
                    Translations.getString("Fiducial.Check.Check") :  //$NON-NLS-1$
                    Translations.getString("Fiducial.Check.NoCheck"); //$NON-NLS-1$
            putValue(NAME, name);
            putValue(SHORT_DESCRIPTION, Translations.getString("PanelDefinition.SetCheckFids.Children.Description") //$NON-NLS-1$
                    + " " + name); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            List<PlacementsHolderLocation<?>> selections = getChildrenSelections();
            for (PlacementsHolderLocation<?> child : selections) {
                child.getDefinition().setCheckFiducials(value);
                childrenTableModel.fireTableCellUpdated(child, 
                        Translations.getString("PlacementsHolderLocationsTableModel.ColumnName.CheckFids")); //$NON-NLS-1$
            }
            Helpers.selectObjectTableRows(childrenTable, selections);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName() == "children") { //$NON-NLS-1$
            childrenTableModel.setPlacementsHolderLocations(rootPanelLocation.getChildren());
            childrenTableModel.fireTableDataChanged();   
        }
        if (evt.getSource() != this && evt.getPropertyName() != "dirty") { //$NON-NLS-1$
            setDirty(true);
        }
    }

    public void selectFiducial(Placement placement) {
        if (placement == null) {
            fiducialTable.getSelectionModel().clearSelection();
            return;
        }
        for (int i = 0; i < fiducialTableModel.getRowCount(); i++) {
            if (fiducialTableModel.getRowObjectAt(i) == placement.getDefinition()) {
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
        for (int i = 0; i < childrenTableModel.getRowCount(); i++) {
            if (childrenTableModel.getRowObjectAt(i) == child.getDefinition()) {
                int index = childrenTable.convertRowIndexToView(i);
                childrenTable.getSelectionModel().setSelectionInterval(index, index);
                childrenTable.scrollRectToVisible(new Rectangle(childrenTable.getCellRect(index, 0, true)));
                break;
            }
        }
    }
}
