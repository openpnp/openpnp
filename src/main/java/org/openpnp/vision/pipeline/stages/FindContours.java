package org.openpnp.vision.pipeline.stages;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.imgproc.Imgproc;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.simpleframework.xml.Attribute;

public class FindContours extends CvStage {
    public enum RetrievalMode {
        ConnectedComponent(Imgproc.RETR_CCOMP),
        External(Imgproc.RETR_EXTERNAL),
        FloodFill(Imgproc.RETR_FLOODFILL),
        List(Imgproc.RETR_LIST),
        Tree(Imgproc.RETR_TREE);
        
        private int code;

        RetrievalMode(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }
    
    public enum ApproximationMethod {
        None(Imgproc.CHAIN_APPROX_NONE),
        Simple(Imgproc.CHAIN_APPROX_SIMPLE),
        Tc89Kcos(Imgproc.CHAIN_APPROX_TC89_KCOS),
        Tc89L1(Imgproc.CHAIN_APPROX_TC89_L1);
        
        private int code;

        ApproximationMethod(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }
    
    @Attribute
    private RetrievalMode retrievalMode = RetrievalMode.List;
    
    @Attribute
    private ApproximationMethod approximationMethod = ApproximationMethod.None;
    
    public RetrievalMode getRetrievalMode() {
        return retrievalMode;
    }

    public void setRetrievalMode(RetrievalMode retrievalMode) {
        this.retrievalMode = retrievalMode;
    }

    public ApproximationMethod getApproximationMethod() {
        return approximationMethod;
    }

    public void setApproximationMethod(ApproximationMethod approximationMethod) {
        this.approximationMethod = approximationMethod;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        Mat mat = pipeline.getWorkingImage();
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(mat, contours, hierarchy, retrievalMode.getCode(), approximationMethod.getCode());
        hierarchy.release();
        return new Result(mat, contours);
    }
}
