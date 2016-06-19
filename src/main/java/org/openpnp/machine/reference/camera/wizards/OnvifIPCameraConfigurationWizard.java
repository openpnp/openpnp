/*
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.machine.reference.camera.wizards;

import java.awt.Color;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.onvif.ver10.schema.VideoResolution;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.machine.reference.camera.OnvifIPCamera;
import org.openpnp.machine.reference.wizards.ReferenceCameraConfigurationWizard;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class OnvifIPCameraConfigurationWizard extends ReferenceCameraConfigurationWizard {
    private final OnvifIPCamera camera;

    private JPanel panelGeneral;

    public OnvifIPCameraConfigurationWizard(OnvifIPCamera camera) {
        super(camera);

        this.camera = camera;

        panelGeneral = new JPanel();
        contentPanel.add(panelGeneral);
        panelGeneral.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null),
                "General", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        panelGeneral.setLayout(new FormLayout(new ColumnSpec[] {
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
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));

        lblIP = new JLabel("Camera IP");
        panelGeneral.add(lblIP, "2, 2, right, default");

        ipTextField = new JTextField();
        panelGeneral.add(ipTextField, "4, 2");
        ipTextField.setColumns(16);

        lbluseFor_ip = new JLabel("(IP:port)");
        panelGeneral.add(lbluseFor_ip, "6, 2");

        lblUsername = new JLabel("Username");
        panelGeneral.add(lblUsername, "2, 4, right, default");

        usernameTextField = new JTextField();
        panelGeneral.add(usernameTextField, "4, 4");
        usernameTextField.setColumns(16);

        lbluseFor_un = new JLabel("(normally required)");
        panelGeneral.add(lbluseFor_un, "6, 4");

        lblPassword = new JLabel("Password");
        panelGeneral.add(lblPassword, "2, 6, right, default");

        passwordTextField = new JTextField();
        panelGeneral.add(passwordTextField, "4, 6");
        passwordTextField.setColumns(16);

        lbluseFor_pw = new JLabel("(leave blank for none)");
        panelGeneral.add(lbluseFor_pw, "6, 6");

        lblFps = new JLabel("FPS");
        panelGeneral.add(lblFps, "2, 8, right, default");

        fpsTextField = new JTextField();
        panelGeneral.add(fpsTextField, "4, 8");
        fpsTextField.setColumns(10);

        lbluseFor_fps = new JLabel("(refresh rate)");
        panelGeneral.add(lbluseFor_fps, "6, 8");

        lblSupportedResolutions = new JLabel("Resolution");
        panelGeneral.add(lblSupportedResolutions, "2, 10, right, default");

        cboSupportedResolutions = new JComboBox<String>();
        refreshResolutionList();
        panelGeneral.add(cboSupportedResolutions, "4, 10, fill, default");

        lbluseFor_res = new JLabel("(only supported resolutions shown)");
        panelGeneral.add(lbluseFor_res, "6, 10");

        lblResizeWidth = new JLabel("Target Width");
        panelGeneral.add(lblResizeWidth, "2, 12, right, default");

        resizeWidthTextField = new JTextField();
        panelGeneral.add(resizeWidthTextField, "4, 12");
        resizeWidthTextField.setColumns(10);

        lbluseFor_rw = new JLabel("(Use 0 for no resizing)");
        panelGeneral.add(lbluseFor_rw, "6, 12");

        lblResizeHeight = new JLabel("Target Height");
        panelGeneral.add(lblResizeHeight, "2, 14, right, default");

        resizeHeightTextField = new JTextField();
        panelGeneral.add(resizeHeightTextField, "4, 14");
        resizeHeightTextField.setColumns(10);

        lbluseFor_rh = new JLabel("(Use 0 for no resizing)");
        panelGeneral.add(lbluseFor_rh, "6, 14");
    }
    
    private void refreshResolutionList() {
        cboSupportedResolutions.removeAllItems();

        // Empty string will cause the camera to use the default/highest
        // resolution available
        cboSupportedResolutions.addItem("");

        List<VideoResolution> supportedResolutions = camera.getSupportedResolutions();
        if (supportedResolutions != null) {
            for (VideoResolution res : supportedResolutions) {
                String strRes = res.getWidth() + "x" + res.getHeight();
    
                cboSupportedResolutions.addItem(strRes);
            }
        }

        if (camera.getPreferredResolution() != null) {
            cboSupportedResolutions.setSelectedItem(camera.getPreferredResolution());
        }
    }

    @Override
    public void createBindings() {
        IntegerConverter intConverter = new IntegerConverter();
        super.createBindings();
        addWrappedBinding(camera, "preferredResolution", cboSupportedResolutions, "selectedItem");
        addWrappedBinding(camera, "resizeWidth", resizeWidthTextField, "text", intConverter);
        addWrappedBinding(camera, "resizeHeight", resizeHeightTextField, "text", intConverter);
        addWrappedBinding(camera, "fps", fpsTextField, "text", intConverter);
        addWrappedBinding(camera, "username", usernameTextField, "text");
        addWrappedBinding(camera, "password", passwordTextField, "text");
        // Should always be last so that it doesn't trigger multiple camera reloads.
        addWrappedBinding(camera, "hostIP", ipTextField, "text");

        ComponentDecorators.decorateWithAutoSelect(fpsTextField);
        ComponentDecorators.decorateWithAutoSelect(ipTextField);
        ComponentDecorators.decorateWithAutoSelect(usernameTextField);
        ComponentDecorators.decorateWithAutoSelect(passwordTextField);
    }

    @Override
    protected void loadFromModel() {
        refreshResolutionList();
        super.loadFromModel();
    }

    @Override
    protected void saveToModel() {
        super.saveToModel();
        if (camera.isDirty()) {
            camera.setHostIP(camera.getHostIP());
        }
        
        refreshResolutionList();
    }

    private JLabel lblIP;
    private JTextField ipTextField;
    private JLabel lblUsername;
    private JTextField usernameTextField;
    private JLabel lblPassword;
    private JTextField passwordTextField;
    private JLabel lblFps;
    private JTextField fpsTextField;
    private JLabel lblSupportedResolutions;
    private JComboBox<String> cboSupportedResolutions;
    private JLabel lblResizeWidth;
    private JTextField resizeWidthTextField;
    private JLabel lblResizeHeight;
    private JTextField resizeHeightTextField;
    private JLabel lbluseFor_ip;
    private JLabel lbluseFor_un;
    private JLabel lbluseFor_pw;
    private JLabel lbluseFor_fps;
    private JLabel lbluseFor_res;
    private JLabel lbluseFor_rw;
    private JLabel lbluseFor_rh;
}
