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
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.IdentifiableListCellRenderer;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.gui.support.PartsComboBoxModel;
import org.openpnp.machine.reference.ReferenceNozzleTip;
import org.openpnp.machine.reference.camera.BufferedImageCamera;
import org.openpnp.machine.reference.feeder.BlindsFeeder;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.HeadMountable;
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
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.ELProperty;

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
        panelPart.setLayout(new FormLayout(new ColumnSpec[] {
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
        panelPart.add(comboBoxPart, "4, 2, fill, default");

        lblRotationInTape = new JLabel("Rotation In Tape");
        panelPart.add(lblRotationInTape, "2, 4, left, default");

        textFieldLocationRotation = new JTextField();
        panelPart.add(textFieldLocationRotation, "4, 4, fill, default");
        textFieldLocationRotation.setColumns(4);

        lblPartTopZ = new JLabel("Part Z");
        lblPartTopZ.setToolTipText("Part pickup Z");
        panelPart.add(lblPartTopZ, "6, 4, right, default");

        textFieldPartZ = new JTextField();
        panelPart.add(textFieldPartZ, "8, 4");
        textFieldPartZ.setColumns(8);

        btnCaptureToolZ = new JButton(captureToolCoordinatesAction);
        btnCaptureToolZ.setHideActionText(true);
        panelPart.add(btnCaptureToolZ, "10, 4");

        lblRetryCount = new JLabel("Retry Count");
        panelPart.add(lblRetryCount, "2, 6, right, default");

        retryCountTf = new JTextField();
        panelPart.add(retryCountTf, "4, 6, fill, default");
        retryCountTf.setColumns(4);

        panelTapeSettings = new JPanel();
        contentPanel.add(panelTapeSettings);
        panelTapeSettings.setBorder(new TitledBorder(
                new EtchedBorder(EtchedBorder.LOWERED, null, null), "Tape Settings",
                TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        panelTapeSettings.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
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
                        FormSpecs.DEFAULT_ROWSPEC,}));

        lblTapeLength = new JLabel("Tape Length");
        lblTapeLength.setToolTipText("Length of the tape.");
        panelTapeSettings.add(lblTapeLength, "2, 2, right, default");

        textFieldTapeLength = new JTextField();
        textFieldTapeLength.setEditable(false);
        textFieldTapeLength.setColumns(10);
        panelTapeSettings.add(textFieldTapeLength, "4, 2");

        lblFeederExtent = new JLabel("Feeder Extent");
        lblFeederExtent.setToolTipText("Total feeder holder extent (all tapes).");
        panelTapeSettings.add(lblFeederExtent, "8, 2, right, default");

        textFieldFeederExtent = new JTextField();
        textFieldFeederExtent.setEditable(false);
        textFieldFeederExtent.setText("");
        panelTapeSettings.add(textFieldFeederExtent, "10, 2");
        textFieldFeederExtent.setColumns(10);

        btnAutoSetup = new JButton(autoSetup);
        btnAutoSetup.setToolTipText("Capture the pocket pitch, size and centerline from the current camera position.");
        panelTapeSettings.add(btnAutoSetup, "14, 4, 1, 3");

        lblPocketPitch = new JLabel("Pocket Pitch");
        lblPocketPitch.setToolTipText("Picth of the part pockets in the tape.");
        panelTapeSettings.add(lblPocketPitch, "2, 4, right, default");

        textFieldPocketPitch = new JTextField();
        panelTapeSettings.add(textFieldPocketPitch, "4, 4");
        textFieldPocketPitch.setColumns(5);

        lblPartSize = new JLabel("Pocket Size");
        lblPartSize.setToolTipText("Size of the pocket across the tape.");
        panelTapeSettings.add(lblPartSize, "8, 4, right, default");

        textFieldPocketSize = new JTextField();
        textFieldPocketSize.setColumns(5);
        panelTapeSettings.add(textFieldPocketSize, "10, 4");

        lblPocketCount = new JLabel("Pocket Count");
        panelTapeSettings.add(lblPocketCount, "2, 6, right, default");

        textFieldPocketCount = new JTextField();
        textFieldPocketCount.setEditable(false);
        panelTapeSettings.add(textFieldPocketCount, "4, 6");
        textFieldPocketCount.setColumns(10);

        lblPocketCenterline = new JLabel("Pocket Centerline");
        lblPocketCenterline.setToolTipText("Centerline of the pockets i.e. the perpendicular distance between the tape pocket centerline and fiducial 1.");
        panelTapeSettings.add(lblPocketCenterline, "8, 6, right, default");

        textFieldPocketCenterline = new JTextField();
        panelTapeSettings.add(textFieldPocketCenterline, "10, 6");
        textFieldPocketCenterline.setColumns(5);

        lblPocketsEmpty = new JLabel("Pockets Empty");
        lblPocketsEmpty.setToolTipText("Number of pockets that are empty at the end of the tape.");
        panelTapeSettings.add(lblPocketsEmpty, "2, 8, right, default");

        textFieldPocketsEmpty = new JTextField();
        panelTapeSettings.add(textFieldPocketsEmpty, "4, 8");
        textFieldPocketsEmpty.setColumns(5);

        lblFeedCount = new JLabel("Feed Count");
        panelTapeSettings.add(lblFeedCount, "8, 8, right, default");

        textFieldFeedCount = new JTextField();
        panelTapeSettings.add(textFieldFeedCount, "10, 8");
        textFieldFeedCount.setColumns(5);

        btnResetFeedCount = new JButton(new AbstractAction("Reset") {
            @Override
            public void actionPerformed(ActionEvent e) {
                textFieldFeedCount.setText("0");
                applyAction.actionPerformed(e);
            }
        });
        panelTapeSettings.add(btnResetFeedCount, "14, 8");

        lblFeederNo = new JLabel("Feeder No.");
        panelTapeSettings.add(lblFeederNo, "2, 10, right, default");
        lblFeederNo.setToolTipText("Feeder lane number inside the same holder.");

        textFieldFeederNo = new JTextField();
        panelTapeSettings.add(textFieldFeederNo, "4, 10");
        textFieldFeederNo.setEditable(false);
        textFieldFeederNo.setColumns(10);

        lblFeedersTotal = new JLabel("Feeders Total");
        panelTapeSettings.add(lblFeedersTotal, "8, 10, right, default");
        lblFeedersTotal.setToolTipText("Total number of feeder lanes in the same holder. ");

        textFieldFeedersTotal = new JTextField();
        panelTapeSettings.add(textFieldFeedersTotal, "10, 10");
        textFieldFeedersTotal.setEditable(false);
        textFieldFeedersTotal.setColumns(5);

        panelCover = new JPanel();
        panelCover.setBorder(new TitledBorder(null, "Cover", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        contentPanel.add(panelCover);
        panelCover.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
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

        lblCoverType = new JLabel("Cover Type");
        panelCover.add(lblCoverType, "2, 2, right, default");

        comboBoxCoverType = new JComboBox(BlindsFeeder.CoverType.values());
        panelCover.add(comboBoxCoverType, "4, 2, fill, default");

        lblCoverOpenClose = new JLabel("Cover Open/Close");
        panelCover.add(lblCoverOpenClose, "8, 2, right, default");

        comboBoxCoverActuation = new JComboBox(BlindsFeeder.CoverActuation.values());
        panelCover.add(comboBoxCoverActuation, "10, 2, fill, default");

        btnOpenCover = new JButton(openCover);
        panelCover.add(btnOpenCover, "14, 2");

        lblPushSpeed = new JLabel("Push speed");
        lblPushSpeed.setToolTipText("Speed factor when pushing the cover.");
        panelCover.add(lblPushSpeed, "2, 4, right, default");

        textFieldPushSpeed = new JTextField();
        panelCover.add(textFieldPushSpeed, "4, 4");
        textFieldPushSpeed.setColumns(10);

        lblPushZOffset = new JLabel("Push Z Offset");
        panelCover.add(lblPushZOffset, "8, 4, right, default");

        textFieldPushZOffset = new JTextField();
        panelCover.add(textFieldPushZOffset, "10, 4, fill, default");
        textFieldPushZOffset.setColumns(10);

        btnCloseThis = new JButton(closeCover);
        panelCover.add(btnCloseThis, "14, 4");

        lblPushHigh = new JLabel("Push High?");
        lblPushHigh.setToolTipText("Push with the higher-up nozzle tip diameter. See the nozzle tip configuration.");
        panelCover.add(lblPushHigh, "8, 6, right, default");

        checkBoxPushHigh = new JCheckBox("");
        panelCover.add(checkBoxPushHigh, "10, 6");

        btnCloseAll = new JButton(closeAllCovers);
        panelCover.add(btnCloseAll, "14, 6");

        lblEdgeBeginDistance = new JLabel("Edge Distance Open");
        lblEdgeBeginDistance.setToolTipText("Distance from first sprocket to the edge used for opening the cover (fiducial 1 side).");
        panelCover.add(lblEdgeBeginDistance, "2, 8, right, default");

        textFieldEdgeOpeningDistance = new JTextField();
        panelCover.add(textFieldEdgeOpeningDistance, "4, 8");
        textFieldEdgeOpeningDistance.setColumns(10);

        lblEdgeEnd = new JLabel("Edge Distance Closed");
        lblEdgeEnd.setToolTipText("Distance from last sprocket to the edge used for closing the cover (fiducial 2 side).");
        panelCover.add(lblEdgeEnd, "8, 8, right, default");

        textFieldEdgeClosingDistance = new JTextField();
        panelCover.add(textFieldEdgeClosingDistance, "10, 8, fill, default");
        textFieldEdgeClosingDistance.setColumns(10);

        btnCalibrateEdges = new JButton("Calibrate Edges");
        btnCalibrateEdges.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                UiUtils.messageBoxOnException(() -> {
                    feeder.calibrateCoverEdges();
                });
            };
        });
        panelCover.add(btnCalibrateEdges, "14, 8");

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
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
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
        lblNormalize.setToolTipText("Normalize the coordinate system to the theoretically correct values according to the 3D printed model and the EIA standards (whole millimeter square grid).");
        panelLocations.add(lblNormalize, "2, 10, right, default");

        chckbxNormalize = new JCheckBox("");
        chckbxNormalize.setToolTipText("");
        panelLocations.add(chckbxNormalize, "4, 10");

        btnCalibrateFiducials = new JButton("Calibrate Fiducials");
        btnCalibrateFiducials.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                calibrateFiducials();
            }
        });
        panelLocations.add(btnCalibrateFiducials, "12, 10");

        JPanel panelVision = new JPanel();
        panelVision.setBorder(new TitledBorder(null, "Vision", TitledBorder.LEADING, TitledBorder.TOP,
                null, null));
        contentPanel.add(panelVision);
        panelVision.setLayout(new FormLayout(new ColumnSpec[] {
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
                        FormSpecs.DEFAULT_ROWSPEC,}));

        lblUseVision = new JLabel("Use Vision?");
        panelVision.add(lblUseVision, "2, 2");

        JButton btnEditPipeline = new JButton("Edit Pipeline");
        btnEditPipeline.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                UiUtils.messageBoxOnException(() -> {
                    editPipeline();
                });
            }
        });

        chckbxUseVision = new JCheckBox("");
        panelVision.add(chckbxUseVision, "4, 2");
        panelVision.add(btnEditPipeline, "2, 4");

        JButton btnResetPipeline = new JButton("Reset Pipeline");
        btnResetPipeline.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                resetPipeline();
            }
        });

        btnPipelineToAllFeeders = new JButton("Set Pipeline To All Feeders");
        btnPipelineToAllFeeders.setToolTipText("Set this pipeline to all the other feeders on this holder.");
        btnPipelineToAllFeeders.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                UiUtils.messageBoxOnException(() -> {
                    setPipelineToAllFeeders();
                });
            }
        });
        panelVision.add(btnPipelineToAllFeeders, "4, 4");
        panelVision.add(btnResetPipeline, "6, 4");
        initDataBindings();
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
        addWrappedBinding(feeder, "retryCount", retryCountTf, "text", intConverter);

        addWrappedBinding(feeder, "tapeLength", textFieldTapeLength, "text", lengthConverter);
        addWrappedBinding(feeder, "feederExtent", textFieldFeederExtent, "text", lengthConverter);
        addWrappedBinding(feeder, "pocketCenterline", textFieldPocketCenterline, "text", lengthConverter);
        addWrappedBinding(feeder, "pocketPitch", textFieldPocketPitch, "text", lengthConverter);
        addWrappedBinding(feeder, "pocketSize", textFieldPocketSize, "text", lengthConverter);
        addWrappedBinding(feeder, "pocketCount", textFieldPocketCount, "text", intConverter);
        addWrappedBinding(feeder, "pocketsEmpty", textFieldPocketsEmpty, "text", intConverter);
        addWrappedBinding(feeder, "feedCount", textFieldFeedCount, "text", intConverter);

        addWrappedBinding(feeder, "coverType", comboBoxCoverType, "selectedItem");
        addWrappedBinding(feeder, "coverActuation", comboBoxCoverActuation, "selectedItem");
        addWrappedBinding(feeder, "edgeOpenDistance", textFieldEdgeOpeningDistance, "text", lengthConverter);
        addWrappedBinding(feeder, "edgeClosedDistance", textFieldEdgeClosingDistance, "text", lengthConverter);
        addWrappedBinding(feeder, "pushSpeed", textFieldPushSpeed, "text", doubleConverter);
        addWrappedBinding(feeder, "pushZOffset", textFieldPushZOffset, "text", lengthConverter);
        addWrappedBinding(feeder, "pushHigh", checkBoxPushHigh, "selected");

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

        addWrappedBinding(feeder, "feederNo", textFieldFeederNo, "text", intConverter);
        addWrappedBinding(feeder, "feedersTotal", textFieldFeedersTotal, "text", intConverter);

        ComponentDecorators.decorateWithAutoSelect(textFieldLocationRotation);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldPartZ);
        ComponentDecorators.decorateWithAutoSelect(retryCountTf);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldTapeLength);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFeederExtent);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldPocketCenterline);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldPocketPitch);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldPocketSize);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldEdgeOpeningDistance);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldEdgeClosingDistance);
        ComponentDecorators.decorateWithAutoSelect(textFieldPushSpeed);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldPushZOffset);
        ComponentDecorators.decorateWithAutoSelect(textFieldFeedCount);
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


    private Action autoSetup = new AbstractAction("Auto Setup", Icons.captureCamera) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Capture the blinds pitch, size and centerline from the current camera position.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                Camera camera = Configuration.get()
                        .getMachine()
                        .getDefaultHead()
                        .getDefaultCamera();

                feeder.findPocketsAndCenterline(camera);
            });
        }
    };

    private Action openCover = new AbstractAction("Open This", Icons.lockOpenOutline) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Open this cover using the nozzle tip.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                feeder.actuateCover(true);
            });
        }
    };
    private Action closeCover = new AbstractAction("Close This", Icons.lockOutline) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Close this cover using the nozzle tip.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                feeder.actuateCover(false);
            });
        }
    };
    private Action closeAllCovers = new AbstractAction("Close All", Icons.lockOutline) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Close all feeder covers of the machine.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                BlindsFeeder.actuateAllFeederCovers(false);
            });
        }
    };


    private JLabel lblFeederExtent;
    private JTextField textFieldFeederExtent;
    private JLabel lblPartTopZ;
    private JButton btnCaptureToolZ;
    private JButton btnCalibrateFiducials;
    private JLabel lblPocketCount;
    private JTextField textFieldPocketCount;
    private JTextField textFieldPocketsEmpty;
    private JLabel lblPocketsEmpty;
    private JButton btnPipelineToAllFeeders;
    private JButton btnOpenCover;
    private JLabel lblEdgeBeginDistance;
    private JTextField textFieldEdgeOpeningDistance;
    private JLabel lblPushSpeed;
    private JTextField textFieldPushSpeed;
    private JPanel panelCover;
    private JLabel lblCoverType;
    private JComboBox comboBoxCoverType;
    private JLabel lblCoverOpenClose;
    private JComboBox comboBoxCoverActuation;
    private JButton btnOpenAll;
    private JButton btnCloseAll;
    private JLabel lblPushZOffset;
    private JTextField textFieldPushZOffset;
    private JLabel lblPushHigh;
    private JCheckBox checkBoxPushHigh;
    private JButton btnCloseThis;
    private JLabel lblEdgeEnd;
    private JTextField textFieldEdgeClosingDistance;
    private JButton btnCalibrateEdges;

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
    protected void initDataBindings() {
    }
}

