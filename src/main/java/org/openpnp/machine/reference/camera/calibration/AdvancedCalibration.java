/*
 * Copyright (C) 2021 Tony Luken <tonyluken62+openpnp@gmail.com>
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
import java.util.Arrays;
import java.util.List;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.vision.pipeline.CvPipeline;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementArray;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persist;

public class AdvancedCalibration extends LensCalibrationParams {
    @Attribute(required = false)
    private boolean overridingOldTransformsAndDistortionCorrectionSettings = false;
    
    @Attribute(required = false)
    private Boolean valid = false;
    
    @Attribute(required = false)
    private Boolean dataAvailable = false;
    
    @Element(required = false)
    private CvPipeline pipeline = new CvPipeline(
            "<cv-pipeline>" +
                "<stages>" +
                    "<cv-stage class=\"org.openpnp.vision.pipeline.stages.ImageCapture\" name=\"image\" enabled=\"true\" default-light=\"true\" settle-first=\"true\" count=\"3\"/>" +
                    "<cv-stage class=\"org.openpnp.vision.pipeline.stages.DetectCircularSymmetry\" name=\"detect_circle\" enabled=\"true\" min-diameter=\"18\" max-diameter=\"25\" max-distance=\"100\" search-width=\"0\" search-height=\"0\" max-target-count=\"1\" min-symmetry=\"1.2\" corr-symmetry=\"0.0\" property-name=\"DetectCircularSymmetry\" outer-margin=\"0.1\" inner-margin=\"0.1\" sub-sampling=\"8\" super-sampling=\"8\" diagnostics=\"false\" heat-map=\"false\"/>" +
                    "<cv-stage class=\"org.openpnp.vision.pipeline.stages.ConvertModelToKeyPoints\" name=\"results\" enabled=\"true\" model-stage-name=\"detect_circle\"/>" +
                "</stages>" +
             "</cv-pipeline>");

    @Element(name = "virtualCameraMatrix", required = false)
    private double[] virtualCameraMatrixArr = new double[9];

    @Element(name = "rectificationMatrix", required = false)
    private double[] rectificationMatrixArr = new double[9];

    @Element(name = "vectorFromMachToPhyCamInMachRefFrame", required = false)
    private double[] vectorFromMachToPhyCamInMachRefFrameArr = new double[3];

    @Element(name = "unitVectorPhyCamZInMachRefFrame", required = false)
    private double[] unitVectorPhyCamZInMachRefFrameArr = new double[3];

    @Element(name = "vectorFromMachToVirCamInMachRefFrame", required = false)
    private double[] vectorFromMachToVirCamInMachRefFrameArr = new double[3];

    @Element(name = "vectorFromPhyCamToDesiredPrincipalPointInPhyCamRefFrame", required = false)
    private double[] vectorFromPhyCamToDesiredPrincipalPointInPhyCamRefFrameArr = new double[3];
    
    @Attribute(required = false) int alphaPercent = 100;
    
    @ElementArray(required = false)
    private double[][][] savedTestPattern3dPointsList = new double[0][0][0];
    
    @ElementArray(required = false)
    private double[][][] savedTestPatternImagePointsList = new double[0][0][0]; 
    
    @ElementArray(required = false)
    private double[][][] modeledTestPatternImagePointsList = new double[0][0][0]; 
    
    @ElementArray(required = false)
    private Integer[] outlierPoints = new Integer[0];
    
    @Attribute(required = false)
    private double rotationErrorZ = 0;

    @Attribute(required = false)
    private double rotationErrorY = 0;

    @Attribute(required = false)
    private double rotationErrorX = 0;

    @Attribute(required = false)
    private double rmsError = 0;
    
    @Attribute(required = false)
    private int desiredRadialLinesPerTestPattern = 32;
    
    @Attribute(required = false)
    private double testPatternFillFraction = 0.90;
    
    @Attribute(required = false)
    private double walkingLoopGain = 0.50;
    
    @Element(required = false)
    private Length trialStep = new Length(0.5, LengthUnit.Millimeters);

    @Element(required = false)
    private Length approximateCameraZ = new Length(0, LengthUnit.Millimeters);
    
    @Attribute(required = false)
    private double approximateMillimetersPerPixel = 0;


    private Mat virtualCameraMatrix = Mat.eye(3, 3, CvType.CV_64FC1);
    private Mat rectificationMatrix = Mat.eye(3, 3, CvType.CV_64FC1);
    private Mat vectorFromMachToPhyCamInMachRefFrame = 
            Mat.zeros(3, 1, CvType.CV_64FC1);
    private Mat unitVectorPhyCamZInMachRefFrame = 
            Mat.zeros(3, 1, CvType.CV_64FC1);
    private Mat vectorFromMachToVirCamInMachRefFrame = 
            Mat.zeros(3, 1, CvType.CV_64FC1);
    private Mat vectorFromPhyCamToDesiredPrincipalPointInPhyCamRefFrame = 
            Mat.zeros(3, 1, CvType.CV_64FC1);

    private ArrayList<Integer> outlierPointList = new ArrayList<Integer>();

    private int fiducialDiameter;
    
    
    @Commit 
    public void commit() {
        super.commit();
        virtualCameraMatrix.put(0, 0, virtualCameraMatrixArr);
        rectificationMatrix.put(0, 0, rectificationMatrixArr);
        vectorFromMachToPhyCamInMachRefFrame.put(0, 0, vectorFromMachToPhyCamInMachRefFrameArr);
        unitVectorPhyCamZInMachRefFrame.put(0, 0, unitVectorPhyCamZInMachRefFrameArr);
        vectorFromMachToVirCamInMachRefFrame.put(0, 0, vectorFromMachToVirCamInMachRefFrameArr);
        vectorFromPhyCamToDesiredPrincipalPointInPhyCamRefFrame.put(0, 0, 
                vectorFromPhyCamToDesiredPrincipalPointInPhyCamRefFrameArr);
        
        //For some reason, the serialization/deserialization process doesn't seem to correctly
        //handle 3D arrays.  If an n x m x p array is serialized and then de-serialized, the
        //array comes back as n x m*p x 1.  Is there a better way to fix this?
        if (savedTestPattern3dPointsList != null && savedTestPattern3dPointsList.length > 0 && 
                savedTestPattern3dPointsList[0].length > 0) {
            if (savedTestPattern3dPointsList[0][0].length == 1) {
                int numberOfPatterns = savedTestPattern3dPointsList.length;
                double[][][] temp = new double[numberOfPatterns][][];
                for (int i=0; i<numberOfPatterns; i++) {
                    int numberOfPoints = savedTestPattern3dPointsList[i].length / 3;
                    temp[i] = new double[numberOfPoints][3];
                    for (int j=0; j<numberOfPoints; j++) {
                        temp[i][j][0] = savedTestPattern3dPointsList[i][3*j][0];
                        temp[i][j][1] = savedTestPattern3dPointsList[i][3*j+1][0];
                        temp[i][j][2] = savedTestPattern3dPointsList[i][3*j+2][0];
                    }
                }
                savedTestPattern3dPointsList = temp;
                dataAvailable = true;
            }
        }

        if (savedTestPatternImagePointsList != null && savedTestPatternImagePointsList.length > 0 && 
                savedTestPatternImagePointsList[0].length > 0) {
            if (savedTestPatternImagePointsList[0][0].length == 1) {
                int numberOfPatterns = savedTestPatternImagePointsList.length;
                double[][][] temp = new double[numberOfPatterns][][];
                for (int i=0; i<numberOfPatterns; i++) {
                    int numberOfPoints = savedTestPatternImagePointsList[i].length / 2;
                    temp[i] = new double[numberOfPoints][2];
                    for (int j=0; j<numberOfPoints; j++) {
                        temp[i][j][0] = savedTestPatternImagePointsList[i][2*j][0];
                        temp[i][j][1] = savedTestPatternImagePointsList[i][2*j+1][0];
                    }
                }
                savedTestPatternImagePointsList = temp;
            }
        }
        
        if (modeledTestPatternImagePointsList != null && modeledTestPatternImagePointsList.length > 0 && 
                modeledTestPatternImagePointsList[0] != null && modeledTestPatternImagePointsList[0].length > 0) {
            if (modeledTestPatternImagePointsList[0][0].length == 1) {
                int numberOfPatterns = modeledTestPatternImagePointsList.length;
                double[][][] temp = new double[numberOfPatterns][][];
                for (int i=0; i<numberOfPatterns; i++) {
                    int numberOfPoints = modeledTestPatternImagePointsList[i].length / 2;
                    temp[i] = new double[numberOfPoints][2];
                    for (int j=0; j<numberOfPoints; j++) {
                        temp[i][j][0] = modeledTestPatternImagePointsList[i][2*j][0];
                        temp[i][j][1] = modeledTestPatternImagePointsList[i][2*j+1][0];
                    }
                }
                modeledTestPatternImagePointsList = temp;
            }
        }
        
        if (outlierPoints != null && outlierPoints.length > 0) {
            outlierPointList = new ArrayList<Integer>(Arrays.asList(outlierPoints));
        }
        else {
            outlierPointList = new ArrayList<Integer>();
        }
    }
    
    @Persist
    public void persist() {
        super.persist();
        virtualCameraMatrix.get(0, 0, virtualCameraMatrixArr);
        rectificationMatrix.get(0, 0, rectificationMatrixArr);
        vectorFromMachToPhyCamInMachRefFrame.get(0, 0, vectorFromMachToPhyCamInMachRefFrameArr);
        unitVectorPhyCamZInMachRefFrame.get(0, 0, unitVectorPhyCamZInMachRefFrameArr);
        vectorFromMachToVirCamInMachRefFrame.get(0, 0, vectorFromMachToVirCamInMachRefFrameArr);
        vectorFromPhyCamToDesiredPrincipalPointInPhyCamRefFrame.get(0, 0, 
                vectorFromPhyCamToDesiredPrincipalPointInPhyCamRefFrameArr);
        
        if (outlierPointList != null) {
            outlierPoints = outlierPointList.toArray(new Integer[0]);
        }
        else {
            outlierPoints = new Integer[0];
        }
    }

    /**
     * Checks if the new advanced calibration settings are overriding the old image
     * transform and lens distortion settings
     * @return true if the new advanced settings are overriding the old settings
     */
    public boolean isOverridingOldTransformsAndDistortionCorrectionSettings() {
        return overridingOldTransformsAndDistortionCorrectionSettings;
    }

    /**
     * Enables the new advanced calibration settings to override the old image transform and 
     * lens distortion settings
     * @param b - set to true to have the new advanced calibration settings override the old 
     * settings
     */
    public void setOverridingOldTransformsAndDistortionCorrectionSettings(boolean b) {
        boolean oldSetting = this.overridingOldTransformsAndDistortionCorrectionSettings;
        this.overridingOldTransformsAndDistortionCorrectionSettings = b;
        firePropertyChange("overridingOldTransformsAndDistortionCorrectionSettings", oldSetting, b);
    }

    /**
     * Checks if the calibration parameters validity flag is set
     * @return true if the current calibration parameters validity flag is set
     */
    public Boolean isValid() {
        return valid;
    }

    /**
     * Sets the calibration parameters validity flag to the specified state
     * @param valid - the state to set the validity flag
     */
    public void setValid(Boolean valid) {
        Boolean oldSetting = this.valid;
        this.valid = valid;
        firePropertyChange("valid", oldSetting, valid);
    }

    /**
     * @return the dataAvailable
     */
    public Boolean isDataAvailable() {
        return dataAvailable;
    }

    /**
     * @param dataAvailable the dataAvailable to set
     */
    public void setDataAvailable(Boolean dataAvailable) {
        Boolean oldSetting = this.dataAvailable;
        this.dataAvailable = dataAvailable;
        firePropertyChange("dataAvailable", oldSetting, dataAvailable);
    }

    /**
     * @return the savedTestPatternImagePointsList
     */
    public double[][][] getSavedTestPatternImagePointsList() {
        return savedTestPatternImagePointsList;
    }

    /**
     * @return the savedTestPattern3dPointsList
     */
    public double[][][] getSavedTestPattern3dPointsList() {
        return savedTestPattern3dPointsList;
    }

    /**
     * @param savedTestPattern3dPointsList the savedTestPattern3dPointsList to set
     */
    public void setSavedTestPattern3dPointsList(double[][][] savedTestPattern3dPointsList) {
        this.savedTestPattern3dPointsList = savedTestPattern3dPointsList;
    }

    /**
     * @param savedTestPatternImagePointsList the savedTestPatternImagePointsList to set
     */
    public void setSavedTestPatternImagePointsList(double[][][] savedTestPatternImagePointsList) {
        this.savedTestPatternImagePointsList = savedTestPatternImagePointsList;
    }

    /**
     * @return the modeledImagePointsList
     */
    public double[][][] getModeledImagePointsList() {
        return modeledTestPatternImagePointsList;
    }

    /**
     * @param modeledImagePointsList the modeledImagePointsList to set
     */
    public void setModeledImagePointsList(double[][][] modeledImagePointsList) {
        this.modeledTestPatternImagePointsList = modeledImagePointsList;
    }

    /**
     * @return the outlierPointList
     */
    public ArrayList<Integer> getOutlierPointList() {
        return outlierPointList; //new ArrayList<Integer>(Arrays.asList(outlierPoints));
    }

    /**
     * @param outlierPointList the outlierPointList to set
     */
    public void setOutlierPoints(ArrayList<Integer> outlierPointList) {
        this.outlierPointList = outlierPointList;
    }

    /**
     * @return the alphaPercent
     */
    public int getAlphaPercent() {
        return alphaPercent;
    }

    /**
     * @param alphaPercent the alphaPercent to set
     */
    public void setAlphaPercent(int alphaPercent) {
        this.alphaPercent = alphaPercent;
    }

    /**
     * @return the pipeline
     */
    public CvPipeline getPipeline() {
        return pipeline;
    }

    /**
     * @param pipeline the pipeline to set
     */
    public void setPipeline(CvPipeline pipeline) {
        this.pipeline = pipeline;
    }

    /**
     * @return the virtual camera matrix
     */
    public Mat getVirtualCameraMatrix() {
        return virtualCameraMatrix;
    }

    /**
     * @param virtualCameraMatrix - the virtual camera matrix to set
     */
    public void setVirtualCameraMatrix(Mat virtualCameraMatrix) {
        this.virtualCameraMatrix = virtualCameraMatrix.clone();
    }

    /**
     * @return the rectification matrix
     */
    public Mat getRectificationMatrix() {
        return rectificationMatrix;
    }

    /**
     * @param rectificationMatrix - the rectification matrix to set
     */
    public void setRectificationMatrix(Mat rectificationMatrix) {
        this.rectificationMatrix = rectificationMatrix.clone();
    }

    /**
     * Gets the vector from the machine origin to the physical camera origin with components
     * expressed in the machine reference frame
     * @return the vector as a 3x1 matrix
     */
    public Mat getVectorFromMachToPhyCamInMachRefFrame() {
        return vectorFromMachToPhyCamInMachRefFrame;
    }

    /**
     * Sets the vector from the machine origin to the physical camera origin with components
     * expressed in the machine reference frame
     * @param vect - the vector as a 3x1 matrix
     */
    public void setVectorFromMachToPhyCamInMachRefFrame(Mat vect) {
        this.vectorFromMachToPhyCamInMachRefFrame = vect;
    }

    /**
     * Gets the unit vector in the direction of the physical camera's Z axis with components
     * expressed in the machine reference frame
     * @return the unit vector as a 3x1 matrix
     */
    public Mat getUnitVectorPhyCamZInMachRefFrame() {
        return unitVectorPhyCamZInMachRefFrame;
    }

    /**
     * Sets the unit vector in the direction of the physical camera's Z axis with components
     * expressed in the machine reference frame
     * @param vect - the vector as a 3x1 matrix
     */
    public void setUnitVectorPhyCamZInMachRefFrame(Mat vect) {
        this.unitVectorPhyCamZInMachRefFrame = vect;
    }

    /**
     * Gets the vector from the machine origin to the virtual camera origin with components
     * expressed in the machine reference frame
     * @return the vector as a 3x1 matrix
     */
    public Mat getVectorFromMachToVirCamInMachRefFrame() {
        return vectorFromMachToVirCamInMachRefFrame;
    }

    /**
     * Sets the vector from the machine origin to the virtual camera origin with components
     * expressed in the machine reference frame
     * @param vect - the vector as a 3x1 matrix
     */
    public void setVectorFromMachToVirCamInMachRefFrame(Mat vect) {
        this.vectorFromMachToVirCamInMachRefFrame = vect;
    }

    /**
     * Gets the vector from the physical camera origin to the desired principal point with 
     * components expressed in the physical camera reference frame
     * @return the vector as a 3x1 matrix
     */
    public Mat getVectorFromPhyCamToDesiredPrincipalPointInPhyCamRefFrame() {
        return vectorFromPhyCamToDesiredPrincipalPointInPhyCamRefFrame;
    }

    /**
     * Sets the vector from the physical camera origin to the desired principal point with 
     * components expressed in the physical camera reference frame
     * @param vect - the vector as a 3x1 matrix
     */
    public void setVectorFromPhyCamToDesiredPrincipalPointInPhyCamRefFrame(Mat vect) {
        this.vectorFromPhyCamToDesiredPrincipalPointInPhyCamRefFrame = vect;
    }

    /**
     * @return the rotationErrorZ
     */
    public double getRotationErrorZ() {
        return rotationErrorZ;
    }

    /**
     * @param rotationErrorZ - the rotationErrorZ to set
     */
    public void setRotationErrorZ(double rotationErrorZ) {
        double oldValue = this.rotationErrorZ;
        this.rotationErrorZ = rotationErrorZ;
        firePropertyChange("rotationErrorZ", oldValue, rotationErrorZ);
    }

    /**
     * @return the rotationErrorY
     */
    public double getRotationErrorY() {
        return rotationErrorY;
    }

    /**
     * @param rotationErrorY - the rotationErrorY to set
     */
    public void setRotationErrorY(double rotationErrorY) {
        double oldValue = this.rotationErrorY;
        this.rotationErrorY = rotationErrorY;
        firePropertyChange("rotationErrorY", oldValue, rotationErrorY);
    }

    /**
     * @return the rotationErrorX
     */
    public double getRotationErrorX() {
        return rotationErrorX;
    }

    /**
     * @param rotationErrorX - the rotationErrorX to set
     */
    public void setRotationErrorX(double rotationErrorX) {
        double oldValue = this.rotationErrorX;
        this.rotationErrorX = rotationErrorX;
        firePropertyChange("rotationErrorX", oldValue, rotationErrorX);
    }

    /**
     * @return the rmsError
     */
    public double getRmsError() {
        return rmsError;
    }

    /**
     * @param rmsError the rmsError to set
     */
    public void setRmsError(double rmsError) {
        double oldValue = this.rmsError;
        this.rmsError = rmsError;
        firePropertyChange("rmsError", oldValue, rmsError);
    }

    /**
     * @return the desiredRadialLinesPerTestPattern
     */
    public int getDesiredRadialLinesPerTestPattern() {
        return desiredRadialLinesPerTestPattern;
    }

    /**
     * @param desiredRadialLinesPerTestPattern the desiredRadialLinesPerTestPattern to set
     */
    public void setDesiredRadialLinesPerTestPattern(int desiredRadialLinesPerTestPattern) {
        this.desiredRadialLinesPerTestPattern = desiredRadialLinesPerTestPattern;
    }

    /**
     * @return the testPatternFillFraction
     */
    public double getTestPatternFillFraction() {
        return testPatternFillFraction;
    }

    /**
     * @param testPatternFillFraction the testPatternFillFraction to set
     */
    public void setTestPatternFillFraction(double testPatternFillFraction) {
        this.testPatternFillFraction = testPatternFillFraction;
    }

    /**
     * @return the walkingLoopGain
     */
    public double getWalkingLoopGain() {
        return walkingLoopGain;
    }

    /**
     * @param walkingLoopGain the walkingLoopGain to set
     */
    public void setWalkingLoopGain(double walkingLoopGain) {
        this.walkingLoopGain = walkingLoopGain;
    }

    /**
     * @return the fiducialDiameter
     */
    public int getFiducialDiameter() {
        return fiducialDiameter;
    }

    /**
     * @param fiducialDiameter the fiducialDiameter to set
     */
    public void setFiducialDiameter(int fiducialDiameter) {
        int oldSetting = this.fiducialDiameter;
        this.fiducialDiameter = fiducialDiameter;
        firePropertyChange("fiducialDiameter", oldSetting, fiducialDiameter);
    }

    public double getApproximateMillimetersPerPixel() {
        return approximateMillimetersPerPixel;
    }

    public void setApproximateMillimetersPerPixel(double approximateMillimetersPerPixel) {
        this.approximateMillimetersPerPixel = approximateMillimetersPerPixel;
    }

    /**
     * @return the trialStep
     */
    public Length getTrialStep() {
        return trialStep;
    }

    /**
     * @param trialStep the trialStep to set
     */
    public void setTrialStep(Length trialStep) {
        this.trialStep = trialStep;
    }

    /**
     * @return the approximateCameraZ
     */
    public Length getApproximateCameraZ() {
        return approximateCameraZ;
    }

    /**
     * @param approximateCameraZ the approximateCameraZ to set
     */
    public void setApproximateCameraZ(Length approximateCameraZ) {
        this.approximateCameraZ = approximateCameraZ;
    }

    public void processRawCalibrationData(Size size) throws Exception {
        processRawCalibrationData(savedTestPattern3dPointsList, 
                savedTestPatternImagePointsList, size);
    }
    
    /**
     * Processes the raw calibration data to set the advanced calibration parameters
     * 
     * @param testPattern3dPoints - a numberOfTestPatterns x numberOfPointsPerTestPattern x 3 array
     * containing the 3D machine coordinates at which the corresponding point in 
     * testPatternImagePoints were collected
     * @param testPatternImagePoints - a numberOfTestPatterns x numberOfPointsPerTestPattern x 2 
     * array containing the 2D image coordinates corresponding to the points in testPattern3dPoints
     * @param size - the size of the image
     * @throws Exception
     */
    public void processRawCalibrationData(double[][][] testPattern3dPoints, 
            double[][][] testPatternImagePoints, Size size) throws Exception {
        
        double primaryZ = testPattern3dPoints[0][0][2];
        
        savedTestPattern3dPointsList = testPattern3dPoints;
        savedTestPatternImagePointsList = testPatternImagePoints;

        int numberOfTestPatterns = Math.min(testPattern3dPoints.length, 
                testPatternImagePoints.length);
        
        //Create lists of Mats to pass to Calib3d.calibrateCamera
        //Note that the Z component of all the test patterns is set to zero so the camera
        //will appear as if it is changing height rather than the patterns.  That will be
        //corrected for when the tvecs are processed later.
        List<Mat> testPattern3dPointsList = new ArrayList<>();
        List<Mat> testPatternImagePointsList = new ArrayList<>();
        for (int iTestPattern=0; iTestPattern<numberOfTestPatterns; iTestPattern++) {
            double[][] tpArray = testPattern3dPoints[iTestPattern];
            Mat tp3dMat = new Mat();
            for (int ptIdx=0; ptIdx<tpArray.length; ptIdx++) {
                Point3 pt3 = new Point3(tpArray[ptIdx]);
                pt3.z = 0; //set Z coordinate to zero
                tp3dMat.push_back(new MatOfPoint3f(pt3));
            }
            testPattern3dPointsList.add(tp3dMat);
            
            tpArray = testPatternImagePoints[iTestPattern];
            Mat tp2dMat = new Mat();
            for (int ptIdx=0; ptIdx<tpArray.length; ptIdx++) {
                tp2dMat.push_back(new MatOfPoint2f(new Point(tpArray[ptIdx])));
            }
            testPatternImagePointsList.add(tp2dMat);
        }

        List<Mat> rvecs = new ArrayList<>();
        List<Mat> tvecs = new ArrayList<>();
        
        double approximateF = Math.abs(approximateCameraZ.
                convertToUnits(LengthUnit.Millimeters).getValue() - primaryZ) / approximateMillimetersPerPixel;
        cameraMatrix.release();
        cameraMatrix = Mat.eye(3, 3, CvType.CV_64FC1);
        cameraMatrix.put(0, 0, approximateF);
        cameraMatrix.put(1, 1, approximateF);
        cameraMatrix.put(0, 2, (size.width - 1.0)/2.0);
        cameraMatrix.put(1, 2, (size.height - 1.0)/2.0);
        Logger.trace("cameraMatrix = " + cameraMatrix.dump());
        
        distortionCoefficients.release();
        distortionCoefficients = Mat.zeros(5, 1, CvType.CV_64FC1);
        
        //OpenCV's Calib3d.calibrateCamera is used to get an initial estimate of the intrinsic 
        //and extrinsic camera parameters.  Note that the camera model used by 
        //Calib3d.calibrateCamera is quite general and has more degrees of freedom than is 
        //necessary for modeling openpnp cameras.  For instance, it assumes the camera can
        //be rotated and translated differently relative to each test pattern.  But in actuality, 
        //for openpnp, the camera has a fixed rotation relative to all test patterns and the 
        //camera Z coordinate (wrt to the machine) is fixed during the collection of all test 
        //patterns.  Because it has more degrees of freedom than necessary, the RMS error of the
        //model fit may appear good but parameter estimates may in fact be significantly in
        //error.
        double rms = Calib3d.calibrateCamera(testPattern3dPointsList, 
                testPatternImagePointsList, size,
                cameraMatrix, distortionCoefficients, rvecs, tvecs, 
                Calib3d.CALIB_FIX_PRINCIPAL_POINT | Calib3d.CALIB_USE_INTRINSIC_GUESS );
        Logger.trace("Calib3d.calibrateCamera rms = " + rms);
        
        for (Mat tp : testPattern3dPointsList) {
            tp.release();
        }
        for (Mat tp : testPatternImagePointsList) {
            tp.release();
        }
        
        Logger.trace("cameraMatrix = " + cameraMatrix.dump());
        Logger.trace("distortionCoefficients = " + distortionCoefficients.dump());

        //Setup and populate an array of camera parameters with an initial guess based on the
        //solution obtained by Calib3d.calibrateCamera
        double[] cameraParams = new double[13+2*numberOfTestPatterns];
        cameraParams[0] = cameraMatrix.get(0, 0)[0];
        cameraParams[1] = cameraMatrix.get(1, 1)[0];
        cameraParams[2] = cameraMatrix.get(0, 2)[0];
        cameraParams[3] = cameraMatrix.get(1, 2)[0];
        cameraParams[4] = distortionCoefficients.get(0, 0)[0];
        cameraParams[5] = distortionCoefficients.get(1, 0)[0];
        cameraParams[6] = distortionCoefficients.get(2, 0)[0];
        cameraParams[7] = distortionCoefficients.get(3, 0)[0];
        cameraParams[8] = distortionCoefficients.get(4, 0)[0];

        //tvecs contains the vector from the camera origin to the test pattern origin with 
        //components expressed in the camera reference system.  Since the Z coordinate
        //of the test patterns were all set to zero before being passed to calibrateCamera, the
        //true Z coordinate of the test patterns must be accounted for here when computing the
        //location of the camera
        double weight = 1.0/tvecs.size();
        int iTestPattern = 0;
        Mat transformFromMachToPhyCamRefFrame = Mat.eye(3, 3, CvType.CV_64FC1);
        Mat sumTransformFromMachToPhyCamRefFrame = Mat.eye(3, 3, CvType.CV_64FC1);
        for (Mat vectorFromPhyCamToTestPattInPhyCamRefFrame : tvecs) {
            Logger.trace("vectorFromPhyCamToTestPattInPhyCamRefFrame = " + 
                    vectorFromPhyCamToTestPattInPhyCamRefFrame.dump());
            
            Mat vectorFromMachToTestPattInMachRefFrame = Mat.zeros(3, 1,
                    CvType.CV_64FC1);
            vectorFromMachToTestPattInMachRefFrame.put(2, 0, 
                    testPattern3dPoints[iTestPattern][0][2]);
            Logger.trace("vectorFromMachToTestPattInMachRefFrame = " + 
                    vectorFromMachToTestPattInMachRefFrame.dump());
            
            //Convert the applicable rotation vector to a rotation matrix
            Calib3d.Rodrigues(rvecs.get(iTestPattern), 
                    transformFromMachToPhyCamRefFrame);
            rvecs.get(iTestPattern).release();
            Logger.trace("transformFromMachToPhyCamRefFrame = " + 
                    transformFromMachToPhyCamRefFrame.dump());

            Core.add(transformFromMachToPhyCamRefFrame, sumTransformFromMachToPhyCamRefFrame, 
                    sumTransformFromMachToPhyCamRefFrame);
            
            Mat tempVectorFromMachToPhyCamInMachRefFrame = Mat.zeros(3, 1, 
                    CvType.CV_64FC1);
            Core.gemm(transformFromMachToPhyCamRefFrame.t(), 
                    vectorFromPhyCamToTestPattInPhyCamRefFrame, -1, 
                    vectorFromMachToTestPattInMachRefFrame, 1, 
                    tempVectorFromMachToPhyCamInMachRefFrame);
            transformFromMachToPhyCamRefFrame.release();
            vectorFromPhyCamToTestPattInPhyCamRefFrame.release();
            vectorFromMachToTestPattInMachRefFrame.release();
            Logger.trace("tempVectorFromMachToPhyCamInMachRefFrame = " + 
                    tempVectorFromMachToPhyCamInMachRefFrame.dump());
            
            //Ideally, all the tempVectorFromMachToPhyCamInMachRefFrame vectors should have the 
            //same Z component but again due to the extra degrees of freedom discussed above, 
            //they may be different so the average of their Z components is used to estimate the
            //true Z component
            cameraParams[12] += weight*
                    tempVectorFromMachToPhyCamInMachRefFrame.get(2, 0)[0]; //Z
            
            //The X and Y components can legitimately be different due to either accidental 
            //horizontal offsets introduced when the calibration rig was changed in height or if 
            //the different calibration fiducials were truly at different locations or, for bottom 
            //cameras, if the nozzle Z axis is not exactly perpendicular to the X-Y plane.
            cameraParams[13 + 2*iTestPattern] = 
                    tempVectorFromMachToPhyCamInMachRefFrame.get(0, 0)[0]; //X
            cameraParams[14 + 2*iTestPattern] = 
                    tempVectorFromMachToPhyCamInMachRefFrame.get(1, 0)[0]; //Y
            tempVectorFromMachToPhyCamInMachRefFrame.release();
            
            iTestPattern++;
        }
        
        //For openpnp, since the physical camera can't rotate relative to the machine axis, all
        //of the rotation vectors should be exactly the same.  But due to the extra degrees of 
        //freedom discussed above, they may not be exactly the same so we need to average them to 
        //get an estimate of the one rotation vector.  However, we can't just average the rotation
        //vectors (has similar problems akin to averaging angles close to +/-180 degrees) so we 
        //convert to rotation matrices, sum them, and then use the Kabsch algorithm to find the 
        //average rotation matrix and then convert back to a rotation vector.
        Mat sDiag = new Mat();
        Mat u = new Mat();
        Mat vt = new Mat();
        Core.SVDecomp(sumTransformFromMachToPhyCamRefFrame, sDiag, u, vt);
        sumTransformFromMachToPhyCamRefFrame.release();
        transformFromMachToPhyCamRefFrame = new Mat();
        Core.gemm(u, vt, 1, vt, 0, transformFromMachToPhyCamRefFrame);
        if (Core.determinant(transformFromMachToPhyCamRefFrame) < 0) {
            Mat s = Mat.eye(3, 3, CvType.CV_64FC1);
            s.put(2, 2, -1);
            Core.gemm(u, s, 1, s, 0, s);
            Core.gemm(s, vt, 1, s, 0, transformFromMachToPhyCamRefFrame);
            s.release();
        }
        Logger.trace("transformFromMachToPhyCamRefFrame = " + 
                transformFromMachToPhyCamRefFrame.dump());
        sDiag.release();
        u.release();
        vt.release();
        
        Mat rvec = new Mat();
        Calib3d.Rodrigues(transformFromMachToPhyCamRefFrame, rvec );
        transformFromMachToPhyCamRefFrame.release();
        cameraParams[9] = rvec.get(0, 0)[0];
        cameraParams[10] = rvec.get(1, 0)[0];
        cameraParams[11] = rvec.get(2, 0)[0];
        rvec.release();

        //Compute a new set of camera parameters based on openpnp's more restrictive camera
        //model that avoids the excess degree of freedom problem discussed above
        modeledTestPatternImagePointsList = new double[numberOfTestPatterns][][];
        rms = CameraCalibrationUtils.computeBestCameraParameters(
                testPattern3dPoints, testPatternImagePoints, modeledTestPatternImagePointsList, 
                outlierPointList, cameraParams, CameraCalibrationUtils.FIX_PRINCIPAL_POINT);
        setRmsError(rms);
        
        //Use the new estimates for the physical camera's matrix
        cameraMatrix.put(0, 0, cameraParams[0]);
        cameraMatrix.put(1, 1, cameraParams[1]);
        cameraMatrix.put(0, 2, cameraParams[2]);
        cameraMatrix.put(1, 2, cameraParams[3]);
        Logger.trace("cameraMatrix = " + cameraMatrix.dump());
        
        //Use the new estimates for the physical camera's lens distortion coefficients
        distortionCoefficients.put(0, 0, cameraParams[4]);
        distortionCoefficients.put(1, 0, cameraParams[5]);
        distortionCoefficients.put(2, 0, cameraParams[6]);
        distortionCoefficients.put(3, 0, cameraParams[7]);
        distortionCoefficients.put(4, 0, cameraParams[8]);
        Logger.trace("distortionCoefficients = " + distortionCoefficients.dump());
        
        //Convert the new estimate of the rotation vector to a rotation matrix
        rvec = Mat.zeros(3, 1, CvType.CV_64FC1);
        rvec.put(0,  0, cameraParams[9]);
        rvec.put(1,  0, cameraParams[10]);
        rvec.put(2,  0, cameraParams[11]);
        Calib3d.Rodrigues(rvec, transformFromMachToPhyCamRefFrame);
        rvec.release();
        Logger.trace("transformFromMachToPhyCamRefFrame = " + 
                transformFromMachToPhyCamRefFrame.dump());
        
        //Compute the physical camera's rotational errors WRT the machine axis (ignoring any 
        //multiples of 90 degrees)
        Mat rot90s = Mat.zeros(3, 3, CvType.CV_64FC1);
        Mat rotErrors = Mat.zeros(3, 3, CvType.CV_64FC1);
        for (int i=0; i<3; i++) {
            double largestMag = 0;
            int largestMagIdx = 0;
            for (int j=0; j<3; j++) {
                if (Math.abs(transformFromMachToPhyCamRefFrame.get(i, j)[0]) > largestMag) {
                    largestMag = Math.abs(transformFromMachToPhyCamRefFrame.get(i, j)[0]);
                    largestMagIdx = j;
                }
            }
            rot90s.put(i, largestMagIdx, Math.signum(transformFromMachToPhyCamRefFrame.
                    get(i, largestMagIdx)[0]));
        }
        Logger.trace("rot90s = " + rot90s.dump());
        Core.gemm(rot90s.t(), transformFromMachToPhyCamRefFrame, 1.0, rot90s, 0, rotErrors );
        Logger.trace("rotErrors = " + rotErrors.dump());
        rot90s.release();

        //Convert from rotation matrix to angles
        setRotationErrorZ(Math.toDegrees(Math.atan2(rotErrors.get(0, 1)[0], 
                rotErrors.get(0, 0)[0])));
        setRotationErrorY(Math.toDegrees(Math.asin(-rotErrors.get(0, 2)[0])));
        setRotationErrorX(Math.toDegrees(Math.atan2(rotErrors.get(1, 2)[0], 
                rotErrors.get(2, 2)[0])));
        rotErrors.release();
        
        Logger.trace("Physical camera rotational errors from ideal (as measured by the right "
                + "hand rule about each machine axis):");
        Logger.trace("Z axis rotational error = {} degrees", rotationErrorZ);
        Logger.trace("Y axis rotational error = {} degrees", rotationErrorY);
        Logger.trace("X axis rotational error = {} degrees", rotationErrorX);
        
        //Fit a least-squared-error line to the camera positions as a function of Z and use it
        //to linearly interpolate the camera X/Y position to that at primary Z.  Note that for
        //bottom cameras, the top row of matrix linearFit is a measure of the non-orthogonality
        //of the nozzle Z axis WRT to the X-Y plane.
        Mat x = Mat.ones(numberOfTestPatterns, 2, CvType.CV_64FC1); //need ones in the second column
        Mat b = Mat.zeros(numberOfTestPatterns, 2, CvType.CV_64FC1);
        for (int i=0; i<numberOfTestPatterns; i++) {
            x.put(i, 0, testPattern3dPoints[i][0][2]);
            b.put(i, 0, cameraParams[13+2*i]);
            b.put(i, 1, cameraParams[14+2*i]);
        }
        //linearFit: top row slopes, bottom row intercepts,
        //left column x fit, right column y fit
        Mat linearFit = Mat.zeros(2, 2, CvType.CV_64FC1);
        //solve x*linearFit = b for matrix linearFit
        Core.solve(x, b, linearFit, Core.DECOMP_SVD);  
        Logger.trace("linearFit = " + linearFit.dump());
        x.release();
        b.release();
        //Use the linear fit to interpolate/extrapolate to primary Z
        Mat z = Mat.ones(1, 2, CvType.CV_64FC1); //need ones in the second column
        z.put(0, 0, primaryZ);
        Mat xy = Mat.zeros(1, 2, CvType.CV_64FC1);
        Core.gemm(z, linearFit, 1, z, 0, xy);
        linearFit.release();
        z.release();
        vectorFromMachToPhyCamInMachRefFrame.put(0, 0, xy.get(0, 0)[0]);  //X
        vectorFromMachToPhyCamInMachRefFrame.put(1, 0, xy.get(0, 1)[0]);  //Y
        vectorFromMachToPhyCamInMachRefFrame.put(2, 0, cameraParams[12]); //Z
        Logger.trace("vectorFromMachToPhyCamInMachRefFrame = " + 
                vectorFromMachToPhyCamInMachRefFrame.dump());
        xy.release();
        
        //Create a unit Z vector
        Mat unitZ = Mat.zeros(3, 1, CvType.CV_64FC1);
        unitZ.put(2, 0, 1);
        Logger.trace("unitZ = " + unitZ.dump());
        
        unitVectorPhyCamZInMachRefFrame.release();
        unitVectorPhyCamZInMachRefFrame = Mat.zeros(3, 1, CvType.CV_64FC1);
        Core.gemm(transformFromMachToPhyCamRefFrame.t(), unitZ, 1, unitZ, 0, 
                unitVectorPhyCamZInMachRefFrame);
        Logger.trace("unitVectorPhyCamZInMachRefFrame = " + 
                unitVectorPhyCamZInMachRefFrame.dump());
        
        Mat unitVectorMachZInPhyCamRefFrame = Mat.zeros(3, 1, CvType.CV_64FC1);
        Core.gemm(transformFromMachToPhyCamRefFrame, unitZ, 1, unitZ, 0, 
                unitVectorMachZInPhyCamRefFrame);
        unitZ.release();
        Logger.trace("unitVectorMachZInPhyCamRefFrame = " + 
                unitVectorMachZInPhyCamRefFrame.dump());
        
        //Compute the Z offset from the camera to the primary Z plane
        double cameraToZPlane = primaryZ - vectorFromMachToPhyCamInMachRefFrame.get(2, 0)[0];
        Logger.trace("cameraToZPlane = " + cameraToZPlane);
        
        Mat vectorFromPhyCamToPrimaryZPlaneInPhyCamRefFrame = Mat.zeros(3, 1, CvType.CV_64FC1);
        Core.multiply(unitVectorMachZInPhyCamRefFrame, 
                new Scalar(cameraToZPlane/unitVectorMachZInPhyCamRefFrame.get(2, 0)[0]), 
                vectorFromPhyCamToPrimaryZPlaneInPhyCamRefFrame);
        Logger.trace("vectorFromPhyCamToPrimaryZPlaneInPhyCamRefFrame = " + 
                vectorFromPhyCamToPrimaryZPlaneInPhyCamRefFrame.dump());
        
        //Normalize the vector to find the desired principal point
        Core.multiply(vectorFromPhyCamToPrimaryZPlaneInPhyCamRefFrame, 
                new Scalar(1.0/vectorFromPhyCamToPrimaryZPlaneInPhyCamRefFrame.get(2, 0)[0]), 
                vectorFromPhyCamToDesiredPrincipalPointInPhyCamRefFrame);
        vectorFromPhyCamToPrimaryZPlaneInPhyCamRefFrame.release();
        Logger.trace("vectorFromPhyCamToDesiredPrincipalPointInPhyCamRefFrame = " + 
                vectorFromPhyCamToDesiredPrincipalPointInPhyCamRefFrame.dump());

        Mat vectorFromPhyCamToDesiredPrincipalPointInMachRefFrame = Mat.zeros(3, 1, CvType.CV_64FC1);
        Core.gemm(transformFromMachToPhyCamRefFrame.t(), 
                vectorFromPhyCamToDesiredPrincipalPointInPhyCamRefFrame, 1, 
                vectorFromPhyCamToDesiredPrincipalPointInPhyCamRefFrame, 0, 
                vectorFromPhyCamToDesiredPrincipalPointInMachRefFrame);
        Logger.trace("vectorFromPhyCamToDesiredPrincipalPointInMachRefFrame = " + 
                vectorFromPhyCamToDesiredPrincipalPointInMachRefFrame.dump());
        vectorFromPhyCamToDesiredPrincipalPointInMachRefFrame.release();

        Mat vectorFromPhyCamToPrimaryZPrincipalPointInMachRefFrame = Mat.zeros(3, 1, 
                CvType.CV_64FC1);
        Core.multiply(unitVectorPhyCamZInMachRefFrame, 
                new Scalar(cameraToZPlane/unitVectorPhyCamZInMachRefFrame.get(2, 0)[0]), 
                vectorFromPhyCamToPrimaryZPrincipalPointInMachRefFrame);
        Logger.trace("vectorFromPhyCamToPrimaryZPrincipalPointInMachRefFrame = " + 
                vectorFromPhyCamToPrimaryZPrincipalPointInMachRefFrame.dump());

        double absoluteCameraToPrimaryZPrincipalPointDistance = 
                Core.norm(vectorFromPhyCamToPrimaryZPrincipalPointInMachRefFrame, Core.NORM_L2);
        vectorFromPhyCamToPrimaryZPrincipalPointInMachRefFrame.release();
        Logger.trace("absoluteCameraToPrimaryZPrincipalPointDistance = " +
                absoluteCameraToPrimaryZPrincipalPointDistance);
        
        //The virtual camera is centered directly above the physical camera and looks straight 
        //down orthogonal to the machine's X-Y plane. This may be intuitive for top cameras, but
        //this is also desired for bottom cameras as this will make the image of the bottom of a
        //part held by the nozzle look as if it were taken from above through the top of the part
        //by an x-ray camera (which is exactly what is desired).
        vectorFromMachToVirCamInMachRefFrame.release();
        vectorFromMachToVirCamInMachRefFrame = vectorFromMachToPhyCamInMachRefFrame.clone();
        vectorFromMachToVirCamInMachRefFrame.put(2, 0, 
                primaryZ + absoluteCameraToPrimaryZPrincipalPointDistance);
        Logger.trace("vectorFromMachToVirCamInMachRefFrame = " + 
                vectorFromMachToVirCamInMachRefFrame.dump());
        
        //The virtual camera is oriented with its X-axis perfectly aligned with the machine's 
        //X-axis and its Y and Z-axis in the exact opposite direction of the machine's respective Y 
        //and Z-axis).
        Mat transformFromMachToVirCamRefFrame = Mat.zeros(3, 3, CvType.CV_64FC1);
        transformFromMachToVirCamRefFrame.put(0,  0,  1);
        transformFromMachToVirCamRefFrame.put(1,  1,  -1);
        transformFromMachToVirCamRefFrame.put(2,  2, -1);
        
        //Construct the rectification matrix
        rectificationMatrix = CameraCalibrationUtils.computeRectificationMatrix(
                transformFromMachToPhyCamRefFrame, vectorFromMachToPhyCamInMachRefFrame, 
                transformFromMachToVirCamRefFrame, vectorFromMachToVirCamInMachRefFrame, 
                primaryZ);
        transformFromMachToPhyCamRefFrame.release();
        transformFromMachToVirCamRefFrame.release();
        Logger.trace("rectification = " + rectificationMatrix.dump());

        virtualCameraMatrix = CameraCalibrationUtils.computeVirtualCameraMatrix(cameraMatrix, 
                distortionCoefficients, rectificationMatrix, size, alphaPercent / 100.0, 
                vectorFromPhyCamToDesiredPrincipalPointInPhyCamRefFrame);
    }
    
    public void initUndistortRectifyMap(Size size, Mat undistortionMap1, Mat undistortionMap2) {
        virtualCameraMatrix = CameraCalibrationUtils.computeVirtualCameraMatrix(cameraMatrix, 
                distortionCoefficients, rectificationMatrix, size, alphaPercent / 100.0, 
                vectorFromPhyCamToDesiredPrincipalPointInPhyCamRefFrame);
        Logger.trace("virtualCameraMatrix = " + virtualCameraMatrix.dump());

        Calib3d.initUndistortRectifyMap(cameraMatrix,
            distortionCoefficients, rectificationMatrix,
            virtualCameraMatrix, size, CvType.CV_32FC1,
            undistortionMap1, undistortionMap2);
    }
    

    public Length getDistanceToCameraAtZ(Length zHeight) {
        double z = zHeight.convertToUnits(LengthUnit.Millimeters).getValue();
        
        double cameraToZPlane = z - vectorFromMachToPhyCamInMachRefFrame.get(2, 0)[0];
        
        Mat vectorPhyCamToPointInMachRefFrame = Mat.zeros(3, 1, CvType.CV_64FC1);
        Core.multiply(unitVectorPhyCamZInMachRefFrame, 
                new Scalar(cameraToZPlane/unitVectorPhyCamZInMachRefFrame.get(2, 0)[0]), 
                vectorPhyCamToPointInMachRefFrame);
        
        double distance = Core.norm(vectorPhyCamToPointInMachRefFrame);
        vectorPhyCamToPointInMachRefFrame.release();
        
        return new Length(distance, LengthUnit.Millimeters).convertToUnits(zHeight.getUnits());
    }

}