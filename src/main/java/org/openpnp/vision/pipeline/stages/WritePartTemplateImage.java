/*
 * Copyright (C) 2017 dzach, @ https://github.com/dzach
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.vision.pipeline.stages;

import java.io.File;

import org.opencv.highgui.Highgui;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Feeder;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Property;
import org.openpnp.vision.pipeline.Stage;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;

@Stage(category = "Image Processing",
        description = "Write a template image to disk given a user defined file name, or infer the image's name from the id of the part loaded in the feeder and write it to the path defined by the user.")

public class WritePartTemplateImage extends CvStage {

    @Attribute(required = false)
    @Property(
            description = "Name of the template image to write, or name of a directory where the image should be written with a name inferred from the part ID.")
    private String templateFile;

    @Attribute(required = false)
    @Property(description = "Extension of image file. Defaults to '.png'.")
    private String extension = ".png";

    @Attribute(required = false)
    @Property(description = "Write image as a package template.")
    private boolean asPackage = false;

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

    public boolean isAsPackage() {
        return asPackage;
    }

    public void setAsPackage(boolean asPackage) {
        this.asPackage = asPackage;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        /**
         * Write a template image to the default path or the path set by the user. If the path ends
         * in 'extension' then the image is written directly from the path. If not, then the path is
         * considered a directory for writing template images, and the template file name to write
         * is deduced by the part ID loaded in the feeder of this pipeline
         */

        File file = null;
        String filepath = templateFile;
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
            filepath += File.separator + "templates" + File.separator + "new";
        }
        // user defined file path
        if (filepath.endsWith(extension)) {

            file = new File(filepath);
            if (file.getParentFile() != null) {
                // make sure the directory exists, if not, create it
                file.getParentFile()
                    .mkdirs();
            }

        }
        else {

            // path is assumed to be a directory containing template images
            // check if a part ID can be found from the feeder this pipeline may belong to
            Feeder feeder = (Feeder) pipeline.getProperty("feeder");
            if (feeder == null || feeder.getPart() == null) {

                throw new Exception(
                        "No part in feeder. Please provide create template image file path.");
            }
            // make sure the specified dir exists, otherwise create it
            new File(filepath).mkdirs();

            if (!filepath.endsWith(File.separator)) {

                filepath += File.separator;
            }
            if (asPackage) {
                filepath += feeder.getPart()
                                  .getPackage()
                                  .getId()
                        + extension;
            }
            else {
                filepath += feeder.getPart()
                                  .getId()
                        + extension;
            }
            file = new File(filepath);
        }
        // Write template image to disk
        Highgui.imwrite(file.getAbsolutePath(), pipeline.getWorkingImage());

        return null;
    }
}
