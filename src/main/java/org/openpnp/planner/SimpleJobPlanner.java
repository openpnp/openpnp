package org.openpnp.planner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Job;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.openpnp.model.Placement.Type;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.simpleframework.xml.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleJobPlanner extends AbstractJobPlanner {
    private final static Logger logger = LoggerFactory.getLogger(SimpleJobPlanner.class);
    
    @SuppressWarnings("unused")
    @Attribute(required = false)
    private String placeHolder;
    
    protected Set<PlacementSolution> solutions = new LinkedHashSet<>();
    
    @Override
    public void setJob(Job job) {
        super.setJob(job);
        solutions.clear();
        for (BoardLocation boardLocation : job.getBoardLocations()) {
        	if (!boardLocation.isEnabled()) {
        		continue;
        	}
        	
            for (Placement placement : boardLocation.getBoard().getPlacements()) {
                if (placement.getType() != Type.Place) {
                    continue;
                }

                if (placement.getSide() != boardLocation.getSide()) {
                    continue;
                }
                
                solutions.add(new PlacementSolution(placement, boardLocation, null, null, null, null));
            }
        }
        
        logger.debug("Planned {} solutions", solutions.size());
    }

    /**
     * For N nozzles, create a list of every possible remaining solutions
     * along with a weight. Weight is a cost of 0 or more with things like
     * a nozzle change increasing the weight.
     * Once all solutions are identified. Sort each list by weight and take the
     * N lowest weighted solutions that do not conflict with each other.
     *
     * TODO: Is there a situation where the order in which we take the weighted
     * solutions from their lists would cause successive nozzles to have to
     * use less optimal solutions? I feel like this is a possible issue but
     * I haven't been able to come up with an example case.
     */
    @Override
    public synchronized Set<PlacementSolution> getNextPlacementSolutions(Head head) {
        Set<PlacementSolution> results = new LinkedHashSet<>();
        Machine machine = Configuration.get().getMachine();
        for (Nozzle nozzle : head.getNozzles()) {
            for (WeightedPlacementSolution solution : getWeightedSolutions(machine, nozzle)) {
                if (solutions.contains(solution.originalSolution)) {
                    results.add((PlacementSolution) solution);
                    solutions.remove(solution.originalSolution);
                    break;
                }
            }
        }
        // If no solutions were found but there are still placements remaining
        // in the job then we failed to either find a nozzletip or feeder
        // for a placement. We return the solutions as they are, which includes
        // nulls for feeder and nozzle.
        // TODO: It would be better if we filled in what we *could* find
        // so that the processor will give the user the correct error.
        // Or we could just throw the error here.
        if (results.size() == 0 && solutions.size() > 0) {
            return solutions;
        }
        return results.size() > 0 ? results : null;
    }
    
    protected List<WeightedPlacementSolution> getWeightedSolutions(Machine machine, Nozzle nozzle) {
        List<WeightedPlacementSolution> weightedSolutions = new ArrayList<>();
        for (PlacementSolution solution : solutions) {
            Part part = solution.placement.getPart();
            if (part == null) {
                continue;
            }
            Set<NozzleTip> compatibleNozzleTips = getCompatibleNozzleTips(nozzle, part);
            Set<Feeder> compatibleFeeders = getCompatibleFeeders(machine, nozzle, part);
            for (NozzleTip nozzleTip : compatibleNozzleTips) {
                for (Feeder feeder : compatibleFeeders) {
                    WeightedPlacementSolution weightedSolution = new WeightedPlacementSolution(
                            solution.placement,
                            solution.boardLocation,
                            nozzle.getHead(),
                            nozzle,
                            nozzleTip,
                            feeder);
                    weightedSolution.weight = 1;
                    weightedSolution.originalSolution = solution;
                    if (nozzle.getNozzleTip() != nozzleTip) {
                        weightedSolution.weight++;
                    }
                    weightedSolutions.add(weightedSolution);
                }
            }
        }
        Collections.sort(weightedSolutions, weightComparator);
        return weightedSolutions;
    }
    
    private static Set<NozzleTip> getCompatibleNozzleTips(Nozzle nozzle, Part part) {
        Set<NozzleTip> nozzleTips = new HashSet<>();
        for (NozzleTip nozzleTip : nozzle.getNozzleTips()) {
            if (nozzleTip.canHandle(part)) {
                nozzleTips.add(nozzleTip);
            }
        }
        return nozzleTips;
    }
    
    private static Set<Feeder> getCompatibleFeeders(Machine machine, Nozzle nozzle, Part part) {
        Set<Feeder> feeders = new HashSet<>();
        for (Feeder feeder : machine.getFeeders()) {
            if (feeder.getPart() == part && feeder.isEnabled()) {
                feeders.add(feeder);
            }
        }
        return feeders;
    }
    
    static Comparator<WeightedPlacementSolution> weightComparator = new Comparator<WeightedPlacementSolution>() {
        @Override
        public int compare(WeightedPlacementSolution o1, WeightedPlacementSolution o2) {
            return Double.compare(o1.weight, o2.weight);
        }
    };

    static class WeightedPlacementSolution extends PlacementSolution {
        public double weight;
        public PlacementSolution originalSolution;
        
        public WeightedPlacementSolution(Placement placement,
                BoardLocation boardLocation, Head head, Nozzle nozzle,
                NozzleTip nozzleTip, Feeder feeder) {
            super(placement, boardLocation, head, nozzle, nozzleTip, feeder);
        }
    }
}
