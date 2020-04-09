package org.openpnp.vision.pipeline.stages;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.KeyPoint;
import org.opencv.core.RotatedRect;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.CvStage.Result.TemplateMatch;
import org.openpnp.vision.pipeline.Property;
import org.openpnp.vision.pipeline.Stage;
import org.simpleframework.xml.Attribute;

@Stage(description="Result model coordinates obtained from images gone through AfficeWarp are not usable as camera "
        + "coordinates. This stage applies the proper reverse Affine Transformation to reconstruct camera coordinates. "
        + "The stage currently supports Lists or single instances of Circles, RotatedRects and KeyPoints. "
        + "For transformations with stretch and shear, some of the model properties are approximated. ")
public class AffineUnwarp extends CvStage {

    @Attribute(required = false)
    @Property(description = "Stage name of the AffineWarp.")
    private String warpStageName = null;

    @Attribute(required = false)
    @Property(description = "Stage name of the results to unwarp. If empty, takes the working model.")
    private String resultsStageName = null;

    public String getWarpStageName() {
        return warpStageName;
    }

    public void setWarpStageName(String warpStageName) {
        this.warpStageName = warpStageName;
    }

    public String getResultsStageName() {
        return resultsStageName;
    }

    public void setResultsStageName(String resultsStageName) {
        this.resultsStageName = resultsStageName;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        if (warpStageName == null || warpStageName.isEmpty()) {
            return null;
        }
        Result warpResult = pipeline.getResult(warpStageName);
        if (warpResult == null || warpResult.getModel() == null) {
            return null;
        }
        if (! (warpResult.getModel() instanceof AffineTransform)) {
            throw new Exception("Stage "+warpStageName+" result model is not an AffineTransform.");
        }
        AffineTransform transform = (AffineTransform)warpResult.getModel();
        AffineTransform transformInverse = transform.createInverse();

        Object model = null;
        if (resultsStageName == null || resultsStageName.isEmpty()) {
            model = pipeline.getWorkingModel();
        }
        else {
            Result result = pipeline.getResult(resultsStageName);
            if (result == null) {
                throw new Exception("Stage "+resultsStageName+" not found.");
            }
            model = result.getModel();
        }

        if (model instanceof List) {
            List newList = null;
            for (Object item : (List)model) {
                if ((item) instanceof Result.Circle) {
                    Result.Circle circle = ((Result.Circle) item);
                    circle = transformCircle(transformInverse, circle);
                    if (newList == null) {
                        newList = new ArrayList<Result.Circle>();
                    }
                    newList.add(circle);
                }
                else if ((item) instanceof RotatedRect) {
                    RotatedRect rect = ((RotatedRect) item);
                    rect = transformRotatedRect(transformInverse, rect);
                    if (newList == null) {
                        newList = new ArrayList<RotatedRect>();
                    }
                    newList.add(rect);
                }
                else if ((item) instanceof TemplateMatch) {
                    TemplateMatch match = ((TemplateMatch) item);
                    match = transformTemplateMatch(transformInverse, match);
                    if (newList == null) {
                        newList = new ArrayList<TemplateMatch>();
                    }
                    newList.add(match);
                }
                else if ((item) instanceof KeyPoint) {
                    KeyPoint keyPoint = ((KeyPoint) item);
                    keyPoint = transformKeyPoint(transformInverse, keyPoint);
                    if (newList == null) {
                        newList = new ArrayList<KeyPoint>();
                    }
                    newList.add(keyPoint);
                }
                else {
                    throw new Exception("Unsupported list item.");
                }
            }
            // return new list or pass through empty model
            if (newList != null) {
                return new Result(null, newList);
            }
            return null;
        }
        else if ((model) instanceof Result.Circle) {
            Result.Circle circle = ((Result.Circle) model);
            circle = transformCircle(transformInverse, circle);
            return new Result(null, circle);
        }
        else if ((model) instanceof RotatedRect) {
            RotatedRect rect = ((RotatedRect) model);
            rect = transformRotatedRect(transformInverse, rect);
            return new Result(null, rect);
        }
        else if ((model) instanceof TemplateMatch) {
            TemplateMatch match = ((TemplateMatch) model);
            match = transformTemplateMatch(transformInverse, match);
            return new Result(null, match);
        }
        else if ((model) instanceof KeyPoint) {
            KeyPoint keyPoint = ((KeyPoint) model);
            keyPoint = transformKeyPoint(transformInverse, keyPoint);
            return new Result(null, keyPoint);
        }
        else {
            throw new Exception("Unsupported model type.");
        }
    }

    protected KeyPoint transformKeyPoint(AffineTransform transformInverse, KeyPoint keyPoint) {
        Point2D.Double p0 = new Point2D.Double(keyPoint.pt.x, keyPoint.pt.y);
        Point2D.Double p1 = new Point2D.Double(keyPoint.pt.x+keyPoint.size*Math.acos(keyPoint.angle), 
                keyPoint.pt.y-keyPoint.size*Math.acos(keyPoint.angle));
        Point2D.Double p0T = new Point2D.Double();
        Point2D.Double p1T = new Point2D.Double();
        transformInverse.transform(p0, p0T);
        transformInverse.transform(p1, p1T);
        // clone
        keyPoint = new KeyPoint((float)p0T.x, (float)p0T.y, (float)p1T.distance(p0T), (float)Math.atan2(p1.y-p0.y, p1.x-p0.x));
        return keyPoint;
    }

    protected RotatedRect transformRotatedRect(AffineTransform transformInverse, RotatedRect rect) {
        org.opencv.core.Point points[] = new org.opencv.core.Point[4];
        rect.points(points);
        int i = 0;
        Point2D.Double points2D[] = new Point2D.Double[4];
        Point2D.Double pointsT[] = new Point2D.Double[4];
        for (org.opencv.core.Point p : points) {
            points2D[i] = new Point2D.Double(p.x, p.y);
            pointsT[i] = new Point2D.Double();
            i++;
        }
        transformInverse.transform(points2D[0], pointsT[0]);
        transformInverse.transform(points2D[1], pointsT[1]);
        transformInverse.transform(points2D[2], pointsT[2]);
        transformInverse.transform(points2D[3], pointsT[3]);
        // take the longer side for the angle
        if (pointsT[0].distance(pointsT[1]) > pointsT[1].distance(pointsT[2])) { 
            // vertical is longer
            double angle = Math.toDegrees(Math.atan2(pointsT[1].y - pointsT[0].y, pointsT[1].x - pointsT[0].x)) + 90;
            rect = new RotatedRect(
                    new org.opencv.core.Point(
                            (pointsT[0].x + pointsT[2].x)/2.0, 
                            (pointsT[0].y + pointsT[2].y)/2.0),
                    new org.opencv.core.Size(
                            pointsT[2].distance(pointsT[1]), // TODO: dot product to compensate shear 
                            pointsT[1].distance(pointsT[0])),
                    angle);
        }
        else {
            // horizonal is longer
            double angle = Math.toDegrees(Math.atan2(pointsT[2].y - pointsT[1].y, pointsT[2].x - pointsT[1].x));
            rect = new RotatedRect(
                    new org.opencv.core.Point(
                            (pointsT[0].x + pointsT[2].x)/2.0, 
                            (pointsT[0].y + pointsT[2].y)/2.0),
                    new org.opencv.core.Size(
                            pointsT[2].distance(pointsT[1]), 
                            pointsT[1].distance(pointsT[0])),// TODO: dot product to compensate shear
                    angle);
        }
        return rect;
    }

    protected Result.Circle transformCircle(AffineTransform transformInverse,
            Result.Circle circle) {
        Point2D.Double p0 = new Point2D.Double(circle.x, circle.y);
        Point2D.Double p1 = new Point2D.Double(circle.x+circle.diameter, circle.y);
        Point2D.Double p2 = new Point2D.Double(circle.x, circle.y+circle.diameter);
        Point2D.Double p0T = new Point2D.Double();
        Point2D.Double p1T = new Point2D.Double();
        Point2D.Double p2T = new Point2D.Double();
        transformInverse.transform(p0, p0T);
        transformInverse.transform(p1, p1T);
        transformInverse.transform(p2, p2T);
        // if the transform includes shear or stretch, we approximate the diameter by taking the mean
        circle = new Result.Circle(p0T.x, p0T.y, (p1T.distance(p0T) + p2T.distance(p0T))/2.0);
        return circle;
    }

    protected TemplateMatch transformTemplateMatch(AffineTransform transformInverse,
            TemplateMatch match) {
        Point2D.Double p0 = new Point2D.Double(match.x, match.y);
        Point2D.Double p1 = new Point2D.Double(match.x+match.width, match.y+match.height);
        Point2D.Double p0T = new Point2D.Double();
        Point2D.Double p1T = new Point2D.Double();
        transformInverse.transform(p0, p0T);
        transformInverse.transform(p1, p1T);
        // if the transform includes shear or stretch or no 90° step rotation, this will only be an approximation
        if (p0T.x < p1T.x) {
            double swap = p0T.x;
            p0T.x = p1T.x;
            p1T.x = swap;
        }
        if (p0T.y < p1T.y) {
            double swap = p0T.y;
            p0T.y = p1T.y;
            p1T.y = swap;
        }
        match = new TemplateMatch(p0T.x, p0T.y, p1T.x - p0T.x, p1T.y - p0T.y, match.score);
        return match;
    }
}
