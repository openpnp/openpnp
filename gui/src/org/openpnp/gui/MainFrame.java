package org.openpnp.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import org.openpnp.Board;
import org.openpnp.Configuration;
import org.openpnp.Job;
import org.openpnp.JobProcessor;
import org.openpnp.JobProcessorDelegate;
import org.openpnp.JobProcessorListener;
import org.openpnp.Location;
import org.openpnp.Part;
import org.openpnp.Placement;
import org.openpnp.Job.JobBoard;
import org.openpnp.JobProcessor.JobError;
import org.openpnp.JobProcessor.JobState;
import org.openpnp.JobProcessor.PickRetryAction;
import org.openpnp.gui.components.BoardView;
import org.openpnp.gui.components.CameraPanel;
import org.openpnp.gui.support.ActionGroup;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Feeder;

/**
 * The main window of the application. Implements the top level menu, Job run and Job setup.
 */
@SuppressWarnings("serial")
public class MainFrame extends JFrame implements JobProcessorListener, JobProcessorDelegate {
	/*
	 * TODO define accelerators and mnemonics
	 * openJobMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
	 */
	private CameraPanel cameraPanel;
	private JTable boardsTable;
	private JTable partsTable;
	private BoardView boardView;
	
	private Configuration configuration;
	private JobProcessor jobProcessor;
	
	private BoardsTableModel boardsTableModel;
	private PartsTableModel partsTableModel;
	
	private int boardsTotal;
	private int boardsComplete;
	private int partsTotal;
	private int partsComplete;
	
	public MainFrame() {
		createUi();
		
		startPauseResumeJobAction.setEnabled(false);
		stopJobAction.setEnabled(false);
		
		try {
			configuration = new Configuration("/Users/jason/Projects/openpnp/trunk/gui/config");
		}
		catch (Exception e) {
			throw new Error(e);
		}
		
		for (Camera camera : configuration.getMachine().getCameras()) {
			cameraPanel.addCamera(camera, camera.getName());
		}
		
		jobProcessor = new JobProcessor(configuration);
		jobProcessor.addListener(this);
		jobProcessor.setDelegate(this);
	}
	
	@Override
	public void jobStateChanged(JobState state) {
		Job job = jobProcessor.getJob();
		if (state == JobState.Stopped) {
			startPauseResumeJobAction.setEnabled(true);
			startPauseResumeJobAction.putValue(AbstractAction.NAME, "Start");
			stopJobAction.setEnabled(false);
		}
		else if (state == JobState.Running) {
			startPauseResumeJobAction.setEnabled(true);
			startPauseResumeJobAction.putValue(AbstractAction.NAME, "Pause");
			stopJobAction.setEnabled(true);
		}
		else if (state == JobState.Paused) {
			startPauseResumeJobAction.setEnabled(true);
			startPauseResumeJobAction.putValue(AbstractAction.NAME, "Resume");
			stopJobAction.setEnabled(true);
		}
	}
	
	private void openJob() {
		FileDialog fileDialog = new FileDialog(MainFrame.this);
		fileDialog.setVisible(true);
		try {
			File file = new File(new File(fileDialog.getDirectory()), fileDialog.getFile());
			jobProcessor.load(file);
		}
		catch (Exception e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "Job Load Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private void startPauseResumeJob() {
		JobState state = jobProcessor.getState();
		if (state == JobState.Stopped) {
			try {
				jobProcessor.start();
			}
			catch (Exception e) {
				JOptionPane.showMessageDialog(this, e.getMessage(), "Job Start Error", JOptionPane.ERROR_MESSAGE);
			}
		}
		else if (state == JobState.Paused) {
			jobProcessor.resume();
		}
		else if (state == JobState.Running) {
			jobProcessor.pause();
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
		JOptionPane.showMessageDialog(this, description, error.toString(), JOptionPane.ERROR_MESSAGE);
	}

	@Override
	public void boardProcessingCompleted(JobBoard board) {
		boardsComplete++;
	}

	@Override
	public void boardProcessingStarted(JobBoard board) {
		// TODO Auto-generated method stub
	}

	@Override
	public void partPicked(JobBoard board, Placement placement) {
		// TODO Auto-generated method stub
	}

	@Override
	public void partPlaced(JobBoard board, Placement placement) {
		// TODO Auto-generated method stub
	}

	@Override
	public void partProcessingCompleted(JobBoard board, Placement placement) {
		partsComplete++;
	}

	@Override
	public void partProcessingStarted(JobBoard board, Placement placement) {
		// TODO Auto-generated method stub
	}
	
	private void createUi() {
		setSize(1280, 1024);
		setTitle("Job");
		
		cameraPanel = new CameraPanel();
		cameraPanel.setBorder(BorderFactory.createTitledBorder("Cameras"));
		cameraPanel.setPreferredSize(new Dimension(400, 300));
		
		partsTable = new JTable(partsTableModel = new PartsTableModel());
		JScrollPane partsTableScroller = new JScrollPane(partsTable);
		partsTableScroller.setPreferredSize(new Dimension(1, 300));
		
		boardsTable = new JTable(boardsTableModel = new BoardsTableModel());
		JScrollPane boardsTableScroller = new JScrollPane(boardsTable);
		boardsTableScroller.setPreferredSize(new Dimension(1, 300));
		
		JPanel partsTableScrollerWrapper = new JPanel();
		partsTableScrollerWrapper.setBorder(BorderFactory.createTitledBorder("Parts"));
		partsTableScrollerWrapper.setLayout(new BorderLayout());
		partsTableScrollerWrapper.add(partsTableScroller);
		
		JPanel boardsTableScrollerWrapper = new JPanel();
		boardsTableScrollerWrapper.setBorder(BorderFactory.createTitledBorder("Boards"));
		boardsTableScrollerWrapper.setLayout(new BorderLayout());
		boardsTableScrollerWrapper.add(boardsTableScroller);
		
		boardView = new BoardView();
		boardView.setBorder(BorderFactory.createTitledBorder("Current Board"));
		boardView.setPreferredSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));

		JPanel jobControls = new JPanel();
		jobControls.setLayout(new FlowLayout(FlowLayout.LEFT));
		jobControls.setBorder(BorderFactory.createTitledBorder("Job Control"));
		
		JButton button;
		button = new JButton(startPauseResumeJobAction);
		button.setPreferredSize(new Dimension(70, 70));
		jobControls.add(button);
		
		button = new JButton(stopJobAction);
		button.setPreferredSize(new Dimension(70, 70));
		jobControls.add(button);
		
//		JLabel jobStatusLabel = new JLabel();
//		jobStatusLabel.setText("STOPPED");
//		jobStatusLabel.setOpaque(true);
//		jobStatusLabel.setBackground(Color.black);
//		jobStatusLabel.setForeground(Color.red);
//		jobStatusLabel.setFont(jobStatusLabel.getFont().deriveFont(Font.BOLD, 52));
//		jobStatusLabel.setBorder(BorderFactory.createLoweredBevelBorder());
//		jobStatusLabel.setHorizontalAlignment(SwingConstants.CENTER);
		
		JPanel jobControlsWrapper = new JPanel();
		jobControlsWrapper.setLayout(new BorderLayout());
		jobControlsWrapper.add(jobControls, BorderLayout.NORTH);
		
		JPanel topLeftBox = new JPanel();
		topLeftBox.setLayout(new BorderLayout());
		topLeftBox.add(cameraPanel, BorderLayout.NORTH);
		topLeftBox.add(jobControlsWrapper, BorderLayout.CENTER);

		JPanel topBox = new JPanel();
		topBox.setLayout(new BorderLayout());
		topBox.add(topLeftBox, BorderLayout.WEST);
		topBox.add(boardView, BorderLayout.CENTER);
		
		JSplitPane tablesSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, boardsTableScrollerWrapper, partsTableScrollerWrapper);
		tablesSplitPane.setContinuousLayout(true);
		tablesSplitPane.setDividerLocation(450);
		
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(topBox, BorderLayout.CENTER);
		getContentPane().add(tablesSplitPane, BorderLayout.SOUTH);
		
		createMenuBar();
	}
	
	private void createMenuBar() {
		JMenuBar menuBar = new JMenuBar();

		menuBar.add(createFileMenu());
		menuBar.add(createEditMenu());
		menuBar.add(createJobMenu());

		setJMenuBar(menuBar);
	}
	
	private JMenu createFileMenu() {
		JMenu menu = new JMenu("File");
		
		menu.add(newJobAction);
		menu.add(openJobAction);
		menu.addSeparator();
		menu.add(closeJobAction);
		menu.addSeparator();
		menu.add(saveJobAction);
		menu.add(saveJobAsAction);
		
		return menu;
	}
	
	private JMenu createEditMenu() {
		JMenu menu = new JMenu("Edit");
		
		menu.add(addBoardAction);
		menu.add(deleteBoardAction);
		menu.add(enableDisableBoardAction);
		menu.addSeparator();
		menu.add(moveBoardUpAction);
		menu.add(moveBoardDownAction);
		menu.addSeparator();
		menu.add(orientBoardAction);
		
		return menu;
	}
	
	private JMenu createJobMenu() {
		JMenu menu = new JMenu("Job");
		
		menu.add(startPauseResumeJobAction);
		menu.add(stopJobAction);
		
		return menu;
	}
	
//	class PartsTableCellRenderer extends DefaultTableCellRenderer {
//		public Component getTableCellRendererComponent(JTable table,
//				Object value, boolean isSelected, boolean hasFocus, int row,
//				int col) {
//			Component comp = super.getTableCellRendererComponent(table, value,
//					isSelected, hasFocus, row, col);
//
//			if (row % 2 == 0) {
//				comp.setForeground(Color.red);
//			}
//			else {
//				comp.setForeground(null);
//			}
//
//			return (comp);
//		}
//	}
	
	class PartsTableModel extends AbstractTableModel {
		private String[] columnNames = new String[] { "Board #", "Part", "Package", "Feeder", "X Pos.", "Y Pos.", "Rotation" };
		private ArrayList<PartMetaData> boardPlacements = new ArrayList<PartMetaData>();
		
		public void setJob(Job job) {
			boardPlacements.clear();
			int boardNumber = 1;
			for (JobBoard board : job.getBoards()) {
				for (Placement placement : board.getBoard().getPlacements()) {
					boardPlacements.add(new PartMetaData(boardNumber, board.getBoard(), placement));
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
				return boardPlacements.get(row).placement.getPart().getReference();
			case 2:
				return boardPlacements.get(row).placement.getPart().getPackage().getReference();
			case 3:
				return boardPlacements.get(row).placement.getPart().getFeederLocations().get(0).getFeeder().getReference();
			case 4:
				return String.format("%2.3f", boardPlacements.get(row).placement.getLocation().getX());
			case 5:
				return String.format("%2.3f", boardPlacements.get(row).placement.getLocation().getY());
			case 6:
				return String.format("%2.3f", boardPlacements.get(row).placement.getLocation().getRotation());
			default:
				return null;
			}
		}
		
		class PartMetaData {
			public int boardNumber;
			public Board board;
			public Placement placement;
			
			public PartMetaData(int boardNumber, Board board, Placement placement) {
				this.boardNumber = boardNumber;
				this.board = board;
				this.placement = placement;
			}
		}
	}
	
	class BoardsTableModel extends AbstractTableModel {
		private String[] columnNames = new String[] { "#", "Board", "X Pos.", "Y Pos.", "Rotation" };
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
