package org.openpnp.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableRowSorter;

import org.openpnp.gui.components.MachineControlsPanel;
import org.openpnp.model.Configuration;
import org.openpnp.model.FeederLocation;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.util.LengthUtil;

public class PartsPanel extends JPanel {
	final private Configuration configuration;
	final private MachineControlsPanel machineControlsPanel;
	
	private PartsTableModel partsTableModel;
	private FeederLocationsTableModel feederLocationsTableModel;
	private TableRowSorter<PartsTableModel> partsTableSorter;
	private JTextField searchTextField;
	private JTable partsTable;
	private JTable feederLocationsTable;

	public PartsPanel(Configuration configuration, MachineControlsPanel machineControlsPanel) {
		this.configuration = configuration;
		this.machineControlsPanel = machineControlsPanel;
		
		setLayout(new BorderLayout(0, 0));
		partsTableModel = new PartsTableModel(configuration);
		partsTableSorter = new TableRowSorter<PartsTableModel>(partsTableModel);
		feederLocationsTableModel = new FeederLocationsTableModel(configuration);

		JPanel panel_5 = new JPanel();
		add(panel_5, BorderLayout.NORTH);
		panel_5.setLayout(new BorderLayout(0, 0));

		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		panel_5.add(toolBar);

		JPanel panel_1 = new JPanel();
		panel_5.add(panel_1, BorderLayout.EAST);

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

		partsTable = new JTable(partsTableModel);
		partsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		feederLocationsTable = new JTable(feederLocationsTableModel);
		feederLocationsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		JSplitPane splitPane = new JSplitPane();
		splitPane.setContinuousLayout(true);
		add(splitPane, BorderLayout.CENTER);

		splitPane.setLeftComponent(new JScrollPane(partsTable));

		splitPane.setRightComponent(new JScrollPane(feederLocationsTable));
		
		partsTable.setRowSorter(partsTableSorter);
		
		partsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) {
					return;
				}
				int index = partsTable.getSelectedRow();
				
				deletePartAction.setEnabled(index != -1);
				newFeederLocationAction.setEnabled(index != -1);
				
				if (index == -1) {
					feederLocationsTableModel.setFeederLocations(null);
				}
				else {
					index = partsTable.convertRowIndexToModel(index);
					Part part = partsTableModel.getPart(index);
					List<FeederLocation> feederLocations = part.getFeederLocations();
					feederLocationsTableModel.setFeederLocations(feederLocations);
				}
			}
		});
		
		feederLocationsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) {
					return;
				}
				int index = feederLocationsTable.getSelectedRow();
				
				deleteFeederLocationAction.setEnabled(index != -1);
				setFeederLocationLocationAction.setEnabled(index != -1);				
			}
		});
		
		
		deletePartAction.setEnabled(false);
		newFeederLocationAction.setEnabled(false);
		deleteFeederLocationAction.setEnabled(false);
		setFeederLocationLocationAction.setEnabled(false);
		
		toolBar.add(newPartAction);
		toolBar.add(deletePartAction);
		toolBar.addSeparator();
		toolBar.add(newFeederLocationAction);
		toolBar.add(deleteFeederLocationAction);
		toolBar.addSeparator();
		toolBar.add(setFeederLocationLocationAction);
	}
	
	private void search() {
		RowFilter<PartsTableModel, Object> rf = null;
		// If current expression doesn't parse, don't update.
		try {
			rf = RowFilter.regexFilter("(?i)"
					+ searchTextField.getText().trim());
		}
		catch (PatternSyntaxException e) {
			System.out.println(e);
			return;
		}
		partsTableSorter.setRowFilter(rf);
	}

	public Action newPartAction = new AbstractAction("New Part") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
		}
	};
	
	public Action deletePartAction = new AbstractAction("Delete Part") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
		}
	};
	
	public Action newFeederLocationAction = new AbstractAction("New Feeder Location") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
		}
	};
	
	public Action deleteFeederLocationAction = new AbstractAction("Delete Feeder Location") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
		}
	};
	
	public Action setFeederLocationLocationAction = new AbstractAction("Set Feeder Location") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			Location location = machineControlsPanel.getDisplayedLocation();
			
			int index = feederLocationsTable.getSelectedRow();
			index = feederLocationsTable.convertRowIndexToModel(index);
			FeederLocation feederLocation = feederLocationsTableModel.getFeederLocation(index);

			feederLocation.setLocation(location);
			
			feederLocationsTableModel.fireTableRowsUpdated(index, index);
			configuration.setDirty(true);
		}
	};
}
