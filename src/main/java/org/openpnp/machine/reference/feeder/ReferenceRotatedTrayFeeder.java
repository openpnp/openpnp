/*
 * Copyright (C) 2017 Sebastian Pichelhofer & Jason von Nieda <jason@vonnieda.org>
 * Rotation Matrix corrections by Martin Gyurkó <nospam@gyurma.de> in 2021
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
import org.openpnp.machine.reference.feeder.wizards.ReferenceRotatedTrayFeederConfigurationWizard;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Commit;

/**
 * Implementation of Feeder that indexes based on an offset. This allows a tray
 * of parts to be picked from without moving any tape. Can handle trays of
 * arbitrary X and Y count and the tray can be rotated at an arbitrary angle
 * relative to the machine.
 */
public class ReferenceRotatedTrayFeeder extends ReferenceFeeder {

    @Attribute
    private int trayCountCols = 1;
    @Attribute
    private int trayCountRows = 1;
    @Element
    private Location offsets = new Location(LengthUnit.Millimeters);
    @Attribute
    private int feedCount = 0;  // UI is base 1, 0 is ok because a pick operation always preceded by a feed, which increments feedCount to 1

    @Attribute(required=false)
    @Deprecated
    private Double trayRotation = null;

    @Attribute(required=false)
    private double componentRotationInTray = 0;

    @Attribute(required=false)
    private boolean legacyPickingInProgress = false;

    @Element
    protected Location lastComponentLocation = new Location(LengthUnit.Millimeters);
    @Element
    protected Location firstRowLastComponentLocation = new Location(LengthUnit.Millimeters);

    @Commit
    public void commit() {
        if (trayRotation != null) {
            Logger.trace("Updating legacy Rotated Tray Feeder to latest version.");
            //In previous versions, the location held the pick rotation and trayRotation held
            //the actual rotation of the tray. In this version, the location holds the actual
            //rotation of the tray and componentRotationInTray holds the rotation of the component
            //relative to the tray. Note, in almost all cases, componentRotationInTray will be one
            //of 0, or +/-90, or +/-180. So, with the new version, pick rotation = 
            //location.getRotation() + componentRotationInTray

            //Convert the values from the old version to the new version
            componentRotationInTray = location.getRotation() - trayRotation;
            location = location.derive(null, null, null, trayRotation);

            //The previous version of the feeder also had a bug which caused it to skip the first
            //component and thereafter it was picking one component ahead of where it should.
            //This bumps the feedCount up by one to account for that bug.
            if ((feedCount > 0) && (feedCount < trayCountCols*trayCountRows)) {
                feedCount++;
                legacyPickingInProgress = true;
            }

            //Remove the deprecated attribute
            trayRotation = null;
        }
    }

    @Override
    public Location getPickLocation() {
        int feedCountBase0 = feedCount -1; // UI uses feedCount base 1, the following calculations are base 0

        // if feedCound is currently zero, assume its one
        // this can happen if the pickLocation is requested before any feed operation
        // return first location in that case
        if (feedCount == 0) {
            feedCountBase0 = 0;
        }
        // limit feed count to tray size
        else if (feedCount > (trayCountCols * trayCountRows)) {
            feedCountBase0 = trayCountCols * trayCountRows -1;
            Logger.warn("{}.getPickLocation: feedCount larger then tray, limiting to maximum.", getName());
        }
        
        //The original version of this feeder fed along either the rows or columns depending on
        //which was shorter. This version now feeds along a row until it is empty and then it moves
        //to the next row. However, if an old version of the feeder was just loaded and it was
        //partially completed (not completely full or completely empty), the picking order will
        //revert to the legacy method until the feed count is reset to 0.
        int colNum, rowNum;
        if (legacyPickingInProgress && (trayCountCols >= trayCountRows)) {
            //Pick parts along a column (stepping through all the rows) until it is empty and then 
            //move to the next column
            rowNum = feedCountBase0 % trayCountRows;
            colNum = feedCountBase0 / trayCountRows;
        } else {
            //Pick parts along a row (stepping through all the columns) until it is empty and then
            //move to the next row (the new default)
            rowNum = feedCountBase0 / trayCountCols;
            colNum = feedCountBase0 % trayCountCols;
        }

        //The definition of the tray has row numbers increasing in the negative y direction so that
        //is why we negate rowNum here:
        Location delta = offsets.multiply(colNum, -rowNum, 0, 0).rotateXy(location.getRotation()).
                derive(null, null, null, componentRotationInTray);
        
        return location.addWithRotation(delta);
    }

    public void feed(Nozzle nozzle) throws Exception {
        Logger.debug("{}.feed({})", getName(), nozzle);

        if (feedCount >= (trayCountCols * trayCountRows)) {
            throw new Exception(this.getName() + " (" + this.partId + ") is empty.");
        }

        setFeedCount(getFeedCount() + 1);
    }

    /**
     * Returns if the feeder can take back a part.
     * Makes the assumption, that after each feed a pick followed,
     * so the pockets are now empty.
     */
    @Override
    public boolean canTakeBackPart() {
        if (feedCount > 0 ) {  
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void takeBackPart(Nozzle nozzle) throws Exception {
        // first check if we can and want to take back this part (should be always be checked before calling, but to be sure)
        if (nozzle.getPart() == null) {
            throw new UnsupportedOperationException("No part loaded that could be taken back.");
        }
        if (!nozzle.getPart().equals(getPart())) {
            throw new UnsupportedOperationException("Feeder: " + getName() + " - Can not take back " + nozzle.getPart().getId() + " this feeder only supports " + getPart().getId());
        }
        if (!canTakeBackPart()) {
            throw new UnsupportedOperationException("Feeder: " + getName() + " - Currently no free slot. Can not take back the part.");
        }

        // ok, now put the part back on the location of the last pick
        nozzle.moveToPickLocation(this);
        nozzle.place();
        nozzle.moveToSafeZ();
        if (nozzle.isPartOffEnabled(Nozzle.PartOffStep.AfterPlace) && !nozzle.isPartOff()) {
            throw new Exception("Feeder: " + getName() + " - Putting part back failed, check nozzle tip");
        }
        // change FeedCount
        setFeedCount(getFeedCount() - 1);
    }

    public int getTrayCountCols() {
        return trayCountCols;
    }

    public void setTrayCountCols(int trayCountCols) {
        int oldValue = this.trayCountCols;
        this.trayCountCols = trayCountCols;
        firePropertyChange("trayCountCols", oldValue, trayCountCols);
        firePropertyChange("remainingCount", trayCountRows*oldValue - feedCount, 
                trayCountRows*trayCountCols - feedCount);
    }

    public int getTrayCountRows() {
        return trayCountRows;
    }

    public void setTrayCountRows(int trayCountRows) {
        int oldValue = this.trayCountRows;
        this.trayCountRows = trayCountRows;
        firePropertyChange("trayCountRows", oldValue, trayCountRows);
        firePropertyChange("remainingCount", oldValue*trayCountCols - feedCount,
                trayCountRows*trayCountCols - feedCount);
    }

    public Location getLastComponentLocation() {
        return lastComponentLocation;
    }

    public void setLastComponentLocation(Location LastComponentLocation) {
        Location oldValue = this.lastComponentLocation;
        this.lastComponentLocation = LastComponentLocation;
        firePropertyChange("lastComponentLocation", oldValue, lastComponentLocation);
    }

    public Location getFirstRowLastComponentLocation() {
        return this.firstRowLastComponentLocation;
    }

    public void setFirstRowLastComponentLocation(Location FirstRowLastComponentLocation) {
        Location oldValue = this.firstRowLastComponentLocation;
        this.firstRowLastComponentLocation = FirstRowLastComponentLocation;
        firePropertyChange("firstRowLastComponentLocation", oldValue, firstRowLastComponentLocation);
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
        if (feedCount == 0) {
            legacyPickingInProgress = false;
        }
        firePropertyChange("feedCount", oldValue, feedCount);
        firePropertyChange("remainingCount", trayCountRows*trayCountCols - oldValue, 
                trayCountRows*trayCountCols - feedCount);
        Logger.debug("{}.setFeedCount(): feedCount {}, pickLocation {}", getName(), feedCount, getPickLocation());
    }

    public int getRemainingCount() {
        return trayCountRows*trayCountCols - feedCount;
    }

    public double getComponentRotationInTray() {
        return componentRotationInTray;
    }

    public void setComponentRotationInTray(double componentRotationInTray) {
        double oldValue = this.componentRotationInTray;
        this.componentRotationInTray = componentRotationInTray;
        firePropertyChange("componentRotationInTray", oldValue, componentRotationInTray);
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceRotatedTrayFeederConfigurationWizard(this);
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
}
