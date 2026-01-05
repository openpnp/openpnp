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
        BufferedImage templateImage = camera.settleAndCapture();
        
        try {
            // 2. Create template from center
            Mat templateMat = OpenCvUtils.toMat(templateImage);
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
            BufferedImage searchImage = camera.settleAndCapture();
            Mat searchMat = OpenCvUtils.toMat(searchImage);
    
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
    
    // Helper match template
    private Point matchTemplate(Mat img, Mat templ) {
        int result_cols = img.cols() - templ.cols() + 1;
        int result_rows = img.rows() - templ.rows() + 1;
        Mat result = new Mat(result_rows, result_cols, CvType.CV_32FC1);

        Imgproc.matchTemplate(img, templ, result, Imgproc.TM_CCOEFF_NORMED);
        MinMaxLocResult mmr = Core.minMaxLoc(result);
        
        result.release();
        return mmr.maxLoc;
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
}
