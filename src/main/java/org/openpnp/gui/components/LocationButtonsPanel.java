/*
 	Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 	
 	This file is part of OpenPnP.
 	
	OpenPnP is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenPnP is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenPnP.  If not, see <http://www.gnu.org/licenses/>.
 	
 	For more information about OpenPnP visit http://openpnp.org
*/

package org.openpnp.gui.components;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.Icons;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.Location;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.HeadMountable;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.UiUtils;

/**
 * A JPanel of 4 small buttons that assist in setting locations. The buttons
 * are Capture Camera Coordinates, Capture Tool Coordinates, Move Camera
 * to Coordinates and Move Tool to Coordinates. If the actuatorId property
 * is set, this causes the component to use the specified Actuator in place
 * of the tool. 
 */
@SuppressWarnings("serial")
public class LocationButtonsPanel extends JPanel {
	private JTextField textFieldX, textFieldY, textFieldZ, textFieldC;
	private String actuatorName;

	private JButton buttonCenterTool;
	private JButton buttonCaptureCamera;
	private JButton buttonCaptureTool;
	
	public LocationButtonsPanel(JTextField textFieldX, JTextField textFieldY,
			JTextField textFieldZ, JTextField textFieldC) {
		FlowLayout flowLayout = (FlowLayout) getLayout();
		flowLayout.setVgap(0);
		flowLayout.setHgap(2);
		this.textFieldX = textFieldX;
		this.textFieldY = textFieldY;
		this.textFieldZ = textFieldZ;
		this.textFieldC = textFieldC;

		buttonCaptureCamera = new JButton(captureCameraCoordinatesAction);
		buttonCaptureCamera.setHideActionText(true);
		add(buttonCaptureCamera);

		buttonCaptureTool = new JButton(captureToolCoordinatesAction);
		buttonCaptureTool.setHideActionText(true);
		add(buttonCaptureTool);

		JButton buttonCenterCamera = new JButton(positionCameraAction);
		buttonCenterCamera.setHideActionText(true);
		add(buttonCenterCamera);

		buttonCenterTool = new JButton(positionToolAction);
		buttonCenterTool.setHideActionText(true);
		add(buttonCenterTool);
		
		buttonCenterToolNoSafeZ = new JButton(positionToolNoSafeZAction);
		buttonCenterToolNoSafeZ.setHideActionText(true);

		setActuatorName(null);
	}
	
	public void setShowPositionToolNoSafeZ(boolean b) {
	    if (b) {
	        add(buttonCenterToolNoSafeZ);
	    }
	    else {
	        remove(buttonCenterToolNoSafeZ);
	    }
	}
	
	public void setActuatorName(String actuatorName) {
		this.actuatorName = actuatorName;
		if (actuatorName == null || actuatorName.trim().length() == 0) {
			buttonCaptureTool.setAction(captureToolCoordinatesAction);
			buttonCenterTool.setAction(positionToolAction);
		}
		else {
			buttonCaptureTool.setAction(captureActuatorCoordinatesAction);
			buttonCenterTool.setAction(positionActuatorAction);
		}
	}

	public String getActuatorName() {
		return actuatorName;
	}
	
	public HeadMountable getTool() throws Exception {
		return MainFrame.machineControlsPanel.getSelectedNozzle();
	}
	
	public Camera getCamera() throws Exception {
		return getTool().getHead().getDefaultCamera();
	}
	
	/**
	 * Get the Actuator with the name provided by setActuatorName() that is
	 * on the same Head as the tool from getTool(). 
	 * @return
	 * @throws Exception
	 */
	public Actuator getActuator() throws Exception {
		if (actuatorName == null) {
			return null;
		}
		HeadMountable tool = getTool();
		Head head = tool.getHead();
		Actuator actuator = head.getActuator(actuatorName);
		if (actuator == null) {
			throw new Exception(String.format(
					"No Actuator with name %s on Head %s",
					actuatorName, 
					head.getName()));
		}
		return actuator;
	}

	private Location getParsedLocation() {
	    double x = 0, y = 0, z = 0, rotation = 0;
		if (textFieldX != null) {
			x = Length.parse(textFieldX.getText()).getValue();
		}
		if (textFieldY != null) {
			y = Length.parse(textFieldY.getText()).getValue();
		}
		if (textFieldZ != null) {
			z = Length.parse(textFieldZ.getText()).getValue();
		}
		if (textFieldC != null) {
			rotation = Double.parseDouble(textFieldC.getText());
		}
		return new Location(Configuration.get().getSystemUnits(), x, y, z, rotation);
	}

	private Action captureCameraCoordinatesAction = new AbstractAction(
			"Get Camera Coordinates", Icons.captureCamera) {
		{
			putValue(Action.SHORT_DESCRIPTION,
					"Capture the location that the camera is centered on.");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			UiUtils.messageBoxOnException(() -> {
				Location l = getCamera().getLocation();
				Helpers.copyLocationIntoTextFields(l, textFieldX, textFieldY,
						null, textFieldC);
			});
		}
	};

	private Action captureToolCoordinatesAction = new AbstractAction(
			"Get Tool Coordinates", Icons.captureTool) {
		{
			putValue(Action.SHORT_DESCRIPTION,
					"Capture the location that the tool is centered on.");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			UiUtils.messageBoxOnException(() -> {
				Location l = getTool().getLocation();
				Helpers.copyLocationIntoTextFields(l, textFieldX, textFieldY,
						textFieldZ, textFieldC);
			});
		}
	};

	private Action captureActuatorCoordinatesAction = new AbstractAction(
			"Get Actuator Coordinates", Icons.capturePin) {
		{
			putValue(Action.SHORT_DESCRIPTION,
					"Capture the location that the actuator is centered on.");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			UiUtils.messageBoxOnException(() -> {
				Actuator actuator = getActuator();
				if (actuator == null) {
					return;
				}
				Helpers.copyLocationIntoTextFields(actuator.getLocation(), 
						textFieldX, textFieldY,
						textFieldZ, textFieldC);
			});
			
		}
	};

	private Action positionCameraAction = new AbstractAction("Position Camera",
			Icons.centerCamera) {
		{
			putValue(Action.SHORT_DESCRIPTION,
					"Position the camera over the center of the location.");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			UiUtils.submitUiMachineTask(() -> {
				Camera camera = getCamera();
	            Location location = getParsedLocation();
			    MovableUtils.moveToLocationAtSafeZ(camera, location, 1.0);
			});
		}
	};

    private Action positionToolAction = new AbstractAction("Position Tool",
            Icons.centerTool) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Position the tool over the center of the location.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                HeadMountable tool = getTool();
                Location location = getParsedLocation();
                MovableUtils.moveToLocationAtSafeZ(tool, location, 1.0);
            });
        }
    };

    private Action positionToolNoSafeZAction = new AbstractAction("Position Tool (Without Safe Z)",
            Icons.centerToolNoSafeZ) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Position the tool over the center of the location without first moving to Safe Z.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                HeadMountable tool = getTool();
                Location location = getParsedLocation();
                tool.moveTo(location, 1.0);
            });
        }
    };

	private Action positionActuatorAction = new AbstractAction(
			"Position Actuator", Icons.centerPin) {
		{
			putValue(Action.SHORT_DESCRIPTION,
					"Position the actuator over the center of the location.");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			UiUtils.submitUiMachineTask(() -> {
				Actuator actuator = getActuator();
	            Location location = getParsedLocation();
			    MovableUtils.moveToLocationAtSafeZ(actuator, location, 1.0);
			});
		}
	};
	private JButton buttonCenterToolNoSafeZ;
}
