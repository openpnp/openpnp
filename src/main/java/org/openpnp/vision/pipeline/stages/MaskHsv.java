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
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.core.Commit;

@Stage(description="Mask color from an image based on the HSV color space. Pixels that fall between (hueMin, saturationMin, valueMin) and (hueMax, saturationMax, valueMax) are set to black in the output image. This stage expects the input to be in HSV_FULL format, so you should do a ConvertColor with Bgr2HsvFull before this stage and ConvertColor Hsv2BgrFull after. These are not applied internally as to not complicate the use of multiple instances of this stage in series. Note that this stage can be used with any 3 channel, 8 bit per channel color space. The order of the filtered channels is hue, saturation, value, but you can use these ranges for other channels.")
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

    @Attribute(required=false)
    @Property(description="Inverts the selection of pixels to mask.")
    private Boolean invert;
    
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

    public Boolean getInvert() {
        return invert;
    }

    public void setInvert(Boolean invert) {
        this.invert = invert;
    }


    @Commit
    public void commit() {
        //This method gets called by the deserializer when configuration .xml files are loading.  It checks the format of
        //each maskHsv and converts any that are in the old format (without an invert flag) to the new format (with an
        //invert flag) correcting hue limits if needed.  This will make the new format backward compatible with old format.
        if (invert == null) {
            Logger.trace( "Old format found in .xml file, converting to new format..." );
            if (hueMin > hueMax) {
                Logger.trace( "    Swapping hue limits and setting invert to true." );
                int temp = hueMax;
                hueMax = hueMin;
                hueMin = temp;
                invert = true;
            }
            else {
                Logger.trace( "    Keeping hue limits as is and setting invert to false." );
                invert = false;
            }
        }
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

        //The mask is normally inverted because it is used to copy the unmasked portions of the
        //image into the final result.
        if (!invert) {
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
