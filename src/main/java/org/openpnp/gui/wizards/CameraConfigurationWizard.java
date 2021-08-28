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

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.ActuatorsComboBoxModel;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.gui.support.NamedConverter;
import org.openpnp.machine.reference.AbstractBroadcastingCamera;
import org.openpnp.machine.reference.ReferenceCamera.FocusSensingMethod;
import org.openpnp.machine.reference.axis.ReferenceVirtualAxis;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
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

    private final static String uppFormat = "%.8f";

    private final static String basicCalibrationInstructions = "<html>"
            + "<ol>"
            + "<li>Place an object with a known width and length on the table. Graphing paper "
            + "is a good, easy choice for this."
            + "<li>Enter the width and length of the object into the X and Y fields of the Object "
            + "Dimensions."
            + "<li>Top camera: Jog the camera to where it is centered over the object and in focus."
            + "<li>Bottom camera: lay the object flat above the camera so it is in focus."
            + "<li>Press Measure and use the camera selection rectangle to measure the object. "
            + "<li>Press Confirm when finished."
            + "<li>The calculated Units Per Pixel values will be inserted into the X and Y fields."
            + "</ol></html>";

    private final static String downLookingCalibrationInstructions = "<html>"
            + "Calibrating Units Per Pixel at two different Z levels allows OpenPnP to "
            + "correctly interpret object sizes in 3D i.e. at arbitrary distance from the camera. "
            + "If you already have calibrated in 2D, you still need two fresh 3D measurements. "
            + "Calibrating in 3D requires nozzle and camera locations, offsets, Safe Z and basic "
            + "machine motion to be set up. "
            + "<ol>"
            + "<li>Select a rectangular object with a known width and length that will fit within "
            + "the camera's field-of-view. Graphing paper is a good, easy choice for this. Enter "
            + "the object's width and length into the X and Y field of the Object Dimensions."
            + "<li>Place the object on the table. Note that the primary Units Per Pixel should be "
            + "calibrated near the same Z height as the top surface of the circuit board(s) to "
            + "be populated and the Secondary as high as possible with the object still reasonably "
            + "in focus. The object should comfortably fit into the field-of-view at both heights."
            + "<li>Jog the nozzle over the center of the object and then down so that it is just "
            + "touching the surface. "
            + "<li>If calibrating the primary Units Per Pixel, you may now press the "
            + "Capture Nozzle Z button to capture the Default Working Height (circuit board surface)."
            + "<li>Press Measure to automatically position the camera over the object and use the "
            + "camera view selection rectangle to measure the object. "
            + "If the object is not in perfect focus, use the middle of the blurry edges for the "
            + "measurement. Press Confirm when finished."
            + "<li>The calculated Units Per Pixel values will be inserted into the X and Y "
            + "fields. Measurement object/camera Z coordinates are recorded."
            + "<li>Place a spacer under the object for the secondary measurement, or for machines "
            + "that can physically move the top camera in Z, jog the camera up as high as possible "
            + "with the object still reasonably in focus. "
            + "<li>Repeat steps 3 through 6 for the secondary Units Per Pixel. " + "</ol></html>";

    private final static String upLookingCalibrationInstructions = "<html>"
            + "Calibrating Units Per Pixel at two different Z levels allows OpenPnP to "
            + "correctly interpret object sizes in 3D i.e. at arbitrary distance from the camera. "
            + "If you already have calibrated in 2D, you still need two fresh 3D measurements. "
            + "Calibrating in 3D requires nozzle and camera locations, offsets, Safe Z and basic "
            + "machine motion to be set up. "
            + "<ol>"
            + "<li>Select an object with a known width, length, and thickness that will fit within "
            + "the camera's field-of-view. Enter the object's width, length, and thickness into "
            + "the X, Y, and Z fields of the Object Dimensions above."
            + "<li>Place the object square on the table and using a nozzle, pick up the object."
            + "<li>Note that the primary Units Per Pixel is calibrated at the up looking camera's "
            + "location while the secondary should be calibrated at a higher location."
            + "<li>WARNING - if the nozzle is not already over the up looking camera's position, "
            + "the next step will automatically move the nozzle to that position."
            + "<li>Press Measure and use the camera selection rectangle to measure the object.  If "
            + "necessary, use the jog panel to rotate the nozzle so that the object is square with "
            + "the selection rectangle.  If the object is not in perfect focus, use the middle of "
            + "the blurry edges for the measurement.  Press Confirm when finished."
            + "<li>The calculated Units Per Pixel values will be inserted into the X and Y "
            + "fields. Measurement object/camera Z coordinates are recorded."
            + "<li>Jog the nozzle up  as high as possible with the object still reasonably "
            + "in focus. " + "<li>Repeat steps 5 through 6 for the secondary Units Per Pixel."
            + "</ol></html>";

    protected Location measurementLocation;

    public CameraConfigurationWizard(AbstractBroadcastingCamera camera) {
        AbstractMachine machine = (AbstractMachine) Configuration.get()
                                                                 .getMachine();
        this.camera = camera;

        panel = new JPanel();
        panel.setBorder(new TitledBorder(null, "Properties", TitledBorder.LEADING, TitledBorder.TOP,
                null, null));
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
        lookingCb.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                adaptDialog();
            }
        });
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
        lblAutoVisible.setToolTipText(
                "<html>If enabled, the CameraView will be automatically selected whenever a<br/>\r\nuser action is related to the camera or when a computer vision result is presented.</html>");
        panel.add(lblAutoVisible, "2, 8, right, default");

        autoVisible = new JCheckBox("");
        panel.add(autoVisible, "4, 8");
        
        lblShowMultiview = new JLabel("Show in multi camera view?");
        lblShowMultiview.setToolTipText("<html>\r\nShow this camera in the Camera Panel when mutiple cameras are shown.<br/>\r\nFor example this can be switched off for capture card cameras that are <br/>\r\nalready exposed through SwitcherCameras.\r\n</html>");
        panel.add(lblShowMultiview, "6, 8, right, default");
        
        shownInMultiCameraView = new JCheckBox("");
        panel.add(shownInMultiCameraView, "8, 8");
        
        lblFocusSensing = new JLabel("Focus Sensing Method");
        panel.add(lblFocusSensing, "2, 10, right, default");
        
        focusSensingMethod = new JComboBox(FocusSensingMethod.values());
        focusSensingMethod.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                reloadWizard = true;
            }
        });
        panel.add(focusSensingMethod, "4, 10, fill, default");
        panelLight = new JPanel();
        panelLight.setBorder(new TitledBorder(null, "Light", TitledBorder.LEADING, TitledBorder.TOP,
                null, null));
        contentPanel.add(panelLight);
        panelLight.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC,
                        ColumnSpec.decode("max(70dlu;default)"), FormSpecs.RELATED_GAP_COLSPEC,
                        ColumnSpec.decode("max(70dlu;default)"), FormSpecs.RELATED_GAP_COLSPEC,
                        ColumnSpec.decode("max(70dlu;default)"), FormSpecs.RELATED_GAP_COLSPEC,
                        FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        lblLightingActuator = new JLabel("Light Actuator");
        panelLight.add(lblLightingActuator, "2, 2, right, default");

        lightActuator = new JComboBox();
        panelLight.add(lightActuator, "4, 2");
        lightActuator.setMaximumRowCount(12);

        lblAllowMachineActuators = new JLabel("Allow Machine Actuators?");
        panelLight.add(lblAllowMachineActuators, "6, 2, right, default");
        lblAllowMachineActuators.setToolTipText(
                "<html>It is recommended to attach the Light Actuator to the camera's head.<br/>However, for backwards-compatibility with how Light Actuators were used in<br/>Scripts, you can enable this switch and choose a Machine actuator. </html>");

        allowMachineActuators = new JCheckBox("");
        panelLight.add(allowMachineActuators, "8, 2");
        allowMachineActuators.setToolTipText(
                "<html>It is recommended to attach the Light Actuator to the camera's head.<br/>However, for backwards-compatibility with how Light Actuators were used in<br/>Scripts, you can enable this switch and choose a Machine actuator. </html>");

        lblOn = new JLabel(" ON");
        panelLight.add(lblOn, "4, 6, left, default");

        lblOff = new JLabel("OFF");
        panelLight.add(lblOff, "8, 6");

        lblBeforeCapture = new JLabel("Before Capture?");
        lblBeforeCapture.setToolTipText(
                "<html>\r\nThe light is actuated ON, before this camera is capturing an<br/>\r\nimage for computer vision. \r\n</html>");
        panelLight.add(lblBeforeCapture, "2, 8, right, default");

        beforeCaptureLightOn = new JCheckBox("");
        panelLight.add(beforeCaptureLightOn, "4, 8");

        lblAfterCapture = new JLabel("After Capture?");
        lblAfterCapture.setToolTipText(
                "<html>\r\nThe light is actuated OFF, after this camera has captured an<br/>\r\nimage for computer vision. \r\n</html>");
        panelLight.add(lblAfterCapture, "6, 8, right, default");

        afterCaptureLightOff = new JCheckBox("");
        panelLight.add(afterCaptureLightOff, "8, 8");

        lblUserActionLight = new JLabel("User Camera Action?");
        panelLight.add(lblUserActionLight, "2, 10, right, default");
        lblUserActionLight.setToolTipText(
                "<html>\r\nThe light is actuated ON when a user action is deliberately positioning<br>\r\nor otherwise using the camera. \r\n</html>");

        userActionLightOn = new JCheckBox("");
        panelLight.add(userActionLightOn, "4, 10");

        lblAntiglare = new JLabel("Anti-Glare?");
        lblAntiglare.setToolTipText(
                "<html>\r\nTo prevent glare from this camera light, the light is actuated OFF, <br/>\r\nbefore any other camera looking the opposite way is capturing. \r\n</html>");
        panelLight.add(lblAntiglare, "6, 10, right, default");

        antiGlareLightOff = new JCheckBox("");
        panelLight.add(antiGlareLightOff, "8, 10");
        allowMachineActuators.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                setActuatorModel(machine, camera);
            }
        });

        panelUpp = new JPanel();
        contentPanel.add(panelUpp);
        panelUpp.setBorder(new TitledBorder(null, "Units Per Pixel", TitledBorder.LEADING,
                TitledBorder.TOP, null));
        panelUpp.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
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
                FormSpecs.RELATED_GAP_COLSPEC,},
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
                FormSpecs.UNRELATED_GAP_ROWSPEC,
                RowSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_ROWSPEC,}));

        lbldCalibration = new JLabel("3D Calibration?");
        panelUpp.add(lbldCalibration, "2, 2, right, default");

        enableUnitsPerPixel3D = new JCheckBox("");
        enableUnitsPerPixel3D.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                adaptDialog();
            }
        });
        panelUpp.add(enableUnitsPerPixel3D, "4, 2");

        lblX = new JLabel("X");
        panelUpp.add(lblX, "4, 4, center, default");

        lblY = new JLabel("Y");
        panelUpp.add(lblY, "6, 4, center, default");

        lblZ = new JLabel("Z");
        panelUpp.add(lblZ, "8, 4, center, default");

        lblCameraZ = new JLabel("Camera Z");
        panelUpp.add(lblCameraZ, "10, 4, center, default");

        lblCalibrationObject = new JLabel("Object Dimensions");
        lblCalibrationObject.setToolTipText("Dimensions of the Calibration Object");
        panelUpp.add(lblCalibrationObject, "2, 6, right, default");

        textFieldWidth = new JTextField();
        panelUpp.add(textFieldWidth, "4, 6");
        textFieldWidth.setText("1");
        textFieldWidth.setColumns(8);

        textFieldHeight = new JTextField();
        panelUpp.add(textFieldHeight, "6, 6");
        textFieldHeight.setText("1");
        textFieldHeight.setColumns(8);

        textFieldThickness = new JTextField();
        panelUpp.add(textFieldThickness, "8, 6");
        textFieldThickness.setText("1");
        textFieldThickness.setColumns(8);

        lblPrimaryUpp = new JLabel("Units per Pixel");
        panelUpp.add(lblPrimaryUpp, "2, 8, right, default");

        textFieldPrimaryUppX = new JTextField();
        textFieldPrimaryUppX.setColumns(8);
        panelUpp.add(textFieldPrimaryUppX, "4, 8, fill, default");

        textFieldPrimaryUppY = new JTextField();
        textFieldPrimaryUppY.setColumns(8);
        panelUpp.add(textFieldPrimaryUppY, "6, 8, fill, default");

        textFieldPrimaryUppZ = new JTextField();
        panelUpp.add(textFieldPrimaryUppZ, "8, 8, fill, default");
        textFieldPrimaryUppZ.setColumns(8);

        cameraPrimaryZ = new JTextField();
        cameraPrimaryZ.setToolTipText("");
        panelUpp.add(cameraPrimaryZ, "10, 8, fill, default");
        cameraPrimaryZ.setColumns(8);

        btnMeasure1 = new JButton(measure1Action);
        panelUpp.add(btnMeasure1, "12, 8");

        btnCancelMeasure1 = new JButton(cancelMeasure1Action);
        panelUpp.add(btnCancelMeasure1, "14, 8");
        cancelMeasure1Action.setEnabled(false);

        lblSecondaryUpp = new JLabel("2nd Units per Pixel");
        panelUpp.add(lblSecondaryUpp, "2, 10, right, default");

        textFieldSecondaryUppX = new JTextField();
        panelUpp.add(textFieldSecondaryUppX, "4, 10, fill, default");
        textFieldSecondaryUppX.setColumns(8);

        textFieldSecondaryUppY = new JTextField();
        panelUpp.add(textFieldSecondaryUppY, "6, 10, fill, default");
        textFieldSecondaryUppY.setColumns(8);

        textFieldSecondaryUppZ = new JTextField();
        panelUpp.add(textFieldSecondaryUppZ, "8, 10, fill, default");
        textFieldSecondaryUppZ.setColumns(8);

        cameraSecondaryZ = new JTextField();
        cameraSecondaryZ.setToolTipText("");
        panelUpp.add(cameraSecondaryZ, "10, 10, fill, default");
        cameraSecondaryZ.setColumns(8);

        btnMeasure2 = new JButton(measure2Action);
        panelUpp.add(btnMeasure2, "12, 10");

        btnCancelMeasure2 = new JButton(cancelMeasure2Action);
        cancelMeasure2Action.setEnabled(false);
        panelUpp.add(btnCancelMeasure2, "14, 10");
        
                lblDefaultWorkingPlane = new JLabel("Default Working Plane");
                panelUpp.add(lblDefaultWorkingPlane, "2, 12, right, default");
        
                textFieldDefaultZ = new JTextField();
                panelUpp.add(textFieldDefaultZ, "8, 12");
                textFieldDefaultZ.setToolTipText(
                        "<html>" + "This is the Z level at which objects in the camera view are assumed<br>"
                                + "to be if their true height is unknown.  Generally this should be set<br>"
                                + "to the Z height of the working surface of the circuit board(s) that<br>"
                                + "are to be populated.</html>");
                textFieldDefaultZ.setColumns(8);
        
                btnCaptureToolZ = new JButton(captureToolZAction);
                panelUpp.add(btnCaptureToolZ, "12, 12");

        panelCal = new JPanel();
        panelCal.setName("Units Per Pixel Calibration Tool");
        panelCal.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"),
                "Calibration Instructions", TitledBorder.LEADING, TitledBorder.TOP, null));
        panelUpp.add(panelCal, "2, 14, 13, 1, fill, fill");
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
                RowSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_ROWSPEC,}));
        cancelMeasure1Action.setEnabled(false);

        lblUppInstructions = new JLabel(upLookingCalibrationInstructions);
        panelCal.add(lblUppInstructions, "2, 2, 13, 1, default, fill");
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

    private void adaptDialog() {
        boolean enable3D = enableUnitsPerPixel3D.isSelected();
        boolean lookingDown = (lookingCb.getSelectedItem() == Camera.Looking.Down);
        lblDefaultWorkingPlane.setVisible(enable3D && lookingDown);
        textFieldDefaultZ.setVisible(enable3D && lookingDown);
        btnCaptureToolZ.setVisible(enable3D && lookingDown);
        lblUppInstructions.setText(enable3D ? 
                (lookingDown ? downLookingCalibrationInstructions
                        : upLookingCalibrationInstructions)
                : basicCalibrationInstructions);
        textFieldThickness.setVisible(enable3D && !lookingDown);
        textFieldPrimaryUppZ.setVisible(enable3D);
        lblSecondaryUpp.setVisible(enable3D);
        textFieldSecondaryUppX.setVisible(enable3D);
        textFieldSecondaryUppY.setVisible(enable3D);
        textFieldSecondaryUppZ.setVisible(enable3D);
        btnMeasure2.setVisible(enable3D);
        btnCancelMeasure2.setVisible(enable3D);
        lblZ.setVisible(enable3D);
        
        boolean cameraZ = enable3D && lookingDown
                && !(camera.getAxisZ() == null || camera.getAxisZ() instanceof ReferenceVirtualAxis);
        lblCameraZ.setVisible(cameraZ);
        cameraPrimaryZ.setVisible(cameraZ);
        cameraSecondaryZ.setVisible(cameraZ);

        lblFocusSensing.setVisible(camera.getHead() == null);
        focusSensingMethod.setVisible(camera.getHead() == null);
    };

    @Override
    public void createBindings() {
        AbstractMachine machine = (AbstractMachine) Configuration.get().getMachine();
        LengthConverter uppLengthConverter = new LengthConverter(uppFormat);
        LengthConverter lengthConverter = new LengthConverter();
        DoubleConverter doubleConverter = new DoubleConverter(Configuration.get().getLengthDisplayFormat());
        NamedConverter<Actuator> actuatorConverter = (camera.getHead() != null
                ? new NamedConverter<>(machine.getActuators(), camera.getHead().getActuators())
                : new NamedConverter<>(machine.getActuators()));

        addWrappedBinding(camera, "name", nameTf, "text");
        addWrappedBinding(camera, "looking", lookingCb, "selectedItem");
        addWrappedBinding(camera, "previewFps", previewFps, "text", doubleConverter);
        addWrappedBinding(camera, "suspendPreviewInTasks", suspendPreviewInTasks, "selected");
        addWrappedBinding(camera, "autoVisible", autoVisible, "selected");
        addWrappedBinding(camera, "shownInMultiCameraView", shownInMultiCameraView, "selected");
        addWrappedBinding(camera, "focusSensingMethod", focusSensingMethod, "selectedItem");

        addWrappedBinding(camera, "allowMachineActuators", allowMachineActuators, "selected");
        addWrappedBinding(camera, "lightActuator", lightActuator, "selectedItem", actuatorConverter);

        addWrappedBinding(camera, "beforeCaptureLightOn", beforeCaptureLightOn, "selected");
        addWrappedBinding(camera, "userActionLightOn", userActionLightOn, "selected");
        addWrappedBinding(camera, "afterCaptureLightOff", afterCaptureLightOff, "selected");
        addWrappedBinding(camera, "antiGlareLightOff", antiGlareLightOff, "selected");

        addWrappedBinding(camera, "enableUnitsPerPixel3D", enableUnitsPerPixel3D, "selected");

        addWrappedBinding(camera, "defaultZ", textFieldDefaultZ, "text", lengthConverter);

        MutableLocationProxy defaultUnitsPerPixel = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, camera, "unitsPerPixelPrimary", defaultUnitsPerPixel, "location");
        addWrappedBinding(defaultUnitsPerPixel, "lengthX", textFieldPrimaryUppX, "text",
                uppLengthConverter);
        addWrappedBinding(defaultUnitsPerPixel, "lengthY", textFieldPrimaryUppY, "text",
                uppLengthConverter);
        addWrappedBinding(defaultUnitsPerPixel, "lengthZ", textFieldPrimaryUppZ, "text",
                lengthConverter);

        addWrappedBinding(camera, "cameraPrimaryZ", cameraPrimaryZ, "text", lengthConverter);

        MutableLocationProxy secondaryUnitsPerPixel = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, camera, "unitsPerPixelSecondary", secondaryUnitsPerPixel,
                "location");
        addWrappedBinding(secondaryUnitsPerPixel, "lengthX", textFieldSecondaryUppX, "text",
                uppLengthConverter);
        addWrappedBinding(secondaryUnitsPerPixel, "lengthY", textFieldSecondaryUppY, "text",
                uppLengthConverter);
        addWrappedBinding(secondaryUnitsPerPixel, "lengthZ", textFieldSecondaryUppZ, "text",
                lengthConverter);

        addWrappedBinding(camera, "cameraSecondaryZ", cameraSecondaryZ, "text", lengthConverter);

        ComponentDecorators.decorateWithAutoSelect(textFieldPrimaryUppX);
        ComponentDecorators.decorateWithAutoSelect(textFieldPrimaryUppY);
        ComponentDecorators.decorateWithAutoSelect(textFieldPrimaryUppZ);
        ComponentDecorators.decorateWithAutoSelect(cameraPrimaryZ);
        ComponentDecorators.decorateWithLengthConversion(textFieldPrimaryUppX, uppFormat);
        ComponentDecorators.decorateWithLengthConversion(textFieldPrimaryUppY, uppFormat);
        ComponentDecorators.decorateWithLengthConversion(textFieldPrimaryUppZ);
        ComponentDecorators.decorateWithLengthConversion(cameraPrimaryZ);

        ComponentDecorators.decorateWithAutoSelect(textFieldSecondaryUppX);
        ComponentDecorators.decorateWithAutoSelect(textFieldSecondaryUppY);
        ComponentDecorators.decorateWithAutoSelect(textFieldSecondaryUppZ);
        ComponentDecorators.decorateWithAutoSelect(cameraSecondaryZ);
        ComponentDecorators.decorateWithLengthConversion(textFieldSecondaryUppX, uppFormat);
        ComponentDecorators.decorateWithLengthConversion(textFieldSecondaryUppY, uppFormat);
        ComponentDecorators.decorateWithLengthConversion(textFieldSecondaryUppZ);
        ComponentDecorators.decorateWithLengthConversion(cameraSecondaryZ);

        ComponentDecorators.decorateWithAutoSelect(textFieldDefaultZ);

        ComponentDecorators.decorateWithAutoSelect(nameTf);
        ComponentDecorators.decorateWithAutoSelect(previewFps);
        ComponentDecorators.decorateWithAutoSelect(textFieldWidth);
        ComponentDecorators.decorateWithAutoSelect(textFieldHeight);
        ComponentDecorators.decorateWithAutoSelect(textFieldThickness);
        ComponentDecorators.decorateWithLengthConversion(textFieldDefaultZ);
        ComponentDecorators.decorateWithLengthConversion(textFieldWidth);
        ComponentDecorators.decorateWithLengthConversion(textFieldHeight);
        ComponentDecorators.decorateWithLengthConversion(textFieldThickness);

        setActuatorModel(machine, camera);
        adaptDialog();
    }

    private Action captureToolZAction = new AbstractAction("", Icons.captureTool) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Capture the Z height that the selected tool is at.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.messageBoxOnException(() -> {
                Location l = MainFrame.get()
                                      .getMachineControls()
                                      .getSelectedTool()
                                      .getLocation();
                Helpers.copyLocationIntoTextFields(l, null, null, textFieldDefaultZ, null);
            });
        }
    };

    private Action measure1Action = new AbstractAction("Measure") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            btnMeasure1.setAction(confirmMeasure1Action);
            cancelMeasure1Action.setEnabled(true);
            measure2Action.setEnabled(false);
            try {
                measure(1);
            }
            catch (Exception e) {
                MessageBoxes.errorBox(MainFrame.get(), "Error", e);
                cancelMeasure1Action.actionPerformed(null);
            }
        }
    };

    private Action confirmMeasure1Action = new AbstractAction("Confirm") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            btnMeasure1.setAction(measure1Action);
            cancelMeasure1Action.setEnabled(false);
            measure2Action.setEnabled(true);
            confirmMeasure(1);
        }
    };

    private Action cancelMeasure1Action = new AbstractAction("Cancel") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            btnMeasure1.setAction(measure1Action);
            cancelMeasure1Action.setEnabled(false);
            measure2Action.setEnabled(true);
            CameraView cameraView = MainFrame.get()
                                             .getCameraViews()
                                             .getCameraView(camera);
            cameraView.setSelectionEnabled(false);
        }
    };

    private Action measure2Action = new AbstractAction("Measure") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            btnMeasure2.setAction(confirmMeasure2Action);
            cancelMeasure2Action.setEnabled(true);
            measure1Action.setEnabled(false);
            try {
                measure(2);
            }
            catch (Exception e) {
                MessageBoxes.errorBox(MainFrame.get(), "Error", e);
                cancelMeasure2Action.actionPerformed(null);
            }

        }
    };

    private Action confirmMeasure2Action = new AbstractAction("Confirm") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            btnMeasure2.setAction(measure2Action);
            cancelMeasure2Action.setEnabled(false);
            measure1Action.setEnabled(true);
            confirmMeasure(2);
        }
    };

    private Action cancelMeasure2Action = new AbstractAction("Cancel") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            btnMeasure2.setAction(measure2Action);
            cancelMeasure2Action.setEnabled(false);
            measure1Action.setEnabled(true);
            CameraView cameraView = MainFrame.get()
                                             .getCameraViews()
                                             .getCameraView(camera);
            cameraView.setSelectionEnabled(false);
        }
    };

    @Override
    protected void saveToModel() {
        super.saveToModel();
        if (reloadWizard) {
            MainFrame.get().getMachineSetupTab().selectCurrentTreePath();
        }
        UiUtils.messageBoxOnException(() -> {
            camera.reinitialize();
        });
    }

    protected void measure(int measurement) throws Exception {
        LengthUnit units = Configuration.get()
                .getSystemUnits();
        Nozzle nozzle = MainFrame.get()
                .getMachineControls()
                .getSelectedNozzle();
        if (!enableUnitsPerPixel3D.isSelected()) {
            measurementLocation = null; 
        }
        else if (lookingCb.getSelectedItem() == Camera.Looking.Up) {
            Location cameraLocation = camera.getLocation()
                    .convertToUnits(units);
            Location nozzleLocation = nozzle.getLocation()
                    .convertToUnits(units);
            double thickness = Double.parseDouble(textFieldThickness.getText());
            Location desiredNozzleLocation;
            if (measurement == 1) {
                desiredNozzleLocation =
                        cameraLocation.add(new Location(units, 0, 0, thickness, 0));
            }
            else {
                desiredNozzleLocation = cameraLocation.derive(null, null, nozzleLocation.getZ(),
                        nozzleLocation.getRotation());
                
            }
            measurementLocation =
                    desiredNozzleLocation.subtract(new Location(units, 0, 0, thickness, 0))
                    .convertToUnits(units);
            if (measurement == 2 
                    && Math.abs(measurementLocation.getZ() - Double.parseDouble(textFieldPrimaryUppZ.getText())) 
                    < new Length(1, LengthUnit.Millimeters).convertToUnits(units).getValue()) {
                throw new Exception("Secondary measurement must be at sufficiently different Z. Please move the nozzle with object up/down in Z first.");
            }
            if (!nozzleLocation.equals(desiredNozzleLocation)) {
                int result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                        "<html>This will position the nozzle "+nozzle.getName()+" over the camera "+camera.getName()+". <br/><br/>"
                                + "<span style=\"color:red;\">CAUTION:</span> Nozzle head offset, nozzle Safe Z, camera <br/>"
                                + "location, basic motion etc. must already be set up.<br/><br/>"
                                + "Are you sure?</html>",
                                null, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (result == JOptionPane.NO_OPTION) {
                    throw new Exception("Measurement aborted.");
                }
                UiUtils.submitUiMachineTask(() -> {
                    MovableUtils.moveToLocationAtSafeZ(nozzle, desiredNozzleLocation);
                    MovableUtils.fireTargetedUserAction(nozzle);
                });
            }
        }
        else {
            measurementLocation = nozzle.getLocation()
                    .convertToUnits(units);
            int result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                    "<html>This will move camera "+camera.getName()+" over to the position of the nozzle "+nozzle.getName()+". <br/><br/>"
                            + "<span style=\"color:red;\">CAUTION:</span> Nozzle head offset, nozzle Safe Z, basic <br/>"
                            + "motion etc. must already be set up.<br/><br/>"
                            + "Are you sure?</html>",
                            null, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (result == JOptionPane.NO_OPTION) {
                throw new Exception("Measurement aborted.");
            }
            UiUtils.submitUiMachineTask(() -> {
                MovableUtils.moveToLocationAtSafeZ(camera, measurementLocation);
                MovableUtils.fireTargetedUserAction(camera);
            });
        }
        Logger.trace("measurementLocation = " + measurementLocation);
        CameraView cameraView = MainFrame.get()
                .getCameraViews()
                .ensureCameraVisible(camera);
        cameraView.setSelectionEnabled(true);
        cameraView.setSelection(0, 0, 100, 100);
    }

    protected void confirmMeasure(int measurement) throws NumberFormatException {
        LengthUnit units = Configuration.get()
                                        .getSystemUnits();
        LengthConverter uppLengthConverter = new LengthConverter(uppFormat);
        LengthConverter lengthConverter = new LengthConverter();
        CameraView cameraView = MainFrame.get()
                .getCameraViews()
                .getCameraView(camera);
        cameraView.setSelectionEnabled(false);
        Rectangle selection = cameraView.getSelection();
        double width = Double.parseDouble(textFieldWidth.getText());
        double height = Double.parseDouble(textFieldHeight.getText());
        Location cameraLocation = ((AbstractCamera) camera).getCameraPhysicalLocation();
        if (measurement == 1) {
            textFieldPrimaryUppX.setText(uppLengthConverter.convertForward(
                    new Length(width / Math.abs(selection.width), units)));
            textFieldPrimaryUppY.setText(uppLengthConverter.convertForward(
                    new Length(height / Math.abs(selection.height), units)));
            if (measurementLocation != null) {
                // 3D measurement
                textFieldPrimaryUppZ.setText(
                        lengthConverter.convertForward(measurementLocation.getLengthZ()));
                cameraPrimaryZ.setText(lengthConverter.convertForward(cameraLocation.getLengthZ()));
                if (lookingCb.getSelectedItem() == Camera.Looking.Up) {
                    textFieldDefaultZ.setText(
                            lengthConverter.convertForward(measurementLocation.getLengthZ()));
                }
            }
        }
        else {
            textFieldSecondaryUppX.setText(uppLengthConverter.convertForward(
                    new Length(width / Math.abs(selection.width), units)));
            textFieldSecondaryUppY.setText(uppLengthConverter.convertForward(
                    new Length(height / Math.abs(selection.height), units)));
            if (measurementLocation != null) {
                // 3D measurement
                textFieldSecondaryUppZ.setText(
                        lengthConverter.convertForward(measurementLocation.getLengthZ()));
                cameraSecondaryZ.setText(lengthConverter.convertForward(cameraLocation.getLengthZ()));
            }
        }
    }

    private JLabel lblLightingActuator;
    private JComboBox lightActuator;
    private JLabel lblAllowMachineActuators;
    private JCheckBox allowMachineActuators;
    private JLabel lblAutoVisible;
    private JCheckBox autoVisible;
    private JPanel panelLight;

    private JPanel panelUpp;
    private JButton btnMeasure1;
    private JButton btnCancelMeasure1;
    private JLabel lblUppInstructions;
    private JTextField textFieldWidth;
    private JTextField textFieldHeight;
    private JTextField textFieldPrimaryUppX;
    private JTextField textFieldPrimaryUppY;
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
    private JTextField textFieldSecondaryUppX;
    private JTextField textFieldSecondaryUppY;
    private JLabel lblPrimaryUpp;
    private JLabel lblSecondaryUpp;
    private JTextField cameraPrimaryZ;
    private JTextField cameraSecondaryZ;
    private JLabel lblCameraZ;
    private JPanel panelCal;
    private JTextField textFieldThickness;
    private JLabel lblDefaultWorkingPlane;
    private JTextField textFieldDefaultZ;
    private JButton btnCaptureToolZ;
    private JLabel lblZ;
    private JTextField textFieldPrimaryUppZ;
    private JTextField textFieldSecondaryUppZ;
    private JButton btnMeasure2;
    private JButton btnCancelMeasure2;
    private JLabel lblCalibrationObject;
    private JLabel lbldCalibration;
    private JCheckBox enableUnitsPerPixel3D;
    private JLabel lblShowMultiview;
    private JCheckBox shownInMultiCameraView;
    private JLabel lblFocusSensing;
    private JComboBox focusSensingMethod;
    private boolean reloadWizard;
}
