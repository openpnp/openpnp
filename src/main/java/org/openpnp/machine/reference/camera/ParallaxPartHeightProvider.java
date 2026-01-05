package org.openpnp.machine.reference.camera;

import java.awt.image.BufferedImage;
import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.camera.wizards.ParallaxPartHeightProviderConfigurationWizard;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.spi.FocusProvider;
import org.openpnp.spi.HeadMountable;
import org.openpnp.util.OpenCvUtils;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

public class ParallaxPartHeightProvider implements FocusProvider {
    @Element(required = false)
    private Length shiftDistance = new Length(2.0, LengthUnit.Millimeters);

    @Attribute(required = false)
    private int featureSize = 64;

    @Attribute(required = false)
    private int framesToAverage = 1;

    @Attribute(required = false)
    private double focalPointZ = 0.0;
    
    @Attribute(required = false)
    private boolean showDiagnostics = true;

    public ParallaxPartHeightProvider() {
    }

    /**
     * Measure the parallax shift in mm for an object roughly at the startLocation.
     * Moves the movable by shiftDistance and uses template matching.
     * 
     * @param camera
     * @param movable
     * @param startLocation
     * @return The measured shift in mm (using default Z units).
     * @throws Exception
     */
    public double measureParallaxShift(Camera camera, HeadMountable movable, Location startLocation) throws Exception {
         // 1. Capture image at start location
        movable.moveTo(startLocation);
        Mat templateMat = captureAndAverage(camera, framesToAverage);
        
        try {
            // 2. Create template from center
            Mat template = cropCenter(templateMat, featureSize);
            
            if (showDiagnostics) {
                CameraView cameraView = MainFrame.get().getCameraViews().getCameraView(camera);
                if (cameraView != null) {
                    cameraView.showFilteredImage(OpenCvUtils.toBufferedImage(template), "Parallax Template", 1000);
                }
            }
    
            // 3. Move camera by shiftDistance in X
            Location shift = new Location(LengthUnit.Millimeters, shiftDistance.getValue(), 0, 0, 0);
            shift = shift.convertToUnits(startLocation.getUnits());
            Location shiftLocation = startLocation.add(shift);
            
            movable.moveTo(shiftLocation);
            
            // 4. Capture search image
            Mat searchMat = captureAndAverage(camera, framesToAverage);
    
            // 5. Find feature using Template Matching
            Point matchLoc = matchTemplate(searchMat, template);
            
            // Center of the search image
            Point center = new Point(searchMat.cols() / 2.0, searchMat.rows() / 2.0);
            
            // The matchLoc is the top-left of the template. Get center of match.
            Point matchCenter = new Point(matchLoc.x + template.cols() / 2.0, matchLoc.y + template.rows() / 2.0);
            
            double dxPx = matchCenter.x - center.x;
            
            Location unitsPerPixel = camera.getUnitsPerPixel(); 
            return Math.abs(dxPx * unitsPerPixel.getX());
        }
        finally {
            // Move back to start
            movable.moveTo(startLocation);
        } 
    }

    public double getFocalPointZ() {
        return focalPointZ;
    }

    public void setFocalPointZ(double focalPointZ) {
        this.focalPointZ = focalPointZ;
    }

    public boolean isShowDiagnostics() {
        return showDiagnostics;
    }

    public void setShowDiagnostics(boolean showDiagnostics) {
        this.showDiagnostics = showDiagnostics;
    }


    @Override
    public Location autoFocus(Camera camera, HeadMountable movable, Length subjectMaxSize, Location location0,
            Location location1) throws Exception {
        
        // Use the helper to measure shift
        double measuredShiftX_mm = measureParallaxShift(camera, movable, location0);
        
        double zDef = camera.getDefaultZ().getValue();
        double zCenter = focalPointZ;
        
        double physicalShiftX_mm = Math.abs(shiftDistance.getValue()); // S
        
        // Guard against divide by zero (infinity Z)
        if (measuredShiftX_mm < 0.0001) {
            Logger.warn("Parallax shift too small to calculate Z.");
            return location0;
        }

        double z_obj = zCenter + physicalShiftX_mm * (zDef - zCenter) / measuredShiftX_mm;

        return new Location(location0.getUnits(), location0.getX(), location0.getY(), z_obj, location0.getRotation());
    }
    
    // Helper to crop center
    private Mat cropCenter(Mat img, int size) {
        int x = (img.cols() - size) / 2;
        int y = (img.rows() - size) / 2;
        return new Mat(img, new org.opencv.core.Rect(x, y, size, size));
    }
    
    // Helper match template with sub-pixel refinement
    private Point matchTemplate(Mat img, Mat templ) {
        int result_cols = img.cols() - templ.cols() + 1;
        int result_rows = img.rows() - templ.rows() + 1;
        Mat result = new Mat(result_rows, result_cols, CvType.CV_32FC1);

        Imgproc.matchTemplate(img, templ, result, Imgproc.TM_CCOEFF_NORMED);
        MinMaxLocResult mmr = Core.minMaxLoc(result);
        
        Point refined = subPixelRefine(result, mmr.maxLoc);
        
        result.release();
        return refined;
    }
    
    private Point subPixelRefine(Mat result, Point maxLoc) {
        int x = (int) maxLoc.x;
        int y = (int) maxLoc.y;
        
        // Check bounds (need 1 pixel border)
        if (x < 1 || x >= result.cols() - 1 || y < 1 || y >= result.rows() - 1) {
            return maxLoc;
        }
        
        // Extract 3x3 window around peak
        // y0 is left/top, y1 is center (max), y2 is right/bottom
        
        // Refine X (horizontal)
        float v_x_minus = (float) result.get(y, x - 1)[0];
        float v_x_center = (float) result.get(y, x)[0];
        float v_x_plus = (float) result.get(y, x + 1)[0];
        
        double deltaX = (v_x_minus - v_x_plus) / (2.0 * (v_x_minus - 2.0 * v_x_center + v_x_plus));
        
        // Refine Y (vertical)
        float v_y_minus = (float) result.get(y - 1, x)[0];
        float v_y_center = (float) result.get(y, x)[0];
        float v_y_plus = (float) result.get(y + 1, x)[0];
        
        double deltaY = (v_y_minus - v_y_plus) / (2.0 * (v_y_minus - 2.0 * v_y_center + v_y_plus));
        
        // Sanity check deltas (should be within +/- 0.5)
        if (Math.abs(deltaX) > 0.6 || Math.abs(deltaY) > 0.6) {
             return maxLoc;
        }
        
        return new Point(maxLoc.x + deltaX, maxLoc.y + deltaY);
    }

    @Override
    public Wizard getConfigurationWizard(Camera camera) {
        return new ParallaxPartHeightProviderConfigurationWizard(camera, this);
    }
    
    public Length getShiftDistance() {
        return shiftDistance;
    }

    public void setShiftDistance(Length shiftDistance) {
        this.shiftDistance = shiftDistance;
    }

    public int getFeatureSize() {
        return featureSize;
    }

    public void setFeatureSize(int featureSize) {
        this.featureSize = featureSize;
    }

    public int getFramesToAverage() {
        return framesToAverage;
    }

    public void setFramesToAverage(int framesToAverage) {
        this.framesToAverage = framesToAverage;
    }
    
    // Helper to capture and average frames
    private Mat captureAndAverage(Camera camera, int count) throws Exception {
        // First frame with settle
        BufferedImage img0 = camera.settleAndCapture();
        Mat m0 = OpenCvUtils.toMat(img0);
        
        if (count <= 1) {
            return m0;
        }

        Mat accumulator = new Mat();
        m0.convertTo(accumulator, CvType.CV_32FC3);
        m0.release();
        
        // Subsequent frames (burst)
        for (int i = 1; i < count; i++) {
            BufferedImage img = camera.capture();
            Mat m = OpenCvUtils.toMat(img);
            Mat floatMat = new Mat();
            m.convertTo(floatMat, CvType.CV_32FC3);
            
            Core.add(accumulator, floatMat, accumulator);
            floatMat.release();
            m.release();
        }
        
        Mat result = new Mat();
        accumulator.convertTo(result, CvType.CV_8UC3, 1.0 / count);
        accumulator.release();
        return result;
    }
}
