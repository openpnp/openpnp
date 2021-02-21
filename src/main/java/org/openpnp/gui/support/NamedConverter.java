/*
 * Copyright (C) 2019 <mark@makr.zone>
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

import java.util.List;

import org.jdesktop.beansbinding.Converter;
import org.openpnp.model.Named;

public class NamedConverter<NamedType extends Named> extends Converter<NamedType, String> {
    final List<NamedType> pool1;
    final List<NamedType> pool2;

    public NamedConverter(List<NamedType> pool) {
        super();
        this.pool1 = pool;
        this.pool2 = null;
    }
    public NamedConverter(List<NamedType> pool1, List<NamedType> pool2) {
        super();
        this.pool1 = pool1;
        this.pool2 = pool2;
    }

    @Override
    public String convertForward(NamedType named) {
        if (named == null) {
            return "";
        } 
        else {
            return named.getName();
        }
    }

    @Override
    public NamedType convertReverse(String s) {
        for (NamedType named : pool1) {
            if (named.getName().equals(s)) {
                return named;
            }
        }
        if (pool2 != null) {
            for (NamedType named : pool2) {
                if (named.getName().equals(s)) {
                    return named;
                }
            }
        }
        return null;
    }
}
