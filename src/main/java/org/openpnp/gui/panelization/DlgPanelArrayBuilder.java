package org.openpnp.gui.panelization;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JComboBox;
import javax.swing.DefaultComboBoxModel;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.GridBagLayout;
import javax.swing.JLabel;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTextField;
import javax.swing.JCheckBox;
import java.awt.Dimension;

import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.FiducialLocatableLocation;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.PanelLocation;
import org.openpnp.model.Placement;
import org.openpnp.util.BeanUtils;
import org.openpnp.util.Utils2D;
import org.pmw.tinylog.Logger;

@SuppressWarnings("serial")
public class DlgPanelArrayBuilder extends JDialog {

    private enum ArrayType {
        Rectangular, Circular;
    }
    
    private LengthUnit systemUnit = Configuration.get().getSystemUnits();

    private List<FiducialLocatableLocation> newChildren = new ArrayList<>();
    private ArrayType arrayType = ArrayType.Rectangular;
    private final JPanel contentPanel = new JPanel();
    private JTextField textFieldColumns;
    private JTextField textFieldRows;
    private JTextField textFieldRowSpacing;
    private JTextField textFieldColumnSpacing;
    private JTextField textFieldAlternateOffset;
    private JTextField textFieldCenterX;
    private JTextField textFieldCenterY;
    private final ButtonGroup buttonGroup = new ButtonGroup();
    private JPanel panelControls;
    private PanelLocation panelLocation;
    private FiducialLocatableLocation rootChildLocation;
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
    private JPanel panelLayout;

    /**
     * Create the dialog.
     */
    public DlgPanelArrayBuilder(PanelLocation panelLocation, FiducialLocatableLocation rootChildLocation) {
        this.panelLocation = panelLocation;
        this.rootChildLocation = rootChildLocation;
        this.addWindowListener(new WindowAdapter( ) {

            @Override
            public void windowClosing(WindowEvent e) {
                // TODO Auto-generated method stub
                cancel();
            }});
        setModalityType(ModalityType.APPLICATION_MODAL);
        setTitle("Panel Array Generator");
        setBounds(100, 100, 600, 480);
        getContentPane().setLayout(new BorderLayout());
        contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        getContentPane().add(contentPanel, BorderLayout.CENTER);
        contentPanel.setLayout(new BorderLayout(0, 0));
        {
            JComboBox<ArrayType> comboBoxArrayType = new JComboBox<>();
            comboBoxArrayType.setToolTipText("Selects the type of array to create");
            comboBoxArrayType.setModel(new DefaultComboBoxModel<ArrayType>(ArrayType.values()));
            comboBoxArrayType.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    CardLayout cardLayout = (CardLayout)(panelControls.getLayout());
                    arrayType = (ArrayType) comboBoxArrayType.getSelectedItem();
                    switch (arrayType) {
                        case Rectangular:
                            cardLayout.show(panelControls, "Rectangular");
                            break;
                        case Circular:
                            cardLayout.show(panelControls, "Circular");
                            break;
                    }
                }
                
            });
            contentPanel.add(comboBoxArrayType, BorderLayout.NORTH);
        }
        {
            JPanel panel = new JPanel();
            contentPanel.add(panel, BorderLayout.CENTER);
            panel.setLayout(new BorderLayout(0, 0));
            {
                panelControls = new JPanel();
                panel.add(panelControls, BorderLayout.NORTH);
                panelControls.setLayout(new CardLayout(0, 0));
                {
                    JPanel panelRectangular = new JPanel();
                    panelControls.add(panelRectangular, "Rectangular");
                    GridBagLayout gbl_panelRectangular = new GridBagLayout();
                    gbl_panelRectangular.columnWidths = new int[]{0, 80, 80, 0, 0, 0, 0, 0};
                    gbl_panelRectangular.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0};
                    gbl_panelRectangular.columnWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
                    gbl_panelRectangular.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
                    panelRectangular.setLayout(gbl_panelRectangular);
                    {
                        JLabel lblNewLabel_2 = new JLabel("Columns (X)");
                        GridBagConstraints gbc_lblNewLabel_2 = new GridBagConstraints();
                        gbc_lblNewLabel_2.insets = new Insets(0, 0, 5, 5);
                        gbc_lblNewLabel_2.gridx = 1;
                        gbc_lblNewLabel_2.gridy = 1;
                        panelRectangular.add(lblNewLabel_2, gbc_lblNewLabel_2);
                    }
                    {
                        JLabel lblNewLabel_1 = new JLabel("Rows (Y)");
                        GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
                        gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
                        gbc_lblNewLabel_1.gridx = 2;
                        gbc_lblNewLabel_1.gridy = 1;
                        panelRectangular.add(lblNewLabel_1, gbc_lblNewLabel_1);
                    }
                    {
                        JLabel lblNewLabel = new JLabel("Count");
                        GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
                        gbc_lblNewLabel.anchor = GridBagConstraints.EAST;
                        gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
                        gbc_lblNewLabel.gridx = 0;
                        gbc_lblNewLabel.gridy = 2;
                        panelRectangular.add(lblNewLabel, gbc_lblNewLabel);
                    }
                    {
                        textFieldColumns = new JTextField();
                        textFieldColumns.setToolTipText("Number of columns in the array");
                        GridBagConstraints gbc_textFieldColumns = new GridBagConstraints();
                        gbc_textFieldColumns.insets = new Insets(0, 0, 5, 5);
                        gbc_textFieldColumns.fill = GridBagConstraints.HORIZONTAL;
                        gbc_textFieldColumns.gridx = 1;
                        gbc_textFieldColumns.gridy = 2;
                        panelRectangular.add(textFieldColumns, gbc_textFieldColumns);
                        textFieldColumns.setColumns(10);
                        BeanUtils.bind(UpdateStrategy.READ_WRITE, this, "columnCount", textFieldColumns, "text", new IntegerConverter());
                        ComponentDecorators.decorateWithAutoSelect(textFieldColumns);
                    }
                    {
                        textFieldRows = new JTextField();
                        textFieldRows.setToolTipText("Number of rows in the array");
                        GridBagConstraints gbc_textFieldRows = new GridBagConstraints();
                        gbc_textFieldRows.insets = new Insets(0, 0, 5, 5);
                        gbc_textFieldRows.fill = GridBagConstraints.HORIZONTAL;
                        gbc_textFieldRows.gridx = 2;
                        gbc_textFieldRows.gridy = 2;
                        panelRectangular.add(textFieldRows, gbc_textFieldRows);
                        textFieldRows.setColumns(10);
                        BeanUtils.bind(UpdateStrategy.READ_WRITE, this, "rowCount", textFieldRows, "text", new IntegerConverter());
                        ComponentDecorators.decorateWithAutoSelect(textFieldRows);
                    }
                    {
                        JLabel lblNewLabel_3 = new JLabel("Step");
                        GridBagConstraints gbc_lblNewLabel_3 = new GridBagConstraints();
                        gbc_lblNewLabel_3.anchor = GridBagConstraints.EAST;
                        gbc_lblNewLabel_3.insets = new Insets(0, 0, 5, 5);
                        gbc_lblNewLabel_3.gridx = 0;
                        gbc_lblNewLabel_3.gridy = 3;
                        panelRectangular.add(lblNewLabel_3, gbc_lblNewLabel_3);
                    }
                    {
                        textFieldColumnSpacing = new JTextField();
                        textFieldColumnSpacing.setToolTipText("The distance from a point in one column to the same point in the next column");
                        GridBagConstraints gbc_textFieldColumnSpacing = new GridBagConstraints();
                        gbc_textFieldColumnSpacing.insets = new Insets(0, 0, 5, 5);
                        gbc_textFieldColumnSpacing.fill = GridBagConstraints.HORIZONTAL;
                        gbc_textFieldColumnSpacing.gridx = 1;
                        gbc_textFieldColumnSpacing.gridy = 3;
                        panelRectangular.add(textFieldColumnSpacing, gbc_textFieldColumnSpacing);
                        textFieldColumnSpacing.setColumns(10);
                        BeanUtils.bind(UpdateStrategy.READ_WRITE, this, "columnSpacing", textFieldColumnSpacing, "text", new LengthConverter());
                        ComponentDecorators.decorateWithLengthConversion(textFieldColumnSpacing);
                    }
                    {
                        textFieldRowSpacing = new JTextField();
                        textFieldRowSpacing.setToolTipText("The distance from a point in one row to the same point in the next row");
                        GridBagConstraints gbc_textFieldRowSpacing = new GridBagConstraints();
                        gbc_textFieldRowSpacing.insets = new Insets(0, 0, 5, 5);
                        gbc_textFieldRowSpacing.fill = GridBagConstraints.HORIZONTAL;
                        gbc_textFieldRowSpacing.gridx = 2;
                        gbc_textFieldRowSpacing.gridy = 3;
                        panelRectangular.add(textFieldRowSpacing, gbc_textFieldRowSpacing);
                        textFieldRowSpacing.setColumns(10);
                        BeanUtils.bind(UpdateStrategy.READ_WRITE, this, "rowSpacing", textFieldRowSpacing, "text", new LengthConverter());
                        ComponentDecorators.decorateWithLengthConversion(textFieldRowSpacing);
                    }
                    {
                        JLabel lblNewLabel_6 = new JLabel("Alternate rows have");
                        GridBagConstraints gbc_lblNewLabel_6 = new GridBagConstraints();
                        gbc_lblNewLabel_6.anchor = GridBagConstraints.EAST;
                        gbc_lblNewLabel_6.insets = new Insets(0, 0, 5, 5);
                        gbc_lblNewLabel_6.gridx = 0;
                        gbc_lblNewLabel_6.gridy = 5;
                        panelRectangular.add(lblNewLabel_6, gbc_lblNewLabel_6);
                    }
                    {
                        String[] alternateOptions = new String[] {
                                "the same number of columns as the first row", 
                                "one more column than the first row", 
                                "one less column than the first row"};

                        JComboBox<String> comboBoxAlternateRows = new JComboBox();
                        comboBoxAlternateRows.setToolTipText("Selects the number of columns in the even rows of the array");
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
                        JLabel lblNewLabel_5 = new JLabel("Alternate rows have");
                        GridBagConstraints gbc_lblNewLabel_5 = new GridBagConstraints();
                        gbc_lblNewLabel_5.anchor = GridBagConstraints.EAST;
                        gbc_lblNewLabel_5.insets = new Insets(0, 0, 5, 5);
                        gbc_lblNewLabel_5.gridx = 0;
                        gbc_lblNewLabel_5.gridy = 6;
                        panelRectangular.add(lblNewLabel_5, gbc_lblNewLabel_5);
                    }
                    {
                        textFieldAlternateOffset = new JTextField();
                        textFieldAlternateOffset.setToolTipText("The amount to offset the columns of the alternate rows relative to the columns of the first row");
                        GridBagConstraints gbc_textFieldAlternateOffset = new GridBagConstraints();
                        gbc_textFieldAlternateOffset.insets = new Insets(0, 0, 5, 5);
                        gbc_textFieldAlternateOffset.fill = GridBagConstraints.HORIZONTAL;
                        gbc_textFieldAlternateOffset.gridx = 1;
                        gbc_textFieldAlternateOffset.gridy = 6;
                        panelRectangular.add(textFieldAlternateOffset, gbc_textFieldAlternateOffset);
                        textFieldAlternateOffset.setColumns(10);
                        BeanUtils.bind(UpdateStrategy.READ_WRITE, this, "alternateOffset", textFieldAlternateOffset, "text", new LengthConverter());
                        ComponentDecorators.decorateWithLengthConversion(textFieldAlternateOffset);
                    }
                    {
                        JLabel lblNewLabel_8 = new JLabel("column offset relative to the first row");
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
                    panelControls.add(panelCircular, "Circular");
                    GridBagLayout gbl_panelCircular = new GridBagLayout();
                    gbl_panelCircular.columnWidths = new int[]{0, 80, 80, 0, 0, 0};
                    gbl_panelCircular.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0};
                    gbl_panelCircular.columnWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
                    gbl_panelCircular.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
                    panelCircular.setLayout(gbl_panelCircular);
                    {
                        JLabel lblNewLabel_11 = new JLabel("X");
                        GridBagConstraints gbc_lblNewLabel_11 = new GridBagConstraints();
                        gbc_lblNewLabel_11.insets = new Insets(0, 0, 5, 5);
                        gbc_lblNewLabel_11.gridx = 1;
                        gbc_lblNewLabel_11.gridy = 0;
                        panelCircular.add(lblNewLabel_11, gbc_lblNewLabel_11);
                    }
                    {
                        JLabel lblNewLabel_12 = new JLabel("Y");
                        GridBagConstraints gbc_lblNewLabel_12 = new GridBagConstraints();
                        gbc_lblNewLabel_12.insets = new Insets(0, 0, 5, 5);
                        gbc_lblNewLabel_12.gridx = 2;
                        gbc_lblNewLabel_12.gridy = 0;
                        panelCircular.add(lblNewLabel_12, gbc_lblNewLabel_12);
                    }
                    {
                        JLabel lblNewLabel_4 = new JLabel("Array Center");
                        GridBagConstraints gbc_lblNewLabel_4 = new GridBagConstraints();
                        gbc_lblNewLabel_4.anchor = GridBagConstraints.EAST;
                        gbc_lblNewLabel_4.insets = new Insets(0, 0, 5, 5);
                        gbc_lblNewLabel_4.gridx = 0;
                        gbc_lblNewLabel_4.gridy = 1;
                        panelCircular.add(lblNewLabel_4, gbc_lblNewLabel_4);
                    }
                    {
                        textFieldCenterX = new JTextField();
                        textFieldCenterX.setToolTipText("The X coordinate of the array center");
                        GridBagConstraints gbc_textFieldCenterX = new GridBagConstraints();
                        gbc_textFieldCenterX.insets = new Insets(0, 0, 5, 5);
                        gbc_textFieldCenterX.fill = GridBagConstraints.HORIZONTAL;
                        gbc_textFieldCenterX.gridx = 1;
                        gbc_textFieldCenterX.gridy = 1;
                        panelCircular.add(textFieldCenterX, gbc_textFieldCenterX);
                        textFieldCenterX.setColumns(10);
                        BeanUtils.bind(UpdateStrategy.READ_WRITE, this, "centerX", textFieldCenterX, "text", new LengthConverter());
                        ComponentDecorators.decorateWithLengthConversion(textFieldCenterX);
                    }
                    {
                        textFieldCenterY = new JTextField();
                        textFieldCenterY.setToolTipText("The Y coordinate of the array center");
                        GridBagConstraints gbc_textFieldCenterY = new GridBagConstraints();
                        gbc_textFieldCenterY.insets = new Insets(0, 0, 5, 5);
                        gbc_textFieldCenterY.fill = GridBagConstraints.HORIZONTAL;
                        gbc_textFieldCenterY.gridx = 2;
                        gbc_textFieldCenterY.gridy = 1;
                        panelCircular.add(textFieldCenterY, gbc_textFieldCenterY);
                        textFieldCenterY.setColumns(10);
                        BeanUtils.bind(UpdateStrategy.READ_WRITE, this, "centerY", textFieldCenterY, "text", new LengthConverter());
                        ComponentDecorators.decorateWithLengthConversion(textFieldCenterY);
                    }
                    {
                        JLabel lblNewLabel_10 = new JLabel("Relative to");
                        GridBagConstraints gbc_lblNewLabel_10 = new GridBagConstraints();
                        gbc_lblNewLabel_10.anchor = GridBagConstraints.EAST;
                        gbc_lblNewLabel_10.insets = new Insets(0, 0, 5, 5);
                        gbc_lblNewLabel_10.gridx = 0;
                        gbc_lblNewLabel_10.gridy = 2;
                        panelCircular.add(lblNewLabel_10, gbc_lblNewLabel_10);
                    }
                    {
                        JRadioButton rdbtnPanel = new JRadioButton("Panel");
                        rdbtnPanel.setToolTipText("The Array Center coordinates are expressed in the Panel's reference frame");
                        rdbtnPanel.setSelected(true);
                        buttonGroup.add(rdbtnPanel);
                        GridBagConstraints gbc_rdbtnPanel = new GridBagConstraints();
                        gbc_rdbtnPanel.anchor = GridBagConstraints.WEST;
                        gbc_rdbtnPanel.insets = new Insets(0, 0, 5, 5);
                        gbc_rdbtnPanel.gridx = 1;
                        gbc_rdbtnPanel.gridy = 2;
                        panelCircular.add(rdbtnPanel, gbc_rdbtnPanel);
                        BeanUtils.bind(UpdateStrategy.READ_WRITE, this, "panelReferenceFrame", rdbtnPanel, "selected");
                    }
                    {
                        JRadioButton rdbtnRootChild = new JRadioButton("Root Child");
                        rdbtnRootChild.setToolTipText("The Array Center Coordinates are expressed in the root child's reference frame");
                        buttonGroup.add(rdbtnRootChild);
                        GridBagConstraints gbc_rdbtnRootChild = new GridBagConstraints();
                        gbc_rdbtnRootChild.anchor = GridBagConstraints.WEST;
                        gbc_rdbtnRootChild.insets = new Insets(0, 0, 5, 5);
                        gbc_rdbtnRootChild.gridx = 2;
                        gbc_rdbtnRootChild.gridy = 2;
                        panelCircular.add(rdbtnRootChild, gbc_rdbtnRootChild);
                    }
                    {
                        JLabel lblNewLabel_7 = new JLabel("Angular Steps");
                        GridBagConstraints gbc_lblNewLabel_7 = new GridBagConstraints();
                        gbc_lblNewLabel_7.anchor = GridBagConstraints.SOUTHEAST;
                        gbc_lblNewLabel_7.insets = new Insets(0, 0, 5, 5);
                        gbc_lblNewLabel_7.gridx = 0;
                        gbc_lblNewLabel_7.gridy = 4;
                        panelCircular.add(lblNewLabel_7, gbc_lblNewLabel_7);
                    }
                    {
                        JSpinner spinnerAngularSteps = new JSpinner();
                        spinnerAngularSteps.setToolTipText("Number of copies to generate angularly around the Array Center");
                        spinnerAngularSteps.setModel(new SpinnerNumberModel(1, 1, null, 1));
                        GridBagConstraints gbc_spinnerAngularSteps = new GridBagConstraints();
                        gbc_spinnerAngularSteps.fill = GridBagConstraints.HORIZONTAL;
                        gbc_spinnerAngularSteps.insets = new Insets(0, 0, 5, 5);
                        gbc_spinnerAngularSteps.gridx = 1;
                        gbc_spinnerAngularSteps.gridy = 4;
                        panelCircular.add(spinnerAngularSteps, gbc_spinnerAngularSteps);
                        BeanUtils.bind(UpdateStrategy.READ_WRITE, this, "angularCount", spinnerAngularSteps, "value");
                    }
                    {
                        JCheckBox chckbxAngleStepsIncrease = new JCheckBox("Increase proportionally with radius");
                        chckbxAngleStepsIncrease.setToolTipText("When selected, the number of angular steps at each radial step is increased proportionally to its radius");
                        GridBagConstraints gbc_chckbxAngleStepsIncrease = new GridBagConstraints();
                        gbc_chckbxAngleStepsIncrease.anchor = GridBagConstraints.WEST;
                        gbc_chckbxAngleStepsIncrease.gridwidth = 2;
                        gbc_chckbxAngleStepsIncrease.insets = new Insets(0, 0, 5, 5);
                        gbc_chckbxAngleStepsIncrease.gridx = 2;
                        gbc_chckbxAngleStepsIncrease.gridy = 4;
                        panelCircular.add(chckbxAngleStepsIncrease, gbc_chckbxAngleStepsIncrease);
                        BeanUtils.bind(UpdateStrategy.READ_WRITE, this, "increaseProportionally", chckbxAngleStepsIncrease, "selected");
                    }
                    {
                        JLabel lblNewLabel_9 = new JLabel("Radial Steps");
                        GridBagConstraints gbc_lblNewLabel_9 = new GridBagConstraints();
                        gbc_lblNewLabel_9.anchor = GridBagConstraints.EAST;
                        gbc_lblNewLabel_9.insets = new Insets(0, 0, 0, 5);
                        gbc_lblNewLabel_9.gridx = 0;
                        gbc_lblNewLabel_9.gridy = 5;
                        panelCircular.add(lblNewLabel_9, gbc_lblNewLabel_9);
                    }
                    {
                        JSpinner spinnerRadialSteps = new JSpinner();
                        spinnerRadialSteps.setToolTipText("Number of copies to generate radially outward from the Array Center");
                        spinnerRadialSteps.setModel(new SpinnerNumberModel(1, 1, null, 1));
                        GridBagConstraints gbc_spinnerRadialSteps = new GridBagConstraints();
                        gbc_spinnerRadialSteps.fill = GridBagConstraints.HORIZONTAL;
                        gbc_spinnerRadialSteps.insets = new Insets(0, 0, 0, 5);
                        gbc_spinnerRadialSteps.gridx = 1;
                        gbc_spinnerRadialSteps.gridy = 5;
                        panelCircular.add(spinnerRadialSteps, gbc_spinnerRadialSteps);
                        BeanUtils.bind(UpdateStrategy.READ_WRITE, this, "radialCount", spinnerRadialSteps, "value");
                    }
                }
            }
            {
                panelLayout = new JPanel() {
                    @Override
                    public void paintComponent(Graphics g) {
                        renderPanelImage(this);
                        if (panelImage != null) {
                            Graphics2D g2 = (Graphics2D) g;
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            g2.drawImage(panelImage, 0, 0, this);
                        }
                        else {
                            super.paintComponent(g);
                        }
                    }
                };
                panelLayout.setBorder(new TitledBorder(null, "Array Layout", TitledBorder.LEADING, TitledBorder.TOP, null, null));
                panel.add(panelLayout, BorderLayout.CENTER);
            }
        }
        {
            JPanel buttonPane = new JPanel();
            buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
            getContentPane().add(buttonPane, BorderLayout.SOUTH);
            {
                JButton okButton = new JButton("OK");
                okButton.setActionCommand("OK");
                okButton.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        newChildren.clear();
                        DlgPanelArrayBuilder.this.dispatchEvent(new WindowEvent(
                                DlgPanelArrayBuilder.this, WindowEvent.WINDOW_CLOSING));
                    }});
                buttonPane.add(okButton);
                getRootPane().setDefaultButton(okButton);
            }
            {
                JButton cancelButton = new JButton("Cancel");
                cancelButton.setActionCommand("Cancel");
                cancelButton.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        cancel();
                        DlgPanelArrayBuilder.this.dispatchEvent(new WindowEvent(
                                DlgPanelArrayBuilder.this, WindowEvent.WINDOW_CLOSING));
                    }});
                buttonPane.add(cancelButton);
            }
        }
    }

    protected void cancel() {
        for (FiducialLocatableLocation newChild : newChildren) {
            panelLocation.removeChild(newChild);
        }
        newChildren.clear();
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
        for (FiducialLocatableLocation newChild : newChildren) {
            panelLocation.removeChild(newChild);
        }
        newChildren.clear();
        switch (arrayType) {
            case Rectangular:
                for (int i=0; i<rowCount; i++) {
                    int cCount = columnCount + ((i % 2) == 0 ? 0 : alternateRowColumnDelta);
                    Length rowOffset = alternateOffset.abs().multiply((i % 2) == 0 ? 0 : -alternateRowColumnDelta);
                    for (int j=0; j<cCount; j++) {
                        if (i != 0 || j != 0) {
                            Location offset = new Location(systemUnit);
                            offset = offset.deriveLengths(columnSpacing.multiply(j).add(rowOffset),
                                    rowSpacing.multiply(i), null, null);
                            FiducialLocatableLocation newChildLocation = null;
                            if (rootChildLocation instanceof BoardLocation) {
                                newChildLocation = new BoardLocation((BoardLocation) rootChildLocation);
                            }
                            else if (rootChildLocation instanceof PanelLocation) {
                                newChildLocation = new PanelLocation((PanelLocation) rootChildLocation);
                            }
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
                        if (i != 0 || j != 0) {
                            double angleDeg = initAngle + angleStep*j;
                            double angleRad = Math.toRadians(angleDeg);
                            Location loc = new Location(systemUnit);
                            loc = loc.deriveLengths(radius.multiply(Math.cos(angleRad)), 
                                    radius.multiply(Math.sin(angleRad)), null, null);
                            loc = loc.add(center);
                            loc = loc.derive(null, null, null, rootChildLocation.getLocation().getRotation() + angleStep*j);
                            FiducialLocatableLocation newChildLocation = null;
                            if (rootChildLocation instanceof BoardLocation) {
                                newChildLocation = new BoardLocation((BoardLocation) rootChildLocation);
                            }
                            else if (rootChildLocation instanceof PanelLocation) {
                                newChildLocation = new PanelLocation((PanelLocation) rootChildLocation);
                            }
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
        panelLayout.repaint();
    }
    
    protected void renderPanelImage(JPanel jPanel) {
        int borderPixels = jPanel.getBorder().getBorderInsets(jPanel).top;
        Dimension currentSize = jPanel.getSize();
        Location panelDimensions = panelLocation.getPanel().getDimensions().convertToUnits(systemUnit);
        double scale = Math.min((currentSize.width - 2*borderPixels)/panelDimensions.getX(), 
                (currentSize.height - 2*borderPixels)/panelDimensions.getY());
        
        panelImage = new BufferedImage(currentSize.width, currentSize.height, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D offScr = (Graphics2D) panelImage.getGraphics();
        offScr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color backGround = Color.BLACK;
        Color copperColor = new Color(250, 160, 27);
        Color maskColor = new Color(151, 49, 176);
        Color profileColor = new Color(181, 167, 177);
        Color rootProfileColor = Color.green;
        Color copyProfileColor = Color.red;
        Color boardColor = new Color(189, 196, 199);
        Color silkColor = Color.WHITE;
        
        offScr.setColor(backGround);
        offScr.fillRect(0, 0, currentSize.width, currentSize.height);
        offScr.translate(borderPixels, borderPixels);
        offScr.scale(scale, scale);
        
        AffineTransform at = new AffineTransform();
        at.translate(0, panelDimensions.getY());
        at.scale(1, -1);

        Shape panelOutline = new Rectangle2D.Double(0, 0, panelDimensions.getX(), panelDimensions.getY());
        
        Shape transformedShape = at.createTransformedShape(panelOutline);
        offScr.setColor(maskColor);
        offScr.fill(transformedShape);
        offScr.setColor(profileColor);
        offScr.setStroke(new BasicStroke((float) (2.0/scale), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        offScr.draw(transformedShape);
        
        drawCoordinateFrameMarker(offScr, at, 10, Color.RED, Color.CYAN, true);
        
        for (FiducialLocatableLocation child : panelLocation.getChildren()) {
            Location loc = child.getLocation().convertToUnits(systemUnit);
            Location dims = child.getFiducialLocatable().getDimensions().convertToUnits(systemUnit);
            AffineTransform at2 = new AffineTransform(at);
            at2.translate(loc.getX(), loc.getY());
            at2.rotate(Math.toRadians(loc.getRotation()));
            
            Shape childOutline = new Rectangle2D.Double(0, 0, dims.getX(), dims.getY());
            transformedShape = at2.createTransformedShape(childOutline);
            if (child != rootChildLocation) {
                if (newChildren.contains(child)) {
                    offScr.setColor(copyProfileColor);
                }
                else {
                    offScr.setColor(maskColor);
                    offScr.fill(transformedShape);
                    offScr.setColor(profileColor);
                }
            }
            else {
                offScr.setColor(maskColor);
                offScr.fill(transformedShape);
                offScr.setColor(rootProfileColor);
            }
            offScr.setStroke(new BasicStroke((float) (2.0/scale), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            offScr.draw(transformedShape);
            
            drawCoordinateFrameMarker(offScr, at2, 10, Color.RED, Color.CYAN, true);
        }
        
        for (Placement fid : panelLocation.getPanel().getPlacements()) {
            if (fid.getSide() == panelLocation.getSide()) {
                Location loc = fid.getLocation().convertToUnits(systemUnit);
                AffineTransform at2 = new AffineTransform(at);
                at2.translate(loc.getX(), loc.getY());
                
                Shape fidOutline = new Ellipse2D.Double(-1.0, -1.0, 2.0, 2.0);
                transformedShape = at2.createTransformedShape(fidOutline);
                offScr.setColor(boardColor);
                offScr.fill(transformedShape);
                fidOutline = new Ellipse2D.Double(-0.5, -0.5, 1.0, 1.0);
                transformedShape = at2.createTransformedShape(fidOutline);
                offScr.setColor(copperColor);
                offScr.fill(transformedShape);
            }
        }

        offScr.dispose();
    }

    private void drawLocationMarker(Graphics2D offScr, AffineTransform at2, int size, Color xColor,
            Color yColor) {
    }
    
    private void drawOriginMarker(Graphics2D offScr, AffineTransform at2, int size, Color xColor,
            Color yColor) {
    }
    
    private void drawCoordinateFrameMarker(Graphics2D offScr, AffineTransform at2, int size, Color xColor,
            Color yColor, boolean rightHanded) {
        int sign = rightHanded ? +1 : -1;
        Path2D xArrow = new Path2D.Double();
        xArrow.moveTo(0, 0);
        xArrow.lineTo(sign*size, 0);
        xArrow.lineTo(sign*size*0.9, size*0.1);
        xArrow.moveTo(sign*size, 0);
        xArrow.lineTo(sign*size*0.9, -size*0.1);
        Path2D yArrow = new Path2D.Double();
        yArrow.moveTo(0, 0);
        yArrow.lineTo(0, size);
        yArrow.lineTo(size*0.1, size*0.9);
        yArrow.moveTo(0, size);
        yArrow.lineTo(-size*0.1, size*0.9);
        offScr.setStroke(new BasicStroke((float) (size/20.0), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        offScr.setColor(xColor);
        offScr.draw(at2.createTransformedShape(xArrow));
        offScr.setColor(yColor);
        offScr.draw(at2.createTransformedShape(yArrow));
    }
    
}
