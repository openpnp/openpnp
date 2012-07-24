package org.openpnp.gui.components;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import java.awt.FlowLayout;

public class LocationButtonsPanel extends JPanel {
	private JTextField textFieldX, textFieldY, textFieldZ, textFieldC;
	
	public LocationButtonsPanel(JTextField textFieldX, JTextField textFieldY, JTextField textFieldZ, JTextField textFieldC) {
		FlowLayout flowLayout = (FlowLayout) getLayout();
		flowLayout.setVgap(0);
		flowLayout.setHgap(2);
		this.textFieldX = textFieldX;
		this.textFieldY = textFieldY;
		this.textFieldZ = textFieldZ;
		this.textFieldC = textFieldC;
		
		JButton buttonCaptureCamera = new JButton(getCameraCoordinatesAction);
		buttonCaptureCamera.setToolTipText("Capture the location that the camera is centered over.");
		buttonCaptureCamera.setText("");
		buttonCaptureCamera.setIcon(new ImageIcon(LocationButtonsPanel.class.getResource("/icons/capture-camera.png")));
		add(buttonCaptureCamera);
		
		JButton buttonCaptureTool = new JButton(getToolCoordinatesAction);
		buttonCaptureTool.setToolTipText("Capture the location that the tool is centered over.");
		buttonCaptureTool.setText("");
		buttonCaptureTool.setIcon(new ImageIcon(LocationButtonsPanel.class.getResource("/icons/capture-tool.png")));
		add(buttonCaptureTool);
		
		JButton buttonCaptureToolZ = new JButton(getToolZAction);
		buttonCaptureToolZ.setToolTipText("Capture only the Z coordinate that the tool is centered over.");
		buttonCaptureToolZ.setText("");
		buttonCaptureToolZ.setIcon(new ImageIcon(LocationButtonsPanel.class.getResource("/icons/capture-tool-z.png")));
		add(buttonCaptureToolZ);
		
		JButton buttonCenterCamera = new JButton(positionCameraAction);
		buttonCenterCamera.setToolTipText("Position the camera over the center of the location.");
		buttonCenterCamera.setText("");
		buttonCenterCamera.setIcon(new ImageIcon(LocationButtonsPanel.class.getResource("/icons/center-camera.png")));
		add(buttonCenterCamera);
		
		JButton buttonCenterTool = new JButton(positionToolAction);
		buttonCenterTool.setToolTipText("Position the tool over the center of the location.");
		buttonCenterTool.setText("");
		buttonCenterTool.setIcon(new ImageIcon(LocationButtonsPanel.class.getResource("/icons/center-tool.png")));
		add(buttonCenterTool);
		
		JButton button = new JButton(positionActuatorAction);
		button.setToolTipText("Position the actuator over the center of the location.");
		button.setText("");
		button.setIcon(new ImageIcon(LocationButtonsPanel.class.getResource("/icons/center-pin.png")));
		add(button);
	}
	
	private Action getCameraCoordinatesAction = new AbstractAction("Get Camera Coordinates") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			Location l = MainFrame.machineControlsPanel.getCameraLocation();
			Helpers.copyLocationIntoTextFields(l, textFieldX, textFieldY, textFieldZ, textFieldC);
		}
	};
	
	private Action getToolCoordinatesAction = new AbstractAction("Get Tool Coordinates") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			Location l = MainFrame.machineControlsPanel.getToolLocation();
			Helpers.copyLocationIntoTextFields(l, textFieldX, textFieldY, textFieldZ, textFieldC);
		}
	};
	
	private Action getToolZAction = new AbstractAction("Get Tool Z") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			Location l = MainFrame.machineControlsPanel.getToolLocation();
			Helpers.copyLocationIntoTextFields(l, null, null, textFieldZ);
		}
	};
	
	private Location getParsedLocation() {
		Location location = new Location(Configuration.get().getSystemUnits());
		if (textFieldX != null) {
			location.setX(Length.parse(textFieldX.getText()).getValue());
		}
		if (textFieldY != null) {
			location.setY(Length.parse(textFieldY.getText()).getValue());
		}
		if (textFieldZ != null) {
			location.setZ(Length.parse(textFieldZ.getText()).getValue());
		}
		if (textFieldC != null) {
			location.setRotation(Double.parseDouble(textFieldC.getText()));
		}
		return location;
	}
	
	private Action positionCameraAction = new AbstractAction("Position Camera") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			MainFrame.machineControlsPanel.submitMachineTask(new Runnable() {
				public void run() {
					Head head = Configuration.get().getMachine().getHeads().get(0);
					Camera camera = MainFrame.cameraPanel.getSelectedCamera();
					Location location = getParsedLocation().convertToUnits(head.getMachine().getNativeUnits());
					location = location.subtract(camera.getLocation());
					try {
						// Move to Safe-Z first
						head.moveTo(head.getX(), head.getY(), 0, head.getC());
						// Move the head to the right position at Safe-Z
						head.moveTo(location.getX(), location.getY(), head.getZ(), location.getRotation());
						// Move Z
						head.moveTo(head.getX(), head.getY(), location.getZ(), head.getC());
					}
					catch (Exception e) {
						MessageBoxes.errorBox(getTopLevelAncestor(), "Movement Error", e);
					}
				}
			});
		}
	};
	
	private Action positionToolAction = new AbstractAction("Position Tool") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			MainFrame.machineControlsPanel.submitMachineTask(new Runnable() {
				public void run() {
					Head head = Configuration.get().getMachine().getHeads().get(0);
					Location location = getParsedLocation().convertToUnits(head.getMachine().getNativeUnits());
					try {
						// Move to Safe-Z first
						head.moveTo(head.getX(), head.getY(), 0, head.getC());
						// Move the head to the right position at Safe-Z
						head.moveTo(location.getX(), location.getY(), head.getZ(), location.getRotation());
						// Move Z
						head.moveTo(head.getX(), head.getY(), location.getZ(), head.getC());
					}
					catch (Exception e) {
						MessageBoxes.errorBox(getTopLevelAncestor(), "Movement Error", e);
					}
				}
			});
		}
	};
	
	private Action positionActuatorAction = new AbstractAction("Position Actuator") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			MainFrame.machineControlsPanel.submitMachineTask(new Runnable() {
				public void run() {
					Head head = Configuration.get().getMachine().getHeads().get(0);
					Location location = getParsedLocation().convertToUnits(head.getMachine().getNativeUnits());
//					location = location.subtract(camera.getLocation());
					try {
						// Move to Safe-Z first
						head.moveTo(head.getX(), head.getY(), 0, head.getC());
						// Move the head to the right position at Safe-Z
						head.moveTo(location.getX(), location.getY(), head.getZ(), location.getRotation());
						// Move Z
						head.moveTo(head.getX(), head.getY(), location.getZ(), head.getC());
					}
					catch (Exception e) {
						MessageBoxes.errorBox(getTopLevelAncestor(), "Movement Error", e);
					}
				}
			});
		}
	};
}
