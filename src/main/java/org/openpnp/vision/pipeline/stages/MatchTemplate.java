package org.openpnp.vision.pipeline.stages;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.CvStage.Result.TemplateMatch;
import org.simpleframework.xml.Attribute;

/**
 * OpenCV based image template matching with local maxima detection improvements from FireSight:
 * https://github.com/firepick1/FireSight. Scans the working image for matches of a template image
 * and returns a list of matches.
 */
public class MatchTemplate extends CvStage {
    /**
     * Name of a prior stage to load the template image from.
     */
    @Attribute
    private String templateStageName;

    /**
     * If maxVal is below this value, then no matches will be reported. Default is 0.7.
     */
    @Attribute
    private double threshold = 0.7f;

    /**
     * Normalized recognition threshold in the interval [0,1]. Used to determine best match of
     * candidates. For CV_TM_CCOEFF, CV_TM_CCOEFF_NORMED, CV_TM_CCORR, and CV_TM_CCORR_NORMED
     * methods, this is a minimum threshold for positive recognition; for all other methods, it is a
     * maximum threshold. Default is 0.85.
     */
    @Attribute
    private double corr = 0.85f;

    public String getTemplateStageName() {
        return templateStageName;
    }

    public void setTemplateStageName(String templateStageName) {
        this.templateStageName = templateStageName;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public double getCorr() {
        return corr;
    }

    public void setCorr(double corr) {
        this.corr = corr;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        if (templateStageName == null) {
            return null;
        }

        Mat mat = pipeline.getWorkingImage();
        Mat template = pipeline.getResult(templateStageName).image;
        Mat result = new Mat();

        // TODO: externalize type
        Imgproc.matchTemplate(mat, template, result, Imgproc.TM_CCOEFF_NORMED);

        MinMaxLocResult mmr = Core.minMaxLoc(result);
        double maxVal = mmr.maxVal;

        double rangeMin = Math.max(threshold, corr * maxVal);
        double rangeMax = maxVal;


        List<TemplateMatch> matches = new ArrayList<>();
        for (Point point : matMaxima(result, rangeMin, rangeMax)) {
            int x = point.x;
            int y = point.y;
            TemplateMatch match = new TemplateMatch(x, y, template.cols(), template.rows(),
                    result.get(y, x)[0] / maxVal);
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

}
