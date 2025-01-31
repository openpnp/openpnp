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

package org.openpnp.model;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.util.ArrayList;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persist;

/**
 * Extends java.awt.geom.Path2D to be serializable and have length units
 */
@SuppressWarnings("serial")
public class GeometricPath2D extends Path2D.Double {
    @Attribute
    protected LengthUnit units = LengthUnit.Millimeters;
    
    @Attribute
    protected Integer windingRule = null;
    
    @ElementList
    protected ArrayList<Integer> segmentTypes = null;
    
    @ElementList
    protected ArrayList<java.lang.Double> segmentPoints = null;;
    
    GeometricPath2D() {
        super();
    }

    /**
     * Constructs a new empty GeometricPath2D object with the specified winding rule to control 
     * operations that require the interior of the path to be defined.
     * @param windingRule - the winding rule
     */
    GeometricPath2D(int windingRule) {
        super(windingRule);
    }
    
    /**
     * Constructs a new empty GeometricPath2D object with the specified winding rule and the 
     * specified initial capacity to store path segments. This number is an initial guess as to how
     * many path segments are in the path, but the storage is expanded as needed to store whatever
     * path segments are added to this path.
     * @param windingRule - the winding rule
     * @param initialCapacity - the estimate for the number of path segments in the path
     */
    GeometricPath2D(int windingRule, int initialCapacity) {
        super(windingRule, initialCapacity);
    }
    
    /**
     * Constructs a new GeometricPath2D object from an arbitrary Shape object. All of the initial 
     * geometry and the winding rule for this path are taken from the specified Shape object and 
     * units are set to Millimeters.
     * @param shape - the specified Shape object
     */
    GeometricPath2D(Shape shape) {
        super(shape);
    }

    /**
     * Constructs a new GeometricPath2D object from an arbitrary Shape object and a specified 
     * LengthUnit. All of the initial geometry and the winding rule for this path are taken from 
     * the specified Shape object and units are set as specified.
     * @param shape - the specified Shape object
     * @param units - the specified units
     */
    GeometricPath2D(Shape shape, LengthUnit units) {
        super(shape);
        this.units = units;
    }

    /**
     * Constructs a new copy of the specified GeometricPath2D
     * @param geometricPath2D - the path to copy
     */
    GeometricPath2D(GeometricPath2D geometricPath2D) {
        super(geometricPath2D);
        units = geometricPath2D.getUnits();
    }
    
    /**
     * Restores the path from the serializable elements immediately following their deserialization
     */
    @Commit
    public void commit() {
        if (windingRule != null) {
            setWindingRule(windingRule);
        }
        if (segmentTypes != null && segmentPoints != null) {
            int idx = 0;
            for (int segType : segmentTypes) {
                switch (segType) {
                    case PathIterator.SEG_MOVETO:
                        moveTo(segmentPoints.get(idx), segmentPoints.get(idx+1));
                        idx += 2;
                    case PathIterator.SEG_LINETO:
                        lineTo(segmentPoints.get(idx), segmentPoints.get(idx+1));
                        idx += 2;
                        break;
                    case PathIterator.SEG_QUADTO:
                        quadTo(segmentPoints.get(idx), segmentPoints.get(idx+1), 
                                segmentPoints.get(idx+2), segmentPoints.get(idx+3));
                        idx += 4;
                        break;
                    case PathIterator.SEG_CUBICTO:
                        curveTo(segmentPoints.get(idx), segmentPoints.get(idx+1), 
                                segmentPoints.get(idx+2), segmentPoints.get(idx+3), 
                                segmentPoints.get(idx+4), segmentPoints.get(idx+5));
                        idx += 6;
                        break;
                    case PathIterator.SEG_CLOSE:
                        closePath();
                        break;
                }
            }
        }
    }
    
    /**
     * Sets the serializable elements from the path just prior to serialization
     */
    @Persist
    public void persist() {
        windingRule = getWindingRule();
        PathIterator pathIter = getPathIterator(null);
        if (!pathIter.isDone()) {
            segmentTypes = new ArrayList<>();
            segmentPoints = new ArrayList<>();
        }
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
     * 
     * @return the units of this
     */
    public LengthUnit getUnits() {
        return units;
    }

    /**
     * Sets the units of this
     * @param units - the units to set
     */
    public void setUnits(LengthUnit units) {
        this.units = units;
    }

    /**
     * Returns a new GeometricPath2D converted to the specified units
     * @param units - the units of the new path
     * @return the new GeometricPath2D
     */
    public GeometricPath2D convertToUnits(LengthUnit units) {
        double scale = (new Length(1, this.units)).divide(new Length(1, units));
        AffineTransform at = new AffineTransform();
        at.scale(scale, scale);
        GeometricPath2D ret = new GeometricPath2D(this.createTransformedShape(at));
        ret.setUnits(units);
        return ret;
    }
}
