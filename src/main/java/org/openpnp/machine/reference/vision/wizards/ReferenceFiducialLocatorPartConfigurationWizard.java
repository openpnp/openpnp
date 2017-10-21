package org.openpnp.machine.reference.vision.wizards;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.machine.reference.vision.ReferenceFiducialLocator;
import org.openpnp.machine.reference.vision.ReferenceFiducialLocator.PartSettings;
import org.openpnp.model.Configuration;
import org.openpnp.model.Part;
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

public class ReferenceFiducialLocatorPartConfigurationWizard extends AbstractConfigurationWizard {
    private final ReferenceFiducialLocator fiducialLocator;
    private final Part part;
    private final PartSettings partSettings;
    
    private JTextField textFieldVisionOffsetX;
    private JTextField textFieldVisionOffsetY;

    public ReferenceFiducialLocatorPartConfigurationWizard(ReferenceFiducialLocator fiducialLocator,
            Part part) {
        this.fiducialLocator = fiducialLocator;
        this.part = part;
        this.partSettings = fiducialLocator.getPartSettings(part);

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
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
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

        JButton btnLoadDefault = new JButton("Reset to Default");
        btnLoadDefault.addActionListener((e) -> {
            int result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                    "This will replace the current part pipeline with the default pipeline. Are you sure?",
                    null, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (result == JOptionPane.YES_OPTION) {
                UiUtils.messageBoxOnException(() -> {
                    partSettings.setPipeline(fiducialLocator.getPipeline().clone());
                    editPipeline();
                });
            }
        });
        panel.add(btnLoadDefault, "6, 2");
        
        JPanel panelVisionOffset = new JPanel();
        contentPanel.add(panelVisionOffset);
        panelVisionOffset.setBorder(new TitledBorder(null, "Vision Offset", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        panelVisionOffset.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        ColumnSpec.decode("left:default:grow"),},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblX = new JLabel("X");
        panelVisionOffset.add(lblX, "4, 2");

        JLabel lblY = new JLabel("Y");
        panelVisionOffset.add(lblY, "6, 2");

        JLabel lblFeedStartLocation = new JLabel("Offset");
        lblFeedStartLocation.setToolTipText(
                "Detection of reflective surfaces can be improved if doing vision with an offset to the camera's center.");
        panelVisionOffset.add(lblFeedStartLocation, "2, 4, right, default");

        textFieldVisionOffsetX = new JTextField();
        panelVisionOffset.add(textFieldVisionOffsetX, "4, 4");
        textFieldVisionOffsetX.setColumns(8);

        textFieldVisionOffsetY = new JTextField();
        panelVisionOffset.add(textFieldVisionOffsetY, "6, 4");
        textFieldVisionOffsetY.setColumns(8);

    }

    private void editPipeline() throws Exception {
        CvPipeline pipeline = partSettings.getPipeline();
        Camera camera = Configuration.get().getMachine().getDefaultHead().getDefaultCamera();
        pipeline.setProperty("camera", camera);
        pipeline.setProperty("part", part);
        pipeline.setProperty("package", part.getPackage());
        pipeline.setProperty("footprint", part.getPackage().getFootprint());
        CvPipelineEditor editor = new CvPipelineEditor(pipeline);
        JDialog dialog = new JDialog(MainFrame.get(), "Fiducial Locator Pipeline");
        dialog.getContentPane().setLayout(new BorderLayout());
        dialog.getContentPane().add(editor);
        dialog.setSize(1024, 768);
        dialog.setVisible(true);
    }
    @Override
    public void createBindings() {
    	LengthConverter lengthConverter = new LengthConverter();
    	
    	MutableLocationProxy visionOffsetLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, part, "offsetVision", visionOffsetLocation, "location");
        addWrappedBinding(visionOffsetLocation, "lengthX", textFieldVisionOffsetX, "text", lengthConverter);
        addWrappedBinding(visionOffsetLocation, "lengthY", textFieldVisionOffsetY, "text", lengthConverter);
        
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldVisionOffsetX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldVisionOffsetY);
    }
        
    
    @Override
    public String getWizardName() {
        return "ReferenceFiducialLocator";
    }
    
}
