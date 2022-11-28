package org.openpnp.gui.tablemodel;

import javax.swing.SwingConstants;

public interface ColumnAlignable {
    public static final int LEFT = SwingConstants.LEFT;
    public static final int CENTER = SwingConstants.CENTER;
    public static final int RIGHT = SwingConstants.RIGHT;
    
    public int[] getColumnAlignments();
}
