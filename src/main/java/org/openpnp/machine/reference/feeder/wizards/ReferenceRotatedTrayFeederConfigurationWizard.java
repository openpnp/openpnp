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
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Icon;
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
import org.openpnp.machine.reference.feeder.ReferenceRotatedTrayFeeder;
import org.openpnp.model.Configuration;
import org.openpnp.model.Part;
import org.simpleframework.xml.Element;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class ReferenceRotatedTrayFeederConfigurationWizard extends AbstractConfigurationWizard {
	private final ReferenceRotatedTrayFeeder feeder;
	private final boolean includePickLocation;

	private JTextField textFieldOffsetsX;
	private JTextField textFieldOffsetsY;

	private JTextField textFieldFeedCount;

	private JPanel panelLocation;
	private JPanel panelParameters;
	private JPanel panelIllustration;
	private JLabel lblX_1;
	private JLabel lblY_1;
	private JLabel lblComponentCount;
	private JTextField textFieldLocationX;
	private JTextField textFieldLocationY;
	private JTextField textFieldFirstRowLastLocationX;
	private JTextField textFieldFirstRowLastLocationY;
	private JTextField textFieldLastLocationX;
	private JTextField textFieldLastLocationY;

	private JTextField textFieldTrayCountCols;
	private JTextField textFieldTrayCountRows;
	private JTextField textFieldTrayRotation;
	private JTextField textFieldComponentRotation;
	private JTextField textFieldComponentZHeight;

	private JPanel panelPart;

	private JComboBox<?> comboBoxPart;
	private LocationButtonsPanel locationButtonsPanel;
	private LocationButtonsPanel lastLocationButtonsPanel;
	private JTextField retryCountTf;

	/**
	 * @wbp.parser.constructor
	 */
	public ReferenceRotatedTrayFeederConfigurationWizard(ReferenceRotatedTrayFeeder feeder) {
		this(feeder, true);
	}

	public ReferenceRotatedTrayFeederConfigurationWizard(ReferenceRotatedTrayFeeder feeder,
			boolean includePickLocation) {
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

		JPanel warningPanel = new JPanel();
		FlowLayout flowLayout = (FlowLayout) warningPanel.getLayout();
		contentPanel.add(warningPanel, 0);

		JLabel lblWarningThisFeeder = new JLabel(
				"Warning: This feeder is incomplete and experimental. Use at your own risk.");
		lblWarningThisFeeder.setFont(new Font("Lucida Grande", Font.PLAIN, 16));
		lblWarningThisFeeder.setForeground(Color.RED);
		lblWarningThisFeeder.setHorizontalAlignment(SwingConstants.LEFT);
		warningPanel.add(lblWarningThisFeeder);

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
					"Tray Component Locations", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
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
							FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC }));

			JLabel firstComponent = new JLabel(
					"<html><b>Point A:</b><br /><span style='font-size:7px'>First Row - First Component</span></html>");
			panelLocation.add(firstComponent, "2, 4");

			lblX_1 = new JLabel("X");
			panelLocation.add(lblX_1, "4, 2");

			lblY_1 = new JLabel("Y");
			panelLocation.add(lblY_1, "6, 2");

			textFieldLocationX = new JTextField();
			panelLocation.add(textFieldLocationX, "4, 4");
			textFieldLocationX.setColumns(6);

			textFieldLocationY = new JTextField();
			panelLocation.add(textFieldLocationY, "6, 4");
			textFieldLocationY.setColumns(6);

			locationButtonsPanel = new LocationButtonsPanel(textFieldLocationX, textFieldLocationY, null, null);
			panelLocation.add(locationButtonsPanel, "8, 4");

			JLabel firstRowLastComponent = new JLabel(
					"<html><b>Point B:</b><br /><span style='font-size:8px'>First Row - Last Component</span></html>");
			panelLocation.add(firstRowLastComponent, "2, 6");

			textFieldFirstRowLastLocationX = new JTextField();
			panelLocation.add(textFieldFirstRowLastLocationX, "4, 6");
			textFieldFirstRowLastLocationX.setColumns(6);

			textFieldFirstRowLastLocationY = new JTextField();
			panelLocation.add(textFieldFirstRowLastLocationY, "6, 6");
			textFieldFirstRowLastLocationY.setColumns(6);

			lastLocationButtonsPanel = new LocationButtonsPanel(textFieldFirstRowLastLocationX,
					textFieldFirstRowLastLocationY, null, null);
			panelLocation.add(lastLocationButtonsPanel, "8, 6");

			JLabel lastComponent = new JLabel(
					"<html><b>Point C:</b><br /><span style='font-size:8px'>Last Row - Last Component</span></html>");
			panelLocation.add(lastComponent, "2, 8");

			textFieldLastLocationX = new JTextField();
			panelLocation.add(textFieldLastLocationX, "4, 8");
			textFieldLastLocationX.setColumns(6);

			textFieldLastLocationY = new JTextField();
			panelLocation.add(textFieldLastLocationY, "6, 8");
			textFieldLastLocationY.setColumns(6);

			lastLocationButtonsPanel = new LocationButtonsPanel(textFieldLastLocationX, textFieldLastLocationY, null,
					null);
			panelLocation.add(lastLocationButtonsPanel, "8, 8");

			panelParameters = new JPanel();
			panelParameters.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null),
					"Tray Parameters", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
			contentPanel.add(panelParameters);

			panelParameters.setLayout(new FormLayout(
					new ColumnSpec[] { FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("left:default:grow"),
							FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("left:default:grow"),
							FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("left:default:grow"),
							FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("left:default:grow"),
							FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("left:default:grow"), },
					new RowSpec[] { FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
							FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC,
							FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
							FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC,
							FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
							FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC,
							FormSpecs.DEFAULT_ROWSPEC }));

			JLabel lblTrayRows = new JLabel("Number of Tray Rows");
			panelParameters.add(lblTrayRows, "2, 2");

			textFieldTrayCountRows = new JTextField();
			panelParameters.add(textFieldTrayCountRows, "4, 2");
			textFieldTrayCountRows.setColumns(10);

			JLabel lblTrayCols = new JLabel("Number of Tray Columns");
			panelParameters.add(lblTrayCols, "6, 2");

			textFieldTrayCountCols = new JTextField();
			panelParameters.add(textFieldTrayCountCols, "8, 2");
			textFieldTrayCountCols.setColumns(10);

			JLabel lblFeedCount = new JLabel("Feed Count");
			panelParameters.add(lblFeedCount, "2, 4");

			textFieldFeedCount = new JTextField();
			panelParameters.add(textFieldFeedCount, "4, 4");
			textFieldFeedCount.setColumns(10);

			lblComponentCount = new JLabel("Components left:");
			panelParameters.add(lblComponentCount, "6, 4");

			JButton btnResetFeedCount = new JButton(new AbstractAction("Reset") {
				@Override
				public void actionPerformed(ActionEvent e) {
					textFieldFeedCount.setText("0");
					int componentleft = Integer.parseInt(textFieldTrayCountCols.getText())
							* Integer.parseInt(textFieldTrayCountRows.getText())
							- Integer.parseInt(textFieldFeedCount.getText());
					lblComponentCount.setText("Components left: " + String.valueOf(componentleft));
					applyAction.actionPerformed(e);
				}
			});
			btnResetFeedCount.setHorizontalAlignment(SwingConstants.LEFT);
			panelParameters.add(btnResetFeedCount, "8, 4, left, default");

			JLabel lblComponentRotation = new JLabel("Component Rotation [°]");
			panelParameters.add(lblComponentRotation, "2, 6");

			textFieldComponentRotation = new JTextField();
			panelParameters.add(textFieldComponentRotation, "4, 6");
			textFieldComponentRotation.setColumns(10);

			JLabel lblComponentZHeight = new JLabel("Z Height");
			panelParameters.add(lblComponentZHeight, "6, 6");

			textFieldComponentZHeight = new JTextField();
			panelParameters.add(textFieldComponentZHeight, "8, 6");
			textFieldComponentZHeight.setColumns(10);

			JSeparator separator = new JSeparator();
			panelParameters.add(separator, "1, 9, 8, 1");

			JButton btnCalcOffsetsRotation = new JButton(new AbstractAction("Calculate Offsets & Tray Rotation") {
				@Override
				public void actionPerformed(ActionEvent e) {

					if ((Integer.parseInt(textFieldTrayCountCols.getText())
							* Integer.parseInt(textFieldTrayCountRows.getText()) <= 1)) {
						MessageBoxes.errorBox(getTopLevelAncestor(), "Error",
								"Need at least 2 components in tray to calculate offsets. Please increase Number of Tray Rows or Columns.");
					}

					// Distance Point A -> Point B
					double deltaX1 = (Double.parseDouble(textFieldLocationX.getText())
							- Double.parseDouble(textFieldFirstRowLastLocationX.getText()));
					double deltaY1 = (Double.parseDouble(textFieldLocationY.getText())
							- Double.parseDouble(textFieldFirstRowLastLocationY.getText()));

					double rot_rad1 = Math.atan(deltaY1 / deltaX1);
					double rot_deg1 = Math.toDegrees(rot_rad1);
					double delta_length1 = Math.sqrt(deltaX1 * deltaX1 + deltaY1 * deltaY1);

					// Distance Point B -> Point C
					double deltaX2 = (Double.parseDouble(textFieldFirstRowLastLocationX.getText())
							- Double.parseDouble(textFieldLastLocationX.getText()));
					double deltaY2 = (Double.parseDouble(textFieldFirstRowLastLocationY.getText())
							- Double.parseDouble(textFieldLastLocationY.getText()));

					double delta_length2 = Math.sqrt(deltaX2 * deltaX2 + deltaY2 * deltaY2);

					textFieldTrayRotation.setText(String.valueOf(round(rot_deg1, 3)));

					if (Integer.parseInt(textFieldTrayCountCols.getText()) > 1) {
						textFieldOffsetsX.setText(String.valueOf(
								round(delta_length1 / (Integer.parseInt(textFieldTrayCountCols.getText()) - 1), 3)));
					} else {
						textFieldOffsetsX.setText("0");
					}

					if (Integer.parseInt(textFieldTrayCountRows.getText()) > 1) {
						textFieldOffsetsY.setText(String.valueOf(
								round(delta_length2 / (Integer.parseInt(textFieldTrayCountRows.getText()) - 1), 3)));
					} else {
						textFieldOffsetsY.setText("0");
					}
				}
			});
			btnCalcOffsetsRotation.setHorizontalAlignment(SwingConstants.LEFT);
			panelParameters.add(btnCalcOffsetsRotation, "2, 12");

			JLabel lblRowOffset = new JLabel("Row Offset");
			panelParameters.add(lblRowOffset, "2, 14");

			textFieldOffsetsX = new JTextField();
			panelParameters.add(textFieldOffsetsX, "4, 14");
			textFieldOffsetsX.setColumns(10);

			JLabel lblColOffset = new JLabel("Column Offset");
			panelParameters.add(lblColOffset, "6, 14");

			textFieldOffsetsY = new JTextField();
			panelParameters.add(textFieldOffsetsY, "8, 14, ");
			textFieldOffsetsY.setColumns(10);

			JLabel lblTrayRotation = new JLabel("Tray Rotation [°]");
			panelParameters.add(lblTrayRotation, "2, 16");

			textFieldTrayRotation = new JTextField();
			panelParameters.add(textFieldTrayRotation, "4, 16");
			textFieldTrayRotation.setColumns(10);

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
			addWrappedBinding(location, "rotation", textFieldComponentRotation, "text", doubleConverter);
			addWrappedBinding(location, "lengthZ", textFieldComponentZHeight, "text", lengthConverter);

			MutableLocationProxy firstRowLastComponentlocation = new MutableLocationProxy();
			bind(UpdateStrategy.READ_WRITE, feeder, "firstRowLastComponentLocation", firstRowLastComponentlocation,
					"location");
			addWrappedBinding(firstRowLastComponentlocation, "lengthX", textFieldFirstRowLastLocationX, "text",
					lengthConverter);
			addWrappedBinding(firstRowLastComponentlocation, "lengthY", textFieldFirstRowLastLocationY, "text",
					lengthConverter);

			MutableLocationProxy lastComponentlocation = new MutableLocationProxy();
			bind(UpdateStrategy.READ_WRITE, feeder, "lastComponentLocation", lastComponentlocation, "location");
			addWrappedBinding(lastComponentlocation, "lengthX", textFieldLastLocationX, "text", lengthConverter);
			addWrappedBinding(lastComponentlocation, "lengthY", textFieldLastLocationY, "text", lengthConverter);

			ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldLocationX);
			ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldLocationY);
			ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldComponentRotation);
			ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldComponentZHeight);
			ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFirstRowLastLocationX);
			ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFirstRowLastLocationY);
			ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldLastLocationX);
			ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldLastLocationY);
		}

		MutableLocationProxy offsets = new MutableLocationProxy();
		bind(UpdateStrategy.READ_WRITE, feeder, "offsets", offsets, "location");
		addWrappedBinding(offsets, "lengthX", textFieldOffsetsX, "text", lengthConverter);
		addWrappedBinding(offsets, "lengthY", textFieldOffsetsY, "text", lengthConverter);

		addWrappedBinding(feeder, "trayCountCols", textFieldTrayCountCols, "text", intConverter);
		addWrappedBinding(feeder, "trayCountRows", textFieldTrayCountRows, "text", intConverter);
		addWrappedBinding(feeder, "feedCount", textFieldFeedCount, "text", intConverter);
		addWrappedBinding(feeder, "trayRotation", textFieldTrayRotation, "text", doubleConverter);

		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldOffsetsX);
		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldOffsetsY);
		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldTrayRotation);
		ComponentDecorators.decorateWithAutoSelect(retryCountTf);
		ComponentDecorators.decorateWithAutoSelect(textFieldTrayCountRows);
		ComponentDecorators.decorateWithAutoSelect(textFieldTrayCountCols);
		ComponentDecorators.decorateWithAutoSelect(textFieldFeedCount);
	}

	@Override
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

	public static double round(double value, int places) {
		if (places < 0) {
			throw new IllegalArgumentException();
		}

		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(places, RoundingMode.HALF_UP);
		return bd.doubleValue();
	}
}
