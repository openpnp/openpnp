/*
 * Copyright (C) 2022 Jason von Nieda <jason@vonnieda.org>, Tony Luken <tonyluken62+openpnp@gmail.com>
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

package org.openpnp.gui.viewers;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.util.ArrayList;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persist;

public abstract class AbstractGraphicalObject {
    @Attribute
    protected Color strokeColor;
    
    @Attribute
    protected Color fillColor;
    
    @ElementList
    protected ArrayList<Integer> segmentTypes = new ArrayList<>();
    
    @ElementList
    protected ArrayList<Double> segmentPoints = new ArrayList<>();
    
    protected transient Shape shape;
    
    AbstractGraphicalObject() {
        shape = null;
        fillColor = null;
        strokeColor = null;
    }

    @Commit
    public void commit() {
        Path2D path = new Path2D.Double();
        int idx = 0;
        for (int segType : segmentTypes) {
            switch (segType) {
                case PathIterator.SEG_MOVETO:
                    path.moveTo(segmentPoints.get(idx), segmentPoints.get(idx+1));
                    idx += 2;
                case PathIterator.SEG_LINETO:
                    path.lineTo(segmentPoints.get(idx), segmentPoints.get(idx+1));
                    idx += 2;
                    break;
                case PathIterator.SEG_QUADTO:
                    path.quadTo(segmentPoints.get(idx), segmentPoints.get(idx+1), 
                            segmentPoints.get(idx+2), segmentPoints.get(idx+3));
                    idx += 4;
                    break;
                case PathIterator.SEG_CUBICTO:
                    path.curveTo(segmentPoints.get(idx), segmentPoints.get(idx+1), 
                            segmentPoints.get(idx+2), segmentPoints.get(idx+3), 
                            segmentPoints.get(idx+4), segmentPoints.get(idx+5));
                    idx += 6;
                    break;
                case PathIterator.SEG_CLOSE:
                    path.closePath();
                    break;
            }
        }
        shape = new Area(path);
    }
    
    @Persist
    public void persist() {
        PathIterator pathIter = shape.getPathIterator(null);
        double[] coords = new double[6];
        while (!pathIter.isDone()) {
            int segType = pathIter.currentSegment(coords);
            segmentTypes.add(segType);
            switch (segType) {
                case PathIterator.SEG_MOVETO:
                case PathIterator.SEG_LINETO:
                    segmentPoints.add(coords[0]);
                    segmentPoints.add(coords[1]);
                    break;
                case PathIterator.SEG_QUADTO:
                    segmentPoints.add(coords[0]);
                    segmentPoints.add(coords[1]);
                    segmentPoints.add(coords[2]);
                    segmentPoints.add(coords[3]);
                    break;
                case PathIterator.SEG_CUBICTO:
                    segmentPoints.add(coords[0]);
                    segmentPoints.add(coords[1]);
                    segmentPoints.add(coords[2]);
                    segmentPoints.add(coords[3]);
                    segmentPoints.add(coords[4]);
                    segmentPoints.add(coords[5]);
                    break;
                case PathIterator.SEG_CLOSE:
                    break;
            }
            pathIter.next();
        }
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
