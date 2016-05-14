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

package org.openpnp.machine.reference.wizards;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.ConfigurationListener;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.AutoSelectTextTable;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.machine.reference.ReferenceNozzleTip;
import org.openpnp.model.Configuration;
import org.openpnp.util.UiUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.ui.CvPipelineEditor;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class ReferenceNozzleTipConfigurationWizard extends AbstractConfigurationWizard {
    private final ReferenceNozzleTip nozzleTip;
    private JPanel panelChanger;
    private JLabel lblX_1;
    private JLabel lblY_1;
    private JLabel lblZ_1;
    private LocationButtonsPanel changerStartLocationButtonsPanel;
    private JLabel lblStartLocation;
    private JTextField textFieldChangerStartX;
    private JTextField textFieldChangerStartY;
    private JTextField textFieldChangerStartZ;
    private JLabel lblMiddleLocation;
    private JTextField textFieldChangerMidX;
    private JTextField textFieldChangerMidY;
    private JTextField textFieldChangerMidZ;
    private JLabel lblEndLocation;
    private JTextField textFieldChangerEndX;
    private JTextField textFieldChangerEndY;
    private JTextField textFieldChangerEndZ;
    private LocationButtonsPanel changerMidLocationButtonsPanel;
    private LocationButtonsPanel changerEndLocationButtonsPanel;
    private JPanel panelPackageCompat;
    private JCheckBox chckbxAllowIncompatiblePackages;
    private JScrollPane scrollPane;
    private JTable table;
    private PackagesTableModel tableModel;

    private Set<org.openpnp.model.Package> compatiblePackages = new HashSet<>();
    private JPanel panelCalibration;
    private JButton btnEditPipeline;
    private JButton btnCalibrate;

    public ReferenceNozzleTipConfigurationWizard(ReferenceNozzleTip nozzleTip) {
        this.nozzleTip = nozzleTip;

        panelPackageCompat = new JPanel();
        panelPackageCompat.setBorder(new TitledBorder(null, "Package Compatibility",
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelPackageCompat);
        panelPackageCompat.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC,
                        ColumnSpec.decode("default:grow"),},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, RowSpec.decode("max(100dlu;min)"),}));

        chckbxAllowIncompatiblePackages = new JCheckBox("Allow Incompatible Packages?");
        panelPackageCompat.add(chckbxAllowIncompatiblePackages, "2, 2");

        scrollPane = new JScrollPane();
        panelPackageCompat.add(scrollPane, "2, 4, fill, default");

        table = new AutoSelectTextTable(tableModel = new PackagesTableModel());
        scrollPane.setViewportView(table);

        panelChanger = new JPanel();
        panelChanger.setBorder(new TitledBorder(null, "Nozzle Tip Changer", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        contentPanel.add(panelChanger);
        panelChanger.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        lblX_1 = new JLabel("X");
        panelChanger.add(lblX_1, "4, 2");

        lblY_1 = new JLabel("Y");
        panelChanger.add(lblY_1, "6, 2");

        lblZ_1 = new JLabel("Z");
        panelChanger.add(lblZ_1, "8, 2");

        lblStartLocation = new JLabel("Start Location");
        panelChanger.add(lblStartLocation, "2, 4, right, default");

        textFieldChangerStartX = new JTextField();
        panelChanger.add(textFieldChangerStartX, "4, 4, fill, default");
        textFieldChangerStartX.setColumns(5);

        textFieldChangerStartY = new JTextField();
        panelChanger.add(textFieldChangerStartY, "6, 4, fill, default");
        textFieldChangerStartY.setColumns(5);

        textFieldChangerStartZ = new JTextField();
        panelChanger.add(textFieldChangerStartZ, "8, 4, fill, default");
        textFieldChangerStartZ.setColumns(5);

        changerStartLocationButtonsPanel = new LocationButtonsPanel(textFieldChangerStartX,
                textFieldChangerStartY, textFieldChangerStartZ, (JTextField) null);
        changerStartLocationButtonsPanel.setShowPositionToolNoSafeZ(true);
        panelChanger.add(changerStartLocationButtonsPanel, "10, 4, fill, default");

        lblMiddleLocation = new JLabel("Middle Location");
        panelChanger.add(lblMiddleLocation, "2, 6, right, default");

        textFieldChangerMidX = new JTextField();
        panelChanger.add(textFieldChangerMidX, "4, 6, fill, default");
        textFieldChangerMidX.setColumns(5);

        textFieldChangerMidY = new JTextField();
        panelChanger.add(textFieldChangerMidY, "6, 6, fill, default");
        textFieldChangerMidY.setColumns(5);

        textFieldChangerMidZ = new JTextField();
        panelChanger.add(textFieldChangerMidZ, "8, 6, fill, default");
        textFieldChangerMidZ.setColumns(5);

        changerMidLocationButtonsPanel = new LocationButtonsPanel(textFieldChangerMidX,
                textFieldChangerMidY, textFieldChangerMidZ, (JTextField) null);
        changerMidLocationButtonsPanel.setShowPositionToolNoSafeZ(true);
        panelChanger.add(changerMidLocationButtonsPanel, "10, 6, fill, default");

        lblEndLocation = new JLabel("End Location");
        panelChanger.add(lblEndLocation, "2, 8, right, default");

        textFieldChangerEndX = new JTextField();
        panelChanger.add(textFieldChangerEndX, "4, 8, fill, default");
        textFieldChangerEndX.setColumns(5);

        textFieldChangerEndY = new JTextField();
        panelChanger.add(textFieldChangerEndY, "6, 8, fill, default");
        textFieldChangerEndY.setColumns(5);

        textFieldChangerEndZ = new JTextField();
        panelChanger.add(textFieldChangerEndZ, "8, 8, fill, default");
        textFieldChangerEndZ.setColumns(5);

        changerEndLocationButtonsPanel = new LocationButtonsPanel(textFieldChangerEndX,
                textFieldChangerEndY, textFieldChangerEndZ, (JTextField) null);
        changerEndLocationButtonsPanel.setShowPositionToolNoSafeZ(true);
        panelChanger.add(changerEndLocationButtonsPanel, "10, 8, fill, default");

        panelCalibration = new JPanel();
        panelCalibration.setBorder(new TitledBorder(null, "Calibration", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        // contentPanel.add(panelCalibration);
        panelCalibration
                .setLayout(
                        new FormLayout(new ColumnSpec[] {FormSpecs.DEFAULT_COLSPEC,},
                                new RowSpec[] {RowSpec.decode("23px"),
                                        FormSpecs.RELATED_GAP_ROWSPEC,
                                        FormSpecs.DEFAULT_ROWSPEC,}));

        btnEditPipeline = new JButton("Edit Pipeline");
        btnEditPipeline.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                UiUtils.messageBoxOnException(() -> {
                    editCalibrationPipeline();
                });
            }
        });
        panelCalibration.add(btnEditPipeline, "1, 1, left, top");

        btnCalibrate = new JButton("Calibrate");
        btnCalibrate.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                calibrate();
            }
        });
        panelCalibration.add(btnCalibrate, "1, 3");
    }

    private void editCalibrationPipeline() throws Exception {
        CvPipeline pipeline = nozzleTip.getCalibration().getCalibrationPipeline();
        CvPipelineEditor editor = new CvPipelineEditor(pipeline);
        JDialog dialog = new JDialog(MainFrame.mainFrame, "Calibration Pipeline");
        dialog.getContentPane().setLayout(new BorderLayout());
        dialog.getContentPane().add(editor);
        dialog.setSize(1024, 768);
        dialog.setVisible(true);
    }

    private void calibrate() {
        UiUtils.submitUiMachineTask(() -> {
            nozzleTip.getCalibration().calibrate(nozzleTip);
        });
    }

    @Override
    public void createBindings() {
        LengthConverter lengthConverter = new LengthConverter();

        addWrappedBinding(nozzleTip, "allowIncompatiblePackages", chckbxAllowIncompatiblePackages,
                "selected");

        MutableLocationProxy changerStartLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, nozzleTip, "changerStartLocation", changerStartLocation,
                "location");
        addWrappedBinding(changerStartLocation, "lengthX", textFieldChangerStartX, "text",
                lengthConverter);
        addWrappedBinding(changerStartLocation, "lengthY", textFieldChangerStartY, "text",
                lengthConverter);
        addWrappedBinding(changerStartLocation, "lengthZ", textFieldChangerStartZ, "text",
                lengthConverter);

        MutableLocationProxy changerMidLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, nozzleTip, "changerMidLocation", changerMidLocation,
                "location");
        addWrappedBinding(changerMidLocation, "lengthX", textFieldChangerMidX, "text",
                lengthConverter);
        addWrappedBinding(changerMidLocation, "lengthY", textFieldChangerMidY, "text",
                lengthConverter);
        addWrappedBinding(changerMidLocation, "lengthZ", textFieldChangerMidZ, "text",
                lengthConverter);

        MutableLocationProxy changerEndLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, nozzleTip, "changerEndLocation", changerEndLocation,
                "location");
        addWrappedBinding(changerEndLocation, "lengthX", textFieldChangerEndX, "text",
                lengthConverter);
        addWrappedBinding(changerEndLocation, "lengthY", textFieldChangerEndY, "text",
                lengthConverter);
        addWrappedBinding(changerEndLocation, "lengthZ", textFieldChangerEndZ, "text",
                lengthConverter);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerStartX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerStartY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerStartZ);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerMidX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerMidY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerMidZ);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerEndX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerEndY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerEndZ);
    }

    @Override
    protected void loadFromModel() {
        compatiblePackages.clear();
        compatiblePackages.addAll(nozzleTip.getCompatiblePackages());
        tableModel.refresh();
        super.loadFromModel();
    }

    @Override
    protected void saveToModel() {
        nozzleTip.setCompatiblePackages(compatiblePackages);
        super.saveToModel();
    }

    public class PackagesTableModel extends AbstractTableModel {
        private String[] columnNames = new String[] {"Package Id", "Compatible?"};
        private List<org.openpnp.model.Package> packages;

        public PackagesTableModel() {
            Configuration.get().addListener(new ConfigurationListener.Adapter() {
                public void configurationComplete(Configuration configuration) throws Exception {
                    refresh();
                }
            });
        }

        public void refresh() {
            packages = new ArrayList<>(Configuration.get().getPackages());
            fireTableDataChanged();
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            return (packages == null) ? 0 : packages.size();
        }

        public org.openpnp.model.Package getPackage(int index) {
            return packages.get(index);
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 1;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            try {
                org.openpnp.model.Package pkg = packages.get(rowIndex);
                if (columnIndex == 1) {
                    if ((Boolean) aValue) {
                        compatiblePackages.add(pkg);
                    }
                    else {
                        compatiblePackages.remove(pkg);
                    }
                    notifyChange();
                }
            }
            catch (Exception e) {
                // TODO: dialog, bad input
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 1) {
                return Boolean.class;
            }
            return super.getColumnClass(columnIndex);
        }

        public Object getValueAt(int row, int col) {
            switch (col) {
                case 0:
                    return packages.get(row).getId();
                case 1:
                    return compatiblePackages.contains(packages.get(row));
                default:
                    return null;
            }
        }
    }
}
