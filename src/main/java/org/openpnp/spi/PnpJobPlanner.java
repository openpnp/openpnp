package org.openpnp.spi;

import java.util.List;
import java.util.ArrayList;

import org.openpnp.Translations;
import org.openpnp.spi.PnpJobProcessor.JobPlacement;

public interface PnpJobPlanner {
    public static class PlannedPlacement {
        public final JobPlacement jobPlacement;
        public final Nozzle nozzle;
        public final NozzleTip nozzleTip;
        public PartAlignment.PartAlignmentOffset alignmentOffsets;
        public Double planningCost;
        
        public PlannedPlacement(Nozzle nozzle, NozzleTip nozzleTip, JobPlacement jobPlacement, Double planningCost) {
            this.nozzle = nozzle;
            this.nozzleTip = nozzleTip;
            this.jobPlacement = jobPlacement;
            this.planningCost = planningCost;
        }

        @Override
        public String toString() {
            return String.format("%s (%s) -> %s", nozzle.getName(), nozzleTip.getName(), jobPlacement);
        }
    }

    public static class PlannerStepResults {
        private final List<PlannedPlacement> plannedPlacements;
        public final long durationMilliseconds;

        public PlannerStepResults(List<PlannedPlacement> plannedPlacements,long durationMilliseconds) {
            this.plannedPlacements = plannedPlacements;
            this.durationMilliseconds = durationMilliseconds;
        }

        public List<PlannedPlacement> getPlannedPlacements() {
            return new ArrayList<PlannedPlacement>(plannedPlacements);
        }
    }
    
    /**
     * JobPlanner tip loading strategy: depending on the strategy, the list of placements is searched to
     * find a placement, that can be handled using the current nozzle tip or the list is strictly
     * followed, potentially executing more nozzle tip changes then needed.
     */
    public enum Strategy {
        Minimize,       // avoid any nozzle tip change by searching the placements list.
        StartAsPlanned, // place the first part with the required nozzle tip and then avoid nozzle tip changes.
        FullyAsPlanned; // strictly follow the placements list executing all nozzle tip changes as needed.

        @Override
        public String toString() {
            return Translations.getString("MachineSetup.JobProcessors.ReferencePnpJobProcessor.Strategy." + this.name());
        }
    }

    /**
     * Call restart() to signal that a new job run will start next. That allows to
     * support a first-run strategies (Strategy.StartAsPlanned).
     */
    public void restart();
    public Strategy getStrategy();
    public void setStrategy(Strategy strategy);
    public List<PlannedPlacement> plan(Head head, List<JobPlacement> jobPlacements, List<NozzleTip> nozzleTips);
    public List<PlannedPlacement> sort(List<PlannedPlacement> plannedPlacements);
}
