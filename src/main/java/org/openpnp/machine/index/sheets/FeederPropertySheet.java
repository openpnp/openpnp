package org.openpnp.machine.index.sheets;

import org.openpnp.spi.PropertySheetHolder;

import javax.swing.*;

public class FeederPropertySheet implements PropertySheetHolder.PropertySheet {
    @Override
    public String getPropertySheetTitle() {
        return "Feeder";
    }

    @Override
    public JPanel getPropertySheetPanel() {
        return new JPanel();
    }
}
