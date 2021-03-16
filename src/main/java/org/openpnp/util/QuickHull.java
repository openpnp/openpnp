package org.openpnp.util;

import java.util.ArrayList;
import java.util.List;

import org.openpnp.model.Point;

/*
 * Copyright (c) 2007 Alexander Hristov. http://www.ahristov.com
 * 
 * Feel free to use this code as you wish, as long as you keep this copyright notice. The only
 * limitation on use is that this code cannot be republished on other web sites.
 *
 * As usual, this code comes with no warranties of any kind.
 */
public class QuickHull {
    public static List<Point> quickHull(List<Point> points) {
        if (points.size() < 3) {
            return points;
        }
        
        ArrayList<Point> convexHull = new ArrayList<Point>();
        
        // find extremals
        int minPoint = -1, maxPoint = -1;
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < points.size(); i++) {
            if (points.get(i).x < minX) {
                minX = points.get(i).x;
                minPoint = i;
            }
            if (points.get(i).x > maxX) {
                maxX = points.get(i).x;
                maxPoint = i;
            }
        }
        Point a = points.get(minPoint);
        Point b = points.get(maxPoint);
        convexHull.add(a);
        convexHull.add(b);
        points.remove(a);
        points.remove(b);

        ArrayList<Point> leftSet = new ArrayList<Point>();
        ArrayList<Point> rightSet = new ArrayList<Point>();

        for (int i = 0; i < points.size(); i++) {
            Point p = points.get(i);
            if (pointLocation(a, b, p) == -1) {
                leftSet.add(p);
            }
            else {
                rightSet.add(p);
            }
        }
        hullSet(a, b, rightSet, convexHull);
        hullSet(b, a, leftSet, convexHull);

        return convexHull;
    }

    /*
     * Computes the square of the distance of point C to the segment defined by points AB
     */
    public static double distance(Point a, Point b, Point c) {
        double aBX = b.x - a.x;
        double aBY = b.y - a.y;
        double num = aBX * (a.y - c.y) - aBY * (a.x - c.x);
        if (num < 0) {
            num = -num;
        }
        return num;
    }

    public static void hullSet(Point a, Point b, ArrayList<Point> set, ArrayList<Point> hull) {
        int insertPosition = hull.indexOf(b);
        if (set.size() == 0) {
            return;
        }
        if (set.size() == 1) {
            Point p = set.get(0);
            set.remove(p);
            hull.add(insertPosition, p);
            return;
        }
        double dist = Double.MIN_VALUE;
        int furthestPoint = -1;
        for (int i = 0; i < set.size(); i++) {
            Point p = set.get(i);
            double distance = distance(a, b, p);
            if (distance > dist) {
                dist = distance;
                furthestPoint = i;
            }
        }
        Point p = set.get(furthestPoint);
        set.remove(furthestPoint);
        hull.add(insertPosition, p);

        // Determine who's to the left of AP
        ArrayList<Point> leftSetAP = new ArrayList<Point>();
        for (int i = 0; i < set.size(); i++) {
            Point m = set.get(i);
            if (pointLocation(a, p, m) == 1) {
                leftSetAP.add(m);
            }
        }

        // Determine who's to the left of PB
        ArrayList<Point> leftSetPB = new ArrayList<Point>();
        for (int i = 0; i < set.size(); i++) {
            Point m = set.get(i);
            if (pointLocation(p, b, m) == 1) {
                leftSetPB.add(m);
            }
        }
        hullSet(a, p, leftSetAP, hull);
        hullSet(p, b, leftSetPB, hull);

    }

    private static double pointLocation(Point a, Point b, Point p) {
        double cp1 = (b.x - a.x) * (p.y - a.y) - (b.y - a.y) * (p.x - a.x);
        return (cp1 > 0) ? 1 : -1;
    }
}
