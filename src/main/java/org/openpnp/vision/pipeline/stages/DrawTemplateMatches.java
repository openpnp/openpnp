package org.openpnp.vision.pipeline.stages;

import java.awt.Color;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.openpnp.vision.FluentCv;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.CvStage.Result.TemplateMatch;
import org.openpnp.vision.pipeline.stages.convert.ColorConverter;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.convert.Convert;

/**
 * Draws TemplatesMatches contained in a List<TemplateMatch> by referencing a previous stage's model data.
 */
public class DrawTemplateMatches extends CvStage {
    @Element(required = false)
    @Convert(ColorConverter.class)
    private Color color = null;

    @Attribute(required = false)
    private String templateMatchesStageName = null;

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public String getTemplateMatchesStageName() {
        return templateMatchesStageName;
    }

    public void setTemplateMatchesStageName(String templateMatchesStageName) {
        this.templateMatchesStageName = templateMatchesStageName;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        if (templateMatchesStageName == null) {
            return null;
        }
        Result result = pipeline.getResult(templateMatchesStageName);
        if (result == null || result.model == null) {
            return null;
        }
        Mat mat = pipeline.getWorkingImage();

        List<TemplateMatch> matches = (List<TemplateMatch>) result.model;
        for (int i = 0; i < matches.size(); i++) {
            TemplateMatch match = matches.get(i);
            double x = match.x;
            double y = match.y;
            double score = match.score;
            double width = match.width;
            double height = match.height;
            Color color_ = this.color == null ? FluentCv.indexedColor(i) : this.color;
            Scalar color = FluentCv.colorToScalar(color_);
            Core.rectangle(mat, new org.opencv.core.Point(x, y),
                    new org.opencv.core.Point(x + width, y + height), color);
            Core.putText(mat, "" + score, new org.opencv.core.Point(x + width, y + height),
                    Core.FONT_HERSHEY_PLAIN, 1.0, color);
        }

        return null;
    }
}
