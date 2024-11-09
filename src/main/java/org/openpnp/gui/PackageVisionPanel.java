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
import java.awt.Dimension;
import java.awt.FlowLayout;
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
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.openpnp.Translations;
import org.jdesktop.beansbinding.Converter;
import org.openpnp.gui.components.AutoSelectTextTable;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.reticle.FootprintReticle;
import org.openpnp.gui.components.reticle.Reticle;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.tablemodel.FootprintTableModel;
import org.openpnp.model.Configuration;
import org.openpnp.model.Footprint;
import org.openpnp.model.Footprint.Generator;
import org.openpnp.model.Footprint.Pad;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Package;
import org.openpnp.spi.Camera;
import org.openpnp.util.UiUtils;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class PackageVisionPanel extends JPanel {
    private FootprintTableModel tableModel;
    private JTable table;

    final private Package pkg;
    final private Footprint footprint;

    public PackageVisionPanel(Package pkg) {
        this.pkg = pkg;
        this.footprint = pkg.getFootprint();

        setLayout(new BorderLayout(0, 0));
        tableModel = new FootprintTableModel(footprint, pkg);

        deleteAction.setEnabled(false);
        toggleMarkAction.setEnabled(false);

        JPanel propertiesPanel = new JPanel();
        add(propertiesPanel, BorderLayout.NORTH);
        propertiesPanel.setBorder(
                new TitledBorder(null, Translations.getString("PackageVisionPanel.SettingsPanel.Border.title"), //$NON-NLS-1$
                        TitledBorder.LEADING, TitledBorder.TOP, null));
        propertiesPanel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        RowSpec.decode("default:grow"),
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblUnits = new JLabel(Translations.getString("PackageVisionPanel.SettingsPanel.UnitsLabel.text")); //$NON-NLS-1$
        propertiesPanel.add(lblUnits, "2, 2, right, default");

        unitsCombo = new JComboBox(LengthUnit.values());
        propertiesPanel.add(unitsCombo, "4, 2, left, default");

        JLabel lblGenerate = new JLabel(Translations.getString("PackageVisionPanel.SettingsPanel.GenerateLabel.text")); //$NON-NLS-1$
        propertiesPanel.add(lblGenerate, "8, 2, right, default");

        JPanel panelGenerate = new JPanel();
        panelGenerate.setBorder(null);
        propertiesPanel.add(panelGenerate, "10, 2, 5, 1, center, fill");
        panelGenerate.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

        JButton generateDual = new JButton(generateDualAction);
        panelGenerate.add(generateDual);

        JButton generateQuad = new JButton(generateQuadAction);
        panelGenerate.add(generateQuad);

        JButton generateBga = new JButton(generateBgaAction);
        panelGenerate.add(generateBga);

        JButton generateKicad = new JButton(generateFromKicad);
        panelGenerate.add(generateKicad);

        JLabel lblBodyWidth = new JLabel(Translations.getString("PackageVisionPanel.SettingsPanel.BodyWidthLabel.text")); //$NON-NLS-1$
        propertiesPanel.add(lblBodyWidth, "2, 4, right, default");

        bodyWidthTf = new JTextField();
        propertiesPanel.add(bodyWidthTf, "4, 4, left, default");
        bodyWidthTf.setColumns(10);

        JLabel lblDimension = new JLabel(Translations.getString("PackageVisionPanel.SettingsPanel.OutsideDimensionLabel.text")); //$NON-NLS-1$
        lblDimension.setToolTipText(Translations.getString("PackageVisionPanel.SettingsPanel.OutsideDimensionLabel.toolTipText")); //$NON-NLS-1$
        propertiesPanel.add(lblDimension, "8, 4, right, default");

        outerDimension = new JTextField();
        propertiesPanel.add(outerDimension, "10, 4, fill, default");
        outerDimension.setColumns(10);

        JLabel lblInnerDim = new JLabel(Translations.getString("PackageVisionPanel.SettingsPanel.InsideDimensionLabel.text")); //$NON-NLS-1$
        lblInnerDim.setToolTipText(Translations.getString("PackageVisionPanel.SettingsPanel.InsideDimensionLabel.toolTipText")); //$NON-NLS-1$
        propertiesPanel.add(lblInnerDim, "12, 4, right, default");

        innerDimension = new JTextField();
        propertiesPanel.add(innerDimension, "14, 4, fill, default");
        innerDimension.setColumns(10);

        JLabel lblBodyHeight = new JLabel(Translations.getString("PackageVisionPanel.SettingsPanel.BodyLengthLabel.text")); //$NON-NLS-1$
        propertiesPanel.add(lblBodyHeight, "2, 6, right, default");

        bodyHeightTf = new JTextField();
        propertiesPanel.add(bodyHeightTf, "4, 6, left, default");
        bodyHeightTf.setColumns(10);

        JLabel lblPadCount = new JLabel(Translations.getString("PackageVisionPanel.SettingsPanel.PadCountLabel.text")); //$NON-NLS-1$
        lblPadCount.setToolTipText(Translations.getString("PackageVisionPanel.SettingsPanel.PadCountLabel.toolTipText")); //$NON-NLS-1$
        propertiesPanel.add(lblPadCount, "8, 6, right, default");

        padCount = new JTextField();
        propertiesPanel.add(padCount, "10, 6, fill, default");
        padCount.setColumns(10);

        JLabel lblPadPitch = new JLabel(Translations.getString("PackageVisionPanel.SettingsPanel.PadPitchLabel.text")); //$NON-NLS-1$
        propertiesPanel.add(lblPadPitch, "12, 6, right, default");

        padPitch = new JTextField();
        propertiesPanel.add(padPitch, "14, 6, fill, default");
        padPitch.setColumns(10);

        JLabel lblPadAcross = new JLabel(Translations.getString("PackageVisionPanel.SettingsPanel.PadAcrossLabel.text")); //$NON-NLS-1$
        lblPadAcross.setToolTipText(Translations.getString("PackageVisionPanel.SettingsPanel.PadAcrossLabel.toolTipText")); //$NON-NLS-1$
        propertiesPanel.add(lblPadAcross, "8, 8, right, default");

        padAcross = new JTextField();
        propertiesPanel.add(padAcross, "10, 8, fill, default");
        padAcross.setColumns(10);

        JLabel lblRound = new JLabel("% Roundness");
        lblRound.setToolTipText(Translations.getString("PackageVisionPanel.SettingsPanel.RoundnessLabel.toolTipText")); //$NON-NLS-1$
        propertiesPanel.add(lblRound, "12, 8, right, default");

        padRoundness = new JTextField();
        propertiesPanel.add(padRoundness, "14, 8, fill, default");
        padRoundness.setColumns(10);

        JPanel tablePanel = new JPanel();
        add(tablePanel, BorderLayout.CENTER);
        tablePanel.setBorder(new TitledBorder(null, Translations.getString(
                "PackageVisionPanel.PadsPanel.Border.title"), TitledBorder.LEADING, TitledBorder.TOP, null, null)); //$NON-NLS-1$

        table = new AutoSelectTextTable(tableModel);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(1).setPreferredWidth(14);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }

                Pad pad = getSelectedPad();

                deleteAction.setEnabled(pad != null);
                toggleMarkAction.setEnabled(pad != null);
            }
        });
        tablePanel.setLayout(new BorderLayout(0, 0));

        JPanel toolbarPanel = new JPanel();
        tablePanel.add(toolbarPanel, BorderLayout.NORTH);
        toolbarPanel.setLayout(new BorderLayout(0, 0));

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolbarPanel.add(toolBar);

        toolBar.add(newAction);
        toolBar.add(deleteAction);
        toolBar.add(toggleMarkAction);

        JScrollPane tableScrollPane = new JScrollPane(table);
        tableScrollPane.setPreferredSize(new Dimension(454, 100));
        tablePanel.add(tableScrollPane);

        showReticle();
        initDataBindings();

        ComponentDecorators.decorateWithAutoSelect(bodyWidthTf);
        ComponentDecorators.decorateWithAutoSelect(bodyHeightTf);

        ComponentDecorators.decorateWithAutoSelect(outerDimension);
        ComponentDecorators.decorateWithAutoSelect(innerDimension);
        ComponentDecorators.decorateWithAutoSelect(padCount);
        ComponentDecorators.decorateWithAutoSelect(padPitch);
        ComponentDecorators.decorateWithAutoSelect(padAcross);
        ComponentDecorators.decorateWithAutoSelect(padRoundness);
    }

    private void showReticle() {
        try {
            Camera camera = Configuration.get().getMachine().getDefaultHead().getDefaultCamera();
            CameraView cameraView = MainFrame.get().getCameraViews().getCameraView(camera);
            if (cameraView == null) {
                return;
            }
            cameraView.removeReticle(PackageVisionPanel.class.getName());
            Reticle reticle = new FootprintReticle(footprint);
            cameraView.setReticle(PackageVisionPanel.class.getName(), reticle);
        }
        catch (Exception e) {
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
            putValue(NAME, Translations.getString("PackageVisionPanel.PadsPanel.Action.NewPad")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString(
                    "PackageVisionPanel.PadsPanel.Action.NewPad.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            String name;
            while ((name = JOptionPane.showInputDialog(getTopLevelAncestor(),
                    "Please enter a name for the new pad.")) != null) {
                name = name.trim();
                if (name.isEmpty()) {
                    break;
                }
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
            putValue(NAME, Translations.getString("PackageVisionPanel.PadsPanel.Action.DeletePad")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString(
                    "PackageVisionPanel.PadsPanel.Action.DeletePad.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            int ret = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                    Translations.getString("DialogMessages.ConfirmDelete.text") //$NON-NLS-1$
                            + " " + getSelectedPad().getName() + "?", //$NON-NLS-1$ //$NON-NLS-2$
                    Translations.getString("DialogMessages.ConfirmDelete.title") //$NON-NLS-1$ //$NON-NLS-2$
                            + " " + getSelectedPad().getName() + "?", JOptionPane.YES_NO_OPTION); //$NON-NLS-1$ //$NON-NLS-2$
            if (ret == JOptionPane.YES_OPTION) {
                footprint.removePad(getSelectedPad());
                tableModel.fireTableDataChanged();
            }
        }
    };

    public final Action toggleMarkAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.footprintToggle);
            putValue(NAME, Translations.getString("PackageVisionPanel.PadsPanel.Action.ToggleMark")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString(
                    "PackageVisionPanel.PadsPanel.Action.ToggleMark.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            footprint.toggleMark(getSelectedPad());
            tableModel.fireTableDataChanged();
        }
    };

    public final Action generateDualAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.footprintDual);
            putValue(SHORT_DESCRIPTION, "Generate a Dual form factor package.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            generatePads(Generator.Dual);
        }
    };

    public final Action generateQuadAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.footprintQuad);
            putValue(SHORT_DESCRIPTION, "Generate a square Quad form factor package.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            generatePads(Generator.Quad);
        }
    };

    public final Action generateBgaAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.footprintBga);
            putValue(SHORT_DESCRIPTION, "Generate a square BGA package.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            generatePads(Generator.Bga);
        }
    };

    public final Action generateFromKicad = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.kicad);
            putValue(SHORT_DESCRIPTION, "Import a footprint from KiCad module.");
        }
        @Override
        public void actionPerformed(ActionEvent arg0) {
            generatePads(Generator.Kicad);
        }
    };

    protected void generatePads(Footprint.Generator type) {
        UiUtils.messageBoxOnException(() -> {
            try{
                if (!footprint.getPads().isEmpty()) {
                    int ret = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                            "Are you sure you want to delete all existing pads?",
                            "Delete all pads?", JOptionPane.YES_NO_OPTION);
                    if (ret != JOptionPane.YES_OPTION) {
                        return; // Abort.
                    }
                    footprint.removeAllPads();
                }
                footprint.generate(type);
            }
            finally {
                tableModel.fireTableDataChanged();
                pkg.fireFootprintChanged();
            }
        });
    }


    private JTextField bodyWidthTf;
    private JTextField bodyHeightTf;
    private JComboBox unitsCombo;
    private JTextField padCount;
    private JTextField padAcross;
    private JTextField padPitch;
    private JTextField outerDimension;
    private JTextField innerDimension;
    private JTextField padRoundness;

    protected void initDataBindings() {
        Converter doubleConverter = new DoubleConverter(Configuration.get().getLengthDisplayFormat());
        Converter intConverter = new IntegerConverter();

        BeanProperty<Footprint, LengthUnit> footprintBeanProperty = BeanProperty.create("units");
        BeanProperty<JComboBox, Object> jComboBoxBeanProperty = BeanProperty.create("selectedItem");
        AutoBinding<Footprint, LengthUnit, JComboBox, Object> autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, footprint, footprintBeanProperty, unitsCombo, jComboBoxBeanProperty);
        autoBinding.bind();
        //
        BeanProperty<Footprint, Double> footprintBeanProperty_1 = BeanProperty.create("bodyWidth");
        BeanProperty<JTextField, String> jTextFieldBeanProperty = BeanProperty.create("text");
        AutoBinding<Footprint, Double, JTextField, String> autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, footprint, footprintBeanProperty_1, bodyWidthTf, jTextFieldBeanProperty);
        autoBinding_1.setConverter(doubleConverter);
        autoBinding_1.bind();
        //
        BeanProperty<Footprint, Double> footprintBeanProperty_2 = BeanProperty.create("bodyHeight");
        BeanProperty<JTextField, String> jTextFieldBeanProperty_1 = BeanProperty.create("text");
        AutoBinding<Footprint, Double, JTextField, String> autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, footprint, footprintBeanProperty_2, bodyHeightTf, jTextFieldBeanProperty_1);
        autoBinding_2.setConverter(doubleConverter);
        autoBinding_2.bind();
        //
        BeanProperty<Footprint, Double> footprintBeanProperty_3 = BeanProperty.create("outerDimension");
        BeanProperty<JTextField, String> jTextFieldBeanProperty_2 = BeanProperty.create("text");
        AutoBinding<Footprint, Double, JTextField, String> autoBinding_3 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, footprint, footprintBeanProperty_3, outerDimension, jTextFieldBeanProperty_2);
        autoBinding_3.setConverter(doubleConverter);
        autoBinding_3.bind();
        //
        BeanProperty<Footprint, Double> footprintBeanProperty_4 = BeanProperty.create("innerDimension");
        BeanProperty<JTextField, String> jTextFieldBeanProperty_3 = BeanProperty.create("text");
        AutoBinding<Footprint, Double, JTextField, String> autoBinding_4 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, footprint, footprintBeanProperty_4, innerDimension, jTextFieldBeanProperty_3);
        autoBinding_4.setConverter(doubleConverter);
        autoBinding_4.bind();
        //
        BeanProperty<Footprint, Integer> footprintBeanProperty_5 = BeanProperty.create("padCount");
        BeanProperty<JTextField, String> jTextFieldBeanProperty_4 = BeanProperty.create("text");
        AutoBinding<Footprint, Integer, JTextField, String> autoBinding_5 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, footprint, footprintBeanProperty_5, padCount, jTextFieldBeanProperty_4);
        autoBinding_5.setConverter(intConverter);
        autoBinding_5.bind();
        //
        BeanProperty<Footprint, Double> footprintBeanProperty_6 = BeanProperty.create("padPitch");
        BeanProperty<JTextField, String> jTextFieldBeanProperty_5 = BeanProperty.create("text");
        AutoBinding<Footprint, Double, JTextField, String> autoBinding_6 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, footprint, footprintBeanProperty_6, padPitch, jTextFieldBeanProperty_5);
        autoBinding_6.setConverter(doubleConverter);
        autoBinding_6.bind();
        //
        BeanProperty<Footprint, Double> footprintBeanProperty_7 = BeanProperty.create("padAcross");
        BeanProperty<JTextField, String> jTextFieldBeanProperty_6 = BeanProperty.create("text");
        AutoBinding<Footprint, Double, JTextField, String> autoBinding_7 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, footprint, footprintBeanProperty_7, padAcross, jTextFieldBeanProperty_6);
        autoBinding_7.setConverter(doubleConverter);
        autoBinding_7.bind();
        //
        BeanProperty<Footprint, Double> footprintBeanProperty_8 = BeanProperty.create("padRoundness");
        BeanProperty<JTextField, String> jTextFieldBeanProperty_7 = BeanProperty.create("text");
        AutoBinding<Footprint, Double, JTextField, String> autoBinding_8 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, footprint, footprintBeanProperty_8, padRoundness, jTextFieldBeanProperty_7);
        autoBinding_8.setConverter(doubleConverter);
        autoBinding_8.bind();

    }
}
