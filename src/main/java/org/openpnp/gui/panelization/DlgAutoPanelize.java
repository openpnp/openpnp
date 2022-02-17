package org.openpnp.gui.panelization;

import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.TableRowSorter;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.JobPanel;
import org.openpnp.gui.MultisortTableHeaderCellRenderer;
import org.openpnp.gui.components.AutoSelectTextTable;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.CustomBooleanRenderer;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.IdentifiableListCellRenderer;
import org.openpnp.gui.support.IdentifiableTableCellRenderer;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.gui.support.PartsComboBoxModel;
import org.openpnp.gui.tablemodel.PanelFiducialsTableModel;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Panel;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.openpnp.model.Board.Side;
import org.openpnp.util.BeanUtils;
import org.openpnp.util.UiUtils;
import org.pmw.tinylog.Logger;

import javax.swing.JScrollPane;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import javax.swing.SwingConstants;
import java.awt.event.ActionListener;

@SuppressWarnings("serial")
public class DlgAutoPanelize extends JDialog {
    final private JobPanel jobPanel;
    
    private JTable table;
    private PanelFiducialsTableModel tableModel;
    private TableRowSorter<PanelFiducialsTableModel> tableSorter;
    
    private JSpinner textFieldPcbColumns;
    private JSpinner textFieldPcbRows;
    private JTextField textFieldBoardXGap;
    private JTextField textFieldBoardYGap;
    private JComboBox<Part> partsComboBox;
    private JCheckBox checkFidsCheckBox;
    
    DoubleConverter doubleConverter = new DoubleConverter(Configuration.get().getLengthDisplayFormat());
    IntegerConverter integerConverter = new IntegerConverter();
    LengthConverter lengthConverter = new LengthConverter();
    private JScrollPane scrollPane;

    private Panel pcbPanel;

    private JLabel lblyDirection;
    private JPanel fiducialsJPanel;

    public DlgAutoPanelize(Frame parent, JobPanel jobPanel) {
        super(parent, "Panelization Settings", true);
        this.jobPanel = jobPanel;
        
        // Specify a placeholder panel for now if we don't have one already
        if (jobPanel.getJob().getPanels() == null || jobPanel.getJob().getPanels().isEmpty()) {
            jobPanel.getJob().addPanel(
                    new Panel("Panel1", 3, 3, new Length(0, LengthUnit.Millimeters),
                            new Length(0, LengthUnit.Millimeters),
                            jobPanel.getJob().getBoardLocations().get(0).getBoard().getDimensions()));
        }

        //Make a copy of the current panel
        pcbPanel = new Panel(jobPanel.getJob().getPanels().get(0));
        
        
//        if (pcbPanel.getRootPcbOffset() == null) {
//            Logger.trace("Updating an old panelization to new format");
//            BoardLocation rootPcb = jobPanel.getJob().getBoardLocations().get(0);
//            Location rootDims = rootPcb.getBoard().getDimensions().convertToUnits(LengthUnit.Millimeters);
//            pcbPanel.setOriginalRootPcbLocation(rootPcb);
//            pcbPanel.setRootPcbOffset(new Location(LengthUnit.Millimeters));
//            Location dimensions = new Location(LengthUnit.Millimeters);
//            pcbPanel.setDimensions(dimensions);
//        }
                
        getRootPane().setLayout(new BoxLayout(getRootPane(), BoxLayout.Y_AXIS));

        JPanel jPanel = new JPanel();
        jPanel.setBorder(new TitledBorder(null, "Panelize Parameters ", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        getRootPane().add(jPanel);

        jPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        jPanel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(87dlu;default):grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(87dlu;default):grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(87dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(98dlu;default):grow"),},
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
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
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
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        JLabel lblXDirection = new JLabel("X Direction", SwingConstants.LEADING);
        jPanel.add(lblXDirection, "4, 2, fill, fill");
        lblyDirection = new JLabel("Y Direction");
        jPanel.add(lblyDirection, "6, 2");
        
        JLabel lblNumberOfBoards = new JLabel("Number of Boards", JLabel.RIGHT);
        jPanel.add(lblNumberOfBoards, "2, 4, fill, fill");

        textFieldPcbColumns = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        textFieldPcbColumns.setToolTipText("The number of board copies in the panel's X direction");
        jPanel.add(textFieldPcbColumns, "4, 4, fill, fill");
        
        textFieldPcbRows = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        textFieldPcbRows.setToolTipText("The number of board copies in the panel's Y direction");
        jPanel.add(textFieldPcbRows, "6, 4, fill, fill");
        
        JLabel lblGapBetweenBoards = new JLabel("Gap Between Boards", JLabel.RIGHT);
        jPanel.add(lblGapBetweenBoards, "2, 6, fill, fill");
        
        textFieldBoardXGap = new JTextField();
        textFieldBoardXGap.setToolTipText("Distance from the right edge of one board to the left edge of the next board");
        jPanel.add(textFieldBoardXGap, "4, 6, fill, fill");
                 
        textFieldBoardYGap = new JTextField();
        textFieldBoardXGap.setToolTipText("Distance from the top edge of one board to the bottom edge of the next board");
        jPanel.add(textFieldBoardYGap, "6, 6, fill, fill");
        
        JLabel label_2 = new JLabel("Check Panel Fiducials", JLabel.RIGHT);
        jPanel.add(label_2, "2, 8, fill, fill");
        checkFidsCheckBox = new JCheckBox();
        checkFidsCheckBox.setToolTipText("Enables automatic panel alignment at the start of a job");
        jPanel.add(checkFidsCheckBox, "4, 8, fill, fill");
        
        lblPanelDimension = new JLabel("Panel Dimensions");
        jPanel.add(lblPanelDimension, "2, 10, right, default");
        
        textFieldPanelWidth = new JTextField();
        jPanel.add(textFieldPanelWidth, "4, 10, fill, default");
        textFieldPanelWidth.setColumns(10);
        
        textFieldPanelLength = new JTextField();
        jPanel.add(textFieldPanelLength, "6, 10, fill, default");
        textFieldPanelLength.setColumns(10);
        
        lblRootBoardOffset = new JLabel("Root Board Offset");
        jPanel.add(lblRootBoardOffset, "2, 12, right, default");
        
        textFieldRootBoardOffsetX = new JTextField();
        jPanel.add(textFieldRootBoardOffsetX, "4, 12, fill, default");
        textFieldRootBoardOffsetX.setColumns(10);
        
        textFieldRootBoardOffsetY = new JTextField();
        jPanel.add(textFieldRootBoardOffsetY, "6, 12, fill, default");
        textFieldRootBoardOffsetY.setColumns(10);

        JLabel lblDefaultPanelFiducial = new JLabel("Default Panel Fiducial Part", JLabel.RIGHT);
        jPanel.add(lblDefaultPanelFiducial, "2, 18, fill, fill");
        
        partsComboBox = new JComboBox<>(new PartsComboBoxModel());
        partsComboBox.setToolTipText("Sets the default fiducial to be used when adding new fiducials to the panel");
        partsComboBox.setRenderer(new IdentifiableListCellRenderer<Part>());
        partsComboBox.setEditable(false);
        partsComboBox.setLightWeightPopupEnabled(false);
        partsComboBox.setMaximumRowCount(7);
        jPanel.add(partsComboBox, "4, 18, 7, 1, fill, fill");
        partsComboBox.setSelectedItem(pcbPanel.getFiducialPart());
        partsComboBox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                pcbPanel.setFiducialPart((Part)partsComboBox.getSelectedItem());
            }
            
        });
        
        tableModel = new PanelFiducialsTableModel(pcbPanel);
        tableSorter = new TableRowSorter<>(tableModel);
        
        fiducialsJPanel = new JPanel();
        fiducialsJPanel.setBorder(new TitledBorder(null, "Panel Fiducials", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        jPanel.add(fiducialsJPanel, "2, 20, 9, 9, fill, default");
        GridBagLayout gbl_panel_1 = new GridBagLayout();
        gbl_panel_1.columnWidths = new int[]{703, 0};
        gbl_panel_1.rowHeights = new int[]{25, 49, 0};
        gbl_panel_1.columnWeights = new double[]{0.0, Double.MIN_VALUE};
        gbl_panel_1.rowWeights = new double[]{0.0, 8.0, Double.MIN_VALUE};
        fiducialsJPanel.setLayout(gbl_panel_1);
        
        JComboBox<Side> sideComboBox = new JComboBox(Side.values());
        sideComboBox.setLightWeightPopupEnabled(false);
        
        table = new AutoSelectTextTable(tableModel);
        table.setDefaultEditor(Part.class, new DefaultCellEditor(partsComboBox));
        table.setDefaultEditor(Side.class, new DefaultCellEditor(sideComboBox));
        table.setRowSorter(tableSorter);
        table.getTableHeader().setDefaultRenderer(new MultisortTableHeaderCellRenderer());
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setDefaultRenderer(Part.class, new IdentifiableTableCellRenderer<Part>());
        table.setDefaultRenderer(Boolean.class, new CustomBooleanRenderer());
        table.getColumnModel().getColumn(0).setMaxWidth(60);
        table.getColumnModel().getColumn(1).setMaxWidth(80);
        table.getColumnModel().getColumn(3).setMaxWidth(80);
        table.getColumnModel().getColumn(4).setMaxWidth(80);
        table.getColumnModel().getColumn(5).setMaxWidth(80);
        
        JToolBar toolBarPlacements = new JToolBar();
        GridBagConstraints gbc_toolBarPlacements = new GridBagConstraints();
        gbc_toolBarPlacements.fill = GridBagConstraints.BOTH;
        gbc_toolBarPlacements.insets = new Insets(0, 0, 5, 0);
        gbc_toolBarPlacements.gridx = 0;
        gbc_toolBarPlacements.gridy = 0;
        fiducialsJPanel.add(toolBarPlacements, gbc_toolBarPlacements);
        
        toolBarPlacements.setFloatable(false);
        JButton btnNewFiducial = new JButton(newFiducialAction);
        btnNewFiducial.setToolTipText("Adds a new fiducial to the panel");
        btnNewFiducial.setHideActionText(true);
        toolBarPlacements.add(btnNewFiducial);
        
        JButton btnRemovePlacement = new JButton(removeFiducialAction);
        btnRemovePlacement.setToolTipText("Deletes the selected fiducials from the panel");
        btnRemovePlacement.setHideActionText(true);
        toolBarPlacements.add(btnRemovePlacement);
        
        scrollPane = new JScrollPane(table);
        GridBagConstraints gbc_scrollPane = new GridBagConstraints();
        gbc_scrollPane.fill = GridBagConstraints.BOTH;
        gbc_scrollPane.gridx = 0;
        gbc_scrollPane.gridy = 1;
        fiducialsJPanel.add(scrollPane, gbc_scrollPane);

        JPanel footerPanel = new JPanel();
        FlowLayout flowLayout = (FlowLayout) footerPanel.getLayout();
        flowLayout.setAlignment(FlowLayout.RIGHT);
        getRootPane().add(footerPanel);

        JButton btnCancel = new JButton("Cancel");
        btnCancel.setAction(cancelAction);
        footerPanel.add(btnCancel);

        JButton btnImport = new JButton("OK");
        btnImport.setAction(okAction);
        footerPanel.add(btnImport);

        setSize(800, 600);
        setResizable(false);
        setLocationRelativeTo(parent);

        JRootPane rootPane = getRootPane();
        KeyStroke stroke = KeyStroke.getKeyStroke("ESCAPE");
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(stroke, "ESCAPE");
        rootPane.getActionMap().put("ESCAPE", cancelAction);
        
        MutableLocationProxy dimensions = new MutableLocationProxy();
        BeanUtils.bind(UpdateStrategy.READ_WRITE, pcbPanel, "dimensions", dimensions, "location");
        BeanUtils.bind(UpdateStrategy.READ_WRITE, dimensions, "lengthX", textFieldPanelWidth, "text", lengthConverter);
        BeanUtils.bind(UpdateStrategy.READ_WRITE, dimensions, "lengthY", textFieldPanelLength, "text", lengthConverter);
        
        MutableLocationProxy rootPcbOffset = new MutableLocationProxy();
        BeanUtils.bind(UpdateStrategy.READ_WRITE, pcbPanel, "rootPcbOffset", rootPcbOffset, "location");
        BeanUtils.bind(UpdateStrategy.READ_WRITE, rootPcbOffset, "lengthX", textFieldRootBoardOffsetX, "text", lengthConverter);
        BeanUtils.bind(UpdateStrategy.READ_WRITE, rootPcbOffset, "lengthY", textFieldRootBoardOffsetY, "text", lengthConverter);
        
        BeanUtils.bind(UpdateStrategy.READ_WRITE, pcbPanel, "columns", textFieldPcbColumns, "value");
        BeanUtils.bind(UpdateStrategy.READ_WRITE, pcbPanel, "rows", textFieldPcbRows, "value");
        
        BeanUtils.bind(UpdateStrategy.READ_WRITE, pcbPanel, "XGap", textFieldBoardXGap, "text", lengthConverter);
        BeanUtils.bind(UpdateStrategy.READ_WRITE, pcbPanel, "YGap", textFieldBoardYGap, "text", lengthConverter);
        
        BeanUtils.bind(UpdateStrategy.READ_WRITE, pcbPanel, "checkFiducials", checkFidsCheckBox, "selected" );
        
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldBoardXGap);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldBoardYGap);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldPanelWidth);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldPanelLength);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldRootBoardOffsetX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldRootBoardOffsetY);
    }

    protected final Action okAction = new AbstractAction() {
        {
            putValue(NAME, "OK");
            putValue(SHORT_DESCRIPTION, "OK");
        }

        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                if (pcbPanel.getChildren().size() == 0) {
                    // Here, the user has effectively shut off panelization by
                    // specifying row = col = 1. In this case, we don't
                    // want the panelization info to appear in the job file any
                    // longer. We also need to remove all the boards created
                    // by the panelization previously EXCEPT for the root PCB.
                    // Remember, too, that the condition upon entry into
                    // this dlg was that there was a single board in the list.
                    // When this feature is turned off, there will again
                    // be a single board in the list
                    jobPanel.getJob().removeAllPanels();                  
                    BoardLocation b = jobPanel.getJob().getBoardLocations().get(0);
                    jobPanel.getJob().removeAllBoards();
                    jobPanel.getJob().addBoardLocation(b);
                    jobPanel.refresh();
                    setVisible(false);
                }
                else {
                    // Here, panelization is active.
                    validatePanel();
                    jobPanel.getJob().removeAllPanels();
                    jobPanel.getJob().addPanel(pcbPanel);
                    jobPanel.populatePanelSettingsIntoBoardLocations();
                    setVisible(false);
                }
            });
        }
    };

    protected final Action cancelAction = new AbstractAction() {
        {
            putValue(NAME, "Cancel");
            putValue(SHORT_DESCRIPTION, "Cancel");
        }

        public void actionPerformed(ActionEvent e) {
            setVisible(false);
        }
    };
    
    protected final Action newFiducialAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.add);
            putValue(NAME, "New Fiducial");
            putValue(SHORT_DESCRIPTION, "Create a new fiducial and add it to the panel.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            Placement newFiducial = new Placement(pcbPanel.getPlacements().createId("PanelFid"));
            newFiducial.setPart(pcbPanel.getFiducialPart());
            newFiducial.setType(Placement.Type.Fiducial);
            pcbPanel.getFiducials().add(newFiducial);
            tableModel.fireTableDataChanged();
            Helpers.selectLastTableRow(table);
        }
    };

    protected List<Placement> getTableSelections() {
        ArrayList<Placement> placements = new ArrayList<>();
        int[] selectedRows = table.getSelectedRows();
        for (int selectedRow : selectedRows) {
            selectedRow = table.convertRowIndexToModel(selectedRow);
            placements.add(pcbPanel.getFiducials().get(selectedRow));
        }
        return placements;
    }

    protected final Action removeFiducialAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.delete);
            putValue(NAME, "Remove Fiducial(s)");
            putValue(SHORT_DESCRIPTION, "Remove the currently selected fiducial(s).");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            for (Placement placement : getTableSelections()) {
                pcbPanel.getFiducials().remove(placement);
            }
            tableModel.fireTableDataChanged();
        }
    };
    private JTextField textFieldPanelWidth;
    private JTextField textFieldPanelLength;
    private JLabel lblPanelDimension;
    private JTextField textFieldRootBoardOffsetX;
    private JTextField textFieldRootBoardOffsetY;
    private JLabel lblRootBoardOffset;

    protected void validatePanel() throws Exception {
//        if (pcbPanel.getXGap().getValue() < 0) {
//            throw new Exception("Gap between boards in the X Direction must be greater than or equal to zero.");
//        }
//        if (pcbPanel.getYGap().getValue() < 0) {
//            throw new Exception("Gap between boards in the Y Direction must be greater than or equal to zero.");
//        }
        if (pcbPanel.isCheckFiducials()) {
            int enabledCount = 0;
            Set<Location> fiducialLocations = new HashSet<Location>();
            for (Placement fid : pcbPanel.getFiducials()) {
                if (fid.isEnabled()) {
                    enabledCount++;
                    fiducialLocations.add(fid.getLocation().convertToUnits(LengthUnit.Millimeters));
                }
            }
            if (enabledCount < 2) {
                throw new Exception("Panel fiducial check requires at least two (three or more is better) enabled fiducials.");
            }
            if (fiducialLocations.size() < enabledCount) {
                throw new Exception("All enabled panel fiducials must have unique coordinates.");
            }
        }
    }
}