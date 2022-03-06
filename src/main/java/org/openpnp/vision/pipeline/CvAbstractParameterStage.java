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

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.simpleframework.xml.Attribute;

public abstract class CvAbstractParameterStage extends CvStage {

    @Attribute(required = false)
    @Property(description = "Label of the parameter.")
    protected String parameterLabel = null;

    @Attribute(required = false)
    @Property(description = "Description of the parameter (for tooltip).")
    protected String parameterDescription = null;

    @Attribute(required = false)
    @Property(description = "Name of the stage to be controlled by the parameter.")
    protected String stageName = null;

    @Attribute(required = false)
    @Property(description = "Name of the property to be controlled by the parameter.")
    protected String propertyName = null;

    @Attribute(required = false)
    @Property(description = "Name of the stage to preview the effect of a parameter change.")
    protected String effectStageName = null;

    @Attribute(required = false)
    @Property(description = "Preview the pipeline result image.")
    protected boolean previewResult = true;

    public String getParameterLabel() {
        return parameterLabel;
    }

    public void setParameterLabel(String parameterLabel) {
        this.parameterLabel = parameterLabel;
    }

    public String getStageName() {
        return stageName;
    }

    public String getParameterDescription() {
        return parameterDescription;
    }

    public void setParameterDescription(String parameterDescription) {
        this.parameterDescription = parameterDescription;
    }

    public void setStageName(String stageName) {
        this.stageName = stageName;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getEffectStageName() {
        return effectStageName;
    }

    public void setEffectStageName(String effectStageName) {
        this.effectStageName = effectStageName;
    }

    public boolean isPreviewResult() {
        return previewResult;
    }

    public void setPreviewResult(boolean previewResult) {
        this.previewResult = previewResult;
    }

    public abstract Object appliedValue(CvPipeline pipeline, Object value);
    public abstract Object defaultParameterValue();
    public abstract String displayValue(Object value);
    protected abstract Class<?> parameterValueType();

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        if (parameterName() == null) {
            throw new Exception("Please assign a stable name to the stage. This name will be used as parameter name "
                    + "and should not be changed later, or assigned data will be lost. Names starting with digits are not allowed.");
        }
        if (parameterLabel == null || parameterLabel.isEmpty()) {
            return null;
        }
        if (stageName == null || stageName.isEmpty()) {
            return null;
        }
        if (propertyName == null || propertyName.isEmpty()) {
            return null;
        }
        CvStage stage = pipeline.getStage(stageName);
        if (stage == null) {
            throw new Exception("Stage \""+stageName+"\" not found");
        }
        Object value = pipeline.getProperty(parameterName());
        if (value == null) {
            value = defaultParameterValue();
        }
        invokeSetter(stage, propertyName, appliedValue(pipeline, value));
        return null;
    }

    protected void invokeSetter(Object obj, String propertyName, Object variableValue) 
            throws IntrospectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        PropertyDescriptor pd;
        pd = new PropertyDescriptor(propertyName, obj.getClass());
        // Implicitly cast double to int.
        if (pd.getPropertyType() == int.class
                && variableValue instanceof Double) {
            variableValue = (int)Math.round((double)variableValue);
        }
        else if (pd.getPropertyType() == Integer.class
                && variableValue instanceof Double) {
            variableValue = (Integer)(int)Math.round((double)variableValue);
        }
        Method setter = pd.getWriteMethod();
        setter.invoke(obj, variableValue);
    }

    /**
     * Reset the assigned parameter value back to the default, to keep the pipeline unchanged.
     * 
     * @param pipeline
     */
    void resetParameterValue(CvPipeline pipeline) {
        CvStage stage = pipeline.getStage(stageName);
        if (stage != null) {
            try {
                invokeSetter(stage, propertyName, appliedValue(pipeline, defaultParameterValue()));
            }
            catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
                    | IntrospectionException e) {
            }
        }
    }

    /**
     * @return The parameter name, which is the stage name, but properly validated. Otherwise null is returned.
     */
    public String parameterName() {
        if (getName() == null || getName().isEmpty()) {
            return null;
        }
        if ("-0123456789".contains(getName().substring(0, 1))) {
            // Do not allow a number as first letter, such as when the sequential number given by the Editor is still present.
            return null;
        }
        return getName();
    }
}
