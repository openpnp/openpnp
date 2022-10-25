package org.openpnp.gui.tablemodel;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.openpnp.Translations;
import org.openpnp.model.AbstractVisionSettings;
import org.openpnp.model.Configuration;

public class VisionSettingsTableModel extends AbstractObjectTableModel implements PropertyChangeListener {

    private String[] columnNames = new String[]{
            Translations.getStringOrDefault("VisionSettingsTableModel.ColumnName.Name", "Name"),
            Translations.getStringOrDefault("VisionSettingsTableModel.ColumnName.AssignedTo", "Assigned To")
    };
    private Class[] columnTypes = new Class[] {String.class, String.class};

    private List<AbstractVisionSettings> visionSettings;

    public VisionSettingsTableModel() {
        Configuration.get().addPropertyChangeListener("visionSettings", this);
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
        return columnIndex == 0 && !getRowObjectAt(rowIndex).isStockSetting();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        AbstractVisionSettings visionSettings = this.visionSettings.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return visionSettings.getName();
            case 1:
                return new AbstractVisionSettings.ListConverter(false)
                        .convertForward(visionSettings.getUsedBottomVisionIn());
            default:
                return null;
        }
    }

    @Override
    public AbstractVisionSettings getRowObjectAt(int index) {
        return visionSettings.get(index);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        AbstractVisionSettings visionSettings = this.visionSettings.get(rowIndex);
        if (columnIndex == 0) {
            visionSettings.setName((String) aValue);
        }
    }

    @Override
    public int indexOf(Object selectedVisionSettings) {
        return visionSettings.indexOf(selectedVisionSettings);
    }

}
