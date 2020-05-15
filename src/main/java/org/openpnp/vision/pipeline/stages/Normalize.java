package org.openpnp.vision.pipeline.stages;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Stage;

@Stage(description="On color images it does RGB Max algorithm, removes shadow and looses color information.")
public class Normalize extends CvStage {

    private void filter(Mat src){
	  Mat dst = src.clone();
	  dst.convertTo(dst, CvType.CV_64FC3); // New line added. 
	  int size = (int) (dst.total() * dst.channels());
	  double[] pixel = new double[size]; // use double[] instead of byte[]
	  dst.get(0, 0, pixel);
          for(int i = 0; i < size; i+=src.channels())
          {
	    double s=pixel[i+0]+pixel[i+1]+pixel[i+2];
	    if(s!=0.0) {
	    	pixel[i+0]=(pixel[i+0]/s)*255;
	    	pixel[i+1]=(pixel[i+1]/s)*255;
	    	pixel[i+2]=(pixel[i+2]/s)*255;
	    }
          }
	  src.put(0, 0, pixel);
	}

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        Mat mat = pipeline.getWorkingImage();
	if(mat.channels()==1) {
		Core.normalize(mat, mat, 0, 255, Core.NORM_MINMAX);	
	} else {
		filter(mat);
	}

        return new Result(mat);
    }
}
