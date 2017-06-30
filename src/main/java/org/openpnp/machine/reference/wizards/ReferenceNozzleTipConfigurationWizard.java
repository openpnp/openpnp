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
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IntegerConverter;
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
import javax.swing.SwingConstants;

public class ReferenceNozzleTipConfigurationWizard extends AbstractConfigurationWizard {
    private final ReferenceNozzleTip nozzleTip;
    private JPanel panelChanger;
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
    private JButton btnReset;
    private JLabel lblEnabled;
    private JCheckBox calibrationEnabledCheckbox;
    private JLabel lblMiddleLocation_1;
    private JTextField textFieldMidX2;
    private JTextField textFieldMidY2;
    private JTextField textFieldMidZ2;
    private LocationButtonsPanel changerMidButtons2;
    private JPanel panelVacuumSensing;
    private JLabel lblPartOnNozzle;
    private JLabel lblPartOffNozzle;
    private JTextField vacuumLevelPartOn;
    private JTextField vacuumLevelPartOff;
    private JPanel panel;
    private JLabel lblName;
    private JTextField nameTf;
    private JTextField textFieldChangerStartToMidSpeed;
    private JTextField textFieldChangerMidToMid2Speed;
    private JTextField textFieldChangerMid2ToEndSpeed;
    private JLabel lblSpeed;
    private JLabel lblX;
    private JLabel lblSpeed1_2;
    private JLabel lblSpeed2_3;
    private JLabel lblSpeed3_4;


    public ReferenceNozzleTipConfigurationWizard(ReferenceNozzleTip nozzleTip) {
        this.nozzleTip = nozzleTip;
        
        panel = new JPanel();
        panel.setBorder(new TitledBorder(null, "Properties", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panel);
        panel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        lblName = new JLabel("Name");
        panel.add(lblName, "2, 2, right, default");
        
        nameTf = new JTextField();
        panel.add(nameTf, "4, 2, fill, default");
        nameTf.setColumns(10);

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
        panelChanger.setLayout(new FormLayout(new ColumnSpec[] {
        		FormSpecs.RELATED_GAP_COLSPEC,
        		FormSpecs.DEFAULT_COLSPEC,
        		FormSpecs.RELATED_GAP_COLSPEC,
        		FormSpecs.DEFAULT_COLSPEC,
        		FormSpecs.RELATED_GAP_COLSPEC,
        		FormSpecs.DEFAULT_COLSPEC,
        		FormSpecs.RELATED_GAP_COLSPEC,
        		FormSpecs.DEFAULT_COLSPEC,
        		FormSpecs.RELATED_GAP_COLSPEC,
        		FormSpecs.DEFAULT_COLSPEC,
        		FormSpecs.RELATED_GAP_COLSPEC,
        		FormSpecs.DEFAULT_COLSPEC,},
        	new RowSpec[] {
        		FormSpecs.RELATED_GAP_ROWSPEC,
        		FormSpecs.DEFAULT_ROWSPEC,
        		FormSpecs.RELATED_GAP_ROWSPEC,
        		FormSpecs.DEFAULT_ROWSPEC,
        		FormSpecs.DEFAULT_ROWSPEC,
        		FormSpecs.DEFAULT_ROWSPEC,
        		FormSpecs.DEFAULT_ROWSPEC,
        		FormSpecs.DEFAULT_ROWSPEC,
        		FormSpecs.DEFAULT_ROWSPEC,
        		FormSpecs.DEFAULT_ROWSPEC,}));
        
        lblX = new JLabel("X");
        panelChanger.add(lblX, "4, 2");

        lblY_1 = new JLabel("Y");
        panelChanger.add(lblY_1, "6, 2");

        lblZ_1 = new JLabel("Z");
        panelChanger.add(lblZ_1, "8, 2");
        
        lblSpeed = new JLabel("Speed");
        panelChanger.add(lblSpeed, "10, 2");
        
        lblSpeed1_2 = new JLabel("1 ↔ 2");
        panelChanger.add(lblSpeed1_2, "8, 5, right, default");
        
        lblSpeed2_3 = new JLabel("2 ↔ 3");
        panelChanger.add(lblSpeed2_3, "8, 7, right, default");
        
        lblSpeed3_4 = new JLabel("3 ↔ 4");
        panelChanger.add(lblSpeed3_4, "8, 9, right, default");
        
        lblStartLocation = new JLabel("First Location");
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
        
        textFieldChangerStartToMidSpeed = new JTextField();
        textFieldChangerStartToMidSpeed.setToolTipText("Speed between First location and Second location");
        panelChanger.add(textFieldChangerStartToMidSpeed, "10, 5, fill, default");
        textFieldChangerStartToMidSpeed.setColumns(5);

        changerStartLocationButtonsPanel = new LocationButtonsPanel(textFieldChangerStartX,
                textFieldChangerStartY, textFieldChangerStartZ, (JTextField) null);
        changerStartLocationButtonsPanel.setShowPositionToolNoSafeZ(true);
        panelChanger.add(changerStartLocationButtonsPanel, "12, 4, fill, default");

        lblMiddleLocation = new JLabel("Second Location");
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
        
        textFieldChangerMidToMid2Speed = new JTextField();
        textFieldChangerMidToMid2Speed.setToolTipText("Speed between Second location and Third location");
        textFieldChangerMidToMid2Speed.setColumns(5);
        panelChanger.add(textFieldChangerMidToMid2Speed, "10, 7, fill, default");

        changerMidLocationButtonsPanel = new LocationButtonsPanel(textFieldChangerMidX,
                textFieldChangerMidY, textFieldChangerMidZ, (JTextField) null);
        changerMidLocationButtonsPanel.setShowPositionToolNoSafeZ(true);
        panelChanger.add(changerMidLocationButtonsPanel, "12, 6, fill, default");
        
        lblMiddleLocation_1 = new JLabel("Third Location");
        panelChanger.add(lblMiddleLocation_1, "2, 8, right, default");
        
        textFieldMidX2 = new JTextField();
        textFieldMidX2.setColumns(5);
        panelChanger.add(textFieldMidX2, "4, 8, fill, default");
        
        textFieldMidY2 = new JTextField();
        textFieldMidY2.setColumns(5);
        panelChanger.add(textFieldMidY2, "6, 8, fill, default");
        
        textFieldMidZ2 = new JTextField();
        textFieldMidZ2.setColumns(5);
        panelChanger.add(textFieldMidZ2, "8, 8, fill, default");
        
        textFieldChangerMid2ToEndSpeed = new JTextField();
        textFieldChangerMid2ToEndSpeed.setToolTipText("Speed between Third location and Last location");
        textFieldChangerMid2ToEndSpeed.setColumns(5);
        panelChanger.add(textFieldChangerMid2ToEndSpeed, "10, 9, fill, default");
        
        changerMidButtons2 = new LocationButtonsPanel(textFieldMidX2, textFieldMidY2, textFieldMidZ2, (JTextField) null);
        changerMidButtons2.setShowPositionToolNoSafeZ(true);
        panelChanger.add(changerMidButtons2, "12, 8, fill, default");

        lblEndLocation = new JLabel("Last Location");
        panelChanger.add(lblEndLocation, "2, 10, right, default");

        textFieldChangerEndX = new JTextField();
        panelChanger.add(textFieldChangerEndX, "4, 10, fill, default");
        textFieldChangerEndX.setColumns(5);

        textFieldChangerEndY = new JTextField();
        panelChanger.add(textFieldChangerEndY, "6, 10, fill, default");
        textFieldChangerEndY.setColumns(5);

        textFieldChangerEndZ = new JTextField();
        panelChanger.add(textFieldChangerEndZ, "8, 10, fill, default");
        textFieldChangerEndZ.setColumns(5);

        changerEndLocationButtonsPanel = new LocationButtonsPanel(textFieldChangerEndX,
                textFieldChangerEndY, textFieldChangerEndZ, (JTextField) null);
        changerEndLocationButtonsPanel.setShowPositionToolNoSafeZ(true);
        panelChanger.add(changerEndLocationButtonsPanel, "12, 10, fill, default");
        
        panelVacuumSensing = new JPanel();
        panelVacuumSensing.setBorder(new TitledBorder(null, "Vacuum Sensing", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelVacuumSensing);
        panelVacuumSensing.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        lblPartOnNozzle = new JLabel("Part On Nozzle Vacuum Value");
        panelVacuumSensing.add(lblPartOnNozzle, "2, 2, right, default");
        
        vacuumLevelPartOn = new JTextField();
        panelVacuumSensing.add(vacuumLevelPartOn, "4, 2");
        vacuumLevelPartOn.setColumns(10);
        
        lblPartOffNozzle = new JLabel("Part Off Nozzle Vacuum Value");
        panelVacuumSensing.add(lblPartOffNozzle, "2, 4, right, default");
        
        vacuumLevelPartOff = new JTextField();
        panelVacuumSensing.add(vacuumLevelPartOff, "4, 4");
        vacuumLevelPartOff.setColumns(10);

        panelCalibration = new JPanel();
        panelCalibration.setBorder(new TitledBorder(null, "Calibration", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        // TODO: Removing panel until this feature is actually working.
        // See: https://github.com/openpnp/openpnp/issues/235
//        contentPanel.add(panelCalibration);
        panelCalibration.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        RowSpec.decode("23px"), FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,}));

        lblEnabled = new JLabel("Enabled?");
        panelCalibration.add(lblEnabled, "2, 2, right, default");

        calibrationEnabledCheckbox = new JCheckBox("");
        panelCalibration.add(calibrationEnabledCheckbox, "3, 2, left, default");

        btnCalibrate = new JButton("Calibrate");
        btnCalibrate.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                calibrate();
            }
        });
        panelCalibration.add(btnCalibrate, "3, 3");

        btnReset = new JButton("Reset");
        btnReset.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                nozzleTip.getCalibration().reset();
            }
        });
        panelCalibration.add(btnReset, "3, 5");

        btnEditPipeline = new JButton("Edit Pipeline");
        btnEditPipeline.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                UiUtils.messageBoxOnException(() -> {
                    editCalibrationPipeline();
                });
            }
        });
        panelCalibration.add(btnEditPipeline, "3, 7, left, top");
    }

    private void editCalibrationPipeline() throws Exception {
        CvPipeline pipeline = nozzleTip.getCalibration().getPipeline();
        CvPipelineEditor editor = new CvPipelineEditor(pipeline);
        JDialog dialog = new JDialog(MainFrame.get(), "Calibration Pipeline");
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
        IntegerConverter intConverter = new IntegerConverter();
        DoubleConverter doubleConverter = new DoubleConverter(Configuration.get().getLengthDisplayFormat());

        addWrappedBinding(nozzleTip, "name", nameTf, "text");
        
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
        addWrappedBinding(nozzleTip, "changerStartToMidSpeed", textFieldChangerStartToMidSpeed, "text",
                doubleConverter);

        MutableLocationProxy changerMidLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, nozzleTip, "changerMidLocation", changerMidLocation,
                "location");
        addWrappedBinding(changerMidLocation, "lengthX", textFieldChangerMidX, "text",
                lengthConverter);
        addWrappedBinding(changerMidLocation, "lengthY", textFieldChangerMidY, "text",
                lengthConverter);
        addWrappedBinding(changerMidLocation, "lengthZ", textFieldChangerMidZ, "text",
                lengthConverter);
        addWrappedBinding(nozzleTip, "changerMidToMid2Speed", textFieldChangerMidToMid2Speed, "text",
                doubleConverter);

        MutableLocationProxy changerMidLocation2 = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, nozzleTip, "changerMidLocation2", changerMidLocation2,
                "location");
        addWrappedBinding(changerMidLocation2, "lengthX", textFieldMidX2, "text",
                lengthConverter);
        addWrappedBinding(changerMidLocation2, "lengthY", textFieldMidY2, "text",
                lengthConverter);
        addWrappedBinding(changerMidLocation2, "lengthZ", textFieldMidZ2, "text",
                lengthConverter);
        addWrappedBinding(nozzleTip, "changerMid2ToEndSpeed", textFieldChangerMid2ToEndSpeed, "text",
                doubleConverter);

        MutableLocationProxy changerEndLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, nozzleTip, "changerEndLocation", changerEndLocation,
                "location");
        addWrappedBinding(changerEndLocation, "lengthX", textFieldChangerEndX, "text",
                lengthConverter);
        addWrappedBinding(changerEndLocation, "lengthY", textFieldChangerEndY, "text",
                lengthConverter);
        addWrappedBinding(changerEndLocation, "lengthZ", textFieldChangerEndZ, "text",
                lengthConverter);
        
        addWrappedBinding(nozzleTip.getCalibration(), "enabled", calibrationEnabledCheckbox, "selected");
        
        addWrappedBinding(nozzleTip, "vacuumLevelPartOn", vacuumLevelPartOn, "text", doubleConverter);
        addWrappedBinding(nozzleTip, "vacuumLevelPartOff", vacuumLevelPartOff, "text", doubleConverter);

        ComponentDecorators.decorateWithAutoSelect(nameTf);
        
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerStartX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerStartY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerStartZ);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerStartToMidSpeed);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerMidX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerMidY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerMidZ);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerMidToMid2Speed);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldMidX2);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldMidY2);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldMidZ2);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerMid2ToEndSpeed);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerEndX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerEndY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerEndZ);
           
        ComponentDecorators.decorateWithAutoSelect(vacuumLevelPartOn);
        ComponentDecorators.decorateWithAutoSelect(vacuumLevelPartOff);
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
