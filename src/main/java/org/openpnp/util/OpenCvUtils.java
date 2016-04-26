package org.openpnp.util;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.util.ArrayList;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenCvUtils {
    private final static Logger logger = LoggerFactory.getLogger(OpenCvUtils.class);

    static {
        nu.pattern.OpenCV.loadShared();
        System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
    }

    /**
     * TODO: This probably doesn't work right on submats. Need to test and fix.
     * 
     * @param m
     * @return
     */
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
        logger.debug("houghCircles({}, {}, {}, {})",
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
        logger.debug("houghCircles(Mat, {}, {}, {})",
                new Object[] {minDiameter, maxDiameter, minDistance});

        saveDebugImage("houghCircles_in", mat);

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

        if (logger.isDebugEnabled()) {
            drawCircles(debug, circles);
            saveDebugImage("houghCircles_debug", debug);
        }

        saveDebugImage("houghCircles_out", mat);

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

    public static void saveDebugImage(String name, Mat mat) {
        if (logger.isDebugEnabled()) {
            try {
                BufferedImage debugImage = OpenCvUtils.toBufferedImage(mat);
                File file = Configuration.get().createResourceFile(OpenCvUtils.class, name + "_",
                        ".png");
                ImageIO.write(debugImage, "PNG", file);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
