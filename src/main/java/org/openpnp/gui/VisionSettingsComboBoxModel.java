package org.openpnp.gui;

import org.openpnp.gui.support.NamedComboBoxModel;
import org.openpnp.model.AbstractVisionSettings;
import org.openpnp.model.BottomVisionSettings;
import org.openpnp.model.Configuration;

@SuppressWarnings("serial")
public class VisionSettingsComboBoxModel extends NamedComboBoxModel<AbstractVisionSettings> {

    public VisionSettingsComboBoxModel() {
        super("vision-settings");
    }

    @Override
    protected void addAllElements() {
        Configuration.get().getVisionSettings().stream()
                .filter(BottomVisionSettings.class::isInstance)
                .sorted(comparator)
                .forEach(this::addElement);
        addElement(null);
    }
}
