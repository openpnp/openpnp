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

package org.openpnp.machine.reference.feeder;



import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceFeeder;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.machine.reference.feeder.wizards.ReferenceTrayFeederConfigurationWizard;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Head;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementation of Feeder that indexes based on an offset. This allows a tray of
 * parts to be picked from without moving any tape. Can handle trays of
 * arbitrary X and Y count.
 */
public class ReferenceTrayFeeder extends ReferenceFeeder {
	private final static Logger logger = LoggerFactory.getLogger(ReferenceTrayFeeder.class);
	
	@Attribute
	private int trayCountX;
	@Attribute
	private int trayCountY;
	@Element
	private Location offsets = new Location(LengthUnit.Millimeters);

	private int pickCount;

	@Override
	public boolean canFeedForHead(Head head) {
		return (pickCount < (trayCountX * trayCountY));
	}

	public Location feed(Head head_, Location pickLocation)
			throws Exception {
		ReferenceHead head = (ReferenceHead) head_;

		int partX = (pickCount / trayCountX);
		int partY = (pickCount - (partX * trayCountX));

		Location l = new Location();
		l.setX(pickLocation.getX() + (partX * offsets.getX()));
		l.setY(pickLocation.getY() + (partY * offsets.getY()));
		l.setZ(pickLocation.getZ());
		l.setRotation(pickLocation.getRotation());
		l.setUnits(pickLocation.getUnits());

		logger.debug(String.format(
				"Feeding part # %d, x %d, y %d, xPos %f, yPos %f", pickCount,
				partX, partY, l.getX(), l.getY()));

		pickCount++;

		return l;
	}

	@Override
	public Wizard getConfigurationWizard() {
		return new ReferenceTrayFeederConfigurationWizard(this);
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

	public Location getOffsets() {
		return offsets;
	}

	public void setOffsets(Location offsets) {
		this.offsets = offsets;
	}

	public int getPickCount() {
		return pickCount;
	}

	public void setPickCount(int pickCount) {
		this.pickCount = pickCount;
	}

}
