package org.openpnp.machine.photon.sheets;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.machine.photon.PhotonFeeder;
import org.openpnp.machine.photon.sheets.gui.GlobalConfigConfigurationWizard;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.util.UiUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.function.IntConsumer;

public class SearchPropertySheet implements PropertySheetHolder.PropertySheet {
    @Override
    public String getPropertySheetTitle() {
        return "Global Config";
    }

    @Override
    public JPanel getPropertySheetPanel() {
    	return new GlobalConfigConfigurationWizard();
    }
}
