package org.openpnp.gui.panelization;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.table.TableRowSorter;

import org.openpnp.gui.MultisortTableHeaderCellRenderer;
import org.openpnp.gui.components.AutoSelectTextTable;
import org.openpnp.gui.tablemodel.FiducialLocatablePlacementsTableModel;
import org.openpnp.model.AbstractLocatable;
import org.openpnp.model.FiducialLocatableLocation;
import org.openpnp.model.PanelLocation;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;

import javax.swing.UIManager;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.openpnp.gui.support.PartsComboBoxModel;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.openpnp.model.Board.Side;
import org.openpnp.gui.support.IdentifiableListCellRenderer;
import org.openpnp.gui.support.IdentifiableTableCellRenderer;
import org.openpnp.util.Utils2D;
import org.pmw.tinylog.Logger;

@SuppressWarnings("serial")
public class DlgChildFiducialSelector extends JDialog {

    private JTable childFiducialsTable;
    private FiducialLocatablePlacementsTableModel tableModel;
    private PanelLocation panelLocation;
    private FiducialLocatableLocation child;
    private JRadioButton childFrame;
    private JRadioButton parentFrame;

    /**
     * Create the dialog.
     */
    public DlgChildFiducialSelector(PanelLocation panelLocation, FiducialLocatableLocation child) {
        this.panelLocation = panelLocation;
        this.child = child;
        this.setTitle("Create Copy of Child Fiducial(s) To Use As Panel Fiducial(s)");
        setModalityType(ModalityType.APPLICATION_MODAL);
        setBounds(100, 100, 600, 400);
        getContentPane().setLayout(new BorderLayout());
        {
            JPanel instructionPanel = new JPanel();
            instructionPanel.setLayout(new BorderLayout());
            getContentPane().add(instructionPanel, BorderLayout.NORTH);
            
            {
                JTextArea txtrSelectOneOr = new JTextArea();
                txtrSelectOneOr.setWrapStyleWord(true);
                txtrSelectOneOr.setLineWrap(true);
                txtrSelectOneOr.setBackground(UIManager.getColor("Label.background"));
                txtrSelectOneOr.setFont(UIManager.getFont("Label.font"));
                txtrSelectOneOr.setText(
                        "Select one or more child fiducials from the list below to act as panel "
                        + "fiducials. Hold down the Control or Shift keys to select multiple items."
                        + "  IMPORTANT - Once the fiducials are added to the panel, changes to the "
                        + "child's position or orientation on the panel will NOT automatically "
                        + "update the panel's fiducials.");
                txtrSelectOneOr.setEditable(false);
                instructionPanel.add(txtrSelectOneOr, BorderLayout.NORTH);
            }
            
            {
                JPanel radioPanel = new JPanel();
                radioPanel.setLayout(new FlowLayout());
                instructionPanel.add(radioPanel, BorderLayout.SOUTH);
                {
                    JLabel lblNewLabel = new JLabel("Display Side and Coordinates relative to: ");
                    radioPanel.add(lblNewLabel);
                }
                {
                    parentFrame = new JRadioButton("Panel");
                    parentFrame.setSelected(true);
                    parentFrame.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            tableModel.setLocalReferenceFrame(false);
                        }});
                    radioPanel.add(parentFrame);
                }
                {
                    childFrame = new JRadioButton("Child");
                    childFrame.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            tableModel.setLocalReferenceFrame(true);
                        }});
                    radioPanel.add(childFrame);
                }
                ButtonGroup buttonGroup = new ButtonGroup();
                buttonGroup.add(childFrame);
                buttonGroup.add(parentFrame);
            }
        }
        {
            @SuppressWarnings("unchecked")
            JComboBox<PartsComboBoxModel> partsComboBox = new JComboBox<>(new PartsComboBoxModel());
            partsComboBox.setMaximumRowCount(20);
            partsComboBox.setRenderer(new IdentifiableListCellRenderer<Part>());
            
            tableModel = new FiducialLocatablePlacementsTableModel() {
                @Override
                public boolean isCellEditable(int rowIndex, int columnIndex) {
                    return false; //Shouldn't be able to edit anything from this dialog
                }

            };
            tableModel.setFiducialLocatable(child.getFiducialLocatable());
            tableModel.setParentLocation(child);
            tableModel.setLocalReferenceFrame(false);
            childFiducialsTable = new AutoSelectTextTable(tableModel);
            
            //Filter out all rows except for the fiducials
            RowFilter<Object, Object> rowFilter = new RowFilter<Object, Object>() {
                public boolean include(Entry<? extends Object, ? extends Object> entry) {
                    return (Placement.Type) entry.getValue(7) == Placement.Type.Fiducial;
                }};
            childFiducialsTable.setAutoCreateRowSorter(true);
            ((TableRowSorter<? extends TableModel>) childFiducialsTable.getRowSorter()).setRowFilter(rowFilter);

            //No need to see the Enabled, Error Handling, or Comments columns so remove them
            TableColumnModel tcm = childFiducialsTable.getColumnModel();
            tcm.removeColumn(tcm.getColumn(9)); //skip Comments column
            tcm.removeColumn(tcm.getColumn(8)); //skip Error Handling column
            tcm.removeColumn(tcm.getColumn(0)); //skip Enabled column

            childFiducialsTable.getTableHeader().setDefaultRenderer(new MultisortTableHeaderCellRenderer());
            childFiducialsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            childFiducialsTable.setDefaultRenderer(Part.class, new IdentifiableTableCellRenderer<Part>());
            JScrollPane scrollPane = new JScrollPane(childFiducialsTable);
            getContentPane().add(scrollPane, BorderLayout.CENTER);
        }
        {
            JPanel buttonPane = new JPanel();
            buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
            getContentPane().add(buttonPane, BorderLayout.SOUTH);
            {
                JButton okButton = new JButton("OK");
                okButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        addSelectedFiducials();
                    }});
                okButton.setActionCommand("OK");
                buttonPane.add(okButton);
                getRootPane().setDefaultButton(okButton);
            }
            {
                JButton cancelButton = new JButton("Cancel");
                cancelButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        close();
                    }});
                cancelButton.setActionCommand("Cancel");
                buttonPane.add(cancelButton);
            }
        }
    }

    
    public List<Placement> getSelections() {
        ArrayList<Placement> selections = new ArrayList<>();
        int[] selectedRows = childFiducialsTable.getSelectedRows();
        for (int selectedRow : selectedRows) {
            selectedRow = childFiducialsTable.convertRowIndexToModel(selectedRow);
            selections.add(child.getFiducialLocatable().getPlacements().get(selectedRow));
        }
        return selections;
    }

    public void addSelectedFiducials() {
        Side childGlobalSide = child.getSide();
        for (Placement fiducial : getSelections()) {
            Logger.trace("Copying fiducial = " + fiducial);
            Placement newFiducial = new Placement(panelLocation.getPanel().getPlacements().createId(fiducial.getId() + "-"));
            newFiducial.setType(Placement.Type.Fiducial);
            newFiducial.setEnabled(true);
            newFiducial.setPart(fiducial.getPart());
            newFiducial.setLocation(Utils2D.calculateBoardPlacementLocation(child, fiducial));
            newFiducial.setSide(fiducial.getSide().flip(childGlobalSide == Side.Bottom));
            newFiducial.setDefinedBy(fiducial.getDefinedBy());
            ((AbstractLocatable) fiducial.getDefinedBy()).addPropertyChangeListener(newFiducial);
            Logger.trace("newFiducial = " + newFiducial);
            panelLocation.getPanel().addPlacement(newFiducial);
        }
        close();
    }
    
    public void close() {
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }
}
