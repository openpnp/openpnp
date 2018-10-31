package org.openpnp.util;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.imageio.ImageIO;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.pmw.tinylog.Logger;

public class OpenCvUtils {


    static {
        nu.pattern.OpenCV.loadShared();
        System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
    }

    public static BufferedImage toBufferedImage(Mat m) {
        Integer type = null;
        if (m.type() == CvType.CV_8UC1) {
            type = BufferedImage.TYPE_BYTE_GRAY;
        }
        else if (m.type() == CvType.CV_8UC3) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        else if (m.type() == CvType.CV_32F) {
            type = BufferedImage.TYPE_BYTE_GRAY;
            Mat tmp = new Mat();
            m.convertTo(tmp, CvType.CV_8UC1, 255);
            // Copy the results into the original Mat and release our temp copy so that when
            // the caller releases the original Mat there is no memory leak.
            tmp.copyTo(m);
            tmp.release();
        }
        if (type == null) {
            throw new Error(String.format("Unsupported Mat: type %d, channels %d, depth %d",
                    m.type(), m.channels(), m.depth()));
        }
        BufferedImage image = new BufferedImage(m.cols(), m.rows(), type);
        m.get(0, 0, ((DataBufferByte) image.getRaster().getDataBuffer()).getData());
        return image;
    }

    public static Mat toMat(BufferedImage img) {
        Integer type = null;
        if (img.getType() == BufferedImage.TYPE_BYTE_GRAY) {
            type = CvType.CV_8UC1;
        }
        else if (img.getType() == BufferedImage.TYPE_3BYTE_BGR) {
            type = CvType.CV_8UC3;
        }
        else {
            img = ImageUtils.convertBufferedImage(img, BufferedImage.TYPE_3BYTE_BGR);
            type = CvType.CV_8UC3;
        }
        Mat mat = new Mat(img.getHeight(), img.getWidth(), type);
        mat.put(0, 0, ((DataBufferByte) img.getRaster().getDataBuffer()).getData());
        return mat;
    }

    /**
     * Finds circles of the given min and maxDiameter, no less than minDistance apart by capturing
     * an image from the given Camera. Results are returned as a List of Location where X and Y
     * represent the center of the circle, Z is that of the Camera and rotation is the diameter of
     * the circle found. Results are returned sorted by distance from the Camera Location in
     * ascending order.
     * 
     * @param camera
     * @param minDiameter
     * @param maxDiameter
     * @param minDistance
     * @return
     * @throws Exception
     */
    public static List<Location> houghCircles(Camera camera, Length minDiameter, Length maxDiameter,
            Length minDistance) throws Exception {
        Logger.debug("houghCircles({}, {}, {}, {})",
                new Object[] {camera.getName(), minDiameter, maxDiameter, minDistance});

        // convert inputs to the same units
        Location unitsPerPixel = camera.getUnitsPerPixel();
        minDiameter = minDiameter.convertToUnits(unitsPerPixel.getUnits());
        maxDiameter = maxDiameter.convertToUnits(unitsPerPixel.getUnits());
        minDistance = minDistance.convertToUnits(unitsPerPixel.getUnits());

        // we average the units per pixel because circles can't be ovals
        double avgUnitsPerPixel = (unitsPerPixel.getX() + unitsPerPixel.getY()) / 2;

        // convert it all to pixels
        double minDiameterPixels = minDiameter.getValue() / avgUnitsPerPixel;
        double maxDiameterPixels = maxDiameter.getValue() / avgUnitsPerPixel;
        double minDistancePixels = minDistance.getValue() / avgUnitsPerPixel;

        BufferedImage image = camera.capture();
        Mat mat = toMat(image);
        Mat circles = houghCircles(mat, minDiameterPixels, maxDiameterPixels, minDistancePixels);

        // convert the results into Locations
        List<Location> locations = new ArrayList<>();
        for (int i = 0; i < circles.cols(); i++) {
            double[] circle = circles.get(0, i);
            double x = circle[0];
            double y = circle[1];
            double radius = circle[2];
            Location location = VisionUtils.getPixelLocation(camera, x, y);
            location = location.derive(null, null, null, radius * 2 * avgUnitsPerPixel);
            locations.add(location);
        }

        // sort by distance from center
        locations = VisionUtils.sortLocationsByDistance(camera.getLocation(), locations);
        return locations;
    }

    public static Mat houghCircles(Mat mat, double minDiameter, double maxDiameter,
            double minDistance) {
        Logger.debug("houghCircles(Mat, {}, {}, {})",
                new Object[] {minDiameter, maxDiameter, minDistance});

        saveDebugImage(OpenCvUtils.class, "houghCircles", "input", mat);

        // save a copy of the image for debugging
        Mat debug = mat.clone();

        // hough requires grayscale images
        mat = toGray(mat);

        // and prefers a blurred image
        mat = gaussianBlur(mat, 9);

        // run the houghcircles algorithm
        Mat circles = new Mat();
        Imgproc.HoughCircles(mat, circles, Imgproc.CV_HOUGH_GRADIENT, 1, minDistance, 80, 10,
                (int) (minDiameter / 2), (int) (maxDiameter / 2));

        if (LogUtils.isDebugEnabled()) {
            drawCircles(debug, circles);
            saveDebugImage(OpenCvUtils.class, "houghCircles", "debug", debug);
        }

        saveDebugImage(OpenCvUtils.class, "houghCircles", "output", mat);

        return circles;
    }

    /**
     * Convert the given Mat to grayscale. Conversion is done in place and if the Mat is already
     * grayscale nothing is done.
     * 
     * @param mat
     * @return
     */
    public static Mat toGray(Mat mat) {
        if (mat.channels() == 1) {
            return mat;
        }
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);
        return mat;
    }

    /**
     * Perform an in place gaussian blur on the given Mat with a kernel of size kernel x kernel.
     * 
     * @param mat
     * @param kernel
     * @return
     */
    public static Mat gaussianBlur(Mat mat, int kernel) {
        Imgproc.GaussianBlur(mat, mat, new Size(kernel, kernel), 0);
        return mat;
    }

    public static Mat drawCircles(Mat mat, Mat circles) {
        for (int i = 0; i < circles.cols(); i++) {
            double[] circle = circles.get(0, i);
            double x = circle[0];
            double y = circle[1];
            double radius = circle[2];
            Core.circle(mat, new Point(x, y), (int) radius, new Scalar(0, 0, 255, 255), 2);
            Core.circle(mat, new Point(x, y), 1, new Scalar(0, 255, 0, 255), 2);
        }
        return mat;
    }

    public static Mat thresholdAdaptive(Mat mat, boolean invert) {
        Imgproc.adaptiveThreshold(mat, mat, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C,
                invert ? Imgproc.THRESH_BINARY_INV : Imgproc.THRESH_BINARY, 3, 5);
        return mat;
    }

    public static Mat thresholdOtsu(Mat mat, boolean invert) {
        Imgproc.threshold(mat, mat, 0, 255,
                (invert ? Imgproc.THRESH_BINARY_INV : Imgproc.THRESH_BINARY) | Imgproc.THRESH_OTSU);
        return mat;
    }
    
    public synchronized static void saveDebugImage(Class implementationClass, String function, String identifier, BufferedImage img) {
        if (img == null) {
            return;
        }
        if (LogUtils.isDebugEnabled()) {
            try {
                File file = new File(Configuration.get().getConfigurationDirectory(), "log");
                file = new File(file, "vision");
                file.mkdirs();
                DateFormat df = new SimpleDateFormat("YYYY-MM-dd'T'HH.mm.ss.SSS");
                file = new File(file, String.format("%s_%s_%s_%s.png", 
                        implementationClass.getSimpleName(), 
                        function, 
                        df.format(new Date()), 
                        identifier));
                ImageIO.write(img, "PNG", file);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void saveDebugImage(Class implementationClass, String function, String identifier, Mat mat) {
        if (mat == null) {
            return;
        }
        saveDebugImage(implementationClass, function, identifier, OpenCvUtils.toBufferedImage(mat));
    }
    
    private enum MinMaxState {
        BEFORE_INFLECTION,
        AFTER_INFLECTION
    }

    /**
     * Ported from the C++ version in FireSight by Karl Lew, which is licensed under the 
     * MIT license.
     * https://github.com/firepick1/FireSight
     * @param mat
     * @param rangeMin
     * @param rangeMax
     * @return
     */
    public static List<java.awt.Point> matMaxima(Mat mat, double rangeMin, double rangeMax) {
        List<java.awt.Point> locations = new ArrayList<>();

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
                                locations.add(new java.awt.Point(c - 1, r));
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
                        locations.add(new java.awt.Point(cEnd, r));
                    }
                }
            }
        }

        return locations;
    }    
}
