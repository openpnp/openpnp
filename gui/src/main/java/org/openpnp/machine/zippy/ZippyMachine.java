package org.openpnp.machine.zippy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.spi.Head;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.openpnp.model.Configuration;

public class ZippyMachine extends ReferenceMachine {
//	@Override
	public List<Class<? extends NozzleTip>> getCompatibleNozzleTipClasses() {
		List<Class<? extends NozzleTip>> l = new ArrayList<Class<? extends NozzleTip>>();
		l.add(ZippyNozzleTip.class);
		return l;
	}
	
    public List<NozzleTip> getNozzleTips() {
    	Configuration configuration = Configuration.get(); 
    	List<NozzleTip> nozzletips = new ArrayList<NozzleTip>(); 
		for (Head head : configuration.getMachine().getHeads()) { //for each head
			for (Nozzle nozzle : head.getNozzles()) { //for each nozzle
				for (NozzleTip nozzletip : nozzle.getNozzleTips()) { //for each nozzletip
					nozzletips.add(nozzletip); //add to list from above
				}
			}
		}
		return nozzletips;
   }

}
