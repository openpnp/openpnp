package org.openpnp.gui;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.PatternSyntaxException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.openpnp.events.PlacementSelectedEvent;
import org.openpnp.gui.components.AutoSelectTextTable;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.components.CameraViewFilter;
import org.openpnp.gui.support.ActionGroup;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.IdentifiableListCellRenderer;
import org.openpnp.gui.support.IdentifiableTableCellRenderer;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.PartsComboBoxModel;
import org.openpnp.gui.tablemodel.PlacementsTableModel;
import org.openpnp.gui.tablemodel.PlacementsTableModel.Status;
import org.openpnp.machine.reference.camera.SimulatedUpCamera;
import org.openpnp.model.Board;
import org.openpnp.model.Board.Side;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.openpnp.model.Placement.ErrorHandling;
import org.openpnp.model.Placement.Type;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.openpnp.spi.PartAlignment;
import org.openpnp.spi.PartAlignment.PartAlignmentOffset;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.UiUtils;
import org.openpnp.util.Utils2D;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.FluentCv;
import org.pmw.tinylog.Logger;

public class JobPlacementsPanel extends JPanel {
    private JTable table;
    private PlacementsTableModel tableModel;
    private TableRowSorter<PlacementsTableModel> tableSorter;
    private ActionGroup boardLocationSelectionActionGroup;
    private ActionGroup singleSelectionActionGroup;
    private ActionGroup multiSelectionActionGroup;
    private ActionGroup captureAndPositionActionGroup;
    private BoardLocation boardLocation;
    private JobPanel jobPanel;

    private static Color typeColorFiducial = new Color(157, 188, 255);
    private static Color typeColorPlacement = new Color(255, 255, 255);
    private static Color statusColorWarning = new Color(252, 255, 157);
    private static Color statusColorReady = new Color(157, 255, 168);
    private static Color statusColorError = new Color(255, 157, 157);
    private static Color statusColorDisabled = new Color(180, 180, 180);

    public JobPlacementsPanel(JobPanel jobPanel) {
    	this.jobPanel = jobPanel;
        createUi();
    }
    private void createUi() {
        setBorder(new TitledBorder(null, "Placements", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        
        Configuration configuration = Configuration.get();
        
        boardLocationSelectionActionGroup = new ActionGroup(newAction);
        boardLocationSelectionActionGroup.setEnabled(false);

        singleSelectionActionGroup = new ActionGroup(removeAction, editPlacementFeederAction,
                setTypeAction, setSideAction, setPlacedAction, setErrorHandlingAction,
                setEnabledAction,
                trainPlacementAction);
        singleSelectionActionGroup.setEnabled(false);

        multiSelectionActionGroup = new ActionGroup(removeAction, setTypeAction, setSideAction,
                setPlacedAction, setErrorHandlingAction, setEnabledAction);
        multiSelectionActionGroup.setEnabled(false);

        captureAndPositionActionGroup = new ActionGroup(captureCameraPlacementLocation,
                captureToolPlacementLocation, moveCameraToPlacementLocation,
                moveCameraToPlacementLocationNext, moveToolToPlacementLocation);
        captureAndPositionActionGroup.setEnabled(false);

        JComboBox<PartsComboBoxModel> partsComboBox = new JComboBox(new PartsComboBoxModel());
        partsComboBox.setRenderer(new IdentifiableListCellRenderer<Part>());
        JComboBox<Side> sidesComboBox = new JComboBox(Side.values());
        // Note we don't use Type.values() here because there are a couple Types that are only
        // there for backwards compatibility and we don't want them in the list.
        JComboBox<Type> typesComboBox = new JComboBox(new Type[] { Type.Placement, Type.Fiducial });
        JComboBox<Type> errorHandlingComboBox = new JComboBox(ErrorHandling.values());
        
                setLayout(new BorderLayout(0, 0));
        tableModel = new PlacementsTableModel(configuration);
        tableSorter = new TableRowSorter<>(tableModel);
        
                table = new AutoSelectTextTable(tableModel);
        table.setRowSorter(tableSorter);
        table.getTableHeader().setDefaultRenderer(new MultisortTableHeaderCellRenderer());
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setDefaultEditor(Side.class, new DefaultCellEditor(sidesComboBox));
        table.setDefaultEditor(Part.class, new DefaultCellEditor(partsComboBox));
        table.setDefaultEditor(Type.class, new DefaultCellEditor(typesComboBox));
        table.setDefaultEditor(ErrorHandling.class, new DefaultCellEditor(errorHandlingComboBox));
        table.setDefaultRenderer(Part.class, new IdentifiableTableCellRenderer<Part>());
        table.setDefaultRenderer(PlacementsTableModel.Status.class, new StatusRenderer());
        table.setDefaultRenderer(Placement.Type.class, new TypeRenderer());
        tableModel.setJobPlacementsPanel(this);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
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
                    captureAndPositionActionGroup.setEnabled(getSelection() != null
                            && getSelection().getSide() == boardLocation.getSide());
                    Configuration.get().getBus().post(new PlacementSelectedEvent(getSelection(),
                            boardLocation, JobPlacementsPanel.this));
                }
            }
        });
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent mouseEvent) {
                if (mouseEvent.getClickCount() != 2) {
                    return;
                }
                int row = table.rowAtPoint(new Point(mouseEvent.getX(), mouseEvent.getY()));
                int col = table.columnAtPoint(new Point(mouseEvent.getX(), mouseEvent.getY()));
                if (tableModel.getColumnClass(col) == Status.class) {
                    Status status = (Status) tableModel.getValueAt(row, col);
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
                    placement.setEnabled(!placement.isEnabled());
                    refreshSelectedRow();
                    updateActivePlacements();
                }
                else {
                    super.keyTyped(e);
                }
            }
        });
        
        JPopupMenu popupMenu = new JPopupMenu();

        JMenu setTypeMenu = new JMenu(setTypeAction);
        setTypeMenu.add(new SetTypeAction(Placement.Type.Placement));
        setTypeMenu.add(new SetTypeAction(Placement.Type.Fiducial));
        popupMenu.add(setTypeMenu);

        JMenu setSideMenu = new JMenu(setSideAction);
        for (Board.Side side : Board.Side.values()) {
            setSideMenu.add(new SetSideAction(side));
        }
        popupMenu.add(setSideMenu);

        JMenu setPlacedMenu = new JMenu(setPlacedAction);
        setPlacedMenu.add(new SetPlacedAction(true));
        setPlacedMenu.add(new SetPlacedAction(false));
        popupMenu.add(setPlacedMenu);

        JMenu setEnabledMenu = new JMenu(setEnabledAction);
        setEnabledMenu.add(new SetEnabledAction(true));
        setEnabledMenu.add(new SetEnabledAction(false));
        popupMenu.add(setEnabledMenu);

        JMenu setErrorHandlingMenu = new JMenu(setErrorHandlingAction);
        setErrorHandlingMenu.add(new SetErrorHandlingAction(ErrorHandling.Alert));
        setErrorHandlingMenu.add(new SetErrorHandlingAction(ErrorHandling.Defer));
        popupMenu.add(setErrorHandlingMenu);

        table.setComponentPopupMenu(popupMenu);

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);
        
        JPanel panel = new JPanel();
        add(panel, BorderLayout.NORTH);
        panel.setLayout(new BorderLayout(0, 0));
        JToolBar toolBarPlacements = new JToolBar();
        panel.add(toolBarPlacements);
        
        toolBarPlacements.setFloatable(false);
        JButton btnNewPlacement = new JButton(newAction);
        btnNewPlacement.setHideActionText(true);
        toolBarPlacements.add(btnNewPlacement);
        JButton btnRemovePlacement = new JButton(removeAction);
        btnRemovePlacement.setHideActionText(true);
        toolBarPlacements.add(btnRemovePlacement);
        toolBarPlacements.addSeparator();
        
        JButton btnPositionCameraPositionLocation = new JButton(moveCameraToPlacementLocation);
        btnPositionCameraPositionLocation.setHideActionText(true);
        toolBarPlacements.add(btnPositionCameraPositionLocation);
        
        JButton btnPositionCameraPositionNextLocation =
                new JButton(moveCameraToPlacementLocationNext);
        btnPositionCameraPositionNextLocation.setHideActionText(true);
        toolBarPlacements.add(btnPositionCameraPositionNextLocation);

        JButton btnPositionToolPositionLocation = new JButton(moveToolToPlacementLocation);
        btnPositionToolPositionLocation.setHideActionText(true);
        toolBarPlacements.add(btnPositionToolPositionLocation);

        toolBarPlacements.addSeparator();

        JButton btnCaptureCameraPlacementLocation = new JButton(captureCameraPlacementLocation);
        btnCaptureCameraPlacementLocation.setHideActionText(true);
        toolBarPlacements.add(btnCaptureCameraPlacementLocation);

        JButton btnCaptureToolPlacementLocation = new JButton(captureToolPlacementLocation);
        btnCaptureToolPlacementLocation.setHideActionText(true);
        toolBarPlacements.add(btnCaptureToolPlacementLocation);

        toolBarPlacements.addSeparator();

        JButton btnEditFeeder = new JButton(editPlacementFeederAction);
        btnEditFeeder.setHideActionText(true);
        toolBarPlacements.add(btnEditFeeder);

        toolBarPlacements.addSeparator();

        JButton btnTrainPlacement = new JButton(trainPlacementAction);
        btnTrainPlacement.setHideActionText(true);
        toolBarPlacements.add(btnTrainPlacement);

        JPanel panel_1 = new JPanel();
        panel.add(panel_1, BorderLayout.EAST);

        JLabel lblNewLabel = new JLabel("Search");
        panel_1.add(lblNewLabel);

        searchTextField = new JTextField();
        searchTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent e) {
                search();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                search();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                search();
            }
        });
        panel_1.add(searchTextField);
        searchTextField.setColumns(15);
    }
    
    private void search() {
        updateRowFilter();
    }
    
    public void refresh() {
        tableModel.fireTableDataChanged();
        updateActivePlacements();
    }

    public void refreshSelectedRow() {
        int index = table.convertRowIndexToModel(table.getSelectedRow());
        tableModel.fireTableRowsUpdated(index, index);
    }

    public void selectPlacement(Placement placement) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (tableModel.getPlacement(i) == placement) {
                int index = table.convertRowIndexToView(i);
                table.getSelectionModel().setSelectionInterval(index, index);
                table.scrollRectToVisible(new Rectangle(table.getCellRect(index, 0, true)));
                break;
            }
        }
    }
    
    // TODO STOPSHIP This is called all over the place and it's likely to rot - need to find
    // a listener or something it can use.
    public void updateActivePlacements() {
        int activePlacements = 0;
        int totalActivePlacements = 0;
        
        List<BoardLocation> boardLocations = this.jobPanel.getJob().getBoardLocations();
        for (BoardLocation boardLocation : boardLocations) {
            if (boardLocation.isEnabled()) {
                activePlacements += boardLocation.getActivePlacements();
                totalActivePlacements += boardLocation.getTotalActivePlacements();
            }
        }
        
        int blTotalActivePlacements = 0;
        int blActivePlacements = 0;
        
        if (boardLocation != null) {
            blTotalActivePlacements = boardLocation.getTotalActivePlacements();
            blActivePlacements = boardLocation.getActivePlacements();
        }
        
        MainFrame.get().setPlacementCompletionStatus(totalActivePlacements - activePlacements, 
                totalActivePlacements, 
                blTotalActivePlacements - blActivePlacements, 
                blTotalActivePlacements);
    }
    
    private void updateRowFilter() {
        List<RowFilter<PlacementsTableModel, Integer>> filters = new ArrayList<>();
        
        RowFilter<PlacementsTableModel, Integer> sideFilter = new RowFilter<PlacementsTableModel, Integer>() {
            public boolean include(Entry<? extends PlacementsTableModel, ? extends Integer> entry) {
                if (boardLocation == null) {
                    return false;
                }
                PlacementsTableModel model = entry.getModel();
                Placement placement = model.getPlacement(entry.getIdentifier());
                return placement.getSide() == boardLocation.getSide();
            }
        };
        filters.add(sideFilter);
        
        try {
            RowFilter<PlacementsTableModel, Integer> searchFilter = RowFilter.regexFilter("(?i)" + searchTextField.getText().trim());
            filters.add(searchFilter);
        }
        catch (PatternSyntaxException e) {
        }
        
        tableSorter.setRowFilter(RowFilter.andFilter(filters));
    }
    
    
    public void setBoardLocation(BoardLocation boardLocation) {
        this.boardLocation = boardLocation;
        if (boardLocation == null) {
            tableModel.setBoardLocation(null);
            boardLocationSelectionActionGroup.setEnabled(false);
        }
        else {
            tableModel.setBoardLocation(boardLocation);
            boardLocationSelectionActionGroup.setEnabled(true);

            updateRowFilter();
        }
        updateActivePlacements();
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
            putValue(SHORT_DESCRIPTION, "Create a new placement and add it to the board.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (Configuration.get().getParts().size() == 0) {
                MessageBoxes.errorBox(getTopLevelAncestor(), "Error",
                        "There are currently no parts defined in the system. Please create at least one part before creating a placement.");
                return;
            }

            String id = JOptionPane.showInputDialog(getTopLevelAncestor(),
                    "Please enter an ID for the new placement.");
            if (id == null) {
                return;
            }
            
            // Check if the new placement ID is unique
            for(Placement compareplacement : boardLocation.getBoard().getPlacements()) {
            	if (compareplacement.getId().equals(id)) {
            		MessageBoxes.errorBox(getTopLevelAncestor(), "Error",
                            "The ID for the new placement already exists");
                    return;
            	}
            }
            
            Placement placement = new Placement(id);

            placement.setPart(Configuration.get().getParts().get(0));
            placement.setLocation(new Location(Configuration.get().getSystemUnits()));
            placement.setSide(boardLocation.getSide());

            boardLocation.getBoard().addPlacement(placement);
            tableModel.fireTableDataChanged();
            updateActivePlacements();
            boardLocation.setPlaced(placement.getId(), false);
            Helpers.selectLastTableRow(table);
        }
    };

    public final Action removeAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.delete);
            putValue(NAME, "Remove Placement(s)");
            putValue(SHORT_DESCRIPTION, "Remove the currently selected placement(s).");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            for (Placement placement : getSelections()) {
                boardLocation.getBoard().removePlacement(placement);
            }
            tableModel.fireTableDataChanged();
            updateActivePlacements();
        }
    };

    public final Action moveCameraToPlacementLocation = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.centerCamera);
            putValue(NAME, "Move Camera To Placement Location");
            putValue(SHORT_DESCRIPTION, "Position the camera at the placement's location.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                Location location = Utils2D.calculateBoardPlacementLocation(boardLocation,
                        getSelection().getLocation());
                System.out.println(boardLocation);
                System.out.println(getSelection());
                System.out.println(location);

                Camera camera = MainFrame.get().getMachineControls().getSelectedTool().getHead()
                        .getDefaultCamera();
                MovableUtils.moveToLocationAtSafeZ(camera, location);
                try {
                    Map<String, Object> globals = new HashMap<>();
                    globals.put("camera", camera);
                    Configuration.get().getScripting().on("Camera.AfterPosition", globals);
                }
                catch (Exception e) {
                    Logger.warn(e);
                }
            });
        }
    };
    public final Action moveCameraToPlacementLocationNext = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.centerCameraMoveNext);
            putValue(NAME, "Move Camera To Next Placement Location ");
            putValue(SHORT_DESCRIPTION,
                    "Position the camera at the next placements location.");
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
               	Location location = Utils2D.calculateBoardPlacementLocation(boardLocation,
                        getSelection().getLocation());
                Camera camera = MainFrame.get().getMachineControls().getSelectedTool().getHead()
                        .getDefaultCamera();
                MovableUtils.moveToLocationAtSafeZ(camera, location);
                
                try {
                    Map<String, Object> globals = new HashMap<>();
                    globals.put("camera", camera);
                    Configuration.get().getScripting().on("Camera.AfterPosition", globals);
                }
                catch (Exception e) {
                    Logger.warn(e);
                }
            });
        };
    };

    public final Action moveToolToPlacementLocation = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.centerTool);
            putValue(NAME, "Move Tool To Placement Location");
            putValue(SHORT_DESCRIPTION, "Position the tool at the placement's location.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            Location location = Utils2D.calculateBoardPlacementLocation(boardLocation,
                    getSelection().getLocation());

            Nozzle nozzle = MainFrame.get().getMachineControls().getSelectedNozzle();
            UiUtils.submitUiMachineTask(() -> {
                MovableUtils.moveToLocationAtSafeZ(nozzle, location);
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
                HeadMountable tool = MainFrame.get().getMachineControls().getSelectedTool();
                Camera camera = tool.getHead().getDefaultCamera();
                Location placementLocation = Utils2D.calculateBoardPlacementLocationInverse(
                        boardLocation, camera.getLocation());
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
            Nozzle nozzle = MainFrame.get().getMachineControls().getSelectedNozzle();
            Location placementLocation = Utils2D
                    .calculateBoardPlacementLocationInverse(boardLocation, nozzle.getLocation());
            getSelection().setLocation(placementLocation);
            table.repaint();
        }
    };

    public final Action editPlacementFeederAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.editFeeder);
            putValue(NAME, "Edit Placement Feeder");
            putValue(SHORT_DESCRIPTION, "Edit the placement's associated feeder definition.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            Placement placement = getSelection();
            MainFrame.get().getFeedersTab().showFeederForPart(placement.getPart());
        }
    };

    public final Action trainPlacementAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.partAlign);
            putValue(NAME, "Train Placement");
            putValue(SHORT_DESCRIPTION, "Use a bottom vision overlay to train the placement.");
        }
        
        Nozzle findNozzle(Part part) throws Exception {
            org.openpnp.model.Package packag = part.getPackage();
            
            for (Nozzle nozzle : Configuration.get().getMachine().getDefaultHead().getNozzles()) {
                if (nozzle.getNozzleTip() == null) {
                    continue;
                }
                if (packag.getCompatibleNozzleTips().contains(nozzle.getNozzleTip())) {
                    return nozzle;
                }
            }
            
            for (Nozzle nozzle : Configuration.get().getMachine().getDefaultHead().getNozzles()) {
                for (NozzleTip nozzleTip : nozzle.getCompatibleNozzleTips()) {
                    if (packag.getCompatibleNozzleTips().contains(nozzleTip)) {
                        nozzle.loadNozzleTip(nozzleTip);
                        return nozzle;
                    }
                }
            }
            
            throw new Exception("No compatible nozzle and nozzle tip found for " + part.getName());
        }
        
        Feeder findFeeder(Part part) throws Exception {
            for (Feeder feeder : Configuration.get().getMachine().getFeeders()) {
                if (!feeder.isEnabled()) {
                    continue;
                }
                if (feeder.getPart() == part) {
                    return feeder;
                }
            }
            throw new Exception("No enabled feeder found for " + part.getName());
        }
        
        void pick(Part part, Feeder feeder, Nozzle nozzle) throws Exception {
            feeder.feed(nozzle);
            Location pickLocation = feeder.getPickLocation();
            MovableUtils.moveToLocationAtSafeZ(nozzle, pickLocation);
            nozzle.pick(part);
            nozzle.moveToSafeZ();
            if (nozzle.getPart() != part) {
                throw new Exception("Picked part does not match expected part. How?");
            }
        }
        
        PartAlignmentOffset align(Nozzle nozzle) throws Exception {
            ((SimulatedUpCamera) VisionUtils.getBottomVisionCamera())
                .setErrorOffsets(
                        new Location(LengthUnit.Millimeters, Math.random(), Math.random(), 0, Math.random() * 10));
            
            for (PartAlignment alignment : Configuration.get().getMachine().getPartAlignments()) {
                if (!alignment.canHandle(nozzle.getPart())) {
                    continue;
                }
                return VisionUtils.findPartAlignmentOffsets(
                        alignment, nozzle.getPart(), null, new Location(LengthUnit.Millimeters), nozzle);
            }

            ((SimulatedUpCamera) VisionUtils.getBottomVisionCamera())
            .setErrorOffsets(
                    new Location(LengthUnit.Millimeters));
            
            throw new Exception("No compatible part alignment found for " + nozzle.getPart().getName());
        }
        
        Location transformCameraLocation(Part part, Location cameraLocation,
                PartAlignmentOffset alignmentOffsets) {
            if (alignmentOffsets.getPreRotated()) {
                cameraLocation =
                        cameraLocation.subtractWithRotation(alignmentOffsets.getLocation());
            }
            else {
                Location alignmentOffsetsLocation = alignmentOffsets.getLocation();
                Location location = new Location(LengthUnit.Millimeters).rotateXyCenterPoint(
                        alignmentOffsetsLocation,
                        cameraLocation.getRotation() - alignmentOffsetsLocation.getRotation());
                location = location.derive(null, null, null,
                        cameraLocation.getRotation() - alignmentOffsetsLocation.getRotation());
                location = location.add(cameraLocation);
                location = location.subtract(alignmentOffsetsLocation);
                cameraLocation = location;
            }
            cameraLocation = cameraLocation.add(
                    new Location(part.getHeight().getUnits(), 0, 0, part.getHeight().getValue(), 0));

            return cameraLocation;
        }

        BufferedImage captureImage(Nozzle nozzle, PartAlignmentOffset offsets) throws Exception {
            ((SimulatedUpCamera) VisionUtils.getBottomVisionCamera())
                .setDrawNozzle(false);
            Camera camera = VisionUtils.getBottomVisionCamera();
            Location location = camera.getLocation();
            location = transformCameraLocation(nozzle.getPart(), location, offsets);
            MovableUtils.moveToLocationAtSafeZ(nozzle, location);
            BufferedImage image = camera.settleAndCapture();
            ((SimulatedUpCamera) VisionUtils.getBottomVisionCamera())
                .setDrawNozzle(true);
            return image;
        }
        
        void discard(Nozzle nozzle) throws Exception {
            Location discardLocation = Configuration.get().getMachine().getDiscardLocation();
            MovableUtils.moveToLocationAtSafeZ(nozzle, discardLocation);
            nozzle.place();
            nozzle.moveToSafeZ();
        }
        
        BufferedImage filterImage(BufferedImage image) {
            // Convert to gray and threshold using Otsu's method
            Mat mat = new FluentCv().toMat(image).toGray().threshold(0).mat();
            
            // Convert to RGBA so we have an alpha channel
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_GRAY2BGRA);
            
            // Turn all black pixels transparent and change channel order to match
            // the BufferedImage we'll create below.
            byte[] pixel = new byte[4];
            for (int y = 0; y < mat.cols(); y++) {
                for (int x = 0; x < mat.rows(); x++) {
                    mat.get(y, x, pixel);
                    if (pixel[0] == 0 && pixel[1] == 0 && pixel[2] == 0) {
                        pixel[3] = 0;
                    }
                    byte b = pixel[0];
                    byte g = pixel[1];
                    byte r = pixel[2];
                    byte a = pixel[3];
                    pixel[0] = a;
                    pixel[1] = b;
                    pixel[2] = g;
                    pixel[3] = r;
                    mat.put(y, x, pixel);
                }
            }

            // Convert the Mat back to BufferedImage
            image = new BufferedImage(mat.cols(), mat.rows(), BufferedImage.TYPE_4BYTE_ABGR);
            mat.get(0, 0, ((DataBufferByte) image.getRaster().getDataBuffer()).getData());
            mat.release();
            
            return image;
        }
        
        Camera moveCameraToPlacement(BoardLocation boardLocation, Placement placement) throws Exception {
            Part part = placement.getPart();
            Location placementLocation =
                    Utils2D.calculateBoardPlacementLocation(boardLocation, placement.getLocation());
            placementLocation = placementLocation.add(new Location(part.getHeight().getUnits(), 0,
                    0, part.getHeight().getValue(), 0));
            Camera camera = Configuration.get().getMachine().getDefaultHead().getDefaultCamera();
            MovableUtils.moveToLocationAtSafeZ(camera, placementLocation);
            return camera;
        }
        
        void render(Camera camera, BufferedImage image) {
            try {
                CameraView cameraView = MainFrame.get().getCameraViews().getCameraView(camera);
                cameraView.setCameraViewFilter(new CameraViewFilter() {
                    @Override
                    public BufferedImage filterCameraImage(Camera camera, BufferedImage cameraImage) {
                        BufferedImage overlay = image;
                        AffineTransform tx;
                        AffineTransformOp op;
                        // TODO STOPSHIP something wrong with rotation when angles are not multiples
                        // of 90*.
                        // Rotate the image about it's center, to the camera's rotation
                        tx = AffineTransform.getRotateInstance(Math.toRadians(-camera.getLocation().getRotation()),
                                overlay.getWidth() / 2, 
                                overlay.getHeight() / 2);
                        op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
                        overlay = op.filter(overlay, null);
                        
                        // Scale the overlay to the camera units per pixel
                        double scaleX = 1, scaleY = 1;
                        try {
                            Location bottomUpp = VisionUtils.getBottomVisionCamera().getUnitsPerPixel().convertToUnits(LengthUnit.Millimeters);
                            Location topUpp = camera.getUnitsPerPixel().convertToUnits(LengthUnit.Millimeters);
                            scaleX = bottomUpp.getX() / topUpp.getX();
                            scaleY = bottomUpp.getY() / topUpp.getY();
                        }
                        catch (Exception e) {
                            
                        }
                        tx = AffineTransform.getScaleInstance(scaleX, scaleY);
                        op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
                        overlay = op.filter(overlay, null);
                        
                        // Create the image that will be used as the final image
                        BufferedImage target = new BufferedImage(cameraImage.getWidth(), cameraImage.getHeight(), cameraImage.getType());
                        Graphics2D g = (Graphics2D) target.getGraphics();
                        
                        // Draw the camera image first
                        g.drawImage(cameraImage, 0, 0, null);

                        // And draw the overlay, centered
                        g.drawImage(overlay, 
                                target.getWidth() / 2 - overlay.getWidth() / 2, 
                                target.getHeight() / 2 - overlay.getHeight() / 2, 
                                null);                
                        g.setColor(Color.red);
                        g.drawRect(target.getWidth() / 2 - overlay.getWidth() / 2,
                                target.getHeight() / 2 - overlay.getHeight() / 2,
                                overlay.getWidth(), 
                                overlay.getHeight());
                        g.dispose();
                        return target;
                    }
                });
            }
            catch (Exception e) {
                // Throw away, just means we're running outside of the UI.
            }
        }
        
        @Override
        public void actionPerformed(ActionEvent arg0) {
            Placement placement = getSelection();
            Part part = placement.getPart();
            UiUtils.submitUiMachineTask(() -> {
                /**
                 * Find a nozzle that can pick the part
                 * Find a feeder that can feeder the part
                 * Pick the part
                 * Align the part
                 * Position at bottom vision (using alignment offsets)
                 * Settle and capture image
                 * Discard or replace part
                 * Move camera to placement
                 * Render image over placement center at 20% opaque
                 */
                
                Nozzle nozzle = findNozzle(part);
                Feeder feeder = findFeeder(part);
                pick(part, feeder, nozzle);
                PartAlignmentOffset offsets = align(nozzle);
                BufferedImage image = captureImage(nozzle, offsets);
                discard(nozzle);
                image = filterImage(image);
                Camera camera = moveCameraToPlacement(boardLocation, placement);
                render(camera, image);
            });
        }
    };

    public final Action setTypeAction = new AbstractAction() {
        {
            putValue(NAME, "Set Type");
            putValue(SHORT_DESCRIPTION, "Set placement type(s) to...");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {}
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
                tableModel.fireTableDataChanged();
                updateActivePlacements();
            }
        }
    };

    public final Action setSideAction = new AbstractAction() {
        {
            putValue(NAME, "Set Side");
            putValue(SHORT_DESCRIPTION, "Set placement side(s) to...");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {}
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
                tableModel.fireTableDataChanged();
                updateActivePlacements();
            }
        }
    };
    
    public final Action setErrorHandlingAction = new AbstractAction() {
        {
            putValue(NAME, "Set Error Handling");
            putValue(SHORT_DESCRIPTION, "Set placement error handling(s) to...");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {}
    };

    class SetErrorHandlingAction extends AbstractAction {
        Placement.ErrorHandling errorHandling;

        public SetErrorHandlingAction(Placement.ErrorHandling errorHandling) {
            this.errorHandling = errorHandling;
            putValue(NAME, errorHandling.toString());
            putValue(SHORT_DESCRIPTION, "Set placement error handling(s) to " + errorHandling.toString());
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            for (Placement placement : getSelections()) {
                placement.setErrorHandling(errorHandling);
                tableModel.fireTableDataChanged();
                updateActivePlacements();
            }
        }
    };
    
    public final Action setPlacedAction = new AbstractAction() {
        {
            putValue(NAME, "Set Placed");
            putValue(SHORT_DESCRIPTION, "Set placed to...");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {}
    };

    class SetPlacedAction extends AbstractAction {
        final Boolean placed;

        public SetPlacedAction(Boolean placed) {
            this.placed = placed;
            String name = placed ? "Placed" : "Not Placed";
            putValue(NAME, name);
            putValue(SHORT_DESCRIPTION, "Set placed to " + name);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            for (Placement placement : getSelections()) {
                boardLocation.setPlaced(placement.getId(), placed);
                tableModel.fireTableDataChanged();   
                updateActivePlacements();
            }
        }
    };

    public final Action setEnabledAction = new AbstractAction() {
        {
            putValue(NAME, "Set Enabled");
            putValue(SHORT_DESCRIPTION, "Set placement enabled to...");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {}
    };
    private JTextField searchTextField;

    class SetEnabledAction extends AbstractAction {
        final Boolean enabled;

        public SetEnabledAction(Boolean enabled) {
            this.enabled = enabled;
            String name = enabled ? "Enabled" : "Disabled";
            putValue(NAME, name);
            putValue(SHORT_DESCRIPTION, "Set placement enabled to " + name);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            for (Placement placement : getSelections()) {
                placement.setEnabled(enabled);
                tableModel.fireTableDataChanged();   
                updateActivePlacements();
            }
        }
    };

    static class TypeRenderer extends DefaultTableCellRenderer {
        public void setValue(Object value) {
            if (value == null) {
                return;
            }
            Type type = (Type) value;
            setText(type.name());
            if (type == Type.Fiducial) {
                setBorder(new LineBorder(getBackground()));
                setForeground(Color.black);
                setBackground(typeColorFiducial);
            }
            else if (type == Type.Placement) {
                setBorder(new LineBorder(getBackground()));
                setForeground(Color.black);
                setBackground(typeColorPlacement);
            }
        }
    }

    static class StatusRenderer extends DefaultTableCellRenderer {
        public void setValue(Object value) {
            if (value == null) {
                return;
            }
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
            else if (status == Status.Disabled) {
                setBorder(new LineBorder(getBackground()));
                setForeground(Color.black);
                setBackground(statusColorDisabled);
                setText("Disabled");
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
