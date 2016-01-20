/*
 	Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 	
 	This file is part of OpenPnP.
 	
	OpenPnP is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenPnP is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenPnP.  If not, see <http://www.gnu.org/licenses/>.
 	
 	For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.machine.reference;

import java.util.Set;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.wizards.ReferenceJobProcessorConfigurationWizard;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.openpnp.planner.SimpleJobPlanner;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;
import org.openpnp.spi.JobPlanner;
import org.openpnp.spi.JobPlanner.PlacementSolution;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.openpnp.spi.VisionProvider;
import org.openpnp.spi.base.AbstractJobProcessor;
import org.openpnp.util.Utils2D;
import org.openpnp.vision.FiducialLocator;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Commit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: Rewrite this as a FSM where each place we would normally check if 
// job should continue is just a state.
// TODO Safe Z should be a Job property, and the user should be able to set it during job setup to be as low as
// possible to make things faster.
public class ReferenceJobProcessor extends AbstractJobProcessor {
	private static final Logger logger = LoggerFactory.getLogger(ReferenceJobProcessor.class);
	
    /**
     * History:
     * 
     * Note: Can't actually use the @Version annotation because of a bug
     * in SimpleXML. See http://sourceforge.net/p/simple/mailman/message/27887562/
     *  
     * 1.0: Initial revision.
     * 1.1: Added jobPlanner, which is moved here from AbstractMachine.
     */
	
    @Attribute(required=false)
    private boolean demoMode;
    
    @Element(required=false)
    private JobPlanner jobPlanner;
    
	public ReferenceJobProcessor() {
	}
	
    @SuppressWarnings("unused")
    @Commit
    private void commit() {
        if (jobPlanner == null) {
            jobPlanner = new SimpleJobPlanner();
        }
    }	
	
	@Override
    public void run() {
	    if (demoMode) {
	        runDemo();
	        return;
	    }
	    
		state = JobState.Running;
		fireJobStateChanged();
		
		Machine machine = Configuration.get().getMachine();
		
		preProcessJob(machine);
		
		for (Head head : machine.getHeads()) {
			fireDetailedStatusUpdated(String.format("Move head %s to Safe-Z.", head.getName()));		
	
			if (!shouldJobProcessingContinue()) {
				return;
			}
	
			try {
				head.moveToSafeZ(1.0);
			}
			catch (Exception e) {
				fireJobEncounteredError(JobError.MachineMovementError, e.getMessage());
				return;
			}
		}
		
        fireDetailedStatusUpdated(String.format("Check fiducials."));        
        
        if (!shouldJobProcessingContinue()) {
            return;
        }

        try {
            checkFiducials();
        }
        catch (Exception e) {
            fireJobEncounteredError(JobError.MachineMovementError, e.getMessage());
            return;
        }
		
		Head head = machine.getHeads().get(0);
		
		jobPlanner.setJob(job);

        Set<PlacementSolution> solutions;
		while ((solutions = jobPlanner.getNextPlacementSolutions(head)) != null) {
		    for (PlacementSolution solution : solutions) {
                BoardLocation bl = solution.boardLocation;
                Part part = solution.placement.getPart();
                Feeder feeder = solution.feeder;
                Placement placement = solution.placement;
                Nozzle nozzle = solution.nozzle;
                NozzleTip nozzleTip = solution.nozzleTip;
                
                firePartProcessingStarted(solution.boardLocation, solution.placement);
                
                if (!changeNozzleTip(nozzle, nozzleTip)) {
                    return;
                }
								
				if (!nozzle.getNozzleTip().canHandle(part)) {
                    fireJobEncounteredError(JobError.PickError, "Selected nozzle tip is not compatible with part");
                    return;
				}
				
				if (!pick(nozzle, feeder, bl, placement)) {
				    return;
				}
			}
		    
            // TODO: a lot of the event fires are broken
		    for (PlacementSolution solution : solutions) {
                Nozzle nozzle = solution.nozzle;
                BoardLocation bl = solution.boardLocation;
                Placement placement = solution.placement;
                Part part = placement.getPart();
                
                fireDetailedStatusUpdated(String.format("Perform bottom vision"));      
                
                if (!shouldJobProcessingContinue()) {
                    return;
                }
                
                Location bottomVisionOffsets;
                try {
                    bottomVisionOffsets = performBottomVision(machine, part, nozzle);
                }
                catch (Exception e) {
                    fireJobEncounteredError(JobError.PartError, e.getMessage());
                    return;
                }
                
                Location placementLocation = placement.getLocation();
                if (bottomVisionOffsets != null) {
                    placementLocation = placementLocation.subtractWithRotation(bottomVisionOffsets);
                }
                placementLocation = 
                        Utils2D.calculateBoardPlacementLocation(bl.getLocation(), bl.getSide(), placementLocation);

                // Update the placementLocation with the proper Z value. This is
                // the distance to the top of the board plus the height of 
                // the part.
                Location boardLocation = bl.getLocation().convertToUnits(placementLocation.getUnits());
                double partHeight = part.getHeight().convertToUnits(placementLocation.getUnits()).getValue();
                placementLocation = placementLocation.derive(null, null, boardLocation.getZ() + partHeight, null);

                if (!place(nozzle, bl, placementLocation, placement)) {
                    return;
                }
            }
		}
		
		fireDetailedStatusUpdated("Job complete.");
		
		state = JobState.Stopped;
		fireJobStateChanged();
	}
	
    // TODO: This needs to be it's own class and the job processor needs to
	// be more abstract. Then we can have job processors that process
	// job types like demo, pnp, solder, etc.
	private void runDemo() {
        state = JobState.Running;
        fireJobStateChanged();
        
        Machine machine = Configuration.get().getMachine();
    
        preProcessJob(machine);
        
        for (Head head : machine.getHeads()) {
            fireDetailedStatusUpdated(String.format("Move head %s to Safe-Z.", head.getName()));        
    
            if (!shouldJobProcessingContinue()) {
                return;
            }
    
            try {
                head.moveToSafeZ(1.0);
            }
            catch (Exception e) {
                fireJobEncounteredError(JobError.MachineMovementError, e.getMessage());
                return;
            }
        }
        
        fireDetailedStatusUpdated(String.format("Check fiducials."));        
        
        if (!shouldJobProcessingContinue()) {
            return;
        }

        try {
            checkFiducials();
        }
        catch (Exception e) {
            fireJobEncounteredError(JobError.MachineMovementError, e.getMessage());
            return;
        }
        
        Head head;
        Camera camera;
        
        try {
            head = machine.getDefaultHead();
            camera = head.getDefaultCamera();
        }
        catch (Exception e) {
            fireJobEncounteredError(JobError.HeadError, e.getMessage());
            return;
        }
        
        jobPlanner.setJob(job);
                
        Set<PlacementSolution> solutions;
        while ((solutions = jobPlanner.getNextPlacementSolutions(head)) != null) {
            for (PlacementSolution solution : solutions) {
                Feeder feeder = solution.feeder;
                
                firePartProcessingStarted(solution.boardLocation, solution.placement);
                
                try {
                    fireDetailedStatusUpdated(String.format("Move to pick location, safe Z at (%s).", feeder.getPickLocation()));
                }
                catch (Exception e) {
                    fireJobEncounteredError(JobError.MachineMovementError, e.getMessage());
                    return;
                }

                if (!shouldJobProcessingContinue()) {
                    return;
                }

                try {
                    camera.moveTo(feeder.getPickLocation().derive(null, null, Double.NaN, null), 1.0);
                    camera.dwell(750);
                }
                catch (Exception e) {
                    fireJobEncounteredError(JobError.MachineMovementError, e.getMessage());
                    return;
                }
            }
            
            // TODO: a lot of the event fires are broken
            for (PlacementSolution solution : solutions) {
                BoardLocation bl = solution.boardLocation;
                Placement placement = solution.placement;

                Location placementLocation = placement.getLocation();
                placementLocation = 
                        Utils2D.calculateBoardPlacementLocation(bl.getLocation(), bl.getSide(), placementLocation);
                
                fireDetailedStatusUpdated(String.format("Move to placement location, safe Z at (%s).", placementLocation));

                if (!shouldJobProcessingContinue()) {
                    return;
                }

                try {
                    camera.moveTo(placementLocation.derive(null, null, Double.NaN, null), 1.0);
                    camera.dwell(750);
                }
                catch (Exception e) {
                    fireJobEncounteredError(JobError.MachineMovementError, e.getMessage());
                    return;
                }
            }
        }
        
        fireDetailedStatusUpdated("Job complete.");
        
        state = JobState.Stopped;
        fireJobStateChanged();
    }
    
    // TODO: Should not bail if there are no fids on the board. Figure out
    // the UI for that.
    protected void checkFiducials() throws Exception {
        FiducialLocator locator = new FiducialLocator();
        for (BoardLocation boardLocation : job.getBoardLocations()) {
            if (!boardLocation.isCheckFiducials()) {
                continue;
            }
            Location location = locator.locateBoard(boardLocation);
            boardLocation.setLocation(location);
        }
    }
	
	protected Location performBottomVision(Machine machine, Part part, Nozzle nozzle) throws Exception {
	    // TODO: I think this stuff actually belongs in VisionProvider but
	    // I have not yet fully thought through the API.
	    
	    // Find the first fixed camera
	    if (machine.getCameras().isEmpty()) {
	        // TODO: return null for now to indicate that no vision was
	        // calculated. In the future we may want this to be based on
	        // configuration.
	        return null;
	    }
	    Camera camera = machine.getCameras().get(0);
	    
	    // Get it's vision provider
	    VisionProvider vp = camera.getVisionProvider();
	    if (vp == null) {
            // TODO: return null for now to indicate that no vision was
            // calculated. In the future we may want this to be based on
            // configuration.
            return null;
	    }
	    
	    // Perform the operation. Note that similar to feeding and nozzle
	    // tip changing, it is up to the VisionProvider to move the camera
	    // and nozzle to where it needs to be.
	    return vp.getPartBottomOffsets(part, nozzle);
	}
	
	protected boolean changeNozzleTip(Nozzle nozzle, NozzleTip nozzleTip) {
        // NozzleTip Changer
        if (nozzle.getNozzleTip() != nozzleTip) {
            fireDetailedStatusUpdated(String.format("Unload nozzle tip from nozzle %s.", nozzle.getName()));        

            if (!shouldJobProcessingContinue()) {
                return false;
            }
            
            try {
                nozzle.unloadNozzleTip();
            }
            catch (Exception e) {
                fireJobEncounteredError(JobError.PickError, e.getMessage());
                return false;
            }
            
            fireDetailedStatusUpdated(String.format("Load nozzle tip %s into nozzle %s.", nozzleTip.getName(), nozzle.getName()));        

            if (!shouldJobProcessingContinue()) {
                return false;
            }
                                
            try {
                nozzle.loadNozzleTip(nozzleTip);
            }
            catch (Exception e) {
                fireJobEncounteredError(JobError.PickError, e.getMessage());
                return false;
            }
            
            if (nozzle.getNozzleTip() != nozzleTip) {
                fireJobEncounteredError(JobError.PickError, "Failed to load correct nozzle tip");
                return false;
            }
        }
        return true;
        // End NozzleTip Changer
	}
	
	protected boolean pick(Nozzle nozzle, Feeder feeder, BoardLocation bl, Placement placement) {
        fireDetailedStatusUpdated(String.format("Move nozzle %s to Safe-Z at (%s).", nozzle.getName(), nozzle.getLocation()));        

        if (!shouldJobProcessingContinue()) {
            return false;
        }

        try {
            nozzle.moveToSafeZ(1.0);
        }
        catch (Exception e) {
            fireJobEncounteredError(JobError.MachineMovementError, e.getMessage());
            return false;
        }

        // TODO: Need to be able to see the thing that caused an error, but we also want to see what is about to happen when paused. Figure it out.
        fireDetailedStatusUpdated(String.format("Request part feed from feeder %s.", feeder.getName()));
        
        if (!shouldJobProcessingContinue()) {
            return false;
        }

        // Request that the Feeder feeds the part
        while (true) {
        	if (!shouldJobProcessingContinue()) {
        		return false;
        	}
            try {
                feeder.feed(nozzle);
                break;
            }
            catch (Exception e) {
                fireJobEncounteredError(JobError.FeederError, e.getMessage());
            }
        }
        
        // Now that the Feeder has done it's feed operation we can get
        // the pick location from it.
        Location pickLocation;
        try {
            pickLocation = feeder.getPickLocation();
        }
        catch (Exception e) {
            fireJobEncounteredError(JobError.FeederError, e.getMessage());
            return false;
        }

        fireDetailedStatusUpdated(String.format("Move to safe Z at (%s).", nozzle.getLocation()));
        
        if (!shouldJobProcessingContinue()) {
            return false;
        }

        try {
            nozzle.moveToSafeZ(1.0);
        }
        catch (Exception e) {
            fireJobEncounteredError(JobError.MachineMovementError, e.getMessage());
            return false;
        }

        fireDetailedStatusUpdated(String.format("Move to pick location, safe Z at (%s).", pickLocation));

        if (!shouldJobProcessingContinue()) {
            return false;
        }
        
        // Move the Nozzle to the pick Location at safe Z
        try {
            nozzle.moveTo(pickLocation.derive(null, null, Double.NaN, null), 1.0);
        }
        catch (Exception e) {
            fireJobEncounteredError(JobError.MachineMovementError, e.getMessage());
            return false;
        }

        fireDetailedStatusUpdated(String.format("Move to pick location Z at (%s).", pickLocation));

        if (!shouldJobProcessingContinue()) {
            return false;
        }

        // Move the Nozzle to the pick Location 
        try {
            nozzle.moveTo(pickLocation, 1.0);
        }
        catch (Exception e) {
            fireJobEncounteredError(JobError.MachineMovementError, e.getMessage());
            return false;
        }

        fireDetailedStatusUpdated(String.format("Request part pick at (%s).", pickLocation));

        if (!shouldJobProcessingContinue()) {
            return false;
        }
        
        // Pick the part
        try {
            // TODO design a way for the head/feeder to indicate that the part
            // failed to pick, use the delegate to notify and potentially retry
            // We now have the delegate for this, just need to use it and 
            // implement the logic for it's potential responses
            nozzle.pick();
        }
        catch (Exception e) {
            fireJobEncounteredError(JobError.PickError, e.getMessage());
            return false;
        }
        
        firePartPicked(bl, placement);

        fireDetailedStatusUpdated(String.format("Move to safe Z at (%s).", nozzle.getLocation()));

        if (!shouldJobProcessingContinue()) {
            return false;
        }

        try {
            nozzle.moveToSafeZ(1.0);
        }
        catch (Exception e) {
            fireJobEncounteredError(JobError.MachineMovementError, e.getMessage());
            return false;
        }
        
        return true;
	}
	
	protected boolean place(Nozzle nozzle, BoardLocation bl, Location placementLocation, Placement placement) {
        fireDetailedStatusUpdated(String.format("Move to placement location, safe Z at (%s).", placementLocation));

        if (!shouldJobProcessingContinue()) {
            return false;
        }

        // Move the nozzle to the placement Location at safe Z
        try {
            nozzle.moveTo(placementLocation.derive(null, null, Double.NaN, null), 1.0);
        }
        catch (Exception e) {
            fireJobEncounteredError(JobError.MachineMovementError, e.getMessage());
            return false;
        }

        fireDetailedStatusUpdated(String.format("Move to placement location Z at (%s).", placementLocation));

        if (!shouldJobProcessingContinue()) {
            return false;
        }

        // Lower the nozzle.
        try {
            nozzle.moveTo(placementLocation, 1.0);
        }
        catch (Exception e) {
            fireJobEncounteredError(JobError.MachineMovementError, e.getMessage());
            return false;
        }

        fireDetailedStatusUpdated(String.format("Request part place. at (X %2.3f, Y %2.3f, Z %2.3f, C %2.3f).", 
                placementLocation.getX(), 
                placementLocation.getY(), 
                placementLocation.getZ(), 
                placementLocation.getRotation()));

        if (!shouldJobProcessingContinue()) {
            return false;
        }
        
        // Place the part
        try {
            nozzle.place();
        }
        catch (Exception e) {
            fireJobEncounteredError(JobError.PlaceError, e.getMessage());
            return false;
        }
        
        firePartPlaced(bl, placement);
        
        fireDetailedStatusUpdated(String.format("Move to safe Z at (%s).", nozzle.getLocation()));      

        if (!shouldJobProcessingContinue()) {
            return false;
        }

        // Return to Safe-Z above the board. 
        try {
            nozzle.moveToSafeZ(1.0);
        }
        catch (Exception e) {
            fireJobEncounteredError(JobError.MachineMovementError, e.getMessage());
            return false;
        }
        
        firePartProcessingComplete(bl, placement);
        
        return true;
	}
	
	/*
	 * Pre-process the Job. We will:
	 * 	Look for setup errors.
	 * 	Look for missing parts.
	 * 	Look for missing feeders.
	 * 	Look for feeders that cannot feed the number of parts that
	 * 		will be needed.
	 * 	Calculate the base Safe-Z for the job.
	 * 	Calculate the number of parts that need to be placed.
	 * 	Calculate the total distance that will need to be traveled.
	 * 	Calculate the total time it should take to place the job.
	 * 
	 * Time calculation is tough unless we also ask the feeders to simulate
	 * their work. Otherwise we can just calculate the total distance *
	 * the feed rate to get close. This doesn't include acceleration and
	 * such. 
	 * 
	 * The base Safe-Z is the maximum of:
	 * 		Highest placement location.
	 * 		Highest pick location.
	 */
	protected void preProcessJob(Machine machine) {
        Head head = machine.getHeads().get(0);
        
        jobPlanner.setJob(job);

        Set<PlacementSolution> solutions;
        while ((solutions = jobPlanner.getNextPlacementSolutions(head)) != null) {
            for (PlacementSolution solution : solutions) {
                BoardLocation bl = solution.boardLocation;
                Part part = solution.placement.getPart();
                Feeder feeder = solution.feeder;
                Placement placement = solution.placement;
                Nozzle nozzle = solution.nozzle;
                NozzleTip nozzleTip = solution.nozzleTip;
	    
                if (part == null) {
                    fireJobEncounteredError(JobError.PartError, String.format("Part not found for Board %s, Placement %s", bl.getBoard().getName(), placement.getId()));
                    return;
                }

                if (nozzle == null) {
                    fireJobEncounteredError(JobError.HeadError, "No Nozzle available to service Placement " + placement);
                    return;
                }
        
                if (feeder == null) {
                    fireJobEncounteredError(JobError.FeederError, "No viable Feeders found for Part " + part.getId());
                    return;
                }
        
                if (nozzleTip == null) {
                    fireJobEncounteredError(JobError.HeadError, "No viable NozzleTips found for Part / Feeder " + part.getId());
                    return;
                }
				
			}
		}
	}
	
    public boolean isDemoMode() {
        return demoMode;
    }

    public void setDemoMode(boolean demoMode) {
        this.demoMode = demoMode;
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceJobProcessorConfigurationWizard(this);
    }
}
