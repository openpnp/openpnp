package org.openpnp.vision.pipeline.stages;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.imgproc.Imgproc;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.simpleframework.xml.Attribute;

public class Rotate extends CvStage {
    @Attribute
    private double degrees;
    
    public double getDegrees() {
        return degrees;
    }

    public void setDegrees(double degrees) {
        this.degrees = degrees;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        Mat mat = pipeline.getWorkingImage();
        
        if (degrees == 0D) {
            return null;
        }

        // See:
        // http://stackoverflow.com/questions/22041699/rotate-an-image-without-cropping-in-opencv-in-c
        Point center = new Point(mat.width() / 2D, mat.height() / 2D);
        Mat mapMatrix = Imgproc.getRotationMatrix2D(center, degrees, 1.0);

        // determine bounding rectangle
        Rect bbox = new RotatedRect(center, mat.size(), degrees).boundingRect();
        // adjust transformation matrix
        double[] cx = mapMatrix.get(0, 2);
        double[] cy = mapMatrix.get(1, 2);
        cx[0] += bbox.width / 2D - center.x;
        cy[0] += bbox.height / 2D - center.y;
        mapMatrix.put(0, 2, cx);
        mapMatrix.put(1, 2, cy);

        Mat dst = new Mat(bbox.width, bbox.height, mat.type());
        Imgproc.warpAffine(mat, dst, mapMatrix, bbox.size(), Imgproc.INTER_LINEAR);
        mat.release();

        mapMatrix.release();

        return new Result(dst);
    }
}
