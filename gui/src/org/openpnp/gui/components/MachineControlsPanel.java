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

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.FocusTraversalPolicy;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import org.openpnp.LengthUnit;
import org.openpnp.Location;
import org.openpnp.gui.support.CameraItem;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MachineListener;
import org.openpnp.util.LengthUtil;

/**
 * Contains controls, DROs and status for the machine.
 * Controls: C right / left, X + / -, Y + / -, Z + / -, stop, pause, slider for jog increment
 * DROs: X, Y, Z, C
 * Radio buttons to select mm or inch.
 * Status: LEDs for vac and actuators. TODO This part is not machine independant. Need to think about that.
 * TODO add a dropdown to select Head
 * @author jason
 */
public class MachineControlsPanel extends JPanel implements MachineListener {
	private Machine machine;
	private Head head;
	private LengthUnit units;

	private JTextField textFieldX;
	private JTextField textFieldY;
	private JTextField textFieldC;
	private JTextField textFieldZ;
	private JSlider sliderIncrements;
	private JRadioButton rdbtnMm;
	private JRadioButton rdbtnInch;
	private JButton btnStartStop;
	private JComboBox comboBoxCoordinateSystem;
	private final ButtonGroup buttonGroup = new ButtonGroup();
	
	private ExecutorService executor = Executors.newSingleThreadExecutor();
	
	/**
	 * Create the panel.
	 */
	public MachineControlsPanel() {
		createUi();
	}
	
	public void setMachine(Machine machine) {
		this.machine = machine;
		this.head = machine.getHeads().get(0);
		setUnits(machine.getNativeUnits());
		machine.addListener(this);
		comboBoxCoordinateSystem.removeAllItems();
		comboBoxCoordinateSystem.addItem("Tool");
		for (Camera camera : machine.getCameras()) {
			if (camera.getHead() != null) {
				comboBoxCoordinateSystem.addItem(new CameraItem(camera));
			}
		}
		comboBoxCoordinateSystem.addItem("Absolute");
		comboBoxCoordinateSystem.setSelectedIndex(0);
		btnStartStop.setAction(machine.isReady() ? stopMachineAction : startMachineAction);

		final Map<KeyStroke, Action> hotkeyActionMap = new HashMap<KeyStroke, Action>();
		
		hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), yPlusAction);
		hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), yMinusAction);
		hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), xMinusAction);
		hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), xPlusAction);
		hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), zPlusAction);
		hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), zMinusAction);
		hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, 0), cMinusAction);
		hotkeyActionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD, 0), cPlusAction);
		
		Toolkit.getDefaultToolkit().getSystemEventQueue().push(new EventQueue() {
			@Override
			protected void dispatchEvent(AWTEvent event) {
				if (event instanceof KeyEvent) {
					KeyStroke ks = KeyStroke.getKeyStrokeForEvent((KeyEvent) event);
					Action action = hotkeyActionMap.get(ks);
					if (action != null && action.isEnabled()) {
						action.actionPerformed(null);
						return;
					}
				}
				super.dispatchEvent(event);
			}
		});
		
		setEnabled(machine.isReady());
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		droAction.setEnabled(enabled);
		xPlusAction.setEnabled(enabled);
		xMinusAction.setEnabled(enabled);
		yPlusAction.setEnabled(enabled);
		yMinusAction.setEnabled(enabled);
		zPlusAction.setEnabled(enabled);
		zMinusAction.setEnabled(enabled);
		cPlusAction.setEnabled(enabled);
		cMinusAction.setEnabled(enabled);
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
			throw new Error("Look, if you are going to set the units to " + units.toString() + " you are going to have to implement it first!");
		}
		this.units = units;
		updateDros();
	}
	
	private void updateDros() {
		if (machine == null || head == null || units == null) {
			return;
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
			cameraLocation = LengthUtil.convertLocation(cameraLocation, machine.getNativeUnits());
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
		x = LengthUtil.convertLength(x, machine.getNativeUnits(), units);
		y = LengthUtil.convertLength(y, machine.getNativeUnits(), units);
		z = LengthUtil.convertLength(z, machine.getNativeUnits(), units);
		if (!textFieldX.hasFocus()) {
			textFieldX.setText(String.format("%1.4f", x));
		}
		if (!textFieldY.hasFocus()) {
			textFieldY.setText(String.format("%1.4f", y));
		}
		if (!textFieldZ.hasFocus()) {
			textFieldZ.setText(String.format("%1.4f", z));
		}
		if (!textFieldC.hasFocus()) {
			textFieldC.setText(String.format("%1.4f", c));
		}
	}
	
	private double getJogIncrement() {
		if (units == LengthUnit.Millimeters) {
			return 0.01 * Math.pow(10, sliderIncrements.getValue() - 1);
		}
		else if (units == LengthUnit.Inches) {
			return 0.001 * Math.pow(10, sliderIncrements.getValue() - 1);
		}
		else {
			throw new Error("Look, if you are going to set the units to " + units.toString() + " you are going to have to implement it first!");
		}
	}
	
	private void jog(final int x, final int y, final int z, final int c) {
		executor.execute(new Runnable() {
			public void run() {
				try {
					double xPos = head.getX();
					double yPos = head.getY();
					double zPos = head.getZ();
					double cPos = head.getC();
					
					double jogIncrement = LengthUtil.convertLength(getJogIncrement(), units, machine.getNativeUnits());
					
					if (x > 0) {
						xPos += jogIncrement;
					}
					else if (x < 0) {
						xPos -= jogIncrement;
					}
					
					if (y > 0) {
						yPos += jogIncrement;
					}
					else if (y < 0) {
						yPos -= jogIncrement;
					}
					
					if (z > 0) {
						zPos += jogIncrement;
					}
					else if (z < 0) {
						zPos -= jogIncrement;
					}
					
					if (c > 0) {
						cPos += jogIncrement;
					}
					else if (c < 0) {
						cPos -= jogIncrement;
					}
					
					head.moveTo(xPos, yPos, zPos, cPos);
				}
				catch (Exception e) {
					e.printStackTrace();
					// TODO
				}
			}
		});
	}
	
	@Override
	public void machineHeadActivity(Machine machine, Head head) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				updateDros();
			}
		});
	}

	@Override
	public void machineStarted(Machine machine) {
		btnStartStop.setAction(machine.isReady() ? stopMachineAction : startMachineAction);
	}

	@Override
	public void machineStartFailed(Machine machine, String reason) {
		btnStartStop.setAction(machine.isReady() ? stopMachineAction : startMachineAction);
	}

	@Override
	public void machineStopped(Machine machine, String reason) {
		btnStartStop.setAction(machine.isReady() ? stopMachineAction : startMachineAction);
	}

	@Override
	public void machineStopFailed(Machine machine, String reason) {
		btnStartStop.setAction(machine.isReady() ? stopMachineAction : startMachineAction);
	}

	private void createUi() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
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
		
		JPanel panelControls = new JPanel();
		add(panelControls);
		GridBagLayout gbl_panelControls = new GridBagLayout();
		gbl_panelControls.rowHeights = new int[] { 25, 25, 25, 25, 25, 25 };
		panelControls.setLayout(gbl_panelControls);
		
		JButton btnYPlus = new JButton(yPlusAction);
		btnYPlus.setFocusable(false);
		btnYPlus.setPreferredSize(new Dimension(55, 50));
		GridBagConstraints gbc_btnYPlus = new GridBagConstraints();
		gbc_btnYPlus.gridheight = 2;
		gbc_btnYPlus.fill = GridBagConstraints.BOTH;
		gbc_btnYPlus.gridx = 3;
		gbc_btnYPlus.gridy = 0;
		panelControls.add(btnYPlus, gbc_btnYPlus);
		
		JButton btnZPlus = new JButton(zPlusAction);
		btnZPlus.setFocusable(false);
		btnZPlus.setPreferredSize(new Dimension(50, 29));
		GridBagConstraints gbc_btnZPlus = new GridBagConstraints();
		gbc_btnZPlus.insets = new Insets(0, 0, 5, 0);
		gbc_btnZPlus.gridheight = 3;
		gbc_btnZPlus.fill = GridBagConstraints.BOTH;
		gbc_btnZPlus.gridx = 5;
		gbc_btnZPlus.gridy = 0;
		panelControls.add(btnZPlus, gbc_btnZPlus);
		
		JButton btnXMinus = new JButton(xMinusAction);
		btnXMinus.setFocusable(false);
		btnXMinus.setPreferredSize(new Dimension(55, 50));
		GridBagConstraints gbc_btnXMinus = new GridBagConstraints();
		gbc_btnXMinus.insets = new Insets(0, 20, 0, 0);
		gbc_btnXMinus.fill = GridBagConstraints.BOTH;
		gbc_btnXMinus.gridheight = 2;
		gbc_btnXMinus.gridx = 2;
		gbc_btnXMinus.gridy = 2;
		panelControls.add(btnXMinus, gbc_btnXMinus);
		
		JButton btnXPlus = new JButton(xPlusAction);
		btnXPlus.setFocusable(false);
		btnXPlus.setPreferredSize(new Dimension(55, 50));
		GridBagConstraints gbc_btnXPlus = new GridBagConstraints();
		gbc_btnXPlus.insets = new Insets(0, 0, 0, 20);
		gbc_btnXPlus.gridheight = 2;
		gbc_btnXPlus.fill = GridBagConstraints.BOTH;
		gbc_btnXPlus.gridx = 4;
		gbc_btnXPlus.gridy = 2;
		panelControls.add(btnXPlus, gbc_btnXPlus);
		
		JButton btnCMinus = new JButton(cMinusAction);
		btnCMinus.setFocusable(false);
		btnCMinus.setPreferredSize(new Dimension(50, 29));
		GridBagConstraints gbc_btnCMinus = new GridBagConstraints();
		gbc_btnCMinus.insets = new Insets(0, 0, 0, 5);
		gbc_btnCMinus.gridheight = 4;
		gbc_btnCMinus.fill = GridBagConstraints.BOTH;
		gbc_btnCMinus.gridx = 0;
		gbc_btnCMinus.gridy = 1;
		panelControls.add(btnCMinus, gbc_btnCMinus);
		
		JButton btnCPlus = new JButton(cPlusAction);
		btnCPlus.setFocusable(false);
		btnCPlus.setPreferredSize(new Dimension(50, 29));
		GridBagConstraints gbc_btnCPlus = new GridBagConstraints();
		gbc_btnCPlus.insets = new Insets(0, 5, 0, 0);
		gbc_btnCPlus.gridheight = 4;
		gbc_btnCPlus.fill = GridBagConstraints.BOTH;
		gbc_btnCPlus.gridx = 1;
		gbc_btnCPlus.gridy = 1;
		panelControls.add(btnCPlus, gbc_btnCPlus);
		
		JButton btnZMinus = new JButton(zMinusAction);
		btnZMinus.setFocusable(false);
		btnZMinus.setPreferredSize(new Dimension(50, 29));
		GridBagConstraints gbc_btnZMinus = new GridBagConstraints();
		gbc_btnZMinus.insets = new Insets(5, 0, 0, 0);
		gbc_btnZMinus.gridheight = 3;
		gbc_btnZMinus.fill = GridBagConstraints.BOTH;
		gbc_btnZMinus.gridx = 5;
		gbc_btnZMinus.gridy = 3;
		panelControls.add(btnZMinus, gbc_btnZMinus);
		
		JButton btnYMinus = new JButton(yMinusAction);
		btnYMinus.setFocusable(false);
		btnYMinus.setPreferredSize(new Dimension(55, 50));
		GridBagConstraints gbc_btnYMinus = new GridBagConstraints();
		gbc_btnYMinus.gridheight = 2;
		gbc_btnYMinus.fill = GridBagConstraints.BOTH;
		gbc_btnYMinus.gridx = 3;
		gbc_btnYMinus.gridy = 4;
		panelControls.add(btnYMinus, gbc_btnYMinus);
		
		JPanel panelStartStop = new JPanel();
		add(panelStartStop);
		panelStartStop.setLayout(new BorderLayout(0, 0));
		
		btnStartStop = new JButton(startMachineAction);
		btnStartStop.setFocusable(false);
		btnStartStop.setForeground(new Color(178, 34, 34));
		panelStartStop.add(btnStartStop);
		btnStartStop.setFont(new Font("Lucida Grande", Font.BOLD, 48));
		btnStartStop.setPreferredSize(new Dimension(160, 70));
		
		setFocusTraversalPolicy(new FocusPolicy());
		setFocusTraversalPolicyProvider(true);
	}
	
	class FocusPolicy extends FocusTraversalPolicy {

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
		
	}
	
	@SuppressWarnings("serial")
	private Action yPlusAction = new AbstractAction("Y+") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			jog(0, 1, 0, 0);
		}
	};
	
	@SuppressWarnings("serial")
	private Action yMinusAction = new AbstractAction("Y-") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			jog(0, -1, 0, 0);
		}
	};
	
	@SuppressWarnings("serial")
	private Action xPlusAction = new AbstractAction("X+") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			jog(1, 0, 0, 0);
		}
	};
	
	@SuppressWarnings("serial")
	private Action xMinusAction = new AbstractAction("X-") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			jog(-1, 0, 0, 0);
		}
	};
	
	@SuppressWarnings("serial")
	private Action zPlusAction = new AbstractAction("Z+") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			jog(0, 0, 1, 0);
		}
	};
	
	@SuppressWarnings("serial")
	private Action zMinusAction = new AbstractAction("Z-") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			jog(0, 0, -1, 0);
		}
	};
	
	@SuppressWarnings("serial")
	private Action cPlusAction = new AbstractAction("C+") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			jog(0, 0, 0, 1);
		}
	};
	
	@SuppressWarnings("serial")
	private Action cMinusAction = new AbstractAction("C-") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			jog(0, 0, 0, -1);
		}
	};
	
	@SuppressWarnings("serial")
	private Action stopMachineAction = new AbstractAction("STOP") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			try {
				machine.stop();
				MachineControlsPanel.this.setEnabled(false);
			}
			catch (Exception e) {
				// TODO
				e.printStackTrace();
			}
		}
	};
	
	@SuppressWarnings("serial")
	private Action startMachineAction = new AbstractAction("START") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			try {
				machine.start();
				MachineControlsPanel.this.setEnabled(true);
			}
			catch (Exception e) {
				// TODO
				e.printStackTrace();
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
				cameraLocation = LengthUtil.convertLocation(cameraLocation, machine.getNativeUnits());
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
			sliderIncrements.requestFocusInWindow();
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
}
