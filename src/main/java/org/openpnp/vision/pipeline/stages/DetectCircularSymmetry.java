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
import java.util.List;

import org.opencv.core.Mat;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.CvStage.Result.Circle;
import org.openpnp.vision.pipeline.Property;
import org.openpnp.vision.pipeline.Stage;
import org.simpleframework.xml.Attribute;

/**
 * Finds the maximum circular symmetry in the working image and stores the results as a single element List<Circle> on the model. 
 */
@Stage(description="Finds circular symmetry in the working image. Diameter range and maximum distance from center can be specified.")
public class DetectCircularSymmetry extends CvStage {

    @Attribute
    @Property(description = "Minimum diameter of the circle, in pixels.")
    private int minDiameter = 10;

    @Attribute
    @Property(description = "Maximum diameter of the circle, in pixels.")
    private int maxDiameter = 100;

    @Attribute(required = false)
    @Property(description = "Maximum distance from center, in pixels.")
    private int maxDistance = 100;

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

    public boolean isDiagnostics() {
        return diagnostics;
    }

    public void setDiagnostics(boolean diagnostics) {
        this.diagnostics = diagnostics;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        Mat mat = pipeline.getWorkingImage();
        List<Result.Circle> circles = new ArrayList<>();
        circles.add(findCircularSymmetry(mat, mat.cols()/2, mat.rows()/2, maxDiameter, minDiameter, maxDistance, diagnostics));
        return new Result(null, circles);
    }

    /**
     * Find the circle that has its center at the greatest circular symmetry in the given image,
     * indicating the largest contrast edge as its diameter.
     * 
     * @param image
     * @param xCenter
     * @param yCenter
     * @param maxDiameter
     * @param diagnostics TODO
     * @param tolerance
     * @return
     * @throws Exception 
     */
    public static Circle findCircularSymmetry(Mat image, int xCenter, int yCenter,
            int maxDiameter, int minDiameter, int maxDistance, boolean diagnostics) throws Exception {
        final int channels = image.channels();
        final int width = image.cols();
        final int height = image.rows();
        maxDiameter |= 1; // make it odd
        minDiameter = Math.max(3, minDiameter | 1);
        final int tolerance = Math.min((width-maxDiameter)-2, Math.min((height-maxDiameter)-2, maxDistance));
        if (tolerance < maxDistance/5) {
            throw new Exception("Image too small for given parameters.");
        }
        final int diameter = maxDiameter + tolerance;
        final int xCrop = xCenter - diameter / 2;
        final int yCrop = yCenter - diameter / 2;
        // Get the pixels out of the Mat.
        byte[] pixelSamples = new byte[height*width*channels]; 
        image.get(0, 0, pixelSamples);
        int r = maxDiameter / 2;
        int r0 = minDiameter / 2 - 1;
        // Create the rings of circular symmetry as lists of indices into the flat array of pixels.
        // The indices are relative to the origin but they can be offset to any x, y (within
        // tolerance) and still remain valid, thanks to modulo properties.
        Object[] ringsA = new Object[r + 1];
        for (int ri = r0; ri < ringsA.length; ri++) {
            ringsA[ri] = new ArrayList<Integer>();
        }
        for (int y = -r, yi = 0; y <= r; y++, yi++) {
            for (int x = -r, xi = 0, idx = yi * width * channels; x <= r; x++, xi++, idx += channels) {
                int distanceSquare = x * x + y * y;
                int distance = (int) Math.round(Math.sqrt(distanceSquare));
                if (r0 <= distance && distance <= r) {
                    ((ArrayList<Integer>) ringsA[distance]).add(idx);
                }
            }
        }
        // Convert indices to arrays (hopefully faster).
        int[][] rings = new int[ringsA.length][];
        for (int ri = r0; ri < ringsA.length; ri++) {
            rings[ri] = ((ArrayList<Integer>) ringsA[ri]).stream()
                    .mapToInt(i -> i)
                    .toArray();
        }
        // Now iterate through all the offsets and find the maximum circular symmetry
        // which is the one with the largest ratio between radial and circular variances.
        double scoreBest = Double.NEGATIVE_INFINITY;
        int xBest = 0;
        int yBest = 0;
        int rContrastBest = 0;
        double [] match = null;
        double minScore = Double.POSITIVE_INFINITY;
        double maxScore = Double.NEGATIVE_INFINITY;
        if (diagnostics) {
            match = new double[tolerance*tolerance];
        }
        for (int yi = yCrop; yi < yCrop + tolerance; yi++) {
            for (int xi = xCrop, idxOffset = (yi * width + xCrop) * channels; xi < xCrop + tolerance; xi++, idxOffset +=
                    channels) {
                double varianceSum = 0.1;
                double lastAvgChannels = 0;
                double contrastBest = Double.NEGATIVE_INFINITY;
                int riContrastBest = 0;
                double[] sumOverall = new double[channels];
                double[] sumSqOverall = new double[channels];
                double[] nOverall = new double[channels];
                // Examine each ring.
                for (int ri = r0; ri < rings.length; ri++) {
                    int[] ring = rings[ri];
                    double n = ring.length;
                    if (n > 0) {
                        long sumChannels = 0;
                        for (int ch = 0; ch < channels; ch++) {
                            // Calculate the variance along the ring.
                            long sum = 0;
                            long sumSq = 0;
                            for (int idx : ring) {
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
                double varianceOverall = 0;
                for (int ch = 0; ch < channels; ch++) {
                    varianceOverall += (sumSqOverall[ch] - (sumOverall[ch] * sumOverall[ch]) / nOverall[ch]);
                }
                double score = varianceOverall/varianceSum; 

                if (match != null) {
                    score = Math.log(Math.log(score));
                    match[(yi - yCrop)*tolerance + (xi-xCrop)] = score;
                    minScore = Math.min(minScore, score); 
                    maxScore = Math.max(maxScore, score); 
                }
                if (scoreBest < score) {
                    scoreBest = score;
                    xBest = xi;
                    yBest = yi;
                    rContrastBest = riContrastBest;
                }
            }
        }
        
        if (match != null) {
            double rscale = 1/(maxScore - minScore);
            double scale = 255*channels*rscale;
            for (int yi = 0; yi < tolerance; yi++) {
                for (int xi = 0; xi < tolerance; xi++) {
                    double s = (match[yi*tolerance + xi] - minScore);
                    double score = s*scale;
                    int dx = xi + xCrop - xBest;
                    int dy = yi + yCrop - yBest;
                    int d = (int)Math.round(Math.sqrt(dx*dx + dy*dy));
                    boolean indicate = (d == rContrastBest || dx == 0 || dy == 0);
                    double alpha = s*rscale;
                    double alphaCompl = 1-alpha;
                    byte [] pixelData = new byte[channels];
                    image.get(yi + yCrop + r, xi + xCrop + r, pixelData);
                    if (channels == 3) {
                        int red = 0;
                        int green = 0;
                        int blue = 0;
                        if (indicate) {
                            red = 255;
                            green = 255;
                            blue = 255;
                            alpha = 0.25;
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
                        pixelData[0] = (byte) (alpha*score + alphaCompl*Byte.toUnsignedInt(pixelData[0]));
                    }
                    image.put(yi + yCrop + r, xi + xCrop + r, pixelData);
                }
            }
        }
        return new CvStage.Result.Circle(r + xBest, r + yBest, rContrastBest * 2);
    }
    
}
