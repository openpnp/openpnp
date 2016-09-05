package org.openpnp.spi.base;

import org.openpnp.model.*;
import org.openpnp.spi.FeederSlot;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.SlottedFeeder;
import org.simpleframework.xml.Attribute;

/**
 * Created by matt on 05/09/2016.
 */
public abstract class AbstractFeederSlot extends AbstractModelObject implements FeederSlot  {
    @Attribute(required=false)
    SlottedFeeder feeder;

    @Attribute(required=false)
    Location pickLocation;

    public AbstractFeederSlot()
    {
        this.id = Configuration.createId();
        this.name = getClass().getSimpleName();
    }
    public Location getPickLocation()
    {
        return pickLocation;
    }
    public Feeder getFeeder()
    {
        return feeder;
    }
    public void setFeeder(SlottedFeeder feeder)
    {
        this.feeder = feeder;
    }

    @Attribute
    protected String id;

    @Attribute(required = false)
    protected String name;

    @Attribute(required = false)
    protected Boolean enabled;
    public void setEnabled(Boolean bFlag) { this.enabled=bFlag; }
    public Boolean getEnabled() { return this.enabled; }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }
}
