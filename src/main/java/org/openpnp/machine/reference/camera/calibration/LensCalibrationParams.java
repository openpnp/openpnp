package org.openpnp.machine.reference.camera.calibration;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.openpnp.model.AbstractModelObject;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persist;

public class LensCalibrationParams extends AbstractModelObject {
    @Attribute(required = false)
    protected boolean enabled = false;

    @Element(name = "cameraMatrix", required = false)
    private double[] cameraMatrixArr = new double[9];

    @Element(name = "distortionCoefficients", required = false)
    private double[] distortionCoefficientsArr = new double[5];

    protected Mat cameraMatrix = new Mat(3, 3, CvType.CV_64FC1);
    protected Mat distortionCoefficients = new Mat(5, 1, CvType.CV_64FC1);

    @Commit void commit() {
        cameraMatrix.put(0, 0, cameraMatrixArr);
        distortionCoefficients.put(0, 0, distortionCoefficientsArr);
    }

    @Persist void persist() {
        cameraMatrix.get(0, 0, cameraMatrixArr);
        distortionCoefficients.get(0, 0, distortionCoefficientsArr);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        Object oldValue = this.isEnabled();
        this.enabled = enabled;
        firePropertyChange("enabled", oldValue, enabled);
    }

    public Mat getCameraMatrixMat() {
        return cameraMatrix;
    }

    public void setCameraMatrixMat(Mat cameraMatrix) {
        this.cameraMatrix = cameraMatrix.clone();
    }

    public Mat getDistortionCoefficientsMat() {
        return distortionCoefficients;
    }

    public void setDistortionCoefficientsMat(Mat distortionCoefficients) {
        this.distortionCoefficients = distortionCoefficients.clone();
    }
}