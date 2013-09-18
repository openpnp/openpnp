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



import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_32F;
import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_8U;
import static com.googlecode.javacv.cpp.opencv_core.cvConvertScaleAbs;
import static com.googlecode.javacv.cpp.opencv_core.cvCreateImage;
import static com.googlecode.javacv.cpp.opencv_core.cvGetSeqElem;
import static com.googlecode.javacv.cpp.opencv_core.cvMinMaxLoc;
import static com.googlecode.javacv.cpp.opencv_core.cvRect;
import static com.googlecode.javacv.cpp.opencv_core.cvResetImageROI;
import static com.googlecode.javacv.cpp.opencv_core.cvSetImageROI;
import static com.googlecode.javacv.cpp.opencv_core.cvSize;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_HOUGH_GRADIENT;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvHoughCircles;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvMatchTemplate;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.openpnp.gui.support.Wizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.Rectangle;
import org.openpnp.spi.Camera;
import org.openpnp.spi.VisionProvider;
import org.simpleframework.xml.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.javacv.cpp.opencv_core.CvMemStorage;
import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvPoint3D32f;
import com.googlecode.javacv.cpp.opencv_core.CvSeq;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_imgproc;

public class OpenCvVisionProvider implements VisionProvider {
    private final static Logger logger = LoggerFactory
            .getLogger(OpenCvVisionProvider.class);

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

    private IplImage getCameraImage() {
        BufferedImage image_ = camera.capture();
        IplImage image = IplImage.createFrom(image_);
        return image;
    }

    private static void setROI(IplImage image, Rectangle roi) {
        cvSetImageROI(
                image,
                cvRect(roi.getX(), roi.getY(), roi.getWidth(), roi.getHeight()));
    }

    @Override
    public Circle[] locateCircles(int roiX, int roiY, int roiWidth,
            int roiHeight, int coiX, int coiY, int minimumDiameter,
            int diameter, int maximumDiameter) throws Exception {

        Rectangle roi = new Rectangle(roiX, roiY, roiWidth, roiHeight);
        return locateCircles(roi, minimumDiameter, maximumDiameter);
    }

    private Circle[] locateCircles(Rectangle roi, int minimumDiameter,
            int maximumDiameter) {

        IplImage image = getCameraImage();
        // cvSetImageROI(image, cvRect(roiX, roiY, roiWidth, roiHeight));
        setROI(image, roi);

        int minRadius = minimumDiameter / 2;
        int maxRadius = maximumDiameter / 2;

        // the accuracy of these numbers is questionable as I just made them up
        // to get this working
        int minDistance = 20;
        int edgeThreshold = 20;
        int circleThreshold = 20;

        return locateCircles(image, minDistance, edgeThreshold,
                circleThreshold, minRadius, maxRadius);

    }

    private Circle[] locateCircles(IplImage image, int minRadius,
            int maxRadius, int minDist, int edgeThreshold, int circleThreshold) {
        CvMemStorage mem = CvMemStorage.create();

        CvSeq circles = cvHoughCircles(image, mem, CV_HOUGH_GRADIENT, 1,
                minDist, edgeThreshold, circleThreshold, minRadius, maxRadius);

        List<Circle> circleList = new ArrayList<Circle>();

        for (int i = 0; i < circles.total(); i++) {
            CvPoint3D32f circle = new CvPoint3D32f(cvGetSeqElem(circles, i));
            Circle temp = new Circle(circle.x(), circle.y(), circle.z());
            circleList.add(temp);
        }

        mem.release();

        Circle[] returnCircles = new Circle[circles.total()];
        return circleList.toArray(returnCircles);
    }

    @Override
    public Point[] locateTemplateMatches(int roiX, int roiY, int roiWidth,
            int roiHeight, int coiX, int coiY, BufferedImage templateImage_)
            throws Exception {
        double minVal[] = new double[1];
        double maxVal[] = new double[1];
        CvPoint minLoc = new CvPoint();
        CvPoint maxLoc = new CvPoint();
        CvPoint resLoc;
        double resValue;

        BufferedImage image_ = camera.capture();
        
        // Convert the camera image and template image to the same type. This
        // is required by the cvMatchTemplate call.
        templateImage_ = convertBufferedImage(templateImage_,BufferedImage.TYPE_INT_ARGB);   
        image_ = convertBufferedImage(image_, BufferedImage.TYPE_INT_ARGB);
        
        IplImage templateImage = IplImage.createFrom(templateImage_);
        IplImage image = IplImage.createFrom(image_);
        
        IplImage result = cvCreateImage(
                cvSize(roiWidth - templateImage.width() + 1, roiHeight
                        - templateImage.height() + 1), IPL_DEPTH_32F, 1);

        cvSetImageROI(image, cvRect(roiX, roiY, roiWidth, roiHeight));
        cvMatchTemplate(image, templateImage, result,
                opencv_imgproc.CV_TM_CCOEFF);
        cvResetImageROI(image);
        cvMinMaxLoc(result, minVal, maxVal, minLoc, maxLoc, null);

        locateTemplateMatchesDebug(image_, roiX, roiY, roiWidth, roiHeight,
                templateImage_, result, minVal[0], maxVal[0]);

        resLoc = maxLoc;
        resValue = maxVal[0];

        // TODO: Figure out certainty and how to filter on it.

        logger.debug(String.format(
                "locateTemplateMatches certainty %f at %d, %d", resValue,
                resLoc.x(), resLoc.y()));

        return new Point[] { new Point(resLoc.x() + roiX, resLoc.y() + roiY) };
    }

    private void locateTemplateMatchesDebug(BufferedImage image_, int roiX,
            int roiY, int roiWidth, int roiHeight,
            BufferedImage templateImage_, IplImage result, double minVal,
            double maxVal) {
        if (logger.isDebugEnabled()) {
            try {
                // Create a debug image that contains the captured image, the
                // template image, the roi image, and the result image.

                int width = Math.max(image_.getWidth(), roiWidth);
                width = Math.max(width, result.width());
                width = Math.max(width, templateImage_.getWidth());
                int height = image_.getHeight() + roiHeight + result.height()
                        + templateImage_.getHeight();

                BufferedImage debugImage = new BufferedImage(width, height,
                        BufferedImage.TYPE_INT_ARGB);

                BufferedImage resultImage = new BufferedImage(result.width(),
                        result.height(), BufferedImage.TYPE_USHORT_GRAY);

                Graphics g = debugImage.getGraphics();

                g.setColor(Color.CYAN);
                g.fillRect(0, 0, width, height);
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, width, 100);

                g.setColor(Color.BLACK);

                int y = 0;

                g.drawImage(templateImage_, 0, y, null);
                y += templateImage_.getHeight();

                g.drawImage(image_, 0, y, null);
                y += image_.getHeight();

                g.drawImage(image_, 0, y, roiWidth, y + roiHeight, roiX, roiY,
                        roiX + roiWidth, roiY + roiHeight, null);
                y += roiHeight;

                IplImage resultTemp = cvCreateImage(
                        cvSize(result.width(), result.height()), IPL_DEPTH_8U,
                        1);
                cvConvertScaleAbs(result, resultTemp,
                        255.0 / (maxVal - minVal), 0);
                resultTemp.copyTo(resultImage, 1.0);
                g.drawImage(resultImage, 0, y, null);
                y += result.height();

                g.dispose();

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
    
    /**
     * Convert a BufferedImage from it's current type to a new, specified
     * type by creating a new BufferedImage and drawing the source image
     * onto it. If the image is already of the specified type it is
     * returned unchanged.
     * @param src
     * @param type
     * @return
     */
    public static BufferedImage convertBufferedImage(BufferedImage src,
            int type) {
        if (src.getType() == type) {
            return src;
        }
        BufferedImage img = new BufferedImage(src.getWidth(), src.getHeight(),
                type);
        Graphics2D g2d = img.createGraphics();
        g2d.drawImage(src, 0, 0, null);
        g2d.dispose();
        return img;
    }
}
