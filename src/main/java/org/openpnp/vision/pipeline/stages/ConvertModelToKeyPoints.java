package org.openpnp.vision.pipeline.stages;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.features2d.KeyPoint;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.CvStage.Result.TemplateMatch;
import org.openpnp.vision.pipeline.Stage;
import org.simpleframework.xml.Attribute;

@Stage(description = "Convert a variety of built in types to KeyPoints. Currently handles Points, Circles, TemplateMatches, and RotatedRects. The center point of each is stored, along with a score and diameter where appropriate. If the input model is a single value the result will be a single value. If the input is a List the result will be a List.")
public class ConvertModelToKeyPoints extends CvStage {
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
            List<KeyPoint> points = new ArrayList<>();
            for (Object o : ((List) model)) {
                points.add(convertToKeyPoint(o));
            }
            return new Result(null, points);
        }
        else {
            return new Result(null, convertToKeyPoint(model));
        }
    }

    private static KeyPoint convertToKeyPoint(Object keyPointHolder) throws Exception {
        if (keyPointHolder instanceof Result.Circle) {
            Result.Circle circle = (Result.Circle) keyPointHolder;
            return new KeyPoint((float) circle.x, (float) circle.y, (float) circle.diameter);
        }
        else if (keyPointHolder instanceof Point) {
            Point point = (Point) keyPointHolder;
            return new KeyPoint((float) point.x, (float) point.y, 0);
        }
        else if (keyPointHolder instanceof RotatedRect) {
            RotatedRect rotatedRect = (RotatedRect) keyPointHolder;
            return new KeyPoint((float) rotatedRect.center.x, (float) rotatedRect.center.y, 0,
                    (float) rotatedRect.angle);
        }
        else if (keyPointHolder instanceof TemplateMatch) {
            TemplateMatch templateMatch = (TemplateMatch) keyPointHolder;
            float width = (float) templateMatch.width;
            float height = (float) templateMatch.height;
            float x = (float) templateMatch.x + (width / 2);
            float y = (float) templateMatch.y + (height / 2);
            float score = (float) templateMatch.score;
            return new KeyPoint(x, y, width + height, score);
        }
        else {
            throw new Exception("Don't know how to convert " + keyPointHolder + "to KeyPoint.");
        }
    }
}
