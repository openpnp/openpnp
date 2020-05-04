package org.openpnp.spi;

/**
 * A LinearInputAxis is either directly a controller axis or something transformed by one
 * of the "mechanical" transformations. This Interface serves as the distinction, so only
 * one top level linear transformation can be set up. 
 */
public interface LinearInputAxis extends Axis {
}
