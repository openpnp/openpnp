/* 
Copyright (c) 2017, Cri.S <phone.cri@gmail.com>
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. All advertising materials mentioning features or use of this software
   must display the following acknowledgement:
   This product includes software developed by the <organization>.
4. Neither the name of the <organization> nor the
   names of its contributors may be used to endorse or promote products
   derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY <COPYRIGHT HOLDER> ''AS IS'' AND ANY
EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
 
 package org.openpnp.vision.pipeline.stages;

import java.awt.Color;
import java.util.List;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.FileNotFoundException;
import org.opencv.core.Mat;
import org.opencv.core.RotatedRect;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.FeatureDetector;
import org.openpnp.model.Configuration;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.stages.convert.ColorConverter;
import org.simpleframework.xml.Attribute;


public class SimpleBlob extends CvStage {
	@Attribute
	private double ThresholdStep = 10.;  	public double getThresholdStep()  { return ThresholdStep; }  public void setThresholdStep(double val) { ThresholdStep=val; }
	@Attribute
	private double ThresholdMin = 50.;  	public double getThresholdMin()  { return ThresholdMin; }  public void setThresholdMin(double val) { ThresholdMin=val; }
	@Attribute
	private double ThresholdMax= 220.;  	public double getThresholdMax()  { return ThresholdMax; }  public void setThresholdMax(double val) { ThresholdMax=val; }
	@Attribute
	private int Repeatability= 2;  	public int getRepeatability()  { return Repeatability; }  public void setRepeatability(int val) { Repeatability=val; }
	@Attribute
	private double DistBetweenBlobs= 10.;  	public double getDistBetweenBlobs()  { return DistBetweenBlobs; }  public void setDistBetweenBlobs(double val) { DistBetweenBlobs=val; }
	@Attribute
	private boolean Color= true;  	public boolean getColor()  { return Color; }  public void setColor(boolean val) { Color=val; }
	@Attribute
	private double ColorValue= 0;  	public double getColorValue()  { return ColorValue; }  public void setColorValue(double val) { ColorValue=val; }
	@Attribute
	private boolean Area= true;  	public boolean getArea()  { return Area; }  public void setArea(boolean val) { Area=val; }
	@Attribute
	private double AreaMin= 25.;  	public double getAreaMin()  { return AreaMin; }  public void setAreaMin(double val) { AreaMin=val; }
	@Attribute
	private double AreaMax= 5000.;  	public double getAreaMax()  { return AreaMax; }  public void setAreaMax(double val) { AreaMax=val; }
	@Attribute
	private boolean Circularity= false;  	public boolean getCircularity()  { return Circularity; }  public void setCircularity(boolean val) { Circularity=val; }
	@Attribute
	private double CircularityMin= 0.80000001192092896;  	public double getCircularityMin()  { return CircularityMin; }  public void setCircularityMin(double val) { CircularityMin=val; }
	@Attribute
	private double CircularityMax= -1;  	public double getCircularityMax()  { return CircularityMax; }  public void setCircularityMax(double val) { CircularityMax=val; }
	@Attribute
	private boolean Inertia= true;  	public boolean getInertia()  { return Inertia; }  public void setInertia(boolean val) { Inertia=val; }
	@Attribute
	private double InertiaRatioMin= 1.0000000149011612E-001;  	public double getInertiaRatioMin()  { return InertiaRatioMin; }  public void setInertiaRatioMin(double val) { InertiaRatioMin=val; }
	@Attribute
	private double InertiaRatioMax= -1;  	public double getInertiaRatioMax()  { return InertiaRatioMax; }  public void setInertiaRatioMax(double val) { InertiaRatioMax=val; }
	@Attribute
	private boolean Convexity= true;  	public boolean getConvexity()  { return Convexity; }  public void setConvexity(boolean val) { Convexity=val; }
	@Attribute
	private double ConvexityMin= 9.4999998807907104E-001;  	public double getConvexityMin()  { return ConvexityMin; }  public void setConvexityMin(double val) { ConvexityMin=val; }
	@Attribute
	private double ConvexityMax= -1;  	public double getConvexityMax()  { return ConvexityMax; }  public void setConvexityMax(double val) { ConvexityMax=val; }

/****************************************/
/* compatibility with standard openPnP */
/**************************************/
	final boolean setup = false;
	void display(Mat mat) {;}
/***********************************/
    
	private void writeToFile(File file, String data) throws Exception {
	   FileOutputStream stream = new FileOutputStream(file);
		OutputStreamWriter outputStreamWriter = new OutputStreamWriter(stream);
		outputStreamWriter.write(data);
		outputStreamWriter.close();
		stream.close();
	}	

	private void createParams() throws Exception {
		File outputFile = File.createTempFile("Detector", ".YAML", Configuration.get().getConfigurationDirectory());
		writeToFile(outputFile, "%YAML:1.0" // java parameter backdoor
						+"\nthresholdStep: "+ThresholdStep
						+"\nminThreshold: "+ThresholdMin
						+"\nmaxThreshold: "+ThresholdMax
						+"\nminRepeatability: "+Repeatability
						+"\nminDistBetweenBlobs: "+DistBetweenBlobs
						+"\nfilterByColor: "+(Color?1:0)
						+"\nblobColor: "+ColorValue
						+"\nfilterByArea: "+(Area?1:0)
						+"\nminArea: "+AreaMin
						+"\nmaxArea: "+(AreaMax<0.?3.4028234663852886E+038:AreaMax)
						+"\nfilterByCircularity: "+(Circularity?1:0) 
						+"\nminCircularity: "+CircularityMin
						+"\nmaxCircularity: "+(CircularityMax<0.?3.4028234663852886E+038:CircularityMax)
						+"\nfilterByInertia: "+(Inertia?1:0)
						+"\nminInertiaRatio: "+InertiaRatioMin
						+"\nmaxInertiaRatio: "+(InertiaRatioMax<0.?3.4028234663852886E+038:InertiaRatioMax)
						+"\nfilterByConvexity: "+(Convexity?1:0)
						+"\nminConvexity: "+ConvexityMin
						+"\nmaxConvexity: "+(ConvexityMax<0.?3.4028234663852886E+038:ConvexityMax)
						+"\n");
		blobDetector.read(outputFile.getAbsolutePath()); outputFile.delete();
	}
			
    public Result process(CvPipeline pipeline) throws Exception {
        Mat src = pipeline.getWorkingImage();
		FeatureDetector blobDetector = FeatureDetector.create(FeatureDetector.SIMPLEBLOB);
		createParams();
		MatOfKeyPoint keypoints = new MatOfKeyPoint();
		blobDetector.detect(src,keypoints); 
		if(setup) {
			Mat dst= new Mat();
			org.opencv.core.Scalar cores = new org.opencv.core.Scalar(0,0,255);
			org.opencv.features2d.Features2d.drawKeypoints(src,keypoints,dst,cores,2);
			display(dst);
		}
		return new Result(keypoints);
	}
}

