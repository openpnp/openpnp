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

package org.openpnp.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.FocusTraversalPolicy;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.support.Icons;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PasteDispenser;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.UiUtils;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

/**
 * Contains controls, DROs and status for the machine. Controls: C right / left, X + / -, Y + / -, Z
 * + / -, stop, pause, slider for jog increment DROs: X, Y, Z, C Radio buttons to select mm or inch.
 * 
 * @author jason
 */
public class JogControlsPanel extends JPanel {
    private final MachineControlsPanel machineControlsPanel;
    private final Frame frame;
    private final Configuration configuration;
    private JPanel panelActuators;
    private JPanel panelDispensers;
    private JSlider sliderIncrements;

    /**
     * Create the panel.
     */
    public JogControlsPanel(Configuration configuration, MachineControlsPanel machineControlsPanel,
            Frame frame) {
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
        discardAction.setEnabled(enabled);
        safezAction.setEnabled(enabled);
        xyZeroAction.setEnabled(enabled);
        zZeroAction.setEnabled(enabled);
        cZeroAction.setEnabled(enabled);
        for (Component c : panelActuators.getComponents()) {
            c.setEnabled(enabled);
        }
        for (Component c : panelDispensers.getComponents()) {
            c.setEnabled(enabled);
        }
    }

    private void setUnits(LengthUnit units) {
        if (units == LengthUnit.Millimeters) {
            Hashtable<Integer, JLabel> incrementsLabels = new Hashtable<>();
            incrementsLabels.put(1, new JLabel("0.01 " + units.getShortName()));
            incrementsLabels.put(2, new JLabel("0.1 " + units.getShortName()));
            incrementsLabels.put(3, new JLabel("1.0 " + units.getShortName()));
            incrementsLabels.put(4, new JLabel("10 " + units.getShortName()));
            incrementsLabels.put(5, new JLabel("100 " + units.getShortName()));
            sliderIncrements.setLabelTable(incrementsLabels);
        }
        else if (units == LengthUnit.Inches) {
            Hashtable<Integer, JLabel> incrementsLabels = new Hashtable<>();
            incrementsLabels.put(1, new JLabel("0.001 " + units.getShortName()));
            incrementsLabels.put(2, new JLabel("0.01 " + units.getShortName()));
            incrementsLabels.put(3, new JLabel("0.1 " + units.getShortName()));
            incrementsLabels.put(4, new JLabel("1.0 " + units.getShortName()));
            incrementsLabels.put(5, new JLabel("10.0 " + units.getShortName()));
            sliderIncrements.setLabelTable(incrementsLabels);
        }
        else {
            throw new Error("setUnits() not implemented for " + units);
        }
        machineControlsPanel.updateDros();
    }

    public double getJogIncrement() {
        if (configuration.getSystemUnits() == LengthUnit.Millimeters) {
            return 0.01 * Math.pow(10, sliderIncrements.getValue() - 1);
        }
        else if (configuration.getSystemUnits() == LengthUnit.Inches) {
            return 0.001 * Math.pow(10, sliderIncrements.getValue() - 1);
        }
        else {
            throw new Error(
                    "getJogIncrement() not implemented for " + configuration.getSystemUnits());
        }
    }

    private void jog(final int x, final int y, final int z, final int c) {
        UiUtils.submitUiMachineTask(() -> {
            Location l = machineControlsPanel.getSelectedNozzle().getLocation()
                    .convertToUnits(Configuration.get().getSystemUnits());
            double xPos = l.getX();
            double yPos = l.getY();
            double zPos = l.getZ();
            double cPos = l.getRotation();

            double jogIncrement =
                    new Length(getJogIncrement(), configuration.getSystemUnits()).getValue();

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

            machineControlsPanel.getSelectedNozzle()
                    .moveTo(new Location(l.getUnits(), xPos, yPos, zPos, cPos));
        });
    }

    private void zero(boolean xy, boolean z, boolean c) {
        UiUtils.submitUiMachineTask(() -> {
            Location l = machineControlsPanel.getSelectedNozzle().getLocation()
                    .convertToUnits(Configuration.get().getSystemUnits());
            l = l.derive(xy ? 0d : null, xy ? 0d : null, z ? 0d : null, c ? 0d : null);
            machineControlsPanel.getSelectedNozzle().moveTo(l);
        });
    }

    private void createUi() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JPanel panel = new JPanel();
        add(panel);
        panel.setBorder(null);

        JPanel panelControls = new JPanel();
        panel.add(panelControls);
        panelControls.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"),},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        JButton homeButton = new JButton(machineControlsPanel.homeAction);
        // We set this Icon explicitly as a WindowBuilder helper. WindowBuilder can't find the
        // homeAction referenced above so the icon doesn't render in the viewer. We set it here
        // so the dialog looks right while editing.
        homeButton.setIcon(Icons.home);
        homeButton.setHideActionText(true);
        panelControls.add(homeButton, "2, 2");

        JLabel lblXy = new JLabel("X/Y");
        lblXy.setFont(new Font("Lucida Grande", Font.PLAIN, 22));
        lblXy.setHorizontalAlignment(SwingConstants.CENTER);
        panelControls.add(lblXy, "8, 2, fill, default");

        JLabel lblZ = new JLabel("Z");
        lblZ.setHorizontalAlignment(SwingConstants.CENTER);
        lblZ.setFont(new Font("Lucida Grande", Font.PLAIN, 22));
        panelControls.add(lblZ, "14, 2");

        JLabel lblDistance = new JLabel("Distance");
        lblDistance.setFont(new Font("Lucida Grande", Font.PLAIN, 10));
        panelControls.add(lblDistance, "18, 2, center, center");

        JLabel lblSpeed = new JLabel("Speed %");
        lblSpeed.setFont(new Font("Lucida Grande", Font.PLAIN, 10));
        panelControls.add(lblSpeed, "20, 2, center, center");

        sliderIncrements = new JSlider();
        panelControls.add(sliderIncrements, "18, 3, 1, 10");
        sliderIncrements.setOrientation(SwingConstants.VERTICAL);
        sliderIncrements.setMajorTickSpacing(1);
        sliderIncrements.setValue(1);
        sliderIncrements.setSnapToTicks(true);
        sliderIncrements.setPaintLabels(true);
        sliderIncrements.setMinimum(1);
        sliderIncrements.setMaximum(5);

        JButton yPlusButton = new JButton(yPlusAction);
        yPlusButton.setHideActionText(true);
        panelControls.add(yPlusButton, "8, 4");

        JButton zUpButton = new JButton(zPlusAction);
        zUpButton.setHideActionText(true);
        panelControls.add(zUpButton, "14, 4");

        speedSlider = new JSlider();
        speedSlider.setValue(100);
        speedSlider.setPaintTicks(true);
        speedSlider.setMinorTickSpacing(1);
        speedSlider.setMajorTickSpacing(25);
        speedSlider.setSnapToTicks(true);
        speedSlider.setPaintLabels(true);
        speedSlider.setOrientation(SwingConstants.VERTICAL);
        panelControls.add(speedSlider, "20, 4, 1, 9");
        speedSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                Configuration.get().getMachine().setSpeed(speedSlider.getValue() * 0.01);
            }
        });

        JButton buttonStartStop = new JButton(machineControlsPanel.startStopMachineAction);
        buttonStartStop.setIcon(Icons.powerOn);
        panelControls.add(buttonStartStop, "2, 6");
        buttonStartStop.setHideActionText(true);

        JButton xMinusButton = new JButton(xMinusAction);
        xMinusButton.setHideActionText(true);
        panelControls.add(xMinusButton, "6, 6");

        JButton homeXyButton = new JButton(xyZeroAction);
        homeXyButton.setHideActionText(true);
        panelControls.add(homeXyButton, "8, 6");

        JButton xPlusButton = new JButton(xPlusAction);
        xPlusButton.setHideActionText(true);
        panelControls.add(xPlusButton, "10, 6");

        JButton homeZButton = new JButton(zZeroAction);
        homeZButton.setHideActionText(true);
        panelControls.add(homeZButton, "14, 6");

        JButton yMinusButton = new JButton(yMinusAction);
        yMinusButton.setHideActionText(true);
        panelControls.add(yMinusButton, "8, 8");

        JButton zDownButton = new JButton(zMinusAction);
        zDownButton.setHideActionText(true);
        panelControls.add(zDownButton, "14, 8");

        JLabel lblC = new JLabel("C");
        lblC.setHorizontalAlignment(SwingConstants.CENTER);
        lblC.setFont(new Font("Lucida Grande", Font.PLAIN, 22));
        panelControls.add(lblC, "4, 12");

        JButton counterclockwiseButton = new JButton(cPlusAction);
        counterclockwiseButton.setHideActionText(true);
        panelControls.add(counterclockwiseButton, "6, 12");

        JButton homeCButton = new JButton(cZeroAction);
        homeCButton.setHideActionText(true);
        panelControls.add(homeCButton, "8, 12");

        JButton clockwiseButton = new JButton(cMinusAction);
        clockwiseButton.setHideActionText(true);
        panelControls.add(clockwiseButton, "10, 12");

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

        JButton btnSafeZ = new JButton(safezAction);
        panelSpecial.add(btnSafeZ);

        JButton btnDiscard = new JButton(discardAction);
        panelSpecial.add(btnDiscard);

        panelDispensers = new JPanel();
        FlowLayout flowLayout = (FlowLayout) panelDispensers.getLayout();
        flowLayout.setAlignment(FlowLayout.LEFT);
        tabbedPane.addTab("Paste Dispensers", null, panelDispensers, null);

        setFocusTraversalPolicy(focusPolicy);
        setFocusTraversalPolicyProvider(true);
    }

    private FocusTraversalPolicy focusPolicy = new FocusTraversalPolicy() {
        @Override
        public Component getComponentAfter(Container aContainer, Component aComponent) {
            return sliderIncrements;
        }

        @Override
        public Component getComponentBefore(Container aContainer, Component aComponent) {
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

    public double getSpeed() {
        return speedSlider.getValue() * 0.01D;
    }

    @SuppressWarnings("serial")
    public Action yPlusAction = new AbstractAction("Y+", Icons.arrowUp) {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            jog(0, 1, 0, 0);
        }
    };

    @SuppressWarnings("serial")
    public Action yMinusAction = new AbstractAction("Y-", Icons.arrowDown) {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            jog(0, -1, 0, 0);
        }
    };

    @SuppressWarnings("serial")
    public Action xPlusAction = new AbstractAction("X+", Icons.arrowRight) {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            jog(1, 0, 0, 0);
        }
    };

    @SuppressWarnings("serial")
    public Action xMinusAction = new AbstractAction("X-", Icons.arrowLeft) {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            jog(-1, 0, 0, 0);
        }
    };

    @SuppressWarnings("serial")
    public Action zPlusAction = new AbstractAction("Z+", Icons.arrowUp) {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            jog(0, 0, 1, 0);
        }
    };

    @SuppressWarnings("serial")
    public Action zMinusAction = new AbstractAction("Z-", Icons.arrowDown) {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            jog(0, 0, -1, 0);
        }
    };

    @SuppressWarnings("serial")
    public Action cPlusAction = new AbstractAction("C+", Icons.rotateCounterclockwise) {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            jog(0, 0, 0, 1);
        }
    };

    @SuppressWarnings("serial")
    public Action cMinusAction = new AbstractAction("C-", Icons.rotateClockwise) {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            jog(0, 0, 0, -1);
        }
    };

    @SuppressWarnings("serial")
    public Action xyZeroAction = new AbstractAction("Zero XY", Icons.zero) {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            zero(true, false, false);
        }
    };

    @SuppressWarnings("serial")
    public Action zZeroAction = new AbstractAction("Zero Z", Icons.zero) {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            zero(false, true, false);
        }
    };

    @SuppressWarnings("serial")
    public Action cZeroAction = new AbstractAction("Zero C", Icons.zero) {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            zero(false, false, true);
        }
    };

    @SuppressWarnings("serial")
    public Action safezAction = new AbstractAction("Head Safe Z") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                Configuration.get().getMachine().getDefaultHead().moveToSafeZ();
            });
        }
    };

    @SuppressWarnings("serial")
    public Action discardAction = new AbstractAction("Discard") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                Nozzle nozzle = machineControlsPanel.getSelectedNozzle();
                // move to the discard location
                MovableUtils.moveToLocationAtSafeZ(nozzle,
                        Configuration.get().getMachine().getDiscardLocation());
                // discard the part
                nozzle.place();
                nozzle.moveToSafeZ();
            });
        }
    };

    @SuppressWarnings("serial")
    public Action raiseIncrementAction = new AbstractAction("Raise Jog Increment") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            sliderIncrements.setValue(
                    Math.min(sliderIncrements.getMaximum(), sliderIncrements.getValue() + 1));
        }
    };

    @SuppressWarnings("serial")
    public Action lowerIncrementAction = new AbstractAction("Lower Jog Increment") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            sliderIncrements.setValue(
                    Math.max(sliderIncrements.getMinimum(), sliderIncrements.getValue() - 1));
        }
    };

    private ConfigurationListener configurationListener = new ConfigurationListener.Adapter() {
        @Override
        public void configurationComplete(Configuration configuration) throws Exception {
            setUnits(configuration.getSystemUnits());
            speedSlider.setValue((int) (configuration.getMachine().getSpeed() * 100));

            panelActuators.removeAll();

            Machine machine = Configuration.get().getMachine();

            for (Actuator actuator : machine.getActuators()) {
                final Actuator actuator_f = actuator;
                final JToggleButton actuatorButton = new JToggleButton(actuator_f.getName());
                actuatorButton.setFocusable(false);
                actuatorButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        final boolean state = actuatorButton.isSelected();
                        UiUtils.submitUiMachineTask(() -> {
                            actuator_f.actuate(state);
                        });
                    }
                });
                panelActuators.add(actuatorButton);
            }
            for (final Head head : machine.getHeads()) {
                for (Actuator actuator : head.getActuators()) {
                    final Actuator actuator_f = actuator;
                    final JToggleButton actuatorButton =
                            new JToggleButton(head.getName() + ":" + actuator_f.getName());
                    actuatorButton.setFocusable(false);
                    actuatorButton.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            final boolean state = actuatorButton.isSelected();
                            UiUtils.submitUiMachineTask(() -> {
                                actuator_f.actuate(state);
                            });
                        }
                    });
                    panelActuators.add(actuatorButton);
                }
                for (final PasteDispenser dispenser : head.getPasteDispensers()) {
                    final JButton dispenserButton =
                            new JButton(head.getName() + ":" + dispenser.getName());
                    dispenserButton.setFocusable(false);
                    dispenserButton.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            UiUtils.submitUiMachineTask(() -> {
                                dispenser.dispense(null, null, 250);
                            });
                        }
                    });
                    panelDispensers.add(dispenserButton);
                }
            }

            setEnabled(machineControlsPanel.isEnabled());
        }
    };
    private JSlider speedSlider;
}
