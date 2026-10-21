/*
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

package org.openpnp.machine.reference.feeder.wizards;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.Translations;
import org.openpnp.events.FeederSelectedEvent;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.processes.RegionOfInterestProcess;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.LongConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.machine.reference.feeder.ReferencePushPullFeeder;
import org.openpnp.machine.reference.feeder.ReferencePushPullFeeder.OcrWrongPartAction;
import org.openpnp.model.Configuration;
import org.openpnp.model.RegionOfInterest;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.util.FeederVisionHelper.PipelineType;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.OcrUtils;
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
public class ReferencePushPullFeederConfigurationWizard
extends AbstractReferenceFeederConfigurationWizard {
    private final ReferencePushPullFeeder feeder;

    public ReferencePushPullFeederConfigurationWizard(ReferencePushPullFeeder feeder) {
        super(feeder, false);
        this.feeder = feeder;

        panelLocations = new JPanel();
        panelLocations.setBorder(new TitledBorder(null, Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.PanelLocations.Border.title"), TitledBorder.LEADING, //$NON-NLS-1$
                TitledBorder.TOP, null, null));

        contentPanel.add(panelLocations);
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
                "ReferencePushPullFeederConfigurationWizard.Action.PreviewVisionFeatures.shortDescription")); //$NON-NLS-1$
        btnShowVisionFeatures.setText(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.Action.PreviewVisionFeatures.text")); //$NON-NLS-1$
        panelLocations.add(btnShowVisionFeatures, "2, 2, default, fill");

        btnAutoSetup = new JButton(autoSetupAction);
        panelLocations.add(btnAutoSetup, "4, 2, 5, 1");
        
                button = new JButton(plusOneAction);
                panelLocations.add(button, "10, 2");

        lblX_1 = new JLabel(Translations.getString("CommonWords.X")); //$NON-NLS-1$
        panelLocations.add(lblX_1, "4, 4");

        lblY_1 = new JLabel(Translations.getString("CommonWords.Y")); //$NON-NLS-1$
        panelLocations.add(lblY_1, "6, 4");

        lblZ_1 = new JLabel(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.ZLabel.text")); //$NON-NLS-1$
        panelLocations.add(lblZ_1, "8, 4");

        lblPickLocation = new JLabel(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.PickLocationLabel.text")); //$NON-NLS-1$
        lblPickLocation.setToolTipText(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.PickLocationLabel.toolTipText")); //$NON-NLS-1$
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

        lblNormalizePickLocation = new JLabel(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.NormalizePickLocationLabel.text")); //$NON-NLS-1$
        lblNormalizePickLocation.setToolTipText(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.NormalizePickLocationLabel.toolTipText")); //$NON-NLS-1$
        panelLocations.add(lblNormalizePickLocation, "2, 8, right, default");

        checkBoxNormalizePickLocation = new JCheckBox("");
        panelLocations.add(checkBoxNormalizePickLocation, "4, 8");
        checkBoxNormalizePickLocation.setSelected(true);

        lblHole1Location = new JLabel(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.Hole1LocationLabel.text")); //$NON-NLS-1$
        lblHole1Location.setToolTipText(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.Hole1LocationLabel.toolTipText")); //$NON-NLS-1$
        panelLocations.add(lblHole1Location, "2, 10, right, default");

        textFieldHole1LocationX = new JTextField();
        panelLocations.add(textFieldHole1LocationX, "4, 10");
        textFieldHole1LocationX.setColumns(10);

        textFieldHole1LocationY = new JTextField();
        panelLocations.add(textFieldHole1LocationY, "6, 10");
        textFieldHole1LocationY.setColumns(10);

        locationButtonsPanelHole1 = new LocationButtonsPanel(textFieldHole1LocationX, textFieldHole1LocationY, (JTextField) null, (JTextField) null);
        panelLocations.add(locationButtonsPanelHole1, "10, 10");

        lblHole2Location = new JLabel(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.Hole2LocationLabel.text")); //$NON-NLS-1$
        lblHole2Location.setToolTipText(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.Hole2LocationLabel.toolTipText")); //$NON-NLS-1$
        panelLocations.add(lblHole2Location, "2, 12, right, default");

        textFieldHole2LocationX = new JTextField();
        panelLocations.add(textFieldHole2LocationX, "4, 12");
        textFieldHole2LocationX.setColumns(10);

        textFieldHole2LocationY = new JTextField();
        panelLocations.add(textFieldHole2LocationY, "6, 12");
        textFieldHole2LocationY.setColumns(10);

        locationButtonsPanelHole2 = new LocationButtonsPanel(textFieldHole2LocationX, textFieldHole2LocationY, (JTextField) null, (JTextField) null);
        panelLocations.add(locationButtonsPanelHole2, "10, 12");

        lblSnapToAxis = new JLabel(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.SnapToAxisLabel.text")); //$NON-NLS-1$
        lblSnapToAxis.setToolTipText(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.SnapToAxisLabel.toolTipText")); //$NON-NLS-1$
        panelLocations.add(lblSnapToAxis, "2, 14, right, default");

        checkBoxSnapToAxis = new JCheckBox("");
        checkBoxSnapToAxis.setToolTipText(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.SnapToAxisCheckBox.toolTipText")); //$NON-NLS-1$
        panelLocations.add(checkBoxSnapToAxis, "4, 14");
        panelLocations = new JPanel();
        panelLocations.setBorder(new TitledBorder(null, Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.PanelTape.Border.title"), TitledBorder.LEADING, //$NON-NLS-1$
                TitledBorder.TOP, null, null));

        panelTape = new JPanel();
        contentPanel.add(panelTape);
        panelTape.setBorder(new TitledBorder(null, Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.PanelTape.Border.title"), TitledBorder.LEADING, TitledBorder.TOP, null)); //$NON-NLS-1$
        panelTape.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(26dlu;default)"),
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

        lblPartPitch = new JLabel(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.PartPitchLabel.text")); //$NON-NLS-1$
        panelTape.add(lblPartPitch, "2, 2, right, default");
        lblPartPitch.setToolTipText(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.PartPitchLabel.toolTipText")); //$NON-NLS-1$

        textFieldPartPitch = new JTextField();
        panelTape.add(textFieldPartPitch, "4, 2");
        textFieldPartPitch.setToolTipText(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.PartPitchTextField.toolTipText")); //$NON-NLS-1$
        textFieldPartPitch.setColumns(5);

        lblRotation = new JLabel(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.RotationInTapeLabel.text")); //$NON-NLS-1$
        panelTape.add(lblRotation, "6, 2, right, default");
        lblRotation.setToolTipText(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.RotationInTapeLabel.toolTipText")); //$NON-NLS-1$

        textFieldRotationInTape = new JTextField();
        panelTape.add(textFieldRotationInTape, "8, 2");
        textFieldRotationInTape.setToolTipText(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.RotationInTapeTextField.toolTipText")); //$NON-NLS-1$
        textFieldRotationInTape.setColumns(10);

        lblFeedPitch = new JLabel(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.FeedPitchLabel.text")); //$NON-NLS-1$
        panelTape.add(lblFeedPitch, "2, 4, right, default");
        lblFeedPitch.setToolTipText(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.FeedPitchLabel.toolTipText")); //$NON-NLS-1$

        textFieldFeedPitch = new JTextField();
        panelTape.add(textFieldFeedPitch, "4, 4");
        textFieldFeedPitch.setToolTipText(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.FeedPitchTextField.toolTipText")); //$NON-NLS-1$
        textFieldFeedPitch.setColumns(10);

        lblMultiplier = new JLabel(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.MultiplierLabel.text")); //$NON-NLS-1$
        panelTape.add(lblMultiplier, "6, 4, right, default");
        lblMultiplier.setToolTipText(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.MultiplierLabel.toolTipText")); //$NON-NLS-1$

        textFieldFeedMultiplier = new JTextField();
        panelTape.add(textFieldFeedMultiplier, "8, 4");
        textFieldFeedMultiplier.setToolTipText(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.MultiplierTextField.toolTipText")); //$NON-NLS-1$
        textFieldFeedMultiplier.setColumns(10);

        btnDiscardParts = new JButton(discardPartsAction);
        btnDiscardParts.setToolTipText(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.Action.DiscardParts.toolTipText")); //$NON-NLS-1$
        panelTape.add(btnDiscardParts, "10, 4");

        lblFeedCount = new JLabel(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.FeedCountLabel.text")); //$NON-NLS-1$
        panelTape.add(lblFeedCount, "6, 6, right, default");
        lblFeedCount.setToolTipText(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.FeedCountLabel.toolTipText")); //$NON-NLS-1$

        textFieldFeedCount = new JTextField();
        panelTape.add(textFieldFeedCount, "8, 6");
        textFieldFeedCount.setToolTipText(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.FeedCountTextField.toolTipText")); //$NON-NLS-1$
        textFieldFeedCount.setColumns(10);

        btnReset = new JButton(resetFeedCountAction);
        panelTape.add(btnReset, "10, 6");

        Head head = null;
        try {
            head = Configuration.get().getMachine().getDefaultHead();
        }
        catch (Exception e) {
            Logger.error(e, "Cannot determine default head of machine.");
        }

        //
        panelVision = new JPanel();
        panelVision.setBorder(new TitledBorder(null, Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.PanelVision.Border.title"), TitledBorder.LEADING, //$NON-NLS-1$
                TitledBorder.TOP, null, null));
        contentPanel.add(panelVision);
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
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));

        lblCalibrationTrigger = new JLabel(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.CalibrationTriggerLabel.text")); //$NON-NLS-1$
        panelVisionEnabled.add(lblCalibrationTrigger, "2, 2, right, default");

        comboBoxCalibrationTrigger = new JComboBox(ReferencePushPullFeeder.CalibrationTrigger.values());
        panelVisionEnabled.add(comboBoxCalibrationTrigger, "4, 2");

        lblPrecisionAverage = new JLabel(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.PrecisionAverageLabel.text")); //$NON-NLS-1$
        lblPrecisionAverage.setToolTipText(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.PrecisionAverageLabel.toolTipText")); //$NON-NLS-1$
        panelVisionEnabled.add(lblPrecisionAverage, "8, 2, right, default");

        textFieldPrecisionAverage = new JTextField();
        textFieldPrecisionAverage.setToolTipText(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.PrecisionAverageTextField.toolTipText")); //$NON-NLS-1$
        textFieldPrecisionAverage.setEditable(false);
        panelVisionEnabled.add(textFieldPrecisionAverage, "10, 2");
        textFieldPrecisionAverage.setColumns(10);

        lblCalibrationCount = new JLabel(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.CalibrationCountLabel.text")); //$NON-NLS-1$
        panelVisionEnabled.add(lblCalibrationCount, "12, 2, right, default");

        textFieldCalibrationCount = new JTextField();
        textFieldCalibrationCount.setEditable(false);
        panelVisionEnabled.add(textFieldCalibrationCount, "14, 2");
        textFieldCalibrationCount.setColumns(10);

        lblPrecisionWanted = new JLabel(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.PrecisionWantedLabel.text")); //$NON-NLS-1$
        lblPrecisionWanted.setToolTipText(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.PrecisionWantedLabel.toolTipText")); //$NON-NLS-1$
        panelVisionEnabled.add(lblPrecisionWanted, "2, 4, right, default");

        textFieldPrecisionWanted = new JTextField();
        textFieldPrecisionWanted.setToolTipText(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.PrecisionWantedTextField.toolTipText")); //$NON-NLS-1$
        panelVisionEnabled.add(textFieldPrecisionWanted, "4, 4");
        textFieldPrecisionWanted.setColumns(10);

        lblPrecisionConfidenceLimit = new JLabel(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.PrecisionConfidenceLimitLabel.text")); //$NON-NLS-1$
        lblPrecisionConfidenceLimit.setToolTipText(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.PrecisionConfidenceLimitLabel.toolTipText")); //$NON-NLS-1$
        panelVisionEnabled.add(lblPrecisionConfidenceLimit, "8, 4, right, default");

        textFieldPrecisionConfidenceLimit = new JTextField();
        textFieldPrecisionConfidenceLimit.setEditable(false);
        panelVisionEnabled.add(textFieldPrecisionConfidenceLimit, "10, 4");
        textFieldPrecisionConfidenceLimit.setColumns(10);

        btnResetStatistics = new JButton(resetStatisticsAction);
        panelVisionEnabled.add(btnResetStatistics, "12, 4, 3, 1");

        lblOcrWrongPart = new JLabel(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.OcrWrongPartActionLabel.text")); //$NON-NLS-1$
        lblOcrWrongPart.setToolTipText(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.OcrWrongPartActionLabel.toolTipText")); //$NON-NLS-1$
        panelVisionEnabled.add(lblOcrWrongPart, "2, 8, right, default");

        comboBoxWrongPartAction = new JComboBox(ReferencePushPullFeeder.OcrWrongPartAction.values());
        panelVisionEnabled.add(comboBoxWrongPartAction, "4, 8");

        List<String> fontList = OcrUtils.createFontSelectionList(feeder.getOcrFontName(), true);

        lblOcrFontName = new JLabel(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.OcrFontNameLabel.text")); //$NON-NLS-1$
        lblOcrFontName.setToolTipText(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.OcrFontNameLabel.toolTipText")); //$NON-NLS-1$
        panelVisionEnabled.add(lblOcrFontName, "8, 8, right, default");
        comboBoxFontName = new JComboBox(fontList.toArray());
        panelVisionEnabled.add(comboBoxFontName, "10, 8");

        btnSetupocrregion = new JButton(setupOcrRegionAction);
        panelVisionEnabled.add(btnSetupocrregion, "12, 8, 3, 1");

        lblStopAfterWrong = new JLabel(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.StopAfterWrongPartLabel.text")); //$NON-NLS-1$
        panelVisionEnabled.add(lblStopAfterWrong, "2, 10, right, default");

        checkBoxStopAfterWrongPart = new JCheckBox("");
        panelVisionEnabled.add(checkBoxStopAfterWrongPart, "4, 10");

        lblFontSizept = new JLabel(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.OcrFontSizeLabel.text")); //$NON-NLS-1$
        lblFontSizept.setToolTipText(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.OcrFontSizeLabel.toolTipText")); //$NON-NLS-1$
        panelVisionEnabled.add(lblFontSizept, "8, 10, right, default");

        textFieldFontSizePt = new JTextField();
        panelVisionEnabled.add(textFieldFontSizePt, "10, 10");
        textFieldFontSizePt.setColumns(10);

        btnEditPipeline = new JButton(editPipelineAction);
        btnEditPipeline.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            }
        });

        btnSetPartByOcr = new JButton(performOcrAction);
        panelVisionEnabled.add(btnSetPartByOcr, "12, 10, 3, 1");

        lblDiscoverOnJobStart = new JLabel(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.DiscoverOnJobStartLabel.text")); //$NON-NLS-1$
        lblDiscoverOnJobStart.setToolTipText(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.DiscoverOnJobStartLabel.toolTipText")); //$NON-NLS-1$
        panelVisionEnabled.add(lblDiscoverOnJobStart, "2, 12, right, default");

        checkBoxDiscoverOnJobStart = new JCheckBox("");
        panelVisionEnabled.add(checkBoxDiscoverOnJobStart, "4, 12");

        btnOcrAllFeeders = new JButton(allFeederOcrAction);
        panelVisionEnabled.add(btnOcrAllFeeders, "12, 12, 3, 1");
        panelVisionEnabled.add(btnEditPipeline, "2, 16");

        lblVisionType = new JLabel(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.VisionTypeLabel.text")); //$NON-NLS-1$
        lblVisionType.setToolTipText(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.VisionTypeLabel.toolTipText")); //$NON-NLS-1$
        panelVisionEnabled.add(lblVisionType, "8, 16, right, default");

        pipelineType = new JComboBox(PipelineType.values());

        panelVisionEnabled.add(pipelineType, "10, 16, fill, default");

        btnResetPipeline = new JButton(resetPipelineAction);
        panelVisionEnabled.add(btnResetPipeline, "12, 16, 3, 1");

        panelCloning = new JPanel();
        panelCloning.setBorder(new TitledBorder(null, Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.PanelCloning.Border.title"), TitledBorder.LEADING, TitledBorder.TOP, null, null)); //$NON-NLS-1$
        contentPanel.add(panelCloning);
        panelCloning.setLayout(new FormLayout(new ColumnSpec[] {
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

        lblUsedAsTemplate = new JLabel(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.UsedAsTemplateLabel.text")); //$NON-NLS-1$
        panelCloning.add(lblUsedAsTemplate, "2, 2, right, default");
        lblUsedAsTemplate.setToolTipText(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.UsedAsTemplateLabel.toolTipText")); //$NON-NLS-1$

        checkBoxUsedAsTemplate = new JCheckBox("");
        checkBoxUsedAsTemplate.setToolTipText(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.UsedAsTemplateCheckBox.toolTipText")); //$NON-NLS-1$
        checkBoxUsedAsTemplate.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (btnSmartClone != null) {
                    btnSmartClone.setAction(checkBoxUsedAsTemplate.isSelected() ? feederCloneToAllAction: feederCloneFromTemplate);
                }
            }});
        panelCloning.add(checkBoxUsedAsTemplate, "4, 2");

        lblCloneLocationSettings = new JLabel(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.CloneLocationSettingsLabel.text")); //$NON-NLS-1$
        lblCloneLocationSettings.setToolTipText(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.CloneLocationSettingsLabel.toolTipText")); //$NON-NLS-1$
        panelCloning.add(lblCloneLocationSettings, "8, 2, right, default");

        checkBoxCloneLocationSettings = new JCheckBox("");
        checkBoxCloneLocationSettings.setSelected(true);
        checkBoxCloneLocationSettings.setToolTipText(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.CloneLocationSettingsCheckBox.toolTipText")); //$NON-NLS-1$
        panelCloning.add(checkBoxCloneLocationSettings, "10, 2");

        btnSmartClone = new JButton(feeder.isUsedAsTemplate() ? feederCloneToAllAction : feederCloneFromTemplate);
        panelCloning.add(btnSmartClone, "14, 2, 1, 7");

        lblTemplate = new JLabel(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.TemplateLabel.text")); //$NON-NLS-1$
        panelCloning.add(lblTemplate, "2, 4, right, default");

        textPaneCloneTemplateStatus = new JTextPane();
        textPaneCloneTemplateStatus.setText("&nbsp;");
        textPaneCloneTemplateStatus.setBackground(UIManager.getColor("control"));
        textPaneCloneTemplateStatus.setContentType("text/html");
        textPaneCloneTemplateStatus.setEditable(false);
        textPaneCloneTemplateStatus.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        JScrollPane textScrollPane = new JScrollPane(textPaneCloneTemplateStatus);
        textScrollPane.setPreferredSize(new Dimension(400, 70));
        panelCloning.add(textScrollPane, "4, 4, 1, 5, default, top");

        lblCloneTapeSetting = new JLabel(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.CloneTapeSettingsLabel.text")); //$NON-NLS-1$
        lblCloneTapeSetting.setToolTipText(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.CloneTapeSettingsLabel.toolTipText")); //$NON-NLS-1$
        panelCloning.add(lblCloneTapeSetting, "8, 4, right, default");

        checkBoxCloneTapeSettings = new JCheckBox("");
        checkBoxCloneTapeSettings.setSelected(true);
        checkBoxCloneTapeSettings.setToolTipText(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.CloneTapeSettingsCheckBox.toolTipText")); //$NON-NLS-1$
        panelCloning.add(checkBoxCloneTapeSettings, "10, 4");

        lblCloneVisionSettings = new JLabel(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.CloneVisionSettingsLabel.text")); //$NON-NLS-1$
        lblCloneVisionSettings.setToolTipText(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.CloneVisionSettingsLabel.toolTipText")); //$NON-NLS-1$
        panelCloning.add(lblCloneVisionSettings, "8, 6, right, default");

        checkBoxCloneVisionSettings = new JCheckBox("");
        checkBoxCloneVisionSettings.setToolTipText(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.CloneVisionSettingsCheckBox.toolTipText")); //$NON-NLS-1$
        checkBoxCloneVisionSettings.setSelected(true);
        panelCloning.add(checkBoxCloneVisionSettings, "10, 6");

        lblClonePushpullSettings = new JLabel(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.ClonePushPullSettingsLabel.text")); //$NON-NLS-1$
        lblClonePushpullSettings.setToolTipText(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.ClonePushPullSettingsLabel.toolTipText")); //$NON-NLS-1$
        panelCloning.add(lblClonePushpullSettings, "8, 8, right, default");

        checkBoxClonePushPullSettings = new JCheckBox("");
        checkBoxClonePushPullSettings.setToolTipText(Translations.getString(
                "ReferencePushPullFeederConfigurationWizard.ClonePushPullSettingsCheckBox.toolTipText")); //$NON-NLS-1$
        checkBoxClonePushPullSettings.setSelected(true);
        panelCloning.add(checkBoxClonePushPullSettings, "10, 8");
    }

    @Override
    public void createBindings() {
        super.createBindings();
        LengthConverter lengthConverter = new LengthConverter();
        IntegerConverter intConverter = new IntegerConverter();
        LongConverter longConverter = new LongConverter();
        DoubleConverter doubleConverter =
                new DoubleConverter(Configuration.get().getLengthDisplayFormat());

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

        addWrappedBinding(feeder, "partPitch", textFieldPartPitch, "text", lengthConverter);
        addWrappedBinding(feeder, "feedPitch", textFieldFeedPitch, "text", lengthConverter);
        addWrappedBinding(feeder, "feedMultiplier", textFieldFeedMultiplier, "text", longConverter);
        addWrappedBinding(feeder, "feedCount", textFieldFeedCount, "text", longConverter);

        bind(UpdateStrategy.READ_WRITE, feeder, "usedAsTemplate", checkBoxUsedAsTemplate, "selected");

        addWrappedBinding(feeder, "calibrationTrigger", comboBoxCalibrationTrigger, "selectedItem");

        addWrappedBinding(feeder, "precisionWanted", textFieldPrecisionWanted, "text", lengthConverter);
        addWrappedBinding(feeder, "calibrationCount", textFieldCalibrationCount, "text", intConverter);
        addWrappedBinding(feeder, "precisionAverage", textFieldPrecisionAverage, "text", lengthConverter);
        addWrappedBinding(feeder, "precisionConfidenceLimit", textFieldPrecisionConfidenceLimit, "text", lengthConverter);

        addWrappedBinding(feeder, "ocrWrongPartAction", comboBoxWrongPartAction, "selectedItem");
        addWrappedBinding(feeder, "ocrStopAfterWrongPart", checkBoxStopAfterWrongPart, "selected");
        addWrappedBinding(feeder, "ocrDiscoverOnJobStart", checkBoxDiscoverOnJobStart, "selected");
        addWrappedBinding(feeder, "ocrFontName", comboBoxFontName, "selectedItem");
        addWrappedBinding(feeder, "ocrFontSizePt", textFieldFontSizePt, "text", doubleConverter);
        addWrappedBinding(feeder, "pipelineType", pipelineType, "selectedItem");

        addWrappedBinding(feeder, "cloneTemplateStatus", textPaneCloneTemplateStatus, "text");

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldPickLocationX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldPickLocationY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldPickLocationZ);
        ComponentDecorators.decorateWithAutoSelect(textFieldRotationInTape);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldHole1LocationX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldHole1LocationY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldHole2LocationX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldHole2LocationY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldPartPitch);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFeedPitch);
        ComponentDecorators.decorateWithAutoSelect(textFieldFontSizePt);
    }

    private Action editPipelineAction =
            new AbstractAction(Translations.getString(
                    "ReferencePushPullFeederConfigurationWizard.Action.EditPipeline.text")) { //$NON-NLS-1$
        {
            putValue(Action.SHORT_DESCRIPTION,
                    Translations.getString("ReferencePushPullFeederConfigurationWizard.Action.EditPipeline.shortDescription")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                UiUtils.confirmMoveToLocationAndAct(
                        getTopLevelAncestor(), 
                        Translations.getString("ReferencePushPullFeederConfigurationWizard.ConfirmMoveToLocation.message"), //$NON-NLS-1$
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
                    "ReferencePushPullFeederConfigurationWizard.Action.ResetPipeline.text")) { //$NON-NLS-1$
        {
            putValue(Action.SHORT_DESCRIPTION,
                    Translations.getString("ReferencePushPullFeederConfigurationWizard.Action.ResetPipeline.shortDescription")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            PipelineType type = (PipelineType) pipelineType.getSelectedItem();
            int result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                    Translations.format("ReferencePushPullFeederConfigurationWizard.Action.ResetPipeline.confirmMessage", type), //$NON-NLS-1$
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
                    "ReferencePushPullFeederConfigurationWizard.Action.ResetStatistics.text")) { //$NON-NLS-1$
        {
            putValue(Action.SHORT_DESCRIPTION,
                    Translations.getString("ReferencePushPullFeederConfigurationWizard.Action.ResetStatistics.shortDescription")); //$NON-NLS-1$
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
                    "ReferencePushPullFeederConfigurationWizard.Action.ResetFeedCount.text")) { //$NON-NLS-1$
        {
            putValue(Action.SHORT_DESCRIPTION,
                    Translations.getString("ReferencePushPullFeederConfigurationWizard.Action.ResetFeedCount.shortDescription")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                    Translations.getString("ReferencePushPullFeederConfigurationWizard.Action.ResetFeedCount.confirmMessage"), //$NON-NLS-1$
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
                    "ReferencePushPullFeederConfigurationWizard.Action.DiscardParts.text")) { //$NON-NLS-1$
        {
            putValue(Action.SHORT_DESCRIPTION,
                    Translations.getString("ReferencePushPullFeederConfigurationWizard.Action.DiscardParts.shortDescription")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                // we apply this because it is OpenPNP custom to do so 
                applyAction.actionPerformed(e);
                feeder.discardParts();
            });
        }
    };
    private Action showVisionFeaturesAction =
            new AbstractAction(Translations.getString(
                    "ReferencePushPullFeederConfigurationWizard.Action.PreviewVisionFeatures.text")) { //$NON-NLS-1$
        {
            putValue(Action.SHORT_DESCRIPTION,
                    Translations.getString("ReferencePushPullFeederConfigurationWizard.Action.PreviewVisionFeatures.shortDescription")); //$NON-NLS-1$
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
                    "ReferencePushPullFeederConfigurationWizard.Action.AutoSetup.text"), Icons.captureCamera) { //$NON-NLS-1$
        {
            putValue(Action.SHORT_DESCRIPTION,
                    Translations.getString("ReferencePushPullFeederConfigurationWizard.Action.AutoSetup.shortDescription")); //$NON-NLS-1$
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
                    String confirmMessage = Translations.getString(
                            "ReferencePushPullFeederConfigurationWizard.Action.AutoSetup.confirmMessagePrefix"); //$NON-NLS-1$
                    if (feeder.isUsedAsTemplate()) {
                        confirmMessage += Translations.getString(
                                "ReferencePushPullFeederConfigurationWizard.Action.AutoSetup.confirmMessageTemplateWarning"); //$NON-NLS-1$
                    }
                    confirmMessage += Translations.getString(
                            "ReferencePushPullFeederConfigurationWizard.Action.AutoSetup.confirmMessageSuffix"); //$NON-NLS-1$
                    result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                            confirmMessage,
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
    private Action allFeederOcrAction =
            new AbstractAction(Translations.getString(
                    "ReferencePushPullFeederConfigurationWizard.Action.AllFeederOcr.text")) { //$NON-NLS-1$
        {
            putValue(Action.SHORT_DESCRIPTION,
                    Translations.getString("ReferencePushPullFeederConfigurationWizard.Action.AllFeederOcr.shortDescription")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            UiUtils.submitUiMachineTask(() -> {
                StringBuilder report = new StringBuilder();
                feeder.performOcrOnAllFeeders(null, false, report);
                SwingUtilities.invokeLater(() -> {
                    if (report.length() == 0) {
                        report.append(Translations.getString(
                                "ReferencePushPullFeederConfigurationWizard.OcrReport.NoActionTaken")); //$NON-NLS-1$
                    }
                    JOptionPane.showMessageDialog(getTopLevelAncestor(), "<html>"+report+"</html>", //$NON-NLS-1$
                            Translations.getString("ReferencePushPullFeederConfigurationWizard.OcrReport.title"), //$NON-NLS-1$
                            JOptionPane.INFORMATION_MESSAGE);
                });
            });
        }
    };

    private Action setupOcrRegionAction =
            new AbstractAction(Translations.getString(
                    "ReferencePushPullFeederConfigurationWizard.Action.SetupOcrRegion.text")) { //$NON-NLS-1$
        {
            putValue(Action.SHORT_DESCRIPTION,
                    Translations.getString("ReferencePushPullFeederConfigurationWizard.Action.SetupOcrRegion.shortDescription")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            UiUtils.submitUiMachineTask(() -> {
                MovableUtils.moveToLocationAtSafeZ(feeder.getCamera(), feeder.getNominalVisionLocation());
                MovableUtils.fireTargetedUserAction(feeder.getCamera());
                SwingUtilities.invokeAndWait(() -> {
                    UiUtils.messageBoxOnException(() -> {
                        new RegionOfInterestProcess(MainFrame.get(), feeder.getCamera(),
                                Translations.getString("ReferencePushPullFeederConfigurationWizard.Action.SetupOcrRegion.text"), true) { //$NON-NLS-1$
                            @Override 
                            public void setResult(RegionOfInterest roi) {
                                feeder.setOcrRegion(roi);
                            }
                        };
                    });
                });
            });
        }
    };

    private Action performOcrAction =
            new AbstractAction(Translations.getString(
                    "ReferencePushPullFeederConfigurationWizard.Action.PartByOcr.text")) { //$NON-NLS-1$
        {
            putValue(Action.SHORT_DESCRIPTION,
                    Translations.getString("ReferencePushPullFeederConfigurationWizard.Action.PartByOcr.shortDescription")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            UiUtils.submitUiMachineTask(() -> {
                MovableUtils.moveToLocationAtSafeZ(feeder.getCamera(), feeder.getOcrLocation());
                MovableUtils.fireTargetedUserAction(feeder.getCamera());
                StringBuilder report = new StringBuilder();
                feeder.performOcr(OcrWrongPartAction.ChangePart, false, report);
                if (report.length() == 0) {
                    report.append(Translations.getString(
                            "ReferencePushPullFeederConfigurationWizard.OcrReport.NoActionTaken")); //$NON-NLS-1$
                }
                JOptionPane.showMessageDialog(getTopLevelAncestor(), "<html>"+report+"</html>", //$NON-NLS-1$
                        Translations.getString("ReferencePushPullFeederConfigurationWizard.OcrReport.title"), //$NON-NLS-1$
                        JOptionPane.INFORMATION_MESSAGE);
            });
        }
    };

    private Action feederCloneFromTemplate =
            new AbstractAction(Translations.getString(
                    "ReferencePushPullFeederConfigurationWizard.Action.CloneFromTemplate.text"), Icons.importt) { //$NON-NLS-1$
        {
            putValue(Action.SHORT_DESCRIPTION,
                    Translations.getString("ReferencePushPullFeederConfigurationWizard.Action.CloneFromTemplate.shortDescription")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                if (checkBoxUsedAsTemplate.isSelected()) {
                    throw new Exception(Translations.getString(
                            "ReferencePushPullFeederConfigurationWizard.Exception.FeederUsedAsTemplate")); //$NON-NLS-1$
                }
                if (!(checkBoxCloneTapeSettings.isSelected()  
                        || checkBoxClonePushPullSettings.isSelected()
                        || checkBoxCloneVisionSettings.isSelected())) {
                    throw new Exception(Translations.getString(
                            "ReferencePushPullFeederConfigurationWizard.Exception.SelectSettingsToClone")); //$NON-NLS-1$
                }
                applyAction.actionPerformed(e);
                if (feeder.getTemplateFeeder(null) == null) {
                    throw new Exception(Translations.getString(
                            "ReferencePushPullFeederConfigurationWizard.Exception.NoTemplateFeederFound")); //$NON-NLS-1$
                }
                int result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                        Translations.format(
                                "ReferencePushPullFeederConfigurationWizard.Action.CloneFromTemplate.confirmMessage", //$NON-NLS-1$
                                feeder.getCloneTemplateStatus()),
                                null, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (result == JOptionPane.YES_OPTION) {
                    feeder.smartClone(null, 
                            checkBoxCloneLocationSettings.isSelected(),
                            checkBoxCloneTapeSettings.isSelected(), 
                            checkBoxClonePushPullSettings.isSelected(),
                            checkBoxCloneVisionSettings.isSelected(), checkBoxCloneVisionSettings.isSelected());
                }
            });
        }
    };

    private Action feederCloneToAllAction =
            new AbstractAction(Translations.getString(
                    "ReferencePushPullFeederConfigurationWizard.Action.CloneToFeeders.text"), Icons.export) { //$NON-NLS-1$
        {
            putValue(Action.SHORT_DESCRIPTION,
                    Translations.getString("ReferencePushPullFeederConfigurationWizard.Action.CloneToFeeders.shortDescription")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                if (!checkBoxUsedAsTemplate.isSelected()) {
                    throw new Exception(Translations.getString(
                            "ReferencePushPullFeederConfigurationWizard.Exception.FeederNotUsedAsTemplate")); //$NON-NLS-1$
                }
                if (!(checkBoxCloneTapeSettings.isSelected()  
                        || checkBoxClonePushPullSettings.isSelected()
                        || checkBoxCloneVisionSettings.isSelected())) {
                    throw new Exception(Translations.getString(
                            "ReferencePushPullFeederConfigurationWizard.Exception.SelectSettingsToClone")); //$NON-NLS-1$
                }
                applyAction.actionPerformed(e);
                if (feeder.getCompatibleFeeders().size() == 0) {
                    throw new Exception(Translations.getString(
                            "ReferencePushPullFeederConfigurationWizard.Exception.NoCompatibleFeedersFound")); //$NON-NLS-1$
                }
                int result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                        Translations.format(
                                "ReferencePushPullFeederConfigurationWizard.Action.CloneToFeeders.confirmMessage", //$NON-NLS-1$
                                feeder.getCloneTemplateStatus()),
                                null, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (result == JOptionPane.YES_OPTION) {
                    for (ReferencePushPullFeeder targetFeeder : feeder.getCompatibleFeeders()) {
                        targetFeeder.cloneFeederSettings( 
                                checkBoxCloneLocationSettings.isSelected(),
                                checkBoxCloneTapeSettings.isSelected(), 
                                checkBoxClonePushPullSettings.isSelected(),
                                checkBoxCloneVisionSettings.isSelected(), checkBoxCloneVisionSettings.isSelected(),
                                feeder);
                    }
                }
            });
        }
    };

    private Action plusOneAction =
            new AbstractAction("", Icons.add) { //$NON-NLS-1$
        {
            putValue(Action.SHORT_DESCRIPTION,
                    Translations.getString("ReferencePushPullFeederConfigurationWizard.Action.PlusOne.shortDescription")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                applyAction.actionPerformed(e);
                ReferencePushPullFeeder newFeeder = feeder.createNewInRow();
                UiUtils.submitUiMachineTask(() -> {
                    Camera camera = feeder.getCamera(); 
                    MovableUtils.moveToLocationAtSafeZ(camera, newFeeder.getPickLocation(0, null));
                    MovableUtils.fireTargetedUserAction(camera);
                    newFeeder.autoSetup();
                    SwingUtilities.invokeLater(() -> {
                        Configuration.get().getBus().post(new FeederSelectedEvent(newFeeder, this));
                    });
                });
            });
        }
    };

    private void editPipeline() throws Exception {
        Camera camera = feeder.getCamera();
        CvPipeline pipeline = feeder.getCvPipeline(camera, false, true, true);
        CvPipelineEditor editor = new CvPipelineEditor(pipeline);
        JDialog dialog = new CvPipelineEditorDialog(MainFrame.get(),
                Translations.format("ReferencePushPullFeederConfigurationWizard.Dialog.Pipeline.title", feeder.getName()), //$NON-NLS-1$
                editor);
        dialog.setVisible(true);
    }

    private JLabel lblPartPitch;
    private JTextField textFieldPartPitch;
    private JTextField textFieldFeedPitch;
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
    private JTextField textFieldFeedMultiplier;
    private JLabel lblMultiplier;
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
    private JButton btnSmartClone;
    private JLabel lblUsedAsTemplate;
    private JCheckBox checkBoxUsedAsTemplate;
    private JButton btnSetupocrregion;
    private JLabel lblOcrFontName;
    private JComboBox comboBoxFontName;
    private JLabel lblFontSizept;
    private JTextField textFieldFontSizePt;
    private JLabel lblOcrWrongPart;
    private JComboBox comboBoxWrongPartAction;
    private JLabel lblDiscoverOnJobStart;
    private JCheckBox checkBoxDiscoverOnJobStart;
    private JButton btnOcrAllFeeders;
    private JLabel lblStopAfterWrong;
    private JCheckBox checkBoxStopAfterWrongPart;
    private JLabel lblSnapToAxis;
    private JCheckBox checkBoxSnapToAxis;
    private JPanel panelCloning;
    private JLabel lblCloneTapeSetting;
    private JCheckBox checkBoxCloneTapeSettings;
    private JLabel lblCloneVisionSettings;
    private JCheckBox checkBoxCloneVisionSettings;
    private JLabel lblClonePushpullSettings;
    private JCheckBox checkBoxClonePushPullSettings;
    private JTextPane textPaneCloneTemplateStatus;
    private JLabel lblTemplate;
    private JButton button;
    private JLabel lblCloneLocationSettings;
    private JCheckBox checkBoxCloneLocationSettings;
    private JButton btnSetPartByOcr;
    private JComboBox pipelineType;
    private JLabel lblVisionType;
}
