/*
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.model;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;

import org.openpnp.gui.importer.KicadModImporter;
import org.python.antlr.base.boolop;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;

/**
 * A Footprint is a group of SMD pads along with length unit information. Footprints can be rendered
 * to a Shape for easy display using 2D primitives.
 */
public class Footprint extends AbstractModelObject{
    @Attribute
    private LengthUnit units = LengthUnit.Millimeters;

    @ElementList(inline = true, required = false)
    private ArrayList<Pad> pads = new ArrayList<>();

    @Attribute(required = false)
    private double bodyWidth;

    @Attribute(required = false)
    private double bodyHeight;

    @Attribute(required = false)
    private double outerDimension;

    @Attribute(required = false)
    private double innerDimension;

    @Attribute(required = false)
    private int padCount;

    @Attribute(required = false)
    private double padPitch;

    @Attribute(required = false)
    private double padAcross;

    @Attribute(required = false)
    private double padRoundness;
    
    public enum Generator {
        Dual,
        Quad,
        Bga,
        Kicad;
    }

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
    
    public Shape getBodyShape() {
        Path2D.Double shape = new Path2D.Double();
        Pad body = new Pad();
        body.setWidth(bodyWidth);
        body.setHeight(bodyHeight);
        shape.append(body.getShape(), false);
        return shape;
    }
    
    public Shape getPadsShape() {
        Path2D.Double shape = new Path2D.Double();
        for (Pad pad : pads) {
            shape.append(pad.getShape(), false);
        }
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

    public void removeAllPads() {
        pads = new ArrayList<>();
    }

    public void addPad(Pad pad) {
        pads.add(pad);
    }

    public void toggleMark(Pad pad) {
        if (pad.getMark()) {
            pad.setMark(false);
        } else {
            for (Pad pad2 : pads) {
                pad2.setMark(false);
            }
            pad.setMark(true);
        }
    }

    public double getBodyWidth() {
        return bodyWidth;
    }

    public void setBodyWidth(double bodyWidth) {
        Object oldValue = this.bodyWidth;
        this.bodyWidth = bodyWidth;
        firePropertyChange("bodyWidth", oldValue, bodyWidth);
    }

    public double getBodyHeight() {
        return bodyHeight;
    }

    public void setBodyHeight(double bodyHeight) {
        Object oldValue = this.bodyHeight;
        this.bodyHeight = bodyHeight;
        firePropertyChange("bodyHeight", oldValue, bodyHeight);
    }



    public double getOuterDimension() {
        return outerDimension;
    }

    public void setOuterDimension(double outerDimension) {
        Object oldValue = this.outerDimension;
        this.outerDimension = outerDimension;
        firePropertyChange("outerDimension", oldValue, outerDimension);
    }

    public double getInnerDimension() {
        return innerDimension;
    }

    public void setInnerDimension(double innerDimension) {
        Object oldValue = this.innerDimension;
        this.innerDimension = innerDimension;
        firePropertyChange("innerDimension", oldValue, innerDimension);
    }

    public int getPadCount() {
        return padCount;
    }

    public void setPadCount(int padCount) {
        Object oldValue = this.padCount;
        this.padCount = padCount;
        firePropertyChange("padCount", oldValue, padCount);
    }

    public double getPadPitch() {
        return padPitch;
    }

    public void setPadPitch(double padPitch) {
        Object oldValue = this.padPitch;
        this.padPitch = padPitch;
        firePropertyChange("padPitch", oldValue, padPitch);
    }

    public double getPadAcross() {
        return padAcross;
    }

    public void setPadAcross(double padAcross) {
        Object oldValue = this.padAcross;
        this.padAcross = padAcross;
        firePropertyChange("padAcross", oldValue, padAcross);
    }

    public double getPadRoundness() {
        return padRoundness;
    }

    public void setPadRoundness(double padRoundness) {
        Object oldValue = this.padRoundness;
        this.padRoundness = padRoundness;
        firePropertyChange("padRoundness", oldValue, padRoundness);
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

        @Attribute(required = false)
        private double rotation = 0;

        @Attribute(required = false)
        private boolean mark = false;

        /**
         * Roundness as a percentage of the lesser of width and height. 0 is square, 100 is round.
         */
        @Attribute(required = false)
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

        public boolean getMark() {
            return mark;
        }

        public void setMark(boolean mark) {
            this.mark = mark;
        }

        public String toString() {
            return x+", "+y+" "+width+" x "+height+" "+rotation+"°";
        }

        public Shape getShape() {
            double r = Math.min(width, height)*roundness/100;
            Shape padShape;
            if (r < 0) {
                // one-sided roundness. How r might be negative ?
                java.awt.geom.Area area = new java.awt.geom.Area(new RoundRectangle2D.Double(-width / 2, -height / 2, width, height,
                        -r, -r));
                area.add(new java.awt.geom.Area(new Rectangle2D.Double(0, -height / 2, width/2, height)));
                padShape = area;
            }
            else {
                padShape = new RoundRectangle2D.Double(-width / 2, -height / 2, width, height,
                    r, r);
            }
            AffineTransform tx = new AffineTransform();
            tx.translate(x, y);
            tx.rotate(Math.toRadians(rotation));
            Path2D.Double result = (Path2D.Double) tx.createTransformedShape(padShape);
            if (mark) {
                /* simple mark in center of pad. It is XORed in vision compositing view so might overlap.
                 * It would be not easy to compute where put mark outside as pads might be each other.
                 */
                r = Math.min(width, height) * 0.9;
                r = Math.min(r, 1);  // limit max.size of mark
                Shape markShape = new java.awt.geom.Ellipse2D.Double(x - r / 2, y - r / 2, r, r);
                result.append(markShape, false);
            }
            return result;
        }

        /**
         * @return The corner points of the pad, starting from the upper left (pin 1) corner,
         * and going counter-clockwise. Roundness is ignored.
         */
        public Point[] corners() {
            Point[] points = new Point[4];
            points[0] = new Point(x - width/2, y + height/2);
            points[1] = new Point(x - width/2, y - height/2);
            points[2] = new Point(x + width/2, y - height/2);
            points[3] = new Point(x + width/2, y + height/2);
            // Rotate
            double s = Math.sin(Math.toRadians(rotation));
            double c = Math.cos(Math.toRadians(rotation));
            int i = 0;
            for (Point pt : points) {
                points[i++] = new Point(c*pt.x - s*pt.y, s*pt.x + c*pt.y);
            }
            return points;
        }

        public Pad boundingBox() {
            double x0 = Double.POSITIVE_INFINITY;
            double x1 = Double.NEGATIVE_INFINITY;
            double y0 = Double.POSITIVE_INFINITY;
            double y1 = Double.NEGATIVE_INFINITY;
            for (Point pt : corners()) {
                x0 = Math.min(pt.x, x0);
                x1 = Math.max(pt.x, x1);
                y0 = Math.min(pt.y, y0);
                y1 = Math.max(pt.y, y1);
            }
            // Create the unrotated bounding pad from it.
            Pad pad = new Pad();
            pad.setName(name);
            pad.setWidth(x1 - x0);
            pad.setHeight(y1 - y0);
            pad.setX(x);
            pad.setY(y);
            return pad;
        }
    }

    public void generate(Footprint.Generator type) throws Exception {
        double outerDimension = getOuterDimension();
        double innerDimension = getInnerDimension();
        int padCount = getPadCount();
        double padAcross = getPadAcross();
        double padPitch = getPadPitch();
        double padRoundness = getPadRoundness();
        switch (type) {
            case Dual: 
            {
                if (padCount % 2 != 0 || padCount <= 0) {
                    throw new Exception("For Dual form factor, the pad count must be positive multiples of 2.");
                }
                double padLength = (outerDimension - innerDimension)/2;
                double x = (outerDimension + innerDimension)/4;
                int padNr = 0;
                // Left side.
                for (; padNr < padCount/2; padNr++) {
                    Pad pad = new Pad();
                    pad.setName(String.valueOf(padNr+1));
                    pad.setWidth(padLength);
                    pad.setHeight(padAcross);
                    pad.setRotation(180); // strictly speaking
                    pad.setX(-x);
                    pad.setY((padCount/4.0 - padNr - 0.5)*padPitch);
                    pad.setRoundness(padRoundness);
                    addPad(pad);
                }
                // Right side.
                for (; padNr < padCount; padNr++) {
                    Pad pad = new Pad();
                    pad.setName(String.valueOf(padNr+1));
                    pad.setWidth(padLength);
                    pad.setHeight(padAcross);
                    pad.setRotation(0);
                    pad.setX(x);
                    pad.setY((padNr - padCount*3/4.0 + 0.5)*padPitch);
                    pad.setRoundness(padRoundness);
                    addPad(pad);
                }
                if (bodyWidth == 0 && bodyHeight == 0) {
                    setBodyWidth(innerDimension);
                    setBodyHeight(padCount/2*padPitch);
                }
                break;
            }
            case Quad:
            {
                if (padCount % 4 != 0 || padCount <= 0) {
                    throw new Exception("For Quad form factor, the pad count must be positive multiples of 4.");
                }
                double padLength = (outerDimension - innerDimension)/2;
                double d = (outerDimension + innerDimension)/4;
                int padNr = 0;
                // Left side.
                for (; padNr < padCount*1/4; padNr++) {
                    Pad pad = new Pad();
                    pad.setName(String.valueOf(padNr+1));
                    pad.setWidth(padLength);
                    pad.setHeight(padAcross);
                    pad.setRotation(180); // strictly speaking
                    pad.setX(-d);
                    pad.setY((padCount/8.0 - padNr - 0.5)*padPitch);
                    pad.setRoundness(padRoundness);
                    addPad(pad);
                }
                // Bottom side.
                for (; padNr < padCount*2/4; padNr++) {
                    Pad pad = new Pad();
                    pad.setName(String.valueOf(padNr+1));
                    pad.setWidth(padLength);
                    pad.setHeight(padAcross);
                    pad.setRotation(-90); 
                    pad.setX((padNr - padCount*3/8.0 + 0.5)*padPitch);
                    pad.setY(-d);
                    pad.setRoundness(padRoundness);
                    addPad(pad);
                }
                // Right side.
                for (; padNr < padCount*3/4; padNr++) {
                    Pad pad = new Pad();
                    pad.setName(String.valueOf(padNr+1));
                    pad.setWidth(padLength);
                    pad.setHeight(padAcross);
                    pad.setRotation(0);
                    pad.setX(d);
                    pad.setY((padNr - padCount*5/8.0 + 0.5)*padPitch);
                    pad.setRoundness(padRoundness);
                    addPad(pad);
                }
                // Top side.
                for (; padNr < padCount*4/4; padNr++) {
                    Pad pad = new Pad();
                    pad.setName(String.valueOf(padNr+1));
                    pad.setWidth(padLength);
                    pad.setHeight(padAcross);
                    pad.setRotation(90); 
                    pad.setX((padCount*7/8.0 - padNr - 0.5)*padPitch);
                    pad.setY(d);
                    pad.setRoundness(padRoundness);
                    addPad(pad);
                }
                if (bodyWidth == 0 && bodyHeight == 0) {
                    setBodyWidth(innerDimension);
                    setBodyHeight(innerDimension);
                }
                break;
            }
            case Bga:
            {
                int cols = (int)Math.sqrt(padCount);
                int rows = cols;
                if (Math.pow(rows, 2) != padCount) {
                    throw new Exception("For PGA, the pad count must be a square number.");
                }
                double d = (outerDimension + innerDimension)/4;
                for (int row = 0; row < rows; row++) {
                    for (int col = 0; col < cols; col++) {
                        double x = (col - cols/2.0 + 0.5)*padPitch;
                        double y = (rows/2.0 - row - 0.5)*padPitch;
                        if (Math.abs(x) > innerDimension/2 || Math.abs(y) > innerDimension/2) {
                            Pad pad = new Pad();
                            pad.setName(String.valueOf((char)('A'+row))+String.valueOf(col));
                            pad.setWidth(padAcross);
                            pad.setHeight(padAcross);
                            pad.setX(x);
                            pad.setY(y);
                            pad.setRoundness(padRoundness);
                            addPad(pad);
                        }
                    }
                }
                setOuterDimension((rows - 1)*padPitch + padAcross);
                if (bodyWidth == 0 && bodyHeight == 0) {
                    setBodyWidth(outerDimension+padPitch);
                    setBodyHeight(outerDimension+padPitch);
                }
                break;
            }
            case Kicad:
            {
                KicadModImporter importer = new KicadModImporter();
                for (Pad pad : importer.getPads()) {
                    addPad(pad);
                }
                break;
            }
        }
    }
}
