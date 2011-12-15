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
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import org.openpnp.BoardLocation;
import org.openpnp.Configuration;
import org.openpnp.Job;
import org.openpnp.JobProcessor;
import org.openpnp.JobProcessor.JobError;
import org.openpnp.JobProcessor.JobState;
import org.openpnp.JobProcessorListener;
import org.openpnp.Placement;
import org.openpnp.gui.components.CameraPanel;
import org.openpnp.gui.components.MachineControlsPanel;
import org.openpnp.gui.support.OSXAdapter;
import org.openpnp.spi.Camera;

/**
 * The main window of the application.
 */
@SuppressWarnings("serial")
// TODO: check out icons at http://www.iconarchive.com/show/soft-scraps-icons-by-deleket.1.html
public class MainFrame extends JFrame {
	/*
	 * TODO define accelerators and mnemonics
	 * openJobMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
	 * Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
	 */
	final private Configuration configuration;
	final private JobProcessor jobProcessor;

	private PartsPanel partsPanel;
	private FeedersPanel feedersPanel;
	private JobPanel jobPanel;

	private JPanel contentPane;
	private MachineControlsPanel machineControlsPanel;
	private CameraPanel cameraPanel;
	private JLabel lblStatus;
	private JTabbedPane panelBottom;

	public MainFrame(Configuration configuration, JobProcessor jobProcessor) {
		this.configuration = configuration;
		this.jobProcessor = jobProcessor;
		
		// Get handlers for quit and close in place
		registerForMacOSXEvents();
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				quit();
			}
		});

		setBounds(100, 100, 1280, 1024);
		
		jobPanel = new JobPanel(configuration, jobProcessor, this);
		partsPanel = new PartsPanel(configuration);
		feedersPanel = new FeedersPanel(configuration);

		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);

		mnFile.add(new JMenuItem(jobPanel.newJobAction));
		mnFile.add(new JMenuItem(jobPanel.openJobAction));
		mnFile.addSeparator();
		mnFile.add(new JMenuItem(jobPanel.closeJobAction));
		mnFile.addSeparator();
		mnFile.add(new JMenuItem(jobPanel.saveJobAction));
		mnFile.add(new JMenuItem(jobPanel.saveJobAsAction));

		JMenu mnEdit = new JMenu("Edit");
		menuBar.add(mnEdit);

		mnEdit.add(new JMenuItem(jobPanel.newBoardAction));
		mnEdit.add(new JMenuItem(jobPanel.addBoardAction));
		mnEdit.add(new JMenuItem(jobPanel.removeBoardAction));
		mnEdit.addSeparator();
		mnEdit.add(new JMenuItem(jobPanel.orientBoardAction));

		JMenu mnJob = new JMenu("Job Control");
		menuBar.add(mnJob);

		mnJob.add(new JMenuItem(jobPanel.startPauseResumeJobAction));
		mnJob.add(new JMenuItem(jobPanel.stepJobAction));
		mnJob.add(new JMenuItem(jobPanel.stopJobAction));

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

		machineControlsPanel = new MachineControlsPanel(configuration);
		machineControlsPanel.setBorder(new TitledBorder(null,
				"Machine Controls", TitledBorder.LEADING, TitledBorder.TOP,
				null, null));
		panel.add(machineControlsPanel);

		cameraPanel = new CameraPanel();
		panelTop.add(cameraPanel, BorderLayout.CENTER);
		cameraPanel.setBorder(new TitledBorder(null, "Cameras",
				TitledBorder.LEADING, TitledBorder.TOP, null, null));

		panelBottom = new JTabbedPane(JTabbedPane.TOP);
		splitPaneTopBottom.setRightComponent(panelBottom);

		lblStatus = new JLabel(" ");
		lblStatus.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null,
				null, null));
		contentPane.add(lblStatus, BorderLayout.SOUTH);

		splitPaneTopBottom.setDividerLocation(600);
		
		panelBottom.addTab("Job", null, jobPanel, null);
		panelBottom.addTab("Parts", null, partsPanel, null);
		panelBottom.addTab("Feeders", null, feedersPanel, null);

		try {
			configuration.load("config");
		}
		catch (Exception e) {
			// TODO: dialog
			throw new Error(e);
		}

		try {
			configuration.getMachine().start();
		}
		catch (Exception e) {
			// TODO: dialog
			throw new Error(e);
		}

		for (Camera camera : configuration.getMachine().getCameras()) {
			cameraPanel.addCamera(camera);
		}
		
		jobProcessor.addListener(new MainFrameJobProcessorListener());
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
				// OSXAdapter.setAboutHandler(this,
				// getClass().getDeclaredMethod("about", (Class[]) null));
				// OSXAdapter.setPreferencesHandler(this, getClass()
				// .getDeclaredMethod("preferences", (Class[]) null));
				// OSXAdapter.setFileHandler(
				// this,
				// getClass().getDeclaredMethod("loadImageFile",
				// new Class[] { String.class }));
			}
			catch (Exception e) {
				System.err.println("Error while loading the OSXAdapter:");
				e.printStackTrace();
			}
		}
	}

	public boolean quit() {
		// Save the configuration if it's dirty
		try {
			if (configuration.isDirty()) {
				configuration.save("config");
				System.out.println("Configuration saved.");
			}
		}
		catch (Exception e) {
			// TODO: dialog, maybe try to recover
		}
		// Attempt to stop the machine on quit
		try {
			configuration.getMachine().setEnabled(false);
		}
		catch (Exception e) {
		}
		System.exit(0);
		return true;
	}

	class MainFrameJobProcessorListener implements JobProcessorListener {
		@Override
		public void jobLoaded(Job job) {
		}
	
		@Override
		public void jobStateChanged(JobState state) {
		}
	
		@Override
		public void jobEncounteredError(JobError error, String description) {
		}
	
		@Override
		public void boardProcessingStarted(BoardLocation board) {
		}
	
		@Override
		public void boardProcessingCompleted(BoardLocation board) {
		}
	
		@Override
		public void partProcessingStarted(BoardLocation board, Placement placement) {
		}
	
		@Override
		public void partPicked(BoardLocation board, Placement placement) {
		}
	
		@Override
		public void partPlaced(BoardLocation board, Placement placement) {
		}
	
		@Override
		public void partProcessingCompleted(BoardLocation board, Placement placement) {
		}
	
		@Override
		public void detailedStatusUpdated(String status) {
			lblStatus.setText(status);
		}
	}
}
