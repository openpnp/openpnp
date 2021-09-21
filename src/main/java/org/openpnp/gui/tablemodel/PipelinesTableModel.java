package org.openpnp.gui.tablemodel;

import org.openpnp.model.Configuration;
import org.openpnp.model.Pipeline;

import javax.swing.table.AbstractTableModel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

public class PipelinesTableModel extends AbstractTableModel implements PropertyChangeListener {

    private String[] columnNames =
            new String[]{"ID", "Name"};

    private List<Pipeline> pipelines;

    public PipelinesTableModel() {
        Configuration.get().addPropertyChangeListener("pipelines", this);
        pipelines = new ArrayList<>(Configuration.get().getPipelines());
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getSource() instanceof Pipeline) {
            fireTableDataChanged();
        } else {
            if (pipelines != null) {
                for (Pipeline pipeline : pipelines) {
                    pipeline.removePropertyChangeListener(this);
                }
            }
            pipelines = new ArrayList<>(Configuration.get().getPipelines());
            fireTableDataChanged();
            for (Pipeline pipeline : pipelines) {
                pipeline.addPropertyChangeListener(this);
            }
        }
    }

    @Override
    public int getRowCount() {
        return (pipelines == null) ? 0 : pipelines.size();
    }

    public Pipeline getPipeline(int index) {
        return pipelines.get(index);
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
    public Object getValueAt(int rowIndex, int columnIndex) {
        Pipeline pipeline = pipelines.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return pipeline.getId();
            case 1:
                return pipeline.getName();
            default:
                return null;
        }
    }
}
