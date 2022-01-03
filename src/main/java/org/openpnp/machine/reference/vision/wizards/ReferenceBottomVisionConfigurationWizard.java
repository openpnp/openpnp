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
    private JTextField maxSearchDistance;
    private JComboBox visionSettings;
    private boolean reloadWizard = false;

    
    public ReferenceBottomVisionConfigurationWizard(ReferenceBottomVision bottomVision) {
        this.bottomVision = bottomVision;

        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(null, "General", TitledBorder.LEADING, TitledBorder.TOP,
                null, null));
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
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblEnabled = new JLabel("Enabled?");
        panel.add(lblEnabled, "2, 2");

        enabledCheckbox = new JCheckBox("");
        panel.add(enabledCheckbox, "4, 2");
        
        JLabel lblMaxSearchDistance = new JLabel("Max. allowed distance");
        lblMaxSearchDistance.setToolTipText("<html>Maximum allowed distance of parts to be detected.<br/>\r\nAlso used by some pipelines for better performance.</html>");
        panel.add(lblMaxSearchDistance, "2, 4, right, default");
        
        maxSearchDistance = new JTextField();
        panel.add(maxSearchDistance, "4, 4, fill, default");
        maxSearchDistance.setColumns(10);

        JLabel lblBottomVision = new JLabel("Bottom Vision Settings");
        panel.add(lblBottomVision, "2, 6, right, default");
                
                visionSettings = new JComboBox(new VisionSettingsComboBoxModel(BottomVisionSettings.class));
                visionSettings.setMaximumRowCount(20);
                visionSettings.setRenderer(new NamedListCellRenderer<>());
                visionSettings.addItemListener(new ItemListener() {
                    public void itemStateChanged(ItemEvent e) {
                        reloadWizard = true;
                    }
                });
                panel.add(visionSettings, "4, 6, 3, 1, fill, default");

        JLabel lblPreRot = new JLabel("Rotate parts prior to vision?");
        lblPreRot.setToolTipText("Pre-rotate default setting for bottom vision. Can be overridden on individual parts.");
        panel.add(lblPreRot, "2, 8");

        preRotCheckbox = new JCheckBox("");
        panel.add(preRotCheckbox, "4, 8");
        
        JLabel lblMaxVisionPasses = new JLabel("Max. vision passes");
        lblMaxVisionPasses.setToolTipText("The maximum number of bottom vision passes performed to get a good fix on the part.");
        panel.add(lblMaxVisionPasses, "2, 10, right, default");
        
        textFieldMaxVisionPasses = new JTextField();
        panel.add(textFieldMaxVisionPasses, "4, 10");
        textFieldMaxVisionPasses.setColumns(10);
        
        JLabel lblMaxLinearOffset = new JLabel("Max. linear offset");
        lblMaxLinearOffset.setToolTipText("The maximum linear part offset accepted as a good fix i.e. where no additional vision pass is needed.");
        panel.add(lblMaxLinearOffset, "2, 12, right, default");
        
        textFieldMaxLinearOffset = new JTextField();
        panel.add(textFieldMaxLinearOffset, "4, 12, fill, default");
        textFieldMaxLinearOffset.setColumns(10);
        
        JLabel lblMaxAngularOffset = new JLabel("Max. angular offset");
        lblMaxAngularOffset.setToolTipText("The maximum angular part offset accepted as a good fix i.e. where no additional vision pass is needed.");
        panel.add(lblMaxAngularOffset, "6, 12, right, default");
        
        textFieldMaxAngularOffset = new JTextField();
        panel.add(textFieldMaxAngularOffset, "8, 12, fill, default");
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
        return "ReferenceBottomVision";
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

        addWrappedBinding(bottomVision, "maxSearchDistance", maxSearchDistance, "text", lengthConverter);
        
        addWrappedBinding(bottomVision, "maxVisionPasses", textFieldMaxVisionPasses, "text", intConverter);
        addWrappedBinding(bottomVision, "maxLinearOffset", textFieldMaxLinearOffset, "text", lengthConverter);
        addWrappedBinding(bottomVision, "maxAngularOffset", textFieldMaxAngularOffset, "text", doubleConverter);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(maxSearchDistance);
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
