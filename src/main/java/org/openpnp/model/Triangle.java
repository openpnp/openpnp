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

import java.util.Arrays;
import java.util.Locale;

import org.openpnp.util.Utils2D;
import org.simpleframework.xml.Attribute;

public class Triangle {
    @Attribute
    Location pt1;
    @Attribute
    Location pt2;
    @Attribute
    Location pt3;

    
    double[] angles = null; 
    
    public Triangle(Location pt1, Location pt2, Location pt3){
        this.pt1 = pt1;
        this.pt2 = pt2;
        this.pt3 = pt3;
    }
    
    public double[] getLineAngles() {
        angles = new double[3];
        angles[0] = Utils2D.getAngleFromPoint(pt1,pt2);
        angles[1] = Utils2D.getAngleFromPoint(pt2,pt3);
        angles[2] = Utils2D.getAngleFromPoint(pt3,pt1);
        return angles;
    }
    
    public double[] getInternalAngles() {
        double[] lineAngles = getLineAngles();
        double[] cornerAngles = new double[3];
        
        cornerAngles[0] = lineAngles[1] - lineAngles[0];
        cornerAngles[1] = lineAngles[2] - lineAngles[1];
        cornerAngles[2] = lineAngles[0] - lineAngles[2];
        for(int i=0; i<3; i++) {
            //Make sure all angles are positive rotations.
            cornerAngles[i] = (cornerAngles[i]+360) % 360.0;
            //Check for external angle
            if (Math.abs(cornerAngles[i]) > 180) {
                cornerAngles[i] = 360-cornerAngles[i];
            }
        }
        return cornerAngles;
    }
    
    public Triangle rotate(double angle, Location pt) {
        return new Triangle(
                pt1.rotateXyCenterPoint(pt, angle),
                pt2.rotateXyCenterPoint(pt, angle),
                pt3.rotateXyCenterPoint(pt, angle)
                );
    }
    
    protected Location skewLocation(Location pt, double skewX, double skewY) {
        return pt.derive(
                pt.getX() + (skewX * pt.getY()), 
                pt.getY() + (skewY * pt.getX()),
                null, null);
    }
    
    public Triangle skew(double skewX, double skewY) {
        
        return new Triangle(
                skewLocation(pt1, skewX, skewY),
                skewLocation(pt2, skewX, skewY),
                skewLocation(pt3, skewX, skewY)
                );
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "(%s, %s, %s)", pt1.toString(), pt2.toString(), pt3.toString());
    }

}
