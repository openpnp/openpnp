package org.openpnp.vision.pipeline.ui;

import org.openpnp.model.Configuration;
import org.openpnp.model.Package;
import org.openpnp.model.Pipeline;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class PipelineEditorPackagesTableModel extends AbstractTableModel implements Reorderable {
    private static String[] columnNames = {"ID"};

    private static Class<?>[] columnClasses = {String.class};

    private final List<Package> packages;
    private final Pipeline pipeline;

    public PipelineEditorPackagesTableModel(Pipeline pipeline) {
        this.pipeline = pipeline;
        packages = getPackages();
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

    private List<Package> getPackages() {
        List<Package> result = new ArrayList<>();
        Configuration.get().getPackages().forEach(pkg -> {
            //TODO: NK: not safe, ensure every part gets at least default pipeline, otherwise pipeline id is null
            if (pkg.getPipeline().getId() != null && pkg.getPipeline().getId().equals(pipeline.getId())) {
                result.add(pkg);
            }
        });

        return result;
    }
}
