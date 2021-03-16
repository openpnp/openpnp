package org.openpnp.vision.pipeline.stages;

import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Property;
import org.openpnp.vision.pipeline.Stage;
import org.simpleframework.xml.Attribute;
import org.opencv.core.RotatedRect;


@Stage(description="It uses pixel sizes. Check part's pixel sizes on detected RotatedRect (i.e. MinAreaRect) and put the same values into this stage. Place this stage after the Pipeline that returns RotatedRect (i.e. MinAreaRect...) and use it as 'results' stage. Rename MinAreaRect into 'result'.")
public class SizeCheck extends CvStage {
    @Attribute
    @Property(description="This value is inaccuracy tolerance +/- pixel size that is considered to be valid.")
    private int tolerance = 5;

    @Attribute
    @Property(description="1st component's size (pixels).")
    private int sizeW = 7;

    @Attribute
    @Property(description="2nd component's size (pixels).")
    private int sizeH = 28;

  
    public int getTolerance() {
        return tolerance;
    }

    public void setTolerance(int tolerance) {
        this.tolerance = tolerance;
    }

    public int getSizeW() {
        return sizeW;
    }

    public void setSizeW(int sizeW) {
        this.sizeW = sizeW;
    }

    public int getSizeH() {
        return sizeH;
    }

    public void setSizeH(int sizeH) {
        this.sizeH = sizeH;
    }
     
    @Override
    public Result process(CvPipeline pipeline) throws Exception {

    	Object model = pipeline.getWorkingModel();
    	RotatedRect r = (RotatedRect)model; 
    
    	if(Math.abs(Math.max(r.size.width, r.size.height)-Math.max(sizeH, sizeW))<=tolerance) { 
    		if(Math.abs(Math.min(r.size.width, r.size.height)-Math.min(sizeH, sizeW))<=tolerance) {
    			return new Result(null,model);
    		}
    	}
    	return null;
    }
}
