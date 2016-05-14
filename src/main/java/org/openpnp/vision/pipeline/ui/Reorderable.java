package org.openpnp.vision.pipeline.ui;

/**
 * From: http://stackoverflow.com/questions/638807/how-do-i-drag-and-drop-a-row-in-a-jtable
 */
public interface Reorderable {
    public void reorder(int fromIndex, int toIndex);
}
