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
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
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
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IdentifiableListCellRenderer;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.gui.support.PartsComboBoxModel;
import org.openpnp.machine.reference.ReferenceFeeder;
import org.openpnp.machine.reference.feeder.ReferenceTrayFeeder;
import org.openpnp.model.Configuration;
import org.openpnp.model.Part;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class ReferenceTrayFeederConfigurationWizard extends AbstractConfigurationWizard {
	private final ReferenceTrayFeeder feeder;
	private final boolean includePickLocation;

	private JTextField textFieldOffsetsX;
	private JTextField textFieldOffsetsY;
	private JTextField textFieldTrayCountX;
	private JTextField textFieldTrayCountY;
	private JTextField textFieldFeedCount;

	private JPanel panelLocation;
	private JLabel lblX_1;
	private JLabel lblY_1;
	private JLabel lblZ_1;
	private JLabel lblX_2;
	private JLabel lblY_2;
	private JLabel lblZ_2;
	private JLabel lblRotation_1;
	private JLabel lblRotation_2;
	private JTextField textFieldLocationX;
	private JTextField textFieldLocationY;
	private JTextField textFieldLocationZ;
	private JTextField textFieldLocationC;
	private JTextField textFieldTrayRotation;
	private JTextField textFieldLastLocationX;
	private JTextField textFieldLastLocationY;
	private JTextField textFieldLastLocationZ;
	private JTextField textFieldLastLocationC;
	private JPanel panelPart;

	private JComboBox comboBoxPart;
	private LocationButtonsPanel locationButtonsPanel;
	private LocationButtonsPanel lastLocationButtonsPanel;
	private JTextField retryCountTf;

	/**
	 * @wbp.parser.constructor
	 */
	public ReferenceTrayFeederConfigurationWizard(ReferenceTrayFeeder feeder) {
		this(feeder, true);
	}

	public ReferenceTrayFeederConfigurationWizard(ReferenceTrayFeeder feeder, boolean includePickLocation) {
		// super(feeder);
		this.feeder = feeder;
		this.includePickLocation = includePickLocation;

		panelPart = new JPanel();
		panelPart.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "General Settings",
				TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		contentPanel.add(panelPart);
		panelPart.setLayout(new FormLayout(
				new ColumnSpec[] { FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
						FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC, FormSpecs.RELATED_GAP_COLSPEC,
						ColumnSpec.decode("default:grow"), },
				new RowSpec[] { FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC,
						FormSpecs.DEFAULT_ROWSPEC, }));

		comboBoxPart = new JComboBox();
		try {
			comboBoxPart.setModel(new PartsComboBoxModel());
		} catch (Throwable t) {
			// Swallow this error. This happens during parsing in
			// in WindowBuilder but doesn't happen during normal run.
		}

		JLabel lblPart = new JLabel("Part");
		panelPart.add(lblPart, "2, 2, right, default");
		comboBoxPart.setRenderer(new IdentifiableListCellRenderer<Part>());
		panelPart.add(comboBoxPart, "4, 2, left, default");

		JLabel lblRetryCount = new JLabel("Retry Count");
		panelPart.add(lblRetryCount, "2, 4, right, default");

		retryCountTf = new JTextField();
		retryCountTf.setText("3");
		panelPart.add(retryCountTf, "4, 4");
		retryCountTf.setColumns(3);

		if (includePickLocation) {
			panelLocation = new JPanel();
			panelLocation.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null),
					"Pick Location", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
			contentPanel.add(panelLocation);
			panelLocation.setLayout(new FormLayout(
					new ColumnSpec[] { FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"),
							FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"),
							FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"),
							FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"),
							FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("left:default:grow"), },
					new RowSpec[] { FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
							FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC,
							FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
							FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC,
							FormSpecs.DEFAULT_ROWSPEC }));

			JLabel FirstComponent = new JLabel("First Component:");
			panelLocation.add(FirstComponent, "2, 2");

			lblX_1 = new JLabel("X");
			panelLocation.add(lblX_1, "2, 4");

			lblY_1 = new JLabel("Y");
			panelLocation.add(lblY_1, "4, 4");

			lblZ_1 = new JLabel("Z");
			panelLocation.add(lblZ_1, "6, 4");

			lblRotation_1 = new JLabel("Rotation");
			panelLocation.add(lblRotation_1, "8, 4");

			textFieldLocationX = new JTextField();
			panelLocation.add(textFieldLocationX, "2, 6");
			textFieldLocationX.setColumns(8);

			textFieldLocationY = new JTextField();
			panelLocation.add(textFieldLocationY, "4, 6");
			textFieldLocationY.setColumns(8);

			textFieldLocationZ = new JTextField();
			panelLocation.add(textFieldLocationZ, "6, 6");
			textFieldLocationZ.setColumns(8);

			textFieldLocationC = new JTextField();
			panelLocation.add(textFieldLocationC, "8, 6");
			textFieldLocationC.setColumns(8);

			locationButtonsPanel = new LocationButtonsPanel(textFieldLocationX, textFieldLocationY, textFieldLocationZ,
					textFieldLocationC);
			panelLocation.add(locationButtonsPanel, "10, 6");

			JLabel LastComponent = new JLabel("Row 1 Last Component:");
			panelLocation.add(LastComponent, "2, 8");

			lblX_2 = new JLabel("X");
			panelLocation.add(lblX_2, "2, 10");

			lblY_2 = new JLabel("Y");
			panelLocation.add(lblY_2, "4, 10");

			lblZ_2 = new JLabel("Z");
			panelLocation.add(lblZ_2, "6, 10");

			lblRotation_2 = new JLabel("Rotation");
			panelLocation.add(lblRotation_2, "8, 10");

			textFieldLastLocationX = new JTextField();
			panelLocation.add(textFieldLastLocationX, "2, 12");
			textFieldLastLocationX.setColumns(8);

			textFieldLastLocationY = new JTextField();
			panelLocation.add(textFieldLastLocationY, "4, 12");
			textFieldLastLocationY.setColumns(8);

			textFieldLastLocationZ = new JTextField();
			panelLocation.add(textFieldLastLocationZ, "6, 12");
			textFieldLastLocationZ.setColumns(8);

			textFieldLastLocationC = new JTextField();
			panelLocation.add(textFieldLastLocationC, "8, 12");
			textFieldLastLocationC.setColumns(8);

			lastLocationButtonsPanel = new LocationButtonsPanel(textFieldLastLocationX, textFieldLastLocationY, textFieldLastLocationZ,
					textFieldLastLocationC);
			panelLocation.add(lastLocationButtonsPanel, "10, 12");

			JPanel panelFields = new JPanel();

			panelFields.setLayout(new FormLayout(
					new ColumnSpec[] { FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
							FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"),
							FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC, ColumnSpec.decode("default:grow"),
							FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"),
							FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC, },
					new RowSpec[] { FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
							FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC,
							FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
							FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC,
							FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
							FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC,
							FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
							FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC,
							FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
							FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC,
							FormSpecs.DEFAULT_ROWSPEC, }));

			JLabel lblX = new JLabel("X");
			panelFields.add(lblX, "4, 2");

			JLabel lblY = new JLabel("Y");
			panelFields.add(lblY, "6, 2");

			JLabel lblFeedStartLocation = new JLabel("Offsets");
			panelFields.add(lblFeedStartLocation, "2, 4, right, default");

			textFieldOffsetsX = new JTextField();
			panelFields.add(textFieldOffsetsX, "4, 4, fill, default");
			textFieldOffsetsX.setColumns(10);

			textFieldOffsetsY = new JTextField();
			panelFields.add(textFieldOffsetsY, "6, 4, 2, 1, fill, default");
			textFieldOffsetsY.setColumns(10);

			JLabel lblTrayCount = new JLabel("Tray Count");
			panelFields.add(lblTrayCount, "2, 6, right, default");

			textFieldTrayCountX = new JTextField();
			panelFields.add(textFieldTrayCountX, "4, 6, fill, default");
			textFieldTrayCountX.setColumns(10);

			textFieldTrayCountY = new JTextField();
			panelFields.add(textFieldTrayCountY, "6, 6, 2, 1, fill, default");
			textFieldTrayCountY.setColumns(10);

			JSeparator separator = new JSeparator();
			panelFields.add(separator, "4, 8, 4, 1");

			JLabel lblFeedCount = new JLabel("Feed Count");
			panelFields.add(lblFeedCount, "2, 10, right, default");

			textFieldFeedCount = new JTextField();
			panelFields.add(textFieldFeedCount, "4, 10, fill, default");
			textFieldFeedCount.setColumns(10);

			JLabel lblTrayRotation = new JLabel("Tray Rotation [°]");
			panelFields.add(lblTrayRotation, "2, 12, right, default");

			textFieldTrayRotation = new JTextField();
			panelFields.add(textFieldTrayRotation, "4, 12, fill, default");
			textFieldTrayRotation.setColumns(10);

			contentPanel.add(panelFields);

			JButton btnResetFeedCount = new JButton(new AbstractAction("Reset") {
				@Override
				public void actionPerformed(ActionEvent e) {
					textFieldFeedCount.setText("0");
					applyAction.actionPerformed(e);
				}
			});
			btnResetFeedCount.setHorizontalAlignment(SwingConstants.LEFT);
			panelFields.add(btnResetFeedCount, "6, 10, left, default");
			
			JButton btnCalcOffsetsRotation = new JButton(new AbstractAction("Calculate X-Offset & Tray Rotation") {
				@Override
				public void actionPerformed(ActionEvent e) {
					//TODO
					double deltaX = (Double.parseDouble(textFieldLocationX.getText()) - Double.parseDouble(textFieldLastLocationX.getText()));
					double deltaY = (Double.parseDouble(textFieldLocationY.getText()) - Double.parseDouble(textFieldLastLocationY.getText()));
					double rot_rad = Math.atan(deltaY/deltaX);
					double rot_deg = Math.toDegrees(rot_rad);
					double delta_length = Math.sqrt(deltaX*deltaX + deltaY*deltaY);
						
					textFieldTrayRotation.setText(String.valueOf(rot_deg));
					textFieldOffsetsX.setText(String.valueOf(delta_length/(Integer.parseInt(textFieldTrayCountX.getText())-1)));
					//textFieldTrayCountY.setText("0");
				}
			});
			btnCalcOffsetsRotation.setHorizontalAlignment(SwingConstants.LEFT);
			panelFields.add(btnCalcOffsetsRotation, "6, 12, left, default");
		}
	}

	@Override
	public void createBindings() {
		LengthConverter lengthConverter = new LengthConverter();
		IntegerConverter intConverter = new IntegerConverter();
		DoubleConverter doubleConverter = new DoubleConverter(Configuration.get().getLengthDisplayFormat());

		addWrappedBinding(feeder, "part", comboBoxPart, "selectedItem");
		addWrappedBinding(feeder, "retryCount", retryCountTf, "text", intConverter);

		if (includePickLocation) {
			MutableLocationProxy location = new MutableLocationProxy();
			bind(UpdateStrategy.READ_WRITE, feeder, "location", location, "location");
			addWrappedBinding(location, "lengthX", textFieldLocationX, "text", lengthConverter);
			addWrappedBinding(location, "lengthY", textFieldLocationY, "text", lengthConverter);
			addWrappedBinding(location, "lengthZ", textFieldLocationZ, "text", lengthConverter);
			addWrappedBinding(location, "rotation", textFieldLocationC, "text", doubleConverter);
			
			//MutableLocationProxy lastComponentlocation = new MutableLocationProxy();
			//bind(UpdateStrategy.READ_WRITE, feeder, "lastComponentLocation", location, "location");
			//addWrappedBinding(lastComponentlocation, "lengthX", textFieldLastLocationX, "text", lengthConverter);
			//addWrappedBinding(lastComponentlocation, "lengthY", textFieldLastLocationY, "text", lengthConverter);
			//addWrappedBinding(lastComponentlocation, "lengthZ", textFieldLastLocationZ, "text", lengthConverter);
			//addWrappedBinding(lastComponentlocation, "rotation", textFieldLastLocationC, "text", doubleConverter);
			
			ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldLocationX);
			ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldLocationY);
			ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldLocationZ);
			ComponentDecorators.decorateWithAutoSelect(textFieldLocationC);
			//ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldLastLocationX);
			//ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldLastLocationY);
			//ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldLastLocationZ);
			//ComponentDecorators.decorateWithAutoSelect(textFieldLastLocationC);
		}

		MutableLocationProxy offsets = new MutableLocationProxy();
		bind(UpdateStrategy.READ_WRITE, feeder, "offsets", offsets, "location");
		addWrappedBinding(offsets, "lengthX", textFieldOffsetsX, "text", lengthConverter);
		addWrappedBinding(offsets, "lengthY", textFieldOffsetsY, "text", lengthConverter);

		addWrappedBinding(feeder, "trayCountX", textFieldTrayCountX, "text", intConverter);
		addWrappedBinding(feeder, "trayCountY", textFieldTrayCountY, "text", intConverter);

		addWrappedBinding(feeder, "feedCount", textFieldFeedCount, "text", intConverter);
		addWrappedBinding(feeder, "trayRotation", textFieldTrayRotation, "text", doubleConverter);

		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldOffsetsX);
		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldOffsetsY);
		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldTrayRotation);

		ComponentDecorators.decorateWithAutoSelect(retryCountTf);

		ComponentDecorators.decorateWithAutoSelect(textFieldTrayCountX);
		ComponentDecorators.decorateWithAutoSelect(textFieldTrayCountY);
		ComponentDecorators.decorateWithAutoSelect(textFieldFeedCount);
	}

	@Override
	protected void saveToModel() {
		super.saveToModel();
		if (feeder.getOffsets().getX() == 0 && feeder.getTrayCountX() > 1) {
			MessageBoxes.errorBox(this, "Error",
					"X offset must be greater than 0 if X tray count is greater than 1 or feed failure will occur.");
		}
		if (feeder.getOffsets().getY() == 0 && feeder.getTrayCountY() > 1) {
			MessageBoxes.errorBox(this, "Error",
					"Y offset must be greater than 0 if Y tray count is greater than 1 or feed failure will occur.");
		}
	}
}
