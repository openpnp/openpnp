package org.openpnp.spi.base;

import org.openpnp.model.Configuration;
import org.openpnp.model.Part;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.openpnp.spi.PartAlignment;
import org.openpnp.spi.PnpJobProcessor;
import org.openpnp.util.MovableUtils;

public abstract class AbstractPnpJobProcessor extends AbstractJobProcessor
        implements PnpJobProcessor {

    public static void discardAll(Head head) throws PnpJobProcessorException {
        for (Nozzle nozzle : head.getNozzles()) {
            discard(nozzle);
        }
    }


    /**
     * Discard the Part, if any, on the given Nozzle. the Nozzle is returned to Safe Z at the end of
     * the operation.
     * 
     * @param nozzle
     * @throws Exception
     */
    public static void discard(Nozzle nozzle) throws PnpJobProcessorException {
        if (nozzle.getPart() == null) {
            return;
        }
        try {
            // move to the discard location
            MovableUtils.moveToLocationAtSafeZ(nozzle,
                    Configuration.get().getMachine().getDiscardLocation());
            // discard the part
            nozzle.place();
            nozzle.moveToSafeZ();
            
            
        }
        catch (Exception e) {
            throw new PnpJobProcessorException(nozzle, e);
        }
    }

    public static NozzleTip findNozzleTip(Nozzle nozzle, Part part) throws PnpJobProcessorException {
        for (NozzleTip nozzleTip : nozzle.getNozzleTips()) {
            if (nozzleTip.canHandle(part)) {
                return nozzleTip;
            }
        }
        throw new PnpJobProcessorException(part,
                "No compatible nozzle tip on nozzle " + nozzle.getName() + " found for part " + part.getId());
    }

    public static boolean nozzleCanHandle(Nozzle nozzle, Part part) {
        for (NozzleTip nozzleTip : nozzle.getNozzleTips()) {
            if (nozzleTip.canHandle(part)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Find the first NozzleTip that is able to handle the given Part.
     * 
     * @param part
     * @return
     * @throws Exception If no compatible NozzleTip can be found.
     */
    public static NozzleTip findNozzleTip(Head head, Part part) throws PnpJobProcessorException {
        for (Nozzle nozzle : head.getNozzles()) {
            try {
                return findNozzleTip(nozzle, part);
            }
            catch (Exception e) {
            }
        }
        throw new PnpJobProcessorException(part, "No compatible nozzle tip on any nozzle found for part " + part.getId());
    }

    /**
     * Find the first enabled Feeder is that is able to feed the given Part.
     * 
     * @param part
     * @return
     * @throws Exception If no Feeder is found that is both enabled and is serving the Part.
     */
    public static Feeder findFeeder(Machine machine, Part part) throws PnpJobProcessorException {
        for (Feeder feeder : machine.getFeeders()) {
            if (feeder.getPart() == part && feeder.isEnabled()) {
                return feeder;
            }
        }
        throw new PnpJobProcessorException(part, "No compatible, enabled feeder found for part " + part.getId());
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
