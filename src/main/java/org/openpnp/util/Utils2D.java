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
 *
 * Changelog: 03/10/2012 Ami: Add rotate using center point
 */

package org.openpnp.util;

import org.openpnp.model.Board.Side;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Length;
import org.openpnp.model.Location;
import org.openpnp.model.Placement;
import org.openpnp.model.Point;


public class Utils2D {
    public static Point rotateTranslateScalePoint(Point point, double c, double x, double y,
            double scaleX, double scaleY) {
        point = rotatePoint(point, c);
        point = translatePoint(point, x, y);
        point = scalePoint(point, scaleX, scaleY);
        return point;
    }

    public static Point rotateTranslateCenterPoint(Point point, double c, double x, double y,
            Point center) {
        point = translatePoint(point, center.getX() * -1, center.getY() * -1);
        point = rotatePoint(point, c);
        point = translatePoint(point, center.getX(), center.getY());
        point = translatePoint(point, x, y);

        return point;
    }

    public static Point translatePoint(Point point, double x, double y) {
        return new Point(point.getX() + x, point.getY() + y);
    }

    /**
     * Rotation is counter-clockwise for positive angles.
     * 
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

    public static Location calculateBoardPlacementLocation(BoardLocation bl,
            Location placementLocation) {
        return calculateBoardPlacementLocation(bl.getLocation(), bl.getSide(),
                bl.getBoard().getDimensions().getX(), placementLocation);
    }

    public static Location calculateBoardPlacementLocation(Location boardLocation, Side side,
            double offset, Location placementLocation) {
        // The Z value of the placementLocation is always ignored, so zero it out to make sure.
        placementLocation = placementLocation.derive(null, null, 0D, null);


        // We will work in the units of the placementLocation, so convert
        // anything that isn't in those units to it.
        boardLocation = boardLocation.convertToUnits(placementLocation.getUnits());

        // If we are placing the bottom of the board we need to invert
        // the placement location.
        if (side == Side.Bottom) {
            placementLocation = placementLocation.invert(true, false, false, false)
                    .add(new Location(placementLocation.getUnits(), offset, 0.0, 0.0, 0.0));
        }

        // Rotate and translate the point into the same coordinate space
        // as the board
        placementLocation = placementLocation.rotateXy(boardLocation.getRotation())
                .addWithRotation(boardLocation);
        return placementLocation;
    }


    public static Location calculateBoardPlacementLocationInverse(BoardLocation boardLocation,
            Location placementLocation) {
        return calculateBoardPlacementLocationInverse(boardLocation.getLocation(),
                boardLocation.getSide(), boardLocation.getBoard().getDimensions().getX(),
                placementLocation);
    }

    public static Location calculateBoardPlacementLocationInverse(Location boardLocation, Side side,
            double offset, Location placementLocation) {
        // inverse steps of calculateBoardPlacementLocation
        boardLocation = boardLocation.convertToUnits(placementLocation.getUnits());
        placementLocation = placementLocation.subtractWithRotation(boardLocation)
                .rotateXy(-boardLocation.getRotation());
        if (side == Side.Bottom) {
            placementLocation = placementLocation.invert(true, false, false, false)
                    .add(new Location(placementLocation.getUnits(), offset, 0.0, 0.0, 0.0));
        }
        // The Z value of the placementLocation is always ignored, so zero it out to make sure.
        placementLocation = placementLocation.derive(null, null, 0D, null);
        return placementLocation;
    }

    /**
     * Given an existing BoardLocation, two Placements and the observed location of those
     * two Placements, calculate the actual Location of the BoardLocation. Note that the
     * BoardLocation is only used to determine which side of the board the Placements
     * are on - it's existing Location is not considered. The returned Location is the
     * absolute Location of the board, including it's angle, with the Z value set to the
     * Z value in the input BoardLocation.
     * @param boardLocation
     * @param placementA
     * @param placementB
     * @param observedLocationA
     * @param observedLocationB
     * @return
     */
    public static Location calculateBoardLocation(BoardLocation boardLocation, Placement placementA,
            Placement placementB, Location observedLocationA, Location observedLocationB) {
        // Create a new BoardLocation based on the input except with a zeroed
        // Location. This will be used to calculate our ideal placement locations.
        BoardLocation bl = new BoardLocation(boardLocation.getBoard());
        bl.setLocation(new Location(boardLocation.getLocation().getUnits()));
        bl.setSide(boardLocation.getSide());

        // Calculate the ideal placement locations. This is where we would expect the
        // placements to be if the board was at 0,0,0,0.
        Location idealA = calculateBoardPlacementLocation(bl, placementA.getLocation());
        Location idealB = calculateBoardPlacementLocation(bl, placementB.getLocation());

        // Just rename a couple variables to make the code easier to read.
        Location actualA = observedLocationA;
        Location actualB = observedLocationB;

        // Make sure all locations are using the same units.
        idealA = idealA.convertToUnits(boardLocation.getLocation().getUnits());
        idealB = idealB.convertToUnits(boardLocation.getLocation().getUnits());
        actualA = actualA.convertToUnits(boardLocation.getLocation().getUnits());
        actualB = actualB.convertToUnits(boardLocation.getLocation().getUnits());

        // Calculate the angle that we expect to see between the two placements
        double idealAngle = Math.toDegrees(
                Math.atan2(idealB.getY() - idealA.getY(), idealB.getX() - idealA.getX()));
        // Now calculate the angle that we observed between the two placements
        double actualAngle = Math.toDegrees(
                Math.atan2(actualB.getY() - actualA.getY(), actualB.getX() - actualA.getX()));

        // The difference in angles is the angle of the board
        double angle = actualAngle - idealAngle;

        // Now we rotate the first placement by the angle, which gives us the location
        // that the placement would be had the board been rotated by that angle.
        Location idealARotated = idealA.rotateXy(angle);

        // And now we subtract that rotated location from the observed location to get
        // the real offset of the board.
        Location location = actualA.subtract(idealARotated);

        // And set the calculated angle and original Z
        location = location.derive(null, null,
                boardLocation.getLocation().convertToUnits(location.getUnits()).getZ(), angle);

        return location;
    }


    public static double normalizeAngle(double angle) {
        while (angle > 360) {
            angle -= 360;
        }
        while (angle < 0) {
            angle += 360;
        }
        return angle;
    }
    
    /**
     * Calculate the Location along the line formed by a and b with distance from a.
     * @param a
     * @param b
     * @param distance
     * @return
     */
    static public Location getPointAlongLine(Location a, Location b, Length distance) {
        b = b.convertToUnits(a.getUnits());
        distance = distance.convertToUnits(a.getUnits());
        
        Point vab = b.subtract(a).getXyPoint();
        double lab = a.getLinearDistanceTo(b);
        Point vu = new Point(vab.x / lab, vab.y / lab);
        vu = new Point(vu.x * distance.getValue(), vu.y * distance.getValue());
        return a.add(new Location(a.getUnits(), vu.x, vu.y, 0, 0));
    }

    static public double getAngleFromPoint(Location firstPoint, Location secondPoint) {
        secondPoint = secondPoint.convertToUnits(firstPoint.getUnits());
        
        double angle = 0.0;
        // above 0 to 180 degrees
        if ((secondPoint.getX() > firstPoint.getX())) {
            angle = (Math.atan2((secondPoint.getX() - firstPoint.getX()),
                    (firstPoint.getY() - secondPoint.getY())) * 180 / Math.PI);
        }
        // above 180 degrees to 360/0
        else if ((secondPoint.getX() <= firstPoint.getX())) {
            angle = 360 - (Math.atan2((firstPoint.getX() - secondPoint.getX()),
                    (firstPoint.getY() - secondPoint.getY())) * 180 / Math.PI);
        }
        return angle;
    }
}
