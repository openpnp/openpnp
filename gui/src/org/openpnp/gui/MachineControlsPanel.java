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
import java.awt.event.ActionListener;
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
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTextField;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.support.CameraItem;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MachineListener;

/**
 * Contains controls, DROs and status for the machine.
 * Controls: C right / left, X + / -, Y + / -, Z + / -, stop, pause, slider for jog increment
 * DROs: X, Y, Z, C
 * Radio buttons to select mm or inch.
 * TODO add a dropdown to select Head
 * @author jason
 */
public class MachineControlsPanel extends JPanel {
	private final JFrame frame;
	
	private Machine machine;
	private Head head;

	private JTextField textFieldX;
	private JTextField textFieldY;
	private JTextField textFieldC;
	private JTextField textFieldZ;
	private JButton btnStartStop;
	private JComboBox comboBoxCoordinateSystem;
	private JSlider sliderIncrements;
	private JRadioButton rdbtnMm;
	private JRadioButton rdbtnInch;
	
	private LengthUnit units;

	private Color startColor = Color.green;
	private Color stopColor = new Color(178, 34, 34);
	
	private ExecutorService executor = Executors.newSingleThreadExecutor();
	
	private JogControlsPanel jogControlsPanel;
	private JDialog jogControlsWindow;
	
	/**
	 * Create the panel.
	 */
	public MachineControlsPanel(Configuration configuration, JFrame frame) {
		this.frame = frame;
		
		jogControlsPanel = new JogControlsPanel(configuration, this, frame);
		
		createUi();
		
		configuration.addListener(configurationListener);

		jogControlsWindow = new JDialog(frame, "Jog Controls");
		jogControlsWindow.setResizable(false);
		jogControlsWindow.getContentPane().setLayout(new BorderLayout());
		jogControlsWindow.getContentPane().add(jogControlsPanel);
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
			rdbtnMm.setSelected(true);
		}
		else if (units == LengthUnit.Inches) {
			Hashtable<Integer, JLabel> incrementsLabels = new Hashtable<Integer, JLabel>();
			incrementsLabels.put(1, new JLabel("0.001"));
			incrementsLabels.put(2, new JLabel("0.01"));
			incrementsLabels.put(3, new JLabel("0.1"));
			incrementsLabels.put(4, new JLabel("1"));
			sliderIncrements.setLabelTable(incrementsLabels);
			rdbtnInch.setSelected(true);
		}
		else {
			throw new Error("setUnits() not implemented for " + units);
		}
		this.units = units;
		updateDros();
	}
	
	public LengthUnit getJogUnits() {
		return units;
	}
	
	public double getJogIncrement() {
		if (units == LengthUnit.Millimeters) {
			return 0.01 * Math.pow(10, sliderIncrements.getValue() - 1);
		}
		else if (units == LengthUnit.Inches) {
			return 0.001 * Math.pow(10, sliderIncrements.getValue() - 1);
		}
		else {
			throw new Error("getJogIncrement() not implemented for " + units);
		}
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		droAction.setEnabled(enabled);
		homeAction.setEnabled(enabled);
		jogControlsPanel.setEnabled(enabled);
	}
	
	public Location getDisplayedLocation() {
		Location location = new Location();
		
		if (machine == null || head == null || getJogUnits() == null) {
			return null;
		}
		double x = 0, y = 0, z = 0, c = 0;
		if (comboBoxCoordinateSystem.getSelectedItem() == null || comboBoxCoordinateSystem.getSelectedItem().equals("Tool")) {
			x = head.getX();
			y = head.getY();
			z = head.getZ();
			c = head.getC();
		}
		else if (comboBoxCoordinateSystem.getSelectedItem() instanceof CameraItem) {
			CameraItem cameraItem = (CameraItem) comboBoxCoordinateSystem.getSelectedItem();
			Camera camera = cameraItem.getCamera();
			Location cameraLocation = camera.getLocation();
			cameraLocation = cameraLocation.convertToUnits(machine.getNativeUnits());
			x = head.getX() + cameraLocation.getX();
			y = head.getY() + cameraLocation.getY();
			z = head.getZ() + cameraLocation.getZ();
			c = head.getC() + cameraLocation.getRotation();
		}
		else if (comboBoxCoordinateSystem.getSelectedItem().equals("Absolute")) {
			x = head.getAbsoluteX();
			y = head.getAbsoluteY();
			z = head.getAbsoluteZ();
			c = head.getAbsoluteC();
		}
		
		location.setX(x);
		location.setY(y);
		location.setZ(z);
		location.setRotation(c);
		location.setUnits(machine.getNativeUnits());
		
		location = location.convertToUnits(getJogUnits());
		
		return location;
	}
	
	public void updateDros() {
		Location location = getDisplayedLocation();
		
		if (location == null) {
			return;
		}
		
		if (!textFieldX.hasFocus()) {
			textFieldX.setText(String.format("%1.4f", location.getX()));
		}
		if (!textFieldY.hasFocus()) {
			textFieldY.setText(String.format("%1.4f", location.getY()));
		}
		if (!textFieldZ.hasFocus()) {
			textFieldZ.setText(String.format("%1.4f", location.getZ()));
		}
		if (!textFieldC.hasFocus()) {
			textFieldC.setText(String.format("%1.4f", location.getRotation()));
		}
	}
	
	private void createUi() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		ButtonGroup buttonGroup = new ButtonGroup();
		
		JPanel panelCoordinateSystem = new JPanel();
		FlowLayout flowLayout = (FlowLayout) panelCoordinateSystem.getLayout();
		flowLayout.setAlignment(FlowLayout.LEFT);
		add(panelCoordinateSystem);
		
		JLabel lblNewLabel = new JLabel("Coordinate System");
		panelCoordinateSystem.add(lblNewLabel);
		
		comboBoxCoordinateSystem = new JComboBox();
		comboBoxCoordinateSystem.addActionListener(coordinateSystemSelectedAction);
		panelCoordinateSystem.add(comboBoxCoordinateSystem);
		
		JPanel panelDrosParent = new JPanel();
		add(panelDrosParent);
		panelDrosParent.setLayout(new BorderLayout(0, 0));
		
		JPanel panel_1 = new JPanel();
		panelDrosParent.add(panel_1, BorderLayout.CENTER);
		
		JPanel panelDros = new JPanel();
		panelDros.setBackground(new Color(224, 255, 255));
		panel_1.add(panelDros);
		panelDros.setLayout(new BoxLayout(panelDros, BoxLayout.Y_AXIS));
		
		JPanel panelDrosFirstLine = new JPanel();
		panelDros.add(panelDrosFirstLine);
		panelDrosFirstLine.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		JLabel lblX = new JLabel("X");
		lblX.setFont(new Font("Lucida Grande", Font.BOLD, 24));
		panelDrosFirstLine.add(lblX);
		
		textFieldX = new JTextField();
		textFieldX.setFocusTraversalKeysEnabled(false);
		textFieldX.setSelectionColor(Color.RED);
		textFieldX.setDisabledTextColor(Color.BLACK);
		textFieldX.setBackground(new Color(143, 188, 143));
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
		textFieldY.setSelectionColor(Color.RED);
		textFieldY.setDisabledTextColor(Color.BLACK);
		textFieldY.setBackground(new Color(143, 188, 143));
		textFieldY.setFont(new Font("Lucida Grande", Font.BOLD, 24));
		textFieldY.setText("0000.0000");
		panelDrosFirstLine.add(textFieldY);
		textFieldY.setColumns(6);
		textFieldY.addFocusListener(droFocusListener);
		textFieldY.setAction(droAction);
		textFieldY.addMouseListener(droMouseListener);
		
		JPanel panelDrosSecondLine = new JPanel();
		panelDros.add(panelDrosSecondLine);
		panelDrosSecondLine.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		JLabel lblC = new JLabel("C");
		lblC.setFont(new Font("Lucida Grande", Font.BOLD, 24));
		panelDrosSecondLine.add(lblC);
		
		textFieldC = new JTextField();
		textFieldC.setFocusTraversalKeysEnabled(false);
		textFieldC.setSelectionColor(Color.RED);
		textFieldC.setDisabledTextColor(Color.BLACK);
		textFieldC.setBackground(new Color(143, 188, 143));
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
		textFieldZ.setSelectionColor(Color.RED);
		textFieldZ.setDisabledTextColor(Color.BLACK);
		textFieldZ.setBackground(new Color(143, 188, 143));
		textFieldZ.setText("0000.0000");
		textFieldZ.setFont(new Font("Lucida Grande", Font.BOLD, 24));
		textFieldZ.setColumns(6);
		textFieldZ.addFocusListener(droFocusListener);
		textFieldZ.setAction(droAction);
		textFieldZ.addMouseListener(droMouseListener);
		panelDrosSecondLine.add(textFieldZ);
		
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
		
		JPanel panelUnits = new JPanel();
		panelIncrements.add(panelUnits);
		rdbtnMm = new JRadioButton("MM");
		buttonGroup.add(rdbtnMm);
		rdbtnMm.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setUnits(LengthUnit.Millimeters);
			}
		});
		panelUnits.setLayout(new BoxLayout(panelUnits, BoxLayout.Y_AXIS));
		panelUnits.add(rdbtnMm);
		
		rdbtnInch = new JRadioButton("Inch");
		buttonGroup.add(rdbtnInch);
		rdbtnInch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setUnits(LengthUnit.Inches);
			}
		});
		panelUnits.add(rdbtnInch);
		
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
			return comboBoxCoordinateSystem;
		}

		@Override
		public Component getComponentBefore(Container aContainer,
				Component aComponent) {
			return comboBoxCoordinateSystem;
		}

		@Override
		public Component getDefaultComponent(Container aContainer) {
			return comboBoxCoordinateSystem;
		}

		@Override
		public Component getFirstComponent(Container aContainer) {
			return comboBoxCoordinateSystem;
		}

		@Override
		public Component getInitialComponent(Window window) {
			return comboBoxCoordinateSystem;
		}

		@Override
		public Component getLastComponent(Container aContainer) {
			return comboBoxCoordinateSystem;
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
			JTextField dro = (JTextField) e.getSource();
			double value = Double.parseDouble(dro.getText());

			Location cameraLocation = null;
			if (comboBoxCoordinateSystem.getSelectedItem() instanceof CameraItem) {
				CameraItem cameraItem = (CameraItem) comboBoxCoordinateSystem.getSelectedItem();
				Camera camera = cameraItem.getCamera();
				cameraLocation = camera.getLocation();
				cameraLocation = cameraLocation.convertToUnits(machine.getNativeUnits());
			}
			
			if (dro == textFieldX) {
				if (cameraLocation != null) {
					value -= cameraLocation.getX();
				}
				head.setPerceivedX(value);
			}
			else if (dro == textFieldY) {
				if (cameraLocation != null) {
					value -= cameraLocation.getY();
				}
				head.setPerceivedY(value);
			}
			else if (dro == textFieldZ) {
				if (cameraLocation != null) {
					value -= cameraLocation.getZ();
				}
				head.setPerceivedZ(value);
			}
			else if (dro == textFieldC) {
				if (cameraLocation != null) {
					value -= cameraLocation.getRotation();
				}
				head.setPerceivedC(value);
			}
			btnStartStop.requestFocus();
		}
	};
	
	private FocusListener droFocusListener = new FocusAdapter() {
		@Override
		public void focusGained(FocusEvent e) {
			JTextField dro = (JTextField) e.getComponent();
			dro.setBackground(Color.RED);
			dro.setSelectionStart(0);
			dro.setSelectionEnd(dro.getText().length());
		}

		@Override
		public void focusLost(FocusEvent e) {
			JTextField dro = (JTextField) e.getComponent();
			dro.setBackground(new Color(143, 188, 143));
			dro.setSelectionEnd(0);
			dro.setSelectionEnd(0);
			updateDros();
		}
	};
	
	private ActionListener coordinateSystemSelectedAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (comboBoxCoordinateSystem.getSelectedItem().equals("Absolute")) {
				droAction.setEnabled(false);
			}
			else {
				droAction.setEnabled(MachineControlsPanel.this.isEnabled());
			}
			updateDros();
		}
	};
	
	@SuppressWarnings("serial")
	public Action homeAction = new AbstractAction("Home") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			executor.submit(new Runnable() {
				public void run() {
					try {
						head.home();
						
						if (comboBoxCoordinateSystem.getSelectedItem() instanceof CameraItem) {
							CameraItem cameraItem = (CameraItem) comboBoxCoordinateSystem.getSelectedItem();
							Camera camera = cameraItem.getCamera();
							Location cameraLocation = camera.getLocation();
							cameraLocation = cameraLocation.convertToUnits(machine.getNativeUnits());
							double x = head.getX() - cameraLocation.getX();
							double y = head.getY() - cameraLocation.getY();
							double z = head.getZ() - cameraLocation.getZ();
							double c = head.getC() - cameraLocation.getRotation();
							
							head.moveTo(x, y, z, c);
						}
						
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
	
	private ConfigurationListener configurationListener = new ConfigurationListener.Adapter() {
		@Override
		public void configurationLoaded(Configuration configuration) {
			if (machine != null) {
				machine.removeListener(machineListener);
			}
			
			machine = configuration.getMachine();
			head = machine.getHeads().get(0);
			setUnits(machine.getNativeUnits());
			machine.addListener(machineListener);
			
			comboBoxCoordinateSystem.removeAllItems();
			comboBoxCoordinateSystem.addItem("Tool");
			for (Camera camera : machine.getCameras()) {
				if (camera.getHead() != null) {
					comboBoxCoordinateSystem.addItem(new CameraItem(camera));
				}
			}
			comboBoxCoordinateSystem.addItem("Absolute");
			comboBoxCoordinateSystem.setSelectedIndex(0);
			
			btnStartStop.setAction(machine.isEnabled() ? stopMachineAction : startMachineAction);
			btnStartStop.setForeground(machine.isEnabled() ? stopColor : startColor);

			setEnabled(machine.isEnabled());
		}
	};
}
