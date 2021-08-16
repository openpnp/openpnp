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

import org.openpnp.model.Area;
import org.openpnp.model.AreaUnit;
import org.openpnp.model.Configuration;

public class AreaCellValue implements Comparable<AreaCellValue> {
    private static Configuration configuration;

    private Area area;

    /**
     * When set, the toString() method will show the units contained within the Area instead of
     * converting to the system units.
     */
    private boolean displayNativeUnits;

    public static void setConfiguration(Configuration configuration) {

        AreaCellValue.configuration = configuration;
    }

    public AreaCellValue(Area area, boolean displayNativeUnits) {
        setArea(area);
        setDisplayNativeUnits(displayNativeUnits);
    }

    public AreaCellValue(Area area) {
        this(area, false);
    }

    public AreaCellValue(String value) {
        Area area = Area.parse(value, false);
        if (area == null) {
            throw new NullPointerException();
        }
        setArea(area);
    }

    public Area getArea() {
        return area;
    }

    public void setArea(Area area) {
        this.area = area;
    }

    public boolean isDisplayNativeUnits() {
        return displayNativeUnits;
    }

    public void setDisplayNativeUnits(boolean displayNativeUnits) {
        this.displayNativeUnits = displayNativeUnits;
    }

    @Override
    public String toString() {
        Area area = this.area;
        if (area.getUnits() == null) {
            return String.format(Locale.US, configuration.getLengthDisplayFormatWithUnits(),
                    area.getValue(), "?");
        }
        if (displayNativeUnits && area.getUnits() != AreaUnit.fromLengthUnit(configuration.getSystemUnits())) {
            return String.format(Locale.US, configuration.getLengthDisplayFormatWithUnits(),
                    area.getValue(), area.getUnits().getShortName());
        }
        else {
            area = area.convertToUnits(AreaUnit.fromLengthUnit(configuration.getSystemUnits()));
            return String.format(Locale.US, configuration.getLengthDisplayFormat(), area.getValue());
        }
    }

    @Override
    public int compareTo(AreaCellValue other)
    {
        double otherValue = other.getArea().convertToUnits(getArea().getUnits()).getValue();
        if (getArea().getValue() == otherValue) {
            return 0;
        }
        if (getArea().getValue() < otherValue) {
            return -1;
        }
        return 1;
    }
}
