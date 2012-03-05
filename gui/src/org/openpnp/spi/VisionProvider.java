package org.openpnp.spi;

/**
 * Provides an interface for implementors of vision systems to implement. A
 * VisionProvider is attached to a Camera in configuration and can be commanded
 * by the system to perform a variety of vision tasks.
 */
public interface VisionProvider {
	/**
	 * Sets the Camera that the VisionProvider should use for image capture.
	 * This is called during setup and will only be called once.
	 * 
	 * @param camera
	 */
	public void setCamera(Camera camera);

	// TODO: decide if results are measured from top or bottom left and
	// standardize on it
	public Circle[] locateCircles(int roiX1, int roiY1, int roiX2, int roiY2,
			int poiX, int poyY, int minimumDiameter, int diameter,
			int maximumDiameter) throws Exception;

	public class Circle {
		private double x;
		private double y;
		private double diameter;
		
		public Circle(double x, double y, double diameter) {
			this.x = x;
			this.y = y;
			this.diameter = diameter;
		}

		public double getX() {
			return x;
		}

		public void setX(double x) {
			this.x = x;
		}

		public double getY() {
			return y;
		}

		public void setY(double y) {
			this.y = y;
		}

		public double getDiameter() {
			return diameter;
		}

		public void setDiameter(double diameter) {
			this.diameter = diameter;
		}
	}
}
