package org.openpnp.spi.base;

import javax.swing.Icon;

import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.Configuration;
import org.openpnp.spi.NozzleTip;
import org.simpleframework.xml.Attribute;

public abstract class AbstractNozzleTip extends AbstractModelObject implements NozzleTip {
    @Attribute
    protected String id;

    @Attribute(required = false)
    protected String name;

    public AbstractNozzleTip() {
        this.id = Configuration.createId("TIP");
        this.name = getClass().getSimpleName();
    }

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
        Object oldValue = this.name;
        this.name = name;
        firePropertyChange("name", oldValue, name);
    }

    @Override
    public Icon getPropertySheetHolderIcon() {
        return null;
    }
}
