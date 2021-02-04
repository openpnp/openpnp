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

package org.openpnp.gui.wizards;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Locale;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.ActuatorsComboBoxModel;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.gui.support.NamedConverter;
import org.openpnp.machine.reference.AbstractBroadcastingCamera;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.base.AbstractCamera;
import org.openpnp.spi.base.AbstractMachine;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.UiUtils;
import org.pmw.tinylog.Logger;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class CameraConfigurationWizard extends AbstractConfigurationWizard {
    private final AbstractBroadcastingCamera camera;

    private static String uppFormat = "%.8f";
    private static String zFormat = "%.4f";

    private static String downLookingCalibrationInstructions = "<html>\r\n\r\n" +
            "Note: Calibrating Units Per Pixel at two different heights is not manditory but "
            + "does enable additional features. If Dual Height calibration is not desired, "
            + "calibrate the Primary Units Per Pixel at <b>exactly</b> the desired working "
            + "height using the instructions below and then copy those settings to the Secondary "
            + "Units Per Pixel fields.\r\n<ol>\r\n" +
            "<li>Select a rectangular object with a known width and length that will fit within "
            + "the camera's field-of-view. Graphing paper is a good, easy choice for this. Enter "
            + "the object's width and length into the Width and Length fields above.\r\n" +
            "<li>Place the object on the table and align it square with the camera's reticle.\r\n" + 
            "<li>Select the Primary (or Secondary) Units Per Pixel to calibrate using the Cal "
            + "Select radio buttons.  Note that the Primary Units Per Pixel should be calibrated "
            + "near the same Z height as the top surface of the circuit board(s) to be populated "
            + "and the Secondary at least as high as the top of the tallest part that may ever be "
            + "placed.\r\n" + 
            "<li>Jog the nozzle over the object and then down so that it is just touching the "
            + "object.  Press Capture Z.  If calibrating the Primary Units Per Pixel, a dialog "
            + "will pop-up asking if the measurement location height should be used as the "
            + "default working height for the camera.  Answer Yes or No.  (If No, be sure to "
            + "manually populate the Default Working Height field after Units Per Pixel "
            + "calibration is complete.)\r\n" + 
            "<li>Jog the camera to where it is centered over the object.\r\n" + 
            "<li>Press Measure and use the camera selection rectangle to measure the object.  "
            + "If the object is not in perfect focus, use the middle of the blurry edges for the "
            + "measurement.  Press Confirm when finished.\r\n" + 
            "<li>The calculated units per pixel values will be inserted into the X and Y "
            + "fields.\r\n" + 
            "<li>Place a spacer (about as thick as the tallest part that may ever be expected to "
            + "be placed) under the object (or for machines that can physically move the top "
            + "camera in Z, jog the camera up or down by about that much) and repeat steps 3 "
            + "though 7 for the Secondary Units Per Pixel. Note that if the object now appears "
            + "too big to fit within the camera's field-of-view, a smaller object can be used for "
            + "this as long as the Width and Length fields are updated to match its size.\r\n" + 
            "</ol>\r\n</html>";
    
    private static String upLookingCalibrationInstructions = "<html>\r\n\r\n" + 
            "Note: Calibrating Units Per Pixel at two different heights is not manditory but it "
            + "does enable additional features. If Dual Height calibration is not desired, "
            + "calibrate the Primary Units Per Pixel per the instructions below and then copy "
            + "those settings to the Secondary Units Per Pixel fields.\r\n<ol>\r\n" +
            "<li>Select an object with a known width, length, and thickness that will fit within "
            + "the camera's field-of-view. Enter the object's width, length, and thickness into "
            + "the Width, Length, and Thickness fields above.\r\n" + 
            "<li>Place the object square on the table and using a nozzle, pick up the object.\r\n" + 
            "<li>Select the Primary (or Secondary) Units Per Pixel to calibrate using the Cal "
            + "Select radio buttons. Note that the Primary Units Per Pixel is calibrated at the "
            + "up looking camera's location while the Secondary should be calibrated at a higher "
            + "location.\r\n" + 
            "<li>WARNING - if the nozzle is not already over the up looking camera's position, "
            + "the next step will automatically move the nozzle to that position.\r\n" + 
            "<li>Press Measure and use the camera selection rectangle to measure the object.  If "
            + "necessary, use the jog panel to rotate the nozzle so that the object is square with "
            + "the selection rectangle.  If the object is not in perfect focus, use the middle of "
            + "the blurry edges for the measurement.  Press Confirm when finished.\r\n" + 
            "<li>The calculated units per pixel values will be inserted into the X and Y "
            + "fields.\r\n" + 
            "<li>Jog the nozzle up by about the thickness of the tallest part that may be "
            + "expected to be placed and repeat steps 3 though 6 for the Secondary Units Per "
            + "Pixel.\r\n" + 
            "</ol>\r\n</html>";

    protected Location measurementLocation;

    public CameraConfigurationWizard(AbstractBroadcastingCamera camera) {
        AbstractMachine machine = (AbstractMachine) Configuration.get().getMachine();
        this.camera = camera;

        panel = new JPanel();
        panel.setBorder(new TitledBorder(null, "Properties", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panel);
        panel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
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

        lblName = new JLabel("Name");
        panel.add(lblName, "2, 2, right, default");

        nameTf = new JTextField();
        panel.add(nameTf, "4, 2");
        nameTf.setColumns(10);

        lblLooking = new JLabel("Looking");
        panel.add(lblLooking, "2, 4, right, default");

        lookingCb = new JComboBox(Camera.Looking.values());
        lookingCb.addActionListener(lookingAction);
        panel.add(lookingCb, "4, 4");

        lblPreviewFps = new JLabel("Preview FPS");
        panel.add(lblPreviewFps, "2, 6, right, default");

        previewFps = new JTextField();
        panel.add(previewFps, "4, 6, fill, default");
        previewFps.setColumns(10);

        lblSuspendDuringTasks = new JLabel("Suspend during tasks?");
        lblSuspendDuringTasks.setToolTipText("<html>Continuous camera preview is suspended during machine tasks, only frames<br/>\r\ncaptured using computer vision are shown. For high Preview FPS this improves <br/>\r\nperformance </html>");
        panel.add(lblSuspendDuringTasks, "6, 6, right, default");

        suspendPreviewInTasks = new JCheckBox("");
        panel.add(suspendPreviewInTasks, "8, 6");

        lblAutoVisible = new JLabel("Auto Camera View?");
        lblAutoVisible.setToolTipText("<html>If enabled, the CameraView will be automatically selected whenever a<br/>\r\nuser action is related to the camera or when a computer vision result is presented.</html>");
        panel.add(lblAutoVisible, "2, 8, right, default");

        autoVisible = new JCheckBox("");
        panel.add(autoVisible, "4, 8");
        panelLight = new JPanel();
        panelLight.setBorder(new TitledBorder(null, "Light", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelLight);
        panelLight.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
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

        lblLightingActuator = new JLabel("Light Actuator");
        panelLight.add(lblLightingActuator, "2, 2, right, default");

        lightActuator = new JComboBox();
        panelLight.add(lightActuator, "4, 2");
        lightActuator.setMaximumRowCount(12);

        lblAllowMachineActuators = new JLabel("Allow Machine Actuators?");
        panelLight.add(lblAllowMachineActuators, "6, 2, right, default");
        lblAllowMachineActuators.setToolTipText("<html>It is recommended to attach the Light Actuator to the camera's head.<br/>\r\nHowever, for backwards-compatibility with how Light Actuators were used in<br/>\r\nScripts, you can enable this switch and choose a Machine actuator. \r\n</html>\r\n");

        allowMachineActuators = new JCheckBox("");
        panelLight.add(allowMachineActuators, "8, 2");
        allowMachineActuators.setToolTipText("<html>It is recommended to attach the Light Actuator to the camera's head.<br/>\r\nHowever, for backwards-compatibility with how Light Actuators were used in<br/>\r\nScripts, you can enable this switch and choose a Machine actuator. \r\n</html>\r\n");

        lblOn = new JLabel(" ON");
        panelLight.add(lblOn, "4, 6, left, default");

        lblOff = new JLabel("OFF");
        panelLight.add(lblOff, "8, 6");

        lblBeforeCapture = new JLabel("Before Capture?");
        lblBeforeCapture.setToolTipText("<html>\r\nThe light is actuated ON, before this camera is capturing an<br/>\r\nimage for computer vision. \r\n</html>");
        panelLight.add(lblBeforeCapture, "2, 8, right, default");

        beforeCaptureLightOn = new JCheckBox("");
        panelLight.add(beforeCaptureLightOn, "4, 8");

        lblAfterCapture = new JLabel("After Capture?");
        lblAfterCapture.setToolTipText("<html>\r\nThe light is actuated OFF, after this camera has captured an<br/>\r\nimage for computer vision. \r\n</html>");
        panelLight.add(lblAfterCapture, "6, 8, right, default");

        afterCaptureLightOff = new JCheckBox("");
        panelLight.add(afterCaptureLightOff, "8, 8");

        lblUserActionLight = new JLabel("User Camera Action?");
        panelLight.add(lblUserActionLight, "2, 10, right, default");
        lblUserActionLight.setToolTipText("<html>\r\nThe light is actuated ON when a user action is deliberately positioning<br>\r\nor otherwise using the camera. \r\n</html>");

        userActionLightOn = new JCheckBox("");
        panelLight.add(userActionLightOn, "4, 10");

        lblAntiglare = new JLabel("Anti-Glare?");
        lblAntiglare.setToolTipText("<html>\r\nTo prevent glare from this camera light, the light is actuated OFF, <br/>\r\nbefore any other camera looking the opposite way is capturing. \r\n</html>");
        panelLight.add(lblAntiglare, "6, 10, right, default");

        antiGlareLightOff = new JCheckBox("");
        panelLight.add(antiGlareLightOff, "8, 10");
        allowMachineActuators.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                setActuatorModel(machine, camera);
            }
        });

        panelDefaultWorkingPlane = new JPanel();
        panelDefaultWorkingPlane.setBorder(new TitledBorder(
                null,
                "Default Working Height", TitledBorder.LEADING, TitledBorder.TOP, null,
                new Color(0, 0, 0)));
        contentPanel.add(panelDefaultWorkingPlane);
        panelDefaultWorkingPlane.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(57dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,}));

        lblNewLabel_4 = new JLabel("Z");
        panelDefaultWorkingPlane.add(lblNewLabel_4, "4, 2, center, default");

        lblNewLabel_2 = new JLabel("Default Working Height");
        panelDefaultWorkingPlane.add(lblNewLabel_2, "2, 4, right, default");

        textFieldDefaultZ = new JTextField();
        textFieldDefaultZ.setToolTipText("<html>" +
                "This is the Z level at which objects in the camera view are assumed<br>" +
                "to be if their true height is unknown.  Generally this should be set<br>" +
                "to the Z height of the working surface of the circuit board(s) that<br>" +
                "are to be populated.</html>");
        panelDefaultWorkingPlane.add(textFieldDefaultZ, "4, 4, fill, default");
        textFieldDefaultZ.setColumns(8);
        
        btnCaptureToolZ = new JButton(captureToolZAction);
        panelDefaultWorkingPlane.add(btnCaptureToolZ, "9, 4");

        panelUpp = new JPanel();
        contentPanel.add(panelUpp);
        panelUpp.setBorder(new TitledBorder(null,
                "Units Per Pixel", TitledBorder.LEADING, TitledBorder.TOP, null,
                new Color(0, 0, 0)));
        panelUpp.setLayout(new FormLayout(new ColumnSpec[] {
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
                ColumnSpec.decode("max(99dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,},
                new RowSpec[] {
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.UNRELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,}));

        lblX = new JLabel("X");
        panelUpp.add(lblX, "4, 2, center, default");

        lblY = new JLabel("Y");
        panelUpp.add(lblY, "6, 2, center, default");

        lblObjectZ = new JLabel("Height Above Camera");
        panelUpp.add(lblObjectZ, "8, 2, center, default");

        lblNewLabel_3 = new JLabel("Cal Select");
        panelUpp.add(lblNewLabel_3, "10, 2, center, default");

        lblNewLabel = new JLabel("Primary");
        panelUpp.add(lblNewLabel, "2, 4, left, default");

        textFieldPrimaryUppX = new JTextField();
        textFieldPrimaryUppX.setColumns(8);
        panelUpp.add(textFieldPrimaryUppX, "4, 4, fill, default");

        textFieldPrimaryUppY = new JTextField();
        textFieldPrimaryUppY.setColumns(8);
        panelUpp.add(textFieldPrimaryUppY, "6, 4, fill, default");

        textFieldPrimaryUppHeightAboveCamera = new JTextField();
        textFieldPrimaryUppHeightAboveCamera.setToolTipText("<html>" +
            "This is the height above the camera at which the Primary Units Per Pixel<br>" +
            "is calibrated.</html>");
        panelUpp.add(textFieldPrimaryUppHeightAboveCamera, "8, 4, fill, default");
        textFieldPrimaryUppHeightAboveCamera.setColumns(8);

        ButtonGroup bg = new ButtonGroup();

        rdbtnPrimaryUpp = new JRadioButton("");
        panelUpp.add(rdbtnPrimaryUpp, "10, 4, center, default");
        bg.add(rdbtnPrimaryUpp);

        lblNewLabel_1 = new JLabel("Secondary");
        panelUpp.add(lblNewLabel_1, "2, 6, left, default");

        textFieldSecondaryUppX = new JTextField();
        panelUpp.add(textFieldSecondaryUppX, "4, 6, fill, default");
        textFieldSecondaryUppX.setColumns(8);

        textFieldSecondaryUppY = new JTextField();
        panelUpp.add(textFieldSecondaryUppY, "6, 6, fill, default");
        textFieldSecondaryUppY.setColumns(8);

        textFieldSecondaryUppHeightAboveCamera = new JTextField();
        textFieldSecondaryUppHeightAboveCamera.setToolTipText("<html>" +
            "This is the height above the camera at which the Secondary Units Per Pixel<br>" +
            "is calibrated. If dual-height calibration is not to be used, set this to the<br>" +
            "same height as the Primary Units Per Pixel.</html>");
        panelUpp.add(textFieldSecondaryUppHeightAboveCamera, "8, 6, fill, default");
        textFieldSecondaryUppHeightAboveCamera.setColumns(8);

        rdbtnSecondaryUpp = new JRadioButton("");
        panelUpp.add(rdbtnSecondaryUpp, "10, 6, center, default");
        bg.add(rdbtnSecondaryUpp);

        panelCal = new JPanel();
        panelCal.setName("Units Per Pixel Calibration Tool");
        panelCal.setBorder(new TitledBorder(null, "Calibration Tool", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        panelUpp.add(panelCal, "2, 8, 11, 1, fill, fill");
        panelCal.setLayout(new FormLayout(new ColumnSpec[] {
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
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,},
                new RowSpec[] {
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,}));
        cancelMeasureAction.setEnabled(false);

        lblWidth = new JLabel("Width (X)");
        panelCal.add(lblWidth, "2, 2, center, default");

        lblHeight = new JLabel("Length (Y)");
        panelCal.add(lblHeight, "4, 2, center, default");

        lblThickness = new JLabel("Thickness (Z)");
        panelCal.add(lblThickness, "6, 2, center, default");

        textFieldWidth = new JTextField();
        panelCal.add(textFieldWidth, "2, 4");
        textFieldWidth.setText("1");
        textFieldWidth.setColumns(8);

        textFieldHeight = new JTextField();
        panelCal.add(textFieldHeight, "4, 4");
        textFieldHeight.setText("1");
        textFieldHeight.setColumns(8);

        textFieldThickness = new JTextField();
        panelCal.add(textFieldThickness, "6, 4, fill, default");
        textFieldThickness.setText("1");
        textFieldThickness.setColumns(8);

        btnMeasure = new JButton("Measure");
        panelCal.add(btnMeasure, "8, 4");
        btnMeasure.setAction(measureAction);

        btnCaptureZ = new JButton("Capture Z");
        panelCal.add(btnCaptureZ, "10, 4");
        btnCaptureZ.setAction(captureZAction);

        btnCancelMeasure = new JButton("Cancel");
        panelCal.add(btnCancelMeasure, "12, 4");
        btnCancelMeasure.setAction(cancelMeasureAction);

        lblUppInstructions = new JLabel(upLookingCalibrationInstructions);
        panelCal.add(lblUppInstructions, "2, 6, 13, 1");
    }

    protected void setActuatorModel(AbstractMachine machine, AbstractCamera camera) {
        if (camera.getHead() == null) {
            lightActuator.setModel(new ActuatorsComboBoxModel(machine));
            allowMachineActuators.setVisible(false);
            lblAllowMachineActuators.setVisible(false);
        }
        else if (allowMachineActuators.isSelected()) {
            lightActuator.setModel(new ActuatorsComboBoxModel(machine, camera.getHead()));
        }
        else {
            lightActuator.setModel(new ActuatorsComboBoxModel(camera.getHead()));
        }
    }

    @Override
    public void createBindings() {
        AbstractMachine machine = (AbstractMachine) Configuration.get().getMachine();
        LengthConverter uppLengthConverter = new LengthConverter(uppFormat);
        LengthConverter zLengthConverter = new LengthConverter(zFormat);
        DoubleConverter doubleConverter = new DoubleConverter(Configuration.get().getLengthDisplayFormat());
        NamedConverter<Actuator> actuatorConverter = (camera.getHead() != null ? 
                new NamedConverter<>(machine.getActuators(), camera.getHead().getActuators()) 
                : new NamedConverter<>(machine.getActuators()));

        addWrappedBinding(camera, "name", nameTf, "text");
        addWrappedBinding(camera, "looking", lookingCb, "selectedItem");
        addWrappedBinding(camera, "previewFps", previewFps, "text", doubleConverter);
        addWrappedBinding(camera, "suspendPreviewInTasks", suspendPreviewInTasks, "selected");
        addWrappedBinding(camera, "autoVisible", autoVisible, "selected");

        addWrappedBinding(camera, "allowMachineActuators", allowMachineActuators, "selected");
        addWrappedBinding(camera, "lightActuator", lightActuator, "selectedItem", actuatorConverter);

        addWrappedBinding(camera, "beforeCaptureLightOn", beforeCaptureLightOn, "selected");
        addWrappedBinding(camera, "userActionLightOn", userActionLightOn, "selected");
        addWrappedBinding(camera, "afterCaptureLightOff", afterCaptureLightOff, "selected");
        addWrappedBinding(camera, "antiGlareLightOff", antiGlareLightOff, "selected");

        addWrappedBinding(camera, "defaultZ", textFieldDefaultZ, "text", zLengthConverter);

        MutableLocationProxy defaultUnitsPerPixel = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, camera, "unitsPerPixel", defaultUnitsPerPixel, "location");
        addWrappedBinding(defaultUnitsPerPixel, "lengthX", textFieldPrimaryUppX, "text", uppLengthConverter);
        addWrappedBinding(defaultUnitsPerPixel, "lengthY", textFieldPrimaryUppY, "text", uppLengthConverter);
        addWrappedBinding(defaultUnitsPerPixel, "lengthZ", textFieldPrimaryUppHeightAboveCamera, "text", zLengthConverter);

        MutableLocationProxy secondaryUnitsPerPixel = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, camera, "unitsPerPixelSecondary", secondaryUnitsPerPixel, "location");
        addWrappedBinding(secondaryUnitsPerPixel, "lengthX", textFieldSecondaryUppX, "text", uppLengthConverter);
        addWrappedBinding(secondaryUnitsPerPixel, "lengthY", textFieldSecondaryUppY, "text", uppLengthConverter);
        addWrappedBinding(secondaryUnitsPerPixel, "lengthZ", textFieldSecondaryUppHeightAboveCamera, "text", zLengthConverter);

        ComponentDecorators.decorateWithAutoSelect(textFieldPrimaryUppX);
        ComponentDecorators.decorateWithAutoSelect(textFieldPrimaryUppY);
        ComponentDecorators.decorateWithAutoSelect(textFieldPrimaryUppHeightAboveCamera);
        ComponentDecorators.decorateWithLengthConversion(textFieldPrimaryUppX, uppFormat);
        ComponentDecorators.decorateWithLengthConversion(textFieldPrimaryUppY, uppFormat);
        ComponentDecorators.decorateWithLengthConversion(textFieldPrimaryUppHeightAboveCamera, zFormat);

        ComponentDecorators.decorateWithAutoSelect(textFieldSecondaryUppX);
        ComponentDecorators.decorateWithAutoSelect(textFieldSecondaryUppY);
        ComponentDecorators.decorateWithAutoSelect(textFieldSecondaryUppHeightAboveCamera);
        ComponentDecorators.decorateWithLengthConversion(textFieldSecondaryUppX, uppFormat);
        ComponentDecorators.decorateWithLengthConversion(textFieldSecondaryUppY, uppFormat);
        ComponentDecorators.decorateWithLengthConversion(textFieldSecondaryUppHeightAboveCamera, zFormat);

        ComponentDecorators.decorateWithAutoSelect(textFieldDefaultZ);

        ComponentDecorators.decorateWithAutoSelect(nameTf);
        ComponentDecorators.decorateWithAutoSelect(previewFps);
        ComponentDecorators.decorateWithAutoSelect(textFieldWidth);
        ComponentDecorators.decorateWithAutoSelect(textFieldHeight);
        ComponentDecorators.decorateWithAutoSelect(textFieldThickness);
        ComponentDecorators.decorateWithLengthConversion(textFieldDefaultZ, zFormat);
        ComponentDecorators.decorateWithLengthConversion(textFieldWidth, zFormat);
        ComponentDecorators.decorateWithLengthConversion(textFieldHeight, zFormat);
        ComponentDecorators.decorateWithLengthConversion(textFieldThickness, zFormat);

        setActuatorModel(machine, camera);
    }

    private Action lookingAction = new AbstractAction("Looking") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            if ((Camera.Looking) lookingCb.getSelectedItem() == Camera.Looking.Up) {
                panelDefaultWorkingPlane.setVisible(false);
                lblUppInstructions.setText(upLookingCalibrationInstructions);
                lblThickness.setVisible(true);
                textFieldThickness.setVisible(true);
                btnCaptureZ.setVisible(false);
                btnMeasure.setEnabled(true);
            }
            else {
                panelDefaultWorkingPlane.setVisible(true);
                lblUppInstructions.setText(downLookingCalibrationInstructions);
                lblThickness.setVisible(false);
                textFieldThickness.setVisible(false);
                btnCaptureZ.setVisible(true);
                btnMeasure.setEnabled(false);
            }
        }
    };

    private Action measureAction = new AbstractAction("Measure") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (rdbtnPrimaryUpp.isSelected() || rdbtnSecondaryUpp.isSelected()) {
                if ((Camera.Looking) lookingCb.getSelectedItem() == Camera.Looking.Up) {
                    LengthUnit units = Configuration.get().getSystemUnits();
                    Location cameraLocation = camera.getLocation().convertToUnits(units);
                    Nozzle nozzle = MainFrame.get().getMachineControls().getSelectedNozzle();
                    Location nozzleLocation = nozzle.getLocation().convertToUnits(units);
                    double thickness = Double.parseDouble(textFieldThickness.getText());
                    Location desiredNozzleLocation;
                    if (rdbtnPrimaryUpp.isSelected()) {
                        desiredNozzleLocation = cameraLocation.add(
                                new Location(units, 0, 0, thickness, 0));
                    }
                    else {
                        desiredNozzleLocation = cameraLocation.derive(null, null,
                                nozzleLocation.getZ(), nozzleLocation.getRotation());
                    }
                    if (!nozzleLocation.getLengthX().equals(desiredNozzleLocation.getLengthX()) || 
                            !nozzleLocation.getLengthY().equals(desiredNozzleLocation.getLengthY()) ||
                            !nozzleLocation.getLengthZ().equals(desiredNozzleLocation.getLengthZ()) ||
                            nozzleLocation.getRotation() != desiredNozzleLocation.getRotation()) {
                        UiUtils.submitUiMachineTask(() -> {
                            MovableUtils.moveToLocationAtSafeZ(nozzle, desiredNozzleLocation);
                        });
                    }
                    measurementLocation = desiredNozzleLocation.subtract(
                            new Location(units, 0, 0, thickness, 0)).convertToUnits(units);
                    Logger.trace("measurementLocation = " + measurementLocation);
                    if (rdbtnPrimaryUpp.isSelected()) {
                        textFieldDefaultZ.setText(String.format(Locale.US, zFormat,
                                measurementLocation.getZ()));
                    }
                }
                btnMeasure.setAction(confirmMeasureAction);
                cancelMeasureAction.setEnabled(true);
                CameraView cameraView = MainFrame.get().getCameraViews().ensureCameraVisible(camera);
                MovableUtils.fireTargetedUserAction(camera);
                cameraView.setSelectionEnabled(true);
                cameraView.setSelection(0, 0, 100, 100);
            }
            else {
                JOptionPane.showMessageDialog(null,
                        "Select either the Primary or the Secondary Units Per Pixel to calibrate.",
                        "Info", JOptionPane.INFORMATION_MESSAGE, null);
            }
        }
    };

    private Action confirmMeasureAction = new AbstractAction("Confirm") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            btnMeasure.setAction(measureAction);
            cancelMeasureAction.setEnabled(false);
            if ((Camera.Looking) lookingCb.getSelectedItem() == Camera.Looking.Up) {
                btnMeasure.setEnabled(true);
            }
            else {
                btnMeasure.setEnabled(false);
                btnCaptureZ.setEnabled(true);
            }
            CameraView cameraView = MainFrame.get().getCameraViews().getCameraView(camera);
            cameraView.setSelectionEnabled(false);
            Rectangle selection = cameraView.getSelection();
            double width = Double.parseDouble(textFieldWidth.getText());
            double height = Double.parseDouble(textFieldHeight.getText());
            if (rdbtnPrimaryUpp.isSelected()) {
                textFieldPrimaryUppX.setText(String.format(Locale.US, uppFormat,
                        (width / Math.abs(selection.width))));
                textFieldPrimaryUppY.setText(String.format(Locale.US, uppFormat,
                        (height / Math.abs(selection.height))));
            }
            else {
                textFieldSecondaryUppX.setText(String.format(Locale.US, uppFormat,
                        (width / Math.abs(selection.width))));
                textFieldSecondaryUppY.setText(String.format(Locale.US, uppFormat,
                        (height / Math.abs(selection.height))));
            }

            double heightAboveCamera = ((AbstractCamera) camera).
                    getHeightAboveCamera(measurementLocation).getValue();
            //            //Get the camera's actual location (ignoring virtual axis)
            //            Location cameraLocation = camera.getActualLocation();
            //
            //            double heightAboveCamera = measurementLocation.subtract(cameraLocation).getZ();
            if (rdbtnPrimaryUpp.isSelected()) {
                textFieldPrimaryUppHeightAboveCamera.setText(String.format(Locale.US, zFormat,
                        heightAboveCamera));
            }
            else {
                textFieldSecondaryUppHeightAboveCamera.setText(String.format(Locale.US, zFormat,
                        heightAboveCamera));
            }
        }
    };

    private Action captureToolZAction =
            new AbstractAction("", Icons.captureTool) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Capture the Z height that the selected tool is at.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.messageBoxOnException(() -> {
                Location l = MainFrame.get().getMachineControls().getSelectedTool().getLocation();
                Helpers.copyLocationIntoTextFields(l, null, null, textFieldDefaultZ, null);
            });
        }
    };

    private Action captureZAction = new AbstractAction("Capture Z") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (rdbtnPrimaryUpp.isSelected() || rdbtnSecondaryUpp.isSelected()) {
                LengthUnit units = Configuration.get().getSystemUnits();
                Nozzle nozzle = MainFrame.get().getMachineControls().getSelectedNozzle();
                measurementLocation = nozzle.getLocation().convertToUnits(units);
                Logger.trace("measurementLocation = " + measurementLocation);
                if ((Camera.Looking) lookingCb.getSelectedItem() != Camera.Looking.Up) {
                    Location safeLocation = measurementLocation.derive(null, null, nozzle.
                            getSafeZ().convertToUnits(units).getValue(), null);
                    UiUtils.submitUiMachineTask(() -> {
                        MovableUtils.moveToLocationAtSafeZ(nozzle, safeLocation);
                    });
                    if (rdbtnPrimaryUpp.isSelected()) {
                        int selection = JOptionPane.showConfirmDialog(null,
                                "If calibrating the Primary Units Per Pixels at a height different than the height\r\n" +
                                        "of the circuit board surface, the Default Working Height field must be manually set\r\n" +
                                        "to the height of the circuit board surface!  Failure to do so will result in inaccurate\r\n" +
                                        "scaling of objects in the camera's view and camera jogging will not perform as expected.\r\n" +
                                        "\r\n" +
                                        "Set the Default Working Height field to the height of the current calibration\r\n" +
                                        "location (" + measurementLocation.getLengthZ() + ")?",
                                        "Warning!",
                                        JOptionPane.YES_NO_OPTION,
                                        JOptionPane.QUESTION_MESSAGE,
                                        null
                                );
                        if (selection == JOptionPane.YES_OPTION) {
                            textFieldDefaultZ.setText(String.format(Locale.US, zFormat, 
                                    measurementLocation.getZ()));
                        }
                    }
                }
                else {
                    //For an up looking camera, the default z is always the same height as the
                    //camera
                    textFieldDefaultZ.setText(String.format(Locale.US, zFormat,
                            measurementLocation.getZ()));
                }
                btnMeasure.setEnabled(true);
                btnCaptureZ.setEnabled(false);
                btnCancelMeasure.setEnabled(true);
            }
            else {
                JOptionPane.showMessageDialog(null,
                        "Select either the Primary or the Secondary Units Per Pixel to calibrate.",
                        "Info", JOptionPane.INFORMATION_MESSAGE, null);
            }
        }
    };

    private Action cancelMeasureAction = new AbstractAction("Cancel") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            btnMeasure.setAction(measureAction);
            btnCancelMeasure.setEnabled(false);
            cancelMeasureAction.setEnabled(false);
            if ((Camera.Looking) lookingCb.getSelectedItem() == Camera.Looking.Up) {
                btnMeasure.setEnabled(true);
                btnCaptureZ.setEnabled(false);
            }
            else {
                btnMeasure.setEnabled(false);
                btnCaptureZ.setEnabled(true);
            }
            CameraView cameraView = MainFrame.get().getCameraViews().getCameraView(camera);
            cameraView.setSelectionEnabled(false);
        }
    };

    @Override
    protected void saveToModel() {
        super.saveToModel();
        UiUtils.messageBoxOnException(() -> {
            camera.reinitialize(); 
        });
    }

    private JLabel lblLightingActuator;
    private JComboBox lightActuator;
    private JLabel lblAllowMachineActuators;
    private JCheckBox allowMachineActuators;
    private JLabel lblAutoVisible;
    private JCheckBox autoVisible;
    private JPanel panelLight;
    
    private JPanel panelUpp;
    private JButton btnMeasure;
    private JButton btnCancelMeasure;
    private JLabel lblUppInstructions;
    private JTextField textFieldWidth;
    private JTextField textFieldHeight;
    private JTextField textFieldPrimaryUppX;
    private JTextField textFieldPrimaryUppY;
    private JLabel lblWidth;
    private JLabel lblHeight;
    private JLabel lblX;
    private JLabel lblY;
    private JPanel panel;
    private JLabel lblName;
    private JLabel lblLooking;
    private JComboBox lookingCb;
    private JTextField nameTf;
    private JLabel lblPreviewFps;
    private JTextField previewFps;
    private JLabel lblSuspendDuringTasks;
    private JCheckBox suspendPreviewInTasks;
    private JLabel lblUserActionLight;
    private JCheckBox userActionLightOn;
    private JLabel lblAntiglare;
    private JCheckBox antiGlareLightOff;
    private JLabel lblAfterCapture;
    private JCheckBox afterCaptureLightOff;
    private JLabel lblOn;
    private JLabel lblOff;
    private JLabel lblBeforeCapture;
    private JCheckBox beforeCaptureLightOn;
    private JRadioButton rdbtnPrimaryUpp;
    private JRadioButton rdbtnSecondaryUpp;
    private JTextField textFieldSecondaryUppX;
    private JTextField textFieldSecondaryUppY;
    private JLabel lblNewLabel;
    private JLabel lblNewLabel_1;
    private JTextField textFieldPrimaryUppHeightAboveCamera;
    private JTextField textFieldSecondaryUppHeightAboveCamera;
    private JLabel lblObjectZ;
    private JLabel lblNewLabel_3;
    private JButton btnCaptureZ;
    private JPanel panelCal;
    private JTextField textFieldThickness;
    private JLabel lblThickness;
    private JPanel panelDefaultWorkingPlane;
    private JLabel lblNewLabel_2;
    private JTextField textFieldDefaultZ;
    private JLabel lblNewLabel_4;
    private JButton btnCaptureToolZ;
}
