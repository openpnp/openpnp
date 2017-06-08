/**
 * code written by dzach, @ https://github.com/dzach
 */
package org.openpnp.vision.pipeline.stages;

import java.io.File;

import org.opencv.core.*;
import org.opencv.highgui.Highgui;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Property;
import org.openpnp.vision.pipeline.Stage;
import org.simpleframework.xml.Attribute;

@Stage(category = "Image Processing",
        description = "Read a template image from disk given a user defined file name, or infer the image's name from the id of the part loaded in the feeder and load it from a path defined by the user.")

public class ReadTemplateImage extends CvStage {

    @Attribute(required = false)
    @Property(description = "Name of a template image, or name of a directory where an image can be found with a name inferred by the part ID.")
    private String templateFile;
    
    public String getTemplateFile() {
        return templateFile;
    }

    public void setTemplateFile(String templateFile) {
        this.templateFile = templateFile;
    }

    @Attribute(required = false)
    @Property(description = "Extension of image file. Defaults to '.png'.")
    private String extension = ".png";
    
    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
      
      if (templateFile == null || templateFile.trim().equals("")) {
        return null;
      }
      // an empty extension has no meaning and confuses the user believing it'll be the default
      if (extension.trim().equals("")) {
        extension = ".png";
      }
      File file = null;
      String filepath = templateFile;
      /**
      * Read a template image from the path set by the user.
      * If the path ends in 'extension' then the image is read directly from the path
      * If not, then the path is considered a directory containing template images, 
      * and the template file name is deduced by the partID of the part loaded in the feeder of this pipeline
      */
      if (!filepath.endsWith(extension)) {
        // path is assumed to be a directory containing template images
        if (pipeline.getFeeder() == null || pipeline.getFeeder().getPart() == null) {
          return null;
        }
        if (!filepath.endsWith(File.separator)) {
          filepath += File.separator;
        }
        filepath += pipeline.getFeeder().getPart().getId() + extension;

      }
      file = new File(filepath);
      if (!file.exists()) {
        return null;
      }
      // Read template image from disk
      Mat templateImage = Highgui.imread(file.getAbsolutePath());

      int width = (int) templateImage.size().width;
      int height = (int) templateImage.size().height;
      if (width == 0 && height == 0) {
        return null;
      }
      return new Result(templateImage, new RotatedRect(new Point(width / 2, height/2), new Size(width, height), (double) 0));
    }
}
