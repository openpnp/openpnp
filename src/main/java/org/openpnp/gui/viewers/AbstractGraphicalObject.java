package org.openpnp.gui.viewers;

import java.awt.Color;
import java.awt.Shape;

public abstract class AbstractGraphicalObject {
    private final Shape shape;
    private final Color fillColor;
    private final Color strokeColor;
    
    AbstractGraphicalObject() {
        shape = null;
        fillColor = null;
        strokeColor = null;
    }

    /**
     * @return the shape
     */
    public Shape getShape() {
        return shape;
    }

    /**
     * @return the fillColor
     */
    public Color getFillColor() {
        return fillColor;
    }

    /**
     * @return the strokeColor
     */
    public Color getStrokeColor() {
        return strokeColor;
    }
    
}
