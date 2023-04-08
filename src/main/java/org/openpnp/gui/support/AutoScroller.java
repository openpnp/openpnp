package org.openpnp.gui.support;

import java.awt.event.*;
import javax.swing.*;

/**
 * The AutoScroller will attempt to keep the viewport positioned based on
 * the users interaction with the scrollbar. The normal behaviour is to keep
 * the viewport positioned to see new data as it is dynamically added.
 */
public class AutoScroller implements AdjustmentListener {

    private JScrollBar scrollBar;
    private boolean adjustScrollBar = true;

    private int previousValue = -1;
    private int previousMaximum = -1;

    public AutoScroller(JScrollPane scrollPane) {
        scrollBar = scrollPane.getVerticalScrollBar();
        scrollBar.addAdjustmentListener(this);
    }

    @Override
    public void adjustmentValueChanged(final AdjustmentEvent e) {
        SwingUtilities.invokeLater(() -> checkScrollBar(e));
    }

    /**
     * Scroll down to the bottom of the list
     */
    public void scrollDown() {
        scrollBar.setValue(scrollBar.getModel().getMaximum());
    }

    /*
     *  Analyze every adjustment event to determine when the viewport
     *  needs to be repositioned.
     */
    private void checkScrollBar(AdjustmentEvent e) {

        //  The scroll bar listModel contains information needed to determine
        //  whether the viewport should be repositioned or not.
        JScrollBar scrollBar = (JScrollBar) e.getSource();
        BoundedRangeModel listModel = scrollBar.getModel();
        int value = listModel.getValue();
        int extent = listModel.getExtent();
        int maximum = listModel.getMaximum();

        boolean valueChanged = previousValue != value;
        boolean maximumChanged = previousMaximum != maximum;

        //  Check if the user has manually repositioned the scrollbar
        if (valueChanged && !maximumChanged) {
            adjustScrollBar = value + extent >= maximum;
        }

        /*
          Reset the "value" so we can reposition the viewport
          and distinguish between a user scroll and a program scroll.
         */
        if (adjustScrollBar) {
            //  Scroll the viewport to the end.
            scrollBar.removeAdjustmentListener(this);
            value = maximum - extent;
            scrollBar.setValue(value);
            scrollBar.addAdjustmentListener(this);
        }

        previousValue = value;
        previousMaximum = maximum;
    }
}