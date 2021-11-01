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
    //    @Attribute(required = false)
    //    @Property(description = "Maximum number of targets to be found.")
    //    private int maxTargetCount = 1;

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

    public enum SymmetryFunction {
        Image,
        Constrast;

        double [] getKernel(int superSampling) {
            switch(this) {
                case Image: {
                    double [] kernel = new double [superSampling];
                    Arrays.fill(kernel, 1.0/superSampling);
                    return kernel;
                }
                case Constrast: {
                    double [] kernel = new double [superSampling*2];
                    Arrays.fill(kernel, 0, superSampling, 1.0/superSampling);
                    Arrays.fill(kernel, superSampling, superSampling*2, -1.0/superSampling);
                    return kernel;
                }
            }
            return null;
        }
    }

    @Attribute(required = false)
    @Property(description = "symmetry finding function.")
    private SymmetryFunction symmetryFunction = SymmetryFunction.Image;

    @Attribute(required = false)
    @Property(description = "minimum relative symmetry.")
    private double minSymmetry = 1.2;

    //    @Attribute(required = false)
    //    @Property(description = "Correlated minimum circular symmetry for multiple matches, i.e. other matches must have "
    //            + "at least this relative symmetry. Must be in the interval [0,1]. ")
    //    private double corrSymmetry = 0.0;
    //
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
    @Property(description = "Property name as controlled by the vision operation using this pipeline.<br/>"
            + "<ul><li><i>propertyName</i>.center</li></ul>"
            + "If set, these will override the properties configured here.")
    private String propertyName = "alignment";

    @Attribute(required = false)
    @Property(description = "Display matches with circle and cross-hairs.")
    private boolean diagnostics = false;

    @Attribute(required = false)
    @Property(description = "Overlay a heat map indicating the local circular symmetry.")
    private boolean heatMap = false;

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

    public boolean isHeatMap() {
        return heatMap;
    }

    public void setHeatMap(boolean heatMap) {
        this.heatMap = heatMap;
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

        }

        final double margin = 1.2;
        RotatedRect rect = findReclinearSymmetry(mat, (int)center.x, (int)center.y, expectedAngle, maxWidth*margin, maxHeight*margin, 
                searchDistance, searchAngle, minSymmetry, symmetryFunction, 
                subSampling, superSampling, diagnostics, heatMap, new ScoreRange());
        return new Result(null, rect);
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
            int subSampling, int superSampling, boolean diagnostics, boolean heatMap, ScoreRange scoreRange) throws Exception {
        boolean outermost = !Double.isFinite(scoreRange.finalScore);
        // Image properties.
        final int channels = image.channels();
        final int width = image.cols();
        final int height = image.rows();
        // Some sanity checks.
        int maxDim = Math.min(width, height);
        maxWidth = Math.min(maxWidth, maxDim);
        maxHeight = Math.min(maxHeight, maxDim);
        double dim = Math.max(maxWidth, maxHeight);
        searchDistance = Math.min(searchDistance, maxDim-dim);
        if (searchDistance < subSampling) {
            throw new Exception("Image too small for maxWidth, maxHeight");
        }
        // The maximum diagonal (even value).
        final int maxDiagonal = 2*(int) Math.ceil(Math.sqrt(maxWidth*maxWidth + maxHeight*maxHeight)/2);
        final int subSamplingEff = Math.max(1, Math.min(subSampling, maxDiagonal/8));
        final int superSamplingEff = (subSamplingEff == 1 ? Math.min(2, superSampling) : 1);
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
        final double rSq = (int)Math.pow(Math.min(wPixels, hPixels), 2)/4;
        // Running best results.
        double scoreBest = Double.NEGATIVE_INFINITY;
        double angleBest = Double.NaN;
        double [] xCrossSection = new double[wCross*channels];
        double [] yCrossSection = new double[hCross*channels];
        double [] xCrossSectionN = new double[wCross*channels];
        double [] yCrossSectionN = new double[hCross*channels];
        double [] xBestCrossSection = new double[wCross*channels];
        double [] yBestCrossSection = new double[hCross*channels];
        // Note, the step angle depends on size of subject.
        double angleStep = Math.min(Math.toRadians(searchAngle)/4, subSamplingEff/Math.max(maxWidth,  maxHeight)/superSamplingEff);
        double a0 = Math.toRadians(expectedAngle - searchAngle);
        double a1 = Math.toRadians(expectedAngle + searchAngle)+angleStep/2;
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
            // Analyze cross-sections contrast.
            double sumContrast = 0;
            int n = 0;
            for (int ch = 0; ch < channels; ch++) {
                // Scan for contrasts, we're using a 4 bin sum.
                Double v0 = null;
                for (int x = ch; x < (wCross-3)*channels; x += channels) {
                    if (xCrossSectionN[x] > 0) {
                        double v = xCrossSection[x] 
                                + xCrossSection[x+channels]
                                        + xCrossSection[x+channels*2]
                                                + xCrossSection[x+channels*3];
                        if (v0 != null) {
                            double dv = v - v0;
                            sumContrast += dv*dv;
                            n++;
                        }
                        v0 = v;
                    }
                }
                v0 = null;
                for (int y = ch; y < (hCross - 3)*channels; y += channels) {
                    if (yCrossSectionN[y] > 0) {
                        double v = yCrossSection[y]
                                + yCrossSection[y+channels]
                                        + yCrossSection[y+channels*2]
                                                + yCrossSection[y+channels*3];
                        if (v0 != null) {
                            double dv = v - v0;
                            sumContrast += dv*dv;
                            n++;
                        }
                        v0 = v;
                    }
                }
            }
            sumContrast /= n;
            if (DEBUG >= 1) {
                System.out.print("subSampling "+subSamplingEff+(superSamplingEff > 1 ? " superSampling "+superSamplingEff : "")+" angle "+Math.toDegrees(angle)+"° "+sumContrast + " n="+n);
            }
            // Take the best:
            if (scoreBest < sumContrast) {
                scoreBest = sumContrast;
                angleBest = angle;
                xBestCrossSection = xCrossSection.clone();
                yBestCrossSection = yCrossSection.clone();
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

        // The final score is reset here so it will reflect the last pass' maximum score. 
        scoreRange.finalScore = 0;
        RotatedRect rect = null;

        // Find the symmetry in X/Y.
        double[] kernel = symmetryFunction.getKernel(superSampling);
        double[] xCrossSectionSmoothed =
                crossSectionWithKernel(channels, wCross, xBestCrossSection, kernel);
        double[] yCrossSectionSmoothed =
                crossSectionWithKernel(channels, hCross, yBestCrossSection, kernel);
        int symmetrySearchK = symmetrySearch - kernel.length;
        ScoreRange xScoreRange = new ScoreRange();
        ScoreRange yScoreRange = new ScoreRange();
        Double xs = findCrossSectionSymmetry(channels, wCross, symmetrySearchK, symmetryWidth, minSymmetry, xCrossSectionSmoothed, xScoreRange);
        Double ys = findCrossSectionSymmetry(channels, hCross, symmetrySearchK, symmetryHeight, minSymmetry, yCrossSectionSmoothed, yScoreRange);
        if (xs != null && ys != null) {
            // Score is average of x, y.
            scoreRange.add((xScoreRange.maxScore + yScoreRange.maxScore)/2);
            // Translate to image coordinates.
            double xT = xs - symmetrySearchK/2.0;
            double yT = ys - symmetrySearchK/2.0;
            double s = Math.sin(angleBest);
            double c = Math.cos(angleBest);
            double xR = xT*c + yT*s; 
            double yR = xT*-s + yT*c; 
            double xBest = xR*subSamplingEff/superSamplingEff + xCenter;
            double yBest = yR*subSamplingEff/superSamplingEff + yCenter;

            // Find the bounds
            double[] ckernel = symmetryFunction/*SymmetryFunction.Constrast*/.getKernel(4);
            double[] xCrossSectionContrast =
                    crossSectionWithKernel(channels, wCross, xBestCrossSection, ckernel);
            double[] yCrossSectionContrast =
                    crossSectionWithKernel(channels, hCross, yBestCrossSection, ckernel);
            int wBest = findCrossSectionBounds(channels, xs, symmetryWidth, xCrossSectionContrast);
            int hBest = findCrossSectionBounds(channels, ys, symmetryHeight, yCrossSectionContrast);
            wBest = (wBest + ckernel.length)*subSamplingEff/superSamplingEff;
            hBest = (hBest + ckernel.length)*subSamplingEff/superSamplingEff;

            // Compose result.
            org.opencv.core.Point pt = new org.opencv.core.Point(xBest, yBest);
            Size sz = new Size(wBest, hBest);
            rect = new RotatedRect(pt, sz, Math.toDegrees(-angleBest)); 
            if (subSamplingEff > 1) {
                // Recursion into finer subSampling and local search.
                rect = findReclinearSymmetry(image, (int)xBest, (int)yBest,  Math.toDegrees(angleBest), maxWidth, maxHeight, 
                        subSamplingEff*iterationRadius, Math.toDegrees(angleStep)*iterationRadius, minSymmetry, symmetryFunction, 
                        subSamplingEff/iterationDivision, superSampling, diagnostics, heatMap, scoreRange);
            }
        }
        if (outermost && diagnostics && rect != null) {
            angleBest = -Math.toRadians(rect.angle);
            double s = Math.sin(angleBest);
            double c = Math.cos(angleBest);
            // Very slow but simple. 
            for (int x = x0Pixels; x < x1Pixels; x++) {
                for (int y = y0Pixels; y < y1Pixels; y++) {
                    double dx = x - rect.center.x;
                    double dy = y - rect.center.y;
                    double dxT = dx*c + dy*-s;
                    double dyT = dx*s + dy*c;
                    double indicate = Math.max(Math.max(Math.max(
                            (1 - Math.abs(dxT))*0.5,
                            (1 - Math.abs(dyT))*0.5),
                            1 - Math.abs(Math.abs(dxT) - rect.size.width/2.0)),
                            1 - Math.abs(Math.abs(dyT) - rect.size.height/2.0));
                    if (indicate > 0) {
                        double alpha = 0;
                        double alphaCompl = 1-alpha;
                        byte [] pixelData = new byte[channels];
                        image.get(y, x, pixelData);
                        if (channels == 3) {
                            int red = 0;
                            int green = 0;
                            int blue = 0;
                            if (indicate > 0) {
                                red = 0;
                                green = 255;
                                blue = 0;
                                alpha = indicate;
                                alphaCompl = 1 - alpha;
                            }
                            if (alpha > 0) {
                                pixelData[2] = (byte) (alpha*red + alphaCompl*Byte.toUnsignedInt(pixelData[2]));
                                pixelData[1] = (byte) (alpha*green + alphaCompl*Byte.toUnsignedInt(pixelData[1]));
                                pixelData[0] = (byte) (alpha*blue + alphaCompl*Byte.toUnsignedInt(pixelData[0]));
                            }
                        }
                        else {
                            if (indicate > 0) {
                                alpha = indicate;
                                alphaCompl = 1 - alpha;
                                pixelData[0] = (byte) (alpha*255 + alphaCompl*Byte.toUnsignedInt(pixelData[2]));
                            }
                        }
                        image.put(y, x, pixelData);
                    }
                }
            }
        }
        return rect;
    }

    protected static double[] crossSectionWithKernel(final int channels, final int crossSize,
            double[] crossSection, double[] kernel) {
        double [] crossSectionKernel = new double[crossSection.length];
        for (int ch = 0; ch < channels; ch++) {
            for (int slot = 0; slot < crossSize - kernel.length; slot++) {
                double v = 0;
                for (int k = 0; k < kernel.length; k++) {
                    v += crossSection[(slot + k)*channels + ch];
                }
                crossSectionKernel[slot*channels + ch] = Math.abs(v);
            }
        }
        return crossSectionKernel;
    }

    protected static Double findCrossSectionSymmetry(final int channels, final int crossSize,
            int symmetrySearch, int symmetrySize, double minSymmetry, 
            double[] crossSection, ScoreRange scoreRange) {
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
                    double dv = (vLeft - vRight);
                    sum[ch] += vLeft;
                    sumSq[ch] += vLeft*vLeft;
                    sum[ch] += vRight;
                    sumSq[ch] += vRight*vRight;
                    sumSym[ch] += dv;
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
            double[] crossSectionContrast) {
        double maxStep = 0;
        int dBest = symmetrySize;
        for (int d = symmetrySize/2-1; d > 0; d--) {
            double i = symmetry + symmetrySize/2 - 1;
            double step = 0;
            for (int ch = 0; ch < channels; ch++) {
                step += crossSectionContrast[(int) ((i - d)*channels + ch)]
                        + crossSectionContrast[(int) ((i + d)*channels + ch)];
            }
            if (step > maxStep*2) {
                maxStep = step;
                dBest = d*2;
            }
        }
        return dBest;
    }
}
