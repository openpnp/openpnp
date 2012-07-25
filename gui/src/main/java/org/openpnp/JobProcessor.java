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

package org.openpnp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openpnp.model.Board;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Job;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.openpnp.model.Point;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.util.Utils2D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO Safe Z should be a Job property, and the user should be able to set it during job setup to be as low as
// possible to make things faster.
public class JobProcessor implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(JobProcessor.class);
	
	public enum JobState {
		Stopped,
		Running,
		Paused,
	}
	
	public enum JobError {
		MachineHomingError,
		MachineMovementError,
		MachineRejectedJobError,
		FeederError,
		HeadError,
		PickError,
		PlaceError,
	}
	
	public enum PickRetryAction {
		RetryWithFeed,
		RetryWithoutFeed,
		SkipAndContinue,
	}
	
	private Configuration configuration;
	private Job job;
	private Set<JobProcessorListener> listeners = new HashSet<JobProcessorListener>();
	private JobProcessorDelegate delegate = new DefaultJobProcessorDelegate();
	private JobState state;
	private Thread thread;
	private Object runLock = new Object();
	
	private boolean pauseAtNextStep;
	
	public JobProcessor(Configuration configuration) {
		this.configuration = configuration;
	}
	
	public void setDelegate(JobProcessorDelegate delegate) {
		this.delegate = delegate;
	}
	
	public void addListener(JobProcessorListener listener) {
		listeners.add(listener);
	}
	
	public void removeListener(JobProcessorListener listener) {
		listeners.remove(listener);
	}
	
	public Job getJob() {
		return job;
	}
	
	public JobState getState() {
		return state;
	}
	
	// TODO: Change this, and most of the other properties on here to bound
	// properties.
	public void load(Job job) {
		stop();
		this.job = job;
		
		fireJobLoaded();
	}
	
	/**
	 * Start the Job. The Job must be in the Stopped state.
	 */
	public void start() throws Exception {
		logger.debug("start()");
		if (state != JobState.Stopped) {
			throw new Exception("Invalid state. Cannot start new job while state is " + state);
		}
		if (thread != null && thread.isAlive()) {
			throw new Exception("Previous Job has not yet finished.");
		}
		thread = new Thread(this);
		thread.start();
	}
	
	/**
	 * Pause a running Job. The Job will stop running at the next opportunity and retain
	 * it's state so that it can be resumed. 
	 */
	public void pause() {
		logger.debug("pause()");
		state = JobState.Paused;
		fireJobStateChanged();
	}
	
	/**
	 * Advances the Job one step. If the Job is not currently started this will
	 * start the Job first.
	 * @throws Exception
	 */
	public void step() throws Exception {
		logger.debug("step()");
		if (state == JobState.Stopped) {
			pauseAtNextStep = true;
			start();
		}
		else {
			pauseAtNextStep = true;
			resume();
		}
	}
	
	/**
	 * Resume a running Job. The Job will resume from where it was paused.
	 */
	public void resume() {
		logger.debug("resume()");
		state = JobState.Running;
		fireJobStateChanged();
		synchronized (runLock) {
			runLock.notifyAll();
		}
	}
	
	/**
	 * Stop a running Job. The Job will stop immediately and will reset to it's 
	 * freshly loaded state. All state about parts already placed will be lost.
	 */
	public void stop() {
		logger.debug("stop()");
		state = JobState.Stopped;
		fireJobStateChanged();
		synchronized (runLock) {
			runLock.notifyAll();
		}
	}
	
	// TODO: shouldn't all the job errors return instead of continuing?
	public void run() {
		state = JobState.Running;
		fireJobStateChanged();
		
		Machine machine = configuration.getMachine();

		/*
		 * Vision: After the Head.pick() operation is when we might do some
		 * vision work. We can use bottom vision, or maybe even top, to
		 * determine how well we picked the part and then adjust internally the
		 * offset we are holding it at.
		 * 
		 * Optimizations: Build a hit list up front of all the movements that
		 * have to be made along with substitution options such as different
		 * feeders and heads for parts, then sort the whole mess by distance and
		 * determine the best route.
		 * 
		 * How do we handle Heads working in tandem? Need multiple threads, or
		 * async Head operations.
		 */
		for (BoardLocation jobBoard : job.getBoardLocations()) {
			Board board = jobBoard.getBoard();
			fireBoardProcessingStarted(jobBoard);
			for (Placement placement : board.getPlacements()) {
				firePartProcessingStarted(jobBoard, placement);
				Part part = placement.getPart();

				// Get a list of Feeders that can source the part
				List<Feeder> feeders = new ArrayList<Feeder>();
				for (Feeder feeder : machine.getFeeders()) {
					// TODO: currently passing null till we determine if we'll pass head or change this system
					if (feeder.getPart() == part && feeder.canFeedForHead(null) && feeder.isEnabled()) {
						feeders.add(feeder);
					}
				}
				
				if (feeders.size() < 1) {
					fireJobEncounteredError(JobError.FeederError, "No viable Feeders found for Part " + part.getId());
				}

				// TODO Loop through this next block checking Feeders against
				// Heads until one works.

				// Determine which feeder to pick the part from. This can be
				// optimized to handle
				// distance, capacity, etc.
				// For now we just take the first Feeder that can feed the part.
				Feeder feeder = feeders.get(0);

				// Get the Location of the pick point from the feeder
				Location pickLocation = feeder.getLocation();

				// Convert the Location to the machine's native units if
				// necessary
				pickLocation = pickLocation.convertToUnits(machine.getNativeUnits());

				// Determine where we will place the part
				Location boardLocation = jobBoard.getLocation();
				Location placementLocation = placement.getLocation();

				// Convert the locations to machine native units
				boardLocation = boardLocation.convertToUnits(machine.getNativeUnits());
				placementLocation = placementLocation.convertToUnits(machine.getNativeUnits());

				// Create the point that represents the final placement location
				// TODO This is currently in relation to the 0,0 of the board
				Point p = new Point(placementLocation.getX(),
						placementLocation.getY());

				// Rotate and translate the point into the same coordinate space
				// as the board
				p = Utils2D.rotateTranslateScalePoint(p, boardLocation
						.getRotation(), boardLocation.getX(), boardLocation
						.getY(), 1.0, 1.0);

				// Update the placementLocation with the transformed point
				placementLocation.setX(p.getX());
				placementLocation.setY(p.getY());

				// Update the placementLocation with the board's rotation and
				// the placement's rotation
				// This sets the rotation of the part itself when it will be
				// placed
				placementLocation
						.setRotation((placementLocation.getRotation() + boardLocation
								.getRotation()) % 360.0);

				// Update the placementLocation with the proper Z value. This is
				// the distance to the top of the board plus the height of 
				// the part.
				double partHeight = part.getHeight().convertToUnits(machine.getNativeUnits()).getValue();
				placementLocation.setZ(boardLocation.getZ() + partHeight);

				// At this point the important data is pickLocation along with
				// it's rotation, which determines
				// where we will obtain the part and placementLocation and it's
				// rotation plus board rotation,
				// which determines where we will place the part

				// Get a list of Heads that can service both the pick and place
				// operations. This is not
				// always going to be # of heads because some heads may be
				// limited in their ability to reach
				// certain parts of the machine.
				List<Head> candidateHeads = machine.getHeads();
				List<Head> heads = new ArrayList<Head>();
				for (Head candidateHead : candidateHeads) {
					if (candidateHead.canPickAndPlace(feeder, pickLocation,
							placementLocation)) {
						heads.add(candidateHead);
					}
				}

				if (heads.size() < 1) {
					fireJobEncounteredError(JobError.HeadError, "No Head available to service Placement " + placement);
				}

				// Choose the Head that will service the operation, can be
				// optimized
				// Currently we just take the first available Head
				Head head = heads.get(0);

				fireDetailedStatusUpdated(String.format("Move to safe Z at (X %2.3f, Y %2.3f, Z %2.3f, C %2.3f).", head.getX(), head.getY(), (double) 0, head.getC()));		

				if (!shouldJobProcessingContinue()) {
					return;
				}

				try {
					head.moveToSafeZ();
				}
				catch (Exception e) {
					fireJobEncounteredError(JobError.MachineMovementError, e.getMessage());
				}

				// TODO: Need to be able to see the thing that caused an error, but we also want to see what is about to happen when paused. Figure it out.
				fireDetailedStatusUpdated(String.format("Request part feed from feeder %s.", feeder.getId()));
				
				if (!shouldJobProcessingContinue()) {
					return;
				}

				// Request that the Feeder feeds the part
				try {
					pickLocation = feeder.feed(head, pickLocation);
				}
				catch (Exception e) {
					fireJobEncounteredError(JobError.FeederError, e.getMessage());
				}

				fireDetailedStatusUpdated(String.format("Move to safe Z at (X %2.3f, Y %2.3f, Z %2.3f, C %2.3f).", head.getX(), head.getY(), (double) 0, head.getC()));
				
				if (!shouldJobProcessingContinue()) {
					return;
				}

				try {
					head.moveToSafeZ();
				}
				catch (Exception e) {
					fireJobEncounteredError(JobError.MachineMovementError, e.getMessage());
				}

				fireDetailedStatusUpdated(String.format("Move to pick location, safe Z at (X %2.3f, Y %2.3f, Z %2.3f, C %2.3f).", pickLocation.getX(), pickLocation.getY(), (double) 0, pickLocation.getRotation()));

				if (!shouldJobProcessingContinue()) {
					return;
				}
				
				// Move the nozzle to the pick Location at safe Z
				try {
					head.moveTo(pickLocation.getX(), pickLocation.getY(), head.getZ(), pickLocation.getRotation());
				}
				catch (Exception e) {
					fireJobEncounteredError(JobError.MachineMovementError, e.getMessage());
				}

				fireDetailedStatusUpdated(String.format("Request part pick at (X %2.3f, Y %2.3f, Z %2.3f, C %2.3f).", pickLocation.getX(), pickLocation.getY(), pickLocation.getZ(), pickLocation.getRotation()));

				if (!shouldJobProcessingContinue()) {
					return;
				}
				
				// Pick the part
				try {
					// TODO design a way for the head/feeder to indicate that the part
					// failed to pick, use the delegate to notify and potentially retry
					// We now have the delegate for this, just need to use it and 
					// implement the logic for it's potential responses
					head.pick(part, feeder, pickLocation);
				}
				catch (Exception e) {
					fireJobEncounteredError(JobError.PickError, e.getMessage());
				}
				
				firePartPicked(jobBoard, placement);

				fireDetailedStatusUpdated(String.format("Move to safe Z at (X %2.3f, Y %2.3f, Z %2.3f, C %2.3f).", head.getX(), head.getY(), (double) 0, head.getC()));

				if (!shouldJobProcessingContinue()) {
					return;
				}

				try {
					head.moveToSafeZ();
				}
				catch (Exception e) {
					fireJobEncounteredError(JobError.MachineMovementError, e.getMessage());
				}

				fireDetailedStatusUpdated(String.format("Move to placement location, safe Z at (X %2.3f, Y %2.3f, Z %2.3f, C %2.3f).", 
						placementLocation.getX(), 
						placementLocation.getY(), 
						(double) 0, 
						placementLocation.getRotation()));

				if (!shouldJobProcessingContinue()) {
					return;
				}

				// Move the nozzle to the placement Location at safe Z
				try {
					head.moveTo(placementLocation.getX(), placementLocation.getY(), head.getZ(), placementLocation.getRotation());
				}
				catch (Exception e) {
					fireJobEncounteredError(JobError.MachineMovementError, e.getMessage());
				}

				fireDetailedStatusUpdated(String.format("Request part place. at (X %2.3f, Y %2.3f, Z %2.3f, C %2.3f).", 
						placementLocation.getX(), 
						placementLocation.getY(), 
						placementLocation.getZ(), 
						placementLocation.getRotation()));

				if (!shouldJobProcessingContinue()) {
					return;
				}
				
				// Place the part
				try {
					head.place(part, placementLocation);
				}
				catch (Exception e) {
					fireJobEncounteredError(JobError.PlaceError, e.getMessage());
				}
				
				firePartPlaced(jobBoard, placement);
				
				fireDetailedStatusUpdated(String.format("Move to safe Z at (X %2.3f, Y %2.3f, Z %2.3f, C %2.3f).", head.getX(), head.getY(), (double) 0, head.getC()));		

				if (!shouldJobProcessingContinue()) {
					return;
				}

				try {
					head.moveToSafeZ();
				}
				catch (Exception e) {
					fireJobEncounteredError(JobError.MachineMovementError, e.getMessage());
				}
				
				firePartProcessingComplete(jobBoard, placement);
			}
			
			fireBoardProcessingCompleted(jobBoard);
		}
		
		fireDetailedStatusUpdated("Job complete.");
		
		state = JobState.Stopped;
		fireJobStateChanged();
	}
	
	/**
	 * Checks if the Job has been Paused or Stopped. If it has been Paused this method
	 * blocks until the Job is Resumed. If the Job has been Stopped it returns false and
	 * the loop should break.
	 */
	private boolean shouldJobProcessingContinue() {
		if (pauseAtNextStep) {
			pauseAtNextStep = false;
			pause();
		}
		while (true) {
			if (state == JobState.Stopped) {
				return false;
			}
			else if (state == JobState.Paused) {
				synchronized (runLock) {
					try {
						runLock.wait();
					}
					catch (InterruptedException ie) {
						throw new Error(ie);
					}
				}
			}
			else {
				break;
			}
		}
		return true;
	}
	
	private void fireJobEncounteredError(JobError error, String description) {
		logger.debug("fireJobEncounteredError({}, {})", error, description);
		for (JobProcessorListener listener : listeners) {
			listener.jobEncounteredError(error, description);
		}
	}
	
	private void fireJobLoaded() {
		logger.debug("fireJobLoaded()");
		for (JobProcessorListener listener : listeners) {
			listener.jobLoaded(job);
		}
	}
	
	private void fireJobStateChanged() {
		logger.debug("fireJobStateChanged({})", state);
		for (JobProcessorListener listener : listeners) {
			listener.jobStateChanged(state);
		}
	}
	
	private void fireBoardProcessingStarted(BoardLocation board) {
		logger.debug("fireBoardProcessingStarted({})", board);
		for (JobProcessorListener listener : listeners) {
			listener.boardProcessingStarted(board);
		}
	}
	
	private void fireBoardProcessingCompleted(BoardLocation board) {
		logger.debug("fireBoardProcessingCompleted({})", board);
		for (JobProcessorListener listener : listeners) {
			listener.boardProcessingCompleted(board);
		}
	}
	
	private void firePartProcessingStarted(BoardLocation board, Placement placement) {
		logger.debug("firePartProcessingStarted({}, {})", board, placement);
		for (JobProcessorListener listener : listeners) {
			listener.partProcessingStarted(board, placement);
		}
	}
	
	private void firePartPicked(BoardLocation board, Placement placement) {
		logger.debug("firePartPicked({}, {})", board, placement);
		for (JobProcessorListener listener : listeners) {
			listener.partPicked(board, placement);
		}
	}
	
	private void firePartPlaced(BoardLocation board, Placement placement) {
		logger.debug("firePartPlaced({}, {})", board, placement);
		for (JobProcessorListener listener : listeners) {
			listener.partPlaced(board, placement);
		}
	}
	
	private void firePartProcessingComplete(BoardLocation board, Placement placement) {
		logger.debug("firePartProcessingComplete({}, {})", board, placement);
		for (JobProcessorListener listener : listeners) {
			listener.partProcessingCompleted(board, placement);
		}
	}
	
	private void fireDetailedStatusUpdated(String status) {
		logger.debug("fireDetailedStatusUpdated({})", status);
		for (JobProcessorListener listener : listeners) {
			listener.detailedStatusUpdated(status);
		}
	}
	
	class DefaultJobProcessorDelegate implements JobProcessorDelegate {
		@Override
		public PickRetryAction partPickFailed(BoardLocation board, Part part,
				Feeder feeder) {
			return PickRetryAction.SkipAndContinue;
		}
	}
}
