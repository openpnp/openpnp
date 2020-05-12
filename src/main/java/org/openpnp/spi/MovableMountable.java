package org.openpnp.spi;

import org.openpnp.model.AxesLocation;
import org.openpnp.model.Location;
import org.openpnp.model.MappedAxes;

/**
 * Anything that can be mounted to the machine and is Movable has its mapped Axes.
 *
 */
public interface MovableMountable extends Movable {
    Axis getAxisX();

    Axis getAxisY();

    Axis getAxisZ();

    Axis getAxisRotation();

    Axis getAxis(Axis.Type type);

    /**
     * Returns the set of motion-controller axes, mapped to the MovableMountable.
     * 
     * @param machine The machine with the axes in proper order. 
     * @return   
     */
    MappedAxes getMappedAxes(Machine machine);

    /**
     * Same as getMappedAxes(Machine machine), but with the motion-controller axes filtered by one driver.
     * 
     * @param machine
     * @param driver
     * @return
     */
    MappedAxes getMappedAxes(Machine machine, Driver driver);

    /**
     * Converts the specified raw motion-controller AxesLocation into a transformed Head Location. 
     * Note, toTransformed() must not throw even if transformations are not well-defined. This would otherwise
     * disrupt operation while setting the machine up. Only if toRaw() will throw, e.g. when you want to move the 
     * machine.  
     * 
     * @param location
     * @param options
     * @return
     */
    Location toTransformed(AxesLocation location, LocationOption... options);

    /**
     * Converts the specified transformed Head Location into a raw motion-controller AxesLocation. 
     * Note, unlike toTransformed() toRaw() will throw if the reverse transformation is not well-defined. 
     * As this typically happens when trying to move the machine, it is an appropriately graded precaution.   
     * 
     * @param location
     * @param options
     * @return
     * @throws Exception
     */
    AxesLocation toRaw(Location location, LocationOption... options) 
            throws Exception;
}
