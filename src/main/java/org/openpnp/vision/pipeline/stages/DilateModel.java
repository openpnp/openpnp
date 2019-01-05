package org.openpnp.vision.pipeline.stages;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.RotatedRect;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Property;
import org.openpnp.vision.pipeline.Stage;
import org.simpleframework.xml.Attribute;

@Stage(category = "Image Processing",
        description = "Dilate or contract a model by a given number of pixels.")

public class DilateModel extends CvStage {

    @Attribute(required = false)
    @Property(description = "Name of stage to input model data from.")
    private String modelStageName = null;

    @Attribute(required = false)
    @Property(description = "Dilate the model by given pixels. Negative values contract the model.")
    private int dilate = 0;

    public String getModelStageName() {
        return modelStageName;
    }

    public void setModelStageName(String modelStageName) {
        this.modelStageName = modelStageName;
    }

    public int getDilate() {
        return dilate;
    }

    public void setDilate(int dilate) {
        this.dilate = dilate;
    }

    private RotatedRect dilateRotatedRect(RotatedRect r) {
        RotatedRect rect = new RotatedRect();
        rect.center = r.center;
        rect.angle = r.angle;
        rect.size.width = r.size.width + dilate * 2;
        rect.size.height = r.size.height + dilate * 2;
        return rect;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        if (modelStageName == null) {
            throw new Exception("Stage name for model must be specified.");
        }

        Result inStage = pipeline.getResult(modelStageName);

        if (inStage.model instanceof RotatedRect) {
            // just one RotatedRect
            RotatedRect model = dilateRotatedRect((RotatedRect) inStage.model);
            return new Result(pipeline.getWorkingImage(), model);

        }
        else if (inStage.model instanceof Result.Circle) {
            // just one circle
            Result.Circle circle = (Result.Circle) inStage.model;
            // do the expansion by increasing the diameter
            Result.Circle model =
                    new Result.Circle(circle.x, circle.y, circle.diameter + dilate * 2);
            return new Result(pipeline.getWorkingImage(), model);

        }
        else if (inStage.model instanceof List<?>) {
            // we've got multiple Circles or RotatedRects
            List multi = (List) inStage.model;
            if (multi.get(0) instanceof Result.Circle) {
                // a collection of circles
                ArrayList<Result.Circle> model = new ArrayList<Result.Circle>();
                for (int i = 0; i < multi.size(); i++) {
                    Result.Circle circle = (Result.Circle) multi.get(i);
                    // dilate each circle by altering its diameter
                    model.add(new Result.Circle(circle.x, circle.y, circle.diameter + dilate * 2));
                }
                return new Result(pipeline.getWorkingImage(), model);

            }
            else if (multi.get(0) instanceof RotatedRect) {
                // a collection of Rotatedmulti
                ArrayList<RotatedRect> model = new ArrayList<RotatedRect>();
                for (int i = 0; i < multi.size(); i++) {
                    // dilate each rotated rect
                    model.add(dilateRotatedRect((RotatedRect) multi.get(i)));
                }
                return new Result(pipeline.getWorkingImage(), model);
            }
        }
        return new Result(pipeline.getWorkingImage(), inStage.model);
    }
}
