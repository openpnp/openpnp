package org.openpnp.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;

import org.openpnp.gui.components.AutoSelectTextTable;
import org.openpnp.gui.components.ClassSelectionDialog;
import org.openpnp.gui.support.ActionGroup;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.tablemodel.PadsTableModel;
import org.openpnp.model.Board.Side;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.BoardPad;
import org.openpnp.model.BoardPad.Type;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.model.Pad;
import org.openpnp.spi.Camera;
import org.openpnp.spi.PasteDispenser;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.Utils2D;

public class JobPastePanel extends JPanel {
    private JTable table;
    private PadsTableModel tableModel;
    private ActionGroup boardLocationSelectionActionGroup;
    private ActionGroup singleSelectionActionGroup;
    private ActionGroup multiSelectionActionGroup;
    private ActionGroup captureAndPositionActionGroup;
    private BoardLocation boardLocation;

    public JobPastePanel(JobPanel jobPanel) {
        Configuration configuration = Configuration.get();
        
        boardLocationSelectionActionGroup = new ActionGroup(newAction);
        boardLocationSelectionActionGroup.setEnabled(false);

        singleSelectionActionGroup = new ActionGroup(removeAction, setTypeAction);
        singleSelectionActionGroup.setEnabled(false);

        multiSelectionActionGroup = new ActionGroup(removeAction, setTypeAction);
        multiSelectionActionGroup.setEnabled(false);

        captureAndPositionActionGroup = new ActionGroup(moveCameraToPadLocation, moveToolToPadLocation);
        captureAndPositionActionGroup.setEnabled(false);

        JComboBox<Side> sidesComboBox = new JComboBox(Side.values());
        JComboBox<Type> typesComboBox = new JComboBox(Type.values());

        setLayout(new BorderLayout(0, 0));
        JToolBar toolBar = new JToolBar();
        add(toolBar, BorderLayout.NORTH);

        toolBar.setFloatable(false);
        JButton btnNewPad = new JButton(newAction);
        btnNewPad.setHideActionText(true);
        toolBar.add(btnNewPad);
        JButton btnRemovePad = new JButton(removeAction);
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
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        table.setDefaultEditor(Side.class, new DefaultCellEditor(
                sidesComboBox));

        table.setDefaultRenderer(Type.class,
                new TypeRenderer());
        table.setDefaultEditor(Type.class, new DefaultCellEditor(
                typesComboBox));
        
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
        
        JPopupMenu popupMenu = new JPopupMenu();
        
        JMenu setTypeMenu = new JMenu(setTypeAction);
        setTypeMenu.add(new SetTypeAction(BoardPad.Type.Paste));
        setTypeMenu.add(new SetTypeAction(BoardPad.Type.Ignore));
        popupMenu.add(setTypeMenu);

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

    public BoardPad getSelection() {
        List<BoardPad> selectedPads = getSelections();
        if (selectedPads.isEmpty()) {
            return null;
        }
        return selectedPads.get(0);
    }
    
    public List<BoardPad> getSelections() {
        ArrayList<BoardPad> rows = new ArrayList<BoardPad>();
        if (boardLocation == null) {
            return rows;
        }
        int[] selectedRows = table.getSelectedRows();
        for (int selectedRow : selectedRows) {
            selectedRow = table.convertRowIndexToModel(selectedRow);
            rows.add(boardLocation.getBoard().getSolderPastePads().get(selectedRow));
        }
        return rows;
    }

    public final Action newAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.add);
            putValue(NAME, "New Pad");
            putValue(SHORT_DESCRIPTION,
                    "Create a new pad and add it to the board.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            List<Class<? extends Pad>> padClasses = new ArrayList<>();
            padClasses.add(Pad.RoundRectangle.class);
            padClasses.add(Pad.Circle.class);
            padClasses.add(Pad.Ellipse.class);
            // See note on Pad.Line
//            padClasses.add(Pad.Line.class);
            ClassSelectionDialog<Pad> dialog = new ClassSelectionDialog<Pad>(
                    JOptionPane.getFrameForComponent(JobPastePanel.this),
                    "Select Pad...",
                    "Please select a pad type from the list below.",
                    padClasses);
            dialog.setVisible(true);
            Class<? extends Pad> padClass = dialog.getSelectedClass();
            if (padClass == null) {
                return;
            }
            try {
                Pad pad = padClass.newInstance();
                BoardPad boardPad = new BoardPad();
                boardPad.setLocation(new Location(Configuration.get()
                        .getSystemUnits()));
                boardPad.setPad(pad);

                boardLocation.getBoard().addSolderPastePad(boardPad);
                tableModel.fireTableDataChanged();
                Helpers.selectLastTableRow(table);
            }
            catch (Exception e) {
                MessageBoxes.errorBox(
                        JOptionPane.getFrameForComponent(JobPastePanel.this),
                        "Pad Error", e);
            }
        }
    };

    public final Action removeAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.delete);
            putValue(NAME, "Remove Pad");
            putValue(SHORT_DESCRIPTION,
                    "Remove the currently selected pad.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            for (BoardPad pad : getSelections()) {
                boardLocation.getBoard().removeSolderPastePad(pad);
            }
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
                            .getSide(), getSelection().getLocation());

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
            Location padLocation = Utils2D
                    .calculateBoardPlacementLocation(boardLocation
                            .getLocation(), boardLocation
                            .getSide(), getSelection().getLocation());

            final PasteDispenser dispenser = MainFrame.machineControlsPanel.getSelectedPasteDispenser();
            final Location location = padLocation;
            MainFrame.machineControlsPanel.submitMachineTask(new Runnable() {
                public void run() {
                    try {
                        MovableUtils.moveToLocationAtSafeZ(dispenser, location,
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
    
    public final Action setTypeAction = new AbstractAction() {
        {
            putValue(NAME, "Set Type");
            putValue(SHORT_DESCRIPTION,
                    "Set pad type(s) to...");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
        }
    };
    
    class SetTypeAction extends AbstractAction {
        final BoardPad.Type type;
        
        public SetTypeAction(BoardPad.Type type) {
            this.type = type;
            putValue(NAME, type.toString());
            putValue(SHORT_DESCRIPTION, "Set pad type(s) to " + type.toString());
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            for (BoardPad pad : getSelections()) {
                pad.setType(type);
            }
        }
    };
    
    static class TypeRenderer extends DefaultTableCellRenderer {
        public void setValue(Object value) {
            Type type = (Type) value;
            setText(type.name());
            if (type == Type.Paste) {
                setBackground(Color.cyan);
            }
            else if (type == Type.Ignore) {
                setBackground(Color.yellow);
            }
        }
    }
}
