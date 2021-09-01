package org.openpnp.gui.support;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.DefaultComboBoxModel;

import org.openpnp.spi.base.AbstractActuator;

public class ActuatorProfilesComboBoxModel extends DefaultComboBoxModel implements PropertyChangeListener {
    private AbstractActuator actuator;

    public ActuatorProfilesComboBoxModel(AbstractActuator actuator) {
        this.actuator = actuator;
        addAllElements();
        actuator.addPropertyChangeListener("profileValues", this);
    }

    private void addAllElements() {
        for (String profile : actuator.getProfileValues()) {
            addElement(profile);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        removeAllElements();
        addAllElements();
    }
}
