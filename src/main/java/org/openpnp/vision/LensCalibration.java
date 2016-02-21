package org.openpnp.vision;

import org.opencv.core.Size;

/**
 * Performs OpenCV based lens calibration based on the techniques described in:
 * http://opencv-java-tutorials.readthedocs.org/en/latest/09-camera-calibration.html
 * http://docs.opencv.org/2.4/doc/tutorials/calib3d/camera_calibration/camera_calibration.html
 */
public class LensCalibration {
    public enum Pattern {
        Chessboard,
        CirclesGrid,
        AsymmetricCirclesGrid
    };
    
    final private Pattern pattern;
    final private Size patternSize;
    final private double objectSize;
    
    public LensCalibration(Pattern pattern, int patternWidth, int patternHeight, double objectSize) {
        this.pattern = pattern;
        this.patternSize = new Size(patternWidth, patternHeight);
        this.objectSize = objectSize;
    }
}
