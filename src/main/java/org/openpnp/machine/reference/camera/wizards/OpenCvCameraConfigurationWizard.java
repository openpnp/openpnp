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
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.machine.reference.camera.OpenCvCamera;
import org.openpnp.machine.reference.camera.OpenCvCamera.OpenCvCaptureProperty;
import org.openpnp.machine.reference.camera.OpenCvCamera.OpenCvCapturePropertyValue;
import org.openpnp.machine.reference.wizards.ReferenceCameraConfigurationWizard;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

/*
 * TODO: newly added properties don't get set on first apply
 */
@SuppressWarnings("serial")
public class OpenCvCameraConfigurationWizard extends ReferenceCameraConfigurationWizard {
    private final OpenCvCamera camera;

    private JPanel panelGeneral;
    
    private List<OpenCvCapturePropertyValue> properties = new ArrayList<>();
    
    private boolean propertyChanging = false;
    private boolean dirty = false;

    public OpenCvCameraConfigurationWizard(OpenCvCamera camera) {
        super(camera);

        this.camera = camera;

        panelGeneral = new JPanel();
        contentPanel.add(panelGeneral);
        panelGeneral.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null),
                "General", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        panelGeneral.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblDeviceId = new JLabel("USB Device Index");
        panelGeneral.add(lblDeviceId, "2, 2, right, default");

        comboBoxDeviceIndex = new JComboBox();
        for (int i = 0; i < 10; i++) {
            comboBoxDeviceIndex.addItem(new Integer(i));
        }
        panelGeneral.add(comboBoxDeviceIndex, "4, 2, left, default");

        lbluseFor_di = new JLabel("(physical camera to use)");
        panelGeneral.add(lbluseFor_di, "6, 2");

        lblFps = new JLabel("FPS");
        panelGeneral.add(lblFps, "2, 4, right, default");

        fpsTextField = new JTextField();
        panelGeneral.add(fpsTextField, "4, 4");
        fpsTextField.setColumns(10);

        lbluseFor_fps = new JLabel("(refresh rate)");
        panelGeneral.add(lbluseFor_fps, "6, 4");

        lblPreferredWidth = new JLabel("Preferred Width");
        panelGeneral.add(lblPreferredWidth, "2, 6, right, default");

        textFieldPreferredWidth = new JTextField();
        panelGeneral.add(textFieldPreferredWidth, "4, 6, fill, default");
        textFieldPreferredWidth.setColumns(10);

        lbluseFor_w = new JLabel("(Use 0 for native resolution)");
        panelGeneral.add(lbluseFor_w, "6, 6");

        lblPreferredHeight = new JLabel("Preferred Height");
        panelGeneral.add(lblPreferredHeight, "2, 8, right, default");

        textFieldPreferredHeight = new JTextField();
        panelGeneral.add(textFieldPreferredHeight, "4, 8, fill, default");
        textFieldPreferredHeight.setColumns(10);

        lbluseFor_h = new JLabel("(Use 0 for native resolution)");
        panelGeneral.add(lbluseFor_h, "6, 8");

        panel = new JPanel();
        panel.setBorder(new TitledBorder(null, "Properties (Experimental)", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        contentPanel.add(panel);
        panel.setLayout(new FormLayout(new ColumnSpec[] {
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
                FormSpecs.DEFAULT_ROWSPEC,}));

        lblProperty = new JLabel("Property");
        panel.add(lblProperty, "2, 2");

        lblValue = new JLabel("Value");
        panel.add(lblValue, "4, 2");

        propertyCb = new JComboBox(OpenCvCaptureProperty.values());
        panel.add(propertyCb, "2, 4");

        propertyValueTf = new JTextField();
        panel.add(propertyValueTf, "4, 4");
        propertyValueTf.setColumns(10);
        
        readPropertyValueBtn = new JButton(readPropertyValueAction);
        readPropertyValueBtn.setHideActionText(true);
        panel.add(readPropertyValueBtn, "6, 4");
        
        setBeforeOpenCk = new JCheckBox("Set Before Open");
        panel.add(setBeforeOpenCk, "8, 4");
        
        setAfterOpenCk = new JCheckBox("Set After Open");
        panel.add(setAfterOpenCk, "10, 4");

        propertyCb.addItemListener(e -> propertyChanged());
        propertyValueTf.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                if (propertyChanging) {
                    return;
                }
                propertyValueChanged();
            }

            public void removeUpdate(DocumentEvent e) {
                if (propertyChanging) {
                    return;
                }
                propertyValueChanged();
            }

            public void insertUpdate(DocumentEvent e) {
                if (propertyChanging) {
                    return;
                }
                propertyValueChanged();
            }
        });
        setBeforeOpenCk.addChangeListener(e -> {
            if (propertyChanging) {
                return;
            }
            OpenCvCapturePropertyValue pv = getPropertyValue((OpenCvCaptureProperty) propertyCb.getSelectedItem());
            if (pv == null) {
                return;
            }
            pv.setBeforeOpen = setBeforeOpenCk.isSelected();
            notifyChange();
        });
        setAfterOpenCk.addChangeListener(e -> {
            if (propertyChanging) {
                return;
            }
            OpenCvCapturePropertyValue pv = getPropertyValue((OpenCvCaptureProperty) propertyCb.getSelectedItem());
            if (pv == null) {
                return;
            }
            pv.setAfterOpen = setAfterOpenCk.isSelected();
            notifyChange();
        });
        
        // preload the properties
        properties = new ArrayList<>(camera.getProperties());
        propertyChanged();
    }

    private void propertyChanged() {
        propertyChanging = true;
        OpenCvCapturePropertyValue pv = getPropertyValue((OpenCvCaptureProperty) propertyCb.getSelectedItem()); 
        propertyValueTf.setText(pv == null ? "" : "" + pv.value);
        setBeforeOpenCk.setSelected(pv == null ? false : pv.setBeforeOpen);
        setAfterOpenCk.setSelected(pv == null ? false : pv.setAfterOpen);
        propertyChanging = false;
    }

    private void propertyValueChanged() {
        String text = propertyValueTf.getText();
        Double value = null;
        try {
            value = Double.valueOf(text);
        }
        catch (Exception e) {
            
        }
        setPropertyValue((OpenCvCaptureProperty) propertyCb.getSelectedItem(), value);
        OpenCvCapturePropertyValue pv = getPropertyValue((OpenCvCaptureProperty) propertyCb.getSelectedItem());
        if (pv == null) {
            return;
        }
        pv.setBeforeOpen = setBeforeOpenCk.isSelected();
        pv.setAfterOpen = setAfterOpenCk.isSelected();
    }
    
    private OpenCvCapturePropertyValue getPropertyValue(OpenCvCaptureProperty property) {
        for (OpenCvCapturePropertyValue pv : properties) {
            if (pv.property == property) {
                return pv;
            }
        }
        return null;
    }

    private void setPropertyValue(OpenCvCaptureProperty property, Double value) {
        OpenCvCapturePropertyValue pv = null;
        for (OpenCvCapturePropertyValue pv_ : properties) {
            if (pv_.property == property) {
                pv = pv_;
            }
        }
        // If the value is null, remove the property.
        if (value == null && pv != null) {
            properties.remove(pv);
            dirty = true;
            notifyChange();
            return;
        }
        // Otherwise, if the property doesn't exist then create it.
        if (pv == null) {
            pv = new OpenCvCapturePropertyValue();
            pv.property = property;
            properties.add(pv);
        }
        // And set the value.
        pv.value = value;
        dirty = true;
        notifyChange();
    }
    
    @Override
    public void createBindings() {
        IntegerConverter intConverter = new IntegerConverter();
        super.createBindings();
        addWrappedBinding(camera, "preferredWidth", textFieldPreferredWidth, "text", intConverter);
        addWrappedBinding(camera, "preferredHeight", textFieldPreferredHeight, "text",
                intConverter);
        addWrappedBinding(camera, "fps", fpsTextField, "text", intConverter);
        // Should always be last so that it doesn't trigger multiple camera reloads.
        addWrappedBinding(camera, "deviceIndex", comboBoxDeviceIndex, "selectedItem");

        ComponentDecorators.decorateWithAutoSelect(textFieldPreferredWidth);
        ComponentDecorators.decorateWithAutoSelect(textFieldPreferredHeight);
        ComponentDecorators.decorateWithAutoSelect(fpsTextField);
        ComponentDecorators.decorateWithAutoSelect(propertyValueTf);
    }
    
    @Override
    protected void loadFromModel() {
        this.properties = new ArrayList<>(camera.getProperties());
        propertyChanged();
        dirty = false;
        super.loadFromModel();
    }

    @Override
    protected void saveToModel() {
        super.saveToModel();
        camera.getProperties().clear();
        camera.getProperties().addAll(this.properties);
        if (camera.isDirty() || dirty) {
            camera.setDeviceIndex(camera.getDeviceIndex());
            dirty = false;
        }
    }
    
    public Action readPropertyValueAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.refresh);
            putValue(NAME, "Read Property Value");
            putValue(SHORT_DESCRIPTION, "Read the current property value directly from the camera.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            propertyValueTf.setText("" + camera.getOpenCvCapturePropertyValue((OpenCvCaptureProperty) propertyCb.getSelectedItem()));
        }
    };



    private JComboBox comboBoxDeviceIndex;
    private JLabel lblPreferredWidth;
    private JLabel lblPreferredHeight;
    private JTextField textFieldPreferredWidth;
    private JTextField textFieldPreferredHeight;
    private JLabel lbluseFor_di;
    private JLabel lbluseFor_w;
    private JLabel lbluseFor_h;
    private JLabel lblFps;
    private JTextField fpsTextField;
    private JLabel lbluseFor_fps;
    private JPanel panel;
    private JLabel lblProperty;
    private JLabel lblValue;
    private JComboBox propertyCb;
    private JTextField propertyValueTf;
    private JCheckBox setBeforeOpenCk;
    private JCheckBox setAfterOpenCk;
    private JButton readPropertyValueBtn;
}
