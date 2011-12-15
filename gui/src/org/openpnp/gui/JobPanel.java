package org.openpnp.gui;

import java.awt.BorderLayout;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openpnp.BoardLocation;
import org.openpnp.Configuration;
import org.openpnp.ConfigurationListener;
import org.openpnp.Job;
import org.openpnp.JobProcessor;
import org.openpnp.JobProcessor.JobError;
import org.openpnp.JobProcessor.JobState;
import org.openpnp.JobProcessor.PickRetryAction;
import org.openpnp.JobProcessorDelegate;
import org.openpnp.JobProcessorListener;
import org.openpnp.Part;
import org.openpnp.Placement;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MachineListener;

public class JobPanel extends JPanel implements JobProcessorListener, JobProcessorDelegate, MachineListener, ConfigurationListener {
	final private Configuration configuration;
	final private JobProcessor jobProcessor;
	final private Frame frame;
	
	private BoardsTableModel boardsTableModel;
	private PlacementsTableModel placementsTableModel;
	private JTable boardsTable;
	private JTable placementsTable;
	
	public JobPanel(Configuration configuration, JobProcessor jobProcessor, Frame frame) {
		this.configuration = configuration;
		this.jobProcessor = jobProcessor;
		this.frame = frame;
		
		boardsTableModel = new BoardsTableModel();
		placementsTableModel = new PlacementsTableModel(configuration);

		placementsTable = new JTable(placementsTableModel);

		boardsTable = new JTable(boardsTableModel);
		boardsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		boardsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) {
					return;
				}
				int index = boardsTable.getSelectedRow();
				if (index == -1) {
					placementsTableModel.setPlacements(null);
				}
				else {
					index = boardsTable.convertRowIndexToModel(index);
					List<Placement> placements = JobPanel.this.jobProcessor.getJob().getBoardLocations().get(index).getBoard().getPlacements();
					placementsTableModel.setPlacements(placements);
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
		
		JSplitPane splitPane = new JSplitPane();
		splitPane.setBorder(null);
		splitPane.setContinuousLayout(true);
		splitPane.setDividerLocation(350);
		splitPane.setLeftComponent(new JScrollPane(boardsTable));
		splitPane.setRightComponent(new JScrollPane(placementsTable));
		
		add(splitPane);
		
		jobProcessor.addListener(this);
		jobProcessor.setDelegate(this);

		configuration.addListener(this);
	}
	
	public void configurationLoaded(Configuration configuration) {
		configuration.getMachine().addListener(this);
		updateJobActions();
	}
	
	public JobProcessor getJobProcessor() {
		return jobProcessor;
	}
	
	@Override
	public void jobStateChanged(JobState state) {
		updateJobActions();
	}

	@Override
	public void jobLoaded(Job job) {
		placementsTableModel.setPlacements(null);
		boardsTableModel.setJob(jobProcessor.getJob());
		updateJobActions();
	}

	@Override
	public PickRetryAction partPickFailed(BoardLocation board, Part part,
			Feeder feeder) {
		return PickRetryAction.SkipAndContinue;
	}

	@Override
	public void jobEncounteredError(JobError error, String description) {
		MessageBoxes.errorBox(this, error.toString(), description);
	}

	@Override
	public void boardProcessingCompleted(BoardLocation board) {
	}

	@Override
	public void boardProcessingStarted(BoardLocation board) {
	}

	@Override
	public void partPicked(BoardLocation board, Placement placement) {
	}

	@Override
	public void partPlaced(BoardLocation board, Placement placement) {
	}

	@Override
	public void partProcessingCompleted(BoardLocation board, Placement placement) {
		// partsComplete++;
	}

	@Override
	public void partProcessingStarted(BoardLocation board, Placement placement) {
	}

	@Override
	public void detailedStatusUpdated(String status) {
	}

	@Override
	public void machineHeadActivity(Machine machine, Head head) {
	}

	@Override
	public void machineEnabled(Machine machine) {
		updateJobActions();
	}

	@Override
	public void machineEnableFailed(Machine machine, String reason) {
	}

	@Override
	public void machineDisabled(Machine machine, String reason) {
		updateJobActions();
		jobProcessor.stop();
	}

	@Override
	public void machineDisableFailed(Machine machine, String reason) {
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
	
	public Action openJobAction = new AbstractAction("Open Job...") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
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

	public Action closeJobAction = new AbstractAction("Close Job") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
		}
	};

	public Action newJobAction = new AbstractAction("New Job") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
		}
	};

	public Action saveJobAction = new AbstractAction("Save Job") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
		}
	};

	public Action saveJobAsAction = new AbstractAction("Save Job As...") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
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
				if (!filename.toLowerCase().endsWith(".board.xml")) {
					filename = filename + ".board.xml";
				}
				File file = new File(new File(fileDialog.getDirectory()),
						filename);
				System.out.println(file.toString());
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
				File file = new File(new File(fileDialog.getDirectory()),
						fileDialog.getFile());
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
		}
	};

	public Action orientBoardAction = new AbstractAction("Set Board Location") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			// Get the currently selected board
			int selectedRow = boardsTable.getSelectedRow();
			BoardLocation boardLocation = jobProcessor.getJob().getBoardLocations()
					.get(selectedRow);
			Wizard wizard = new OrientBoardWizard(boardLocation, configuration);
//				startWizard(wizard);
		}
	};
}
