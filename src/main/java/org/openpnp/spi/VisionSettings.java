package org.openpnp.spi;

import org.openpnp.model.Identifiable;
import org.openpnp.vision.pipeline.CvPipeline;

public interface VisionSettings extends WizardConfigurable, Identifiable {
    public String getName();

    public CvPipeline getCvPipeline();
}
