package org.openpnp.gui;

import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableRowSorter;

import org.openpnp.FeederLocation;
import org.openpnp.spi.Feeder;
import javax.swing.border.TitledBorder;

public class FeedersPanel extends JPanel implements WizardContainer {
	private JTable table;

	private FeedersTableModel tableModel;
	private TableRowSorter<FeedersTableModel> tableSorter;
	private JTextField searchTextField;
	JPanel configurationPanel;

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
		
		JSplitPane splitPane = new JSplitPane();
		splitPane.setContinuousLayout(true);
		splitPane.setDividerLocation(0.3);
		add(splitPane, BorderLayout.CENTER);
		
		configurationPanel = new JPanel();
		configurationPanel.setBorder(new TitledBorder(null, "Configuration", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		splitPane.setLeftComponent(new JScrollPane(table));
		splitPane.setRightComponent(configurationPanel);
		configurationPanel.setLayout(new BorderLayout(0, 0));
		table.setRowSorter(tableSorter);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) {
					return;
				}
				int index = table.getSelectedRow();
				
				configurationPanel.removeAll();
				if (index != -1) {
					index = table.convertRowIndexToModel(index);
					Feeder feeder = tableModel.getFeeder(index);
					Wizard wizard = feeder.getConfigurationWizard();
					if (wizard != null) {
						wizard.setWizardContainer(FeedersPanel.this);
						JPanel panel = wizard.getWizardPanel();
						configurationPanel.add(panel);
					}
				}
				revalidate();
				repaint();
			}
		});
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

	@Override
	public void wizardCompleted(Wizard wizard) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void wizardCancelled(Wizard wizard) {
		// TODO Auto-generated method stub
		
	}
}
