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

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Property;
import org.openpnp.vision.pipeline.Stage;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;

@Stage(category = "Image Processing",
        description = "Filter RotatedRects that fit in the template's size, with a tolerance and find the closest RotatedRect to the center of the screen.")

public class ClosestModel extends CvStage {

    @Attribute(required = false)
    @Property(description = "Allow log messages in the log.")
    private boolean log = false;

    @Attribute(required = true)
    @Property(
            description = "Name of a prior stage to load the filter size from. The filter stage should contain a single RotatedRect model.")
    private String filterStageName;

    @Attribute(required = true)
    @Property(description = "Name of a prior stage to load the model from.")
    private String modelStageName;

    @Attribute(required = false)
    @Property(description = "Filter tolerance.")
    private double tolerance = 0.2f;

    @Attribute(required = false)
    @Property(description = "Scale filter by this value.")
    private double scale = 1.0f;

    public boolean isLog() {
        return log;
    }

    public void setLog(boolean log) {
        this.log = log;
    }

    public String getModelStageName() {
        return modelStageName;
    }

    public void setModelStageName(String modelStageName) {
        this.modelStageName = modelStageName;
    }

    public String getFilterStageName() {
        return filterStageName;
    }

    public void setFilterStageName(String filterStageName) {
        this.filterStageName = filterStageName;
    }

    public double getTolerance() {
        return tolerance;
    }

    public void setTolerance(double tolerance) {
        this.tolerance = tolerance;
    }

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {

        Object model = null;
        RotatedRect rrect = null;
        RotatedRect closest = null;
        RotatedRect frect = null;
        double tHeightMax = 0, tHeightMin = 0, tWidthMax = 0, tWidthMin = 0;
        // needed to get the center of the image
        Mat image = pipeline.getWorkingImage();
        // check for the existance of an input model
        if (modelStageName == null || modelStageName.trim()
                                                    .equals("")) {
            // model = (List<?>)pipeline.getWorkingModel();
            return null;

        }
        else {

            model = pipeline.getResult(modelStageName).model;
        }
        if (model == null || image == null) {
            if (log) {
                Logger.info("Null image or model");
            }
            return null;
        }
        // list to keep filtered rotated rects in.
        ArrayList multi = new ArrayList();

        // Check for the existance of a filter
        if (filterStageName != null && !filterStageName.trim()
                                                       .equals("")) {

            CvStage filterStage = pipeline.getStage(filterStageName);
            if (filterStage != null && pipeline.getResult(filterStage) != null) {

                Result filterResult = pipeline.getResult(filterStage);
                if (filterResult.model instanceof RotatedRect) {
                    frect = ((RotatedRect) filterResult.model);
                }
                else if (filterResult.model instanceof List<?>
                        && ((List<?>) filterResult.model).get(0) instanceof RotatedRect) {
                    frect = ((RotatedRect) ((List<?>) filterResult.model).get(0));
                }
                else {
                    // we only handle RotatedRect filters
                }
            }
        }
        if (log) {
            Logger.info("filter rect = " + frect);
        }
        Point screenCenter = new Point(image.size().width / 2.0, image.size().height / 2.0);

        if (frect != null) {
            // calculate filter range
            tHeightMax =
                    Math.max(frect.size.width, frect.size.height) * (1 + tolerance / 2.0) * scale;
            tHeightMin =
                    Math.max(frect.size.width, frect.size.height) * (1 - tolerance / 2.0) * scale;
            tWidthMax =
                    Math.min(frect.size.width, frect.size.height) * (1 + tolerance / 2.0) * scale;
            tWidthMin =
                    Math.min(frect.size.width, frect.size.height) * (1 - tolerance / 2.0) * scale;
            if (log) {
                Logger.info("scale = " + scale + ", tolerance = " + tolerance);
                Logger.info("scaled filter limits = " + Math.round(tWidthMin) + ":"
                        + Math.round(tWidthMax) + " x " + Math.round(tHeightMin) + ":"
                        + Math.round(tHeightMax));
            }
        }
        if (model instanceof RotatedRect) {

            multi.add(model);


        }
        else if (model instanceof List<?> && ((List<?>) model).get(0) instanceof RotatedRect) {

            multi = (ArrayList) model;

        }
        else {
            // we only handle rotated rects here
        }
        if (log && multi.size() == 0) {
            Logger.info("No input model found.");
        }
        // find the closest rotatedRect to the center of the screen
        // Given the oportunity of a loop through the rects and the size of the model represented by
        // the template,
        // also filter those rects that fit in the template's size, with a tolerance

        double distance = 10e8; // a really big number
        double rHeight = 0, rWidth = 0;
        for (RotatedRect r : (ArrayList<RotatedRect>) multi) {
            // Convention here is: larger side = height, smaller side = width
            rHeight = Math.max(r.size.width, r.size.height);
            rWidth = Math.min(r.size.width, r.size.height);
            // filter rects to be template's size +- tolerance
            if (frect != null && (rHeight > tHeightMax || rHeight < tHeightMin || rWidth > tWidthMax
                    || rWidth < tWidthMin)) {
                if (log) {
                    Logger.info("<<< filtered out rect with size=" + r.size
                            + ", because filter size is = " + frect.size.width * scale + "x"
                            + frect.size.height * scale);
                }
                continue;
            }
            // find distance from center
            double dc = Math.sqrt(Math.pow(screenCenter.x - r.center.x, 2)
                    + Math.pow(screenCenter.y - r.center.y, 2));
            if (dc < distance) {
                distance = dc;
                closest = r;
            }
        }
        if (closest != null) {
            rrect = closest.clone();
            // correct input model rrect so that it comes in a more "upright" position, i.e. width <
            // height
            // This convention has to be consistant throughout the pipeline
            if (rrect.size.width > rrect.size.height) {
                rrect.angle += 90.0;
                double tmp = rrect.size.width;
                rrect.size.width = rrect.size.height;
                rrect.size.height = tmp;
            }
            if (log) {
                Logger.info("Delivering model = " + closest);
            }
            ArrayList out = new ArrayList();
            out.add(closest);
            return new Result(null, out);
        }
        if (log) {
            Logger.info("No model was found after filtering");
        }
        return null;
    }
}
