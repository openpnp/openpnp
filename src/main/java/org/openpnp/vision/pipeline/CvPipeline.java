package org.openpnp.vision.pipeline;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;
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
    
    private final Object context;
    
    @Element
    private List<CvStage> stages = new ArrayList<>();
    
    private Mat mat;
    
    public CvPipeline(Object context) {
        this.context = context;
    }
    
    public Object getContext() {
        return context;
    }
    
    public Object getContextProperty(String name) {
        return null;
    }
    
    public Mat getWorkingImage() {
        return mat;
    }
    
    public void setWorkingImage(Mat mat) {
        this.mat = mat;
    }
    
    public void process() throws Exception {
        for (CvStage stage : stages) {
            stage.process(this);
        }
    }
}
