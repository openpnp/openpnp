package org.openpnp.vision.pipeline;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opencv.core.Mat;
import org.openpnp.model.Configuration;
import org.openpnp.vision.pipeline.CvStage.Result;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A CvPipeline performs computer operations on a working image by processing in series a list of
 * CvStage instances. Each CvStage instance can modify the working image and return a new image
 * along with data extracted from the image. After processing the image users can get access to the
 * images and models from each stage.
 * 
 * CvPipeline is serializable using toXmlString and fromXmlString. This makes it easy to export
 * pipelines and exchange them with others.
 */
@Root
public class CvPipeline {
    static {
        nu.pattern.OpenCV.loadShared();
        System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
    }

    private final static Logger logger = LoggerFactory.getLogger(CvPipeline.class);

    @ElementList
    private ArrayList<CvStage> stages = new ArrayList<>();

    private Map<CvStage, Result> results = new HashMap<CvStage, Result>();

    private Mat workingImage;

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
        for (CvStage stage : stages) {
            if (name.equals(stage.getName())) {
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
        return results.get(stage);
    }

    /**
     * Get the current working image. Primarily intended to be called from CvStage implementations.
     * 
     * @return
     */
    public Mat getWorkingImage() {
        return workingImage;
    }

    public void process() throws Exception {
        release();
        for (CvStage stage : stages) {
            // Process and time the stage and get the result.
            long t = System.nanoTime();
            Result result = stage.process(this);
            t = System.nanoTime() - t;
            
            // If no result is returned we at least want to store the processing time, so we
            // create an empty one.
            if (result == null) {
                result = new Result(null, null, t);
            }
            
            // Store the result for later.
            results.put(stage, result);

            // If the result did not contain an image we're done.
            if (result.image == null) {
                continue;
            }
            // Update the result with a clone of the returned image so that it is not modified
            // later.
            results.put(stage, new Result(result.image.clone(), result.model, t));
            
            // If the result image is different from the workingImage, release the working
            // image and then replace it with the result.
            if (result.image != workingImage) {
                if (workingImage != null) {
                    workingImage.release();
                }
                workingImage = result.image;
            }
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
        }
        for (Result result : results.values()) {
            if (result.image != null) {
                result.image.release();
            }
        }
        results.clear();
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
            String name = "" + i;
            if (getStage(name) == null) {
                return name;
            }
        }
    }
}
