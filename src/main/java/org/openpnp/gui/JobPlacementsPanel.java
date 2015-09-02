package org.openpnp.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;

import org.openpnp.gui.components.AutoSelectTextTable;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.components.reticle.PackageReticle;
import org.openpnp.gui.components.reticle.Reticle;
import org.openpnp.gui.support.ActionGroup;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.IdentifiableListCellRenderer;
import org.openpnp.gui.support.IdentifiableTableCellRenderer;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.PartsComboBoxModel;
import org.openpnp.gui.tablemodel.PlacementsTableModel;
import org.openpnp.gui.tablemodel.PlacementsTableModel.Status;
import org.openpnp.model.Board.Side;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.openpnp.model.Placement.Type;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Nozzle;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.Utils2D;

public class JobPlacementsPanel extends JPanel {
    private JTable placementsTable;
    private PlacementsTableModel placementsTableModel;
    private ActionGroup boardLocationSelectionActionGroup;
    private ActionGroup placementSelectionActionGroup;
    private ActionGroup placementCaptureAndPositionActionGroup;
    private BoardLocation boardLocation;

    public JobPlacementsPanel(JobPanel jobPanel) {
        Configuration configuration = Configuration.get();
        
        boardLocationSelectionActionGroup = new ActionGroup(newPlacementAction);
        boardLocationSelectionActionGroup.setEnabled(false);

        placementSelectionActionGroup = new ActionGroup(removePlacementAction,
                editPlacementFeederAction);
        placementSelectionActionGroup.setEnabled(false);

        placementCaptureAndPositionActionGroup = new ActionGroup(
                captureCameraPlacementLocation, captureToolPlacementLocation,
                moveCameraToPlacementLocation, moveToolToPlacementLocation);
        placementCaptureAndPositionActionGroup.setEnabled(false);

        JComboBox<PartsComboBoxModel> partsComboBox = new JComboBox<>(
                new PartsComboBoxModel());
        partsComboBox.setRenderer(new IdentifiableListCellRenderer<Part>());
        JComboBox<Side> sidesComboBox = new JComboBox<>(Side.values());
        JComboBox<Type> typesComboBox = new JComboBox<>(Type.values());

        setLayout(new BorderLayout(0, 0));
        JToolBar toolBarPlacements = new JToolBar();
        add(toolBarPlacements, BorderLayout.NORTH);

        toolBarPlacements.setFloatable(false);
        JButton btnNewPlacement = new JButton(newPlacementAction);
        btnNewPlacement.setHideActionText(true);
        toolBarPlacements.add(btnNewPlacement);
        JButton btnRemovePlacement = new JButton(removePlacementAction);
        btnRemovePlacement.setHideActionText(true);
        toolBarPlacements.add(btnRemovePlacement);
        toolBarPlacements.addSeparator();
        JButton btnCaptureCameraPlacementLocation = new JButton(
                captureCameraPlacementLocation);
        btnCaptureCameraPlacementLocation.setHideActionText(true);
        toolBarPlacements.add(btnCaptureCameraPlacementLocation);

        JButton btnCaptureToolPlacementLocation = new JButton(
                captureToolPlacementLocation);
        btnCaptureToolPlacementLocation.setHideActionText(true);
        toolBarPlacements.add(btnCaptureToolPlacementLocation);

        JButton btnPositionCameraPositionLocation = new JButton(
                moveCameraToPlacementLocation);
        btnPositionCameraPositionLocation.setHideActionText(true);
        toolBarPlacements.add(btnPositionCameraPositionLocation);

        JButton btnPositionToolPositionLocation = new JButton(
                moveToolToPlacementLocation);
        btnPositionToolPositionLocation.setHideActionText(true);
        toolBarPlacements.add(btnPositionToolPositionLocation);

        toolBarPlacements.addSeparator();

        JButton btnEditFeeder = new JButton(editPlacementFeederAction);
        btnEditFeeder.setHideActionText(true);
        toolBarPlacements.add(btnEditFeeder);

        placementsTableModel = new PlacementsTableModel(configuration);

        placementsTable = new AutoSelectTextTable(placementsTableModel);
        placementsTable.setAutoCreateRowSorter(true);
        placementsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        placementsTable.setDefaultEditor(Side.class, new DefaultCellEditor(
                sidesComboBox));
        placementsTable.setDefaultEditor(Part.class, new DefaultCellEditor(
                partsComboBox));
        placementsTable.setDefaultEditor(Type.class, new DefaultCellEditor(
                typesComboBox));
        placementsTable.setDefaultRenderer(Part.class,
                new IdentifiableTableCellRenderer<Part>());
        placementsTable.setDefaultRenderer(PlacementsTableModel.Status.class,
                new StatusRenderer());
        placementsTable.setDefaultRenderer(Placement.Type.class,
                new TypeRenderer());
        placementsTable.getSelectionModel().addListSelectionListener(
                new ListSelectionListener() {
                    @Override
                    public void valueChanged(ListSelectionEvent e) {
                        if (e.getValueIsAdjusting()) {
                            return;
                        }
                        placementSelectionActionGroup
                                .setEnabled(getSelectedPlacement() != null);
                        placementCaptureAndPositionActionGroup
                                .setEnabled(getSelectedPlacement() != null
                                        && getSelectedPlacement().getSide() == boardLocation
                                                .getSide());
                        Placement placement = getSelectedPlacement();
                        CameraView cameraView = MainFrame.cameraPanel
                                .getSelectedCameraView();
                        if (cameraView != null) {
                            if (placement != null) {
                                Reticle reticle = new PackageReticle(placement
                                        .getPart().getPackage());
                                cameraView.setReticle(JobPlacementsPanel.this
                                        .getClass().getName(), reticle);
                            }
                            else {
                                cameraView
                                        .removeReticle(JobPlacementsPanel.this
                                                .getClass().getName());
                            }
                        }
                    }
                });
        placementsTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent mouseEvent) {
                if (mouseEvent.getClickCount() != 2) {
                    return;
                }
                int row = placementsTable.rowAtPoint(new Point(mouseEvent
                        .getX(), mouseEvent.getY()));
                int col = placementsTable.columnAtPoint(new Point(mouseEvent
                        .getX(), mouseEvent.getY()));
                if (placementsTableModel.getColumnClass(col) == Status.class) {
                    Status status = (Status) placementsTableModel.getValueAt(
                            row, col);
                    // TODO: This is some sample code for handling the user
                    // wishing to do something with the status. Not using it
                    // right now but leaving it here for the future.
                    System.out.println(status);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(placementsTable);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void setBoardLocation(BoardLocation boardLocation) {
        this.boardLocation = boardLocation;
        if (boardLocation == null) {
            placementsTableModel.setBoard(null);
            boardLocationSelectionActionGroup.setEnabled(false);
        }
        else {
            placementsTableModel.setBoard(boardLocation.getBoard());
            boardLocationSelectionActionGroup.setEnabled(true);
        }
    }

    public Placement getSelectedPlacement() {
        if (boardLocation == null) {
            return null;
        }
        int index = placementsTable.getSelectedRow();
        if (index == -1) {
            return null;
        }
        else {
            index = placementsTable.convertRowIndexToModel(index);
            return boardLocation.getBoard().getPlacements()
                    .get(index);
        }
    }

    public final Action newPlacementAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.add);
            putValue(NAME, "New Placement");
            putValue(SHORT_DESCRIPTION,
                    "Create a new placement and add it to the board.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (Configuration.get().getParts().size() == 0) {
                MessageBoxes
                        .errorBox(
                                getTopLevelAncestor(),
                                "Error",
                                "There are currently no parts defined in the system. Please create at least one part before creating a placement.");
                return;
            }

            String id = JOptionPane.showInputDialog(getTopLevelAncestor(),
                    "Please enter an ID for the new placement.");
            if (id == null) {
                return;
            }
            // TODO: Make sure it's unique.
            Placement placement = new Placement(id);

            placement.setPart(Configuration.get().getParts().get(0));
            placement.setLocation(new Location(Configuration.get()
                    .getSystemUnits()));

            boardLocation.getBoard().addPlacement(placement);
            placementsTableModel.fireTableDataChanged();
            Helpers.selectLastTableRow(placementsTable);
        }
    };

    public final Action removePlacementAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.delete);
            putValue(NAME, "Remove Placement");
            putValue(SHORT_DESCRIPTION,
                    "Remove the currently selected placement.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            Placement placement = getSelectedPlacement();
            boardLocation.getBoard().removePlacement(placement);
            placementsTableModel.fireTableDataChanged();
        }
    };

    public final Action moveCameraToPlacementLocation = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.centerCamera);
            putValue(NAME, "Move Camera To Placement Location");
            putValue(SHORT_DESCRIPTION,
                    "Position the camera at the placement's location.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            Location placementLocation = Utils2D
                    .calculateBoardPlacementLocation(boardLocation
                            .getLocation(), boardLocation
                            .getSide(), getSelectedPlacement().getLocation());

            final Camera camera = MainFrame.cameraPanel.getSelectedCamera();
            if (camera.getHead() == null) {
                MessageBoxes.errorBox(getTopLevelAncestor(), "Move Error",
                        "Camera is not movable.");
                return;
            }
            final Location location = placementLocation;
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

    public final Action moveToolToPlacementLocation = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.centerTool);
            putValue(NAME, "Move Tool To Placement Location");
            putValue(SHORT_DESCRIPTION,
                    "Position the tool at the placement's location.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            Location placementLocation = Utils2D
                    .calculateBoardPlacementLocation(boardLocation
                            .getLocation(), boardLocation
                            .getSide(), getSelectedPlacement().getLocation());

            final Nozzle nozzle = MainFrame.machineControlsPanel
                    .getSelectedNozzle();
            final Location location = placementLocation;
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

    public final Action captureCameraPlacementLocation = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.captureCamera);
            putValue(NAME, "Capture Camera Placement Location");
            putValue(SHORT_DESCRIPTION,
                    "Set the placement's location to the camera's current position.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            final Camera camera = MainFrame.cameraPanel.getSelectedCamera();
            if (camera.getHead() == null) {
                MessageBoxes.errorBox(getTopLevelAncestor(), "Error",
                        "Camera is not movable.");
                return;
            }
            Location placementLocation = Utils2D
                    .calculateBoardPlacementLocation(boardLocation
                            .getLocation(), getSelectedPlacement().getSide(),
                            MainFrame.cameraPanel.getSelectedCameraLocation()
                                    .invert(true, true, true, true));
            getSelectedPlacement().setLocation(
                    placementLocation.invert(true, true, true, true));
            placementsTable.repaint();
        }
    };

    public final Action captureToolPlacementLocation = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.captureTool);
            putValue(NAME, "Capture Tool Placement Location");
            putValue(SHORT_DESCRIPTION,
                    "Set the placement's location to the tool's current position.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            Nozzle nozzle = MainFrame.machineControlsPanel.getSelectedNozzle();
            Location placementLocation = Utils2D
                    .calculateBoardPlacementLocation(boardLocation
                            .getLocation(), getSelectedPlacement().getSide(),
                            nozzle.getLocation().invert(true, true, true, true));
            getSelectedPlacement().setLocation(
                    placementLocation.invert(true, true, true, true));
            placementsTable.repaint();
        }
    };

    public final Action editPlacementFeederAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.editFeeder);
            putValue(NAME, "Edit Placement Feeder");
            putValue(SHORT_DESCRIPTION,
                    "Edit the placement's associated feeder definition.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            Placement placement = getSelectedPlacement();
            Feeder feeder = null;
            for (Feeder f : Configuration.get().getMachine().getFeeders()) {
                if (f.getPart() == placement.getPart()) {
                    feeder = f;
                }
            }
            MainFrame.feedersPanel.showFeeder(feeder);
        }
    };

    static class TypeRenderer extends DefaultTableCellRenderer {
        public void setValue(Object value) {
            Type type = (Type) value;
            setText(type.name());
            if (type == Type.Fiducial) {
                setBackground(Color.cyan);
            }
            else if (type == Type.Ignore) {
                setBackground(Color.yellow);
            }
            else if (type == Type.Place) {
                setBackground(Color.green);
            }
        }
    }

    static class StatusRenderer extends DefaultTableCellRenderer {
        public void setValue(Object value) {
            Status status = (Status) value;
            if (status == Status.Ready) {
                setBackground(Color.green);
                setText("Ready");
            }
            else if (status == Status.MissingFeeder) {
                setBackground(Color.yellow);
                setText("Missing Feeder");
            }
            else if (status == Status.MissingPart) {
                setBackground(Color.red);
                setText("Missing Part");
            }
            else if (status == Status.MissingPart) {
                setBackground(Color.red);
                setText(status.toString());
            }
        }
    }
}
