package org.openpnp.spi;

import org.openpnp.model.Identifiable;
import org.openpnp.model.Named;
import org.openpnp.vision.pipeline.CvPipeline;

public interface Pipeline extends Identifiable, Named, PropertySheetHolder {

    /**
     * Get the attached CvPipeline
     * @return
     */
    public CvPipeline getCvPipeline();

    /**
     * Get the default pipeline for this class
     * @return
     */
    public CvPipeline createDefaultCvPipeline();

    /**
     * Reset the pipeline to its default
     */
    public void resetCvPipeline();

    /**
     * Return the associated camera with the pipeline
     * @return
     */
    public Camera getCamera() throws Exception;
}
