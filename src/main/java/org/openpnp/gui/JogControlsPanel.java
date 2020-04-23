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
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openpnp.ConfigurationListener;
import org.openpnp.Translations;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.WrapLayout;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Head;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;
import org.openpnp.util.BeanUtils;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.UiUtils;
import org.pmw.tinylog.Logger;

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
    private final Configuration configuration;
    private JPanel panelActuators;
    private JSlider sliderIncrements;
    private JCheckBox boardProtectionOverrideCheck;

    /**
     * Create the panel.
     */
    public JogControlsPanel(Configuration configuration,
            MachineControlsPanel machineControlsPanel) {
        this.machineControlsPanel = machineControlsPanel;
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
        xyParkAction.setEnabled(enabled);
        zParkAction.setEnabled(enabled);
        cParkAction.setEnabled(enabled);
        for (Component c : panelActuators.getComponents()) {
            c.setEnabled(enabled);
        }
    }

    private void setUnits(LengthUnit units) {
        if (units == LengthUnit.Millimeters) {
            Hashtable<Integer, JLabel> incrementsLabels = new Hashtable<>();
            incrementsLabels.put(1, new JLabel("0.01")); //$NON-NLS-1$
            incrementsLabels.put(2, new JLabel("0.1")); //$NON-NLS-1$
            incrementsLabels.put(3, new JLabel("1.0")); //$NON-NLS-1$
            incrementsLabels.put(4, new JLabel("10")); //$NON-NLS-1$
            incrementsLabels.put(5, new JLabel("100")); //$NON-NLS-1$
            sliderIncrements.setLabelTable(incrementsLabels);
        }
        else if (units == LengthUnit.Inches) {
            Hashtable<Integer, JLabel> incrementsLabels = new Hashtable<>();
            incrementsLabels.put(1, new JLabel("0.001")); //$NON-NLS-1$
            incrementsLabels.put(2, new JLabel("0.01")); //$NON-NLS-1$
            incrementsLabels.put(3, new JLabel("0.1")); //$NON-NLS-1$
            incrementsLabels.put(4, new JLabel("1.0")); //$NON-NLS-1$
            incrementsLabels.put(5, new JLabel("10.0")); //$NON-NLS-1$
            sliderIncrements.setLabelTable(incrementsLabels);
        }
        else {
            throw new Error("setUnits() not implemented for " + units); //$NON-NLS-1$
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
                    "getJogIncrement() not implemented for " + configuration.getSystemUnits()); //$NON-NLS-1$
        }
    }

    public boolean getBoardProtectionOverrideEnabled() {
        return boardProtectionOverrideCheck.isSelected();
    }

    private void jog(final int x, final int y, final int z, final int c) {
        UiUtils.submitUiMachineTask(() -> {
            HeadMountable tool = machineControlsPanel.getSelectedTool();
            jogTool(x, y, z, c, tool);
        });
    }

    public void jogTool(final int x, final int y, final int z, final int c, HeadMountable tool)
            throws Exception {
        Location l = tool.getLocation()
                         .convertToUnits(Configuration.get()
                                                      .getSystemUnits());
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

        Location targetLocation = new Location(l.getUnits(), xPos, yPos, zPos, cPos);
        if (!this.getBoardProtectionOverrideEnabled()) {
            /* check board location before movement */
            List<BoardLocation> boardLocations = machineControlsPanel.getJobPanel()
                                                                     .getJob()
                                                                     .getBoardLocations();
            for (BoardLocation boardLocation : boardLocations) {
                if (!boardLocation.isEnabled()) {
                    continue;
                }
                boolean safe = nozzleLocationIsSafe(boardLocation.getLocation(),
                        boardLocation.getBoard()
                                     .getDimensions(),
                        targetLocation, new Length(1.0, l.getUnits()));
                if (!safe) {
                    throw new Exception(
                            "Nozzle would crash into board: " + boardLocation.toString() + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
                            "To disable the board protection go to the \"Safety\" tab in the \"Machine Controls\" panel."); //$NON-NLS-1$
                }
            }
        }

        tool.moveTo(targetLocation);
    }

    private boolean nozzleLocationIsSafe(Location origin, Location dimension, Location nozzle,
            Length safeDistance) {
        double distance = safeDistance.convertToUnits(nozzle.getUnits())
                                      .getValue();
        Location originConverted = origin.convertToUnits(nozzle.getUnits());
        Location dimensionConverted = dimension.convertToUnits(dimension.getUnits());
        double boardUpperZ = originConverted.getZ();
        boolean containsXY = pointWithinRectangle(originConverted, dimensionConverted, nozzle);
        boolean containsZ = nozzle.getZ() <= (boardUpperZ + distance);
        return !(containsXY && containsZ);
    }

    private boolean pointWithinRectangle(Location origin, Location dimension, Location point) {
        double rotation = Math.toRadians(origin.getRotation());
        double ay = origin.getY() + Math.sin(rotation) * dimension.getX();
        double ax = origin.getX() + Math.cos(rotation) * dimension.getX();
        Location a = new Location(dimension.getUnits(), ax, ay, 0.0, 0.0);

        double cx = origin.getX() - Math.cos(Math.PI / 2 - rotation) * dimension.getY();
        double cy = origin.getY() + Math.sin(Math.PI / 2 - rotation) * dimension.getY();
        Location c = new Location(dimension.getUnits(), cx, cy, 0.0, 0.0);

        double bx = ax + cx - origin.getX();
        double by = ay + cy - origin.getY();
        Location b = new Location(dimension.getUnits(), bx, by, 0.0, 0.0);

        return pointWithinTriangle(origin, b, a, point) || pointWithinTriangle(origin, c, b, point);
    }

    private boolean pointWithinTriangle(Location p1, Location p2, Location p3, Location p) {
        double alpha = ((p2.getY() - p3.getY()) * (p.getX() - p3.getX())
                + (p3.getX() - p2.getX()) * (p.getY() - p3.getY()))
                / ((p2.getY() - p3.getY()) * (p1.getX() - p3.getX())
                        + (p3.getX() - p2.getX()) * (p1.getY() - p3.getY()));
        double beta = ((p3.getY() - p1.getY()) * (p.getX() - p3.getX())
                + (p1.getX() - p3.getX()) * (p.getY() - p3.getY()))
                / ((p2.getY() - p3.getY()) * (p1.getX() - p3.getX())
                        + (p3.getX() - p2.getX()) * (p1.getY() - p3.getY()));
        double gamma = 1.0 - alpha - beta;

        return (alpha > 0.0) && (beta > 0.0) && (gamma > 0.0);
    }

    private void createUi() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        setFocusTraversalPolicy(focusPolicy);
        setFocusTraversalPolicyProvider(true);

        JTabbedPane tabbedPane_1 = new JTabbedPane(JTabbedPane.TOP);
        add(tabbedPane_1);

        JPanel panelControls = new JPanel();
        tabbedPane_1.addTab("Jog", null, panelControls, null); //$NON-NLS-1$
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
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
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
        homeButton.setToolTipText(Translations.getString("JogControlsPanel.homeButton.toolTipText")); //$NON-NLS-1$ //$NON-NLS-1$
        panelControls.add(homeButton, "2, 2"); //$NON-NLS-1$

        JLabel lblXy = new JLabel("X/Y"); //$NON-NLS-1$
        lblXy.setFont(new Font("Lucida Grande", Font.PLAIN, 22)); //$NON-NLS-1$
        lblXy.setHorizontalAlignment(SwingConstants.CENTER);
        panelControls.add(lblXy, "8, 2, fill, default"); //$NON-NLS-1$

        JLabel lblZ = new JLabel("Z"); //$NON-NLS-1$
        lblZ.setHorizontalAlignment(SwingConstants.CENTER);
        lblZ.setFont(new Font("Lucida Grande", Font.PLAIN, 22)); //$NON-NLS-1$
        panelControls.add(lblZ, "14, 2"); //$NON-NLS-1$

        JLabel lblDistance = new JLabel("<html>" + Translations.getString("JogControlsPanel.Label.Distance") + "<br>[" + configuration.getSystemUnits().getShortName() + "/deg]</html>"); //$NON-NLS-1$
        lblDistance.setFont(new Font("Lucida Grande", Font.PLAIN, 10)); //$NON-NLS-1$
        panelControls.add(lblDistance, "18, 2, center, center"); //$NON-NLS-1$

        JLabel lblSpeed = new JLabel("<html>" + Translations.getString("JogControlsPanel.Label.Speed") + "<br>[%]</html>"); //$NON-NLS-1$
        lblSpeed.setFont(new Font("Lucida Grande", Font.PLAIN, 10)); //$NON-NLS-1$
        panelControls.add(lblSpeed, "20, 2, center, center"); //$NON-NLS-1$

        sliderIncrements = new JSlider();
        panelControls.add(sliderIncrements, "18, 3, 1, 10"); //$NON-NLS-1$
        sliderIncrements.setOrientation(SwingConstants.VERTICAL);
        sliderIncrements.setMajorTickSpacing(1);
        sliderIncrements.setValue(1);
        sliderIncrements.setSnapToTicks(true);
        sliderIncrements.setPaintLabels(true);
        sliderIncrements.setMinimum(1);
        sliderIncrements.setMaximum(5);

        JButton yPlusButton = new JButton(yPlusAction);
        yPlusButton.setHideActionText(true);
        panelControls.add(yPlusButton, "8, 4"); //$NON-NLS-1$

        JButton zUpButton = new JButton(zPlusAction);
        zUpButton.setHideActionText(true);
        panelControls.add(zUpButton, "14, 4"); //$NON-NLS-1$

        speedSlider = new JSlider();
        speedSlider.setValue(100);
        speedSlider.setPaintTicks(true);
        speedSlider.setMinorTickSpacing(1);
        speedSlider.setMajorTickSpacing(25);
        speedSlider.setSnapToTicks(true);
        speedSlider.setPaintLabels(true);
        speedSlider.setOrientation(SwingConstants.VERTICAL);
        panelControls.add(speedSlider, "20, 4, 1, 9"); //$NON-NLS-1$
        speedSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                Configuration.get()
                             .getMachine()
                             .setSpeed(speedSlider.getValue() * 0.01);
            }
        });

        JButton positionNozzleBtn = new JButton(machineControlsPanel.targetToolAction);
        positionNozzleBtn.setIcon(Icons.centerTool);
        positionNozzleBtn.setHideActionText(true);
        positionNozzleBtn.setToolTipText(Translations.getString("JogControlsPanel.Action.positionSelectedNozzle"));
        panelControls.add(positionNozzleBtn, "22, 4"); //$NON-NLS-1$

        JButton buttonStartStop = new JButton(machineControlsPanel.startStopMachineAction);
        buttonStartStop.setIcon(Icons.powerOn);
        panelControls.add(buttonStartStop, "2, 6"); //$NON-NLS-1$
        buttonStartStop.setHideActionText(true);

        JButton xMinusButton = new JButton(xMinusAction);
        xMinusButton.setHideActionText(true);
        panelControls.add(xMinusButton, "6, 6"); //$NON-NLS-1$

        JButton homeXyButton = new JButton(xyParkAction);
        homeXyButton.setHideActionText(true);
        panelControls.add(homeXyButton, "8, 6"); //$NON-NLS-1$

        JButton xPlusButton = new JButton(xPlusAction);
        xPlusButton.setHideActionText(true);
        panelControls.add(xPlusButton, "10, 6"); //$NON-NLS-1$

        JButton homeZButton = new JButton(zParkAction);
        homeZButton.setHideActionText(true);
        panelControls.add(homeZButton, "14, 6"); //$NON-NLS-1$

        JButton yMinusButton = new JButton(yMinusAction);
        yMinusButton.setHideActionText(true);
        panelControls.add(yMinusButton, "8, 8"); //$NON-NLS-1$

        JButton zDownButton = new JButton(zMinusAction);
        zDownButton.setHideActionText(true);
        panelControls.add(zDownButton, "14, 8"); //$NON-NLS-1$

        JButton positionCameraBtn = new JButton(machineControlsPanel.targetCameraAction);
        positionCameraBtn.setIcon(Icons.centerCamera);
        positionCameraBtn.setHideActionText(true);
        positionCameraBtn.setToolTipText(Translations.getString("JogControlsPanel.Action.positionCamera"));
        panelControls.add(positionCameraBtn, "22, 8"); //$NON-NLS-1$

        JLabel lblC = new JLabel("C"); //$NON-NLS-1$
        lblC.setHorizontalAlignment(SwingConstants.CENTER);
        lblC.setFont(new Font("Lucida Grande", Font.PLAIN, 22)); //$NON-NLS-1$
        panelControls.add(lblC, "4, 12"); //$NON-NLS-1$

        JButton counterclockwiseButton = new JButton(cPlusAction);
        counterclockwiseButton.setHideActionText(true);
        panelControls.add(counterclockwiseButton, "6, 12"); //$NON-NLS-1$

        JButton homeCButton = new JButton(cParkAction);
        homeCButton.setHideActionText(true);
        panelControls.add(homeCButton, "8, 12"); //$NON-NLS-1$

        JButton clockwiseButton = new JButton(cMinusAction);
        clockwiseButton.setHideActionText(true);
        panelControls.add(clockwiseButton, "10, 12"); //$NON-NLS-1$

        JPanel panelSpecial = new JPanel();
        tabbedPane_1.addTab(Translations.getString("JogControlsPanel.Tab.Special"), null, panelSpecial, null); //$NON-NLS-1$
        FlowLayout flowLayout_1 = (FlowLayout) panelSpecial.getLayout();
        flowLayout_1.setAlignment(FlowLayout.LEFT);

        JButton btnSafeZ = new JButton(safezAction);
        panelSpecial.add(btnSafeZ);

        JButton btnDiscard = new JButton(discardAction);
        panelSpecial.add(btnDiscard);

        panelActuators = new JPanel();
        tabbedPane_1.addTab(Translations.getString("JogControlsPanel.Tab.Actuators"), null, panelActuators, null); //$NON-NLS-1$
        panelActuators.setLayout(new WrapLayout(WrapLayout.LEFT));

        JPanel panelSafety = new JPanel();
        tabbedPane_1.addTab(Translations.getString("JogControlsPanel.Tab.Safety"), null, panelSafety, null); //$NON-NLS-1$
        panelSafety.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));

        boardProtectionOverrideCheck = new JCheckBox(Translations.getString("JogControlsPanel.Label.OverrideBoardProtection")); //$NON-NLS-1$
        boardProtectionOverrideCheck.setToolTipText(Translations.getString("JogControlsPanel.Label.OverrideBoardProtection.Description")); //$NON-NLS-1$
        panelSafety.add(boardProtectionOverrideCheck, "1, 1"); //$NON-NLS-1$
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
    public Action yPlusAction = new AbstractAction("Y+", Icons.arrowUp) { //$NON-NLS-1$
        @Override
        public void actionPerformed(ActionEvent arg0) {
            jog(0, 1, 0, 0);
        }
    };

    @SuppressWarnings("serial")
    public Action yMinusAction = new AbstractAction("Y-", Icons.arrowDown) { //$NON-NLS-1$
        @Override
        public void actionPerformed(ActionEvent arg0) {
            jog(0, -1, 0, 0);
        }
    };

    @SuppressWarnings("serial")
    public Action xPlusAction = new AbstractAction("X+", Icons.arrowRight) { //$NON-NLS-1$
        @Override
        public void actionPerformed(ActionEvent arg0) {
            jog(1, 0, 0, 0);
        }
    };

    @SuppressWarnings("serial")
    public Action xMinusAction = new AbstractAction("X-", Icons.arrowLeft) { //$NON-NLS-1$
        @Override
        public void actionPerformed(ActionEvent arg0) {
            jog(-1, 0, 0, 0);
        }
    };

    @SuppressWarnings("serial")
    public Action zPlusAction = new AbstractAction("Z+", Icons.arrowUp) { //$NON-NLS-1$
        @Override
        public void actionPerformed(ActionEvent arg0) {
            jog(0, 0, 1, 0);
        }
    };

    @SuppressWarnings("serial")
    public Action zMinusAction = new AbstractAction("Z-", Icons.arrowDown) { //$NON-NLS-1$
        @Override
        public void actionPerformed(ActionEvent arg0) {
            jog(0, 0, -1, 0);
        }
    };

    @SuppressWarnings("serial")
    public Action cPlusAction = new AbstractAction("C+", Icons.rotateCounterclockwise) { //$NON-NLS-1$
        @Override
        public void actionPerformed(ActionEvent arg0) {
            jog(0, 0, 0, 1);
        }
    };

    @SuppressWarnings("serial")
    public Action cMinusAction = new AbstractAction("C-", Icons.rotateClockwise) { //$NON-NLS-1$
        @Override
        public void actionPerformed(ActionEvent arg0) {
            jog(0, 0, 0, -1);
        }
    };

    @SuppressWarnings("serial")
    public Action xyParkAction = new AbstractAction(Translations.getString("JogControlsPanel.Action.ParkXY"), Icons.park) { //$NON-NLS-1$
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                MovableUtils.park(machineControlsPanel.getSelectedTool().getHead());
            });
        }
    };

    @SuppressWarnings("serial")
    public Action zParkAction = new AbstractAction(Translations.getString("JogControlsPanel.Action.ParkZ"), Icons.park) { //$NON-NLS-1$
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                HeadMountable hm = machineControlsPanel.getSelectedTool();
                hm.moveToSafeZ();
            });
        }
    };

    @SuppressWarnings("serial")
    public Action cParkAction = new AbstractAction(Translations.getString("JogControlsPanel.Action.ParkC"), Icons.park) { //$NON-NLS-1$
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                HeadMountable hm = machineControlsPanel.getSelectedTool();
                Location location = hm.getLocation();
                location = location.derive(null, null, null, 0.);
                hm.moveTo(location);
            });
        }
    };

    @SuppressWarnings("serial")
    public Action safezAction = new AbstractAction(Translations.getString("JogControlsPanel.Action.HeadSafeZ")) { //$NON-NLS-1$
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                Configuration.get()
                             .getMachine()
                             .getDefaultHead()
                             .moveToSafeZ();
            });
        }
    };

    @SuppressWarnings("serial")
    public Action discardAction = new AbstractAction(Translations.getString("JogControlsPanel.Action.Discard")) { //$NON-NLS-1$
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                Nozzle nozzle = machineControlsPanel.getSelectedNozzle();
                // move to the discard location
                try {
                    Map<String, Object> globals = new HashMap<>();
                    globals.put("nozzle", nozzle);
                    Configuration.get().getScripting().on("Job.BeforeDiscard", globals);
                }
                catch (Exception e) {
                    Logger.warn(e);
                }
                MovableUtils.moveToLocationAtSafeZ(nozzle, Configuration.get()
                                                                        .getMachine()
                                                                        .getDiscardLocation());
                // discard the part
                nozzle.place();
                nozzle.moveToSafeZ();
                try {
                    Map<String, Object> globals = new HashMap<>();
                    globals.put("nozzle", nozzle);
                    Configuration.get().getScripting().on("Job.AfterDiscard", globals);
                }
                catch (Exception e) {
                    Logger.warn(e);
                }
            });
        }
    };

    @SuppressWarnings("serial")
    public Action raiseIncrementAction = new AbstractAction(Translations.getString("JogControlsPanel.Action.RaiseJogIncrement")) { //$NON-NLS-1$
        @Override
        public void actionPerformed(ActionEvent arg0) {
            sliderIncrements.setValue(
                    Math.min(sliderIncrements.getMaximum(), sliderIncrements.getValue() + 1));
        }
    };

    @SuppressWarnings("serial")
    public Action lowerIncrementAction = new AbstractAction(Translations.getString("JogControlsPanel.Action.LowerJogIncrement")) { //$NON-NLS-1$
        @Override
        public void actionPerformed(ActionEvent arg0) {
            sliderIncrements.setValue(
                    Math.max(sliderIncrements.getMinimum(), sliderIncrements.getValue() - 1));
        }
    };
	
    @SuppressWarnings("serial")
    public Action setIncrement1Action = new AbstractAction(Translations.getString("JogControlsPanel.Action.FirstJogIncrement")) { //$NON-NLS-1$
        @Override
        public void actionPerformed(ActionEvent arg0) {
            sliderIncrements.setValue(1);
		}
    };
    @SuppressWarnings("serial")
    public Action setIncrement2Action = new AbstractAction(Translations.getString("JogControlsPanel.Action.SecondJogIncrement")) { //$NON-NLS-1$
        @Override
        public void actionPerformed(ActionEvent arg0) {
            sliderIncrements.setValue(2);
		}
    };
    @SuppressWarnings("serial")
    public Action setIncrement3Action = new AbstractAction(Translations.getString("JogControlsPanel.Action.ThirdJogIncrement")) { //$NON-NLS-1$
        @Override
        public void actionPerformed(ActionEvent arg0) {
            sliderIncrements.setValue(3);
		}
    };
    @SuppressWarnings("serial")
    public Action setIncrement4Action = new AbstractAction(Translations.getString("JogControlsPanel.Action.FourthJogIncrement")) { //$NON-NLS-1$
        @Override
        public void actionPerformed(ActionEvent arg0) {
            sliderIncrements.setValue(4);
		}
    };
    @SuppressWarnings("serial")
    public Action setIncrement5Action = new AbstractAction(Translations.getString("JogControlsPanel.Action.FifthJogIncrement")) { //$NON-NLS-1$
        @Override
        public void actionPerformed(ActionEvent arg0) {
            sliderIncrements.setValue(5);
		}
    };


    private void addActuator(Actuator actuator) {
        String name = actuator.getHead() == null ? actuator.getName() : actuator.getHead()
                                                                                .getName()
                + ":" + actuator.getName(); //$NON-NLS-1$
        JButton actuatorButton = new JButton(name);
        actuatorButton.addActionListener((e) -> {
            ActuatorControlDialog dlg = new ActuatorControlDialog(actuator);
            dlg.pack();
            dlg.revalidate();
            dlg.setLocationRelativeTo(JogControlsPanel.this);
            dlg.setVisible(true);
        });
        BeanUtils.addPropertyChangeListener(actuator, "name", e -> { //$NON-NLS-1$
            actuatorButton.setText(
                    actuator.getHead() == null ? actuator.getName() : actuator.getHead()
                                                                              .getName()
                            + ":" + actuator.getName()); //$NON-NLS-1$
        });
        panelActuators.add(actuatorButton);
        actuatorButtons.put(actuator, actuatorButton);
    }

    private void removeActuator(Actuator actuator) {
        panelActuators.remove(actuatorButtons.remove(actuator));
    }

    private ConfigurationListener configurationListener = new ConfigurationListener.Adapter() {
        @Override
        public void configurationComplete(Configuration configuration) throws Exception {
            setUnits(configuration.getSystemUnits());
            speedSlider.setValue((int) (configuration.getMachine()
                                                     .getSpeed()
                    * 100));

            panelActuators.removeAll();

            Machine machine = Configuration.get()
                                           .getMachine();

            for (Actuator actuator : machine.getActuators()) {
                addActuator(actuator);
            }
            for (final Head head : machine.getHeads()) {
                for (Actuator actuator : head.getActuators()) {
                    addActuator(actuator);
                }
            }


            PropertyChangeListener listener = (e) -> {
                if (e.getOldValue() == null && e.getNewValue() != null) {
                    Actuator actuator = (Actuator) e.getNewValue();
                    addActuator(actuator);
                }
                else if (e.getOldValue() != null && e.getNewValue() == null) {
                    removeActuator((Actuator) e.getOldValue());
                }
            };

            BeanUtils.addPropertyChangeListener(machine, "actuators", listener); //$NON-NLS-1$
            for (Head head : machine.getHeads()) {
                BeanUtils.addPropertyChangeListener(head, "actuators", listener); //$NON-NLS-1$
            }


            setEnabled(machineControlsPanel.isEnabled());
        }
    };

    private Map<Actuator, JButton> actuatorButtons = new HashMap<>();
    private JSlider speedSlider;
}
