package org.openpnp.vision.pipeline.stages;

import org.opencv.highgui.Highgui;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.simpleframework.xml.Attribute;

/**
 * Save the working image to the specified path. The format is chosen based
 * on the filename's extension.
 */
public class SaveImage extends CvStage {
    @Attribute
    private String path;

    public String getPath() {
        return path;
    }

    public SaveImage setPath(String path) {
        this.path = path;
        return this;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        Highgui.imwrite(path, pipeline.getWorkingImage());
        return null;
    }
}
