package org.openpnp.vision.pipeline.stages;

import java.awt.Color;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.openpnp.vision.FluentCv;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.simpleframework.xml.Attribute;

/**
 * Remove color from an image based on the HSV color space. Pixels that fall between (hueMin,
 * saturationMin, valueMin) and (hueMax, saturationMax, valueMax) are set to black in the output
 * image.
 * 
 * This stage expects the input to be in HSV_FULL format, so you should do a ConvertColor
 * with Bgr2HsvFull before this stage and ConvertColor Hsv2BgrFull after. These are not applied
 * internally as to not complicate the use of multiple instances of this stage in series.
 * 
 * Note that this stage can be used with any 3 channel, 8 bit per channel color space. The order
 * of the filtered channels is hue, saturation, value, but you can use these ranges for other
 * channels.
 */
public class MaskHsv extends CvStage {
    @Attribute
    private int hueMin = 31;

    @Attribute
    private int hueMax = 116;

    @Attribute
    private int saturationMin = 0;

    @Attribute
    private int saturationMax = 255;

    @Attribute
    private int valueMin = 0;

    @Attribute
    private int valueMax = 255;

    public int getHueMin() {
        return hueMin;
    }

    public void setHueMin(int hueMin) {
        this.hueMin = hueMin;
    }

    public int getHueMax() {
        return hueMax;
    }

    public void setHueMax(int hueMax) {
        this.hueMax = hueMax;
    }

    public int getSaturationMin() {
        return saturationMin;
    }

    public void setSaturationMin(int saturationMin) {
        this.saturationMin = saturationMin;
    }

    public int getSaturationMax() {
        return saturationMax;
    }

    public void setSaturationMax(int saturationMax) {
        this.saturationMax = saturationMax;
    }

    public int getValueMin() {
        return valueMin;
    }

    public void setValueMin(int valueMin) {
        this.valueMin = valueMin;
    }

    public int getValueMax() {
        return valueMax;
    }

    public void setValueMax(int valueMax) {
        this.valueMax = valueMax;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        Mat mat = pipeline.getWorkingImage();
        Mat mask = mat.clone();
        Mat masked = mat.clone();
        Scalar color = FluentCv.colorToScalar(Color.black);
        mask.setTo(color);
        masked.setTo(color);
        Core.inRange(mat, new Scalar(hueMin, saturationMin, valueMin), new Scalar(hueMax, saturationMax, valueMax), mask);
        Core.bitwise_not(mask, mask);
        mat.copyTo(masked, mask);
        return new Result(masked);
    }
}
