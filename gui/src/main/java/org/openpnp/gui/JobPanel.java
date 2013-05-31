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
import java.awt.Color;
import java.awt.FileDialog;
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
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
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
import org.openpnp.gui.importer.BoardImporter;
import org.openpnp.gui.importer.MountsmdUlpImporter;
import org.openpnp.gui.importer.MountsmdPosImporter;
import org.openpnp.gui.processes.TwoPlacementBoardLocationProcess;
import org.openpnp.gui.support.ActionGroup;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.IdentifiableListCellRenderer;
import org.openpnp.gui.support.IdentifiableTableCellRenderer;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.PartsComboBoxModel;
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
import org.openpnp.spi.Machine;
import org.openpnp.spi.MachineListener;
import org.openpnp.spi.Nozzle;
import org.openpnp.util.MovableUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class JobPanel extends JPanel {
	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory
			.getLogger(JobPanel.class);

	final private Configuration configuration;
	final private JobProcessor jobProcessor;
	final private MainFrame frame;
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

	public JobPanel(Configuration configuration, JobProcessor jobProcessor,
			MainFrame frame, MachineControlsPanel machineControlsPanel) {
		this.configuration = configuration;
		this.jobProcessor = jobProcessor;
		this.frame = frame;
		this.machineControlsPanel = machineControlsPanel;

		jobSaveActionGroup = new ActionGroup(saveJobAction, saveJobAsAction);
		jobSaveActionGroup.setEnabled(false);

		boardLocationSelectionActionGroup = new ActionGroup(removeBoardAction,
				captureCameraBoardLocationAction,
				captureToolBoardLocationAction, newPlacementAction,
				moveCameraToBoardLocationAction, moveToolToBoardLocationAction,
				twoPointLocateBoardLocationAction);
		boardLocationSelectionActionGroup.setEnabled(false);

		placementSelectionActionGroup = new ActionGroup(removePlacementAction,
				captureCameraPlacementLocation, captureToolPlacementLocation);
		placementSelectionActionGroup.setEnabled(false);

		boardLocationsTableModel = new BoardLocationsTableModel(configuration);
		placementsTableModel = new PlacementsTableModel(configuration);

		JComboBox sidesComboBox = new JComboBox(Side.values());

		boardLocationsTable = new AutoSelectTextTable(boardLocationsTableModel);
		boardLocationsTable.setAutoCreateRowSorter(true);
		boardLocationsTable
				.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		boardLocationsTable.setDefaultEditor(Side.class, new DefaultCellEditor(
				sidesComboBox));

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
					}
				});

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
		pnlBoards.setBorder(new TitledBorder(new EtchedBorder(
				EtchedBorder.LOWERED, null, null), "Boards",
				TitledBorder.LEADING, TitledBorder.TOP, null,
				new Color(0, 0, 0)));
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
		toolBarBoards.addSeparator();

		JButton btnTwoPointBoardLocation = new JButton(
				twoPointLocateBoardLocationAction);
		toolBarBoards.add(btnTwoPointBoardLocation);
		btnTwoPointBoardLocation.setHideActionText(true);

		pnlBoards.add(new JScrollPane(boardLocationsTable));
		JPanel pnlPlacements = new JPanel();
		pnlPlacements.setBorder(new TitledBorder(null, "Placements",
				TitledBorder.LEADING, TitledBorder.TOP, null, null));
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
		pnlPlacements.add(new JScrollPane(placementsTable));

		splitPane.setLeftComponent(pnlBoards);
		splitPane.setRightComponent(pnlPlacements);

		add(splitPane);

		jobProcessor.addListener(jobProcessorListener);
		jobProcessor.setDelegate(jobProcessorDelegate);

        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            public void configurationComplete(Configuration configuration) throws Exception {
                configuration.getMachine().addListener(machineListener);
                updateJobActions();

                // Create an empty Job if one is not loaded
                if (JobPanel.this.jobProcessor.getJob() == null) {
                    Job job = new Job();
                    JobPanel.this.jobProcessor.load(job);
                }
            }
        });
	}

	public void refreshSelectedBoardRow() {
		boardLocationsTableModel.fireTableRowsUpdated(
				boardLocationsTable.getSelectedRow(),
				boardLocationsTable.getSelectedRow());
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

	public final Action openJobAction = new AbstractAction("Open Job...") {
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
				if (fileDialog.getFile() == null) {
					return;
				}
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

	public final Action newJobAction = new AbstractAction("New Job") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (!checkForModifications()) {
				return;
			}
			jobProcessor.load(new Job());
		}
	};

	public final Action saveJobAction = new AbstractAction("Save Job") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			saveJob();
		}
	};

	public final Action saveJobAsAction = new AbstractAction("Save Job As...") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			saveJobAs();
		}
	};

	public final Action startPauseResumeJobAction = new AbstractAction() {
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

	public final Action stepJobAction = new AbstractAction() {
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

	public final Action stopJobAction = new AbstractAction() {
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

	public final Action newBoardAction = new AbstractAction() {
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

				Helpers.selectLastTableRow(boardLocationsTable);
			}
			catch (Exception e) {
				e.printStackTrace();
				MessageBoxes.errorBox(frame, "Unable to create new board",
						e.getMessage());
			}
		}
	};

	public final Action addBoardAction = new AbstractAction() {
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
				// TODO: Move to a list property listener.
				boardLocationsTableModel.fireTableDataChanged();

				Helpers.selectLastTableRow(boardLocationsTable);
			}
			catch (Exception e) {
				e.printStackTrace();
				MessageBoxes.errorBox(frame, "Board load failed",
						e.getMessage());
			}
		}
	};

	public final Action removeBoardAction = new AbstractAction("Remove Board") {
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

	public final Action captureCameraBoardLocationAction = new AbstractAction() {
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
			getSelectedBoardLocation().setLocation(MainFrame.cameraPanel.getSelectedCameraLocation());
			boardLocationsTableModel.fireTableRowsUpdated(
					boardLocationsTable.getSelectedRow(),
					boardLocationsTable.getSelectedRow());
		}
	};

	public final Action captureToolBoardLocationAction = new AbstractAction() {
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
			getSelectedBoardLocation().setLocation(machineControlsPanel.getSelectedNozzle().getLocation());
			boardLocationsTableModel.fireTableRowsUpdated(
					boardLocationsTable.getSelectedRow(),
					boardLocationsTable.getSelectedRow());
		}
	};

	public final Action moveCameraToBoardLocationAction = new AbstractAction(
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
            final Camera camera = MainFrame.cameraPanel.getSelectedCamera();
            if (camera.getHead() == null) {
                MessageBoxes.errorBox(getTopLevelAncestor(), "Move Error", "Camera is not movable.");
                return;
            }
            final Location location = getSelectedBoardLocation().getLocation();
			MainFrame.machineControlsPanel.submitMachineTask(new Runnable() {
				public void run() {
					try {
						MovableUtils.moveToLocationAtSafeZ(camera, location, 1.0);
					}
					catch (Exception e) {
						MessageBoxes.errorBox(getTopLevelAncestor(),
								"Move Error", e);
					}
				}
			});
		}
	};

	public final Action moveToolToBoardLocationAction = new AbstractAction() {
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
                final Nozzle nozzle = machineControlsPanel.getSelectedNozzle();
                final Location location = getSelectedBoardLocation().getLocation();
				public void run() {
					try {
						MovableUtils.moveToLocationAtSafeZ(nozzle, location, 1.0);
					}
					catch (Exception e) {
						MessageBoxes.errorBox(getTopLevelAncestor(),
								"Move Error", e);
					}
				}
			});
		}
	};

	public final Action twoPointLocateBoardLocationAction = new AbstractAction() {
		{
			putValue(
					SMALL_ICON,
					new ImageIcon(JobPanel.class
							.getResource("/icons/two-point-locate.png")));
			putValue(NAME, "Two Point Board Location");
			putValue(SHORT_DESCRIPTION,
					"Set the board's location and rotation using two placements.");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			new TwoPlacementBoardLocationProcess(frame, JobPanel.this);
		}
	};

	public final Action newPlacementAction = new AbstractAction() {
		{
			putValue(SMALL_ICON,
					new ImageIcon(JobPanel.class.getResource("/icons/new.png")));
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

			BoardLocation boardLocation = getSelectedBoardLocation();
			String id = JOptionPane.showInputDialog(frame,
					"Please enter an ID for the new placement.");
			if (id == null) {
				return;
			}
			// TODO: Make sure it's unique.
			Placement placement = new Placement(id);

			placement.setPart(Configuration.get().getParts().get(0));
			placement.setLocation(new Location(Configuration.get().getSystemUnits()));
			
			boardLocation.getBoard().addPlacement(placement);
			placementsTableModel.fireTableDataChanged();
			Helpers.selectLastTableRow(placementsTable);
		}
	};

	public final Action removePlacementAction = new AbstractAction() {
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

	public final Action captureCameraPlacementLocation = new AbstractAction() {
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
			MessageBoxes.notYetImplemented(getTopLevelAncestor());
		}
	};

	public final Action captureToolPlacementLocation = new AbstractAction() {
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
			MessageBoxes.notYetImplemented(getTopLevelAncestor());
		}
	};
	
	public final Action importMountsmdUlpAction = new AbstractAction("EAGLE mountsmd.ulp") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (getSelectedBoardLocation() == null) {
				return;
			}
			BoardImporter importer = new MountsmdUlpImporter(JOptionPane.getFrameForComponent(JobPanel.this));
			try {
				Board importedBoard = importer.importBoard();
				Board existingBoard = getSelectedBoardLocation().getBoard();
				for (Placement placement : importedBoard.getPlacements()) {
					existingBoard.addPlacement(placement);
				}
				placementsTableModel.fireTableDataChanged();
			}
			catch (Exception e) {
				MessageBoxes.errorBox(getTopLevelAncestor(), "Import Failed", e);
			}
		}
	};
	
	public final Action importMountsmdPosAction = new AbstractAction("KiCAD  mountsmd.pos") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (getSelectedBoardLocation() == null) {
				return;
			}
			BoardImporter importer = new MountsmdPosImporter(JOptionPane.getFrameForComponent(JobPanel.this));
			try {
				Board importedBoard = importer.importBoard();
				Board existingBoard = getSelectedBoardLocation().getBoard();
				for (Placement placement : importedBoard.getPlacements()) {
					existingBoard.addPlacement(placement);
				}
				placementsTableModel.fireTableDataChanged();
			}
			catch (Exception e) {
				MessageBoxes.errorBox(getTopLevelAncestor(), "Import Failed", e);
			}
		}
	};
	
	private final JobProcessorListener jobProcessorListener = new JobProcessorListener.Adapter() {
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

	private final JobProcessorDelegate jobProcessorDelegate = new JobProcessorDelegate() {
		@Override
		public PickRetryAction partPickFailed(BoardLocation board, Part part,
				Feeder feeder) {
			return PickRetryAction.SkipAndContinue;
		}
	};

	private final MachineListener machineListener = new MachineListener.Adapter() {
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

	private final PropertyChangeListener titlePropertyChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			updateTitle();
			jobSaveActionGroup.setEnabled(jobProcessor.getJob().isDirty());
		}
	};
}
