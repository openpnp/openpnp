package org.openpnp.vision.pipeline.stages;

import java.awt.Color;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.openpnp.model.Length;
import org.openpnp.model.Location;
import org.openpnp.vision.FluentCv;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Property;
import org.openpnp.vision.pipeline.Stage;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.core.Commit;

@Stage(description="Mask everything in the working image outside of a circle centered at the center of the image with the specified diameter.")
public class MaskCircle extends CvStage {
    @Attribute
    @Property(description="The diameter of the circle to mask. Use a negative value to invert the mask.")
    private int diameter = 100;

    @Attribute(required = false)
    @Property(description = "Name of the property through which OpenPnP controls this stage. Use \"MaskCircle\" for standard control.")
    private String propertyName = "MaskCircle";

    public int getDiameter() {
        return diameter;
    }

    public void setDiameter(int diameter) {
        this.diameter = diameter;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    @Commit
    void commit() {
        if (diameter == 0) {
            // MaskCircle with diameter 0 is traditionally used to paint the image black, 
            // no control by propertName wanted. 
            propertyName = "";
        }
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        Mat mat = pipeline.getWorkingImage();
        Mat mask = mat.clone();
        Mat masked = mat.clone();
        Scalar color = FluentCv.colorToScalar(Color.black);
        mask.setTo(color);
        masked.setTo(color);

        //Check for overriding properties
        int diameter = this.diameter;
        Point center = new Point(mat.cols()*0.5, mat.rows()*0.5);
        
        diameter = getPossiblePipelinePropertyOverride(diameter, pipeline, propertyName+".diameter", 
                Double.class, Integer.class, Length.class);
        
        center = getPossiblePipelinePropertyOverride(center, pipeline, propertyName+".center", 
                Point.class, org.openpnp.model.Point.class, Location.class);

        Imgproc.circle(mask, center,  Math.abs(diameter) / 2, 
                new Scalar(255, 255, 255), -1);
        if (diameter < 0) {
            Core.bitwise_not(mask,mask);
        }
        mat.copyTo(masked, mask);
        mask.release();
        return new Result(masked);
    }
}
