package org.openpnp.vision.pipeline.stages;

import java.io.File;

import org.opencv.imgcodecs.Imgcodecs;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Property;
import org.openpnp.vision.pipeline.Stage;
import org.simpleframework.xml.Attribute;

@Stage(
  category   ="Image Processing", 
  description="Replace the working image with the image loaded from a given path.")
  
public class ImageRead extends CvStage {
    @Attribute
    @Property(description="Absolute path of the image file to read.")
    private File file = new File("");

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        if (!file.exists()) {
            return null;
        }
        return new Result(Imgcodecs.imread(file.getAbsolutePath()));
    }
}
