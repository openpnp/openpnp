package org.openpnp.machine.reference.driver.wizards;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.openpnp.Translations;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.machine.reference.driver.GcodeAsyncDriver;
import org.openpnp.machine.reference.driver.GcodeDriver;
import org.openpnp.model.Configuration;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import java.awt.Component;
import javax.swing.Box;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;

public class GcodeAsyncDriverSettings extends AbstractConfigurationWizard {
    private final GcodeDriver driver;
    private JCheckBox confirmationFlowControl;
    private JTextField interpolationTimeStep;
    private JTextField interpolationMinStep;
    private JTextField interpolationMaxSteps;
    private JTextField junctionDeviation;
    private JTextField interpolationJerkSteps;
    private JCheckBox reportedLocationConfirmation;

    public GcodeAsyncDriverSettings(GcodeAsyncDriver driver) {
        this.driver = driver;

        JPanel settingsPanel = new JPanel();
        settingsPanel.setBorder(new TitledBorder(null,
                Translations.getString("GcodeAsyncDriverSettings.SettingsPanel.Border.title"), //$NON-NLS-1$ 
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(settingsPanel);
        settingsPanel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
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
                FormSpecs.DEFAULT_ROWSPEC,}));
        JPanel interpolationPanel = new JPanel();
        interpolationPanel.setBorder(new TitledBorder(null, 
                Translations.getString("GcodeAsyncDriverSettings.InterpolationPanel.Border.title"), //$NON-NLS-1$ 
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(interpolationPanel);
        interpolationPanel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
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
                RowSpec.decode("fill:default:grow"),}));

        JLabel lblMaximumNumberOf = new JLabel(Translations.getString("GcodeAsyncDriverSettings.InterpolationPanel.MaximumNumberofStepsLabel.text")); //$NON-NLS-1$
        lblMaximumNumberOf.setToolTipText(Translations.getString("GcodeAsyncDriverSettings.InterpolationPanel.MaximumNumberofStepsLabel.toolTipText")); //$NON-NLS-1$
        interpolationPanel.add(lblMaximumNumberOf, "2, 2, right, default");

        interpolationMaxSteps = new JTextField();
        interpolationPanel.add(interpolationMaxSteps, "4, 2");
        interpolationMaxSteps.setColumns(10);
        
        JLabel lblMaxumNumberOf = new JLabel(Translations.getString("GcodeAsyncDriverSettings.InterpolationPanel.MaximumNumberofJerkStepsLabel.text")); //$NON-NLS-1$
        lblMaxumNumberOf.setToolTipText(Translations.getString("GcodeAsyncDriverSettings.InterpolationPanel.MaximumNumberofJerkStepsLabel.toolTipText")); //$NON-NLS-1$
        interpolationPanel.add(lblMaxumNumberOf, "2, 4, right, default");
        
        interpolationJerkSteps = new JTextField();
        interpolationPanel.add(interpolationJerkSteps, "4, 4, fill, default");
        interpolationJerkSteps.setColumns(10);

        JLabel lblInterpolationTimeStep = new JLabel(Translations.getString("GcodeAsyncDriverSettings.InterpolationPanel.MinimumStepTimeLabel.text")); //$NON-NLS-1$
        interpolationPanel.add(lblInterpolationTimeStep, "2, 6, right, default");
        lblInterpolationTimeStep.setToolTipText(Translations.getString("GcodeAsyncDriverSettings.InterpolationPanel.MinimumStepTimeLabel.toolTipText")); //$NON-NLS-1$

        interpolationTimeStep = new JTextField();
        interpolationPanel.add(interpolationTimeStep, "4, 6");
        interpolationTimeStep.setColumns(10);

        JLabel lblInterpolationMinimumTicks = new JLabel(Translations.getString("GcodeAsyncDriverSettings.InterpolationPanel.MinimumAxisResolutionTicksLabel.text")); //$NON-NLS-1$
        interpolationPanel.add(lblInterpolationMinimumTicks, "2, 8, right, default");
        lblInterpolationMinimumTicks.setToolTipText(Translations.getString("GcodeAsyncDriverSettings.InterpolationPanel.MinimumAxisResolutionTicksLabel.toolTipText")); //$NON-NLS-1$

        interpolationMinStep = new JTextField();
        interpolationPanel.add(interpolationMinStep, "4, 8");
        interpolationMinStep.setColumns(10);
        
        JLabel lblJunctionDeviation = new JLabel(Translations.getString("GcodeAsyncDriverSettings.InterpolationPanel.MaximumJunctionDeviationLabel.text")); //$NON-NLS-1$
        lblJunctionDeviation.setToolTipText(Translations.getString("GcodeAsyncDriverSettings.InterpolationPanel.MaximumJunctionDeviationLabel.toolTipText")); //$NON-NLS-1$
        interpolationPanel.add(lblJunctionDeviation, "2, 10, right, default");
        
        junctionDeviation = new JTextField();
        interpolationPanel.add(junctionDeviation, "4, 10, fill, default");
        junctionDeviation.setColumns(10);

        JLabel lblConfirmationFlowControl = new JLabel(Translations.getString("GcodeAsyncDriverSettings.SettingsPanel.ConfirmationFlowControlLabel.text")); //$NON-NLS-1$
        lblConfirmationFlowControl.setToolTipText(Translations.getString("GcodeAsyncDriverSettings.SettingsPanel.ConfirmationFlowControlLabel.toolTipText")); //$NON-NLS-1$
        settingsPanel.add(lblConfirmationFlowControl, "2, 2, right, default");

        confirmationFlowControl = new JCheckBox("");
        confirmationFlowControl.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (!confirmationFlowControl.isSelected()) {
                    reportedLocationConfirmation.setSelected(true);
                }
            }
        });
        settingsPanel.add(confirmationFlowControl, "4, 2");

        JLabel lblRequestLocation = new JLabel(Translations.getString("GcodeAsyncDriverSettings.SettingsPanel.LocationConfirmationLabel.text")); //$NON-NLS-1$
        lblRequestLocation.setToolTipText(Translations.getString("GcodeAsyncDriverSettings.SettingsPanel.LocationConfirmationLabel.toolTipText")); //$NON-NLS-1$
        settingsPanel.add(lblRequestLocation, "2, 4, right, default");

        reportedLocationConfirmation = new JCheckBox("");
        reportedLocationConfirmation.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (!reportedLocationConfirmation.isSelected()) {
                    confirmationFlowControl.setSelected(true);
                }
            }
        });
        settingsPanel.add(reportedLocationConfirmation, "4, 4");

    }

    @Override
    public void createBindings() {
        IntegerConverter intConverter = new IntegerConverter();
        //DoubleConverter doubleConverter = new DoubleConverter(Configuration.get().getLengthDisplayFormat());
        DoubleConverter doubleConverterFine = new DoubleConverter("%.6f");
        LengthConverter lengthConverter = new LengthConverter();

        addWrappedBinding(driver, "confirmationFlowControl", confirmationFlowControl, "selected");
        addWrappedBinding(driver, "reportedLocationConfirmation", reportedLocationConfirmation, "selected");
        addWrappedBinding(driver, "interpolationMaxSteps", interpolationMaxSteps, "text", intConverter);
        addWrappedBinding(driver, "interpolationJerkSteps", interpolationJerkSteps, "text", intConverter);
        addWrappedBinding(driver, "interpolationTimeStep", interpolationTimeStep, "text", doubleConverterFine);
        addWrappedBinding(driver, "interpolationMinStep", interpolationMinStep, "text", intConverter);
        addWrappedBinding(driver, "junctionDeviation", junctionDeviation, "text", lengthConverter);

        ComponentDecorators.decorateWithAutoSelect(interpolationMaxSteps);
        ComponentDecorators.decorateWithAutoSelect(interpolationJerkSteps);
        ComponentDecorators.decorateWithAutoSelect(interpolationTimeStep);
        ComponentDecorators.decorateWithAutoSelect(interpolationMinStep);
        ComponentDecorators.decorateWithAutoSelect(junctionDeviation);
    }
}
