/**
 * code written by dzach, @ https://github.com/dzach
 */
package org.openpnp.vision.pipeline.stages;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.CvStage.Result.TemplateMatch;
import org.openpnp.vision.pipeline.Property;
import org.openpnp.vision.pipeline.Stage;
import org.simpleframework.xml.Attribute;

import org.pmw.tinylog.Logger;

/**
 * OpenCV based image template matching with local maxima detection improvements from FireSight:
 * https://github.com/firepick1/FireSight. Scans the working image for matches of a template image
 * and returns a list of matches.
 */
@Stage(category = "Image Processing",
        description = "Filter RotatedRects that fit in the template's size, with a tolerance and find the closest RotatedRect to the center of the screen.")

public class ClosestModel extends CvStage {

    @Attribute(required = true)
    @Property(description = "Name of a prior stage to load the model from.")
    private String modelStageName;

    public String getModelStageName() {
        return modelStageName;
    }

    public void setModelStageName(String modelStageName) {
        this.modelStageName = modelStageName;
    }

    @Attribute(required = true)
    @Property(description = "Name of a prior stage to load the filter size from. The filter stage should contain a single RotatedRect model.")
    private String filterStageName;

    public String getFilterStageName() {
        return filterStageName;
    }

    public void setFilterStageName(String filterStageName) {
        this.filterStageName = filterStageName;
    }

    @Attribute(required = false)
    @Property(description = "Filter tolerance.")
    private double tolerance = 0.2f;

    public double getTolerance() {
        return tolerance;
    }

    public void setTolerance(double tolerance) {
        this.tolerance = tolerance;
    }

    @Attribute(required = false)
    @Property(description = "Scale filter by this value.")
    private double scale = 1.0f;

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {

      Object model = null;
      RotatedRect rrect = null;
      RotatedRect closest = null;
      RotatedRect frect = null;
      double tHeightMax=0, tHeightMin=0, tWidthMax=0, tWidthMin=0;
      // needed to get the center of the image
      Mat image = pipeline.getWorkingImage();
      // check for the existance of an input model
      if (modelStageName == null || modelStageName.trim().equals("")) {
        //model = (List<?>)pipeline.getWorkingModel();
        return null;
        
      } else {
      
        model = pipeline.getResult(modelStageName).model;
      }
      if (model == null || image == null) {
        return null;
      }
      // list to keep filtered rotated rects in.
      ArrayList multi = new ArrayList();

      // Check for the existance of a filter
      if (filterStageName != null && !filterStageName.trim().equals("")) {

        CvStage filterStage = pipeline.getStage(filterStageName);
        if (filterStage != null && pipeline.getResult(filterStage) != null) {

          Result filterResult = pipeline.getResult(filterStage);
          if (filterResult.model instanceof RotatedRect) {
            frect = ((RotatedRect) filterResult.model);
          } else if (
            filterResult.model instanceof List<?> && 
            ((List<?>) filterResult.model).get(0) instanceof RotatedRect
          ) {
            frect = ((RotatedRect) ((List<?>) filterResult.model).get(0));
          } else {
            // we only handle RotatedRect filters
          }
        }
      }
      Point screenCenter = new Point(image.size().width/2.0, image.size().height/2.0);
      
      if (frect != null) {
        // calculate filter range
        tHeightMax = Math.max(frect.size.width,frect.size.height) * (1 + tolerance / 2.0) * scale;
        tHeightMin = Math.max(frect.size.width,frect.size.height) * (1 - tolerance / 2.0) * scale;
        tWidthMax  = Math.min(frect.size.width,frect.size.height) * (1 + tolerance / 2.0) * scale;
        tWidthMin  = Math.min(frect.size.width,frect.size.height) * (1 - tolerance / 2.0) * scale;

      }
      if (model instanceof RotatedRect) {

        multi.add(model);

        
      } else if (model instanceof List<?> && ((List<?>)model).get(0) instanceof RotatedRect) {

        multi = (ArrayList) model;
        
      } else {
        // we only handle rotated rects here
      }
      // find the closest rotatedRect to the center of the screen
      // Given the oportunity of a loop through the rects and the size of the model represented by the template, 
      // also filter those rects that fit in the template's size, with a tolerance

      double distance = 10e8; // a really big number
      double rHeight = 0, rWidth = 0;
      for (RotatedRect r : (ArrayList<RotatedRect>) multi) {
        // Convention here is: larger side = height, smaller side = width
        rHeight = Math.max(r.size.width,r.size.height);
        rWidth  = Math.min(r.size.width,r.size.height);
        // filter rects to be  template's size +- tolerance
        if (
          frect != null && 
          (rHeight > tHeightMax || rHeight < tHeightMin || rWidth  > tWidthMax || rWidth  < tWidthMin)
        ) {
          continue;
        }
        // find distance from center
        double dc = Math.sqrt(
          Math.pow(screenCenter.x - r.center.x, 2) + 
          Math.pow(screenCenter.y - r.center.y, 2)
        );
        if (dc < distance) {
          distance = dc;
          closest = r;
        }
      }
      if (closest != null) {
        rrect = closest.clone();
        // correct input model rrect so that it comes in a more "upright" position, i.e. width < height
        // This convention has to be consistant throughout the pipeline 
        if (rrect.size.width > rrect.size.height) {
          rrect.angle +=90.0;
          double tmp = rrect.size.width;
          rrect.size.width = rrect.size.height;
          rrect.size.height = tmp;
        }
        multi.clear();
        multi.add(closest);
        return new Result(null, multi);
      }
      return null;
    }
}
