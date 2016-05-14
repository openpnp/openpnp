package org.openpnp.vision.pipeline.stages;

import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.simpleframework.xml.Attribute;

public class ImageRecall extends CvStage {
    @Attribute(required = false)
    private String imageStageName = null;

    public String getImageStageName() {
        return imageStageName;
    }

    public void setImageStageName(String imageStageName) {
        this.imageStageName = imageStageName;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        if (imageStageName == null) {
            return null;
        }
        Result result = pipeline.getResult(imageStageName);
        if (result == null || result.image == null) {
            return null;
        }
        return new Result(result.image.clone());
    }
}
