package org.openpnp.machine.reference.vision.wizards;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.model.AbstractVisionSettings;
import org.openpnp.util.UiUtils;
import org.openpnp.vision.pipeline.ui.CvPipelineEditor;
import org.openpnp.vision.pipeline.ui.CvPipelineEditorDialog;

import javax.swing.*;
import javax.swing.border.TitledBorder;

@SuppressWarnings("serial")
public class VisionSettingsConfigurationWizard extends AbstractConfigurationWizard {
    private AbstractVisionSettings visionSettings;

    private JTextField idEntry;
    private JTextField nameEntry;

    JPanel panel;
    
    public VisionSettingsConfigurationWizard(AbstractVisionSettings visionSettings) {
        this.visionSettings = visionSettings;
        createUi();
    }

    private void createUi() {
        createPanel();

        JLabel lblId = new JLabel("ID");
        JLabel lblName = new JLabel("Name");

        idEntry = new JTextField();
        idEntry.setText(visionSettings.getId());
        idEntry.setColumns(10);

        nameEntry = new JTextField();
        nameEntry.setText(visionSettings.getName());
        nameEntry.setColumns(20);

        panel.add(lblId, "2, 2, right, default");
        panel.add(idEntry, "4, 2, left, default");
        panel.add(lblName, "2, 4, right, default");
        panel.add(nameEntry, "4, 4, left, default");

        JButton editPipelineButton = new JButton("Edit");
        editPipelineButton.addActionListener(e -> UiUtils.messageBoxOnException(this::editPipeline));
        panel.add(editPipelineButton, "4, 6");
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

    private void editPipeline() {
        CvPipelineEditor editor = new CvPipelineEditor(visionSettings, true);
        JDialog dialog = new CvPipelineEditorDialog(MainFrame.get(), "Vision Pipeline", editor);
        dialog.setVisible(true);
    }

    @Override
    public void createBindings() {
        addWrappedBinding(visionSettings, "id", idEntry, "text");
        addWrappedBinding(visionSettings, "name", nameEntry, "text");
    }
}
