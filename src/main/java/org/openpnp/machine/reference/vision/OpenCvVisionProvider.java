/*
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
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

package org.openpnp.machine.reference.vision;



import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.vision.wizards.OpenCvVisionProviderConfigurationWizard;
import org.openpnp.spi.Camera;
import org.openpnp.spi.VisionProvider;
import org.openpnp.util.ImageUtils;
import org.openpnp.util.LogUtils;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.util.VisionUtils;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Root;

@Root
public class OpenCvVisionProvider implements VisionProvider {


    static {
        nu.pattern.OpenCV.loadShared();
        System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
    }

    protected Camera camera;

    @Override
    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new OpenCvVisionProviderConfigurationWizard(this);
    }

    protected Mat getCameraImage() {
        BufferedImage image_ = camera.capture();
        Mat image = OpenCvUtils.toMat(image_);
        return image;
    }

    /**
     * Attempt to find matches of the given template within the current camera frame. Matches are
     * returned as TemplateMatch objects which contain a Location in Camera coordinates. The results
     * are sorted best score to worst score.
     * 
     * @param template
     * @return
     */
    public List<TemplateMatch> getTemplateMatches(BufferedImage template) {
        BufferedImage image = camera.capture();

        // Convert the camera image and template image to the same type. This
        // is required by the cvMatchTemplate call.
        template = ImageUtils.convertBufferedImage(template, BufferedImage.TYPE_BYTE_GRAY);
        image = ImageUtils.convertBufferedImage(image, BufferedImage.TYPE_BYTE_GRAY);

        Mat templateMat = OpenCvUtils.toMat(template);
        Mat imageMat = OpenCvUtils.toMat(image);
        Mat resultMat = new Mat();

        Imgproc.matchTemplate(imageMat, templateMat, resultMat, Imgproc.TM_CCOEFF_NORMED);

        Mat debugMat = null;
        if (LogUtils.isDebugEnabled()) {
            debugMat = imageMat.clone();
        }

        MinMaxLocResult mmr = Core.minMaxLoc(resultMat);
        double maxVal = mmr.maxVal;

        double threshold = 0.7f;
        double corr = 0.85f;

        double rangeMin = Math.max(threshold, corr * maxVal);
        double rangeMax = maxVal;

        List<TemplateMatch> matches = new ArrayList<>();
        for (Point point : OpenCvUtils.matMaxima(resultMat, rangeMin, rangeMax)) {
            TemplateMatch match = new TemplateMatch();
            int x = point.x;
            int y = point.y;
            match.score = resultMat.get(y, x)[0] / maxVal;

            if (LogUtils.isDebugEnabled()) {
                Core.rectangle(debugMat, new org.opencv.core.Point(x, y),
                        new org.opencv.core.Point(x + templateMat.cols(), y + templateMat.rows()),
                        new Scalar(255));
                Core.putText(debugMat, "" + match.score,
                        new org.opencv.core.Point(x + templateMat.cols(), y + templateMat.rows()),
                        Core.FONT_HERSHEY_PLAIN, 1.0, new Scalar(255));
            }

            match.location = VisionUtils.getPixelLocation(camera, x + (templateMat.cols() / 2),
                    y + (templateMat.rows() / 2));
            matches.add(match);
        }

        Collections.sort(matches, new Comparator<TemplateMatch>() {
            @Override
            public int compare(TemplateMatch o1, TemplateMatch o2) {
                return ((Double) o2.score).compareTo(o1.score);
            }
        });

        long t = System.currentTimeMillis();
        OpenCvUtils.saveDebugImage(OpenCvVisionProvider.class, "getTemplateMatches", "template", templateMat);
        OpenCvUtils.saveDebugImage(OpenCvVisionProvider.class, "getTemplateMatches", "camera", imageMat);
        OpenCvUtils.saveDebugImage(OpenCvVisionProvider.class, "getTemplateMatches", "result", resultMat);
        OpenCvUtils.saveDebugImage(OpenCvVisionProvider.class, "getTemplateMatches", "debug", debugMat);

        return matches;
    }

    @Override
    public Point[] locateTemplateMatches(int roiX, int roiY, int roiWidth, int roiHeight, int coiX,
            int coiY, BufferedImage templateImage_) throws Exception {
        BufferedImage cameraImage_ = camera.capture();

        // Convert the camera image and template image to the same type. This
        // is required by the cvMatchTemplate call.
        templateImage_ =
                ImageUtils.convertBufferedImage(templateImage_, BufferedImage.TYPE_INT_ARGB);
        cameraImage_ = ImageUtils.convertBufferedImage(cameraImage_, BufferedImage.TYPE_INT_ARGB);

        Mat templateImage = OpenCvUtils.toMat(templateImage_);
        Mat cameraImage = OpenCvUtils.toMat(cameraImage_);
        Mat roiImage = new Mat(cameraImage, new Rect(roiX, roiY, roiWidth, roiHeight));

        // http://stackoverflow.com/questions/17001083/opencv-template-matching-example-in-android
        Mat resultImage = new Mat(roiImage.cols() - templateImage.cols() + 1,
                roiImage.rows() - templateImage.rows() + 1, CvType.CV_32FC1);
        Imgproc.matchTemplate(roiImage, templateImage, resultImage, Imgproc.TM_CCOEFF);

        MinMaxLocResult mmr = Core.minMaxLoc(resultImage);

        org.opencv.core.Point matchLoc = mmr.maxLoc;
        double matchValue = mmr.maxVal;

        // TODO: Figure out certainty and how to filter on it.

        Logger.debug(String.format("locateTemplateMatches certainty %f at %f, %f", matchValue,
                matchLoc.x, matchLoc.y));
        locateTemplateMatchesDebug(roiImage, templateImage, matchLoc);

        return new Point[] {new Point(((int) matchLoc.x) + roiX, ((int) matchLoc.y) + roiY)};
    }

    private void locateTemplateMatchesDebug(Mat roiImage, Mat templateImage,
            org.opencv.core.Point matchLoc) {
        if (LogUtils.isDebugEnabled()) {
            try {
                Core.rectangle(roiImage, matchLoc,
                        new org.opencv.core.Point(matchLoc.x + templateImage.cols(),
                                matchLoc.y + templateImage.rows()),
                        new Scalar(0, 255, 0));

                OpenCvUtils.saveDebugImage(OpenCvVisionProvider.class, "locateTemplateMatches", "debug", roiImage);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
