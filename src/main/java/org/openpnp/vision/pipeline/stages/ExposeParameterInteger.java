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

import org.openpnp.vision.pipeline.CvAbstractParamStage;
import org.openpnp.vision.pipeline.Property;
import org.openpnp.vision.pipeline.Stage;
import org.simpleframework.xml.Attribute;

@Stage(description="Exposes an Integer stage property as an external parameter to this pipeline.")
public class ExposeParameterInteger extends CvAbstractParamStage {

    @Attribute(required = false)
    @Property(description = "Minimum value of the parameter.")
    private int minimumValue = 0;

    @Attribute(required = false)
    @Property(description = "Maximum value of the parameter.")
    private int maximumValue = 255;

    @Attribute(required = false)
    @Property(description = "Default value of the parameter.")
    private int defaultValue = 128;

    enum ScaleFunction {
        ScaleNormal,
        ScaleReverse,
        ScaleSquared
    };

    @Attribute(required = false)
    @Property(description = "The transformation function applied to the parameter, before it is assigned to the stage property.")
    private ScaleFunction scaleFuntion = ScaleFunction.ScaleNormal;

    public int getMinimumValue() {
        return minimumValue;
    }

    public void setMinimumValue(int minimumValue) {
        this.minimumValue = minimumValue;
    }

    public int getMaximumValue() {
        return maximumValue;
    }

    public void setMaximumValue(int maximumValue) {
        this.maximumValue = maximumValue;
    }

    @Override
    public Integer getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Integer defaultValue) {
        this.defaultValue = defaultValue;
    }

    public ScaleFunction getScaleFuntion() {
        return scaleFuntion;
    }

    public void setScaleFuntion(ScaleFunction scaleFuntion) {
        this.scaleFuntion = scaleFuntion;
    }

    @Override
    protected Class<?> getParameterValueType() {
        return Integer.class;
    }

    @Override
    protected
    Object transformValue(Object value) {
        switch (getScaleFuntion()) {
            case ScaleReverse:
                return getMinimumValue() + getMaximumValue() - (int)value;
            case ScaleSquared:
                return (int)value*(int)value;
            default:
                return value;
        }
    }
}
