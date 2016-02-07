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

package org.openpnp.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.openpnp.gui.components.AutoSelectTextTable;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.reticle.FootprintReticle;
import org.openpnp.gui.components.reticle.Reticle;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.tablemodel.FootprintTableModel;
import org.openpnp.model.Configuration;
import org.openpnp.model.Footprint;
import org.openpnp.model.Footprint.Pad;
import org.openpnp.model.LengthUnit;
import org.openpnp.spi.Camera;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class FootprintPanel extends JPanel {
    private final static Logger logger = LoggerFactory.getLogger(FootprintPanel.class);

    private FootprintTableModel tableModel;
    private JTable table;

    final private Footprint footprint;

    public FootprintPanel(Footprint footprint) {
        this.footprint = footprint;

        setLayout(new BorderLayout(0, 0));
        tableModel = new FootprintTableModel(footprint);

        deleteAction.setEnabled(false);

        JPanel propertiesPanel = new JPanel();
        add(propertiesPanel, BorderLayout.NORTH);
        propertiesPanel.setBorder(
                new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "Settings",
                        TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        propertiesPanel.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"),},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblUnits = new JLabel("Units");
        propertiesPanel.add(lblUnits, "2, 2, right, default");

        unitsCombo = new JComboBox(LengthUnit.values());
        propertiesPanel.add(unitsCombo, "4, 2, left, default");

        JLabel lblBodyWidth = new JLabel("Body Width");
        propertiesPanel.add(lblBodyWidth, "2, 4, right, default");

        bodyWidthTf = new JTextField();
        propertiesPanel.add(bodyWidthTf, "4, 4, left, default");
        bodyWidthTf.setColumns(10);

        JLabel lblBodyHeight = new JLabel("Body Height");
        propertiesPanel.add(lblBodyHeight, "2, 6, right, default");

        bodyHeightTf = new JTextField();
        propertiesPanel.add(bodyHeightTf, "4, 6, left, default");
        bodyHeightTf.setColumns(10);

        JPanel tablePanel = new JPanel();
        add(tablePanel, BorderLayout.CENTER);
        tablePanel.setBorder(
                new TitledBorder(null, "Pads", TitledBorder.LEADING, TitledBorder.TOP, null, null));

        table = new AutoSelectTextTable(tableModel);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }

                Pad pad = getSelectedPad();

                deleteAction.setEnabled(pad != null);
            }
        });
        tablePanel.setLayout(new BorderLayout(0, 0));

        JPanel toolbarPanel = new JPanel();
        tablePanel.add(toolbarPanel, BorderLayout.NORTH);
        toolbarPanel.setLayout(new BorderLayout(0, 0));

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolbarPanel.add(toolBar);

        JButton btnNew = toolBar.add(newAction);
        JButton btnDelete = toolBar.add(deleteAction);


        JScrollPane tableScrollPane = new JScrollPane(table);
        tablePanel.add(tableScrollPane);

        showReticle();
        initDataBindings();
    }

    protected void initDataBindings() {
        DoubleConverter doubleConverter =
                new DoubleConverter(Configuration.get().getLengthDisplayFormat());

        BeanProperty<Footprint, LengthUnit> footprintBeanProperty = BeanProperty.create("units");
        BeanProperty<JComboBox, Object> jComboBoxBeanProperty = BeanProperty.create("selectedItem");
        AutoBinding<Footprint, LengthUnit, JComboBox, Object> autoBinding =
                Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, footprint,
                        footprintBeanProperty, unitsCombo, jComboBoxBeanProperty);
        autoBinding.bind();
        //
        BeanProperty<Footprint, Double> footprintBeanProperty_1 = BeanProperty.create("bodyWidth");
        BeanProperty<JTextField, String> jTextFieldBeanProperty = BeanProperty.create("text");
        AutoBinding<Footprint, Double, JTextField, String> autoBinding_1 =
                Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, footprint,
                        footprintBeanProperty_1, bodyWidthTf, jTextFieldBeanProperty);
        autoBinding_1.setConverter(doubleConverter);
        autoBinding_1.bind();
        //
        BeanProperty<Footprint, Double> footprintBeanProperty_2 = BeanProperty.create("bodyHeight");
        BeanProperty<JTextField, String> jTextFieldBeanProperty_1 = BeanProperty.create("text");
        AutoBinding<Footprint, Double, JTextField, String> autoBinding_2 =
                Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, footprint,
                        footprintBeanProperty_2, bodyHeightTf, jTextFieldBeanProperty_1);
        autoBinding_2.setConverter(doubleConverter);
        autoBinding_2.bind();

        ComponentDecorators.decorateWithAutoSelect(bodyWidthTf);
        ComponentDecorators.decorateWithAutoSelect(bodyHeightTf);
    }

    private void showReticle() {
        try {
            Camera camera = Configuration.get().getMachine().getDefaultHead().getDefaultCamera();
            CameraView cameraView = MainFrame.cameraPanel.getCameraView(camera);
            if (cameraView == null) {
                return;
            }
            cameraView.removeReticle(FootprintPanel.class.getName());
            Reticle reticle = new FootprintReticle(footprint);
            cameraView.setReticle(FootprintPanel.class.getName(), reticle);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Pad getSelectedPad() {
        int index = table.getSelectedRow();
        if (index == -1) {
            return null;
        }
        index = table.convertRowIndexToModel(index);
        return tableModel.getPad(index);
    }

    public final Action newAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.add);
            putValue(NAME, "New Part...");
            putValue(SHORT_DESCRIPTION, "Create a new part, specifying it's ID.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            String name;
            while ((name = JOptionPane.showInputDialog(getTopLevelAncestor(),
                    "Please enter a name for the new pad.")) != null) {
                Pad pad = new Pad();
                pad.setName(name);
                footprint.addPad(pad);
                tableModel.fireTableDataChanged();
                Helpers.selectLastTableRow(table);
                break;
            }
        }
    };

    public final Action deleteAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.delete);
            putValue(NAME, "Delete Part");
            putValue(SHORT_DESCRIPTION, "Delete the currently selected part.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            int ret = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                    "Are you sure you want to delete " + getSelectedPad().getName(),
                    "Delete " + getSelectedPad().getName() + "?", JOptionPane.YES_NO_OPTION);
            if (ret == JOptionPane.YES_OPTION) {
                footprint.removePad(getSelectedPad());
            }
        }
    };
    private JTextField bodyWidthTf;
    private JTextField bodyHeightTf;
    private JComboBox unitsCombo;
}
