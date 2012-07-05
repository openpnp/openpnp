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

package org.openpnp.machine.reference.feeder;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

import org.jdesktop.beansbinding.AbstractBindingListener;
import org.jdesktop.beansbinding.Binding;
import org.jdesktop.beansbinding.BindingListener;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.support.BufferedImageIconConverter;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.JBindings;
import org.openpnp.gui.support.JBindings.WrappedBinding;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.Wizard;
import org.openpnp.gui.support.WizardContainer;
import org.openpnp.model.Location;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

class ReferenceTapeFeederConfigurationWizard extends JPanel implements Wizard {
	private final ReferenceTapeFeeder feeder;
	
	private WizardContainer wizardContainer;

	private JTextField feedStartX;
	private JTextField feedStartY;
	private JTextField feedStartZ;
	private JTextField feedEndX;
	private JTextField feedEndY;
	private JTextField feedEndZ;
	private JTextField feedRate;
	private JButton feedStartAutoFill;
	private JButton feedEndAutoFill;
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
	private JLabel lblLeft;
	private JTextField textFieldAoeLeft;
	private JLabel lblTop;
	private JTextField textFieldAoeTop;
	private JLabel lblRight;
	private JTextField textFieldAoeRight;
	private JLabel lblBottom;
	private JTextField textFieldAoeBottom;
	private JButton btnChangeAoe;
	

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

		JLabel lblFeedRate = new JLabel("Feed Rate");
		panelGeneral.add(lblFeedRate, "2, 2");

		feedRate = new JTextField();
		panelGeneral.add(feedRate, "4, 2");
		feedRate.setColumns(5);

		lblActuatorId = new JLabel("Actuator Id");
		panelGeneral.add(lblActuatorId, "2, 4");

		textFieldActuatorId = new JTextField();
		panelGeneral.add(textFieldActuatorId, "4, 4");
		textFieldActuatorId.setColumns(5);

		JScrollPane scrollPane = new JScrollPane(panelFields);

		panelLocations = new JPanel();
		panelFields.add(panelLocations);
		panelLocations.setBorder(new TitledBorder(null, "Locations",
				TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panelLocations.setLayout(new FormLayout(
				new ColumnSpec[] { FormFactory.RELATED_GAP_COLSPEC,
						FormFactory.DEFAULT_COLSPEC,
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

		JLabel lblFeedStartLocation = new JLabel("Feed Start Location");
		panelLocations.add(lblFeedStartLocation, "2, 6");

		feedStartX = new JTextField();
		panelLocations.add(feedStartX, "4, 6");
		feedStartX.setColumns(10);

		feedStartY = new JTextField();
		panelLocations.add(feedStartY, "6, 6");
		feedStartY.setColumns(10);

		feedStartZ = new JTextField();
		panelLocations.add(feedStartZ, "8, 6");
		feedStartZ.setColumns(10);

		feedStartAutoFill = new JButton("Set to Current");
		panelLocations.add(feedStartAutoFill, "10, 6");

		JLabel lblFeedEndLocation = new JLabel("Feed End Location");
		panelLocations.add(lblFeedEndLocation, "2, 8");

		feedEndX = new JTextField();
		panelLocations.add(feedEndX, "4, 8");
		feedEndX.setColumns(10);

		feedEndY = new JTextField();
		panelLocations.add(feedEndY, "6, 8");
		feedEndY.setColumns(10);

		feedEndZ = new JTextField();
		panelLocations.add(feedEndZ, "8, 8");
		feedEndZ.setColumns(10);

		feedEndAutoFill = new JButton("Set to Current");
		panelLocations.add(feedEndAutoFill, "10, 8");

		feedEndAutoFill.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Location l = wizardContainer.getMachineControlsPanel()
						.getDisplayedLocation();
				l = l.convertToUnits(feeder.getFeedEndLocation().getUnits());
				feedEndX.setText(l.getLengthX().toString());
				feedEndY.setText(l.getLengthY().toString());
				feedEndZ.setText(l.getLengthZ().toString());
			}
		});

		feedStartAutoFill.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Location l = wizardContainer.getMachineControlsPanel()
						.getDisplayedLocation();
				l = l.convertToUnits(feeder.getFeedStartLocation().getUnits());
				feedStartX.setText(l.getLengthX().toString());
				feedStartY.setText(l.getLengthY().toString());
				feedStartZ.setText(l.getLengthZ().toString());
			}
		});

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
														ColumnSpec.decode("center:default"),
														FormFactory.RELATED_GAP_COLSPEC,
														FormFactory.DEFAULT_COLSPEC,
														FormFactory.RELATED_GAP_COLSPEC,
														FormFactory.DEFAULT_COLSPEC,
														FormFactory.RELATED_GAP_COLSPEC,
														FormFactory.DEFAULT_COLSPEC,
														FormFactory.RELATED_GAP_COLSPEC,
														FormFactory.DEFAULT_COLSPEC,},
													new RowSpec[] {
														FormFactory.RELATED_GAP_ROWSPEC,
														FormFactory.DEFAULT_ROWSPEC,
														FormFactory.RELATED_GAP_ROWSPEC,
														FormFactory.DEFAULT_ROWSPEC,
														FormFactory.RELATED_GAP_ROWSPEC,
														FormFactory.DEFAULT_ROWSPEC,
														FormFactory.RELATED_GAP_ROWSPEC,
														FormFactory.DEFAULT_ROWSPEC,
														FormFactory.RELATED_GAP_ROWSPEC,
														FormFactory.DEFAULT_ROWSPEC,}));
												
												lblTop = new JLabel("Top");
												panelAoE.add(lblTop, "6, 2, center, default");
												
												textFieldAoeTop = new JTextField();
												panelAoE.add(textFieldAoeTop, "6, 4, center, default");
												textFieldAoeTop.setColumns(5);
												
												lblLeft = new JLabel("Left");
												panelAoE.add(lblLeft, "2, 6, right, default");
												
												textFieldAoeLeft = new JTextField();
												panelAoE.add(textFieldAoeLeft, "4, 6, left, default");
												textFieldAoeLeft.setColumns(5);
												
												btnChangeAoe = new JButton(changeAoeAction);
												panelAoE.add(btnChangeAoe, "6, 6, center, default");
												
												textFieldAoeRight = new JTextField();
												panelAoE.add(textFieldAoeRight, "8, 6, right, default");
												textFieldAoeRight.setColumns(5);
												
												lblRight = new JLabel("Right");
												panelAoE.add(lblRight, "10, 6, left, default");
												
												textFieldAoeBottom = new JTextField();
												panelAoE.add(textFieldAoeBottom, "6, 8, center, default");
												textFieldAoeBottom.setColumns(5);
												
												lblBottom = new JLabel("Bottom");
												panelAoE.add(lblBottom, "6, 10, center, default");
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
		IntegerConverter intConverter = new IntegerConverter("%d");
		LengthConverter lengthConverter = new LengthConverter();
		DoubleConverter doubleConverter = new DoubleConverter("%2.3f");
		BufferedImageIconConverter imageConverter = new BufferedImageIconConverter();
		BindingListener listener = new AbstractBindingListener() {
			@Override
			public void synced(Binding binding) {
				saveAction.setEnabled(true);
				cancelAction.setEnabled(true);
			}
		};

		wrappedBindings.add(JBindings.bind(feeder, "feedRate", feedRate,
				"text", doubleConverter, listener));
		wrappedBindings.add(JBindings.bind(feeder, "actuatorId",
				textFieldActuatorId, "text", listener));
		wrappedBindings.add(JBindings.bind(feeder, "feedStartLocation.lengthX",
				feedStartX, "text", lengthConverter, listener));
		wrappedBindings.add(JBindings.bind(feeder, "feedStartLocation.lengthY",
				feedStartY, "text", lengthConverter, listener));
		wrappedBindings.add(JBindings.bind(feeder, "feedStartLocation.lengthZ",
				feedStartZ, "text", lengthConverter, listener));
		wrappedBindings.add(JBindings.bind(feeder, "feedEndLocation.lengthX",
				feedEndX, "text", lengthConverter, listener));
		wrappedBindings.add(JBindings.bind(feeder, "feedEndLocation.lengthY",
				feedEndY, "text", lengthConverter, listener));
		wrappedBindings.add(JBindings.bind(feeder, "feedEndLocation.lengthZ",
				feedEndZ, "text", lengthConverter, listener));
		wrappedBindings.add(JBindings.bind(feeder, "vision.enabled",
				chckbxVisionEnabled, "selected", null, listener));
		wrappedBindings.add(JBindings.bind(feeder, "vision.templateImage",
				labelTemplateImage, "icon", imageConverter, listener));
		
		wrappedBindings.add(JBindings.bind(feeder, "vision.areaOfInterest.left",
				textFieldAoeLeft, "text", intConverter, listener));
		wrappedBindings.add(JBindings.bind(feeder, "vision.areaOfInterest.top",
				textFieldAoeTop, "text", intConverter, listener));
		wrappedBindings.add(JBindings.bind(feeder, "vision.areaOfInterest.right",
				textFieldAoeRight, "text", intConverter, listener));
		wrappedBindings.add(JBindings.bind(feeder, "vision.areaOfInterest.bottom",
				textFieldAoeBottom, "text", intConverter, listener));
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

	private Action saveAction = new AbstractAction("Apply") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			saveToModel();
			wizardContainer
					.wizardCompleted(ReferenceTapeFeederConfigurationWizard.this);
			btnChangeTemplateImage.setAction(changeTemplateImageAction);
		}
	};

	private Action cancelAction = new AbstractAction("Reset") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			loadFromModel();
			btnChangeTemplateImage.setAction(changeTemplateImageAction);
		}
	};

	private Action changeTemplateImageAction = new AbstractAction(
			"Change") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			CameraView cameraView = MainFrame.cameraPanel.getSelectedCameraView(); 
			cameraView.setSelectionEnabled(true);
			org.openpnp.model.Rectangle r = feeder.getVision().getTemplateImageCoordinates();
			if (r == null || r.getWidth() == 0 || r.getHeight() == 0) {
				cameraView.setSelection(0, 0, 100, 100);
			}
			else {
				cameraView.setSelection(r.getLeft(), r.getTop(), r.getWidth(), r.getHeight());
			}
			btnChangeTemplateImage.setAction(captureTemplateImageAction);
		}
	};
	
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
	
	private Action changeAoeAction = new AbstractAction(
			"Change") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			CameraView cameraView = MainFrame.cameraPanel.getSelectedCameraView(); 
			cameraView.setSelectionEnabled(true);
			org.openpnp.model.Rectangle r = feeder.getVision().getAreaOfInterest();
			if (r == null || r.getWidth() == 0 || r.getHeight() == 0) {
				cameraView.setSelection(0, 0, 100, 100);
			}
			else {
				cameraView.setSelection(r.getLeft(), r.getTop(), r.getWidth(), r.getHeight());
			}
			btnChangeAoe.setAction(captureAoeAction);
		}
	};
	
	// TODO: This is completely useless. Need to be capturing machine
	// coordinates, not image coordinates.
	private Action captureAoeAction = new AbstractAction(
			"Capture") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			CameraView cameraView = MainFrame.cameraPanel.getSelectedCameraView(); 
			cameraView.setSelectionEnabled(false);
			Rectangle r = cameraView.getSelection();
			textFieldAoeLeft.setText("" + (int) r.getX());
			textFieldAoeTop.setText("" + (int) r.getY());
			textFieldAoeRight.setText("" + (int) r.getMaxX());
			textFieldAoeBottom.setText("" + (int) r.getMaxY());
			btnChangeAoe.setAction(changeAoeAction);
		}
	};
}