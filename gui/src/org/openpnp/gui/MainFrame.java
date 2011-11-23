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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.openpnp.BoardLocation;
import org.openpnp.Configuration;
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
import org.openpnp.gui.support.OSXAdapter;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MachineListener;
import org.openpnp.spi.Wizard;
import org.openpnp.spi.WizardContainer;

/**
 * The main window of the application. Implements the top level menu, Job run
 * and Job setup.
 */
@SuppressWarnings("serial")
public class MainFrame extends MainFrameUi implements JobProcessorListener,
		JobProcessorDelegate, MachineListener, WizardContainer {

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
		
		// Get handlers for quit and close in place
		registerForMacOSXEvents();
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				quit();
			}
		});
		

		try {
			configuration = new Configuration("config");
		}
		catch (Exception e) {
			throw new Error(e);
		}
		
		try {
			configuration.getMachine().start();
		}
		catch (Exception e) {
			// TODO: message box
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

	public void registerForMacOSXEvents() {
		if ((System.getProperty("os.name").toLowerCase().startsWith("mac os x"))) {
			try {
				// Generate and register the OSXAdapter, passing it a hash of
				// all the methods we wish to
				// use as delegates for various
				// com.apple.eawt.ApplicationListener methods
				OSXAdapter.setQuitHandler(this,
						getClass().getDeclaredMethod("quit", (Class[]) null));
//				OSXAdapter.setAboutHandler(this,
//						getClass().getDeclaredMethod("about", (Class[]) null));
//				OSXAdapter.setPreferencesHandler(this, getClass()
//						.getDeclaredMethod("preferences", (Class[]) null));
//				OSXAdapter.setFileHandler(
//						this,
//						getClass().getDeclaredMethod("loadImageFile",
//								new Class[] { String.class }));
			}
			catch (Exception e) {
				System.err.println("Error while loading the OSXAdapter:");
				e.printStackTrace();
			}
		}
	}
	
	public boolean quit() {
		// Attempt to stop the machine on quit
		try {
			configuration.getMachine().setEnabled(false);
		}
		catch (Exception e) {
		}
		System.exit(0);
		return true;
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

		// We allow the above to run first so that all state is represented
		// correctly
		// even if the machine is disabled.
		if (!configuration.getMachine().isEnabled()) {
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
	protected void orientBoard() {
		// Get the currently selected board
		int selectedRow = boardsTable.getSelectedRow();
		BoardLocation board = jobProcessor.getJob().getBoards().get(selectedRow);
		Wizard wizard = new OrientBoardWizard(board, configuration);
		startWizard(wizard);
	}
	
	private void startWizard(Wizard wizard) {
		// TODO: If there is already a wizard running, take care of that
		
		// Configure the wizard
		wizard.setWizardContainer(this);
		
		// Create a titled panel to hold the wizard
		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createTitledBorder("Wizard: " + wizard.getWizardName()));
		panel.setLayout(new BorderLayout());
		panel.add(wizard.getWizardPanel());
		panelBottom.add(panel, "Wizard");
		panelBottomCardLayout.show(panelBottom, "Wizard");
	}
	
	public void wizardCompleted(Wizard wizard) {
		
	}
	
	public void wizardCancelled(Wizard wizard) {
		
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
		lblStatus.setText(status);
	}

	@Override
	public void machineHeadActivity(Machine machine, Head head) {
	}

	@Override
	public void machineEnabled(Machine machine) {
		updateJobControls();
	}

	@Override
	public void machineEnableFailed(Machine machine, String reason) {
	}

	@Override
	public void machineDisabled(Machine machine, String reason) {
		updateJobControls();
		jobProcessor.stop();
	}

	@Override
	public void machineDisableFailed(Machine machine, String reason) {
	}
}
