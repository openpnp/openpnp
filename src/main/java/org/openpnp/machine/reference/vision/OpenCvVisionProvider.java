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
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
import org.openpnp.spi.Camera;
import org.openpnp.spi.VisionProvider;
import org.openpnp.util.ImageUtils;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.util.VisionUtils;
import org.simpleframework.xml.Root;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Root
public class OpenCvVisionProvider implements VisionProvider {
    private final static Logger logger = LoggerFactory.getLogger(OpenCvVisionProvider.class);

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
        // TODO: ROI
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
        if (logger.isDebugEnabled()) {
            debugMat = imageMat.clone();
        }

        MinMaxLocResult mmr = Core.minMaxLoc(resultMat);
        double maxVal = mmr.maxVal;

        // TODO: Externalize?
        double threshold = 0.7f;
        double corr = 0.85f;

        double rangeMin = Math.max(threshold, corr * maxVal);
        double rangeMax = maxVal;

        List<TemplateMatch> matches = new ArrayList<>();
        for (Point point : matMaxima(resultMat, rangeMin, rangeMax)) {
            TemplateMatch match = new TemplateMatch();
            int x = point.x;
            int y = point.y;
            match.score = resultMat.get(y, x)[0] / maxVal;

            if (logger.isDebugEnabled()) {
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
        saveDebugImage(t + "_0_template", templateMat);
        saveDebugImage(t + "_1_camera", imageMat);
        saveDebugImage(t + "_2_result", resultMat);
        saveDebugImage(t + "_3_debug", debugMat);

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

        logger.debug(String.format("locateTemplateMatches certainty %f at %f, %f", matchValue,
                matchLoc.x, matchLoc.y));
        locateTemplateMatchesDebug(roiImage, templateImage, matchLoc);

        return new Point[] {new Point(((int) matchLoc.x) + roiX, ((int) matchLoc.y) + roiY)};
    }

    protected void saveDebugImage(String name, Mat mat) {
        if (logger.isDebugEnabled()) {
            try {
                BufferedImage debugImage = OpenCvUtils.toBufferedImage(mat);
                File file = Configuration.get().createResourceFile(OpenCvVisionProvider.class,
                        name + "_", ".png");
                ImageIO.write(debugImage, "PNG", file);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void locateTemplateMatchesDebug(Mat roiImage, Mat templateImage,
            org.opencv.core.Point matchLoc) {
        if (logger.isDebugEnabled()) {
            try {
                Core.rectangle(roiImage, matchLoc,
                        new org.opencv.core.Point(matchLoc.x + templateImage.cols(),
                                matchLoc.y + templateImage.rows()),
                        new Scalar(0, 255, 0));

                BufferedImage debugImage = OpenCvUtils.toBufferedImage(roiImage);
                File file = Configuration.get().createResourceFile(OpenCvVisionProvider.class,
                        "debug_", ".png");
                ImageIO.write(debugImage, "PNG", file);
                logger.debug("Debug image filename {}", file);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    enum MinMaxState {
        BEFORE_INFLECTION,
        AFTER_INFLECTION
    }

    static List<Point> matMaxima(Mat mat, double rangeMin, double rangeMax) {
        List<Point> locations = new ArrayList<>();

        int rEnd = mat.rows() - 1;
        int cEnd = mat.cols() - 1;

        // CHECK EACH ROW MAXIMA FOR LOCAL 2D MAXIMA
        for (int r = 0; r <= rEnd; r++) {
            MinMaxState state = MinMaxState.BEFORE_INFLECTION;
            double curVal = mat.get(r, 0)[0];
            for (int c = 1; c <= cEnd; c++) {
                double val = mat.get(r, c)[0];

                if (val == curVal) {
                    continue;
                }
                else if (curVal < val) {
                    if (state == MinMaxState.BEFORE_INFLECTION) {
                        // n/a
                    }
                    else {
                        state = MinMaxState.BEFORE_INFLECTION;
                    }
                }
                else { // curVal > val
                    if (state == MinMaxState.BEFORE_INFLECTION) {
                        if (rangeMin <= curVal && curVal <= rangeMax) { // ROW
                                                                        // MAXIMA
                            if (0 < r && (mat.get(r - 1, c - 1)[0] >= curVal
                                    || mat.get(r - 1, c)[0] >= curVal)) {
                                // cout << "reject:r-1 " << r << "," << c-1 <<
                                // endl;
                                // - x x
                                // - - -
                                // - - -
                            }
                            else if (r < rEnd && (mat.get(r + 1, c - 1)[0] > curVal
                                    || mat.get(r + 1, c)[0] > curVal)) {
                                // cout << "reject:r+1 " << r << "," << c-1 <<
                                // endl;
                                // - - -
                                // - - -
                                // - x x
                            }
                            else if (1 < c && (0 < r && mat.get(r - 1, c - 2)[0] >= curVal
                                    || mat.get(r, c - 2)[0] > curVal
                                    || r < rEnd && mat.get(r + 1, c - 2)[0] > curVal)) {
                                // cout << "reject:c-2 " << r << "," << c-1 <<
                                // endl;
                                // x - -
                                // x - -
                                // x - -
                            }
                            else {
                                locations.add(new Point(c - 1, r));
                            }
                        }
                        state = MinMaxState.AFTER_INFLECTION;
                    }
                    else {
                        // n/a
                    }
                }

                curVal = val;
            }

            // PROCESS END OF ROW
            if (state == MinMaxState.BEFORE_INFLECTION) {
                if (rangeMin <= curVal && curVal <= rangeMax) { // ROW MAXIMA
                    if (0 < r && (mat.get(r - 1, cEnd - 1)[0] >= curVal
                            || mat.get(r - 1, cEnd)[0] >= curVal)) {
                        // cout << "rejectEnd:r-1 " << r << "," << cEnd-1 <<
                        // endl;
                        // - x x
                        // - - -
                        // - - -
                    }
                    else if (r < rEnd && (mat.get(r + 1, cEnd - 1)[0] > curVal
                            || mat.get(r + 1, cEnd)[0] > curVal)) {
                        // cout << "rejectEnd:r+1 " << r << "," << cEnd-1 <<
                        // endl;
                        // - - -
                        // - - -
                        // - x x
                    }
                    else if (1 < r && mat.get(r - 1, cEnd - 2)[0] >= curVal
                            || mat.get(r, cEnd - 2)[0] > curVal
                            || r < rEnd && mat.get(r + 1, cEnd - 2)[0] > curVal) {
                        // cout << "rejectEnd:cEnd-2 " << r << "," << cEnd-1 <<
                        // endl;
                        // x - -
                        // x - -
                        // x - -
                    }
                    else {
                        locations.add(new Point(cEnd, r));
                    }
                }
            }
        }

        return locations;
    }
}
