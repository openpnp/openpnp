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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.openpnp.RequiresConfigurationResolution;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.wizards.ReferenceHeadConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.VisionProvider;
import org.openpnp.spi.VisionProvider.Circle;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persist;
import org.simpleframework.xml.core.PersistenceException;
import org.simpleframework.xml.core.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReferenceHead implements Head, RequiresConfigurationResolution {
	private final static Logger logger = LoggerFactory.getLogger(ReferenceHead.class);
	
	private ReferenceMachine machine;
	private double x, y, z, c;
	private double offsetX, offsetY, offsetZ, offsetC;
	private boolean softLimitsOverridden;

	@Attribute
	private String id;
	@Attribute
	private int pickDwellMilliseconds;
	@Attribute(required = false)
	private int placeDwellMilliseconds;
	@Element
	private Length feedRate;
	@Element
	private SoftLimits softLimits = new SoftLimits();
	@Element(required = false)
	private Homing homing = new Homing();
	@ElementList(required = false, name = "actuators")
	private ArrayList<ReferenceActuator> actuatorsList = new ArrayList<ReferenceActuator>();

	private LinkedHashMap<String, ReferenceActuator> actuators = new LinkedHashMap<String, ReferenceActuator>();

	@Commit
	private void commit() {
		for (ReferenceActuator actuator : actuatorsList) {
			actuators.put(actuator.getId(), actuator);
		}
	}

	@Persist
	private void persist() {
		actuatorsList.clear();
		actuatorsList.addAll(actuators.values());
	}
	
	@Override
	public void resolve(Configuration configuration) throws Exception {
		this.machine = (ReferenceMachine) configuration.getMachine();
		for (ReferenceActuator actuator : actuators.values()) {
			configuration.resolve(actuator);
			actuator.setReferenceHead(this);
		}
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	@Override
	public void home() throws Exception {
		// TODO: Perform home switch homing first.
		logger.debug("Perform home switch homing");

		// Apply the homing location to the current position
		logger.debug("Apply the homing location to the current position");
		Location homingLocation = homing.location.convertToUnits(machine
				.getNativeUnits());
		setPerceivedX(homingLocation.getX());
		setPerceivedY(homingLocation.getY());
		setPerceivedZ(homingLocation.getZ());
		setPerceivedC(homingLocation.getRotation());

		if (homing.vision.enabled) {
			logger.debug("Home With Vision");
			homeWithVision();
			Location homingDotLocation = homing.vision.homingDotLocation;
			logger.debug("Set homing dot location");
			setPerceivedX(homingDotLocation.getX());
			setPerceivedY(homingDotLocation.getY());
			setPerceivedZ(homingDotLocation.getZ());
		}
	}

	private void homeWithVision() throws Exception {
		for (int i = 0; i < 8; i++) {
			double x = getX();
			double y = getY();

			logger.debug("Attempt " + i);
			Camera camera = attemptHomeWithVision();

			if (x == getX() && y == getY()) {
				logger.debug("Vision homing complete, move to offsets");
				Location cameraOffsets = camera.getLocation().convertToUnits(
						machine.getNativeUnits());
				moveTo(getX() + cameraOffsets.getX(),
						getY() + cameraOffsets.getY(), getZ(), getC());
				return;
			}

			// Give the head a moment to settle before trying again
			Thread.sleep(200);
		}

		throw new Exception("Unable to settle after 8 tries. Giving up.");
	}

	private Camera attemptHomeWithVision() throws Exception {
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
		
		// Get the homing dot location and convert to native units.
		Location homingDotLocation = homing.getVision().getHomingDotLocation();
		homingDotLocation = homingDotLocation.convertToUnits(machine.getNativeUnits());
		double x = homingDotLocation.getX();
		double y = homingDotLocation.getY();
		double z = homingDotLocation.getZ();
		
		// Apply the camera offsets. We subtract instead of adding because we
		// want to position the camera over the location versus wanting to know
		// where the camera is in relation to the location.
		x -= camera.getLocation().getX();
		y -= camera.getLocation().getY();
		z -= camera.getLocation().getZ();
		
		// Go to Safe-Z
		moveTo(getX(), getY(), 0, getC());

		// Position the camera over the homing dot
		moveTo(x, y, 0, getC());
		
		// Move the camera to be in focus over the homing dot
		moveTo(getX(), getY(), z, getC());
		
		VisionProvider vision = camera.getVisionProvider();

		/*
		 * Start looking for the homing dot. We first try to find an exact
		 * match. If one is not found we expand the search until we either find
		 * the dot or we go past the allowable range.
		 */
		boolean found = false;
		double diameter = homing.vision.homingDotDiameter;
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
			double imageHeightInUnits = image.getHeight()
					* unitsPerPixel.getY();

			circleX -= imageWidthInUnits / 2;
			circleY -= imageHeightInUnits / 2;

			// get the position of the head
			x = getX();
			y = getY();

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
		Length feedRate = this.feedRate.convertToUnits(getMachine().getNativeUnits());
		moveTo(x, y, z, c, feedRate.getValue());
	}

	@Override
	public void moveTo(double x, double y, double z, double c,
			double feedRatePerMinute) throws Exception {
		if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)
				|| Double.isNaN(c)) {
			throw new Exception(
					String.format(
							"Movement to %2.4f, %2.4f, %2.4f, %2.4f is not valid. You have bad data somewhere.",
							x, y, z, c));
		}
		Location min = softLimits.getMinimums().convertToUnits(getMachine().getNativeUnits());
		Location max = softLimits.getMaximums().convertToUnits(getMachine().getNativeUnits());
		
		if (!softLimitsOverridden && softLimits.enabled && (
				x < min.getX() || x > max.getX() || 
				y < min.getY() || y > max.getY() || 
				z < min.getZ() || z > max.getZ() || 
				c < min.getRotation() || c > max.getRotation() 
				)) {
			throw new Exception(
					String.format(
							"Movement to %2.4f, %2.4f, %2.4f, %2.4f would violate soft limits of (%2.4f, %2.4f), (%2.4f, %2.4f), (%2.4f, %2.4f), (%2.4f, %2.4f).",
							x, y, z, c,
							min.getX(), max.getX(),
							min.getY(), max.getY(),
							min.getZ(), max.getZ(),
							min.getRotation(), max.getRotation()
							));
		}
		double feedRateMmPerMinute = new Length(feedRatePerMinute,
				machine.getNativeUnits())
				.convertToUnits(getMachine().getNativeUnits()).getValue();
		machine.getDriver().moveTo(this, x + offsetX, y + offsetY, z + offsetZ,
				c + offsetC, feedRateMmPerMinute);
		this.x = x + offsetX;
		this.y = y + offsetY;
		this.z = z + offsetZ;
		this.c = c + offsetC;
		machine.fireMachineHeadActivity(machine, this);
	}
	
	/**
	 * Can be called by the driver to provide updates during the move
	 * operation. This will allow the DROs to update more naturally, and allows
	 * things like simulated cameras to update during the move operation.
	 * It is not required that this be called, but it is recommended.
	 * @param x
	 * @param y
	 * @param z
	 * @param c
	 */
	public void updateDuringMoveTo(double x, double y, double z, double c) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.c = c;
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
	public List<Actuator> getActuators() {
		ArrayList<Actuator> l = new ArrayList<Actuator>();
		l.addAll(actuators.values());
		return l;
	}

	public ReferenceActuator getActuator(String name) {
		return actuators.get(name);
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

	public int getPickDwellMilliseconds() {
		return pickDwellMilliseconds;
	}

	public void setPickDwellMilliseconds(int pickDwellMilliseconds) {
		this.pickDwellMilliseconds = pickDwellMilliseconds;
	}

	public int getPlaceDwellMilliseconds() {
		return placeDwellMilliseconds;
	}

	public void setPlaceDwellMilliseconds(int placeDwellMilliseconds) {
		this.placeDwellMilliseconds = placeDwellMilliseconds;
	}

	public Length getFeedRate() {
		return feedRate;
	}

	public void setFeedRate(Length feedRate) {
		this.feedRate = feedRate;
	}

	public SoftLimits getSoftLimits() {
		return softLimits;
	}

	public void setSoftLimits(SoftLimits softLimits) {
		this.softLimits = softLimits;
	}

	public Homing getHoming() {
		return homing;
	}

	public void setHoming(Homing homing) {
		this.homing = homing;
	}

	@Override
	public Wizard getConfigurationWizard() {
		return new ReferenceHeadConfigurationWizard(this);
	}
	
	@Override
	public Machine getMachine() {
		return machine;
	}

	/**
	 * Stores all the values associated with the Head's soft limits.
	 */
	public static class SoftLimits {
		@Element
		private Location minimums = new Location(LengthUnit.Millimeters, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
		@Element
		private Location maximums = new Location(LengthUnit.Millimeters, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
		@Attribute(required=false)
		private boolean enabled;
		
		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public Location getMinimums() {
			return minimums;
		}

		public void setMinimums(Location minimums) {
			this.minimums = minimums;
		}

		public Location getMaximums() {
			return maximums;
		}

		public void setMaximums(Location maximums) {
			this.maximums = maximums;
		}
	}

	/**
	 * Stores configuration information related to how the Head locates it's
	 * home position.
	 */
	public static class Homing {
		@Element(required = false)
		private Vision vision = new Vision();
		/**
		 * The position loaded into the Head when the homing operation has
		 * completed successfully. This is used to offset home from your homing
		 * switches, if needed. If the process of using your home switches lands
		 * the Head at 0,0,0,0 then this Location can be 0,0,0,0. But perhaps
		 * your have your X home switch at the end of it's travel instead of the
		 * beginning and that is 400mm from where you would like to consider
		 * home. By using 400,0,0,0 when you home the head it will then think it
		 * is at 400,0,0,0 and going to 0,0,0,0 will take you to "home".
		 */
		@Element(required = false)
		private Location location = new Location(LengthUnit.Millimeters);

		public Vision getVision() {
			return vision;
		}

		public void setVision(Vision vision) {
			this.vision = vision;
		}

		public Location getLocation() {
			return location;
		}

		public void setLocation(Location location) {
			this.location = location;
		}

		/**
		 * Vision information required for homing.
		 */
		public static class Vision {
			@Attribute(required = false)
			private boolean enabled;

			@Attribute(required = false)
			private double homingDotDiameter;
			@Element(required = false)
			private Location homingDotLocation = new Location(
					LengthUnit.Millimeters);

			public boolean isEnabled() {
				return enabled;
			}

			public void setEnabled(boolean enabled) {
				this.enabled = enabled;
			}

			public double getHomingDotDiameter() {
				return homingDotDiameter;
			}

			public void setHomingDotDiameter(double homingDotDiameter) {
				this.homingDotDiameter = homingDotDiameter;
			}

			public Location getHomingDotLocation() {
				return homingDotLocation;
			}

			public void setHomingDotLocation(Location homingDotLocation) {
				this.homingDotLocation = homingDotLocation;
			}

			@SuppressWarnings("unused")
			@Validate
			private void validate() throws Exception {
				if (enabled) {
					if (homingDotDiameter == 0) {
						throw new PersistenceException(
								"ReferenceHead.Vision: homing-dot-diameter is required if vision is enabled.");
					}
					if (homingDotLocation == null) {
						throw new PersistenceException(
								"ReferenceHead.Vision: homing-dot-location is required if vision is enabled.");
					}
				}
			}
		}
	}
}
