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
 *
 * Changelog:
 * 03/10/2012 Ami: Add rotate using center point
 */

package org.openpnp.util;

import org.openpnp.model.Board.Side;
import org.openpnp.model.Location;
import org.openpnp.model.Point;


public class Utils2D {
	public static Point rotateTranslateScalePoint(Point point, double c, double x, double y, double scaleX, double scaleY) {
		point = rotatePoint(point, c);
		point = translatePoint(point, x, y);
		point = scalePoint(point, scaleX, scaleY);
		return point;
	}
	
	public static Point rotateTranslateCenterPoint(Point point, double c, double x, double y, Point center) {
		point = translatePoint(point, center.getX() * -1, center.getY() * -1);
		point = rotatePoint(point, c);
		point = translatePoint(point,center.getX(), center.getY());
		point = translatePoint(point, x, y);
	
		return point;
	}
	
	public static Point translatePoint(Point point, double x, double y) {
		return new Point(point.getX() + x, point.getY() + y);
	}
	
	/**
	 * Rotation is counter-clockwise for positive angles.
	 * @param point
	 * @param c
	 * @return
	 */
	public static Point rotatePoint(Point point, double c) {
		double x = point.getX();
		double y = point.getY();
		
		// convert degrees to radians
		c = Math.toRadians(c);
		
		// rotate the points
		double xn = x * Math.cos(c) - y * Math.sin(c);
		double yn = x * Math.sin(c) + y * Math.cos(c);
		
		x = xn;
		y = yn;
		
		return new Point(x, y);
	}
	
	public static Point scalePoint(Point point, double scaleX, double scaleY) {
		return new Point(point.getX() * scaleX, point.getY() * scaleY);
	}
	
	public static Location calculateBoardPlacementLocation(Location boardLocation, Side boardSide, Location placementLocation) {
        // We will work in the units of the placementLocation, so convert
        // anything that isn't in those units to it.
        boardLocation = boardLocation.convertToUnits(placementLocation.getUnits());
        
        // If we are placing the bottom of the board we need to invert
        // the placement location.
        if (boardSide == Side.Bottom) {
            placementLocation = placementLocation.invert(true, false, false, false);
        }

        // Create the point that represents the final placement location
        Point p = new Point(placementLocation.getX(),
                placementLocation.getY());

        // Rotate and translate the point into the same coordinate space
        // as the board
        p = Utils2D.rotateTranslateScalePoint(p, boardLocation
                .getRotation(), boardLocation.getX(), boardLocation
                .getY(), 1.0, 1.0);

        // Update the placementLocation with the transformed point
        placementLocation = placementLocation.derive(p.getX(), p.getY(), null, null);

        // Update the placementLocation with the board's rotation and
        // the placement's rotation
        // This sets the rotation of the part itself when it will be
        // placed
        placementLocation = placementLocation.derive(
                null, 
                null, 
                null,
                (placementLocation.getRotation() + boardLocation.getRotation()) % 360.0);
        
        // Set the final location's Z to that of the board location.
        placementLocation = placementLocation.derive(null, null, boardLocation.getZ(), null);
        
        return placementLocation;
	    
	}
	
   /**
     * Given two "ideal" unrotated and unoffset Locations and two matching
     * "actual" Locations that have been offset and rotated, calculate the
     * angle of rotation and offset between them.
     * 
     * Angle is the difference between the angles between the two ideal
     * Locations and the two actual Locations.
     * 
     * Offset is the difference between one of the ideal Locations having been
     * rotated by Angle and the matching actual Location. 
     * 
     * @param idealA
     * @param idealB
     * @param actualA
     * @param actualB
     * @return
     */
    public static Location calculateAngleAndOffset(Location idealA, Location idealB, Location actualA, Location actualB) {
        idealB = idealB.convertToUnits(idealA.getUnits());
        actualA = actualA.convertToUnits(idealA.getUnits());
        actualB = actualB.convertToUnits(idealA.getUnits());

        double angle = Math.toDegrees(Math.atan2(actualA.getY() - actualB.getY(), actualA.getX() - actualB.getX())
                - Math.atan2(idealA.getY() - idealB.getY(), idealA.getX() - idealB.getX()));
        
        Location idealARotated = idealA.rotateXy(angle);
        
        Location offset = actualA.subtract(idealARotated);
        
        return new Location(idealA.getUnits(), offset.getX(), offset.getY(), 0, angle);
    }
}
