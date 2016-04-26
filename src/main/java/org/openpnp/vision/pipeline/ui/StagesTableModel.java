package org.openpnp.vision.pipeline.ui;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;

@SuppressWarnings("serial")
public class StagesTableModel extends AbstractTableModel implements Reorderable {
    private static String[] columnNames = {"Enabled", "Name", "Stage"};

    private static Class<?>[] columnClasses = {Boolean.class, String.class, String.class};

    private final List<CvStage> stages;
    private final CvPipeline pipeline;

    public StagesTableModel(CvPipeline pipeline) {
        this.pipeline = pipeline;
        this.stages = pipeline.getStages();
    }

    public void refresh() {
        fireTableDataChanged();
    }

    @Override
    public void reorder(int fromIndex, int toIndex) {
        CvStage stage = getStage(fromIndex);
        pipeline.remove(stage);
        if (fromIndex < toIndex) {
            toIndex--;
        }
        pipeline.insert(stage, toIndex);
        refresh();
    }

    public CvStage getStage(int rowIndex) {
        return stages.get(rowIndex);
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 0 || columnIndex == 1;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        CvStage stage = getStage(rowIndex);
        switch (columnIndex) {
            case 0:
                return stage.isEnabled();
            case 1:
                return stage.getName();
            case 2:
                return stage.getClass().getSimpleName();
        }
        return null;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        CvStage stage = getStage(rowIndex);
        switch (columnIndex) {
            case 0:
                stage.setEnabled((Boolean) aValue);
                break;
            case 1:
                stage.setName(aValue.toString());
                break;
        }
    }

    @Override
    public int getRowCount() {
        return stages.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnClasses[columnIndex];
    }
}
