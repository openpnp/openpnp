package org.openpnp.vision.pipeline.stages;

import java.io.File;

import org.opencv.highgui.Highgui;
import org.openpnp.model.Configuration;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.simpleframework.xml.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Save the working image as an image file in the debug directory using the specified prefix and
 * suffix. The suffix should be a file extension (including the period).
 */
public class ImageWriteDebug extends CvStage {
    private final static Logger logger = LoggerFactory.getLogger(ImageWriteDebug.class);

    @Attribute
    private String prefix = "debug";

    @Attribute
    private String suffix = ".png";

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        if (!logger.isDebugEnabled()) {
            return null;
        }
        File file = Configuration.get().createResourceFile(getClass(), prefix, suffix);
        Highgui.imwrite(file.getAbsolutePath(), pipeline.getWorkingImage());
        return null;
    }
}
