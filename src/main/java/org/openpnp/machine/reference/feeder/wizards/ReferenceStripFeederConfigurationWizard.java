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

package org.openpnp.machine.reference.feeder.wizards;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.support.BufferedImageIconConverter;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.machine.reference.feeder.ReferenceStripFeeder;
import org.openpnp.model.Configuration;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class ReferenceStripFeederConfigurationWizard extends
        AbstractReferenceFeederConfigurationWizard {
    private final ReferenceStripFeeder feeder;

    private JTextField textFieldFeedStartX;
    private JTextField textFieldFeedStartY;
    private JTextField textFieldFeedStartZ;
    private JTextField textFieldFeedEndX;
    private JTextField textFieldFeedEndY;
    private JTextField textFieldFeedEndZ;
    private JTextField textFieldFeedRate;
    private JLabel lblActuatorId;
    private JTextField textFieldActuatorId;
    private JPanel panelGeneral;
    private JPanel panelLocations;
    private LocationButtonsPanel locationButtonsPanelFeedStart;
    private LocationButtonsPanel locationButtonsPanelFeedEnd;

    public ReferenceStripFeederConfigurationWizard(ReferenceStripFeeder feeder) {
        super(feeder);
        this.feeder = feeder;

        JPanel panelFields = new JPanel();
        panelFields.setLayout(new BoxLayout(panelFields, BoxLayout.Y_AXIS));

        panelGeneral = new JPanel();
        panelGeneral.setBorder(new TitledBorder(null, "General Settings",
                TitledBorder.LEADING, TitledBorder.TOP, null, null));

        panelFields.add(panelGeneral);
        panelGeneral.setLayout(new FormLayout(
                new ColumnSpec[] { FormFactory.RELATED_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.RELATED_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC, }, new RowSpec[] {
                        FormFactory.RELATED_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.RELATED_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC, }));

        JLabel lblFeedRate = new JLabel("Feed Speed (0 - 1)");
        panelGeneral.add(lblFeedRate, "2, 2");

        textFieldFeedRate = new JTextField();
        panelGeneral.add(textFieldFeedRate, "4, 2");
        textFieldFeedRate.setColumns(5);

        lblActuatorId = new JLabel("Actuator Name");
        panelGeneral.add(lblActuatorId, "2, 4, right, default");

        textFieldActuatorId = new JTextField();
        panelGeneral.add(textFieldActuatorId, "4, 4");
        textFieldActuatorId.setColumns(5);

        panelLocations = new JPanel();
        panelFields.add(panelLocations);
        panelLocations.setBorder(new TitledBorder(null, "Locations",
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        panelLocations
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
                                FormFactory.DEFAULT_ROWSPEC, }));

        JLabel lblX = new JLabel("X");
        panelLocations.add(lblX, "4, 4");

        JLabel lblY = new JLabel("Y");
        panelLocations.add(lblY, "6, 4");

        JLabel lblZ = new JLabel("Z");
        panelLocations.add(lblZ, "8, 4");

        JLabel lblFeedStartLocation = new JLabel("Reference Hole Location");
        panelLocations.add(lblFeedStartLocation, "2, 6, right, default");

        textFieldFeedStartX = new JTextField();
        panelLocations.add(textFieldFeedStartX, "4, 6");
        textFieldFeedStartX.setColumns(8);

        textFieldFeedStartY = new JTextField();
        panelLocations.add(textFieldFeedStartY, "6, 6");
        textFieldFeedStartY.setColumns(8);

        textFieldFeedStartZ = new JTextField();
        panelLocations.add(textFieldFeedStartZ, "8, 6");
        textFieldFeedStartZ.setColumns(8);

        locationButtonsPanelFeedStart = new LocationButtonsPanel(
                textFieldFeedStartX, textFieldFeedStartY, textFieldFeedStartZ,
                null);
        panelLocations.add(locationButtonsPanelFeedStart, "10, 6");

        JLabel lblFeedEndLocation = new JLabel("Last Hole Location");
        panelLocations.add(lblFeedEndLocation, "2, 8, right, default");

        textFieldFeedEndX = new JTextField();
        panelLocations.add(textFieldFeedEndX, "4, 8");
        textFieldFeedEndX.setColumns(8);

        textFieldFeedEndY = new JTextField();
        panelLocations.add(textFieldFeedEndY, "6, 8");
        textFieldFeedEndY.setColumns(8);

        textFieldFeedEndZ = new JTextField();
        panelLocations.add(textFieldFeedEndZ, "8, 8");
        textFieldFeedEndZ.setColumns(8);

        locationButtonsPanelFeedEnd = new LocationButtonsPanel(
                textFieldFeedEndX, textFieldFeedEndY, textFieldFeedEndZ, null);
        panelLocations.add(locationButtonsPanelFeedEnd, "10, 8");

        contentPanel.add(panelFields);
    }

    @Override
    public void createBindings() {
        super.createBindings();
        LengthConverter lengthConverter = new LengthConverter();
        IntegerConverter intConverter = new IntegerConverter();
        DoubleConverter doubleConverter = new DoubleConverter(Configuration.get().getLengthDisplayFormat());
        BufferedImageIconConverter imageConverter = new BufferedImageIconConverter();

//        addWrappedBinding(feeder, "feedSpeed", textFieldFeedRate, "text",
//                doubleConverter);
//        addWrappedBinding(feeder, "actuatorName", textFieldActuatorId, "text");

        MutableLocationProxy feedStartLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "referenceHoleLocation",
                feedStartLocation, "location");
        addWrappedBinding(feedStartLocation, "lengthX", textFieldFeedStartX,
                "text", lengthConverter);
        addWrappedBinding(feedStartLocation, "lengthY", textFieldFeedStartY,
                "text", lengthConverter);
        addWrappedBinding(feedStartLocation, "lengthZ", textFieldFeedStartZ,
                "text", lengthConverter);

        MutableLocationProxy feedEndLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "lastHoleLocation",
                feedEndLocation, "location");
        addWrappedBinding(feedEndLocation, "lengthX", textFieldFeedEndX,
                "text", lengthConverter);
        addWrappedBinding(feedEndLocation, "lengthY", textFieldFeedEndY,
                "text", lengthConverter);
        addWrappedBinding(feedEndLocation, "lengthZ", textFieldFeedEndZ,
                "text", lengthConverter);

        ComponentDecorators
                .decorateWithAutoSelectAndLengthConversion(textFieldFeedRate);
        ComponentDecorators.decorateWithAutoSelect(textFieldActuatorId);
        ComponentDecorators
                .decorateWithAutoSelectAndLengthConversion(textFieldFeedStartX);
        ComponentDecorators
                .decorateWithAutoSelectAndLengthConversion(textFieldFeedStartY);
        ComponentDecorators
                .decorateWithAutoSelectAndLengthConversion(textFieldFeedStartZ);
        ComponentDecorators
                .decorateWithAutoSelectAndLengthConversion(textFieldFeedEndX);
        ComponentDecorators
                .decorateWithAutoSelectAndLengthConversion(textFieldFeedEndY);
        ComponentDecorators
                .decorateWithAutoSelectAndLengthConversion(textFieldFeedEndZ);
    }
}