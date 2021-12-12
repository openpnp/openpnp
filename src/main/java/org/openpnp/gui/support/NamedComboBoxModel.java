package org.openpnp.gui.support;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.DefaultComboBoxModel;

import org.openpnp.model.Configuration;
import org.openpnp.model.Named;

@SuppressWarnings("serial")
public class NamedComboBoxModel<T extends Named> extends DefaultComboBoxModel<T> implements PropertyChangeListener {
    protected final NamedComparator<T> comparator = new NamedComparator<>();

    public NamedComboBoxModel(String property) {
        addAllElements();
        Configuration.get().addPropertyChangeListener(property, this);
    }

    protected void addAllElements() {
        throw new UnsupportedOperationException("Calling this function from general NamedComboBoxModel is not supported");
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        removeAllElements();
        addAllElements();
    }
}
