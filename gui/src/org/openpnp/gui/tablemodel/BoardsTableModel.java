package org.openpnp.gui.tablemodel;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.openpnp.model.Board;
import org.openpnp.model.Configuration;

public class BoardsTableModel extends AbstractTableModel implements PropertyChangeListener {
	private String[] columnNames = new String[] { "Board", "Path" };

	private final Configuration configuration;
	
	private List<Board> boards; 
	
	public BoardsTableModel(Configuration configuration) {
		this.configuration = configuration;
		boards = configuration.getBoards();
		configuration.addPropertyChangeListener("boards", this);
	}
	
	@Override
	public String getColumnName(int column) {
		return columnNames[column];
	}

	public int getColumnCount() {
		return columnNames.length;
	}

	public int getRowCount() {
		return boards.size();
	}
	
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		if (columnIndex == 1) {
			return false;
		}
		return true;
	}
	
	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		try {
			Board board = boards.get(rowIndex);
			if (columnIndex == 0) {
				board.setName(aValue.toString());
			}
		}
		catch (Exception e) {
			// TODO: dialog, bad input
		}
	}

	public Object getValueAt(int row, int col) {
		Board board = boards.get(row);
		switch (col) {
		case 0:
			return board.getName();
		case 1:
			return board.getFile().getPath();
		default:
			return null;
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		boards = configuration.getBoards();
		fireTableDataChanged();
	}
}