package org.openpnp.model;

import java.util.HashSet;
import java.util.Set;

import org.openpnp.model.JobPlacement.JobPlacementState;

/**
 * Stores the processing state of a Job while the Job is running. Can also
 * be persisted to resume a Job that was not completed.
 */
public class JobSession extends AbstractModelObject {
    private Job job;
    private Set<JobPlacement> placements = new HashSet<JobPlacement>();
    
    /**
     * Initialize the session with the specified Job.
     * @param job
     */
    public JobSession(Job job) {
        this.job = job;
        for (BoardLocation boardLocation : job.getBoardLocations()) {
            for (Placement placement : boardLocation.getBoard().getPlacements()) {
                JobPlacement jobPlacement = new JobPlacement();
                jobPlacement.setBoardLocation(boardLocation);
                jobPlacement.setPlacement(placement);
                placements.add(jobPlacement);
            }
        }
    }
    
    /**
     * Return the Set of placements that are in the current state.
     * @param state
     * @return
     */
    public Set<JobPlacement> getPlacementsInState(JobPlacementState state) {
        Set<JobPlacement> results = new HashSet<JobPlacement>();
        for (JobPlacement placement : placements) {
            if (placement.getState() == state) {
                results.add(placement);
            }
        }
        return results;
    }
    
    /**
     * Sets the state of all Placements to the given value.
     */
    public void setPlacementState(JobPlacementState state) {
        for (JobPlacement placement : placements) {
            placement.setState(state);
        }
    }
    
    /**
     * Changes any placement states with the fromState to the toState.
     * @param fromState
     * @param toState
     */
    public void changePlacementState(JobPlacementState fromState, JobPlacementState toState) {
        for (JobPlacement placement : placements) {
            if (placement.getState() == fromState) {
                placement.setState(toState);
            }
        }
    }
    
    public Job getJob() {
        return job;
    }
}
