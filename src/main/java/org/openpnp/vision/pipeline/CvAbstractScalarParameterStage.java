/*
 * Copyright (C) 2022 <mark@makr.zone>
 * inspired and based on work by
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

package org.openpnp.vision.pipeline;

/**
 * Abstracts a scalar parameter, typically set by a slider. The scalar is abstracted by an 
 * integer, so discrete/enumerated settings can also be modeled reliably. 
 */
public abstract class CvAbstractScalarParameterStage extends CvAbstractParameterStage {

    abstract public int minimumScalar();
    abstract public int maximumScalar();
    public int convertToScalar(Object value) {
        return Math.max(minimumScalar(), 
                Math.min(maximumScalar(),
                        (int)value));
    }
    abstract public Object convertToValue(int scalar);
}
