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

import org.openpnp.gui.support.AreaConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.model.Area;
import org.openpnp.model.AreaUnit;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.spi.Camera;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.pipeline.CvAbstractScalarParameterStage;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.Property;
import org.openpnp.vision.pipeline.Stage;
import org.simpleframework.xml.Attribute;

@Stage(description="Exposes a numeric stage property as an external parameter to this pipeline.")
public class ParameterNumeric extends CvAbstractScalarParameterStage {

    @Attribute(required = false)
    @Property(description = "Minimum value of the parameter.")
    private double minimumValue = 0.0;

    @Attribute(required = false)
    @Property(description = "Maximum value of the parameter.")
    private double  maximumValue = 1.0;

    @Attribute(required = false)
    @Property(description = "Default value of the parameter.")
    private double defaultValue = 0.5;

    enum NumericType {
        Integer(Integer.class),
        Double(Double.class),
        Squared(Double.class), 
        Exponential(Double.class), 
        Millimeters(Length.class), 
        MillimetersToPixels(Length.class), 
        SquareMillimeters(Area.class), 
        SquareMillimetersToPixels(Area.class);

        private Class<?> type;
        NumericType(Class<?> type) {
            this.type = type;
        }
        double fnScalar(double value) {
            switch (this) {
                case Squared:
                case SquareMillimeters:
                case SquareMillimetersToPixels:
                    return Math.sqrt(Math.max(0, value));
                case Exponential:
                    return Math.log(Math.max(0, value));
            }
            return value;
        }
        double fnValue(double value) {
            switch (this) {
                case Squared:
                case SquareMillimeters:
                case SquareMillimetersToPixels:
                    return Math.pow(value, 2);
                case Exponential:
                    return Math.exp(value);
            }
            return value;
        }
        Object asTyped(double value) {
            switch (this) {
                case Integer: 
                    return (int)Math.round(value);
                case Millimeters:
                case MillimetersToPixels:
                    return new Length(value, LengthUnit.Millimeters);
                case SquareMillimeters:
                case SquareMillimetersToPixels:
                    return new Area(value, AreaUnit.SquareMillimeters);
                default:
                    return (Double)value;
            }
        }
    }

    @Attribute(required = false)
    @Property(description = "Type and unit of numeric.")
    private NumericType numericType = NumericType.Double;

    public double getMinimumValue() {
        return minimumValue;
    }

    public void setMinimumValue(double minimumValue) {
        this.minimumValue = minimumValue;
    }

    public double getMaximumValue() {
        return maximumValue;
    }

    public void setMaximumValue(double maximumValue) {
        this.maximumValue = maximumValue;
    }

    public double getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(double defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public Object defaultParameterValue() {
        return defaultValue;
    }

    @Override
    public Object appliedValue(CvPipeline pipeline, Object value) {
        switch (numericType) {
            case MillimetersToPixels:
            case SquareMillimetersToPixels:
                Camera camera = (Camera) pipeline.getProperty("camera");
                if (value instanceof Length) {
                    return VisionUtils.toPixels((Length) value, camera);
                }
                else if (value instanceof Area) {
                    return VisionUtils.toPixels((Area) value, camera);
                }
        }
        return asNumeric(value); 
    }

    @Override
    protected Class<?> parameterValueType() {
        return numericType.type;
    }

    public NumericType getNumericType() {
        return numericType;
    }

    public void setNumericType(NumericType numericType) {
        this.numericType = numericType;
    }

    @Override
    public int minimumScalar() {
        return 0;
    }

    @Override
    public int maximumScalar() {
        return 1000;
    }

    @Override
    public int convertToScalar(Object value) {
        double num = asNumeric(value);
        // map it to 0..1
        double ratio = (numericType.fnScalar((double)num) - numericType.fnScalar(getMinimumValue()))/(numericType.fnScalar(getMaximumValue()) - numericType.fnScalar(getMinimumValue()));
        int scalar = minimumScalar() + (int)Math.round(ratio*(maximumScalar() - minimumScalar()));
        //Logger.trace(String.valueOf(value)+ " -> scalar "+scalar);
        return super.convertToScalar(scalar);
    }

    protected double asNumeric(Object value) {
        double num;
        if (value instanceof Double) {
            num = (double)value;
        }
        else if (value instanceof Integer) {
            num = (double)(int)value;
        }
        else if (value instanceof Length) {
            num = ((Length)value).convertToUnits(LengthUnit.Millimeters).getValue();
        }
        else if (value instanceof Area) {
            num = ((Area)value).convertToUnits(AreaUnit.SquareMillimeters).getValue();
        }
        else {
            num = getDefaultValue();
        }
        return num;
    }

    @Override
    public Object convertToValue(int scalar) {
        double ratio = (double)(scalar - minimumScalar())/(maximumScalar() - minimumScalar());
        double value = numericType.fnValue(numericType.fnScalar(getMinimumValue()) + ratio*(numericType.fnScalar(getMaximumValue()) - numericType.fnScalar(getMinimumValue())));
        //Logger.trace("scalar "+scalar+" -> "+String.valueOf(value));
        return numericType.asTyped(value);
    }

    @Override
    public String displayValue(Object value) {
        if (value instanceof Length) {
            return new LengthConverter().convertForward(((Length)value));
        }
        else if (value instanceof Area) {
            return new AreaConverter("%.4f").convertForward(((Area)value));
        }
        return String.valueOf(value);
    }
}
