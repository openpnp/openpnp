package org.openpnp.vision;

import java.util.ArrayList;
import java.util.List;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point3;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performs OpenCV based lens calibration based on the techniques described in:
 * http://opencv-java-tutorials.readthedocs.org/en/latest/09-camera-calibration.html
 * http://docs.opencv.org/2.4/doc/tutorials/calib3d/camera_calibration/camera_calibration.html
 * https://github.com/Itseez/opencv/blob/master/samples/cpp/tutorial_code/calib3d/camera_calibration
 * /camera_calibration.cpp
 * 
 * FishEye model code is included but unfinished. This code cannot be finished until we are using
 * OpenCV 3.
 */
public class LensCalibration {
    protected final static Logger logger = LoggerFactory.getLogger(LensCalibration.class);

    static {
        nu.pattern.OpenCV.loadShared();
        System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
    }

    public enum Pattern {
        Chessboard, CirclesGrid, AsymmetricCirclesGrid
    };

    public enum LensModel {
        Pinhole, Fisheye
    }

    final private LensModel lensModel;
    final private Pattern pattern;
    final private Size patternSize;
    final private double objectSize;
    final private MatOfPoint3f objectPoints;
    final private long applyDelayMs;

    private List<Mat> imagePointsList = new ArrayList<>();
    private List<Mat> objectPointsList = new ArrayList<>();
    private Size imageSize;
    private Mat cameraMatrix;
    private Mat distortionCoefficients;
    private long lastApplyMs;

    public LensCalibration(LensModel lensModel, Pattern pattern, int patternWidth,
            int patternHeight, double objectSize, long applyDelayMs) {
        if (lensModel == LensModel.Fisheye) {
            throw new Error(lensModel + " LensModel not yet supported. OpenCV 3+ needed.");
        }
        this.lensModel = lensModel;
        this.pattern = pattern;
        this.patternSize = new Size(patternWidth, patternHeight);
        this.objectSize = objectSize;
        this.applyDelayMs = applyDelayMs;
        // We only need to calculate this once, so we do it ahead of time
        // and then add it to the list with each processed image.
        objectPoints = calculateObjectPoints();
    }
    
    public void close() {
    	if (cameraMatrix != null) {
    		cameraMatrix.release();
    	}
    	if (distortionCoefficients != null) {
    		distortionCoefficients.release();
    	}
    	
    	objectPoints.release();
    	for (Mat imagePoints : imagePointsList) {
    		imagePoints.release();
    	}
    }

    public Mat apply(Mat mat) {
        if (imageSize == null) {
            imageSize = mat.size();
        }

        MatOfPoint2f imagePoints = findImagePoints(mat);
        if (imagePoints == null) {
            return null;
        }

        Calib3d.drawChessboardCorners(mat, patternSize, imagePoints, true);

        if (System.currentTimeMillis() - lastApplyMs > applyDelayMs) {
            objectPointsList.add(objectPoints);
            imagePointsList.add(imagePoints);
            lastApplyMs = System.currentTimeMillis();
        }

        return mat;
    }

    public boolean calibrate() {
        Mat cameraMatrix;
        Mat distortionCoefficients;

        cameraMatrix = Mat.eye(3, 3, CvType.CV_64F);

        if (lensModel == LensModel.Fisheye) {
            distortionCoefficients = Mat.zeros(4, 1, CvType.CV_64F);
        }
        else {
            distortionCoefficients = Mat.zeros(8, 1, CvType.CV_64F);
        }

        List<Mat> rvecs = new ArrayList<>();
        List<Mat> tvecs = new ArrayList<>();

        double rms;

        if (lensModel == LensModel.Fisheye) {
            // TODO:
            throw new Error(lensModel + " LensModel not yet supported. OpenCV 3+ needed.");
            // Mat _rvecs, _tvecs;
            // rms = fisheye::calibrate(objectPoints, imagePoints, imageSize, cameraMatrix,
            // distCoeffs, _rvecs,
            // _tvecs, s.flag);
            //
            // rvecs.reserve(_rvecs.rows);
            // tvecs.reserve(_tvecs.rows);
            // for(int i = 0; i < int(objectPoints.size()); i++){
            // rvecs.push_back(_rvecs.row(i));
            // tvecs.push_back(_tvecs.row(i));
            // }
        }
        else {
            rms = Calib3d.calibrateCamera(objectPointsList, imagePointsList, imageSize,
                    cameraMatrix, distortionCoefficients, rvecs, tvecs);
        }

        for (Mat rvec : rvecs) {
        	rvec.release();
        }
        rvecs.clear();
        for (Mat tvec : tvecs) {
        	tvec.release();
        }
        tvecs.clear();
        
        boolean ok = Core.checkRange(cameraMatrix) && Core.checkRange(distortionCoefficients);

        logger.info("calibrate() ok {}, rms {}", ok, rms);

        if (ok) {
            this.cameraMatrix = cameraMatrix;
            this.distortionCoefficients = distortionCoefficients;
        } else {
        	cameraMatrix.release();
        	distortionCoefficients.release();
        }

        return ok;
    }

    public int getPatternFoundCount() {
        return imagePointsList.size();
    }

    public boolean isCalibrated() {
        return cameraMatrix != null && distortionCoefficients != null;
    }

    public Mat getCameraMatrix() {
        return cameraMatrix;
    }

    public Mat getDistortionCoefficients() {
        return distortionCoefficients;
    }

    private MatOfPoint2f findImagePoints(Mat mat) {
        MatOfPoint2f imagePoints = new MatOfPoint2f();
        boolean found = false;
        switch (pattern) {
            case Chessboard:
                int chessBoardFlags =
                        Calib3d.CALIB_CB_ADAPTIVE_THRESH | Calib3d.CALIB_CB_NORMALIZE_IMAGE;
                if (lensModel != LensModel.Fisheye) {
                    // fast check erroneously fails with high distortions like fisheye
                    chessBoardFlags |= Calib3d.CALIB_CB_FAST_CHECK;
                }
                found = Calib3d.findChessboardCorners(mat, patternSize, imagePoints,
                        chessBoardFlags);
                if (found) {
                    // improve the found corners' coordinate accuracy for chessboard
                    Mat matGray = new Mat();
                    Imgproc.cvtColor(mat, matGray, Imgproc.COLOR_BGR2GRAY);
                    Imgproc.cornerSubPix(matGray, imagePoints, new Size(11, 11), new Size(-1, -1),
                            new TermCriteria(TermCriteria.EPS + TermCriteria.COUNT, 30, 0.1));
                    matGray.release();
                }
                break;
            case CirclesGrid:
                found = Calib3d.findCirclesGridDefault(mat, patternSize, imagePoints);
                break;
            case AsymmetricCirclesGrid:
                found = Calib3d.findCirclesGridDefault(mat, patternSize, imagePoints,
                        Calib3d.CALIB_CB_ASYMMETRIC_GRID);
                break;
        }
        if (found) {
        	return imagePoints;
        } else {
        	imagePoints.release();
        	return null;
        }
    }

    private MatOfPoint3f calculateObjectPoints() {
        MatOfPoint3f obj = new MatOfPoint3f();

        switch (pattern) {
            case Chessboard:
            case CirclesGrid:
                for (int i = 0; i < patternSize.height; ++i) {
                    for (int j = 0; j < patternSize.width; ++j) {
                        obj.push_back(
                                new MatOfPoint3f(new Point3(j * objectSize, i * objectSize, 0)));
                    }
                }
                break;
            case AsymmetricCirclesGrid:
                for (int i = 0; i < patternSize.height; i++) {
                    for (int j = 0; j < patternSize.width; j++) {
                        obj.push_back(new MatOfPoint3f(
                                new Point3((2 * j + i % 2) * objectSize, i * objectSize, 0)));
                    }
                }
                break;
        }
        return obj;
    }
}
