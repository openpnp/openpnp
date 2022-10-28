package org.openpnp.machine.reference.psh;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;

import org.openpnp.Translations;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.ClassSelectionDialog;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Head;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.TransformedAxis;
import org.openpnp.spi.base.SimplePropertySheetHolder;

public class ActuatorsPropertySheetHolder extends SimplePropertySheetHolder {
    final Head head;
    
    public ActuatorsPropertySheetHolder(Head head, String title, List<? extends PropertySheetHolder> children,
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
            putValue(SMALL_ICON, Icons.add);
            putValue(NAME, "New Actuator...");
            putValue(SHORT_DESCRIPTION, Translations.getStringOrDefault(
                    "ActuatorsPropertySheetHolder.Action.NewActuator", "Create a new actuator."));
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            Configuration configuration = Configuration.get();
            ClassSelectionDialog<Actuator> dialog = new ClassSelectionDialog<>(MainFrame.get(),
                    Translations.getStringOrDefault(
                            "ActuatorsPropertySheetHolder.ActuatorSelectionDialog.title", "Select Actuator..."),
                    Translations.getStringOrDefault(
                            "ActuatorsPropertySheetHolder.ActuatorSelectionDialog.description",
                            "Please select a Actuator implementation from the list below."),
                    configuration.getMachine().getCompatibleActuatorClasses());
            dialog.setVisible(true);
            Class<? extends Actuator> cls = dialog.getSelectedClass();
            if (cls == null) {
                return;
            }
            try {
                Actuator actuator = cls.newInstance();

                if (head != null) {
                    head.addActuator(actuator);
                }
                else {
                    Configuration.get().getMachine().addActuator(actuator);
                }
            }
            catch (Exception e) {
                MessageBoxes.errorBox(MainFrame.get(), "Camera Error", e);
            }
        }
    };
}
