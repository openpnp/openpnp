package org.openpnp;

import org.openpnp.Job.JobBoard;
import org.openpnp.JobProcessor.PickRetryAction;
import org.openpnp.spi.Feeder;

public interface JobProcessorDelegate {
	/**
	 * Notifies the delegate that the machine failed to pick the part and waits for
	 * a PickRetryAction response to determine what to do next.
	 * @param board
	 * @param part
	 * @param feeder
	 * @return
	 */
	public PickRetryAction partPickFailed(JobBoard board, Part part, Feeder feeder);
}
