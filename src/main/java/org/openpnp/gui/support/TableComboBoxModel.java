package org.openpnp.gui.support;

import org.openpnp.model.Configuration;
import org.openpnp.model.Identifiable;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

@SuppressWarnings("serial")
public class TableComboBoxModel<T extends Identifiable> extends DefaultComboBoxModel<T> implements PropertyChangeListener {
    protected final IdentifiableComparator<T> comparator = new IdentifiableComparator<>();

    public TableComboBoxModel(String property) {
        addAllElements();
        Configuration.get().addPropertyChangeListener(property, this);
    }

    protected void addAllElements() {
        throw new UnsupportedOperationException("Calling this function from GeneralTableComboBoxModel is not supported");
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        removeAllElements();
        addAllElements();
    }
}
