package org.openpnp.vision.pipeline.stages;

import java.io.File;

import org.opencv.highgui.Highgui;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.simpleframework.xml.Attribute;

/**
 * Save the working image to the specified path. The format is chosen based on the filename's
 * extension.
 */
public class ImageWrite extends CvStage {
    @Attribute
    private File file = new File("");

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        Highgui.imwrite(file.getAbsolutePath(), pipeline.getWorkingImage());
        return null;
    }
}
