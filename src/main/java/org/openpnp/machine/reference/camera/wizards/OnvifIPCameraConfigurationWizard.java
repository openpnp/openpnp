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

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

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
                FormSpecs.DEFAULT_ROWSPEC,}));

        lblIP = new JLabel("Network IP");
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

        lblPreferredWidth = new JLabel("Preferred Width");
        panelGeneral.add(lblPreferredWidth, "2, 10, right, default");

        textFieldPreferredWidth = new JTextField();
        panelGeneral.add(textFieldPreferredWidth, "4, 10, fill, default");
        textFieldPreferredWidth.setColumns(10);

        lbluseFor_w = new JLabel("(Use 0 for highest resolution)");
        panelGeneral.add(lbluseFor_w, "6, 10");

        lblPreferredHeight = new JLabel("Preferred Height");
        panelGeneral.add(lblPreferredHeight, "2, 12, right, default");

        textFieldPreferredHeight = new JTextField();
        panelGeneral.add(textFieldPreferredHeight, "4, 12, fill, default");
        textFieldPreferredHeight.setColumns(10);

        lbluseFor_h = new JLabel("(Use 0 for highest resolution)");
        panelGeneral.add(lbluseFor_h, "6, 12");
    }

    @Override
    public void createBindings() {
        IntegerConverter intConverter = new IntegerConverter();
        super.createBindings();
        addWrappedBinding(camera, "preferredWidth", textFieldPreferredWidth, "text", intConverter);
        addWrappedBinding(camera, "preferredHeight", textFieldPreferredHeight, "text",
                intConverter);
        addWrappedBinding(camera, "fps", fpsTextField, "text", intConverter);
        addWrappedBinding(camera, "username", usernameTextField, "text");
        addWrappedBinding(camera, "password", passwordTextField, "text");
        // Should always be last so that it doesn't trigger multiple camera reloads.
        addWrappedBinding(camera, "hostIP", ipTextField, "text");

        ComponentDecorators.decorateWithAutoSelect(textFieldPreferredWidth);
        ComponentDecorators.decorateWithAutoSelect(textFieldPreferredHeight);
        ComponentDecorators.decorateWithAutoSelect(fpsTextField);
        ComponentDecorators.decorateWithAutoSelect(ipTextField);
        ComponentDecorators.decorateWithAutoSelect(usernameTextField);
        ComponentDecorators.decorateWithAutoSelect(passwordTextField);
    }

    @Override
    protected void saveToModel() {
        super.saveToModel();
        if (camera.isDirty()) {
            camera.setHostIP(camera.getHostIP());
        }
    }

    private JLabel lblPreferredWidth;
    private JLabel lblPreferredHeight;
    private JTextField textFieldPreferredWidth;
    private JTextField textFieldPreferredHeight;
    private JLabel lbluseFor_ip;
    private JLabel lbluseFor_un;
    private JLabel lbluseFor_pw;
    private JLabel lbluseFor_w;
    private JLabel lbluseFor_h;
    private JLabel lblIP;
    private JTextField ipTextField;
    private JLabel lblUsername;
    private JTextField usernameTextField;
    private JLabel lblPassword;
    private JTextField passwordTextField;
    private JLabel lblFps;
    private JTextField fpsTextField;
}
