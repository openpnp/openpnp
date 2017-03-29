package org.openpnp.vision.pipeline.stages;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.MatOfPoint;
import org.opencv.core.RotatedRect;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.simpleframework.xml.Attribute;
import org.openpnp.vision.pipeline.Stage;
import org.openpnp.vision.pipeline.Property;

@Stage(category="Image Processing", description="Filters rotated rects based on given size and tolerance. Orientation of width and height does not matter.")
public class FilterRects extends CvStage {
    @Attribute
    @Property(description="Width of filtered rects. Filtering takes tolerance into consideration.")
    private int width = 50;
    
    public int getWidth() {
      return width;
    }
    public void setWidth(int width) {
      this.width = width;
    }
    
    @Attribute
    @Property(description="Height of filtered rects. Filtering takes tolerance into consideration.")
    private int height = 50;

    public int getHeight() {
      return height;
    }
    public void setHeight(int height) {
      this.height = height;
    }
    
    @Attribute
    @Property(description="Tolerance of desired rect size.")
    private int tolerance = 5;
    
    public int getTolerance() {
      return tolerance;
    }
    public void setTolerance(int tolerance) {
      this.tolerance = tolerance;
    }
    

    @Attribute
    @Property(description="Pipeline stage that outputs rotated rects.")
    private String rectStageName = "";
    
    public String getRectStageName() {
        return rectStageName;
    }

    public void setRectStageName(String rectStageName) {
        this.rectStageName = rectStageName;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        if (rectStageName == null) {
            return null;
        }
        Result result = pipeline.getResult(rectStageName);
        if (result == null || result.model == null) {
            return null;
        }
        List<RotatedRect> rects = (List<RotatedRect>) result.model;
        List<RotatedRect> results = new ArrayList<RotatedRect>();
        // we need to sort sizes to do comparisons
        double w = Math.max(width,height);
        double h = Math.min(width, height);
        double rw, rh;
        for (RotatedRect rect : rects) {
          rw = Math.max(rect.size.width,rect.size.height);
          rh = Math.min(rect.size.width,rect.size.height);
            if (rw >= w - tolerance && rw <= w + tolerance && rh >= h - tolerance && rh <= h + tolerance) {
              // passes criteria
              results.add(rect);
            }
        }
        return new Result(null, results);
    }
}
