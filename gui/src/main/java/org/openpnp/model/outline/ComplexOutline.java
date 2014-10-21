package org.openpnp.model.outline;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.ElementListUnion;
import org.simpleframework.xml.core.Commit;

public class ComplexOutline extends AbstractOutline {
    private Path2D.Double shape;
    
    @ElementListUnion({
        @ElementList(entry="ellipse", inline=true, type=EllipseOp.class),
        @ElementList(entry="line", inline=true, type=LineOp.class),
        @ElementList(entry="circle", inline=true, type=CircleOp.class)
    })
    private ArrayList<Op> ops = new ArrayList<Op>();
    
    public ComplexOutline() {
    }
    
    @SuppressWarnings("unused")
    @Commit
    private void commit() {
        generateShape();
    }
    
    private void generateShape() {
        shape = new Path2D.Double();
        for (Op op : ops) {
            shape.append(op.getShape(), false);
        }
    }
    
    @Override
    public Shape getShape() {
        return shape;
    }
    
    public static interface Op {
        Shape getShape();
    }
    
    public static class LineOp implements Op {
        @Attribute
        private double x1;
        
        @Attribute
        private double y1;
        
        @Attribute
        private double x2;
        
        @Attribute
        private double y2;
        
        public Shape getShape() {
            return new Line2D.Double(
                    x1,
                    -y1,
                    x2,
                    -y2);
        }
    }
    
    public static class EllipseOp implements Op {
        @Attribute
        private double centerX;
        
        @Attribute
        private double centerY;
        
        @Attribute
        private double width;
        
        @Attribute
        private double height;
        
        public Shape getShape() {
            return new Ellipse2D.Double(
                    centerX - (width / 2), 
                    centerY - (height / 2), 
                    width, 
                    height);
        }
    }
    
    public static class CircleOp implements Op {
        @Attribute
        private double centerX;
        
        @Attribute
        private double centerY;
        
        @Attribute
        private double radius;
        
        public Shape getShape() {
            return new Ellipse2D.Double(
                    centerX - radius, 
                    centerY - radius, 
                    radius * 2, 
                    radius * 2);
        }
    }    
}
