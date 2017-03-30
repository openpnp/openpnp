package org.openpnp.vision.pipeline.stages;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

import org.opencv.core.MatOfPoint;
import org.opencv.core.RotatedRect;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.simpleframework.xml.Attribute;
import org.openpnp.vision.pipeline.Stage;
import org.openpnp.vision.pipeline.Property;
import org.pmw.tinylog.Logger;

@Stage(category="Image Processing", description="Filters rotated rects based on given size and tolerance. Orientation of width and height does not matter.")
public class FilterRects extends CvStage {
    @Attribute
    @Property(description="Width of filtered rects. Filtering takes tolerance into consideration.")
    private double width = 50.0;
    
    public double getWidth() {
      return width;
    }
    public void setWidth(double width) {
      this.width = Math.abs(width);
    }
    
    @Attribute
    @Property(description="Height of filtered rects. Filtering takes tolerance into consideration.")
    private double height = 50.0;

    public double getHeight() {
      return height;
    }
    public void setHeight(double height) {
      this.height = Math.abs(height);
    }
    
    @Attribute
    @Property(description="Tolerance of specified sizes. Can be expressed as a percentage, e.g. 5%. If negative, sizes are maximum values.")
    private String tolerance = "5%";
    
    public String getTolerance() {
      return tolerance;
    }
    public void setTolerance(String tolerance) {
      this.tolerance = tolerance;
    }
    
    @Attribute
    @Property(description="Aspect ratio of filtered rects, used if one or both of width and height are 0. If both width and height are 0, then any rect satisfying the aspect ratio specified will be selected.")
    private double aspect = 0.0;
    
    public double getAspect() {
      return aspect;
    }
    public void setAspect(double aspect) {
      this.aspect = Math.abs(aspect);
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
        String dimErr = "Width, height, or width and aspect, or aspect, must be non zero.";
        
        Result result = pipeline.getResult(rectStageName);
        if (result == null || result.model == null) {
            return null;
        }
        
        List<RotatedRect> rects; 
        if (result.model instanceof RotatedRect) {
            rects = Collections.singletonList((RotatedRect)result.model);
        }
        else {
            rects = (List<RotatedRect>)result.model;
        }

        List<RotatedRect> results = new ArrayList<RotatedRect>();
                
        // we need to sort sizes to do comparisons. The following assures w is always greater than h
        double w = Math.max(width,height), ww, upper_w;
        double h = Math.min(width, height), hh, upper_h;
        double rw, rh, tol, extUpper;
        boolean tolIsPerc;
        
        // check the validity of input
        if (w == 0.0) {
          // this means h is also 0
          if (aspect == 0.0) {
            // cannot calculate absolute dimensions
            throw new Exception(dimErr);
            
          } else if (!tolerance.matches("[\\+\\-0-9]*\\.?[0-9]+%*")) {
            // bad input
            throw new Exception("If only aspect is specified, tolerance should be expressed as a percentage, e.g. 5%");
          }
        }
        // parse tolerance
        if (tolerance.matches("[\\+\\-0-9]*\\.?[0-9]+%*")) {
          tol = Double.parseDouble(tolerance.replaceAll("%","")) / 100.0;
          tolIsPerc = true;
        } else {
          tol = Double.parseDouble(tolerance);
          tolIsPerc = false;
        }
        if (tol >= 0) {
          // range will be dimension +- tolerance/2
          extUpper = 1;
          tol = tol / 2.0;
        } else {
          // range will be dimension 0- tolerance
          extUpper = 0;
          tol = Math.abs(tol);
        }
        
        if (h == 0.0 && aspect != 0.0) {
          // derive height from aspect
          h = w / aspect;
        }
        for (RotatedRect rect : rects) {
        
          rw = Math.max(rect.size.width,rect.size.height);
          rh = Math.min(rect.size.width,rect.size.height);
          
          if (width == 0.0 && height == 0.0) {
            // Any size:
            // Select rects based only on the aspect ratio of each input rectangle
            w = rw;
            h = rw / aspect;
          }
          
          // trick to avoid lengthy checking of tolerance criteria bellow
          ww = tolIsPerc ? w : 1;
          hh = tolIsPerc ? h : 1;
          upper_w = tol * ww * extUpper;
          upper_h = tol * hh * extUpper;

          // check criteria
          if (rw >= w - tol * ww && rw <= w + upper_w && rh >= h - tol * hh && rh <= h + upper_h) {
            // rect passes criteria
            results.add(rect);
          }
        }
        return new Result(null, results);
    }
}
