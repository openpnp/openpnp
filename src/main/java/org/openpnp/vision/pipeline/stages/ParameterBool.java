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

package org.openpnp.vision.pipeline.stages;

import org.openpnp.vision.pipeline.CvAbstractScalarParameterStage;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.Property;
import org.openpnp.vision.pipeline.Stage;
import org.simpleframework.xml.Attribute;

@Stage(description="Exposes a Boolean stage property as an external parameter to this pipeline.")
public class ParameterBool extends CvAbstractScalarParameterStage {

    @Attribute(required = false)
    @Property(description = "Invert the sense of the Boolean, as presented to the user.")
    private boolean invert = false;

    @Attribute(required = false)
    @Property(description = "Default value of the parameter.")
    private boolean defaultValue = false;

    public boolean isInvert() {
        return invert;
    }

    public void setInvert(boolean invert) {
        this.invert = invert;
    }

    public boolean isDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(boolean defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public Object defaultParameterValue() {
        return defaultValue;
    }

    @Override
    public Object appliedValue(CvPipeline pipeline, Object value) {
        if (value instanceof Boolean) {
            return (boolean)value;
        }
        return defaultValue; 
    }

    @Override
    protected Class<?> parameterValueType() {
        return Boolean.class;
    }

    @Override
    public int minimumScalar() {
        return 0;
    }

    @Override
    public int maximumScalar() {
        return 1;
    }

    @Override
    public int convertToScalar(Object value) {
        boolean val = defaultValue;
        if (value instanceof Boolean) {
            val = isInvert() ^ (boolean)value;
        }
        return super.convertToScalar(val ? maximumScalar() : minimumScalar());
    }

    @Override
    public Object convertToValue(int scalar) {
        boolean val = isInvert() ^ (scalar != 0); 
        return val;
    }

    @Override
    public String displayValue(Object value) {
        return (boolean)value ? "On" : "Off";
    }
}
