package org.openpnp.model;


/**
 * Stores state specific to a Job, Board, and Placement. This is used to
 * maintain state during the running of a Job.
 */
public class JobPlacement extends AbstractModelObject {
    public enum JobPlacementState {
        Ready,
        Processing,
        Skipped,
        Failed,
    }
    
    private BoardLocation boardLocation;
    private Placement placement;
    private JobPlacementState state = JobPlacementState.Ready;
    
    public BoardLocation getBoardLocation() {
        return boardLocation;
    }

    public void setBoardLocation(BoardLocation boardLocation) {
        this.boardLocation = boardLocation;
    }
    
    public Placement getPlacement() {
        return placement;
    }
    
    public void setPlacement(Placement placement) {
        this.placement = placement;
    }
    
    public JobPlacementState getState() {
        return state;
    }
    
    public void setState(JobPlacementState state) {
        this.state = state;
    }
}
