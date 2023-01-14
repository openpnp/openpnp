/*
 * Copyright (C) 2022 Jason von Nieda <jason@vonnieda.org>, Tony Luken <tonyluken62+openpnp@gmail.com>
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

package org.openpnp.gui.panelization;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.Translations;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.viewers.PlacementsHolderLocationViewer;
import org.openpnp.model.Board;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Panel;
import org.openpnp.model.PanelLocation;
import org.openpnp.model.PlacementsHolderLocation;
import org.openpnp.util.BeanUtils;
import org.openpnp.util.Utils2D;
import javax.swing.border.EtchedBorder;

@SuppressWarnings("serial")
public class PanelArrayBuilderDialog extends JDialog {
    private static final String PREF_WINDOW_X = "PanelArrayBuilderDialog.windowX"; //$NON-NLS-1$
    private static final int PREF_WINDOW_X_DEF = 100;
    private static final String PREF_WINDOW_Y = "PanelArrayBuilderDialog.windowY"; //$NON-NLS-1$
    private static final int PREF_WINDOW_Y_DEF = 100;
    private static final String PREF_WINDOW_WIDTH = "PanelArrayBuilderDialog.windowWidth"; //$NON-NLS-1$
    private static final int PREF_WINDOW_WIDTH_DEF = 800;
    private static final String PREF_WINDOW_HEIGHT = "PanelArrayBuilderDialog.windowHeight"; //$NON-NLS-1$
    private static final int PREF_WINDOW_HEIGHT_DEF = 600;

    private enum ArrayType {
        Rectangular, Circular;
        
        @Override
        public String toString() {
            switch (this) {
                case Circular:
                    return Translations.getString("PanelArrayBuilderDialog.ArrayType.Circular"); //$NON-NLS-1$
                case Rectangular:
                    return Translations.getString("PanelArrayBuilderDialog.ArrayType.Rectangular"); //$NON-NLS-1$
                default:
                    return Translations.getString("PanelArrayBuilderDialog.ArrayType.Unknown"); //$NON-NLS-1$
            }
        }
    }
    
    private LengthUnit systemUnit = Configuration.get().getSystemUnits();

    private List<PlacementsHolderLocation<?>> newChildren = new ArrayList<>();
    private ArrayType arrayType = ArrayType.Rectangular;
    private final JPanel contentPanel = new JPanel();
    private JTextField textFieldRowSpacing;
    private JTextField textFieldColumnSpacing;
    private JTextField textFieldAlternateOffset;
    private JTextField textFieldCenterX;
    private JTextField textFieldCenterY;
    private final ButtonGroup buttonGroup = new ButtonGroup();
    private JPanel panelControls;
    private PanelLocation panelLocation;
    private PlacementsHolderLocation<?> rootChildLocation;
    private Runnable refresh;
    protected BufferedImage panelImage;
    private int columnCount = 1;
    private int rowCount = 1;
    private int angularCount = 1;
    private int radialCount = 1;
    private Length columnSpacing = new Length(10, LengthUnit.Millimeters);
    private Length rowSpacing = new Length(10, LengthUnit.Millimeters);
    private Length alternateOffset = new Length(0, LengthUnit.Millimeters);
    private int alternateRowColumnDelta = 0;
    private Length centerX = new Length(0, LengthUnit.Millimeters);
    private Length centerY = new Length(0, LengthUnit.Millimeters);
    private boolean increaseProportionally = false;
    private boolean panelReferenceFrame = true;
    private PlacementsHolderLocationViewer panelLayout;
    private Preferences prefs = Preferences.userNodeForPackage(PanelArrayBuilderDialog.class);

    private String rootChildId;

    private boolean dirtyState;

    private WindowAdapter windowCloseListener;

    /**
     * Create the dialog.
     */
    public PanelArrayBuilderDialog(PanelLocation panelLocation, 
            PlacementsHolderLocation<?> rootChildLocation, Runnable refresh) {
        super(MainFrame.get(), 
                Translations.getString("PanelArrayBuilderDialog.Frame.Title"), //$NON-NLS-1$
                ModalityType.APPLICATION_MODAL);
        this.panelLocation = panelLocation;
        this.rootChildLocation = rootChildLocation;
        this.refresh = refresh;
        this.rootChildId  = rootChildLocation.getId();
        this.dirtyState = panelLocation.isDirty();
        
        windowCloseListener = new WindowAdapter( ) {

            @Override
            public void windowClosing(WindowEvent e) {
                cancel();
            }
        };
        addWindowListener(windowCloseListener);

        if (prefs.getInt(PREF_WINDOW_WIDTH, 50) < 50) {
            prefs.putInt(PREF_WINDOW_WIDTH, PREF_WINDOW_WIDTH_DEF);
        }

        if (prefs.getInt(PREF_WINDOW_HEIGHT, 50) < 50) {
            prefs.putInt(PREF_WINDOW_HEIGHT, PREF_WINDOW_HEIGHT_DEF);
        }

        setBounds(prefs.getInt(PREF_WINDOW_X, PREF_WINDOW_X_DEF),
                prefs.getInt(PREF_WINDOW_Y, PREF_WINDOW_Y_DEF),
                prefs.getInt(PREF_WINDOW_WIDTH, PREF_WINDOW_WIDTH_DEF),
                prefs.getInt(PREF_WINDOW_HEIGHT, PREF_WINDOW_HEIGHT_DEF));
        if (prefs.getInt(PREF_WINDOW_X, Integer.MIN_VALUE) == Integer.MIN_VALUE) {
            setLocationRelativeTo(MainFrame.get());
            prefs.putInt(PREF_WINDOW_X, getLocation().x);
            prefs.putInt(PREF_WINDOW_Y, getLocation().y);
        }
        
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                prefs.putInt(PREF_WINDOW_X, getLocation().x);
                prefs.putInt(PREF_WINDOW_Y, getLocation().y);
            }

            @Override
            public void componentResized(ComponentEvent e) {
                prefs.putInt(PREF_WINDOW_WIDTH, getSize().width);
                prefs.putInt(PREF_WINDOW_HEIGHT, getSize().height);
            }
        });
        
        getContentPane().setLayout(new BorderLayout());
        contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        getContentPane().add(contentPanel, BorderLayout.CENTER);
        contentPanel.setLayout(new BorderLayout(0, 0));
        {
            {
                JPanel panel = new JPanel();
                contentPanel.add(panel, BorderLayout.NORTH);
                GridBagLayout gbl_panel = new GridBagLayout();
                gbl_panel.columnWidths = new int[]{104, 198, 0};
                gbl_panel.rowHeights = new int[] {5, 30, 5};
                gbl_panel.columnWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
                gbl_panel.rowWeights = new double[]{0.0, 0.0, 0.0};
                panel.setLayout(gbl_panel);
                {
                    JLabel lblNewLabel_13 = new JLabel(
                            Translations.getString("PanelArrayBuilderDialog.Label.ArrayType")); //$NON-NLS-1$
                    GridBagConstraints gbc_lblNewLabel_13 = new GridBagConstraints();
                    gbc_lblNewLabel_13.anchor = GridBagConstraints.EAST;
                    gbc_lblNewLabel_13.fill = GridBagConstraints.VERTICAL;
                    gbc_lblNewLabel_13.insets = new Insets(0, 0, 5, 5);
                    gbc_lblNewLabel_13.gridx = 0;
                    gbc_lblNewLabel_13.gridy = 1;
                    panel.add(lblNewLabel_13, gbc_lblNewLabel_13);
                }
                JComboBox<ArrayType> comboBoxArrayType = new JComboBox<>();
                GridBagConstraints gbc_comboBoxArrayType = new GridBagConstraints();
                gbc_comboBoxArrayType.insets = new Insets(0, 0, 5, 0);
                gbc_comboBoxArrayType.anchor = GridBagConstraints.NORTH;
                gbc_comboBoxArrayType.fill = GridBagConstraints.HORIZONTAL;
                gbc_comboBoxArrayType.gridx = 1;
                gbc_comboBoxArrayType.gridy = 1;
                panel.add(comboBoxArrayType, gbc_comboBoxArrayType);
                comboBoxArrayType.setToolTipText(
                        Translations.getString("PanelArrayBuilderDialog.ComboBoxArrayType.ToolTip")); //$NON-NLS-1$
                comboBoxArrayType.setModel(new DefaultComboBoxModel<ArrayType>(ArrayType.values()));
                comboBoxArrayType.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        CardLayout cardLayout = (CardLayout)(panelControls.getLayout());
                        arrayType = (ArrayType) comboBoxArrayType.getSelectedItem();
                        switch (arrayType) {
                            case Rectangular:
                                cardLayout.show(panelControls, "Rectangular"); //$NON-NLS-1$
                                break;
                            case Circular:
                                cardLayout.show(panelControls, "Circular"); //$NON-NLS-1$
                                break;
                        }
                        generateArray();
                    }
                    
                });
            }
        }
        {
            JPanel panel = new JPanel();
            contentPanel.add(panel, BorderLayout.CENTER);
            panel.setLayout(new BorderLayout(0, 0));
            {
                panelControls = new JPanel();
                panelControls.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
                panel.add(panelControls, BorderLayout.NORTH);
                panelControls.setLayout(new CardLayout(0, 0));
                {
                    JPanel panelRectangular = new JPanel();
                    panelControls.add(panelRectangular,
                            "Rectangular"); //$NON-NLS-1$
                    GridBagLayout gbl_panelRectangular = new GridBagLayout();
                    gbl_panelRectangular.columnWidths = new int[]{0, 80, 80, 0, 0, 0, 0, 0};
                    gbl_panelRectangular.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0};
                    gbl_panelRectangular.columnWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
                    gbl_panelRectangular.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
                    panelRectangular.setLayout(gbl_panelRectangular);
                    {
                        JLabel lblNewLabel_2 = new JLabel(
                                Translations.getString("PanelArrayBuilderDialog.Rectangular.Label.Columns")); //$NON-NLS-1$
                        GridBagConstraints gbc_lblNewLabel_2 = new GridBagConstraints();
                        gbc_lblNewLabel_2.insets = new Insets(0, 0, 5, 5);
                        gbc_lblNewLabel_2.gridx = 1;
                        gbc_lblNewLabel_2.gridy = 1;
                        panelRectangular.add(lblNewLabel_2, gbc_lblNewLabel_2);
                    }
                    {
                        JLabel lblNewLabel_1 = new JLabel(
                                Translations.getString("PanelArrayBuilderDialog.Rectangular.Label.Rows")); //$NON-NLS-1$
                        GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
                        gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
                        gbc_lblNewLabel_1.gridx = 2;
                        gbc_lblNewLabel_1.gridy = 1;
                        panelRectangular.add(lblNewLabel_1, gbc_lblNewLabel_1);
                    }
                    {
                        JLabel lblNewLabel = new JLabel(
                                Translations.getString("PanelArrayBuilderDialog.Rectangular.Label.Count")); //$NON-NLS-1$
                        GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
                        gbc_lblNewLabel.anchor = GridBagConstraints.EAST;
                        gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
                        gbc_lblNewLabel.gridx = 0;
                        gbc_lblNewLabel.gridy = 2;
                        panelRectangular.add(lblNewLabel, gbc_lblNewLabel);
                    }
                    {
                        JSpinner spinnerColumns = new JSpinner();
                        spinnerColumns.setToolTipText(
                                Translations.getString("PanelArrayBuilderDialog.Rectangular.SpinnerColumns.ToolTip")); //$NON-NLS-1$
                        spinnerColumns.setModel(new SpinnerNumberModel(1, 1, null, 1));
                        GridBagConstraints gbc_spinnerColumns = new GridBagConstraints();
                        gbc_spinnerColumns.fill = GridBagConstraints.HORIZONTAL;
                        gbc_spinnerColumns.insets = new Insets(0, 0, 5, 5);
                        gbc_spinnerColumns.gridx = 1;
                        gbc_spinnerColumns.gridy = 2;
                        panelRectangular.add(spinnerColumns, gbc_spinnerColumns);
                        BeanUtils.bind(UpdateStrategy.READ_WRITE, this, "columnCount", //$NON-NLS-1$
                                spinnerColumns, "value"); //$NON-NLS-1$
                    }
                    {
                        JSpinner spinnerRows = new JSpinner();
                        spinnerRows.setToolTipText(
                                Translations.getString("PanelArrayBuilderDialog.Rectangular.SpinnerRows.ToolTip")); //$NON-NLS-1$
                        spinnerRows.setModel(new SpinnerNumberModel(1, 1, null, 1));
                        GridBagConstraints gbc_spinnerRows = new GridBagConstraints();
                        gbc_spinnerRows.fill = GridBagConstraints.HORIZONTAL;
                        gbc_spinnerRows.insets = new Insets(0, 0, 5, 5);
                        gbc_spinnerRows.gridx = 2;
                        gbc_spinnerRows.gridy = 2;
                        panelRectangular.add(spinnerRows, gbc_spinnerRows);
                        BeanUtils.bind(UpdateStrategy.READ_WRITE, this, "rowCount", //$NON-NLS-1$
                                spinnerRows, "value"); //$NON-NLS-1$
                    }
                    {
                        JLabel lblNewLabel_3 = new JLabel(
                                Translations.getString("PanelArrayBuilderDialog.Rectangular.Label.Step")); //$NON-NLS-1$
                        GridBagConstraints gbc_lblNewLabel_3 = new GridBagConstraints();
                        gbc_lblNewLabel_3.anchor = GridBagConstraints.EAST;
                        gbc_lblNewLabel_3.insets = new Insets(0, 0, 5, 5);
                        gbc_lblNewLabel_3.gridx = 0;
                        gbc_lblNewLabel_3.gridy = 3;
                        panelRectangular.add(lblNewLabel_3, gbc_lblNewLabel_3);
                    }
                    {
                        textFieldColumnSpacing = new JTextField();
                        textFieldColumnSpacing.setToolTipText(
                                Translations.getString("PanelArrayBuilderDialog.Rectangular.TextFieldColumnSpacing.ToolTip")); //$NON-NLS-1$
                        GridBagConstraints gbc_textFieldColumnSpacing = new GridBagConstraints();
                        gbc_textFieldColumnSpacing.insets = new Insets(0, 0, 5, 5);
                        gbc_textFieldColumnSpacing.fill = GridBagConstraints.HORIZONTAL;
                        gbc_textFieldColumnSpacing.gridx = 1;
                        gbc_textFieldColumnSpacing.gridy = 3;
                        panelRectangular.add(textFieldColumnSpacing, gbc_textFieldColumnSpacing);
                        textFieldColumnSpacing.setColumns(10);
                        BeanUtils.bind(UpdateStrategy.READ_WRITE, this, "columnSpacing", //$NON-NLS-1$
                                textFieldColumnSpacing, "text", new LengthConverter()); //$NON-NLS-1$
                        ComponentDecorators.decorateWithLengthConversion(textFieldColumnSpacing);
                    }
                    {
                        textFieldRowSpacing = new JTextField();
                        textFieldRowSpacing.setToolTipText(
                                Translations.getString("PanelArrayBuilderDialog.Rectangular.TextFieldRowSpacing.ToolTip")); //$NON-NLS-1$
                        GridBagConstraints gbc_textFieldRowSpacing = new GridBagConstraints();
                        gbc_textFieldRowSpacing.insets = new Insets(0, 0, 5, 5);
                        gbc_textFieldRowSpacing.fill = GridBagConstraints.HORIZONTAL;
                        gbc_textFieldRowSpacing.gridx = 2;
                        gbc_textFieldRowSpacing.gridy = 3;
                        panelRectangular.add(textFieldRowSpacing, gbc_textFieldRowSpacing);
                        textFieldRowSpacing.setColumns(10);
                        BeanUtils.bind(UpdateStrategy.READ_WRITE, this, "rowSpacing", //$NON-NLS-1$
                                textFieldRowSpacing, "text", new LengthConverter()); //$NON-NLS-1$
                        ComponentDecorators.decorateWithLengthConversion(textFieldRowSpacing);
                    }
                    {
                        JLabel lblNewLabel_6 = new JLabel(
                                Translations.getString("PanelArrayBuilderDialog.Rectangular.Label.AlternateRowsComboBox")); //$NON-NLS-1$
                        GridBagConstraints gbc_lblNewLabel_6 = new GridBagConstraints();
                        gbc_lblNewLabel_6.anchor = GridBagConstraints.EAST;
                        gbc_lblNewLabel_6.insets = new Insets(0, 0, 5, 5);
                        gbc_lblNewLabel_6.gridx = 0;
                        gbc_lblNewLabel_6.gridy = 5;
                        panelRectangular.add(lblNewLabel_6, gbc_lblNewLabel_6);
                    }
                    {
                        String[] alternateOptions = new String[] {
                                Translations.getString("PanelArrayBuilderDialog.Rectangular.AlternateRowsComboBox.Option.SameNumberColumns"),  //$NON-NLS-1$
                                Translations.getString("PanelArrayBuilderDialog.Rectangular.AlternateRowsComboBox.Option.OneMoreColumn"),  //$NON-NLS-1$
                                Translations.getString("PanelArrayBuilderDialog.Rectangular.AlternateRowsComboBox.Option.OneLessColumn")}; //$NON-NLS-1$

                        JComboBox<String> comboBoxAlternateRows = new JComboBox<String>();
                        comboBoxAlternateRows.setToolTipText(
                                Translations.getString("PanelArrayBuilderDialog.Rectangular.AlternateRowsComboBox.ToolTip")); //$NON-NLS-1$
                        comboBoxAlternateRows.setModel(new DefaultComboBoxModel<String>(alternateOptions));
                        GridBagConstraints gbc_comboBoxAlternateRows = new GridBagConstraints();
                        gbc_comboBoxAlternateRows.gridwidth = 5;
                        gbc_comboBoxAlternateRows.insets = new Insets(0, 0, 5, 5);
                        gbc_comboBoxAlternateRows.fill = GridBagConstraints.HORIZONTAL;
                        gbc_comboBoxAlternateRows.gridx = 1;
                        gbc_comboBoxAlternateRows.gridy = 5;
                        panelRectangular.add(comboBoxAlternateRows, gbc_comboBoxAlternateRows);
                        comboBoxAlternateRows.addActionListener(new ActionListener() {

                            @Override
                            public void actionPerformed(ActionEvent e) {
                                switch (comboBoxAlternateRows.getSelectedIndex()) {
                                    case 0:
                                        setAlternateRowColumnDelta(0);
                                        break;
                                    case 1:
                                        setAlternateRowColumnDelta(1);
                                        break;
                                    case 2:
                                        setAlternateRowColumnDelta(-1);
                                        break;
                                }
                            }});
                    }
                    {
                        JLabel lblNewLabel_5 = new JLabel(
                                Translations.getString("PanelArrayBuilderDialog.Rectangular.Label.AlternateRowColumnOffset.Leader")); //$NON-NLS-1$
                        GridBagConstraints gbc_lblNewLabel_5 = new GridBagConstraints();
                        gbc_lblNewLabel_5.anchor = GridBagConstraints.EAST;
                        gbc_lblNewLabel_5.insets = new Insets(0, 0, 5, 5);
                        gbc_lblNewLabel_5.gridx = 0;
                        gbc_lblNewLabel_5.gridy = 6;
                        panelRectangular.add(lblNewLabel_5, gbc_lblNewLabel_5);
                    }
                    {
                        textFieldAlternateOffset = new JTextField();
                        textFieldAlternateOffset.setToolTipText(
                                Translations.getString("PanelArrayBuilderDialog.Rectangular.TextFieldAlternateOffset.ToolTip")); //$NON-NLS-1$
                        GridBagConstraints gbc_textFieldAlternateOffset = new GridBagConstraints();
                        gbc_textFieldAlternateOffset.insets = new Insets(0, 0, 5, 5);
                        gbc_textFieldAlternateOffset.fill = GridBagConstraints.HORIZONTAL;
                        gbc_textFieldAlternateOffset.gridx = 1;
                        gbc_textFieldAlternateOffset.gridy = 6;
                        panelRectangular.add(textFieldAlternateOffset, gbc_textFieldAlternateOffset);
                        textFieldAlternateOffset.setColumns(10);
                        BeanUtils.bind(UpdateStrategy.READ_WRITE, this, "alternateOffset", //$NON-NLS-1$
                                textFieldAlternateOffset, "text", new LengthConverter()); //$NON-NLS-1$
                        ComponentDecorators.decorateWithLengthConversion(textFieldAlternateOffset);
                    }
                    {
                        JLabel lblNewLabel_8 = new JLabel(
                                Translations.getString("PanelArrayBuilderDialog.Rectangular.Label.AlternateRowColumnOffset.Trailer")); //$NON-NLS-1$
                        GridBagConstraints gbc_lblNewLabel_8 = new GridBagConstraints();
                        gbc_lblNewLabel_8.anchor = GridBagConstraints.WEST;
                        gbc_lblNewLabel_8.gridwidth = 4;
                        gbc_lblNewLabel_8.insets = new Insets(0, 0, 5, 5);
                        gbc_lblNewLabel_8.gridx = 2;
                        gbc_lblNewLabel_8.gridy = 6;
                        panelRectangular.add(lblNewLabel_8, gbc_lblNewLabel_8);
                    }
                }
                {
                    JPanel panelCircular = new JPanel();
                    panelControls.add(panelCircular, "Circular"); //$NON-NLS-1$
                    GridBagLayout gbl_panelCircular = new GridBagLayout();
                    gbl_panelCircular.columnWidths = new int[]{0, 80, 80, 0, 0, 0};
                    gbl_panelCircular.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0};
                    gbl_panelCircular.columnWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
                    gbl_panelCircular.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
                    panelCircular.setLayout(gbl_panelCircular);
                    {
                        JLabel lblNewLabel_11 = new JLabel("X"); //$NON-NLS-1$
                        GridBagConstraints gbc_lblNewLabel_11 = new GridBagConstraints();
                        gbc_lblNewLabel_11.insets = new Insets(0, 0, 5, 5);
                        gbc_lblNewLabel_11.gridx = 1;
                        gbc_lblNewLabel_11.gridy = 0;
                        panelCircular.add(lblNewLabel_11, gbc_lblNewLabel_11);
                    }
                    {
                        JLabel lblNewLabel_12 = new JLabel("Y"); //$NON-NLS-1$
                        GridBagConstraints gbc_lblNewLabel_12 = new GridBagConstraints();
                        gbc_lblNewLabel_12.insets = new Insets(0, 0, 5, 5);
                        gbc_lblNewLabel_12.gridx = 2;
                        gbc_lblNewLabel_12.gridy = 0;
                        panelCircular.add(lblNewLabel_12, gbc_lblNewLabel_12);
                    }
                    {
                        JLabel lblNewLabel_4 = new JLabel(
                                Translations.getString("PanelArrayBuilderDialog.Circular.Label.ArrayCenter")); //$NON-NLS-1$
                        GridBagConstraints gbc_lblNewLabel_4 = new GridBagConstraints();
                        gbc_lblNewLabel_4.anchor = GridBagConstraints.EAST;
                        gbc_lblNewLabel_4.insets = new Insets(0, 0, 5, 5);
                        gbc_lblNewLabel_4.gridx = 0;
                        gbc_lblNewLabel_4.gridy = 1;
                        panelCircular.add(lblNewLabel_4, gbc_lblNewLabel_4);
                    }
                    {
                        textFieldCenterX = new JTextField();
                        textFieldCenterX.setToolTipText(
                                Translations.getString("PanelArrayBuilderDialog.Circular.TextFieldCenterX.ToolTip")); //$NON-NLS-1$
                        GridBagConstraints gbc_textFieldCenterX = new GridBagConstraints();
                        gbc_textFieldCenterX.insets = new Insets(0, 0, 5, 5);
                        gbc_textFieldCenterX.fill = GridBagConstraints.HORIZONTAL;
                        gbc_textFieldCenterX.gridx = 1;
                        gbc_textFieldCenterX.gridy = 1;
                        panelCircular.add(textFieldCenterX, gbc_textFieldCenterX);
                        textFieldCenterX.setColumns(10);
                        BeanUtils.bind(UpdateStrategy.READ_WRITE, this, "centerX", //$NON-NLS-1$
                                textFieldCenterX, "text", new LengthConverter()); //$NON-NLS-1$
                        ComponentDecorators.decorateWithLengthConversion(textFieldCenterX);
                    }
                    {
                        textFieldCenterY = new JTextField();
                        textFieldCenterY.setToolTipText(
                                Translations.getString("PanelArrayBuilderDialog.Circular.TextFieldCenterY.ToolTip")); //$NON-NLS-1$
                        GridBagConstraints gbc_textFieldCenterY = new GridBagConstraints();
                        gbc_textFieldCenterY.insets = new Insets(0, 0, 5, 5);
                        gbc_textFieldCenterY.fill = GridBagConstraints.HORIZONTAL;
                        gbc_textFieldCenterY.gridx = 2;
                        gbc_textFieldCenterY.gridy = 1;
                        panelCircular.add(textFieldCenterY, gbc_textFieldCenterY);
                        textFieldCenterY.setColumns(10);
                        BeanUtils.bind(UpdateStrategy.READ_WRITE, this, "centerY", //$NON-NLS-1$
                                textFieldCenterY, "text", new LengthConverter()); //$NON-NLS-1$
                        ComponentDecorators.decorateWithLengthConversion(textFieldCenterY);
                    }
                    {
                        JLabel lblNewLabel_10 = new JLabel(
                                Translations.getString("PanelArrayBuilderDialog.Circular.Label.RelativeTo")); //$NON-NLS-1$
                        GridBagConstraints gbc_lblNewLabel_10 = new GridBagConstraints();
                        gbc_lblNewLabel_10.anchor = GridBagConstraints.EAST;
                        gbc_lblNewLabel_10.insets = new Insets(0, 0, 5, 5);
                        gbc_lblNewLabel_10.gridx = 0;
                        gbc_lblNewLabel_10.gridy = 2;
                        panelCircular.add(lblNewLabel_10, gbc_lblNewLabel_10);
                    }
                    {
                        JRadioButton rdbtnPanel = new JRadioButton(
                                Translations.getString("PanelArrayBuilderDialog.Circular.RadioButton.Panel")); //$NON-NLS-1$
                        rdbtnPanel.setToolTipText(
                                Translations.getString("PanelArrayBuilderDialog.Circular.RadioButton.Panel.ToolTip")); //$NON-NLS-1$
                        rdbtnPanel.setSelected(true);
                        buttonGroup.add(rdbtnPanel);
                        GridBagConstraints gbc_rdbtnPanel = new GridBagConstraints();
                        gbc_rdbtnPanel.anchor = GridBagConstraints.WEST;
                        gbc_rdbtnPanel.insets = new Insets(0, 0, 5, 5);
                        gbc_rdbtnPanel.gridx = 1;
                        gbc_rdbtnPanel.gridy = 2;
                        panelCircular.add(rdbtnPanel, gbc_rdbtnPanel);
                        BeanUtils.bind(UpdateStrategy.READ_WRITE, this, "panelReferenceFrame", //$NON-NLS-1$
                                rdbtnPanel, "selected"); //$NON-NLS-1$
                    }
                    {
                        JRadioButton rdbtnRootChild = new JRadioButton(
                                Translations.getString("PanelArrayBuilderDialog.Circular.RadioButton.RootChild")); //$NON-NLS-1$
                        rdbtnRootChild.setToolTipText(
                                Translations.getString("PanelArrayBuilderDialog.Circular.RadioButton.RootChild.ToolTip")); //$NON-NLS-1$
                        buttonGroup.add(rdbtnRootChild);
                        GridBagConstraints gbc_rdbtnRootChild = new GridBagConstraints();
                        gbc_rdbtnRootChild.anchor = GridBagConstraints.WEST;
                        gbc_rdbtnRootChild.insets = new Insets(0, 0, 5, 5);
                        gbc_rdbtnRootChild.gridx = 2;
                        gbc_rdbtnRootChild.gridy = 2;
                        panelCircular.add(rdbtnRootChild, gbc_rdbtnRootChild);
                    }
                    {
                        JLabel lblNewLabel_7 = new JLabel(
                                Translations.getString("PanelArrayBuilderDialog.Circular.Label.AngularSteps")); //$NON-NLS-1$
                        GridBagConstraints gbc_lblNewLabel_7 = new GridBagConstraints();
                        gbc_lblNewLabel_7.anchor = GridBagConstraints.SOUTHEAST;
                        gbc_lblNewLabel_7.insets = new Insets(0, 0, 5, 5);
                        gbc_lblNewLabel_7.gridx = 0;
                        gbc_lblNewLabel_7.gridy = 4;
                        panelCircular.add(lblNewLabel_7, gbc_lblNewLabel_7);
                    }
                    {
                        JSpinner spinnerAngularSteps = new JSpinner();
                        spinnerAngularSteps.setToolTipText(
                                Translations.getString("PanelArrayBuilderDialog.Circular.SpinnerAngularSteps.ToolTip")); //$NON-NLS-1$
                        spinnerAngularSteps.setModel(new SpinnerNumberModel(1, 1, null, 1));
                        GridBagConstraints gbc_spinnerAngularSteps = new GridBagConstraints();
                        gbc_spinnerAngularSteps.fill = GridBagConstraints.HORIZONTAL;
                        gbc_spinnerAngularSteps.insets = new Insets(0, 0, 5, 5);
                        gbc_spinnerAngularSteps.gridx = 1;
                        gbc_spinnerAngularSteps.gridy = 4;
                        panelCircular.add(spinnerAngularSteps, gbc_spinnerAngularSteps);
                        BeanUtils.bind(UpdateStrategy.READ_WRITE, this, "angularCount", //$NON-NLS-1$
                                spinnerAngularSteps, "value"); //$NON-NLS-1$
                    }
                    {
                        JCheckBox chckbxAngleStepsIncrease = new JCheckBox(
                                Translations.getString("PanelArrayBuilderDialog.Circular.CheckBox.IncreaseProportionallyWithRadius")); //$NON-NLS-1$
                        chckbxAngleStepsIncrease.setToolTipText(
                                Translations.getString("PanelArrayBuilderDialog.Circular.CheckBox.IncreaseProportionallyWithRadius.ToolTip")); //$NON-NLS-1$
                        GridBagConstraints gbc_chckbxAngleStepsIncrease = new GridBagConstraints();
                        gbc_chckbxAngleStepsIncrease.anchor = GridBagConstraints.WEST;
                        gbc_chckbxAngleStepsIncrease.gridwidth = 2;
                        gbc_chckbxAngleStepsIncrease.insets = new Insets(0, 0, 5, 5);
                        gbc_chckbxAngleStepsIncrease.gridx = 2;
                        gbc_chckbxAngleStepsIncrease.gridy = 4;
                        panelCircular.add(chckbxAngleStepsIncrease, gbc_chckbxAngleStepsIncrease);
                        BeanUtils.bind(UpdateStrategy.READ_WRITE, this, "increaseProportionally", //$NON-NLS-1$
                                chckbxAngleStepsIncrease, "selected"); //$NON-NLS-1$
                    }
                    {
                        JLabel lblNewLabel_9 = new JLabel(
                                Translations.getString("PanelArrayBuilderDialog.Circular.Label.RadialSteps")); //$NON-NLS-1$
                        GridBagConstraints gbc_lblNewLabel_9 = new GridBagConstraints();
                        gbc_lblNewLabel_9.anchor = GridBagConstraints.EAST;
                        gbc_lblNewLabel_9.insets = new Insets(0, 0, 0, 5);
                        gbc_lblNewLabel_9.gridx = 0;
                        gbc_lblNewLabel_9.gridy = 5;
                        panelCircular.add(lblNewLabel_9, gbc_lblNewLabel_9);
                    }
                    {
                        JSpinner spinnerRadialSteps = new JSpinner();
                        spinnerRadialSteps.setToolTipText(
                                Translations.getString("PanelArrayBuilderDialog.Circular.spinnerRadialSteps.ToolTip")); //$NON-NLS-1$
                        spinnerRadialSteps.setModel(new SpinnerNumberModel(1, 1, null, 1));
                        GridBagConstraints gbc_spinnerRadialSteps = new GridBagConstraints();
                        gbc_spinnerRadialSteps.fill = GridBagConstraints.HORIZONTAL;
                        gbc_spinnerRadialSteps.insets = new Insets(0, 0, 0, 5);
                        gbc_spinnerRadialSteps.gridx = 1;
                        gbc_spinnerRadialSteps.gridy = 5;
                        panelCircular.add(spinnerRadialSteps, gbc_spinnerRadialSteps);
                        BeanUtils.bind(UpdateStrategy.READ_WRITE, this, "radialCount", //$NON-NLS-1$
                                spinnerRadialSteps, "value"); //$NON-NLS-1$
                    }
                }
            }
            {
                panelLayout = new PlacementsHolderLocationViewer(panelLocation, false);
                panelLayout.setArrayRoot(rootChildLocation);
                panelLayout.setShowLocations(false);
                panelLayout.setShowReticle(false);
                panel.add(panelLayout, BorderLayout.CENTER);
            }
        }
        {
            JPanel buttonPane = new JPanel();
            buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
            getContentPane().add(buttonPane, BorderLayout.SOUTH);
            {
                JButton okButton = new JButton(
                        Translations.getString("General.Ok")); //$NON-NLS-1$
                okButton.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (newChildren.isEmpty()) {
                            restoreRootChildLocation();
                        }
                        PanelArrayBuilderDialog.this.removeWindowListener(windowCloseListener);
                        PanelArrayBuilderDialog.this.dispatchEvent(new WindowEvent(
                                PanelArrayBuilderDialog.this, WindowEvent.WINDOW_CLOSING));
                    }});
                buttonPane.add(okButton);
                getRootPane().setDefaultButton(okButton);
            }
            {
                JButton cancelButton = new JButton(
                        Translations.getString("General.Cancel")); //$NON-NLS-1$
                cancelButton.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        cancel();
                        PanelArrayBuilderDialog.this.dispatchEvent(new WindowEvent(
                                PanelArrayBuilderDialog.this, WindowEvent.WINDOW_CLOSING));
                    }});
                buttonPane.add(cancelButton);
            }
        }
    }

    protected void cancel() {
        for (PlacementsHolderLocation<?> newChild : newChildren) {
            panelLocation.removeChild(newChild);
        }
        restoreRootChildLocation();
    }
    
    protected void restoreRootChildLocation() {
        if (!rootChildLocation.getId().equals(rootChildId)) {
            rootChildLocation.setId(rootChildId);
        }
        if (panelLocation.isDirty() != dirtyState) {
            panelLocation.setDirty(dirtyState);
        }
        refresh.run();
    }

    /**
     * @return the columnCount
     */
    public int getColumnCount() {
        return columnCount;
    }

    /**
     * @param columnCount the columnCount to set
     */
    public void setColumnCount(int columnCount) {
        this.columnCount = columnCount;
        generateArray();
    }

    /**
     * @return the rowCount
     */
    public int getRowCount() {
        return rowCount;
    }

    /**
     * @param rowCount the rowCount to set
     */
    public void setRowCount(int rowCount) {
        this.rowCount = rowCount;
        generateArray();
    }

    /**
     * @return the angularCount
     */
    public int getAngularCount() {
        return angularCount;
    }

    /**
     * @param angularCount the angularCount to set
     */
    public void setAngularCount(int angularCount) {
        this.angularCount = angularCount;
        generateArray();
    }

    /**
     * @return the radialCount
     */
    public int getRadialCount() {
        return radialCount;
    }

    /**
     * @param radialCount the radialCount to set
     */
    public void setRadialCount(int radialCount) {
        this.radialCount = radialCount;
        generateArray();
    }

    /**
     * @return the columnSpacing
     */
    public Length getColumnSpacing() {
        return columnSpacing;
    }

    /**
     * @param columnSpacing the columnSpacing to set
     */
    public void setColumnSpacing(Length columnSpacing) {
        this.columnSpacing = columnSpacing;
        generateArray();
    }

    /**
     * @return the rowSpacing
     */
    public Length getRowSpacing() {
        return rowSpacing;
    }

    /**
     * @param rowSpacing the rowSpacing to set
     */
    public void setRowSpacing(Length rowSpacing) {
        this.rowSpacing = rowSpacing;
        generateArray();
    }

    /**
     * @return the alternateOffset
     */
    public Length getAlternateOffset() {
        return alternateOffset;
    }

    /**
     * @param alternateOffset the alternateOffset to set
     */
    public void setAlternateOffset(Length alternateOffset) {
        this.alternateOffset = alternateOffset;
        generateArray();
    }

    /**
     * @return the alternateRowColumnDelta
     */
    public int getAlternateRowColumnDelta() {
        return alternateRowColumnDelta;
    }

    /**
     * @param alternateRowColumnDelta the alternateRowColumnDelta to set
     */
    public void setAlternateRowColumnDelta(int alternateRowColumnDelta) {
        this.alternateRowColumnDelta = alternateRowColumnDelta;
        generateArray();
    }

    /**
     * @return the centerX
     */
    public Length getCenterX() {
        return centerX;
    }

    /**
     * @param centerX the centerX to set
     */
    public void setCenterX(Length centerX) {
        this.centerX = centerX;
        generateArray();
    }

    /**
     * @return the centerY
     */
    public Length getCenterY() {
        return centerY;
    }

    /**
     * @param centerY the centerY to set
     */
    public void setCenterY(Length centerY) {
        this.centerY = centerY;
        generateArray();
    }

    /**
     * @return the increaseProportionally
     */
    public boolean isIncreaseProportionally() {
        return increaseProportionally;
    }

    /**
     * @param increaseProportionally the increaseProportionally to set
     */
    public void setIncreaseProportionally(boolean increaseProportionally) {
        this.increaseProportionally = increaseProportionally;
        generateArray();
    }

    /**
     * @return the panelReferenceFrame
     */
    public boolean isPanelReferenceFrame() {
        return panelReferenceFrame;
    }

    /**
     * @param panelReferenceFrame the panelReferenceFrame to set
     */
    public void setPanelReferenceFrame(boolean panelReferenceFrame) {
        this.panelReferenceFrame = panelReferenceFrame;
        generateArray();
    }

    protected void generateArray() {
        for (PlacementsHolderLocation<?> newChild : newChildren) {
            panelLocation.removeChild(newChild);
        }
        newChildren.clear();
        switch (arrayType) {
            case Rectangular:
                for (int i=0; i<rowCount; i++) {
                    int cCount = columnCount + ((i % 2) == 0 ? 0 : alternateRowColumnDelta);
                    Length rowOffset = alternateOffset.abs().multiply((i % 2) == 0 ? 0 : -alternateRowColumnDelta);
                    for (int j=0; j<cCount; j++) {
                        if (i == 0 && j == 0 ) {
                            if (rowCount > 1 || cCount > 1) {
                                rootChildLocation.setId(String.format("%s[%d,%d]", rootChildId, i+1, j+1)); //$NON-NLS-1$
                            }
                        }
                        else {
                            Location offset = new Location(systemUnit);
                            offset = offset.deriveLengths(columnSpacing.multiply(j).add(rowOffset),
                                    rowSpacing.multiply(i), null, null);
                            PlacementsHolderLocation<?> newChildLocation = null;
                            if (rootChildLocation instanceof BoardLocation) {
                                newChildLocation = new BoardLocation(new Board(((BoardLocation) rootChildLocation).getBoard()));
                                ((BoardLocation) newChildLocation).setDefinition((BoardLocation) newChildLocation);
                            }
                            else if (rootChildLocation instanceof PanelLocation) {
                                newChildLocation = new PanelLocation(new Panel(((PanelLocation) rootChildLocation).getPanel()));
                                ((PanelLocation) newChildLocation).setDefinition((PanelLocation) newChildLocation);
                            }
                            newChildLocation.setCheckFiducials(rootChildLocation.isCheckFiducials());
                            newChildLocation.setId(String.format("%s[%d,%d]", rootChildId, i+1, j+1)); //$NON-NLS-1$
                            newChildLocation.setLocation(rootChildLocation.getLocation().add(offset));
                            newChildren.add(newChildLocation);
                            panelLocation.addChild(newChildLocation);
                        }
                    }
                }
                break;
            case Circular:
                double angleStep;
                int aCount = angularCount;
                Location center = new Location(systemUnit);
                center = center.deriveLengths(centerX, centerY, rootChildLocation.getLocation().getLengthZ(), null);
                if (!panelReferenceFrame) {
                    center = Utils2D.calculateBoardPlacementLocation(rootChildLocation, center);
                }
                Length radiusStep = rootChildLocation.getLocation().getLinearLengthTo(center);
                
                for (int i=0; i<radialCount; i++) {
                    Length radius = radiusStep.add(radiusStep.multiply(i));
                    double initAngle = Utils2D.getAngleFromPoint(center, rootChildLocation.getLocation());
                    angleStep = 360.0 / angularCount;
                    if (increaseProportionally) {
                        angleStep /= (i+1);
                    }
                    
                    for (int j=0; j<aCount; j++) {
                        if (i == 0 && j == 0 && (aCount > 1 || radialCount > 1)) {
                            rootChildLocation.setId(String.format("%s[%d,%d]", rootChildId, i+1, j+1)); //$NON-NLS-1$
                        }
                        else {
                            double angleDeg = initAngle + angleStep*j;
                            double angleRad = Math.toRadians(angleDeg);
                            Location loc = new Location(systemUnit);
                            loc = loc.deriveLengths(radius.multiply(Math.cos(angleRad)), 
                                    radius.multiply(Math.sin(angleRad)), null, null);
                            loc = loc.add(center);
                            loc = loc.derive(null, null, null, rootChildLocation.getLocation().getRotation() + angleStep*j);
                            PlacementsHolderLocation<?> newChildLocation = null;
                            if (rootChildLocation instanceof BoardLocation) {
                                newChildLocation = new BoardLocation(new Board(((BoardLocation) rootChildLocation).getBoard()));
                            }
                            else if (rootChildLocation instanceof PanelLocation) {
                                newChildLocation = new PanelLocation(new Panel(((PanelLocation) rootChildLocation).getPanel()));
                            }
                            newChildLocation.setCheckFiducials(rootChildLocation.isCheckFiducials());
                            newChildLocation.setId(String.format("%s[%d,%d]", rootChildId, i+1, j+1)); //$NON-NLS-1$
                            newChildLocation.setLocation(loc);
                            newChildren.add(newChildLocation);
                            panelLocation.addChild(newChildLocation);
                        }
                    }
                    if (increaseProportionally) {
                        aCount += angularCount;
                    }
                }
                break;
        }
        panelLayout.setNewArrayMembers(newChildren);
        refresh.run();
        panelLayout.regenerate();
    }
        
}
