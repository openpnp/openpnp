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

package org.openpnp.gui.components;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.openpnp.Translations;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.Icons;
import org.openpnp.machine.reference.ContactProbeNozzle;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.Location;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.HeadMountable;
import org.openpnp.util.Cycles;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.UiUtils;

/**
 * A JPanel of 4 small buttons that assist in setting locations. The buttons are Capture Camera
 * Coordinates, Capture Tool Coordinates, Move Camera to Coordinates and Move Tool to Coordinates.
 * If the actuatorId property is set, this causes the component to use the specified Actuator in
 * place of the tool.
 */
@SuppressWarnings("serial")
public class LocationButtonsPanel extends JPanel {
    private JTextField textFieldX, textFieldY, textFieldZ, textFieldC;
    private String actuatorName;

    private JButton buttonCenterTool;
    private JButton buttonCaptureCamera;
    private JButton buttonCaptureTool;
    private JButton buttonCenterToolNoSafeZ;
    private JButton buttonContactProbeTool;
    private JSeparator separator;
    private JButton buttonCenterCamera;

    private Location baseLocation;
    private boolean baseLocationVectorial;
    private boolean contactProbeReference;

    public LocationButtonsPanel(JTextField textFieldX, JTextField textFieldY, JTextField textFieldZ,
            JTextField textFieldC) {
        FlowLayout flowLayout = (FlowLayout) getLayout();
        flowLayout.setVgap(0);
        flowLayout.setHgap(2);
        this.textFieldX = textFieldX;
        this.textFieldY = textFieldY;
        this.textFieldZ = textFieldZ;
        this.textFieldC = textFieldC;

        buttonCenterCamera = new JButton(positionCameraAction);
        buttonCenterCamera.setHideActionText(true);
        add(buttonCenterCamera);

        buttonCenterTool = new JButton(positionToolAction);
        buttonCenterTool.setHideActionText(true);
        add(buttonCenterTool);

        buttonCenterToolNoSafeZ = new JButton(positionToolNoSafeZAction);
        buttonCenterToolNoSafeZ.setHideActionText(true);

        buttonContactProbeTool = new JButton(contactProbeNozzleAction);
        buttonContactProbeTool.setHideActionText(true);

        separator = new JSeparator();
        separator.setOrientation(SwingConstants.VERTICAL);
        add(separator);

        buttonCaptureCamera = new JButton(captureCameraCoordinatesAction);
        buttonCaptureCamera.setHideActionText(true);
        add(buttonCaptureCamera);

        buttonCaptureTool = new JButton(captureToolCoordinatesAction);
        buttonCaptureTool.setHideActionText(true);
        add(buttonCaptureTool);

        setActuatorName(null);
    }

    public Location getBaseLocation() {
        return baseLocation;
    }

    public void setBaseLocation(Location baseLocation) {
        this.baseLocation = baseLocation;
    }

    public boolean isBaseLocationVectorial() {
        return baseLocationVectorial;
    }

    public void setBaseLocationVectorial(boolean baseLocationVectorial) {
        this.baseLocationVectorial = baseLocationVectorial;
    }

    @Override
    public void setEnabled(boolean enabled) {
        buttonCenterCamera.setEnabled(enabled);
        buttonCenterTool.setEnabled(enabled);
        buttonCenterToolNoSafeZ.setEnabled(enabled);
        buttonContactProbeTool.setEnabled(enabled);
        buttonCaptureCamera.setEnabled(enabled);
        buttonCaptureTool.setEnabled(enabled);
        super.setEnabled(enabled);
    }

    public void setShowCameraButtons(boolean shown) {
        if (shown) {
            // not implemented
        }
        else {
            remove(buttonCenterCamera);
            remove(buttonCaptureCamera);
            remove(separator);
        }
        validate();
    }

    public void setShowToolButtons(boolean shown) {
        if (shown) {
            // not implemented
        }
        else {
            remove(buttonCenterTool);
            remove(buttonCenterToolNoSafeZ);
            remove(buttonContactProbeTool);
            remove(buttonCaptureTool);
            remove(separator);
        }
        validate();
    }

    public void setShowPositionToolNoSafeZ(boolean b) {
        if (b) {
            add(buttonCenterToolNoSafeZ, 2);
        }
        else {
            remove(buttonCenterToolNoSafeZ);
        }
    }

    public void setShowContactProbeTool(boolean b) {
        if (b) {
            add(buttonContactProbeTool, 2);
        }
        else {
            remove(buttonContactProbeTool);
        }
    }

    public boolean isContactProbeReference() {
        return contactProbeReference;
    }

    public void setContactProbeReference(boolean contactProbeReference) {
        this.contactProbeReference = contactProbeReference;
    }

    public void setActuatorName(String actuatorName) {
        this.actuatorName = actuatorName;
        if (actuatorName == null || actuatorName.trim().length() == 0) {
            buttonCaptureTool.setAction(captureToolCoordinatesAction);
            buttonCenterTool.setAction(positionToolAction);
            buttonCenterToolNoSafeZ.setAction(positionToolNoSafeZAction);
        }
        else {
            buttonCaptureTool.setAction(captureActuatorCoordinatesAction);
            buttonCenterTool.setAction(positionActuatorAction);
            buttonCenterToolNoSafeZ.setAction(positionActuatorNoSafeZAction);
        }
    }

    public String getActuatorName() {
        return actuatorName;
    }

    public HeadMountable getTool() throws Exception {
        return MainFrame.get().getMachineControls().getSelectedNozzle();
    }

    public Camera getCamera() throws Exception {
        return getTool().getHead().getDefaultCamera();
    }

    /**
     * Get the Actuator with the name provided by setActuatorName() that is on the same Head as the
     * tool from getTool().
     *
     * @return
     * @throws Exception
     */
    public Actuator getActuator() throws Exception {
        if (actuatorName == null) {
            return null;
        }
        HeadMountable tool = getTool();
        Head head = tool.getHead();
        Actuator actuator = head.getActuatorByName(actuatorName);
        if (actuator == null) {
            throw new Exception(String.format("No Actuator with name %s on Head %s", actuatorName,
                    head.getName()));
        }
        return actuator;
    }

    private Location getParsedLocation() {
        double x = 0, y = 0, z = 0, rotation = 0;
        if (textFieldX != null && ! textFieldX.getText().isEmpty()) {
            x = Length.parse(textFieldX.getText()).getValue();
        }
        if (textFieldY != null && ! textFieldY.getText().isEmpty()) {
            y = Length.parse(textFieldY.getText()).getValue();
        }
        if (textFieldZ != null && ! textFieldZ.getText().isEmpty()) {
            z = Length.parse(textFieldZ.getText()).getValue();
        }
        if (textFieldC != null && ! textFieldC.getText().isEmpty()) {
            rotation = Double.parseDouble(textFieldC.getText());
        }
        return new Location(Configuration.get().getSystemUnits(), x, y, z, rotation);
    }

    private Action captureCameraCoordinatesAction =
            new AbstractAction(Translations.getString("LocalButtonsPanel.Action.CaptureCameraCoordinates"),
                    Icons.captureCamera) {
                {
                    putValue(Action.SHORT_DESCRIPTION,Translations.getString(
                            "LocalButtonsPanel.Action.CaptureCameraCoordinates.Description"));
                }

                @Override
                public void actionPerformed(ActionEvent arg0) {
                    UiUtils.submitUiMachineTask(() -> {
                        Location l = getCamera().getLocation();
                        Location lz = Cycles.zProbe(l);
                        if (lz != null) {
                            l = lz;
                        }
                        if (baseLocation != null) {
                            l = l.subtractWithRotation(baseLocation);
                            if (!isBaseLocationVectorial()) {
                                l = l.rotateXy(-baseLocation.getRotation());
                            }
                        }
                        final Location lf = l;
                        SwingUtilities.invokeAndWait(() -> {
                            Helpers.copyLocationIntoTextFields(lf,
                                    textFieldX,
                                    textFieldY,
                                    lz == null ? null : textFieldZ,
                                    textFieldC);
                        });
                    });
                }
            };

    private Action captureToolCoordinatesAction =
            new AbstractAction(Translations.getString("LocalButtonsPanel.Action.CaptureToolCoordinates"),
                    Icons.captureTool) {
                {
                    putValue(Action.SHORT_DESCRIPTION, Translations.getString(
                            "LocalButtonsPanel.Action.CaptureToolCoordinates.Description"));
                }

                @Override
                public void actionPerformed(ActionEvent arg0) {
                    UiUtils.submitUiMachineTask(() -> {
                        Location l = getTool().getLocation();
                        if (baseLocation != null) {
                            l = l.subtractWithRotation(baseLocation);
                            if (!isBaseLocationVectorial()) {
                                l = l.rotateXy(-baseLocation.getRotation());
                            }
                        }
                        final Location lf = l;
                        SwingUtilities.invokeAndWait(() -> {
                            Helpers.copyLocationIntoTextFields(lf, textFieldX, textFieldY, textFieldZ,
                                    textFieldC);
                        });
                    });
                }
            };

    private Action captureActuatorCoordinatesAction =
            new AbstractAction(Translations.getString("LocalButtonsPanel.Action.CaptureActuatorCoordinates"
            ), Icons.capturePin) {
                {
                    putValue(Action.SHORT_DESCRIPTION, Translations.getString(
                            "LocalButtonsPanel.Action.CaptureActuatorCoordinates.Description"));
                }

                @Override
                public void actionPerformed(ActionEvent arg0) {
                    UiUtils.submitUiMachineTask(() -> {
                        Actuator actuator = getActuator();
                        if (actuator == null) {
                            return;
                        }
                        Location l = actuator.getLocation();
                        if (baseLocation != null) {
                            l = l.subtractWithRotation(baseLocation);
                            if (!isBaseLocationVectorial()) {
                                l = l.rotateXy(-baseLocation.getRotation());
                            }
                        }
                        final Location lf = l;
                        SwingUtilities.invokeAndWait(() -> {
                            Helpers.copyLocationIntoTextFields(lf, textFieldX,
                                    textFieldY, textFieldZ, textFieldC);
                        });
                    });

                }
            };

    private Action positionCameraAction =
            new AbstractAction(Translations.getString("LocalButtonsPanel.Action.PositionCamera"
            ), Icons.centerCamera) {
                {
                    putValue(Action.SHORT_DESCRIPTION, Translations.getString(
                            "LocalButtonsPanel.Action.PositionCamera.Description"));
                }

                @Override
                public void actionPerformed(ActionEvent arg0) {
                    UiUtils.submitUiMachineTask(() -> {
                        Camera camera = getCamera();
                        Location location = getParsedLocation();
                        if (baseLocation != null) {
                            if (!isBaseLocationVectorial()) {
                                location = location.rotateXy(baseLocation.getRotation());
                            }
                            location = location.addWithRotation(baseLocation);
                        }
                        MovableUtils.moveToLocationAtSafeZ(camera, location);
                        MovableUtils.fireTargetedUserAction(camera);

                        Map<String, Object> globals = new HashMap<>();
                        globals.put("camera", camera);
                        Configuration.get().getScripting().on("Camera.AfterPosition", globals);
                    });
                }
            };

    private Action positionToolAction = new AbstractAction(Translations.getString(
            "LocalButtonsPanel.Action.PositionTool"), Icons.centerTool) {
        {
            putValue(Action.SHORT_DESCRIPTION, Translations.getString(
                    "LocalButtonsPanel.Action.PositionTool.Description"));
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                HeadMountable tool = getTool();
                Location location = getParsedLocation();
                if (baseLocation != null) {
                    if (!isBaseLocationVectorial()) {
                        location = location.rotateXy(baseLocation.getRotation());
                    }
                    location = location.addWithRotation(baseLocation);
                }
                MovableUtils.moveToLocationAtSafeZ(tool, location);
                MovableUtils.fireTargetedUserAction(tool);
            });
        }
    };

    private Action positionToolNoSafeZAction =
            new AbstractAction(Translations.getString("LocalButtonsPanel.Action.PositionToolNoSafeZ"),
                    Icons.centerToolNoSafeZ) {
        {
            putValue(Action.SHORT_DESCRIPTION, Translations.getString(
                    "LocalButtonsPanel.Action.PositionToolNoSafeZ.Description"));
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                HeadMountable tool = getTool();
                Location location = getParsedLocation();
                if (baseLocation != null) {
                    if (!isBaseLocationVectorial()) {
                        location = location.rotateXy(baseLocation.getRotation());
                    }
                    location = location.addWithRotation(baseLocation);
                }
                tool.moveTo(location);
                MovableUtils.fireTargetedUserAction(tool);
            });
        }
    };

    private Action contactProbeNozzleAction =
            new AbstractAction(Translations.getString("LocalButtonsPanel.Action.ContactProbeNozzle"),
                    Icons.contactProbeNozzle) {
        {
            putValue(Action.SHORT_DESCRIPTION, Translations.getString(
                            "LocalButtonsPanel.Action.ContactProbeNozzle.Description"));
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            int result = JOptionPane.YES_OPTION;
            if (isContactProbeReference()) {
                /// Warn the user.
                result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                        "<html>This will overwrite the Z reference and therefore<br/>"
                                +"change the meaning of previously captured Z coordinates.<br/>"
                                +"<span color=\"red\">You will need to recapture these locations!</span>"
                                +"<br/><br/>"
                                +"Are you sure?</html>",
                                null, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            }
            if (result == JOptionPane.YES_OPTION) {
                UiUtils.submitUiMachineTask(() -> {
                    HeadMountable tool = getTool();
                    if (isContactProbeReference()) {
                        // Always use the probing default nozzle for reference probing.
                        tool = ContactProbeNozzle.getDefaultNozzle();
                    }
                    if (! (tool instanceof ContactProbeNozzle)) {
                        throw new Exception("Nozzle "+tool.getName()+" is not a ContactProbeNozzle.");
                    }
                    ContactProbeNozzle nozzle =  (ContactProbeNozzle)tool;
                    Location nominalLocation = getParsedLocation();
                    if (baseLocation != null) {
                        nominalLocation = nominalLocation.rotateXy(baseLocation.getRotation());
                        nominalLocation = nominalLocation.addWithRotation(baseLocation);
                    }
                    if (isContactProbeReference()) {
                        nozzle.resetZCalibration();
                    }
                    final Location probedLocation = nozzle.contactProbeCycle(nominalLocation);
                    MovableUtils.fireTargetedUserAction(nozzle);
                    SwingUtilities.invokeAndWait(() -> {
                        Helpers.copyLocationIntoTextFields(probedLocation, null, null, textFieldZ,
                                null);
                    });
                });
            }
        }
    };

    private Action positionActuatorAction =
            new AbstractAction(Translations.getString("LocalButtonsPanel.Action.PositionActuator"),
                    Icons.centerPin) {
        {
            putValue(Action.SHORT_DESCRIPTION, Translations.getString(
                    "LocalButtonsPanel.Action.PositionActuator.Description"));
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                Actuator actuator = getActuator();
                Location location = getParsedLocation();
                if (baseLocation != null) {
                    if (!isBaseLocationVectorial()) {
                        location = location.rotateXy(baseLocation.getRotation());
                    }
                    location = location.addWithRotation(baseLocation);
                }
                MovableUtils.moveToLocationAtSafeZ(actuator, location);
                MovableUtils.fireTargetedUserAction(actuator);
            });
        }
    };

    private Action positionActuatorNoSafeZAction =
            new AbstractAction(Translations.getString("LocalButtonsPanel.Action.PositionActuatorNoSafeZ"),
                    Icons.centerPinNoSafeZ) {
        {
            putValue(Action.SHORT_DESCRIPTION, Translations.getString(
                    "LocalButtonsPanel.Action.PositionActuatorNoSafeZ.Description"));
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                Actuator actuator = getActuator();
                Location location = getParsedLocation();
                if (baseLocation != null) {
                    if (!isBaseLocationVectorial()) {
                        location = location.rotateXy(baseLocation.getRotation());
                    }
                    location = location.addWithRotation(baseLocation);
                }
                actuator.moveTo(location);
                MovableUtils.fireTargetedUserAction(actuator);
            });
        }
    };
}
