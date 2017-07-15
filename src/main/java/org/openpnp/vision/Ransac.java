package org.openpnp.vision;

import java.util.Collections;
import java.util.List;

import org.opencv.core.Point;

public class Ransac {
    /*
     * http://users.utcluj.ro/~igiosan/Resources/PRS/L1/lab_01e.pdf
     * http://cs.gmu.edu/~kosecka/cs682/lect-fitting.pdf
     * http://introcs.cs.princeton.edu/java/36inheritance/LeastSquares.java.html
     */
    public static Point[] ransac(List<Point> points, int maxIterations, double threshold) {
        Point bestA = null, bestB = null;
        int bestInliers = 0;
        for (int i = 0; i < maxIterations; i++) {
            // take a random sample of two points
            Collections.shuffle(points);
            Point a = points.get(0);
            Point b = points.get(1);
            // find the inliers
            int inliers = 0;
            for (Point p : points) {
                double distance = FluentCv.pointToLineDistance(a, b, p);
                if (distance <= threshold) {
                    inliers++;
                }
            }
            if (inliers > bestInliers) {
                bestA = a;
                bestB = b;
                bestInliers = inliers;
            }
        }
        return new Point[] {bestA, bestB};
    }
}
