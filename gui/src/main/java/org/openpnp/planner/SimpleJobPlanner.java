package org.openpnp.planner;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Job;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;
import org.simpleframework.xml.Attribute;

public class SimpleJobPlanner extends AbstractJobPlanner {
    @SuppressWarnings("unused")
    @Attribute(required = false)
    private String placeHolder;
    
    private Set<PlacementSolution> solutions = new LinkedHashSet<PlacementSolution>();
    
    @Override
    public void setJob(Job job) {
        super.setJob(job);
        Head head = Configuration.get().getMachine().getHeads().get(0);
        for (BoardLocation boardLocation : job.getBoardLocations()) {
            for (Placement placement : boardLocation.getBoard().getPlacements()) {
                if (placement.getSide() != boardLocation.getSide()) {
                    continue;
                }
                solutions.add(new PlacementSolution(placement, boardLocation, head, null, null, null));
            }
        }
    }

    @Override
    public synchronized Set<PlacementSolution> getNextPlacementSolutions(Head head) {
        // TODO: Make sure the head and nozzle can reach the feeder and placement
        // might need to actually translate everything to final coordinates first, maybe
        // that gets weird
        Set<PlacementSolution> results = new LinkedHashSet<PlacementSolution>();
        Iterator<PlacementSolution> i = solutions.iterator();
        for (Nozzle nozzle : head.getNozzles()) {
            if (!i.hasNext()) {
                break;
            }
            PlacementSolution solution = i.next();
            i.remove();
            Feeder feeder = getFeederSolution(Configuration.get().getMachine(), nozzle, solution.placement.getPart());
            // We potentially return null here for Feeder, which lets the JobProcessor know that no applicable
            // Feeder was found. 
            solution = new PlacementSolution(solution.placement, solution.boardLocation, solution.head, nozzle, nozzle.getNozzleTip(), feeder);
            results.add(solution);
        }
        return results.size() > 0 ? results : null;
    }
    
    private static Feeder getFeederSolution(Machine machine, Nozzle nozzle, Part part) {
        // Get a list of Feeders that can source the part
        List<Feeder> feeders = new ArrayList<Feeder>();
        for (Feeder feeder : machine.getFeeders()) {
            if (feeder.getPart() == part && feeder.canFeedToNozzle(nozzle) && feeder.isEnabled()) {
                feeders.add(feeder);
            }
        }
        if (feeders.size() < 1) {
            return null;
        }
        // For now we just take the first Feeder that can feed the part.
        Feeder feeder = feeders.get(0);
        return feeder;
    }
}
