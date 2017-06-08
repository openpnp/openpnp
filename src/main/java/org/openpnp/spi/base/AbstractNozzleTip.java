package org.openpnp.spi.base;

import javax.swing.Icon;

import org.openpnp.gui.MainFrame;
import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.spi.NozzleTip;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

public abstract class AbstractNozzleTip extends AbstractModelObject implements NozzleTip {
    @Element(required = false)
    protected Length tipDiameter;

    @Attribute
    protected String id;

    @Attribute(required = false)
    protected String name;

    public AbstractNozzleTip() {
        this.id = Configuration.createId("TIP");
        this.name = getClass().getSimpleName();
        this.tipDiameter = new Length(1.0, Configuration.get().getSystemUnits());
    }

    public Length getTipDiameter() {
        return tipDiameter;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setTipDiameter(Length tipDiameter) {
        Object oldValue = this.tipDiameter;
        this.tipDiameter = tipDiameter;
        firePropertyChange("name", oldValue, this.tipDiameter);
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
