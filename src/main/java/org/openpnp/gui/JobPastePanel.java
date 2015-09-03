package org.openpnp.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openpnp.gui.components.AutoSelectTextTable;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.support.ActionGroup;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.tablemodel.PadsTableModel;
import org.openpnp.model.Board.Side;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.model.Pad;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Nozzle;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.Utils2D;

public class JobPastePanel extends JPanel {
    private JTable table;
    private PadsTableModel tableModel;
    private ActionGroup boardLocationSelectionActionGroup;
    private ActionGroup padSelectionActionGroup;
    private ActionGroup padCaptureAndPositionActionGroup;
    private BoardLocation boardLocation;

    public JobPastePanel(JobPanel jobPanel) {
        Configuration configuration = Configuration.get();
        
        boardLocationSelectionActionGroup = new ActionGroup(newPadAction);
        boardLocationSelectionActionGroup.setEnabled(false);

        padSelectionActionGroup = new ActionGroup(removePadAction);
        padSelectionActionGroup.setEnabled(false);

        padCaptureAndPositionActionGroup = new ActionGroup(moveCameraToPadLocation, moveToolToPadLocation);
        padCaptureAndPositionActionGroup.setEnabled(false);

        JComboBox<Side> sidesComboBox = new JComboBox<>(Side.values());

        setLayout(new BorderLayout(0, 0));
        JToolBar toolBar = new JToolBar();
        add(toolBar, BorderLayout.NORTH);

        toolBar.setFloatable(false);
        JButton btnNewPad = new JButton(newPadAction);
        btnNewPad.setHideActionText(true);
        toolBar.add(btnNewPad);
        JButton btnRemovePad = new JButton(removePadAction);
        btnRemovePad.setHideActionText(true);
        toolBar.add(btnRemovePad);
        toolBar.addSeparator();


        JButton btnPositionCameraPositionLocation = new JButton(
                moveCameraToPadLocation);
        btnPositionCameraPositionLocation.setHideActionText(true);
        toolBar.add(btnPositionCameraPositionLocation);

        JButton btnPositionToolPositionLocation = new JButton(
                moveToolToPadLocation);
        btnPositionToolPositionLocation.setHideActionText(true);
        toolBar.add(btnPositionToolPositionLocation);

        toolBar.addSeparator();

        tableModel = new PadsTableModel(configuration);

        table = new AutoSelectTextTable(tableModel);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultEditor(Side.class, new DefaultCellEditor(
                sidesComboBox));
        table.getSelectionModel().addListSelectionListener(
                new ListSelectionListener() {
                    @Override
                    public void valueChanged(ListSelectionEvent e) {
                        if (e.getValueIsAdjusting()) {
                            return;
                        }
                        padSelectionActionGroup
                                .setEnabled(getSelectedPad() != null);
                        padCaptureAndPositionActionGroup
                                .setEnabled(getSelectedPad() != null
                                        && getSelectedPad().getSide() == boardLocation
                                                .getSide());
                        Pad pad = getSelectedPad();
                        CameraView cameraView = MainFrame.cameraPanel
                                .getSelectedCameraView();
                        if (cameraView != null) {
                            if (pad != null) {
                                // TODO
//                                Reticle reticle = new PackageReticle(pad
//                                        .getPart().getPackage());
//                                cameraView.setReticle(JobPastePanel.this
//                                        .getClass().getName(), reticle);
                            }
                            else {
                                cameraView
                                        .removeReticle(JobPastePanel.this
                                                .getClass().getName());
                            }
                        }
                    }
                });

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void setBoardLocation(BoardLocation boardLocation) {
        this.boardLocation = boardLocation;
        if (boardLocation == null) {
            tableModel.setBoard(null);
            boardLocationSelectionActionGroup.setEnabled(false);
        }
        else {
            tableModel.setBoard(boardLocation.getBoard());
            boardLocationSelectionActionGroup.setEnabled(true);
        }
    }

    public Pad getSelectedPad() {
        if (boardLocation == null) {
            return null;
        }
        int index = table.getSelectedRow();
        if (index == -1) {
            return null;
        }
        else {
            index = table.convertRowIndexToModel(index);
            return boardLocation.getBoard().getSolderPastePads().get(index);
        }
    }

    public final Action newPadAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.add);
            putValue(NAME, "New Pad");
            putValue(SHORT_DESCRIPTION,
                    "Create a new pad and add it to the board.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            // TODO: FAKE
            Pad pad = new Pad.RoundRectangle();
            
            pad.setLocation(new Location(Configuration.get()
                    .getSystemUnits()));

            boardLocation.getBoard().addSolderPastePad(pad);
            tableModel.fireTableDataChanged();
            Helpers.selectLastTableRow(table);
        }
    };

    public final Action removePadAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.delete);
            putValue(NAME, "Remove Pad");
            putValue(SHORT_DESCRIPTION,
                    "Remove the currently selected pad.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            Pad pad = getSelectedPad();
            boardLocation.getBoard().removeSolderPastePad(pad);
            tableModel.fireTableDataChanged();
        }
    };

    public final Action moveCameraToPadLocation = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.centerCamera);
            putValue(NAME, "Move Camera To Pad Location");
            putValue(SHORT_DESCRIPTION,
                    "Position the camera at the pad's location.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            // TODO: Probably wrong
            Location padLocation = Utils2D
                    .calculateBoardPlacementLocation(boardLocation
                            .getLocation(), boardLocation
                            .getSide(), getSelectedPad().getLocation());

            final Camera camera = MainFrame.cameraPanel.getSelectedCamera();
            if (camera.getHead() == null) {
                MessageBoxes.errorBox(getTopLevelAncestor(), "Move Error",
                        "Camera is not movable.");
                return;
            }
            final Location location = padLocation;
            MainFrame.machineControlsPanel.submitMachineTask(new Runnable() {
                public void run() {
                    try {
                        MovableUtils.moveToLocationAtSafeZ(camera, location,
                                1.0);
                    }
                    catch (Exception e) {
                        MessageBoxes.errorBox(getTopLevelAncestor(),
                                "Move Error", e);
                    }
                }
            });
        }
    };

    public final Action moveToolToPadLocation = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.centerTool);
            putValue(NAME, "Move Tool To Pad Location");
            putValue(SHORT_DESCRIPTION,
                    "Position the tool at the pad's location.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            // TODO: Probably wrong
            Location padLocation = Utils2D
                    .calculateBoardPlacementLocation(boardLocation
                            .getLocation(), boardLocation
                            .getSide(), getSelectedPad().getLocation());

            final Nozzle nozzle = MainFrame.machineControlsPanel
                    .getSelectedNozzle();
            final Location location = padLocation;
            MainFrame.machineControlsPanel.submitMachineTask(new Runnable() {
                public void run() {
                    try {
                        MovableUtils.moveToLocationAtSafeZ(nozzle, location,
                                1.0);
                    }
                    catch (Exception e) {
                        MessageBoxes.errorBox(getTopLevelAncestor(),
                                "Move Error", e);
                    }
                }
            });
        }
    };
}
