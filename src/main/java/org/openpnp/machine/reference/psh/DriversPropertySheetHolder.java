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
import org.openpnp.spi.Driver;
import org.openpnp.spi.Machine;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.base.SimplePropertySheetHolder;

public class DriversPropertySheetHolder extends SimplePropertySheetHolder {
    private Machine machine;
    
    public DriversPropertySheetHolder(Machine machine, String title, List<? extends PropertySheetHolder> children,
            Icon icon) {
        super(title, children, icon);
        this.machine = machine;
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return new Action[] {newDriverAction};
    }
    
    public Action newDriverAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.add);
            putValue(NAME, "New Driver...");
            putValue(SHORT_DESCRIPTION, "Create a new driver.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            Configuration configuration = Configuration.get();
            ClassSelectionDialog<Driver> dialog = new ClassSelectionDialog<>(MainFrame.get(),
                    "Select Driver...", "Please select a Driver implemention from the list below.",
                    configuration.getMachine().getCompatibleDriverClasses());
            dialog.setVisible(true);
            Class<? extends Driver> driverClass = dialog.getSelectedClass();
            if (driverClass == null) {
                return;
            }
            try {
                Driver driver = driverClass.newInstance();

                machine.addDriver(driver);
            }
            catch (Exception e) {
                MessageBoxes.errorBox(MainFrame.get(), "Driver Error", e);
            }
        }
    };
}
