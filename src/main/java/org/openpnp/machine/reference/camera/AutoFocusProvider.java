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

package org.openpnp.machine.reference.camera;

import java.awt.Color;
import java.awt.image.BufferedImage;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.camera.wizards.AutoFocusProviderConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.spi.FocusProvider;
import org.openpnp.spi.HeadMountable;
import org.openpnp.util.ImageUtils;
import org.openpnp.util.MovableUtils;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

public class AutoFocusProvider implements FocusProvider {
    @Element(required = false)
    Length focalResolution = new Length(0.05, LengthUnit.Millimeters);

    @Attribute(required = false)
    int averagedFrames = 1;

    @Attribute(required = false)
    double focusSpeed = 0.5;

    @Attribute(required = false)
    boolean showDiagnostics = true;

    public Length getFocalResolution() {
        return focalResolution;
    }

    public void setFocalResolution(Length focalResolution) {
        this.focalResolution = focalResolution;
    }

    public int getAveragedFrames() {
        return averagedFrames;
    }

    public void setAveragedFrames(int averagedFrames) {
        this.averagedFrames = averagedFrames;
    }

    public double getFocusSpeed() {
        return focusSpeed;
    }

    public void setFocusSpeed(double focusSpeed) {
        this.focusSpeed = focusSpeed;
    }

    public boolean isShowDiagnostics() {
        return showDiagnostics;
    }

    public void setShowDiagnostics(boolean showDiagnostics) {
        this.showDiagnostics = showDiagnostics;
    }

    @Override
    public Location autoFocus(Camera camera, HeadMountable movable,
            Length subjectMaxSize,
            Location location0, Location location1) throws Exception {
        // Compute the pixel diameter of the subject maximum size. 
        // As we don't know the focus [Z] plane, we take standard UnitsPerPixel.
        Location unitsPerPixel = camera.getUnitsPerPixel();
        int diameter = (int) Math.ceil(Math.max(
                subjectMaxSize.divide(unitsPerPixel.getLengthX()),
                subjectMaxSize.divide(unitsPerPixel.getLengthY())));
        diameter = Math.min(Math.min(diameter, camera.getHeight()-50), camera.getWidth()-50);
        diameter &= ~1; // Must be an even number.

        double speed = Configuration.get().getMachine().getSpeed();
        // Try to start from a 1mm retract location to get rid of any backlash that may not be compensated (typical in Z axes).
        Location retract = location1.convertToUnits(LengthUnit.Millimeters).unitVectorTo(location0).multiply(1.0);
        Location retractedLocation = location0.add(retract);
        // Limit to soft limits.
        location0 = movable.getApproximativeLocation(location0, location0);
        location1 = movable.getApproximativeLocation(location1, location1);
        retractedLocation = movable.getApproximativeLocation(retractedLocation, location0);
        // Move the movable to the retracted location at safe Z.
        MovableUtils.moveToLocationAtSafeZ(movable, retractedLocation);
        // Switch on the light.
        camera.actuateLightBeforeCapture();
        CameraView cameraView = MainFrame.get().getCameraViews().getCameraView(camera);
        BufferedImage bestFilteredImage = null; 
        try {
            final int maxCurveSteps = 10+1;
            while(true) {
                int curveSteps = Math.max(2, Math.min(maxCurveSteps, 
                        1+(int)Math.round(location1.getXyzLengthTo(location0).divide(focalResolution))));
                Location focalStep = location1.subtractWithRotation(location0).multiply(1.0/(curveSteps-1));
                double [] focusCurve = new double[curveSteps];
                Integer bestFocus = null;
                for (int step = 0; step < curveSteps; step++) {
                    Location l = location0.add(focalStep.multiply(step));
                    movable.moveTo(l, focusSpeed*speed);
                    BufferedImage image = camera.settleAndCapture();
                    BufferedImage filteredImage = null;
                    if (showDiagnostics) {
                        final int xCrop = (image.getWidth() - diameter)/2;
                        final int yCrop = (image.getHeight() - diameter)/2;
                        filteredImage = ImageUtils.clone(image.getSubimage(xCrop, yCrop, diameter+1, diameter+1)); 
                    }
                    double focusScore = focusScore(image, diameter, filteredImage);
                    for (int i = 1; i < averagedFrames; i++) {
                        image = camera.capture();
                        focusScore += focusScore(image, diameter, null);
                    }
                    focusCurve[step] = focusScore;
                    if (bestFocus == null || focusCurve[bestFocus] < focusScore) {
                        bestFocus = step;
                        bestFilteredImage = filteredImage;
                    }
                    if (filteredImage != null) { 
                        cameraView.showFilteredImage(filteredImage, "Auto Focus "+(bestFocus == step ? "▲" : "▼"), 1000);
                    }
                    Logger.trace("Focus score at "+l+" is "+focusScore+", step size "+focalStep);
                }

                if (focalStep.getXyzLengthTo(Location.origin).divide(focalResolution) < 1.5) {
                    // We reached focal resolution, just take the best focus location.
                    Location l = location0.add(focalStep.multiply(bestFocus));
                    movable.moveTo(l, focusSpeed*speed);
                    return l; 
                }

                // Find the next iteration sub-range. 
                double nextFocus = Math.max(1.0, Math.min(curveSteps-1-1.0, bestFocus));
                Location oldLocation0 = location0; 
                location0 = oldLocation0.add(focalStep.multiply(nextFocus-1.0));
                location1 = oldLocation0.add(focalStep.multiply(nextFocus+1.0));
                // Retract, same as at the start.
                retractedLocation = location0.add(retract);
                // Limit to soft limits.
                location0 = movable.getApproximativeLocation(location0, location0);
                location1 = movable.getApproximativeLocation(location1, location1);
                retractedLocation = movable.getApproximativeLocation(retractedLocation, retractedLocation);
                // Retract for next pass.
                movable.moveTo(retractedLocation);
            }
        }
        finally {
            // Whatever happens, switch off the light when done.
            camera.actuateLightAfterCapture();
            if (bestFilteredImage != null) { 
                cameraView.showFilteredImage(bestFilteredImage, "Auto Focus \u26AB", 2000);
            }
        }
    }

    /**
     * The focus score is computed by detecting the hardest edges in the camera image for a specific fraction of the pixels  
     * and then returning the lowest edge hardness of that group (fractile). 
     * 
     * @param image
     * @param diameter
     * @param filteredImage
     * @return
     */
    protected double focusScore(BufferedImage image, int diameter, BufferedImage filteredImage) {
        diameter &= ~1; // Must be an even number.
        final int bands = image.getRaster().getNumBands();
        final int width = image.getWidth();
        final int height = image.getHeight();
        final int wCrop = diameter+1;
        final int hCrop = diameter+1;
        final int xCrop = (width - diameter)/2;
        final int yCrop = (height - diameter)/2;
        // Get the pixels of a central crop.
        int [] pixelSamples = image.getRaster().getPixels(xCrop, yCrop, wCrop, hCrop, 
                new int[wCrop*hCrop*bands]);
        final int maxSample = 255; // TODO: find out the effective sample range?
        final int histogramSize = 1+(int)Math.ceil(maxSample*Math.sqrt(bands*2));
        int [] edgeHistogram = new int[histogramSize];
        int xNext = bands;
        int yNext = bands*wCrop;
        int r = diameter/2;
        int rSquare = r*r;
        int rSquareBracket = (r-3)*(r-3);
        int over = histogramSize;
        int passes = filteredImage != null ? 2 : 1;
        int markupRGB = new Color(0, 255, 0).getRGB(); 
        for (int pass = 0; pass < passes; pass++) {
            int i = 0;
            for (int y = -r, yi = 0; y < r; y++, yi++) {
                for (int x = -r, xi = 0; x < r; x++, xi++) {
                    int distanceSquare = x*x + y*y;
                    if (distanceSquare <= rSquare) {
                        int edge = 0;
                        for (int b = 0; b < bands; b++) {
                            //assert (((x+r)+(y+r)*wCrop)*bands+b == i);
                            int xEdge = pixelSamples[i] - pixelSamples[i+xNext];
                            int yEdge = pixelSamples[i] - pixelSamples[i+yNext];
                            edge += Math.pow(xEdge, 2) + Math.pow(yEdge, 2);
                            i++;
                        }
                        edge = (int)Math.round(Math.sqrt(edge));
                        edgeHistogram[edge]++;
                        if (edge >= over) {// && xi > 0 && yi > 0) {
                            filteredImage.setRGB(xi, yi, markupRGB);
                        }
                    }
                    else {
                        // Outside the mask, skip pixel.
                        i += xNext;
                    }
                    if (filteredImage != null 
                            && distanceSquare >= rSquareBracket
                            && distanceSquare <= rSquare) {
                        filteredImage.setRGB(xi, yi, 0);
                    }
                }
                // On the edge, skip one more pixel.
                i += xNext;
            }
            // the significant "feature" pixel amount should grow with the diameter to the power of 1.2.
            final int significantPixels = (int)Math.pow(diameter, 1.3)/10; 
            int fractile = significantPixels;
            int exceeding = 0;
            for (int h = histogramSize-1; h >= 0; h--) {
                exceeding += edgeHistogram[h];
                if (exceeding >= fractile) {
                    double inter = (exceeding - fractile)/(double)edgeHistogram[h];
                    if (pass == passes - 1) {
                        return (h+inter);
                    }
                    over = h;
                    break;
                }
            }
        }
        return 0;
    }

    @Override
    public Wizard getConfigurationWizard(Camera camera) {
        return new AutoFocusProviderConfigurationWizard(camera, this);
    }
}
