package org.openpnp.spi.base;

import org.openpnp.model.Configuration;
import org.openpnp.model.Part;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.openpnp.spi.PnpJobProcessor;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.util.MovableUtils;

public abstract class AbstractPnpJobProcessor extends AbstractJobProcessor
        implements PnpJobProcessor {


    /**
     * Discard the Part, if any, on the given Head or all Heads if parameter is null. 
	 * The Nozzle is returned to Safe Z at the end of the operation.
     * 
     * @param head
     * @throws Exception
     */
    public static void discardAll(Head head) throws Exception {
    	for (Head hd : Configuration.get().getMachine().getHeads()) {
			if(head==null||hd==head) {
				for (Nozzle nozzle : head.getNozzles()) {
					discard(nozzle);
				}
			}
		}
    }


    /**
     * Discard the Part, if any, on the given Nozzle. the Nozzle is returned to Safe Z at the end of
     * the operation.
     * 
     * @param nozzle
     * @throws Exception
     */
    public static void discard(Nozzle nozzle) throws Exception {
        if (nozzle.getPart() == null) {
            return;
        }
        // move to the discard location
        MovableUtils.moveToLocationAtSafeZ(nozzle,
                Configuration.get().getMachine().getDiscardLocation());
        // discard the part
        nozzle.place();
		// repeat the air puff to clean filters over discard location
        nozzle.place();
        nozzle.place();
        nozzle.place();
        nozzle.moveToSafeZ();
        nozzle.place();
    }

    public static NozzleTip findNozzleTip(Nozzle nozzle, Part part) throws Exception {
        for (NozzleTip nozzleTip : nozzle.getNozzleTips()) {
            if (nozzleTip.canHandle(part)) {
                return nozzleTip;
            }
        }
        throw new Exception(
                "No compatible nozzle tip on nozzle " + nozzle.getName() + " found for part " + part.getId());
    }

 		
	public static boolean nozzleCanHandle(Nozzle nozzle, Part part) {
		if(nozzle==null||nozzle.getNozzleTip()==null) return false;
	 
		if(((ReferenceNozzle)nozzle).isChangerEnabled()) {
			for (NozzleTip nozzleTip : nozzle.getNozzleTips()) {
				if (nozzleTip.canHandle(part)) {
					return true;
				}
			}
			return false;
		}
		return nozzle.getNozzleTip().canHandle(part);
	}

    /**
     * Find the first NozzleTip that is able to handle the given Part.
     * 
	 * @param head , if set to null, it searches all heads.
     * @param part
     * @return
     * @throws Exception If no compatible NozzleTip can be found.
     */
    public static NozzleTip findNozzleTip(Head head, Part part) throws Exception {
		for (Head hd : Configuration.get().getMachine().getHeads()) {
			if(head==null||hd==head) {
				for (Nozzle nozzle : head.getNozzles()) {
					try {
						return findNozzleTip(nozzle, part);
					}
					catch (Exception e) {
					}
				}
			}
		}
        throw new Exception("No compatible nozzle tip on any nozzle found for part " + part.getId());
    }

    /**
     * Find the first enabled Feeder is that is able to feed the given Part.
     * 
     * @param part
     * @return
     * @throws Exception If no Feeder is found that is both enabled and is serving the Part.
     */
    public static Feeder findFeeder(Machine machine, Part part) throws Exception {
		if(part!=null)
        for (Feeder feeder : machine.getFeeders()) {
            if (feeder.getPart().getId().equals(part.getId()) && feeder.isEnabled()) {
                return feeder;
            }
        }
        throw new Exception("No compatible, enabled feeder found for part " + part.getId());
    }


}
