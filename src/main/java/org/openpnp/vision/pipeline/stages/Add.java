package org.openpnp.vision.pipeline.stages;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Stage;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

@Stage(description="Adds two images together.")
public class Add extends CvStage {
    @Element(required = false)

    @Attribute(required = false)
    private String firstStageName = null;

    @Attribute(required = false)
    private String secondStageName = null;
    
    public String getFirstStageName() {
        return firstStageName;
    }

    public void setFirstStageName(String firstStageName) {
        this.firstStageName = firstStageName;
    }

    public String getSecondStageName() {
        return secondStageName;
    }

    public void setSecondStageName(String secondStageName) {
        this.secondStageName = secondStageName;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        if (firstStageName == null) {
            return null;
        }
        if (secondStageName == null) {
            return null;
        }
        // TODO STOPSHIP memory?
        Mat first = pipeline.getResult(firstStageName).image;
        Mat second = pipeline.getResult(secondStageName).image;
        
        Mat out = new Mat();
        Core.add(first, second, out);
        return new Result(out);
    }
}
