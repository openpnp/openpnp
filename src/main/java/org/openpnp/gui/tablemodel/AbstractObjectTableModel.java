package org.openpnp.gui.tablemodel;

import javax.swing.table.AbstractTableModel;

public abstract class AbstractObjectTableModel extends AbstractTableModel {

    /**
     * @param index
     * @return the object at the row index of the object table model.
     */
    public abstract Object getRowObjectAt(int index);

    /**
     * @param object
     * @return the row index of the object in the object table model.
     */
    public int indexOf(Object object) {
        int count = getRowCount();
        for (int i = 0; i < count; i++) {
            if (getRowObjectAt(i) == object) {
                return i;
            }
        }
        return -1;
    }
}
