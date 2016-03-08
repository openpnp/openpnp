package org.openpnp.vision.pipeline.stages;

import org.opencv.highgui.Highgui;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.simpleframework.xml.Attribute;

/**
 * Replace the working image with the image loaded from a given path.
 */
public class LoadImage extends CvStage {
    @Attribute
    private String path;

    public String getPath() {
        return path;
    }

    public LoadImage setPath(String path) {
        this.path = path;
        return this;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        return new Result(Highgui.imread(path));
    }
}
