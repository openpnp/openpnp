/*
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.gui;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
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
import java.lang.reflect.Method;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import org.openpnp.gui.components.CameraPanel;
import org.openpnp.gui.importer.BoardImporter;
import org.openpnp.gui.importer.DipTraceImporter;
import org.openpnp.gui.importer.EagleBoardImporter;
import org.openpnp.gui.importer.EagleMountsmdUlpImporter;
import org.openpnp.gui.importer.KicadPosImporter;
import org.openpnp.gui.importer.NamedCSVImporter;
import org.openpnp.gui.importer.SolderPasteGerberImporter;
import org.openpnp.gui.support.HeadCellValue;
import org.openpnp.gui.support.LengthCellValue;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.OSXAdapter;
import org.openpnp.gui.support.RotationCellValue;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.pmw.tinylog.Logger;

import com.jgoodies.common.swing.MnemonicUtils;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

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
    private static final String PREF_WINDOW_STYLE_MULTIPLE = "MainFrame.windowStyleMultiple";
    private static final boolean PREF_WINDOW_STYLE_MULTIPLE_DEF = false;

    private final Configuration configuration;

    private static MainFrame mainFrame;

    private MachineControlsPanel machineControlsPanel;
    private PartsPanel partsPanel;
    private PackagesPanel packagesPanel;
    private FeedersPanel feedersPanel;
    private JobPanel jobPanel;
    private CameraPanel cameraPanel;
    private JPanel panelCameraAndInstructions;
    private JPanel panelMachine;
    private MachineSetupPanel machineSetupPanel;

    public static MainFrame get() {
        return mainFrame;
    }

    public MachineControlsPanel getMachineControls() {
        return machineControlsPanel;
    }

    public PartsPanel getPartsTab() {
        return partsPanel;
    }

    public PackagesPanel getPackagesTab() {
        return packagesPanel;
    }

    public FeedersPanel getFeedersTab() {
        return feedersPanel;
    }

    public JobPanel getJobTab() {
        return jobPanel;
    }

    public CameraPanel getCameraViews() {
        return cameraPanel;
    }

    public MachineSetupPanel getMachineSetupTab() {
        return machineSetupPanel;
    }

    private JPanel contentPane;
    private JTabbedPane tabs;
    private JSplitPane splitPaneMachineAndTabs;
    private TitledBorder panelInstructionsBorder;
    private JPanel panelInstructions;
    private JPanel panelInstructionActions;
    private JPanel panel_1;
    private JButton btnInstructionsNext;
    private JButton btnInstructionsCancel;
    private JTextPane lblInstructions;
    private JPanel panel_2;
    private JMenuBar menuBar;
    private JMenu mnImport;
    private JMenu mnScripts;
    private JMenu mnWindows;

    public JTabbedPane getTabs() {
        return tabs;
    }

    private Preferences prefs = Preferences.userNodeForPackage(MainFrame.class);

    private ActionListener instructionsCancelActionListener;
    private ActionListener instructionsProceedActionListener;

    public MainFrame(Configuration configuration) {
        mainFrame = this;
        this.configuration = configuration;
        LengthCellValue.setConfiguration(configuration);
        RotationCellValue.setConfiguration(configuration);
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
        jobPanel = new JobPanel(configuration, this);
        partsPanel = new PartsPanel(configuration, this);
        packagesPanel = new PackagesPanel(configuration, this);
        feedersPanel = new FeedersPanel(configuration, this);
        machineSetupPanel = new MachineSetupPanel();

        menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        // File
        //////////////////////////////////////////////////////////////////////
        JMenu mnFile = new JMenu("File");
        mnFile.setMnemonic(KeyEvent.VK_F);
        menuBar.add(mnFile);

        mnFile.add(new JMenuItem(jobPanel.newJobAction));
        mnFile.add(new JMenuItem(jobPanel.openJobAction));

        mnFile.add(jobPanel.mnOpenRecent);

        mnFile.addSeparator();
        mnFile.add(new JMenuItem(jobPanel.saveJobAction));
        mnFile.add(new JMenuItem(jobPanel.saveJobAsAction));


        // File -> Import
        //////////////////////////////////////////////////////////////////////
        mnFile.addSeparator();
        mnImport = new JMenu("Import Board");
        mnImport.setMnemonic(KeyEvent.VK_I);
        mnFile.add(mnImport);


        if (!macOsXMenus) {
            mnFile.addSeparator();
            mnFile.add(new JMenuItem(quitAction));
        }

        // Edit
        //////////////////////////////////////////////////////////////////////
        JMenu mnEdit = new JMenu("Edit");
        mnEdit.setMnemonic(KeyEvent.VK_E);
        menuBar.add(mnEdit);

        JMenu mnEditAddBoard = new JMenu(jobPanel.addBoardAction);
        mnEditAddBoard.add(new JMenuItem(jobPanel.addNewBoardAction));
        mnEditAddBoard.add(new JMenuItem(jobPanel.addExistingBoardAction));
        mnEdit.add(mnEditAddBoard);
        mnEdit.add(new JMenuItem(jobPanel.removeBoardAction));
        mnEdit.addSeparator();
        mnEdit.add(new JMenuItem(jobPanel.captureToolBoardLocationAction));

        // View
        //////////////////////////////////////////////////////////////////////
        JMenu mnView = new JMenu("View");
        mnView.setMnemonic(KeyEvent.VK_V);
        menuBar.add(mnView);

        ButtonGroup buttonGroup = new ButtonGroup();

        JMenu mnUnits = new JMenu("System Units");
        mnUnits.setMnemonic(KeyEvent.VK_S);
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
        mnJob.setMnemonic(KeyEvent.VK_J);
        menuBar.add(mnJob);

        mnJob.add(new JMenuItem(jobPanel.startPauseResumeJobAction));
        mnJob.add(new JMenuItem(jobPanel.stepJobAction));
        mnJob.add(new JMenuItem(jobPanel.stopJobAction));

        // Machine
        //////////////////////////////////////////////////////////////////////
        JMenu mnCommands = new JMenu("Machine");
        mnCommands.setMnemonic(KeyEvent.VK_M);
        menuBar.add(mnCommands);
        mnCommands.addSeparator();

        // Scripts
        /////////////////////////////////////////////////////////////////////
        mnScripts = new JMenu("Scripts");
        mnScripts.setMnemonic(KeyEvent.VK_S);
        menuBar.add(mnScripts);

        // Windows
        /////////////////////////////////////////////////////////////////////
        mnWindows = new JMenu("Window");
        mnWindows.setMnemonic(KeyEvent.VK_W);
        menuBar.add(mnWindows);

        JCheckBoxMenuItem windowStyleMultipleMenuItem =
                new JCheckBoxMenuItem(windowStyleMultipleSelected);
        mnWindows.add(windowStyleMultipleMenuItem);
        if (prefs.getBoolean(PREF_WINDOW_STYLE_MULTIPLE, PREF_WINDOW_STYLE_MULTIPLE_DEF)) {
            windowStyleMultipleMenuItem.setSelected(true);
        }

        // Help
        /////////////////////////////////////////////////////////////////////
        JMenu mnHelp = new JMenu("Help");
        mnHelp.setMnemonic(KeyEvent.VK_H);
        menuBar.add(mnHelp);
        if (!macOsXMenus) {
            mnHelp.add(new JMenuItem(aboutAction));
        }
        mnHelp.add(quickStartLinkAction);
        mnHelp.add(setupAndCalibrationLinkAction);
        mnHelp.add(userManualLinkAction);
        mnHelp.addSeparator();
        mnHelp.add(submitDiagnosticsAction);
        if (isInstallerAvailable()) {
            mnHelp.add(new JMenuItem(checkForUpdatesAction));
        }

        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(new BorderLayout(0, 0));

        splitPaneMachineAndTabs = new JSplitPane();
        splitPaneMachineAndTabs.setBorder(null);
        splitPaneMachineAndTabs.setContinuousLayout(true);
        contentPane.add(splitPaneMachineAndTabs, BorderLayout.CENTER);

        panelMachine = new JPanel();
        splitPaneMachineAndTabs.setLeftComponent(panelMachine);
        panelMachine.setLayout(new BorderLayout(0, 0));

        // Add global hotkeys for the arrow keys
        final Map<KeyStroke, Action> hotkeyActionMap = new HashMap<>();

        int mask = KeyEvent.CTRL_DOWN_MASK;

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

        panelCameraAndInstructions = new JPanel();
        panelMachine.add(panelCameraAndInstructions, BorderLayout.CENTER);

        panelInstructions = new JPanel();
        panelInstructions.setVisible(false);
        panelCameraAndInstructions.setLayout(new BorderLayout(0, 0));
        panelInstructions.setBorder(panelInstructionsBorder = new TitledBorder(null, "Instructions",
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
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

        machineControlsPanel = new MachineControlsPanel(configuration, jobPanel);
        panelMachine.add(machineControlsPanel, BorderLayout.SOUTH);

        mnCommands.add(new JMenuItem(machineControlsPanel.homeAction));
        mnCommands.add(new JMenuItem(machineControlsPanel.startStopMachineAction));

        hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, mask),
                machineControlsPanel.getJogControlsPanel().yPlusAction);
        hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, mask),
                machineControlsPanel.getJogControlsPanel().yMinusAction);
        hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, mask),
                machineControlsPanel.getJogControlsPanel().xMinusAction);
        hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, mask),
                machineControlsPanel.getJogControlsPanel().xPlusAction);
        hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_QUOTE, mask),
                machineControlsPanel.getJogControlsPanel().zPlusAction);
        hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, mask),
                machineControlsPanel.getJogControlsPanel().zMinusAction);
        hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, mask),
                machineControlsPanel.getJogControlsPanel().cPlusAction);
        hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD, mask),
                machineControlsPanel.getJogControlsPanel().cMinusAction);
        hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, mask),
                machineControlsPanel.getJogControlsPanel().lowerIncrementAction);
        hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, mask),
                machineControlsPanel.getJogControlsPanel().raiseIncrementAction);

        tabs = new JTabbedPane(JTabbedPane.TOP);
        splitPaneMachineAndTabs.setRightComponent(tabs);

        splitPaneMachineAndTabs
                .setDividerLocation(prefs.getInt(PREF_DIVIDER_POSITION, PREF_DIVIDER_POSITION_DEF));
        splitPaneMachineAndTabs.addPropertyChangeListener("dividerLocation",
                new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        prefs.putInt(PREF_DIVIDER_POSITION,
                                splitPaneMachineAndTabs.getDividerLocation());
                    }
                });

        tabs.addTab("Job", null, jobPanel, null);
        tabs.addTab("Parts", null, partsPanel, null);
        tabs.addTab("Packages", null, packagesPanel, null);
        tabs.addTab("Feeders", null, feedersPanel, null);
        tabs.addTab("Machine Setup", null, machineSetupPanel, null);

        LogPanel logPanel = new LogPanel();
        tabs.addTab("Log", null, logPanel, null);

        panelStatusAndDros = new JPanel();
        panelStatusAndDros.setBorder(null);
        contentPane.add(panelStatusAndDros, BorderLayout.SOUTH);
        panelStatusAndDros.setLayout(new FormLayout(
                new ColumnSpec[] {ColumnSpec.decode("default:grow"), ColumnSpec.decode("8px"),
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {RowSpec.decode("20px"),}));

        lblStatus = new JLabel(" ");
        lblStatus.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
        panelStatusAndDros.add(lblStatus, "1, 1");

        droLbl = new JLabel("X 0000.0000, Y 0000.0000, Z 0000.0000, R 0000.0000");
        droLbl.setOpaque(true);
        droLbl.setFont(new Font("Monospaced", Font.PLAIN, 13));
        droLbl.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
        panelStatusAndDros.add(droLbl, "4, 1");

        cameraPanel.setBorder(new TitledBorder(null, "Cameras", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        panelCameraAndInstructions.add(cameraPanel, BorderLayout.CENTER);

        registerBoardImporters();

        addComponentListener(componentListener);

        try {
            configuration.load();
            configuration.getScripting().setMenu(mnScripts);
        }
        catch (Exception e) {
            e.printStackTrace();
            MessageBoxes.errorBox(this, "Configuration Load Error",
                    "There was a problem loading the configuration. The reason was:<br/><br/>"
                            + e.getMessage() + "<br/><br/>"
                            + "Please check your configuration files and try again. They are located at: "
                            + configuration.getConfigurationDirectory().getAbsolutePath()
                            + "<br/><br/>"
                            + "If you would like to start with a fresh configuration, just delete the entire directory at the location above.<br/><br/>"
                            + "OpenPnP will now exit.");
            System.exit(1);
        }
        splitWindows();
    }

    // 20161222 - ldpgh/lutz_dd
    /**
     * Add multiple windows (aka JFrame) to OpenPnp for the camera (frameCamera) and the machine
     * controls (frameMachineControls).
     *
     * ATTENTION ... the current implementation in MainFrame.java requires a refactoring on the
     * long-term to separate JFrame from JPanels
     */
    public void splitWindows() {
        if (prefs.getBoolean(PREF_WINDOW_STYLE_MULTIPLE, PREF_WINDOW_STYLE_MULTIPLE_DEF)) {
            // pin panelCameraAndInstructions to a separate JFrame
            JDialog frameCamera = new JDialog(this, "OpenPnp - Camera", false);
            // as of today no smart way found to get an adjusted size
            // ... so main window size is used for the camera window
            frameCamera.setSize(getFrames()[0].getSize());
            frameCamera.add(panelCameraAndInstructions);
            frameCamera.setVisible(true);

            // pin machineControlsPanel to a separate JFrame
            JDialog frameMachineControls = new JDialog(this, "OpenPnp - Machine Controls", false);
            // as of today no smart way found to get an adjusted size
            // ... so hardcoded values used (usually not a good idea)
            frameMachineControls.add(machineControlsPanel);
            frameMachineControls.setVisible(true);
            frameMachineControls.pack();

            // move the splitPaneDivider to position 0 to fill the gap of the
            // relocated panels 'panelCameraAndInstructions' & 'machineControlsPanel'
            splitPaneMachineAndTabs.setDividerLocation(0);
        }
        else {
            panelMachine.add(panelCameraAndInstructions, BorderLayout.CENTER);
            // A value of 0 indicates 'multiple window style' was used before.
            if (0 == prefs.getInt(PREF_DIVIDER_POSITION, PREF_DIVIDER_POSITION_DEF)) {
                // Reset the Divider position back to the default value.
                splitPaneMachineAndTabs.setDividerLocation(PREF_DIVIDER_POSITION_DEF);
            }
        }
    }
    
    public boolean isInstallerAvailable() {
        try {
            Class.forName("com.install4j.api.launcher.ApplicationLauncher");
            return true;
        }
        catch (Throwable e) {
            return false;
        }
    }

    public JLabel getDroLabel() {
        return droLbl;
    }

    private void registerBoardImporters() {
        registerBoardImporter(EagleBoardImporter.class);
        registerBoardImporter(EagleMountsmdUlpImporter.class);
        registerBoardImporter(KicadPosImporter.class);
        registerBoardImporter(DipTraceImporter.class);
        registerBoardImporter(NamedCSVImporter.class);
        registerBoardImporter(SolderPasteGerberImporter.class);
    }

    /**
     * Register a BoardImporter with the system, causing it to gain a menu location in the
     * File->Import menu.
     * 
     * @param boardImporterClass
     */
    public void registerBoardImporter(final Class<? extends BoardImporter> boardImporterClass) {
        final BoardImporter boardImporter;
        try {
            boardImporter = boardImporterClass.newInstance();
        }
        catch (Exception e) {
            throw new Error(e);
        }
        JMenuItem menuItem = new JMenuItem(new AbstractAction() {
            {
                putValue(NAME, boardImporter.getImporterName());
                putValue(SHORT_DESCRIPTION, boardImporter.getImporterDescription());
                putValue(MNEMONIC_KEY, KeyEvent.VK_I);
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                jobPanel.importBoard(boardImporterClass);
            }
        });
        mnImport.add(menuItem);
    }

    public void showInstructions(String title, String instructions, boolean showCancelButton,
            boolean showProceedButton, String proceedButtonText,
            ActionListener cancelActionListener, ActionListener proceedActionListener) {
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
        dialog.setSize(750, 550);
        dialog.setLocationRelativeTo(null);
        dialog.setModal(true);
        dialog.setVisible(true);
    }

    public boolean quit() {
        Logger.info("Shutting down...");
        try {
            Preferences.userRoot().flush();
        }
        catch (Exception e) {

        }

        // Save the configuration
        try {
            configuration.save();
        }
        catch (Exception e) {
            String message = "There was a problem saving the configuration. The reason was:\n\n"
                    + e.getMessage() + "\n\nDo you want to quit without saving?";
            message = message.replaceAll("\n", "<br/>");
            message = message.replaceAll("\r", "");
            message = "<html><body width=\"400\">" + message + "</body></html>";
            int result = JOptionPane.showConfirmDialog(this, message, "Configuration Save Error",
                    JOptionPane.YES_NO_OPTION);
            if (result != JOptionPane.YES_OPTION) {
                return false;
            }
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
        // Attempt to stop the machine on quit
        try {
            configuration.getMachine().close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        Logger.info("Shutdown complete, exiting.");
        System.exit(0);
        return true;
    }

    public void setStatus(String status) {
        SwingUtilities.invokeLater(() -> {
            lblStatus.setText(status);
        });
    }

    public void showTab(String title) {
        int index = tabs.indexOfTab(title);
        tabs.setSelectedIndex(index);
    }

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

    private Action inchesUnitSelected = new AbstractAction(LengthUnit.Inches.name()) {
        {
            putValue(MNEMONIC_KEY, KeyEvent.VK_I);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            configuration.setSystemUnits(LengthUnit.Inches);
            MessageBoxes.infoBox("Notice",
                    "Please restart OpenPnP for the changes to take effect.");
        }
    };

    private Action millimetersUnitSelected = new AbstractAction(LengthUnit.Millimeters.name()) {
        {
            putValue(MNEMONIC_KEY, KeyEvent.VK_M);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            configuration.setSystemUnits(LengthUnit.Millimeters);
            MessageBoxes.infoBox("Notice",
                    "Please restart OpenPnP for the changes to take effect.");
        }
    };

    private Action windowStyleMultipleSelected = new AbstractAction("Multiple Window Style") {
        {
            putValue(MNEMONIC_KEY, KeyEvent.VK_M);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (mnWindows.getItem(0).isSelected()) {
                prefs.putBoolean(PREF_WINDOW_STYLE_MULTIPLE, true);
            }
            else {
                prefs.putBoolean(PREF_WINDOW_STYLE_MULTIPLE, false);
            }
            MessageBoxes.infoBox("Windows Style Changed",
                    "Window style has been changed. Please restart OpenPnP to see the changes.");
        }
    };

    private Action quitAction = new AbstractAction("Exit") {
        {
            putValue(MNEMONIC_KEY, KeyEvent.VK_X);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            quit();
        }
    };

    private Action aboutAction = new AbstractAction("About") {
        {
            putValue(MNEMONIC_KEY, KeyEvent.VK_A);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            about();
        }
    };
    
    private Action checkForUpdatesAction = new AbstractAction("Check For Updates...") {
        {
            putValue(MNEMONIC_KEY, KeyEvent.VK_U);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            try {
                Class applicationLauncher = Class.forName("com.install4j.api.launcher.ApplicationLauncher");
                Class callback = Class.forName("com.install4j.api.launcher.ApplicationLauncher$Callback");
                Method launchApplication = applicationLauncher.getMethod("launchApplication", String.class, String[].class, boolean.class, callback);
                launchApplication.invoke(null, "125", null, false, null);
            }
            catch (Exception e) {
                MessageBoxes.errorBox(MainFrame.this, "Unable to launch update application.", e);
            }
        }
    };
    
    private Action quickStartLinkAction = new AbstractAction("Quick Start") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            String uri = "https://github.com/openpnp/openpnp/wiki/Quick-Start";
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(new URI(uri));
                }
                else {
                    throw new Exception("Not supported.");
                }
            }
            catch (Exception e) {
                MessageBoxes.errorBox(MainFrame.this, "Unable to launch default browser.", "Unable to launch default browser. Please visit " + uri);
            }
        }
    };
    
    private Action setupAndCalibrationLinkAction = new AbstractAction("Setup and Calibration") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            String uri = "https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration";
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(new URI(uri));
                }
                else {
                    throw new Exception("Not supported.");
                }
            }
            catch (Exception e) {
                MessageBoxes.errorBox(MainFrame.this, "Unable to launch default browser.", "Unable to launch default browser. Please visit " + uri);
            }
        }
    };
    
    private Action userManualLinkAction = new AbstractAction("User Manual") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            String uri = "https://github.com/openpnp/openpnp/wiki/User-Manual";
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(new URI(uri));
                }
                else {
                    throw new Exception("Not supported.");
                }
            }
            catch (Exception e) {
                MessageBoxes.errorBox(MainFrame.this, "Unable to launch default browser.", "Unable to launch default browser. Please visit " + uri);
            }
        }
    };
    
    private Action submitDiagnosticsAction = new AbstractAction("Submit Diagnostics") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            SubmitDiagnosticsDialog dialog = new SubmitDiagnosticsDialog();
            dialog.setModal(true);
            dialog.setSize(620, 720);
            dialog.setLocationRelativeTo(MainFrame.get());
            dialog.setVisible(true);
        }
    };
    
    private JPanel panelStatusAndDros;
    private JLabel droLbl;
    private JLabel lblStatus;
}
