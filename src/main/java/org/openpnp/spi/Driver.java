package org.openpnp.spi;

import java.io.Closeable;

import org.openpnp.model.Identifiable;
import org.openpnp.model.Named;

/**
 * An Axis can be any coordinate dimension, either a raw machine controller axis or a 
 * a Cartesian-coordinate or rotary axis to be transformed into a raw axis. 
 */
public interface Driver extends Identifiable, Named, Closeable, WizardConfigurable, PropertySheetHolder {
}
