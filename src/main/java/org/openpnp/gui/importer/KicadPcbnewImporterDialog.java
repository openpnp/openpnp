/*
 * Copyright (C) 2025 <jaytektas@github.com> 
 * inspired and based on work
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org> and Cri.S <phone.cri@gmail.com>
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

package org.openpnp.gui.importer;

import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import org.openpnp.Translations;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.model.Board;
import org.openpnp.model.Configuration;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.Component;
import java.text.DecimalFormat;
import javax.swing.JLabel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableModelEvent;
import org.openpnp.model.Abstract2DLocatable.Side;
import org.openpnp.model.Footprint;
import org.openpnp.model.Package;
import org.openpnp.model.Part;
import org.openpnp.model.Footprint.Pad;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Placement;

/**
 *
 * @author jay
 */
public class KicadPcbnewImporterDialog extends javax.swing.JDialog {

    KicadPcb kicadpcb;
    Board board = new Board();

    DoubleRenderer doubleRenderer = new DoubleRenderer();
    FlagBasedColorRenderer flagExistsRenderer = new FlagBasedColorRenderer("Exists", Color.ORANGE, Color.GREEN);

    Configuration cfg = Configuration.get();

    /**
     * A return status code - returned if Cancel button has been pressed
     */
    public static final int RET_CANCEL = 0;
    /**
     * A return status code - returned if OK button has been pressed
     */
    public static final int RET_OK = 1;

    /**
     * Creates new form
     * @param parent
     * @param modal
     */
    public KicadPcbnewImporterDialog(java.awt.Frame parent, boolean  modal) {
        super(parent, modal);
        initComponents();

        jTablePackages.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        jTablePackages.getSelectionModel().addListSelectionListener((ListSelectionEvent e) -> {
            if (!e.getValueIsAdjusting()) {
                int[] selectedRows = jTablePackages.getSelectedRows();
                if (selectedRows.length == 1) {
                    int selectedRowIndex = selectedRows[0];

                    // clear pad table
                    ((javax.swing.table.DefaultTableModel) jTablePads.getModel()).setRowCount(0);

                    // row have to find THIS kiFootprint
                    KiFootprint kifootprint = kicadpcb.getFootprint(getStringValueAtTable(jTablePackages, selectedRowIndex, "UUID"));

                    for (KiPad kipad : kifootprint.getKiPads()) {
                        double rotation = kipad.getLocation().getRotation();
                        rotation -= kifootprint.getLocation().getRotation();
                        rotation %= 360;

                        Object newPadRow[] = {
                            kipad.getName(),
                            kipad.getLocation().getX(),
                            kipad.getLocation().getY(),
                            kipad.getWidth(),
                            kipad.getHeight(),
                            rotation,
                            kipad.getRoundness()};
                        ((javax.swing.table.DefaultTableModel) jTablePads.getModel()).addRow(newPadRow);
                    }
                }
            }
        });

        jTablePlacements.getModel().addTableModelListener((TableModelEvent e) -> {
            int row = e.getFirstRow();
            int column = e.getColumn();
            TableModel model = (TableModel) e.getSource();
            String columnName = model.getColumnName(column);
            if (columnName.equals("I")) {
                populatePartTable();
                populatePackageTable();
            }
        });

        // Close the dialog when Esc is pressed
        String cancelName = "cancel";
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), cancelName);
        ActionMap actionMap = getRootPane().getActionMap();
        actionMap.put(cancelName, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doClose(RET_CANCEL);
            }
        });
    }

    public Board getBoard() {
        return board;
    }

    private void populatePackageTable() {
        ((javax.swing.table.DefaultTableModel) jTablePads.getModel()).setRowCount(0);
        ((javax.swing.table.DefaultTableModel) jTablePackages.getModel()).setRowCount(0);

        for (int row = 0; row < jTableParts.getModel().getRowCount(); row++) {
            boolean packageExists = false;
            boolean packageFound = false;

            KiFootprint kiFootprint = kicadpcb.getFootprint(getStringValueAtTable(jTableParts, row, "UUID"));

            // check table for entry
            for (int i = 0; i < jTablePackages.getModel().getRowCount(); i++) {
                if (kiFootprint.getPackageID().equals(getStringValueAtTable(jTableParts, i, "PackageID"))) {
                    packageFound = true;
                }
            }

            // package already in configuration?
            if (cfg.getPackage(kiFootprint.getPackageID()) != null) {
                packageExists = true;
            }

            if (!packageFound) {
                Object newPackageRow[] = {kiFootprint.getUUID(), packageExists, kiFootprint.getPackageID(), kiFootprint.getDescription(), kiFootprint.getStringProperty("Tape"), kiFootprint.getDoubleProperty("Width"), kiFootprint.getDoubleProperty("Length")};
                ((javax.swing.table.DefaultTableModel) jTablePackages.getModel()).addRow(newPackageRow);
            }
        }
    }

    private boolean shouldImport(KiFootprint kiFootprint) {
        if (!jCheckBoxPlaceDNP.isSelected()
                && (kiFootprint.getAttr().contains("dnp") || kiFootprint.getAttr().contains("exclude_from_pos_files"))) {
            return false;
        }

        if (!jCheckBoxPlaceThroughHole.isSelected() && kiFootprint.getAttr().contains("through_hole")) {
            return false;
        }

        if (!jCheckBoxPlaceSMD.isSelected() && kiFootprint.getAttr().contains("smd")) {
            return false;
        }

        if (jCheckBoxPlaceFCu.isSelected() && kiFootprint.getLayers().contains("F.Cu")) {
            return true;
        }

        if (jCheckBoxPlaceBCu.isSelected() && kiFootprint.getLayers().contains("B.Cu")) {
            return true;
        }

        return false;
    }

    private void populatePartTable() {
        ((javax.swing.table.DefaultTableModel) jTableParts.getModel()).setRowCount(0);
        for (int i = 0; i < jTablePlacements.getModel().getRowCount(); i++) {
            boolean partExists = false;
            boolean partFound = false;

            if (getBooleanValueAtTable(jTablePlacements, i, "I")) {
                KiFootprint kiFootprint = kicadpcb.getFootprint(getStringValueAtTable(jTablePlacements, i, "UUID"));

                // check table for entry
                for (int t = 0; t < jTableParts.getModel().getRowCount(); t++) {
                    if (kiFootprint.getPartID().equals(getStringValueAtTable(jTableParts, t, "PartID"))) {
                        partFound = true;
                    }
                }

                if (!partFound) {
                    // part already in configuration?
                    if (cfg.getPart(getStringValueAtTable(jTablePlacements, i, "PartID")) != null) {
                        partExists = true;
                    }

                    Double height = kiFootprint.getDoubleProperty("Height");

                    Object newPartRow[] = {
                        kiFootprint.getUUID(),
                        partExists,
                        kiFootprint.getPartID(),
                        kiFootprint.getDescription(),
                        kiFootprint.getPackageID(),
                        height};

                    ((javax.swing.table.DefaultTableModel) jTableParts.getModel()).addRow(newPartRow);
                }
            }
        }
    }

    private void populatePlacementTable() {
        ((javax.swing.table.DefaultTableModel) jTablePlacements.getModel()).setRowCount(0);
        for (KiFootprint kiFootprint : kicadpcb.getKiFootprints()) {
            boolean partExists = false;

            // part already in configuration?
            if (cfg.getPart(kiFootprint.getPartID()) != null) {
                partExists = true;
            }

            double placementX = kiFootprint.getLocation().getX();
            double placementY = kiFootprint.getLocation().getY();
            if (jCheckBoxUseDrillPlaceOrigin.isSelected()) {
                placementX -= kicadpcb.getOriginX();
                placementY -= kicadpcb.getOriginY();
            }

            // invert
            placementY = -placementY;

            double rotation = kiFootprint.getLocation().getRotation();
            String placementSide = "Top";

            // placement layer may change location
            if (kiFootprint.getLayers().contains("B.Cu")) {
                placementSide = "Bottom";
                if (placementX != 0) {
                    placementX = -placementX;
                }
                rotation = 180 - rotation;
                if (rotation == -0.0) {
                    rotation = 0;
                }
            }

            Object newPlacementRow[]
                    = {
                        kiFootprint.getUUID(),
                        partExists,
                        shouldImport(kiFootprint),
                        kiFootprint.getStringProperty("Reference"),
                        kiFootprint.getPartID(),
                        placementSide,
                        placementX,
                        placementY,
                        rotation,
                        kiFootprint.getStringProperty("#")};

            ((javax.swing.table.DefaultTableModel) jTablePlacements.getModel()).addRow(newPlacementRow);
        }
    }

    private void populateTables() {
        populatePlacementTable();
        populatePartTable();
        populatePackageTable();
    }

    /**
     * @return the return status of this dialog - one of RET_OK or RET_CANCEL
     */
    public int getReturnStatus() {
        return returnStatus;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane4 = new javax.swing.JTabbedPane();
        jPanel7 = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        jTablePlacements = new javax.swing.JTable();
        jCheckBoxUseDrillPlaceOrigin = new javax.swing.JCheckBox();
        jCheckBoxPlaceDNP = new javax.swing.JCheckBox();
        jCheckBoxPlaceSMD = new javax.swing.JCheckBox();
        jCheckBoxPlaceFCu = new javax.swing.JCheckBox();
        jCheckBoxPlaceBCu = new javax.swing.JCheckBox();
        jCheckBoxPlaceThroughHole = new javax.swing.JCheckBox();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane5 = new javax.swing.JScrollPane();
        jTableParts = new javax.swing.JTable();
        jCheckBoxUpdatePart = new javax.swing.JCheckBox();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane6 = new javax.swing.JScrollPane();
        jTablePackages = new javax.swing.JTable();
        jScrollPane7 = new javax.swing.JScrollPane();
        jTablePads = new javax.swing.JTable();
        jCheckBoxUpdatePackage = new javax.swing.JCheckBox();
        jCheckBoxUpdateFootprint = new javax.swing.JCheckBox();
        jButtonChooseFile = new javax.swing.JButton();
        jButtonImport = new javax.swing.JButton();
        jButtonCancel = new javax.swing.JButton();

        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        jScrollPane4.setPreferredSize(new java.awt.Dimension(200, 200));

        jTablePlacements.setAutoCreateRowSorter(true);
        jTablePlacements.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "UUID", "Exists", "I", "Ref", "PartID", "Side", "X", "Y", "Rotation", "Comment"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, true, false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane4.setViewportView(jTablePlacements);
        if (jTablePlacements.getColumnModel().getColumnCount() > 0) {
            jTablePlacements.getColumnModel().getColumn(0).setMinWidth(0);
            jTablePlacements.getColumnModel().getColumn(0).setPreferredWidth(0);
            jTablePlacements.getColumnModel().getColumn(0).setMaxWidth(0);
            jTablePlacements.getColumnModel().getColumn(1).setMinWidth(0);
            jTablePlacements.getColumnModel().getColumn(1).setPreferredWidth(0);
            jTablePlacements.getColumnModel().getColumn(1).setMaxWidth(0);
            jTablePlacements.getColumnModel().getColumn(2).setMinWidth(30);
            jTablePlacements.getColumnModel().getColumn(2).setPreferredWidth(30);
            jTablePlacements.getColumnModel().getColumn(2).setMaxWidth(30);
            jTablePlacements.getColumnModel().getColumn(3).setPreferredWidth(45);
            jTablePlacements.getColumnModel().getColumn(4).setPreferredWidth(150);
            jTablePlacements.getColumnModel().getColumn(4).setCellRenderer(flagExistsRenderer);
            jTablePlacements.getColumnModel().getColumn(5).setPreferredWidth(50);
            jTablePlacements.getColumnModel().getColumn(6).setMinWidth(65);
            jTablePlacements.getColumnModel().getColumn(6).setPreferredWidth(65);
            jTablePlacements.getColumnModel().getColumn(6).setMaxWidth(65);
            jTablePlacements.getColumnModel().getColumn(6).setCellRenderer(doubleRenderer);
            jTablePlacements.getColumnModel().getColumn(7).setMinWidth(65);
            jTablePlacements.getColumnModel().getColumn(7).setPreferredWidth(65);
            jTablePlacements.getColumnModel().getColumn(7).setMaxWidth(65);
            jTablePlacements.getColumnModel().getColumn(7).setCellRenderer(doubleRenderer);
            jTablePlacements.getColumnModel().getColumn(8).setMinWidth(65);
            jTablePlacements.getColumnModel().getColumn(8).setPreferredWidth(65);
            jTablePlacements.getColumnModel().getColumn(8).setMaxWidth(65);
            jTablePlacements.getColumnModel().getColumn(8).setCellRenderer(doubleRenderer);
            jTablePlacements.getColumnModel().getColumn(9).setPreferredWidth(400);
        }

        jCheckBoxUseDrillPlaceOrigin.setSelected(true);
        jCheckBoxUseDrillPlaceOrigin.setLabel("Drill/Place File Origin");
        jCheckBoxUseDrillPlaceOrigin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxUseDrillPlaceOriginActionPerformed(evt);
            }
        });

        jCheckBoxPlaceDNP.setText("Place DNP");
        jCheckBoxPlaceDNP.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxPlaceDNPActionPerformed(evt);
            }
        });

        jCheckBoxPlaceSMD.setSelected(true);
        jCheckBoxPlaceSMD.setText("Place SMD");
        jCheckBoxPlaceSMD.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxPlaceSMDActionPerformed(evt);
            }
        });

        jCheckBoxPlaceFCu.setSelected(true);
        jCheckBoxPlaceFCu.setText("Place F.Cu");
        jCheckBoxPlaceFCu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxPlaceFCuActionPerformed(evt);
            }
        });

        jCheckBoxPlaceBCu.setSelected(true);
        jCheckBoxPlaceBCu.setText("Place B.Cu");
        jCheckBoxPlaceBCu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxPlaceBCuActionPerformed(evt);
            }
        });

        jCheckBoxPlaceThroughHole.setText("Place Through hole");
        jCheckBoxPlaceThroughHole.setActionCommand("Place through hole");
        jCheckBoxPlaceThroughHole.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxPlaceThroughHoleActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addComponent(jCheckBoxUseDrillPlaceOrigin)
                        .addGap(18, 18, 18)
                        .addComponent(jCheckBoxPlaceDNP)
                        .addGap(18, 18, 18)
                        .addComponent(jCheckBoxPlaceSMD)
                        .addGap(18, 18, 18)
                        .addComponent(jCheckBoxPlaceFCu)
                        .addGap(18, 18, 18)
                        .addComponent(jCheckBoxPlaceBCu)
                        .addGap(18, 18, 18)
                        .addComponent(jCheckBoxPlaceThroughHole)
                        .addGap(0, 355, Short.MAX_VALUE))))
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 630, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckBoxUseDrillPlaceOrigin)
                    .addComponent(jCheckBoxPlaceDNP)
                    .addComponent(jCheckBoxPlaceSMD)
                    .addComponent(jCheckBoxPlaceFCu)
                    .addComponent(jCheckBoxPlaceBCu)
                    .addComponent(jCheckBoxPlaceThroughHole))
                .addContainerGap())
        );

        jTabbedPane4.addTab("Placements", jPanel7);

        jTableParts.setAutoCreateRowSorter(true);
        jTableParts.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "UUID", "Exists", "PartID", "Description", "PackageID", "Height"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Boolean.class, java.lang.Object.class, java.lang.Object.class, java.lang.String.class, java.lang.Double.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane5.setViewportView(jTableParts);
        if (jTableParts.getColumnModel().getColumnCount() > 0) {
            jTableParts.getColumnModel().getColumn(0).setMinWidth(0);
            jTableParts.getColumnModel().getColumn(0).setPreferredWidth(0);
            jTableParts.getColumnModel().getColumn(0).setMaxWidth(0);
            jTableParts.getColumnModel().getColumn(1).setMinWidth(0);
            jTableParts.getColumnModel().getColumn(1).setPreferredWidth(0);
            jTableParts.getColumnModel().getColumn(1).setMaxWidth(0);
            jTableParts.getColumnModel().getColumn(2).setPreferredWidth(150);
            jTableParts.getColumnModel().getColumn(2).setCellRenderer(flagExistsRenderer);
            jTableParts.getColumnModel().getColumn(3).setPreferredWidth(400);
            jTableParts.getColumnModel().getColumn(4).setPreferredWidth(150);
            jTableParts.getColumnModel().getColumn(5).setMinWidth(65);
            jTableParts.getColumnModel().getColumn(5).setPreferredWidth(65);
            jTableParts.getColumnModel().getColumn(5).setMaxWidth(65);
            jTableParts.getColumnModel().getColumn(5).setCellRenderer(doubleRenderer);
        }

        jCheckBoxUpdatePart.setSelected(true);
        jCheckBoxUpdatePart.setText("Update existing Part");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 1131, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jCheckBoxUpdatePart)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 629, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxUpdatePart)
                .addContainerGap())
        );

        jTabbedPane4.addTab("Parts", jPanel1);

        jTablePackages.setAutoCreateRowSorter(true);
        jTablePackages.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "UUID", "Exists", "PackageID", "Description", "Tape Specification", "Width", "Length"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Boolean.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Double.class, java.lang.Double.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane6.setViewportView(jTablePackages);
        if (jTablePackages.getColumnModel().getColumnCount() > 0) {
            jTablePackages.getColumnModel().getColumn(0).setMinWidth(0);
            jTablePackages.getColumnModel().getColumn(0).setPreferredWidth(0);
            jTablePackages.getColumnModel().getColumn(0).setMaxWidth(0);
            jTablePackages.getColumnModel().getColumn(1).setMinWidth(0);
            jTablePackages.getColumnModel().getColumn(1).setPreferredWidth(0);
            jTablePackages.getColumnModel().getColumn(1).setMaxWidth(0);
            jTablePackages.getColumnModel().getColumn(2).setPreferredWidth(150);
            jTablePackages.getColumnModel().getColumn(2).setCellRenderer(flagExistsRenderer);
            jTablePackages.getColumnModel().getColumn(3).setPreferredWidth(400);
            jTablePackages.getColumnModel().getColumn(4).setPreferredWidth(100);
            jTablePackages.getColumnModel().getColumn(5).setMinWidth(65);
            jTablePackages.getColumnModel().getColumn(5).setPreferredWidth(65);
            jTablePackages.getColumnModel().getColumn(5).setMaxWidth(65);
            jTablePackages.getColumnModel().getColumn(5).setCellRenderer(doubleRenderer);
            jTablePackages.getColumnModel().getColumn(6).setMinWidth(65);
            jTablePackages.getColumnModel().getColumn(6).setPreferredWidth(65);
            jTablePackages.getColumnModel().getColumn(6).setMaxWidth(65);
            jTablePackages.getColumnModel().getColumn(6).setCellRenderer(doubleRenderer);
        }

        jTablePads.setAutoCreateRowSorter(true);
        jTablePads.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Name", "X", "Y", "Width", "Length", "Rot", "Round"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane7.setViewportView(jTablePads);
        if (jTablePads.getColumnModel().getColumnCount() > 0) {
            jTablePads.getColumnModel().getColumn(0).setPreferredWidth(100);
            jTablePads.getColumnModel().getColumn(1).setMinWidth(65);
            jTablePads.getColumnModel().getColumn(1).setPreferredWidth(65);
            jTablePads.getColumnModel().getColumn(1).setMaxWidth(65);
            jTablePads.getColumnModel().getColumn(1).setCellRenderer(doubleRenderer);
            jTablePads.getColumnModel().getColumn(2).setMinWidth(65);
            jTablePads.getColumnModel().getColumn(2).setPreferredWidth(65);
            jTablePads.getColumnModel().getColumn(2).setMaxWidth(65);
            jTablePads.getColumnModel().getColumn(2).setCellRenderer(doubleRenderer);
            jTablePads.getColumnModel().getColumn(3).setMinWidth(65);
            jTablePads.getColumnModel().getColumn(3).setPreferredWidth(65);
            jTablePads.getColumnModel().getColumn(3).setMaxWidth(65);
            jTablePads.getColumnModel().getColumn(3).setCellRenderer(doubleRenderer);
            jTablePads.getColumnModel().getColumn(4).setMinWidth(65);
            jTablePads.getColumnModel().getColumn(4).setPreferredWidth(65);
            jTablePads.getColumnModel().getColumn(4).setMaxWidth(65);
            jTablePads.getColumnModel().getColumn(4).setCellRenderer(doubleRenderer);
            jTablePads.getColumnModel().getColumn(5).setMinWidth(65);
            jTablePads.getColumnModel().getColumn(5).setPreferredWidth(65);
            jTablePads.getColumnModel().getColumn(5).setMaxWidth(65);
            jTablePads.getColumnModel().getColumn(5).setCellRenderer(doubleRenderer);
            jTablePads.getColumnModel().getColumn(6).setMinWidth(65);
            jTablePads.getColumnModel().getColumn(6).setPreferredWidth(65);
            jTablePads.getColumnModel().getColumn(6).setMaxWidth(65);
            jTablePads.getColumnModel().getColumn(6).setCellRenderer(doubleRenderer);
        }

        jCheckBoxUpdatePackage.setSelected(true);
        jCheckBoxUpdatePackage.setText("Update existing packages");

        jCheckBoxUpdateFootprint.setSelected(true);
        jCheckBoxUpdateFootprint.setText("Update footprint");
        jCheckBoxUpdateFootprint.setActionCommand("Update existing footprint");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jScrollPane7, javax.swing.GroupLayout.PREFERRED_SIZE, 483, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(626, 654, Short.MAX_VALUE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jScrollPane6)
                        .addContainerGap())
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jCheckBoxUpdatePackage)
                            .addComponent(jCheckBoxUpdateFootprint))
                        .addGap(0, 0, Short.MAX_VALUE))))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 368, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxUpdatePackage)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane7, javax.swing.GroupLayout.PREFERRED_SIZE, 227, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxUpdateFootprint)
                .addContainerGap())
        );

        jTabbedPane4.addTab("Packages", jPanel2);

        jButtonChooseFile.setLabel("Choose pcbnew file...");
        jButtonChooseFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonChooseFileActionPerformed(evt);
            }
        });

        jButtonImport.setText("Import");
        jButtonImport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonImportActionPerformed(evt);
            }
        });

        jButtonCancel.setText("Cancel");
        jButtonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCancelActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTabbedPane4)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jButtonChooseFile)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButtonCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jButtonImport, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jTabbedPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 704, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jButtonImport, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jButtonCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jButtonChooseFile, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Closes the dialog
     */
    private void closeDialog(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_closeDialog
        doClose(RET_CANCEL);
    }//GEN-LAST:event_closeDialog

    private void jCheckBoxUseDrillPlaceOriginActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxUseDrillPlaceOriginActionPerformed
        populatePlacementTable();
    }//GEN-LAST:event_jCheckBoxUseDrillPlaceOriginActionPerformed

    private void jCheckBoxPlaceBCuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxPlaceBCuActionPerformed
        populateTables();
    }//GEN-LAST:event_jCheckBoxPlaceBCuActionPerformed


    private void jButtonChooseFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonChooseFileActionPerformed
        // choose file
        FileDialog fileDialog = new FileDialog(this);
        fileDialog.setFilenameFilter(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".kicad_pcb"); //$NON-NLS-1$
            }
        });

        fileDialog.setFile("*.kicad_pcb"); //$NON-NLS-1$
        fileDialog.pack();
        fileDialog.setLocationRelativeTo(this);
        fileDialog.setVisible(true);

        try {
            if (fileDialog.getFile() == null) {
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();

            MessageBoxes.errorBox(this,
                    Translations.getString("BoardsPanel.Action.AddBoard.ExistingBoard.ErrorMessage"), //$NON-NLS-1$
                    e.getMessage());
        }

        // open the file and read as a string
        Path path = Paths.get(fileDialog.getDirectory() + fileDialog.getFile());
        fileDialog.dispose();

        String sexpFile = "";
        try {
            sexpFile = Files.readString(path);
        } catch (IOException ex) {
            Logger.getLogger(KicadPcbnewImporter.class.getName()).log(Level.SEVERE, null, ex);
        }

        kicadpcb = new KicadPcb(sexpFile);

        populateTables();

    }//GEN-LAST:event_jButtonChooseFileActionPerformed

    private void jButtonImportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonImportActionPerformed

        for (int row = 0; row < jTablePackages.getModel().getRowCount(); row++) {

            String uuid = getStringValueAtTable(jTablePackages, row, "UUID");
            String packageID = getStringValueAtTable(jTablePackages, row, "PackageID");
            Package pnpPackage = cfg.getPackage(packageID);
            KiFootprint kiFootprint = kicadpcb.getFootprint(uuid);

            if (pnpPackage == null) {
                pnpPackage = new Package(packageID);
                cfg.addPackage(pnpPackage);
            }

            // package import
            if (getBooleanValueAtTable(jTablePackages, row, "I")) {
                if (jCheckBoxUpdatePackage.isSelected()) {
                    pnpPackage.setDescription(getStringValueAtTable(jTablePackages, row, "Description"));
                    pnpPackage.setTapeSpecification(getStringValueAtTable(jTablePackages, row, "Tape Specification"));

                    boolean newFootprint = false;
                    Footprint pnpFootprint = pnpPackage.getFootprint();

                    if (pnpFootprint == null) {
                        pnpFootprint = new Footprint();
                        pnpPackage.setFootprint(pnpFootprint);
                        newFootprint = true;
                    }

                    // footprint import
                    if (newFootprint || jCheckBoxUpdateFootprint.isSelected()) {

                        pnpFootprint.setBodyWidth(getDoubleValueAtTable(jTablePackages, row, "Width"));
                        pnpFootprint.setBodyHeight(getDoubleValueAtTable(jTablePackages, row, "Length"));

                        // fetch all the pads from file
                        pnpFootprint.removeAllPads();
                        // add pads to kiFootprint
                        for (KiPad kipad : kiFootprint.getKiPads()) {
                            Pad pnpPad = new Pad();
                            pnpPad.setName(kipad.getName());
                            pnpPad.setWidth(kipad.getWidth());
                            pnpPad.setHeight(kipad.getHeight());
                            pnpPad.setX(kipad.getLocation().getX());
                            pnpPad.setY(-kipad.getLocation().getY());

                            double rotation = (kipad.getLocation().getRotation() - kiFootprint.getLocation().getRotation()) % 360;
                            pnpPad.setRotation(rotation);
                            pnpPad.setRoundness(kipad.getRoundness());

                            pnpFootprint.addPad(pnpPad);
                        }
                    }
                }
            }
        }

        // parts import
        for (int row = 0; row < jTableParts.getModel().getRowCount(); row++) {
            // part import
            String partID = getStringValueAtTable(jTableParts, row, "PartID");
            Part pnpPart = cfg.getPart(partID);

            boolean newPart = false;

            if (pnpPart == null) {
                pnpPart = new Part(partID);
                cfg.addPart(pnpPart);
                newPart = true;
            }

            if (newPart || jCheckBoxUpdatePart.isSelected()) {

                pnpPart.setHeight(new Length(getDoubleValueAtTable(jTableParts, row, "Height"), LengthUnit.Millimeters));
                pnpPart.setName(getStringValueAtTable(jTableParts, row, "Description"));

                String packageID = getStringValueAtTable(jTableParts, row, "PackageID");
                pnpPart.setPackage(cfg.getPackage(packageID));
            }
        }

        // placements
        for (int row = 0; row < jTablePlacements.getModel().getRowCount(); row++) {

            // placement
            if (getValueAtTable(jTablePlacements, row, "I").toString().equalsIgnoreCase("true")) {
                Placement pnpPlacement = new Placement(getStringValueAtTable(jTablePlacements, row, "Ref"));
                pnpPlacement.setLocation(new Location(LengthUnit.Millimeters,
                        getDoubleValueAtTable(jTablePlacements, row, "X"),
                        getDoubleValueAtTable(jTablePlacements, row, "Y"),
                        0,
                        getDoubleValueAtTable(jTablePlacements, row, "Rotation")
                ));

                if (getStringValueAtTable(jTablePlacements, row, "Side").equals("Top")) {
                    pnpPlacement.setSide(Side.Top);
                } else {
                    pnpPlacement.setSide(Side.Bottom);
                }
                String partID = getStringValueAtTable(jTablePlacements, row, "PartID");
                pnpPlacement.setPart(cfg.getPart(partID));
                pnpPlacement.setComments(getStringValueAtTable(jTablePlacements, row, "Comment"));
                board.addPlacement(pnpPlacement);
            }
        }

        doClose(RET_OK);
    }//GEN-LAST:event_jButtonImportActionPerformed

    private void jButtonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCancelActionPerformed
        returnStatus = RET_CANCEL;
        setVisible(false);
    }//GEN-LAST:event_jButtonCancelActionPerformed

    private void jCheckBoxPlaceSMDActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxPlaceSMDActionPerformed
        populateTables();
    }//GEN-LAST:event_jCheckBoxPlaceSMDActionPerformed

    private void jCheckBoxPlaceDNPActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxPlaceDNPActionPerformed
        populateTables();
    }//GEN-LAST:event_jCheckBoxPlaceDNPActionPerformed

    private void jCheckBoxPlaceFCuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxPlaceFCuActionPerformed
        populateTables();
    }//GEN-LAST:event_jCheckBoxPlaceFCuActionPerformed

    private void jCheckBoxPlaceThroughHoleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxPlaceThroughHoleActionPerformed
        populateTables();
    }//GEN-LAST:event_jCheckBoxPlaceThroughHoleActionPerformed

    private void doClose(int retStatus) {
        returnStatus = retStatus;
        setVisible(false);
    }

    private class ColorTableCellRenderer extends DefaultTableCellRenderer {

        private Color textColor;

        public ColorTableCellRenderer(Color color) {
            this.textColor = color;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus,
                int row, int column) {
            // Get the default component for rendering
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            // Set the foreground color (text color)
            component.setForeground(textColor);

            return component;
        }
    }

    class DoubleRenderer extends JLabel implements TableCellRenderer {

        private DecimalFormat formatter;

        public DoubleRenderer() {
            setHorizontalAlignment(JLabel.RIGHT); // Align text to the right for better readability
            formatter = new DecimalFormat("#,##0.000"); // Format to three decimal places with commas
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus,
                int row, int column) {
            if (value instanceof Double) {
                setText(formatter.format((Double) value));
            } else {
                // Handle cases where the cell might not contain a Double
                setText(value == null ? "" : value.toString());
            }

            // Set background and foreground colors based on selection and focus
            if (isSelected) {
                setOpaque(true);
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }

            return this;
        }
    }

    private class FlagBasedColorRenderer extends DefaultTableCellRenderer {

        private String flagColumn;
        private Color trueColor;
        private Color falseColor;

        public FlagBasedColorRenderer(String flagColumn, Color trueColor, Color falseColor) {
            this.flagColumn = flagColumn;
            this.trueColor = trueColor;
            this.falseColor = falseColor;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus,
                int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (getBooleanValueAtTable(table, row, flagColumn)) {
                component.setForeground(trueColor);
            } else {
                component.setForeground(falseColor);
            }

            return component;
        }
    }

    private static boolean getBooleanValueAtTable(JTable table, int row, String columnName) {
        Object value = getValueAtTable(table, row, columnName);
        if (value instanceof Boolean) {
            return ((Boolean) value);
        }
        return false;
    }

    private static double getDoubleValueAtTable(JTable table, int row, String columnName) {
        Object value = getValueAtTable(table, row, columnName);
        if (value instanceof Double) {
            return ((Double) value);
        }
        return 0;
    }

    private static String getStringValueAtTable(JTable table, int row, String columnName) {
        Object value = getValueAtTable(table, row, columnName);
        if (value instanceof String) {
            return value.toString();
        }
        return new String();
    }

    private static Object getValueAtTable(JTable table, int row, String columnName) {
        int columnIndex = findColumnIndex(table.getModel(), columnName);
        if (columnIndex != -1) {
            row = table.convertRowIndexToModel(row);
            return table.getModel().getValueAt(row, columnIndex);
        }
        return null; // Or throw an exception if the column is not found
    }

    private static int findColumnIndex(TableModel model, String columnName) {
        for (int i = 0; i < model.getColumnCount(); i++) {
            if (columnName.equals(model.getColumnName(i))) {
                return i;
            }
        }
        return -1; // Column not found
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonCancel;
    private javax.swing.JButton jButtonChooseFile;
    private javax.swing.JButton jButtonImport;
    private javax.swing.JCheckBox jCheckBoxPlaceBCu;
    private javax.swing.JCheckBox jCheckBoxPlaceDNP;
    private javax.swing.JCheckBox jCheckBoxPlaceFCu;
    private javax.swing.JCheckBox jCheckBoxPlaceSMD;
    private javax.swing.JCheckBox jCheckBoxPlaceThroughHole;
    private javax.swing.JCheckBox jCheckBoxUpdateFootprint;
    private javax.swing.JCheckBox jCheckBoxUpdatePackage;
    private javax.swing.JCheckBox jCheckBoxUpdatePart;
    private javax.swing.JCheckBox jCheckBoxUseDrillPlaceOrigin;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JTabbedPane jTabbedPane4;
    private javax.swing.JTable jTablePackages;
    private javax.swing.JTable jTablePads;
    private javax.swing.JTable jTableParts;
    private javax.swing.JTable jTablePlacements;
    // End of variables declaration//GEN-END:variables

    private int returnStatus = RET_CANCEL;
}
