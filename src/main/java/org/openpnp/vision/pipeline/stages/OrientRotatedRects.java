package org.openpnp.vision.pipeline.stages;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.RotatedRect;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Property;
import org.openpnp.vision.pipeline.Stage;
import org.simpleframework.xml.Attribute;

@Stage(category = "Model Transforms",
        description = "Sets the angle of a RotatedRect or List<RotatedRect> to match the specified orientation, either landscape or portrait.")

public class OrientRotatedRects extends CvStage {
    public enum Orientation {
        Landscape,
        Portrait
    }
    
    @Attribute(required = false)
    @Property(description = "Name of a prior stage containing a RotatedRect or List<RotatedRect>.")
    private String rotatedRectsStageName;
    
    @Attribute(required = false)
    private Orientation orientation = Orientation.Landscape;
    
    @Attribute(required = false)
    private boolean negateAngle = false;
    
    public String getRotatedRectsStageName() {
        return rotatedRectsStageName;
    }

    public void setRotatedRectsStageName(String rotatedRectsStageName) {
        this.rotatedRectsStageName = rotatedRectsStageName;
    }
    
    public Orientation getOrientation() {
        return orientation;
    }

    public void setOrientation(Orientation orientation) {
        this.orientation = orientation;
    }
    
    public boolean isNegateAngle() {
        return negateAngle;
    }

    public void setNegateAngle(boolean negateAngle) {
        this.negateAngle = negateAngle;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        if (rotatedRectsStageName == null || rotatedRectsStageName.trim().equals("")) {
            return null;
        }
        
        Object model = pipeline.getResult(rotatedRectsStageName).model;
        
        if (model == null) {
            return null;
        }
        
        if (model instanceof List) {
            List<RotatedRect> results = new ArrayList<>();
            for (Object o : (List<?>) model) {
                if (o instanceof RotatedRect) {
                    RotatedRect r = (RotatedRect) o;
                    r = orient(r);
                    results.add(r);
                }
            }
            return new Result(null, results);
        }
        else if (model instanceof RotatedRect) {
            RotatedRect r = (RotatedRect) model;
            r = orient(r);
            return new Result(null, r);
        }
        
        return null;
    }
    
    private RotatedRect orient(RotatedRect r1) {
        RotatedRect r2 = (RotatedRect) r1.clone();
        if ((r2.size.height > r2.size.width && orientation == Orientation.Landscape) 
                || (r2.size.width > r2.size.height && orientation == Orientation.Portrait)) {
            double tmp = r2.size.height;
            r2.size.height = r2.size.width;
            r2.size.width = tmp;
            r2.angle -= 90;
        }
        if (negateAngle) {
            r2.angle = -r2.angle;
        }
        return r2;
    }
}
