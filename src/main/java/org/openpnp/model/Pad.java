package org.openpnp.model;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;

import org.openpnp.model.Board.Side;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

public abstract class Pad extends AbstractModelObject {
    public enum Type {
        Paste,
        Ignore
    }
    
    @Attribute(required=false)
    private Type type = Type.Paste;

    @Attribute
    protected Side side = Side.Top;
    
    @Element
    protected Location location = new Location(LengthUnit.Millimeters);
    
    @Attribute(required=false)
    protected String name;
    
    public abstract Shape getShape();
    
    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        Location oldValue = this.location;
        this.location = location;
        firePropertyChange("location", oldValue, location);
    }

    public Side getSide() {
        return side;
    }
    
    public void setSide(Side side) {
        Object oldValue = this.side;
        this.side = side;
        firePropertyChange("side", oldValue, side);
    }
    
    public Type getType() {
        return type;
    }
    
    public void setType(Type type) {
        Object oldValue = this.type;
        this.type = type;
        firePropertyChange("type", oldValue, type);
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        Object oldValue = this.name;
        this.name = name;
        firePropertyChange("name", oldValue, name);
    }
    
    public static class Line extends Pad {
        @Attribute
        private double x1;
        
        @Attribute
        private double y1;
        
        @Attribute
        private double x2;
        
        @Attribute
        private double y2;
        
        public double getX1() {
            return x1;
        }

        public void setX1(double x1) {
            double oldValue = this.x1;
            this.x1 = x1;
            firePropertyChange("x1", oldValue, x1);
        }
        
        public double getY1() {
            return y1;
        }

        public void setY1(double y1) {
            double oldValue = this.y1;
            this.y1 = y1;
            firePropertyChange("y1", oldValue, y1);
        }
        
        public double getX2() {
            return x2;
        }

        public void setX2(double x2) {
            double oldValue = this.x2;
            this.x2 = x2;
            firePropertyChange("x2", oldValue, x2);
        }
        
        public double getY2() {
            return y2;
        }

        public void setY2(double y2) {
            double oldValue = this.y2;
            this.y2 = y2;
            firePropertyChange("y2", oldValue, y2);
        }
        
        public Shape getShape() {
            return new Line2D.Double(
                    location.getX() + x1,
                    location.getY() - y1,
                    location.getX() + x2,
                    location.getY() - y2);
        }
    }
    
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
            return new Ellipse2D.Double(
                    location.getX() - (width / 2), 
                    location.getY() - (height / 2), 
                    width, 
                    height);
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
            return new Ellipse2D.Double(
                    location.getX() - radius, 
                    location.getY() - radius, 
                    radius * 2, 
                    radius * 2);
        }
    }
    
    public static class RoundRectangle extends Pad {
        @Attribute
        private double width;
        
        @Attribute
        private double height;
        
        @Attribute(required=false)
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
            Shape shape = new RoundRectangle2D.Double(
                    -width / 2,
                    -height / 2,
                    width,
                    height,
                    width / 1.0 * roundness,
                    height / 1.0 * roundness);
            AffineTransform tx = new AffineTransform();
            tx.translate(location.getX(), -location.getY());
            tx.rotate(Math.toRadians(-location.getRotation()));
            return tx.createTransformedShape(shape);
        }
    }    
}
