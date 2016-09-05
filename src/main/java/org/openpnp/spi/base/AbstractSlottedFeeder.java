package org.openpnp.spi.base;

import org.openpnp.model.Location;
import org.openpnp.spi.FeederSlot;
/**
 * Created by matt on 05/09/2016.
 */
public abstract class AbstractSlottedFeeder extends AbstractFeeder {

    public boolean isEnabled()
    {

       /* if(slotRequired && slot==null)
        {
            return false;
        } */

        return enabled;
    }

    public Location getPickLocation() throws Exception
    {
        //     - Overrides getPickLocation() to return the location of the assigned slot, if any.
        return null;
    }
}
