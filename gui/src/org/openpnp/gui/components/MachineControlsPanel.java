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
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.Hashtable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.AbstractAction;
import javax.swing.Action;
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
@SuppressWarnings("serial")
public class MachineControlsPanel extends JPanel {
	private JTextField xDroTextField, yDroTextField, zDroTextField, cDroTextField;
	private JLabel[] statusLedLabels;
	private JSlider incrementsSlider;
	private LengthUnit units;
	private Machine machine;
	private JRadioButton mmUnitsRadioButton;
	private JRadioButton inchUnitsRadioButton;
	private ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(1);
	
	public MachineControlsPanel() {
		createUi();
	}
	
	private void createUi() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

//		Font droLabelFont = getFont().deriveFont(16f);
//		Font droTextFieldFont = getFont().deriveFont(16f);
		Font droLabelFont = getFont();
		Font droTextFieldFont = getFont();

		JLabel xDroLabel = new JLabel("X");
		xDroLabel.setFont(droLabelFont);
		
		JLabel yDroLabel = new JLabel("Y");
		yDroLabel.setFont(droLabelFont);
		
		JLabel zDroLabel = new JLabel("Z");
		zDroLabel.setFont(droLabelFont);
		
		JLabel cDroLabel = new JLabel("C");
		cDroLabel.setFont(droLabelFont);
		
		xDroTextField = new JTextField();
		xDroTextField.setFont(droTextFieldFont);
		xDroTextField.setColumns(9);
		
		yDroTextField = new JTextField();
		yDroTextField.setFont(droTextFieldFont);
		yDroTextField.setColumns(9);
		
		zDroTextField = new JTextField();
		zDroTextField.setFont(droTextFieldFont);
		zDroTextField.setColumns(9);
		
		cDroTextField = new JTextField();
		cDroTextField.setFont(droTextFieldFont);
		cDroTextField.setColumns(9);
		
		mmUnitsRadioButton = new JRadioButton(mmUnitsAction);
		inchUnitsRadioButton = new JRadioButton(inchUnitsAction);
		
		ButtonGroup unitsButtonGroup = new ButtonGroup();
		unitsButtonGroup.add(mmUnitsRadioButton);
		unitsButtonGroup.add(inchUnitsRadioButton);
		
		JPanel droFirstLineBox = new JPanel(new FlowLayout());
		droFirstLineBox.add(xDroLabel);
		droFirstLineBox.add(xDroTextField);
		droFirstLineBox.add(Box.createHorizontalStrut(10));
		droFirstLineBox.add(yDroLabel);
		droFirstLineBox.add(yDroTextField);
		
		JPanel droSecondLineBox = new JPanel(new FlowLayout());
		droSecondLineBox.add(cDroLabel);
		droSecondLineBox.add(cDroTextField);
		droSecondLineBox.add(Box.createHorizontalStrut(10));
		droSecondLineBox.add(zDroLabel);
		droSecondLineBox.add(zDroTextField);

		Box droBox = Box.createVerticalBox();
		droBox.add(droFirstLineBox);
		droBox.add(droSecondLineBox);
		
		Box unitsBox = Box.createVerticalBox();
		unitsBox.add(mmUnitsRadioButton);
		unitsBox.add(inchUnitsRadioButton);
		
		incrementsSlider = new JSlider(JSlider.HORIZONTAL);
		incrementsSlider.setMinimum(1);
		incrementsSlider.setMaximum(4);
		incrementsSlider.setSnapToTicks(true);
		incrementsSlider.setPaintLabels(true);
		incrementsSlider.setPaintTicks(true);
		
		JPanel droAndUnitsBox = new JPanel(new BorderLayout());
		droAndUnitsBox.add(droBox, BorderLayout.CENTER);
		droAndUnitsBox.add(unitsBox, BorderLayout.EAST);
		droAndUnitsBox.add(incrementsSlider, BorderLayout.SOUTH);
		
		add(droAndUnitsBox);
		
		JButton counterClockwiseButton = new JButton(cNegativeAction);
		counterClockwiseButton.setPreferredSize(new Dimension(40, 180));
		
		JButton clockwiseButton = new JButton(cPositiveAction);
		clockwiseButton.setPreferredSize(new Dimension(40, 180));
		
		JButton xNegativeButton = new JButton(xNegativeAction);
		xNegativeButton.setPreferredSize(new Dimension(70, 70));
		
		JButton xPositiveButton = new JButton(xPositiveAction);
		xPositiveButton.setPreferredSize(new Dimension(70, 70));
		
		JButton yPositiveButton = new JButton(yPositiveAction);
		yPositiveButton.setPreferredSize(new Dimension(70, 70));
		
		JButton yNegativeButton = new JButton(yNegativeAction);
		yNegativeButton.setPreferredSize(new Dimension(70, 70));
		
		JButton zPositiveButton = new JButton(zPositiveAction);
		zPositiveButton.setPreferredSize(new Dimension(70, 70));
		
		JButton zNegativeButton = new JButton(zNegativeAction);
		zNegativeButton.setPreferredSize(new Dimension(70, 70));
		
		JPanel buttonsBox = new JPanel(new FlowLayout());
		buttonsBox.add(counterClockwiseButton);
		buttonsBox.add(clockwiseButton);
		
		JPanel xyButtonsBox = new JPanel(new GridLayout(3, 3));
		xyButtonsBox.add(Box.createGlue());
		xyButtonsBox.add(yPositiveButton);
		xyButtonsBox.add(Box.createGlue());
		xyButtonsBox.add(xNegativeButton);
		xyButtonsBox.add(Box.createGlue());
		xyButtonsBox.add(xPositiveButton);
		xyButtonsBox.add(Box.createGlue());
		xyButtonsBox.add(yNegativeButton);
		xyButtonsBox.add(Box.createGlue());
		
		buttonsBox.add(xyButtonsBox);
		
		JPanel zButtonsBox = new JPanel(new GridLayout(2, 1));
		zButtonsBox.add(zPositiveButton);
		zButtonsBox.add(zNegativeButton);
		
		buttonsBox.add(zButtonsBox);
		
		add(buttonsBox);

		incrementsSlider.setValue(1);
		
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
			incrementsSlider.setLabelTable(incrementsLabels);
			mmUnitsRadioButton.setSelected(true);
		}
		else if (units == LengthUnit.Inches) {
			Hashtable<Integer, JLabel> incrementsLabels = new Hashtable<Integer, JLabel>();
			incrementsLabels.put(1, new JLabel("0.001"));
			incrementsLabels.put(2, new JLabel("0.01"));
			incrementsLabels.put(3, new JLabel("0.1"));
			incrementsLabels.put(4, new JLabel("1"));
			incrementsSlider.setLabelTable(incrementsLabels);
			inchUnitsRadioButton.setSelected(true);
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
		xDroTextField.setText(String.format("%1.4f", x));
		yDroTextField.setText(String.format("%1.4f", y));
		zDroTextField.setText(String.format("%1.4f", z));
		cDroTextField.setText(String.format("%1.4f", c));
	}
	
	private double getJogIncrement() {
		if (units == LengthUnit.Millimeters) {
			return 0.01 * Math.pow(10, incrementsSlider.getValue() - 1);
		}
		else if (units == LengthUnit.Inches) {
			return 0.001 * Math.pow(10, incrementsSlider.getValue() - 1);
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
	
	private Action mmUnitsAction = new AbstractAction("MM") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			setUnits(LengthUnit.Millimeters);
		}
	};
	
	private Action inchUnitsAction = new AbstractAction("Inch") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			setUnits(LengthUnit.Inches);
		}
	};
	
	private Action xPositiveAction = new AbstractAction("+") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			jog(1, 0, 0, 0);
		}
	};
	
	private Action xNegativeAction = new AbstractAction("-") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			jog(-1, 0, 0, 0);
		}
	};
	
	private Action yPositiveAction = new AbstractAction("+") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			jog(0, 1, 0, 0);
		}
	};
	
	private Action yNegativeAction = new AbstractAction("-") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			jog(0, -1, 0, 0);
		}
	};
	
	private Action zPositiveAction = new AbstractAction("+") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			jog(0, 0, 1, 0);
		}
	};
	
	private Action zNegativeAction = new AbstractAction("-") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			jog(0, 0, -1, 0);
		}
	};
	
	private Action cPositiveAction = new AbstractAction(")") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			jog(0, 0, 0, 1);
		}
	};
	
	private Action cNegativeAction = new AbstractAction("(") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			jog(0, 0, 0, -1);
		}
	};
}
