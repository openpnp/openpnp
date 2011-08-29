package org.openpnp;

/**
 * A Fiducial is an identifying mark on a Board that can be used to identify the offset and rotation of the board
 * for placement operations.
 */
public interface Fiducial {
	Location getLocation();
}
