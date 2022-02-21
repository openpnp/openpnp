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
    @Property(description = "Name of the parameter.")
    protected String parameterName = null;

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

    public String getParameterName() {
        return parameterName;
    }

    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
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

    public abstract Object getDefaultValue();
    public abstract String displayValue(Object value);
    protected abstract Class<?> getParameterValueType();

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        if (parameterName == null || parameterName.isEmpty()) {
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
        Object value = getPossiblePipelinePropertyOverride(getDefaultValue(), pipeline, getParameterName(), 
                getParameterValueType());
        invokeSetter(stage, propertyName, value);
        return null;
    }

    protected void invokeSetter(Object obj, String propertyName, Object variableValue) throws IntrospectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        PropertyDescriptor pd;
        pd = new PropertyDescriptor(propertyName, obj.getClass());
        Method setter = pd.getWriteMethod();
        setter.invoke(obj, variableValue);
    }

    protected Object invokeGetter(Object obj, String variableName) throws IntrospectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        PropertyDescriptor pd = new PropertyDescriptor(variableName, obj.getClass());
        Method getter = pd.getReadMethod();
        return getter.invoke(obj);
    }

    void resetValue(CvPipeline pipeline) {
        CvStage stage = pipeline.getStage(stageName);
        if (stage != null) {
            try {
                invokeSetter(stage, propertyName, getDefaultValue());
            }
            catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
                    | IntrospectionException e) {
            }
        }
    }

}
