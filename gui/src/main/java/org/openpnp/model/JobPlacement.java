package org.openpnp.model;

import org.simpleframework.xml.Attribute;

/**
 * Stores state specific to a Job, Board, and Placement. This is used to
 * maintain state during the running of a Job.
 */
public class JobPlacement extends AbstractModelObject {
    private String placementId;
    
    @Attribute
    private boolean complete;

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }
}
