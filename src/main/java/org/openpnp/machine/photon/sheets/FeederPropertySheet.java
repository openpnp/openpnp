package org.openpnp.machine.photon.sheets;

import org.openpnp.machine.photon.PhotonFeeder;
import org.openpnp.machine.photon.sheets.gui.FeederConfigurationWizard;
import org.openpnp.spi.PropertySheetHolder;

import javax.swing.*;

public class FeederPropertySheet implements PropertySheetHolder.PropertySheet {
    private final PhotonFeeder feeder;

    public FeederPropertySheet(PhotonFeeder feeder) {
        this.feeder = feeder;
    }

    @Override
    public String getPropertySheetTitle() {
        return "Feeder";
    }

    @Override
    public JPanel getPropertySheetPanel() {
        return new FeederConfigurationWizard(feeder);
    }
}
