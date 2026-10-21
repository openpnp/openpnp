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
import org.openpnp.Translations;
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
        panelLocations.setBorder(new TitledBorder(null, Translations.getString(
                "ReferenceStripFeederConfigurationWizard.PanelTapeSettings.Border.title"), TitledBorder.LEADING, //$NON-NLS-1$
                TitledBorder.TOP, null, null));

        panelTape = new JPanel();
        panelFields.add(panelTape);
        panelTape.setBorder(new TitledBorder(null, Translations.getString(
                "ReferenceStripFeederConfigurationWizard.PanelTapeSettings.Border.title"), TitledBorder.LEADING, TitledBorder.TOP, null)); //$NON-NLS-1$
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

        lblPartPitch = new JLabel(Translations.getString(
                "ReferenceStripFeederConfigurationWizard.PartPitchLabel.text")); //$NON-NLS-1$
        panelTape.add(lblPartPitch, "2, 2, right, default");
        lblPartPitch.setToolTipText(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.PartPitchLabel.toolTipText")); //$NON-NLS-1$

        textFieldPartPitch = new JComboBox<>(pitchValues);
        panelTape.add(textFieldPartPitch, "4, 2");
        textFieldPartPitch.setToolTipText(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.PartPitchComboBox.toolTipText")); //$NON-NLS-1$

        lblFeedPitch = new JLabel(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.FeedPitchLabel.text")); //$NON-NLS-1$
        panelTape.add(lblFeedPitch, "6, 2, right, default");
        lblFeedPitch.setToolTipText(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.FeedPitchLabel.toolTipText")); //$NON-NLS-1$

        textFieldFeedPitch = new JComboBox<>(pitchValues);
        panelTape.add(textFieldFeedPitch, "8, 2");
        textFieldFeedPitch.setToolTipText(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.FeedPitchComboBox.toolTipText")); //$NON-NLS-1$

        btnDiscardParts = new JButton(discardPartsAction);
        btnDiscardParts.setToolTipText(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.Action.DiscardParts.toolTipText")); //$NON-NLS-1$
        panelTape.add(btnDiscardParts, "10, 2");

        lblRotation = new JLabel(Translations.getString(
                "ReferenceStripFeederConfigurationWizard.RotationInTapeLabel.text")); //$NON-NLS-1$
        panelTape.add(lblRotation, "2, 4, right, default");
        lblRotation.setToolTipText(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.RotationInTapeLabel.toolTipText")); //$NON-NLS-1$

        textFieldRotationInTape = new JTextField();
        panelTape.add(textFieldRotationInTape, "4, 4");
        textFieldRotationInTape.setToolTipText(Translations.getString(
                "ReferenceStripFeederConfigurationWizard.RotationInTapeLabel.toolTipText")); //$NON-NLS-1$
        textFieldRotationInTape.setColumns(10);

        lblFeedCount = new JLabel(Translations.getString(
                "ReferenceStripFeederConfigurationWizard.FeedCountLabel.text")); //$NON-NLS-1$
        panelTape.add(lblFeedCount, "6, 4, right, default");
        lblFeedCount.setToolTipText(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.FeedCountLabel.toolTipText")); //$NON-NLS-1$

        textFieldFeedCount = new JTextField();
        panelTape.add(textFieldFeedCount, "8, 4");
        textFieldFeedCount.setToolTipText(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.FeedCountTextField.toolTipText")); //$NON-NLS-1$
        textFieldFeedCount.setColumns(10);

        btnReset = new JButton(resetFeedCountAction);
        panelTape.add(btnReset, "10, 4");

// Panel End: Tape Settings

// Panel: Locations
        panelLocations = new JPanel();
        panelLocations.setBorder(new TitledBorder(null, Translations.getString(
                "ReferenceStripFeederConfigurationWizard.PanelLocations.Border.title"), TitledBorder.LEADING, //$NON-NLS-1$
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
        btnShowVisionFeatures.setToolTipText(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.Action.PreviewVisionFeatures.toolTipText")); //$NON-NLS-1$
        btnShowVisionFeatures.setText(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.Action.PreviewVisionFeatures.text")); //$NON-NLS-1$
        panelLocations.add(btnShowVisionFeatures, "2, 2, default, fill");

        btnAutoSetup = new JButton(autoSetupAction);
        panelLocations.add(btnAutoSetup, "4, 2, 5, 1");

        lblX_1 = new JLabel(Translations.getString("CommonWords.X")); //$NON-NLS-1$
        panelLocations.add(lblX_1, "4, 4, center, default");

        lblY_1 = new JLabel(Translations.getString("CommonWords.Y")); //$NON-NLS-1$
        panelLocations.add(lblY_1, "6, 4, center, default");

        lblZ_1 = new JLabel(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.ZLabel.text")); //$NON-NLS-1$
        panelLocations.add(lblZ_1, "8, 4, center, default");

        lblPickLocation = new JLabel(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.PickLocationLabel.text")); //$NON-NLS-1$
        lblPickLocation.setToolTipText(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.PickLocationLabel.toolTipText")); //$NON-NLS-1$
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

        lblHole1Location = new JLabel(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.Hole1LocationLabel.text")); //$NON-NLS-1$
        lblHole1Location.setToolTipText(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.Hole1LocationLabel.toolTipText")); //$NON-NLS-1$
        panelLocations.add(lblHole1Location, "2, 8, right, default");

        textFieldHole1LocationX = new JTextField();
        panelLocations.add(textFieldHole1LocationX, "4, 8");
        textFieldHole1LocationX.setColumns(10);

        textFieldHole1LocationY = new JTextField();
        panelLocations.add(textFieldHole1LocationY, "6, 8");
        textFieldHole1LocationY.setColumns(10);

        locationButtonsPanelHole1 = new LocationButtonsPanel(textFieldHole1LocationX, textFieldHole1LocationY, (JTextField) null, (JTextField) null);
        panelLocations.add(locationButtonsPanelHole1, "10, 8");

        lblHole2Location = new JLabel(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.Hole2LocationLabel.text")); //$NON-NLS-1$
        lblHole2Location.setToolTipText(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.Hole2LocationLabel.toolTipText")); //$NON-NLS-1$
        panelLocations.add(lblHole2Location, "2, 10, right, default");

        textFieldHole2LocationX = new JTextField();
        panelLocations.add(textFieldHole2LocationX, "4, 10");
        textFieldHole2LocationX.setColumns(10);

        textFieldHole2LocationY = new JTextField();
        panelLocations.add(textFieldHole2LocationY, "6, 10");
        textFieldHole2LocationY.setColumns(10);

        locationButtonsPanelHole2 = new LocationButtonsPanel(textFieldHole2LocationX, textFieldHole2LocationY, (JTextField) null, (JTextField) null);
        panelLocations.add(locationButtonsPanelHole2, "10, 10");

        lblNormalizePickLocation = new JLabel(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.NormalizePickLocationLabel.text")); //$NON-NLS-1$
        panelLocations.add(lblNormalizePickLocation, "2, 12, right, default");
        lblNormalizePickLocation.setToolTipText(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.NormalizePickLocationLabel.toolTipText")); //$NON-NLS-1$

        checkBoxNormalizePickLocation = new JCheckBox("");
        panelLocations.add(checkBoxNormalizePickLocation, "4, 12");
        checkBoxNormalizePickLocation.setSelected(true);
        checkBoxNormalizePickLocation.setToolTipText(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.NormalizePickLocationCheckBox.toolTipText")); //$NON-NLS-1$

        lblSnapToAxis = new JLabel(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.SnapToAxisLabel.text")); //$NON-NLS-1$
        lblSnapToAxis.setToolTipText(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.SnapToAxisLabel.toolTipText")); //$NON-NLS-1$
        panelLocations.add(lblSnapToAxis, "6, 12, right, default");

        checkBoxSnapToAxis = new JCheckBox("");
        checkBoxSnapToAxis.setToolTipText(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.SnapToAxisCheckBox.toolTipText")); //$NON-NLS-1$
        panelLocations.add(checkBoxSnapToAxis, "8, 12");


// Panel End: Locations


// Panel: Vision
        panelVision = new JPanel();
        panelVision.setBorder(new TitledBorder(null, Translations.getString(
                "ReferenceStripFeederConfigurationWizard.PanelVision.Border.title"), TitledBorder.LEADING, //$NON-NLS-1$
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

        lblVisionType = new JLabel(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.VisionTypeLabel.text")); //$NON-NLS-1$
        lblVisionType.setToolTipText(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.VisionTypeLabel.toolTipText")); //$NON-NLS-1$
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


        lblCalibrationTrigger = new JLabel(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.CalibrationTriggerLabel.text")); //$NON-NLS-1$
        panelVisionEnabled.add(lblCalibrationTrigger, "2, 4, right, default");

        comboBoxCalibrationTrigger = new JComboBox(CalibrationTrigger.values());
        panelVisionEnabled.add(comboBoxCalibrationTrigger, "4, 4");

        lblPrecisionAverage = new JLabel(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.PrecisionAverageLabel.text")); //$NON-NLS-1$
        lblPrecisionAverage.setToolTipText(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.PrecisionAverageLabel.toolTipText")); //$NON-NLS-1$
        panelVisionEnabled.add(lblPrecisionAverage, "8, 4, right, default");

        textFieldPrecisionAverage = new JTextField();
        textFieldPrecisionAverage.setToolTipText(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.PrecisionAverageTextField.toolTipText")); //$NON-NLS-1$
        textFieldPrecisionAverage.setEditable(false);
        panelVisionEnabled.add(textFieldPrecisionAverage, "10, 4");
        textFieldPrecisionAverage.setColumns(10);

        lblCalibrationCount = new JLabel(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.CalibrationCountLabel.text")); //$NON-NLS-1$
        panelVisionEnabled.add(lblCalibrationCount, "12, 4, right, default");

        textFieldCalibrationCount = new JTextField();
        textFieldCalibrationCount.setEditable(false);
        panelVisionEnabled.add(textFieldCalibrationCount, "14, 4");
        textFieldCalibrationCount.setColumns(10);

        lblPrecisionWanted = new JLabel(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.PrecisionWantedLabel.text")); //$NON-NLS-1$
        lblPrecisionWanted.setToolTipText(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.PrecisionWantedLabel.toolTipText")); //$NON-NLS-1$
        panelVisionEnabled.add(lblPrecisionWanted, "2, 6, right, default");

        textFieldPrecisionWanted = new JTextField();
        textFieldPrecisionWanted.setToolTipText(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.PrecisionWantedTextField.toolTipText")); //$NON-NLS-1$
        panelVisionEnabled.add(textFieldPrecisionWanted, "4, 6");
        textFieldPrecisionWanted.setColumns(10);

        lblPrecisionConfidenceLimit = new JLabel(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.PrecisionConfidenceLimitLabel.text")); //$NON-NLS-1$
        lblPrecisionConfidenceLimit.setToolTipText(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.PrecisionConfidenceLimitLabel.toolTipText")); //$NON-NLS-1$
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
                Translations.getString("BambooFeederAutoVisionConfigurationWizard.PanelActuators.Border.title"), TitledBorder.LEADING, TitledBorder.TOP, null)); //$NON-NLS-1$
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

        lblActuator = new JLabel(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.ActuatorLabel.text")); //$NON-NLS-1$
        panelActuator.add(lblActuator, "4, 2, center, default");

        lblActuatorValue = new JLabel(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.ActuatorValueLabel.text")); //$NON-NLS-1$
        panelActuator.add(lblActuatorValue, "6, 2, center, default");

        lblFeed = new JLabel(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.FeedLabel.text")); //$NON-NLS-1$
        panelActuator.add(lblFeed, "2, 4, right, default");
        lblFeed.setToolTipText(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.FeedLabel.toolTipText")); //$NON-NLS-1$


        comboBoxFeedActuator = new JComboBox();
        comboBoxFeedActuator.setModel(new ActuatorsComboBoxModel(Configuration.get().getMachine()));
        panelActuator.add(comboBoxFeedActuator, "4, 4, fill, default");
        comboBoxFeedActuator.setToolTipText(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.FeedActuatorComboBox.toolTipText")); //$NON-NLS-1$


        feedActuatorValue = new JTextField();
        panelActuator.add(feedActuatorValue, "6, 4");
        feedActuatorValue.setColumns(10);
        feedActuatorValue.setToolTipText(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.FeedActuatorValueTextField.toolTipText")); //$NON-NLS-1$

        btnTestFeedActuator = new JButton(testFeedActuatorAction);
        btnTestFeedActuator.setToolTipText(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.Action.TestFeed.toolTipText")); //$NON-NLS-1$
        panelActuator.add(btnTestFeedActuator, "8, 4");

        lblPostPick = new JLabel(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.PostPickLabel.text")); //$NON-NLS-1$
        panelActuator.add(lblPostPick, "2, 6, right, default");
        lblPostPick.setToolTipText(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.PostPickLabel.toolTipText")); //$NON-NLS-1$


        comboBoxPostPickActuator = new JComboBox();
        comboBoxPostPickActuator.setModel(new ActuatorsComboBoxModel(Configuration.get().getMachine()));
        panelActuator.add(comboBoxPostPickActuator, "4, 6, fill, default");
        comboBoxPostPickActuator.setToolTipText(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.PostPickActuatorComboBox.toolTipText")); //$NON-NLS-1$


        postPickActuatorValue = new JTextField();
        postPickActuatorValue.setColumns(10);
        panelActuator.add(postPickActuatorValue, "6, 6");
        postPickActuatorValue.setToolTipText(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.PostPickActuatorValueTextField.toolTipText")); //$NON-NLS-1$


        btnTestPostPickActuator = new JButton(testPostPickActuatorAction);
        panelActuator.add(btnTestPostPickActuator, "8, 6");

        lblMoveBeforeFeed = new JLabel(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.MoveBeforeFeedLabel.text")); //$NON-NLS-1$
        panelActuator.add(lblMoveBeforeFeed, "2, 8, right, default");
        lblMoveBeforeFeed.setToolTipText(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.MoveBeforeFeedLabel.toolTipText")); //$NON-NLS-1$

        ckBoxMoveBeforeFeed = new JCheckBox();
        panelActuator.add(ckBoxMoveBeforeFeed, "4, 8, left, default");
        ckBoxMoveBeforeFeed.setToolTipText(Translations.getString(
                "BambooFeederAutoVisionConfigurationWizard.MoveBeforeFeedCheckBox.toolTipText")); //$NON-NLS-1$

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
            new AbstractAction(Translations.getString(
                    "ReferenceStripFeederConfigurationWizard.PanelVision.EditPipelineButton.text")) { //$NON-NLS-1$
        {
            putValue(Action.SHORT_DESCRIPTION,
                    Translations.getString("BambooFeederAutoVisionConfigurationWizard.Action.EditPipeline.shortDescription")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                UiUtils.confirmMoveToLocationAndAct(
                        getTopLevelAncestor(),
                        Translations.getString("BambooFeederAutoVisionConfigurationWizard.ConfirmMoveToLocation.message"), //$NON-NLS-1$
                        feeder.getCamera(),
                        feeder.getNominalVisionLocation(),
                        true, () -> {
                            editPipeline();
                        });
            });
        }
    };

    private Action resetPipelineAction =
            new AbstractAction(Translations.getString(
                    "ReferenceStripFeederConfigurationWizard.PanelVision.ResetPipelineButton.text")) { //$NON-NLS-1$
        {
            putValue(Action.SHORT_DESCRIPTION,
                    Translations.getString("BambooFeederAutoVisionConfigurationWizard.Action.ResetPipeline.shortDescription")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            PipelineType type = (PipelineType) pipelineType.getSelectedItem();
            int result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                    Translations.format("BambooFeederAutoVisionConfigurationWizard.Action.ResetPipeline.confirmMessage", type), //$NON-NLS-1$
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
            new AbstractAction(Translations.getString(
                    "BambooFeederAutoVisionConfigurationWizard.Action.ResetStatistics.text")) { //$NON-NLS-1$
        {
            putValue(Action.SHORT_DESCRIPTION,
                    Translations.getString("BambooFeederAutoVisionConfigurationWizard.Action.ResetStatistics.shortDescription")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                feeder.resetCalibrationStatistics();
            });
        }
    };

    private Action resetFeedCountAction =
            new AbstractAction(Translations.getString(
                    "BambooFeederAutoVisionConfigurationWizard.Action.ResetFeedCount.text")) { //$NON-NLS-1$
        {
            putValue(Action.SHORT_DESCRIPTION,
                    Translations.getString("BambooFeederAutoVisionConfigurationWizard.Action.ResetFeedCount.shortDescription")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                    Translations.getString("BambooFeederAutoVisionConfigurationWizard.Action.ResetFeedCount.confirmMessage"), //$NON-NLS-1$
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
            new AbstractAction(Translations.getString(
                    "BambooFeederAutoVisionConfigurationWizard.Action.DiscardParts.text")) { //$NON-NLS-1$
        {
            putValue(Action.SHORT_DESCRIPTION,
                    Translations.getString("BambooFeederAutoVisionConfigurationWizard.Action.DiscardParts.shortDescription")); //$NON-NLS-1$
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
            new AbstractAction(Translations.getString(
                    "BambooFeederAutoVisionConfigurationWizard.Action.PreviewVisionFeatures.text")) { //$NON-NLS-1$
        {
            putValue(Action.SHORT_DESCRIPTION,
                    Translations.getString("BambooFeederAutoVisionConfigurationWizard.Action.PreviewVisionFeatures.toolTipText")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.submitUiMachineTask(() -> {
                feeder.showFeatures();
            });
        }
    };
    private Action autoSetupAction =
            new AbstractAction(Translations.getString(
                    "BambooFeederAutoVisionConfigurationWizard.Action.AutoSetup.text"), Icons.captureCamera) { //$NON-NLS-1$
        {
            putValue(Action.SHORT_DESCRIPTION,
                    Translations.getString("BambooFeederAutoVisionConfigurationWizard.Action.AutoSetup.toolTipText")); //$NON-NLS-1$
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
                            Translations.getString("BambooFeederAutoVisionConfigurationWizard.Action.AutoSetup.confirmMessage"), //$NON-NLS-1$
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

    private Action testFeedActuatorAction = new AbstractAction(Translations.getString(
            "BambooFeederAutoVisionConfigurationWizard.Action.TestFeed.text")) { //$NON-NLS-1$
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                if (feeder.getFeedActuatorName() == null || feeder.getFeedActuatorName().equals("")) {
                  throw new Exception(Translations.format(
                          "BambooFeederAutoVisionConfigurationWizard.Exception.NoFeedActuatorName", feeder.getName())); //$NON-NLS-1$
                }
                Actuator actuator = Configuration.get().getMachine().getActuatorByName(feeder.getFeedActuatorName());

                if (actuator == null) {
                    throw new Exception(Translations.format(
                            "BambooFeederAutoVisionConfigurationWizard.Exception.FeedActuatorNotFound", feeder.getFeedActuatorName())); //$NON-NLS-1$
                }
                // Use the generic Object method to interpret the value as the actuator.valueType.
                actuator.actuate((Object)feeder.getFeedActuatorValue());
            });
        }
    };

    private Action testPostPickActuatorAction = new AbstractAction(Translations.getString(
            "BambooFeederAutoVisionConfigurationWizard.Action.TestPostPick.text")) { //$NON-NLS-1$
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                if (feeder.getPostPickActuatorName() == null || feeder.getPostPickActuatorName().equals("")) {
                  throw new Exception(Translations.format(
                          "BambooFeederAutoVisionConfigurationWizard.Exception.NoPostPickActuatorName", feeder.getName())); //$NON-NLS-1$
                }
                Actuator actuator = Configuration.get().getMachine().getActuatorByName(feeder.getPostPickActuatorName());

                if (actuator == null) {
                    throw new Exception(Translations.format(
                            "BambooFeederAutoVisionConfigurationWizard.Exception.PostPickActuatorNotFound", feeder.getPostPickActuatorName())); //$NON-NLS-1$
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
        JDialog dialog = new CvPipelineEditorDialog(MainFrame.get(),
                Translations.format("BambooFeederAutoVisionConfigurationWizard.Dialog.Pipeline.title", feeder.getName()), //$NON-NLS-1$
                editor);
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
}
