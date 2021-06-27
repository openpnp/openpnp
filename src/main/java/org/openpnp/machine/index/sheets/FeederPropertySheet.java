package org.openpnp.machine.index.sheets;

import org.openpnp.machine.index.IndexFeeder;
import org.openpnp.machine.index.sheets.gui.FeederConfigurationWizard;
import org.openpnp.spi.PropertySheetHolder;

import javax.swing.*;

public class FeederPropertySheet implements PropertySheetHolder.PropertySheet {
    private final IndexFeeder feeder;

    public FeederPropertySheet(IndexFeeder feeder) {

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
