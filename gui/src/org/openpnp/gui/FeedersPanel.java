package org.openpnp.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.regex.PatternSyntaxException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableRowSorter;

import org.openpnp.Configuration;
import org.openpnp.gui.components.MachineControlsPanel;
import org.openpnp.spi.Feeder;

public class FeedersPanel extends JPanel implements WizardContainer {
	private final Configuration configuration;
	private final MachineControlsPanel machineControlsPanel;
	
	private JTable table;

	private FeedersTableModel tableModel;
	private TableRowSorter<FeedersTableModel> tableSorter;
	private JTextField searchTextField;
	JPanel configurationPanel;

	public FeedersPanel(Configuration configuration, MachineControlsPanel machineControlsPanel) {
		this.configuration = configuration;
		this.machineControlsPanel = machineControlsPanel;
		
		setLayout(new BorderLayout(0, 0));
		tableModel = new FeedersTableModel(configuration);

		JPanel panel = new JPanel();
		add(panel, BorderLayout.NORTH);
		panel.setLayout(new BorderLayout(0, 0));

		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		panel.add(toolBar, BorderLayout.CENTER);
		
		toolBar.add(newFeederAction);
		toolBar.add(deleteFeederAction);
		toolBar.addSeparator();
		toolBar.add(feedFeederAction);

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

	@Override
	public void wizardCompleted(Wizard wizard) {
		configuration.setDirty(true);
	}

	@Override
	public void wizardCancelled(Wizard wizard) {
	}
	
	@Override
	public Configuration getConfiguration() {
		return configuration;
	}

	@Override
	public MachineControlsPanel getMachineControlsPanel() {
		return machineControlsPanel;
	}

	public Action newFeederAction = new AbstractAction("New...") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
		}
	};
	
	public Action deleteFeederAction = new AbstractAction("Delete") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
		}
	};
	
	public Action feedFeederAction = new AbstractAction("Feed") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
		}
	};
}