/*
 	Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 	
 	This file is part of OpenPnP.
 	
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
 */

package org.openpnp.machine.reference;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;

import org.openpnp.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;
import org.openpnp.spi.VisionProvider;
import org.openpnp.spi.VisionProvider.Circle;
import org.openpnp.util.LengthUtil;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

public class ReferenceHead implements Head {
	public static final String PIN_ACTUATOR_NAME = "Pin";

	private ReferenceMachine machine;
	private double x, y, z, c;
	private double offsetX, offsetY, offsetZ, offsetC;

	@Attribute
	private String id;
	@Attribute
	private int pickDwellMilliseconds;
	@Attribute(required = false)
	private int placeDwellMilliseconds;
	@Attribute
	private double feedRate;
	@Element
	private SoftLimits softLimits = new SoftLimits();
	@Element(required=false)
	private Homing homing = new Homing();
	
	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setReferenceMachine(ReferenceMachine machine) throws Exception {
		this.machine = machine;
	}

	@Override
	public void home() throws Exception {
		if (homing.useVision) {
			homeWithVision();
		}
		else {
			moveTo(getX(), getY(), 0, getC());
			moveTo(0, 0, 0, 0);
		}
	}
	
	private void homeWithVision() throws Exception {
		for (int i = 0; i < 8; i++) {
			double x = getX();
			double y = getY();
			
			Camera camera = attemptHomeWithVision();
			
			if (x == getX() && y == getY()) {
				Location cameraOffsets = camera.getLocation().convertToUnits(machine.getNativeUnits());
				moveTo(getX() + cameraOffsets.getX(), getY() + cameraOffsets.getY(), getZ(), getC());
				return;
			}
			
			// Give the head a moment to settle before trying again
			Thread.sleep(200);
		}
		
		throw new Exception("Unable to settle after 8 tries. Giving up.");
	}
	
	private Camera attemptHomeWithVision() throws Exception {
		// TODO: To to the defined dot location before starting search 
		
		// find the Camera to be used for homing
		Camera camera = null;
		for (Camera c : machine.getCameras()) {
			if (c.getHead() == this && c.getVisionProvider() != null) {
				camera = c;
			}
		}

		if (camera == null) {
			throw new Exception("No homing camera found");
		}

		VisionProvider vision = camera.getVisionProvider();

		/*
		 * Start looking for the homing dot. We first try to find an exact
		 * match. If one is not found we expand the search until we either find
		 * the dot or we go past the allowable range.
		 */
		boolean found = false;
		double diameter = homing.homingDotDiameter;
		double minimumDiameter = diameter;
		double maximumDiameter = diameter;
		double minimumAllowableDiameter = diameter - (diameter * 0.10);
		double maximumAllowableDiameter = diameter + (diameter * 0.10);
		double diameterIncrement = 0.05;
		Location unitsPerPixel = camera.getUnitsPerPixel().convertToUnits(
				machine.getNativeUnits());
		int diameterPixels = (int) (diameter / unitsPerPixel.getX());
		while (!found) {
			// calculate the minimum and maximum diameter in pixels
			int minimumDiameterPixels = (int) (minimumDiameter / unitsPerPixel
					.getX());
			int maximumDiameterPixels = (int) (maximumDiameter / unitsPerPixel
					.getX());
			// search for circles that match
			Circle[] circles = vision.locateCircles(-1, -1, -1, -1, -1, -1,
					minimumDiameterPixels, diameterPixels,
					maximumDiameterPixels);
			// if no circles were found we'll expand the search
			if (circles == null || circles.length == 0) {
				minimumDiameter -= diameterIncrement;
				maximumDiameter += diameterIncrement;
				// if we haven't found anything by time we reach the limits we
				// bail
				if (minimumDiameter < minimumAllowableDiameter
						|| maximumDiameter > maximumAllowableDiameter) {
					throw new Exception("Homing dot not found");
				}
				continue;
			}
			// locateCircles returns the Circles ordered by diameter and then
			// distance from the point of interest. So, we're going to assume
			// the best matching Circle is first and use that.
			Circle circle = circles[0];
			
			// determine the position and diameter of the Circle, in units
			double circleX = circle.getX() * unitsPerPixel.getX();
			double circleY = circle.getY() * unitsPerPixel.getY();
			double circleDiameter = circle.getDiameter() * unitsPerPixel.getX();
			
			// move the position from the bottom left to the center
			BufferedImage image = camera.capture();
			double imageWidthInUnits = image.getWidth() * unitsPerPixel.getX();
			double imageHeightInUnits = image.getHeight() * unitsPerPixel.getY();
			
			circleX -= imageWidthInUnits / 2;
			circleY -= imageHeightInUnits / 2;

			// get the position of the head
			double x = getX();
			double y = getY();
			
			// apply the offset from the circle
			x += circleX;
			y += circleY;

			// and go there
			moveTo(x, y, getZ(), getC());
			
			found = true;
		}
		
		return camera;
	}

	@Override
	public void moveTo(double x, double y, double z, double c) throws Exception {
		moveTo(x, y, z, c, feedRate);
	}

	@Override
	public void moveTo(double x, double y, double z, double c,
			double feedRatePerMinute) throws Exception {
		if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)
				|| Double.isNaN(c)) {
			throw new Exception(
					String.format(
							"Movement to %2.4f, %2.4f, %2.4f, %2.4f is invalid. You have bad data somewhere.",
							x, y, z, c));
		}
		if (x < softLimits.minX || x > softLimits.maxX || y < softLimits.minY
				|| y > softLimits.maxY || z < softLimits.minZ
				|| z > softLimits.maxZ || c < softLimits.minC
				|| c > softLimits.maxC) {
			throw new Exception(
					String.format(
							"Movement to %2.4f, %2.4f, %2.4f, %2.4f would violate soft limits of (%2.4f, %2.4f), (%2.4f, %2.4f), (%2.4f, %2.4f), (%2.4f, %2.4f).",
							x, y, z, c, softLimits.minX, softLimits.maxX,
							softLimits.minY, softLimits.maxY, softLimits.minZ,
							softLimits.maxZ, softLimits.minC, softLimits.maxC));
		}
		double feedRateMmPerMinute = LengthUtil.convertLength(
				feedRatePerMinute, machine.getNativeUnits(),
				LengthUnit.Millimeters);
		machine.getDriver().moveTo(this, x + offsetX, y + offsetY, z + offsetZ,
				c + offsetC, feedRateMmPerMinute);
		this.x = x + offsetX;
		this.y = y + offsetY;
		this.z = z + offsetZ;
		this.c = c + offsetC;
		machine.fireMachineHeadActivity(machine, this);
	}

	@Override
	public boolean canPickAndPlace(Feeder feeder, Location pickLocation,
			Location placeLocation) {
		return true;
	}

	@Override
	public void pick() throws Exception {
		machine.getDriver().pick(this, null);
		machine.fireMachineHeadActivity(machine, this);
	}

	@Override
	public void pick(Part part, Feeder feeder, Location pickLocation)
			throws Exception {
		// move to the pick location
		moveTo(pickLocation.getX(), pickLocation.getY(), getZ(),
				pickLocation.getRotation());
		// lower the nozzle
		moveTo(getX(), getY(), pickLocation.getZ(), getC());

		// pick the part
		machine.getDriver().pick(this, part);
		machine.fireMachineHeadActivity(machine, this);
		Thread.sleep(pickDwellMilliseconds);
	}

	@Override
	public void place() throws Exception {
		machine.getDriver().place(this);
		machine.fireMachineHeadActivity(machine, this);
	}

	@Override
	public void place(Part part, Location placeLocation) throws Exception {
		// move to the place location
		moveTo(placeLocation.getX(), placeLocation.getY(), getZ(),
				placeLocation.getRotation());
		// lower the nozzle
		moveTo(getX(), getY(), placeLocation.getZ(), getC());
		// place the part
		machine.getDriver().place(this);
		machine.fireMachineHeadActivity(machine, this);
		Thread.sleep(placeDwellMilliseconds);
	}

	@Override
	public List<String> getActuatorNames() {
		return Collections.singletonList(PIN_ACTUATOR_NAME);
	}

	@Override
	public void actuate(String actuator, boolean on) throws Exception {
		if (actuator.equals(PIN_ACTUATOR_NAME)) {
			machine.getDriver().actuate(this, 0, on);
			machine.fireMachineHeadActivity(machine, this);
		}
		else {
			throw new Exception("Unrecognized actuator: " + actuator);
		}
	}

	@Override
	public double getX() {
		return x - offsetX;
	}

	@Override
	public double getY() {
		return y - offsetY;
	}

	@Override
	public double getZ() {
		return z - offsetZ;
	}

	@Override
	public double getC() {
		return c - offsetC;
	}

	@Override
	public void setPerceivedX(double perceivedX) {
		offsetX = x - perceivedX;
		machine.fireMachineHeadActivity(machine, this);
	}

	@Override
	public void setPerceivedY(double perceivedY) {
		offsetY = y - perceivedY;
		machine.fireMachineHeadActivity(machine, this);
	}

	@Override
	public void setPerceivedZ(double perceivedZ) {
		offsetZ = z - perceivedZ;
		machine.fireMachineHeadActivity(machine, this);
	}

	@Override
	public void setPerceivedC(double perceivedC) {
		offsetC = c - perceivedC;
		machine.fireMachineHeadActivity(machine, this);
	}

	@Override
	public double getAbsoluteX() {
		return x;
	}

	@Override
	public double getAbsoluteY() {
		return y;
	}

	@Override
	public double getAbsoluteZ() {
		return z;
	}

	@Override
	public double getAbsoluteC() {
		return c;
	}

	static class SoftLimits {
		@Attribute
		private double minX = Double.NEGATIVE_INFINITY;
		@Attribute
		private double maxX = Double.POSITIVE_INFINITY;
		@Attribute
		private double minY = Double.NEGATIVE_INFINITY;
		@Attribute
		private double maxY = Double.POSITIVE_INFINITY;
		@Attribute
		private double minZ = Double.NEGATIVE_INFINITY;
		@Attribute
		private double maxZ = Double.POSITIVE_INFINITY;
		@Attribute
		private double minC = Double.NEGATIVE_INFINITY;
		@Attribute
		private double maxC = Double.POSITIVE_INFINITY;
	}
	
	static class Homing {
		@Attribute(required=false)
		private boolean useVision;
		@Attribute(required=false)
		private double homingDotDiameter;
		@Element(required=false)
		private Location homingDotLocation;
		
		public Homing() {
			homingDotLocation = new Location();
			homingDotLocation.setUnits(LengthUnit.Millimeters);
		}
	}
}
