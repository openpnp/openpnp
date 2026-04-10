/*
 * Copyright (C) 2026 mcix
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

package org.openpnp.machine.hwgc;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.hwgc.wizards.HwgcFeederConfigurationWizard;
import org.openpnp.machine.reference.ReferenceFeeder;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Driver;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;
import javax.swing.Action;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

/**
 * Feeder driver for HWGC SMT machines.
 * Activates feeders using the FEEDER_SWITCH (0x40) protocol command.
 *
 * The machine has up to 50 feeders (0-49): front 0-24, back 25-49.
 */
public class HwgcFeeder extends ReferenceFeeder {

    @Attribute(required = false)
    private int feederNumber = 0;

    @Attribute(required = false)
    private int feedCount = 0;

    @Element(required = false)
    private Length partPitchInTape = new Length(4, LengthUnit.Millimeters);

    /** Time in ms to keep the feeder solenoid active. */
    @Attribute(required = false)
    private int feedDurationMs = 200;

    @Override
    public Location getPickLocation() throws Exception {
        return location;
    }

    @Override
    public void feed(Nozzle nozzle) throws Exception {
        HwgcDriver driver = findHwgcDriver();
        if (driver == null) {
            throw new Exception("No HwgcDriver found in machine configuration");
        }

        Logger.debug("HWGC feeder {}: feeding", feederNumber);
        driver.sendFeeder(feederNumber, true);
        Thread.sleep(feedDurationMs);
        driver.sendFeeder(feederNumber, false);
        feedCount++;
    }

    private HwgcDriver findHwgcDriver() {
        for (Driver d : Configuration.get().getMachine().getDrivers()) {
            if (d instanceof HwgcDriver) {
                return (HwgcDriver) d;
            }
        }
        return null;
    }

    // ── Getters/setters ──

    public int getFeederNumber() {
        return feederNumber;
    }

    public void setFeederNumber(int feederNumber) {
        this.feederNumber = feederNumber;
    }

    public int getFeedCount() {
        return feedCount;
    }

    public void setFeedCount(int feedCount) {
        this.feedCount = feedCount;
    }

    public Length getPartPitchInTape() {
        return partPitchInTape;
    }

    public void setPartPitchInTape(Length partPitchInTape) {
        this.partPitchInTape = partPitchInTape;
    }

    public int getFeedDurationMs() {
        return feedDurationMs;
    }

    public void setFeedDurationMs(int feedDurationMs) {
        this.feedDurationMs = feedDurationMs;
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new HwgcFeederConfigurationWizard(this);
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
