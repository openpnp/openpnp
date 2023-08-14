/*
 * Copyright (C) 2023 Sebastian Pichelhofer, Jason von Nieda <jason@vonnieda.org>, Tony Luken
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

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
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
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.Converter;
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
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class ReferenceRotatedTrayFeederConfigurationWizard extends AbstractConfigurationWizard {
    private static final double RIGHT_ANGLE_TOLERANCE = 2.5; //degrees
    private static final double OFFSET_AND_ROTATION_TOLERANCE = 0.002;
    
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
	private JLabel lblPickRetryCount;
	private JTextField pickRetryCount;

	/**
	 * @wbp.parser.constructor
	 */
	public ReferenceRotatedTrayFeederConfigurationWizard(ReferenceRotatedTrayFeeder feeder) {
		this(feeder, true);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
    public ReferenceRotatedTrayFeederConfigurationWizard(ReferenceRotatedTrayFeeder feeder,
			boolean includePickLocation) {
		// super(feeder);
		this.feeder = feeder;
		this.includePickLocation = includePickLocation;

		panelPart = new JPanel();
		panelPart.setBorder(new TitledBorder(null, "General Settings",
				TitledBorder.LEADING, TitledBorder.TOP, null));
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
		        FormSpecs.DEFAULT_ROWSPEC,}));

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

		JLabel lblRetryCount = new JLabel("Feed Retry Count");
		panelPart.add(lblRetryCount, "2, 4, right, default");

		retryCountTf = new JTextField();
		retryCountTf.setText("3");
		panelPart.add(retryCountTf, "4, 4");
		retryCountTf.setColumns(3);
		
		lblPickRetryCount = new JLabel("Pick Retry Count");
		panelPart.add(lblPickRetryCount, "2, 6, right, default");
		
		pickRetryCount = new JTextField();
		pickRetryCount.setText("3");
		pickRetryCount.setColumns(3);
		panelPart.add(pickRetryCount, "4, 6, fill, default");

		if (includePickLocation) {
			panelLocation = new JPanel();
			panelLocation.setBorder(new TitledBorder(null,
					"Tray Component Locations", TitledBorder.LEADING, TitledBorder.TOP, null));
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

//			locationButtonsPanel = new LocationButtonsPanel(textFieldLocationX, textFieldLocationY,
//			        null, textFieldTrayRotation);
//			panelLocation.add(locationButtonsPanel, "8, 4");

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

			lastLocationButtonsPanel = new LocationButtonsPanel(textFieldLastLocationX, 
			        textFieldLastLocationY, null, null);
			panelLocation.add(lastLocationButtonsPanel, "8, 8");

			panelParameters = new JPanel();
			panelParameters.setBorder(new TitledBorder(null,
					"Tray Parameters", TitledBorder.LEADING, TitledBorder.TOP, null));
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
                    feeder.setFeedCount(0);
				}
			});
			btnResetFeedCount.setHorizontalAlignment(SwingConstants.LEFT);
			panelParameters.add(btnResetFeedCount, "8, 4, left, default");

			JLabel lblComponentRotation = new JLabel("Component Rotation in Tray [°]");
			panelParameters.add(lblComponentRotation, "2, 6");

			textFieldComponentRotation = new JTextField();
			panelParameters.add(textFieldComponentRotation, "4, 6");
			textFieldComponentRotation.setColumns(10);
			textFieldComponentRotation.setToolTipText("Rotation of the components relative to the tray's A->B (row) axis");
			
			JLabel lblComponentZHeight = new JLabel("Z Height");
			panelParameters.add(lblComponentZHeight, "6, 6");

			textFieldComponentZHeight = new JTextField();
			panelParameters.add(textFieldComponentZHeight, "8, 6");
			textFieldComponentZHeight.setColumns(10);

			JSeparator separator = new JSeparator();
			panelParameters.add(separator, "1, 9, 8, 1");

			JButton btnCalcOffsetsRotation = new JButton(
			        new AbstractAction("Calculate Offsets & Tray Rotation") {
				@Override
				public void actionPerformed(ActionEvent e) {

//					if ((Integer.parseInt(textFieldTrayCountCols.getText())
//							* Integer.parseInt(textFieldTrayCountRows.getText()) <= 1)) {
//						MessageBoxes.errorBox(getTopLevelAncestor(), "Error",
//								"Need at least 2 components in tray to calculate offsets. Please "
//								+ "increase Number of Tray Rows or Columns.");
//						return;
//					}

			        Location offsetsAndRotation;
                    try {
                        offsetsAndRotation = calculateOffsetsAndRotation();
                    }
                    catch (Exception e1) {
                        MessageBoxes.errorBox(getTopLevelAncestor(), "Error", e1.getMessage());
                        return;
                    }

                    textFieldOffsetsX.requestFocusInWindow(); //so the component decorator runs
//					if (Integer.parseInt(textFieldTrayCountCols.getText()) > 1) {
                        textFieldOffsetsX.setText(String.valueOf(offsetsAndRotation.getX()));
//					} else {
//						textFieldOffsetsX.setText("0");
//					}

                    textFieldOffsetsY.requestFocusInWindow(); //so the component decorator runs
//					if (Integer.parseInt(textFieldTrayCountRows.getText()) > 1) {
                        textFieldOffsetsY.setText(String.valueOf(offsetsAndRotation.getY()));
//					} else {
//						textFieldOffsetsY.setText("0");
//					}
					
                    textFieldTrayRotation.requestFocusInWindow(); //so the component decorator runs
                    textFieldTrayRotation.setText(String.valueOf(offsetsAndRotation.getRotation()));

                    //Return the focus back to the button
                    ((JButton) (e.getSource())).requestFocusInWindow();
				}
			});
			btnCalcOffsetsRotation.setHorizontalAlignment(SwingConstants.LEFT);
			panelParameters.add(btnCalcOffsetsRotation, "2, 12");

			JLabel lblRowOffset = new JLabel("Column Offset");
			panelParameters.add(lblRowOffset, "2, 14");

			textFieldOffsetsX = new JTextField();
			panelParameters.add(textFieldOffsetsX, "4, 14");
			textFieldOffsetsX.setColumns(10);

			JLabel lblColOffset = new JLabel("Row Offset");
			panelParameters.add(lblColOffset, "6, 14");

			textFieldOffsetsY = new JTextField();
			panelParameters.add(textFieldOffsetsY, "8, 14, ");
			textFieldOffsetsY.setColumns(10);

			JLabel lblTrayRotation = new JLabel("Tray Rotation [°]");
			panelParameters.add(lblTrayRotation, "2, 16");

			textFieldTrayRotation = new JTextField();
			textFieldTrayRotation.setToolTipText("Angle of the tray's A->B (row) axis relative to "
			        + "the machine's positive X-axis");
			panelParameters.add(textFieldTrayRotation, "4, 16");
			textFieldTrayRotation.setColumns(10);

            locationButtonsPanel = new LocationButtonsPanel(textFieldLocationX, textFieldLocationY,
                    null, textFieldTrayRotation);
            panelLocation.add(locationButtonsPanel, "8, 4");

			panelIllustration = new JPanel();
			panelIllustration.setBorder(new TitledBorder(null,
					"Tray Illustration", TitledBorder.LEADING, TitledBorder.TOP, null));
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
        addWrappedBinding(feeder, "feedRetryCount", retryCountTf, "text", intConverter);
        addWrappedBinding(feeder, "pickRetryCount", pickRetryCount, "text", intConverter);

		if (includePickLocation) {
			MutableLocationProxy location = new MutableLocationProxy();
			bind(UpdateStrategy.READ_WRITE, feeder, "location", location, "location");
			addWrappedBinding(location, "lengthX", textFieldLocationX, "text", lengthConverter);
			addWrappedBinding(location, "lengthY", textFieldLocationY, "text", lengthConverter);
			addWrappedBinding(location, "rotation", textFieldTrayRotation, "text", doubleConverter);
			addWrappedBinding(location, "lengthZ", textFieldComponentZHeight, "text", lengthConverter);

	        addWrappedBinding(feeder, "componentRotationInTray", textFieldComponentRotation, 
	                "text", doubleConverter);

			MutableLocationProxy firstRowLastComponentlocation = new MutableLocationProxy();
			bind(UpdateStrategy.READ_WRITE, feeder, "firstRowLastComponentLocation", 
			        firstRowLastComponentlocation, "location");
			addWrappedBinding(firstRowLastComponentlocation, "lengthX", 
			        textFieldFirstRowLastLocationX, "text", lengthConverter);
			addWrappedBinding(firstRowLastComponentlocation, "lengthY", 
			        textFieldFirstRowLastLocationY, "text", lengthConverter);

			MutableLocationProxy lastComponentlocation = new MutableLocationProxy();
			bind(UpdateStrategy.READ_WRITE, feeder, "lastComponentLocation", 
			        lastComponentlocation, "location");
			addWrappedBinding(lastComponentlocation, "lengthX", textFieldLastLocationX, "text", 
			        lengthConverter);
			addWrappedBinding(lastComponentlocation, "lengthY", textFieldLastLocationY, "text", 
			        lengthConverter);

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

        bind(UpdateStrategy.READ_WRITE, feeder, "feedCount", textFieldFeedCount, "text", intConverter);

		bind(UpdateStrategy.READ, feeder, "remainingCount", lblComponentCount, "text", 
		        new Converter<Integer, String>() {
	        
	        @Override
	        public String convertForward(Integer count) {
	            return "Components left: " + String.valueOf(count);
	        }

	        @Override
	        public Integer convertReverse(String s) {
	            return Integer.parseInt(s.substring(17));
	        }
	    });

		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldOffsetsX);
		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldOffsetsY);
		ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldTrayRotation);
        ComponentDecorators.decorateWithAutoSelect(retryCountTf);
        ComponentDecorators.decorateWithAutoSelect(pickRetryCount);
		ComponentDecorators.decorateWithAutoSelect(textFieldTrayCountRows);
		ComponentDecorators.decorateWithAutoSelect(textFieldTrayCountCols);
		ComponentDecorators.decorateWithAutoSelect(textFieldFeedCount);
	}

	/**
	 * Calculates the tray's x (column) and y (row) offsets as well as the tray rotation.
	 * @return the offsets and rotation
	 */
	public Location calculateOffsetsAndRotation() throws Exception {
        double ax = Double.parseDouble(textFieldLocationX.getText());
        double ay = Double.parseDouble(textFieldLocationY.getText());
        double bx = Double.parseDouble(textFieldFirstRowLastLocationX.getText());
        double by = Double.parseDouble(textFieldFirstRowLastLocationY.getText());
        double cx = Double.parseDouble(textFieldLastLocationX.getText());
        double cy = Double.parseDouble(textFieldLastLocationY.getText());
        
        int nCols = Integer.parseInt(textFieldTrayCountCols.getText());
        int nRows = Integer.parseInt(textFieldTrayCountRows.getText());
        
        if (nCols*nRows < 1) {
            throw new Exception("Feeder must have at least one row and one column.");
        }
        
        // Distance Point A -> Point B
        double deltaX1 = bx - ax;
        double deltaY1 = by - ay;

        double delta_length1 = Math.hypot(deltaX1, deltaY1);
        
        if ((delta_length1 > 0) && (nCols <= 1)) {
            throw new Exception("Points A and B are different which is inconsistent with Number of Tray Columns = " + String.valueOf(nCols));
        }
        if ((delta_length1 == 0) && (nCols > 1)) {
            throw new Exception("Points A and B are the same which is inconsistent with Number of Tray Columns = " + String.valueOf(nCols));
        }

        // Distance Point B -> Point C
        double deltaX2 = cx - bx;
        double deltaY2 = cy - by;

        double delta_length2 = Math.hypot(deltaX2, deltaY2);

        if ((delta_length2 > 0) && (nRows <= 1)) {
            throw new Exception("Points B and C are different which is inconsistent with Number of Tray Rows = " + String.valueOf(nRows));
        }
        if ((delta_length2 == 0) && (nRows > 1)) {
            throw new Exception("Points B and C are the same which is inconsistent with Number of Tray Rows = " + String.valueOf(nRows));
        }

        // Distance Point C -> Point A
        double deltaX3 = ax - cx;
        double deltaY3 = ay - cy;

        double delta_length3 = Math.hypot(deltaX3, deltaY3);
        
        //Use the law of cosines to compute angle ABC and check to see if it is close to 90 degrees.
        //A NaN is ok here, it just means we have a single row or column and therefore there is no
        //90 degree angle to check.
        Double checkAngleDeg = Math.toDegrees(Math.acos((delta_length3*delta_length3 - 
                delta_length2*delta_length2 - delta_length1*delta_length1)/
                (-2*delta_length1*delta_length2)));
        if (checkAngleDeg != Double.NaN && (checkAngleDeg < 90-RIGHT_ANGLE_TOLERANCE || 
                checkAngleDeg > 90+RIGHT_ANGLE_TOLERANCE)) {
            throw new Exception("Tray angle ABC should be 90 degrees but is "
                    + String.format("%.3f", checkAngleDeg)
                    + " degrees, double check coordinates of points A, B and C.");
        }

        double colStep = nCols <= 1 ? 0 : delta_length1/(nCols-1);
        double rowStep = nRows <= 1 ? 0 : delta_length2/(nRows-1);
        
        //Determine whether the points go clockwise or counter-clockwise around the
        //tray by using the shoelace formula to compute the area of the triangle. The
        //area will be positive if counter-clockwise. We use this to determine
        //if points A, B and C were picked as shown in the illustration (clockwise) or
        //were picked going the other way around (counter-clockwise).
        double doubleArea = (ay + by) * (ax - bx) + (by + cy) * (bx - cx) + 
                (cy + ay) * (cx - ax);
        if (doubleArea > 0) {
            //The definition of the tray has the row numbers increasing in the negative
            //y direction (towards point C). Therefore, if the points were picked the
            //opposite way of the definition, we need to negate the row offset so that 
            //the row numbers increase in the positive y direction.
            delta_length2 *= -1;
        }
        
        //Compute the tray rotation using a four-quadrant arc tangent so that the tray
        //can be oriented at any rotation relative to the machine X-Y axes.
        double rotDeg = Double.parseDouble(textFieldTrayRotation.getText());
        if (deltaX1 != 0 || deltaY1 != 0) {
            //We have multiple columns so compute the tray rotation from the points on
            //the first row
            rotDeg = Math.toDegrees(Math.atan2(deltaY1, deltaX1));
        }
        else if (deltaX2 != 0 || deltaY2 != 0) {
            //We have only a single column but multiple rows so compute the angle of the column and
            //add 90 degrees to it to get the tray rotation
            rotDeg = Math.toDegrees(Math.atan2(deltaY2, deltaX2)) + 90;
        }
        
        return new Location(Configuration.get().getSystemUnits(), colStep, rowStep, 0, rotDeg);
	}
	
	@Override
    public void validateInput() throws Exception {
        double offsetX = Double.parseDouble(textFieldOffsetsX.getText());
        double offsetY = Double.parseDouble(textFieldOffsetsY.getText());
//        int nCols = Integer.parseInt(textFieldTrayCountCols.getText());
//        int nRows = Integer.parseInt(textFieldTrayCountRows.getText());
        double rot = Double.parseDouble(textFieldTrayRotation.getText());
        
        Location offsetsAndRotation = calculateOffsetsAndRotation();
        
        if ((Math.abs(offsetsAndRotation.getX() - offsetX) > OFFSET_AND_ROTATION_TOLERANCE) ||
                (Math.abs(offsetsAndRotation.getY() - offsetY) > OFFSET_AND_ROTATION_TOLERANCE) ||
                (Math.abs(offsetsAndRotation.getRotation() - rot) > OFFSET_AND_ROTATION_TOLERANCE)) {
            throw new Exception(
                    "Offsets and/or Tray Rotation are inconsistent with points A, B, C, number of "
                    + "rows, and/or number of columns. Double check those values and then click "
                    + "the Calculate Offsets & Tray Rotation button to make them consistent.");
        }
//        if ((offsetX == 0) && (nCols > 1)) {
//            throw new Exception(
//                    "Column Offset must be non-zero if Number of Tray Columns is greater than 1, "
//                    + "otherwise a feed failure will occur.");
//        }
//        if ((offsetY == 0) && (nRows > 1)) {
//            throw new Exception(
//                    "Row Offset must be non-zero if Number of Tray Rows is greater than 1, "
//                    + "otherwise a feed failure will occur.");
//        }
    }

}
