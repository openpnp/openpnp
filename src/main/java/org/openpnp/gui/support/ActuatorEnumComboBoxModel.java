package org.openpnp.gui.support;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.DefaultComboBoxModel;

import org.openpnp.spi.base.AbstractActuator;

public class ActuatorEnumComboBoxModel extends DefaultComboBoxModel implements PropertyChangeListener {
    private AbstractActuator actuator;

    public ActuatorEnumComboBoxModel(AbstractActuator actuator) {
        this.actuator = actuator;
        addAllElements();
        actuator.addPropertyChangeListener("profileValues", this);
    }

    private void addAllElements() {
        Object values[] = actuator.getValues();
        if (values != null) {
            for (Object value : values) {
                addElement(value);
            }
        } else {
            Object[] enumConstants = actuator.getValueClass().getEnumConstants();
            if (enumConstants != null) {
                for (Object value : enumConstants) {
                    addElement(value);
                }
            }
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        removeAllElements();
        addAllElements();
    }
}
