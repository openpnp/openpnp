package org.openpnp.vision;


import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;

import javax.imageio.ImageIO;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.openpnp.model.Length;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.util.VisionUtils;

/**
 * A fluent API for some of the most commonly used OpenCV primitives.
 * Successive operations modify a running Mat. By specifying a tag on
 * an operation the result of the operation will be stored and can be
 * recalled back into the current Mat.
 * 
 *  Heavily influenced by FireSight by Karl Lew
 *  https://github.com/firepick1/FireSight
 */
public class FluentCv {
    static {
        nu.pattern.OpenCV.loadShared();
        System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
    }

    private LinkedHashMap<String, Mat> stored = new LinkedHashMap<>();
	private Mat mat = new Mat();
	
	public FluentCv toMat(BufferedImage img, String... tag) {
        Integer type = null;
        if (img.getType() == BufferedImage.TYPE_BYTE_GRAY) {
            type = CvType.CV_8UC1;
        }
        else if (img.getType() == BufferedImage.TYPE_3BYTE_BGR) {
            type = CvType.CV_8UC3;
        }
        else {
            img = convertBufferedImage(img, BufferedImage.TYPE_3BYTE_BGR);
            type = CvType.CV_8UC3;
        }
        Mat mat = new Mat(img.getHeight(), img.getWidth(), type);
        mat.put(0, 0, ((DataBufferByte) img.getRaster().getDataBuffer()).getData());
		return store(mat, tag);
	}
	
	public FluentCv toGray(String...tag) {
    	if (mat.channels() == 1) {
    		return this;
    	}
		Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);
		return store(mat, tag);
	}
	
	public FluentCv cvtColor(int code, String... tag) {
		Imgproc.cvtColor(mat, mat, code);
		return store(mat, tag);
	}
	
	public FluentCv thresholdOtsu(boolean invert, String... tag) {
    	Imgproc.threshold(
    			mat, 
    			mat,
    			0,
    			255, 
    			(invert ? Imgproc.THRESH_BINARY_INV : Imgproc.THRESH_BINARY) | Imgproc.THRESH_OTSU);
		return store(mat, tag);
	}
	
	public FluentCv thresholdAdaptive(boolean invert, String...tag) {
    	Imgproc.adaptiveThreshold(
    			mat, 
    			mat, 
    			255, 
    			Imgproc.ADAPTIVE_THRESH_MEAN_C, 
    			invert ? Imgproc.THRESH_BINARY_INV : Imgproc.THRESH_BINARY, 
    			3,
    			5);
		return store(mat, tag);
	}
	
	public FluentCv gaussianBlur(int kernelSize, String... tag) {
    	Imgproc.GaussianBlur(mat, mat, new Size(kernelSize, kernelSize), 0);
		return store(mat, tag);
	}
	
	public FluentCv houghCircles(Camera camera, 
    		Length minDiameter, 
    		Length maxDiameter, 
    		Length minDistance,
    		String... tag) {
        return houghCircles(
        		(int) VisionUtils.toPixels(minDiameter, camera), 
        		(int) VisionUtils.toPixels(maxDiameter, camera), 
        		(int) VisionUtils.toPixels(minDistance, camera),
        		tag);
	}
	
	public FluentCv houghCircles(int minDiameter, int maxDiameter, int minDistance, String... tag) {
    	Mat circles = new Mat();
    	Imgproc.HoughCircles(
    			mat, 
    			circles, 
    			Imgproc.CV_HOUGH_GRADIENT, 
    			1, 
    			minDistance,
    			80, 
    			10, 
    			minDiameter / 2, 
    			maxDiameter / 2);
		store(circles, tag);
		return this;
	}
	
	public FluentCv circlesToLocations(Camera camera, List<Location> locations) {
    	Location unitsPerPixel = camera
    			.getUnitsPerPixel()
    			.convertToUnits(camera.getLocation().getUnits());
    	double avgUnitsPerPixel = (unitsPerPixel.getX() + unitsPerPixel.getY()) / 2;

    	for (int i = 0; i < mat.cols(); i++) {
    		double[] circle = mat.get(0, i);
    		double x = circle[0];
    		double y = circle[1];
    		double radius = circle[2];
            Location location = VisionUtils.getPixelLocation(camera, x, y);
            location = location.derive(null, null, null, radius * 2 * avgUnitsPerPixel);
            locations.add(location); 
    	}
    	
    	VisionUtils.sortLocationsByDistance(camera.getLocation(), locations);
		return this;
	}
	
	public FluentCv drawCircles(String baseTag, String... tag) {
		Mat mat = get(baseTag);
		if (mat == null) {
			mat = new Mat();
		}
    	for (int i = 0; i < this.mat.cols(); i++) {
    		double[] circle = this.mat.get(0, i);
    		double x = circle[0];
    		double y = circle[1];
    		double radius = circle[2];
        	Core.circle(mat, new Point(x, y), (int) radius, new Scalar(0, 0, 255, 255), 2);
        	Core.circle(mat, new Point(x, y), 1, new Scalar(0, 255, 0, 255), 2);
    	}
		return store(mat, tag);
	}
	
	public FluentCv recall(String tag) {
		mat = get(tag);
		return this;
	}
	
	public FluentCv write(File file) throws Exception {
		ImageIO.write(toBufferedImage(), "PNG", file);
		return this;
	}
	
	public FluentCv read(File file, String... tag) throws Exception {
		 return toMat(ImageIO.read(file), tag);
	}
	
	public BufferedImage toBufferedImage() {
        Integer type = null;
        if (mat.type() == CvType.CV_8UC1) {
            type = BufferedImage.TYPE_BYTE_GRAY;
        }
        else if (mat.type() == CvType.CV_8UC3) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        else if (mat.type() == CvType.CV_32F) {
            type = BufferedImage.TYPE_BYTE_GRAY;
            Mat tmp = new Mat();
            mat.convertTo(tmp, CvType.CV_8UC1, 255);
            mat = tmp;
        }
        if (type == null) {
            throw new Error(String.format("Unsupported Mat: type %d, channels %d, depth %d", 
            		mat.type(), 
            		mat.channels(), 
            		mat.depth()));
        }
        BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), type);
        mat.get(0, 0, ((DataBufferByte) image.getRaster().getDataBuffer()).getData());
        return image;
	}
	
	public Mat mat() {
		return mat;
	}
	
	private FluentCv store(Mat mat, String... tag) {
		this.mat = mat;
		if (tag != null && tag.length > 0) {
			// Clone so that future writes to the pipeline Mat
			// don't overwrite our stored one.
			stored.put(tag[0], this.mat.clone());
		}
		return this;
	}
	
	private Mat get(String tag) {
		Mat mat = stored.get(tag);
		if (mat == null) {
			return null;
		}
		// Clone so that future writes to the pipeline Mat
		// don't overwrite our stored one.
		return mat.clone();
	}
	
    private static BufferedImage convertBufferedImage(BufferedImage src, int type) {
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
