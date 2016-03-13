package org.openpnp.vision.pipeline.stages;

import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.simpleframework.xml.Attribute;

public class ImageRecall extends CvStage {
    @Attribute(required = false)
    private String modelStageName = null;

    public String getModelStageName() {
        return modelStageName;
    }

    public void setModelStageName(String modelStageName) {
        this.modelStageName = modelStageName;
    }

    @Override
    // TODO: This isn't working, probably the original image is getting overwritten somehow.
    public Result process(CvPipeline pipeline) throws Exception {
        if (modelStageName == null) {
            return null;
        }
        Result result = pipeline.getResult(modelStageName);
        if (result == null || result.image == null) {
            return null;
        }
        return new Result(result.image);
    }
}
