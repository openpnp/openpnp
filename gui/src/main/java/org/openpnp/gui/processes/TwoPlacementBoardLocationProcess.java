package org.openpnp.gui.processes;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.openpnp.gui.JobPanel;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.model.Placement;
import org.openpnp.model.Point;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.util.Utils2D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Guides the user through the two point board location operation using
 * step by step instructions.
 * 
 * TODO: Select the right camera on startup and then disable the CameraPanel while active.
 * TODO: Disable the BoardLocation table while active.
 */
public class TwoPlacementBoardLocationProcess {
	private static final Logger logger = LoggerFactory.getLogger(TwoPlacementBoardLocationProcess.class);
	
	private final MainFrame mainFrame;
	private final JobPanel jobPanel;
	
	private int step = -1;
	private String[] instructions = new String[] {
		"<html><body>Move the camera's crosshairs to the center of an easily identifiable placement near the top left corner of the board. Click Next to continue.</body></html>",			
		"<html><body>Now select the placement you have targetted from the table below. Click Next to continue.</body></html>",			
		"<html><body>Next, move the camera's crosshairs to the center of a second placement near the bottom right corner of the board. Click Next to continue.</body></html>",			
		"<html><body>And select the placement you have targetted from the table below. Click Next to continue.</body></html>",
		"<html><body>The board's location and rotation has been set. Click Finish to position the camera at the board's origin, or Cancel to quit.</body></html>",
	};
	
	private Placement placementA, placementB;
	private Location placementLocationA, placementLocationB;
	
	public TwoPlacementBoardLocationProcess(MainFrame mainFrame, JobPanel jobPanel) {
		this.mainFrame = mainFrame;
		this.jobPanel = jobPanel;
		advance();
	}
	
	private void advance() {
		boolean stepResult = true;
		if (step == 0) {
			stepResult = step1();
		}
		else if (step == 1) {
			stepResult = step2();
		}
		else if (step == 2) {
			stepResult = step3();
		}
		else if (step == 3) {
			stepResult = step4();
		}
		else if (step == 4) {
			stepResult = step5();
		}
		if (!stepResult) {
			return;
		}
		step++;
		if (step == 5) {
			mainFrame.hideInstructions();
		}
		else {
			String title = String.format("Set Board Location (%d / 5)", step + 1);
			mainFrame.showInstructions(
				title,
				instructions[step], 
				true, 
				true, 
				step == 4 ? "Finish" : "Next",
				cancelActionListener,
				proceedActionListener);
		}
	}
	
	private boolean step1() {
		placementLocationA = MainFrame.machineControlsPanel.getCameraLocation();
		if (placementLocationA == null) {
			MessageBoxes.errorBox(mainFrame, "Error", "Please position the camera.");
			return false;
		}
		return true;
	}
	
	private boolean step2() {
		placementA = jobPanel.getSelectedPlacement();
		if (placementA == null) {
			MessageBoxes.errorBox(mainFrame, "Error", "Please select a placement.");
			return false;
		}
		return true;
	}
	
	private boolean step3() {
		placementLocationB = MainFrame.machineControlsPanel.getCameraLocation();
		if (placementLocationB == null) {
			MessageBoxes.errorBox(mainFrame, "Error", "Please position the camera.");
			return false;
		}
		return true;
	}
	
	private boolean step4() {
		placementB = jobPanel.getSelectedPlacement();
		if (placementB == null || placementB == placementA) {
			MessageBoxes.errorBox(mainFrame, "Error", "Please select a second placement.");
			return false;
		}
		
		// Get the Locations we'll be using and convert to system units.
		Location boardLocationA = placementLocationA.convertToUnits(Configuration.get().getSystemUnits());
		Location placementLocationA = placementA.getLocation().convertToUnits(Configuration.get().getSystemUnits());
		Location boardLocationB = placementLocationB.convertToUnits(Configuration.get().getSystemUnits());
		Location placementLocationB = placementB.getLocation().convertToUnits(Configuration.get().getSystemUnits());
	
		logger.debug(String.format("locate"));
		logger.debug(String.format("%s - %s", boardLocationA,
				placementLocationA));
		logger.debug(String.format("%s - %s", boardLocationB,
				placementLocationB));
	
		// Calculate the expected angle between the two coordinates, based
		// on their locations in the placement.
		double x1 = placementLocationA.getX();
		double y1 = placementLocationA.getY();
		double x2 = placementLocationB.getX();
		double y2 = placementLocationB.getY();
		double expectedAngle = Math.atan2(y1 - y2, x1 - x2);
		expectedAngle = Math.toDegrees(expectedAngle);
		logger.debug("expectedAngle " + expectedAngle);
	
		// Then calculate the actual angle between the two coordinates,
		// based on the captured values.
		x1 = boardLocationA.getX();
		y1 = boardLocationA.getY();
		x2 = boardLocationB.getX();
		y2 = boardLocationB.getY();
		double indicatedAngle = Math.atan2(y1 - y2, x1 - x2);
		indicatedAngle = Math.toDegrees(indicatedAngle);
		logger.debug("indicatedAngle " + indicatedAngle);
	
		// Subtract the difference and we have the angle that the board
		// is rotated by.
		double angle = indicatedAngle - expectedAngle;
		logger.debug("angle " + angle);
	
		// Now we want to derive the position of 0,0 in relation to the
		// two captured coordinates. We will use the intersection of two
		// circles centered at the coordinates with a radius of the
		// distance from each coordinate to 0,0.
	
		// Circle intersection solver borrowed from
		// http://www.vb-helper.com/howto_circle_circle_intersection.html
	
		// Get the two circles center points and radius.
		double cx0 = boardLocationA.getX();
		double cy0 = boardLocationA.getY();
		double radius0 = Math.sqrt(Math.pow(placementLocationA.getX(), 2)
				+ Math.pow(placementLocationA.getY(), 2));
	
		double cx1 = boardLocationB.getX();
		double cy1 = boardLocationB.getY();
		double radius1 = Math.sqrt(Math.pow(placementLocationB.getX(), 2)
				+ Math.pow(placementLocationB.getY(), 2));
	
		logger.debug(String.format("%f %f %f %f %f %f", cx0, cy0, radius0,
				cx1, cy1, radius1));
	
		// Calculate the distance between the two center points.
		double dx = cx0 - cx1;
		double dy = cy0 - cy1;
		double dist = Math.sqrt(dx * dx + dy * dy);
	
		double a = (radius0 * radius0 - radius1 * radius1 + dist * dist)
				/ (2 * dist);
		double h = Math.sqrt(radius0 * radius0 - a * a);
	
		double cx2 = cx0 + a * (cx1 - cx0) / dist;
		double cy2 = cy0 + a * (cy1 - cy0) / dist;
	
		double intersectionx1 = cx2 + h * (cy1 - cy0) / dist;
		double intersectiony1 = cy2 - h * (cx1 - cx0) / dist;
		double intersectionx2 = cx2 - h * (cy1 - cy0) / dist;
		double intersectiony2 = cy2 + h * (cx1 - cx0) / dist;
	
		// We now have the locations of the two intersecting points on
		// the circles. Now we have to figure out which one is correct.
		Point p0 = new Point(intersectionx1, intersectiony1);
		Point p1 = new Point(intersectionx2, intersectiony2);
	
		logger.debug(String.format("p0 = %s, p1 = %s", p0, p1));
	
		// Create two points based on the boardLocationA.
		Point p0r = new Point(boardLocationA.getX(), boardLocationA.getY());
		Point p1r = new Point(boardLocationA.getX(), boardLocationA.getY());
	
		// Translate each point by one of the results from the circle
		// intersection
		p0r = Utils2D.translatePoint(p0r, p0.getX() * -1, p0.getY() * -1);
		p1r = Utils2D.translatePoint(p1r, p1.getX() * -1, p1.getY() * -1);
	
		// Rotate each point by the negative of the angle previously
		// calculated. This effectively de-rotates the point with one of the
		// results as the origin.
		p0r = Utils2D.rotatePoint(p0r, angle * -1);
		p1r = Utils2D.rotatePoint(p1r, angle * -1);
	
		logger.debug(String.format("p0r = %s, p1r = %s", p0r, p1r));
	
		// Now, whichever result is closer to the value of boardLocationA
		// is the right result. So, calculate the linear distance between
		// the calculated point and the placementLocationA.
		double d0 = Math.abs(Math.sqrt(Math.pow(
				p0r.x - placementLocationA.getX(), 2)
				+ Math.pow(p0r.y - placementLocationA.getY(), 2)));
		double d1 = Math.abs(Math.sqrt(Math.pow(
				p1r.x - placementLocationA.getX(), 2)
				+ Math.pow(p1r.y - placementLocationA.getY(), 2)));
	
		logger.debug(String.format("d0 %f, d1 %f", d0, d1));
	
		Point result = ((d0 < d1) ? p0 : p1);
	
		logger.debug("Result: " + result);
	
		Location boardLocation = new Location(Configuration.get()
				.getSystemUnits(), result.x, result.y, 0, angle * -1);

		jobPanel.getSelectedBoardLocation().setLocation(boardLocation);
		jobPanel.refreshSelectedBoardRow();
		
		return true;
	}
	
	private boolean step5() {
		MainFrame.machineControlsPanel.submitMachineTask(new Runnable() {
			public void run() {
				Head head = Configuration.get().getMachine().getHeads().get(0);
				try {
					Camera camera = MainFrame.cameraPanel
							.getSelectedCamera();
					Location location = jobPanel.getSelectedBoardLocation()
							.getLocation();
					location = location.convertToUnits(Configuration.get().getMachine().getNativeUnits());
					location = location.subtract(camera.getLocation());
					head.moveToSafeZ();
					// Move the head to the location at Safe-Z
					head.moveTo(location.getX(), location.getY(),
							head.getZ(), location.getRotation());
					// Move Z
					head.moveTo(head.getX(), head.getY(), location.getZ(),
							head.getC());
				}
				catch (Exception e) {
					MessageBoxes.errorBox(mainFrame,
							"Move Error", e);
				}
			}
		});
		
		return true;
	}
	
	private void cancel() {
		mainFrame.hideInstructions();
	}
	
	private final ActionListener proceedActionListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			advance();
		}
	};
	
	private final ActionListener cancelActionListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			cancel();
		}
	};
}
