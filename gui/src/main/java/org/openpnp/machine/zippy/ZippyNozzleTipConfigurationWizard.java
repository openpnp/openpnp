/*
 	Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 	
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

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.spi.NozzleTip;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

public class ZippyNozzleTipConfigurationWizard extends
        AbstractConfigurationWizard {
    private final NozzleTip nozzletip;

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

    public ZippyNozzleTipConfigurationWizard(NozzleTip nozzletip) {
        this.nozzletip = nozzletip;

        //setup panel for nozzle offsets (crookedness)
//        JPanel panelFields = new JPanel();
//        panelFields.setLayout(new BoxLayout(panelFields, BoxLayout.Y_AXIS));

        panelOffsets = new JPanel();
//        panelFields.add(panelOffsets);
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
        
        
        
        //add panels to wizard content
         contentPanel.add(panelOffsets);
         contentPanel.add(panelMirrorWaypoints);
         contentPanel.add(panelChangerWaypoints);
   }

    @Override
    public void createBindings() {
        LengthConverter lengthConverter = new LengthConverter();

        MutableLocationProxy nozzleOffsets = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, nozzletip, "nozzleOffsets", nozzleOffsets, "location");
        addWrappedBinding(nozzleOffsets, "lengthX", locationX, "text", lengthConverter);
        addWrappedBinding(nozzleOffsets, "lengthY", locationY, "text", lengthConverter);
        addWrappedBinding(nozzleOffsets, "lengthZ", locationZ, "text", lengthConverter);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(locationX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(locationY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(locationZ);

        MutableLocationProxy mirrorStartLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, nozzletip, "mirrorStartLocation", mirrorStartLocation, "location");
        addWrappedBinding(mirrorStartLocation, "lengthX", textFieldMirrorStartX, "text", lengthConverter);
        addWrappedBinding(mirrorStartLocation, "lengthY", textFieldMirrorStartY, "text", lengthConverter);
        addWrappedBinding(mirrorStartLocation, "lengthZ", textFieldMirrorStartZ, "text", lengthConverter);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldMirrorStartX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldMirrorStartY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldMirrorStartZ);

        MutableLocationProxy mirrorMidLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, nozzletip, "mirrorMidLocation", mirrorMidLocation, "location");
        addWrappedBinding(mirrorMidLocation, "lengthX", textFieldMirrorMidX, "text", lengthConverter);
        addWrappedBinding(mirrorMidLocation, "lengthY", textFieldMirrorMidY, "text", lengthConverter);
        addWrappedBinding(mirrorMidLocation, "lengthZ", textFieldMirrorMidZ, "text", lengthConverter);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldMirrorMidX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldMirrorMidY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldMirrorMidZ);

        MutableLocationProxy mirrorEndLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, nozzletip, "mirrorEndLocation", mirrorEndLocation, "location");
        addWrappedBinding(mirrorEndLocation, "lengthX", textFieldMirrorEndX, "text", lengthConverter);
        addWrappedBinding(mirrorEndLocation, "lengthY", textFieldMirrorEndY, "text", lengthConverter);
        addWrappedBinding(mirrorEndLocation, "lengthZ", textFieldMirrorEndZ, "text", lengthConverter);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldMirrorEndX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldMirrorEndY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldMirrorEndZ);

        MutableLocationProxy changerStartLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, nozzletip, "changerStartLocation", changerStartLocation, "location");
        addWrappedBinding(changerStartLocation, "lengthX", textFieldChangerStartX, "text", lengthConverter);
        addWrappedBinding(changerStartLocation, "lengthY", textFieldChangerStartY, "text", lengthConverter);
        addWrappedBinding(changerStartLocation, "lengthZ", textFieldChangerStartZ, "text", lengthConverter);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerStartX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerStartY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerStartZ);

        MutableLocationProxy changerMidLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, nozzletip, "changerMidLocation", changerMidLocation, "location");
        addWrappedBinding(changerMidLocation, "lengthX", textFieldChangerMidX, "text", lengthConverter);
        addWrappedBinding(changerMidLocation, "lengthY", textFieldChangerMidY, "text", lengthConverter);
        addWrappedBinding(changerMidLocation, "lengthZ", textFieldChangerMidZ, "text", lengthConverter);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerMidX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerMidY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerMidZ);

        MutableLocationProxy changerEndLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, nozzletip, "changerEndLocation", changerEndLocation, "location");
        addWrappedBinding(changerEndLocation, "lengthX", textFieldChangerEndX, "text", lengthConverter);
        addWrappedBinding(changerEndLocation, "lengthY", textFieldChangerEndY, "text", lengthConverter);
        addWrappedBinding(changerEndLocation, "lengthZ", textFieldChangerEndZ, "text", lengthConverter);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerEndX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerEndY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerEndZ);

    }
}