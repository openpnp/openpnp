/**
 * code written by dzach, @ https://github.com/dzach
 */
package org.openpnp.vision.pipeline.stages;

import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.imgproc.Imgproc;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Property;
import org.openpnp.vision.pipeline.Stage;
import org.simpleframework.xml.Attribute;

@Stage(category = "Image Processing",
        description = "Create a cropped template image in portrait orientation based on a model.")

public class CreateModelTemplateImage extends CvStage {
    @Attribute(required = false)
    @Property(description = "Name of a prior stage to retrieve the model from.")
    private String modelStageName;

    @Attribute(required = false)
    @Property(
            description = "Orientation of the output image. Zero is up, angles increase clockwise.")
    private double degrees = 0;

    public String getModelStageName() {
        return modelStageName;
    }

    public void setModelStageName(String modelStageName) {
        this.modelStageName = modelStageName;
    }

    public double getDegrees() {
        return degrees;
    }

    public void setDegrees(double degrees) {
        this.degrees = degrees;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {

        if (modelStageName == null || modelStageName.trim()
                                                    .equals("")) {
            return null;
        }
        Result modelResult = pipeline.getResult(modelStageName);
        Mat image = pipeline.getWorkingImage()
                            .clone();
        RotatedRect rect = null;

        if (modelResult == null || modelResult.model == null) {
            return null;
        }

        if (modelResult.model instanceof RotatedRect) {

            rect = ((RotatedRect) modelResult.model).clone();

        }
        else if (

        modelResult.model instanceof List<?>
                && ((List<?>) modelResult.model).get(0) instanceof RotatedRect) {

            rect = ((RotatedRect) ((List<?>) modelResult.model).get(0)).clone();
        }
        else {
            // we only handle RotatedRects
            return null;
        }

        // correct angle for portrait orientation
        if (rect.size.width > rect.size.height) {
            rect.angle += 90.0;
            double tmp = rect.size.width;
            rect.size.width = rect.size.height;
            rect.size.height = tmp;
        }
        // rotate code adapted from Rotate.java
        Mat mapMatrix = Imgproc.getRotationMatrix2D(rect.center, rect.angle - degrees, 1.0);
        rect.angle = degrees;
        // determine bounding rectangle
        Rect bbox = rect.boundingRect();
        // adjust transformation matrix
        double[] cx = mapMatrix.get(0, 2);
        double[] cy = mapMatrix.get(1, 2);
        cx[0] += bbox.width / 2D - rect.center.x;
        cy[0] += bbox.height / 2D - rect.center.y;
        mapMatrix.put(0, 2, cx);
        mapMatrix.put(1, 2, cy);
        /*
         * warpAffine can also crop the image, after rotating it, to bbox.size(), so there is no
         * need for a following crop stage
         */
        Imgproc.warpAffine(image, image, mapMatrix, bbox.size(), Imgproc.INTER_LINEAR);

        // reset rect center to the center of its new bbox
        rect.center.x = bbox.width / 2.0;
        rect.center.y = bbox.height / 2.0;

        return new Result(image, rect);
    }

}
