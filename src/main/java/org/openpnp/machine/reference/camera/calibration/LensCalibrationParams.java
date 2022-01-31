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

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.openpnp.model.AbstractModelObject;
import org.pmw.tinylog.Logger;
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

    protected Mat cameraMatrix = Mat.zeros(3, 3, CvType.CV_64FC1);
    protected Mat distortionCoefficients = Mat.zeros(5, 1, CvType.CV_64FC1);

    @Commit void commit() {
        cameraMatrix.put(0, 0, cameraMatrixArr);
        distortionCoefficients.put(0, 0, distortionCoefficientsArr);
        if (!Core.checkRange(distortionCoefficients, true, -10e6, +10e6)) {
            Logger.warn("distortionCoefficients = " + distortionCoefficients.dump());
            Logger.warn("Distortion Coefficients have extreme values - resetting all to zero");
            distortionCoefficients.put(0, 0, new double[5] );
        }
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