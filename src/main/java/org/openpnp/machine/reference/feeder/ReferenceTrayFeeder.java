/*
 * Copyright (C) 2017 Sebastian Pichelhofer & Jason von Nieda <jason@vonnieda.org>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.machine.reference.feeder;

import javax.swing.Action;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceFeeder;
import org.openpnp.machine.reference.feeder.wizards.ReferenceTrayFeederConfigurationWizard;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

/**
 * Implementation of Feeder that indexes based on an offset. This allows a tray
 * of parts to be picked from without moving any tape. Can handle trays of
 * arbitrary X and Y count.
 */
public class ReferenceTrayFeeder extends ReferenceFeeder {

	@Attribute
	private int trayCountX = 1;
	@Attribute
	private int trayCountY = 1;
	@Element
	private Location offsets = new Location(LengthUnit.Millimeters);
	@Attribute
	private int feedCount = 0;
	@Attribute
	private double trayRotation = 0;
	@Attribute
	protected Location lastComponentLocation = new Location(LengthUnit.Millimeters);

	private Location pickLocation;

	@Override
	public Location getPickLocation() throws Exception {
		if (pickLocation == null) {
			pickLocation = location;
		}
		Logger.debug("{}.getPickLocation => {}", getName(), pickLocation);
		return pickLocation;
	}

	public void feed(Nozzle nozzle) throws Exception {
		Logger.debug("{}.feed({})", getName(), nozzle);
		int partX, partY;

		if (feedCount >= (trayCountX * trayCountY)) {
			throw new Exception("Tray empty.");
		}

		if (trayCountX >= trayCountY) {
			// X major axis.
			partX = feedCount / trayCountY;
			partY = feedCount % trayCountY;
		} else {
			// Y major axis.
			partX = feedCount % trayCountX;
			partY = feedCount / trayCountX;
		}

		// Multiply the offsets by the X/Y part indexes to get the total offsets
		// and then add the pickLocation to offset the final value.
		// and then add them to the location to get the final pickLocation.
		// pickLocation = location.add(offsets.multiply(partX, partY, 0.0,
		// 0.0));

		double delta_x1 = partX * offsets.getX() * Math.cos(Math.toRadians(trayRotation));
		double delta_y1 = Math.sqrt(partX * offsets.getX() * partX * offsets.getX() - delta_x1 * delta_x1);
		Location delta1 = new Location(LengthUnit.Millimeters, delta_x1, delta_y1, 0, 0);

		double delta_x2 = partY * offsets.getY() * Math.cos(Math.toRadians(90-trayRotation))* -1;
		double delta_y2 = Math.sqrt(partY * offsets.getY() * partY * offsets.getY() - delta_x2 * delta_x2) * -1;
		Location delta2 = new Location(LengthUnit.Millimeters, delta_x2, delta_y2, 0, 0);

		pickLocation = location.add(delta1.add(delta2));

		Logger.debug(String.format("Feeding part # %d, x %d, y %d, xPos %f, yPos %f, rPos %f", feedCount, partX, partY,
				pickLocation.getX(), pickLocation.getY(), pickLocation.getRotation()));

		setFeedCount(getFeedCount() + 1);
	}

	public int getTrayCountX() {
		return trayCountX;
	}

	public void setTrayCountX(int trayCountX) {
		this.trayCountX = trayCountX;
	}

	public int getTrayCountY() {
		return trayCountY;
	}

	public void setTrayCountY(int trayCountY) {
		this.trayCountY = trayCountY;
	}

	public Location getLastComponentLocation() {
		return lastComponentLocation;
	}

	public void setLastComponentLocation(Location LastComponentLocation) {
		this.lastComponentLocation = LastComponentLocation;
	}

	public Location getOffsets() {
		return offsets;
	}

	public void setOffsets(Location offsets) {
		this.offsets = offsets;
	}

	public double getTrayRotation() {
		return trayRotation;
	}

	public void setTrayRotation(double trayrotation) {
		this.trayRotation = trayrotation;
	}

	public int getFeedCount() {
		return feedCount;
	}

	public void setFeedCount(int feedCount) {
		int oldValue = this.feedCount;
		this.feedCount = feedCount;
		firePropertyChange("feedCount", oldValue, feedCount);
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public Wizard getConfigurationWizard() {
		return new ReferenceTrayFeederConfigurationWizard(this);
	}

	@Override
	public String getPropertySheetHolderTitle() {
		return getClass().getSimpleName() + " " + getName();
	}

	@Override
	public PropertySheetHolder[] getChildPropertySheetHolders() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Action[] getPropertySheetHolderActions() {
		// TODO Auto-generated method stub
		return null;
	}
}
