package org.openpnp.vision.pipeline.stages;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.imgproc.Imgproc;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.simpleframework.xml.Attribute;

/**
 * Finds the smallest rotated rectangle that encloses pixels that fall within the given range.
 * Input should be a grayscale image. 
 */
public class MinAreaRect extends CvStage {
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
        MatOfPoint2f pointsMat = new MatOfPoint2f(points.toArray(new Point[]{}));
        RotatedRect r = Imgproc.minAreaRect(pointsMat);
        pointsMat.release();
        return new Result(null, r);
    }
}
