package org.openpnp.vision.pipeline.stages;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.imgproc.Imgproc;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.simpleframework.xml.Attribute;


/*
<cv-pipeline>
<stages>
   <cv-stage class="org.openpnp.vision.pipeline.stages.ImageRead" name="0" enabled="true" file="/Users/jason/Projects/openpnp/private/Pictures/Vision Tests/OpenPnP-Bottom_Vision_Test_Images-2016-03-17/13_r45.png"/>
   <cv-stage class="org.openpnp.vision.pipeline.stages.MaskCircle" name="4" enabled="true" diameter="550"/>
   <cv-stage class="org.openpnp.vision.pipeline.stages.ConvertColor" name="5" enabled="true" conversion="Bgr2HsvFull"/>
   <cv-stage class="org.openpnp.vision.pipeline.stages.MaskHsv" name="6" enabled="true" hue-min="40" hue-max="130" saturation-min="0" saturation-max="255" value-min="0" value-max="255"/>
   <cv-stage class="org.openpnp.vision.pipeline.stages.ConvertColor" name="7" enabled="true" conversion="Hsv2BgrFull"/>
   <cv-stage class="org.openpnp.vision.pipeline.stages.ConvertColor" name="2" enabled="true" conversion="Bgr2Gray"/>
   <cv-stage class="org.openpnp.vision.pipeline.stages.Threshold" name="9" enabled="true" threshold="240" auto="false" invert="false"/>
   <cv-stage class="org.openpnp.vision.pipeline.stages.BottomVisionCriS" name="1" enabled="true" threshold="240"/>
   <cv-stage class="org.openpnp.vision.pipeline.stages.ImageRecall" name="8" enabled="true" image-stage-name="0"/>
   <cv-stage class="org.openpnp.vision.pipeline.stages.DrawRotatedRects" name="3" enabled="true" rotated-rects-stage-name="1" thickness="1">
      <color r="255" g="0" b="102" a="255"/>
   </cv-stage>
</stages>
</cv-pipeline>
*/

public class BottomVisionCriS extends CvStage {
    @Attribute
    int threshold = 240;
    
    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    private static RotatedRect getRotatedRect(MatOfPoint contour) {
        if (contour == null)
            return null;
        MatOfPoint2f contour_ = new MatOfPoint2f();
        contour.convertTo(contour_, CvType.CV_32FC1);
        return Imgproc.minAreaRect(contour_);
    }

    private static List<MatOfPoint> joinContours(List<MatOfPoint> contours) {
        List<MatOfPoint> ret = new ArrayList<>();
        // filter it
        List<MatOfPoint> contours2 = new ArrayList<>(contours);
        for (MatOfPoint contour : contours) {
            RotatedRect rotatedRect = getRotatedRect(contour);
            double area = rotatedRect.size.area();
            if (area <= 5.0) {
                contours2.remove(contour);
            }
        }
        contours = contours2;

        final ArrayList<Point> temp = new ArrayList<>();
        for (MatOfPoint contour : contours) {
            temp.addAll(((MatOfPoint) contour).toList());
        }

        contours.clear();
        if (temp.size() != 0) {
            MatOfPoint mat = new MatOfPoint();
            mat.fromList(temp);
            contours.add(mat);
        }
        return contours;
    }

    static public Mat rotate(Mat img, double angle) {
        double radians = Math.toRadians(angle);
        Point center = new Point(img.size().width / 2, img.size().height / 2);
        Mat rotImage = Imgproc.getRotationMatrix2D(center, angle, 1.0);
        Imgproc.warpAffine(img, img, rotImage, img.size(),
                Imgproc.INTER_LINEAR + Imgproc.CV_WARP_FILL_OUTLIERS);
        return img;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        Mat mat = pipeline.getWorkingImage();
        Mat tmp = new Mat();
        // Do OpenCV magic
        List<MatOfPoint> contours = new ArrayList<>();
        RotatedRect rot = null;
        double ang;
        Imgproc.threshold(mat, tmp, threshold, 255, 0);
        Imgproc.findContours(tmp.clone(), contours, new Mat(), Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_SIMPLE);
        if (contours.size() == 0) {
            return new Result(mat, contours);
        }
        // join, get angle,
        contours = joinContours(contours);
        rot = getRotatedRect(contours.get(0));
        ang = rot.angle % 45;
        // roate and repeat match, get center
        tmp = rotate(tmp, -ang);
        contours.clear();
        Imgproc.findContours(tmp.clone(), contours, new Mat(), Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_SIMPLE);
        contours = joinContours(contours);
        rot = getRotatedRect(contours.get(0));
        
        rot.angle = ang;
        return new Result(mat, rot);
    }

}
