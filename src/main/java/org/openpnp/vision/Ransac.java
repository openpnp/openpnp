package org.openpnp.vision;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

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

    // TODO: This currently seems to give much worse results than ransac. Figure out why.
    public static List<RansacLine> multiRansac(List<Point> points, int maxIterations,
            double threshold) {
        Random random = new Random();
        Set<RansacLine> lines = new HashSet<>();
        for (int i = 0; i < maxIterations; i++) {
            // take a random sample of two points
            Point a = points.get(random.nextInt(points.size()));
            Point b = points.get(random.nextInt(points.size()));
            RansacLine line = new RansacLine(a, b, 0);
            // if we have already processed this pair, skip it
            if (lines.contains(line)) {
                continue;
            }
            // add the result
            lines.add(line);
            // find the inliers
            for (Point p : points) {
                double distance = FluentCv.pointToLineDistance(a, b, p);
                if (distance <= threshold) {
                    line.inliers++;
                }
            }
        }
        List<RansacLine> results = new ArrayList<>(lines);
        Collections.sort(results, new Comparator<RansacLine>() {
            @Override
            public int compare(RansacLine o1, RansacLine o2) {
                return o2.inliers - o1.inliers;
            }
        });
        return results;
    }

    public static class RansacLine {
        public Point a;
        public Point b;
        public transient int inliers;

        public RansacLine(Point a, Point b, int inliers) {
            this.a = a;
            this.b = b;
            this.inliers = inliers;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((a == null) ? 0 : a.hashCode());
            result = prime * result + ((b == null) ? 0 : b.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            RansacLine other = (RansacLine) obj;
            if (a == null) {
                if (other.a != null)
                    return false;
            }
            else if (!a.equals(other.a))
                return false;
            if (b == null) {
                if (other.b != null)
                    return false;
            }
            else if (!b.equals(other.b))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "RansacLine [a=" + a + ", b=" + b + ", inliers=" + inliers + "]";
        }
    }
}
