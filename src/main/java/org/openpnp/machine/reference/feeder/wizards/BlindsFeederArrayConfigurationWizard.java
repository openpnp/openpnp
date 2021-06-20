/*
 * Copyright (C) 2019-2021 <mark@makr.zone>
 * based on the ReferenceStripFeederConfigurationWizard 
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 * 
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

package org.openpnp.machine.reference.feeder.wizards;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.PrintWriter;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.apache.commons.io.IOUtils;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.machine.reference.feeder.BlindsFeeder;
import org.openpnp.machine.reference.feeder.BlindsFeeder.OcrAction;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Camera;
import org.openpnp.util.OcrUtil;
import org.openpnp.util.UiUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.ui.CvPipelineEditor;
import org.pmw.tinylog.Logger;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class BlindsFeederArrayConfigurationWizard extends AbstractConfigurationWizard {
    private final BlindsFeeder feeder;

    private JTextField textFieldFiducial1X;
    private JTextField textFieldFiducial1Y;
    private JTextField textFieldFiducial2X;
    private JTextField textFieldFiducial2Y;
    private JTextField textFieldFiducial3X;
    private JTextField textFieldFiducial3Y;
    private JLabel lblNormalize;
    private JCheckBox chckbxNormalize;
    private JPanel panelLocations;
    private LocationButtonsPanel locationButtonsPanelFiducial1;
    private LocationButtonsPanel locationButtonsPanelFiducial2;
    private LocationButtonsPanel locationButtonsPanelFiducial3;
    private JLabel lblFiducial3Location;
    private JCheckBox chckbxUseVision;
    private JLabel lblUseVision;

    private JButton btnCalibrateFiducials;
    private JButton btnPipelineToAllFeeders;
    private JButton btnExtractOpenscadModel;

    private JLabel lblOcrAction;
    private JComboBox ocrAction;
    private JLabel lblOcrMargin;
    private JTextField ocrMargin;
    private JLabel lblOcrFontName;
    private JComboBox ocrFontName;
    private JLabel lblFontSizept;
    private JTextField ocrFontSizePt;
    private JLabel lblOcrTextOrientation;
    private JComboBox ocrTextOrientation;
    private JButton btnSetOcrSettings;

    private JPanel panelArray;
    private JLabel label;
    private JLabel lblGroupName;
    private JComboBox feederGroupName;

    public BlindsFeederArrayConfigurationWizard(BlindsFeeder feeder) {
        this.feeder = feeder;
        List<String> blindsFeederGroupNames = feeder.getBlindsFeederGroupNames();

        panelArray = new JPanel();
        contentPanel.add(panelArray);
        panelArray.setBorder(new TitledBorder(null, "Array", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        panelArray.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(140dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("left:default:grow"),},
                new RowSpec[] {
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        RowSpec.decode("max(16dlu;min)"),}));

        lblGroupName = new JLabel("Feeder Group Name");
        panelArray.add(lblGroupName, "2, 2, right, default");
        feederGroupName = new JComboBox(blindsFeederGroupNames.toArray());
        feederGroupName.setEditable(true);
        panelArray.add(feederGroupName, "4, 2, fill, default");

        label_1 = new JLabel(" ");
        panelArray.add(label_1, "6, 2");

        btnExtractOpenscadModel = new JButton(extract3DPrintingAction);
        panelArray.add(btnExtractOpenscadModel, "8, 2, 1, 3, right, default");


        panelLocations = new JPanel();
        contentPanel.add(panelLocations);
        panelLocations.setBorder(new TitledBorder(null, "Locations", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        panelLocations.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("left:default:grow"),},
                new RowSpec[] {
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,}));


        JLabel lblX = new JLabel("X");
        panelLocations.add(lblX, "4, 4, center, default");

        JLabel lblY = new JLabel("Y");
        panelLocations.add(lblY, "6, 4, center, default");

        JLabel lblFiducial1Location = new JLabel("Fiducial 1");
        lblFiducial1Location.setToolTipText(
                "The location of the first diamond shaped fiducial (marked by a square besides it).");
        panelLocations.add(lblFiducial1Location, "2, 6, right, default");

        textFieldFiducial1X = new JTextField();
        panelLocations.add(textFieldFiducial1X, "4, 6");
        textFieldFiducial1X.setColumns(8);

        textFieldFiducial1Y = new JTextField();
        panelLocations.add(textFieldFiducial1Y, "6, 6");
        textFieldFiducial1Y.setColumns(8);


        locationButtonsPanelFiducial1 = new LocationButtonsPanel(textFieldFiducial1X,
                textFieldFiducial1Y, null, null);
        panelLocations.add(locationButtonsPanelFiducial1, "10, 6");

        JLabel lblFiducial2Location = new JLabel("Fiducial 2");
        lblFiducial2Location.setToolTipText(
                "The location of the second diamond shaped fiducial counter-clockwise from the first.");
        panelLocations.add(lblFiducial2Location, "2, 8, right, default");

        textFieldFiducial2X = new JTextField();
        panelLocations.add(textFieldFiducial2X, "4, 8");
        textFieldFiducial2X.setColumns(8);

        textFieldFiducial2Y = new JTextField();
        panelLocations.add(textFieldFiducial2Y, "6, 8");
        textFieldFiducial2Y.setColumns(8);


        locationButtonsPanelFiducial2 = new LocationButtonsPanel(textFieldFiducial2X, 
                textFieldFiducial2Y, null, null);
        panelLocations.add(locationButtonsPanelFiducial2, "10, 8");

        lblFiducial3Location = new JLabel("Fiducial 3");
        lblFiducial3Location.setToolTipText("The location of the third diamond shaped fiducial counter-clockwise from the first.");
        panelLocations.add(lblFiducial3Location, "2, 10, right, default");

        textFieldFiducial3X = new JTextField();
        textFieldFiducial3X.setColumns(8);
        panelLocations.add(textFieldFiducial3X, "4, 10");

        textFieldFiducial3Y = new JTextField();
        textFieldFiducial3Y.setColumns(8);
        panelLocations.add(textFieldFiducial3Y, "6, 10");

        locationButtonsPanelFiducial3 = new LocationButtonsPanel(textFieldFiducial3X, 
                textFieldFiducial3Y, null, null);
        panelLocations.add(locationButtonsPanelFiducial3, "10, 10");

        lblNormalize = new JLabel("Normalize");
        lblNormalize.setToolTipText("<html>\r\nNormalize the fiducial distances and shear to the theoretically correct <br />\r\nvalues (whole millimeter square grid). This means you trust the mechanics  <br />\r\nof your machine and of your 3D printer over the computer vision fiducial fixes.  <br />\r\nOverall absolute position and angle are still determined by vision. \r\n</html>");
        panelLocations.add(lblNormalize, "2, 12, right, default");

        chckbxNormalize = new JCheckBox("");
        chckbxNormalize.setToolTipText("");
        panelLocations.add(chckbxNormalize, "4, 12");

        btnCalibrateFiducials = new JButton(calibrateFiducialsAction);
        panelLocations.add(btnCalibrateFiducials, "10, 12");

        JPanel panelVision = new JPanel();
        panelVision.setBorder(new TitledBorder(null, "Vision Settings", TitledBorder.LEADING, TitledBorder.TOP,
                null, null));
        contentPanel.add(panelVision);
        panelVision.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,}));

        lblUseVision = new JLabel("Use Fiducial Vision?");
        lblUseVision.setToolTipText("<html><p>Use vision for fiducial calibration when the feeder is first used. </p>\r\n<p>Even if fiducial vision is disabled, vision will still be used for setup and <br />\r\ncover open checking</p><html>");
        panelVision.add(lblUseVision, "2, 2, right, default");

        JButton btnEditPipeline = new JButton(editPipelineAction);

        chckbxUseVision = new JCheckBox("");
        panelVision.add(chckbxUseVision, "4, 2");

        lblOcrAction = new JLabel("OCR Action");
        panelVision.add(lblOcrAction, "2, 4, right, default");

        ocrAction = new JComboBox(BlindsFeeder.OcrAction.values());
        ocrAction.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                adaptDialog();
            }
        });
        panelVision.add(ocrAction, "4, 4, fill, default");

        List<String> fontList = OcrUtil.createFontSelectionList(feeder.getOcrFontName(), true);

        label = new JLabel(" ");
        panelVision.add(label, "6, 4");

        btnSetOcrSettings = new JButton(setOcrSettingsToAllAction);
        panelVision.add(btnSetOcrSettings, "8, 4");

        lblOcrTextOrientation = new JLabel("OCR Text Orientation");
        panelVision.add(lblOcrTextOrientation, "2, 6, right, default");

        ocrTextOrientation = new JComboBox(BlindsFeeder.OcrTextOrientation.values());
        panelVision.add(ocrTextOrientation, "4, 6, fill, default");

        lblOcrMargin = new JLabel("OCR Margin");
        panelVision.add(lblOcrMargin, "6, 6, right, default");

        ocrMargin = new JTextField();
        panelVision.add(ocrMargin, "8, 6, fill, default");
        ocrMargin.setColumns(10);
        lblOcrFontName = new JLabel("OCR Font");
        panelVision.add(lblOcrFontName, "2, 8, right, default");
        ocrFontName = new JComboBox(fontList.toArray());
        lblOcrFontName.setToolTipText("<html>Name of the OCR font to be recognized.<br/>\r\nMonospace fonts work much better, allow lower resolution and therefore faster <br/>\r\noperation. Use a font where all the used characters are easily distinguishable.<br/>\r\nFonts with clear separation between glyphs are much preferred.</html>");
        panelVision.add(ocrFontName, "4, 8, fill, default");

        lblFontSizept = new JLabel("Font Size [pt]");
        panelVision.add(lblFontSizept, "6, 8, right, default");

        ocrFontSizePt = new JTextField();
        panelVision.add(ocrFontSizePt, "8, 8, fill, default");
        ocrFontSizePt.setColumns(10);

        panelVision.add(btnEditPipeline, "2, 12");

        JButton btnResetPipeline = new JButton(resetPipelineAction);
        panelVision.add(btnResetPipeline, "4, 12");

        btnPipelineToAllFeeders = new JButton(setPipelineToAllAction);
        btnPipelineToAllFeeders.setText("Set Pipeline to all");
        panelVision.add(btnPipelineToAllFeeders, "8, 12");

    }

    protected void adaptDialog() {
        BlindsFeeder.OcrAction action = (OcrAction) ocrAction.getSelectedItem();
        boolean ocrEnabled = (action != OcrAction.None);
        lblOcrMargin.setVisible(ocrEnabled);
        ocrMargin.setVisible(ocrEnabled);
        lblOcrFontName.setVisible(ocrEnabled);
        ocrFontName.setVisible(ocrEnabled);
        lblFontSizept.setVisible(ocrEnabled);
        ocrFontSizePt.setVisible(ocrEnabled);
        lblOcrTextOrientation.setVisible(ocrEnabled);
        ocrTextOrientation.setVisible(ocrEnabled);
    }

    @Override
    public void createBindings() {
        LengthConverter lengthConverter = new LengthConverter();
        //IntegerConverter intConverter = new IntegerConverter();
        DoubleConverter doubleConverter = new DoubleConverter(Configuration.get()
                .getLengthDisplayFormat());

        addWrappedBinding(feeder, "feederGroupName", feederGroupName, "selectedItem");

        MutableLocationProxy location = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "location", location, "location");
        MutableLocationProxy fiducial1Location = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "fiducial1Location", fiducial1Location, "location");
        addWrappedBinding(fiducial1Location, "lengthX", textFieldFiducial1X, "text", lengthConverter);
        addWrappedBinding(fiducial1Location, "lengthY", textFieldFiducial1Y, "text", lengthConverter);

        MutableLocationProxy fiducial2Location = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "fiducial2Location", fiducial2Location, "location");
        addWrappedBinding(fiducial2Location, "lengthX", textFieldFiducial2X, "text", lengthConverter);
        addWrappedBinding(fiducial2Location, "lengthY", textFieldFiducial2Y, "text", lengthConverter);

        MutableLocationProxy fiducial3Location = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "fiducial3Location", fiducial3Location, "location");
        addWrappedBinding(fiducial3Location, "lengthX", textFieldFiducial3X, "text", lengthConverter);
        addWrappedBinding(fiducial3Location, "lengthY", textFieldFiducial3Y, "text", lengthConverter);

        addWrappedBinding(feeder, "normalize", chckbxNormalize, "selected");

        addWrappedBinding(feeder, "visionEnabled", chckbxUseVision, "selected");
        addWrappedBinding(feeder, "ocrAction", ocrAction, "selectedItem");
        addWrappedBinding(feeder, "ocrMargin", ocrMargin, "text", lengthConverter);
        addWrappedBinding(feeder, "ocrFontName", ocrFontName, "selectedItem");
        addWrappedBinding(feeder, "ocrFontSizePt", ocrFontSizePt, "text", doubleConverter);
        addWrappedBinding(feeder, "ocrTextOrientation", ocrTextOrientation, "selectedItem");

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFiducial1X);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFiducial1Y);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFiducial2X);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFiducial2Y);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFiducial3X);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFiducial3Y);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(ocrMargin);

        adaptDialog();
    }

    private Action extract3DPrintingAction =
            new AbstractAction("Extract 3D-Printing Files", Icons.openSCadIcon) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Extract OpenSCAD files to generate models for 3D-printing the BlindsFeeders.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                JFileChooser j = new JFileChooser();
                //j.setSelectedFile(directory);
                j.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                j.setMultiSelectionEnabled(false);
                if (j.showOpenDialog(getTopLevelAncestor()) == JFileChooser.APPROVE_OPTION) {
                    File directory = j.getSelectedFile();
                    boolean opended = true;
                    for (String fileName : new String[] { "BlindsFeeder-Library.scad", "BlindsFeeder-3DPrinting.scad" }) {
                        String fileContent = IOUtils.toString(BlindsFeeder.class
                                .getResource(fileName));
                        File file = new File(directory,  fileName);
                        if (file.exists()) {
                            throw new Exception("File "+file.getAbsolutePath()+" already esists.");
                        }
                        try (PrintWriter out = new PrintWriter(file.getAbsolutePath())) {
                            out.print(fileContent);
                        }
                        try {
                            java.awt.Desktop.getDesktop().edit(file);
                        }
                        catch (Exception e1) {
                            Logger.error(e1);
                            opended = false;
                        }
                    }
                    if (! opended) {
                        JOptionPane.showMessageDialog(getTopLevelAncestor(), "<html><p>Files extracted to:</p><p>"+directory.getAbsolutePath()+"</p><p>Cannot open with OpenSCAD automatically (Desktop command failed)</p>");
                    }
                }
            });
        }
    };

    private Action calibrateFiducialsAction =
            new AbstractAction("Calibrate Fiducials") {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Calibrate the fiducials to redetermine their precise locations.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            calibrateFiducials();
        }
    };

    private Action editPipelineAction =
            new AbstractAction("Edit Pipeline") {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Edit the Pipeline to be used for all vision operations of this feeder.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                editPipeline();
            });
        }
    };

    private Action resetPipelineAction =
            new AbstractAction("Reset Pipeline") {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Reset the Pipeline for this feeder to the OpenPNP standard.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                resetPipeline();
            });
        }
    };

    private Action setOcrSettingsToAllAction =
            new AbstractAction("Set OCR Settings to all") {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Set these OCR settings to all the BlindsFeeders on the machine.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                int result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                        "This will replace the OCR settings of all the other BlindsFeeders on the machine with those of this BlindsFeeder. Are you sure?",
                        null, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (result == JOptionPane.YES_OPTION) {
                    UiUtils.messageBoxOnException(() -> {
                        setOcrSettingsToAllFeeders();
                    });
                }
            });
        }
    };

    private Action setPipelineToAllAction =
            new AbstractAction("Set Pipeline to all") {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Set this pipeline to all the BlindsFeeders on the machine.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                int result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                        "This will replace the pipeline of all the other BlindsFeeders on the machine with the pipeline of this BlindsFeeder. Are you sure?",
                        null, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (result == JOptionPane.YES_OPTION) {
                    UiUtils.messageBoxOnException(() -> {
                        setPipelineToAllFeeders();
                    });
                }
            });
        }
    };
    private JLabel label_1;

    private void calibrateFiducials() {
        UiUtils.submitUiMachineTask(() -> {
            feeder.calibrateFeederLocations();
        });
    }

    private void editPipeline() throws Exception {
        // Make sure we're editing a new common pipeline instance for all the feeders in this array.
        // The setPipeline() will make sure that all the feeders in the array have the same new clone referenced. 
        feeder.setPipeline(feeder.getPipeline().clone());
        // Prepare and edit the pipeline.
        Camera camera = Configuration.get().getMachine().getDefaultHead().getDefaultCamera();
        CvPipeline pipeline = feeder.getCvPipeline(camera, false);
        CvPipelineEditor editor = new CvPipelineEditor(pipeline);
        JDialog dialog = new JDialog(MainFrame.get(), feeder.getName() + " Pipeline");
        dialog.getContentPane().setLayout(new BorderLayout());
        dialog.getContentPane().add(editor);
        dialog.setSize(1024, 768);
        dialog.setVisible(true);
    }

    private void resetPipeline() {
        feeder.resetPipeline();
    }

    private void setOcrSettingsToAllFeeders() throws CloneNotSupportedException {
        feeder.setOcrSettingsToAllFeeders();
    }

    private void setPipelineToAllFeeders() throws CloneNotSupportedException {
        feeder.setPipelineToAllFeeders();
    }
}

