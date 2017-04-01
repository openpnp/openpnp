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
    @Property(description="Tolerance of specified sizes. If negative, sizes are maximum values.")
    private double tolerance = 5;
    
    public double getTolerance() {
      return tolerance;
    }
    public void setTolerance(double tolerance) {
      this.tolerance = tolerance;
    }
    
    @Attribute
    @Property(description="Aspect ratio for selecting rects, used if one or both of width and height are 0. If both width and height are 0, then any rect with size satisfying the aspect ratio and tolerance requirements will be selected.")
    private double aspect = 0.0;
    
    public double getAspect() {
      return aspect;
    }
    public void setAspect(double aspect) {
      this.aspect = Math.abs(aspect);
    }

    @Attribute
    @Property(description="Tolerance is taken to be a percentage, e.g. 5 = 5%.")
    private boolean toleranceIsPerc = false;
    
    public boolean gettoleranceIsPerc() {
        return toleranceIsPerc;
    }

    public void settoleranceIsPerc(boolean toleranceIsPerc) {
        this.toleranceIsPerc = toleranceIsPerc;
    }
    
    @Attribute
    @Property(description="Show selection ranges in the Log.")
    private boolean logSelectionRanges = true;
    
    public boolean getLogSelectionRanges() {
        return logSelectionRanges;
    }

    public void setLogSelectionRanges(boolean logSelectionRanges) {
        this.logSelectionRanges = logSelectionRanges;
    }

    @Attribute
    @Property(description="Pipeline stage that outputs rotated rects.")
    private String rectStageName = null;
    
    public String getRectStageName() {
        return rectStageName;
    }

    public void setRectStageName(String rectStageName) {
        this.rectStageName = rectStageName;
    }
    private void logInfo(String s) {
      if (logSelectionRanges) {
       Logger.info(s);
      }
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
        String pass;
        
        // check the validity of the input
        if (w == 0.0) {
          // this means h is also 0
          if (aspect == 0.0) {
            // cannot calculate absolute dimensions
            throw new Exception("Width, height, or width and aspect, or aspect, must be non zero.");
            
          } else if (!toleranceIsPerc) {
            // bad input
            throw new Exception("If only aspect is specified, tolerance must be expressed as a percentage.");
          }
        }
        // parse tolerance
        if (toleranceIsPerc) {
          tol = tolerance / 100.0;
        } else {
          tol = tolerance;
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
          if (w != 0)
            logInfo("derived height parameter: " + h);
        }
        // iterate over input rects
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
          ww = toleranceIsPerc ? w : 1;
          hh = toleranceIsPerc ? h : 1;
          upper_w = tol * ww * extUpper;
          upper_h = tol * hh * extUpper;

          // check criteria
          
          if (rw >= w - tol * ww && rw <= w + upper_w && rh >= h - tol * hh && rh <= h + upper_h) {
            // rect passes criteria
            results.add(rect);
            pass = "[PASS]";
          } 
          else {
            pass = "[REJ] ";
          }
          logInfo(pass + " widths  range: " + (w - tol * ww) + " <= " + rw + " <= " + (w + upper_w));
          logInfo(pass + " heights range: " + (h - tol * hh) + " <= " + rh + " <= " + (h + upper_h));
          if (aspect != 0) {
            logInfo(pass + " aspect ratio range: " + (aspect * (1 - tol)) + " <= " + (rw / rh) + " <= " + (aspect * (1 + tol)));
          }
        }
        // deside what type to return
        if(results.size()==0) {
          return null;
        }
        if(result.model instanceof RotatedRect) {
          return new Result(null, results.get(0));
        }
        return new Result(null, results);
    }
}
