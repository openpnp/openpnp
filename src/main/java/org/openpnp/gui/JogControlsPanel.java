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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.Location;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.PasteDispenser;

/**
 * Contains controls, DROs and status for the machine. Controls: C right / left,
 * X + / -, Y + / -, Z + / -, stop, pause, slider for jog increment DROs: X, Y,
 * Z, C Radio buttons to select mm or inch.
 * 
 * @author jason
 */
public class JogControlsPanel extends JPanel {
	private final MachineControlsPanel machineControlsPanel;
	private final Frame frame;
	private final Configuration configuration;
    private JPanel panelActuators;
    private JPanel panelDispensers;

	/**
	 * Create the panel.
	 */
	public JogControlsPanel(Configuration configuration,
			MachineControlsPanel machineControlsPanel, Frame frame) {
		this.machineControlsPanel = machineControlsPanel;
		this.frame = frame;
		this.configuration = configuration;

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
		pickAction.setEnabled(enabled);
		placeAction.setEnabled(enabled);
		for (Component c : panelActuators.getComponents()) {
			c.setEnabled(enabled);
		}
	}

	private void jog(final int x, final int y, final int z, final int c) {
		machineControlsPanel.submitMachineTask(new Runnable() {
			public void run() {
				try {
				    Location l = machineControlsPanel.getSelectedNozzle().getLocation().convertToUnits(Configuration.get().getSystemUnits());
					double xPos = l.getX();
					double yPos = l.getY();
					double zPos = l.getZ();
					double cPos = l.getRotation();

					double jogIncrement = new Length(machineControlsPanel
							.getJogIncrement(), configuration.getSystemUnits())
							.getValue();

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
						cPos -= jogIncrement;
					}
					else if (c < 0) {
						cPos += jogIncrement;
					}
					
					machineControlsPanel.getSelectedNozzle().moveTo(new Location(l.getUnits(), xPos, yPos, zPos, cPos), 1.0);
				}
				catch (Exception e) {
					MessageBoxes.errorBox(frame, "Jog Failed", e.getMessage());
				}
			}
		});
	}

	private void createUi() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

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

		JButton btnHome = new JButton(machineControlsPanel.homeAction);
		btnHome.setFocusable(false);
		panelSpecial.add(btnHome);
		
		JButton btnPick = new JButton(pickAction);
		panelSpecial.add(btnPick);
		
		JButton btnPlace = new JButton(placeAction);
		panelSpecial.add(btnPlace);
		
		panelDispensers = new JPanel();
		FlowLayout flowLayout = (FlowLayout) panelDispensers.getLayout();
		flowLayout.setAlignment(FlowLayout.LEFT);
		tabbedPane.addTab("Paste Dispensers", null, panelDispensers, null);
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
    public Action pickAction = new AbstractAction("Pick") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            machineControlsPanel.submitMachineTask(new Runnable() {
                public void run() {
                    try {
                        machineControlsPanel.getSelectedNozzle().pick();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        MessageBoxes.errorBox(frame,
                                "Pick Operation Failed", e.getMessage());
                    }
                }
            });
        }
    };

    @SuppressWarnings("serial")
    public Action placeAction = new AbstractAction("Place") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            machineControlsPanel.submitMachineTask(new Runnable() {
                public void run() {
                    try {
                        machineControlsPanel.getSelectedNozzle().place();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        MessageBoxes.errorBox(frame,
                                "Place Operation Failed", e.getMessage());
                    }
                }
            });
        }
    };

	private ConfigurationListener configurationListener = new ConfigurationListener.Adapter() {
		@Override
		public void configurationComplete(Configuration configuration) throws Exception {
			panelActuators.removeAll();

			Machine machine = Configuration.get().getMachine();
			
			for (final Head head : machine.getHeads()) {
                for (Actuator actuator : head.getActuators()) {
                    final Actuator actuator_f = actuator;
                    final JToggleButton actuatorButton = new JToggleButton(
                            head.getName() + ":" + actuator_f.getName());
                    actuatorButton.setFocusable(false);
                    actuatorButton.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            final boolean state = actuatorButton.isSelected();
                            machineControlsPanel.submitMachineTask(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                actuator_f.actuate(state);
                                            }
                                            catch (Exception e) {
                                                MessageBoxes.errorBox(frame,
                                                        "Actuator Command Failed",
                                                        e.getMessage());
                                            }
                                        }
                                    });
                        }
                    });
                    panelActuators.add(actuatorButton);
                }
                for (final PasteDispenser dispenser : head.getPasteDispensers()) {
                    final JButton dispenserButton = new JButton(
                            head.getName() + ":" + dispenser.getName());
                    dispenserButton.setFocusable(false);
                    dispenserButton.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            machineControlsPanel.submitMachineTask(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                dispenser.dispense(null, null, 250);
                                            }
                                            catch (Exception e) {
                                                MessageBoxes.errorBox(frame,
                                                        "Dispenser Command Failed",
                                                        e.getMessage());
                                            }
                                        }
                                    });
                        }
                    });
                    panelDispensers.add(dispenserButton);
                }
			}

			setEnabled(machineControlsPanel.isEnabled());
		}
	};
}
