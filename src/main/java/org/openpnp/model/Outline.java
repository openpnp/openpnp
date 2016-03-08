package org.openpnp.model;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.ElementListUnion;

public class Outline {
    @ElementListUnion({
            @ElementList(entry = "ellipse", inline = true, required = false,
                    type = Outline.Ellipse.class),
            @ElementList(entry = "line", inline = true, required = false,
                    type = Outline.Line.class),
            @ElementList(entry = "circle", inline = true, required = false,
                    type = Outline.Circle.class)})
    private ArrayList<Outline.OutlineElement> elements = new ArrayList<>();

    @Attribute
    private LengthUnit units = LengthUnit.Millimeters;

    public Shape getShape() {
        if (elements.isEmpty()) {
            return null;
        }
        Path2D.Double shape = new Path2D.Double();
        for (Outline.OutlineElement element : elements) {
            shape.append(element.getShape(), false);
        }

        return shape;
    }

    public LengthUnit getUnits() {
        return units;
    }

    public void setUnits(LengthUnit units) {
        this.units = units;
    }

    public static interface OutlineElement {
        Shape getShape();
    }

    public static class Line implements Outline.OutlineElement {
        @Attribute
        private double x1;

        @Attribute
        private double y1;

        @Attribute
        private double x2;

        @Attribute
        private double y2;

        public Shape getShape() {
            return new Line2D.Double(x1, -y1, x2, -y2);
        }
    }

    public static class Ellipse implements Outline.OutlineElement {
        @Attribute
        private double x;

        @Attribute
        private double y;

        @Attribute
        private double width;

        @Attribute
        private double height;

        public Shape getShape() {
            return new Ellipse2D.Double(x - (width / 2), y - (height / 2), width, height);
        }
    }

    public static class Circle implements Outline.OutlineElement {
        @Attribute
        private double x;

        @Attribute
        private double y;

        @Attribute
        private double radius;

        public Shape getShape() {
            return new Ellipse2D.Double(x - radius, y - radius, radius * 2, radius * 2);
        }
    }
}
