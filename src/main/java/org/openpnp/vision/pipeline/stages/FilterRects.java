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

@Stage(category="Image Processing", description="Filters rotated rects based on given size and tolerance. Orientation of width and length does not matter.")
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
    @Property(description="Tolerance for the width of filtered rects.")
    private double widthTolerance = 5.0;
    
    public double getWidthTolerance() {
      return widthTolerance;
    }
    public void setWidthTolerance(double widthTolerance) {
      this.widthTolerance = widthTolerance;
    }
    
    @Attribute
    @Property(description="Length of filtered rects. Filtering takes tolerance into consideration.")
    private double length = 50.0;

    public double getLength() {
      return length;
    }
    public void setLength(double length) {
      this.length = Math.abs(length);
    }
    
    @Attribute
    @Property(description="Tolerance for the length of filtered rects.")
    private double lengthTolerance = 5.0;
    
    public double getLengthTolerance() {
      return lengthTolerance;
    }
    public void setLengthTolerance(double lengthTolerance) {
      this.lengthTolerance = lengthTolerance;
    }

    @Attribute
    @Property(description="Aspect ratio for selecting rects, used if one or both of width and length are 0. If both width and length are 0, then any rect with size satisfying the aspect ratio and tolerance requirements will be selected.")
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
    private boolean enableLogging = false;
    
    public boolean getEnableLogging() {
        return enableLogging;
    }

    public void setEnableLogging(boolean enableLogging) {
        this.enableLogging = enableLogging;
    }

    @Attribute
    @Property(description="Previous pipeline stage that outputs rotated rects.")
    private String rotatedRectsStageName = null;
    
    public String getRotatedRectsStageName() {
        return rotatedRectsStageName;
    }

    public void setRotatedRectsStageName(String rotatedRectsStageName) {
        this.rotatedRectsStageName = rotatedRectsStageName;
    }
    private void logInfo(String s) {
      if (enableLogging) {
       Logger.info(s);
      }
    }
    // assign values here to minimize storage allocation costs in time, and use pointers to these values.\u25A0
    private static String[] verdict = {" ","+"};
    
    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        if (rotatedRectsStageName == null) {
            return null;
        }
        
        Result result = pipeline.getResult(rotatedRectsStageName);
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
                
        double w, l, wtol, ltol, rw, rl, extUpperW, extUpperL;
        int sizeType = 0;
        String pass, sWtol,sLtol;
        boolean swapped;
        
        // we need to sort sizes to do comparisons. The following assures w is always greater than l
        if (width >= length) {
          w = width;
          l = length;
          /*
          if tolerances are positive, they are applied as dimension+tolerance/2-tolerance/2
          if negative, they are is applied as dimension+0-tolerance
          */
          extUpperW = widthTolerance >=0? 1:0; 
          wtol = Math.abs(widthTolerance);
          extUpperL = lengthTolerance >=0? 1:0;
          ltol = Math.abs(lengthTolerance);
        } 
        else {
          // swap width and length, and tolerances
          w = length;
          l = width;
          extUpperW = lengthTolerance >=0? 1:0; 
          wtol = Math.abs(lengthTolerance);
          extUpperL = widthTolerance >=0? 1:0;
          ltol = Math.abs(widthTolerance);
        }
        
        // check the validity of the input
        if (w == 0.0) {
          // this means l is also 0
          if (aspectRatio == 0.0) {
            // cannot calculate absolute dimensions
            throw new Exception("width, length, or width and aspectRatio, or aspectRatio, must be non zero.");
            
          } else if (!toleranceIsPercentage) {
            // bad input
            throw new Exception("If only aspectRatio is specified, tolerance must be expressed as a percentage.");
          } 
          else {
            sizeType = 2;
          }
        }
        else if (l == 0.0 && aspectRatio != 0.0) {
          // derive length from aspect ratio
          l = w / aspectRatio;
          sizeType = 1;
        }
        // At this point we have l and/or w set to non-zero, and any tolerance and aspectRatio, 
        // or have l,w zero, and have aspectRatio non-zero, and toleranceIsPercent
        //
        // 1. if w and l are non-zero, tolerance can be either percent or units. If it's units it can be converted to percent
        // 2. if w and ascpect ratio are non-zero, then l can be derived and then tolerance as per (1)
        // 3. if only aspect ratio and toleranceIsPercent are non-zero, tolerance is already a percentage
        
        // normalize tolerance to 0.0-1.0
        if (toleranceIsPercentage) 
        {
          wtol = wtol / 100.0;
          ltol = ltol / 100.0;
        } 
        else {
          wtol = wtol / w;
          ltol = ltol / l;
        }

        // range will be either dimension +- tolerance/2, or dimension+0-tolerance
        if (extUpperW == 1) {
          // tolerance/2 extends above width
          wtol = wtol / 2.0;
        }
        if (extUpperL == 1) {
          ltol = ltol / 2.0;
        }
        
        // iterate over input rects
        for (RotatedRect rect : rects) {
        
          rw = Math.max(rect.size.width,rect.size.height);
          rl = Math.min(rect.size.width,rect.size.height);
          
          if (sizeType == 2) {
            // Any size:
            // Select rects based only on the aspect ratio of each input rectangle
            // Parameter l is derived from rw
            w = rw;
            l = rw / aspectRatio;
          } 
          // calculate rect size limits
          double wlow = w * (1 - wtol), whigh = w * (1 + wtol * extUpperW),
                 llow = l * (1 - ltol), lhigh = l * (1 + ltol * extUpperL);

          // check criteria
          if (rw >= wlow && rw <= whigh && rl >= llow && rl <= lhigh) 
          {
            // rect passes criteria
            results.add(rect);
            pass = verdict[1];
          } 
          else {
            pass = verdict[0];
          }
          if (enableLogging) {
            Logger.info("{} rect: xy={},{}, size={}x{}px, angle={}°, area={}", pass, (int)rect.center.x, (int)rect.center.y, (int)rw, (int)rl, (int)rect.angle,(int)(rw * rl));
            if (toleranceIsPercentage) {
              sWtol = ""+wtol*100+"%";
              sLtol = ""+ltol*100+"%";
            } else {
              sWtol = ""+(int)(w * wtol)+"px";
              sLtol = ""+(int)(l * ltol)+"px";
            }
            Logger.info("   stage: tolerance={},{}, aspectRatio={}, {}",               (extUpperW>0?"+-":"+0-")+sWtol,(extUpperL >0? "+-":"+0-")+sLtol,
              aspectRatio, (sizeType==2? "any size":""));
            Logger.info("   widths: {} <= {} <= {}, mean = {}",
              String.format("%.3f",wlow),  String.format("%.3f",rw), 
              String.format("%.3f",whigh), String.format("%.3f",(wlow + whigh)/2.0));
            Logger.info("   lengths: {} <= {} <= {}, mean = {}", 
              String.format("%.3f",llow),  String.format("%.3f",rl), 
              String.format("%.3f",lhigh), String.format("%.3f",(llow + lhigh)/2.0));
            if (sizeType > 0) {
              Logger.info("   rect: aspectRatio={}",
                String.format("%.3f",rw / rl));
            }
          }
        }
        if (enableLogging) {
          Logger.info("Total detected rects: {}",results.size());
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
