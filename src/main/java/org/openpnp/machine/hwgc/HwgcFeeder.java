/*
 * Copyright (C) 2026 Contributed by Arnoud @ DeltaProto <arnoud@deltaproto.com>
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

import javax.swing.Action;

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
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

/**
 * Feeder driver for HWGC SMT machines.
 * Activates feeders using the FEEDER_SWITCH (0x40) protocol command.
 *
 * <p>Slot numbering: feederNumber is the user-visible 1..50 slot index
 * (front 1..25, back 26..50). The HwgcDriver protocol is 0-indexed,
 * so we subtract 1 before sending to hardware.
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

        int hwIndex = feederNumber - 1;

        // Always close first so that consecutive picks from the same feeder
        // (e.g. n2 then n3) each get a full close-open cycle.  Without this,
        // the second feed() finds the feeder still open and the hardware does
        // not advance the tape.
        Logger.info("HWGC feeder {} (hw index {}): closing before feed", feederNumber, hwIndex);
        driver.sendFeeder(hwIndex, false);
        Thread.sleep(feedDurationMs);

        Logger.info("HWGC feeder {} (hw index {}): opening for pick", feederNumber, hwIndex);
        driver.sendFeeder(hwIndex, true);
        Thread.sleep(feedDurationMs);
        feedCount++;
    }

    @Override
    public void postPick(Nozzle nozzle) throws Exception {
        HwgcDriver driver = findHwgcDriver();
        if (driver == null) {
            throw new Exception("No HwgcDriver found in machine configuration");
        }
        int hwIndex = feederNumber - 1;
        Logger.info("HWGC feeder {} (hw index {}): closing after pick", feederNumber, hwIndex);
        driver.sendFeeder(hwIndex, false);
    }

    /**
     * Holds the feeder solenoid open (or releases it). Used by the wizard
     * Open / Close buttons so the operator can teach the pick Z height
     * with the cover lifted. Unlike {@link #feed(Nozzle)}, this does not
     * auto-release after a delay — the operator must explicitly close.
     */
    public void setOpen(boolean open) throws Exception {
        HwgcDriver driver = findHwgcDriver();
        if (driver == null) {
            throw new Exception("No HwgcDriver found in machine configuration");
        }
        int hwIndex = feederNumber - 1;
        Logger.debug("HWGC feeder {} (hw index {}): {}",
                feederNumber, hwIndex, open ? "open" : "close");
        driver.sendFeeder(hwIndex, open);
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
