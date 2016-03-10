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
import org.openpnp.vision.FluentCv;
import org.openpnp.vision.pipeline.CvStage.Result;
import org.openpnp.vision.pipeline.stages.ConvertColor;
import org.openpnp.vision.pipeline.stages.LoadImage;
import org.openpnp.vision.pipeline.stages.SaveImage;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Root
public class CvPipeline {
    static {
        nu.pattern.OpenCV.loadShared();
        System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
    }

    private final static Logger logger = LoggerFactory
            .getLogger(CvPipeline.class);
    
    @ElementList
    private ArrayList<CvStage> stages = new ArrayList<>();
    
    private Map<CvStage, Result> results = new HashMap<CvStage, Result>();
    
    private Mat workingImage;
    
    public void add(String name, CvStage stage) {
        if (name == null) {
            name = generateUniqueName();
        }
        stage.setName(name);
        stages.add(stage);
    }
    
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
    
    public Result getResult(String name) {
        return getResult(getStage(name));
    }
    
    public Result getResult(CvStage stage) {
        return results.get(stage);
    }
    
    public Mat getWorkingImage() {
        return workingImage;
    }
    
    public void process() throws Exception {
        release();
        for (CvStage stage : stages) {
            // Process the stage and get the result.
            Result result = stage.process(this);
            // If no result is returned we're done.
            if (result == null) {
                continue;
            }
            // Store the result for later.
            results.put(stage, result);

            // If the result did not contain an image we're done.
            if (result.image == null) {
                continue;
            }
            // If the result is the workingImage we don't change the
            // workingImage but we clone it into the result so that it doesn't
            // get changed later.
            if (result.image == workingImage) {
                results.put(stage, new Result(result.image.clone(), result.model));
            }
            // Otherwise we set the workingImage to the returned image and
            // clone it into the result.
            else {
                if (workingImage != null) {
                    workingImage.release();
                }
                workingImage = result.image;
                results.put(stage, new Result(result.image.clone(), result.model));
            }
        }
    }
    
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
    
    public String toXmlString() throws Exception {
        Serializer ser = Configuration.createSerializer();
        StringWriter sw = new StringWriter();
        ser.write(this, sw);
        return sw.toString();
    }
    
    public void fromXmlString(String s) throws Exception {
        Serializer ser = Configuration.createSerializer();
        StringReader sr = new StringReader(s);
        CvPipeline pipeline = ser.read(CvPipeline.class, sr);
        stages.clear();
        for (CvStage stage : pipeline.getStages()) {
            add(stage);
        }
    }
    
    private String generateUniqueName() {
        for (int i = 0; ; i++) {
            String name = "" + i;
            if (getStage(name) == null) {
                return name;
            }
        }
    }
    
    public static void main(String[] args) throws Exception {
        CvPipeline pipeline = new CvPipeline();
//        pipeline.add(new LoadImage().setPath("/Users/jason/Desktop/t.png"));
//        pipeline.add(new ConvertColor().setConversion(FluentCv.ColorCode.Bgr2Gray));
//        pipeline.add(new SaveImage().setPath("/Users/jason/Desktop/t_gray.png"));
        pipeline.process();
        
        System.out.println(pipeline.toXmlString());
        
        String s = pipeline.toXmlString();
        
        pipeline = new CvPipeline();
        
        pipeline.fromXmlString(s);
        
        System.out.println(pipeline.toXmlString());
    }
}
