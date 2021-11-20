package org.openpnp.vision.pipeline.ui;

import org.openpnp.model.Configuration;
import org.openpnp.model.Package;

import javax.swing.table.AbstractTableModel;
import java.util.List;

public class PipelineEditorPackagesTableModel extends AbstractTableModel implements Reorderable {
    private static String[] columnNames = {"ID"};

    private static Class<?>[] columnClasses = {String.class};

    private final List<Package> packages;

    public PipelineEditorPackagesTableModel(String visionSettingsId) {
        packages = getPackages(visionSettingsId);
    }

    public void refresh() {
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return packages.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Package pkg = getPackage(rowIndex);
        if (columnIndex == 0) {
            return pkg.getId();
        }

        return null;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        Package pkg = getPackage(rowIndex);
        if (columnIndex == 0) {
            pkg.setId(aValue.toString());
        }
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnClasses[columnIndex];
    }

    @Override
    public void reorder(int fromIndex, int toIndex) {

    }

    public Package getPackage(int index) {
        return packages.get(index);
    }

    private List<Package> getPackages(String visionSettingsId) {
        return Configuration.get().getPackages(visionSettingsId);
    }
}
