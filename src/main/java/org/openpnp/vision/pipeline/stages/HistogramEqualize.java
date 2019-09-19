package org.openpnp.vision.pipeline.stages;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Property;
import org.openpnp.vision.pipeline.Stage;
import org.openpnp.vision.pipeline.stages.ThresholdAdaptive.AdaptiveMethod;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

@Root
@Stage(description="Applies histogram equalization to the selected channels of the image.  For gray scale images this will increase the image contrast.  For color images, the results will vary depending on the image format and channels selected for equalization.  Generally applying histogram equalization to a color image will result in a false color image; however, contrast enhancement can be achieved on HSV formats by applying equalization to only the third channel (V).")
public class HistogramEqualize extends CvStage {

    public enum ChannelsToEqualize {
        First(1),
        Second(2),
        Third(4),
        FirstAndSecond(1+2),
        FirstAndThird(1+4),
        SecondAndThird(2+4),
        All(1+2+4);
        
        private int code;

        ChannelsToEqualize(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }
    
    @Attribute(required=false)
    @Property(description="Selects which channel(s) of the image to equalize.  This setting has no effect on single channel (gray scale) images.")
    private ChannelsToEqualize channelsToEqualize = ChannelsToEqualize.All;
    
    public ChannelsToEqualize getChannelsToEqualize() {
        return channelsToEqualize;
    }

    public void setChannelsToEqualize(ChannelsToEqualize channelsToEqualize) {
        this.channelsToEqualize = channelsToEqualize;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        Mat mat = pipeline.getWorkingImage();
        int nChannels = mat.channels();
        Mat workingMat = new Mat();
        for (int i=0; i<nChannels; i++) {
            if ((nChannels == 1) || ((channelsToEqualize.getCode()>>i) % 2 == 1)) {
                Core.extractChannel(mat, workingMat, i);
                Imgproc.equalizeHist(workingMat, workingMat);
                Core.insertChannel(workingMat, mat, i);
            };
        };
        return null;
    }
}
