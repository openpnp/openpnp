package org.openpnp.gui;

import org.openpnp.gui.support.NamedComboBoxModel;
import org.openpnp.model.AbstractVisionSettings;
import org.openpnp.model.Configuration;

@SuppressWarnings("serial")
public class VisionSettingsComboBoxModel<T extends AbstractVisionSettings> extends NamedComboBoxModel<AbstractVisionSettings> {
    public VisionSettingsComboBoxModel(Class<T> visionClass) {
        super("visionSettings", (Class<AbstractVisionSettings>) visionClass);
    }

    @Override
    protected void addAllElements() {
        Configuration.get().getVisionSettings().stream()
        .filter(namedClass::isInstance)
        .sorted(comparator)
        .forEach(this::addElement);
        addElement(null);
    }
}
