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
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Camera;
import org.openpnp.spi.HeadMountable;
import org.openpnp.util.UiUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.ui.CvPipelineEditor;

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


    public BlindsFeederConfigurationWizard(BlindsFeeder feeder) {
        this.feeder = feeder;

        panelPart = new JPanel();
        panelPart.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null),
                "General Settings", TitledBorder.LEADING, TitledBorder.TOP, null,
                new Color(0, 0, 0)));
        contentPanel.add(panelPart);
        panelPart.setLayout(new FormLayout(new ColumnSpec[] {
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

        lblRotationInTape = new JLabel("Rotation In Tape");
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

        btnShowInfo = new JButton(showFeaturesAction);
        panelTapeSettings.add(btnShowInfo, "14, 2");

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

        lblFirstPocket = new JLabel("First Pocket");
        lblFirstPocket.setToolTipText("First pocket of the tape that contains a part. Use the Show Features Button to indicate pocket numbers.");
        panelTapeSettings.add(lblFirstPocket, "2, 8, right, default");

        textFieldFirstPocket = new JTextField();
        panelTapeSettings.add(textFieldFirstPocket, "4, 8, fill, default");
        textFieldFirstPocket.setColumns(10);

        lblFeederNo = new JLabel("Feeder No.");
        panelTapeSettings.add(lblFeederNo, "8, 8, right, default");
        lblFeederNo.setToolTipText("Feeder lane number inside the same holder.");

        textFieldFeederNo = new JTextField();
        panelTapeSettings.add(textFieldFeederNo, "10, 8");
        textFieldFeederNo.setEditable(false);
        textFieldFeederNo.setColumns(10);

        lblLastPocket = new JLabel("Last Pocket");
        lblLastPocket.setToolTipText("Last pocket of the tape that contains a part. Use the Show Features Button to indicate pocket numbers.");
        panelTapeSettings.add(lblLastPocket, "2, 10, right, default");

        textFieldLastPocket = new JTextField();
        panelTapeSettings.add(textFieldLastPocket, "4, 10");
        textFieldLastPocket.setColumns(5);

        lblFeedersTotal = new JLabel("Feeders Total");
        panelTapeSettings.add(lblFeedersTotal, "8, 10, right, default");
        lblFeedersTotal.setToolTipText("Total number of feeder lanes in the same holder. ");

        textFieldFeedersTotal = new JTextField();
        panelTapeSettings.add(textFieldFeedersTotal, "10, 10");
        textFieldFeedersTotal.setEditable(false);
        textFieldFeedersTotal.setColumns(5);

        lblFeedCount = new JLabel("Feed Count");
        panelTapeSettings.add(lblFeedCount, "2, 12, right, default");

        textFieldFeedCount = new JTextField();
        panelTapeSettings.add(textFieldFeedCount, "4, 12");
        textFieldFeedCount.setColumns(5);

        btnResetFeedCount = new JButton(resetFeedCountAction);
        panelTapeSettings.add(btnResetFeedCount, "14, 12");

        panelCover = new JPanel();
        panelCover.setBorder(new TitledBorder(null, "Cover Settings", TitledBorder.LEADING,
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

        btnOpenAll = new JButton(openAllCovers);
        panelCover.add(btnOpenAll, "14, 6");

        btnCloseAll = new JButton(closeAllCovers);
        btnCloseAll.setToolTipText("Close the opened covers of all the feeders of the machine (including those of enabled feeders where the cover state is unknown).");
        panelCover.add(btnCloseAll, "14, 8");

        lblEdgeBeginDistance = new JLabel("Edge Distance Open");
        lblEdgeBeginDistance.setToolTipText("Distance from first sprocket to the edge used for opening the cover (fiducial 1 side).");
        panelCover.add(lblEdgeBeginDistance, "2, 10, right, default");

        textFieldEdgeOpeningDistance = new JTextField();
        panelCover.add(textFieldEdgeOpeningDistance, "4, 10");
        textFieldEdgeOpeningDistance.setColumns(10);

        lblEdgeEnd = new JLabel("Edge Distance Closed");
        lblEdgeEnd.setToolTipText("Distance from last sprocket to the edge used for closing the cover (fiducial 2 side).");
        panelCover.add(lblEdgeEnd, "8, 10, right, default");

        textFieldEdgeClosingDistance = new JTextField();
        panelCover.add(textFieldEdgeClosingDistance, "10, 10, fill, default");
        textFieldEdgeClosingDistance.setColumns(10);

        btnCalibrateEdges = new JButton(calibrateEdgesAction);
        panelCover.add(btnCalibrateEdges, "14, 10");

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

        lblUseVision = new JLabel("Use Fiducial Vision?");
        lblUseVision.setToolTipText("<html><p>Use vision for fiducial calibration when the feeder is first used. </p>\r\n<p>Even if fiducial vision is disabled, vision will still be used for setup and <br />\r\ncover open checking</p><html>");
        panelVision.add(lblUseVision, "2, 2");

        JButton btnEditPipeline = new JButton(editPipelineAction);

        chckbxUseVision = new JCheckBox("");
        panelVision.add(chckbxUseVision, "4, 2");
        panelVision.add(btnEditPipeline, "2, 4");

        JButton btnResetPipeline = new JButton(resetPipelineAction);

        btnPipelineToAllFeeders = new JButton(setPipelineToAllAction);
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
        addWrappedBinding(feeder, "feedRetryCount", retryCountTf, "text", intConverter);

        addWrappedBinding(feeder, "tapeLength", textFieldTapeLength, "text", lengthConverter);
        addWrappedBinding(feeder, "feederExtent", textFieldFeederExtent, "text", lengthConverter);
        addWrappedBinding(feeder, "pocketCenterline", textFieldPocketCenterline, "text", lengthConverter);
        addWrappedBinding(feeder, "pocketPitch", textFieldPocketPitch, "text", lengthConverter);
        addWrappedBinding(feeder, "pocketSize", textFieldPocketSize, "text", lengthConverter);
        addWrappedBinding(feeder, "pocketCount", textFieldPocketCount, "text", intConverter);
        addWrappedBinding(feeder, "firstPocket", textFieldFirstPocket, "text", intConverter);
        addWrappedBinding(feeder, "lastPocket", textFieldLastPocket, "text", intConverter);
        addWrappedBinding(feeder, "feedCount", textFieldFeedCount, "text", intConverter);

        addWrappedBinding(feeder, "coverType", comboBoxCoverType, "selectedItem");
        addWrappedBinding(feeder, "coverActuation", comboBoxCoverActuation, "selectedItem");
        addWrappedBinding(feeder, "edgeOpenDistance", textFieldEdgeOpeningDistance, "text", lengthConverter);
        addWrappedBinding(feeder, "edgeClosedDistance", textFieldEdgeClosingDistance, "text", lengthConverter);
        addWrappedBinding(feeder, "pushSpeed", textFieldPushSpeed, "text", doubleConverter);
        addWrappedBinding(feeder, "pushZOffset", textFieldPushZOffset, "text", lengthConverter);

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
        //ComponentDecorators.decorateWithAutoSelect(textFieldFirstPocket);
        //ComponentDecorators.decorateWithAutoSelect(textFieldLastPocket);
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
                        java.awt.Desktop.getDesktop().edit(file);
                    }
                }
            });
        }
    };

    private Action showFeaturesAction =
            new AbstractAction("Show Features") {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Show the features recognized by vision, taking the camera center and/or already set feeder properties into consideration.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.submitUiMachineTask(() -> {
                feeder.showFeatures();
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

    private Action calibrateEdgesAction =
            new AbstractAction("Calibrate Cover Edges") {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Calibrate the cover edges against the nozzle tip to get precise open/close positioning.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            UiUtils.submitUiMachineTask(() -> {
                feeder.calibrateCoverEdges();
            });
        };
    };

    private Action resetFeedCountAction =
            new AbstractAction("Reset") {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Reset the Feed Count to 0 (for a newly loaded tape).");
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            textFieldFeedCount.setText("0");
            applyAction.actionPerformed(e);
            UiUtils.submitUiMachineTask(() -> {
                // when resetting the feed count, we also assume we fumbled with the cover 
                feeder.setCoverPosition(new Length(Double.NaN, LengthUnit.Millimeters));
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


    private Action autoSetup = new AbstractAction("Auto Setup", Icons.captureCamera) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Capture the blinds pitch, size and centerline from the current camera position.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            UiUtils.submitUiMachineTask(() -> {
                Camera camera = Configuration.get()
                        .getMachine()
                        .getDefaultHead()
                        .getDefaultCamera();

                feeder.findPocketsAndCenterline(camera);
            });
        }
    };

    private Action openCover = new AbstractAction("Open Cover", Icons.lockOpenOutline) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Open this cover using the nozzle tip.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            UiUtils.submitUiMachineTask(() -> {
                feeder.actuateCover(MainFrame.get().getMachineControls().getSelectedNozzle(), true);
            });
        }
    };
    private Action closeCover = new AbstractAction("Close Cover", Icons.lockOutline) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Close this cover using the nozzle tip.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            UiUtils.submitUiMachineTask(() -> {
                feeder.actuateCover(MainFrame.get().getMachineControls().getSelectedNozzle(), false);
            });
        }
    };

    private Action openAllCovers = new AbstractAction("Open All Covers") {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Open the covers of all the enabled feeders of the machine.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            UiUtils.submitUiMachineTask(() -> {
                BlindsFeeder.actuateAllFeederCovers(true);
            });
        }
    };

    private Action closeAllCovers = new AbstractAction("Close All Covers") {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Close the opened covers of all the feeders of the machine.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            UiUtils.submitUiMachineTask(() -> {
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
    private JTextField textFieldLastPocket;
    private JLabel lblLastPocket;
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
    private JButton btnCloseThis;
    private JLabel lblEdgeEnd;
    private JTextField textFieldEdgeClosingDistance;
    private JButton btnCalibrateEdges;
    private JButton btnShowInfo;
    private JButton btnOpenAllButton;
    private JTextField textFieldFirstPocket;
    private JLabel lblFirstPocket;
    private JButton btnExtractOpenscadModel;

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
    private class SwingAction extends AbstractAction {
        public SwingAction() {
            putValue(NAME, "SwingAction");
            putValue(SHORT_DESCRIPTION, "Some short description");
        }
        public void actionPerformed(ActionEvent e) {
        }
    }
}

