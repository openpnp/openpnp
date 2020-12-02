/*
 * Copyright (C) 2019-2020 <mark@makr.zone>
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
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.PrintWriter;

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
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.apache.commons.io.IOUtils;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.IdentifiableListCellRenderer;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.gui.support.PartsComboBoxModel;
import org.openpnp.machine.reference.feeder.BlindsFeeder;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Camera;
import org.openpnp.spi.HeadMountable;
import org.openpnp.util.UiUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.ui.CvPipelineEditor;
import org.pmw.tinylog.Logger;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class BlindsFeederConfigurationWizard extends AbstractConfigurationWizard {
    private final BlindsFeeder feeder;

    private JPanel panelPart;
    private JTextField textFieldPartZ;

    private JComboBox comboBoxPart;
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
    private JLabel lblRotationInTape;
    private JTextField textFieldLocationRotation;
    private JCheckBox chckbxUseVision;
    private JLabel lblUseVision;
    private JLabel lblPart;
    private JLabel lblRetryCount;
    private JTextField retryCountTf;
    private JLabel lblFiducial3Location;
    private JLabel lblPartTopZ;
    private JButton btnCaptureToolZ;
    private JButton btnCalibrateFiducials;
    private JButton btnPipelineToAllFeeders;
    private JButton btnExtractOpenscadModel;


    public BlindsFeederConfigurationWizard(BlindsFeeder feeder) {
        this.feeder = feeder;

        panelPart = new JPanel();
        panelPart.setBorder(new TitledBorder(null,
                "General Settings", TitledBorder.LEADING, TitledBorder.TOP, null,
                new Color(0, 0, 0)));
        contentPanel.add(panelPart);
        panelPart.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("right:default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
        try {
        }
        catch (Throwable t) {
            // Swallow this error. This happens during parsing in
            // in WindowBuilder but doesn't happen during normal run.
        }

        lblPart = new JLabel("Part");
        panelPart.add(lblPart, "2, 2, right, default");

        comboBoxPart = new JComboBox();
        comboBoxPart.setModel(new PartsComboBoxModel());
        comboBoxPart.setRenderer(new IdentifiableListCellRenderer<Part>());
        panelPart.add(comboBoxPart, "4, 2, 13, 1, left, default");

        lblRotationInTape = new JLabel("Rotation in Tape");
        lblRotationInTape.setToolTipText("<html><p>The part rotation in relation to the tape orientation. </p>\r\n<ul><li>What is 0° <strong>for the rotation of the part</strong> is determined by how the part footprint<br />\r\nis drawn in your ECAD. However look up \"Zero Component Orientation\" for the <br />\r\nstandardized way to do this. </li>\r\n<li>What is 0° <strong>for the rotation of the tape</strong> is defined in accordance to the <br />\r\nEIA-481-C \"Quadrant designations\".</li>\r\n<li>Consequently a <strong>Rotation In Tape</strong> of 0° means that the part is oriented upwards as <br />\r\ndrawn in the ECAD, when holding the tape horizontal with the sprocket holes <br/>\r\nat the top. If the tape has sprocket holes on both sides, look at the round, not <br/>\r\nthe elongated holes.</li>\r\n<li>Also consult \"EIA-481-C\" to see how parts should be oriented in the tape.</li></html>\r\n");
        panelPart.add(lblRotationInTape, "2, 4, right, default");

        textFieldLocationRotation = new JTextField();
        panelPart.add(textFieldLocationRotation, "4, 4, fill, default");
        textFieldLocationRotation.setColumns(4);

        lblPartTopZ = new JLabel("Part Z");
        lblPartTopZ.setToolTipText("Part pickup Z");
        panelPart.add(lblPartTopZ, "8, 4, right, default");

        textFieldPartZ = new JTextField();
        panelPart.add(textFieldPartZ, "10, 4, fill, default");
        textFieldPartZ.setColumns(8);

        btnCaptureToolZ = new JButton(captureToolCoordinatesAction);
        btnCaptureToolZ.setHideActionText(true);
        panelPart.add(btnCaptureToolZ, "12, 4, left, default");

        btnExtractOpenscadModel = new JButton(extract3DPrintingAction);
        panelPart.add(btnExtractOpenscadModel, "16, 4");

        lblRetryCount = new JLabel("Retry Count");
        panelPart.add(lblRetryCount, "2, 6, right, default");

        retryCountTf = new JTextField();
        panelPart.add(retryCountTf, "4, 6, fill, default");
        retryCountTf.setColumns(4);

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
                FormSpecs.DEFAULT_COLSPEC,
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
                FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblX = new JLabel("X");
        panelLocations.add(lblX, "4, 2");

        JLabel lblY = new JLabel("Y");
        panelLocations.add(lblY, "8, 2");

        JLabel lblFiducial1Location = new JLabel("Fiducial 1");
        lblFiducial1Location.setToolTipText(
                "The location of the first diamond shaped fiducial (marked by a square besides it).");
        panelLocations.add(lblFiducial1Location, "2, 4, right, default");

        textFieldFiducial1X = new JTextField();
        panelLocations.add(textFieldFiducial1X, "4, 4");
        textFieldFiducial1X.setColumns(8);

        textFieldFiducial1Y = new JTextField();
        panelLocations.add(textFieldFiducial1Y, "8, 4");
        textFieldFiducial1Y.setColumns(8);


        locationButtonsPanelFiducial1 = new LocationButtonsPanel(textFieldFiducial1X,
                textFieldFiducial1Y, null, null);
        panelLocations.add(locationButtonsPanelFiducial1, "12, 4");

        JLabel lblFiducial2Location = new JLabel("Fiducial 2");
        lblFiducial2Location.setToolTipText(
                "The location of the second diamond shaped fiducial counter-clockwise from the first.");
        panelLocations.add(lblFiducial2Location, "2, 6, right, default");

        textFieldFiducial2X = new JTextField();
        panelLocations.add(textFieldFiducial2X, "4, 6");
        textFieldFiducial2X.setColumns(8);

        textFieldFiducial2Y = new JTextField();
        panelLocations.add(textFieldFiducial2Y, "8, 6");
        textFieldFiducial2Y.setColumns(8);


        locationButtonsPanelFiducial2 = new LocationButtonsPanel(textFieldFiducial2X, 
                textFieldFiducial2Y, null, null);
        panelLocations.add(locationButtonsPanelFiducial2, "12, 6");

        lblFiducial3Location = new JLabel("Fiducial 3");
        lblFiducial3Location.setToolTipText("The location of the third diamond shaped fiducial counter-clockwise from the first.");
        panelLocations.add(lblFiducial3Location, "2, 8, right, default");

        textFieldFiducial3X = new JTextField();
        textFieldFiducial3X.setColumns(8);
        panelLocations.add(textFieldFiducial3X, "4, 8");

        textFieldFiducial3Y = new JTextField();
        textFieldFiducial3Y.setColumns(8);
        panelLocations.add(textFieldFiducial3Y, "8, 8");

        locationButtonsPanelFiducial3 = new LocationButtonsPanel(textFieldFiducial3X, 
                textFieldFiducial3Y, null, null);
        panelLocations.add(locationButtonsPanelFiducial3, "12, 8");

        lblNormalize = new JLabel("Normalize");
        lblNormalize.setToolTipText("<html>\r\nNormalize the fiducial distances and shear to the theoretically correct <br />\r\nvalues (whole millimeter square grid). This means you trust the mechanics  <br />\r\nof your machine and of your 3D printer over the computer vision fiducial fixes.  <br />\r\nOverall absolute position and angle are still determined by vision. \r\n</html>");
        panelLocations.add(lblNormalize, "2, 10, right, default");

        chckbxNormalize = new JCheckBox("");
        chckbxNormalize.setToolTipText("");
        panelLocations.add(chckbxNormalize, "4, 10");

        btnCalibrateFiducials = new JButton(calibrateFiducialsAction);
        panelLocations.add(btnCalibrateFiducials, "12, 10");

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
                ColumnSpec.decode("max(70dlu;default)"),},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));

        lblUseVision = new JLabel("Use Fiducial Vision?");
        lblUseVision.setToolTipText("<html><p>Use vision for fiducial calibration when the feeder is first used. </p>\r\n<p>Even if fiducial vision is disabled, vision will still be used for setup and <br />\r\ncover open checking</p><html>");
        panelVision.add(lblUseVision, "2, 2");

        JButton btnEditPipeline = new JButton(editPipelineAction);

        chckbxUseVision = new JCheckBox("");
        panelVision.add(chckbxUseVision, "4, 2");
        panelVision.add(btnEditPipeline, "2, 4");

        JButton btnResetPipeline = new JButton(resetPipelineAction);

        btnPipelineToAllFeeders = new JButton(setPipelineToAllAction);
        btnPipelineToAllFeeders.setText("Set Pipeline to all");
        panelVision.add(btnPipelineToAllFeeders, "4, 4");
        panelVision.add(btnResetPipeline, "6, 4");
    }

    @Override
    public void createBindings() {
        LengthConverter lengthConverter = new LengthConverter();
        IntegerConverter intConverter = new IntegerConverter();
        DoubleConverter doubleConverter = new DoubleConverter(Configuration.get()
                .getLengthDisplayFormat());

        MutableLocationProxy location = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "location", location, "location");
        addWrappedBinding(location, "rotation", textFieldLocationRotation, "text", doubleConverter);
        addWrappedBinding(location, "lengthZ", textFieldPartZ, "text", lengthConverter);

        addWrappedBinding(feeder, "part", comboBoxPart, "selectedItem");
        addWrappedBinding(feeder, "feedRetryCount", retryCountTf, "text", intConverter);

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

        addWrappedBinding(feeder, "visionEnabled", chckbxUseVision, "selected");
        addWrappedBinding(feeder, "normalize", chckbxNormalize, "selected");

        ComponentDecorators.decorateWithAutoSelect(textFieldLocationRotation);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldPartZ);
        ComponentDecorators.decorateWithAutoSelect(retryCountTf);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFiducial1X);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFiducial1Y);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFiducial2X);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFiducial2Y);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFiducial3X);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFiducial3Y);
    }

    public HeadMountable getTool() throws Exception {
        return MainFrame.get().getMachineControls().getSelectedNozzle();
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

    private Action setPipelineToAllAction =
            new AbstractAction("Set Pipeline To All") {
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

    private Action captureToolCoordinatesAction =
            new AbstractAction("Get Tool Z", Icons.captureTool) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Capture the Z height that the tool is at.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.messageBoxOnException(() -> {
                Location l = getTool().getLocation();
                Helpers.copyLocationIntoTextFields(l, null, null, textFieldPartZ, null);
            });
        }
    };

    private void calibrateFiducials() {
        UiUtils.submitUiMachineTask(() -> {
            feeder.calibrateFeederLocations();
        });
    }

    private void editPipeline() throws Exception {
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

    private void setPipelineToAllFeeders() throws CloneNotSupportedException {
        feeder.setPipelineToAllFeeders();
    }
}

