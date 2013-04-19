package org.openpnp.spi;

import java.util.Set;

import org.openpnp.model.BoardLocation;
import org.openpnp.model.Job;
import org.openpnp.model.Placement;


/**
 * The JobPlanner is responsible for planning the order in which Placements
 * are processed and determining which Head/Nozzle/NozzleTip/Feeder combination
 * will be used for each Placement. It is called each time a Head is ready for
 * new work.
 */
public interface JobPlanner {
    /**
     * Represents one solution from the planner for a particular Placement.
     * Includes the Head, Nozzle, NozzleTip and Feeder that should be used
     * to service the Placement. 
     */
    public static class PlacementSolution {
        public BoardLocation boardLocation;
        public Placement placement;
        public Head head;
        public Nozzle nozzle;
        public NozzleTip nozzleTip;
        public Feeder feeder;
        
        public PlacementSolution(Placement placement, BoardLocation boardLocation, Head head, Nozzle nozzle, NozzleTip nozzleTip, Feeder feeder) {
            this.placement = placement;
            this.boardLocation = boardLocation;
            this.head = head;
            this.nozzle = nozzle;
            this.nozzleTip = nozzleTip;
            this.feeder = feeder;
        }
    }
    
    public void setJob(Job job);
    
    /**
     * Gets a Set of the next available PlacementSolutions for the
     * specified Head. The order in which Heads service Placements
     * can not be reliably determined, so it is important that this
     * method return PlacementSolutions for the specified Head using
     * the state of the Job as it stands now.
     * 
     * This method must be thread safe as it may be called by multiple
     * threads for different Heads simultaneously. This doesn't mean it
     * necessarily needs to process concurrently - it can simply be
     * synchronized although concurrent processing is an option for
     * more advanced planners. 
     * @param head
     * @return
     */
    Set<PlacementSolution> getNextPlacementSolutions(Head head);
}
