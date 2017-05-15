package org.openpnp.vision.pipeline.stages;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.features2d.KeyPoint;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Stage;
import org.simpleframework.xml.Attribute;

@Stage(description="Convert a variety of built in types to Points. Currently handles KeyPoints, Circles and RotatedRects. The center point of each is stored. If the input model is a single value the result will be a single value. If the input is a List the result will be a List.")
public class ConvertModelToPoints extends CvStage {
    @Attribute(required = false)
    private String modelStageName;
    
    public String getModelStageName() {
        return modelStageName;
    }

    public void setModelStageName(String modelStageName) {
        this.modelStageName = modelStageName;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        if (modelStageName == null) {
            return null;
        }
        Result result = pipeline.getResult(modelStageName);
        if (result == null || result.model == null) {
            return null;
        }
        Object model = result.model;
        if (model instanceof List) {
            List<Point> points = new ArrayList<>();
            for (Object o : ((List) model)) {
                points.add(convertToPoint(o));
            }
            return new Result(null, points);
        }
        else {
            return new Result(null, convertToPoint(model));
        }
    }

    private static Point convertToPoint(Object pointHolder) throws Exception {
        if (pointHolder instanceof Result.Circle) {
            Result.Circle circle = (Result.Circle) pointHolder;
            return new Point(circle.x, circle.y);
        }
        else if (pointHolder instanceof KeyPoint) {
            KeyPoint keyPoint = (KeyPoint) pointHolder;
            return keyPoint.pt;
        }
        else if (pointHolder instanceof RotatedRect) {
            RotatedRect rotatedRect = (RotatedRect) pointHolder;
            return rotatedRect.center;
        }
        else {
            throw new Exception("Don't know how to convert " + pointHolder + "to Point.");
        }
    }
}
