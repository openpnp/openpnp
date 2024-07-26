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

package org.openpnp.machine.reference.wizards;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.openpnp.Translations;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.machine.reference.ReferencePnpJobProcessor;
import org.openpnp.machine.reference.ReferencePnpJobProcessor.JobOrderHint;
import org.openpnp.spi.PnpJobPlanner.Strategy;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class ReferencePnpJobProcessorConfigurationWizard extends AbstractConfigurationWizard {
    private final ReferencePnpJobProcessor jobProcessor;
    private JComboBox<JobOrderHint> comboBoxJobOrder;
    private JComboBox<Strategy> comboBoxPlannerStrategy;
    private JTextField maxVisionRetriesTextField;
    private JCheckBox steppingToNextMotion;
    private JCheckBox optimizeMultipleNozzles;
    
    public ReferencePnpJobProcessorConfigurationWizard(ReferencePnpJobProcessor jobProcessor) {
        this.jobProcessor = jobProcessor;
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        JPanel panelGeneral = new JPanel();
        panelGeneral.setBorder(new TitledBorder(null, "General", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        contentPanel.add(panelGeneral);
        panelGeneral.setLayout(new FormLayout(new ColumnSpec[] {
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
                        FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblJobOrder = new JLabel(Translations.getString("MachineSetup.JobProcessors.ReferencePnpJobProcessor.Label.JobOrder")); //$NON-NLS-1$
        panelGeneral.add(lblJobOrder, "2, 2, right, default");

        comboBoxJobOrder = new JComboBox<JobOrderHint>(JobOrderHint.values());
        panelGeneral.add(comboBoxJobOrder, "4, 2");

        JLabel lblPlannerStrategy = new JLabel(Translations.getString("MachineSetup.JobProcessors.ReferencePnpJobProcessor.Label.PlannerStrategy")); //$NON-NLS-1$
        panelGeneral.add(lblPlannerStrategy, "2, 4, right, default");

        comboBoxPlannerStrategy = new JComboBox<Strategy>(Strategy.values());
        panelGeneral.add(comboBoxPlannerStrategy, "4, 4");

        JLabel lblMaxVisionRetries = new JLabel(Translations.getString("MachineSetup.JobProcessors.ReferencePnpJobProcessor.Label.MaxVisionRetries")); //$NON-NLS-1$
        panelGeneral.add(lblMaxVisionRetries, "2, 6, right, default");

        maxVisionRetriesTextField = new JTextField();
        panelGeneral.add(maxVisionRetriesTextField, "4, 6");
        maxVisionRetriesTextField.setColumns(10);

        JLabel lblStepsMotion = new JLabel(Translations.getString("ReferencePnpJobProcessorConfigurationWizard.lblStepsMotion.text")); //$NON-NLS-1$
        lblStepsMotion.setToolTipText(Translations.getString("ReferencePnpJobProcessorConfigurationWizard.lblStepsMotion.toolTipText")); //$NON-NLS-1$
        panelGeneral.add(lblStepsMotion, "2, 8, right, default");

        steppingToNextMotion = new JCheckBox(); 
        panelGeneral.add(steppingToNextMotion, "4, 8");

        JLabel lblOptimizeMultipleNozzles = new JLabel(Translations.getString("ReferencePnpJobProcessorConfigurationWizard.lblOptimizeMultipleNozzles.text")); //$NON-NLS-1$
        lblOptimizeMultipleNozzles.setToolTipText(Translations.getString("ReferencePnpJobProcessorConfigurationWizard.lblOptimizeMultipleNozzles.toolTipText")); //$NON-NLS-1$
        panelGeneral.add(lblOptimizeMultipleNozzles, "2, 10, right, default");

        optimizeMultipleNozzles = new JCheckBox(); 
        panelGeneral.add(optimizeMultipleNozzles, "4, 10");
    }

    @Override
    public void createBindings() {
        IntegerConverter intConverter = new IntegerConverter();

        addWrappedBinding(jobProcessor, "jobOrder", comboBoxJobOrder, "selectedItem");
        addWrappedBinding(jobProcessor.planner, "strategy", comboBoxPlannerStrategy, "selectedItem");
        addWrappedBinding(jobProcessor, "maxVisionRetries", maxVisionRetriesTextField, "text", intConverter);
        addWrappedBinding(jobProcessor, "steppingToNextMotion", steppingToNextMotion, "selected");
        addWrappedBinding(jobProcessor, "optimizeMultipleNozzles", optimizeMultipleNozzles, "selected");
        
        ComponentDecorators.decorateWithAutoSelect(maxVisionRetriesTextField);
    }
}
