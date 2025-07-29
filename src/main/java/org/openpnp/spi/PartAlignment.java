package org.openpnp.spi;

import java.awt.image.BufferedImage;

import org.openpnp.gui.support.Wizard;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Location;
import org.openpnp.model.Named;
import org.openpnp.model.Part;
import org.openpnp.model.PartSettingsHolder;
import org.openpnp.model.Placement;
import org.openpnp.model.Solutions;

/**
 * A method to allow after-pick, pre-place alignment of parts on the nozzle. Bottom vision
 * is an implementation of this interface, but other implementations could include laser
 * alignment or pit alignment.  
 */
public interface PartAlignment extends PartSettingsHolder, Named, Solutions.Subject, PropertySheetHolder {

    public class PartAlignmentOffset
    {
        private Location location;
        private Boolean preRotated;

        public Location getLocation()
        {
            return location;
        }

        public Boolean getPreRotated()
        {
            return preRotated;
        }

        public PartAlignmentOffset(Location loc, Boolean PreRotated)
        {
            location=loc;
            preRotated=PreRotated;
        }

        public String toString() {
            return "offset ( location: " + location.toString() + " pre-rotated" + Boolean.toString(preRotated) + ")";
        }
    }

    /**
     * Perform the part alignment operation. The method must return a Location containing
     * the offsets on the nozzle of the aligned part and these offsets will be applied
     * by the JobProcessor. The offsets returned may be zero if the alignment process
     * results in physical alignment of the part as in the case of pit based alignment. The
     * Z portion of the Location is ignored.
     * @param part
     * @param nozzle
     * @return
     * @throws Exception if the alignment fails for any reason. The caller may retry.
     */
    PartAlignmentOffset findOffsets(Part part, BoardLocation boardLocation, Placement placement, Nozzle nozzle) throws Exception;
    
    /**
     * Return the location where the alignment will take place.
     * 
     * @param part
     * @param boardLocation
     * @param placement
     * @param nozzle
     * @return
     * @throws Exception
     */
    Location getLocation(Part part, BoardLocation boardLocation, Placement placement, Nozzle nozzle) throws Exception;

    /**
     * Get a Wizard for configuring the PartAlignment instance properties for a specific
     * PartSettingsHolder (Part or Package).
     * @param partSettingsHolder
     * @return
     */
    Wizard getPartConfigurationWizard(PartSettingsHolder partSettingsHolder);

    public boolean canHandle(PartSettingsHolder partSettingsHolder, boolean allowDisabled);

    boolean isEnabled();

    /**
     * Display the result of an Alignment on the camera view. 
     * 
     * @param image
     * @param part
     * @param offsets
     * @param camera
     * @param nozzle
     */
    void displayResult(BufferedImage image, Part part, Location offsets, Camera camera,
            Nozzle nozzle);

}
