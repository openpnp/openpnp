package org.openpnp.model;

import org.openpnp.spi.PartAlignment;
import org.openpnp.spi.VisionSettings;
import org.openpnp.vision.pipeline.CvPipeline;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

public abstract class AbstractVisionSettings extends AbstractModelObject implements VisionSettings {
    @Attribute()
    private String id;

    @Attribute(required = false)
    private String name;

    @Element()
    private CvPipeline cvPipeline;

    public AbstractVisionSettings(){}

    public AbstractVisionSettings(String id) {
        this.id = id;
        this.cvPipeline = new CvPipeline();
    }

    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public CvPipeline getCvPipeline() {
        if(cvPipeline == null) {
            cvPipeline = new CvPipeline();
        }

        return cvPipeline;
    }

    public void setCvPipeline(CvPipeline cvPipeline) {
        this.cvPipeline = cvPipeline;
    }

    public String toString() {
        return String.format("id %s", id);
    }
}
