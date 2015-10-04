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
//import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
//import java.awt.geom.RoundRectangle2D;
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
    private ArrayList<Pad> pads = new ArrayList<Pad>();
    
    public Shape getShape() {
        if (pads.isEmpty()) {
            return null;
        }
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

    public void addPad(Pad pad) {
    	pads.add(pad);
    }
}