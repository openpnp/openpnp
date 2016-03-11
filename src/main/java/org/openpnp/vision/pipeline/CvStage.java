package org.openpnp.vision.pipeline;

import org.opencv.core.Mat;
import org.simpleframework.xml.Attribute;

/**
 * Base class for a stage in a CvPipeline. A CvStage has a unique name within a pipeline and is able
 * to perform computer vision operations reuslting in either a modified working image or a new image
 * and optional model data extracted from the image.
 */
public abstract class CvStage {
    @Attribute
    private String name;

    /**
     * Perform an operation in a pipeline. Typical implementations will call
     * CvPipeline#getWorkingImage(), perform some type of operation on the image and will return a
     * Result containing a modified image and model data about features found in the image.
     * 
     * @param pipeline
     * @return A Result object containing a modified or new image along with optional model data.
     *         This method may also return null which indicates that no model data or image is to be
     *         stored for later retrieval. In either case the call may modify the working image. Any
     *         image that gets returned as a result is cloned before storage so that further
     *         modifications of it will not change the stored result.
     * @throws Exception
     */
    public abstract Result process(CvPipeline pipeline) throws Exception;

    public String getName() {
        return name;
    }

    public CvStage setName(String name) {
        this.name = name;
        return this;
    }

    public static class Result {
        final public Mat image;
        final public Object model;
        final public long processingTimeNs;

        public Result(Mat image, Object model, long processingTimeNs) {
            this.image = image;
            this.model = model;
            this.processingTimeNs = processingTimeNs;
        }
        
        public Result(Mat image, Object model) {
            this(image, model, 0);
        }

        public Result(Mat image) {
            this(image, null, 0);
        }
    }
}
