package org.openpnp.vision;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import org.opencv.core.Point;

public class Ransac {
    private static class LineIndices implements Comparable<LineIndices> {
        public List<Integer> indices = new ArrayList<>();

        public LineIndices(List<Integer> indices) {
            this.indices.addAll(indices);
            this.indices.sort(null);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            LineIndices that = (LineIndices) o;

            return indices.equals(that.indices);
        }

        @Override
        public int compareTo(LineIndices o) {
            // NOTE: aIndex and bIndex don't matter for comparisons
            int cmp = Integer.compare(this.indices.size(), o.indices.size());
            if (cmp != 0) {
                return cmp;
            }

            for (int i=0; i<this.indices.size(); i++) {
                Integer a = this.indices.get(i);
                Integer b = o.indices.get(i);
                cmp = a.compareTo(b);
                if (cmp != 0) {
                    return cmp;
                }
            }

            return 0;
        }
    }

    public static class Line {
        public Point a;
        public Point b;

        public Line(Point a, Point b) {
            this.a = a;
            this.b = b;
        }
    }
    /*
     * http://users.utcluj.ro/~igiosan/Resources/PRS/L1/lab_01e.pdf
     * http://cs.gmu.edu/~kosecka/cs682/lect-fitting.pdf
     * http://introcs.cs.princeton.edu/java/36inheritance/LeastSquares.java.html
     */
    public static List<Line> ransac(List<Point> points, int maxIterations, double pointToLineDistanceThreshold) {
        List<Integer> pointMapping = new ArrayList<>(points.size());
        for (int i=0; i<points.size(); i++) {
            pointMapping.add(i);
        }

        List<LineIndices> resultIndices = new ArrayList<>();
        for (int i = 0; i < maxIterations; i++) {
            // take a random sample of two points
            Collections.shuffle(pointMapping);
            Integer aIndex = pointMapping.get(0);
            Integer bIndex = pointMapping.get(1);
            Point a = points.get(aIndex);
            Point b = points.get(bIndex);
            // find the inliers
            List<Integer> inliers = new ArrayList<>();
            for (Integer pIndex : pointMapping) {
                Point p = points.get(pIndex);
                double distance = FluentCv.pointToLineDistance(a, b, p);
                if (distance <= pointToLineDistanceThreshold) {
                    inliers.add(pIndex);
                }
            }
            if (inliers.size() >= 2) {
                // Must check for duplicates as we're just randomly shuffling and testing again; the same line may
                // come up many times, both from the same starting points or from other points on the same line
                LineIndices lineIndices = new LineIndices(inliers);
                if (!resultIndices.contains(lineIndices)) {
                    resultIndices.add(lineIndices);
                }
            }
        }

        // Sort the results by the number of points, descending
        resultIndices.sort(new Comparator<LineIndices>() {
            @Override
            public int compare(LineIndices o1, LineIndices o2) {
                return -Integer.compare(o1.indices.size(), o2.indices.size());
            }
        });

        List<Line> results = new ArrayList<>(resultIndices.size());
        for (LineIndices lineIndices : resultIndices) {
            Line line = getLongestLine(points, lineIndices);
            results.add(line);
        }
        return results;
    }

    public static List<Line> ransac(List<Point> points, int maxIterations, double pointToLineDistanceThreshold, double pointSpacing, double pointSpacingEpsilon) {
        if (points.size() < 2) {
            return new ArrayList<Line>();
        }

        List<Integer> pointMapping = new ArrayList<>(points.size());
        for (int i=0; i<points.size(); i++) {
            pointMapping.add(i);
        }

        List<LineIndices> resultIndices = new ArrayList<>();
        for (int i = 0; i < maxIterations; i++) {
            // take a random sample of two points
            Collections.shuffle(pointMapping);
            Integer aIndex = pointMapping.get(0);
            Integer bIndex = pointMapping.get(1);
            Point a = points.get(aIndex);
            Point b = points.get(bIndex);
            // find the inliers
            List<Integer> inliers = new ArrayList<>();
            for (Integer pIndex : pointMapping) {
                Point p = points.get(pIndex);
                double distance = FluentCv.pointToLineDistance(a, b, p);
                if (distance <= pointToLineDistanceThreshold) {
                    inliers.add(pIndex);
                }
            }
            List<Integer> spacedInliers = filterInliersWithSpacing(a, b, points, inliers, pointSpacing, pointSpacingEpsilon);
            if (spacedInliers.size() >= 2) {
                // Must check for duplicates as we're just randomly shuffling and testing again; the same line may
                // come up many times, both from the same starting points or from other points on the same line
                LineIndices lineIndices = new LineIndices(spacedInliers);
                if (!resultIndices.contains(lineIndices)) {
                    resultIndices.add(lineIndices);
                }
            }
        }

        // Sort the results by the number of points, descending
        resultIndices.sort(new Comparator<LineIndices>() {
            @Override
            public int compare(LineIndices o1, LineIndices o2) {
                return -Integer.compare(o1.indices.size(), o2.indices.size());
            }
        });

        List<Line> results = new ArrayList<>(resultIndices.size());
        for (LineIndices lineIndices : resultIndices) {
            Line line = getLongestLine(points, lineIndices);
            results.add(line);
        }
        return results;
    }

    private static List<Integer> filterInliersWithSpacing(Point firstPoint, Point secondPoint, List<Point> points, List<Integer> inliers, double pointSpacing, double pointSpacingEpsilon) {
        Point lineDir = new Point(secondPoint.x - firstPoint.x, secondPoint.y - firstPoint.y);

        TreeSet<Integer> indicesOnLine = new TreeSet<>();
        List<Integer> spacedInliers = new ArrayList<>(inliers.size());
        for (Integer pIndex : inliers) {
            Point p = points.get(pIndex);

            Point diff = new Point(p.x - firstPoint.x, p.y - firstPoint.y);
            double distance = Math.sqrt(diff.dot(diff));
            double variance = distance % pointSpacing;
            if ((variance <= pointSpacingEpsilon) || ((pointSpacing - variance) <= pointSpacingEpsilon)) {
                double signedDistance = distance * (lineDir.dot(diff) > 0.0 ? 1.0 : -1.0);
                Integer indexOnLine = (int)Math.round(signedDistance / pointSpacing);
                if (!indicesOnLine.contains(indexOnLine)) {
                    indicesOnLine.add(indexOnLine);

                    spacedInliers.add(pIndex);
                }
            }
        }

        // Discard this line if any index is missing
        Integer minIndex = indicesOnLine.first();
        Integer maxIndex = indicesOnLine.last();
        for (Integer idx=minIndex + 1; idx<maxIndex; idx++) {
            if (!indicesOnLine.contains(idx)) {
                return new ArrayList<>();
            }
        }

        return spacedInliers;
    }

    private static Line getLongestLine(List<Point> points, LineIndices lineIndices) {
        Integer bestAIndex = 0;
        Integer bestBIndex = 0;
        double bestDistance = 0.0;
        for (int i=0; i<(lineIndices.indices.size() - 1); i++) {
            for (int j=i+1; j<lineIndices.indices.size(); j++) {
                Integer aIndex = lineIndices.indices.get(i);
                Integer bIndex = lineIndices.indices.get(j);
                Point a = points.get(aIndex);
                Point b = points.get(bIndex);

                Point diff = new Point(b.x - a.x, b.y - a.y);
                double distance = Math.sqrt(diff.dot(diff));
                if (distance > bestDistance) {
                    bestAIndex = aIndex;
                    bestBIndex = bIndex;
                    bestDistance = distance;
                }
            }
        }

        return new Line(points.get(bestAIndex), points.get(bestBIndex));
    }
}
