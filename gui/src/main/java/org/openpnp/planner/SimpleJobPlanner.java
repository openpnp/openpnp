package org.openpnp.planner;

import java.util.HashSet;
import java.util.Set;

import org.openpnp.model.JobPlacement;
import org.openpnp.model.JobPlacement.JobPlacementState;
import org.openpnp.spi.Head;

public class SimpleJobPlanner extends AbstractJobPlanner {
    @Override
    public synchronized Set<PlacementSolution> getNextPlacementSolutions(Head head) {
        Set<PlacementSolution> solutions = new HashSet<PlacementSolution>();
        
//        StupidJobPlanner
//        1. Find an unplaced and unchecked placement. If none, fail.
//        2. See if the head can access it. If not, go to 1.
//        3. Find a feeder that can feed the part to the head. If none, go to 1. 
//        4. Return Placement / Feeder solution.
        
        Set<JobPlacement> placements = jobSession.getPlacementsInState(JobPlacementState.Ready);
        if (placements.size() == 0) {
            return solutions;
        }
        JobPlacement placement = placements.iterator().next();
        
        PlacementSolution solution = new PlacementSolution(
                placement.getPlacement(), 
                placement.getBoardLocation(),
                head,
                null,
                null,
                null);
        solutions.add(solution);
        return solutions;
    }
}
