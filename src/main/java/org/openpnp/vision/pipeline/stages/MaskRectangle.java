package org.openpnp.vision.pipeline.stages;

import java.awt.Color;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.openpnp.vision.FluentCv;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.simpleframework.xml.Attribute;

/**
 * Mask everything in the working image outside of a circle centered at the center of the image with
 * the specified diameter.
 */
public class MaskRectangle extends CvStage {
    @Attribute
    private int width = 100;
    @Attribute
    private int height = 100;


    public int getWidth() {
        return width;
    }


    public void setWidth(int width) {
        this.width = width;
    }


    public int getHeight() {
        return height;
    }


    public void setHeight(int height) {
        this.height = height;
    }


    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        Mat mat = pipeline.getWorkingImage();
        Mat mask = mat.clone();
        Mat masked = mat.clone();
        Scalar color = FluentCv.colorToScalar(Color.black);
        mask.setTo(color);
        masked.setTo(color);
        Point low = new Point(mat.cols() / 2 - getWidth() / 2, mat.rows() / 2 - getHeight() / 2);
        Point high = new Point(mat.cols() / 2 + getWidth() / 2, mat.rows() / 2 + getHeight() / 2);
        Core.rectangle(mask, low, high, new Scalar(255, 255, 255), -1);
        if (getWidth() * getHeight() < 0) {
            Core.bitwise_not(mask, mask);
        }
        mat.copyTo(masked, mask);
        mask.release();
        return new Result(masked);
    }
}
