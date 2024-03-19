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
import org.openpnp.Translations;
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
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.util.Utils2D;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class ReferenceRotatedTrayFeederConfigurationWizard extends AbstractConfigurationWizard {
    private static final double RIGHT_ANGLE_TOLERANCE = 2.5; //degrees
    private static final LengthUnit VALIDATION_UNITS = LengthUnit.Millimeters;
    private static final double VALIDATION_TOLERANCE = 0.03;

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

    private MutableLocationProxy firstRowFirstColumn = new MutableLocationProxy();
    private MutableLocationProxy firstRowLastColumn = new MutableLocationProxy();
    private MutableLocationProxy lastRowLastColumn = new MutableLocationProxy();
    private MutableLocationProxy offsetsAndRotation = new MutableLocationProxy();
    private int nRows;
    private int nCols;
    private int wizardFeedCount;

    /**
     * @wbp.parser.constructor
     */
    public ReferenceRotatedTrayFeederConfigurationWizard(ReferenceRotatedTrayFeeder feeder) {
        this(feeder, true);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public ReferenceRotatedTrayFeederConfigurationWizard(ReferenceRotatedTrayFeeder feeder,
            boolean includePickLocation) {
        this.feeder = feeder;
        this.includePickLocation = includePickLocation;

        panelPart = new JPanel();
        panelPart.setBorder(new TitledBorder(null, Translations.getString("ReferenceRotatedTrayFeederConfigurationWizard.GeneralSettings"), //$NON-NLS-1$
                TitledBorder.LEADING, TitledBorder.TOP, null));
        contentPanel.add(panelPart);
        panelPart.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),}, //$NON-NLS-1$
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

        JLabel lblPart = new JLabel(Translations.getString("ReferenceRotatedTrayFeederConfigurationWizard.Part")); //$NON-NLS-1$
        panelPart.add(lblPart, "2, 2, right, default"); //$NON-NLS-1$
        comboBoxPart.setRenderer(new IdentifiableListCellRenderer<Part>());
        panelPart.add(comboBoxPart, "4, 2, left, default"); //$NON-NLS-1$

        JLabel lblRetryCount = new JLabel(Translations.getString("ReferenceRotatedTrayFeederConfigurationWizard.FeedRetryCount")); //$NON-NLS-1$
        panelPart.add(lblRetryCount, "2, 4, right, default"); //$NON-NLS-1$

        retryCountTf = new JTextField();
        retryCountTf.setText("3"); //$NON-NLS-1$
        panelPart.add(retryCountTf, "4, 4"); //$NON-NLS-1$
        retryCountTf.setColumns(3);

        lblPickRetryCount = new JLabel(Translations.getString("ReferenceRotatedTrayFeederConfigurationWizard.PickRetryCount")); //$NON-NLS-1$
        panelPart.add(lblPickRetryCount, "2, 6, right, default"); //$NON-NLS-1$

        pickRetryCount = new JTextField();
        pickRetryCount.setText("3"); //$NON-NLS-1$
        pickRetryCount.setColumns(3);
        panelPart.add(pickRetryCount, "4, 6, fill, default"); //$NON-NLS-1$

        if (includePickLocation) {
            panelLocation = new JPanel();
            panelLocation.setBorder(new TitledBorder(null,
                    Translations.getString("ReferenceRotatedTrayFeederConfigurationWizard.TrayComponentLocations"), TitledBorder.LEADING, TitledBorder.TOP, null)); //$NON-NLS-1$
            contentPanel.add(panelLocation);
            panelLocation.setLayout(new FormLayout(
                    new ColumnSpec[] { FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"), //$NON-NLS-1$
                            FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"), //$NON-NLS-1$
                            FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"), //$NON-NLS-1$
                            FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"), //$NON-NLS-1$
                            FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("left:default:grow"), }, //$NON-NLS-1$
                    new RowSpec[] { FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                            FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC,
                            FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                            FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC }));

            JLabel firstComponent = new JLabel(
                    Translations.getString("ReferenceRotatedTrayFeederConfigurationWizard.FirstRowFirstColumn")); //$NON-NLS-1$
            panelLocation.add(firstComponent, "2, 4"); //$NON-NLS-1$

            lblX_1 = new JLabel(Translations.getString("ReferenceRotatedTrayFeederConfigurationWizard.X")); //$NON-NLS-1$
            panelLocation.add(lblX_1, "4, 2"); //$NON-NLS-1$

            lblY_1 = new JLabel(Translations.getString("ReferenceRotatedTrayFeederConfigurationWizard.Y")); //$NON-NLS-1$
            panelLocation.add(lblY_1, "6, 2"); //$NON-NLS-1$

            textFieldLocationX = new JTextField();
            panelLocation.add(textFieldLocationX, "4, 4"); //$NON-NLS-1$
            textFieldLocationX.setColumns(6);

            textFieldLocationY = new JTextField();
            panelLocation.add(textFieldLocationY, "6, 4"); //$NON-NLS-1$
            textFieldLocationY.setColumns(6);

            JLabel firstRowLastComponent = new JLabel(
                    Translations.getString("ReferenceRotatedTrayFeederConfigurationWizard.FirstRowLastColumn")); //$NON-NLS-1$
            panelLocation.add(firstRowLastComponent, "2, 6"); //$NON-NLS-1$

            textFieldFirstRowLastLocationX = new JTextField();
            panelLocation.add(textFieldFirstRowLastLocationX, "4, 6"); //$NON-NLS-1$
            textFieldFirstRowLastLocationX.setColumns(6);

            textFieldFirstRowLastLocationY = new JTextField();
            panelLocation.add(textFieldFirstRowLastLocationY, "6, 6"); //$NON-NLS-1$
            textFieldFirstRowLastLocationY.setColumns(6);

            lastLocationButtonsPanel = new LocationButtonsPanel(textFieldFirstRowLastLocationX,
                    textFieldFirstRowLastLocationY, null, null);
            panelLocation.add(lastLocationButtonsPanel, "8, 6"); //$NON-NLS-1$

            JLabel lastComponent = new JLabel(
                    Translations.getString("ReferenceRotatedTrayFeederConfigurationWizard.LastRowLastColumn")); //$NON-NLS-1$
            panelLocation.add(lastComponent, "2, 8"); //$NON-NLS-1$

            textFieldLastLocationX = new JTextField();
            panelLocation.add(textFieldLastLocationX, "4, 8"); //$NON-NLS-1$
            textFieldLastLocationX.setColumns(6);

            textFieldLastLocationY = new JTextField();
            panelLocation.add(textFieldLastLocationY, "6, 8"); //$NON-NLS-1$
            textFieldLastLocationY.setColumns(6);

            lastLocationButtonsPanel = new LocationButtonsPanel(textFieldLastLocationX, 
                    textFieldLastLocationY, null, null);
            panelLocation.add(lastLocationButtonsPanel, "8, 8"); //$NON-NLS-1$

            panelParameters = new JPanel();
            panelParameters.setBorder(new TitledBorder(null,
                    Translations.getString("ReferenceRotatedTrayFeederConfigurationWizard.TrayParameters"), TitledBorder.LEADING, TitledBorder.TOP, null)); //$NON-NLS-1$
            contentPanel.add(panelParameters);

            panelParameters.setLayout(new FormLayout(
                    new ColumnSpec[] { FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("left:default:grow"), //$NON-NLS-1$
                            FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("left:default:grow"), //$NON-NLS-1$
                            FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("left:default:grow"), //$NON-NLS-1$
                            FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("left:default:grow"), //$NON-NLS-1$
                            FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("left:default:grow"), }, //$NON-NLS-1$
                    new RowSpec[] { FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                            FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC,
                            FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                            FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC,
                            FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                            FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC,
                            FormSpecs.DEFAULT_ROWSPEC }));

            JLabel lblTrayRows = new JLabel(Translations.getString("ReferenceRotatedTrayFeederConfigurationWizard.NumberOfRows")); //$NON-NLS-1$
            panelParameters.add(lblTrayRows, "2, 2"); //$NON-NLS-1$

            textFieldTrayCountRows = new JTextField();
            panelParameters.add(textFieldTrayCountRows, "4, 2"); //$NON-NLS-1$
            textFieldTrayCountRows.setColumns(10);

            JLabel lblTrayCols = new JLabel(Translations.getString("ReferenceRotatedTrayFeederConfigurationWizard.NumberOfColumns")); //$NON-NLS-1$
            panelParameters.add(lblTrayCols, "6, 2"); //$NON-NLS-1$

            textFieldTrayCountCols = new JTextField();
            panelParameters.add(textFieldTrayCountCols, "8, 2"); //$NON-NLS-1$
            textFieldTrayCountCols.setColumns(10);

            JLabel lblFeedCount = new JLabel(Translations.getString("ReferenceRotatedTrayFeederConfigurationWizard.FeedCount")); //$NON-NLS-1$
            panelParameters.add(lblFeedCount, "2, 4"); //$NON-NLS-1$

            textFieldFeedCount = new JTextField();
            panelParameters.add(textFieldFeedCount, "4, 4"); //$NON-NLS-1$
            textFieldFeedCount.setColumns(10);

            lblComponentCount = new JLabel(Translations.getString("ReferenceRotatedTrayFeederConfigurationWizard.ComponentsRemaining")); //$NON-NLS-1$
            panelParameters.add(lblComponentCount, "6, 4"); //$NON-NLS-1$

            JButton btnResetFeedCount = new JButton(new AbstractAction(Translations.getString("ReferenceRotatedTrayFeederConfigurationWizard.Reset")) { //$NON-NLS-1$
                @Override
                public void actionPerformed(ActionEvent e) {
                    feeder.setFeedCount(0);
                }
            });
            btnResetFeedCount.setHorizontalAlignment(SwingConstants.LEFT);
            panelParameters.add(btnResetFeedCount, "8, 4, left, default"); //$NON-NLS-1$

            JLabel lblComponentRotation = new JLabel(Translations.getString("ReferenceRotatedTrayFeederConfigurationWizard.ComponentRotation")); //$NON-NLS-1$
            panelParameters.add(lblComponentRotation, "2, 6"); //$NON-NLS-1$

            textFieldComponentRotation = new JTextField();
            panelParameters.add(textFieldComponentRotation, "4, 6"); //$NON-NLS-1$
            textFieldComponentRotation.setColumns(10);
            textFieldComponentRotation.setToolTipText(Translations.getString("ReferenceRotatedTrayFeederConfigurationWizard.ComponentRotation.ToolTip")); //$NON-NLS-1$

            JLabel lblComponentZHeight = new JLabel(Translations.getString("ReferenceRotatedTrayFeederConfigurationWizard.ZHeight")); //$NON-NLS-1$
            panelParameters.add(lblComponentZHeight, "6, 6"); //$NON-NLS-1$

            textFieldComponentZHeight = new JTextField();
            panelParameters.add(textFieldComponentZHeight, "8, 6"); //$NON-NLS-1$
            textFieldComponentZHeight.setColumns(10);

            JSeparator separator = new JSeparator();
            panelParameters.add(separator, "1, 9, 8, 1"); //$NON-NLS-1$

            JButton btnCalcOffsetsRotation = new JButton(
                    new AbstractAction(Translations.getString("ReferenceRotatedTrayFeederConfigurationWizard.CalculateOffsetsAndTrayRotation")) { //$NON-NLS-1$
                        @Override
                        public void actionPerformed(ActionEvent e) {

                            try {
                                offsetsAndRotation.setLocation(calculateOffsetsAndRotation());
                            }
                            catch (Exception e1) {
                                MessageBoxes.errorBox(getTopLevelAncestor(), Translations.getString("ReferenceRotatedTrayFeederConfigurationWizard.Error"), e1.getMessage()); //$NON-NLS-1$
                                return;
                            }
                        }
                    });
            btnCalcOffsetsRotation.setHorizontalAlignment(SwingConstants.LEFT);
            panelParameters.add(btnCalcOffsetsRotation, "2, 12"); //$NON-NLS-1$

            JLabel lblRowOffset = new JLabel(Translations.getString("ReferenceRotatedTrayFeederConfigurationWizard.ColumnOffset")); //$NON-NLS-1$
            panelParameters.add(lblRowOffset, "2, 14"); //$NON-NLS-1$

            textFieldOffsetsX = new JTextField();
            panelParameters.add(textFieldOffsetsX, "4, 14"); //$NON-NLS-1$
            textFieldOffsetsX.setColumns(10);

            JLabel lblColOffset = new JLabel(Translations.getString("ReferenceRotatedTrayFeederConfigurationWizard.RowOffset")); //$NON-NLS-1$
            panelParameters.add(lblColOffset, "6, 14"); //$NON-NLS-1$

            textFieldOffsetsY = new JTextField();
            panelParameters.add(textFieldOffsetsY, "8, 14, "); //$NON-NLS-1$
            textFieldOffsetsY.setColumns(10);

            JLabel lblTrayRotation = new JLabel(Translations.getString("ReferenceRotatedTrayFeederConfigurationWizard.TrayRotation")); //$NON-NLS-1$
            panelParameters.add(lblTrayRotation, "2, 16"); //$NON-NLS-1$

            textFieldTrayRotation = new JTextField();
            textFieldTrayRotation.setToolTipText(Translations.getString("ReferenceRotatedTrayFeederConfigurationWizard.TrayRotation.ToolTip")); //$NON-NLS-1$
            panelParameters.add(textFieldTrayRotation, "4, 16"); //$NON-NLS-1$
            textFieldTrayRotation.setColumns(10);

            //This need to come after textFieldLocationX, textFieldLocationY and 
            //textFieldTrayRotation are initialized
            locationButtonsPanel = new LocationButtonsPanel(textFieldLocationX, textFieldLocationY,
                    null, textFieldTrayRotation);
            panelLocation.add(locationButtonsPanel, "8, 4"); //$NON-NLS-1$

            panelIllustration = new JPanel();
            panelIllustration.setBorder(new TitledBorder(null,
                    Translations.getString("ReferenceRotatedTrayFeederConfigurationWizard.TrayIllustration"), TitledBorder.LEADING, TitledBorder.TOP, null)); //$NON-NLS-1$
            contentPanel.add(panelIllustration);

            InputStream stream = getClass().getResourceAsStream("/illustrations/rotatedtrayfeeder.png"); //$NON-NLS-1$
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

        //---------------------------------------------------------------------------------------//
        //These bindings are used to access the various text fields of the GUI without having to
        //parse each text field when getting the value and format each value when setting the value.
        //These bindings must come before any of the other bindings so they don't step on the toes
        //of those other bindings.
        bind(UpdateStrategy.READ_WRITE, firstRowFirstColumn, "lengthX", textFieldLocationX, "text", lengthConverter); //$NON-NLS-1$ //$NON-NLS-2$
        bind(UpdateStrategy.READ_WRITE, firstRowFirstColumn, "lengthY", textFieldLocationY, "text", lengthConverter); //$NON-NLS-1$ //$NON-NLS-2$

        bind(UpdateStrategy.READ_WRITE, firstRowLastColumn, "lengthX", textFieldFirstRowLastLocationX, "text", lengthConverter); //$NON-NLS-1$ //$NON-NLS-2$
        bind(UpdateStrategy.READ_WRITE, firstRowLastColumn, "lengthY", textFieldFirstRowLastLocationY, "text", lengthConverter); //$NON-NLS-1$ //$NON-NLS-2$

        bind(UpdateStrategy.READ_WRITE, lastRowLastColumn, "lengthX", textFieldLastLocationX, "text", lengthConverter); //$NON-NLS-1$ //$NON-NLS-2$
        bind(UpdateStrategy.READ_WRITE, lastRowLastColumn, "lengthY", textFieldLastLocationY, "text", lengthConverter); //$NON-NLS-1$ //$NON-NLS-2$

        bind(UpdateStrategy.READ_WRITE, this, "nRows", textFieldTrayCountRows, "text", intConverter); //$NON-NLS-1$ //$NON-NLS-2$
        bind(UpdateStrategy.READ_WRITE, this, "nCols", textFieldTrayCountCols, "text", intConverter); //$NON-NLS-1$ //$NON-NLS-2$

        bind(UpdateStrategy.READ_WRITE, this, "wizardFeedCount", textFieldFeedCount, "text", intConverter); //$NON-NLS-1$ //$NON-NLS-2$

        bind(UpdateStrategy.READ_WRITE, offsetsAndRotation, "lengthX", textFieldOffsetsX, "text", lengthConverter); //$NON-NLS-1$ //$NON-NLS-2$
        bind(UpdateStrategy.READ_WRITE, offsetsAndRotation, "lengthY", textFieldOffsetsY, "text", lengthConverter); //$NON-NLS-1$ //$NON-NLS-2$
        bind(UpdateStrategy.READ_WRITE, offsetsAndRotation, "rotation", textFieldTrayRotation, "text", doubleConverter); //$NON-NLS-1$ //$NON-NLS-2$
        //---------------------------------------------------------------------------------------//

        addWrappedBinding(feeder, "part", comboBoxPart, "selectedItem"); //$NON-NLS-1$ //$NON-NLS-2$
        addWrappedBinding(feeder, "feedRetryCount", retryCountTf, "text", intConverter); //$NON-NLS-1$ //$NON-NLS-2$
        addWrappedBinding(feeder, "pickRetryCount", pickRetryCount, "text", intConverter); //$NON-NLS-1$ //$NON-NLS-2$

        if (includePickLocation) {
            MutableLocationProxy location = new MutableLocationProxy();
            bind(UpdateStrategy.READ_WRITE, feeder, "location", location, "location"); //$NON-NLS-1$ //$NON-NLS-2$
            addWrappedBinding(location, "lengthX", textFieldLocationX, "text", lengthConverter); //$NON-NLS-1$ //$NON-NLS-2$
            addWrappedBinding(location, "lengthY", textFieldLocationY, "text", lengthConverter); //$NON-NLS-1$ //$NON-NLS-2$
            addWrappedBinding(location, "rotation", textFieldTrayRotation, "text", doubleConverter); //$NON-NLS-1$ //$NON-NLS-2$
            addWrappedBinding(location, "lengthZ", textFieldComponentZHeight, "text", lengthConverter); //$NON-NLS-1$ //$NON-NLS-2$

            addWrappedBinding(feeder, "componentRotationInTray", textFieldComponentRotation,  //$NON-NLS-1$
                    "text", doubleConverter); //$NON-NLS-1$

            MutableLocationProxy firstRowLastComponentlocation = new MutableLocationProxy();
            bind(UpdateStrategy.READ_WRITE, feeder, "firstRowLastComponentLocation",  //$NON-NLS-1$
                    firstRowLastComponentlocation, "location"); //$NON-NLS-1$
            addWrappedBinding(firstRowLastComponentlocation, "lengthX",  //$NON-NLS-1$
                    textFieldFirstRowLastLocationX, "text", lengthConverter); //$NON-NLS-1$
            addWrappedBinding(firstRowLastComponentlocation, "lengthY",  //$NON-NLS-1$
                    textFieldFirstRowLastLocationY, "text", lengthConverter); //$NON-NLS-1$

            MutableLocationProxy lastComponentlocation = new MutableLocationProxy();
            bind(UpdateStrategy.READ_WRITE, feeder, "lastComponentLocation",  //$NON-NLS-1$
                    lastComponentlocation, "location"); //$NON-NLS-1$
            addWrappedBinding(lastComponentlocation, "lengthX", textFieldLastLocationX, "text",  //$NON-NLS-1$ //$NON-NLS-2$
                    lengthConverter);
            addWrappedBinding(lastComponentlocation, "lengthY", textFieldLastLocationY, "text",  //$NON-NLS-1$ //$NON-NLS-2$
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
        bind(UpdateStrategy.READ_WRITE, feeder, "offsets", offsets, "location"); //$NON-NLS-1$ //$NON-NLS-2$
        addWrappedBinding(offsets, "lengthX", textFieldOffsetsX, "text", lengthConverter); //$NON-NLS-1$ //$NON-NLS-2$
        addWrappedBinding(offsets, "lengthY", textFieldOffsetsY, "text", lengthConverter); //$NON-NLS-1$ //$NON-NLS-2$

        addWrappedBinding(feeder, "trayCountCols", textFieldTrayCountCols, "text", intConverter); //$NON-NLS-1$ //$NON-NLS-2$
        addWrappedBinding(feeder, "trayCountRows", textFieldTrayCountRows, "text", intConverter); //$NON-NLS-1$ //$NON-NLS-2$

        addWrappedBinding(feeder, "feedCount", textFieldFeedCount, "text", intConverter); //$NON-NLS-1$ //$NON-NLS-2$

        bind(UpdateStrategy.READ, feeder, "remainingCount", lblComponentCount, "text",  //$NON-NLS-1$ //$NON-NLS-2$
                new Converter<Integer, String>() {

            @Override
            public String convertForward(Integer count) {
                return Translations.getString("ReferenceRotatedTrayFeederConfigurationWizard.ComponentsRemaining") + String.valueOf(count); //$NON-NLS-1$
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

    public int getnRows() {
        return nRows;
    }

    public void setnRows(int nRows) {
        this.nRows = nRows;
    }

    public int getnCols() {
        return nCols;
    }

    public void setnCols(int nCols) {
        this.nCols = nCols;
    }

    public int getwizardFeedCount() {
        return wizardFeedCount;
    }

    public void setwizardFeedCount(int wizardFeedCount) {
        int oldValue = this.wizardFeedCount;
        this.wizardFeedCount = wizardFeedCount;
        firePropertyChange("wizardFeedCount", oldValue, wizardFeedCount);
    }

    /**
     * Calculates the tray's x (column) and y (row) offsets as well as the tray rotation.
     * @return the offsets and rotation
     */
    public Location calculateOffsetsAndRotation() throws Exception {
        if (nCols < 1 || nRows < 1) {
            throw new Exception(Translations.getString("ReferenceRotatedTrayFeederConfigurationWizard.ErrorMessage.AtLeastOneRowAndOneColumn")); //$NON-NLS-1$
        }

        // Distance Point A -> Point B
        Length abLength = firstRowFirstColumn.getLocation().
                getLinearLengthTo(firstRowLastColumn.getLocation());

        if ((abLength.getValue() > 0) && (nCols == 1)) {
            throw new Exception(Translations.getString("ReferenceRotatedTrayFeederConfigurationWizard.ErrorMessage.SingleColumnInconsistency")); //$NON-NLS-1$
        }
        if ((abLength.getValue() == 0) && (nCols > 1)) {
            throw new Exception(String.format(Translations.getString("ReferenceRotatedTrayFeederConfigurationWizard.ErrorMessage.MultipleColumnInconsistency"), nCols)); //$NON-NLS-1$
        }

        // Distance Point B -> Point C
        Length bcLength = firstRowLastColumn.getLocation().
                getLinearLengthTo(lastRowLastColumn.getLocation());

        if ((bcLength.getValue() > 0) && (nRows == 1)) {
            throw new Exception(Translations.getString("ReferenceRotatedTrayFeederConfigurationWizard.ErrorMessage.SingleRowInconsistency")); //$NON-NLS-1$
        }
        if ((bcLength.getValue() == 0) && (nRows > 1)) {
            throw new Exception(String.format(Translations.getString("ReferenceRotatedTrayFeederConfigurationWizard.ErrorMessage.MultipleRowInconsistency"), nRows)); //$NON-NLS-1$
        }

        Length colStep = nCols > 1 ? abLength.divide(nCols-1) : new Length(0, LengthUnit.Millimeters);
        Length rowStep = nRows > 1 ? bcLength.divide(nRows-1) : new Length(0, LengthUnit.Millimeters);

        // Angle of the rows and columns relative to the machine
        double rowAngleDeg = Utils2D.getAngleFromPoint(firstRowFirstColumn.getLocation(), 
                firstRowLastColumn.getLocation());
        double colAngleDeg = Utils2D.getAngleFromPoint(firstRowLastColumn.getLocation(), 
                lastRowLastColumn.getLocation());

        if ((nRows > 1) && (nCols > 1)) {
            //Compute angle ABC (the angle between the rows and columns)
            double checkAngleDeg = Utils2D.normalizeAngle180(rowAngleDeg - colAngleDeg);

            //Verify angle ABC is near +/-90 degrees, if not, throw an exception
            double checkDeg = Math.abs(checkAngleDeg);
            if ((checkDeg < 90-RIGHT_ANGLE_TOLERANCE) || (checkDeg > 90+RIGHT_ANGLE_TOLERANCE)) {
                throw new Exception(String.format(Translations.getString("ReferenceRotatedTrayFeederConfigurationWizard.ErrorMessage.TrayAngleNot90"), checkDeg)); //$NON-NLS-1$
            }

            //If the angle is negative, the points were defined going the opposite way around the 
            //feeder than shown in the illustration. In that case, we need to negate the row offset
            //so that the feed moves in the correct direction when stepping to the next row.
            if (checkAngleDeg < 0) {
                rowStep = rowStep.multiply(-1);
            }
        }

        //Initialize the tray rotation to that shown in the GUI, this will be used if the feeder has
        //only a single column and single row
        double rotDeg = offsetsAndRotation.getRotation();
        if (nCols > 1) {
            //We have multiple columns so the tray rotation is the angle of the row(s)
            rotDeg = rowAngleDeg;
        }
        else if (nRows > 1) {
            //We have only a single column but multiple rows so the tray rotation is the angle of
            //the column plus 90 degrees
            rotDeg = colAngleDeg + 90;
        }

        LengthUnit units = Configuration.get().getSystemUnits();
        return new Location(units, colStep.convertToUnits(units).getValue(), 
                rowStep.convertToUnits(units).getValue(), 0, rotDeg);
    }

    @Override
    public void validateInput() throws Exception {
        //Make sure the feed count isn't pointing beyond the end of the feeder
        if (wizardFeedCount < 0) {
            setwizardFeedCount(0);
        }
        if (wizardFeedCount > nCols*nRows) {
            setwizardFeedCount(nCols*nRows);
        }

        //Compute offsets and rotation from points A, B, C, number of rows and number of columns
        Location offsetsAndRotation = calculateOffsetsAndRotation().convertToUnits(VALIDATION_UNITS);

        //The offsets and rotation shown on the GUI
        double offsetX = this.offsetsAndRotation.getLengthX().convertToUnits(VALIDATION_UNITS).getValue();
        double offsetY = this.offsetsAndRotation.getLengthY().convertToUnits(VALIDATION_UNITS).getValue();
        double rot = this.offsetsAndRotation.getRotation();

        //Compare them and if any are too different, throw an exception - note, all values are
        //still saved regardless of this exception being thrown or not
        if ((Math.abs(offsetsAndRotation.getX() - offsetX) > VALIDATION_TOLERANCE) ||
                (Math.abs(offsetsAndRotation.getY() - offsetY) > VALIDATION_TOLERANCE) ||
                (Math.abs(offsetsAndRotation.getRotation() - rot) > VALIDATION_TOLERANCE)) {
            throw new Exception(
                    Translations.getString("ReferenceRotatedTrayFeederConfigurationWizard.ErrorMessage.OffsetsAndRotationInconsistency")); //$NON-NLS-1$
        }
    }

}
