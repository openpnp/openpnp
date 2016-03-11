package org.openpnp.vision.pipeline.stages;

import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;

/**
 * Converts the color of the current working image to the specified conversion. 
 */
public class ModelTest extends CvStage {
    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        return new Result(null, "hi there");
    }
}
