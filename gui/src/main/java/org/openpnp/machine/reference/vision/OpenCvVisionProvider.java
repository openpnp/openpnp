/*
 	Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 	
 	This file is part of OpenPnP.
 	
	OpenPnP is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenPnP is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenPnP.  If not, see <http://www.gnu.org/licenses/>.
 	
 	For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.machine.reference.vision;



import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.vision.wizards.OpenCvVisionProviderConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.VisionProvider;
import org.openpnp.spi.Camera.Looking;
import org.openpnp.util.OpenCvUtils;
import org.simpleframework.xml.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenCvVisionProvider implements VisionProvider {
    private final static Logger logger = LoggerFactory
            .getLogger(OpenCvVisionProvider.class);

    static {
        nu.pattern.OpenCV.loadShared();
        System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
    }    

    // SimpleXML requires at least one attribute or element on a class before
    // it will recognize it.
    @Attribute(required = false)
    private String dummy;

    private Camera camera;

    @Override
    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new OpenCvVisionProviderConfigurationWizard(this);
    }

    private Mat getCameraImage() {
        BufferedImage image_ = camera.capture();
        Mat image = OpenCvUtils.toMat(image_);
        return image;
    }

    @Override
    public Point[] locateTemplateMatches(int roiX, int roiY, int roiWidth,
            int roiHeight, int coiX, int coiY, BufferedImage templateImage_)
            throws Exception {
        BufferedImage cameraImage_ = camera.capture();
        
        // Convert the camera image and template image to the same type. This
        // is required by the cvMatchTemplate call.
        templateImage_ = OpenCvUtils.convertBufferedImage(templateImage_,BufferedImage.TYPE_INT_ARGB);   
        cameraImage_ = OpenCvUtils.convertBufferedImage(cameraImage_, BufferedImage.TYPE_INT_ARGB);
        
        Mat templateImage = OpenCvUtils.toMat(templateImage_);
        Mat cameraImage = OpenCvUtils.toMat(cameraImage_);
        Mat roiImage = new Mat(cameraImage, new Rect(roiX, roiY, roiWidth, roiHeight));
        
        // http://stackoverflow.com/questions/17001083/opencv-template-matching-example-in-android
        Mat resultImage = new Mat(
                roiImage.cols() - templateImage.cols() + 1, 
                roiImage.rows() - templateImage.rows() + 1, 
                CvType.CV_32FC1);
        Imgproc.matchTemplate(roiImage, templateImage, resultImage, Imgproc.TM_CCOEFF);
        
        MinMaxLocResult mmr = Core.minMaxLoc(resultImage);

        org.opencv.core.Point matchLoc = mmr.maxLoc;
        double matchValue = mmr.maxVal;

        // TODO: Figure out certainty and how to filter on it.

        logger.debug(String.format(
                "locateTemplateMatches certainty %f at %f, %f", matchValue,
                matchLoc.x, matchLoc.y));
        locateTemplateMatchesDebug(roiImage, templateImage, matchLoc);

        return new Point[] { new Point(((int) matchLoc.x) + roiX, ((int) matchLoc.y) + roiY) };
    }

    private void locateTemplateMatchesDebug(Mat roiImage, Mat templateImage, org.opencv.core.Point matchLoc) {
        if (logger.isDebugEnabled()) {
            try {
                Core.rectangle(roiImage, matchLoc, new org.opencv.core.Point(matchLoc.x + templateImage.cols(),
                        matchLoc.y + templateImage.rows()), new Scalar(0, 255, 0));                
                
                BufferedImage debugImage = OpenCvUtils.toBufferedImage(roiImage);
                File file = Configuration.get().createResourceFile(
                        OpenCvVisionProvider.class, "debug_", ".png");
                ImageIO.write(debugImage, "PNG", file);
                logger.debug("Debug image filename {}", file);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Location getPartBottomOffsets(Part part, Nozzle nozzle)
            throws Exception {
        if (camera.getLooking() != Looking.Up) {
            throw new Exception("Bottom vision only implemented for Up looking cameras");
        }

        // Position the part above the center of the camera.
        // First move to Safe-Z.
        nozzle.moveToSafeZ(1.0);
        // Then move to the camera in X, Y at Safe-Z and rotate the
        // part to 0.
        nozzle.moveTo(camera.getLocation().derive(null, null, Double.NaN, 0.0), 1.0);
        // Then lower the part to the Camera's focal point in Z. Maintain the 
        // part's rotation at 0.
        nozzle.moveTo(camera.getLocation().derive(null, null, null, Double.NaN), 1.0);
        // Grab an image.
        BufferedImage image = camera.capture();
        // Return to Safe-Z just to be safe.
        nozzle.moveToSafeZ(1.0);
        // TODO: Do OpenCV magic
        // Return the offsets. Make sure to convert them to real units instead
        // of pixels. Use camera.getUnitsPerPixel().
        return new Location(LengthUnit.Millimeters, 0, 0, 0, 0);
    } 
}
