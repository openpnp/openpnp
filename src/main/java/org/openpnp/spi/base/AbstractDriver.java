package org.openpnp.spi.base;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JOptionPane;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.axis.ReferenceControllerAxis;
import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Axis;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Driver;
import org.openpnp.spi.Nozzle;
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
    public boolean isSupportingPreMove() {
        return false;
    }

    protected void createAxisMappingDefaults(ReferenceMachine machine) throws Exception {
        if (machine.getAxes().size() == 0) {
            // Create and map the standard axes to the HeadMountables. 
            ReferenceControllerAxis axisX = migrateAxis(machine, Axis.Type.X, "");
            ReferenceControllerAxis axisY = migrateAxis(machine, Axis.Type.Y, "");
            for (Camera hm : machine.getDefaultHead().getCameras()) {
                ((AbstractHeadMountable)hm).setAxisX(axisX);
                ((AbstractHeadMountable)hm).setAxisY(axisY);
            }
            for (Nozzle hm : machine.getDefaultHead().getNozzles()) {
                // Note, we create dedicated axes per Nozzle, assuming this is a test driver that does not
                // care about shared/dedicated axes or a single nozzle test GcodeDriver.  
                ReferenceControllerAxis axisZ = migrateAxis(machine, Axis.Type.Z, hm.getName());
                ReferenceControllerAxis axisRotation = migrateAxis(machine, Axis.Type.Rotation, hm.getName());
                ((AbstractHeadMountable)hm).setAxisX(axisX);
                ((AbstractHeadMountable)hm).setAxisY(axisY);
                ((AbstractHeadMountable)hm).setAxisZ(axisZ);
                ((AbstractHeadMountable)hm).setAxisRotation(axisRotation);
            }
            // No movable actuators mapped for these test drivers.
        }
    }

    protected ReferenceControllerAxis migrateAxis(ReferenceMachine machine, Axis.Type type, String suffix)
            throws Exception {
        ReferenceControllerAxis axis;
        axis = new ReferenceControllerAxis();
        axis.setType(type);
        axis.setName(type+suffix);
        axis.setDriver(this);
        machine.addAxis(axis);
        return axis;
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {
                new PropertySheetWizardAdapter(getConfigurationWizard()),
        };
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return new Action[] { deleteAction, permutateUpAction, permutateDownAction };
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

    @SuppressWarnings("serial")
    public Action permutateUpAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.arrowUp);
            putValue(NAME, "Permutate Up");
            putValue(SHORT_DESCRIPTION, "Move the currently selected driver one position up.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            Configuration.get().getMachine().permutateDriver(AbstractDriver.this, -1);
        }
    };

    @SuppressWarnings("serial")
    public Action permutateDownAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.arrowDown);
            putValue(NAME, "Permutate Down");
            putValue(SHORT_DESCRIPTION, "Move the currently selected driver one position down.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            Configuration.get().getMachine().permutateDriver(AbstractDriver.this, +1);
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

