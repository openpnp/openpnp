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
 * 
 * The current planning system does not do a good job of handling the case where
 * one feeder may fail to feed so we want to fall back to another one.
 * 
 * For instance, say you load 5 strip feeders all with the same part. When the
 * first one is consumed you want to just fall back to the next and next until
 * they are all exhausted, and then finally throw an error. Or alternately, tell
 * the user that no feeders are available and let them reload some.
 * We can do this to a point with canFeedToNozzle, but it does not allow us to
 * resume once the user has corrected the problem because a new planning
 * solution will not be created until the job is restarted.
 * 
 * Perhaps the job planner needs to be bi-directional. You ask for a list
 * of solutions and then tell it which ones you consumed. Until they are
 * consumed they still available for planning - although that would break with
 * multiple heads.
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
        
        @Override
        public String toString() {
            return "PlacementSolution [boardLocation=" + boardLocation
                    + ", placement=" + placement + ", head=" + head
                    + ", nozzle=" + nozzle + ", nozzleTip=" + nozzleTip
                    + ", feeder=" + feeder + "]";
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
