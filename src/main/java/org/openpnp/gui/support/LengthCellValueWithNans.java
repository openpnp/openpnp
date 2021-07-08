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

import org.openpnp.model.Length;

/**
 * Extends LengthCellValue to allow the length value field to take on the 
 * value of NaN (not-a-number)
 *
 */
public class LengthCellValueWithNans extends LengthCellValue {
    public LengthCellValueWithNans(Length length, boolean displayNativeUnits) {
        super(length, displayNativeUnits);
    }

    public LengthCellValueWithNans(Length length) {
        super(length, false);
    }

    public LengthCellValueWithNans(String value) {
        super(new Length(), false);
        Length length = Length.parse(value, false);
        if (length == null) {
            String v = value.trim();
            if (v.toLowerCase().startsWith("nan")) {
                length = Length.parse("1.0" + v.substring(3), false);
            }
            if (length == null) {
                throw new NullPointerException();
            }
            length.setValue(Double.NaN);
        }
        setLength(length);
    }
}
