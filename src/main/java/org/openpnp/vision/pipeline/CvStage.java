package org.openpnp.vision.pipeline;

import org.opencv.core.Mat;
import org.simpleframework.xml.Attribute;

public abstract class CvStage {
    @Attribute
    private String name;
    
    public abstract Result process(CvPipeline pipeline) throws Exception;
    
    public String getName() {
        return name;
    }

    public CvStage setName(String name) {
        this.name = name;
        return this;
    }

    public static class Result {
        final public Mat image;
        final public Object model;
        
        public Result(Mat image, Object model) {
            this.image = image;
            this.model = model;
        }
        
        public Result(Mat image) {
            this(image, null);
        }
    }
}
