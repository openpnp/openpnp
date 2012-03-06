package org.openpnp.machine.reference.vision;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.openpnp.CameraListener;
import org.openpnp.machine.reference.vision.roborealm.RoboRealm;
import org.openpnp.spi.Camera;
import org.openpnp.spi.VisionProvider;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.core.Commit;

/*
 * TODO: Optimization: Once we need more than one program maybe we can settle
 * on a single program that does blobs and circles. Add multiple filters
 * to the program and have them each use the source. Since they update
 * different vars it should work. Maybe.
 */
public class RoboRealmVisionProvider implements VisionProvider, CameraListener {
	private static Map<String, RoboRealm> roboRealms = new HashMap<String, RoboRealm>();

	@Attribute
	private String host;
	@Attribute
	private int port;
	
	@Attribute(required=false)
	private int autoCaptureFps;

	private Camera camera;
	private RoboRealm roboRealm;

	private CirclesProgram circlesProgram = new CirclesProgram(3, 100);

	private RoboRealmProgram lastProgram;
	
	private Executor executor = Executors.newSingleThreadExecutor();

	@Commit
	private void commit() {
		synchronized (roboRealms) {
			roboRealm = roboRealms.get(host + ":" + port);
			if (roboRealm == null) {
				roboRealm = new RoboRealm(host, port);
				roboRealms.put(host + ":" + port, roboRealm);
			}
		}
	}

	@Override
	public void setCamera(Camera camera) {
		if (this.camera != null) {
			throw new Error("Camera already set!");
		}
		this.camera = camera;
		if (autoCaptureFps != 0) {
			camera.startContinuousCapture(this, autoCaptureFps);
		}
	}

	@Override
	public Circle[] locateCircles(int roiX1, int roiY1, int roiX2, int roiY2,
			int poiX, int poyY, int minimumDiameter, int diameter,
			int maximumDiameter) throws Exception {
		Map<String, String> variables;
		int width, height;
		// TODO need error checking on RoboRealm, need to be able to determine
		// if we didn't find anything or if it's down
		synchronized (roboRealm) {
			boolean result;
			if (lastProgram != circlesProgram) {
				circlesProgram.setMinimumRadius(minimumDiameter / 2);
				circlesProgram.setMaximumRadius(maximumDiameter / 2);
				result = roboRealm.execute(circlesProgram.toString()); 
				if (!result) {
					throw new Exception("Failed to execute program on RoboRealm. Is it running?");
				}
				lastProgram = circlesProgram;
			}
			else {
				if (circlesProgram.getMinimumRadius() != (minimumDiameter / 2)) {
					circlesProgram.setMinimumRadius(minimumDiameter / 2);
					result = roboRealm.setParameter("Circles", 0, "min_radius", ""
							+ circlesProgram.getMinimumRadius());
					if (!result) {
						throw new Exception("Failed to set parameter on RoboRealm. Is it running?");
					}
				}

				if (circlesProgram.getMaximumRadius() != (maximumDiameter / 2)) {
					circlesProgram.setMaximumRadius(maximumDiameter / 2);
					result = roboRealm.setParameter("Circles", 0, "max_radius", ""
							+ circlesProgram.getMaximumRadius());
					if (!result) {
						throw new Exception("Failed to set parameter on RoboRealm. Is it running?");
					}
				}
			}

			BufferedImage image = camera.capture();

			width = image.getWidth();
			height = image.getHeight();
			
			result = roboRealm.setImage(image);
			if (!result) {
				throw new Exception("Failed to set image on RoboRealm. Is it running?");
			}

			variables = roboRealm.getVariables("CIRCLES_COUNT,CIRCLES");
			if (variables == null) {
				throw new Exception("Failed to get variables from RoboRealm. Is it running?");
			}
		}
		String circlesCountVar = variables.get("response.CIRCLES_COUNT");
		String circlesVar = variables.get("response.CIRCLES");
		if (circlesCountVar == null || circlesVar == null) {
			return null;
		}
		int circlesCount = Integer.parseInt(circlesCountVar);
		if (circlesCount == 0) {
			return null;
		}
		String[] circlesVarParts = circlesVar.split(",");
		ArrayList<Circle> circles = new ArrayList<Circle>();
		for (int i = 0; i < circlesCount; i++) {
			int x = Integer.parseInt(circlesVarParts[i * 13 + 0]);
			int y = Integer.parseInt(circlesVarParts[i * 13 + 1]);
			int d = Integer.parseInt(circlesVarParts[i * 13 + 2]) * 2;

			if (roiX1 == -1) {
				roiX1 = 0;
			}
			if (roiY1 == -1) {
				roiY1 = 0;
			}
			if (roiX2 == -1) {
				roiX2 = width;
			}
			if (roiY2 == -1) {
				roiY2 = height;
			}

			// TODO Check ROI

			if (d >= minimumDiameter && d <= maximumDiameter) {
				Circle circle = new Circle(x, y, d);
				circles.add(circle);
			}
		}
		// TODO sort
		return circles.size() > 0 ? circles.toArray(new Circle[] {}) : null;
	}
	
	@Override
	public void frameReceived(final BufferedImage img) {
		executor.execute(new Runnable() {
			public void run() {
				synchronized (roboRealm) {
					roboRealm.setImage(img);
				}
			}
		});
	}

	private interface RoboRealmProgram {

	}

	private static class CirclesProgram implements RoboRealmProgram {
		private static String format = "<head><version>2.44.12</version></head>"
				+ "<Adaptive_Threshold>"
				+ "<mean_offset>5</mean_offset>"
				+ "<filter_size>21</filter_size>"
				+ "<min_threshold>0</min_threshold>"
				+ "<channel_type>1</channel_type>"
				+ "<max_threshold>255</max_threshold>"
				+ "</Adaptive_Threshold>"
				+ "<Canny>"
				+ "<high_threshold>30</high_threshold>"
				+ "<low_threshold>0</low_threshold>"
				+ "<theta>1.0</theta>"
				+ "</Canny>"
				+ "<Circles>"
				+ "<center_color_index>2</center_color_index>"
				+ "<max_radius>%d</max_radius>"
				+ "<min_radius>%d</min_radius>"
				+ "<circle_color_index>5</circle_color_index>"
				+ "<isolation>3</isolation>"
				+ "<fill_circles>TRUE</fill_circles>"
				+ "<overlay_image>Source</overlay_image>"
				+ "<radius_color_index>7</radius_color_index>"
				+ "<statistics_image>Source</statistics_image>"
				+ "<threshold>2</threshold>" + "</Circles>";

		private int minimumRadius;
		private int maximumRadius;

		public CirclesProgram(int minimumRadius, int maximumRadius) {
			this.minimumRadius = minimumRadius;
			this.maximumRadius = maximumRadius;
		}

		public void setMinimumRadius(int minimumRadius) {
			this.minimumRadius = minimumRadius;
		}

		public void setMaximumRadius(int maximumRadius) {
			this.maximumRadius = maximumRadius;
		}

		public int getMinimumRadius() {
			return minimumRadius;
		}

		public int getMaximumRadius() {
			return maximumRadius;
		}

		@Override
		public String toString() {
			return String.format(format, maximumRadius, minimumRadius);
		}
	}
}
