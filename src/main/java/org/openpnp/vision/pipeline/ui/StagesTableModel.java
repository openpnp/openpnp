package org.openpnp.vision.pipeline.ui;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;

public class StagesTableModel extends AbstractTableModel {
    private static String[] columnNames = {
            "Name",
            "Stage"
    };
    
    private static Class<?>[] columnClasses = {
            String.class,
            String.class
    };
    
    private final CvPipeline pipeline;
    private final List<CvStage> stages;
    
    public StagesTableModel(CvPipeline pipeline) {
        this.pipeline = pipeline;
        this.stages = pipeline.getStages();
    }
    
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 0;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        CvStage stage = stages.get(rowIndex);
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
        // TODO Auto-generated method stub
        super.setValueAt(aValue, rowIndex, columnIndex);
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
