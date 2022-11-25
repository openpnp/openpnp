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

package org.openpnp.gui;

import javax.swing.JPanel;
import javax.swing.BoxLayout;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.Translations;
import org.openpnp.gui.components.ComponentDecorators;

import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.layout.FormSpecs;
import javax.swing.JLabel;
import javax.swing.JTextField;
import org.openpnp.model.Package;

@SuppressWarnings("serial")
public class PackageSettingsPanel extends JPanel {
    private final org.openpnp.model.Package pkg;
    private JPanel vacuumBlowOffPanel;
    private JLabel lblNewLabel;
    private JTextField vacuumLevel;
    private JLabel lblBlowOffLevel;
    private JTextField blowOffLevel;

    public PackageSettingsPanel(org.openpnp.model.Package pkg) {
        this.pkg = pkg;
        createUi();
        initDataBindings();
    }
    
    private void createUi() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        
        vacuumBlowOffPanel = new JPanel();
        vacuumBlowOffPanel.setBorder(new TitledBorder(null, Translations.getString(
                "PackageSettingsPanel.Border.title"),
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        add(vacuumBlowOffPanel);
        vacuumBlowOffPanel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        lblNewLabel = new JLabel(Translations.getString("PackageSettingsPanel.VacuumLevelLabel.text"));
        vacuumBlowOffPanel.add(lblNewLabel, "2, 2, right, default");
        
        vacuumLevel = new JTextField();
        vacuumBlowOffPanel.add(vacuumLevel, "4, 2, left, default");
        vacuumLevel.setColumns(10);
        
        lblBlowOffLevel = new JLabel(Translations.getString("PackageSettingsPanel.BlowOffLevelLabel.text"));
        vacuumBlowOffPanel.add(lblBlowOffLevel, "2, 4, right, default");
        
        blowOffLevel = new JTextField();
        vacuumBlowOffPanel.add(blowOffLevel, "4, 4, left, default");
        blowOffLevel.setColumns(10);
    }
    protected void initDataBindings() {
        BeanProperty<Package, Double> packageBeanProperty = BeanProperty.create("pickVacuumLevel");
        BeanProperty<JTextField, String> jTextFieldBeanProperty = BeanProperty.create("text");
        AutoBinding<Package, Double, JTextField, String> autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, pkg, packageBeanProperty, vacuumLevel, jTextFieldBeanProperty);
        autoBinding.bind();
        //
        BeanProperty<Package, Double> packageBeanProperty_1 = BeanProperty.create("placeBlowOffLevel");
        BeanProperty<JTextField, String> jTextFieldBeanProperty_1 = BeanProperty.create("text");
        AutoBinding<Package, Double, JTextField, String> autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, pkg, packageBeanProperty_1, blowOffLevel, jTextFieldBeanProperty_1);
        autoBinding_1.bind();
        
        ComponentDecorators.decorateWithAutoSelect(vacuumLevel);
        ComponentDecorators.decorateWithAutoSelect(blowOffLevel);
    }
}
