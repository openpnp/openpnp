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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.BufferedImageIconConverter;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.JBindings;
import org.openpnp.gui.support.JBindings.WrappedBinding;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.SaveResetBindingListener;
import org.openpnp.gui.support.Wizard;
import org.openpnp.gui.support.WizardContainer;
import org.openpnp.machine.reference.feeder.ReferenceTapeFeeder;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import org.openpnp.gui.components.LocationButtonsPanel;

@SuppressWarnings("serial")
public class ReferenceTapeFeederConfigurationWizard extends JPanel implements Wizard {
	private final ReferenceTapeFeeder feeder;
	
	private WizardContainer wizardContainer;

	private JTextField textFieldFeedStartX;
	private JTextField textFieldFeedStartY;
	private JTextField textFieldFeedStartZ;
	private JTextField textFieldFeedEndX;
	private JTextField textFieldFeedEndY;
	private JTextField textFieldFeedEndZ;
	private JTextField textFieldFeedRate;
	private JButton btnSave;
	private JButton btnCancel;
	private JLabel lblActuatorId;
	private JTextField textFieldActuatorId;
	private JPanel panelGeneral;
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
	private JLabel lblTopLeft;
	private JLabel lblX_1;
	private JLabel lblY_1;
	private JLabel lblZ_1;
	private JTextField textFieldTopLeftX;
	private JTextField textFieldTopLeftY;
	private JTextField textFieldTopLeftZ;
	private JLabel lblBottomRight;
	private JTextField textFieldBottomRightX;
	private JTextField textFieldBottomRightY;
	private JTextField textFieldBottomRightZ;
	

	private List<WrappedBinding> wrappedBindings = new ArrayList<WrappedBinding>();

	public ReferenceTapeFeederConfigurationWizard(ReferenceTapeFeeder referenceTapeFeeder) {
		this.feeder = referenceTapeFeeder;

		setLayout(new BorderLayout());

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

		JLabel lblFeedRate = new JLabel("Feed Rate (units/min)");
		panelGeneral.add(lblFeedRate, "2, 2");

		textFieldFeedRate = new JTextField();
		panelGeneral.add(textFieldFeedRate, "4, 2");
		textFieldFeedRate.setColumns(5);

		lblActuatorId = new JLabel("Actuator Id");
		panelGeneral.add(lblActuatorId, "2, 4, right, default");

		textFieldActuatorId = new JTextField();
		panelGeneral.add(textFieldActuatorId, "4, 4");
		textFieldActuatorId.setColumns(5);

		JScrollPane scrollPane = new JScrollPane(panelFields);
		scrollPane.getVerticalScrollBar().setUnitIncrement(Configuration.get().getVerticalScrollUnitIncrement());

		panelLocations = new JPanel();
		panelFields.add(panelLocations);
		panelLocations.setBorder(new TitledBorder(null, "Locations",
				TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panelLocations.setLayout(new FormLayout(new ColumnSpec[] {
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("left:default:grow"),},
			new RowSpec[] {
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,}));

		JLabel lblX = new JLabel("X");
		panelLocations.add(lblX, "4, 4");

		JLabel lblY = new JLabel("Y");
		panelLocations.add(lblY, "6, 4");

		JLabel lblZ = new JLabel("Z");
		panelLocations.add(lblZ, "8, 4");

		JLabel lblFeedStartLocation = new JLabel("Feed Start Location");
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
		
		locationButtonsPanelFeedStart = new LocationButtonsPanel(textFieldFeedStartX, textFieldFeedStartY, textFieldFeedStartZ, null);
		panelLocations.add(locationButtonsPanelFeedStart, "10, 6");

		JLabel lblFeedEndLocation = new JLabel("Feed End Location");
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
		
		locationButtonsPanelFeedEnd = new LocationButtonsPanel(textFieldFeedEndX, textFieldFeedEndY, textFieldFeedEndZ, null);
		panelLocations.add(locationButtonsPanelFeedEnd, "10, 8");

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
		panelVisionTemplateAndAoe.setLayout(new FormLayout(new ColumnSpec[] {
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,},
			new RowSpec[] {
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,}));

		panelTemplate = new JPanel();
		panelTemplate.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "Template Image", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
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
										
												btnChangeTemplateImage = new JButton(changeTemplateImageAction);
												btnChangeTemplateImage.setAlignmentX(Component.CENTER_ALIGNMENT);
												panelTemplate.add(btnChangeTemplateImage);
												
												panelAoE = new JPanel();
												panelAoE.setBorder(new TitledBorder(null, "Area of Interest", TitledBorder.LEADING, TitledBorder.TOP, null, null));
												panelVisionTemplateAndAoe.add(panelAoE, "4, 2, fill, fill");
												panelAoE.setLayout(new FormLayout(new ColumnSpec[] {
														FormFactory.RELATED_GAP_COLSPEC,
														FormFactory.DEFAULT_COLSPEC,
														FormFactory.RELATED_GAP_COLSPEC,
														ColumnSpec.decode("default:grow"),
														FormFactory.RELATED_GAP_COLSPEC,
														ColumnSpec.decode("default:grow"),
														FormFactory.RELATED_GAP_COLSPEC,
														ColumnSpec.decode("default:grow"),
														FormFactory.RELATED_GAP_COLSPEC,
														ColumnSpec.decode("default:grow"),},
													new RowSpec[] {
														FormFactory.RELATED_GAP_ROWSPEC,
														FormFactory.DEFAULT_ROWSPEC,
														FormFactory.RELATED_GAP_ROWSPEC,
														FormFactory.DEFAULT_ROWSPEC,
														FormFactory.RELATED_GAP_ROWSPEC,
														FormFactory.DEFAULT_ROWSPEC,}));
												
												lblX_1 = new JLabel("X");
												panelAoE.add(lblX_1, "4, 2");
												
												lblY_1 = new JLabel("Y");
												panelAoE.add(lblY_1, "6, 2");
												
												lblZ_1 = new JLabel("Z");
												panelAoE.add(lblZ_1, "8, 2");
												
												lblTopLeft = new JLabel("Top Left");
												panelAoE.add(lblTopLeft, "2, 4, right, default");
												
												textFieldTopLeftX = new JTextField();
												panelAoE.add(textFieldTopLeftX, "4, 4, fill, default");
												textFieldTopLeftX.setColumns(6);
												
												textFieldTopLeftY = new JTextField();
												panelAoE.add(textFieldTopLeftY, "6, 4, fill, default");
												textFieldTopLeftY.setColumns(6);
												
												textFieldTopLeftZ = new JTextField();
												panelAoE.add(textFieldTopLeftZ, "8, 4, fill, default");
												textFieldTopLeftZ.setColumns(6);
												
												locationButtonsPanelAoeTopLeft = new LocationButtonsPanel(textFieldTopLeftX, textFieldTopLeftY, textFieldTopLeftZ, null);
												panelAoE.add(locationButtonsPanelAoeTopLeft, "10, 4");
												
												lblBottomRight = new JLabel("Bottom Right");
												panelAoE.add(lblBottomRight, "2, 6, right, default");
												
												textFieldBottomRightX = new JTextField();
												panelAoE.add(textFieldBottomRightX, "4, 6, fill, default");
												textFieldBottomRightX.setColumns(6);
												
												textFieldBottomRightY = new JTextField();
												panelAoE.add(textFieldBottomRightY, "6, 6, fill, default");
												textFieldBottomRightY.setColumns(6);
												
												textFieldBottomRightZ = new JTextField();
												panelAoE.add(textFieldBottomRightZ, "8, 6, fill, default");
												textFieldBottomRightZ.setColumns(6);
												
												locationButtonsPanelAoeBottomRight = new LocationButtonsPanel(textFieldBottomRightX, textFieldBottomRightY, textFieldBottomRightZ, null);
												panelAoE.add(locationButtonsPanelAoeBottomRight, "10, 6");
		scrollPane.setBorder(null);
		add(scrollPane, BorderLayout.CENTER);

		JPanel panelActions = new JPanel();
		panelActions.setLayout(new FlowLayout(FlowLayout.RIGHT));
		add(panelActions, BorderLayout.SOUTH);

		btnCancel = new JButton(cancelAction);
		panelActions.add(btnCancel);

		btnSave = new JButton(saveAction);
		panelActions.add(btnSave);
		
		createBindings();
		loadFromModel();
	}

	private void createBindings() {
		LengthConverter lengthConverter = new LengthConverter(Configuration.get());
		BufferedImageIconConverter imageConverter = new BufferedImageIconConverter();
		SaveResetBindingListener listener = new SaveResetBindingListener(saveAction, cancelAction);

		wrappedBindings.add(JBindings.bind(feeder, "feedRate", textFieldFeedRate,
				"text", lengthConverter, listener));
		wrappedBindings.add(JBindings.bind(feeder, "actuatorId",
				textFieldActuatorId, "text", listener));
		wrappedBindings.add(JBindings.bind(feeder, "feedStartLocation.lengthX",
				textFieldFeedStartX, "text", lengthConverter, listener));
		wrappedBindings.add(JBindings.bind(feeder, "feedStartLocation.lengthY",
				textFieldFeedStartY, "text", lengthConverter, listener));
		wrappedBindings.add(JBindings.bind(feeder, "feedStartLocation.lengthZ",
				textFieldFeedStartZ, "text", lengthConverter, listener));
		wrappedBindings.add(JBindings.bind(feeder, "feedEndLocation.lengthX",
				textFieldFeedEndX, "text", lengthConverter, listener));
		wrappedBindings.add(JBindings.bind(feeder, "feedEndLocation.lengthY",
				textFieldFeedEndY, "text", lengthConverter, listener));
		wrappedBindings.add(JBindings.bind(feeder, "feedEndLocation.lengthZ",
				textFieldFeedEndZ, "text", lengthConverter, listener));
		wrappedBindings.add(JBindings.bind(feeder, "vision.enabled",
				chckbxVisionEnabled, "selected", listener));
		wrappedBindings.add(JBindings.bind(feeder, "vision.templateImage",
				labelTemplateImage, "icon", imageConverter, listener));
		
		wrappedBindings.add(JBindings.bind(feeder, "vision.areaOfInterestTopLeft.lengthX",
				textFieldTopLeftX, "text", lengthConverter, listener));
		wrappedBindings.add(JBindings.bind(feeder, "vision.areaOfInterestTopLeft.lengthY",
				textFieldTopLeftY, "text", lengthConverter, listener));
		wrappedBindings.add(JBindings.bind(feeder, "vision.areaOfInterestTopLeft.lengthZ",
				textFieldTopLeftZ, "text", lengthConverter, listener));
		
		wrappedBindings.add(JBindings.bind(feeder, "vision.areaOfInterestBottomRight.lengthX",
				textFieldBottomRightX, "text", lengthConverter, listener));
		wrappedBindings.add(JBindings.bind(feeder, "vision.areaOfInterestBottomRight.lengthY",
				textFieldBottomRightY, "text", lengthConverter, listener));
		wrappedBindings.add(JBindings.bind(feeder, "vision.areaOfInterestBottomRight.lengthZ",
				textFieldBottomRightZ, "text", lengthConverter, listener));
		
		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFeedRate);
		ComponentDecorators.decorateWithAutoSelect(textFieldActuatorId);
		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFeedStartX);
		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFeedStartY);
		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFeedStartZ);
		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFeedEndX);
		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFeedEndY);
		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFeedEndZ);
		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldTopLeftX);
		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldTopLeftY);
		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldTopLeftZ);
		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldBottomRightX);
		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldBottomRightY);
		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldBottomRightZ);
	}

	private void loadFromModel() {
		for (WrappedBinding wrappedBinding : wrappedBindings) {
			wrappedBinding.reset();
		}
		saveAction.setEnabled(false);
		cancelAction.setEnabled(false);
	}

	private void saveToModel() {
		for (WrappedBinding wrappedBinding : wrappedBindings) {
			wrappedBinding.save();
		}
		saveAction.setEnabled(false);
		cancelAction.setEnabled(false);
	}

	@Override
	public void setWizardContainer(WizardContainer wizardContainer) {
		this.wizardContainer = wizardContainer;
	}

	@Override
	public JPanel getWizardPanel() {
		return this;
	}

	@Override
	public String getWizardName() {
		// TODO Auto-generated method stub
		return null;
	}

	@SuppressWarnings("serial")
	private Action saveAction = new AbstractAction("Apply") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			saveToModel();
			wizardContainer
					.wizardCompleted(ReferenceTapeFeederConfigurationWizard.this);
			btnChangeTemplateImage.setAction(changeTemplateImageAction);
		}
	};

	@SuppressWarnings("serial")
	private Action cancelAction = new AbstractAction("Reset") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			loadFromModel();
			btnChangeTemplateImage.setAction(changeTemplateImageAction);
		}
	};

	@SuppressWarnings("serial")
	private Action changeTemplateImageAction = new AbstractAction(
			"Change") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			CameraView cameraView = MainFrame.cameraPanel.getSelectedCameraView(); 
			cameraView.setSelectionEnabled(true);
//			org.openpnp.model.Rectangle r = feeder.getVision().getTemplateImageCoordinates();
			org.openpnp.model.Rectangle r = null;
			if (r == null || r.getWidth() == 0 || r.getHeight() == 0) {
				cameraView.setSelection(0, 0, 100, 100);
			}
			else {
				cameraView.setSelection(r.getLeft(), r.getTop(), r.getWidth(), r.getHeight());
			}
			btnChangeTemplateImage.setAction(captureTemplateImageAction);
		}
	};
	
	@SuppressWarnings("serial")
	private Action captureTemplateImageAction = new AbstractAction(
			"Capture") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			new Thread() {
				public void run() {
					CameraView cameraView = MainFrame.cameraPanel.getSelectedCameraView(); 
					BufferedImage image = cameraView.captureSelectionImage();
					if (image == null) {
						MessageBoxes.errorBox(
								ReferenceTapeFeederConfigurationWizard.this, 
								"No Image Selected", 
								"Please select an area of the camera image using the mouse.");
					}
					else {
						labelTemplateImage.setIcon(new ImageIcon(image));
					}
					cameraView.setSelectionEnabled(false);
					btnChangeTemplateImage.setAction(changeTemplateImageAction);
				}
			}.start();
		}
	};
	
	private LocationButtonsPanel locationButtonsPanelFeedStart;
	private LocationButtonsPanel locationButtonsPanelFeedEnd;
	private LocationButtonsPanel locationButtonsPanelAoeTopLeft;
	private LocationButtonsPanel locationButtonsPanelAoeBottomRight;
	
//	private Action changeAoeAction = new AbstractAction(
//			"Change") {
//		@Override
//		public void actionPerformed(ActionEvent arg0) {
//			CameraView cameraView = MainFrame.cameraPanel.getSelectedCameraView(); 
//			cameraView.setSelectionEnabled(true);
//			org.openpnp.model.Rectangle r = feeder.getVision().getAreaOfInterest();
//			if (r == null || r.getWidth() == 0 || r.getHeight() == 0) {
//				cameraView.setSelection(0, 0, 100, 100);
//			}
//			else {
//				cameraView.setSelection(r.getLeft(), r.getTop(), r.getWidth(), r.getHeight());
//			}
//			btnChangeAoe.setAction(captureAoeAction);
//		}
//	};
//	
//	// TODO: This is completely useless. Need to be capturing machine
//	// coordinates, not image coordinates.
//	private Action captureAoeAction = new AbstractAction(
//			"Capture") {
//		@Override
//		public void actionPerformed(ActionEvent arg0) {
//			CameraView cameraView = MainFrame.cameraPanel.getSelectedCameraView(); 
//			cameraView.setSelectionEnabled(false);
//			Rectangle r = cameraView.getSelection();
//			textFieldAoeLeft.setText("" + (int) r.getX());
//			textFieldAoeTop.setText("" + (int) r.getY());
//			textFieldAoeRight.setText("" + (int) r.getMaxX());
//			textFieldAoeBottom.setText("" + (int) r.getMaxY());
//			btnChangeAoe.setAction(changeAoeAction);
//		}
//	};
}