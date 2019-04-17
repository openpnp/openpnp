/*
 * Copyright (C) 2019 <mark@makr.zone>
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
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.components.CameraViewActionEvent;
import org.openpnp.gui.components.CameraViewActionListener;
import org.openpnp.gui.components.CameraViewFilter;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.IdentifiableListCellRenderer;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.gui.support.PartsComboBoxModel;
import org.openpnp.machine.reference.camera.BufferedImageCamera;
import org.openpnp.machine.reference.feeder.BlindsFeeder;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Camera;
import org.openpnp.util.HslColor;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.util.UiUtils;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.FluentCv;
import org.openpnp.vision.Ransac;
import org.openpnp.vision.Ransac.Line;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.ui.CvPipelineEditor;
import org.pmw.tinylog.Logger;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class BlindsFeederConfigurationWizard extends AbstractConfigurationWizard {
    private final BlindsFeeder feeder;

    private JPanel panelPart;

    private JComboBox comboBoxPart;
    private JTextField textFieldFiducial1X;
    private JTextField textFieldFiducial1Y;
    private JTextField textFieldFiducial1Z;
    private JTextField textFieldFiducial2X;
    private JTextField textFieldFiducial2Y;
    private JTextField textFieldFiducial2Z;
    private JTextField textFieldFiducial3X;
    private JTextField textFieldFiducial3Y;
    private JTextField textFieldFiducial3Z;
    private JLabel lblFeederNo;
    private JTextField textFieldFeederNo;
    private JLabel lblFeedersTotal;
    private JTextField textFieldFeedersTotal;
    private JLabel lblNormalize;
    private JCheckBox chckbxNormalize;
    private JLabel lblPocketPitch;
    private JTextField textFieldPocketPitch;
    private JPanel panelTapeSettings;
    private JPanel panelLocations;
    private LocationButtonsPanel locationButtonsPanelFiducial1;
    private LocationButtonsPanel locationButtonsPanelFiducial2;
    private LocationButtonsPanel locationButtonsPanelFiducial3;
    private JLabel lblFeedCount;
    private JTextField textFieldFeedCount;
    private JButton btnResetFeedCount;
    private JLabel lblRotationInTape;
    private JTextField textFieldLocationRotation;
    private JButton btnAutoSetup;
    private JCheckBox chckbxUseVision;
    private JLabel lblUseVision;
    private JLabel lblPart;
    private JLabel lblRetryCount;
    private JTextField retryCountTf;
    private JLabel lblFiducial3Location;
    private JLabel lblTapeLength;
    private JTextField textFieldTapeLength;
    private JLabel lblPartSize;
    private JTextField textFieldPocketSize;
    private JLabel lblPocketCenterline;
    private JTextField textFieldPocketCenterline;

    private boolean logDebugInfo = false;
    private Camera autoSetupCamera;


    public BlindsFeederConfigurationWizard(BlindsFeeder feeder) {
        this.feeder = feeder;

        panelPart = new JPanel();
        panelPart.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null),
                "General Settings", TitledBorder.LEADING, TitledBorder.TOP, null,
                new Color(0, 0, 0)));
        contentPanel.add(panelPart);
        panelPart.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));
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
        panelPart.add(comboBoxPart, "4, 2, left, default");

        lblRotationInTape = new JLabel("Rotation In Tape");
        panelPart.add(lblRotationInTape, "2, 4, left, default");

        textFieldLocationRotation = new JTextField();
        panelPart.add(textFieldLocationRotation, "4, 4, fill, default");
        textFieldLocationRotation.setColumns(4);

        lblRetryCount = new JLabel("Retry Count");
        panelPart.add(lblRetryCount, "2, 6, right, default");

        retryCountTf = new JTextField();
        retryCountTf.setText("3");
        panelPart.add(retryCountTf, "4, 6, fill, default");
        retryCountTf.setColumns(3);

        panelTapeSettings = new JPanel();
        contentPanel.add(panelTapeSettings);
        panelTapeSettings.setBorder(new TitledBorder(
                new EtchedBorder(EtchedBorder.LOWERED, null, null), "Tape Settings",
                TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        panelTapeSettings.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
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
                        FormSpecs.DEFAULT_ROWSPEC,}));

        btnAutoSetup = new JButton(autoSetup);
        panelTapeSettings.add(btnAutoSetup, "2, 2, 11, 1");

        lblTapeLength = new JLabel("Tape Length");
        lblTapeLength.setToolTipText("Length of the tape.");
        panelTapeSettings.add(lblTapeLength, "2, 4, right, default");

        textFieldTapeLength = new JTextField();
        textFieldTapeLength.setEditable(false);
        textFieldTapeLength.setColumns(10);
        panelTapeSettings.add(textFieldTapeLength, "4, 4");

        lblFeederExtent = new JLabel("Feeder Extent");
        lblFeederExtent.setToolTipText("Total feeder holder extent (all tapes).");
        panelTapeSettings.add(lblFeederExtent, "8, 4, right, default");

        textFieldFeederExtent = new JTextField();
        textFieldFeederExtent.setEditable(false);
        textFieldFeederExtent.setText("");
        panelTapeSettings.add(textFieldFeederExtent, "10, 4");
        textFieldFeederExtent.setColumns(10);

        lblPocketPitch = new JLabel("Pocket Pitch");
        lblPocketPitch.setToolTipText("Picth of the part pockets in the tape.");
        panelTapeSettings.add(lblPocketPitch, "2, 6, right, default");

        textFieldPocketPitch = new JTextField();
        panelTapeSettings.add(textFieldPocketPitch, "4, 6");
        textFieldPocketPitch.setColumns(5);

        lblPartSize = new JLabel("Pocket Size");
        lblPartSize.setToolTipText("Size of the pocket across the tape.");
        panelTapeSettings.add(lblPartSize, "8, 6, right, default");

        textFieldPocketSize = new JTextField();
        textFieldPocketSize.setColumns(5);
        panelTapeSettings.add(textFieldPocketSize, "10, 6");

        lblPocketCenterline = new JLabel("Pocket Centerline");
        lblPocketCenterline.setToolTipText("Centerline of the pockets i.e. the perpendicular distance between the tape pocket centerline and fiducial 1.");
        panelTapeSettings.add(lblPocketCenterline, "2, 8, right, default");

        textFieldPocketCenterline = new JTextField();
        panelTapeSettings.add(textFieldPocketCenterline, "4, 8");
        textFieldPocketCenterline.setColumns(10);

        lblFeedCount = new JLabel("Feed Count");
        panelTapeSettings.add(lblFeedCount, "8, 8, right, default");

        textFieldFeedCount = new JTextField();
        panelTapeSettings.add(textFieldFeedCount, "10, 8");
        textFieldFeedCount.setColumns(10);

        btnResetFeedCount = new JButton(new AbstractAction("Reset") {
            @Override
            public void actionPerformed(ActionEvent e) {
                textFieldFeedCount.setText("0");
                applyAction.actionPerformed(e);
            }
        });
        panelTapeSettings.add(btnResetFeedCount, "12, 8");

        JPanel panelVision = new JPanel();
        panelVision.setBorder(new TitledBorder(null, "Vision", TitledBorder.LEADING, TitledBorder.TOP,
                null, null));
        contentPanel.add(panelVision);
        panelVision.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        lblUseVision = new JLabel("Use Vision?");
        panelVision.add(lblUseVision, "2, 2");

        chckbxUseVision = new JCheckBox("");
        panelVision.add(chckbxUseVision, "4, 2");

        JButton btnEditPipeline = new JButton("Edit Pipeline");
        btnEditPipeline.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                UiUtils.messageBoxOnException(() -> {
                    editPipeline();
                });
            }
        });
        panelVision.add(btnEditPipeline, "2, 4");

        JButton btnResetPipeline = new JButton("Reset Pipeline");
        btnResetPipeline.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                resetPipeline();
            }
        });
        panelVision.add(btnResetPipeline, "4, 4");

        panelLocations = new JPanel();
        contentPanel.add(panelLocations);
        panelLocations.setBorder(new TitledBorder(null, "Locations", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        panelLocations.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
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
        panelLocations.add(lblX, "4, 2");

        JLabel lblY = new JLabel("Y");
        panelLocations.add(lblY, "6, 2");

        JLabel lblZ_1 = new JLabel("Z");
        panelLocations.add(lblZ_1, "8, 2");

        JLabel lblFiducial1Location = new JLabel("Fiducial 1");
        lblFiducial1Location.setToolTipText(
                "The location of the first diamond shaped fiducial (marked by a square besides it).");
        panelLocations.add(lblFiducial1Location, "2, 4, right, default");

        textFieldFiducial1X = new JTextField();
        panelLocations.add(textFieldFiducial1X, "4, 4");
        textFieldFiducial1X.setColumns(8);

        textFieldFiducial1Y = new JTextField();
        panelLocations.add(textFieldFiducial1Y, "6, 4");
        textFieldFiducial1Y.setColumns(8);

        textFieldFiducial1Z = new JTextField();
        panelLocations.add(textFieldFiducial1Z, "8, 4");
        textFieldFiducial1Z.setColumns(8);

        locationButtonsPanelFiducial1 = new LocationButtonsPanel(textFieldFiducial1X,
                textFieldFiducial1Y, textFieldFiducial1Z, null);
        panelLocations.add(locationButtonsPanelFiducial1, "10, 4");

        JLabel lblFiducial2Location = new JLabel("Fiducial 2");
        lblFiducial2Location.setToolTipText(
                "The location of the second diamond shaped fiducial counter-clockwise from the first.");
        panelLocations.add(lblFiducial2Location, "2, 6, right, default");

        textFieldFiducial2X = new JTextField();
        panelLocations.add(textFieldFiducial2X, "4, 6");
        textFieldFiducial2X.setColumns(8);

        textFieldFiducial2Y = new JTextField();
        panelLocations.add(textFieldFiducial2Y, "6, 6");
        textFieldFiducial2Y.setColumns(8);

        textFieldFiducial2Z = new JTextField();
        panelLocations.add(textFieldFiducial2Z, "8, 6");
        textFieldFiducial2Z.setColumns(8);

        locationButtonsPanelFiducial2 = new LocationButtonsPanel(textFieldFiducial2X, textFieldFiducial2Y,
                textFieldFiducial2Z, null);
        panelLocations.add(locationButtonsPanelFiducial2, "10, 6");

        lblFiducial3Location = new JLabel("Fiducial 3");
        lblFiducial3Location.setToolTipText("The location of the third diamond shaped fiducial counter-clockwise from the first.");
        panelLocations.add(lblFiducial3Location, "2, 8, right, default");

        textFieldFiducial3X = new JTextField();
        textFieldFiducial3X.setColumns(8);
        panelLocations.add(textFieldFiducial3X, "4, 8");

        textFieldFiducial3Y = new JTextField();
        textFieldFiducial3Y.setColumns(8);
        panelLocations.add(textFieldFiducial3Y, "6, 8");

        textFieldFiducial3Z = new JTextField();
        textFieldFiducial3Z.setColumns(8);
        panelLocations.add(textFieldFiducial3Z, "8, 8");

        locationButtonsPanelFiducial3 = new LocationButtonsPanel((JTextField) null, (JTextField) null, (JTextField) null, (JTextField) null);
        panelLocations.add(locationButtonsPanelFiducial3, "10, 8");

        lblNormalize = new JLabel("Normalize");
        lblNormalize.setToolTipText("Normalize the coordinate system to the theoretically correct values according to the 3D printed modell and the EIA standards (whole millimeter square grid).");
        panelLocations.add(lblNormalize, "2, 10, right, default");

        chckbxNormalize = new JCheckBox("");
        chckbxNormalize.setToolTipText("");
        panelLocations.add(chckbxNormalize, "4, 10");

        lblFeederNo = new JLabel("Feeder No.");
        lblFeederNo.setToolTipText("Feeder lane number inside the same holder.");
        panelLocations.add(lblFeederNo, "2, 12, right, default");

        textFieldFeederNo = new JTextField();
        textFieldFeederNo.setEditable(false);
        panelLocations.add(textFieldFeederNo, "4, 12");
        textFieldFeederNo.setColumns(10);

        lblFeedersTotal = new JLabel("Feeders Total");
        lblFeedersTotal.setToolTipText("Total number of feeder lanes in the same holder. ");
        panelLocations.add(lblFeedersTotal, "6, 12, right, default");

        textFieldFeedersTotal = new JTextField();
        textFieldFeedersTotal.setEditable(false);
        textFieldFeedersTotal.setColumns(10);
        panelLocations.add(textFieldFeedersTotal, "8, 12");
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

        addWrappedBinding(feeder, "part", comboBoxPart, "selectedItem");
        addWrappedBinding(feeder, "retryCount", retryCountTf, "text", intConverter);

        addWrappedBinding(feeder, "tapeLength", textFieldTapeLength, "text", lengthConverter);
        addWrappedBinding(feeder, "feederExtent", textFieldFeederExtent, "text", lengthConverter);
        addWrappedBinding(feeder, "pocketCenterline", textFieldPocketCenterline, "text", lengthConverter);
        addWrappedBinding(feeder, "pocketPitch", textFieldPocketPitch, "text", lengthConverter);
        addWrappedBinding(feeder, "pocketSize", textFieldPocketSize, "text", lengthConverter);
        addWrappedBinding(feeder, "feedCount", textFieldFeedCount, "text", intConverter);

        MutableLocationProxy fiducial1Location = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "fiducial1Location", fiducial1Location, "location");
        addWrappedBinding(fiducial1Location, "lengthX", textFieldFiducial1X, "text", lengthConverter);
        addWrappedBinding(fiducial1Location, "lengthY", textFieldFiducial1Y, "text", lengthConverter);
        addWrappedBinding(fiducial1Location, "lengthZ", textFieldFiducial1Z, "text", lengthConverter);

        MutableLocationProxy fiducial2Location = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "fiducial2Location", fiducial2Location, "location");
        addWrappedBinding(fiducial2Location, "lengthX", textFieldFiducial2X, "text", lengthConverter);
        addWrappedBinding(fiducial2Location, "lengthY", textFieldFiducial2Y, "text", lengthConverter);
        addWrappedBinding(fiducial2Location, "lengthZ", textFieldFiducial2Z, "text", lengthConverter);

        MutableLocationProxy fiducial3Location = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "fiducial3Location", fiducial3Location, "location");
        addWrappedBinding(fiducial3Location, "lengthX", textFieldFiducial3X, "text", lengthConverter);
        addWrappedBinding(fiducial3Location, "lengthY", textFieldFiducial3Y, "text", lengthConverter);
        addWrappedBinding(fiducial3Location, "lengthZ", textFieldFiducial3Z, "text", lengthConverter);

        addWrappedBinding(feeder, "visionEnabled", chckbxUseVision, "selected");
        addWrappedBinding(feeder, "normalize", chckbxNormalize, "selected");

        addWrappedBinding(feeder, "feederNo", textFieldFeederNo, "text", intConverter);
        addWrappedBinding(feeder, "feedersTotal", textFieldFeedersTotal, "text", intConverter);

        ComponentDecorators.decorateWithAutoSelect(textFieldLocationRotation);
        ComponentDecorators.decorateWithAutoSelect(retryCountTf);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldTapeLength);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFeederExtent);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldPocketCenterline);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldPocketPitch);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldPocketSize);
        ComponentDecorators.decorateWithAutoSelect(textFieldFeedCount);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFiducial1X);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFiducial1Y);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFiducial1Z);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFiducial2X);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFiducial2Y);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFiducial2Z);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFiducial3X);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFiducial3Y);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFiducial3Z);
    }

    private Action autoSetup = new AbstractAction("Auto Setup") {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                autoSetupCamera = Configuration.get()
                        .getMachine()
                        .getDefaultHead()
                        .getDefaultCamera();
            }
            catch (Exception ex) {
                MessageBoxes.errorBox(getTopLevelAncestor(), "Auto Setup Failure", ex);
                return;
            }

            btnAutoSetup.setAction(autoSetupCancel);

            CameraView cameraView = MainFrame.get()
                    .getCameraViews()
                    .getCameraView(autoSetupCamera);
            cameraView.addActionListener(autoSetupBlind1Clicked);
            cameraView.setText("Click on the center of any one pocket opening in the feeder cover.");
            cameraView.flash();

            logDebugInfo = (e.getModifiers() & ActionEvent.ALT_MASK) != 0;

            cameraView.setCameraViewFilter(new CameraViewFilter() {
                private boolean hasShownError = false;

                @Override
                public BufferedImage filterCameraImage(Camera camera, BufferedImage image) {
                    try {
                        BufferedImage bufferedImage = showShapes(camera, image);
                        hasShownError = false;
                        return bufferedImage;
                    }
                    catch (Exception e) {
                        if (!hasShownError) {
                            hasShownError = true;
                            MessageBoxes.errorBox(MainFrame.get(), "Error", e);
                        }
                        else {
                            Logger.debug("{}: {}", "Error", e);
                        }
                    }

                    return null;
                }
            });
        }
    };

    private Action autoSetupCancel = new AbstractAction("Cancel Auto Setup") {
        @Override
        public void actionPerformed(ActionEvent e) {
            btnAutoSetup.setAction(autoSetup);
            CameraView cameraView = MainFrame.get()
                    .getCameraViews()
                    .getCameraView(autoSetupCamera);
            cameraView.setText(null);
            cameraView.setCameraViewFilter(null);
            cameraView.removeActionListener(autoSetupBlind1Clicked);
            cameraView.removeActionListener(autoSetupFiducial1Clicked);
        }
    };

    private Location pocketLocation;
    private Location fiducial1Location;
    private List<Location> shapeLocations; 

    private CameraViewActionListener autoSetupBlind1Clicked = new CameraViewActionListener() {
        @Override
        public void actionPerformed(final CameraViewActionEvent action) {
            pocketLocation = action.getLocation();
            boolean isSecondaryFeeder = feeder.updateFromConnectedFeeder(pocketLocation, false);
            final CameraView cameraView = MainFrame.get()
                    .getCameraViews()
                    .getCameraView(autoSetupCamera);
            cameraView.removeActionListener(this);
            Configuration.get()
            .getMachine()
            .submit(new Callable<Void>() {
                public Void call() throws Exception {
                    cameraView.setText("Checking pocket blinds...");
                    autoSetupCamera.moveTo(pocketLocation);
                    shapeLocations = findBlindsAndCenterline(autoSetupCamera);
                    if (shapeLocations.size() < 1) {
                        throw new Exception("No blinds found at selected location");
                    }

                    cameraView.setText(
                            "Now click on the center of the first fiducial at the corner.");
                    cameraView.flash();

                    cameraView.addActionListener(autoSetupFiducial1Clicked);
                    return null;
                }
            }, new FutureCallback<Void>() {
                @Override
                public void onSuccess(Void result) {}

                @Override
                public void onFailure(final Throwable t) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            autoSetupCancel.actionPerformed(null);
                            MessageBoxes.errorBox(getTopLevelAncestor(),
                                    "Auto Setup Failure", t);
                        }
                    });
                }
            });
        }
    };

    private CameraViewActionListener autoSetupFiducial1Clicked = new CameraViewActionListener() {
        @Override
        public void actionPerformed(final CameraViewActionEvent action) {
            fiducial1Location = action.getLocation();
            final CameraView cameraView = MainFrame.get()
                    .getCameraViews()
                    .getCameraView(autoSetupCamera);
            cameraView.removeActionListener(this);
            Configuration.get()
            .getMachine()
            .submit(new Callable<Void>() {
                public Void call() throws Exception {
                    cameraView.setText("Checking first fiducial...");
                    autoSetupCamera.moveTo(fiducial1Location);
                    List<Location> fiducial1Locations = findBlindsAndCenterline(autoSetupCamera);
                    if (fiducial1Locations.size() < 1) {
                        throw new Exception("No fiducial found at selected location");
                    }


                    feeder.setFeedCount(1);
                    autoSetupCamera.moveTo(feeder.getPickLocation());
                    feeder.setFeedCount(0);

                    cameraView.setText("Setup complete!");
                    Thread.sleep(1500);
                    cameraView.setText(null);
                    cameraView.setCameraViewFilter(null);
                    btnAutoSetup.setAction(autoSetup);

                    return null;
                }
            }, new FutureCallback<Void>() {
                @Override
                public void onSuccess(Void result) {}

                @Override
                public void onFailure(final Throwable t) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            autoSetupCancel.actionPerformed(null);
                            MessageBoxes.errorBox(getTopLevelAncestor(),
                                    "Auto Setup Failure", t);
                        }
                    });
                }
            });
        }
    };
    private JLabel lblFeederExtent;
    private JTextField textFieldFeederExtent;

    private List<Location> findBlindsAndCenterline(Camera camera) throws Exception {
        // Process the pipeline to clean up the image and detect the blinds
        try (CvPipeline pipeline = getCvPipeline(camera, true)) {
            pipeline.process();

            // Grab the results
            FindShapes findShapesResults = new FindShapes(camera, pipeline).invoke();
            List<RotatedRect> blinds = findShapesResults.getBlinds();
            if (blinds.isEmpty()) {
                throw new Exception("Feeder " + getName() + ": No blinds found.");
            }


            return shapeLocations;
        }
    }


    /**
     * Show candidate blinds and fiducials in the image. 
     * Red are any rects that are found. 
     * Orange are rects that passed the distance check but failed the line check. 
     * Blue are rects that passed the line check but are considered superfluous. 
     * Green passed all checks and are considered good.
     * 
     * @param camera
     * @param image
     * @return
     */
    private BufferedImage showShapes(Camera camera, BufferedImage image) throws Exception {
        // BufferedCameraImage is used as we want to run the pipeline on an existing image
        BufferedImageCamera bufferedImageCamera = new BufferedImageCamera(camera, image);

        try (CvPipeline pipeline = getCvPipeline(bufferedImageCamera, true)) {
            // Process the pipeline to clean up the image and detect the blinds
            pipeline.process();
            // Grab the results
            Mat resultMat = pipeline.getWorkingImage().clone();
            FindShapes findShapesResults = new FindShapes(camera, pipeline).invoke();

            drawRotatedRects(resultMat, findShapesResults.getBlinds(), Color.blue);
            drawRotatedRects(resultMat, findShapesResults.getFiducials(), Color.white);
            drawLines(resultMat, findShapesResults.getLines(), Color.red);
            BufferedImage showResult = OpenCvUtils.toBufferedImage(resultMat);
            resultMat.release();
            return showResult;
        }
    }

    private class FindShapes {
        private Camera camera;
        private CvPipeline pipeline;

        private List<RotatedRect> blinds;
        private List<RotatedRect> fiducials;
        private List<Line> lines;

        public FindShapes(Camera camera, CvPipeline pipeline) {
            this.camera = camera;
            this.pipeline = pipeline;
        }

        public List<RotatedRect> getBlinds() {
            return blinds;
        }
        public List<RotatedRect> getFiducials() {
            return fiducials;
        }
        public List<Line> getLines() {
            return lines;
        }

        private double angleNorm(double angle) {
            while (angle > 45) {
                angle -= 90;
            }
            while (angle < -45) {
                angle += 90;
            }
            return angle;
        }

        public FindShapes invoke() throws Exception {
            List<RotatedRect> results = null;
            try {
                // Grab the results
                results = (List<RotatedRect>) pipeline.getResult(VisionUtils.PIPELINE_RESULTS_NAME).model;
                if (results == null /*???|| results.isEmpty()*/) {
                    throw new Exception("Feeder " + feeder.getName() + ": No blinds found.");
                }
                // in accordance with EIA-481 etc. we use millimeters.
                Location mmScale = camera.getUnitsPerPixel().convertToUnits(LengthUnit.Millimeters);
                // TODO: configurable?
                final double fidMin = 1.8;
                final double fidMax = 2.2;
                final double fidAspect = 1.2; 
                // TODO: configurable?
                double blindMin = feeder.getPocketPitch().convertToUnits(LengthUnit.Millimeters).getValue()*0.5;
                double blindMax = feeder.getPocketSize().convertToUnits(LengthUnit.Millimeters).getValue();
                double blindAspect = 1.2*blindMax/blindMin;
                if (blindMax == 0) {
                    blindMax = 22;
                    blindAspect = 2;
                }
                if (blindMin == 0) {
                    blindMin = 0.5;
                    blindAspect = 3;
                }
                double sizeTolerance = 0.5/mmScale.getX();
                double angleTolerance = 20;
                double cosinusTolerance = Math.cos(angleTolerance*Math.PI/180);

                // Convert camera center.
                Location cameraLocation = feeder.transformMachineToFeederLocation(camera.getLocation());
                // Try to make sense of it.
                boolean angleTolerant = 
                        BlindsFeeder.nullLocation.equals(feeder.getFiducial1Location())
                        ||BlindsFeeder.nullLocation.equals(feeder.getFiducial2Location());
                boolean positionTolerant = angleTolerant || feeder.getPocketCenterline().getValue() == 0;
                blinds = new ArrayList<>();
                fiducials = new ArrayList<>();
                List<Point> ransacPoints = new ArrayList<>();
                for (RotatedRect result : results) {
                    Point points[] = new Point[4];
                    result.points(points);
                    boolean isAtMargin = false;
                    for (int i = 0; i < 4; i++) {
                        // Filter out rects sticking to the edges.
                        if (points[i].x <= 2 || points[i].x >= camera.getWidth() - 2
                                || points[i].y <= 2 || points[i].y >= camera.getHeight() - 2) {
                            isAtMargin = true;
                        }
                        // Next, please.
                        i++;
                    }
                    if (! isAtMargin) {
                        // Convert shape center.
                        Location location = feeder.transformMachineToFeederLocation(VisionUtils.getPixelLocation(camera, result.center.x, result.center.y));
                        double angle = feeder.transformPixelToFeederAngle(result.angle);
                        Location mmSize = mmScale.multiply(result.size.width, result.size.height, 0, 0);
                        if (positionTolerant || Math.abs(cameraLocation.convertToUnits(LengthUnit.Millimeters).getLinearDistanceTo(location)) < 5) {
                            if (angleTolerant || Math.abs(angleNorm(angle - 45)) < angleTolerance) {
                                if (mmSize.getX() > fidMin && mmSize.getX() < fidMax && mmSize.getY() > fidMin && mmSize.getY() < fidMax
                                        && mmSize.getX()/mmSize.getY() < fidAspect 
                                        && mmSize.getY()/mmSize.getX() < fidAspect) {
                                    fiducials.add(result);
                                }
                            }
                            if (angleTolerant || Math.abs(angleNorm(angle - 0)) < angleTolerance) {
                                if (mmSize.getX() > blindMin && mmSize.getX() < blindMax && mmSize.getY() > blindMin && mmSize.getY() < blindMax
                                        && mmSize.getX()/mmSize.getY() < blindAspect 
                                        && mmSize.getY()/mmSize.getX() < blindAspect) {
                                    // Fits the size requirements.
                                    blinds.add(result);
                                    // collect for ransac too
                                    for (int i = 0; i < results.size(); i++) {
                                        ransacPoints.add(result.center.clone());
                                    }
                                }
                            }
                        }
                    }
                }
                // Get rid of outliers.
                List<RotatedRect> bestList = null;
                int bestSize = 0;
                Line bestLine = null;
                // For each two blinds speculate about a pitch and line.
                for (int i = 0; i < blinds.size(); i++) {
                    RotatedRect blind1 = blinds.get(i);
                    Location location1 = feeder.transformMachineToFeederLocation(VisionUtils.getPixelLocation(camera, blind1.center.x, blind1.center.y))
                            .convertToUnits(LengthUnit.Millimeters);
                    double angle1 = feeder.transformPixelToFeederAngle(blind1.angle);
                    for (int j = i+1; j < blinds.size(); j++) {
                        RotatedRect blind2 = blinds.get(j);
                        Location location2 = feeder.transformMachineToFeederLocation(VisionUtils.getPixelLocation(camera, blind2.center.x, blind2.center.y))
                                .convertToUnits(LengthUnit.Millimeters);
                        double angle2 = feeder.transformPixelToFeederAngle(blind2.angle);
                        if (Math.abs(angleNorm(angle1 - angle2)) < angleTolerance
                                && Math.abs(blind1.size.width - blind2.size.width) < sizeTolerance
                                && Math.abs(blind1.size.height - blind2.size.height) < sizeTolerance) {
                            // Same angle and dimension.
                            double distance2 = location2.getLinearDistanceTo(location1);
                            int pitch = (int) Math.round(distance2/2)*2;
                            if (pitch == 2 || (pitch % 4) == 0) {
                                // Valid pitch.
                                //double presetPitch = feeder.getPocketPitch().convertToUnits(LengthUnit.Millimeters).getValue();
                                Location axis = location2.subtract(location1).multiply(1/distance2, 1/distance2, 0, 0);
                                List<RotatedRect> candList = new ArrayList<>();
                                candList.add(blind1);
                                candList.add(blind2);
                                Line line = new Line(blind1.center, blind2.center);
                                double minCos = 0;
                                double maxCos = 1;
                                for (int k = 0; k < blinds.size(); k++) {
                                    if (k != i && k != j) {
                                        RotatedRect blind3 = blinds.get(k);
                                        Location location3 = feeder.transformMachineToFeederLocation(VisionUtils.getPixelLocation(camera, blind3.center.x, blind3.center.y))
                                                .convertToUnits(LengthUnit.Millimeters);
                                        double angle3 = feeder.transformPixelToFeederAngle(blind3.angle);
                                        if (Math.abs(angleNorm(angle1 - angle3)) < angleTolerance
                                                && Math.abs(blind1.size.width - blind3.size.width) < sizeTolerance
                                                && Math.abs(blind1.size.height - blind3.size.height) < sizeTolerance) {
                                            // Same angle and dimension.
                                            double distance3 = location3.getLinearDistanceTo(location1);
                                            double pitchFactor = Math.round(distance3/pitch);
                                            double pitchErr = Math.abs(pitch*pitchFactor - distance3); 
                                            if (pitchErr < 1) {
                                                // Matches pitch
                                                Location vector = location3.subtract(location1);
                                                double cosinus = (axis.getX()*vector.getX() + axis.getY()*vector.getY())/distance3;
                                                if (Math.abs(cosinus) > cosinusTolerance) {
                                                    // Right, we got one
                                                    candList.add(blind3);
                                                    if (cosinus < minCos) {
                                                        line.a = blind3.center;
                                                        minCos = cosinus;
                                                    }
                                                    else if (cosinus > maxCos) {
                                                        line.b = blind3.center;
                                                        maxCos = cosinus;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                if (bestList == null || candList.size() > bestSize) {
                                    bestList = candList;
                                    bestSize = candList.size();
                                    bestLine = line;
                                }
                            }
                        }
                    }
                }
                lines = new ArrayList<>();
                if (bestLine != null && bestSize >= 3) {
                    lines.add(bestLine);
                }
            }
            catch (ClassCastException e) {
                throw new Exception("Unrecognized result type (should be RotatedRect): " + results);
            }

            return this;
        }
    }

    private void drawRotatedRects(Mat mat, List<RotatedRect> shapes, Color color) {
        if (shapes == null || shapes.isEmpty()) {
            return;
        }
        Color centerColor = new HslColor(color).getComplementary();
        for (RotatedRect rect : shapes) {
            double x = rect.center.x;
            double y = rect.center.y;
            FluentCv.drawRotatedRect(mat, rect, color, 3);
            Imgproc.circle(mat, new Point(x, y), 2, FluentCv.colorToScalar(centerColor), 4);
        }
    }

    private void drawLines(Mat mat, List<Line> lines, Color color) {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        for (Line line : lines) {
            Imgproc.line(mat, line.a, line.b, FluentCv.colorToScalar(color), 5);
        }
    }


    /*
     * private List<Location> deriveReferenceHoles(

            Location firstPartLocation,
            Location secondPartLocation,
            List<Location> part1HoleLocations,
            List<Location> part2HoleLocations) throws Exception {

        Location partsLocationRay = secondPartLocation.subtract(firstPartLocation);
        Point feedDirection = new Point(partsLocationRay.getX(), partsLocationRay.getY());

        // We are only interested in the pair of holes closest to each part
        part1HoleLocations = part1HoleLocations.subList(0, Math.min(2, part1HoleLocations.size()));
        part2HoleLocations = part2HoleLocations.subList(0, Math.min(2, part2HoleLocations.size()));

        // Part 2's reference hole is the one farthest from part 1, in the direction of part 2
        List<LocationsAlongRay> part2HoleLocationsAlongRay = new ArrayList<>(part2HoleLocations.size());
        for (Location partLocation : part2HoleLocations) {
            Location loc = partLocation.convertToUnits(partsLocationRay.getUnits());
            Point p = new Point(loc.getX() - firstPartLocation.getX(), loc.getY() - firstPartLocation.getY());
            double distanceAlongRay = feedDirection.dot(p);
            part2HoleLocationsAlongRay.add(new LocationsAlongRay(partLocation, distanceAlongRay));
        }
        part2HoleLocationsAlongRay.sort(null);
        List<Location> part2SortedLocations = new ArrayList<>(part2HoleLocationsAlongRay.size());
        part2HoleLocationsAlongRay.forEach(locationAlongRay -> part2SortedLocations.add(locationAlongRay.location));
        List<Location> part2ReverseSortedLocations = Lists.reverse(part2SortedLocations);
        Location part2ReferenceHole = part2ReverseSortedLocations.get(0);

        // Part 1's reference hole is the one closest to part 2's reference hole.
        List<Location> part1SortedLocations = VisionUtils.sortLocationsByDistance(part2ReferenceHole, part1HoleLocations);
        Location part1ReferenceHole = part1SortedLocations.get(0);
        double holePitchMin = feeder.getHolePitchMin().convertToUnits(part1ReferenceHole.getUnits()).getValue();
        double referenceHoleDistance = part1ReferenceHole.getLinearDistanceTo(part2ReferenceHole);
        // Part 1 and part 2 may have the same reference holes for 2mm part-pitch tape, so "the one closest to part 2's
        // reference hole" will be part 2's reference hole - thus we want the other hole
        if (referenceHoleDistance < holePitchMin) {
            part1ReferenceHole = part1SortedLocations.get(1);
        }

        // Get the vector perpendicular to feedDirection (perp dot is (-y, x) and gives the vector rotated 90° to the
        // left, but we want the vector rotated 90° to the right and thus (y, -x))
        Point expectedHoleHalfspace = new Point(feedDirection.y, -feedDirection.x);
        Location hole1RelativeLocation = part1ReferenceHole.subtract(firstPartLocation);
        Location hole2RelativeLocation = part2ReferenceHole.subtract(firstPartLocation);
        Point h1 = new Point(hole1RelativeLocation.getX(), hole1RelativeLocation.getY());
        Point h2 = new Point(hole2RelativeLocation.getX(), hole2RelativeLocation.getY());
        double h1Dist = expectedHoleHalfspace.dot(h1);
        double h2Dist = expectedHoleHalfspace.dot(h2);
        boolean correctOrientation = (h1Dist > 0.0) && (h2Dist > 0.0);

        if (this.logDebugInfo) {
            Logger.info("deriveReferenceHoles");
            Logger.info("  feedDirection: " + feedDirection);
            Logger.info("  firstPartLocation: " + firstPartLocation);
            Logger.info("  secondPartLocation: " + secondPartLocation);
            Logger.info("  part1HoleLocations: " + part1HoleLocations);
            Logger.info("  part2HoleLocations: " + part2HoleLocations);
            Logger.info("  part2HoleLocationsAlongRay: " + part2HoleLocationsAlongRay);
            Logger.info("  part2ReverseSortedLocations: " + part2ReverseSortedLocations);
            Logger.info("  part1SortedLocations: " + part1SortedLocations);
            Logger.info("  referenceHoleDistance: " + referenceHoleDistance);
            Logger.info("  h1Dist: " + h1Dist);
            Logger.info("  h2Dist: " + h2Dist);
            Logger.info("  correctOrientation: " + correctOrientation);
        }

        if (!correctOrientation) {
            throw new Exception("The tape is oriented incorrectly for the feed direction of the components selected");
        }

        List<Location> referenceHoles = new ArrayList<>();
        referenceHoles.add(part1ReferenceHole);
        referenceHoles.add(part2ReferenceHole);
        return referenceHoles;
    }

    private static class LocationsAlongRay implements Comparable<LocationsAlongRay> {
        public Location location;
        public double distance;

        public LocationsAlongRay(Location location, double distance) {
            this.location = location;
            this.distance = distance;
        }

        @Override
        public int compareTo(LocationsAlongRay o) {
            return Double.compare(this.distance, o.distance);
        }

        @Override
        public String toString() {
            return String.format(Locale.US, "(%s, %s)", Double.toString(distance), location.toString());
        }
    }
     */

    private void editPipeline() throws Exception {
        Camera camera = Configuration.get().getMachine().getDefaultHead().getDefaultCamera();
        CvPipeline pipeline = getCvPipeline(camera, false);
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

    private CvPipeline getCvPipeline(Camera camera, boolean clone) {
        try {
            CvPipeline pipeline = feeder.getPipeline();;
            if (clone) {
                pipeline = pipeline.clone();
            }
            pipeline.setProperty("camera", camera);
            pipeline.setProperty("feeder", feeder);
            /* this won't work, properties must be hardwired in the stages
             * 
             * // Provide pixel min/max area to pipeline.
            // We restrict this to 24mm tape carrier having a 20.1mm max pocket size.
            // See ANSI/EIA-48 1 -C p. 11.
            double mm = VisionUtils.toPixels(new Length(1, LengthUnit.Millimeters), camera);
            Integer minArea = (int) (0.8*mm*1*mm); // 2mm pitch punched paper carrier tape (0402). 
            Integer maxArea = (int) (21*mm*21*mm); // 24mm tape carrier.
            // feeder specific
            double nominalArea = VisionUtils.toPixels(feeder.getPocketPitch(), camera)
             *VisionUtils.toPixels(feeder.getPocketSize(), camera);
            Integer minArea = (int) (0.75*nominalArea); 
            Integer maxArea = (int) (1.25*nominalArea); 
            pipeline.setProperty("FilterContours.minArea", minArea);
            pipeline.setProperty("FilterContours.maxArea", maxArea);
             */
            return pipeline;
        }
        catch (CloneNotSupportedException e) {
            throw new Error(e);
        }
    }
}

