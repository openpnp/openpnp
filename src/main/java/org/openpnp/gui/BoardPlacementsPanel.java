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
import java.awt.Component;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;

import org.openpnp.events.DefinitionStructureChangedEvent;
import org.openpnp.events.PlacementSelectedEvent;
import org.openpnp.gui.components.AutoSelectTextTable;
import org.openpnp.gui.importer.BoardImporter;
import org.openpnp.gui.support.ActionGroup;
import org.openpnp.gui.support.CustomBooleanRenderer;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.IdentifiableListCellRenderer;
import org.openpnp.gui.support.IdentifiableTableCellRenderer;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.PartsComboBoxModel;
import org.openpnp.gui.tablemodel.PlacementsHolderPlacementsTableModel;
import org.openpnp.gui.tablemodel.PlacementsTableModel.Status;
import org.openpnp.model.AbstractLocatable.Side;
import org.openpnp.model.Board;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.BoardPad;
import org.openpnp.model.Configuration;
import org.openpnp.model.Configuration.TablesLinked;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.openpnp.model.Placement.ErrorHandling;
import org.openpnp.model.Placement.Type;
import org.openpnp.util.IdentifiableList;
import org.pmw.tinylog.Logger;

import com.google.common.eventbus.Subscribe;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;

@SuppressWarnings("serial")
public class BoardPlacementsPanel extends JPanel {
    private JTable table;
    private PlacementsHolderPlacementsTableModel tableModel;
    private TableRowSorter<PlacementsHolderPlacementsTableModel> tableSorter;
    private ActionGroup singleSelectionActionGroup;
    private ActionGroup multiSelectionActionGroup;
    private BoardsPanel boardsPanel;
    private Board board;

    private static Color typeColorFiducial = new Color(157, 188, 255);
    private static Color typeColorPlacement = new Color(255, 255, 255);
    
    public BoardPlacementsPanel(BoardsPanel boardsPanel) {
    	this.boardsPanel = boardsPanel;
        createUi();
    }
    
    private void createUi() {
        setBorder(new TitledBorder(null, "Placements", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        
        Configuration configuration = Configuration.get();
        
        singleSelectionActionGroup = new ActionGroup(removeAction, 
                setTypeAction, setSideAction, setErrorHandlingAction, setEnabledAction);
        singleSelectionActionGroup.setEnabled(false);

        multiSelectionActionGroup = new ActionGroup(removeAction,
                setTypeAction, setSideAction, setErrorHandlingAction, setEnabledAction);
        multiSelectionActionGroup.setEnabled(false);

        @SuppressWarnings("unchecked")
        JComboBox<PartsComboBoxModel> partsComboBox = new JComboBox<>(new PartsComboBoxModel());
        partsComboBox.setMaximumRowCount(20);
        partsComboBox.setRenderer(new IdentifiableListCellRenderer<Part>());
        JComboBox<Side> sidesComboBox = new JComboBox<>(Side.values());
        // Note we don't use Type.values() here because there are a couple Types that are only
        // there for backwards compatibility and we don't want them in the list.
        JComboBox<Type> typesComboBox = new JComboBox<>(new Type[] { Type.Placement, Type.Fiducial });
        JComboBox<ErrorHandling> errorHandlingComboBox = new JComboBox<>(ErrorHandling.values());
        
        setLayout(new BorderLayout(0, 0));
        tableModel = new PlacementsHolderPlacementsTableModel();
        tableSorter = new TableRowSorter<>(tableModel);
        
        table = new AutoSelectTextTable(tableModel);
        table.setRowSorter(tableSorter);
        table.getTableHeader().setDefaultRenderer(new MultisortTableHeaderCellRenderer());
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setDefaultEditor(Side.class, new DefaultCellEditor(sidesComboBox));
        table.setDefaultEditor(Part.class, new DefaultCellEditor(partsComboBox));
        table.setDefaultEditor(Type.class, new DefaultCellEditor(typesComboBox));
        table.setDefaultEditor(ErrorHandling.class, new DefaultCellEditor(errorHandlingComboBox));
        table.setDefaultRenderer(Part.class, new IdentifiableTableCellRenderer<Part>());
        table.setDefaultRenderer(Placement.Type.class, new TypeRenderer());
        table.setDefaultRenderer(Boolean.class, new CustomBooleanRenderer());
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }

                boolean updateLinkedTables = MainFrame.get().getTabs().getSelectedComponent() == MainFrame.get().getBoardsTab() 
                        && Configuration.get().getTablesLinked() == TablesLinked.Linked;
                
                if (getSelections().size() > 1) {
                    // multi select
                    singleSelectionActionGroup.setEnabled(false);
                    multiSelectionActionGroup.setEnabled(true);
                }
                else {
                    // single select, or no select
                    multiSelectionActionGroup.setEnabled(false);
                    singleSelectionActionGroup.setEnabled(getSelection() != null);
                    MainFrame mainFrame = MainFrame.get();
                    Component selectedComponent = mainFrame.getTabs().getSelectedComponent();
                    if (updateLinkedTables) {
                        Configuration.get().getBus().post(new PlacementSelectedEvent(getSelection(),
                                new BoardLocation(board), BoardPlacementsPanel.this));
                    }
                    if (getSelection() != null
                            && (selectedComponent == mainFrame.getJobTab() ||
                                    selectedComponent == mainFrame.getBoardsTab())
                            && Configuration.get().getTablesLinked() == TablesLinked.Linked) {
                        Part selectedPart = getSelection().getPart();
                        mainFrame.getPartsTab().selectPartInTable(selectedPart);
                        mainFrame.getPackagesTab().selectPackageInTable(selectedPart.getPackage());
                        mainFrame.getFeedersTab().selectFeederForPart(selectedPart);
                        mainFrame.getVisionSettingsTab().selectVisionSettingsInTable(selectedPart);
                    }
                }
            }
        });
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent mouseEvent) {
                if (mouseEvent.getClickCount() != 2) {
                    return;
                }
                int row = table.rowAtPoint(new Point(mouseEvent.getX(), mouseEvent.getY()));
                int col = table.columnAtPoint(new Point(mouseEvent.getX(), mouseEvent.getY()));
                if (tableModel.getColumnClass(col) == Status.class) {
                    Status status = (Status) tableModel.getValueAt(row, col);
                    // TODO: This is some sample code for handling the user
                    // wishing to do something with the status. Not using it
                    // right now but leaving it here for the future.
                    System.out.println(status);
                }
            }
        });
        table.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == ' ') {
                    Placement placement = getSelection();
                    placement.setEnabled(!placement.isEnabled());
                    refreshSelectedRow();
                }
                else {
                    super.keyTyped(e);
                }
            }
        });
        
        JPopupMenu popupMenu = new JPopupMenu();

        JMenu setTypeMenu = new JMenu(setTypeAction);
        setTypeMenu.add(new SetTypeAction(Placement.Type.Placement));
        setTypeMenu.add(new SetTypeAction(Placement.Type.Fiducial));
        popupMenu.add(setTypeMenu);

        JMenu setSideMenu = new JMenu(setSideAction);
        for (Side side : Side.values()) {
            setSideMenu.add(new SetSideAction(side));
        }
        popupMenu.add(setSideMenu);

        JMenu setEnabledMenu = new JMenu(setEnabledAction);
        setEnabledMenu.add(new SetEnabledAction(true));
        setEnabledMenu.add(new SetEnabledAction(false));
        popupMenu.add(setEnabledMenu);

        JMenu setErrorHandlingMenu = new JMenu(setErrorHandlingAction);
        setErrorHandlingMenu.add(new SetErrorHandlingAction(ErrorHandling.Alert));
        setErrorHandlingMenu.add(new SetErrorHandlingAction(ErrorHandling.Defer));
        popupMenu.add(setErrorHandlingMenu);

        table.setComponentPopupMenu(popupMenu);

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);
        
        JPanel panel = new JPanel();
        add(panel, BorderLayout.NORTH);
        panel.setLayout(new BorderLayout(0, 0));
        JToolBar toolBarPlacements = new JToolBar();
        panel.add(toolBarPlacements);
        
        toolBarPlacements.setFloatable(false);
        JButton btnNewPlacement = new JButton(newAction);
        btnNewPlacement.setHideActionText(true);
        toolBarPlacements.add(btnNewPlacement);
        JButton btnRemovePlacement = new JButton(removeAction);
        btnRemovePlacement.setHideActionText(true);
        toolBarPlacements.add(btnRemovePlacement);

        List<Class<? extends BoardImporter>> boardImporters = new ArrayList<>();
        try (ScanResult scanResult = new ClassGraph().enableClassInfo()
                .acceptPackages(BoardImporter.class.getPackageName()).scan()) {
            ClassInfoList importerClassInfoList = scanResult.
                    getClassesImplementing(BoardImporter.class.getCanonicalName());
            for (ClassInfo boardImporterInfo : importerClassInfoList) {
                boardImporters.add((Class<? extends BoardImporter>) (boardImporterInfo.loadClass()));
            }
        }
        
        toolBarPlacements.addSeparator();
        JButton btnImport = new JButton(importAction);
        btnImport.setHideActionText(true);
        btnImport.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                JPopupMenu menu = new JPopupMenu();
                for (Class<? extends BoardImporter> bi : boardImporters) {
                    final BoardImporter boardImporter;
                    try {
                        boardImporter = bi.newInstance();
                    }
                    catch (Exception ex) {
                        throw new Error(ex);
                    }

                    menu.add(new JMenuItem(new AbstractAction() {
                        {
                            putValue(NAME, boardImporter.getImporterName());
                            putValue(SHORT_DESCRIPTION, boardImporter.getImporterDescription());
                            putValue(MNEMONIC_KEY, KeyEvent.VK_I);
                        }

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            importBoard(bi);
                            refresh();
                        }
                    }));
                }
                menu.show(btnImport, (int) btnImport.getWidth(), (int) btnImport.getHeight());
            }
        });
        importAction.setEnabled(false);
        toolBarPlacements.add(btnImport);

        JPanel panel_1 = new JPanel();
        panel.add(panel_1, BorderLayout.EAST);

        JLabel lblNewLabel = new JLabel("Search");
        panel_1.add(lblNewLabel);

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

        Configuration.get().getBus().register(this);
    }
    
    @Subscribe
    public void boardDefinitionStructureChanged(DefinitionStructureChangedEvent event) {
        Logger.trace("boardDefinitionStructureChanged DefinitionStructureChangedEvent = " + event);
        if (board != null && 
                event.definition == board) {
            SwingUtilities.invokeLater(() -> {
                refresh();
            });
        }
    }

    private void search() {
        updateRowFilter();
    }
    
    public void refresh() {
        tableModel.fireTableDataChanged();
    }

    public void refreshSelectedRow() {
        int index = table.convertRowIndexToModel(table.getSelectedRow());
        tableModel.fireTableRowsUpdated(index, index);
    }

    public void selectPlacement(Placement placement) {
        if (placement == null) {
            table.getSelectionModel().clearSelection();
        }
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (tableModel.getRowObjectAt(i) == placement) {
                int index = table.convertRowIndexToView(i);
                table.getSelectionModel().setSelectionInterval(index, index);
                table.scrollRectToVisible(new Rectangle(table.getCellRect(index, 0, true)));
                break;
            }
        }
    }
    
    private void updateRowFilter() {
        List<RowFilter<PlacementsHolderPlacementsTableModel, Integer>> filters = new ArrayList<>();
        
        try {
            RowFilter<PlacementsHolderPlacementsTableModel, Integer> searchFilter = RowFilter.regexFilter("(?i)" + searchTextField.getText().trim());
            filters.add(searchFilter);
        }
        catch (PatternSyntaxException e) {
        }
        
        tableSorter.setRowFilter(RowFilter.andFilter(filters));
    }
    
    
    public void setBoard(Board board) {
        this.board = board;
        tableModel.setFiducialLocatable(board);
        importAction.setEnabled(board != null);
        updateRowFilter();
    }

    public Placement getSelection() {
        List<Placement> selectedPlacements = getSelections();
        if (selectedPlacements.isEmpty()) {
            return null;
        }
        return selectedPlacements.get(0);
    }

    public List<Placement> getSelections() {
        ArrayList<Placement> placements = new ArrayList<>();
        if (board == null) {
            return placements;
        }
        int[] selectedRows = table.getSelectedRows();
        for (int selectedRow : selectedRows) {
            selectedRow = table.convertRowIndexToModel(selectedRow);
            placements.add(board.getPlacements().get(selectedRow));
        }
        return placements;
    }

    public final Action newAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.add);
            putValue(NAME, "New Placement");
            putValue(SHORT_DESCRIPTION, "Create a new placement and add it to the board.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (Configuration.get().getParts().size() == 0) {
                MessageBoxes.errorBox(getTopLevelAncestor(), "Error",
                        "There are currently no parts defined in the system. Please create at least one part before creating a placement.");
                return;
            }

            String id = JOptionPane.showInputDialog(getTopLevelAncestor(),
                    "Please enter an ID for the new placement.");
            if (id == null) {
                return;
            }
            
            // Check if the new placement ID is unique
            for(Placement compareplacement : board.getPlacements()) {
                if (compareplacement.getId().equals(id)) {
                    MessageBoxes.errorBox(getTopLevelAncestor(), "Error",
                            "The ID for the new placement already exists");
                    return;
                }
            }
            
            Placement placement = new Placement(id);

            placement.setPart(Configuration.get().getParts().get(0));
            placement.setLocation(new Location(Configuration.get().getSystemUnits()));
            placement.setSide(Side.Top);

            board.addPlacement(placement);
            tableModel.fireTableDataChanged();
            Helpers.selectLastTableRow(table);

            Configuration.get().getBus()
                .post(new DefinitionStructureChangedEvent(board, "placements", BoardPlacementsPanel.this));
        }
    };

    public final Action removeAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.delete);
            putValue(NAME, "Remove Placement(s)");
            putValue(SHORT_DESCRIPTION, "Remove the currently selected placement(s).");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            for (Placement placement : getSelections()) {
                board.removePlacement(placement);
            }
            tableModel.fireTableDataChanged();

            Configuration.get().getBus()
                .post(new DefinitionStructureChangedEvent(board, "placements", BoardPlacementsPanel.this));
        }
    };

    public void importBoard(Class<? extends BoardImporter> boardImporterClass) {
        if (boardsPanel.getSelection() == null) {
            MessageBoxes.errorBox(getTopLevelAncestor(), "Import Failed", //$NON-NLS-1$
                    "Please select a board to import into."); //$NON-NLS-1$
            return;
        }
        
        BoardImporter boardImporter;
        try {
            boardImporter = boardImporterClass.newInstance();
        }
        catch (Exception e) {
            MessageBoxes.errorBox(getTopLevelAncestor(), "Import Failed", e); //$NON-NLS-1$
            return;
        }

        try {
            Board importedBoard = boardImporter.importBoard((Frame) getTopLevelAncestor());
            if (importedBoard != null) {
                Board existingBoard = boardsPanel.getSelection();
                IdentifiableList<Placement> existingPlacements = existingBoard.getPlacements();
                int importOption = 1;
                if (!existingPlacements.isEmpty()) {
                    //Option 0: Merge imported placements with existing placements
                    //Option 1: Import after deleting all existing placements
                    //Option 2: Cancel the import
                    Object[] options = {"Merge new with existing",
                            "Replace existing with new",
                            "Cancel"};
                    importOption = JOptionPane.showOptionDialog((Frame) getTopLevelAncestor(),
                            "The Selected Board Already Has Existing Placements",
                            "What do you want to do?",
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            options,
                            options[2]);
                    if (importOption == 2) {
                        return;
                    }
                }
                if (importOption == 1) {
                    existingPlacements.clear();
                }
                for (Placement placement : importedBoard.getPlacements()) {
                    if (importOption == 0 && (existingPlacements.get(placement.getId()) != null)) {
                        Placement existingPlacement = existingPlacements.get(placement.getId());
                        existingPlacement.setPart(placement.getPart());
                        existingPlacement.setSide(placement.getSide());
                        existingPlacement.setLocation(placement.getLocation());
                    }
                    else {
                        existingBoard.addPlacement(placement);
                    }
                }
                for (BoardPad pad : importedBoard.getSolderPastePads()) {
                    // TODO: This is a temporary hack until we redesign the
                    // importer
                    // interface to be more intuitive. The Gerber importer tends
                    // to return everything in Inches, so this is a method to
                    // try to get it closer to what the user expects to see.
                    pad.setLocation(pad.getLocation()
                            .convertToUnits(boardsPanel.getSelection().getDimensions().getUnits()));
                    existingBoard.addSolderPastePad(pad);
                }

                Configuration.get().getBus()
                    .post(new DefinitionStructureChangedEvent(board, "placements", BoardPlacementsPanel.this));
            }
        }
        catch (Exception e) {
            MessageBoxes.errorBox(getTopLevelAncestor(), "Import Failed", e); //$NON-NLS-1$
        }
    }

    public final Action importAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.importt);
            putValue(NAME, "Import Placements");
            putValue(SHORT_DESCRIPTION, "Import placements from CAD files");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
        }
    };

    public final Action setTypeAction = new AbstractAction() {
        {
            putValue(NAME, "Set Type");
            putValue(SHORT_DESCRIPTION, "Set placement type(s) to...");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {}
    };

    class SetTypeAction extends AbstractAction {
        final Placement.Type type;

        public SetTypeAction(Placement.Type type) {
            this.type = type;
            putValue(NAME, type.toString());
            putValue(SHORT_DESCRIPTION, "Set placement type(s) to " + type.toString());
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            for (Placement placement : getSelections()) {
                placement.setType(type);
                tableModel.fireTableDataChanged();
            }
        }
    };

    public final Action setSideAction = new AbstractAction() {
        {
            putValue(NAME, "Set Side");
            putValue(SHORT_DESCRIPTION, "Set placement side(s) to...");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {}
    };

    class SetSideAction extends AbstractAction {
        final Side side;

        public SetSideAction(Side side) {
            this.side = side;
            putValue(NAME, side.toString());
            putValue(SHORT_DESCRIPTION, "Set placement side(s) to " + side.toString());
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            for (Placement placement : getSelections()) {
                placement.setSide(side);
                tableModel.fireTableDataChanged();
            }
        }
    };
    
    public final Action setErrorHandlingAction = new AbstractAction() {
        {
            putValue(NAME, "Set Error Handling");
            putValue(SHORT_DESCRIPTION, "Set placement error handling(s) to...");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {}
    };

    class SetErrorHandlingAction extends AbstractAction {
        Placement.ErrorHandling errorHandling;

        public SetErrorHandlingAction(Placement.ErrorHandling errorHandling) {
            this.errorHandling = errorHandling;
            putValue(NAME, errorHandling.toString());
            putValue(SHORT_DESCRIPTION, "Set placement error handling(s) to " + errorHandling.toString());
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            for (Placement placement : getSelections()) {
                placement.setErrorHandling(errorHandling);
                tableModel.fireTableDataChanged();
            }
        }
    };
    
    public final Action setEnabledAction = new AbstractAction() {
        {
            putValue(NAME, "Set Enabled");
            putValue(SHORT_DESCRIPTION, "Set placement enabled to...");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {}
    };
    private JTextField searchTextField;

    class SetEnabledAction extends AbstractAction {
        final Boolean enabled;

        public SetEnabledAction(Boolean enabled) {
            this.enabled = enabled;
            String name = enabled ? "Enabled" : "Disabled";
            putValue(NAME, name);
            putValue(SHORT_DESCRIPTION, "Set placement enabled to " + name);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            for (Placement placement : getSelections()) {
                placement.setEnabled(enabled);
                tableModel.fireTableDataChanged();   
            }
        }
    };

    static class TypeRenderer extends DefaultTableCellRenderer {
        @Override
        public void setValue(Object value) {
            if (value == null) {
                return;
            }
            Type type = (Type) value;
            setText(type.name());
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            Color alternateRowColor = UIManager.getColor("Table.alternateRowColor");
            if (value == Type.Fiducial) {
                c.setForeground(Color.black);
                c.setBackground(typeColorFiducial);
            } else if (isSelected) {
                c.setForeground(table.getSelectionForeground());
                c.setBackground(table.getSelectionBackground());
            } else {
                c.setForeground(table.getForeground());
                c.setBackground(row%2==0 ? table.getBackground() : alternateRowColor);
            }

            return c;
        }
    }

}
