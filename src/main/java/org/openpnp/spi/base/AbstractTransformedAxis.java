/*
 * Copyright (C) 2020 <mark@makr.zone>
 * inspired and based on work
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

package org.openpnp.spi.base;

import org.openpnp.model.AxesLocation;
import org.openpnp.model.LengthUnit;
import org.openpnp.spi.TransformedAxis;

public abstract class AbstractTransformedAxis extends AbstractAxis implements TransformedAxis {
    // Convenience functions for null checking.
    public static AxesLocation toTransformed(AbstractAxis axis, AxesLocation location) {
        if (axis != null) {
            return axis.toTransformed(location);
        }
        return location;
    }
    public static AxesLocation toRaw(AbstractAxis axis, AxesLocation location) throws Exception {
        if (axis != null) {
            return axis.toRaw(location);
        }
        return location;
    }
}