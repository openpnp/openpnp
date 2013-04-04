package org.openpnp.spi.base;

import org.openpnp.model.Part;
import org.openpnp.spi.Feeder;
import org.simpleframework.xml.Attribute;

public abstract class AbstractFeeder implements Feeder {
    @Attribute
    protected String id;
    @Attribute
    protected boolean enabled;
    @Attribute
    protected String partId;
    
    protected Part part;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public Part getPart() {
        return part;
    }
}
