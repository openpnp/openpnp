package org.openpnp.vision.pipeline.stages;

import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;

public class ModelSet extends CvStage {
    final Object model;
    
    public ModelSet(Object model) {
        this.model = model;
    }
    
    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        return new Result(null, model);
    }
}
