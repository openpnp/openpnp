/*
 * Copyright (C) 2021 Tony Luken <tonyluken@att.net>
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

package org.openpnp.machine.reference.camera.calibration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.math3.exception.TooManyEvaluationsException;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresFactory;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer.Optimum;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.fitting.leastsquares.MultivariateJacobianFunction;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.optim.ConvergenceChecker;
import org.apache.commons.math3.optim.SimpleVectorValueChecker;
import org.apache.commons.math3.util.Pair;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Subdiv2D;
import org.pmw.tinylog.Logger;

/**
 * Provides a set of utilities to aid in camera calibration
 *
 */
public class CameraCalibrationUtils {

    public static final int FIX_ASPECT_RATIO = 1;
    public static final int FIX_PRINCIPAL_POINT = 2;
    public static final int FIX_DISTORTION_COEFFICENTS = 4;
    public static final int FIX_ROTATION = 8;

    // The following threshold is used to decide if a collected data point is a possible outlier.
    // This assumes the residual errors are bivariate normally distributed. Lower values will cause
    // more points to be rejected. For reference, here are some other possible settings:
    // 2.4103 -> include 98.7% and reject 0.3%
    // 1.7308 -> include 95.0% and reject 5.0%
    // 0.8326 -> include 50.0% and reject 50.0%
    // See https://en.wikipedia.org/wiki/Circular_error_probable for a more detailed explanation
    public static final double sigmaThresholdForRejectingOutliers = 2.4103;

    private static final int maxEvaluations = 1000;
    private static final int maxIterations = 1000;

    private static int numberOfTestPatterns;
    private static int numberOfParameters;
    private static double aspectRatio;

    /**
     * Computes the best, in a least squared error sense, set of camera parameters that transform 
     * the given test pattern 3D coordinates to the corresponding 2D image points. This is very 
     * similar to what OpenCv's Calib3d.calibrateCamera does except this implementation takes 
     * advantage of the fact that it is known that the rotation of the camera WRT the test patterns
     * is constant and that the Z height of the camera is also constant for all test patterns.
     * 
     * The openpnp camera model that transforms 3D machine coordinates to 2D image points is:
     * 
     *      U = K * D( R * (X - T) )
     *      
     *      Where:
     *      U   is a 3x1 vector containing the image coordinates, in pixels, of a point (Z = 1 for 
     *            all points);
     *      K   is the 3x3 intrinsic camera matrix;
     *      D() is OpenCV's five parameter distortion model - takes a 3x1 vector as a parameter in 
     *            undistorted camera coordinates and returns a normalized 3x1 vector (Z=1) in 
     *            distorted camera coordinates;
     *      R   is a 3x3 rotation matrix that transforms vectors from the machine reference frame to
     *            the camera reference frame;
     *      X   is a 3x1 vector containing the machine coordinates of the same point as in U; and
     *      T   is a 3x1 vector containing the machine coordinates of the camera
     *
     *      Note that this is very similar as to how OpenCV models a camera except that here T is
     *      in the machine reference frame rather than in the camera's reference frame.  This is so
     *      that we can treat the Z component of T as a single constant (since we know the camera
     *      is not changing in Z during the calibration process).
     *      
     *      The objective of the calibration process to use a set of known U's and X's to solve
     *      for the unknown K, D(), R, and T (see the description of parameters below)
     * 
     * @param testPattern3dPoints - a numberOfTestPatterns x numberOfPointsPerTestPattern x 3 array
     *        containing the 3D machine coordinates at which the corresponding point in
     *        testPatternImagePoints was collected.
     * @param testPatternImagePoints - a numberOfTestPatterns x numberOfPointsPerTestPattern x 2
     *        array containing the 2D image coordinates of the corresponding point in
     *        testPattern3dPoints.
     * @param modeledImagePoints - a numberOfTestPatterns x numberOfPointsPerTestPattern x 2 array
     *        that, upon return of this method, will contain the 2D modeled image coordinates of the
     *        corresponding points in testPattern3dPoints.
     * @param outlierPointList - an output list of indices to points that were excluded from the
     *        parameter estimation due to being possible outliers.
     * @param parameters - a 13+2*numberOfTestPatterns element array containing the camera
     *        parameters to be estimated in the following order: fx, fy, cx, cy, k1, k2, p1, p2, k3,
     *        Rx, Ry, Rz, cam_z, cam_x[0], cam_y[0], ... cam_x[numberOfTestPatterns-1],
     *        cam_y[numberOfTestPatterns-1]. Where fx, fy, cx, and cy are the intrinsic camera
     *        matrix components; k1, k2, p1, p2, and k3 are the intrinsic camera lens distortion
     *        coefficients; Rx, Ry, and Rz are the extrinsic camera rotation vector components;
     *        cam_z is the camera Z machine coordinate; and cam_x[0], cam_y[0], ...
     *        cam_x[numberOfTestPatterns-1], and cam_y[numberOfTestPatterns-1] are the camera X/Y
     *        machine coordinates for each test pattern. This array needs to be initialized with
     *        approximately correct values prior to calling this method and upon return of this
     *        method will contain the best fit camera parameters.
     * @return the DRMS error in pixels of the model's fit to the data points.
     * @throws Exception 
     */
    public static double computeBestCameraParameters(double[][][] testPattern3dPoints,
            double[][][] testPatternImagePoints, double[][][] modeledImagePoints,
            List<Integer> outlierPointList, double[] parameters) throws Exception {
        return computeBestCameraParameters(testPattern3dPoints, testPatternImagePoints,
                modeledImagePoints, outlierPointList, parameters, 0);
    }

    /**
     * Computes the best, in a least squared error sense, set of camera parameters that transform 
     * the given test pattern 3D coordinates to the corresponding 2D image points. This is very 
     * similar to what OpenCv's Calib3d.calibrateCamera does except this implementation takes 
     * advantage of the fact that it is known that the rotation of the camera WRT the test patterns
     * is constant and that the Z height of the camera is also constant for all test patterns.
     * 
     * The openpnp camera model that transforms 3D machine coordinates to 2D image points is:
     * 
     *      U = K * D( R * (X - T) )
     *      
     *      Where:
     *      U   is a 3x1 vector containing the image coordinates, in pixels, of a point (Z = 1 for 
     *            all points);
     *      K   is the 3x3 intrinsic camera matrix;
     *      D() is OpenCV's five parameter distortion model - takes a 3x1 vector as a parameter in 
     *            undistorted camera coordinates and returns a normalized 3x1 vector (Z=1) in 
     *            distorted camera coordinates;
     *      R   is a 3x3 rotation matrix that transforms vectors from the machine reference frame to
     *            the camera reference frame;
     *      X   is a 3x1 vector containing the machine coordinates of the same point as in U; and
     *      T   is a 3x1 vector containing the machine coordinates of the camera
     *
     *      Note that this is very similar as to how OpenCV models a camera except that here T is
     *      in the machine reference frame rather than in the camera's reference frame.  This is so
     *      that we can treat the Z component of T as a single constant (since we know the camera
     *      is not changing in Z during the calibration process).
     *      
     *      The objective of the calibration process to use a set of known U's and X's to solve
     *      for the unknown K, D(), R, and T (see the description of parameters below)
     * 
     * @param testPattern3dPoints - a numberOfTestPatterns x numberOfPointsPerTestPattern x 3 array
     *        containing the 3D machine coordinates at which the corresponding point in
     *        testPatternImagePoints was collected.
     * @param testPatternImagePoints - a numberOfTestPatterns x numberOfPointsPerTestPattern x 2
     *        array containing the 2D image coordinates of the corresponding point in
     *        testPattern3dPoints.
     * @param modeledImagePoints - a numberOfTestPatterns x numberOfPointsPerTestPattern x 2 array
     *        that, upon return of this method, will contain the 2D modeled image coordinates of the
     *        corresponding points in testPattern3dPoints.
     * @param outlierPointList - an output list of indices to points that were excluded from the
     *        parameter estimation due to being possible outliers.
     * @param parameters - a 13+2*numberOfTestPatterns element array containing the camera
     *        parameters to be estimated in the following order: fx, fy, cx, cy, k1, k2, p1, p2, k3,
     *        Rx, Ry, Rz, cam_z, cam_x[0], cam_y[0], ... cam_x[numberOfTestPatterns-1],
     *        cam_y[numberOfTestPatterns-1]. Where fx, fy, cx, and cy are the intrinsic camera
     *        matrix components; k1, k2, p1, p2, and k3 are the intrinsic camera lens distortion
     *        coefficients; Rx, Ry, and Rz are the extrinsic camera rotation vector components;
     *        cam_z is the camera Z machine coordinate; and cam_x[0], cam_y[0], ...
     *        cam_x[numberOfTestPatterns-1], and cam_y[numberOfTestPatterns-1] are the camera X/Y
     *        machine coordinates for each test pattern. This array needs to be initialized with
     *        approximately correct values prior to calling this method and upon return of this
     *        method will contain the best fit camera parameters.
     * @param flags - Flags used to force certain parameters be retained from the initial starting
     *        values. Can be one or more of the follow added together: FIX_ASPECT_RATIO,
     *        FIX_CENTER_POINT, FIX_DISTORTION_COEFFICENTS, and FIX_ROTATION.
     * @return the DRMS error in pixels of the model's fit to the data points.
     * @throws Exception 
     */
    public static double computeBestCameraParameters(double[][][] testPattern3dPoints,
            double[][][] testPatternImagePoints, double[][][] modeledImagePoints,
            List<Integer> outlierPointList, double[] parameters, int flags) throws Exception {
        numberOfTestPatterns = testPattern3dPoints.length;
        // The number of model parameters includes 4 camera matrix entries, 5 distortion
        // coefficients, 3 rotation vector coefficients, 1 camera Z component, and an X/Y camera
        // coordinate pair for each test pattern
        numberOfParameters = 4 + 5 + 3 + 1 + 2 * numberOfTestPatterns;
        aspectRatio = parameters[1] / parameters[0];

        TreeSet<Integer> outlierPoints = new TreeSet<Integer>();

        LevenbergMarquardtOptimizer optimizer = new LevenbergMarquardtOptimizer();
        Optimum optimum = null;

        // Up to two attempts are made. The first uses all available data points and the second uses
        // only data points whose residual error from the first attempt are within
        // sigmaThresholdForRejectingOutliers standard deviations of the modeled value. This should
        // help prevent any points that are extreme outliers due to measurement errors from
        // distorting the results.
        for (int attempt = 0; attempt < 2; attempt++) {
            // The model is used to compute the modeled 2D image points as well as the Jacobian (the
            // derivatives of each modeled point wrt each camera parameter). Any points in
            // outlierPoints are omitted from the model.
            CalibrationModel model =
                    new CalibrationModel(testPattern3dPoints, outlierPoints, flags);

            // A vector of the observed 2D image points (in x0, y0, x1, y1, ... order) is created
            // omitting any in outlierPoints
            RealVector observed = new ArrayRealVector();
            int iPoint = 0;
            for (int i = 0; i < numberOfTestPatterns; i++) {
                for (int j = 0; j < testPatternImagePoints[i].length; j++) {
                    if (!outlierPoints.contains(iPoint)) {
                        observed =
                                observed.append(new ArrayRealVector(testPatternImagePoints[i][j]));
                    }
                    iPoint++;
                }
            }

            RealVector start = new ArrayRealVector(parameters);

            ConvergenceChecker<LeastSquaresProblem.Evaluation> checker =
                    LeastSquaresFactory.evaluationChecker(new SimpleVectorValueChecker(1e-6, 1e-8));

            LeastSquaresProblem lsp = LeastSquaresFactory.create(model, observed, start, checker,
                    maxEvaluations, maxIterations);

            try {
                optimum = optimizer.optimize(lsp);
            }
            catch (TooManyEvaluationsException e) {
                throw(new Exception("Camera model failed to converge. Try increasing "
                        + "the number of Radial Lines Per Cal Z."));
            }
            Logger.trace("rms error = " + optimum.getRMS());
            Logger.trace("number of evaluations = " + optimum.getEvaluations());
            Logger.trace("number of iterations = " + optimum.getIterations());
            Logger.trace("parameters = " + optimum.getPoint()
                                                  .toString());

            if (outlierPoints.isEmpty()) {
                // To convert from RMS to to DRMS (that is from single dimensional RMS to Distance
                // RMS) we need to multiply by sqrt(2) and then to convert to variance everything
                // gets squared so it just becomes a multiply by 2
                double varianceThresholdForRejectingOutliers =
                        2 * Math.pow(sigmaThresholdForRejectingOutliers * optimum.getRMS(), 2);

                double[] residuals = optimum.getResiduals()
                                            .toArray();
                for (int i = 0; i < residuals.length / 2; i++) {
                    // Check the sum of the squares of the X and Y residuals against the threshold
                    if (residuals[2 * i] * residuals[2 * i] + residuals[2 * i + 1]
                            * residuals[2 * i + 1] > varianceThresholdForRejectingOutliers) {
                        outlierPoints.add(i);
                    }
                }
                outlierPointList.clear();
                outlierPointList.addAll(outlierPoints);

                if (outlierPoints.isEmpty()) {
                    Logger.trace("No outliers found in the data set");
                    // no outliers found so no need for a second attempt
                    break;
                }
                else {
                    Logger.trace("Index of outliers found in the data set: " + outlierPoints);
                    Logger.trace(
                            "Repeating parameter estimation with {} outliers removed from the "
                                    + "set of {} points",
                            outlierPoints.size(), residuals.length / 2);
                }
            }
        }

        RealVector ans = optimum.getPoint();
        if ((flags & FIX_ASPECT_RATIO) != 0) {
            ans.setEntry(1, ans.getEntry(0));
        }

        System.arraycopy(ans.toArray(), 0, parameters, 0, numberOfParameters);

        //Evaluate the model at all points using the best set of camera parameters
        CalibrationModel model = new CalibrationModel(testPattern3dPoints, null, flags);
        RealVector vectModeledImagePoints = model.value(ans).getFirst();
        int iPoint = 0;
        for (int i = 0; i < numberOfTestPatterns; i++) {
            modeledImagePoints[i] = new double[testPatternImagePoints[i].length][2];
            for (int j = 0; j < testPatternImagePoints[i].length; j++) {
                modeledImagePoints[i][j][0] = vectModeledImagePoints.getEntry(iPoint);
                iPoint++;
                modeledImagePoints[i][j][1] = vectModeledImagePoints.getEntry(iPoint);
                iPoint++;
            }
        }

        // return DRMS rather than RMS
        return Math.sqrt(2) * optimum.getRMS();
    }

    /**
     * This class implements the MultivariateJacobianFunction interface to return the modeled 2D 
     * image points as well as the Jacobian wrt the model parameters.
     */
    private static class CalibrationModel implements MultivariateJacobianFunction {

        double[][][] testPattern3dPoints;
        int totalNumberOfPoints;
        double allowCenterToChange;
        double allowDistortionToChange;
        double allowRotationToChange;
        TreeSet<Integer> outlierPoints;
        int flags;

        /**
         * Constructor for the calibration model
         * 
         * @param testPattern3dPoints - a numberOfTestPatterns x numberOfPointsPerTestPattern x 3
         *        array containing the 3D machine coordinates at which the corresponding image
         *        points were collected
         * @param outlierPoints - a set indices to image point outliers that should be excluded from
         *        the model
         * @param flags - Flags used to force certain parameters be retained from the initial
         *        starting values. Can be one or more of the follow added together:
         *        FIX_ASPECT_RATIO, FIX_CENTER_POINT, FIX_DISTORTION_COEFFICENTS, and FIX_ROTATION.
         */
        public CalibrationModel(double[][][] testPattern3dPoints, TreeSet<Integer> outlierPoints,
                int flags) {
            if (outlierPoints != null) {
                this.outlierPoints = outlierPoints;
            }
            else {
                this.outlierPoints = new TreeSet<Integer>();
            }
            this.flags = flags;
            this.testPattern3dPoints = testPattern3dPoints;
            totalNumberOfPoints = 0;
            for (int iTP = 0; iTP < numberOfTestPatterns; iTP++) {
                totalNumberOfPoints += testPattern3dPoints[iTP].length;
            }
            totalNumberOfPoints -= this.outlierPoints.size();

            if ((flags & FIX_PRINCIPAL_POINT) == 0) {
                allowCenterToChange = 1;
            }
            else {
                allowCenterToChange = 0;
            }
            if ((flags & FIX_DISTORTION_COEFFICENTS) == 0) {
                allowDistortionToChange = 1;
            }
            else {
                allowDistortionToChange = 0;
            }
            if ((flags & FIX_ROTATION) == 0) {
                allowRotationToChange = 1;
            }
            else {
                allowRotationToChange = 0;
            }
        }

        /**
         * This method is called whenever the solver needs to evaluate the model with a specific set
         * of camera parameters to obtain the modeled 2D image points and/or their Jacobian
         * 
         * @param cameraParameters - the camera parameters to use when evaluating the model supplied
         * in the following order: fx, fy, cx, cy, k1, k2, p1, p2, k3, Rx, Ry, Rz, cam_z, cam_x[0], 
         * cam_y[0], ... cam_x[numberOfTestPatterns-1], cam_y[numberOfTestPatterns-1]
         * @return the modeled 2D image points as a RealVector in the following order: x[0], y[0], 
         * x[1], y[1], ..., x[2*totalNumberOfPoints-1], y[2*totalNumberOfPoints-1]; and their 
         * Jacobian as a RealMatrix with rows in the same order as the point order and columns in 
         * the same order as the camera parameters
         */
        @Override
        public Pair<RealVector, RealMatrix> value(RealVector cameraParameters) {
            // parameter order is fx, fy, cx, cy, k1, k2, p1, p2, k3, Rx, Ry, Rz, cam_z,
            // cam_x[0], cam_y[0], ... cam_x[numberOfTestPatterns-1], cam_y[numberOfTestPatterns-1]
            RealVector funcValue =
                    MatrixUtils.createRealVector(new double[2 * totalNumberOfPoints]);
            RealMatrix funcJacobian =
                    MatrixUtils.createRealMatrix(2 * totalNumberOfPoints, numberOfParameters);
            double fx = cameraParameters.getEntry(0);
            double fy;
            if ((flags & FIX_ASPECT_RATIO) == 0) {
                fy = cameraParameters.getEntry(1);
            }
            else {
                fy = aspectRatio * cameraParameters.getEntry(0);
            }
            double cx = cameraParameters.getEntry(2);
            double cy = cameraParameters.getEntry(3);
            double k1 = cameraParameters.getEntry(4);
            double k2 = cameraParameters.getEntry(5);
            double p1 = cameraParameters.getEntry(6);
            double p2 = cameraParameters.getEntry(7);
            double k3 = cameraParameters.getEntry(8);
            double rx = cameraParameters.getEntry(9);
            double ry = cameraParameters.getEntry(10);
            double rz = cameraParameters.getEntry(11);
            if (rx == 0 && ry == 0 && rz == 0) {
                rz = 1e-6;
            }
            double camZ = cameraParameters.getEntry(12);

            // Note: all variables of the form tempnnn are the result of common subexpression
            // optimization performed in SageMath
            double temp010 = rz * rz;
            double temp009 = ry * ry;
            double temp008 = rx * rx;
            double temp105 = temp010 * rz;
            double temp090 = temp009 * ry;
            double temp062 = temp008 * rx;
            double temp007 = temp008 + temp009 + temp010;
            double temp013 = Math.sqrt(temp007);
            double temp017 = 1.0 / temp013;
            double temp050 = temp017 / temp007;
            double temp018 = Math.sin(temp013);
            double temp051 = ry * temp008 * temp018 * temp050;
            double temp064 = rx * ry * temp018 * temp050;
            double temp084 = ry * temp010 * temp018 * temp050;
            double temp016 = rz * temp017 * temp018;
            double temp108 = temp010 * temp018 * temp050;
            double temp092 = rx * temp010 * temp018 * temp050;
            double temp059 = temp008 * temp018 * temp050;
            double temp028 = ry * temp017 * temp018;
            double temp056 = rx * ry * rz * temp018 * temp050;
            double temp088 = ry * rz * temp018 * temp050;
            double temp095 = rx * temp009 * temp018 * temp050;
            double temp065 = rz * temp008 * temp018 * temp050;
            double temp049 = rx * rz * temp018 * temp050;
            double temp080 = temp009 * temp018 * temp050;
            double temp081 = rz * temp009 * temp018 * temp050;
            double temp060 = temp017 * temp018;
            double temp037 = rx * temp017 * temp018;
            double temp012 = Math.cos(temp013);
            double temp011 = temp012 - 1;
            double temp006 = 1.0 / temp007;
            double temp053 = temp006 * temp006;
            double temp052 = 2 * ry * temp008 * temp011 * temp053;
            double temp066 = 2 * rz * temp008 * temp011 * temp053;
            double temp085 = 2 * ry * temp010 * temp011 * temp053;
            double temp094 = 2 * rx * temp009 * temp011 * temp053;
            double temp082 = 2 * rz * temp009 * temp011 * temp053;
            double temp091 = 2 * rx * temp010 * temp011 * temp053;
            double temp057 = 2 * rx * ry * rz * temp011 * temp053;
            double temp031 = temp006 * temp010 * temp011 - temp012;
            double temp015 = rx * ry * temp006 * temp011;
            double temp014 = temp015 + temp016;
            double temp034 = temp015 - temp016;
            double temp048 = ry * temp006 * temp011;
            double temp058 = temp006 * temp008 * temp012;
            double temp030 = ry * rz * temp006 * temp011;
            double temp029 = -rx * temp017 * temp018 + temp030;
            double temp036 = temp030 + temp037;
            double temp079 = temp006 * temp009 * temp012;
            double temp083 = rz * temp006 * temp011;
            double temp087 = ry * rz * temp006 * temp012;
            double temp020 = rx * rz * temp006 * temp011;
            double temp019 = -ry * temp017 * temp018 + temp020;
            double temp027 = temp020 + temp028;
            double temp035 = temp006 * temp009 * temp011 - temp012;
            double temp047 = rx * rz * temp006 * temp012;
            double temp063 = rx * ry * temp006 * temp012;
            double temp096 = rx * temp006 * temp011;
            double temp107 = temp006 * temp010 * temp012;

            int rowIdx = 0;
            int iPoint = 0;
            for (int iTP = 0; iTP < numberOfTestPatterns; iTP++) {
                for (int iPt = 0; iPt < testPattern3dPoints[iTP].length; iPt++) {
                    if (!outlierPoints.contains(iPoint)) {
                        double temp023 = camZ - testPattern3dPoints[iTP][iPt][2];
                        double temp022 = cameraParameters.getEntry(14 + 2 * iTP)
                                - testPattern3dPoints[iTP][iPt][1];
                        double temp021 = cameraParameters.getEntry(13 + 2 * iTP)
                                - testPattern3dPoints[iTP][iPt][0];
                        double temp026 = temp021 * temp027 + temp022 * temp029 + temp023 * temp031;
                        double temp043 = 1.0 / temp026;
                        double temp025 = temp043 * temp043;
                        double temp076 = temp043 * temp025;
                        double temp033 = temp021 * temp034 + temp022 * temp035 + temp023 * temp036;
                        double temp032 = temp033 * temp033;
                        double temp121 = -temp031 * temp032 * temp076;
                        double temp133 = -temp029 * temp032 * temp076;
                        double temp024 = temp025 * temp032;
                        double temp127 = -temp027 * temp032 * temp076;
                        double temp131 = temp025 * temp033 * temp035;
                        double temp125 = temp025 * temp033 * temp034;
                        double temp119 = temp025 * temp033 * temp036;
                        double temp046 = -temp022 * (temp037 - temp094 - temp095)
                                + temp021 * (temp047 - temp048 - temp049 + temp051 + temp052)
                                + temp023 * (temp056 + temp057 - temp058 + temp059 - temp060);
                        double temp073 = temp025 * temp033 * temp046;
                        double temp078 = temp023 * (temp028 - temp084 - temp085)
                                - temp021 * (temp056 + temp057 - temp060 - temp079 + temp080)
                                - temp022 * (temp063 - temp064 + temp081 + temp082 - temp083);
                        double temp101 = temp032 * temp076 * temp078;
                        double temp068 = -temp023 * (temp037 - temp091 - temp092)
                                + temp022 * (temp056 + temp057 + temp058 - temp059 + temp060)
                                - temp021 * (temp063 - temp064 - temp065 - temp066 + temp083);
                        double temp077 = -temp032 * temp068 * temp076;
                        double temp061 = -(2 * rx * temp006 * temp011 - temp018 * temp050 * temp062
                                - 2 * temp011 * temp053 * temp062 + temp037) * temp021
                                - temp022 * (temp047 + temp048 - temp049 - temp051 - temp052)
                                + temp023 * (temp063 - temp064 + temp065 + temp066 - temp083);
                        double temp097 = temp021 * (temp028 - temp051 - temp052)
                                - temp023 * (temp056 + temp057 + temp060 + temp079 - temp080)
                                + temp022 * (temp087 - temp088 - temp094 - temp095 + temp096);
                        double temp086 = -(2 * ry * temp006 * temp011 - temp018 * temp050 * temp090
                                - 2 * temp011 * temp053 * temp090 + temp028) * temp022
                                - temp023 * (temp063 - temp064 - temp081 - temp082 + temp083)
                                + temp021 * (temp087 - temp088 + temp094 + temp095 - temp096);
                        double temp102 = temp025 * temp033 * temp086;
                        double temp104 = -(2 * rz * temp006 * temp011 - temp018 * temp050 * temp105
                                - 2 * temp011 * temp053 * temp105 + temp016) * temp023
                                + temp022 * (temp047 - temp048 - temp049 + temp084 + temp085)
                                - temp021 * (temp087 - temp088 - temp091 - temp092 + temp096);
                        double temp113 = temp032 * temp076 * temp104;
                        double temp106 = -(temp016 - temp081 - temp082) * temp022
                                - temp023 * (temp047 + temp048 - temp049 - temp084 - temp085)
                                + temp021 * (temp056 + temp057 + temp060 + temp107 - temp108);
                        double temp114 = -temp025 * temp033 * temp106;
                        double temp109 = (temp016 - temp065 - temp066) * temp021
                                - temp022 * (temp056 + temp057 - temp060 - temp107 + temp108)
                                - temp023 * (temp087 - temp088 + temp091 + temp092 - temp096);
                        double temp005 = temp006 * temp008 * temp011 - temp012;
                        double temp004 = temp005 * temp021 + temp014 * temp022 + temp019 * temp023;
                        double temp118 = temp004 * temp019 * temp025;
                        double temp130 = temp004 * temp014 * temp025;
                        double temp103 = -temp004 * temp025 * temp097;
                        double temp124 = temp004 * temp005 * temp025;
                        double temp074 = temp004 * temp025 * temp061;
                        double temp115 = temp004 * temp025 * temp109;
                        double temp003 = temp004 * temp004;
                        double temp126 = -temp003 * temp027 * temp076;
                        double temp123 = temp124 + temp125 + temp126 + temp127;
                        double temp075 = -temp003 * temp068 * temp076;
                        double temp072 = temp073 + temp074 + temp075 + temp077;
                        double temp112 = temp003 * temp076 * temp104;
                        double temp111 = temp112 + temp113 + temp114 + temp115;
                        double temp120 = -temp003 * temp031 * temp076;
                        double temp117 = temp118 + temp119 + temp120 + temp121;
                        double temp132 = -temp003 * temp029 * temp076;
                        double temp129 = temp130 + temp131 + temp132 + temp133;
                        double temp041 = temp003 * temp025;
                        double temp040 = temp024 + temp041;
                        double temp042 = temp040 * temp040;
                        double temp128 = 2 * k2 * temp040 * temp129 + 3 * k3 * temp042 * temp129
                                + k1 * temp129;
                        double temp116 = 2 * k2 * temp040 * temp117 + 3 * k3 * temp042 * temp117
                                + k1 * temp117;
                        double temp110 = 2 * k2 * temp040 * temp111 + 3 * k3 * temp042 * temp111
                                + k1 * temp111;
                        double temp071 = 2 * k2 * temp040 * temp072 + 3 * k3 * temp042 * temp072
                                + k1 * temp072;
                        double temp122 = 2 * k2 * temp040 * temp123 + 3 * k3 * temp042 * temp123
                                + k1 * temp123;
                        double temp039 = temp040 * temp042;
                        double temp038 = k3 * temp039 + k1 * temp040 + k2 * temp042 + 1;
                        double temp045 = 3 * temp025 * temp032 + temp041;
                        double temp044 = 2 * p2 * temp004 * temp025 * temp033
                                + temp033 * temp038 * temp043 + p1 * temp045;
                        double temp100 = temp003 * temp076 * temp078;
                        double temp099 = temp100 + temp101 + temp102 + temp103;
                        double temp098 = 2 * k2 * temp040 * temp099 + 3 * k3 * temp042 * temp099
                                + k1 * temp099;
                        double temp002 = 3 * temp003 * temp025 + temp024;
                        double temp001 = 2 * p1 * temp004 * temp025 * temp033
                                + temp004 * temp038 * temp043 + p2 * temp002;

                        // the x component of the modeled image 2D point
                        funcValue.setEntry(rowIdx, fx * temp001 + cx);

                        // the partial derivatives of x wrt to each of the camera parameters
                        funcJacobian.setEntry(rowIdx, 0, temp001);
                        funcJacobian.setEntry(rowIdx, 1, 0);
                        funcJacobian.setEntry(rowIdx, 2, 1 * allowCenterToChange);
                        funcJacobian.setEntry(rowIdx, 3, 0 * allowCenterToChange);
                        funcJacobian.setEntry(rowIdx, 4,
                                fx * temp004 * temp040 * temp043 * allowDistortionToChange);
                        funcJacobian.setEntry(rowIdx, 5,
                                fx * temp004 * temp042 * temp043 * allowDistortionToChange);
                        funcJacobian.setEntry(rowIdx, 6,
                                2 * fx * temp004 * temp025 * temp033 * allowDistortionToChange);
                        funcJacobian.setEntry(rowIdx, 7, fx * temp002 * allowDistortionToChange);
                        funcJacobian.setEntry(rowIdx, 8,
                                fx * temp004 * temp039 * temp043 * allowDistortionToChange);
                        funcJacobian.setEntry(rowIdx, 9,
                                (4 * p1 * temp004 * temp033 * temp068 * temp076
                                        - 2 * p1 * temp004 * temp025 * temp046
                                        - 2 * p1 * temp025 * temp033 * temp061
                                        + temp004 * temp025 * temp038 * temp068
                                        - temp038 * temp043 * temp061
                                        - 2 * temp004 * temp043 * temp071
                                        - 2 * (3 * temp004 * temp025 * temp061
                                                - 3 * temp003 * temp068 * temp076 + temp073
                                                + temp077) * p2)
                                        * fx * allowRotationToChange);
                        funcJacobian.setEntry(rowIdx, 10,
                                -(4 * p1 * temp004 * temp033 * temp076 * temp078
                                        + temp004 * temp025 * temp038 * temp078
                                        + 2 * p1 * temp004 * temp025 * temp086
                                        - 2 * p1 * temp025 * temp033 * temp097
                                        - temp038 * temp043 * temp097
                                        + 2 * temp004 * temp043 * temp098
                                        + 2 * (3 * temp003 * temp076 * temp078
                                                - 3 * temp004 * temp025 * temp097 + temp101
                                                + temp102) * p2)
                                        * fx * allowRotationToChange);
                        funcJacobian.setEntry(rowIdx, 11,
                                (4 * p1 * temp004 * temp033 * temp076 * temp104
                                        + temp004 * temp025 * temp038 * temp104
                                        - 2 * p1 * temp004 * temp025 * temp106
                                        + 2 * p1 * temp025 * temp033 * temp109
                                        + temp038 * temp043 * temp109
                                        + 2 * temp004 * temp043 * temp110
                                        + 2 * (3 * temp003 * temp076 * temp104
                                                + 3 * temp004 * temp025 * temp109 + temp113
                                                + temp114) * p2)
                                        * fx * allowRotationToChange);
                        funcJacobian.setEntry(rowIdx, 12,
                                -(4 * p1 * temp004 * temp031 * temp033 * temp076
                                        - 2 * p1 * temp019 * temp025 * temp033
                                        - 2 * p1 * temp004 * temp025 * temp036
                                        + temp004 * temp025 * temp031 * temp038
                                        - temp019 * temp038 * temp043
                                        - 2 * temp004 * temp043 * temp116
                                        - 2 * (3 * temp004 * temp019 * temp025
                                                - 3 * temp003 * temp031 * temp076 + temp119
                                                + temp121) * p2)
                                        * fx);
                        funcJacobian.setEntry(rowIdx, 13 + 2 * iTP,
                                -(4 * p1 * temp004 * temp027 * temp033 * temp076
                                        - 2 * p1 * temp005 * temp025 * temp033
                                        - 2 * p1 * temp004 * temp025 * temp034
                                        + temp004 * temp025 * temp027 * temp038
                                        - temp005 * temp038 * temp043
                                        - 2 * temp004 * temp043 * temp122
                                        - 2 * (3 * temp004 * temp005 * temp025
                                                - 3 * temp003 * temp027 * temp076 + temp125
                                                + temp127) * p2)
                                        * fx);
                        funcJacobian.setEntry(rowIdx, 14 + 2 * iTP,
                                -(4 * p1 * temp004 * temp029 * temp033 * temp076
                                        - 2 * p1 * temp014 * temp025 * temp033
                                        - 2 * p1 * temp004 * temp025 * temp035
                                        + temp004 * temp025 * temp029 * temp038
                                        - temp014 * temp038 * temp043
                                        - 2 * temp004 * temp043 * temp128
                                        - 2 * (3 * temp004 * temp014 * temp025
                                                - 3 * temp003 * temp029 * temp076 + temp131
                                                + temp133) * p2)
                                        * fx);
                        rowIdx++;

                        // the y component of the modeled image 2D point
                        funcValue.setEntry(rowIdx, fy * temp044 + cy);

                        // the partial derivatives of y wrt to each of the camera parameters
                        funcJacobian.setEntry(rowIdx, 0, 0);
                        if ((flags & FIX_ASPECT_RATIO) == 0) {
                            funcJacobian.setEntry(rowIdx, 0, 0);
                            funcJacobian.setEntry(rowIdx, 1, temp044);
                        }
                        else {
                            funcJacobian.setEntry(rowIdx, 0, aspectRatio * temp044);
                            funcJacobian.setEntry(rowIdx, 1, 0);
                        }
                        funcJacobian.setEntry(rowIdx, 2, 0 * allowCenterToChange);
                        funcJacobian.setEntry(rowIdx, 3, 1 * allowCenterToChange);
                        funcJacobian.setEntry(rowIdx, 4,
                                fy * temp033 * temp040 * temp043 * allowDistortionToChange);
                        funcJacobian.setEntry(rowIdx, 5,
                                fy * temp033 * temp042 * temp043 * allowDistortionToChange);
                        funcJacobian.setEntry(rowIdx, 6, fy * temp045 * allowDistortionToChange);
                        funcJacobian.setEntry(rowIdx, 7,
                                2 * fy * temp004 * temp025 * temp033 * allowDistortionToChange);
                        funcJacobian.setEntry(rowIdx, 8,
                                fy * temp033 * temp039 * temp043 * allowDistortionToChange);
                        funcJacobian.setEntry(rowIdx, 9,
                                (4 * p2 * temp004 * temp033 * temp068 * temp076
                                        - 2 * p2 * temp004 * temp025 * temp046
                                        - 2 * p2 * temp025 * temp033 * temp061
                                        + temp025 * temp033 * temp038 * temp068
                                        - temp038 * temp043 * temp046
                                        - 2 * temp033 * temp043 * temp071
                                        - 2 * (3 * temp025 * temp033 * temp046
                                                - 3 * temp032 * temp068 * temp076 + temp074
                                                + temp075) * p1)
                                        * fy * allowRotationToChange);
                        funcJacobian.setEntry(rowIdx, 10,
                                -(4 * p2 * temp004 * temp033 * temp076 * temp078
                                        + temp025 * temp033 * temp038 * temp078
                                        + 2 * p2 * temp004 * temp025 * temp086
                                        - 2 * p2 * temp025 * temp033 * temp097
                                        + temp038 * temp043 * temp086
                                        + 2 * temp033 * temp043 * temp098
                                        + 2 * (3 * temp032 * temp076 * temp078
                                                + 3 * temp025 * temp033 * temp086 + temp100
                                                + temp103) * p1)
                                        * fy * allowRotationToChange);
                        funcJacobian.setEntry(rowIdx, 11,
                                (4 * p2 * temp004 * temp033 * temp076 * temp104
                                        + temp025 * temp033 * temp038 * temp104
                                        - 2 * p2 * temp004 * temp025 * temp106
                                        + 2 * p2 * temp025 * temp033 * temp109
                                        - temp038 * temp043 * temp106
                                        + 2 * temp033 * temp043 * temp110
                                        + 2 * (3 * temp032 * temp076 * temp104
                                                - 3 * temp025 * temp033 * temp106 + temp112
                                                + temp115) * p1)
                                        * fy * allowRotationToChange);
                        funcJacobian.setEntry(rowIdx, 12,
                                -(4 * p2 * temp004 * temp031 * temp033 * temp076
                                        - 2 * p2 * temp019 * temp025 * temp033
                                        - 2 * p2 * temp004 * temp025 * temp036
                                        + temp025 * temp031 * temp033 * temp038
                                        - temp036 * temp038 * temp043
                                        - 2 * temp033 * temp043 * temp116
                                        - 2 * (3 * temp025 * temp033 * temp036
                                                - 3 * temp031 * temp032 * temp076 + temp118
                                                + temp120) * p1)
                                        * fy);
                        funcJacobian.setEntry(rowIdx, 13 + 2 * iTP,
                                -(4 * p2 * temp004 * temp027 * temp033 * temp076
                                        - 2 * p2 * temp005 * temp025 * temp033
                                        - 2 * p2 * temp004 * temp025 * temp034
                                        + temp025 * temp027 * temp033 * temp038
                                        - temp034 * temp038 * temp043
                                        - 2 * temp033 * temp043 * temp122
                                        - 2 * (3 * temp025 * temp033 * temp034
                                                - 3 * temp027 * temp032 * temp076 + temp124
                                                + temp126) * p1)
                                        * fy);
                        funcJacobian.setEntry(rowIdx, 14 + 2 * iTP,
                                -(4 * p2 * temp004 * temp029 * temp033 * temp076
                                        - 2 * p2 * temp014 * temp025 * temp033
                                        - 2 * p2 * temp004 * temp025 * temp035
                                        + temp025 * temp029 * temp033 * temp038
                                        - temp035 * temp038 * temp043
                                        - 2 * temp033 * temp043 * temp128
                                        - 2 * (3 * temp025 * temp033 * temp035
                                                - 3 * temp029 * temp032 * temp076 + temp130
                                                + temp132) * p1)
                                        * fy);
                        rowIdx++;
                    }
                    iPoint++;
                }
            }
            return new Pair<RealVector, RealMatrix>(funcValue, funcJacobian);
        }

    }

    /**
     * Computes the rectification matrix that converts normalized physical camera coordinates to the
     * virtual camera coordinates
     * 
     * @param transformFromMachToPhyCamRefFrame - the 3x3 rotation matrix that converts vector
     *        components from the machine coordinate system to the physical camera coordinate system
     * @param vectorFromMachToPhyCamInMachRefFrame - the 3x1 vector from the machine origin to the
     *        physical camera origin with components represented in the machine coordinate system
     * @param transformFromMachToVirCamRefFrame - the 3x3 rotation matrix that converts vector
     *        components from the machine coordinate system to the virtual camera coordinate system
     * @param vectorFromMachToVirCamInMachRefFrame - the 3x1 vector from the machine origin to the
     *        virtual camera origin with components represented in the machine coordinate system
     * @param primaryZ - the machine Z coordinate, in millimeters, that is used as the default
     *        imaging height
     * @return
     */
    public static Mat computeRectificationMatrix(Mat transformFromMachToPhyCamRefFrame,
            Mat vectorFromMachToPhyCamInMachRefFrame, Mat transformFromMachToVirCamRefFrame,
            Mat vectorFromMachToVirCamInMachRefFrame, double primaryZ) {

        double primaryZToPhyCamZ = vectorFromMachToPhyCamInMachRefFrame.get(2, 0)[0] - primaryZ;

        Mat transformFromPhyCamToVirCamRefFrame = Mat.eye(3, 3, CvType.CV_64FC1);
        Core.gemm(transformFromMachToVirCamRefFrame, transformFromMachToPhyCamRefFrame.t(), 1,
                transformFromMachToPhyCamRefFrame, 0, transformFromPhyCamToVirCamRefFrame);

        Mat vectorFromVirCamToPhyCamInMachRefFrame = Mat.zeros(3, 1, CvType.CV_64FC1);
        Core.subtract(vectorFromMachToPhyCamInMachRefFrame, vectorFromMachToVirCamInMachRefFrame,
                vectorFromVirCamToPhyCamInMachRefFrame);

        Mat vectorFromVirCamToPhyCamInVirCamRefFrame = Mat.zeros(3, 1, CvType.CV_64FC1);
        Core.gemm(transformFromMachToVirCamRefFrame, vectorFromVirCamToPhyCamInMachRefFrame, 1,
                vectorFromVirCamToPhyCamInMachRefFrame, 0,
                vectorFromVirCamToPhyCamInVirCamRefFrame);

        // A set of normalized physical camera points is chosen, the quantity and the specific 
        // points are not important although at least four of the points need to form a 
        // quadrilateral. Note that because these are normalized coordinates, the Z coordinate of 
        // all points is assumed to be 1
        MatOfPoint2f cameraPoints = new MatOfPoint2f();
        cameraPoints.push_back(new MatOfPoint2f(new Point(0, 0)));
        cameraPoints.push_back(new MatOfPoint2f(new Point(-100, -100)));
        cameraPoints.push_back(new MatOfPoint2f(new Point(+100, -100)));
        cameraPoints.push_back(new MatOfPoint2f(new Point(+100, +100)));
        cameraPoints.push_back(new MatOfPoint2f(new Point(-100, +100)));

        MatOfPoint2f cameraHatPoints = new MatOfPoint2f();

        // For each of the normalized physical camera points, compute its normalized coordinates in
        // the virtual camera's coordinate system by projecting the point onto the defaultZ plane
        // and then normalizing the vector from the virtual camera to the point
        for (int i = 0; i < cameraPoints.rows(); i++) {
            // The normalized physical camera point is designated as point PPrime
            Mat vectorFromPhyCamToPPrimeInPhyCamRefFrame = Mat.ones(3, 1, CvType.CV_64FC1); // Z = 1
            vectorFromPhyCamToPPrimeInPhyCamRefFrame.put(0, 0, cameraPoints.get(i, 0));
            Logger.trace("vectorFromPhyCamToPPrimeInPhyCamRefFrame = "
                    + vectorFromPhyCamToPPrimeInPhyCamRefFrame.dump());

            Mat vectorFromPhyCamToPPrimeInVirCamRefFrame = Mat.zeros(3, 1, CvType.CV_64FC1);
            Core.gemm(transformFromPhyCamToVirCamRefFrame, vectorFromPhyCamToPPrimeInPhyCamRefFrame,
                    1, vectorFromPhyCamToPPrimeInPhyCamRefFrame, 0,
                    vectorFromPhyCamToPPrimeInVirCamRefFrame);
            vectorFromPhyCamToPPrimeInPhyCamRefFrame.release();
            Logger.trace("vectorFromPhyCamToPPrimeInVirCamRefFrame = "
                    + vectorFromPhyCamToPPrimeInVirCamRefFrame.dump());

            // Scale the vector so that its Z component is the height of the physical camera above
            // primary Z. This will place the tip of the vector on the primary Z plane. This point
            // on the primary Z plane is designated as point P.
            Mat vectorFromPhyCamToPInVirCamRefFrame = Mat.zeros(3, 1, CvType.CV_64FC1);
            Core.multiply(vectorFromPhyCamToPPrimeInVirCamRefFrame,
                    new Scalar(primaryZToPhyCamZ
                            / vectorFromPhyCamToPPrimeInVirCamRefFrame.get(2, 0)[0]),
                    vectorFromPhyCamToPInVirCamRefFrame);
            vectorFromPhyCamToPPrimeInVirCamRefFrame.release();
            Logger.trace("vectorFromPhyCamToPInVirCamRefFrame = "
                    + vectorFromPhyCamToPInVirCamRefFrame.dump());

            Mat vectorFromVirCamToPInVirCamRefFrame = Mat.zeros(3, 1, CvType.CV_64FC1);
            Core.add(vectorFromPhyCamToPInVirCamRefFrame, vectorFromVirCamToPhyCamInVirCamRefFrame,
                    vectorFromVirCamToPInVirCamRefFrame);
            vectorFromPhyCamToPInVirCamRefFrame.release();
            Logger.trace("vectorFromVirCamToPInVirCamRefFrame = "
                    + vectorFromVirCamToPInVirCamRefFrame.dump());

            // Normalize the vector so that its Z component is 1. The tip of this vector is
            // designated PHatPrime
            Mat vectorFromVirCamToPHatPrimeInVirCamRefFrame = Mat.zeros(3, 1, CvType.CV_64FC1);
            Core.multiply(vectorFromVirCamToPInVirCamRefFrame,
                    new Scalar(1.0 / vectorFromVirCamToPInVirCamRefFrame.get(2, 0)[0]),
                    vectorFromVirCamToPHatPrimeInVirCamRefFrame);
            vectorFromVirCamToPInVirCamRefFrame.release();
            Logger.trace("vectorFromVirCamToPHatPrimeInVirCamRefFrame = "
                    + vectorFromVirCamToPHatPrimeInVirCamRefFrame.dump());

            // Save the normalized point
            cameraHatPoints.push_back(new MatOfPoint2f(
                    new Point(vectorFromVirCamToPHatPrimeInVirCamRefFrame.get(0, 0)[0],
                            vectorFromVirCamToPHatPrimeInVirCamRefFrame.get(1, 0)[0])));
            vectorFromVirCamToPHatPrimeInVirCamRefFrame.release();
        }

        // The rectification matrix is the homography matrix that takes the physical camera points
        // to the virtual camera points
        Mat rectification = Calib3d.findHomography(cameraPoints, cameraHatPoints);

        // Cleanup
        cameraPoints.release();
        cameraHatPoints.release();
        transformFromPhyCamToVirCamRefFrame.release();
        vectorFromVirCamToPhyCamInMachRefFrame.release();
        vectorFromVirCamToPhyCamInVirCamRefFrame.release();

        return rectification;
    }

    /**
     * Computes the virtual camera matrix. This is similar to openCV's getOptimalNewCameraMatrix
     * except that it also takes into account any image rectification.
     * 
     * @param physicalCameraMatrix - the physical camera's intrinsic matrix
     * @param distortionCoefficients - the physical camera's lens distortion coefficients
     * @param rectification - the rectification matrix that takes the physical camera points to the
     *        virtual camera points
     * @param size - the physical camera's image size
     * @param alpha - a free scaling parameter in the range 0 to 1 inclusive. A value of a zero
     *        ensures only valid image pixels are displayed but may result in the loss of some valid
     *        pixels around the edge of the image. A value of one ensure all valid pixels are
     *        displayed but that may result in some invalid (usually black) pixels being displayed
     *        around the edge of the image.
     * @param keepPrincipalPoint - if set true, the virtual camera's principal point is set to match
     *        that of the physical camera's principal point
     * @return the virtual camera's intrinsic camera matrix
     */
    public static Mat computeVirtualCameraMatrix(Mat physicalCameraMatrix,
            Mat distortionCoefficients, Mat rectification, Size size, double alpha,
            boolean keepPrincipalPoint) {
        Mat principalPoint = null;
        if (keepPrincipalPoint) {
            MatOfPoint2f point = new MatOfPoint2f();
            point.push_back(new MatOfPoint2f(new org.opencv.core.Point(
                    physicalCameraMatrix.get(0, 2)[0], physicalCameraMatrix.get(1, 2)[0])));

            MatOfPoint2f centerPoint = new MatOfPoint2f();

            // Compute the corresponding point in the undistorted and rectified image
            Calib3d.undistortPoints(point, centerPoint, physicalCameraMatrix,
                    distortionCoefficients, rectification);
            point.release();

            principalPoint = Mat.ones(3, 1, CvType.CV_64FC1); // Z = 1
            principalPoint.put(0, 0, centerPoint.get(0, 0)[0]);
            principalPoint.put(1, 0, centerPoint.get(0, 0)[1]);
            centerPoint.release();
        }

        Mat ret = computeVirtualCameraMatrix(physicalCameraMatrix, distortionCoefficients,
                rectification, size, alpha, principalPoint);
        principalPoint.release();

        return ret;
    }

    /**
     * Computes the virtual camera matrix. This is similar to openCV's getOptimalNewCameraMatrix
     * except that it also takes into account image rectification.
     * 
     * @param physicalCameraMatrix - the physical camera's intrinsic matrix
     * @param distortionCoefficients - the physical camera's lens distortion coefficients
     * @param rectification - the rectification matrix that takes the physical camera points to the
     *        virtual camera points
     * @param size - the physical camera's image size
     * @param alpha - a free scaling parameter in the range 0 to 1 inclusive. A value of a zero
     *        ensures only valid image pixels are displayed but may result in the loss of some valid
     *        pixels around the edge of the image. A value of one ensure all valid pixels are
     *        displayed but that may result in some invalid (usually black) pixels being displayed
     *        around the edge of the image.
     * @param principalPoint - a 3x1 matrix containing the point in physical camera coordinates of
     *        the desired principal point of the virtual camera, if null, the principal point will
     *        be computed so as to maximize the valid pixels in the resulting image
     * @return the virtual camera's intrinsic camera matrix
     */
    public static Mat computeVirtualCameraMatrix(Mat physicalCameraMatrix,
            Mat distortionCoefficients, Mat rectification, Size size, double alpha,
            Mat principalPoint) {
        // Generate a set of points around the outer perimeter of the distorted unrectifed image
        int numberOfPointsPerSide = 250;
        MatOfPoint2f distortedPoints = new MatOfPoint2f();
        double xStep = (size.width - 1) / (numberOfPointsPerSide - 1);
        double yStep = (size.height - 1) / (numberOfPointsPerSide - 1);
        for (int iSidePt = 0; iSidePt < numberOfPointsPerSide - 1; iSidePt++) {
            // down the left side
            distortedPoints.push_back(
                    new MatOfPoint2f(new org.opencv.core.Point(0, iSidePt * yStep)));

            // left-to-right across the bottom
            distortedPoints.push_back(
                    new MatOfPoint2f(new org.opencv.core.Point(iSidePt * xStep, size.height - 1)));

            // up the right side
            distortedPoints.push_back(new MatOfPoint2f(new org.opencv.core.Point(size.width - 1,
                    (numberOfPointsPerSide - 1 - iSidePt) * yStep)));

            // right-to-left across the top
            distortedPoints.push_back(new MatOfPoint2f(
                    new org.opencv.core.Point((numberOfPointsPerSide - 1 - iSidePt) * xStep, 0)));
        }

        MatOfPoint2f undistortedPoints = new MatOfPoint2f();

        // Compute the corresponding points in the undistorted and rectified image
        Calib3d.undistortPoints(distortedPoints, undistortedPoints, physicalCameraMatrix,
                distortionCoefficients, rectification);
        distortedPoints.release();

        double aspectRatio = size.height / size.width;

        boolean keepPrincipalPoint = false;
        double centerX = 0;
        double centerY = 0;
        if (principalPoint != null) {
            keepPrincipalPoint = true;

            // Convert the desired principal point from physical camera to virtual camera
            // coordinates
            Mat virCamPrincipalPoint = Mat.zeros(3, 1, CvType.CV_64FC1);
            Core.gemm(rectification, principalPoint, 1.0, principalPoint, 0.0,
                    virCamPrincipalPoint);

            // Set the center to the normalized virtual camera coordinates
            centerX = virCamPrincipalPoint.get(0, 0)[0] / virCamPrincipalPoint.get(2, 0)[0];
            centerY = virCamPrincipalPoint.get(1, 0)[0] / virCamPrincipalPoint.get(2, 0)[0];
            virCamPrincipalPoint.release();
        }

        // First find the outer bounding rectangle (of the correct aspect ratio) that encloses all
        // of
        // the undistorted image. This corresponds to the edge of the image for alpha = 1. If the
        // principal point was specified, the center of the bounding rectangle is placed at that
        // point and the rectangle is sized so that it just touches the boundary of the undistorted
        // image most distance from the center; otherwise, the center is placed such that the
        // bounding rectangle is as small as possible but still encloses all of the undistorted
        // image.
        double outerMaxX = Double.NEGATIVE_INFINITY;
        double outerMinX = Double.POSITIVE_INFINITY;
        double outerMaxY = Double.NEGATIVE_INFINITY;
        double outerMinY = Double.POSITIVE_INFINITY;

        // Check each undistorted point to see if it is an extreme point
        for (int i = 0; i < undistortedPoints.rows(); i++) {
            double[] pt = undistortedPoints.get(i, 0);
            if (pt[0] > outerMaxX) {
                outerMaxX = pt[0];
            }
            else if (pt[0] < outerMinX) {
                outerMinX = pt[0];
            }
            if (pt[1] > outerMaxY) {
                outerMaxY = pt[1];
            }
            else if (pt[1] < outerMinY) {
                outerMinY = pt[1];
            }
        }

        // If principal point was specified, adjust the extremes to keep them centered about the
        // specified point
        if (keepPrincipalPoint) {
            double outer = Math.max(outerMaxX - centerX, centerX - outerMinX);
            outerMaxX = centerX + outer;
            outerMinX = centerX - outer;
            outer = Math.max(outerMaxY - centerY, centerY - outerMinY);
            outerMaxY = centerY + outer;
            outerMinY = centerY - outer;
        }

        // Find the center of the extreme points
        double outerCenterX = (outerMaxX + outerMinX) / 2;
        double outerCenterY = (outerMaxY + outerMinY) / 2;

        // Compute the normalized coordinate to pixel scaling (this is the f terms in the intrinsic
        // camera matrix)
        double outerF;
        if ((outerMaxY - outerMinY) / (outerMaxX - outerMinX) >= aspectRatio) {
            // Constrained by the height
            outerF = size.height / (outerMaxY - outerMinY);
        }
        else {
            // Constrained by the width
            outerF = size.width / (outerMaxX - outerMinX);
        }

        // Compute the location of the normalized camera coordinate (0, 0, 1) in pixels relative to
        // the upper left corner of the image (the c terms in the intrinsic camera matrix)
        double outerCx = (size.width - 1) / 2 - outerF * outerCenterX;
        double outerCy = (size.height - 1) / 2 - outerF * outerCenterY;

        // Now find the inscribed rectangle (of the correct aspect ratio) that just fits within the
        // undistorted image. This corresponds to the edge of the image for alpha = 0. If the
        // principal point was specified, the center of the rectangle is placed at that point and
        // the rectangle is sized so that it just touches the edge of the undistorted image nearest 
        // to the center; otherwise, the center of the rectangle is placed such that the inscribed
        // rectangle is as large as possible but is still within the bounds of the undistorted
        // image.
        double innerMinX = Double.NEGATIVE_INFINITY;
        double innerMaxX = Double.POSITIVE_INFINITY;
        double innerMinY = Double.NEGATIVE_INFINITY;
        double innerMaxY = Double.POSITIVE_INFINITY;

        // To find the inscribed rectangle, the algorithm needs to start with a point that is
        // interior to the undistorted and rectified image boundary. With almost any conceivable
        // amount of distortion and rectification, the point at the intersection of the lines
        // connecting opposite corners of the distorted image should fall within the interior.

        // Get the four corner points
        double x[] = new double[4];
        double y[] = new double[4];
        for (int i = 0; i < 4; i++) {
            x[i] = undistortedPoints.get(i * numberOfPointsPerSide, 0)[0];
            y[i] = undistortedPoints.get(i * numberOfPointsPerSide, 0)[1];
        }

        // Compute the intersection point of the lines connecting opposite corners and use that as
        // the starting center point
        double denom = (x[1] - x[3]) * y[0] - x[0] * (y[1] - y[3]) + x[2] * (y[1] - y[3])
                - (x[1] - x[3]) * y[2];
        double innerCenterX = ((x[1] - x[3]) * x[2] * y[0] - x[0] * (x[1] - x[3]) * y[2]
                - (x[3] * y[1] - x[1] * y[3]) * x[0] + (x[3] * y[1] - x[1] * y[3]) * x[2]) / denom;
        double innerCenterY = ((x[2] * (y[1] - y[3]) - x[3] * y[1] + x[1] * y[3]) * y[0]
                - (x[0] * (y[1] - y[3]) - x[3] * y[1] + x[1] * y[3]) * y[2]) / denom;

        // This is the angle from the center of the rectangle to one of its lower right corner
        double angle = Math.atan2(size.height, size.width);

        boolean fullyConstrained = false;
        double maxWidth = 0;
        double maxHeight = 0;

        // Iterate until the rectangle is fully constrained (can't grow any larger)
        while (!fullyConstrained) {
            innerMinX = Double.NEGATIVE_INFINITY;
            innerMaxX = Double.POSITIVE_INFINITY;
            innerMinY = Double.NEGATIVE_INFINITY;
            innerMaxY = Double.POSITIVE_INFINITY;

            // Find the constraining point(s) of the rectangle. Note that all points need to be
            // checked (not just the corners) because a side of the rectangle may be tangent to the
            // boundary at some point between the corners. To do that, it is necessary to determine
            // which side of the rectangle each point may constrain by checking the angle from the
            // center of the rectangle to the point.
            for (int i = 0; i < undistortedPoints.rows(); i++) {
                double[] pt = undistortedPoints.get(i, 0);

                // Angle from the center of the rectangle to the point (remember in this context
                // angles are measured clock-wise since positive X is right and positive Y is down)
                double ptAngle = Math.atan2(pt[1] - innerCenterY, pt[0] - innerCenterX);

                // Make ptAngle in the range [-angle, 2*PI-angle)
                if (ptAngle < -angle) {
                    ptAngle += 2 * Math.PI;
                }

                if ((ptAngle >= -angle) && (ptAngle < angle)) {
                    // right side
                    if (pt[0] < innerMaxX) {
                        innerMaxX = pt[0];
                    }
                }
                else if ((ptAngle >= angle) && (ptAngle < Math.PI - angle)) {
                    // bottom side
                    if (pt[1] < innerMaxY) {
                        innerMaxY = pt[1];
                    }
                }
                else if ((ptAngle >= Math.PI - angle) && (ptAngle < Math.PI + angle)) {
                    // left side
                    if (pt[0] > innerMinX) {
                        innerMinX = pt[0];
                    }
                }
                else { // if ((ptAngle >= Math.PI + angle) && (ptAngle < 2*Math.PI - angle)) {
                       // top side
                    if (pt[1] > innerMinY) {
                        innerMinY = pt[1];
                    }
                }
            }

            // At this point, if any of the limits are not finite, the assumption that the starting
            // point was on the interior of the undistorted and rectified image boundary was
            // invalid.
            // In that case, just force the limits to valid values.
            if (!Double.isFinite(innerMaxX) || !Double.isFinite(innerMinX)
                    || !Double.isFinite(innerMaxY) || !Double.isFinite(innerMinY)) {
                innerMaxX = innerCenterX + 1;
                innerMinX = innerCenterX - 1;
                innerMaxY = innerCenterY + 1;
                innerMinY = innerCenterY - 1;
            }

            // If the principal point was specified, force the limits to be centered on that point
            if (keepPrincipalPoint) {
                double inner = Math.min(innerMaxX - centerX, centerX - innerMinX);
                innerMaxX = centerX + inner;
                innerMinX = centerX - inner;
                inner = Math.min(innerMaxY - centerY, centerY - innerMinY);
                innerMaxY = centerY + inner;
                innerMinY = centerY - inner;
            }

            // Compute the offsets from the current rectangle center to the limit centers
            double innerCenterOffsetX = (innerMaxX + innerMinX) / 2 - innerCenterX;
            double innerCenterOffsetY = (innerMaxY + innerMinY) / 2 - innerCenterY;

            double newHeight;
            double newWidth;

            // Make adjustments to the limits based on the aspect ratio
            if ((innerMaxY - innerMinY) / (innerMaxX - innerMinX) >= aspectRatio) {
                // Constrained by the width, can only move the rectangle up/down so center it
                // between
                // the Y limits
                newWidth = innerMaxX - innerMinX;
                newHeight = newWidth * aspectRatio;
                innerMaxY = (innerMaxY + innerMinY) / 2 + newHeight / 2;
                innerMinY = (innerMaxY + innerMinY) / 2 - newHeight / 2;
            }
            else {
                // Constrained by the height, can only move the rectangle left/right so center it
                // between the X limits
                newHeight = innerMaxY - innerMinY;
                newWidth = newHeight / aspectRatio;
                innerMaxX = (innerMaxX + innerMinX) / 2 + newWidth / 2;
                innerMinX = (innerMaxX + innerMinX) / 2 - newWidth / 2;
            }

            // Move the center for the next possible iteration
            innerCenterX += innerCenterOffsetX;
            innerCenterY += innerCenterOffsetY;

            // Check to see if the rectangle is fully constrained
            fullyConstrained = (Math.abs(innerCenterOffsetX) < 0.0001)
                    && (Math.abs(innerCenterOffsetY) < 0.0001);
            if (newWidth > maxWidth) {
                maxWidth = newWidth;
                fullyConstrained = false;
            }
            if (newHeight > maxHeight) {
                maxHeight = newHeight;
                fullyConstrained = false;
            }
        }
        undistortedPoints.release();

        double innerF;

        // Compute the scaling from normalized coordinates to pixels (this is the f terms in the
        // intrinsic camera matrix)
        if ((innerMaxY - innerMinY) / (innerMaxX - innerMinX) >= aspectRatio) {
            // Constrained by the width
            innerF = size.width / (innerMaxX - innerMinX);
        }
        else {
            // Constrained by the height
            innerF = size.height / (innerMaxY - innerMinY);
        }

        // Compute the location of the normalized camera coordinate (0, 0, 1) in pixels relative to
        // the upper left corner of the image (these are the c terms in the intrinsic camera matrix)
        double innerCx = (size.width - 1) / 2 - innerF * innerCenterX;
        double innerCy = (size.height - 1) / 2 - innerF * innerCenterY;

        // Interpolate to the desired alpha value
        double f = outerF * alpha + innerF * (1 - alpha);
        double cx = outerCx * alpha + innerCx * (1 - alpha);
        double cy = outerCy * alpha + innerCy * (1 - alpha);
        
        // Populate the virtual camera matrix
        Mat ret = Mat.eye(3, 3, CvType.CV_64FC1); //needs a 1 in the lower right corner
        ret.put(0, 0, f);
        ret.put(0, 2, cx);
        ret.put(1, 1, f);
        ret.put(1, 2, cy);

        return ret;
    }

    /**
     * Computes the 2D error between the actual image points and the modeled image points
     * 
     * @param actual2DPoints - the actual image points
     * @param modeled2DPoints - the modeled image points
     * @return the 2D residual errors
     */
    public static List<double[]> computeResidualErrors(double[][][] actual2DPoints,
            double[][][] modeled2DPoints) {
        return computeResidualErrors(actual2DPoints, modeled2DPoints, null, null);
    }

    /**
     * Computes the 2D error between the actual image points and the modeled image points
     * 
     * @param actual2DPoints - the actual image points
     * @param modeled2DPoints - the modeled image points
     * @param outlierList - a list of outlier points to exclude, set to null to not exclude any 
     * points
     * @return the 2D residual errors
     */
    public static List<double[]> computeResidualErrors(double[][][] actual2DPoints,
            double[][][] modeled2DPoints, ArrayList<Integer> outlierList) {
        return computeResidualErrors(actual2DPoints, modeled2DPoints, null, outlierList);
    }

    /**
     * Computes the 2D error between the actual image points and the modeled image points
     * 
     * @param actual2DPoints - the actual image points
     * @param modeled2DPoints - the modeled image points
     * @param heightIndex - index to select which calibration height to compute the residual 
     * errors, set to null to compute over all heights
     * @return the 2D residual errors
     */
    public static List<double[]> computeResidualErrors(double[][][] actual2DPoints,
            double[][][] modeled2DPoints, Integer heightIndex) {
        return computeResidualErrors(actual2DPoints, modeled2DPoints, heightIndex, null);
    }

    /**
     * Computes the 2D error between the actual image points and the modeled image points
     * 
     * @param actual2DPoints - the actual image points
     * @param modeled2DPoints - the modeled image points
     * @param heightIndex - index to select which calibration height to compute the residual 
     * errors, set to null to compute over all heights
     * @param outlierList - a list of outlier points to exclude, set to null to not exclude any 
     * points
     * @return the 2D residual errors
     */
    public static List<double[]> computeResidualErrors(double[][][] actual2DPoints,
            double[][][] modeled2DPoints, Integer heightIndex, ArrayList<Integer> outlierList) {
        if (outlierList == null) {
            outlierList = new ArrayList<Integer>();
        }
        List<double[]> retList = new ArrayList<>();
        int iPoint = 0;
        for (int i = 0; i < actual2DPoints.length; i++) {
            for (int j = 0; j < actual2DPoints[i].length; j++) {
                if ((heightIndex == null || heightIndex == i) && !outlierList.contains(iPoint)) {
                    double[] error = new double[2];
                    for (int k = 0; k < 2; k++) {
                        error[k] = actual2DPoints[i][j][k] - modeled2DPoints[i][j][k];
                    }
                    retList.add(error);
                }
                iPoint++;
            }
        }
        return retList;
    }

    /**
     * Computes the Distance Root-Mean-Squared (DRMS) error for a given set of 2D errors
     *  
     * @param residuals - the errors
     * @return the DRMS value of the errors
     */
    public static double computeDrmsError(List<double[]> residuals) {
        double sumOfSquares = 0;
        for (double[] residual : residuals) {
            sumOfSquares += residual[0] * residual[0] + residual[1] * residual[1];
        }
        return Math.sqrt(sumOfSquares / residuals.size());
    }

    /**
     * Generates a heat map of the residual errors between the actual image points and the modeled 
     * image points. The image is scaled so that darkest red indicates the areas of highest error 
     * and darkest blue zero error.
     * 
     * @param size - the image size
     * @param testPatternIndex - selects which calibration height for which the image is generated
     * @param actual2DPoints - the actual 2D image points
     * @param modeled2DPoints - the modeled 2D image points
     * @param outlierList - a list of outlier points to exclude from the image, set to null to not
     * exclude any points
     * @return the heat map of errors
     */
    public static Mat generateErrorImage(Size size, int testPatternIndex,
            double[][][] actual2DPoints, double[][][] modeled2DPoints,
            ArrayList<Integer> outlierList) {
        if (outlierList == null) {
            outlierList = new ArrayList<Integer>();
        }
        
        //Subdivide a rectangular area based on the actual 2D image points and keep track of the 
        //magnitude of the residual error associated with each point
        Rect rect = new Rect(new Point(0, 0), size);
        Subdiv2D subdiv2D = new Subdiv2D(rect);
        int iPoint = 0;
        double maxError = 0;
        Map<Integer, Double> idToErrorMap = new HashMap<Integer, Double>();
        List<double[]> residualList = computeResidualErrors(actual2DPoints, modeled2DPoints);
        for (int i = 0; i < actual2DPoints.length; i++) {
            for (int j = 0; j < actual2DPoints[i].length; j++) {
                if ((i == testPatternIndex) && !outlierList.contains(iPoint)) {
                    double[] errXY = residualList.get(iPoint);
                    double error = Math.hypot(errXY[0], errXY[1]);
                    maxError = Math.max(maxError, error);
                    try {
                        int id = subdiv2D.insert(
                                new Point(actual2DPoints[i][j][0], actual2DPoints[i][j][1]));
                        idToErrorMap.put(id, error);
                    }
                    catch (Exception e) {
                        //ok
                    }
                }
                iPoint++;
            }
        }

        //Find the Voronoi facet associated with each point and shade it based on the magnitude of 
        //the residual error for that point 
        Set<Integer> keys = idToErrorMap.keySet();
        Mat grayImage = new Mat((int) size.height, (int) size.width, CvType.CV_8UC1);
        for (int key : keys) {
            MatOfInt id = new MatOfInt(key);
            List<MatOfPoint2f> facetList = new ArrayList<>();
            MatOfPoint2f facetCenters = new MatOfPoint2f();
            subdiv2D.getVoronoiFacetList(id, facetList, facetCenters);
            id.release();
            facetCenters.release();

            MatOfPoint facetVertices = new MatOfPoint();
            for (MatOfPoint2f p : facetList) {
                for (int i = 0; i < p.rows(); i++) {
                    facetVertices.push_back(
                            new MatOfPoint(new Point((int) p.get(i, 0)[0], (int) p.get(i, 0)[1])));
                }
                p.release();
            }
            Imgproc.fillConvexPoly(grayImage, facetVertices,
                    new Scalar(255.0 * idToErrorMap.get(key) / maxError));
            facetVertices.release();
        }

        //Smooth the image 
        double kernelSize =
                1.3 * Math.sqrt(size.height * size.width / actual2DPoints[testPatternIndex].length);
        Imgproc.blur(grayImage, grayImage, new Size(kernelSize, kernelSize));
        
        //Apply the TURBO color mapping - 0 : Dark Blue ... 255 : Dark Red
        Mat colorImage = new Mat();
        Imgproc.applyColorMap(grayImage, colorImage, Imgproc.COLORMAP_TURBO);
        grayImage.release();
        
        return colorImage;
    }
}
