/*
 * Copyright (C) 2021 <mark@makr.zone>
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

package org.openpnp.vision.pipeline.stages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.opencv.core.Mat;
import org.openpnp.model.Length;
import org.openpnp.model.Location;
import org.openpnp.model.Point;
import org.openpnp.spi.Camera;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Property;
import org.openpnp.vision.pipeline.Stage;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;

/**
 * Finds the maximum circular symmetry in the working image and stores the results as a single element List<Circle> on the model. 
 */
@Stage(description="Finds circular symmetry in the working image. Diameter range and maximum search distance can be specified.")
public class DetectCircularSymmetry extends CvStage {

    @Attribute
    @Property(description = "Minimum diameter of the circle, in pixels.")
    private int minDiameter = 10;

    @Attribute
    @Property(description = "Maximum diameter of the circle, in pixels.")
    private int maxDiameter = 100;

    @Attribute(required = false)
    @Property(description = "Maximum search distance from nominal center, in pixels.")
    private int maxDistance = 100;

    @Attribute(required = false)
    @Property(description = "Maximum number of targets to be found.")
    private int maxTargetCount = 1;

    @Attribute(required = false)
    @Property(description = "Minimum relative circular symmetry (overall pixel variance vs. circular pixel variance).")
    private double minSymmetry = 1.2;

    @Attribute(required = false)
    @Property(description = "Correlated minimum circular symmetry for multiple matches, i.e. other matches must have "
            + "at least this relative symmetry. Must be in the interval [0,1]. ")
    private double corrSymmetry = 0.0;

    @Attribute(required = false)
    @Property(description = "Property name as controlled by the vision operation using this pipeline.<br/>"
            + "<ul><li><i>propertyName</i>.diameter</li><li><i>propertyName</i>.maxDistance</li><li><i>propertyName</i>.center</li></ul>"
            + "If set, these will override the properties configured here.")
    private String propertyName = "";

    @Attribute(required = false)
    @Property(description = "Relative outer diameter margin used when the propertyName is set.")
    private double outerMargin = 0.2;

    @Attribute(required = false)
    @Property(description = "Relative inner diameter margin used when the propertyName is set.")
    private double innerMargin = 0.4;

    @Attribute(required = false)
    @Property(description = "To speed things up, only one pixel out of a square of subSampling × subSampling pixels is sampled. "
            + "The best region is then locally searched using iteration with smaller and smaller subSampling size.<br/>"
            + "The subSampling value will automatically be reduced for small diameters. "
            + "Use BlurGaussian before this stage if subSampling suffers from moiré effects.")
    private int subSampling = 8;

    @Attribute(required = false)
    @Property(description = "The superSampling value can be used to achieve sub-pixel final precision: "
            + "1 means no supersampling, 2 means half sub-pixel precision etc.")
    private int superSampling = 1;

    @Attribute(required = false)
    @Property(description = "Overlay match map.")
    private boolean diagnostics = false;

    public int getMinDiameter() {
        return minDiameter;
    }

    public void setMinDiameter(int minDiameter) {
        this.minDiameter = minDiameter;
    }

    public int getMaxDiameter() {
        return maxDiameter;
    }

    public void setMaxDiameter(int maxDiameter) {
        this.maxDiameter = maxDiameter;
    }

    public int getMaxDistance() {
        return maxDistance;
    }

    public void setMaxDistance(int maxDistance) {
        this.maxDistance = maxDistance;
    }

    public double getMinSymmetry() {
        return minSymmetry;
    }

    public void setMinSymmetry(double minSymmetry) {
        this.minSymmetry = minSymmetry;
    }

    public int getSubSampling() {
        return subSampling;
    }

    public void setSubSampling(int subSampling) {
        this.subSampling = subSampling;
    }

    public int getSuperSampling() {
        return superSampling;
    }

    public void setSuperSampling(int superSampling) {
        this.superSampling = superSampling;
    }

    public boolean isDiagnostics() {
        return diagnostics;
    }

    public void setDiagnostics(boolean diagnostics) {
        this.diagnostics = diagnostics;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public double getOuterMargin() {
        return outerMargin;
    }

    public void setOuterMargin(double outerMargin) {
        this.outerMargin = outerMargin;
    }

    public double getInnerMargin() {
        return innerMargin;
    }

    public void setInnerMargin(double innerMargin) {
        this.innerMargin = innerMargin;
    }

    public int getMaxTargetCount() {
        return maxTargetCount;
    }

    public void setMaxTargetCount(int maxTargetCount) {
        this.maxTargetCount = maxTargetCount;
    }

    public double getCorrSymmetry() {
        return corrSymmetry;
    }

    public void setCorrSymmetry(double corrSymmetry) {
        this.corrSymmetry = corrSymmetry;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        Camera camera = (Camera) pipeline.getProperty("camera");
        Mat mat = pipeline.getWorkingImage();
        // Get overriding properties, if any and convert to pixels.
        Length diameterByProperty = (Length) pipeline.getProperty(propertyName+".diameter");
        int minDiameter = this.minDiameter;
        int maxDiameter = this.maxDiameter;
        if (diameterByProperty != null && diameterByProperty.getValue() > 0) {
            double diameter = VisionUtils.toPixels(diameterByProperty, camera);
            minDiameter = (int) Math.round(diameter*(1.0-innerMargin));
            maxDiameter = (int) Math.round(diameter*(1.0+outerMargin));
        }
        int maxDistance = this.maxDistance;
        Length maxDistanceByProperty = (Length) pipeline.getProperty(propertyName+".maxDistance");
        if (maxDistanceByProperty != null && maxDistanceByProperty.getValue() > 0) {
            maxDistance = (int) Math.round(VisionUtils.toPixels(maxDistanceByProperty, camera));
        }
        Point center = new Point(mat.cols()*0.5, mat.rows()*0.5);
        Location centerByProperty = (Location) pipeline.getProperty(propertyName+".center");
        if (centerByProperty != null) {
            center = VisionUtils.getLocationPixels(camera, centerByProperty);
        }

        List<Result.Circle> circles = findCircularSymmetry(mat, (int)center.x, (int)center.y, 
                maxDiameter, minDiameter, maxDistance*2, maxTargetCount, minSymmetry, corrSymmetry, subSampling, superSampling, diagnostics, new ScoreRange());
        return new Result(null, circles);
    }

    public static class ScoreRange {
        public double minScore = Double.POSITIVE_INFINITY;
        public double maxScore = Double.NEGATIVE_INFINITY;
        public double finalScore = Double.NEGATIVE_INFINITY;
        void add(double score) {
            if (score > 1) {
                minScore = Math.min(minScore, score); 
                maxScore = Math.max(maxScore, score);
                finalScore = Math.max(finalScore, score);
            }
        }
    }

    public static class SymmetryCircle extends CvStage.Result.Circle {
        public SymmetryCircle(double x, double y, double diameter, double score) {
            super(x, y, diameter);
            this.score = score;
        }

        public double score;

        public double getScore() {
            return score;
        }

        public void setScore(double score) {
            this.score = score;
        }

        @Override
        public String toString() {
            return "Circle [x=" + x + ", y=" + y + ", diameter=" + diameter + ", score="+ score + "]";
        }
    }

    /**
     * The detection will recurse into a local search with finer subSampling. A range of iterationRadius*subSampling 
     * pixels around the preliminary best location will be searched.  
     */
    static final private int iterationRadius = 2;
    /**
     * The subSampling block size will be divided by iterationDivision when recursing into a local search. 
     */
    static final private int iterationDivision = 4;
    /**
     * Some extra debugging stuff used for development, that might be useful again in the future.  
     */
    static final boolean DEBUG = false;

    /**
     * Find the circle that has its center at the greatest circular symmetry in the given image,
     * indicating the largest contrast edge as its diameter.
     * 
     * @param image             Image to be searched. If diagnostics are enabled, this image will be modified. 
     * @param xCenter           Nominal X center of the search area inside the given image, in pixels.
     * @param yCenter           Nominal Y center of the search area inside the given image, in pixels.
     * @param maxDiameter       Maximum diameter of the examined circular symmetry area (pixels outside it are ignored).
     * @param minDiameter       Minimum diameter of the examined circular symmetry area (pixels inside it are ignored).
     * @param searchRange       Search range around the given center.
     * @param maxTargetCount    Maximum number of wanted targets detected.
     * @param minSymmetry       The minimum circular symmetry required to detect a match. This is the ratio of overall pixel
     *                          variance divided by the circular pixel variance (sum of ring pixel variances). 
     * @param corrSymmetry      Correlated minimum circular symmetry for multiple matches, i.e. other matches must have 
     *                          at least this relative symmetry. Must be in the interval [0,1].
     * @param subSampling       Sub-sampling pixel distance, i.e. only one pixel out of a square of size subSampling will be 
     *                          examined on the first pass. 
     * @param superSampling     Super-sampling pixel fraction, i.e. the result will have 1/superSampling sub-pixel accuracy.   
     * @param diagnostics       If true, draws a diagnostic heat map, circle and cross hairs into the image. 
     * @param scoreRange        Outputs the score range of all the sampled center candidates.
     * @return                  A list of the the detected circles (currently only the best).
     * @throws Exception
     */
    public static  List<Result.Circle> findCircularSymmetry(Mat image, int xCenter, int yCenter,
            int maxDiameter, int minDiameter, int searchRange, int maxTargetCount, 
            double minSymmetry, double corrSymmetry, int subSampling,
            int superSampling, boolean diagnostics, ScoreRange scoreRange) throws Exception {
        // Image properties.
        final int channels = image.channels();
        final int width = image.cols();
        final int height = image.rows();
        // Some sanity checks on the diameters.
        minDiameter = Math.max(3, minDiameter | 1); // make it odd
        maxDiameter = Math.max(minDiameter+4, maxDiameter | 1); // make it odd
        superSampling = Math.min(16, superSampling);
        // Effective subSampling may have to be finer if the searched circular edge is finer. 
        final int subSamplingEff = Math.max(1, 
                Math.min(subSampling, Math.min((maxDiameter-minDiameter)/4, minDiameter/2)));
        // Constrain the search range to the image.
        final int searchRangeEff = (
                Math.min((xCenter-maxDiameter/2)-2-subSamplingEff, 
                        Math.min((width-xCenter-maxDiameter/2)-2-subSamplingEff,
                                Math.min((yCenter-maxDiameter/2)-2-subSamplingEff, 
                                        Math.min((height-yCenter-maxDiameter/2)-2-subSamplingEff, 
                                                (searchRange+1)/2))))*2)
                /subSamplingEff*subSamplingEff; // round to multiple of subSamplingEff
        if (searchRangeEff < searchRange/5) {
            throw new Exception("Image too small for given parameters.");
        }
        // Derive some working variables. 
        final int rSearch = searchRangeEff/2+1;
        final int rSearchSq = rSearch*rSearch;
        int r = maxDiameter / 2;
        int r0 = minDiameter / 2 - 1;
        final int diameter = maxDiameter + searchRangeEff + subSamplingEff/2 + 1;
        final int xCrop = xCenter - diameter/2;
        final int yCrop = yCenter - diameter/2;
        // Create super sampling offsets if needed.
        double [] superSamplingOffsets;
        final boolean finalSamplingPass = (subSamplingEff == 1 && (searchRange <= iterationRadius || superSampling <= 1));
        if (finalSamplingPass && superSampling > 1) {
            superSamplingOffsets = new double[superSampling];
            for (int s = 0; s < superSampling; s++) {
                superSamplingOffsets[s] = ((double)s)/superSampling - 0.5;
            }
        }
        else {
            superSamplingOffsets = new double[] { 0.0 };
        }

        // Get the pixels out of the Mat.
        byte[] pixelSamples = new byte[diameter*width*channels]; 
        image.get(yCrop, 0, pixelSamples);

        // Running best results.
        double scoreBest = Double.NEGATIVE_INFINITY;
        double xBest = 0;
        double yBest = 0;
        int rContrastBest = 0;
        double [] scoreMap = null;
        int[] radiusMap = null;
        boolean showDiagnostics = (diagnostics && superSamplingOffsets.length == 1); 
        int searchRangeMap = searchRangeEff/subSamplingEff;
        if (showDiagnostics || maxTargetCount > 1) {
            scoreMap = new double[searchRangeMap*searchRangeMap];
            radiusMap = new int[searchRangeMap*searchRangeMap];
            // The final score is reset her so it will reflect the last pass' maximum score. 
            scoreRange.finalScore = 0;
        }
        // Outer super-sampling loop. 
        for (double xOffset : superSamplingOffsets) {
            for (double yOffset : superSamplingOffsets) {
                double scoreBestSampling = Double.NEGATIVE_INFINITY;
                double xBestSampling = 0;
                double yBestSampling = 0;
                // Create the concentric rings of circular symmetry as lists of indices into the flat array of pixels.
                // The indices are relative to the origin but they can be offset to any x, y (within
                // range) and still remain valid, thanks to modulo behavior.
                Object[] ringsA = new Object[r + 1];
                int nRingSamples = 0;
                for (int ri = r0; ri < ringsA.length; ri += subSamplingEff) {
                    ringsA[ri] = new ArrayList<Integer>();
                }
                for (int y = -r, yi = 0; y <= r; y += subSamplingEff, yi += subSamplingEff) {
                    for (int x = -r, idx = yi*width*channels; 
                            x <= r; 
                            x += subSamplingEff, idx += channels*subSamplingEff) {
                        double dx = x - xOffset;
                        double dy = y - yOffset;
                        double distanceSquare = dx*dx + dy*dy;
                        double d = Math.sqrt(distanceSquare);
                        int distance = r0 + (-r0 + (int) Math.round(d))/subSamplingEff*subSamplingEff;
                        if (r0 <= distance && distance <= r) {
                            ((ArrayList<Integer>) ringsA[distance]).add(idx);
                            nRingSamples++;
                        }
                    }
                }
                // Convert indices to arrays (hopefully faster).
                int[][] rings = new int[ringsA.length][];
                for (int ri = r0; ri < ringsA.length; ri += subSamplingEff) {
                    rings[ri] = ((ArrayList<Integer>) ringsA[ri]).stream()
                            .mapToInt(i -> i)
                            .toArray();
                }
                // Now iterate through all the offsets and find the maximum circular symmetry
                // which is the one with the largest ratio between radial and circular variances.
                for (int yi = 0, yis = 0; yi < searchRangeEff; yi += subSamplingEff, yis++) {
                    for (int xi = 0, xis = 0, idxOffset = (yi*width + xCrop) * channels; 
                            xi < searchRangeEff; 
                            xi += subSamplingEff, xis++, idxOffset += channels*subSamplingEff) {
                        int distSq = (xi - rSearch)*(xi - rSearch) + (yi - rSearch)*(yi - rSearch);
                        if (distSq <= rSearchSq) {
                            double varianceSum = 0.01; // Prevent div by zero.
                            double lastAvgChannels = 0;
                            double contrastBest = Double.NEGATIVE_INFINITY;
                            int riContrastBest = 0;
                            double[] sumOverall = new double[channels];
                            double[] sumSqOverall = new double[channels];
                            double[] nOverall = new double[channels];
                            // Examine each ring.
                            for (int ri = r0; ri < rings.length; ri += subSamplingEff) {
                                int[] ring = rings[ri];
                                double n = ring.length;
                                if (n > 0) {
                                    long sumChannels = 0;
                                    for (int ch = 0; ch < channels; ch++) {
                                        // Calculate the variance along the ring.
                                        long sum = 0;
                                        long sumSq = 0;
                                        for (int idx : ring) {

                                            if (DEBUG) {
                                                int idxDebug = idxOffset + idx;
                                                int yDebug = (idxDebug/channels)/width;
                                                int xDebug = (idxDebug/channels)%width;
                                                if (xDebug < xi || xDebug > xCrop + xi + r*2 + 1
                                                        || yDebug < yi || yDebug > yi + r*2 + 1) {
                                                    throw new Exception("unexpected idx offset calculation");
                                                }
                                                if (finalSamplingPass 
                                                        && xi == searchRangeEff/2 && yi == searchRangeEff/2 
                                                        && ri == rings.length-1
                                                        && yOffset == xOffset) {
                                                    byte [] pixelData = new byte[channels];
                                                    image.get(yDebug+yCrop, xDebug, pixelData);
                                                    int dc = Arrays.binarySearch(superSamplingOffsets, xOffset) % channels;
                                                    pixelData[2-dc] = (byte)255;
                                                    image.put(yDebug+yCrop, xDebug, pixelData);
                                                }
                                            }

                                            int pixel = Byte.toUnsignedInt(pixelSamples[idxOffset + idx + ch]);
                                            sum += pixel;
                                            sumSq += pixel * pixel;
                                        }
                                        sumChannels += sum;
                                        sumOverall[ch] += sum;
                                        sumSqOverall[ch] += sumSq;
                                        nOverall[ch] += n;
                                        double variance = (sumSq - (sum * sum) / n);// / Math.log(ri+4);
                                        varianceSum += variance;
                                    }
                                    double avgChannels = sumChannels / n;
                                    if (ri*2 >= minDiameter) {
                                        double contrast = Math.abs(avgChannels - lastAvgChannels);
                                        if (contrastBest < contrast) {
                                            contrastBest = contrast;
                                            riContrastBest = ri;
                                        }
                                    }
                                    lastAvgChannels = avgChannels;
                                }
                            }
                            double varianceOverall = 0.01; // Prevent zero.
                            for (int ch = 0; ch < channels; ch++) {
                                varianceOverall += (sumSqOverall[ch] - (sumOverall[ch] * sumOverall[ch]) / nOverall[ch]);
                            }
                            double score = varianceOverall/varianceSum; 
                            if (scoreBestSampling < score) {
                                scoreBestSampling = score;
                                xBestSampling = xi + xCrop + r + 0.5 + xOffset;
                                yBestSampling = yi + yCrop + r + 0.5 + yOffset;
                                if (scoreBest < score) {
                                    scoreBest = score;
                                    xBest = xBestSampling;
                                    yBest = yBestSampling;
                                    rContrastBest = riContrastBest;
                                }
                            }
                            if (scoreMap != null) {
                                scoreMap[yis*searchRangeMap + xis] = score;
                                radiusMap[yis*searchRangeMap + xis] = riContrastBest;
                                scoreRange.add(score);
                            }
                        }
                    }
                }
                if (DEBUG) {
                    Logger.trace("best circular symmetry at subSampling "+subSamplingEff+", range "+searchRangeEff
                            +(finalSamplingPass ? ", superSampling "+superSampling+" offsets Y"+xOffset+" Y"+yOffset : "")
                            +": "+scoreBestSampling+" X"+xBestSampling+" Y"+yBestSampling
                            + " ring samples "+nRingSamples);
                }
            }
        }

        // Process the search results.
        List<CvStage.Result.Circle> ret;
        if (maxTargetCount > 1) {
            // Need to find multiple targets. Process the score map.
            List<SymmetryCircle> maxima = new ArrayList<SymmetryCircle>();
            if (scoreBest > minSymmetry) {
                // Find the local maxima.
                for (int yis = 1, yim0 = 0, yim1 = searchRangeMap, yim2 = searchRangeMap*2;  
                        yis < searchRangeMap-1; yis++, yim0 += searchRangeMap, yim1 += searchRangeMap, yim2 += searchRangeMap) {
                    for (int xis = 1; xis < searchRangeMap-1; xis++) {
                        //      x0 x1 x2
                        //    -----------
                        // y0 ¦  0  1  2
                        // y1 ¦  3  4  5
                        // y2 ¦  6  7  8
                        double score = scoreMap[yim1 + xis];
                        // This will only find true peaks, no plateaus, but plateaus will never happen in real life. 
                        if (score > minSymmetry
                                && scoreMap[yim1 + xis - 1] < score 
                                && scoreMap[yim1 + xis + 1] < score 
                                && scoreMap[yim0 + xis - 1] < score 
                                && scoreMap[yim0 + xis] < score 
                                && scoreMap[yim0 + xis + 1] < score
                                && scoreMap[yim2 + xis - 1] < score 
                                && scoreMap[yim2 + xis] < score 
                                && scoreMap[yim2 + xis + 1] < score) {
                            SymmetryCircle circle = new SymmetryCircle(
                                    xis*subSamplingEff + xCrop + r + 0.5, 
                                    yis*subSamplingEff + yCrop + r + 0.5, 
                                    radiusMap[yim1 + xis],
                                    score);
                            maxima.add(circle);
                        }
                    }
                }
                // Take only those with no better-scoring overlaps.
                List<SymmetryCircle> maximaFiltered = new ArrayList<SymmetryCircle>();
                int sqMinDistance = maxDiameter*maxDiameter;
                for (SymmetryCircle cand : maxima) {
                    boolean noBetterOverlap = true;
                    for (SymmetryCircle other : maxima) {
                        if (other.score > cand.score) {
                            double dx = cand.x - other.x;
                            double dy = cand.y - other.y;
                            double dSq = dx*dx + dy*dy;
                            if (dSq < sqMinDistance) {
                                // Better-scoring overlap.
                                noBetterOverlap = false;
                                break;
                            }
                        }
                    }
                    if (noBetterOverlap) {
                        maximaFiltered.add(cand);
                    }
                }

                List<SymmetryCircle> samplingFiltered = new ArrayList<SymmetryCircle>();
                if (finalSamplingPass) {
                    samplingFiltered.addAll(maximaFiltered);
                }
                else {
                    // For each local maxima...
                    for (SymmetryCircle localBest : maximaFiltered) {
                        // ... recursion into finer subSampling and local search.
                        List<CvStage.Result.Circle> localRet = findCircularSymmetry(image, (int)localBest.x, (int)localBest.y, maxDiameter, minDiameter, 
                                subSamplingEff*iterationRadius, 1, minSymmetry, corrSymmetry, 
                                subSamplingEff/iterationDivision, superSampling, diagnostics, scoreRange);
                        if (localRet.size() > 0) { 
                            samplingFiltered.add((SymmetryCircle) localRet.get(0));
                        }
                    }
                }

                // Sort best results first.
                Collections.sort(samplingFiltered, new Comparator<SymmetryCircle>() {
                    @Override
                    public int compare(SymmetryCircle o1, SymmetryCircle o2) {
                        return ((Double) o2.score).compareTo(o1.score);
                    }
                });

                // Limit the result count and correlated minimum symmetry.
                ret = new ArrayList<>();
                if (samplingFiltered.size() > 0) {
                    int n = 0;
                    double minScore = samplingFiltered.get(0).score*corrSymmetry;
                    for (SymmetryCircle circle : samplingFiltered) {
                        if (circle.score < minScore) {
                            break;
                        }
                        ret.add(circle);
                        if (++n >= maxTargetCount) {
                            break;
                        }
                    }
                }
            }
            else {
                // Empty.
                ret = new ArrayList<>();
            }
        }
        else {  
            // Just one result to process (maxTargetCount == 1).
            if (finalSamplingPass) {
                // Got the final result.
                ret = new ArrayList<>();
                if (scoreBest > minSymmetry) {
                    ret.add(new SymmetryCircle(xBest, yBest, rContrastBest * 2, scoreBest));
                }
            }
            else {
                // Recursion into finer subSampling and local search.
                ret = findCircularSymmetry(image, (int)(xBest), (int)(yBest), maxDiameter, minDiameter, 
                        subSamplingEff*iterationRadius, 1, minSymmetry, corrSymmetry, 
                        subSamplingEff/iterationDivision, superSampling, diagnostics, scoreRange);
            }
        }

        if (showDiagnostics) {
            // Paint diagnostics.
            double rscale = 1/(scoreHeat(scoreRange.maxScore) - scoreHeat(scoreRange.minScore));
            double scale = 255*channels*rscale;
            for (int yi = 0; yi < searchRangeEff-subSamplingEff/2; yi++) {
                for (int xi = 0; xi < searchRangeEff-subSamplingEff/2; xi++) {
                    // Coordinates into scoreMap (must be multiples of subSamplingEff). 
                    int xis = (xi+subSamplingEff/2)/subSamplingEff;
                    int yis = (yi+subSamplingEff/2)/subSamplingEff;
                    // Mask to search Radius.
                    double s = scoreMap[yis*searchRangeMap + xis];
                    if (s > 1.0) {
                        /// Pixel coordinates.
                        int col = xi + xCrop + r;
                        int row = yi + yCrop + r;
                        double dx2 = (col - xBest);
                        double  dy2 = (row - yBest);
                        int distance2 = (int)Math.round(Math.sqrt(dx2*dx2 + dy2*dy2));
                        // Put the heat-map/indicator pixel back, but only if outside the local search range. 
                        if (subSamplingEff == 1 
                                || distance2 >= subSamplingEff*iterationRadius/2)  {
                            // Get the score.
                            double heat = scoreHeat(s);
                            if (!Double.isFinite(heat)) {
                                heat = scoreHeat(scoreRange.minScore);
                            }
                            heat -= scoreHeat(scoreRange.minScore);
                            double score = heat*scale;
                            // Determine if this pixel coordinate is part of the indicator (cross-hairs and diameter).
                            boolean indicate = false;
                            for (CvStage.Result.Circle circle : ret) {
                                double dx = xi + xCrop - circle.x + r + 0.01;
                                double dy = yi + yCrop - circle.y + r + 0.01;
                                int distance = (int)Math.round(Math.sqrt(dx*dx + dy*dy));
                                indicate = (distance == (int)(circle.diameter/2) 
                                        || ((Math.round(dx) == 0 || Math.round(dy) == 0) 
                                                && distance < circle.diameter));
                                if (indicate) {
                                    break;
                                }
                            }
                            // Overlay the score as a heat-map, alpha-blended with the image.
                            double alpha = Math.pow(heat*rscale, 3);
                            double alphaCompl = 1-alpha;
                            byte [] pixelData = new byte[channels];
                            image.get(row, col, pixelData);
                            if (channels == 3) {
                                int red = 0;
                                int green = 0;
                                int blue = 0;
                                if (indicate) {
                                    red = 0;
                                    green = 255;
                                    blue = 0;
                                    alpha = 0.5;
                                    alphaCompl = 1 - alpha;
                                }
                                else if (score <= 255) {
                                    blue = (int) score;
                                }
                                else if (score <= 255+255) {
                                    blue = (int) (255+255-score);
                                    red = (int) (score-255);
                                }
                                else {
                                    red = (int) 255;
                                    green = (int) (score - 255-255);
                                }
                                pixelData[2] = (byte) (alpha*red + alphaCompl*Byte.toUnsignedInt(pixelData[2]));
                                pixelData[1] = (byte) (alpha*green + alphaCompl*Byte.toUnsignedInt(pixelData[1]));
                                pixelData[0] = (byte) (alpha*blue + alphaCompl*Byte.toUnsignedInt(pixelData[0]));
                            }
                            else {
                                if (indicate) {
                                    pixelData[0] = (byte) 255; 
                                }
                                else {
                                    pixelData[0] = (byte) (alpha*score + alphaCompl*Byte.toUnsignedInt(pixelData[0]));
                                }
                            }
                            image.put(row, col, pixelData);
                        } 
                    }
                }
            }
        }
        return ret;
    }

    private static double scoreHeat(double score) {
        return Math.log(Math.log(score));
    }
}
