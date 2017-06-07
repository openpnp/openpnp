/**
 * code written by dzach, @ https://github.com/dzach
 */
package org.openpnp.vision.pipeline.stages;
 
import java.io.File;
import org.opencv.core.*;
import org.opencv.highgui.Highgui;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.Property;
import org.openpnp.vision.pipeline.Stage;
import org.simpleframework.xml.Attribute;

@Stage(category = "Image Processing",
        description = "Write a template image to disk given a user defined file name, or infer the image's name from the id of the part loaded in the feeder and write it to the path defined by the user.")

public class WriteTemplateImage extends CvStage {

    @Attribute(required = false)
    @Property(description = "Name of the template image to write, or name of a directory where the image should be written with a name inferred by the part ID.")
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
      /**
      * Write a template image to the path set by the user.
      * If the path ends in 'extension' then the image is written directly from the path.
      * If not, then the path is considered a directory for writing template images, 
      * and the template file name to write is deduced by the part ID loaded in the feeder of this pipeline
      */
      
      if (templateFile == null || templateFile.trim().equals("")) {
        return null;
      }
      String msg = "Could not find a png template image.";
      File file = null;

      if (templateFile.endsWith(extension)) {

        file = new File(templateFile);

      } else {

        // path is assumed to be a directory containing template images
        // check if a part ID can be found from the feeder this pipeline may belong to

        if (pipeline.getFeeder() == null || pipeline.getFeeder().getPart() == null) {
          
          throw new Exception(msg + " No part in feeder, either.");
        }
        // make sure the specified dir exists, otherwise create it
        new File(templateFile).mkdirs();
        
        if (!templateFile.endsWith(File.separator)) {
          
          templateFile += File.separator;
        }
        templateFile +=  pipeline.getFeeder().getPart().getId() + extension;
        file = new File(templateFile);
      }
      // Write template image to disk
      Highgui.imwrite(file.getAbsolutePath(), pipeline.getWorkingImage());

      return null;
    }
}