package org.openpnp.spi.base;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JOptionPane;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Driver;
import org.openpnp.spi.PropertySheetHolder;
import org.simpleframework.xml.Attribute;

public abstract class AbstractDriver extends AbstractModelObject implements Driver {

    @Attribute(required = false) 
    protected String id;

    @Attribute(required = false)
    protected String name;

    public AbstractDriver() {
        this.id = Configuration.createId("DRV");
        this.name = getClass().getSimpleName();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        Object oldValue = this.name;
        this.name = name;
        firePropertyChange("name", oldValue, name);
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {
                new PropertySheetWizardAdapter(getConfigurationWizard()),
        };
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return new Action[] { deleteAction };
    }

    @SuppressWarnings("serial")
    public Action deleteAction = new AbstractAction("Delete Driver") {
        {
            putValue(SMALL_ICON, Icons.delete);
            putValue(NAME, "Delete Driver");
            putValue(SHORT_DESCRIPTION, "Delete the currently selected driver.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            int ret = JOptionPane.showConfirmDialog(MainFrame.get(),
                    "Are you sure you want to delete " + getName() + "?",
                    "Delete " + getName() + "?", JOptionPane.YES_NO_OPTION);
            if (ret == JOptionPane.YES_OPTION) {
                Configuration.get().getMachine().removeDriver(AbstractDriver.this);
            }
        }
    };

    @Override
    public Icon getPropertySheetHolderIcon() {
        return Icons.driver;
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        return null;
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName() + " " + getName();
    }

}
