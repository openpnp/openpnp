package org.openpnp.vision.pipeline.stages;

import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;

/**
 * Programmatic stage which simply sets it's model to the object passed in on the constructor. This
 * is used for setting up pipelines before they are edited by the user. 
 */
public class SetModel extends CvStage {
    private Object model;
    
    public SetModel(Object model) {
        this.model = model;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        return new Result(null, model);
    }
}
