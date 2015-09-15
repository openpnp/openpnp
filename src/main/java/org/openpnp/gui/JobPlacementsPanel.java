package org.openpnp.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

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
    private JTable table;
    private PlacementsTableModel tableModel;
    private ActionGroup boardLocationSelectionActionGroup;
    private ActionGroup singleSelectionActionGroup;
    private ActionGroup multiSelectionActionGroup;
    private ActionGroup captureAndPositionActionGroup;
    private BoardLocation boardLocation;

    public JobPlacementsPanel(JobPanel jobPanel) {
        Configuration configuration = Configuration.get();
        
        boardLocationSelectionActionGroup = new ActionGroup(newAction);
        boardLocationSelectionActionGroup.setEnabled(false);

        singleSelectionActionGroup = new ActionGroup(removeAction,
                editPlacementFeederAction, setTypeAction);
        singleSelectionActionGroup.setEnabled(false);

        multiSelectionActionGroup = new ActionGroup(removeAction, setTypeAction);
        multiSelectionActionGroup.setEnabled(false);

        captureAndPositionActionGroup = new ActionGroup(
                captureCameraPlacementLocation, captureToolPlacementLocation,
                moveCameraToPlacementLocation, moveToolToPlacementLocation);
        captureAndPositionActionGroup.setEnabled(false);

        JComboBox<PartsComboBoxModel> partsComboBox = new JComboBox(
                new PartsComboBoxModel());
        partsComboBox.setRenderer(new IdentifiableListCellRenderer<Part>());
        JComboBox<Side> sidesComboBox = new JComboBox(Side.values());
        JComboBox<Type> typesComboBox = new JComboBox(Type.values());

        setLayout(new BorderLayout(0, 0));
        JToolBar toolBarPlacements = new JToolBar();
        add(toolBarPlacements, BorderLayout.NORTH);

        toolBarPlacements.setFloatable(false);
        JButton btnNewPlacement = new JButton(newAction);
        btnNewPlacement.setHideActionText(true);
        toolBarPlacements.add(btnNewPlacement);
        JButton btnRemovePlacement = new JButton(removeAction);
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

        tableModel = new PlacementsTableModel(configuration);

        table = new AutoSelectTextTable(tableModel);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setDefaultEditor(Side.class, new DefaultCellEditor(
                sidesComboBox));
        table.setDefaultEditor(Part.class, new DefaultCellEditor(
                partsComboBox));
        table.setDefaultEditor(Type.class, new DefaultCellEditor(
                typesComboBox));
        table.setDefaultRenderer(Part.class,
                new IdentifiableTableCellRenderer<Part>());
        table.setDefaultRenderer(PlacementsTableModel.Status.class,
                new StatusRenderer());
        table.setDefaultRenderer(Placement.Type.class,
                new TypeRenderer());
        table.getSelectionModel().addListSelectionListener(
                new ListSelectionListener() {
                    @Override
                    public void valueChanged(ListSelectionEvent e) {
                        if (e.getValueIsAdjusting()) {
                            return;
                        }
                        
                        if (getSelections().size() > 1) {
                            // multi select
                            singleSelectionActionGroup.setEnabled(false);
                            captureAndPositionActionGroup.setEnabled(false);
                            multiSelectionActionGroup.setEnabled(true);
                        }
                        else {
                            // single select, or no select
                            multiSelectionActionGroup.setEnabled(false);
                            singleSelectionActionGroup.setEnabled(getSelection() != null);
                            captureAndPositionActionGroup.setEnabled(getSelection() != null && getSelection().getSide() == boardLocation.getSide());
                        }
                        showReticle();
                    }
                });
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent mouseEvent) {
                if (mouseEvent.getClickCount() != 2) {
                    return;
                }
                int row = table.rowAtPoint(new Point(mouseEvent
                        .getX(), mouseEvent.getY()));
                int col = table.columnAtPoint(new Point(mouseEvent
                        .getX(), mouseEvent.getY()));
                if (tableModel.getColumnClass(col) == Status.class) {
                    Status status = (Status) tableModel.getValueAt(
                            row, col);
                    // TODO: This is some sample code for handling the user
                    // wishing to do something with the status. Not using it
                    // right now but leaving it here for the future.
                    System.out.println(status);
                }
            }
        });
        
        JPopupMenu popupMenu = new JPopupMenu();
        
        JMenu setTypeMenu = new JMenu(setTypeAction);
        setTypeMenu.add(new SetTypeAction(Placement.Type.Place));
        setTypeMenu.add(new SetTypeAction(Placement.Type.Ignore));
        setTypeMenu.add(new SetTypeAction(Placement.Type.Fiducial));
        popupMenu.add(setTypeMenu);

        table.setComponentPopupMenu(popupMenu);        

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);
    }
    
    private void showReticle() {
        Placement placement;
        if (getSelections().size() > 1) {
            placement = null;
        }
        else {
            placement = getSelection();
        }
        CameraView cameraView = MainFrame.cameraPanel.getSelectedCameraView();
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

    public Placement getSelection() {
        List<Placement> selectedPlacements = getSelections();
        if (selectedPlacements.isEmpty()) {
            return null;
        }
        return selectedPlacements.get(0);
    }
    
    public List<Placement> getSelections() {
        ArrayList<Placement> placements = new ArrayList<Placement>();
        if (boardLocation == null) {
            return placements;
        }
        int[] selectedRows = table.getSelectedRows();
        for (int selectedRow : selectedRows) {
            selectedRow = table.convertRowIndexToModel(selectedRow);
            placements.add(boardLocation.getBoard().getPlacements().get(selectedRow));
        }
        return placements;
    }

    public final Action newAction = new AbstractAction() {
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
            tableModel.fireTableDataChanged();
            Helpers.selectLastTableRow(table);
        }
    };

    public final Action removeAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.delete);
            putValue(NAME, "Remove Placement(s)");
            putValue(SHORT_DESCRIPTION,
                    "Remove the currently selected placement(s).");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            for (Placement placement : getSelections()) {
                boardLocation.getBoard().removePlacement(placement);
            }
            tableModel.fireTableDataChanged();
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
                            .getSide(), getSelection().getLocation());

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
                            .getSide(), getSelection().getLocation());

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
                            .getLocation(), getSelection().getSide(),
                            MainFrame.cameraPanel.getSelectedCameraLocation()
                                    .invert(true, true, true, true));
            getSelection().setLocation(
                    placementLocation.invert(true, true, true, true));
            table.repaint();
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
                            .getLocation(), getSelection().getSide(),
                            nozzle.getLocation().invert(true, true, true, true));
            getSelection().setLocation(
                    placementLocation.invert(true, true, true, true));
            table.repaint();
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
            Placement placement = getSelection();
            Feeder feeder = null;
            for (Feeder f : Configuration.get().getMachine().getFeeders()) {
                if (f.getPart() == placement.getPart()) {
                    feeder = f;
                }
            }
            MainFrame.feedersPanel.showFeeder(feeder);
        }
    };
    
    public final Action setTypeAction = new AbstractAction() {
        {
            putValue(NAME, "Set Type");
            putValue(SHORT_DESCRIPTION,
                    "Set placement type(s) to...");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
        }
    };
    
    class SetTypeAction extends AbstractAction {
        final Placement.Type type;
        
        public SetTypeAction(Placement.Type type) {
            this.type = type;
            putValue(NAME, type.toString());
            putValue(SHORT_DESCRIPTION, "Set placement type(s) to " + type.toString());
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            for (Placement placement : getSelections()) {
                placement.setType(type);
            }
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
