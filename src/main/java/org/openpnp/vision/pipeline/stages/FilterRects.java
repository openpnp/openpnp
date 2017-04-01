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
    private double rectWidth = 50.0;
    
    public double getRectWidth() {
      return rectWidth;
    }
    public void setRectWidth(double rectWidth) {
      this.rectWidth = Math.abs(rectWidth);
    }
    
    @Attribute
    @Property(description="Height of filtered rects. Filtering takes tolerance into consideration.")
    private double rectHeight = 50.0;

    public double getRectHeight() {
      return rectHeight;
    }
    public void setRectHeight(double rectHeight) {
      this.rectHeight = Math.abs(rectHeight);
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
    @Property(description="Aspect ratio for selecting rects, used if one or both of rectWidth and rectHeight are 0. If both rectWidth and rectHeight are 0, then any rect with size satisfying the aspect ratio and tolerance requirements will be selected.")
    private double aspectRatio = 0.0;
    
    public double getAspectRatio() {
      return aspectRatio;
    }
    public void setAspectRatio(double aspectRatio) {
      this.aspectRatio = Math.abs(aspectRatio);
    }

    @Attribute
    @Property(description="Tolerance is taken to be a percentage, e.g. 5 = 5%.")
    private boolean toleranceIsPercentage = false;
    
    public boolean getToleranceIsPercentage() {
        return toleranceIsPercentage;
    }

    public void setToleranceIsPercentage(boolean toleranceIsPercentage) {
        this.toleranceIsPercentage = toleranceIsPercentage;
    }
    
    @Attribute
    @Property(description="Show selection ranges in the Log.")
    private boolean logSelectionRanges = false;
    
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
    // assign values here to minimize storage allocation costs in time, and use pointers to these values.
    private static String[] verdict = {"[REJ]","[PASS]"};
    
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
        double w = Math.max(rectWidth,rectHeight), ww, upper_w;
        double h = Math.min(rectWidth, rectHeight), hh, upper_h;
        double rw, rh, tol, extUpper;
        String pass;
        
        // check the validity of the input
        if (w == 0.0) {
          // this means h is also 0
          if (aspectRatio == 0.0) {
            // cannot calculate absolute dimensions
            throw new Exception("rectWidth, rectHeight, or rectWidth and aspectRatio, or aspectRatio, must be non zero.");
            
          } else if (!toleranceIsPercentage) {
            // bad input
            throw new Exception("If only aspectRatio is specified, tolerance must be expressed as a percentage.");
          }
        }
        // parse tolerance
        if (toleranceIsPercentage) {
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
        
        if (h == 0.0 && aspectRatio != 0.0) {
          // derive rectHeight from aspect ratio
          h = w / aspectRatio;
          if (w != 0)
            logInfo("derived rectHeight parameter: " + h);
        }
        // iterate over input rects
        for (RotatedRect rect : rects) {
        
          rw = Math.max(rect.size.width,rect.size.height);
          rh = Math.min(rect.size.width,rect.size.height);
          
          if (rectWidth == 0.0 && rectHeight == 0.0) {
            // Any size:
            // Select rects based only on the aspect ratio of each input rectangle
            w = rw;
            h = rw / aspectRatio;
          } 
          
          // trick to avoid lengthy checking of tolerance criteria bellow
          ww = toleranceIsPercentage ? w : 1;
          hh = toleranceIsPercentage ? h : 1;
          upper_w = tol * ww * extUpper;
          upper_h = tol * hh * extUpper;

          // check criteria
          
          if (rw >= w - tol * ww && rw <= w + upper_w && rh >= h - tol * hh && rh <= h + upper_h) {
            // rect passes criteria
            results.add(rect);
            pass = verdict[1];
          } 
          else {
            pass = verdict[0];
          }
          logInfo(pass + " width  range: " + (w - tol * ww) + " <= " + rw + " <= " + (w + upper_w));
          logInfo(pass + " height range: " + (h - tol * hh) + " <= " + rh + " <= " + (h + upper_h));
          if (aspectRatio != 0) {
            logInfo(pass + " aspectRatio range: " + (aspectRatio * (1 - tol)) + " <= " + (rw / rh) + " <= " + (aspectRatio * (1 + tol)));
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
