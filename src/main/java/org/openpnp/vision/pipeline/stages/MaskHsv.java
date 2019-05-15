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

@Stage(description="Remove color from an image based on the HSV color space. Pixels that fall between (hueMin, saturationMin, valueMin) and (hueMax, saturationMax, valueMax) are set to black in the output image. This stage expects the input to be in HSV_FULL format, so you should do a ConvertColor with Bgr2HsvFull before this stage and ConvertColor Hsv2BgrFull after. These are not applied internally as to not complicate the use of multiple instances of this stage in series. Note that this stage can be used with any 3 channel, 8 bit per channel color space. The order of the filtered channels is hue, saturation, value, but you can use these ranges for other channels.")
public class MaskHsv extends CvStage {
    @Attribute
    @Property(description="First hue to be masked.  Note hues range from 0 to 255 (inclusive) but in a circular fashion so that 255 is directly adjacent to 0 (as 359 degrees is adjacent to 0 degrees).  To mask hues that cross the 255-0 boundary, set hueMin greater than hueMax.  As a rough guide, yellows fall in the range 21 to 64, greens 64 to 107, cyans 107 to 149, blues 149 to 192, magentas 192 to 235, and reds 235 to 21.")
    private int hueMin = 31;

    @Attribute
    @Property(description="Last hue to be masked.  Note hues range from 0 to 255 (inclusive) but in a circular fashion so that 255 is directly adjacent to 0 (as 359 degrees is adjacent to 0 degrees).  To mask hues that cross the 255-0 boundary, set hueMin greater than hueMax.  As a rough guide, yellows fall in the range 21 to 64, greens 64 to 107, cyans 107 to 149, blues 149 to 192, magentas 192 to 235, and reds 235 to 21.")
    private int hueMax = 116;

    @Attribute
    @Property(description="Minimum saturation to be masked.  Note saturations range from 0 to 255 (inclusive). Setting saturationMin greater than saturationMax will result in no pixels being masked.")
    private int saturationMin = 0;

    @Attribute
    @Property(description="Maximum saturation to be masked.  Note saturations range from 0 to 255 (inclusive). Setting saturationMax less than saturationMin will result in no pixels being masked.")
    private int saturationMax = 255;

    @Attribute
    @Property(description="Minimum value to be masked.  Note values range from 0 to 255 (inclusive). Setting valueMin greater than valueMax will result in no pixels being masked.")
    private int valueMin = 0;

    @Attribute
    @Property(description="Maximum value to be masked.  Note values range from 0 to 255 (inclusive). Setting valueMax less than valueMin will result in no pixels being masked.")
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
        // FCA Change to have the possibility to work inside the interval or outside (when min>max)
        Scalar min;
        Scalar max;
        if (hueMin <= hueMax) {
            min = new Scalar(hueMin, saturationMin, valueMin);
            max = new Scalar(hueMax, saturationMax, valueMax);
            Core.inRange(mat, min, max, mask);
        }
        else {
            //Hue range wraps past 255 back through 0 so the mask needs to include the range from hueMin
            //to 255 in addition to the range from 0 to hueMax.  To accomplish this, a mask for each separate
            //range is created and then ORed together to form the actual mask.
            min = new Scalar(hueMin, saturationMin, valueMin);
            max = new Scalar(255, saturationMax, valueMax);
            Core.inRange(mat, min, max, mask);
            
            Mat mask2 = mask.clone();
            mask2.setTo(color);
            min = new Scalar(0, saturationMin, valueMin);
            max = new Scalar(hueMax, saturationMax, valueMax);
            Core.inRange(mat, min, max, mask2);
            
            Core.bitwise_or(mask, mask2, mask);
        }

        //The mask is inverted because it is used to copy the unmasked portions of the
        //image into the final result.
        Core.bitwise_not(mask, mask);

        mat.copyTo(masked, mask);
        return new Result(masked);
    }
}
