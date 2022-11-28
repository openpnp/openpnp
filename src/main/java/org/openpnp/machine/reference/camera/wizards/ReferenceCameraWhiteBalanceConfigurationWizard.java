/*
 * Copyright (C) 2021 <mark@makr.zone>
 * inspired and based on work
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

package org.openpnp.machine.reference.camera.wizards;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.openpnp.Translations;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.components.SimpleGraphView;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.PercentIntegerConverter;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.SimpleGraph;
import org.openpnp.util.UiUtils;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class ReferenceCameraWhiteBalanceConfigurationWizard extends AbstractConfigurationWizard {
    private final ReferenceCamera referenceCamera;
    private JPanel panelColorBalance;

    public ReferenceCameraWhiteBalanceConfigurationWizard(ReferenceCamera referenceCamera) {
        this.referenceCamera = referenceCamera;

        panelColorBalance = new JPanel();
        panelColorBalance.setBorder(new TitledBorder(null, Translations.getString(
                "ReferenceCameraWhiteBalanceConfigurationWizard.ColorBalancePanel.Border.title"), //$NON-NLS-1$
                TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        contentPanel.add(panelColorBalance);
        panelColorBalance.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                RowSpec.decode("max(100dlu;default)"),
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));

        
        lblRedBalance = new JLabel(Translations.getString(
                "ReferenceCameraWhiteBalanceConfigurationWizard.ColorBalancePanel.RedBalanceLabel.text")); //$NON-NLS-1$
        panelColorBalance.add(lblRedBalance, "2, 2, right, default");
        
        redBalance = new JSlider();
        redBalance.setMajorTickSpacing(100);
        redBalance.setMaximum(200);
        redBalance.setPaintTicks(true);
        redBalance.setValue(100);
        panelColorBalance.add(redBalance, "4, 2, fill, default");
        
        lblGreenBalance = new JLabel(Translations.getString(
                "ReferenceCameraWhiteBalanceConfigurationWizard.ColorBalancePanel.GreenBalanceLabel.text")); //$NON-NLS-1$
        panelColorBalance.add(lblGreenBalance, "2, 4, right, default");
        
        greenBalance = new JSlider();
        greenBalance.setPaintTicks(true);
        greenBalance.setValue(100);
        greenBalance.setMaximum(200);
        greenBalance.setMajorTickSpacing(100);
        panelColorBalance.add(greenBalance, "4, 4, fill, default");
        
        lblBlueBalance = new JLabel(Translations.getString(
                "ReferenceCameraWhiteBalanceConfigurationWizard.ColorBalancePanel.BlueBalanceLabel.text")); //$NON-NLS-1$
        panelColorBalance.add(lblBlueBalance, "2, 6, right, default");
        
        blueBalance = new JSlider();
        blueBalance.setValue(100);
        blueBalance.setMajorTickSpacing(100);
        blueBalance.setMaximum(200);
        blueBalance.setPaintTicks(true);
        panelColorBalance.add(blueBalance, "4, 6, fill, default");
        
        lblRedGamma = new JLabel(Translations.getString(
                "ReferenceCameraWhiteBalanceConfigurationWizard.ColorBalancePanel.RedGammaLabel.text")); //$NON-NLS-1$
        panelColorBalance.add(lblRedGamma, "2, 10, right, default");
        
        redGammaPercent = new JSlider();
        redGammaPercent.setMajorTickSpacing(100);
        redGammaPercent.setValue(100);
        redGammaPercent.setMaximum(200);
        redGammaPercent.setPaintTicks(true);
        panelColorBalance.add(redGammaPercent, "4, 10, fill, default");
        
        lblGreenGamma = new JLabel(Translations.getString(
                "ReferenceCameraWhiteBalanceConfigurationWizard.ColorBalancePanel.GreenGammaLabel.text")); //$NON-NLS-1$
        panelColorBalance.add(lblGreenGamma, "2, 12, right, default");
        
        greenGammaPercent = new JSlider();
        greenGammaPercent.setValue(100);
        greenGammaPercent.setPaintTicks(true);
        greenGammaPercent.setMaximum(200);
        greenGammaPercent.setMajorTickSpacing(100);
        panelColorBalance.add(greenGammaPercent, "4, 12, fill, default");
        
        lblBlueGamma = new JLabel(Translations.getString(
                "ReferenceCameraWhiteBalanceConfigurationWizard.ColorBalancePanel.BlueGammaLabel.text")); //$NON-NLS-1$
        panelColorBalance.add(lblBlueGamma, "2, 14, right, default");
        
        blueGammaPercent = new JSlider();
        blueGammaPercent.setValue(100);
        blueGammaPercent.setPaintTicks(true);
        blueGammaPercent.setMaximum(200);
        blueGammaPercent.setMajorTickSpacing(100);
        panelColorBalance.add(blueGammaPercent, "4, 14, fill, default");
        
        colorGraph = new SimpleGraphView();
        colorGraph.setPreferredSize(new Dimension(300, 300));
        panelColorBalance.add(colorGraph, "4, 18, 1, 15, left, fill");
        
        lblOutput = new JLabel(Translations.getString(
                "ReferenceCameraWhiteBalanceConfigurationWizard.ColorBalancePanel.OutputLevelLabel.text")); //$NON-NLS-1$
        panelColorBalance.add(lblOutput, "2, 18, 1, 15, right, default");
        
        lblAutoWhitebalance = new JLabel(Translations.getString(
                "ReferenceCameraWhiteBalanceConfigurationWizard.ColorBalancePanel.AutoWhiteBalanceLabel.text")); //$NON-NLS-1$
        panelColorBalance.add(lblAutoWhitebalance, "6, 18, center, default");
        
        btnAutowhitebalance = new JButton(autoWhiteBalanceAction);
        panelColorBalance.add(btnAutowhitebalance, "6, 20, fill, fill");
        
        btnAutowhitebalance_1 = new JButton(autoWhiteBalanceBrightAction);
        panelColorBalance.add(btnAutowhitebalance_1, "6, 22, default, fill");
        
        btnAutowhiteBalanceAdvanced = new JButton(autoWhiteBalanceRoughMapAction);
        panelColorBalance.add(btnAutowhiteBalanceAdvanced, "6, 26, default, fill");
        
        btnAutowhiteBalanceAdvanced_1 = new JButton(autoWhiteBalanceFineMapAction);
        panelColorBalance.add(btnAutowhiteBalanceAdvanced_1, "6, 28, default, fill");
        
        btnReset = new JButton(resetAction);
        panelColorBalance.add(btnReset, "6, 32");
        
        lblInputLevel = new JLabel(Translations.getString(
                "ReferenceCameraWhiteBalanceConfigurationWizard.ColorBalancePanel.InputLevelLabel.text")); //$NON-NLS-1$
        panelColorBalance.add(lblInputLevel, "4, 34, center, default");
        initDataBindings();
    }

    @Override
    public void createBindings() {
    }

    private final Action autoWhiteBalanceAction = new AbstractAction() {
        {
            putValue(NAME, Translations.getString(
                    "ReferenceCameraWhiteBalanceConfigurationWizard.Action.AutoWhiteBalance")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString(
                    "ReferenceCameraWhiteBalanceConfigurationWizard.Action.AutoWhiteBalance.Description")); //$NON-NLS-1$
        }

        public void actionPerformed(ActionEvent e) {
            UiUtils.submitUiMachineTask(() -> {
                referenceCamera.autoAdjustWhiteBalance(false);
                refreshUi();
            });
        }
    };

    private final Action autoWhiteBalanceBrightAction = new AbstractAction() {
        {
            putValue(NAME, Translations.getString(
                    "ReferenceCameraWhiteBalanceConfigurationWizard.Action.Brightest")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString(
                    "ReferenceCameraWhiteBalanceConfigurationWizard.Action.Brightest.Description")); //$NON-NLS-1$
        }

        public void actionPerformed(ActionEvent e) {
            UiUtils.submitUiMachineTask(() -> {
                referenceCamera.autoAdjustWhiteBalance(true);
                refreshUi();
            });
        }
    };

    private final Action autoWhiteBalanceRoughMapAction = new AbstractAction() {
        {
            putValue(NAME, Translations.getString(
                    "ReferenceCameraWhiteBalanceConfigurationWizard.Action.MappedRoughly")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString(
                    "ReferenceCameraWhiteBalanceConfigurationWizard.Action.MappedRoughly.Description")); //$NON-NLS-1$
        }

        public void actionPerformed(ActionEvent e) {
            UiUtils.submitUiMachineTask(() -> {
                referenceCamera.autoAdjustWhiteBalanceMapped(8);
                refreshUi();
            });
        }

    };

    private final Action autoWhiteBalanceFineMapAction = new AbstractAction() {
        {
            putValue(NAME, Translations.getString(
                    "ReferenceCameraWhiteBalanceConfigurationWizard.Action.MappedFinely")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString(
                    "ReferenceCameraWhiteBalanceConfigurationWizard.Action.MappedFinely.Description")); //$NON-NLS-1$
        }

        public void actionPerformed(ActionEvent e) {
            UiUtils.submitUiMachineTask(() -> {
                referenceCamera.autoAdjustWhiteBalanceMapped(32);
                refreshUi();
            });
        }
    };

    private final Action resetAction = new AbstractAction() {
        {
            putValue(NAME, Translations.getString(
                    "ReferenceCameraWhiteBalanceConfigurationWizard.Action.Reset")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString(
                    "ReferenceCameraWhiteBalanceConfigurationWizard.Action.Reset.Description")); //$NON-NLS-1$
        }

        public void actionPerformed(ActionEvent e) {
            UiUtils.submitUiMachineTask(() -> {
                referenceCamera.resetWhiteBalance();
                refreshUi();
            });
        }
    };

    private JLabel lblRedBalance;
    private JLabel lblGreenBalance;
    private JLabel lblBlueBalance;
    private JSlider redBalance;
    private JSlider greenBalance;
    private JSlider blueBalance;
    private JButton btnAutowhitebalance;
    private JButton btnReset;
    private JButton btnAutowhitebalance_1;
    private JLabel lblRedGamma;
    private JLabel lblGreenGamma;
    private JLabel lblBlueGamma;
    private JSlider redGammaPercent;
    private JSlider greenGammaPercent;
    private JSlider blueGammaPercent;
    private JButton btnAutowhiteBalanceAdvanced;
    private SimpleGraphView colorGraph;
    private JButton btnAutowhiteBalanceAdvanced_1;
    private JLabel lblOutput;
    private JLabel lblInputLevel;
    private JLabel lblAutoWhitebalance;

    protected void initDataBindings() {
        BeanProperty<ReferenceCamera, Double> referenceCameraBeanProperty = BeanProperty.create("redBalance");
        BeanProperty<JSlider, Integer> jSliderBeanProperty = BeanProperty.create("value");
        AutoBinding<ReferenceCamera, Double, JSlider, Integer> autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, referenceCamera, referenceCameraBeanProperty, redBalance, jSliderBeanProperty);
        autoBinding.setConverter(new PercentIntegerConverter());
        autoBinding.bind();
        //
        BeanProperty<ReferenceCamera, Double> referenceCameraBeanProperty_1 = BeanProperty.create("greenBalance");
        BeanProperty<JSlider, Integer> jSliderBeanProperty_1 = BeanProperty.create("value");
        AutoBinding<ReferenceCamera, Double, JSlider, Integer> autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, referenceCamera, referenceCameraBeanProperty_1, greenBalance, jSliderBeanProperty_1);
        autoBinding_1.setConverter(new PercentIntegerConverter());
        autoBinding_1.bind();
        //
        BeanProperty<ReferenceCamera, Double> referenceCameraBeanProperty_2 = BeanProperty.create("blueBalance");
        BeanProperty<JSlider, Integer> jSliderBeanProperty_2 = BeanProperty.create("value");
        AutoBinding<ReferenceCamera, Double, JSlider, Integer> autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, referenceCamera, referenceCameraBeanProperty_2, blueBalance, jSliderBeanProperty_2);
        autoBinding_2.setConverter(new PercentIntegerConverter());
        autoBinding_2.bind();
        //
        BeanProperty<ReferenceCamera, Double> referenceCameraBeanProperty_3 = BeanProperty.create("redGamma");
        BeanProperty<JSlider, Integer> jSliderBeanProperty_3 = BeanProperty.create("value");
        AutoBinding<ReferenceCamera, Double, JSlider, Integer> autoBinding_3 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, referenceCamera, referenceCameraBeanProperty_3, redGammaPercent, jSliderBeanProperty_3);
        autoBinding_3.setConverter(new PercentIntegerConverter());
        autoBinding_3.bind();
        //
        BeanProperty<ReferenceCamera, Double> referenceCameraBeanProperty_4 = BeanProperty.create("greenGamma");
        BeanProperty<JSlider, Integer> jSliderBeanProperty_4 = BeanProperty.create("value");
        AutoBinding<ReferenceCamera, Double, JSlider, Integer> autoBinding_4 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, referenceCamera, referenceCameraBeanProperty_4, greenGammaPercent, jSliderBeanProperty_4);
        autoBinding_4.setConverter(new PercentIntegerConverter());
        autoBinding_4.bind();
        //
        BeanProperty<ReferenceCamera, Double> referenceCameraBeanProperty_5 = BeanProperty.create("blueGamma");
        BeanProperty<JSlider, Integer> jSliderBeanProperty_5 = BeanProperty.create("value");
        AutoBinding<ReferenceCamera, Double, JSlider, Integer> autoBinding_5 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, referenceCamera, referenceCameraBeanProperty_5, blueGammaPercent, jSliderBeanProperty_5);
        autoBinding_5.setConverter(new PercentIntegerConverter());
        autoBinding_5.bind();
        //
        BeanProperty<ReferenceCamera, SimpleGraph> referenceCameraBeanProperty_6 = BeanProperty.create("colorBalanceGraph");
        BeanProperty<SimpleGraphView, SimpleGraph> simpleGraphViewBeanProperty = BeanProperty.create("graph");
        AutoBinding<ReferenceCamera, SimpleGraph, SimpleGraphView, SimpleGraph> autoBinding_6 = Bindings.createAutoBinding(UpdateStrategy.READ, referenceCamera, referenceCameraBeanProperty_6, colorGraph, simpleGraphViewBeanProperty);
        autoBinding_6.bind();
    }

    protected void refreshUi() {
        MovableUtils.fireTargetedUserAction(referenceCamera);
        SwingUtilities.invokeLater(() -> {
            CameraView cameraView = MainFrame.get().getCameraViews().getCameraView(referenceCamera);
            cameraView.setShowImageInfo(true);
        });
    }
}
