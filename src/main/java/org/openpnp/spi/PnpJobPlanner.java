package org.openpnp.spi;

import java.util.List;

import org.openpnp.spi.PnpJobProcessor.JobPlacement;
import org.openpnp.model.Location;

public interface PnpJobPlanner {
    public static class PlannedPlacement {
        public final JobPlacement jobPlacement;
        public final Nozzle nozzle;
        public final NozzleTip nozzleTip;
        public Feeder feeder;
        public PartAlignment.PartAlignmentOffset alignmentOffsets;
        
        // the following locations are used for optimization purposes and may not be accurate or complete.
        public Location pickLocation;   // (preliminary) location the pick will take place - as feed/pick might not work, this location is preliminary and optimization using it my fail.
        public Location alignLocation;  // location the alignment will take place. - may be null if alignment for this part is disabled
        public Location placeLocation;  // location the part will be place to

        public PlannedPlacement(Nozzle nozzle, NozzleTip nozzleTip, JobPlacement jobPlacement) {
            this.nozzle = nozzle;
            this.nozzleTip = nozzleTip;
            this.jobPlacement = jobPlacement;
        }

        @Override
        public String toString() {
            return String.format("%s (%s) -> %s", nozzle.getName(), nozzleTip.getName(), jobPlacement);
        }
    }

    
    public List<PlannedPlacement> plan(Head head, List<JobPlacement> placements);
}
