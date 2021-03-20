package org.openpnp.machine.reference.wizards;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import org.apache.commons.collections.map.LinkedMap;
import org.jdesktop.beansbinding.Converter;
import org.openpnp.gui.support.ActuatorsComboBoxModel;
import org.openpnp.gui.support.Converters;
import org.openpnp.machine.reference.ReferenceActuatorMapper;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.base.AbstractMachine;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ReferenceActuatorMapperConfigurationWizard extends ReferenceActuatorConfigurationWizard {

    private JPanel mappingPanel;

    private JButton addBtn;
    private JTabbedPane tabbedPane;
    private Map<Object, MappingComponent> map = new HashMap<>();

    public ReferenceActuatorMapperConfigurationWizard(AbstractMachine machine, ReferenceActuatorMapper actuator) {
        super(machine, actuator);
    }

    @Override
    protected void createUi(AbstractMachine machine) {
        super.createUi(machine);

        mappingPanel = new JPanel();
        mappingPanel.setBorder(new TitledBorder(null, "Mapping", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(mappingPanel);

        mappingPanel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow")},
                new RowSpec[] {
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        RowSpec.decode("default:grow")}));

        addBtn = new JButton("Add Mapping");
        mappingPanel.add(addBtn, "2, 2");
        addBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Object value = null;
                Object[] values = actuator.getValues();
                while(true) {
                    value = JOptionPane.showInputDialog(getTopLevelAncestor(), "Please enter the value to map", "ReferenceActuatorMapper", JOptionPane.QUESTION_MESSAGE, null, values, value);
                    if (value == null) {
                        break;
                    }
                    if (value instanceof String) {
                        value = Converters.getConverter(actuator.getValueClass()).convertReverse((String) value);
                    }
                    if (value == null) {
                        JOptionPane.showMessageDialog(getTopLevelAncestor(), "Invalid value", "ReferenceActuatorMapper", JOptionPane.ERROR_MESSAGE); //$NON-NLS-1
                        continue;
                    }
                    if (map.containsKey(value)) {
                        JOptionPane.showMessageDialog(getTopLevelAncestor(), "Value already mapped", "ReferenceActuatorMapper", JOptionPane.ERROR_MESSAGE); //$NON-NLS-1
                        continue;
                    }
                    addMapping(value, Collections.emptyMap());
                    break;
                }
            }
        });

        tabbedPane = new JTabbedPane();
        mappingPanel.add(tabbedPane, "2, 4, 3, 1");
    }

    @Override
    protected void loadFromModel() {
        map.clear();
        for (Map.Entry<Object, ReferenceActuatorMapper.Mapping> entry : getActuator().getMap().entrySet()) {
            ReferenceActuatorMapper.Mapping mapping = entry.getValue();
            addMapping(entry.getKey(), mapping.getActuators());
        }

        super.loadFromModel();
    }

    @Override
    protected void saveToModel() {
        super.saveToModel();

        Map<Object, ReferenceActuatorMapper.Mapping> map = getActuator().getMap();
        map.clear();
        for (Map.Entry<Object, MappingComponent> entry : this.map.entrySet()) {
            ReferenceActuatorMapper.Mapping mapping = new ReferenceActuatorMapper.Mapping();
            mapping.getActuators().putAll(entry.getValue().getData());

            map.put(entry.getKey(), mapping);
        }
    }

    private ReferenceActuatorMapper getActuator() {
        return (ReferenceActuatorMapper) actuator;
    }



    private void addMapping(Object key, Map<String, Object> values) {
        String title = Converters.getConverter(actuator.getValueClass()).convertForward(key);
        MappingComponent child = new MappingComponent(key, values);
        map.put(key, child);
        tabbedPane.addTab(title, null, child, "Mapping for " + title);

        int index = tabbedPane.indexOfTab(title);
        JPanel pnlTab = new JPanel(new GridBagLayout());
        pnlTab.setOpaque(false);
        JLabel lblTitle = new JLabel(title);
        JButton btnClose = new JButton(UIManager.getIcon("InternalFrame.closeIcon"));
        btnClose.setBorderPainted(false);
        btnClose.setBorder(null);
        btnClose.setMargin(new Insets(0, 0, 0, 0));
        btnClose.setContentAreaFilled(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.ipadx = 5;
        gbc.weightx = 1;

        pnlTab.add(lblTitle, gbc);

        gbc.gridx++;
        gbc.weightx = 0;
        pnlTab.add(btnClose, gbc);

        btnClose.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                removeMapping(key);
            }
        });

        tabbedPane.setTabComponentAt(index, pnlTab);
        notifyChange();
    }

    private void removeMapping(Object key) {
        tabbedPane.remove(map.get(key));
        map.remove(key);
        notifyChange();
    }

    private class MappingComponent extends JComponent {
        private final MappingTable table;


        public MappingComponent(Object key, Map<String, Object> values) {
            setLayout(new BorderLayout());
            table = new MappingTable(new MappingTableModel());
            table.getModel().getData().putAll(values);
            JScrollPane scrollPane = new JScrollPane(table);
            table.setFillsViewportHeight(true);
            add(scrollPane, BorderLayout.CENTER);

            JPanel buttons = new JPanel();

            JButton addBtn = new JButton("Add Actuator");
            addBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    ActuatorsComboBoxModel actuatorsComboBoxModel = new ActuatorsComboBoxModel(Configuration.get().getMachine(), null);
                    Object[] values = new Object[actuatorsComboBoxModel.getSize() - 1];
                    for (int i = 0; i < actuatorsComboBoxModel.getSize() - 1; ++i) {
                        String actuatorName = (String) actuatorsComboBoxModel.getElementAt(i + 1);
                        values[i] = actuatorName;
                    }
                    Object value = null;
                    while(true) {
                        value = JOptionPane.showInputDialog(getTopLevelAncestor(), "Please select a actuator", "ReferenceActuatorMapper", JOptionPane.QUESTION_MESSAGE, null, values, value);
                        if (value == null) {
                            break;
                        }
                        if (table.getModel().getData().containsKey(value)) {
                            JOptionPane.showMessageDialog(getTopLevelAncestor(), "Actuator already mapped", "ReferenceActuatorMapper", JOptionPane.ERROR_MESSAGE); //$NON-NLS-1
                            continue;
                        }
                        addActuator(value);
                        break;
                    }
                }
            });
            buttons.add(addBtn);

            JButton removeBtn = new JButton("Remove Actuator");
            removeBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int selectedRow = table.getSelectedRow();
                    if (selectedRow >= 0) {
                        removeActuator(table.getModel().getData().get(selectedRow));
                    }
                }
            });
            buttons.add(removeBtn);

            add(buttons, BorderLayout.SOUTH);

            table.addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if ("tableCellEditor".equals(evt.getPropertyName())) {
                        if (!table.isEditing()) {
                            notifyChange();
                        }
                    }
                }
            });
        }

        private void addActuator(Object value) {
            LinkedMap data = table.getModel().getData();
            data.put(value, null);
            int index = data.size() - 1;
            table.getModel().fireTableRowsInserted(index, index);
            notifyChange();
        }

        private void removeActuator(Object o) {
            table.getCellEditor().cancelCellEditing();
            LinkedMap data = table.getModel().getData();
            int index = data.indexOf(o);
            data.remove(index);
            table.getModel().fireTableRowsDeleted(index, index);
            notifyChange();
        }

        public LinkedMap getData() {
            return table.getModel().getData();
        }
    }

    private static class MappingTable extends JTable {
        private final DefaultTableCellRenderer normalTableCellRenderer;
        private final DefaultTableCellRenderer faultyTableCellRenderer;
        private final DefaultTableCellRenderer invalidTableCellRenderer;

        public MappingTable(MappingTableModel mappingTableModel) {
            super(mappingTableModel);
            normalTableCellRenderer = new DefaultTableCellRenderer();
            faultyTableCellRenderer = new DefaultTableCellRenderer();
            faultyTableCellRenderer.setBackground(Color.YELLOW);
            invalidTableCellRenderer = new DefaultTableCellRenderer();
            invalidTableCellRenderer.setBackground(Color.RED);
        }

        @Override
        public TableCellEditor getCellEditor(int row, int column) {
            if (column == 1) {
                Actuator actuator = getActuator(getModel().getData().get(row));
                if (actuator != null) {
                    Object[] values = actuator.getValues();
                    if (values != null) {
                        return new DefaultCellEditor(new JComboBox(values));
                    } else {
                        return new ActuatorValueClassCellEditor(actuator);
                    }
                }
            }
            return super.getCellEditor(row, column);
        }

        @Override
        public TableCellRenderer getCellRenderer(int row, int column) {
            DefaultTableCellRenderer cellRenderer = normalTableCellRenderer;
            Actuator actuator = getActuator(getModel().getData().get(row));
            Object value = getModel().getData().getValue(row);
            if (actuator != null) {
                if (value instanceof String) {
                    try {
                        Converters.getConverter(actuator.getValueClass()).convertReverse((String) value);
                    } catch (Throwable e) {
                        cellRenderer = faultyTableCellRenderer;
                    }
                } else if (value == null) {
                    cellRenderer = faultyTableCellRenderer;
                }
            } else {
                cellRenderer = invalidTableCellRenderer;
            }
            return cellRenderer;
        }

        public MappingTableModel getModel() {
            return (MappingTableModel) super.getModel();
        }

        private Actuator getActuator(Object name) {
            try {
                return ReferenceActuatorMapper.Mapping.getActuatorByName((String) name);
            } catch (Throwable e) {
                return null;
            }
        }
    }

    private static class ActuatorValueClassCellEditor extends DefaultCellEditor {

        private final Actuator actuator;
        private final Converter<Object, String> converter;

        public ActuatorValueClassCellEditor(Actuator actuator) {
            super(new JTextField());
            this.actuator = actuator;
            converter = Converters.getConverter(actuator.getValueClass());
        }

        @Override
        public Object getCellEditorValue() {
            Object value = super.getCellEditorValue();
            if (!String.class.equals(actuator.getValueClass())) {
                try {
                    value = converter.convertReverse((String) value);
                } catch (Throwable e) {
                    value = MappingTableModel.WRONG_VALUE;
                }
            }
            return value;
        }
    }

    private static class MappingTableModel extends AbstractTableModel {
        public static final Object WRONG_VALUE = new Object();
        private String[] columnNames = new String[] { "Actuator", "Value"};
        private Class<?>[] columnClasses = new Class[] { Actuator.class, Object.class};
        private LinkedMap data = new LinkedMap();

        public MappingTableModel() {
        }

        public LinkedMap getData() {
            return data;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnClasses[columnIndex];
        }

        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            return data.size();
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 1;
        }

        public Object getValueAt(int row, int col) {
            switch (col) {
                case 0:
                    return data.get(row);
                case 1:
                    return data.getValue(row);
                default:
                    return null;
            }
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex == 1) {
                if (aValue != WRONG_VALUE) {
                    data.put(data.get(rowIndex), aValue);
                }
            }
        }
    }
}
