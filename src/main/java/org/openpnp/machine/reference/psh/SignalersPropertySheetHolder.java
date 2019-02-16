package org.openpnp.machine.reference.psh;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.ClassSelectionDialog;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Machine;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.Signaler;
import org.openpnp.spi.base.SimplePropertySheetHolder;

public class SignalersPropertySheetHolder extends SimplePropertySheetHolder {
    final Machine machine;
    
    public SignalersPropertySheetHolder(Machine machine, String title, List<? extends PropertySheetHolder> children,
            Icon icon) {
        super(title, children, icon);
        this.machine = machine;
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return new Action[] {newAction};
    }
    
    public Action newAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.add);
            putValue(NAME, "New Signaler...");
            putValue(SHORT_DESCRIPTION, "Create a new signaler.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            Configuration configuration = Configuration.get();
            ClassSelectionDialog<Signaler> dialog = new ClassSelectionDialog<>(MainFrame.get(),
                    "Select Nozzle...", "Please select a Signaler implemention from the list below.",
                    configuration.getMachine().getCompatibleSignalerClasses());
            dialog.setVisible(true);
            Class<? extends Signaler> cls = dialog.getSelectedClass();
            if (cls == null) {
                return;
            }
            try {
                Signaler signaler = cls.newInstance();

                machine.addSignaler(signaler);
            }
            catch (Exception e) {
                MessageBoxes.errorBox(MainFrame.get(), "Error", e);
            }
        }
    };
}
