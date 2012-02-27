package org.openpnp.gui;

import java.awt.BorderLayout;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FilenameFilter;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultCellEditor;
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

import org.openpnp.gui.components.MachineControlsPanel;
import org.openpnp.gui.support.ActionGroup;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.model.Board;
import org.openpnp.model.Board.Side;
import org.openpnp.model.Configuration;
import org.openpnp.model.Placement;

public class BoardsPanel extends JPanel {
	final private Configuration configuration;
	final private Frame frame;
	final private MachineControlsPanel machineControlsPanel;
	
	private BoardsTableModel boardsTableModel;
	private PlacementsTableModel placementsTableModel;
	private JTable boardsTable;
	private JTable placementsTable;
	
	private ActionGroup boardSelectionActionGroup;
	private ActionGroup placementSelectionActionGroup;
	
	public BoardsPanel(Configuration configuration, 
			Frame frame, 
			MachineControlsPanel machineControlsPanel) {
		this.configuration = configuration;
		this.frame = frame;
		this.machineControlsPanel = machineControlsPanel;
		
		boardSelectionActionGroup = new ActionGroup(newPlacementAction);
		boardSelectionActionGroup.setEnabled(false);
		
		placementSelectionActionGroup = new ActionGroup(removePlacementAction);
		placementSelectionActionGroup.setEnabled(false);
		
		boardsTableModel = new BoardsTableModel(configuration);
		placementsTableModel = new PlacementsTableModel(configuration);

		JComboBox sidesComboBox = new JComboBox(Side.values());

		placementsTable = new JTable(placementsTableModel);
		placementsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		placementsTable.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(sidesComboBox));
		
		placementsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) {
					return;
				}
				placementSelectionActionGroup.setEnabled(getSelectedPlacement() != null);
			}
		});
		
		boardsTable = new JTable(boardsTableModel);
		boardsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		// TODO: Add a tooltip for the path, see http://docs.oracle.com/javase/tutorial/uiswing/components/table.html#celltooltip
		
		boardsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
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
		setLayout(new BorderLayout(0, 0));
		
		JPanel panel = new JPanel();
		add(panel, BorderLayout.NORTH);
		panel.setLayout(new BorderLayout(0, 0));
		
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		panel.add(toolBar);
		
		toolBar.add(new JButton(newBoardAction));
		toolBar.add(new JButton(addBoardAction));
		toolBar.addSeparator();
		toolBar.add(new JButton(newPlacementAction));
		toolBar.add(new JButton(removePlacementAction));
		
		JSplitPane splitPane = new JSplitPane();
		splitPane.setBorder(null);
		splitPane.setContinuousLayout(true);
		splitPane.setDividerLocation(350);
		splitPane.setLeftComponent(new JScrollPane(boardsTable));
		splitPane.setRightComponent(new JScrollPane(placementsTable));
		
		add(splitPane);
	}
	
	public boolean checkForModifications() {
		for (Board board : configuration.getBoards()) {
			if (board.isDirty()) {
				int result = JOptionPane.showConfirmDialog(
						frame, 
						"Do you want to save your changes to " + board.getFile().getName() + "?" +
						"\n" +
						"If you don't save, your changes will be lost.",
						"Save " + board.getFile().getName() + "?", 
						JOptionPane.YES_NO_CANCEL_OPTION);
				if (result == JOptionPane.YES_OPTION) {
					try {
						configuration.saveBoard(board);
					}
					catch (Exception e) {
						MessageBoxes.errorBox(BoardsPanel.this, "Board Save Error", e.getMessage());
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
	
	public Action newBoardAction = new AbstractAction("New Board...") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			FileDialog fileDialog = new FileDialog(frame, "Save New Board As...", FileDialog.SAVE);
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
				
				Board board = configuration.getBoard(file);
			}
			catch (Exception e) {
				e.printStackTrace();
				MessageBoxes.errorBox(BoardsPanel.this, "Unable to create new board", e.getMessage());
			}
		}
	};

	public Action addBoardAction = new AbstractAction("Load Board...") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			FileDialog fileDialog = new FileDialog(frame);
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
			
				Board board = configuration.getBoard(file);
			}
			catch (Exception e) {
				e.printStackTrace();
				MessageBoxes.errorBox(BoardsPanel.this, "Board load failed", e.getMessage());
			}
		}
	};

	public Action newPlacementAction = new AbstractAction("New Placement") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			Board board = getSelectedBoard();
			// TODO: Make sure it's unique
			String id = JOptionPane.showInputDialog(frame, "Please enter an ID for the new placement.");
			if (id == null) {
				return;
			}
			Placement placement = new Placement(id);
			placement.getLocation().setUnits(configuration.getMachine().getNativeUnits());
			board.addPlacement(placement);
			placementsTableModel.fireTableDataChanged();
		}
	};
	
	public Action removePlacementAction = new AbstractAction("Remove Placement") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			Board board = getSelectedBoard();
			Placement placement = getSelectedPlacement();
			board.removePlacement(placement);
			placementsTableModel.fireTableDataChanged();
		}
	};
}
