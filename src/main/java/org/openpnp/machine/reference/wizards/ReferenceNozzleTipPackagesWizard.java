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

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
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
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.machine.reference.ReferenceNozzleTip;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.spi.HeadMountable;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.UiUtils;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.ui.CvPipelineEditor;
import org.openpnp.vision.pipeline.ui.CvPipelineEditorDialog;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class ReferenceNozzleTipPackagesWizard extends AbstractConfigurationWizard {
    private final ReferenceNozzleTip nozzleTip;
    private JPanel panelPackageCompat;
    private JCheckBox chckbxAllowIncompatiblePackages;
    private JScrollPane scrollPane;
    private JTable table;
    private PackagesTableModel tableModel;

    private Set<org.openpnp.model.Package> compatiblePackages = new HashSet<>();


    public ReferenceNozzleTipPackagesWizard(ReferenceNozzleTip nozzleTip) {
        this.nozzleTip = nozzleTip;

        panelPackageCompat = new JPanel();
        contentPanel.add(panelPackageCompat);
        panelPackageCompat.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                RowSpec.decode("fill:max(100dlu;min):grow"),}));

        chckbxAllowIncompatiblePackages = new JCheckBox("Allow Incompatible Packages?");
        panelPackageCompat.add(chckbxAllowIncompatiblePackages, "2, 2");

        scrollPane = new JScrollPane();
        panelPackageCompat.add(scrollPane, "2, 4, fill, default");

        table = new AutoSelectTextTable(tableModel = new PackagesTableModel());
        scrollPane.setViewportView(table);
        
        CellConstraints cc = new CellConstraints();
       
    }
    

    @SuppressWarnings("serial")     // Question is this allowed? This is stolen code from LocationButtonsPanel
    private Action positionToolAction = new AbstractAction("Position Tool", Icons.centerTool) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Position the tool over the bottom camera.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                HeadMountable nozzle = nozzleTip.getParentNozzle();
                Camera camera = VisionUtils.getBottomVisionCamera();
                Location location = camera.getLocation();

                MovableUtils.moveToLocationAtSafeZ(nozzle, location);
            });
        }
    };

    private void editCalibrationPipeline() throws Exception {
        CvPipeline pipeline = nozzleTip.getCalibration().getPipeline();
        CvPipelineEditor editor = new CvPipelineEditor(pipeline);
        JDialog dialog = new CvPipelineEditorDialog(MainFrame.get(), "Calibration Pipeline", editor);
        dialog.setVisible(true);
    }

   
    @Override
    public void createBindings() {
        addWrappedBinding(nozzleTip, "allowIncompatiblePackages", chckbxAllowIncompatiblePackages,
                "selected");
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
