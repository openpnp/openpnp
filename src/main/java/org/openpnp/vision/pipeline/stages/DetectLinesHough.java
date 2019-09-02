package org.openpnp.vision.pipeline.stages;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Property;
import org.openpnp.vision.pipeline.Stage;
import org.simpleframework.xml.Attribute;
import org.pmw.tinylog.Logger;

/**
 * Finds lines in the working image and stores the results as a List<Circle> on the model. 
 */
@Stage(description="Finds circles in the working image. Diameter and spacing can be specified.")
public class DetectLinesHough extends CvStage {

    @Attribute
    @Property(description = "Distance resolution from center.")
    private double rho = 0.5;

    @Attribute
    @Property(description = "Angular resolution, in degrees.")
    private double theta = Math.PI / 180.0;

    @Attribute
    @Property(description = "Minimum accumulator count.")
    private int threshold = 1000;

    @Attribute
    @Property(description = "Minimum line length, as a percent of the diagonal image length.")
    private double minLineLength = 30.0;

    @Attribute
    @Property(description = "Max line gap, as a percent of the diagonal image length.")
    private double maxLineGap = 5.0;

    public double getRho() {
        return this.rho;
    }

    public void setRho(double v) {
        this.rho = v;
    }

    public double getTheta() {
        return this.theta;
    }

    public void setTheta(double v) {
        this.theta = v;
    }

    public int getThreshold() {
        return this.threshold;
    }

    public void setThreshold(int v) {
        this.threshold = v;
    }

    public double getMinLineLength() {
        return this.minLineLength;
    }

    public void setMinLineLength(double v) {
        this.minLineLength = v;
    }

    public double getMaxLineGap() {
        return this.maxLineGap;
    }

    public void setMaxLineGap(double v) {
        this.maxLineGap = v;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        Mat mat = pipeline.getWorkingImage();

				double m = mat.width();
				double q = mat.height();


				m = m * m;
				q = q * q;
				m = Math.sqrt(m + q);
				q = m;

				m *= this.minLineLength / 100.0;
				q *= this.maxLineGap / 100.0;

        Mat output = new Mat();

        Imgproc.HoughLinesP(mat, output, this.rho, this.theta,
					this.threshold, m, q);

        List<MatOfPoint> contours = new ArrayList<>();
        for (int i = 0; i < output.rows(); i++) {
        	List<Point> tmp = new ArrayList<>();
					tmp.add(new Point(output.get(i, 0)[0],output.get(i, 0)[1]));
					tmp.add(new Point(output.get(i, 0)[2],output.get(i, 0)[3]));
					MatOfPoint mp = new MatOfPoint();
					mp.fromList(tmp);
					contours.add(mp);
        }
				output.release();

        return new Result(null, contours);
    }
}
