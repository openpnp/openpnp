package org.openpnp.machine.reference.vision.wizards;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.openpnp.Translations;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.VisionSettingsComboBoxModel;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.NamedListCellRenderer;
import org.openpnp.machine.reference.vision.ReferenceBottomVision;
import org.openpnp.model.BottomVisionSettings;
import org.openpnp.model.Configuration;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class ReferenceBottomVisionConfigurationWizard extends AbstractConfigurationWizard {
    private final ReferenceBottomVision bottomVision;
    private JCheckBox enabledCheckbox;
    private JCheckBox preRotCheckbox;
    private JTextField textFieldMaxVisionPasses;
    private JTextField textFieldMaxLinearOffset;
    private JTextField textFieldMaxAngularOffset;
    private JComboBox visionSettings;
    private boolean reloadWizard = false;

    
    public ReferenceBottomVisionConfigurationWizard(ReferenceBottomVision bottomVision) {
        this.bottomVision = bottomVision;

        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(null, Translations.getString(
                "ReferenceBottomVisionConfigurationWizard.GeneralPanel.Border.title"), //$NON-NLS-1$
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panel);
        panel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("right:default"),
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
                FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblEnabled = new JLabel(Translations.getString(
                "ReferenceBottomVisionConfigurationWizard.GeneralPanel.EnabledLabel.text")); //$NON-NLS-1$
        panel.add(lblEnabled, "2, 2");

        enabledCheckbox = new JCheckBox("");
        panel.add(enabledCheckbox, "4, 2");

        JLabel lblBottomVision = new JLabel(Translations.getString(
                "ReferenceBottomVisionConfigurationWizard.GeneralPanel.BottomVisionSettingsLabel.text")); //$NON-NLS-1$
        panel.add(lblBottomVision, "2, 4, right, default");
                
                visionSettings = new JComboBox(new VisionSettingsComboBoxModel(BottomVisionSettings.class));
                visionSettings.setMaximumRowCount(20);
                visionSettings.setRenderer(new NamedListCellRenderer<>());
                visionSettings.addItemListener(new ItemListener() {
                    public void itemStateChanged(ItemEvent e) {
                        reloadWizard = true;
                    }
                });
                panel.add(visionSettings, "4, 4, 3, 1, fill, default");

        JLabel lblPreRot = new JLabel(Translations.getString(
                "ReferenceBottomVisionConfigurationWizard.GeneralPanel.RotatePartsLabel.text")); //$NON-NLS-1$
        lblPreRot.setToolTipText(Translations.getString(
                "ReferenceBottomVisionConfigurationWizard.GeneralPanel.RotatePartsLabel.toolTipText")); //$NON-NLS-1$
        panel.add(lblPreRot, "2, 6");

        preRotCheckbox = new JCheckBox("");
        panel.add(preRotCheckbox, "4, 6");
        
        JLabel lblMaxVisionPasses = new JLabel(Translations.getString(
                "ReferenceBottomVisionConfigurationWizard.GeneralPanel.MaxVisionPassesLabel.text")); //$NON-NLS-1$
        lblMaxVisionPasses.setToolTipText(Translations.getString(
                "ReferenceBottomVisionConfigurationWizard.GeneralPanel.MaxVisionPassesLabel.toolTipText")); //$NON-NLS-1$
        panel.add(lblMaxVisionPasses, "2, 8, right, default");
        
        textFieldMaxVisionPasses = new JTextField();
        panel.add(textFieldMaxVisionPasses, "4, 8");
        textFieldMaxVisionPasses.setColumns(10);
        
        JLabel lblMaxLinearOffset = new JLabel(Translations.getString(
                "ReferenceBottomVisionConfigurationWizard.GeneralPanel.MaxLinearOffsetLabel.text")); //$NON-NLS-1$
        lblMaxLinearOffset.setToolTipText(Translations.getString(
                "ReferenceBottomVisionConfigurationWizard.GeneralPanel.MaxLinearOffsetLabel.toolTipText")); //$NON-NLS-1$
        panel.add(lblMaxLinearOffset, "2, 10, right, default");
        
        textFieldMaxLinearOffset = new JTextField();
        panel.add(textFieldMaxLinearOffset, "4, 10, fill, default");
        textFieldMaxLinearOffset.setColumns(10);
        
        JLabel lblMaxAngularOffset = new JLabel(Translations.getString(
                "ReferenceBottomVisionConfigurationWizard.GeneralPanel.MaxAngularOffsetLabel.text")); //$NON-NLS-1$
        lblMaxAngularOffset.setToolTipText(Translations.getString(
                "ReferenceBottomVisionConfigurationWizard.GeneralPanel.MaxAngularOffsetLabel.toolTipText")); //$NON-NLS-1$
        panel.add(lblMaxAngularOffset, "6, 10, right, default");
        
        textFieldMaxAngularOffset = new JTextField();
        panel.add(textFieldMaxAngularOffset, "8, 10, fill, default");
        textFieldMaxAngularOffset.setColumns(10);

        preRotCheckbox.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateEnabledState();
            }
        });
    }

    private void updateEnabledState() {
        boolean enabled = (preRotCheckbox.getModel().isSelected());
        /* No longer disable the config, as the enabled checkbox is only the system default
         * and pre-rotate can still be enabled on individual parts. Left the code for the moment
           as this might be reconsidered.
        textFieldMaxVisionPasses.setEnabled(enabled);
        textFieldMaxLinearOffset.setEnabled(enabled);
        textFieldMaxAngularOffset.setEnabled(enabled);
        */
    }

    @Override
    public String getWizardName() {
        return Translations.getString("ReferenceBottomVisionConfigurationWizard.wizardName"); //$NON-NLS-1$
    }

    @Override
    public void createBindings() {
        LengthConverter lengthConverter = new LengthConverter();
        IntegerConverter intConverter = new IntegerConverter();
        DoubleConverter doubleConverter = new DoubleConverter(Configuration.get()
                                                                           .getLengthDisplayFormat());

        addWrappedBinding(bottomVision, "bottomVisionSettings", visionSettings, "selectedItem");
        addWrappedBinding(bottomVision, "enabled", enabledCheckbox, "selected");
        addWrappedBinding(bottomVision, "preRotate", preRotCheckbox, "selected");

        addWrappedBinding(bottomVision, "maxVisionPasses", textFieldMaxVisionPasses, "text", intConverter);
        addWrappedBinding(bottomVision, "maxLinearOffset", textFieldMaxLinearOffset, "text", lengthConverter);
        addWrappedBinding(bottomVision, "maxAngularOffset", textFieldMaxAngularOffset, "text", doubleConverter);

        ComponentDecorators.decorateWithAutoSelect(textFieldMaxVisionPasses);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldMaxLinearOffset);
        ComponentDecorators.decorateWithAutoSelect(textFieldMaxAngularOffset);
        
        updateEnabledState();
    }

    @Override
    protected void saveToModel() {
        super.saveToModel();
        if (reloadWizard) {
            // Reselect the tree path to reload the wizard with potentially different property sheets. 
            MainFrame.get().getMachineSetupTab().selectCurrentTreePath();
        }
    }
}
