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

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.Converter;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.components.CameraViewActionEvent;
import org.openpnp.gui.components.CameraViewActionListener;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.FeederParentsComboBoxModel;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.IdentifiableListCellRenderer;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.gui.support.PartsComboBoxModel;
import org.openpnp.machine.reference.ReferenceFeeder;
import org.openpnp.machine.reference.feeder.ReferenceFeederGroup;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Feeder;
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

	private JTextField textFieldLocationX;
    private JTextField textFieldLocationY;
    private JTextField textFieldLocationR;
    
    private JTextField textFieldFid1X;
    private JTextField textFieldFid1Y;
    
    private JTextField textFieldFid2X;
    private JTextField textFieldFid2Y;
    
	private JPanel panelPart;

    private JComboBox<?> comboBoxParent;
    private JComboBox<?> comboBoxPart;
    private JButton btnSetLocationWithFiducials;
    private JButton btnFineTuneLocationWithFiducials;
    private Location fid1Captured;
    private Location fid2Captured;
    private JButton btnDefineFiducialLocations;
    
    private MutableLocationProxy upToDateLocation;
    private MutableLocationProxy upToDateFid1LocalLocation;
    private MutableLocationProxy upToDateFid2LocalLocation;
    private LocationButtonsPanel locationButtonsPanelFid1;
    private LocationButtonsPanel locationButtonsPanelFid2;

    private Camera autoSetupCamera;


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
        
        boolean hasChildren = feeder.hasChildren();
		
		JPanel warningPanel = new JPanel();
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

        JLabel lblR_1 = new JLabel("Rot");
        panelLocation.add(lblR_1, "8, 2");

        textFieldLocationX = new JTextField();
        textFieldLocationX.setColumns(6);
        textFieldLocationX.setEnabled(!hasChildren);
        panelLocation.add(textFieldLocationX, "4, 4");

        textFieldLocationY = new JTextField();
        textFieldLocationY.setColumns(6);
        textFieldLocationY.setEnabled(!hasChildren);
        panelLocation.add(textFieldLocationY, "6, 4");

        textFieldLocationR = new JTextField();
        textFieldLocationR.setColumns(6);
        textFieldLocationR.setEnabled(!hasChildren);
        panelLocation.add(textFieldLocationR, "8, 4");

        LocationButtonsPanel locationButtonsPanel = new LocationButtonsPanel(textFieldLocationX, textFieldLocationY, null, textFieldLocationR);
        locationButtonsPanel.setEnabledCapture(!hasChildren);
		panelLocation.add(locationButtonsPanel, "10, 4");
		
        btnSetLocationWithFiducials = new JButton(setLocationWithFiducials);
        btnSetLocationWithFiducials.setHorizontalAlignment(SwingConstants.LEFT);
        btnSetLocationWithFiducials.setToolTipText("<html><p width=\"400\">" + 
                "Use to manually re-align the feeder group after it has been moved.  Follow instructions in the Down Camera view..." +
                "</p></html>");
        panelLocation.add(btnSetLocationWithFiducials, "12, 4, left, default");
        
        btnFineTuneLocationWithFiducials = new JButton(fineTuneLocationWithFiducials);
        btnFineTuneLocationWithFiducials.setHorizontalAlignment(SwingConstants.LEFT);
        btnFineTuneLocationWithFiducials.setToolTipText("<html><p width=\"400\">" + 
                "Attempts to automatically re-align the feeder group after it has been moved slightly.  If the fiducials can't be located automatically, it falls back to the manual method (follow instructions in the Down Camera view...)" +
                "</p></html>");
        panelLocation.add(btnFineTuneLocationWithFiducials, "14, 4, left, default");
        

		panelParameters = new JPanel();
		panelParameters.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null),
				"Definition of Fiducial Locations (Local Coordinates)", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
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
        textFieldFid1X.setEnabled(!hasChildren);
        panelParameters.add(textFieldFid1X, "4, 4");

        textFieldFid1Y = new JTextField();
        textFieldFid1Y.setColumns(6);
        textFieldFid1Y.setEnabled(!hasChildren);
        panelParameters.add(textFieldFid1Y, "6, 4");

        locationButtonsPanelFid1 = new LocationButtonsPanel(textFieldFid1X, textFieldFid1Y, null, null);
        locationButtonsPanelFid1.setBaseLocation(feeder.getLocation());
        locationButtonsPanelFid1.setEnabledCapture(!hasChildren);
        panelParameters.add(locationButtonsPanelFid1, "8, 4");

        btnDefineFiducialLocations = new JButton(defineFiducialLocations);
        btnDefineFiducialLocations.setHorizontalAlignment(SwingConstants.LEFT);
        btnDefineFiducialLocations.setToolTipText("<html><p width=\"400\">" + 
                "Use vision to define the local locations of the fiducials of a new feeder group.  Follow instructions in the Down Camera view..." +
                "</p></html>");
        btnDefineFiducialLocations.setEnabled(!hasChildren);
        panelParameters.add(btnDefineFiducialLocations, "10, 4, 1, 3, left, center");

        
        JLabel fid2Label = new JLabel("Fiducial B            ");
        panelParameters.add(fid2Label, "2, 6");

        textFieldFid2X = new JTextField();
        textFieldFid2X.setColumns(6);
        textFieldFid2X.setEnabled(!hasChildren);
        panelParameters.add(textFieldFid2X, "4, 6");

        textFieldFid2Y = new JTextField();
        textFieldFid2Y.setColumns(6);
        textFieldFid2Y.setEnabled(!hasChildren);
        panelParameters.add(textFieldFid2Y, "6, 6");

        locationButtonsPanelFid2 = new LocationButtonsPanel(textFieldFid2X, textFieldFid2Y, null, null);
        locationButtonsPanelFid2.setBaseLocation(feeder.getLocation());
        locationButtonsPanelFid2.setEnabledCapture(!hasChildren);
        panelParameters.add(locationButtonsPanelFid2, "8, 6");
	}

	
	@Override
	public void createBindings() {
		LengthConverter lengthConverter = new LengthConverter();
		FeederConverter feederConverter = new FeederConverter();
		DoubleConverter doubleConverter = new DoubleConverter(Configuration.get().getLengthDisplayFormat());
		
        addWrappedBinding(feeder, "parentId", comboBoxParent, "selectedItem", feederConverter);
        addWrappedBinding(feeder, "part", comboBoxPart, "selectedItem");
        
		MutableLocationProxy location = new MutableLocationProxy();
		bind(UpdateStrategy.READ_WRITE, feeder, "location", location, "location");
        addWrappedBinding(location, "lengthX", textFieldLocationX, "text", lengthConverter);
        addWrappedBinding(location, "lengthY", textFieldLocationY, "text", lengthConverter);
        addWrappedBinding(location, "rotation", textFieldLocationR, "text", doubleConverter);

        upToDateLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_ONCE, feeder, "location", upToDateLocation, "location");
        bind(UpdateStrategy.READ_WRITE, upToDateLocation, "lengthX", textFieldLocationX, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, upToDateLocation, "lengthY", textFieldLocationY, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, upToDateLocation, "rotation", textFieldLocationR, "text", doubleConverter);
        bind(UpdateStrategy.READ, upToDateLocation, "location", locationButtonsPanelFid1, "baseLocation");
        bind(UpdateStrategy.READ, upToDateLocation, "location", locationButtonsPanelFid2, "baseLocation");
        
        
        MutableLocationProxy fid1LocalLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "expectedFiducial1", fid1LocalLocation,
                "location");
        addWrappedBinding(fid1LocalLocation, "lengthX", textFieldFid1X, "text",
                lengthConverter);
        addWrappedBinding(fid1LocalLocation, "lengthY", textFieldFid1Y, "text",
                lengthConverter);

        upToDateFid1LocalLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_ONCE, feeder, "expectedFiducial1", upToDateFid1LocalLocation, "location");
        bind(UpdateStrategy.READ_WRITE, upToDateFid1LocalLocation, "lengthX", textFieldFid1X, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, upToDateFid1LocalLocation, "lengthY", textFieldFid1Y, "text", lengthConverter);
        
        MutableLocationProxy fid2LocalLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "expectedFiducial2", fid2LocalLocation,
                "location");
        addWrappedBinding(fid2LocalLocation, "lengthX", textFieldFid2X, "text",
                lengthConverter);
        addWrappedBinding(fid2LocalLocation, "lengthY", textFieldFid2Y, "text",
                lengthConverter);

        upToDateFid2LocalLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_ONCE, feeder, "expectedFiducial2", upToDateFid2LocalLocation, "location");
        bind(UpdateStrategy.READ_WRITE, upToDateFid2LocalLocation, "lengthX", textFieldFid2X, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, upToDateFid2LocalLocation, "lengthY", textFieldFid2Y, "text", lengthConverter);
        

		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldLocationX);
		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldLocationY);
		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldLocationR);
		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFid1X);
		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFid1Y);
		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFid2X);
		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFid2Y);

	}

    private Action setLocationWithFiducials = new AbstractAction("Set Location With Fiducials") {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (upToDateFid1LocalLocation.getLocation().equals(upToDateFid2LocalLocation.getLocation())) {
                MessageBoxes.infoBox("Can't set location" , "Fiducial locations need to be defined first");
                return;
            }
            try {
                autoSetupCamera = Configuration.get()
                                               .getMachine()
                                               .getDefaultHead()
                                               .getDefaultCamera();
            }
            catch (Exception ex) {
                MessageBoxes.errorBox(getTopLevelAncestor(), "Set Location With Fiducials Failure", ex);
                return;
            }

            btnSetLocationWithFiducials.setAction(setLocationWithFiducialsCancel);

            CameraView cameraView = MainFrame.get()
                                             .getCameraViews()
                                             .getCameraView(autoSetupCamera);
            cameraView.addActionListener(setLocationWithFiducialsAClicked);
            cameraView.setText("Click on " + feeder.getName() + " Fiducial A.");
            cameraView.flash();
            cameraView.flash();
            cameraView.flash();
        }
    };

    private Action setLocationWithFiducialsCancel = new AbstractAction("Cancel Set Location With Fiducials") {
        @Override
        public void actionPerformed(ActionEvent e) {
            btnSetLocationWithFiducials.setAction(setLocationWithFiducials);
            btnFineTuneLocationWithFiducials.setAction(fineTuneLocationWithFiducials);
            CameraView cameraView = MainFrame.get()
                                             .getCameraViews()
                                             .getCameraView(autoSetupCamera);
            cameraView.setText(null);
            cameraView.setCameraViewFilter(null);
            cameraView.removeActionListener(setLocationWithFiducialsAClicked);
            cameraView.removeActionListener(setLocationWithFiducialsBClicked);
        }
    };

    private CameraViewActionListener setLocationWithFiducialsAClicked = new CameraViewActionListener() {
        @Override
        public void actionPerformed(final CameraViewActionEvent action) {
            fid1Captured = action.getLocation();
            final CameraView cameraView = MainFrame.get()
                                                   .getCameraViews()
                                                   .getCameraView(autoSetupCamera);
            cameraView.removeActionListener(this);
            Part fidPart = (Part) comboBoxPart.getSelectedItem();
            if (fidPart != null) {
                UiUtils.submitUiMachineTask(() -> {
                    cameraView.setText("Checking Fiducial A...");
                    Configuration.get().getMachine().getFiducialLocator()
                        .getHomeFiducialLocation(fid1Captured, fidPart);
                    fid1Captured = MainFrame.get().getMachineControls().getSelectedNozzle().getHead().getDefaultCamera().getLocation();
                    cameraView.setText(
                            "Now click on Fiducial B.");
                    cameraView.flash();
                    cameraView.flash();
                    cameraView.flash();
                });
            } else {
                cameraView.setText(
                        "Now click on Fiducial B.");
                cameraView.flash();
                cameraView.flash();
                cameraView.flash();
            }

            cameraView.addActionListener(setLocationWithFiducialsBClicked);
        }
    };

    private CameraViewActionListener setLocationWithFiducialsBClicked = new CameraViewActionListener() {
        @Override
        public void actionPerformed(final CameraViewActionEvent action) {
            fid2Captured = action.getLocation();
            final CameraView cameraView = MainFrame.get()
                                                   .getCameraViews()
                                                   .getCameraView(autoSetupCamera);
            cameraView.removeActionListener(this);
            Part fidPart = (Part) comboBoxPart.getSelectedItem();
            if (fidPart != null) {
                UiUtils.submitUiMachineTask(() -> {
                    cameraView.setText("Checking Fiducial B...");
                    Configuration.get().getMachine().getFiducialLocator()
                        .getHomeFiducialLocation(fid2Captured, fidPart);
                    fid2Captured = MainFrame.get().getMachineControls().getSelectedNozzle().getHead().getDefaultCamera().getLocation();
                    computeLocationFromFiducials();
                    cameraView.setText("Setup complete!");
                    cameraView.flash();
                    cameraView.flash();
                    cameraView.flash();
                    Thread.sleep(1500);
                    cameraView.setText(null);
                    cameraView.setCameraViewFilter(null);
                    btnSetLocationWithFiducials.setAction(setLocationWithFiducials);
                });
            }
        }
    };

    private Action fineTuneLocationWithFiducials = new AbstractAction("Fine Tune Location") {
        @Override
        public void actionPerformed(ActionEvent e) {
            Part fidPart = (Part) comboBoxPart.getSelectedItem();
            if (fidPart == null) {
                MessageBoxes.infoBox("Can't fine tune location" , "Select a fiducial part first");
                return;
            }
            if (upToDateFid1LocalLocation.getLocation().equals(upToDateFid2LocalLocation.getLocation())) {
                MessageBoxes.infoBox("Can't fine tune location" , "Fiducial locations need to be defined first");
                return;
            }
            try {
                autoSetupCamera = Configuration.get()
                                               .getMachine()
                                               .getDefaultHead()
                                               .getDefaultCamera();
            }
            catch (Exception ex) {
                MessageBoxes.errorBox(getTopLevelAncestor(), "Fine Tune Location With Fiducials Failure", ex);
                return;
            }

            btnFineTuneLocationWithFiducials.setAction(setLocationWithFiducialsCancel);

            CameraView cameraView = MainFrame.get()
                                             .getCameraViews()
                                             .getCameraView(autoSetupCamera);
            cameraView.setText("Attempting to locate " + feeder.getName() + " Fiducial A.");
            cameraView.flash();
            cameraView.flash();
            cameraView.flash();
            fid1Captured = ReferenceFeeder.convertToGlobalLocation(upToDateLocation.getLocation(), upToDateFid1LocalLocation.getLocation());
            UiUtils.submitUiMachineTask(() -> {
                fid1Captured = Configuration.get().getMachine().getFiducialLocator()
                    .getHomeFiducialLocation(fid1Captured, fidPart);
                if (fid1Captured == null) {
                    cameraView.setText("Failed to find " + feeder.getName() + " Fiducial A.");
                    cameraView.flash();
                    cameraView.flash();
                    cameraView.flash();
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            defineFiducialLocations.actionPerformed(e);
                        }
                    });
                    return;
                }
                cameraView.setText("Attempting to locate " + feeder.getName() + " Fiducial B.");
                cameraView.flash();
                cameraView.flash();
                cameraView.flash();
                fid2Captured = ReferenceFeeder.convertToGlobalLocation(upToDateLocation.getLocation(), upToDateFid2LocalLocation.getLocation());
                fid2Captured = Configuration.get().getMachine().getFiducialLocator()
                    .getHomeFiducialLocation(fid2Captured, fidPart);
                if (fid1Captured == null) {
                    cameraView.setText("Failed to find " + feeder.getName() + " Fiducial B.");
                    cameraView.flash();
                    cameraView.flash();
                    cameraView.flash();
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            defineFiducialLocations.actionPerformed(e);
                        }
                    });
                    return;
                }
                computeLocationFromFiducials();
                cameraView.setText("Fine tune complete!");
                cameraView.flash();
                cameraView.flash();
                cameraView.flash();
                Thread.sleep(1500);
                cameraView.setText(null);
                cameraView.setCameraViewFilter(null);
                btnFineTuneLocationWithFiducials.setAction(fineTuneLocationWithFiducials);
            });
        }
    };

    private void computeLocationFromFiducials()  {
        LengthUnit units = upToDateFid1LocalLocation.getLocation().getUnits();
	    double[][] source = { {upToDateFid1LocalLocation.getLocation().getX(),upToDateFid2LocalLocation.getLocation().convertToUnits(units).getX()},
	                          {upToDateFid1LocalLocation.getLocation().getY(),upToDateFid2LocalLocation.getLocation().convertToUnits(units).getY()} };
        double[][] dest = { {fid1Captured.convertToUnits(units).getX(),fid2Captured.convertToUnits(units).getX()},
                            {fid1Captured.convertToUnits(units).getY(),fid2Captured.convertToUnits(units).getY()} };
        
        double rotAngleDeg = Math.toDegrees( Math.atan2(dest[1][0] - dest[1][1], dest[0][0] - dest[0][1]) - 
                Math.atan2(source[1][0] - source[1][1], source[0][0] - source[0][1] ) );
      
        Location sourceMid = new Location(units, (source[0][0] + source[0][1]) / 2.0,
                (source[1][0] + source[1][1]) / 2.0, 0.0, 0.0);
      
        Location destMid = new Location(units, (dest[0][0] + dest[0][1]) / 2.0,
                (dest[1][0] + dest[1][1]) / 2.0, 0.0, rotAngleDeg);
      
        Location newLocation = destMid.subtract(sourceMid).rotateXyCenterPoint(destMid, rotAngleDeg)
                .derive(upToDateLocation.getLocation(), false, false, true, false);

        upToDateLocation.setLocation(newLocation);
    }
	
    private Action defineFiducialLocations = new AbstractAction("Define Fiducial Locations Using Vision") {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                autoSetupCamera = Configuration.get()
                                               .getMachine()
                                               .getDefaultHead()
                                               .getDefaultCamera();
            }
            catch (Exception ex) {
                MessageBoxes.errorBox(getTopLevelAncestor(), "Define Fiducial Locations Failure", ex);
                return;
            }

            btnDefineFiducialLocations.setAction(defineFiducialLocationsCancel);

            CameraView cameraView = MainFrame.get()
                                             .getCameraViews()
                                             .getCameraView(autoSetupCamera);
            cameraView.addActionListener(defineFiducialLocationsAClicked);
            cameraView.setText("Click on " + feeder.getName() + " Fiducial A.");
            cameraView.flash();
            cameraView.flash();
            cameraView.flash();
        }
    };
    
    private Action defineFiducialLocationsCancel = new AbstractAction("Cancel Define Fiducial Locations") {
        @Override
        public void actionPerformed(ActionEvent e) {
            btnDefineFiducialLocations.setAction(defineFiducialLocations);
            CameraView cameraView = MainFrame.get()
                                             .getCameraViews()
                                             .getCameraView(autoSetupCamera);
            cameraView.setText(null);
            cameraView.setCameraViewFilter(null);
            cameraView.removeActionListener(defineFiducialLocationsAClicked);
            cameraView.removeActionListener(defineFiducialLocationsBClicked);
        }
    };

    private CameraViewActionListener defineFiducialLocationsAClicked = new CameraViewActionListener() {
        @Override
        public void actionPerformed(final CameraViewActionEvent action) {
            fid1Captured = action.getLocation();
            final CameraView cameraView = MainFrame.get()
                                                   .getCameraViews()
                                                   .getCameraView(autoSetupCamera);
            cameraView.removeActionListener(this);
            Part fidPart = (Part) comboBoxPart.getSelectedItem();
            if (fidPart != null) {
                UiUtils.submitUiMachineTask(() -> {
                    cameraView.setText("Checking Fiducial A...");
                    Configuration.get().getMachine().getFiducialLocator()
                        .getHomeFiducialLocation(fid1Captured, fidPart);
                    fid1Captured = MainFrame.get().getMachineControls().getSelectedNozzle().getHead().getDefaultCamera().getLocation();
                    cameraView.setText(
                            "Now click on Fiducial B.");
                    cameraView.flash();
                    cameraView.flash();
                    cameraView.flash();
                });
            } else {
                cameraView.setText(
                        "Now click on Fiducial B.");
                cameraView.flash();
                cameraView.flash();
                cameraView.flash();
            }

            cameraView.addActionListener(defineFiducialLocationsBClicked);
        }
    };

    private CameraViewActionListener defineFiducialLocationsBClicked = new CameraViewActionListener() {
        @Override
        public void actionPerformed(final CameraViewActionEvent action) {
            fid2Captured = action.getLocation();
            final CameraView cameraView = MainFrame.get()
                                                   .getCameraViews()
                                                   .getCameraView(autoSetupCamera);
            cameraView.removeActionListener(this);
            Part fidPart = (Part) comboBoxPart.getSelectedItem();
            if (fidPart != null) {
                UiUtils.submitUiMachineTask(() -> {
                    cameraView.setText("Checking Fiducial B...");
                    Configuration.get().getMachine().getFiducialLocator()
                        .getHomeFiducialLocation(fid2Captured, fidPart);
                    fid2Captured = MainFrame.get().getMachineControls().getSelectedNozzle().getHead().getDefaultCamera().getLocation();
                    final Location fid1 = fid1Captured;
                    final Location fid2 = fid2Captured;
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            Location fid1Captured = ReferenceFeeder.convertToLocalLocation(upToDateLocation.getLocation(), fid1);
                            Helpers.copyLocationIntoTextFields(fid1Captured, 
                                    textFieldFid1X, 
                                    textFieldFid1Y, 
                                    null,
                                    null);
                            Location fid2Captured = ReferenceFeeder.convertToLocalLocation(upToDateLocation.getLocation(), fid2);
                            Helpers.copyLocationIntoTextFields(fid2Captured, 
                                    textFieldFid2X, 
                                    textFieldFid2Y, 
                                    null,
                                    null);
                        }
                    });
                    cameraView.setText("Setup complete!");
                    cameraView.flash();
                    cameraView.flash();
                    cameraView.flash();
                    Thread.sleep(1500);
                    cameraView.setText(null);
                    cameraView.setCameraViewFilter(null);
                    btnDefineFiducialLocations.setAction(defineFiducialLocations);
                });
            }
        }
    };

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
