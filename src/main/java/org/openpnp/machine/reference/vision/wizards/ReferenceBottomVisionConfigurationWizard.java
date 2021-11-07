package org.openpnp.machine.reference.vision.wizards;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.machine.reference.vision.ReferenceBottomVision;
import org.openpnp.machine.reference.vision.ReferenceBottomVision.PartSettings;
import org.openpnp.model.Configuration;
import org.openpnp.util.UiUtils;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.ui.CvPipelineEditor;
import org.openpnp.vision.pipeline.ui.CvPipelineEditorDialog;

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

        JLabel lblPipeline = new JLabel("Pipeline");
        panel.add(lblPipeline, "2, 6");

        JButton editPipelineButton = new JButton("Edit");
        editPipelineButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                UiUtils.messageBoxOnException(() -> {
                    editPipeline();
                });
            }
        });
        panel.add(editPipelineButton, "4, 6");

        JButton btnResetToDefault = new JButton("Reset to Default");
        btnResetToDefault.addActionListener((e) -> {
            int result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                    "This will replace the current pipeline with the built in default pipeline. Are you sure?",
                    null, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (result == JOptionPane.YES_OPTION) {
                UiUtils.messageBoxOnException(() -> {
                    bottomVision.setPipeline(ReferenceBottomVision.createDefaultPipeline());
                    editPipeline();
                });
            }
        });
        panel.add(btnResetToDefault, "6, 6");

        JButton btnResetAllTo = new JButton("Reset All Parts");
        btnResetAllTo.addActionListener((e) -> {
            int result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                    "This will replace all custom part pipelines with the current pipeline. Are you sure?",
                    null, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (result == JOptionPane.YES_OPTION) {
                UiUtils.messageBoxOnException(() -> {
                    for (PartSettings partSettings : bottomVision.getPartSettingsByPartId()
                                                                 .values()) {
                        partSettings.setPipeline(bottomVision.getPipeline()
                                                             .clone());
                    }
                    MessageBoxes.infoBox("Parts Reset",
                            "All custom part pipelines have been reset.");
                });
            }
        });
        panel.add(btnResetAllTo, "8, 6");

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
    
    private void editPipeline() throws Exception {
        CvPipeline pipeline = bottomVision.getPipeline();
        pipeline.setProperty("camera", VisionUtils.getBottomVisionCamera());
		pipeline.setProperty("nozzle", MainFrame.get().getMachineControls().getSelectedNozzle());
        CvPipelineEditor editor = new CvPipelineEditor(pipeline);
        JDialog dialog = new CvPipelineEditorDialog(MainFrame.get(), "Bottom Vision Pipeline", editor);
        dialog.setVisible(true);
}

    @Override
    public String getWizardName() {
        return "ReferenceBottomVision";
    }
    
    @Override
    public void createBindings() {
        addWrappedBinding(bottomVision, "enabled", enabledCheckbox, "selected");
        addWrappedBinding(bottomVision, "preRotate", preRotCheckbox, "selected");
        
        LengthConverter lengthConverter = new LengthConverter();
        IntegerConverter intConverter = new IntegerConverter();
        DoubleConverter doubleConverter = new DoubleConverter(Configuration.get()
                                                                           .getLengthDisplayFormat());
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
}
