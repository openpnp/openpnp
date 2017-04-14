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

package org.openpnp.gui.support;

import java.util.Locale;

import org.openpnp.model.Configuration;

public class RotationCellValue implements Comparable<RotationCellValue> {
    private static Configuration configuration;

    private double rotation;

    /**
     * When set, the toString() method will show the units contained within the Length instead of
     * converting to the system units.
     */
    private boolean displayNativeUnits;

    public static void setConfiguration(Configuration configuration) {
        RotationCellValue.configuration = configuration;
    }

    public RotationCellValue(double Rotation, boolean displayNativeUnits) {
        setRotation(Rotation);
        setDisplayNativeUnits(displayNativeUnits);
    }

    public RotationCellValue(double Rotation) {
        this(Rotation, false);
    }

    public RotationCellValue(String value) {
        double rotation = Double.parseDouble(value);

        setRotation(rotation);
    }

    public double getRotation() {
        return rotation;
    }

    public void setRotation(double Rotation) {
        this.rotation = Rotation;
    }

    public boolean isDisplayNativeUnits() {
        return displayNativeUnits;
    }

    public void setDisplayNativeUnits(boolean displayNativeUnits) {
        this.displayNativeUnits = displayNativeUnits;
    }

    @Override
    public String toString() {
        double r = rotation;
        return String.format(Locale.US, configuration.getLengthDisplayFormat(), r);

    }

    @Override
    public int compareTo(RotationCellValue other)
    {
        if(getRotation()==other.getRotation()) {
            return 0;
        }
        if(getRotation() < other.getRotation()) {
            return -1;
        }
        return 1;
    }
}
