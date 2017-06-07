/**
 * code written by dzach, @ https://github.com/dzach
 */
package org.openpnp.vision.pipeline.stages;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.Mat;
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
        description = "OpenCV based image template matching with local maxima detection improvements. On match, returns the true orientation of the input model.")

public class MatchPartTemplate extends CvStage {
    /**
     * Name of a prior stage to load the template image from.
     */
    @Attribute
    @Property(description = "Name of a prior stage to load the template image from.")
    private String templateStageName;

    /**
     * Name of a prior stage to load the working model from.
     */
    @Attribute
    @Property(description = "Name of a prior stage to load the working model from.")
    private String modelStageName;

    /**
     * If maxVal is below this value, then no matches will be reported. Default is 0.4.
     */
    @Attribute
    @Property(description = "If maximum value is below this value, then no matches will be reported. Default is 0.4.")
    private double threshold = 0.4f;

    public String getTemplateStageName() {
        return templateStageName;
    }

    public void setTemplateStageName(String templateStageName) {
        this.templateStageName = templateStageName;
    }

    public String getModelStageName() {
        return modelStageName;
    }

    public void setModelStageName(String modelStageName) {
        this.modelStageName = modelStageName;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {

      Object model = null;
      // check for the existance of an input model
      if (modelStageName == null || modelStageName.trim().equals("")) {
        //model = (List<?>)pipeline.getWorkingModel();
        return null;
        
      } else {
      
        model = (List<?>)pipeline.getResult(modelStageName.toString()).model;
      }
      Mat originalImage = pipeline.getWorkingImage().clone();
      
      if (model == null || originalImage == null) {
        return null;
      }
      // check for the existance of a template
      if (templateStageName == null || templateStageName.trim().equals("")) {
        return null;
      }
      Result template = pipeline.getResult(templateStageName.toString());
      // no template image is an error
      if (template == null || template.image == null) {
      
        return null;
      }
      
      Mat timage = template.image.clone();
      Result result = new Result(originalImage);
      RotatedRect rrect = null;
      
      if (model instanceof RotatedRect) {
      
        rrect = ((RotatedRect) model).clone();
        
      } else if (
        model instanceof List<?> && 
        ((List<?>) model).get(0) instanceof RotatedRect
      ){
        rrect = ((RotatedRect) ((List<?>)model).get(0)).clone();
        
      } else {
        // only RotatedRects are handled
        return null;
      }
      // put the model at a known orientation, upright, or portrait 
      if (rrect.size.width > rrect.size.height) {
        rrect.angle +=90.0;
        double tmp = rrect.size.width;
        rrect.size.width = rrect.size.height;
        rrect.size.height = tmp;
      }
      // store original rect for later use
      RotatedRect orect = rrect.clone();
            
      // crop the model to reduce processing time
      Rect bbox = rrect.boundingRect();
      // find the largest possible dimension for the crop, i.e. the diagonal, using the Pythagorean theorem
      double msz = Math.sqrt(Math.pow(bbox.width,2) + Math.pow(bbox.height,2));
      // cropped image will be square
      Size sz = new Size(msz,msz);
      // create the new image matrix
      Mat image = new Mat(sz,originalImage.type());
      // crop the image
      Imgproc.getRectSubPix(originalImage, sz,rrect.center,image);
      // adjust rrect to the new center
      rrect.center.x = sz.width/2.0;
      rrect.center.y = sz.height/2.0;
            // we need a RotatedRect model for the template
      RotatedRect trect =  null;
      if (template.model == null) {
        // no model in template so, make one up
        trect = new RotatedRect(
          new org.opencv.core.Point(((int)timage.size().width) / 2, ((int)timage.size().height) / 2), 
          timage.size(), 
          0.0
        );
      } else {
        trect = ((RotatedRect)template.model).clone();
      }
      // first rotation
      // rotate the template to be the same as rrect
      timage = rotateRect(timage,trect,-rrect.angle);

      // variables to keep score and winning rotation
      double maxscore = 0;
      int winrot = 0;
      // we will be advancing the rotation in steps of 90 deg
      double angleAdv = 90.0;
      
      // match template 4 times, each differing by 90deg 
      for (int i=1; i<=4; i++) {
        // first rotation already done
        if (i>1) {
          // fast rotate/flip the template 90deg
          Core.flip(timage.t(), timage, 1);
          // reset rect center  to the center of the image
          trect.center.x = timage.size().width/2.0;
          trect.center.y = timage.size().height/2.0;
          trect.angle -= angleAdv;
          /*
          file = new File("tests/template"+i+".png");
          Highgui.imwrite(file.getAbsolutePath(), timage);
          drawRotatedRect(timage, trect, new Scalar(255,255,255));
          */
        }
        Result mresult = matchTemplate(image,timage);
        List<TemplateMatch> matches = (List<TemplateMatch>) mresult.model;
        double stageScore = 0;
        // get the best of local matches
        for (int j = 0; j < matches.size(); j++) {
          TemplateMatch match = matches.get(j);
          double x = match.x;
          double y = match.y;
          double score = match.score;
          double width = match.width;
          double height = match.height;
          if (score > maxscore) {
            maxscore = score;
            winrot = i;
          }
          if (score > stageScore) {
            stageScore = score;
          }
        }
      }
      // correct original model's angle to the orientation detected
      orect.angle = rrect.angle + (winrot-1) * angleAdv;
      
      result = new Result(originalImage, new ArrayList<RotatedRect>());
      
      if (winrot != 0) {
        
        orect.angle = -orect.angle % 360.0;
                
      } else {
        // No match was found, deliver original model unchanged. What else can we do?
        orect = (RotatedRect) ((List<?>)model).get(0);
      }
      ((List<RotatedRect>)result.model).add(orect);
      return result;
    }

    Result matchTemplate(Mat mat, Mat template) {
    
        Mat result = new Mat();

        // TODO: externalize type
        Imgproc.matchTemplate(mat, template, result, Imgproc.TM_CCOEFF_NORMED);

        MinMaxLocResult mmr = Core.minMaxLoc(result);
        double maxVal = mmr.maxVal;

        double rangeMax = maxVal;

        // Since matchTemplate type is fixed to TM_CCOEFF_NORMED, corr is not actually needed
        // Using just threshold is enought
        List<TemplateMatch> matches = new ArrayList<>();
        for (Point point : matMaxima(result, threshold, rangeMax)) {
            int x = point.x;
            int y = point.y;
            TemplateMatch match = new TemplateMatch(x, y, template.cols(), template.rows(),
                    result.get(y, x)[0]);
            matches.add(match);
        }

        Collections.sort(matches, new Comparator<TemplateMatch>() {
            @Override
            public int compare(TemplateMatch o1, TemplateMatch o2) {
                return ((Double) o2.score).compareTo(o1.score);
            }
        });


        return new Result(result, matches);
    }

    enum MinMaxState {
        BEFORE_INFLECTION, AFTER_INFLECTION
    }

    static List<Point> matMaxima(Mat mat, double rangeMin, double rangeMax) {
        List<Point> locations = new ArrayList<>();

        int rEnd = mat.rows() - 1;
        int cEnd = mat.cols() - 1;

        // CHECK EACH ROW MAXIMA FOR LOCAL 2D MAXIMA
        for (int r = 0; r <= rEnd; r++) {
            MinMaxState state = MinMaxState.BEFORE_INFLECTION;
            double curVal = mat.get(r, 0)[0];
            for (int c = 1; c <= cEnd; c++) {
                double val = mat.get(r, c)[0];

                if (val == curVal) {
                    continue;
                }
                else if (curVal < val) {
                    if (state == MinMaxState.BEFORE_INFLECTION) {
                        // n/a
                    }
                    else {
                        state = MinMaxState.BEFORE_INFLECTION;
                    }
                }
                else { // curVal > val
                    if (state == MinMaxState.BEFORE_INFLECTION) {
                        if (rangeMin <= curVal && curVal <= rangeMax) { // ROW
                                                                        // MAXIMA
                            if (0 < r && (mat.get(r - 1, c - 1)[0] >= curVal
                                    || mat.get(r - 1, c)[0] >= curVal)) {
                                // cout << "reject:r-1 " << r << "," << c-1 <<
                                // endl;
                                // - x x
                                // - - -
                                // - - -
                            }
                            else if (r < rEnd && (mat.get(r + 1, c - 1)[0] > curVal
                                    || mat.get(r + 1, c)[0] > curVal)) {
                                // cout << "reject:r+1 " << r << "," << c-1 <<
                                // endl;
                                // - - -
                                // - - -
                                // - x x
                            }
                            else if (1 < c && (0 < r && mat.get(r - 1, c - 2)[0] >= curVal
                                    || mat.get(r, c - 2)[0] > curVal
                                    || r < rEnd && mat.get(r + 1, c - 2)[0] > curVal)) {
                                // cout << "reject:c-2 " << r << "," << c-1 <<
                                // endl;
                                // x - -
                                // x - -
                                // x - -
                            }
                            else {
                                locations.add(new Point(c - 1, r));
                            }
                        }
                        state = MinMaxState.AFTER_INFLECTION;
                    }
                    else {
                        // n/a
                    }
                }

                curVal = val;
            }

            // PROCESS END OF ROW
            if (state == MinMaxState.BEFORE_INFLECTION) {
                if (rangeMin <= curVal && curVal <= rangeMax) { // ROW MAXIMA
                    if (0 < r && (mat.get(r - 1, cEnd - 1)[0] >= curVal
                            || mat.get(r - 1, cEnd)[0] >= curVal)) {
                        // cout << "rejectEnd:r-1 " << r << "," << cEnd-1 <<
                        // endl;
                        // - x x
                        // - - -
                        // - - -
                    }
                    else if (r < rEnd && (mat.get(r + 1, cEnd - 1)[0] > curVal
                            || mat.get(r + 1, cEnd)[0] > curVal)) {
                        // cout << "rejectEnd:r+1 " << r << "," << cEnd-1 <<
                        // endl;
                        // - - -
                        // - - -
                        // - x x
                    }
                    else if (1 < r && mat.get(r - 1, cEnd - 2)[0] >= curVal
                            || mat.get(r, cEnd - 2)[0] > curVal
                            || r < rEnd && mat.get(r + 1, cEnd - 2)[0] > curVal) {
                        // cout << "rejectEnd:cEnd-2 " << r << "," << cEnd-1 <<
                        // endl;
                        // x - -
                        // x - -
                        // x - -
                    }
                    else {
                        locations.add(new Point(cEnd, r));
                    }
                }
            }
        }

        return locations;
    }

    static Mat rotateRect(Mat mat, RotatedRect rect, double degrees) {
      // get the affine mattrix
      Mat mapMatrix = Imgproc.getRotationMatrix2D(rect.center, degrees, 1.0);
      // adjust rect angle to coincide with the rotation. This modifies the model
      rect.angle -= degrees;
      // find the new bbox for the now rotated rect
      Rect bbox = rect.boundingRect();
      // adjust transformation matrix
      double[] cx = mapMatrix.get(0, 2);
      double[] cy = mapMatrix.get(1, 2);
      cx[0] += bbox.width / 2D - rect.center.x;
      cy[0] += bbox.height / 2D - rect.center.y;
      mapMatrix.put(0, 2, cx);
      mapMatrix.put(1, 2, cy);
      // rotate and crop
      Imgproc.warpAffine(mat, mat, mapMatrix, bbox.size(), Imgproc.INTER_LINEAR);
      mapMatrix.release();
      // adjust the model to the new center
      bbox = rect.boundingRect();
      rect.center.x = bbox.width/2.0;
      rect.center.y = bbox.height/2.0;
      return mat;
    }

}
