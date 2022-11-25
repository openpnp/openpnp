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
import org.openpnp.spi.Axis;
import org.openpnp.spi.Machine;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.base.SimplePropertySheetHolder;

public class AxesPropertySheetHolder extends SimplePropertySheetHolder {
    private Machine machine;
    
    public AxesPropertySheetHolder(Machine machine, String title, List<? extends PropertySheetHolder> children,
            Icon icon) {
        super(title, children, icon);
        this.machine = machine;
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return new Action[] {newAxisAction};
    }
    
    public Action newAxisAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.add);
            putValue(NAME, "New Axis...");
            putValue(SHORT_DESCRIPTION, Translations.getString("AxisPropertySheetHolder.Action.NewAxis.Description"));
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            Configuration configuration = Configuration.get();
            ClassSelectionDialog<Axis> dialog = new ClassSelectionDialog<>(MainFrame.get(),
                    Translations.getString("AxisPropertySheetHolder.SelectionDialog.title"),
                    Translations.getString("AxisPropertySheetHolder.SelectionDialog.description"),
                    configuration.getMachine().getCompatibleAxisClasses());
            dialog.setVisible(true);
            Class<? extends Axis> axisClass = dialog.getSelectedClass();
            if (axisClass == null) {
                return;
            }
            try {
                Axis axis = axisClass.newInstance();

                if (axis.getType() == null) {
                    axis.setType(Axis.Type.X);
                }
                
                machine.addAxis(axis);
            }
            catch (Exception e) {
                MessageBoxes.errorBox(MainFrame.get(), "Axis Error", e);
            }
        }
    };
}
