package org.openpnp.gui;

import org.openpnp.gui.support.TableComboBoxModel;
import org.openpnp.model.BottomVisionSettings;
import org.openpnp.model.Configuration;
import org.openpnp.model.AbstractVisionSettings;

@SuppressWarnings("serial")
public class VisionSettingsComboBoxModel extends TableComboBoxModel<AbstractVisionSettings> {

    public VisionSettingsComboBoxModel() {
        super("vision-settings");
    }

    @Override
    protected void addAllElements() {
        Configuration.get().getVisionSettingsList().stream()
                .filter(BottomVisionSettings.class::isInstance)
                .sorted(comparator)
                .forEach(this::addElement);
    }
}
