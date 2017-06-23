/**
 * code written by dzach, @ https://github.com/dzach
 */
package org.openpnp.vision.pipeline.stages;

import java.io.File;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Camera;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Property;
import org.openpnp.vision.pipeline.Stage;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;

@Stage(category = "Image Processing",
        description = "Read a template image from disk given a user defined file name, or infer the image's name from the id of the part loaded in the feeder and load it from a path defined by the user.")

public class ReadTemplateImage extends CvStage {

    @Attribute(required = false)
    @Property(
            description = "Name of a template image, or name of a directory where an image can be found with a name inferred from part or package ID.")
    private String templateFile;

    @Attribute(required = false)
    @Property(description = "Extension of image file. Defaults to '.png'.")
    private String extension = ".png";

    @Attribute(required = false)
    @Property(description = "Enable logging.")
    private boolean log = false;

    public String getTemplateFile() {
        return templateFile;
    }

    public void setTemplateFile(String templateFile) {
        this.templateFile = templateFile;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public boolean isLog() {
        return log;
    }

    public void setLog(boolean log) {
        this.log = log;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {

        // an empty extension has no meaning and confuses the user believing it'll be the default
        if (extension.trim()
                     .equals("")) {
            extension = ".png";
        }
        File file = null;
        String filepath = templateFile;
        String filename = null;
        double width = 0;
        double height = 0;
        /**
         * Read a template image from the default path, or a path set by the user. If the path ends
         * in 'extension' then the image is read directly from the path If not, then the path is
         * considered a directory containing template images, and the template file name is deduced
         * by the part ID or the package ID of the part loaded in the feeder of this pipeline
         */
        // default file location
        if (filepath == null || filepath.trim()
                                        .equals("")) {
            try {
                filepath = Configuration.get()
                                        .getConfigurationDirectory()
                                        .toString();
            }
            catch (Throwable e) {
                Logger.debug(e.getMessage() + " Now trying root path.");
            }
            filepath += File.separator + "templates";
        }
        if (filepath.endsWith(extension)) {
            // user defined file path
            file = new File(filepath);
            if (log) {
                Logger.info("Using user defined template image.");
            }
        }
        else {
            // path is assumed to be a directory containing template images
            if (pipeline.getFeeder() == null || pipeline.getFeeder()
                                                        .getPart() == null) {
                if (log) {
                    Logger.info(
                            "No feeder, part, or useable templateFile found. Cannot figure out part name.");
                }
                return null;
            }
            if (!filepath.endsWith(File.separator)) {
                filepath += File.separator;
            }
            filename = filepath + pipeline.getFeeder()
                                          .getPart()
                                          .getId()
                    + extension;
            file = new File(filename);
            if (!file.exists()) {
                // try the package id
                filename = filepath + pipeline.getFeeder()
                                              .getPart()
                                              .getPackage()
                                              .getId()
                        + extension;
                file = new File(filename);
                if (!file.exists()) {
                    // If package body dimensions are set, use them as a template.
                    // As is, useful only for non-polarized rectangular parts
                    // TODO: it would be best if we could define a package outline, e.g. as a
                    // polygon
                    // and use that to draw the part and match templates
                    if (pipeline.getFeeder()
                                .getPart()
                                .getPackage()
                                .getFootprint() != null) {
                        width = pipeline.getFeeder()
                                        .getPart()
                                        .getPackage()
                                        .getFootprint()
                                        .getBodyWidth();
                        height = pipeline.getFeeder()
                                         .getPart()
                                         .getPackage()
                                         .getFootprint()
                                         .getBodyHeight();
                        if (width == 0 || height == 0) {
                            if (log) {
                                Logger.info("Package body dimensions are not set.");
                            }
                            // can't create 0 sized template
                            return null;
                        }
                        // portrait mode
                        if (width > height) {
                            double tmp = width;
                            width = height;
                            height = tmp;
                        }
                        // get length conversion value from camera
                        Camera camera = pipeline.getCamera();
                        width /= camera.getUnitsPerPixel()
                                       .getX();
                        height /= camera.getUnitsPerPixel()
                                        .getY();
                        // create a white rect image as the template
                        Mat templateImage = new Mat((int) height, (int) width, CvType.CV_8UC3);
                        templateImage.setTo(new Scalar(255, 255, 255));
                        // create a model
                        RotatedRect rrect = new RotatedRect(new Point(width / 2, height / 2),
                                new Size(width, height), 0.0);
                        if (log) {
                            Logger.info("Using package body as a template.");
                        }
                        // that's all we can do for now
                        return new Result(templateImage, rrect);
                    }
                    else {
                        return null;
                    }
                }
                else {
                    if (log) {
                        Logger.info("Using package template image.");
                    }
                }
            }
            else {
                if (log) {
                    Logger.info("Using part template image.");
                }
            }
        }
        // Read template image from disk
        Mat templateImage = Highgui.imread(file.getAbsolutePath());

        width = templateImage.size().width;
        height = templateImage.size().height;
        if (width == 0.0 && height == 0.0) {
            return null;
        }
        return new Result(templateImage, new RotatedRect(new Point(width / 2, height / 2),
                new Size(width, height), (double) 0));
    }
}
