package org.openpnp.spi.base;

import org.openpnp.spi.NozzleTip;
import org.simpleframework.xml.Attribute;

public abstract class AbstractNozzleTip implements NozzleTip {
    @Attribute
    protected String id;
    
    @Override
    public String getId() {
        return id;
    }
}
