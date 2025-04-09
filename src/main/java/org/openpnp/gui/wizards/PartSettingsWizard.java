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

package org.openpnp.gui.wizards;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.Translations;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.model.Part;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class PartSettingsWizard extends AbstractConfigurationWizard {
    private final Part part;
    private JPanel pickConditionsPanel;
    private JLabel lblNewLabel;
    private JTextField textFieldPickRetryCount;

    public PartSettingsWizard(Part part) {
        super();
        this.part = part;
        createUi();
    }
    
    private void createUi() {
        pickConditionsPanel = new JPanel();
        pickConditionsPanel.setBorder(new TitledBorder(null, Translations.getString(
                "PartSettingsWizard.pickConditionsPanel.Border.title"), //$NON-NLS-1$
                TitledBorder.LEADING, TitledBorder.TOP, null));
        contentPanel.add(pickConditionsPanel);
        pickConditionsPanel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        lblNewLabel = new JLabel(Translations.getString(
                "PartSettingsWizard.pickConditionsPanel.pickRetryCountLabel.text")); //$NON-NLS-1$
        pickConditionsPanel.add(lblNewLabel, "2, 2, right, default");
        
        textFieldPickRetryCount = new JTextField();
        pickConditionsPanel.add(textFieldPickRetryCount, "4, 2, left, default");
        textFieldPickRetryCount.setColumns(10);
    }
    
    @Override
    public void createBindings() {
        IntegerConverter intConverter = new IntegerConverter();
        bind(UpdateStrategy.READ_WRITE, part, "pickRetryCount", textFieldPickRetryCount, "text", intConverter); 
        
        ComponentDecorators.decorateWithAutoSelect(textFieldPickRetryCount);
    }
}
