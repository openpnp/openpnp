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
	
	public void boardProcessingStarted(BoardLocation board);
	
	public void boardProcessingCompleted(BoardLocation board);
	
	public void partProcessingStarted(BoardLocation board, Placement placement);
	
	public void partPicked(BoardLocation board, Placement placement);
	
	public void partPlaced(BoardLocation board, Placement placement);
	
	public void partProcessingCompleted(BoardLocation board, Placement placement);
	
	// TODO maybe partProcessingFailed with a reason
	
	public void detailedStatusUpdated(String status);
}
