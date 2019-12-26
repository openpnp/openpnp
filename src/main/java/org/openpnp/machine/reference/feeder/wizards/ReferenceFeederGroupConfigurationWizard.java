/*
 * Copyright (C) 2017 Sebastian Pichelhofer & Jason von Nieda <jason@vonnieda.org>
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

package org.openpnp.machine.reference.feeder.wizards;

import java.awt.Color;
import java.awt.Event;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.Converter;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.FeederParentsComboBoxModel;
import org.openpnp.gui.support.IdentifiableListCellRenderer;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.machine.reference.feeder.ReferenceFeederGroup;
import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.Configuration;
import org.openpnp.model.Part;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.base.AbstractFeeder;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class ReferenceFeederGroupConfigurationWizard extends AbstractConfigurationWizard {
	private final ReferenceFeederGroup feeder;
	private final boolean includePickLocation;

	private JTextField textFieldOffsetsX;
	private JTextField textFieldOffsetsY;

	private JTextField textFieldFeedCount;

	private JPanel panelLocation;
	private JPanel panelParameters;
	private JPanel panelIllustration;

	private JTextField textFieldLocationX;
    private JTextField textFieldLocationY;
    private JTextField textFieldLocationZ;
    private JTextField textFieldLocationR;
    
    private JTextField textFieldFid1X;
    private JTextField textFieldFid1Y;
    
    private JTextField textFieldFid2X;
    private JTextField textFieldFid2Y;
    
	private JPanel panelPart;

    private JComboBox<?> comboBoxParent;
//	private LocationButtonsPanel locationButtonsPanel;

	/**
	 * @wbp.parser.constructor
	 */
	public ReferenceFeederGroupConfigurationWizard(ReferenceFeederGroup feeder) {
		this(feeder, true);
	}

	public ReferenceFeederGroupConfigurationWizard(ReferenceFeederGroup feeder,
			boolean includePickLocation) {
		// super(feeder);
		this.feeder = feeder;
		this.includePickLocation = includePickLocation;

		JPanel warningPanel = new JPanel();
		FlowLayout flowLayout = (FlowLayout) warningPanel.getLayout();
		contentPanel.add(warningPanel, 0);

		JLabel lblWarningThisFeeder = new JLabel(
				"Warning: This feeder is experimental. Use at your own risk.");
		lblWarningThisFeeder.setFont(new Font("Lucida Grande", Font.PLAIN, 16));
		lblWarningThisFeeder.setForeground(Color.RED);
		lblWarningThisFeeder.setHorizontalAlignment(SwingConstants.LEFT);
		warningPanel.add(lblWarningThisFeeder);
        try {
        } catch (Throwable t) {
            // Swallow this error. This happens during parsing in
            // in WindowBuilder but doesn't happen during normal run.
        }


        panelPart = new JPanel();
        panelPart.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "General Settings",
                TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        contentPanel.add(panelPart);
        panelPart.setLayout(new FormLayout(new ColumnSpec[] {
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
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC}));
        
        comboBoxParent = new JComboBox();
        comboBoxParent.setModel(new FeederParentsComboBoxModel(feeder));
                
        JLabel lblParent = new JLabel("Parent");
        panelPart.add(lblParent, "2, 2, right, default");
        //comboBoxOwner.setRenderer(new IdentifiableListCellRenderer<Parent>());
        panelPart.add(comboBoxParent, "4, 2, left, default");



		panelLocation = new JPanel();
		panelLocation.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null),
				"Feeder Group Location and Rotation (Machine Coordinates)", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		contentPanel.add(panelLocation);
		panelLocation.setLayout(new FormLayout(new ColumnSpec[] {
		        FormSpecs.RELATED_GAP_COLSPEC,
		        ColumnSpec.decode("default:grow"),
		        FormSpecs.RELATED_GAP_COLSPEC,
		        ColumnSpec.decode("default:grow"),
		        FormSpecs.RELATED_GAP_COLSPEC,
		        ColumnSpec.decode("default:grow"),
		        FormSpecs.RELATED_GAP_COLSPEC,
		        ColumnSpec.decode("default:grow"),
		        FormSpecs.RELATED_GAP_COLSPEC,
		        ColumnSpec.decode("default:grow"),
		        FormSpecs.RELATED_GAP_COLSPEC,
		        ColumnSpec.decode("default:grow"),
		        FormSpecs.RELATED_GAP_COLSPEC,
		        ColumnSpec.decode("left:default:grow"),},
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

		JLabel locationlabel = new JLabel("Location and Rotation");
		panelLocation.add(locationlabel, "2, 4");

		JLabel lblX_1 = new JLabel("X");
		panelLocation.add(lblX_1, "4, 2");

		JLabel lblY_1 = new JLabel("Y");
        panelLocation.add(lblY_1, "6, 2");

        JLabel lblZ_1 = new JLabel("Z");
        panelLocation.add(lblZ_1, "8, 2");

        JLabel lblR_1 = new JLabel("Rot");
        panelLocation.add(lblR_1, "10, 2");

        textFieldLocationX = new JTextField();
        textFieldLocationX.setColumns(6);
        panelLocation.add(textFieldLocationX, "4, 4");

        textFieldLocationY = new JTextField();
        textFieldLocationY.setColumns(6);
        panelLocation.add(textFieldLocationY, "6, 4");

        textFieldLocationZ = new JTextField();
        textFieldLocationZ.setColumns(6);
        panelLocation.add(textFieldLocationZ, "8, 4");

        textFieldLocationR = new JTextField();
        textFieldLocationR.setColumns(6);
        panelLocation.add(textFieldLocationR, "10, 4");

        LocationButtonsPanel locationButtonsPanel = new LocationButtonsPanel(textFieldLocationX, textFieldLocationY, textFieldLocationZ, textFieldLocationR);
		panelLocation.add(locationButtonsPanel, "12, 4");
		
        JButton btnSetLocationWithFiducials = new JButton(new AbstractAction("Set Using Fiducial Locations") {
            @Override
            public void actionPerformed(ActionEvent e) {
                //textFieldFeedCount.setText("0");
                //int componentleft = Integer.parseInt(textFieldTrayCountCols.getText())
                //      * Integer.parseInt(textFieldTrayCountRows.getText())
                //      - Integer.parseInt(textFieldFeedCount.getText());
                //lblComponentCount.setText("Components left: " + String.valueOf(componentleft));
                //applyAction.actionPerformed(e);
            }
        });
        btnSetLocationWithFiducials.setHorizontalAlignment(SwingConstants.LEFT);
        panelLocation.add(btnSetLocationWithFiducials, "14, 4, left, default");



		panelParameters = new JPanel();
		panelParameters.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null),
				"Fiducial Locations (Local Coordinates)", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		contentPanel.add(panelParameters);

		panelParameters.setLayout(new FormLayout(
				new ColumnSpec[] { 
                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"),
                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"),
                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"),
                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"),
                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"),
                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"),
                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("left:default:grow"), },
				new RowSpec[] { 
				        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
						FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC, 
						FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC, 
						FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
						FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC, 
						FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC, 
						FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
						FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC, 
						FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC }));

        JLabel fid1Label = new JLabel("Fiducial1            ");
        panelParameters.add(fid1Label, "2, 4");

        lblX_1 = new JLabel("X");
        panelParameters.add(lblX_1, "4, 2");

        lblY_1 = new JLabel("Y");
        panelParameters.add(lblY_1, "6, 2");

        textFieldFid1X = new JTextField();
        textFieldFid1X.setColumns(6);
        panelParameters.add(textFieldFid1X, "4, 4");

        textFieldFid1Y = new JTextField();
        textFieldFid1Y.setColumns(6);
        panelParameters.add(textFieldFid1Y, "6, 4");

        locationButtonsPanel = new LocationButtonsPanel(textFieldFid1X, textFieldFid1Y, null, null);
        panelParameters.add(locationButtonsPanel, "8, 4");


        
        JLabel fid2Label = new JLabel("Fiducial2            ");
        panelParameters.add(fid2Label, "2, 8");

        lblX_1 = new JLabel("X");
        panelParameters.add(lblX_1, "4, 6");

        lblY_1 = new JLabel("Y");
        panelParameters.add(lblY_1, "6, 6");

        textFieldFid2X = new JTextField();
        textFieldFid2X.setColumns(6);
        panelParameters.add(textFieldFid2X, "4, 8");

        textFieldFid2Y = new JTextField();
        textFieldFid2Y.setColumns(6);
        panelParameters.add(textFieldFid2Y, "6, 8");

        locationButtonsPanel = new LocationButtonsPanel(textFieldFid2X, textFieldFid2Y, null, null);
        panelParameters.add(locationButtonsPanel, "8, 8");



		panelIllustration = new JPanel();
		panelIllustration.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null),
				"Tray Illustration", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		contentPanel.add(panelIllustration);

		InputStream stream = getClass().getResourceAsStream("/illustrations/rotatedtrayfeeder.png");
		ImageIcon illustrationicon = null;
		try {
			illustrationicon = new ImageIcon(ImageIO.read(stream));

		} catch (IOException e1) {
			e1.printStackTrace();
		}
		JLabel illustationlabel = new JLabel();
		illustationlabel.setIcon(illustrationicon);
		panelIllustration.add(illustationlabel);
	}

	
	@Override
	public void createBindings() {
		LengthConverter lengthConverter = new LengthConverter();
		IntegerConverter intConverter = new IntegerConverter();
		FeederConverter feederConverter = new FeederConverter();
		DoubleConverter doubleConverter = new DoubleConverter(Configuration.get().getLengthDisplayFormat());
        addWrappedBinding(feeder, "parentId", comboBoxParent, "selectedItem", feederConverter);

		MutableLocationProxy location = new MutableLocationProxy();
		bind(UpdateStrategy.READ_WRITE, feeder, "location", location, "location");
		addWrappedBinding(location, "lengthX", textFieldLocationX, "text", lengthConverter);
		addWrappedBinding(location, "lengthY", textFieldLocationY, "text", lengthConverter);
		addWrappedBinding(location, "lengthZ", textFieldLocationZ, "text", lengthConverter);
        addWrappedBinding(location, "rotation", textFieldLocationR, "text", doubleConverter);

        MutableLocationProxy fid1Location = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "expectedFiducial1", fid1Location,
                "location");
        addWrappedBinding(fid1Location, "lengthX", textFieldFid1X, "text",
                lengthConverter);
        addWrappedBinding(fid1Location, "lengthY", textFieldFid1Y, "text",
                lengthConverter);

        MutableLocationProxy fid2Location = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "expectedFiducial2", fid2Location,
                "location");
        addWrappedBinding(fid2Location, "lengthX", textFieldFid2X, "text",
                lengthConverter);
        addWrappedBinding(fid2Location, "lengthY", textFieldFid2Y, "text",
                lengthConverter);

		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldLocationX);
		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldLocationY);
		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldLocationZ);
		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldLocationR);
		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFid1X);
		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFid1Y);
		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFid2X);
		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFid2Y);

	}

/*	@Override
	protected void saveToModel() {
		super.saveToModel();

		int componentleft = (Integer.parseInt(textFieldTrayCountCols.getText())
				* Integer.parseInt(textFieldTrayCountRows.getText())) - Integer.parseInt(textFieldFeedCount.getText());
		lblComponentCount.setText("Components left: " + String.valueOf(componentleft));

		if ((feeder.getOffsets().getX() == 0) && (feeder.getTrayCountCols() > 1)) {
			MessageBoxes.errorBox(this, "Error",
					"Column Offset  must be greater than 0 if Number of Tray Columns is greater than 1 or feed failure will occur.");
		}
		if ((feeder.getOffsets().getY() == 0) && (feeder.getTrayCountRows() > 1)) {
			MessageBoxes.errorBox(this, "Error",
					"Row Offset must be greater than 0 if Number of Tray Rows is greater than 1 or feed failure will occur.");
		}
	}
*/
	public static double round(double value, int places) {
		if (places < 0) {
			throw new IllegalArgumentException();
		}

		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(places, RoundingMode.HALF_UP);
		return bd.doubleValue();
	}
	
	class FeederConverter extends Converter<String, Object> {

        @Override
        public Object convertForward(String arg0) {
            if (arg0.equals(AbstractFeeder.ROOT_FEEDER_ID)) {
                return arg0;
            } else {
                return Configuration.get().getMachine().getFeeder(arg0);
            }
        }

        @Override
        public String convertReverse(Object arg0) {
            String feederId;
            try {
                feederId = ((Feeder )arg0).getId();
            } catch (Exception e) {
                feederId = (String )arg0;
            }
            return feederId;
        }
	    
	}
}
