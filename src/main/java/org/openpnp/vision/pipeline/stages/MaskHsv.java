package org.openpnp.vision.pipeline.stages;

import java.awt.Color;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.openpnp.vision.FluentCv;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Property;
import org.openpnp.vision.pipeline.Stage;
import org.simpleframework.xml.Attribute;

@Stage(description="Mask color from an image based on the HSV color space. Pixels that fall between (hueMin, saturationMin, valueMin) and (hueMax, saturationMax, valueMax) are set to black in the output image. This stage expects the input to be in HSV_FULL format, so you should do a ConvertColor with Bgr2HsvFull before this stage and ConvertColor Hsv2BgrFull after. These are not applied internally as to not complicate the use of multiple instances of this stage in series. Note that this stage can be used with any 3 channel, 8 bit per channel color space. The order of the filtered channels is hue, saturation, value, but you can use these ranges for other channels.")
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

    @Attribute(required = false)
    @Property(description = "If set, the mask is returned directly as a grayscale image with the masked area black, the unmasked white. Otherwise the masked area is blackened in the source image.")
    private boolean binaryMask = false;

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

    public boolean isBinaryMask() {
        return binaryMask;
    }

    public void setBinaryMask(boolean binaryMask) {
        this.binaryMask = binaryMask;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        Mat mat = pipeline.getWorkingImage();
        Mat mask = mat.clone();
        Mat masked = mat.clone();
        Scalar color = FluentCv.colorToScalar(Color.black);
        mask.setTo(color);
        masked.setTo(color);
        // FCA Change to have the possibility to work inside the interval or outside (when min>max)
        Scalar min;
        Scalar max;
        if (hueMin <= hueMax) {
            min = new Scalar(hueMin, saturationMin, valueMin);
            max = new Scalar(hueMax, saturationMax, valueMax);
        }
        else {
            min = new Scalar(hueMax, saturationMin, valueMin);
            max = new Scalar(hueMin, saturationMax, valueMax);
        }
        Core.inRange(mat, min, max, mask);

        if (hueMin <= hueMax) {
            Core.bitwise_not(mask, mask);
        }

        if (binaryMask) {
            return new Result(mask);
        } 
        else {
            mat.copyTo(masked, mask);
            return new Result(masked);
        }
    }
}
