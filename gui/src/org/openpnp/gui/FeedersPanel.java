package org.openpnp.gui;

import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.regex.PatternSyntaxException;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.table.TableRowSorter;

public class FeedersPanel extends JPanel {
	private JTable table;

	private FeedersTableModel tableModel;
	private TableRowSorter<FeedersTableModel> tableSorter;
	private JTextField searchTextField;

	public FeedersPanel() {
		setLayout(new BorderLayout(0, 0));
		tableModel = new FeedersTableModel();

		JPanel panel = new JPanel();
		add(panel, BorderLayout.NORTH);
		panel.setLayout(new BorderLayout(0, 0));

		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		panel.add(toolBar, BorderLayout.CENTER);

		JPanel panel_1 = new JPanel();
		panel.add(panel_1, BorderLayout.EAST);

		JLabel lblSearch = new JLabel("Search");
		panel_1.add(lblSearch);

		searchTextField = new JTextField();
		searchTextField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent arg0) {
				search();
			}
		});
		panel_1.add(searchTextField);
		searchTextField.setColumns(15);
		table = new JTable(tableModel);
		tableSorter = new TableRowSorter<FeedersTableModel>(tableModel);
		table.setRowSorter(tableSorter);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		add(new JScrollPane(table));
	}

	private void search() {
		RowFilter<FeedersTableModel, Object> rf = null;
		// If current expression doesn't parse, don't update.
		try {
			rf = RowFilter.regexFilter("(?i)" + searchTextField.getText().trim());
		}
		catch (PatternSyntaxException e) {
			System.out.println(e);
			return;
		}
		tableSorter.setRowFilter(rf);
	}

	public void refresh() {
		tableModel.refresh();
	}
}
