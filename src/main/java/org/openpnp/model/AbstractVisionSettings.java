package org.openpnp.model;

import org.openpnp.spi.VisionSettings;
import org.openpnp.vision.pipeline.CvPipeline;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

public abstract class AbstractVisionSettings extends AbstractModelObject implements VisionSettings {
    @Attribute()
    private String id;

    @Attribute(required = false)
    private String name;

    @Attribute
    protected boolean enabled;

    @Element()
    private CvPipeline cvPipeline;

    protected AbstractVisionSettings() {
    }

    protected AbstractVisionSettings(String id) {
        this.id = id;
        this.cvPipeline = new CvPipeline();
    }

    @Override
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CvPipeline getCvPipeline() {
        if (cvPipeline == null) {
            cvPipeline = new CvPipeline();
        }

        return cvPipeline;
    }

    public void setCvPipeline(CvPipeline cvPipeline) {
        this.cvPipeline = cvPipeline;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String toString() {
        return String.format("id %s", id);
    }
}
