package org.openpnp.vision.pipeline.stages;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.openpnp.vision.FluentCv;
import org.openpnp.vision.FluentCv.ColorSpace;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Property;
import org.openpnp.vision.pipeline.Stage;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.core.Commit;

@Stage(description="Mask color from an image based on the HSV color space. Pixels that fall between (hueMin, saturationMin, valueMin) and (hueMax, saturationMax, valueMax) are set to black in the output image. This stage expects the input to be in HSV_FULL format, so you should do a ConvertColor with Bgr2HsvFull before this stage and ConvertColor Hsv2BgrFull after. These are not applied internally as to not complicate the use of multiple instances of this stage in series. Note that this stage can be used with any 3 channel, 8 bit per channel color space. The order of the filtered channels is hue, saturation, value, but you can use these ranges for other channels.")
public class MaskHsv extends CvStage {
    @Attribute(required=false)
    @Property(description="Sets each channel's min and max limits such that the pixels falling in and around the channel's histogram peak bin are masked.  The number of histogram bins that get masked is controlled by the value of the fractionToMask parameter.")
    private Boolean auto = false;
    
    @Attribute(required=false)
    @Property(description="When the auto flag is set, the min and max limits are set so that the fraction of pixels in the image that get masked is approximately equal to this value.  The pixels with the most commonly occurring colors are masked first.   Valid range is from 0.0 to 1.0 (inclusive).")
    private double fractionToMask = 0.0;
    
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

    @Attribute(required = false)
    @Property(description = "Soft edge of the HSV mask bounding box. If not 0, the mask is computed and applied with gradual impact, creating a more natural result.")
    private int softEdge = 0;

    @Attribute(required = false)
    @Property(description = "Soft factor, i.e. how strongly the mask is applied, from 0 ... 1.0.")
    private double softFactor = 1;

    @Attribute(required=false)
    @Property(description="Inverts the selection of pixels to mask.")
    private Boolean invert;

    @Attribute(required = false)
    @Property(description = "If set, the mask is returned directly as a grayscale image with the masked area black, the unmasked white. Otherwise the masked area is blackened in the source image.")
    private boolean binaryMask = false;

    @Attribute(required = false)
    @Property(description = "Name of the property through which OpenPnP controls this stage. Use \"MaskHsv\" for standard control.")
    private String propertyName = "MaskHsv";

    public Boolean getAuto() {
        return auto;
    }
    
    public void setAuto(Boolean auto) {
        this.auto = auto;
    }
    
    public double getFractionToMask() {
        return fractionToMask;
    }
    
    public void setFractionToMask(double fractionToMask) {
        if ((fractionToMask >= 0.0) && (fractionToMask <= 1.0)) {
            this.fractionToMask = fractionToMask;
        }
    }
    
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

    public int getSoftEdge() {
        return softEdge;
    }

    public void setSoftEdge(int softEdge) {
        this.softEdge = softEdge;
    }

    public double getSoftFactor() {
        return softFactor;
    }

    public void setSoftFactor(double softFactor) {
        this.softFactor = softFactor;
    }

    public Boolean getInvert() {
        return invert;
    }

    public void setInvert(Boolean invert) {
        this.invert = invert;
    }

    public boolean isBinaryMask() {
        return binaryMask;
    }

    public void setBinaryMask(boolean binaryMask) {
        this.binaryMask = binaryMask;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    @Commit
    public void commit() {
        //This method gets called by the deserializer when configuration .xml files are loading.  It checks the format of
        //each maskHsv and converts any that are in the old format (without an invert flag) to the new format (with an
        //invert flag) correcting hue limits if needed.  This will make the new format backward compatible with old format.
        if (invert == null) {
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

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        commit();
        softFactor = Math.max(0, softFactor);

        Mat mat = pipeline.getWorkingImage();
        Mat masked = mat.clone();
        Scalar color = FluentCv.colorToScalar(Color.black);
        Mat mask = mat.clone();
        mask.setTo(color);
        masked.setTo(color);

        if (auto) {
            autoAdjust(mat, mask, color);
        }
        int hueMin = getPossiblePipelinePropertyOverride(this.hueMin, pipeline, propertyName+".hueMin");
        int hueMax = getPossiblePipelinePropertyOverride(this.hueMax, pipeline, propertyName+".hueMax");
        int saturationMin = getPossiblePipelinePropertyOverride(this.saturationMin, pipeline, propertyName+".saturationMin");
        int saturationMax = getPossiblePipelinePropertyOverride(this.saturationMax, pipeline, propertyName+".saturationMax");
        int valueMin = getPossiblePipelinePropertyOverride(this.valueMin, pipeline, propertyName+".valueMin");
        int valueMax = getPossiblePipelinePropertyOverride(this.valueMax, pipeline, propertyName+".valueMax");

        if (softEdge > 0 || softFactor < 1) {
            int cols = mat.cols();
            int rows = mat.rows();
            int channels = mat.channels();
            int linearSize = cols*rows*channels;
            byte[] pixelSamples = new byte[linearSize]; 
            byte[] pixelMask = new byte[cols*rows];
            Arrays.fill(pixelMask, (byte)255);
            double complementFactor = 1 - softFactor;
            byte floor = (byte) (complementFactor*255.999);
            boolean factorOne = softFactor == 1.0;
            boolean hueAll = (hueMin == 0 && hueMax == 255);
            int saturationMinE = saturationMin == 0 ? saturationMin - softEdge : saturationMin;
            int saturationMaxE = saturationMax == 255 ? saturationMax + softEdge : saturationMax;
            int valueMinE = valueMin == 0 ? valueMin - softEdge : valueMin;
            int valueMaxE = valueMax == 255 ? valueMax + softEdge : valueMax;
            int hueMid = hueAll ? 128 : hueMin <= hueMax ? (hueMin + hueMax)/2 : (((256 + hueMin + hueMax)/2)&0xFF);
            int hueSpan = hueAll ? 128 : Math.max(0, (((hueMax - hueMin)&0xFF) + 1 )/2  - softEdge/2);
            int saturationMid = (saturationMinE + saturationMaxE)/2;
            int saturationSpan = Math.max(0, (saturationMaxE - saturationMinE + 1)/2 - softEdge/2);
            int valueMid = (valueMinE + valueMaxE)/2;
            int valueSpan =  Math.max(0, (valueMaxE - valueMinE + 1)/2 - softEdge/2);
            int softEdgeSq = softEdge*softEdge;

            mat.get(0, 0, pixelSamples);
            for (int i = 0, j = 0; i < linearSize; i += channels, j++) {
                int hue = Byte.toUnsignedInt(pixelSamples[i]);
                int saturation = Byte.toUnsignedInt(pixelSamples[i+1]);
                int value = Byte.toUnsignedInt(pixelSamples[i+2]);
                int hueDS = Math.abs(hue - hueMid);
                int hueD = Math.max(0, (hueDS < 128 ? hueDS : 256 - hueDS) - hueSpan);
                int saturationD = Math.max(0, Math.abs(saturation - saturationMid) - saturationSpan);
                int valueD = Math.max(0, Math.abs(value - valueMid) - valueSpan);
                int sq = hueD*hueD + saturationD*saturationD + valueD*valueD;
                if (sq >= softEdgeSq || sq == 0) {
                    if (sq == 0 ^ invert) {
                        // Inside mask.
                        if (factorOne) {
                            hue = saturation = value = 0;
                        }
                        else {
                            //saturation = (byte)(saturation*complementFactor);
                            value = (byte)(value*complementFactor);
                        }
                        pixelMask[j] = floor;
                    }
                }
                else {
                    // Inside soft edge.
                    double d = //Math.sqrt(sq)/softEdge;
                            Math.cos(Math.PI*Math.sqrt(sq)/softEdge)*-0.5+0.5;
                    if (invert) {
                        d = 1 - d;
                    }
                    double f = softFactor*d + complementFactor;
                    //saturation = (int) (value*f);
                    value = (int) (value*f);
                    pixelMask[j] = (byte) (255.999*f);
                }
                pixelSamples[i] = (byte) hue;
                pixelSamples[i+1] = (byte) saturation;
                pixelSamples[i+2] = (byte) value;
            }
            if (binaryMask) {
                masked.release();
                mask.release();
                mask = new Mat(rows, cols, CvType.CV_8U);
                mask.put(0, 0, pixelMask);
                return new Result(mask, ColorSpace.Gray);
            }
            else {
                mask.release();
                masked.put(0, 0, pixelSamples);
                return new Result(masked);
            }
        }
        else {
            // Change to have the possibility to work inside the interval or outside (when min>max)
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
                mask2.release();
            }

            //The mask is normally inverted because it is used to copy the unmasked portions of the
            //image into the final result.
            if (!invert) {
                Core.bitwise_not(mask, mask);
            }
            double fractionActuallyMasked = 1.0 - Core.countNonZero(mask) / (double) ( mat.rows() * mat.cols() ) ;
            Logger.trace( "Fraction actually masked = " + fractionActuallyMasked );
            if (binaryMask) {
                masked.release();
                return new Result(mask, ColorSpace.Gray);
            } else {
                mat.copyTo(masked, mask);
                mask.release();
                return new Result(masked);
            }
        }
    }

    protected void autoAdjust(Mat mat, Mat mask, Scalar color) {
        // Note that in the code below, because hue, saturation, and value are each considered separately
        // with the final mask being generated as the logical AND of the three; the fraction of pixels
        // actually masked will end up being less than the input parameter fractionToMask.  That could be
        // fixed by doing a binary search on amountToMask to drive the actual number of pixels masked to
        // the desired value but for now we'll keep it simple and just live with that fact.
        
        int numberOfBins = 256;
        
        Mat workingMat = mat.clone();
        workingMat.setTo(color);
        
        //Copy all pixels of the image where Value is not zero (black) into a new working image
        Scalar min = new Scalar(0, 0, 1);
        Scalar max = new Scalar(255, 255, 255);
        Core.inRange(mat, min, max, mask);
        mat.copyTo(workingMat, mask); //all pixels where Value = 0 will now also have Hue = Saturation = 0

        //Compute the number of masked pixels in the original image
        double numberOfOriginallyMaskedPixels = mat.rows() * mat.cols() - Core.countNonZero(mask);
        
        double amountToMask = fractionToMask * ( mat.rows() * mat.cols() - numberOfOriginallyMaskedPixels );
        
        Mat mv = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC1);
        
        //Compute Hue histogram
        Core.extractChannel(workingMat, mv, 0);
        ArrayList<Mat> listOfMat = new ArrayList<Mat>();
        listOfMat.add(mv);
        MatOfInt channels = new MatOfInt(0);
        Mat hist = new Mat(numberOfBins,1,CvType.CV_64F);
        MatOfInt histSize = new MatOfInt(numberOfBins); // number of bins
        MatOfFloat ranges = new MatOfFloat(0, 256); // range of data (upper range is exclusive)
        Imgproc.calcHist(listOfMat, channels, new Mat(), hist, histSize, ranges);
        
        //Adjust the zero bin for the pixels already masked
        hist.put(0, 0, hist.get(0, 0)[0] - numberOfOriginallyMaskedPixels );
        
        //Logger.trace( "histH = " + hist.dump() );
        
        //The peak of the hue histogram is found, i.e., the most common hue
        Core.MinMaxLocResult histMinMaxLoc = Core.minMaxLoc(hist);
        int peakIdx = (int) histMinMaxLoc.maxLoc.y;
        Logger.trace( "Hue peakIdx = " + peakIdx );
        
        double amountMasked = 0;
        double currentLevel = 0;
        double lastLevel = Double.MAX_VALUE;
        int startIdx = peakIdx;
        int endIdx = ( peakIdx - 1 + numberOfBins ) % numberOfBins;
        boolean upDown = true;
        
        //Now working equally down both sides of the histogram peak, sum the bins
        //until the desired quantity around the peak is found.  Note that for
        //asymmetric distributions, this will result in the peak NOT being centered
        //between the lower and upper limits. 
        while ( amountMasked < amountToMask ) {
            if ( upDown ) {
                //working in the positive direction
                endIdx = ( endIdx + 1 ) % numberOfBins; //circular increment used here because hue is a circular quantity     
                currentLevel = hist.get(endIdx, 0)[0];
                amountMasked = amountMasked + currentLevel;
            } else {
                //working in the negative direction
                startIdx = ( startIdx - 1 + numberOfBins ) % numberOfBins; //circular decrement  
                currentLevel = hist.get(startIdx, 0)[0];
                amountMasked = amountMasked + currentLevel;
            }
            if (currentLevel <= lastLevel) {
                lastLevel = currentLevel;
                upDown = !upDown;
            }
        }
        if (amountToMask == 0) {
            //Use -1 to indicate no masking as no actual values will be equal to this value
            endIdx = -1;
            startIdx = -1;
        }
        setHueMax( endIdx );
        Logger.trace( "hueMax = " + hueMax );
        setHueMin( startIdx );
        Logger.trace( "hueMin = " + hueMin );
        
         
        //Compute Saturation histogram
        Core.extractChannel(workingMat, mv, 1);
        listOfMat.clear();
        listOfMat.add(mv);
        Imgproc.calcHist(listOfMat, channels, new Mat(), hist, histSize, ranges);
        
        //Adjust the zero bin for the pixels already masked
        hist.put(0, 0, hist.get(0, 0)[0] - numberOfOriginallyMaskedPixels );
        
        //Logger.trace( "histS = " + hist.dump() );
        
        //The peak of the saturation histogram is found, i.e., the most common saturation
        histMinMaxLoc = Core.minMaxLoc(hist);
        peakIdx = (int) histMinMaxLoc.maxLoc.y;
        Logger.trace( "Saturation peakIdx = " + peakIdx );
        
        amountMasked = 0;
        currentLevel = 0;
        lastLevel = Double.MAX_VALUE;
        startIdx = peakIdx;
        endIdx = peakIdx - 1;
        upDown = true;
        
        //Now working equally down both sides of the histogram peak, sum the bins
        //until the desired quantity around the peak is found.
        while ( (amountMasked < amountToMask) && ( (startIdx > 0) || (endIdx < (numberOfBins-1)) ) ) {
            if ( upDown ) {
                if ( endIdx < (numberOfBins - 1) ) {
                    //working in the positive direction
                    endIdx = endIdx + 1;
                    currentLevel = hist.get(endIdx, 0)[0];
                    amountMasked = amountMasked + currentLevel;
                    if ( (currentLevel < lastLevel) && (startIdx > 0) ) {
                        //reverse direction
                        lastLevel = currentLevel;
                        upDown = !upDown;
                    }
                } else {
                    currentLevel = 0;
                    upDown = false;
                }
            } else {
                if ( startIdx > 0 ) {
                    //working in the negative direction
                    startIdx = startIdx - 1;
                    currentLevel = hist.get(startIdx, 0)[0];
                    amountMasked = amountMasked + currentLevel;
                    if ( (currentLevel < lastLevel) && (endIdx < (numberOfBins-1)) ) {
                        //reverse direction
                        lastLevel = currentLevel;
                        upDown = !upDown;
                    }
                } else {
                    currentLevel = 0;
                    upDown = true;
                }
            }
        }
        if (amountToMask == 0) {
            //Use -1 to indicate no masking as no actual values will be equal to this value
            endIdx = -1;
            startIdx = -1;
        }
        setSaturationMax( endIdx );
        Logger.trace( "saturationMax = " + saturationMax );
        setSaturationMin( startIdx );
        Logger.trace( "saturationMin = " + saturationMin );

        
        //Compute Value histogram
        Core.extractChannel(workingMat, mv, 2);
        listOfMat.clear();
        listOfMat.add(mv);
        Imgproc.calcHist(listOfMat, channels, new Mat(), hist, histSize, ranges);
        
        //Adjust the zero bin for the pixels already masked
        hist.put(0, 0, hist.get(0, 0)[0] - numberOfOriginallyMaskedPixels );
        
        //Logger.trace( "histV = " + hist.dump() );
        
        //The peak of the value histogram is found, i.e., the most common value
        histMinMaxLoc = Core.minMaxLoc(hist);
        peakIdx = (int) histMinMaxLoc.maxLoc.y;
        Logger.trace( "Value peakIdx = " + peakIdx );
        
        amountMasked = 0;
        currentLevel = 0;
        lastLevel = Double.MAX_VALUE;
        startIdx = peakIdx;
        endIdx = peakIdx - 1;
        upDown = true;
        
        //Now working equally down both sides of the histogram peak, sum the bins
        //until the desired quantity around the peak is found.
        while ( (amountMasked < amountToMask) && ( (startIdx > 0) || (endIdx < (numberOfBins-1)) ) ) {
            if ( upDown ) {
                if ( endIdx < (numberOfBins - 1) ) {
                    //working in the positive direction
                    endIdx = endIdx + 1;
                    currentLevel = hist.get(endIdx, 0)[0];
                    amountMasked = amountMasked + currentLevel;
                    if ( (currentLevel < lastLevel) && (startIdx > 0) ) {
                        //reverse direction
                        lastLevel = currentLevel;
                        upDown = !upDown;
                    }
                } else {
                    currentLevel = 0;
                    upDown = false;
                }
            } else {
                if ( startIdx > 0 ) {
                    //working in the negative direction
                    startIdx = startIdx - 1;
                    currentLevel = hist.get(startIdx, 0)[0];
                    amountMasked = amountMasked + currentLevel;
                    if ( (currentLevel < lastLevel) && (endIdx < (numberOfBins-1)) ) {
                        //reverse direction
                        lastLevel = currentLevel;
                        upDown = !upDown;
                    }
                } else {
                    currentLevel = 0;
                    upDown = true;
                }
            }
        }
        if (amountToMask == 0) {
            //Use -1 to indicate no masking as no actual values will be equal to this value
            endIdx = -1;
            startIdx = -1;
        }
        workingMat.release();
        mv.release();
        hist.release();
        ranges.release();
        channels.release();
        histSize.release();
        setValueMax( endIdx );
        Logger.trace( "valueMax = " + valueMax );
        setValueMin( startIdx );
        Logger.trace( "valueMin = " + valueMin );
    }
}
