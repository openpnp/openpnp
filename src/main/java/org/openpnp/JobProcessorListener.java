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

import org.openpnp.model.BoardLocation;
import org.openpnp.model.Job;
import org.openpnp.model.Placement;
import org.openpnp.spi.JobProcessor.JobError;
import org.openpnp.spi.JobProcessor.JobState;

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
	
	/**
	 * Fired when the JobProcessor has begun operations that include the
	 * given Placement.
	 * @param board
	 * @param placement
	 */
	public void partProcessingStarted(BoardLocation board, Placement placement);
	
	/**
	 * Fired when the JobProcessor has completed the pick operation for the
	 * given Placement.
	 * @param board
	 * @param placement
	 */
	public void partPicked(BoardLocation board, Placement placement);
	
	/**
	 * Fired when the JobProcessor has completed the place operation for the
	 * given Placement.
	 * @param board
	 * @param placement
	 */
	public void partPlaced(BoardLocation board, Placement placement);
	
	/**
	 * Fired when the JobProcessor has completed all operations regarding the
	 * given Placement.
	 * @param board
	 * @param placement
	 */
	public void partProcessingCompleted(BoardLocation board, Placement placement);
	
	// TODO Maybe partProcessingFailed with a reason
	
	// TODO Add job progress information, especially after pre-processing
	// so that listeners can know the total placement count to be processed.
	
	/**
	 * Fired when the JobProcessor is able to report detailed, human readable
	 * status information about the Job's progress.
	 * @param status
	 */
	public void detailedStatusUpdated(String status);
	
	static public class Adapter implements JobProcessorListener {

		@Override
		public void jobLoaded(Job job) {
		}

		@Override
		public void jobStateChanged(JobState state) {
		}

		@Override
		public void jobEncounteredError(JobError error, String description) {
		}

		@Override
		public void partProcessingStarted(BoardLocation board,
				Placement placement) {
		}

		@Override
		public void partPicked(BoardLocation board, Placement placement) {
		}

		@Override
		public void partPlaced(BoardLocation board, Placement placement) {
		}

		@Override
		public void partProcessingCompleted(BoardLocation board,
				Placement placement) {
		}

		@Override
		public void detailedStatusUpdated(String status) {
		}
	}
}
