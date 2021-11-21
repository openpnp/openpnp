package org.openpnp.machine.reference.vision.wizards;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.machine.reference.vision.ReferenceBottomVision;
import org.openpnp.model.BottomVisionSettings;
import org.openpnp.model.Configuration;
import org.openpnp.util.UiUtils;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.ui.CvPipelineEditor;
import org.openpnp.vision.pipeline.ui.CvPipelineEditorDialog;

import javax.swing.*;
import javax.swing.border.TitledBorder;

@SuppressWarnings("serial")
public class BottomVisionSettingsConfigurationWizard extends AbstractConfigurationWizard {
    private BottomVisionSettings visionSettings;
    
    private JPanel panel;
    private JCheckBox enabledCheckbox;
    private JComboBox comboBoxPreRotate;
    private JComboBox comboBoxCheckPartSizeMethod;
    private JTextField textPartSizeTolerance;
    private JComboBox comboBoxMaxRotation;
    
    public BottomVisionSettingsConfigurationWizard(BottomVisionSettings visionSettings) {
        this.visionSettings = visionSettings;
        createUi();
    }

    private void createUi() {
        createPanel();

        JLabel lblEnabled = new JLabel("Enabled?");
        panel.add(lblEnabled, "2, 2");

        enabledCheckbox = new JCheckBox("");
        panel.add(enabledCheckbox, "4, 2");

        JLabel lblPrerotate = new JLabel("Pre-rotate");
        panel.add(lblPrerotate, "2, 4, right, default");

        comboBoxPreRotate = new JComboBox(ReferenceBottomVision.PreRotateUsage.values());
        panel.add(comboBoxPreRotate, "4, 4");

        JLabel lblMaxRotation = new JLabel("Rotation");
        panel.add(lblMaxRotation, "2, 6, right, default");

        comboBoxMaxRotation = new JComboBox(ReferenceBottomVision.MaxRotation.values());
        comboBoxMaxRotation.setToolTipText(
                "Adjust for all parts, where only some minor offset is expected. Full for parts, where bottom vision detects pin 1");
        panel.add(comboBoxMaxRotation, "4, 6, fill, default");
        
        JLabel lblPartCheckType = new JLabel("Part size check");
        panel.add(lblPartCheckType, "2, 8");

        comboBoxCheckPartSizeMethod = new JComboBox(ReferenceBottomVision.PartSizeCheckMethod.values());
        panel.add(comboBoxCheckPartSizeMethod, "4, 8, fill, default");

        JLabel lblPartSizeTolerance = new JLabel("Size tolerance (%)");
        panel.add(lblPartSizeTolerance, "2, 12");

        textPartSizeTolerance = new JTextField();
        panel.add(textPartSizeTolerance, "4, 12, fill, default");
        
        JLabel lblPipeline = new JLabel("Pipeline");
        panel.add(lblPipeline, "2, 14, right, default");

        JButton editPipelineButton = new JButton("Edit");
        editPipelineButton.addActionListener(e -> UiUtils.messageBoxOnException(this::editPipeline));
        panel.add(editPipelineButton, "4, 14");
        
        JButton resetPipelineButton = new JButton("Reset to Default");
        resetPipelineButton.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                    "This will replace the bottom vision settings with the default settings. Are you sure??", null,
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (result == JOptionPane.YES_OPTION) {
                UiUtils.messageBoxOnException(() -> {
                    ReferenceBottomVision bottomVision = (ReferenceBottomVision) Configuration.get().getMachine().getPartAlignments().get(0);
                    visionSettings.setValues(bottomVision.getBottomVisionSettings());
                });
            }
        });
        panel.add(resetPipelineButton, "6, 14");
    }

    private void createPanel() {
        panel = new JPanel();
        panel.setBorder(new TitledBorder(null, "General", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panel);
        panel.setLayout(new FormLayout(new ColumnSpec[]{
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("right:max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[]{
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
                        FormSpecs.DEFAULT_ROWSPEC,}));
    }

    private void editPipeline() throws Exception {
        CvPipeline pipeline = visionSettings.getCvPipeline();
        pipeline.setProperty("camera", VisionUtils.getBottomVisionCamera());
        pipeline.setProperty("nozzle", MainFrame.get().getMachineControls().getSelectedNozzle());
        
        CvPipelineEditor editor = new CvPipelineEditor(pipeline);
        JDialog dialog = new CvPipelineEditorDialog(MainFrame.get(), "Bottom Vision Pipeline", editor);
        dialog.setVisible(true);
    }

    @Override
    public void createBindings() {
        IntegerConverter intConverter = new IntegerConverter();
        
        addWrappedBinding(visionSettings, "enabled", enabledCheckbox, "selected");
        addWrappedBinding(visionSettings, "preRotateUsage", comboBoxPreRotate, "selectedItem");
        addWrappedBinding(visionSettings, "checkPartSizeMethod", comboBoxCheckPartSizeMethod, "selectedItem");
        addWrappedBinding(visionSettings, "checkSizeTolerancePercent", textPartSizeTolerance, "text", intConverter);
        addWrappedBinding(visionSettings, "maxRotation", comboBoxMaxRotation, "selectedItem");
    }
}
