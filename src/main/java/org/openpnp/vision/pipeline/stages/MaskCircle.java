package org.openpnp.vision.pipeline.stages;

import java.awt.Color;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.openpnp.model.Length;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.FluentCv;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Property;
import org.openpnp.vision.pipeline.Stage;
import org.simpleframework.xml.Attribute;

@Stage(description="Mask everything in the working image outside of a circle centered at the center of the image with the specified diameter.")
public class MaskCircle extends CvStage {
    @Attribute
    @Property(description="The diameter of the circle to mask. Use a negative value to invert the mask.")
    private int diameter = 100;
    
    public int getDiameter() {
        return diameter;
    }

    public void setDiameter(int diameter) {
        this.diameter = diameter;
    }
    
    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        Camera camera = (Camera) pipeline.getProperty("camera");
        
        Mat mat = pipeline.getWorkingImage();
        Mat mask = mat.clone();
        Mat masked = mat.clone();
        Scalar color = FluentCv.colorToScalar(Color.black);
        mask.setTo(color);
        masked.setTo(color);

        //Check for overriding properties
        int diameter = this.diameter;
        String property = "MaskCircle.diameter";
        Object diameterByProperty = pipeline.getProperty(property);
        if (diameterByProperty instanceof Length) {
            if (camera != null) {
                diameter = (int) Math.round(VisionUtils.toPixels((Length) diameterByProperty, 
                    camera));
            }
            else {
                throw new Exception("Pipeline property \"camera\" not set");
            }
        }
        else if (diameterByProperty instanceof Double) {
            diameter = (int) Math.round((Double) diameterByProperty);
        }
        else if (diameterByProperty instanceof Integer) {
            diameter = (Integer) diameterByProperty;
        }
        else if (diameterByProperty != null) {
            throw new Exception("Invalid type \"" + diameterByProperty.getClass() + "\" "
                    + "for pipeline property \"" + property + "\" - Must be a Length, Double, "
                    + "or Integer");
        }
        
        org.openpnp.model.Point center = new org.openpnp.model.Point(mat.cols()*0.5, mat.rows()*0.5);
        property = "MaskCircle.center";
        Object centerByProperty = pipeline.getProperty(property);
        if (centerByProperty instanceof Location) {
            if (camera != null) {
                center = VisionUtils.getLocationPixels(camera, (Location) centerByProperty);
            }
            else {
                throw new Exception("Pipeline property \"camera\" not set");
            }
        }
        else if (centerByProperty instanceof Point) {
            center = (org.openpnp.model.Point) centerByProperty;
        }
        else if (centerByProperty != null){
            throw new Exception("Invalid type \"" + centerByProperty.getClass() + "\" "
                    + "for pipeline property \"" + property + "\" - Must be a Location or Point");
        }
        
        Imgproc.circle(mask, new Point(center.x, center.y),  Math.abs(diameter) / 2, 
                new Scalar(255, 255, 255), -1);
        if(diameter < 0) {
            Core.bitwise_not(mask,mask);
        }
        mat.copyTo(masked, mask);
        mask.release();
        return new Result(masked);
    }
}
