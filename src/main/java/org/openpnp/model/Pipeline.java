package org.openpnp.model;

import org.openpnp.vision.pipeline.CvPipeline;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

public class Pipeline extends AbstractModelObject implements Identifiable {
    @Attribute()
    private String id;

    @Attribute(required = false)
    private String name;

    @Element()
    private CvPipeline cvPipeline;

    public Pipeline(){}

    public Pipeline(String id) {
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

    //TODO: NK constructor for creation of a new pipeline
}
