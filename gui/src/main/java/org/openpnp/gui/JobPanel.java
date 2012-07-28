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
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.openpnp.model.Point;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MachineListener;
import org.openpnp.util.Utils2D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class JobPanel extends JPanel implements ConfigurationListener {
	private static final Logger logger = LoggerFactory
			.getLogger(JobPanel.class);

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

	private Location boardLocationA, boardLocationB;
	private Placement boardLocationPlacementA, boardLocationPlacementB;

	public JobPanel(Configuration configuration, JobProcessor jobProcessor,
			Frame frame, MachineControlsPanel machineControlsPanel) {
		this.configuration = configuration;
		this.jobProcessor = jobProcessor;
		this.frame = frame;
		this.machineControlsPanel = machineControlsPanel;

		jobSaveActionGroup = new ActionGroup(saveJobAction, saveJobAsAction);
		jobSaveActionGroup.setEnabled(false);

		boardLocationSelectionActionGroup = new ActionGroup(removeBoardAction,
				captureCameraBoardLocationAction,
				captureToolBoardLocationAction, newPlacementAction,
				moveCameraToBoardLocationAction, moveToolToBoardLocationAction);
		boardLocationSelectionActionGroup.setEnabled(false);

		placementSelectionActionGroup = new ActionGroup(removePlacementAction,
				captureCameraPlacementLocation, captureToolPlacementLocation,
				twoPointLocateBoardLocationActionA,
				twoPointLocateBoardLocationActionB);
		placementSelectionActionGroup.setEnabled(false);

		boardLocationsTableModel = new BoardLocationsTableModel(configuration);
		placementsTableModel = new PlacementsTableModel(configuration);

		JComboBox sidesComboBox = new JComboBox(Side.values());

		placementsTable = new AutoSelectTextTable(placementsTableModel);
		placementsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		placementsTable.getColumnModel().getColumn(2)
				.setCellEditor(new DefaultCellEditor(sidesComboBox));

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

		boardLocationsTable = new AutoSelectTextTable(boardLocationsTableModel);
		boardLocationsTable
				.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		boardLocationsTable.getColumnModel().getColumn(1)
				.setCellEditor(new DefaultCellEditor(sidesComboBox));

		boardLocationsTable.getSelectionModel().addListSelectionListener(
				new ListSelectionListener() {
					@Override
					public void valueChanged(ListSelectionEvent e) {
						if (e.getValueIsAdjusting()) {
							return;
						}
						BoardLocation boardLocation = getSelectedBoardLocation();
						boardLocationSelectionActionGroup
								.setEnabled(boardLocation != null);
						if (boardLocation == null) {
							placementsTableModel.setBoard(null);
						}
						else {
							placementsTableModel.setBoard(boardLocation
									.getBoard());
						}
						boardLocationA = boardLocationB = null;
						twoPointLocateBoardLocationAction
								.setEnabled(boardLocationA != null
										&& boardLocationB != null);
					}
				});

		setLayout(new BorderLayout(0, 0));

		splitPane = new JSplitPane();
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

		JPanel pnlBoards = new JPanel();
		pnlBoards.setLayout(new BorderLayout(0, 0));

		JToolBar toolBarBoards = new JToolBar();
		toolBarBoards.setFloatable(false);
		pnlBoards.add(toolBarBoards, BorderLayout.NORTH);

		JButton btnStartPauseResumeJob = new JButton(startPauseResumeJobAction);
		btnStartPauseResumeJob.setHideActionText(true);
		toolBarBoards.add(btnStartPauseResumeJob);
		JButton btnStepJob = new JButton(stepJobAction);
		btnStepJob.setHideActionText(true);
		toolBarBoards.add(btnStepJob);
		JButton btnStopJob = new JButton(stopJobAction);
		btnStopJob.setHideActionText(true);
		toolBarBoards.add(btnStopJob);
		toolBarBoards.addSeparator();
		JButton btnNewBoard = new JButton(newBoardAction);
		btnNewBoard.setHideActionText(true);
		toolBarBoards.add(btnNewBoard);
		JButton btnAddBoard = new JButton(addBoardAction);
		btnAddBoard.setHideActionText(true);
		toolBarBoards.add(btnAddBoard);
		JButton btnRemoveBoard = new JButton(removeBoardAction);
		btnRemoveBoard.setHideActionText(true);
		toolBarBoards.add(btnRemoveBoard);
		toolBarBoards.addSeparator();
		JButton btnCaptureCameraBoardLocation = new JButton(
				captureCameraBoardLocationAction);
		btnCaptureCameraBoardLocation.setHideActionText(true);
		toolBarBoards.add(btnCaptureCameraBoardLocation);

		JButton btnCaptureToolBoardLocation = new JButton(
				captureToolBoardLocationAction);
		btnCaptureToolBoardLocation.setHideActionText(true);
		toolBarBoards.add(btnCaptureToolBoardLocation);

		JButton btnPositionCameraBoardLocation = new JButton(
				moveCameraToBoardLocationAction);
		btnPositionCameraBoardLocation.setHideActionText(true);
		toolBarBoards.add(btnPositionCameraBoardLocation);

		JButton btnPositionToolBoardLocation = new JButton(
				moveToolToBoardLocationAction);
		btnPositionToolBoardLocation.setHideActionText(true);
		toolBarBoards.add(btnPositionToolBoardLocation);

		pnlBoards.add(new JScrollPane(boardLocationsTable));
		JPanel pnlPlacements = new JPanel();
		pnlPlacements.setLayout(new BorderLayout(0, 0));

		JToolBar toolBarPlacements = new JToolBar();
		toolBarPlacements.setFloatable(false);
		pnlPlacements.add(toolBarPlacements, BorderLayout.NORTH);
		JButton btnNewPlacement = new JButton(newPlacementAction);
		btnNewPlacement.setHideActionText(true);
		toolBarPlacements.add(btnNewPlacement);
		JButton btnRemovePlacement = new JButton(removePlacementAction);
		btnRemovePlacement.setHideActionText(true);
		toolBarPlacements.add(btnRemovePlacement);
		toolBarPlacements.addSeparator();
		JButton btnCaptureCameraPlacementLocation = new JButton(
				captureCameraPlacementLocation);
		btnCaptureCameraPlacementLocation.setToolTipText("");
		btnCaptureCameraPlacementLocation.setText("");
		btnCaptureCameraPlacementLocation.setHideActionText(true);
		toolBarPlacements.add(btnCaptureCameraPlacementLocation);

		JButton btnCaptureToolPlacementLocation = new JButton(
				captureToolPlacementLocation);
		btnCaptureToolPlacementLocation.setToolTipText("");
		btnCaptureToolPlacementLocation.setText("");
		btnCaptureToolPlacementLocation.setHideActionText(true);
		toolBarPlacements.add(btnCaptureToolPlacementLocation);

		toolBarPlacements.addSeparator();

		JButton btnTwoPointBoardLocationA = new JButton(
				twoPointLocateBoardLocationActionA);
		toolBarPlacements.add(btnTwoPointBoardLocationA);

		JButton btnTwoPointBoardLocationB = new JButton(
				twoPointLocateBoardLocationActionB);
		toolBarPlacements.add(btnTwoPointBoardLocationB);

		JButton btnTwoPointBoardLocation = new JButton(
				twoPointLocateBoardLocationAction);
		btnTwoPointBoardLocation.setEnabled(false);
		toolBarPlacements.add(btnTwoPointBoardLocation);
		pnlPlacements.add(new JScrollPane(placementsTable));

		splitPane.setLeftComponent(pnlBoards);
		splitPane.setRightComponent(pnlPlacements);

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
			return JobPanel.this.jobProcessor.getJob().getBoardLocations()
					.get(index);
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
			return getSelectedBoardLocation().getBoard().getPlacements()
					.get(index);
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
			String name = (job.getFile() == null ? UNTITLED_JOB_FILENAME : job
					.getFile().getName());
			int result = JOptionPane.showConfirmDialog(frame,
					"Do you want to save your changes to " + name + "?" + "\n"
							+ "If you don't save, your changes will be lost.",
					"Save " + name + "?", JOptionPane.YES_NO_CANCEL_OPTION);
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
				configuration.saveJob(jobProcessor.getJob(), jobProcessor
						.getJob().getFile());
				return true;
			}
			catch (Exception e) {
				MessageBoxes.errorBox(frame, "Job Save Error", e.getMessage());
				return false;
			}
		}
	}

	private boolean saveJobAs() {
		FileDialog fileDialog = new FileDialog(frame, "Save Job As...",
				FileDialog.SAVE);
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
			File file = new File(new File(fileDialog.getDirectory()), filename);
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
			startPauseResumeJobAction.putValue(
					AbstractAction.SMALL_ICON,
					new ImageIcon(JobPanel.class
							.getResource("/icons/start.png")));
			startPauseResumeJobAction.putValue(
					AbstractAction.SHORT_DESCRIPTION,
					"Start processing the job.");
			stopJobAction.setEnabled(false);
			stepJobAction.setEnabled(true);
		}
		else if (state == JobState.Running) {
			startPauseResumeJobAction.setEnabled(true);
			startPauseResumeJobAction.putValue(AbstractAction.NAME, "Pause");
			startPauseResumeJobAction.putValue(
					AbstractAction.SMALL_ICON,
					new ImageIcon(JobPanel.class
							.getResource("/icons/pause.png")));
			startPauseResumeJobAction.putValue(
					AbstractAction.SHORT_DESCRIPTION,
					"Pause processing of the job.");
			stopJobAction.setEnabled(true);
			stepJobAction.setEnabled(false);
		}
		else if (state == JobState.Paused) {
			startPauseResumeJobAction.setEnabled(true);
			startPauseResumeJobAction.putValue(AbstractAction.NAME, "Resume");
			startPauseResumeJobAction.putValue(
					AbstractAction.SMALL_ICON,
					new ImageIcon(JobPanel.class
							.getResource("/icons/start.png")));
			startPauseResumeJobAction.putValue(
					AbstractAction.SHORT_DESCRIPTION,
					"Resume processing of the job.");
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
				(job.getFile() == null ? UNTITLED_JOB_FILENAME : job.getFile()
						.getName()));
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

	public Action startPauseResumeJobAction = new AbstractAction() {
		{
			putValue(
					SMALL_ICON,
					new ImageIcon(JobPanel.class
							.getResource("/icons/start.png")));
			putValue(NAME, "Start");
			putValue(SHORT_DESCRIPTION, "Start processing the job.");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			JobState state = jobProcessor.getState();
			if (state == JobState.Stopped) {
				try {
					jobProcessor.start();
				}
				catch (Exception e) {
					MessageBoxes.errorBox(frame, "Job Start Error",
							e.getMessage());
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

	public Action stepJobAction = new AbstractAction() {
		{
			putValue(
					SMALL_ICON,
					new ImageIcon(JobPanel.class.getResource("/icons/step.png")));
			putValue(NAME, "Step");
			putValue(SHORT_DESCRIPTION,
					"Process one step of the job and pause.");
		}

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

	public Action stopJobAction = new AbstractAction() {
		{
			putValue(
					SMALL_ICON,
					new ImageIcon(JobPanel.class.getResource("/icons/stop.png")));
			putValue(NAME, "Stop");
			putValue(SHORT_DESCRIPTION, "Stop processing the job.");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			jobProcessor.stop();
		}
	};

	public Action newBoardAction = new AbstractAction() {
		{
			putValue(SMALL_ICON,
					new ImageIcon(JobPanel.class.getResource("/icons/new.png")));
			putValue(NAME, "New Board...");
			putValue(SHORT_DESCRIPTION,
					"Create a new board and add it to the job.");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			FileDialog fileDialog = new FileDialog(frame,
					"Save New Board As...", FileDialog.SAVE);
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
				MessageBoxes.errorBox(frame, "Unable to create new board",
						e.getMessage());
			}
		}
	};

	public Action addBoardAction = new AbstractAction() {
		{
			putValue(SMALL_ICON,
					new ImageIcon(JobPanel.class.getResource("/icons/add.png")));
			putValue(NAME, "Add Board...");
			putValue(SHORT_DESCRIPTION, "Add an existing board to the job.");
		}

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
				MessageBoxes.errorBox(frame, "Board load failed",
						e.getMessage());
			}
		}
	};

	public Action removeBoardAction = new AbstractAction("Remove Board") {
		{
			putValue(
					SMALL_ICON,
					new ImageIcon(JobPanel.class
							.getResource("/icons/delete.png")));
			putValue(NAME, "Remove Board");
			putValue(SHORT_DESCRIPTION,
					"Remove the selected board from the job.");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			int index = boardLocationsTable.getSelectedRow();
			if (index != -1) {
				index = boardLocationsTable.convertRowIndexToModel(index);
				BoardLocation boardLocation = JobPanel.this.jobProcessor
						.getJob().getBoardLocations().get(index);
				JobPanel.this.jobProcessor.getJob().removeBoardLocation(
						boardLocation);
				boardLocationsTableModel.fireTableDataChanged();
			}
		}
	};

	public Action captureCameraBoardLocationAction = new AbstractAction() {
		{
			putValue(
					SMALL_ICON,
					new ImageIcon(JobPanel.class
							.getResource("/icons/capture-camera.png")));
			putValue(NAME, "Capture Camera Location");
			putValue(SHORT_DESCRIPTION,
					"Set the board's location to the camera's current position.");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			getSelectedBoardLocation().setLocation(
					machineControlsPanel.getCameraLocation());
			boardLocationsTableModel.fireTableRowsUpdated(
					boardLocationsTable.getSelectedRow(),
					boardLocationsTable.getSelectedRow());
		}
	};

	public Action captureToolBoardLocationAction = new AbstractAction() {
		{
			putValue(
					SMALL_ICON,
					new ImageIcon(JobPanel.class
							.getResource("/icons/capture-tool.png")));
			putValue(NAME, "Capture Tool Location");
			putValue(SHORT_DESCRIPTION,
					"Set the board's location to the tool's current position.");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			getSelectedBoardLocation().setLocation(
					machineControlsPanel.getToolLocation());
			boardLocationsTableModel.fireTableRowsUpdated(
					boardLocationsTable.getSelectedRow(),
					boardLocationsTable.getSelectedRow());
		}
	};

	public Action moveCameraToBoardLocationAction = new AbstractAction(
			"Move Camera To Board Location") {
		{
			putValue(
					SMALL_ICON,
					new ImageIcon(JobPanel.class
							.getResource("/icons/center-camera.png")));
			putValue(NAME, "Move Camera To Board Location");
			putValue(SHORT_DESCRIPTION,
					"Position the camera at the board's location.");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			MainFrame.machineControlsPanel.submitMachineTask(new Runnable() {
				public void run() {
					Head head = configuration.getMachine().getHeads().get(0);
					try {
						Camera camera = MainFrame.cameraPanel
								.getSelectedCamera();
						Location location = getSelectedBoardLocation()
								.getLocation();
						location = location.convertToUnits(configuration
								.getMachine().getNativeUnits());
						location = location.subtract(camera.getLocation());
						head.moveToSafeZ();
						// Move the head to the location at Safe-Z
						head.moveTo(location.getX(), location.getY(),
								head.getZ(), location.getRotation());
						// Move Z
						head.moveTo(head.getX(), head.getY(), location.getZ(),
								head.getC());
					}
					catch (Exception e) {
						MessageBoxes.errorBox(getTopLevelAncestor(),
								"Move Error", e);
					}
				}
			});
		}
	};

	public Action moveToolToBoardLocationAction = new AbstractAction() {
		{
			putValue(
					SMALL_ICON,
					new ImageIcon(JobPanel.class
							.getResource("/icons/center-tool.png")));
			putValue(NAME, "Move Tool To Board Location");
			putValue(SHORT_DESCRIPTION,
					"Position the tool at the board's location.");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			MainFrame.machineControlsPanel.submitMachineTask(new Runnable() {
				public void run() {
					Head head = configuration.getMachine().getHeads().get(0);
					try {
						Location location = getSelectedBoardLocation()
								.getLocation();
						location = location.convertToUnits(configuration
								.getMachine().getNativeUnits());
						head.moveToSafeZ();
						// Move the head to the location at Safe-Z
						head.moveTo(location.getX(), location.getY(),
								head.getZ(), location.getRotation());
						// Move Z
						head.moveTo(head.getX(), head.getY(), location.getZ(),
								head.getC());
					}
					catch (Exception e) {
						MessageBoxes.errorBox(getTopLevelAncestor(),
								"Move Error", e);
					}
				}
			});
		}
	};

	public Action twoPointLocateBoardLocationAction = new AbstractAction() {
		{
			putValue(
					SMALL_ICON,
					new ImageIcon(JobPanel.class
							.getResource("/icons/two-point-locate.png")));
			putValue(NAME, "Finish");
			putValue(SHORT_DESCRIPTION,
					"Set the board's location and rotation using two points.");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			// Get the Locations we'll be using and convert to system units.
			Location placementLocationA = new Location(LengthUnit.Millimeters,
					0, 2.13, 0, 0);
			Location boardLocationA = new Location(LengthUnit.Millimeters,
					11.464044, 8.979940, 0, 0);
			Location placementLocationB = new Location(LengthUnit.Millimeters,
					2.8, 0, 0, 0);
			Location boardLocationB = new Location(LengthUnit.Millimeters,
					12.324044, 5.619940, 0, 0);

			logger.debug(String.format("locate"));
			logger.debug(String.format("%s - %s", boardLocationA,
					placementLocationA));
			logger.debug(String.format("%s - %s", boardLocationB,
					placementLocationB));

			// Calculate the expected angle between the two coordinates, based
			// on their locations in the placement.
			double x1 = placementLocationA.getX();
			double y1 = placementLocationA.getY();
			double x2 = placementLocationB.getX();
			double y2 = placementLocationB.getY();
			double expectedAngle = Math.atan2(y1 - y2, x1 - x2);
			expectedAngle = Math.toDegrees(expectedAngle);
			logger.debug("expectedAngle " + expectedAngle);

			// Then calculate the actual angle between the two coordinates,
			// based on the captured values.
			x1 = boardLocationA.getX();
			y1 = boardLocationA.getY();
			x2 = boardLocationB.getX();
			y2 = boardLocationB.getY();
			double indicatedAngle = Math.atan2(y1 - y2, x1 - x2);
			indicatedAngle = Math.toDegrees(indicatedAngle);
			logger.debug("indicatedAngle " + indicatedAngle);

			// Subtract the difference and we have our angle.
			double angle = indicatedAngle - expectedAngle;
			logger.debug("angle " + angle);

			// Circle intersection solver stolen from
			// http://www.vb-helper.com/howto_circle_circle_intersection.html

			double cx0 = boardLocationA.getX();
			double cy0 = boardLocationA.getY();
			double radius0 = Math.sqrt(Math.pow(placementLocationA.getX(), 2)
					+ Math.pow(placementLocationA.getY(), 2));

			double cx1 = boardLocationB.getX();
			double cy1 = boardLocationB.getY();
			double radius1 = Math.sqrt(Math.pow(placementLocationB.getX(), 2)
					+ Math.pow(placementLocationB.getY(), 2));

			logger.debug(String.format("%f %f %f %f %f %f", cx0, cy0, radius0,
					cx1, cy1, radius1));

			double dx = cx0 - cx1;
			double dy = cy0 - cy1;
			double dist = Math.sqrt(dx * dx + dy * dy);

			double a = (radius0 * radius0 - radius1 * radius1 + dist * dist)
					/ (2 * dist);
			double h = Math.sqrt(radius0 * radius0 - a * a);

			double cx2 = cx0 + a * (cx1 - cx0) / dist;
			double cy2 = cy0 + a * (cy1 - cy0) / dist;

			double intersectionx1 = cx2 + h * (cy1 - cy0) / dist;
			double intersectiony1 = cy2 - h * (cx1 - cx0) / dist;
			double intersectionx2 = cx2 - h * (cy1 - cy0) / dist;
			double intersectiony2 = cy2 + h * (cx1 - cx0) / dist;

			Point p0 = new Point(intersectionx1, intersectiony1);
			Point p1 = new Point(intersectionx2, intersectiony2);

			logger.debug(String.format("p0 = %s, p1 = %s", p0, p1));

			// Create two points based on the boardLocationA.
			Point p0r = new Point(boardLocationA.getX(), boardLocationA.getY());
			Point p1r = new Point(boardLocationA.getX(), boardLocationA.getY());

			// Translate each point by one of the results from the circle
			// intersection
			p0r = Utils2D.translatePoint(p0r, p0.getX() * -1, p0.getY() * -1);
			p1r = Utils2D.translatePoint(p1r, p1.getX() * -1, p1.getY() * -1);

			// Rotate each point by the negative of the angle previously
			// calculated. This effectively de-rotates the point with one of the
			// results as the origin.
			p0r = Utils2D.rotatePoint(p0r, angle * -1);
			p1r = Utils2D.rotatePoint(p1r, angle * -1);

			logger.debug(String.format("p0r = %s, p1r = %s", p0r, p1r));

			// Now, whichever result is closer to the value of boardLocationA
			// is the right result. So, calculate the linear distance between
			// the calculated point and the placementLocationA.
			double d0 = Math.abs(Math.sqrt(Math.pow(
					p0r.x - placementLocationA.getX(), 2)
					+ Math.pow(p0r.y - placementLocationA.getY(), 2)));
			double d1 = Math.abs(Math.sqrt(Math.pow(
					p1r.x - placementLocationA.getX(), 2)
					+ Math.pow(p1r.y - placementLocationA.getY(), 2)));

			logger.debug(String.format("d0 %f, d1 %f", d0, d1));

			Point result = ((d0 < d1) ? p0 : p1);

			logger.debug("Result: " + result);

			Location boardLocation = new Location(Configuration.get()
					.getSystemUnits(), result.x, result.y, 0, angle * -1);

			getSelectedBoardLocation().setLocation(boardLocation);
			boardLocationsTableModel.fireTableRowsUpdated(
					boardLocationsTable.getSelectedRow(),
					boardLocationsTable.getSelectedRow());
		}
	};

	public Action twoPointLocateBoardLocationActionA = new AbstractAction() {
		{
			putValue(
					SMALL_ICON,
					new ImageIcon(JobPanel.class
							.getResource("/icons/two-point-locate.png")));
			putValue(NAME, "A");
			putValue(SHORT_DESCRIPTION,
					"Set the location of the first placement to locate the board.");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			boardLocationA = MainFrame.machineControlsPanel.getCameraLocation();
			boardLocationPlacementA = getSelectedPlacement();
			twoPointLocateBoardLocationAction.setEnabled(boardLocationA != null
					&& boardLocationB != null);
		}
	};

	public Action twoPointLocateBoardLocationActionB = new AbstractAction() {
		{
			putValue(
					SMALL_ICON,
					new ImageIcon(JobPanel.class
							.getResource("/icons/two-point-locate.png")));
			putValue(NAME, "B");
			putValue(SHORT_DESCRIPTION,
					"Set the location of the second placement to locate the board.");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			boardLocationB = MainFrame.machineControlsPanel.getCameraLocation();
			boardLocationPlacementB = getSelectedPlacement();
			twoPointLocateBoardLocationAction.setEnabled(boardLocationA != null
					&& boardLocationB != null);
		}
	};

	public Action newPlacementAction = new AbstractAction() {
		{
			putValue(SMALL_ICON,
					new ImageIcon(JobPanel.class.getResource("/icons/new.png")));
			putValue(NAME, "New Placement");
			putValue(SHORT_DESCRIPTION,
					"Create a new placement and add it to the board.");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			BoardLocation boardLocation = getSelectedBoardLocation();
			String id = JOptionPane.showInputDialog(frame,
					"Please enter an ID for the new placement.");
			if (id == null) {
				return;
			}
			// TODO: Make sure it's unique.
			Placement placement = new Placement(id);
			boardLocation.getBoard().addPlacement(placement);
			placementsTableModel.fireTableDataChanged();
		}
	};

	public Action removePlacementAction = new AbstractAction() {
		{
			putValue(
					SMALL_ICON,
					new ImageIcon(JobPanel.class
							.getResource("/icons/delete.png")));
			putValue(NAME, "Remove Placement");
			putValue(SHORT_DESCRIPTION,
					"Remove the currently selected placement.");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			BoardLocation boardLocation = getSelectedBoardLocation();
			Placement placement = getSelectedPlacement();
			boardLocation.getBoard().removePlacement(placement);
			placementsTableModel.fireTableDataChanged();
		}
	};

	public Action captureCameraPlacementLocation = new AbstractAction() {
		{
			putValue(
					SMALL_ICON,
					new ImageIcon(JobPanel.class
							.getResource("/icons/capture-camera.png")));
			putValue(NAME, "Capture Camera Placement Location");
			putValue(SHORT_DESCRIPTION,
					"Set the placement's location to the camera's current position.");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			// Location boardLocation =
			// getSelectedBoardLocation().getLocation();
			// getSelectedPlacement().setLocation(machineControlsPanel.getCameraLocation().subtract(boardLocation));
			// placementsTableModel.fireTableRowsUpdated(placementsTable.getSelectedRow(),
			// placementsTable.getSelectedRow());
			MessageBoxes.errorBox(getTopLevelAncestor(), "Not Yet Implemented",
					"This action is not yet implemented.");
		}
	};

	public Action captureToolPlacementLocation = new AbstractAction() {
		{
			putValue(
					SMALL_ICON,
					new ImageIcon(JobPanel.class
							.getResource("/icons/capture-tool.png")));
			putValue(NAME, "Capture Tool Placement Location");
			putValue(SHORT_DESCRIPTION,
					"Set the placement's location to the tool's current position.");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			// Location boardLocation =
			// getSelectedBoardLocation().getLocation();
			// getSelectedPlacement().setLocation(machineControlsPanel.getToolLocation().subtract(boardLocation));
			// placementsTableModel.fireTableRowsUpdated(placementsTable.getSelectedRow(),
			// placementsTable.getSelectedRow());
			MessageBoxes.errorBox(getTopLevelAncestor(), "Not Yet Implemented",
					"This action is not yet implemented.");
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
			MessageBoxes.errorBox(frame, error.toString(), description
					+ "\n\nThe job will be paused.");
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
