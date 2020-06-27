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
import java.awt.Dimension;
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
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import org.openpnp.Translations;
import org.openpnp.gui.components.CameraPanel;
import org.openpnp.gui.importer.BoardImporter;
import org.openpnp.gui.importer.DipTraceImporter;
import org.openpnp.gui.importer.EagleBoardImporter;
import org.openpnp.gui.importer.EagleMountsmdUlpImporter;
import org.openpnp.gui.importer.KicadPosImporter;
import org.openpnp.gui.importer.LabcenterProteusImporter; //
import org.openpnp.gui.importer.NamedCSVImporter;
import org.openpnp.gui.support.HeadCellValue;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.LengthCellValue;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.OSXAdapter;
import org.openpnp.gui.support.RotationCellValue;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.pmw.tinylog.Logger;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

/**
 * The main window of the application.
 */
@SuppressWarnings("serial")
public class MainFrame extends JFrame {
    private static final String PREF_WINDOW_X = "MainFrame.windowX"; //$NON-NLS-1$
    private static final int PREF_WINDOW_X_DEF = 0;
    private static final String PREF_WINDOW_Y = "MainFrame.windowY"; //$NON-NLS-1$
    private static final int PREF_WINDOW_Y_DEF = 0;
    private static final String PREF_WINDOW_WIDTH = "MainFrame.windowWidth"; //$NON-NLS-1$
    private static final int PREF_WINDOW_WIDTH_DEF = 1024;
    private static final String PREF_WINDOW_HEIGHT = "MainFrame.windowHeight"; //$NON-NLS-1$
    private static final int PREF_WINDOW_HEIGHT_DEF = 768;
    private static final String PREF_DIVIDER_POSITION = "MainFrame.dividerPosition"; //$NON-NLS-1$
    private static final int PREF_DIVIDER_POSITION_DEF = -1;
    private static final String PREF_WINDOW_STYLE_MULTIPLE = "MainFrame.windowStyleMultiple"; //$NON-NLS-1$
    private static final boolean PREF_WINDOW_STYLE_MULTIPLE_DEF = false;

    private static final String PREF_CAMERA_WINDOW_X = "CameraFrame.windowX"; //$NON-NLS-1$
    private static final int PREF_CAMERA_WINDOW_X_DEF = 0;
    private static final String PREF_CAMERA_WINDOW_Y = "CameraFrame.windowY"; //$NON-NLS-1$
    private static final int PREF_CAMERA_WINDOW_Y_DEF = 0;
    private static final String PREF_CAMERA_WINDOW_WIDTH = "CameraFrame.windowWidth"; //$NON-NLS-1$
    private static final int PREF_CAMERA_WINDOW_WIDTH_DEF = 800;
    private static final String PREF_CAMERA_WINDOW_HEIGHT = "CameraFrame.windowHeight"; //$NON-NLS-1$
    private static final int PREF_CAMERA_WINDOW_HEIGHT_DEF = 600;

    private static final String PREF_MACHINECONTROLS_WINDOW_X = "MachineControlsFrame.windowX"; //$NON-NLS-1$
    private static final int PREF_MACHINECONTROLS_WINDOW_X_DEF = 0;
    private static final String PREF_MACHINECONTROLS_WINDOW_Y = "MachineControlsFrame.windowY"; //$NON-NLS-1$
    private static final int PREF_MACHINECONTROLS_WINDOW_Y_DEF = 0;
    private static final String PREF_MACHINECONTROLS_WINDOW_WIDTH = "MachineControlsFrame.windowWidth"; //$NON-NLS-1$
    private static final int PREF_MACHINECONTROLS_WINDOW_WIDTH_DEF = 490;
    private static final String PREF_MACHINECONTROLS_WINDOW_HEIGHT = "MachineControlsFrame.windowHeight"; //$NON-NLS-1$
    private static final int PREF_MACHINECONTROLS_WINDOW_HEIGHT_DEF = 340;

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
    private JDialog frameCamera;
    private JDialog frameMachineControls;
    private Map<KeyStroke, Action> hotkeyActionMap;
    
    private UndoManager undoManager = new UndoManager();

    public static MainFrame get() {
        return mainFrame;
    }
    
    public UndoManager getUndoManager() {
        return undoManager;
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
    private ScheduledExecutorService scheduledExecutor;
    private JMenuBar menuBar;
    private JMenu mnImport;
    private JMenu mnScripts;
    private JMenu mnWindows;

    public JTabbedPane getTabs() {
        return tabs;
    }

    public Map<KeyStroke, Action> getHotkeyActionMap() {
        return hotkeyActionMap;
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
        JMenu mnFile = new JMenu(Translations.getString("Menu.File")); //$NON-NLS-1$
        mnFile.setMnemonic(KeyEvent.VK_F);
        menuBar.add(mnFile);

        mnFile.add(new JMenuItem(jobPanel.newJobAction));
        mnFile.add(new JMenuItem(jobPanel.openJobAction));

        mnFile.add(jobPanel.mnOpenRecent);

        mnFile.addSeparator();
        mnFile.add(new JMenuItem(jobPanel.saveJobAction));
        mnFile.add(new JMenuItem(jobPanel.saveJobAsAction));
        mnFile.addSeparator();
        mnFile.add(new JMenuItem(saveConfigAction));


        // File -> Import
        //////////////////////////////////////////////////////////////////////
        mnFile.addSeparator();
        mnImport = new JMenu(Translations.getString("Menu.File.ImportBoard")); //$NON-NLS-1$
        mnImport.setMnemonic(KeyEvent.VK_I);
        mnFile.add(mnImport);


        if (!macOsXMenus) {
            mnFile.addSeparator();
            mnFile.add(new JMenuItem(quitAction));
        }

        // Edit
        //////////////////////////////////////////////////////////////////////
        JMenu mnEdit = new JMenu(Translations.getString("Menu.Edit")); //$NON-NLS-1$
        mnEdit.setMnemonic(KeyEvent.VK_E);
        menuBar.add(mnEdit);

        mnEdit.add(new JMenuItem(undoAction));
        mnEdit.add(new JMenuItem(redoAction));
        mnEdit.addSeparator();
        JMenu mnEditAddBoard = new JMenu(jobPanel.addBoardAction);
        mnEditAddBoard.add(new JMenuItem(jobPanel.addNewBoardAction));
        mnEditAddBoard.add(new JMenuItem(jobPanel.addExistingBoardAction));
        mnEdit.add(mnEditAddBoard);
        mnEdit.add(new JMenuItem(jobPanel.removeBoardAction));
        mnEdit.addSeparator();
        mnEdit.add(new JMenuItem(jobPanel.captureToolBoardLocationAction));

        // View
        //////////////////////////////////////////////////////////////////////
        JMenu mnView = new JMenu(Translations.getString("Menu.View")); //$NON-NLS-1$
        mnView.setMnemonic(KeyEvent.VK_V);
        menuBar.add(mnView);

        // View -> System Units
        ButtonGroup buttonGroup = new ButtonGroup();
        JMenu mnUnits = new JMenu(Translations.getString("Menu.View.SystemUnits")); //$NON-NLS-1$
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
        
        // View -> Language
        buttonGroup = new ButtonGroup();
        JMenu mnLanguage = new JMenu(Translations.getString("Menu.View.Language")); //$NON-NLS-1$
        mnView.add(mnLanguage);

        menuItem = new JCheckBoxMenuItem(new LanguageSelectionAction(Locale.US));
        buttonGroup.add(menuItem);
        mnLanguage.add(menuItem);
        
        menuItem = new JCheckBoxMenuItem(new LanguageSelectionAction(new Locale("ru")));
        buttonGroup.add(menuItem);
        mnLanguage.add(menuItem);

        menuItem = new JCheckBoxMenuItem(new LanguageSelectionAction(new Locale("es")));
        buttonGroup.add(menuItem);
        mnLanguage.add(menuItem);

        menuItem = new JCheckBoxMenuItem(new LanguageSelectionAction(new Locale("fr")));
        buttonGroup.add(menuItem);
        mnLanguage.add(menuItem);
		
        menuItem = new JCheckBoxMenuItem(new LanguageSelectionAction(new Locale("it")));
        buttonGroup.add(menuItem);
        mnLanguage.add(menuItem);

        menuItem = new JCheckBoxMenuItem(new LanguageSelectionAction(new Locale("de")));
        buttonGroup.add(menuItem);
        mnLanguage.add(menuItem);

        for (int i = 0; i < mnLanguage.getItemCount(); i++) {
            JCheckBoxMenuItem item = (JCheckBoxMenuItem) mnLanguage.getItem(i);
            LanguageSelectionAction action = (LanguageSelectionAction) item.getAction();
            if (action.getLocale().equals(Configuration.get().getLocale())) {
                item.setSelected(true);
            }
        }
        

        // Job
        //////////////////////////////////////////////////////////////////////
        JMenu mnJob = new JMenu(Translations.getString("Menu.Job")); //$NON-NLS-1$
        mnJob.setMnemonic(KeyEvent.VK_J);
        menuBar.add(mnJob);

        mnJob.add(new JMenuItem(jobPanel.startPauseResumeJobAction));
        mnJob.add(new JMenuItem(jobPanel.stepJobAction));
        mnJob.add(new JMenuItem(jobPanel.stopJobAction));
        
        mnJob.addSeparator();
        
        mnJob.add(new JMenuItem(jobPanel.resetAllPlacedAction));

        // Machine
        //////////////////////////////////////////////////////////////////////
        JMenu mnCommands = new JMenu(Translations.getString("Menu.Machine")); //$NON-NLS-1$
        mnCommands.setMnemonic(KeyEvent.VK_M);
        menuBar.add(mnCommands);
        mnCommands.addSeparator();

        // Scripts
        /////////////////////////////////////////////////////////////////////
        mnScripts = new JMenu(Translations.getString("Menu.Scripts")); //$NON-NLS-1$
        mnScripts.setMnemonic(KeyEvent.VK_S);
        menuBar.add(mnScripts);

        // Windows
        /////////////////////////////////////////////////////////////////////
        mnWindows = new JMenu(Translations.getString("Menu.Window")); //$NON-NLS-1$
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
        JMenu mnHelp = new JMenu(Translations.getString("Menu.Help")); //$NON-NLS-1$
        mnHelp.setMnemonic(KeyEvent.VK_H);
        menuBar.add(mnHelp);
        if (!macOsXMenus) {
            mnHelp.add(new JMenuItem(aboutAction));
        }
        mnHelp.add(quickStartLinkAction);
        mnHelp.add(setupAndCalibrationLinkAction);
        mnHelp.add(userManualLinkAction);
        mnHelp.addSeparator();
        mnHelp.add(changeLogAction);
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
        hotkeyActionMap = new HashMap<>();

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
        panelInstructions.setBorder(panelInstructionsBorder = new TitledBorder(null, Translations.getString("General.Instructions"), //$NON-NLS-1$
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

        btnInstructionsCancel = new JButton(Translations.getString("General.Cancel")); //$NON-NLS-1$
        btnInstructionsCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                if (instructionsCancelActionListener != null) {
                    instructionsCancelActionListener.actionPerformed(arg0);
                }
            }
        });
        panel_2.add(btnInstructionsCancel);

        btnInstructionsNext = new JButton(Translations.getString("General.Next")); //$NON-NLS-1$
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
        // does not seem to work with html
        //lblInstructions.setFont(new Font("Lucida Grande", Font.PLAIN, 14)); //$NON-NLS-1$
        // instead use the HONOR_DISPLAY_PROPERTIES to set the proper system dialog font and size 
        lblInstructions.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        lblInstructions.setBackground(UIManager.getColor("Panel.background")); //$NON-NLS-1$
        lblInstructions.setContentType("text/html"); //$NON-NLS-1$
        lblInstructions.setEditable(false);
        panel_1.add(lblInstructions);

        labelIcon = new JLabel(); 
        labelIcon.setIcon(Icons.processActivity1Icon);
        panelInstructions.add(labelIcon, BorderLayout.WEST);

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
        hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_H, mask),
                machineControlsPanel.homeAction);
        hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK),
                jobPanel.startPauseResumeJobAction); // Ctrl-Shift-R for Start
        hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK),
                jobPanel.stepJobAction); // Ctrl-Shift-S for Step
        hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK),
                jobPanel.stopJobAction); // Ctrl-Shift-A for Stop
        hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK),
                machineControlsPanel.getJogControlsPanel().xyParkAction); // Ctrl-Shift-P for xyPark
        hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK),
                machineControlsPanel.getJogControlsPanel().zParkAction); // Ctrl-Shift-P for zPark
        hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK),
                machineControlsPanel.getJogControlsPanel().safezAction); // Ctrl-Shift-Z for safezAction
        hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK),
                machineControlsPanel.getJogControlsPanel().discardAction); // Ctrl-Shift-D for discard
        hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F1, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK),
                machineControlsPanel.getJogControlsPanel().setIncrement1Action);
        hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK),
                machineControlsPanel.getJogControlsPanel().setIncrement2Action);
        hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK),
                machineControlsPanel.getJogControlsPanel().setIncrement3Action);
        hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F4, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK),
                machineControlsPanel.getJogControlsPanel().setIncrement4Action);
        hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK),
                machineControlsPanel.getJogControlsPanel().setIncrement5Action);
				
        tabs = new JTabbedPane(JTabbedPane.TOP);
        splitPaneMachineAndTabs.setRightComponent(tabs);

        splitPaneMachineAndTabs
                .setDividerLocation(prefs.getInt(PREF_DIVIDER_POSITION, PREF_DIVIDER_POSITION_DEF));
        splitPaneMachineAndTabs.addPropertyChangeListener("dividerLocation", //$NON-NLS-1$
                new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        prefs.putInt(PREF_DIVIDER_POSITION,
                                splitPaneMachineAndTabs.getDividerLocation());
                    }
                });

        tabs.addTab("Job", null, jobPanel, null); //$NON-NLS-1$
        tabs.addTab("Parts", null, partsPanel, null); //$NON-NLS-1$
        tabs.addTab("Packages", null, packagesPanel, null); //$NON-NLS-1$
        tabs.addTab("Feeders", null, feedersPanel, null); //$NON-NLS-1$
        tabs.addTab("Machine Setup", null, machineSetupPanel, null); //$NON-NLS-1$

        LogPanel logPanel = new LogPanel();
        tabs.addTab("Log", null, logPanel, null); //$NON-NLS-1$

        panelStatusAndDros = new JPanel();
        panelStatusAndDros.setBorder(null);
        contentPane.add(panelStatusAndDros, BorderLayout.SOUTH);
        panelStatusAndDros.setLayout(new FormLayout(
                new ColumnSpec[] {ColumnSpec.decode("default:grow"), ColumnSpec.decode("8px"), //$NON-NLS-1$ //$NON-NLS-2$
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC, 
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {RowSpec.decode("20px"),})); //$NON-NLS-1$

        
        // Status Information
        lblStatus = new JLabel(" "); //$NON-NLS-1$
        lblStatus.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
        panelStatusAndDros.add(lblStatus, "1, 1"); //$NON-NLS-1$
        
        
        // Placement Information
        lblPlacements = new JLabel(" Placements: 0 / 0 Total | 0 / 0 Selected Board "); //$NON-NLS-1$
        lblPlacements.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
        panelStatusAndDros.add(lblPlacements, "4, 1"); //$NON-NLS-1$
        
        
        // Placements Progress Bar
        prgbrPlacements = new JProgressBar();
        prgbrPlacements.setMinimum(0);
        prgbrPlacements.setMaximum(100);
        prgbrPlacements.setStringPainted(true);
        prgbrPlacements.setPreferredSize(new Dimension(200, 16));
        prgbrPlacements.setValue(0);
        panelStatusAndDros.add(prgbrPlacements, "6, 1"); //$NON-NLS-1$

        
        // DRO 
        droLbl = new JLabel("X 0000.0000, Y 0000.0000, Z 0000.0000, R 0000.0000"); //$NON-NLS-1$
        droLbl.setOpaque(true);
        droLbl.setFont(new Font("Monospaced", Font.PLAIN, 13)); //$NON-NLS-1$
        droLbl.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
        panelStatusAndDros.add(droLbl, "8, 1"); //$NON-NLS-1$

        cameraPanel.setBorder(new TitledBorder(null, "Cameras", TitledBorder.LEADING, //$NON-NLS-1$
                TitledBorder.TOP, null, null));
        panelCameraAndInstructions.add(cameraPanel, BorderLayout.CENTER);

        registerBoardImporters();

        addComponentListener(componentListener);
        
        boolean configurationLoaded = false;
        while (!configurationLoaded) {
	        try {
	            configuration.load();
	            configuration.getScripting().setMenu(mnScripts);
	            
	            if (Configuration.get().getMachine().getProperty("Welcome2_0_Dialog_Shown") == null) {
	                Welcome2_0Dialog dialog = new Welcome2_0Dialog(this);
	                dialog.setSize(750, 550);
	                dialog.setLocationRelativeTo(null);
	                dialog.setModal(true);
	                dialog.setVisible(true);
	                Configuration.get().getMachine().setProperty("Welcome2_0_Dialog_Shown", true);
	            }
	            configurationLoaded = true;    
	        }
	        catch (Exception e) {
	            e.printStackTrace();
	            if (!MessageBoxes.errorBoxWithRetry(this, "Configuration Load Error", //$NON-NLS-1$
	                    "There was a problem loading the configuration. The reason was:<br/><br/>" //$NON-NLS-1$
	                            + e.getMessage() + "<br/><br/>" //$NON-NLS-1$
	                            + "Please check your configuration files and try again. They are located at: " //$NON-NLS-1$
	                            + configuration.getConfigurationDirectory().getAbsolutePath()
	                            + "<br/><br/>" //$NON-NLS-1$
	                            + "If you would like to start with a fresh configuration, just delete the entire directory at the location above.<br/><br/>" //$NON-NLS-1$
	                            + "Retry loading (else openpnp will exit) ?")) { //$NON-NLS-1$
	            	System.exit(1);
	            }
	        }
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
            frameCamera = new JDialog(this, "OpenPnp - Camera", false); //$NON-NLS-1$
            // as of today no smart way found to get an adjusted size
            // ... so main window size is used for the camera window
            frameCamera.getContentPane().add(panelCameraAndInstructions);
            frameCamera.setVisible(true);
            frameCamera.addComponentListener(cameraWindowListener);

            if (prefs.getInt(PREF_CAMERA_WINDOW_WIDTH, 50) < 50) {
                prefs.putInt(PREF_CAMERA_WINDOW_WIDTH, PREF_CAMERA_WINDOW_WIDTH_DEF);
            }

            if (prefs.getInt(PREF_CAMERA_WINDOW_HEIGHT, 50) < 50) {
                prefs.putInt(PREF_CAMERA_WINDOW_HEIGHT, PREF_CAMERA_WINDOW_HEIGHT_DEF);
            }

            frameCamera.setBounds(prefs.getInt(PREF_CAMERA_WINDOW_X, PREF_CAMERA_WINDOW_X_DEF),
                    prefs.getInt(PREF_CAMERA_WINDOW_Y, PREF_CAMERA_WINDOW_Y_DEF),
                    prefs.getInt(PREF_CAMERA_WINDOW_WIDTH, PREF_CAMERA_WINDOW_WIDTH_DEF),
                    prefs.getInt(PREF_CAMERA_WINDOW_HEIGHT, PREF_CAMERA_WINDOW_HEIGHT_DEF));

            // pin machineControlsPanel to a separate JFrame
            frameMachineControls = new JDialog(this, "OpenPnp - Machine Controls", false); //$NON-NLS-1$
            // as of today no smart way found to get an adjusted size
            // ... so hardcoded values used (usually not a good idea)
            frameMachineControls.getContentPane().add(machineControlsPanel);
            frameMachineControls.setVisible(true);
            frameMachineControls.pack();
            frameMachineControls.addComponentListener(machineControlsWindowListener);

            if (prefs.getInt(PREF_MACHINECONTROLS_WINDOW_WIDTH, 50) < 50) {
                prefs.putInt(PREF_MACHINECONTROLS_WINDOW_WIDTH, PREF_MACHINECONTROLS_WINDOW_WIDTH_DEF);
            }

            if (prefs.getInt(PREF_MACHINECONTROLS_WINDOW_HEIGHT, 50) < 50) {
                prefs.putInt(PREF_MACHINECONTROLS_WINDOW_HEIGHT, PREF_MACHINECONTROLS_WINDOW_HEIGHT_DEF);
            }

            frameMachineControls.setBounds(prefs.getInt(PREF_MACHINECONTROLS_WINDOW_X, PREF_MACHINECONTROLS_WINDOW_X_DEF),
                    prefs.getInt(PREF_MACHINECONTROLS_WINDOW_Y, PREF_MACHINECONTROLS_WINDOW_Y_DEF),
                    prefs.getInt(PREF_MACHINECONTROLS_WINDOW_WIDTH, PREF_MACHINECONTROLS_WINDOW_WIDTH_DEF),
                    prefs.getInt(PREF_MACHINECONTROLS_WINDOW_HEIGHT, PREF_MACHINECONTROLS_WINDOW_HEIGHT_DEF));
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
            Class.forName("com.install4j.api.launcher.ApplicationLauncher"); //$NON-NLS-1$
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
    	registerBoardImporter(LabcenterProteusImporter.class);
        registerBoardImporter(EagleBoardImporter.class);
        registerBoardImporter(EagleMountsmdUlpImporter.class);
        registerBoardImporter(KicadPosImporter.class);
        registerBoardImporter(DipTraceImporter.class);
        registerBoardImporter(NamedCSVImporter.class);
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
        if (scheduledExecutor == null) {
            scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
            scheduledExecutor.scheduleAtFixedRate(new Runnable() {
                public void run() {
                    labelIcon.setIcon(labelIcon.getIcon() == Icons.processActivity1Icon ? Icons.processActivity2Icon : Icons.processActivity1Icon);
                }
            }, 0, 1000, TimeUnit.MILLISECONDS);
        }
    }

    public void hideInstructions() {
        scheduledExecutor.shutdown();
        scheduledExecutor = null;
        panelInstructions.setVisible(false);
        doLayout();
    }

    public boolean registerForMacOSXEvents() {
        if ((System.getProperty("os.name").toLowerCase().startsWith("mac os x"))) { //$NON-NLS-1$ //$NON-NLS-2$
            try {
                // Generate and register the OSXAdapter, passing it a hash of
                // all the methods we wish to
                // use as delegates for various
                // com.apple.eawt.ApplicationListener methods
                OSXAdapter.setQuitHandler(this,
                        getClass().getDeclaredMethod("quit", (Class[]) null)); //$NON-NLS-1$
                OSXAdapter.setAboutHandler(this,
                        getClass().getDeclaredMethod("about", (Class[]) null)); //$NON-NLS-1$
                // OSXAdapter.setPreferencesHandler(this, getClass()
                // .getDeclaredMethod("preferences", (Class[]) null));
                // OSXAdapter.setFileHandler(
                // this,
                // getClass().getDeclaredMethod("loadImageFile",
                // new Class[] { String.class }));
                return true;
            }
            catch (Exception e) {
                System.err.println("Error while loading the OSXAdapter:"); //$NON-NLS-1$
                e.printStackTrace();
            }
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

    public boolean saveConfig() {
        // Save the configuration
        try {
            Preferences.userRoot().flush();
        }
        catch (Exception e) {
            MessageBoxes.errorBox(MainFrame.this, "Save Preferences", e); //$NON-NLS-1$
        }
        
        try {
            configuration.save();
        }
        catch (Exception e) {
			String message = "There was a problem saving the configuration. The reason was:\n\n" + e.getMessage() //$NON-NLS-1$
					+ "\n\n"; //$NON-NLS-1$
			message = message.replaceAll("\n", "<br/>"); //$NON-NLS-1$ //$NON-NLS-2$
			message = message.replaceAll("\r", ""); //$NON-NLS-1$ //$NON-NLS-2$
			message = "<html><body width=\"400\">" + message + "</body></html>"; //$NON-NLS-1$ //$NON-NLS-2$
			JOptionPane.showMessageDialog(this, message, "Configuration Save Error", JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
			return false;
        }

        Logger.debug("Config saved successfully!"); //$NON-NLS-1$
        return true;
    }

    public boolean quit() {
        Logger.info("Shutting down..."); //$NON-NLS-1$
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
            String message = "There was a problem saving the configuration. The reason was:\n\n" //$NON-NLS-1$
                    + e.getMessage() + "\n\nDo you want to quit without saving?"; //$NON-NLS-1$
            message = message.replaceAll("\n", "<br/>"); //$NON-NLS-1$ //$NON-NLS-2$
            message = message.replaceAll("\r", ""); //$NON-NLS-1$ //$NON-NLS-2$
            message = "<html><body width=\"400\">" + message + "</body></html>"; //$NON-NLS-1$ //$NON-NLS-2$
            int result = JOptionPane.showConfirmDialog(this, message, "Configuration Save Error", //$NON-NLS-1$
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
        Logger.info("Shutdown complete, exiting."); //$NON-NLS-1$
        System.exit(0);
        return true;
    }

    public void setStatus(String status) {
        SwingUtilities.invokeLater(() -> {
            lblStatus.setText(status);
        });
    }
    
    public void setPlacementCompletionStatus(int totalPlacementsCompleted, int totalPlacements, int boardPlacementsCompleted, int boardPlacements) {
        SwingUtilities.invokeLater(() -> {
            lblPlacements.setText(String.format(" Placements: %d / %d Total | %d / %d Selected Board ", totalPlacementsCompleted, totalPlacements, boardPlacementsCompleted, boardPlacements));
        	prgbrPlacements.setValue((int)(((float)totalPlacementsCompleted / (float)totalPlacements) * 100.0f));
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

    private ComponentListener cameraWindowListener = new ComponentAdapter() {
        @Override
        public void componentMoved(ComponentEvent e) {
            prefs.putInt(PREF_CAMERA_WINDOW_X, frameCamera.getLocation().x);
            prefs.putInt(PREF_CAMERA_WINDOW_Y, frameCamera.getLocation().y);
        }

        @Override
        public void componentResized(ComponentEvent e) {
            prefs.putInt(PREF_CAMERA_WINDOW_WIDTH, frameCamera.getSize().width);
            prefs.putInt(PREF_CAMERA_WINDOW_HEIGHT, frameCamera.getSize().height);
        }
    };

    private ComponentListener machineControlsWindowListener = new ComponentAdapter() {
        @Override
        public void componentMoved(ComponentEvent e) {
            prefs.putInt(PREF_MACHINECONTROLS_WINDOW_X, frameMachineControls.getLocation().x);
            prefs.putInt(PREF_MACHINECONTROLS_WINDOW_Y, frameMachineControls.getLocation().y);
        }

        @Override
        public void componentResized(ComponentEvent e) {
            prefs.putInt(PREF_MACHINECONTROLS_WINDOW_WIDTH, frameMachineControls.getSize().width);
            prefs.putInt(PREF_MACHINECONTROLS_WINDOW_HEIGHT, frameMachineControls.getSize().height);
        }
    };
    
    private Action inchesUnitSelected = new AbstractAction(LengthUnit.Inches.name()) {
        {
            putValue(MNEMONIC_KEY, KeyEvent.VK_I);
        }
        
        @Override
        public void actionPerformed(ActionEvent arg0) {
            configuration.setSystemUnits(LengthUnit.Inches);
          MessageBoxes.infoBox("Notice", //$NON-NLS-1$
                  "Please restart OpenPnP for the changes to take effect."); //$NON-NLS-1$
      }
    };

    private Action millimetersUnitSelected = new AbstractAction(LengthUnit.Millimeters.name()) {
        {
            putValue(MNEMONIC_KEY, KeyEvent.VK_M);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            configuration.setSystemUnits(LengthUnit.Millimeters);
            MessageBoxes.infoBox("Notice", //$NON-NLS-1$
                    "Please restart OpenPnP for the changes to take effect."); //$NON-NLS-1$
        }
    };

    private Action windowStyleMultipleSelected = new AbstractAction(Translations.getString("Menu.Window.MultipleStyle")) { //$NON-NLS-1$
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
            MessageBoxes.infoBox("Windows Style Changed", //$NON-NLS-1$
                    "Window style has been changed. Please restart OpenPnP to see the changes."); //$NON-NLS-1$
        }
    };

    private Action saveConfigAction = new AbstractAction(Translations.getString("Menu.File.SaveConfiguration")) { //$NON-NLS-1$
        @Override
        public void actionPerformed(ActionEvent arg0) {
			saveConfig();
        }
    };

    private Action quitAction = new AbstractAction(Translations.getString("Menu.File.Exit")) { //$NON-NLS-1$
        {
            putValue(MNEMONIC_KEY, KeyEvent.VK_X);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            quit();
        }
    };

    private Action aboutAction = new AbstractAction(Translations.getString("Menu.Help.About")) { //$NON-NLS-1$
        {
            putValue(MNEMONIC_KEY, KeyEvent.VK_A);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            about();
        }
    };
    
    private Action checkForUpdatesAction = new AbstractAction(Translations.getString("Menu.Help.CheckForUpdates")) { //$NON-NLS-1$
        {
            putValue(MNEMONIC_KEY, KeyEvent.VK_U);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            try {
                Class applicationLauncher = Class.forName("com.install4j.api.launcher.ApplicationLauncher"); //$NON-NLS-1$
                Class callback = Class.forName("com.install4j.api.launcher.ApplicationLauncher$Callback"); //$NON-NLS-1$
                Method launchApplication = applicationLauncher.getMethod("launchApplication", String.class, String[].class, boolean.class, callback); //$NON-NLS-1$
                launchApplication.invoke(null, "125", null, false, null); //$NON-NLS-1$
            }
            catch (Exception e) {
                MessageBoxes.errorBox(MainFrame.this, "Unable to launch update application.", e); //$NON-NLS-1$
            }
        }
    };
    
    private Action quickStartLinkAction = new AbstractAction(Translations.getString("Menu.Help.QuickStart")) { //$NON-NLS-1$
        @Override
        public void actionPerformed(ActionEvent arg0) {
            String uri = "https://github.com/openpnp/openpnp/wiki/Quick-Start"; //$NON-NLS-1$
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(new URI(uri));
                }
                else {
                    throw new Exception("Not supported."); //$NON-NLS-1$
                }
            }
            catch (Exception e) {
                MessageBoxes.errorBox(MainFrame.this, "Unable to launch default browser.", "Unable to launch default browser. Please visit " + uri); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
    };
    
    private Action setupAndCalibrationLinkAction = new AbstractAction(Translations.getString("Menu.Help.SetupAndCalibration")) { //$NON-NLS-1$
        @Override
        public void actionPerformed(ActionEvent arg0) {
            String uri = "https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration"; //$NON-NLS-1$
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(new URI(uri));
                }
                else {
                    throw new Exception("Not supported."); //$NON-NLS-1$
                }
            }
            catch (Exception e) {
                MessageBoxes.errorBox(MainFrame.this, "Unable to launch default browser.", "Unable to launch default browser. Please visit " + uri); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
    };
    
    private Action userManualLinkAction = new AbstractAction(Translations.getString("Menu.Help.UserManual")) { //$NON-NLS-1$
        @Override
        public void actionPerformed(ActionEvent arg0) {
            String uri = "https://github.com/openpnp/openpnp/wiki/User-Manual"; //$NON-NLS-1$
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(new URI(uri));
                }
                else {
                    throw new Exception("Not supported."); //$NON-NLS-1$
                }
            }
            catch (Exception e) {
                MessageBoxes.errorBox(MainFrame.this, "Unable to launch default browser.", "Unable to launch default browser. Please visit " + uri); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
    };
    
    private Action changeLogAction = new AbstractAction(Translations.getString("Menu.Help.ChangeLog")) { //$NON-NLS-1$
        @Override
        public void actionPerformed(ActionEvent arg0) {
            String uri = "https://github.com/openpnp/openpnp/blob/develop/CHANGES.md"; //$NON-NLS-1$
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(new URI(uri));
                }
                else {
                    throw new Exception("Not supported."); //$NON-NLS-1$
                }
            }
            catch (Exception e) {
                MessageBoxes.errorBox(MainFrame.this, "Unable to launch default browser.", "Unable to launch default browser. Please visit " + uri); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
    };
    
    private Action submitDiagnosticsAction = new AbstractAction(Translations.getString("Menu.Help.SubmitDiagnostics")) { //$NON-NLS-1$
        @Override
        public void actionPerformed(ActionEvent arg0) {
            SubmitDiagnosticsDialog dialog = new SubmitDiagnosticsDialog();
            dialog.setModal(true);
            dialog.setSize(620, 720);
            dialog.setLocationRelativeTo(MainFrame.get());
            dialog.setVisible(true);
        }
    };
    
    public final Action undoAction = new AbstractAction(Translations.getString("Menu.Edit.Undo")) {
        {
            putValue(MNEMONIC_KEY, KeyEvent.VK_Z);
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke('Z',
                    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            try {
                undoManager.undo();
            }
            catch (Exception e) {
                
            }
        }
    };
    
    public final Action redoAction = new AbstractAction(Translations.getString("Menu.Edit.Redo")) {
        {
//            putValue(MNEMONIC_KEY, KeyEvent.VK_Y);
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke('Z',
                    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.SHIFT_MASK));
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            try {
                undoManager.redo();
            }
            catch (Exception e) {
                
            }
        }
    };
    
    public class LanguageSelectionAction extends AbstractAction {
        private final Locale locale;
        
        public LanguageSelectionAction(Locale locale) {
            this.locale = locale;
            this.putValue(NAME, locale.getDisplayName());
        }
        
        public Locale getLocale() {
            return locale;
        }
        
        public void actionPerformed(ActionEvent arg0) {
          configuration.setLocale(locale);
          MessageBoxes.infoBox("Notice", //$NON-NLS-1$
                  "Please restart OpenPnP for the changes to take effect."); //$NON-NLS-1$
      }
    }
    
    private JPanel panelStatusAndDros;
    private JLabel droLbl;
    private JLabel lblStatus;
    private JLabel lblPlacements;
    private JProgressBar prgbrPlacements;
    private JLabel labelIcon;
}
