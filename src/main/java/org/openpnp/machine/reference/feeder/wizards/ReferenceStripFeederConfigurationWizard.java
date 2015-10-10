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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.components.CameraViewActionEvent;
import org.openpnp.gui.components.CameraViewActionListener;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.IdentifiableListCellRenderer;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.gui.support.PartsComboBoxModel;
import org.openpnp.machine.reference.feeder.ReferenceStripFeeder;
import org.openpnp.machine.reference.feeder.ReferenceStripFeeder.TapeType;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Camera;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.util.VisionUtils;

import com.google.common.collect.Lists;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class ReferenceStripFeederConfigurationWizard extends
        AbstractConfigurationWizard {
    private final ReferenceStripFeeder feeder;

    private JPanel panelPart;

    private JComboBox comboBoxPart;

    private JTextField textFieldFeedStartX;
    private JTextField textFieldFeedStartY;
    private JTextField textFieldFeedStartZ;
    private JTextField textFieldFeedEndX;
    private JTextField textFieldFeedEndY;
    private JTextField textFieldFeedEndZ;
    private JTextField textFieldTapeWidth;
    private JLabel lblPartPitch;
    private JTextField textFieldPartPitch;
    private JPanel panelTapeSettings;
    private JPanel panelLocations;
    private LocationButtonsPanel locationButtonsPanelFeedStart;
    private LocationButtonsPanel locationButtonsPanelFeedEnd;
    private JLabel lblFeedCount;
    private JTextField textFieldFeedCount;
    private JButton btnResetFeedCount;
    private JLabel lblTapeType;
    private JComboBox comboBoxTapeType;
    private JLabel lblRotationInTape;
    private JTextField textFieldLocationRotation;
    private JButton btnAutoSetup;

    public ReferenceStripFeederConfigurationWizard(ReferenceStripFeeder feeder) {
        this.feeder = feeder;

        panelPart = new JPanel();
        panelPart.setBorder(new TitledBorder(null, "Part",
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelPart);
        panelPart
                .setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));

        comboBoxPart = new JComboBox();
        try {
            comboBoxPart.setModel(new PartsComboBoxModel());
        }
        catch (Throwable t) {
            // Swallow this error. This happens during parsing in
            // in WindowBuilder but doesn't happen during normal run.
        }
        comboBoxPart.setRenderer(new IdentifiableListCellRenderer<Part>());
        panelPart.add(comboBoxPart, "2, 2, 3, 1, left, default");
        
        lblRotationInTape = new JLabel("Rotation In Tape");
        panelPart.add(lblRotationInTape, "2, 4, left, default");
        
        textFieldLocationRotation = new JTextField();
        panelPart.add(textFieldLocationRotation, "4, 4, fill, default");
        textFieldLocationRotation.setColumns(4);

        panelTapeSettings = new JPanel();
        contentPanel.add(panelTapeSettings);
        panelTapeSettings.setBorder(new TitledBorder(new EtchedBorder(
                EtchedBorder.LOWERED, null, null), "Tape Settings",
                TitledBorder.LEADING, TitledBorder.TOP, null,
                new Color(0, 0, 0)));
        panelTapeSettings.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
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
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));

        lblTapeType = new JLabel("Tape Type");
        panelTapeSettings.add(lblTapeType, "2, 2, right, default");

        comboBoxTapeType = new JComboBox(TapeType.values());
        panelTapeSettings.add(comboBoxTapeType, "4, 2, fill, default");

        JLabel lblTapeWidth = new JLabel("Tape Width");
        panelTapeSettings.add(lblTapeWidth, "2, 4");

        textFieldTapeWidth = new JTextField();
        panelTapeSettings.add(textFieldTapeWidth, "4, 4");
        textFieldTapeWidth.setColumns(5);

        lblPartPitch = new JLabel("Part Pitch");
        panelTapeSettings.add(lblPartPitch, "2, 6, right, default");

        textFieldPartPitch = new JTextField();
        panelTapeSettings.add(textFieldPartPitch, "4, 6");
        textFieldPartPitch.setColumns(5);

        lblFeedCount = new JLabel("Feed Count");
        panelTapeSettings.add(lblFeedCount, "2, 8");

        textFieldFeedCount = new JTextField();
        panelTapeSettings.add(textFieldFeedCount, "4, 8");
        textFieldFeedCount.setColumns(10);

        btnResetFeedCount = new JButton(new AbstractAction("Reset") {
            @Override
            public void actionPerformed(ActionEvent e) {
                textFieldFeedCount.setText("0");
                applyAction.actionPerformed(e);
            }
        });
        panelTapeSettings.add(btnResetFeedCount, "6, 8");
        
        btnAutoSetup = new JButton(autoSetup);
        panelTapeSettings.add(btnAutoSetup, "4, 10");

        panelLocations = new JPanel();
        contentPanel.add(panelLocations);
        panelLocations.setBorder(new TitledBorder(null, "Locations",
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        panelLocations
                .setLayout(new FormLayout(new ColumnSpec[] {
                        FormSpecs.RELATED_GAP_COLSPEC,
                        FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC,
                        FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC,
                        FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC,
                        FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC,
                        ColumnSpec.decode("left:default:grow"), },
                        new RowSpec[] { FormSpecs.RELATED_GAP_ROWSPEC,
                                FormSpecs.DEFAULT_ROWSPEC,
                                FormSpecs.RELATED_GAP_ROWSPEC,
                                FormSpecs.DEFAULT_ROWSPEC,
                                FormSpecs.RELATED_GAP_ROWSPEC,
                                FormSpecs.DEFAULT_ROWSPEC, }));

        JLabel lblX = new JLabel("X");
        panelLocations.add(lblX, "4, 2");

        JLabel lblY = new JLabel("Y");
        panelLocations.add(lblY, "6, 2");

        JLabel lblZ_1 = new JLabel("Z");
        panelLocations.add(lblZ_1, "8, 2");

        JLabel lblFeedStartLocation = new JLabel("Reference Hole Location");
        panelLocations.add(lblFeedStartLocation, "2, 4, right, default");

        textFieldFeedStartX = new JTextField();
        panelLocations.add(textFieldFeedStartX, "4, 4");
        textFieldFeedStartX.setColumns(8);

        textFieldFeedStartY = new JTextField();
        panelLocations.add(textFieldFeedStartY, "6, 4");
        textFieldFeedStartY.setColumns(8);

        textFieldFeedStartZ = new JTextField();
        panelLocations.add(textFieldFeedStartZ, "8, 4");
        textFieldFeedStartZ.setColumns(8);

        locationButtonsPanelFeedStart = new LocationButtonsPanel(
                textFieldFeedStartX, textFieldFeedStartY, textFieldFeedStartZ,
                null);
        panelLocations.add(locationButtonsPanelFeedStart, "10, 4");

        JLabel lblFeedEndLocation = new JLabel("Last Hole Location");
        panelLocations.add(lblFeedEndLocation, "2, 6, right, default");

        textFieldFeedEndX = new JTextField();
        panelLocations.add(textFieldFeedEndX, "4, 6");
        textFieldFeedEndX.setColumns(8);

        textFieldFeedEndY = new JTextField();
        panelLocations.add(textFieldFeedEndY, "6, 6");
        textFieldFeedEndY.setColumns(8);

        textFieldFeedEndZ = new JTextField();
        panelLocations.add(textFieldFeedEndZ, "8, 6");
        textFieldFeedEndZ.setColumns(8);

        locationButtonsPanelFeedEnd = new LocationButtonsPanel(
                textFieldFeedEndX, textFieldFeedEndY, textFieldFeedEndZ, null);
        panelLocations.add(locationButtonsPanelFeedEnd, "10, 6");
    }

    @Override
    public void createBindings() {
        LengthConverter lengthConverter = new LengthConverter();
        IntegerConverter intConverter = new IntegerConverter();
        DoubleConverter doubleConverter = new DoubleConverter(Configuration
                .get().getLengthDisplayFormat());

        MutableLocationProxy location = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "location", location,
                "location");
        addWrappedBinding(location, "rotation", textFieldLocationRotation, "text",
                doubleConverter);

        addWrappedBinding(feeder, "part", comboBoxPart, "selectedItem");
        addWrappedBinding(feeder, "tapeType", comboBoxTapeType, "selectedItem");

        addWrappedBinding(feeder, "tapeWidth", textFieldTapeWidth, "text",
                lengthConverter);
        addWrappedBinding(feeder, "partPitch", textFieldPartPitch, "text",
                lengthConverter);
        addWrappedBinding(feeder, "feedCount", textFieldFeedCount, "text",
                intConverter);

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

        ComponentDecorators.decorateWithAutoSelect(textFieldLocationRotation);
        ComponentDecorators
                .decorateWithAutoSelectAndLengthConversion(textFieldTapeWidth);
        ComponentDecorators
                .decorateWithAutoSelectAndLengthConversion(textFieldPartPitch);
        ComponentDecorators.decorateWithAutoSelect(textFieldFeedCount);
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
    
    private Action autoSetup = new AbstractAction("Auto Setup") {
        @Override
        public void actionPerformed(ActionEvent e) {
        	CameraView cameraView = MainFrame.cameraPanel.getSelectedCameraView();
        	cameraView.addActionListener(autoSetupPart1Clicked);
        	cameraView.setText("Click on the center of the first part in the tape.");
        }
    };
    
    private List<Location> part1HoleLocations;
    private CameraViewActionListener autoSetupPart1Clicked = new CameraViewActionListener() {
		@Override
		public void actionPerformed(final CameraViewActionEvent action) {
        	final CameraView cameraView = MainFrame.cameraPanel.getSelectedCameraView();
        	final Camera camera = cameraView.getCamera();
        	cameraView.removeActionListener(this);
			Configuration.get().getMachine().submit(new Callable<Void>() {
				public Void call() throws Exception {
					cameraView.setText("Checking first part...");
		        	camera.moveTo(action.getLocation(), 1.0);
		        	Thread.sleep(750);
		            part1HoleLocations = OpenCvUtils.houghCircles(
		                    camera, 
		                    feeder.getHoleDiameter().multiply(0.9), 
		                    feeder.getHoleDiameter().multiply(1.1), 
		                    feeder.getHolePitch().multiply(0.9));
		            // TODO: Need to handle special case when there is only one closest
		            // hole to the clicked location, like if it's the start of the tape.
		            
		            for (Location location : part1HoleLocations) {
		            	System.out.println(action.getLocation() + " " + location + " " + location.getLinearLengthTo(action.getLocation()));
		            }
		            
		            cameraView.setText("Now click on the center of the second part in the tape.");
		            
		            cameraView.addActionListener(autoSetupPart2Clicked);
		        	return null;
				}
			});
		}
	};
    
    private CameraViewActionListener autoSetupPart2Clicked = new CameraViewActionListener() {
		@Override
		public void actionPerformed(final CameraViewActionEvent action) {
        	final CameraView cameraView = MainFrame.cameraPanel.getSelectedCameraView();
        	final Camera camera = cameraView.getCamera();
        	cameraView.removeActionListener(this);
			Configuration.get().getMachine().submit(new Callable<Void>() {
				public Void call() throws Exception {
					cameraView.setText("Checking second part...");
		        	camera.moveTo(action.getLocation(), 1.0);
		        	Thread.sleep(750);
		            List<Location> part2HoleLocations = OpenCvUtils.houghCircles(
		                    camera, 
		                    feeder.getHoleDiameter().multiply(0.9), 
		                    feeder.getHoleDiameter().multiply(1.1), 
		                    feeder.getHolePitch().multiply(0.9));
		            
		        	List<Location> referenceHoles = deriveReferenceHoles(
		        			part1HoleLocations, 
		        			part2HoleLocations);
		        	final Location referenceHole1 = referenceHoles.get(0);
		        	final Location referenceHole2 = referenceHoles.get(1);
		        	Length partPitch = referenceHole1.getLinearLengthTo(referenceHole2);
		        	partPitch.setValue(Math.round(partPitch.getValue()));
		        	feeder.setReferenceHoleLocation(referenceHole1);
		        	feeder.setLastHoleLocation(referenceHole2);
		        	
		        	final Length partPitch_ = partPitch;
		        	SwingUtilities.invokeLater(new Runnable() {
		        		public void run() {
				        	Helpers.copyLocationIntoTextFields(
				        			referenceHole1, 
				        			textFieldFeedStartX, 
				        			textFieldFeedStartY, 
				        			textFieldFeedStartZ);
				        	Helpers.copyLocationIntoTextFields(
				        			referenceHole2, 
				        			textFieldFeedEndX, 
				        			textFieldFeedEndY, 
				        			textFieldFeedEndZ);
				        	textFieldPartPitch.setText(partPitch_.getValue() + "");
		        		}
		        	});
		        	
		        	feeder.setFeedCount(1);
		        	camera.moveTo(feeder.getPickLocation(), 1.0);
		        	feeder.setFeedCount(0);
		        	cameraView.setText("Setup complete!");
		        	Thread.sleep(1500);
		        	cameraView.setText(null);

		        	return null;
				}
			});
		}
	};
	
	private List<Location> deriveReferenceHoles(List<Location> part1HoleLocations, List<Location> part2HoleLocations) {
		// We are only interested in the pair of holes closest to each part
		part1HoleLocations = part1HoleLocations.subList(0, Math.min(2,  part1HoleLocations.size()));
		part2HoleLocations = part2HoleLocations.subList(0, Math.min(2,  part2HoleLocations.size()));
		
		// Part 1's reference hole is the one closest to either of part 2's holes.
		Location part1ReferenceHole = VisionUtils.sortLocationsByDistance(
				part2HoleLocations.get(0),
				part1HoleLocations).get(0);
		// Part 2's reference hole is the one farthest from part 1's reference hole.
		Location part2ReferenceHole = Lists.reverse(VisionUtils.sortLocationsByDistance(
				part1ReferenceHole,
				part2HoleLocations)).get(0);
		
		List<Location> referenceHoles = new ArrayList<>();
		referenceHoles.add(part1ReferenceHole);
		referenceHoles.add(part2ReferenceHole);
		return referenceHoles;
	}
	
    private Action autoSetupCancel = new AbstractAction("Auto Setup") {
        @Override
        public void actionPerformed(ActionEvent e) {
        	MainFrame.mainFrame.hideInstructions();
        	CameraView cameraView = MainFrame.cameraPanel.getSelectedCameraView();
        	cameraView.removeActionListener(autoSetupPart1Clicked);
        	cameraView.removeActionListener(autoSetupPart2Clicked);
        }
    };
}