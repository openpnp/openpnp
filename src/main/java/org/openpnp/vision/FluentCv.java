/*
 	Copyright (C) 2015 Jason von Nieda <jason@vonnieda.org>
 	
 	This file is part of OpenPnP.
 	
 	You may use this file under either the GPLv3 License or the MIT
 	License at your preference. Functions in OpenPnP that this
 	file rely on are also available under these terms. See the two
 	licenses below.
 	
	GPLv3 License Terms
	-------------------
	
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
 	
 	
 	MIT License Terms
 	-----------------
 	
 	The MIT License (MIT)

	Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:
	
	The above copyright notice and this permission notice shall be included in
	all copies or substantial portions of the Software.
	
	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
	AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
	OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
	THE SOFTWARE.
*/

package org.openpnp.vision;


import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import javax.imageio.ImageIO;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.openpnp.model.Length;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.util.HslColor;
import org.openpnp.util.VisionUtils;

/**
 * A fluent API for some of the most commonly used OpenCV primitives.
 * Successive operations modify a running Mat. By specifying a tag on
 * an operation the result of the operation will be stored and can be
 * recalled back into the current Mat.
 * 
 * Heavily influenced by FireSight by Karl Lew
 * https://github.com/firepick1/FireSight
 * 
 * In the spirit of FireSight, this code is licensed differently from the
 * rest of OpenPnP. Please see the license header above.
 * 
 * WARNING WARNING WARNING WARNING WARNING WARNING WARNING WARNING 
 * This API is still under heavy development and is likely to change
 * significantly in the near future.  
 *  
 * TODO: Rethink operations that return or process data points versus
 * images. Perhaps these should require a tag to work with and
 * leave the image unchanged.
 * 
 * There is a bit of a divergence right now between how things like
 * contours and rotated rects are handled versus circles. Circles
 * already have a Mat representation that we can kind of coerce along
 * the pipeline where contours do not (List<MatOfPoint>). We need to
 * pick one of the methods and stick with it, doing translation where
 * needed.
 * 
 * Keeping things in Mat does give the benefit of not moving too much
 * memory between OpenCv and Java. 
 */
public class FluentCv {
    static {
        nu.pattern.OpenCV.loadShared();
        System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
    }
    
    public enum ColorCode {
    	Bgr2Gray(Imgproc.COLOR_BGR2GRAY),
    	Rgb2Gray(Imgproc.COLOR_RGB2GRAY),
    	Gray2Bgr(Imgproc.COLOR_GRAY2BGR),
    	Gray2Rgb(Imgproc.COLOR_GRAY2RGB);
    	
    	private int code;
    	
    	ColorCode(int code) {
    		this.code = code;
    	}
    	
    	public int getCode() {
    		return code;
    	}
    }

    private LinkedHashMap<String, Mat> stored = new LinkedHashMap<>();
	private Mat mat = new Mat();
	private Camera camera;
	
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
	
	public FluentCv cvtColor(ColorCode code, String... tag) {
		Imgproc.cvtColor(mat, mat, code.getCode());
		return store(mat, tag);
	}
	
	/**
	 * Apply a threshold to the Mat. If the threshold value is 0 then the Otsu
	 * flag will be added and the threshold value ignored. Otsu performs
	 * automatic determination of the threshold value by sampling the
	 * image.
	 * @param threshold
	 * @param tag
	 * @return
	 */
	public FluentCv threshold(double threshold, String... tag) {
		return threshold(threshold, false, tag);
	}
	
	public FluentCv threshold(double threshold, boolean invert, String... tag) {
		int type = invert ? Imgproc.THRESH_BINARY_INV : Imgproc.THRESH_BINARY;
		if (threshold == 0) {
			type |= Imgproc.THRESH_OTSU;
		}
    	Imgproc.threshold(
    			mat, 
    			mat,
    			threshold,
    			255, 
    			type);
		return store(mat, tag);
	}

	public FluentCv thresholdAdaptive(String...tag) {
		return thresholdAdaptive(false, tag);
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
	
	public FluentCv houghCircles( 
    		Length minDiameter, 
    		Length maxDiameter, 
    		Length minDistance,
    		String... tag) {
		checkCamera();
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
	
	public FluentCv circlesToPoints(List<Point> points) {
    	for (int i = 0; i < mat.cols(); i++) {
    		double[] circle = mat.get(0, i);
    		double x = circle[0];
    		double y = circle[1];
    		points.add(new Point(x, y));
    	}
    	return this;
	}
	
	public FluentCv circlesToLocations(List<Location> locations) {
		checkCamera();
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
	
	public FluentCv drawCircles(
			String baseTag, 
			Color color, 
			String... tag) {
		Color centerColor = new HslColor(color).getComplementary();
		Mat mat = get(baseTag);
		if (mat == null) {
			mat = new Mat();
		}
    	for (int i = 0; i < this.mat.cols(); i++) {
    		double[] circle = this.mat.get(0, i);
    		double x = circle[0];
    		double y = circle[1];
    		double radius = circle[2];
        	Core.circle(
        			mat, 
        			new Point(x, y), 
        			(int) radius, 
        			colorToScalar(color), 
        			2);
        	Core.circle(
        			mat, 
        			new Point(x, y), 
        			1, 
        			colorToScalar(centerColor), 
        			2);
    	}
		return store(mat, tag);
		
	}
	
	public FluentCv drawCircles(String baseTag, String... tag) {
		return drawCircles(baseTag, Color.red, tag);
	}
	
	public FluentCv recall(String tag) {
		mat = get(tag);
		return this;
	}
	
	public FluentCv store(String tag) {
		return store(mat, tag);
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

	public FluentCv settleAndCapture(String... tag) {
		checkCamera();
		return toMat(camera.settleAndCapture(), tag);
	}
	
	/**
	 * Set a Camera that can be used for calculations that require a Camera
	 * Location or units per pixel.
	 * @param camera
	 * @return
	 */
	public FluentCv setCamera(Camera camera) {
		this.camera = camera;
		return this;
	}
	
	public FluentCv filterCirclesByDistance(
			Length minDistance,
			Length maxDistance,
			String... tag
			) {
		
		double minDistancePx = VisionUtils.toPixels(minDistance, camera);
		double maxDistancePx = VisionUtils.toPixels(maxDistance, camera);
		return filterCirclesByDistance(
				camera.getWidth() / 2, 
				camera.getHeight() / 2, 
				minDistancePx, 
				maxDistancePx, 
				tag); 
	}
	
	public FluentCv filterCirclesByDistance(
			double originX,
			double originY,
			double minDistance,
			double maxDistance,
			String...tag
			) {
		List<float[]> results = new ArrayList<float[]>();
    	for (int i = 0; i < this.mat.cols(); i++) {
    		float[] circle = new float[3];
    		this.mat.get(0, i, circle);
    		float x = circle[0];
    		float y = circle[1];
    		float radius = circle[2];
    		double distance = Math.sqrt(Math.pow(x - originX, 2) + Math.pow(y - originY, 2));
    		if (distance >= minDistance && distance <= maxDistance) {
    			results.add(new float[] { x, y, radius });
    		}
    	}
    	// It really seems like there must be a better way to do this, but after hours
    	// and hours of trying I can't find one. How the hell do you append an element
    	// of 3 channels to a Mat?!
		Mat r = new Mat(1, results.size(), CvType.CV_32FC3);
		for (int i = 0; i < results.size(); i++) {
			r.put(0, i, results.get(i));
		}
		return store(r, tag);
	}
	
	public FluentCv filterCirclesToLine(Length maxDistance, String... tag) {
		return filterCirclesToLine(VisionUtils.toPixels(maxDistance, camera), tag);
	}
	
	/**
	 * Filter circles as returned from e.g. houghCircles to only those that are within
	 * maxDistance of the best fitting line.
	 * @param tag
	 * @return
	 */
	public FluentCv filterCirclesToLine(double maxDistance, String... tag) {
		if (this.mat.cols() < 2) {
			return store(this.mat, tag);
		}
		
    	List<Point> points = new ArrayList<Point>();
    	// collect the circles into a list of points
    	for (int i = 0; i < this.mat.cols(); i++) {
    		float[] circle = new float[3];
    		this.mat.get(0, i, circle);
    		float x = circle[0];
    		float y = circle[1];
    		points.add(new Point(x, y));
    	}
    	
		Point[] line = Ransac.ransac(points, 100, maxDistance);
    	Point a = line[0];
    	Point b = line[1];
		
    	// filter the points by distance from the resulting line
		List<float[]> results = new ArrayList<float[]>();
		for (int i = 0; i < this.mat.cols(); i++) {
    		float[] circle = new float[3];
    		this.mat.get(0, i, circle);
    		Point p = new Point(circle[0], circle[1]);
    		if (pointToLineDistance(a, b, p) <= maxDistance) {
    			results.add(circle);
    		}
    	}
		
    	// It really seems like there must be a better way to do this, but after hours
    	// and hours of trying I can't find one. How the hell do you append an element
    	// of 3 channels to a Mat?!
		Mat r = new Mat(1, results.size(), CvType.CV_32FC3);
		for (int i = 0; i < results.size(); i++) {
			r.put(0, i, results.get(i));
		}
		return store(r, tag);
	}
	
	public Mat mat() {
		return mat;
	}
	
	public FluentCv mat(Mat mat, String... tag) {
		return store(mat, tag);
	}
	
	/**
	 * Calculate the absolute difference between the previously
	 * stored Mat called source1 and the current Mat.
	 * @param source1
	 * @param tag
	 */
	public FluentCv absDiff(String source1, String... tag) {
		Core.absdiff(get(source1), mat, mat);
		return store(mat, tag);
	}
	
	public FluentCv canny(double threshold1, double threshold2, String... tag) {
		Imgproc.Canny(mat, mat, threshold1, threshold2);
		return store(mat, tag);
	}
	
	public FluentCv findContours(List<MatOfPoint> contours, String... tag) {
		Mat hierarchy = new Mat();
		Imgproc.findContours(mat, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);
		return store(mat, tag);
	}
	
	public FluentCv drawContours(List<MatOfPoint> contours, Color color, int thickness, String... tag) {
		if (color == null) {
			for (int i = 0; i < contours.size(); i++) {
				Imgproc.drawContours(mat, contours, i, colorToScalar(indexedColor(i)), thickness);
			}
		}
		else {
			Imgproc.drawContours(mat, contours, -1, colorToScalar(color), thickness);
		}
		return store(mat, tag);
	}
	
	public FluentCv filterContoursByArea(List<MatOfPoint> contours, double areaMin, double areaMax) {
		for (Iterator<MatOfPoint> i = contours.iterator(); i.hasNext(); ) {
			MatOfPoint contour = i.next();
			double area = Imgproc.contourArea(contour);
			if (area < areaMin || area > areaMax) {
				i.remove();
			}
		}
		return this;
	}
	
	public FluentCv drawRects(List<RotatedRect> rects, Color color, int thickness, String... tag) {
		for (int i = 0; i < rects.size(); i++) {
			RotatedRect rect = rects.get(i);
			if (color == null) {
				drawRotatedRect(mat, rect, indexedColor(i), thickness);
			}
			else {
				drawRotatedRect(mat, rect, color, thickness);
			}
		}
		return store(mat, tag);
	}
	
	public FluentCv getContourRects(List<MatOfPoint> contours, List<RotatedRect> rects) {
		for (int i = 0; i < contours.size(); i++) {
			MatOfPoint2f contour_ = new MatOfPoint2f();
		    contours.get(i).convertTo(contour_, CvType.CV_32FC2);
			RotatedRect rect = Imgproc.minAreaRect(contour_);
			rects.add(rect);
		}
		return this;
	}
	
	public FluentCv filterRectsByArea(List<RotatedRect> rects, double areaMin, double areaMax) {
		for (Iterator<RotatedRect> i = rects.iterator(); i.hasNext(); ) {
			RotatedRect rect = i.next();
			double area = rect.size.width * rect.size.height;
			if (area < areaMin || area > areaMax) {
				i.remove();
			}
		}
		return this;
	}
	
	private void checkCamera() {
		if (camera == null) {
			throw new Error("Call setCamera(Camera) before calling methods that rely on units per pixel.");
		}
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
	
	private static Scalar colorToScalar(Color color) {
		return new Scalar(
				color.getBlue(), 
				color.getGreen(), 
				color.getRed(), 
				255);
	}
	
	/**
	 * Return a Color from an imaginary list of colors starting at index 0
	 * and extending on to Integer.MAX_VALUE. Can be used to pick a different
	 * color for each object in a list. Colors are not guaranteed to be unique
	 * but successive colors will be significantly different from each other.
	 * @param i
	 * @return
	 */
	private static Color indexedColor(int i) {
		float h = (i * 59) % 360;
		float s = Math.max((i * i) % 100, 80);
		float l = Math.max((i * i) % 100, 50);
		Color color = new HslColor(h, s, l).getRGB();
		return color;
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
    
	// From http://www.ahristov.com/tutorial/geometry-games/point-line-distance.html
    public static double pointToLineDistance(Point A, Point B, Point P) {
		double normalLength = Math.sqrt((B.x - A.x) * (B.x - A.x) + (B.y - A.y) * (B.y - A.y));
		return Math.abs((P.x - A.x) * (B.y - A.y) - (P.y - A.y) * (B.x - A.x)) / normalLength;
	}
	
	/**
	 * Draw the infinite line defined by the two points to the extents
	 * of the image instead of just between the two points.
	 * From: http://stackoverflow.com/questions/13160722/how-to-draw-line-not-line-segment-opencv-2-4-2
	 * @param img
	 * @param p1
	 * @param p2
	 * @param color
	 */
	private static void infiniteLine(Mat img, Point p1, Point p2, Color color) {
		Point p = new Point(), q = new Point();
		// Check if the line is a vertical line because vertical lines don't
		// have slope
		if (p1.x != p2.x) {
			p.x = 0;
			q.x = img.cols();
			// Slope equation (y1 - y2) / (x1 - x2)
			float m = (float) ((p1.y - p2.y) / (p1.x - p2.x));
			// Line equation: y = mx + b
			float b = (float) (p1.y - (m * p1.x));
			p.y = m * p.x + b;
			q.y = m * q.x + b;
		} else {
			p.x = q.x = p2.x;
			p.y = 0;
			q.y = img.rows();
		}
		Core.line(img, p, q, colorToScalar(color));
	}
	
	private static void drawRotatedRect(Mat mat, RotatedRect rect, Color color, int thickness) {
		// From: http://stackoverflow.com/questions/23327502/opencv-how-to-draw-minarearect-in-java
		Point points[] = new Point[4];
	    rect.points(points);
	    Scalar color_ = colorToScalar(color);
	    for(int j = 0; j < 4; ++j) {
	        Core.line(mat, points[j], points[(j + 1) % 4], color_, thickness);
	    }		
	}
}
