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

import java.util.HashSet;
import java.util.Set;

import javax.swing.Action;

import org.openpnp.JobProcessorDelegate;
import org.openpnp.JobProcessorListener;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Footprint;
import org.openpnp.model.Footprint.Pad;
import org.openpnp.model.Job;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;
import org.openpnp.spi.JobProcessor;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.Utils2D;
import org.simpleframework.xml.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReferenceSolderPasteDispenseJobProcessor implements Runnable, JobProcessor {
	private static final Logger logger = LoggerFactory.getLogger(ReferenceSolderPasteDispenseJobProcessor.class);
	
	protected Job job;
	private Set<JobProcessorListener> listeners = new HashSet<JobProcessorListener>();
	private JobProcessorDelegate delegate = new DefaultJobProcessorDelegate();
	protected JobState state;
	private Thread thread;
	private Object runLock = new Object();
	
	private boolean pauseAtNextStep;
	
	@Attribute(required=false)
	private String dummy;
	
	public ReferenceSolderPasteDispenseJobProcessor() {
	}
	
	@Override
    public void setDelegate(JobProcessorDelegate delegate) {
		this.delegate = delegate;
	}
	
	@Override
    public void addListener(JobProcessorListener listener) {
		listeners.add(listener);
	}
	
	@Override
    public void removeListener(JobProcessorListener listener) {
		listeners.remove(listener);
	}
	
	@Override
    public Job getJob() {
		return job;
	}
	
	@Override
    public JobState getState() {
		return state;
	}
	
	// TODO: Change this, and most of the other properties on here to bound
	// properties.
	@Override
    public void load(Job job) {
		stop();
		this.job = job;
		
		fireJobLoaded();
	}

	@Override
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
	
	@Override
    public void pause() {
		logger.debug("pause()");
		state = JobState.Paused;
		fireJobStateChanged();
	}
	
	@Override
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
	
	@Override
    public void resume() {
		logger.debug("resume()");
		state = JobState.Running;
		fireJobStateChanged();
		synchronized (runLock) {
			runLock.notifyAll();
		}
	}
	
	@Override
    public void stop() {
		logger.debug("stop()");
		state = JobState.Stopped;
		fireJobStateChanged();
		synchronized (runLock) {
			runLock.notifyAll();
		}
	}
	
	@Override
    public void run() {
		state = JobState.Running;
		fireJobStateChanged();
		
		Machine machine = Configuration.get().getMachine();
		
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
		
		Head head = machine.getHeads().get(0);
		Nozzle nozzle = head.getNozzles().get(0);

		for (BoardLocation boardLocation : job.getBoardLocations()) {
		    for (Placement placement : boardLocation.getBoard().getPlacements()) {
		        if (placement.getSide() != boardLocation.getSide()) {
		            continue;
		        }
		        if (placement.getType() != Placement.Type.Place) {
		            continue;
		        }
		        Footprint footprint = placement.getPart().getPackage().getFootprint();
		        for (Pad pad : footprint.getPads()) {
		            Location padLocation = new Location(footprint.getUnits(), pad.getX(), pad.getY(), 0, 0);
		            Location placementLocation = placement.getLocation();
		            Location location = placementLocation.add(padLocation);
		            location = Utils2D.calculateBoardPlacementLocation(boardLocation.getLocation(), boardLocation.getSide(), location);

		            fireDetailedStatusUpdated(String.format("Move to pad location, safe Z at (%s).", location));
		            if (!shouldJobProcessingContinue()) {
		                return;
		            }
		            try {
	                    MovableUtils.moveToLocationAtSafeZ(nozzle, location, 1.0);
		            }
		            catch (Exception e) {
		                fireJobEncounteredError(JobError.MachineMovementError, e.getMessage());
		            }
		            
                    fireDetailedStatusUpdated(String.format("Dispense.", location));
                    if (!shouldJobProcessingContinue()) {
                        return;
                    }
                    try {
                        Thread.sleep(250);
                    }
                    catch (Exception e) {
                        fireJobEncounteredError(JobError.MachineMovementError, e.getMessage());
                    }
		        }
		    }
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
	protected boolean shouldJobProcessingContinue() {
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
	
	protected void fireJobEncounteredError(JobError error, String description) {
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
	
	protected void fireJobStateChanged() {
		logger.debug("fireJobStateChanged({})", state);
		for (JobProcessorListener listener : listeners) {
			listener.jobStateChanged(state);
		}
	}
	
	protected void firePartProcessingStarted(BoardLocation board, Placement placement) {
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
	
	protected void fireDetailedStatusUpdated(String status) {
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
	
    @Override
    public Wizard getConfigurationWizard() {
        return null;
    }
    
    
    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {
                new PropertySheetWizardAdapter(getConfigurationWizard())
        };
    }
    
    @Override
    public Action[] getPropertySheetHolderActions() {
        // TODO Auto-generated method stub
        return null;
    }
}
