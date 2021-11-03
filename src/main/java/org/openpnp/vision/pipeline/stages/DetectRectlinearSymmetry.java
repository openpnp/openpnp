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

import java.util.Arrays;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.opencv.core.Mat;
import org.opencv.core.RotatedRect;
import org.opencv.core.Size;
import org.openpnp.model.Length;
import org.openpnp.model.Location;
import org.openpnp.model.Point;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Property;
import org.openpnp.vision.pipeline.Stage;
import org.simpleframework.xml.Attribute;

/**
 * Finds the object and angle with maximum rectlinear symmetry. 
 */
@Stage(description="Finds the object and angle with maximum rectlinear symmetry in the working image.")
public class DetectRectlinearSymmetry extends CvStage {

    @Attribute(required = false)
    @Property(description = "Expected angle.")
    private double expectedAngle = 0;

    @Attribute(required = false)
    @Property(description = "Search distance from the center.")
    private double searchDistance = 100;

    @Attribute(required = false)
    @Property(description = "Search angle, two-sided around expected angle.")
    private double searchAngle = 45;

    @Attribute(required = false)
    @Property(description = "Maximum rectangular width of the detected object.")
    private double maxWidth = 100;

    @Attribute(required = false)
    @Property(description = "Maximum rectangular height of the detected object.")
    private double maxHeight = 100;

    @Attribute(required = false)
    @Property(description = "Margin around the detected object, used to detect the edges.")
    private int margin = 40;

    @Attribute(required = false)
    @Property(description = "Symmetry finding function.")
    private SymmetryFunction symmetryFunction = SymmetryFunction.Image;

    @Attribute(required = false)
    @Property(description = "Minimum relative symmetry. Values larger than 1.0 indicate symmetry.")
    private double minSymmetry = 1.2;

    @Attribute(required = false)
    @Property(description = "To speed things up, only one pixel out of a square of subSampling × subSampling pixels is sampled. "
            + "The best region is then locally searched using iteration with smaller and smaller subSampling size.<br/>"
            + "The subSampling value will automatically be reduced. "
            + "Use BlurGaussian before this stage if subSampling suffers from moiré effects.")
    private int subSampling = 8;

    @Attribute(required = false)
    @Property(description = "The superSampling value can be used to achieve sub-pixel final precision:<br/>"
            + "1 means no supersampling, 2 means half sub-pixel precision etc.<br/>"
            + "Negative values can be used to stop refining subSampling, -2 means it will stop at a 2-pixel resolution.")
    private int superSampling = 1;

    @Attribute(required = false)
    @Property(description = "Smoothing applied to the sampled cross-sections. Given as the Gaussian kernel size.<br/>"
            + "This is needed to eliminate interferences, when angular sampling coincides with the pixel raster or its diagonals.")
    private int smoothing = 5;

    @Attribute(required = false)
    @Property(description = "Property name as controlled by the vision operation using this pipeline.<br/>"
            + "If set, these will override the properties configured here.")
    private String propertyName = "alignment";

    @Attribute(required = false)
    @Property(description = "Display the match with cross-hairs and bounds.")
    private boolean diagnostics = false;

    @Attribute(required = false)
    @Property(description = "Overlay a diagnostic map indicating the angular reclinear contrast and rectlinear cross-section.")
    private boolean diagnosticsMap = false;

    
    public double getExpectedAngle() {
        return expectedAngle;
    }

    public void setExpectedAngle(double expectedAngle) {
        this.expectedAngle = expectedAngle;
    }

    public double getSearchDistance() {
        return searchDistance;
    }

    public void setSearchDistance(double searchDistance) {
        this.searchDistance = searchDistance;
    }

    public double getSearchAngle() {
        return searchAngle;
    }

    public void setSearchAngle(double searchAngle) {
        this.searchAngle = searchAngle;
    }

    public double getMaxWidth() {
        return maxWidth;
    }

    public void setMaxWidth(double maxWidth) {
        this.maxWidth = maxWidth;
    }

    public double getMaxHeight() {
        return maxHeight;
    }

    public void setMaxHeight(double maxHeight) {
        this.maxHeight = maxHeight;
    }

    public int getMargin() {
        return margin;
    }

    public void setMargin(int margin) {
        this.margin = margin;
    }

    public SymmetryFunction getSymmetryFunction() {
        return symmetryFunction;
    }

    public void setSymmetryFunction(SymmetryFunction symmetryFunction) {
        this.symmetryFunction = symmetryFunction;
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

    public int getSmoothing() {
        return smoothing;
    }

    public void setSmoothing(int smoothing) {
        this.smoothing = smoothing;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    //    public int getMaxTargetCount() {
    //        return maxTargetCount;
    //    }
    //
    //    public void setMaxTargetCount(int maxTargetCount) {
    //        this.maxTargetCount = maxTargetCount;
    //    }
    //
    //    public double getCorrSymmetry() {
    //        return corrSymmetry;
    //    }
    //
    //    public void setCorrSymmetry(double corrSymmetry) {
    //        this.corrSymmetry = corrSymmetry;
    //    }
    //

    public boolean isDiagnostics() {
        return diagnostics;
    }

    public void setDiagnostics(boolean diagnostics) {
        this.diagnostics = diagnostics;
    }

    public boolean isDiagnosticsMap() {
        return diagnosticsMap;
    }

    public void setDiagnosticsMap(boolean diagnosticsMap) {
        this.diagnosticsMap = diagnosticsMap;
    }


    public enum SymmetryFunction {
        Image,
        Constrast;

        double [] getKernel(double sigma, int size) {
            size |= 1;
            switch(this) {
                case Image: {
                    double [] kernel = getGaussianKernel(sigma, 0, size);
                    return kernel;
                }
                case Constrast: {
                    double [] gauss = getGaussianKernel(sigma, 0, size);
                    double [] kernel = new double [size*2];
                    for (int i = 0; i < size; i++) {
                        kernel[i] = gauss[i];
                    }
                    for (int i = 0; i < size; i++) {
                        kernel[i+size] = -gauss[i];
                    }
                    return kernel;
                }
            }
            return null;
        }
    }


    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        Mat mat = pipeline.getWorkingImage();

        // Get overriding properties, if any and convert to pixels if necessary.
        Point center = new Point(mat.cols()*0.5, mat.rows()*0.5);
        double expectedAngle = getExpectedAngle();
        double searchAngle = getSearchAngle();
        double searchDistance = getSearchDistance();
        double maxWidth = getMaxWidth();
        double maxHeight = getMaxHeight();
        double margin = getMargin();

        if (!propertyName.isEmpty()) {

            center = getPossiblePipelinePropertyOverride(center, pipeline, 
                    propertyName + ".center", Point.class, org.opencv.core.Point.class, 
                    Location.class);

            expectedAngle = getPossiblePipelinePropertyOverride(expectedAngle, pipeline, 
                    propertyName + ".expectedAngle", Double.class, Integer.class);

            searchAngle = getPossiblePipelinePropertyOverride(searchAngle, pipeline, 
                    propertyName + ".searchAngle", Double.class, Integer.class);

            searchDistance = getPossiblePipelinePropertyOverride(searchDistance, pipeline, 
                    propertyName + ".searchDistance", Double.class, Integer.class, Length.class);

            maxWidth = getPossiblePipelinePropertyOverride(maxWidth, pipeline, 
                    propertyName + ".maxWidth", Double.class, Integer.class, Length.class);

            maxHeight = getPossiblePipelinePropertyOverride(maxHeight, pipeline, 
                    propertyName + ".maxHeight", Double.class, Integer.class, Length.class);

            margin = getPossiblePipelinePropertyOverride(margin, pipeline, 
                    propertyName + ".margin", Double.class, Integer.class, Length.class);
        }

        RotatedRect rect = findReclinearSymmetry(mat, (int)center.x, (int)center.y, expectedAngle, maxWidth+margin, maxHeight+margin, 
                searchDistance, searchAngle, minSymmetry, symmetryFunction, 
                subSampling, superSampling, smoothing, diagnostics, diagnosticsMap, new ScoreRange());
        return new Result(null, rect);
    }

    public static class ScoreRange {
        public double minScore = Double.POSITIVE_INFINITY;
        public double maxScore = Double.NEGATIVE_INFINITY;
        public double finalScore = Double.NEGATIVE_INFINITY;
        void add(double score) {
            if (score > 0) {
                minScore = Math.min(minScore, score); 
                maxScore = Math.max(maxScore, score);
                finalScore = Math.max(finalScore, score);
            }
        }
    }

    /**
     * The detection will recurse into a local search with finer subSampling. A range of iterationRadius*subSampling 
     * pixels around the preliminary best location will be searched.  
     */
    static final private int iterationRadius = 4;
    /**
     * The subSampling block size will be divided by iterationDivision when recursing into a local search. 
     */
    static final private int iterationDivision = 4;
    /**
     * Some extra debugging stuff used for development, that might be useful again in the future. DEBUG has levels 1 and 2.  
     */
    static final int DEBUG = 0;


    public static RotatedRect findReclinearSymmetry(Mat image, int xCenter, int yCenter, double expectedAngle,
            double maxWidth, double maxHeight, 
            double searchDistance, double searchAngle, double minSymmetry, SymmetryFunction symmetryFunction,
            int subSampling, int superSampling, int gaussianSmoothing, boolean diagnostics, boolean heatMap, ScoreRange scoreRange) throws Exception {
        boolean innermost = subSampling <= Math.max(1, -superSampling);
        // Image properties.
        final int channels = image.channels();
        final int width = image.cols();
        final int height = image.rows();
        // Some sanity checks.
        int maxDim = Math.min(width, height);
        maxWidth = Math.min(maxWidth, maxDim);
        maxHeight = Math.min(maxHeight, maxDim);
        // The maximum diagonal (even value).
        final int maxDiagonal = 2*(int) Math.ceil(Math.sqrt(maxWidth*maxWidth + maxHeight*maxHeight)/2);
        double dim = Math.max(maxWidth, maxHeight);
        searchDistance = Math.min(searchDistance, maxDim - dim);
        if (searchDistance < subSampling) {
            throw new Exception("Image too small for maxWidth, maxHeight");
        }
        final int subSamplingEff = Math.max(1, Math.min(subSampling, maxDiagonal/16));
        // Super sampling seems to only work reliably with 1 or 2. Sampling interference is a problem.
        final int superSamplingEff = (subSamplingEff == 1 ? Math.max(1, Math.min(2, superSampling)) : 1);
        final int searchDiameter = 2*(int) Math.ceil(searchDistance);

        // Get the pixels out of the Mat. 
        // TODO: optimize for a limited search angle, i.e. currently takes the slice for full rotation of the max rectangle. 
        int r = maxDiagonal/2;
        final int x0Pixels = Math.max(0, (xCenter - r - searchDiameter/2)/subSamplingEff)*subSamplingEff; 
        final int y0Pixels = Math.max(0, (yCenter - r - searchDiameter/2)/subSamplingEff)*subSamplingEff; 
        final int x1Pixels = Math.min(width/subSamplingEff, (xCenter + r + searchDiameter/2)/subSamplingEff)*subSamplingEff; 
        final int y1Pixels = Math.min(height/subSamplingEff, (yCenter + r + searchDiameter/2)/subSamplingEff)*subSamplingEff; 
        final int wPixels = x1Pixels - x0Pixels;
        final int hPixels = y1Pixels - y0Pixels;
        final int cxPixels = xCenter - x0Pixels;
        final int cyPixels = yCenter - y0Pixels;
        byte[] pixelSamples = new byte[width*hPixels*channels]; 
        image.get(y0Pixels, 0, pixelSamples);

        // First phase: determine the angle with the largest rectlinear cross-section contrast.
        int symmetrySearch = (superSamplingEff*searchDiameter/subSamplingEff/2)*2;
        int symmetryWidth= superSamplingEff*(int)maxWidth/subSamplingEff;
        int symmetryHeight = superSamplingEff*(int)maxHeight/subSamplingEff;
        final int wCross = symmetrySearch+symmetryWidth;
        final int hCross = symmetrySearch+symmetryHeight;
        final double cxCross = wCross/2; 
        final double cyCross = hCross/2; 
        //final double rSq = (int)Math.pow(Math.min(wPixels, hPixels), 2)/4;
        // Running best results.
        double scoreBest = Double.NEGATIVE_INFINITY;
        double angleBest = Double.NaN;
        double [] xCrossSection = new double[wCross*channels];
        double [] yCrossSection = new double[hCross*channels];
        double [] xCrossSectionN = new double[wCross*channels];
        double [] yCrossSectionN = new double[hCross*channels];
        double [] xCrossSectionFiltered = new double[wCross*channels];
        double [] yCrossSectionFiltered = new double[hCross*channels];
        double [] xBestCrossSection = new double[wCross*channels];
        double [] yBestCrossSection = new double[hCross*channels];
        // Note, the step angle depends on size of subject.
        double angleStep = Math.min(Math.toRadians(searchAngle)/4, subSamplingEff/Math.max(maxWidth,  maxHeight)/superSamplingEff);
        double a0 = Math.toRadians(expectedAngle - searchAngle);
        double a1 = Math.toRadians(expectedAngle + searchAngle)+angleStep/2;
        TreeMap<Double, Double> angleScore = null;
        if (heatMap) {
            angleScore = new TreeMap<>();
        }
        for (double angle = a0; angle <= a1; angle += angleStep) {
            // Note, this is the reverse rotation, i.e. angle is negative.
            double s = superSamplingEff*Math.sin(-angle)/subSamplingEff;
            double c = superSamplingEff*Math.cos(-angle)/subSamplingEff;
            // Reset cross-section- 
            Arrays.fill(xCrossSection, 0);
            Arrays.fill(yCrossSection, 0);
            Arrays.fill(xCrossSectionN, 0);
            Arrays.fill(yCrossSectionN, 0);
            // Calculate the cross-sections from the pixels.
            for (int y = 0, dy = -cyPixels, iy = 0; y < hPixels; y += subSamplingEff, dy += subSamplingEff, iy += width*channels*subSamplingEff) {
                double sy = s*dy;
                double cy = c*dy;
                for (int x = 0, dx = -cxPixels, idx = iy + x0Pixels*channels; x < wPixels; x += subSamplingEff, dx += subSamplingEff, idx += channels*subSamplingEff) {
                    double sx = s*dx;
                    double cx = c*dx;
                    // Note: this is a left-handed coordinate system, i.e. y pointing down.
                    double xCross = cx + sy + cxCross;
                    double yCross = -sx + cy + cyCross;
                    int ixCross = (int) Math.round(xCross);
                    int iyCross = (int) Math.round(yCross);
                    double xWeight1 = xCross + 0.5 - ixCross;
                    double xWeight0 = 1 - xWeight1;
                    double yWeight1 = yCross + 0.5 - iyCross;
                    double yWeight0 = 1 - yWeight1;
                    if (iyCross > 1 && iyCross < hCross) {
                        if (ixCross > 1 && ixCross < wCross) {
                            /*int dSq = dx*dx + dy*dy;
                            if (dSq < rSq)*/ {
                                for (int ch = 0; ch < channels; ch++) {
                                    int pixel = Byte.toUnsignedInt(pixelSamples[idx + ch]);
                                    int xai = ixCross*channels + ch;
                                    int yai = iyCross*channels + ch;
                                    xCrossSection[xai] += pixel*xWeight1;
                                    xCrossSection[xai - channels] += pixel*xWeight0;
                                    yCrossSection[yai] += pixel*yWeight1;
                                    yCrossSection[yai - channels] += pixel*yWeight0;
                                    xCrossSectionN[xai] += xWeight1;
                                    xCrossSectionN[xai - channels] += xWeight0;
                                    yCrossSectionN[yai] += yWeight1;
                                    yCrossSectionN[yai - channels] += yWeight0;
                                    if (DEBUG >= 2) {
                                        if (Math.abs(angle - (a0+a1)/2) < angleStep) {
                                            byte [] pixelData = new byte[channels];
                                            image.get(y0Pixels + y, x0Pixels + x, pixelData);
                                            if (ch == 2) {
                                                pixelData[ch] = (byte)(255.0*ixCross/wCross);
                                            }
                                            else if (ch == 1) {
                                                pixelData[ch] = (byte)(255.0*iyCross/hCross);
                                            }
                                            image.put(y0Pixels + y, x0Pixels + x, pixelData);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // Normalize
            for (int x = 0; x < wCross*channels; x++) {
                if (xCrossSectionN[x] > 0) {
                    xCrossSection[x] /= xCrossSectionN[x]; 
                }
            }
            for (int y = 0; y < hCross*channels; y++) {
                if (yCrossSectionN[y] > 0) {
                    yCrossSection[y] /= yCrossSectionN[y];
                }
            }
            // Note, we're using a gaussian kernel to get rid of sampling interferences especially at the 45° step angles.
            double[] kernel = getGaussianKernel(superSamplingEff, 0, (gaussianSmoothing*superSamplingEff)|1);
            applyKernelToCrossSection(channels, wCross, xCrossSection, kernel, xCrossSectionFiltered); 
            applyKernelToCrossSection(channels, hCross, yCrossSection, kernel, yCrossSectionFiltered); 
            // Analyze cross-sections contrast.
            double sumContrast = 
                    sumContrast(channels, wCross, xCrossSectionFiltered, xCrossSectionN)
                    + sumContrast(channels, hCross, yCrossSectionFiltered, yCrossSectionN);
            if (DEBUG >= 1) {
                System.out.print("subSampling "+subSamplingEff+(superSamplingEff > 1 ? " superSampling "+superSamplingEff : "")
                        +" angle "+Math.toDegrees(angle)+"° contrast "+sumContrast);
            }
            if (angleScore != null) {
                angleScore.put(angle, sumContrast);
            }
            // Take the best:
            if (scoreBest < sumContrast) {
                scoreBest = sumContrast;
                angleBest = angle;
                xBestCrossSection = xCrossSectionFiltered.clone();
                yBestCrossSection = yCrossSectionFiltered.clone();
                if (DEBUG >= 1) {
                    System.out.println(" * ");
                }
            }
            else {
                if (DEBUG >= 1) {
                    System.out.println("");
                }
            }
        }

        RotatedRect rect = null;

        // Find the symmetry in X/Y.
        double[] kernel = symmetryFunction.getKernel(superSamplingEff, superSamplingEff|1);
        double[] xCrossSectionSmoothed =
                applyKernelToCrossSection(channels, wCross, xBestCrossSection, kernel);
        double[] yCrossSectionSmoothed =
                applyKernelToCrossSection(channels, hCross, yBestCrossSection, kernel);
        ScoreRange xScoreRange = new ScoreRange();
        ScoreRange yScoreRange = new ScoreRange();
        Double xs = findCrossSectionSymmetry(channels, wCross, symmetrySearch, symmetryWidth, minSymmetry, xCrossSectionSmoothed, xScoreRange);
        Double ys = findCrossSectionSymmetry(channels, hCross, symmetrySearch, symmetryHeight, minSymmetry, yCrossSectionSmoothed, yScoreRange);
        // The final score is reset here so it will reflect the last pass' maximum score. 
        scoreRange.finalScore = 0;
        // Score is average of x, y.
        scoreRange.add((xScoreRange.maxScore + yScoreRange.maxScore)/2);
        if (xs != null && ys != null) {
            // Translate to image coordinates.
            double xT = xs - symmetrySearch/2.0;
            double yT = ys - symmetrySearch/2.0;
            double s = Math.sin(angleBest);
            double c = Math.cos(angleBest);
            double xR = xT*c + yT*s; 
            double yR = xT*-s + yT*c; 
            double xBest = xR*subSamplingEff/superSamplingEff + xCenter;
            double yBest = yR*subSamplingEff/superSamplingEff + yCenter;

            // Find the bounds.
            double[] ckernel = getGaussianKernel(superSamplingEff, 0, (gaussianSmoothing*superSamplingEff)|1);
            double[] xCrossSectionContrast =
                    applyKernelToCrossSection(channels, wCross, xBestCrossSection, ckernel);
            double[] yCrossSectionContrast =
                    applyKernelToCrossSection(channels, hCross, yBestCrossSection, ckernel);
            int wBest = findCrossSectionBounds(channels, xs, symmetryWidth, xCrossSectionContrast);
            int hBest = findCrossSectionBounds(channels, ys, symmetryHeight, yCrossSectionContrast);
            wBest = (wBest + ckernel.length)*subSamplingEff/superSamplingEff;
            hBest = (hBest + ckernel.length)*subSamplingEff/superSamplingEff;

            // Compose result.
            org.opencv.core.Point pt = new org.opencv.core.Point(xBest, yBest);
            Size sz = new Size(wBest, hBest);
            rect = new RotatedRect(pt, sz, Math.toDegrees(-angleBest)); 
            if (!innermost) {
                // Recursion into finer subSampling and local search.
                rect = findReclinearSymmetry(image, (int)xBest, (int)yBest,  Math.toDegrees(angleBest), maxWidth, maxHeight, 
                        subSamplingEff*iterationRadius, Math.toDegrees(angleStep)*iterationRadius, minSymmetry, symmetryFunction, 
                        subSamplingEff/iterationDivision, superSampling, gaussianSmoothing, diagnostics, heatMap, scoreRange);
            }
        }
        if ((innermost && diagnostics) || heatMap) {
            angleBest = (rect != null ? -Math.toRadians(rect.angle) : expectedAngle);
            double rxCenter = (rect != null ? rect.center.x : xCenter);
            double ryCenter = (rect != null ? rect.center.y : yCenter);
            double rWidth = (rect != null ? rect.size.width : maxWidth);
            double rHeight= (rect != null ? rect.size.height : maxHeight);
            double s = Math.sin(angleBest);
            double c = Math.cos(angleBest);
            double r0 = maxDiagonal*0.5;
            double r1 = maxDiagonal*0.6;
            double angleScoreMin = angleScore != null ? Collections.min(angleScore.values()) : 0;
            double angleScoreMax = angleScore != null ? Collections.max(angleScore.values()) : 0;
            double angleScoreFactor = (r1 - r0)/(angleScoreMax - angleScoreMin);
            double xsScore = xs != null ? xs : symmetrySearch/2.0;
            double ysScore = ys != null ? ys : symmetrySearch/2.0;
            double [] xCrossScoreMax = new double [channels];
            for (int i = 0; i < wCross*channels; i++) {
                int ch = i % channels;
                xCrossScoreMax[ch] = Math.max(xCrossScoreMax[ch], subSamplingEff*xCrossSectionSmoothed[i]); 
            }
            double [] yCrossScoreMax = new double [channels];
            for (int i = 0; i < hCross*channels; i++) {
                int ch = i % channels;
                yCrossScoreMax[ch] = Math.max(yCrossScoreMax[ch], subSamplingEff*yCrossSectionSmoothed[i]); 
            }

            // Very slow but simple. 
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    double dx = x - rxCenter;
                    double dy = y - ryCenter;
                    double dxT = dx*c + dy*-s;
                    double dyT = dx*s + dy*c;
                    double hairsIndicate = 0;
                    double angleIndicate = 0;
                    double crossIndicate = 0;
                    boolean north = false;

                    double ringRadius = Math.sqrt(dx*dx + dy*dy);
                    if (ringRadius > r1) {
                        if (heatMap && innermost) {
                            double xsT = dxT*superSamplingEff/subSamplingEff + symmetryWidth/2 + xsScore;
                            double ysT = dyT*superSamplingEff/subSamplingEff + symmetryHeight/2 + ysScore;
                            int xsi = (int)Math.round(xsT);
                            int ysi = (int)Math.round(ysT);
                            double xWeight1 = xsT - xsi + 0.5;
                            double yWeight1 = ysT - ysi + 0.5;
                            for (int ch = 0; ch < channels; ch++) {
                                if (xsi > 1 && xsi < wCross - 1) {
                                    crossIndicate = Math.max(
                                            (xCrossSectionSmoothed[xsi*channels + ch]*xWeight1 
                                                    + xCrossSectionSmoothed[(xsi - 1)*channels + ch]*(1 - xWeight1))
                                            /xCrossScoreMax[ch], crossIndicate);
                                }
                                if (ysi > 1 && ysi < hCross - 1) {
                                    crossIndicate = Math.max(
                                            (yCrossSectionSmoothed[ysi*channels + ch]*yWeight1 
                                                    + yCrossSectionSmoothed[(ysi - 1)*channels + ch]*(1 - yWeight1))
                                            /yCrossScoreMax[ch], crossIndicate);
                                }
                            }
                            crossIndicate = Math.pow(crossIndicate, 2);
                        }
                    }
                    else if (ringRadius > r0) {
                        if (angleScore != null && ringRadius < r1) {
                            // Note, in the tradition of the OpenPnP cross-hairs, 0° point is indicated at "north", not "east",
                            // Therefore rotate be +90° i.e. swap dx, dy and revert sign of dx (we're in the left-handed system where y points down). 
                            double ringAngle = Math.atan2(-dx, -dy);
                            while (ringAngle < a0) {
                                ringAngle += 2*Math.PI;
                            }
                            while (ringAngle > a1) {
                                ringAngle -= 2*Math.PI;
                            }
                            Entry<Double, Double> entry0 = angleScore.floorEntry(ringAngle);
                            Entry<Double, Double> entry1 = angleScore.ceilingEntry(ringAngle);
                            if (entry0 != null && entry1 != null) {
                                double da = entry1.getKey() - entry0.getKey();
                                double weight1 = (ringAngle - entry0.getKey())/da;
                                double weight0 = 1 - weight1;
                                double score = ((weight0*entry0.getValue() + weight1*entry1.getValue()) - angleScoreMin)*angleScoreFactor;
                                angleIndicate = Math.max(0, (0.8/Math.log(Math.E*subSamplingEff))*Math.min(1, (r0+score)-ringRadius));
                            }
                        }
                    }
                    else {
                        if (innermost && diagnostics) {
                            hairsIndicate = Math.max(Math.max(Math.max(
                                    (1 - Math.abs(dxT))*0.9,
                                    (1 - Math.abs(dyT))*0.9),
                                    (1 - Math.abs(Math.abs(dxT) - rWidth/2.0))*0.4),
                                    (1 - Math.abs(Math.abs(dyT) - rHeight/2.0))*0.4);
                            north = dyT < -rHeight/2.0 && Math.abs(dxT) < 1;
                        }
                    }
                    if (hairsIndicate > 0 || angleIndicate > 0 || crossIndicate > 0) {
                        double alpha = 0;
                        double alphaCompl = 1-alpha;
                        byte [] pixelData = new byte[channels];
                        image.get(y, x, pixelData);
                        if (channels == 3) {
                            int red = 0;
                            int green = 0;
                            int blue = 0;
                            if (hairsIndicate > 0) {
                                if (scoreRange.finalScore < minSymmetry) {
                                    red = 255;
                                }
                                else {
                                    green = 255;
                                    if (north) {
                                        red = 255;
                                    }
                                }
                            }
                            if (angleIndicate > 0) {
                                red = 255;
                                green = 255;
                                blue = 64;
                            }
                            if (crossIndicate > 0) {
                                blue = 255;
                            }
                            alpha = Math.max(Math.max(hairsIndicate, angleIndicate), crossIndicate);
                            alphaCompl = 1 - alpha;
                            if (alpha > 0) {
                                pixelData[2] = (byte) (alpha*red + alphaCompl*Byte.toUnsignedInt(pixelData[2]));
                                pixelData[1] = (byte) (alpha*green + alphaCompl*Byte.toUnsignedInt(pixelData[1]));
                                pixelData[0] = (byte) (alpha*blue + alphaCompl*Byte.toUnsignedInt(pixelData[0]));
                            }
                        }
                        else {
                            if (hairsIndicate > 0) {
                                alpha = hairsIndicate;
                                alphaCompl = 1 - alpha;
                                pixelData[0] = (byte) (alpha*255 + alphaCompl*Byte.toUnsignedInt(pixelData[2]));
                            }
                        }
                        image.put(y, x, pixelData);
                    }
                }
            }
        }
        if (scoreRange.finalScore < minSymmetry) {
            rect = null;
        }
        return rect;
    }

    protected static double sumContrast(final int channels, final int size,
            double[] crossSection, double[] crossSectionN) {
        double sumContrast = 0;
        for (int ch = 0; ch < channels; ch++) {
            // Scan for contrasts.
            Double v0 = null;
            for (int x = ch; x < size*channels; x += channels) {
                if (crossSectionN[x] > 0) {
                    double v = crossSection[x];
                    if (v0 != null) {
                        double dv = v - v0;
                        sumContrast += dv*dv;
                    }
                    v0 = v;
                }
            }
        }
        return sumContrast;
    }

    protected static double[] applyKernelToCrossSection(final int channels, final int size,
            double[] crossSection, double[] kernel) {
        double [] crossSectionFiltered = new double[crossSection.length];
        return applyKernelToCrossSection(channels, size, crossSection, kernel, crossSectionFiltered);
    }

    protected static double[] applyKernelToCrossSection(final int channels, final int size,
            double[] crossSection, double[] kernel, double[] crossSectionFiltered) {
        int kernelSize = kernel.length;
        int halfSize = kernelSize/2;
        for (int ch = 0; ch < channels; ch++) {
            for (int slot = halfSize; slot < size - halfSize; slot++) {
                double v = 0;
                for (int k = 0; k < kernelSize; k++) {
                    v += crossSection[(slot + k - halfSize)*channels + ch]*kernel[k];
                }
                crossSectionFiltered[slot*channels + ch] = Math.abs(v);
            }
            // HACK: fill up the margins
            for (int slot = 0; slot < halfSize; slot++) {
                crossSectionFiltered[slot*channels + ch] = crossSectionFiltered[halfSize*channels + ch]; 
            }
            for (int slot = size - halfSize; slot < size; slot++) {
                crossSectionFiltered[slot*channels + ch] = crossSectionFiltered[(size-halfSize-1)*channels + ch]; 
            }
        }
        return crossSectionFiltered;
    }

    protected static Double findCrossSectionSymmetry(final int channels, final int size,
            int symmetrySearch, int symmetrySize, double minSymmetry, 
            double[] crossSection, ScoreRange scoreRange) {
        //        // Baseline variance.
        //        double [] sumBase = new double[channels];
        //        double [] sumBaseSq = new double[channels];
        //        int [] nBase = new int [channels];
        //        for (int i = 0; i < size; i++) {
        //            for (int ch = 0; ch < channels; ch++) {
        //                double v = crossSection[i*channels + ch];
        //                sumBase[ch] += v;
        //                sumBaseSq[ch] += v*v;
        //                nBase[ch]++;
        //            }
        //        }
        //        double varianceBase = 0;
        //        for (int ch = 0; ch < channels; ch++) { 
        //            varianceBase += (sumBaseSq[ch] - sumBase[ch]*sumBase[ch]/nBase[ch])/nBase[ch];
        //        }
        // Find the best symmetry.
        Double coordBest = null;
        double scoreBest = minSymmetry;
        for (int s = 0; s <= symmetrySearch; s++) {
            double [] sum = new double[channels];
            double [] sumSq = new double[channels];
            double [] sumSym = new double[channels];
            double [] sumSymSq = new double[channels];
            int [] n = new int[channels];
            for (int left = s, right = s + symmetrySize - 1; left < s + symmetrySize/2; left++, right--) {
                for (int ch = 0; ch < channels; ch++) {
                    double vLeft = crossSection[left*channels + ch];
                    double vRight = crossSection[right*channels + ch];
                    double dv = Math.abs(vLeft - vRight);
                    sum[ch] += vLeft + vRight;
                    sumSq[ch] += vLeft*vLeft + vRight*vRight;
                    //sumSym[ch] += dv;
                    sumSymSq[ch] += dv*dv;
                    n[ch]++;
                }
            }

            double variance = 0;
            double varianceSym = 1e-8;
            for (int ch = 0; ch < channels; ch++) { 
                variance += (sumSq[ch] - (sum[ch]*sum[ch])/(2*n[ch]));
                varianceSym += (sumSymSq[ch] - (sumSym[ch]*sumSym[ch])/n[ch]);
            }
            double score = variance/varianceSym;
            scoreRange.add(score);
            if (DEBUG >= 1) {
                System.out.println("Symmetry s="+s+" variance="+variance+" varianceSym="+varianceSym+" score="+score);
            }
            if (scoreBest < score) {
                scoreBest = score;
                coordBest = s + 0.5;
            }
        }
        return coordBest;
    }

    protected static int findCrossSectionBounds(final int channels, double symmetry, int symmetrySize,
            double[] crossSection) {
        double maxSignal = 0;
        int dBest = symmetrySize;
        for (int d = symmetrySize/2-1; d > 0; d--) {
            double i = symmetry + symmetrySize/2 - 1;
            double signal = 0;
            for (int ch = 0; ch < channels; ch++) {
                signal += crossSection[(int) ((i - d)*channels + ch)]
                        + crossSection[(int) ((i + d)*channels + ch)];
            }
            if (signal > maxSignal*1.8) {
                maxSignal = signal;
                dBest = d*2;
            }
        }
        return dBest;
    }

    private static double gaussErrorFunction(double x) {
        //from http://picomath.org/javascript/erf.js.html

        // constants
        double a1 = 0.254829592;
        double a2 = -0.284496736;
        double a3 = 1.421413741;
        double a4 = -1.453152027;
        double a5 = 1.061405429;
        double p = 0.3275911;

        // Save the sign of x
        double sign = Math.signum(x);
        x = Math.abs(x);

        // A&S formula 7.1.26
        double t = 1.0/(1.0 + p*x);
        double y = 1.0 - (((((a5*t + a4)*t) + a3)*t + a2)*t + a1)*t*Math.exp(-x*x);

        return sign*y;
    }

    private static double getIntGaussian(double x, double mu, double sigma) {
        return 0.5 * gaussErrorFunction((x-mu)/(Math.sqrt(2) * sigma));
    }

    protected static double [] getGaussianKernel(double sigma, double mu, int kernelSize) {
        double xStart = -(kernelSize/2.0);
        double xEnd = (kernelSize/2.0);
        double step = 1;
        double [] kernel = new double [kernelSize];
        int bin = 0;
        double lastInt = getIntGaussian(xStart, mu, sigma);
        for (double xi = xStart; xi < xEnd; xi += step) {
            double newInt = getIntGaussian(xi + step, mu, sigma);
            kernel[bin++] = newInt - lastInt;
            lastInt = newInt;
        }

        // Sum of weights.
        double sum = 0;
        for (double c : kernel) {
            sum += c;
        }

        // Normalize.
        for (bin = 0; bin < kernel.length; bin++) {
            kernel[bin] /= sum;
        }
        return kernel;
    }
}
