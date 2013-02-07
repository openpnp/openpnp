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

package org.openpnp.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.FocusTraversalPolicy;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Hashtable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.components.CameraPanel;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MachineListener;
import org.openpnp.spi.Movable;

/**
 * TODO Add a dropdown to select Head
 * TODO Lots of little bugs relating to setting absolute mode while a dro is
 * selected and managing the background color, enabled and selected state of
 * the dros.
 * @author jason
 */
public class MachineControlsPanel extends JPanel {
	private final JFrame frame;
	private final CameraPanel cameraPanel;
	private final Configuration configuration;
	
	private Machine machine;
	private Movable movable;

	private JTextField textFieldX;
	private JTextField textFieldY;
	private JTextField textFieldC;
	private JTextField textFieldZ;
	private JButton btnStartStop;
	private JSlider sliderIncrements;
	
	private Color startColor = Color.green;
	private Color stopColor = new Color(178, 34, 34);
	private Color droNormalColor = new Color(143, 188, 143);
	private Color droEditingColor = Color.yellow;
	private Color droWarningColor = Color.red;
	
	private ExecutorService machineTaskExecutor = Executors.newSingleThreadExecutor();
	
	private JogControlsPanel jogControlsPanel;
	private JDialog jogControlsWindow;
	
	/**
	 * Create the panel.
	 */
	public MachineControlsPanel(Configuration configuration, JFrame frame, CameraPanel cameraPanel) {
		this.frame = frame;
		this.cameraPanel = cameraPanel;
		this.configuration = configuration;
		
		jogControlsPanel = new JogControlsPanel(configuration, this, frame);
		
		createUi();
		
		configuration.addListener(configurationListener);

		jogControlsWindow = new JDialog(frame, "Jog Controls");
		jogControlsWindow.setResizable(false);
		jogControlsWindow.getContentPane().setLayout(new BorderLayout());
		jogControlsWindow.getContentPane().add(jogControlsPanel);
	}
	
	// TODO: Change this to take an interface that will pass in the Machine,
	// Configuration, Head, etc. and handle exceptions with the proper dialog.
	public void submitMachineTask(Runnable runnable) {
		if (!machine.isEnabled()) {
			MessageBoxes.errorBox(getTopLevelAncestor(), "Machine Error", "Machine is not started.");
			return;
		}
		machineTaskExecutor.submit(runnable);
	}
	
	public JogControlsPanel getJogControlsPanel() {
		return jogControlsPanel;
	}
	
	private void setUnits(LengthUnit units) {
		if (units == LengthUnit.Millimeters) {
			Hashtable<Integer, JLabel> incrementsLabels = new Hashtable<Integer, JLabel>();
			incrementsLabels.put(1, new JLabel("0.01"));
			incrementsLabels.put(2, new JLabel("0.1"));
			incrementsLabels.put(3, new JLabel("1.0"));
			incrementsLabels.put(4, new JLabel("10"));
			sliderIncrements.setLabelTable(incrementsLabels);
		}
		else if (units == LengthUnit.Inches) {
			Hashtable<Integer, JLabel> incrementsLabels = new Hashtable<Integer, JLabel>();
			incrementsLabels.put(1, new JLabel("0.001"));
			incrementsLabels.put(2, new JLabel("0.01"));
			incrementsLabels.put(3, new JLabel("0.1"));
			incrementsLabels.put(4, new JLabel("1.0"));
			sliderIncrements.setLabelTable(incrementsLabels);
		}
		else {
			throw new Error("setUnits() not implemented for " + units);
		}
		updateDros();
	}

	/**
	 * Gets the current location of the tool.
	 * TODO: Should be moved to MainFrame, or some other master controller.
	 * @return
	 */
	public Location getToolLocation() {
		return new Location(
				machine.getNativeUnits(), 
				head.getX(), 
				head.getY(), 
				head.getZ(), 
				head.getC());
	}
	
	/**
	 * Gets the location that is currently being looked at by the selected
	 * Camera. If no Cameras is selected, or All Cameras are selected, returns
	 * null.
	 * TODO: This should be moved to MainFrame, or some other master controller.
	 * @return
	 */
	public Location getCameraLocation() {
		CameraView cameraView = cameraPanel.getSelectedCameraView();
		if (cameraView == null) {
			return null;
		}
		Location toolLocation = getToolLocation();
		Location cameraLocation = cameraView.getCamera().getLocation();
		return toolLocation.add(cameraLocation);
	}
	
	public double getJogIncrement() {
		if (configuration.getSystemUnits() == LengthUnit.Millimeters) {
			return 0.01 * Math.pow(10, sliderIncrements.getValue() - 1);
		}
		else if (configuration.getSystemUnits() == LengthUnit.Inches) {
			return 0.001 * Math.pow(10, sliderIncrements.getValue() - 1);
		}
		else {
			throw new Error("getJogIncrement() not implemented for " + configuration.getSystemUnits());
		}
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		droAction.setEnabled(enabled);
		homeAction.setEnabled(enabled);
		goToZeroAction.setEnabled(enabled);
		jogControlsPanel.setEnabled(enabled);
		targetCameraAction.setEnabled(enabled);
		targetToolAction.setEnabled(enabled);
	}
	
	public void updateDros() {
		if (machine == null || movable == null) {
			return;
		}
		
		Location l = movable.getLocation();
		l = l.convertToUnits(configuration.getSystemUnits());
		
		double x, y, z, c;
		
		x = l.getX();
		y = l.getY();
		z = l.getZ();
		c = l.getRotation();
		
		
		if (!textFieldX.hasFocus()) {
			textFieldX.setText(String.format(configuration.getLengthDisplayFormat(), x));
		}
		if (!textFieldY.hasFocus()) {
			textFieldY.setText(String.format(configuration.getLengthDisplayFormat(), y));
		}
		if (!textFieldZ.hasFocus()) {
			textFieldZ.setText(String.format(configuration.getLengthDisplayFormat(), z));
		}
		if (!textFieldC.hasFocus()) {
			textFieldC.setText(String.format(configuration.getLengthDisplayFormat(), c));
		}
	}
	
	private void createUi() {
		showAbsoluteCoordinatesAction.putValue(Action.SELECTED_KEY, false);
		
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		ButtonGroup buttonGroup = new ButtonGroup();
		
		JPanel panelDrosParent = new JPanel();
		add(panelDrosParent);
		panelDrosParent.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		JPanel panelDros = new JPanel();
		panelDrosParent.add(panelDros);
		panelDros.setLayout(new BoxLayout(panelDros, BoxLayout.Y_AXIS));
		
		JPanel panelDrosFirstLine = new JPanel();
		panelDros.add(panelDrosFirstLine);
		panelDrosFirstLine.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		JLabel lblX = new JLabel("X");
		lblX.setFont(new Font("Lucida Grande", Font.BOLD, 24));
		panelDrosFirstLine.add(lblX);
		
		textFieldX = new JTextField();
		textFieldX.setFocusTraversalKeysEnabled(false);
		textFieldX.setSelectionColor(droEditingColor);
		textFieldX.setDisabledTextColor(Color.BLACK);
		textFieldX.setBackground(droNormalColor);
		textFieldX.setFont(new Font("Lucida Grande", Font.BOLD, 24));
		textFieldX.setText("0000.0000");
		panelDrosFirstLine.add(textFieldX);
		textFieldX.setColumns(6);
		textFieldX.addFocusListener(droFocusListener);
		textFieldX.setAction(droAction);
		textFieldX.addMouseListener(droMouseListener);
		
		Component horizontalStrut = Box.createHorizontalStrut(15);
		panelDrosFirstLine.add(horizontalStrut);
		
		JLabel lblY = new JLabel("Y");
		lblY.setFont(new Font("Lucida Grande", Font.BOLD, 24));
		panelDrosFirstLine.add(lblY);
		
		textFieldY = new JTextField();
		textFieldY.setFocusTraversalKeysEnabled(false);
		textFieldY.setSelectionColor(droEditingColor);
		textFieldY.setDisabledTextColor(Color.BLACK);
		textFieldY.setBackground(droNormalColor);
		textFieldY.setFont(new Font("Lucida Grande", Font.BOLD, 24));
		textFieldY.setText("0000.0000");
		panelDrosFirstLine.add(textFieldY);
		textFieldY.setColumns(6);
		textFieldY.addFocusListener(droFocusListener);
		textFieldY.setAction(droAction);
		
		JButton btnTargetTool = new JButton(targetToolAction);
		panelDrosFirstLine.add(btnTargetTool);
		btnTargetTool.setToolTipText("Position the tool at the camera's current location.");
		textFieldY.addMouseListener(droMouseListener);
		
		JPanel panelDrosSecondLine = new JPanel();
		panelDros.add(panelDrosSecondLine);
		panelDrosSecondLine.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		JLabel lblC = new JLabel("C");
		lblC.setFont(new Font("Lucida Grande", Font.BOLD, 24));
		panelDrosSecondLine.add(lblC);
		
		textFieldC = new JTextField();
		textFieldC.setFocusTraversalKeysEnabled(false);
		textFieldC.setSelectionColor(droEditingColor);
		textFieldC.setDisabledTextColor(Color.BLACK);
		textFieldC.setBackground(droNormalColor);
		textFieldC.setText("0000.0000");
		textFieldC.setFont(new Font("Lucida Grande", Font.BOLD, 24));
		textFieldC.setColumns(6);
		textFieldC.addFocusListener(droFocusListener);
		textFieldC.setAction(droAction);
		textFieldC.addMouseListener(droMouseListener);
		panelDrosSecondLine.add(textFieldC);
		
		Component horizontalStrut_1 = Box.createHorizontalStrut(15);
		panelDrosSecondLine.add(horizontalStrut_1);
		
		JLabel lblZ = new JLabel("Z");
		lblZ.setFont(new Font("Lucida Grande", Font.BOLD, 24));
		panelDrosSecondLine.add(lblZ);
		
		textFieldZ = new JTextField();
		textFieldZ.setFocusTraversalKeysEnabled(false);
		textFieldZ.setSelectionColor(droEditingColor);
		textFieldZ.setDisabledTextColor(Color.BLACK);
		textFieldZ.setBackground(droNormalColor);
		textFieldZ.setText("0000.0000");
		textFieldZ.setFont(new Font("Lucida Grande", Font.BOLD, 24));
		textFieldZ.setColumns(6);
		textFieldZ.addFocusListener(droFocusListener);
		textFieldZ.setAction(droAction);
		textFieldZ.addMouseListener(droMouseListener);
		panelDrosSecondLine.add(textFieldZ);
		
		JButton btnTargetCamera = new JButton(targetCameraAction);
		panelDrosSecondLine.add(btnTargetCamera);
		btnTargetCamera.setToolTipText("Position the camera at the tool's current location.");
		
		JPanel panelIncrements = new JPanel();
		add(panelIncrements);
		
		sliderIncrements = new JSlider();
		panelIncrements.add(sliderIncrements);
		sliderIncrements.setMajorTickSpacing(1);
		sliderIncrements.setValue(1);
		sliderIncrements.setSnapToTicks(true);
		sliderIncrements.setPaintLabels(true);
		sliderIncrements.setPaintTicks(true);
		sliderIncrements.setMinimum(1);
		sliderIncrements.setMaximum(4);
		
		JPanel panelStartStop = new JPanel();
		add(panelStartStop);
		panelStartStop.setLayout(new BorderLayout(0, 0));
		
		btnStartStop = new JButton(startMachineAction);
		btnStartStop.setFocusable(true);
		btnStartStop.setForeground(startColor);
		panelStartStop.add(btnStartStop);
		btnStartStop.setFont(new Font("Lucida Grande", Font.BOLD, 48));
		btnStartStop.setPreferredSize(new Dimension(160, 70));
		
		setFocusTraversalPolicy(focusPolicy);
		setFocusTraversalPolicyProvider(true);
	}
	
	private FocusTraversalPolicy focusPolicy = new FocusTraversalPolicy() {
		@Override
		public Component getComponentAfter(Container aContainer,
				Component aComponent) {
			return sliderIncrements;
		}

		@Override
		public Component getComponentBefore(Container aContainer,
				Component aComponent) {
			return sliderIncrements;
		}

		@Override
		public Component getDefaultComponent(Container aContainer) {
			return sliderIncrements;
		}

		@Override
		public Component getFirstComponent(Container aContainer) {
			return sliderIncrements;
		}

		@Override
		public Component getInitialComponent(Window window) {
			return sliderIncrements;
		}

		@Override
		public Component getLastComponent(Container aContainer) {
			return sliderIncrements;
		}
	};
	
	@SuppressWarnings("serial")
	private Action stopMachineAction = new AbstractAction("STOP") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			try {
				machine.setEnabled(false);
				MachineControlsPanel.this.setEnabled(false);
			}
			catch (Exception e) {
				MessageBoxes.errorBox(MachineControlsPanel.this, "Stop Failed", e.getMessage());
			}
		}
	};
	
	@SuppressWarnings("serial")
	private Action startMachineAction = new AbstractAction("START") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			try {
				machine.setEnabled(true);
				MachineControlsPanel.this.setEnabled(true);
			}
			catch (Exception e) {
				MessageBoxes.errorBox(MachineControlsPanel.this, "Start Failed", e.getMessage());
			}
		}
	};
	
	private MouseListener droMouseListener = new MouseAdapter() {
		private boolean hadFocus;

		@Override
		public void mousePressed(MouseEvent e) {
			JTextField dro = (JTextField) e.getComponent();
			hadFocus = dro.hasFocus();
		}
		
		public void mouseClicked(MouseEvent e) {
			JTextField dro = (JTextField) e.getComponent();
			if (hadFocus) {
				dro.transferFocus();
			}
		}
	};
	
	@SuppressWarnings("serial")
	private Action droAction = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
		    // TODO: Remove
//			JTextField dro = (JTextField) e.getSource();
//			double value = Double.parseDouble(dro.getText());
//
//			if (dro == textFieldX) {
//				head.setPerceivedX(value);
//			}
//			else if (dro == textFieldY) {
//				head.setPerceivedY(value);
//			}
//			else if (dro == textFieldZ) {
//				head.setPerceivedZ(value);
//			}
//			else if (dro == textFieldC) {
//				head.setPerceivedC(value);
//			}
//			btnStartStop.requestFocus();
		}
	};
	
	private FocusListener droFocusListener = new FocusAdapter() {
		@Override
		public void focusGained(FocusEvent e) {
			JTextField dro = (JTextField) e.getComponent();
			dro.setBackground(droEditingColor);
			dro.setSelectionStart(0);
			dro.setSelectionEnd(dro.getText().length());
		}

		@Override
		public void focusLost(FocusEvent e) {
			JTextField dro = (JTextField) e.getComponent();
			dro.setBackground(droNormalColor);
			dro.setSelectionEnd(0);
			dro.setSelectionEnd(0);
			updateDros();
		}
	};
	
	public Action showAbsoluteCoordinatesAction = new AbstractAction("Show Absolute Coordinates") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (getValue(Action.SELECTED_KEY).equals(true)) {
				droAction.setEnabled(false);
				textFieldX.setBackground(droWarningColor);
				textFieldY.setBackground(droWarningColor);
				textFieldZ.setBackground(droWarningColor);
				textFieldC.setBackground(droWarningColor);
			}
			else {
				droAction.setEnabled(MachineControlsPanel.this.isEnabled());
				textFieldX.setBackground(droNormalColor);
				textFieldY.setBackground(droNormalColor);
				textFieldZ.setBackground(droNormalColor);
				textFieldC.setBackground(droNormalColor);
			}
			updateDros();
		}
	};
	
	@SuppressWarnings("serial")
	public Action goToZeroAction = new AbstractAction("Go To Zero") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			submitMachineTask(new Runnable() {
				public void run() {
					try {
						head.moveToSafeZ();
						// Move to 0, 0, 0, 0.
						head.moveTo(0, 0, 0, 0);
					}
					catch (Exception e) {
						e.printStackTrace();
						MessageBoxes.errorBox(frame, "Homing Failed", e);
					}
				}
			});
		}
	};
	
	@SuppressWarnings("serial")
	public Action homeAction = new AbstractAction("Home") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			submitMachineTask(new Runnable() {
				public void run() {
					try {
						head.home();
					}
					catch (Exception e) {
						e.printStackTrace();
						MessageBoxes.errorBox(frame, "Homing Failed", e);
					}
				}
			});
		}
	};
	
	public Action showHideJogControlsWindowAction = new AbstractAction("Show Jog Controls") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (jogControlsWindow.isVisible()) {
				jogControlsWindow.setVisible(false);
			}
			else {
				jogControlsWindow.setVisible(true);
				jogControlsWindow.pack();
				int x = (int) getLocationOnScreen().getX();
				int y = (int) getLocationOnScreen().getY();
				x += (getSize().getWidth() / 2) - (jogControlsWindow.getSize().getWidth() / 2);
				y += getSize().getHeight();
				jogControlsWindow.setLocation(x, y);
			}
		}
	};
	
	@SuppressWarnings("serial")
	public Action raiseIncrementAction = new AbstractAction("Raise Jog Increment") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			sliderIncrements.setValue(Math.min(sliderIncrements.getMaximum(), sliderIncrements.getValue() + 1));
		}
	};
	
	@SuppressWarnings("serial")
	public Action lowerIncrementAction = new AbstractAction("Lower Jog Increment") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			sliderIncrements.setValue(Math.max(sliderIncrements.getMinimum(), sliderIncrements.getValue() - 1));
		}
	};
	
	@SuppressWarnings("serial")
	public Action targetToolAction = new AbstractAction(null, new ImageIcon(MachineControlsPanel.class.getResource("/icons/center-tool.png"))) {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			final Location location = getCameraLocation();
			submitMachineTask(new Runnable() {
				public void run() {
					try {
						head.moveToSafeZ();
						head.moveTo(
								location.getX(),
								location.getY(),
								location.getZ(),
								head.getC());
					}
					catch (Exception e) {
						MessageBoxes.errorBox(frame, "Move Failed", e);
					}
				}
			});
		}
	};
	
	@SuppressWarnings("serial")
	public Action targetCameraAction = new AbstractAction(null, new ImageIcon(MachineControlsPanel.class.getResource("/icons/center-camera.png"))) {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			CameraView cameraView = cameraPanel.getSelectedCameraView();
			if (cameraView == null) {
				return;
			}
			final Location location = getToolLocation();
			final Location cameraLocation = cameraView.getCamera().getLocation().convertToUnits(location.getUnits());
			submitMachineTask(new Runnable() {
				public void run() {
					try {
						head.moveToSafeZ();
						head.moveTo(
								location.getX() - cameraLocation.getX(),
								location.getY() - cameraLocation.getY(),
								location.getZ() - cameraLocation.getZ(),
								head.getC());
					}
					catch (Exception e) {
						MessageBoxes.errorBox(frame, "Move Failed", e);
					}
				}
			});
		}
	};
	
	private MachineListener machineListener = new MachineListener.Adapter() {
		@Override
		public void machineHeadActivity(Machine machine, Head head) {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					updateDros();
				}
			});
		}

		@Override
		public void machineEnabled(Machine machine) {
			btnStartStop.setAction(machine.isEnabled() ? stopMachineAction : startMachineAction);
			btnStartStop.setForeground(machine.isEnabled() ? stopColor : startColor);
		}

		@Override
		public void machineEnableFailed(Machine machine, String reason) {
			btnStartStop.setAction(machine.isEnabled() ? stopMachineAction : startMachineAction);
			btnStartStop.setForeground(machine.isEnabled() ? stopColor : startColor);
		}

		@Override
		public void machineDisabled(Machine machine, String reason) {
			btnStartStop.setAction(machine.isEnabled() ? stopMachineAction : startMachineAction);
			btnStartStop.setForeground(machine.isEnabled() ? stopColor : startColor);
		}

		@Override
		public void machineDisableFailed(Machine machine, String reason) {
			btnStartStop.setAction(machine.isEnabled() ? stopMachineAction : startMachineAction);
			btnStartStop.setForeground(machine.isEnabled() ? stopColor : startColor);
		}
	};
	
	private ConfigurationListener configurationListener = new ConfigurationListener() {
		@Override
		public void configurationLoaded(Configuration configuration) {
			if (machine != null) {
				machine.removeListener(machineListener);
			}
			
			machine = configuration.getMachine();
			movable = machine.getHeads().get(0).getNozzles().get(0);
			setUnits(configuration.getSystemUnits());
			machine.addListener(machineListener);
			
			btnStartStop.setAction(machine.isEnabled() ? stopMachineAction : startMachineAction);
			btnStartStop.setForeground(machine.isEnabled() ? stopColor : startColor);

			setEnabled(machine.isEnabled());
		}
	};
}
