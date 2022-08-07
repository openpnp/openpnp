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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
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
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
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
import org.openpnp.machine.reference.feeder.ReferencePushPullFeeder.PipelineType;
import org.openpnp.model.Configuration;
import org.openpnp.model.RegionOfInterest;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
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

        JPanel panelFields = new JPanel();
        panelFields.setLayout(new BoxLayout(panelFields, BoxLayout.Y_AXIS));

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
        
                button = new JButton(plusOneAction);
                panelLocations.add(button, "10, 2");

        lblX_1 = new JLabel("X");
        panelLocations.add(lblX_1, "4, 4");

        lblY_1 = new JLabel("Y");
        panelLocations.add(lblY_1, "6, 4");

        lblZ_1 = new JLabel("Z");
        panelLocations.add(lblZ_1, "8, 4");

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

        lblNormalizePickLocation = new JLabel("Normalize?");
        lblNormalizePickLocation.setToolTipText("Normalize the pick location relative to the sprocket holes according to the EIA-481 standard.");
        panelLocations.add(lblNormalizePickLocation, "2, 8, right, default");

        checkBoxNormalizePickLocation = new JCheckBox("");
        panelLocations.add(checkBoxNormalizePickLocation, "4, 8");
        checkBoxNormalizePickLocation.setSelected(true);

        lblHole1Location = new JLabel("Hole 1 Location");
        lblHole1Location.setToolTipText("<html>Choose Hole 1 closer to the tape reel.<br/>\r\nIf possible choose two holes that bracket the part(s) to be picked.\r\n</html>");
        panelLocations.add(lblHole1Location, "2, 10, right, default");

        textFieldHole1LocationX = new JTextField();
        panelLocations.add(textFieldHole1LocationX, "4, 10");
        textFieldHole1LocationX.setColumns(10);

        textFieldHole1LocationY = new JTextField();
        panelLocations.add(textFieldHole1LocationY, "6, 10");
        textFieldHole1LocationY.setColumns(10);

        locationButtonsPanelHole1 = new LocationButtonsPanel(textFieldHole1LocationX, textFieldHole1LocationY, (JTextField) null, (JTextField) null);
        panelLocations.add(locationButtonsPanelHole1, "10, 10");

        lblHole2Location = new JLabel("Hole 2 Location");
        lblHole2Location.setToolTipText("<html>Choose Hole 2 further away from the tape reel.<br/>\r\nIf possible choose two holes that bracket the part(s) to be picked.\r\n</html>");
        panelLocations.add(lblHole2Location, "2, 12, right, default");

        textFieldHole2LocationX = new JTextField();
        panelLocations.add(textFieldHole2LocationX, "4, 12");
        textFieldHole2LocationX.setColumns(10);

        textFieldHole2LocationY = new JTextField();
        panelLocations.add(textFieldHole2LocationY, "6, 12");
        textFieldHole2LocationY.setColumns(10);

        locationButtonsPanelHole2 = new LocationButtonsPanel(textFieldHole2LocationX, textFieldHole2LocationY, (JTextField) null, (JTextField) null);
        panelLocations.add(locationButtonsPanelHole2, "10, 12");

        lblSnapToAxis = new JLabel("Snap to Axis?");
        lblSnapToAxis.setToolTipText("Snap rows of sprocket holes to the Axis parallel.");
        panelLocations.add(lblSnapToAxis, "2, 14, right, default");

        checkBoxSnapToAxis = new JCheckBox("");
        checkBoxSnapToAxis.setToolTipText("Snap rows of sprocket holes to the Axis parallel.");
        panelLocations.add(checkBoxSnapToAxis, "4, 14");
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

        lblPartPitch = new JLabel("Part Pitch");
        panelTape.add(lblPartPitch, "2, 2, right, default");
        lblPartPitch.setToolTipText("Pitch of the parts in the tape (2mm, 4mm, 8mm, 12mm, etc.)");

        textFieldPartPitch = new JTextField();
        panelTape.add(textFieldPartPitch, "4, 2");
        textFieldPartPitch.setToolTipText("Pitch of the parts in the tape (2mm, 4mm, 8mm, 12mm, etc.)");
        textFieldPartPitch.setColumns(5);

        lblRotation = new JLabel("Rotation in Tape");
        panelTape.add(lblRotation, "6, 2, right, default");
        lblRotation.setToolTipText("<html>Rotation of the part inside the tape as seen when the sprocket holes <br/>\r\nare on top. Your E-CAD part orientation is the reference.<br/>\r\nSee also: \r\n<ul>\r\n<li>EIA-481</li>\r\n<li>Component Zero Orientations for CAD Libraries</li>\r\n</ul>\r\n</html>");

        textFieldRotationInTape = new JTextField();
        panelTape.add(textFieldRotationInTape, "8, 2");
        textFieldRotationInTape.setToolTipText("<html>\n<p>The <strong>Rotation in Tape</strong> setting must be interpreted relative to the tape's orientation, <br/>\nregardless of how the feeder/tape is oriented on the machine. </p>\n<ol>\n<li>\n<p>Look at the <strong>neutral</strong> upright orientation of the part package/footprint <br/>\nas drawn inside your E-CAD <strong>library</strong>.</p>\n</li>\n<li>\n<p>Note how pin 1, polarity, cathode etc. are oriented.  <br/>\nThis is your 0° for the part.</p>\n</li>\n<li>\n<p>Look at the tape so that the sprocket holes are at the top. <br/>\nThis is your 0° tape orientation (per EIA-481 industry standard).</p>\n</li>\n<li>\n<p>Determine how the part is rotated inside the tape pocket, <em>relative</em> from  <br/>\nits upright orientation in (1).  Positive rotation goes counter-clockwise.<br/>\nThis is your <strong>Rotation in Tape</strong>.</p>\n</li>\n</ol>\n</html>");
        textFieldRotationInTape.setColumns(10);

        lblFeedPitch = new JLabel("Feed Pitch");
        panelTape.add(lblFeedPitch, "2, 4, right, default");
        lblFeedPitch.setToolTipText("How much the tape will be advanced by one lever actuation (usually multiples of 4mm)");

        textFieldFeedPitch = new JTextField();
        panelTape.add(textFieldFeedPitch, "4, 4");
        textFieldFeedPitch.setToolTipText("How much the tape will be advanced by one lever actuation (usually multiples of 4mm)");
        textFieldFeedPitch.setColumns(10);

        lblMultiplier = new JLabel("Multiplier");
        panelTape.add(lblMultiplier, "6, 4, right, default");
        lblMultiplier.setToolTipText("To improve efficiency you can actuate the feeder multiple times to feed more parts per feed cycle.");

        textFieldFeedMultiplier = new JTextField();
        panelTape.add(textFieldFeedMultiplier, "8, 4");
        textFieldFeedMultiplier.setToolTipText("To improve efficiency you can actuate the feeder multiple times to feed more parts per feed cycle.");
        textFieldFeedMultiplier.setColumns(10);

        btnDiscardParts = new JButton(discardPartsAction);
        btnDiscardParts.setToolTipText("<html>Discard parts left over in the (multi-part) feed cycle.<br/>\r\nStarts with a fresh feed cycle including vision calibration (if enabled). \r\n</html>");
        panelTape.add(btnDiscardParts, "10, 4");

        lblFeedCount = new JLabel("Feed Count");
        panelTape.add(lblFeedCount, "6, 6, right, default");
        lblFeedCount.setToolTipText("Total feed count of the feeder.");

        textFieldFeedCount = new JTextField();
        panelTape.add(textFieldFeedCount, "8, 6");
        textFieldFeedCount.setToolTipText("Total feed count of the feeder.");
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
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));

        lblCalibrationTrigger = new JLabel("Calibration Trigger");
        panelVisionEnabled.add(lblCalibrationTrigger, "2, 2, right, default");

        comboBoxCalibrationTrigger = new JComboBox(ReferencePushPullFeeder.CalibrationTrigger.values());
        panelVisionEnabled.add(comboBoxCalibrationTrigger, "4, 2");

        lblPrecisionAverage = new JLabel("Precision Average");
        lblPrecisionAverage.setToolTipText("Obtained precision average i.e. offset of the pick location, as detected by the calibration");
        panelVisionEnabled.add(lblPrecisionAverage, "8, 2, right, default");

        textFieldPrecisionAverage = new JTextField();
        textFieldPrecisionAverage.setToolTipText("Obtained precision average i.e. offset of the pick location, as detected by the calibration");
        textFieldPrecisionAverage.setEditable(false);
        panelVisionEnabled.add(textFieldPrecisionAverage, "10, 2");
        textFieldPrecisionAverage.setColumns(10);

        lblCalibrationCount = new JLabel("Calibration Count");
        panelVisionEnabled.add(lblCalibrationCount, "12, 2, right, default");

        textFieldCalibrationCount = new JTextField();
        textFieldCalibrationCount.setEditable(false);
        panelVisionEnabled.add(textFieldCalibrationCount, "14, 2");
        textFieldCalibrationCount.setColumns(10);

        lblPrecisionWanted = new JLabel("Precision wanted");
        lblPrecisionWanted.setToolTipText("Precision wanted i.e. the tolerable pick location offset");
        panelVisionEnabled.add(lblPrecisionWanted, "2, 4, right, default");

        textFieldPrecisionWanted = new JTextField();
        textFieldPrecisionWanted.setToolTipText("Precision wanted i.e. the tolerable pick location offset");
        panelVisionEnabled.add(textFieldPrecisionWanted, "4, 4");
        textFieldPrecisionWanted.setColumns(10);

        lblPrecisionConfidenceLimit = new JLabel("Precision Confidence Limit");
        lblPrecisionConfidenceLimit.setToolTipText("Precision obtained with 95% confidence (assuming normal distribution)");
        panelVisionEnabled.add(lblPrecisionConfidenceLimit, "8, 4, right, default");

        textFieldPrecisionConfidenceLimit = new JTextField();
        textFieldPrecisionConfidenceLimit.setEditable(false);
        panelVisionEnabled.add(textFieldPrecisionConfidenceLimit, "10, 4");
        textFieldPrecisionConfidenceLimit.setColumns(10);

        btnResetStatistics = new JButton(resetStatisticsAction);
        panelVisionEnabled.add(btnResetStatistics, "12, 4, 3, 1");

        lblOcrWrongPart = new JLabel("OCR Wrong Part Action");
        lblOcrWrongPart.setToolTipText("<html>Determines what action should be taken when OCR detects the wrong Part ID in the feeder.<br/> \r\n<ul>\r\n<li>\r\n<strong>None</strong>: Use this setting if you don't want to use OCR.</li>\r\n<li>\r\n<strong>SwapFeeders</strong>: If a wrong part is detected but the right part is selected<br/>\r\nin a different ReferencePushPullFeeder, the locations of the two feeders are swapped. The<br/>\r\nswapped-in feeder will be enabled. This will happen, if you unload/reload/rearrange your<br/>\r\nfeeders on the machine.</li>\r\n<li>\r\n<strong>SwapOrCreate</strong>: Works like <strong>SwapFeeders</strong>, but if no<br/>\r\nother feeder with the right part is found, a new one will be created and swapped-in at the<br/>\r\ncurrent feeder's location. The current feeder is then disabled in turn (they are now sitting at<br/>\r\nthe same location and only one must be enabled).</li>\r\n<li>\r\n<strong>ChangePart</strong>: The part in the current feeder is changed. This will only<br/>\r\nwork correctly, if the tape settings etc. remain the same between the parts i.e. if you restrict<br/>\r\nany reloading/rearranging to groups of feeders with the same settings.</li>\r\n<li>\r\n<strong>ChangePartAndClone</strong>: The part in the current feeder is changed but<br/>\r\nsettings are cloned from a template feeder.</li>\r\n</ul>\r\n</html>\r\n");
        panelVisionEnabled.add(lblOcrWrongPart, "2, 8, right, default");

        comboBoxWrongPartAction = new JComboBox(ReferencePushPullFeeder.OcrWrongPartAction.values());
        panelVisionEnabled.add(comboBoxWrongPartAction, "4, 8");

        List<String> fontList = OcrUtils.createFontSelectionList(feeder.getOcrFontName(), true);

        lblOcrFontName = new JLabel("OCR Font Name");
        lblOcrFontName.setToolTipText("<html>Name of the OCR font to be recognized or [Barcode].<br/>\r\nMonospace fonts work much better, allow lower resolution and therefore faster <br/>\r\noperation. Use a font where all the used characters are easily distinguishable.<br/>\r\nFonts with clear separation between glyphs are much preferred.</html>");
        panelVisionEnabled.add(lblOcrFontName, "8, 8, right, default");
        comboBoxFontName = new JComboBox(fontList.toArray());
        panelVisionEnabled.add(comboBoxFontName, "10, 8");

        btnSetupocrregion = new JButton(setupOcrRegionAction);
        panelVisionEnabled.add(btnSetupocrregion, "12, 8, 3, 1");

        lblStopAfterWrong = new JLabel("Stop after wrong part?");
        panelVisionEnabled.add(lblStopAfterWrong, "2, 10, right, default");

        checkBoxStopAfterWrongPart = new JCheckBox("");
        panelVisionEnabled.add(checkBoxStopAfterWrongPart, "4, 10");

        lblFontSizept = new JLabel("OCR Font Size [pt]");
        lblFontSizept.setToolTipText("The OCR font size in typographic points (1 pt = 1/72 in).");
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

        lblDiscoverOnJobStart = new JLabel("Check on Job Start?");
        lblDiscoverOnJobStart.setToolTipText("<html>On Job Start, check that the correct parts are selected in OCR-enabled feeders at their locations. <br/>\r\nOtherwise the Job is stopped.<br/>\r\nThis will also vision-calibrate the feeders' locations, if calibration is enabled.</html>");
        panelVisionEnabled.add(lblDiscoverOnJobStart, "2, 12, right, default");

        checkBoxDiscoverOnJobStart = new JCheckBox("");
        panelVisionEnabled.add(checkBoxDiscoverOnJobStart, "4, 12");

        btnOcrAllFeeders = new JButton(allFeederOcrAction);
        panelVisionEnabled.add(btnOcrAllFeeders, "12, 12, 3, 1");
        panelVisionEnabled.add(btnEditPipeline, "2, 16");

        lblVisionType = new JLabel("Vision Type");
        lblVisionType.setToolTipText("<html>\r\n<p>Choose the vision type, then press <strong>Reset Pipeline</strong> to assign the<br/>\r\ndefault pipeline of that type. Sprocket holes are detected as follows:</p>\r\n<ul>\r\n<li><strong>ColorKeyed</strong>: the background under the holes must be of a vivid color<br/>\r\n(green by default).</li>\r\n<li><strong>CircularSymmetry</strong>: the shape of the holes must be circular, their<br/>\r\ninside/outside must be plain.</li>\r\n</ul>\r\n<p>Both types of pipeline will further assess detected holes by size, alignment, pitch<br/>\r\nand expected distance.</p>\r\n</html>");
        panelVisionEnabled.add(lblVisionType, "8, 16, right, default");

        pipelineType = new JComboBox(PipelineType.values());

        panelVisionEnabled.add(pipelineType, "10, 16, fill, default");

        btnResetPipeline = new JButton(resetPipelineAction);
        panelVisionEnabled.add(btnResetPipeline, "12, 16, 3, 1");

        contentPanel.add(panelFields);

        panelCloning = new JPanel();
        panelCloning.setBorder(new TitledBorder(null, "Clone Settings", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        panelFields.add(panelCloning);
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

        lblUsedAsTemplate = new JLabel("Use this one as Template?");
        panelCloning.add(lblUsedAsTemplate, "2, 2, right, default");
        lblUsedAsTemplate.setToolTipText("<html>Use this feeder as a template for cloning settings to other feeders. <br/>\r\nThe templates are matched by tape & reel specification or package of the parts <br/>\r\nloaded in feeders. <br/>\r\nWhen no template matches formally, the feeder \nwith the greatest similarities <br/>\r\nis taken (feed pitch, tape width, proximity, etc.).</html>");

        checkBoxUsedAsTemplate = new JCheckBox("");
        checkBoxUsedAsTemplate.setToolTipText("<html>Use this feeder as a template for cloning settings to other feeders. <br/>\r\nThe templates are matched by tape & reel specification or package of the parts <br/>\r\nloaded in feeders. <br/>\r\nWhen no template matches formally, the feeder \nwith the greatest similarities <br/>\r\nis taken (feed pitch, tape width, proximity, etc.).</html>");
        checkBoxUsedAsTemplate.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                // by setting anything, we fire a property change
                if (btnSmartClone != null) {
                    btnSmartClone.setAction(checkBoxUsedAsTemplate.isSelected() ? feederCloneToAllAction: feederCloneFromTemplate);
                }
                // need to "apply" this immediately
                feeder.setUsedAsTemplate(checkBoxUsedAsTemplate.isSelected());
            }
        });
        panelCloning.add(checkBoxUsedAsTemplate, "4, 2");

        lblCloneLocationSettings = new JLabel("Clone Location Settings?");
        lblCloneLocationSettings.setToolTipText("Clone the X/Y-invariable Location settings, i.e. Pick Location Z and options. ");
        panelCloning.add(lblCloneLocationSettings, "8, 2, right, default");

        checkBoxCloneLocationSettings = new JCheckBox("");
        checkBoxCloneLocationSettings.setSelected(true);
        checkBoxCloneLocationSettings.setToolTipText("Clone the X/Y-invariable Location settings, i.e. Pick Location Z and options. ");
        panelCloning.add(checkBoxCloneLocationSettings, "10, 2");

        btnSmartClone = new JButton(feeder.isUsedAsTemplate() ? feederCloneToAllAction : feederCloneFromTemplate);
        panelCloning.add(btnSmartClone, "14, 2, 1, 7");

        lblTemplate = new JLabel("Template:");
        panelCloning.add(lblTemplate, "2, 4, right, default");

        textPaneCloneTemplateStatus = new JTextPane();
        textPaneCloneTemplateStatus.setMaximumSize(new Dimension(400, 2147483647));
        textPaneCloneTemplateStatus.setText("&nbsp;");
        textPaneCloneTemplateStatus.setBackground(UIManager.getColor("control"));
        textPaneCloneTemplateStatus.setContentType("text/html");
        textPaneCloneTemplateStatus.setEditable(false);
        textPaneCloneTemplateStatus.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        panelCloning.add(textPaneCloneTemplateStatus, "4, 4, 1, 5, default, top");

        lblCloneTapeSetting = new JLabel("Clone Tape Setting?");
        lblCloneTapeSetting.setToolTipText("Clone the Tape Settings. ");
        panelCloning.add(lblCloneTapeSetting, "8, 4, right, default");

        checkBoxCloneTapeSettings = new JCheckBox("");
        checkBoxCloneTapeSettings.setSelected(true);
        checkBoxCloneTapeSettings.setToolTipText("Clone the Tape Settings. ");
        panelCloning.add(checkBoxCloneTapeSettings, "10, 4");

        lblCloneVisionSettings = new JLabel("Clone Vision Settings?");
        lblCloneVisionSettings.setToolTipText("Clone the Vision settings, including the pipeline.");
        panelCloning.add(lblCloneVisionSettings, "8, 6, right, default");

        checkBoxCloneVisionSettings = new JCheckBox("");
        checkBoxCloneVisionSettings.setToolTipText("Clone the Vision settings, including the pipeline.");
        checkBoxCloneVisionSettings.setSelected(true);
        panelCloning.add(checkBoxCloneVisionSettings, "10, 6");

        lblClonePushpullSettings = new JLabel("Clone Push-Pull Settings?");
        lblClonePushpullSettings.setToolTipText("Clone the Push-Pull Motion Settings.");
        panelCloning.add(lblClonePushpullSettings, "8, 8, right, default");

        checkBoxClonePushPullSettings = new JCheckBox("");
        checkBoxClonePushPullSettings.setToolTipText("Clone the Push-Pull Motion Settings.");
        checkBoxClonePushPullSettings.setSelected(true);
        panelCloning.add(checkBoxClonePushPullSettings, "10, 8");
        initDataBindings();
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

        addWrappedBinding(feeder, "usedAsTemplate", checkBoxUsedAsTemplate, "selected");

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
                            +(feeder.isUsedAsTemplate() ? 
                                    "<br/><p color=\"red\">This feeder is marked as template. Are you really, really sure?</p>" : "")
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
    private Action allFeederOcrAction =
            new AbstractAction("All Feeder OCR") {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "<html>Go to all the feeders with OCR and rediscover the parts loaded in them. </html>");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            UiUtils.submitUiMachineTask(() -> {
                StringBuilder report = new StringBuilder();
                feeder.performOcrOnAllFeeders(null, false, report);
                SwingUtilities.invokeLater(() -> {
                    if (report.length() == 0) {
                        report.append("No action taken.");
                    }
                    JOptionPane.showMessageDialog(getTopLevelAncestor(), "<html>"+report+"</html>", "OCR Report", JOptionPane.INFORMATION_MESSAGE);
                });
            });
        }
    };

    private Action setupOcrRegionAction =
            new AbstractAction("Setup OCR Region") {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "<html>Moves the camera to the vision location and lets you select the OCR region of interest.</html>");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            UiUtils.submitUiMachineTask(() -> {
                MovableUtils.moveToLocationAtSafeZ(feeder.getCamera(), feeder.getNominalVisionLocation());
                MovableUtils.fireTargetedUserAction(feeder.getCamera());
                SwingUtilities.invokeAndWait(() -> {
                    UiUtils.messageBoxOnException(() -> {
                        new RegionOfInterestProcess(MainFrame.get(), feeder.getCamera(), "Setup OCR Region") {
                            @Override 
                            public void setResult(RegionOfInterest roi) {
                                try {
                                    feeder.setOcrOffsetLocation();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                feeder.setOcrRegion(roi);
                            }
                        };
                    });
                });
            });
        }
    };

    private Action performOcrAction =
            new AbstractAction("Part by OCR") {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "<html>Perform OCR and assign the recognized part.</html>");
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
                    report.append("No action taken.");
                }
                JOptionPane.showMessageDialog(getTopLevelAncestor(), "<html>"+report+"</html>", "OCR Report", JOptionPane.INFORMATION_MESSAGE);
            });
        }
    };

    private Action feederCloneFromTemplate =
            new AbstractAction("Clone from Template", Icons.importt) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "<html>Clone the settings from the selected template feeder, <br/>transforming any coordinates to the pick location and orientation.</html>");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                if (checkBoxUsedAsTemplate.isSelected()) {
                    throw new Exception("This feeder is used as a template and cannot be overwritten.");
                }
                if (!(checkBoxCloneTapeSettings.isSelected()  
                        || checkBoxClonePushPullSettings.isSelected()
                        || checkBoxCloneVisionSettings.isSelected())) {
                    throw new Exception("Please select some feeder settings to clone.");
                }
                applyAction.actionPerformed(e);
                if (feeder.getTemplateFeeder(null) == null) {
                    throw new Exception("No suitable template feeder found.");
                }
                int result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                        "<html><p>This will overwrite the selected settings with those from the template:<br/><br/>"
                                + feeder.getCloneTemplateStatus()+"<br/><br/>"
                                + "Are you sure?",
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
            new AbstractAction("Clone to Feeders", Icons.export) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "<html>Clone the settings from this feeder to all compatible feeders.</html>");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                if (!checkBoxUsedAsTemplate.isSelected()) {
                    throw new Exception("This feeder is not used as a template.");
                }
                if (!(checkBoxCloneTapeSettings.isSelected()  
                        || checkBoxClonePushPullSettings.isSelected()
                        || checkBoxCloneVisionSettings.isSelected())) {
                    throw new Exception("Please select some feeder settings to clone.");
                }
                applyAction.actionPerformed(e);
                if (feeder.getCompatibleFeeders().size() == 0) {
                    throw new Exception("No suitable feeders found to clone to.");
                }
                int result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                        "<html>This will overwrite the selected settings in all the target feeders:<br/><br/>"
                                + feeder.getCloneTemplateStatus()+"<br/><br/>   "
                                +"Are you sure?</html>",
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
            new AbstractAction("", Icons.add) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "<html>Add one more feeder like this one, advancing in a row.</html>");
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
        JDialog dialog = new CvPipelineEditorDialog(MainFrame.get(), feeder.getName() + " Pipeline", editor);
        dialog.setVisible(true);
    }

    protected void initDataBindings() {
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
