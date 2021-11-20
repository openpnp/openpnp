package org.openpnp.vision.pipeline.ui;

import org.openpnp.model.Configuration;
import org.openpnp.model.Part;

import javax.swing.table.AbstractTableModel;
import java.util.List;

public class PipelineEditorPartsTableModel extends AbstractTableModel implements Reorderable {
    private static String[] columnNames = {"ID"};

    private static Class<?>[] columnClasses = {String.class};

    private final List<Part> parts;

    public PipelineEditorPartsTableModel(String visionSettingsId) {
        parts = getParts(visionSettingsId);
    }

    public void refresh() {
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return parts.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Part part = getPart(rowIndex);
        if (columnIndex == 0) {
            return part.getId();
        }

        return null;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        Part part = getPart(rowIndex);
        if (columnIndex == 0) {
            part.setId(aValue.toString());
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

    public Part getPart(int index) {
        return parts.get(index);
    }

    private List<Part> getParts(String visionSettingsId) {
        return Configuration.get().getParts(visionSettingsId);
    }
}
