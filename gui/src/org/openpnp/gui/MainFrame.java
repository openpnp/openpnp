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

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import org.openpnp.JobProcessor;
import org.openpnp.JobProcessorListener;
import org.openpnp.gui.components.CameraPanel;
import org.openpnp.gui.components.MachineControlsPanel;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.OSXAdapter;
import org.openpnp.model.Configuration;
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
	final private MachineControlsPanel machineControlsPanel;

	private PartsPanel partsPanel;
	private FeedersPanel feedersPanel;
	private JobPanel jobPanel;
	private MachinePanel machinePanel;
	private CamerasPanel camerasPanel;
	private BoardsPanel boardsPanel;

	private JPanel contentPane;
	private CameraPanel cameraPanel;
	private JLabel lblStatus;
	private JTabbedPane panelBottom;

	public MainFrame(Configuration configuration, JobProcessor jobProcessor, MachineControlsPanel machineControlsPanel) {
		this.configuration = configuration;
		this.jobProcessor = jobProcessor;
		this.machineControlsPanel = machineControlsPanel;
		
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		
		// Get handlers for quit and close in place
		registerForMacOSXEvents();
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				quit();
			}
		});

		setBounds(100, 100, 1280, 1024);
//		setBounds(100, 100, 1024, 768);
		
		machinePanel = new MachinePanel(configuration);
		jobPanel = new JobPanel(configuration, jobProcessor, this, machineControlsPanel);
		partsPanel = new PartsPanel(configuration, machineControlsPanel, this);
		feedersPanel = new FeedersPanel(configuration, machineControlsPanel);
		camerasPanel = new CamerasPanel(configuration, machineControlsPanel);
		boardsPanel = new BoardsPanel(configuration, this, machineControlsPanel);

		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);

		mnFile.add(new JMenuItem(jobPanel.newJobAction));
		mnFile.add(new JMenuItem(jobPanel.openJobAction));
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

		machineControlsPanel.setBorder(new TitledBorder(null,
				"Machine Controls", TitledBorder.LEADING, TitledBorder.TOP,
				null, null));
		
		panel.add(machineControlsPanel);
		
		// Add global hotkeys for the arrow keys
		final Map<KeyStroke, Action> hotkeyActionMap = new HashMap<KeyStroke, Action>();
		
		hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.CTRL_DOWN_MASK), machineControlsPanel.yPlusAction);
		hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.CTRL_DOWN_MASK), machineControlsPanel.yMinusAction);
		hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.CTRL_DOWN_MASK), machineControlsPanel.xMinusAction);
		hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.CTRL_DOWN_MASK), machineControlsPanel.xPlusAction);
		hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, KeyEvent.CTRL_DOWN_MASK), machineControlsPanel.zPlusAction);
		hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, KeyEvent.CTRL_DOWN_MASK), machineControlsPanel.zMinusAction);
		hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, KeyEvent.CTRL_DOWN_MASK), machineControlsPanel.cMinusAction);
		hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD, KeyEvent.CTRL_DOWN_MASK), machineControlsPanel.cPlusAction);
		hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, KeyEvent.CTRL_DOWN_MASK), machineControlsPanel.lowerIncrementAction);
		hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, KeyEvent.CTRL_DOWN_MASK), machineControlsPanel.raiseIncrementAction);
		
		// TODO need to restrict this capture somehow, it breaks textfields
		// and using arrow keys to move through lists.
		Toolkit.getDefaultToolkit().getSystemEventQueue().push(new EventQueue() {
			@Override
			protected void dispatchEvent(AWTEvent event) {
				if (event instanceof KeyEvent) {
					KeyStroke ks = KeyStroke.getKeyStrokeForEvent((KeyEvent) event);
					Action action = hotkeyActionMap.get(ks);
					if (action != null && action.isEnabled()) {
						action.actionPerformed(null);
						return;
					}
				}
				super.dispatchEvent(event);
			}
		});
		

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
		panelBottom.addTab("Boards", null, boardsPanel, null);
		panelBottom.addTab("Parts", null, partsPanel, null);
		panelBottom.addTab("Feeders", null, feedersPanel, null);
		panelBottom.addTab("Cameras", null, camerasPanel, null);
		panelBottom.addTab("Machine", null, machinePanel, null);

		try {
			configuration.load("config");
		}
		catch (Exception e) {
			e.printStackTrace();
			MessageBoxes.errorBox(
					this, 
					"Configuration Load Error", 
					"There was a problem loading the configuration. The reason was:\n\n" + e.getMessage() + "\n\nPlease check your configuration files and try again. The program will now exit.");
			System.exit(1);
		}

		for (Camera camera : configuration.getMachine().getCameras()) {
			cameraPanel.addCamera(camera);
		}
		
		jobProcessor.addListener(jobProcessorListener);
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
			}
		}
		catch (Exception e) {
			MessageBoxes.errorBox(
					MainFrame.this, 
					"Configuration Save Error",
					"There was a problem saving the configuration. The reason was:\n\n" + e.getMessage() + "\n\nPlease check your configuration and try again.");
			return false;
		}
		if (!boardsPanel.checkForModifications()) {
			return false;
		}
		if (!jobPanel.checkForModifications()) {
			return false;
		}
		// Attempt to stop the machine on quit
		try {
			configuration.getMachine().setEnabled(false);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		System.exit(0);
		return true;
	}
	
	private JobProcessorListener jobProcessorListener = new JobProcessorListener.Adapter() {
		@Override
		public void detailedStatusUpdated(String status) {
			lblStatus.setText(status);
		}
	};
}
