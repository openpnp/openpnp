/*
    Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
    
    This file is part of OpenPnP.
    
    OpenPnP is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenPnP is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenPnP.  If not, see <http://www.gnu.org/licenses/>.
    
    For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.model;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;

/**
 * A Footprint is a group of SMD pads along with length unit information.
 * Footprints can be rendered to a Shape for easy display using 2D primitives.
 */
public class Footprint {
    @Attribute
    private LengthUnit units = LengthUnit.Millimeters;
    
    @ElementList(inline=true, required=false)
    private ArrayList<Pad> pads = new ArrayList<>();
    
    @Attribute(required=false)
    private double bodyWidth;
    
    @Attribute(required=false)
    private double bodyHeight;
    
    public Shape getShape() {
        Path2D.Double shape = new Path2D.Double();
        for (Pad pad : pads) {
            shape.append(pad.getShape(), false);
        }
        
        Pad body = new Pad();
        body.setWidth(bodyWidth);
        body.setHeight(bodyHeight);
        shape.append(body.getShape(), false);
        
        return shape;
    }
    
    public LengthUnit getUnits() {
        return units;
    }

    public void setUnits(LengthUnit units) {
        this.units = units;
    }
    
    public List<Pad> getPads() {
        return pads;
    }
    
    public void removePad(Pad pad) {
        pads.remove(pad);
    }
    
    public void addPad(Pad pad) {
        pads.add(pad);
    }
    
    public double getBodyWidth() {
        return bodyWidth;
    }

    public void setBodyWidth(double bodyWidth) {
        this.bodyWidth = bodyWidth;
    }

    public double getBodyHeight() {
        return bodyHeight;
    }

    public void setBodyHeight(double bodyHeight) {
        this.bodyHeight = bodyHeight;
    }



    public static class Pad {
        @Attribute
        private String name;
        
        @Attribute
        private double x;
        
        @Attribute
        private double y;
        
        @Attribute
        private double width;
        
        @Attribute
        private double height;
        
        @Attribute(required=false)
        private double rotation = 0;
        
        /**
         * Roundness as a percentage of the width and height. 0 is square,
         * 100 is round.
         */
        @Attribute(required=false)
        private double roundness = 0;
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public double getX() {
            return x;
        }
        
        public void setX(double x) {
            this.x = x;
        }
        
        public double getY() {
            return y;
        }
        
        public void setY(double y) {
            this.y = y;
        }
        
        public double getWidth() {
            return width;
        }
        
        public void setWidth(double width) {
            this.width = width;
        }
        
        public double getHeight() {
            return height;
        }
        
        public void setHeight(double height) {
            this.height = height;
        }
        
        public double getRotation() {
            return rotation;
        }
        
        public void setRotation(double rotation) {
            this.rotation = rotation;
        }
        
        public double getRoundness() {
            return roundness;
        }
        
        public void setRoundness(double roundness) {
            this.roundness = roundness;
        }
        
        public Shape getShape() {
            Shape shape = new RoundRectangle2D.Double(
                    -width / 2,
                    -height / 2,
                    width,
                    height,
                    width / 100.0 * roundness,
                    height / 100.0 * roundness);
            AffineTransform tx = new AffineTransform();
            tx.translate(x, -y);
            tx.rotate(Math.toRadians(-rotation));
            return tx.createTransformedShape(shape);
        }
    }
}