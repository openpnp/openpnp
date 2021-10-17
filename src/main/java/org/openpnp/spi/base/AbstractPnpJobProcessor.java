package org.openpnp.spi.base;

import org.openpnp.machine.neoden4.Neoden4Feeder;
import org.openpnp.model.Configuration;
import org.openpnp.model.Part;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PartAlignment;
import org.openpnp.spi.PnpJobProcessor;
import org.openpnp.util.Cycles;

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
        	// Save part reference to find feeder
            Part part = nozzle.getPart();
            
            // If discarded part's feeder is instance of Neoden4Feeder
            // increment discard count
            Feeder feeder = findFeeder(Configuration.get().getMachine(), part);
            if (feeder instanceof Neoden4Feeder) {
            	int discardCount = ((Neoden4Feeder) feeder).getDiscardCount();
            	((Neoden4Feeder) feeder).setDiscardCount(discardCount + 1);
            }
            
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
    public static Feeder findFeeder(Machine machine, Part part) throws JobProcessorException {
        for (Feeder feeder : machine.getFeeders()) {
            if (feeder.getPart() == part && feeder.isEnabled()) {
                return feeder;
            }
        }
        throw new JobProcessorException(part, "No compatible, enabled feeder found for part " + part.getId());
    }


    public static PartAlignment findPartAligner(Machine machine, Part part) {
        for (PartAlignment partAlignment : machine.getPartAlignments()) {
            if (partAlignment.canHandle(part)) {
                return partAlignment;
            }
        }

        // if we can't find a part-aligner, thats ok.. the user might not have defined one, so we
        // place without aligning
        return null;
    }

}
