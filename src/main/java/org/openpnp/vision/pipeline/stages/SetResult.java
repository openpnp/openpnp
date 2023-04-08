package org.openpnp.vision.pipeline.stages;

import org.opencv.core.Mat;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;

public class SetResult extends CvStage {
    private Mat image;
    private Object model;
    
    public SetResult() {
        this(null, null);
    }
    
    public SetResult(Mat image, Object model) {
        this.image = image;
        this.model = model;
    }
    
    public Mat getImage() {
        return image;
    }

    public void setImage(Mat image) {
        this.image = image;
    }

    public Object getModel() {
        return model;
    }

    public void setModel(Object model) {
        this.model = model;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        return new Result(image.clone(), model);
    }
}
