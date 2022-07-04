/*
 * Copyright (C) 2022 <mark@makr.zone>
 * inspired and based on work
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeSet;

import org.opencv.core.RotatedRect;
import org.openpnp.model.Footprint.Pad;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.openpnp.util.NanosecondTime;
import org.openpnp.util.TravellingSalesman;
import org.openpnp.util.Utils2D;
import org.openpnp.util.VisionUtils;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

/**
 * A Footprint is a group of SMD pads along with length unit information. Footprints can be rendered
 * to a Shape for easy display using 2D primitives.
 */
public class VisionCompositing extends AbstractModelObject{
    public enum CompositingMethod {
        None,
        Restricted,
        Automatic,
        SingleCorners;

        public boolean isEnforced() {
            return this == Automatic || this == SingleCorners;
        }
    }

    public enum CompositingSolution {
        Square,
        Box,
        Z,
        Arrow,
        Figure7,
        Angle,
        Trapezoid, 
        Small, 
        VisionOffsets,
        NoFootprint,
        NoCameraRoaming,
        RestrictedCameraRoaming,
        Invalid;

        public boolean isAdvanced() {
            return ordinal() < Small.ordinal();
        }
        public boolean isInvalid() {
            return ordinal() >= NoCameraRoaming.ordinal();
        }
    }

    public enum ShotConfiguration {
        Square,
        Box,
        MirrorX,
        MirrorY,
        Corner,
        Unknown;

        boolean isDiagonal() {
            return this == Square || this == Box;
        }
    }

    @Attribute
    private CompositingMethod compositingMethod = CompositingMethod.Restricted;

    @Element(required = false)
    private Length maxPickTolerance = new Length(0, LengthUnit.Millimeters);

    @Attribute(required = false)
    private double minLeverageFactor = 0.2; 

    @Attribute(required = false)
    private int extraShots = 0;

    @Attribute(required = false)
    private boolean allowInside = true;

    private static final double minCameraRadiusOverlap = 0.02; // 2% overlap, ~7 pixels at 720p/2

    private static final double eps = 1e-5;

    public CompositingMethod getCompositingMethod() {
        return compositingMethod;
    }

    public void setCompositingMethod(CompositingMethod compositingMethod) {
        Object oldValue = this.compositingMethod;
        this.compositingMethod = compositingMethod;
        firePropertyChange("compositingMethod", oldValue, compositingMethod);
    }

    public Length getMaxPickTolerance() {
        return maxPickTolerance;
    }

    public void setMaxPickTolerance(Length maxPickTolerance) {
        Object oldValue = this.maxPickTolerance;
        this.maxPickTolerance = maxPickTolerance;
        firePropertyChange("maxPickTolerance", oldValue, maxPickTolerance);
    }

    public double getMinLeverageFactor() {
        return minLeverageFactor;
    }

    public void setMinLeverageFactor(double minLeverageFactor) {
        Object oldValue = this.minLeverageFactor;
        this.minLeverageFactor = minLeverageFactor;
        firePropertyChange("minLeverageFactor", oldValue, minLeverageFactor);
    }

    public boolean isAllowInside() {
        return allowInside;
    }

    public void setAllowInside(boolean allowInside) {
        Object oldValue = this.allowInside;
        this.allowInside = allowInside;
        firePropertyChange("allowInside", oldValue, allowInside);
    }

    public int getExtraShots() {
        return extraShots;
    }

    public void setExtraShots(int extraShots) {
        Object oldValue = this.extraShots;
        this.extraShots = extraShots;
        firePropertyChange("extraShots", oldValue, extraShots);
    }

    public Camera getCamera() {
        try {
            return VisionUtils.getBottomVisionCamera();
        }
        catch (Exception e) {
        }
        return null;
    }

    public static class Shot {
        private ArrayList<Corner> corners;
        private double x;
        private double y;
        private double width;
        private double height;
        private double minMaskRadius;
        private double maxMaskRadius;
        private boolean optional;
        ShotConfiguration configuration;

        public Shot(ArrayList<Corner> corners, 
                double x, double y, 
                double width, double height,
                double minMaskRadius, double maxMaskRadius, boolean optional, 
                ShotConfiguration configuration) {
            super();
            this.corners = corners;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.minMaskRadius = minMaskRadius;
            this.maxMaskRadius = maxMaskRadius;
            this.optional = optional;
            this.configuration = configuration;
        }

        public Shot(ArrayList<Corner> corners,double minMaskRadius, double maxMaskRadius,
                double tolerance, ShotConfiguration configuration) {
            super();
            this.corners = corners;
            double x0 = 0;
            double y0 = 0;
            double x1 = 0;
            double y1 = 0;
            double x = 0;
            double y = 0;
            int n = 0;
            boolean optional = true;
            for (Corner corner : corners) {
                x0 = Math.min(x0,  corner.getX());
                x1 = Math.max(x1,  corner.getX());
                y0 = Math.min(y0,  corner.getY());
                y1 = Math.max(y1,  corner.getY());
                x += corner.getX();  
                y += corner.getY();
                n++;
                if (!corner.isOptional()) {
                    optional = false;
                }
            }
            this.x = x/n;
            this.y = y/n;
            this.width = x1 - x0 + tolerance*2;
            this.height= y1 - y0 + tolerance*2;
            this.minMaskRadius = minMaskRadius;
            this.maxMaskRadius = maxMaskRadius;
            this.optional = optional;
            this.configuration = configuration;
        }

        public List<Corner> getCorners() {
            if (corners == null) {
                corners = new ArrayList<>();
            }
            return Collections.unmodifiableList(corners);
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getWidth() {
            return width;
        }

        public double getHeight() {
            return height;
        }

        public double getMinMaskRadius() {
            return minMaskRadius;
        }

        public double getMaxMaskRadius() {
            return maxMaskRadius;
        }

        public boolean isOptional() {
            return optional;
        }

        public ShotConfiguration getConfiguration() {
            return configuration;
        }

        private boolean hasEdge(int num) {
            if (corners == null) {
                return true;
            }
            for (Corner corner : corners) {
                if (num == 0 && corner.getXSign() < 0) {
                    return true;
                }
                if (num == 1 && corner.getXSign() > 0) {
                    return true;
                }
                if (num == 2 && corner.getYSign() > 0) {
                    return true;
                }
                if (num == 3 && corner.getYSign() < 0) {
                    return true;
                }
            }
            return false;
        }

        public boolean hasLeftEdge() {
            return hasEdge(0);
        }
        public boolean hasRightEdge() {
            return hasEdge(1);
        }
        public boolean hasTopEdge() {
            return hasEdge(2);
        }
        public boolean hasBottomEdge() {
            return hasEdge(3);
        }
    }

    public static class Corner implements Comparable<Corner> {
        private double x;
        private double y;
        private double minMaskRadius;
        private double maxMaskRadius;
        private int xSign;
        private int ySign;
        private boolean optional;

        private int rating;
        private boolean square;
        private CompositingSolution compositeSolution;

        private Corner diagonalBuddy;
        private Corner xAlignedBuddy;
        private Corner yAlignedBuddy;
        private Corner xSymmetricBuddy;
        private Corner ySymmetricBuddy;
        private Corner xMirrorBuddy;
        private Corner yMirrorBuddy;

        public Corner(double x, double y, 
                double minMaskRadius, double maxMaskRadius,
                int xSign, int ySign) {
            this.x = x;
            this.y = y;
            this.minMaskRadius = minMaskRadius;
            this.maxMaskRadius = maxMaskRadius;
            this.xSign = xSign;
            this.ySign = ySign;
        }

        public int getRating() {
            return rating;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getMinMaskRadius() {
            return minMaskRadius;
        }

        public double getMaxMaskRadius() {
            return maxMaskRadius;
        }

        public int getXSign() {
            return xSign;
        }

        public int getYSign() {
            return ySign;
        }

        public boolean isOptional() {
            return optional;
        }

        public CompositingSolution getCompositeSolution() {
            return compositeSolution;
        }

        public ArrayList<Corner> computeBuddies(ArrayList<Corner> corners, double minAlignDistance, double minSymmetryDistance, int pass) {
            for (Corner corner2 : corners) {
                if (this != corner2) {
                    boolean xSymmetric = (getXSign() == -corner2.getXSign()
                            && Math.abs(getX() + corner2.getX()) < eps
                            && Math.abs(getX() - corner2.getX()) > minSymmetryDistance);
                    boolean ySymmetric = (getYSign() == -corner2.getYSign()
                            && Math.abs(getY() + corner2.getY()) < eps
                            && Math.abs(getY() - corner2.getY()) > minSymmetryDistance);
                    boolean xAligned = (getXSign() == corner2.getXSign()
                            && Math.abs(getX() - corner2.getX()) < eps
                            && Math.abs(getY() - corner2.getY()) > minAlignDistance);
                    boolean yAligned = (getYSign() == corner2.getYSign()
                            && Math.abs(getY() - corner2.getY()) < eps
                            && Math.abs(getX() - corner2.getX()) > minAlignDistance);
                    boolean square = (xSymmetric && ySymmetric 
                            && Math.abs(Math.abs(getX()) - Math.abs(getY())) < eps);
                    if (pass > 0) {
                        if (xSymmetric && corner2.yMirrorBuddy != null){
                            if (xSymmetricBuddy == null || 
                                    getYDistance(xSymmetricBuddy.yMirrorBuddy) < getYDistance(corner2.yMirrorBuddy)) {
                                xSymmetricBuddy = corner2;
                            }
                        }
                        if (ySymmetric && corner2.xMirrorBuddy != null){
                            if (ySymmetricBuddy == null || 
                                    getXDistance(ySymmetricBuddy.xMirrorBuddy) < getXDistance(corner2.xMirrorBuddy)) {
                                ySymmetricBuddy = corner2;
                            }
                        }
                    }
                    if (xAligned){
                        if (xAlignedBuddy == null || 
                                getYDistance(xAlignedBuddy) < getYDistance(corner2)) {
                            xAlignedBuddy = corner2;
                        }
                    }
                    if (yAligned){
                        if (yAlignedBuddy == null || 
                                getXDistance(yAlignedBuddy) < getXDistance(corner2)) {
                            yAlignedBuddy = corner2;
                        }
                    }
                    if (xSymmetric && ySymmetric) {
                        diagonalBuddy = corner2;
                        if (square) {
                            this.square = true;
                        }
                    }
                    if (xSymmetric && yAligned) {
                        xMirrorBuddy = corner2;
                    }
                    if (ySymmetric && xAligned) {
                        yMirrorBuddy = corner2;
                    }
                }
            }
            ArrayList<Corner> group = null;
            if (pass > 0) {
                group = computeCornerSolution();
            }
            return group;
        }

        boolean pairsInOneShotWith(Corner corner2) {
            // To pair in one shot, the signs must be outwards.
            if (corner2 == null) {
                return false;
            }
            if (Math.abs(getX() - corner2.getX()) > eps ) { 
                if (getXSign() == corner2.getXSign()
                        || getXSign()*(getX() - corner2.getX()) < eps) {
                    return false;
                }
            }
            else {
                if (getXSign() != corner2.getXSign()) {
                    return false;
                }
            }
            if (Math.abs(getY() - corner2.getY()) > eps ) { 
                if (getYSign() == corner2.getYSign()
                        || getYSign()*(getY() - corner2.getY()) < eps) {
                    return false;
                }
            }
            else {
                if (getYSign() != corner2.getYSign()) {
                    return false;
                }
            }
            return true;
        }

        private ArrayList<Corner> computeCornerSolution() {
            ArrayList<Corner> solution = null;
            rating = 0;
            int minCorners = 0;
            if (square) {
                // Gives us both symmetric position and symmetric angle with just two corners.
                minCorners = 2;
                rating = 7;
                solution = new ArrayList<>();
                solution.add(this);
                solution.add(diagonalBuddy);
                if (xMirrorBuddy != null) {
                    solution.add(xMirrorBuddy);
                    rating++;
                }
                if (yMirrorBuddy != null) {
                    solution.add(yMirrorBuddy);
                    rating++;
                }
                compositeSolution = CompositingSolution.Square;
            }
            else if (diagonalBuddy != null && xMirrorBuddy != null && yMirrorBuddy != null) {
                // Gives us symmetric position and asymmetric angle with three corners, non-square box and X configuration.
                minCorners = 3;
                rating = 6;
                solution = new ArrayList<>();
                solution.add(this);
                solution.add(diagonalBuddy);
                if (getXDistance(xMirrorBuddy) > getYDistance(yMirrorBuddy) - eps) {
                    solution.add(xMirrorBuddy);
                    solution.add(yMirrorBuddy);
                }
                else {
                    solution.add(yMirrorBuddy);
                    solution.add(xMirrorBuddy);
                }
                compositeSolution = CompositingSolution.Box;
            }
            else if (diagonalBuddy != null && (xAlignedBuddy != null || yAlignedBuddy != null)) {
                // Gives us symmetric position and asymmetric angle with three corners.
                minCorners = 3;
                rating = 4;
                solution = new ArrayList<>();
                solution.add(this);
                solution.add(diagonalBuddy);
                if (yAlignedBuddy == null) {
                    solution.add(xAlignedBuddy);
                    if (diagonalBuddy.xAlignedBuddy != null) {
                        // Z configuration
                        solution.add(diagonalBuddy.xAlignedBuddy);
                        rating++;
                        compositeSolution = CompositingSolution.Z;
                    }
                    else {
                        compositeSolution = CompositingSolution.Figure7;
                    }
                }
                else if (xAlignedBuddy == null) {
                    solution.add(yAlignedBuddy);
                    if (diagonalBuddy.yAlignedBuddy != null) {
                        // Z configuration
                        solution.add(diagonalBuddy.yAlignedBuddy);
                        rating++;
                        compositeSolution = CompositingSolution.Z;
                    }
                    else {
                        compositeSolution = CompositingSolution.Figure7;
                    }
                }
                else if (getYDistance(xAlignedBuddy) > getXDistance(yAlignedBuddy) - eps) {
                    solution.add(xAlignedBuddy);
                    rating++;
                    if (diagonalBuddy.xAlignedBuddy != null
                            && getXDistance(yAlignedBuddy) - eps < diagonalBuddy.getYDistance(diagonalBuddy.xAlignedBuddy) - eps) {
                        // Z configuration
                        solution.add(diagonalBuddy.xAlignedBuddy);
                        compositeSolution = CompositingSolution.Z;
                    }
                    else {
                        solution.add(yAlignedBuddy);
                        compositeSolution = CompositingSolution.Arrow;
                    }
                }
                else {
                    solution.add(yAlignedBuddy);
                    rating++;
                    if (diagonalBuddy.yAlignedBuddy != null
                            && getXDistance(xAlignedBuddy) - eps < diagonalBuddy.getXDistance(diagonalBuddy.yAlignedBuddy) - eps) {
                        // Z configuration
                        solution.add(diagonalBuddy.yAlignedBuddy);
                        compositeSolution = CompositingSolution.Z;
                    }
                    else {
                        solution.add(xAlignedBuddy);
                        compositeSolution = CompositingSolution.Arrow;
                    }
                }
            }
            else if (xMirrorBuddy != null && yMirrorBuddy != null) {
                // Gives us asymmetric position and asymmetric angle with three corners.
                minCorners = 3;
                rating = 3;
                solution = new ArrayList<>();
                solution.add(this);
                solution.add(xMirrorBuddy);
                solution.add(yMirrorBuddy);
                compositeSolution = CompositingSolution.Angle;
            }
            else if (xMirrorBuddy != null && ySymmetricBuddy != null) {
                // Gives us trapezoidal position and angle with four corners.
                minCorners = 4;
                rating = 2;
                solution = new ArrayList<>();
                solution.add(this);
                solution.add(xMirrorBuddy);
                solution.add(ySymmetricBuddy);
                solution.add(ySymmetricBuddy.xMirrorBuddy);
                compositeSolution = CompositingSolution.Trapezoid;
            }
            else if (yMirrorBuddy != null && xSymmetricBuddy != null) {
                // Gives us trapezoidal position and angle with four corners.
                minCorners = 4;
                rating = 2;
                solution = new ArrayList<>();
                solution.add(this);
                solution.add(yMirrorBuddy);
                solution.add(xSymmetricBuddy);
                solution.add(xSymmetricBuddy.yMirrorBuddy);
                compositeSolution = CompositingSolution.Trapezoid;
            }
            if (solution != null) { 
                int i = 0;
                for (Corner corner : solution) {
                    corner.optional = (++i > minCorners);
                }
            }
            return solution;
        }

        private double getXDistance(Corner shot2) {
            return Math.abs(x - shot2.x);
        }

        private double getYDistance(Corner shot2) {
            return Math.abs(y - shot2.y);
        }

        @Override
        public int compareTo(Corner corner2) {
            // By rating, reversed.
            int diff = getRating() - corner2.getRating();
            if (diff != 0) { 
                return -diff;
            }
            // By distance from center, reversed.
            double d1 = Math.hypot(getX(), getY());
            double d2 = Math.hypot(corner2.getX(), corner2.getY());
            if (Math.abs(d1 - d2) > eps) {
                return d1 > d2 ? -1 : 1;
            }
            // By angle from pin1 (rotated 270°), counter-clockwise.
            double a1 = Math.atan2(getX(), -getY());
            double a2 = Math.atan2(corner2.getX(), -corner2.getY());
            return Double.compare(a1, a2);
        }
    }

    public class Composite { 
        // Basic parameters.
        final private Package pkg;
        final private BottomVisionSettings visionSettings;
        final private Footprint footprint;
        final private LengthUnit units;
        final private Camera camera; 
        private Location upp;
        final private Nozzle nozzle;
        final private NozzleTip nozzleTip;
        final private Location locationAndRotation;

        // Computation results
        private double tolerance;
        private double cameraViewRadius;
        private double expectedAngle;
        private TreeSet<Double> leftEdges;
        private TreeSet<Double> rightEdges;
        private TreeSet<Double> topEdges;
        private TreeSet<Double> bottomEdges;
        private ArrayList<Shot> compositeShots;
        private CompositingSolution compositingSolution;
        private List<Shot> shotsTravel;

        // Gathered pipeline results.
        private HashMap<Corner, Point> cornerMap = new HashMap<>();

        // Final detection results.
        private Point detectedCenter;
        private double detectedAngle;
        private Point detectedScale;
        private Point detectedSize;
        private RotatedRect detectedRotatedRect;
        private double maxCornerRadius;
        private ArrayList<Footprint.Pad> rectifiedPads;
        private int outOfRoamingCandidates;
        private double computeTime;

        public Composite(org.openpnp.model.Package pkg, BottomVisionSettings visionSettings,
                Nozzle nozzle, NozzleTip nozzleTip, Camera camera, Location locationAndRotation) throws Exception {
            this.pkg = pkg;
            this.visionSettings = visionSettings;
            this.footprint = pkg.getFootprint();
            this.units = footprint.getUnits();
            this.camera = camera;
            this.nozzle = nozzle;
            this.nozzleTip = nozzleTip;
            this.locationAndRotation = locationAndRotation.convertToUnits(units);
            this.expectedAngle = locationAndRotation.getRotation();
            double t0 = NanosecondTime.getRuntimeSeconds();
            // Compute the solution.
            compute();
            this.computeTime = NanosecondTime.getRuntimeSeconds() - t0;
        }

        public Package getPackage() {
            return pkg;
        }

        public Footprint getFootprint() {
            return footprint;
        }

        public LengthUnit getUnits() {
            return units;
        }

        public Camera getCamera() {
            return camera;
        }

        public Nozzle getNozzle() {
            return nozzle;
        }

        protected NozzleTip getNozzleTip() {
            return nozzleTip;
        }

        public double getComputeTime() {
            return computeTime;
        }

        public ArrayList<Shot> getCompositeShots() {
            return compositeShots;
        }

        public CompositingSolution getCompositingSolution() {
            return compositingSolution;
        }

        public VisionCompositing getParent() {
            return VisionCompositing.this;
        }

        public Location getLocationAndRotation() {
            return locationAndRotation;
        }

        public List<Footprint.Pad> getRectifiedPads() {
            if (rectifiedPads == null) {
                return null;
            }
            return Collections.unmodifiableList(rectifiedPads);
        }

        public List<Shot> getShotsTravel() {
            prepareTravel();
            return shotsTravel;
        }

        public double getTolerance() {
            return tolerance;
        }

        public double getExpectedAngle() {
            return expectedAngle;
        }

        public double getMaxCornerRadius() {
            return maxCornerRadius;
        }

        public Point getDetectedCenter() {
            return detectedCenter;
        }

        public double getDetectedAngle() {
            return detectedAngle;
        }

        public Point getDetectedScale() {
            return detectedScale;
        }

        public Point getDetectedSize() {
            return detectedSize;
        }

        public RotatedRect getDetectedRotatedRect() {
            return detectedRotatedRect;
        }
        private final double invHypot = 1/Math.hypot(1, 1); 
        private final double[] xOctogonalHullSign = new double[] { -invHypot,  0, +invHypot, -1, +1, -invHypot,  0, +invHypot };
        private final double[] yOctogonalHullSign = new double[] { -invHypot, -1, -invHypot,  0,  0, +invHypot, +1, +invHypot };
        
        private double octogonalHull[] = new double[8]; 

        /**
         * Compute the needed compositing solution for the given package footprint. 
         * 
         * @param pkg
         * @return
         * @throws Exception
         */
        public void compute() throws Exception {
            // Compute some lengths.
            tolerance = maxPickTolerance.convertToUnits(units).getValue();
            if (tolerance <= 0) {
                tolerance = nozzleTip
                        .getMaxPickTolerance()
                        .convertToUnits(units).getValue();
            }
            upp = camera.getUnitsPerPixel().convertToUnits(units);
            cameraViewRadius = Math.min(camera.getWidth()*upp.getX(), camera.getHeight()*upp.getY()) / 2; 
            double maxPartDiameter = nozzleTip.getMaxPartDiameter().convertToUnits(units).getValue();
            if (footprint.getPads().isEmpty() 
                    || visionSettings.getVisionOffset().isInitialized()
                    || !camera.getRoamingRadius().isInitialized()) {
                // No footprint, or vision offsets present, or no roaming radius set.
                // Resort to classic single shot.
                compositeShots = new ArrayList<>();
                compositeShots.add(new Shot(null, 0, 0, 
                        maxPartDiameter, maxPartDiameter, maxPartDiameter/2, maxPartDiameter/2, false, ShotConfiguration.Unknown));
                if (visionSettings.getVisionOffset().isInitialized()) {
                    compositingSolution = CompositingSolution.VisionOffsets;
                }
                else if (!camera.getRoamingRadius().isInitialized()) {
                    compositingSolution = CompositingSolution.NoCameraRoaming;
                }
                else {
                    compositingSolution = CompositingSolution.NoFootprint;
                }
                return;
            }
            // Add the body to the octogonal hull.
            Pad body = new Pad();
            body.setWidth(footprint.getBodyWidth());
            body.setHeight(footprint.getBodyHeight());
            addPadToOctogalHull(body);
            // Rectify and fuse pads.
            // As a heuristic, we assume pads are ordered in lines. If not, it will be less performant bus still ok.  
            ArrayList<Footprint.Pad> rectPads = new ArrayList<>();
            for (Footprint.Pad pad : footprint.getPads()) {
                if (Math.abs(pad.getRotation() % 90) > eps) {
                    if (compositingMethod.isEnforced()) { 
                        throw new Exception("Package "+pkg.getId()+" pad "+pad.getName()+" not at 90° step angle.");
                    }
                }
                Pad rectifiedPad = pad.boundingBox();
                rectPads.add(rectifiedPad);
            }
            // Call it twice for column/row fusing in BGAs.
            ArrayList<Footprint.Pad> fusedPads = rectPads;
            fusedPads = fusedPads(fusedPads);
            fusedPads = fusedPads(fusedPads);
            rectifiedPads = fusedPads;
            // Calculate the octogonal hull.
            for (Footprint.Pad pad : rectifiedPads) {
                addPadToOctogalHull(pad);
            }

            // Create the pad edges.
            leftEdges = new TreeSet<>();
            rightEdges = new TreeSet<>();
            topEdges = new TreeSet<>();
            bottomEdges = new TreeSet<>();
            for (Footprint.Pad pad : rectifiedPads) {
                if (allowInside || pad.getX() - pad.getWidth()/2 < 0) {
                    addCoordinate(leftEdges, pad.getX() - pad.getWidth()/2);
                }
                if (allowInside || pad.getY() - pad.getHeight()/2 < 0) {
                    addCoordinate(bottomEdges, pad.getY() - pad.getHeight()/2);
                }
                if (allowInside || pad.getX() + pad.getWidth()/2 > 0) {
                    addCoordinate(rightEdges, pad.getX() + pad.getWidth()/2);
                }
                if (allowInside || pad.getY() + pad.getHeight()/2 > 0) {
                    addCoordinate(topEdges, pad.getY() + pad.getHeight()/2);
                }
            }
            double overallWidth = rightEdges.last() - leftEdges.first();
            double overallHeight = topEdges.last() - bottomEdges.first();
            double overallDiagonal = Math.hypot(overallWidth, overallHeight);
            boolean isSymmetric = Math.abs(rightEdges.last() + leftEdges.first()) < eps
                    && Math.abs(topEdges.last() + bottomEdges.first()) < eps;

            // By combining X, Y edges, find eligible corners, including those out in the "air".
            ArrayList<Corner> corners = new ArrayList<>();
            findEligibleCorners(leftEdges,  bottomEdges, -1, -1, corners);
            findEligibleCorners(leftEdges,  topEdges,    -1, +1, corners);
            findEligibleCorners(rightEdges, bottomEdges, +1, -1, corners);
            findEligibleCorners(rightEdges, topEdges,    +1, +1, corners);

            // Compute the corner buddy solutions.
            ArrayList<Corner> cornerSolution = null;
            double minLeverage = Math.min(overallWidth, overallHeight)*minLeverageFactor;
            int count = computeBuddies(minLeverage, corners);
            CompositingSolution compositeSolution = CompositingSolution.Small;
            if (count > 0) {
                // Sort by rating etc.
                Collections.sort(corners);
                // Store the best solution.
                Corner mainCorner = corners.get(0);
                cornerSolution = mainCorner.computeCornerSolution();
                compositeSolution = mainCorner.getCompositeSolution();
            }
            // Start composing results.
            compositeShots = new ArrayList<>();
            double minRadius = overallDiagonal/2 + tolerance;
            double cameraRoamingRadius = camera.getRoamingRadius().convertToUnits(units).getValue()/2;
            if (compositingMethod == CompositingMethod.None 
                    || (isSymmetric && minRadius < cameraViewRadius && !compositingMethod.isEnforced())) {
                // Whole footprint fits in camera.
                compositeShots.add(new Shot(cornerSolution, 0, 0, 
                        overallWidth+2*tolerance, overallHeight+2*tolerance, 
                        overallDiagonal+2*tolerance, overallDiagonal+2*tolerance,
                        false, ShotConfiguration.Unknown));
                compositeSolution = (minRadius < cameraViewRadius ? 
                        CompositingSolution.Small : CompositingSolution.RestrictedCameraRoaming);
            }
            else if (cornerSolution != null){
                // Compose shots from corners.
                composeShots(cornerSolution);
            }
            if (compositeShots.size() == 0) {
                // No solution, resort to single shot, but mark it invalid
                compositeShots.add(new Shot(corners, 0, 0, 
                        overallWidth+2*tolerance, overallHeight+2*tolerance, 
                        overallDiagonal+2*tolerance, overallDiagonal+2*tolerance,
                        false, ShotConfiguration.Unknown));
                if (outOfRoamingCandidates > 0) {
                    compositeSolution = CompositingSolution.RestrictedCameraRoaming;
                }
                else {
                    compositeSolution = CompositingSolution.Invalid;
                }
            }
            this.compositingSolution = compositeSolution;
        }

        /**
         * Fuses the pads that are very close together (tolerance). This greatly reduces the computing cost.
         * 
         * @param pads
         * @return
         */
        protected ArrayList<Footprint.Pad> fusedPads(ArrayList<Footprint.Pad> pads) {
            ArrayList<Footprint.Pad> fusedPads = new ArrayList<>();
            Footprint.Pad runningPad = null;
            for (Footprint.Pad pad : pads) {
                if (runningPad != null) {
                    Footprint.Pad fusedPad = null;
                    if (Math.abs(runningPad.getY() - pad.getY()) < eps && Math.abs(runningPad.getHeight() - pad.getHeight()) < eps) {
                        if (Math.abs(runningPad.getX() - pad.getX()) < runningPad.getWidth()/2  + pad.getWidth()/2 + tolerance) {
                            // fuse left-right
                            fusedPad = new Footprint.Pad();
                            double x0 = Math.min(runningPad.getX() - runningPad.getWidth()/2, pad.getX() - pad.getWidth()/2);
                            double x1 = Math.max(runningPad.getX() + runningPad.getWidth()/2, pad.getX() + pad.getWidth()/2);
                            fusedPad.setX((x0 + x1)/2);
                            fusedPad.setWidth(x1 - x0);
                            fusedPad.setY(runningPad.getY());
                            fusedPad.setHeight(runningPad.getHeight());
                        }
                    }
                    else if (Math.abs(runningPad.getX() - pad.getX()) < eps && Math.abs(runningPad.getWidth() - pad.getWidth()) < eps) {
                        if (Math.abs(runningPad.getY() - pad.getY()) < runningPad.getHeight()/2  + pad.getHeight()/2 + tolerance) {
                            // fuse top-bottom
                            fusedPad = new Footprint.Pad();
                            double y0 = Math.min(runningPad.getY() - runningPad.getHeight()/2, pad.getY() - pad.getHeight()/2);
                            double y1 = Math.max(runningPad.getY() + runningPad.getHeight()/2, pad.getY() + pad.getHeight()/2);
                            fusedPad.setX(runningPad.getX());
                            fusedPad.setWidth(runningPad.getWidth());
                            fusedPad.setY((y0 + y1)/2);
                            fusedPad.setHeight(y1 - y0);
                        }
                    }
                    if (fusedPad == null) {
                        fusedPads.add(runningPad);
                        runningPad = pad;
                    }
                    else {
                        runningPad = fusedPad;
                    }
                }
                else {
                    runningPad = pad;
                }
            }
            if (runningPad != null) {
                fusedPads.add(runningPad);
            }
            return fusedPads;
        }

        /**
         * @param pad
         */
        protected void addPadToOctogalHull(Footprint.Pad pad) {
            for (int i = 0; i < 8; i++) {
                double h = pad.getX()*xOctogonalHullSign[i] + pad.getWidth()*Math.abs(xOctogonalHullSign[i])*0.5
                         + pad.getY()*yOctogonalHullSign[i] + pad.getHeight()*Math.abs(yOctogonalHullSign[i])*0.5;
                if (octogonalHull[i] < h) {
                    octogonalHull[i] = h;
                }
            }
        }

        private void composeShots(ArrayList<Corner> solution) {
            maxCornerRadius = 0; 
            for (Corner corner : solution) {
                double x = corner.getX();
                double y = corner.getY();
                double r = Math.hypot(x, y);
                if (maxCornerRadius < r) {
                    maxCornerRadius = r;
                }
            }
            while (solution.size() > 0) {
                Corner corner = solution.get(0);
                solution.remove(corner);
                double bestDistance = Double.NEGATIVE_INFINITY;
                double bestOverreach = 0;
                ShotConfiguration bestConfig = null;
                Corner bestBuddy = null;
                Corner bestBuddy2 = null;
                Corner bestBuddy3 = null;
                if (compositingMethod != CompositingMethod.SingleCorners) {
                    for (Corner buddy : solution) {
                        if (corner.pairsInOneShotWith(buddy)) {
                            ShotConfiguration config = null;
                            Corner buddy2 = null;
                            Corner buddy3 = null;
                            if (corner.diagonalBuddy == buddy) {
                                if (corner.pairsInOneShotWith(corner.xMirrorBuddy)) {
                                    if (solution.contains(corner.xMirrorBuddy)) {
                                        buddy2 = corner.xMirrorBuddy;
                                    }
                                }
                                if (corner.pairsInOneShotWith(corner.yMirrorBuddy)) {
                                    if (solution.contains(corner.yMirrorBuddy)) {
                                        buddy3 = corner.yMirrorBuddy;
                                    }
                                }
                                if (corner.square) {
                                    config = ShotConfiguration.Square;
                                }
                                else if (buddy2 != null || buddy3 != null) {
                                    config = ShotConfiguration.Box;
                                }
                            }
                            else if (corner.xMirrorBuddy == buddy) {
                                config = ShotConfiguration.MirrorY;
                            }
                            else if (corner.yMirrorBuddy == buddy) {
                                config = ShotConfiguration.MirrorY;
                            }
                            if (config != null) {
                                double distance = Math.hypot(
                                        corner.getXDistance(buddy),
                                        corner.getYDistance(buddy));
                                double overreach = Math.min(Math.min(
                                        (cameraViewRadius - tolerance)*2 - distance,
                                        corner.getMaxMaskRadius() - distance), 
                                        buddy.getMaxMaskRadius() - distance);
                                double minOverreach = 0;
                                if (!config.isDiagonal()) {
                                    // With a horizontal/vertical buddy we must also test whether min mask radius is covered.
                                    // This is not needed for a square/box config, where the radius goes all around.
                                    double minMaskRadius = Math.max(corner.getMinMaskRadius(), buddy.getMinMaskRadius());
                                    double minRadius = Math.hypot(minMaskRadius, distance/2);
                                    minOverreach = minRadius - distance/2;
                                }
                                if (overreach > minOverreach + tolerance) {
                                    // Corners can reach each other. 
                                    if (bestConfig == null 
                                            || bestConfig.ordinal() > config.ordinal() 
                                            || (bestConfig == config && bestDistance < distance)) {
                                        bestDistance = distance;
                                        bestOverreach = overreach;
                                        bestConfig = config;
                                        bestBuddy = buddy;
                                        bestBuddy2 = buddy2;
                                        bestBuddy3 = buddy3;
                                    }
                                }
                            }
                        }
                    }
                }
                ArrayList<Corner> shotCorners = new ArrayList<>();
                shotCorners.add(corner);
                if (bestBuddy != null) {
                    // buddy shot
                    solution.remove(bestBuddy);
                    shotCorners.add(bestBuddy);
                    if (bestBuddy2 != null) {
                        solution.remove(bestBuddy2);
                        shotCorners.add(bestBuddy2);
                    }
                    if (bestBuddy3 != null) {
                        solution.remove(bestBuddy3);
                        shotCorners.add(bestBuddy3);
                    }
                    compositeShots.add(new Shot(shotCorners,
                            bestDistance/2 + tolerance,
                            Math.min(cameraViewRadius, bestDistance/2 + bestOverreach),
                            tolerance, bestConfig));
                }
                else {
                    compositeShots.add(new Shot(shotCorners, 
                            corner.getMinMaskRadius(),
                            Math.min(cameraViewRadius, corner.getMaxMaskRadius()),
                            tolerance, ShotConfiguration.Corner));
                }
            }
        }

        private double requiredRoamingRadius(double xCorner, double yCorner) {
            double requiredRoamingRadius = 0;
            for (int i = 0; i < 8; i++) {
                double h = xCorner*xOctogonalHullSign[i]
                         + yCorner*yOctogonalHullSign[i];
                double r = octogonalHull[i] - h;
                if (requiredRoamingRadius < r) {
                    requiredRoamingRadius = r;
                }
            }
            return requiredRoamingRadius;
        }

        private int computeBuddies(double minLeverage, ArrayList<Corner> corners) {
            int solutionCount = 0;
            for (int pass = 0; pass < 2; pass++) {
                for (Corner corner : corners) {
                    if (corner.computeBuddies(corners, minLeverage, minLeverage/2, pass) != null) {
                        solutionCount++;
                    }
                }
            }
            return solutionCount; 
        }

        private void findEligibleCorners(TreeSet<Double> xEdges, TreeSet<Double> yEdges, int xSign, int ySign, 
                ArrayList<Corner> corners) {
            double maxRoamingRadius = camera.getRoamingRadius().convertToUnits(units).getValue();
            // Try all combos of edges in X, Y.
            for (double yEdge : yEdges) {
                for (double xEdge : xEdges) {
                    if (requiredRoamingRadius(xEdge, yEdge) > maxRoamingRadius) {
                        outOfRoamingCandidates++;
                        continue; // Out of the questions, as we can't move the part that far.
                    }

                    // Test the pads in the vicinity.
                    double nearestOutsidePadDistance = cameraViewRadius*2;
                    double nearestEdgePadDistanceX = Double.POSITIVE_INFINITY;
                    double nearestEdgePadDistanceY = Double.POSITIVE_INFINITY;
                    ArrayList<Footprint.Pad> edgeXPads = new ArrayList<>();
                    ArrayList<Footprint.Pad> edgeYPads = new ArrayList<>();
                    for (Footprint.Pad pad : rectifiedPads) {
                        double x = pad.getX() + xSign*pad.getWidth()/2; 
                        double y = pad.getY() + ySign*pad.getHeight()/2;
                        if (xSign*(x - xEdge) > eps || ySign*(y - yEdge) > eps) {
                            // Pad is outside the corner. Records its distance. 
                            double distance = getPadDistance(xEdge, yEdge, pad);
                            if (nearestOutsidePadDistance > distance) {
                                nearestOutsidePadDistance = distance;
                            }
                        }
                        else { 
                            if (Math.abs(x - xEdge) < eps) {
                                // Pad is on our X edge(s).
                                double distance = getPadDistance(xEdge, yEdge, pad);
                                if (nearestEdgePadDistanceX > distance) {
                                    nearestEdgePadDistanceX = distance;
                                }
                                edgeXPads.add(pad);
                            }
                            if (Math.abs(y - yEdge) < eps) {
                                // Pad is on our Y edge(s).
                                double distance = getPadDistance(xEdge, yEdge, pad);
                                if (nearestEdgePadDistanceY > distance) {
                                    nearestEdgePadDistanceY = distance;
                                }
                                edgeYPads.add(pad);
                            }
                        }
                    }
                    // Find edge pads that define the edge sufficiently.
                    // Note, if the corner is not right at the pads (like with a Quad package) the the edge
                    // defined by the pads must be must be at least as long as the distance to the first pad, 
                    // i.e. the overlap distance must be x2 or more (extended to next pad). Otherwise the 
                    // MinAreaRect might find a diagonal bounding box with less area. The wedge gain in X 
                    // obtained by going diagonal must always be less that the loss in Y and vice versa.
                    //
                    //           x2
                    //   |<------|------> > >
                    // ..:....... _   _   _
                    //   :   ____| |_| |_| |
                    //   :  |
                    //   :__| 
                    //   |__
                    //    __|
                    //   |__
                    //    __|
                    //   |__

                    double minOverlap = cameraViewRadius*minCameraRadiusOverlap;
                    double edgePadDistance = Math.max(nearestEdgePadDistanceX, nearestEdgePadDistanceY);
                    // Distances in X along the Y edge.
                    double wantedDistanceX = edgePadDistance*2 + minOverlap;
                    double overlapEdgePadDistanceX = Double.POSITIVE_INFINITY;
                    for (Footprint.Pad pad : edgeYPads) {
                        double distance = getPadDistance(xEdge, yEdge, pad);
                        if (distance + minOverlap > wantedDistanceX) {
                            // Pad is beyond, must extend overlap.
                            if (overlapEdgePadDistanceX > distance + minOverlap) {
                                overlapEdgePadDistanceX = distance + minOverlap;
                            }
                        }
                        else if (wantedDistanceX < distance + pad.getWidth()) {
                            // Pad is across, take exactly the wanted distance.
                            if (overlapEdgePadDistanceX > wantedDistanceX) {
                                overlapEdgePadDistanceX = wantedDistanceX;
                                break; // can't get any better
                            }
                        }
                    }
                    // Distances in Y along the X edge.
                    double wantedDistanceY = edgePadDistance*2 + minOverlap;
                    double overlapEdgePadDistanceY = Double.POSITIVE_INFINITY;
                    for (Footprint.Pad pad : edgeXPads) {
                        double distance = getPadDistance(xEdge, yEdge, pad);
                        if (distance + minOverlap > wantedDistanceY) {
                            // Pad is beyond, must extend overlap.
                            if (overlapEdgePadDistanceY > distance + minOverlap) {
                                overlapEdgePadDistanceY = distance + minOverlap;
                            }
                        }
                        else if (wantedDistanceY < distance + pad.getHeight()) {
                            // Pad is across, take exactly the wanted distance.
                            if (overlapEdgePadDistanceY > wantedDistanceY) {
                                overlapEdgePadDistanceY = wantedDistanceY;
                                break; // can't get any better
                            }
                        }
                    }
                    double overlapEdgePadDistance = Math.max(overlapEdgePadDistanceX, overlapEdgePadDistanceY);
                    if (overlapEdgePadDistance + tolerance < nearestOutsidePadDistance - tolerance 
                            && overlapEdgePadDistance + tolerance < cameraViewRadius) {
                        // This guy is eligible.
                        corners.add(new Corner(xEdge, yEdge, 
                                overlapEdgePadDistance + tolerance, 
                                nearestOutsidePadDistance - tolerance,
                                xSign, ySign));
                    }
                }
            }
        }

        private double getPadDistance(double x, double y, Footprint.Pad pad) {
            double dx;
            if (pad.getX() - pad.getWidth()/2 > x) {
                dx = pad.getX() - pad.getWidth()/2 - x;
            }
            else if (pad.getX() + pad.getWidth()/2 < x) {
                dx = pad.getX() + pad.getWidth()/2 - x;
            }
            else {
                dx = 0;
            }
            double dy;
            if (pad.getY() - pad.getHeight()/2 > y) {
                dy = pad.getY() - pad.getHeight()/2 - y;
            }
            else if (pad.getY() + pad.getHeight()/2 < y) {
                dy = pad.getY() + pad.getHeight()/2 - y;
            }
            else {
                dy = 0;
            }
            return Math.hypot(dx,  dy);
        }

        private void addCoordinate(TreeSet<Double> edges, double coordinate) {
            Double nearest = getNearest(edges, coordinate);
            if (nearest == null || Math.abs(nearest - coordinate) > eps) {
                edges.add(coordinate);
            }
        }

        private Double getNearest(TreeSet<Double> set, double coordinate) {
            Double nearest0 = set.lower(coordinate);
            Double nearest1 = set.higher(coordinate);
            if (nearest0 != null 
                    && (nearest1 == null || coordinate - nearest0 < nearest1 - coordinate)) {
                return nearest0;
            }
            else {
                return nearest1;
            }
        }

        public void prepareTravel() {
            ArrayList<Shot> visitedShots = new ArrayList<>();
            int extraShots = 0;
            for (Shot shot : compositeShots) {
                if (shot.isOptional()) {
                    if (extraShots < getParent().getExtraShots()) {
                        visitedShots.add(shot);
                        extraShots++;
                    }
                }
                else {
                    visitedShots.add(shot);
                }
            }
            // Use a traveling salesman algorithm to optimize the path to visit the placements
            Location start = nozzle.getLocation().convertToUnits(getUnits());
            TravellingSalesman<Shot> tsm = new TravellingSalesman<>(
                    visitedShots, 
                    new TravellingSalesman.Locator<Shot>() { 
                        @Override
                        public Location getLocation(Shot locatable) {
                            return getShotLocation(locatable);
                        }
                    }, 
                    // start from current location
                    start,
                    // leave open
                    null);

            // Solve it using the default heuristics.
            tsm.solve();
            shotsTravel = tsm.getTravel();
        }

        public Location getShotLocation(Shot shot) {
            // Note, we move the nozzle, not the camera, so it is inverted.
            Location location = new Location(units,
                    -shot.getX(), -shot.getY(), 0, 0);
            location = location.rotateXy(locationAndRotation.getRotation());
            location = location.addWithRotation(locationAndRotation);
            return location;
        }

        public Point[] convertToPoints(Shot shot, RotatedRect rect) {
            rect = Utils2D.rotateToExpectedAngle(rect, expectedAngle);
            // Convert to points. We don't use RotatedRect.point() as it does not document an order and might change in the future.
            double angle = VisionUtils.getPixelAngle(camera, rect.angle);
            Location center = VisionUtils.getPixelCenterOffsets(camera, rect.center.x, rect.center.y).convertToUnits(units);
            Location size = new Location(units, rect.size.width*upp.getX(), rect.size.height*upp.getY(), 0, 0);
            double s = Math.sin(Math.toRadians(angle));
            double c = Math.cos(Math.toRadians(angle));
            double sinHalfW = s*size.getX()/2;
            double sinHalfH = s*size.getY()/2;
            double cosHalfW = c*size.getX()/2;
            double cosHalfH = c*size.getY()/2;
            double ec = Math.cos(Math.toRadians(expectedAngle));
            double es = Math.sin(Math.toRadians(expectedAngle));
            double cx = center.getX() + ec*shot.getX() - es*shot.getY();
            double cy = center.getY() + es*shot.getX() + ec*shot.getY();
            // Row, then column sorting.
            Point[] points = new Point[4];
            // Upper left.
            points[0] = new Point(
                    cx - cosHalfW - sinHalfH, 
                    cy - sinHalfW + cosHalfH);
            // Upper Right.
            points[1] = new Point(
                    cx + cosHalfW - sinHalfH, 
                    cy + sinHalfW + cosHalfH);
            // Lower left.
            points[2] = new Point(
                    cx - cosHalfW + sinHalfH, 
                    cy - sinHalfW - cosHalfH);
            // Lower Right.
            points[3] = new Point(
                    cx + cosHalfW + sinHalfH, 
                    cy + sinHalfW - cosHalfH);
            return points;
        }

        public void accumulateShotDetection(Shot shot, RotatedRect rect) {
            if (!compositingSolution.isAdvanced()) {
                detectedRotatedRect = rect;
            }
            else {
                Point[] points = convertToPoints(shot, rect);
                for (Corner corner : shot.getCorners()) {
                    int idx = (corner.getXSign() < 0 ? 0 : 1)
                            + (corner.getYSign() > 0 ? 0 : 2);
                    cornerMap.put(corner, points[idx]);
                }
            }
        }

        public void interpret() {
            if (!compositingSolution.isAdvanced()) {
                return;
            }
            // Find the center and angle.
            Point centerSum = new Point(0, 0);
            int centerWeights = 0;
            double angleSum = 0;
            int angleWeights = 0;
            double xScaleSum = 0;
            double xScaleWeights = 0;
            double yScaleSum = 0;
            double yScaleWeights = 0;
            // Note, the same configurations will be computed many times over in 
            // mirrored or rotated counter-parts. We just don't care.
            for (Entry<Corner, Point> entry : cornerMap.entrySet()) {
                Corner corner = entry.getKey();
                Point point1 = entry.getValue();
                // Diagonal
                if (corner.diagonalBuddy != null) {
                    Point point2 = cornerMap.get(corner.diagonalBuddy);
                    if (point2 != null) {
                        // We got the center point.
                        centerSum = centerSum
                                .add(point1)
                                .add(point2);
                        centerWeights += 2;
                        if (corner.square) {
                            // Diagonal of a square, gives us the angle too (at 45°).
                            Point diff = point2.subtract(point1);
                            double angle = Math.toDegrees(Math.atan2(diff.y, diff.x)) - 45;
                            angle += Math.round((expectedAngle - angle)/90)*90;
                            angleSum += angle;
                            angleWeights++;
                            // We got a scale indication
                            double distance = diff.distance();
                            double dx = Math.abs(corner.diagonalBuddy.getX() - corner.getX());
                            double dy = Math.abs(corner.diagonalBuddy.getY() - corner.getY());
                            xScaleSum += distance;
                            xScaleWeights += dx;
                            yScaleSum += distance;
                            yScaleWeights += dy;
                        }
                    }
                }
                // Pair of mirrors in Y
                if (corner.ySymmetricBuddy != null) {
                    Point point2 = cornerMap.get(corner.xMirrorBuddy);
                    Point point3 = cornerMap.get(corner.ySymmetricBuddy);
                    Point point4 = cornerMap.get(corner.ySymmetricBuddy.xMirrorBuddy);
                    if (point2 != null && point3 != null && point4 != null) {
                        // We got the center point.
                        centerSum = centerSum
                                .add(point1)
                                .add(point2)
                                .add(point3)
                                .add(point4);
                        centerWeights += 4;
                    }
                }
                // Pair of mirrors in X
                if (corner.xSymmetricBuddy != null) {
                    Point point2 = cornerMap.get(corner.yMirrorBuddy);
                    Point point3 = cornerMap.get(corner.xSymmetricBuddy);
                    Point point4 = cornerMap.get(corner.xSymmetricBuddy.yMirrorBuddy);
                    if (point2 != null && point3 != null && point4 != null) {
                        // We got the center point.
                        centerSum = centerSum
                                .add(point1)
                                .add(point2)
                                .add(point3)
                                .add(point4);
                        centerWeights += 4;
                    }
                }
                // Aligned in X
                for (Corner buddy : new Corner[] { 
                        corner.xAlignedBuddy, 
                        corner.yAlignedBuddy, 
                        corner.xMirrorBuddy, 
                        corner.yMirrorBuddy, 
                }) {
                    if (buddy != null) {
                        Point point2 = cornerMap.get(buddy);
                        if (point2 != null) {
                            // We got an angle indication.
                            Point diff = point2.subtract(point1);
                            double angle = Math.toDegrees(Math.atan2(diff.y, diff.x));
                            angle += Math.round((expectedAngle - angle)/90)*90;
                            angleSum += angle;
                            angleWeights++;
                            // We got a scale indication
                            double distance = diff.distance();
                            double dx = Math.abs(buddy.getX() - corner.getX());
                            double dy = Math.abs(buddy.getY() - corner.getY());
                            if (dx > dy) {
                                xScaleSum += distance;
                                xScaleWeights += dx;
                            }
                            else {
                                yScaleSum += distance;
                                yScaleWeights += dy;
                            }
                        }
                    }
                }
            }
            // Evaluate the stats.
            detectedCenter = centerSum.divide(centerWeights);
            detectedAngle = angleSum/angleWeights;
            detectedAngle += Math.round((expectedAngle - detectedAngle)/90)*90;
            detectedScale = new Point(
                    xScaleSum/xScaleWeights, 
                    yScaleSum/yScaleWeights); 
            detectedSize = new Point(
                    detectedScale.x*Math.max(-leftEdges.first(), rightEdges.last())*2,
                    detectedScale.y*Math.max(-bottomEdges.first(), topEdges.last())*2);
            // For pipeline result compatibility, make the RotatedRect in OpenCv pixel coordinates. 
            double angle = VisionUtils.getPixelAngle(camera, detectedAngle);
            org.opencv.core.Point center = new org.opencv.core.Point(
                    camera.getWidth()*0.5 + detectedCenter.x/upp.getX(), 
                    camera.getHeight()*0.5 - detectedCenter.y/upp.getY());
            org.opencv.core.Size size = new org.opencv.core.Size(
                    detectedSize.x/upp.getX(), 
                    detectedSize.y/upp.getY());
            detectedRotatedRect = new RotatedRect(center, size, angle);
        }
    }
}
