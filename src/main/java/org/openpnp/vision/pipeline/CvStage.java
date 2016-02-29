package org.openpnp.vision.pipeline;

public interface CvStage {
    public void process(CvPipeline pipeline) throws Exception;
}
