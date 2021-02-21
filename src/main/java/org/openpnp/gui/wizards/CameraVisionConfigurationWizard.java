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

import java.awt.Font;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.SimpleGraphView;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.ActuatorsComboBoxModel;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LongConverter;
import org.openpnp.gui.support.NamedConverter;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Camera;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.base.AbstractCamera;
import org.openpnp.spi.base.AbstractMachine;
import org.openpnp.spi.base.AbstractCamera.SettleMethod;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.UiUtils;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class CameraVisionConfigurationWizard extends AbstractConfigurationWizard {
    private final AbstractCamera camera;
    
    public CameraVisionConfigurationWizard(AbstractCamera camera) {
        AbstractMachine machine = (AbstractMachine) Configuration.get().getMachine();
        this.camera = camera;


        panelVision = new JPanel();
        panelVision.setBorder(new TitledBorder(null, "Camera Settling", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        contentPanel.add(panelVision);
        panelVision.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(50dlu;min)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("min:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.MIN_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.MIN_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        RowSpec.decode("max(70dlu;default):grow"),}));

        lblSettleMethod = new JLabel("Settle Method");
        panelVision.add(lblSettleMethod, "2, 2, 1, 3, right, default");

        settleMethod = new JComboBox(AbstractCamera.SettleMethod.values());
        settleMethod.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                adaptDialog();
            }
        });
        panelVision.add(settleMethod, "4, 2, 1, 3, fill, default");

        lblSettleTimeMs = new JLabel("Settle Time (ms)");
        panelVision.add(lblSettleTimeMs, "8, 2, right, center");

        settleTimeMs = new JTextField();
        panelVision.add(settleTimeMs, "10, 2, fill, center");
        settleTimeMs.setColumns(10);

        lblSettleTimeoutMs = new JLabel("Settle Timeout (ms)");
        panelVision.add(lblSettleTimeoutMs, "8, 4, right, default");

        settleTimeoutMs = new JTextField();
        panelVision.add(settleTimeoutMs, "10, 4, fill, default");
        settleTimeoutMs.setColumns(10);

        lblSettleThreshold = new JLabel("Settle Threshold");
        panelVision.add(lblSettleThreshold, "2, 6, right, default");

        settleThreshold = new JTextField();
        panelVision.add(settleThreshold, "4, 6, fill, default");
        settleThreshold.setColumns(10);

        lblSettleDebounce = new JLabel("Debounce Frames");
        panelVision.add(lblSettleDebounce, "8, 6, right, default");

        settleDebounce = new JTextField();
        panelVision.add(settleDebounce, "10, 6, fill, default");
        settleDebounce.setColumns(10);

        lblSettleFullColor = new JLabel("Color Sensitive?");
        lblSettleFullColor.setToolTipText("Compare as full color image, i.e. difference in colors with same brightness will register.");
        panelVision.add(lblSettleFullColor, "2, 8, right, default");

        settleFullColor = new JCheckBox("");
        settleFullColor.setToolTipText("");
        panelVision.add(settleFullColor, "4, 8");

        panelSettleTest = new JPanel();
        panelVision.add(panelSettleTest, "14, 2, 1, 11, right, bottom");
        panelSettleTest.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.MIN_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.MIN_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.MIN_COLSPEC,},
                new RowSpec[] {
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,}));

        btnTestRear = new JButton(settleTestRearAction);
        panelSettleTest.add(btnTestRear, "4, 2");

        btnTestZ = new JButton(settleTestUpAction);
        panelSettleTest.add(btnTestZ, "6, 2");

        btnTestLeft = new JButton(settleTestLeftAction);
        panelSettleTest.add(btnTestLeft, "2, 4");

        btnSettleTestCam = new JButton(settleTestAction);
        panelSettleTest.add(btnSettleTestCam, "4, 4");

        btnTestRight = new JButton(settleTestRightAction);
        panelSettleTest.add(btnTestRight, "6, 4");

        btnTestFront = new JButton(settleTestFrontAction);
        panelSettleTest.add(btnTestFront, "4, 6");

        btnTestRotate = new JButton(settleTestRotateAction);
        panelSettleTest.add(btnTestRotate, "6, 6");

        lblSettleGradient = new JLabel("Edge Sensitive?");
        lblSettleGradient.setToolTipText("Use the gradients of the images rather than brightness.");
        panelVision.add(lblSettleGradient, "8, 8, right, default");

        settleGradients = new JCheckBox("");
        panelVision.add(settleGradients, "10, 8");

        lblContrastEnhance = new JLabel("Enhance Contrast");
        lblContrastEnhance.setToolTipText("How much it should enhance the contrast from 0.0 (original image) to 1.0 (full dynamic range).");
        panelVision.add(lblContrastEnhance, "2, 10, right, default");

        settleContrastEnhance = new JTextField();
        panelVision.add(settleContrastEnhance, "4, 10, default, top");
        settleContrastEnhance.setColumns(10);

        lblSettleGaussianBlur = new JLabel("Denoise (Pixel)");
        lblSettleGaussianBlur.setToolTipText("<html>\r\nDiameter in pixels of the Gaussian Blur used to denoise the images. <br/>\r\nFor large diameters the image will be scaled down for better speed.\r\n</html>");
        panelVision.add(lblSettleGaussianBlur, "8, 10, right, default");

        settleGaussianBlur = new JTextField();
        panelVision.add(settleGaussianBlur, "10, 10, fill, default");
        settleGaussianBlur.setColumns(10);

        lblSettleMaskCircle = new JLabel("Center Mask");
        lblSettleMaskCircle.setToolTipText("<html>\r\n<p>Size of the central circular mask, relative to the camera dimension <br/>\r\n(height or width, whichever is smaller).</p>\r\n<p>Examples:</p>\r\n<ul>\r\n<li>0.0 No mask</li>\r\n<li>0.5 Circular center area of half the camera view</li>\r\n<li>1.0 Circular center area to the edge of the camera view</li>\r\n<li>1.5 Circular area vignetting the camera view</li>\r\n</ul>\r\n</html>");
        panelVision.add(lblSettleMaskCircle, "2, 12, right, default");

        settleMaskCircle = new JTextField();
        panelVision.add(settleMaskCircle, "4, 12, fill, default");
        settleMaskCircle.setColumns(10);

        lblSettleDiagnostics = new JLabel("Diagnostics?");
        lblSettleDiagnostics.setToolTipText("Enable graphical diagnostics and replay of settle frames.");
        panelVision.add(lblSettleDiagnostics, "8, 12, right, default");

        settleDiagnostics = new JCheckBox("");
        settleDiagnostics.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                adaptDialog();
            }
        });
        panelVision.add(settleDiagnostics, "10, 12");

        lblSettleGraph = new JLabel("<html>\r\n<body style=\"text-align:right\">\r\n<p>\r\nDifference <span style=\"color:#FF0000\">&mdash;&mdash;</span>\r\n</p>\r\n<p>\r\nThreshold <span style=\"color:#00BB00\">&mdash;&mdash;</span>\r\n</p>\r\n<p>\r\nCapture <span style=\"color:#005BD9\">&mdash;&mdash;</span>\r\n</p>\r\n</body>\r\n</html>");
        panelVision.add(lblSettleGraph, "2, 14");

        settleGraph = new SimpleGraphView();
        settleGraph.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("selectedX")) {
                    Double t = settleGraph.getSelectedX();
                    camera.setRecordedImagePlayed(t);
                }
            }
        });
        settleGraph.setFont(new Font("Dialog", Font.PLAIN, 11));
        panelVision.add(settleGraph, "4, 14, 11, 1, default, fill");
    }

    private void adaptDialog() {
        AbstractCamera.SettleMethod method = (SettleMethod) settleMethod.getSelectedItem();
        boolean fixedTime = (method == SettleMethod.FixedTime);

        lblSettleTimeMs.setVisible(fixedTime);
        settleTimeMs.setVisible(fixedTime);
        lblSettleTimeoutMs.setVisible(!fixedTime);
        settleTimeoutMs.setVisible(!fixedTime);

        lblSettleThreshold.setVisible(!fixedTime);
        settleThreshold.setVisible(!fixedTime);

        lblSettleDebounce.setVisible(!fixedTime);
        settleDebounce.setVisible(!fixedTime);

        lblSettleFullColor.setVisible(!fixedTime);
        settleFullColor.setVisible(!fixedTime);

        lblSettleGaussianBlur.setVisible(!fixedTime);
        settleGaussianBlur.setVisible(!fixedTime);

        lblSettleGradient.setVisible(!fixedTime);
        settleGradients.setVisible(!fixedTime);

        lblSettleMaskCircle.setVisible(!fixedTime);
        settleMaskCircle.setVisible(!fixedTime);

        lblContrastEnhance.setVisible(!fixedTime);
        settleContrastEnhance.setVisible(!fixedTime);

        lblSettleDiagnostics.setVisible(!fixedTime);
        settleDiagnostics.setVisible(!fixedTime);

        lblSettleGraph.setVisible(settleDiagnostics.isSelected() && !fixedTime);
        settleGraph.setVisible(settleDiagnostics.isSelected() && !fixedTime);
        panelSettleTest.setVisible(settleDiagnostics.isSelected() && !fixedTime);
    }

    @Override
    public void createBindings() {
        AbstractMachine machine = (AbstractMachine) Configuration.get().getMachine();
        LongConverter longConverter = new LongConverter();
        IntegerConverter intConverter = new IntegerConverter();
        DoubleConverter doubleConverter = new DoubleConverter(Configuration.get().getLengthDisplayFormat());

        addWrappedBinding(camera, "settleMethod", settleMethod, "selectedItem");
        addWrappedBinding(camera, "settleTimeMs", settleTimeMs, "text", longConverter);
        addWrappedBinding(camera, "settleTimeoutMs", settleTimeoutMs, "text", longConverter);
        addWrappedBinding(camera, "settleDebounce", settleDebounce, "text", intConverter);
        addWrappedBinding(camera, "settleThreshold", settleThreshold, "text", doubleConverter);
        addWrappedBinding(camera, "settleFullColor", settleFullColor, "selected");
        addWrappedBinding(camera, "settleGaussianBlur", settleGaussianBlur, "text", intConverter);
        addWrappedBinding(camera, "settleGradients", settleGradients, "selected");
        addWrappedBinding(camera, "settleMaskCircle", settleMaskCircle, "text", doubleConverter);
        addWrappedBinding(camera, "settleContrastEnhance", settleContrastEnhance, "text", doubleConverter);
        addWrappedBinding(camera, "settleDiagnostics", settleDiagnostics, "selected");
        addWrappedBinding(camera, "settleGraph", settleGraph, "graph");

        ComponentDecorators.decorateWithAutoSelect(settleTimeMs);
        ComponentDecorators.decorateWithAutoSelect(settleTimeMs);
        ComponentDecorators.decorateWithAutoSelect(settleTimeoutMs);
        ComponentDecorators.decorateWithAutoSelect(settleDebounce);
        ComponentDecorators.decorateWithAutoSelect(settleThreshold);
        ComponentDecorators.decorateWithAutoSelect(settleGaussianBlur);
        ComponentDecorators.decorateWithAutoSelect(settleMaskCircle);
        ComponentDecorators.decorateWithAutoSelect(settleContrastEnhance);

        if (camera.getHead() != null) {
            // The down-looking camera is moving in X/Y, no Z and Rotation will happen.
            btnTestRotate.setVisible(false);
            btnTestZ.setVisible(false);
        }
        adaptDialog();
    }

    private HeadMountable getJogTool() {
        if (camera.getHead() == null) {
            // Bottom camera - jog the nozzle in front of it.
            return MainFrame.get().getMachineControls().getSelectedNozzle();
        }
        else {
            // Down-looking camera can jog itself.
            return camera;
        }
    }

    protected void settleTestPlanar(ActionEvent e, int x, int y, int c) throws HeadlessException {
        UiUtils.messageBoxOnException(() -> {
            applyAction.actionPerformed(e);
            HeadMountable jogTool = getJogTool();
            if ((x != 0 || y != 0) 
                    && !initialJogWarningDisplayed) {
                int result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                        "<html>This will move "+jogTool.getName()+" for one jog increment and back. <br/>"
                                + ((jogTool instanceof Camera) ? "" : "<span style=\"color:red\">WARNING: No move to Safe Z!</span><br/>")
                                + "Are you sure?</html>",
                                null, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (result == JOptionPane.NO_OPTION) {
                    // cancel this
                    return;
                }
                initialJogWarningDisplayed = true;
            }
            UiUtils.submitUiMachineTask(() -> {
                if (jogTool instanceof Camera) {
                    camera.moveToSafeZ();
                }
                MainFrame.get().getMachineControls().getJogControlsPanel().jogTool(x, y, 0, c, jogTool);
                MainFrame.get().getMachineControls().getJogControlsPanel().jogTool(-x, -y, 0, -c, jogTool);
                camera.lightSettleAndCapture();
            });
        });
    }

    private Action settleTestLeftAction = new AbstractAction("", Icons.arrowRight) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Makes a move to the left and back, then settles the Camera. Uses the Jog increment distance.");
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            settleTestPlanar(e, -1, 0, 0);
        }
    };
    private Action settleTestRightAction = new AbstractAction("", Icons.arrowLeft) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Makes a move to the right and back, then settles the Camera. Uses the Jog increment distance.");
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            settleTestPlanar(e, 1, 0, 0);
        }
    };
    private Action settleTestRearAction = new AbstractAction("", Icons.arrowDown) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Makes a move to the rear and back, then settles the Camera. Uses the Jog increment distance.");
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            settleTestPlanar(e, 0, 1, 0);
        }
    };
    private Action settleTestFrontAction = new AbstractAction("", Icons.arrowUp) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Makes a move to the front and back, then settles the Camera. Uses the Jog increment distance.");
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            settleTestPlanar(e, 0, -1, 0);
        }
    };
    private Action settleTestRotateAction = new AbstractAction("", Icons.rotateCounterclockwise) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Makes a rotation and back, then settles the Camera. Uses the Jog increment.");
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            settleTestPlanar(e, 0, 0, 1);
        }
    };
    private Action settleTestUpAction = new AbstractAction("", Icons.centerTool) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Moves the nozzle to the camera at Safe Z (or just to Safe Z and back), then settles the camera.");
        }
        @Override 
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                applyAction.actionPerformed(e);
                HeadMountable jogTool = getJogTool();
                if (camera.getHead() == null 
                        && jogTool.getLocation().convertToUnits(LengthUnit.Millimeters)
                        .getXyzDistanceTo(camera.getLocation()) > 5.0) {
                    // Go to the camera.
                    UiUtils.submitUiMachineTask(() -> {
                        MovableUtils.moveToLocationAtSafeZ(jogTool, camera.getLocation());
                        camera.lightSettleAndCapture();
                        MovableUtils.fireTargetedUserAction(camera);
                    });
                }
                else {
                    //just perform a SafeZ and back
                    Location location = jogTool.getLocation();
                    UiUtils.submitUiMachineTask(() -> {
                        jogTool.moveToSafeZ();
                        jogTool.moveTo(location);
                        camera.lightSettleAndCapture();
                        MovableUtils.fireTargetedUserAction(camera);
                    });
                }
            });
        }
    };
    private Action settleTestAction = new AbstractAction("", Icons.captureCamera) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Test-Settle the Camera with no motion.");
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                applyAction.actionPerformed(e);
                camera.lightSettleAndCapture();
                MovableUtils.fireTargetedUserAction(camera);
            });
        }
    };

    private JPanel panelVision;
    private JLabel lblSettleTimeMs;
    private JTextField settleTimeMs;
    private JLabel lblSettleMethod;
    private JComboBox settleMethod;
    private JLabel lblSettleTimeoutMs;
    private JTextField settleTimeoutMs;
    private JTextField settleThreshold;
    private JLabel lblSettleThreshold;
    private JTextField settleGaussianBlur;
    private JLabel lblSettleGaussianBlur;
    private JLabel lblSettleFullColor;
    private JCheckBox settleFullColor;
    private JLabel lblSettleMaskCircle;
    private JTextField settleMaskCircle;
    private JLabel lblSettleDiagnostics;
    private JCheckBox settleDiagnostics;
    private SimpleGraphView settleGraph;
    private JLabel lblSettleGraph;
    private JLabel lblSettleGradient;
    private JCheckBox settleGradients;
    private JButton btnTestRight;
    private JButton btnTestRear;
    private JButton btnTestZ;

    private boolean initialJogWarningDisplayed;
    private JButton btnTestLeft;
    private JButton btnTestFront;
    private JButton btnTestRotate;
    private JButton btnSettleTestCam;
    private JPanel panelSettleTest;
    private JLabel lblContrastEnhance;
    private JTextField settleContrastEnhance;
    private JLabel lblSettleDebounce;
    private JTextField settleDebounce;
}
