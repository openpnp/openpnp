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
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableRowSorter;

import org.openpnp.Translations;
import org.openpnp.events.PlacementsHolderLocationSelectedEvent;
import org.openpnp.events.PlacementsHolderSelectedEvent;
import org.openpnp.events.PlacementSelectedEvent;
import org.openpnp.gui.components.AutoSelectTextTable;
import org.openpnp.gui.support.ActionGroup;
import org.openpnp.gui.support.MonospacedFontTableCellRenderer;
import org.openpnp.gui.support.TableUtils;
import org.openpnp.gui.support.CustomAlignmentRenderer;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.LengthCellValue;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.tablemodel.PlacementsHolderTableModel;
import org.openpnp.model.Board;
import org.openpnp.model.Configuration;
import org.openpnp.model.Placement;
import org.openpnp.model.Configuration.TablesLinked;
import org.pmw.tinylog.Logger;

import com.google.common.eventbus.Subscribe;

@SuppressWarnings("serial")
public class BoardsPanel extends JPanel {
    final private Configuration configuration;
    final private MainFrame frame;

    private static final String PREF_DIVIDER_POSITION = "BoardsPanel.dividerPosition"; //$NON-NLS-1$
    private static final int PREF_DIVIDER_POSITION_DEF = -1;

    private PlacementsHolderTableModel boardsTableModel;
    private JTable boardsTable;
    private JSplitPane splitPane;

    private ActionGroup singleSelectionActionGroup;
    private ActionGroup multiSelectionActionGroup;

    private Preferences prefs = Preferences.userNodeForPackage(BoardsPanel.class);

    private final BoardPlacementsPanel boardPlacementsPanel;

    private JPanel pnlPlacements;
    
    public BoardsPanel(Configuration configuration, MainFrame frame) {
        this.configuration = configuration;
        this.frame = frame;
        
        singleSelectionActionGroup = new ActionGroup(removeBoardAction, copyBoardAction);
        singleSelectionActionGroup.setEnabled(false);
        
        multiSelectionActionGroup = new ActionGroup(removeBoardAction);
        multiSelectionActionGroup.setEnabled(false);
        
        boardsTableModel = new PlacementsHolderTableModel(configuration, 
                () -> configuration.getBoards(), Board.class);
        configuration.addPropertyChangeListener("boards", new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
//                Logger.trace("PropertyChangeEvent = " + evt);
                boardsTableModel.fireTableDataChanged();
            }});
        
        boardsTable = new AutoSelectTextTable(boardsTableModel) {
            @Override
            public String getToolTipText(MouseEvent e) {

                java.awt.Point p = e.getPoint();
                int row = rowAtPoint(p);
                int col = columnAtPoint(p);

                if (row >= 0) {
                    if (col == 0) {
                        row = boardsTable.convertRowIndexToModel(row);
                        return configuration.getBoards().get(row).getFile().toString();
                    }
                }

                return super.getToolTipText();
            }
        };

        TableRowSorter<PlacementsHolderTableModel> boardsTableSorter = new TableRowSorter<>(boardsTableModel);
        boardsTable.setRowSorter(boardsTableSorter);
        boardsTable.getTableHeader().setDefaultRenderer(new MultisortTableHeaderCellRenderer());
        boardsTable.setDefaultRenderer(LengthCellValue.class, new MonospacedFontTableCellRenderer());
        boardsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        boardsTable.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
        
        TableUtils.setColumnAlignment(boardsTableModel, boardsTable);
        
        TableUtils.installColumnWidthSavers(boardsTable, prefs, "BoardsPanel.boardsTable.columnWidth");
        
        boardsTable.getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                SwingUtilities.invokeLater(() -> {
                    getBoardPlacementsPanel().refresh();
                });
            }
        });

        boardsTable.getSelectionModel()
                .addListSelectionListener(new ListSelectionListener() {
                    @Override
                    public void valueChanged(ListSelectionEvent e) {
                        if (e.getValueIsAdjusting()) {
                            return;
                        }
                        
                        boolean updateLinkedTables = MainFrame.get().getTabs().getSelectedComponent() == MainFrame.get().getBoardsTab() 
                                && Configuration.get().getTablesLinked() == TablesLinked.Linked;

                        List<Board> selections = getSelections();
                        if (selections.size() == 0) {
                            singleSelectionActionGroup.setEnabled(false);
                            multiSelectionActionGroup.setEnabled(false);
                            getBoardPlacementsPanel().setBoard(null);
                            if (updateLinkedTables) {
                                Configuration.get().getBus()
                                    .post(new PlacementsHolderSelectedEvent(null, BoardsPanel.this));
                            }
                        }
                        else if (selections.size() == 1) {
                            multiSelectionActionGroup.setEnabled(false);
                            singleSelectionActionGroup.setEnabled(true);
                            getBoardPlacementsPanel().setBoard(selections.get(0));
                            if (updateLinkedTables) {
                                Configuration.get().getBus()
                                    .post(new PlacementsHolderSelectedEvent(selections.get(0), BoardsPanel.this));
                            }
                        }
                        else {
                            singleSelectionActionGroup.setEnabled(false);
                            multiSelectionActionGroup.setEnabled(true);
                            getBoardPlacementsPanel().setBoard(null);
                            if (updateLinkedTables) {
                                Configuration.get().getBus()
                                    .post(new PlacementsHolderSelectedEvent(null, BoardsPanel.this));
                            }
                        }
                        MainFrame.get().updateMenuState(BoardsPanel.this);
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

        JPanel pnlBoards = new JPanel();
        pnlBoards.setBorder(new TitledBorder(null,
                Translations.getString("BoardsPanel.Tab.Boards"),
                TitledBorder.LEADING, TitledBorder.TOP, null)); //$NON-NLS-1$
        pnlBoards.setLayout(new BorderLayout(0, 0));

        JToolBar toolBarBoards = new JToolBar();
        toolBarBoards.setFloatable(false);
        pnlBoards.add(toolBarBoards, BorderLayout.NORTH);

        JButton btnAddBoard = new JButton(addBoardAction);
        btnAddBoard.setHideActionText(true);
        btnAddBoard.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                JPopupMenu menu = new JPopupMenu();
                menu.add(new JMenuItem(addNewBoardAction));
                menu.add(new JMenuItem(addExistingBoardAction));
                menu.show(btnAddBoard, (int) btnAddBoard.getWidth(), (int) btnAddBoard.getHeight());
            }
        });
        toolBarBoards.add(btnAddBoard);
        
        JButton btnRemoveBoard = new JButton(removeBoardAction);
        btnRemoveBoard.setHideActionText(true);
        toolBarBoards.add(btnRemoveBoard);
        
        JButton btnCopyBoard = new JButton(copyBoardAction);
        btnCopyBoard.setHideActionText(true);
        toolBarBoards.add(btnCopyBoard);

        pnlBoards.add(new JScrollPane(boardsTable));
        splitPane.setLeftComponent(pnlBoards);
        
        pnlPlacements = new JPanel();
        pnlPlacements.setLayout(new BorderLayout(0, 0));
        splitPane.setRightComponent(pnlPlacements);

        boardPlacementsPanel = new BoardPlacementsPanel(this);
        pnlPlacements.add(getBoardPlacementsPanel());
        
        add(splitPane);

        Configuration.get().getBus().register(this);
    }
    
    public JTable getFiducialLocatableLocationsTable() {
        return boardsTable;
    }

    @Subscribe
    public void boardLocationSelected(PlacementsHolderLocationSelectedEvent event) {
        if (event.source == this || event.placementsHolderLocation == null || !(event.placementsHolderLocation.getPlacementsHolder() instanceof Board)) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            selectBoard((Board) event.placementsHolderLocation.getPlacementsHolder().getDefinition());
        });
    }

    @Subscribe
    public void placementSelected(PlacementSelectedEvent event) {
        if (event.source == this || event.source == getBoardPlacementsPanel() || event.placementsHolderLocation == null || !(event.placementsHolderLocation.getPlacementsHolder() instanceof Board)) {
            return;
        }
        Placement placement = event.placement == null ? null : (Placement) event.placement.getDefinition();
        SwingUtilities.invokeLater(() -> {
            selectBoard((Board) event.placementsHolderLocation.getPlacementsHolder().getDefinition());
            getBoardPlacementsPanel().selectPlacement(placement);
        });
    }

    private void selectBoard(Board board) {
        if (board == null) {
            boardsTable.getSelectionModel().clearSelection();
            return;
        }
        for (int i = 0; i < boardsTableModel.getRowCount(); i++) {
            if (configuration.getBoards().get(i) == board) {
                int index = boardsTable.convertRowIndexToView(i);
                boardsTable.getSelectionModel().setSelectionInterval(index, index);
                boardsTable.scrollRectToVisible(
                        new Rectangle(boardsTable.getCellRect(index, 0, true)));
                break;
            }
        }
    }

    public void refresh() {
        boardsTableModel.fireTableDataChanged();
    }

    public void refreshSelectedRow() {
        int index = boardsTable.convertRowIndexToModel(boardsTable.getSelectedRow());
        boardsTableModel.fireTableRowsUpdated(index, index);
    }

    public Board getSelection() {
        List<Board> selections = getSelections();
        if (selections.isEmpty()) {
            return null;
        }
        return selections.get(0);
    }

    public List<Board> getSelections() {
        ArrayList<Board> selections = new ArrayList<>();
        int[] selectedRows = boardsTable.getSelectedRows();
        for (int selectedRow : selectedRows) {
            selectedRow = boardsTable.convertRowIndexToModel(selectedRow);
            selections.add(configuration.getBoards().get(selectedRow));
        }
        return selections;
    }

    public final Action addBoardAction = new AbstractAction() {
        {
            putValue(NAME, Translations.getString("BoardPanel.Action.AddBoard")); //$NON-NLS-1$
            putValue(SMALL_ICON, Icons.add);
            putValue(SHORT_DESCRIPTION, Translations.getString("BoardPanel.Action.AddBoard.Description")); //$NON-NLS-1$
            putValue(MNEMONIC_KEY, KeyEvent.VK_A);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {}
    };

    public final Action addNewBoardAction = new AbstractAction() {
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

                Board board = addBoard(file);

                selectBoard(board);
            }
            catch (Exception e) {
                e.printStackTrace();
                MessageBoxes.errorBox(frame, Translations.getString("BoardPanel.Action.AddBoard.NewBoard.ErrorMessage"), e.getMessage()); //$NON-NLS-1$
            }
        }
    };

    public final Action addExistingBoardAction = new AbstractAction() {
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

                Board board = addBoard(file);

                selectBoard(board);
            }
            catch (Exception e) {
                e.printStackTrace();
                MessageBoxes.errorBox(frame, Translations.getString("BoardPanel.Action.AddBoard.ExistingBoard.ErrorMessage"), e.getMessage()); //$NON-NLS-1$
            }
        }
    };

    protected Board addBoard(File file) throws Exception {
        Board board = configuration.getBoard(file);
        // TODO: Move to a list property listener.
        boardsTableModel.fireTableDataChanged();
        return board;
    }
    
    public BoardPlacementsPanel getBoardPlacementsPanel() {
        return boardPlacementsPanel;
    }

    public final Action removeBoardAction = new AbstractAction("Remove Board") { //$NON-NLS-1$
        {
            putValue(SMALL_ICON, Icons.delete);
            putValue(NAME, Translations.getString("BoardPanel.Action.RemoveBoard")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("BoardPanel.Action.RemoveBoard.Description")); //$NON-NLS-1$
            putValue(MNEMONIC_KEY, KeyEvent.VK_R);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            for (Board selection : getSelections()) {
                if (configuration.isInUse(selection)) {
                    MessageBoxes.errorBox(BoardsPanel.this, Translations.getString("BoardPanel.Action.RemoveBoard.ErrorBox.Title"),
                            String.format(Translations.getString("BoardPanel.Action.RemoveBoard.ErrorBox.MessageFormat"), selection.getName()));
                }
                else {
                    configuration.removeBoard(selection);
                }
            }
            boardsTableModel.fireTableDataChanged();
        }
    };
    
    public final Action copyBoardAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.copy);
            putValue(NAME, Translations.getString("BoardPanel.Action.CopyBoard")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("BoardPanel.Action.CopyBoard.Description")); //$NON-NLS-1$
            putValue(MNEMONIC_KEY, KeyEvent.VK_COPY);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            Board boardToCopy = getSelection();
            FileDialog fileDialog = new FileDialog(frame, Translations.getString("BoardPanel.Action.CopyBoard.SaveDialog"), FileDialog.SAVE); //$NON-NLS-1$
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

                Board newBoard = new Board(boardToCopy);
                newBoard.setFile(file);
                newBoard.setName(file.getName());
                newBoard.setDirty(false);
                configuration.addBoard(newBoard);
                configuration.saveBoard(newBoard);
                boardsTableModel.fireTableDataChanged();
                selectBoard(newBoard);
            }
            catch (Exception e) {
                e.printStackTrace();
                MessageBoxes.errorBox(frame, Translations.getString("BoardPanel.Action.CopyBoard.ErrorMessage"), e.getMessage()); //$NON-NLS-1$
            }
        }
    };

}
