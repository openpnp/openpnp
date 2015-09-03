package org.openpnp.model;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;

import org.openpnp.model.Board.Side;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

public abstract class Pad extends AbstractModelObject {
    @Attribute
    protected Side side = Side.Top;
    
    @Element
    protected Location location = new Location(LengthUnit.Millimeters);
    
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
    
//    public static class Line extends Pad {
//        @Attribute
//        private double x1;
//        
//        @Attribute
//        private double y1;
//        
//        @Attribute
//        private double x2;
//        
//        @Attribute
//        private double y2;
//        
//        public Shape getShape() {
//            return new Line2D.Double(
//                    x1,
//                    -y1,
//                    x2,
//                    -y2);
//        }
//    }
//    
//    public static class Ellipse extends Pad {
//        // TODO: Probably needs rotation.
//        
//        @Attribute
//        private double x;
//        
//        @Attribute
//        private double y;
//        
//        @Attribute
//        private double width;
//        
//        @Attribute
//        private double height;
//
//        public Shape getShape() {
//            return new Ellipse2D.Double(
//                    x - (width / 2), 
//                    y - (height / 2), 
//                    width, 
//                    height);
//        }
//    }
//    
//    public static class Circle extends Pad {
//        @Attribute
//        private double x;
//        
//        @Attribute
//        private double y;
//        
//        @Attribute
//        private double radius;
//        
//        public Shape getShape() {
//            return new Ellipse2D.Double(
//                    x - radius, 
//                    y - radius, 
//                    radius * 2, 
//                    radius * 2);
//        }
//    }
    
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
