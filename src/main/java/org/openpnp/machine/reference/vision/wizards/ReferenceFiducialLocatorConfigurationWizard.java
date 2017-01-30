package org.openpnp.machine.reference.vision.wizards;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.machine.reference.vision.ReferenceFiducialLocator;
import org.openpnp.machine.reference.vision.ReferenceFiducialLocator.CustomFiducialPipelineSettings;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PartAlignment;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.UiUtils;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.ui.CvPipelineEditor;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class ReferenceFiducialLocatorConfigurationWizard extends AbstractConfigurationWizard {
    private final ReferenceFiducialLocator fiducialLocator;
    private final CustomFiducialPipelineSettings fiducialSettings;

    private JCheckBox enabledCheckbox;
    private JButton editCustomPipelineButton;

    public ReferenceFiducialLocatorConfigurationWizard(ReferenceFiducialLocator fiducialLocator,
            Part part) {
        this.fiducialLocator = fiducialLocator;
        this.fiducialSettings = fiducialLocator.getFiducialSettings(part);

        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(null, "Fiducial", TitledBorder.LEADING, TitledBorder.TOP,
                null, null));
        contentPanel.add(panel);
        panel.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("right:default"),
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                		FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        JButton editDefaultPipelineButton = new JButton("Edit Default Fiducial Pipeline");
        editDefaultPipelineButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                UiUtils.messageBoxOnException(() -> {
                    editDefaultPipeline();
                });
            }
        });
        panel.add(editDefaultPipelineButton, "4, 2");
        
        JLabel lblEnabled = new JLabel("Enabled?");
        panel.add(lblEnabled, "2, 4");

        enabledCheckbox = new JCheckBox("");

        editCustomPipelineButton = new JButton("Edit Fiducial Pipeline for Part");
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
                    editCustomPipelineButton.setEnabled(enabledCheckbox.isSelected());
            		if (enabledCheckbox.isSelected() && fiducialSettings.getPipeline() == null) {
            			try {
            				fiducialSettings.setPipeline(fiducialLocator.getDefaultPipeline().clone());
            			}
            			catch (Exception ignored) {
            			}
            		}
                });
			}
		});
        panel.add(enabledCheckbox, "4, 4");

        JLabel lblPipeline = new JLabel("Pipeline");
        panel.add(lblPipeline, "2, 6");

        panel.add(editCustomPipelineButton, "4, 6");
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
	        	pipeline.setCamera(Configuration.get().getMachine().getDefaultHead().getDefaultCamera());
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

    @Override
    public void createBindings() {
        addWrappedBinding(fiducialSettings, "enabled", enabledCheckbox, "selected");
    }
    
    @Override
    protected void loadFromModel() {
        super.loadFromModel();
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
    	if (!enabledCheckbox.isSelected()) {
        	fiducialSettings.setPipeline(null);
        }
    	else {
    		if (fiducialSettings.getPipeline() == null) {
    			try {
    				fiducialSettings.setPipeline(fiducialLocator.getDefaultPipeline().clone());
    			}
    			catch (Exception e) {
    			}
    		}
    	}
        super.saveToModel();
    }
}
