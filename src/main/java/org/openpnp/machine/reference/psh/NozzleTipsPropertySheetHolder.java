package org.openpnp.machine.reference.psh;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;

import org.openpnp.Translations;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.machine.reference.ReferenceNozzleTip;
import org.openpnp.model.Configuration;
import org.openpnp.spi.NozzleTip;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.base.SimplePropertySheetHolder;

public class NozzleTipsPropertySheetHolder extends SimplePropertySheetHolder {
    public NozzleTipsPropertySheetHolder(String title, List<? extends PropertySheetHolder> children,
            Icon icon) {
        super(title, children, icon);
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return new Action[] {newAction};
    }
    
    public Action newAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.nozzleTipAdd);
            putValue(NAME, Translations.getString("NozzleTipsPropertySheetHolder.Action.NewNozzleTip")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString(
                    "NozzleTipsPropertySheetHolder.Action.NewNozzleTip.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            Configuration configuration = Configuration.get();
            Class<? extends NozzleTip> cls = ReferenceNozzleTip.class;
            try {
                NozzleTip nozzleTip = cls.newInstance();

                configuration.getMachine().addNozzleTip(nozzleTip);
            }
            catch (Exception e) {
                MessageBoxes.errorBox(MainFrame.get(), "Error", e);
            }
        }
    };
}
