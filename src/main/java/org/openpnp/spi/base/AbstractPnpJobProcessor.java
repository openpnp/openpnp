package org.openpnp.spi.base;

import org.pmw.tinylog.Logger;
import org.openpnp.model.Part;
import org.openpnp.model.Location;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PnpJobProcessor;
import org.openpnp.util.Cycles;
import org.openpnp.util.FeederUtils;

public abstract class AbstractPnpJobProcessor extends AbstractJobProcessor
        implements PnpJobProcessor {

    public static void discardAll(Head head) throws JobProcessorException {
        for (Nozzle nozzle : head.getNozzles()) {
            discard(nozzle);
        }
    }


    /**
     * Discard the Part, if any, on the given Nozzle.
     * 
     * @param nozzle
     * @throws Exception
     */
    public static void discard(Nozzle nozzle) throws JobProcessorException {
        try {
            Cycles.discard(nozzle);
        }
        catch (Exception e) {
            throw new JobProcessorException(nozzle, e);
        }
    }

    /**
     * Find the first enabled Feeder is that is able to feed the given Part.
     * 
     * @param part
     * @return
     * @throws Exception If no Feeder is found that is both enabled and is serving the Part.
     */
    public static Feeder findFeeder(Machine machine,Part part,Feeder preference,Location datum) throws JobProcessorException {
        Feeder feeder = null;

        try {
            feeder = FeederUtils.findFeeder(machine,part,preference,datum);
        } catch (Exception e) {
            throw new JobProcessorException(null, e);
        }

        if(feeder==null) {
            throw new JobProcessorException(part, "No compatible, enabled feeder found for part " + part.getId());
        }

        return feeder;
    }

}
