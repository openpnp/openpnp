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
import java.awt.Color;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openpnp.ConfigurationListener;
import org.openpnp.JobProcessorDelegate;
import org.openpnp.JobProcessorListener;
import org.openpnp.gui.components.AutoSelectTextTable;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.importer.BoardImporter;
import org.openpnp.gui.processes.TwoPlacementBoardLocationProcess;
import org.openpnp.gui.support.ActionGroup;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.tablemodel.BoardLocationsTableModel;
import org.openpnp.model.Board;
import org.openpnp.model.Board.Side;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.BoardPad;
import org.openpnp.model.Configuration;
import org.openpnp.model.Job;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.JobProcessor;
import org.openpnp.spi.JobProcessor.JobError;
import org.openpnp.spi.JobProcessor.JobState;
import org.openpnp.spi.JobProcessor.PickRetryAction;
import org.openpnp.spi.Locatable;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MachineListener;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.UiUtils;
import org.openpnp.vision.FiducialLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class JobPanel extends JPanel {
    @SuppressWarnings("unused")
    private static final Logger logger = LoggerFactory
            .getLogger(JobPanel.class);

    final private Configuration configuration;
    final private MainFrame frame;
    final private MachineControlsPanel machineControlsPanel;

    private static final String PREF_DIVIDER_POSITION = "JobPanel.dividerPosition";
    private static final int PREF_DIVIDER_POSITION_DEF = -1;

    private static final String UNTITLED_JOB_FILENAME = "Untitled.job.xml";

    private static final String PREF_RECENT_FILES = "JobPanel.recentFiles";
    private static final int PREF_RECENT_FILES_MAX = 10;

    private JobProcessor jobProcessor;

    private BoardLocationsTableModel boardLocationsTableModel;
    private JTable boardLocationsTable;
    private JSplitPane splitPane;

    private ActionGroup jobSaveActionGroup;
    private ActionGroup boardLocationSelectionActionGroup;

    private Preferences prefs = Preferences.userNodeForPackage(JobPanel.class);

    public JMenu mnOpenRecent;

    private List<File> recentJobs = new ArrayList<>();

    private final JobPlacementsPanel jobPlacementsPanel;
    private final JobPastePanel jobPastePanel;
    
    private JTabbedPane tabbedPane;

    public JobPanel(Configuration configuration, MainFrame frame,
            MachineControlsPanel machineControlsPanel) {
        this.configuration = configuration;
        this.frame = frame;
        this.machineControlsPanel = machineControlsPanel;

        jobSaveActionGroup = new ActionGroup(saveJobAction);
        jobSaveActionGroup.setEnabled(false);

        boardLocationSelectionActionGroup = new ActionGroup(removeBoardAction,
                captureCameraBoardLocationAction,
                captureToolBoardLocationAction,
                moveCameraToBoardLocationAction, moveToolToBoardLocationAction,
                twoPointLocateBoardLocationAction, fiducialCheckAction);
        boardLocationSelectionActionGroup.setEnabled(false);

        boardLocationsTableModel = new BoardLocationsTableModel(configuration);

        // Suppress because adding the type specifiers breaks WindowBuilder.
        @SuppressWarnings("rawtypes")
        JComboBox sidesComboBox = new JComboBox(Side.values());

        boardLocationsTable = new AutoSelectTextTable(boardLocationsTableModel);
        boardLocationsTable.setAutoCreateRowSorter(true);
        boardLocationsTable
                .setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        boardLocationsTable.setDefaultEditor(Side.class, new DefaultCellEditor(
                sidesComboBox));

        boardLocationsTable.getSelectionModel().addListSelectionListener(
                new ListSelectionListener() {
                    @Override
                    public void valueChanged(ListSelectionEvent e) {
                        if (e.getValueIsAdjusting()) {
                            return;
                        }
                        BoardLocation boardLocation = getSelectedBoardLocation();
                        boardLocationSelectionActionGroup
                                .setEnabled(boardLocation != null);
                        jobPlacementsPanel.setBoardLocation(boardLocation);
                        jobPastePanel.setBoardLocation(boardLocation);
                    }
                });

        setLayout(new BorderLayout(0, 0));

        splitPane = new JSplitPane();
        splitPane.setBorder(null);
        splitPane.setContinuousLayout(true);
        splitPane.setDividerLocation(prefs.getInt(PREF_DIVIDER_POSITION,
                PREF_DIVIDER_POSITION_DEF));
        splitPane.addPropertyChangeListener("dividerLocation",
                new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        prefs.putInt(PREF_DIVIDER_POSITION,
                                splitPane.getDividerLocation());
                    }
                });

        JPanel pnlBoards = new JPanel();
        pnlBoards.setBorder(new TitledBorder(new EtchedBorder(
                EtchedBorder.LOWERED, null, null), "Boards",
                TitledBorder.LEADING, TitledBorder.TOP, null,
                new Color(0, 0, 0)));
        pnlBoards.setLayout(new BorderLayout(0, 0));

        JToolBar toolBarBoards = new JToolBar();
        toolBarBoards.setFloatable(false);
        pnlBoards.add(toolBarBoards, BorderLayout.NORTH);

        JButton btnStartPauseResumeJob = new JButton(startPauseResumeJobAction);
        btnStartPauseResumeJob.setHideActionText(true);
        toolBarBoards.add(btnStartPauseResumeJob);
        JButton btnStepJob = new JButton(stepJobAction);
        btnStepJob.setHideActionText(true);
        toolBarBoards.add(btnStepJob);
        JButton btnStopJob = new JButton(stopJobAction);
        btnStopJob.setHideActionText(true);
        toolBarBoards.add(btnStopJob);
        toolBarBoards.addSeparator();
        JButton btnNewBoard = new JButton(newBoardAction);
        btnNewBoard.setHideActionText(true);
        toolBarBoards.add(btnNewBoard);
        JButton btnAddBoard = new JButton(addBoardAction);
        btnAddBoard.setHideActionText(true);
        toolBarBoards.add(btnAddBoard);
        JButton btnRemoveBoard = new JButton(removeBoardAction);
        btnRemoveBoard.setHideActionText(true);
        toolBarBoards.add(btnRemoveBoard);
        toolBarBoards.addSeparator();
        JButton btnCaptureCameraBoardLocation = new JButton(
                captureCameraBoardLocationAction);
        btnCaptureCameraBoardLocation.setHideActionText(true);
        toolBarBoards.add(btnCaptureCameraBoardLocation);

        JButton btnCaptureToolBoardLocation = new JButton(
                captureToolBoardLocationAction);
        btnCaptureToolBoardLocation.setHideActionText(true);
        toolBarBoards.add(btnCaptureToolBoardLocation);

        JButton btnPositionCameraBoardLocation = new JButton(
                moveCameraToBoardLocationAction);
        btnPositionCameraBoardLocation.setHideActionText(true);
        toolBarBoards.add(btnPositionCameraBoardLocation);

        JButton btnPositionToolBoardLocation = new JButton(
                moveToolToBoardLocationAction);
        btnPositionToolBoardLocation.setHideActionText(true);
        toolBarBoards.add(btnPositionToolBoardLocation);
        toolBarBoards.addSeparator();

        JButton btnTwoPointBoardLocation = new JButton(
                twoPointLocateBoardLocationAction);
        toolBarBoards.add(btnTwoPointBoardLocation);
        btnTwoPointBoardLocation.setHideActionText(true);

        JButton btnFiducialCheck = new JButton(fiducialCheckAction);
        toolBarBoards.add(btnFiducialCheck);
        btnFiducialCheck.setHideActionText(true);

        pnlBoards.add(new JScrollPane(boardLocationsTable));
        JPanel pnlRight = new JPanel();
        pnlRight.setLayout(new BorderLayout(0, 0));

        splitPane.setLeftComponent(pnlBoards);
        splitPane.setRightComponent(pnlRight);

        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        pnlRight.add(tabbedPane, BorderLayout.CENTER);
        
        tabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                Machine machine = Configuration.get().getMachine();
                JobProcessor.Type type = getSelectedJobProcessorType();
                setJobProcessor(machine.getJobProcessors().get(type));
            }
        });        

        jobPastePanel = new JobPastePanel(this);
        jobPlacementsPanel = new JobPlacementsPanel(this);

        add(splitPane);

        mnOpenRecent = new JMenu("Open Recent Job...");
        loadRecentJobs();
        
        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            public void configurationComplete(Configuration configuration)
                    throws Exception {
                Machine machine = configuration.getMachine();
                
                machine.addListener(machineListener);

                for (JobProcessor jobProcessor : machine.getJobProcessors().values()) {
                    jobProcessor.addListener(jobProcessorListener);
                    jobProcessor.setDelegate(jobProcessorDelegate);
                }

                if (machine.getJobProcessors().get(JobProcessor.Type.PickAndPlace) != null) {
                    tabbedPane.addTab("Pick and Place", null, jobPlacementsPanel, null);
                    // Creating the tab should fire off the selection event, setting
                    // the JobProcessor but this fails on some Linux based systems,
                    // so here we detect if it failed and force setting it.
                    if (jobProcessor == null) {
                        setJobProcessor(machine.getJobProcessors().get(JobProcessor.Type.PickAndPlace));
                    }
                }
                if (machine.getJobProcessors().get(JobProcessor.Type.SolderPaste) != null) {
                    tabbedPane.addTab("Solder Paste", null, jobPastePanel, null);
                    // Creating the tab should fire off the selection event, setting
                    // the JobProcessor but this fails on some Linux based systems,
                    // so here we detect if it failed and force setting it.
                    if (jobProcessor == null) {
                        setJobProcessor(machine.getJobProcessors().get(JobProcessor.Type.SolderPaste));
                    }
                }
                
                // Create an empty Job if one is not loaded
                if (jobProcessor.getJob() == null) {
                    Job job = new Job();
                    jobProcessor.load(job);
                }
            }
        });
    }
    
    public JobProcessor.Type getSelectedJobProcessorType() {
        String activeTabTitle = tabbedPane.getTitleAt(tabbedPane.getSelectedIndex());
        if (activeTabTitle.equals("Solder Paste")) {
            return JobProcessor.Type.SolderPaste;
        }
        else if (activeTabTitle.equals("Pick and Place")) {
            return JobProcessor.Type.PickAndPlace;
        }
        else {
            throw new Error("Unknown job tab title: " + activeTabTitle);
        }
    }
    
    /**
     * Unregister the listener and delegate for the JobProcessor, set the new
     * JobProcessor and add the listener and delegate back. If a job was
     * previously loaded into the JobProcessor, load it into the new one.
     * 
     * The sequencing of making this work is a bit complex. When the app is
     * starting the following happens:
     * 1. The UI is created and shown. At this time no JobProcessor is set.
     * 2. The Configuration is loaded and the completion listener is called.
     * 3. The Configuration listener checks which JobProcessors are registered
     * and adds tabs for each.
     * 4. The first tab that is added causes a selection event to happen, which
     * fires a ChangeEvent on the ChangeListener above.
     * 5. The ChangeListener checks which tab was selected and calls this
     * method with the appropriate JobProcessor.
     * @param jobProcessor
     */
    private void setJobProcessor(JobProcessor jobProcessor) {
        Job job = null;
        if (this.jobProcessor != null) {
            job = this.jobProcessor.getJob();
            if (this.jobProcessor.getState() != null && this.jobProcessor.getState() != JobProcessor.JobState.Stopped) {
                throw new AssertionError("this.jobProcessor.getState() " + this.jobProcessor.getState() + " != JobProcessor.JobState.Stopped");
            }
            this.jobProcessor.removeListener(jobProcessorListener);
            this.jobProcessor.setDelegate(null);
        }
        this.jobProcessor = jobProcessor;
        jobProcessor.addListener(jobProcessorListener);
        jobProcessor.setDelegate(jobProcessorDelegate);
        if (job != null) {
            jobProcessor.load(job);
        }
    }
    
    public JobPlacementsPanel getJobPlacementsPanel() {
        return jobPlacementsPanel;
    }

    private void updateRecentJobsMenu() {
        mnOpenRecent.removeAll();
        for (File file : recentJobs) {
            mnOpenRecent.add(new OpenRecentJobAction(file));
        }
    }

    private void loadRecentJobs() {
        recentJobs.clear();
        for (int i = 0; i < PREF_RECENT_FILES_MAX; i++) {
            String path = prefs.get(PREF_RECENT_FILES + "_" + i, null);
            if (path != null) {
                File file = new File(path);
                recentJobs.add(file);
            }
        }
        updateRecentJobsMenu();
    }

    private void saveRecentJobs() {
        // blow away all the existing values
        for (int i = 0; i < PREF_RECENT_FILES_MAX; i++) {
            prefs.remove(PREF_RECENT_FILES + "_" + i);
        }
        // update with what we have now
        for (int i = 0; i < recentJobs.size(); i++) {
            prefs.put(PREF_RECENT_FILES + "_" + i, recentJobs.get(i)
                    .getAbsolutePath());
        }
        updateRecentJobsMenu();
    }

    private void addRecentJob(File file) {
        while (recentJobs.contains(file)) {
            recentJobs.remove(file);
        }
        // add to top
        recentJobs.add(0, file);
        // limit length
        while (recentJobs.size() > PREF_RECENT_FILES_MAX) {
            recentJobs.remove(recentJobs.size() - 1);
        }
        saveRecentJobs();
    }

    public void refreshSelectedBoardRow() {
        boardLocationsTableModel.fireTableRowsUpdated(
                boardLocationsTable.getSelectedRow(),
                boardLocationsTable.getSelectedRow());
    }

    public BoardLocation getSelectedBoardLocation() {
        int index = boardLocationsTable.getSelectedRow();
        if (index == -1) {
            return null;
        }
        else {
            index = boardLocationsTable.convertRowIndexToModel(index);
            return JobPanel.this.jobProcessor.getJob().getBoardLocations()
                    .get(index);
        }
    }

    /**
     * Checks if there are any modifications that need to be saved. Prompts the
     * user if there are. Returns true if it's okay to exit.
     * 
     * @return
     */
    public boolean checkForModifications() {
        if (!checkForBoardModifications()) {
            return false;
        }
        if (!checkForJobModifications()) {
            return false;
        }
        return true;
    }

    private boolean checkForJobModifications() {
        if (jobProcessor.getJob().isDirty()) {
            Job job = jobProcessor.getJob();
            String name = (job.getFile() == null ? UNTITLED_JOB_FILENAME : job
                    .getFile().getName());
            int result = JOptionPane.showConfirmDialog(frame,
                    "Do you want to save your changes to " + name + "?" + "\n"
                            + "If you don't save, your changes will be lost.",
                    "Save " + name + "?", JOptionPane.YES_NO_CANCEL_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                return saveJob();
            }
            else if (result == JOptionPane.CANCEL_OPTION) {
                return false;
            }
        }
        return true;
    }

    private boolean checkForBoardModifications() {
        for (Board board : configuration.getBoards()) {
            if (board.isDirty()) {
                int result = JOptionPane
                        .showConfirmDialog(
                                getTopLevelAncestor(),
                                "Do you want to save your changes to "
                                        + board.getFile().getName()
                                        + "?"
                                        + "\n"
                                        + "If you don't save, your changes will be lost.",
                                "Save " + board.getFile().getName() + "?",
                                JOptionPane.YES_NO_CANCEL_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    try {
                        configuration.saveBoard(board);
                    }
                    catch (Exception e) {
                        MessageBoxes.errorBox(getTopLevelAncestor(),
                                "Board Save Error", e.getMessage());
                        return false;
                    }
                }
                else if (result == JOptionPane.CANCEL_OPTION) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean saveJob() {
        if (jobProcessor.getJob().getFile() == null) {
            return saveJobAs();
        }
        else {
            try {
                File file = jobProcessor.getJob().getFile();
                configuration.saveJob(jobProcessor.getJob(), file);
                addRecentJob(file);
                return true;
            }
            catch (Exception e) {
                MessageBoxes.errorBox(frame, "Job Save Error", e.getMessage());
                return false;
            }
        }
    }

    private boolean saveJobAs() {
        FileDialog fileDialog = new FileDialog(frame, "Save Job As...",
                FileDialog.SAVE);
        fileDialog.setFilenameFilter(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".job.xml");
            }
        });
        fileDialog.setVisible(true);
        try {
            String filename = fileDialog.getFile();
            if (filename == null) {
                return false;
            }
            if (!filename.toLowerCase().endsWith(".job.xml")) {
                filename = filename + ".job.xml";
            }
            File file = new File(new File(fileDialog.getDirectory()), filename);
            if (file.exists()) {
                int ret = JOptionPane
                        .showConfirmDialog(
                                getTopLevelAncestor(),
                                file.getName()
                                        + " already exists. Do you want to replace it?",
                                "Replace file?", JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE);
                if (ret != JOptionPane.YES_OPTION) {
                    return false;
                }
            }
            configuration.saveJob(jobProcessor.getJob(), file);
            addRecentJob(file);
            return true;
        }
        catch (Exception e) {
            MessageBoxes.errorBox(frame, "Job Save Error", e.getMessage());
            return false;
        }
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
            startPauseResumeJobAction.putValue(AbstractAction.SMALL_ICON,
                    Icons.start);
            startPauseResumeJobAction.putValue(
                    AbstractAction.SHORT_DESCRIPTION,
                    "Start processing the job.");
            stopJobAction.setEnabled(false);
            stepJobAction.setEnabled(true);
            tabbedPane.setEnabled(true);
        }
        else if (state == JobState.Running) {
            startPauseResumeJobAction.setEnabled(true);
            startPauseResumeJobAction.putValue(AbstractAction.NAME, "Pause");
            startPauseResumeJobAction.putValue(AbstractAction.SMALL_ICON,
                    Icons.pause);
            startPauseResumeJobAction.putValue(
                    AbstractAction.SHORT_DESCRIPTION,
                    "Pause processing of the job.");
            stopJobAction.setEnabled(true);
            stepJobAction.setEnabled(false);
            tabbedPane.setEnabled(false);
        }
        else if (state == JobState.Paused) {
            startPauseResumeJobAction.setEnabled(true);
            startPauseResumeJobAction.putValue(AbstractAction.NAME, "Resume");
            startPauseResumeJobAction.putValue(AbstractAction.SMALL_ICON,
                    Icons.start);
            startPauseResumeJobAction.putValue(
                    AbstractAction.SHORT_DESCRIPTION,
                    "Resume processing of the job.");
            stopJobAction.setEnabled(true);
            stepJobAction.setEnabled(true);
            tabbedPane.setEnabled(false);
        }

        // We allow the above to run first so that all state is represented
        // correctly even if the machine is disabled.
        if (!configuration.getMachine().isEnabled()) {
            startPauseResumeJobAction.setEnabled(false);
            stopJobAction.setEnabled(false);
            stepJobAction.setEnabled(false);
        }
    }

    private void updateTitle() {
        Job job = jobProcessor.getJob();
        String title = String.format("OpenPnP - %s%s",
                job.isDirty() ? "*" : "",
                (job.getFile() == null ? UNTITLED_JOB_FILENAME : job.getFile()
                        .getName()));
        frame.setTitle(title);
    }

    public void importBoard(Class<? extends BoardImporter> boardImporterClass) {
        if (getSelectedBoardLocation() == null) {
            MessageBoxes.errorBox(getTopLevelAncestor(), "Import Failed",
                    "Please select a board in the Jobs tab to import into.");
            return;
        }

        BoardImporter boardImporter;
        try {
            boardImporter = boardImporterClass.newInstance();
        }
        catch (Exception e) {
            MessageBoxes.errorBox(getTopLevelAncestor(), "Import Failed", e);
            return;
        }

        try {
            Board importedBoard = boardImporter
                    .importBoard((Frame) getTopLevelAncestor());
            if (importedBoard != null) {
                Board existingBoard = getSelectedBoardLocation().getBoard();
                for (Placement placement : importedBoard.getPlacements()) {
                    existingBoard.addPlacement(placement);
                }
                for (BoardPad pad : importedBoard.getSolderPastePads()) {
                    // TODO: This is a temporary hack until we redesign the importer
                    // interface to be more intuitive. The Gerber importer tends
                    // to return everything in Inches, so this is a method to
                    // try to get it closer to what the user expects to see.
                    pad.setLocation(pad.getLocation().convertToUnits(getSelectedBoardLocation().getLocation().getUnits()));
                    existingBoard.addSolderPastePad(pad);
                }
                jobPlacementsPanel.setBoardLocation(getSelectedBoardLocation());
                jobPastePanel.setBoardLocation(getSelectedBoardLocation());
            }
        }
        catch (Exception e) {
            MessageBoxes.errorBox(getTopLevelAncestor(), "Import Failed", e);
        }
    }

    public final Action openJobAction = new AbstractAction("Open Job...") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (!checkForModifications()) {
                return;
            }
            FileDialog fileDialog = new FileDialog(frame);
            fileDialog.setFilenameFilter(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".job.xml");
                }
            });
            fileDialog.setVisible(true);
            try {
                if (fileDialog.getFile() == null) {
                    return;
                }
                File file = new File(new File(fileDialog.getDirectory()),
                        fileDialog.getFile());
                Job job = configuration.loadJob(file);
                jobProcessor.load(job);
                addRecentJob(file);
            }
            catch (Exception e) {
                e.printStackTrace();
                MessageBoxes.errorBox(frame, "Job Load Error", e.getMessage());
            }
        }
    };

    public final Action newJobAction = new AbstractAction("New Job") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (!checkForModifications()) {
                return;
            }
            jobProcessor.load(new Job());
        }
    };

    public final Action saveJobAction = new AbstractAction("Save Job") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            saveJob();
        }
    };

    public final Action saveJobAsAction = new AbstractAction("Save Job As...") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            saveJobAs();
        }
    };

    public final Action startPauseResumeJobAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.start);
            putValue(NAME, "Start");
            putValue(SHORT_DESCRIPTION, "Start processing the job.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            JobState state = jobProcessor.getState();
            if (state == JobState.Stopped) {
                try {
                    jobProcessor.start();
                }
                catch (Exception e) {
                    MessageBoxes.errorBox(frame, "Job Start Error",
                            e.getMessage());
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

    public final Action stepJobAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.step);
            putValue(NAME, "Step");
            putValue(SHORT_DESCRIPTION,
                    "Process one step of the job and pause.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            try {
                jobProcessor.step();
            }
            catch (Exception e) {
                MessageBoxes.errorBox(frame, "Job Step Failed", e.getMessage());
            }
        }
    };

    public final Action stopJobAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.stop);
            putValue(NAME, "Stop");
            putValue(SHORT_DESCRIPTION, "Stop processing the job.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            jobProcessor.stop();
        }
    };

    public final Action newBoardAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.neww);
            putValue(NAME, "New Board...");
            putValue(SHORT_DESCRIPTION,
                    "Create a new board and add it to the job.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            FileDialog fileDialog = new FileDialog(frame,
                    "Save New Board As...", FileDialog.SAVE);
            fileDialog.setFilenameFilter(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".board.xml");
                }
            });
            fileDialog.setVisible(true);
            try {
                String filename = fileDialog.getFile();
                if (filename == null) {
                    return;
                }
                if (!filename.toLowerCase().endsWith(".board.xml")) {
                    filename = filename + ".board.xml";
                }
                File file = new File(new File(fileDialog.getDirectory()),
                        filename);

                Board board = configuration.getBoard(file);
                BoardLocation boardLocation = new BoardLocation(board);
                jobProcessor.getJob().addBoardLocation(boardLocation);
                boardLocationsTableModel.fireTableDataChanged();

                Helpers.selectLastTableRow(boardLocationsTable);
            }
            catch (Exception e) {
                e.printStackTrace();
                MessageBoxes.errorBox(frame, "Unable to create new board",
                        e.getMessage());
            }
        }
    };

    public final Action addBoardAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.add);
            putValue(NAME, "Add Board...");
            putValue(SHORT_DESCRIPTION, "Add an existing board to the job.");
        }

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
                if (fileDialog.getFile() == null) {
                    return;
                }
                File file = new File(new File(fileDialog.getDirectory()),
                        fileDialog.getFile());

                Board board = configuration.getBoard(file);
                BoardLocation boardLocation = new BoardLocation(board);
                jobProcessor.getJob().addBoardLocation(boardLocation);
                // TODO: Move to a list property listener.
                boardLocationsTableModel.fireTableDataChanged();

                Helpers.selectLastTableRow(boardLocationsTable);
            }
            catch (Exception e) {
                e.printStackTrace();
                MessageBoxes.errorBox(frame, "Board load failed",
                        e.getMessage());
            }
        }
    };

    public final Action removeBoardAction = new AbstractAction("Remove Board") {
        {
            putValue(SMALL_ICON, Icons.delete);
            putValue(NAME, "Remove Board");
            putValue(SHORT_DESCRIPTION,
                    "Remove the selected board from the job.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            int index = boardLocationsTable.getSelectedRow();
            if (index != -1) {
                index = boardLocationsTable.convertRowIndexToModel(index);
                BoardLocation boardLocation = JobPanel.this.jobProcessor
                        .getJob().getBoardLocations().get(index);
                JobPanel.this.jobProcessor.getJob().removeBoardLocation(
                        boardLocation);
                boardLocationsTableModel.fireTableDataChanged();
            }
        }
    };

    public final Action captureCameraBoardLocationAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.captureCamera);
            putValue(NAME, "Capture Camera Location");
            putValue(SHORT_DESCRIPTION,
                    "Set the board's location to the camera's current position.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
        	UiUtils.messageBoxOnException(() -> {
            	HeadMountable tool = MainFrame.machineControlsPanel.getSelectedTool();
            	Camera camera = tool.getHead().getDefaultCamera();
                double z = getSelectedBoardLocation().getLocation().getZ();
                getSelectedBoardLocation().setLocation(camera.getLocation().derive(null, null, z, null));
                boardLocationsTableModel.fireTableRowsUpdated(
                        boardLocationsTable.getSelectedRow(),
                        boardLocationsTable.getSelectedRow());
        	});
        }
    };

    public final Action captureToolBoardLocationAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.captureTool);
            putValue(NAME, "Capture Tool Location");
            putValue(SHORT_DESCRIPTION,
                    "Set the board's location to the tool's current position.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
        	HeadMountable tool = MainFrame.machineControlsPanel.getSelectedTool();
                double z = getSelectedBoardLocation().getLocation().getZ();
                getSelectedBoardLocation().setLocation(tool.getLocation().derive(null, null, z, null));
            boardLocationsTableModel.fireTableRowsUpdated(
                    boardLocationsTable.getSelectedRow(),
                    boardLocationsTable.getSelectedRow());
        }
    };

    public final Action moveCameraToBoardLocationAction = new AbstractAction(
            "Move Camera To Board Location") {
        {
            putValue(SMALL_ICON, Icons.centerCamera);
            putValue(NAME, "Move Camera To Board Location");
            putValue(SHORT_DESCRIPTION,
                    "Position the camera at the board's location.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
        	UiUtils.submitUiMachineTask(() -> {
        		HeadMountable tool = MainFrame.machineControlsPanel.getSelectedTool();
            	Camera camera = tool.getHead().getDefaultCamera();
            	MainFrame.cameraPanel.ensureCameraVisible(camera);
                Location location = getSelectedBoardLocation().getLocation();
                MovableUtils.moveToLocationAtSafeZ(camera, location, 1.0);
        	});
        }
    };

    public final Action moveToolToBoardLocationAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.centerTool);
            putValue(NAME, "Move Tool To Board Location");
            putValue(SHORT_DESCRIPTION,
                    "Position the tool at the board's location.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
        	UiUtils.submitUiMachineTask(() -> {
                HeadMountable tool = MainFrame.machineControlsPanel.getSelectedTool();
                Location location = getSelectedBoardLocation().getLocation();
                MovableUtils.moveToLocationAtSafeZ(tool, location, 1.0);
        	});
        }
    };

    public final Action twoPointLocateBoardLocationAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.twoPointLocate);
            putValue(NAME, "Two Point Board Location");
            putValue(SHORT_DESCRIPTION,
                    "Set the board's location and rotation using two placements.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
        	UiUtils.messageBoxOnException(() -> {
                new TwoPlacementBoardLocationProcess(frame, JobPanel.this);
        	});
        }
    };

    public final Action fiducialCheckAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.fiducialCheck);
            putValue(NAME, "Fiducial Check");
            putValue(SHORT_DESCRIPTION,
                    "Perform a fiducial check for the board and update it's location and rotation.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
        	UiUtils.submitUiMachineTask(() -> {
                Location location = FiducialLocator.locateBoard(getSelectedBoardLocation());
                getSelectedBoardLocation().setLocation(location);
                refreshSelectedBoardRow();
                HeadMountable tool = MainFrame.machineControlsPanel.getSelectedTool();
                Camera camera = tool.getHead().getDefaultCamera();
                MainFrame.cameraPanel.ensureCameraVisible(camera);
                MovableUtils.moveToLocationAtSafeZ(camera, location, 1.0);
        	});
        }
    };

    public class OpenRecentJobAction extends AbstractAction {
        private final File file;

        public OpenRecentJobAction(File file) {
            this.file = file;
            putValue(NAME, file.getName());
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (!checkForModifications()) {
                return;
            }
            try {
                Job job = configuration.loadJob(file);
                jobProcessor.load(job);
                addRecentJob(file);
            }
            catch (Exception e) {
                e.printStackTrace();
                MessageBoxes.errorBox(frame, "Job Load Error", e.getMessage());
            }
        }
    }

    private final JobProcessorListener jobProcessorListener = new JobProcessorListener.Adapter() {
        @Override
        public void jobStateChanged(JobState state) {
            updateJobActions();
        }

        @Override
        public void jobLoaded(Job job) {
            if (boardLocationsTableModel.getJob() != job) {
                // If the same job is being loaded there is no reason to reset
                // the table, so skip it. This allows us to leave the same
                // row selected in the case of switching job processors and
                // tabs.
                boardLocationsTableModel.setJob(job);
            }
            job.addPropertyChangeListener("dirty", titlePropertyChangeListener);
            job.addPropertyChangeListener("file", titlePropertyChangeListener);
            updateTitle();
            updateJobActions();
        }

        @Override
        public void jobEncounteredError(JobError error, String description) {
            MessageBoxes.errorBox(frame, error.toString(), description
                    + "\n\nThe job will be paused.");
            // TODO: Implement a way to retry, abort, etc.
            jobProcessor.pause();
        }
    };

    private final JobProcessorDelegate jobProcessorDelegate = new JobProcessorDelegate() {
        @Override
        public PickRetryAction partPickFailed(BoardLocation board, Part part,
                Feeder feeder) {
            return PickRetryAction.SkipAndContinue;
        }
    };

    private final MachineListener machineListener = new MachineListener.Adapter() {
        @Override
        public void machineEnabled(Machine machine) {
            updateJobActions();
        }

        @Override
        public void machineDisabled(Machine machine, String reason) {
            updateJobActions();
            jobProcessor.stop();
        }
    };

    private final PropertyChangeListener titlePropertyChangeListener = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            updateTitle();
            jobSaveActionGroup.setEnabled(jobProcessor.getJob().isDirty());
        }
    };
}
