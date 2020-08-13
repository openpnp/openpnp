package org.openpnp.spi;

import java.io.Closeable;

import org.openpnp.model.Identifiable;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Named;

/**
 * A driver is the connection between OpenPnP and a machine controller. This is the purely conceptual interface 
 * while the ReferenceDriver interface holds the interface used for the ReferenceMachine.  
 */
public interface Driver extends Identifiable, Named, Closeable, WizardConfigurable, PropertySheetHolder {

    LengthUnit getUnits();

    boolean isSupportingPreMove();

    boolean isUsingLetterVariables();

    Length getFeedRatePerSecond();
}
