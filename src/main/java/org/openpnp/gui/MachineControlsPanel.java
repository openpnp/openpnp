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

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Locale;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.NozzleItem;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MachineListener;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PasteDispenser;
import org.openpnp.util.BeanUtils;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.UiUtils;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class MachineControlsPanel extends JPanel {
    private final Configuration configuration;

    private Nozzle selectedNozzle;
    private JComboBox comboBoxNozzles;

    private JogControlsPanel jogControlsPanel;

    private Location markLocation = null;
    
    private Color droNormalColor = new Color(0xBDFFBE);
    private Color droSavedColor = new Color(0x90cce0);

    /**
     * Create the panel.
     */
    public MachineControlsPanel(Configuration configuration) {
        this.configuration = configuration;

        jogControlsPanel = new JogControlsPanel(configuration, this);

        createUi();

        configuration.addListener(configurationListener);
    }

    public void setSelectedNozzle(Nozzle nozzle) {
        selectedNozzle = nozzle;
        comboBoxNozzles.setSelectedItem(selectedNozzle);
        updateDros();
    }

    public Nozzle getSelectedNozzle() {
        return selectedNozzle;
    }

    public PasteDispenser getSelectedPasteDispenser() {
        try {
            // TODO: We don't actually have a way to select a dispenser yet, so
            // until we do we just return the first one.
            return Configuration.get().getMachine().getDefaultHead().getDefaultPasteDispenser();
        }
        catch (Exception e) {
            return null;
        }
    }

    /**
     * Currently returns the selected Nozzle. Intended to eventually return either the selected
     * Nozzle or PasteDispenser.
     * @return
     */
    public HeadMountable getSelectedTool() {
        return getSelectedNozzle();
    }

    public JogControlsPanel getJogControlsPanel() {
        return jogControlsPanel;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        homeAction.setEnabled(enabled);
        jogControlsPanel.setEnabled(enabled);
        targetCameraAction.setEnabled(enabled);
        targetToolAction.setEnabled(enabled);
    }

    public Location getCurrentLocation() {
        if (selectedNozzle == null) {
            return null;
        }

        Location l = selectedNozzle.getLocation();
        l = l.convertToUnits(configuration.getSystemUnits());

        return l;
    }

    public void updateDros() {
        Location l = getCurrentLocation();
        if (l == null) {
            return;
        }
        
        if (markLocation != null) {
            l = l.subtract(markLocation);
        }

        double x, y, z, c;

        x = l.getX();
        y = l.getY();
        z = l.getZ();
        c = l.getRotation();

        MainFrame.get().getDroLabel().setText(String.format("X:%-9s Y:%-9s Z:%-9s C:%-9s",
                String.format(Locale.US, configuration.getLengthDisplayFormat(), x),
                String.format(Locale.US, configuration.getLengthDisplayFormat(), y),
                String.format(Locale.US, configuration.getLengthDisplayFormat(), z),
                String.format(Locale.US, configuration.getLengthDisplayFormat(), c)));
    }

    private void createUi() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JPanel panel = new JPanel();
        add(panel);
        panel.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC,
                        ColumnSpec.decode("default:grow"),},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        comboBoxNozzles = new JComboBox();
        comboBoxNozzles.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setSelectedNozzle(((NozzleItem) comboBoxNozzles.getSelectedItem()).getNozzle());
            }
        });
        panel.add(comboBoxNozzles, "2, 2, fill, default");

        add(jogControlsPanel);
    }

    @SuppressWarnings("serial")
    public Action startStopMachineAction = new AbstractAction("Stop", Icons.powerOn) {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            setEnabled(false);
            new Thread(() -> {
                Machine machine = Configuration.get().getMachine();
                boolean enable = !machine.isEnabled();
                try {
                    Configuration.get().getMachine().setEnabled(enable);
                    setEnabled(true);
                }
                catch (Exception t) {
                    MessageBoxes.errorBox(MachineControlsPanel.this, "Enable Failure",
                            t.getMessage());
                    setEnabled(true);
                }
            }).start();
        }
    };

    @SuppressWarnings("serial")
    public Action homeAction = new AbstractAction("Home", Icons.home) {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                selectedNozzle.getHead().home();
            });
        }
    };

    @SuppressWarnings("serial")
    public Action targetToolAction = new AbstractAction(null, Icons.centerTool) {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                HeadMountable tool = getSelectedTool();
                Camera camera = tool.getHead().getDefaultCamera();
                MovableUtils.moveToLocationAtSafeZ(tool, camera.getLocation());
            });
        }
    };

    @SuppressWarnings("serial")
    public Action targetCameraAction = new AbstractAction(null, Icons.centerCamera) {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                HeadMountable tool = getSelectedTool();
                Camera camera = tool.getHead().getDefaultCamera();
                MovableUtils.moveToLocationAtSafeZ(camera, tool.getLocation());
            });
        }
    };

    private void updateStartStopButton(boolean enabled) {
        startStopMachineAction.putValue(Action.NAME, enabled ? "Stop" : "Start");
        startStopMachineAction.putValue(Action.SMALL_ICON,
                enabled ? Icons.powerOff : Icons.powerOn);
    }

    private MachineListener machineListener = new MachineListener.Adapter() {
        @Override
        public void machineHeadActivity(Machine machine, Head head) {
            EventQueue.invokeLater(() -> updateDros());
        }

        @Override
        public void machineEnabled(Machine machine) {
            updateStartStopButton(machine.isEnabled());
            setEnabled(true);
            EventQueue.invokeLater(() -> updateDros());
        }

        @Override
        public void machineEnableFailed(Machine machine, String reason) {
            updateStartStopButton(machine.isEnabled());
        }

        @Override
        public void machineDisabled(Machine machine, String reason) {
            updateStartStopButton(machine.isEnabled());
            setEnabled(false);
        }

        @Override
        public void machineDisableFailed(Machine machine, String reason) {
            updateStartStopButton(machine.isEnabled());
        }
    };

    private ConfigurationListener configurationListener = new ConfigurationListener.Adapter() {
        @Override
        public void configurationComplete(Configuration configuration) {
            SwingUtilities.invokeLater(() -> {
                MainFrame.get().getDroLabel().setBackground(droNormalColor);
            });
            MainFrame.get().getDroLabel().addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    SwingUtilities.invokeLater(() -> {
                        if (markLocation == null) {
                            markLocation = getCurrentLocation();
                            MainFrame.get().getDroLabel().setBackground(droSavedColor);
                        }
                        else {
                            markLocation = null;
                            MainFrame.get().getDroLabel().setBackground(droNormalColor);
                        }
                        updateDros();
                    });
                }
            });
            
            Machine machine = configuration.getMachine();
            if (machine != null) {
                machine.removeListener(machineListener);
            }

            for (Head head : machine.getHeads()) {
                for (Nozzle nozzle : head.getNozzles()) {
                    comboBoxNozzles.addItem(new NozzleItem(nozzle));
                }
            }
            setSelectedNozzle(((NozzleItem) comboBoxNozzles.getItemAt(0)).getNozzle());

            machine.addListener(machineListener);

            updateStartStopButton(machine.isEnabled());

            setEnabled(machine.isEnabled());
            
            for (Head head : machine.getHeads()) {
                BeanUtils.addPropertyChangeListener(head, "nozzles", (e) -> {
                    if (e.getOldValue() == null && e.getNewValue() != null) {
                        Nozzle nozzle = (Nozzle) e.getNewValue();
                        comboBoxNozzles.addItem(new NozzleItem(nozzle));
                    }
                    else if (e.getOldValue() != null && e.getNewValue() == null) {
                        for (int i = 0; i < comboBoxNozzles.getItemCount(); i++) {
                            NozzleItem item = (NozzleItem) comboBoxNozzles.getItemAt(i);
                            if (item.getNozzle() == e.getOldValue()) {
                                comboBoxNozzles.removeItemAt(i);
                            }
                        }
                    }
                });
            }
            
        }
    };
}
