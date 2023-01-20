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
import java.awt.Component;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
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
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;

import org.openpnp.Translations;
import org.openpnp.events.DefinitionStructureChangedEvent;
import org.openpnp.events.PlacementSelectedEvent;
import org.openpnp.gui.components.AutoSelectTextTable;
import org.openpnp.gui.importer.BoardImporter;
import org.openpnp.gui.importer.SolderPasteGerberImporter;
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
import org.openpnp.gui.tablemodel.PlacementsHolderPlacementsTableModel;
import org.openpnp.gui.tablemodel.PlacementsHolderPlacementsTableModel.Status;
import org.openpnp.gui.viewers.PlacementsHolderLocationViewerDialog;
import org.openpnp.model.Abstract2DLocatable.Side;
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
    private Preferences prefs = Preferences.userNodeForPackage(BoardPlacementsPanel.class);
    protected PlacementsHolderLocationViewerDialog boardViewer;
    private JButton btnImport;
    private List<BoardImporter> boardImporters;
    private Configuration configuration;
    
    private static Color typeColorFiducial = new Color(157, 188, 255);
    private static Color typeColorPlacement = new Color(255, 255, 255);
    
    public BoardPlacementsPanel(BoardsPanel boardsPanel) {
    	this.boardsPanel = boardsPanel;
    	boardImporters = scanForBoardImporters();
        createUi();
    }
    
    /**
     * Scans the importer's package for BoardImporters
     * @return the list of BoardImporters that were found
     */
    @SuppressWarnings("unchecked")
    private List<BoardImporter> scanForBoardImporters() {
        List<BoardImporter> boardImporters = new ArrayList<>();
        try (ScanResult scanResult = new ClassGraph().enableClassInfo()
                .acceptPackages(BoardImporter.class.getPackage().getName()).scan()) {
            ClassInfoList importerClassInfoList = scanResult.
                    getClassesImplementing(BoardImporter.class.getCanonicalName());
            for (ClassInfo boardImporterInfo : importerClassInfoList) {
                BoardImporter boardImporter;
                try {
                    boardImporter = ((Class<? extends BoardImporter>) boardImporterInfo.loadClass())
                            .getDeclaredConstructor().newInstance();
                }
                catch (Exception e) {
                    throw new Error(e);
                }
                
                //For now, skip the solder paste importer
                Logger.trace(boardImporter.getClass().getSimpleName());
                if (boardImporter.getClass() == SolderPasteGerberImporter.class) {
                    continue;
                }
                
                boardImporters.add(boardImporter);
            }
        }
        return boardImporters;
    }
    
    private void createUi() {
        setBorder(new TitledBorder(null, 
                Translations.getString("BoardPanel.BoardPlacements.Placements"), //$NON-NLS-1$
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        
        configuration = Configuration.get();
        
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
        tableModel = new PlacementsHolderPlacementsTableModel(this);
        tableSorter = new TableRowSorter<>(tableModel);
        
        table = new AutoSelectTextTable(tableModel);
        
        TableColumnModel tcm = table.getColumnModel();
        tcm.removeColumn(tcm.getColumn(9)); //remove Status column
        tcm.removeColumn(tcm.getColumn(8)); //remove Placed column
        
        table.setRowSorter(tableSorter);
        table.getTableHeader().setDefaultRenderer(new MultisortTableHeaderCellRenderer());
        
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setDefaultEditor(Side.class, new DefaultCellEditor(sidesComboBox));
        table.setDefaultEditor(Part.class, new DefaultCellEditor(partsComboBox));
        table.setDefaultEditor(Type.class, new DefaultCellEditor(typesComboBox));
        table.setDefaultRenderer(Type.class, new TypeRenderer());
        table.setDefaultEditor(ErrorHandling.class, new DefaultCellEditor(errorHandlingComboBox));
        table.setDefaultRenderer(Part.class, new IdentifiableTableCellRenderer<Part>());
        table.setDefaultRenderer(Boolean.class, new CustomBooleanRenderer());
        table.setDefaultRenderer(LengthCellValue.class, new MonospacedFontTableCellRenderer());
        table.setDefaultRenderer(RotationCellValue.class, new MonospacedFontTableCellRenderer());
        table.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
        
        TableUtils.setColumnAlignment(tableModel, table);
        
        TableUtils.installColumnWidthSavers(table, prefs, "BoardPlacementsPanel.placementsTable.columnWidth"); //$NON-NLS-1$
        
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }

                boolean updateLinkedTables = MainFrame.get().getTabs().getSelectedComponent() == MainFrame.get().getBoardsTab() 
                        && configuration.getTablesLinked() == TablesLinked.Linked;
                
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
                        configuration.getBus().post(new PlacementSelectedEvent(getSelection(),
                                new BoardLocation(board), BoardPlacementsPanel.this));
                    }
                    if (getSelection() != null
                            && (selectedComponent == mainFrame.getJobTab() ||
                                    selectedComponent == mainFrame.getBoardsTab())
                            && configuration.getTablesLinked() == TablesLinked.Linked) {
                        Part selectedPart = getSelection().getPart();
                        mainFrame.getPartsTab().selectPartInTable(selectedPart);
                        if (selectedPart != null) {
                            mainFrame.getPackagesTab().selectPackageInTable(selectedPart.getPackage());
                        }
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
        newAction.setEnabled(false);
        toolBarPlacements.add(btnNewPlacement);
        
        JButton btnRemovePlacement = new JButton(removeAction);
        btnRemovePlacement.setHideActionText(true);
        toolBarPlacements.add(btnRemovePlacement);

        toolBarPlacements.addSeparator();
        btnImport = new JButton(importAction);
        btnImport.setHideActionText(true);
        importAction.setEnabled(false);
        toolBarPlacements.add(btnImport);

        toolBarPlacements.addSeparator();
        
        JButton btnViewer = new JButton(viewerAction);
        btnViewer.setHideActionText(true);
        viewerAction.setEnabled(false);
        toolBarPlacements.add(btnViewer);
        
        JPanel panel_1 = new JPanel();
        panel.add(panel_1, BorderLayout.EAST);

        JLabel lblNewLabel = new JLabel(Translations.getString("BoardPanel.BoardPlacements.Placements.Search")); //$NON-NLS-1$
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

        configuration.getBus().register(this);
    }
    
    @Subscribe
    public void placementSelectedEventHandler(PlacementSelectedEvent event) {
        if (event.source == this || event.placementsHolderLocation == null || !(event.placementsHolderLocation.getPlacementsHolder() instanceof Board)) {
            return;
        }
        Placement placement = event.placement == null ? null : (Placement) event.placement.getDefinition();
        SwingUtilities.invokeLater(() -> {
            selectPlacement(placement);
        });
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
            return;
        }
        int index = tableModel.indexOf(placement);
        if (index >= 0) {
            index = table.convertRowIndexToView(index);
            table.getSelectionModel().setSelectionInterval(index, index);
            table.scrollRectToVisible(new Rectangle(table.getCellRect(index, 0, true)));
        }
    }
    
    private void updateRowFilter() {
        List<RowFilter<PlacementsHolderPlacementsTableModel, Integer>> filters = new ArrayList<>();
        
        try {
            RowFilter<PlacementsHolderPlacementsTableModel, Integer> searchFilter = RowFilter.regexFilter("(?i)" + searchTextField.getText().trim()); //$NON-NLS-1$
            filters.add(searchFilter);
        }
        catch (PatternSyntaxException e) {
        }
        
        tableSorter.setRowFilter(RowFilter.andFilter(filters));
    }
    
    
    public void setBoard(Board board) {
        this.board = board;
        tableModel.setPlacementsHolder(board);
        newAction.setEnabled(board != null);
        importAction.setEnabled(board != null);
        viewerAction.setEnabled(board != null);
        if (boardViewer != null) {
            boardViewer.setPlacementsHolder(board);
        }
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

    public List<BoardImporter> getBoardImporters() {
        return boardImporters;
    }

    public final Action newAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.add);
            putValue(NAME, Translations.getString("BoardPanel.BoardPlacements.Action.NewPlacement")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("BoardPanel.BoardPlacements.Action.NewPlacement.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (configuration.getParts().size() == 0) {
                MessageBoxes.errorBox(getTopLevelAncestor(), Translations.getString("General.Error"), //$NON-NLS-1$
                        Translations.getString("BoardPanel.BoardPlacements.NewPlacement.ErrorMessageBox.NoPartsMessage")); //$NON-NLS-1$
                return;
            }

            String id = JOptionPane.showInputDialog(getTopLevelAncestor(),
                    Translations.getString("BoardPanel.BoardPlacements.NewPlacement.InputDialog.enterIdMessage")); //$NON-NLS-1$
            if (id == null) {
                return;
            }
            
            // Check if the new placement ID is unique
            for(Placement compareplacement : board.getPlacements()) {
                if (compareplacement.getId().equals(id)) {
                    MessageBoxes.errorBox(getTopLevelAncestor(), Translations.getString("General.Error"), //$NON-NLS-1$
                            Translations.getString("BoardPanel.BoardPlacements.NewPlacement.ErrorMessageBox.IdAlreadyExistsMessage")); //$NON-NLS-1$
                    return;
                }
            }
            
            Placement placement = new Placement(id);

            placement.setPart(configuration.getParts().get(0));
            placement.setLocation(new Location(configuration.getSystemUnits()));
            placement.setSide(Side.Top);

            board.addPlacement(placement);
            tableModel.fireTableDataChanged();
            Helpers.selectLastTableRow(table);

            configuration.getBus()
                .post(new DefinitionStructureChangedEvent(board, "placements", BoardPlacementsPanel.this)); //$NON-NLS-1$
        }
    };

    public final Action removeAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.delete);
            putValue(NAME, Translations.getString("BoardPanel.BoardPlacements.Action.RemovePlacement")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("BoardPanel.BoardPlacements.Action.RemovePlacement.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            for (Placement placement : getSelections()) {
                board.removePlacement(placement);
            }
            tableModel.fireTableDataChanged();

            configuration.getBus()
                .post(new DefinitionStructureChangedEvent(board, "placements", BoardPlacementsPanel.this)); //$NON-NLS-1$
        }
    };

    public void importBoard(Class<? extends BoardImporter> boardImporterClass) {
        if (boardsPanel.getSelection() == null) {
            MessageBoxes.errorBox(getTopLevelAncestor(), Translations.getString("BoardPanel.BoardPlacements.Importer.Fail"), //$NON-NLS-1$
                    Translations.getString("BoardPanel.BoardPlacements.Importer.Fail.Message")); //$NON-NLS-1$
            return;
        }
        
        BoardImporter boardImporter;
        try {
            boardImporter = boardImporterClass.newInstance();
        }
        catch (Exception e) {
            MessageBoxes.errorBox(getTopLevelAncestor(), Translations.getString("BoardPanel.BoardPlacements.Importer.Fail"), e); //$NON-NLS-1$
            return;
        }

        try {
            Board importedBoard = boardImporter.importBoard((Frame) getTopLevelAncestor());
            if (importedBoard != null) {
                IdentifiableList<Placement> existingPlacements = board.getPlacements();
                int importOption = 1;
                if (!existingPlacements.isEmpty()) {
                    //Option 0: Merge imported placements with existing placements - existing 
                    //          placements with Ids matching those in the imported set are updated,
                    //          existing placements with Ids that don't match any in the imported 
                    //          set are left unchanged, and placements in the imported set that 
                    //          don't match any in the existing set are added 
                    //Option 1: Import after deleting all existing placements
                    //Option 2: Cancel the import
                    Object[] options = {
                            Translations.getString("BoardPanel.BoardPlacements.Importer.OptionsBox.Merge"), //$NON-NLS-1$
                            Translations.getString("BoardPanel.BoardPlacements.Importer.OptionsBox.Replace"), //$NON-NLS-1$
                            Translations.getString("General.Cancel")}; //$NON-NLS-1$
                    importOption = JOptionPane.showOptionDialog((Frame) getTopLevelAncestor(),
                            Translations.getString("BoardPanel.BoardPlacements.Importer.OptionsBox.Question"), //$NON-NLS-1$
                            Translations.getString("BoardPanel.BoardPlacements.Importer.OptionsBox.Title"), //$NON-NLS-1$
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            options,
                            options[2]);
                    if (importOption == 2 || importOption == JOptionPane.CLOSED_OPTION) {
                        return;
                    }
                }
                if (importOption == 1) {
                    board.removeAllPlacements();
                }
                for (Placement placement : importedBoard.getPlacements()) {
                    if (importOption == 0 && (existingPlacements.get(placement.getId()) != null)) {
                        Placement existingPlacement = existingPlacements.get(placement.getId());
                        existingPlacement.setPart(placement.getPart());
                        existingPlacement.setSide(placement.getSide());
                        existingPlacement.setLocation(placement.getLocation());
                        existingPlacement.setComments(placement.getComments());
                    }
                    else {
                        Placement newPlacement = new Placement(placement);
                        newPlacement.setDefinition(newPlacement);
                        board.addPlacement(newPlacement);
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
                    board.addSolderPastePad(pad);
                }
                
                importedBoard.dispose();
                
                configuration.getBus()
                    .post(new DefinitionStructureChangedEvent(board, "placements", BoardPlacementsPanel.this)); //$NON-NLS-1$
            }
        }
        catch (Exception e) {
            MessageBoxes.errorBox(getTopLevelAncestor(), Translations.getString("BoardPanel.BoardPlacements.Importer.Fail"), e); //$NON-NLS-1$
        }
    }

    public final Action importAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.importt);
            putValue(NAME, Translations.getString("BoardPanel.BoardPlacements.Action.Import")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("BoardPanel.BoardPlacements.Action.Import.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            JPopupMenu menu = new JPopupMenu();
            for (BoardImporter bi : boardImporters) {
                final BoardImporter boardImporter = bi;
                menu.add(new JMenuItem(new AbstractAction() {
                    {
                        putValue(NAME, boardImporter.getImporterName());
                        putValue(SHORT_DESCRIPTION, boardImporter.getImporterDescription());
                        putValue(MNEMONIC_KEY, KeyEvent.VK_I);
                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        importBoard(boardImporter.getClass());
                        refresh();
                    }
                }));
            }
            menu.show(btnImport, (int) btnImport.getWidth(), (int) btnImport.getHeight());
        }
    };

    public final Action viewerAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.colorTrue);
            putValue(NAME, Translations.getString("BoardPanel.BoardPlacements.Action.View")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("BoardPanel.BoardPlacements.Action.View.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (boardViewer == null) {
                boardViewer = new PlacementsHolderLocationViewerDialog(new BoardLocation(board), false);
                boardViewer.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        boardViewer = null;
                    }
                });
            }
            else {
                boardViewer.setExtendedState(Frame.NORMAL);
            }
            boardViewer.setVisible(true);
        }
    };

    public final Action setTypeAction = new AbstractAction() {
        {
            putValue(NAME, Translations.getString("BoardPanel.BoardPlacements.Action.SetType")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("BoardPanel.BoardPlacements.Action.SetType.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {}
    };

    class SetTypeAction extends AbstractAction {
        final Placement.Type type;

        public SetTypeAction(Placement.Type type) {
            this.type = type;
            String name;
            if (type == Placement.Type.Fiducial) {
                name = Translations.getString("Placement.Type.Fiducial"); //$NON-NLS-1$
            }
            else if (type == Placement.Type.Placement) {
                name = Translations.getString("Placement.Type.Placement"); //$NON-NLS-1$
            }
            else {
                name = type.toString();
            }
            putValue(NAME, name);
            putValue(SHORT_DESCRIPTION, Translations.getString("BoardPanel.BoardPlacements.Action.SetType.ToolTip") + //$NON-NLS-1$
                    " " + name); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            for (Placement placement : getSelections()) {
                placement.setType(type);
                tableModel.fireTableCellUpdated(placement, 
                        Translations.getString("PlacementsHolderPlacementsTableModel.ColumnName.Type")); //$NON-NLS-1$
            }
        }
    };

    public final Action setSideAction = new AbstractAction() {
        {
            putValue(NAME, Translations.getString("BoardPanel.BoardPlacements.Action.SetSide")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("BoardPanel.BoardPlacements.Action.SetSide.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {}
    };

    class SetSideAction extends AbstractAction {
        final Side side;

        public SetSideAction(Side side) {
            this.side = side;
            String name;
            if (side == Side.Top) {
                name = Translations.getString("Placement.Side.Top"); //$NON-NLS-1$
            }
            else {
                name = Translations.getString("Placement.Side.Bottom"); //$NON-NLS-1$
            }
            putValue(NAME, name);
            putValue(SHORT_DESCRIPTION, Translations.getString("BoardPanel.BoardPlacements.Action.SetSide.ToolTip") + //$NON-NLS-1$
                    " " + name); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            for (Placement placement : getSelections()) {
                placement.setSide(side);
                tableModel.fireTableCellUpdated(placement, 
                        Translations.getString("PlacementsHolderPlacementsTableModel.ColumnName.Side")); //$NON-NLS-1$
            }
        }
    };
    
    public final Action setErrorHandlingAction = new AbstractAction() {
        {
            putValue(NAME, Translations.getString("BoardPanel.BoardPlacements.Action.SetErrorHandling")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("BoardPanel.BoardPlacements.Action.SetErrorHandling.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {}
    };

    class SetErrorHandlingAction extends AbstractAction {
        Placement.ErrorHandling errorHandling;

        public SetErrorHandlingAction(Placement.ErrorHandling errorHandling) {
            this.errorHandling = errorHandling;
            String name;
            if (errorHandling == Placement.ErrorHandling.Alert) {
                name = Translations.getString("Placement.ErrorHandling.Alert"); //$NON-NLS-1$
            }
            else {
                name = Translations.getString("Placement.ErrorHandling.Defer"); //$NON-NLS-1$
            }
            putValue(NAME, name);
            putValue(SHORT_DESCRIPTION, Translations.getString("BoardPanel.BoardPlacements.Action.SetErrorHandling.ToolTip") + //$NON-NLS-1$
                    " " + name); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            for (Placement placement : getSelections()) {
                placement.setErrorHandling(errorHandling);
                tableModel.fireTableCellUpdated(placement, 
                        Translations.getString("PlacementsHolderPlacementsTableModel.ColumnName.ErrorHandling")); //$NON-NLS-1$
            }
        }
    };
    
    public final Action setEnabledAction = new AbstractAction() {
        {
            putValue(NAME, Translations.getString("BoardPanel.BoardPlacements.Action.SetEnabled")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("BoardPanel.BoardPlacements.Action.SetEnabled.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {}
    };
    private JTextField searchTextField;

    class SetEnabledAction extends AbstractAction {
        final Boolean enabled;

        public SetEnabledAction(Boolean enabled) {
            this.enabled = enabled;
            String name = enabled ? 
                    Translations.getString("General.Enabled") :  //$NON-NLS-1$
                    Translations.getString("General.Disabled"); //$NON-NLS-1$
            putValue(NAME, name);
            putValue(SHORT_DESCRIPTION, Translations.getString("BoardPanel.BoardPlacements.Action.SetEnabled.ToolTip") +  //$NON-NLS-1$
                    " " + name); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            for (Placement placement : getSelections()) {
                placement.setEnabled(enabled);
                tableModel.fireTableCellUpdated(placement, 
                        Translations.getString("PlacementsHolderPlacementsTableModel.ColumnName.Enabled")); //$NON-NLS-1$
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
            String name;
            if (type == Placement.Type.Fiducial) {
                name = Translations.getString("Placement.Type.Fiducial"); //$NON-NLS-1$
            }
            else if (type == Placement.Type.Placement) {
                name = Translations.getString("Placement.Type.Placement"); //$NON-NLS-1$
            }
            else {
                name = value.toString();
            }
            setText(name); //$NON-NLS-1$
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            Color alternateRowColor = UIManager.getColor("Table.alternateRowColor"); //$NON-NLS-1$
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
