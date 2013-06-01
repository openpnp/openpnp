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
import org.openpnp.machine.reference.feeder.wizards.ReferenceTrayFeederConfigurationWizard;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Nozzle;
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
	private int trayCountX = 1;
	@Attribute
	private int trayCountY = 1;
	@Element
	private Location offsets = new Location(LengthUnit.Millimeters);
	@Attribute
	private int feedCount = 0;
	
	private Location pickLocation;

	@Override
	public boolean canFeedToNozzle(Nozzle nozzle) {
		boolean result = (feedCount < (trayCountX * trayCountY));
		logger.debug("{}.canFeedToNozzle({}) => {}", new Object[]{getId(), nozzle, result});
		return result;
	}
	
	@Override
    public Location getPickLocation() throws Exception {
	    if (pickLocation == null) {
	        pickLocation = location;
	    }
		logger.debug("{}.getPickLocation => {}", getId(), pickLocation);
		return pickLocation;
    }

    public void feed(Nozzle nozzle)
			throws Exception {
		logger.debug("{}.feed({})", getId(), nozzle);
		int partX, partY;
        
        if (trayCountX >= trayCountY) {
            // X major axis.
            partX = feedCount / trayCountY;
            partY = feedCount % trayCountY;
        }
        else {
            // Y major axis.
            partX = feedCount % trayCountX;
            partY = feedCount / trayCountX;
        }
        
        // Multiply the offsets by the X/Y part indexes to get the total offsets
        // and then add the pickLocation to offset the final value.
        pickLocation = offsets
                .multiply(partX, partY, 0.0, 0.0)
                .add(location);

        logger.debug(String.format(
                "Feeding part # %d, x %d, y %d, xPos %f, yPos %f", feedCount,
                partX, partY, pickLocation.getX(), pickLocation.getY()));
        
        feedCount++;
	}

	@Override
	public Wizard getConfigurationWizard() {
		return new ReferenceTrayFeederConfigurationWizard(this);
	}

	public int getTrayCountX() {
		logger.debug("{}.getTrayCountX => {}", getId(), trayCountX);
		return trayCountX;
	}

	public void setTrayCountX(int trayCountX) {
		logger.debug("{}.setTrayCountX({})", getId(), trayCountX);
		this.trayCountX = trayCountX;
	}

	public int getTrayCountY() {
		logger.debug("{}.getTrayCountY => {}", getId(), trayCountY);
		return trayCountY;
	}

	public void setTrayCountY(int trayCountY) {
		logger.debug("{}.setTrayCountY({})", getId(), trayCountY);
		this.trayCountY = trayCountY;
	}

	public Location getOffsets() {
		return offsets;
	}

	public void setOffsets(Location offsets) {
		logger.debug("{}.setOffsets({})", getId(), offsets);
		this.offsets = offsets;
	}

	public int getFeedCount() {
		logger.debug("{}.getFeedCount => {}", getId(), feedCount);
		return feedCount;
	}

	public void setFeedCount(int feedCount) {
		logger.debug("{}.setFeedCount({})", getId(), feedCount);
		this.feedCount = feedCount;
	}

	@Override
	public Location getLocation() {
		Location result = super.getLocation();
		logger.debug("{}.getLocation => {}", getId(), result);
		return result;
	}

	@Override
	public void setLocation(Location location) {
		logger.debug("{}.setLocation({})", getId(), location);
		super.setLocation(location);
	}

	@Override
	public void setEnabled(boolean enabled) {
		logger.debug("{}.setEnabled({})", getId(), enabled);
		super.setEnabled(enabled);
	}

	@Override
	public void setPart(Part part) {
		logger.debug("{}.setPart({})", getId(), part);
		super.setPart(part);
	}

	@Override
	public String toString() {
		return getId();
	}

}
