package org.openpnp.util;

import java.util.TreeSet;

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
import org.opencv.core.Size;
import org.pmw.tinylog.Logger;

public class CameraCalibrationUtils {

    private static final int maxEvaluations = 500;
    private static final int maxIterations = 500;
    public static final int KEEP_ASPECT_RATIO = 1;
    public static final int KEEP_CENTER_POINT = 2;
    public static final int KEEP_DISTORTION_COEFFICENTS = 4;
    public static final int KEEP_ROTATION = 8;
    
    
    private static int numberOfTestPatterns;
    private static int numberOfParameters;
    private static double aspectRatio;

    public static double[] ComputeBestCameraParameters(double[][][] testPattern3dPoints, 
            double[][][] testPatternImagePoints, double[] starting) {
        return ComputeBestCameraParameters(testPattern3dPoints, 
                testPatternImagePoints, starting, 0);
    }
    
    public static double[] ComputeBestCameraParameters(double[][][] testPattern3dPoints, 
            double[][][] testPatternImagePoints, double[] starting, int flags) {
        numberOfTestPatterns = testPattern3dPoints.length;
        numberOfParameters = 4 + 5 + 3 + 1 + 2*numberOfTestPatterns;
        aspectRatio = starting[1] / starting[0];
        TreeSet<Integer> badPoints = new TreeSet<Integer>();

        LevenbergMarquardtOptimizer optimizer = new LevenbergMarquardtOptimizer();
        Optimum optimum = null;
        
        for (int attempt=0; attempt<2; attempt++) {
            CalibrationModel model = new CalibrationModel(testPattern3dPoints, badPoints, flags);
            
            RealVector observed = new ArrayRealVector();
            int iPoint = 0;
            for (int i=0; i<numberOfTestPatterns; i++) {
                for (int j=0; j<testPatternImagePoints[i].length; j++) {
                    if (!badPoints.contains(iPoint)) {
                        observed = observed.append(new ArrayRealVector(testPatternImagePoints[i][j]));
                    }
                    iPoint++;
                }
            }
            
            RealVector start = new ArrayRealVector(starting);
    
            ConvergenceChecker<LeastSquaresProblem.Evaluation> checker = LeastSquaresFactory.evaluationChecker(new SimpleVectorValueChecker(1e-6, 1e-8));
            
            LeastSquaresProblem lsp = LeastSquaresFactory.create(model, observed, start, checker, maxEvaluations, maxIterations);
            
            optimum = optimizer.optimize(lsp);
            Logger.trace("rms error = " + optimum.getRMS());
            Logger.trace("number of evaluations = " + optimum.getEvaluations());
            Logger.trace("number of iterations = " + optimum.getIterations());
            Logger.trace("residuals = " + optimum.getResiduals().toString());
            Logger.trace("parameters = " + optimum.getPoint().toString());
            
            if (badPoints.isEmpty()) {
                double[] residuals = optimum.getResiduals().toArray();
                double residualVariance = 0;
                for (int i=0; i<residuals.length; i++) {
                    residualVariance += residuals[i]*residuals[i];
                }
                residualVariance = residualVariance/residuals.length;
                for (int i=0; i<residuals.length; i++) {
                    if (residuals[i]*residuals[i] > 9*residualVariance) {
                        badPoints.add(i/2);
                    }
                }
                
                if (badPoints.isEmpty()) {
                    break;
                }
                else {
                    Logger.trace("Repeating parameter estimation with {} outliers removed from the set of {} points", badPoints.size(), residuals.length/2);
                }
            }
        }

        RealVector ans = optimum.getPoint();
        if ((flags & KEEP_ASPECT_RATIO) != 0) {
            ans.setEntry(1, ans.getEntry(0));
        }
        return ans.toArray();
    }
    
    private static class CalibrationModel implements MultivariateJacobianFunction {

        double[][][] testPattern3dPoints;
        int totalNumberOfPoints;
        double allowCenterToChange;
        double allowDistortionToChange;
        double allowRotationToChange;
        TreeSet<Integer> badPoints;
        int flags;
        
        public CalibrationModel(double[][][] testPattern3dPoints, TreeSet<Integer> badPoints, int flags) {
            this.badPoints = badPoints;
            this.flags = flags;
            this.testPattern3dPoints = testPattern3dPoints;
            totalNumberOfPoints = 0;
            for (int iTP=0; iTP<numberOfTestPatterns; iTP++) {
                totalNumberOfPoints += testPattern3dPoints[iTP].length;
            }
            totalNumberOfPoints -= badPoints.size();
            
            if ((flags & KEEP_CENTER_POINT) == 0)  {
                allowCenterToChange = 1;
            }
            else {
                allowCenterToChange = 0;
            }
            if ((flags & KEEP_DISTORTION_COEFFICENTS) == 0)  {
                allowDistortionToChange = 1;
            }
            else {
                allowDistortionToChange = 0;
            }
            if ((flags & KEEP_ROTATION) == 0)  {
                allowRotationToChange = 1;
            }
            else {
                allowRotationToChange = 0;
            }
        }
        
        @Override
        public Pair<RealVector, RealMatrix> value(RealVector point) {
            RealVector funcValue = MatrixUtils.createRealVector(new double[2*totalNumberOfPoints]);
            RealMatrix funcJacobian = MatrixUtils.createRealMatrix(2*totalNumberOfPoints,13+2*numberOfTestPatterns);
            //point order is fx, fy, cx, cy, k1, k2, p1, p2, k3, Rx, Ry, Rz, cam_z, cam_i_x, cam_i_y
            double fx = point.getEntry(0);
            double fy;
            if ((flags & KEEP_ASPECT_RATIO) == 0) {
                fy = point.getEntry(1);
            }
            else {
                fy = aspectRatio * point.getEntry(0);
            }
            double cx = point.getEntry(2);
            double cy = point.getEntry(3);
            double k1 = point.getEntry(4);
            double k2 = point.getEntry(5);
            double p1 = point.getEntry(6);
            double p2 = point.getEntry(7);
            double k3 = point.getEntry(8);
            double Rx = point.getEntry(9);
            double Ry = point.getEntry(10);
            double Rz = point.getEntry(11);
            if (Rx==0 && Ry==0 && Rz == 0) {
                Rz = 1e-6;
            }
            double cam_z = point.getEntry(12);
            double temp010 = Rz*Rz;
            double temp009 = Ry*Ry;
            double temp008 = Rx*Rx;
            double temp105 = temp010*Rz;
            double temp090 = temp009*Ry;
            double temp062 = temp008*Rx;
            double temp007 = temp008 + temp009 + temp010;
            double temp013 = Math.sqrt(temp007);
            double temp017 = 1.0/temp013;
            double temp050 = temp017/temp007;
            double temp018 = Math.sin(temp013);
            double temp051 = Ry*temp008*temp018*temp050;
            double temp064 = Rx*Ry*temp018*temp050;
            double temp084 = Ry*temp010*temp018*temp050;
            double temp016 = Rz*temp017*temp018;
            double temp108 = temp010*temp018*temp050;
            double temp092 = Rx*temp010*temp018*temp050;
            double temp059 = temp008*temp018*temp050;
            double temp028 = Ry*temp017*temp018;
            double temp056 = Rx*Ry*Rz*temp018*temp050;
            double temp088 = Ry*Rz*temp018*temp050;
            double temp095 = Rx*temp009*temp018*temp050;
            double temp065 = Rz*temp008*temp018*temp050;
            double temp049 = Rx*Rz*temp018*temp050;
            double temp080 = temp009*temp018*temp050;
            double temp081 = Rz*temp009*temp018*temp050;
            double temp060 = temp017*temp018;
            double temp037 = Rx*temp017*temp018;
            double temp012 = Math.cos(temp013);
            double temp011 = temp012 - 1;
            double temp006 = 1.0/temp007;
            double temp053 = temp006*temp006;
            double temp052 = 2*Ry*temp008*temp011*temp053;
            double temp066 = 2*Rz*temp008*temp011*temp053;
            double temp085 = 2*Ry*temp010*temp011*temp053;
            double temp094 = 2*Rx*temp009*temp011*temp053;
            double temp082 = 2*Rz*temp009*temp011*temp053;
            double temp091 = 2*Rx*temp010*temp011*temp053;
            double temp057 = 2*Rx*Ry*Rz*temp011*temp053;
            double temp031 = temp006*temp010*temp011 - temp012;
            double temp015 = Rx*Ry*temp006*temp011;
            double temp014 = temp015 + temp016;
            double temp034 = temp015 - temp016;
            double temp048 = Ry*temp006*temp011;
            double temp058 = temp006*temp008*temp012;
            double temp030 = Ry*Rz*temp006*temp011;
            double temp029 = -Rx*temp017*temp018 + temp030;
            double temp036 = temp030 + temp037;
            double temp079 = temp006*temp009*temp012;
            double temp083 = Rz*temp006*temp011;
            double temp087 = Ry*Rz*temp006*temp012;
            double temp020 = Rx*Rz*temp006*temp011;
            double temp019 = -Ry*temp017*temp018 + temp020;
            double temp027 = temp020 + temp028;
            double temp035 = temp006*temp009*temp011 - temp012;
            double temp047 = Rx*Rz*temp006*temp012;
            double temp063 = Rx*Ry*temp006*temp012;
            double temp096 = Rx*temp006*temp011;
            double temp107 = temp006*temp010*temp012;
            
            int rowIdx = 0;
            int iPoint = 0;
            for (int iTP=0; iTP<numberOfTestPatterns; iTP++) {
                for (int iPt=0; iPt<testPattern3dPoints[iTP].length; iPt++) {
                    if (!badPoints.contains(iPoint)) {
                        double temp023 = cam_z - testPattern3dPoints[iTP][iPt][2];
                        double temp022 = point.getEntry(14+2*iTP) - testPattern3dPoints[iTP][iPt][1];
                        double temp021 = point.getEntry(13+2*iTP) - testPattern3dPoints[iTP][iPt][0];
                        double temp026 = temp021*temp027 + temp022*temp029 + temp023*temp031;
                        double temp043 = 1.0/temp026;
                        double temp025 = temp043*temp043;
                        double temp076 = temp043*temp025;
                        double temp033 = temp021*temp034 + temp022*temp035 + temp023*temp036;
                        double temp032 = temp033*temp033;
                        double temp121 = -temp031*temp032*temp076;
                        double temp133 = -temp029*temp032*temp076;
                        double temp024 = temp025*temp032;
                        double temp127 = -temp027*temp032*temp076;
                        double temp131 = temp025*temp033*temp035;
                        double temp125 = temp025*temp033*temp034;
                        double temp119 = temp025*temp033*temp036;
                        double temp046 = -temp022*(temp037 - temp094 - temp095) + 
                                temp021*(temp047 - temp048 - temp049 + temp051 + temp052) + 
                                temp023*(temp056 + temp057 - temp058 + temp059 - temp060);
                        double temp073 = temp025*temp033*temp046;
                        double temp078 = temp023*(temp028 - temp084 - temp085) - 
                                temp021*(temp056 + temp057 - temp060 - temp079 + temp080) - 
                                temp022*(temp063 - temp064 + temp081 + temp082 - temp083);
                        double temp101 = temp032*temp076*temp078;
                        double temp068 = -temp023*(temp037 - temp091 - temp092) + 
                                temp022*(temp056 + temp057 + temp058 - temp059 + temp060) - 
                                temp021*(temp063 - temp064 - temp065 - temp066 + temp083);
                        double temp077 = -temp032*temp068*temp076;
                        double temp061 = -(2*Rx*temp006*temp011 - temp018*temp050*temp062 - 
                                2*temp011*temp053*temp062 + temp037)*temp021 - 
                                temp022*(temp047 + temp048 - temp049 - temp051 - temp052) + 
                                temp023*(temp063 - temp064 + temp065 + temp066 - temp083);
                        double temp097 = temp021*(temp028 - temp051 - temp052) - 
                                temp023*(temp056 + temp057 + temp060 + temp079 - temp080) + 
                                temp022*(temp087 - temp088 - temp094 - temp095 + temp096);
                        double temp086 = -(2*Ry*temp006*temp011 - temp018*temp050*temp090 - 
                                2*temp011*temp053*temp090 + temp028)*temp022 - 
                                temp023*(temp063 - temp064 - temp081 - temp082 + temp083) + 
                                temp021*(temp087 - temp088 + temp094 + temp095 - temp096);
                        double temp102 = temp025*temp033*temp086;
                        double temp104 = -(2*Rz*temp006*temp011 - temp018*temp050*temp105 - 
                                2*temp011*temp053*temp105 + temp016)*temp023 + 
                                temp022*(temp047 - temp048 - temp049 + temp084 + temp085) - 
                                temp021*(temp087 - temp088 - temp091 - temp092 + temp096);
                        double temp113 = temp032*temp076*temp104;
                        double temp106 = -(temp016 - temp081 - temp082)*temp022 - 
                                temp023*(temp047 + temp048 - temp049 - temp084 - temp085) + 
                                temp021*(temp056 + temp057 + temp060 + temp107 - temp108);
                        double temp114 = -temp025*temp033*temp106;
                        double temp109 = (temp016 - temp065 - temp066)*temp021 - 
                                temp022*(temp056 + temp057 - temp060 - temp107 + temp108) - 
                                temp023*(temp087 - temp088 + temp091 + temp092 - temp096);
                        double temp005 = temp006*temp008*temp011 - temp012;
                        double temp004 = temp005*temp021 + temp014*temp022 + temp019*temp023;
                        double temp118 = temp004*temp019*temp025;
                        double temp130 = temp004*temp014*temp025;
                        double temp103 = -temp004*temp025*temp097;
                        double temp124 = temp004*temp005*temp025;
                        double temp074 = temp004*temp025*temp061;
                        double temp115 = temp004*temp025*temp109;
                        double temp003 = temp004*temp004;
                        double temp126 = -temp003*temp027*temp076;
                        double temp123 = temp124 + temp125 + temp126 + temp127;
                        double temp075 = -temp003*temp068*temp076;
                        double temp072 = temp073 + temp074 + temp075 + temp077;
                        double temp112 = temp003*temp076*temp104;
                        double temp111 = temp112 + temp113 + temp114 + temp115;
                        double temp120 = -temp003*temp031*temp076;
                        double temp117 = temp118 + temp119 + temp120 + temp121;
                        double temp132 = -temp003*temp029*temp076;
                        double temp129 = temp130 + temp131 + temp132 + temp133;
                        double temp041 = temp003*temp025;
                        double temp040 = temp024 + temp041;
                        double temp042 = temp040*temp040;
                        double temp128 = 2*k2*temp040*temp129 + 3*k3*temp042*temp129 + k1*temp129;
                        double temp116 = 2*k2*temp040*temp117 + 3*k3*temp042*temp117 + k1*temp117;
                        double temp110 = 2*k2*temp040*temp111 + 3*k3*temp042*temp111 + k1*temp111;
                        double temp071 = 2*k2*temp040*temp072 + 3*k3*temp042*temp072 + k1*temp072;
                        double temp122 = 2*k2*temp040*temp123 + 3*k3*temp042*temp123 + k1*temp123;
                        double temp039 = temp040*temp042;
                        double temp038 = k3*temp039 + k1*temp040 + k2*temp042 + 1;
                        double temp045 = 3*temp025*temp032 + temp041;
                        double temp044 = 2*p2*temp004*temp025*temp033 + temp033*temp038*temp043 + p1*temp045;
                        double temp100 = temp003*temp076*temp078;
                        double temp099 = temp100 + temp101 + temp102 + temp103;
                        double temp098 = 2*k2*temp040*temp099 + 3*k3*temp042*temp099 + k1*temp099;
                        double temp002 = 3*temp003*temp025 + temp024;
                        double temp001 = 2*p1*temp004*temp025*temp033 + temp004*temp038*temp043 + p2*temp002;
                        
                        funcValue.setEntry(rowIdx, fx*temp001 + cx);
                        
                        funcJacobian.setEntry(rowIdx, 0, temp001);
                        funcJacobian.setEntry(rowIdx, 1, 0);
                        funcJacobian.setEntry(rowIdx, 2, 1*allowCenterToChange);
                        funcJacobian.setEntry(rowIdx, 3, 0*allowCenterToChange);
                        funcJacobian.setEntry(rowIdx, 4, fx*temp004*temp040*temp043*allowDistortionToChange);
                        funcJacobian.setEntry(rowIdx, 5, fx*temp004*temp042*temp043*allowDistortionToChange);
                        funcJacobian.setEntry(rowIdx, 6, 2*fx*temp004*temp025*temp033*allowDistortionToChange);
                        funcJacobian.setEntry(rowIdx, 7, fx*temp002*allowDistortionToChange);
                        funcJacobian.setEntry(rowIdx, 8, fx*temp004*temp039*temp043*allowDistortionToChange);
                        funcJacobian.setEntry(rowIdx, 9, (4*p1*temp004*temp033*temp068*temp076 - 2*p1*temp004*temp025*temp046 - 
                                2*p1*temp025*temp033*temp061 + temp004*temp025*temp038*temp068 - 
                                temp038*temp043*temp061 - 2*temp004*temp043*temp071 - 
                                2*(3*temp004*temp025*temp061 - 3*temp003*temp068*temp076 + temp073 + temp077)*p2)*fx*allowRotationToChange);
                        funcJacobian.setEntry(rowIdx, 10, -(4*p1*temp004*temp033*temp076*temp078 + temp004*temp025*temp038*temp078 + 
                                2*p1*temp004*temp025*temp086 - 2*p1*temp025*temp033*temp097 - 
                                temp038*temp043*temp097 + 2*temp004*temp043*temp098 + 
                                2*(3*temp003*temp076*temp078 - 3*temp004*temp025*temp097 + temp101 + temp102)*p2)*fx*allowRotationToChange);
                        funcJacobian.setEntry(rowIdx, 11, (4*p1*temp004*temp033*temp076*temp104 + temp004*temp025*temp038*temp104 - 
                                2*p1*temp004*temp025*temp106 + 2*p1*temp025*temp033*temp109 + 
                                temp038*temp043*temp109 + 2*temp004*temp043*temp110 + 
                                2*(3*temp003*temp076*temp104 + 3*temp004*temp025*temp109 + temp113 + temp114)*p2)*fx*allowRotationToChange);
                        funcJacobian.setEntry(rowIdx, 12, -(4*p1*temp004*temp031*temp033*temp076 - 2*p1*temp019*temp025*temp033 - 
                                2*p1*temp004*temp025*temp036 + temp004*temp025*temp031*temp038 - 
                                temp019*temp038*temp043 - 2*temp004*temp043*temp116 - 
                                2*(3*temp004*temp019*temp025 - 3*temp003*temp031*temp076 + temp119 + temp121)*p2)*fx);
                        funcJacobian.setEntry(rowIdx, 13+2*iTP, -(4*p1*temp004*temp027*temp033*temp076 - 2*p1*temp005*temp025*temp033 - 
                                2*p1*temp004*temp025*temp034 + temp004*temp025*temp027*temp038 - 
                                temp005*temp038*temp043 - 2*temp004*temp043*temp122 - 
                                2*(3*temp004*temp005*temp025 - 3*temp003*temp027*temp076 + temp125 + temp127)*p2)*fx);
                        funcJacobian.setEntry(rowIdx, 14+2*iTP, -(4*p1*temp004*temp029*temp033*temp076 - 2*p1*temp014*temp025*temp033 - 
                                2*p1*temp004*temp025*temp035 + temp004*temp025*temp029*temp038 - 
                                temp014*temp038*temp043 - 2*temp004*temp043*temp128 - 
                                2*(3*temp004*temp014*temp025 - 3*temp003*temp029*temp076 + temp131 + temp133)*p2)*fx);
                        rowIdx++;
                        
                        funcValue.setEntry(rowIdx, fy*temp044 + cy);
                        
                        funcJacobian.setEntry(rowIdx, 0, 0);
                        if ((flags & KEEP_ASPECT_RATIO) == 0) {
                            funcJacobian.setEntry(rowIdx, 0, 0);
                            funcJacobian.setEntry(rowIdx, 1, temp044);
                        }
                        else {
                            funcJacobian.setEntry(rowIdx, 0, aspectRatio * temp044);
                            funcJacobian.setEntry(rowIdx, 1, 0);
                        }
                        funcJacobian.setEntry(rowIdx, 2, 0*allowCenterToChange);
                        funcJacobian.setEntry(rowIdx, 3, 1*allowCenterToChange);
                        funcJacobian.setEntry(rowIdx, 4, fy*temp033*temp040*temp043*allowDistortionToChange);
                        funcJacobian.setEntry(rowIdx, 5, fy*temp033*temp042*temp043*allowDistortionToChange);
                        funcJacobian.setEntry(rowIdx, 6, fy*temp045*allowDistortionToChange);
                        funcJacobian.setEntry(rowIdx, 7, 2*fy*temp004*temp025*temp033*allowDistortionToChange);
                        funcJacobian.setEntry(rowIdx, 8, fy*temp033*temp039*temp043*allowDistortionToChange);
                        funcJacobian.setEntry(rowIdx, 9, (4*p2*temp004*temp033*temp068*temp076 - 2*p2*temp004*temp025*temp046 - 
                                2*p2*temp025*temp033*temp061 + temp025*temp033*temp038*temp068 - 
                                temp038*temp043*temp046 - 2*temp033*temp043*temp071 - 
                                2*(3*temp025*temp033*temp046 - 3*temp032*temp068*temp076 + temp074 + temp075)*p1)*fy);
                        funcJacobian.setEntry(rowIdx, 10, -(4*p2*temp004*temp033*temp076*temp078 + temp025*temp033*temp038*temp078 + 
                                2*p2*temp004*temp025*temp086 - 2*p2*temp025*temp033*temp097 + 
                                temp038*temp043*temp086 + 2*temp033*temp043*temp098 + 
                                2*(3*temp032*temp076*temp078 + 3*temp025*temp033*temp086 + temp100 + temp103)*p1)*fy);
                        funcJacobian.setEntry(rowIdx, 11, (4*p2*temp004*temp033*temp076*temp104 + temp025*temp033*temp038*temp104 - 
                                2*p2*temp004*temp025*temp106 + 2*p2*temp025*temp033*temp109 - 
                                temp038*temp043*temp106 + 2*temp033*temp043*temp110 + 
                                2*(3*temp032*temp076*temp104 - 3*temp025*temp033*temp106 + temp112 + temp115)*p1)*fy);
                        funcJacobian.setEntry(rowIdx, 12, -(4*p2*temp004*temp031*temp033*temp076 - 2*p2*temp019*temp025*temp033 - 
                                2*p2*temp004*temp025*temp036 + temp025*temp031*temp033*temp038 - 
                                temp036*temp038*temp043 - 2*temp033*temp043*temp116 - 
                                2*(3*temp025*temp033*temp036 - 3*temp031*temp032*temp076 + temp118 + temp120)*p1)*fy);
                        funcJacobian.setEntry(rowIdx, 13+2*iTP, -(4*p2*temp004*temp027*temp033*temp076 - 2*p2*temp005*temp025*temp033 - 
                                2*p2*temp004*temp025*temp034 + temp025*temp027*temp033*temp038 - 
                                temp034*temp038*temp043 - 2*temp033*temp043*temp122 - 
                                2*(3*temp025*temp033*temp034 - 3*temp027*temp032*temp076 + temp124 + temp126)*p1)*fy);
                        funcJacobian.setEntry(rowIdx, 14+2*iTP, -(4*p2*temp004*temp029*temp033*temp076 - 2*p2*temp014*temp025*temp033 - 
                                2*p2*temp004*temp025*temp035 + temp025*temp029*temp033*temp038 - 
                                temp035*temp038*temp043 - 2*temp033*temp043*temp128 - 
                                2*(3*temp025*temp033*temp035 - 3*temp029*temp032*temp076 + temp130 + temp132)*p1)*fy);
                        rowIdx++;
                    }
                    iPoint++;
                }
            }
            return new Pair<RealVector, RealMatrix>(funcValue, funcJacobian);
        }
        
    }
}
