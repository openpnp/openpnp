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

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.machine.reference.ReferenceNozzleTip;
import org.openpnp.model.Configuration;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class ReferenceNozzleTipConfigurationWizard extends AbstractConfigurationWizard {
    private final ReferenceNozzleTip nozzleTip;
    private JPanel panelPickAndPlace;
    private JLabel lblPickDwellTime;
    private JLabel lblPlaceDwellTime;
    private JTextArea lblDwellTime;
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
    private JPanel panelPartDimensions;
    private JLabel lblMaxPartHeight;
    private JTextField maxPartHeight;
    private JLabel lblMaxPartDiameter;
    private JTextField maxPartDiameter;
    private JLabel lblMinPartDiameter;
    private JTextField minPartDiameter;
    private JLabel lblMaxPickTolerance;
    private JTextField maxPickTolerance;
    private JLabel lblPlaceBlowoffLevel;
    private JTextField placeBlowOffLevel;


    public ReferenceNozzleTipConfigurationWizard(ReferenceNozzleTip nozzleTip) {
        this.nozzleTip = nozzleTip;
        
        panel = new JPanel();
        panel.setBorder(new TitledBorder(null, "Properties", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panel);
        panel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
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
        
        panelPickAndPlace = new JPanel();
        panelPickAndPlace.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Pick & Place", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        contentPanel.add(panelPickAndPlace);
        panelPickAndPlace.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("min(100dlu;pref):grow"),},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
          
        lblPickDwellTime = new JLabel("Pick Dwell Time (ms)");
        panelPickAndPlace.add(lblPickDwellTime, "2, 2, right, default");
        
        pickDwellTf = new JTextField();
        panelPickAndPlace.add(pickDwellTf, "4, 2");
        pickDwellTf.setColumns(10);
        lblDwellTime = new JTextArea ("Note: Total Dwell Time is the sum of Nozzle Dwell Time plus the Nozzle Tip Dwell Time.");
        lblDwellTime.setBackground(UIManager.getColor("Label.background"));
        lblDwellTime.setForeground(UIManager.getColor("Label.foreground"));
        lblDwellTime.setFont(UIManager.getFont("Label.font"));
        lblDwellTime.setWrapStyleWord(true);
        lblDwellTime.setLineWrap(true);
        lblDwellTime.setEditable(false);
        panelPickAndPlace.add(lblDwellTime, "6, 2, 1, 3, fill, center");
        
        lblPlaceDwellTime = new JLabel("Place Dwell Time (ms)");
        panelPickAndPlace.add(lblPlaceDwellTime, "2, 4, right, default");
        
        placeDwellTf = new JTextField();
        panelPickAndPlace.add(placeDwellTf, "4, 4");
        placeDwellTf.setColumns(10);
        
        CellConstraints cc = new CellConstraints();
        
        lblPlaceBlowoffLevel = new JLabel("Place Blow-Off Level");
        lblPlaceBlowoffLevel.setToolTipText("Default placement blow-off level, if none is given on the Package. ");
        panelPickAndPlace.add(lblPlaceBlowoffLevel, "2, 8, right, default");
        
        placeBlowOffLevel = new JTextField();
        panelPickAndPlace.add(placeBlowOffLevel, "4, 8, fill, default");
        placeBlowOffLevel.setColumns(10);

        panelPushAndDrag = new JPanel();
        panelPushAndDrag.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Push and Drag Usage", TitledBorder.LEADING, TitledBorder.TOP, null));
        contentPanel.add(panelPushAndDrag);
        panelPushAndDrag.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
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
        
        lblLowDiameter = new JLabel("Outside Diameter");
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

        panelPartDimensions= new JPanel();
        panelPartDimensions.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"),
                "Part Dimensions", TitledBorder.LEADING, TitledBorder.TOP, null));
        contentPanel.add(panelPartDimensions);

        panelPartDimensions.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
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
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        lblMinPartDiameter = new JLabel("Min. Part Diameter");
        lblMinPartDiameter.setToolTipText("<html>\nMinimum part diameter, to be picked with this the nozzle tip.<br/>\nNote, the <strong>Minimum Part Diameter</strong> minus two times the <strong>Pick Tolerance</strong><br/>\ndetermines the minimum <em>inner</em> diameter of the nozzle tip that<br/>\nis always considered covered by the part. This inner diameter is ignored<br/>\nin the Background Calibration key color analysis and diagnostics.\n</html>");
        panelPartDimensions.add(lblMinPartDiameter, "2, 2, right, default");
        
        minPartDiameter = new JTextField();
        panelPartDimensions.add(minPartDiameter, "4, 2, fill, default");
        minPartDiameter.setColumns(10);

        lblMaxPartDiameter = new JLabel("Max. Part Diameter");
        lblMaxPartDiameter.setToolTipText(
                "<html>\nMaximum diameter/diagonal of parts picked with this nozzle tip.<br/>\n<br/>\nNote, when using Vision Compositing (bottom vision multi-shot), this does<br/>\nnot limit the parts size, but rather the size of a single shot (mask diameter).\n</html>\n");
        panelPartDimensions.add(lblMaxPartDiameter, "2, 4, right, default");

        maxPartDiameter = new JTextField();
        panelPartDimensions.add(maxPartDiameter, "4, 4, fill, default");
        maxPartDiameter.setColumns(10);

        lblMaxPartHeight = new JLabel("Max. Part Height");
        lblMaxPartHeight.setToolTipText(
                "Maximum part heights picked with this nozzle tip. Used for dynamic safe Z, if part height is unknown.");
        panelPartDimensions.add(lblMaxPartHeight, "2, 6, right, default");

        maxPartHeight = new JTextField();
        panelPartDimensions.add(maxPartHeight, "4, 6, fill, default");
        maxPartHeight.setColumns(10);
        
        lblMaxPickTolerance = new JLabel("Max. Pick Tolerance");
        lblMaxPickTolerance.setToolTipText("<html>\nMaximum assumed pick tolerance allowed with this nozzle tip.<br/>\nThis determines how far away from the nominal location a detected <br/>\nBottom Vision alignment position is accepted. It also reduces the <br/>\ncomputation time of some vision operations by limiting the search range.\n</html>\n\n");
        panelPartDimensions.add(lblMaxPickTolerance, "2, 8, right, default");
        
        maxPickTolerance = new JTextField();
        maxPickTolerance.setToolTipText("");
        panelPartDimensions.add(maxPickTolerance, "4, 8, fill, default");
        maxPickTolerance.setColumns(10);
    }

    @Override
    public void createBindings() {
        IntegerConverter intConverter = new IntegerConverter();
        LengthConverter lengthConverter = new LengthConverter();
        DoubleConverter doubleConverter = new DoubleConverter(Configuration.get().getLengthDisplayFormat());
        
        addWrappedBinding(nozzleTip, "name", nameTf, "text");

        addWrappedBinding(nozzleTip, "pickDwellMilliseconds", pickDwellTf, "text", intConverter);
        addWrappedBinding(nozzleTip, "placeDwellMilliseconds", placeDwellTf, "text", intConverter);
        addWrappedBinding(nozzleTip, "placeBlowOffLevel", placeBlowOffLevel, "text", doubleConverter);

        addWrappedBinding(nozzleTip, "diameterLow", textFieldLowDiameter, "text", lengthConverter);

        addWrappedBinding(nozzleTip, "pushAndDragAllowed", chckbxPushAndDragAllowed, "selected");

        addWrappedBinding(nozzleTip, "minPartDiameter", minPartDiameter, "text", lengthConverter);
        addWrappedBinding(nozzleTip, "maxPartDiameter", maxPartDiameter, "text", lengthConverter);
        addWrappedBinding(nozzleTip, "maxPartHeight", maxPartHeight, "text", lengthConverter);
        addWrappedBinding(nozzleTip, "maxPickTolerance", maxPickTolerance, "text", lengthConverter);

        ComponentDecorators.decorateWithAutoSelect(nameTf);

        ComponentDecorators.decorateWithAutoSelect(pickDwellTf);
        ComponentDecorators.decorateWithAutoSelect(placeDwellTf);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldLowDiameter);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(minPartDiameter);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(maxPartDiameter);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(maxPartHeight);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(maxPickTolerance);
    }
}
