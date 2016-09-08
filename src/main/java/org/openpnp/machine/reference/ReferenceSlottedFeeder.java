package org.openpnp.machine.reference;

import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.SlottedFeeder;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.FeederSlot;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.base.AbstractFeeder;
import org.openpnp.spi.base.AbstractSlottedFeeder;
import org.simpleframework.xml.Element;

import java.util.List;

public abstract class ReferenceSlottedFeeder extends ReferenceFeeder implements SlottedFeeder {

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    @Override
    public void feed(Nozzle nozzle) throws Exception {

        FeederSlot slot = Configuration.get().getMachine().getFeederSlotByFeeder(this);

        if (slot == null) {
            /*
                   If our slot is null then we aren't mounted, yet the jobprocessor wants us to feed,
                   so prompt the user and get them to mount the feeder...

                   Really this needs some logic to suggest which is the best bank, but for now, we just use
                   the first one in our configuration?
             */
            List<FeederSlot> feederSlots = Configuration.get().getMachine().getFeederSlots();

            FeederSlot suggestedSlot = null;

            // look for any empty slots we could use?
            Boolean foundEmptySlot = false;
            for(int i=0;i<feederSlots.size();i++)
            {
                if(feederSlots.get(i).getFeeder() == null)
                {
                    foundEmptySlot = true;
                    suggestedSlot = feederSlots.get(i);
                    break;
                }
            }

            Boolean foundUnusedSlot = false;
            if(!foundEmptySlot)
            {
                // all our slots are in use... loop round the job and see if any won't get used anymore
                ReferencePnpJobProcessor jobProcessor = (ReferencePnpJobProcessor) Configuration.get().getMachine().getPnpJobProcessor();
                List<ReferencePnpJobProcessor.JobPlacement> jobPlacements = jobProcessor.getJobPlacements();

                for (FeederSlot feederSlot: feederSlots)
                {
                    Feeder feeder = feederSlot.getFeeder();

                    Boolean feederUsed = false;
                    for (ReferencePnpJobProcessor.JobPlacement jobPlacement : jobPlacements) {
                        if (jobPlacement.status == ReferencePnpJobProcessor.JobPlacement.Status.Complete) {
                            continue;
                        }

                        if (jobPlacement.placement.getPart() == feeder.getPart())
                        {
                            feederUsed = true;
                            break;
                        }
                    }

                    if(feederUsed == false)
                    {
                        suggestedSlot = feederSlot;
                        foundUnusedSlot = true;
                        break;
                    }
                }
            }

            if(!foundEmptySlot && !foundUnusedSlot)
            {
                // no free slots, and we have all the feeders currently mounted in the banks still needed by the rest of the program
                // for now... we just tell the user to put the feeder in the first bank slot.

                for (FeederSlot feederSlot: feederSlots)
                {
                    suggestedSlot = feederSlot;
                    break;
                }
            }

            String msg = "Feeder '"+this.getName() + "' is required but not mounted into a feeder slot, please mount it into feeder slot '"+suggestedSlot.getName()+"' and press OK.";
            throw new Exception(msg);
        }
    }
}
