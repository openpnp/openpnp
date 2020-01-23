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

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.openpnp.model.Board.Side;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
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
    
    /**
     * Creates an AffineTransform for a BoardLocation which handles it's position and
     * rotation. This is used when the BoardLocation does not yet have a transform created
     * by a fiducial check.
     * 
     * @param bl
     * @return
     */
    private static AffineTransform getDefaultBoardPlacementLocationTransform(BoardLocation bl) {
        Location l = bl.getLocation().convertToUnits(LengthUnit.Millimeters);
        AffineTransform tx = new AffineTransform();
        tx.translate(l.getX(), l.getY());
        tx.rotate(Math.toRadians(l.getRotation()));
        if (bl.getSide() == Side.Bottom) {
            /**
             * Translate by the board width. This is used to support the "New" Board Location
             * system ala https://github.com/openpnp/openpnp/wiki/Board-Locations.
             */
            tx.translate(bl.getBoard().getDimensions().convertToUnits(LengthUnit.Millimeters).getX(), 0);
        }
        return tx;
    }
    
    /**
     * Calculate the apparent angle from the transform. We need this because when we
     * created the transform we captured the apparent angle and that is used to position
     * in X, Y, but we also need the actual value to add to the placement rotation so that
     * the nozzle is rotated to the correct angle as well.
     * Note, there is probably a better way to do this. If you know how, please let me know!
     */
    private static double getTransformAngle(AffineTransform tx) {
        Point2D.Double a = new Point2D.Double(0, 0);
        Point2D.Double b = new Point2D.Double(1, 1);
        Point2D.Double c = new Point2D.Double(0, 0);
        Point2D.Double d = new Point2D.Double(1, 1);
        c = (Point2D.Double) tx.transform(c, null);
        d = (Point2D.Double) tx.transform(d, null);
        double angle = Math.toDegrees(Math.atan2(d.y - c.y, d.x - c.x) - Math.atan2(b.y - a.y, b.x - a.x));
        return angle;
    }

    public static Location calculateBoardPlacementLocation(BoardLocation bl,
            Location placementLocation) {
        AffineTransform tx = bl.getPlacementTransform();        
        if (tx == null) {
            tx = getDefaultBoardPlacementLocationTransform(bl);
        }
        
        // The affine calculations are always done in millimeters, so we convert everything
        // before we start calculating and then we'll convert it back to the original
        // units at the end.
        LengthUnit placementUnits = placementLocation.getUnits();
        Location boardLocation = bl.getLocation().convertToUnits(LengthUnit.Millimeters);
        placementLocation = placementLocation.convertToUnits(LengthUnit.Millimeters);

        if (bl.getSide() == Side.Bottom) {
        	placementLocation = placementLocation.invert(true, false, false, false);
        }

        double angle = getTransformAngle(tx);
        
        Point2D p = new Point2D.Double(placementLocation.getX(), placementLocation.getY());
        p = tx.transform(p, null);
        
        // The final result is the transformed X,Y, the BoardLocation's Z, and the
        // transform angle + placement angle.
        Location l = new Location(LengthUnit.Millimeters, 
                p.getX(), 
                p.getY(), 
                boardLocation.getZ(), 
                angle + placementLocation.getRotation());
        l = l.convertToUnits(placementUnits);
        return l;
    }

    public static Location calculateBoardPlacementLocationInverse(BoardLocation bl,
            Location placementLocation) {
        AffineTransform tx = bl.getPlacementTransform();
        if (tx == null) {
            tx = getDefaultBoardPlacementLocationTransform(bl);
        }
        
        try {
            tx = tx.createInverse();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
        // The affine calculations are always done in millimeters, so we convert everything
        // before we start calculating and then we'll convert it back to the original
        // units at the end.
        LengthUnit placementUnits = placementLocation.getUnits();
        placementLocation = placementLocation.convertToUnits(LengthUnit.Millimeters);

        double angle = getTransformAngle(tx);
        
        Point2D p = new Point2D.Double(placementLocation.getX(), placementLocation.getY());
        p = tx.transform(p, null);
        
        // The final result is the transformed X,Y, Z = 0, and the
        // transform angle + placement angle.
        Location l = new Location(LengthUnit.Millimeters, 
                bl.getSide() == Side.Bottom ? -p.getX() : p.getX(), 
                p.getY(), 
                0., 
                angle + placementLocation.getRotation());
        l = l.convertToUnits(placementUnits);
        return l;
    }

    /**
     * Given an existing BoardLocation, two Placements and the observed location of those
     * two Placements, calculate the actual Location of the BoardLocation. Note that the
     * BoardLocation is only used to determine which side of the board the Placements
     * are on - it's existing Location is not considered. The returned Location is the
     * absolute Location of the board, including it's angle, with the Z value set to the
     * Z value in the input BoardLocation.
     * 
     * TODO Replace this with deriveAffineTransform. This is essentially a two fiducial check.
     * 
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
    
    public static double normalizeAngle180(double angle) {
        while (angle >= 180) {
            angle -= 360;
        }
        while (angle < -180) {
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
    public static Location getPointAlongLine(Location a, Location b, Length distance) {
        b = b.convertToUnits(a.getUnits());
        distance = distance.convertToUnits(a.getUnits());
        
        Point vab = b.subtract(a).getXyPoint();
        double lab = a.getLinearDistanceTo(b);
        Point vu = new Point(vab.x / lab, vab.y / lab);
        vu = new Point(vu.x * distance.getValue(), vu.y * distance.getValue());
        return a.add(new Location(a.getUnits(), vu.x, vu.y, 0, 0));
    }

    /**
     * Calculate the angle in degrees between a ray from the first point in the positive x direction to
     * the ray from the first point to the second point.
     * @param firstPoint
     * @param secondPoint
     * @return
     */
    public static double getAngleFromPoint(Location firstPoint, Location secondPoint) {
        secondPoint = secondPoint.convertToUnits(firstPoint.getUnits());
        return Math.toDegrees(Math.atan2(secondPoint.getY() - firstPoint.getY(), secondPoint.getX() - firstPoint.getX()));
    }
    
    public static double distance(Point2D.Double a, Point2D.Double b) {
        return (Math.sqrt(Math.pow(b.x - a.x, 2) + Math.pow(b.y - a.y, 2)));
    }
    
    public static AffineTransform deriveAffineTransform(Location source1, Location source2, Location source3,
            Location dest1, Location dest2, Location dest3) {
        source1 = source1.convertToUnits(LengthUnit.Millimeters);
        source2 = source2.convertToUnits(LengthUnit.Millimeters);
        source3 = source3.convertToUnits(LengthUnit.Millimeters);
        dest1 = dest1.convertToUnits(LengthUnit.Millimeters);
        dest2 = dest2.convertToUnits(LengthUnit.Millimeters);
        dest3 = dest3.convertToUnits(LengthUnit.Millimeters);
        return deriveAffineTransform(source1.getX(), source1.getY(), 
                source2.getX(), source2.getY(),
                source3.getX(), source3.getY(),
                dest1.getX(), dest1.getY(),
                dest2.getX(), dest2.getY(),
                dest3.getX(), dest3.getY());
    }
    
    public static AffineTransform deriveAffineTransform(Location source1, Location source2,
            Location dest1, Location dest2) {
        source1 = source1.convertToUnits(LengthUnit.Millimeters);
        source2 = source2.convertToUnits(LengthUnit.Millimeters);
        dest1 = dest1.convertToUnits(LengthUnit.Millimeters);
        dest2 = dest2.convertToUnits(LengthUnit.Millimeters);
        return deriveAffineTransform(source1.getX(), source1.getY(), 
                source2.getX(), source2.getY(),
                dest1.getX(), dest1.getY(),
                dest2.getX(), dest2.getY());
    }
    
    // https://stackoverflow.com/questions/21270892/generate-affinetransform-from-3-points
    public static AffineTransform deriveAffineTransform(
            double sourceX1, double sourceY1,
            double sourceX2, double sourceY2,
            double sourceX3, double sourceY3,
            double destX1, double destY1,
            double destX2, double destY2,
            double destX3, double destY3) {
        RealMatrix source = MatrixUtils.createRealMatrix(new double[][] {
            {sourceX1, sourceX2, sourceX3}, 
            {sourceY1, sourceY2, sourceY3}, 
            {1, 1, 1}
        });

        RealMatrix dest = MatrixUtils.createRealMatrix(new double[][] { 
            {destX1, destX2, destX3}, 
            {destY1, destY2, destY3} 
        });

        RealMatrix inverse = new LUDecomposition(source).getSolver().getInverse();
        RealMatrix transform = dest.multiply(inverse);

        double m00 = transform.getEntry(0, 0);
        double m01 = transform.getEntry(0, 1);
        double m02 = transform.getEntry(0, 2);
        double m10 = transform.getEntry(1, 0);
        double m11 = transform.getEntry(1, 1);
        double m12 = transform.getEntry(1, 2);

        return new AffineTransform(m00, m10, m01, m11, m02, m12);       
    }  
    
    // Best keywords: transformation matrix between two line segments
    // https://stackoverflow.com/questions/42328398/transformation-matrix-between-two-line-segments
    public static AffineTransform deriveAffineTransform(
            double sourceX1, double sourceY1,
            double sourceX2, double sourceY2,
            double destX1, double destY1,
            double destX2, double destY2) {
        Point2D.Double a = new Point2D.Double(sourceX1, sourceY1);
        Point2D.Double b = new Point2D.Double(sourceX2, sourceY2);
        Point2D.Double c = new Point2D.Double(destX1, destY1);
        Point2D.Double d = new Point2D.Double(destX2, destY2);
        
        double len_ab = distance(a, b);
        double len_cd = distance(c, d);
        double scale = len_cd / len_ab;
        
        double r = Math.atan2(d.y - c.y, d.x - c.x) - Math.atan2(b.y - a.y, b.x - a.x);
        
        AffineTransform tx = new AffineTransform();
        tx.translate(c.x, c.y);
        tx.rotate(r);
        tx.scale(scale, scale);
        tx.translate(-a.x, -a.y);
        return tx;
    }  
    
    /**
     * Calculate the area of a triangle. Returns 0 if the triangle is degenerate.
     * @param p1
     * @param p2
     * @param p3
     * @return
     */
    public static double triangleArea(Placement p1, Placement p2, Placement p3) {
        double a = p1.getLocation().getLinearDistanceTo(p2.getLocation());
        double b = p2.getLocation().getLinearDistanceTo(p3.getLocation());
        double c = p3.getLocation().getLinearDistanceTo(p1.getLocation());
        double s = (a + b + c) / 2.;
        return Math.sqrt(s * (s - a) * (s - b) * (s - c));
    }
    
    public static List<Placement> mostDistantPair(List<Placement> points) {
        Placement maxA = null, maxB = null;
        double max = 0;
        for (Placement a : points) {
            for (Placement b : points) {
                if (a == b) {
                    continue;
                }
                double d = a.getLocation().getLinearDistanceTo(b.getLocation());
                if (d > max) {
                    maxA = a;
                    maxB = b;
                    max = d;
                }
            }
        }
        ArrayList<Placement> results = new ArrayList<>();
        results.add(maxA);
        results.add(maxB);
        return results;
    }
}
