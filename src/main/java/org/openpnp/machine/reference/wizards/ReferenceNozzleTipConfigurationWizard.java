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

package org.openpnp.machine.reference.wizards;

import java.util.HashSet;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.machine.reference.ReferenceNozzleTip;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import javax.swing.JCheckBox;
import javax.swing.UIManager;
import java.awt.Color;

public class ReferenceNozzleTipConfigurationWizard extends AbstractConfigurationWizard {
    private final ReferenceNozzleTip nozzleTip;
    private JPanel panelDwellTime;
    private JLabel lblPickDwellTime;
    private JLabel lblPlaceDwellTime;
    private JLabel lblDwellTime;
    private JTextField pickDwellTf;
    private JTextField placeDwellTf;

    private Set<org.openpnp.model.Package> compatiblePackages = new HashSet<>();
    private JPanel panel;
    private JLabel lblName;
    private JTextField nameTf;
    private JPanel panelPushAndDrag;
    private JComponent lblPushAndDragAllowed;
    private JLabel lblLowDiameter;
    private JTextField textFieldLowDiameter;
    private JCheckBox chckbxPushAndDragAllowed;


    public ReferenceNozzleTipConfigurationWizard(ReferenceNozzleTip nozzleTip) {
        this.nozzleTip = nozzleTip;
        
        panel = new JPanel();
        panel.setBorder(new TitledBorder(null, "Properties", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panel);
        panel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        lblName = new JLabel("Name");
        panel.add(lblName, "2, 2, right, default");
        
        nameTf = new JTextField();
        panel.add(nameTf, "4, 2, fill, default");
        nameTf.setColumns(10);
        
        panelDwellTime = new JPanel();
        panelDwellTime.setBorder(new TitledBorder(null, "Dwell Times", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelDwellTime);
        panelDwellTime.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
          
        lblPickDwellTime = new JLabel("Pick Dwell Time (ms)");
        panelDwellTime.add(lblPickDwellTime, "2, 2, right, default");
        
        pickDwellTf = new JTextField();
        panelDwellTime.add(pickDwellTf, "4, 2");
        pickDwellTf.setColumns(10);
        
        lblPlaceDwellTime = new JLabel("Place Dwell Time (ms)");
        panelDwellTime.add(lblPlaceDwellTime, "2, 4, right, default");
        
        placeDwellTf = new JTextField();
        panelDwellTime.add(placeDwellTf, "4, 4");
        placeDwellTf.setColumns(10);
        
        CellConstraints cc = new CellConstraints();
        lblDwellTime = new JLabel("Note: Total Dwell Time is the sum of Nozzle Dwell Time plus the Nozzle Tip Dwell Time.");
        panelDwellTime.add(lblDwellTime, cc.xywh(2, 6, 5, 1));

        panelPushAndDrag = new JPanel();
        panelPushAndDrag.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Push and Drag Usage", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        contentPanel.add(panelPushAndDrag);
        panelPushAndDrag.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        lblLowDiameter = new JLabel("Lowest Outside Diameter");
        lblLowDiameter.setToolTipText("Outside diameter of the nozzle tip at the lowest ~0.75mm.");
        panelPushAndDrag.add(lblLowDiameter, "2, 4, right, default");
        
        textFieldLowDiameter = new JTextField();
        panelPushAndDrag.add(textFieldLowDiameter, "4, 4");
        textFieldLowDiameter.setColumns(10);
        
        lblPushAndDragAllowed = new JLabel("Push & Drag allowed?");
        lblPushAndDragAllowed.setToolTipText("<html>\r\n<p>\r\nDetermines if the NozzleTip is allowed to be used <br/>\r\nfor pushing and dragging. \r\n</p><p>\r\nShould only be enabled for NozzleTips that are <br/>\r\nsturdy enough to take the lateral forces, including <br/>\r\nthe occasional snag. </p>\r\n</html>");
        panelPushAndDrag.add(lblPushAndDragAllowed, "2, 2, right, default");
        
        chckbxPushAndDragAllowed = new JCheckBox("");
        panelPushAndDrag.add(chckbxPushAndDragAllowed, "4, 2");
        
    }
    

    @Override
    public void createBindings() {
        IntegerConverter intConverter = new IntegerConverter();
        LengthConverter lengthConverter = new LengthConverter();

        addWrappedBinding(nozzleTip, "name", nameTf, "text");
        
        addWrappedBinding(nozzleTip, "pickDwellMilliseconds", pickDwellTf, "text", intConverter);
        addWrappedBinding(nozzleTip, "placeDwellMilliseconds", placeDwellTf, "text", intConverter);
        
        addWrappedBinding(nozzleTip, "diameterLow", textFieldLowDiameter, "text", lengthConverter);
        
        addWrappedBinding(nozzleTip, "pushAndDragAllowed", chckbxPushAndDragAllowed, "selected");
        
        ComponentDecorators.decorateWithAutoSelect(nameTf);
        
        ComponentDecorators.decorateWithAutoSelect(pickDwellTf);
        ComponentDecorators.decorateWithAutoSelect(placeDwellTf);
        
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldLowDiameter);

    }
}
