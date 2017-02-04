package org.openpnp.machine.reference.vision.wizards;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

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
import org.openpnp.model.Footprint;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Camera;
import org.openpnp.util.UiUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.stages.ImageInput;
import org.openpnp.vision.pipeline.ui.CvPipelineEditor;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class ReferenceFiducialLocatorConfigurationWizard extends AbstractConfigurationWizard {
    private final ReferenceFiducialLocator fiducialLocator;
    private final PartFiducialPipeline fiducialSettings;
    private final Part fiducialPart;

    private JCheckBox enabledCheckbox;
    private JCheckBox useTemplateCheckbox;
    private JButton editCustomPipelineButton;

    public ReferenceFiducialLocatorConfigurationWizard(ReferenceFiducialLocator fiducialLocator,
            Part part) {
        this.fiducialLocator = fiducialLocator;
        this.fiducialPart = part;

        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(null, "Fiducial Vision Settings", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        contentPanel.add(panel);
        panel.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("right:default"),
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblPipeline = new JLabel("Fiducial Vision Pipeline");
        JButton editDefaultPipelineButton = new JButton("Edit Default Fiducial Pipeline");
        JLabel lblEnabled = new JLabel("Use custom part pipeline?");
        enabledCheckbox = new JCheckBox("");
        editCustomPipelineButton = new JButton("Edit Fiducial Pipeline for Part");
        JLabel lblTemplate = new JLabel("Use part footprint template?");
        useTemplateCheckbox = new JCheckBox("");

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
        panel.add(lblTemplate, "2, 4");
        panel.add(useTemplateCheckbox, "6, 4");
        panel.add(lblEnabled, "2, 6");
        panel.add(enabledCheckbox, "4, 6");
        panel.add(editCustomPipelineButton, "6, 6");
        this.fiducialSettings = fiducialLocator.getFiducialSettings(part);
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
        BufferedImage template = null;
        Camera camera = null;
        
        try {
            camera = Configuration.get().getMachine().getDefaultHead().getDefaultCamera();
        }
        catch (Exception ignored) {
        }
        if (pipeline.getCamera() == null) {
            pipeline.setCamera(camera);
        }

        try {
            Footprint fp = null;
            try {
                fp = fiducialPart.getPackage().getFootprint();
            }
            catch (Exception e) {
                fp = null;
            }
            if (fp != null) {
                CvStage templateStage = pipeline.getStage("template");
                if (templateStage != null) {
                    if (templateStage instanceof ImageInput) {
                        ImageInput imgIn = (ImageInput) templateStage;
                        template = ReferenceFiducialLocator.createTemplate(camera.getUnitsPerPixel(), fp);
                        imgIn.setInputImage(template);
                    }
                }
            }
        }
        catch (Exception ignored) {
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
        addWrappedBinding(fiducialSettings, "useCustomPipeline", enabledCheckbox, "selected");
        addWrappedBinding(fiducialSettings, "useTemplateMatch", useTemplateCheckbox, "selected");
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
