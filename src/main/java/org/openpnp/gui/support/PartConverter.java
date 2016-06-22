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

import org.jdesktop.beansbinding.Converter;
import org.openpnp.model.Configuration;
import org.openpnp.model.Part;

public class PartConverter extends Converter<Part, String> {
    private Configuration configuration;
    private IdentifiableObjectToStringConverter<Part> toStringConverter =
            new IdentifiableObjectToStringConverter<>();

    public PartConverter(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public String convertForward(Part part) {
        return toStringConverter.getPreferredStringForItem(part);
    }

    @Override
    public Part convertReverse(String partId) {
        if (partId == null) {
            return null;
        }
        Part part = configuration.getPart(partId);
        if (part == null) {
            throw new RuntimeException("Invalid part id");
        }
        return part;
    }

}
