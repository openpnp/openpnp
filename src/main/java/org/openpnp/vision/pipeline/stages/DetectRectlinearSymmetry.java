/*
 * Copyright (C) 2022 <mark@makr.zone>
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
import org.openpnp.util.KernelUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Property;
import org.openpnp.vision.pipeline.Stage;
import org.openpnp.vision.pipeline.stages.DetectRectlinearSymmetry.SymmetryFunction;
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
    @Property(description = "Search distance around the center.")
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
    @Property(description = "Margin around the detected object, used to detect edges.")
    private int margin = 40;

    @Attribute(required = false)
    @Property(description = "When finding symmetry, a cross-section pixel count of less than the <strong>minFeatureSize</strong> is ignored.<br/>"
            + "This is used to remove masking imperfections, i.e. specks up to a certain size and frequency in a masked image. "
            + "Requires an input image that was masked with a Threshold, MaskHSV stage, etc., i.e. where background pixels are truly black.<br/>"
            + "This is only relevant, when <strong>OutlineSymmetryMasked</strong> is used.")
    private double minFeatureSize = 40;

    @Attribute(required = false)
    @Property(description = "Tells the stage whether the part is symmetric across its width. The stage then either takes the symmetricFunction, or the asymmetricFunction.")
    private boolean symmetricAcrossWidth = true;

    @Attribute(required = false)
    @Property(description = "Tells the stage whether the part is symmetric across its height. The stage then either takes the symmetricFunction, or the asymmetricFunction.")
    private boolean symmetricAcrossHeight = true;

    @Attribute(required = false)
    @Property(description = "Determines how the cross-section is evaluated for <strong>symmetric</strong> parts.<br/><ul>"
            + "<li><strong>FullSymmetry</strong> looks for full inner and outline symmetry. Use for truly symmetric subjects and best precision.</li>"
            + "<li><strong>EdgeSymmetry</strong> looks for full inner and outline symmetry of edges. Use for partially "
            + "symmetric subjects, where some, but not all features are present on both sides, or where shades differ.</li>"
            + "<li><strong>OutlineSymmetry</strong> looks for outline symmetry only. Used for subjects that are symmetric "
            + "on their outline, but not on the inside.</li>"
            + "<li><strong>OutlineEdgeSymmetry</strong> looks for outline symmetry of edges only. Used for subjects that are symmetric "
            + "on their outline, but not on the inside, and where some, but not all features are present on both sides, or where shades differ.</li>"
            + "<li><strong>OutlineSymmetryMasked</strong> looks for outline mask symmetry only. Use for quite asymmetric subjects. "
            + "Requires setting a mask <strong>threshold</strong>.<br/>"
            + "</li></ul>")
    private SymmetryFunction symmetricFunction = SymmetryFunction.FullSymmetry;

    @Attribute(required = false)
    @Property(description = "Determines how the cross-section is evaluated for <strong>asymmetric</strong> parts.<br/><ul>"
            + "<li><strong>FullSymmetry</strong> looks for full inner and outline symmetry. Use for truly symmetric subjects and best precision.</li>"
            + "<li><strong>EdgeSymmetry</strong> looks for full inner and outline symmetry of edges. Use for partially "
            + "symmetric subjects, where some, but not all features are present on both sides, or where shades differ.</li>"
            + "<li><strong>OutlineSymmetry</strong> looks for outline symmetry only. Used for subjects that are symmetric "
            + "on their outline, but not on the inside.</li>"
            + "<li><strong>OutlineEdgeSymmetry</strong> looks for outline symmetry of edges only. Used for subjects that are symmetric "
            + "on their outline, but not on the inside, and where some, but not all features are present on both sides, or where shades differ.</li>"
            + "<li><strong>OutlineSymmetryMasked</strong> looks for outline mask symmetry only. Use for quite asymmetric subjects. "
            + "Requires setting a mask <strong>threshold</strong>.<br/>"
            + "</li></ul>")
    private SymmetryFunction asymmetricFunction = SymmetryFunction.OutlineSymmetryMasked;

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
    @Property(description = "Gamma to be applied to the image. The input signal is raised to the power gamma. "
            + "With gammas > 1.0 the bright image parts are emphasized.")
    private int gamma = 2;

    @Attribute(required = false)
    @Property(description = "Luminance threshold to be used for masking in the OutlineSymmetryMasked option.")
    private int threshold = 128;

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

    public double getMinFeatureSize() {
        return minFeatureSize;
    }

    public void setMinFeatureSize(double minFeatureSize) {
        this.minFeatureSize = minFeatureSize;
    }

    public SymmetryFunction getSymmetricFunction() {
        return symmetricFunction;
    }

    public void setSymmetricFunction(SymmetryFunction symmetryAcrossWidth) {
        this.symmetricFunction = symmetryAcrossWidth;
    }

    public SymmetryFunction getAsymmetricFunction() {
        return asymmetricFunction;
    }

    public void setAsymmetricFunction(SymmetryFunction symmetryAcrossHeight) {
        this.asymmetricFunction = symmetryAcrossHeight;
    }

    public boolean isSymmetricAcrossWidth() {
        return symmetricAcrossWidth;
    }

    public void setSymmetricAcrossWidth(boolean symmetricAcrossWidth) {
        this.symmetricAcrossWidth = symmetricAcrossWidth;
    }

    public boolean isSymmetricAcrossHeight() {
        return symmetricAcrossHeight;
    }

    public void setSymmetricAcrossHeight(boolean symmetricAcrossHeight) {
        this.symmetricAcrossHeight = symmetricAcrossHeight;
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

    public int getGamma() {
        return gamma;
    }

    public void setGamma(int gamma) {
        this.gamma = gamma;
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

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


    /**
     * Determines how the cross-section across the height of the detected subject is evaluated.<br/><ul>
     * <li><strong>FullSymmetry</strong> looks for full inner and outline symmetry. Use for truly symmetric subjects and best precision.</li>
     * <li><strong>EdgeSymmetry</strong> looks for full inner and outline symmetry of edges. Use for partially 
     * symmetric subjects, where some, but not all features are present on both sides, or where shades differ.</li>
     * <li><strong>OutlineSymmetry</strong> looks for outline symmetry only. Used for subjects that are symmetric 
     * on their outline, but not on the inside.</li>
     * <li><strong>OutlineEdgeSymmetry</strong> looks for outline symmetry of edges only. Used for subjects that are symmetric 
     * on their outline, but not on the inside, and where some, but not all features are present on both sides, or where shades differ.</li>
     * <li><strong>OutlineSymmetryMasked</strong> looks for outline mask symmetry only. Use for quite asymmetric subjects. 
     * Requires an input image that was masked with a Threshold, MaskHSV stage, etc., i.e. where background pixels are truly black.<br/>
     * </li></ul>
     *
     */
    public enum SymmetryFunction {
        FullSymmetry,
        EdgeSymmetry,
        OutlineSymmetry,
        OutlineEdgeSymmetry,
        OutlineSymmetryMasked;

        public boolean isEdge() {
            switch(this) {
                case FullSymmetry:
                case OutlineSymmetry: 
                case OutlineSymmetryMasked: 
                    return false;
                case EdgeSymmetry:
                case OutlineEdgeSymmetry:
                    return true;
            }
            return false;
        }

        public boolean isOutline() {
            return this == OutlineSymmetry || this == OutlineEdgeSymmetry || isMasked();
        }

        public boolean isMasked() {
            return this == OutlineSymmetryMasked;
        }

        double [] getKernel(int size) {
            int sigma = Math.max(1,  size/2);
            size |= 1;
            if (isEdge()) {
                double [] gauss = KernelUtils.getGaussianKernel(sigma, 0, size);
                double [] kernel = new double [size+1];
                for (int i = 0; i < size; i++) {
                    kernel[i] += gauss[i];
                    kernel[i+1] -= gauss[i];
                }
                return kernel;
            }
            else {
                double [] kernel = KernelUtils.getGaussianKernel(sigma, 0, size);
                return kernel;
            }
        }

        public double getOffset() {
            if (isEdge()) { 
                return 0.0;
            }
            return 0.5;
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
        double minFeatureSize = getMinFeatureSize();
        int threshold = getThreshold();
        boolean symmetricAcrossWidth = isSymmetricAcrossWidth();
        boolean symmetricAcrossHeight = isSymmetricAcrossHeight();

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

            minFeatureSize = getPossiblePipelinePropertyOverride(minFeatureSize, pipeline, 
                    propertyName + ".minFeatureSize", Double.class, Integer.class, Length.class);

            threshold = getPossiblePipelinePropertyOverride(threshold, pipeline, 
                    propertyName + ".threshold", Integer.class);

            symmetricAcrossWidth = getPossiblePipelinePropertyOverride(symmetricAcrossWidth, pipeline, 
                    propertyName + ".symmetricAcrossWidth");

            symmetricAcrossHeight = getPossiblePipelinePropertyOverride(symmetricAcrossHeight, pipeline, 
                    propertyName + ".symmetricAcrossHeight");
        }

        RotatedRect rect = findReclinearSymmetry(mat, (int)center.x, (int)center.y, expectedAngle, 
                maxWidth+margin, maxHeight+margin, searchDistance, searchAngle, 
                minSymmetry, 
                symmetricAcrossWidth ? getSymmetricFunction() :  getAsymmetricFunction(), 
                symmetricAcrossHeight ? getSymmetricFunction() :  getAsymmetricFunction(), 
                minFeatureSize,
                subSampling, superSampling, smoothing, gamma,
                threshold, diagnostics, diagnosticsMap, new ScoreRange());
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
     * The detection will recurse into a local search with finer subSampling. An angle of iterationAngle*step 
     * around the preliminary best angle will be searched.  
     */
    static final private double iterationAngle = 1.1;
    /**
     * The detection will recurse into a local search with finer subSampling. A range of iterationRadius*subSampling 
     * pixels around the preliminary best symmetry axis will be searched.  
     */
    static final private double iterationRadius = 4;
    /**
     * The subSampling block size will be divided by iterationDivision when recursing into a local search. 
     */
    static final private int iterationDivision = 4;
    /**
     * Some extra debugging stuff used for development, that might be useful again in the future. DEBUG has levels 1 and 2.  
     */
    static final int DEBUG = 0;

    /**
     * Find the angle, location and bounds of the subject with largest rectlinear symmetry. 
     * 
     * @param image                 The image to be searched.
     * @param xCenter               Center, around which the search is to be performed.
     * @param yCenter
     * @param expectedAngle         Expected angle of the subject.
     * @param maxWidth              Maximum width and height of the searched subject. Must include a margin around it, to detect edges. 
     * @param maxHeight
     * @param searchDistance        Maximum search distance around the center.
     * @param searchAngle           Maximum search angle on both sides of the expected angle.
     * @param minSymmetry           Minimum symmetry for a successful detection. Values higher than 1.0 mean symmetry is present.
     * @param xSymmetryFunction     The symmetry function determines, how the cross-section is interpreted to find symmetry. See {@link SymmetryFunction} 
     * @param ySymmetryFunction
     * @param minFeatureSize        Minimum feature size in pixels, if the {@link SymmetryFunction.OutlineSymmetryMasked} is used. 
     * @param subSampling           To speed up things, the image is first sub-sampled, i.e. only every subSampling × subSampling pixel is analyzed.
     *                              Once a preliminary match is obtained, recursion is used to refine it.  
     * @param superSampling         In the final recursion, superSampling is applied, i.e. the sampled pixels are binned into a finder cross-section, 
     *                              allowing for sub-pixel precision. 
     * @param gaussianSmoothing     Cross-sections are subject to pixel grid interferences if the sampling angle is at 45° multiples. Using Gaussian
     *                              smoothing with a kernel of this size, this is remedied. 
     * @param gamma                 Pixel luminance is raised to the power of gamma. Choosing > 1 gammas allows for boosting bright areas.
     * @param threshold             Pixel luminance threshold for the {@link SymmetryFunction} OutlineSymmetryMasked option.
     * @param diagnostics           Overlay diagnostic cross-hairs and bounds on top of the image.
     * @param diagnosticMap         Overlay diagnostic maps for angular contrast and cross-section profiles on top of the image.
     * @param scoreRange            Returns symmetry score ranges.
     * @return
     * @throws Exception
     */
    public static RotatedRect findReclinearSymmetry(Mat image, int xCenter, int yCenter, double expectedAngle,
            double maxWidth, double maxHeight, double searchDistance, double searchAngle,  
            double minSymmetry, SymmetryFunction xSymmetryFunction, SymmetryFunction ySymmetryFunction, double minFeatureSize,
            int subSampling, int superSampling, int gaussianSmoothing, double gamma,
            int threshold, boolean diagnostics, boolean diagnosticMap, ScoreRange scoreRange) throws Exception {
        boolean innermost = subSampling <= Math.max(1, -superSampling);
        // Image properties.
        final int channels = image.channels();
        final int width = image.cols();
        final int height = image.rows();
        // Some sanity checks.
        int maxDim = Math.min(width, height);
        maxWidth = Math.min(maxWidth, maxDim - subSampling*4);
        maxHeight = Math.min(maxHeight, maxDim - subSampling*4);
        // The maximum diagonal (even value).
        final int maxDiagonal = 2*(int) Math.ceil(Math.sqrt(maxWidth*maxWidth + maxHeight*maxHeight)/2);
        double maxSpan  = Math.max(maxWidth, maxHeight);
        searchDistance = Math.min(searchDistance, maxDim - maxSpan);
        if (searchDistance < subSampling) {
            throw new Exception("Image too small for maxWidth, maxHeight");
        }
        final int subSamplingEff = Math.max(1, Math.min(subSampling, Math.min(maxDiagonal/16, (int)searchDistance/2)));
        // Super sampling seems to only work reliably with 2. Sampling vs. pixel grid interference seems to be a big problem.
        final int superSamplingEff = (subSamplingEff == 1 ? Math.max(1, Math.min(maxDiagonal/100, superSampling)) : 1);
        final int searchDiameter = 2*(int) Math.ceil(searchDistance);

        // Get the pixels out of the Mat. 
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
        double [] xCrossSectionN = new double[wCross];
        double [] yCrossSectionN = new double[hCross];
        double [] xCrossSectionMasked = new double[wCross];
        double [] yCrossSectionMasked = new double[hCross];
        double [] xCrossSectionFiltered = new double[wCross*channels];
        double [] yCrossSectionFiltered = new double[hCross*channels];
        double [] xBestCrossSection = new double[wCross*channels];
        double [] yBestCrossSection = new double[hCross*channels];
        double [] xBestCrossSectionMasked = new double[wCross];
        double [] yBestCrossSectionMasked = new double[hCross];
        // Note, the step angle depends on size of subject.
        double angleStep = Math.max(0.0001, Math.min(Math.toRadians(searchAngle)/4, subSamplingEff/maxSpan/superSamplingEff));
        double a0 = Math.toRadians(expectedAngle - searchAngle);
        double a1 = Math.toRadians(expectedAngle + searchAngle)+angleStep/2;
        TreeMap<Double, Double> angleScore = null;
        if (diagnosticMap) {
            angleScore = new TreeMap<>();
        }
        double[] kernel = KernelUtils.getGaussianKernel(superSamplingEff, 0, (gaussianSmoothing*superSamplingEff)|1);
        double thresholdLuminance = Math.pow(threshold, gamma)*channels;

        // Determine the angle with the largest rectlinear cross-section contrast.
        for (double angle = a0; angle <= a1; angle += angleStep) {
            // Note, this is the reverse rotation, i.e. angle is negative.
            double s = superSamplingEff*Math.sin(-angle)/subSamplingEff;
            double c = superSamplingEff*Math.cos(-angle)/subSamplingEff;
            // Reset cross-sections. 
            Arrays.fill(xCrossSection, 0);
            Arrays.fill(yCrossSection, 0);
            Arrays.fill(xCrossSectionN, 0);
            Arrays.fill(yCrossSectionN, 0);
            Arrays.fill(xCrossSectionMasked, 0);
            Arrays.fill(yCrossSectionMasked, 0);
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
                                double luminance = 0;
                                for (int ch = 0; ch < channels; ch++) {
                                    int xai = ixCross*channels + ch;
                                    int yai = iyCross*channels + ch;
                                    double pixel = Math.pow(Byte.toUnsignedInt(pixelSamples[idx + ch]), gamma);
                                    luminance += pixel;
                                    xCrossSection[xai] += pixel*xWeight1;
                                    xCrossSection[xai - channels] += pixel*xWeight0;
                                    yCrossSection[yai] += pixel*yWeight1;
                                    yCrossSection[yai - channels] += pixel*yWeight0;
                                    if (DEBUG >= 2) {
                                        if (Math.abs(angle - (a0+a1)/2) < angleStep) {
                                            byte [] pixelData = new byte[channels];
                                            image.get(y0Pixels + y, x0Pixels + x, pixelData);
                                            if (ch == 2) {
                                                pixelData[ch] = (byte)(127.0*ixCross/wCross + pixelData[ch]/2);
                                            }
                                            else if (ch == 1) {
                                                pixelData[ch] = (byte)(127.0*iyCross/hCross + pixelData[ch]/2);
                                            }
                                            image.put(y0Pixels + y, x0Pixels + x, pixelData);
                                        }
                                    }
                                }
                                xCrossSectionN[ixCross] += xWeight1;
                                xCrossSectionN[ixCross - 1] += xWeight0;
                                yCrossSectionN[iyCross] += yWeight1;
                                yCrossSectionN[iyCross - 1] += yWeight0;
                                if (luminance > thresholdLuminance) {
                                    xCrossSectionMasked[ixCross] += xWeight1;
                                    xCrossSectionMasked[ixCross - 1] += xWeight0;
                                    yCrossSectionMasked[iyCross] += yWeight1;
                                    yCrossSectionMasked[iyCross - 1] += yWeight0;
                                }
                            }
                        }
                    }
                }
            }
            // Normalize
            for (int x = 0; x < wCross; x++) {
                if (xCrossSectionN[x] > 0) {
                    for (int ch = 0; ch < channels; ch++) {
                        xCrossSection[x*channels + ch] /= xCrossSectionN[x];
                    }
                }
            }
            for (int y = 0; y < hCross; y++) {
                if (yCrossSectionN[y] > 0) {
                    for (int ch = 0; ch < channels; ch++) {
                        yCrossSection[y*channels + ch] /= yCrossSectionN[y];
                    }
                }
            }
            // We're using a gaussian kernel to get rid of sampling interferences especially at the 45° step angles.
            KernelUtils.applyKernel(channels, wCross, xCrossSection, kernel, xCrossSectionFiltered); 
            KernelUtils.applyKernel(channels, hCross, yCrossSection, kernel, yCrossSectionFiltered); 
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
                xBestCrossSectionMasked = xCrossSectionMasked.clone();
                yBestCrossSectionMasked = yCrossSectionMasked.clone();
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

        if (xSymmetryFunction.isMasked()) {
            applyMasked(channels, wCross, minFeatureSize, subSamplingEff, superSamplingEff, 
                    xBestCrossSectionMasked, kernel, xBestCrossSection);
        }
        if (ySymmetryFunction.isMasked()) {
            applyMasked(channels, hCross, minFeatureSize, subSamplingEff, superSamplingEff,  
                    yBestCrossSectionMasked, kernel, yBestCrossSection);
        }
        // Find the symmetry in X/Y.
        double[] xKernel = xSymmetryFunction.getKernel(1);
        double[] xCrossSectionSymmetry =
                KernelUtils.applyKernel(channels, wCross, xBestCrossSection, xKernel);
        double[] yKernel = ySymmetryFunction.getKernel(1);
        double[] yCrossSectionSymmetry =
                KernelUtils.applyKernel(channels, hCross, yBestCrossSection, yKernel);
        ScoreRange xScoreRange = new ScoreRange();
        ScoreRange yScoreRange = new ScoreRange();
        Double xs = findCrossSectionSymmetry(channels, wCross, symmetrySearch, symmetryWidth, xSymmetryFunction, 
                xCrossSectionSymmetry, xScoreRange);
        Double ys = findCrossSectionSymmetry(channels, hCross, symmetrySearch, symmetryHeight, ySymmetryFunction, 
                yCrossSectionSymmetry, yScoreRange);
        // The final score is reset here so in recursion, it will reflect the last pass' maximum score. 
        scoreRange.finalScore = 0;
        // Score is the worse of x, y symmetry.
        scoreRange.add(Math.min(xScoreRange.maxScore, yScoreRange.maxScore));
        // Process the results of the symmetry search.
        RotatedRect rect = null;
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
            int wBest = findCrossSectionBounds(channels, xs, symmetryWidth, xBestCrossSection);
            int hBest = findCrossSectionBounds(channels, ys, symmetryHeight, yBestCrossSection);
            wBest = wBest*subSamplingEff/superSamplingEff + 2;
            hBest = hBest*subSamplingEff/superSamplingEff + 2;

            // Compose result.
            org.opencv.core.Point pt = new org.opencv.core.Point(xBest, yBest);
            Size sz = new Size(wBest, hBest);
            rect = new RotatedRect(pt, sz, Math.toDegrees(-angleBest)); 
            if (!innermost) {
                // Recursion into finer subSampling and local search.
                // Need to assess the maximum error angle by the detected subject size, i.e. if a very small
                // subject is detected with large maxSpan, the detected angle can be off considerably.    
                double angleError = angleStep*maxSpan/Math.max(wBest, hBest);
                rect = findReclinearSymmetry(image, (int)xBest, (int)yBest,  Math.toDegrees(angleBest), 
                        Math.min(maxWidth, wBest+subSamplingEff*iterationRadius*2), 
                        Math.min(maxHeight, hBest+subSamplingEff*iterationRadius*2), 
                        subSamplingEff*iterationRadius, 
                        Math.toDegrees(angleError)*iterationAngle,  
                        minSymmetry, xSymmetryFunction, ySymmetryFunction, minFeatureSize,
                        subSamplingEff/iterationDivision, superSampling, gaussianSmoothing, gamma,  
                        threshold, diagnostics, diagnosticMap, scoreRange);
            }
        }
        // Draw the diagnostic info onto the working image.
        if ((innermost && diagnostics) || diagnosticMap) {
            angleBest = (rect != null ? -Math.toRadians(rect.angle) : expectedAngle);
            double rxCenter = (rect != null ? rect.center.x : xCenter);
            double ryCenter = (rect != null ? rect.center.y : yCenter);
            double rWidth = (rect != null ? rect.size.width : maxWidth);
            double rHeight= (rect != null ? rect.size.height : maxHeight);
            double s = Math.sin(angleBest);
            double c = Math.cos(angleBest);
            double r0 = Math.sqrt(rWidth*rWidth + rHeight*rHeight)*0.5 + 20;
            double r1 = r0*1.05 + 40;
            double angleScoreMin = angleScore != null ? Collections.min(angleScore.values()) : 0;
            double angleScoreMax = angleScore != null ? Collections.max(angleScore.values()) : 0;
            double angleScoreFactor = (r1 - r0)/(angleScoreMax - angleScoreMin);
            double xsScore = xs != null ? xs : symmetrySearch/2.0;
            double ysScore = ys != null ? ys : symmetrySearch/2.0;
            if (xSymmetryFunction.isOutline()) {
                applyCrossSectionContour(channels, xsScore, symmetryWidth, wCross,
                        xCrossSectionSymmetry);
            }
            if (ySymmetryFunction.isOutline()) {
                applyCrossSectionContour(channels, ysScore, symmetryHeight, hCross,
                        yCrossSectionSymmetry);
            }
            double [] xCrossScoreMax = new double [channels];
            for (int i = 0; i < wCross*channels; i++) {
                int ch = i % channels;
                xCrossScoreMax[ch] = Math.max(xCrossScoreMax[ch], xCrossSectionSymmetry[i]); 
            }
            double [] yCrossScoreMax = new double [channels];
            for (int i = 0; i < hCross*channels; i++) {
                int ch = i % channels;
                yCrossScoreMax[ch] = Math.max(yCrossScoreMax[ch], yCrossSectionSymmetry[i]); 
            }

            // Slow but simple: render for each pixel.
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
                        // Overlay a cross-section map.
                        if (diagnosticMap && innermost) {
                            double xsT = dxT*superSamplingEff/subSamplingEff + symmetryWidth/2 + xsScore;
                            double ysT = dyT*superSamplingEff/subSamplingEff + symmetryHeight/2 + ysScore;
                            int xsi = (int)Math.round(xsT);
                            int ysi = (int)Math.round(ysT);
                            double xWeight1 = xsT - xsi + 0.5;
                            double yWeight1 = ysT - ysi + 0.5;
                            for (int ch = 0; ch < channels; ch++) {
                                if (xsi > 1 && xsi < wCross - 1) {
                                    crossIndicate = Math.max(
                                            (xCrossSectionSymmetry[xsi*channels + ch]*xWeight1 
                                                    + xCrossSectionSymmetry[(xsi - 1)*channels + ch]*(1 - xWeight1))
                                            /xCrossScoreMax[ch], crossIndicate);
                                }
                                if (ysi > 1 && ysi < hCross - 1) {
                                    crossIndicate = Math.max(
                                            (yCrossSectionSymmetry[ysi*channels + ch]*yWeight1 
                                                    + yCrossSectionSymmetry[(ysi - 1)*channels + ch]*(1 - yWeight1))
                                            /yCrossScoreMax[ch], crossIndicate);
                                }
                            }
                            crossIndicate = Math.pow(crossIndicate, 1);
                        }
                    }
                    else if (ringRadius > r0) {
                        // Overlay an angular contrast graph.
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
                        // Overlay cross-hairs.
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
                        // Something to draw on that pixel.
                        double alpha = 0;
                        double alphaCompl = 1-alpha;
                        byte [] pixelData = new byte[channels];
                        image.get(y, x, pixelData);
                        if (channels == 3) {
                            // RGB.
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
                            // Gray (or other). 
                            alpha = Math.max(Math.max(hairsIndicate, angleIndicate), crossIndicate*0.5);
                            alphaCompl = 1 - alpha;
                            if (alpha > 0) {
                                pixelData[0] = (byte) (alpha*255 + alphaCompl*Byte.toUnsignedInt(pixelData[0]));
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

    /**
     * Sum the contrast across the cross-section (sum of squares).
     * 
     * @param channels
     * @param size
     * @param crossSection
     * @param crossSectionN
     * @return
     */
    protected static double sumContrast(final int channels, final int size,
            double[] crossSection, double[] crossSectionN) {
        double sumContrast = 0;
        for (int ch = 0; ch < channels; ch++) {
            Double v0 = null;
            for (int x = ch; x < size; x++) {
                if (crossSectionN[x] > 0) {
                    double v = crossSection[x*channels + ch];
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

    /**
     * Instead of taking the cross-section cumulative brightness, compare the number of non.zero pixels against 
     * the minFeatureSize and apply a step function. The signal is essentially deciding whether something significant 
     * is present or not.  
     * 
     * @param channels
     * @param size
     * @param minFeatureSize
     * @param subSampling
     * @param superSampling
     * @param crossSectionMasked
     * @param kernel
     * @param crossSection
     */
    protected static void applyMasked(final int channels, final int size, double minFeatureSize, int subSampling, 
            int superSampling, double[] crossSectionMasked, double [] kernel, double[] crossSection) {
        KernelUtils.applyKernel(1, size, crossSection, kernel);
        double minFeatureSizeEff = minFeatureSize/subSampling/superSampling;
        for (int i = 0; i < size; i++) {
            for (int ch = 0; ch < channels; ch++) {
                crossSection[i*channels + ch] = Math.atan(crossSectionMasked[i] - minFeatureSizeEff)+Math.PI/2;
            }
        }
    }

    /**
     * Create the contour of the cross-section signal from both sides, meeting at the symmetry point s. 
     * 
     * @param channels
     * @param s
     * @param symmetryWidth
     * @param size
     * @param crossSection
     */
    protected static void applyCrossSectionContour(final int channels, double s, int symmetryWidth,
            final int size, double[] crossSection) {
        double [] xLeftMax = new double [channels];
        double [] xRightMax = new double [channels];
        int symmetry = (int) (s + symmetryWidth/2);
        for (int left = 0, right = size - 1; left < size; left++, right--) {
            for (int ch = 0; ch < channels; ch++) {
                xLeftMax[ch] = Math.max(crossSection[left*channels + ch], xLeftMax[ch]);
                xRightMax[ch] = Math.max(crossSection[right*channels + ch], xRightMax[ch]);
                if (left <= symmetry) {
                    crossSection[left*channels + ch] = xLeftMax[ch];
                }
                if (right > symmetry) {
                    crossSection[right*channels + ch] = xRightMax[ch];
                }
            }
        }
    }

    /**
     * Find the best symmetry in the cross-section. Winner is the maximum rate of left/right-sided variance, 
     * divided by the sum of squares of the left vs. right differences.   
     * 
     * @param channels
     * @param size
     * @param symmetrySearch
     * @param symmetrySize
     * @param symmetryFunction
     * @param crossSection
     * @param scoreRange
     * @return
     */
    protected static Double findCrossSectionSymmetry(final int channels, final int size,
            int symmetrySearch, int symmetrySize, SymmetryFunction symmetryFunction, 
            double[] crossSection, ScoreRange scoreRange) {
        // Find the best symmetry.
        Double coordBest = null;
        double scoreBest = 0;
        for (int s = 0; s <= symmetrySearch; s++) {
            double [] sumLeft = new double[channels];
            double [] sumLeftSq = new double[channels];
            double [] sumRight = new double[channels];
            double [] sumRightSq = new double[channels];
            double [] sumSymSq = new double[channels];
            int [] n = new int[channels];
            double [] vLeftMax = new double[channels];
            double [] vRightMax = new double[channels];
            int padding = symmetrySearch;
            for (int left = s - padding, right = s + symmetrySize - 1 + padding; left < s + symmetrySize/2; left++, right--) {
                for (int ch = 0; ch < channels; ch++) {
                    double vLeft = crossSection[Math.max(0, left)*channels + ch];
                    double vRight = crossSection[Math.min(size-1, right)*channels + ch];
                    if (symmetryFunction.isOutline()) {
                        vLeftMax[ch] = Math.max(vLeftMax[ch], vLeft);
                        vLeft = vLeftMax[ch];
                        vRightMax[ch] = Math.max(vRightMax[ch], vRight);
                        vRight = vRightMax[ch];
                    }
                    double dv = Math.abs(vLeft - vRight);
                    //System.out.println("Symmetry s="+s+" left="+left+" right="+right+" vLeft="+vLeft+" vRight="+vRight+" dv="+dv);
                    sumLeft[ch] += vLeft;
                    sumLeftSq[ch] += vLeft*vLeft;
                    sumRight[ch] += vRight;
                    sumRightSq[ch] += vRight*vRight;
                    sumSymSq[ch] += dv*dv;
                    n[ch]++;
                }
            }

            double varianceLeft = 0;
            double varianceRight = 0;
            double varianceSym = 1e-8;
            for (int ch = 0; ch < channels; ch++) { 
                varianceLeft += (sumLeftSq[ch] - (sumLeft[ch]*sumLeft[ch])/n[ch]);
                varianceRight += (sumRightSq[ch] - (sumRight[ch]*sumRight[ch])/n[ch]);
                varianceSym += sumSymSq[ch]/n[ch];
            }
            double variance = Math.min(varianceLeft, varianceRight);
            double score = variance/varianceSym;
            scoreRange.add(score);
            if (DEBUG >= 1) {
                System.out.print("Symmetry s="+s+" variance="+variance+" varianceSym="+varianceSym+" score="+score);
            }
            if (scoreBest < score) {
                scoreBest = score;
                coordBest = s + symmetryFunction.getOffset();
                if (DEBUG >= 1) {
                    System.out.println(" *");
                }
            }
            else {
                if (DEBUG >= 1) {
                    System.out.println("");
                }
            }
        }
        return coordBest;
    }

    /**
     * Find the bounds of the symmetry in the cross-section. Looks for the largest symmetric jump in the cross-section
     * signal outline.
     * 
     * @param channels
     * @param symmetry
     * @param symmetrySize
     * @param crossSection
     * @return
     */
    protected static int findCrossSectionBounds(final int channels, double symmetry, int symmetrySize,
            double[] crossSection) {
        double sumpBest = 0;
        int boundsBest = symmetrySize;
        double [] signalLeft = new double[channels];
        double [] signalRight = new double[channels];
        Double signal0 = null;
        for (int half = symmetrySize/2-1; half > 0; half--) {
            double i = symmetry + symmetrySize/2 - 1;
            double signal = 0;
            for (int ch = 0; ch < channels; ch++) {
                signalLeft[ch] = Math.max(crossSection[(int) ((i - half)*channels + ch)], signalLeft[ch]);
                signalRight[ch] = Math.max(crossSection[(int) ((i + half)*channels + ch)], signalRight[ch]);
                signal += signalLeft[ch] + signalRight[ch]; 
            }
            if (signal0 != null) {
                double jump = signal - signal0;
                if (sumpBest < jump) {
                    sumpBest = jump;
                    boundsBest = half*2;
                }
            }
            signal0 = signal;
        }
        return boundsBest;
    }
}
