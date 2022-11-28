package org.openpnp.gui.tablemodel;

public interface ColumnWidthSaveable {
    public static final int FIXED = 0;
    public static final int PROPORTIONAL = 1;
    
    public int[] getColumnWidthTypes();
}
