package org.openpnp.gui.support;

import org.openpnp.model.AbstractModelObject;

/**
 * A simple value wrapper that provides PropertyChangeListener support. Can be used in place of
 * basic fields.
 */
public class BoundProperty<T> extends AbstractModelObject {
    T value;

    public BoundProperty(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        T oldValue = this.value;
        this.value = value;
        firePropertyChange("value", oldValue, value);
    }
}
