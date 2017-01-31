package org.openpnp.machine.reference.vision.wizards;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.machine.reference.vision.ReferenceFiducialLocator;
import org.openpnp.machine.reference.vision.ReferenceFiducialLocator.PartFiducialPipeline;
import org.openpnp.model.Configuration;
import org.openpnp.model.Part;
import org.openpnp.util.UiUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.ui.CvPipelineEditor;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class ReferenceFiducialLocatorConfigurationWizard extends AbstractConfigurationWizard {
    private static final long serialVersionUID = 2L;
    private final ReferenceFiducialLocator fiducialLocator;
    private final PartFiducialPipeline fiducialSettings;

    private JCheckBox enabledCheckbox;
    private JButton editCustomPipelineButton;

    public ReferenceFiducialLocatorConfigurationWizard(ReferenceFiducialLocator fiducialLocator,
            Part part) {
        this.fiducialLocator = fiducialLocator;
        this.fiducialSettings = fiducialLocator.getFiducialSettings(part);

        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(null, "Fiducial Vision Settings", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        contentPanel.add(panel);
        panel.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("right:default"),
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblPipeline = new JLabel("Fiducial Vision Pipeline");
        JButton editDefaultPipelineButton = new JButton("Edit Default Fiducial Pipeline");
        JLabel lblEnabled = new JLabel("Custom part pipeline enabled?");
        enabledCheckbox = new JCheckBox("");
        editCustomPipelineButton = new JButton("Edit Fiducial Pipeline for Part");

        editDefaultPipelineButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                UiUtils.messageBoxOnException(() -> {
                    editDefaultPipeline();
                });
            }
        });

        editCustomPipelineButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                UiUtils.messageBoxOnException(() -> {
                    editCustomPipeline(part);
                });
            }
        });

        enabledCheckbox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                UiUtils.messageBoxOnException(() -> {
                    handleEnableCheckbox();
                });
            }
        });

        // column then row, 1-based
        panel.add(lblPipeline, "2, 2");
        panel.add(editDefaultPipelineButton, "6, 2");
        panel.add(lblEnabled, "2, 4");
        panel.add(enabledCheckbox, "4, 4");
        panel.add(editCustomPipelineButton, "6, 4");
    }

    private void editDefaultPipeline() throws Exception {
        CvPipeline pipeline = fiducialLocator.getDefaultPipeline();
        editPipeline(pipeline);
    }

    private void editCustomPipeline(Part part) throws Exception {
        CvPipeline pipeline = fiducialLocator.getFiducialSettings(part).getPipeline();
        editPipeline(pipeline);
    }

    private void editPipeline(CvPipeline pipeline) {
        if (pipeline.getCamera() == null) {
            try {
                pipeline.setCamera(
                        Configuration.get().getMachine().getDefaultHead().getDefaultCamera());
            }
            catch (Exception e) {
            }
        }

        JDialog dialog = new JDialog(MainFrame.get(), "Fiducial Vision Pipeline");
        CvPipelineEditor editor = new CvPipelineEditor(pipeline);
        dialog.getContentPane().setLayout(new BorderLayout());
        dialog.getContentPane().add(editor);
        dialog.setSize(1024, 768);
        dialog.setVisible(true);
    }

    private void handleEnableCheckbox() {
        // if selected but pipeline is null (new) get default
        // do this now instead of on Apply or button will cause Null error
        if (enabledCheckbox.isSelected() && fiducialSettings.getPipeline() == null) {
            try {
                fiducialSettings.setPipeline(fiducialLocator.getDefaultPipeline().clone());
            }
            catch (Exception ignored) {
            }
        }
        // button is enabled when checkbox is selected
        editCustomPipelineButton.setEnabled(enabledCheckbox.isSelected());
    }

    @Override
    public void createBindings() {
        addWrappedBinding(fiducialSettings, "enabled", enabledCheckbox, "selected");
    }

    @Override
    protected void loadFromModel() {
        super.loadFromModel();

        // after load, set button enable
        // and set up pipelines where enabled, but no record in XML (manually edited?)
        editCustomPipelineButton.setEnabled(enabledCheckbox.isSelected());
        if (enabledCheckbox.isSelected() && fiducialSettings.getPipeline() == null) {
            try {
                fiducialSettings.setPipeline(fiducialLocator.getDefaultPipeline().clone());
            }
            catch (Exception e) {
            }
        }
    }

    @Override
    protected void saveToModel() {
        // before save, nerf the pipeline if not enabled (sorry)
        if (!enabledCheckbox.isSelected()) {
            fiducialSettings.setPipeline(null);
        }
        else {
            // if enabled but we didn't save a pipeline, that's okay, fix on load
            if (fiducialSettings.getPipeline() == null) {
                // try {
                // fiducialSettings.setPipeline(fiducialLocator.getDefaultPipeline().clone());
                // }
                // catch (Exception e) {
                // }
            }
        }
        super.saveToModel();
    }
}
