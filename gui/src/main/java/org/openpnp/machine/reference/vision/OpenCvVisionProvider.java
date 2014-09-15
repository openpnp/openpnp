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
import org.openpnp.model.Configuration;
import org.openpnp.model.Rectangle;
import org.openpnp.spi.Camera;
import org.openpnp.spi.VisionProvider;
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
        // TODO Auto-generated method stub
        return null;
    }

    private Mat getCameraImage() {
        BufferedImage image_ = camera.capture();
        Mat image = OpenCvUtils.toMat(image_);
        return image;
    }

//    private static void setROI(Mat image, Rectangle roi) {
//        cvSetImageROI(
//                image,
//                cvRect(roi.getX(), roi.getY(), roi.getWidth(), roi.getHeight()));
//    }

    @Override
    public Circle[] locateCircles(int roiX, int roiY, int roiWidth,
            int roiHeight, int coiX, int coiY, int minimumDiameter,
            int diameter, int maximumDiameter) throws Exception {

        Rectangle roi = new Rectangle(roiX, roiY, roiWidth, roiHeight);
        return locateCircles(roi, minimumDiameter, maximumDiameter);
    }

    private Circle[] locateCircles(Rectangle roi, int minimumDiameter,
            int maximumDiameter) {
//
//        Mat image = getCameraImage();
//        setROI(image, roi);
//
//        int minRadius = minimumDiameter / 2;
//        int maxRadius = maximumDiameter / 2;
//
//        // the accuracy of these numbers is questionable as I just made them up
//        // to get this working
//        int minDistance = 20;
//        int edgeThreshold = 20;
//        int circleThreshold = 20;
//
//        return locateCircles(image, minDistance, edgeThreshold,
//                circleThreshold, minRadius, maxRadius);
        return new Circle[0];
    }

    private Circle[] locateCircles(Mat image, int minRadius,
            int maxRadius, int minDist, int edgeThreshold, int circleThreshold) {
//        CvMemStorage mem = CvMemStorage.create();
//
//        CvSeq circles = cvHoughCircles(image, mem, CV_HOUGH_GRADIENT, 1,
//                minDist, edgeThreshold, circleThreshold, minRadius, maxRadius);
//
//        List<Circle> circleList = new ArrayList<Circle>();
//
//        for (int i = 0; i < circles.total(); i++) {
//            CvPoint3D32f circle = new CvPoint3D32f(cvGetSeqElem(circles, i));
//            Circle temp = new Circle(circle.x(), circle.y(), circle.z());
//            circleList.add(temp);
//        }
//
//        mem.release();
//
//        Circle[] returnCircles = new Circle[circles.total()];
//        return circleList.toArray(returnCircles);
        return new Circle[0];
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
        locateTemplateMatchesDebug(cameraImage, templateImage, matchLoc);

        return new Point[] { new Point(((int) matchLoc.x) + roiX, ((int) matchLoc.y) + roiY) };
    }

    private void locateTemplateMatchesDebug(Mat roiImage, Mat templateImage, org.opencv.core.Point matchLoc) {
        if (logger.isDebugEnabled()) {
            try {
//                Core.rectangle(roiImage, matchLoc, new org.opencv.core.Point(matchLoc.x + templateImage.cols(),
//                        matchLoc.y + templateImage.rows()), new Scalar(0, 255, 0));                
                
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
}
