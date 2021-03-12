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

package org.openpnp.machine.neoden4.wizards;

import java.awt.Color;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.machine.neoden4.Neoden4Camera;
import org.openpnp.util.UiUtils;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import javax.swing.SwingConstants;

@SuppressWarnings("serial")
public class Neoden4CameraConfigurationWizard extends AbstractConfigurationWizard {
    private final Neoden4Camera camera;

    private JPanel panelGeneral;

    public Neoden4CameraConfigurationWizard(Neoden4Camera camera) {
        this.camera = camera;

        panelGeneral = new JPanel();
        contentPanel.add(panelGeneral);
        panelGeneral.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null),
                "General", TitledBorder.LEADING, TitledBorder.TOP, null));
        panelGeneral.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
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
            new RowSpec[] {
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

        lbluseFor_ip = new JLabel("(IP)");
        panelGeneral.add(lbluseFor_ip, "6, 2");
        
        lblCameraPort = new JLabel("Port");
        lblCameraPort.setHorizontalAlignment(SwingConstants.TRAILING);
        panelGeneral.add(lblCameraPort, "8, 2, right, default");
        
        cameraPortTextField = new JTextField();
        cameraPortTextField.setColumns(16);
        panelGeneral.add(cameraPortTextField, "10, 2");
        
        lblCameraID = new JLabel("Camera ID");
        lblCameraID.setHorizontalAlignment(SwingConstants.TRAILING);
        panelGeneral.add(lblCameraID, "2, 4, right, default");
        
        cameraIDTextField = new JTextField();
        cameraIDTextField.setColumns(16);
        panelGeneral.add(cameraIDTextField, "4, 4, fill, default");
        
        lblTimeout = new JLabel("Timeout");
        lblTimeout.setHorizontalAlignment(SwingConstants.TRAILING);
        panelGeneral.add(lblTimeout, "2, 6, right, default");
        
        timeoutMillisTextField = new JTextField();
        timeoutMillisTextField.setColumns(16);
        panelGeneral.add(timeoutMillisTextField, "4, 6, fill, default");
        
        lbluseForTimeout = new JLabel("(millisecs)");
        panelGeneral.add(lbluseForTimeout, "6, 6");
                
        panelImage = new JPanel();
        contentPanel.add(panelImage);
        panelImage.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), 
        		"Image settings", TitledBorder.LEADING, TitledBorder.TOP, null));
        panelImage.setLayout(new FormLayout(new ColumnSpec[] {
        		FormSpecs.RELATED_GAP_COLSPEC,
        		FormSpecs.DEFAULT_COLSPEC,
        		FormSpecs.RELATED_GAP_COLSPEC,
        		FormSpecs.DEFAULT_COLSPEC,
        		FormSpecs.RELATED_GAP_COLSPEC,
        		FormSpecs.RELATED_GAP_COLSPEC,
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
        		FormSpecs.RELATED_GAP_ROWSPEC,
        		FormSpecs.RELATED_GAP_ROWSPEC,
        		FormSpecs.DEFAULT_ROWSPEC,
        		FormSpecs.RELATED_GAP_ROWSPEC,
        		FormSpecs.DEFAULT_ROWSPEC,}));

        lblImageWidth = new JLabel("Width");
        lblImageWidth.setHorizontalAlignment(SwingConstants.TRAILING);
        panelImage.add(lblImageWidth, "2, 2, right, default");

        imageWidthTextField = new JTextField();
        panelImage.add(imageWidthTextField, "4, 2");
        imageWidthTextField.setColumns(16);
        
        lblShiftX = new JLabel("Shift X");
        panelImage.add(lblShiftX, "8, 2, right, default");
        
        shiftXTextField = new JTextField();
        panelImage.add(shiftXTextField, "10, 2, fill, default");
        shiftXTextField.setColumns(10);
        
        lblXPixels = new JLabel("(pixels)");
        panelImage.add(lblXPixels, "12, 2");

        lblImageHeight = new JLabel("Height");
        lblImageHeight.setHorizontalAlignment(SwingConstants.TRAILING);
        panelImage.add(lblImageHeight, "2, 4, right, default");

        imageHeightTextField = new JTextField();
        panelImage.add(imageHeightTextField, "4, 4");
        imageHeightTextField.setColumns(16);
        
        lblShiftY = new JLabel("Shift Y");
        panelImage.add(lblShiftY, "8, 4, right, default");
        
        shiftYTextField = new JTextField();
        panelImage.add(shiftYTextField, "10, 4, fill, default");
        shiftYTextField.setColumns(10);
        
        lblYPixels = new JLabel("(pixels)");
        panelImage.add(lblYPixels, "12, 4");
        
        lblExposure = new JLabel("Exposure");
        lblExposure.setHorizontalAlignment(SwingConstants.TRAILING);
        panelImage.add(lblExposure, "2, 8, right, default");
        
        cameraExposureTextField = new JTextField();
        cameraExposureTextField.setColumns(16);
        panelImage.add(cameraExposureTextField, "4, 8, fill, default");
        
        lblGain = new JLabel("Gain");
        lblGain.setHorizontalAlignment(SwingConstants.TRAILING);
        panelImage.add(lblGain, "2, 10, right, default");
        
        cameraGainTextField = new JTextField();
        cameraGainTextField.setColumns(16);
        panelImage.add(cameraGainTextField, "4, 10, fill, default");
    }
    
    @Override
    public void createBindings() {
        IntegerConverter intConverter = new IntegerConverter();
        addWrappedBinding(camera, "hostPort", cameraPortTextField, "text", intConverter);
        addWrappedBinding(camera, "cameraId", cameraIDTextField, "text", intConverter);
        addWrappedBinding(camera, "width", imageWidthTextField, "text", intConverter);
        addWrappedBinding(camera, "height", imageHeightTextField, "text", intConverter);
        addWrappedBinding(camera, "timeout", timeoutMillisTextField, "text", intConverter);
        addWrappedBinding(camera, "exposure", cameraExposureTextField, "text", intConverter);
        addWrappedBinding(camera, "gain", cameraGainTextField, "text", intConverter);
        addWrappedBinding(camera, "shiftX", shiftXTextField, "text", intConverter);
        addWrappedBinding(camera, "shiftY", shiftYTextField, "text", intConverter);
    
        // Should always be last so that it doesn't trigger multiple camera reloads.
        addWrappedBinding(camera, "hostIP", ipTextField, "text");

        //ComponentDecorators.decorateWithAutoSelect(fpsTextField);
        //ComponentDecorators.decorateWithAutoSelect(ipTextField);
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
    private JPanel panelImage;
    private JLabel lblImageWidth;
    private JLabel lblImageHeight;
    private JTextField imageWidthTextField;
    private JTextField imageHeightTextField;
    private JLabel lblCameraPort;
    private JTextField cameraPortTextField;
    private JLabel lblCameraID;
    private JTextField cameraIDTextField;
    private JLabel lblShiftX;
    private JLabel lblShiftY;
    private JTextField shiftXTextField;
    private JTextField shiftYTextField;
    private JLabel lblXPixels;
    private JLabel lblYPixels;
    private JLabel lblTimeout;
    private JTextField timeoutMillisTextField;
    private JLabel lbluseForTimeout;
    private JLabel lblExposure;
    private JLabel lblGain;
    private JTextField cameraExposureTextField;
    private JTextField cameraGainTextField;
}
