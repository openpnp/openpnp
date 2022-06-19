package org.openpnp.machine.neoden4.wizards;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.machine.neoden4.Neoden4SwitcherCamera;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Camera;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class Neoden4SwitcherCameraConfigurationWizard extends AbstractConfigurationWizard {
    private final Neoden4SwitcherCamera camera;
    private JLabel lblNewLabel;
    private JLabel lblNewLabel_1;
    private JComboBox sourceCamera;
    private JTextField switcher;

    private JLabel lblExposure;
    private JLabel lblGain;
    private JTextField cameraExposureTextField;
    private JTextField cameraGainTextField;
    
    public Neoden4SwitcherCameraConfigurationWizard(Neoden4SwitcherCamera camera) {
        this.camera = camera;
        createUi();
    }
    private void createUi() {
        contentPanel.setLayout(new FormLayout(new ColumnSpec[] {
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
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        lblNewLabel = new JLabel("Source Camera");
        contentPanel.add(lblNewLabel, "2, 2, right, default");
        
        sourceCamera = new JComboBox();
        contentPanel.add(sourceCamera, "4, 2, fill, default");
        
        lblNewLabel_1 = new JLabel("Switcher Number");
        contentPanel.add(lblNewLabel_1, "2, 4, right, default");
        
        switcher = new JTextField();
        contentPanel.add(switcher, "4, 4, fill, default");
        switcher.setColumns(10);
        
        for (Camera camera : Configuration.get().getMachine().getCameras()) {
            sourceCamera.addItem(camera);
        }
        
        
        lblExposure = new JLabel("Exposure");
        lblExposure.setHorizontalAlignment(SwingConstants.TRAILING);
        contentPanel.add(lblExposure, "2, 6, right, default");
        
        cameraExposureTextField = new JTextField();
        cameraExposureTextField.setColumns(16);
        contentPanel.add(cameraExposureTextField, "4, 6, fill, default");
        
        lblGain = new JLabel("Gain");
        lblGain.setHorizontalAlignment(SwingConstants.TRAILING);
        contentPanel.add(lblGain, "2, 8, right, default");
        
        cameraGainTextField = new JTextField();
        cameraGainTextField.setColumns(16);
        contentPanel.add(cameraGainTextField, "4, 8, fill, default");
    }

    @Override
    public void createBindings() {
        IntegerConverter intConverter = new IntegerConverter();
        addWrappedBinding(camera, "camera", sourceCamera, "selectedItem");
        addWrappedBinding(camera, "switcher", switcher, "text", intConverter);
        addWrappedBinding(camera, "exposure", cameraExposureTextField, "text", intConverter);
        addWrappedBinding(camera, "gain", cameraGainTextField, "text", intConverter);
        
        ComponentDecorators.decorateWithAutoSelect(switcher);
    }
}
