package org.openpnp.vision.pipeline.stages;

import java.awt.Color;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;

import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.openpnp.vision.FluentCv;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Property;
import org.openpnp.vision.pipeline.Stage;
import org.simpleframework.xml.Attribute;
import org.pmw.tinylog.Logger;

@Stage(description="Find expected rectangle centered on nozzle.")
public class DetectRectangle extends CvStage {

    @Attribute
    @Property(description = "Index of rectangle to return.")
    private int index = 0;

    @Attribute
    @Property(description = "Minimum accumulator count to be considered a line.")
    private int threshold = 20;

    @Attribute
    @Property(description = "Weight of accumulator count that contributes to score")
    private double accWeight = 0.1;

    @Attribute
    @Property(description = "Maximum angle deviation of lines to be considered a pair.")
    private double deltaTheta = 0.03;

    @Attribute
    @Property(description = "Maximum angle deviation of pairs to be considered a rectangle")
    private double deltaAlpha = 0.03;

    @Attribute
    @Property(description = "Maximum symmetry deviation of lines to be considered a pair")
    private double deltaRho = 20;

    @Attribute
    @Property(description = "Minimum line separation to be considered a pair")
    private double minRho = 10;

    @Attribute
    @Property(description = "Estimated pair distance A")
    private double rhoA = 15;

    @Attribute
    @Property(description = "Estimated pair distance B")
    private double rhoB = 15;

    public int getIndex() {
        return this.index;
    }

    public void setIndex(int v) {
        this.index = v;
    }
    
    public int getThreshold() {
        return this.threshold;
    }

    public void setThreshold(int v) {
        this.threshold = v;
    }
    
    public double getAccWeight() {
        return this.accWeight;
    }

    public void setAccWeight(double v) {
        this.accWeight = v;
    }
    
    public double getDeltaTheta() {
        return this.deltaTheta;
    }

    public void setDeltaTheta(double v) {
        this.deltaTheta = v;
    }
    
    public double getDeltaAlpha() {
        return this.deltaAlpha;
    }

    public void setDeltaAlpha(double v) {
        this.deltaAlpha = v;
    }
    
    public double getDeltaRho() {
        return this.deltaRho;
    }

    public void setDeltaRho(double v) {
        this.deltaRho = v;
    }
    
    public double getMinRho() {
        return this.minRho;
    }

    public void setMinRho(double v) {
        this.minRho = v;
    }
    
    public double getRhoA() {
        return this.rhoA;
    }

    public void setRhoA(double v) {
        this.rhoA = v;
    }
    
    public double getRhoB() {
        return this.rhoB;
    }

    public void setRhoB(double v) {
        this.rhoB = v;
    }
    
    private class HoughLine {
        final private double rho;
        final private double theta;

        private HoughLine(double rho, double theta) {
            this.rho = rho;
            this.theta = theta;
        }
    }

    private class HoughPair {
        final private int[] lines;
        final private double alpha;
        final private double xi;

        private HoughPair(int a, int b, double alpha, double xi) {
            this.lines = new int[2];
            this.lines[0] = a;
            this.lines[1] = b;
            this.alpha = alpha;
            this.xi = xi;
        }
    }

    private class HoughRect {
        final private HoughPair[] pairs;
        final private double score;

        private HoughRect(HoughPair a, HoughPair b, double score) {
            this.pairs = new HoughPair[2];
            this.pairs[0] = a;
            this.pairs[1] = b;
            this.score = score;
        }
    }

    private void drawLine(Mat image, Mat lines, int lineNo) {
        double rho = lines.get(lineNo, 0)[0];
        double theta = lines.get(lineNo, 0)[1];
        double a = Math.cos(theta);
        double b = Math.sin(theta);
        double x0 = a * rho;
        double y0 = b * rho;
        Point pt1 = new Point(x0 + 1000.0*(-b), y0 + 1000.0*(a));
        Point pt2 = new Point(x0 - 1000.0*(-b), y0 - 1000.0*(a));
        Scalar c = FluentCv.colorToScalar(Color.white);
        Imgproc.line(image, pt1, pt2, c, 1);
    }
    
    private void drawRect(Mat image, Mat lines, HoughRect rect) {
        drawLine(image, lines, rect.pairs[0].lines[0]);
        drawLine(image, lines, rect.pairs[0].lines[1]);
        drawLine(image, lines, rect.pairs[1].lines[0]);
        drawLine(image, lines, rect.pairs[1].lines[1]);
    }

    private List<HoughLine> translateLines(Mat image, Mat matLines) {
        List<HoughLine> lines = new ArrayList<>();

        double halfWidth = image.width() / 2.0;
        double halfHeight = image.height() / 2.0;

        for (int i = 0; i < matLines.rows() && i < 256; i++) {
            double rho = matLines.get(i, 0)[0];
            double theta = matLines.get(i, 0)[1];
            double diff = halfWidth * Math.cos(theta) + halfHeight * Math.sin(theta);
            /* List index matches original OpenCV index */
            lines.add(new HoughLine(rho - diff, theta));
        }

        return lines;
    }

    private List<HoughPair> findPairs(List<HoughLine> lines) {
        /* For every line, see if there is another line that is both parallel,
         * and approximately equidistant from the origin */
        List<HoughPair> pairs = new ArrayList<>();

        for (int i = 0; i < lines.size()-1; i++) {
            HoughLine a = lines.get(i);
            for (int j = i+1; j < lines.size(); j++) {
                HoughLine b = lines.get(j);

                double xi = Math.abs(a.rho - b.rho);
                if ((Math.abs(a.theta - b.theta) < this.deltaTheta) && 
                        (Math.abs(a.rho + b.rho) < this.deltaRho) &&
                                             (xi > this.minRho)) {

                    double alpha = (a.theta + b.theta) / 2.0;
                    pairs.add(new HoughPair(i, j, alpha, xi));
                }
            }
        }

        return pairs;
    }

    private List<HoughRect> findRects(List<HoughPair> pairs) {
        double score;
        List<HoughRect> rects = new ArrayList<>();

        for (int i = 0; i < pairs.size()-1; i++) {
            HoughPair a = pairs.get(i);
            for (int j = i+1; j < pairs.size(); j++) {
                HoughPair b = pairs.get(j);

                if (Math.abs(Math.abs(a.alpha - b.alpha) - Math.PI/2.0) < this.deltaAlpha) {
                    /* FIXME: Surely there's a way to get the actual weight from opencv. Failing that,
                     * lower line indexes have higher accumulator values */
                    score = (a.lines[0] + a.lines[1] + b.lines[0] + b.lines[1]) * this.accWeight;

                    score += Math.min(Math.abs(a.xi - this.rhoA) + Math.abs(b.xi - this.rhoB),
                                      Math.abs(a.xi - this.rhoB) + Math.abs(b.xi - this.rhoA));

                    rects.add(new HoughRect(a, b, score));
                }
            }
        }

        return rects;
    }
    
    private Point getCorner(Mat lines, int a, int b) {
        double rhoA = lines.get(a, 0)[0];
        double rhoB = lines.get(b, 0)[0];
        double thetaA = lines.get(a, 0)[1];
        double thetaB = lines.get(b, 0)[1];
        double det = Math.sin(thetaB - thetaA);
        /* Determinant will never be zero as deltaAlpha guarantees they are not parallel */ 
        double x = (rhoA*Math.sin(thetaB) - rhoB*Math.sin(thetaA)) / det;
        double y = (rhoB*Math.cos(thetaA) - rhoA*Math.cos(thetaB)) / det;
        return new Point(x, y);
    }

    private List<MatOfPoint> getContours(Mat lines, HoughRect rect) {
        List<MatOfPoint> contours = new ArrayList<>();
        List<Point> tmp = new ArrayList<>();
        MatOfPoint mp = new MatOfPoint();
        tmp.add(getCorner(lines, rect.pairs[0].lines[0], rect.pairs[1].lines[0]));
        tmp.add(getCorner(lines, rect.pairs[0].lines[0], rect.pairs[1].lines[1]));
        tmp.add(getCorner(lines, rect.pairs[0].lines[1], rect.pairs[1].lines[0]));
        tmp.add(getCorner(lines, rect.pairs[0].lines[1], rect.pairs[1].lines[1]));
        mp.fromList(tmp);
        contours.add(mp);
        return contours;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        Mat matImage = pipeline.getWorkingImage();
        Mat matLines = new Mat();

        /* Lines are sorted so that the first line has the highest accumulator value */
        Imgproc.HoughLines(matImage, matLines, 1.0, 0.01, this.threshold);

        List<HoughLine> lines = translateLines(matImage, matLines);
        List<HoughPair> pairs = findPairs(lines);
        List<HoughRect> rects = findRects(pairs);

        if (rects.size() <= this.index) {
            matLines.release();
            return new Result(matImage, null);
        }

        /* Sort rects based on score */
        Collections.sort(rects, new Comparator<HoughRect>() {
            @Override
            public int compare(HoughRect a, HoughRect b) {
                return ((Double) a.score).compareTo(b.score);
            }
        });

        drawRect(matImage, matLines, rects.get(this.index));
        List<MatOfPoint> contours = getContours(matLines, rects.get(this.index));

        matLines.release();
        return new Result(matImage, contours);
    }
}
