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
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import org.openpnp.ConfigurationListener;
import org.openpnp.JobProcessorListener;
import org.openpnp.gui.components.CameraPanel;
import org.openpnp.gui.support.HeadCellValue;
import org.openpnp.gui.support.LengthCellValue;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.OSXAdapter;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;

/**
 * The main window of the application.
 */
@SuppressWarnings("serial")
public class MainFrame extends JFrame {
	private static final String PREF_WINDOW_X = "MainFrame.windowX";
	private static final int PREF_WINDOW_X_DEF = 0;
	private static final String PREF_WINDOW_Y = "MainFrame.windowY";
	private static final int PREF_WINDOW_Y_DEF = 0;
	private static final String PREF_WINDOW_WIDTH = "MainFrame.windowWidth";
	private static final int PREF_WINDOW_WIDTH_DEF = 1024;
	private static final String PREF_WINDOW_HEIGHT = "MainFrame.windowHeight";
	private static final int PREF_WINDOW_HEIGHT_DEF = 768;
	private static final String PREF_DIVIDER_POSITION = "MainFrame.dividerPosition";
	private static final int PREF_DIVIDER_POSITION_DEF = -1;

	/*
	 * TODO define accelerators and mnemonics
	 * openJobMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
	 * Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
	 */
	private final Configuration configuration;

	// TODO: Really should switch to some kind of DI model, but this will do
	// for now.
	public static MachineControlsPanel machineControlsPanel;
	public static PartsPanel partsPanel;
	public static PackagesPanel packagesPanel;
	public static FeedersPanel feedersPanel;
	public static JobPanel jobPanel;
	public static MachinePanel machinePanel;
	public static CamerasPanel camerasPanel;
	public static BoardsPanel boardsPanel;
	public static HeadsPanel headsPanel;
	public static ActuatorsPanel actuatorsPanel;
	public static NozzleTipsPanel nozzletipPanel;
	public static CameraPanel cameraPanel;

	private JPanel contentPane;
	private JLabel lblStatus;
	private JTabbedPane panelBottom;
	private JSplitPane splitPaneTopBottom;
	private TitledBorder panelInstructionsBorder;

	private Preferences prefs = Preferences.userNodeForPackage(MainFrame.class);
	
	private ActionListener instructionsCancelActionListener;
	private ActionListener instructionsProceedActionListener;

	public MainFrame(Configuration configuration) {
		this.configuration = configuration;
		LengthCellValue.setConfiguration(configuration);
		HeadCellValue.setConfiguration(configuration);

		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		// Get handlers for Mac application menu in place.
		boolean macOsXMenus = registerForMacOSXEvents();
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				quit();
			}
		});
		
        if (prefs.getInt(PREF_WINDOW_WIDTH, 50) < 50) {
            prefs.putInt(PREF_WINDOW_WIDTH, PREF_WINDOW_WIDTH_DEF);
        }
        
        if (prefs.getInt(PREF_WINDOW_HEIGHT, 50) < 50) {
            prefs.putInt(PREF_WINDOW_HEIGHT, PREF_WINDOW_HEIGHT_DEF);
        }
		        
		setBounds(prefs.getInt(PREF_WINDOW_X, PREF_WINDOW_X_DEF),
				prefs.getInt(PREF_WINDOW_Y, PREF_WINDOW_Y_DEF),
				prefs.getInt(PREF_WINDOW_WIDTH, PREF_WINDOW_WIDTH_DEF),
				prefs.getInt(PREF_WINDOW_HEIGHT, PREF_WINDOW_HEIGHT_DEF));

		cameraPanel = new CameraPanel();
		machineControlsPanel = new MachineControlsPanel(configuration, this,
				cameraPanel);
		machinePanel = new MachinePanel();
		jobPanel = new JobPanel(configuration, this, machineControlsPanel);
		partsPanel = new PartsPanel(configuration, this);
		packagesPanel = new PackagesPanel(configuration, this);
		feedersPanel = new FeedersPanel(configuration);
		camerasPanel = new CamerasPanel(this, configuration);
		boardsPanel = new BoardsPanel(configuration);
		headsPanel = new HeadsPanel(this, configuration, machineControlsPanel);
		actuatorsPanel = new ActuatorsPanel(configuration);
		nozzletipPanel = new NozzleTipsPanel(configuration);

		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		// File
		//////////////////////////////////////////////////////////////////////
		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);

		mnFile.add(new JMenuItem(jobPanel.newJobAction));
		mnFile.add(new JMenuItem(jobPanel.openJobAction));
		mnFile.addSeparator();
		mnFile.add(new JMenuItem(jobPanel.saveJobAction));
		mnFile.add(new JMenuItem(jobPanel.saveJobAsAction));
		
		// File -> Import
		//////////////////////////////////////////////////////////////////////
		mnFile.addSeparator();
		JMenu mnImport = new JMenu("Import Board");
		mnFile.add(mnImport);
		mnImport.add(new JMenuItem(jobPanel.importMountsmdUlpAction));
		mnImport.add(new JMenuItem(jobPanel.importMountsmdPosAction));
		
		
		if (!macOsXMenus) {
			mnFile.addSeparator();
			mnFile.add(new JMenuItem(quitAction));
		}

		// Edit
		//////////////////////////////////////////////////////////////////////
		JMenu mnEdit = new JMenu("Edit");
		menuBar.add(mnEdit);

		mnEdit.add(new JMenuItem(jobPanel.newBoardAction));
		mnEdit.add(new JMenuItem(jobPanel.addBoardAction));
		mnEdit.add(new JMenuItem(jobPanel.removeBoardAction));
		mnEdit.addSeparator();
		mnEdit.add(new JMenuItem(jobPanel.captureToolBoardLocationAction));

		// View
		//////////////////////////////////////////////////////////////////////
		JMenu mnView = new JMenu("View");
		menuBar.add(mnView);

		ButtonGroup buttonGroup = new ButtonGroup();

		JMenu mnUnits = new JMenu("System Units");
		mnView.add(mnUnits);

		JMenuItem menuItem;
		menuItem = new JCheckBoxMenuItem(inchesUnitSelected);
		buttonGroup.add(menuItem);
		if (configuration.getSystemUnits() == LengthUnit.Inches) {
			menuItem.setSelected(true);
		}
		mnUnits.add(menuItem);
		menuItem = new JCheckBoxMenuItem(millimetersUnitSelected);
		buttonGroup.add(menuItem);
		if (configuration.getSystemUnits() == LengthUnit.Millimeters) {
			menuItem.setSelected(true);
		}
		mnUnits.add(menuItem);

		// Job Control
		//////////////////////////////////////////////////////////////////////
		JMenu mnJob = new JMenu("Job Control");
		menuBar.add(mnJob);

		mnJob.add(new JMenuItem(jobPanel.startPauseResumeJobAction));
		mnJob.add(new JMenuItem(jobPanel.stepJobAction));
		mnJob.add(new JMenuItem(jobPanel.stopJobAction));

		// Machine
		//////////////////////////////////////////////////////////////////////
		JMenu mnCommands = new JMenu("Machine");
		menuBar.add(mnCommands);

		mnCommands.add(new JMenuItem(machineControlsPanel.homeAction));
		mnCommands.add(new JMenuItem(machineControlsPanel.goToZeroAction));
		
		// Help
		/////////////////////////////////////////////////////////////////////
		if (!macOsXMenus) {
			JMenu mnHelp = new JMenu("Help");
			menuBar.add(mnHelp);
			
			mnHelp.add(new JMenuItem(aboutAction));
		}

		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));

		splitPaneTopBottom = new JSplitPane();
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

		hotkeyActionMap
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP,
						KeyEvent.CTRL_DOWN_MASK), machineControlsPanel
						.getJogControlsPanel().yPlusAction);
		hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN,
				KeyEvent.CTRL_DOWN_MASK), machineControlsPanel
				.getJogControlsPanel().yMinusAction);
		hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,
				KeyEvent.CTRL_DOWN_MASK), machineControlsPanel
				.getJogControlsPanel().xMinusAction);
		hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,
				KeyEvent.CTRL_DOWN_MASK), machineControlsPanel
				.getJogControlsPanel().xPlusAction);
		hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP,
				KeyEvent.CTRL_DOWN_MASK), machineControlsPanel
				.getJogControlsPanel().zPlusAction);
		hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN,
				KeyEvent.CTRL_DOWN_MASK), machineControlsPanel
				.getJogControlsPanel().zMinusAction);
		hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_COMMA,
				KeyEvent.CTRL_DOWN_MASK), machineControlsPanel
				.getJogControlsPanel().cMinusAction);
		hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD,
				KeyEvent.CTRL_DOWN_MASK), machineControlsPanel
				.getJogControlsPanel().cPlusAction);
		hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS,
				KeyEvent.CTRL_DOWN_MASK),
				machineControlsPanel.lowerIncrementAction);
		hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS,
				KeyEvent.CTRL_DOWN_MASK),
				machineControlsPanel.raiseIncrementAction);
		hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB,
				KeyEvent.CTRL_DOWN_MASK),
				machineControlsPanel.showHideJogControlsWindowAction);

		Toolkit.getDefaultToolkit().getSystemEventQueue()
				.push(new EventQueue() {
					@Override
					protected void dispatchEvent(AWTEvent event) {
						if (event instanceof KeyEvent) {
							KeyStroke ks = KeyStroke
									.getKeyStrokeForEvent((KeyEvent) event);
							Action action = hotkeyActionMap.get(ks);
							if (action != null && action.isEnabled()) {
								action.actionPerformed(null);
								return;
							}
						}
						super.dispatchEvent(event);
					}
				});
		
		JPanel panelCameraAndInstructions = new JPanel(new BorderLayout());
		panelCameraAndInstructions.add(cameraPanel, BorderLayout.CENTER);

		panelTop.add(panelCameraAndInstructions, BorderLayout.CENTER);
		cameraPanel.setBorder(new TitledBorder(null, "Cameras",
				TitledBorder.LEADING, TitledBorder.TOP, null, null));
		
		panelInstructions = new JPanel();
		panelInstructions.setVisible(false);
		panelInstructions.setBorder(panelInstructionsBorder = new TitledBorder(null, "Instructions", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panelCameraAndInstructions.add(panelInstructions, BorderLayout.SOUTH);
		panelInstructions.setLayout(new BorderLayout(0, 0));
		
		panelInstructionActions = new JPanel();
		panelInstructionActions.setAlignmentY(Component.BOTTOM_ALIGNMENT);
		panelInstructions.add(panelInstructionActions, BorderLayout.EAST);
		panelInstructionActions.setLayout(new BorderLayout(0, 0));
		
		panel_2 = new JPanel();
		FlowLayout flowLayout_2 = (FlowLayout) panel_2.getLayout();
		flowLayout_2.setVgap(0);
		flowLayout_2.setHgap(0);
		panelInstructionActions.add(panel_2, BorderLayout.SOUTH);
		
		btnInstructionsCancel = new JButton("Cancel");
		btnInstructionsCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (instructionsCancelActionListener != null) {
					instructionsCancelActionListener.actionPerformed(arg0);
				}
			}
		});
		panel_2.add(btnInstructionsCancel);
		
		btnInstructionsNext = new JButton("Next");
		btnInstructionsNext.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (instructionsProceedActionListener != null) {
					instructionsProceedActionListener.actionPerformed(arg0);
				}
			}
		});
		panel_2.add(btnInstructionsNext);
		
		panel_1 = new JPanel();
		panelInstructions.add(panel_1, BorderLayout.CENTER);
		panel_1.setLayout(new BorderLayout(0, 0));
		
		lblInstructions = new JTextPane();
		lblInstructions.setFont(new Font("Lucida Grande", Font.PLAIN, 14));
		lblInstructions.setBackground(UIManager.getColor("Panel.background"));
		lblInstructions.setContentType("text/html");
		lblInstructions.setEditable(false);
		panel_1.add(lblInstructions);

		panelBottom = new JTabbedPane(JTabbedPane.TOP);
		splitPaneTopBottom.setRightComponent(panelBottom);

		lblStatus = new JLabel(" ");
		lblStatus.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null,
				null, null));
		contentPane.add(lblStatus, BorderLayout.SOUTH);

		splitPaneTopBottom.setDividerLocation(prefs.getInt(
				PREF_DIVIDER_POSITION, PREF_DIVIDER_POSITION_DEF));
		splitPaneTopBottom.addPropertyChangeListener("dividerLocation",
				new PropertyChangeListener() {
					@Override
					public void propertyChange(PropertyChangeEvent evt) {
						prefs.putInt(PREF_DIVIDER_POSITION,
								splitPaneTopBottom.getDividerLocation());
					}
				});

		panelBottom.addTab("Job", null, jobPanel, null);
		panelBottom.addTab("Boards", null, boardsPanel, null);
		panelBottom.addTab("Parts", null, partsPanel, null);
		panelBottom.addTab("Packages", null, packagesPanel, null);
		panelBottom.addTab("Feeders", null, feedersPanel, null);
		panelBottom.addTab("Cameras", null, camerasPanel, null);
		panelBottom.addTab("Machine", null, machinePanel, null);
		panelBottom.addTab("Heads", null, headsPanel, null);
		panelBottom.addTab("Actuators", null, actuatorsPanel, null);
		panelBottom.addTab("NozzleTips", null, nozzletipPanel, null);

		addComponentListener(componentListener);

		try {
			configuration.load();
		}
		catch (Exception e) {
			e.printStackTrace();
			MessageBoxes
					.errorBox(
							this,
							"Configuration Load Error",
							"There was a problem loading the configuration. The reason was:\n\n"
									+ e.getMessage()
									+ "\n\nPlease check your configuration files and try again. The program will now exit.");
			System.exit(1);
		}

		for (Camera camera : configuration.getMachine().getCameras()) {
			cameraPanel.addCamera(camera);
		}
        for (Head head : Configuration.get().getMachine().getHeads()) {
            for (Camera camera : head.getCameras()) {
                cameraPanel.addCamera(camera);
            }
        }

		configuration.addListener(new ConfigurationListener.Adapter() {
		    @Override
            public void configurationComplete(Configuration configuration)
                    throws Exception {
		        configuration.getMachine().getJobProcessor().addListener(jobProcessorListener);
            }
		});
	}
	
	public void showInstructions(
			String title,
			String instructions, 
			boolean showCancelButton, 
			boolean showProceedButton, 
			String proceedButtonText,
			ActionListener cancelActionListener,
			ActionListener proceedActionListener) {
		panelInstructionsBorder.setTitle(title);
		lblInstructions.setText(instructions);
		btnInstructionsCancel.setVisible(showCancelButton);
		btnInstructionsNext.setVisible(showProceedButton);
		btnInstructionsNext.setText(proceedButtonText);
		instructionsCancelActionListener = cancelActionListener;
		instructionsProceedActionListener = proceedActionListener;
		panelInstructions.setVisible(true);
		doLayout();
		panelInstructions.repaint();
	}
	
	public void hideInstructions() {
		panelInstructions.setVisible(false);
		doLayout();
	}

	public boolean registerForMacOSXEvents() {
		if ((System.getProperty("os.name").toLowerCase().startsWith("mac os x"))) {
			try {
				// Generate and register the OSXAdapter, passing it a hash of
				// all the methods we wish to
				// use as delegates for various
				// com.apple.eawt.ApplicationListener methods
				OSXAdapter.setQuitHandler(this,
						getClass().getDeclaredMethod("quit", (Class[]) null));
				OSXAdapter.setAboutHandler(this,
						getClass().getDeclaredMethod("about", (Class[]) null));
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
			return true;
		}
		return false;
	}

	public void about() {
		AboutDialog dialog = new AboutDialog(this);
		dialog.setSize(350, 350);
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);		
	}

	public boolean quit() {
		try {
			Preferences.userRoot().flush();
		}
		catch (Exception e) {

		}

		// Save the configuration if it's dirty
		try {
			if (configuration.isDirty()) {
				configuration.save();
			}
		}
		catch (Exception e) {
			MessageBoxes
					.errorBox(
							MainFrame.this,
							"Configuration Save Error",
							"There was a problem saving the configuration. The reason was:\n\n"
									+ e.getMessage()
									+ "\n\nPlease check your configuration and try again.");
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

	private ComponentListener componentListener = new ComponentAdapter() {
		@Override
		public void componentMoved(ComponentEvent e) {
			prefs.putInt(PREF_WINDOW_X, getLocation().x);
			prefs.putInt(PREF_WINDOW_Y, getLocation().y);
		}

		@Override
		public void componentResized(ComponentEvent e) {
			prefs.putInt(PREF_WINDOW_WIDTH, getSize().width);
			prefs.putInt(PREF_WINDOW_HEIGHT, getSize().height);
		}
	};

	private Action inchesUnitSelected = new AbstractAction(
			LengthUnit.Inches.name()) {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			configuration.setSystemUnits(LengthUnit.Inches);
			MessageBoxes.errorBox(MainFrame.this, "Notice",
					"Please restart OpenPnP for the changes to take effect.");
		}
	};

	private Action millimetersUnitSelected = new AbstractAction(
			LengthUnit.Millimeters.name()) {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			configuration.setSystemUnits(LengthUnit.Millimeters);
			MessageBoxes.errorBox(MainFrame.this, "Notice",
					"Please restart OpenPnP for the changes to take effect.");
		}
	};
	
	private Action quitAction = new AbstractAction("Exit") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			quit();
		}
	};
	
	private Action aboutAction = new AbstractAction("About") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			about();
		}
	};
	
	private JPanel panelInstructions;
	private JPanel panelInstructionActions;
	private JPanel panel_1;
	private JButton btnInstructionsNext;
	private JButton btnInstructionsCancel;
	private JTextPane lblInstructions;
	private JPanel panel_2;
}
