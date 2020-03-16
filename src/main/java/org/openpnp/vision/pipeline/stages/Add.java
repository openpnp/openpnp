package org.openpnp.vision.pipeline.stages;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Stage;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

@Stage(description="Adds two images together, scale either, or subtract second.")
public class Add extends CvStage {
    @Element(required = false)

    @Attribute(required = false)
    private String firstStageName = null;

    @Attribute(required = false)
    private String secondStageName = null;
    
    @Attribute(required = false)
    private double firstScalar = 1.0;

    @Attribute(required = false)
    private double secondScalar = 1.0;

    public String getFirstStageName() {
        return firstStageName;
    }

    public void setFirstStageName(String firstStageName) {
        this.firstStageName = firstStageName;
    }

    public String getSecondStageName() {
        return secondStageName;
    }

    public void setSecondStageName(String secondStageName) {
        this.secondStageName = secondStageName;
    }

    public double getFirstScalar() {
        return firstScalar;
    }

    public void setFirstScalar(double v) {
        this.firstScalar = v;
    }

    public double getSecondScalar() {
        return secondScalar;
    }

    public void setSecondScalar(double v) {
        this.secondScalar = v;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        if (firstStageName == null) {
            return null;
        }
        if (secondStageName == null) {
            return null;
        }
        // TODO STOPSHIP memory?
        Mat first = pipeline.getResult(firstStageName).image;
        Mat second = pipeline.getResult(secondStageName).image;

				if(this.firstScalar < 0){
					throw new Exception("firstScalar < 0!");
				}

        Mat f = first.clone();
				if(this.firstScalar != 1.0){
					Core.multiply(first, new Scalar(this.firstScalar), f);
				}

        Mat s = second.clone();
				if(this.secondScalar != 1.0){
					Core.multiply(second, new Scalar(Math.abs(this.secondScalar)), s);
				}
        
        Mat out = new Mat();
				if(this.secondScalar > 0){
	        Core.add(f, s, out);
				}
				else{
	        Core.subtract(f, s, out);
				}
				f.release();
				s.release();

        return new Result(out);
    }
}
