package org.openpnp.spi;

import java.util.ArrayList;
import java.util.List;

import org.openpnp.gui.support.Wizard;
import org.openpnp.model.PlacementsHolderLocation;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.model.PartSettingsHolder;


/**
 * Provides a method to detect and orient boards using a fiducial system. This interface is
 * primarily implemented by the ReferenceFidicualLocator but is provided so that future expansion
 * can occur without requiring configuration changes.
 */
public interface FiducialLocator extends PropertySheetHolder {
    public Location locateAllPlacementsHolder(List<PlacementsHolderLocation<?>> placementsHolderLocations, Location endLocation) 
            throws Exception;

    default Location locatePlacementsHolder(PlacementsHolderLocation<?> placementsHolderLocation) 
            throws Exception {
        List<PlacementsHolderLocation<?>> list = new ArrayList<PlacementsHolderLocation<?>>();
        list.add(placementsHolderLocation);
        // the default implementation assumes, that the end location is the origin of the board/panel
        return locateAllPlacementsHolder(list, placementsHolderLocation.getGlobalLocation());
    }

    public Location getHomeFiducialLocation(Location location, Part part) throws Exception;
    
    /**
     * Get a Wizard for configuring the FiducialLocator instance properties for a specific
     * part settings holder.
     * @param part
     * @return
     */
    Wizard getPartConfigurationWizard(PartSettingsHolder partSettingsHolder);
}
