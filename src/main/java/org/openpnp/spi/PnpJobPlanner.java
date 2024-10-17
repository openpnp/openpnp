package org.openpnp.spi;

import java.util.List;

import org.openpnp.spi.PnpJobProcessor.JobPlacement;

public interface PnpJobPlanner {
    public static class PlannedPlacement {
        public final JobPlacement jobPlacement;
        public final Nozzle nozzle;
        public final NozzleTip nozzleTip;
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
    
    /**
     * JobPlanner strategy: depending on the strategy, the list of placements is searched to
     * find a placement, that can be handled using the current nozzle tip or the list is strictly
     * followed, potentially executing more nozzle tip changes then needed.
     */
    public enum Strategy {
        Minimize,       // avoid any nozzle tip change by searching the placements list.
        StartAsPlanned, // place the first part with the required nozzle tip and then avoid nozzle tip changes.
        FullyAsPlanned  // strictly follow the placements list executing all nozzle tip changes as needed.
    }

    /**
     * Call restart() to signal that a new job run will start next. That allows to
     * support a first-run strategies (Strategy.StartAsPlanned).
     */
    public void restart();
    public Strategy getStrategy();
    public void setStrategy(Strategy strategy);
    public List<PlannedPlacement> plan(Head head, List<JobPlacement> placements, List<NozzleTip> nozzleTips);
    public List<PlannedPlacement> sort(List<PlannedPlacement> plannedPlacements);
}
