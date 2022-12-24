/*
 * Copyright (C) 2022 Jason von Nieda <jason@vonnieda.org>, Tony Luken <tonyluken62+openpnp@gmail.com>
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

package org.openpnp.gui.panelization;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.table.TableColumnModel;

import org.openpnp.Translations;
import org.openpnp.gui.MultisortTableHeaderCellRenderer;
import org.openpnp.gui.components.AutoSelectTextTable;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.IdentifiableTableCellRenderer;
import org.openpnp.gui.tablemodel.PlacementsHolderPlacementsTableModel;
import org.openpnp.model.Abstract2DLocatable.Side;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.PanelLocation;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.openpnp.model.PlacementsHolderLocation;
import org.openpnp.model.Point;
import org.openpnp.util.Collect;
import org.openpnp.util.QuickHull;
import org.openpnp.util.Utils2D;

@SuppressWarnings("serial")
public class ChildFiducialSelectorDialog extends JDialog {

    private enum PlacementTypes {FiducialsOnly, PlacementsOnly, Both};
        
    private PlacementTypes placementTypes = PlacementTypes.FiducialsOnly;
    private JTable childFiducialsTable;
    private PlacementsHolderPlacementsTableModel tableModel;
    private PanelLocation panelLocation;
    private JRadioButton placementsButton;
    private JRadioButton fiducialsButton;
    private JRadioButton bothButton;
    private List<Placement> allPseudoPlacements;
    private List<Placement> filteredPseudoPlacements;
    private List<Placement> filteredPseudoPlacementsHull;
    private List<Placement> goodPseudoPlacements;
    protected boolean showHullOnly = true;

    /**
     * Create the dialog.
     */
    public ChildFiducialSelectorDialog(PanelLocation panelLocation) {
        this.panelLocation = panelLocation;
        
        allPseudoPlacements = new ArrayList<>();
        generateAllPseudoPlacementsList(panelLocation);

        this.setTitle(panelLocation.getPanel().getName() + 
                Translations.getString("ChildFiducialSelectorDialog.Frame.Title")); //$NON-NLS-1$
        setModalityType(ModalityType.APPLICATION_MODAL);
        setBounds(100, 100, 800, 600);
        getContentPane().setLayout(new BorderLayout());
        {
            JPanel panel = new JPanel();
            panel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
            getContentPane().add(panel, BorderLayout.CENTER);
            panel.setLayout(new BorderLayout(0, 0));
            JPanel instructionPanel = new JPanel();
            panel.add(instructionPanel, BorderLayout.NORTH);
            instructionPanel.setLayout(new BorderLayout());
            
            {
                JTextArea txtrSelectOneOr = new JTextArea();
                txtrSelectOneOr.setWrapStyleWord(true);
                txtrSelectOneOr.setLineWrap(true);
                txtrSelectOneOr.setBackground(UIManager.getColor("Label.background")); //$NON-NLS-1$
                txtrSelectOneOr.setFont(UIManager.getFont("Label.font")); //$NON-NLS-1$
                txtrSelectOneOr.setText(
                        Translations.getString("ChildFiducialSelectorDialog.TextArea.Instructions")); //$NON-NLS-1$
                txtrSelectOneOr.setEditable(false);
                instructionPanel.add(txtrSelectOneOr, BorderLayout.NORTH);
            }
            JPanel radioPanel = new JPanel();
            radioPanel.setLayout(new FlowLayout());
            instructionPanel.add(radioPanel, BorderLayout.SOUTH);
            {
                JLabel lblNewLabel = new JLabel(
                        Translations.getString("ChildFiducialSelectorDialog.Label.Show")); //$NON-NLS-1$
                radioPanel.add(lblNewLabel);
            }
            {
                JCheckBox chckbxHullOnly = new JCheckBox(
                        Translations.getString("ChildFiducialSelectorDialog.CheckBox.HullOnly")); //$NON-NLS-1$
                chckbxHullOnly.setSelected(true);
                chckbxHullOnly.setToolTipText(
                        Translations.getString("ChildFiducialSelectorDialog.CheckBox.HullOnly.ToolTip")); //$NON-NLS-1$
                chckbxHullOnly.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        showHullOnly  = chckbxHullOnly.isSelected();
                        updateTable();
                    }
                });
                radioPanel.add(chckbxHullOnly);
            }
            {
                fiducialsButton = new JRadioButton(
                        Translations.getString("ChildFiducialSelectorDialog.RadioButton.Fiducials")); //$NON-NLS-1$
                fiducialsButton.setSelected(true);
                fiducialsButton.setToolTipText(
                        Translations.getString("ChildFiducialSelectorDialog.RadioButton.Fiducials.ToolTip")); //$NON-NLS-1$
                fiducialsButton.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        placementTypes = PlacementTypes.FiducialsOnly;
                        updateTable();
                    }
                });
                radioPanel.add(fiducialsButton);
            }
            {
                placementsButton = new JRadioButton(
                        Translations.getString("ChildFiducialSelectorDialog.RadioButton.Placements")); //$NON-NLS-1$
                placementsButton.setToolTipText(
                        Translations.getString("ChildFiducialSelectorDialog.RadioButton.Placements.ToolTip")); //$NON-NLS-1$
                placementsButton.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        placementTypes = PlacementTypes.PlacementsOnly;
                        updateTable();
                    }
                });
                radioPanel.add(placementsButton);
            }
            {
                bothButton = new JRadioButton(
                        Translations.getString("ChildFiducialSelectorDialog.RadioButton.Both")); //$NON-NLS-1$
                bothButton.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        placementTypes = PlacementTypes.Both;
                        updateTable();
                    }
                });
                radioPanel.add(bothButton);
            }
            {
                ButtonGroup buttonGroup = new ButtonGroup();
                buttonGroup.add(placementsButton);
                buttonGroup.add(fiducialsButton);
                buttonGroup.add(bothButton);
            }
            {
                Component horizontalStrut = Box.createHorizontalStrut(40);
                radioPanel.add(horizontalStrut);
            }
            {
                JButton btnSelectGood = new JButton(
                        Translations.getString("ChildFiducialSelectorDialog.Button.AutoSelect")); //$NON-NLS-1$
                btnSelectGood.setToolTipText(
                        Translations.getString("ChildFiducialSelectorDialog.Button.AutoSelect.ToolTip")); //$NON-NLS-1$
                btnSelectGood.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        generateFilteredList();
                        generateGoodList();
                        Helpers.selectObjectTableRows(childFiducialsTable, goodPseudoPlacements);
                    }
                });
                radioPanel.add(btnSelectGood);
            }
            
            tableModel = new PlacementsHolderPlacementsTableModel() {
                @Override
                public boolean isCellEditable(int rowIndex, int columnIndex) {
                    return columnIndex == 0; //Only the enabled setting is editable
                }

            };
            
            tableModel.setLocalReferenceFrame(false);
            
            childFiducialsTable = new AutoSelectTextTable(tableModel);
            childFiducialsTable.setAutoCreateRowSorter(true);
            childFiducialsTable.getTableHeader()
                .setDefaultRenderer(new MultisortTableHeaderCellRenderer());
            childFiducialsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            childFiducialsTable.setDefaultRenderer(Part.class, 
                    new IdentifiableTableCellRenderer<Part>());
            
            //No need to see the Error Handling or Comments columns so remove them
            TableColumnModel tcm = childFiducialsTable.getColumnModel();
            tcm.removeColumn(tcm.getColumn(9)); //skip Comments column
            tcm.removeColumn(tcm.getColumn(8)); //skip Error Handling column
            
            JScrollPane scrollPane = new JScrollPane(childFiducialsTable);
            panel.add(scrollPane, BorderLayout.CENTER);
            {
                JPanel buttonPane = new JPanel();
                panel.add(buttonPane, BorderLayout.SOUTH);
                buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
                {
                    JButton okButton = new JButton(
                            Translations.getString("ChildFiducialSelectorDialog.Button.Ok")); //$NON-NLS-1$
                    okButton.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            addSelectedItemsToPanel();
                        }});
                    okButton.setActionCommand(
                            Translations.getString("ChildFiducialSelectorDialog.Button.Ok")); //$NON-NLS-1$
                    buttonPane.add(okButton);
                    getRootPane().setDefaultButton(okButton);
                }
                {
                    JButton cancelButton = new JButton(
                            Translations.getString("ChildFiducialSelectorDialog.Button.Cancel")); //$NON-NLS-1$
                    cancelButton.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            close();
                        }});
                    cancelButton.setActionCommand(
                            Translations.getString("ChildFiducialSelectorDialog.Button.Cancel")); //$NON-NLS-1$
                    buttonPane.add(cancelButton);
                }
            }
        }

        updateTable();
    }

    
    public List<Placement> getSelections() {
        ArrayList<Placement> selections = new ArrayList<>();
        int[] selectedRows = childFiducialsTable.getSelectedRows();
        for (int selectedRow : selectedRows) {
            selectedRow = childFiducialsTable.convertRowIndexToModel(selectedRow);
            selections.add((showHullOnly ? filteredPseudoPlacementsHull : 
                filteredPseudoPlacements).get(selectedRow));
        }
        return selections;
    }

    public void addSelectedItemsToPanel() {
        for (Placement fiducial : getSelections()) {
            panelLocation.getPanel().getDefinition().addPseudoPlacement(
                    panelLocation.getPanel().getDefinition().createPseudoPlacement(fiducial.getId()));
        }
        close();
    }
    
    public void close() {
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }
    
    private void updateTable() {
        generateFilteredList();
        if (showHullOnly) {
            tableModel.setPlacements(filteredPseudoPlacementsHull);
        }
        else {
            tableModel.setPlacements(filteredPseudoPlacements);
        }
    }
    
    private void generateAllPseudoPlacementsList(PanelLocation panelLocation) {
        for (PlacementsHolderLocation<?> child : panelLocation.getChildren()) {
            String uniqueId = child.getUniqueId();
            for (Placement placement : child.getPlacementsHolder().getPlacements()) {
                String id = (uniqueId != null ? uniqueId + PlacementsHolderLocation.ID_DELIMITTER : "") //$NON-NLS-1$
                        + placement.getId();
                
                Placement pseudoPlacement = new Placement(placement);
                pseudoPlacement.removePropertyChangeListener(pseudoPlacement);
                pseudoPlacement.setDefinition(pseudoPlacement);
                pseudoPlacement.setEnabled(true);
                pseudoPlacement.setLocation(Utils2D.calculateBoardPlacementLocation(child, 
                        placement).derive(null, null, 0.0, null));
                pseudoPlacement.setId(id);
                pseudoPlacement.setSide(placement.getSide().
                        flip(child.getGlobalSide() == Side.Bottom));
                pseudoPlacement.setComments(
                        Translations.getString("ChildFiducialSelectorDialog.PseudoPlacement.Comment")); //$NON-NLS-1$
                pseudoPlacement.addPropertyChangeListener(pseudoPlacement);
                allPseudoPlacements.add(pseudoPlacement);
            }
            if (child instanceof PanelLocation) {
                generateAllPseudoPlacementsList((PanelLocation) child);
            }
        }
    }
    
    private void generateFilteredList() {
        filteredPseudoPlacements = new ArrayList<>();
        for (Placement placement : allPseudoPlacements) {
            if (((placement.getType() == Placement.Type.Placement && 
                    (placementTypes == PlacementTypes.PlacementsOnly || 
                    placementTypes == PlacementTypes.Both)) ||
                    (placement.getType() == Placement.Type.Fiducial && 
                    (placementTypes == PlacementTypes.FiducialsOnly || 
                    placementTypes == PlacementTypes.Both)))) {
                filteredPseudoPlacements.add(placement);
            }
        }
        generateHullList();
    }
    
    private void generateHullList() {
        filteredPseudoPlacementsHull = new ArrayList<>();
        for (int iSide=0; iSide<2; iSide++) {
            Map<Point, Placement> pointToPlacementMap = new HashMap<>();
            List<Point> allPoints = new ArrayList<>();
            for (Placement placement : filteredPseudoPlacements) {
                if (placement.getSide() == (iSide == 0 ? Side.Top : Side.Bottom)) {
                    Location loc = placement.getLocation().convertToUnits(LengthUnit.Millimeters);
                    Point point = new Point(loc.getX(), loc.getY());
                    allPoints.add(point);
                    pointToPlacementMap.put(point, placement);
                }
            }
            List<Point> hullPoints = null;
            try {
                hullPoints = QuickHull.quickHull(allPoints);
            }
            catch (Exception ex) {
                hullPoints = allPoints;
            }
            
            for (Point point : hullPoints) {
                filteredPseudoPlacementsHull.add(pointToPlacementMap.get(point));
            }

        }
    }
    
    private void generateGoodList() {
        goodPseudoPlacements = new ArrayList<>();
        
        for (int iSide=0; iSide<2; iSide++) {
            List<Point> candidatePoints = new ArrayList<>();
            Map<Point, Placement> pointToPlacementMap = new HashMap<>(); 
            for (Placement placement : filteredPseudoPlacementsHull) {
                if (placement.isEnabled() && placement.getSide() == (iSide == 0 ? Side.Top : Side.Bottom)) {
                    Location loc = placement.getLocation().convertToUnits(LengthUnit.Millimeters);
                    Point point = new Point(loc.getX(), loc.getY());
                    candidatePoints.add(point);
                    pointToPlacementMap.put(point, placement);
                }
            }
            
            List<Point> goodPoints = null;
            int goodSize = Math.min(4, candidatePoints.size());
            double maxArea = 0;
            for (List<Point> points : Collect.allCombinationsOfSize(candidatePoints, goodSize)) {
                double area = Utils2D.polygonArea(points);
                if (goodPoints == null || area > maxArea) {
                    goodPoints = points;
                    maxArea = area;
                }
            }
            for (Point point : goodPoints) {
                goodPseudoPlacements.add(pointToPlacementMap.get(point));
            }
        }
    }
}
