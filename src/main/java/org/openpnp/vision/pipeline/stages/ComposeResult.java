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

import org.opencv.core.Mat;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Property;
import org.openpnp.vision.pipeline.Stage;
import org.simpleframework.xml.Attribute;

@Stage(category = "Image Processing",
        description = "Retrieves the result of a user defined stage, or the working result in the pipeline, when this stage is processed.")

public class ComposeResult extends CvStage {
    @Attribute(required = false)
    @Property(
            description = "Name of a prior stage to retrieve the image from. An empty name will retrieve the working image.")
    // and working model")
    private String imageStageName;

    @Attribute(required = false)
    @Property(description = "Name of a prior stage to retrieve the model from.")
    // and working model")
    private String modelStageName;

    public String getImageStageName() {
        return imageStageName;
    }

    public void setImageStageName(String imageStageName) {
        this.imageStageName = imageStageName;
    }

    public String getModelStageName() {
        return modelStageName;
    }

    public void setModelStageName(String modelStageName) {
        this.modelStageName = modelStageName;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {

        Object model = null;
        Mat image = null;

        if (modelStageName == null || modelStageName.trim()
                                                    .equals("")) {
            // use pipeline.getWorkingModel() when it becomes available
            model = null;
            // return new Result(pipeline.getWorkingImage());
        }
        else {
            model = pipeline.getResult(modelStageName).model;
        }
        if (imageStageName == null || imageStageName.trim()
                                                    .equals("")) {
            image = pipeline.getWorkingImage();
        }
        else {
            image = pipeline.getResult(imageStageName).image;
        }
        if (model == null) {
            return new Result(image);
        }
        return new Result(image, model);
    }
}
