package org.openpnp.vision.pipeline.stages;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.RotatedRect;
import org.opencv.imgproc.Imgproc;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.simpleframework.xml.Attribute;

public class MinAreaRectContours extends CvStage {
    @Attribute(required = false)
    private String contoursStageName = null;

    public String getContoursStageName() {
        return contoursStageName;
    }

    public void setContoursStageName(String contoursStageName) {
        this.contoursStageName = contoursStageName;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        if (contoursStageName == null) {
            return null;
        }
        Result result = pipeline.getResult(contoursStageName);
        if (result == null || result.model == null) {
            return null;
        }
        List<MatOfPoint> contours = (List<MatOfPoint>) result.model;
        List<RotatedRect> results = new ArrayList<RotatedRect>();
        for (MatOfPoint contour : contours) {
            RotatedRect rect = Imgproc.minAreaRect(new MatOfPoint2f(contour.toArray()));
            results.add(rect);
        }
        return new Result(null, results);
    }
}
