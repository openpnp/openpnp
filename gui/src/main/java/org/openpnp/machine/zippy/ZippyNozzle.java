package org.openpnp.machine.zippy;

import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.spi.NozzleTip;

public class ZippyNozzle extends ReferenceNozzle {

//    @Override
    public void addNozzleTip(NozzleTip nozzletip) throws Exception {
        nozzletips.add(nozzletip);
    }	
}
