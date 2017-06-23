package org.openpnp.machine.reference.vision.wizards;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.machine.reference.vision.ReferenceFiducialLocator;
import org.openpnp.machine.reference.vision.ReferenceFiducialLocator.PartSettings;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Camera;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.util.UiUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.stages.SetResult;
import org.openpnp.vision.pipeline.ui.CvPipelineEditor;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class ReferenceFiducialLocatorConfigurationWizard extends AbstractConfigurationWizard {
    private final ReferenceFiducialLocator fiducialLocator;

    public ReferenceFiducialLocatorConfigurationWizard(ReferenceFiducialLocator fiducialLocator) {
        this.fiducialLocator = fiducialLocator;

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
                FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblPipeline = new JLabel("Pipeline");
        panel.add(lblPipeline, "2, 2");

        JButton editPipelineButton = new JButton("Edit");
        editPipelineButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                UiUtils.messageBoxOnException(() -> {
                    editPipeline();
                });
            }
        });
        panel.add(editPipelineButton, "4, 2");

        JButton btnResetToDefault = new JButton("Reset to Default");
        btnResetToDefault.addActionListener((e) -> {
            int result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                    "This will replace the current pipeline with the built in default pipeline. Are you sure?",
                    null, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (result == JOptionPane.YES_OPTION) {
                UiUtils.messageBoxOnException(() -> {
                    fiducialLocator.setPipeline(ReferenceFiducialLocator.createDefaultPipeline());
                    editPipeline();
                });
            }
        });
        panel.add(btnResetToDefault, "6, 2");

        JButton btnResetAllTo = new JButton("Reset All Parts");
        btnResetAllTo.addActionListener((e) -> {
            int result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                    "This will replace all custom part pipelines with the current pipeline. Are you sure?",
                    null, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (result == JOptionPane.YES_OPTION) {
                UiUtils.messageBoxOnException(() -> {
                    for (PartSettings partSettings : fiducialLocator.getPartSettingsByPartId()
                            .values()) {
                        partSettings.setPipeline(fiducialLocator.getPipeline().clone());
                    }
                    MessageBoxes.infoBox("Parts Reset",
                            "All custom part pipelines have been reset.");
                });
            }
        });
        panel.add(btnResetAllTo, "8, 2");
    }

    private void editPipeline() throws Exception {
        CvPipeline pipeline = fiducialLocator.getPipeline();
        Camera camera = Configuration.get().getMachine().getDefaultHead().getDefaultCamera();
        if (pipeline.getStage("template") instanceof SetResult) {
            SetResult setResult = (SetResult) pipeline.getStage("template");
            // We could inject a default template here since we don't have a part.
        }
        pipeline.setCamera(camera);
        CvPipelineEditor editor = new CvPipelineEditor(pipeline);
        JDialog dialog = new JDialog(MainFrame.get(), "Fiducial Locator Pipeline");
        dialog.getContentPane().setLayout(new BorderLayout());
        dialog.getContentPane().add(editor);
        dialog.setSize(1024, 768);
        dialog.setVisible(true);
    }

    @Override
    public void createBindings() {
    }
    
    @Override
    public String getWizardName() {
        return "ReferenceFiducialLocator";
    }
}
