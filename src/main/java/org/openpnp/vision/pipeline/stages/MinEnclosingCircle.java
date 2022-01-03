package org.openpnp.vision.pipeline.stages;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.openpnp.vision.FluentCv;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.simpleframework.xml.Attribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Finds the smallest circle that encloses pixels that fall within the given range.
 * Input should be a grayscale image.
 */
public class MinEnclosingCircle extends CvStage {
    @Attribute
    private int thresholdMin;

    @Attribute
    private int thresholdMax;

    public int getThresholdMin() {
        return thresholdMin;
    }

    public void setThresholdMin(int thresholdMin) {
        this.thresholdMin = thresholdMin;
    }

    public int getThresholdMax() {
        return thresholdMax;
    }

    public void setThresholdMax(int thresholdMax) {
        this.thresholdMax = thresholdMax;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        if (pipeline.getWorkingColorSpace() != FluentCv.ColorSpace.Gray){
            throw new Exception(String.format("%s is not compatible with %s colorspace. Only Grey colorspace is supported.", MinEnclosingCircle.class.getSimpleName(), pipeline.getWorkingColorSpace()));
        }

        Mat mat = pipeline.getWorkingImage();
        List<Point> points = new ArrayList<>();
        byte[] rowData = new byte[mat.cols()];
        for (int row = 0, rows = mat.rows(); row < rows; row++) {
            mat.get(row, 0, rowData);
            for (int col = 0, cols = mat.cols(); col < cols; col++) {
                int pixel = ((int) rowData[col]) & 0xff;
                if (pixel >= thresholdMin && pixel <= thresholdMax) {
                    points.add(new Point(col, row));
                }
            }
        }
        if (points.isEmpty()) {
            return null;
        }
        MatOfPoint2f pointsMat = new MatOfPoint2f(points.toArray(new Point[]{}));

        Point center = new Point();
        float[] radius = new float[1];
        Imgproc.minEnclosingCircle(pointsMat, center, radius);
        pointsMat.release();

        Result.Circle detectedCircle = new Result.Circle(center.x, center.y, radius[0] * 2);

        // Returning List instead of a single circle for compatibility with other stages
        // ie DrawCircles that works with List
        List<Result.Circle> model = Collections.singletonList(detectedCircle);

        return new Result(null, model);
    }
}
