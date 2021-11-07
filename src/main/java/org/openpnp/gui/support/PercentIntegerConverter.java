/*
 * Copyright (C) 2021 <mark@makr.zone>
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

public class PercentIntegerConverter extends Converter<Double, Integer> {
    public PercentIntegerConverter() {
    }

    @Override
    public Integer convertForward(Double arg0) {
        return (int)Math.round(arg0*100);
    }

    @Override
    public Double convertReverse(Integer arg0) {
        return arg0*0.01;
    }
}
