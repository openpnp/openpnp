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
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Axis;
import org.openpnp.spi.ControllerAxis;
import org.openpnp.spi.PropertySheetHolder;
import org.simpleframework.xml.Attribute;

/**
 * @author Markus
 *
 */
public abstract class AbstractAxis extends AbstractModelObject implements Axis {

    @Attribute
    protected String id;

    @Attribute(required = false)
    protected String name;

    @Attribute(required = false)
    protected Axis.Type type;

    public AbstractAxis() {
        this.id = Configuration.createId("AXS");
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
    public Axis.Type getType() {
        return type;
    }

    @Override
    public void setType(Axis.Type type) {
        this.type = type;
    }

    @Override
    public double getLocationAxisCoordinate(Location location) {
        switch (type) { 
            case X:
                return location.getX();
            case Y:
                return location.getY();
            case Z:
                return location.getZ();
            case Rotation:
                return location.getRotation();
            default:
                return 0.0;
        }
    }

    /**
     * @return the ControllerAxis that is the ultimate input axis of the TransformedAxis stack. 
     */
    public abstract ControllerAxis getControllerAxis();

    public static ControllerAxis getControllerAxis(AbstractAxis axis) {
        if (axis != null) {
            return axis.getControllerAxis();
        }
        return null;
    }

    /**
     * Transform the raw axis coordinate taken from the specified location into it's corresponding 
     * transformed coordinate. 
     * The transformed coordinate is what the user sees, while the raw coordinate is what the
     * motion controller sees.
     * Some transformations handle multiple axes, therefore the full Location is passed through.
     * 
     * A ControllerAxis will just return the unchanged coordinate. 
     * A TransformedAxis will first call the input axes' transformation and then perform its own.
     *        
     * @param location
     * @return the transformed axis coordinate in the LengthUnit of the given Location.  
     */
    public abstract double toTransformed(Location location); 

    /**
     * Transform the specified transformed location into it's corresponding raw location, returning
     * the coordinate for this axis. 
     * The transformed location is what the user sees, while the raw coordinate is what the motion 
     * controller sees.
     * Some transformations handle multiple axes, therefore the full Location is passed through.
     * 
     * A ControllerAxis will just return the unchanged coordinate. 
     * A TransformedAxis will first perform its own transformation and then call the input axis'.
     *        
     * @param location
     * @return the raw axis coordinate in the LengthUnit of the given Location.  
     */
    public abstract double toRaw(Location location, double [][] invertedAffineTransform); 

    /**
     * @param lengthUnit
     * @return the transformation 5D vector (factors, offset in given LengthUnit) for this Axis. 
     * Returns the unit vector if not a linear transformation.  
     */
    public double[] getLinearTransform(LengthUnit lengthUnit) {
        switch (type) {
            case X: 
                return new double [] {1, 0, 0, 0, 0};
            case Y: 
                return new double [] {0, 1, 0, 0, 0};
            case Z: 
                return new double [] {0, 0, 1, 0, 0};
            case Rotation: 
                return new double [] {0, 0, 0, 1, 0};
            default:
                return new double [] {0, 0, 0, 0, 0};
        }
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
    public Action deleteAction = new AbstractAction("Delete Axis") {
        {
            putValue(SMALL_ICON, Icons.delete);
            putValue(NAME, "Delete Axis");
            putValue(SHORT_DESCRIPTION, "Delete the currently selected axis.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            int ret = JOptionPane.showConfirmDialog(MainFrame.get(),
                    "Are you sure you want to delete " + getName() + "?",
                    "Delete " + getName() + "?", JOptionPane.YES_NO_OPTION);
            if (ret == JOptionPane.YES_OPTION) {
                Configuration.get().getMachine().removeAxis(AbstractAxis.this);
            }
        }
    };

    @SuppressWarnings("serial")
    public Action permutateUpAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.arrowUp);
            putValue(NAME, "Permutate Up");
            putValue(SHORT_DESCRIPTION, "Move the currently selected axis one position up.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            Configuration.get().getMachine().permutateAxis(AbstractAxis.this, -1);
        }
    };

    @SuppressWarnings("serial")
    public Action permutateDownAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.arrowDown);
            putValue(NAME, "Permutate Down");
            putValue(SHORT_DESCRIPTION, "Move the currently selected axis one position down.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            Configuration.get().getMachine().permutateAxis(AbstractAxis.this, +1);
        }
    };

    @Override
    public Icon getPropertySheetHolderIcon() {
        if (type == Axis.Type.Rotation) {
            return Icons.axisRotation;
        }
        else {
            return Icons.axisCartesian;
        }
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
