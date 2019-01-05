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
import org.openpnp.util.OpenCvUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.CvStage.Result.TemplateMatch;
import org.openpnp.vision.pipeline.Property;
import org.openpnp.vision.pipeline.Stage;
import org.simpleframework.xml.Attribute;

/**
 * OpenCV based image template matching with local maxima detection improvements from FireSight:
 * https://github.com/firepick1/FireSight. Scans the working image for matches of a template image
 * and returns a list of matches.
 */
@Stage(category = "Image Processing",
        description = "OpenCV based image template matching with local maxima detection improvements.")

public class MatchTemplate extends CvStage {
    /**
     * Name of a prior stage to load the template image from.
     */
    @Attribute
    @Property(description = "Name of a prior stage to load the template image from.")
    private String templateStageName;

    /**
     * If maxVal is below this value, then no matches will be reported. Default is 0.7.
     */
    @Attribute
    @Property(description = "If maximum value is below this value, then no matches will be reported. Default is 0.7.")
    private double threshold = 0.7f;

    /**
     * Normalized recognition threshold in the interval [0,1]. Used to determine best match of
     * candidates. For CV_TM_CCOEFF, CV_TM_CCOEFF_NORMED, CV_TM_CCORR, and CV_TM_CCORR_NORMED
     * methods, this is a minimum threshold for positive recognition; for all other methods, it is a
     * maximum threshold. Default is 0.85.
     */
    @Attribute
    @Property(description = "Normalized minimum recognition threshold for the CCOEFF_NORMED method, in the interval [0,1]. Default is 0.85.")
    private double corr = 0.85f;

    @Attribute(required = false)
    @Property(description = "Normalize results to maximum value.")
    private boolean normalize = true;

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

    public boolean isNormalize() {
        return normalize;
    }

    public void setNormalize(boolean normalize) {
        this.normalize = normalize;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        if (templateStageName == null) {
            return null;
        }

        Mat mat = pipeline.getWorkingImage();
        Mat template = pipeline.getResult(templateStageName).image;
        Mat result = new Mat();

        Imgproc.matchTemplate(mat, template, result, Imgproc.TM_CCOEFF_NORMED);

        MinMaxLocResult mmr = Core.minMaxLoc(result);
        double maxVal = mmr.maxVal;

        double rangeMin = Math.max(threshold, corr * maxVal);
        double rangeMax = maxVal;


        List<TemplateMatch> matches = new ArrayList<>();
        for (Point point : OpenCvUtils.matMaxima(result, rangeMin, rangeMax)) {
            int x = point.x;
            int y = point.y;
            TemplateMatch match = new TemplateMatch(x, y, template.cols(), template.rows(),
                    result.get(y, x)[0] / (normalize? maxVal : 1.0));
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
}
