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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.openpnp.ConfigurationListener;
import org.openpnp.Translations;
import org.openpnp.events.BoardLocationSelectedEvent;
import org.openpnp.events.JobLoadedEvent;
import org.openpnp.events.PlacementSelectedEvent;
import org.openpnp.gui.components.AutoSelectTextTable;
import org.openpnp.gui.importer.BoardImporter;
import org.openpnp.gui.panelization.DlgAutoPanelize;
import org.openpnp.gui.panelization.DlgPanelXOut;
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
import org.openpnp.model.Placement;
import org.openpnp.model.Placement.Type;
import org.openpnp.spi.Camera;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.JobProcessor;
import org.openpnp.spi.JobProcessor.TextStatusListener;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MachineListener;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.UiUtils;

import com.google.common.eventbus.Subscribe;

@SuppressWarnings("serial")
public class JobPanel extends JPanel {
    enum State {
        Stopped,
        Paused,
        Running,
        Pausing,
        Stopping
    }
    
    final private Configuration configuration;
    final private MainFrame frame;

    private static final String PREF_DIVIDER_POSITION = "JobPanel.dividerPosition"; //$NON-NLS-1$
    private static final int PREF_DIVIDER_POSITION_DEF = -1;

    private static final String UNTITLED_JOB_FILENAME = "Untitled.job.xml"; //$NON-NLS-1$

    private static final String PREF_RECENT_FILES = "JobPanel.recentFiles"; //$NON-NLS-1$
    private static final int PREF_RECENT_FILES_MAX = 10;

    private BoardLocationsTableModel tableModel;
    private JTable table;
    private JSplitPane splitPane;

    private ActionGroup singleSelectionActionGroup;
    private ActionGroup multiSelectionActionGroup;

    private Preferences prefs = Preferences.userNodeForPackage(JobPanel.class);

    public JMenu mnOpenRecent;

    private List<File> recentJobs = new ArrayList<>();

    private final JobPlacementsPanel jobPlacementsPanel;
    private final JobPastePanel jobPastePanel;

    private JTabbedPane tabbedPane;

    private Job job;

    private JobProcessor jobProcessor;
    
    private State state = State.Stopped;

    public JobPanel(Configuration configuration, MainFrame frame) {
        this.configuration = configuration;
        this.frame = frame;

        singleSelectionActionGroup =
                new ActionGroup(removeBoardAction, captureCameraBoardLocationAction,
                        captureToolBoardLocationAction, moveCameraToBoardLocationAction,
                        moveCameraToBoardLocationNextAction, moveToolToBoardLocationAction,
                        twoPointLocateBoardLocationAction, fiducialCheckAction, panelizeAction,
                        setEnabledAction,setCheckFidsAction, setSideAction);
        singleSelectionActionGroup.setEnabled(false);
        
        multiSelectionActionGroup = new ActionGroup(removeBoardAction, setEnabledAction, setCheckFidsAction, setSideAction);
        multiSelectionActionGroup.setEnabled(false);
        
        panelizeXOutAction.setEnabled(false);
        panelizeFiducialCheck.setEnabled(false);
        tableModel = new BoardLocationsTableModel(configuration);

        // Suppress because adding the type specifiers breaks WindowBuilder.
        @SuppressWarnings({"unchecked", "rawtypes"})
        JComboBox sidesComboBox = new JComboBox(Side.values());

        table = new AutoSelectTextTable(tableModel) {
            @Override
            public String getToolTipText(MouseEvent e) {

                java.awt.Point p = e.getPoint();
                int row = rowAtPoint(p);
                int col = columnAtPoint(p);

                if (row >= 0) {
                    if (col == 0) {
                        row = table.convertRowIndexToModel(row);
                        BoardLocation boardLocation =
                                tableModel.getBoardLocation(row);
                        if (boardLocation != null) {
                            return boardLocation.getBoard()
                                                .getFile()
                                                .toString();
                        }
                    }
                }

                return super.getToolTipText();
            }
        };

        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setDefaultEditor(Side.class, new DefaultCellEditor(sidesComboBox));

        table.getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                SwingUtilities.invokeLater(() -> {
                    // One of 3 things can be happening here:
                    // First is row 0 is being edited. In normal mode, nothing
                    // special needs to be done. In Auto Panelize mode, the
                    // computed panel PCBs (that is, the ones PCB derived from
                    // the panel parameters) must be updated. The second is that
                    // row 1 or higher needs to be edited. This can only happen when
                    // NOT in autopanelize mode as the editing is blocked in the
                    // BoardLocationTableModel class. Finally, when the table wants
                    // to update itself (eg due to TableDataChange event being
                    // fired) it
                    // will set the first row to 0 and the last row to 2147483647
                    // (maxint). This is a behavior of the table...we simply detect
                    // it here to ascertain the mode

                    // Below, we check for each of these.
                    if (e.getFirstRow() == 0 && e.getLastRow() == 0) {
                        // Here, the first row is being edited. The function below
                        // will check if
                        // we're in autopanelize mode and update other rows
                        // accordingly
                        populatePanelSettingsIntoBoardLocations();
                    }
                    else if (e.getFirstRow() > 0 && e.getLastRow() <= Integer.MAX_VALUE) {
                        // Here, we're not in auto panelize mode (since row 1 or
                        // higher could be edited.
                        // Do nothing
                    }
                    else if (e.getFirstRow() == 0 && e.getLastRow() == Integer.MAX_VALUE) {
                        // A generic table update in response to TableDataChange
                        // event
                        updatePanelizationIconState();
                    }
                    jobPlacementsPanel.setBoardLocation(getSelection());
                });
            }
        });
        
        table.getSelectionModel()
                .addListSelectionListener(new ListSelectionListener() {
                    @Override
                    public void valueChanged(ListSelectionEvent e) {
                        if (e.getValueIsAdjusting()) {
                            return;
                        }
                        
                        List<BoardLocation> selections = getSelections();
                        if (selections.size() == 0) {
                            singleSelectionActionGroup.setEnabled(false);
                            multiSelectionActionGroup.setEnabled(false);
                            jobPlacementsPanel.setBoardLocation(null);
                            jobPastePanel.setBoardLocation(null);
                            Configuration.get().getBus()
                                .post(new BoardLocationSelectedEvent(null, JobPanel.this));
                        }
                        else if (selections.size() == 1) {
                            multiSelectionActionGroup.setEnabled(false);
                            singleSelectionActionGroup.setEnabled(true);
                            jobPlacementsPanel.setBoardLocation(selections.get(0));
                            jobPastePanel.setBoardLocation(selections.get(0));
                            Configuration.get().getBus()
                                .post(new BoardLocationSelectedEvent(selections.get(0), JobPanel.this));
                        }
                        else {
                            singleSelectionActionGroup.setEnabled(false);
                            multiSelectionActionGroup.setEnabled(true);
                            jobPlacementsPanel.setBoardLocation(null);
                            jobPastePanel.setBoardLocation(null);
                            Configuration.get().getBus()
                                .post(new BoardLocationSelectedEvent(null, JobPanel.this));
                        }

                        updatePanelizationIconState();
                    }
                });

        setLayout(new BorderLayout(0, 0));

        splitPane = new JSplitPane();
        splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        splitPane.setBorder(null);
        splitPane.setContinuousLayout(true);
        splitPane
                .setDividerLocation(prefs.getInt(PREF_DIVIDER_POSITION, PREF_DIVIDER_POSITION_DEF));
        splitPane.addPropertyChangeListener("dividerLocation", new PropertyChangeListener() { //$NON-NLS-1$
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                prefs.putInt(PREF_DIVIDER_POSITION, splitPane.getDividerLocation());
            }
        });

        JPanel pnlBoards = new JPanel();
        pnlBoards.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null),
                "Boards", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0))); //$NON-NLS-1$
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
        JButton btnAddBoard = new JButton(addBoardAction);
        btnAddBoard.setHideActionText(true);
        btnAddBoard.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                JPopupMenu menu = new JPopupMenu();
                menu.add(new JMenuItem(addNewBoardAction));
                menu.add(new JMenuItem(addExistingBoardAction));
                menu.show(btnAddBoard, (int) btnAddBoard.getWidth(), (int) btnAddBoard.getHeight());
            }
        });
        toolBarBoards.add(btnAddBoard);
        JButton btnRemoveBoard = new JButton(removeBoardAction);
        btnRemoveBoard.setHideActionText(true);
        toolBarBoards.add(btnRemoveBoard);
        toolBarBoards.addSeparator();
        JButton btnCaptureCameraBoardLocation = new JButton(captureCameraBoardLocationAction);
        btnCaptureCameraBoardLocation.setHideActionText(true);
        toolBarBoards.add(btnCaptureCameraBoardLocation);

        JButton btnCaptureToolBoardLocation = new JButton(captureToolBoardLocationAction);
        btnCaptureToolBoardLocation.setHideActionText(true);
        toolBarBoards.add(btnCaptureToolBoardLocation);

        JButton btnPositionCameraBoardLocation = new JButton(moveCameraToBoardLocationAction);
        btnPositionCameraBoardLocation.setHideActionText(true);
        toolBarBoards.add(btnPositionCameraBoardLocation);

        JButton btnPositionCameraBoardLocationNext =
                new JButton(moveCameraToBoardLocationNextAction);
        btnPositionCameraBoardLocationNext.setHideActionText(true);
        toolBarBoards.add(btnPositionCameraBoardLocationNext);
        JButton btnPositionToolBoardLocation = new JButton(moveToolToBoardLocationAction);
        btnPositionToolBoardLocation.setHideActionText(true);
        toolBarBoards.add(btnPositionToolBoardLocation);
        toolBarBoards.addSeparator();

        JButton btnTwoPointBoardLocation = new JButton(twoPointLocateBoardLocationAction);
        toolBarBoards.add(btnTwoPointBoardLocation);
        btnTwoPointBoardLocation.setHideActionText(true);

        JButton btnFiducialCheck = new JButton(fiducialCheckAction);
        toolBarBoards.add(btnFiducialCheck);
        btnFiducialCheck.setHideActionText(true);
        toolBarBoards.addSeparator();
        JButton btnPanelize = new JButton(panelizeAction);
        toolBarBoards.add(btnPanelize);
        btnPanelize.setHideActionText(true);
        JButton btnPanelizeXOut = new JButton(panelizeXOutAction);
        toolBarBoards.add(btnPanelizeXOut);
        btnPanelizeXOut.setHideActionText(true);
        JButton btnPanelizeFidCheck = new JButton(panelizeFiducialCheck);
        toolBarBoards.add(btnPanelizeFidCheck);
        btnPanelizeFidCheck.setHideActionText(true);

        pnlBoards.add(new JScrollPane(table));
        JPanel pnlRight = new JPanel();
        pnlRight.setLayout(new BorderLayout(0, 0));

        splitPane.setLeftComponent(pnlBoards);
        splitPane.setRightComponent(pnlRight);

        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        pnlRight.add(tabbedPane, BorderLayout.CENTER);

        jobPastePanel = new JobPastePanel(this);
        jobPlacementsPanel = new JobPlacementsPanel(this);

        add(splitPane);

        mnOpenRecent = new JMenu("Open Recent Job..."); //$NON-NLS-1$
        mnOpenRecent.setMnemonic(KeyEvent.VK_R);
        loadRecentJobs();

        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            public void configurationComplete(Configuration configuration) throws Exception {
                Machine machine = configuration.getMachine();

                machine.addListener(machineListener);

                if (machine.getPnpJobProcessor() != null) {
                    tabbedPane.addTab("Pick and Place", null, jobPlacementsPanel, null); //$NON-NLS-1$
                    machine.getPnpJobProcessor().addTextStatusListener(textStatusListener);
                }

                if (machine.getPasteDispenseJobProcessor() != null) {
                    tabbedPane.addTab("Solder Paste", null, jobPastePanel, null); //$NON-NLS-1$
                    machine.getPasteDispenseJobProcessor()
                            .addTextStatusListener(textStatusListener);
                }

                // Create an empty Job if one is not loaded
                if (getJob() == null) {
                    setJob(new Job());
                }
            }
        });

        JPopupMenu popupMenu = new JPopupMenu();

        JMenu setSideMenu = new JMenu(setSideAction);
        for (Board.Side side : Board.Side.values()) {
            setSideMenu.add(new SetSideAction(side));
        }
        popupMenu.add(setSideMenu);

        JMenu setEnabledMenu = new JMenu(setEnabledAction);
        setEnabledMenu.add(new SetEnabledAction(true));
        setEnabledMenu.add(new SetEnabledAction(false));
        popupMenu.add(setEnabledMenu);

        JMenu setCheckFidsMenu = new JMenu(setCheckFidsAction);
        setCheckFidsMenu.add(new SetCheckFidsAction(true));
        setCheckFidsMenu.add(new SetCheckFidsAction(false));
        popupMenu.add(setCheckFidsMenu);

        table.setComponentPopupMenu(popupMenu);

        Configuration.get().getBus().register(this);
    }
    
    void setState(State newState) {
        this.state = newState;
        updateJobActions();
    }
    
    public JTable getBoardLocationsTable() {
        return table;
    }

    @Subscribe
    public void boardLocationSelected(BoardLocationSelectedEvent event) {
        if (event.source == this) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            MainFrame.get().showTab("Job"); //$NON-NLS-1$

            selectBoardLocation(event.boardLocation);
        });
    }

    @Subscribe
    public void placementSelected(PlacementSelectedEvent event) {
        if (event.source == this || event.source == jobPlacementsPanel) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            MainFrame.get().showTab("Job"); //$NON-NLS-1$

            showTab("Pick and Place"); //$NON-NLS-1$

            selectBoardLocation(event.boardLocation);

            jobPlacementsPanel.selectPlacement(event.placement);
        });
    }

    private void selectBoardLocation(BoardLocation boardLocation) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (tableModel.getBoardLocation(i) == boardLocation) {
                int index = table.convertRowIndexToView(i);
                table.getSelectionModel().setSelectionInterval(index, index);
                table.scrollRectToVisible(
                        new Rectangle(table.getCellRect(index, 0, true)));
                break;
            }
        }
    }

    private void showTab(String title) {
        int index = tabbedPane.indexOfTab(title);
        tabbedPane.setSelectedIndex(index);
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        if (this.job != null) {
            this.job.removePropertyChangeListener("dirty", titlePropertyChangeListener); //$NON-NLS-1$
            this.job.removePropertyChangeListener("file", titlePropertyChangeListener); //$NON-NLS-1$
        }
        this.job = job;
        tableModel.setJob(job);
        job.addPropertyChangeListener("dirty", titlePropertyChangeListener); //$NON-NLS-1$
        job.addPropertyChangeListener("file", titlePropertyChangeListener); //$NON-NLS-1$
        updateTitle();
        updateJobActions();
        getJobPlacementsPanel().updateActivePlacements();
        Configuration.get().getBus().post(new JobLoadedEvent(job));
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
            String path = prefs.get(PREF_RECENT_FILES + "_" + i, null); //$NON-NLS-1$
            if (path != null && new File(path).exists()) {
                File file = new File(path);
                recentJobs.add(file);
            }
        }
        updateRecentJobsMenu();
    }

    private void saveRecentJobs() {
        // blow away all the existing values
        for (int i = 0; i < PREF_RECENT_FILES_MAX; i++) {
            prefs.remove(PREF_RECENT_FILES + "_" + i); //$NON-NLS-1$
        }
        // update with what we have now
        for (int i = 0; i < recentJobs.size(); i++) {
            prefs.put(PREF_RECENT_FILES + "_" + i, recentJobs.get(i).getAbsolutePath()); //$NON-NLS-1$
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

    public void refresh() {
        tableModel.fireTableDataChanged();
    }

    public void refreshSelectedBoardRow() {
        tableModel.fireTableRowsUpdated(table.getSelectedRow(),
                table.getSelectedRow());
    }

    public BoardLocation getSelection() {
        List<BoardLocation> selections = getSelections();
        if (selections.isEmpty()) {
            return null;
        }
        return selections.get(0);
    }

    public List<BoardLocation> getSelections() {
        ArrayList<BoardLocation> selections = new ArrayList<>();
        int[] selectedRows = table.getSelectedRows();
        for (int selectedRow : selectedRows) {
            selectedRow = table.convertRowIndexToModel(selectedRow);
            selections.add(job.getBoardLocations().get(selectedRow));
        }
        return selections;
    }

    /**
     * Checks if there are any modifications that need to be saved. Prompts the user if there are.
     * Returns true if it's okay to exit.
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
        if (getJob().isDirty()) {
            String name = (job.getFile() == null ? UNTITLED_JOB_FILENAME : job.getFile().getName());
            int result = JOptionPane.showConfirmDialog(frame,
                    "Do you want to save your changes to " + name + "?" + "\n" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                            + "If you don't save, your changes will be lost.", //$NON-NLS-1$
                    "Save " + name + "?", JOptionPane.YES_NO_CANCEL_OPTION); //$NON-NLS-1$ //$NON-NLS-2$
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
                int result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                        "Do you want to save your changes to " + board.getFile().getName() + "?" //$NON-NLS-1$ //$NON-NLS-2$
                                + "\n" + "If you don't save, your changes will be lost.", //$NON-NLS-1$ //$NON-NLS-2$
                        "Save " + board.getFile().getName() + "?", //$NON-NLS-1$ //$NON-NLS-2$
                        JOptionPane.YES_NO_CANCEL_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    try {
                        configuration.saveBoard(board);
                    }
                    catch (Exception e) {
                        MessageBoxes.errorBox(getTopLevelAncestor(), "Board Save Error", //$NON-NLS-1$
                                e.getMessage());
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
        if (getJob().getFile() == null) {
            return saveJobAs();
        }
        else {
            try {
                File file = getJob().getFile();
                configuration.saveJob(getJob(), file);
                addRecentJob(file);
                return true;
            }
            catch (Exception e) {
                MessageBoxes.errorBox(frame, "Job Save Error", e.getMessage()); //$NON-NLS-1$
                return false;
            }
        }
    }

    private boolean saveJobAs() {
        FileDialog fileDialog = new FileDialog(frame, "Save Job As...", FileDialog.SAVE); //$NON-NLS-1$
        fileDialog.setFilenameFilter(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".job.xml"); //$NON-NLS-1$
            }
        });
        fileDialog.setVisible(true);
        try {
            String filename = fileDialog.getFile();
            if (filename == null) {
                return false;
            }
            if (!filename.toLowerCase().endsWith(".job.xml")) { //$NON-NLS-1$
                filename = filename + ".job.xml"; //$NON-NLS-1$
            }
            File file = new File(new File(fileDialog.getDirectory()), filename);
            if (file.exists()) {
                int ret = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                        file.getName() + " already exists. Do you want to replace it?", //$NON-NLS-1$
                        "Replace file?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE); //$NON-NLS-1$
                if (ret != JOptionPane.YES_OPTION) {
                    return false;
                }
            }
            configuration.saveJob(getJob(), file);
            addRecentJob(file);
            return true;
        }
        catch (Exception e) {
            MessageBoxes.errorBox(frame, "Job Save Error", e.getMessage()); //$NON-NLS-1$
            return false;
        }
    }

    /**
     * Updates the Job controls based on the Job state and the Machine's readiness.
     */
    private void updateJobActions() {
        if (state == State.Stopped) {
            startPauseResumeJobAction.setEnabled(true);
            startPauseResumeJobAction.putValue(AbstractAction.NAME, "Start"); //$NON-NLS-1$
            startPauseResumeJobAction.putValue(AbstractAction.SMALL_ICON, Icons.start);
            startPauseResumeJobAction.putValue(AbstractAction.SHORT_DESCRIPTION,
                    "Start processing the job."); //$NON-NLS-1$
            stopJobAction.setEnabled(false);
            stepJobAction.setEnabled(true);
            tabbedPane.setEnabled(true);
        }
        else if (state == State.Running) {
            startPauseResumeJobAction.setEnabled(true);
            startPauseResumeJobAction.putValue(AbstractAction.NAME, "Pause"); //$NON-NLS-1$
            startPauseResumeJobAction.putValue(AbstractAction.SMALL_ICON, Icons.pause);
            startPauseResumeJobAction.putValue(AbstractAction.SHORT_DESCRIPTION,
                    "Pause processing of the job."); //$NON-NLS-1$
            stopJobAction.setEnabled(true);
            stepJobAction.setEnabled(false);
            tabbedPane.setEnabled(false);
        }
        else if (state == State.Paused) {
            startPauseResumeJobAction.setEnabled(true);
            startPauseResumeJobAction.putValue(AbstractAction.NAME, "Resume"); //$NON-NLS-1$
            startPauseResumeJobAction.putValue(AbstractAction.SMALL_ICON, Icons.start);
            startPauseResumeJobAction.putValue(AbstractAction.SHORT_DESCRIPTION,
                    "Resume processing of the job."); //$NON-NLS-1$
            stopJobAction.setEnabled(true);
            stepJobAction.setEnabled(true);
            tabbedPane.setEnabled(false);
        }
        else if (state == State.Pausing) {
            startPauseResumeJobAction.setEnabled(false);
            stopJobAction.setEnabled(false);
            stepJobAction.setEnabled(false);
            tabbedPane.setEnabled(false);
        }
        else if (state == State.Stopping) {
            startPauseResumeJobAction.setEnabled(false);
            stopJobAction.setEnabled(false);
            stepJobAction.setEnabled(false);
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
        String title = String.format("OpenPnP - %s%s", job.isDirty() ? "*" : "", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                (job.getFile() == null ? UNTITLED_JOB_FILENAME : job.getFile().getName()));
        frame.setTitle(title);
    }
    
    private boolean checkJobStopped() {
        if (state != State.Stopped) {
            MessageBoxes.errorBox(this, "Error", "Job must be stopped first."); //$NON-NLS-1$ //$NON-NLS-2$
            return false;
        }
        return true;
    }

    public void importBoard(Class<? extends BoardImporter> boardImporterClass) {
        if (!checkJobStopped()) {
            return;
        }
        if (getSelection() == null) {
            MessageBoxes.errorBox(getTopLevelAncestor(), "Import Failed", //$NON-NLS-1$
                    "Please select a board in the Jobs tab to import into."); //$NON-NLS-1$
            return;
        }

        BoardImporter boardImporter;
        try {
            boardImporter = boardImporterClass.newInstance();
        }
        catch (Exception e) {
            MessageBoxes.errorBox(getTopLevelAncestor(), "Import Failed", e); //$NON-NLS-1$
            return;
        }

        try {
            Board importedBoard = boardImporter.importBoard((Frame) getTopLevelAncestor());
            if (importedBoard != null) {
                Board existingBoard = getSelection().getBoard();
                for (Placement placement : importedBoard.getPlacements()) {
                    existingBoard.addPlacement(placement);
                }
                for (BoardPad pad : importedBoard.getSolderPastePads()) {
                    // TODO: This is a temporary hack until we redesign the
                    // importer
                    // interface to be more intuitive. The Gerber importer tends
                    // to return everything in Inches, so this is a method to
                    // try to get it closer to what the user expects to see.
                    pad.setLocation(pad.getLocation()
                            .convertToUnits(getSelection().getLocation().getUnits()));
                    existingBoard.addSolderPastePad(pad);
                }
                jobPlacementsPanel.setBoardLocation(getSelection());
                jobPastePanel.setBoardLocation(getSelection());
            }
        }
        catch (Exception e) {
            MessageBoxes.errorBox(getTopLevelAncestor(), "Import Failed", e); //$NON-NLS-1$
        }
    }

    public final Action openJobAction = new AbstractAction(Translations.getString("JobPanel.Action.Job.Open")) { //$NON-NLS-1$
        {
            putValue(MNEMONIC_KEY, KeyEvent.VK_O);
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke('O',
                    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (!checkJobStopped()) {
                return;
            }
            if (!checkForModifications()) {
                return;
            }
            FileDialog fileDialog = new FileDialog(frame);
            fileDialog.setFilenameFilter(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".job.xml"); //$NON-NLS-1$
                }
            });
            fileDialog.setVisible(true);
            try {
                if (fileDialog.getFile() == null) {
                    return;
                }
                File file = new File(new File(fileDialog.getDirectory()), fileDialog.getFile());
                Job job = configuration.loadJob(file);
                setJob(job);
                addRecentJob(file);
            }
            catch (Exception e) {
                e.printStackTrace();
                MessageBoxes.errorBox(frame, "Job Load Error", e.getMessage()); //$NON-NLS-1$
            }
        }
    };

    public final Action newJobAction = new AbstractAction(Translations.getString("JobPanel.Action.Job.New")) { //$NON-NLS-1$
        {
            putValue(MNEMONIC_KEY, KeyEvent.VK_N);
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke('N',
                    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (!checkJobStopped()) {
                return;
            }
            if (!checkForModifications()) {
                return;
            }
            setJob(new Job());
        }
    };

    public final Action saveJobAction = new AbstractAction(Translations.getString("JobPanel.Action.Job.Save")) { //$NON-NLS-1$
        {
            putValue(MNEMONIC_KEY, KeyEvent.VK_S);
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke('S',
                    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            saveJob();
        }
    };

    public final Action saveJobAsAction = new AbstractAction(Translations.getString("JobPanel.Action.Job.SaveAs")) { //$NON-NLS-1$
        {
            putValue(MNEMONIC_KEY, KeyEvent.VK_A);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            saveJobAs();
        }
    };

    /**
     * Initialize the job processor and start the run thread. The run thread will run one step and
     * then either loop if the state is Running or exit if the state is Stepping.
     * 
     * @throws Exception
     */
    public void jobStart() throws Exception {
        String title = tabbedPane.getTitleAt(tabbedPane.getSelectedIndex());
        if (title.equals("Solder Paste")) { //$NON-NLS-1$
            jobProcessor = Configuration.get().getMachine().getPasteDispenseJobProcessor();
        }
        else if (title.equals("Pick and Place")) { //$NON-NLS-1$
            jobProcessor = Configuration.get().getMachine().getPnpJobProcessor();
            
            if (isAllPlaced()) {
                int ret = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                        "All placements have been placed already. Reset all placements before starting job?", //$NON-NLS-1$
                        "Reset placement status?", JOptionPane.YES_NO_OPTION, //$NON-NLS-1$
                        JOptionPane.WARNING_MESSAGE);
                if (ret == JOptionPane.YES_OPTION) {
                    for (BoardLocation boardLocation : job.getBoardLocations()) {
                        boardLocation.clearAllPlaced();
                    }
                    jobPlacementsPanel.refresh();
                }
            }
            
            
        }
        else {
            throw new Error("Programmer error: Unknown tab title."); //$NON-NLS-1$
        }
        jobProcessor.initialize(job);
        jobRun();
    }
    
    public void jobRun() {
        UiUtils.submitUiMachineTask(() -> {
            do {
                if (!jobProcessor.next()) {
                    setState(State.Stopped);
                }
            } while (state == State.Running);
            
            if (state == State.Pausing) {
                setState(State.Paused);
            }

            return null;
        }, (e) -> {

        }, (t) -> {
            List<String> options = new ArrayList<>();
            String retryOption = "Try Again"; //$NON-NLS-1$
            String skipOption = "Skip"; //$NON-NLS-1$
            String ignoreContinueOption = "Ignore and Continue"; //$NON-NLS-1$
            String pauseOption = "Pause Job"; //$NON-NLS-1$

            options.add(retryOption);
            if (jobProcessor.canSkip()) {
                options.add(skipOption);
            }
            if (jobProcessor.canIgnoreContinue()) {
            	options.add(ignoreContinueOption);
            }
            options.add(pauseOption);
            int result = JOptionPane.showOptionDialog(getTopLevelAncestor(), t.getMessage(),
                    "Job Error", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE, null, //$NON-NLS-1$
                    options.toArray(), retryOption);
            String selectedOption = (result<0) ? "Pause Job" : options.get(result);
            if (selectedOption.equals(retryOption)) {
                jobRun();
            }
            // Skip
            else if (selectedOption.equals(skipOption)) {
                UiUtils.messageBoxOnException(() -> {
                    // Tell the job processor to skip the current placement and then call jobRun()
                    // to start things back up, either running or stepping.
                    jobSkip();
                });
            }
            //ignore/continue
            else if (selectedOption.equals(ignoreContinueOption)) {
                UiUtils.messageBoxOnException(() -> {
                    // Tell the job processor ignore error and continue as if everything were normal
                    jobIgnoreContinue();
                });
            }
            // Pause or cancel dialog
            else {
                // We are either Running or Stepping. If Stepping, there is nothing to do. Just
                // clear the dialog and let the user take control. If Running we need to transition
                // to Stepping.
                if (state == State.Running) {
                    try {
                        setState(State.Paused);
                    }
                    catch (Exception e) {
                        // Since we are checking if we're in the Running state this should not
                        // ever happen. If it does, the Error will let us know.
                        e.printStackTrace();
                        throw new Error(e);
                    }
                }
            }
        });
    }

    public void jobSkip() {
        UiUtils.submitUiMachineTask(() -> {
            jobProcessor.skip();
            jobRun();
        });
    }

    public void jobIgnoreContinue() {
        UiUtils.submitUiMachineTask(() -> {
            jobProcessor.ignoreContinue();
            jobRun();
        });
    }

    private void jobAbort() {
        UiUtils.submitUiMachineTask(() -> {
            try {
                jobProcessor.abort();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            setState(State.Stopped);
        });
    }
    
    private void updatePanelizationIconState() {
    	// If more than board is in the job list, then autopanelize isn't allowed
        if (getJob().isUsingPanel() == false && table.getRowCount() > 1){
        	panelizeAction.setEnabled(false);
        	panelizeFiducialCheck.setEnabled(false);
            panelizeXOutAction.setEnabled(false);	
        }
        
        if (getJob().getBoardLocations() == null) {
            panelizeFiducialCheck.setEnabled(false);
            panelizeXOutAction.setEnabled(false);
        }

        // The add existing/new PC icons are only enabled IF
        // 1. The autopanelize feature is not in use
        if (getJob().isUsingPanel() == false) {
            panelizeFiducialCheck.setEnabled(false);
            panelizeXOutAction.setEnabled(false);
            addNewBoardAction.setEnabled(true);
            addBoardAction.setEnabled(true);            
        }
        else {
            addNewBoardAction.setEnabled(false);
            addBoardAction.setEnabled(false);
            panelizeFiducialCheck.setEnabled(true);
            panelizeXOutAction.setEnabled(true);
        }

        // The delete PCB icon is only enabled IF
        // 1. autopanelize is not in use OR
        // 2. autopanelize is in use and row 0 (first pcb) is selected
        if (getJob().isUsingPanel() == false
                || (getJob().isUsingPanel() && table.getSelectedRow() == 0)) {
            removeBoardAction.setEnabled(true);
        }
        else {
            removeBoardAction.setEnabled(false);
        }
    }

    public void populatePanelSettingsIntoBoardLocations() {
        if (getJob().isUsingPanel()) {

            // Here, we're using a panel and 0,0 board location has been updated. At this point,
            // we want the panel to update the other board locations based on the offset and
            // rotation
            // of the 0,0 panel
            getJob().getPanels().get(0).setLocation(getJob());

            tableModel.fireTableDataChanged();
            Helpers.selectFirstTableRow(table);
        }
    }

    public final Action startPauseResumeJobAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.start);
            putValue(NAME, Translations.getString("JobPanel.Action.Job.Start")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("JobPanel.Action.Job.Start.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.messageBoxOnException(() -> {
                if (state == State.Stopped) {
                    setState(State.Running);
                    jobStart();
                }
                else if (state == State.Paused) {
                    setState(State.Running);
                    jobRun();
                }
                // If we're running and the user hits pause we pause.
                else if (state == State.Running) {
                    setState(State.Pausing);
                }
                else {
                    throw new Exception("Don't know how to change from state " + state);
                }
            });
        }
    };

    public final Action stepJobAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.step);
            putValue(NAME, Translations.getString("JobPanel.Action.Job.Step")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("JobPanel.Action.Job.Step.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.messageBoxOnException(() -> {
                if (state == State.Stopped) {
                    setState(State.Pausing);
                    jobStart();
                }
                else if (state == State.Paused) {
                    setState(State.Pausing);
                    jobRun();
                }
            });
        }
    };

    public final Action stopJobAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.stop);
            putValue(NAME, Translations.getString("JobPanel.Action.Job.Stop")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("JobPanel.Action.Job.Stop.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.messageBoxOnException(() -> {
                setState(State.Stopping);
                jobAbort();
            });
        }
    };
    
    public final Action resetAllPlacedAction = new AbstractAction() {
        {
            putValue(NAME, Translations.getString("JobPanel.Action.Job.ResetAllPlaced")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("JobPanel.Action.Job.ResetAllPlaced.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            for (BoardLocation boardLocation : job.getBoardLocations()) {
                boardLocation.clearAllPlaced();
            }
            jobPlacementsPanel.refresh();
        }
    };

    public final Action addBoardAction = new AbstractAction() {
        {
            putValue(NAME, Translations.getString("JobPanel.Action.Job.AddBoard")); //$NON-NLS-1$
            putValue(SMALL_ICON, Icons.add);
            putValue(SHORT_DESCRIPTION, Translations.getString("JobPanel.Action.Job.AddBoard.Description")); //$NON-NLS-1$
            putValue(MNEMONIC_KEY, KeyEvent.VK_A);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {}
    };

    public final Action addNewBoardAction = new AbstractAction() {
        {
            putValue(NAME, Translations.getString("JobPanel.Action.Job.AddBoard.NewBoard")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("JobPanel.Action.Job.AddBoard.NewBoard.Description")); //$NON-NLS-1$
            putValue(MNEMONIC_KEY, KeyEvent.VK_N);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            FileDialog fileDialog = new FileDialog(frame, "Save New Board As...", FileDialog.SAVE); //$NON-NLS-1$
            fileDialog.setFilenameFilter(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".board.xml"); //$NON-NLS-1$
                }
            });
            fileDialog.setVisible(true);
            try {
                String filename = fileDialog.getFile();
                if (filename == null) {
                    return;
                }
                if (!filename.toLowerCase().endsWith(".board.xml")) { //$NON-NLS-1$
                    filename = filename + ".board.xml"; //$NON-NLS-1$
                }
                File file = new File(new File(fileDialog.getDirectory()), filename);

                Board board = configuration.getBoard(file);
                BoardLocation boardLocation = new BoardLocation(board);
                getJob().addBoardLocation(boardLocation);
                tableModel.fireTableDataChanged();

                Helpers.selectLastTableRow(table);
            }
            catch (Exception e) {
                e.printStackTrace();
                MessageBoxes.errorBox(frame, "Unable to create new board", e.getMessage()); //$NON-NLS-1$
            }
            updatePanelizationIconState();
        }
    };

    public final Action addExistingBoardAction = new AbstractAction() {
        {
            putValue(NAME, Translations.getString("JobPanel.Action.Job.AddBoard.ExistingBoard")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("JobPanel.Action.Job.AddBoard.ExistingBoard.Description")); //$NON-NLS-1$
            putValue(MNEMONIC_KEY, KeyEvent.VK_E);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            FileDialog fileDialog = new FileDialog(frame);
            fileDialog.setFilenameFilter(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".board.xml"); //$NON-NLS-1$
                }
            });
            fileDialog.setVisible(true);
            try {
                if (fileDialog.getFile() == null) {
                    return;
                }
                File file = new File(new File(fileDialog.getDirectory()), fileDialog.getFile());

                Board board = configuration.getBoard(file);
                BoardLocation boardLocation = new BoardLocation(board);
                getJob().addBoardLocation(boardLocation);
                // TODO: Move to a list property listener.
                tableModel.fireTableDataChanged();

                Helpers.selectLastTableRow(table);
            }
            catch (Exception e) {
                e.printStackTrace();
                MessageBoxes.errorBox(frame, "Board load failed", e.getMessage()); //$NON-NLS-1$
            }
            updatePanelizationIconState();
        }
    };

    public final Action removeBoardAction = new AbstractAction("Remove Board") { //$NON-NLS-1$
        {
            putValue(SMALL_ICON, Icons.delete);
            putValue(NAME, Translations.getString("JobPanel.Action.Job.RemoveBoard")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("JobPanel.Action.Job.RemoveBoard.Description")); //$NON-NLS-1$
            putValue(MNEMONIC_KEY, KeyEvent.VK_R);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (getJob().isUsingPanel()) {
                getJob().removeAllBoards();
                getJob().removeAllPanels();
                tableModel.fireTableDataChanged();
                addNewBoardAction.setEnabled(true);
                addExistingBoardAction.setEnabled(true);
                removeBoardAction.setEnabled(true);
            }
            else {
                List<BoardLocation> selections = getSelections();
                for (BoardLocation selection : getSelections()) {
                    getJob().removeBoardLocation(selection);
                }
                tableModel.fireTableDataChanged();
            }
            updatePanelizationIconState();
        }
    };

    public final Action captureCameraBoardLocationAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.captureCamera);
            putValue(NAME, "Capture Camera Location"); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION,
                    "Set the board's location to the camera's current position."); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.messageBoxOnException(() -> {
                HeadMountable tool = MainFrame.get().getMachineControls().getSelectedTool();
                Camera camera = tool.getHead().getDefaultCamera();
                double z = getSelection().getLocation().getZ();
                getSelection()
                        .setLocation(camera.getLocation().derive(null, null, z, null));
                tableModel.fireTableRowsUpdated(table.getSelectedRow(),
                        table.getSelectedRow());
            });
        }
    };

    public final Action captureToolBoardLocationAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.captureTool);
            putValue(NAME, "Capture Tool Location"); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, "Set the board's location to the tool's current position."); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            HeadMountable tool = MainFrame.get().getMachineControls().getSelectedTool();
            double z = getSelection().getLocation().getZ();
            getSelection().setLocation(tool.getLocation().derive(null, null, z, null));
            tableModel.fireTableRowsUpdated(table.getSelectedRow(),
                    table.getSelectedRow());
        }
    };

    public final Action moveCameraToBoardLocationAction =
            new AbstractAction("Move Camera To Board Location") { //$NON-NLS-1$
                {
                    putValue(SMALL_ICON, Icons.centerCamera);
                    putValue(NAME, "Move Camera To Board Location"); //$NON-NLS-1$
                    putValue(SHORT_DESCRIPTION, "Position the camera at the board's location."); //$NON-NLS-1$
                }

                @Override
                public void actionPerformed(ActionEvent arg0) {
                    UiUtils.submitUiMachineTask(() -> {
                        HeadMountable tool = MainFrame.get().getMachineControls().getSelectedTool();
                        Camera camera = tool.getHead().getDefaultCamera();
                        MainFrame.get().getCameraViews().ensureCameraVisible(camera);
                        Location location = getSelection().getLocation();
                        MovableUtils.moveToLocationAtSafeZ(camera, location);
                    });
                }
            };
    public final Action moveCameraToBoardLocationNextAction =
            new AbstractAction("Move Camera To Board Location") { //$NON-NLS-1$
                {
                    putValue(SMALL_ICON, Icons.centerCameraMoveNext);
                    putValue(NAME, "Move Camera to the Next Board"); //$NON-NLS-1$
                    putValue(SHORT_DESCRIPTION,
                            "Position the camera at the next board's location."); //$NON-NLS-1$
                }

                @Override
                public void actionPerformed(ActionEvent arg0) {
                    UiUtils.submitUiMachineTask(() -> {
                        // Need to keep current focus owner so that the space bar can be
                        // used after the initial click. Otherwise, button focus is lost
                        // when table is updated
                    	Component comp = MainFrame.get().getFocusOwner();
                    	Helpers.selectNextTableRow(table);
                    	comp.requestFocus();
                       HeadMountable tool = MainFrame.get().getMachineControls().getSelectedTool();
                        Camera camera = tool.getHead().getDefaultCamera();
                        MainFrame.get().getCameraViews().ensureCameraVisible(camera);
                        Location location = getSelection().getLocation();
                        
                        MovableUtils.moveToLocationAtSafeZ(camera, location);
                       
                    });
                }
            };

    public final Action moveToolToBoardLocationAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.centerTool);
            putValue(NAME, "Move Tool To Board Location"); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, "Position the tool at the board's location."); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                HeadMountable tool = MainFrame.get().getMachineControls().getSelectedTool();
                Location location = getSelection().getLocation();
                MovableUtils.moveToLocationAtSafeZ(tool, location);
            });
        }
    };

    public final Action twoPointLocateBoardLocationAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.twoPointLocate);
            putValue(NAME, "Two Point Board Location"); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION,
                    "Set the board's location and rotation using two placements."); //$NON-NLS-1$
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
            putValue(NAME, "Fiducial Check"); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION,
                    "Perform a fiducial check for the board and update it's location and rotation."); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                Location location = Configuration.get().getMachine().getFiducialLocator()
                        .locateBoard(getSelection());
                refreshSelectedBoardRow();
                HeadMountable tool = MainFrame.get().getMachineControls().getSelectedTool();
                Camera camera = tool.getHead().getDefaultCamera();
                MainFrame.get().getCameraViews().ensureCameraVisible(camera);
                MovableUtils.moveToLocationAtSafeZ(camera, location);
            });
        }
    };

    public final Action panelizeAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.autoPanelize);
            putValue(NAME, "Panelize Board"); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, "Autopanelize the loaded board into an array"); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {

            if (job.isUsingPanel() == false) {
                if (job.getBoardLocations().size() > 1) {
                    MessageBoxes.errorBox(frame, "Panelize Error", //$NON-NLS-1$
                            "Panelization can only occur on a single board."); //$NON-NLS-1$
                    return;
                }
            }

            DlgAutoPanelize dlg = new DlgAutoPanelize(frame, JobPanel.this);
            dlg.setVisible(true);
        }
    };

    public final Action panelizeXOutAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.autoPanelizeXOut);
            putValue(NAME, "Xout Panelized"); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, "Skip certain PCBs on Panelized Boards"); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            DlgPanelXOut dlg = new DlgPanelXOut(frame, JobPanel.this);
            dlg.setVisible(true);
        }
    };

    public final Action panelizeFiducialCheck = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.autoPanelizeFidCheck);
            putValue(NAME, "Panelized Fid Check"); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION,
                    "Perform a fiducial check on a panel and update its position and rotation"); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                Helpers.selectFirstTableRow(table);
                Location location = Configuration.get().getMachine().getFiducialLocator()
                        .locateBoard(getSelection(), true);
                getSelection().setLocation(location);
                refreshSelectedBoardRow();
                HeadMountable tool = MainFrame.get().getMachineControls().getSelectedTool();
                Camera camera = tool.getHead().getDefaultCamera();
                MainFrame.get().getCameraViews().ensureCameraVisible(camera);
                MovableUtils.moveToLocationAtSafeZ(camera, location);

            });
        }

    };
    
    public final Action setEnabledAction = new AbstractAction() {
        {
            putValue(NAME, "Set Enabled");
            putValue(SHORT_DESCRIPTION, "Set board(s) enabled to...");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {}
    };

    class SetEnabledAction extends AbstractAction {
        final Boolean value;

        public SetEnabledAction(Boolean value) {
            this.value = value;
            String name = value ? "Enabled" : "Disabled";
            putValue(NAME, name);
            putValue(SHORT_DESCRIPTION, "Set board(s) enabled to " + value);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            for (BoardLocation bl : getSelections()) {
                bl.setEnabled(value);
            }
        }
    };

    public final Action setCheckFidsAction = new AbstractAction() {
        {
            putValue(NAME, "Set Check Fids");
            putValue(SHORT_DESCRIPTION, "Set check fids to...");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {}
    };

    class SetCheckFidsAction extends AbstractAction {
        final Boolean value;

        public SetCheckFidsAction(Boolean value) {
            this.value = value;
            String name = value ? "Check" : "Don't Check";
            putValue(NAME, name);
            putValue(SHORT_DESCRIPTION, "Set check fids to " + value);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            for (BoardLocation bl : getSelections()) {
                bl.setCheckFiducials(value);
            }
        }
    };
    
    public final Action setSideAction = new AbstractAction() {
        {
            putValue(NAME, "Set Side");
            putValue(SHORT_DESCRIPTION, "Set board side(s) to...");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {}
    };

    class SetSideAction extends AbstractAction {
        final Board.Side side;

        public SetSideAction(Board.Side side) {
            this.side = side;
            putValue(NAME, side.toString());
            putValue(SHORT_DESCRIPTION, "Set board side(s) to " + side.toString());
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            for (BoardLocation bl : getSelections()) {
                bl.setSide(side);
            }
            jobPlacementsPanel.setBoardLocation(getSelection());
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
            if (!checkJobStopped()) {
                return;
            }
            if (!checkForModifications()) {
                return;
            }
            try {
                Job job = configuration.loadJob(file);
                setJob(job);
                addRecentJob(file);
            }
            catch (Exception e) {
                e.printStackTrace();
                MessageBoxes.errorBox(frame, "Job Load Error", e.getMessage()); //$NON-NLS-1$
            }
        }
    }

    private final MachineListener machineListener = new MachineListener.Adapter() {
        @Override
        public void machineEnabled(Machine machine) {
            updateJobActions();
        }

        @Override
        public void machineDisabled(Machine machine, String reason) {
            setState(State.Stopped);
            updateJobActions();
        }
    };

    private final PropertyChangeListener titlePropertyChangeListener =
            new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    updateTitle();
                }
            };

    private final TextStatusListener textStatusListener = text -> {
        MainFrame.get().setStatus(text);
        // Repainting here refreshes the tables, which contain status that needs to be updated.
        // Would be better to have property notifiers but this is going to have to do for now.
        repaint();
    };
    
    boolean isAllPlaced() {
    	for (BoardLocation boardLocation : job.getBoardLocations()) {
    	    if (!boardLocation.isEnabled()) {
    	        continue;
    	    }
        	for (Placement placement : boardLocation.getBoard().getPlacements()) {
        	    if (placement.getType() != Type.Place) {
        	        continue;
        	    }
        	    if (placement.getSide() != boardLocation.getSide()) {
        	        continue;
        	    }
        		if (!boardLocation.getPlaced(placement.getId())) {
    				return false;
        		}
        	}
    	}
    	return true;
    }
}
