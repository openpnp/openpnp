package org.openpnp.spi;

import org.openpnp.model.Identifiable;
import org.openpnp.model.Location;
import org.openpnp.model.Named;

/**
 * An Axis can be any coordinate dimension, either a raw machine controller axis or a 
 * a Cartesian-coordinate or rotary axis to be transformed into a raw axis. 
 */
public interface Axis extends Identifiable, Named, WizardConfigurable, PropertySheetHolder {
    public enum Type {
        X,
        Y,
        Z,
        Rotation
    }

    public Type getType();

    public void setType(Type type);

    double getLocationAxisCoordinate(Location location);

}
