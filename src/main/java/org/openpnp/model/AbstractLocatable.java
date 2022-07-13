package org.openpnp.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.beanutils.BeanUtils;
import org.openpnp.spi.Definable;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

public abstract class AbstractLocatable extends AbstractModelObject implements Definable, Identifiable, PropertyChangeListener {

    @Element
    protected Location location;

    @Attribute
    protected String id;
    
    protected transient Definable definedBy;

    private transient boolean dirty;
    
    AbstractLocatable() {
        definedBy = this;
    }
    
    AbstractLocatable(AbstractLocatable abstractLocatable) {
        super();
        location = abstractLocatable.location;
        setDefinedBy(abstractLocatable.getDefinedBy());
        id = abstractLocatable.id;
    }
    
    AbstractLocatable(Location location) {
        this();
        this.location = location;
    }
    
    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        Location oldValue = this.location;
        this.location = location;
        firePropertyChange("location", oldValue, location);
    }

    public Definable getDefinedBy() {
        return definedBy;
    }
    
    public void setDefinedBy(Definable definedBy) {
        Definable oldValue = this.definedBy;
        this.definedBy = definedBy;
        firePropertyChange("definedBy", oldValue, definedBy);
        if (oldValue != null) {
            ((AbstractLocatable) oldValue).removePropertyChangeListener(this);
        }
        if (definedBy != null) {
            ((AbstractLocatable) definedBy).addPropertyChangeListener(this);
        }
    }
    
    public boolean isDefinedBy(Definable definedBy) {
        return this.definedBy == definedBy;
    };

    public String getId() {
        return id;
    }

    public void setId(String id) {
        String oldValue = this.id;
        this.id = id;
        firePropertyChange("id", oldValue, id);
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        boolean oldValue = this.dirty;
        this.dirty = dirty;
        firePropertyChange("dirty", oldValue, dirty);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
//        Logger.trace(String.format("PropertyChangeEvent handled by AbstractLocatable @%08x = %s", this.hashCode(), evt));
        if (evt.getSource() != AbstractLocatable.this || evt.getPropertyName() != "dirty") {
            dirty = true;
            if (evt.getSource() == definedBy) {
                try {
                    Logger.trace("Attempting to set property: " + evt.getPropertyName() + " = " + evt.getNewValue());
                    BeanUtils.setProperty(this, evt.getPropertyName(), evt.getNewValue());
                }
                catch (IllegalAccessException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                catch (InvocationTargetException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        
    }
}
