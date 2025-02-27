/*
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
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
 * Implementation of Feeder that indexes based on an offset. This allows a tray of parts to be
 * picked from without moving any tape. Can handle trays of arbitrary X and Y count.
 */
public class ReferenceTrayFeeder extends ReferenceFeeder {


    @Attribute
    private int trayCountX = 1;
    @Attribute
    private int trayCountY = 1;
    @Element
    private Location offsets = new Location(LengthUnit.Millimeters);
    @Attribute
    private int feedCount = 0;  // UI is base 1, 0 is ok because a pick operation always preceded by a feed, which increments feedCount to 1

    @Override
    public Location getPickLocation() {
        int partX, partY;
        int feedCountBase0 = feedCount -1; // UI uses feedCount base 1, the following calculations are base 0

        // if feedCound is currently zero, assume its one
        // this can happen if the pickLocation is requested before any feed operation
        // return first location in that case
        if (feedCount == 0) {
            feedCountBase0 = 0;
        }
        // limit feed count to tray size
        else if (feedCount > (trayCountX * trayCountY)) {
            feedCountBase0 = trayCountX * trayCountY -1;
            Logger.warn("{}.getPickLocation: feedCount larger then tray, limiting to maximum.", getName());
        }

        if (trayCountX >= trayCountY) {
            // X major axis.
            partX = feedCountBase0 / trayCountY;
            partY = feedCountBase0 % trayCountY;
        }
        else {
            // Y major axis.
            partX = feedCountBase0 % trayCountX;
            partY = feedCountBase0 / trayCountX;
        }

        // Multiply the offsets by the X/Y part indexes to get the total offsets
        // and then add the pickLocation to offset the final value.
        // and then add them to the location to get the final pickLocation.
        return location.add(offsets.multiply(partX, partY, 0.0, 0.0));
    }

    public void feed(Nozzle nozzle) throws Exception {
        Logger.debug("{}.feed({})", getName(), nozzle);

        if (feedCount >= (trayCountX * trayCountY)) {
            throw new Exception("Feeder: " + getName() + " (" + getPart().getId() + ") - tray empty.");
        }

        if (getFeedOptions() == FeedOptions.Normal || getFeedCount() == 0) {
            setFeedCount(getFeedCount() + 1);
        }
        if (getFeedOptions() == FeedOptions.SkipNext) {
            setFeedOptions(FeedOptions.Normal);
        }
    }

    /**
     * Returns if the feeder can take back a part.
     * Makes the assumption, that after each feed a pick followed,
     * so the pockets are now empty.
     */
    @Override
    public boolean canTakeBackPart() {
        return (feedCount > 0);
    }

    @Override
    public void takeBackPart(Nozzle nozzle) throws Exception {
        super.takeBackPart(nozzle);
        putPartBack(nozzle);
        // change FeedCount
        if (getFeedOptions() == FeedOptions.Normal) {
            setFeedCount(getFeedCount() - 1);
        }
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

    public int getFeedCount() {
        return feedCount;
    }

    public void setFeedCount(int feedCount) {
        int oldValue = this.feedCount;
        this.feedCount = feedCount;
        firePropertyChange("feedCount", oldValue, feedCount);
        Logger.debug("{}.setFeedCount(): feedCount {}, pickLocation {}", getName(), feedCount, getPickLocation());
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
        return null;
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return null;
    }

    @Override
    public boolean supportsFeedOptions() {
        return true;
    }
}
