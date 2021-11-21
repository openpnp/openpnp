package org.openpnp.gui.tablemodel;

import org.openpnp.model.Configuration;
import org.openpnp.model.AbstractVisionSettings;

import javax.swing.table.AbstractTableModel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

public class VisionSettingsModel extends AbstractTableModel implements PropertyChangeListener {

    private String[] columnNames =
            new String[]{"ID", "Name"};
    private Class[] columnTypes = new Class[] {String.class, String.class};

    private List<AbstractVisionSettings> visionSettings;

    public VisionSettingsModel() {
        Configuration.get().addPropertyChangeListener("vision-settings", this);
        visionSettings = new ArrayList<>(Configuration.get().getVisionSettings());
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getSource() instanceof AbstractVisionSettings) {
            fireTableDataChanged();
        } else {
            if (visionSettings != null) {
                for (AbstractVisionSettings visionSettings : this.visionSettings) {
                    visionSettings.removePropertyChangeListener(this);
                }
            }
            visionSettings = new ArrayList<>(Configuration.get().getVisionSettings());
            fireTableDataChanged();
            for (AbstractVisionSettings visionSettings : this.visionSettings) {
                visionSettings.addPropertyChangeListener(this);
            }
        }
    }

    @Override
    public int getRowCount() {
        return (visionSettings == null) ? 0 : visionSettings.size();
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnTypes[columnIndex];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex != 0;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        AbstractVisionSettings visionSettings = this.visionSettings.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return visionSettings.getId();
            case 1:
                return visionSettings.getName();
            default:
                return null;
        }
    }

    public AbstractVisionSettings getVisionSettings(int index) {
        return visionSettings.get(index);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        AbstractVisionSettings visionSettings = this.visionSettings.get(rowIndex);
        if (columnIndex == 1) {
            visionSettings.setName((String) aValue);
        }
    }

}
