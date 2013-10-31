/*
 	Copyright (C) 2013 Richard Spelling <openpnp@chebacco.com>
 	
 	This file is part of OpenPnP.
 	
	OpenPnP is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenPnP is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenPnP.  If not, see <http://www.gnu.org/licenses/>.
 	
 	For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.machine.zippy;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.BufferedImageIconConverter;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.machine.reference.feeder.wizards.ReferenceTapeFeederConfigurationWizard;
import org.openpnp.machine.zippy.ZippyNozzleTip;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

public class ZippyNozzleTipConfigurationWizard extends
        AbstractConfigurationWizard {
    private final ZippyNozzleTip zippynozzletip;

    private JTextField textFieldMirrorStartX;
    private JTextField textFieldMirrorStartY;
    private JTextField textFieldMirrorStartZ;
    private JTextField textFieldMirrorMidX;
    private JTextField textFieldMirrorMidY;
    private JTextField textFieldMirrorMidZ;
    private JTextField textFieldMirrorEndX;
    private JTextField textFieldMirrorEndY;
    private JTextField textFieldMirrorEndZ;
    private LocationButtonsPanel locationButtonsPanelMirrorStart;
    private LocationButtonsPanel locationButtonsPanelMirrorMid;
    private LocationButtonsPanel locationButtonsPanelMirrorEnd;

    private JTextField textFieldChangerStartX;
    private JTextField textFieldChangerStartY;
    private JTextField textFieldChangerStartZ;
    private JTextField textFieldChangerMidX;
    private JTextField textFieldChangerMidY;
    private JTextField textFieldChangerMidZ;
    private JTextField textFieldChangerEndX;
    private JTextField textFieldChangerEndY;
    private JTextField textFieldChangerEndZ;
    private LocationButtonsPanel locationButtonsPanelChangerStart;
    private LocationButtonsPanel locationButtonsPanelChangerMid;
    private LocationButtonsPanel locationButtonsPanelChangerEnd;
   
    private JTextField locationX;
    private JTextField locationY;
    private JTextField locationZ;
    private JPanel panelOffsets;
    private JPanel panelMirrorWaypoints;
    private JPanel panelChangerWaypoints;

    private JPanel panelVision;
    private JPanel panelLocations;
    private JCheckBox chckbxVisionEnabled;
    private JPanel panelVisionEnabled;
    private JPanel panelTemplate;
    private JLabel labelTemplateImage;
    private JButton btnChangeTemplateImage;
    private JSeparator separator;
    private JPanel panelVisionTemplateAndAoe;
    private JPanel panelAoE;
    private JLabel lblX_1;
    private JLabel lblY_1;
    private JTextField textFieldAoiX;
    private JTextField textFieldAoiY;
    private JTextField textFieldAoiWidth;
    private JTextField textFieldAoiHeight;
    private LocationButtonsPanel locationButtonsPanelFeedStart;
    private LocationButtonsPanel locationButtonsPanelFeedEnd;
    private JLabel lblWidth;
    private JLabel lblHeight;
    private JButton btnChangeAoi;
    private JButton btnCancelChangeAoi;
    private JPanel panel;
    private JButton btnCancelChangeTemplateImage;

    public ZippyNozzleTipConfigurationWizard(ZippyNozzleTip zippynozzletip) {
        this.zippynozzletip = zippynozzletip;

        //setup panel for nozzle offsets (crookedness)
        JPanel panelFields = new JPanel();
        panelFields.setLayout(new BoxLayout(panelFields, BoxLayout.Y_AXIS));

        panelOffsets = new JPanel();
        panelFields.add(panelOffsets);
        panelOffsets.setBorder(new TitledBorder(new EtchedBorder(
                EtchedBorder.LOWERED, null, null), "Offsets",
                TitledBorder.LEADING, TitledBorder.TOP, null,
                new Color(0, 0, 0)));
        panelOffsets.setLayout(new FormLayout(
                new ColumnSpec[] { FormFactory.RELATED_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.RELATED_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.RELATED_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.RELATED_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC, }, new RowSpec[] {
                        FormFactory.RELATED_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.RELATED_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC, }));

        JLabel lblX = new JLabel("X");
        panelOffsets.add(lblX, "2, 2");

        JLabel lblY = new JLabel("Y");
        panelOffsets.add(lblY, "4, 2");

        JLabel lblZ = new JLabel("Z");
        panelOffsets.add(lblZ, "6, 2");

        locationX = new JTextField();
        panelOffsets.add(locationX, "2, 4");
        locationX.setColumns(5);

        locationY = new JTextField();
        panelOffsets.add(locationY, "4, 4");
        locationY.setColumns(5);

        locationZ = new JTextField();
        panelOffsets.add(locationZ, "6, 4");
        locationZ.setColumns(5);
        
        // setup panel for mirror waypoints
        panelMirrorWaypoints = new JPanel();
//        panelFields.add(panelMirrorWaypoints);
        panelMirrorWaypoints.setBorder(new TitledBorder(null, "Mirror Waypoints",
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        panelMirrorWaypoints
                .setLayout(new FormLayout(new ColumnSpec[] {
                        FormFactory.RELATED_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.RELATED_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.RELATED_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.RELATED_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.RELATED_GAP_COLSPEC,
                        ColumnSpec.decode("left:default:grow"), },
                        new RowSpec[] { FormFactory.RELATED_GAP_ROWSPEC,
		                        FormFactory.DEFAULT_ROWSPEC,
		                        FormFactory.RELATED_GAP_ROWSPEC,
		                        FormFactory.DEFAULT_ROWSPEC,
		                        FormFactory.RELATED_GAP_ROWSPEC,
                                FormFactory.DEFAULT_ROWSPEC,
                                FormFactory.RELATED_GAP_ROWSPEC,
                                FormFactory.DEFAULT_ROWSPEC,
                                FormFactory.RELATED_GAP_ROWSPEC,
                                FormFactory.DEFAULT_ROWSPEC, }));

        JLabel lblmwX = new JLabel("X");
        panelMirrorWaypoints.add(lblmwX, "4, 4");

        JLabel lblmwY = new JLabel("Y");
        panelMirrorWaypoints.add(lblmwY, "6, 4");

        JLabel lblmwZ = new JLabel("Z");
        panelMirrorWaypoints.add(lblmwZ, "8, 4");

        //start location
        JLabel lblMirrorStartLocation = new JLabel("Mirror Start Location");
        panelMirrorWaypoints.add(lblMirrorStartLocation, "2, 6, right, default");

        textFieldMirrorStartX = new JTextField();
        panelMirrorWaypoints.add(textFieldMirrorStartX, "4, 6");
        textFieldMirrorStartX.setColumns(8);

        textFieldMirrorStartY = new JTextField();
        panelMirrorWaypoints.add(textFieldMirrorStartY, "6, 6");
        textFieldMirrorStartY.setColumns(8);

        textFieldMirrorStartZ = new JTextField();
        panelMirrorWaypoints.add(textFieldMirrorStartZ, "8, 6");
        textFieldMirrorStartZ.setColumns(8);

        locationButtonsPanelMirrorStart = new LocationButtonsPanel(
                textFieldMirrorStartX, textFieldMirrorStartY, textFieldMirrorStartZ,
                null);
        panelMirrorWaypoints.add(locationButtonsPanelMirrorStart, "10, 6");
   
       //mid location
        JLabel lblMirrorMidLocation = new JLabel("Mirror Middle Location");
        panelMirrorWaypoints.add(lblMirrorMidLocation, "2, 8, right, default");

        textFieldMirrorMidX = new JTextField();
        panelMirrorWaypoints.add(textFieldMirrorMidX, "4, 8");
        textFieldMirrorMidX.setColumns(8);

        textFieldMirrorMidY = new JTextField();
        panelMirrorWaypoints.add(textFieldMirrorMidY, "6, 8");
        textFieldMirrorMidY.setColumns(8);

        textFieldMirrorMidZ = new JTextField();
        panelMirrorWaypoints.add(textFieldMirrorMidZ, "8, 8");
        textFieldMirrorMidZ.setColumns(8);

        locationButtonsPanelMirrorMid = new LocationButtonsPanel(
                textFieldMirrorMidX, textFieldMirrorMidY, textFieldMirrorMidZ, null);
        panelMirrorWaypoints.add(locationButtonsPanelMirrorMid, "10, 8");
        
        //end location
        JLabel lblMirrorEndLocation = new JLabel("Mirror End Location");
        panelMirrorWaypoints.add(lblMirrorEndLocation, "2, 10, right, default");

        textFieldMirrorEndX = new JTextField();
        panelMirrorWaypoints.add(textFieldMirrorEndX, "4, 10");
        textFieldMirrorEndX.setColumns(8);

        textFieldMirrorEndY = new JTextField();
        panelMirrorWaypoints.add(textFieldMirrorEndY, "6, 10");
        textFieldMirrorEndY.setColumns(8);

        textFieldMirrorEndZ = new JTextField();
        panelMirrorWaypoints.add(textFieldMirrorEndZ, "8, 10");
        textFieldMirrorEndZ.setColumns(8);

        locationButtonsPanelMirrorEnd = new LocationButtonsPanel(
                textFieldMirrorEndX, textFieldMirrorEndY, textFieldMirrorEndZ, null);
        panelMirrorWaypoints.add(locationButtonsPanelMirrorEnd, "10, 10");
        
        

        // setup panel for Changer waypoints
        panelChangerWaypoints = new JPanel();
//        panelFields.add(panelChangerWaypoints);
        panelChangerWaypoints.setBorder(new TitledBorder(null, "Changer Waypoints",
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        panelChangerWaypoints
                .setLayout(new FormLayout(new ColumnSpec[] {
                        FormFactory.RELATED_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.RELATED_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.RELATED_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.RELATED_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.RELATED_GAP_COLSPEC,
                        ColumnSpec.decode("left:default:grow"), },
                        new RowSpec[] { FormFactory.RELATED_GAP_ROWSPEC,
		                        FormFactory.DEFAULT_ROWSPEC,
		                        FormFactory.RELATED_GAP_ROWSPEC,
		                        FormFactory.DEFAULT_ROWSPEC,
		                        FormFactory.RELATED_GAP_ROWSPEC,
                                FormFactory.DEFAULT_ROWSPEC,
                                FormFactory.RELATED_GAP_ROWSPEC,
                                FormFactory.DEFAULT_ROWSPEC,
                                FormFactory.RELATED_GAP_ROWSPEC,
                                FormFactory.DEFAULT_ROWSPEC, }));

        JLabel lblchX = new JLabel("X");
        panelChangerWaypoints.add(lblchX, "4, 4");

        JLabel lblchY = new JLabel("Y");
        panelChangerWaypoints.add(lblchY, "6, 4");

        JLabel lblchZ = new JLabel("Z");
        panelChangerWaypoints.add(lblchZ, "8, 4");

        //start location
        JLabel lblChangerStartLocation = new JLabel("Changer Start Location");
        panelChangerWaypoints.add(lblChangerStartLocation, "2, 6, right, default");

        textFieldChangerStartX = new JTextField();
        panelChangerWaypoints.add(textFieldChangerStartX, "4, 6");
        textFieldChangerStartX.setColumns(8);

        textFieldChangerStartY = new JTextField();
        panelChangerWaypoints.add(textFieldChangerStartY, "6, 6");
        textFieldChangerStartY.setColumns(8);

        textFieldChangerStartZ = new JTextField();
        panelChangerWaypoints.add(textFieldChangerStartZ, "8, 6");
        textFieldChangerStartZ.setColumns(8);

        locationButtonsPanelChangerStart = new LocationButtonsPanel(
                textFieldChangerStartX, textFieldChangerStartY, textFieldChangerStartZ,
                null);
        panelChangerWaypoints.add(locationButtonsPanelChangerStart, "10, 6");
   
       //mid location
        JLabel lblChangerMidLocation = new JLabel("Changer Middle Location");
        panelChangerWaypoints.add(lblChangerMidLocation, "2, 8, right, default");

        textFieldChangerMidX = new JTextField();
        panelChangerWaypoints.add(textFieldChangerMidX, "4, 8");
        textFieldChangerMidX.setColumns(8);

        textFieldChangerMidY = new JTextField();
        panelChangerWaypoints.add(textFieldChangerMidY, "6, 8");
        textFieldChangerMidY.setColumns(8);

        textFieldChangerMidZ = new JTextField();
        panelChangerWaypoints.add(textFieldChangerMidZ, "8, 8");
        textFieldChangerMidZ.setColumns(8);

        locationButtonsPanelChangerMid = new LocationButtonsPanel(
                textFieldChangerMidX, textFieldChangerMidY, textFieldChangerMidZ, null);
        panelChangerWaypoints.add(locationButtonsPanelChangerMid, "10, 8");
        
        //end location
        JLabel lblChangerEndLocation = new JLabel("Changer End Location");
        panelChangerWaypoints.add(lblChangerEndLocation, "2, 10, right, default");

        textFieldChangerEndX = new JTextField();
        panelChangerWaypoints.add(textFieldChangerEndX, "4, 10");
        textFieldChangerEndX.setColumns(8);

        textFieldChangerEndY = new JTextField();
        panelChangerWaypoints.add(textFieldChangerEndY, "6, 10");
        textFieldChangerEndY.setColumns(8);

        textFieldChangerEndZ = new JTextField();
        panelChangerWaypoints.add(textFieldChangerEndZ, "8, 10");
        textFieldChangerEndZ.setColumns(8);

        locationButtonsPanelChangerEnd = new LocationButtonsPanel(
                textFieldChangerEndX, textFieldChangerEndY, textFieldChangerEndZ, null);
        panelChangerWaypoints.add(locationButtonsPanelChangerEnd, "10, 10");
        
        //vision panel
        panelVision = new JPanel();
        panelVision.setBorder(new TitledBorder(null, "Vision",
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        panelFields.add(panelVision);
        panelVision.setLayout(new BoxLayout(panelVision, BoxLayout.Y_AXIS));

        panelVisionEnabled = new JPanel();
        FlowLayout fl_panelVisionEnabled = (FlowLayout) panelVisionEnabled
                .getLayout();
        fl_panelVisionEnabled.setAlignment(FlowLayout.LEFT);
        panelVision.add(panelVisionEnabled);

        chckbxVisionEnabled = new JCheckBox("Vision Enabled?");
        panelVisionEnabled.add(chckbxVisionEnabled);

        separator = new JSeparator();
        panelVision.add(separator);

        panelVisionTemplateAndAoe = new JPanel();
        panelVision.add(panelVisionTemplateAndAoe);
        panelVisionTemplateAndAoe
                .setLayout(new FormLayout(new ColumnSpec[] {
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.RELATED_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC, }, new RowSpec[] {
                        FormFactory.RELATED_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC, }));
        
        panelTemplate = new JPanel();
        panelTemplate.setBorder(new TitledBorder(new EtchedBorder(
                EtchedBorder.LOWERED, null, null), "Template Image",
                TitledBorder.LEADING, TitledBorder.TOP, null,
                new Color(0, 0, 0)));
        panelVisionTemplateAndAoe.add(panelTemplate, "2, 2, center, fill");
        panelTemplate.setLayout(new BoxLayout(panelTemplate, BoxLayout.Y_AXIS));

        labelTemplateImage = new JLabel("");
        labelTemplateImage.setAlignmentX(Component.CENTER_ALIGNMENT);
        panelTemplate.add(labelTemplateImage);
        labelTemplateImage.setBorder(new BevelBorder(BevelBorder.LOWERED, null,
                null, null, null));
        labelTemplateImage.setMinimumSize(new Dimension(150, 150));
        labelTemplateImage.setMaximumSize(new Dimension(150, 150));
        labelTemplateImage.setHorizontalAlignment(SwingConstants.CENTER);
        labelTemplateImage.setSize(new Dimension(150, 150));
        labelTemplateImage.setPreferredSize(new Dimension(150, 150));

        panel = new JPanel();
        panelTemplate.add(panel);

        btnChangeTemplateImage = new JButton(selectTemplateImageAction);
        panel.add(btnChangeTemplateImage);
        btnChangeTemplateImage.setAlignmentX(Component.CENTER_ALIGNMENT);

        btnCancelChangeTemplateImage = new JButton(
                cancelSelectTemplateImageAction);
        panel.add(btnCancelChangeTemplateImage);

        panelAoE = new JPanel();
        panelAoE.setBorder(new TitledBorder(null, "Area of Interest",
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        panelVisionTemplateAndAoe.add(panelAoE, "4, 2, fill, fill");
        panelAoE.setLayout(new FormLayout(
                new ColumnSpec[] { FormFactory.RELATED_GAP_COLSPEC,
                        ColumnSpec.decode("default:grow"),
                        FormFactory.RELATED_GAP_COLSPEC,
                        ColumnSpec.decode("default:grow"),
                        FormFactory.RELATED_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.RELATED_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.RELATED_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.RELATED_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC, }, new RowSpec[] {
                        FormFactory.RELATED_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.RELATED_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC, }));

        lblX_1 = new JLabel("X");
        panelAoE.add(lblX_1, "2, 2");

        lblY_1 = new JLabel("Y");
        panelAoE.add(lblY_1, "4, 2");

        lblWidth = new JLabel("Width");
        panelAoE.add(lblWidth, "6, 2");

        lblHeight = new JLabel("Height");
        panelAoE.add(lblHeight, "8, 2");

        textFieldAoiX = new JTextField();
        panelAoE.add(textFieldAoiX, "2, 4, fill, default");
        textFieldAoiX.setColumns(5);

        textFieldAoiY = new JTextField();
        panelAoE.add(textFieldAoiY, "4, 4, fill, default");
        textFieldAoiY.setColumns(5);

        textFieldAoiWidth = new JTextField();
        panelAoE.add(textFieldAoiWidth, "6, 4, fill, default");
        textFieldAoiWidth.setColumns(5);

        textFieldAoiHeight = new JTextField();
        panelAoE.add(textFieldAoiHeight, "8, 4, fill, default");
        textFieldAoiHeight.setColumns(5);

        btnChangeAoi = new JButton("Change");
        btnChangeAoi.setAction(selectAoiAction);
        panelAoE.add(btnChangeAoi, "10, 4");

        btnCancelChangeAoi = new JButton("Cancel");
        btnCancelChangeAoi.setAction(cancelSelectAoiAction);
        panelAoE.add(btnCancelChangeAoi, "12, 4");

        cancelSelectTemplateImageAction.setEnabled(false);
        cancelSelectAoiAction.setEnabled(false);

        
        //add panels to wizard content
        panelFields.add(panelOffsets);
        panelFields.add(panelMirrorWaypoints);
        panelFields.add(panelChangerWaypoints);
        contentPanel.add(panelFields);
  }

    @Override
    public void createBindings() {
        LengthConverter lengthConverter = new LengthConverter();
        IntegerConverter intConverter = new IntegerConverter();
        BufferedImageIconConverter imageConverter = new BufferedImageIconConverter();
        
        MutableLocationProxy nozzleOffsets = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, zippynozzletip, "nozzleOffsets", nozzleOffsets, "location");
        addWrappedBinding(nozzleOffsets, "lengthX", locationX, "text", lengthConverter);
        addWrappedBinding(nozzleOffsets, "lengthY", locationY, "text", lengthConverter);
        addWrappedBinding(nozzleOffsets, "lengthZ", locationZ, "text", lengthConverter);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(locationX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(locationY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(locationZ);

        MutableLocationProxy mirrorStartLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, zippynozzletip, "mirrorStartLocation", mirrorStartLocation, "location");
        addWrappedBinding(mirrorStartLocation, "lengthX", textFieldMirrorStartX, "text", lengthConverter);
        addWrappedBinding(mirrorStartLocation, "lengthY", textFieldMirrorStartY, "text", lengthConverter);
        addWrappedBinding(mirrorStartLocation, "lengthZ", textFieldMirrorStartZ, "text", lengthConverter);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldMirrorStartX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldMirrorStartY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldMirrorStartZ);

        MutableLocationProxy mirrorMidLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, zippynozzletip, "mirrorMidLocation", mirrorMidLocation, "location");
        addWrappedBinding(mirrorMidLocation, "lengthX", textFieldMirrorMidX, "text", lengthConverter);
        addWrappedBinding(mirrorMidLocation, "lengthY", textFieldMirrorMidY, "text", lengthConverter);
        addWrappedBinding(mirrorMidLocation, "lengthZ", textFieldMirrorMidZ, "text", lengthConverter);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldMirrorMidX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldMirrorMidY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldMirrorMidZ);

        MutableLocationProxy mirrorEndLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, zippynozzletip, "mirrorEndLocation", mirrorEndLocation, "location");
        addWrappedBinding(mirrorEndLocation, "lengthX", textFieldMirrorEndX, "text", lengthConverter);
        addWrappedBinding(mirrorEndLocation, "lengthY", textFieldMirrorEndY, "text", lengthConverter);
        addWrappedBinding(mirrorEndLocation, "lengthZ", textFieldMirrorEndZ, "text", lengthConverter);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldMirrorEndX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldMirrorEndY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldMirrorEndZ);

        MutableLocationProxy changerStartLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, zippynozzletip, "changerStartLocation", changerStartLocation, "location");
        addWrappedBinding(changerStartLocation, "lengthX", textFieldChangerStartX, "text", lengthConverter);
        addWrappedBinding(changerStartLocation, "lengthY", textFieldChangerStartY, "text", lengthConverter);
        addWrappedBinding(changerStartLocation, "lengthZ", textFieldChangerStartZ, "text", lengthConverter);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerStartX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerStartY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerStartZ);

        MutableLocationProxy changerMidLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, zippynozzletip, "changerMidLocation", changerMidLocation, "location");
        addWrappedBinding(changerMidLocation, "lengthX", textFieldChangerMidX, "text", lengthConverter);
        addWrappedBinding(changerMidLocation, "lengthY", textFieldChangerMidY, "text", lengthConverter);
        addWrappedBinding(changerMidLocation, "lengthZ", textFieldChangerMidZ, "text", lengthConverter);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerMidX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerMidY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerMidZ);

        MutableLocationProxy changerEndLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, zippynozzletip, "changerEndLocation", changerEndLocation, "location");
        addWrappedBinding(changerEndLocation, "lengthX", textFieldChangerEndX, "text", lengthConverter);
        addWrappedBinding(changerEndLocation, "lengthY", textFieldChangerEndY, "text", lengthConverter);
        addWrappedBinding(changerEndLocation, "lengthZ", textFieldChangerEndZ, "text", lengthConverter);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerEndX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerEndY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerEndZ);

        addWrappedBinding(zippynozzletip, "vision.enabled", chckbxVisionEnabled, "selected");
        addWrappedBinding(zippynozzletip, "vision.templateImage", labelTemplateImage, "icon", imageConverter);
        addWrappedBinding(zippynozzletip, "vision.areaOfInterest.x", textFieldAoiX, "text", intConverter);
        addWrappedBinding(zippynozzletip, "vision.areaOfInterest.y", textFieldAoiY, "text", intConverter);
        addWrappedBinding(zippynozzletip, "vision.areaOfInterest.width", textFieldAoiWidth, "text", intConverter);
        addWrappedBinding(zippynozzletip, "vision.areaOfInterest.height", textFieldAoiHeight, "text", intConverter);
        ComponentDecorators.decorateWithAutoSelect(textFieldAoiX);
        ComponentDecorators.decorateWithAutoSelect(textFieldAoiY);
        ComponentDecorators.decorateWithAutoSelect(textFieldAoiWidth);
        ComponentDecorators.decorateWithAutoSelect(textFieldAoiHeight);


    }
    @SuppressWarnings("serial")
    private Action selectTemplateImageAction = new AbstractAction("Select") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            CameraView cameraView = MainFrame.cameraPanel
                    .getSelectedCameraView();
            cameraView.setSelectionEnabled(true);
            // org.openpnp.model.Rectangle r =
            // feeder.getVision().getTemplateImageCoordinates();
            org.openpnp.model.Rectangle r = null;
            if (r == null || r.getWidth() == 0 || r.getHeight() == 0) {
                cameraView.setSelection(0, 0, 100, 100);
            }
            else {
                // cameraView.setSelection(r.getLeft(), r.getTop(),
                // r.getWidth(), r.getHeight());
            }
            btnChangeTemplateImage.setAction(confirmSelectTemplateImageAction);
            cancelSelectTemplateImageAction.setEnabled(true);
        }
    };

    @SuppressWarnings("serial")
    private Action confirmSelectTemplateImageAction = new AbstractAction(
            "Confirm") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            new Thread() {
                public void run() {
                    CameraView cameraView = MainFrame.cameraPanel
                            .getSelectedCameraView();
                    BufferedImage image = cameraView.captureSelectionImage();
                    if (image == null) {
                        MessageBoxes
                                .errorBox(
                                        ZippyNozzleTipConfigurationWizard.this,
                                        "No Image Selected",
                                        "Please select an area of the camera image using the mouse.");
                    }
                    else {
                        labelTemplateImage.setIcon(new ImageIcon(image));
                    }
                    cameraView.setSelectionEnabled(false);
                    btnChangeTemplateImage.setAction(selectTemplateImageAction);
                    cancelSelectTemplateImageAction.setEnabled(false);
                }
            }.start();
        }
    };

    @SuppressWarnings("serial")
    private Action cancelSelectTemplateImageAction = new AbstractAction(
            "Cancel") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            btnChangeTemplateImage.setAction(selectTemplateImageAction);
            cancelSelectTemplateImageAction.setEnabled(false);
            CameraView cameraView = MainFrame.cameraPanel
                    .getSelectedCameraView();
            if (cameraView == null) {
                MessageBoxes.errorBox(getTopLevelAncestor(), "Error",
                        "Unable to locate Camera.");
            }
            cameraView.setSelectionEnabled(false);
        }
    };

    @SuppressWarnings("serial")
    private Action selectAoiAction = new AbstractAction("Select") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            btnChangeAoi.setAction(confirmSelectAoiAction);
            cancelSelectAoiAction.setEnabled(true);

            CameraView cameraView = MainFrame.cameraPanel.getSelectedCameraView();
            cameraView.setSelectionEnabled(true);
/*            org.openpnp.model.Rectangle r = zippynozzletip.getVision().getAreaOfInterest();
            if (r == null || r.getWidth() == 0 || r.getHeight() == 0) {
                cameraView.setSelection(0, 0, 100, 100);
            }
            else {
                cameraView.setSelection(r.getX(), r.getY(), r.getWidth(),
                        r.getHeight());
            }
*/        }
    };

    @SuppressWarnings("serial")
    private Action confirmSelectAoiAction = new AbstractAction("Confirm") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            new Thread() {
                public void run() {
                    btnChangeAoi.setAction(selectAoiAction);
                    cancelSelectAoiAction.setEnabled(false);

                    CameraView cameraView = MainFrame.cameraPanel
                            .getSelectedCameraView();
                    cameraView.setSelectionEnabled(false);
                    final Rectangle rect = cameraView.getSelection();
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            textFieldAoiX.setText(Integer.toString(rect.x));
                            textFieldAoiY.setText(Integer.toString(rect.y));
                            textFieldAoiWidth.setText(Integer
                                    .toString(rect.width));
                            textFieldAoiHeight.setText(Integer
                                    .toString(rect.height));
                        }
                    });
                }
            }.start();
        }
    };

    @SuppressWarnings("serial")
    private Action cancelSelectAoiAction = new AbstractAction("Cancel") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            btnChangeAoi.setAction(selectAoiAction);
            cancelSelectAoiAction.setEnabled(false);
            CameraView cameraView = MainFrame.cameraPanel
                    .getSelectedCameraView();
            if (cameraView == null) {
                MessageBoxes.errorBox(getTopLevelAncestor(), "Error",
                        "Unable to locate Camera.");
            }
            cameraView.setSelectionEnabled(false);
        }
    };
}