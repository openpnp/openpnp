package org.openpnp.spi.base;

import org.openpnp.gui.support.Wizard;
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
    public Wizard getConfigurationWizard() {
        // TODO Auto-generated method stub
        return null;
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
