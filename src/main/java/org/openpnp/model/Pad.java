package org.openpnp.model;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

import org.simpleframework.xml.Attribute;

public abstract class Pad extends AbstractModelObject {
    @Attribute
    protected LengthUnit units = LengthUnit.Millimeters;

    public abstract Shape getShape();

    public abstract Pad convertToUnits(LengthUnit units);

    public LengthUnit getUnits() {
        return units;
    }

    public void setUnits(LengthUnit units) {
        Object oldValue = units;
        this.units = units;
        firePropertyChange("units", oldValue, units);
    }

    // TODO: Line doesn't really work as a shape, so I am removing it
    // until we really have a need for it at which point it can be revisited.
    // public static class Line extends Pad {
    // @Attribute
    // private double x1;
    //
    // @Attribute
    // private double y1;
    //
    // @Attribute
    // private double x2;
    //
    // @Attribute
    // private double y2;
    //
    // public double getX1() {
    // return x1;
    // }
    //
    // public void setX1(double x1) {
    // double oldValue = this.x1;
    // this.x1 = x1;
    // firePropertyChange("x1", oldValue, x1);
    // }
    //
    // public double getY1() {
    // return y1;
    // }
    //
    // public void setY1(double y1) {
    // double oldValue = this.y1;
    // this.y1 = y1;
    // firePropertyChange("y1", oldValue, y1);
    // }
    //
    // public double getX2() {
    // return x2;
    // }
    //
    // public void setX2(double x2) {
    // double oldValue = this.x2;
    // this.x2 = x2;
    // firePropertyChange("x2", oldValue, x2);
    // }
    //
    // public double getY2() {
    // return y2;
    // }
    //
    // public void setY2(double y2) {
    // double oldValue = this.y2;
    // this.y2 = y2;
    // firePropertyChange("y2", oldValue, y2);
    // }
    //
    // public Shape getShape() {
    // return new Line2D.Double(
    // x1,
    // y1,
    // x2,
    // y2);
    // }
    // }

    public static class Ellipse extends Pad {
        @Attribute
        private double width;

        @Attribute
        private double height;

        public double getWidth() {
            return width;
        }

        public void setWidth(double width) {
            double oldValue = this.width;
            this.width = width;
            firePropertyChange("width", oldValue, width);
        }

        public double getHeight() {
            return height;
        }

        public void setHeight(double height) {
            double oldValue = this.height;
            this.height = height;
            firePropertyChange("height", oldValue, height);
        }

        public Shape getShape() {
            return new Ellipse2D.Double(-width / 2, -height / 2, width, height);
        }

        @Override
        public Ellipse convertToUnits(LengthUnit units) {
            Ellipse that = new Ellipse();
            that.setUnits(units);
            that.setHeight(Length.convertToUnits(height, this.units, units));
            that.setWidth(Length.convertToUnits(width, this.units, units));
            return that;
        }
    }


    public static class Circle extends Pad {
        @Attribute
        private double radius;

        public double getRadius() {
            return radius;
        }

        public void setRadius(double radius) {
            double oldValue = this.radius;
            this.radius = radius;
            firePropertyChange("radius", oldValue, radius);
        }

        public Shape getShape() {
            return new Ellipse2D.Double(-radius, -radius, radius * 2, radius * 2);
        }

        @Override
        public Circle convertToUnits(LengthUnit units) {
            Circle that = new Circle();
            that.setUnits(units);
            that.setRadius(Length.convertToUnits(radius, this.units, units));
            return that;
        }
    }

    public static class RoundRectangle extends Pad {
        @Attribute
        private double width;

        @Attribute
        private double height;

        @Attribute(required = false)
        private double roundness;

        public double getWidth() {
            return width;
        }

        public void setWidth(double width) {
            double oldValue = this.width;
            this.width = width;
            firePropertyChange("width", oldValue, width);
        }

        public double getHeight() {
            return height;
        }

        public void setHeight(double height) {
            double oldValue = this.height;
            this.height = height;
            firePropertyChange("height", oldValue, height);
        }

        public double getRoundness() {
            return roundness;
        }

        public void setRoundness(double roundness) {
            double oldValue = this.roundness;
            this.roundness = roundness;
            firePropertyChange("roundness", oldValue, roundness);
        }

        public Shape getShape() {
            return new RoundRectangle2D.Double(-width / 2, -height / 2, width, height,
                    width / 1.0 * roundness, height / 1.0 * roundness);
        }

        @Override
        public RoundRectangle convertToUnits(LengthUnit units) {
            RoundRectangle that = new RoundRectangle();
            that.setUnits(units);
            that.setHeight(Length.convertToUnits(height, this.units, units));
            that.setWidth(Length.convertToUnits(width, this.units, units));
            // don't convert roundness because it's a percentage
            return that;
        }
    }
}
