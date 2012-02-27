package org.openpnp.gui;

import java.awt.BorderLayout;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FilenameFilter;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openpnp.ConfigurationListener;
import org.openpnp.JobProcessor;
import org.openpnp.JobProcessor.JobError;
import org.openpnp.JobProcessor.JobState;
import org.openpnp.JobProcessor.PickRetryAction;
import org.openpnp.JobProcessorDelegate;
import org.openpnp.JobProcessorListener;
import org.openpnp.gui.components.MachineControlsPanel;
import org.openpnp.gui.support.ActionGroup;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.model.Board;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Job;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MachineListener;

public class JobPanel extends JPanel implements ConfigurationListener {
	final private Configuration configuration;
	final private JobProcessor jobProcessor;
	final private Frame frame;
	final private MachineControlsPanel machineControlsPanel;
	
	final private static String UNTITLED = "Untitled";
	
	private BoardLocationsTableModel boardLocationsTableModel;
	private PlacementsTableModel placementsTableModel;
	private JTable boardLocationsTable;
	private JTable placementsTable;
	
	private ActionGroup jobSaveActionGroup;
	private ActionGroup boardLocationSelectionActionGroup;
	private ActionGroup placementSelectionActionGroup;
	
	public JobPanel(Configuration configuration, 
			JobProcessor jobProcessor, 
			Frame frame, 
			MachineControlsPanel machineControlsPanel) {
		this.configuration = configuration;
		this.jobProcessor = jobProcessor;
		this.frame = frame;
		this.machineControlsPanel = machineControlsPanel;
		
		jobSaveActionGroup = new ActionGroup(saveJobAction, saveJobAsAction);
		jobSaveActionGroup.setEnabled(false);
		
		boardLocationSelectionActionGroup = new ActionGroup(removeBoardAction, 
				orientBoardAction,
				newPlacementAction);
		boardLocationSelectionActionGroup.setEnabled(false);
		
		placementSelectionActionGroup = new ActionGroup(removePlacementAction,
				orientPlacementAction);
		placementSelectionActionGroup.setEnabled(false);
		
		boardLocationsTableModel = new BoardLocationsTableModel();
		placementsTableModel = new PlacementsTableModel(configuration);

		placementsTable = new JTable(placementsTableModel);
		placementsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		placementsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) {
					return;
				}
				placementSelectionActionGroup.setEnabled(getSelectedPlacement() != null);
			}
		});
		
//		JComboBox partsComboBox = new JComboBox(new String[] { "R-0805-10K", "R-0805-1K", "R-0805-100K" });
//		placementsTable.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(partsComboBox));

		boardLocationsTable = new JTable(boardLocationsTableModel);
		boardLocationsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		boardLocationsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) {
					return;
				}
				BoardLocation boardLocation = getSelectedBoardLocation();
				boardLocationSelectionActionGroup.setEnabled(boardLocation != null);
				if (boardLocation == null) {
					placementsTableModel.setBoard(null);
				}
				else {
					placementsTableModel.setBoard(boardLocation.getBoard());
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
		
		toolBar.add(new JButton(startPauseResumeJobAction));
		toolBar.add(new JButton(stepJobAction));
		toolBar.add(new JButton(stopJobAction));
		toolBar.addSeparator();
		toolBar.add(new JButton(newBoardAction));
		toolBar.add(new JButton(addBoardAction));
		toolBar.add(new JButton(removeBoardAction));
		toolBar.addSeparator();
		toolBar.add(new JButton(orientBoardAction));
		toolBar.addSeparator();
		toolBar.add(new JButton(newPlacementAction));
		toolBar.add(new JButton(removePlacementAction));
		toolBar.addSeparator();
		toolBar.add(new JButton(orientPlacementAction));
		
		JSplitPane splitPane = new JSplitPane();
		splitPane.setBorder(null);
		splitPane.setContinuousLayout(true);
		splitPane.setDividerLocation(350);
		splitPane.setLeftComponent(new JScrollPane(boardLocationsTable));
		splitPane.setRightComponent(new JScrollPane(placementsTable));
		
		add(splitPane);
		
		jobProcessor.addListener(jobProcessorListener);
		jobProcessor.setDelegate(jobProcessorDelegate);

		configuration.addListener(this);
	}
	
	public BoardLocation getSelectedBoardLocation() {
		int index = boardLocationsTable.getSelectedRow();
		if (index == -1) {
			return null;
		}
		else {
			index = boardLocationsTable.convertRowIndexToModel(index);
			return JobPanel.this.jobProcessor.getJob().getBoardLocations().get(index);
		}
	}
	
	public Placement getSelectedPlacement() {
		if (getSelectedBoardLocation() == null) {
			return null;
		}
		int index = placementsTable.getSelectedRow();
		if (index == -1) {
			return null;
		}
		else {
			index = placementsTable.convertRowIndexToModel(index);
			return getSelectedBoardLocation().getBoard().getPlacements().get(index);
		}
	}
	
	public void configurationLoaded(Configuration configuration) {
		configuration.getMachine().addListener(machineListener);
		updateJobActions();
		
		// Create an empty Job if one is not loaded
		if (jobProcessor.getJob() == null) {
			Job job = new Job();
			jobProcessor.load(job);
		}
	}
	
	public boolean checkForModifications() {
		if (jobProcessor.getJob().isDirty()) {
			Job job = jobProcessor.getJob();
			String name = (job.getFile() == null ? UNTITLED : job.getFile().getName());
			int result = JOptionPane.showConfirmDialog(
					frame,
					"Do you want to save your changes to " + name + "?" +
					"\n" +
					"If you don't save, your changes will be lost.",
					"Save " + name + "?", 
					JOptionPane.YES_NO_CANCEL_OPTION);
			if (result == JOptionPane.YES_OPTION) {
				return saveJob();
			}
			else if (result == JOptionPane.CANCEL_OPTION) {
				return false;
			}
		}
		return true;
	}
	
	private boolean saveJob() {
		if (jobProcessor.getJob().getFile() == null) {
			return saveJobAs();
		}
		else {
			try {
				configuration.saveJob(jobProcessor.getJob(), jobProcessor.getJob().getFile());
				return true;
			}
			catch (Exception e) {
				MessageBoxes.errorBox(JobPanel.this, "Job Save Error", e.getMessage());
				return false;
			}
		}
	}
	
	private boolean saveJobAs() {
		FileDialog fileDialog = new FileDialog(frame, "Save Job As...", FileDialog.SAVE);
		fileDialog.setFilenameFilter(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".job.xml");
			}
		});
		fileDialog.setVisible(true);
		try {
			String filename = fileDialog.getFile();
			if (filename == null) {
				return false;
			}
			if (!filename.toLowerCase().endsWith(".job.xml")) {
				filename = filename + ".job.xml";
			}
			File file = new File(new File(fileDialog.getDirectory()),
					filename);
			configuration.saveJob(jobProcessor.getJob(), file);
			return true;
		}
		catch (Exception e) {
			MessageBoxes.errorBox(JobPanel.this, "Job Save Error", e.getMessage());
			return false;
		}
	}
	
	/**
	 * Updates the Job controls based on the Job state and the Machine's
	 * readiness.
	 */
	private void updateJobActions() {
		JobState state = jobProcessor.getState();
		if (state == JobState.Stopped) {
			startPauseResumeJobAction.setEnabled(true);
			startPauseResumeJobAction.putValue(AbstractAction.NAME, "Start");
			stopJobAction.setEnabled(false);
			stepJobAction.setEnabled(true);
		}
		else if (state == JobState.Running) {
			startPauseResumeJobAction.setEnabled(true);
			startPauseResumeJobAction.putValue(AbstractAction.NAME, "Pause");
			stopJobAction.setEnabled(true);
			stepJobAction.setEnabled(false);
		}
		else if (state == JobState.Paused) {
			startPauseResumeJobAction.setEnabled(true);
			startPauseResumeJobAction.putValue(AbstractAction.NAME, "Resume");
			stopJobAction.setEnabled(true);
			stepJobAction.setEnabled(true);
		}

		// We allow the above to run first so that all state is represented
		// correctly even if the machine is disabled.
		if (!configuration.getMachine().isEnabled()) {
			startPauseResumeJobAction.setEnabled(false);
			stopJobAction.setEnabled(false);
			stepJobAction.setEnabled(false);
		}
	}
	
	private void updateTitle() {
		Job job = jobProcessor.getJob();
		String title = String.format("OpenPnP - %s%s",
				job.isDirty() ? "*" : "",
				(job.getFile() == null ? UNTITLED : job.getFile().getName()));
		frame.setTitle(title);
	}
	
	public Action openJobAction = new AbstractAction("Open Job...") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (!checkForModifications()) {
				return;
			}
			FileDialog fileDialog = new FileDialog(frame);
			fileDialog.setFilenameFilter(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.toLowerCase().endsWith(".job.xml");
				}
			});
			fileDialog.setVisible(true);
			try {
				File file = new File(new File(fileDialog.getDirectory()),
						fileDialog.getFile());
				Job job = configuration.loadJob(file);
				jobProcessor.load(job);
			}
			catch (Exception e) {
				e.printStackTrace();
				MessageBoxes.errorBox(JobPanel.this, "Job Load Error", e.getMessage());
			}
		}
	};

	public Action newJobAction = new AbstractAction("New Job") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (!checkForModifications()) {
				return;
			}
			jobProcessor.load(new Job());
		}
	};

	public Action saveJobAction = new AbstractAction("Save Job") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			saveJob();
		}
	};

	public Action saveJobAsAction = new AbstractAction("Save Job As...") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			saveJobAs();
		}
	};

	public Action startPauseResumeJobAction = new AbstractAction("Start") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			JobState state = jobProcessor.getState();
			if (state == JobState.Stopped) {
				try {
					jobProcessor.start();
				}
				catch (Exception e) {
					MessageBoxes.errorBox(JobPanel.this, "Job Start Error", e.getMessage());
				}
			}
			else if (state == JobState.Paused) {
				jobProcessor.resume();
			}
			else if (state == JobState.Running) {
				jobProcessor.pause();
			}
		}
	};

	public Action stepJobAction = new AbstractAction("Step") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			try {
				jobProcessor.step();
			}
			catch (Exception e) {
				MessageBoxes.errorBox(JobPanel.this, "Job Step Failed", e.getMessage());
			}
		}
	};

	public Action stopJobAction = new AbstractAction("Stop") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			jobProcessor.stop();
		}
	};

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
				BoardLocation boardLocation = new BoardLocation(board);
				boardLocation.getLocation().setUnits(configuration.getMachine().getNativeUnits());
				jobProcessor.getJob().addBoardLocation(boardLocation);
				boardLocationsTableModel.fireTableDataChanged();
			}
			catch (Exception e) {
				e.printStackTrace();
				MessageBoxes.errorBox(JobPanel.this, "Unable to create new board", e.getMessage());
			}
		}
	};

	public Action addBoardAction = new AbstractAction("Add Board...") {
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
				BoardLocation boardLocation = new BoardLocation(board);
				boardLocation.getLocation().setUnits(configuration.getMachine().getNativeUnits());
				jobProcessor.getJob().addBoardLocation(boardLocation);
				boardLocationsTableModel.fireTableDataChanged();
			}
			catch (Exception e) {
				e.printStackTrace();
				MessageBoxes.errorBox(JobPanel.this, "Board load failed", e.getMessage());
			}
		}
	};

	public Action removeBoardAction = new AbstractAction("Remove Board") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			int index = boardLocationsTable.getSelectedRow();
			if (index != -1) {
				index = boardLocationsTable.convertRowIndexToModel(index);
				BoardLocation boardLocation = JobPanel.this.jobProcessor.getJob().getBoardLocations().get(index);
				JobPanel.this.jobProcessor.getJob().removeBoardLocation(boardLocation);
				boardLocationsTableModel.fireTableDataChanged();
			}
		}
	};

	public Action orientBoardAction = new AbstractAction("Set Board Location") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			getSelectedBoardLocation().setLocation(machineControlsPanel.getDisplayedLocation());
			boardLocationsTableModel.fireTableDataChanged();
		}
	};
	
	public Action newPlacementAction = new AbstractAction("New Placement") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			BoardLocation boardLocation = getSelectedBoardLocation();
			// TODO: Make sure it's unique.
			String id = JOptionPane.showInputDialog(frame, "Please enter an ID for the new placement.");
			if (id == null) {
				return;
			}
			Placement placement = new Placement(id);
			placement.getLocation().setUnits(configuration.getMachine().getNativeUnits());
			boardLocation.getBoard().addPlacement(placement);
			placementsTableModel.fireTableDataChanged();
		}
	};
	
	public Action removePlacementAction = new AbstractAction("Remove Placement") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			BoardLocation boardLocation = getSelectedBoardLocation();
			Placement placement = getSelectedPlacement();
			boardLocation.getBoard().removePlacement(placement);
			placementsTableModel.fireTableDataChanged();
		}
	};
	
	public Action orientPlacementAction = new AbstractAction("Set Placement Location") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			// TODO: This needs to be changed to be relative to the Board's position.
			getSelectedPlacement().setLocation(machineControlsPanel.getDisplayedLocation());
			placementsTableModel.fireTableDataChanged();
		}
	};
	
	private JobProcessorListener jobProcessorListener = new JobProcessorListener.Adapter() {
		@Override
		public void jobStateChanged(JobState state) {
			updateJobActions();
		}

		@Override
		public void jobLoaded(Job job) {
			placementsTableModel.setBoard(null);
			boardLocationsTableModel.setJob(jobProcessor.getJob());
			job.addPropertyChangeListener("dirty", titlePropertyChangeListener);
			job.addPropertyChangeListener("file", titlePropertyChangeListener);
			updateTitle();
			updateJobActions();
		}

		@Override
		public void jobEncounteredError(JobError error, String description) {
			MessageBoxes.errorBox(JobPanel.this, error.toString(), description);
		}
	};
	
	private JobProcessorDelegate jobProcessorDelegate = new JobProcessorDelegate() {
		@Override
		public PickRetryAction partPickFailed(BoardLocation board, Part part,
				Feeder feeder) {
			return PickRetryAction.SkipAndContinue;
		}
	};
	
	private MachineListener machineListener = new MachineListener.Adapter() {
		@Override
		public void machineEnabled(Machine machine) {
			updateJobActions();
		}

		@Override
		public void machineDisabled(Machine machine, String reason) {
			updateJobActions();
			jobProcessor.stop();
		}
	};
	
	private PropertyChangeListener titlePropertyChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			updateTitle();
			jobSaveActionGroup.setEnabled(jobProcessor.getJob().isDirty());
		}
	};
}
