package org.openpnp.machine.reference.camera.wizards;


import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.machine.reference.camera.MjpgCaptureCamera;
import org.openpnp.util.UiUtils;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class MjpgCaptureCameraWizard extends AbstractConfigurationWizard {

    private final MjpgCaptureCamera camera;
    private JPanel panelGeneral;

    public MjpgCaptureCameraWizard(MjpgCaptureCamera camera) {
        this.camera = camera;

        panelGeneral = new JPanel();
        contentPanel.add(panelGeneral);
        panelGeneral.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null),
                "General", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        panelGeneral.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
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

        lblIP = new JLabel("Camera IP Address and Port Number");
        panelGeneral.add(lblIP, "2, 2, right, default");

        ipTextField = new JTextField();
        panelGeneral.add(ipTextField, "4, 2");
        ipTextField.setColumns(16);

        lbluseFor_ip = new JLabel("(IP:port)");
        panelGeneral.add(lbluseFor_ip, "6, 2");
        
        lblTimeout = new JLabel("Timeout [ms]");
        panelGeneral.add(lblTimeout, "2, 4, right, default");
        
        timeout = new JTextField();
        panelGeneral.add(timeout, "4, 4, left, default");
        timeout.setColumns(10);

    }

    @Override
    public void createBindings() {
        IntegerConverter intConverter = new IntegerConverter();

        // Should always be last so that it doesn't trigger multiple camera reloads.
        addWrappedBinding(camera, "mjpgURL", ipTextField, "text");
        addWrappedBinding(camera, "timeout", timeout, "text", intConverter);

        ComponentDecorators.decorateWithAutoSelect(ipTextField);
        ComponentDecorators.decorateWithAutoSelect(timeout);
    }

    @Override
    protected void loadFromModel() {
        super.loadFromModel();
    }

    @Override
    protected void saveToModel() {
        super.saveToModel();
        if (camera.isDirty()) {
            UiUtils.messageBoxOnException(() -> {
                camera.reinitialize(); 
            });
        }
    }

    private JLabel lblIP;
    private JTextField ipTextField;
    private JLabel lbluseFor_ip;
    private JLabel lblTimeout;
    private JTextField timeout;


}
