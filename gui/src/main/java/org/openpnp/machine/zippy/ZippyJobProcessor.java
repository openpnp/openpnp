/*
 	Copyright (C) 2013 Richard Spelling <openpnp@chebacco.com>
 	
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


package org.openpnp.machine.zippy;

import java.util.LinkedHashMap;
import java.util.Set;

import org.openpnp.machine.reference.ReferenceJobProcessor;
import org.openpnp.model.Board;
import org.openpnp.model.Board.Side;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.openpnp.model.Point;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;
import org.openpnp.spi.JobPlanner;
import org.openpnp.spi.JobPlanner.PlacementSolution;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.openpnp.util.Utils2D;

public class ZippyJobProcessor extends ReferenceJobProcessor {
	
	public ZippyJobProcessor() {
	}
	
	/*
	 * Pre-process the Job. We will:
	 * 	Look for setup errors.
	 * 	Look for missing parts.
	 * 	Look for missing feeders.
	 * 	Look for feeders that cannot feed the number of parts that
	 * 		will be needed.
	 *  Look for nozzletips for each part //TODO
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

	@Override
	protected void preProcessJob(Machine machine) {
		Set<PlacementSolution> solutions;
//		Machine machine = configuration.getMachine();
		Head head = machine.getHeads().get(0);
		JobPlanner jobPlanner = machine.getJobPlanner();

		for (BoardLocation jobBoard : job.getBoardLocations()) {
			Board board = jobBoard.getBoard();
			
			for (Placement placement : board.getPlacements()) {
				if (placement.getSide() != jobBoard.getSide()) {
					continue;
				}
				
				Part part = placement.getPart();
				if (part == null) {
					fireJobEncounteredError(JobError.PartError, String.format("Part not found for Board %s, Placement %s", board.getName(), placement.getId()));
					return;
				}
			}	
            // Dry run of solutions to look for errors
			jobPlanner.setJob(job);
			while ((solutions = jobPlanner.getNextPlacementSolutions(head)) != null) {
			    for (PlacementSolution solution : solutions) {
					
					BoardLocation bl = solution.boardLocation;
					Part part = solution.placement.getPart();
					Feeder feeder = solution.feeder;
					Placement placement = solution.placement;
					Nozzle nozzle = solution.nozzle;
					NozzleTip nozzletip = solution.nozzleTip;

					if (nozzle == null) {
	                    fireJobEncounteredError(JobError.HeadError, "No Nozzle available to service Placement " + placement);
	                    return;
	                }
	
					if (nozzletip == null) {
	                    fireJobEncounteredError(JobError.HeadError, "No NozzleTip available to service Placement " + placement);
	                    return;
	                }
	
					if (feeder == null) {
						fireJobEncounteredError(JobError.FeederError, "No viable Feeders found for Part " + part.getId());
						return;
					}
			    }
			}
			
		}
	}
	
	@Override
	public void run() {
		Set<PlacementSolution> solutions;
		Machine machine = Configuration.get().getMachine();
		Head head = machine.getHeads().get(0);
		JobPlanner jobPlanner = machine.getJobPlanner();

		//pre job stuff
		state = JobState.Running;
		fireJobStateChanged();
		try {
			head.home();
			Nozzle nozzle = head.getNozzles().get(0);
			Location l = new Location(LengthUnit.Millimeters, 5.0, 20.0, 1.0, 0.0);
			nozzle.moveTo(l, 1.0);
		}
		catch (Exception e) {
			fireJobEncounteredError(JobError.MachineMovementError, e.getMessage());
			return;
		}
		
		
		//pre-test for we have feeders, etc
		preProcessJob(machine);
		jobPlanner.setJob(job);
        
		while ((solutions = jobPlanner.getNextPlacementSolutions(head)) != null) {
		    LinkedHashMap<PlacementSolution, Location> placementSolutionLocations = new LinkedHashMap<PlacementSolution, Location>();
		    for (PlacementSolution solution : solutions) {
				firePartProcessingStarted(solution.boardLocation, solution.placement);
				
				BoardLocation bl = solution.boardLocation;
				Part part = solution.placement.getPart();
				Feeder feeder = solution.feeder;
				Placement placement = solution.placement;
				Nozzle nozzle = solution.nozzle;
				NozzleTip nozzletip = solution.nozzleTip;

				// Determine where we will place the part
				Location boardLocation = bl.getLocation();
				Location placementLocation = placement.getLocation();

				// We will work in the units of the placementLocation, so convert
				// anything that isn't in those units to it.
				boardLocation = boardLocation.convertToUnits(placementLocation.getUnits());
				
				// If we are placing the bottom of the board we need to invert
				// the placement location.
				if (bl.getSide() == Side.Bottom) {
					placementLocation = placementLocation.invert(true, false, false, false);
				}

				// Create the point that represents the final placement location
				Point p = new Point(placementLocation.getX(),
						placementLocation.getY());

				// Rotate and translate the point into the same coordinate space
				// as the board
				p = Utils2D.rotateTranslateScalePoint(p, boardLocation
						.getRotation(), boardLocation.getX(), boardLocation
						.getY(), 1.0, 1.0);

				// Update the placementLocation with the transformed point
				placementLocation = placementLocation.derive(p.getX(), p.getY(), null, null);

				// Update the placementLocation with the board's rotation and
				// the placement's rotation
				// This sets the rotation of the part itself when it will be
				// placed
				placementLocation = placementLocation.derive(
				        null, 
				        null, 
				        null,
				        (placementLocation.getRotation() + boardLocation.getRotation()) % 360.0);

				// Update the placementLocation with the proper Z value. This is
				// the distance to the top of the board minus the height of 
				// the part.
				double partHeight = part.getHeight().convertToUnits(placementLocation.getUnits()).getValue();
				placementLocation = placementLocation.derive(null, null, boardLocation.getZ() - partHeight, null);

				if( !nozzle.getNozzleTip().getId().equals(nozzletip.getId())){
					try {
						((ZippyNozzleTip) nozzle.getNozzleTip()).unload((ZippyNozzle) nozzle);
						((ZippyNozzleTip) nozzletip).load((ZippyNozzle) nozzle);
						((ZippyNozzleTip) nozzletip).calibrate((ZippyNozzle) nozzle);
					}
					catch (Exception e) {
						fireJobEncounteredError(JobError.MachineMovementError, e.getMessage());
						return;
					}
				}
				pick(nozzle, feeder, bl, placement);
				placementSolutionLocations.put(solution, placementLocation);
			}
		    
            // TODO: a lot of the event fires are broken
		    for (PlacementSolution solution : solutions) {
                Nozzle nozzle = solution.nozzle;
                BoardLocation bl = solution.boardLocation;
                Placement placement = solution.placement;
                Location placementLocation = placementSolutionLocations.get(solution);
                place(nozzle, bl, placementLocation, placement);
            }
		    
		}
		//post job clean up
		state = JobState.Stopped;
		fireJobStateChanged();
		//park
		try {
			head.home();
			Nozzle nozzle = head.getNozzles().get(0);
			Location l = new Location(LengthUnit.Millimeters, 0.0, 0.0, 8.5, 0.0);
			nozzle.moveTo(l, 1.0);
		}
		catch (Exception e) {
			fireJobEncounteredError(JobError.MachineMovementError, e.getMessage());
			return;
		}
	}

}
