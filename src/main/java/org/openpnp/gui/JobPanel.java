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
import java.awt.Component;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.openpnp.ConfigurationListener;
import org.openpnp.Translations;
import org.openpnp.events.FiducialLocatableLocationSelectedEvent;
import org.openpnp.events.JobLoadedEvent;
import org.openpnp.events.PlacementSelectedEvent;
import org.openpnp.gui.components.AutoSelectTextTable;
import org.openpnp.gui.importer.BoardImporter;
import org.openpnp.gui.panelization.DlgAutoPanelize;
import org.openpnp.gui.panelization.DlgPanelXOut;
import org.openpnp.gui.processes.MultiPlacementBoardLocationProcess;
import org.openpnp.gui.support.ActionGroup;
import org.openpnp.gui.support.CustomBooleanRenderer;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.tablemodel.BoardLocationsTableModel;
import org.openpnp.gui.tablemodel.FiducialLocatableLocationsTableModel;
import org.openpnp.model.Board;
import org.openpnp.model.Board.Side;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.BoardPad;
import org.openpnp.model.Configuration;
import org.openpnp.model.FiducialLocatable;
import org.openpnp.model.FiducialLocatableLocation;
import org.openpnp.model.Job;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Panel;
import org.openpnp.model.PanelLocation;
import org.openpnp.model.Motion;
import org.openpnp.model.Placement;
import org.openpnp.model.Placement.Type;
import org.openpnp.spi.Camera;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.JobProcessor;
import org.openpnp.spi.JobProcessor.TextStatusListener;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MachineListener;
import org.openpnp.spi.MotionPlanner;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.UiUtils;
import org.openpnp.util.Utils2D;
import org.pmw.tinylog.Logger;

import com.google.common.eventbus.Subscribe;
import java.awt.FlowLayout;
import java.awt.CardLayout;

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

    private FiducialLocatableLocationsTableModel jobTableModel;
    private JTable jobTable;
    private JSplitPane splitPane;

    private ActionGroup singleSelectionActionGroup;
    private ActionGroup multiSelectionActionGroup;

    private Preferences prefs = Preferences.userNodeForPackage(JobPanel.class);

    public JMenu mnOpenRecent;

    private List<File> recentJobs = new ArrayList<>();

    private final JobPlacementsPanel jobPlacementsPanel;

    private Job job;

    private JobProcessor jobProcessor;
    
    private State state = State.Stopped;
    
    // try https://tips4java.wordpress.com/2010/01/24/table-row-rendering/ to show affine transform set

    public JobPanel(Configuration configuration, MainFrame frame) {
        this.configuration = configuration;
        this.frame = frame;

        singleSelectionActionGroup =
                new ActionGroup(removeBoardAction, captureCameraBoardLocationAction,
                        captureToolBoardLocationAction, moveCameraToBoardLocationAction,
                        moveCameraToBoardLocationNextAction, moveToolToBoardLocationAction,
                        twoPointLocateBoardLocationAction, fiducialCheckAction,
                        setEnabledAction,setCheckFidsAction, setSideAction);
        singleSelectionActionGroup.setEnabled(false);
        
        multiSelectionActionGroup = new ActionGroup(removeBoardAction, setEnabledAction, setCheckFidsAction, setSideAction);
        multiSelectionActionGroup.setEnabled(false);
        
        jobTableModel = new FiducialLocatableLocationsTableModel(configuration);


        // Suppress because adding the type specifiers breaks WindowBuilder.
        @SuppressWarnings({"unchecked", "rawtypes"})
        JComboBox sidesComboBox = new JComboBox(Side.values());

        jobTable = new AutoSelectTextTable(jobTableModel) {
            @Override
            public String getToolTipText(MouseEvent e) {

                java.awt.Point p = e.getPoint();
                int row = rowAtPoint(p);
                int col = columnAtPoint(p);

                if (row >= 0) {
                    if (col == 0) {
                        row = jobTable.convertRowIndexToModel(row);
                        FiducialLocatableLocation fiducialLocatableLocation =
                                job.getFiducialLocatableLocations().get(row);
                        if (fiducialLocatableLocation != null) {
                            return fiducialLocatableLocation.getFiducialLocatable()
                                                .getFile()
                                                .toString();
                        }
                    }
                }

                return super.getToolTipText();
            }
        };

        RowFilter<Object, Object> notFirstRow = new RowFilter<Object, Object>() {
            public boolean include(Entry<? extends Object, ? extends Object> entry) {
                return (Integer) entry.getIdentifier() != 0;
            }};
        jobTable.setAutoCreateRowSorter(true);
        ((TableRowSorter<? extends TableModel>) jobTable.getRowSorter()).setRowFilter(notFirstRow);
        jobTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        jobTable.setDefaultEditor(Side.class, new DefaultCellEditor(sidesComboBox));
        jobTable.setDefaultRenderer(Boolean.class, new CustomBooleanRenderer());
        jobTable.getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                Logger.trace("TableModelEvent = " +
                    "col:" + e.getColumn() +
                    " firstRow:" + e.getFirstRow() +
                    " lastRow:" + e.getLastRow() +
                    " source:" + e.getSource() +
                    " type:" + e.getType() );
                SwingUtilities.invokeLater(() -> {
                    jobPlacementsPanel.refresh();
                });
            }
        });
        
        jobTable.getSelectionModel()
                .addListSelectionListener(new ListSelectionListener() {
                    @Override
                    public void valueChanged(ListSelectionEvent e) {
                        if (e.getValueIsAdjusting()) {
                            return;
                        }
                        
                        List<FiducialLocatableLocation> selections = getSelections();
                        if (selections.size() == 0) {
                            singleSelectionActionGroup.setEnabled(false);
                            multiSelectionActionGroup.setEnabled(false);
                            jobPlacementsPanel.setFiducialLocatableLocation(null);
                            Configuration.get().getBus()
                                .post(new FiducialLocatableLocationSelectedEvent(null, JobPanel.this));
                            Configuration.get().getBus()
                            .post(new PlacementSelectedEvent(null, null, JobPanel.this));
                        }
                        else if (selections.size() == 1) {
                            multiSelectionActionGroup.setEnabled(false);
                            singleSelectionActionGroup.setEnabled(true);
                            jobPlacementsPanel.setFiducialLocatableLocation(selections.get(0));
                            if (selections.get(0).getParent() != job.getRootPanelLocation()) {
                                Configuration.get().getBus()
                                .post(new FiducialLocatableLocationSelectedEvent((FiducialLocatableLocation) selections.get(0).getParent().getDefinedBy(), JobPanel.this));
                            }
                            Configuration.get().getBus()
                            .post(new FiducialLocatableLocationSelectedEvent((FiducialLocatableLocation) selections.get(0).getDefinedBy(), JobPanel.this));
                            Configuration.get().getBus()
                            .post(new PlacementSelectedEvent(null, selections.get(0), JobPanel.this));
                        }
                        else {
                            singleSelectionActionGroup.setEnabled(false);
                            multiSelectionActionGroup.setEnabled(true);
                            jobPlacementsPanel.setFiducialLocatableLocation(null);
                            Configuration.get().getBus()
                                .post(new FiducialLocatableLocationSelectedEvent(null, JobPanel.this));
                            Configuration.get().getBus()
                            .post(new PlacementSelectedEvent(null, null, JobPanel.this));
                        }
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
        pnlBoards.setBorder(new TitledBorder(null,
                Translations.getString("JobPanel.Tab.Boards"),
                TitledBorder.LEADING, TitledBorder.TOP, null)); //$NON-NLS-1$
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
                menu.addSeparator();
                menu.add(new JMenuItem(addNewPanelAction));
                menu.add(new JMenuItem(addExistingPanelAction));
                menu.show(btnAddBoard, (int) btnAddBoard.getWidth(), (int) btnAddBoard.getHeight());
            }
        });
        toolBarBoards.add(btnAddBoard);
        JButton btnRemoveBoard = new JButton(removeBoardAction);
        btnRemoveBoard.setHideActionText(true);
        toolBarBoards.add(btnRemoveBoard);
        
        toolBarBoards.addSeparator();
        
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

        JButton btnCaptureCameraBoardLocation = new JButton(captureCameraBoardLocationAction);
        btnCaptureCameraBoardLocation.setHideActionText(true);
        toolBarBoards.add(btnCaptureCameraBoardLocation);

        JButton btnCaptureToolBoardLocation = new JButton(captureToolBoardLocationAction);
        btnCaptureToolBoardLocation.setHideActionText(true);
        toolBarBoards.add(btnCaptureToolBoardLocation);

        
        toolBarBoards.addSeparator();

        JButton btnTwoPointBoardLocation = new JButton(twoPointLocateBoardLocationAction);
        toolBarBoards.add(btnTwoPointBoardLocation);
        btnTwoPointBoardLocation.setHideActionText(true);

        JButton btnFiducialCheck = new JButton(fiducialCheckAction);
        toolBarBoards.add(btnFiducialCheck);
        btnFiducialCheck.setHideActionText(true);
        toolBarBoards.addSeparator();
//        JButton btnPanelize = new JButton(panelizeAction);
//        toolBarBoards.add(btnPanelize);
//        btnPanelize.setHideActionText(true);
//        JButton btnPanelizeXOut = new JButton(panelizeXOutAction);
//        toolBarBoards.add(btnPanelizeXOut);
//        btnPanelizeXOut.setHideActionText(true);
//        JButton btnPanelizeFidCheck = new JButton(panelizeFiducialCheck);
//        toolBarBoards.add(btnPanelizeFidCheck);
//        btnPanelizeFidCheck.setHideActionText(true);

        pnlBoards.add(new JScrollPane(jobTable));

        splitPane.setLeftComponent(pnlBoards);

        jobPlacementsPanel = new JobPlacementsPanel(this);
        splitPane.setRightComponent(jobPlacementsPanel);
        
        add(splitPane);

        mnOpenRecent = new JMenu(Translations.getString("JobPanel.Action.Job.RecentJobs")); //$NON-NLS-1$
        mnOpenRecent.setMnemonic(KeyEvent.VK_R);
        loadRecentJobs();

        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            public void configurationComplete(Configuration configuration) throws Exception {
                Machine machine = configuration.getMachine();

                machine.addListener(machineListener);

                machine.getPnpJobProcessor().addTextStatusListener(textStatusListener);

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

        jobTable.setComponentPopupMenu(popupMenu);

        Configuration.get().getBus().register(this);
    }
    
    void setState(State newState) {
        this.state = newState;
        updateJobActions();
    }
    
    public JTable getFiducialLocatableLocationsTable() {
        return jobTable;
    }

    @Subscribe
    public void fiducialLocatableLocationSelected(FiducialLocatableLocationSelectedEvent event) {
        if (event.source == this) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
//            MainFrame.get().showTab("Job"); //$NON-NLS-1$

            selectFiducialLocatableLocation(event.fiducialLocatableLocation);
        });
    }

    @Subscribe
    public void placementSelected(PlacementSelectedEvent event) {
        if (event.source == this || event.source == jobPlacementsPanel) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
//            MainFrame.get().showTab("Job"); //$NON-NLS-1$

            selectFiducialLocatableLocation(event.fiducialLocatableLocation);

            jobPlacementsPanel.selectPlacement(event.placement);
        });
    }

    private void selectFiducialLocatableLocation(FiducialLocatableLocation fiducialLocatableLocation) {
        if (fiducialLocatableLocation == null) {
            jobTable.getSelectionModel().clearSelection();
        }
        for (int i = 0; i < jobTableModel.getRowCount(); i++) {
            if (job.getFiducialLocatableLocations().get(i) == fiducialLocatableLocation) {
                int index = jobTable.convertRowIndexToView(i);
                jobTable.getSelectionModel().setSelectionInterval(index, index);
                jobTable.scrollRectToVisible(
                        new Rectangle(jobTable.getCellRect(index, 0, true)));
                break;
            }
        }
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
        jobTableModel.setJob(job);
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
        jobTableModel.fireTableDataChanged();
    }

    public void refreshSelectedRow() {
        int index = jobTable.convertRowIndexToModel(jobTable.getSelectedRow());
        jobTableModel.fireTableRowsUpdated(index, index);
    }

    public FiducialLocatableLocation getSelection() {
        List<FiducialLocatableLocation> selections = getSelections();
        if (selections.isEmpty()) {
            return null;
        }
        return selections.get(0);
    }

    public List<FiducialLocatableLocation> getSelections() {
        ArrayList<FiducialLocatableLocation> selections = new ArrayList<>();
        int[] selectedRows = jobTable.getSelectedRows();
        for (int selectedRow : selectedRows) {
            selectedRow = jobTable.convertRowIndexToModel(selectedRow);
            selections.add(job.getFiducialLocatableLocations().get(selectedRow));
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
        if (!checkForPanelModifications()) {
            return false;
        }
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

    private boolean checkForPanelModifications() {
        for (Panel panel : configuration.getPanels()) {
            if (panel.isDirty()) {
                int result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                        "Do you want to save your changes to " + panel.getFile().getName() + "?" //$NON-NLS-1$ //$NON-NLS-2$
                                + "\n" + "If you don't save, your changes will be lost.", //$NON-NLS-1$ //$NON-NLS-2$
                        "Save " + panel.getFile().getName() + "?", //$NON-NLS-1$ //$NON-NLS-2$
                        JOptionPane.YES_NO_CANCEL_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    try {
                        configuration.savePanel(panel);
                    }
                    catch (Exception e) {
                        MessageBoxes.errorBox(getTopLevelAncestor(), "Panel Save Error", //$NON-NLS-1$
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
                e.printStackTrace();
                MessageBoxes.errorBox(frame, "Job Save Error", e.toString()); //$NON-NLS-1$
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
            startPauseResumeJobAction.putValue(AbstractAction.NAME,
                    Translations.getString("JobPanel.Action.Job.Start")); //$NON-NLS-1$
            startPauseResumeJobAction.putValue(AbstractAction.SMALL_ICON, Icons.start);
            startPauseResumeJobAction.putValue(AbstractAction.SHORT_DESCRIPTION,
                    Translations.getString("JobPanel.Action.Job.Start.Description")); //$NON-NLS-1$
            stopJobAction.setEnabled(false);
            stepJobAction.setEnabled(true);
        }
        else if (state == State.Running) {
            startPauseResumeJobAction.setEnabled(true);
            startPauseResumeJobAction.putValue(AbstractAction.NAME,
                    Translations.getString("JobPanel.Action.Job.Pause")); //$NON-NLS-1$
            startPauseResumeJobAction.putValue(AbstractAction.SMALL_ICON, Icons.pause);
            startPauseResumeJobAction.putValue(AbstractAction.SHORT_DESCRIPTION,
                    Translations.getString("JobPanel.Action.Job.Pause.Description")); //$NON-NLS-1$
            stopJobAction.setEnabled(true);
            stepJobAction.setEnabled(false);
        }
        else if (state == State.Paused) {
            startPauseResumeJobAction.setEnabled(true);
            startPauseResumeJobAction.putValue(AbstractAction.NAME,
                    Translations.getString("JobPanel.Action.Job.Resume")); //$NON-NLS-1$
            startPauseResumeJobAction.putValue(AbstractAction.SMALL_ICON, Icons.start);
            startPauseResumeJobAction.putValue(AbstractAction.SHORT_DESCRIPTION,
                    Translations.getString("JobPanel.Action.Job.Resume.Description")); //$NON-NLS-1$
            stopJobAction.setEnabled(true);
            stepJobAction.setEnabled(true);
        }
        else if (state == State.Pausing) {
            startPauseResumeJobAction.setEnabled(false);
            stopJobAction.setEnabled(false);
            stepJobAction.setEnabled(false);
        }
        else if (state == State.Stopping) {
            startPauseResumeJobAction.setEnabled(false);
            stopJobAction.setEnabled(false);
            stepJobAction.setEnabled(false);
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
                Board existingBoard = (Board) ((BoardLocation) getSelection()).getBoard();
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
                jobPlacementsPanel.setFiducialLocatableLocation((BoardLocation) getSelection());
                frame.getFeedersTab().updateView();
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
                frame.getFeedersTab().updateView();
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
        jobProcessor = Configuration.get().getMachine().getPnpJobProcessor();
        if (isAllPlaced()) {
            int ret = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                    "All placements have been placed already. Reset all placements before starting job?", //$NON-NLS-1$
                    "Reset placement status?", JOptionPane.YES_NO_OPTION, //$NON-NLS-1$
                    JOptionPane.WARNING_MESSAGE);
            if (ret == JOptionPane.YES_OPTION) {
                job.clearAllPlaced();
//                for (BoardLocation boardLocation : job.getBoardLocations()) {
//                    boardLocation.clearAllPlaced();
//                }
                jobPlacementsPanel.refresh();
            }
        }
        jobProcessor.initialize(job);
        jobRun();
    }
    
    public void jobRun() {
        UiUtils.submitUiMachineTask(() -> {
            // For optional motion stepping, remember the past move.
            MotionPlanner motionPlanner = Configuration.get().getMachine().getMotionPlanner();
            Motion pastMotion = motionPlanner.getLastMotion();
            do {
                do { 
                    if (!jobProcessor.next()) {
                        setState(State.Stopped);
                        break;
                    }
                    else if (state == State.Pausing) {
                        // We're pausing, but check if we need motion before we can pause for real.
                        Motion lastMotion = motionPlanner.getLastMotion();
                        if (! (jobProcessor.isSteppingToNextMotion() && lastMotion == pastMotion)) {
                            break;
                        }
                    }
                }
                while (state == State.Pausing);
            } while (state == State.Running);
            
            if (state == State.Pausing) {
                setState(State.Paused);
            }

            return null;
        }, (e) -> {

        }, (t) -> {
            /**
             * TODO It would be nice to give the user the ability to single click suppress errors
             * on the currently processing placement, but that requires knowledge of the currently
             * processing placement. With the current model where JobProcessor is available for
             * both dispense and PnP this is not possible. Once dispense is removed we can include
             * the current placement in the thrown error and add this feature.
             */
            
            MessageBoxes.errorBox(getTopLevelAncestor(), "Job Error", t.getMessage());
            if (state == State.Running || state == State.Pausing) {
                setState(State.Paused);
            }
            else if (state == State.Stopping) {
                setState(State.Stopped);
            }
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
    
//    private void updatePanelizationIconState() {
//    	// If more than board is in the job list, then autopanelize isn't allowed
//        if (getJob().isUsingPanel() == false && jobTable.getRowCount() > 1){
//        	panelizeAction.setEnabled(false);
//        	panelizeFiducialCheck.setEnabled(false);
//            panelizeXOutAction.setEnabled(false);	
//        }
//        
//        if (getJob().getBoardLocations() == null) {
//            panelizeFiducialCheck.setEnabled(false);
//            panelizeXOutAction.setEnabled(false);
//        }
//
//        // The add existing/new PC icons are only enabled IF
//        // 1. The autopanelize feature is not in use
//        if (getJob().isUsingPanel() == false) {
//            panelizeFiducialCheck.setEnabled(false);
//            panelizeXOutAction.setEnabled(false);
//            addNewBoardAction.setEnabled(true);
//            addBoardAction.setEnabled(true);            
//        }
//        else {
//            addNewBoardAction.setEnabled(false);
//            addBoardAction.setEnabled(false);
//            panelizeFiducialCheck.setEnabled(true);
//            panelizeXOutAction.setEnabled(true);
//        }
//
//        // The delete PCB icon is only enabled IF
//        // 1. autopanelize is not in use OR
//        // 2. autopanelize is in use and row 0 (first pcb) is selected
//        if (getJob().isUsingPanel() == false
//                || (getJob().isUsingPanel() && jobTable.getSelectedRow() == 0)) {
//            removeBoardAction.setEnabled(true);
//        }
//        else {
//            removeBoardAction.setEnabled(false);
//        }
//    }

//    public void populatePanelSettingsIntoBoardLocations() {
//        if (getJob().isUsingPanel()) {
//
//            // Here, we're using a panel and 0,0 board location has been updated. At this point,
//            // we want the panel to update the other board locations based on the offset and
//            // rotation
//            // of the 0,0 panel
//            getJob().getPanels().get(0).setLocation(getJob());
//
//            tableModel.fireTableDataChanged();
//            Helpers.selectFirstTableRow(table);
//        }
//    }

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
            job.clearAllPlaced();
//            for (BoardLocation boardLocation : job.getBoardLocations()) {
//                boardLocation.clearAllPlaced();
//            }
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
            fileDialog.setFile("*.board.xml");
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

                addBoard(file);

                Helpers.selectLastTableRow(jobTable);
            }
            catch (Exception e) {
                e.printStackTrace();
                MessageBoxes.errorBox(frame, "Unable to create new board", e.getMessage()); //$NON-NLS-1$
            }
//            updatePanelizationIconState();
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
            fileDialog.setFile("*.board.xml");
            fileDialog.setVisible(true);
            try {
                if (fileDialog.getFile() == null) {
                    return;
                }
                File file = new File(new File(fileDialog.getDirectory()), fileDialog.getFile());

                addBoard(file);

                Helpers.selectLastTableRow(jobTable);
            }
            catch (Exception e) {
                e.printStackTrace();
                MessageBoxes.errorBox(frame, "Board load failed", e.getMessage()); //$NON-NLS-1$
            }
//            updatePanelizationIconState();
        }
    };

    protected void addBoard(File file) throws Exception {
        Board board = configuration.getBoard(file);
        BoardLocation boardLocation = new BoardLocation(board);
        getJob().addBoardLocation(boardLocation);
        getJob().getRootPanelLocation().addChild(boardLocation);
        // TODO: Move to a list property listener.
        jobTableModel.fireTableDataChanged();
    }
    
    public final Action addNewPanelAction = new AbstractAction() {
        {
            putValue(NAME, Translations.getString("JobPanel.Action.Job.AddBoard.NewPanel")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("JobPanel.Action.Job.AddBoard.NewPanel.Description")); //$NON-NLS-1$
//            putValue(MNEMONIC_KEY, KeyEvent.VK_N);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            FileDialog fileDialog = new FileDialog(frame, "Save New Panel As...", FileDialog.SAVE); //$NON-NLS-1$
            fileDialog.setFilenameFilter(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".panel.xml"); //$NON-NLS-1$
                }
            });
            fileDialog.setFile("*.panel.xml");
            fileDialog.setVisible(true);
            try {
                String filename = fileDialog.getFile();
                if (filename == null) {
                    return;
                }
                if (!filename.toLowerCase().endsWith(".panel.xml")) { //$NON-NLS-1$
                    filename = filename + ".panel.xml"; //$NON-NLS-1$
                }
                File file = new File(new File(fileDialog.getDirectory()), filename);

                Panel panel = configuration.getPanel(file);
                PanelLocation panelLocation = new PanelLocation(panel);
                getJob().addPanelLocation(panelLocation);
                getJob().getRootPanelLocation().addChild(panelLocation);
                jobTableModel.fireTableDataChanged();

//                Helpers.selectLastTableRow(jobTable);
                Helpers.selectObjectTableRow(jobTable, panelLocation);
            }
            catch (Exception e) {
                e.printStackTrace();
                MessageBoxes.errorBox(frame, "Unable to create new panel", e.getMessage()); //$NON-NLS-1$
            }
//            updatePanelizationIconState();
        }
    };

    public final Action addExistingPanelAction = new AbstractAction() {
        {
            putValue(NAME, Translations.getString("JobPanel.Action.Job.AddBoard.ExistingPanel")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("JobPanel.Action.Job.AddBoard.ExistingPanel.Description")); //$NON-NLS-1$
//            putValue(MNEMONIC_KEY, KeyEvent.VK_E);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            FileDialog fileDialog = new FileDialog(frame);
            fileDialog.setFilenameFilter(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".panel.xml"); //$NON-NLS-1$
                }
            });
            fileDialog.setFile("*.panel.xml");
            fileDialog.setVisible(true);
            try {
                if (fileDialog.getFile() == null) {
                    return;
                }
                File file = new File(new File(fileDialog.getDirectory()), fileDialog.getFile());

                PanelLocation panelLocation = new PanelLocation();
                panelLocation.setFileName(file.getAbsolutePath());
                getJob().addPanelLocation(panelLocation);
//                getJob().getRootPanelLocation().addChild(panelLocation);
//                configuration.resolvePanels(job, job.getPanelLocations());
                configuration.resolvePanel(job, panelLocation);
                
                // TODO: Move to a list property listener.
                jobTableModel.fireTableDataChanged();

                Helpers.selectObjectTableRow(jobTable, panelLocation);
            }
            catch (Exception e) {
                e.printStackTrace();
                MessageBoxes.errorBox(frame, "Panel load failed", e.getMessage()); //$NON-NLS-1$
            }
//            updatePanelizationIconState();
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
//            if (getJob().isUsingPanel()) {
//                getJob().removeAllBoards();
//                getJob().removeAllPanels();
//                jobTableModel.fireTableDataChanged();
//                addNewBoardAction.setEnabled(true);
//                addExistingBoardAction.setEnabled(true);
//                removeBoardAction.setEnabled(true);
//            }
//            else {
                for (FiducialLocatableLocation selection : getSelections()) {
                    getJob().removeFiducialLocatableLocation(selection);
                    getJob().getRootPanelLocation().removeChild(selection);

                }
                jobTableModel.fireTableDataChanged();
//            }
//            updatePanelizationIconState();
        }
    };

    public final Action captureCameraBoardLocationAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.captureCamera);
            putValue(NAME,Translations.getString("JobPanel.Action.Job.Board.CaptureCameraLocation")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION,
                    Translations.getString("JobPanel.Action.Job.Board.CaptureCameraLocation.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.messageBoxOnException(() -> {
                if (getSelection().getParent() != job.getRootPanelLocation()) {
                    throw new Exception("Can't update the location of a board or panel that is a child of a larger panel.");
                }
                HeadMountable tool = MainFrame.get().getMachineControls().getSelectedTool();
                Camera camera = tool.getHead().getDefaultCamera();
                double z = getSelection().getLocation().getZ();
                getSelection()
                        .setLocation(camera.getLocation().derive(null, null, z, null));
                refresh();
            });
        }
    };

    public final Action captureToolBoardLocationAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.captureTool);
            putValue(NAME, Translations.getString("JobPanel.Action.Job.Board.CaptureToolLocation")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("JobPanel.Action.Job.Board.CaptureToolLocation.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.messageBoxOnException(() -> {
                if (getSelection().getParent() != job.getRootPanelLocation()) {
                    throw new Exception("Can't update the location of a board or panel that is a child of a larger panel.");
                }
                HeadMountable tool = MainFrame.get().getMachineControls().getSelectedTool();
                double z = getSelection().getLocation().getZ();
                getSelection().setLocation(tool.getLocation().derive(null, null, z, null));
                refresh();
            });
        }
    };

    public final Action moveCameraToBoardLocationAction =
            new AbstractAction(Translations.getString("JobPanel.Action.Job.Camera.PositionAtBoardLocation")) { //$NON-NLS-1$
                {
                    putValue(SMALL_ICON, Icons.centerCamera);
                    putValue(NAME, Translations.getString("JobPanel.Action.Job.Camera.PositionAtBoardLocation")); //$NON-NLS-1$
                    putValue(SHORT_DESCRIPTION, Translations.getString("JobPanel.Action.Job.Camera.PositionAtBoardLocation.Description")); //$NON-NLS-1$
                }

                @Override
                public void actionPerformed(ActionEvent arg0) {
                    UiUtils.submitUiMachineTask(() -> {
                        HeadMountable tool = MainFrame.get().getMachineControls().getSelectedTool();
                        Camera camera = tool.getHead().getDefaultCamera();
                        Location location = getSelection().getGlobalLocation();
                        MovableUtils.moveToLocationAtSafeZ(camera, location);
                        MovableUtils.fireTargetedUserAction(camera);

                        Map<String, Object> globals = new HashMap<>();
                        globals.put("camera", camera);
                        Configuration.get().getScripting().on("Camera.AfterPosition", globals);
                    });
                }
            };
    public final Action moveCameraToBoardLocationNextAction =
            new AbstractAction(Translations.getString("JobPanel.Action.Job.Camera.PositionAtNextBoardLocation")) { //$NON-NLS-1$
                {
                    putValue(SMALL_ICON, Icons.centerCameraMoveNext);
                    putValue(NAME, Translations.getString("JobPanel.Action.Job.Camera.PositionAtNextBoardLocation")); //$NON-NLS-1$
                    putValue(SHORT_DESCRIPTION,
                            Translations.getString("JobPanel.Action.Job.Camera.PositionAtNextBoardLocation.Description")); //$NON-NLS-1$
                }

                @Override
                public void actionPerformed(ActionEvent arg0) {
                    UiUtils.submitUiMachineTask(() -> {
                        // Need to keep current focus owner so that the space bar can be
                        // used after the initial click. Otherwise, button focus is lost
                        // when table is updated
                    	Component comp = MainFrame.get().getFocusOwner();
                    	Helpers.selectNextTableRow(jobTable);
                    	comp.requestFocus();
                        HeadMountable tool = MainFrame.get().getMachineControls().getSelectedTool();
                        Camera camera = tool.getHead().getDefaultCamera();
                        Location location = getSelection().getGlobalLocation();
                        MovableUtils.moveToLocationAtSafeZ(camera, location);
                        MovableUtils.fireTargetedUserAction(camera);

                        Map<String, Object> globals = new HashMap<>();
                        globals.put("camera", camera);
                        Configuration.get().getScripting().on("Camera.AfterPosition", globals);
                    });
                }
            };

    public final Action moveToolToBoardLocationAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.centerTool);
            putValue(NAME, Translations.getString("JobPanel.Action.Job.Tool.PositionAtBoardLocation")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("JobPanel.Action.Job.Tool.PositionAtBoardLocation.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                HeadMountable tool = MainFrame.get().getMachineControls().getSelectedTool();
                Location location = getSelection().getGlobalLocation();
                MovableUtils.moveToLocationAtSafeZ(tool, location);
                MovableUtils.fireTargetedUserAction(tool);
            });
        }
    };

    public final Action twoPointLocateBoardLocationAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.twoPointLocate);
            putValue(NAME, Translations.getString("JobPanel.Action.Job.Board.TwoPointBoardLocation")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION,
                    Translations.getString("JobPanel.Action.Job.Board.TwoPointBoardLocation.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.messageBoxOnException(() -> {
                if (!(getSelection() instanceof BoardLocation)) {
                    throw new Exception("Can't locate a Panel using placements.");
                }
                new MultiPlacementBoardLocationProcess(frame, JobPanel.this);
            });
        }
    };

    public final Action fiducialCheckAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.fiducialCheck);
            putValue(NAME, Translations.getString("JobPanel.Action.Job.Board.FiducialCheck")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION,
                    Translations.getString("JobPanel.Action.Job.Board.FiducialCheck.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                FiducialLocatableLocation fiducialLocatableLocation = getSelection();
                Location location = Configuration.get().getMachine().getFiducialLocator()
                        .locateBoard(fiducialLocatableLocation);
                
                /**
                 * Set the board's location to the one returned from the fiducial check. We have
                 * to store and restore the placement transform because setting the location
                 * clears it.
                 */
                if (fiducialLocatableLocation.getParent() == job.getRootPanelLocation()) {
                    AffineTransform tx = fiducialLocatableLocation.getLocalToParentTransform();
                    fiducialLocatableLocation.setLocation(location);
                    fiducialLocatableLocation.setLocalToParentTransform(tx);
                }
                refresh();
                
                /**
                 * Move the camera to the calculated position.
                 */
                HeadMountable tool = MainFrame.get().getMachineControls().getSelectedTool();
                Camera camera = tool.getHead().getDefaultCamera();
                MovableUtils.moveToLocationAtSafeZ(camera, location);
                MovableUtils.fireTargetedUserAction(camera);
            });
        }
    };

//    public final Action panelizeAction = new AbstractAction() {
//        {
//            putValue(SMALL_ICON, Icons.autoPanelize);
//            putValue(NAME, Translations.getString("JobPanel.Action.Job.Board.Panelize")); //$NON-NLS-1$
//            putValue(SHORT_DESCRIPTION, Translations.getString("JobPanel.Action.Job.Board.Panelize.Description")); //$NON-NLS-1$
//        }
//
//        @Override
//        public void actionPerformed(ActionEvent arg0) {
//
//            if (job.isUsingPanel() == false) {
//                if (job.getBoardLocations().size() > 1) {
//                    MessageBoxes.errorBox(frame,
//                            Translations.getString("JobPanel.Action.Job.Board.Panelize.Error"), //$NON-NLS-1$
//                            Translations.getString("JobPanel.Action.Job.Board.Panelize.Error.Description")); //$NON-NLS-1$
//                    return;
//                }
//            }
//
//            DlgAutoPanelize dlg = new DlgAutoPanelize(frame, JobPanel.this);
//            dlg.setVisible(true);
//        }
//    };

//    public final Action panelizeXOutAction = new AbstractAction() {
//        {
//            putValue(SMALL_ICON, Icons.autoPanelizeXOut);
//            putValue(NAME, Translations.getString("JobPanel.Action.Job.Board.Panelize.SkipBoard")); //$NON-NLS-1$
//            putValue(SHORT_DESCRIPTION, Translations.getString("JobPanel.Action.Job.Board.Panelize.SkipBoard.Description")); //$NON-NLS-1$
//        }
//
//        @Override
//        public void actionPerformed(ActionEvent arg0) {
//            DlgPanelXOut dlg = new DlgPanelXOut(frame, JobPanel.this);
//            dlg.setVisible(true);
//        }
//    };
//
//    public final Action panelizeFiducialCheck = new AbstractAction() {
//        {
//            putValue(SMALL_ICON, Icons.autoPanelizeFidCheck);
//            putValue(NAME, Translations.getString("JobPanel.Action.Job.Board.Panelize.FiducialCheck")); //$NON-NLS-1$
//            putValue(SHORT_DESCRIPTION,
//                    Translations.getString("JobPanel.Action.Job.Board.Panelize.FiducialCheck.Description")); //$NON-NLS-1$
//        }
//
//        @Override
//        public void actionPerformed(ActionEvent arg0) {
//            UiUtils.submitUiMachineTask(() -> {
//                Helpers.selectFirstTableRow(jobTable);
//                Location location = Configuration.get().getMachine().getFiducialLocator()
//                        .locateBoard(getSelection(), true);
//
//                /**
//                 * Set the panel's location to the one returned from the fiducial check. We have
//                 * to store and restore the placement transform because setting the location
//                 * clears it.
//                 */
//                AffineTransform tx = getSelection().getLocalToParentTransform();
//                getSelection().setLocation(location);
//                getSelection().setLocalToParentTransform(tx);
//                refreshSelectedRow();
//                
//                HeadMountable tool = MainFrame.get().getMachineControls().getSelectedTool();
//                Camera camera = tool.getHead().getDefaultCamera();
//                MovableUtils.moveToLocationAtSafeZ(camera, location);
//                MovableUtils.fireTargetedUserAction(camera);
//            });
//        }
//
//    };
    
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
            List<FiducialLocatableLocation> selections = getSelections();
            for (FiducialLocatableLocation bl : selections) {
                if (bl.isParentBranchEnabled()) {
                    bl.setLocallyEnabled(value);
                }
            }
            refresh();
            Helpers.selectObjectTableRows(jobTable, selections);
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
            for (FiducialLocatableLocation bl : getSelections()) {
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
            List<FiducialLocatableLocation> selections = getSelections();
            for (FiducialLocatableLocation bl : selections) {
                if (bl.getParent() == job.getRootPanelLocation() && bl.getSide() != side) {
                    Side oldSide = bl.getSide();
                    if (side != oldSide) {
                        Location savedLocation = bl.getGlobalLocation();
                        bl.setSide(side);
                        if (bl.getParent() == job.getRootPanelLocation()) {
                            bl.setGlobalLocation(savedLocation);
                        }
                        bl.setLocalToParentTransform(null);
                    }
                    jobTableModel.fireDecendantsUpdated(bl);
                }
            }
            jobPlacementsPanel.refresh();
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
                frame.getFeedersTab().updateView();
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
                if (placement.getType() != Type.Placement) {
                    continue;
                }
                if (!placement.isEnabled()) {
                    continue;
                }
        	    if (placement.getSide() != boardLocation.getSide()) {
        	        continue;
        	    }
//        		if (!boardLocation.getPlaced(placement.getId())) {
//    				return false;
//        		}
                if (!job.getPlaced(boardLocation, placement.getId())) {
                    return false;
                }
        	}
    	}
    	return true;
    }
}
