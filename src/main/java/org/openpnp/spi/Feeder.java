/*
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.spi;

import java.util.List;

import org.openpnp.model.Identifiable;
import org.openpnp.model.Location;
import org.openpnp.model.Named;
import org.openpnp.model.Part;



/**
 * A Feeder is an abstraction that represents any type of part source. It can be a tape and reel
 * feeder, a tray handler, a single part in a specific location or anything else that can be used as
 * a pick source.
 */
public interface Feeder extends Identifiable, Named, WizardConfigurable, PropertySheetHolder {
    /**
     * Return true is the Feeder is currently enabled and can be considered in Job planning.
     * 
     * @return
     */
    public boolean isEnabled();

    public void setEnabled(boolean enabled);

    /**
     * Get the Part that is loaded into this Feeder.
     * 
     * @return
     */
    public Part getPart();

    /**
     * Set the Part that is loaded into this Feeder.
     */
    public void setPart(Part part);

    /**
     * Gets the Location from which the currently available Part should be picked from. This value
     * may not be valid until after a feed has been performed for Feeders who update the pick
     * location.
     * 
     * @return
     */
    public Location getPickLocation() throws Exception;

    /**
     * Some feeders need preparation for a Job that is best done up front and in bulk, such as vision 
     * calibration, actuating covers, checking OCR labels etc. Some prep requires the head moving to the 
     * feeder. For efficiency the JobProcessor uses a Travelling Salesman algorithm to visit
     * all these feeders. The locations for these visits are gathered using getJobPreparationLocation().
     *  
     * @return The location for the feeder Job preparation visit or null if none.
     */
    public Location getJobPreparationLocation();
    
    /**
     * Prepares a Feeder for usage in a Job. This is done for all the feeders that are enabled and 
     * contain Parts that are used in pending placements. Preparation is done when the Job is started, 
     * so it can perform bulk initialization that should not be postponed until the Nozzle.feed() 
     * 
     * @param visit true for visits along the getJobPreparationLocation() travel path, false for 
     * general preparation (second pass for visited feeders).
     *  
     * @throws Exception
     */
    public void prepareForJob(boolean visit) throws Exception;
    
    /**
     * Commands the Feeder to do anything it needs to do to prepare the part to be picked by the
     * specified Nozzle. If the Feeder requires Head interaction to feed it will perform those
     * operations during this call.
     * 
     * @param nozzle The Nozzle to be used for picking after the feed is completed. The Feeder may
     *        use this Nozzle to determine which Head, and therefore which Actuators and Cameras it
     *        can use for assistance.
     * @return The Location where the fed part can be picked from.
     * @throws Exception
     */
    public void feed(Nozzle nozzle) throws Exception;

    public void postPick(Nozzle nozzle) throws Exception;
    
    public int getFeedRetryCount();
    
    public int getPickRetryCount();
}
