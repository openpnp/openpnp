package org.openpnp.spi.base;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JOptionPane;

import org.openpnp.Translations;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.Configuration;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.Signaler;
import org.openpnp.spi.WizardConfigurable;
import org.simpleframework.xml.Attribute;

public abstract class AbstractSignaler extends AbstractModelObject implements Signaler, WizardConfigurable {

    @Attribute
    protected String id;

    @Attribute(required = false)
    protected String name;
    
    public AbstractSignaler() {
        this.id = Configuration.createId("SIG");
        this.name = getClass().getSimpleName();
    }

    @Override
    public void signalMachineState(AbstractMachine.State state) {}

    @Override
    public void signalJobProcessorState(AbstractJobProcessor.State state) {}

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName() + " " + getName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        return null;
    }

    @Override
    public Icon getPropertySheetHolderIcon() {
        return null;
    }

    @Override
    public Wizard getConfigurationWizard() {
        return null;
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {new PropertySheetWizardAdapter(getConfigurationWizard())};
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return new Action[] { deleteAction };
    }
    
    public Action deleteAction = new AbstractAction("Delete Signaler") {
        {
            putValue(SMALL_ICON, Icons.delete);
            putValue(NAME, "Delete Signaler");
            putValue(SHORT_DESCRIPTION, Translations.getString("AbstractSignaler.Action.Delete.Description"));
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            int ret = JOptionPane.showConfirmDialog(MainFrame.get(),
                    Translations.getString("DialogMessages.ConfirmDelete.text") + " " + getName() + "?",
                    Translations.getString("DialogMessages.ConfirmDelete.title") + " " + getName() + "?",
                    JOptionPane.YES_NO_OPTION);
            if (ret == JOptionPane.YES_OPTION) {
                Configuration.get().getMachine().removeSignaler(AbstractSignaler.this);
            }
        }
    };
}
