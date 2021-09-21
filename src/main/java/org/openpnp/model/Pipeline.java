package org.openpnp.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;

import java.util.ArrayList;

public class Pipeline extends AbstractModelObject implements Identifiable {
    @Attribute(required = false)
    private String id;

    @Attribute(required = false)
    private String name;

    @ElementList(inline = true, entry = "stage", required = false)
    private ArrayList<Stage> stages = new ArrayList<>();

    @Override
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    //TODO: contructor for creation of a new pipeline

    public static class Stage {

        @Attribute(required = false)
        private String name;

        @Attribute(required = false)
        private boolean enabled;

        @Attribute(required = false)
        private boolean settleFirst;

        @Attribute(required = false)
        private int count;

        @Attribute(required = false)
        private int kernelSize;

        @Attribute(required = false)
        private String conversion;

    }
}
