package org.openpnp.vision.pipeline.ui;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;

@SuppressWarnings("serial")
public class StagesTableModel extends AbstractTableModel {
    private static String[] columnNames = {
            "Name",
            "Stage"
    };
    
    private static Class<?>[] columnClasses = {
            String.class,
            String.class
    };
    
    private final List<CvStage> stages;
    
    public StagesTableModel(CvPipeline pipeline) {
        this.stages = pipeline.getStages();
    }
    
    public void refresh() {
        fireTableDataChanged();
    }
    
    public CvStage getStage(int rowIndex) {
        return stages.get(rowIndex);
    }
    
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 0;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        CvStage stage = getStage(rowIndex);
        switch (columnIndex) {
            case 0:
                return stage.getName();
            case 1:
                return stage.getClass().getSimpleName();
        }
        return null;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        CvStage stage = getStage(rowIndex);
        stage.setName(aValue.toString());
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
