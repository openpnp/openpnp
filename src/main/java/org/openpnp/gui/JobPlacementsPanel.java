package org.openpnp.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;

import org.openpnp.gui.components.AutoSelectTextTable;
import org.openpnp.gui.support.ActionGroup;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.IdentifiableListCellRenderer;
import org.openpnp.gui.support.IdentifiableTableCellRenderer;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.PartsComboBoxModel;
import org.openpnp.gui.tablemodel.PlacementsTableModel;
import org.openpnp.gui.tablemodel.PlacementsTableModel.Status;
import org.openpnp.model.Board;
import org.openpnp.model.Board.Side;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.openpnp.model.Placement.Type;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Nozzle;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.UiUtils;
import org.openpnp.util.Utils2D;

public class JobPlacementsPanel extends JPanel {
    private JTable table;
    private PlacementsTableModel tableModel;
    private ActionGroup boardLocationSelectionActionGroup;
    private ActionGroup singleSelectionActionGroup;
    private ActionGroup multiSelectionActionGroup;
    private ActionGroup captureAndPositionActionGroup;
    private BoardLocation boardLocation;
    
    private static Color typeColorIgnore = new Color(252, 255, 157);
    private static Color typeColorFiducial = new Color(157, 188, 255);
    private static Color typeColorPlace = new Color(157, 255, 168);
    private static Color statusColorWarning = new Color(252, 255, 157);
    private static Color statusColorReady = new Color(157, 255, 168);
    private static Color statusColorError = new Color(255, 157, 157);

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
        table.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == ' ') {
                    Placement placement = getSelection();
                    placement.setType(placement.getType() == Type.Place ? Type.Ignore : Type.Place);
                    tableModel.fireTableRowsUpdated(table.getSelectedRow(), table.getSelectedRow());
                }
                else {
                    super.keyTyped(e);
                }
            }
        });
        
        JPopupMenu popupMenu = new JPopupMenu();
        
        JMenu setTypeMenu = new JMenu(setTypeAction);
        for (Placement.Type type : Placement.Type.values()) {
            setTypeMenu.add(new SetTypeAction(type));
        }
        popupMenu.add(setTypeMenu);

        JMenu setSideMenu = new JMenu(setSideAction);
        for (Board.Side side : Board.Side.values()) {
        	setSideMenu.add(new SetSideAction(side));
        }
        popupMenu.add(setSideMenu);

        table.setComponentPopupMenu(popupMenu);        

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

    public Placement getSelection() {
        List<Placement> selectedPlacements = getSelections();
        if (selectedPlacements.isEmpty()) {
            return null;
        }
        return selectedPlacements.get(0);
    }
    
    public List<Placement> getSelections() {
        ArrayList<Placement> placements = new ArrayList<>();
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
        	UiUtils.submitUiMachineTask(() -> {
                Location location = Utils2D.calculateBoardPlacementLocation(
                		boardLocation,
                		getSelection().getLocation());

                Camera camera = MainFrame
                		.machineControlsPanel
                		.getSelectedTool()
                		.getHead()
                		.getDefaultCamera();
                MovableUtils.moveToLocationAtSafeZ(camera, location, 1.0);
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
            Location location = Utils2D.calculateBoardPlacementLocation(
            		boardLocation,
            		getSelection().getLocation());

            Nozzle nozzle = MainFrame.machineControlsPanel
                    .getSelectedNozzle();
            UiUtils.submitUiMachineTask(() -> {
                MovableUtils.moveToLocationAtSafeZ(nozzle, location, 1.0);
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
        	UiUtils.messageBoxOnException(() -> {
            	HeadMountable tool = MainFrame.machineControlsPanel.getSelectedTool();
            	Camera camera = tool.getHead().getDefaultCamera();
                Location placementLocation = Utils2D.calculateBoardPlacementLocationInverse(
                		boardLocation,
                        camera.getLocation());
                getSelection().setLocation(placementLocation);
                table.repaint();
        	});
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
                    .calculateBoardPlacementLocationInverse(
                		boardLocation,
                        nozzle.getLocation());
                getSelection().setLocation(placementLocation);
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
            MainFrame.feedersPanel.showFeederForPart(placement.getPart());
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
    
    public final Action setSideAction = new AbstractAction() {
        {
            putValue(NAME, "Set Side");
            putValue(SHORT_DESCRIPTION,
                    "Set placement side(s) to...");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
        }
    };
    
    class SetSideAction extends AbstractAction {
        final Board.Side side;
        
        public SetSideAction(Board.Side side) {
            this.side = side;
            putValue(NAME, side.toString());
            putValue(SHORT_DESCRIPTION, "Set placement side(s) to " + side.toString());
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            for (Placement placement : getSelections()) {
                placement.setSide(side);
            }
        }
    };
    
    static class TypeRenderer extends DefaultTableCellRenderer {
        public void setValue(Object value) {
            Type type = (Type) value;
            setText(type.name());
            if (type == Type.Fiducial) {
                setBorder(new LineBorder(getBackground()));
                setForeground(Color.black);
                setBackground(typeColorFiducial);
            }
            else if (type == Type.Ignore) {
                setBorder(new LineBorder(getBackground()));
                setForeground(Color.black);
                setBackground(typeColorIgnore);
            }
            else if (type == Type.Place) {
                setBorder(new LineBorder(getBackground()));
                setForeground(Color.black);
                setBackground(typeColorPlace);
            }
        }
    }
    
    static class StatusRenderer extends DefaultTableCellRenderer {
        public void setValue(Object value) {
            Status status = (Status) value;
            if (status == Status.Ready) {
                setBorder(new LineBorder(getBackground()));
                setForeground(Color.black);
                setBackground(statusColorReady);
                setText("Ready");
            }
            else if (status == Status.MissingFeeder) {
                setBorder(new LineBorder(getBackground()));
                setForeground(Color.black);
                setBackground(statusColorError);
                setText("Missing Feeder");
            }
            else if (status == Status.ZeroPartHeight) {
                setBorder(new LineBorder(getBackground()));
                setForeground(Color.black);
                setBackground(statusColorWarning);
                setText("Part Height");
            }
            else if (status == Status.MissingPart) {
                setBorder(new LineBorder(getBackground()));
                setForeground(Color.black);
                setBackground(statusColorError);
                setText("Missing Part");
            }
            else {
                setBorder(new LineBorder(getBackground()));
                setForeground(Color.black);
                setBackground(statusColorError);
                setText(status.toString());
            }
        }
    }
}
