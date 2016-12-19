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
import org.openpnp.spi.Head;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.base.SimplePropertySheetHolder;

public class NozzlesPropertySheetHolder extends SimplePropertySheetHolder {
    final Head head;
    
    public NozzlesPropertySheetHolder(Head head, String title, List<? extends PropertySheetHolder> children,
            Icon icon) {
        super(title, children, icon);
        this.head = head;
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return new Action[] {newAction};
    }
    
    public Action newAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.nozzleAdd);
            putValue(NAME, "New Nozzle...");
            putValue(SHORT_DESCRIPTION, "Create a new nozzle.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            Configuration configuration = Configuration.get();
            ClassSelectionDialog<Nozzle> dialog = new ClassSelectionDialog<>(MainFrame.get(),
                    "Select Nozzle...", "Please select a Nozzle implemention from the list below.",
                    configuration.getMachine().getCompatibleNozzleClasses());
            dialog.setVisible(true);
            Class<? extends Nozzle> cls = dialog.getSelectedClass();
            if (cls == null) {
                return;
            }
            try {
                Nozzle nozzle = cls.newInstance();

                head.addNozzle(nozzle);
            }
            catch (Exception e) {
                MessageBoxes.errorBox(MainFrame.get(), "Error", e);
            }
        }
    };
}
