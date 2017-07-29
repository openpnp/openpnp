package org.openpnp.vision.pipeline.ui;

import com.l2fprod.common.propertysheet.PropertySheetTable;

public class PipelinePropertySheetTable extends PropertySheetTable {
    private PipelinePanel pipelinePanel;

    public PipelinePropertySheetTable(PipelinePanel pipelinePanel) {
        this.pipelinePanel = pipelinePanel;
    }

    @Override
    public void setValueAt(Object aValue, int row, int column) {
        super.setValueAt(aValue, row, column);

        pipelinePanel.onStagePropertySheetValueChanged(aValue, row, column);
    }
}
