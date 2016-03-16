package org.openpnp.vision.pipeline.stages;

import java.lang.reflect.Method;

import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.simpleframework.xml.Attribute;

import com.l2fprod.common.beans.BeanUtils;

/**
 * Get an object from a previous stage's model by querying the named property. The resulting
 * object is stored as this stage's model. 
 */
public class ReadModelProperty extends CvStage {
    @Attribute
    private String modelStageName;
    
    @Attribute
    private String propertyName;
    
    public String getModelStageName() {
        return modelStageName;
    }

    public void setModelStageName(String modelStageName) {
        this.modelStageName = modelStageName;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        if (modelStageName == null) {
            throw new Exception("modelStageName is required.");
        }
        
        if (propertyName == null) {
            throw new Exception("propertyName is required.");
        }
        
        Result result = pipeline.getResult(modelStageName);
        Object model = result.model;

        Method method = BeanUtils.getReadMethod(model.getClass(), propertyName);
        Object value = method.invoke(model);
        
        return new Result(null, value);
    }
}
