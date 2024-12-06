/*
 * Copyright (C) 2024 <pandaplacer.ca@gmail.com>
 * based on the ReferencePushPullFeeder
 * Copyright (C) 2020 <mark@makr.zone>
 * based on the ReferenceLeverFeeder
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

package org.openpnp.machine.pandaplacer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.support.ActuatorsComboBoxModel;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.LongConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.gui.support.NamedConverter;
import org.openpnp.machine.reference.FeederWithOptions.FeedOptions;
import org.openpnp.machine.reference.feeder.wizards.AbstractReferenceFeederConfigurationWizard;
import org.openpnp.machine.pandaplacer.BambooFeederAutoVision;
import org.openpnp.machine.pandaplacer.AbstractPandaplacerVisionFeeder.CalibrationTrigger;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Camera;
import org.openpnp.util.FeederVisionHelper.PipelineType;
import org.openpnp.util.UiUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.ui.CvPipelineEditor;
import org.openpnp.vision.pipeline.ui.CvPipelineEditorDialog;
import org.pmw.tinylog.Logger;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class BambooFeederAutoVisionConfigurationWizard
extends AbstractReferenceFeederConfigurationWizard {
    private final BambooFeederAutoVision feeder;

    public BambooFeederAutoVisionConfigurationWizard(BambooFeederAutoVision feeder) {
        super(feeder, false);
        this.feeder = feeder;

        String[] pitchValues = new String[] {"2 mm", "4 mm", "8 mm", "12 mm", "16 mm", "20 mm", "24 mm", "28 mm", "32 mm"};

        JPanel panelFields = new JPanel();
        panelFields.setLayout(new BoxLayout(panelFields, BoxLayout.Y_AXIS));

// Panel: Tape Settings
        panelLocations = new JPanel();
        panelLocations.setBorder(new TitledBorder(null, "Tape Settings", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));

        panelTape = new JPanel();
        panelFields.add(panelTape);
        panelTape.setBorder(new TitledBorder(null, "Tape Settings", TitledBorder.LEADING, TitledBorder.TOP, null));
        panelTape.setLayout(new FormLayout(new ColumnSpec[] {
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
                FormSpecs.DEFAULT_ROWSPEC,}));

        lblPartPitch = new JLabel("Part Pitch");
        panelTape.add(lblPartPitch, "2, 2, right, default");
        lblPartPitch.setToolTipText("Pitch of the parts in the tape (2mm, 4mm, 8mm, 12mm, etc.)");

        textFieldPartPitch = new JComboBox<>(pitchValues);
        panelTape.add(textFieldPartPitch, "4, 2");
        textFieldPartPitch.setToolTipText("Pitch of the parts in the tape (2mm, 4mm, 8mm, 12mm, etc.)");

        lblFeedPitch = new JLabel("Feed Pitch");
        panelTape.add(lblFeedPitch, "6, 2, right, default");
        lblFeedPitch.setToolTipText("How much the tape will be advanced by one lever actuation (usually multiples of 4mm)");

        textFieldFeedPitch = new JComboBox<>(pitchValues);
        panelTape.add(textFieldFeedPitch, "8, 2");
        textFieldFeedPitch.setToolTipText("How much the tape will be advanced by one lever actuation (usually multiples of 4mm)");

        btnDiscardParts = new JButton(discardPartsAction);
        btnDiscardParts.setToolTipText("<html>Discard parts left over in the (multi-part) feed cycle.<br/>\r\nStarts with a fresh feed cycle including vision calibration (if enabled). \r\n</html>");
        panelTape.add(btnDiscardParts, "10, 2");

        lblRotation = new JLabel("Rotation in Tape");
        panelTape.add(lblRotation, "2, 4, right, default");
        lblRotation.setToolTipText("<html>Rotation of the part inside the tape as seen when the sprocket holes <br/>\r\nare on top. Your E-CAD part orientation is the reference.<br/>\r\nSee also: \r\n<ul>\r\n<li>EIA-481</li>\r\n<li>Component Zero Orientations for CAD Libraries</li>\r\n</ul>\r\n</html>");

        textFieldRotationInTape = new JTextField();
        panelTape.add(textFieldRotationInTape, "4, 4");
        textFieldRotationInTape.setToolTipText("<html>\n<p>The <strong>Rotation in Tape</strong> setting must be interpreted relative to the tape's orientation, <br/>\nregardless of how the feeder/tape is oriented on the machine. </p>\n<ol>\n<li>\n<p>Look at the <strong>neutral</strong> upright orientation of the part package/footprint <br/>\nas drawn inside your E-CAD <strong>library</strong>.</p>\n</li>\n<li>\n<p>Note how pin 1, polarity, cathode etc. are oriented.  <br/>\nThis is your 0° for the part.</p>\n</li>\n<li>\n<p>Look at the tape so that the sprocket holes are at the top. <br/>\nThis is your 0° tape orientation (per EIA-481 industry standard).</p>\n</li>\n<li>\n<p>Determine how the part is rotated inside the tape pocket, <em>relative</em> from  <br/>\nits upright orientation in (1).  Positive rotation goes counter-clockwise.<br/>\nThis is your <strong>Rotation in Tape</strong>.</p>\n</li>\n</ol>\n</html>");
        textFieldRotationInTape.setColumns(10);

        lblFeedCount = new JLabel("Feed Count");
        panelTape.add(lblFeedCount, "6, 4, right, default");
        lblFeedCount.setToolTipText("Total feed count of the feeder.");

        textFieldFeedCount = new JTextField();
        panelTape.add(textFieldFeedCount, "8, 4");
        textFieldFeedCount.setToolTipText("Total feed count of the feeder.");
        textFieldFeedCount.setColumns(10);

        btnReset = new JButton(resetFeedCountAction);
        panelTape.add(btnReset, "10, 4");

// Panel End: Tape Settings

// Panel: Locations
        panelLocations = new JPanel();
        panelLocations.setBorder(new TitledBorder(null, "Locations", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));

        panelFields.add(panelLocations);
        panelLocations.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(26dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("left:min"),
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
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));

        btnShowVisionFeatures = new JButton(showVisionFeaturesAction);
        btnShowVisionFeatures.setToolTipText("Preview the features recognized by Computer Vision.");
        btnShowVisionFeatures.setText("Preview Vision Features");
        panelLocations.add(btnShowVisionFeatures, "2, 2, default, fill");

        btnAutoSetup = new JButton(autoSetupAction);
        panelLocations.add(btnAutoSetup, "4, 2, 5, 1");

        lblX_1 = new JLabel("X");
        panelLocations.add(lblX_1, "4, 4, center, default");

        lblY_1 = new JLabel("Y");
        panelLocations.add(lblY_1, "6, 4, center, default");

        lblZ_1 = new JLabel("Z");
        panelLocations.add(lblZ_1, "8, 4, center, default");

        lblPickLocation = new JLabel("Pick Location");
        lblPickLocation.setToolTipText("<html>Pick Location of the part. If multiple are produced by a feed operation<br/>\r\nthis must be the last one picked i.e. the one closest to the the tape reel.</html>");
        panelLocations.add(lblPickLocation, "2, 6, right, default");

        textFieldPickLocationX = new JTextField();
        panelLocations.add(textFieldPickLocationX, "4, 6");
        textFieldPickLocationX.setColumns(10);

        textFieldPickLocationY = new JTextField();
        panelLocations.add(textFieldPickLocationY, "6, 6");
        textFieldPickLocationY.setColumns(10);

        textFieldPickLocationZ = new JTextField();
        panelLocations.add(textFieldPickLocationZ, "8, 6");
        textFieldPickLocationZ.setColumns(10);

        locationButtonsPanelFirstPick = new LocationButtonsPanel(textFieldPickLocationX, textFieldPickLocationY, textFieldPickLocationZ, null);
        panelLocations.add(locationButtonsPanelFirstPick, "10, 6");

        lblHole1Location = new JLabel("Hole 1 Location");
        lblHole1Location.setToolTipText("<html>Choose Hole 1 closer to the tape reel.<br/>\r\nIf possible choose two holes that bracket the part(s) to be picked.\r\n</html>");
        panelLocations.add(lblHole1Location, "2, 8, right, default");

        textFieldHole1LocationX = new JTextField();
        panelLocations.add(textFieldHole1LocationX, "4, 8");
        textFieldHole1LocationX.setColumns(10);

        textFieldHole1LocationY = new JTextField();
        panelLocations.add(textFieldHole1LocationY, "6, 8");
        textFieldHole1LocationY.setColumns(10);

        locationButtonsPanelHole1 = new LocationButtonsPanel(textFieldHole1LocationX, textFieldHole1LocationY, (JTextField) null, (JTextField) null);
        panelLocations.add(locationButtonsPanelHole1, "10, 8");

        lblHole2Location = new JLabel("Hole 2 Location");
        lblHole2Location.setToolTipText("<html>Choose Hole 2 further away from the tape reel.<br/>\r\nIf possible choose two holes that bracket the part(s) to be picked.\r\n</html>");
        panelLocations.add(lblHole2Location, "2, 10, right, default");

        textFieldHole2LocationX = new JTextField();
        panelLocations.add(textFieldHole2LocationX, "4, 10");
        textFieldHole2LocationX.setColumns(10);

        textFieldHole2LocationY = new JTextField();
        panelLocations.add(textFieldHole2LocationY, "6, 10");
        textFieldHole2LocationY.setColumns(10);

        locationButtonsPanelHole2 = new LocationButtonsPanel(textFieldHole2LocationX, textFieldHole2LocationY, (JTextField) null, (JTextField) null);
        panelLocations.add(locationButtonsPanelHole2, "10, 10");

        lblNormalizePickLocation = new JLabel("Normalize?");
        panelLocations.add(lblNormalizePickLocation, "2, 12, right, default");
        lblNormalizePickLocation.setToolTipText("Normalize the pick location relative to the sprocket holes according to the EIA-481 standard.");

        checkBoxNormalizePickLocation = new JCheckBox("");
        panelLocations.add(checkBoxNormalizePickLocation, "4, 12");
        checkBoxNormalizePickLocation.setSelected(true);
        checkBoxNormalizePickLocation.setToolTipText("Normalize the pick location relative to the sprocket holes according to the EIA-481 standard.");

        lblSnapToAxis = new JLabel("Snap to Axis?");
        lblSnapToAxis.setToolTipText("Snap rows of sprocket holes to the Axis parallel.");
        panelLocations.add(lblSnapToAxis, "6, 12, right, default");

        checkBoxSnapToAxis = new JCheckBox("");
        checkBoxSnapToAxis.setToolTipText("Snap rows of sprocket holes to the Axis parallel.");
        panelLocations.add(checkBoxSnapToAxis, "8, 12");


// Panel End: Locations


// Panel: Vision
        panelVision = new JPanel();
        panelVision.setBorder(new TitledBorder(null, "Vision", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        panelFields.add(panelVision);
        panelVision.setLayout(new BoxLayout(panelVision, BoxLayout.Y_AXIS));

        panelVisionEnabled = new JPanel();
        panelVision.add(panelVisionEnabled);
        panelVisionEnabled.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.LABEL_COMPONENT_GAP_COLSPEC,
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
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.LINE_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));

        lblVisionType = new JLabel("Vision Type");
        lblVisionType.setToolTipText("<html>\r\n<p>Choose the vision type, then press <strong>Reset Pipeline</strong> to assign the<br/>\r\ndefault pipeline of that type. Sprocket holes are detected as follows:</p>\r\n<ul>\r\n<li><strong>ColorKeyed</strong>: the background under the holes must be of a vivid color<br/>\r\n(green by default).</li>\r\n<li><strong>CircularSymmetry</strong>: the shape of the holes must be circular, their<br/>\r\ninside/outside must be plain.</li>\r\n</ul>\r\n<p>Both types of pipeline will further assess detected holes by size, alignment, pitch<br/>\r\nand expected distance.</p>\r\n</html>");
        panelVisionEnabled.add(lblVisionType, "2, 2, right, default");

        pipelineType = new JComboBox(PipelineType.values());
        panelVisionEnabled.add(pipelineType, "4, 2, fill, default");

        btnEditPipeline = new JButton(editPipelineAction);
        btnEditPipeline.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            }
        });
        panelVisionEnabled.add(btnEditPipeline, "8, 2, 3, 1");
        
        btnResetPipeline = new JButton(resetPipelineAction);
        panelVisionEnabled.add(btnResetPipeline, "12, 2, 3, 1");


        lblCalibrationTrigger = new JLabel("Calibration Trigger");
        panelVisionEnabled.add(lblCalibrationTrigger, "2, 4, right, default");

        comboBoxCalibrationTrigger = new JComboBox(CalibrationTrigger.values());
        panelVisionEnabled.add(comboBoxCalibrationTrigger, "4, 4");

        lblPrecisionAverage = new JLabel("Precision Average");
        lblPrecisionAverage.setToolTipText("Obtained precision average i.e. offset of the pick location, as detected by the calibration");
        panelVisionEnabled.add(lblPrecisionAverage, "8, 4, right, default");

        textFieldPrecisionAverage = new JTextField();
        textFieldPrecisionAverage.setToolTipText("Obtained precision average i.e. offset of the pick location, as detected by the calibration");
        textFieldPrecisionAverage.setEditable(false);
        panelVisionEnabled.add(textFieldPrecisionAverage, "10, 4");
        textFieldPrecisionAverage.setColumns(10);

        lblCalibrationCount = new JLabel("Calibration Count");
        panelVisionEnabled.add(lblCalibrationCount, "12, 4, right, default");

        textFieldCalibrationCount = new JTextField();
        textFieldCalibrationCount.setEditable(false);
        panelVisionEnabled.add(textFieldCalibrationCount, "14, 4");
        textFieldCalibrationCount.setColumns(10);

        lblPrecisionWanted = new JLabel("Precision wanted");
        lblPrecisionWanted.setToolTipText("Precision wanted i.e. the tolerable pick location offset");
        panelVisionEnabled.add(lblPrecisionWanted, "2, 6, right, default");

        textFieldPrecisionWanted = new JTextField();
        textFieldPrecisionWanted.setToolTipText("Precision wanted i.e. the tolerable pick location offset");
        panelVisionEnabled.add(textFieldPrecisionWanted, "4, 6");
        textFieldPrecisionWanted.setColumns(10);

        lblPrecisionConfidenceLimit = new JLabel("Precision Confidence Limit");
        lblPrecisionConfidenceLimit.setToolTipText("Precision obtained with 95% confidence (assuming normal distribution)");
        panelVisionEnabled.add(lblPrecisionConfidenceLimit, "8, 6, right, default");

        textFieldPrecisionConfidenceLimit = new JTextField();
        textFieldPrecisionConfidenceLimit.setEditable(false);
        panelVisionEnabled.add(textFieldPrecisionConfidenceLimit, "10, 6");
        textFieldPrecisionConfidenceLimit.setColumns(10);

        btnResetStatistics = new JButton(resetStatisticsAction);
        panelVisionEnabled.add(btnResetStatistics, "12, 6, 3, 1");


// Panel End: Vision


// Panel: Actuators
        panelActuator = new JPanel();
        panelActuator.setBorder(new TitledBorder(null,
                "Actuators", TitledBorder.LEADING, TitledBorder.TOP, null));
        panelFields.add(panelActuator);
        panelActuator.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(52dlu;default)"), //ColumnSpec.decode("default:grow"),
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
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,}));

        lblActuator = new JLabel("Actuator");
        panelActuator.add(lblActuator, "4, 2, center, default");

        lblActuatorValue = new JLabel("Actuator Value");
        panelActuator.add(lblActuatorValue, "6, 2, center, default");

        lblFeed = new JLabel("Feed");
        panelActuator.add(lblFeed, "2, 4, right, default");
        lblFeed.setToolTipText("Select the actuator for the feed action");


        comboBoxFeedActuator = new JComboBox();
        comboBoxFeedActuator.setModel(new ActuatorsComboBoxModel(Configuration.get().getMachine()));
        panelActuator.add(comboBoxFeedActuator, "4, 4, fill, default");
        comboBoxFeedActuator.setToolTipText("Select the actuator for the feed action");


        feedActuatorValue = new JTextField();
        panelActuator.add(feedActuatorValue, "6, 4");
        feedActuatorValue.setColumns(10);
        feedActuatorValue.setToolTipText("<html>\r\n<p>For Duble: numerical value<br/>\r\nFor Boolean: 1 = True, 0 = False</p></html>");

        comboBoxFeedOptions = new JComboBox(FeedOptions.values());
        panelActuator.add(comboBoxFeedOptions, "8, 4, fill, default");
        comboBoxFeedOptions.setToolTipText("Enables skipping physical tape transition which is handy for feeder tuning or fixing abnormal situation as e.g. manual part replacement on tape when lost from nozzle tip.");

        btnTestFeedActuator = new JButton(testFeedActuatorAction);
        panelActuator.add(btnTestFeedActuator, "10, 4");

        lblPostPick = new JLabel("Post Pick");
        panelActuator.add(lblPostPick, "2, 6, right, default");
        lblPostPick.setToolTipText("<html>\r\n<p>Select the actuator for the post pick action<br/>\r\nThis is optional: blank selection will skip the post pick operation\r\n</p></html>");


        comboBoxPostPickActuator = new JComboBox();
        comboBoxPostPickActuator.setModel(new ActuatorsComboBoxModel(Configuration.get().getMachine()));
        panelActuator.add(comboBoxPostPickActuator, "4, 6, fill, default");
        comboBoxPostPickActuator.setToolTipText("<html>\r\n<p>Select the actuator for the post pick action<br/>\r\nThis is optional: blank selection will skip the post pick operation\r\n</p></html>");


        postPickActuatorValue = new JTextField();
        postPickActuatorValue.setColumns(10);
        panelActuator.add(postPickActuatorValue, "6, 6");
        postPickActuatorValue.setToolTipText("<html>\r\n<p>For Duble: numerical value<br/>\r\nFor Boolean: 1 = True, 0 = False</p></html>");


        btnTestPostPickActuator = new JButton(testPostPickActuatorAction);
        panelActuator.add(btnTestPostPickActuator, "10, 6");

        lblMoveBeforeFeed = new JLabel("Move before feed");
        panelActuator.add(lblMoveBeforeFeed, "2, 8, right, default");
        lblMoveBeforeFeed.setToolTipText("Move nozzle to pick location before actuating the feed actuator");

        ckBoxMoveBeforeFeed = new JCheckBox();
        panelActuator.add(ckBoxMoveBeforeFeed, "4, 8, left, default");
        ckBoxMoveBeforeFeed.setToolTipText("Move nozzle to pick location before actuating the feed actuator");

// Panel End: Actuators

        contentPanel.add(panelFields);
        initDataBindings();
    }

    @Override
    public void createBindings() {
        super.createBindings();
        LengthConverter lengthConverter = new LengthConverter();
        LengthConverter lengthConverterPitch = new LengthConverter("%.0f mm");
        IntegerConverter intConverter = new IntegerConverter();
        LongConverter longConverter = new LongConverter();
        DoubleConverter doubleConverter =
                new DoubleConverter(Configuration.get().getLengthDisplayFormat());
        actuatorConverter = (new NamedConverter<>(Configuration.get().getMachine().getActuators()));

        MutableLocationProxy firstPickLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "location", firstPickLocation, "location");
        addWrappedBinding(firstPickLocation, "lengthX", textFieldPickLocationX, "text",
                lengthConverter);
        addWrappedBinding(firstPickLocation, "lengthY", textFieldPickLocationY, "text",
                lengthConverter);
        addWrappedBinding(firstPickLocation, "lengthZ", textFieldPickLocationZ, "text",
                lengthConverter);

        addWrappedBinding(feeder, "normalizePickLocation", checkBoxNormalizePickLocation, "selected");

        addWrappedBinding(feeder, "rotationInFeeder", textFieldRotationInTape, "text",
                doubleConverter);

        MutableLocationProxy hole1Location = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "hole1Location", hole1Location, "location");
        addWrappedBinding(hole1Location, "lengthX", textFieldHole1LocationX, "text",
                lengthConverter);
        addWrappedBinding(hole1Location, "lengthY", textFieldHole1LocationY, "text",
                lengthConverter);

        MutableLocationProxy hole2Location = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "hole2Location", hole2Location, "location");
        addWrappedBinding(hole2Location, "lengthX", textFieldHole2LocationX, "text",
                lengthConverter);
        addWrappedBinding(hole2Location, "lengthY", textFieldHole2LocationY, "text",
                lengthConverter);

        addWrappedBinding(feeder, "snapToAxis", checkBoxSnapToAxis, "selected");

        addWrappedBinding(feeder, "partPitch", textFieldPartPitch, "selectedItem", lengthConverterPitch);
        addWrappedBinding(feeder, "feedPitch", textFieldFeedPitch, "selectedItem", lengthConverterPitch);
        addWrappedBinding(feeder, "feedCount", textFieldFeedCount, "text", longConverter);


        addWrappedBinding(feeder, "calibrationTrigger", comboBoxCalibrationTrigger, "selectedItem");

        addWrappedBinding(feeder, "precisionWanted", textFieldPrecisionWanted, "text", lengthConverter);
        addWrappedBinding(feeder, "calibrationCount", textFieldCalibrationCount, "text", intConverter);
        addWrappedBinding(feeder, "precisionAverage", textFieldPrecisionAverage, "text", lengthConverter);
        addWrappedBinding(feeder, "precisionConfidenceLimit", textFieldPrecisionConfidenceLimit, "text", lengthConverter);

        addWrappedBinding(feeder, "pipelineType", pipelineType, "selectedItem");

        addWrappedBinding(feeder, "feedActuator", comboBoxFeedActuator, "selectedItem", actuatorConverter);
        addWrappedBinding(feeder, "feedActuatorValue", feedActuatorValue, "text", doubleConverter);
        addWrappedBinding(feeder, "feedOptions", comboBoxFeedOptions, "selectedItem");

        addWrappedBinding(feeder, "postPickActuator", comboBoxPostPickActuator, "selectedItem", actuatorConverter);
        addWrappedBinding(feeder, "postPickActuatorValue", postPickActuatorValue, "text", doubleConverter);

        addWrappedBinding(feeder, "moveBeforeFeed", ckBoxMoveBeforeFeed, "selected");


        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldPickLocationX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldPickLocationY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldPickLocationZ);
        ComponentDecorators.decorateWithAutoSelect(textFieldRotationInTape);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldHole1LocationX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldHole1LocationY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldHole2LocationX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldHole2LocationY);

        ComponentDecorators.decorateWithAutoSelect(feedActuatorValue);
        ComponentDecorators.decorateWithAutoSelect(postPickActuatorValue);
    }

    private Action editPipelineAction =
            new AbstractAction("Edit Pipeline") {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Edit the Pipeline to be used for all vision operations of this feeder.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                UiUtils.confirmMoveToLocationAndAct(
                        getTopLevelAncestor(),
                        "move the camera to the proper feeder vision location before editing the pipeline",
                        feeder.getCamera(),
                        feeder.getNominalVisionLocation(),
                        true, () -> {
                            editPipeline();
                        });
            });
        }
    };

    private Action resetPipelineAction =
            new AbstractAction("Reset Pipeline") {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Reset the Pipeline for this feeder to the selected type default.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            PipelineType type = (PipelineType) pipelineType.getSelectedItem();
            int result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                    "This will reset the pipeline to the "+type+" type default. Are you sure?",
                    null, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (result == JOptionPane.YES_OPTION) {
                applyAction.actionPerformed(null);
                UiUtils.messageBoxOnException(() -> {
                    feeder.resetPipeline(type);
                });
            }
        }
    };

    private Action resetStatisticsAction =
            new AbstractAction("Reset Statistics") {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Reset the average obtained precision statistics.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                feeder.resetCalibrationStatistics();
            });
        }
    };

    private Action resetFeedCountAction =
            new AbstractAction("Reset Feed Count") {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Reset the feed count e.g. when a tape has been changed.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                    "This will reset the recorded feed count of this feeder. Are you sure?",
                    null, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (result == JOptionPane.YES_OPTION) {
                UiUtils.messageBoxOnException(() -> {
                    // we apply this because it is OpenPNP custom to do so
                    applyAction.actionPerformed(e);
                    // set it back to 0
                    feeder.setFeedCount(0);
                });
            }
        }
    };
    private Action discardPartsAction =
            new AbstractAction("Discard Parts") {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Discard parts that have been produced by the last tape transport.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                // we apply this because it is OpenPNP custom to do so
                applyAction.actionPerformed(e);
                // round the feed count up to the next multiple of the parts per feed operation
                feeder.setFeedCount(((feeder.getFeedCount()-1)/feeder.getPartsPerFeedCycle()+1)*feeder.getPartsPerFeedCycle());
                feeder.resetCalibration();
            });
        }
    };
    private Action showVisionFeaturesAction =
            new AbstractAction("Preview Vision Features") {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Preview the features recognized by Computer Vision.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.submitUiMachineTask(() -> {
                feeder.showFeatures();
            });
        }
    };
    private Action autoSetupAction =
            new AbstractAction("Auto-Setup with Camera at Pick Location", Icons.captureCamera) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "<html>Center the camera on the pick location and press this button to Auto-Setup <br/>"
                            +"If there are multiple picks per feed cycle, choose the one closest to the tape reel.</html>");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                int result;
                if (!feeder.getLocation().multiply(1, 1, 0, 0).isInitialized()) {
                    // if the feeder.location X, Y is zero, we assume this is a freshly created feeder
                    result = JOptionPane.YES_OPTION;
                }
                else {
                    // ask the user
                    result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                            "<html>"
                            + "<p>This may overwrite all your current settings. Are you sure?</p>"
                            + "</html>",
                            null, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                }
                if (result == JOptionPane.YES_OPTION) {
                    applyAction.actionPerformed(e);
                    UiUtils.submitUiMachineTask(() -> {
                        feeder.autoSetup();
                    });
                }
            });
        }
    };

    private Action testFeedActuatorAction = new AbstractAction("Test feed") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                if (feeder.getFeedActuatorName() == null || feeder.getFeedActuatorName().equals("")) {
                  throw new Exception("No feedActuatorName specified for feeder " + feeder.getName() + ".");
                }
                Actuator actuator = Configuration.get().getMachine().getActuatorByName(feeder.getFeedActuatorName());

                if (actuator == null) {
                    throw new Exception("Feed failed. Unable to find an actuator named " + feeder.getFeedActuatorName());
                }
                // Use the generic Object method to interpret the value as the actuator.valueType.
                actuator.actuate((Object)feeder.getFeedActuatorValue());
            });
        }
    };

    private Action testPostPickActuatorAction = new AbstractAction("Test post pick") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                if (feeder.getPostPickActuatorName() == null || feeder.getPostPickActuatorName().equals("")) {
                  throw new Exception("No postPickActuatorName specified for feeder " + feeder.getName() + ".");
                }
                Actuator actuator = Configuration.get().getMachine().getActuatorByName(feeder.getPostPickActuatorName());

                if (actuator == null) {
                    throw new Exception("Feed failed. Unable to find an actuator named " + feeder.getPostPickActuatorName());
                }
                // Use the generic Object method to interpret the value as the actuator.valueType.
                actuator.actuate((Object)feeder.getPostPickActuatorValue());
            });
        }
    };

    private void editPipeline() throws Exception {
        Camera camera = feeder.getCamera();
        CvPipeline pipeline = feeder.getCvPipeline(camera, false, true);
        CvPipelineEditor editor = new CvPipelineEditor(pipeline);
        JDialog dialog = new CvPipelineEditorDialog(MainFrame.get(), feeder.getName() + " Pipeline", editor);
        dialog.setVisible(true);
    }

    protected void initDataBindings() {
    }

    private JLabel lblPartPitch;
    private JComboBox<String> textFieldPartPitch;
    private JComboBox<String> textFieldFeedPitch;
    private JLabel lblFeedPitch;
    private JPanel panelLocations;
    private JPanel panelTape;
    private JPanel panelVision;
    private JPanel panelVisionEnabled;
    private LocationButtonsPanel locationButtonsPanelFirstPick;
    private LocationButtonsPanel locationButtonsPanelHole1;
    private LocationButtonsPanel locationButtonsPanelHole2;
    private JLabel lblZ_1;
    private JLabel lblRotation;
    private JLabel lblY_1;
    private JLabel lblX_1;
    private JLabel lblPickLocation;
    private JTextField textFieldPickLocationX;
    private JTextField textFieldPickLocationY;
    private JTextField textFieldPickLocationZ;
    private JTextField textFieldRotationInTape;
    private JLabel lblHole1Location;
    private JTextField textFieldHole1LocationX;
    private JTextField textFieldHole1LocationY;
    private JTextField textFieldHole2LocationX;
    private JTextField textFieldHole2LocationY;
    private JButton btnEditPipeline;
    private JButton btnResetPipeline;
    private JLabel lblFeedCount;
    private JTextField textFieldFeedCount;
    private JButton btnReset;
    private JButton btnDiscardParts;
    private JLabel lblHole2Location;
    private JButton btnShowVisionFeatures;
    private JButton btnAutoSetup;
    private JLabel lblCalibrationTrigger;
    private JComboBox comboBoxCalibrationTrigger;
    private JLabel lblCalibrationCount;
    private JTextField textFieldCalibrationCount;
    private JLabel lblPrecisionAverage;
    private JTextField textFieldPrecisionAverage;
    private JLabel lblPrecisionWanted;
    private JTextField textFieldPrecisionWanted;
    private JButton btnResetStatistics;
    private JLabel lblPrecisionConfidenceLimit;
    private JTextField textFieldPrecisionConfidenceLimit;
    private JLabel lblNormalizePickLocation;
    private JCheckBox checkBoxNormalizePickLocation;
    private JLabel lblSnapToAxis;
    private JCheckBox checkBoxSnapToAxis;
    private JComboBox pipelineType;
    private JLabel lblVisionType;
    private JLabel lblActuator;
    private JLabel lblActuatorValue;
    private JPanel panelActuator;
    private JLabel lblFeed;
    private JComboBox comboBoxFeedActuator;
    private JTextField feedActuatorValue;
    private JComboBox comboBoxPostPickActuator;
    private JTextField postPickActuatorValue;
    private JButton btnTestFeedActuator;
    private JButton btnTestPostPickActuator;
    private JCheckBox ckBoxMoveBeforeFeed;
    private JLabel lblPostPick;
    private JLabel lblMoveBeforeFeed;
    private NamedConverter<Actuator> actuatorConverter;
    private JComboBox comboBoxFeedOptions;
}
