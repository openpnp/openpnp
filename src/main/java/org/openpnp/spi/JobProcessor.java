package org.openpnp.spi;

import org.openpnp.JobProcessorDelegate;
import org.openpnp.JobProcessorListener;
import org.openpnp.model.Job;

public interface JobProcessor extends WizardConfigurable, PropertySheetHolder {
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
        PartError
    }
    
    public enum PickRetryAction {
        RetryWithFeed,
        RetryWithoutFeed,
        SkipAndContinue,
    }
    
    public enum Type {
        PickAndPlace,
        SolderPaste
    }
    
    public abstract void setDelegate(JobProcessorDelegate delegate);

    public abstract void addListener(JobProcessorListener listener);

    public abstract void removeListener(JobProcessorListener listener);

    public abstract Job getJob();

    public abstract JobState getState();

    // TODO: Change this, and most of the other properties on here to bound
    // properties.
    public abstract void load(Job job);

    /**
     * Start the Job. The Job must be in the Stopped state.
     */
    public abstract void start() throws Exception;

    /**
     * Pause a running Job. The Job will stop running at the next opportunity and retain
     * it's state so that it can be resumed. 
     */
    public abstract void pause();

    /**
     * Advances the Job one step. If the Job is not currently started this will
     * start the Job first.
     * @throws Exception
     */
    public abstract void step() throws Exception;

    /**
     * Resume a running Job. The Job will resume from where it was paused.
     */
    public abstract void resume();

    /**
     * Stop a running Job. The Job will stop immediately and will reset to it's 
     * freshly loaded state. All state about parts already placed will be lost.
     */
    public abstract void stop();

    public abstract void run();

}