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

import org.openpnp.ConfigurationListener;
import org.openpnp.JobProcessor;
import org.openpnp.JobProcessor.JobError;
import org.openpnp.JobProcessor.JobState;
import org.openpnp.JobProcessor.PickRetryAction;
import org.openpnp.JobProcessorDelegate;
import org.openpnp.JobProcessorListener;
import org.openpnp.gui.components.AutoSelectTextTable;
import org.openpnp.gui.support.ActionGroup;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.tablemodel.BoardLocationsTableModel;
import org.openpnp.gui.tablemodel.PlacementsTableModel;
import org.openpnp.model.Board;
import org.openpnp.model.Board.Side;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Job;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MachineListener;
import javax.swing.ImageIcon;

public class JobPanel extends JPanel implements ConfigurationListener {
	final private Configuration configuration;
	final private JobProcessor jobProcessor;
	final private Frame frame;
	final private MachineControlsPanel machineControlsPanel;
	
	private static final String PREF_DIVIDER_POSITION = "JobPanel.dividerPosition";
	private static final int PREF_DIVIDER_POSITION_DEF = -1;
	
	final private static String UNTITLED_JOB_FILENAME = "Untitled.job.xml";
	
	private BoardLocationsTableModel boardLocationsTableModel;
	private PlacementsTableModel placementsTableModel;
	private JTable boardLocationsTable;
	private JTable placementsTable;
	private JSplitPane splitPane; 
	
	private ActionGroup jobSaveActionGroup;
	private ActionGroup boardLocationSelectionActionGroup;
	private ActionGroup placementSelectionActionGroup;
	
	private Preferences prefs = Preferences.userNodeForPackage(JobPanel.class);
	
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
				captureCameraBoardLocationAction,
				captureToolBoardLocationAction,
				newPlacementAction,
				moveCameraToBoardLocationAction,
				moveToolToBoardLocationAction);
		boardLocationSelectionActionGroup.setEnabled(false);
		
		placementSelectionActionGroup = new ActionGroup(removePlacementAction,
				captureCameraPlacementLocation,
				captureToolPlacementLocation);
		placementSelectionActionGroup.setEnabled(false);
		
		boardLocationsTableModel = new BoardLocationsTableModel(configuration);
		placementsTableModel = new PlacementsTableModel(configuration);

		JComboBox sidesComboBox = new JComboBox(Side.values());
		
		placementsTable = new AutoSelectTextTable(placementsTableModel);
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
		
		boardLocationsTable = new AutoSelectTextTable(boardLocationsTableModel);
		boardLocationsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		boardLocationsTable.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(sidesComboBox));
		
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
		
		splitPane = new JSplitPane();
		splitPane.setBorder(null);
		splitPane.setContinuousLayout(true);
		splitPane.setDividerLocation(500);
		splitPane.addPropertyChangeListener("dividerLocation",
				new PropertyChangeListener() {
					@Override
					public void propertyChange(PropertyChangeEvent evt) {
						prefs.putInt(PREF_DIVIDER_POSITION,
								splitPane.getDividerLocation());
					}
				});
		
		JPanel left = new JPanel();
		left.setLayout(new BorderLayout(0, 0));
		
		JToolBar toolBar_1 = new JToolBar();
		toolBar_1.setFloatable(false);
		left.add(toolBar_1, BorderLayout.NORTH);
		
		JButton button = new JButton(startPauseResumeJobAction);
		toolBar_1.add(button);
		JButton button_1 = new JButton(stepJobAction);
		toolBar_1.add(button_1);
		JButton button_2 = new JButton(stopJobAction);
		toolBar_1.add(button_2);
		toolBar_1.addSeparator();
		JButton button_3 = new JButton(newBoardAction);
		toolBar_1.add(button_3);
		JButton button_4 = new JButton(addBoardAction);
		toolBar_1.add(button_4);
		JButton button_5 = new JButton(removeBoardAction);
		toolBar_1.add(button_5);
		toolBar_1.addSeparator();
		JButton button_6 = new JButton(captureCameraBoardLocationAction);
		button_6.setIcon(new ImageIcon(JobPanel.class.getResource("/icons/capture-camera.png")));
		button_6.setToolTipText("Set the board's location to the camera's location.");
		button_6.setHideActionText(true);
		toolBar_1.add(button_6);
		
		JButton button_11 = new JButton(captureToolBoardLocationAction);
		button_11.setIcon(new ImageIcon(JobPanel.class.getResource("/icons/capture-tool.png")));
		button_11.setToolTipText("Set the board's location to the tool's location.");
		button_11.setHideActionText(true);
		toolBar_1.add(button_11);
		
		JButton button_7 = new JButton(moveCameraToBoardLocationAction);
		button_7.setToolTipText("Position the camera at the board's location.");
		button_7.setIcon(new ImageIcon(JobPanel.class.getResource("/icons/center-camera.png")));
		button_7.setHideActionText(true);
		toolBar_1.add(button_7);
		
		JButton btnNewButton = new JButton(moveToolToBoardLocationAction);
		btnNewButton.setToolTipText("Position the tool at the board's location.");
		btnNewButton.setHideActionText(true);
		btnNewButton.setIcon(new ImageIcon(JobPanel.class.getResource("/icons/center-tool.png")));
		toolBar_1.add(btnNewButton);
		
		left.add(new JScrollPane(boardLocationsTable));
		JPanel right = new JPanel();
		right.setLayout(new BorderLayout(0, 0));
		
		JToolBar toolBar_2 = new JToolBar();
		toolBar_2.setFloatable(false);
		right.add(toolBar_2, BorderLayout.NORTH);
		JButton button_8 = new JButton(newPlacementAction);
		toolBar_2.add(button_8);
		JButton button_9 = new JButton(removePlacementAction);
		toolBar_2.add(button_9);
		toolBar_2.addSeparator();
		JButton button_10 = new JButton(captureCameraPlacementLocation);
		button_10.setHideActionText(true);
		button_10.setIcon(new ImageIcon(JobPanel.class.getResource("/icons/capture-camera.png")));
		button_10.setText("");
		toolBar_2.add(button_10);
		
		JButton button_12 = new JButton(captureToolPlacementLocation);
		button_12.setHideActionText(true);
		button_12.setIcon(new ImageIcon(JobPanel.class.getResource("/icons/capture-tool.png")));
		toolBar_2.add(button_12);
		right.add(new JScrollPane(placementsTable));
		
		splitPane.setLeftComponent(left);
		splitPane.setRightComponent(right);
		
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
			String name = (job.getFile() == null ? UNTITLED_JOB_FILENAME : job.getFile().getName());
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
				MessageBoxes.errorBox(frame, "Job Save Error", e.getMessage());
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
			MessageBoxes.errorBox(frame, "Job Save Error", e.getMessage());
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
				(job.getFile() == null ? UNTITLED_JOB_FILENAME : job.getFile().getName()));
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
				MessageBoxes.errorBox(frame, "Job Load Error", e.getMessage());
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
					MessageBoxes.errorBox(frame, "Job Start Error", e.getMessage());
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
				MessageBoxes.errorBox(frame, "Job Step Failed", e.getMessage());
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
				jobProcessor.getJob().addBoardLocation(boardLocation);
				boardLocationsTableModel.fireTableDataChanged();
			}
			catch (Exception e) {
				e.printStackTrace();
				MessageBoxes.errorBox(frame, "Unable to create new board", e.getMessage());
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
				jobProcessor.getJob().addBoardLocation(boardLocation);
				boardLocationsTableModel.fireTableDataChanged();
			}
			catch (Exception e) {
				e.printStackTrace();
				MessageBoxes.errorBox(frame, "Board load failed", e.getMessage());
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

	public Action captureCameraBoardLocationAction = new AbstractAction("Capture Camera Location") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			getSelectedBoardLocation().setLocation(machineControlsPanel.getCameraLocation());
			boardLocationsTableModel.fireTableRowsUpdated(boardLocationsTable.getSelectedRow(), boardLocationsTable.getSelectedRow());
		}
	};
	
	// TODO: Need a better name for the MenuItem
	public Action captureToolBoardLocationAction = new AbstractAction("Capture Tool Location") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			getSelectedBoardLocation().setLocation(machineControlsPanel.getToolLocation());
			boardLocationsTableModel.fireTableRowsUpdated(boardLocationsTable.getSelectedRow(), boardLocationsTable.getSelectedRow());
		}
	};
	
	public Action moveCameraToBoardLocationAction = new AbstractAction("Move Camera To Board Location") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			MainFrame.machineControlsPanel.submitMachineTask(new Runnable() {
				public void run() {
					Head head = configuration.getMachine().getHeads().get(0);
					try {
						Camera camera = MainFrame.cameraPanel.getSelectedCamera();
						Location location = getSelectedBoardLocation().getLocation();
						location = location.convertToUnits(configuration.getMachine().getNativeUnits());
						location = location.subtract(camera.getLocation());
						head.moveToSafeZ();
						// Move the head to the location at Safe-Z 
						head.moveTo(location.getX(), location.getY(), head.getZ(), location.getRotation());
						// Move Z
						head.moveTo(head.getX(), head.getY(), location.getZ(), head.getC());
					}
					catch (Exception e) {
						MessageBoxes.errorBox(getTopLevelAncestor(), "Move Error", e);
					}
				}
			});
		}
	};
	
	public Action moveToolToBoardLocationAction = new AbstractAction("Move Tool To Board Location") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			MainFrame.machineControlsPanel.submitMachineTask(new Runnable() {
				public void run() {
					Head head = configuration.getMachine().getHeads().get(0);
					try {
						Location location = getSelectedBoardLocation().getLocation();
						location = location.convertToUnits(configuration.getMachine().getNativeUnits());
						head.moveToSafeZ();
						// Move the head to the location at Safe-Z 
						head.moveTo(location.getX(), location.getY(), head.getZ(), location.getRotation());
						// Move Z
						head.moveTo(head.getX(), head.getY(), location.getZ(), head.getC());
					}
					catch (Exception e) {
						MessageBoxes.errorBox(getTopLevelAncestor(), "Move Error", e);
					}
				}
			});
		}
	};
	
	public Action newPlacementAction = new AbstractAction("New Placement") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			BoardLocation boardLocation = getSelectedBoardLocation();
			String id = JOptionPane.showInputDialog(frame, "Please enter an ID for the new placement.");
			if (id == null) {
				return;
			}
			// TODO: Make sure it's unique.
			Placement placement = new Placement(id);
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
	
	public Action captureCameraPlacementLocation = new AbstractAction("Capture Camera Placement Location") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
//			Location boardLocation = getSelectedBoardLocation().getLocation();
//			getSelectedPlacement().setLocation(machineControlsPanel.getCameraLocation().subtract(boardLocation));
//			placementsTableModel.fireTableRowsUpdated(placementsTable.getSelectedRow(), placementsTable.getSelectedRow());
			MessageBoxes.errorBox(getTopLevelAncestor(), "Not Yet Implemented", "This action is not yet implemented.");
		}
	};
	
	public Action captureToolPlacementLocation = new AbstractAction("Capture Tool Placement Location") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
//			Location boardLocation = getSelectedBoardLocation().getLocation();
//			getSelectedPlacement().setLocation(machineControlsPanel.getToolLocation().subtract(boardLocation));
//			placementsTableModel.fireTableRowsUpdated(placementsTable.getSelectedRow(), placementsTable.getSelectedRow());
			MessageBoxes.errorBox(getTopLevelAncestor(), "Not Yet Implemented", "This action is not yet implemented.");
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
			MessageBoxes.errorBox(frame, error.toString(), description + "\n\nThe job will be paused.");
			// TODO: Implement a way to retry, abort, etc.
			jobProcessor.pause();
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
