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

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FocusTraversalPolicy;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;

import org.openpnp.ConfigurationListener;
import org.openpnp.LengthUnit;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.util.LengthUtil;

/**
 * Contains controls, DROs and status for the machine.
 * Controls: C right / left, X + / -, Y + / -, Z + / -, stop, pause, slider for jog increment
 * DROs: X, Y, Z, C
 * Radio buttons to select mm or inch.
 * TODO add a dropdown to select Head
 * @author jason
 */
public class JogControlsPanel extends JPanel {
	private final MachineControlsPanel machineControlsPanel;
	private final Frame frame;
	
	private Machine machine;
	private Head head;
	private LengthUnit units;
	private JSlider sliderIncrements;
	private JRadioButton rdbtnMm;
	private JRadioButton rdbtnInch;
	private JToggleButton btnPickPlace;
	private final ButtonGroup buttonGroup = new ButtonGroup();
	private JPanel panelActuators;
	
	// TODO: Move out to main, or get it from the MachineControlsPanel
	private ExecutorService executor = Executors.newSingleThreadExecutor();
	
	/**
	 * Create the panel.
	 */
	public JogControlsPanel(Configuration configuration, MachineControlsPanel machineControlsPanel, Frame frame) {
		this.machineControlsPanel = machineControlsPanel;
		this.frame = frame;
		
		createUi();
		
		configuration.addListener(configurationListener);
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		xPlusAction.setEnabled(enabled);
		xMinusAction.setEnabled(enabled);
		yPlusAction.setEnabled(enabled);
		yMinusAction.setEnabled(enabled);
		zPlusAction.setEnabled(enabled);
		zMinusAction.setEnabled(enabled);
		cPlusAction.setEnabled(enabled);
		cMinusAction.setEnabled(enabled);
		pickPlaceAction.setEnabled(enabled);
		for (Component c : panelActuators.getComponents()) {
			c.setEnabled(enabled);
		}
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
		machineControlsPanel.updateDros();
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
					MessageBoxes.errorBox(frame, "Jog Failed", e.getMessage());
				}
			}
		});
	}
	
	private void createUi() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
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
		gbc_btnYPlus.insets = new Insets(0, 0, 5, 0);
		gbc_btnYPlus.gridheight = 2;
		gbc_btnYPlus.fill = GridBagConstraints.BOTH;
		gbc_btnYPlus.gridx = 3;
		gbc_btnYPlus.gridy = 0;
		panelControls.add(btnYPlus, gbc_btnYPlus);
		
		JButton btnZPlus = new JButton(zPlusAction);
		btnZPlus.setFocusable(false);
		btnZPlus.setPreferredSize(new Dimension(50, 29));
		GridBagConstraints gbc_btnZPlus = new GridBagConstraints();
		gbc_btnZPlus.insets = new Insets(0, 5, 5, 0);
		gbc_btnZPlus.gridheight = 3;
		gbc_btnZPlus.fill = GridBagConstraints.BOTH;
		gbc_btnZPlus.gridx = 5;
		gbc_btnZPlus.gridy = 0;
		panelControls.add(btnZPlus, gbc_btnZPlus);
		
		JButton btnXMinus = new JButton(xMinusAction);
		btnXMinus.setFocusable(false);
		btnXMinus.setPreferredSize(new Dimension(55, 50));
		GridBagConstraints gbc_btnXMinus = new GridBagConstraints();
		gbc_btnXMinus.insets = new Insets(0, 0, 5, 5);
		gbc_btnXMinus.fill = GridBagConstraints.BOTH;
		gbc_btnXMinus.gridheight = 2;
		gbc_btnXMinus.gridx = 2;
		gbc_btnXMinus.gridy = 2;
		panelControls.add(btnXMinus, gbc_btnXMinus);
		
		btnPickPlace = new JToggleButton(pickPlaceAction);
		btnPickPlace.setFocusable(false);
		btnPickPlace.setPreferredSize(new Dimension(55, 50));
		btnPickPlace.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
			}
		});
		GridBagConstraints gbc_btnPickPlace = new GridBagConstraints();
		gbc_btnPickPlace.insets = new Insets(0, 0, 5, 0);
		gbc_btnPickPlace.fill = GridBagConstraints.BOTH;
		gbc_btnPickPlace.gridheight = 2;
		gbc_btnPickPlace.gridx = 3;
		gbc_btnPickPlace.gridy = 2;
		panelControls.add(btnPickPlace, gbc_btnPickPlace);
		
		JButton btnXPlus = new JButton(xPlusAction);
		btnXPlus.setFocusable(false);
		btnXPlus.setPreferredSize(new Dimension(55, 50));
		GridBagConstraints gbc_btnXPlus = new GridBagConstraints();
		gbc_btnXPlus.insets = new Insets(0, 5, 5, 0);
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
		gbc_btnCPlus.insets = new Insets(0, 0, 0, 5);
		gbc_btnCPlus.gridheight = 4;
		gbc_btnCPlus.fill = GridBagConstraints.BOTH;
		gbc_btnCPlus.gridx = 1;
		gbc_btnCPlus.gridy = 1;
		panelControls.add(btnCPlus, gbc_btnCPlus);
		
		JButton btnZMinus = new JButton(zMinusAction);
		btnZMinus.setFocusable(false);
		btnZMinus.setPreferredSize(new Dimension(50, 29));
		GridBagConstraints gbc_btnZMinus = new GridBagConstraints();
		gbc_btnZMinus.insets = new Insets(5, 5, 0, 0);
		gbc_btnZMinus.gridheight = 3;
		gbc_btnZMinus.fill = GridBagConstraints.BOTH;
		gbc_btnZMinus.gridx = 5;
		gbc_btnZMinus.gridy = 3;
		panelControls.add(btnZMinus, gbc_btnZMinus);
		
		JButton btnYMinus = new JButton(yMinusAction);
		btnYMinus.setFocusable(false);
		btnYMinus.setPreferredSize(new Dimension(55, 50));
		GridBagConstraints gbc_btnYMinus = new GridBagConstraints();
		gbc_btnYMinus.insets = new Insets(5, 0, 0, 0);
		gbc_btnYMinus.gridheight = 2;
		gbc_btnYMinus.fill = GridBagConstraints.BOTH;
		gbc_btnYMinus.gridx = 3;
		gbc_btnYMinus.gridy = 4;
		panelControls.add(btnYMinus, gbc_btnYMinus);
		
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		add(tabbedPane);
		
		JPanel panelSpecial = new JPanel();
		tabbedPane.addTab("Special Commands", null, panelSpecial, null);
		FlowLayout flowLayout_1 = (FlowLayout) panelSpecial.getLayout();
		flowLayout_1.setAlignment(FlowLayout.LEFT);
		
		panelActuators = new JPanel();
		tabbedPane.addTab("Actuators", null, panelActuators, null);
		FlowLayout fl_panelActuators = (FlowLayout) panelActuators.getLayout();
		fl_panelActuators.setAlignment(FlowLayout.LEFT);
		
		JButton btnNewButton = new JButton(machineControlsPanel.homeAction);
		btnNewButton.setFocusable(false);
		panelSpecial.add(btnNewButton);
		
		setFocusTraversalPolicy(focusPolicy);
		setFocusTraversalPolicyProvider(true);
	}
	
	@SuppressWarnings("serial")
	public Action yPlusAction = new AbstractAction("Y+") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			jog(0, 1, 0, 0);
		}
	};
	
	@SuppressWarnings("serial")
	public Action yMinusAction = new AbstractAction("Y-") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			jog(0, -1, 0, 0);
		}
	};
	
	@SuppressWarnings("serial")
	public Action xPlusAction = new AbstractAction("X+") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			jog(1, 0, 0, 0);
		}
	};
	
	@SuppressWarnings("serial")
	public Action xMinusAction = new AbstractAction("X-") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			jog(-1, 0, 0, 0);
		}
	};
	
	@SuppressWarnings("serial")
	public Action zPlusAction = new AbstractAction("Z+") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			jog(0, 0, 1, 0);
		}
	};
	
	@SuppressWarnings("serial")
	public Action zMinusAction = new AbstractAction("Z-") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			jog(0, 0, -1, 0);
		}
	};
	
	@SuppressWarnings("serial")
	public Action cPlusAction = new AbstractAction("C+") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			jog(0, 0, 0, 1);
		}
	};
	
	@SuppressWarnings("serial")
	public Action cMinusAction = new AbstractAction("C-") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			jog(0, 0, 0, -1);
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
	public Action pickPlaceAction = new AbstractAction("O") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			final boolean state = btnPickPlace.isSelected();
			executor.submit(new Runnable() {
				public void run() {
					try {
						if (state) {
							head.pick();
						}
						else {
							head.place();
						}
					}
					catch (Exception e) {
						MessageBoxes.errorBox(frame, "Pick/Place Operation Failed", e.getMessage());
					}
				}
			});
		}
	};
	
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
	
	private ConfigurationListener configurationListener = new ConfigurationListener.Adapter() {
		@Override
		public void configurationLoaded(Configuration configuration) {
			panelActuators.removeAll();
			
			machine = configuration.getMachine();
			head = machine.getHeads().get(0);
			setUnits(machine.getNativeUnits());
			

			for (String actuatorName : head.getActuatorNames()) {
				final String actuatorName_f = actuatorName;
				final JToggleButton actuatorButton = new JToggleButton(actuatorName);
				actuatorButton.setFocusable(false);
				actuatorButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						final boolean state = actuatorButton.isSelected();
						executor.execute(new Runnable() {
							@Override
							public void run() {
								try {
									head.actuate(actuatorName_f, state);
								}
								catch (Exception e) {
									MessageBoxes.errorBox(frame, "Actuator Command Failed", e.getMessage());
								}
							}
						});
					}
				});
				panelActuators.add(actuatorButton);
			}
			
			setEnabled(machineControlsPanel.isEnabled());
		}
	};
}
