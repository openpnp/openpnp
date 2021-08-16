/*
 * Copyright (C) 2021 Tony Luken <tonyluken@att.net>
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

package org.openpnp.gui.support;

import java.util.Locale;

import org.openpnp.model.Volume;
import org.openpnp.model.VolumeUnit;
import org.openpnp.model.Configuration;

public class VolumeCellValue implements Comparable<VolumeCellValue> {
    private static Configuration configuration;

    private Volume volume;

    /**
     * When set, the toString() method will show the units contained within the Volume instead of
     * converting to the system units.
     */
    private boolean displayNativeUnits;

    public static void setConfiguration(Configuration configuration) {

        VolumeCellValue.configuration = configuration;
    }

    public VolumeCellValue(Volume volume, boolean displayNativeUnits) {
        setVolume(volume);
        setDisplayNativeUnits(displayNativeUnits);
    }

    public VolumeCellValue(Volume volume) {
        this(volume, false);
    }

    public VolumeCellValue(String value) {
        Volume volume = Volume.parse(value, false);
        if (volume == null) {
            throw new NullPointerException();
        }
        setVolume(volume);
    }

    public Volume getVolume() {
        return volume;
    }

    public void setVolume(Volume volume) {
        this.volume = volume;
    }

    public boolean isDisplayNativeUnits() {
        return displayNativeUnits;
    }

    public void setDisplayNativeUnits(boolean displayNativeUnits) {
        this.displayNativeUnits = displayNativeUnits;
    }

    @Override
    public String toString() {
        Volume volume = this.volume;
        if (volume.getUnits() == null) {
            return String.format(Locale.US, configuration.getLengthDisplayFormatWithUnits(),
                    volume.getValue(), "?");
        }
        if (displayNativeUnits && volume.getUnits() != VolumeUnit.fromLengthUnit(configuration.getSystemUnits())) {
            return String.format(Locale.US, configuration.getLengthDisplayFormatWithUnits(),
                    volume.getValue(), volume.getUnits().getShortName());
        }
        else {
            volume = volume.convertToUnits(VolumeUnit.fromLengthUnit(configuration.getSystemUnits()));
            return String.format(Locale.US, configuration.getLengthDisplayFormat(), volume.getValue());
        }
    }

    @Override
    public int compareTo(VolumeCellValue other)
    {
        double otherValue = other.getVolume().convertToUnits(getVolume().getUnits()).getValue();
        if (getVolume().getValue() == otherValue) {
            return 0;
        }
        if (getVolume().getValue() < otherValue) {
            return -1;
        }
        return 1;
    }
}
