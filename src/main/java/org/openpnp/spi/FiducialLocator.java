package org.openpnp.spi;

import org.openpnp.gui.support.Wizard;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Location;
import org.openpnp.model.Part;


/**
 * Provides a method to detect and orient boards using a fiducial system. This interface
 * is primarily implemented by the ReferenceFidicualLocator but is provided so that
 * future expansion can occur without requiring configuration changes. 
 */
public interface FiducialLocator extends PropertySheetHolder {
    Location locateBoard(BoardLocation boardLocation) throws Exception;
    Location getHomeFiducialLocation(Location location, Part part ) throws Exception;
    
    Wizard getPartConfigurationWizard(Part part);
}
