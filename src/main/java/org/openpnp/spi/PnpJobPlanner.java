package org.openpnp.spi;

import java.util.List;

import org.openpnp.spi.PnpJobProcessor.JobPlacement;

public interface PnpJobPlanner {
    public static class PlannedPlacement {
        public final JobPlacement jobPlacement;
        public final Nozzle nozzle;
        public final NozzleTip nozzleTip;
        public Feeder feeder;
        public PartAlignment.PartAlignmentOffset alignmentOffsets;

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
