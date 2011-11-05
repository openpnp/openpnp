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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;

import org.openpnp.Board;
import org.openpnp.Configuration;
import org.openpnp.Job;
import org.openpnp.Job.JobBoard;
import org.openpnp.JobProcessor;
import org.openpnp.JobProcessor.JobError;
import org.openpnp.JobProcessor.JobState;
import org.openpnp.JobProcessor.PickRetryAction;
import org.openpnp.JobProcessorDelegate;
import org.openpnp.JobProcessorListener;
import org.openpnp.Location;
import org.openpnp.Part;
import org.openpnp.Placement;
import org.openpnp.gui.components.CameraPanel;
import org.openpnp.gui.components.MachineControlsPanel;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Feeder;

/**
 * The main window of the application. Implements the top level menu, Job run
 * and Job setup.
 */
@SuppressWarnings("serial")
public class MainFrame extends JFrame implements JobProcessorListener,
		JobProcessorDelegate {
	/*
	 * TODO define accelerators and mnemonics
	 * openJobMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
	 * Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
	 */
	private Configuration configuration;
	private JobProcessor jobProcessor;
	private BoardsTableModel boardsTableModel;
	private PartsTableModel partsTableModel;

	private JPanel contentPane;
	private MachineControlsPanel machineControlsPanel;
	private CameraPanel cameraPanel;
	private JTable boardsTable;
	private JTable partsTable;
	private JLabel lblStatus;

	public MainFrame() {
		createUi();
		
		try {
			configuration = new Configuration("config");
		}
		catch (Exception e) {
			throw new Error(e);
		}

		for (Camera camera : configuration.getMachine().getCameras()) {
			cameraPanel.addCamera(camera);
		}

		jobProcessor = new JobProcessor(configuration);
		jobProcessor.addListener(this);
		jobProcessor.setDelegate(this);

		machineControlsPanel.setMachine(configuration.getMachine());
		
		lblStatus = new JLabel("...");
		lblStatus.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		contentPane.add(lblStatus, BorderLayout.SOUTH);
	}

	@Override
	public void jobStateChanged(JobState state) {
		Job job = jobProcessor.getJob();
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
	}

	private void openJob() {
		FileDialog fileDialog = new FileDialog(MainFrame.this);
		fileDialog.setVisible(true);
		try {
			File file = new File(new File(fileDialog.getDirectory()),
					fileDialog.getFile());
			jobProcessor.load(file);
		}
		catch (Exception e) {
			JOptionPane.showMessageDialog(this, e.getMessage(),
					"Job Load Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void startPauseResumeJob() {
		JobState state = jobProcessor.getState();
		if (state == JobState.Stopped) {
			try {
				jobProcessor.start();
			}
			catch (Exception e) {
				JOptionPane.showMessageDialog(this, e.getMessage(),
						"Job Start Error", JOptionPane.ERROR_MESSAGE);
			}
		}
		else if (state == JobState.Paused) {
			jobProcessor.resume();
		}
		else if (state == JobState.Running) {
			jobProcessor.pause();
		}
	}
	
	private void stepJob() {
		try {
			jobProcessor.step();
		}
		catch (Exception e) {
			JOptionPane.showMessageDialog(this, e.getMessage(),
					"Job Start Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void stopJob() {
		jobProcessor.stop();
	}

	@Override
	public void jobLoaded(Job job) {
		partsTableModel.setJob(jobProcessor.getJob());
		boardsTableModel.setJob(jobProcessor.getJob());
		startPauseResumeJobAction.setEnabled(true);
	}

	@Override
	public PickRetryAction partPickFailed(JobBoard board, Part part,
			Feeder feeder) {
		return PickRetryAction.SkipAndContinue;
	}

	@Override
	public void jobEncounteredError(JobError error, String description) {
		JOptionPane.showMessageDialog(this, description, error.toString(),
				JOptionPane.ERROR_MESSAGE);
	}

	@Override
	public void boardProcessingCompleted(JobBoard board) {
		// boardsComplete++;
	}

	@Override
	public void boardProcessingStarted(JobBoard board) {
	}

	@Override
	public void partPicked(JobBoard board, Placement placement) {
	}

	@Override
	public void partPlaced(JobBoard board, Placement placement) {
	}

	@Override
	public void partProcessingCompleted(JobBoard board, Placement placement) {
		// partsComplete++;
	}

	@Override
	public void partProcessingStarted(JobBoard board, Placement placement) {
	}
	
	@Override
	public void detailedStatusUpdated(String status) {
		lblStatus.setText(status);
	}

	private void createUi() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 1280, 1024);

		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);

		JMenuItem mntmNew = new JMenuItem("New");
		mntmNew.setAction(newJobAction);
		mnFile.add(mntmNew);

		JMenuItem mntmOpen = new JMenuItem("Open");
		mntmOpen.setAction(openJobAction);
		mnFile.add(mntmOpen);

		mnFile.addSeparator();

		JMenuItem mntmClose = new JMenuItem("Close");
		mntmClose.setAction(closeJobAction);
		mnFile.add(mntmClose);

		mnFile.addSeparator();

		JMenuItem mntmSave = new JMenuItem("Save");
		mntmSave.setAction(saveJobAction);
		mnFile.add(mntmSave);

		JMenuItem mntmSaveAs = new JMenuItem("Save As");
		mntmSaveAs.setAction(saveJobAsAction);
		mnFile.add(mntmSaveAs);

		JMenu mnEdit = new JMenu("Edit");
		menuBar.add(mnEdit);

		JMenuItem mntmAddBoard = new JMenuItem("Add Board");
		mntmAddBoard.setAction(addBoardAction);
		mnEdit.add(mntmAddBoard);

		JMenuItem mntmDeleteBoard = new JMenuItem("Delete Board");
		mntmDeleteBoard.setAction(deleteBoardAction);
		mnEdit.add(mntmDeleteBoard);

		JMenuItem mntmEnableBoard = new JMenuItem("Enable Board");
		mntmEnableBoard.setAction(enableDisableBoardAction);
		mnEdit.add(mntmEnableBoard);

		mnEdit.addSeparator();

		JMenuItem mntmMoveBoardUp = new JMenuItem("Move Board Up");
		mntmMoveBoardUp.setAction(moveBoardUpAction);
		mnEdit.add(mntmMoveBoardUp);

		JMenuItem mntmMoveBoardDown = new JMenuItem("Move Board Down");
		mntmMoveBoardDown.setAction(moveBoardDownAction);
		mnEdit.add(mntmMoveBoardDown);

		mnEdit.addSeparator();

		JMenuItem mntmSetBoardLocation = new JMenuItem("Set Board Location");
		mntmSetBoardLocation.setAction(orientBoardAction);
		mnEdit.add(mntmSetBoardLocation);

		JMenu mnJob = new JMenu("Job Control");
		menuBar.add(mnJob);

		JMenuItem mntmNewMenuItem = new JMenuItem("Start Job");
		mntmNewMenuItem.setAction(startPauseResumeJobAction);
		mnJob.add(mntmNewMenuItem);

		JMenuItem mntmStepJob = new JMenuItem("Step Job");
		mntmStepJob.setAction(stepJobAction);
		mnJob.add(mntmStepJob);

		JMenuItem mntmStopJob = new JMenuItem("Stop Job");
		mntmStopJob.setAction(stopJobAction);
		mnJob.add(mntmStopJob);

		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));
		
		JSplitPane splitPaneTopBottom = new JSplitPane();
		splitPaneTopBottom.setBorder(null);
		splitPaneTopBottom.setOrientation(JSplitPane.VERTICAL_SPLIT);
		splitPaneTopBottom.setContinuousLayout(true);
		contentPane.add(splitPaneTopBottom, BorderLayout.CENTER);
		
		JPanel panelTop = new JPanel();
		splitPaneTopBottom.setLeftComponent(panelTop);
		panelTop.setLayout(new BorderLayout(0, 0));
		
				JPanel panelLeftColumn = new JPanel();
				panelTop.add(panelLeftColumn, BorderLayout.WEST);
				FlowLayout flowLayout = (FlowLayout) panelLeftColumn.getLayout();
				flowLayout.setVgap(0);
				flowLayout.setHgap(0);
				
						JPanel panel = new JPanel();
						panelLeftColumn.add(panel);
						panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
						
								machineControlsPanel = new MachineControlsPanel();
								machineControlsPanel.setBorder(new TitledBorder(null,
										"Machine Controls", TitledBorder.LEADING, TitledBorder.TOP,
										null, null));
								panel.add(machineControlsPanel);
								
										cameraPanel = new CameraPanel();
										panelTop.add(cameraPanel, BorderLayout.CENTER);
										cameraPanel.setBorder(new TitledBorder(null, "Cameras",
												TitledBorder.LEADING, TitledBorder.TOP, null, null));
		
		JPanel panelBottom = new JPanel();
		splitPaneTopBottom.setRightComponent(panelBottom);
		panelBottom.setLayout(new BorderLayout(0, 0));

		JPanel panelJob = new JPanel();
		panelBottom.add(panelJob, BorderLayout.CENTER);
		panelJob.setBorder(new TitledBorder(null, "Job", TitledBorder.LEADING,
				TitledBorder.TOP, null, null));
		panelJob.setLayout(new BorderLayout(0, 0));

		partsTable = new JTable(partsTableModel = new PartsTableModel());
		JScrollPane partsTableScroller = new JScrollPane(partsTable);

		boardsTable = new JTable(boardsTableModel = new BoardsTableModel());
		JScrollPane boardsTableScroller = new JScrollPane(boardsTable);

		JPanel panelRight = new JPanel();
		panelRight.setLayout(new BorderLayout());
		panelRight.add(partsTableScroller);

		JPanel panelLeft = new JPanel();
		panelLeft.setLayout(new BorderLayout());
		
		JPanel panelJobControl = new JPanel();
		panelLeft.add(panelJobControl, BorderLayout.WEST);
		panelJobControl.setLayout(new BoxLayout(panelJobControl, BoxLayout.Y_AXIS));
		
		JButton btnStart = new JButton("Start");
		btnStart.setAction(startPauseResumeJobAction);
		btnStart.setPreferredSize(new Dimension(80, 80));
		btnStart.setFocusable(false);
		panelJobControl.add(btnStart);
		
		JButton btnStep = new JButton(stepJobAction);
		btnStep.setPreferredSize(new Dimension(80, 80));
		btnStep.setFocusable(false);
		panelJobControl.add(btnStep);
		
		JButton btnStop = new JButton("Stop");
		btnStop.setAction(stopJobAction);
		btnStop.setPreferredSize(new Dimension(80, 80));
		btnStop.setFocusable(false);
		panelJobControl.add(btnStop);
		
		Component glue = Box.createGlue();
		panelJobControl.add(glue);
		panelLeft.add(boardsTableScroller);

		JSplitPane splitPaneLeftRight = new JSplitPane();
		splitPaneLeftRight.setBorder(null);
		panelJob.add(splitPaneLeftRight);
		splitPaneLeftRight.setContinuousLayout(true);
		splitPaneLeftRight.setDividerLocation(350);
		splitPaneLeftRight.setLeftComponent(panelLeft);
		splitPaneLeftRight.setRightComponent(panelRight);
		splitPaneTopBottom.setDividerLocation(600);

		startPauseResumeJobAction.setEnabled(false);
		stopJobAction.setEnabled(false);
		stepJobAction.setEnabled(false);
	}

	// class PartsTableCellRenderer extends DefaultTableCellRenderer {
	// public Component getTableCellRendererComponent(JTable table,
	// Object value, boolean isSelected, boolean hasFocus, int row,
	// int col) {
	// Component comp = super.getTableCellRendererComponent(table, value,
	// isSelected, hasFocus, row, col);
	//
	// if (row % 2 == 0) {
	// comp.setForeground(Color.red);
	// }
	// else {
	// comp.setForeground(null);
	// }
	//
	// return (comp);
	// }
	// }

	class PartsTableModel extends AbstractTableModel {
		private String[] columnNames = new String[] { "Board #", "Part",
				"Package", "Feeder", "X Pos.", "Y Pos.", "Rotation" };
		private ArrayList<PartMetaData> boardPlacements = new ArrayList<PartMetaData>();

		public void setJob(Job job) {
			boardPlacements.clear();
			int boardNumber = 1;
			for (JobBoard board : job.getBoards()) {
				for (Placement placement : board.getBoard().getPlacements()) {
					boardPlacements.add(new PartMetaData(boardNumber, board
							.getBoard(), placement));
				}
				boardNumber++;
			}
			fireTableDataChanged();
		}

		@Override
		public String getColumnName(int column) {
			return columnNames[column];
		}

		public int getColumnCount() {
			return columnNames.length;
		}

		public int getRowCount() {
			return boardPlacements.size();
		}

		public Object getValueAt(int row, int col) {
			switch (col) {
			case 0:
				return boardPlacements.get(row).boardNumber;
			case 1:
				return boardPlacements.get(row).placement.getPart()
						.getReference();
			case 2:
				return boardPlacements.get(row).placement.getPart()
						.getPackage().getReference();
			case 3:
				return boardPlacements.get(row).placement.getPart()
						.getFeederLocations().get(0).getFeeder().getReference();
			case 4:
				return String
						.format("%2.3f", boardPlacements.get(row).placement
								.getLocation().getX());
			case 5:
				return String
						.format("%2.3f", boardPlacements.get(row).placement
								.getLocation().getY());
			case 6:
				return String.format("%2.3f",
						boardPlacements.get(row).placement.getLocation()
								.getRotation());
			default:
				return null;
			}
		}

		class PartMetaData {
			public int boardNumber;
			public Board board;
			public Placement placement;

			public PartMetaData(int boardNumber, Board board,
					Placement placement) {
				this.boardNumber = boardNumber;
				this.board = board;
				this.placement = placement;
			}
		}
	}

	class BoardsTableModel extends AbstractTableModel {
		private String[] columnNames = new String[] { "#", "Board", "X Pos.",
				"Y Pos.", "Rotation" };
		private Job job;

		public void setJob(Job job) {
			this.job = job;
			fireTableDataChanged();
		}

		@Override
		public String getColumnName(int column) {
			return columnNames[column];
		}

		public int getColumnCount() {
			return columnNames.length;
		}

		public int getRowCount() {
			if (job == null) {
				return 0;
			}
			return job.getBoards().size();
		}

		public Object getValueAt(int row, int col) {
			JobBoard board = job.getBoards().get(row);
			Location loc = board.getLocation();
			switch (col) {
			case 0:
				return (row + 1);
			case 1:
				return board.getBoard().getReference();
			case 2:
				return String.format("%2.3f", loc.getX());
			case 3:
				return String.format("%2.3f", loc.getY());
			case 4:
				return String.format("%2.3f", loc.getRotation());
			default:
				return null;
			}
		}
	}

	private Action stopJobAction = new AbstractAction("Stop") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			stopJob();
		}
	};

	private Action startPauseResumeJobAction = new AbstractAction("Start") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			startPauseResumeJob();
		}
	};

	private Action stepJobAction = new AbstractAction("Step") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			stepJob();
		}
	};

	private Action openJobAction = new AbstractAction("Open Job...") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			openJob();
		}
	};

	private Action closeJobAction = new AbstractAction("Close Job") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
		}
	};

	private Action newJobAction = new AbstractAction("New Job") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
		}
	};

	private Action saveJobAction = new AbstractAction("Save Job") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
		}
	};

	private Action saveJobAsAction = new AbstractAction("Save Job As...") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
		}
	};

	private Action addBoardAction = new AbstractAction("Add Board...") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
		}
	};

	private Action moveBoardUpAction = new AbstractAction("Move Board Up") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
		}
	};

	private Action moveBoardDownAction = new AbstractAction("Move Board Down") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
		}
	};

	private Action orientBoardAction = new AbstractAction("Set Board Location") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
		}
	};

	private Action deleteBoardAction = new AbstractAction("Delete Board") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
		}
	};

	private Action enableDisableBoardAction = new AbstractAction("Enable Board") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
		}
	};
}
