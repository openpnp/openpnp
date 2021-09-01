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

import org.jdesktop.beansbinding.Converter;
import org.openpnp.model.Area;
import org.openpnp.model.AreaUnit;
import org.openpnp.model.Configuration;

public class AreaConverter extends Converter<Area, String> {
    final String format;
    
    public AreaConverter() {
        this(Configuration.get().getLengthDisplayFormat());
    }
    
    public AreaConverter(String format) {
        this.format = format;
    }
    
    @Override
    public String convertForward(Area area) {
        area = area.convertToUnits(AreaUnit.fromLengthUnit(Configuration.get().getSystemUnits()));
        return String.format(Locale.US, format, area.getValue());
    }

    @Override
    public Area convertReverse(String s) {
        Area area = Area.parse(s, false);
        if (area == null) {
            throw new RuntimeException("Unable to parse " + s);
        }
        if (area.getUnits() == null) {
            area.setUnits(AreaUnit.fromLengthUnit(Configuration.get().getSystemUnits()));
        }
        return area;
    }
}
