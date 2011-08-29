package org.openpnp;

import org.openpnp.Job.JobBoard;
import org.openpnp.JobProcessor.JobError;
import org.openpnp.JobProcessor.JobState;

/**
 * Allows an interested listener to receive events as the JobProcessor does it's work. Methods in
 * this interface are for passive observers.  
 *
 */
public interface JobProcessorListener {
	public void jobLoaded(Job job);
	
	/**
	 * Indicates that the state of the Job has changed. This is generally in response to
	 * one of the Job control methods such as start(), pause(), resume(), or stop() being
	 * called. 
	 * @param state
	 */
	public void jobStateChanged(JobState state);
	
	public void jobEncounteredError(JobError error, String description);
	
	public void boardProcessingStarted(JobBoard board);
	
	public void boardProcessingCompleted(JobBoard board);
	
	public void partProcessingStarted(JobBoard board, Placement placement);
	
	public void partPicked(JobBoard board, Placement placement);
	
	public void partPlaced(JobBoard board, Placement placement);
	
	public void partProcessingCompleted(JobBoard board, Placement placement);
	
	// TODO maybe partProcessingFailed with a reason
}
