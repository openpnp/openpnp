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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

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
import org.openpnp.gui.components.CameraPanel;
import org.openpnp.gui.components.MachineControlsPanel;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.OSXAdapter;
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
// TODO: check out icons at http://www.iconarchive.com/show/soft-scraps-icons-by-deleket.1.html
// TODO: add JobProcessorListener back in to listen for detailedStatusUpdate
public class MainFrame extends JFrame implements WizardContainer {
	/*
	 * TODO define accelerators and mnemonics
	 * openJobMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
	 * Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
	 */
	private Configuration configuration;
	private Machine machine;

	private JPanel contentPane;
	private MachineControlsPanel machineControlsPanel;
	private CameraPanel cameraPanel;
	private JLabel lblStatus;
	private JTabbedPane panelBottom;
	private PartsPanel partsPanel;
	private FeedersPanel feedersPanel;
	private JobPanel jobPanel;

	public MainFrame() {
		// Get handlers for quit and close in place
		registerForMacOSXEvents();
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				quit();
			}
		});

		try {
			Configuration.get().load("config");
		}
		catch (Exception e) {
			// TODO: dialog
			throw new Error(e);
		}

		configuration = Configuration.get();
		machine = configuration.getMachine();

		createUi();

		try {
			machine.start();
		}
		catch (Exception e) {
			// TODO: dialog
			throw new Error(e);
		}

		for (Camera camera : machine.getCameras()) {
			cameraPanel.addCamera(camera);
		}

		machineControlsPanel.setMachine(machine);
		partsPanel.refresh();
		feedersPanel.refresh();
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
			if (Configuration.get().isDirty()) {
				Configuration.get().save("config");
				System.out.println("Configuration saved.");
			}
		}
		catch (Exception e) {
			// TODO: dialog, maybe try to recover
		}
		// Attempt to stop the machine on quit
		try {
			machine.setEnabled(false);
		}
		catch (Exception e) {
		}
		System.exit(0);
		return true;
	}

	private void startWizard(Wizard wizard) {
		// TODO: If there is already a wizard running, take care of that

		// Configure the wizard
		wizard.setWizardContainer(this);

		// Create a titled panel to hold the wizard
		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createTitledBorder("Wizard: "
				+ wizard.getWizardName()));
		panel.setLayout(new BorderLayout());
		panel.add(wizard.getWizardPanel());
		panelBottom.add(panel, "Wizard");
		// TODO: broken
		// panelBottomCardLayout.show(panelBottom, "Wizard");
	}

	public void wizardCompleted(Wizard wizard) {

	}

	public void wizardCancelled(Wizard wizard) {

	}

	private void createUi() {
		setBounds(100, 100, 1280, 1024);
		
		jobPanel = new JobPanel(this);
		partsPanel = new PartsPanel();
		feedersPanel = new FeedersPanel();

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

		mnEdit.add(new JMenuItem(jobPanel.addBoardAction));
		mnEdit.add(new JMenuItem(jobPanel.deleteBoardAction));
		mnEdit.add(new JMenuItem(jobPanel.enableDisableBoardAction));
		mnEdit.addSeparator();
		mnEdit.add(new JMenuItem(jobPanel.moveBoardUpAction));
		mnEdit.add(new JMenuItem(jobPanel.moveBoardDownAction));
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

		machineControlsPanel = new MachineControlsPanel();
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
	}
}
