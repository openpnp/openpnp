package org.openpnp.machine.reference.vision;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
public class RoboRealmVisionProvider implements VisionProvider {
	private static Map<String, RoboRealm> roboRealms = new HashMap<String, RoboRealm>();

	@Attribute
	private String host;
	@Attribute
	private int port;

	private Camera camera;
	private RoboRealm roboRealm;

	private CirclesProgram circlesProgram = new CirclesProgram(3, 100);

	private RoboRealmProgram lastProgram;

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
	}

	@Override
	public Circle[] locateCircles(int roiX1, int roiY1, int roiX2, int roiY2,
			int poiX, int poyY, int minimumDiameter, int diameter,
			int maximumDiameter) throws Exception {
		Map<String, String> variables;
		int width, height;
		synchronized (roboRealm) {
			BufferedImage image = camera.capture();

			width = image.getWidth();
			height = image.getHeight();

			boolean executeProgram = false;

			if (circlesProgram.getMinimumRadius() >= (minimumDiameter / 2)
					|| circlesProgram.getMaximumRadius() <= (maximumDiameter / 2)) {
				circlesProgram.setMinimumRadius(minimumDiameter / 2);
				circlesProgram.setMaximumRadius(maximumDiameter / 2);
				executeProgram = true;
			}

			if (lastProgram != circlesProgram) {
				lastProgram = circlesProgram;
				executeProgram = true;
			}

			if (executeProgram) {
				roboRealm.execute(lastProgram.toString());
			}

			roboRealm.setImage(image);

			variables = roboRealm.getVariables(null);
		}
		int circlesCount = Integer.parseInt(variables
				.get("response.CIRCLES_COUNT"));
		String circlesVar = variables.get("response.CIRCLES");
		if (circlesCount == 0 || circlesVar == null) {
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
