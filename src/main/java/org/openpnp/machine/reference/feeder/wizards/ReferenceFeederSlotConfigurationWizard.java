
package org.openpnp.machine.reference.feeder.wizards;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.ConfigurationListener;
import org.openpnp.gui.components.AutoSelectTextTable;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.support.*;
import org.openpnp.machine.reference.feeder.ReferenceAutoSlottableFeeder;
import org.openpnp.machine.reference.feeder.ReferenceFeederSlot;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Feeder;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class ReferenceFeederSlotConfigurationWizard extends AbstractConfigurationWizard {
    private final ReferenceFeederSlot feederSlot;
    private JPanel panelChanger;
    private JLabel lblX_1;
    private JLabel lblY_1;
    private JLabel lblZ_1;
    private LocationButtonsPanel changerStartLocationButtonsPanel;
    private JLabel lblStartLocation;
    private JTextField textFieldChangerStartX;
    private JTextField textFieldChangerStartY;
    private JTextField textFieldChangerStartZ;
    private JLabel lblMiddleLocation;
    private JTextField textFieldChangerMidX;
    private JTextField textFieldChangerMidY;
    private JTextField textFieldChangerMidZ;
    private JLabel lblEndLocation;
    private JTextField textFieldChangerEndX;
    private JTextField textFieldChangerEndY;
    private JTextField textFieldChangerEndZ;
    private LocationButtonsPanel changerMidLocationButtonsPanel;
    private LocationButtonsPanel changerEndLocationButtonsPanel;
    private JPanel panelPackageCompat;
    private JCheckBox chckbxAllowIncompatiblePackages;
    private JScrollPane scrollPane;
    private JTable table;
    private FeedersTableModel tableModel;

    private Set<org.openpnp.spi.Feeder> compatibleFeeders = new HashSet<>();
    private JPanel panelCalibration;
    private JButton btnEditPipeline;
    private JButton btnCalibrate;
    private JButton btnReset;
    private JLabel lblEnabled;
    private JCheckBox calibrationEnabledCheckbox;

    private JPanel panelLocation;
    private JLabel lblZ;
    private JLabel lblRotation;
    private JTextField textFieldLocationX;
    private JTextField textFieldLocationY;
    private JTextField textFieldLocationZ;
    private JTextField textFieldLocationC;
    private JPanel panelPart;

    private JComboBox comboBoxPart;
    private LocationButtonsPanel locationButtonsPanel;

    final private Configuration configuration;

    // Extra public methods
    public String[] getChildFeeders() {
        // return validStates;

        if(configuration.getMachine()== null) {
            String[] feederNameList = new String[1];
            feederNameList[0] = "None";
            return feederNameList;
        }

        List<Feeder> feeders = configuration.getMachine().getFeeders();


        int iChildFeedCount=0;
        for(int i=0;i<feeders.size();i++)
        {
            String className = feeders.get(i).getClass().getSimpleName();

            if(feeders.get(i).getClass().getSimpleName().compareTo("ReferenceAutoSlottableFeeder")==0)
            {
                ReferenceAutoSlottableFeeder mountableFeeder = (ReferenceAutoSlottableFeeder) feeders.get(i);

                iChildFeedCount++;
            }
        }
        String[] feederNameList = new String[iChildFeedCount+1];
        feederNameList[0]="None";

        int iIndex = 1;
        for(int i=0;i<feeders.size();i++)
        {
            if(feeders.get(i).getClass().getSimpleName().compareTo("ReferenceAutoSlottableFeeder")==0)
            {
                ReferenceAutoSlottableFeeder mountableFeeder = (ReferenceAutoSlottableFeeder) feeders.get(i);
                feederNameList[iIndex] = mountableFeeder.getName();
                iIndex++;
            }
        }

        return feederNameList;
    }

/*
       String[] childFeeders = tableModel.getChildFeeders();

                comboBox.removeAllItems();;
                for(int i=0;i<childFeeders.length;i++)
                {
                    comboBox.addItem(childFeeders[i]);
                }
 */
    // Protected methods
    protected boolean isValidValue(Object value) {
        if (value instanceof String) {
            String sValue = (String)value;

            String[] childFeeders = getChildFeeders();

            for (int i = 0; i < childFeeders.length; i++) {
                if (sValue.equals(childFeeders[i])) {
                    return true;
                }
            }
        }

        return false;
    }

    public ReferenceFeederSlotConfigurationWizard(ReferenceFeederSlot feederSlot) {
        this.feederSlot = feederSlot;
        this.configuration = Configuration.get();

        panelPackageCompat = new JPanel();
        panelPackageCompat.setBorder(new TitledBorder(null, "Feeder Compatibility",
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelPackageCompat);
        panelPackageCompat.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC,
                        ColumnSpec.decode("default:grow"),},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, RowSpec.decode("max(100dlu;min)"),}));

        chckbxAllowIncompatiblePackages = new JCheckBox("Allow Incompatible Feeders?");
        panelPackageCompat.add(chckbxAllowIncompatiblePackages, "2, 2");

        scrollPane = new JScrollPane();
        panelPackageCompat.add(scrollPane, "2, 4, fill, default");

        table = new AutoSelectTextTable(tableModel = new FeedersTableModel());
        scrollPane.setViewportView(table);


        panelPart = new JPanel();
        panelPart.setBorder(
                new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "General Settings", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
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
                        FormSpecs.DEFAULT_ROWSPEC,}));

        comboBoxPart = new JComboBox();
        try {
            comboBoxPart.setModel(new FeedersComboBoxModel());
        }
        catch (Throwable t) {
            // Swallow this error. This happens during parsing in
            // in WindowBuilder but doesn't happen during normal run.
        }

        JLabel lblPart = new JLabel("Feeder");
        panelPart.add(lblPart, "2, 2, right, default");
        comboBoxPart.setRenderer(new IdentifiableListCellRenderer<Feeder>());
        panelPart.add(comboBoxPart, "4, 2, left, default");


        if (true) {
            panelLocation = new JPanel();
            panelLocation.setBorder(new TitledBorder(
                    new EtchedBorder(EtchedBorder.LOWERED, null, null), "Pick Location",
                    TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
            contentPanel.add(panelLocation);
            panelLocation
                    .setLayout(new FormLayout(
                            new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC,
                                    ColumnSpec.decode("default:grow"),
                                    FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec
                                    .decode("default:grow"),
                                    FormSpecs.RELATED_GAP_COLSPEC,
                                    ColumnSpec.decode("default:grow"),
                                    FormSpecs.RELATED_GAP_COLSPEC,
                                    ColumnSpec.decode("default:grow"),
                                    FormSpecs.RELATED_GAP_COLSPEC,
                                    ColumnSpec.decode("left:default:grow"),},
                            new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                                    FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

            lblX_1 = new JLabel("X");
            panelLocation.add(lblX_1, "2, 2");

            lblY_1 = new JLabel("Y");
            panelLocation.add(lblY_1, "4, 2");

            lblZ = new JLabel("Z");
            panelLocation.add(lblZ, "6, 2");

            lblRotation = new JLabel("Rotation");
            panelLocation.add(lblRotation, "8, 2");

            textFieldLocationX = new JTextField();
            panelLocation.add(textFieldLocationX, "2, 4");
            textFieldLocationX.setColumns(8);

            textFieldLocationY = new JTextField();
            panelLocation.add(textFieldLocationY, "4, 4");
            textFieldLocationY.setColumns(8);

            textFieldLocationZ = new JTextField();
            panelLocation.add(textFieldLocationZ, "6, 4");
            textFieldLocationZ.setColumns(8);

            textFieldLocationC = new JTextField();
            panelLocation.add(textFieldLocationC, "8, 4");
            textFieldLocationC.setColumns(8);

            locationButtonsPanel = new LocationButtonsPanel(textFieldLocationX, textFieldLocationY,
                    textFieldLocationZ, textFieldLocationC);
            panelLocation.add(locationButtonsPanel, "10, 4");
        }

       Configuration.get().addListener(new ConfigurationListener.Adapter() {
            public void configurationComplete(Configuration configuration) throws Exception {
                // refresh();
            }
        });
    }


    @Override
    public void createBindings() {
        LengthConverter lengthConverter = new LengthConverter();
        IntegerConverter intConverter = new IntegerConverter();
        DoubleConverter doubleConverter =
                new DoubleConverter(Configuration.get().getLengthDisplayFormat());

        addWrappedBinding(feederSlot, "child", comboBoxPart, "selectedItem");

        if (true) {
            MutableLocationProxy location = new MutableLocationProxy();
            bind(UpdateStrategy.READ_WRITE, feederSlot, "location", location, "location");
            addWrappedBinding(location, "lengthX", textFieldLocationX, "text", lengthConverter);
            addWrappedBinding(location, "lengthY", textFieldLocationY, "text", lengthConverter);
            addWrappedBinding(location, "lengthZ", textFieldLocationZ, "text", lengthConverter);
            addWrappedBinding(location, "rotation", textFieldLocationC, "text", doubleConverter);
            ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldLocationX);
            ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldLocationY);
            ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldLocationZ);
            ComponentDecorators.decorateWithAutoSelect(textFieldLocationC);
        }

    }

    @Override
    protected void loadFromModel() {
        compatibleFeeders.clear();
      //  compatiblePackages.addAll(nozzleTip.getCompatiblePackages());
        tableModel.refresh();
        super.loadFromModel();
    }

    @Override
    protected void saveToModel() {
    //    nozzleTip.setCompatiblePackages(compatiblePackages);
        super.saveToModel();
    }

    public class FeedersTableModel extends AbstractTableModel {
        private String[] columnNames = new String[] {"Feeder Id", "Compatible?"};
        private List<org.openpnp.spi.Feeder> feeders;

        public FeedersTableModel() {
            Configuration.get().addListener(new ConfigurationListener.Adapter() {
                public void configurationComplete(Configuration configuration) throws Exception {
                    refresh();
                }
            });
        }

        public void refresh() {
            feeders = new ArrayList<>(Configuration.get().getMachine().getFeeders());
            fireTableDataChanged();
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            return (feeders == null) ? 0 : feeders.size();
        }

        public org.openpnp.spi.Feeder getFeeder(int index) {
            return feeders.get(index);
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 1;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            try {
                org.openpnp.spi.Feeder pkg = feeders.get(rowIndex);
                if (columnIndex == 1) {
                    if ((Boolean) aValue) {
                        compatibleFeeders.add(pkg);
                    }
                    else {
                        compatibleFeeders.remove(pkg);
                    }
                    notifyChange();
                }
            }
            catch (Exception e) {
                // TODO: dialog, bad input
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 1) {
                return Boolean.class;
            }
            return super.getColumnClass(columnIndex);
        }

        public Object getValueAt(int row, int col) {
            switch (col) {
                case 0:
                    return feeders.get(row).getId();
                case 1:
                    return compatibleFeeders.contains(feeders.get(row));
                default:
                    return null;
            }
        }
    }
}
