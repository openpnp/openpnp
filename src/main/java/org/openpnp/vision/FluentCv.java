package org.openpnp.vision;


import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.imageio.ImageIO;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
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
 *  Heavily influenced by FireSight by Karl Lew
 *  https://github.com/firepick1/FireSight
 *  
 *  TODO: Rethink operations that return or process data points versus
 *  images. Perhaps these should require a tag to work with and
 *  leave the image unchanged.
 */
public class FluentCv {
    static {
        nu.pattern.OpenCV.loadShared();
        System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
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
    		double radius = circle[2];
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
		// fitLine doesn't like when you give it a zero length array, so just
		// bail if there are not enough points to make a line.
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
    	
    	long t = System.currentTimeMillis();
		// match the points to a line
    	MatOfPoint pointsMat = new MatOfPoint();
		pointsMat.fromList(points);
		Mat line = new Mat();
		Imgproc.fitLine(
				pointsMat, 
				line, 
				Imgproc.CV_DIST_HUBER, 
				0, 
				0.01, 
				0.01);
		float vx = (float) line.get(0, 0)[0]; 
		float vy = (float) line.get(1, 0)[0]; 
		float x = (float) line.get(2, 0)[0]; 
		float y = (float) line.get(3, 0)[0];
		Point a = new Point(x, y);
		Point b = new Point(x + vx, y + vy);
		System.out.println(System.currentTimeMillis() - t);
		
    	t = System.currentTimeMillis();
		Point[] lineRansac = ransac(points, 100, maxDistance);
    	a = lineRansac[0];
    	b = lineRansac[1];
		System.out.println(System.currentTimeMillis() - t);
		System.out.println();
		
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

	/*
	 * http://users.utcluj.ro/~igiosan/Resources/PRS/L1/lab_01e.pdf
	 * http://cs.gmu.edu/~kosecka/cs682/lect-fitting.pdf
	 * http://introcs.cs.princeton.edu/java/36inheritance/LeastSquares.java.html
	 */
	public static Point[] ransac(List<Point> points, int maxIterations, double threshold) {
		Point bestA = null, bestB = null;
		int bestInliers = 0;
		for (int i = 0; i < maxIterations; i++) {
			// take a random sample of two points
			Collections.shuffle(points);
			Point a = points.get(0);
			Point b = points.get(1);
			// find the inliers
			int inliers = 0;
			for (Point p : points) {
				double distance = pointToLineDistance(a, b, p);
				if (distance <= threshold) {
					inliers++;
				}
			}
			if (inliers > bestInliers) {
				bestA = a;
				bestB = b;
				bestInliers = inliers;
			}
		}
		return new Point[] { bestA, bestB };
	}
	
	// TODO: This currently seems to give much worse results than ransac. Figure out why.
	public static List<RansacLine> multiRansac(List<Point> points, int maxIterations, double threshold) {
		Random random = new Random();
		Set<RansacLine> lines = new HashSet<>();
		for (int i = 0; i < maxIterations; i++) {
			// take a random sample of two points
			Point a = points.get(random.nextInt(points.size()));
			Point b = points.get(random.nextInt(points.size()));
			RansacLine line = new RansacLine(a, b, 0);
			// if we have already processed this pair, skip it
			if (lines.contains(line)) {
				continue;
			}
			// add the result
			lines.add(line);
			// find the inliers
			for (Point p : points) {
				double distance = pointToLineDistance(a, b, p);
				if (distance <= threshold) {
					line.inliers++;
				}
			}
		}
		List<RansacLine> results = new ArrayList<>(lines);
		Collections.sort(results, new Comparator<RansacLine>() {
			@Override
			public int compare(RansacLine o1, RansacLine o2) {
				return o2.inliers - o1.inliers;
			}
		});
		return results;
	}
	
	public static class RansacLine {
		public Point a;
		public Point b;
		public transient int inliers;
		
		public RansacLine(Point a, Point b, int inliers) {
			this.a = a;
			this.b = b;
			this.inliers = inliers;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((a == null) ? 0 : a.hashCode());
			result = prime * result + ((b == null) ? 0 : b.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			RansacLine other = (RansacLine) obj;
			if (a == null) {
				if (other.a != null)
					return false;
			} else if (!a.equals(other.a))
				return false;
			if (b == null) {
				if (other.b != null)
					return false;
			} else if (!b.equals(other.b))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "RansacLine [a=" + a + ", b=" + b + ", inliers=" + inliers + "]";
		}
	}
	
    public static void main(String[] args) throws Exception {
    	List<Point> points = new ArrayList<>();
		FluentCv cv = new FluentCv()
			.read(new File("/Users/jason/Desktop/test.png"), "original")
			.toGray()
			.gaussianBlur(9)
			.houghCircles(28, 32, 10, "hough")
			.drawCircles("original", Color.red, "unfiltered")
			.recall("hough")
			.filterCirclesToLine(10)
			.circlesToPoints(points)
			.drawCircles("unfiltered", Color.green)
			.write(new File("/Users/jason/Desktop/test_out.png"));
    }
    
}
