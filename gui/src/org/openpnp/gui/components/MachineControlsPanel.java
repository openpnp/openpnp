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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.openpnp.LengthUnit;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.util.LengthUtil;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * Contains controls, DROs and status for the machine.
 * Controls: C right / left, X + / -, Y + / -, Z + / -, stop, pause, slider for jog increment
 * DROs: X, Y, Z, C
 * Status: LEDs for vac and actuators. TODO This part is not machine independant. Need to think about that.
 * Also: Radio buttons to select mm or inch.
 * TODO add a dropdown to select Head
 * TODO think about how commands to the machine should interface with the GUI. The GUI should not lock up while running
 * a command. Should the Machine queue the commands and have a way to check if it's done yet? 
 * @author jason
 *
 */
public class MachineControlsPanel extends JPanel {
	private Machine machine;
	private LengthUnit units;
	private ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(1);

	private JTextField textFieldX;
	private JTextField textFieldY;
	private JTextField textFieldC;
	private JTextField textFieldZ;
	private JSlider sliderIncrements;
	private JRadioButton rdbtnMm;
	private JRadioButton rdbtnInch;
	private final ButtonGroup buttonGroup = new ButtonGroup();

	/**
	 * Create the panel.
	 */
	public MachineControlsPanel() {
		createUi();
		
		// TODO it would be better if the Machine notified us of updates, then it could provide more timely
		// updates
		scheduler.scheduleAtFixedRate(new Runnable() {
			public void run() {
				if (!SwingUtilities.isEventDispatchThread()) {
					SwingUtilities.invokeLater(this);
					return;
				}
				updateDros();
			}
		}, 250, 250, TimeUnit.MILLISECONDS);
	}
	
	public void setMachine(Machine machine) {
		this.machine = machine;
		setUnits(machine.getNativeUnits());
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
		if (machine == null || units == null) {
			return;
		}
		Head head = machine.getHeads().get(0);
		double x = head.getX();
		double y = head.getY();
		double z = head.getZ();
		double c = head.getC();
		x = LengthUtil.convertLength(x, machine.getNativeUnits(), units);
		y = LengthUtil.convertLength(y, machine.getNativeUnits(), units);
		z = LengthUtil.convertLength(z, machine.getNativeUnits(), units);
		textFieldX.setText(String.format("%1.4f", x));
		textFieldY.setText(String.format("%1.4f", y));
		textFieldZ.setText(String.format("%1.4f", z));
		textFieldC.setText(String.format("%1.4f", c));
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
	
	private void jog(int x, int y, int z, int c) {
		Head head = machine.getHeads().get(0);
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
		
		try {
			head.moveTo(xPos, yPos, zPos, cPos);
		}
		catch (Exception e) {
			// TODO
		}
	}
	
	private void createUi() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		JPanel panel = new JPanel();
		add(panel);
		panel.setLayout(new BorderLayout(0, 0));
		
		JPanel panel_1 = new JPanel();
		panel.add(panel_1, BorderLayout.CENTER);
		
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
		textFieldX.setBackground(new Color(143, 188, 143));
		textFieldX.setFont(new Font("Lucida Grande", Font.BOLD, 24));
		textFieldX.setText("0000.0000");
		panelDrosFirstLine.add(textFieldX);
		textFieldX.setColumns(6);
		
		Component horizontalStrut = Box.createHorizontalStrut(15);
		panelDrosFirstLine.add(horizontalStrut);
		
		JLabel lblY = new JLabel("Y");
		lblY.setFont(new Font("Lucida Grande", Font.BOLD, 24));
		panelDrosFirstLine.add(lblY);
		
		textFieldY = new JTextField();
		textFieldY.setBackground(new Color(143, 188, 143));
		textFieldY.setFont(new Font("Lucida Grande", Font.BOLD, 24));
		textFieldY.setText("0000.0000");
		panelDrosFirstLine.add(textFieldY);
		textFieldY.setColumns(6);
		
		JPanel panelDrosSecondLine = new JPanel();
		panelDros.add(panelDrosSecondLine);
		panelDrosSecondLine.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		JLabel lblC = new JLabel("C");
		lblC.setFont(new Font("Lucida Grande", Font.BOLD, 24));
		panelDrosSecondLine.add(lblC);
		
		textFieldC = new JTextField();
		textFieldC.setBackground(new Color(143, 188, 143));
		textFieldC.setText("0000.0000");
		textFieldC.setFont(new Font("Lucida Grande", Font.BOLD, 24));
		textFieldC.setColumns(6);
		panelDrosSecondLine.add(textFieldC);
		
		Component horizontalStrut_1 = Box.createHorizontalStrut(15);
		panelDrosSecondLine.add(horizontalStrut_1);
		
		JLabel lblZ = new JLabel("Z");
		lblZ.setFont(new Font("Lucida Grande", Font.BOLD, 24));
		panelDrosSecondLine.add(lblZ);
		
		textFieldZ = new JTextField();
		textFieldZ.setBackground(new Color(143, 188, 143));
		textFieldZ.setText("0000.0000");
		textFieldZ.setFont(new Font("Lucida Grande", Font.BOLD, 24));
		textFieldZ.setColumns(6);
		panelDrosSecondLine.add(textFieldZ);
		
		JPanel panel_3 = new JPanel();
		add(panel_3);
		
		sliderIncrements = new JSlider();
		panel_3.add(sliderIncrements);
		sliderIncrements.setMajorTickSpacing(1);
		sliderIncrements.setValue(1);
		sliderIncrements.setSnapToTicks(true);
		sliderIncrements.setPaintLabels(true);
		sliderIncrements.setPaintTicks(true);
		sliderIncrements.setMinimum(1);
		sliderIncrements.setMaximum(4);
		
		JPanel panelUnits = new JPanel();
		panel_3.add(panelUnits);
		
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
		
		JButton btnYPlus = new JButton("Y+");
		btnYPlus.setFocusable(false);
		btnYPlus.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				jog(0, 1, 0, 0);
			}
		});
		btnYPlus.setPreferredSize(new Dimension(55, 50));
		GridBagConstraints gbc_btnYPlus = new GridBagConstraints();
		gbc_btnYPlus.gridheight = 2;
		gbc_btnYPlus.fill = GridBagConstraints.BOTH;
		gbc_btnYPlus.gridx = 3;
		gbc_btnYPlus.gridy = 0;
		panelControls.add(btnYPlus, gbc_btnYPlus);
		
		JButton btnZPlus = new JButton("Z+");
		btnZPlus.setFocusable(false);
		btnZPlus.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				jog(0, 0, 1, 0);
			}
		});
		btnZPlus.setPreferredSize(new Dimension(50, 29));
		GridBagConstraints gbc_btnZPlus = new GridBagConstraints();
		gbc_btnZPlus.insets = new Insets(0, 0, 5, 0);
		gbc_btnZPlus.gridheight = 3;
		gbc_btnZPlus.fill = GridBagConstraints.BOTH;
		gbc_btnZPlus.gridx = 5;
		gbc_btnZPlus.gridy = 0;
		panelControls.add(btnZPlus, gbc_btnZPlus);
		
		JButton btnXMinus = new JButton("X-");
		btnXMinus.setFocusable(false);
		btnXMinus.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				jog(-1, 0, 0, 0);
			}
		});
		btnXMinus.setPreferredSize(new Dimension(55, 50));
		GridBagConstraints gbc_btnXMinus = new GridBagConstraints();
		gbc_btnXMinus.insets = new Insets(0, 20, 0, 0);
		gbc_btnXMinus.fill = GridBagConstraints.BOTH;
		gbc_btnXMinus.gridheight = 2;
		gbc_btnXMinus.gridx = 2;
		gbc_btnXMinus.gridy = 2;
		panelControls.add(btnXMinus, gbc_btnXMinus);
		
		JButton btnXPlus = new JButton("X+");
		btnXPlus.setFocusable(false);
		btnXPlus.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				jog(1, 0, 0, 0);
			}
		});
		btnXPlus.setPreferredSize(new Dimension(55, 50));
		GridBagConstraints gbc_btnXPlus = new GridBagConstraints();
		gbc_btnXPlus.insets = new Insets(0, 0, 0, 20);
		gbc_btnXPlus.gridheight = 2;
		gbc_btnXPlus.fill = GridBagConstraints.BOTH;
		gbc_btnXPlus.gridx = 4;
		gbc_btnXPlus.gridy = 2;
		panelControls.add(btnXPlus, gbc_btnXPlus);
		
		JButton btnCMinus = new JButton("C-");
		btnCMinus.setFocusable(false);
		btnCMinus.setFocusTraversalPolicyProvider(true);
		btnCMinus.setFocusTraversalKeysEnabled(false);
		btnCMinus.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				jog(0, 0, 0, -1);
			}
		});
		btnCMinus.setPreferredSize(new Dimension(50, 29));
		GridBagConstraints gbc_btnCMinus = new GridBagConstraints();
		gbc_btnCMinus.insets = new Insets(0, 0, 0, 5);
		gbc_btnCMinus.gridheight = 4;
		gbc_btnCMinus.fill = GridBagConstraints.BOTH;
		gbc_btnCMinus.gridx = 0;
		gbc_btnCMinus.gridy = 1;
		panelControls.add(btnCMinus, gbc_btnCMinus);
		
		JButton btnCPlus = new JButton("C+");
		btnCPlus.setFocusable(false);
		btnCPlus.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				jog(0, 0, 0, 1);
			}
		});
		btnCPlus.setPreferredSize(new Dimension(50, 29));
		GridBagConstraints gbc_btnCPlus = new GridBagConstraints();
		gbc_btnCPlus.insets = new Insets(0, 5, 0, 0);
		gbc_btnCPlus.gridheight = 4;
		gbc_btnCPlus.fill = GridBagConstraints.BOTH;
		gbc_btnCPlus.gridx = 1;
		gbc_btnCPlus.gridy = 1;
		panelControls.add(btnCPlus, gbc_btnCPlus);
		
		JButton btnZMinus = new JButton("Z-");
		btnZMinus.setFocusable(false);
		btnZMinus.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				jog(0, 0, -1, 0);
			}
		});
		btnZMinus.setPreferredSize(new Dimension(50, 29));
		GridBagConstraints gbc_btnZMinus = new GridBagConstraints();
		gbc_btnZMinus.insets = new Insets(5, 0, 0, 0);
		gbc_btnZMinus.gridheight = 3;
		gbc_btnZMinus.fill = GridBagConstraints.BOTH;
		gbc_btnZMinus.gridx = 5;
		gbc_btnZMinus.gridy = 3;
		panelControls.add(btnZMinus, gbc_btnZMinus);
		
		JButton btnYMinus = new JButton("Y-");
		btnYMinus.setFocusable(false);
		btnYMinus.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				jog(0, -1, 0, 0);
			}
		});
		btnYMinus.setPreferredSize(new Dimension(55, 50));
		GridBagConstraints gbc_btnYMinus = new GridBagConstraints();
		gbc_btnYMinus.gridheight = 2;
		gbc_btnYMinus.fill = GridBagConstraints.BOTH;
		gbc_btnYMinus.gridx = 3;
		gbc_btnYMinus.gridy = 4;
		panelControls.add(btnYMinus, gbc_btnYMinus);
		
		JPanel panel_2 = new JPanel();
		add(panel_2);
		panel_2.setLayout(new BorderLayout(0, 0));
		
		JButton btnEstop = new JButton("E-STOP");
		btnEstop.setFocusable(false);
		btnEstop.setForeground(new Color(178, 34, 34));
		panel_2.add(btnEstop);
		btnEstop.setFont(new Font("Lucida Grande", Font.BOLD, 48));
		btnEstop.setPreferredSize(new Dimension(160, 70));
	}
}
