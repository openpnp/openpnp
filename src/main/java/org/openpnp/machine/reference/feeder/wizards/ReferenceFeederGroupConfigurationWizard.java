/*
 * Copyright (C) 2017, 2020 Tony Luken,  Sebastian Pichelhofer & Jason von Nieda <jason@vonnieda.org>
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

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Event;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.TextEvent;
import java.awt.geom.AffineTransform;
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
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.Converter;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.FeederParentsComboBoxModel;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.IdentifiableListCellRenderer;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.gui.support.PartsComboBoxModel;
import org.openpnp.machine.reference.ReferenceFeeder;
import org.openpnp.machine.reference.feeder.ReferenceFeederGroup;
import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.base.AbstractFeeder;
import org.openpnp.util.UiUtils;
import org.openpnp.util.Utils2D;
import org.pmw.tinylog.Logger;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class ReferenceFeederGroupConfigurationWizard extends AbstractConfigurationWizard {
	private final ReferenceFeederGroup feeder;

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
    private JComboBox<?> comboBoxPart;
    private JPanel panel;
    
    private JButton btnSetLocationWithFiducials;
    private boolean fid1;
    private Location fid1Captured;
    private Location fid2Captured;
    
    MutableLocationProxy upToDateLocation;
    MutableLocationProxy fid1LocalLocation;
    MutableLocationProxy fid2LocalLocation;
    LocationButtonsPanel locationButtonsPanelFid1;
    LocationButtonsPanel locationButtonsPanelFid2;


	/**
	 * @wbp.parser.constructor
	 */
	public ReferenceFeederGroupConfigurationWizard(ReferenceFeederGroup feeder) {
		this(feeder, true);
	}

	public ReferenceFeederGroupConfigurationWizard(ReferenceFeederGroup feeder,
			boolean includePickLocation) {
		this.feeder = feeder;
        fid1Captured = null;
        fid2Captured = null;
		
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
                FormSpecs.RELATED_GAP_ROWSPEC}));
        
        comboBoxParent = new JComboBox();
        comboBoxParent.setModel(new FeederParentsComboBoxModel(feeder));
                
        JLabel lblParent = new JLabel("Parent");
        panelPart.add(lblParent, "2, 2, right, default");
        panelPart.add(comboBoxParent, "4, 2, left, default");

        comboBoxPart = new JComboBox();
        comboBoxPart.setModel(new PartsComboBoxModel());
        comboBoxPart.setRenderer(new IdentifiableListCellRenderer<Part>());

        JLabel lblFiducial = new JLabel("Fiducial");
        panelPart.add(lblFiducial, "2, 4, right, default");
        panelPart.add(comboBoxPart, "4, 4, left, default");



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
		        ColumnSpec.decode("default:grow"),},
		    new RowSpec[] {
		        FormSpecs.RELATED_GAP_ROWSPEC,
		        FormSpecs.DEFAULT_ROWSPEC,
		        FormSpecs.RELATED_GAP_ROWSPEC,
		        FormSpecs.DEFAULT_ROWSPEC,
		        FormSpecs.RELATED_GAP_ROWSPEC,
		        RowSpec.decode("default:grow"),
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
		
        btnSetLocationWithFiducials = new JButton(new AbstractAction("Set Location Using Fiducials") {
            @Override
            public void actionPerformed(ActionEvent e) {
                ((TitledBorder )panel.getBorder()).setTitle("Jog camera over Fiducial A and Click Ok");
                panel.setVisible(true);
                fid1 = true;
                btnSetLocationWithFiducials.setEnabled(false);
            }
        });
        btnSetLocationWithFiducials.setHorizontalAlignment(SwingConstants.LEFT);
        panelLocation.add(btnSetLocationWithFiducials, "14, 4, left, default");
        
        panel = new JPanel();
        panel.setVisible(false);
        panel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null),
                "Jog camera over Fiducial1 and Click Ok", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(255, 0, 0)));
        panelLocation.add(panel, "4, 6, fill, fill");
        panel.setLayout(new FormLayout(
                new ColumnSpec[] { 
                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"),
                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("left:default:grow"), },
                new RowSpec[] { 
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC, 
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC }));



        JButton btnOk = new JButton(new AbstractAction("Ok") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (fid1) {
                        UiUtils.submitUiMachineTask(() -> {
                            fid1Captured = MainFrame.get().getMachineControls().getSelectedNozzle().getHead().getDefaultCamera().getLocation();
                            Part fidPart = (Part) comboBoxPart.getSelectedItem();
                            if (fidPart != null) {
                                Configuration.get().getMachine().getFiducialLocator()
                                        .getHomeFiducialLocation(fid1Captured, fidPart);
                                fid1Captured = MainFrame.get().getMachineControls().getSelectedNozzle().getHead().getDefaultCamera().getLocation();
                            }
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    Logger.trace("Fid1 capture location = " + fid1Captured);
                                    ((TitledBorder )panel.getBorder()).setTitle("Jog camera over Fiducial B and Click Ok");
                                    panel.repaint();
                                }
                            });
                        });
                        fid1 = false;
                    } else {
                        UiUtils.submitUiMachineTask(() -> {
                            fid2Captured = MainFrame.get().getMachineControls().getSelectedNozzle().getHead().getDefaultCamera().getLocation();
                            Part fidPart = (Part) comboBoxPart.getSelectedItem();
                            if (fidPart != null) {
                                Configuration.get().getMachine().getFiducialLocator()
                                        .getHomeFiducialLocation(fid2Captured, fidPart);
                                fid2Captured = MainFrame.get().getMachineControls().getSelectedNozzle().getHead().getDefaultCamera().getLocation();
                            }
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    Logger.trace("Fid2 capture location = " + fid2Captured);
                                    ((TitledBorder )panel.getBorder()).setTitle("Jog camera over Fiducial A and Click Ok");
                                    panel.setVisible(false);
                                    fid1 = true;
                                    btnSetLocationWithFiducials.setEnabled(true);
                                    computeLocationFromFiducials();
                                }
                            });
                        });
                   }
                }
            });
        btnOk.setHorizontalAlignment(SwingConstants.LEFT);
        btnOk.setForeground(new Color(255,0,0));
        panel.add(btnOk, "2, 2, left, default");
    
    
    
        JButton btnCancel = new JButton(new AbstractAction("Cancel") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    ((TitledBorder )panel.getBorder()).setTitle("Jog camera over Fiducial1 and Click Ok");
                    panel.setVisible(false);
                    fid1 = true;
                    btnSetLocationWithFiducials.setEnabled(true);
                }
            });
        btnOk.setHorizontalAlignment(SwingConstants.LEFT);
        panel.add(btnCancel, "4, 2, left, default");



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

        JLabel fid1Label = new JLabel("Fiducial A            ");
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

        locationButtonsPanelFid1 = new LocationButtonsPanel(textFieldFid1X, textFieldFid1Y, null, null);
        locationButtonsPanelFid1.setBaseLocation(feeder.getLocation());
        panelParameters.add(locationButtonsPanelFid1, "8, 4");

        JButton btnFid1Vision = new JButton(new AbstractAction("Set Using Vision Pipeline") {
            @Override
            public void actionPerformed(ActionEvent e) {
                UiUtils.submitUiMachineTask(() -> {
                    Location fid1Captured = MainFrame.get().getMachineControls().getSelectedNozzle().getHead().
                            getDefaultCamera().getLocation();
                    Part fidPart = (Part) comboBoxPart.getSelectedItem();
                    if (fidPart != null) {
                        Configuration.get().getMachine().getFiducialLocator()
                                .getHomeFiducialLocation(fid1Captured, fidPart);
                        fid1Captured = MainFrame.get().getMachineControls().getSelectedNozzle().getHead().
                                getDefaultCamera().getLocation();
                    }
                    final Location fid1 = fid1Captured;
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            Location fid1Captured = ReferenceFeeder.convertToLocalLocation(upToDateLocation.getLocation(), fid1);
                            Helpers.copyLocationIntoTextFields(fid1Captured, 
                                    textFieldFid1X, 
                                    textFieldFid1Y, 
                                    null,
                                    null);
                        }
                    });
                });
            }
        });
        btnFid1Vision.setHorizontalAlignment(SwingConstants.LEFT);
        btnFid1Vision.setToolTipText("Manually jog camera over approximate location of the fiducial and then " + 
                "click this button to use the vision pipeline to capture the exact center.  WARNING - may cause machine motion!");
        btnFid1Vision.setForeground(new Color(255,0,0));
        panelParameters.add(btnFid1Vision, "10, 4, left, default");



        
        JLabel fid2Label = new JLabel("Fiducial B            ");
        panelParameters.add(fid2Label, "2, 6");

        textFieldFid2X = new JTextField();
        textFieldFid2X.setColumns(6);
        panelParameters.add(textFieldFid2X, "4, 6");

        textFieldFid2Y = new JTextField();
        textFieldFid2Y.setColumns(6);
        panelParameters.add(textFieldFid2Y, "6, 6");

        locationButtonsPanelFid2 = new LocationButtonsPanel(textFieldFid2X, textFieldFid2Y, null, null);
        locationButtonsPanelFid2.setBaseLocation(feeder.getLocation());
        panelParameters.add(locationButtonsPanelFid2, "8, 6");

        JButton btnFid2Vision = new JButton(new AbstractAction("Set Using Vision Pipeline") {
            @Override
            public void actionPerformed(ActionEvent e) {
                UiUtils.submitUiMachineTask(() -> {
                    Location fid2Captured = MainFrame.get().getMachineControls().getSelectedNozzle().getHead().
                            getDefaultCamera().getLocation();
                    Part fidPart = (Part) comboBoxPart.getSelectedItem();
                    if (fidPart != null) {
                        Configuration.get().getMachine().getFiducialLocator()
                                .getHomeFiducialLocation(fid2Captured, fidPart);
                        fid2Captured = MainFrame.get().getMachineControls().getSelectedNozzle().getHead().
                                getDefaultCamera().getLocation();
                    }
                    final Location fid2 = fid2Captured;
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            Location fid2Captured = ReferenceFeeder.convertToLocalLocation(upToDateLocation.getLocation(), fid2);
                            Helpers.copyLocationIntoTextFields(fid2Captured, 
                                    textFieldFid2X, 
                                    textFieldFid2Y, 
                                    null,
                                    null);
                        }
                    });
                });
            }
        });
        btnFid2Vision.setHorizontalAlignment(SwingConstants.LEFT);
        btnFid2Vision.setToolTipText("Manually jog camera over approximate location of the fiducial and then " + 
                "click this button to use the vision pipeline to capture the exact center.  WARNING - may cause machine motion!");
        btnFid2Vision.setForeground(new Color(255,0,0));
        panelParameters.add(btnFid2Vision, "10, 6, left, default");



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
        addWrappedBinding(feeder, "part", comboBoxPart, "selectedItem");
        
		MutableLocationProxy location = new MutableLocationProxy();
		bind(UpdateStrategy.READ_WRITE, feeder, "location", location, "location");
        addWrappedBinding(location, "lengthX", textFieldLocationX, "text", lengthConverter);
        addWrappedBinding(location, "lengthY", textFieldLocationY, "text", lengthConverter);
        addWrappedBinding(location, "lengthZ", textFieldLocationZ, "text", lengthConverter);
        addWrappedBinding(location, "rotation", textFieldLocationR, "text", doubleConverter);

        upToDateLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_ONCE, feeder, "location", upToDateLocation, "location");
        bind(UpdateStrategy.READ_WRITE, upToDateLocation, "lengthX", textFieldLocationX, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, upToDateLocation, "lengthY", textFieldLocationY, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, upToDateLocation, "lengthZ", textFieldLocationZ, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, upToDateLocation, "rotation", textFieldLocationR, "text", doubleConverter);
        bind(UpdateStrategy.READ, upToDateLocation, "location", locationButtonsPanelFid1, "baseLocation");
        bind(UpdateStrategy.READ, upToDateLocation, "location", locationButtonsPanelFid2, "baseLocation");
        
        
        fid1LocalLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "expectedFiducial1", fid1LocalLocation,
                "location");
        addWrappedBinding(fid1LocalLocation, "lengthX", textFieldFid1X, "text",
                lengthConverter);
        addWrappedBinding(fid1LocalLocation, "lengthY", textFieldFid1Y, "text",
                lengthConverter);

        fid2LocalLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "expectedFiducial2", fid2LocalLocation,
                "location");
        addWrappedBinding(fid2LocalLocation, "lengthX", textFieldFid2X, "text",
                lengthConverter);
        addWrappedBinding(fid2LocalLocation, "lengthY", textFieldFid2Y, "text",
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

	private void computeLocationFromFiducials()  {
	    double[][] source = { {fid1LocalLocation.getLocation().getX(),fid2LocalLocation.getLocation().getX()},
	                          {fid1LocalLocation.getLocation().getY(),fid2LocalLocation.getLocation().getY()} };
        double[][] dest = { {fid1Captured.getX(),fid2Captured.getX()},
                            {fid1Captured.getY(),fid2Captured.getY()} };
        
//	    Utils2D.testComputeScalingRotationAndTranslation();
//	    double[][] source = { {-1.0, 1.0, 0.0}, {-1.0, -1.0, 2.0} };
//	    double[][] dest =   { {1.0, 1.0, -2.0}, {-0.25, 0.25, 0.0} }; 
//        AffineTransform at = Utils2D.deriveAffineTransform(source, dest);
//        Logger.trace("Transform = " + at);
        
        
        double rotAngleDeg = Math.toDegrees( Math.atan2(dest[1][0] - dest[1][1], dest[0][0] - dest[0][1]) - 
                Math.atan2(source[1][0] - source[1][1], source[0][0] - source[0][1] ) );
      
        Location sourceMid = new Location(LengthUnit.Millimeters, (source[0][0] + source[0][1]) / 2.0,
                (source[1][0] + source[1][1]) / 2.0, 0.0, 0.0);
      
        Location destMid = new Location(LengthUnit.Millimeters, (dest[0][0] + dest[0][1]) / 2.0,
                (dest[1][0] + dest[1][1]) / 2.0, 0.0, rotAngleDeg);
      
        Location newLocation = destMid.subtract(sourceMid).rotateXyCenterPoint(destMid, rotAngleDeg)
                .derive(upToDateLocation.getLocation(), false, false, true, false);
        Logger.trace("New Location = " + newLocation);
        upToDateLocation.setLocation(newLocation);
    }
	
	
	class FeederConverter extends Converter<String, Object> {

        @Override
        public Object convertForward(String arg0) {
            if (arg0.equals(Feeder.ROOT_FEEDER_ID)) {
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
