/*
 	Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 	
 	This file is part of OpenPnP.
 	
	OpenPnP is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenPnP is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenPnP.  If not, see <http://www.gnu.org/licenses/>.
 	
 	For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.gui;

import java.awt.BorderLayout;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FilenameFilter;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openpnp.gui.components.AutoSelectTextTable;
import org.openpnp.gui.support.ActionGroup;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.IdentifiableListCellRenderer;
import org.openpnp.gui.support.IdentifiableTableCellRenderer;
import org.openpnp.gui.support.PartsComboBoxModel;
import org.openpnp.gui.tablemodel.BoardsTableModel;
import org.openpnp.gui.tablemodel.PlacementsTableModel;
import org.openpnp.model.Board;
import org.openpnp.model.Board.Side;
import org.openpnp.model.Configuration;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import javax.swing.border.TitledBorder;

@SuppressWarnings("serial")
public class BoardsPanel extends JPanel {
	final private Configuration configuration;

	private static final String PREF_DIVIDER_POSITION = "BoardsPanel.dividerPosition";
	private static final int PREF_DIVIDER_POSITION_DEF = -1;

	private BoardsTableModel boardsTableModel;
	private PlacementsTableModel placementsTableModel;
	private JTable boardsTable;
	private JTable placementsTable;

	private ActionGroup boardSelectionActionGroup;
	private ActionGroup placementSelectionActionGroup;

	private Preferences prefs = Preferences
			.userNodeForPackage(BoardsPanel.class);

	public BoardsPanel(Configuration configuration) {
		this.configuration = configuration;

		boardSelectionActionGroup = new ActionGroup(newPlacementAction);
		boardSelectionActionGroup.setEnabled(false);

		placementSelectionActionGroup = new ActionGroup(removePlacementAction);
		placementSelectionActionGroup.setEnabled(false);

		boardsTableModel = new BoardsTableModel(configuration);
		placementsTableModel = new PlacementsTableModel(configuration);

		JComboBox sidesComboBox = new JComboBox(Side.values());
		setLayout(new BorderLayout(0, 0));

		JPanel panelBoards = new JPanel();
		panelBoards.setBorder(new TitledBorder(null, "Boards",
				TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panelBoards.setLayout(new BorderLayout(0, 0));

		boardsTable = new AutoSelectTextTable(boardsTableModel);
		boardsTable.setAutoCreateRowSorter(true);
		boardsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// TODO: Add a tooltip for the path, see
		// http://docs.oracle.com/javase/tutorial/uiswing/components/table.html#celltooltip

		boardsTable.getSelectionModel().addListSelectionListener(
				new ListSelectionListener() {
					@Override
					public void valueChanged(ListSelectionEvent e) {
						if (e.getValueIsAdjusting()) {
							return;
						}
						Board board = getSelectedBoard();
						boardSelectionActionGroup.setEnabled(board != null);
						if (board == null) {
							placementsTableModel.setBoard(null);
						}
						else {
							placementsTableModel.setBoard(board);
						}
					}
				});

		JToolBar toolBarBoards = new JToolBar();
		panelBoards.add(toolBarBoards, BorderLayout.NORTH);
		toolBarBoards.setFloatable(false);

		JButton btnNewBoard = new JButton(newBoardAction);
		btnNewBoard.setHideActionText(true);
		toolBarBoards.add(btnNewBoard);
		JButton btnLoadBoard = new JButton(addBoardAction);
		btnLoadBoard.setHideActionText(true);
		toolBarBoards.add(btnLoadBoard);
		JScrollPane scrollPaneBoards = new JScrollPane(boardsTable);
		panelBoards.add(scrollPaneBoards, BorderLayout.CENTER);

		JPanel panelPlacements = new JPanel();
		panelPlacements.setBorder(new TitledBorder(null, "Placements",
				TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panelPlacements.setLayout(new BorderLayout(0, 0));

		JComboBox partsComboBox = new JComboBox(new PartsComboBoxModel());
		partsComboBox.setRenderer(new IdentifiableListCellRenderer<Part>());

		placementsTable = new AutoSelectTextTable(placementsTableModel);
		placementsTable.setAutoCreateRowSorter(true);
		placementsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		placementsTable.setDefaultEditor(Side.class, new DefaultCellEditor(
				sidesComboBox));
		placementsTable.setDefaultEditor(Part.class, new DefaultCellEditor(
				partsComboBox));
		placementsTable.setDefaultRenderer(Part.class,
				new IdentifiableTableCellRenderer<Part>());

		placementsTable.getSelectionModel().addListSelectionListener(
				new ListSelectionListener() {
					@Override
					public void valueChanged(ListSelectionEvent e) {
						if (e.getValueIsAdjusting()) {
							return;
						}
						placementSelectionActionGroup
								.setEnabled(getSelectedPlacement() != null);
					}
				});
		JScrollPane scrollPanePlacements = new JScrollPane(placementsTable);
		panelPlacements.add(scrollPanePlacements, BorderLayout.CENTER);

		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		panelPlacements.add(toolBar, BorderLayout.NORTH);
		JButton btnNewPlacement = new JButton(newPlacementAction);
		toolBar.add(btnNewPlacement);
		btnNewPlacement.setHideActionText(true);
		JButton btnRemovePlacement = new JButton(removePlacementAction);
		toolBar.add(btnRemovePlacement);
		btnRemovePlacement.setHideActionText(true);

		final JSplitPane splitPane = new JSplitPane();
		splitPane.setBorder(null);
		splitPane.setContinuousLayout(true);
		splitPane.setDividerLocation(prefs.getInt(PREF_DIVIDER_POSITION,
				PREF_DIVIDER_POSITION_DEF));
		splitPane.addPropertyChangeListener("dividerLocation",
				new PropertyChangeListener() {
					@Override
					public void propertyChange(PropertyChangeEvent evt) {
						prefs.putInt(PREF_DIVIDER_POSITION,
								splitPane.getDividerLocation());
					}
				});
		splitPane.setLeftComponent(panelBoards);
		splitPane.setRightComponent(panelPlacements);
		add(splitPane);
	}

	public boolean checkForModifications() {
		for (Board board : configuration.getBoards()) {
			if (board.isDirty()) {
				int result = JOptionPane
						.showConfirmDialog(
								getTopLevelAncestor(),
								"Do you want to save your changes to "
										+ board.getFile().getName()
										+ "?"
										+ "\n"
										+ "If you don't save, your changes will be lost.",
								"Save " + board.getFile().getName() + "?",
								JOptionPane.YES_NO_CANCEL_OPTION);
				if (result == JOptionPane.YES_OPTION) {
					try {
						configuration.saveBoard(board);
					}
					catch (Exception e) {
						MessageBoxes.errorBox(getTopLevelAncestor(),
								"Board Save Error", e.getMessage());
						return false;
					}
				}
				else if (result == JOptionPane.CANCEL_OPTION) {
					return false;
				}
			}
		}
		return true;
	}

	public Board getSelectedBoard() {
		int index = boardsTable.getSelectedRow();
		if (index == -1) {
			return null;
		}
		else {
			index = boardsTable.convertRowIndexToModel(index);
			return configuration.getBoards().get(index);
		}
	}

	public Placement getSelectedPlacement() {
		if (getSelectedBoard() == null) {
			return null;
		}
		int index = placementsTable.getSelectedRow();
		if (index == -1) {
			return null;
		}
		else {
			index = placementsTable.convertRowIndexToModel(index);
			return getSelectedBoard().getPlacements().get(index);
		}
	}

	public Action newBoardAction = new AbstractAction() {
		{
			putValue(SMALL_ICON,
					new ImageIcon(getClass().getResource("/icons/new.png")));
			putValue(NAME, "New Board...");
			putValue(SHORT_DESCRIPTION,
					"Create a new board and add it to the list.");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			FileDialog fileDialog = new FileDialog(
					(Frame) getTopLevelAncestor(), "Save New Board As...",
					FileDialog.SAVE);
			fileDialog.setFilenameFilter(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.toLowerCase().endsWith(".board.xml");
				}
			});
			fileDialog.setVisible(true);
			try {
				String filename = fileDialog.getFile();
				if (filename == null) {
					return;
				}
				if (!filename.toLowerCase().endsWith(".board.xml")) {
					filename = filename + ".board.xml";
				}
				File file = new File(new File(fileDialog.getDirectory()),
						filename);

				// Calling getBoard() loads the board if it's not loaded and
				// fires the PCL that will refresh the table.
				configuration.getBoard(file);

				Helpers.selectLastTableRow(boardsTable);
			}
			catch (Exception e) {
				e.printStackTrace();
				MessageBoxes.errorBox(getTopLevelAncestor(),
						"Unable to create new board", e.getMessage());
			}
		}
	};

	public Action addBoardAction = new AbstractAction() {
		{
			putValue(SMALL_ICON,
					new ImageIcon(getClass().getResource("/icons/add.png")));
			putValue(NAME, "Load Board...");
			putValue(SHORT_DESCRIPTION, "Load an existing board.");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			FileDialog fileDialog = new FileDialog(
					(Frame) getTopLevelAncestor());
			fileDialog.setFilenameFilter(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.toLowerCase().endsWith(".board.xml");
				}
			});
			fileDialog.setVisible(true);
			try {
				if (fileDialog.getFile() == null) {
					return;
				}
				File file = new File(new File(fileDialog.getDirectory()),
						fileDialog.getFile());

				configuration.getBoard(file);

				Helpers.selectLastTableRow(boardsTable);
			}
			catch (Exception e) {
				e.printStackTrace();
				MessageBoxes.errorBox(getTopLevelAncestor(),
						"Board load failed", e.getMessage());
			}
		}
	};

	public Action newPlacementAction = new AbstractAction() {
		{
			putValue(SMALL_ICON,
					new ImageIcon(getClass().getResource("/icons/new.png")));
			putValue(NAME, "New Placement");
			putValue(SHORT_DESCRIPTION,
					"Create a new placement and add it to the board.");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (Configuration.get().getParts().size() == 0) {
				MessageBoxes
						.errorBox(
								getTopLevelAncestor(),
								"Error",
								"There are currently no parts defined in the system. Please create at least one part before creating a placement.");
				return;
			}

			Board board = getSelectedBoard();
			// TODO: Make sure it's unique
			String id = JOptionPane.showInputDialog(getTopLevelAncestor(),
					"Please enter an ID for the new placement.");
			if (id == null) {
				return;
			}
			
			
			// TODO: Make sure it's unique.
			Placement placement = new Placement(id);

			placement.setPart(Configuration.get().getParts().get(0));
			placement.getLocation().setUnits(configuration.getSystemUnits());
			
			board.addPlacement(placement);
			placementsTableModel.fireTableDataChanged();
			Helpers.selectLastTableRow(placementsTable);
		}
	};

	public Action removePlacementAction = new AbstractAction("Remove Placement") {
		{
			putValue(SMALL_ICON,
					new ImageIcon(getClass().getResource("/icons/delete.png")));
			putValue(NAME, "Remove Placement");
			putValue(SHORT_DESCRIPTION,
					"Remove the currently selected placement.");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			Board board = getSelectedBoard();
			Placement placement = getSelectedPlacement();
			board.removePlacement(placement);
			placementsTableModel.fireTableDataChanged();
		}
	};
}
