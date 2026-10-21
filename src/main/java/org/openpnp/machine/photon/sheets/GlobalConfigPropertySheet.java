package org.openpnp.machine.photon.sheets;

import org.openpnp.Translations;
import org.openpnp.machine.photon.sheets.gui.GlobalConfigConfigurationWizard;
import org.openpnp.spi.PropertySheetHolder;

import javax.swing.*;

public class GlobalConfigPropertySheet implements PropertySheetHolder.PropertySheet {
    @Override
    public String getPropertySheetTitle() {
        return Translations.getString("PhotonFeeder.PropertySheet.GlobalConfig.title"); //$NON-NLS-1$
    }

    @Override
    public JPanel getPropertySheetPanel() {
    	return new GlobalConfigConfigurationWizard();
    }
}
