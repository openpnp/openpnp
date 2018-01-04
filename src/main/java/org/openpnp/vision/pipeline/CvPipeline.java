package org.openpnp.vision.pipeline;

import java.io.IOException;
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
import org.openpnp.vision.pipeline.CvStage.Result;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.AnnotationStrategy;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.stream.Format;
import org.simpleframework.xml.stream.HyphenStyle;
import org.simpleframework.xml.stream.Style;

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
public class CvPipeline implements AutoCloseable {
    static {
        nu.pattern.OpenCV.loadShared();
        System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
    }

    @ElementList
    private ArrayList<CvStage> stages = new ArrayList<>();

    private Map<CvStage, Result> results = new HashMap<CvStage, Result>();
    
    private Map<String, Object> properties = new HashMap<String, Object>();

    private Mat workingImage;
    private Object workingModel;
    
    private long totalProcessingTimeNs;
    
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
        return getResult(getStage(name));
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
        return results.get(stage);
    }

    /**
     * Get the current working image. Primarily intended to be called from CvStage implementations.
     * 
     * @return
     */
    public Mat getWorkingImage() {
        if (workingImage == null || (workingImage.cols() == 0 && workingImage.rows() == 0)) {
            workingImage = new Mat(480, 640, CvType.CV_8UC3, new Scalar(0, 0, 0));
            Core.line(workingImage, new Point(0, 0), new Point(640, 480), new Scalar(0, 0, 255));
            Core.line(workingImage, new Point(640, 0), new Point(0, 480), new Scalar(0, 0, 255));
        }
        return workingImage;
    }

    public Object getWorkingModel() {
      return workingModel;
    }

    public long getTotalProcessingTimeNs() {
      return totalProcessingTimeNs;
    }

    public void setTotalProcessingTimeNs(long totalProcessingTimeNs) {
      this.totalProcessingTimeNs = totalProcessingTimeNs;
    }

    public void process() {
        totalProcessingTimeNs = 0;
        release();
        for (CvStage stage : stages) {
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
            processingTimeNs = System.nanoTime() - processingTimeNs;
            totalProcessingTimeNs += processingTimeNs;

            Mat image = null;
            Object model = null;
            if (result != null) {
                image = result.image;
                model = result.model;
            }
            if(stage.isEnabled() && model != null) {
              workingModel=model;
            }
            // If the result image is null and there is a working image,
            // replace the result image with a clone of the working image.
            if (image == null) {
                if (workingImage != null) {
                    image = workingImage.clone();
                }
            }
            // If the result image is not null:
            // Release the working image if the result image is different.
            // Replace the working image with the result image.
            // Clone the result image for storage.
            else {
                if (workingImage != null && workingImage != image) {
                    workingImage.release();
                }
                workingImage = image;
                image = image.clone();
            }

            results.put(stage, new Result(image, model, processingTimeNs));
        }
    }

    /**
     * Release any temporary resources associated with the processing of the pipeline. Should be
     * called when the pipeline is no longer needed. This is primarily to release retained native
     * resources from OpenCV.
     */
    public void release() {
        if (workingImage != null) {
            workingImage.release();
            workingImage = null;
        }
        for (Result result : results.values()) {
            if (result.image != null) {
                result.image.release();
            }
        }
        workingModel = null;
        results.clear();
    }
    
    @Override
    public void close() throws IOException {
        release();
    }
    
    @Override
    protected void finalize() throws Throwable {
        release();
        super.finalize();
    }

    /**
     * Convert the pipeline to an XML string that can be read back in with #fromXmlString.
     * 
     * @return
     * @throws Exception
     */
    public String toXmlString() throws Exception {
        Serializer ser = createSerializer();
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
        Serializer ser = createSerializer();
        StringReader sr = new StringReader(s);
        CvPipeline pipeline = ser.read(CvPipeline.class, sr);
        stages.clear();
        for (CvStage stage : pipeline.getStages()) {
            add(stage);
        }
    }

    private String generateUniqueName() {
        for (int i = 0;; i++) {
            String name = "" + i;
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
    
    public Object getProperty(String name) {
        return properties.get(name);
    }
    
    public void setProperty(String name, Object value) {
        properties.put(name, value);
    }
    
    private static Serializer createSerializer() {
        Style style = new HyphenStyle();
        Format format = new Format(style);
        AnnotationStrategy strategy = new AnnotationStrategy();
        Serializer serializer = new Persister(strategy, format);
        return serializer;
    }
}
