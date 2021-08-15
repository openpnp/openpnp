package org.openpnp.gui.support;

import java.awt.*;
import java.awt.color.ColorSpace;

@SuppressWarnings("unused")
public class FlexibleColor extends Color {
    public FlexibleColor(int r, int g, int b) {
        super(r, g, b);
    }

    public FlexibleColor(int r, int g, int b, int a) {
        super(r, g, b, a);
    }

    public FlexibleColor(int rgb) {
        super(rgb);
    }

    public FlexibleColor(int rgba, boolean hasalpha) {
        super(rgba, hasalpha);
    }

    public FlexibleColor(float r, float g, float b) {
        super(r, g, b);
    }

    public FlexibleColor(float r, float g, float b, float a) {
        super(r, g, b, a);
    }

    public FlexibleColor(ColorSpace cspace, float[] components, float alpha) {
        super(cspace, components, alpha);
    }

    public Color brighter(int add) {
        int r = getRed();
        int g = getGreen();
        int b = getBlue();
        int alpha = getAlpha();
        return new Color(Math.min(r + add, 255),
                Math.min(g + add, 255),
                Math.min(b + add, 255),
                alpha);
    }

    public Color darker(int subtract) {
        return new Color(Math.max(getRed()  - subtract, 0),
                Math.max(getGreen() - subtract, 0),
                Math.max(getBlue()  - subtract, 0),
                getAlpha());
    }
}
