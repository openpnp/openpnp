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

import java.awt.FileDialog;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;

import org.openpnp.Configuration;
import org.openpnp.Job;
import org.openpnp.Job.JobBoard;
import org.openpnp.JobProcessor;
import org.openpnp.JobProcessor.JobError;
import org.openpnp.JobProcessor.JobState;
import org.openpnp.JobProcessor.PickRetryAction;
import org.openpnp.JobProcessorDelegate;
import org.openpnp.JobProcessorListener;
import org.openpnp.Part;
import org.openpnp.Placement;
import org.openpnp.gui.components.CameraPanel;
import org.openpnp.gui.components.MachineControlsPanel;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MachineListener;

/**
 * The main window of the application. Implements the top level menu, Job run
 * and Job setup.
 */
@SuppressWarnings("serial")
public class MainFrame extends MainFrameUI implements JobProcessorListener,
		JobProcessorDelegate, MachineListener {
	/*
	 * TODO define accelerators and mnemonics
	 * openJobMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
	 * Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
	 */
	private Configuration configuration;
	private JobProcessor jobProcessor;
	private BoardsTableModel boardsTableModel;
	private PartsTableModel partsTableModel;

	public MainFrame() {
		super();
		
		try {
			configuration = new Configuration("config");
		}
		catch (Exception e) {
			throw new Error(e);
		}

		for (Camera camera : configuration.getMachine().getCameras()) {
			cameraPanel.addCamera(camera);
		}
		
		boardsTableModel = new BoardsTableModel();
		partsTableModel = new PartsTableModel();
		
		boardsTable.setModel(boardsTableModel);
		partsTable.setModel(partsTableModel);

		jobProcessor = new JobProcessor(configuration);
		jobProcessor.addListener(this);
		jobProcessor.setDelegate(this);
		configuration.getMachine().addListener(this);

		machineControlsPanel.setMachine(configuration.getMachine());
		
		updateJobControls();
	}
	
	/**
	 * Updates the Job controls based on the Job state and the Machine's
	 * readiness.
	 */
	private void updateJobControls() {
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

		// We allow the above to run first so that all state is represented correctly
		// even if the machine is disabled.
		if (!configuration.getMachine().isReady()) {
			startPauseResumeJobAction.setEnabled(false);
			stopJobAction.setEnabled(false);
			stepJobAction.setEnabled(false);
		}
	}

	@Override
	public void jobStateChanged(JobState state) {
		updateJobControls();
	}

	@Override
	protected void openJob() {
		FileDialog fileDialog = new FileDialog(MainFrame.this);
		fileDialog.setVisible(true);
		try {
			File file = new File(new File(fileDialog.getDirectory()),
					fileDialog.getFile());
			jobProcessor.load(file);
		}
		catch (Exception e) {
			MessageBoxes.errorBox(this, "Job Load Error", e.getMessage());
		}
	}

	@Override
	protected void startPauseResumeJob() {
		JobState state = jobProcessor.getState();
		if (state == JobState.Stopped) {
			try {
				jobProcessor.start();
			}
			catch (Exception e) {
				MessageBoxes.errorBox(this, "Job Start Error", e.getMessage());
			}
		}
		else if (state == JobState.Paused) {
			jobProcessor.resume();
		}
		else if (state == JobState.Running) {
			jobProcessor.pause();
		}
	}
	
	@Override
	protected void stepJob() {
		try {
			jobProcessor.step();
		}
		catch (Exception e) {
			MessageBoxes.errorBox(this, "Job Start Error", e.getMessage());
		}
	}

	@Override
	protected void stopJob() {
		jobProcessor.stop();
	}

	@Override
	public void jobLoaded(Job job) {
		partsTableModel.setJob(jobProcessor.getJob());
		boardsTableModel.setJob(jobProcessor.getJob());
		updateJobControls();
	}

	@Override
	public PickRetryAction partPickFailed(JobBoard board, Part part,
			Feeder feeder) {
		return PickRetryAction.SkipAndContinue;
	}

	@Override
	public void jobEncounteredError(JobError error, String description) {
		MessageBoxes.errorBox(this, error.toString(), description);
	}

	@Override
	public void boardProcessingCompleted(JobBoard board) {
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
	
	@Override
	public void machineHeadActivity(Machine machine, Head head) {
	}

	@Override
	public void machineStarted(Machine machine) {
		updateJobControls();
	}

	@Override
	public void machineStartFailed(Machine machine, String reason) {
	}

	@Override
	public void machineStopped(Machine machine, String reason) {
		updateJobControls();
		jobProcessor.stop();
	}

	@Override
	public void machineStopFailed(Machine machine, String reason) {
	}
}
