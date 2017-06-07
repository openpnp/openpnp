package org.openpnp.vision.pipeline;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.openpnp.model.Configuration;
import org.openpnp.model.Part;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.Feeder;
import org.openpnp.vision.pipeline.CvStage.Result;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Serializer;


/**
 * A CvPipeline performs computer vision operations on a working image by processing in series a
 * list of CvStage instances. Each CvStage instance can modify the working image and return a new
 * image along with data extracted from the image. After processing the image callers can get access
 * to the images and models from each stage.
 * 
 * CvPipeline is serializable using toXmlString and fromXmlString. This makes it easy to export
 * pipelines and exchange them with others.
 * 
 * This work takes inspiration from several existing projects:
 * 
 * FireSight by Karl Lew and Šimon Fojtů: https://github.com/firepick1/FireSight
 * 
 * RoboRealm: http://www.roborealm.com/
 * 
 * TODO: Add measuring to image window.
 * 
 * TODO: Add info showing pixel coordinates when mouse is in image window.
 */
@Root
public class CvPipeline {
    static {
        nu.pattern.OpenCV.loadShared();
        System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
    }

    @ElementList
    private ArrayList<CvStage> stages = new ArrayList<>();

    private Map<String, Result> results = new HashMap<String, Result>();

    private Mat workingImage;

    private Camera camera;
    private Nozzle nozzle;
    private Feeder feeder;
    private Part	part;
    
    private long totalProcessingTimeNs;

	
    /**************************************************************************
     * Setter for internal flags
     * Different Naming is used, because internal use only || checkstyle don't allow that
     * Every flag have a prepending comment line that describes there use
	 * Default value for flag must be false.
     */
	 
	// indicate if camera is downlooking
	private boolean downLooking;
	// indicate if fiducial is searched
	private boolean fiducial=true;
	// if inside editor, different path is used, no optimisations
	private boolean editor=true; // TODO need to be false, actually for legacy code.
    public void setOptionEditor(boolean val) { 	editor=val; }
	
	// return single result, looping disabled
	private boolean single;
    public void setOptionSingle(boolean val) {  single=val; }
    /**************************************************************************/

    public CvPipeline() {
        
    }
    
    public CvPipeline(String xmlPipeline) {
        try {
            fromXmlString(xmlPipeline);
        }
        catch (Exception e) {
            throw new Error(e);
        }
    }

    /**
     * Add the given CvStage to the end of the pipeline using the given name. If name is null a
     * unique one will be generated and set on the stage.
     * 
     * @param name
     * @param stage
     */
    public void add(String name, CvStage stage) {
        if (name == null) {
            name = generateUniqueName();
        }
        stage.setName(name);
        stages.add(stage);
    }

    /**
     * Add the given CvStage to the end of the pipeline. If the stage does not have a name a unique
     * one will be generated and set on the stage.
     * 
     * @param stage
     */
    public void add(CvStage stage) {
        add(stage.getName(), stage);
    }

    public void insert(String name, CvStage stage, int index) {
        if (name == null) {
            name = generateUniqueName();
        }
        stage.setName(name);
        stages.add(index, stage);
    }

    public void insert(CvStage stage, int index) {
        insert(stage.getName(), stage, index);
    }

    public void remove(String name) {
        remove(getStage(name));
    }

    public void remove(CvStage stage) {
		results.remove(stage.getName()); 
        stages.remove(stage);
    }

    public List<CvStage> getStages() {
        return Collections.unmodifiableList(stages);
    }

    public CvStage getStage(String name) {
        if (name == null) {
            return null;
        }
        for (CvStage stage : stages) {
            if (stage.getName().equals(name)) {
                return stage;
            }
        }
        return null;
    }

    /**
     * Get the Result returned by the CvStage with the given name. May return null if the stage did
     * not return a result.
     * 
     * @param name
     * @return
     */
    public Result getResult(String name) {
        if (name == null) {
            return null;
        }
        return results.get(name);
    }

    /**
     * Get the Result returned by give CvStage. May return null if the stage did not return a
     * result.
     * 
     * @param stage
     * @return
     */
    public Result getResult(CvStage stage) {
        if (stage == null) {
            return null;
        }
        return results.get(stage.getName());
    }

	List <Mat> workingImages = new ArrayList<>();
    /**
     * Get the current working image. Primarily intended to be called from CvStage implementations.
     * 
     * @return
     */
    public Mat getWorkingImage() {
		Mat img;
        if (workingImage == null || (workingImage.cols() == 0 && workingImage.rows() == 0)) {
            workingImage = new Mat(480, 640, CvType.CV_8UC3, new Scalar(0, 0, 255));
			Core.rectangle(workingImage, new Point(10,10), new Point(470,630), new Scalar(0), -1);
			workingImages.add(workingImage);
        }
		img = workingImage.clone();
		workingImages.add(img);
        return img;
    }

    public void setWorkingImage(Mat img) { // img get not released !!!
        workingImage = img;
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
		downLooking = camera.getHead()!=null;
		if(!downLooking) {
			fiducial=false;
		}
    }

    public Camera getCamera() {
        return camera;
    }

    public void setNozzle(Nozzle nozzle) {
        this.nozzle = nozzle;
    }

    public Nozzle getNozzle() {
        return nozzle;
    }
  
    public void setFeeder(Feeder feeder) {
		fiducial = false;
        this.feeder = feeder;
    }

    public Feeder getFeeder() {
        return feeder;
    }
  
    public void setPart(Part part) {
        this.part = part;
    }

    public Part getPart() {
        return part;
    }

    public long getTotalProcessingTimeNs() {
      return totalProcessingTimeNs;
    }

	
	private boolean isNull(Object obj) {
							if(obj!=null) {
								if(obj instanceof List<?>) {
									if(((List)obj).size()==0) {
										return true;
									}
									return false;
								}
								return false;	
							}
							return true;	
	}
	
    public void process() {
        long totalProcessingTimeNs = System.nanoTime();
        release();
		int mark=-1,last=stages.size()-1,pos=0;
		for(int limit=1000;pos>=0&&pos<=last;pos++,limit--) {
			CvStage stage = stages.get(pos);
		    // Process and time the stage and get the result.
            long processingTimeNs = System.nanoTime();
            Result result = null;
            try {
                if (!stage.isEnabled()) {
                    throw new Exception("Stage not enabled.");
                }
                result = stage.process(this);
            }
            catch (Exception e) {
                result = new Result(null, e);
            }

            Mat image = null;
            Object model = null;
            if (result != null) {
                image = result.image;
                model = result.model;
            }

            // If the result image is null and there is a working image, replace the result image
            // replace the result image with a clone of the working image.
            if (image != null && workingImage!=image ) {
                workingImage = image;
				if(!workingImages.contains(image)) {
					workingImages.add(image);
				}
            }
			String id = stage.getName();
			result= new Result(workingImage, model, processingTimeNs = System.nanoTime() - processingTimeNs);
			if(id.startsWith("result")&&id.length()==7) {
				if(id.charAt(6)=='_') {
					if(isNull(model)) {
						mark = -1;
						results.put("result", result);
						pos=last; // break;
					} else {
						if(!single) {
							mark = pos;
						}
					}
				}
				if(Character.isDigit(id.charAt(6))) {
					if(!isNull(model)) {
						results.put("result", result);
						if(!editor) {
							pos=last; // break;
						}
					}
				}
			}
			results.put(id, result);
			if(pos==last&&mark>-1) {
				if(limit<0) { break ; }
				pos=--mark;
			}
        }
            totalProcessingTimeNs = (System.nanoTime() - totalProcessingTimeNs );
			this.totalProcessingTimeNs=totalProcessingTimeNs;
    }

    /**
     * Release any temporary resources associated with the processing of the pipeline. Should be
     * called when the pipeline is no longer needed. This is primarily to release retained native
     * resources from OpenCV.
     */
    public void release() {
        results.clear();
		for(Mat img: workingImages) {
			img.release();
		}
		workingImage=null;
		workingImages.clear();
    }

    /**
     * Convert the pipeline to an XML string that can be read back in with #fromXmlString.
     * 
     * @return
     * @throws Exception
     */
    public String toXmlString() throws Exception {
        Serializer ser = Configuration.createSerializer();
        StringWriter sw = new StringWriter();
        ser.write(this, sw);
        return sw.toString();
    }

    /**
     * Parse the pipeline in the given String and replace the current pipeline with the results.
     * 
     * @param s
     * @throws Exception
     */
    public void fromXmlString(String s) throws Exception {
        release();
        Serializer ser = Configuration.createSerializer();
        StringReader sr = new StringReader(s);
        CvPipeline pipeline = ser.read(CvPipeline.class, sr);
        stages.clear();
        for (CvStage stage : pipeline.getStages()) {
            add(stage);
        }
    }

    private String generateUniqueName() {
        for (int i = 0;; i++) {
            String name = "S" + i;
            if (getStage(name) == null) {
                return name;
            }
        }
    }

    @Override
    public CvPipeline clone() throws CloneNotSupportedException {
        try {
            return new CvPipeline(toXmlString());
        }
        catch (Exception e) {
            throw new CloneNotSupportedException(e.getMessage());
        }
    }
}
