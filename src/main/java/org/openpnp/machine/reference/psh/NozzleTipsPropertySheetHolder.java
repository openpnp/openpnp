package org.openpnp.machine.reference.psh;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.machine.reference.ReferenceNozzleTip;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.base.SimplePropertySheetHolder;

public class NozzleTipsPropertySheetHolder extends SimplePropertySheetHolder {
    final Nozzle nozzle;
    
    public NozzleTipsPropertySheetHolder(Nozzle nozzle, String title, List<? extends PropertySheetHolder> children,
            Icon icon) {
        super(title, children, icon);
        this.nozzle = nozzle;
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return new Action[] {newAction};
    }
    
    public Action newAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.nozzleTipAdd);
            putValue(NAME, "New Nozzle Tip...");
            putValue(SHORT_DESCRIPTION, "Create a new nozzle tip.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            Configuration configuration = Configuration.get();
//            ClassSelectionDialog<Nozzle> dialog = new ClassSelectionDialog<>(MainFrame.get(),
//                    "Select Nozzle...", "Please select a Nozzle implemention from the list below.",
//                    configuration.getMachine().getCompatibleNozzleClasses());
//            dialog.setVisible(true);
//            Class<? extends Nozzle> cls = dialog.getSelectedClass();
            Class<? extends NozzleTip> cls = ReferenceNozzleTip.class;
            if (cls == null) {
                return;
            }
            try {
                NozzleTip nozzleTip = cls.newInstance();

                nozzle.addNozzleTip(nozzleTip);
            }
            catch (Exception e) {
                MessageBoxes.errorBox(MainFrame.get(), "Error", e);
            }
        }
    };
}
